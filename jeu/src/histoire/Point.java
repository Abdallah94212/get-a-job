package histoire;

/** Un endroit du decor sur lequel on peut agir quand un objectif le demande : un sac a prendre, un lit ou dormir. */
public record Point(String id, Lieu lieu, String image, int hauteur, String invite) {
    /** hauteur : de combien l'image est posee au-dessus du sol (un objet sur un etabli). */
}
