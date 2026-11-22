package getajob.ecrans;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.util.List;

import getajob.monde.Niveau;
import getajob.moteur.Clavier;
import getajob.moteur.Jeu;
import getajob.moteur.Ressources;

/** L'écran titre : l'avenue défile lentement derrière le nom du jeu. Entrée lance l'histoire, Échap quitte. */
public class EcranTitre extends Ecran {
    private double defilement;
    private double temps;

    public EcranTitre(Jeu jeu) {
        super(jeu);
    }

    @Override
    public void mettreAJour(double dt, Clavier clavier) {
        temps += dt;
        defilement = (defilement + dt * 12) % (Niveau.LARGEUR - Jeu.LARGEUR);
        if (clavier.vientDAppuyer(KeyEvent.VK_ENTER, KeyEvent.VK_SPACE)) {
            Ressources.jouerSon("ui_valide");
            jeu.changerEcran(new EcranRecit(jeu, "VENDREDI, 23 H. LE KREMLIN-BICÊTRE.", List.of(
                    "Trois mois de loyer en retard : 120 euros.",
                    "À 6 heures, la régie coupe le courant.",
                    "Youngssef, le livreur du Maître Poulet, s'est tordu la cheville.",
                    "Ce soir, ses livraisons sont pour toi : 40 euros la course.",
                    "Mais des racketteurs traînent dans l'avenue. Ils visent les livreurs.",
                    "Trois livraisons. Rentrer avant l'aube. Payer."),
                    "FLÈCHES OU ZQSD : MARCHER     ESPACE : REPOUSSER     ÉCHAP : PAUSE",
                    new EcranPartie(jeu)));
        } else if (clavier.vientDAppuyer(KeyEvent.VK_ESCAPE)) {
            System.exit(0);
        }
    }

    @Override
    public void dessiner(Graphics2D g) {
        g.drawImage(Ressources.image("avenue"), -(int) defilement, 0, null);
        g.setColor(new Color(18, 20, 28, 150));
        g.fillRect(0, 0, Jeu.LARGEUR, Jeu.HAUTEUR);
        ecrireAuCentre(g, "GET A JOB", 110, 64, ORANGE);
        ecrireAuCentre(g, "UNE NUIT POUR PAYER LE LOYER", 136, 16, BLANC);
        if ((int) (temps * 2) % 2 == 0) {                      // le texte clignote doucement
            ecrireAuCentre(g, "ENTRÉE : JOUER", 200, 14, GRIS);
        }
        ecrireAuCentre(g, "ÉCHAP : QUITTER", 250, 11, GRIS);
    }
}
