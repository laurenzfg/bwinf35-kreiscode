package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import de.laurenzgrote.bundeswettbewerb35.kreiscode.Coordinate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Stack;

public class ColorToBW {
    private int width, height;

    // Binärbild
    private boolean[][] swImage;

    // Durchschnittswerte
    private double avgBrightness = 0.0;
    private double avgScharr = 0.0;

    public static boolean[][] colorToBW (BufferedImage rgbImage) {
        return new ColorToBW(rgbImage).getSWImage();
    }

    private ColorToBW(BufferedImage rgbImage) {
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();

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

                // Helligkeit auf einer linearen Skala bestimmen. Formel:
                // https://en.wikipedia.org/wiki/Grayscale#Colorimetric_.28luminance-preserving.29_conversion_to_grayscale
                double lineaerLuminance = r*0.2126 + g*0.7152 + b*0.0722;

                // Helligkeitswert abspeichern
                brightness[x][y] = lineaerLuminance;
                avgBrightness += lineaerLuminance; // Counter für arithm. Mittel
            }
        }
        // Arithm. Mittel der Helligkeit berechnen
        avgBrightness /= width*height;

        // Entrauschen durch Gauß-Filter
        double[][] gaussImage = gaussianFilter(brightness);
        // Kantenerkennung
        double[][] scharrImage = scharrFilter(gaussImage);

        // Speichern des Binärbildes
        swImage = toBinary(scharrImage, gaussImage);
    }

    private boolean[][] getSWImage() {
        return swImage;
    }

    /**
     * Weichzeichnen mit Gauß: http://homepages.inf.ed.ac.uk/rbf/HIPR2/gsmooth.htm
     * @param in Ausgangsbild
     * @return gegaußtetes Bild
     */
    private double[][] gaussianFilter(double[][] in) {
        // Gaßsche Normalverteilung. Sigma: 5
        // Berechnungstool: http://dev.theomader.com/gaussian-kernel-calculator/
        final double kernel[] =  {0.192077,0.203914,0.208019,0.203914,0.192077};
        // Abstand, der zur Seite zur Berechnung benötigt wird
        final int kernelGap = (kernel.length - 1) / 2; // ==2

        // Anwendung des Filters in horizontale Richung:
        double[][] hGauss = new double[width][height];

        for (int x = 0; x < width; x++) {
            // An den Rändern Platz lassen, da dort der Kernel nicht "hineinpasst"
            for (int y = kernelGap; y < height - kernelGap; y++) {
                double newVal = 0.0;    // Neuer Grauwert nach Weichzeichnung
                int i = 0;              // Counter für Position im Kernel
                // Einfügen der umliegenden Felder nach Gewicht
                for (int y1 = y - kernelGap; y1 <= y + kernelGap; y1++) {
                    newVal += in[x][y1] * kernel[i];
                    i++;
                }
                // Speichern des neuen Grauwertes
                hGauss[x][y] = newVal;
            }
        }

        // Anwendung des Filters in vertikale Richung:
        double[][] vGauss = new double[width][height];

        for (int y = 0; y < height; y++) {
            // An den Rändern Platz lassen, da dort der Kernel nicht "hineinpasst"
            for (int x = kernelGap; x < width - kernelGap; x++) {
                double newVal = 0.0;    // Neuer Grauwert nach Weichzeichnung
                int i = 0;              // Counter für Position im Kernel
                // Ausrechnen der Kerne mit Convulution (http://homepages.inf.ed.ac.uk/rbf/HIPR2/convolve.htm)
                for (int x1 = x - kernelGap; x1 <= x + kernelGap; x1++) {
                    newVal += hGauss[x1][y] * kernel[i]; // Einfügen des Wertes
                    i++;                                 // Nächste Position im Kernel
                }
                // Speichern des neuen Grauwertes
                vGauss[x][y] = newVal;
            }
        }

        // Randwerte sind nicht korrekt gemittelt worden, dort wird die Ausgangsfarbe eingefügt
        for (int y = 0; y < height; y++) {
            // Obere/Untere Zeilen oder nur die Bande Rechts und Links
            if (y < kernelGap || y >= height - kernelGap) {
                // Oben/Unten
                for (int x = 0; x < width; x++)
                    vGauss[x][y] = in[x][y];
            } else {
                // Rechts + Links
                for (int x = 0; y < kernelGap; x++)
                    vGauss[x][y] = in[x][y];
                for (int x = width - kernelGap; x < width; x++)
                    vGauss[x][y] = in[x][y];
            }
        }

        return vGauss;
    }


    /**
     * Berechnung der Ableitung über den Bildkontrast mit dem Scharr-Operator
     * http://homepages.inf.ed.ac.uk/rbf/HIPR2/sobel.htm
     * @param in
     * @return Ergebnis des Sobel-Operators
     */
    private double[][] scharrFilter(double[][] in) {
        // Scharr-Kernel
        // (Die .ac.uk-Seite verwendet den älteren Sobel-Operator,
        //  der Scharr-Operator ist aber bei kleinen Kerneln besser:
        //  http://docs.opencv.org/2.4/doc/tutorials/imgproc/imgtrans/sobel_derivatives/sobel_derivatives.html#

        // Kernel für vertikale Ableitung:
        final double[] vertKernel = {-3.0, -10.0, -3.0,
                0.0, 0.0, 0.0,
                3.0, 10.0, 3.0};
        // Kernel für horizontale Ableitung:
        final double[] horizKernel = {-3.0, 0.0, 3.0,
                -10.0, 0.0, 10.0,
                -3.0, 0.0, 3.0};
        // Abstand, der zur Seite zur Berechnung benötigt wird
        final int kernelGap = (int) ((Math.sqrt(vertKernel.length) - 1) / 2); // ==1

        // Ergebniss d. Ableitungsfunktionen des Graphens in beide Richtungen
        double[][] vertScharr = new double[width][height];
        double[][] horizScharr = new double[width][height];

        for (int x = kernelGap; x < width - kernelGap; x++) {
            for (int y = kernelGap; y < height - kernelGap; y++) {
                int maskPos = 0;    // Counter für Position im Kernel
                double vert = 0.0; // Vertikaler Ableitungswert
                double horiz = 0.0; // Horizontaler Ableitungswert
                // Ausrechnen der Kerne mit Convulution (http://homepages.inf.ed.ac.uk/rbf/HIPR2/convolve.htm)
                for (int x1 = x - 1; x1 <= x + 1; x1++) {
                    for (int y1 = y - 1; y1 <= y + 1; y1++) {
                        // Laden der relevanten Multiplikatoren
                        double vertMultiplier = vertKernel[maskPos];
                        double horizMultiplier = horizKernel[maskPos];
                        // Einfügen der entsprechenden Werte
                        vert += in[x1][y1] * vertMultiplier;
                        horiz += in[x1][y1] * horizMultiplier;
                        maskPos++; // Nächste Position im Kernel
                    }
                }
                // Speichern der berechneten Werte
                vertScharr[x][y] = vert;
                horizScharr[x][y] = horiz;
            }
        }

        // Vereinigen der beiden Ableitungen
        double[][] combScharr = new double[width][height];
        for (int x = kernelGap; x < width - kernelGap; x++) {
            for (int y = kernelGap; y < height - kernelGap; y++) {
                // Laden der beiden Ableitungswerte zu (x, y)
                double vert = vertScharr[x][y];
                double horiz = horizScharr[x][y];
                // Formel
                double val = Math.sqrt(vert * vert + horiz * horiz);
                // Speichern
                combScharr[x][y] = val;
                avgScharr += val;
            }
        }
        avgScharr /= (width - 2 * kernelGap) * (height - 2 * kernelGap); // Mitteln

        // Auch hier gibts wieder unebachtete Randwerte
        // Hier setze ich dann einen hohen Wert ein, damit im nächsen Schritt ein schwarzer Rand entsteht
        for (int y = 0; y < height; y++) {
            // Obere/Untere Zeilen oder nur die Bande Rechts und Links
            if (y < kernelGap || y >= height - kernelGap) {
                // Oben/Unten
                for (int x = 0; x < width; x++)
                    combScharr[x][y] = Integer.MAX_VALUE;
            } else {
                // Rechts + Links
                for (int x = 0; y < kernelGap; x++)
                    combScharr[x][y] = Integer.MAX_VALUE;
                for (int x = width - kernelGap; x < width; x++)
                    combScharr[x][y] = Integer.MAX_VALUE;
            }
        }

        return combScharr;
    }
    private boolean[][] toBinary(double[][] scharrImage, double[][] gaussImage) {
        // Binärisieren mit Schwellwert
        boolean[][] binaryImage = new boolean[width][height];
        double treshhold = Math.max(200, 1.5*avgScharr); // Treshhold durch abschätzen ermittelt :D
        Stack<Coordinate> hysteresisStack = new Stack<>();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (scharrImage[x][y] > treshhold) {
                    binaryImage[x][y] = true;
                    hysteresisStack.push(new Coordinate(x, y));
                }

            }
        }

        // Hysterese mit Vervollständigung
        treshhold = Math.max(100, avgScharr); // Treshhold durch abschätzen ermittelt :D
        while (!hysteresisStack.empty()) {
            Coordinate c = hysteresisStack.pop();
            int x = c.getX(); int y = c.getY();

            // Links
            x -= 1;
            if (x >= 0 && !binaryImage[x][y] && scharrImage[x][y] > treshhold) {
                binaryImage[x][y] = true;
                hysteresisStack.push(new Coordinate(x, y));
            }
            // Rechts
            x += 2;
            if (x < width && !binaryImage[x][y] && scharrImage[x][y] > treshhold) {
                binaryImage[x][y] = true;
                hysteresisStack.push(new Coordinate(x, y));
            }
            // Oben
            x -= 1; y -= 1;
            if (y >= 0 && !binaryImage[x][y] && scharrImage[x][y] > treshhold) {
                binaryImage[x][y] = true;
                hysteresisStack.push(new Coordinate(x, y));
            }
            // Unten
            y += 2;
            if (y < height && !binaryImage[x][y] && scharrImage[x][y] > treshhold) {
                binaryImage[x][y] = true;
                hysteresisStack.push(new Coordinate(x, y));
            }
        }

        return binaryImage;
    }
}
