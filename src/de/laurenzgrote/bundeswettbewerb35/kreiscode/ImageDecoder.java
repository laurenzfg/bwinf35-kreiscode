package de.laurenzgrote.bundeswettbewerb35.kreiscode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class ImageDecoder {

    /**
     * POJO für eine Koordinate mit Abweichung von der berechneten Kreismitte
     */
    private class CircleCenterCoordinate {
        Coordinate coordinate;
        double delta;

        public CircleCenterCoordinate(Coordinate coordinate, double delta) {
            this.coordinate = coordinate;
            this.delta = delta;
        }
    }

    // Konstanten
    // Ab wann ist Grau Schwarz?
    private final double minAVGBlack = 0.4; // From 0.0 (black) to 1.0 (white)
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

    // Map der Kreismittelpunkte
    // Key: Zusammenhangskomponente-ID des Kreises
    private HashMap<Integer, CircleCenterCoordinate> circleCenters = new HashMap<>();

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
        int cnt = 0;
        for (int x = rgbImage.getMinX(); x < width; x++) {
            for (int y = rgbImage.getMinY(); y < height; y++) {
                Color color = new Color(rgbImage.getRGB(x, y));
                double percentile = (color.getRed() / 255.0) +
                        (color.getGreen() / 255.0) +
                        (color.getBlue()  / 255.0);
                percentile /= 3.0;

                if (percentile <= minAVGBlack) {
                    swImage[x][y] = true;
                    cnt++;
                } else {
                    swImage[x][y] = false;
                }
            }
        }
        int corrected;
        do {
            System.err.println("INVOKED");
            corrected = complete();
        } while (corrected > 0);
        System.err.println("Found so many black fields: " + cnt);
    }


    /**
     * Vervollständigt wahrscheinlich Schwarze Felder
     * @return Anzahl der Vervollständigungen
     */
    private int complete () {
        int cnt = 0;

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

        return cnt;
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

            // Neue Zeile, akt. Streak damit abgeschlossen
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

            // Neue Spalte, akt. Streak damit abgeschlossen
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
        int kreiseCnt = 0;
        for (int x = rgbImage.getMinX(); x < width; ++x) {
            for (int y = rgbImage.getMinY(); y < height; y++) {
                int lengthHere = hStreak[x][y];
                if (lengthHere > 0) {
                    // Überprüfen auf Kreismittelpunkt
                    int center = x + (lengthHere / 2);
                    if (center < width && vStreak[center][y] == lengthHere) {
                        Coordinate coordinate = new Coordinate(center, y);
                        if (structureNos[center][y] == -1) {
                            int structNo = aktStructure;

                            double circle_size = (Math.PI * lengthHere * lengthHere) / 4.0;
                            double actual_size = floodFill(coordinate);
                            double delta = Math.abs(circle_size - actual_size);

                            // Ist es ein Kreis?
                            // lengthHere ist willkürlich
                            if (delta < lengthHere) {
                                circleCenters.put(structNo, new CircleCenterCoordinate(coordinate, delta));
                                kreiseCnt++;
                            }
                        } else {
                            // Wurde schon als Kreis erkannt
                            int structNo = structureNos[center][y];

                            // actual_size wurde schon bei der letzten Flood-Fill ermittelt
                            double actual_size = structureSizes.get(structNo);
                            double circle_size = (Math.PI * lengthHere * lengthHere) / 4.0;

                            double lastDelta = lengthHere;
                            if (circleCenters.containsKey(structNo)) {
                                // JA --> Diese Postion kleineres Delta
                                lastDelta = circleCenters.get(structNo).delta;
                            }
                            double delta = Math.abs(circle_size - actual_size);

                            if (delta < lastDelta)
                                circleCenters.put(structNo, new CircleCenterCoordinate(coordinate, delta));
                        }
                    }
                }
            }
        }
        System.err.println(kreiseCnt);
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
        // Einfache Koordinatenliste, Delta zum berechneten Punkt wird entfernt.
        ArrayList<Coordinate> out = new ArrayList<>();

        for (CircleCenterCoordinate ccc : circleCenters.values()) {
            out.add(ccc.coordinate);
        }

        return out;
    }
}
