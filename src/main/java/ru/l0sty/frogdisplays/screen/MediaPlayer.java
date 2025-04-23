package ru.l0sty.frogdisplays.screen;

import com.github.felipeucelli.javatube.Stream;
import com.github.felipeucelli.javatube.Youtube;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.event.SeekFlags;
import org.freedesktop.gstreamer.elements.AppSink;
import org.lwjgl.opengl.GL11;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MediaPlayer {
    // Исходная ссылка на YouTube-видео
    private final String youtubeUrl;
    private int lastQuality;
    // Конвейеры GStreamer для видео и аудио
    private volatile Pipeline videoPipeline;
    private volatile Pipeline audioPipeline;
    // Элемент appsink для получения видеокадров
    private volatile AppSink videoSink;
    // Последний полученный кадр
    private volatile BufferedImage currentFrame;
    // Текущая громкость (по умолчанию 1.0)
    private volatile double currentVolume = 0.0;

    // Список доступных видео потоков (только видео)
    private volatile List<Stream> availableVideoStreams;
    // Текущий выбранный видео поток
    private volatile Stream currentVideoStream;

    // Флаг успешной инициализации (создания пайплайнов и извлечения потоков)
    private volatile boolean initialized = false;
    // Экзекутор для асинхронного выполнения операций
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    // Будущий результат инициализации
    private final Future<?> initFuture;

    public static boolean captureSamples = true;

    private boolean paused = true;

    /**
     * Конструктор, принимающий ссылку на YouTube-видео.
     * Долгие операции (извлечение потоков и создание конвейеров) выполняются асинхронно.
     */
    public MediaPlayer(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
        // Инициализация GStreamer (однократно для приложения)
        Gst.init("MediaPlayer");
        // Асинхронная инициализация
        initFuture = executor.submit(() -> {
            try {
                // Извлечение потоков с YouTube через библиотеку javatube
                System.out.println("Started search");
                Youtube yt;

                List<Stream> allStreams;
                try {
                    yt = new Youtube(youtubeUrl, "IOS");
                    System.out.println("Found Youtube object");
                    allStreams  = yt.streams().getAll();
                    System.out.println("Stream count: " + allStreams.size());
                } catch (Exception e) {
                    System.out.println("Failed to load youtube video" + youtubeUrl);
                    throw new RuntimeException(e);
                }

                // Отбираем только видео потоки
                availableVideoStreams = allStreams.stream()
                        .filter(s -> "video/webm".equals(s.getMimeType()))
                        .toList();

                // Выбираем видео поток по качеству: сначала ищем 480p, если нет – первый доступный
                Optional<Stream> videoStreamOpt = availableVideoStreams.stream()
                        .filter(stream -> stream.getResolution() != null)
                        .filter(s -> s.getResolution().contains("144"))
                        .findFirst();

                lastQuality = 144;

                if (videoStreamOpt.isEmpty()) {
                    videoStreamOpt = availableVideoStreams.stream().findFirst();
                }

                // Выбираем аудио поток – последний из списка с mime-типом "mp4"
                List<Stream> audioStreams = allStreams.stream()
                        .filter(s -> "audio/webm".equals(s.getMimeType()))
                        .toList();
                Optional<Stream> audioStreamOpt = audioStreams.isEmpty()
                        ? Optional.empty() : Optional.of(audioStreams.getLast());

                if (videoStreamOpt.isEmpty() || audioStreamOpt.isEmpty()) {
                    System.err.println("Не удалось выбрать видео или аудио поток.");
                    return;
                }

                currentVideoStream = videoStreamOpt.get();
                String videoStreamUrl = currentVideoStream.getUrl();
                String audioStreamUrl = audioStreamOpt.get().getUrl();

                // Создание конвейера для видео с использованием appsink (без вывода окна)
                String videoPipelineDesc = "uridecodebin uri=\"" + videoStreamUrl + "\" ! videoconvert ! video/x-raw,format=RGBA ! appsink name=videosink";
                videoPipeline = (Pipeline) Gst.parseLaunch(videoPipelineDesc);
                videoSink = (AppSink) videoPipeline.getElementByName("videosink");
                videoSink.set("emit-signals", true);
                videoSink.set("sync", true);
                videoSink.connect((AppSink.NEW_SAMPLE) elem -> {
                    Sample sample = elem.pullSample();
                    if (sample == null || !captureSamples)
                        return FlowReturn.OK;
                    Caps caps = sample.getCaps();
                    Structure struct = caps.getStructure(0);
                    int width = struct.getInteger("width");
                    int height = struct.getInteger("height");
                    Buffer buffer = sample.getBuffer();
                    ByteBuffer byteBuffer = buffer.map(false);
                    byte[] data = new byte[byteBuffer.remaining()];
                    byteBuffer.get(data);
                    buffer.unmap();
                    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                    image.getRaster().setDataElements(0, 0, width, height, data);
                    currentFrame = image;
                    sample.dispose();

                    return FlowReturn.OK;
                });

                // Создание аудиоконвейера с элементом volume
                String audioPipelineDesc = "uridecodebin uri=\"" + audioStreamUrl + "\" ! audioconvert ! audioresample ! volume name=volumeElement volume=" + currentVolume + " ! autoaudiosink";
                audioPipeline = (Pipeline) Gst.parseLaunch(audioPipelineDesc);

                initialized = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Асинхронно запускает воспроизведение обоих конвейеров.
     */
    public void play() {
        executor.submit(() -> {
            if (!initialized) return;
            Clock audioClock = audioPipeline.getClock();
            if (audioClock != null) {
                videoPipeline.setClock(audioClock);
            }
            videoPipeline.play();
            audioPipeline.play();
            paused = false;
        });
    }

    /**
     * Асинхронно переводит конвейеры в режим паузы.
     */
    public void pause() {
        executor.submit(() -> {
            if (!initialized) return;
            videoPipeline.pause();
            audioPipeline.pause();
            paused = true;
        });
    }

    /**
     * Асинхронно продолжает воспроизведение.
     */
    public void resume() {
        executor.submit(() -> {
            if (!initialized) return;
            videoPipeline.play();
            audioPipeline.play();
            paused = false;
        });
    }

    /**
     * Асинхронно останавливает воспроизведение и освобождает ресурсы.
     */
    public void stop() {
        executor.submit(() -> {
            if (videoPipeline != null) videoPipeline.stop();
            if (audioPipeline != null) audioPipeline.stop();
            executor.shutdownNow();
        });
        executor.shutdown();
    }

    /**
     * Асинхронно выполняет seek обоих конвейеров к заданному времени в секундах.
     *
     * @param nanos абсолютное время в секундах.
     */
    public void seekTo(long nanos) {
        executor.submit(() -> {
            if (!initialized) return;
            EnumSet<SeekFlags> seekFlags = EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE);

            videoPipeline.seekSimple(Format.TIME, seekFlags, nanos);
            audioPipeline.seekSimple(Format.TIME, seekFlags, nanos);
        });
    }

    /**
     * Асинхронно выполняет относительный seek (смещение в секундах) для обоих конвейеров.
     *
     * @param offsetSeconds смещение в секундах (положительное или отрицательное).
     */
    public void seekRelative(double offsetSeconds) {
        executor.submit(() -> {
            if (!initialized) return;
            long current = videoPipeline.queryPosition(Format.TIME);
            if (current < 0) {
                current = 0;
            }

            long offset = (long) (offsetSeconds * 1e9);

            long target = current + offset;

            if (target < 0) {
                target = 0;
            }

            long duration = audioPipeline.queryDuration(Format.TIME);

            if (target > duration) target = duration-1;

            seekTo(target);
        });
    }


    /**
     * Возвращает текущую позицию (аудио) в наносекундах.
     * Если конвейеры не инициализированы, возвращает 0.
     */
    public long getCurrentTime() {
        if (!initialized) return 0;
        return audioPipeline.queryPosition(Format.TIME);
    }

    public long getDuration() {
        if (!initialized) return 0;
        return audioPipeline.queryDuration(Format.TIME);
    }

    /**
     * Асинхронно изменяет громкость аудио.
     *
     * @param volume значение громкости (например, 0.0 ... 1.0).
     */
    public void setVolume(double volume) {
        currentVolume = volume;
        executor.submit(() -> {
            if (!initialized) return;
            Element volumeElement = audioPipeline.getElementByName("volumeElement");
            if (volumeElement != null) {
                volumeElement.set("volume", volume);
            }
        });
    }

    /**
     * Возвращает текущую громкость.
     */
    public double getVolume() {
        return currentVolume;
    }

    /**
     * Позволяет обновить OpenGL-текстуру текущим видеокадром.
     *
     * @param textureId     идентификатор текстуры.
     * @param textureWidth  ширина текстуры.
     * @param textureHeight высота текстуры.
     */
    public void updateFrame(int textureId, int textureWidth, int textureHeight) {
        if (currentFrame == null || !captureSamples) return;

        uploadBufferedImageToTexture(currentFrame, textureId, textureWidth, textureHeight);
    }

    public boolean textureFilled() {
        return currentFrame != null;
    }

    /**
     * Метод загрузки BufferedImage в OpenGL-текстуру.
     */
    private void uploadBufferedImageToTexture(BufferedImage image, int textureId, int textureWidth, int textureHeight) {
        BufferedImage textureImage = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = textureImage.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, textureWidth, textureHeight);
        g.setComposite(AlphaComposite.SrcOver);
        double scale = Math.max((double) textureWidth / image.getWidth(), (double) textureHeight / image.getHeight());
        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);
        int x = (textureWidth - scaledWidth) / 2;
        int y = (textureHeight - scaledHeight) / 2;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, x, y, scaledWidth, scaledHeight, null);
        g.dispose();
        byte[] pixelData = ((DataBufferByte) textureImage.getRaster().getDataBuffer()).getData();
        byte[] rgbaData = new byte[pixelData.length];
        for (int i = 0; i < pixelData.length; i += 4) {
            byte a = pixelData[i];
            byte b = pixelData[i + 1];
            byte gByte = pixelData[i + 2];
            byte r = pixelData[i + 3];
            rgbaData[i] = r;
            rgbaData[i + 1] = gByte;
            rgbaData[i + 2] = b;
            rgbaData[i + 3] = a;
        }
        ByteBuffer buffer = ByteBuffer.allocateDirect(rgbaData.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(rgbaData);
        buffer.flip();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, textureWidth, textureHeight,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    }

    /**
     * Возвращает true, если инициализация (извлечение потоков и создание пайплайнов) завершена.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Возвращает список доступных качеств (например, "144p", "240p", "360p", "480p", "720p", ...).
     * Если инициализация не завершена, возвращается пустой список.
     */
    public List<Integer> getAvailableQualities() {
        if (!initialized || availableVideoStreams == null) return Collections.emptyList();

        System.out.println("Returning non-empty list");

        // Собираем уникальные значения качества
        return availableVideoStreams.stream()
                .map(Stream::getResolution)
                .filter(Objects::nonNull)
                .distinct()
                .map(q -> Integer.parseInt(q.replace("p", "")))
                .sorted(Integer::compareTo)
                .collect(Collectors.toList());
    }

    /**
     * Асинхронно устанавливает требуемое качество видео.
     * Если такого качества нет, выбирается ближайшее по числовому значению.
     *
     * @param desiredQuality требуемое качество (например, "480p").
     */
    public void setQuality(String desiredQuality) {
        executor.submit(() -> {
            if (!initialized || availableVideoStreams == null) return;
            int target;
            try {
                // Извлекаем число из строки (например, из "480p")
                target = Integer.parseInt(desiredQuality.replaceAll("\\D+", ""));
            } catch (NumberFormatException e) {
                System.err.println("Неверный формат качества: " + desiredQuality);
                return;
            }

            if (target == lastQuality) return;

            // Поиск потока, у которого качество максимально близко к заданному
            Optional<Stream> bestMatch = availableVideoStreams.stream().filter(stream -> stream.getResolution() != null).min((s1, s2) -> {
                int q1, q2;
                try {
                    q1 = Integer.parseInt(s1.getResolution().replaceAll("\\D+", ""));
                } catch (NumberFormatException e) { q1 = Integer.MAX_VALUE; }
                try {
                    q2 = Integer.parseInt(s2.getResolution().replaceAll("\\D+", ""));
                } catch (NumberFormatException e) { q2 = Integer.MAX_VALUE; }
                return Integer.compare(Math.abs(q1 - target), Math.abs(q2 - target));
            });
            if (bestMatch.isEmpty()) return;
            Stream chosenStream = bestMatch.get();
            // Если выбранный поток совпадает с текущим, ничего не делаем
            if (currentVideoStream != null && chosenStream.getUrl().equals(currentVideoStream.getUrl()))
                return;

            // Создаем новый видео-конвейер для выбранного качества
            String newVideoPipelineDesc = "uridecodebin uri=\"" + chosenStream.getUrl() + "\" ! videoconvert ! video/x-raw,format=RGBA ! appsink name=videosink";
            Pipeline newVideoPipeline = (Pipeline) Gst.parseLaunch(newVideoPipelineDesc);
            AppSink newVideoSink = (AppSink) newVideoPipeline.getElementByName("videosink");
            newVideoSink.set("emit-signals", true);
            newVideoSink.set("sync", true);
            newVideoSink.connect((AppSink.NEW_SAMPLE) elem -> {
                Sample sample = elem.pullSample();
                if (sample == null)
                    return org.freedesktop.gstreamer.FlowReturn.OK;
                Caps caps = sample.getCaps();
                Structure struct = caps.getStructure(0);
                int width = struct.getInteger("width");
                int height = struct.getInteger("height");
                Buffer buffer = sample.getBuffer();
                ByteBuffer byteBuffer = buffer.map(false);
                byte[] data = new byte[byteBuffer.remaining()];
                byteBuffer.get(data);
                buffer.unmap();
                BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
                image.getRaster().setDataElements(0, 0, width, height, data);
                currentFrame = image;
                sample.dispose();
                return org.freedesktop.gstreamer.FlowReturn.OK;
            });

            // Синхронизируем новый видео-конвейер с аудио (если возможно)
            Clock audioClock = audioPipeline.getClock();
            if (audioClock != null) {
                newVideoPipeline.setClock(audioClock);
            }
            // Запускаем новый видео-конвейер
            if (paused) newVideoPipeline.pause();
            else newVideoPipeline.play();


            // Останавливаем и освобождаем старый видео-конвейер
            if (videoPipeline != null) {
                videoPipeline.stop();
                videoPipeline.dispose();
            }
            // Обновляем ссылку на текущий видео поток и конвейер
            videoPipeline = newVideoPipeline;

            seekTo(audioPipeline.queryPosition(Format.TIME));

            videoSink = newVideoSink;
            currentVideoStream = chosenStream;
        });
    }
}
