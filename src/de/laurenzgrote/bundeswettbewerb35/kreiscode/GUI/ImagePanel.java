package de.laurenzgrote.bundeswettbewerb35.kreiscode.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

class ImagePanel extends JPanel implements ComponentListener{

    private final int trueColor = Color.BLACK.getRGB();
    private final int falseColor = Color.WHITE.getRGB();
    private final int overlayColor = Color.MAGENTA.getRGB();

    private BufferedImage unscaledImage;
    private Image scaledImage;
    private double lastWidth = Double.NEGATIVE_INFINITY;

    void setImage(BufferedImage unscaledImage) {
        this.unscaledImage = unscaledImage;

        lastWidth = Double.NEGATIVE_INFINITY; // New Image --> Scaling needed
        this.repaint();
    }

    void setImage(boolean[][] feld) {
        unscaledImage = new BufferedImage(feld.length, feld[0].length, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < feld.length; x++) {
            for (int y = 0; y < feld[0].length; y++) {
                if (feld[x][y]) {
                    unscaledImage.setRGB(x, y, trueColor);
                } else {
                    unscaledImage.setRGB(x, y, falseColor);
                }
            }
        }

        lastWidth = Double.NEGATIVE_INFINITY; // New Image --> Scaling needed
        this.repaint();
    }

    void markCoordinate (int x, int y) {
        for (int i = x - 10; i <= x + 10; i++) {
            for (int j = y - 2; j <= y + 2; j++) {
                highlightPixel(i, j);
            }
        }
        for (int i = y - 10; i <= y + 10; i++) {
            for (int j = x - 2; j <= x + 2; j++) {
                highlightPixel(j, i);
            }
        }
    }
    void highlightPixel (int x, int y) {
        if (x < unscaledImage.getWidth() && x >= 0 && y < unscaledImage.getHeight() && y >= 0)
            unscaledImage.setRGB(x, y, overlayColor);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (unscaledImage != null) {
            double maxWidth = getWidth() - 20;
            if (maxWidth != lastWidth) {
                // TODO: Bei ungewöhnlcihem Seitenverhältnis height dann zu groß!
                scaledImage = unscaledImage.getScaledInstance((int) maxWidth, -1, Image.SCALE_SMOOTH);
                System.err.println("Scaled image");
                lastWidth = maxWidth;
            }

            graphics.drawImage(scaledImage, 10, 10, this);
        }
    }

    @Override
    public void componentResized(ComponentEvent componentEvent) {
        this.repaint();
    }

    @Override
    public void componentMoved(ComponentEvent componentEvent) {
    }

    @Override
    public void componentShown(ComponentEvent componentEvent) {

    }

    @Override
    public void componentHidden(ComponentEvent componentEvent) {

    }
}
