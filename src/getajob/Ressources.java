package getajob;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;

/**
 * Tout ce que le jeu lit sur le disque, dans le dossier ressources/ : les images, la police, les obstacles, les sons.
 * Chaque image n'est lue qu'une fois, puis gardée en mémoire (un cache) : on la redemande à chaque image du jeu.
 */
public final class Ressources {
    private static final Path DOSSIER = Path.of("ressources");
    private static final Map<String, BufferedImage> IMAGES = new HashMap<>();
    private static Font police;

    private Ressources() {
    }

    /** L'image ressources/images/nom.png. */
    public static BufferedImage image(String nom) {
        return IMAGES.computeIfAbsent(nom, n -> {
            try {
                return ImageIO.read(DOSSIER.resolve("images").resolve(n + ".png").toFile());
            } catch (IOException e) {
                throw new UncheckedIOException("Image introuvable : " + n, e);
            }
        });
    }

    /** La police du jeu (Barlow Condensed), à la taille voulue en pixels. */
    public static Font police(float taille) {
        if (police == null) {
            try {
                police = Font.createFont(Font.TRUETYPE_FONT, DOSSIER.resolve("polices/BarlowCondensed-SemiBold.ttf").toFile());
            } catch (Exception e) {
                police = new Font(Font.SANS_SERIF, Font.BOLD, 12);    // sans le fichier, une police du système
            }
        }
        return police.deriveFont(taille);
    }

    /** Les obstacles du décor : une ligne "x1 y1 x2 y2" par rectangle, au sol. Les lignes en # sont des commentaires. */
    public static List<Rectangle> obstacles() {
        List<Rectangle> obstacles = new ArrayList<>();
        try {
            for (String ligne : Files.readAllLines(DOSSIER.resolve("obstacles.txt"))) {
                if (ligne.isBlank() || ligne.startsWith("#")) {
                    continue;
                }
                String[] n = ligne.trim().split("\\s+");
                int x1 = Integer.parseInt(n[0]);
                int y1 = Integer.parseInt(n[1]);
                int x2 = Integer.parseInt(n[2]);
                int y2 = Integer.parseInt(n[3]);
                obstacles.add(new Rectangle(x1, y1, x2 - x1, y2 - y1));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("obstacles.txt introuvable", e);
        }
        return obstacles;
    }

    /** Joue ressources/sons/nom.wav une fois. Sans carte son (ou pendant les tests), le jeu continue en silence. */
    public static void jouerSon(String nom) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(DOSSIER.resolve("sons").resolve(nom + ".wav").toFile()));
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    clip.close();                      // le son est fini : on rend la ressource au système
                }
            });
            clip.start();
        } catch (Exception e) {
            // un son qui ne se joue pas ne doit jamais arrêter la partie
        }
    }
}
