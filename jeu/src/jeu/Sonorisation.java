package jeu;

import decor.Rendu;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Decide de ce qu'on entend, image apres image, selon le lieu. La carte sonore du jeu est ici (voir aussi SON.md).
 *
 * <p>Chaque lieu a :
 * <ul>
 *   <li>un fond : des boucles qui ne s'arretent pas (le frigo, le neon, les serveurs du labo) ;</li>
 *   <li>des sources placees dans le decor, plus fortes quand on s'en approche et a gauche ou a droite selon ou
 *       elles sont (la radio sur le comptoir, la friteuse au fond, la foule devant le cafe) ;</li>
 *   <li>des evenements qui arrivent au hasard, chacun a son rythme, pour que rien ne tourne en boucle visible
 *       (un klaxon, la vaisselle, un bip du labo, les voisins qui se disputent a travers le mur) ;</li>
 *   <li>un sol, une taille de salle, et ce qu'on y entend du dehors : la rue et la pluie passent par le meme bus,
 *       grand ouvert dans la rue, etouffe derriere une vitre, absent sous terre.</li>
 * </ul>
 * Il s'occupe aussi du corps (les pas selon le sol, la course, l'arret), du scooter (ralenti, montee en regime,
 * pneus sur chaussee mouillee), des portes et des gestes (lire, ranger, recolter...).
 */
final class Sonorisation {
    /** Un son qui vient d'un repere de la carte : plus fort quand on s'approche, a gauche ou a droite selon ou il est. */
    private record Emetteur(String repere, String son, float rayon, float gain, boolean pluieSeulement) {
    }

    /** Un bruit qui revient de temps en temps : entre min et max secondes, jamais deux fois la meme prise. */
    private record Evenement(String son, float gain, float min, float max) {
    }

    /**
     * Un lieu : ses boucles (nom, gain...), ce qu'on entend du dehors (0 : rien ; 1 : on est dehors) et a travers quoi
     * (la frequence au-dessus de laquelle le mur coupe), la pluie sur ses vitres, sa salle, son sol et ses evenements.
     */
    private record Lieu(Object[] boucles, float dehors, float coupure, float vitre, float taille, float reverb, String sol,
                        List<Evenement> evenements) {
    }

    private static final List<Evenement> RUE = List.of(
            new Evenement("klaxon", 0.3f, 14, 40), new Evenement("bus_passe", 0.45f, 40, 95),
            new Evenement("scooter_passe", 0.4f, 22, 60), new Evenement("moto_passe", 0.32f, 35, 90),
            new Evenement("sirene_loin", 0.3f, 45, 120), new Evenement("chien_loin", 0.24f, 30, 90),
            new Evenement("alarme_loin", 0.14f, 90, 200), new Evenement("bouteille", 0.2f, 30, 80),
            new Evenement("porte", 0.13f, 20, 50), new Evenement("rideau_fer", 0.16f, 60, 150),
            new Evenement("clochette", 0.1f, 30, 70), new Evenement("passants", 0.2f, 8, 22),
            new Evenement("rafale", 0.32f, 35, 90), new Evenement("plaque_egout", 0.12f, 60, 150));

    private static final Map<String, Lieu> LIEUX = new HashMap<>();

    static {
        Lieu rue = new Lieu(new Object[0], 1f, 10000f, 0f, 0.35f, 0.07f, "rue", RUE);
        LIEUX.put("avenue_bas", rue);
        LIEUX.put("avenue_milieu", rue);
        LIEUX.put("avenue_haut", rue);
        // chez soi : le frigo, l'horloge, la tele restee sur la neige ; la rue en bas, derriere la vitre
        LIEUX.put("appartement", new Lieu(new Object[] {"amb_frigo", 0.3f, "amb_courant", 0.18f, "amb_horloge", 0.32f},
                0.42f, 700f, 0.45f, 0.22f, 0.09f, "pas_bois", List.of(
                new Evenement("voisins", 0.32f, 70, 160), new Evenement("robinet", 0.16f, 50, 120),
                new Evenement("parquet", 0.18f, 25, 60), new Evenement("chien_loin", 0.2f, 40, 100),
                new Evenement("sirene_loin", 0.22f, 60, 140))));
        LIEUX.put("maitre_poulet", new Lieu(new Object[] {"amb_frigo", 0.45f, "amb_neon", 0.42f, "amb_foule", 0.1f,
                "amb_ventilation", 0.3f}, 0.4f, 900f, 0.3f, 0.33f, 0.11f, "pas_carrelage", List.of(
                new Evenement("vaisselle", 0.28f, 10, 25), new Evenement("casserole", 0.22f, 15, 35),
                new Evenement("micro_ondes", 0.22f, 30, 70), new Evenement("caisse", 0.28f, 25, 60),
                new Evenement("hachoir", 0.26f, 12, 30), new Evenement("clochette_2", 0.16f, 40, 90),
                new Evenement("clients", 0.2f, 10, 25))));
        LIEUX.put("labo", new Lieu(new Object[] {"amb_machine", 0.32f, "amb_courant", 0.28f, "amb_ventilation", 0.35f},
                0.15f, 500f, 0.15f, 0.5f, 0.16f, "pas_beton", List.of(
                new Evenement("bip", 0.22f, 6, 18), new Evenement("ferraille", 0.18f, 20, 50),
                new Evenement("interrupteur", 0.18f, 30, 70), new Evenement("bulles", 0.24f, 12, 30),
                new Evenement("clavier", 0.18f, 25, 60))));
        LIEUX.put("garage", new Lieu(new Object[] {"amb_courant", 0.4f, "amb_compresseur", 0.18f}, 0.6f, 1600f, 0f, 0.7f,
                0.22f, "pas_beton", List.of(new Evenement("outil", 0.28f, 10, 25), new Evenement("ferraille", 0.24f, 14, 35),
                new Evenement("declic", 0.18f, 30, 60))));
        LIEUX.put("epicerie", new Lieu(new Object[] {"amb_frigo", 0.5f, "amb_neon", 0.5f}, 0.35f, 900f, 0.3f, 0.33f, 0.11f,
                "pas_carrelage", List.of(new Evenement("tinte", 0.24f, 12, 30), new Evenement("caisse", 0.24f, 30, 70),
                new Evenement("clients", 0.14f, 20, 45))));
        LIEUX.put("boucherie", new Lieu(new Object[] {"amb_frigo", 0.7f, "amb_neon", 0.5f}, 0.3f, 900f, 0.25f, 0.33f, 0.11f,
                "pas_carrelage", List.of(new Evenement("hachoir", 0.38f, 8, 20), new Evenement("couteau", 0.28f, 12, 28),
                new Evenement("caisse", 0.24f, 30, 60))));
        // la rame : le roulement, les gens, les journaux ; pas de dehors, pas de pluie
        LIEUX.put("metro", new Lieu(new Object[] {"amb_metro", 0.9f, "amb_foule", 0.08f}, 0f, 500f, 0f, 0.38f, 0.1f,
                "pas_metal", List.of(new Evenement("clients", 0.16f, 6, 16), new Evenement("vetement", 0.14f, 10, 25),
                new Evenement("livre_page", 0.14f, 20, 50))));
        // la cour du 29 : la ville derriere les immeubles, la pluie qui goutte, des fenetres ouvertes, l'echo entre les murs
        LIEUX.put("cour", new Lieu(new Object[] {"amb_ruissellement", 0.3f}, 0.55f, 1800f, 0f, 0.5f, 0.18f, "rue", List.of(
                new Evenement("voisins", 0.22f, 40, 100), new Evenement("chien_loin", 0.2f, 30, 80),
                new Evenement("bouteille", 0.18f, 30, 70), new Evenement("rafale", 0.3f, 30, 80))));
        LIEUX.put("namek", new Lieu(new Object[] {"amb_vent", 0.8f, "amb_cigales", 0.45f}, 0f, 500f, 0f, 0f, 0f, "pas_gravier",
                List.of(new Evenement("rafale", 0.4f, 20, 50), new Evenement("cailloux", 0.2f, 15, 35),
                        new Evenement("feuillage", 0.18f, 12, 30))));
        List<Evenement> bureaux = List.of(new Evenement("sonnerie_bureau", 0.24f, 25, 60),
                new Evenement("clavier", 0.24f, 8, 20), new Evenement("pas_carrelage", 0.12f, 10, 25),
                new Evenement("clients", 0.13f, 15, 30));
        LIEUX.put("bureaux", new Lieu(new Object[] {"amb_ventilation", 0.7f}, 0.15f, 600f, 0.25f, 0.6f, 0.16f, "pas_carrelage", bureaux));
        LIEUX.put("hall_tour", new Lieu(new Object[] {"amb_ventilation", 0.6f}, 0.2f, 600f, 0.2f, 0.85f, 0.24f, "pas_carrelage",
                List.of(new Evenement("ascenseur", 0.3f, 40, 90), new Evenement("pas_carrelage", 0.14f, 8, 20),
                        new Evenement("clients", 0.14f, 12, 30))));
        LIEUX.put("etage_anges", new Lieu(new Object[] {"amb_ventilation", 0.4f}, 0.1f, 500f, 0.2f, 0.6f, 0.16f, "pas_carrelage", bureaux));
        LIEUX.put("bureau_baba", new Lieu(new Object[] {"amb_ventilation", 0.3f, "amb_horloge", 0.25f}, 0.1f, 500f, 0.2f, 0.4f,
                0.1f, "pas_carrelage", List.of()));
    }

    private static final Lieu INCONNU = new Lieu(new Object[0], 0.3f, 800f, 0f, 0.3f, 0.08f, "pas_dur", List.of());

    private static final Map<String, List<Emetteur>> EMETTEURS = Map.ofEntries(
            Map.entry("avenue_bas", List.of(new Emetteur("poulet", "amb_foule", 280, 0.45f, false),
                    new Emetteur("brasserie", "amb_foule", 220, 0.35f, false),
                    new Emetteur("poulet", "amb_neon", 90, 0.22f, false),
                    new Emetteur("telephonie", "amb_pluie_vitre", 110, 0.35f, true),
                    new Emetteur("presse", "amb_pluie_vitre", 110, 0.3f, true))),
            Map.entry("avenue_milieu", List.of(new Emetteur("cafe", "amb_foule", 280, 0.5f, false),
                    new Emetteur("laverie", "amb_machine", 170, 0.35f, false),
                    new Emetteur("labo", "amb_bouillon", 110, 0.18f, false),
                    new Emetteur("abri", "amb_pluie_toit", 140, 0.8f, true),
                    new Emetteur("cafe", "amb_pluie_vitre", 120, 0.35f, true),
                    new Emetteur("epicerie", "amb_pluie_vitre", 110, 0.3f, true))),
            Map.entry("avenue_haut", List.of(new Emetteur("metro", "amb_metro", 220, 0.22f, false),
                    new Emetteur("tour", "amb_ventilation", 260, 0.3f, false),
                    new Emetteur("bureaux", "amb_ventilation", 160, 0.25f, false))),
            Map.entry("appartement", List.of(new Emetteur("tele", "tele_neige", 260, 0.12f, false))),
            Map.entry("maitre_poulet", List.of(new Emetteur("comptoir", "amb_radio", 300, 0.34f, false),
                    new Emetteur("arriere", "amb_friture", 280, 0.42f, false))),
            Map.entry("labo", List.of(new Emetteur("poste", "amb_serveurs", 240, 0.55f, false),
                    new Emetteur("cuves", "amb_bouillon", 220, 0.6f, false))),
            Map.entry("garage", List.of(new Emetteur("etabli", "amb_radio", 260, 0.16f, false))),
            Map.entry("epicerie", List.of(new Emetteur("frigos", "amb_compresseur", 220, 0.35f, false))),
            Map.entry("boucherie", List.of(new Emetteur("caisses", "amb_compresseur", 220, 0.3f, false))));

    /** Les sols des cartes de rue dependent de la pluie ; les autres sont dans LIEUX. */
    private static boolean pluvieux(String meteo) {
        return "pluie".equals(meteo);
    }

    private final Audio audio;
    private final Ressources res;
    private final Random hasard = new Random(7);
    private final Map<String, Float> prochains = new HashMap<>();
    private String carteSuivie;
    private float distance;
    private int pasFaits;
    private float vitessePas;
    private boolean versDroitePas = true;
    private float moteurLisse;
    private float moteurAvant;

    Sonorisation(Audio audio, Ressources res) {
        this.audio = audio;
        this.res = res;
    }

    /** A ciel ouvert : les trois cartes de l'avenue, et la cour du 29. */
    static boolean dehors(String carte) {
        return carte.startsWith("avenue") || carte.equals("cour");
    }

    private static Lieu lieu(String carte) {
        return LIEUX.getOrDefault(carte, INCONNU);
    }

    /** La musique d'un lieu quand rien d'autre n'en impose une : la rue a son theme, le labo le sien, le reste se tait. */
    static String musiqueDuLieu(String carte) {
        if (dehors(carte)) {
            return "mus_rue";
        }
        return carte.equals("labo") ? "mus_labo" : null;
    }

    /**
     * Met a jour les ambiances pour quelqu'un qui ecoute en x sur cette carte. moteur : la vitesse du scooter s'il le
     * conduit, ou -1. courant : faux quand l'electricite est coupee chez soi (le frigo se tait, l'horloge reste).
     * En scene, les pas et les petits bruits se taisent, mais le lieu continue de vivre.
     */
    void suivre(String carte, decor.Scene scene, float x, float camX, String meteo, float moteur, boolean enScene, float dt,
                Map<String, float[]> enPlus, boolean courant) {
        boolean pluie = pluvieux(meteo);
        Lieu ici = lieu(carte);
        Map<String, float[]> couches = new HashMap<>(enPlus);
        // le dehors : la rue, le periph, la pluie ; etouffes par les murs quand on est dedans
        if (ici.dehors() > 0) {
            float d = ici.dehors();
            couches.put(pluie ? "amb_rue_mouillee" : "amb_rue", new float[] {0.75f * d, 0, 1});
            couches.put("amb_periph", new float[] {0.45f * d, 0, 1});      // le periph, au loin, qui ne s'arrete jamais
            if (pluie) {
                couches.put("amb_pluie", new float[] {0.85f * d, 0, 1});
                couches.put("amb_gouttiere", new float[] {0.3f * d, 0, 1});
                if (dehors(carte)) {
                    couches.put("amb_ruissellement", new float[] {0.25f, 0, 1});
                }
            }
        }
        audio.dehors(ici.coupure());
        audio.salle(ici.taille(), ici.reverb());
        if (pluie && ici.vitre() > 0) {
            couches.put("amb_pluie_vitre", new float[] {ici.vitre(), 0, 1});
        }
        if (pluie && carte.equals("garage")) {
            couches.put("amb_pluie_toit", new float[] {0.55f, 0, 1});
        }
        Object[] fond = ici.boucles();
        for (int i = 0; i < fond.length; i += 2) {
            String son = (String) fond[i];
            if (!courant && (son.equals("amb_frigo") || son.equals("amb_courant"))) {
                continue;                                              // plus de courant : le silence d'un frigo arrete
            }
            couches.putIfAbsent(son, new float[] {(Float) fond[i + 1], 0, 1});
        }
        float centre = camX + Rendu.L / 2f;
        for (Emetteur e : EMETTEURS.getOrDefault(carte, List.of())) {
            float[] ou = scene.reperes.get(e.repere());
            if (ou == null || (e.pluieSeulement() && !pluie) || (!courant && e.son().startsWith("tele"))) {
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
        moteur(couches, moteur, pluie, dt);
        audio.ambiances(couches);
        if (!carte.equals(carteSuivie)) {                              // un nouveau lieu : ses evenements repartent de zero
            carteSuivie = carte;
            prochains.clear();
        }
        if (!enScene) {
            evenements(ici, carte, pluie, dt);
        }
    }

    /**
     * Le scooter qu'on conduit : le ralenti qui hoquette a l'arret, le regime qui monte avec la vitesse (et un peu plus
     * quand on accelere), qui retombe quand on lache ; les pneus qui chuintent sur la chaussee mouillee.
     */
    private void moteur(Map<String, float[]> couches, float moteur, boolean pluie, float dt) {
        if (moteur < 0) {
            moteurLisse = 0;
            moteurAvant = 0;
            return;
        }
        float s = Math.min(1f, moteur / 220f);
        float accel = dt > 0 ? (s - moteurAvant) / dt : 0;
        moteurAvant = s;
        moteurLisse += (s - moteurLisse) * Math.min(1f, dt * 6);
        float charge = accel > 0.3f ? 0.12f : accel < -0.3f ? -0.08f : 0f;
        couches.put("scooter_ralenti", new float[] {0.09f + 0.32f * (1 - moteurLisse), 0, 1 + 0.15f * moteurLisse});
        couches.put("scooter", new float[] {Math.max(0f, 0.5f * moteurLisse + charge), 0, 0.72f + 0.58f * moteurLisse + charge * 0.6f});
        if (pluie) {
            couches.put("amb_pneus_mouilles", new float[] {0.4f * moteurLisse, 0, 0.8f + 0.4f * moteurLisse});
        }
    }

    /** Les evenements du lieu, chacun a son propre rythme : des couches qui ne se repetent jamais de la meme facon. */
    private void evenements(Lieu ici, String carte, boolean pluie, float dt) {
        for (Evenement e : ici.evenements()) {
            Float reste = prochains.get(e.son());
            if (reste == null) {                                       // le premier arrive a un moment au hasard
                prochains.put(e.son(), e.min() * 0.3f + hasard.nextFloat() * e.max());
                continue;
            }
            reste -= dt;
            if (reste > 0) {
                prochains.put(e.son(), reste);
                continue;
            }
            prochains.put(e.son(), e.min() + hasard.nextFloat() * (e.max() - e.min()));
            float pan = hasard.nextFloat() * 1.6f - 0.8f;
            float gain = e.gain() * (0.6f + 0.4f * hasard.nextFloat());
            float hauteur = 0.92f + hasard.nextFloat() * 0.16f;
            switch (e.son()) {
                case "passants" -> passants(Audio.Bus.EXTERIEUR, gain, pan);
                case "clients" -> passants(Audio.Bus.EFFETS, gain, pan);
                case "rafale" -> {
                    if (pluie || carte.equals("namek") || hasard.nextBoolean()) {
                        audio.jouer(Audio.Bus.EXTERIEUR, "rafale", gain, pan, hauteur, 0);
                        if (dehors(carte)) {
                            audio.jouer(Audio.Bus.EXTERIEUR, "grince", gain * 0.5f, -pan, hauteur, 0.8f);   // une enseigne qui bouge
                        }
                    }
                }
                case "pas_carrelage" -> {                              // quelqu'un traverse le hall, au loin
                    for (int k = 0; k < 5; k++) {
                        audio.jouer(Audio.Bus.EFFETS, "pas_carrelage", gain * (1 - k * 0.12f), pan + k * 0.06f, hauteur, k * 0.42f);
                    }
                }
                default -> audio.jouer(Audio.busDe(e.son()), e.son(), gain, pan, hauteur, 0);
            }
        }
    }

    /** Des gens qui parlent, plus loin : trois a six syllabes d'une voix de passant, sans qu'on comprenne les mots. */
    private void passants(Audio.Bus bus, float gain, float pan) {
        String banque = hasard.nextBoolean() ? "v_homme" : "v_femme_passante";
        float hauteur = 0.88f + hasard.nextFloat() * 0.25f;
        int syllabes = 3 + hasard.nextInt(4);
        float t = 0;
        for (int k = 0; k < syllabes; k++) {
            audio.jouer(bus, banque, gain * (0.7f + 0.3f * hasard.nextFloat()), pan, hauteur * (1.04f - 0.03f * k), t);
            t += 0.1f + hasard.nextFloat() * 0.09f + (hasard.nextFloat() < 0.15f ? 0.25f : 0);
        }
    }

    // ------------------------------------------------------------------ le corps

    /**
     * Les pieds, a chaque image ou l'on marche : un pas tous les 22 pixels (30 en courant), selon le sol ; dans la rue
     * mouillee, une flaque de temps en temps ; chez soi, une latte qui grince. La course froisse les vetements, et
     * s'arreter net apres une course, ou faire demi-tour en courant, fait crisser la semelle.
     */
    void pieds(String carte, String meteo, float chemin, boolean court, float vitesse, boolean versDroite) {
        String sol = sol(carte, meteo);
        boolean demiTour = versDroite != versDroitePas && vitesse > 110;
        if ((vitessePas > 120 && vitesse < 25) || demiTour) {
            audio.jouer(Audio.Bus.EFFETS, sol, 0.42f, 0, 0.82f, 0);
            audio.jouer(Audio.Bus.EFFETS, "vetement", 0.2f, 0, 1.1f, 0.02f);
        }
        vitessePas = vitesse;
        versDroitePas = versDroite;
        distance += chemin;
        if (distance < (court ? 30 : 22)) {
            return;
        }
        distance = 0;
        pasFaits++;
        float gain = court ? 0.72f : 0.5f;
        audio.jouer(Audio.Bus.EFFETS, sol, gain, 0, 0.93f + hasard.nextFloat() * 0.14f, 0);
        if (sol.equals("pas_mouille") && hasard.nextFloat() < 0.15f) {
            audio.jouer(Audio.Bus.EFFETS, "pas_flaque", gain * 0.6f, 0, 1f, 0.01f);
        }
        if (sol.equals("pas_bois") && hasard.nextFloat() < 0.08f) {
            audio.jouer(Audio.Bus.EFFETS, "parquet", 0.3f, 0, 1f, 0.03f);
        }
        if (court && pasFaits % 3 == 0) {
            audio.jouer(Audio.Bus.EFFETS, "vetement", 0.16f, 0, 1.05f, 0.02f);
        }
    }

    /** Le sol sous les pieds : le carrelage d'une boutique, le beton du garage, la rue seche ou mouillee. */
    static String sol(String carte, String meteo) {
        String sol = lieu(carte).sol();
        if (sol.equals("rue")) {
            return pluvieux(meteo) ? "pas_mouille" : "pas_dur";
        }
        return sol;
    }

    /** On passe une porte : la poignee, la porte qui s'ouvre, puis qui se referme derriere soi. */
    void porte(String depart, String arrivee) {
        if (dehors(depart) && dehors(arrivee)) {
            return;                                                    // d'un bout de l'avenue a l'autre : pas de porte
        }
        String lieu = dehors(arrivee) ? depart : arrivee;
        switch (lieu) {
            case "maitre_poulet", "epicerie", "boucherie" -> {
                audio.jouer("clochette", 0.55f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "porte_ferme", 0.3f, 0, 1.05f, 0.55f);
            }
            case "garage" -> audio.jouer("rideau_fer", 0.6f, 0, 1);
            case "metro" -> audio.jouer("porte_lourde", 0.55f, 0, 0.9f);
            case "labo" -> {                                           // la porte blindee : lourde, verrouillee
                audio.jouer("serrure", 0.5f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "porte_lourde", 0.6f, 0, 0.85f, 0.25f);
            }
            case "appartement" -> {
                audio.jouer("poignee", 0.45f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "porte_ouvre_bois", 0.5f, 0, 1, 0.12f);
                audio.jouer(Audio.Bus.EFFETS, "porte_ferme", 0.4f, 0, 1, 0.75f);
            }
            default -> {
                audio.jouer("poignee", 0.4f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "porte_ouvre", 0.5f, 0, 1, 0.1f);
                audio.jouer(Audio.Bus.EFFETS, "porte_ferme", 0.35f, 0, 1, 0.7f);
            }
        }
    }

    /** Ce qu'on fait de ses mains, point par point : chaque geste a son bruit, pas un "objet" generique. */
    void geste(String point) {
        switch (point.replaceFirst("[0-9]+$", "")) {
            case "lettre" -> audio.jouer("papier", 0.6f, 0, 1);
            case "hagakure" -> {
                audio.jouer("livre_ouvre", 0.6f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "livre_page", 0.45f, 0, 1, 0.35f);
            }
            case "plan" -> audio.jouer("froissement", 0.45f, 0, 1.1f);
            case "rs6_regard" -> audio.jouer("froissement", 0.4f, 0, 0.8f);
            case "chambre_froide", "caisses" -> {
                audio.jouer("effort", 0.45f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "caisse_pose", 0.6f, 0, 1, 0.3f);
            }
            case "poste_labo" -> {
                audio.jouer("clavier", 0.4f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "bip", 0.3f, 0, 1, 0.5f);
            }
            case "casier_labo" -> audio.jouer("porte_grince", 0.45f, 0, 1.2f);
            case "flipper" -> {                                        // le tiroir de l'etabli, le boitier qui s'allume
                audio.jouer("tiroir_ouvre", 0.45f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "declic", 0.4f, 0, 1, 0.45f);
                audio.jouer(Audio.Bus.EFFETS, "bip_3", 0.3f, 0, 1, 0.6f);
                audio.jouer(Audio.Bus.EFFETS, "tiroir_ferme", 0.4f, 0, 1, 0.9f);
            }
            case "rs6_bache" -> { }                                    // la scene des deux beepers fait elle-meme ses bruits
            case "plant" -> {
                audio.jouer("feuillage", 0.55f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "feuillage", 0.4f, 0, 1.1f, 0.18f);
            }
            case "rs6_namek" -> {
                audio.jouer("caisse_pose", 0.5f, 0, 1);
                audio.jouer(Audio.Bus.EFFETS, "porte_lourde", 0.5f, 0, 1.1f, 0.6f);
            }
            case "cuves" -> {
                audio.jouer("ferraille", 0.45f, 0, 0.9f);
                audio.jouer(Audio.Bus.EFFETS, "bulles", 0.35f, 0, 1, 0.3f);
            }
            case "comptoir_epicerie" -> audio.jouer("sac_pose", 0.65f, 0, 1);
            default -> audio.jouer("objet", 0.6f, 0, 1);
        }
    }

    /** On monte sur le scooter : la bequille qu'on releve, la selle, puis le moteur qui part. */
    void monterScooter() {
        audio.jouer("bequille", 0.45f, 0, 1.1f);
        audio.jouer(Audio.Bus.EFFETS, "vetement", 0.3f, 0, 1, 0.12f);
        audio.jouer(Audio.Bus.EFFETS, "scooter_demarre", 0.7f, 0, 1, 0.35f);
    }

    /** On le gare : le moteur qu'on coupe, la bequille. */
    void garerScooter() {
        audio.jouer("scooter_coupe", 0.6f, 0, 1);
        audio.jouer(Audio.Bus.EFFETS, "bequille", 0.45f, 0, 0.95f, 0.5f);
        audio.jouer(Audio.Bus.EFFETS, "cles_main", 0.35f, 0, 1, 0.9f);
    }

    Audio audio() {
        return audio;
    }

    Ressources ressources() {
        return res;
    }
}
