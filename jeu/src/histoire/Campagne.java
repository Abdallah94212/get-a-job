package histoire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Toute l'histoire jouable : les chapitres, les personnages, les lieux fermes et les cinematiques. */
public final class Campagne {
    /** Nom de la periode qui suit la derniere mission : pour les personnages qui restent une fois l'histoire jouee. */
    public static final String FIN = "fin";

    /** Celui qui parle dans un dialogue : le nom affiche et l'image de son portrait. */
    public record Voix(String nom, String portrait) {
    }

    private final Map<String, Voix> voix = new LinkedHashMap<>();
    private final Map<String, Float> timbres = new LinkedHashMap<>();
    private final List<Chapitre> chapitres = new ArrayList<>();
    private final List<Mission> missions = new ArrayList<>();
    private final Map<String, Cinematique> cinematiques = new LinkedHashMap<>();
    private final Map<String, Figurant> figurants = new LinkedHashMap<>();
    private final Map<String, Point> points = new LinkedHashMap<>();
    private final Map<String, String> verrous = new LinkedHashMap<>();
    private final Map<String, String> nomsDesLieux = new LinkedHashMap<>();
    private String carteDepart = "";
    private float xDepart;
    private float yDepart;

    public List<Chapitre> chapitres() {
        return chapitres;
    }

    /** Toutes les missions, dans l'ordre ou on les joue. */
    public List<Mission> missions() {
        return missions;
    }

    public Mission mission(String id) {
        return missions.stream().filter(m -> m.id().equals(id)).findFirst().orElse(null);
    }

    /** Place de la mission dans la campagne, ou -1 si elle n'existe pas. "fin" designe ce qui vient apres la derniere. */
    public int rang(String id) {
        if (FIN.equals(id)) {
            return missions.size();
        }
        for (int i = 0; i < missions.size(); i++) {
            if (missions.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    public Chapitre chapitreDe(Mission mission) {
        return chapitres.stream().filter(c -> c.missions().contains(mission)).findFirst().orElse(null);
    }

    public Cinematique cinematique(String id) {
        return id == null ? null : cinematiques.get(id);
    }

    public Map<String, Cinematique> cinematiques() {
        return cinematiques;
    }

    public Map<String, Figurant> figurants() {
        return figurants;
    }

    /** La voix d'un personnage : la sienne s'il est declare, sinon son identifiant en guise de nom. */
    public Voix voix(String id) {
        Voix declaree = voix.get(id);
        if (declaree != null) {
            return declaree;
        }
        Figurant figurant = figurants.get(id);
        if (figurant != null) {
            String image = figurant.image();
            return new Voix(figurant.nom(), "buste_" + (image.startsWith("p_") ? image.substring(2) : image));
        }
        return new Voix(id.toUpperCase(), null);
    }

    /** La hauteur de voix d'un personnage pour le petit son des dialogues : 1 est la voix moyenne. */
    public float timbre(String id) {
        return timbres.getOrDefault(id, 1f);
    }

    void accorder(String id, float hauteur) {
        timbres.put(id, hauteur);
    }

    void donnerVoix(String id, String nom, String portrait) {
        voix.put(id, new Voix(nom, portrait));
    }

    public Map<String, Point> points() {
        return points;
    }

    /** Les lieux fermes au debut du jeu, avec la phrase qui explique pourquoi on n'y entre pas encore. */
    public Map<String, String> verrous() {
        return verrous;
    }

    /** Le nom d'un lieu ferme tel qu'on l'annonce au joueur quand il s'ouvre. */
    public String nomDuLieu(String carte) {
        return nomsDesLieux.getOrDefault(carte, carte.toUpperCase());
    }

    void fermer(String carte, String nom, String raison) {
        verrous.put(carte, raison);
        nomsDesLieux.put(carte, nom);
    }

    public String carteDepart() {
        return carteDepart;
    }

    public float xDepart() {
        return xDepart;
    }

    public float yDepart() {
        return yDepart;
    }

    void ajouter(Chapitre chapitre) {
        chapitres.add(chapitre);
    }

    void ajouter(Chapitre chapitre, Mission mission) {
        chapitre.missions().add(mission);
        missions.add(mission);
    }

    void partirDe(String carte, float x, float y) {
        carteDepart = carte;
        xDepart = x;
        yDepart = y;
    }

    /**
     * Cherche ce qui rendrait la campagne injouable : une cible qui n'existe pas, une cinematique introuvable,
     * un lieu ferme que rien n'ouvre avant qu'on doive y aller. Renvoie la liste des defauts, vide si tout va bien.
     */
    public List<String> defauts() {
        List<String> defauts = new ArrayList<>();
        if (missions.isEmpty()) {
            defauts.add("aucune mission");
        }
        for (Cinematique cinematique : cinematiques.values()) {
            for (String perdu : cinematique.sautsPerdus()) {
                defauts.add("cinematique " + cinematique.id() + " : etiquette inconnue " + perdu);
            }
        }
        List<String> ouverts = new ArrayList<>();
        for (Mission mission : missions) {
            ouverts.addAll(mission.ouvre());
            if (mission.etapes().isEmpty()) {
                defauts.add(mission.id() + " : aucun objectif principal");
            }
            for (String id : new String[] {mission.cinematiqueDebut(), mission.cinematiqueFin()}) {
                if (id != null && !cinematiques.containsKey(id)) {
                    defauts.add(mission.id() + " : cinematique inconnue " + id);
                }
            }
            List<Objectif> tous = new ArrayList<>(mission.etapes());
            tous.addAll(mission.facultatifs());
            for (Objectif objectif : tous) {
                String carte = carteDe(objectif);
                if (carte == null) {
                    defauts.add(mission.id() + " : cible inconnue " + objectif.cible());
                } else if (verrous.containsKey(carte) && !ouverts.contains(carte)) {
                    defauts.add(mission.id() + " : " + objectif.cible() + " est dans un lieu encore ferme, " + carte);
                }
                if (objectif.suite() != null && !cinematiques.containsKey(objectif.suite())) {
                    defauts.add(mission.id() + " : cinematique inconnue " + objectif.suite());
                }
                if (objectif.genre() == Objectif.Genre.PARLER && !presentPendant(objectif.cible(), mission)) {
                    defauts.add(mission.id() + " : " + objectif.cible() + " n'est pas la pendant la mission");
                }
            }
        }
        return defauts;
    }

    /** La carte ou se trouve la cible d'un objectif, ou null si la cible n'existe pas. */
    public String carteDe(Objectif objectif) {
        return switch (objectif.genre()) {
            case PARLER -> figurants.containsKey(objectif.cible()) ? figurants.get(objectif.cible()).lieu().carte() : null;
            case AGIR, ATTEINDRE -> points.containsKey(objectif.cible()) ? points.get(objectif.cible()).lieu().carte() : null;
            case ALLER -> objectif.cible();
        };
    }

    private boolean presentPendant(String figurant, Mission mission) {
        Figurant f = figurants.get(figurant);
        int rang = rang(mission.id());
        return f != null && rang >= rang(f.de()) && rang <= rang(f.a());
    }
}
