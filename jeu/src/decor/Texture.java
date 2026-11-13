package decor;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Image en ARGB chargee depuis un PNG. */
public record Texture(int largeur, int hauteur, int[] pixels) {
    public static Texture charger(Path fichier) throws IOException {
        BufferedImage image = ImageIO.read(fichier.toFile());
        if (image == null) {
            throw new IOException("Image illisible : " + fichier);
        }
        int l = image.getWidth();
        int h = image.getHeight();
        return new Texture(l, h, image.getRGB(0, 0, l, h, null, 0, l));
    }
}
