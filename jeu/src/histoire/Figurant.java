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
    private float amplitude;
    private float vitesse;
    private boolean garde;
    private boolean miroir;

    public Figurant(String id, Lieu lieu, String image, String nom, String de, String a) {
        this.id = id;
        this.lieu = lieu;
        this.image = image;
        this.nom = nom;
        this.de = de;
        this.a = a;
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
