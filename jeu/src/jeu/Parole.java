package jeu;

import histoire.Campagne;
import java.text.Normalizer;
import java.util.Map;
import java.util.Random;

/**
 * La voix d'un personnage pendant que sa replique s'ecrit : pas de doublage, des syllabes sans langue
 * ("bwa-mo-de..."), propres a chacun, qui suivent le texte.
 *
 * <p>Une syllabe part a chaque groupe de voyelles, pas a chaque lettre, et jamais plus vite que le debit du
 * personnage : Youngssef mitraille, Geto prend son temps. La phrase a une melodie : elle part un peu haut et descend,
 * remonte sur la fin d'une question, monte et force sur une exclamation, s'eteint sur des points de suspension.
 * L'emotion de la replique ("dit geto:colere") change la hauteur, l'ecart, la force et le debit, et peut s'annoncer
 * par une petite reaction avant le texte : un soupir, un "hm", une inspiration. Les interjections du texte
 * ("Hm.", "Pff", "Haha", "Hein ?") sont dites par une vraie reaction, pas par des syllabes.
 *
 * <p>La voix s'arrete net quand le joueur termine la replique d'une touche ou ferme la boite.
 */
final class Parole {
    /** Ce qu'une emotion change : hauteur, ecart melodique, force, debit, et la reaction qui l'annonce. */
    record Emotion(float hauteur, float melodie, float force, float debit, String reaction) {
    }

    static final Map<String, Emotion> EMOTIONS = Map.ofEntries(
            Map.entry("neutre", new Emotion(1f, 1f, 1f, 1f, null)),
            Map.entry("joie", new Emotion(1.07f, 1.35f, 1.05f, 1.1f, null)),
            Map.entry("rire", new Emotion(1.06f, 1.3f, 1.05f, 1.1f, "rire")),
            Map.entry("colere", new Emotion(0.96f, 0.75f, 1.3f, 1.18f, "grogne")),
            Map.entry("menace", new Emotion(0.9f, 0.5f, 0.95f, 0.8f, null)),
            Map.entry("triste", new Emotion(0.93f, 0.6f, 0.8f, 0.82f, "soupir")),
            Map.entry("fatigue", new Emotion(0.92f, 0.55f, 0.8f, 0.8f, "soupir")),
            Map.entry("peur", new Emotion(1.1f, 1.25f, 0.85f, 1.25f, "inspire")),
            Map.entry("surpris", new Emotion(1.12f, 1.3f, 1.1f, 1.05f, "oh")),
            Map.entry("gene", new Emotion(1f, 0.7f, 0.75f, 0.85f, "hm")),
            Map.entry("hesite", new Emotion(0.98f, 0.8f, 0.85f, 0.8f, "hm")),
            Map.entry("doute", new Emotion(1f, 1f, 0.9f, 0.9f, "hein")),
            Map.entry("insolent", new Emotion(0.97f, 0.9f, 1.05f, 0.92f, null)));

    /** Les interjections qu'on dit avec le corps plutot qu'avec des syllabes. */
    private static final Map<String, String> INTERJECTIONS = Map.ofEntries(
            Map.entry("hm", "hm"), Map.entry("hmm", "hm"), Map.entry("mmh", "hm"), Map.entry("euh", "hm"),
            Map.entry("ha", "rire"), Map.entry("haha", "rire"), Map.entry("hahaha", "rire"), Map.entry("hehe", "rire"),
            Map.entry("hein", "hein"), Map.entry("oh", "oh"), Map.entry("ah", "oh"), Map.entry("pff", "soupir"),
            Map.entry("pfff", "soupir"), Map.entry("tss", "soupir"), Map.entry("ouf", "soupir"), Map.entry("grr", "grogne"));

    /** La duree des reactions qui ouvrent une replique : le texte attend qu'elles soient dites. */
    private static final Map<String, Float> DUREES = Map.of("soupir", 0.6f, "hm", 0.4f, "oh", 0.25f, "inspire", 0.3f,
            "rire", 0.45f, "grogne", 0.3f, "hein", 0.25f);

    private final Audio audio;
    private final Campagne campagne;
    private final Random hasard = new Random(3);
    private Campagne.Vocal vocal;
    /** L'ecart entre deux syllabes de cette voix : un peu moins que la duree de ses syllabes, pour qu'elles s'enchainent. */
    private float pasDeVoix = 0.1f;
    private final Map<String, Float> durees = new java.util.HashMap<>();
    private Emotion emotion = EMOTIONS.get("neutre");
    private String texte = "";
    private int vu;
    private int sautJusqua;
    private float depuis = 10f;
    private float pan;
    private boolean active;

    Parole(Audio audio, Campagne campagne) {
        this.audio = audio;
        this.campagne = campagne;
    }

    /**
     * Une replique commence : qui parle, sur quel ton, et de quel cote de l'ecran (son buste). Renvoie le temps que
     * le texte doit attendre avant de s'ecrire : celui de la reaction qui l'ouvre, s'il y en a une.
     */
    float commencer(String qui, String ton, String replique, float cote) {
        vocal = campagne.vocal(qui);
        pasDeVoix = 0.62f * durees.computeIfAbsent(vocal.banque(), b -> audio.duree("v_" + b));
        emotion = EMOTIONS.getOrDefault(ton == null || ton.isEmpty() ? "neutre" : ton, EMOTIONS.get("neutre"));
        texte = replique;
        vu = 0;
        sautJusqua = 0;
        depuis = 10f;
        pan = cote;
        active = true;
        if (emotion.reaction() != null) {
            reagir(emotion.reaction(), 1f);
            return DUREES.getOrDefault(emotion.reaction(), 0.3f);
        }
        return 0f;
    }

    /** Le texte deja ecrit, a cette image : on dit les syllabes des lettres qui viennent d'apparaitre. */
    void suivre(String visible, float dt) {
        if (!active) {
            return;
        }
        depuis += dt;
        int fin = Math.min(visible.length(), texte.length());
        for (int i = vu; i < fin; i++) {
            char c = texte.charAt(i);
            boolean lettre = Character.isLetter(c);
            if (lettre && (i == 0 || !Character.isLetter(texte.charAt(i - 1)))) {
                int bout = i;
                while (bout < texte.length() && Character.isLetter(texte.charAt(bout))) {
                    bout++;
                }
                String reaction = INTERJECTIONS.get(simple(texte.substring(i, bout)));
                if (reaction != null) {                               // "Hm." : un vrai murmure, pas des syllabes
                    reagir(reaction, 0.9f);
                    sautJusqua = bout;
                    depuis = -0.15f;
                }
            }
            if (i < sautJusqua || !lettre || !voyelle(c) || (i > 0 && voyelle(texte.charAt(i - 1)))) {
                continue;
            }
            float intervalle = pasDeVoix / (vocal.debit() * emotion.debit());
            if (finDePhrase(i) == '…') {
                intervalle *= 1.2f;
            }
            if (depuis >= intervalle) {
                syllabe(i);
                depuis = Math.min(depuis - intervalle, intervalle * 0.5f);   // le rythme moyen reste juste, sans rafale
            }
        }
        vu = Math.max(vu, fin);
    }

    /** Le joueur a termine la replique ou ferme la boite : la voix s'arrete tout de suite. */
    void couper() {
        if (active) {
            audio.couper(Audio.Bus.VOIX);
        }
        active = false;
        vu = texte.length();
    }

    boolean active() {
        return active;
    }

    /** Une syllabe : sa hauteur suit la phrase (descente, question, exclamation), sa force aussi. */
    private void syllabe(int i) {
        int debut = debutDePhrase(i);
        int fin = finDePhraseIndex(i);
        float avance = fin > debut ? (i - debut) / (float) (fin - debut) : 0f;
        char ponctuation = finDePhrase(i);
        float hauteur = vocal.hauteur() * emotion.hauteur() * (1.05f - 0.1f * avance);
        float force = 0.6f * emotion.force();
        int lettresRestantes = 0;
        for (int k = i; k < fin; k++) {
            if (Character.isLetter(texte.charAt(k))) {
                lettresRestantes++;
            }
        }
        switch (ponctuation) {
            case '?' -> {
                if (lettresRestantes <= 5) {
                    hauteur *= 1.16f;                                  // la question remonte sur la fin
                }
            }
            case '!' -> {
                hauteur *= 1.06f;
                force *= 1.18f;
            }
            case '…' -> force *= 0.82f;
            default -> { }
        }
        if (motEnCapitales(i)) {
            force *= 1.25f;
        }
        float ecart;
        synchronized (hasard) {
            ecart = (hasard.nextFloat() * 2 - 1) * 0.045f * vocal.melodie() * emotion.melodie();
        }
        audio.jouer(Audio.Bus.VOIX, "v_" + vocal.banque(), Math.min(1f, force), pan, hauteur * (1 + ecart), 0f);
    }

    private void reagir(String reaction, float force) {
        String nom = "v_" + vocal.banque() + "_" + reaction;
        if (audio.existe(nom)) {
            audio.jouer(Audio.Bus.VOIX, nom, 0.75f * force * emotion.force(), pan, vocal.hauteur(), 0f);
        }
    }

    private int debutDePhrase(int i) {
        for (int k = i - 1; k >= 0; k--) {
            if (".!?…".indexOf(texte.charAt(k)) >= 0) {
                return k + 1;
            }
        }
        return 0;
    }

    private int finDePhraseIndex(int i) {
        for (int k = i; k < texte.length(); k++) {
            if (".!?…".indexOf(texte.charAt(k)) >= 0) {
                return k;
            }
        }
        return texte.length();
    }

    /** Ce qui termine la phrase en cours : '.', '!', '?', ou '…' pour des points de suspension. */
    private char finDePhrase(int i) {
        int k = finDePhraseIndex(i);
        if (k >= texte.length()) {
            return '.';
        }
        char c = texte.charAt(k);
        if (c == '.' && k + 1 < texte.length() && texte.charAt(k + 1) == '.') {
            return '…';
        }
        return c;
    }

    private boolean motEnCapitales(int i) {
        int a = i;
        while (a > 0 && Character.isLetter(texte.charAt(a - 1))) {
            a--;
        }
        int b = i;
        while (b < texte.length() && Character.isLetter(texte.charAt(b))) {
            b++;
        }
        String mot = texte.substring(a, b);
        return mot.length() >= 2 && mot.equals(mot.toUpperCase()) && !mot.equals(mot.toLowerCase());
    }

    private static boolean voyelle(char c) {
        return "aeiouy".indexOf(simple(String.valueOf(c)).charAt(0)) >= 0;
    }

    /** Sans accents, en minuscules : "Hé" devient "he". */
    static String simple(String mot) {
        String s = Normalizer.normalize(mot.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.isEmpty() ? " " : s;
    }
}
