package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Etat;
import histoire.Inventaire;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'acte 1 joue pour de vrai : Youngssef ne se presente qu'au moment ou on lui parle ; Geto recoit son appel (ca
 * sonne, il decroche, la voix au bout du fil, il raccroche, puis revient au heros) ; ses sacs passent dans
 * l'inventaire, se voient, se livrent, ne se livrent pas sans eux ; l'inventaire empile, protege et se sauvegarde.
 */
class ActeUnTest {
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

    /** Va devant ce personnage, tourne vers lui, appuie sur E. */
    private void parlerA(String id) {
        Monde.Habitant h = partie.monde().habitant(id);
        partie.monde().joueur.x = h.acteur.x - 20;
        partie.monde().joueur.y = h.acteur.y + 4;
        partie.monde().joueur.versDroite = true;
        robot.images(2);
        robot.frapper(KeyEvent.VK_E);
    }

    private void jusquA(String mission, int etape, Set<String> drapeaux) {
        partie.progression().restaurer(robot.jeu.res.campagne.rang(mission), etape, Set.of(), true, drapeaux);
    }

    @Test
    void youngssefNeSePresenteQuEnLeRencontrant() {
        assertTrue(robot.avancerVersObjectif(), "on descend dans la rue");    // 0.1 : sortir de chez soi
        boolean ficheTropTot = false;
        for (int i = 0; i < 4000 && (partie.enScene() || partie.auBilan() || partie.auCalendrier() || partie.encartOuvert()); i++) {
            ficheTropTot |= "presente_youngssef".equals(partie.encartCourant());
            robot.frapper(KeyEvent.VK_E);
        }
        assertFalse(ficheTropTot, "il appelle de loin en sortant : pas encore de fiche");
        assertEquals("1.1", partie.progression().courante().id());
        assertFalse(partie.progression().drapeau("vu_presente_youngssef"));
        partie.monde().aller("avenue_bas", 600, 220);
        robot.images(30);
        assertFalse(partie.encartOuvert(), "le voir dans la rue ne suffit pas");
        parlerA("youngssef");
        assertTrue(partie.enScene());
        robot.images(4);
        assertEquals("presente_youngssef", partie.encartCourant(), "E devant lui : sa fiche, avant sa premiere replique");
        robot.laisserPasser();
        assertTrue(partie.progression().drapeau("vu_presente_youngssef"));
    }

    @Test
    void getoRecoitUnAppelQuOnDistingueDeLaConversation() {
        jusquA("1.2", 0, Set.of("pret_scooter"));
        partie.monde().aller("maitre_poulet", 120, 236);
        parlerA("geto");
        List<String> etats = new ArrayList<>();
        List<String> pendantAppel = new ArrayList<>();
        int etat = 0;
        for (int i = 0; i < 3000 && partie.enScene(); i++) {
            String appel = partie.scenographe().appel();
            if (etats.isEmpty() || !java.util.Objects.equals(etats.get(etats.size() - 1), appel)) {
                etats.add(appel);
            }
            String pos = partie.positionScene();
            if (appel != null && appel.endsWith("ligne")) {
                pendantAppel.add(pos);
                Monde.Habitant geto = partie.monde().habitant("geto");
                assertEquals(geto.acteur.x < partie.monde().heros().x ? -1 : 1, geto.sens, "en ligne, il se detourne du heros");
            }
            if (partie.encartOuvert() || partie.scenographeAuChoix()) {
                robot.images(20);
            } else {
                robot.images(etat++ % 2 == 0 ? 25 : 2);
            }
            robot.frapper(KeyEvent.VK_E);
        }
        assertTrue(etats.contains("geto:sonne"), "son telephone sonne : " + etats);
        assertTrue(etats.contains("geto:ligne"), "il decroche : " + etats);
        assertNull(etats.get(etats.size() - 1), "il raccroche");
        assertTrue(etats.indexOf("geto:sonne") < etats.indexOf("geto:ligne"));
        List<String> sons = robot.jeu.audio.journal();
        assertTrue(sons.contains("tel_sonnerie") && sons.contains("tel_decroche") && sons.contains("tel_raccroche"),
                "sonnerie, decroche, raccroche");
        assertTrue(partie.progression().etat().inventaire().possede("enveloppe"), "l'enveloppe de Bidoche, en poche");
    }

    @Test
    void lesSacsDeGetoSeDonnentSePortentEtSeLivrent() throws Exception {
        jusquA("1.1", 2, Set.of("scooter", "pret_scooter", "vu_presente_geto"));
        Inventaire inv = partie.progression().etat().inventaire();
        inv.retirer("sac_lili", inv.quantite("sac_lili"));
        inv.retirer("sac_omar", inv.quantite("sac_omar"));
        partie.monde().aller("maitre_poulet", 120, 236);
        parlerA("geto");
        boolean vuEnLAir = false;
        for (int i = 0; i < 2000 && partie.enScene(); i++) {
            vuEnLAir |= partie.positionScene().contains(":DONNE");
            robot.images(3);
            robot.frapper(KeyEvent.VK_E);
        }
        assertTrue(vuEnLAir, "la remise se joue : Geto tend le sac");
        assertEquals(1, inv.quantite("sac_lili"));
        assertEquals(1, inv.quantite("sac_omar"));
        // ils restent apres un changement de lieu et une sauvegarde
        partie.monde().aller("avenue_bas", 400, 220);
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(2);
        robot.jeu.continuer();
        partie = robot.partie();
        inv = partie.progression().etat().inventaire();
        assertEquals(1, inv.quantite("sac_lili"), "toujours en poche apres la sauvegarde");
        // sans le sac, Tata Lili n'est pas livree
        assertTrue(inv.retirer("sac_lili", 1));
        int etape = partie.progression().etape();
        parlerA("diallo");
        assertEquals(etape, partie.progression().etape(), "pas de livraison sans le sac");
        assertFalse(partie.enScene());
        inv.ajouter("sac_lili", 1);
        parlerA("diallo");
        robot.laisserPasser();
        assertEquals(etape + 1, partie.progression().etape(), "livree");
        assertFalse(inv.possede("sac_lili"), "le sac est parti chez elle");
        assertEquals(1, inv.quantite("sac_omar"), "celui d'Omar, toujours la");
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(2);
        robot.jeu.continuer();
        assertFalse(robot.partie().progression().etat().inventaire().possede("sac_lili"), "un sac livre ne revient pas");
    }

    @Test
    void uneVieillePartieRetrouveLesSacsDejaRemis() {
        Etat etat = partie.progression().etat();
        etat.inventaire().retirer("sac_lili", etat.inventaire().quantite("sac_lili"));
        jusquA("1.1", 3, Set.of("scooter"));                           // Geto a deja donne les sacs, avant que l'inventaire existe
        assertEquals(1, etat.inventaire().quantite("sac_lili"));
        assertEquals(1, etat.inventaire().quantite("sac_omar"));
        jusquA("1.1", 3, Set.of("scooter"));
        assertEquals(1, etat.inventaire().quantite("sac_lili"), "jamais en double");
        jusquA("1.3", 3, Set.of("sachets"));                           // le premier client : trois sachets
        assertEquals(3, etat.inventaire().quantite("sachets"));
        jusquA("2.3", 0, Set.of("beeper", "boitier_orange"));
        assertTrue(etat.inventaire().possede("beeper") && etat.inventaire().possede("boitier_orange"));
    }

    @Test
    void lInventaireEmpileProtegeEtEquipe() {
        Etat etat = partie.progression().etat();
        Inventaire inv = etat.inventaire();
        inv.ajouter("sachets", 3);
        assertEquals(1, inv.lignes(Inventaire.Categorie.LIVRAISONS).stream().filter(l -> l.id.equals("sachets")).count(), "une pile");
        assertTrue(inv.retirer("sachets", 1));
        assertEquals(2, inv.quantite("sachets"));
        assertFalse(inv.retirer("sachets", 5), "on ne livre pas ce qu'on n'a pas");
        assertEquals(2, inv.quantite("sachets"));
        partie.progression().marquer("+beeper");
        partie.progression().marquer("+beeper");
        inv.ajouter("beeper", 1);
        assertEquals(1, inv.quantite("beeper"), "un objet unique ne se duplique pas");
        etat.prendreArme("tuyau");
        etat.prendreArme("planche");
        etat.prendreArme("bouteille");
        etat.prendreArme("batte");
        assertEquals(4, inv.armes(), "quatre armes dans le sac");
        assertEquals("batte", etat.armeEnMain().id(), "la derniere en main");
        Etat.Arme laissee = etat.prendreArme("barre");
        assertEquals("batte", laissee.id(), "le sac est plein : celle qu'on tenait reste");
        assertEquals(4, inv.armes());
        // dans les poches : E range l'arme, G la pose, un objet de l'histoire ne se pose pas
        partie.monde().aller("cour", 300, 236);
        robot.frapper(KeyEvent.VK_I);
        assertTrue(partie.auxPoches());
        robot.frapper(KeyEvent.VK_TAB);                                // livraisons -> armes
        while (!partie.progression().etat().inventaire().lignes(Inventaire.Categorie.ARMES).isEmpty()
                && etat.armeEnMain() != null && !etat.armeEnMain().id().equals("tuyau")) {
            robot.frapper(KeyEvent.VK_RIGHT);
            robot.frapper(KeyEvent.VK_E);
        }
        assertEquals("tuyau", etat.armeEnMain().id(), "E prend une arme en main");
        robot.frapper(KeyEvent.VK_E);
        assertNull(etat.armeEnMain(), "E la range");
        int sol = etat.auSol().size();
        robot.frapper(KeyEvent.VK_G);
        assertEquals(sol + 1, etat.auSol().size(), "G la pose par terre");
        assertEquals(3, inv.armes());
        robot.frapper(KeyEvent.VK_TAB);                                // armes -> objets : le beeper
        robot.frapper(KeyEvent.VK_G);
        assertEquals(1, inv.quantite("beeper"), "un objet de l'histoire ne se jette pas");
        robot.frapper(KeyEvent.VK_I);
        assertFalse(partie.auxPoches());
        assertNotEquals(0, inv.lignes().size());
    }
}
