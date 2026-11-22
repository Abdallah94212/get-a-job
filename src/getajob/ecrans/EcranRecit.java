package getajob.ecrans;

import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

import getajob.moteur.Clavier;
import getajob.moteur.Jeu;

/**
 * Un écran de récit : un titre, puis des phrases qui apparaissent une à une sur fond noir.
 * Il sert pour l'introduction et pour les trois fins (victoire, plus de vies, trop tard). Entrée : écran suivant.
 */
public class EcranRecit extends Ecran {
    private static final double DELAI_ENTRE_LES_LIGNES = 1.1;

    private final String titre;
    private final List<String> lignes;
    private final String aide;
    private final Ecran suivant;
    private double temps;

    public EcranRecit(Jeu jeu, String titre, List<String> lignes, String aide, Ecran suivant) {
        super(jeu);
        this.titre = titre;
        this.lignes = lignes;
        this.aide = aide;
        this.suivant = suivant;
    }

    @Override
    public void mettreAJour(double dt, Clavier clavier) {
        temps += dt;
        if (clavier.vientDAppuyer(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)) {
            if (lignesVisibles() < lignes.size()) {
                temps = lignes.size() * DELAI_ENTRE_LES_LIGNES;   // tout afficher d'un coup
            } else {
                jeu.changerEcran(suivant);
            }
        }
    }

    private int lignesVisibles() {
        return Math.min(lignes.size(), (int) (temps / DELAI_ENTRE_LES_LIGNES) + 1);
    }

    @Override
    public void dessiner(Graphics2D g) {
        g.setColor(NUIT);
        g.fillRect(0, 0, Jeu.LARGEUR, Jeu.HAUTEUR);
        ecrireAuCentre(g, titre, 54, 20, ORANGE);
        int y = 90;
        for (int i = 0; i < lignesVisibles(); i++) {
            ecrireAuCentre(g, lignes.get(i), y, 14, BLANC);
            y += 20;
        }
        if (lignesVisibles() == lignes.size()) {
            ecrireAuCentre(g, aide, 236, 11, GRIS);
            ecrireAuCentre(g, "ENTRÉE : CONTINUER", 254, 11, GRIS);
        }
    }
}
