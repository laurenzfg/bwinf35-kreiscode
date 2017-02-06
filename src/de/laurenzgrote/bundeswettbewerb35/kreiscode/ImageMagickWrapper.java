package de.laurenzgrote.bundeswettbewerb35.kreiscode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ImageMagickWrapper {

    /**
     * Liest jedes von ImageMagick lesbare Bildformat ein.
     * ImageMagick muss per "convert" aufrufbar sein!
     * Also: Kein Windows, sorry ;-)
     * @param file Einzulesende Datei
     * @return BufferedImage der eingegebenen Datei
     * @throws IOException unwahrscheinlich
     * @throws InterruptedException Fehler bei der Ausführung von ImageMagick
     */
    public static BufferedImage read(File file) throws IOException, InterruptedException {
        // Auf der Bash mit ImageMagick konvertieren
        String command = "convert " + file.getAbsolutePath() + " .kreiscodeConverted.png";

        // Kommando ausführen und auf Beendigung warten
        Process process = Runtime.getRuntime().exec(command);
        process.waitFor();

        // Datei wurde nach out.png geschrieben
        // --> von dort mit ImageIO lesen
        File convertedImage = new File(".kreiscodeConverted.png");
        BufferedImage image = ImageIO.read(convertedImage);

        // Bild nun im Arbeitsspeicher vorhanden, Datei kann gelöscht werden
        Files.delete(convertedImage.toPath());
        return image;
    }
}
