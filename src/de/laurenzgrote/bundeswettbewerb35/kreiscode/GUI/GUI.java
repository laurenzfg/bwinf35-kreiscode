package de.laurenzgrote.bundeswettbewerb35.kreiscode.GUI;

import de.laurenzgrote.bundeswettbewerb35.kreiscode.*;
import de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing.CircleCenters;
import de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing.ColorToBW;
import de.laurenzgrote.bundeswettbewerb35.kreiscode.ImageProcessing.ImageDecoder;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;

public class GUI extends JFrame {
    // FileFilter für JFilceChooser
    private final FileFilter imageFilter = new FileNameExtensionFilter("Bilddateien", "png", "jpg", "jpeg", "gif");
    private final FileFilter dictFilter = new FileNameExtensionFilter("Wörterbuchdateien", "txt");

    // Buttons in der Swing-Oberfläche
    private final JMenuItem showSW;
    private final JMenuItem showCC;
    private final JMenuItem showTrap;
    private final JButton decodeButton;
    private final JButton selectDictButton;
    private final JLabel selectedDictLabel;
    private final JLabel selectedFileLabel;
    private final JButton selectFileButton;
    private final JCheckBox useImageMagick;
    private final ImagePanel imagePanel;

    // Farbiges Bild von der Platte
    private BufferedImage originalImage;
    private File selectedDict;

    // Versch. Zwischenstufen der Bilddekodierung
    private boolean[][] swImage;
    private CircleCenters circleCenters;
    private ImageDecoder imageDecoder;

    // Konstruktor der die GUI erzeugt
    public GUI() {
        super("Kreiscode - Laurenz Grote");
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLayout(new BorderLayout());

        // JMenu
        JMenuBar jMenuBar = new JMenuBar();
        showSW = new JMenuItem("Zeige S/W-Bild");
        showCC = new JMenuItem("Zeige CircleCenters");
        showTrap = new JMenuItem("Zeige Trapeze");
        jMenuBar.add(showSW);
        jMenuBar.add(showCC);
        jMenuBar.add(showTrap);

        // Center Panel
        imagePanel = new ImagePanel();

        // Einstellungen Panel
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Einstellungen"));
        settingsPanel.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4,4,4,4);

        JLabel dictLabel = new JLabel("Wörterbuch:");
        selectedDictLabel = new JLabel("BwInf-Default");
        selectDictButton = new JButton("Öffnen");

        JLabel fileLabel = new JLabel("Bild:");
        selectedFileLabel = new JLabel(".");
        selectFileButton = new JButton("Öffnen");
        useImageMagick = new JCheckBox("ImageMagick");

        decodeButton = new JButton("Dekodieren");

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        settingsPanel.add(dictLabel, c);

        c.gridx = 2;
        c.gridwidth = 3;
        c.weightx = 0.5;
        settingsPanel.add(selectedDictLabel, c);

        c.gridx = 5;
        c.gridwidth = 1;
        c.weightx = 0;
        settingsPanel.add(selectDictButton, c);

        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        settingsPanel.add(fileLabel, c);

        c.gridx = 2;
        c.gridwidth = 3;
        settingsPanel.add(selectedFileLabel, c);

        c.gridx = 5;
        c.gridwidth = 1;
        settingsPanel.add(selectFileButton, c);

        c.gridy = 3;
        c.gridx = 5;
        settingsPanel.add(useImageMagick, c);

        c.gridx = 0;
        c.gridy = 100;
        c.gridwidth = 100;
        settingsPanel.add(decodeButton, c);

        // Alles ins JFrame
        this.add(jMenuBar, BorderLayout.NORTH);
        this.add(settingsPanel, BorderLayout.PAGE_END);
        this.add(imagePanel, BorderLayout.CENTER);

        // Listeners drauf
        jMenuBarListeners();
        selectFileListener();
        selectDictListener();
        decodeListener();

        setVisible(true); // TADA!
    }

    // Listener für die Menubar
    private void jMenuBarListeners() {
        showSW.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (swImage != null) {
                    CustomDialogs.showSWDialog(swImage);
                }
            }
        });
        showCC.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (circleCenters != null) {
                    CustomDialogs.showCCDialog(swImage, circleCenters.getCircleCenters());
                }
            }
        });
        showTrap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (imageDecoder != null) {
                    CustomDialogs.showTrapezialsDialog(swImage, imageDecoder.getTrapezials());
                }
            }
        });
    }

    // Dekodieren des ausgewählten Bildes
    private void decodeListener() {
        decodeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // Schwarz-Weiß-Bild ermitteln
                swImage = ColorToBW.colorToBW(originalImage);
                // CircleCenters ermitteln
                circleCenters = new CircleCenters(swImage);
                // Mit den Kreismittelpunkten die Beudeutung der Kreiscodes ermitteln
                java.util.List<Coordinate> circleCenters = GUI.this.circleCenters.getCircleCenters();
                // Dekodieren des Bildes
                imageDecoder = new ImageDecoder(swImage, circleCenters, selectedDict);
                // Der Kreismittelpunktliste die Bedeutungen hinzufügen
                circleCenters = imageDecoder.getCircleCentersWithMeanings();

                // Einzeichnen der Ergebnisse in das Bild in der GUI
                for (Coordinate c : circleCenters) {
                    imagePanel.addText(c.getX(), c.getY(), c.getCircleMeaning());
                }
            }
        });
    }

    // Auswahl der Bilddatei
    private void selectFileListener() {
        selectFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileFilter(imageFilter);
                int returnVal = jFileChooser.showOpenDialog(GUI.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jFileChooser.getSelectedFile();
                    try {
                        // Muss der Imagemagick-Wrapper benutzt werden?
                        if (useImageMagick.isSelected()) {
                            originalImage = ImageMagickWrapper.read(file);
                        } else {
                            originalImage = ImageIO.read(file);
                        }
                        // Schreiben des Dateinamens in die GUI
                        selectedFileLabel.setText(file.getName());
                        // Laden des Bildes
                        imagePanel.setImage(originalImage);
                    } catch (IOException|InterruptedException e) {
                        System.err.println("Kritischer Fehler beim Einlesen der Bilddatei");
                        e.printStackTrace();
                    }
                } else {
                    System.err.println("Bildeinlesevorgang durch Nutzer abgebrochen");
                }
            }
        });
    }

    // Auswahl der Wörterbuchdatei
    private void selectDictListener() {
        selectDictButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                final JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setFileFilter(dictFilter);
                int returnVal = jFileChooser.showOpenDialog(GUI.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = jFileChooser.getSelectedFile();
                    // Abspeichern der URL des Wörterbuches in einem Feld
                    selectedDict = file;
                    String filename = file.toString();
                    // Schreiben des Dateinamens in die GUI
                    selectedDictLabel.setText(filename);
                } else {
                    System.err.println("Abbruch");
                }
            }
        });
    }
}
