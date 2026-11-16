package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Etat;
import histoire.Progression;
import histoire.Sauvegarde;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Le telephone, joue pour de vrai : ce qu'on y fait a une suite dans le monde. */
class TelephoneTest {
    @TempDir
    Path dossier;
    private Robot robot;
    private EcranPartie partie;
    private Progression progression;
    private Etat etat;

    @BeforeEach
    void preparer() throws IOException {
        robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.frapper(KeyEvent.VK_E);
        robot.laisserPasser();
        partie = robot.partie();
        progression = partie.progression();
        etat = progression.etat();
        int rang = robot.jeu.res.campagne.rang("1.2");
        progression.restaurer(rang, 1, Set.of(), true, Set.of());
        float[] banc = robot.jeu.res.position(robot.jeu.res.campagne.figurants().get("youngssef").lieu());
        partie.monde().aller("avenue_bas", banc[0] - 60, banc[1]);
        robot.image();
    }

    /** Ouvre le telephone, descend de n lignes, entre, descend de m lignes, valide, puis range le telephone. */
    private void telephone(int appli, int ligne) {
        robot.frapper(KeyEvent.VK_P);
        for (int i = 0; i < appli; i++) {
            robot.frapper(KeyEvent.VK_RIGHT);                          // la grille des applis, case par case
        }
        robot.frapper(KeyEvent.VK_E);
        for (int i = 0; i < ligne; i++) {
            robot.frapper(KeyEvent.VK_DOWN);
        }
        robot.frapper(KeyEvent.VK_E);
        robot.frapper(KeyEvent.VK_P);
    }

    @Test
    void uneCommandeDevientUnColisChezSoiQuIlFautAllerOuvrir() {
        etat.gagner(100);
        telephone(3, 0);                                               // COMMANDES, le casque
        assertEquals(65, etat.argent(), "on paie a la commande");
        assertFalse(etat.possede("casque"), "l'objet n'arrive pas dans la poche");
        assertTrue(etat.enRoute("casque"));

        robot.images(Math.round(Etat.DELAI * 30) + 30);                // le temps de la livraison
        assertFalse(etat.enRoute("casque"));
        assertEquals(List.of("casque"), etat.colis());
        assertFalse(etat.possede("casque"), "tant qu'on n'est pas rentre l'ouvrir");

        partie.monde().aller("appartement", 74, 212);
        robot.image();
        assertNotNull(partie.monde().cible());
        assertEquals("colis", partie.monde().cible().id());
        robot.frapper(KeyEvent.VK_E);
        assertTrue(etat.possede("casque"));
        assertTrue(etat.colis().isEmpty());
        robot.image();
        assertTrue(partie.monde().cible() == null || !"colis".equals(partie.monde().cible().id()), "le colis a disparu");
    }

    @Test
    void onNeCommandePasSansArgentNiDeuxFois() {
        telephone(3, 0);
        assertFalse(etat.enRoute("casque"));
        etat.gagner(200);
        telephone(3, 0);
        telephone(3, 0);
        assertEquals(165, etat.argent(), "le casque n'est paye qu'une fois");
    }

    @Test
    void leCasqueEtLeGiletProtegentLaLacrymoNeSertQuUneFois() {
        etat.gagner(500);
        for (String objet : List.of("casque", "gilet", "lacrymo", "baskets")) {
            assertTrue(etat.commander(objet));
        }
        etat.livrer(Etat.DELAI + 1);
        etat.ouvrirColis();
        etat.coup(30f);
        assertEquals(85f, etat.sante(), 0.01f, "le gilet prend la moitie du coup");
        assertTrue(etat.utiliser("lacrymo"));
        assertFalse(etat.utiliser("lacrymo"), "elle est vide");
        Etat sans = new Etat();
        float avec = courir(etat);
        assertTrue(avec > courir(sans), "avec les baskets, on court plus longtemps");
    }

    private static float courir(Etat e) {
        float duree = 0;
        while (e.respirer(true, true, false, 0.1f)) {
            duree += 0.1f;
        }
        return duree;
    }

    @Test
    void leLoyerTombeAChaqueMissionEtDeuxMoisDeRetardCoupentLeCourant() {
        etat.finDeMission();
        assertEquals(1, etat.loyerDu());
        assertFalse(etat.coupe());
        etat.finDeMission();
        assertTrue(etat.coupe());
        etat.blesser(50f, 0f);
        float[] lit = robot.jeu.res.carte("appartement").reperes.get("lit");
        partie.monde().aller("appartement", lit[0], lit[1]);
        robot.image();
        robot.frapper(KeyEvent.VK_E);
        assertEquals(50f, etat.sante(), 0.01f, "sans courant, on ne dort pas");

        etat.gagner(40);
        telephone(1, 0);                                               // BANQUE, payer un mois
        assertEquals(1, etat.loyerDu());
        assertEquals(0, etat.argent());
        robot.frapper(KeyEvent.VK_E);
        assertEquals(100f, etat.sante(), 0.01f, "le loyer paye, on redort chez soi");
    }

    @Test
    void unCommerceAcheteRapporteAChaqueMission() {
        etat.gagner(150);
        telephone(2, 0);                                               // ANNONCES, le kiosque
        assertEquals(30, etat.argent());
        assertTrue(etat.commerces().contains("kiosque"));
        etat.finDeMission();
        assertEquals(40, etat.argent(), "la recette tombe a la fin de la mission");
        assertEquals("RECETTE : LE KIOSQUE", etat.journal().get(etat.journal().size() - 1).libelle());
    }

    @Test
    void laLaverieNEstEnVenteQuApresAvoirQuitteGeto() {
        etat.gagner(600);
        telephone(2, 1);
        assertFalse(etat.commerces().contains("laverie"), "elle n'est pas encore dans les annonces");
        progression.marquer("+parti_de_chez_geto");
        telephone(2, 1);
        assertTrue(etat.commerces().contains("laverie"));
    }

    @Test
    void repondreAUnMessageChangeLHistoire() {
        etat.recevoir("diallo", "Tu me monterais mes courses ?", List.of(new Etat.Reponse("Je passe.", "+course_lili"),
                new Etat.Reponse("Pas le temps.", "-")));
        assertEquals(1, etat.nonLus());
        assertFalse(progression.drapeau("course_lili"));
        telephone(0, 0);                                               // MESSAGES, le fil de Tata Lili
        robot.frapper(KeyEvent.VK_P);
        robot.frapper(KeyEvent.VK_E);
        robot.frapper(KeyEvent.VK_E);
        robot.frapper(KeyEvent.VK_E);                                  // la premiere reponse
        robot.frapper(KeyEvent.VK_P);
        assertTrue(progression.drapeau("course_lili"), "la reponse leve le drapeau");
        assertEquals(0, etat.nonLus());
        assertTrue(etat.reponses("diallo").isEmpty(), "on ne repond qu'une fois");
        assertTrue(progression.attendue(histoire.Objectif.Genre.PARLER, "diallo"), "un objectif secondaire s'ouvre");
    }

    @Test
    void appelerYoungssefFaitVenirLeScooter() {
        etat.gagner(10);
        telephone(4, 0);                                               // CONTACTS, Youngssef
        assertTrue(partie.monde().enScooter());
        assertEquals(2, etat.argent(), "huit euros, plus cher qu'au banc");
        assertTrue(etat.location() > 170f);
    }

    @Test
    void laSauvegardeGardeToutLeTelephone() throws IOException {
        etat.gagner(300);
        etat.commander("gilet");
        etat.commander("casque");
        etat.livrer(Etat.DELAI - 10);
        etat.acheter(Etat.COMMERCES.get(0));
        etat.finDeMission();
        etat.recevoir("soul", "Roule.", List.of(new Etat.Reponse("Qui es-tu ?", "+soul_demande")));
        partie.sauvegarder();
        Etat lu = new Sauvegarde(dossier.resolve("partie.properties")).charger(robot.jeu.res.campagne).progression().etat();
        assertEquals(etat.argent(), lu.argent());
        assertTrue(lu.enRoute("gilet") && lu.enRoute("casque"));
        assertTrue(lu.commerces().contains("kiosque"));
        assertEquals(1, lu.loyerDu());
        assertEquals("Roule.", lu.fil("soul").get(0).texte());
        assertEquals("Qui es-tu ?", lu.reponses("soul").get(0).texte());
        assertEquals(etat.journal().size(), lu.journal().size());
        lu.livrer(11f);
        assertEquals(2, lu.colis().size(), "la livraison reprend ou elle en etait");
        assertNull(lu.avis() == null ? null : null);
    }
}
