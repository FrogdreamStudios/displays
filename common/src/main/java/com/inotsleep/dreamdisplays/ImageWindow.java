package com.inotsleep.dreamdisplays;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

/**
 * Окно для вывода одного кадра VideoGrabber-а.
 * Можно передавать либо уже готовый BufferedImage,
 * либо Frame — он будет конвертирован при отрисовке.
 */
public class ImageWindow extends JFrame {
    private final ImagePanel panel;

    public ImageWindow(String title, int width, int height) {
        super(title);
        panel = new ImagePanel();
        panel.setPreferredSize(new Dimension(width, height));
        add(panel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    /**
     * Устанавливает новый BufferedImage (готовый к рисованию).
     * Обычно конвертация Frame→BufferedImage делается до вызова этого метода.
     */
    public void setImage(BufferedImage img) {
        SwingUtilities.invokeLater(() -> {
            panel.setImage(img);
        });
    }

    /**
     * Устанавливает новый кадр Frame из JavaCV.
     * Конвертация происходит внутри invokeLater, в UI-потоке.
     */
    public void setFrame(Frame frame) {
        SwingUtilities.invokeLater(() -> {
            Java2DFrameConverter converter = panel.getConverter();
            BufferedImage img = converter.getBufferedImage(frame, 1.0, true, ColorSpace.getInstance(ColorSpace.CS_sRGB));
            if (img != null) panel.setImage(img);
        });
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage image;
        private final Java2DFrameConverter converter = new Java2DFrameConverter();

        public Java2DFrameConverter getConverter() {
            return converter;
        }

        public void setImage(BufferedImage img) {
            this.image = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                // Пропорционально вписать в панель
                int panelW = getWidth(), panelH = getHeight();
                int imgW = image.getWidth(), imgH = image.getHeight();
                double scale = Math.min((double)panelW/imgW, (double)panelH/imgH);
                int w = (int)(imgW*scale), h = (int)(imgH*scale);
                int x = (panelW - w)/2, y = (panelH - h)/2;
                g.drawImage(image, x, y, w, h, this);
            }
        }
    }
}
