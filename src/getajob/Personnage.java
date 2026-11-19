package getajob;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Une entité qui marche : elle a deux images de marche, regarde à gauche ou à droite, et ne traverse pas les obstacles.
 * Le héros, les racketteurs et les clients sont des personnages ; ce qui les distingue, c'est QUI décide de leurs pas
 * (le clavier, une petite intelligence, ou personne) : c'est dans leurs sous-classes.
 */
public abstract class Personnage extends Entite {
    private final Animation marche;
    protected boolean versDroite = true;
    protected boolean enMarche;

    protected Personnage(double x, double y, String sprite) {
        super(x, y);
        marche = new Animation(0.17, Ressources.image(sprite), Ressources.image(sprite + "_2"));
    }

    /**
     * Essaie de se déplacer de (dx, dy). On teste chaque axe séparément : si x est bloqué par un obstacle mais pas y,
     * le personnage glisse le long de l'obstacle au lieu de rester collé.
     */
    protected void deplacer(double dx, double dy, Niveau niveau) {
        if (dx != 0 && !niveau.bloque(piedsEn(x + dx, y))) {
            x += dx;
        }
        if (dy != 0 && !niveau.bloque(piedsEn(x, y + dy))) {
            y += dy;
        }
        if (dx != 0) {
            versDroite = dx > 0;
        }
    }

    /** Le petit rectangle des pieds, s'ils étaient en (px, py) : c'est lui qu'on teste contre les obstacles. */
    private Rectangle piedsEn(double px, double py) {
        return new Rectangle((int) px - 6, (int) py - 3, 12, 4);
    }

    /** Fait marcher (ou arrête) l'animation selon que le personnage bouge ce tour-ci. */
    protected void animer(double dt, boolean bouge) {
        enMarche = bouge;
        if (bouge) {
            marche.avancer(dt);
        } else {
            marche.recommencer();
        }
    }

    @Override
    protected BufferedImage image() {
        return marche.imageCourante();
    }

    /** Une ombre au sol, puis l'image, retournée si le personnage regarde à gauche. */
    @Override
    public void dessiner(Graphics2D g, Camera camera) {
        BufferedImage img = image();
        int ecranX = (int) Math.round(x) - img.getWidth() / 2 - camera.x();
        int ecranY = (int) Math.round(y) - img.getHeight();
        g.setColor(new Color(0, 0, 0, 90));
        g.fillOval((int) Math.round(x) - 9 - camera.x(), (int) Math.round(y) - 3, 18, 5);
        if (versDroite) {
            g.drawImage(img, ecranX, ecranY, null);
        } else {
            g.drawImage(img, ecranX + img.getWidth(), ecranY, -img.getWidth(), img.getHeight(), null);   // largeur négative : miroir
        }
    }
}
