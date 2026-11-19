package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Etat;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le combat hybride, dans les seize situations qui comptent : enchainer, frapper du pied, balayer, attaquer en l'air,
 * esquiver, se battre a plusieurs ; A qui tire de loin et frappe de pres, avec une marge entre les deux ; changer de
 * cible ; un obstacle qui arrete les balles ; ramasser une arme ; tirer, recharger, vider un chargeur, la rafale ;
 * changer d'arme en plein combat ; tout retrouver apres un chargement. Et les nouveaux ennemis : le tireur vise puis tire,
 * la brute charge (on saute par-dessus), un ennemi arme lache son arme en tombant, la relique ne se duplique pas.
 */
class CombatArsenalTest {
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
        partie.progression().marquer("+vu_combat +vu_armes +vu_armes_feu +vu_armes_lourdes +vu_katana_relique +vu_ennemis_armes"
                + " +vu_astuce_touches +vu_astuce_garde +vu_astuce_entoure +vu_astuce_lancer +vu_astuce_fuite +vu_astuce_contre"
                + " +vu_astuce_tir +vu_astuce_tireur +vu_astuce_charge +vu_astuce_air");
        partie.monde().aller("cour", 300, 236);
        partie.monde().joueur.versDroite = true;
        etat = partie.progression().etat();
        etat.soigner(100);
    }

    private Combat combat() {
        return partie.combat();
    }

    /** Un ennemi pose la, qui ne bouge plus et n'attaque pas (il "recule" pour longtemps). */
    private Combat.Ennemi poser(Combat.Ennemi e, float x, float y) {
        e.acteur.x = x;
        e.acteur.y = y;
        e.etat = Combat.Phase.RECULE;
        e.minuteur = 50;
        e.repos = 50;
        e.vx = 0;
        return e;
    }

    private Combat.Ennemi un(Combat.Genre genre, float x) {
        combat().commencer(genre, 1, 300, 236, null, null, partie.monde().scene());
        return poser(combat().ennemis().get(0), x, 236);
    }

    private void tenir(int code, int images) {
        robot.touches.presser(code);
        robot.images(images);
        robot.touches.relacher(code);
        robot.image();
    }

    // ------------------------------------------------------------------ 1. 2. les poings, les pieds, la balayette

    @Test
    void aEnchaineDesCoupsDifferents() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 322);
        e.pv = 999;
        Set<String> vus = new LinkedHashSet<>();
        for (int i = 0; i < 90; i++) {
            if (i % 5 == 0) {
                robot.touches.presser(KeyEvent.VK_A);
            }
            robot.image();
            robot.touches.relacher(KeyEvent.VK_A);
            if (combat().geste() != null) {
                vus.add(combat().geste().nom());
            }
            poser(e, 322, 236);
            e.pv = 999;
        }
        List<String> l = new ArrayList<>(vus);
        assertTrue(l.size() >= 4, "un vrai enchainement, des coups differents : " + l);
        assertEquals("poing", l.get(0));
        assertEquals("direct", l.get(1));
        assertTrue(l.contains("circulaire"), "l'enchainement finit par le circulaire : " + l);
    }

    @Test
    void leCirculaireEnvoieVolerEtLaBalayetteCouche() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 322);
        e.pv = 999;
        boolean envole = false;
        for (int i = 0; i < 90 && !envole; i++) {
            if (i % 5 == 0) {
                robot.touches.presser(KeyEvent.VK_A);
            }
            robot.image();
            robot.touches.relacher(KeyEvent.VK_A);
            if (e.etat == Combat.Phase.VOLE) {
                envole = true;
                break;
            }
            if (e.etat != Combat.Phase.VOLE) {
                poser(e, 322, 236);
            }
        }
        assertTrue(envole, "le dernier coup le projette");
        robot.images(60);
        assertTrue(e.auSol() || e.etat == Combat.Phase.APPROCHE, "il retombe, et reste au sol un moment : " + e.etat);
        Combat.Ennemi f = un(Combat.GUETTEUR, 280);
        robot.frapper(KeyEvent.VK_L);
        robot.images(12);
        assertEquals(Combat.Phase.SOL, f.etat, "la balayette fait tomber, meme derriere");
    }

    @Test
    void leCoupDePiedPorteEtRepousse() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 330);
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_K);
        robot.images(20);
        assertEquals(pv - Combat.PIED.degats(), e.pv, 0.01f);
        assertTrue(e.acteur.x > 336, "il recule sous le coup");
    }

    // ------------------------------------------------------------------ 3. 4. le saut

    @Test
    void sauterPuisAttaquerEnLAir() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 340);
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_SPACE);
        robot.images(3);
        assertTrue(partie.monde().enLAir());
        robot.frapper(KeyEvent.VK_A);
        assertSame(Combat.PIED_SAUTE, combat().geste(), "en l'air, A est un coup de pied saute");
        for (int i = 0; i < 20 && e.pv == pv; i++) {
            robot.image();
        }
        assertTrue(e.pv < pv, "il touche");
        assertTrue(e.etat == Combat.Phase.VOLE || e.auSol(), "le coup saute projette : " + e.etat);
        robot.images(40);
        assertFalse(partie.monde().enLAir(), "il retombe");
        assertNull(combat().geste(), "et se remet en garde");
    }

    @Test
    void lAttaquePlongeanteCoucheToutAutourALaReception() {
        combat().commencer(Combat.GUETTEUR, 2, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi a = poser(combat().ennemis().get(0), 330, 236);
        Combat.Ennemi b = poser(combat().ennemis().get(1), 278, 236);
        robot.frapper(KeyEvent.VK_SPACE);
        robot.images(8);
        robot.touches.presser(KeyEvent.VK_DOWN);
        robot.frapper(KeyEvent.VK_A);
        robot.touches.relacher(KeyEvent.VK_DOWN);
        assertSame(Combat.PLONGEON, combat().geste());
        robot.images(30);
        assertTrue(a.auSol() && b.auSol(), "l'onde couche ceux de devant et de derriere : " + a.etat + " " + b.etat);
    }

    @Test
    void apresLaReceptionAEnchaineAuSol() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 322);
        e.pv = 999;
        robot.frapper(KeyEvent.VK_SPACE);
        for (int i = 0; i < 60 && (partie.monde().enLAir() || partie.monde().hauteurDuSaut() > 0); i++) {
            robot.image();
        }
        robot.frapper(KeyEvent.VK_A);
        assertSame(Combat.DIRECT, combat().geste(), "juste apres la reception, A reprend l'enchainement au deuxieme coup");
    }

    @Test
    void sauterEsquiveLeCoupEtLeTireurPeutViserHaut() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 318);
        e.etat = Combat.Phase.PREPARE;
        e.minuteur = 0.12f;
        e.coupsRestants = 0;
        float sante = etat.sante();
        robot.frapper(KeyEvent.VK_SPACE);
        robot.images(8);
        assertEquals(sante, etat.sante(), 0.01f, "le coup est passe sous ses pieds");
        robot.images(40);
        e.etat = Combat.Phase.PREPARE;                                  // cette fois, il l'a vu sauter : il vise haut
        e.minuteur = 0.2f;
        e.haut = true;
        e.acteur.x = partie.monde().joueur.x + 18;
        robot.frapper(KeyEvent.VK_SPACE);
        robot.images(8);
        assertTrue(etat.sante() < sante, "un coup vise haut le cueille en l'air");
    }

    @Test
    void lEsquiveRendIntouchableEtDeplace() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 318);
        e.etat = Combat.Phase.PREPARE;
        e.minuteur = 0.1f;
        e.coupsRestants = 0;
        float x = partie.monde().joueur.x;
        float sante = etat.sante();
        robot.frapper(KeyEvent.VK_C);
        robot.images(6);
        assertEquals(sante, etat.sante(), 0.01f, "l'esquive passe sous le coup");
        assertTrue(partie.monde().joueur.x < x - 20, "loin de lui, en arriere : " + (partie.monde().joueur.x - x));
    }

    @Test
    void laGardeEncaisseEtLeContreRiposte() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 322);
        e.etat = Combat.Phase.PREPARE;
        e.minuteur = 0.5f;                                              // trop tot pour un contre : X tient la garde
        e.coupsRestants = 0;
        float sante = etat.sante();
        robot.touches.presser(KeyEvent.VK_X);
        robot.images(20);
        robot.touches.relacher(KeyEvent.VK_X);
        robot.image();
        float perte = sante - etat.sante();
        assertTrue(perte > 0 && perte < Combat.GUETTEUR.degats() * 0.3f, "la garde prend l'essentiel : " + perte);
        Combat.Ennemi f = un(Combat.GUETTEUR, 322);
        f.etat = Combat.Phase.PREPARE;
        f.minuteur = 0.25f;
        f.coupsRestants = 0;
        sante = etat.sante();
        robot.frapper(KeyEvent.VK_X);
        assertSame(Combat.RIPOSTE, combat().geste(), "au signe blanc, X contre");
        assertTrue(f.etat == Combat.Phase.VACILLE || f.etat == Combat.Phase.SOL || f.etat == Combat.Phase.RECULE, "il vacille : " + f.etat);
        robot.images(20);
        assertEquals(sante, etat.sante(), 0.01f, "le coup contre ne touche pas");
        assertTrue(f.pv < Combat.GUETTEUR.pv(), "la riposte porte");
    }

    // ------------------------------------------------------------------ 5. a plusieurs

    @Test
    void contreLaBandeUnSeulTireurAlaFoisEtDeuxAuPlusAuContact() {
        combat().commencer(Combat.groupe("visite", 4), 300, 236, null, null, partie.monde().scene());
        int tireurs = 0;
        int contact = 0;
        boolean aVise = false;
        for (int i = 0; i < 30 * 10; i++) {
            robot.image();
            etat.soigner(100);
            long t = combat().ennemis().stream().filter(e -> e.etat == Combat.Phase.VISE || e.etat == Combat.Phase.TIRE).count();
            long c = combat().ennemis().stream().filter(e -> !e.colere && (e.etat == Combat.Phase.PREPARE || e.etat == Combat.Phase.FRAPPE
                    || e.etat == Combat.Phase.CHARGE)).count();
            tireurs = (int) Math.max(tireurs, t);
            contact = (int) Math.max(contact, c);
            aVise |= t > 0;
        }
        assertTrue(aVise, "le tireur finit par viser");
        assertEquals(1, tireurs, "un seul tireur fait feu a la fois");
        assertTrue(contact <= 2, "pas plus de deux attaques au contact : " + contact);
    }

    // ------------------------------------------------------------------ 6. 7. 8. A : tir de loin, coup de pres

    @Test
    void aTireDeLoinEtFrappeDePres() {
        etat.prendreArme("pistolet");
        Combat.Ennemi e = un(Combat.GUETTEUR, 420);
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_A);
        assertEquals(7, etat.chargees(), "une balle est partie");
        assertNull(combat().geste(), "un tir, pas un coup");
        assertTrue(e.pv < pv, "elle touche, a cent vingt pixels");
        robot.images(30);
        poser(e, 322, 236);
        robot.images(2);
        robot.frapper(KeyEvent.VK_A);
        assertNotNull(combat().geste(), "de pres : un coup");
        assertEquals("poing", combat().geste().nom());
        assertEquals(7, etat.chargees(), "et aucune balle");
    }

    @Test
    void laFrontiereEntreTirEtCorpsACorpsAUneMarge() {
        etat.prendreArme("pistolet");
        Combat.Ennemi e = un(Combat.GUETTEUR, 330);
        robot.images(2);
        assertTrue(combat().auCorps(), "a trente pixels : corps a corps");
        poser(e, 350, 236);
        robot.images(2);
        assertTrue(combat().auCorps(), "a cinquante : on reste au corps a corps (la marge)");
        poser(e, 366, 236);
        robot.images(2);
        assertFalse(combat().auCorps(), "a soixante-six : on repasse au tir");
        poser(e, 350, 236);
        robot.images(2);
        assertFalse(combat().auCorps(), "a cinquante, en venant du tir : on reste au tir");
        poser(e, 332, 236);
        robot.images(2);
        assertTrue(combat().auCorps(), "a trente-deux : corps a corps");
    }

    @Test
    void sansArmeAFeuAElanceVersLaCible() {
        Combat.Ennemi e = un(Combat.GUETTEUR, 380);
        float x = partie.monde().joueur.x;
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_A);
        robot.images(25);
        assertTrue(partie.monde().joueur.x > x + 40, "il s'est jete vers lui");
        assertTrue(e.pv < pv, "et a frappe en arrivant");
    }

    // ------------------------------------------------------------------ 9. la cible

    @Test
    void laDirectionTenueChoisitLaCible() {
        combat().commencer(Combat.GUETTEUR, 2, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi devant = poser(combat().ennemis().get(0), 328, 236);
        Combat.Ennemi derriere = poser(combat().ennemis().get(1), 262, 236);
        robot.images(2);
        assertSame(devant, combat().choisirCible(partie.monde(), 0, 0, 105, false), "sans direction : celui de devant");
        assertSame(derriere, combat().choisirCible(partie.monde(), -1, 0, 105, false), "la gauche tenue : celui de gauche, plus loin");
        robot.touches.presser(KeyEvent.VK_LEFT);
        robot.frapper(KeyEvent.VK_A);
        robot.touches.relacher(KeyEvent.VK_LEFT);
        assertSame(derriere, combat().cible(), "A vise celui qu'on designe");
        assertFalse(partie.monde().joueur.versDroite, "il se tourne vers lui");
    }

    // ------------------------------------------------------------------ 10. les murs arretent les balles

    @Test
    void unObstacleArreteLesBalles() {
        partie.monde().aller("appartement", 30, 250);
        partie.monde().joueur.versDroite = true;
        etat.prendreArme("pistolet");
        combat().commencer(Combat.GUETTEUR, 1, 30, 250, null, null, partie.monde().scene());
        Combat.Ennemi e = poser(combat().ennemis().get(0), 175, 250);
        assertFalse(Combat.ligneLibre(partie.monde().scene(), 30, 250, 175, 250), "le meuble est entre eux");
        assertNull(combat().choisirCible(partie.monde(), 0, 0, 230, true), "on ne vise pas a travers");
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_A);
        assertEquals(7, etat.chargees(), "la balle part quand meme, devant");
        assertEquals(pv, e.pv, 0.01f, "et s'arrete dans le meuble");
        assertTrue(combat().traces().stream().allMatch(t -> t.x1() < 152), "la trace s'arrete au meuble");
    }

    @Test
    void lesBallesDuTireurSArretentAussiAuMur() {
        partie.monde().aller("appartement", 30, 250);
        combat().commencer(Combat.TIREUR, 1, 30, 250, null, null, partie.monde().scene());
        Combat.Ennemi e = combat().ennemis().get(0);
        e.acteur.x = 175;
        e.acteur.y = 250;
        float sante = etat.sante();
        for (int i = 0; i < 30 * 4; i++) {
            e.acteur.x = 175;
            e.acteur.y = 250;
            partie.monde().joueur.x = 30;
            partie.monde().joueur.y = 250;
            robot.image();
        }
        assertEquals(sante, etat.sante(), 0.01f, "pas de ligne de tir : il ne touche pas");
    }

    // ------------------------------------------------------------------ 11. ramasser une arme de contact

    @Test
    void ramasserLaBatteEtEnchainerAvec() {
        etat.auSol().add(new Etat.AuSol("batte", 0, "cour", 308, 236));
        partie.monde().joueur.x = 300;
        robot.images(2);
        robot.frapper(KeyEvent.VK_E);
        assertEquals("batte", etat.armeEnMain().id(), "E la ramasse, elle passe en main");
        Combat.Ennemi e = un(Combat.GUETTEUR, 326);
        e.pv = 999;
        Set<String> vus = new LinkedHashSet<>();
        for (int i = 0; i < 40; i++) {
            if (i % 5 == 0) {
                robot.touches.presser(KeyEvent.VK_A);
            }
            robot.image();
            robot.touches.relacher(KeyEvent.VK_A);
            if (combat().geste() != null) {
                vus.add(combat().geste().nom());
            }
            if (e.etat != Combat.Phase.VOLE) {
                poser(e, 326, 236);
            }
            e.pv = 999;
        }
        assertTrue(vus.contains("batte_1") && vus.contains("batte_2_haut"), "de cote, puis par-dessus : " + vus);
    }

    @Test
    void leKatanaSouleveEtLaReliqueResteUnique() {
        etat.prendreArme("katana");
        Combat.Ennemi e = un(Combat.GUETTEUR, 326);
        e.pv = 999;
        boolean souleve = false;
        for (int i = 0; i < 60 && !souleve; i++) {
            if (i % 4 == 0) {
                robot.touches.presser(KeyEvent.VK_A);
            }
            robot.image();
            robot.touches.relacher(KeyEvent.VK_A);
            souleve = e.etat == Combat.Phase.VOLE;
            if (!souleve) {
                poser(e, 326, 236);
            }
        }
        assertTrue(souleve, "la troisieme coupe le souleve");
        etat.prendreArme("katana_relique");
        assertNull(etat.prendreArme("katana_relique"), "rien ne se passe la deuxieme fois");
        assertEquals(1, etat.inventaire().quantite("katana_relique"), "une seule relique");
        robot.frapper(KeyEvent.VK_H);
        assertEquals("katana_relique", etat.armeEnMain().id(), "elle ne se lance pas");
    }

    // ------------------------------------------------------------------ 12. 13. 14. tirer, recharger, rafale, vide

    @Test
    void tirerAuPistoletPuisRecharger() {
        etat.prendreArme("pistolet");
        etat.inventaire().ajouter("balles_9", 16);
        un(Combat.GUETTEUR, 450).pv = 9999;
        for (int i = 0; i < 8; i++) {
            robot.frapper(KeyEvent.VK_A);
            robot.images(10);
        }
        assertEquals(0, etat.chargees(), "huit balles, huit tirs");
        robot.frapper(KeyEvent.VK_A);
        assertTrue(combat().chargeurVide(), "le clic a vide");
        assertTrue(combat().rechargement() >= 0, "et il recharge de lui-meme");
        robot.images(45);
        assertEquals(8, etat.chargees(), "un chargeur neuf");
        assertEquals(8, etat.reserve(), "pris dans la poche");
        robot.frapper(KeyEvent.VK_A);
        robot.images(10);
        robot.frapper(KeyEvent.VK_R);
        robot.images(45);
        assertEquals(8, etat.chargees(), "R complete le chargeur");
        assertEquals(7, etat.reserve());
    }

    @Test
    void laMitrailletteTireTantQueAResteEnfonceEtSEcarte() {
        etat.prendreArme("mitraillette");
        Combat.Ennemi e = un(Combat.GUETTEUR, 440);
        e.pv = 9999;
        tenir(KeyEvent.VK_A, 30);
        int tires = 25 - etat.chargees();
        assertTrue(tires >= 8 && tires <= 12, "une rafale d'une seconde : une dizaine de balles, pas plus : " + tires);
        assertTrue(e.pv < 9999, "elle touche");
    }

    @Test
    void leFusilPorteLoinEtFrappeFort() {
        etat.prendreArme("fusil");
        Combat.Ennemi e = un(Combat.GUETTEUR, 560);
        float pv = e.pv;
        robot.frapper(KeyEvent.VK_A);
        assertEquals(29, etat.chargees());
        assertTrue(pv - e.pv >= 12, "a deux cent soixante pixels, il touche encore fort : " + (pv - e.pv));
        tenir(KeyEvent.VK_A, 20);
        assertTrue(etat.chargees() < 27, "et il tire en continu");
    }

    @Test
    void plusDeBallesPlusDeTir() {
        etat.prendreArme("pistolet");
        Combat.Ennemi e = un(Combat.GUETTEUR, 450);
        e.pv = 9999;
        for (int i = 0; i < 12; i++) {
            robot.frapper(KeyEvent.VK_A);
            robot.images(10);
        }
        assertEquals(0, etat.chargees());
        assertEquals(0, etat.reserve());
        robot.frapper(KeyEvent.VK_A);
        assertTrue(combat().chargeurVide(), "il le sait : clic");
        assertTrue(combat().rechargement() < 0, "rien a recharger");
        robot.frapper(KeyEvent.VK_R);
        assertTrue(combat().rechargement() < 0);
    }

    // ------------------------------------------------------------------ 15. changer d'arme en combat

    @Test
    void vPasseDUneArmeALAutreEnPleinCombat() {
        etat.prendreArme("katana");
        etat.prendreArme("pistolet");
        etat.inventaire().equiper(null);
        Combat.Ennemi e = un(Combat.GUETTEUR, 450);
        e.pv = 9999;
        robot.frapper(KeyEvent.VK_V);
        assertEquals("katana", etat.armeEnMain().id());
        robot.frapper(KeyEvent.VK_V);
        assertEquals("pistolet", etat.armeEnMain().id());
        robot.frapper(KeyEvent.VK_A);
        assertEquals(8, etat.chargees(), "l'arme sort a peine : le tir attend un instant");
        robot.images(6);
        assertEquals(7, etat.chargees(), "et part tout seul, sans rappuyer : l'appui n'est pas perdu");
        robot.frapper(KeyEvent.VK_V);
        assertNull(etat.armeEnMain(), "les poings");
    }

    // ------------------------------------------------------------------ 16. la sauvegarde

    @Test
    void lesArmesEtLesMunitionsSurviventAUnChargement() throws Exception {
        etat.prendreArme("fusil");
        etat.prendreArme("pistolet");
        etat.inventaire().ajouter("balles_9", 16);
        un(Combat.GUETTEUR, 450).pv = 9999;
        for (int i = 0; i < 3; i++) {
            robot.frapper(KeyEvent.VK_A);
            robot.images(10);
        }
        combat().oublier();
        partie.sauvegarder();
        robot.jeu.ouvrirMenu();
        robot.images(2);
        robot.jeu.continuer();
        Etat relu = robot.partie().progression().etat();
        assertEquals("pistolet", relu.armeEnMain().id());
        assertEquals(5, relu.chargees(), "le chargeur entame");
        assertEquals(16, relu.reserve());
        histoire.Inventaire.Ligne fusil = relu.inventaire().lignes().stream().filter(l -> l.id.equals("fusil")).findFirst().orElseThrow();
        assertEquals(30, fusil.usure, "le fusil, chargeur plein");
    }

    // ------------------------------------------------------------------ les ennemis

    @Test
    void leTireurViseAvantDeTirerEtLEsquiveLEvite() {
        combat().commencer(Combat.TIREUR, 1, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi e = combat().ennemis().get(0);
        e.acteur.x = 420;
        e.acteur.y = 236;
        e.repos = 0;
        boolean vise = false;
        float sante = etat.sante();
        for (int i = 0; i < 60 && e.etat != Combat.Phase.VISE; i++) {
            partie.monde().joueur.x = 300;
            robot.image();
        }
        vise = e.etat == Combat.Phase.VISE;
        assertTrue(vise, "il vise d'abord : on le voit venir");
        assertEquals(sante, etat.sante(), 0.01f, "rien tant qu'il vise");
        while (e.etat == Combat.Phase.VISE && e.minuteur > 0.08f) {
            robot.image();
        }
        robot.frapper(KeyEvent.VK_C);                                   // au dernier moment
        robot.images(8);
        assertEquals(sante, etat.sante(), 0.01f, "l'esquive au bon moment : la balle passe");
    }

    @Test
    void laBruteChargeEtPasseSousLeSaut() {
        combat().commencer(Combat.BRUTE, 1, 300, 236, null, null, partie.monde().scene());
        Combat.Ennemi e = combat().ennemis().get(0);
        e.acteur.x = 400;
        e.acteur.y = 236;
        e.etat = Combat.Phase.PREPARE;
        e.charge = true;
        e.minuteur = 0.05f;
        float sante = etat.sante();
        robot.images(4);
        assertEquals(Combat.Phase.CHARGE, e.etat, "elle charge");
        robot.frapper(KeyEvent.VK_SPACE);
        for (int i = 0; i < 30 && e.etat == Combat.Phase.CHARGE; i++) {
            robot.image();
        }
        assertEquals(sante, etat.sante(), 0.01f, "elle passe dessous");
        assertTrue(e.acteur.x < 300, "et continue sa course");
        assertEquals(Combat.Phase.VACILLE, e.etat, "essoufflee : c'est le moment de frapper");
    }

    @Test
    void laBruteNeBronchePasPourUnPetitCoup() {
        Combat.Ennemi e = un(Combat.BRUTE, 322);
        e.etat = Combat.Phase.APPROCHE;
        e.repos = 50;
        robot.frapper(KeyEvent.VK_J);
        robot.images(4);
        assertTrue(e.pv < Combat.BRUTE.pv(), "touchee");
        assertFalse(e.etat == Combat.Phase.RECULE || e.auSol(), "mais elle avance encore : " + e.etat);
    }

    @Test
    void unEnnemiArmeLacheSonArmeEnTombant() {
        Combat.Ennemi e = un(Combat.SABREUR, 322);
        e.pv = 1;
        int avant = etat.auSol().size();
        robot.frapper(KeyEvent.VK_K);
        robot.images(20);
        assertFalse(e.vivant());
        assertEquals(avant + 1, etat.auSol().size(), "son sabre reste par terre");
        assertEquals("katana", etat.auSol().get(etat.auSol().size() - 1).arme());
    }
}
