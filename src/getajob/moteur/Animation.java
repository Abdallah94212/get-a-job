package getajob.moteur;

import java.awt.image.BufferedImage;

/**
 * Une animation simple : quelques images qui se succèdent, chacune affichée pendant la même durée.
 * Exemple : la marche du héros = deux images, 0,17 s chacune, en boucle.
 */
public class Animation {
    private final BufferedImage[] images;
    private final double dureeParImage;
    private double temps;

    public Animation(double dureeParImage, BufferedImage... images) {
        this.dureeParImage = dureeParImage;
        this.images = images;
    }

    /** Fait avancer l'animation du temps écoulé. */
    public void avancer(double dt) {
        temps += dt;
    }

    /** Revient à la première image (le personnage s'arrête). */
    public void recommencer() {
        temps = 0;
    }

    /** L'image à montrer maintenant : on compte combien de durées se sont écoulées, modulo le nombre d'images. */
    public BufferedImage imageCourante() {
        int rang = (int) (temps / dureeParImage) % images.length;
        return images[rang];
    }
}
