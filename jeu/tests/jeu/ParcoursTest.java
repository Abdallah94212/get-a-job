package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import decor.Joueur;
import decor.Porte;
import decor.Scene;
import histoire.Campagne;
import histoire.Cinematique;
import histoire.Figurant;
import histoire.Mission;
import histoire.Objectif;
import histoire.Plan;
import histoire.Point;
import histoire.Progression;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Le jeu entier, joue sans fenetre : du menu a la fin de l'acte 2, avec ses portes fermees et sa sauvegarde. */
class ParcoursTest {
    @TempDir
    Path dossier;
    private Robot robot;

    @BeforeEach
    void preparer() throws IOException {
        robot = new Robot(dossier.resolve("partie.properties"), null);
    }

    private void nouvellePartie() {
        assertNull(robot.partie(), "le jeu s'ouvre sur le menu");
        robot.frapper(KeyEvent.VK_E);
        assertNotNull(robot.partie(), "Nouvelle partie lance la campagne");
    }

    /** Fait les objectifs secondaires qui se trouvent sur la carte courante. */
    private void faireLesSecondaires() {
        EcranPartie partie = robot.partie();
        Mission mission = partie.progression().courante();
        for (Objectif facultatif : mission.facultatifs()) {
            Campagne campagne = robot.jeu.res.campagne;
            if (!partie.progression().attendue(facultatif.genre(), facultatif.cible())
                    || !partie.monde().carte().equals(campagne.carteDe(facultatif))) {
                continue;
            }
            float[] ou = facultatif.genre() == Objectif.Genre.AGIR
                    ? robot.jeu.res.position(campagne.points().get(facultatif.cible()).lieu())
                    : robot.jeu.res.position(campagne.figurants().get(facultatif.cible()).lieu());
            partie.monde().joueur.x = ou[0];
            partie.monde().joueur.y = Math.max(partie.monde().scene().yMin, ou[1]);
            robot.frapper(KeyEvent.VK_E);
            robot.laisserPasser();
        }
    }

    @Test
    void nouvellePartieLanceLIntroductionEtOnPeutLaPasser() {
        nouvellePartie();
        robot.images(3);
        assertTrue(robot.partie().enScene(), "la campagne commence par la cinematique d'introduction");
        robot.frapper(KeyEvent.VK_ESCAPE);
        assertFalse(robot.partie().enScene(), "Echap passe la cinematique sans bloquer la partie");
        assertEquals("appartement", robot.partie().monde().carte());
        assertEquals("0.1", robot.partie().progression().courante().id());
        assertTrue(robot.partie().progression().debutJoue());
    }

    @Test
    void leLaboResteFermeTantQueLHistoireNeLOuvrePas() {
        nouvellePartie();
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        partie.monde().aller("avenue_milieu", 100, 210);
        for (String ferme : List.of("labo", "garage", "epicerie")) {
            Porte porte = partie.monde().scene().portes.stream().filter(p -> p.cible().equals(ferme)).findFirst().orElseThrow();
            assertFalse(robot.prendre(porte), "on ne doit pas entrer dans " + ferme + " au debut du jeu");
            assertEquals("avenue_milieu", partie.monde().carte());
        }
        assertTrue(robot.prendre(partie.monde().scene().portes.stream().filter(Porte::automatique).findFirst().orElseThrow())
                || !partie.monde().carte().equals("avenue_milieu"), "les bouts de l'avenue restent ouverts");
    }

    @Test
    void onPeutJouerToutesLesMissionsJusquALaFin() {
        nouvellePartie();
        Progression progression = robot.partie().progression();
        Set<String> scenesVues = new HashSet<>();
        for (int tour = 0; tour < 500 && !progression.terminee(); tour++) {
            robot.laisserPasser();
            if (progression.terminee()) {
                break;
            }
            Mission mission = progression.courante();
            faireLesSecondaires();
            assertFalse(robot.partie().enScene() || robot.partie().auBilan());
            String objectif = mission.id() + ", objectif " + (progression.etape() + 1) + " : " + progression.objectif().texte();
            assertTrue(robot.avancerVersObjectif(), "impossible d'avancer : " + objectif);
            scenesVues.add(mission.id());
        }
        robot.laisserPasser();
        assertTrue(progression.terminee(), "la campagne doit pouvoir se terminer");
        assertEquals(9, scenesVues.size(), "chaque mission a ete jouee");
        for (String lieu : List.of("metro", "garage", "namek", "labo", "epicerie")) {
            assertTrue(progression.lieuOuvert(lieu), lieu + " doit etre ouvert a la fin");
        }
        assertFalse(progression.lieuOuvert("hall_tour"), "la tour appartient aux actes suivants");
        assertTrue(robot.jeu.sauvegarde.existe());
    }

    @Test
    void onPeutToutJouerAPiedEnTenantLesTouches() {
        nouvellePartie();
        Progression progression = robot.partie().progression();
        for (int tour = 0; tour < 200 && !progression.terminee(); tour++) {
            robot.laisserPasser();
            if (progression.terminee()) {
                break;
            }
            String objectif = progression.courante().id() + ", objectif " + (progression.etape() + 1) + ", carte "
                    + robot.partie().monde().carte();
            assertTrue(robot.marcherVersObjectif(), "impossible d'y aller a pied : " + objectif);
        }
        robot.laisserPasser();
        assertTrue(progression.terminee(), "la campagne se termine en marchant, sans jamais se poser sur une cible");
    }

    @Test
    void quitterPuisContinuerReprendLaMemeMissionAuMemeEndroit() throws IOException {
        nouvellePartie();
        Progression progression = robot.partie().progression();
        while (!progression.courante().id().equals("1.3")) {
            robot.laisserPasser();
            if (!progression.courante().id().equals("1.3")) {
                assertTrue(robot.avancerVersObjectif());
            }
        }
        robot.laisserPasser();
        assertTrue(robot.avancerVersObjectif());
        robot.laisserPasser();
        int etape = progression.etape();
        String carte = robot.partie().monde().carte();
        robot.frapper(KeyEvent.VK_ESCAPE);
        for (int i = 0; i < 4; i++) {
            robot.frapper(KeyEvent.VK_DOWN);
        }
        robot.frapper(KeyEvent.VK_E);
        assertNull(robot.partie(), "Menu principal ramene au menu");

        Robot retour = new Robot(dossier.resolve("partie.properties"), null);
        retour.frapper(KeyEvent.VK_E);
        assertNotNull(retour.partie(), "avec une partie enregistree, le menu propose d'abord Continuer");
        assertEquals("1.3", retour.partie().progression().courante().id());
        assertEquals(etape, retour.partie().progression().etape());
        assertEquals(carte, retour.partie().monde().carte());
        assertFalse(retour.partie().enScene(), "on ne revoit pas l'ouverture d'une mission deja commencee");
        assertTrue(retour.partie().progression().lieuOuvert("labo"), "le labo s'ouvre avec la mission 1.3");
        assertFalse(retour.partie().progression().lieuOuvert("namek"));
    }

    /** Les cases ou le joueur peut se tenir sur une carte, en partant de toutes ses entrees. */
    private Set<Long> accessibles(String carte) {
        Ressources res = robot.jeu.res;
        Scene scene = res.carte(carte);
        Deque<int[]> file = new ArrayDeque<>();
        Set<Long> vues = new HashSet<>();
        List<float[]> entrees = new ArrayList<>();
        entrees.add(new float[] {scene.departX, scene.departY});
        for (String autre : List.of("avenue_bas", "avenue_milieu", "avenue_haut", "maitre_poulet", "appartement", "labo", "epicerie",
                "garage", "namek", "metro", "boucherie")) {
            for (Porte porte : res.carte(autre).portes) {
                if (porte.cible().equals(carte)) {
                    entrees.add(new float[] {porte.arriveeX(), porte.arriveeY()});
                }
            }
        }
        for (float[] entree : entrees) {
            assertFalse(scene.bloque(entree[0], entree[1], Joueur.RAYON), carte + " : on arrive dans un obstacle en " + entree[0]);
            file.add(new int[] {Math.round(entree[0] / 4), Math.round(entree[1] / 4)});
        }
        while (!file.isEmpty()) {
            int[] c = file.poll();
            long cle = ((long) c[0] << 32) | (c[1] & 0xFFFFFFFFL);
            if (scene.bloque(c[0] * 4, c[1] * 4, Joueur.RAYON) || !vues.add(cle)) {
                continue;
            }
            file.add(new int[] {c[0] + 1, c[1]});
            file.add(new int[] {c[0] - 1, c[1]});
            file.add(new int[] {c[0], c[1] + 1});
            file.add(new int[] {c[0], c[1] - 1});
        }
        return vues;
    }

    private static boolean aPortee(Set<Long> cases, float x, float y, float porteeX, float porteeY) {
        for (long cle : cases) {
            float cx = (int) (cle >> 32) * 4f;
            float cy = (int) cle * 4f;
            if (Math.abs(cx - x) <= porteeX && Math.abs(cy - y) <= porteeY) {
                return true;
            }
        }
        return false;
    }

    @Test
    void toutCeQueLHistoireDemandeEstAccessibleAPied() {
        Campagne campagne = robot.jeu.res.campagne;
        for (Point point : campagne.points().values()) {
            float[] ou = robot.jeu.res.position(point.lieu());
            assertTrue(aPortee(accessibles(point.lieu().carte()), ou[0], ou[1], Monde.PORTEE_X - 4, Monde.PORTEE_Y - 4),
                    "on ne peut pas atteindre " + point.id());
        }
        for (Mission mission : campagne.missions()) {
            for (Objectif objectif : mission.etapes()) {
                if (objectif.genre() == Objectif.Genre.PARLER) {
                    Figurant figurant = campagne.figurants().get(objectif.cible());
                    float[] ou = robot.jeu.res.position(figurant.lieu());
                    assertTrue(aPortee(accessibles(figurant.lieu().carte()), ou[0], ou[1], Monde.PORTEE_X - 4, Monde.PORTEE_Y - 4),
                            "on ne peut pas parler a " + figurant.id());
                }
            }
        }
        for (String carte : List.of("avenue_bas", "avenue_milieu", "avenue_haut", "garage", "maitre_poulet", "appartement", "labo",
                "epicerie", "namek", "metro", "boucherie")) {
            Set<Long> cases = accessibles(carte);
            for (Porte porte : robot.jeu.res.carte(carte).portes) {
                boolean atteinte = cases.stream().anyMatch(c -> porte.devant((int) (c >> 32) * 4f, (int) (long) c * 4f));
                assertTrue(atteinte, carte + " : la porte vers " + porte.cible() + " est hors d'atteinte");
            }
        }
    }

    @Test
    void chaqueCinematiqueSeJoueJusquAuBout() {
        nouvellePartie();
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        Ressources res = robot.jeu.res;
        Scenographe scenographe = new Scenographe(res, partie.monde(), partie.progression(), robot.jeu.reglages, robot.jeu.audio,
                robot.jeu.affichage);
        for (Cinematique cinematique : res.campagne.cinematiques().values()) {
            for (Plan plan : cinematique.plans()) {
                if (plan.genre() == Plan.Genre.DIT) {
                    Campagne.Voix voix = res.campagne.voix(plan.mot(0));
                    assertNotNull(res.image(voix.portrait()), cinematique.id() + " : pas de portrait pour " + plan.mot(0));
                } else if (plan.genre() == Plan.Genre.ACTEUR) {
                    assertNotNull(res.image(plan.mot(1)), cinematique.id() + " : image inconnue " + plan.mot(1));
                }
            }
            if (cinematique.id().equals("confusion")) {
                partie.monde().aller("garage", 396, 208);
            } else if (cinematique.id().startsWith("rame") || cinematique.id().equals("frotteur") || cinematique.id().equals("femme")) {
                partie.monde().aller("metro", 470, 236);
            }
            scenographe.jouer(cinematique);
            Touches touches = new Touches();
            for (int i = 0; i < 20000 && scenographe.enCours(); i++) {
                if (i % 2 == 0) {
                    touches.presser(KeyEvent.VK_E);
                } else {
                    touches.relacher(KeyEvent.VK_E);
                }
                scenographe.maj(Robot.PAS, touches);
                touches.finImage();
                scenographe.dessiner(robot.rendu, true);
            }
            assertFalse(scenographe.enCours(), cinematique.id() + " ne se termine pas");
        }
        for (Figurant figurant : res.campagne.figurants().values()) {
            assertNotNull(res.image(figurant.image()), "image inconnue pour " + figurant.id());
            assertNotNull(res.position(figurant.lieu()));
        }
    }


    /** Joue la campagne jusqu'au debut de la mission voulue, en prenant toujours la premiere reponse. */
    private void allerJusqua(String mission) {
        Progression progression = robot.partie().progression();
        for (int tour = 0; tour < 400 && !mission.equals(progression.courante().id()); tour++) {
            robot.laisserPasser();
            if (!mission.equals(progression.courante().id())) {
                assertTrue(robot.avancerVersObjectif(), "bloque avant " + mission);
            }
        }
        robot.laisserPasser();
    }

    @Test
    void onPeutQuitterEtReprendreApresChaqueObjectif() throws IOException {
        nouvellePartie();
        Progression progression = robot.partie().progression();
        String vu = "";
        for (int tour = 0; tour < 500 && !progression.terminee(); tour++) {
            robot.laisserPasser();
            if (progression.terminee()) {
                break;
            }
            String ici = progression.courante().id() + "/" + progression.etape();
            if (!ici.equals(vu)) {
                vu = ici;
                robot.partie().sauvegarder();
                Robot reprise = new Robot(dossier.resolve("partie.properties"), null);
                reprise.frapper(KeyEvent.VK_E);
                EcranPartie p = reprise.partie();
                assertNotNull(p, "Continuer doit reprendre la partie en " + ici);
                assertEquals(ici, p.progression().courante().id() + "/" + p.progression().etape());
                assertEquals(robot.partie().monde().carte(), p.monde().carte(), "meme lieu en " + ici);
                assertEquals(progression.drapeaux(), p.progression().drapeaux(), "memes souvenirs en " + ici);
                assertFalse(p.enScene(), "une scene deja vue ne se rejoue pas en " + ici);
                assertNotNull(p.monde().direction(p.progression().objectif()), "on sait ou aller apres la reprise, en " + ici);
            }
            assertTrue(robot.avancerVersObjectif(), "bloque en " + ici);
        }
        assertTrue(progression.terminee());
    }

    @Test
    void lesChoixDeDialogueChangentLaSuite() throws IOException {
        nouvellePartie();
        Progression progression = robot.partie().progression();
        while (!(progression.courante().id().equals("1.1") && progression.etape() == 5)) {
            robot.laisserPasser();
            assertTrue(robot.avancerVersObjectif());
        }
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        Monde.Habitant omar = partie.monde().habitant("omar");
        partie.monde().joueur.x = omar.acteur.x;
        partie.monde().joueur.y = 214;
        robot.frapper(KeyEvent.VK_E);
        robot.repondre(1);
        robot.laisserPasser();
        assertTrue(progression.drapeau("omar_doit"), "« D'accord, demain » : Omar ne paie pas");
        assertFalse(progression.drapeau("omar_paye"));
        robot.laisserPasser();

        Robot autre = new Robot(dossier.resolve("autre.properties"), null);
        autre.frapper(KeyEvent.VK_E);
        Progression p2 = autre.partie().progression();
        while (!(p2.courante().id().equals("1.1") && p2.etape() == 5)) {
            autre.laisserPasser();
            assertTrue(autre.avancerVersObjectif());
        }
        autre.laisserPasser();
        Monde.Habitant omar2 = autre.partie().monde().habitant("omar");
        autre.partie().monde().joueur.x = omar2.acteur.x;
        autre.partie().monde().joueur.y = 214;
        autre.frapper(KeyEvent.VK_E);
        autre.repondre(0);
        autre.laisserPasser();
        assertTrue(p2.drapeau("omar_paye"), "« Geto a dit : pas sans l'argent » : Omar paie");
    }

    @Test
    void leScooterNExisteQueDansLesRuesEtSeRend() {
        nouvellePartie();
        Progression progression = robot.partie().progression();
        while (!(progression.courante().id().equals("1.1") && progression.etape() == 2)) {
            robot.laisserPasser();
            assertTrue(robot.avancerVersObjectif());
        }
        robot.laisserPasser();
        Monde monde = robot.partie().monde();
        assertTrue(progression.drapeau("scooter"));
        assertTrue(monde.enScooter(), "dans l'avenue, on est sur le scooter");
        assertTrue(monde.joueur.marche > decor.Joueur.MARCHE, "et on va plus vite");
        assertTrue(robot.avancerVersObjectif(), "entrer au Maitre Poulet");
        assertFalse(monde.enScooter(), "dans une boutique, on le laisse devant");
        while (!progression.courante().id().equals("1.2")) {
            robot.laisserPasser();
            assertTrue(robot.avancerVersObjectif());
        }
        assertFalse(progression.drapeau("scooter"), "le scooter est rendu a Youngssef");
    }

    @Test
    void leSonSuitLeLieuLaMeteoEtLaScene() {
        nouvellePartie();
        Audio audio = robot.jeu.audio;
        robot.images(5);
        assertTrue(audio.journal().contains("tonnerre") || audio.ambiancesCourantes().containsKey("amb_pluie")
                || audio.ambiancesCourantes().containsKey("amb_pluie_vitre"),
                "l'introduction commence sous la pluie : chez le heros, on l'entend derriere la vitre");
        robot.laisserPasser();
        robot.images(3);
        assertTrue(audio.ambiancesCourantes().containsKey("amb_pluie_vitre"), "chez soi, la pluie est etouffee par la vitre");
        EcranPartie partie = robot.partie();
        partie.monde().aller("avenue_bas", 600, 200);
        robot.images(3);
        assertTrue(audio.ambiancesCourantes().containsKey("amb_pluie"), "dehors, la pluie tombe sur la chaussee");
        assertTrue(audio.ambiancesCourantes().containsKey("amb_rue_mouillee"));
        assertTrue(audio.ambiancesCourantes().containsKey("amb_foule"), "devant le Maitre Poulet, on entend du monde");
        assertEquals("mus_rue", audio.musiqueCourante());
        partie.monde().aller("avenue_bas", 2600, 200);
        robot.images(3);
        assertFalse(audio.ambiancesCourantes().containsKey("amb_foule"), "loin des commerces, plus de voix");
        partie.monde().aller("metro", 470, 236);
        robot.images(3);
        assertTrue(audio.ambiancesCourantes().containsKey("amb_metro"));
        assertFalse(audio.ambiancesCourantes().containsKey("amb_pluie"), "sous terre, plus de pluie");
        partie.monde().aller("labo", 300, 236);
        robot.images(3);
        assertEquals("mus_labo", audio.musiqueCourante(), "le labo a son theme");
        int pas = (int) audio.journal().stream().filter(n -> n.startsWith("pas_")).count();
        robot.touches.presser(KeyEvent.VK_RIGHT);
        robot.images(60);
        robot.touches.relacher(KeyEvent.VK_RIGHT);
        assertTrue(audio.journal().stream().filter(n -> n.startsWith("pas_")).count() > pas + 3, "on entend ses pas");
    }

    @Test
    void chaqueSonDemandeExiste() {
        Audio audio = new Audio(Path.of("ressources", "sons"), new Reglages(null), true);
        Ressources res = robot.jeu.res;
        List<String> sons = new ArrayList<>(List.of("amb_pluie", "amb_gouttiere", "amb_rue_mouillee", "amb_rue", "amb_foule",
                "amb_pluie_toit", "amb_pluie_vitre", "amb_frigo", "amb_courant", "amb_machine", "amb_bouillon", "amb_ventilation",
                "amb_metro", "amb_vent", "amb_neon", "scooter", "voix", "objet", "objectif", "mission", "nouvelle_mission", "ui_deplace",
                "ui_valide", "ui_retour", "ui_refus", "porte", "porte_ouvre", "porte_lourde", "clochette", "rideau_fer", "metal",
                "outils", "metro_annonce", "vibreur", "mus_menu", "mus_rue", "mus_labo"));
        for (String sol : List.of("pas_mouille_1", "pas_mouille_2", "pas_mouille_3", "pas_dur_1", "pas_dur_2", "pas_dur_3", "pas_dur_4",
                "pas_bois_1", "pas_bois_2", "pas_bois_3", "pas_bois_4")) {
            sons.add(sol);
        }
        for (Mission mission : res.campagne.missions()) {
            if (mission.musique() != null) {
                sons.add(mission.musique());
            }
            for (Objectif objectif : mission.etapes()) {
                if (objectif.musique() != null) {
                    sons.add(objectif.musique());
                }
            }
        }
        for (Cinematique cinematique : res.campagne.cinematiques().values()) {
            for (Plan plan : cinematique.plans()) {
                if (plan.genre() == Plan.Genre.SON || (plan.genre() == Plan.Genre.MUSIQUE && !plan.mot(0).equals("silence"))) {
                    sons.add(plan.mot(0));
                }
            }
        }
        for (String son : sons) {
            assertTrue(audio.existe(son), "son introuvable : " + son);
        }
    }

    @Test
    void unGardeNeVoitQueDevantLui() {
        nouvellePartie();
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.3"), 3, Set.of(), true);
        partie.monde().aller("avenue_milieu", 100, 240);
        Monde.Habitant garde = partie.monde().habitants().stream().filter(h -> h.figurant.garde()).findFirst().orElseThrow();
        garde.sens = 1;
        partie.monde().joueur.x = garde.acteur.x + 40;
        partie.monde().joueur.y = garde.acteur.y;
        assertNotNull(partie.monde().gardeQuiVoit(), "devant le garde, on est vu");
        partie.monde().joueur.x = garde.acteur.x - 40;
        assertNull(partie.monde().gardeQuiVoit(), "dans son dos, on passe");
        partie.monde().joueur.x = garde.acteur.x + 40;
        partie.monde().joueur.y = garde.acteur.y + 40;
        assertNull(partie.monde().gardeQuiVoit(), "au large, sur la chaussee, on passe");
    }

    @Test
    void seFaireVoirRameneAuDebutDesLivraisons() {
        nouvellePartie();
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.3"), 4, Set.of(), true);
        partie.monde().aller("avenue_milieu", 100, 240);
        Monde.Habitant garde = partie.monde().habitants().stream().filter(h -> h.figurant.garde()).findFirst().orElseThrow();
        partie.monde().joueur.x = garde.acteur.x + garde.sens * 30;
        partie.monde().joueur.y = garde.acteur.y;
        robot.images(2);
        assertEquals(3, partie.progression().etape(), "repere : on recommence a la premiere livraison");
        float[] repli = robot.jeu.res.position(partie.progression().courante().repli());
        assertEquals(repli[0], partie.monde().joueur.x, 12f, "et on est ramene a l'abri");
    }

    @Test
    void leChronoEcouleFaitRecommencerLaSequence() {
        nouvellePartie();
        robot.laisserPasser();
        EcranPartie partie = robot.partie();
        partie.progression().restaurer(robot.jeu.res.campagne.rang("2.3"), 2, Set.of(), true);
        partie.monde().aller("namek", 230, 240);
        robot.images(30 * 52);
        assertEquals(0, partie.progression().etape(), "apres 50 secondes, la recolte recommence au premier plant");
    }
}
