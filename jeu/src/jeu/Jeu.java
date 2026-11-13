package jeu;

import decor.Rendu;
import histoire.Progression;
import histoire.Sauvegarde;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Le jeu entier, sans sa fenetre : il tient l'ecran courant (menu ou partie) et passe de l'un a l'autre.
 * On peut donc le faire tourner sans afficher quoi que ce soit, ce qui sert aux essais.
 */
public final class Jeu {
    final Ressources res;
    final Reglages reglages;
    final Audio audio;
    final Sauvegarde sauvegarde;
    final Affichage affichage;
    private Ecran ecran;
    private Runnable surQuitter = () -> { };
    private Runnable surReglages = () -> { };

    public Jeu(Path ressources, Sauvegarde sauvegarde, Reglages reglages, boolean muet) throws IOException {
        this.res = new Ressources(ressources);
        this.sauvegarde = sauvegarde;
        this.reglages = reglages;
        this.audio = new Audio(ressources.resolve("sons"), reglages, muet);
        this.affichage = new Affichage(res);
        ouvrirMenu();
    }

    /** Ce que la fenetre doit faire quand le joueur quitte, et quand il change un reglage d'affichage. */
    public void brancher(Runnable quitter, Runnable reglagesChanges) {
        surQuitter = quitter;
        surReglages = reglagesChanges;
    }

    public Reglages reglages() {
        return reglages;
    }

    public void maj(float dt, Touches touches) {
        ecran.maj(dt, touches);
    }

    public void dessiner(Rendu rendu) {
        rendu.tramage = reglages.trame;
        ecran.dessiner(rendu);
    }

    /** La partie en cours, ou null quand on est au menu. */
    EcranPartie partie() {
        return ecran instanceof EcranPartie partie ? partie : null;
    }

    void ouvrirMenu() {
        ecran = new EcranMenu(this);
    }

    /** Efface la partie enregistree et recommence la campagne a son debut. */
    void nouvellePartie() {
        try {
            sauvegarde.effacer();
        } catch (IOException erreur) {
            System.err.println("Ancienne partie non effacee : " + erreur.getMessage());
        }
        ecran = new EcranPartie(this, new Progression(res.campagne), res.campagne.carteDepart(), res.campagne.xDepart(),
                res.campagne.yDepart());
    }

    /** Reprend la partie enregistree. Si elle est illisible, on reste au menu. */
    void continuer() {
        try {
            Sauvegarde.Partie partie = sauvegarde.charger(res.campagne);
            if (!res.carteExiste(partie.carte())) {
                throw new IOException("carte inconnue " + partie.carte());
            }
            ecran = new EcranPartie(this, partie.progression(), partie.carte(), partie.x(), partie.y());
        } catch (IOException erreur) {
            System.err.println("Partie illisible : " + erreur.getMessage());
            audio.jouer("ui_refus", 0.7f, 0, 1);
        }
    }

    /** La progression enregistree, ou une progression vierge s'il n'y a pas de partie lisible. */
    Progression progressionEnregistree() {
        try {
            return sauvegarde.existe() ? sauvegarde.charger(res.campagne).progression() : new Progression(res.campagne);
        } catch (IOException erreur) {
            return new Progression(res.campagne);
        }
    }

    void reglagesChanges() {
        reglages.enregistrer();
        surReglages.run();
    }

    void quitter() {
        surQuitter.run();
    }
}
