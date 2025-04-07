package ru.l0sty.frogdisplays.testVideo;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import javax.sound.sampled.*;
import java.nio.ShortBuffer;

/**
 * Аудиоплеер для воспроизведения аудиопотока из m3u8-ссылки с использованием FFmpeg (JavaCV) и Java Sound.
 * Нативные библиотеки FFmpeg включаются через bytedeco-ffmpeg-platform – установки дополнительно не требуются.
 */
public class AudioPlayer {
    private String streamUrl;
    private FFmpegFrameGrabber grabber;
    private SourceDataLine line;
    private Thread playbackThread;
    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    public AudioPlayer(String streamUrl) {
        this.streamUrl = streamUrl;
    }

    /**
     * Запускает воспроизведение аудио с начала (или с заданного времени).
     * Если startTimestamp равен 0, то воспроизведение начинается с начала.
     *
     * @param startTimestamp время начала воспроизведения в микросекундах
     */
    private void playInternal(long startTimestamp) {
        stopped = false;
        paused = false;
        playbackThread = new Thread(() -> {
            try {
                grabber = new FFmpegFrameGrabber(streamUrl);
                grabber.start();
                // Если указан стартовый timestamp (> 0), перематываем поток
                if (startTimestamp > 0) {
                    grabber.setTimestamp(startTimestamp);
                }

                int sampleRate = grabber.getSampleRate();
                int channels = grabber.getAudioChannels();

                // Настраиваем аудио формат: 16 бит, little-endian, signed PCM
                AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
                line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(format);
                line.start();

                Frame frame;
                while (!stopped && (frame = grabber.grabSamples()) != null) {
                    // Реализация паузы
                    while (paused && !stopped) {
                        if (grabber.getAudioFrameRate()!=0) Thread.sleep((long) (1000/grabber.getAudioFrameRate()));
                    }
                    if (stopped) break;

                    if (frame.samples != null && frame.samples.length > 0) {
                        // Предполагается, что аудио данные находятся в frame.samples[0] в виде ShortBuffer
                        ShortBuffer channelSamples = (ShortBuffer) frame.samples[0];
                        channelSamples.rewind();
                        int numSamples = channelSamples.remaining();
                        byte[] buffer = new byte[numSamples * 2]; // 2 байта на short
                        for (int i = 0; i < numSamples; i++) {
                            short sample = channelSamples.get();
                            buffer[i * 2] = (byte) (sample & 0xFF);
                            buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                        }
                        line.write(buffer, 0, buffer.length);
                    }
                }
                line.drain();
                line.stop();
                line.close();
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        playbackThread.start();
    }

    /**
     * Запускает воспроизведение аудио с начала.
     */
    public void play() {
        playInternal(0);
    }

    /**
     * Приостанавливает воспроизведение аудио.
     */
    public void pause() {
        paused = true;
    }

    /**
     * Возобновляет воспроизведение аудио.
     */
    public void resume() {
        paused = false;
    }

    /**
     * Перематывает аудио на заданное время (в секундах).
     * В данном примере реализовано простое изменение временной метки в grabber-е.
     *
     * @param seconds время в секундах, на которое нужно перемотать аудио
     */
    public void seek(double seconds) {
        try {
            if (grabber != null) {
                // FFmpegFrameGrabber использует микросекунды для timestamp
                long timestamp = (long) (seconds * 1_000_000);
                grabber.setTimestamp(timestamp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Устанавливает громкость аудио.
     * Если поддерживается управляющий элемент MASTER_GAIN, применяется он.
     *
     * @param volume значение громкости в диапазоне от 0.0 до 1.0
     */
    public void setVolume(float volume) {
        if (line != null && line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();
            float newGain = min + (max - min) * volume;
            gainControl.setValue(newGain);
        }
    }

    /**
     * Полностью останавливает плеер и освобождает ресурсы.
     */
    public void stop() {
        stopped = true;
        if (playbackThread != null) {
            playbackThread.interrupt();
            try {
                playbackThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        playbackThread = null;
    }

    /**
     * Меняет ссылку стриминга, продолжая воспроизведение с того же места.
     * Сначала сохраняется текущий timestamp, затем останавливается текущий поток,
     * обновляется URL, и запускается новый поток с перемоткой на сохранённую позицию.
     *
     * @param newUrl новая m3u8 ссылка для потока
     */
    public void changeStreamUrl(String newUrl) {
        long currentTimestamp = 0;
        if (grabber != null) {
            currentTimestamp = grabber.getTimestamp();
        }
        stop();
        this.streamUrl = newUrl;
        playInternal(currentTimestamp);
    }
}