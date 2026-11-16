package jeu;

import decor.Rendu;
import decor.Texture;
import histoire.Encart;
import histoire.Progression;
import java.util.List;

/**
 * Les encarts explicatifs : quand le joueur decouvre un objet ou une mecanique, le jeu s'arrete un instant et la
 * presente. A gauche l'illustration, qui flotte doucement ; a droite ce que c'est, en grand, puis trois ou quatre
 * lignes courtes. E pour reprendre. Un encart ne s'affiche qu'une fois par partie (il laisse un drapeau "vu_...").
 *
 * <p>Le systeme sert a toutes les mecaniques : un encart se declare dans la campagne ("encart id GENRE | TITRE |
 * image | ligne | ligne"), et se montre depuis une scene ("encart id") ou depuis le jeu (premiere poubelle, premier
 * combat).
 */
final class Encarts {
    private static final int X = 52;
    private static final int Y = 44;
    private static final int L = 376;
    private static final int H = 184;

    private final Ressources res;
    private final Audio audio;
    private final Affichage affichage;
    private Encart courant;
    private float age;
    /** Les encarts qui attendent leur tour : deux decouvertes au meme instant s'expliquent l'une apres l'autre. */
    private final java.util.ArrayDeque<Encart> suivants = new java.util.ArrayDeque<>();

    Encarts(Ressources res, Audio audio, Affichage affichage) {
        this.res = res;
        this.audio = audio;
        this.affichage = affichage;
    }

    /** Montre cet encart s'il n'a jamais ete vu. Renvoie vrai s'il s'ouvre. */
    boolean ouvrir(String id, Progression progression) {
        Encart e = res.campagne.encarts().get(id);
        if (e == null || progression.drapeau("vu_" + id)) {
            return false;
        }
        progression.marquer("+vu_" + id);
        if (courant != null) {
            suivants.add(e);
            return true;
        }
        courant = e;
        age = 0;
        audio.jouer("objectif", 0.5f, 0, 0.8f);
        audio.jouer(Audio.Bus.EFFETS, "papier", 0.3f, 0, 1.3f, 0.05f);
        return true;
    }

    void toutFermer() {
        courant = null;
        suivants.clear();
    }

    boolean ouvert() {
        return courant != null;
    }

    String courant() {
        return courant == null ? null : courant.id();
    }

    /** Une image : renvoie vrai a l'instant ou le joueur le referme. On ne peut pas le fermer par megarde tout de suite. */
    boolean maj(float dt, Touches touches) {
        if (courant == null) {
            return false;
        }
        age += dt;
        if (age > 0.6f && (touches.valider() || touches.retour())) {
            courant = suivants.poll();
            age = 0;
            audio.jouer(courant == null ? "ui_retour" : "objectif", 0.5f, 0, courant == null ? 1 : 0.8f);
            return courant == null;
        }
        return false;
    }

    /**
     * La fiche d'un personnage qu'on rencontre : son portrait manga a gauche, qui glisse en place ; son nom en grand, son
     * role, deux ou trois choses que le heros sait de lui, et ou ils en sont tous les deux.
     */
    private void fiche(Rendu rendu, float doux) {
        int y = Y + Math.round((1 - doux) * 14);
        rendu.rect(X - 1, y - 1, L + 2, H + 2, 0x4A4A62, 256);
        rendu.rect(X, y, L, H, 0x14141E, 256);
        int pl = 136;                                                   // le panneau du portrait : un fond chaud, en bandes
        for (int k = 0; k < H; k += 4) {
            float t = (float) k / H;
            int r = Math.round(0x3A + (0x18 - 0x3A) * t);
            int g = Math.round(0x22 + (0x12 - 0x22) * t);
            rendu.rect(X, y + k, pl, Math.min(4, H - k), (r << 16) | (g << 8) | 0x20, 256);
        }
        rendu.rect(X + pl, y, 2, H, Affichage.ORANGE, 256);
        Texture portrait = res.image(courant.image());
        if (portrait != null) {
            int glisse = Math.round((1 - doux) * -20);
            int px = X + (pl - portrait.largeur()) / 2 + glisse;
            int py = y + H - portrait.hauteur();
            rendu.calque(portrait, px, py);
        }
        int tx = X + pl + 14;
        int largeur = X + L - 12 - tx;
        affichage.petite().ecrire(rendu, "NOUVEAU PERSONNAGE", tx, y + 12, Affichage.ORANGE, 1);
        int echelle = affichage.police().largeur(courant.titre(), 3) <= largeur ? 3 : 2;
        affichage.police().ecrire(rendu, courant.titre(), tx, y + 24, Affichage.BLANC, echelle);
        int ly = y + 24 + echelle * 9 + 6;
        List<String> lignes = courant.lignes();
        if (!lignes.isEmpty()) {
            affichage.police().ecrire(rendu, affichage.police().propre(lignes.get(0)), tx, ly, Affichage.ORANGE, 1);
            ly += 16;
        }
        for (int i = 1; i < lignes.size(); i++) {
            String ligne = lignes.get(i);
            boolean lien = ligne.startsWith("TOI ET");
            if (lien) {                                                 // ou ils en sont : a part, en bas
                int deux = ligne.indexOf(':');
                rendu.rect(tx, y + H - 34, largeur, 1, 0x2A2A36, 256);
                affichage.petite().ecrire(rendu, ligne.substring(0, deux).trim(), tx, y + H - 28, Affichage.SOMBRE, 1);
                String apres = ligne.substring(deux + 1).trim();
                apres = apres.isEmpty() ? apres : Character.toUpperCase(apres.charAt(0)) + apres.substring(1);
                affichage.police().ecrire(rendu, affichage.police().propre(apres), tx, y + H - 18, Affichage.BLANC, 1);
                continue;
            }
            rendu.rect(tx, ly + 4, 3, 3, Affichage.ORANGE, 256);
            for (String m : affichage.police().couper(affichage.police().propre(ligne), largeur - 8)) {
                affichage.police().ecrire(rendu, m, tx + 8, ly, Affichage.GRIS, 1);
                ly += 12;
            }
            ly += 3;
        }
        if (age > 0.6f) {
            int bx = X + L - 70;
            int by = y + 10;
            boolean clair = ((int) (age * 2)) % 2 == 0;
            rendu.rect(bx, by - 1, 10, 10, clair ? Affichage.BLANC : Affichage.GRIS, 256);
            affichage.police().ecrire(rendu, "E", bx + 3, by, Affichage.ENCRE, 1);
            affichage.police().ecrireTitre(rendu, "COMPRIS", bx + 14, by, Affichage.BLANC);
        }
    }

    void dessiner(Rendu rendu) {
        if (courant == null) {
            return;
        }
        float entree = Math.min(1f, age / 0.25f);
        float doux = 1 - (1 - entree) * (1 - entree);
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x05050A, Math.round(170 * doux));
        if (courant.genre().equals("PERSONNAGE")) {
            fiche(rendu, doux);
            return;
        }
        int y = Y + Math.round((1 - doux) * 14);
        // la carte : un fond sombre, un filet clair, une bande orange en haut
        rendu.rect(X - 1, y - 1, L + 2, H + 2, 0x4A4A62, 256);
        rendu.rect(X, y, L, H, 0x14141E, 256);
        rendu.rect(X, y, L, 3, Affichage.ORANGE, 256);
        // l'illustration, dans sa vitrine, qui flotte
        int vx = X + 12;
        int vy = y + 18;
        int cote = 124;
        rendu.rect(vx, vy, cote, cote, 0x0B0B12, 256);
        for (int k = 0; k < cote; k += 8) {                             // une grille tres discrete, comme une table lumineuse
            rendu.rect(vx + k, vy, 1, cote, 0x16161F, 256);
            rendu.rect(vx, vy + k, cote, 1, 0x16161F, 256);
        }
        Texture image = res.image(courant.image());
        if (image != null) {
            int k = Math.max(1, Math.min(6, (cote - 16) / Math.max(image.largeur(), image.hauteur())));
            int flotte = Math.round((float) Math.sin(age * 2.4) * 2);
            int ix = vx + (cote - image.largeur() * k) / 2;
            int iy = vy + (cote - image.hauteur() * k) / 2 + flotte;
            rendu.rect(ix + 3, vy + cote - 10, image.largeur() * k - 6, 2, 0x000000, 160);   // son ombre
            rendu.calqueEchelle(image, ix, iy, k);
        }
        rendu.rect(vx, vy + cote - 1, cote, 1, Affichage.ORANGE, 120);
        // le texte : le genre, le titre, puis les lignes
        int tx = vx + cote + 14;
        int largeur = X + L - 14 - tx;
        affichage.petite().ecrire(rendu, courant.genre(), tx, y + 16, Affichage.ORANGE, 1);
        affichage.police().ecrire(rendu, courant.titre(), tx, y + 28, Affichage.BLANC, 2);
        rendu.rect(tx, y + 52, 24, 1, Affichage.ORANGE, 256);
        int ly = y + 62;
        for (String ligne : courant.lignes()) {
            List<String> morceaux = affichage.police().couper(affichage.police().propre(ligne), largeur - 8);
            rendu.rect(tx, ly + 4, 3, 3, Affichage.ORANGE, 256);
            for (String m : morceaux) {
                affichage.police().ecrire(rendu, m, tx + 8, ly, Affichage.GRIS, 1);
                ly += 12;
            }
            ly += 4;
        }
        // E : reprendre, quand on peut
        if (age > 0.6f) {
            int bx = X + L - 76;
            int by = y + H - 18;
            boolean clair = ((int) (age * 2)) % 2 == 0;
            rendu.rect(bx, by - 1, 10, 10, clair ? Affichage.BLANC : Affichage.GRIS, 256);
            affichage.police().ecrire(rendu, "E", bx + 3, by, Affichage.ENCRE, 1);
            affichage.police().ecrireTitre(rendu, "COMPRIS", bx + 14, by, Affichage.BLANC);
        }
    }
}
