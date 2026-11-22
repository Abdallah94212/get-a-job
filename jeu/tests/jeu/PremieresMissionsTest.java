package jeu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import decor.Porte;
import histoire.Etat;
import histoire.Objectif;
import histoire.Progression;
import histoire.Sauvegarde;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Le debut de l'acte 1, joue au clavier par le robot comme un vrai joueur : les livraisons, le scooter rendu, la paie
 * chez Geto, puis le remboursement obligatoire de Youngssef AVANT le metro (une vraie transaction : 12 euros sortent du
 * compte, une seule fois, et cela tient a la sauvegarde). Puis les deux facons d'aller au metro : en scooter (parcours A,
 * on ressort a la station a cote du scooter, qui n'a pas bouge) ou a pied (parcours B, on ressort au Maitre Poulet).
 */
class PremieresMissionsTest {
    @TempDir
    Path dossier;
    private Robot robot;
    private Path fichier;

    @BeforeEach
    void commencer() throws Exception {
        fichier = dossier.resolve("partie.properties");
        robot = new Robot(fichier, null);
        robot.jeu.nouvellePartie();
        robot.images(3);
        // on joue l'intro et la mission 1.1 a pied, jusqu'au debut de la 1.2 : rien n'est triche
        Progression p = progression();
        for (int tour = 0; tour < 200 && !p.courante().id().equals("1.2"); tour++) {
            robot.laisserPasser();
            if (!p.courante().id().equals("1.2")) {
                assertTrue(robot.marcherVersObjectif(), "bloque en " + p.courante().id() + " : " + p.objectif().texte());
            }
        }
        robot.laisserPasser();
        assertEquals("1.2", p.courante().id());
        assertEquals(0, p.etape(), "la 1.2 commence chez Geto");
        assertFalse(p.drapeau("scooter") || p.drapeau("scooter_gare"), "le scooter a ete rendu a Youngssef en fin de 1.1");
    }

    private Progression progression() {
        return robot.partie().progression();
    }

    private Etat etat() {
        return progression().etat();
    }

    private Monde monde() {
        return robot.partie().monde();
    }

    /** Va a pied (ou en scooter) jusqu'a ce personnage, dans la carte ou l'on est, et appuie sur E. */
    private void parlerA(String id) {
        float[] ou = null;
        for (Monde.Habitant h : monde().habitants()) {
            if (h.figurant.id().equals(id)) {
                ou = new float[] {h.acteur.x, h.acteur.y};
            }
        }
        assertNotNull(ou, id + " doit etre la");
        float[] cible = ou;
        assertTrue(robot.marcher((x, y) -> Math.abs(x - cible[0]) <= Monde.PORTEE_X - 6 && Math.abs(y - cible[1]) <= Monde.PORTEE_Y - 6,
                cible[0], 0), "on arrive pres de " + id);
        robot.frapper(KeyEvent.VK_E);
    }

    /** Va jusqu'a cette carte au clavier, porte apres porte, comme un joueur (sans regarder l'objectif). */
    private void allerA(String but) {
        for (int i = 0; i < 6 && !monde().carte().equals(but); i++) {
            Porte porte = robot.porteVers(but);
            assertNotNull(porte, "un chemin vers " + but + " depuis " + monde().carte());
            String ici = monde().carte();
            assertTrue(robot.marcher((x, y) -> porte.devant(x, y), (porte.x1() + porte.x2()) / 2f, porte.automatique() ? 0 : 4),
                    "on arrive devant la porte vers " + porte.cible());
            if (monde().carte().equals(ici)) {
                if (porte.automatique()) {
                    int vers = (porte.x1() + porte.x2()) / 2f > monde().joueur.x ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT;
                    robot.touches.presser(vers);
                    for (int k = 0; k < 40 && monde().carte().equals(ici); k++) {
                        robot.image();
                    }
                    robot.touches.relacher(vers);
                } else {
                    robot.frapper(KeyEvent.VK_E);
                }
            }
        }
        assertEquals(but, monde().carte());
    }

    /** Fait l'objectif en cours au clavier : porte apres porte, puis E devant la cible. */
    private void faireAPied(String quoi) {
        int etape = progression().etape();
        String mission = progression().courante().id();
        for (int i = 0; i < 8 && progression().etape() == etape && progression().courante().id().equals(mission); i++) {
            robot.laisserPasser();
            assertTrue(robot.marcherVersObjectif(), quoi + " : " + progression().objectif().texte());
        }
        robot.laisserPasser();
    }

    /** Geto paie, puis il faut aller rembourser Youngssef : l'objectif l'exige, avec la somme. */
    private void seFairePayerChezGeto() {
        int avant = etat().argent();
        faireAPied("Geto paie les livraisons");
        assertEquals(avant + 24, etat().argent(), "Geto a paye les 24 euros des deux livraisons");
        assertTrue(progression().drapeau("part_youngssef"), "la part de Youngssef est due");
        assertTrue(etat().inventaire().possede("enveloppe"), "Geto a deja confie la course : l'enveloppe pour Bidoche");
        assertEquals(1, progression().etape());
        assertEquals("AVANT LE MÉTRO : RENDS SES 12 EUROS À YOUNGSSEF, AU BANC", progression().objectif().texte());
        assertEquals(12, progression().objectif().argent());
    }

    private void rembourserYoungssef() {
        int avant = etat().argent();
        faireAPied("on rembourse Youngssef au banc");
        assertEquals(avant - 12, etat().argent(), "les 12 euros sont vraiment sortis du compte");
        assertEquals(1, partsNotees(), "la banque note la transaction, une fois");
        assertFalse(progression().drapeau("part_youngssef"), "plus rien n'est du");
        assertTrue(progression().drapeau("youngssef_paye"));
        assertEquals(2, progression().etape(), "on peut enfin partir au metro");
        assertEquals(Objectif.Genre.ALLER, progression().objectif().genre());
        assertEquals("metro", progression().objectif().cible());
    }

    private long partsNotees() {
        return etat().journal().stream().filter(l -> l.libelle().equals("LA PART DE YOUNGSSEF")).count();
    }

    /** Joue la suite de la 1.2 (la rame, Bidoche, les caisses, le retour) jusqu'a « rapporte la viande a Old Geto ». */
    private void jouerJusquAuRetour() {
        Progression p = progression();
        for (int tour = 0; tour < 60 && !p.objectif().texte().contains("RAPPORTE LA VIANDE"); tour++) {
            robot.laisserPasser();
            if (!p.objectif().texte().contains("RAPPORTE LA VIANDE")) {
                assertTrue(robot.avancerVersObjectif(), "bloque : " + p.objectif().texte());
            }
        }
        robot.laisserPasser();
        assertTrue(p.objectif().texte().contains("RAPPORTE LA VIANDE"), p.objectif().texte());
        assertFalse(robot.partie().enScene(), "la scene du retour est finie");
        assertTrue(etat().inventaire().possede("caisses_poulet"), "on rapporte bien les caisses");
    }

    /** Le scooter de Youngssef montre au banc (rendu), ou absent du banc parce qu'il est gare ailleurs. */
    private boolean scooterAuBanc() {
        return monde().carte().equals("avenue_bas") && monde().habitants().stream().anyMatch(h -> h.figurant.id().equals("scooter_y"));
    }

    @Test
    void lesObjectifsDeLaCourseSontDansLeBonOrdre() {
        List<String> textes = progression().courante().etapes().stream().map(Objectif::texte).toList();
        assertEquals("VA TE FAIRE PAYER AU MAÎTRE POULET", textes.get(0));
        assertEquals("AVANT LE MÉTRO : RENDS SES 12 EUROS À YOUNGSSEF, AU BANC", textes.get(1));
        assertEquals("REMONTE L'AVENUE JUSQU'AU MÉTRO, TOUT EN HAUT", textes.get(2));
        assertTrue(progression().courante().facultatifs().stream().noneMatch(o -> o.cible().equals("youngssef")),
                "rembourser n'est plus une option qu'on peut oublier");
    }

    @Test
    void onNePartPasAuMetroSansAvoirRembourseYoungssef() {
        seFairePayerChezGeto();
        allerA("metro");                                               // on file au metro sans passer par le banc
        robot.laisserPasser();
        assertEquals(1, progression().etape(), "entrer dans le metro ne fait pas partir la rame");
        assertFalse(robot.partie().enScene(), "pas de scene de depart");
        assertTrue(progression().drapeau("part_youngssef"));
    }

    @Test
    void sansLesDouzeEurosOnNePeutPasRembourserNiAvancer() throws IOException {
        seFairePayerChezGeto();
        int argent = etat().argent();
        assertTrue(etat().payer(argent - 5), "on a tout depense sauf 5 euros");
        allerA("avenue_bas");
        parlerA("youngssef");
        robot.images(2);
        assertEquals("IL TE MANQUE 7 EUROS POUR LE REMBOURSER.", robot.partie().refusAffiche());
        robot.laisserPasser();
        assertEquals(5, etat().argent(), "rien n'est sorti du compte");
        assertEquals(1, progression().etape(), "l'objectif attend toujours");
        assertTrue(progression().drapeau("part_youngssef"));
        assertEquals(0, partsNotees());
        etat().gagner(20, "ESSAI");
        rembourserYoungssef();
    }

    @Test
    void onNeRembourseQuUneFoisEtCelaTientALaSauvegarde() throws IOException {
        seFairePayerChezGeto();
        rembourserYoungssef();
        int apres = etat().argent();
        // on lui reparle : il ne reclame plus rien, il propose seulement son scooter (le robot le loue : 5 euros)
        parlerA("youngssef");
        robot.laisserPasser();
        assertEquals(apres - 5, etat().argent(), "seule la location est payee : pas de deuxieme remboursement");
        assertEquals(1, partsNotees(), "une seule fois « la part de Youngssef » dans la banque");
        assertEquals(2, progression().etape());
        apres = etat().argent();
        Sauvegarde.Partie relue = new Sauvegarde(fichier).charger(robot.jeu.res.campagne);
        assertEquals("1.2", relue.progression().courante().id());
        assertEquals(2, relue.progression().etape(), "on reprend au depart pour le metro, pas au remboursement");
        assertTrue(relue.progression().drapeau("youngssef_paye"));
        assertFalse(relue.progression().drapeau("part_youngssef"));
        assertEquals(apres, relue.progression().etat().argent());
    }

    @Test
    void parcoursA_enScooterOnRessortALaStationAcoteDuScooter() throws IOException {
        seFairePayerChezGeto();
        rembourserYoungssef();
        parlerA("youngssef");                                          // « Je le prends » : 5 euros, trois minutes
        int avantLocation = etat().argent();
        robot.laisserPasser();
        assertEquals(avantLocation - 5, etat().argent(), "la location est payee");
        assertTrue(monde().enScooter(), "on roule");
        allerA("metro");                                               // jusqu'a la bouche de metro, en scooter
        assertTrue(progression().drapeau("metro_en_scooter"), "le jeu note qu'on est venu en scooter");
        assertTrue(progression().drapeau("scooter_gare"));
        assertEquals("avenue_haut", etat().scooterCarte(), "garé devant la station");
        float gareX = etat().scooterX();
        float gareY = etat().scooterY();
        assertTrue(Math.abs(gareX - 502) < 60, "juste devant la bouche de metro : " + gareX);
        jouerJusquAuRetour();
        assertEquals("avenue_haut", monde().carte(), "on ressort a la station");
        assertTrue(Math.abs(monde().joueur.x - gareX) <= 24 && Math.abs(monde().joueur.y - gareY) <= 24,
                "a cote du scooter, a la sortie du metro : " + monde().joueur.x + "," + monde().joueur.y + " / " + gareX + "," + gareY);
        assertTrue(monde().scene().porteEn(monde().joueur.x, monde().joueur.y) == null, "pas pose sur la porte du metro");
        assertEquals("avenue_haut", etat().scooterCarte(), "le scooter n'a pas ete deplace");
        assertEquals(gareX, etat().scooterX(), 0.01f);
        assertEquals(gareY, etat().scooterY(), 0.01f);
        assertNotNull(monde().objet("scooter_gare"), "on le voit, la ou on l'a laisse");
        long scooters = monde().habitants().stream().filter(h -> h.figurant.id().startsWith("scooter")).count();
        assertEquals(0, scooters, "pas de deuxieme scooter dans la rue");
        // la sauvegarde garde tout : la carte, le scooter, le drapeau
        Sauvegarde.Partie relue = new Sauvegarde(fichier).charger(robot.jeu.res.campagne);
        assertEquals("avenue_haut", relue.carte());
        assertEquals("avenue_haut", relue.progression().etat().scooterCarte());
        assertEquals(gareX, relue.progression().etat().scooterX(), 0.01f);
        assertTrue(relue.progression().drapeau("scooter_gare"));
        // on remonte dessus et on repart
        assertNotNull(monde().cible());
        assertEquals("scooter_gare", monde().cible().id(), "E propose de remonter sur le scooter");
        robot.frapper(KeyEvent.VK_E);
        robot.laisserPasser();
        assertTrue(monde().enScooter(), "on repart avec");
        assertFalse(progression().drapeau("scooter_gare"));
        faireAPied("on rapporte la viande");
        assertTrue(progression().objectif().texte().contains("RANGE LES CAISSES"), progression().objectif().texte());
        robot.laisserPasser();
    }

    @Test
    void parcoursB_aPiedOnRessortAuMaitrePoulet() throws IOException {
        seFairePayerChezGeto();
        rembourserYoungssef();
        assertTrue(scooterAuBanc(), "le scooter reste au banc : on ne le prend pas");
        allerA("metro");                                               // a pied
        assertFalse(progression().drapeau("metro_en_scooter"), "le jeu note qu'on est venu a pied");
        assertFalse(progression().drapeau("scooter") || progression().drapeau("scooter_gare"));
        jouerJusquAuRetour();
        assertEquals("avenue_bas", monde().carte(), "on redescend jusqu'au Maitre Poulet, comme avant");
        float poulet = robot.jeu.res.abscisse("avenue_bas", "@poulet+50");
        assertTrue(Math.abs(monde().joueur.x - poulet) < 4 && Math.abs(monde().joueur.y - 200) < 4,
                "devant le Maitre Poulet : " + monde().joueur.x + "," + monde().joueur.y);
        assertTrue(scooterAuBanc(), "le scooter n'a pas bouge du banc");
        assertNull(monde().objet("scooter_gare"), "aucun scooter gare ailleurs");
        Sauvegarde.Partie relue = new Sauvegarde(fichier).charger(robot.jeu.res.campagne);
        assertEquals("avenue_bas", relue.carte());
        assertFalse(relue.progression().drapeau("metro_en_scooter"));
        faireAPied("on rapporte la viande a Geto");
        assertTrue(progression().objectif().texte().contains("RANGE LES CAISSES"), progression().objectif().texte());
        robot.laisserPasser();
    }
}
