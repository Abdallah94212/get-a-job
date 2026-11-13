package jeu;

import decor.Rendu;

/** La page des options, la meme dans le menu principal et dans la pause. */
final class PageOptions {
    private static final String[] NOMS = {"MUSIQUE", "AMBIANCES", "EFFETS SONORES", "TRAME À L'ANCIENNE", "AFFICHAGE",
        "VITESSE DU TEXTE", "RETOUR"};

    private final Jeu jeu;
    private int choix;

    PageOptions(Jeu jeu) {
        this.jeu = jeu;
    }

    /** Renvoie vrai quand le joueur quitte la page. */
    boolean maj(Touches touches) {
        Reglages r = jeu.reglages;
        if (touches.retour()) {
            choix = 0;
            return true;
        }
        if (touches.haut() || touches.bas()) {
            choix = Math.floorMod(choix + (touches.bas() ? 1 : -1), NOMS.length);
            jeu.audio.jouer("ui_deplace", 0.7f, 0, 1);
        }
        int pas = (touches.droite() || touches.valider() ? 1 : 0) - (touches.gauche() ? 1 : 0);
        if (pas == 0) {
            return false;
        }
        switch (choix) {
            case 0 -> r.musique = touches.valider() ? (r.musique + 1) % 11 : Reglages.borne(r.musique + pas, 0, 10);
            case 1 -> r.ambiance = touches.valider() ? (r.ambiance + 1) % 11 : Reglages.borne(r.ambiance + pas, 0, 10);
            case 2 -> r.effets = touches.valider() ? (r.effets + 1) % 11 : Reglages.borne(r.effets + pas, 0, 10);
            case 3 -> r.trame = !r.trame;
            case 4 -> r.taille = Math.floorMod(r.taille + pas, Reglages.TAILLES.length);
            case 5 -> r.vitesseTexte = Math.floorMod(r.vitesseTexte + pas, Reglages.VITESSES.length);
            default -> {
                if (touches.valider()) {
                    choix = 0;
                    return true;
                }
                return false;
            }
        }
        jeu.reglagesChanges();
        jeu.audio.jouer("ui_valide", 0.6f, 0, 1);
        return false;
    }

    void dessiner(Rendu rendu, int x, int y) {
        Reglages r = jeu.reglages;
        Affichage a = jeu.affichage;
        a.police().ecrire(rendu, "OPTIONS", x, y, Affichage.ORANGE, 2);
        String[] valeurs = {null, null, null, r.trame ? "OUI" : "NON", Reglages.TAILLES[r.taille], Reglages.VITESSES[r.vitesseTexte], ""};
        int[] volumes = {r.musique, r.ambiance, r.effets};
        for (int i = 0; i < NOMS.length; i++) {
            int ligne = y + 28 + i * 15;
            Menus.ligne(rendu, a, NOMS[i], x + 14, ligne, i == choix, true);
            if (i < 3) {
                for (int k = 0; k < 10; k++) {
                    rendu.rect(x + 150 + k * 7, ligne + 2, 5, 7, k < volumes[i] ? Affichage.ORANGE : Affichage.SOMBRE, 256);
                }
            } else {
                a.police().ecrire(rendu, valeurs[i], x + 150, ligne, Affichage.GRIS, 1);
            }
        }
        a.petite().ecrire(rendu, "GAUCHE, DROITE : CHANGER   ECHAP : RETOUR", x, y + 138, Affichage.GRIS, 1);
    }
}
