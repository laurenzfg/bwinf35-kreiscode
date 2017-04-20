package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;

import de.laurenzgrote.bundeswettbewerb35.kreiscode.Coordinate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Stack;

import static java.lang.Math.PI;

/**
 * Eckenerkennung mit dem Canny-Algorithmus und anschließenden Ausfüllung der schwarzen Bildbereiche
 * http://docs.opencv.org/3.1.0/da/d22/tutorial_py_canny.html
 * https://en.wikipedia.org/wiki/Canny_edge_detector
 */
public class EdgeDetector {
    private int width, height;

    // Kanten (Ergebnis von Canny)
    private boolean[][] edgesImage;

    // Ausgefülltes edgesImage
    private boolean[][] swImage;

    /**
     * @param rgbImage Buntes Bild
     */
    public EdgeDetector(BufferedImage rgbImage) {
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();

        // linearen Helligkeitswert für jeden Pixel berechnen (Graustufenbild erzeugen)
        double[][] brightness = new double[width][height];

        // Dafür folgendes für jeden Pixel ausführen
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // ... RGB-Kanalwerte (0,0,0 Schwarz, 255,255,255 Weiß) ermitteln
                Color color = new Color(rgbImage.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();

                // Daraus Helligkeit auf einer linearen Skala bestimmen. Formel:
                // https://en.wikipedia.org/wiki/Grayscale#Colorimetric_.28luminance-preserving.29_conversion_to_grayscale
                brightness[x][y] = r*0.2126 + g*0.7152 + b*0.0722; // 0: Schwarz - 255: Weiß
            }
        }

        // Entrauschen dieses Graustufenbildes durch Gauß-Filter
        double[][] gaussImage = gaussianFilter(brightness);
        // Ableitung bestimmen
        double[][] scharrImage = scharrFilter(gaussImage);
        // Ecken mit Hysterese herausfiltern
        edgesImage = hysteresis(scharrImage);
        // Ausfüllen der Polygone
        swImage = fill(edgesImage);
    }

    /**
     * Weichzeichnen mit Gauß
     * http://homepages.inf.ed.ac.uk/rbf/HIPR2/gsmooth.htm
     * @param in Ausgangsbild
     * @return gegaußtetes Bild
     */
    private double[][] gaussianFilter(double[][] in) {
        // Gaßsche Normalverteilung, Sigma: 3
        // Berechnungstool: http://dev.theomader.com/gaussian-kernel-calculator/
        final double kernel[] =  {0.063327,0.093095,0.122589,0.144599,0.152781,0.144599,0.122589,0.093095,0.063327};
        // Abstand, der zur Seite zur Berechnung benötigt wird
        final int kernelGap = (kernel.length - 1) / 2; // ==2

        // Ergebnis der Convolution in horizontale Richung:
        double[][] hGauss = new double[width][height];

        // Convolution in horizontale Richtung
        for (int x = 0; x < width; x++) {
            // An den Rändern Platz lassen, da dort der Kernel nicht "hineinpasst"
            for (int y = kernelGap; y < height - kernelGap; y++) {
                double newVal = 0.0;    // Neuer Grauwert nach Weichzeichnung
                int i = 0;              // Counter für Position im Kernel
                // Durchführung der Convolution (http://homepages.inf.ed.ac.uk/rbf/HIPR2/convolve.htm)
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
                // Durchführung der Convolution (http://homepages.inf.ed.ac.uk/rbf/HIPR2/convolve.htm)
                for (int x1 = x - kernelGap; x1 <= x + kernelGap; x1++) {
                    newVal += hGauss[x1][y] * kernel[i]; // Einfügen des Wertes
                    i++;                                 // Nächste Position im Kernel
                }
                // Speichern des neuen Grauwertes
                vGauss[x][y] = newVal;
            }
        }

        // Randwerte sind nicht korrekt ermittelt worden, dort werden die Ausgangsfarben eingefügt,
        // damit ein komplettes Bild ausgegeben wird
        for (int y = 0; y < height; y++) {
            // Oberen und Unten müssen ganze Zeilen korrigiert werden
            if (y < kernelGap || y >= height - kernelGap) {
                // Oben/Unten
                for (int x = 0; x < width; x++)
                    vGauss[x][y] = in[x][y];
            } else {
                // Rechts und Links muss nur an den Seiten korrigiert werden
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
     * @return Ergebnis des Scharr-Operators
     */
    private double[][] scharrFilter(double[][] in) {
        // Scharr-Kernel
        // (Die .ac.uk-Seite verwendet den älteren Sobel-Operator,
        //  der Scharr-Operator ist aber bei kleinen Kerneln besser:
        //  http://docs.opencv.org/2.4/doc/tutorials/imgproc/imgtrans/sobel_derivatives/sobel_derivatives.html#formulation

        // Kernel für vertikale Ableitung:
        final double[] vertKernel = {-3.0, -10.0, -3.0,
                                    0.0, 0.0, 0.0,
                                    3.0, 10.0, 3.0};
        // Kernel für horizontale Ableitung:
        final double[] horizKernel = {-3.0, 0.0, 3.0,
                                    -10.0, 0.0, 10.0,
                                    -3.0, 0.0, 3.0};

        // Abstand, der zur Seite Für die Kernel benötigt wird
        int kernelGap = (int) ((Math.sqrt(vertKernel.length) - 1) / 2); // ==1

        // Ergebniss d. Ableitungsfunktionen des Graphens in beide Richtungen
        double[][] vertScharr = new double[width][height];
        double[][] horizScharr = new double[width][height];

        for (int x = kernelGap; x < width - kernelGap; x++) {
            for (int y = kernelGap; y < height - kernelGap; y++) {
                int maskPos = 0;    // Counter für Position im Kernel
                double vert = 0.0;  // Vertikaler Ableitungswert
                double horiz = 0.0; // Horizontaler Ableitungswert

                // Ausrechnen beider Kernel mit Convulution (http://homepages.inf.ed.ac.uk/rbf/HIPR2/convolve.htm)
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
        // sqrt(vert^2+horiz^2)
        double[][] combScharr = new double[width][height];
        int[][] angles = new int[width][height];
        for (int x = kernelGap; x < width - kernelGap; x++) {
            for (int y = kernelGap; y < height - kernelGap; y++) {
                // Laden der beiden Ableitungswerte zu (x, y)
                double vert = vertScharr[x][y];
                double horiz = horizScharr[x][y];
                // Speichern
                angles[x][y] = getAngle(horiz, vert);
                combScharr[x][y] = Math.sqrt(vert * vert + horiz * horiz);
            }
        }

        // Non-Maximum-Suppression
        double[][] nmsImage = new double[width][height];
        for (int x = kernelGap; x < width - kernelGap; x++) {
            for (int y = kernelGap; y < height - kernelGap; y++) {
                int direction = angles[x][y];
                double hereVal = combScharr[x][y];
                double a = 0, b = 0;
                switch(direction) {
                    case 0:
                        // Vertikal
                        a = combScharr[x-1][y];
                        b = combScharr[x+1][y];
                        break;
                    case 1:
                        // SW-NE
                        a = combScharr[x-1][y-1];
                        b = combScharr[x+1][y+1];
                        break;
                    case 2:
                        // Horizontal
                        a = combScharr[x][y-1];
                        b = combScharr[x][y+1];
                        break;
                    case 3:
                        // SE-NW
                        a = combScharr[x-1][y+1];
                        b = combScharr[x+1][y-1];
                }
                if (hereVal >= a && hereVal >= b) {
                    nmsImage[x][y] = hereVal;
                } else {
                    nmsImage[x][y] = 0.0;
                }
            }
        }

        // Hier gibt es ebenfalls einen Randbereich,
        // dort setze 0 ein. Damit werden dort keine Ecken erkannt
        for (int x = 0; x < width; x++) {
            // Ich fasse hier den Rand größer auf,
            // da an den Rändern aufgrund des Übergangs von Nicht-Wichgezeichnet
            // zu Weichgezeichnet Kanten erkannt wurden
            for (int y = 0; y < height; y++) {
                kernelGap *= 2;
                if (y < kernelGap || y >= height - kernelGap || x < kernelGap || x >= width - kernelGap) {
                    nmsImage[x][y] = 0.0;
                }
            }
        }

        return nmsImage;
    }

    // 0: N-S; 1: SW-NE; 2: W-E; 3: SE-NW
    private int getAngle (double horiz, double vert) {
        // http://en.cppreference.com/w/cpp/numeric/math/atan2
        double a = Math.atan2(vert, horiz);
        if ((a > 0.375*PI && a < 0.625*PI) || ((a < -0.375*PI && a > -0.625*PI)))
            return 0;
        if ((a < -0.625*PI && a > -0.875*PI) || (a > 0.125*PI && a < 0.375*PI))
            return 1;
        if ((a < 0.125*PI && a > -0.125*PI) || a > 0.875*PI || a < -0.875 * PI)
            return 2;
        return 3;
    }

    /**
     * Das Ergebnis des Scharr-Operators mit dem Hysterese binärisieren:
     * http://homepages.inf.ed.ac.uk/rbf/HIPR2/canny.htm
     * @param scharrImage
     * @return
     */
    private boolean[][] hysteresis(double[][] scharrImage) {
        boolean[][] binaryImage = new boolean[width][height]; // Ausgabebild
        // Binärisieren mit hohem Schwellwert
        double treshhold = 300.0;
        Stack<Coordinate> hysteresisStack = new Stack<>(); // Die Trues mit hohem Wert für spätere Hysterese

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (scharrImage[x][y] > treshhold) {
                    binaryImage[x][y] = true;
                    hysteresisStack.push(new Coordinate(x, y)); // Für anliegende Felder gilt ggfs. kleinere Schwelle
                }

            }
        }

        // Bereich mit kleiner Schwelle
        treshhold /= 2.0; // Treshhold senken
        while (!hysteresisStack.empty()) { // Für jedes Kantenfeld
            Coordinate c = hysteresisStack.pop();
            int x = c.getX(); int y = c.getY();

            // Gucken ob es daneben False-Felder gibt, die mit dem halben Treshhold True sind
            // für die an diese Felder angrenzenden Felder gilt ebenfalls die kleine Schwelle

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

    // Ausfüllen der Polygone
    private boolean[][] fill (boolean[][] in) {
        int[][] result = new int[width][height];

        // Initialisieren mit -1
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                result[x][y] = -1;
            }
        }

        int cnt = 0;
        // Der Hintergrund ist Komponente 0 --> Flood-Fill über die Falses von [0][0]
        // Standard Flood-Fill:
        Stack<Coordinate> ffStack = new Stack<>();
        ffStack.add(new Coordinate(0, 0));
        while (!ffStack.empty()) {
            Coordinate c = ffStack.pop();
            int x = c.getX();
            int y = c.getY();
            if (!in[x][y] && result[x][y] == -1) {
                result[x][y] = cnt;

                if (x-1 >= 0)
                    ffStack.push(new Coordinate(x-1, y));
                if (x+1 < width)
                    ffStack.push(new Coordinate(x+1, y));
                if (y-1 >= 0)
                    ffStack.push(new Coordinate(x, y-1));
                if (y+1 < height)
                    ffStack.push(new Coordinate(x, y+1));
            }
        }
        cnt++;

        // Markieren der weiteren Komponenten
        boolean flag; // Wenn alles bestimmt wurde
        do {
            flag = false;

            // Stapel für schwarze/weiße Felder der neuen Komponente
            Stack<Coordinate> blackStack = new Stack<>();
            Stack<Coordinate> whiteStack = new Stack<>();

            // Schwarzen Stack mit an den bestimmten Bereich angrenzenden Schwarzen Feldern initialisieren
            for (int x = 0; x < width - 1; x++) {
                for (int y = 0; y < height; y++) {
                    if (result[x][y] >= 0 && in[x+1][y] && result[x+1][y] == -1) {
                        blackStack.add(new Coordinate(x+1, y));
                        flag = true; // Erstes Feld geschwärzt --> Es gibt noch eine neue Komponente
                    }
                }
            }

            // Für alle schwarzen Felder der neuen Komponente
            while (!blackStack.empty()) {
                Coordinate c = blackStack.pop();
                int x = c.getX();
                int y = c.getY();

                result[x][y] = cnt; // Markieren

                // Angrenzende unbestimmte Felder auf entsprechenden Stapel legen:
                if (x-1 >= 0 && result[x-1][y] == -1) {
                    if (in[x - 1][y]) {
                        blackStack.add(new Coordinate(x - 1, y));
                    } else {
                        whiteStack.add(new Coordinate(x - 1, y));
                    }
                }
                if (x+1 < width && result[x+1][y] == -1) {
                    if (in[x + 1][y]) {
                        blackStack.add(new Coordinate(x + 1, y));
                    } else {
                        whiteStack.add(new Coordinate(x + 1, y));
                    }
                }
                if (y - 1 >= 0 && result[x][y - 1] == -1) {
                    if (in[x][y - 1]) {
                        blackStack.add(new Coordinate(x, y - 1));
                    } else {
                        whiteStack.add(new Coordinate(x, y - 1));
                    }
                }
                if (y + 1 < height && result[x][y + 1] == -1) {
                    if (in[x][y + 1]) {
                        blackStack.add(new Coordinate(x, y + 1));
                    } else {
                        whiteStack.add(new Coordinate(x, y + 1));
                    }
                }
            }

            // Für alle weißen Felder der neuen Komponente
            while(!whiteStack.empty()) {
                Coordinate c = whiteStack.pop();
                int x = c.getX();
                int y = c.getY();

                result[x][y] = cnt; // Markieren

                // Nur angrenzende weiße Felder markieren, schwarze sind die Grenze zur nächsten Komponente
                if (x-1 >= 0 && result[x-1][y] == -1 && !in[x - 1][y])
                    whiteStack.add(new Coordinate(x - 1, y));
                if (x+1 < width && result[x+1][y] == -1 && !in[x + 1][y])
                    whiteStack.add(new Coordinate(x + 1, y));
                if (y-1 >= 0 && result[x][y-1] == -1 && !in[x][y-1])
                    whiteStack.add(new Coordinate(x, y-1));
                if (y+1 < height && result[x][y+1] == -1 && !in[x][y+1])
                    whiteStack.add(new Coordinate(x, y+1));
            }

            cnt++; // Nächste Komponente!
        } while (flag);

        // Gerade Zahlen --> false; Ungerade Zahlen --> True
        boolean[][] out = new boolean[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (result[x][y] % 2 != 0) {
                    out[x][y] = true;
                } else {
                    out[x][y] = false;
                }
            }
        }

        return out;
    }

    public boolean[][] getSWImage() {
        return swImage;
    }

    public boolean[][] getEdgesImage() {
        return edgesImage;
    }
}
