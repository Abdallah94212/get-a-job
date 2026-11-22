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

    /**
     * Une partie enregistree avant que le remboursement de Youngssef devienne un objectif de la 1.2 (format 1) : ses
     * etapes sont decalees d'un cran, sauf si l'on n'etait pas encore parti et qu'on ne l'avait pas paye.
     */
    @Test
    void unePartieDAvantLeRemboursementObligatoireReprendAuBonObjectif() throws IOException {
        Campagne vraie = LecteurCampagne.lire(Path.of("ressources", "histoire", "campagne.txt"));
        Path fichier = dossier.resolve("ancienne.properties");
        Sauvegarde ancienne = new Sauvegarde(fichier);
        String lieu = "carte=avenue_bas\nx=600\ny=210\n";

        Files.writeString(fichier, "mission=1.2\netape=1\ndrapeaux=part_youngssef\n" + lieu);
        Progression pasPaye = ancienne.charger(vraie).progression();
        assertEquals(1, pasPaye.etape(), "pas encore parti, pas paye : on doit d'abord rembourser");
        assertTrue(pasPaye.objectif().texte().contains("12 EUROS À YOUNGSSEF"));

        Files.writeString(fichier, "mission=1.2\netape=1\ndrapeaux=youngssef_paye\n" + lieu);
        assertEquals(2, ancienne.charger(vraie).progression().etape(), "deja paye (l'ancienne option) : direction le metro");

        Files.writeString(fichier, "mission=1.2\netape=6\ndrapeaux=youngssef_paye\n" + lieu);
        Progression loin = ancienne.charger(vraie).progression();
        assertEquals(7, loin.etape(), "plus loin dans la 1.2 : le meme objectif qu'avant");
        assertEquals("caisses", loin.objectif().cible());

        Files.writeString(fichier, "format=2\nmission=1.2\netape=1\ndrapeaux=part_youngssef\n" + lieu);
        assertEquals(1, ancienne.charger(vraie).progression().etape(), "une partie au nouveau format n'est pas touchee");
    }
}
