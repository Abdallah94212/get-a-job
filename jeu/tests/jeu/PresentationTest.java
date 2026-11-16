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
 * Les personnages importants se presentent la premiere fois qu'ils parlent (une fiche, une seule fois), et le telephone
 * se lit comme un telephone : une grille d'applis, l'equipe qui mene aux operations, l'heure du quartier.
 */
class PresentationTest {
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

    private void dire(String qui) {
        Cinematique c = new Cinematique("essai");
        c.plans().add(new Plan(Plan.Genre.DIT, new String[] {qui, ""}, "SALUT."));
        partie.jouerScene(c);
        robot.images(3);
    }

    @Test
    void unPersonnageSePresenteLaPremiereFoisQuIlParleEtPasDeuxFois() {
        partie.progression().marquer("-vu_presente_youngssef");
        dire("youngssef_boutique");                                     // le meme Youngssef, a la boutique
        assertTrue(partie.encartOuvert());
        assertEquals("presente_youngssef", partie.encartCourant(), "sa fiche avant sa premiere replique");
        assertTrue(partie.enScene(), "la scene attend la fiche");
        robot.laisserPasser();
        assertTrue(partie.progression().drapeau("vu_presente_youngssef"));
        dire("youngssef");
        assertFalse(partie.encartOuvert(), "on ne le presente qu'une fois");
        robot.laisserPasser();
        partie.progression().marquer("-vu_presente_geto");
        dire("geto_tel");                                               // Geto au telephone, c'est Geto
        assertEquals("presente_geto", partie.encartCourant());
        robot.laisserPasser();
        for (String id : new String[] {"youngssef", "geto", "diallo", "omar", "gero", "mounir", "rayan", "moussa", "haddad"}) {
            assertTrue(robot.jeu.res.campagne.encarts().containsKey("presente_" + id), "fiche de " + id);
            assertTrue(robot.jeu.res.image(robot.jeu.res.campagne.encarts().get("presente_" + id).image()) != null, "portrait de " + id);
        }
    }

    @Test
    void leTelephoneEstUneGrilleEtLEquipeMeneAuxOperations() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.6"), 1, Set.of(), true,
                Set.of("organisation", "recrue_youngssef", "recrue_rayan", "recrue_moussa", "youngssef_paye", "youngssef_promis"));
        partie.progression().etat().organisation().recruter(partie.progression());
        robot.frapper(KeyEvent.VK_P);
        assertTrue(partie.auTelephone());
        robot.frapper(KeyEvent.VK_DOWN);                                // une rangee plus bas : l'equipe (4e appli)
        robot.frapper(KeyEvent.VK_E);
        decor.Rendu vu = new decor.Rendu();
        robot.jeu.dessiner(vu);                                         // l'ecran de l'equipe se dessine
        robot.frapper(KeyEvent.VK_E);                                   // E sur un petit : ses operations
        int avant = partie.progression().etat().argent();
        partie.progression().etat().gagner(500, "ESSAI");
        robot.frapper(KeyEvent.VK_E);                                   // une operation
        robot.frapper(KeyEvent.VK_E);                                   // confiee au mieux place
        assertTrue(partie.progression().etat().argent() < avant + 500, "l'operation est partie depuis l'equipe");
        robot.frapper(KeyEvent.VK_P);
        assertFalse(partie.auTelephone());
    }
}
