package histoire;

/**
 * Une porte fermee seulement a certains moments : le rideau du garage, cadenasse la nuit du vol. Depuis cette carte,
 * vers cette destination, tant que la condition est vraie ; la phrase dit pourquoi.
 */
public record Fermeture(String depuis, String vers, String condition, String phrase) {
}
