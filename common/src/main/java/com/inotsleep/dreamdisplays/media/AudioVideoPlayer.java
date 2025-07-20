package com.inotsleep.dreamdisplays.media;

import com.inotsleep.dreamdisplays.media.ytdlp.Format;
import com.inotsleep.dreamdisplays.media.ytdlp.YouTubeCacheEntry;
import com.inotsleep.dreamdisplays.media.ytdlp.YtDlpExecutor;
import me.inotsleep.utils.logging.LoggingManager;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.*;
import java.nio.ShortBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioVideoPlayer {
    private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();

    private String code;

    private int quality = 480;
    private String language;

    private volatile float volume = 0.3f;
    private volatile float volumeDb = 0;

    private SourceDataLine audioLine;
    private FloatControl volumeControl;

    private FFmpegFrameGrabber videoGrabber;
    private FFmpegFrameGrabber audioGrabber;

    private volatile boolean paused = true;
    private volatile boolean muted = false;
    private volatile boolean initialized = false;

    private volatile boolean stopped = false;

    private AtomicBoolean seeked = new AtomicBoolean(false);

    Thread audioGrabberThread;
    Thread videoGrabberThread;

    private static final int BLOCK_SIZE = 512;

    private volatile double currentAudioUs;
    private volatile Frame videoFrame;

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor(r -> new Thread(r, "AudioVideoPlayer executor"));


    public AudioVideoPlayer(String code) {
        this.code = code;
    }

    public Frame getFrame() {
        return videoFrame;
    }

    public void onInit(Runnable task) {
        if (initialized) {
            EXECUTOR_SERVICE.execute(task);
            return;
        }
        tasks.add(task);
    }

    private void runInitializedTasks() {
        Runnable task;
        while ((task = tasks.poll()) != null) {
            try {
                EXECUTOR_SERVICE.execute(task);
            } catch (Exception e) {
                LoggingManager.getLogger().error("Error encountered while enqueuing task", e);
            }
        }
    }

    public boolean mute(boolean muted) {
        if (!initialized) return this.muted;
        this.muted = muted;

        volumeControl.setValue(muted ? volumeControl.getMinimum() : volumeDb);

        return muted;
    }

    public float setVolume(float volume) {
        if (!initialized) return this.volume;
        this.volume = volume;

        float minDb = volumeControl.getMinimum();
        float maxDb = volumeControl.getMaximum();

        float db = minDb + (maxDb - minDb) * volume;
        volumeControl.setValue(db);

        this.volumeDb = db;

        return volume;
    }

    public float getVolume() {
        return volume;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean setPaused(boolean paused) {
        if (!initialized) return this.paused;
        this.paused = paused;
        return paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public void initialize(YtDlpExecutor executor) throws FFmpegFrameGrabber.Exception, LineUnavailableException, InterruptedException {
        YouTubeCacheEntry cacheEntry = executor.getFormats(code);

        if (createAudioGrabber(cacheEntry)) return;
        if (createVideoGrabber(cacheEntry)) return;

        initialized = true;
        runInitializedTasks();
    }

    private boolean createAudioGrabber(YouTubeCacheEntry cacheEntry) throws FFmpegFrameGrabber.Exception, LineUnavailableException {
        List<Format> audioFormats = cacheEntry
                .getAudioFormats(language)
                .stream()
                .filter(format -> "opus".equals(format.getAcodec()))
                .toList();

        Optional<Format> optionalAudioFormat = selectBestAudioFormat(audioFormats);

        if (optionalAudioFormat.isEmpty()) {
            LoggingManager.error("Audio Format not found: " + code + " " + language);
            return true;
        }

        Format audioFormat = optionalAudioFormat.get();
        audioGrabber = new FFmpegFrameGrabber(audioFormat.getUrl());
        configureGrabber(audioGrabber, audioFormat);

        audioGrabber.start();

        configureAudioLine();

        audioGrabberThread = new Thread(this::runAudioGrabber, "Audio Grabber Thread");
        audioGrabberThread.start();
        return false;
    }

    private void runAudioGrabber() {
        try {
            Frame frame;
            while ((frame = audioGrabber.grabSamples()) != null && !stopped) {
                ShortBuffer sb = (ShortBuffer) frame.samples[0];
                sb.rewind();
                int totalSamples = sb.remaining();
                short[] allSamples = new short[totalSamples];
                sb.get(allSamples);

                long frameEndUs = audioGrabber.getTimestamp();
                double frameEndMs = frameEndUs / 1000.0;

                double sampleRate    = audioGrabber.getSampleRate();
                double frameDurationMs = totalSamples * 1000.0 / sampleRate;

                double frameStartMs = frameEndMs - frameDurationMs;

                int offset = 0;
                while (offset < totalSamples) {
                    int chunkSize = Math.min(BLOCK_SIZE, totalSamples - offset);

                    currentAudioUs = (frameStartMs + offset * 1000.0 / sampleRate)*1000;

                    if (currentAudioUs < 0) {
                        currentAudioUs = 0;
                    }

                    byte[] buf = new byte[chunkSize * 2];
                    for (int i = 0; i < chunkSize; i++) {
                        short s = allSamples[offset + i];
                        buf[2*i] = (byte) (s & 0xff);
                        buf[2*i + 1] = (byte) ((s >> 8) & 0xff);
                    }

                    while (paused) {
                        Thread.sleep(10);
                    }

                    if (seeked.get()) {
                        seeked.getAndSet(false);
                        break;
                    }

                    audioLine.write(buf, 0, buf.length);
                    offset += chunkSize;
                }
            }
            audioLine.drain();

            onEOF();
        } catch (Exception e) {
            LoggingManager.error("Audio Grabber Thread: " + e);
        }
    }

    private void onEOF() {
        boolean latestPaused = paused;
        paused = false;
        try {
            audioGrabber.stop();
            videoGrabber.stop();

            audioGrabberThread.interrupt();
            videoGrabberThread.interrupt();

            audioGrabber.start();
            videoGrabber.start();

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

    private boolean createVideoGrabber(YouTubeCacheEntry cacheEntry) throws FFmpegFrameGrabber.Exception {
        List<Format> videoFormats = cacheEntry.getVideoFormats(quality);

        Optional<Format> optionalVideoFormat = selectByCodecPreference(videoFormats);
        if (optionalVideoFormat.isEmpty()) {
            LoggingManager.error("Video Format not found: " + code + " " + quality);
            return true;
        }

        Format videoFormat = optionalVideoFormat.get();
        videoGrabber = new FFmpegFrameGrabber(videoFormat.getUrl());
        configureGrabber(videoGrabber, videoFormat);

        videoGrabberThread = new Thread(this::runVideoGrabber, "Video Grabber Thread");
        videoGrabberThread.start();

        videoGrabber.start();
        return false;
    }

    private void runVideoGrabber() {
        try {
            boolean isNull = false;
            while (!isNull && !stopped) {
                videoFrame = videoGrabber.grabImage();
                isNull = videoFrame == null;

                while (!isNull && videoFrame.image == null) {
                    videoFrame = videoGrabber.grabImage();
                }

                if (paused) {
                    Thread.sleep(10);
                }

                while (currentAudioUs < videoGrabber.getTimestamp()) {
                    Thread.sleep(10);
                }
            }
        } catch (FrameGrabber.Exception | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void configureAudioLine() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                audioGrabber.getSampleRate(),
                16,
                audioGrabber.getAudioChannels(),
                true,
                false
        );

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        audioLine = (SourceDataLine) AudioSystem.getLine(info);

        audioLine.open(format);
        audioLine.start();

        volumeControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
    }

    private static @NotNull Optional<Format> selectBestAudioFormat(List<Format> audioFormats) {
        return audioFormats.stream().max(Comparator.comparingDouble(f ->
                f.getAbr() != null && f.getAbr() > 0
                        ? f.getAbr()
                        : (f.getTbr() != null ? f.getTbr() : 0.0)
        ));
    }

    private static void configureGrabber(FFmpegFrameGrabber grabber, Format format) {
        if (format.getHttpHeaders() != null && !format.getHttpHeaders().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String,String> e : format.getHttpHeaders().entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append("\r\n");
            }
            grabber.setOption("headers", sb.toString());
        }

        grabber.setOption("reconnect", "1");
        grabber.setOption("reconnect_streamed", "1");
        grabber.setOption("reconnect_delay_max", "5");
        grabber.setOption("stream_loop",    "-1");
    }

    private static final List<String> CODEC_PRIORITY = List.of(
            "avc1",
            "vp9",
            "av01"
    );

    public static Optional<Format> selectByCodecPreference(List<Format> formats) {
        return formats.stream()
                .min(Comparator.comparingInt(f -> {
                    String vc = f.getVcodec().toLowerCase();
                    for (int i = 0; i < CODEC_PRIORITY.size(); i++) {
                        if (vc.startsWith(CODEC_PRIORITY.get(i))) {
                            return i;
                        }
                    }
                    return CODEC_PRIORITY.size();
                }));
    }
}
