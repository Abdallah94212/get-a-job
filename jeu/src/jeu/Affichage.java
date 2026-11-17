package jeu;

import decor.Police;
import decor.Rendu;
import decor.Texture;
import histoire.EtatMission;
import java.util.ArrayList;
import java.util.List;

/** Les elements d'interface dessines par-dessus le decor : boite de dialogue, bustes, choix, telephone, pictogrammes. */
final class Affichage {
    static final int ORANGE = 0xFF7A12;
    static final int BLANC = 0xECECE6;
    static final int GRIS = 0xAAAAB0;
    static final int SOMBRE = 0x60606C;
    static final int VERT = 0x46F06E;
    static final int ENCRE = 0x14141A;

    /** Le haut de la boite de dialogue : les bustes s'y posent, les choix s'empilent au-dessus. */
    static final int BOITE = 194;

    private final Police police;
    private final Police petite;

    Affichage(Ressources res) {
        this.police = res.police;
        this.petite = res.petite;
    }

    Police police() {
        return police;
    }

    Police petite() {
        return petite;
    }

    static List<String> debut(List<String> lignes, int lettres) {
        List<String> visibles = new ArrayList<>();
        int reste = lettres;
        for (String ligne : lignes) {
            if (reste <= 0) {
                break;
            }
            visibles.add(ligne.substring(0, Math.min(ligne.length(), reste)));
            reste -= ligne.length() + 1;
        }
        return visibles;
    }

    /**
     * Un buste pose sur la boite de dialogue : a droite pour celui qui parle au heros, a gauche pour le heros.
     * Celui qui ne parle pas reste, assombri, pour qu'on sache toujours avec qui l'on parle.
     */
    void buste(Rendu rendu, Texture buste, boolean aGauche, boolean parle) {
        if (buste == null) {
            return;
        }
        int x = aGauche ? 14 : Rendu.L - 14 - buste.largeur();
        int y = BOITE - buste.hauteur() + 2;
        rendu.calque(buste, x, y);
        if (!parle) {
            rendu.rect(x, y, buste.largeur(), buste.hauteur() - 2, ENCRE, 120);
        }
    }

    /** La boite de dialogue : un onglet orange avec le nom de celui qui parle, et le texte qui s'ecrit. */
    void dialogue(Rendu rendu, String nom, boolean nomAGauche, String entier, int lettres, boolean complet, long tempsMs) {
        rendu.rect(10, BOITE, Rendu.L - 20, 66, ENCRE, 240);
        rendu.rect(10, BOITE, Rendu.L - 20, 1, SOMBRE, 256);
        rendu.rect(10, BOITE + 65, Rendu.L - 20, 1, SOMBRE, 256);
        rendu.rect(10, BOITE, 1, 66, SOMBRE, 256);
        rendu.rect(Rendu.L - 11, BOITE, 1, 66, SOMBRE, 256);
        if (nom != null) {
            int largeur = police.largeurTitre(nom) + 16;
            int x = nomAGauche ? 18 : Rendu.L - 18 - largeur;
            rendu.rect(x, BOITE - 13, largeur, 14, ORANGE, 256);
            police.ecrireTitre(rendu, nom, x + 8, BOITE - 10, ENCRE);
        }
        int y = BOITE + 10;
        for (String ligne : debut(police.couper(police.propre(Casse.phrase(entier)), Rendu.L - 60), lettres)) {
            police.ecrire(rendu, ligne, 28, y, nom == null ? GRIS : BLANC, 1);
            y += 14;                                                   // un interligne large : on lit sans effort
        }
        if (complet && tempsMs / 400 % 2 == 0) {
            fleche(rendu, Rendu.L - 30, BOITE + 54, BLANC);
        }
    }

    static final int BLEU_APPEL = 0x4A7BD8;

    /**
     * La boite d'une replique au telephone : un onglet bleu avec un combine, le nom suivi de "au telephone" ; la voix
     * au bout du fil (distant) s'ecrit en bleu pale, entre guillemets : elle n'est pas dans la piece.
     */
    void dialogueAppel(Rendu rendu, String nom, boolean distant, String entier, int lettres, boolean complet, long tempsMs) {
        rendu.rect(10, BOITE, Rendu.L - 20, 66, distant ? 0x10141E : ENCRE, 240);
        rendu.rect(10, BOITE, Rendu.L - 20, 1, BLEU_APPEL, 256);
        rendu.rect(10, BOITE + 65, Rendu.L - 20, 1, SOMBRE, 256);
        rendu.rect(10, BOITE, 1, 66, SOMBRE, 256);
        rendu.rect(Rendu.L - 11, BOITE, 1, 66, SOMBRE, 256);
        String onglet = distant ? "AU TÉLÉPHONE : " + nom : nom + " · AU TÉLÉPHONE";
        int largeur = police.largeurTitre(onglet) + 26;
        int x = Rendu.L - 18 - largeur;                                 // sous celui qui parle, a droite : le heros n'est pas dans cet appel
        rendu.rect(x, BOITE - 13, largeur, 14, BLEU_APPEL, 256);
        rendu.rect(x + 6, BOITE - 10, 4, 8, ENCRE, 256);                // le combine
        rendu.rect(x + 7, BOITE - 9, 2, 5, 0xC8E0FF, 256);
        police.ecrireTitre(rendu, onglet, x + 16, BOITE - 10, BLANC);
        int y = BOITE + 10;
        String t = distant ? "« " + entier + " »" : entier;
        for (String ligne : debut(police.couper(police.propre(Casse.phrase(t)), Rendu.L - 60), lettres + (distant ? 2 : 0))) {
            police.ecrire(rendu, ligne, 28, y, distant ? 0xB8CCF0 : BLANC, 1);
            y += 14;
        }
        if (complet && tempsMs / 400 % 2 == 0) {
            fleche(rendu, Rendu.L - 30, BOITE + 54, BLANC);
        }
    }

    /** A la place d'un visage, pour la voix au bout du fil : l'ecran d'un appel en cours, sa duree, la voix qui ondule. */
    void ecranAppel(Rendu rendu, String nom, float duree, boolean parle, long tempsMs) {
        int l = 104;
        int h = 72;
        int x = Rendu.L - 14 - l;
        int y = BOITE - h - 16;
        rendu.rect(x - 1, y - 1, l + 2, h + 2, 0x2A2A36, 256);
        rendu.rect(x, y, l, h, 0x0E1220, 245);
        rendu.rect(x + 6, y + 7, 4, 4, (tempsMs / 500) % 2 == 0 ? 0x3BB273 : 0x1E5A3A, 256);
        petite.ecrire(rendu, "APPEL EN COURS", x + 14, y + 6, GRIS, 1);
        police.ecrire(rendu, police.propre(nom), x + 6, y + 18, BLANC, 1);
        int s = Math.round(duree);
        petite.ecrire(rendu, String.format("%02d:%02d", s / 60, s % 60), x + 6, y + 32, SOMBRE, 1);
        for (int k = 0; k < 16; k++) {                                  // l'onde de la voix
            double v = parle ? Math.abs(Math.sin(tempsMs / 70.0 + k * 1.7) * Math.sin(tempsMs / 190.0 + k)) : 0.08;
            int bh = 1 + (int) Math.round(v * 14);
            rendu.rect(x + 6 + k * 6, y + 58 - bh, 3, bh, BLEU_APPEL, parle ? 230 : 120);
        }
    }

    /** Les reponses possibles du heros, empilees au-dessus de la boite, a gauche. La choisie est en clair. */
    void choix(Rendu rendu, List<String> options, int choisi) {
        int hauteur = 15;
        int y0 = BOITE - 6 - options.size() * hauteur;
        int largeur = 0;
        for (String option : options) {
            largeur = Math.max(largeur, police.largeur(option, 1) + 28);
        }
        for (int i = 0; i < options.size(); i++) {
            int y = y0 + i * hauteur;
            boolean actif = i == choisi;
            rendu.rect(14, y, largeur, hauteur - 2, actif ? BLANC : ENCRE, actif ? 256 : 220);
            rendu.rect(14, y, 2, hauteur - 2, actif ? ORANGE : SOMBRE, 256);
            if (actif) {
                flecheCote(rendu, 20, y + 2, true, ORANGE);
            }
            police.ecrire(rendu, options.get(i), 30, y + 3, actif ? ENCRE : BLANC, 1);
        }
    }

    /** Un texte centre sur fond noir : les cartons de recit. */
    void carton(Rendu rendu, String entier, int lettres, int y, int couleur) {
        List<String> lignes = police.couper(police.propre(Casse.phrase(entier)), 360);
        int depart = y - lignes.size() * 7;
        List<String> visibles = debut(lignes, lettres);
        for (int i = 0; i < visibles.size(); i++) {
            int gauche = (Rendu.L - police.largeur(lignes.get(i), 1)) / 2;
            police.ecrire(rendu, visibles.get(i), gauche, depart + i * 15, couleur, 1);
        }
    }

    /** Un telephone tenu dans la main, un message a l'ecran. Il tremble un instant quand il vibre. */
    void telephone(Rendu rendu, String expediteur, String message, float age) {
        int secousse = age < 0.6f ? (int) (Math.sin(age * 70) * 2) : 0;
        int x = 300 + secousse;
        int y = 34;
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 110);
        rendu.rect(x, y, 140, 210, 0x1A1A20, 256);
        rendu.rect(x + 6, y + 14, 128, 172, 0x24303C, 256);
        rendu.rect(x + 56, y + 6, 28, 3, 0x40404C, 256);
        rendu.rect(x + 6, y + 14, 128, 18, 0x18222C, 256);
        police.ecrire(rendu, expediteur, x + 12, y + 18, BLANC, 1);
        petite.ecrire(rendu, "MAINTENANT", x + 84, y + 21, GRIS, 1);
        List<String> lignes = police.couper(police.propre(message), 104);
        rendu.rect(x + 12, y + 42, 116, lignes.size() * 12 + 10, 0x3A4A5A, 256);
        for (int i = 0; i < lignes.size(); i++) {
            police.ecrire(rendu, lignes.get(i), x + 18, y + 47 + i * 12, BLANC, 1);
        }
    }

    /** L'annonce du metro : un bandeau en haut de l'ecran, comme les ecrans des rames. */
    void annonce(Rendu rendu, String texte) {
        int largeur = police.largeur(texte, 1) + 28;
        int x = (Rendu.L - largeur) / 2;
        rendu.rect(x, 20, largeur, 17, 0x283466, 250);
        rendu.rect(x, 20, largeur, 1, 0x5A6AAA, 256);
        rendu.rect(x + 6, 25, 6, 6, 0xF0C040, 256);
        police.ecrire(rendu, texte, x + 18, 24, BLANC, 1);
    }

    static void fleche(Rendu rendu, int x, int y, int couleur) {
        for (int j = 0; j < 4; j++) {
            for (int i = j; i < 7 - j; i++) {
                rendu.point(x + i, y + j, couleur);
            }
        }
    }

    static void flecheCote(Rendu rendu, int x, int y, boolean versDroite, int couleur) {
        for (int i = 0; i < 5; i++) {
            for (int j = i; j < 9 - i; j++) {
                rendu.point(versDroite ? x + i : x - i, y + j, couleur);
            }
        }
    }

    static void etat(Rendu rendu, int x, int y, EtatMission etat) {
        switch (etat) {
            case VERROUILLEE -> {
                rendu.rect(x + 1, y + 3, 6, 5, SOMBRE, 256);
                rendu.rect(x + 2, y, 4, 1, SOMBRE, 256);
                rendu.rect(x + 2, y + 1, 1, 2, SOMBRE, 256);
                rendu.rect(x + 5, y + 1, 1, 2, SOMBRE, 256);
            }
            case DISPONIBLE -> {
                for (int j = 0; j < 7; j++) {
                    int demi = 3 - Math.abs(3 - j);
                    rendu.rect(x + 3 - demi, y + j, demi * 2 + 1, 1, ORANGE, 256);
                }
            }
            case TERMINEE -> {
                for (int i = 0; i < 3; i++) {
                    rendu.rect(x + i, y + 3 + i, 1, 2, VERT, 256);
                }
                for (int i = 0; i < 5; i++) {
                    rendu.rect(x + 3 + i, y + 5 - i, 1, 2, VERT, 256);
                }
            }
        }
    }

    void etiquette(Rendu rendu, String texte, int centreX, int y, int couleur) {
        int largeur = police.largeur(texte, 1) + 12;
        rendu.cadre(centreX - largeur / 2, y, largeur, 15);
        police.ecrire(rendu, texte, centreX - largeur / 2 + 6, y + 2, couleur, 1);
    }
}
