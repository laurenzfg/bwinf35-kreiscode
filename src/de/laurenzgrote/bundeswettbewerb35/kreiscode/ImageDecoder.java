package de.laurenzgrote.bundeswettbewerb35.kreiscode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ImageDecoder {

    // Konstanten
    // Ab wann ist Grau Schwarz?
    private final double minAVGBlack = 0.3; // Von 0.0 (black) zu 1.0 (white)
    private final int minADJ = 4; // Wieviele Felder müssen bei der Vervollständigung Schwarz sein?

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

    // Liste über die Kreismittelpunkte, indiziert nach Zusammenhangskomponentenid
    ArrayList<Coordinate> circleCenters = new ArrayList<>();

    public ImageDecoder(BufferedImage rgbImage) {
        // Boilerplate-Code
        this.rgbImage = rgbImage;
        width = rgbImage.getWidth();
        height = rgbImage.getHeight();

        // Arrayinitialisierung auf Bildrößte
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
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Durchschnitt der Helligkeit jeder Primärfarbe
                // 0 nicht da, 255 100%
                // --> 0,0,0 Schwarz, 255,255,255 Weiß
                Color color = new Color(rgbImage.getRGB(x, y));
                double percentile = (color.getRed() / 255) +
                        (color.getGreen() / 255) +
                        (color.getBlue()  / 255);
                percentile /= 3.0;

                brightness[x][y] = percentile;
            }
        }

        // 0mal glätten
        brightness = glaette(brightness, 0);

        // Was unterm Treshold liegt wird als Schwarz gespeichert
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (brightness[x][y] < minAVGBlack)
                    swImage[x][y] = true;

        // vervollstaendige();

    }

    private double[][] glaette (double[][] in, int n) {
        // Glättung n mal vornehmen
        for (int i = 0; i < 3; i++) {
            double[][] newBrightness = new double[width][height];
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                    double average = 0;
                    for (int x1 = x - 1; x1 <= x + 1; x1++)
                        for (int y1 = y - 1; y1 <= y + 1; y1++)
                            average += in[x1][y1];
                    average /= 10;
                    newBrightness[x][y] = average;
                }
            }
            in = newBrightness;
        }
        return in;
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
                        if (adj >= minADJ) {
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

        // Horizontal (Zeilenweise) Scannen
        for (int y = rgbImage.getMinY(); y < height; ++y) {
            for (int x = rgbImage.getMinX(); x < width; ++x) {
                if (swImage[x][y]) {
                    // Schwarz
                    streakLength++;
                } else {
                    // Weiß
                    // Ende einer Streak?
                    if (streakLength > 0) {
                        for (int i = x - 1; i >= x - streakLength; i--) {
                            hStreak[i][y] = streakLength;
                        }
                        streakLength = 0;
                    }
                }
            }

            // Neue Zeile, akt. Streak damit wenn vorhanden abgeschlossen
            if (streakLength > 0) {
                for (int i = width - 1; i >= width - streakLength; i--) {
                    hStreak[i][y] = streakLength;
                }
                streakLength = 0;
            }
        }
        // Vertikal (Spaltenweise) Scannen
        for (int x = rgbImage.getMinX(); x < width; ++x) {
            for (int y = rgbImage.getMinY(); y < height; ++y) {
                if (swImage[x][y]) {
                    // Schwarz
                    streakLength++;
                } else {
                    // Weiß
                    // Ende einer Streak?
                    if (streakLength > 0) {
                        for (int i = y - 1; i >= y - streakLength; i--) {
                            vStreak[x][i] = streakLength;
                        }
                        streakLength = 0;
                    }
                }
            }

            // Neue Spalte, akt. Streak damit wenn vorhanden abgeschlossen
            if (streakLength > 0) {
                for (int i = height - 1; i >= height - streakLength; i--) {
                    vStreak[x][i] = streakLength;
                }
                streakLength = 0;
            }
        }
    }

    /**
     * Sucht Kreise
     */
    private void scanForCircles() {
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
                        if (Math.abs(hStreakLength - vStreakLength) < 2 && first > 0 && last < height &&
                                (swImage[center][first]) && swImage[center][last]){
                            // Kreiskriterium I erfüllt,
                            // --> Kandidat für Mittelpunkt also Mittelpunkt der Streak
                            Coordinate coord = new Coordinate(center, y);

                            // Fäche nach Kreisformel
                            double circleSize = (Math.PI * hStreakLength * hStreakLength) / 4.0;
                            double actualSize = floodFill(coord); // Gemessene Größe

                            // Delta zwischen Fläche nach Kreisformel und gemessener Fläche bestimmen
                            double delta = Math.min(circleSize, actualSize) / Math.max(circleSize, actualSize);
                            // Ist das Delta zwischen Fläche nach Kreisformal und gemessener Fläche klein genug?
                            if (delta >= 0.95) {
                                // Jetzt Test auf umgebenden schwarzen Ring
                                double third = hStreakLength * (1.0 / 3.0);
                                double delta2 = 0;

                                delta2 += Math.min(hStreak[center + hStreakLength][y], third) /
                                        Math.max(hStreak[center + hStreakLength][y], third);
                                delta2 += Math.min(hStreak[center - hStreakLength][y], third) /
                                        Math.max(hStreak[center - hStreakLength][y], third);
                                delta2 += Math.min(vStreak[center][y + hStreakLength], third) /
                                        Math.max(vStreak[center][y + hStreakLength], third);
                                delta2 += Math.min(vStreak[center][y - hStreakLength], third) /
                                        Math.max(vStreak[center][y - hStreakLength], third);

                                delta2 /= 4.0;
                                if (delta2 >= 0.95)
                                    circleCenters.add(coord);
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
