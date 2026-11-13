package jeu;

import decor.Police;
import decor.Scene;
import decor.Texture;
import histoire.Campagne;
import histoire.LecteurCampagne;
import histoire.Lieu;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Tout ce que le jeu lit sur le disque : les cartes, les images des personnages, les polices et la campagne. */
public final class Ressources {
    private static final Pattern REPERE = Pattern.compile("@([a-z_0-9]+)([+-]\\d+)?");

    final Police police;
    final Police petite;
    final Campagne campagne;
    private final Path cartes;
    private final Path images;
    private final Map<String, Scene> scenes = new HashMap<>();
    private final Map<String, Texture> textures = new HashMap<>();
    private int etapeDeLaRue;
    private boolean getoEnnemi;

    /** racine : le dossier "ressources", avec ses sous-dossiers decor, interface et histoire. */
    public Ressources(Path racine) throws IOException {
        cartes = racine.resolve("decor");
        images = racine.resolve("interface");
        police = Police.charger(images, 5);
        petite = Police.charger(images, 3);
        campagne = LecteurCampagne.lire(racine.resolve("histoire").resolve("campagne.txt"));
    }

    public Campagne campagne() {
        return campagne;
    }

    /** Une carte du jeu, lue une seule fois, aux couleurs de l'etape ou en est l'histoire. */
    public Scene carte(String nom) {
        Scene scene = scenes.computeIfAbsent(nom, n -> {
            try {
                return Scene.charger(cartes, n + ".niveau");
            } catch (IOException erreur) {
                throw new UncheckedIOException("Carte illisible : " + n, erreur);
            }
        });
        scene.teinter(etapeDeLaRue);
        return scene;
    }

    /** Suit l'histoire : la rue s'eteint etape par etape, et Old Geto devient gris quand le heros le quitte. */
    public void suivre(histoire.Progression progression) {
        getoEnnemi = progression.getoEnnemi();
        int etape = progression.etapeDeLaRue();
        if (etape != etapeDeLaRue) {
            etapeDeLaRue = etape;
            scenes.values().forEach(scene -> scene.teinter(etape));
        }
    }

    public boolean carteExiste(String nom) {
        return Files.isRegularFile(cartes.resolve(nom + ".niveau"));
    }

    /** L'image d'un personnage, d'un portrait ou d'un objet de l'histoire, ou null si elle n'existe pas. */
    public Texture image(String nom) {
        if (nom == null) {
            return null;
        }
        if (getoEnnemi && (nom.equals("geto") || nom.equals("buste_geto"))) {
            nom = nom + "_gris";
        }
        if (nom.startsWith("buste_") && !Files.isRegularFile(images.resolve(nom + ".png")) && nom.lastIndexOf('_') > 5) {
            return image(nom.substring(0, nom.lastIndexOf('_')));
        }
        return textures.computeIfAbsent(nom, n -> {
            Path fichier = images.resolve(n + ".png");
            if (!Files.isRegularFile(fichier)) {
                return null;
            }
            try {
                return Texture.charger(fichier);
            } catch (IOException erreur) {
                throw new UncheckedIOException("Image illisible : " + n, erreur);
            }
        });
    }

    /** L'endroit exact d'un lieu de l'histoire sur sa carte : {x, y}. */
    public float[] position(Lieu lieu) {
        float[] repere = carte(lieu.carte()).reperes.get(lieu.repere());
        if (repere == null) {
            throw new IllegalStateException("Repere inconnu : " + lieu.carte() + " " + lieu.repere());
        }
        return new float[] {repere[0] + lieu.dx(), Float.isNaN(lieu.y()) ? repere[1] : lieu.y()};
    }

    /** Une abscisse ecrite dans une cinematique : un nombre, ou un repere de la carte comme "@banc+20". */
    public float abscisse(String carte, String texte) {
        Matcher m = REPERE.matcher(texte);
        if (!m.matches()) {
            return Float.parseFloat(texte);
        }
        float[] repere = carte(carte).reperes.get(m.group(1));
        if (repere == null) {
            throw new IllegalStateException("Repere inconnu : " + carte + " " + m.group(1));
        }
        return repere[0] + (m.group(2) == null ? 0 : Integer.parseInt(m.group(2)));
    }
}
