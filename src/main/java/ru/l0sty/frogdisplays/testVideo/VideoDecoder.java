package ru.l0sty.frogdisplays.testVideo;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.lwjgl.opengl.GL11;
import java.awt.image.DataBufferByte;

/**
 * Класс VideoDecoder отвечает за декодирование m3u8-видео потока.
 * Используется FFmpeg через JavaCV для получения кадров, которые затем
 * загружаются в OpenGL-текстуру.
 */
public class VideoDecoder {
    private String streamUrl;
    private FFmpegFrameGrabber frameGrabber;
    private volatile boolean running = false;
    private Thread decodeThread;
    private volatile BufferedImage currentFrame;
    private Java2DFrameConverter converter = new Java2DFrameConverter();
    private double currentTime = 0; // время воспроизведения в секундах

    public VideoDecoder(String streamUrl) {
        this.streamUrl = streamUrl;
        frameGrabber = new FFmpegFrameGrabber(streamUrl);
    }

    /**
     * Запускает процесс декодирования видео.
     */
    public void start() {
        try {
            frameGrabber.start();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        running = true;
        decodeThread = new Thread(this::decodeLoop);
        decodeThread.start();
    }

    /**
     * Основной цикл декодирования, работающий в отдельном потоке.
     */
    private void decodeLoop() {
        long baseTimestamp = -1;       // Базовая метка времени первого кадра
        long startTimeMillis = 0;      // Системное время, когда пришёл первый кадр

        while (running) {
            try {
                Frame frame = frameGrabber.grab();
                if (frame == null) {
                    break;
                }
                // Если первый кадр — фиксируем базовую метку и время старта
                if (baseTimestamp < 0) {
                    baseTimestamp = frameGrabber.getTimestamp();
                    startTimeMillis = System.currentTimeMillis();
                }
                // Вычисляем относительное время воспроизведения (в секундах)
                currentTime = (frameGrabber.getTimestamp() - baseTimestamp) / 1e6;
                currentFrame = converter.convert(frame);

                // Вычисляем ожидаемое время, прошедшее с начала воспроизведения (в миллисекундах)
                long expectedElapsedMillis = (frameGrabber.getTimestamp() - baseTimestamp) / 1000;
                // Фактически прошедшее время с первого кадра
                long actualElapsedMillis = System.currentTimeMillis() - startTimeMillis;
                long delay = expectedElapsedMillis - actualElapsedMillis;

                if (delay > 0) {
                    Thread.sleep(delay);
                }
                // Если delay отрицательный, значит, мы уже опаздываем — сразу переходим к следующему кадру
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Приостанавливает декодирование.
     */
    public void pause() {
        running = false;
        try {
            decodeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Возобновляет декодирование.
     */
    public void resume() {
        if (!running) {
            start();
        }
    }

    /**
     * Перематывает видео к заданному времени (в секундах).
     */
    public void seek(double seconds) {
        try {
            frameGrabber.setTimestamp((long) (seconds * 1e6));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Возвращает текущее время воспроизведения (в секундах).
     */
    public double getCurrentTime() {
        return currentTime;
    }

    /**
     * Обновляет содержимое OpenGL-текстуры текущим кадром видео.
     * Вызывается каждый игровой тик.
     *
     * @param textureId ID OpenGL-текстуры, в которую загружается кадр.
     */
    public void updateFrame(int textureId, int textureWidth, int textureHeight) {
        if (currentFrame != null) {
            uploadBufferedImageToTexture(currentFrame, textureId, textureWidth, textureHeight);
        }
    }

    /**
     * Загружает BufferedImage в OpenGL-текстуру.
     */
    private void uploadBufferedImageToTexture(BufferedImage image, int textureId, int textureWidth, int textureHeight) {
        // Создаем пустое изображение для текстуры с поддержкой прозрачности
        BufferedImage textureImage = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = textureImage.createGraphics();

        // Очищаем изображение прозрачным фоном
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, textureWidth, textureHeight);
        g.setComposite(AlphaComposite.SrcOver);

        // Используем Math.max для масштабирования с покрытием (cover):
        // изображение масштабируется так, чтобы заполнить текстуру полностью, а его части выходящие за границы обрезаются.
        double scale = Math.max((double) textureWidth / image.getWidth(), (double) textureHeight / image.getHeight());
        int scaledWidth = (int) Math.round(image.getWidth() * scale);
        int scaledHeight = (int) Math.round(image.getHeight() * scale);

        // Вычисляем координаты, чтобы изображение было отцентрировано.
        // Если scaledWidth/Height больше, чем размеры текстуры, то x и/или y будут отрицательными – это нормально.
        int x = (textureWidth - scaledWidth) / 2;
        int y = (textureHeight - scaledHeight) / 2;

        // Настройка качества масштабирования
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        // Рисуем изображение с масштабированием и центрированием (лишние части автоматически обрезаются)
        g.drawImage(image, x, y, scaledWidth, scaledHeight, null);
        g.dispose();

        // Получаем данные пикселей из итогового изображения текстуры
        byte[] pixelData = ((DataBufferByte) textureImage.getRaster().getDataBuffer()).getData();

        // Преобразуем данные из ABGR в RGBA (OpenGL ожидает RGBA)
        byte[] rgbaData = new byte[pixelData.length];
        for (int i = 0; i < pixelData.length; i += 4) {
            byte a = pixelData[i];
            byte b = pixelData[i + 1];
            byte gByte = pixelData[i + 2];
            byte r = pixelData[i + 3];
            rgbaData[i]     = r;
            rgbaData[i + 1] = gByte;
            rgbaData[i + 2] = b;
            rgbaData[i + 3] = a;
        }

        ByteBuffer buffer = ByteBuffer.allocateDirect(rgbaData.length);
        buffer.order(ByteOrder.nativeOrder());
        buffer.put(rgbaData);
        buffer.flip();

        // Обновляем текстуру с заданными размерами
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, textureWidth, textureHeight,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
    }



    /**
     * Обновляет m3u8-ссылку для потока.
     * При вызове метод останавливает текущий декодер, создаёт новый FFmpegFrameGrabber
     * с новым URL, устанавливает таймстамп на текущее время воспроизведения и перезапускает декодирование.
     *
     * @param newUrl новый m3u8 URL
     */
    public synchronized void updateStreamUrl(String newUrl) {
        // Останавливаем текущий процесс декодирования
        running = false;
        try {
            if (decodeThread != null) {
                decodeThread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            frameGrabber.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Обновляем URL и создаём новый grabber
        this.streamUrl = newUrl;
        frameGrabber = new FFmpegFrameGrabber(newUrl);
        try {
            frameGrabber.start();
            // Продолжаем с сохранённой позицией воспроизведения
            frameGrabber.setTimestamp((long) (currentTime * 1e6));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        running = true;
        decodeThread = new Thread(this::decodeLoop);
        decodeThread.start();
    }
}
