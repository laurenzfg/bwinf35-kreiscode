package de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing;
import de.laurenzgrote.bundeswettbewerb35.kreiscode.Coordinate;

import java.util.*;
import java.util.List;

public class CircleCenters {
    // S/W-Bild für Dekodiervorgang
    private boolean[][] swImage;
    private int width, height;
    private int tolerance;


    // Zähler der vertikal/horizontal durchgehend Schwarz gefärbten Stellen
    private int[][] hStreak;
    private int[][] vStreak;

    // Zusammenhangskomponenten im S/W-Bild
    private int aktStructure = 0; // Anzahl d. Zusammenhangskomponenten

    private int[][] structureNos; // Zusammenhangskomponente-ID nach Bildpixel
    private ArrayList<Integer> structureSizes = new ArrayList<>(); // Größen der Struktur der jeweiligen ID
    // Liste über die CircleCenters, indiziert nach ZusammenhangskomponentenID
    private ArrayList<Coordinate> circleCenters = new ArrayList<>();

    public CircleCenters(boolean[][] swImage) {
        // Übernehmen des S/W Bildes
        this.swImage = swImage;
        width = swImage.length;
        height = swImage[0].length;

        // Eine Streak gilt als beendet, wenn 0.00125 Promille der Pixelzahl des Bildes an Falses aufeinander folgen
        // Bsp.: In einem 2000x2000-Bild dürfen 5px nacheinander False sein
        // Daraus resultiert dann BTW ein Mindest-U von: 6px --> Mindestdurchmesser Kreiscode 42px
        tolerance = (int) (width * height * 0.000001);

        hStreak = new int[width][height];
        vStreak = new int[width][height];
        structureNos = new int[width][height];

        // Zusammenhangskomponenten noch nicht ermittelt
        // --> Default-Nummer -1 setzen
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                structureNos[i][j] = -1;
            }
        }

        scanStreaks();
        scanForCircles();
    }

    /**
     * Zählt durchgehend Schwarze Linien vertikal und horizontal
     */
    private void scanStreaks() {
        for (int x = 0; x < width; ++x) {
            scanStreaks(x, 0, 0, 1);
        }
        for (int y = 0; y < height; ++y) {
            scanStreaks(0, y, 1, 0);
        }
    }

    /**
     * Funktion zum Scannen nach Linien
     * @param x Startkoordinate
     * @param y Startkoordinate
     * @param deltaX Bewegung in x-Richtung
     * @param deltaY Bewegung in y-Richtung
     */
    private void scanStreaks(int x, int y, int deltaX, int deltaY) {
        int streakLength  = 0;

        // Solange sich der Cursor im Bild befindet
        while (x >= 0 && x < width && y >= 0 && y < height) {
            // Auszählen
            if (swImage[x][y]) {
                streakLength++;
            } else {
                // Streak ist hier beendet
                markStreak(x, y, deltaX, deltaY, streakLength);
                streakLength = 0;
            }

            // Cursor verschieben
            x += deltaX;
            y += deltaY;
        }

        // Neue Zeile / Spalte, akt. Streak damit wenn vorhanden abgeschlossen
        if (streakLength > 0) {
            // Streak ist hier beendet
            markStreak(x, y, deltaX, deltaY, streakLength);
        }
    }


    /**
     * Hilfsfunktion zum Schreiben der Streak-Länge in alle Felder der Streak
     * @param x Letzte Koordinate des Erkennungsprozesses
     * @param y Letzte Koordinate des Erkennungsprozesses
     * @param deltaX Bewegung in x-Richtung
     * @param deltaY Bewegung in y-Richtung
     * @param streakLength Berechnete Länge der Streak (inkl. anschließendem Toleranz-Suffix
     */
    private void markStreak(int x, int y, int deltaX, int deltaY, int streakLength) {
        // Über die Gesamte streakLength Streaklänge schreiben
        for (int i = 0; i <= streakLength; i++) {
            // Bestimme ob wir horizontal oder vertikal scannen
            if (x > 0 && x < width && y > 0 && y < height) {
                if (deltaX == 0) {
                    // Vertikal
                    vStreak[x][y] = streakLength;
                } else {
                    // Horizontal
                    hStreak[x][y] = streakLength;
                }
            }
            // Zeiger rückwärts verschieben
            x -= deltaX;
            y -= deltaY;
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

                // Sind wir in einer ausreichen langen Streak UND fängt sie in dieser Zeile an?
                if (hStreakLength > 10 && (x == 0 || !swImage[x-1][y])) {
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
                        if (Math.abs(hStreakLength - vStreakLength) < tolerance
                                && first > 0 && last > 0 && last < height && first < height) {
                            // Haben die vertikalen Extrema die gleiche Streaklänge     
                            if (vStreak[center][first] == vStreakLength && vStreak[center][last] == vStreakLength) {
                                // Kreiskriterium I erfüllt,
                                // --> Kandidat für Mittelpunkt also Mittelpunkt der Streak
                                Coordinate coord = new Coordinate(center, y, hStreakLength);

                                double circleSize = (Math.PI * hStreakLength * hStreakLength) / 4.0; // Berechnete Größe
                                double actualSize = floodFill(coord); // Gemessene Größe

                                // Delta zwischen Fläche nach Kreisformel und gemessener Fläche bestimmen
                                double delta = Math.min(circleSize, actualSize) / Math.max(circleSize, actualSize);
                                // Ist das Delta zwischen Fläche nach Kreisformal und gemessener Fläche klein genug?
                                // Und kann da größenmäßig überhaupt ein kompletter KreisCode sein?
                                // Radius (von Center) : 6u = 2x Durchmesser
                                if (delta >= 0.90 && center + 2*hStreakLength < width && center - 2*hStreakLength >= 0) {
                                    // Jetzt Test auf umgebenden schwarzen Ring
                                    double u = hStreakLength / 3.0;
                                    circleSize = 6.0 * Math.PI * (u*u);
                                    actualSize = floodFill(new Coordinate(center + hStreakLength, y));
                                    delta = Math.min(circleSize, actualSize) / Math.max(circleSize, actualSize);
                                    if (delta >= 0.7)
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
        // Wurde die Struktur schon durchgerechnet?
        int structNo = structureNos[from.getX()][from.getY()];
        if (structNo == -1) {
            // NEIN
            int size = 0;
            Queue<Coordinate> q = new ArrayDeque<>();
            q.add(from);

            while (!q.isEmpty()) {
                Coordinate c = q.poll();
                int x = c.getX();
                int y = c.getY();

                // Ist das Feld noch nicht besucht worden?
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
        } else {
            // JA
            return structureSizes.get(structNo);
        }
    }

    public List<Coordinate> getCircleCenters() {
        return circleCenters;
    }
}
