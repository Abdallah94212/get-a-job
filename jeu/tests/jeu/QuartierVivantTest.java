package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Calendrier;
import histoire.Organisation;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le quartier vit sans le joueur, et reagit a lui : les passants vont et viennent (plus l'apres-midi que la nuit),
 * les habitants suivent leur routine et suivent des yeux le heros, s'ecartent quand on les bouscule ; frappes, ils
 * fuient, ripostent ou s'en souviennent ; la police s'en mele. Les petits de l'equipe partis en operation ne sont pas
 * a la boutique. Et le combat : les ennemis ripostent, se gardent, tombent et se relevent ; le heros enchaine, tombe,
 * se releve.
 */
class QuartierVivantTest {
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
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.2"), 0, Set.of(), true, Set.of());
        robot.laisserPasser();
    }

    private Monde monde() {
        return partie.monde();
    }

    private long passants() {
        return monde().habitants().stream().filter(h -> h.passant).count();
    }

    private int foule(int heure) {
        partie.progression().etat().calendrier().regler(5, heure * 60);
        monde().aller("avenue_milieu", 2000, 230);
        int pire = 0;
        for (int i = 0; i < 30 * 40; i++) {                            // quarante secondes au meme endroit
            robot.image();
            partie.progression().etat().calendrier().regler(5, heure * 60);
            pire = Math.max(pire, (int) passants());
        }
        return pire;
    }

    @Test
    void desPassantsVontEtViennentPlusLApresMidiQueLaNuit() {
        int nuit = foule(3);
        int apresMidi = foule(15);
        assertTrue(nuit >= 1, "meme la nuit, quelqu'un passe");
        assertTrue(apresMidi > nuit, "plus de monde l'apres-midi (" + apresMidi + ") que la nuit (" + nuit + ")");
        Monde.Habitant p = monde().habitants().stream().filter(h -> h.passant).findFirst().orElseThrow();
        float x = p.acteur.x;
        robot.images(60);
        assertTrue(!monde().habitants().contains(p) || Math.abs(p.acteur.x - x) > 1 || p.partenaire != null,
                "un passant marche (ou discute)");
    }

    @Test
    void lesHabitantsSuiventLeurRoutineEtRegardentLeHeros() {
        monde().aller("avenue_milieu", 400, 230);                      // loin de Savon
        Monde.Habitant savon = monde().habitant("rayan");
        assertNotNull(savon);
        float minX = savon.acteur.x;
        float maxX = savon.acteur.x;
        for (int i = 0; i < 30 * 40; i++) {
            robot.image();
            minX = Math.min(minX, savon.acteur.x);
            maxX = Math.max(maxX, savon.acteur.x);
        }
        assertTrue(maxX - minX > 8, "Savon va et vient devant la laverie : " + (maxX - minX));
        assertTrue(maxX - minX <= 2 * 34 + 12, "sans s'eloigner de sa place");
        Monde.Habitant ticket = monde().habitant("moussa");
        monde().aller("avenue_milieu", ticket.acteur.x - 40, ticket.acteur.y);
        monde().joueur.versDroite = true;
        robot.images(20);
        assertEquals(-1, ticket.sens, "Ticket regarde le heros, a sa gauche");
        monde().aller("avenue_milieu", ticket.acteur.x + 40, ticket.acteur.y);
        ticket = monde().habitant("moussa");
        robot.images(20);
        assertEquals(1, ticket.sens, "puis a sa droite");
    }

    @Test
    void bousculeIlSEcarteEtLeDit() {
        monde().aller("avenue_milieu", 400, 230);
        Monde.Habitant savon = monde().habitant("rayan");
        savon.pause = 99;
        savon.enRoute = false;
        float y = savon.acteur.y;
        monde().aller("avenue_milieu", savon.acteur.x - 20, savon.acteur.y);
        Monde.Habitant revu = monde().habitant("rayan");
        robot.touches.presser(KeyEvent.VK_RIGHT);
        for (int i = 0; i < 40 && revu.bulle == null; i++) {
            robot.image();
        }
        robot.touches.relacher(KeyEvent.VK_RIGHT);
        assertNotNull(revu.bulle, "il le dit");
        assertTrue(Math.abs(revu.acteur.y - y) >= 9, "et il s'ecarte");
    }

    /** Pose le heros juste devant cet habitant, tourne vers lui, et frappe. */
    private void frapper(Monde.Habitant h) {
        monde().joueur.x = h.acteur.x - 16;
        monde().joueur.y = h.acteur.y;
        monde().joueur.versDroite = true;
        robot.frapper(KeyEvent.VK_J);
        robot.images(4);
    }

    @Test
    void frappeUnePassanteFuitEtLesAutresReagissent() {
        monde().aller("avenue_bas", 600, 230);
        Monde.Habitant passante = monde().habitant("passant_bas");
        assertNotNull(passante);
        passante.acteur.x = 700;
        float avant = passante.acteur.x;
        frapper(passante);
        assertEquals("fuite", passante.reaction, "elle fuit");
        assertNotNull(passante.bulle, "elle crie");
        robot.images(60);
        assertTrue(Math.abs(passante.acteur.x - avant) > 40, "elle s'est eloignee");
        assertFalse(partie.combat().actif(), "elle ne se bat pas");
        robot.images(30 * 12);
        assertNull(passante.reaction, "calmee, elle retourne a sa vie");
    }

    @Test
    void unCostaudRipostePuisNInsistePlus() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.3"), 0, Set.of(), true, Set.of());
        robot.laisserPasser();
        monde().aller("avenue_milieu", 2900, 230);
        Monde.Habitant sportif = monde().habitant("client_salle");
        assertNotNull(sportif);
        frapper(sportif);
        assertTrue(partie.combat().actif(), "il riposte : c'est une bagarre");
        assertEquals("client_salle", partie.combat().rue());
        robot.laisserPasser();                                         // l'encart du combat, puis la bagarre
        assertFalse(partie.combat().actif());
        assertEquals("fuite", sportif.reaction, "battu, il s'en va");
        assertNotNull(sportif.acteur.texture, "il reprend son dessin");
    }

    @Test
    void laPoliceIntervientEtDonneUneAmende() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.3"), 0, Set.of(), true, Set.of());
        robot.laisserPasser();
        monde().aller("avenue_milieu", 1600, 230);
        Monde.Habitant flic = monde().habitant("police_a");
        assertNotNull(flic);
        partie.progression().etat().gagner(200, "ESSAI");
        int argent = partie.progression().etat().argent();
        frapper(flic);
        assertTrue(partie.combat().actif());
        assertTrue(partie.combat().ennemis().stream().anyMatch(e -> e.genre == Combat.POLICIER));
        robot.images(25);
        robot.frapper(KeyEvent.VK_E);                                  // l'encart du combat se referme
        partie.progression().etat().blesser(99, 0);                    // il ne se defend pas
        for (int i = 0; i < 600 && partie.combat().actif(); i++) {
            robot.image();
        }
        assertFalse(partie.combat().actif());
        assertTrue(partie.progression().etat().argent() < argent, "interpelle : une amende");
        assertTrue(partie.progression().etat().journal().stream().anyMatch(l -> l.libelle().startsWith("AMENDE")));
    }

    @Test
    void unPetitDeLEquipeSeSouvientDuCoupEtNEstPasLaQuandIlTravaille() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.6"), 1, Set.of(), true,
                Set.of("organisation", "recrue_youngssef", "recrue_rayan", "recrue_moussa", "youngssef_paye", "youngssef_promis"));
        Organisation orga = partie.progression().etat().organisation();
        orga.recruter(partie.progression());
        partie.progression().etat().gagner(2000, "ESSAI");
        monde().aller("epicerie", 200, 220);
        Monde.Habitant savon = monde().habitant("rayan_boutique");
        assertNotNull(savon, "Savon est a la boutique");
        int confiance = orga.membre("rayan").confiance;
        frapper(savon);
        assertTrue(orga.membre("rayan").confiance < confiance, "frappe, sa confiance baisse");
        assertFalse(partie.combat().actif(), "il ne se bat pas contre toi");
        assertNull(orga.lancer("livraisons", "youngssef", Organisation.Paie.JUSTE, partie.progression().etat()));
        monde().peupler();
        assertNull(monde().habitant("youngssef_boutique"), "Youngssef est sur la route");
        monde().aller("avenue_milieu", 1500, 230);
        assertNull(monde().habitant("scooter_epicerie"), "avec son scooter");
        for (int i = 0; i < 20 && !orga.enCours().isEmpty(); i++) {
            orga.avancer(200, partie.progression().etat());
        }
        monde().peupler();
        assertNotNull(monde().habitant("scooter_epicerie"), "rentre, le scooter est gare devant l'epicerie");
    }

    // ------------------------------------------------------------------ le combat

    private Combat.Ennemi ennemi(Combat.Genre genre, int nombre) {
        monde().aller("cour", 300, 236);
        monde().joueur.versDroite = true;
        partie.combat().commencer(genre, nombre, 300, 236, "guetteur_battu", "guetteur_gagne", monde().scene());
        return partie.combat().ennemis().get(0);
    }

    @Test
    void lesAliensRipostentMemeQuandOnFrappeSansArret() {
        ennemi(Combat.ALIEN, 3);
        partie.progression().etat().soigner(100);
        float sante = partie.progression().etat().sante();
        EnumSet<Combat.Phase> vues = EnumSet.noneOf(Combat.Phase.class);
        for (int i = 0; i < 30 * 12 && partie.combat().actif(); i++) {
            Combat.Ennemi proche = partie.combat().ennemis().stream().filter(Combat.Ennemi::vivant)
                    .min((a, b) -> Float.compare(Math.abs(a.acteur.x - monde().joueur.x), Math.abs(b.acteur.x - monde().joueur.x)))
                    .orElse(null);
            if (proche != null) {
                monde().joueur.versDroite = proche.acteur.x > monde().joueur.x;
            }
            robot.touches.presser(KeyEvent.VK_J);                      // il martele J
            robot.image();
            robot.touches.relacher(KeyEvent.VK_J);
            robot.image();
            partie.combat().ennemis().forEach(e -> vues.add(e.etat));
            partie.progression().etat().soigner(1);                    // pour durer
        }
        assertTrue(partie.progression().etat().sante() < sante + 30 * 12 / 2, "ils touchent");
        assertTrue(vues.contains(Combat.Phase.PREPARE) && vues.contains(Combat.Phase.FRAPPE), "ils attaquent : " + vues);
        assertTrue(vues.contains(Combat.Phase.SOL) || vues.contains(Combat.Phase.GARDE) || vues.contains(Combat.Phase.ESQUIVE),
                "ils se gardent, esquivent ou tombent : " + vues);
    }

    @Test
    void troisCoupsDeSuiteIlTombeSeReleveEtRevientEnColere() {
        Combat.Ennemi e = ennemi(Combat.GUETTEUR, 1);
        e.acteur.x = 318;
        e.repos = 10;
        for (int k = 0; k < 3 && e.etat != Combat.Phase.SOL; k++) {
            e.acteur.x = 318;
            e.etat = Combat.Phase.RECULE;                               // il encaisse, sans se garder
            e.minuteur = 5;
            e.vx = 0;
            robot.frapper(KeyEvent.VK_J);
            robot.images(10);
        }
        assertEquals(Combat.Phase.SOL, e.etat, "au troisieme coup, il tombe");
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_J);
        robot.images(8);
        assertEquals(pv, e.pv, 0.01f, "on ne frappe pas un homme a terre");
        for (int i = 0; i < 60 && e.etat != Combat.Phase.APPROCHE; i++) {
            robot.image();
        }
        assertEquals(Combat.Phase.APPROCHE, e.etat, "il se releve");
        assertTrue(e.colere, "en colere : il attaque tout de suite");
    }

    @Test
    void leHerosEnchaineTroisCoupsJusquAuCrochet() {
        Combat.Ennemi e = ennemi(Combat.GUETTEUR, 1);
        e.acteur.x = 400;
        boolean crochet = false;
        robot.touches.presser(KeyEvent.VK_J);
        robot.image();
        robot.touches.relacher(KeyEvent.VK_J);
        for (int i = 0; i < 40; i++) {
            if (i % 5 == 2) {
                robot.touches.presser(KeyEvent.VK_J);
            }
            robot.image();
            robot.touches.relacher(KeyEvent.VK_J);
            crochet |= partie.combat().geste() == Combat.CROCHET;
        }
        assertTrue(crochet, "direct, second direct, crochet");
    }

    @Test
    void troisCoupsEncaissesLeHerosTombeEtSeReleve() {
        Combat.Ennemi e = ennemi(Combat.GUETTEUR, 1);
        partie.progression().etat().soigner(100);
        e.acteur.x = 318;
        e.repos = 0;
        boolean auSol = false;
        boolean releve = false;
        for (int i = 0; i < 30 * 10; i++) {
            robot.image();
            e.acteur.x = Math.max(e.acteur.x, monde().joueur.x + 14);
            if (partie.combat().heroAuSol()) {
                auSol = true;
                assertTrue(partie.combat().occupe(), "a terre, il ne fait rien d'autre");
            } else if (auSol) {
                releve = true;
                break;
            }
        }
        assertTrue(auSol, "il finit par tomber");
        assertTrue(releve, "et se relever");
    }

    @Test
    void lHorlogeDesPassantsSuitLeCalendrier() {
        assertEquals(1, Vie.foule(Calendrier.Moment.NUIT, false));
        assertTrue(Vie.foule(Calendrier.Moment.APRES_MIDI, true) < Vie.foule(Calendrier.Moment.APRES_MIDI, false), "la pluie vide la rue");
    }
}
