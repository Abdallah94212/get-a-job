package histoire;

/**
 * Un endroit d'une carte, donne par rapport a un repere du decor (la porte du Maitre Poulet, le banc...).
 * y vaut NaN quand on garde la hauteur du repere.
 */
public record Lieu(String carte, String repere, float dx, float y) {
}
