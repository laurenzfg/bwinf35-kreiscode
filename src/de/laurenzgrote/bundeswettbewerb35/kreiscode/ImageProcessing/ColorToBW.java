package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ColorToBW {
    // Buntes Bild
    private BufferedImage rgbImage;
    private int width, height;

    public static boolean[][] colorToBW (BufferedImage rgbImage) {
        return new ColorToBW(rgbImage).generateSW();
    }

    private ColorToBW(BufferedImage rgbImage) {
        this.rgbImage = rgbImage;
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();
    }

    /**
     * Generiert S/W-Bild aus buntem Bild
     */
    private boolean[][] generateSW () {
        // Helligkeitswert für jeden Pixel berechnen
        double[][] brightness = new double[width][height];

        // Für jedes Pixel...
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // ... RGB-Kanalwerte des Pixels (0,0,0 Schwarz, 255,255,255 Weiß) ermitteln
                Color color = new Color(rgbImage.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                // Helligkeit auf einer linearen Skala. Formel:
                // https://en.wikipedia.org/wiki/Grayscale#Colorimetric_.28luminance-preserving.29_conversion_to_grayscale
                double lineaerLuminance = r*0.2126 + g*0.7152 + b*0.0722;

                // Helligkeit abspeichern + zum Durchschnitt dazuaddieren
                // Double lässt maximal ein Bild mit 1.7977*10^308 (DOUBLE_MAX/255) Pixeln zu. Is genug.
                brightness[x][y] = lineaerLuminance;
            }
        }

        // Schärfen des Bildes
        boolean[][] swImage = schaerfe(brightness);

        // + Bildvervollständigung
        return vervollstaendige(swImage);
    }

    /**
     * Schärt das Bild. Jedes Pixeln bekommt als Helligkeit den Durschnitt
     * aus der eigenen Helligkeit und der Helligkeit der Umgebungspixel.
     * @param in Ausgangsbild
     * @return geschärftes Bild
     */
    private boolean[][] schaerfe(double[][] in) {
        double[][] newBrightness = new double[width][height];
        double avgBrightness = 0.0;
        // Randreihe wird nicht beachtet
        // Billiges Anti-Out-Of-Bounds
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                double avg = in[x][y];
                for (int x1 = x - 1; x1 <= x + 1; x1++)
                    for (int y1 = y - 1; y1 <= y + 1; y1++)
                        avg += in[x1][y1];
                avg /= 10.0;
                newBrightness[x][y] = (in[x][y] + avg) / 2.0;
                avgBrightness += newBrightness[x][y];
            }
        }
        avgBrightness /= (double) (width*height);

        // Treshold für die S/W ist dunkler als 3/4 d. Durchschnitts
        double treshhold = avgBrightness * 0.75;
        boolean[][] swImage = new boolean[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (newBrightness[x][y] <= treshhold)
                    swImage[x][y] = true;

        return swImage;
    }

    /**
     * Vervollständigt das S/W-Bild
     */
    private boolean[][] vervollstaendige(boolean[][] in) {
        int width = in.length;
        int height = in[0].length;

        boolean[][] out = new boolean[width][height];

        for (int minAdj = 4; minAdj < 10; minAdj++){
            // Jedes Feld, dass mehr als 4 Schwarze Nachabrn hat ist wahrscheinlich auch Schwarz
            // --> Schwarz färben
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    // Für True-Felder muss keine Vervollständigung stattfinden
                    if (!in[x][y]) {
                        int adj = -1; // Startfeld soll nicht mitgezählt werden
                        for (int x1 = x - 1; x1 <= x + 1; x1++)
                            for (int y1 = y - 1; y1 <= y + 1; y1++)
                                if (in[x1][y1]) adj++;
                        if (adj >= minAdj) {
                            out[x][y] = true;
                        }
                    } else {
                        out[x][y] = true;
                    }
                }
            }
            in = out;
            out = new boolean[width][height];
        }

        return in;
    }
}
