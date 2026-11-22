package getajob.entites;

import java.awt.Color;
import java.awt.Graphics2D;

import getajob.monde.Niveau;
import getajob.moteur.Camera;

/**
 * Un racketteur. Il fait des allers-retours sur sa portion d'avenue ; s'il voit le héros d'assez près, il le poursuit.
 * Un coup de poing l'étourdit deux secondes : il ne peut plus rien faire, et on en profite pour passer.
 *
 * Son comportement est une petite machine à états : à chaque tour, il est dans UN état, et son état décide de ce qu'il fait.
 */
public class Ennemi extends Personnage {
    public enum Etat { PATROUILLE, POURSUITE, ETOURDI }

    private static final double VITESSE_PATROUILLE = 40;
    private static final double VITESSE_POURSUITE = 68;      // moins vite que le héros (95) : on peut toujours le semer
    private static final double PORTEE_DE_VUE = 110;          // en x
    private static final double HAUTEUR_DE_VUE = 40;          // en y : il ne voit que sur son trottoir
    private static final double DUREE_ETOURDI = 2.0;          // après un coup de poing
    private static final double PAUSE_APRES_ATTRAPE = 1.0;    // le temps que le héros s'enfuie
    private static final double RECUL = 22;                   // en pixels, sous un coup de poing

    private final double debutRonde;
    private final double finRonde;
    private Etat etat = Etat.PATROUILLE;
    private double etourdi;
    private boolean versLaFin = true;

    /** Un racketteur qui fait sa ronde entre debutRonde et finRonde, à la profondeur y. */
    public Ennemi(double debutRonde, double finRonde, double y) {
        super(debutRonde, y, "p_gars_geto");
        this.debutRonde = debutRonde;
        this.finRonde = finRonde;
    }

    @Override
    public void mettreAJour(double dt, Niveau niveau) {
        Joueur joueur = niveau.joueur();
        boolean leVoit = Math.abs(joueur.x() - x) < PORTEE_DE_VUE && Math.abs(joueur.y() - y) < HAUTEUR_DE_VUE;
        // la machine à états : ce qu'il fait dépend de son état, et chaque état dit quand on en sort
        switch (etat) {
            case PATROUILLE -> {
                faireSaRonde(dt, niveau);
                if (leVoit) {
                    etat = Etat.POURSUITE;
                }
            }
            case POURSUITE -> {
                poursuivre(joueur, dt, niveau);
                if (!leVoit) {
                    etat = Etat.PATROUILLE;
                }
            }
            case ETOURDI -> {
                animer(dt, false);
                etourdi -= dt;
                if (etourdi <= 0) {
                    etat = Etat.PATROUILLE;
                }
            }
        }
    }

    /** Il va droit sur le héros, sur les deux axes. */
    private void poursuivre(Joueur joueur, double dt, Niveau niveau) {
        double dx = Math.signum(joueur.x() - x) * VITESSE_POURSUITE * dt;
        double dy = Math.abs(joueur.y() - y) > 2 ? Math.signum(joueur.y() - y) * VITESSE_POURSUITE * 0.6 * dt : 0;
        deplacer(dx, dy, niveau);
        animer(dt, true);
    }

    /** Il marche vers un bout de sa ronde, puis repart vers l'autre. */
    private void faireSaRonde(double dt, Niveau niveau) {
        double but = versLaFin ? finRonde : debutRonde;
        if (Math.abs(but - x) < 4) {
            versLaFin = !versLaFin;
        }
        deplacer(Math.signum(but - x) * VITESSE_PATROUILLE * dt, 0, niveau);
        animer(dt, true);
    }

    /** Le héros l'a frappé : il recule et reste étourdi. */
    public void recevoirCoup(boolean venantDeLaGauche, Niveau niveau) {
        etat = Etat.ETOURDI;
        etourdi = DUREE_ETOURDI;
        deplacer(venantDeLaGauche ? RECUL : -RECUL, 0, niveau);
        versDroite = !venantDeLaGauche;                       // il fait face à celui qui l'a frappé
    }

    /** Il vient d'attraper le héros : il s'arrête un instant (le temps que le héros s'enfuie). */
    public void apresAvoirAttrape() {
        etat = Etat.ETOURDI;
        etourdi = PAUSE_APRES_ATTRAPE;
    }

    public boolean peutAttraper() {
        return etat != Etat.ETOURDI;
    }

    public Etat etat() {
        return etat;
    }

    @Override
    public void dessiner(Graphics2D g, Camera camera) {
        super.dessiner(g, camera);
        if (etat == Etat.ETOURDI) {                          // trois petites étoiles qui tournent au-dessus de sa tête
            g.setColor(new Color(255, 220, 90));
            for (int i = 0; i < 3; i++) {
                double angle = etourdi * 6 + i * 2.1;
                int ex = (int) (x + Math.cos(angle) * 8) - camera.x();
                int ey = (int) (y - 52 + Math.sin(angle) * 2);
                g.fillRect(ex, ey, 2, 2);
            }
        }
    }
}
