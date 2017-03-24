package de.laurenzgrote.bundeswettbewerb35.kreiscode;

import java.io.File;
import java.util.List;

public class ImageDecoder {
    // Decoderklasse
    SequenceDecoder decoder;

    // S/W-Bild für Dekodiervorgang
    private boolean[][] swImage;
    private int width, height;

    private boolean[][] trapezials;
    private boolean[][] visited;
    // Liste der Kreismittelpunkte u. der dazugehörigen Durchmesser
    private List<Coordinate> circleCenters;

    public ImageDecoder(boolean[][] swImage, List<Coordinate> circleCenters, File dict) {
        this.decoder = new SequenceDecoder(dict);
        this.swImage = swImage;
        this.circleCenters = circleCenters;

        width = swImage.length;
        height = swImage[0].length;

        visited = new boolean[width][height];
        trapezials = new boolean[width][height];

        decode();
    }

    /**
     * Dekodiert die Bedeutungen aller in circleCenters gegebenen Kreise
     */
    private void decode() {
        // Für jeden Kreismittelpunkt
        for (int i = 0; i < circleCenters.size(); i++) {
            Coordinate cC = circleCenters.get(i);
            // bestimme die einteilenden Linien
            Coordinate[][] lines = calculateLines(cC);

            // Und rastere aus diesen Trapeze in das visited-Array
            for (int n = 0; n < 16; n++) {
                // Diese Linien + die rechts angrenzenden
                Coordinate[] hereLines = lines[n];
                // Bei n = 15 *nicht* 16, sondern 0
                Coordinate[] nextLines = lines[(n+1)%16];
                // Rastern wir die einteilende Linie
                bresenham(hereLines[0], hereLines[1]);
                // Rastern der oberen Linie
                bresenham(hereLines[0], nextLines[0]);
                // Rastern der unteren Linie
                bresenham(hereLines[1], nextLines[1]);
            }

            // Bestimmen der Überwiegenden Farbe in den jeweiligen Segmenten
            boolean[] res = decodeTrapezials(lines);
            // Bestimmen der Bedeutung des 2byte-Arrays
            String s = decoder.decode(res);
            // Speichern
            cC.setCircleMeaning(s);
            circleCenters.set(i, cC);
        }
    }

    /**
     * Berechnet die Punkte für die Trapeze
     * @param cC Koordinaten des Kreismittelpunktes
     * @return Linien, die den Ring in N=16 Segmente einteilen. Untere Koordinate in [n][0], obere in n[1]
     */
    private Coordinate[][] calculateLines(Coordinate cC) {
        int diameter = cC.getStreakLength();
        Coordinate[][] lines = new Coordinate[16][2];

        // 22.5° in Bogenmaß
        final double factor = (22.5*Math.PI)/180.0;
        // u wie in der Doku
        final double u = diameter / 3.0;
        int x = cC.getX(); int y = cC.getY();

        // Alle 16 Linien berechnen
        for (int n = 0; n < 16; ++n) {
            int pX = (int) Math.round(Math.cos(n*factor) * 4.5*u + x);
            int pY = (int) Math.round(Math.sin(n*factor) * 4.5*u + y);
            lines[n][0] = new Coordinate(pX, pY);
            pX = (int) Math.round(Math.cos(n*factor) * 5.5*u + x);
            pY = (int) Math.round(Math.sin(n*factor) * 5.5*u + y);
            lines[n][1] = new Coordinate(pX, pY);
        }
        return lines;
    }

    private boolean[] decodeTrapezials(Coordinate[][] lines) {
        boolean[] result = new boolean[16];

        for (int n = 0; n < 16; n++) {
            Coordinate[] hereLines = lines[n];
            Coordinate[] nextLines = lines[(n+1)%16];

            int pX = hereLines[0].getX() + hereLines[1].getX() + nextLines[0].getX() + nextLines[1].getX();
            int pY = hereLines[0].getY() + hereLines[1].getY() + nextLines[0].getY() + nextLines[1].getY();
            pX /= 4;
            pY /= 4;

            result[n] = segmentColor(pX, pY);
        }

        return result;
    }

    // Globale vars für Weiß und Schwarz.
    // Schlechter Stil, aber sonst würde der FloodFill-Code unleserlich werden
    private int whites, blacks;
    /**
     * Bestimmt die Farbe eines durch Trapezstriche begrenzten Segmentes
     * @param x x-Koordinate eines beliebigen Punktes im Segment
     * @param y y-Koordinate eines beliebigen Punktes im Segment
     * @return True wenn Schwarz > 50% d. Fläche, sonst False
     */
    private boolean segmentColor(int x, int y) {
        // Clearen der vars / flood Fill starten
        whites = 0;
        blacks = 0;
        floodFill(x, y);
        // Auszählen
        if (blacks > whites)
            return true;
        return false;
    }

    private void floodFill(int x, int y) {
        // FloodFill mit Auszählen von Weiß/SChwarz
        if(x > 0 && y > 0 && x < width && y < height && !trapezials[x][y] && !visited[x][y]) {
            visited[x][y] = true;
            if (swImage[x][y]) {
                blacks++;
            } else {
                whites++;
            }
            floodFill(x+1, y);
            floodFill(x-1, y);
            floodFill(x, y + 1);
            floodFill(x, y - 1);
        }
    }

    // Needs some love
    void bresenham(Coordinate a, Coordinate b)
    {
        int x1 = a.getX(); int y1 = a.getY();
        int x2 = b.getX(); int y2 = b.getY();
        // delta of exact value and rounded value of the dependant variable
        int d = 0;

        int dy = Math.abs(y2 - y1);
        int dx = Math.abs(x2 - x1);

        int dy2 = (dy << 1); // slope scaling factors to avoid floating
        int dx2 = (dx << 1); // point

        int ix = x1 < x2 ? 1 : -1; // increment direction
        int iy = y1 < y2 ? 1 : -1;

        if (dy <= dx) {
            for (;;) {
                trapezials[x1][y1] = true;
                if (x1 == x2)
                    break;
                x1 += ix;
                d += dy2;
                if (d > dx) {
                    y1 += iy;
                    d -= dx2;
                }
            }
        } else {
            for (;;) {
                trapezials[x1][y1] = true;
                if (y1 == y2)
                    break;
                y1 += iy;
                d += dx2;
                if (d > dy) {
                    x1 += ix;
                    d -= dy2;
                }
            }
        }
    }

    public boolean[][] getTrapezials() {
        return trapezials;
    }

    public List<Coordinate> getUpdatedCircleCenters() {
        return circleCenters;
    }
}
