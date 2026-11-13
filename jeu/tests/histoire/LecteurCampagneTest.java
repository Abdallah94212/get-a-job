package histoire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** La lecture du fichier de campagne, et les garde-fous qui refusent une histoire qu'on ne pourrait pas finir. */
class LecteurCampagneTest {
    private static List<String> petiteAvec(String avant, String apres) {
        List<String> lignes = new ArrayList<>(ProgressionTest.PETITE);
        lignes.replaceAll(l -> l.equals(avant) ? apres : l);
        return lignes;
    }

    @Test
    void laPetiteCampagneEstLueEnEntier() throws IOException {
        Campagne campagne = LecteurCampagne.lire(ProgressionTest.PETITE);
        assertEquals(1, campagne.chapitres().size());
        assertEquals(2, campagne.missions().size());
        Mission m1 = campagne.mission("m1");
        assertEquals("PREMIÈRE", m1.titre());
        assertEquals("La rue", m1.lieu());
        assertEquals("On commence.", m1.description());
        assertEquals(30, m1.chrono());
        assertEquals(3, m1.etapes().size());
        assertEquals(1, m1.facultatifs().size());
        assertTrue(m1.etapes().get(1).sousPression());
        assertEquals(Objectif.Genre.PARLER, m1.etapes().get(0).genre());
        assertTrue(campagne.mission("m2").ouvre().contains("cave"));
        assertEquals("FERMÉ.", campagne.verrous().get("cave"));
        assertEquals("LA CAVE", campagne.nomDuLieu("cave"));
        assertEquals("rue", campagne.carteDepart());
        assertEquals("LE CHEF", campagne.voix("chef").nom());
        assertEquals("buste_chef", campagne.voix("chef").portrait());
        assertTrue(campagne.cinematique("bonjour").surPlace());
        assertEquals("Bonjour.", campagne.cinematique("bonjour").plans().get(0).texte());
    }

    @Test
    void lesChoixEtLesSautsSontLus() throws IOException {
        List<String> lignes = new ArrayList<>(ProgressionTest.PETITE);
        lignes.addAll(List.of("cinematique question", "dit chef | Alors ?", "choix | Oui. -> oui | Non. -> non",
                "etiquette oui", "drapeau +accepte", "saut fin", "etiquette non", "dit chef | Dommage.", "etiquette fin",
                "si +accepte merci", "etiquette merci"));
        Cinematique question = LecteurCampagne.lire(lignes).cinematique("question");
        Plan choix = question.plans().get(1);
        assertEquals(Plan.Genre.CHOIX, choix.genre());
        assertEquals(2, choix.options().size());
        assertEquals("Non.", choix.options().get(1).texte());
        assertEquals("non", choix.options().get(1).etiquette());
        assertEquals(5, question.etiquette("non"));
        assertTrue(question.sautsPerdus().isEmpty());
    }

    @Test
    void unSautVersUneEtiquetteAbsenteEstRefuse() {
        List<String> lignes = new ArrayList<>(ProgressionTest.PETITE);
        lignes.addAll(List.of("cinematique perdue", "choix | Oui. -> oui | Non. -> nulle_part", "etiquette oui"));
        IOException erreur = assertThrows(IOException.class, () -> LecteurCampagne.lire(lignes));
        assertTrue(erreur.getMessage().contains("nulle_part"), erreur.getMessage());
    }

    @Test
    void lesConditionsDesObjectifsEtDesPersonnagesSontLues() throws IOException {
        List<String> lignes = new ArrayList<>(ProgressionTest.PETITE);
        lignes.replaceAll(l -> l.equals("option agir livre | LIS LE LIVRE") ? "option agir livre si +lu -perdu musique:mus_rue | LIS LE LIVRE" : l);
        lignes.add(lignes.indexOf("figurant chef | rue porte 0 - | p_chef | LE CHEF | m1 fin") + 1, "condition +invite");
        Campagne campagne = LecteurCampagne.lire(lignes);
        Objectif livre = campagne.mission("m1").facultatifs().get(0);
        assertEquals("+lu -perdu", livre.condition());
        assertEquals("mus_rue", livre.musique());
        assertEquals("+invite", campagne.figurants().get("chef").condition());
    }

    @Test
    void uneLigneFausseEstSignaleeAvecSonNumero() {
        List<String> lignes = new ArrayList<>(ProgressionTest.PETITE);
        lignes.add("danse chef | Une instruction qui n'existe pas.");
        IOException erreur = assertThrows(IOException.class, () -> LecteurCampagne.lire(lignes));
        assertTrue(erreur.getMessage().contains("ligne " + lignes.size()), erreur.getMessage());
        assertThrows(IOException.class, () -> LecteurCampagne.lire(List.of("resume | Un résumé sans mission.")));
    }

    @Test
    void uneCibleQuiNExistePasRendLaCampagneInjouable() {
        IOException erreur = assertThrows(IOException.class, () -> LecteurCampagne.lire(
                petiteAvec("objectif agir coffre | OUVRE LE COFFRE", "objectif agir tresor | OUVRE LE TRÉSOR")));
        assertTrue(erreur.getMessage().contains("tresor"), erreur.getMessage());
    }

    @Test
    void unObjectifDansUnLieuJamaisOuvertEstRefuse() {
        IOException erreur = assertThrows(IOException.class, () -> LecteurCampagne.lire(petiteAvec("ouvre cave", "resume | Rien n'ouvre la cave.")));
        assertTrue(erreur.getMessage().contains("ferme"), erreur.getMessage());
    }

    @Test
    void uneCinematiqueIntrouvableEstRefusee() {
        IOException erreur = assertThrows(IOException.class, () -> LecteurCampagne.lire(petiteAvec("cinematique bonjour", "cinematique salut")));
        assertTrue(erreur.getMessage().contains("bonjour"), erreur.getMessage());
    }

    @Test
    void parlerAUnAbsentEstRefuse() {
        IOException erreur = assertThrows(IOException.class, () -> LecteurCampagne.lire(petiteAvec(
                "figurant chef | rue porte 0 - | p_chef | LE CHEF | m1 fin", "figurant chef | rue porte 0 - | p_chef | LE CHEF | m2 fin")));
        assertTrue(erreur.getMessage().contains("chef"), erreur.getMessage());
    }

    @Test
    void laVraieCampagneEstJouableSurLePapier() throws IOException {
        Campagne campagne = LecteurCampagne.lire(Path.of("ressources", "histoire", "campagne.txt"));
        assertTrue(campagne.defauts().isEmpty());
        assertEquals(3, campagne.chapitres().size());
        assertEquals(9, campagne.missions().size());
        for (Mission mission : campagne.missions()) {
            assertFalse(mission.titre().isBlank(), mission.id());
            assertFalse(mission.description().isBlank(), mission.id() + " n'a pas de description");
            assertFalse(mission.lieu().isBlank(), mission.id() + " n'a pas de zone");
            assertFalse(mission.personnages().isBlank(), mission.id() + " n'a pas de personnages");
            assertNotNull(campagne.chapitreDe(mission));
        }
        for (String lieu : List.of("metro", "garage", "namek", "labo", "epicerie")) {
            assertTrue(campagne.missions().stream().anyMatch(m -> m.ouvre().contains(lieu)), lieu + " ne s'ouvre jamais");
        }
        Progression debut = new Progression(campagne);
        assertFalse(debut.lieuOuvert("labo"), "le labo ne doit pas etre libre d'acces au debut");
        assertFalse(debut.lieuOuvert("hall_tour"));
    }
}
