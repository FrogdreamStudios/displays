package com.inotsleep.dreamdisplays.client.media;

import com.inotsleep.dreamdisplays.client.Config;
import com.inotsleep.dreamdisplays.client.media.ytdlp.Format;
import com.inotsleep.dreamdisplays.client.media.ytdlp.YouTubeCacheEntry;
import com.inotsleep.dreamdisplays.client.media.ytdlp.YtDlpExecutor;
import me.inotsleep.utils.logging.LoggingManager;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

public class AudioVideoPlayer {
    public static final int VIDEO_QUEUE_CAPACITY = 30;
    public static final long MAX_VIDEO_LEAD_US = 20_000;
    private static Java2DFrameConverter converter;
    private String code = "";
    private int quality = Config.getInstance().defaultQuality;
    private String language = "default";
    private int userQuality = Config.getInstance().defaultQuality;
    private String userLanguage = "default";
    private String userCode = "";

    private volatile float volume = (float) Config.getInstance().defaultVolume;
    private volatile float attenuation = 1.0f;
    private volatile float volumeDb = 0;

    private SourceDataLine audioLine;
    private FloatControl volumeControl;
    private AudioFormat javaSoundFormat;
    private long audioStartPtsUs;
    private FFmpegFrameGrabber audioGrabber;
    private volatile double currentAudioUs;

    private FFmpegFrameGrabber videoGrabber;
    private final ArrayBlockingQueue<VFrame> videoQueue = new ArrayBlockingQueue<>(VIDEO_QUEUE_CAPACITY);

    private volatile boolean paused = true;
    private volatile boolean muted = false;
    private volatile boolean initialized = false;
    private volatile boolean errored = false;
    private volatile boolean stopped = false;
    private final AtomicBoolean seeked = new AtomicBoolean(false);

    private Thread audioGrabberThread;
    private Thread videoGrabberThread;

    private static final int BLOCK_SIZE = 512;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(r -> new Thread(r, "AudioVideoPlayer task executor"));
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "AudioVideoPlayer executor"));

    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    public long getDuration() {
        return audioGrabber.getLengthInTime();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isErrored() {
        return errored;
    }

    private static final class VFrame {
        public final Frame frame;
        public final long ptsUs;
        public VFrame(Frame f, long ptsUs) { this.frame = f; this.ptsUs = ptsUs; }
    }

    public AudioVideoPlayer() {}

    public AudioVideoPlayer(String code, int quality, String language) {
        this.code = code;
        this.quality = quality;
        this.language = language;
        this.userLanguage = language;
        this.userCode = code;
        this.userQuality = quality;
    }

    public long getTimestamp() { return (long) currentAudioUs; }

    public long getPlayedAudioUs() {
        if (audioLine == null || javaSoundFormat == null) return (long) currentAudioUs;
        long playedFrames = audioLine.getLongFramePosition();
        double rate = javaSoundFormat.getFrameRate();
        return audioStartPtsUs + (long)(playedFrames * 1_000_000.0 / rate);
    }

    public long getVideoAudioUsDifference() {
        VFrame head = videoQueue.peek();
        if (head != null) {
            return head.ptsUs - getPlayedAudioUs();
        }

        return 0;
    }

    public BufferedImage getImage() {
        return getImage(ColorSpace.getInstance(ColorSpace.CS_sRGB));
    }

    public BufferedImage getImage(ColorSpace cs) {
        VFrame head = videoQueue.peek();
        if (head != null) {
            long diff = head.ptsUs - getPlayedAudioUs();
            if (diff <= MAX_VIDEO_LEAD_US) {
                VFrame pkt = videoQueue.poll();
                try {
                    if (pkt != null && pkt.frame.image != null && pkt.frame.image.length > 0 && pkt.frame.image[0] != null) {
                        return getConverter().getBufferedImage(pkt.frame, 1.0, false, cs);
                    }
                } finally {
                    if (pkt != null) pkt.frame.close();
                }
            }
        }
        return null;
    }

    public void seekRelative(long time) { seekTo(getTimestamp() + time); }

    public void seekTo(long time) {
        if (!initialized || seeked.get()) return;
        executor.execute(() -> {
            boolean lastPaused = paused;
            paused = true;
            seeked.set(true);
            long maxTimestamp = Math.min(videoGrabber.getLengthInTime(), audioGrabber.getLengthInTime());
            long settingTime = Math.min(Math.max(time, 0), maxTimestamp);
            currentAudioUs = settingTime;
            audioStartPtsUs = settingTime;
            videoQueue.clear();
            try {
                videoGrabber.setTimestamp(settingTime);
                audioGrabber.setTimestamp(settingTime);
                audioLine.flush();
            } catch (FFmpegFrameGrabber.Exception e) {
                LoggingManager.error("Unable to set timestamp", e);
            }
            seeked.set(false);
            paused = lastPaused;
        });
    }

    public void onInit(Runnable task) {
        if (initialized) { EXECUTOR_SERVICE.execute(task); return; }
        tasks.add(task);
    }

    public boolean mute(boolean muted) {
        if (!initialized) return this.muted;

        if (this.muted == muted) return muted;

        this.muted = muted;
        volumeControl.setValue(muted ? volumeControl.getMinimum() : volumeDb);
        return muted;
    }

    public float setAttenuation(float attenuation) {
        this.attenuation = attenuation;

        setVolume(volume);

        return attenuation;
    }

    public float setVolume(float volume) {
        if (!initialized) return this.volume;
        this.volume = volume;
        float minDb = volumeControl.getMinimum();
        float maxDb = volumeControl.getMaximum();
        float db = minDb + (maxDb - minDb) * (volume * attenuation);
        volumeControl.setValue(db);
        this.volumeDb = db;
        return volume;
    }

    public float getVolume() { return volume; }
    public boolean isMuted() { return muted; }

    public boolean setPaused(boolean paused) {
        if (!initialized) return this.paused;
        this.paused = paused;
        return paused;
    }

    public boolean isPaused() { return paused; }

    public void setQuality(int quality) {
        this.userQuality = quality;
    }

    public void setLanguage(String language) { this.userLanguage = language; }
    public void setCode(String code) { this.userCode = code; }

    public void forceValues() {
        code = userCode;
        language = userLanguage;
        quality = userQuality;
    }

    public boolean updateQualityAndLanguage() {
        if (quality == userQuality && Objects.equals(language, userLanguage) && Objects.equals(code, userCode)) return false;

        errored = false;
        initialized = false;
        long latestTime = getTimestamp();
        boolean lastPaused = paused;
        paused = true;
        close();
        stopped = false;
        if (code.equals(userCode)) onInit(() -> seekTo(latestTime));
        onInit(() -> { setVolume(volume); paused = lastPaused; });
        code = userCode;
        quality = userQuality;
        language = userLanguage;
        initialize();

        return true;
    }

    public void close() {
        stopped = true;
        if (audioGrabberThread != null) audioGrabberThread.interrupt();
        if (videoGrabberThread != null) videoGrabberThread.interrupt();
        try {
            if (audioGrabber != null) audioGrabber.stop();
            if (videoGrabber != null) videoGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            LoggingManager.error("Error stopping grabber", e);
        }
        try {
            if (audioLine != null) {
                audioLine.flush();
                audioLine.close();
            }
        } catch (Exception ignore) {}
    }

    public void initialize() {
        errored = false;

        if (code == null || code.isEmpty()) {
            errored = true;
            return;
        }

        YouTubeCacheEntry cacheEntry = YtDlpExecutor.getInstance().getFormats(code);

        if (createAudioGrabber(cacheEntry) || createVideoGrabber(cacheEntry)) {
            errored = true;
            return;
        }
        initialized = true;
        runInitializedTasks();
    }

    private void runInitializedTasks() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try { EXECUTOR_SERVICE.execute(task); } catch (Exception e) {
                LoggingManager.getLogger().error("Error encountered while enqueuing task", e);
            }
        }
    }

    private boolean createAudioGrabber(YouTubeCacheEntry cacheEntry) {
        LoggingManager.info("Creating audio grabber");

        List<Format> audioFormats = cacheEntry.getAudioFormats(language);
        Optional<Format> optionalAudioFormat = selectBestAudioFormat(audioFormats);
        if (optionalAudioFormat.isEmpty()) {
            LoggingManager.error("Audio Format not found: " + code + " " + language);
            return true;
        }
        Format audioFormat = optionalAudioFormat.get();

        LoggingManager.info("Chosen format: " + audioFormat.getAcodec() + " " + audioFormat.getFormatNote());

        audioGrabber = new FFmpegFrameGrabber(audioFormat.getUrl());
        configureGrabber(audioGrabber, audioFormat);
        try {
            audioGrabber.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            LoggingManager.error("Error starting grabber", e);
            return true;
        }
        configureAudioLine();
        audioStartPtsUs = audioGrabber.getTimestamp();
        currentAudioUs = audioStartPtsUs;
        audioGrabberThread = new Thread(this::runAudioGrabber, "Audio Grabber Thread");
        audioGrabberThread.start();

        LoggingManager.info("Audio grabber created");
        return false;
    }

    private void runAudioGrabber() {
        try {
            Frame frame;
            boolean isNull;
            outerLoop:
            while (!stopped) {
                while (seeked.get() || paused || stopped) {
                    if (stopped) break;
                    LockSupport.parkNanos(500_000);
                }
                frame = audioGrabber.grabSamples();
                isNull = frame == null;
                if (isNull) break;
                ShortBuffer sb = (ShortBuffer) frame.samples[0];
                sb.rewind();
                int totalSamples = sb.remaining();
                short[] allSamples = new short[totalSamples];
                sb.get(allSamples);
                long frameEndUs = audioGrabber.getTimestamp();
                double sampleRate = audioGrabber.getSampleRate();
                double frameDurationMs = totalSamples * 1000.0 / sampleRate;
                double frameStartMs = frameEndUs / 1000.0 - frameDurationMs;
                int offset = 0;
                while (offset < totalSamples) {
                    int chunkSize = Math.min(BLOCK_SIZE, totalSamples - offset);
                    currentAudioUs = (frameStartMs + offset * 1000.0 / sampleRate) * 1000.0;
                    if (currentAudioUs < 0) currentAudioUs = 0;

                    byte[] buf = new byte[chunkSize * 2];
                    for (int i = 0; i < chunkSize; i++) {
                        short s = allSamples[offset + i];
                        buf[2 * i] = (byte) (s & 0xff);
                        buf[2 * i + 1] = (byte) ((s >> 8) & 0xff);
                    }
                    while (paused || stopped) {
                        if (stopped) break;
                        LockSupport.parkNanos(500_000);
                    }
                    if (seeked.get()) continue outerLoop;
                    audioLine.write(buf, 0, buf.length);
                    offset += chunkSize;
                }
            }
            audioLine.drain();
            if (!seeked.get()) onEOF();
        } catch (Exception e) {
            LoggingManager.error("Audio Grabber Thread: " + e);
        }
    }

    private void onEOF() {
        if (stopped) return;
        boolean latestPaused = paused;
        paused = false;
        try {
            audioGrabber.stop();
            videoGrabber.stop();
            if (audioGrabberThread != null) audioGrabberThread.interrupt();
            if (videoGrabberThread != null) videoGrabberThread.interrupt();
            audioGrabber.start();
            videoGrabber.start();
            audioStartPtsUs = audioGrabber.getTimestamp();
            audioGrabberThread = new Thread(this::runAudioGrabber, "Audio Grabber Thread");
            audioGrabberThread.start();
            videoGrabberThread = new Thread(this::runVideoGrabber, "Video Grabber Thread");
            videoGrabberThread.start();
        } catch (FFmpegFrameGrabber.Exception e) {
            LoggingManager.error("Audio Grabber Thread: " + e);
            return;
        }
        paused = latestPaused;
    }

    private boolean createVideoGrabber(YouTubeCacheEntry cacheEntry) {
        LoggingManager.info("Creating video grabber");

        List<Format> videoFormats = cacheEntry.getVideoFormats(quality);
        Optional<Format> optionalVideoFormat = selectByCodecPreference(videoFormats);
        if (optionalVideoFormat.isEmpty()) {
            LoggingManager.error("Video Format not found: " + code + " " + quality);
            return true;
        }
        Format videoFormat = optionalVideoFormat.get();

        LoggingManager.info("Chosen format: " + videoFormat.getVcodec() + " " + videoFormat.getFormatNote());


        videoGrabber = new FFmpegFrameGrabber(videoFormat.getUrl());
        configureGrabber(videoGrabber, videoFormat);
        try { videoGrabber.start(); } catch (FFmpegFrameGrabber.Exception e) {
            LoggingManager.error("Video Grabber Thread: " + e); return true; }
        videoGrabberThread = new Thread(this::runVideoGrabber, "Video Grabber Thread");
        videoGrabberThread.start();

        LoggingManager.info("Video Grabber created");
        return false;
    }

    private void runVideoGrabber() {
        try {
            while (!stopped) {
                while (seeked.get() || paused || stopped) {
                    if (stopped) return;
                    LockSupport.parkNanos(500_000);
                }
                Frame raw = videoGrabber.grabImage();
                if (raw == null) {
                    break; // EOS
                }
                if (raw.image == null) {
                    raw.close();
                    continue;
                }
                long pts = videoGrabber.getTimestamp();

                Frame copy = raw.clone();
                raw.close();
                VFrame pkt = new VFrame(copy, pts);
                try {
                    videoQueue.put(pkt);
                } catch (InterruptedException ie) {
                    copy.close();
                    return;
                }
            }
        } catch (FFmpegFrameGrabber.Exception e) {
            LoggingManager.error("Video Grabber Thread: " + e);
        }
    }

    private void configureAudioLine() {
        AudioFormat format = new AudioFormat(
                audioGrabber.getSampleRate(),
                16,
                audioGrabber.getAudioChannels(),
                true,
                false
        );
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        try {
            audioLine = (SourceDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();
            volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
            javaSoundFormat = format;
        } catch (LineUnavailableException e) {
            LoggingManager.error("Audio Grabber Thread: " + e);
        }
    }

    private static Optional<Format> selectBestAudioFormat(List<Format> audioFormats) {
        return audioFormats.stream().max(
                Comparator
                        .<Format, Boolean>comparing((f) -> {
                            String codec = f.getAcodec();
                            return codec != null && codec.toLowerCase(Locale.ROOT).contains("opus");
                        })
                        .thenComparing(f -> {
                                String c = f.getContainer();
                                return c != null && c.toLowerCase(Locale.ROOT).contains("dash");
                        })
                        .thenComparingDouble(f ->
                                (f.getAbr() != null && f.getAbr() > 0 ? f.getAbr()
                                        : (f.getTbr() != null ? f.getTbr() : 0.0))
                        )
        );
    }

    private static void configureGrabber(FFmpegFrameGrabber grabber, Format format) {
        if (format.getHttpHeaders() != null && !format.getHttpHeaders().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : format.getHttpHeaders().entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
            }
            grabber.setOption("headers", sb.toString());
        }
        grabber.setOption("reconnect", "1");
        grabber.setOption("reconnect_streamed", "1");
        grabber.setOption("reconnect_delay_max", "5");
        grabber.setOption("stream_loop", "-1");
        grabber.setOption("threads", "0");
    }

    private static final List<String> CODEC_PRIORITY = List.of("avc1", "vp9", "av01");

    private static Optional<Format> selectByCodecPreference(List<Format> formats) {
        return formats.stream().min(Comparator.comparingInt(f -> {
            String vc = f.getVcodec().toLowerCase();
            for (int i = 0; i < CODEC_PRIORITY.size(); i++) {
                if (vc.startsWith(CODEC_PRIORITY.get(i))) return i;
            }
            return CODEC_PRIORITY.size();
        }));
    }

    private static Java2DFrameConverter getConverter() {
        if (converter == null) {
            converter = new Java2DFrameConverter();
        }
        return converter;
    }
}