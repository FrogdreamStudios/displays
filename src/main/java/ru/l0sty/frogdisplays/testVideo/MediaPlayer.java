package ru.l0sty.frogdisplays.testVideo;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import org.lwjgl.opengl.GL11;

public class MediaPlayer {
    // URL для видео и аудио
    private String videoStreamUrl;
    private String audioStreamUrl;

    // ======== Видео ==========
    private FFmpegFrameGrabber videoGrabber;
    private volatile boolean videoRunning = false;
    private Thread videoThread;
    private volatile BufferedImage currentFrame;
    private Java2DFrameConverter videoConverter = new Java2DFrameConverter();

    // ======== Аудио ==========
    private FFmpegFrameGrabber audioGrabber;
    private volatile boolean audioRunning = false;
    private Thread audioThread;
    private SourceDataLine audioLine;

    // ======== Синхронизация ==========
    // syncTime – мастер-время, обновляемое аудиопотоком (в секундах)
    private volatile double syncTime = 0;

    // Флаги паузы
    private volatile boolean videoPaused = false;
    private volatile boolean audioPaused = false;

    // Флаг, сигнализирующий о том, что выполнен seek (для обработки синхронизации)
    private volatile boolean justSeeked = false;

    // ======== Прочие поля ========
    // Используем системное время для аудио
    private long audioStartMillis = 0;

    // ======== Конструктор ==========
    public MediaPlayer(String videoStreamUrl, String audioStreamUrl) {
        this.videoStreamUrl = videoStreamUrl;
        this.audioStreamUrl = audioStreamUrl;
        videoGrabber = new FFmpegFrameGrabber(videoStreamUrl);
        audioGrabber = new FFmpegFrameGrabber(audioStreamUrl);
    }

    /**
     * Запускает воспроизведение: инициализирует грабберы, аудиовыход и стартует
     * отдельные потоки для видео и аудио.
     */
    public void play() {
        try {
            videoGrabber.start();
            audioGrabber.start();
            setupAudioLine();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        videoRunning = true;
        audioRunning = true;
        videoThread = new Thread(this::videoDecodeLoop);
        audioThread = new Thread(this::audioDecodeLoop);
        videoThread.start();
        audioThread.start();
    }

    /**
     * Инициализирует аудиовыход по параметрам аудиограббера.
     */
    private void setupAudioLine() throws LineUnavailableException {
        int sampleRate = audioGrabber.getSampleRate();
        int channels = audioGrabber.getAudioChannels();
        AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        audioLine = (SourceDataLine) AudioSystem.getLine(info);
        audioLine.open(format);
        audioLine.start();
        // Зафиксируем время старта аудио для системного времени
        audioStartMillis = System.currentTimeMillis();
    }

    /**
     * Поток аудио: захватывает аудиофреймы, выводит их в аудиоустройство и обновляет мастер-время синхронизации.
     */
    private void audioDecodeLoop() {
        while (audioRunning) {
            try {
                if (audioPaused) {
                    Thread.sleep(10);
                    continue;
                }
                Frame frame = audioGrabber.grabSamples();
                if (frame == null) {
                    // Перезапускаем аудиопоток при достижении конца
                    audioGrabber.setTimestamp(0);
                    audioStartMillis = System.currentTimeMillis();
                    continue;
                }
                // Получаем время по грабберу и системному времени, выбираем большее
                double tsFromGrabber = audioGrabber.getTimestamp() / 1e6;
                double systemAudioTime = (System.currentTimeMillis() - audioStartMillis) / 1000.0;
                double audioCurrentTime = Math.max(tsFromGrabber, systemAudioTime);
                syncTime = audioCurrentTime;

                if (frame.samples != null && frame.samples.length > 0) {
                    ShortBuffer channelSamples = (ShortBuffer) frame.samples[0];
                    channelSamples.rewind();
                    int numSamples = channelSamples.remaining();
                    byte[] buffer = new byte[numSamples * 2]; // 2 байта на short
                    for (int i = 0; i < numSamples; i++) {
                        short sample = channelSamples.get();
                        buffer[i * 2] = (byte) (sample & 0xFF);
                        buffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
                    }
                    audioLine.write(buffer, 0, buffer.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cleanupAudio();
    }

    /**
     * Поток видео: захватывает кадры, синхронизует их по времени аудио и обновляет currentFrame.
     * При выполнении seek флаг justSeeked отключает задержку, чтобы избежать «зависания».
     *
     * Основное изменение – если разница между аудио и видео (diff) находится в пределах ±100 мс,
     * то вместо sleep() используется Thread.yield(), чтобы не добавлять дополнительную задержку.
     */
    private void videoDecodeLoop() {
        while (videoRunning) {
            try {
                if (videoPaused) {
                    Thread.sleep(10);
                    continue;
                }
                // Если был выполнен seek, сбрасываем флаг и пропускаем задержку
                if (justSeeked) {
                    justSeeked = false;
                    Thread.yield();
                }

                Frame frame = videoGrabber.grab();
                if (frame == null) {
                    videoGrabber.setTimestamp(0);
                    continue;
                }
                currentFrame = videoConverter.convert(frame);
                // Получаем текущее время видео в секундах
                long videoTS = videoGrabber.getTimestamp();
                double videoTime = videoTS / 1e6;

                // Вычисляем разницу: diff > 0 – аудио опережает видео, diff < 0 – видео опережает аудио
                double diff = syncTime - videoTime;
                System.out.println("VideoTime: " + videoTime + " AudioTime: " + syncTime + " Diff: " + diff);

                // Если разница меньше ±100 мс, не используем sleep, чтобы не вносить дополнительную задержку.
                if (Math.abs(diff) < 0.1) {
                    Thread.yield();
                } else if (diff < -0.1) { // видео опережает аудио более чем на 100 мс
                    // Пытаемся сократить задержку, спим лишь на половину разницы
                    long sleepTime = (long)((-diff) * 500);
                    Thread.sleep(sleepTime);
                } else if (diff > 0.1) { // видео отстаёт от аудио более чем на 100 мс
                    // В данном случае можно попробовать не ждать, чтобы быстрее догнать аудио
                    Thread.yield();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        cleanupVideo();
    }

    public void setVolume(float volume) {
        if (audioLine != null && audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gainControl = (FloatControl) audioLine.getControl(FloatControl.Type.MASTER_GAIN);
            float min = gainControl.getMinimum();
            float max = gainControl.getMaximum();
            float newGain = min + (max - min) * volume;
            gainControl.setValue(newGain);
        }
    }

    /**
     * Останавливает и освобождает аудиограббер и аудиовыход.
     */
    private void cleanupAudio() {
        try {
            if (audioLine != null) {
                audioLine.drain();
                audioLine.stop();
                audioLine.close();
            }
            if (audioGrabber != null) {
                audioGrabber.stop();
                audioGrabber.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Останавливает и освобождает видеограббер.
     */
    private void cleanupVideo() {
        try {
            if (videoGrabber != null) {
                videoGrabber.stop();
                videoGrabber.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Управление воспроизведением
    public void pause() {
        videoPaused = true;
        audioPaused = true;
    }

    public void resume() {
        videoPaused = false;
        audioPaused = false;
    }

    /**
     * Останавливает оба потока воспроизведения.
     */
    public void stop() {
        videoRunning = false;
        audioRunning = false;
        try {
            if (videoThread != null) {
                videoThread.join();
            }
            if (audioThread != null) {
                audioThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Относительный seek для видео и аудио.
     * После seek устанавливается флаг justSeeked, чтобы в следующем цикле видео не зависать.
     *
     * @param offsetSeconds смещение в секундах
     */
    public synchronized void seekRelative(double offsetSeconds) {
        try {
            long offsetMicro = (long) (offsetSeconds * 1_000_000);
            long currentTS = (long) (syncTime * 1_000_000);
            long newTS = currentTS + offsetMicro;
            if (newTS < 0) {
                newTS = 0;
            }
            if (videoGrabber != null) {
                try {
                    long videoDuration = videoGrabber.getLengthInTime();
                    if (videoDuration > 0 && newTS > videoDuration) {
                        newTS = videoDuration;
                    }
                } catch (Exception ex) { }
                videoGrabber.setTimestamp(newTS);
            }
            if (audioGrabber != null) {
                try {
                    long audioDuration = audioGrabber.getLengthInTime();
                    if (audioDuration > 0 && newTS > audioDuration) {
                        newTS = audioDuration;
                    }
                } catch (Exception ex) { }
                audioGrabber.setTimestamp(newTS);
                // Пересчитываем системное время для аудио
                audioStartMillis = System.currentTimeMillis() - newTS / 1000;
                syncTime = (double) newTS / 1_000_000;
            }
            justSeeked = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Выполняет seek для видео и аудио к заданному времени.
     *
     * @param seconds время в секундах
     */
    public synchronized void seekTo(double seconds) {
        try {
            long newTS = (long) (seconds * 1_000_000);
            if (newTS < 0) {
                newTS = 0;
            }
            if (videoGrabber != null) {
                try {
                    long videoDuration = videoGrabber.getLengthInTime();
                    if (videoDuration > 0 && newTS > videoDuration) {
                        newTS = videoDuration;
                    }
                } catch (Exception ex) { }
                videoGrabber.setTimestamp(newTS);
            }
            if (audioGrabber != null) {
                try {
                    long audioDuration = audioGrabber.getLengthInTime();
                    if (audioDuration > 0 && newTS > audioDuration) {
                        newTS = audioDuration;
                    }
                } catch (Exception ex) { }
                audioGrabber.setTimestamp(newTS);
                audioStartMillis = System.currentTimeMillis() - newTS / 1000;
                syncTime = (double) newTS / 1_000_000;
            }
            justSeeked = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getVideoCurrentTime() {
        return videoGrabber.getTimestamp() / 1e6;
    }

    public double getAudioCurrentTime() {
        return audioGrabber.getTimestamp() / 1e6;
    }

    /**
     * Обновляет содержимое OpenGL-текстуры текущим видео-кадром.
     */
    public void updateFrame(int textureId, int textureWidth, int textureHeight) {
        if (currentFrame != null) {
            uploadBufferedImageToTexture(currentFrame, textureId, textureWidth, textureHeight);
        }
    }

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
     * Меняет m3u8-ссылку для аудио, продолжая воспроизведение с того же места.
     */
    public void changeAudioStreamUrl(String newUrl) {
        long currentTimestamp = 0;
        if (audioGrabber != null) {
            currentTimestamp = audioGrabber.getTimestamp();
        }
        stopAudio();
        this.audioStreamUrl = newUrl;
        audioGrabber = new FFmpegFrameGrabber(newUrl);
        playAudioInternal(currentTimestamp);
    }

    private void stopAudio() {
        audioRunning = false;
        if (audioThread != null) {
            audioThread.interrupt();
            try {
                audioThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        audioThread = null;
    }

    /**
     * Запускает аудио с указанного таймстампа.
     */
    private void playAudioInternal(long startTimestamp) {
        try {
            audioGrabber.start();
            audioGrabber.setTimestamp(startTimestamp);
            setupAudioLine();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        audioThread = new Thread(this::audioDecodeLoop);
        audioRunning = true;
        audioThread.start();
    }

    /**
     * Завершает работу плеера и освобождает ресурсы.
     */
    public void close() {
        stop();
        try {
            if (videoGrabber != null) {
                videoGrabber.stop();
                videoGrabber.close();
            }
            if (audioGrabber != null) {
                audioGrabber.stop();
                audioGrabber.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
