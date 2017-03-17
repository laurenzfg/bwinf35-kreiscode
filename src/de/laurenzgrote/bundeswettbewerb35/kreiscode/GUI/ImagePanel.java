package de.laurenzgrote.bundeswettbewerb35.kreiscode.GUI;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

class ImagePanel extends JPanel implements ComponentListener{
    // Div. Farben
    private final Color BLACK = Color.BLACK;
    private final Color WHITE = Color.WHITE;
    private final Color highlightColor = Color.MAGENTA;
    private final Color GREEN_BG = new Color(0,128,0,220);
    private final Color textColor = new Color(128,0,128);

    private class DrawingDirective {
        // Enum wäre hier besser, aber ich wollte für 
        // die GUI den Footprint klein halten
        int type; // 0:crosshair 1: text
        int x;
        int y;

        Color color; // ggfs. Farbe
        String s; // ggfs. String
    }

    // Drawing-Pipeline
    private ArrayList<DrawingDirective> drawingDirectives = new ArrayList<>();

    private BufferedImage unscaledImage;
    private Image scaledImage;
    private int lastWidth = Integer.MIN_VALUE;
    private double scalFactor = 0.0;

    // Prozedur um eine Bitmap als Hintergrund zu wählen
    void setImage(BufferedImage unscaledImage) {
        this.unscaledImage = unscaledImage;

        lastWidth = Integer.MIN_VALUE; // New Image --> Scaling needed
        this.repaint();
    }

    // Prozedur um eine S/W-Bitmap als Hintergrund zu wählen
    void setImage(boolean[][] feld) {
        int width = feld.length;
        // Wir nehmen an, dass alle Spalten u.Zeilen gleich lang sind,
        // das feld[][] also ein Rechteck repräsentiert
        int height = feld[0].length;

        unscaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < feld.length; x++) {
            for (int y = 0; y < feld[0].length; y++) {
                if (feld[x][y]) {
                    unscaledImage.setRGB(x, y, BLACK.getRGB());
                } else {
                    unscaledImage.setRGB(x, y, WHITE.getRGB());
                }
            }
        }

        lastWidth = Integer.MIN_VALUE; // Neues Bild muss auf Canvas-Größe skaliert werden
        this.repaint(); // Lass ma' zeichnen
    }

    private void addDirective(int type, int x, int y, Color color, String s) {
        DrawingDirective dD = new DrawingDirective();
        dD.type=type;
        dD.x = x;
        dD.y = y;
        dD.color = color;
        dD.s =s;
        drawingDirectives.add(dD);
    }

    // Highlight Pixel geht nicht über die Pipe sondern direkt in die Bitmap
    public void highlightPixel (int x, int y) {
        if (x < unscaledImage.getWidth() && x >= 0 && y < unscaledImage.getHeight() && y >= 0) {
            unscaledImage.setRGB(x, y, highlightColor.getRGB());
        }
    }
    public void addCrosshair (int x, int y) {
        addDirective(0, x, y, highlightColor, null);
    }
    public void addText (int x, int y, String s) {
        addDirective(1, x, y, null, s);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        // Setzen des Fonts
        graphics.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));

        if (unscaledImage != null) {
            int maxWidth = getWidth();
            if (maxWidth != lastWidth) {
                // TODO: Bei ungewöhnlichem Seitenverhältnis height dann zu groß!
                scaledImage = unscaledImage.getScaledInstance(maxWidth, -1, Image.SCALE_SMOOTH);
                System.err.println("Scaled image");
                lastWidth = maxWidth;
                scalFactor = ((double) maxWidth) / ((double) unscaledImage.getWidth());
            }

            graphics.drawImage(scaledImage, 0, 0, this);

            // Malen der Pipeline
            for (DrawingDirective dD: drawingDirectives) {
                int x = (int) Math.round((double) dD.x * scalFactor);
                int y = (int) Math.round((double) dD.y * scalFactor);
                switch (dD.type) {
                    case 0:
                        // Crosshair
                        graphics.setColor(dD.color);
                        graphics.fillRect(x-8, y-1, 16,2);
                        graphics.fillRect(x-1, y-8, 2,16);
                        break;
                    case 1:
                        // String
                        // Box
                        graphics.setColor(GREEN_BG);
                        graphics.fillRect(x-10, y-10, 20,20);
                        graphics.setColor(textColor);
                        graphics.drawString(dD.s, x-(dD.s.length()*5), y+5);
                        break;
                }
            }
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
