package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le combat a plusieurs possibilites : balayage, coup de pied charge, objets lances (trajectoire, impact, retombee),
 * orientation vers la cible, combats a plusieurs (pas de corps qui se superposent, pas plus de deux attaques a la
 * fois), et la fuite d'une bagarre de rue. Aucun coup ne touche avant sa phase d'impact.
 */
class CombatAvanceTest {
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
        partie.progression().marquer("+vu_combat +vu_astuce_touches +vu_astuce_garde +vu_astuce_entoure +vu_astuce_lancer +vu_astuce_fuite");
        partie.monde().aller("cour", 300, 236);
        partie.monde().joueur.versDroite = true;
    }

    private Combat combat() {
        return partie.combat();
    }

    private Combat.Ennemi poser(Combat.Ennemi e, float x, Combat.Phase etat) {
        e.acteur.x = x;
        e.acteur.y = 236;
        e.etat = etat;
        e.minuteur = 5;
        e.repos = 10;
        e.vx = 0;
        return e;
    }

    @Test
    void leBalayageFaitTomberDevantEtDerriereMemeEnGarde() {
        combat().commencer(Combat.GUETTEUR, 2, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi devant = poser(combat().ennemis().get(0), 322, Combat.Phase.GARDE);
        Combat.Ennemi derriere = poser(combat().ennemis().get(1), 280, Combat.Phase.RECULE);
        robot.frapper(KeyEvent.VK_L);
        robot.images(12);
        assertEquals(Combat.Phase.SOL, devant.etat, "devant, garde levee ou pas : au sol");
        assertEquals(Combat.Phase.SOL, derriere.etat, "derriere aussi");
    }

    @Test
    void leCoupDePiedChargeCasseLaGarde() {
        combat().commencer(Combat.GUETTEUR, 1, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi e = poser(combat().ennemis().get(0), 326, Combat.Phase.GARDE);
        float pv = e.pv;
        robot.touches.presser(KeyEvent.VK_K);
        robot.images(14);                                              // pres d'une demi-seconde : il charge
        e.etat = Combat.Phase.GARDE;
        e.minuteur = 5;
        robot.touches.relacher(KeyEvent.VK_K);
        robot.images(16);
        assertEquals(pv - Combat.PIED_FORT.degats(), e.pv, 0.01f, "toute la force passe, malgre la garde");
        assertEquals(Combat.Phase.SOL, e.etat, "et il tombe");
    }

    @Test
    void unePierreLanceeVoleToucheEtNeToucheQuEnArrivant() {
        combat().commencer(Combat.GUETTEUR, 1, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi e = poser(combat().ennemis().get(0), 370, Combat.Phase.RECULE);
        partie.progression().etat().inventaire().ajouter("pierre", 1);
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_H);
        assertEquals(0, partie.progression().etat().inventaire().quantite("pierre"), "elle a quitte la poche");
        assertEquals(pv, e.pv, 0.01f, "pas de degats a l'appui : il faut qu'elle arrive");
        boolean enLAir = false;
        for (int i = 0; i < 30 && e.pv == pv; i++) {
            enLAir |= !combat().projectiles().isEmpty();
            e.acteur.x = 370;
            e.etat = Combat.Phase.RECULE;
            e.minuteur = 5;
            robot.image();
        }
        assertTrue(enLAir, "on la voit voler");
        assertTrue(e.pv < pv, "elle touche");
    }

    @Test
    void uneArmeLanceeRetombeEtSeRamasse() {
        partie.progression().etat().prendreArme("barre");
        combat().commencer(Combat.GUETTEUR, 1, 300, 236, null, null, partie.monde().scene());
        poser(combat().ennemis().get(0), 120, Combat.Phase.RECULE);    // personne dans la trajectoire
        int sol = partie.progression().etat().auSol().size();
        robot.frapper(KeyEvent.VK_H);
        robot.images(40);
        assertEquals(null, partie.progression().etat().armeEnMain());
        assertEquals(sol + 1, partie.progression().etat().auSol().size(), "la barre est par terre, plus loin");
    }

    @Test
    void lHerosSeTourneVersLAdversaireDansSonDos() {
        combat().commencer(Combat.GUETTEUR, 1, 300, 236, null, null, partie.monde().scene());
        poser(combat().ennemis().get(0), 282, Combat.Phase.RECULE);
        partie.monde().joueur.versDroite = true;
        robot.frapper(KeyEvent.VK_J);
        assertFalse(partie.monde().joueur.versDroite, "il se retourne pour frapper");
    }

    @Test
    void aPlusieursLesCorpsNeSeSuperposentPasEtDeuxAuPlusAttaquent() {
        combat().commencer(Combat.ALIEN, 4, 300, 236, null, null, partie.monde().scene());
        partie.progression().etat().soigner(100);
        int pire = 0;
        int superposes = 0;
        for (int i = 0; i < 30 * 8; i++) {
            robot.image();
            partie.progression().etat().soigner(100);
            long attaquent = combat().ennemis().stream().filter(e -> !e.colere
                    && (e.etat == Combat.Phase.PREPARE || e.etat == Combat.Phase.FRAPPE || e.etat == Combat.Phase.BOND)).count();
            pire = (int) Math.max(pire, attaquent);
            for (Combat.Ennemi a : combat().ennemis()) {
                for (Combat.Ennemi b : combat().ennemis()) {
                    if (a != b && a.vivant() && b.vivant() && Math.abs(a.acteur.x - b.acteur.x) < 4 && Math.abs(a.acteur.y - b.acteur.y) < 4) {
                        superposes++;
                    }
                }
            }
        }
        assertTrue(pire <= 2, "pas plus de deux attaques a la fois : " + pire);
        assertTrue(superposes < 30, "les corps ne restent pas l'un dans l'autre : " + superposes);
        boolean gauche = combat().ennemis().stream().anyMatch(e -> e.acteur.x < partie.monde().joueur.x);
        boolean droite = combat().ennemis().stream().anyMatch(e -> e.acteur.x > partie.monde().joueur.x);
        assertTrue(gauche && droite, "ils l'encerclent");
    }

    @Test
    void onPeutFuirUneBagarreDeRue() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.3"), 0, Set.of(), true, Set.of("vu_combat"));
        partie.monde().aller("avenue_milieu", 2900, 230);
        Monde.Habitant sportif = partie.monde().habitant("client_salle");
        combat().bagarre(Combat.PASSANT, sportif.acteur, "client_salle");
        sportif.reaction = "bagarre";
        partie.monde().joueur.x = sportif.acteur.x + 400;
        for (int i = 0; i < 30 * 4 && combat().actif(); i++) {
            partie.monde().joueur.x = sportif.acteur.x + 400;
            robot.image();
        }
        assertFalse(combat().actif(), "assez loin, assez longtemps : la bagarre s'arrete");
    }
}
