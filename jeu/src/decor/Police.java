package decor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * La police en pixels du jeu : une planche de lettres dessinee par designs/decor/outils.py, et l'ordre des lettres.
 * Tout s'ecrit en capitales, comme sur les enseignes de l'avenue ; la grande police garde les accents.
 */
public final class Police {
    private final Texture planche;
    private final String lettres;
    private final int pas;

    private Police(Texture planche, String lettres) {
        this.planche = planche;
        this.lettres = lettres;
        this.pas = planche.largeur() / lettres.length();
    }

    /** taille : 5 pour les textes, 3 pour les petites mentions. */
    public static Police charger(Path dossier, int taille) throws IOException {
        return new Police(Texture.charger(dossier.resolve("police" + taille + ".png")),
                Files.readString(dossier.resolve("police" + taille + ".txt"), StandardCharsets.UTF_8));
    }

    /** Hauteur d'une ligne, interligne compris. */
    public int ligne() {
        return planche.hauteur() + 1;
    }

    public int largeur(String texte, int echelle) {
        return Math.max(0, propre(texte).length() * pas - 1) * echelle;
    }

    /** Ramene un texte aux lettres que la police connait : capitales, guillemets droits, accents retires s'il le faut. */
    public String propre(String texte) {
        String t = texte.toUpperCase(Locale.FRENCH).replace('’', '\'').replace('«', '"').replace('»', '"')
                .replace("…", "...").replace("Œ", "OE").replace("Æ", "AE").replace(' ', ' ');
        StringBuilder sortie = new StringBuilder(t.length());
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (lettres.indexOf(c) < 0) {
                String nu = Normalizer.normalize(String.valueOf(c), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
                c = nu.length() == 1 && lettres.indexOf(nu.charAt(0)) >= 0 ? nu.charAt(0) : ' ';
            }
            sortie.append(c);
        }
        return sortie.toString();
    }

    /** Ecrit le texte, son coin haut gauche en (x, y). Renvoie la largeur ecrite. */
    public int ecrire(Rendu rendu, String texte, int x, int y, int rvb, int echelle) {
        String t = propre(texte);
        for (int n = 0; n < t.length(); n++) {
            int rang = lettres.indexOf(t.charAt(n));
            for (int j = 0; j < planche.hauteur(); j++) {
                for (int i = 0; i < pas - 1; i++) {
                    if ((planche.pixels()[j * planche.largeur() + rang * pas + i] >>> 24) < 128) {
                        continue;
                    }
                    for (int a = 0; a < echelle; a++) {
                        for (int b = 0; b < echelle; b++) {
                            rendu.point(x + (n * pas + i) * echelle + a, y + j * echelle + b, rvb);
                        }
                    }
                }
            }
        }
        return largeur(texte, echelle);
    }

    public void ecrireCentre(Rendu rendu, String texte, int centreX, int y, int rvb, int echelle) {
        ecrire(rendu, texte, centreX - largeur(texte, echelle) / 2, y, rvb, echelle);
    }

    /** Coupe un texte en lignes qui tiennent dans la largeur donnee, sans couper les mots. */
    public List<String> couper(String texte, int largeurMax) {
        List<String> lignes = new ArrayList<>();
        StringBuilder ligne = new StringBuilder();
        for (String mot : texte.split(" ")) {
            if (ligne.length() > 0 && largeur(ligne + " " + mot, 1) > largeurMax) {
                lignes.add(ligne.toString());
                ligne.setLength(0);
            }
            if (ligne.length() > 0) {
                ligne.append(' ');
            }
            ligne.append(mot);
        }
        if (ligne.length() > 0) {
            lignes.add(ligne.toString());
        }
        return lignes;
    }
}
