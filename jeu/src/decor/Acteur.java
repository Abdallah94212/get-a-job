package decor;

/**
 * Un personnage ou un objet mobile pose dans le decor : le heros, un passant, un sac a ramasser.
 * (x, y) est le point ou ses pieds touchent le sol ; c'est y qui decide qui passe devant qui.
 */
public final class Acteur {
    public final String id;
    public Texture texture;
    /** Le second dessin de la marche, ou null : on alterne les deux quand l'acteur marche. */
    public Texture pas;
    public float x;
    public float y;
    /** De combien l'image est soulevee au-dessus du sol : un objet pose sur un etabli. */
    public int hauteur;
    public boolean miroir;
    public boolean marche;
    public boolean ombre = true;

    public Acteur(String id, Texture texture, float x, float y) {
        this.id = id;
        this.texture = texture;
        this.x = x;
        this.y = y;
    }
}
