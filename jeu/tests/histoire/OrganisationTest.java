package histoire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import histoire.Organisation.Bilan;
import histoire.Organisation.Issue;
import histoire.Organisation.Membre;
import histoire.Organisation.Paie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * L'organisation et le mois d'affaires : les petits, leurs jauges, ce que coute et rapporte chaque operation, et un
 * mois entier qui doit se relire euro par euro dans la banque.
 */
class OrganisationTest {
    private Progression progression;
    private Etat etat;
    private Organisation orga;

    @BeforeEach
    void preparer() throws IOException {
        progression = new Progression(LecteurCampagne.lire(List.of("depart rue 10 200", "chapitre c1 | C | T | A.", "mission m1 | M | Lieu | Personne",
                "objectif aller rue | VA")));
        progression.marquer("+organisation +recrue_youngssef +recrue_rayan +recrue_moussa +youngssef_paye +youngssef_promis");
        etat = progression.etat();
        orga = etat.organisation();
        orga.recruter(progression);
        etat.gagner(600, "DÉPART");
    }

    @Test
    void youngssefEstLeMeilleurSurLaRoute() {
        Membre y = orga.membre("youngssef");
        Organisation.Operation route = Organisation.operation("livraisons");
        for (Membre m : orga.membres()) {
            if (m != y) {
                assertTrue(orga.chance(route, y, Paie.JUSTE) > orga.chance(route, m, Paie.JUSTE), "personne ne roule mieux que lui");
                assertTrue(y.fiabilite > m.fiabilite);
            }
        }
        assertEquals(3, orga.membres().size());
        Organisation.Operation namek = Organisation.operation("namek");
        assertTrue(orga.chance(namek, y, Paie.PRIME) > orga.chance(namek, y, Paie.RADIN), "la paie compte aussi");
    }

    @Test
    void lancerPaieUneSeuleFoisEtLaRecetteEntreALaFin() {
        int avant = etat.argent();
        assertNull(orga.lancer("livraisons", "youngssef", Paie.JUSTE, etat));
        int paye = 20 + Organisation.paie(Organisation.operation("livraisons"), Paie.JUSTE);
        assertEquals(avant - paye, etat.argent(), "les frais et la paie sortent au depart");
        assertFalse(orga.membre("youngssef").libre());
        assertNotNull(orga.lancer("livraisons", "rayan", Paie.JUSTE, etat), "une operation ne part pas deux fois");
        assertNotNull(orga.lancer("comptoir", "youngssef", Paie.JUSTE, etat), "un petit occupe ne part pas ailleurs");
        orga.avancer(30, etat);
        assertEquals(avant - paye, etat.argent(), "rien ne bouge pendant l'operation");
        List<Bilan> bilans = avancerJusquAuBout();
        assertEquals(1, bilans.size());
        Bilan b = bilans.get(0);
        assertEquals(avant - paye + b.recette() - b.labo(), etat.argent(), "a la fin, seule la recette entre : rien n'est paye deux fois");
        assertEquals(b.recette() - b.labo() - b.frais() - b.paie(), b.net());
        assertTrue(orga.membre("youngssef").libre());
    }

    @Test
    void onNePeutPasLancerSansLArgentNiLaConfiance() {
        etat.payer(etat.argent(), "TOUT");
        assertTrue(orga.lancer("livraisons", "youngssef", Paie.JUSTE, etat).startsWith("IL TE MANQUE"));
        assertTrue(orga.enCours().isEmpty());
        etat.gagner(2000, "RENFORT");
        assertTrue(orga.lancer("namek", "moussa", Paie.JUSTE, etat).contains("CONFIANCE"), "Ticket ne te suit pas encore sur Namek");
        orga.membre("moussa").confiance = 20;
        assertTrue(orga.lancer("ardoise", "moussa", Paie.RADIN, etat).contains("REFUSE"), "trop mal paye, il refuse");
        assertEquals(2000, etat.argent(), "un refus ne coute rien");
    }

    @Test
    void lesOperationsReussissentRatentOuRapportentMoins() {
        EnumSet<Issue> vues = EnumSet.noneOf(Issue.class);
        etat.gagner(20000, "RÉSERVE");
        for (int i = 0; i < 80 && vues.size() < 3; i++) {
            orga.lancer("ardoise", "rayan", Paie.JUSTE, etat);           // hors de son talent : il rate parfois
            orga.membre("rayan").confiance = 50;
            for (Bilan b : avancerJusquAuBout()) {
                vues.add(b.issue());
                switch (b.issue()) {
                    case REUSSIE -> assertTrue(b.recette() >= 110 * 0.6f);
                    case MOINS -> assertTrue(b.recette() > 0 && b.recette() < 110 * 0.5f, "une vente decevante : " + b.recette());
                    case RATEE -> {
                        assertEquals(0, b.recette());
                        assertTrue(b.net() < 0, "un echec coute la paie");
                    }
                }
            }
        }
        assertEquals(EnumSet.allOf(Issue.class), vues);
    }

    @Test
    void lesJaugesBougentAvecLaPaieEtLeResultat() {
        etat.gagner(20000, "RÉSERVE");
        Membre t = orga.membre("moussa");
        int confiance = t.confiance;
        orga.lancer("ardoise", "moussa", Paie.PRIME, etat);
        Bilan b = avancerJusquAuBout().get(0);
        assertTrue(t.confiance > confiance || b.issue() == Issue.RATEE, "une prime fait monter sa confiance");
        Membre s = orga.membre("rayan");
        int avant = s.confiance;
        for (int i = 0; i < 4; i++) {
            orga.lancer("comptoir", "rayan", Paie.RADIN, etat);
            avancerJusquAuBout();
        }
        assertTrue(s.confiance <= avant - 4 * 6 + 1, "payer radin use la confiance : " + avant + " -> " + s.confiance);
        Membre y = orga.membre("youngssef");
        int fiable = y.fiabilite;
        for (int i = 0; i < 12; i++) {
            orga.lancer("livraisons", "youngssef", Paie.JUSTE, etat);
            avancerJusquAuBout();
        }
        assertTrue(y.fiabilite > fiable, "l'experience le rend plus fiable");
        assertTrue(y.fiabilite <= 92, "jusqu'a un plafond");
    }

    @Test
    void namekCoutePuisRembourseAvecLaCuveDeGero() {
        progression.marquer("+gero_cadence");
        etat.gagner(1000, "RÉSERVE");
        assertTrue(orga.disponibles(progression).stream().anyMatch(o -> o.id().equals("namek")));
        int avant = etat.argent();
        assertNull(orga.lancer("namek", "youngssef", Paie.JUSTE, etat));
        assertEquals(avant - 150 - 200, etat.argent(), "l'essence, le peage, les plants et la paie, d'avance");
        Bilan b = avancerJusquAuBout().get(0);
        if (b.recette() > 0) {
            assertEquals(Math.min(900, b.recette()), b.labo(), "Gero prend sa cuve sur la vente");
            assertTrue(etat.journal().stream().anyMatch(l -> l.libelle().startsWith("LABO DE GERO") && l.montant() == -b.labo()));
        }
        assertEquals(avant - 350 + b.recette() - b.labo(), etat.argent());
    }

    @Test
    void lesOperationsEnCoursSurviventALaSauvegarde(@TempDir Path dossier) throws IOException {
        orga.lancer("livraisons", "youngssef", Paie.PRIME, etat);
        orga.lancer("comptoir", "rayan", Paie.JUSTE, etat);
        orga.avancer(40, etat);
        Sauvegarde sauvegarde = new Sauvegarde(dossier.resolve("partie.properties"));
        sauvegarde.enregistrer(progression, "rue", 10, 200);
        Etat relu = sauvegarde.charger(progression.campagne()).progression().etat();
        Organisation o = relu.organisation();
        assertEquals(2, o.enCours().size());
        assertEquals(etat.argent(), relu.argent());
        assertEquals("livraisons", o.membre("youngssef").occupation, "il est toujours sur la route");
        assertEquals(orga.membre("moussa").confiance, o.membre("moussa").confiance);
        assertEquals(orga.enCours().get(0).reste, o.enCours().get(0).reste, 0.01f);
        assertEquals(orga.totalPaies(), o.totalPaies());
        int argent = relu.argent();
        List<Bilan> fin = o.avancer(2000, relu);
        fin.addAll(o.avancer(2000, relu));
        assertEquals(2, fin.size(), "elles se terminent apres la relecture, sans etre perdues");
        assertEquals(argent + fin.stream().mapToInt(b -> b.recette() - b.labo()).sum(), relu.argent(), "et sans etre payees une deuxieme fois");
    }

    @Test
    void unMoisDAffairesSeRelitEuroParEuro() {
        progression.marquer("+gero_cadence");
        for (int i = 0; i < 6; i++) {                                   // Ticket gagne la confiance qu'il faut (comme dans le jeu)
            orga.lancer("ardoise", "moussa", Paie.PRIME, etat);
            avancerJusquAuBout();
        }
        etat.gagner(320, "LA CAISSE DU PREMIER JOUR");
        etat.finDeMission();                                            // un mois de loyer en retard, chez soi
        assertEquals(1, etat.loyerDu());
        Mois mois = new Mois(progression);
        int depart = mois.depart();
        int somme = 0;
        int messages = 0;
        Set<String> libelles = new java.util.HashSet<>();
        List<Integer> soldes = new ArrayList<>();
        while (!mois.fini()) {
            Mois.Jour j = mois.jourSuivant();
            for (Mois.Evenement e : j.evenements()) {
                somme += e.montant();
                messages += e.montant() == 0 ? 1 : 0;
                libelles.add(e.texte().split(" ")[0]);
            }
            soldes.add(j.solde());
        }
        int[] r = mois.resume();
        assertEquals(0, etat.loyerDu(), "le loyer de l'appartement est rattrape pendant le mois");
        assertEquals(etat.argent() - depart, somme, "chaque euro du mois est une ligne de la banque");
        assertEquals(r[4], r[0] - r[1] - r[2] - r[3], "recettes - paies - labo - frais = ce que le compte a gagne");
        assertTrue(etat.argent() > 7000 && etat.argent() < 14000, "un mois d'affaires : autour de 10 000 euros, pas plus : " + etat.argent());
        assertTrue(r[2] >= 900, "Gero a ete paye au moins une fois");
        assertTrue(libelles.containsAll(Set.of("LOYER", "PAIE", "VENTE", "LABO", "FRAIS")), "loyers, paies, ventes, cuves, frais : " + libelles);
        assertTrue(messages > 10, "les petits ecrivent pendant le mois");
        assertTrue(soldes.stream().anyMatch(s -> soldes.indexOf(s) > 0 && s < soldes.get(soldes.indexOf(s) - 1)), "le solde descend aussi");
        assertTrue(orga.bilans().stream().anyMatch(b -> b.issue() != Issue.REUSSIE), "tout ne reussit pas");
    }

    /** Fait passer le temps jusqu'a ce qu'il n'y ait plus rien en cours (retards compris). */
    private List<Bilan> avancerJusquAuBout() {
        List<Bilan> tous = new ArrayList<>();
        for (int i = 0; i < 20 && !orga.enCours().isEmpty(); i++) {
            tous.addAll(orga.avancer(200, etat));
        }
        return tous;
    }
}
