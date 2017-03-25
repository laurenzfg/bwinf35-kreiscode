package de.laurenzgrote.bundeswettbewerb35.kreiscode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Kreismittelpunkte {
    // Daten des bunten Eingabebildes
    private BufferedImage rgbImage;
    private int width;
    private int height;

    // S/W-Bild für Dekodiervorgang
    private boolean[][] swImage;

    // Zähler der vertikal/horizontal durchgehend Schwarz gefärbten Stellen
    private int[][] hStreak;
    private int[][] vStreak;

    // Zusammenhangskomponenten im S/W-Bild
    private int aktStructure = 0; // Anzahl d. Zusammenhangskomponenten
    private int[][] structureNos; // Zusammenhangskomponente-ID nach Bildpixel
    private ArrayList<Integer> structureSizes = new ArrayList<>(); // Größen je Zusammenhangskomponten

    // Liste über die Kreismittelpunkte, indiziert nach ZusammenhangskomponentenID
    ArrayList<Coordinate> circleCenters = new ArrayList<>();

    public Kreismittelpunkte(BufferedImage rgbImage) {
        // Boilerplate-Code
        this.rgbImage = rgbImage;
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();

        // Arrayinitialisierung auf Bildröße
        swImage = new boolean[width][height];
        hStreak = new int[width][height];
        vStreak = new int[width][height];
        structureNos = new int[width][height];

        // Zusammenhangskomponenten noch nicht ermittelt
        // --> Default-Nummer -1
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                structureNos[i][j] = -1;
            }
        }

        // Aufrufen der Dekodierschritte
        generateSW();
        scanStreaks();
        scanForCircles();
    }

    /**
     * Generiert S/W-Bild aus buntem Bild
     */
    private void generateSW () {
        // Helligkeitswert für jeden Pixel berechnen
        double[][] brightness = new double[width][height];
        double avgBrightness = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Durchschnitt der Helligkeit jeder Primärfarbe
                // 0 nicht da, 255 100%
                // --> 0,0,0 Schwarz, 255,255,255 Weiß
                Color color = new Color(rgbImage.getRGB(x, y));
                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                // https://en.wikipedia.org/wiki/Grayscale#Colorimetric_.28luminance-preserving.29_conversion_to_grayscale
                // https://www.w3.org/Graphics/Color/sRGB
                double percentile = r*0.2126 + g*0.7152 + b*0.0722;
                /*double percentile = (color.getRed() / 255.0) +
                        (color.getGreen() / 255.0) +
                        (color.getBlue()  / 255.0);*/
                percentile /= 3.0;

                avgBrightness += percentile;
                brightness[x][y] = percentile;
            }
        }

        brightness = glaette(brightness);

        // Was unterm Treshold liegt wird als Schwarz gespeichert
        double treshhold = avgBrightness / ((double) (width * height)) * 0.75;
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (brightness[x][y] < treshhold)
                    swImage[x][y] = true;

        vervollstaendige();

    }

    private double[][] glaette(double[][] in) {
        // Glättung n mal vornehmen
        double[][] newBrightness = new double[width][height];
        for (int x = 1; x < width - 1; x++) {
            for (int y = 1; y < height - 1; y++) {
                double average = 0;
                for (int x1 = x - 1; x1 <= x + 1; x1++)
                    for (int y1 = y - 1; y1 <= y + 1; y1++)
                        average += in[x1][y1];
                average /= 10.0;
                newBrightness[x][y] = average;
            }
        }
        return newBrightness;
    }

    private void vervollstaendige() {
        // Vervollsändigung
        int cnt;
        do {
            cnt = 0;
            // TODO sobald absehbar dass das nichts wird continue
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    if (!swImage[x][y]) {
                        int adj = -1;
                        for (int x1 = x - 1; x1 <= x + 1; x1++)
                            for (int y1 = y - 1; y1 <= y + 1; y1++)
                                if (swImage[x1][y1])
                                    adj++;
                        if (adj >= 4) {
                            swImage[x][y] = true;
                            cnt++;
                        }
                    }
                }
            }
        } while (cnt > 0);
    }

    /**
     * Zählt durchgehend Schwarze Linien vertikal und horizontal
     */
    @SuppressWarnings("Duplicates")
    private void scanStreaks() {
        int streakLength = 0;
        int falsesSoFar = 0;
        // Horizontal (Zeilenweise) Scannen
        int tolerance = (int) Math.round(width * 1.0/600.0); // Toleranz ist 1/3% der Breite
        for (int y = rgbImage.getMinY(); y < height; ++y) {
            for (int x = rgbImage.getMinX(); x < width; ++x) {
                if (swImage[x][y]) {
                    // Schwarz
                    streakLength++;
                } else if (falsesSoFar == tolerance) {
                    // Weiß
                    // Ende einer Streak?
                    if (streakLength > 0) {
                        for (int i = x - 1; i >= x - streakLength; i--) {
                            hStreak[i][y] = streakLength;
                        }
                        streakLength = 0;
                        falsesSoFar = 0;
                    }
                } else {
                    falsesSoFar++;
                }
            }

            // Neue Zeile, akt. Streak damit wenn vorhanden abgeschlossen
            if (streakLength > 0) {
                for (int i = width - 1; i >= width - streakLength; i--) {
                    hStreak[i][y] = streakLength;
                }
                streakLength = 0;
                falsesSoFar = 0;
            }
        }
        // Vertikal (Spaltenweise) Scannen
        tolerance = (int) Math.round(height * 1.0/600.0); // Toleranz ist 2/3% der Höhe
        for (int x = rgbImage.getMinX(); x < width; ++x) {
            for (int y = rgbImage.getMinY(); y < height; ++y) {
                if (swImage[x][y]) {
                    // Schwarz
                    streakLength++;
                } else if (falsesSoFar == tolerance){
                    // Weiß
                    // Ende einer Streak?
                    if (streakLength > 0) {
                        for (int i = y - 1; i >= y - streakLength; i--) {
                            vStreak[x][i] = streakLength;
                        }
                        streakLength = 0;
                        falsesSoFar = 0;
                    }
                } else {
                    falsesSoFar++;
                }
            }

            // Neue Spalte, akt. Streak damit wenn vorhanden abgeschlossen
            if (streakLength > 0) {
                for (int i = height - 1; i >= height - streakLength; i--) {
                    vStreak[x][i] = streakLength;
                }
                streakLength = 0;
                falsesSoFar = 0;
            }
        }
    }

    /**
     * Sucht Kreise
     */
    private void scanForCircles() {
        int tolerance = (int) Math.round(Math.round((width + height) / 2.0) * 2.0/300.0);
        // Horizontal über das Bild iterieren
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; y++) {
                // Horizontale Streak-Länge
                int hStreakLength = hStreak[x][y];

                // Sind wir in einer Streak UND fängt sie in dieser Zeile an?
                if (hStreakLength > 0 && (x == 0 || !swImage[x-1][y])) {
                    // Bestimmen des Mittelpunktes der Streak
                    int center = x + (hStreakLength / 2);
                    // Ist der Mittelpunkt der Streak innerhalb des Bildes?
                    // Wurde schon per Flood-Fill die Flächengröße bestimmt?
                    if (center < width && structureNos[center][y] == -1) {
                        // Ist am Mittelpunkt d. Horizontalen die vertikale Streak genauso lang und
                        // hat die vertikale Streak hier ihren Mittelpunkt? (siehe Doku)
                        int vStreakLength = vStreak[center][y];
                        int halfVLength = vStreakLength / 2;
                        int first = y - halfVLength + 1;
                        int last = y + halfVLength - 1;
                        if (Math.abs(hStreakLength - vStreakLength) < tolerance && first > 0 && last > 0 && last < height && first < height) {
                            if (swImage[center][first] && swImage[center][last]) {
                                // Kreiskriterium I erfüllt,
                                // --> Kandidat für Mittelpunkt also Mittelpunkt der Streak
                                Coordinate coord = new Coordinate(center, y, hStreakLength);

                                // Fäche nach Kreisformel
                                double circleSize = (Math.PI * hStreakLength * hStreakLength) / 4.0;
                                double actualSize = floodFill(coord); // Gemessene Größe

                                // Delta zwischen Fläche nach Kreisformel und gemessener Fläche bestimmen
                                double delta = Math.min(circleSize, actualSize) / Math.max(circleSize, actualSize);
                                // Ist das Delta zwischen Fläche nach Kreisformal und gemessener Fläche klein genug?
                                if (delta >= 0.90) {
                                    // Jetzt Test auf umgebenden schwarzen Ring
                                    double third = hStreakLength / 3.0;
                                    double delta2 = 0;

                                    int left = Math.min(center + hStreakLength, width - 1);
                                    int right = Math.max(center - hStreakLength, 0);
                                    int below = Math.min(y + hStreakLength, height - 1);
                                    int above = Math.max(y - hStreakLength, 0);
                                    delta2 += Math.min(hStreak[left][y], third) /
                                            Math.max(hStreak[left][y], third);
                                    delta2 += Math.min(hStreak[right][y], third) /
                                            Math.max(hStreak[right][y], third);
                                    delta2 += Math.min(vStreak[center][above], third) /
                                            Math.max(vStreak[center][above], third);
                                    delta2 += Math.min(vStreak[center][below], third) /
                                            Math.max(vStreak[center][below], third);

                                    delta2 /= 4.0;
                                    if (delta2 >= 0.80)
                                        circleCenters.add(coord);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Berechnet mithilfe einer Flood-Fill die Größe einer Zusammenhangskomponente.
     * Vergibt automatisch ID.
     * @param from Ausgangspunkt der Flood-Fill
     * @return Größe der Zusammenhangskomponente
     */
    private int floodFill(Coordinate from) {
        int size = 0;
        Queue<Coordinate> q = new ArrayDeque<>();
        q.add(from);

        while (!q.isEmpty()) {
            Coordinate c = q.poll();
            int x = c.getX();
            int y = c.getY();

            if (structureNos[x][y] == -1) {
                structureNos[x][y] = aktStructure;
                size++;
                // Anliegende Felder der Queue hinzufügen
                if (x + 1 < width && swImage[x + 1][y])
                    q.add(new Coordinate(x + 1, y));
                if (x - 1 >= 0 && swImage[x - 1][y])
                    q.add(new Coordinate(x - 1, y));
                if (y + 1 < height && swImage[x][y+1])
                    q.add(new Coordinate(x, y + 1));
                if (y - 1 >= 0 && swImage[x][y-1])
                    q.add(new Coordinate(x, y - 1));
            }
        }

        aktStructure++; // ID-Zähler erhöhen
        structureSizes.add(size); // Speichern der Größe
        return size; // Rückgabe der Größe
    }

    // Getter-Methoden
    public boolean[][] getSwImage() {
        return swImage;
    }
    public List<Coordinate> getCircleCenters() {
        return circleCenters;
    }
}
