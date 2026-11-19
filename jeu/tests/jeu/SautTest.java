package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Cinematique;
import histoire.Plan;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le heros saute (Espace), a pied : il monte, retombe, l'ombre reste au sol ; un coup passe sous lui ; Espace ne fait
 * plus parler les gens en jeu (E le fait), mais fait toujours avancer un dialogue.
 */
class SautTest {
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
        partie.progression().marquer("+vu_combat +vu_astuce_touches");
        partie.monde().aller("cour", 300, 236);
    }

    @Test
    void ilMonteEtRetombe() {
        robot.frapper(KeyEvent.VK_SPACE);
        float plusHaut = 0;
        int images = 0;
        while (partie.monde().hauteurDuSaut() > 0 && images < 60) {
            plusHaut = Math.max(plusHaut, partie.monde().hauteurDuSaut());
            robot.image();
            images++;
        }
        assertTrue(plusHaut > 18, "il monte : " + plusHaut);
        assertTrue(images > 10 && images < 40, "et retombe vite : " + images + " images");
        assertEquals(0, partie.monde().hauteurDuSaut(), 0.01f);
    }

    @Test
    void espaceNeFaitPasParlerMaisEFaitParler() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.1"), 0, Set.of(), true, Set.of("vu_presente_youngssef"));
        partie.monde().aller("avenue_bas", 400, 220);
        Monde.Habitant y = partie.monde().habitant("youngssef");
        partie.monde().joueur.x = y.acteur.x - 20;
        partie.monde().joueur.y = y.acteur.y + 4;
        robot.images(2);
        robot.frapper(KeyEvent.VK_SPACE);
        assertFalse(partie.enScene(), "Espace saute, il ne parle pas");
        robot.images(40);
        robot.frapper(KeyEvent.VK_E);
        assertTrue(partie.enScene(), "E parle");
    }

    @Test
    void espaceFaitToujoursAvancerUnDialogue() {
        Cinematique c = new Cinematique("essai");
        c.plans().add(new Plan(Plan.Genre.RECIT, new String[] {}, "UN."));
        c.plans().add(new Plan(Plan.Genre.RECIT, new String[] {}, "DEUX."));
        partie.jouerScene(c);
        robot.images(60);                                              // la premiere replique est ecrite en entier
        String avant = partie.positionScene();
        robot.frapper(KeyEvent.VK_SPACE);
        assertTrue(avant.contains("#0") && partie.positionScene().contains("#1"), "Espace passe a la suivante : " + avant + " -> "
                + partie.positionScene());
    }

    @Test
    void unCoupPasseSousLuiQuandIlSaute() {
        partie.combat().commencer(Combat.GUETTEUR, 1, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi e = partie.combat().ennemis().get(0);
        e.acteur.x = 318;
        e.acteur.y = 236;
        e.etat = Combat.Phase.PREPARE;
        e.minuteur = 0.12f;                                            // il frappe dans un instant
        e.coupsRestants = 0;
        float sante = partie.progression().etat().sante();
        robot.frapper(KeyEvent.VK_SPACE);
        robot.images(8);
        assertEquals(sante, partie.progression().etat().sante(), 0.01f, "le coup est passe sous ses pieds");
    }

    @Test
    void pasDeSautAScooterNiLesBrasCharges() {
        partie.monde().aller("avenue_bas", 600, 220);
        partie.progression().marquer("+scooter");
        robot.frapper(KeyEvent.VK_SPACE);
        assertEquals(0, partie.monde().hauteurDuSaut(), 0.01f, "a scooter, pas de saut");
        partie.progression().marquer("-scooter +charge");
        robot.frapper(KeyEvent.VK_SPACE);
        assertEquals(0, partie.monde().hauteurDuSaut(), 0.01f, "vingt kilos dans les bras, non plus");
    }
}
