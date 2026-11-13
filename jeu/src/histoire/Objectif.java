package histoire;

/**
 * Une chose a faire dans une mission. La cible est l'identifiant d'un personnage, d'un point du decor ou d'une carte.
 * Sous pression, l'objectif fait partie d'une sequence chronometree ou furtive : si elle echoue, on la reprend a son debut.
 * condition : des drapeaux a avoir (+nom) ou a ne pas avoir (-nom) pour que l'objectif compte ; vide s'il n'y en a pas.
 * musique : le theme a jouer tant que cet objectif est en cours, ou null pour garder celui de la mission.
 */
public record Objectif(Genre genre, String cible, String texte, boolean facultatif, boolean sousPression, String suite,
                       String condition, String musique) {
    /**
     * PARLER a un personnage, AGIR sur un point du decor (touche E), ALLER dans un lieu (il suffit d'y entrer),
     * ATTEINDRE un point du decor (il suffit de s'en approcher : c'est ainsi qu'une scene se declenche en chemin).
     */
    public enum Genre {
        PARLER, AGIR, ALLER, ATTEINDRE
    }
}
