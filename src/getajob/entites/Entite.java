package getajob.entites;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import getajob.monde.Niveau;
import getajob.moteur.Camera;

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

    /** Dessine l'image debout sur ses pieds. */
    public void dessiner(Graphics2D g, Camera camera) {
        BufferedImage img = image();
        g.drawImage(img, ecranX(img, camera), ecranY(img), null);
    }

    /** Bord gauche de l'image à l'écran : centrée sur les pieds, décalée par la caméra (passage du monde à l'écran). */
    protected int ecranX(BufferedImage img, Camera camera) {
        return (int) Math.round(x) - img.getWidth() / 2 - camera.x();
    }

    /** Bord haut de l'image à l'écran : le bas de l'image touche le sol, en y. */
    protected int ecranY(BufferedImage img) {
        return (int) Math.round(y) - img.getHeight();
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
