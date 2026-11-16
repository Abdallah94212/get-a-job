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
        this(ressources, sauvegarde, reglages, new Audio(ressources.resolve("sons"), reglages, muet));
    }

    /** Avec un mixeur donne : un mixeur hors ligne, par exemple, pour mesurer ce qu'on entendrait. */
    Jeu(Path ressources, Sauvegarde sauvegarde, Reglages reglages, Audio audio) throws IOException {
        this.res = new Ressources(ressources);
        this.sauvegarde = sauvegarde;
        this.reglages = reglages;
        this.audio = audio;
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

    /**
     * Une image du jeu. Une erreur ne doit jamais figer la fenetre : elle est ecrite dans le journal des incidents,
     * l'ecran rend la main au joueur, et si elles se repetent on revient au menu (la partie est enregistree).
     */
    public void maj(float dt, Touches touches) {
        try {
            audio.tic(dt);
            ecran.maj(dt, touches);
        } catch (RuntimeException | StackOverflowError erreur) {
            incident("maj", erreur);
        }
    }

    public void dessiner(Rendu rendu) {
        try {
            rendu.tramage = reglages.trame;
            rendu.effacerTextes();
            ecran.dessiner(rendu);
        } catch (RuntimeException | StackOverflowError erreur) {
            incident("dessin", erreur);
        }
    }

    private int incidents;
    private long dernierIncident;
    private int rafale;

    /** Le nombre d'erreurs rattrapees depuis le lancement (les essais verifient qu'il reste a zero). */
    public int incidents() {
        return incidents;
    }

    private void incident(String ou, Throwable erreur) {
        incidents++;
        long maintenant = System.nanoTime();
        rafale = maintenant - dernierIncident < 5_000_000_000L ? rafale + 1 : 1;
        dernierIncident = maintenant;
        java.io.StringWriter trace = new java.io.StringWriter();
        erreur.printStackTrace(new java.io.PrintWriter(trace));
        String ligne = java.time.LocalDateTime.now() + " [" + ou + "] " + trace;
        System.err.print(ligne);
        try {
            java.nio.file.Path dossier = sauvegarde.dossier();
            java.nio.file.Files.createDirectories(dossier);
            java.nio.file.Files.writeString(dossier.resolve("incidents.log"), ligne, java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ignoree) {
            // le journal est un confort : on ne fige pas le jeu pour lui
        }
        try {
            if (rafale >= 3) {                                         // l'ecran ne s'en remet pas : retour au menu
                rafale = 0;
                ouvrirMenu();
            } else {
                ecran.secourir();
            }
        } catch (RuntimeException | StackOverflowError encore) {
            ouvrirMenu();
        }
    }

    /** La partie en cours, ou null quand on est au menu. */
    EcranPartie partie() {
        return ecran instanceof EcranPartie partie ? partie : null;
    }

    void ouvrirMenu() {
        audio.changerDEcran();
        ecran = new EcranMenu(this);
    }

    /** Efface la partie enregistree et recommence la campagne a son debut. */
    void nouvellePartie() {
        try {
            sauvegarde.effacer();
        } catch (IOException erreur) {
            System.err.println("Ancienne partie non effacee : " + erreur.getMessage());
        }
        audio.changerDEcran();
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
            audio.changerDEcran();
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
