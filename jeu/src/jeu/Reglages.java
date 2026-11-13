package jeu;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Les options du joueur : le son, l'affichage, la vitesse du texte. Gardees d'une partie a l'autre. */
public final class Reglages {
    static final String[] TAILLES = {"FENÊTRE X2", "FENÊTRE X3", "FENÊTRE X4", "PLEIN ÉCRAN"};
    static final String[] VITESSES = {"LENTE", "NORMALE", "RAPIDE"};
    private static final float[] LETTRES_PAR_SECONDE = {26f, 46f, 90f};

    int musique = 6;
    int ambiance = 7;
    int effets = 8;
    boolean trame = true;
    int taille;
    int vitesseTexte = 1;
    private final Path fichier;

    /** fichier peut valoir null : les reglages ne sont alors ni lus ni ecrits (essais). */
    public Reglages(Path fichier) {
        this.fichier = fichier;
        if (fichier == null || !Files.isRegularFile(fichier)) {
            return;
        }
        Properties p = new Properties();
        try (Reader entree = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            p.load(entree);
            musique = borne(Integer.parseInt(p.getProperty("musique", "6")), 0, 10);
            ambiance = borne(Integer.parseInt(p.getProperty("ambiance", "7")), 0, 10);
            effets = borne(Integer.parseInt(p.getProperty("effets", "8")), 0, 10);
            trame = Boolean.parseBoolean(p.getProperty("trame", "true"));
            taille = borne(Integer.parseInt(p.getProperty("taille", "0")), 0, TAILLES.length - 1);
            vitesseTexte = borne(Integer.parseInt(p.getProperty("texte", "1")), 0, VITESSES.length - 1);
        } catch (IOException | NumberFormatException erreur) {
            System.err.println("Reglages illisibles, valeurs par defaut : " + erreur.getMessage());
        }
    }

    public static Reglages parDefaut() {
        return new Reglages(Path.of(System.getProperty("user.home"), ".get-a-job", "reglages.properties"));
    }

    static int borne(int valeur, int min, int max) {
        return Math.max(min, Math.min(max, valeur));
    }

    float lettresParSeconde() {
        return LETTRES_PAR_SECONDE[vitesseTexte];
    }

    void enregistrer() {
        if (fichier == null) {
            return;
        }
        Properties p = new Properties();
        p.setProperty("musique", Integer.toString(musique));
        p.setProperty("ambiance", Integer.toString(ambiance));
        p.setProperty("effets", Integer.toString(effets));
        p.setProperty("trame", Boolean.toString(trame));
        p.setProperty("taille", Integer.toString(taille));
        p.setProperty("texte", Integer.toString(vitesseTexte));
        try {
            Files.createDirectories(fichier.getParent());
            try (Writer sortie = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
                p.store(sortie, "GET A JOB - reglages");
            }
        } catch (IOException erreur) {
            System.err.println("Reglages non enregistres : " + erreur.getMessage());
        }
    }
}
