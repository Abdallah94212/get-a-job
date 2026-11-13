package decor;

/** Une image posee dans le decor. La parallaxe dit a quelle vitesse elle defile : moins de 1, elle est au loin. */
public record Element(Texture texture, float x, float y, float parallaxe, int drapeaux, float lumiere) {
    public static final int ADDITIF = 1;
    public static final int SCINTILLE = 2;
    public static final int CLIGNOTE = 4;
    public static final int MIROIR = 8;

    public boolean a(int drapeau) {
        return (drapeaux & drapeau) != 0;
    }

    /** Hauteur du pied de l'image : sert a savoir qui passe devant qui. */
    public float base() {
        return y + texture.hauteur();
    }
}
