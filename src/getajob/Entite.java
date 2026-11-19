package getajob;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Tout ce qui existe dans la rue : le héros, les racketteurs, les clients. C'est la classe mère de tous les objets du jeu.
 *
 * La rue est vue de côté, en « 2,5D » : x va de gauche à droite le long de l'avenue, y va du trottoir (en haut)
 * vers la chaussée (en bas). La position (x, y) d'une entité est celle de ses PIEDS, au sol.
 * C'est pour cela que les collisions se calculent avec des rectangles au sol : deux personnages se touchent
 * quand leurs pieds sont proches, pas quand leurs têtes se superposent à l'écran.
 *
 * Chaque sous-classe dit comment elle se met à jour (mettreAJour) et quelle image elle montre (image) :
 * le jeu les traite toutes de la même façon, c'est le polymorphisme.
 */
public abstract class Entite {
    protected double x;
    protected double y;

    protected Entite(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /** Ce que fait l'entité pendant dt secondes. Elle connaît le niveau pour savoir ce qui l'entoure. */
    public abstract void mettreAJour(double dt, Niveau niveau);

    /** L'image à dessiner en ce moment. */
    protected abstract BufferedImage image();

    /** La zone de contact au sol : sert à savoir si deux entités se touchent. */
    public Rectangle contact() {
        return new Rectangle((int) x - 9, (int) y - 6, 18, 8);
    }

    public boolean touche(Entite autre) {
        return contact().intersects(autre.contact());
    }

    /** Dessine l'image debout sur ses pieds : centrée en x, le bas de l'image en y. La caméra décale tout le monde. */
    public void dessiner(Graphics2D g, Camera camera) {
        BufferedImage img = image();
        int ecranX = (int) Math.round(x) - img.getWidth() / 2 - camera.x();
        int ecranY = (int) Math.round(y) - img.getHeight();
        g.drawImage(img, ecranX, ecranY, null);
    }

    /** Pose l'entité ailleurs (le début de la partie, les tests). */
    public void placer(double nouveauX, double nouveauY) {
        x = nouveauX;
        y = nouveauY;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }
}
