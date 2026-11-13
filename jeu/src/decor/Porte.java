package decor;

/**
 * Un passage vers une autre carte. Automatique, il se declenche en marchant dessus (les bouts de l'avenue).
 * Sinon il faut se tenir devant, pres du mur, et appuyer sur E (les portes des commerces).
 */
public record Porte(float x1, float x2, float yMax, String cible, float arriveeX, float arriveeY, boolean automatique,
             Texture etiquette) {
    public boolean devant(float x, float y) {
        return x >= x1 && x <= x2 && y <= yMax;
    }
}
