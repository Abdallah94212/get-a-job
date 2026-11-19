package jeu;

import decor.Rendu;

/** La page des options, la meme dans le menu principal et dans la pause. */
final class PageOptions {
    private static final String[] NOMS = {"VOLUME GÉNÉRAL", "MUSIQUE", "AMBIANCES", "EFFETS SONORES", "VOIX", "INTERFACE",
        "TRAME À L'ANCIENNE", "AFFICHAGE", "VITESSE DU TEXTE", "RETOUR"};
    private static final int VOLUMES = 6;
    private static final int PAS = 13;

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
        if (choix < VOLUMES) {
            int[] v = volumes(r);
            int nouveau = touches.valider() ? (v[choix] + 1) % 11 : Reglages.borne(v[choix] + pas, 0, 10);
            switch (choix) {
                case 0 -> r.general = nouveau;
                case 1 -> r.musique = nouveau;
                case 2 -> r.ambiance = nouveau;
                case 3 -> r.effets = nouveau;
                case 4 -> r.voix = nouveau;
                default -> r.interfaceSon = nouveau;
            }
            jeu.reglagesChanges();
            Audio.Bus[] bus = {Audio.Bus.INTERFACE, Audio.Bus.MUSIQUE, Audio.Bus.AMBIANCE, Audio.Bus.EFFETS, Audio.Bus.VOIX,
                Audio.Bus.INTERFACE};
            jeu.audio.jouer(bus[choix], echantillon(choix), 0.8f, 0, 1, 0);   // on entend tout de suite ce qu'on regle
            return false;
        }
        switch (choix) {
            case 6 -> r.trame = !r.trame;
            case 7 -> r.taille = Math.floorMod(r.taille + pas, Reglages.TAILLES.length);
            case 8 -> r.vitesseTexte = Math.floorMod(r.vitesseTexte + pas, Reglages.VITESSES.length);
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

    private static int[] volumes(Reglages r) {
        return new int[] {r.general, r.musique, r.ambiance, r.effets, r.voix, r.interfaceSon};
    }

    /** Le son qui fait entendre le volume qu'on vient de changer : une voix pour les voix, une flaque pour les
     * ambiances, un accord pour la musique : chacun joue sur le bus qu'il regle. */
    private static String echantillon(int choix) {
        return switch (choix) {
            case 1 -> "chapitre";
            case 2 -> "pas_flaque";
            case 3 -> "porte_ferme";
            case 4 -> "v_heros";
            default -> "ui_valide";
        };
    }

    void dessiner(Rendu rendu, int x, int y) {
        Reglages r = jeu.reglages;
        Affichage a = jeu.affichage;
        a.police().ecrire(rendu, "OPTIONS", x, y, Affichage.ORANGE, 2);
        String[] valeurs = {null, null, null, null, null, null, r.trame ? "OUI" : "NON", Reglages.TAILLES[r.taille],
            Reglages.VITESSES[r.vitesseTexte], ""};
        int[] volumes = volumes(r);
        for (int i = 0; i < NOMS.length; i++) {
            int ligne = y + 26 + i * PAS + (i >= VOLUMES ? 4 : 0);
            Menus.ligne(rendu, a, NOMS[i], x + 14, ligne, i == choix, true);
            if (i < VOLUMES) {
                for (int k = 0; k < 10; k++) {
                    rendu.rect(x + 150 + k * 7, ligne + 2, 5, 7, k < volumes[i] ? Affichage.ORANGE : Affichage.SOMBRE, 256);
                }
            } else {
                a.police().ecrire(rendu, valeurs[i], x + 150, ligne, Affichage.GRIS, 1);
            }
        }
        a.petite().ecrire(rendu, "GAUCHE, DROITE : CHANGER   ECHAP : RETOUR", x, y + 26 + NOMS.length * PAS + 10, Affichage.GRIS, 1);
    }
}
