package histoire;

/**
 * Un pas d'une cinematique : un decor a montrer, une replique, un personnage qui marche, un effet...
 * Le sens des champs depend du genre ; les nombres sont gardes en texte car ils peuvent citer un repere ("@banc+20").
 */
public record Plan(Genre genre, String[] mots, String texte) {
    public enum Genre {
        CARTE, NOIR, RECIT, DIT, ACTEUR, RETIRE, MARCHE, CAMERA, ATTEND, EFFET, TITRE, CARTON, JOUEUR,
        CHOIX, ETIQUETTE, SAUT, SI, DRAPEAU, SON, MUSIQUE, AMBIANCE, METEO, GROS_PLAN, SMS, ANNONCE, POSE, REGARDE
    }

    /** Les options d'un choix : le texte affiche, puis l'etiquette ou sauter. */
    public record Option(String texte, String etiquette) {
    }

    public java.util.List<Option> options() {
        java.util.List<Option> options = new java.util.ArrayList<>();
        for (String morceau : texte.split("\\|")) {
            String[] parts = morceau.split("->");
            options.add(new Option(parts[0].trim(), parts.length > 1 ? parts[1].trim() : ""));
        }
        return options;
    }

    public String mot(int rang) {
        return rang < mots.length ? mots[rang] : "";
    }
}
