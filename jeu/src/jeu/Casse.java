package jeu;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Les textes ecrits tout en capitales dans la campagne (objectifs, recits, notifications) se lisent mal en longueur.
 * On les remet en minuscules, avec une majuscule en debut de phrase, et les noms propres retrouvent la leur.
 * Les titres, les noms et les etiquettes courtes gardent leurs capitales : on n'y touche pas.
 */
final class Casse {
    /** Les noms propres de l'histoire, ecrits comme il faut ; les plus longs d'abord ("Old Geto" avant "Geto"). */
    private static final List<String> NOMS = List.of(
            "Krousty Corp", "Maître Poulet", "Old Geto", "Tata Lili", "Papy Bocal", "Cell Perfect", "Kremlin-Bicêtre",
            "Place d'Italie", "Porte d'Italie", "La Courneuve", "Fontainebleau", "Youngssef", "Stalingrad", "Villejuif",
            "Hagakure", "Piccolo", "Bidoche", "Krousty", "Crimée", "Namek", "Savon", "Ticket", "Titan", "Uranus", "Paris",
            "Omar", "Gero", "Geto", "Lili", "Soul", "Baba", "RS6", "SDF", "TV");
    private static final Pattern PHRASE = Pattern.compile("(^|[.!?…]\\s+|«\\s*)(\\p{L})");
    private static final Pattern TOUCHE = Pattern.compile("(^|\\s)([epf])(\\s*:)");

    private Casse() {
    }

    /** Remet un texte tout en capitales en casse de phrase. Un texte qui a deja des minuscules est laisse tel quel. */
    static String phrase(String texte) {
        if (texte == null || texte.isEmpty() || !texte.equals(texte.toUpperCase(Locale.FRENCH)) || texte.chars().noneMatch(Character::isLetter)) {
            return texte;
        }
        String t = texte.toLowerCase(Locale.FRENCH);
        for (String nom : NOMS) {
            t = Pattern.compile("(?<![\\p{L}])" + Pattern.quote(nom.toLowerCase(Locale.FRENCH)) + "(?![\\p{L}])")
                    .matcher(t).replaceAll(Matcher.quoteReplacement(nom));
        }
        Matcher m = PHRASE.matcher(t);
        StringBuilder sortie = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sortie, Matcher.quoteReplacement(m.group(1) + m.group(2).toUpperCase(Locale.FRENCH)));
        }
        m.appendTail(sortie);
        Matcher k = TOUCHE.matcher(sortie.toString());                 // les touches : "P : telephone"
        sortie = new StringBuilder();
        while (k.find()) {
            k.appendReplacement(sortie, Matcher.quoteReplacement(k.group(1) + k.group(2).toUpperCase(Locale.FRENCH) + k.group(3)));
        }
        k.appendTail(sortie);
        return sortie.toString();
    }
}
