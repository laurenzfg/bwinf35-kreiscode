package de.laurenzgrote.bundeswettbewerb35.kreiscode.GUI;

import de.laurenzgrote.bundeswettbewerb35.kreiscode.Coordinate;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CustomDialogs {

    private static JFrame get2DDisplay(String title, boolean[][] image) {
        JFrame jFrame = new JFrame(title);
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        jFrame.setSize(600, 600);
        ImagePanel boolean2DPanel = new ImagePanel();
        boolean2DPanel.setImage(image);
        jFrame.getContentPane().add(boolean2DPanel, BorderLayout.CENTER);
        return jFrame;
    }

    public static void showSWDialog(boolean[][] swImage) {
        JFrame jFrame = get2DDisplay("S/W-Bild", swImage);
        jFrame.setVisible(true);
    }

    public static void showCCDialog(boolean[][] swImage, List<Coordinate> circleCenters) {
        JFrame jFrame = get2DDisplay("CircleCenters", swImage);
        ImagePanel ip = (ImagePanel) jFrame.getContentPane().getComponent(0);
        for (Coordinate c : circleCenters) {
            ip.addCrosshair(c.getX(), c.getY());
        }

        jFrame.setVisible(true);
    }
    public static void showTrapezialsDialog(boolean[][] swImage, boolean[][] trapezials) {
        JFrame jFrame = get2DDisplay("Trapeze", swImage);
        ImagePanel ip = (ImagePanel) jFrame.getContentPane().getComponent(0);

        // Iterieren übers ganze Bild und da wo Trapzepixel sind anmalen!
        int width = swImage.length;
        int height = swImage[0].length;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (trapezials[x][y]) {
                    ip.highlightPixel(x, y);
                }
            }
        }
        jFrame.setVisible(true);
    }
}
