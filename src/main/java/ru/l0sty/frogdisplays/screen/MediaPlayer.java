package ru.l0sty.frogdisplays.screen;

import com.github.felipeucelli.javatube.Stream;
import com.github.felipeucelli.javatube.Youtube;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.elements.AppSink;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * MediaPlayer – компактная, стабильная версия.
 * Публичный интерфейс сохранён.
 */
public class MediaPlayer {
    // GStreamer constants
    private static final String MIME_VIDEO = "video/webm";
    private static final String MIME_AUDIO = "audio/webm";
    private static final String USER_AGENT = "IOS";

    // Public API fields
    private final String youtubeUrl;
    private volatile double currentVolume;
    public static boolean captureSamples = true;

    // GStreamer pipelines
    private volatile Pipeline videoPipeline;
    private volatile Pipeline audioPipeline;
    private volatile AppSink videoSink;

    private volatile List<Stream> availableVideoStreams;
    private volatile Stream currentVideoStream;
    private volatile boolean initialized;
    private int lastQuality;

    // Frame buffers
    private volatile BufferedImage currentFrame;
    private volatile ByteBuffer preparedBuffer;

    // Для динамической переаллокации под новую ширину/высоту
    private volatile int lastTexW = 0;
    private volatile int lastTexH = 0;
    private volatile int preparedW = 0;
    private volatile int preparedH = 0;

    // Executors
    private final ExecutorService gstExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "MediaPlayer-gst"));
    private final ExecutorService frameExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "MediaPlayer-frame"));
    private final Future<?> initFuture;

    private BufferedImage textureImage;
    private final Screen screen;

    // Constructor
    public MediaPlayer(String youtubeUrl, Screen screen) {
        this.youtubeUrl = youtubeUrl;
        this.screen = screen;
        Gst.init("MediaPlayer");
        initFuture = gstExecutor.submit(this::initialize);
    }

    // PUBLIC API
    public void play()   { gstExecutor.submit(this::doPlay); }
    public void pause()  { gstExecutor.submit(this::doPause); }
    public void stop() {
        gstExecutor.submit(this::doStop);
        gstExecutor.shutdownNow();
        frameExecutor.shutdownNow();
    }

    public void seekTo(long nanos) { gstExecutor.submit(() -> doSeek(nanos)); }
    public void seekRelative(double sec) {
        gstExecutor.submit(() -> {
            if (!initialized) return;
            long cur = Math.max(0, audioPipeline.queryPosition(Format.TIME));
            long tgt = Math.max(0, cur + (long)(sec * 1e9));
            long dur = Math.max(0, audioPipeline.queryDuration(Format.TIME) - 1);
            doSeek(Math.min(tgt, dur));
        });
    }

    public long getCurrentTime() { return initialized ? audioPipeline.queryPosition(Format.TIME) : 0; }
    public long getDuration()    { return initialized ? audioPipeline.queryDuration(Format.TIME) : 0; }

    public void setVolume(double v) { currentVolume = v; gstExecutor.submit(this::applyVolume); }
    public double getVolume()       { return currentVolume; }

    public boolean isInitialized()  { return initialized; }

    /** Готовность текстуры по не-нулевым размерам экрана. */
    public boolean textureFilled() {
        return screen != null
                && screen.textureWidth  > 0
                && screen.textureHeight > 0;
    }

    /**
     * Обновляем содержимое OpenGL-текстуры.
     * Размеры берём из screen.textureWidth/Height.
     * При изменении размеров — TexImage2D, иначе — TexSubImage2D.
     */
    public void updateFrame(int texId) {
        if (preparedBuffer == null) return;

        int w = screen.textureWidth, h = screen.textureHeight;
        // guard: буфер ещё не пересоздан под текущий размер
        if (w != preparedW || h != preparedH) return;

        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);

        if (w != lastTexW || h != lastTexH) {
            // сначала аллоцируем хранилище
            GL11.glTexImage2D(
                    GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA,
                    w, h, 0,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    preparedBuffer
            );
            lastTexW = w;
            lastTexH = h;
        } else {
            // быстрый апдейт
            GL11.glTexSubImage2D(
                    GL11.GL_TEXTURE_2D, 0,
                    0, 0, w, h,
                    GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                    preparedBuffer
            );
        }
    }

    public List<Integer> getAvailableQualities() {
        if (!initialized || availableVideoStreams == null) return Collections.emptyList();
        return availableVideoStreams.stream()
                .map(Stream::getResolution)
                .filter(Objects::nonNull)
                .map(r -> Integer.parseInt(r.replaceAll("\\D+", "")))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public void setQuality(String q) {
        gstExecutor.submit(() -> changeQuality(q));
    }

    // INITIALIZATION
    private void initialize() {
        try {
            Youtube yt = new Youtube(youtubeUrl, USER_AGENT);
            List<Stream> all = yt.streams().getAll();

            // все видео-потоки
            availableVideoStreams = all.stream()
                    .filter(s -> MIME_VIDEO.equals(s.getMimeType()))
                    .toList();

            // выбрали одну «по умолчанию» для видео
            Optional<Stream> videoOpt = pickVideo(144).or(() -> availableVideoStreams.stream().findFirst());

            // выбрали аудио один раз и навсегда (можно тут же сортировать по bitrate)
            Optional<Stream> audioOpt = all.stream()
                    .filter(s -> MIME_AUDIO.equals(s.getMimeType()))
                    .reduce((first, next) -> next);

            if (videoOpt.isEmpty() || audioOpt.isEmpty()) {
                System.err.println("No streams available");
                return;
            }

            currentVideoStream = videoOpt.get();
            lastQuality = parseQuality(currentVideoStream);

            // строим аудио-пайплайн один раз
            audioPipeline = buildAudioPipeline(audioOpt.get().getUrl());

            // строим видео-пайплайн под текущее качество
            videoPipeline = buildVideoPipeline(currentVideoStream.getUrl());

            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Pipeline buildVideoPipeline(String uri) {
        // GStreamer отдаёт RGBA
        String desc = "uridecodebin uri=\"" + uri + "\" ! videoconvert ! video/x-raw,format=RGBA ! appsink name=videosink";
        Pipeline p = (Pipeline) Gst.parseLaunch(desc);
        configureVideoSink((AppSink)p.getElementByName("videosink"));
        p.pause();
        return p;
    }

    private Pipeline buildAudioPipeline(String uri) {
        String desc = "uridecodebin uri=\"" + uri + "\" ! audioconvert ! audioresample"
                + " ! volume name=volumeElement volume=" + currentVolume
                + " ! autoaudiosink";
        return (Pipeline) Gst.parseLaunch(desc);
    }

    private void configureVideoSink(AppSink sink) {
        this.videoSink = sink;
        sink.set("emit-signals", true);
        sink.set("sync", true);
        sink.connect((AppSink.NEW_SAMPLE) elem -> {
            Sample s = elem.pullSample();
            if (s == null || !captureSamples) return FlowReturn.OK;
            try {
                currentFrame = sampleToImage(s, currentFrame);
                prepareBufferAsync();
            } finally {
                s.dispose();
            }
            return FlowReturn.OK;
        });
    }

    // FRAME PROCESSING

    /** RGBA→ABGR для BufferedImage */
    private static BufferedImage sampleToImage(Sample sample, BufferedImage reuse) {
        Structure st = sample.getCaps().getStructure(0);
        int w = st.getInteger("width"), h = st.getInteger("height");
        Buffer buf = sample.getBuffer();
        ByteBuffer bb = buf.map(false);
        try {
            if (reuse == null || reuse.getWidth() != w || reuse.getHeight() != h) {
                reuse = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            }
            byte[] dst = ((DataBufferByte)reuse.getRaster().getDataBuffer()).getData();
            byte[] src = new byte[dst.length];
            bb.get(src);
            // конвертируем: R G B A → A B G R
            for (int i = 0; i < src.length; i += 4) {
                byte r = src[i], g = src[i+1], b = src[i+2], a = src[i+3];
                dst[i]   = a;
                dst[i+1] = b;
                dst[i+2] = g;
                dst[i+3] = r;
            }
        } finally {
            buf.unmap();
        }
        return reuse;
    }

    private void prepareBufferAsync() {
        if (currentFrame == null) return;
        int w = screen.textureWidth, h = screen.textureHeight;
        if (w == 0 || h == 0) return;
        try {
            frameExecutor.submit(this::prepareBuffer);
        } catch (RejectedExecutionException ignored) {}
    }

    private void prepareBuffer() {
        int w = screen.textureWidth, h = screen.textureHeight;
        if (w == 0 || h == 0) return;

        if (textureImage == null || textureImage.getWidth()  != w
                || textureImage.getHeight() != h) {
            textureImage = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
        }
        Graphics2D g = textureImage.createGraphics();
        // очистка
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.SrcOver);

        // aspect-fit отрисовка кадра
        double scale = Math.max((double)w/currentFrame.getWidth(),
                (double)h/currentFrame.getHeight());
        int sw = (int)Math.round(currentFrame.getWidth()*scale);
        int sh = (int)Math.round(currentFrame.getHeight()*scale);
        int x  = (w - sw)/2, y = (h - sh)/2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(currentFrame, x, y, sw, sh, null);
        g.dispose();

        preparedBuffer = imageToDirect(textureImage);
        preparedW = w;
        preparedH = h;
    }

    private static ByteBuffer imageToDirect(BufferedImage img) {
        byte[] abgr = ((DataBufferByte)img.getRaster().getDataBuffer()).getData();
        ByteBuffer buf = ByteBuffer.allocateDirect(abgr.length)
                .order(ByteOrder.nativeOrder());
        // ABGR → RGBA
        for (int i = 0; i < abgr.length; i += 4) {
            byte a = abgr[i];
            byte b = abgr[i+1];
            byte g = abgr[i+2];
            byte r = abgr[i+3];
            buf.put(r).put(g).put(b).put(a);
        }
        buf.flip();
        return buf;
    }

    // PLAYBACK HELPERS

    private void doPlay() {
        if (!initialized) return;
        audioPipeline.play();
        Clock c = audioPipeline.getClock();
        if (c != null && videoPipeline != null) {
            videoPipeline.setClock(c);
        }
        videoPipeline.play();
    }

    private void doPause() {
        if (!initialized) return;
        if (videoPipeline != null) videoPipeline.pause();
        if (audioPipeline != null) audioPipeline.pause();
    }

    private void doStop() {
        if (videoPipeline != null) { videoPipeline.stop(); videoPipeline.dispose(); }
        if (audioPipeline != null) { audioPipeline.stop(); audioPipeline.dispose(); }
    }

    private void doSeek(long ns) {
        if (!initialized) return;
        // оба, если нужна синхронность
        EnumSet<SeekFlags> flags = EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE);
        videoPipeline.seekSimple(Format.TIME, flags, ns);
        audioPipeline.seekSimple(Format.TIME, flags, ns);
    }

    private void applyVolume() {
        if (!initialized) return;
        Element v = audioPipeline.getElementByName("volumeElement");
        if (v != null) v.set("volume", currentVolume);
    }

    // QUALITY HELPERS

    private Optional<Stream> pickVideo(int target) {
        return availableVideoStreams.stream()
                .filter(s -> s.getResolution()!=null)
                .min(Comparator.comparingInt(s -> Math.abs(parseQuality(s) - target)));
    }

    private static int parseQuality(Stream s) {
        try {
            return Integer.parseInt(s.getResolution().replaceAll("\\D+", ""));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    private void changeQuality(String desired) {
        if (!initialized || availableVideoStreams==null) return;
        int target;
        try {
            target = Integer.parseInt(desired.replaceAll("\\D+", ""));
        } catch (NumberFormatException e) {
            return;
        }
        if (target == lastQuality) return;

        Optional<Stream> best = pickVideo(target);
        if (best.isEmpty()) return;
        Stream chosen = best.get();
        if (chosen.getUrl().equals(currentVideoStream.getUrl())) return;

        // текущая позиция по аудио
        long pos = audioPipeline.queryPosition(Format.TIME);

        // остановить старый videoPipeline
        videoPipeline.stop();
        videoPipeline.dispose();

        // собрать новый под нужный URL
        Pipeline newVid = buildVideoPipeline(chosen.getUrl());
        // синхронизировать его по аудио-часам
        Clock c = audioPipeline.getClock();
        if (c != null) newVid.setClock(c);

        // поставить на нужную позицию и запустить
        EnumSet<SeekFlags> flags = EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE);
        newVid.seekSimple(Format.TIME, flags, pos);
        newVid.play();
        audioPipeline.play();
        
        // заменить
        videoPipeline = newVid;
        currentVideoStream = chosen;
        lastQuality = parseQuality(chosen);
    }
}