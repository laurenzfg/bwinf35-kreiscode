package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import de.laurenzgrote.bundeswettbewerb35.kreiscode.Coordinate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Stack;

public class ColorToBW {
    // Buntes Bild
    private BufferedImage rgbImage;
    private int width, height;
    private double avgBrightness = 0.0;

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
                avgBrightness += lineaerLuminance;
            }
        }

        avgBrightness /= width*height;

        // Schärfen und Binärisieren des Bildes
        double[][] medianFilteredImage = medianFilter(brightness);
        boolean[][] sobelImage = scharr(medianFilteredImage);

        return sobelImage;
        // + Bildvervollständigung
        //return hysterisis(sobelImage);
    }

    /**
     * Schärt das Bild mit einem Medianfilter. https://en.wikipedia.org/wiki/Median_filter
     * @param in Ausgangsbild
     * @return geschärftes Bild
     */
    private double[][] medianFilter(double[][] in) {
        final double mask[] =  {1.0, 1.0, 1.0, 1.0, 1.0,
                                1.0, 2.0, 3.0, 2.0, 1.0,
                                1.0, 3.0, 4.0, 3.0, 1.0,
                                1.0, 2.0, 3.0, 2.0, 1.0,
                                1.0, 1.0, 1.0, 1.0, 1.0};

        double[][] medianFilteredImage = new double[width][height];

        // Randreihe wird nicht beachtet
        // Billiges Anti-Out-Of-Bounds
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                double medianArray[] = new double[40]; // s. Maske in Doku
                int maskPos = 0;
                for (int x1 = x - 2; x1 <= x + 2; x1++) {
                    for (int y1 = y - 2; y1 <= y + 2; y1++) {
                        double multiplier = mask[maskPos];
                        for (int i = 0; i < multiplier; i++) {
                            medianArray[maskPos + i] = in[x][y];
                        }
                        maskPos++;
                    }
                }
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
    private boolean[][] scharr(double[][] in) {
        final double[] vertMask     = {-1.0, -2.0, -1.0,
                                        0.0, 0.0 , 0.0,
                                        1.0, 2.0 , 1.0};
        final double[] horizMask    = {-1.0, 0.0 , 1.0,
                                       -2.0, 0.0 , 2.0,
                                       -1.0, 0.0 , 1.0};

        double[][] vertScharr = new double[width][height];
        double[][] horizScharr = new double[width][height];

        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                int maskPos = 0;
                double vert =  0.0;
                double horiz = 0.0;
                for (int x1 = x - 1; x1 <= x + 1; x1++) {
                    for (int y1 = y - 1; y1 <= y + 1; y1++) {
                        double vertMultiplier  = vertMask[maskPos];
                        double horizMultiplier = horizMask[maskPos];
                        vert  += in[x1][y1] * vertMultiplier;
                        horiz += in[x1][y1] * horizMultiplier;
                        maskPos++;
                    }
                }
                vertScharr[x][y] = vert;
                horizScharr[x][y] = horiz;
            }
        }

        double[][] combScharr = new double[width][height];
        double avgScharr = 0.0;
        for (int x = 2; x < width - 2; x++) {
            for (int y = 2; y < height - 2; y++) {
                double vert  = vertScharr[x][y];
                double horiz = horizScharr[x][y];
                double val = Math.sqrt(vert * vert + horiz * horiz);
                combScharr[x][y] = val;
                avgScharr += val;
            }
        }
        avgScharr /= width*height;

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

/*    private boolean[][] binary(double[][] in) {
        boolean[][] floodFillBorder = new boolean[width][height];

        // "Bande"
        for (int x = 0; x < width; x++) {
            floodFillBorder[x][0]           = true;
            floodFillBorder[x][height-1]    = true;
        }
        for (int y = 0; y < height; y++) {
            floodFillBorder[0][y]           = true;
            floodFillBorder[width-1][y]     = true;
        }

        // Am Rand sind "komische" Werte
        for (int x = 3; x < width-3; x++) {
            for (int y = 3; y < height - 3; y++) {
                if (in[x][y] > 150)
                    floodFillBorder[x][y] = true;
            }
        }

        boolean[][] swImage = new boolean[width][height];

        // Mit TRUE intialisieren
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                swImage[x][y] = true;
            }
        }

        Stack<Coordinate> ffStack = new Stack<>();
        ffStack.push(new Coordinate(1, 1));

*//*        while(!ffStack.empty()) {
            Coordinate c = ffStack.pop();
            int x = c.getX(); int y = c.getY();
            if (!floodFillBorder[x][y]) {
                swImage[x][y] = false;
                floodFillBorder[x][y] = true;

                ffStack.push(new Coordinate(x - 1, y));
                ffStack.push(new Coordinate(x + 1, y));
                ffStack.push(new Coordinate(x, y - 1));
                ffStack.push(new Coordinate(x, y + 1));
            }
        }*//*

        return floodFillBorder;
    }*/

    /**
     * Vervollständigt das S/W-Bild
     */
    private boolean[][] hysterisis(boolean[][] in) {
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
