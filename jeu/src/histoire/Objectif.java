package histoire;

/**
 * Une chose a faire dans une mission. La cible est l'identifiant d'un personnage, d'un point du decor ou d'une carte.
 * Sous pression, l'objectif fait partie d'une sequence chronometree ou furtive : si elle echoue, on la reprend a son debut.
 * condition : des drapeaux a avoir (+nom) ou a ne pas avoir (-nom) pour que l'objectif compte ; vide s'il n'y en a pas.
 * musique : le theme a jouer tant que cet objectif est en cours, ou null pour garder celui de la mission.
 * objet : ce qu'il faut avoir sur soi pour que l'objectif compte (une livraison : le sac), ou null.
 * argent : la somme qu'il faut avoir sur soi pour que l'objectif compte (rembourser quelqu'un), ou 0. La scene qui suit
 * fait la vraie transaction ; tant que la somme manque, l'objectif ne compte pas et on ne peut pas passer a la suite.
 */
public record Objectif(Genre genre, String cible, String texte, boolean facultatif, boolean sousPression, String suite,
                       String condition, String musique, String objet, int argent) {
    /**
     * PARLER a un personnage, AGIR sur un point du decor (touche E), ALLER dans un lieu (il suffit d'y entrer),
     * ATTEINDRE un point du decor (il suffit de s'en approcher : c'est ainsi qu'une scene se declenche en chemin).
     */
    public enum Genre {
        PARLER, AGIR, ALLER, ATTEINDRE,
        /** DELEGUER une operation (cible : son identifiant, ou * pour n'importe laquelle) depuis le telephone. */
        DELEGUER,
        /** ENCAISSER le resultat d'une operation : il suffit qu'elle se termine (* : n'importe laquelle). */
        ENCAISSER
    }
}
