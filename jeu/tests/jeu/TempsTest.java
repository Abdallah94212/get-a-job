package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Calendrier;
import histoire.Etat;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le temps et la continuite entre les missions : chaque mission a son jour et son heure, l'ecart se voit, et apres une
 * nuit ou plus on se reveille chez soi ; le scooter emprunte retourne chez Youngssef ; tombe a scooter, on le retrouve
 * la ou on est tombe ; une arme posee reste par terre, meme apres une sauvegarde.
 */
class TempsTest {
    @TempDir
    Path dossier;
    private Robot robot;
    private EcranPartie partie;

    @BeforeEach
    void commencer() throws Exception {
        robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        robot.laisserPasser();
        partie = robot.partie();
    }

    /** Pose la partie juste avant le debut de cette mission, au milieu de la rue, a cette heure. */
    private void avant(String mission, int jour, int minutes, Set<String> drapeaux) {
        partie.progression().restaurer(robot.jeu.res.campagne.rang(mission), 0, Set.of(), false, drapeaux);
        partie.progression().etat().calendrier().regler(jour, minutes);
        partie.monde().aller("avenue_milieu", 1400, 200);
    }

    @Test
    void chaqueMissionDitQuandElleSePasseEtLeTempsNeReculeJamais() {
        int avant = -1;
        for (histoire.Mission m : robot.jeu.res.campagne.missions()) {
            assertTrue(m.jour() > 0, "la mission " + m.id() + " dit son jour et son heure");
            int t = m.jour() * Calendrier.JOURNEE + m.heure();
            assertTrue(t > avant, "le temps avance de " + m.id() + " a la suivante");
            avant = t;
        }
        assertEquals("VENDREDI", Calendrier.nomDuJour(1));
        assertEquals("VENDREDI", Calendrier.nomDuJour(29), "quatre semaines plus tard, c'est encore un vendredi");
        assertEquals("LE LENDEMAIN SOIR", Calendrier.delai(2, 3, 22 * 60, 20 * 60));
        assertEquals("TROIS SEMAINES PLUS TARD", Calendrier.delai(2, 22, 20 * 60, 20 * 1440));
    }

    @Test
    void apresUneNuitOnSeReveilleChezSoiEtLeTempsQuiPasseSeVoit() {
        avant("1.3", 2, 2 * 60 + 30, Set.of());                       // 1.2 finie a 2 h 30 ; la 1.3 est le samedi a 22 h
        robot.image();
        assertTrue(partie.auCalendrier(), "l'ecran du temps qui passe s'affiche");
        assertEquals("appartement", partie.monde().carte(), "on se reveille chez soi, pas au milieu de la rue");
        Calendrier cal = partie.progression().etat().calendrier();
        assertEquals(2, cal.jour());
        assertEquals("22:00", cal.horloge());
        robot.images(50);
        robot.frapper(KeyEvent.VK_E);
        assertFalse(partie.auCalendrier());
        robot.laisserPasser();
        assertEquals("appartement", partie.monde().carte(), "la scene d'ouverture ne nous teleporte plus chez Geto");
        assertTrue(partie.libre());
    }

    @Test
    void quelquesMinutesPlusTardOnResteOuLOnEst() {
        avant("1.2", 2, 0, Set.of());                                  // la 1.2 suit la 1.1 de trente minutes
        robot.image();
        assertFalse(partie.auCalendrier());
        assertEquals("avenue_milieu", partie.monde().carte());
        assertEquals("00:30", partie.progression().etat().calendrier().horloge());
    }

    @Test
    void leScooterEmprunteRetourneChezYoungssefApresUneNuit() {
        avant("1.3", 2, 3 * 60, Set.of("scooter_gare"));
        partie.progression().etat().garer("avenue_milieu", 1500, 200);
        robot.image();
        assertFalse(partie.progression().drapeau("scooter_gare"));
        assertNull(partie.progression().etat().scooterCarte(), "il n'est plus gare au milieu de l'avenue");
        partie.monde().aller("avenue_bas", 400, 200);
        assertNotNull(partie.monde().habitant("scooter_y"), "il est a sa place, a cote du banc de Youngssef");
    }

    @Test
    void tombeAScooterOnLeRetrouveLaOuOnEstTombe() {
        robot.laisserPasser();
        partie.monde().aller("avenue_bas", 900, 200);
        partie.progression().marquer("+scooter");
        assertTrue(partie.monde().enScooter());
        partie.progression().etat().blesser(200, 0);
        for (int i = 0; i < 5 && !partie.monde().carte().equals("appartement"); i++) {
            robot.image();
        }
        assertEquals("appartement", partie.monde().carte());
        assertTrue(partie.progression().drapeau("scooter_gare"), "le scooter n'a pas disparu");
        assertEquals("avenue_bas", partie.progression().etat().scooterCarte());
        assertEquals(900, partie.progression().etat().scooterX(), 1f);
    }

    @Test
    void uneArmePoseeResteParTerreEtSeRamasse() throws Exception {
        partie.monde().aller("cour", 300, 236);
        Etat etat = partie.progression().etat();
        etat.prendreArme("barre");
        robot.frapper(KeyEvent.VK_G);
        assertNull(etat.armeEnMain());
        assertEquals(1, etat.auSol().size(), "elle est par terre, pas effacee");
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(2);
        robot.jeu.continuer();
        partie = robot.partie();
        etat = partie.progression().etat();
        assertEquals(1, etat.auSol().size(), "toujours la apres une sauvegarde");
        Etat.AuSol posee = etat.auSol().get(0);
        partie.monde().aller(posee.carte(), posee.x(), posee.y());
        assertEquals(Monde.AU_SOL, partie.monde().cible().id());
        robot.frapper(KeyEvent.VK_E);
        assertEquals("barre", etat.armeEnMain().id(), "on la ramasse");
        assertTrue(etat.auSol().isEmpty());
    }

    @Test
    void lHorlogeTourneQuandOnJoueEtUneNuitFaitAvancerLEquipe() {
        Calendrier cal = partie.progression().etat().calendrier();
        int avant = cal.jour() * Calendrier.JOURNEE + cal.minutes();
        robot.images(30 * 60);                                         // une minute de jeu
        int apres = cal.jour() * Calendrier.JOURNEE + cal.minutes();
        assertTrue(apres - avant >= 55 && apres - avant <= 65, "une minute du quartier par seconde : " + (apres - avant));
    }
}
