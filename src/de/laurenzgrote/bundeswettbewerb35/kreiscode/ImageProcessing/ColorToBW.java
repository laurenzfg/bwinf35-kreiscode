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
        // +Durchschnitt mitberechnen
        double avgBrightness = 0;

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
                avgBrightness += lineaerLuminance;
                brightness[x][y] = lineaerLuminance;
            }
        }

        // Durchschnitt berechnen
        avgBrightness /= (double) (width*height);

        // Schärfen des Bildes
        double[][] sharpenedBrightness = schaerfe(brightness);
        // Treshold für die S/W ist dunkler als 3/4 d. Durchschnitts
        double treshhold = avgBrightness * 0.75;
        boolean[][] swImage = new boolean[width][height];
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (sharpenedBrightness[x][y] < treshhold)
                    swImage[x][y] = true;

        // + Bildvervollständigung
        return vervollstaendige(swImage);
    }

    /**
     * Schärt das Bild. Jedes Pixeln bekommt als Helligkeit den Durschnitt
     * aus der eigenen Helligkeit und der Helligkeit der Umgebungspixel.
     * @param in Ausgangsbild
     * @return geschärftes Bild
     */
    private double[][] schaerfe(double[][] in) {
        double[][] newBrightness = new double[width][height];
        // Randreihe wird nicht beachtet
        // Billiges Anti-Out-Of-Bounds
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                double avg = newBrightness[x][y];
                for (int x1 = x - 1; x1 <= x + 1; x1++)
                    for (int y1 = y - 1; y1 <= y + 1; y1++)
                        avg += in[x1][y1];
                avg /= 10.0;
                newBrightness[x][y] = avg;
            }
        }
        return newBrightness;
    }

    /**
     * Vervollständigt das S/W-Bild
     */
    private boolean[][] vervollstaendige(boolean[][] in) {
        // Zählen der Vervollständigten Pixel
        int cnt;
        // Es wird solange wiederholt, bis keine Vervollständigung mehr stattfinden kann
        do {
            cnt = 0;
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
                        if (adj >= 4) {
                            in[x][y] = true;
                            cnt++;
                        }
                    }
                }
            }
        } while (cnt > 0);

        return in;
    }
}
