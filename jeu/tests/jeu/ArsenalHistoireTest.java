package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Etat;
import histoire.Objectif;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'arsenal arrive avec l'histoire, jamais sans raison : les poings et les objets de la rue a l'acte 1 ; une batte dans la
 * cour du 29 ; le vieux pistolet de Papy Bocal quand il cede l'epicerie ; a Namek, la lame plantee dans la roche, qui ne
 * sort que pour celui qui porte l'eclat irise ; et la visite des gars de Geto, qui tourne au combat contre une bande armee
 * (un sabreur, un mitrailleur, une brute) : ce qu'ils lachent, et le sac tombe de leur coffre.
 */
class ArsenalHistoireTest {
    @TempDir
    Path dossier;
    private Robot robot;
    private EcranPartie partie;
    private Etat etat;

    @BeforeEach
    void commencer() throws Exception {
        robot = new Robot(dossier.resolve("partie.properties"), null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        robot.laisserPasser();
        partie = robot.partie();
        partie.progression().marquer("+vu_armes_feu +vu_armes_lourdes +vu_katana_relique +vu_ennemis_armes +vu_combat +vu_armes");
        etat = partie.progression().etat();
    }

    @Test
    void lesArmesSuiventLaProgression() {
        for (String id : java.util.List.of("bouteille", "planche", "tuyau", "barre")) {
            assertEquals(1, Etat.arme(id).rang(), id + " : des le debut");
        }
        assertEquals(2, Etat.arme("batte").rang());
        assertEquals(2, Etat.arme("pistolet").rang());
        assertEquals(3, Etat.arme("katana").rang());
        assertEquals(3, Etat.arme("mitraillette").rang());
        assertEquals(4, Etat.arme("fusil").rang());
        assertEquals(4, Etat.arme("pistolet_lourd").rang());
        assertTrue(Etat.arme("katana_relique").relique());
    }

    @Test
    void papyBocalLaisseSonPistoletEnCedantLEpicerie() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.5"), 7, Set.of(), true, Set.of());
        partie.jouerScene(robot.jeu.res.campagne.cinematique("haddad"));
        robot.laisserPasser();
        assertEquals("pistolet", etat.armeEnMain().id(), "le pistolet de son frere, sous le comptoir");
        assertEquals(8, etat.chargees(), "charge");
        assertEquals(16, etat.reserve(), "et une boite de balles");
    }

    @Test
    void laLameDeNamekNeSortQuePourLEclat() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.4"), 0, Set.of(), true, Set.of());
        assertFalse(partie.progression().attendue(Objectif.Genre.AGIR, "lame_namek"), "sans l'eclat, la lame reste dans la roche");
        partie.progression().marquer("+eclat_irise");
        assertTrue(partie.progression().attendue(Objectif.Genre.AGIR, "lame_namek"), "avec l'eclat, elle repond");
        partie.jouerScene(robot.jeu.res.campagne.cinematique("lame_namek"));
        robot.laisserPasser();
        assertEquals("katana_relique", etat.armeEnMain().id());
        assertTrue(etat.armeEnMain().relique());
    }

    @Test
    void laVisiteTourneAuCombatContreUneBandeArmee() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.7"), 0, Set.of(), true, Set.of());
        partie.jouerScene(robot.jeu.res.campagne.cinematique("etincelle"));
        for (int i = 0; i < 3000 && !partie.combat().actif(); i++) {
            robot.frapper(java.awt.event.KeyEvent.VK_E);
        }
        assertTrue(partie.combat().actif(), "la visite tourne au combat");
        Set<Combat.Style> styles = partie.combat().ennemis().stream().map(e -> e.genre.style()).collect(Collectors.toSet());
        assertTrue(styles.containsAll(Set.of(Combat.Style.CORPS, Combat.Style.SABRE, Combat.Style.DISTANCE, Combat.Style.BRUTE)),
                "un bagarreur, un sabreur, un tireur, une brute : " + styles);
        for (Combat.Ennemi e : partie.combat().ennemis()) {            // on les met tous a terre
            e.pv = 0;
            e.etat = Combat.Phase.TOMBE;
            e.minuteur = 0;
        }
        robot.images(3);
        robot.laisserPasser();
        assertTrue(partie.progression().drapeau("visite_finie"));
        Set<String> par = etat.auSol().stream().filter(a -> a.carte().equals(partie.monde().carte())).map(Etat.AuSol::arme)
                .collect(Collectors.toSet());
        assertTrue(par.contains("katana") && par.contains("mitraillette"), "leurs armes sont restees par terre : " + par);
        assertTrue(partie.progression().attendue(Objectif.Genre.AGIR, "sac_geto"), "le sac tombe du coffre");
        partie.jouerScene(robot.jeu.res.campagne.cinematique("sac_geto"));
        robot.laisserPasser();
        assertTrue(etat.inventaire().possede("fusil") && etat.inventaire().possede("pistolet_lourd"), "Geto preparait une guerre");
        assertEquals(30, etat.inventaire().quantite("balles_fusil"));
    }
}
