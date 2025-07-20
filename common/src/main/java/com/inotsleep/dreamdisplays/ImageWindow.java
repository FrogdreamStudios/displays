package com.inotsleep.dreamdisplays;

import javax.swing.*;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

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

    public void setImage(BufferedImage img) {
        SwingUtilities.invokeLater(() -> {
            panel.setImage(img);
        });
    }

    public void setFrame(Frame frame) {
        SwingUtilities.invokeLater(() -> {
            Java2DFrameConverter converter = panel.getConverter();
            BufferedImage img = converter.getBufferedImage(frame, 1.0, false, ColorSpace.getInstance(ColorSpace.CS_sRGB));
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
