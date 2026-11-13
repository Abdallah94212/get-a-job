package jeu;

import decor.Rendu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Decide de ce qu'on entend, image apres image : l'ambiance du lieu (la pluie dehors, etouffee dedans), les sons
 * qui viennent d'un endroit precis et changent selon qu'on s'en approche (la foule devant le cafe, la pluie sur
 * l'abribus), les pas selon le sol, le moteur du scooter, et la musique du moment.
 */
final class Sonorisation {
    /** Un son qui vient d'un repere de la carte : plus fort quand on s'approche, a gauche ou a droite selon ou il est. */
    private record Emetteur(String repere, String son, float rayon, float gain, boolean pluieSeulement) {
    }

    /** Le fond sonore de chaque interieur : des boucles et leur volume. Les rues sont traitees a part. */
    private static final Map<String, Object[]> INTERIEURS = Map.ofEntries(
            Map.entry("appartement", new Object[] {"amb_frigo", 0.35f, "amb_courant", 0.25f}),
            Map.entry("maitre_poulet", new Object[] {"amb_frigo", 0.55f, "amb_neon", 0.5f, "amb_foule", 0.12f}),
            Map.entry("labo", new Object[] {"amb_machine", 0.55f, "amb_bouillon", 0.6f, "amb_courant", 0.35f}),
            Map.entry("garage", new Object[] {"amb_courant", 0.5f}),
            Map.entry("epicerie", new Object[] {"amb_frigo", 0.7f, "amb_neon", 0.55f}),
            Map.entry("boucherie", new Object[] {"amb_frigo", 0.8f, "amb_neon", 0.6f}),
            Map.entry("metro", new Object[] {"amb_metro", 0.9f}),
            Map.entry("namek", new Object[] {"amb_vent", 0.9f}),
            Map.entry("bureaux", new Object[] {"amb_ventilation", 0.7f}),
            Map.entry("hall_tour", new Object[] {"amb_ventilation", 0.6f}),
            Map.entry("etage_anges", new Object[] {"amb_ventilation", 0.4f}),
            Map.entry("bureau_baba", new Object[] {"amb_ventilation", 0.3f}));

    private static final Map<String, List<Emetteur>> EMETTEURS = Map.of(
            "avenue_bas", List.of(new Emetteur("poulet", "amb_foule", 280, 0.45f, false),
                    new Emetteur("brasserie", "amb_foule", 220, 0.35f, false)),
            "avenue_milieu", List.of(new Emetteur("cafe", "amb_foule", 280, 0.5f, false),
                    new Emetteur("laverie", "amb_machine", 170, 0.35f, false),
                    new Emetteur("labo", "amb_bouillon", 130, 0.25f, false),
                    new Emetteur("abri", "amb_pluie_toit", 140, 0.8f, true)),
            "avenue_haut", List.of(new Emetteur("metro", "amb_metro", 220, 0.22f, false)));

    /** Les sols : ce qu'on entend sous les pieds, carte par carte. Dehors, ca depend de la pluie. */
    private static final Map<String, String> SOLS = Map.of("appartement", "pas_bois", "namek", "pas_dur");

    private static final int[] NOMBRE_DE_PAS = {3, 4, 4};

    private final Audio audio;
    private final Ressources res;
    private final Random hasard = new Random(7);
    private float distance;
    private float prochainBruit = 6f;
    private int dernierPas;

    Sonorisation(Audio audio, Ressources res) {
        this.audio = audio;
        this.res = res;
    }

    static boolean dehors(String carte) {
        return carte.startsWith("avenue");
    }

    /** La musique d'un lieu quand rien d'autre n'en impose une : la rue a son theme, le labo le sien, le reste se tait. */
    static String musiqueDuLieu(String carte) {
        if (dehors(carte)) {
            return "mus_rue";
        }
        return carte.equals("labo") ? "mus_labo" : null;
    }

    /**
     * Met a jour les ambiances pour un joueur en (x, y) sur cette carte. moteur : la vitesse du scooter s'il le
     * conduit, ou -1. En scene, les pas et les petits bruits se taisent, mais le lieu continue de vivre.
     */
    void suivre(String carte, decor.Scene scene, float x, float camX, String meteo, float moteur, boolean enScene, float dt,
                Map<String, float[]> enPlus) {
        boolean pluie = "pluie".equals(meteo);
        Map<String, float[]> couches = new HashMap<>(enPlus);
        if (dehors(carte)) {
            couches.put(pluie ? "amb_rue_mouillee" : "amb_rue", new float[] {0.75f, 0, 1});
            if (pluie) {
                couches.put("amb_pluie", new float[] {0.85f, 0, 1});
                couches.put("amb_gouttiere", new float[] {0.3f, 0, 1});
            }
            float centre = camX + Rendu.L / 2f;
            for (Emetteur e : EMETTEURS.getOrDefault(carte, List.of())) {
                float[] ou = scene.reperes.get(e.repere());
                if (ou == null || (e.pluieSeulement() && !pluie)) {
                    continue;
                }
                float ecart = Math.abs(ou[0] - x);
                if (ecart < e.rayon()) {
                    float gain = e.gain() * (1 - ecart / e.rayon());
                    float pan = Math.max(-0.8f, Math.min(0.8f, (ou[0] - centre) / (Rendu.L / 2f)));
                    float[] deja = couches.get(e.son());
                    if (deja == null || deja[0] < gain) {
                        couches.put(e.son(), new float[] {gain, pan, 1});
                    }
                }
            }
        } else {
            Object[] fond = INTERIEURS.getOrDefault(carte, new Object[0]);
            for (int i = 0; i < fond.length; i += 2) {
                couches.putIfAbsent((String) fond[i], new float[] {(Float) fond[i + 1], 0, 1});
            }
            if (pluie && !carte.equals("metro") && !carte.equals("namek")) {
                couches.put(carte.equals("garage") ? "amb_pluie_toit" : "amb_pluie_vitre",
                        new float[] {carte.equals("garage") ? 0.55f : 0.45f, 0, 1});
            }
        }
        if (moteur >= 0) {
            couches.put("scooter", new float[] {moteur > 1 ? 0.5f : 0.3f, 0, 0.75f + Math.min(1f, moteur / 220f) * 0.55f});
        }
        audio.ambiances(couches);
        if (!enScene) {
            bruitsAuHasard(carte, dt);
        }
    }

    /** De temps en temps, un bruit qui rappelle qu'on n'est pas seul : un outil au garage, une porte au loin. */
    private void bruitsAuHasard(String carte, float dt) {
        prochainBruit -= dt;
        if (prochainBruit > 0) {
            return;
        }
        prochainBruit = 7 + hasard.nextFloat() * 12;
        float pan = hasard.nextFloat() * 1.4f - 0.7f;
        if (carte.equals("garage")) {
            audio.jouer(hasard.nextBoolean() ? "metal" : "outils", 0.35f, pan, 0.9f + hasard.nextFloat() * 0.2f);
        } else if (dehors(carte)) {
            audio.jouer("porte", 0.12f, pan, 0.8f);
        }
    }

    /** Le joueur a fait ce chemin depuis la derniere image : un pas tous les 24 pixels, selon le sol. */
    void marcher(String carte, String meteo, float chemin, boolean court) {
        distance += chemin;
        if (distance < (court ? 30 : 22)) {
            return;
        }
        distance = 0;
        String sol = SOLS.getOrDefault(carte, dehors(carte) && "pluie".equals(meteo) ? "pas_mouille" : "pas_dur");
        int nombre = sol.equals("pas_mouille") ? NOMBRE_DE_PAS[0] : NOMBRE_DE_PAS[1];
        int rang = (dernierPas + 1 + hasard.nextInt(nombre - 1)) % nombre;
        dernierPas = rang;
        audio.jouer(sol + "_" + (rang + 1), court ? 0.75f : 0.55f, 0, 0.92f + hasard.nextFloat() * 0.16f);
    }

    /** Le bruit d'une porte, selon le lieu ou l'on entre. */
    void porte(String depart, String arrivee) {
        switch (arrivee) {
            case "maitre_poulet", "epicerie", "boucherie" -> audio.jouer("clochette", 0.6f, 0, 1);
            case "garage" -> audio.jouer("rideau_fer", 0.6f, 0, 1);
            case "metro" -> audio.jouer(depart.equals("boucherie") ? "porte" : "porte_lourde", 0.6f, 0, 0.9f);
            default -> {
                if (!dehors(depart) || !dehors(arrivee)) {
                    audio.jouer(dehors(arrivee) ? "porte" : "porte_ouvre", 0.6f, 0, 1);
                }
            }
        }
    }

    Audio audio() {
        return audio;
    }

    Ressources ressources() {
        return res;
    }
}
