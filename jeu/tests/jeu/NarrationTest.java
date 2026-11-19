package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Cinematique;
import histoire.Etat;
import histoire.Plan;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * La narration tient debout : une reponse silencieuse n'envoie rien ; le loyer annonce paye est paye (la banque, le
 * recapitulatif et la sauvegarde le disent) ; l'argent des clients va a Geto ; le mecano parle de la RS6 et du boitier,
 * Gero n'en dit rien ; le souvenir de Soul seme sans reveler ; l'introduction ne montre pas le mur du plan ; une scene
 * fait de la place autour d'elle.
 */
class NarrationTest {
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

    private String texteDe(String scene) {
        Cinematique c = robot.jeu.res.campagne.cinematique(scene);
        return c.plans().stream().map(p -> p.texte() + " " + String.join(" ", p.mots())).collect(Collectors.joining("\n"));
    }

    private void parlerA(String id) {
        Monde.Habitant h = partie.monde().habitant(id);
        partie.monde().joueur.x = h.acteur.x - 20;
        partie.monde().joueur.y = h.acteur.y + 4;
        partie.monde().joueur.versDroite = true;
        robot.images(2);
        robot.frapper(KeyEvent.VK_E);
    }

    @Test
    void neRienRepondreNEnvoieRien() {
        Etat etat = partie.progression().etat();
        etat.recevoir("soul", "Ne cherche pas ce numéro.", List.of(new Etat.Reponse("Qui es-tu ?", "+soul_demande"),
                new Etat.Reponse("(Ne rien répondre.)", "-")));
        assertTrue(etat.reponses("soul").get(1).silencieuse());
        String drapeaux = etat.repondre("soul", 1);
        assertEquals("", drapeaux);
        assertTrue(etat.fil("soul").stream().noneMatch(Etat.Message::deMoi), "aucun message n'est parti vers Soul");
        assertTrue(etat.reponses("soul").isEmpty(), "l'echange est clos");
        etat.recevoir("soul", "Roule.", List.of(new Etat.Reponse("Qui es-tu ?", "+soul_demande")));
        etat.repondre("soul", 0);
        assertEquals("Qui es-tu ?", etat.fil("soul").get(etat.fil("soul").size() - 1).texte(), "une vraie reponse, elle, part");
    }

    @Test
    void leLoyerAnnoncePayeEstPayeEtLeResteApresUnChargement() throws Exception {
        Etat etat = partie.progression().etat();
        assertEquals(3, etat.loyerDu(), "trois mois de retard au debut, comme le dit la lettre");
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.4"), 0, Set.of(), false, Set.of());
        etat.calendrier().regler(2, 3 * 60);                           // la nuit de la 1.3 ; la 1.4 est trois semaines plus tard
        robot.image();
        assertTrue(partie.auCalendrier());
        assertEquals(0, etat.loyerDu(), "trois semaines de livraisons : le retard est regle, comme le dit la scene");
        assertTrue(etat.journal().stream().anyMatch(l -> l.libelle().startsWith("LIVRAISONS DE CELL POUR GETO")));
        assertTrue(etat.journal().stream().filter(l -> l.libelle().startsWith("LOYER DU 12")).count() >= 4,
                "les quatre mois dus, chacun une ligne de la banque");
        robot.images(40);
        robot.frapper(KeyEvent.VK_E);
        robot.laisserPasser();
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(2);
        robot.jeu.continuer();
        assertEquals(0, robot.partie().progression().etat().loyerDu(), "apres un chargement, toujours paye");
    }

    @Test
    void leRecapitulatifNeRepetePasLeScooter() {
        partie.progression().marquer("-scooter -scooter_gare");
        List<String> pendant = partie.progression().laisserPasser(20 * 60);
        assertTrue(pendant.stream().noneMatch(l -> l.contains("SCOOTER")), "rien a dire du scooter s'il n'a pas bouge");
        partie.progression().marquer("+scooter_gare");
        pendant = partie.progression().laisserPasser(20 * 60);
        assertEquals(1, pendant.stream().filter(l -> l.contains("SCOOTER")).count(), "dit une fois, quand ca arrive");
    }

    @Test
    void lArgentDesClientsVaAGeto() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.1"), 3, Set.of(), true,
                Set.of("scooter", "pret_scooter", "vu_presente_diallo"));
        Etat etat = partie.progression().etat();
        partie.monde().aller("avenue_bas", 400, 220);
        int argent = etat.argent();
        parlerA("diallo");
        robot.laisserPasser();
        assertEquals(12, etat.inventaire().quantite("recette_geto"), "les douze euros de Lili sont pour Geto");
        assertEquals(argent + 3, etat.argent(), "seul le pourboire est pour toi");
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.2"), 0, Set.of(), true, Set.of("omar_doit", "vu_presente_geto"));
        partie.monde().aller("maitre_poulet", 120, 236);
        argent = etat.argent();
        parlerA("geto");
        robot.laisserPasser();
        assertFalse(etat.inventaire().possede("recette_geto"), "rendu a Geto");
        assertTrue(etat.argent() >= argent + 24, "il paie la course");
    }

    @Test
    void leMecanoParleDeLaRS6EtGeroNEnDitRien() {
        String mecano = texteDe("omar_paie");
        assertTrue(mecano.contains("RS6") && mecano.contains("boîtier") && mecano.contains("électronique"),
                "le mecano d'Omar parle de la voiture, de son electronique et de son boitier");
        String gero = texteDe("gero_marche");
        assertFalse(gero.contains("Omar"), "Gero ne sait pas qui transporte pour Geto");
        assertTrue(gero.contains("j'ai déjà une idée de la voiture que je vais prendre"), "le heros a deja son plan");
        assertFalse(gero.toLowerCase().contains("je te conseille"), "Gero n'est pas un mentor");
    }

    @Test
    void leSouvenirDeSoulSemeSansReveler() {
        String fb = texteDe("retour");
        assertTrue(fb.contains("Nordine") && fb.contains("Krousty"), "l'ami disparu, et la Krousty");
        assertTrue(fb.contains("prévisible") || fb.contains("Il le savait"), "Soul trouve ca logique");
        assertFalse(fb.toLowerCase().contains("je travaille pour"), "Soul ne dit rien de lui");
        assertTrue(fb.contains("la veille"), "il savait avant : un indice, relu plus tard");
    }

    @Test
    void lIntroductionNeMontrePasLeMurDuPlan() {
        histoire.Mission intro = robot.jeu.res.campagne.mission("0.1");
        assertTrue(intro.facultatifs().stream().noneMatch(o -> o.cible().equals("plan")), "pas de mur du plan au premier soir");
        assertNotNull(robot.jeu.res.campagne.cinematique("fin_acte1"), "il prend forme a la fin de l'acte 1");
    }

    @Test
    void uneSceneFaitDeLaPlaceAutourDuSdf() {
        partie.progression().restaurer(robot.jeu.res.campagne.rang("1.4"), 1, Set.of(), true, Set.of("parti_de_chez_geto"));
        partie.monde().aller("avenue_bas", 300, 220);
        Monde.Habitant sdf = partie.monde().habitant("sdf");
        assertNotNull(sdf);
        assertTrue(partie.monde().habitant("diallo") == null, "Tata Lili n'a rien a faire la ce soir");
        Monde.Habitant passante = partie.monde().habitant("passant_bas");
        passante.acteur.x = sdf.acteur.x + 20;                          // une passante tout contre le SDF
        partie.monde().joueur.x = sdf.acteur.x - 40;
        partie.jouerScene(robot.jeu.res.campagne.cinematique("sdf_appel"));
        robot.images(90);
        assertTrue(Math.abs(passante.acteur.x - sdf.acteur.x) > 60, "elle s'ecarte pour laisser la scene : "
                + Math.abs(passante.acteur.x - sdf.acteur.x));
        assertTrue(Math.abs(sdf.acteur.x - partie.monde().heros().x) < 120, "le SDF, lui, reste a sa place");
    }
}
