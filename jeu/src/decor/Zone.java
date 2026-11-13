package decor;

/** Un lieu de l'histoire : quand le joueur passe devant, son nom s'affiche. */
public record Zone(String nom, float x1, float x2, Texture etiquette) {
    public boolean contient(float x) {
        return x >= x1 && x <= x2;
    }
}
