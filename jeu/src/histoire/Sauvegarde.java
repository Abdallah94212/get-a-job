package histoire;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** Ecrit et relit la partie dans un petit fichier texte : la mission, l'objectif, et l'endroit ou se tient le joueur. */
public final class Sauvegarde {
    /** Une partie relue : la progression, et l'endroit ou reposer le joueur. */
    public record Partie(Progression progression, String carte, float x, float y) {
    }

    private final Path fichier;

    public Sauvegarde(Path fichier) {
        this.fichier = fichier;
    }

    /** Le fichier de sauvegarde habituel, dans le dossier personnel du joueur. */
    public static Sauvegarde parDefaut() {
        return new Sauvegarde(Path.of(System.getProperty("user.home"), ".get-a-job", "partie.properties"));
    }

    public boolean existe() {
        return Files.isRegularFile(fichier);
    }

    public void effacer() throws IOException {
        Files.deleteIfExists(fichier);
    }

    public void enregistrer(Progression progression, String carte, float x, float y) throws IOException {
        Properties p = new Properties();
        Mission mission = progression.courante();
        p.setProperty("mission", mission == null ? "" : mission.id());
        p.setProperty("etape", Integer.toString(progression.etape()));
        p.setProperty("facultatifs", String.join(",", progression.facultatifsFaits()));
        p.setProperty("debut", Boolean.toString(progression.debutJoue()));
        p.setProperty("drapeaux", String.join(",", progression.drapeaux()));
        p.setProperty("carte", carte);
        p.setProperty("x", Float.toString(x));
        p.setProperty("y", Float.toString(y));
        if (fichier.getParent() != null) {
            Files.createDirectories(fichier.getParent());
        }
        try (Writer sortie = Files.newBufferedWriter(fichier, StandardCharsets.UTF_8)) {
            p.store(sortie, "GET A JOB - partie en cours");
        }
    }

    /** Relit la partie. Leve une IOException si le fichier manque, est abime ou parle d'une mission qui n'existe plus. */
    public Partie charger(Campagne campagne) throws IOException {
        Properties p = new Properties();
        try (Reader entree = Files.newBufferedReader(fichier, StandardCharsets.UTF_8)) {
            p.load(entree);
        }
        try {
            String mission = p.getProperty("mission", "");
            int rang = mission.isEmpty() ? campagne.missions().size() : campagne.rang(mission);
            Set<String> facultatifs = new LinkedHashSet<>();
            for (String cible : p.getProperty("facultatifs", "").split(",")) {
                if (!cible.isBlank()) {
                    facultatifs.add(cible);
                }
            }
            Set<String> drapeaux = new LinkedHashSet<>();
            for (String nom : p.getProperty("drapeaux", "").split(",")) {
                if (!nom.isBlank()) {
                    drapeaux.add(nom);
                }
            }
            Progression progression = new Progression(campagne);
            progression.restaurer(rang, Integer.parseInt(p.getProperty("etape", "0")), facultatifs,
                    Boolean.parseBoolean(p.getProperty("debut", "false")), drapeaux);
            String carte = p.getProperty("carte");
            if (carte == null || carte.isBlank()) {
                throw new IOException("Sauvegarde sans carte");
            }
            return new Partie(progression, carte, Float.parseFloat(p.getProperty("x")), Float.parseFloat(p.getProperty("y")));
        } catch (IllegalArgumentException | NullPointerException erreur) {
            throw new IOException("Sauvegarde illisible : " + erreur.getMessage(), erreur);
        }
    }
}
