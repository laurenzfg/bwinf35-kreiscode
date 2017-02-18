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
    private final double minAVGBlack = 0.1; // Von 0.0 (black) zu 1.0 (white)
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
        double avg = 0;
        // Hellsten Punkt finden
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = new Color(rgbImage.getRGB(x, y));
                avg += color.getRed() + color.getGreen() +
                            color.getBlue();
            }
        }
        avg /= width * height;
        // Helligkeitswert für jeden Pixel berechnen
        double[][] brightness = new double[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Durchschnitt der Helligkeit jeder Primärfarbe
                // 0 nicht da, 255 100%
                // --> 0,0,0 Schwarz, 255,255,255 Weiß
                Color color = new Color(rgbImage.getRGB(x, y));
                double percentile = (color.getRed() / avg) +
                        (color.getGreen() / avg) +
                        (color.getBlue()  / avg);
                percentile /= 3.0;

                brightness[x][y] = percentile;
            }
        }

        // Glättung n mal vornehmen
        for (int n = 0; n < 3; n++) {
            double[][] newBrightness = new double[width][height];
            for (int x = 1; x < width - 1; x++) {
                for (int y = 1; y < height - 1; y++) {
                        double average = 0;
                        for (int x1 = x - 1; x1 <= x + 1; x1++)
                            for (int y1 = y - 1; y1 <= y + 1; y1++)
                                average += brightness[x1][y1];
                        average /= 10;
                        newBrightness[x][y] = average;
                }
            }
            brightness = newBrightness;
        }

        // Was unterm Treshold liegt wird als Schwarz gespeichert
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (brightness[x][y] < minAVGBlack)
                    swImage[x][y] = true;

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
        // Horizontal über das Bild iterieren
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; y++) {
                // Horizontale Streak-Länge
                int lengthHere = hStreak[x][y];

                // Sind wir in einer Streak UND fängt sie in dieser Zeile an?
                if (lengthHere > 0 && (x == 0 || !swImage[x-1][y])) {
                    // Bestimmen des Mittelpunktes der Streak
                    int center = x + (lengthHere / 2);
                    // Ist am Mittelpunkt der vertikale Streak genauso lang? (siehe Doku)
                    // TODO: Fuzzyness-nötig ?!
                    if (center < width && vStreak[center][y] == lengthHere) {
                        // Kreiskriterium I erfüllt,
                        // --> Kandidat für Mittelpunkt also Mittelpunkt der Streak
                        Coordinate coord = new Coordinate(center, y);

                        // Fäche nach Kreisformel
                        double circleSize = (Math.PI * lengthHere * lengthHere) / 4.0;

                        int structNo; // ID der Fläche (Zusammenhangskomponente)
                        double actualSize; // Gemessene Größe

                        double maxDelta = 100; // Maximal akzeptiertes Delta zur Akzeptanz als Kreis
                        // 100 ist willkürlich

                        // Wurde schon per Flood-Fill die Flächengröße bestimmt?
                        if (structureNos[center][y] == -1) {
                            // Nein
                            // Flächen-ID ist also eine neue Nummer
                            structNo = aktStructure;

                            // Messen der Fläche
                            actualSize = floodFill(coord);

                        } else {
                            // Ja
                            // actualSize u. Flächen-ID wurde schon bei der letzten Flood-Fill ermittelt
                            // --> laden aus dem Arbeitsspeicher
                            structNo = structureNos[center][y];
                            actualSize = structureSizes.get(structNo);

                            // Maximales Delta ist das bei der letzten Kreisplatzierung gemessene Delta.
                            // Wenn bei der letzten Messung zwar die Fläche ausgemessen wurde,
                            // aber Kreiskriterium II nicht erfüllt war, gilt das allgemeine maxDelta.
                            if (circleCenters.containsKey(structNo))
                                maxDelta = circleCenters.get(structNo).delta;
                        }

                        // Delta zwischen Fläche nach Kreisformel und gemessener Fläche bestimmen
                        double delta = Math.abs(actualSize - circleSize);
                        // Ist das Delta zwischen Fläche nach Kreisformal und gemessener Fläche klein genug?
                        if (delta < maxDelta)
                            circleCenters.put(structNo, new CircleCenterCoordinate(coord, delta));
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
        // Einfache Koordinatenliste, Delta zum berechneten Punkt wird entfernt.
        ArrayList<Coordinate> out = new ArrayList<>();

        for (CircleCenterCoordinate ccc : circleCenters.values()) {
            out.add(ccc.coordinate);
        }

        return out;
    }
}
