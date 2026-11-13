package decor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/** Enregistre des images du decor seul, sans ouvrir de fenetre, pour le verifier d'un coup d'oeil. */
public final class Capture {
    private Capture() {
    }

    /** Arguments : dossier des cartes, dossier de sortie, puis des positions "carte:x,y". */
    public static void main(String[] args) throws IOException {
        Path dossier = Path.of(args[0]);
        Path sortie = Path.of(args[1]);
        Files.createDirectories(sortie);
        Rendu rendu = new Rendu();
        for (int i = 2; i < args.length; i++) {
            String[] p = args[i].split("[:,]");
            Scene scene = Scene.charger(dossier, p[0] + ".niveau");
            Acteur heros = new Acteur("toi", scene.joueur, Float.parseFloat(p[1]), Float.parseFloat(p[2]));
            float max = Math.max(scene.xMin - 12, scene.xMax + 12 - Rendu.L);
            float camX = Math.max(scene.xMin - 12, Math.min(max, heros.x - Rendu.L / 2f + 40));
            rendu.dessiner(scene, Math.round(camX), 8000, List.of(heros));
            enregistrer(rendu.image, sortie.resolve(String.format("vue%02d.png", i - 1)), 2);
        }
    }

    /** Ecrit une image du jeu dans un fichier PNG, agrandie zoom fois. */
    public static void enregistrer(int[] pixels, Path fichier, int zoom) throws IOException {
        BufferedImage image = new BufferedImage(Rendu.L * zoom, Rendu.H * zoom, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < Rendu.H * zoom; y++) {
            for (int x = 0; x < Rendu.L * zoom; x++) {
                image.setRGB(x, y, pixels[(y / zoom) * Rendu.L + x / zoom]);
            }
        }
        ImageIO.write(image, "png", fichier.toFile());
    }
}
