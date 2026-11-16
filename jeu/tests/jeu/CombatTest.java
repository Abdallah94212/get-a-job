package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import decor.Porte;
import histoire.Etat;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le combat, les poubelles, les armes, les encarts et la petite porte du garage, joues dans le vrai jeu.
 */
class CombatTest {
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

    /** Un combat contre un ennemi pose juste devant le heros, dans la cour. */
    private Combat.Ennemi seul(Combat.Genre genre) {
        partie.monde().aller("cour", 300, 236);
        partie.monde().joueur.versDroite = true;
        partie.combat().commencer(genre, 1, 300, 236, "guetteur_battu", "guetteur_gagne", partie.monde().scene());
        Combat.Ennemi e = partie.combat().ennemis().get(0);
        e.acteur.x = 318;
        e.acteur.y = 236;
        e.repos = 10;                                                  // il attend : on mesure nos coups
        return e;
    }

    /** L'ennemi reste ou on le pose (il encaisse) : pour mesurer la portee d'un coup sans qu'il recule de lui-meme. */
    private static void figer(Combat.Ennemi e, float x) {
        e.acteur.x = x;
        e.vx = 0;
        e.etat = Combat.Phase.RECULE;
        e.minuteur = 5;
    }

    @Test
    void lePoingEstRapideLePiedPorteLoinEtFrappeFort() {
        Combat.Ennemi e = seul(Combat.GUETTEUR);
        figer(e, 318);
        robot.frapper(KeyEvent.VK_J);
        robot.images(10);
        float apresPoing = e.pv;
        assertEquals(Combat.GUETTEUR.pv() - Combat.POING.degats(), apresPoing, 0.01f, "le poing touche");
        figer(e, 330);                                                 // hors de portee du poing, a portee du pied
        robot.frapper(KeyEvent.VK_J);
        robot.images(12);
        assertEquals(apresPoing, e.pv, 0.01f, "trop loin pour un poing");
        figer(e, 330);
        float avant = e.acteur.x;
        robot.frapper(KeyEvent.VK_K);
        robot.images(18);
        assertEquals(apresPoing - Combat.PIED.degats(), e.pv, 0.01f, "le pied porte plus loin et fait plus mal");
        assertTrue(e.acteur.x > avant + 8, "le pied repousse");
        assertTrue(Combat.PIED.duree() > Combat.POING.duree() * 1.5f, "le pied est plus lent");
    }

    @Test
    void unEnnemiPrepareSonCoupPuisTouche() {
        Combat.Ennemi e = seul(Combat.ALIEN);
        e.repos = 0;
        float sante = partie.progression().etat().sante();
        boolean prepare = false;
        for (int i = 0; i < 90 && partie.progression().etat().sante() == sante; i++) {
            robot.image();
            prepare |= e.etat == Combat.Phase.PREPARE;
        }
        assertTrue(prepare, "on le voit preparer son coup avant qu'il frappe");
        assertTrue(partie.progression().etat().sante() < sante, "puis il touche");
    }

    @Test
    void frapperLePremierInterromptSonCoup() {
        Combat.Ennemi e = seul(Combat.GUETTEUR);
        e.etat = Combat.Phase.PREPARE;
        e.minuteur = 0.6f;
        robot.frapper(KeyEvent.VK_J);
        robot.images(2);                                               // la fenetre active du poing
        assertEquals(Combat.Phase.RECULE, e.etat, "touche, il perd le coup qu'il preparait");
    }

    @Test
    void gagnerJoueLaSceneDeLaVictoire() {
        Combat.Ennemi e = seul(Combat.GUETTEUR);
        e.pv = 1;
        robot.frapper(KeyEvent.VK_J);
        robot.images(40);
        assertFalse(partie.combat().actif());
        assertTrue(partie.enScene(), "la scene de la victoire se joue");
        robot.laisserPasser();
        assertTrue(partie.progression().drapeau("guetteur_battu"));
    }

    @Test
    void perdreNeTuePasEtJoueLaSceneDeLaDefaite() {
        Combat.Ennemi e = seul(Combat.GUETTEUR);
        e.repos = 0;
        partie.progression().etat().blesser(99.5f, 0f);
        for (int i = 0; i < 200 && partie.combat().actif(); i++) {
            robot.image();
        }
        assertFalse(partie.combat().actif());
        robot.laisserPasser();
        assertTrue(partie.progression().drapeau("guetteur_vu"), "la defaite a sa scene");
        assertEquals("cour", partie.monde().carte(), "on ne se reveille pas chez soi : l'histoire continue");
        assertTrue(partie.progression().etat().sante() > 20f);
    }

    @Test
    void lesPoubellesSeFouillentUneFoisEtDonnentParfoisUneArme() {
        partie.monde().aller("cour", 250, 224);
        assertEquals("fouille:fouille_arme", partie.monde().cible().id());
        robot.frapper(KeyEvent.VK_E);
        robot.laisserPasser();                                         // les encarts : la fouille, puis les armes
        Etat etat = partie.progression().etat();
        assertNotNull(etat.armeEnMain(), "celle-la cache toujours de quoi se defendre");
        assertTrue(etat.fouillee("fouille_arme"));
        Monde.Cible apres = partie.monde().cible();
        assertTrue(apres == null || !apres.id().equals("fouille:fouille_arme"), "on ne la fouille qu'une fois");
        int armes = 0;
        int rien = 0;
        for (String id : new String[] {"fouille0", "fouille1"}) {
            for (String carte : new String[] {"avenue_bas", "avenue_milieu", "avenue_haut"}) {
                float[] p = robot.jeu.res.carte(carte).reperes.get(id);
                partie.monde().aller(carte, p[0], p[1]);
                etat.lacherArme();
                int argent = etat.argent();
                robot.frapper(KeyEvent.VK_E);
                robot.laisserPasser();
                if (etat.armeEnMain() != null) {
                    armes++;
                } else if (etat.argent() == argent) {
                    rien++;
                }
            }
        }
        assertTrue(rien >= 1, "toutes les poubelles ne donnent pas quelque chose : " + rien);
        etat.finDeMission();
        assertFalse(etat.fouillee("fouille_arme"), "a la mission suivante, elles sont pleines a nouveau");
    }

    @Test
    void uneArmeFrappePlusFortEtFinitParCasser() {
        partie.progression().etat().prendreArme("bouteille");
        Combat.Ennemi e = seul(Combat.GUETTEUR);
        robot.frapper(KeyEvent.VK_J);
        robot.images(14);
        assertEquals(Combat.GUETTEUR.pv() - Etat.arme("bouteille").degats(), e.pv, 0.01f);
        assertNull(partie.progression().etat().armeEnMain(), "la bouteille a eclate");
        partie.progression().etat().prendreArme("barre");
        assertEquals(-1, partie.progression().etat().coupsRestants(), "la barre ne casse pas");
    }

    @Test
    void lArmeEtLesFouillesSontSauvegardees() throws Exception {
        partie.progression().etat().prendreArme("tuyau");
        partie.progression().etat().fouiller("fouille0");
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(3);
        robot.jeu.continuer();
        Etat etat = robot.partie().progression().etat();
        assertEquals("tuyau", etat.armeEnMain().id());
        assertTrue(etat.fouillee("fouille0"));
    }

    @Test
    void unEncartNeSAfficheQuUneFois() {
        partie.monde().aller("cour", 208, 222);
        robot.frapper(KeyEvent.VK_E);
        assertTrue(partie.encartOuvert(), "la premiere fouille s'explique");
        robot.images(30);
        robot.frapper(KeyEvent.VK_E);
        if (partie.encartOuvert()) {                                   // une arme trouvee : son encart suit celui de la fouille
            assertEquals("armes", partie.encartCourant());
            robot.images(30);
            robot.frapper(KeyEvent.VK_E);
        }
        assertFalse(partie.encartOuvert(), "E le referme");
        partie.monde().aller("cour", 483, 220);
        robot.frapper(KeyEvent.VK_E);
        assertTrue(!"fouille".equals(partie.encartCourant()), "on n'explique pas deux fois la meme chose");
        assertTrue(partie.progression().drapeau("vu_fouille"));
    }

    @Test
    void laNuitDuVolOnPasseParLaCourEtLaPetitePorte() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.3"), 0, Set.of(), true, Set.of("rs6_nuit", "boitier_orange", "beeper"));
        Monde monde = partie.monde();
        monde.aller("avenue_milieu", 2529, 192);
        Porte garage = monde.scene().portes.stream().filter(p -> p.cible().equals("garage")).findFirst().orElseThrow();
        assertFalse(robot.prendre(garage), "le rideau est cadenasse : on n'entre pas par devant");
        Porte hall = monde.scene().portes.stream().filter(p -> p.cible().equals("cour")).findFirst().orElseThrow();
        assertTrue(robot.prendre(hall), "le hall du 29 mene a la cour");
        assertEquals("cour", monde.carte());
        Porte petite = monde.scene().portes.stream().filter(p -> p.cible().equals("garage")).findFirst().orElseThrow();
        assertTrue(petite.x1() > 400, "la petite porte est au fond de la cour, pas a l'entree");
        robot.laisserPasser();
        assertTrue(robot.prendre(petite));
        assertEquals("garage", monde.carte());
        assertTrue(monde.joueur.x > 700, "on arrive au fond du garage, par derriere");
        Porte retour = monde.scene().portes.stream().filter(p -> p.cible().equals("cour")).findFirst().orElseThrow();
        assertTrue(retour.x1() > 700, "et la porte du fond ramene dans la cour");
    }
}
