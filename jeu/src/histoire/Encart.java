package histoire;

import java.util.List;

/**
 * Un encart qui explique une mecanique ou un objet, au moment ou le joueur la decouvre : une illustration, un titre,
 * quelques lignes courtes. Le jeu se met en pause le temps de le lire ; chaque encart ne s'affiche qu'une fois.
 * genre : ce qu'il annonce (NOUVEL OBJET, A SAVOIR, COMBAT...), pour la hierarchie visuelle.
 */
public record Encart(String id, String genre, String titre, String image, List<String> lignes) {
}
