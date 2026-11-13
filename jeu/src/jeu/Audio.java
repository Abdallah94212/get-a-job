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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Le son du jeu : un petit mixeur logiciel qui melange en continu la musique, les ambiances en boucle et les effets,
 * chacun avec son volume reglable. Tout passe par une seule sortie stereo ; les fondus evitent les coupures seches.
 * Les sons sont lus dans ressources/sons (WAV ou AU mu-law, mono, 22 050 Hz) et gardes en memoire.
 * Sans carte son, ou pendant les essais, il ne joue rien mais garde trace de ce qu'il aurait joue.
 */
public final class Audio {
    /** Chaque son appartient a un canal, qui a son propre reglage de volume. */
    enum Canal {
        MUSIQUE, AMBIANCE, EFFETS
    }

    static final int FREQUENCE = 22050;
    private static final int BLOC = 512;

    /** Un son en train de jouer. Ses valeurs cibles sont posees par le jeu ; le mixeur s'en rapproche doucement. */
    private static final class Voix {
        final String nom;
        final Canal canal;
        final boolean boucle;
        double position;
        float gain;
        volatile float gainCible;
        volatile float pan;
        volatile float vitesse;
        float fondu;
        volatile boolean finie;

        Voix(String nom, Canal canal, boolean boucle, float gain, float pan, float vitesse, float fondu) {
            this.nom = nom;
            this.canal = canal;
            this.boucle = boucle;
            this.gain = fondu > 0 ? 0 : gain;
            this.gainCible = gain;
            this.pan = pan;
            this.vitesse = vitesse;
            this.fondu = fondu;
        }
    }

    private final Path dossier;
    private final Reglages reglages;
    private final Map<String, short[]> sons = new ConcurrentHashMap<>();
    private final List<Voix> voix = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Voix> boucles = new HashMap<>();
    private final List<String> journal = Collections.synchronizedList(new ArrayList<>());
    private Voix musique;
    private String theme;
    private boolean actif;

    /** muet : ne joue rien et ne lit aucun fichier (essais) ; garde seulement la trace de ce qui est demande. */
    public Audio(Path dossier, Reglages reglages, boolean muet) {
        this.dossier = dossier;
        this.reglages = reglages;
        if (!muet) {
            demarrer();
        }
    }

    /** Vrai si le fichier de ce son existe. */
    boolean existe(String nom) {
        return fichier(nom) != null;
    }

    private Path fichier(String nom) {
        for (String extension : new String[] {".wav", ".au"}) {
            Path f = dossier.resolve(nom + extension);
            if (Files.isRegularFile(f)) {
                return f;
            }
        }
        return null;
    }

    private short[] charger(String nom) {
        return sons.computeIfAbsent(nom, n -> {
            Path f = fichier(n);
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

    private void demarrer() {
        try {
            AudioFormat format = new AudioFormat(FREQUENCE, 16, 2, true, false);
            SourceDataLine ligne = AudioSystem.getSourceDataLine(format);
            ligne.open(format, BLOC * 4 * 4);
            ligne.start();
            actif = true;
            Thread mixeur = new Thread(() -> melanger(ligne), "mixeur");
            mixeur.setDaemon(true);
            mixeur.start();
            Thread lecture = new Thread(this::toutCharger, "chargement-sons");
            lecture.setDaemon(true);
            lecture.setPriority(Thread.MIN_PRIORITY);
            lecture.start();
        } catch (Exception erreur) {
            System.err.println("Pas de son : " + erreur.getMessage());
        }
    }

    /** Lit tous les sons a l'avance, sans bloquer le jeu : les plus courts d'abord. */
    private void toutCharger() {
        try (Stream<Path> fichiers = Files.list(dossier)) {
            fichiers.filter(f -> f.toString().endsWith(".wav") || f.toString().endsWith(".au"))
                    .sorted((a, b) -> Long.compare(a.toFile().length(), b.toFile().length()))
                    .forEach(f -> charger(f.getFileName().toString().replaceFirst("\\.(wav|au)$", "")));
        } catch (IOException erreur) {
            System.err.println("Sons illisibles : " + erreur.getMessage());
        }
    }

    // ------------------------------------------------------------------ ce que le jeu demande

    /** Joue un effet une fois, au centre. */
    void jouer(String nom) {
        jouer(nom, 1f, 0f, 1f);
    }

    /** Joue un effet une fois. pan va de -1 (a gauche) a 1 (a droite) ; vitesse change aussi la hauteur. */
    void jouer(String nom, float gain, float pan, float vitesse) {
        journal.add(nom);
        if (actif) {
            voix.add(new Voix(nom, Canal.EFFETS, false, gain, pan, vitesse, 0));
        }
    }

    /** Change de musique en fondu enchaine. null ou "silence" : la musique s'eteint doucement. */
    void musique(String nouveau) {
        String voulu = nouveau == null || nouveau.equals("silence") ? null : nouveau;
        if (java.util.Objects.equals(voulu, theme)) {
            return;
        }
        theme = voulu;
        journal.add("musique:" + (voulu == null ? "silence" : voulu));
        if (!actif) {
            return;
        }
        if (musique != null) {
            musique.gainCible = 0;
            musique.fondu = 1.6f;
        }
        musique = voulu == null ? null : new Voix(voulu, Canal.MUSIQUE, true, 1f, 0f, 1f, 2.2f);
        if (musique != null) {
            voix.add(musique);
        }
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
                    entree.getValue().fondu = 1.2f;
                    boucles.remove(entree.getKey());
                }
            }
            for (Map.Entry<String, float[]> c : couches.entrySet()) {
                float[] v = c.getValue();
                Voix existante = boucles.get(c.getKey());
                if (existante == null) {
                    Canal canal = c.getKey().startsWith("scooter") ? Canal.EFFETS : Canal.AMBIANCE;
                    existante = new Voix(c.getKey(), canal, true, v[0], v[1], v[2], 1.2f);
                    boucles.put(c.getKey(), existante);
                    if (actif) {
                        voix.add(existante);
                    }
                }
                existante.gainCible = v[0];
                existante.pan = v[1];
                existante.vitesse = v[2];
                existante.fondu = 0.35f;
            }
        }
    }

    /** Les boucles d'ambiance demandees en ce moment, avec leur gain. */
    Map<String, Float> ambiancesCourantes() {
        Map<String, Float> courantes = new LinkedHashMap<>();
        synchronized (boucles) {
            boucles.forEach((nom, v) -> courantes.put(nom, v.gainCible));
        }
        return courantes;
    }

    /** Tout ce qui a ete demande depuis le debut, dans l'ordre : pour verifier que le son suit le jeu. */
    List<String> journal() {
        synchronized (journal) {
            return new ArrayList<>(journal);
        }
    }

    // ------------------------------------------------------------------ le melange, dans son propre fil

    private float volume(Canal canal) {
        int reglage = switch (canal) {
            case MUSIQUE -> reglages.musique;
            case AMBIANCE -> reglages.ambiance;
            case EFFETS -> reglages.effets;
        };
        float v = reglage / 10f;
        return v * v;
    }

    private void melanger(SourceDataLine ligne) {
        float[] gauche = new float[BLOC];
        float[] droite = new float[BLOC];
        byte[] sortie = new byte[BLOC * 4];
        float pas = 1f / FREQUENCE;
        while (true) {
            java.util.Arrays.fill(gauche, 0);
            java.util.Arrays.fill(droite, 0);
            List<Voix> enCours;
            synchronized (voix) {
                enCours = new ArrayList<>(voix);
            }
            for (Voix v : enCours) {
                short[] donnees = sons.get(v.nom);
                if (donnees == null) {
                    if (!v.boucle) {
                        donnees = charger(v.nom);
                    } else {
                        continue;
                    }
                }
                if (donnees.length == 0) {
                    v.finie = true;
                    continue;
                }
                float volume = volume(v.canal);
                float angle = (Math.max(-1f, Math.min(1f, v.pan)) + 1) * (float) Math.PI / 4;
                float g = (float) Math.cos(angle) * 1.41f;
                float d = (float) Math.sin(angle) * 1.41f;
                float vitesse = v.vitesse;
                float cible = v.gainCible;
                float rampe = v.fondu > 0 ? pas / v.fondu : 1f;
                for (int i = 0; i < BLOC; i++) {
                    if (v.gain < cible) {
                        v.gain = Math.min(cible, v.gain + rampe);
                    } else if (v.gain > cible) {
                        v.gain = Math.max(cible, v.gain - rampe);
                    }
                    int j = (int) v.position;
                    if (j >= donnees.length - 1) {
                        if (!v.boucle) {
                            v.finie = true;
                            break;
                        }
                        v.position -= donnees.length - 1;
                        j = (int) v.position;
                    }
                    float f = (float) (v.position - j);
                    float e = (donnees[j] * (1 - f) + donnees[j + 1] * f) / 32768f * v.gain * volume;
                    gauche[i] += e * g;
                    droite[i] += e * d;
                    v.position += vitesse;
                }
                if (v.gain <= 0 && cible <= 0 && v.fondu > 0) {
                    v.finie = true;
                }
            }
            voix.removeIf(v -> v.finie);
            for (int i = 0; i < BLOC; i++) {
                int l = Math.round((float) Math.tanh(gauche[i] * 0.9f) * 32000);
                int r = Math.round((float) Math.tanh(droite[i] * 0.9f) * 32000);
                sortie[i * 4] = (byte) l;
                sortie[i * 4 + 1] = (byte) (l >> 8);
                sortie[i * 4 + 2] = (byte) r;
                sortie[i * 4 + 3] = (byte) (r >> 8);
            }
            ligne.write(sortie, 0, sortie.length);
        }
    }
}
