package getajob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les règles de la nuit : prendre, livrer, être payé, rentrer ; et les deux façons de perdre. */
class TourneeTest {
    private Niveau niveau;
    private Tournee tournee;
    private Joueur joueur;

    @BeforeEach
    void preparer() {
        niveau = new Niveau(new Clavier());
        tournee = new Tournee(niveau);
        niveau.ennemis().clear();
        joueur = niveau.joueur();
    }

    private void tour() {
        niveau.mettreAJour(1.0 / 60);
        tournee.mettreAJour(1.0 / 60);
    }

    /** Pose le héros au comptoir, puis devant le client actuel : une livraison complète. */
    private void faireUneLivraison() {
        joueur.placer(Niveau.COMPTOIR.getCenterX(), 200);
        tour();
        assertTrue(joueur.porteUneCommande(), "on prend la commande au comptoir");
        Client client = tournee.clientActuel();
        joueur.placer(client.x() - 8, client.y());
        tour();
    }

    @Test
    void laNuitCommenceAuMaitrePoulet() {
        assertEquals(Tournee.Etape.CHERCHER_LA_COMMANDE, tournee.etape());
        assertEquals("23:00", tournee.heure());
        assertEquals(Tournee.Issue.EN_COURS, tournee.issue());
    }

    @Test
    void uneLivraisonRapporteQuaranteEuros() {
        faireUneLivraison();
        assertEquals(40, joueur.argent());
        assertEquals(1, tournee.livraisons());
        assertTrue(niveau.clients().get(0).servi());
        assertEquals("Monsieur Diallo", tournee.clientActuel().nom(), "au suivant");
        assertEquals(1, niveau.ennemis().size(), "un racketteur de plus dans l'avenue");
    }

    @Test
    void sansCommandeLeClientNePaiePas() {
        Client client = tournee.clientActuel();
        joueur.placer(client.x() - 8, client.y());
        tour();
        assertEquals(0, joueur.argent());
    }

    @Test
    void troisLivraisonsPuisRentrerCestGagne() {
        faireUneLivraison();
        faireUneLivraison();
        faireUneLivraison();
        assertEquals(120, joueur.argent());
        assertEquals(Tournee.Etape.RENTRER, tournee.etape());
        assertEquals(Tournee.Issue.EN_COURS, tournee.issue(), "il faut encore rentrer chez soi");
        joueur.placer(Niveau.CHEZ_TOI.getCenterX(), 200);
        tour();
        assertEquals(Tournee.Issue.VICTOIRE, tournee.issue());
    }

    @Test
    void sixHeuresSonneCestPerdu() {
        tournee.avancerLHorloge(7 * 60);
        assertEquals("06:00", tournee.heure());
        assertEquals(Tournee.Issue.TROP_TARD, tournee.issue());
    }

    @Test
    void plusDeViesCestPerdu() {
        for (int i = 0; i < Joueur.VIES_AU_DEPART; i++) {
            joueur.etreTouche(true, niveau);
        }
        assertEquals(Tournee.Issue.PLUS_DE_VIES, tournee.issue());
    }
}
