package histoire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** La sauvegarde : ce qu'on ecrit doit se relire a l'identique, et un fichier abime ne doit pas faire planter le jeu. */
class SauvegardeTest {
    @TempDir
    Path dossier;
    private Campagne campagne;
    private Sauvegarde sauvegarde;

    @BeforeEach
    void preparer() throws IOException {
        campagne = LecteurCampagne.lire(ProgressionTest.PETITE);
        sauvegarde = new Sauvegarde(dossier.resolve("sous-dossier").resolve("partie.properties"));
    }

    @Test
    void unePartieEcriteSeRelitALIdentique() throws IOException {
        Progression progression = new Progression(campagne);
        progression.marquerDebutJoue();
        progression.valider(Objectif.Genre.AGIR, "livre");
        progression.valider(Objectif.Genre.PARLER, "chef");
        progression.marquer("+omar_paye +scooter");
        assertFalse(sauvegarde.existe());
        sauvegarde.enregistrer(progression, "rue", 123.5f, 210f);
        assertTrue(sauvegarde.existe());

        Sauvegarde.Partie relue = sauvegarde.charger(campagne);
        assertEquals("m1", relue.progression().courante().id());
        assertEquals(1, relue.progression().etape());
        assertTrue(relue.progression().debutJoue());
        assertTrue(relue.progression().facultatifsFaits().contains("livre"));
        assertTrue(relue.progression().drapeau("omar_paye"), "les choix faits en dialogue survivent a la sauvegarde");
        assertTrue(relue.progression().drapeau("scooter"));
        assertEquals("rue", relue.carte());
        assertEquals(123.5f, relue.x());
        assertEquals(210f, relue.y());
    }

    @Test
    void uneCampagneTermineeSeSauvegardeAussi() throws IOException {
        Progression progression = new Progression(campagne);
        progression.restaurer(2, 0, java.util.Set.of(), false);
        sauvegarde.enregistrer(progression, "cave", 1f, 2f);
        assertTrue(sauvegarde.charger(campagne).progression().terminee());
    }

    @Test
    void effacerSupprimeLaPartie() throws IOException {
        sauvegarde.enregistrer(new Progression(campagne), "rue", 0f, 0f);
        sauvegarde.effacer();
        assertFalse(sauvegarde.existe());
        sauvegarde.effacer();
    }

    @Test
    void unFichierAbsentOuAbimeEstSignaleSansPlanter() throws IOException {
        assertThrows(IOException.class, () -> sauvegarde.charger(campagne));
        Path fichier = dossier.resolve("abime.properties");
        Sauvegarde abimee = new Sauvegarde(fichier);
        Files.writeString(fichier, "mission=m1\netape=pas-un-nombre\ncarte=rue\nx=1\ny=2\n");
        assertThrows(IOException.class, () -> abimee.charger(campagne));
        Files.writeString(fichier, "mission=mission-disparue\netape=0\ncarte=rue\nx=1\ny=2\n");
        assertThrows(IOException.class, () -> abimee.charger(campagne));
        Files.writeString(fichier, "mission=m1\netape=0\nx=1\ny=2\n");
        assertThrows(IOException.class, () -> abimee.charger(campagne));
        Files.writeString(fichier, "mission=m1\netape=0\ncarte=rue\n");
        assertThrows(IOException.class, () -> abimee.charger(campagne));
    }
}
