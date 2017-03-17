package de.laurenzgrote.bundeswettbewerb35.kreiscode.GUI;

import de.laurenzgrote.bundeswettbewerb35.kreiscode.Coordinate;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CustomDialogs {

    public static void showSWDialog(boolean[][] swImage) {
        JFrame jFrame = new JFrame("Kreiscode, S/W Image");
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setSize(600, 600);

        ImagePanel boolean2DPanel = new ImagePanel();
        boolean2DPanel.setImage(swImage);
        jFrame.getContentPane().add(boolean2DPanel, BorderLayout.CENTER);

        jFrame.setVisible(true);
    }
    public static void showCCDialog(boolean[][] swImage, List<Coordinate> circleCenters) {
        JFrame jFrame = new JFrame("Kreiscode, Kreismittelpunkte");
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setSize(600, 600);

        ImagePanel boolean2DPanel = new ImagePanel();
        boolean2DPanel.setImage(swImage);

        for (Coordinate c : circleCenters) {
            boolean2DPanel.addCrosshair(c.getX(), c.getY());
        }

        jFrame.getContentPane().add(boolean2DPanel, BorderLayout.CENTER);

        jFrame.setVisible(true);
    }
    public static void showTrapezialsDialog(boolean[][] swImage, boolean[][] trapezials) {
        JFrame jFrame = new JFrame("Kreiscode, S/W Image");
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setSize(600, 600);

        ImagePanel boolean2DPanel = new ImagePanel();
        boolean2DPanel.setImage(swImage);
        jFrame.getContentPane().add(boolean2DPanel, BorderLayout.CENTER);

        for (int x = 0; x < trapezials.length; x++) {
            for (int y = 0; y < trapezials[0].length; y++) {
                if (trapezials[x][y]) {
                    boolean2DPanel.highlightPixel(x, y);
                }
            }
        }
        jFrame.setVisible(true);
    }
}
