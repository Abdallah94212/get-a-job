package getajob.ecrans;

import java.awt.Color;
import java.awt.Graphics2D;

import getajob.moteur.Clavier;
import getajob.moteur.Jeu;
import getajob.moteur.Ressources;

/**
 * Un écran du jeu : le titre, un récit, ou la partie. Le jeu n'en affiche qu'un à la fois ; passer de l'un à l'autre,
 * c'est changer d'état (Jeu.changerEcran). Chaque écran sait se mettre à jour et se dessiner à sa façon.
 */
public abstract class Ecran {
    protected static final Color ORANGE = new Color(240, 122, 26);
    protected static final Color BLANC = new Color(236, 232, 223);
    protected static final Color GRIS = new Color(150, 156, 172);
    protected static final Color NUIT = new Color(18, 20, 28);

    protected final Jeu jeu;

    protected Ecran(Jeu jeu) {
        this.jeu = jeu;
    }

    public abstract void mettreAJour(double dt, Clavier clavier);

    public abstract void dessiner(Graphics2D g);

    /** Écrit un texte centré horizontalement (y est la ligne de base du texte). */
    protected static void ecrireAuCentre(Graphics2D g, String texte, int y, float taille, Color couleur) {
        g.setFont(Ressources.police(taille));
        int largeur = g.getFontMetrics().stringWidth(texte);
        g.setColor(couleur);
        g.drawString(texte, (Jeu.LARGEUR - largeur) / 2, y);
    }
}
