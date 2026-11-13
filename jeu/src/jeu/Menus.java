package jeu;

import decor.Rendu;
import histoire.Campagne;
import histoire.EtatMission;
import histoire.Mission;
import histoire.Objectif;
import histoire.Progression;
import java.util.List;

/** Les morceaux de menu partages par le menu principal et la pause : une ligne a choisir, la fiche d'une mission, l'aide. */
final class Menus {
    private Menus() {
    }

    /** Une ligne de menu : orange et marquee d'une fleche quand elle est choisie, eteinte quand on ne peut pas la prendre. */
    static void ligne(Rendu rendu, Affichage affichage, String texte, int x, int y, boolean choisie, boolean active) {
        if (choisie) {
            Affichage.flecheCote(rendu, x - 12, y + 1, true, Affichage.ORANGE);
        }
        affichage.police().ecrire(rendu, texte, x, y, !active ? Affichage.SOMBRE : choisie ? Affichage.ORANGE : Affichage.BLANC, 1);
    }

    private static int paragraphe(Rendu rendu, Affichage affichage, String texte, int x, int y, int largeur, int couleur) {
        List<String> lignes = affichage.police().couper(affichage.police().propre(texte), largeur);
        for (String ligne : lignes) {
            affichage.police().ecrire(rendu, ligne, x, y, couleur, 1);
            y += 11;
        }
        return y;
    }

    /** Tout ce qu'on sait d'une mission : son titre, son lieu, ses personnages, son histoire, ou on en est. */
    static void ficheMission(Rendu rendu, Affichage affichage, Campagne campagne, Progression progression, Mission mission,
                             int x, int y, int largeur) {
        if (mission == null) {
            affichage.police().ecrire(rendu, "FIN DE L'ACTE 2", x, y, Affichage.ORANGE, 2);
            paragraphe(rendu, affichage, "Tu as joué toute l'histoire écrite à ce jour. Les actes 3 à 7 sont en cours d'écriture. "
                    + "Le quartier reste ouvert : promène-toi.", x, y + 30, largeur, Affichage.GRIS);
            return;
        }
        EtatMission etat = progression.etat(mission);
        affichage.petite().ecrire(rendu, campagne.chapitreDe(mission).titre() + " - " + campagne.chapitreDe(mission).sousTitre(),
                x, y, Affichage.GRIS, 1);
        affichage.police().ecrire(rendu, mission.id() + " " + mission.titre(), x, y + 8, Affichage.ORANGE, 2);
        Affichage.etat(rendu, x, y + 37, etat);
        String nomEtat = switch (etat) {
            case VERROUILLEE -> "VERROUILLÉE";
            case DISPONIBLE -> "DISPONIBLE";
            case TERMINEE -> "TERMINÉE";
        };
        affichage.police().ecrire(rendu, nomEtat, x + 13, y + 34,
                etat == EtatMission.TERMINEE ? Affichage.VERT : etat == EtatMission.DISPONIBLE ? Affichage.ORANGE : Affichage.SOMBRE, 1);
        int ligne = y + 52;
        if (etat == EtatMission.VERROUILLEE) {
            Mission avant = campagne.missions().get(campagne.rang(mission.id()) - 1);
            paragraphe(rendu, affichage, "Termine d'abord la mission " + avant.id() + ", " + avant.titre() + ".", x, ligne, largeur, Affichage.GRIS);
            return;
        }
        affichage.police().ecrire(rendu, "LIEU : " + mission.lieu(), x, ligne, Affichage.GRIS, 1);
        affichage.police().ecrire(rendu, "AVEC : " + mission.personnages(), x, ligne + 11, Affichage.GRIS, 1);
        ligne = paragraphe(rendu, affichage, mission.description(), x, ligne + 28, largeur, Affichage.BLANC) + 8;
        int total = mission.etapes().size();
        if (etat == EtatMission.TERMINEE) {
            Affichage.etat(rendu, x, ligne + 2, EtatMission.TERMINEE);
            affichage.police().ecrire(rendu, "OBJECTIFS : " + total + " SUR " + total, x + 13, ligne, Affichage.VERT, 1);
            return;
        }
        affichage.police().ecrire(rendu, "OBJECTIF EN COURS (" + (progression.etape() + 1) + " SUR " + total + ")", x, ligne, Affichage.ORANGE, 1);
        Affichage.etat(rendu, x, ligne + 15, EtatMission.DISPONIBLE);
        ligne = paragraphe(rendu, affichage, progression.objectif().texte(), x + 13, ligne + 12, largeur - 13, Affichage.BLANC) + 4;
        for (Objectif facultatif : mission.facultatifs()) {
            boolean fait = progression.facultatifsFaits().contains(facultatif.cible());
            if (fait) {
                Affichage.etat(rendu, x, ligne + 2, EtatMission.TERMINEE);
            } else {
                affichage.police().ecrire(rendu, "+", x + 1, ligne, Affichage.GRIS, 1);
            }
            ligne = paragraphe(rendu, affichage, "SECONDAIRE : " + facultatif.texte(), x + 13, ligne, largeur - 13, Affichage.GRIS);
        }
    }

    /** Les touches et les signes du jeu. */
    static void commandes(Rendu rendu, Affichage affichage, int x, int y) {
        affichage.police().ecrire(rendu, "COMMANDES", x, y, Affichage.ORANGE, 2);
        String[][] touches = {
            {"FLÈCHES OU ZQSD", "MARCHER"},
            {"MAJ", "COURIR"},
            {"E, ENTRÉE, ESPACE", "PARLER, AGIR, ENTRER, SUITE"},
            {"ÉCHAP", "PAUSE ; PASSER UNE CINÉMATIQUE"},
            {"T", "TRAME À L'ANCIENNE, OUI OU NON"},
        };
        for (int i = 0; i < touches.length; i++) {
            affichage.police().ecrire(rendu, touches[i][0], x, y + 30 + i * 14, Affichage.BLANC, 1);
            affichage.police().ecrire(rendu, touches[i][1], x + 116, y + 30 + i * 14, Affichage.GRIS, 1);
        }
        int ligne = y + 110;
        Affichage.fleche(rendu, x, ligne + 3, Affichage.ORANGE);
        affichage.police().ecrire(rendu, "L'OBJECTIF EN COURS, OU LA PORTE À PRENDRE", x + 14, ligne, Affichage.GRIS, 1);
        Affichage.fleche(rendu, x, ligne + 17, Affichage.GRIS);
        affichage.police().ecrire(rendu, "UN OBJECTIF SECONDAIRE", x + 14, ligne + 14, Affichage.GRIS, 1);
        Affichage.etat(rendu, x, ligne + 30, EtatMission.VERROUILLEE);
        affichage.police().ecrire(rendu, "UN LIEU FERMÉ : L'HISTOIRE L'OUVRIRA", x + 14, ligne + 28, Affichage.GRIS, 1);
    }
}
