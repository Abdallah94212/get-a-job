package histoire;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Lit la campagne dans un fichier texte, une instruction par ligne : "mot arguments | texte | texte".
 * Le fichier decrit d'abord le quartier (lieux fermes, personnages, points), puis les chapitres et leurs missions,
 * puis les cinematiques. Voir ressources/histoire/campagne.txt, qui explique chaque instruction.
 */
public final class LecteurCampagne {
    private final Campagne campagne = new Campagne();
    private Chapitre chapitre;
    private Mission mission;
    private Figurant figurant;
    private Cinematique cinematique;
    private boolean dansCinematiques;
    private int numero;

    private LecteurCampagne() {
    }

    public static Campagne lire(Path fichier) throws IOException {
        return lire(Files.readAllLines(fichier, StandardCharsets.UTF_8));
    }

    public static Campagne lire(List<String> lignes) throws IOException {
        LecteurCampagne lecteur = new LecteurCampagne();
        for (String ligne : lignes) {
            lecteur.numero++;
            String propre = ligne.strip();
            if (propre.isEmpty() || propre.startsWith("#")) {
                continue;
            }
            try {
                lecteur.instruction(propre);
            } catch (RuntimeException erreur) {
                throw new IOException("Campagne, ligne " + lecteur.numero + " : " + erreur.getMessage() + " (" + propre + ")", erreur);
            }
        }
        List<String> defauts = lecteur.campagne.defauts();
        if (!defauts.isEmpty()) {
            throw new IOException("Campagne injouable : " + String.join(" ; ", defauts));
        }
        return lecteur.campagne;
    }

    private void instruction(String ligne) {
        String[] champs = ligne.split("\\s*\\|\\s*", -1);
        String[] mots = champs[0].trim().split("\\s+");
        String[] args = Arrays.copyOfRange(mots, 1, mots.length);
        String texte = champs.length > 1 ? champs[1] : "";
        switch (mots[0]) {
            case "depart" -> campagne.partirDe(args[0], Float.parseFloat(args[1]), Float.parseFloat(args[2]));
            case "verrou" -> campagne.fermer(args[0], champs[1], champs[2]);
            case "figurant" -> {
                String[] periode = champs[4].trim().split("\\s+");
                figurant = new Figurant(args[0], lieu(champs[1]), champs[2], champs[3], periode[0], periode[periode.length - 1]);
                campagne.figurants().put(args[0], figurant);
            }
            case "bavarde" -> exiger(figurant, "figurant").repliques().add(new Replique(String.join(" ", args), texte));
            case "condition" -> exiger(figurant, "figurant").conditionner(String.join(" ", args));
            case "service" -> exiger(figurant, "figurant").proposer(args[0]);
            case "tourne" -> exiger(figurant, "figurant").tourner();
            case "routine" -> exiger(figurant, "figurant").habituer(args[0], args.length > 1 ? Float.parseFloat(args[1]) : 0f);
            case "membre" -> exiger(figurant, "figurant").rattacher(args[0]);
            case "caractere" -> exiger(figurant, "figurant").temperer(args[0]);
            case "encart" -> {                                     // encart <id> <genre> | <TITRE> | <image> | ligne | ligne...
                if (champs.length < 3) {                           // dans une scene, "encart id" : on le montre
                    exiger(cinematique, "cinematique").plans().add(new Plan(Plan.Genre.ENCART, args, ""));
                    return;
                }
                campagne.encarts().put(args[0], new Encart(args[0], args.length > 1 ? args[1].replace('_', ' ') : "À SAVOIR",
                        champs[1].trim(), champs[2].trim(), Arrays.stream(champs, 3, champs.length).map(String::trim).toList()));
            }
            case "ferme" -> campagne.fermetures().add(new Fermeture(args[0], args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)),
                    texte));
            case "vocal" -> campagne.donnerVocal(args[0], new Campagne.Vocal(args[1], args.length > 2 ? Float.parseFloat(args[2]) : 1f,
                    args.length > 3 ? Float.parseFloat(args[3]) : 1f, args.length > 4 ? Float.parseFloat(args[4]) : 1f));
            case "patrouille", "garde" -> exiger(figurant, "figurant")
                    .patrouiller(Float.parseFloat(args[0]), Float.parseFloat(args[1]), mots[0].equals("garde"));
            case "voix" -> campagne.donnerVoix(args[0], champs[1], champs[2]);
            case "point" -> {
                String[] image = champs[2].split(":");
                campagne.points().put(args[0], new Point(args[0], lieu(champs[1]), image[0].equals("-") ? null : image[0],
                        image.length > 1 ? Integer.parseInt(image[1]) : 0, champs[3]));
            }
            case "repli" -> exiger(mission, "mission").seReplier(lieu(String.join(" ", args)));
            case "chapitre" -> {
                chapitre = new Chapitre(args[0], champs[1], champs[2], champs[3]);
                campagne.ajouter(chapitre);
            }
            case "mission" -> {
                mission = new Mission(args[0], champs[1], champs[2], champs[3]);
                campagne.ajouter(exiger(chapitre, "chapitre"), mission);
            }
            case "resume" -> exiger(mission, "mission").decrire(texte);
            case "ouvre" -> Arrays.stream(args).forEach(exiger(mission, "mission")::ouvrir);
            case "debut" -> exiger(mission, "mission").cinematiques(args[0], null);
            case "fin" -> exiger(mission, "mission").cinematiques(null, args[0]);
            case "chrono" -> exiger(mission, "mission").chronometrer(Integer.parseInt(args[0]));
            case "meteo" -> {
                if (dansCinematiques) {
                    exiger(cinematique, "cinematique").plans().add(new Plan(Plan.Genre.METEO, args, texte));
                } else {
                    exiger(mission, "mission").regler(args[0], null);
                }
            }
            case "musique" -> {
                if (cinematique != null && mission == null || dansCinematiques) {
                    exiger(cinematique, "cinematique").plans().add(new Plan(Plan.Genre.MUSIQUE, args, texte));
                } else {
                    exiger(mission, "mission").regler(null, args[0]);
                }
            }
            case "ellipse" -> exiger(mission, "mission").ellipser(args[0]);
            case "quand" -> exiger(mission, "mission").dater(Integer.parseInt(args[0]), minutes(args[1]));
            case "furtive" -> exiger(mission, "mission").rendreFurtive();
            case "objectif", "option" -> exiger(mission, "mission").ajouter(objectif(args, texte, mots[0].equals("option")));
            case "cinematique" -> {
                dansCinematiques = true;
                cinematique = new Cinematique(args[0]);
                campagne.cinematiques().put(args[0], cinematique);
            }
            default -> {
                Plan.Genre genre = Plan.Genre.valueOf(mots[0].toUpperCase());
                if (genre == Plan.Genre.DIT && args.length > 0 && args[0].contains(":")) {   // "dit geto:colere" : le ton
                    String[] ton = args[0].split(":", 2);
                    args = new String[] {ton[0], ton[1]};
                }
                String[] suite = champs.length > 2 ? Arrays.copyOfRange(champs, 1, champs.length) : null;
                exiger(cinematique, "cinematique").plans().add(new Plan(genre, args, suite == null ? texte : String.join("|", suite)));
            }
        }
    }

    /** "parler geto pression si +drapeau -autre musique:tension -> scene" : le genre, la cible, puis des options. */
    private static Objectif objectif(String[] args, String texte, boolean facultatif) {
        boolean pression = false;
        String suite = null;
        String musique = null;
        String objet = null;
        StringBuilder condition = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("pression")) {
                pression = true;
            } else if (args[i].equals("->")) {
                suite = args[++i];
            } else if (args[i].equals("si")) {
                while (i + 1 < args.length && !args[i + 1].equals("->")
                        && (args[i + 1].startsWith("+") || args[i + 1].startsWith("-"))) {
                    condition.append(' ').append(args[++i]);
                }
            } else if (args[i].startsWith("musique:")) {
                musique = args[i].substring(8);
            } else if (args[i].startsWith("avec:")) {
                objet = args[i].substring(5);
                if (!Inventaire.connu(objet)) {
                    throw new IllegalArgumentException("objet inconnu " + objet);
                }
            } else {
                throw new IllegalArgumentException("option inconnue " + args[i]);
            }
        }
        return new Objectif(Objectif.Genre.valueOf(args[0].toUpperCase()), args[1], texte, facultatif, pression, suite,
                condition.toString().trim(), musique, objet);
    }

    /** "22:00" ou "22h" : en minutes depuis minuit. */
    public static int minutes(String heure) {
        String[] m = heure.replace('h', ':').split(":");
        return Integer.parseInt(m[0]) * 60 + (m.length > 1 && !m[1].isEmpty() ? Integer.parseInt(m[1]) : 0);
    }

    /** "carte repere decalage hauteur" ; un tiret garde la hauteur du repere. */
    private static Lieu lieu(String champ) {
        String[] m = champ.trim().split("\\s+");
        float dx = m.length > 2 ? Float.parseFloat(m[2]) : 0f;
        float y = m.length > 3 && !m[3].equals("-") ? Float.parseFloat(m[3]) : Float.NaN;
        return new Lieu(m[0], m[1], dx, y);
    }

    private static <T> T exiger(T objet, String nom) {
        if (objet == null) {
            throw new IllegalStateException("instruction hors d'un bloc " + nom);
        }
        return objet;
    }
}
