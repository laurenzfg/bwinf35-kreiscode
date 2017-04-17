package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

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

                // Helligkeit abspeichern
                brightness[x][y] = lineaerLuminance;
            }
        }

        // Schärfen und Binärisieren des Bildes
        boolean[][] swImage = medianFilter(brightness);

        // + Bildvervollständigung
        return vervollstaendige(swImage);
    }

    /**
     * Schärt das Bild mit einem Medianfilter. https://en.wikipedia.org/wiki/Median_filter
     * @param in Ausgangsbild
     * @return geschärftes Bild
     */
    private boolean[][] medianFilter(double[][] in) {
        final int mask[] = {1,1,1,1,1,
                            1,2,3,2,1,
                            1,3,4,3,1,
                            1,2,3,2,1,
                            1,1,1,1,1};

        double[][] newBrightness = new double[width][height];

        // Randreihe wird nicht beachtet
        // Billiges Anti-Out-Of-Bounds
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                double medianArray[] = new double[40]; // s. Maske in Doku
                int pos = 0;
                for (int x1 = x - 2; x1 <= x + 2; x1++) {
                    for (int y1 = y - 2; y1 <= y + 2; y1++) {
                        int multiplier = mask[pos];
                        for (int i = 0; i < multiplier; i++) {
                            medianArray[pos + i] = in[x][y];
                        }
                        pos++;
                    }
                }
                Arrays.sort(medianArray);
                newBrightness[x][y] = medianArray[19]; // 20 -> 19 (Nullindizierung)
            }
        }

        // https://de.wikipedia.org/wiki/Canny-Algorithmus
        boolean[][] swImage = new boolean[width][height];
        for (int x = 1; x < width-1; x++) {
            for (int y = 1; y < height - 1; y++) {
                double fieldLuminance = newBrightness[x][y];
                double surroundingLuminance = 0.0;

                int pos = 0;
                for (int x1 = x - 1; x1 <= x + 1; x1++) {
                    for (int y1 = y - 1; y1 <= y + 1; y1++) {
                        if (x1 != x || y1 != y) {
                            surroundingLuminance += newBrightness[x1][y1];
                            pos++;
                        }
                    }
                }

                surroundingLuminance /= 8.0;
                if (Math.abs(fieldLuminance - surroundingLuminance) < 15.0)
                    swImage[x][y] = true;
                }
        }

        return swImage;
    }

    /**
     * Vervollständigt das S/W-Bild
     */
    private boolean[][] vervollstaendige(boolean[][] in) {
        int width = in.length;
        int height = in[0].length;

        boolean[][] out = new boolean[width][height];

        for (int i = 0; i < 100; i++){
            int cnt = 0;
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    // Für True-Felder muss keine Vervollständigung stattfinden
                    if (!in[x][y]) {
                        int adj = -1; // Startfeld soll nicht mitgezählt werden
                        for (int x1 = x - 1; x1 <= x + 1; x1++)
                            for (int y1 = y - 1; y1 <= y + 1; y1++)
                                if (in[x1][y1]) adj++;
                        if (adj > 4) {
                            out[x][y] = true;
                            cnt++;
                        }
                    } else {
                        out[x][y] = true;
                    }
                }
            }
            in = out;
            out = new boolean[width][height];
            if (cnt == 0)
                break;
        }

        return in;
    }
}
