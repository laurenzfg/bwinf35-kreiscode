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
            boolean2DPanel.markCoordinate(c.getX(), c.getY());
        }

        jFrame.getContentPane().add(boolean2DPanel, BorderLayout.CENTER);

        jFrame.setVisible(true);
    }
}
