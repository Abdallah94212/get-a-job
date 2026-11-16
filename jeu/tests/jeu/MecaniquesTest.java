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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Les mecaniques jouees pour de vrai, dans le jeu sans fenetre : le souffle, le scooter, l'argent, la sante. */
class MecaniquesTest {
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
    }

    /** Place l'histoire au debut d'une mission, et le heros a cote d'un personnage. */
    private void aller(String mission, String carte, String personnage, String... drapeaux) {
        int rang = robot.jeu.res.campagne.missions().indexOf(robot.jeu.res.campagne.mission(mission));
        progression.restaurer(rang, 0, Set.of(), true, Set.of(drapeaux));
        float[] ou = robot.jeu.res.position(robot.jeu.res.campagne.figurants().get(personnage).lieu());
        partie.monde().aller(carte, ou[0] - 14, Math.max(robot.jeu.res.carte(carte).yMin, ou[1]));
        robot.image();
    }

    private void parler() {
        robot.frapper(KeyEvent.VK_E);
        assertTrue(partie.enScene(), "il a quelque chose a proposer");
        robot.laisserPasser();
    }

    @Test
    void courirEssouffleEtLeSouffleRevient() {
        aller("1.2", "avenue_bas", "youngssef");
        robot.touches.presser(KeyEvent.VK_RIGHT);
        robot.touches.presser(KeyEvent.VK_SHIFT);
        robot.images(30);
        assertTrue(partie.monde().court());
        assertTrue(etat.endurance() < 90f, "courir coute du souffle");
        robot.images(200);
        assertTrue(etat.essouffle());
        assertFalse(partie.monde().court(), "a bout de souffle, il marche meme si la touche est tenue");
        robot.touches.relacher(KeyEvent.VK_RIGHT);
        robot.touches.relacher(KeyEvent.VK_SHIFT);
        robot.images(90);
        assertFalse(etat.essouffle());
    }

    @Test
    void onLoueLeScooterOnEnDescendEtOnYRemonte() {
        aller("1.2", "avenue_bas", "youngssef");
        etat.gagner(20);
        parler();
        assertEquals(15, etat.argent(), "la location coute cinq euros");
        assertTrue(partie.monde().enScooter());
        assertTrue(etat.location() > 170f);

        robot.frapper(KeyEvent.VK_F);
        assertFalse(partie.monde().enScooter(), "F : on descend");
        assertTrue(partie.monde().scooterGare());
        float gareX = etat.scooterX();

        robot.touches.presser(KeyEvent.VK_LEFT);
        robot.images(40);
        robot.touches.relacher(KeyEvent.VK_LEFT);
        robot.images(5);
        assertTrue(partie.monde().joueur.x < gareX - 30, "a pied, on s'eloigne du scooter");
        assertEquals(gareX, etat.scooterX(), "il reste ou on l'a laisse");

        partie.monde().joueur.x = gareX;
        robot.frapper(KeyEvent.VK_E);
        assertTrue(partie.monde().enScooter(), "E pres du scooter : on remonte");
        assertFalse(partie.monde().scooterGare());
    }

    @Test
    void sansArgentPasDeScooter() {
        aller("1.2", "avenue_bas", "youngssef");
        parler();
        assertFalse(partie.monde().enScooter());
        assertEquals(0, etat.argent());
    }

    @Test
    void laLocationFinitEtLeScooterRepart() {
        aller("1.2", "avenue_bas", "youngssef");
        etat.gagner(5);
        parler();
        etat.louer(0.2f);
        robot.images(12);
        assertFalse(partie.monde().enScooter());
        assertFalse(progression.drapeau("scooter"));
        assertEquals(0f, etat.location());
    }

    @Test
    void leScooterPretePourLaMissionNEstPasFacture() {
        aller("1.1", "avenue_bas", "youngssef", "pret_scooter");
        progression.restaurer(progression.rang(), 2, Set.of(), true, Set.of("pret_scooter"));
        etat.gagner(20);
        robot.image();
        parler();
        assertEquals(20, etat.argent(), "ce soir-la, il le prete");
    }

    @Test
    void unMenuChezGetoSoigneEtCouteHuitEuros() {
        aller("1.2", "maitre_poulet", "geto");
        progression.restaurer(progression.rang(), 1, Set.of(), true, Set.of());
        robot.image();
        etat.blesser(60f, 0f);
        etat.gagner(10);
        parler();
        assertEquals(2, etat.argent());
        assertEquals(85f, etat.sante(), 0.01f);
        assertEquals(etat.enduranceMax(), etat.endurance(), 0.01f, "manger rend le souffle");
    }

    @Test
    void unePartieSauvegardeeRetrouveSanteArgentEtScooter() throws IOException {
        aller("1.2", "avenue_bas", "youngssef");
        etat.gagner(20);
        parler();
        robot.frapper(KeyEvent.VK_F);
        etat.blesser(40f, 0f);
        partie.sauvegarder();
        Sauvegarde.Partie relue = new Sauvegarde(dossier.resolve("partie.properties")).charger(robot.jeu.res.campagne);
        Etat lu = relue.progression().etat();
        assertEquals(15, lu.argent());
        assertEquals(60f, lu.sante(), 0.01f);
        assertEquals("avenue_bas", lu.scooterCarte());
        assertEquals(etat.scooterX(), lu.scooterX(), 0.01f);
        assertTrue(lu.location() > 100f);
        assertTrue(relue.progression().drapeau("scooter_gare"));
    }

    @Test
    void aZeroDeSanteOnSeReveilleChezSoiAvecMoinsDArgent() {
        aller("1.2", "avenue_bas", "youngssef");
        etat.gagner(100);
        etat.blesser(500f, 0f);
        robot.images(2);
        assertEquals("appartement", partie.monde().carte());
        assertEquals(70, etat.argent());
        assertEquals(50f, etat.sante());
    }

    @Test
    void dormirChezSoiRemetSurPied() {
        aller("1.2", "avenue_bas", "youngssef");
        etat.blesser(50f, 0f);
        float[] lit = robot.jeu.res.carte("appartement").reperes.get("lit");
        assertNotNull(lit);
        partie.monde().aller("appartement", lit[0], lit[1]);
        robot.image();
        assertNotNull(partie.monde().cible());
        assertEquals("dormir", partie.monde().cible().id());
        robot.frapper(KeyEvent.VK_E);
        assertEquals(100f, etat.sante());
        assertNull(partie.monde().cible() == null ? null : "dormir".equals(partie.monde().cible().id()) ? "encore" : null,
                "en pleine forme, le lit ne propose plus rien");
    }
}
