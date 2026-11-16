package histoire;

import java.util.ArrayList;
import java.util.List;

/**
 * Un personnage pose dans le decor. Il n'est la qu'entre deux missions de l'histoire (de, a), et seulement si sa
 * condition est remplie. Il a des choses a dire quand on lui parle sans raison, et il peut faire les cent pas
 * ou monter la garde.
 */
public final class Figurant {
    private final String id;
    private final Lieu lieu;
    private final String image;
    private final String nom;
    private final String de;
    private final String a;
    private final List<Replique> repliques = new ArrayList<>();
    private String condition = "";
    private String service;
    private float amplitude;
    private float vitesse;
    private boolean garde;
    private boolean miroir;
    private String routine = "poste";
    private float rayon;
    private String membre;
    private String caractere;

    /**
     * Ce qu'il fait quand personne ne lui parle : "poste" (il reste la, regarde autour de lui), "flane" (il va et vient
     * autour de sa place, s'arrete, repart), "assis" (il ne bouge pas, il suit des yeux qui passe), "comptoir" (derriere
     * un comptoir : quelques pas, pas plus).
     */
    public String routine() {
        return routine;
    }

    public float rayon() {
        return rayon;
    }

    public void habituer(String genre, float distance) {
        routine = genre;
        rayon = distance;
    }

    /** Le petit de l'equipe qu'il represente : absent du decor tant qu'il est parti sur une operation. */
    public String membre() {
        return membre;
    }

    public void rattacher(String id) {
        membre = id;
    }

    /**
     * Comment il reagit si on le bouscule ou le frappe : "fragile" (il fuit, il crie), "costaud" (il riposte),
     * "police" (elle intervient), "proche" (quelqu'un de l'histoire : il encaisse mal, il s'en souviendra). Par defaut,
     * deduit de son dessin.
     */
    public String caractere() {
        if (caractere != null) {
            return caractere;
        }
        return switch (image) {
            case "p_policier" -> "police";
            case "p_sportif", "p_mecano", "p_gars_geto", "p_frotteur", "p_client" -> "costaud";
            case "p_cliente", "p_vieux", "p_ecouteurs", "p_femme", "p_voyageur", "p_diallo", "p_haddad" -> "fragile";
            default -> "proche";
        };
    }

    public void temperer(String genre) {
        caractere = genre;
    }

    public Figurant(String id, Lieu lieu, String image, String nom, String de, String a) {
        this.id = id;
        this.lieu = lieu;
        this.image = image;
        this.nom = nom;
        this.de = de;
        this.a = a;
    }

    /** La scene qu'il joue quand on lui parle sans que l'histoire l'attende : il vend, il loue, il soigne. */
    public String service() {
        return service;
    }

    public void proposer(String scene) {
        service = scene;
    }

    public String id() {
        return id;
    }

    public Lieu lieu() {
        return lieu;
    }

    /** Nom de l'image du personnage ; son buste porte le nom "buste_" suivi de son identifiant d'image. */
    public String image() {
        return image;
    }

    public String nom() {
        return nom;
    }

    public String de() {
        return de;
    }

    public String a() {
        return a;
    }

    public String condition() {
        return condition;
    }

    public List<Replique> repliques() {
        return repliques;
    }

    public float amplitude() {
        return amplitude;
    }

    public float vitesse() {
        return vitesse;
    }

    public boolean garde() {
        return garde;
    }

    /** Vrai s'il regarde vers la gauche au repos. */
    public boolean miroir() {
        return miroir;
    }

    void patrouiller(float amplitudePas, float vitessePas, boolean monteLaGarde) {
        amplitude = amplitudePas;
        vitesse = vitessePas;
        garde = monteLaGarde;
    }

    void conditionner(String drapeaux) {
        condition = drapeaux;
    }

    void tourner() {
        miroir = true;
    }
}
