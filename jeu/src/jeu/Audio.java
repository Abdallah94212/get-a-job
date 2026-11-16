package jeu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Le son du jeu : un mixeur logiciel, seul responsable de tout ce qu'on entend.
 *
 * <p>Chaque son passe par un bus (musique, ambiances, monde exterieur, effets, voix, interface), qui a son propre
 * volume reglable, son filtre passe-bas et sa part de reverberation. Le jeu ne pose que des intentions : un lieu
 * (la taille de la salle, le dehors qu'on entend a travers une vitre), un etat de mixage pour une cinematique (le
 * monde qui s'etouffe au moment d'un choc, puis revient), une conversation (la musique se range sous les voix).
 * Le mixeur s'en rapproche doucement : rien ne coupe sec.
 *
 * <p>Il garde aussi l'ordre : pas plus de {@value #MAX_VOIX} sons a la fois (les plus faibles cedent la place),
 * pas deux fois le meme son dans la meme fraction de seconde, une famille de prises ("pas_beton" : pas_beton_1 a 6)
 * jouee sans jamais repeter la prise d'avant. La musique de la rue respire : elle joue, se tait un long moment, revient.
 *
 * <p>Les fichiers sont ranges par dossier dans ressources/sons (WAV ou AU mu-law, mono, 22 050 Hz). Les sons courts
 * sont lus a l'avance ; les musiques sont chargees quand on les demande et liberees quand on ne les entend plus.
 * Sans carte son, ou pendant les essais, il ne joue rien mais garde trace de ce qu'il aurait joue ; hors ligne, il
 * mixe pour de vrai dans un tableau, pour qu'on puisse mesurer ce qu'il produit.
 */
public final class Audio {
    /** Chaque son appartient a un bus : un volume reglable, un filtre, une reverberation. */
    public enum Bus {
        MUSIQUE, AMBIANCE, EXTERIEUR, EFFETS, VOIX, INTERFACE
    }

    static final int FREQUENCE = 22050;
    static final int MAX_VOIX = 48;
    private static final int BLOC = 512;
    private static final float OUVERT = 10000f;
    /** Les fichiers sont ranges bas pour garder de la marge : le mixage final les remonte a un niveau d'ecoute normal. */
    private static final float GAIN_SORTIE = 2.5f;
    /** La musique qui respire : apres un passage complet, elle laisse la place a la ville, puis revient. */
    private static final Set<String> RESPIRE = Set.of("mus_rue", "mus_nuit");
    /** Les sons qu'on entend de l'interieur du corps : jamais etouffes, meme quand le monde l'est. */
    private static final Set<String> DEDANS = Set.of("coeur", "coeur_vite", "acouphene", "souffle", "souffle_air", "aie_1",
            "aie_2", "aie_3", "effort");
    private static final Set<String> MONDE = Set.of("amb_rue", "amb_rue_mouillee", "amb_pluie", "amb_gouttiere", "amb_periph",
            "amb_ruissellement", "sirene_loin", "chien_loin", "alarme_loin", "tonnerre", "bus_passe", "scooter_passe", "moto_passe",
            "voiture_passe", "voiture_passe_sec");

    /** Un son en train de jouer. Ses valeurs cibles sont posees par le jeu ; le mixeur s'en rapproche doucement. */
    private static final class Voix {
        final String nom;
        final Bus bus;
        final boolean boucle;
        double position;
        float gain;
        volatile float gainCible;
        volatile float pan;
        volatile float vitesse;
        volatile float fondu;
        volatile boolean finie;
        boolean respire;
        int retard;

        Voix(String nom, Bus bus, boolean boucle, float gain, float pan, float vitesse, float fondu) {
            this.nom = nom;
            this.bus = bus;
            this.boucle = boucle;
            this.gain = fondu > 0 ? 0 : gain;
            this.gainCible = gain;
            this.pan = pan;
            this.vitesse = vitesse;
            this.fondu = fondu;
        }
    }

    /** L'etat d'un bus dans le mixeur : son gain de mixage et son filtre, lisses, et la memoire du filtre. */
    private static final class Etat {
        float gain = 1;
        float gainCible = 1;
        float coupure = OUVERT;
        float coupureCible = OUVERT;
        float lissage = 0.3f;
        final float[] memoire = new float[4];
    }

    private final Path dossier;
    private final Reglages reglages;
    private final Map<String, Path> fichiers = new HashMap<>();
    private final Map<String, List<String>> familles = new HashMap<>();
    private final Map<String, String> derniereprise = new ConcurrentHashMap<>();
    private final Map<String, Float> dernierDepart = new ConcurrentHashMap<>();
    /** Le temps du jeu, en secondes : il avance avec les images, pas avec l'horloge de la machine. */
    private volatile float temps;
    private final Map<String, short[]> sons = new ConcurrentHashMap<>();
    private final List<Voix> voix = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Voix> boucles = new HashMap<>();
    private final List<String> journal = Collections.synchronizedList(new ArrayList<>());
    private final Map<Bus, Etat> etats = new java.util.EnumMap<>(Bus.class);
    private final Random hasard = new Random(11);
    private ExecutorService chargeur;
    private Voix musique;
    private String theme;
    private float repriseMusique = -1;
    private String etatMixage = "normal";
    private volatile boolean parole;
    private float duck = 1;
    private volatile float coupureDehors = OUVERT;
    private volatile float salleTaille = 0.3f;
    private volatile float salleMix = 0.08f;
    private float salleMixLisse = 0.08f;
    private float salleTailleLisse = 0.3f;
    private final float[][] peignes = new float[4][];
    private final int[] peigneIndex = new int[4];
    private final float[] peigneMemoire = new float[4];
    private final float[][] passeTout = {new float[556], new float[441]};
    private final int[] passeToutIndex = new int[2];
    private boolean actif;
    private boolean horsLigne;
    private double aRendre;
    private float limite = 1f;
    private float limiteAvant = 1f;

    /** muet : ne joue rien et ne lit aucun son (essais) ; garde seulement la trace de ce qui est demande. */
    public Audio(Path dossier, Reglages reglages, boolean muet) {
        this(dossier, reglages, muet, false);
    }

    private Audio(Path dossier, Reglages reglages, boolean muet, boolean horsLigne) {
        this.dossier = dossier;
        this.reglages = reglages;
        for (Bus b : Bus.values()) {
            etats.put(b, new Etat());
        }
        int[] delais = {1116, 1188, 1277, 1356};
        for (int i = 0; i < 4; i++) {
            peignes[i] = new float[delais[i] / 2];
        }
        indexer();
        this.horsLigne = horsLigne;
        if (horsLigne) {
            actif = true;
        } else if (!muet) {
            demarrer();
        }
    }

    /** Un mixeur qui calcule vraiment le son, mais dans un tableau : pour mesurer ce qu'on entendrait. */
    static Audio horsLigne(Path dossier, Reglages reglages) {
        return new Audio(dossier, reglages, false, true);
    }

    // ------------------------------------------------------------------ la bibliotheque

    /** Repere tous les fichiers, dossier par dossier, et regroupe les prises d'un meme son (nom_1, nom_2...). */
    private void indexer() {
        if (!Files.isDirectory(dossier)) {
            return;
        }
        try (Stream<Path> tout = Files.walk(dossier)) {
            tout.filter(f -> f.toString().endsWith(".wav") || f.toString().endsWith(".au")).forEach(f -> {
                String nom = f.getFileName().toString().replaceFirst("\\.(wav|au)$", "");
                fichiers.put(nom, f);
                if (nom.matches(".*_[0-9]+")) {
                    familles.computeIfAbsent(nom.replaceFirst("_[0-9]+$", ""), k -> new ArrayList<>()).add(nom);
                }
            });
        } catch (IOException erreur) {
            System.err.println("Sons illisibles : " + erreur.getMessage());
        }
        familles.values().forEach(Collections::sort);
    }

    /** Vrai si ce son existe : un fichier, ou une famille de prises. */
    boolean existe(String nom) {
        return fichiers.containsKey(nom) || familles.containsKey(nom);
    }

    /** La duree d'un son en secondes (la moyenne de ses prises pour une famille), lue d'apres la taille du fichier. */
    float duree(String nom) {
        List<String> noms = familles.containsKey(nom) ? familles.get(nom) : fichiers.containsKey(nom) ? List.of(nom) : List.of();
        double total = 0;
        for (String n : noms) {
            Path f = fichiers.get(n);
            try {
                long octets = Files.size(f);
                total += f.toString().endsWith(".au") ? (octets - 24) / (double) FREQUENCE : (octets - 44) / 2.0 / FREQUENCE;
            } catch (IOException erreur) {
                return 0.1f;
            }
        }
        return noms.isEmpty() ? 0.1f : (float) (total / noms.size());
    }

    /** Le fichier a jouer : le son lui-meme, ou une prise de sa famille, jamais la meme que la fois d'avant. */
    private String prise(String nom) {
        List<String> famille = familles.get(nom);
        if (famille == null || famille.isEmpty()) {
            return nom;
        }
        if (fichiers.containsKey(nom) && !famille.contains(nom)) {   // "clochette" et "clochette_2" : une seule famille
            famille.add(nom);
        }
        String avant = derniereprise.get(nom);
        String choisie;
        synchronized (hasard) {
            do {
                choisie = famille.get(hasard.nextInt(famille.size()));
            } while (famille.size() > 1 && choisie.equals(avant));
        }
        derniereprise.put(nom, choisie);
        return choisie;
    }

    /** Le bus d'un son, d'apres son nom : chaque famille de sons a sa place dans le mixage. */
    static Bus busDe(String nom) {
        if (nom.startsWith("mus_")) {
            return Bus.MUSIQUE;
        }
        if (nom.startsWith("v_") || DEDANS.contains(nom)) {
            return Bus.VOIX;
        }
        if (nom.startsWith("ui_") || nom.startsWith("tel_") || nom.equals("objectif") || nom.equals("mission")
                || nom.equals("nouvelle_mission") || nom.equals("notif") || nom.equals("chapitre")) {
            return Bus.INTERFACE;
        }
        if (MONDE.contains(nom) || nom.startsWith("klaxon") || nom.startsWith("rafale") || nom.startsWith("orage")) {
            return Bus.EXTERIEUR;
        }
        if (nom.startsWith("amb_") && !nom.equals("amb_pneus_mouilles") || nom.startsWith("tele") || nom.equals("montee")) {
            return Bus.AMBIANCE;
        }
        return Bus.EFFETS;
    }

    private short[] charger(String nom) {
        return sons.computeIfAbsent(nom, n -> {
            Path f = fichiers.get(n);
            if (f == null) {
                System.err.println("Son introuvable : " + n);
                return new short[0];
            }
            try (AudioInputStream brut = AudioSystem.getAudioInputStream(f.toFile());
                 AudioInputStream pcm = AudioSystem.getAudioInputStream(
                         new AudioFormat(brut.getFormat().getSampleRate(), 16, 1, true, false), brut)) {
                byte[] octets = pcm.readAllBytes();
                short[] echantillons = new short[octets.length / 2];
                for (int i = 0; i < echantillons.length; i++) {
                    echantillons[i] = (short) ((octets[i * 2] & 255) | octets[i * 2 + 1] << 8);
                }
                return echantillons;
            } catch (IOException | UnsupportedAudioFileException | IllegalArgumentException erreur) {
                System.err.println("Son illisible : " + n + " (" + erreur.getMessage() + ")");
                return new short[0];
            }
        });
    }

    /** Charge un son sans bloquer celui qui le demande. */
    private void chargerPlusTard(String nom) {
        if (chargeur != null && !sons.containsKey(nom)) {
            chargeur.submit(() -> charger(nom));
        }
    }

    private void demarrer() {
        try {
            AudioFormat format = new AudioFormat(FREQUENCE, 16, 2, true, false);
            SourceDataLine ligne = AudioSystem.getSourceDataLine(format);
            ligne.open(format, BLOC * 4 * 4);
            ligne.start();
            actif = true;
            chargeur = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "chargement-sons");
                t.setDaemon(true);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            });
            chargeur.submit(this::toutCharger);
            Thread mixeur = new Thread(() -> {
                float[] g = new float[BLOC];
                float[] d = new float[BLOC];
                byte[] sortie = new byte[BLOC * 4];
                while (true) {
                    melanger(g, d);
                    for (int i = 0; i < BLOC; i++) {
                        int l = Math.round(g[i] * 32000);
                        int r = Math.round(d[i] * 32000);
                        sortie[i * 4] = (byte) l;
                        sortie[i * 4 + 1] = (byte) (l >> 8);
                        sortie[i * 4 + 2] = (byte) r;
                        sortie[i * 4 + 3] = (byte) (r >> 8);
                    }
                    ligne.write(sortie, 0, sortie.length);
                }
            }, "mixeur");
            mixeur.setDaemon(true);
            mixeur.setPriority(Thread.MAX_PRIORITY - 1);
            mixeur.start();
        } catch (Exception erreur) {
            System.err.println("Pas de son : " + erreur.getMessage());
        }
    }

    /** Lit a l'avance les sons courts, sans bloquer le jeu : les plus legers d'abord. Les musiques attendent qu'on les demande. */
    private void toutCharger() {
        fichiers.entrySet().stream()
                .filter(e -> !e.getKey().startsWith("mus_") && !e.getKey().equals("amb_radio"))
                .sorted((a, b) -> Long.compare(a.getValue().toFile().length(), b.getValue().toFile().length()))
                .forEach(e -> charger(e.getKey()));
    }

    // ------------------------------------------------------------------ ce que le jeu demande

    /** Le jeu a avance de dt secondes. */
    void tic(float dt) {
        temps += dt;
    }

    /** Joue un effet une fois, au centre. */
    void jouer(String nom) {
        jouer(nom, 1f, 0f, 1f);
    }

    /** Joue un son une fois. pan va de -1 (a gauche) a 1 (a droite) ; vitesse change aussi la hauteur. */
    void jouer(String nom, float gain, float pan, float vitesse) {
        jouer(busDe(nom), nom, gain, pan, vitesse, 0f);
    }

    /** Joue un son une fois, sur ce bus, apres ce retard (en secondes). */
    void jouer(Bus bus, String nom, float gain, float pan, float vitesse, float retard) {
        Float avant = dernierDepart.get(nom);
        if (retard == 0 && avant != null && temps - avant < 0.035f) {
            return;                                                    // le meme son, deux fois dans la meme image : un seul
        }
        dernierDepart.put(nom, temps);
        String fichier = prise(nom);
        journal.add(nom);
        if (!actif || gain <= 0.001f) {
            return;
        }
        Voix v = new Voix(fichier, bus, false, gain, pan, vitesse, 0);
        v.retard = Math.round(retard * FREQUENCE);
        synchronized (voix) {
            long memes = voix.stream().filter(x -> !x.boucle && x.nom.equals(fichier) && !x.finie).count();
            if (memes >= 4) {                                          // pas plus de quatre fois le meme son : le plus ancien s'efface
                voix.stream().filter(x -> !x.boucle && x.nom.equals(fichier)).findFirst().ifPresent(this::effacer);
            }
            if (voix.size() >= MAX_VOIX) {                             // trop de sons : le plus faible des ponctuels cede
                Voix faible = voix.stream().filter(x -> !x.boucle && x.bus != Bus.MUSIQUE)
                        .min((a, b) -> Float.compare(a.gainCible, b.gainCible)).orElse(null);
                if (faible == null || faible.gainCible > gain) {
                    return;
                }
                faible.finie = true;
            }
            voix.add(v);
        }
    }

    /**
     * Un son qui vient d'un endroit : plus il est loin de celui qui ecoute, plus il est faible ; a gauche ou a
     * droite selon ou il se trouve. portee : la distance (en pixels) a laquelle on ne l'entend plus.
     */
    void jouerA(String nom, float gain, float xSource, float xEcoute, float portee, float vitesse) {
        float ecart = xSource - xEcoute;
        float proche = Math.max(0f, 1f - Math.abs(ecart) / portee);
        if (proche <= 0) {
            return;
        }
        float pan = Math.max(-0.85f, Math.min(0.85f, ecart / (portee * 0.55f)));
        jouer(busDe(nom), nom, gain * proche * proche, pan, vitesse, 0f);
    }

    private void effacer(Voix v) {
        v.gainCible = 0;
        v.fondu = 0.02f;
    }

    /** Fait taire tout de suite les sons ponctuels d'un bus : une voix qu'on interrompt, une scene qu'on quitte. */
    void couper(Bus bus) {
        synchronized (voix) {
            voix.stream().filter(v -> v.bus == bus && !v.boucle).forEach(this::effacer);
        }
        journal.add("couper:" + bus.name().toLowerCase());
    }

    /** On change d'ecran : plus d'effets, plus de voix ; les boucles et la musique suivront ce que demande le suivant. */
    void changerDEcran() {
        couper(Bus.EFFETS);
        couper(Bus.VOIX);
        couper(Bus.EXTERIEUR);
        couper(Bus.AMBIANCE);
        mixage("normal", 0.5f);
        parole(false);
    }

    /** Change de musique en fondu enchaine. null ou "silence" : la musique s'eteint doucement. */
    void musique(String nouveau) {
        String voulu = nouveau == null || nouveau.equals("silence") ? null : nouveau;
        if (java.util.Objects.equals(voulu, theme)) {
            respirer();
            return;
        }
        String ancien = theme;
        theme = voulu;
        journal.add("musique:" + (voulu == null ? "silence" : voulu));
        if (!actif) {
            return;
        }
        if (musique != null) {
            musique.gainCible = 0;
            musique.fondu = 2.2f;
        }
        musique = null;
        repriseMusique = -1;
        if (voulu != null) {
            lancerMusique(voulu, voulu.equals(ancien) ? 2.2f : 2.6f);
        }
        liberer(ancien);
    }

    private void lancerMusique(String nom, float fondu) {
        chargerPlusTard(nom);
        if (horsLigne) {
            charger(nom);
        }
        musique = new Voix(nom, Bus.MUSIQUE, true, 1f, 0f, 1f, fondu);
        musique.respire = RESPIRE.contains(nom);
        voix.add(musique);
    }

    /** La musique de la rue a fini son passage : la ville seule pendant un moment, puis elle revient. */
    private void respirer() {
        if (!actif || theme == null || musique == null || !musique.finie) {
            return;
        }
        if (repriseMusique < 0) {
            synchronized (hasard) {
                repriseMusique = temps + 35 + hasard.nextInt(45);
            }
            journal.add("musique:respire");
        } else if (temps >= repriseMusique) {
            repriseMusique = -1;
            lancerMusique(theme, 6f);
        }
    }

    /** Libere une musique qu'on n'entend plus : elle pese lourd en memoire. */
    private void liberer(String nom) {
        if (nom == null || chargeur == null) {
            return;
        }
        chargeur.submit(() -> {
            try {
                Thread.sleep(3000);                                    // le temps que son fondu s'acheve
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            boolean entendue;
            synchronized (voix) {
                entendue = voix.stream().anyMatch(v -> v.nom.equals(nom));
            }
            if (!entendue && !nom.equals(theme)) {
                sons.remove(nom);
            }
        });
    }

    String musiqueCourante() {
        return theme;
    }

    /**
     * Donne l'ensemble des ambiances en boucle a faire entendre maintenant : nom, puis {gain, pan, vitesse}.
     * Celles qui ne sont plus demandees s'eteignent en fondu ; les nouvelles montent en fondu.
     */
    void ambiances(Map<String, float[]> couches) {
        synchronized (boucles) {
            for (Map.Entry<String, Voix> entree : new ArrayList<>(boucles.entrySet())) {
                if (!couches.containsKey(entree.getKey())) {
                    entree.getValue().gainCible = 0;
                    entree.getValue().fondu = lent(entree.getKey()) ? 3.5f : 1.2f;
                    boucles.remove(entree.getKey());
                }
            }
            for (Map.Entry<String, float[]> c : couches.entrySet()) {
                float[] v = c.getValue();
                Voix existante = boucles.get(c.getKey());
                if (existante == null) {
                    existante = new Voix(c.getKey(), busDe(c.getKey()), true, v[0], v[1], v[2], lent(c.getKey()) ? 3.5f : 1.2f);
                    boucles.put(c.getKey(), existante);
                    if (actif) {
                        if (horsLigne) {
                            charger(c.getKey());
                        } else {
                            chargerPlusTard(c.getKey());
                        }
                        voix.add(existante);
                    }
                }
                existante.gainCible = v[0];
                existante.pan = v[1];
                existante.vitesse = v[2];
                existante.fondu = lent(c.getKey()) ? 2.5f : 0.35f;
            }
        }
    }

    /** La pluie et le vent arrivent et repartent lentement : on ne coupe pas la meteo comme un robinet. */
    private static boolean lent(String nom) {
        return nom.startsWith("amb_pluie") || nom.equals("amb_gouttiere") || nom.equals("amb_ruissellement") || nom.equals("amb_vent")
                || nom.equals("amb_rue_mouillee");
    }

    /** Les boucles d'ambiance demandees en ce moment, avec leur gain. */
    Map<String, Float> ambiancesCourantes() {
        Map<String, Float> courantes = new LinkedHashMap<>();
        synchronized (boucles) {
            boucles.forEach((nom, v) -> courantes.put(nom, v.gainCible));
        }
        return courantes;
    }

    /**
     * Un etat de mixage, rejoint en duree secondes. normal : tout est ouvert. choc : le monde s'etouffe et recule,
     * seuls restent le corps et la voix (un coup au coeur). sourdine : on est dans sa tete, le monde est loin.
     * silence : tout se tait, sauf le corps et l'interface.
     */
    void mixage(String nom, float duree) {
        etatMixage = nom;
        journal.add("mixage:" + nom);
        float lissage = Math.max(0.02f, duree);
        for (Bus b : Bus.values()) {
            Etat e = etats.get(b);
            e.lissage = lissage;
            boolean monde = b == Bus.AMBIANCE || b == Bus.EXTERIEUR || b == Bus.EFFETS;
            switch (nom) {
                case "choc" -> {
                    e.gainCible = monde ? 0.6f : b == Bus.MUSIQUE ? 0.25f : 1f;
                    e.coupureCible = monde || b == Bus.MUSIQUE ? 260f : OUVERT;
                }
                case "sourdine" -> {
                    e.gainCible = monde ? 0.75f : 1f;
                    e.coupureCible = monde ? 750f : OUVERT;
                }
                case "silence" -> {
                    e.gainCible = monde || b == Bus.MUSIQUE ? 0f : 1f;
                    e.coupureCible = OUVERT;
                }
                default -> {
                    e.gainCible = 1f;
                    e.coupureCible = OUVERT;
                }
            }
        }
    }

    String mixageCourant() {
        return etatMixage;
    }

    /** Quelqu'un parle : la musique et les ambiances se rangent un peu sous la voix. */
    void parole(boolean active) {
        parole = active;
    }

    /** Le lieu ou l'on est : la taille de la salle (0 : dehors a decouvert, 1 : un grand hall) et sa part de reverberation. */
    void salle(float taille, float mix) {
        salleTaille = Math.max(0f, Math.min(1f, taille));
        salleMix = Math.max(0f, Math.min(0.5f, mix));
    }

    /** Ce qu'on entend du dehors : grand ouvert dans la rue, etouffe derriere une vitre ou un mur. */
    void dehors(float coupure) {
        coupureDehors = coupure;
    }

    /** Tout ce qui a ete demande depuis le debut, dans l'ordre : pour verifier que le son suit le jeu. */
    List<String> journal() {
        synchronized (journal) {
            return new ArrayList<>(journal);
        }
    }

    /** Le nombre de sons qui jouent en ce moment (boucles comprises). */
    int voixEnCours() {
        synchronized (voix) {
            return (int) voix.stream().filter(v -> !v.finie).count();
        }
    }

    /** Les sons ponctuels qui jouent en ce moment sur ce bus. */
    int ponctuelsEnCours(Bus bus) {
        synchronized (voix) {
            return (int) voix.stream().filter(v -> v.bus == bus && !v.boucle && !v.finie && v.gainCible > 0).count();
        }
    }

    /** Hors ligne : calcule ce nombre de secondes de son, en stereo entrelacee (gauche, droite, gauche...). */
    float[] rendre(float secondes) {
        aRendre += secondes * FREQUENCE;                               // le reste d'un bloc passe a l'appel suivant
        int blocs = (int) (aRendre / BLOC);
        aRendre -= blocs * BLOC;
        float[] sortie = new float[blocs * BLOC * 2];
        float[] g = new float[BLOC];
        float[] d = new float[BLOC];
        for (int b = 0; b < blocs; b++) {
            melanger(g, d);
            for (int i = 0; i < BLOC; i++) {
                sortie[(b * BLOC + i) * 2] = g[i];
                sortie[(b * BLOC + i) * 2 + 1] = d[i];
            }
        }
        return sortie;
    }

    // ------------------------------------------------------------------ le melange, dans son propre fil

    private float volume(Bus bus) {
        int reglage = switch (bus) {
            case MUSIQUE -> reglages.musique;
            case AMBIANCE, EXTERIEUR -> reglages.ambiance;
            case EFFETS -> reglages.effets;
            case VOIX -> reglages.voix;
            case INTERFACE -> reglages.interfaceSon;
        };
        float v = reglage / 10f * reglages.general / 10f;
        return v * v;
    }

    private final float[][] busG = new float[Bus.values().length][BLOC];
    private final float[][] busD = new float[Bus.values().length][BLOC];
    private final float[] envoi = new float[BLOC];

    /** Calcule un bloc de son : chaque voix dans son bus, chaque bus filtre et dose, la salle, puis la sortie. */
    private void melanger(float[] gauche, float[] droite) {
        for (int b = 0; b < busG.length; b++) {
            java.util.Arrays.fill(busG[b], 0);
            java.util.Arrays.fill(busD[b], 0);
        }
        java.util.Arrays.fill(envoi, 0);
        float pas = 1f / FREQUENCE;
        List<Voix> enCours;
        synchronized (voix) {
            enCours = new ArrayList<>(voix);
        }
        for (Voix v : enCours) {
            if (v.finie) {
                continue;
            }
            short[] donnees = sons.get(v.nom);
            if (donnees == null) {
                if (!v.boucle && !horsLigne) {
                    chargerPlusTard(v.nom);                            // pas encore lu : il partira au bloc suivant
                    continue;
                }
                if (horsLigne) {
                    donnees = charger(v.nom);
                } else {
                    continue;
                }
            }
            if (donnees.length < 2) {
                v.finie = true;
                continue;
            }
            int b = v.bus.ordinal();
            float angle = (Math.max(-1f, Math.min(1f, v.pan)) + 1) * (float) Math.PI / 4;
            float g = (float) Math.cos(angle) * 1.41f;
            float d = (float) Math.sin(angle) * 1.41f;
            float vitesse = v.vitesse;
            float cible = v.gainCible;
            float rampe = v.fondu > 0 ? pas / v.fondu : 1f;
            float[] bg = busG[b];
            float[] bd = busD[b];
            for (int i = 0; i < BLOC; i++) {
                if (v.retard > 0) {
                    v.retard--;
                    continue;
                }
                if (v.gain < cible) {
                    v.gain = Math.min(cible, v.gain + rampe);
                } else if (v.gain > cible) {
                    v.gain = Math.max(cible, v.gain - rampe);
                }
                int j = (int) v.position;
                if (j >= donnees.length - 1) {
                    if (!v.boucle || v.respire) {
                        v.finie = true;
                        break;
                    }
                    v.position -= donnees.length - 1;
                    j = (int) v.position;
                }
                float f = (float) (v.position - j);
                float e = (donnees[j] * (1 - f) + donnees[j + 1] * f) / 32768f * v.gain;
                bg[i] += e * g;
                bd[i] += e * d;
                v.position += vitesse;
            }
            if (v.gain <= 0 && cible <= 0 && (v.fondu > 0 || !v.boucle)) {
                v.finie = true;
            }
        }
        synchronized (voix) {
            voix.removeIf(v -> v.finie);
        }
        // la conversation : la musique et le monde se rangent sous les voix
        float duckCible = parole ? 1f : 0f;
        duck += (duckCible - duck) * Math.min(1f, BLOC * pas / 0.35f);
        java.util.Arrays.fill(gauche, 0);
        java.util.Arrays.fill(droite, 0);
        salleMixLisse += (salleMix - salleMixLisse) * Math.min(1f, BLOC * pas / 1.5f);
        salleTailleLisse += (salleTaille - salleTailleLisse) * Math.min(1f, BLOC * pas / 1.5f);
        for (Bus bus : Bus.values()) {
            Etat e = etats.get(bus);
            float k = Math.min(1f, BLOC * pas / e.lissage);
            e.gain += (e.gainCible - e.gain) * k;
            float voulue = bus == Bus.EXTERIEUR ? Math.min(e.coupureCible, coupureDehors) : e.coupureCible;
            e.coupure = (float) Math.exp(Math.log(e.coupure) + (Math.log(voulue) - Math.log(e.coupure)) * k);
            float abaisse = switch (bus) {
                case MUSIQUE -> 1f - 0.45f * duck;
                case AMBIANCE, EXTERIEUR -> 1f - 0.25f * duck;
                case EFFETS -> 1f - 0.15f * duck;
                default -> 1f;
            };
            float niveau = volume(bus) * e.gain * abaisse;
            float[] bg = busG[bus.ordinal()];
            float[] bd = busD[bus.ordinal()];
            boolean filtre = e.coupure < OUVERT * 0.95f;
            float a = 1f - (float) Math.exp(-2 * Math.PI * e.coupure / FREQUENCE);
            float envoiBus = switch (bus) {
                case EFFETS -> 1f;
                case VOIX -> 0.55f;
                case AMBIANCE -> 0.25f;
                default -> 0f;
            };
            float[] m = e.memoire;
            for (int i = 0; i < BLOC; i++) {
                float l = bg[i];
                float r = bd[i];
                if (filtre) {                                          // deux poles : 12 dB par octave, comme a travers un mur
                    m[0] += a * (l - m[0]);
                    m[1] += a * (m[0] - m[1]);
                    m[2] += a * (r - m[2]);
                    m[3] += a * (m[2] - m[3]);
                    l = m[1];
                    r = m[3];
                } else {
                    m[0] = m[1] = l;
                    m[2] = m[3] = r;
                }
                gauche[i] += l * niveau;
                droite[i] += r * niveau;
                envoi[i] += (l + r) * 0.5f * niveau * envoiBus;
            }
        }
        reverberer(gauche, droite);
        // le niveau de sortie, puis le limiteur : il baisse tout de suite quand ca deborde, remonte lentement
        float crete = 0;
        for (int i = 0; i < BLOC; i++) {
            crete = Math.max(crete, Math.max(Math.abs(gauche[i]), Math.abs(droite[i])) * GAIN_SORTIE);
        }
        float voulu = crete > 0.8f ? 0.8f / crete : 1f;
        limite = voulu < limite ? voulu : limite + (voulu - limite) * 0.04f;
        float depart = Math.min(limite, limiteAvant);                  // l'attaque est immediate, le retour progressif
        limiteAvant = limite;
        for (int i = 0; i < BLOC; i++) {
            float g = GAIN_SORTIE * (depart + (limite - depart) * i / BLOC);
            gauche[i] = (float) Math.tanh(gauche[i] * g);              // et une courbe douce, au cas ou
            droite[i] = (float) Math.tanh(droite[i] * g);
        }
    }

    /** La salle : quatre echos qui se repondent puis se diffusent (une reverberation de Schroeder, legere). */
    private void reverberer(float[] gauche, float[] droite) {
        if (salleMixLisse < 0.005f) {
            return;
        }
        float retour = 0.55f + 0.35f * salleTailleLisse;
        float amorti = 0.45f - 0.25f * salleTailleLisse;
        for (int i = 0; i < BLOC; i++) {
            float x = envoi[i] * 0.25f;
            float somme = 0;
            for (int c = 0; c < 4; c++) {
                float[] ligne = peignes[c];
                int k = peigneIndex[c];
                float sortie = ligne[k];
                peigneMemoire[c] = sortie * (1 - amorti) + peigneMemoire[c] * amorti;
                ligne[k] = x + peigneMemoire[c] * retour;
                peigneIndex[c] = (k + 1) % ligne.length;
                somme += sortie;
            }
            for (int p = 0; p < 2; p++) {
                float[] ligne = passeTout[p];
                int k = passeToutIndex[p];
                float tampon = ligne[k];
                float y = -somme + tampon;
                ligne[k] = somme + tampon * 0.5f;
                passeToutIndex[p] = (k + 1) % ligne.length;
                somme = y;
            }
            gauche[i] += somme * salleMixLisse;
            droite[i] += somme * salleMixLisse * (i % 2 == 0 ? 0.96f : 1.04f);
        }
    }
}
