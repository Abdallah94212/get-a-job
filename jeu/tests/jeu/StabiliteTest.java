package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Cinematique;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Aucun etat ne doit retenir le joueur : chaque scene de la campagne se passe avec Echap et se joue jusqu'au bout avec E,
 * un encart ouvert par une scene se voit et se ferme, une touche relachee hors de la fenetre ne reste pas "tenue", et
 * une partie tapee au hasard rend toujours la main, sans une seule erreur rattrapee.
 */
class StabiliteTest {
    @TempDir
    Path dossier;
    private Robot robot;

    @BeforeEach
    void commencer() throws Exception {
        robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        robot.laisserPasser();
    }

    /** Echap, E, Echap... jusqu'a avoir la main (un choix se valide avec E). Renvoie le nombre d'images que ca a pris. */
    private int reprendreLaMain() {
        for (int i = 0; i < 3000; i++) {
            EcranPartie p = robot.partie();
            if (p == null) {
                robot.jeu.continuer();
                robot.image();
                continue;
            }
            if (p.libre()) {
                return i;
            }
            if (p.combat().actif() && !p.enScene() && !p.encartOuvert()) {
                return i;                                             // un combat : on a la main, il faut se battre
            }
            if (p.enPause() || p.auTelephone() || p.encartOuvert() && i % 2 == 0) {
                robot.images(20);
                robot.frapper(KeyEvent.VK_ESCAPE);
            } else if (p.scenographeAuChoix() || p.auBilan() || p.auCalendrier() || p.encartOuvert()) {
                robot.images(20);
                robot.frapper(KeyEvent.VK_E);
            } else {
                robot.frapper(KeyEvent.VK_ESCAPE);
            }
        }
        return -1;
    }

    @Test
    void chaqueSceneSePasseAvecEchapEtSeJoueAvecE() {
        List<String> bloquees = new ArrayList<>();
        for (Cinematique c : robot.jeu.res.campagne.cinematiques().values()) {
            for (int touche : new int[] {KeyEvent.VK_ESCAPE, KeyEvent.VK_E}) {
                robot.jeu.nouvellePartie();
                robot.images(2);
                robot.partie().jouerScene(c);
                int i = 0;
                while (i++ < 20000 && robot.partie() != null && (robot.partie().enScene() || robot.partie().encartOuvert()
                        || robot.partie().auBilan())) {
                    robot.images(touche == KeyEvent.VK_E ? 6 : 20);
                    robot.frapper(robot.partie().scenographeAuChoix() || robot.partie().encartOuvert() ? KeyEvent.VK_E : touche);
                }
                if (robot.partie() == null || robot.partie().enScene() || robot.partie().encartOuvert()) {
                    bloquees.add(c.id() + (touche == KeyEvent.VK_E ? " (E)" : " (Echap)"));
                }
            }
        }
        assertTrue(bloquees.isEmpty(), "scenes qui ne rendent pas la main : " + bloquees);
        assertEquals(0, robot.jeu.incidents(), "aucune erreur rattrapee");
    }

    @Test
    void unEncartOuvertParUneSceneSeVoitEtSeFerme() {
        EcranPartie p = robot.partie();
        Cinematique scene = new Cinematique("essai");
        scene.plans().add(new histoire.Plan(histoire.Plan.Genre.ENCART, new String[] {"fouille"}, ""));
        scene.plans().add(new histoire.Plan(histoire.Plan.Genre.RECIT, new String[] {}, "APRÈS L'ENCART."));
        p.progression().marquer("-vu_fouille");
        p.jouerScene(scene);
        robot.images(2);
        assertTrue(p.encartOuvert(), "la scene attend l'encart");
        robot.images(30);
        decor.Rendu vu = new decor.Rendu();
        robot.jeu.dessiner(vu);
        assertEquals(0x14141E, vu.image[150 * decor.Rendu.L + 58] & 0xFFFFFF, "la carte de l'encart est dessinee par-dessus la scene");
        robot.frapper(KeyEvent.VK_ESCAPE);
        assertTrue(!p.encartOuvert(), "Echap le referme");
        assertTrue(reprendreLaMain() >= 0);
    }

    @Test
    void uneToucheRelacheeHorsDeLaFenetreNeResteNonTenue() {
        Touches t = robot.touches;
        t.presser(KeyEvent.VK_E);                                     // E enfonce, puis la fenetre perd le clavier
        t.finImage();
        t.toutLacher();                                               // ce que fait la fenetre quand elle perd le focus
        t.presser(KeyEvent.VK_E);
        assertTrue(t.valider(), "une nouvelle frappe de E compte");
        t.relacher(KeyEvent.VK_E);
        t.finImage();
    }

    @Test
    void unePartieTapeeAuHasardRendToujoursLaMain() {
        Random hasard = new Random(7);
        int[] touches = {KeyEvent.VK_E, KeyEvent.VK_ESCAPE, KeyEvent.VK_P, KeyEvent.VK_J, KeyEvent.VK_K, KeyEvent.VK_F,
            KeyEvent.VK_G, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_ENTER};
        int pire = 0;
        for (int essai = 0; essai < 60; essai++) {
            for (int k = 0; k < 40; k++) {
                int code = touches[hasard.nextInt(touches.length)];
                robot.touches.presser(code);
                robot.images(1 + hasard.nextInt(8));
                robot.touches.relacher(code);
                robot.image();
            }
            int pris = reprendreLaMain();
            assertTrue(pris >= 0, "la main revient au joueur (essai " + essai + ", scene "
                    + (robot.partie() == null ? "menu" : robot.partie().positionScene()) + ")");
            pire = Math.max(pire, pris);
        }
        assertEquals(0, robot.jeu.incidents(), "aucune erreur rattrapee");
    }
}
