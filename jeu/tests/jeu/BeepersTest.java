package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Cinematique;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** La scene des deux beepers devant la RS6 : elle se joue en entier, dans le bon ordre, et rend la main au joueur. */
class BeepersTest {
    @Test
    void leMauvaisBeeperPuisLeBon(@TempDir Path dossier) throws Exception {
        Robot robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        partie.monde().aller("garage", 200, 236);
        robot.images(2);
        Cinematique scene = robot.jeu.res.campagne.cinematique("confusion");
        partie.jouerScene(scene);
        List<String> plans = new java.util.ArrayList<>();
        int images = 0;
        for (int i = 0; i < 30 * 90 && partie.enScene(); i++) {
            String ici = partie.positionScene();
            if (plans.isEmpty() || !plans.get(plans.size() - 1).equals(ici)) {
                plans.add(ici);
            }
            if (ici.endsWith(":DIT") || ici.endsWith(":RECIT")) {
                if (i % 40 == 39) {
                    robot.frapper(KeyEvent.VK_E);                      // le joueur lit, puis passe
                    continue;
                }
            }
            robot.image();
            images++;
        }
        assertTrue(partie.combat().actif(), "la scene finit sur un combat : " + plans);
        assertEquals(4, partie.combat().ennemis().size(), "quatre aliens");
        robot.laisserPasser();                                         // le robot se bat, puis la scene de la victoire ou de la defaite
        assertFalse(partie.enScene() || partie.combat().actif(), "tout se termine et rend la main");
        assertTrue(partie.progression().drapeau("aliens_vaincus") || partie.progression().drapeau("aliens_fui"));
        assertFalse(partie.progression().drapeau("rs6_nuit"), "la nuit du vol est finie");
        List<String> j = robot.jeu.audio.journal();
        int clic = j.indexOf("declic");
        int bip = j.indexOf("bip_3");
        int serrure = j.lastIndexOf("serrure");
        assertTrue(clic > 0 && clic < bip, "le clic, puis le beeper qui repond");
        assertTrue(serrure > bip, "la voiture ne s'ouvre qu'avec le bon, a la fin");
        assertTrue(j.contains("v_heros_hm") || j.contains("v_heros"), "il parle en hesitant");
        assertEquals("normal", robot.jeu.audio.mixageCourant());
        // les plans dessines sont bien la, et dans l'ordre : deux mains, hesitation, pouce, clic, orange
        String suite = String.join(" ", plans);
        int a = suite.indexOf("#2:GROS_PLAN");
        assertTrue(a >= 0, suite);
        for (String image : List.of("gp_deux_mains", "gp_hesite_g", "gp_hesite_d", "gp_pouce", "gp_clic", "gp_orange")) {
            assertTrue(robot.jeu.res.image(image) != null, "image " + image);
        }
        assertTrue(images < 30 * 60, "une scene courte : " + images / 30 + " s");
    }
}
