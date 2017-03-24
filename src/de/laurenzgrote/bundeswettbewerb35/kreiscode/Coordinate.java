package de.laurenzgrote.bundeswettbewerb35.kreiscode;

public class Coordinate {
    private int x, y;
    private int streakLength;

    private String circleMeaning = "";
    public Coordinate(int x, int y, int streakLength) {
        this.x = x;
        this.y = y;
        this.streakLength = streakLength;
    }

    public Coordinate(int x, int y) {
        this.x = x;
        this.y = y;
        streakLength = -1; // Default-Wert
    }

    public Coordinate() {
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getStreakLength() {
        return streakLength;
    }

    public void setStreakLength(int streakLength) {
        this.streakLength = streakLength;
    }

    public String getCircleMeaning() {
        return circleMeaning;
    }

    public void setCircleMeaning(String circleMeaning) {
        this.circleMeaning = circleMeaning;
    }

    public String toString() {
        return x + ", " + y;
    }
}
