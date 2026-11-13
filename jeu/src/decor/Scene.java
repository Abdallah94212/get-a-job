package decor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Le decor en couches, charge depuis un fichier .niveau : images, obstacles et lieux. */
public final class Scene {
    /** Couches, du fond vers l'avant : lointain, facades, objets tries, premier plan, lumieres. */
    static final int LOIN = 0;
    static final int FOND = 1;
    static final int OBJETS = 2;
    static final int DEVANT = 3;
    static final int LUMIERES = 4;

    public final List<List<Element>> couches = new ArrayList<>();
    public final List<float[]> solides = new ArrayList<>();
    public final List<Zone> zones = new ArrayList<>();
    public final List<Porte> portes = new ArrayList<>();
    public final Map<String, Texture> textures = new HashMap<>();
    /** Les pixels d'origine (Endesga pure) des textures que les etapes recolorent. */
    private final Map<Texture, int[]> origines = new HashMap<>();
    private int etape;
    /** Les endroits que l'histoire cite par leur nom : la porte du Maitre Poulet, le banc, le comptoir... */
    public final Map<String, float[]> reperes = new HashMap<>();
    public Texture ciel;
    public Texture joueur;
    public int nuit;
    public float departX;
    public float departY;
    public float xMin;
    public float xMax;
    public float yMin;
    public float yMax;

    private Scene() {
        for (int i = 0; i <= LUMIERES; i++) {
            couches.add(new ArrayList<>());
        }
    }

    public static Scene charger(Path dossier, String fichier) throws IOException {
        Scene scene = new Scene();
        for (String ligne : Files.readAllLines(dossier.resolve(fichier))) {
            String[] m = ligne.trim().split("\\s+");
            switch (m[0]) {
                case "texture" -> scene.textures.put(m[1], Texture.charger(dossier.resolve(m[2])));
                case "image" -> scene.couches.get(Integer.parseInt(m[2])).add(new Element(
                        scene.texture(m[1]), f(m[3]), f(m[4]), f(m[5]), Integer.parseInt(m[6]), f(m[7])));
                case "solide" -> scene.solides.add(new float[] {f(m[1]), f(m[2]), f(m[3]), f(m[4])});
                case "zone" -> scene.zones.add(new Zone(m[1], f(m[2]), f(m[3]), scene.texture(m[4])));
                case "depart" -> {
                    scene.departX = f(m[1]);
                    scene.departY = f(m[2]);
                }
                case "limites" -> {
                    scene.xMin = f(m[1]);
                    scene.xMax = f(m[2]);
                    scene.yMin = f(m[3]);
                    scene.yMax = f(m[4]);
                }
                case "ciel" -> scene.ciel = scene.texture(m[1]);
                case "joueur" -> scene.joueur = scene.texture(m[1]);
                case "repere" -> scene.reperes.put(m[1], new float[] {f(m[2]), f(m[3])});
                case "porte" -> scene.portes.add(new Porte(f(m[1]), f(m[2]), f(m[3]), m[4], f(m[5]), f(m[6]),
                        m[7].equals("1"), m[8].equals("-") ? null : scene.texture(m[8])));
                case "nuit" -> scene.nuit = Integer.parseInt(m[1]) << 16 | Integer.parseInt(m[2]) << 8 | Integer.parseInt(m[3]);
                default -> { }
            }
        }
        scene.couches.get(OBJETS).sort(Comparator.comparingDouble(Element::base));
        return scene;
    }

    /**
     * Met le decor aux couleurs d'une etape de l'histoire (0 : le quartier vivant ... 3 : plus rien que du gris).
     * Rien n'est redessine : chacune des 32 couleurs est remplacee par celle de l'etape. Les halos, les flaques
     * de lumiere et tout ce qui n'est pas dans la palette restent tels quels.
     */
    public void teinter(int voulue) {
        voulue = Math.max(0, Math.min(Palette.SURFACES.length - 1, voulue));
        if (voulue == etape) {
            return;
        }
        etape = voulue;
        Map<Integer, Integer> rang = new HashMap<>();
        for (int i = 0; i < Palette.ORIGINE.length; i++) {
            rang.put(Palette.ORIGINE[i], i);
        }
        for (Map.Entry<String, Texture> entree : textures.entrySet()) {
            String nom = entree.getKey();
            if (nom.startsWith("halo_") || nom.startsWith("flaque") || nom.startsWith("police")) {
                continue;
            }
            int[] pixels = entree.getValue().pixels();
            int[] origine = origines.computeIfAbsent(entree.getValue(), t -> t.pixels().clone());
            for (int i = 0; i < pixels.length; i++) {
                int p = origine[i];
                int alpha = p >>> 24;
                Integer k = alpha == 0 ? null : rang.get(p & 0xFFFFFF);
                pixels[i] = k == null ? p : alpha << 24 | (alpha == 254 ? Palette.LUMIERES : Palette.SURFACES)[etape][k];
            }
        }
    }

    public int etape() {
        return etape;
    }

    private static float f(String texte) {
        return Float.parseFloat(texte);
    }

    private Texture texture(String nom) throws IOException {
        Texture texture = textures.get(nom);
        if (texture == null) {
            throw new IOException("Texture inconnue : " + nom);
        }
        return texture;
    }

    /** Vrai si les pieds du joueur, en (x, y), sortent du niveau ou touchent un obstacle. */
    public boolean bloque(float x, float y, float rayon) {
        if (x - rayon < xMin || x + rayon > xMax || y < yMin || y > yMax) {
            return true;
        }
        for (float[] s : solides) {
            if (x + rayon > s[0] && x - rayon < s[2] && y > s[1] && y < s[3]) {
                return true;
            }
        }
        return false;
    }

    public Porte porteEn(float x, float y) {
        for (Porte porte : portes) {
            if (porte.devant(x, y)) {
                return porte;
            }
        }
        return null;
    }

    public Zone zoneEn(float x) {
        for (Zone zone : zones) {
            if (zone.contient(x)) {
                return zone;
            }
        }
        return null;
    }
}
