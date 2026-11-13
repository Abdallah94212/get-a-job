package jeu;

import decor.Rendu;
import decor.Scene;
import histoire.Chapitre;
import histoire.EtatMission;
import histoire.Mission;
import histoire.Progression;
import java.util.List;

/**
 * Le menu principal. Derriere lui, l'avenue defile lentement : c'est le decor du jeu qui sert de fond.
 * Ses pages : l'accueil, le mode histoire (chapitres et missions), les options, les commandes, la presentation.
 */
final class EcranMenu implements Ecran {
    private enum Page {
        ACCUEIL, HISTOIRE, OPTIONS, COMMANDES, A_PROPOS, REMPLACER
    }

    private static final String[] CHOIX = {"NOUVELLE PARTIE", "CONTINUER", "MODE HISTOIRE", "OPTIONS", "COMMANDES", "À PROPOS", "QUITTER"};
    private static final String[] PRESENTATION = {
        "Le Kremlin-Bicêtre, avenue de Fontainebleau. Un jeune du quartier, seul, parti de rien. Un soir, devant sa télé, "
            + "quelque chose bascule. Il n'a plus qu'un but : inscrire son nom dans la légende.",
        "Tout en bas de l'avenue, le Maître Poulet. Tout en haut, la tour de la Krousty Corp. Chaque mission ouvre un lieu de plus.",
        "Cette version contient le prologue et les actes 1 et 2, en neuf missions. Les actes 3 à 7 sont en cours d'écriture.",
    };
    private static final String[] CREDITS = {
        "HISTOIRE, DÉCORS ET PERSONNAGES : PROJET GET A JOB, EPITECH.",
        "MEUBLES ET PLANTES : PH64 PIXEL PACK, STACY KENDRA LOVE, PURPLEHEART, NARCISSIST INTERACTIVE. CC-BY 4.0.",
        "MUSIQUES, CC0 : TSORTHAN GROVE, OMFGDUDE ET QUBODUP, JOTH, GENA SEALOV, MUTANTLEG, ADIUTORIUM, YD.",
        "SONS, CC0 : RUBBERDUCK, KRESIEK, OGREBANE, IGNASD, ALXL, GBOXMIKEFOZZY, BMACZERO. CC-BY-SA 4.0 : GREGOR QUENDEL.",
        "PALETTE : ENDESGA 32, PAR ENDESGA. ARBRES, CC0 : KARSIORI.",
    };

    private final Jeu jeu;
    private final Affichage affichage;
    private final Scene fond;
    private final PageOptions options;
    private Progression progression;
    private boolean partieExiste;
    private Page page = Page.ACCUEIL;
    private int choix;
    private int mission;
    private int reponse;
    private float temps;

    EcranMenu(Jeu jeu) {
        this.jeu = jeu;
        this.affichage = jeu.affichage;
        this.fond = jeu.res.carte("avenue_bas");
        this.options = new PageOptions(jeu);
        relire();
        choix = partieExiste ? 1 : 0;
    }

    /** Relit la partie enregistree : c'est elle que montre le mode histoire. */
    private void relire() {
        partieExiste = jeu.sauvegarde.existe();
        progression = jeu.progressionEnregistree();
        mission = Math.min(progression.rang(), jeu.res.campagne.missions().size() - 1);
    }

    @Override
    public void maj(float dt, Touches touches) {
        temps += dt;
        jeu.audio.musique("mus_menu");
        jeu.audio.ambiances(java.util.Map.of("amb_pluie", new float[] {0.35f, 0, 1}, "amb_rue_mouillee", new float[] {0.25f, 0, 1}));
        switch (page) {
            case ACCUEIL -> accueil(touches);
            case HISTOIRE -> histoire(touches);
            case OPTIONS -> {
                if (options.maj(touches)) {
                    page = Page.ACCUEIL;
                }
            }
            case REMPLACER -> {
                if (touches.haut() || touches.bas()) {
                    reponse = 1 - reponse;
                    jeu.audio.jouer("ui_deplace", 0.7f, 0, 1);
                }
                if (touches.retour() || (touches.valider() && reponse == 0)) {
                    page = Page.ACCUEIL;
                } else if (touches.valider()) {
                    jeu.audio.jouer("ui_valide", 0.6f, 0, 1);
                    jeu.nouvellePartie();
                }
            }
            default -> {
                if (touches.retour() || touches.valider()) {
                    page = Page.ACCUEIL;
                }
            }
        }
    }

    private void accueil(Touches touches) {
        if (touches.haut() || touches.bas()) {
            do {
                choix = Math.floorMod(choix + (touches.bas() ? 1 : -1), CHOIX.length);
            } while (choix == 1 && !partieExiste);
            jeu.audio.jouer("ui_deplace", 0.7f, 0, 1);
        }
        if (!touches.valider()) {
            return;
        }
        jeu.audio.jouer("ui_valide", 0.6f, 0, 1);
        switch (choix) {
            case 0 -> {
                if (partieExiste) {
                    reponse = 0;
                    page = Page.REMPLACER;
                } else {
                    jeu.nouvellePartie();
                }
            }
            case 1 -> jeu.continuer();
            case 2 -> page = Page.HISTOIRE;
            case 3 -> page = Page.OPTIONS;
            case 4 -> page = Page.COMMANDES;
            case 5 -> page = Page.A_PROPOS;
            default -> jeu.quitter();
        }
    }

    private void histoire(Touches touches) {
        List<Mission> missions = jeu.res.campagne.missions();
        if (touches.retour()) {
            page = Page.ACCUEIL;
        } else if (touches.haut() || touches.bas()) {
            mission = Math.floorMod(mission + (touches.bas() ? 1 : -1), missions.size());
            jeu.audio.jouer("ui_deplace", 0.7f, 0, 1);
        } else if (touches.valider()) {
            if (progression.etat(missions.get(mission)) == EtatMission.DISPONIBLE) {
                jeu.audio.jouer("ui_valide", 0.6f, 0, 1);
                if (partieExiste) {
                    jeu.continuer();
                } else {
                    jeu.nouvellePartie();
                }
            } else {
                jeu.audio.jouer("ui_refus", 0.7f, 0, 1);
            }
        }
    }

    @Override
    public void dessiner(Rendu rendu) {
        float max = fond.xMax + 12 - Rendu.L;
        float camX = 180 + (float) (1 - Math.cos(temps / 26.0)) / 2f * Math.min(1300, max - 180);
        rendu.dessiner(fond, Math.round(camX), Math.round(temps * 1000), List.of());
        rendu.pluie(Math.round(temps * 1000), camX, 0.8f);
        switch (page) {
            case ACCUEIL, REMPLACER -> dessinerAccueil(rendu);
            case HISTOIRE -> dessinerHistoire(rendu);
            case OPTIONS -> {
                rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 150);
                rendu.cadre(90, 50, 300, 170);
                options.dessiner(rendu, 104, 62);
            }
            case COMMANDES -> {
                rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 150);
                rendu.cadre(70, 40, 340, 190);
                Menus.commandes(rendu, affichage, 84, 52);
                affichage.petite().ecrire(rendu, "ECHAP : RETOUR", 84, 218, Affichage.GRIS, 1);
            }
            default -> dessinerPresentation(rendu);
        }
        if (temps < 0.6f) {
            rendu.fondu(1f - temps / 0.6f);
        }
    }

    private void dessinerAccueil(Rendu rendu) {
        for (int x = 0; x < 250; x += 10) {                              // un voile sombre qui s'efface vers la droite
            rendu.rect(x, 0, 10, Rendu.H, 0x08080E, 210 - x * 3 / 4);
        }
        affichage.police().ecrire(rendu, "GET A JOB", 30, 30, 0x7A3A08, 5);
        affichage.police().ecrire(rendu, "GET A JOB", 28, 28, Affichage.ORANGE, 5);
        affichage.police().ecrire(rendu, "INSCRIS TON NOM DANS LA LÉGENDE.", 30, 88, Affichage.GRIS, 1);
        for (int i = 0; i < CHOIX.length; i++) {
            Menus.ligne(rendu, affichage, CHOIX[i], 44, 118 + i * 16, i == choix && page == Page.ACCUEIL, i != 1 || partieExiste);
        }
        if (partieExiste) {
            Mission enCours = progression.courante();
            String texte = enCours == null ? "ACTES 1 ET 2 TERMINÉS" : enCours.id() + " " + enCours.titre();
            affichage.petite().ecrire(rendu, "PARTIE EN COURS : " + affichage.petite().propre(texte), 152, 137, Affichage.GRIS, 1);
        }
        affichage.petite().ecrire(rendu, "FLECHES : CHOISIR   E : VALIDER   ECHAP : RETOUR", 30, 256, Affichage.GRIS, 1);
        affichage.petite().ecrire(rendu, "PROLOGUE, ACTES 1 ET 2", Rendu.L - 96, 256, Affichage.SOMBRE, 1);
        if (page == Page.REMPLACER) {
            rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 150);
            rendu.cadre(110, 90, 260, 86);
            affichage.police().ecrireCentre(rendu, "UNE PARTIE EST EN COURS.", Rendu.L / 2, 98, Affichage.BLANC, 1);
            affichage.police().ecrireCentre(rendu, "LA REMPLACER PAR UNE NOUVELLE ?", Rendu.L / 2, 111, Affichage.BLANC, 1);
            Menus.ligne(rendu, affichage, "NON, LA GARDER", 180, 134, reponse == 0, true);
            Menus.ligne(rendu, affichage, "OUI, RECOMMENCER", 180, 150, reponse == 1, true);
        }
    }

    /** Le parcours de la campagne : a gauche les chapitres et leurs missions, a droite la fiche de la mission choisie. */
    private void dessinerHistoire(Rendu rendu) {
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 170);
        rendu.cadre(12, 10, 196, 250);
        rendu.cadre(214, 10, 254, 250);
        affichage.police().ecrire(rendu, "MODE HISTOIRE", 22, 16, Affichage.ORANGE, 1);
        List<Mission> missions = jeu.res.campagne.missions();
        int y = 34;
        for (Chapitre chapitre : jeu.res.campagne.chapitres()) {
            affichage.petite().ecrire(rendu, chapitre.titre() + " - " + affichage.petite().propre(chapitre.sousTitre()), 22, y + 2, Affichage.GRIS, 1);
            rendu.rect(22, y + 9, 176, 1, Affichage.SOMBRE, 256);
            y += 13;
            for (Mission m : chapitre.missions()) {
                EtatMission etat = progression.etat(m);
                boolean choisie = missions.indexOf(m) == mission;
                if (choisie) {
                    rendu.rect(18, y - 1, 184, 13, 0x3A3A48, 256);
                }
                Affichage.etat(rendu, 24, y + 2, etat);
                affichage.police().ecrire(rendu, m.id() + " " + m.titre(), 38, y,
                        etat == EtatMission.VERROUILLEE ? Affichage.SOMBRE : choisie ? Affichage.ORANGE : Affichage.BLANC, 1);
                y += 14;
            }
            y += 4;
        }
        Affichage.etat(rendu, 24, y + 2, EtatMission.VERROUILLEE);
        affichage.petite().ecrire(rendu, "ACTES 3 A 7 : EN COURS D'ECRITURE", 38, y + 3, Affichage.SOMBRE, 1);
        Mission choisie = missions.get(mission);
        Menus.ficheMission(rendu, affichage, jeu.res.campagne, progression, choisie, 224, 18, 234);
        String aide = progression.etat(choisie) == EtatMission.DISPONIBLE
                ? "E : " + (partieExiste ? "REPRENDRE CETTE MISSION" : "COMMENCER") + "   ECHAP : RETOUR" : "ECHAP : RETOUR";
        affichage.petite().ecrire(rendu, aide, 224, 250, Affichage.GRIS, 1);
    }

    private void dessinerPresentation(Rendu rendu) {
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 170);
        rendu.cadre(40, 14, 400, 242);
        affichage.police().ecrire(rendu, "GET A JOB", 54, 22, Affichage.ORANGE, 2);
        int y = 50;
        for (String paragraphe : PRESENTATION) {
            for (String ligne : affichage.police().couper(affichage.police().propre(paragraphe), 372)) {
                affichage.police().ecrire(rendu, ligne, 54, y, Affichage.BLANC, 1);
                y += 11;
            }
            y += 7;
        }
        affichage.police().ecrire(rendu, "CRÉDITS", 54, y, Affichage.ORANGE, 1);
        y += 13;
        for (String credit : CREDITS) {                                  // en petits caracteres, comme au dos d'une boite de jeu
            for (String ligne : affichage.petite().couper(affichage.petite().propre(credit), 372)) {
                affichage.petite().ecrire(rendu, ligne, 54, y, Affichage.GRIS, 1);
                y += 8;
            }
            y += 2;
        }
        affichage.petite().ecrire(rendu, "ECHAP : RETOUR", 54, 244, Affichage.GRIS, 1);
    }
}
