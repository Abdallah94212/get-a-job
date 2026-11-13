package histoire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les regles de la progression : l'ordre des missions, les lieux qui s'ouvrent, les sequences qu'on recommence. */
class ProgressionTest {
    /** Une petite campagne : deux missions, un lieu ferme (la cave) que la seconde ouvre. */
    static final List<String> PETITE = List.of(
            "depart rue 10 200",
            "verrou cave | LA CAVE | FERMÉ.",
            "figurant chef | rue porte 0 - | p_chef | LE CHEF | m1 fin",
            "point sac | rue porte 0 - | - | E : PRENDRE",
            "point livre | rue porte 0 - | - | E : LIRE",
            "point coffre | cave fond 0 - | - | E : OUVRIR",
            "chapitre c1 | CHAPITRE 1 | LE DÉBUT | Accroche.",
            "mission m1 | PREMIÈRE | La rue | Le chef",
            "resume | On commence.",
            "chrono 30",
            "objectif parler chef -> bonjour | PARLE AU CHEF",
            "objectif agir sac pression | PRENDS LE SAC",
            "objectif agir sac pression | REPRENDS LE SAC",
            "option agir livre | LIS LE LIVRE",
            "mission m2 | SECONDE | La cave | Personne",
            "ouvre cave",
            "objectif aller cave | DESCENDS",
            "objectif agir coffre | OUVRE LE COFFRE",
            "cinematique bonjour",
            "dit chef | Bonjour.");

    private Campagne campagne;
    private Progression progression;

    @BeforeEach
    void preparer() throws IOException {
        campagne = LecteurCampagne.lire(PETITE);
        progression = new Progression(campagne);
    }

    @Test
    void auDebutSeuleLaPremiereMissionEstDisponible() {
        assertEquals(EtatMission.DISPONIBLE, progression.etat(campagne.mission("m1")));
        assertEquals(EtatMission.VERROUILLEE, progression.etat(campagne.mission("m2")));
        assertEquals("m1", progression.courante().id());
        assertEquals("PARLE AU CHEF", progression.objectif().texte());
    }

    @Test
    void uneCibleQueLHistoireNAttendPasNeFaitRienAvancer() {
        assertNull(progression.valider(Objectif.Genre.AGIR, "sac"));
        assertNull(progression.valider(Objectif.Genre.AGIR, "coffre"));
        assertNull(progression.valider(Objectif.Genre.ALLER, "cave"));
        assertEquals(0, progression.etape());
    }

    @Test
    void lesObjectifsPrincipauxSeFontDansLOrdre() {
        Progression.Avancee avancee = progression.valider(Objectif.Genre.PARLER, "chef");
        assertNotNull(avancee);
        assertEquals("bonjour", avancee.objectif().suite());
        assertFalse(avancee.missionTerminee());
        assertEquals(1, progression.etape());
    }

    @Test
    void unObjectifSecondaireSeFaitAToutMomentEtUneSeuleFois() {
        assertTrue(progression.attendue(Objectif.Genre.AGIR, "livre"));
        Progression.Avancee avancee = progression.valider(Objectif.Genre.AGIR, "livre");
        assertTrue(avancee.objectif().facultatif());
        assertEquals(0, progression.etape(), "un objectif secondaire ne fait pas avancer la mission");
        assertFalse(progression.attendue(Objectif.Genre.AGIR, "livre"));
        assertNull(progression.valider(Objectif.Genre.AGIR, "livre"));
    }

    @Test
    void finirUneMissionDebloqueLaSuivanteEtSonLieu() {
        assertFalse(progression.lieuOuvert("cave"), "la cave est fermee tant que la mission 2 n'a pas commence");
        assertTrue(progression.lieuOuvert("rue"), "un lieu sans verrou est toujours ouvert");
        progression.valider(Objectif.Genre.AGIR, "livre");
        progression.valider(Objectif.Genre.PARLER, "chef");
        progression.valider(Objectif.Genre.AGIR, "sac");
        Progression.Avancee fin = progression.valider(Objectif.Genre.AGIR, "sac");
        assertTrue(fin.missionTerminee());
        assertEquals("m2", fin.suivante().id());
        assertEquals(1, fin.facultatifsFaits());
        assertEquals(EtatMission.TERMINEE, progression.etat(campagne.mission("m1")));
        assertEquals(EtatMission.DISPONIBLE, progression.etat(campagne.mission("m2")));
        assertTrue(progression.lieuOuvert("cave"));
        assertFalse(progression.debutJoue(), "la nouvelle mission n'a pas encore ete presentee");
    }

    @Test
    void echouerRamenAuDebutDeLaSequenceSousPression() {
        progression.valider(Objectif.Genre.PARLER, "chef");
        assertTrue(progression.sousPression());
        progression.valider(Objectif.Genre.AGIR, "sac");
        assertEquals(2, progression.etape());
        progression.echouer();
        assertEquals(1, progression.etape(), "on reprend au premier objectif chronometre, pas avant");
        progression.echouer();
        assertEquals(1, progression.etape());
    }

    @Test
    void laDerniereMissionTermineLaCampagne() {
        progression.restaurer(1, 1, Set.of(), true);
        Progression.Avancee fin = progression.valider(Objectif.Genre.AGIR, "coffre");
        assertTrue(fin.campagneTerminee());
        assertTrue(progression.terminee());
        assertNull(progression.objectif());
        assertEquals(EtatMission.TERMINEE, progression.etat(campagne.mission("m2")));
        assertNull(progression.valider(Objectif.Genre.AGIR, "coffre"));
    }

    @Test
    void unPersonnageNEstLaQuePendantSaPeriode() throws IOException {
        Figurant chef = campagne.figurants().get("chef");
        assertTrue(progression.present(chef));
        progression.restaurer(2, 0, Set.of(), false);
        assertTrue(progression.present(chef), "\"fin\" garde le personnage une fois l'histoire jouee");
    }

    @Test
    void lesDrapeauxRetiennentLesChoixEtConditionnentLaSuite() {
        progression.marquer("+lu -rien +invite");
        assertTrue(progression.drapeau("lu"));
        assertTrue(progression.verifie("+lu -perdu"));
        assertFalse(progression.verifie("+lu +perdu"));
        assertTrue(progression.verifie(""));
        progression.marquer("-lu");
        assertFalse(progression.drapeau("lu"));
    }

    @Test
    void unObjectifConditionneNAttendQueSesDrapeaux() throws IOException {
        List<String> lignes = new java.util.ArrayList<>(PETITE);
        lignes.replaceAll(l -> l.equals("option agir livre | LIS LE LIVRE") ? "option agir livre si +trouve | LIS LE LIVRE" : l);
        Progression p = new Progression(LecteurCampagne.lire(lignes));
        assertFalse(p.attendue(Objectif.Genre.AGIR, "livre"), "le livre n'est pas encore trouve");
        assertTrue(p.secondairesOuverts().isEmpty());
        p.marquer("+trouve");
        assertTrue(p.attendue(Objectif.Genre.AGIR, "livre"));
        assertEquals(1, p.secondairesOuverts().size());
    }

    @Test
    void unPersonnageConditionneApparaitEtDisparait() throws IOException {
        List<String> lignes = new java.util.ArrayList<>(PETITE);
        lignes.add(lignes.indexOf("figurant chef | rue porte 0 - | p_chef | LE CHEF | m1 fin") + 1, "condition -parti");
        Progression p = new Progression(LecteurCampagne.lire(lignes));
        Figurant chef = p.campagne().figurants().get("chef");
        assertTrue(p.present(chef));
        p.marquer("+parti");
        assertFalse(p.present(chef));
    }

    @Test
    void restaurerRefuseUnEtatQuiNExistePas() {
        assertThrows(IllegalArgumentException.class, () -> progression.restaurer(7, 0, Set.of(), false));
        assertThrows(IllegalArgumentException.class, () -> progression.restaurer(-1, 0, Set.of(), false));
        assertThrows(IllegalArgumentException.class, () -> progression.restaurer(0, 9, Set.of(), false));
    }

    @Test
    void leTexteSEcritLettreApresLettre() {
        TexteProgressif texte = new TexteProgressif("BONJOUR", 10f);
        assertEquals("", texte.visible());
        texte.avancer(0.35f);
        assertEquals("BON", texte.visible());
        assertFalse(texte.complet());
        texte.terminer();
        assertEquals("BONJOUR", texte.visible());
        assertTrue(texte.complet());
        texte.avancer(5f);
        assertEquals("BONJOUR", texte.visible());
    }
}
