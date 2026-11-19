package getajob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les racketteurs : leur ronde, la poursuite, le coup de poing qui les étourdit, et ce qui arrive quand ils attrapent le héros. */
class CombatTest {
    private Clavier clavier;
    private Niveau niveau;
    private Tournee tournee;
    private Joueur joueur;
    private Ennemi ennemi;

    @BeforeEach
    void preparer() {
        clavier = new Clavier();
        niveau = new Niveau(clavier);
        tournee = new Tournee(niveau);
        niveau.ennemis().clear();
        niveau.ajouterEnnemi(1000, 1200, 226);
        ennemi = niveau.ennemis().get(0);
        joueur = niveau.joueur();
    }

    private void tour() {
        niveau.mettreAJour(1.0 / 60);
        tournee.mettreAJour(1.0 / 60);
        clavier.finDuTour();
    }

    @Test
    void seulIlFaitSaRonde() {
        joueur.placer(300, 200);                       // très loin
        for (int i = 0; i < 120; i++) {
            tour();
        }
        assertEquals(Ennemi.Etat.PATROUILLE, ennemi.etat());
        assertTrue(ennemi.x() > 1000, "il avance vers la fin de sa ronde : " + ennemi.x());
    }

    @Test
    void ilPoursuitLeHerosQuiApproche() {
        joueur.placer(1080, 226);
        tour();
        assertEquals(Ennemi.Etat.POURSUITE, ennemi.etat());
    }

    @Test
    void unCoupDePoingLEtourditEtLeRepousse() {
        ennemi.placer(1108, 226);
        joueur.placer(1085, 226);                      // à sa gauche, à portée de poing mais pas encore attrapé
        clavier.appuyer(KeyEvent.VK_SPACE);
        tour();
        assertEquals(Joueur.VIES_AU_DEPART, joueur.vies(), "il a frappé avant d'être attrapé");
        assertEquals(Ennemi.Etat.ETOURDI, ennemi.etat());
        assertTrue(ennemi.x() > 1120, "repoussé vers la droite : " + ennemi.x());
        assertFalse(ennemi.peutAttraper(), "étourdi, il ne peut plus attraper personne");
    }

    @Test
    void attrapeIlPrendLaCommandeEtUneVie() {
        joueur.prendreCommande();
        joueur.placer(1100, 226);
        ennemi.placer(1104, 226);
        tour();
        assertEquals(Joueur.VIES_AU_DEPART - 1, joueur.vies());
        assertFalse(joueur.porteUneCommande(), "le sac est perdu");
        assertFalse(joueur.peutEtreTouche(), "il clignote : intouchable un instant");
        tour();
        assertEquals(Joueur.VIES_AU_DEPART - 1, joueur.vies(), "pas deux vies d'un coup");
    }
}
