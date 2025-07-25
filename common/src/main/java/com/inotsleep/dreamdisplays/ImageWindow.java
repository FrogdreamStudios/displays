package com.inotsleep.dreamdisplays;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

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
        if (img == null) {
            return;
        }
        SwingUtilities.invokeLater(() -> panel.setImage(img));
    }

    private static class ImagePanel extends JPanel {
        private final AtomicReference<BufferedImage> imageRef = new AtomicReference<>();

        void setImage(BufferedImage img) {
            imageRef.set(img);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage image = imageRef.get();
            if (image == null) return;

            int panelW = getWidth(), panelH = getHeight();
            int imgW = image.getWidth(), imgH = image.getHeight();

            double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
            int w = (int) (imgW * scale);
            int h = (int) (imgH * scale);
            int x = (panelW - w) / 2;
            int y = (panelH - h) / 2;

            g.drawImage(image, x, y, w, h, this);
        }
    }
}
