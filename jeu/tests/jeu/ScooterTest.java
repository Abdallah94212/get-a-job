package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import decor.Joueur;
import decor.Porte;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le scooter, joue pour de vrai : monter, conduire, s'arreter, descendre (E), s'eloigner, revenir, remonter ; le garer
 * devant un commerce et y entrer ; la location qui finit sans que le scooter disparaisse ; la partie rechargee.
 */
class ScooterTest {
    @TempDir
    Path dossier;
    private Robot robot;
    private EcranPartie partie;

    @BeforeEach
    void enSelle() throws Exception {
        robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        robot.laisserPasser();
        partie = robot.partie();
        partie.monde().aller("avenue_bas", 980, 218);
        robot.images(3);
        robot.laisserPasser();                                         // la mission suivante s'ouvre sur une scene
        partie.monde().aller("avenue_bas", 980, 218);
        partie.progression().etat().louer(180);
        partie.progression().marquer("+scooter -scooter_gare");
        partie.monde().peupler();
        robot.images(2);
        assertTrue(partie.monde().enScooter());
    }

    private Monde monde() {
        return partie.monde();
    }

    private void rouler(int touche, int images) {
        robot.touches.presser(touche);
        robot.images(images);
        robot.touches.relacher(touche);
    }

    /** Lache tout et attend que le scooter s'arrete. */
    private void sArreter() {
        for (int i = 0; i < 120 && monde().joueur.vitesse() > 1; i++) {
            robot.image();
        }
    }

    @Test
    void leCycleComplet() {
        rouler(KeyEvent.VK_RIGHT, 50);
        Monde.Cible enRoute = monde().cible();
        assertTrue(enRoute == null || !enRoute.id().equals(Monde.DESCENDRE) || monde().joueur.vitesse() < Monde.ARRET_SCOOTER,
                "on ne saute pas d'un scooter lance");
        sArreter();
        Monde.Cible cible = monde().cible();
        assertNotNull(cible, "a l'arret, E propose de descendre");
        assertEquals(Monde.DESCENDRE, cible.id());
        assertEquals("E : DESCENDRE DU SCOOTER", cible.invite());
        float garéX = monde().joueur.x;
        float garéY = monde().joueur.y;
        robot.frapper(KeyEvent.VK_E);
        assertFalse(monde().enScooter(), "il est descendu");
        assertTrue(monde().scooterGare());
        assertEquals(garéX, partie.progression().etat().scooterX(), 0.01f, "le scooter reste ou on l'a arrete");
        assertEquals(garéY, partie.progression().etat().scooterY(), 0.01f);
        assertFalse(monde().scene().bloque(monde().joueur.x, monde().joueur.y, Joueur.RAYON), "le pied pose hors des murs");
        // on s'eloigne, longtemps : le scooter ne bouge pas, ne disparait pas
        rouler(KeyEvent.VK_LEFT, 120);
        robot.images(200);
        assertTrue(monde().scooterGare());
        assertEquals(garéX, partie.progression().etat().scooterX(), 0.01f);
        assertTrue(monde().joueur.x < garéX - 60, "on est bien parti a pied");
        // on revient : E pour remonter
        for (int i = 0; i < 400 && (monde().cible() == null || !monde().cible().id().equals("scooter_gare")); i++) {
            robot.touches.presser(monde().joueur.x < garéX ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT);
            robot.image();
            robot.touches.relacher(KeyEvent.VK_RIGHT);
            robot.touches.relacher(KeyEvent.VK_LEFT);
        }
        assertEquals("scooter_gare", monde().cible().id(), "devant le scooter, E propose de remonter");
        robot.frapper(KeyEvent.VK_E);
        assertTrue(monde().enScooter(), "il est remonte");
        assertEquals(garéX, monde().joueur.x, 0.01f, "sur le scooter, a sa place");
        rouler(KeyEvent.VK_RIGHT, 40);
        assertTrue(monde().joueur.x > garéX + 20, "et il repart : " + garéX + " -> " + monde().joueur.x + " y " + monde().joueur.y
                + " mode " + partie.positionScene() + " scooter " + monde().enScooter());
    }

    @Test
    void onLeGareDevantUnCommerceEtOnEntre() {
        Porte poulet = monde().scene().portes.stream().filter(p -> p.cible().equals("maitre_poulet")).findFirst().orElseThrow();
        float x = (poulet.x1() + poulet.x2()) / 2;
        monde().poser(x, poulet.yMax() - 2);
        robot.images(2);
        assertTrue(monde().enScooter());
        assertEquals("maitre_poulet", monde().cible().id(), "devant la porte, E fait entrer");
        robot.frapper(KeyEvent.VK_E);
        robot.laisserPasser();
        assertEquals("maitre_poulet", monde().carte());
        assertFalse(monde().enScooter());
        assertEquals("avenue_bas", partie.progression().etat().scooterCarte(), "le scooter est reste garé devant");
        assertEquals(x, partie.progression().etat().scooterX(), 0.01f);
        // on ressort : il est la, devant la porte
        Porte sortie = monde().scene().portes.stream().filter(p -> p.cible().equals("avenue_bas")).findFirst().orElseThrow();
        monde().poser((sortie.x1() + sortie.x2()) / 2, sortie.yMax() - 2);
        robot.images(2);
        robot.frapper(KeyEvent.VK_E);
        robot.laisserPasser();
        assertEquals("avenue_bas", monde().carte());
        assertFalse(monde().enScooter(), "on ressort a pied");
        assertTrue(monde().scooterGare());
        assertTrue(Math.abs(monde().joueur.x - x) < Monde.PORTEE_X, "le scooter est juste la");
        assertEquals("scooter_gare", monde().cible().id());
    }

    @Test
    void laLocationFinitSansQueLeScooterDisparaisse() {
        partie.progression().etat().louer(1f);
        rouler(KeyEvent.VK_RIGHT, 50);
        robot.images(30);
        assertFalse(monde().enScooter(), "la location est finie : on s'arrete");
        assertTrue(monde().scooterGare(), "le scooter reste la ou il est");
        float x = partie.progression().etat().scooterX();
        int argent = partie.progression().etat().argent();
        partie.progression().etat().gagner(-argent, "TEST");
        monde().poser(x, partie.progression().etat().scooterY());
        robot.images(2);
        robot.frapper(KeyEvent.VK_E);
        assertFalse(monde().enScooter(), "sans 5 euros, on ne repart pas");
        partie.progression().etat().gagner(20, "TEST");
        robot.frapper(KeyEvent.VK_E);
        assertTrue(monde().enScooter(), "avec 5 euros, on le reloue sur place");
        assertEquals(15, partie.progression().etat().argent());
    }

    @Test
    void leScooterGareSurvitAuChargement() throws Exception {
        rouler(KeyEvent.VK_RIGHT, 30);
        sArreter();
        robot.frapper(KeyEvent.VK_E);
        float x = partie.progression().etat().scooterX();
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(5);
        robot.jeu.continuer();
        robot.images(5);
        EcranPartie reprise = robot.partie();
        assertTrue(reprise.monde().scooterGare());
        assertEquals(x, reprise.progression().etat().scooterX(), 0.01f);
        assertFalse(reprise.monde().enScooter());
    }
}
