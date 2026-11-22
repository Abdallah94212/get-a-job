package getajob.moteur;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.Timer;

import getajob.ecrans.Ecran;
import getajob.ecrans.EcranTitre;

/**
 * Le jeu : la zone de dessin de la fenêtre, et la boucle de jeu.
 *
 * La boucle tourne environ 60 fois par seconde (un Timer Swing toutes les 16 ms). À chaque tour :
 *   1. on mesure le temps écoulé depuis le tour précédent (dt, en secondes) ;
 *   2. on met à jour l'écran courant : c'est la logique du jeu (déplacements, collisions, règles) ;
 *   3. on oublie les touches « juste appuyées » (elles ne comptent qu'un tour) ;
 *   4. on redessine.
 * Tout se passe dans le même fil (celui de Swing) : pas de problème de partage de données entre fils.
 *
 * Le jeu dessine dans une petite image de 480 x 270 pixels, agrandie 3 fois sans lissage : c'est le pixel art.
 */
public class Jeu extends JPanel {
    public static final int LARGEUR = 480;
    public static final int HAUTEUR = 270;
    public static final int ZOOM = 3;

    private final BufferedImage image = new BufferedImage(LARGEUR, HAUTEUR, BufferedImage.TYPE_INT_RGB);
    private final Clavier clavier = new Clavier();
    private final Timer boucle = new Timer(16, evenement -> tour());
    private Ecran ecran;
    private long dernierTour;

    public Jeu() {
        setPreferredSize(new Dimension(LARGEUR * ZOOM, HAUTEUR * ZOOM));
        setFocusable(true);
        addKeyListener(clavier);
        ecran = new EcranTitre(this);
    }

    public void demarrer() {
        dernierTour = System.nanoTime();
        boucle.start();
        requestFocusInWindow();
    }

    /** Un tour de boucle. dt est plafonné à 1/20 s : après un blocage, le monde ne fait pas un saut énorme. */
    private void tour() {
        long maintenant = System.nanoTime();
        double dt = Math.min((maintenant - dernierTour) / 1_000_000_000.0, 0.05);
        dernierTour = maintenant;
        ecran.mettreAJour(dt, clavier);
        clavier.finDuTour();
        repaint();
    }

    /** Passe à un autre écran (titre, récit, partie) : c'est ainsi que le jeu change d'état. */
    public void changerEcran(Ecran nouveau) {
        ecran = nouveau;
    }

    public Clavier clavier() {
        return clavier;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D pinceau = image.createGraphics();
        pinceau.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        ecran.dessiner(pinceau);
        pinceau.dispose();
        Graphics2D ecranReel = (Graphics2D) g;
        ecranReel.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        ecranReel.drawImage(image, 0, 0, LARGEUR * ZOOM, HAUTEUR * ZOOM, null);
    }
}
