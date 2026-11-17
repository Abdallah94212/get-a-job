package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La route de Namek : on y entre depuis la scene de depart, on la conduit au clavier jusqu'au bout, on en sort sur
 * Namek pour la recolte. Les chocs abiment et ralentissent, le vide de la piste et la panne font repartir au debut du
 * troncon, Echap met en pause sans rien bloquer, et une partie quittee au volant reprend la route.
 */
class ConduiteTest {
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
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.4"), 0, Set.of(), false, Set.of("boitier_orange"));
        partie.progression().etat().calendrier().regler(29, 6 * 60);
        robot.image();                                                 // la mission commence : sa scene, puis le volant
        for (int i = 0; i < 400 && !partie.auVolant(); i++) {
            robot.images(5);
            robot.frapper(KeyEvent.VK_E);
        }
        assertTrue(partie.auVolant(), "la scene de depart rend la main au volant");
    }

    private Conduite route() {
        return partie.conduite();
    }

    private void demarrer() {
        robot.images(30);
        robot.frapper(KeyEvent.VK_E);                                  // les consignes lues
        robot.images(100);                                             // le decompte
        assertEquals(Conduite.Etape.ROUTE, route().etape());
    }

    @Test
    void onConduitJusquANamekEtOnRecolte() {
        StringBuilder trace = new StringBuilder();
        for (int i = 0; i < 400 && partie.auVolant(); i++) {
            robot.conduire();
            if (i % 10 == 0 && partie.auVolant()) {
                trace.append(String.format(" [%d s=%.0f v=%.0f t=%d e=%.0f d=%.0f %s]", i, route().s, route().vitesse, route().troncon(),
                        route().essence, route().degats, route().etape()));
            }
        }
        assertFalse(partie.auVolant(), "la route se termine :" + trace);
        robot.laisserPasser();
        assertEquals("namek", partie.monde().carte(), "on arrive a Namek, pour la recolte");
        assertTrue(partie.progression().drapeau("route_namek_faite"));
        assertTrue(partie.progression().drapeaux().stream().noneMatch(d -> d.startsWith("conduite:")));
        assertEquals("plant0", partie.progression().objectif().cible(), "la recolte commence");
    }

    @Test
    void unChocAbimeEtRalentit() {
        demarrer();
        Conduite c = route();
        Conduite.Obstacle camion = c.obstacles().stream().filter(o -> o.genre == Conduite.Genre.CAMION).findFirst().orElseThrow();
        c.s = camion.s - 40;
        c.x = c.centre(camion.s) + camion.dx;
        c.vitesse = 300;
        robot.touches.presser(KeyEvent.VK_UP);
        robot.images(15);
        robot.touches.relacher(KeyEvent.VK_UP);
        assertTrue(c.degats > 10, "la carrosserie en a pris un coup : " + c.degats);
    }

    @Test
    void sortirDeLaPisteFaitRepartirAuDebutDuTroncon() {
        demarrer();
        Conduite c = route();
        float debutPiste = Conduite.debutDe(2);
        c.s = debutPiste + 600;
        c.x = c.centre(c.s) + c.largeur(c.s) / 2 + 30;                 // au-dela du bord : le vide
        c.vitesse = 200;
        robot.images(2);
        assertEquals(Conduite.Etape.PERTE, c.etape());
        robot.images(60);
        assertEquals(Conduite.Etape.ROUTE, c.etape(), "on repart");
        assertTrue(c.s < debutPiste + 100, "au debut de la piste");
        assertTrue(Math.abs(c.x - c.centre(c.s)) < 5, "au milieu de la route");
    }

    @Test
    void laPanneSecheRameneAuDebutDuTronconAvecUnBidon() {
        demarrer();
        Conduite c = route();
        c.essence = 0;
        c.vitesse = 0;
        robot.images(2);
        assertEquals(Conduite.Etape.PERTE, c.etape());
        robot.images(60);
        assertTrue(c.essence >= 50, "un bidon dans le reservoir : " + c.essence);
    }

    @Test
    void echapMetEnPauseEtRendLaMain() {
        demarrer();
        robot.frapper(KeyEvent.VK_ESCAPE);
        assertEquals(Conduite.Etape.PAUSE, route().etape());
        float s = route().s;
        robot.images(30);
        assertEquals(s, route().s, 0.01f, "en pause, rien ne bouge");
        robot.frapper(KeyEvent.VK_ESCAPE);
        assertEquals(Conduite.Etape.ROUTE, route().etape());
    }

    @Test
    void unePartieQuitteeAuVolantReprendLaRoute() throws Exception {
        demarrer();
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(2);
        robot.jeu.continuer();
        assertNotNull(robot.partie().conduite(), "la route reprend");
        assertTrue(robot.partie().auVolant());
    }

    /** Un obstacle de ce genre, et rien d'autre autour : les autres sont ecartes pour mesurer celui-la seul. */
    private Conduite.Obstacle seul(Conduite.Genre genre) {
        Conduite c = route();
        Conduite.Obstacle o = c.obstacles().stream().filter(x -> x.genre == genre && x.s > 2000).findFirst().orElseThrow();
        for (Conduite.Obstacle autre : c.obstacles()) {
            if (autre != o && Math.abs(autre.s - o.s) < 400) {
                autre.pris = true;
            }
        }
        return o;
    }

    @Test
    void frolerUnRocherSansLeToucherNeCouteRien() {
        demarrer();
        Conduite c = route();
        Conduite.Obstacle r = seul(Conduite.Genre.ROCHER);
        r.vs = 0;
        float[] rs6 = Conduite.visible(robot.jeu.res.image("cond_rs6"));
        c.s = r.s - 60;
        c.x = c.centre(r.s) + r.dx + (r.l + rs6[0]) / 2f + 1;           // a un pixel de la partie visible
        c.vitesse = 150;
        float avant = c.degats;
        robot.images(25);
        assertEquals(avant, c.degats, 0.5f, "on passe a cote sans le toucher");
    }

    @Test
    void percuterUneVoitureParLArriereNeLaTraversePas() {
        demarrer();
        Conduite c = route();
        Conduite.Obstacle v = seul(Conduite.Genre.CAMION);
        c.s = v.s - 80;
        c.x = c.centre(v.s) + v.dx;
        c.vitesse = 380;
        robot.touches.presser(KeyEvent.VK_UP);
        float pire = Float.MAX_VALUE;
        for (int i = 0; i < 60; i++) {
            robot.image();
            if (Math.abs(c.x - (c.centre(v.s) + v.dx)) < 4) {
                pire = Math.min(pire, v.s - c.s);
            }
        }
        robot.touches.relacher(KeyEvent.VK_UP);
        assertTrue(c.degats > 0, "le choc compte");
        assertTrue(pire > 10, "on reste derriere elle, on ne passe pas a travers : " + pire);
    }

    @Test
    void eraflerParLeCoteRepousse() {
        demarrer();
        Conduite c = route();
        Conduite.Obstacle r = seul(Conduite.Genre.ROCHER);
        c.s = r.s;
        c.x = c.centre(r.s) + r.dx + 4;                                  // collee contre lui
        c.vitesse = 100;
        robot.images(2);
        float ecart = Math.abs(c.x - (c.centre(r.s) + r.dx));
        assertTrue(ecart >= r.l / 2f, "elle est repoussee hors de lui : " + ecart);
    }

    @Test
    void laVoitureEstDessineeLaOuElleTouche() throws Exception {
        demarrer();
        Conduite c = route();
        Conduite.Obstacle v = seul(Conduite.Genre.CAMION);
        c.s = v.s - 120;
        c.x = c.centre(v.s) + v.dx;
        c.vitesse = 380;
        robot.touches.presser(KeyEvent.VK_UP);
        float avant = c.degats;
        for (int i = 0; i < 60 && c.degats == avant; i++) {
            robot.image();
        }
        robot.touches.relacher(KeyEvent.VK_UP);
        assertTrue(c.degats > avant, "le choc a lieu");
        // a l'instant du choc, a l'ecran, l'avant de la RS6 touche l'arriere de l'autre voiture (a 3 pixels pres)
        decor.Texture rs6 = robot.jeu.res.image("cond_rs6");
        decor.Texture autre = robot.jeu.res.image(v.image);
        float avantRs6 = c.ecranYDe(c.s) - Conduite.visible(rs6)[1] / 2f;
        float arriereAutre = c.ecranYDe(v.s) + Conduite.visible(autre)[1] / 2f;
        assertTrue(Math.abs(avantRs6 - arriereAutre) <= 3, "ce qu'on voit toucher est ce qui touche : " + avantRs6 + " / " + arriereAutre);
    }
}
