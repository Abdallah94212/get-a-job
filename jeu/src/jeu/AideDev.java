package jeu;

import decor.Rendu;
import histoire.Mission;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * AIDE AU DEVELOPPEMENT, A SUPPRIMER A LA FIN DU PROJET (ce fichier, dev/, ressources/dev/, et les lignes marquees
 * "AideDev" dans Jeu, Fenetre et EcranPartie). Active seulement avec -Dgaj.dev=true (./dev/lancer.sh) : F2 ouvre un
 * menu pour aller au debut de n'importe quelle mission (une vraie partie, jouee par le robot puis enregistree), et
 * quelques outils de test. En mode aide, la partie est enregistree a part : la vraie sauvegarde n'est pas touchee.
 */
final class AideDev {
    static final boolean ACTIVE = Boolean.getBoolean("gaj.dev");

    private record Entree(String texte, Runnable action) {
    }

    private final Jeu jeu;
    private boolean ouvert;
    private int choix;
    private String info = "";
    private float infoReste;

    AideDev(Jeu jeu) {
        this.jeu = jeu;
    }

    boolean ouvert() {
        return ouvert;
    }

    private List<Entree> entrees() {
        List<Entree> e = new ArrayList<>();
        for (Mission m : jeu.res.campagne.missions()) {
            e.add(new Entree("MISSION " + m.id() + "  " + m.titre(), () -> aller(m.id())));
        }
        EcranPartie p = jeu.partie();
        if (p != null) {
            e.add(new Entree("OBJECTIF SUIVANT (LE VALIDER)", p::devObjectifSuivant));
            e.add(new Entree("+500 EUROS", () -> p.progression().etat().gagner(500, "AIDE AU TEST")));
            e.add(new Entree("SOIGNER, REPOSER", () -> {
                p.progression().etat().soigner(100);
                p.progression().etat().reposer();
            }));
            e.add(new Entree("DONNER : 5 PIERRES, 5 CANETTES, UNE BARRE", () -> {
                p.progression().etat().inventaire().ajouter("pierre", 5);
                p.progression().etat().inventaire().ajouter("canette", 5);
                p.progression().etat().prendreArme("barre");
            }));
            e.add(new Entree("ARSENAL : PISTOLET, MITRAILLETTE, FUSIL, KATANA", () -> {
                histoire.Etat etat = p.progression().etat();
                for (String id : List.of("katana", "mitraillette", "fusil", "pistolet")) {
                    etat.prendreArme(id, p.monde().carte(), p.monde().joueur.x - 12, p.monde().joueur.y);
                }
                etat.inventaire().ajouter("balles_9", 120);
                etat.inventaire().ajouter("balles_45", 30);
                etat.inventaire().ajouter("balles_fusil", 90);
            }));
            e.add(new Entree("ARSENAL : LE KATANA RELIQUE ET LE PISTOLET LOURD", () -> {
                histoire.Etat etat = p.progression().etat();
                etat.prendreArme("pistolet_lourd", p.monde().carte(), p.monde().joueur.x - 12, p.monde().joueur.y);
                etat.prendreArme("katana_relique", p.monde().carte(), p.monde().joueur.x - 12, p.monde().joueur.y);
                etat.inventaire().ajouter("balles_45", 30);
            }));
            e.add(new Entree("COMBAT ICI : LA BANDE DE GETO (4, ARMES)", () -> p.devCombat(Combat.groupe("visite", 4))));
            e.add(new Entree("COMBAT ICI : 2 TIREURS ET 1 MITRAILLEUR",
                    () -> p.devCombat(List.of(Combat.TIREUR, Combat.MITRAILLEUR, Combat.TIREUR))));
            e.add(new Entree("COMBAT ICI : UNE BRUTE ET UN SABREUR", () -> p.devCombat(List.of(Combat.BRUTE, Combat.SABREUR))));
            e.add(new Entree("COMBAT ICI : 3 ALIENS", () -> p.devCombat(Combat.ALIEN, 3)));
            e.add(new Entree("COMBAT ICI : 2 GARS DE GETO", () -> p.devCombat(Combat.GUETTEUR, 2)));
            e.add(new Entree("LA ROUTE DE NAMEK, MAINTENANT", p::devRoute));
            e.add(new Entree("LE TEMPS : + 6 HEURES", () -> p.progression().etat().calendrier().passer(6 * 60)));
        }
        return e;
    }

    /** Une image, quand l'aide est ouverte (ou F2 pour l'ouvrir). Renvoie vrai si elle a pris le clavier. */
    boolean maj(float dt, Touches touches) {
        infoReste = Math.max(0, infoReste - dt);
        if (touches.frappee(KeyEvent.VK_F2, KeyEvent.VK_M)) {           // F2 (fn + F2 sur un Mac), ou M
            ouvert = !ouvert;
            return true;
        }
        if (!ouvert) {
            return false;
        }
        List<Entree> e = entrees();
        if (touches.retour()) {
            ouvert = false;
        } else if (touches.haut() || touches.bas()) {
            choix = Math.floorMod(choix + (touches.bas() ? 1 : -1), e.size());
        } else if (touches.valider()) {
            Entree x = e.get(Math.min(choix, e.size() - 1));
            x.action().run();
            info = "FAIT : " + x.texte();
            infoReste = 2.5f;
            ouvert = false;
        }
        return true;
    }

    /** Recharge la partie gardee au debut de cette mission (ressources/dev/), dans la sauvegarde de test. */
    private void aller(String mission) {
        Path source = jeu.res.dossier().resolve("dev").resolve(mission + ".properties");
        try {
            Files.copy(source, jeu.sauvegarde.fichier(), StandardCopyOption.REPLACE_EXISTING);
            jeu.continuer();
        } catch (IOException erreur) {
            info = "PAS DE PARTIE POUR " + mission + " : LANCE ./dev/raccourcis.sh";
            infoReste = 4f;
        }
    }

    void dessiner(Rendu rendu, Affichage affichage) {
        if (infoReste > 0) {
            rendu.rect(4, Rendu.H - 16, affichage.petite().largeur(info, 1) + 10, 11, 0x203820, 230);
            affichage.petite().ecrire(rendu, info, 9, Rendu.H - 14, Affichage.VERT, 1);
        }
        if (!ouvert) {
            String t = "M : MISSIONS DE TEST";
            int l = affichage.petite().largeur(t, 1) + 10;
            rendu.rect(Rendu.L - l - 4, 4, l, 11, 0x0A140A, 220);
            affichage.petite().ecrire(rendu, t, Rendu.L - l + 1, 6, 0x5ADA5A, 1);
            return;
        }
        List<Entree> e = entrees();
        int l = 250;
        int x = 8;
        int y = 8;
        int h = Math.min(Rendu.H - 16, 22 + e.size() * 10);
        rendu.rect(x, y, l, h, 0x0A140A, 240);
        rendu.rect(x, y, l, 2, 0x5ADA5A, 256);
        affichage.petite().ecrire(rendu, "MISSIONS DE TEST   HAUT, BAS, E : CHOISIR   ÉCHAP : FERMER", x + 6, y + 6, 0x5ADA5A, 1);
        int premier = Math.max(0, Math.min(choix - 10, e.size() - 22));
        for (int i = premier; i < e.size() && i < premier + 22; i++) {
            int ly = y + 18 + (i - premier) * 10;
            if (i == choix) {
                rendu.rect(x + 2, ly - 1, l - 4, 9, 0x1E3A1E, 256);
            }
            affichage.petite().ecrire(rendu, e.get(i).texte(), x + 8, ly, i == choix ? Affichage.BLANC : Affichage.GRIS, 1);
        }
    }
}
