package histoire;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Une mission de la campagne : ce qu'elle raconte, ce qu'il faut y faire, et ce qu'elle ouvre dans le quartier. */
public final class Mission {
    private final String id;
    private final String titre;
    private final String lieu;
    private final String personnages;
    private final List<Objectif> etapes = new ArrayList<>();
    private final List<Objectif> facultatifs = new ArrayList<>();
    private final Set<String> ouvre = new LinkedHashSet<>();
    private String description = "";
    private String cinematiqueDebut;
    private String cinematiqueFin;
    private int chrono;
    private boolean furtive;
    private Lieu repli;
    private String meteo = "sec";
    private String musique;

    public Mission(String id, String titre, String lieu, String personnages) {
        this.id = id;
        this.titre = titre;
        this.lieu = lieu;
        this.personnages = personnages;
    }

    public String id() {
        return id;
    }

    public String titre() {
        return titre;
    }

    /** La zone du quartier ou la mission se joue. */
    public String lieu() {
        return lieu;
    }

    public String personnages() {
        return personnages;
    }

    public String description() {
        return description;
    }

    /** Les objectifs principaux, a faire dans l'ordre. */
    public List<Objectif> etapes() {
        return etapes;
    }

    /** Les objectifs secondaires : on peut les faire a tout moment de la mission, ou jamais. */
    public List<Objectif> facultatifs() {
        return facultatifs;
    }

    /** Les lieux fermes que cette mission ouvre des qu'elle commence. */
    public Set<String> ouvre() {
        return ouvre;
    }

    public String cinematiqueDebut() {
        return cinematiqueDebut;
    }

    public String cinematiqueFin() {
        return cinematiqueFin;
    }

    /** Duree en secondes accordee aux objectifs sous pression, ou 0 s'il n'y a pas de chrono. */
    public int chrono() {
        return chrono;
    }

    /** Vrai si se faire voir par un garde fait echouer les objectifs sous pression. */
    public boolean furtive() {
        return furtive;
    }

    /** Ou le joueur est ramene quand la sequence sous pression echoue, ou null s'il reste sur place. */
    public Lieu repli() {
        return repli;
    }

    /** "pluie" ou "sec" : le temps qu'il fait dehors pendant la mission. */
    public String meteo() {
        return meteo;
    }

    /** Le theme musical de la mission dans les rues, ou null pour celui du lieu. */
    public String musique() {
        return musique;
    }

    void regler(String quelMeteo, String quelleMusique) {
        meteo = quelMeteo != null ? quelMeteo : meteo;
        musique = quelleMusique != null ? quelleMusique : musique;
    }

    void seReplier(Lieu lieu) {
        repli = lieu;
    }

    void decrire(String texte) {
        description = texte;
    }

    void ajouter(Objectif objectif) {
        (objectif.facultatif() ? facultatifs : etapes).add(objectif);
    }

    void ouvrir(String lieuFerme) {
        ouvre.add(lieuFerme);
    }

    void cinematiques(String debut, String fin) {
        cinematiqueDebut = debut != null ? debut : cinematiqueDebut;
        cinematiqueFin = fin != null ? fin : cinematiqueFin;
    }

    void chronometrer(int secondes) {
        chrono = secondes;
    }

    void rendreFurtive() {
        furtive = true;
    }
}
