package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class ColorToBW {
    // Buntes Bild
    private BufferedImage rgbImage;
    private int width, height;

    private double avgBrightness = 0.0; // Wird später erfasst

    public static boolean[][] colorToBW (BufferedImage rgbImage) {
        return new ColorToBW(rgbImage).generateSW();
    }

    private ColorToBW(BufferedImage rgbImage) {
        this.rgbImage = rgbImage;
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();
    }

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
                avgBrightness += lineaerLuminance; // Counter für arithm. Mittel
            }
        }
        // Mittel berechnen
        avgBrightness /= width*height;

        // Entrauschen durch Median
        double[][] medianFilteredImage = medianFilter(brightness);
        // Kantenerkennung und ausfüllen der Flächen
        boolean[][] scharrImage = scharrFilter(medianFilteredImage);

        return scharrImage;
    }

    /**
     * Schärt das Bild mit einem Medianfilter. https://en.wikipedia.org/wiki/Median_filter
     * @param in Ausgangsbild
     * @return geschärftes Bild
     */
    private double[][] medianFilter(double[][] in) {
        // Gewichtung des Pixels und seiner Umgebung
        final double mask[] =  {1.0, 1.0, 1.0, 1.0, 1.0,
                                1.0, 2.0, 3.0, 2.0, 1.0,
                                1.0, 3.0, 4.0, 3.0, 1.0,
                                1.0, 2.0, 3.0, 2.0, 1.0,
                                1.0, 1.0, 1.0, 1.0, 1.0};
        // Ausgabe der Funktion
        double[][] medianFilteredImage = new double[width][height];

        // Randreihe wird nicht beachtet, damit kein OutOfBounds entsteht
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                // Werte für Median
                double medianArray[] = new double[40];
                // Position in der 1D-Maske
                int maskPos = 0;
                // Für jedes Pixel um das Pixel (x, y):
                for (int x1 = x - 2; x1 <= x + 2; x1++) {
                    for (int y1 = y - 2; y1 <= y + 2; y1++) {
                        // Sooft in die Liste wie die maske bestimmt!
                        double multiplier = mask[maskPos];
                        for (int i = 0; i < multiplier; i++) {
                            medianArray[maskPos + i] = in[x][y];
                        }
                        maskPos++;
                    }
                }

                // Sortieren und Median in die Ausgabe schreiben
                Arrays.sort(medianArray);
                medianFilteredImage[x][y] = medianArray[19]; // 20 -> 19 (Nullindizierung)
            }
        }

        return medianFilteredImage;
    }


    /**
     * http://docs.opencv.org/2.4/doc/tutorials/imgproc/imgtrans/sobel_derivatives/sobel_derivatives.html
     * @param in
     * @return Ergebnis des Sobel-Operators
     */
    private boolean[][] scharrFilter(double[][] in) {
        // Scharr-Masken
        final double[] vertMask     = {-3.0, -10.0, -3.0,
                                        0.0, 0.0  , 0.0,
                                        3.0, 10.0 , 3.0};
        final double[] horizMask    = {-3.0, 0.0 , 3.0,
                                       -10.0,0.0 ,10.0,
                                       -3.0, 0.0 , 3.0};

        // Ableitungsfunktionen des Graphens in beide Richtungen
        double[][] vertScharr = new double[width][height];
        double[][] horizScharr = new double[width][height];

        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                int maskPos = 0; // Pos. in 1D-Maske
                double vert =  0.0; // Werte der Ableitung
                double horiz = 0.0;
                // Umliegende Pixel um (x, y)
                for (int x1 = x - 1; x1 <= x + 1; x1++) {
                    for (int y1 = y - 1; y1 <= y + 1; y1++) {
                        // Laden der relevanten Multiplier
                        double vertMultiplier  = vertMask[maskPos];
                        double horizMultiplier = horizMask[maskPos];
                        // Und enstprechen hinaufrechnen
                        vert  += in[x1][y1] * vertMultiplier;
                        horiz += in[x1][y1] * horizMultiplier;
                        maskPos++;
                    }
                }
                // Speichern der berechneten Werte
                vertScharr[x][y] = vert;
                horizScharr[x][y] = horiz;
            }
        }

        // Vereinigen der beiden Ableitungen
        double[][] combScharr = new double[width][height];
        double avgScharr = 0.0;
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                // Laden der beiden Ableitungswerte zu (x, y)
                double vert  = vertScharr[x][y];
                double horiz = horizScharr[x][y];
                // Formel
                double val = Math.sqrt(vert * vert + horiz * horiz);
                // Speichern in Variable und Durchschnittscounter
                combScharr[x][y] = val;
                avgScharr += val;
            }
        }
        avgScharr /= width*height; // Mitteln

        boolean[][] binaryImage = new boolean[width][height];
        double treshhold = Math.max(75, 1.5*avgScharr);
        // Am Rand sind "komische" Werte
        for (int x = 3; x < width-3; x++) {
            for (int y = 3; y < height - 3; y++) {
                if (combScharr[x][y] > treshhold)
                    binaryImage[x][y] = true;
            }
        }

        // Erweitern
        /*for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                if (binaryImage[x][y]) {
                    double color = in[x][y];

                    Stack<Coordinate> ffStack = new Stack<>();
                    ffStack.push(new Coordinate(x - 1, y));
                    ffStack.push(new Coordinate(x + 1, y));
                    ffStack.push(new Coordinate(x, y - 1));
                    ffStack.push(new Coordinate(x, y + 1));

                    while(!ffStack.empty()) {
                        Coordinate c = ffStack.pop();
                        int x1 = c.getX();
                        int y1 = c.getY();
                        if (!binaryImage[x1][y1] && in[x1][y1] < avgBrightness && combScharr[x1][y1] < treshhold) {
                            binaryImage[x1][y1] = true;

                            if (x1 - 1 >= 0)
                                ffStack.push(new Coordinate(x1 - 1, y1));
                            if (x1 + 1 < width)
                                ffStack.push(new Coordinate(x1 + 1, y1));
                            if (y1 - 1 >= 0)
                                ffStack.push(new Coordinate(x1, y1 - 1));
                            if (y1 + 1 < height)
                                ffStack.push(new Coordinate(x1, y1 + 1));
                        }
                    }
                }
            }
        }*/

        return binaryImage;
    }
}
