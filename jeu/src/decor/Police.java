package decor;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.FontRenderContext;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Les polices du jeu. Le decor est en pixels ; le texte, lui, est net : il est dessine en haute definition par-dessus
 * l'image (voir Rendu.texte), pour qu'on le lise sans effort.
 *
 * Deux familles, libres (SIL Open Font License), dans ressources/polices :
 * - Atkinson Hyperlegible, pour tout ce qu'on lit : dialogues, objectifs, telephone, notifications. Elle a ete
 *   dessinee pour etre lue sans fatigue : chaque lettre se distingue de ses voisines, meme petite.
 * - Barlow Condensed, pour ce qu'on repere d'un coup d'oeil : titres, noms, etiquettes, chiffres. Une signaletique
 *   de rue, serree, qui va avec les enseignes de l'avenue.
 *
 * Les mesures restent en pixels du jeu (480 x 270), pour que les mises en page ne changent pas de logique.
 */
public final class Police {
    private static final FontRenderContext MESURE = new FontRenderContext(null, true, true);
    private static final Map<Path, Font[]> FAMILLES = new HashMap<>();

    private final Font corps;
    private final Font titre;
    /** La taille du corps, en pixels du jeu ; la hauteur des capitales en decoule. */
    private final float taille;
    private final float tailleTitre;
    private final int interligne;

    private Police(Font corps, Font titre, float taille, float tailleTitre, int interligne) {
        this.corps = corps;
        this.titre = titre;
        this.taille = taille;
        this.tailleTitre = tailleTitre;
        this.interligne = interligne;
    }

    /** taille : 5 pour les textes courants, 3 pour les petites mentions (elles passent en Barlow, plus serree). */
    public static Police charger(Path dossierInterface, int taille) throws IOException {
        Font[] f = familles(dossierInterface.resolveSibling("polices"));
        return taille >= 5 ? new Police(f[0], f[2], 9.2f, 10.2f, 12) : new Police(f[1], f[2], 7.4f, 7.4f, 9);
    }

    private static Font[] familles(Path dossier) throws IOException {
        Font[] deja = FAMILLES.get(dossier);
        if (deja != null) {
            return deja;
        }
        Font[] f = {lire(dossier.resolve("AtkinsonHyperlegible-Regular.ttf")), lire(dossier.resolve("BarlowCondensed-Medium.ttf")),
            lire(dossier.resolve("BarlowCondensed-SemiBold.ttf")), lire(dossier.resolve("AtkinsonHyperlegible-Bold.ttf"))};
        FAMILLES.put(dossier, f);
        return f;
    }

    private static Font lire(Path fichier) throws IOException {
        try {
            return Font.createFont(Font.TRUETYPE_FONT, fichier.toFile());
        } catch (FontFormatException erreur) {
            throw new IOException("Police illisible : " + fichier, erreur);
        }
    }

    /** Hauteur d'une ligne, interligne compris. */
    public int ligne() {
        return interligne;
    }

    /** La police a employer : le corps du texte, ou les titres quand on ecrit en grand. */
    private Font police(int echelle) {
        return polices.computeIfAbsent(echelle, e -> e >= 2 ? titre.deriveFont(tailleTitre * e * Rendu.F) : corps.deriveFont(taille * Rendu.F));
    }

    /** Les polices a chaque taille et les largeurs deja mesurees : on ne les recalcule pas a chaque image. */
    private final java.util.Map<Integer, Font> polices = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> largeurs = new java.util.HashMap<>();
    private Font titreCorps;

    private Font titreCorps() {
        if (titreCorps == null) {
            titreCorps = titre.deriveFont(tailleTitre * Rendu.F);
        }
        return titreCorps;
    }

    private int mesurer(String cle, Font f, String texte) {
        Integer deja = largeurs.get(cle);
        if (deja != null) {
            return deja;
        }
        if (largeurs.size() > 4000) {
            largeurs.clear();
        }
        int l = (int) Math.ceil(f.getStringBounds(propre(texte), MESURE).getWidth() / Rendu.F);
        largeurs.put(cle, l);
        return l;
    }

    private float hauteurCapitales(int echelle) {
        return (echelle >= 2 ? tailleTitre * echelle * 0.7f : taille * 0.72f);
    }

    public int largeur(String texte, int echelle) {
        return mesurer(echelle + "|" + texte, police(echelle), texte);
    }

    /** Le texte tel qu'on l'affiche : les guillemets et les espaces typographiques sont gardes ; la casse aussi. */
    public String propre(String texte) {
        return texte.replace(' ', ' ').replace("...", "…");
    }

    /** Ecrit le texte, le haut de ses capitales en y. Renvoie la largeur ecrite. */
    public int ecrire(Rendu rendu, String texte, int x, int y, int rvb, int echelle) {
        String t = propre(texte);
        rendu.texte(t, x, y + hauteurCapitales(echelle), police(echelle), rvb, false);
        return largeur(texte, echelle);
    }

    /** Pareil, avec une ombre douce dessous : pour le texte pose directement sur le decor. */
    public int ecrireOmbre(Rendu rendu, String texte, int x, int y, int rvb, int echelle) {
        String t = propre(texte);
        rendu.texte(t, x, y + hauteurCapitales(echelle), police(echelle), rvb, true);
        return largeur(texte, echelle);
    }

    /** En lettres de titre (Barlow), a la taille du corps : les noms, les etiquettes. */
    public int ecrireTitre(Rendu rendu, String texte, int x, int y, int rvb) {
        String t = propre(texte);
        rendu.texte(t, x, y + tailleTitre * 0.7f, titreCorps(), rvb, false);
        return largeurTitre(texte);
    }

    public int largeurTitre(String texte) {
        return mesurer("t|" + texte, titreCorps(), texte);
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
