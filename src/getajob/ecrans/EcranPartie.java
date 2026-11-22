package getajob.ecrans;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

import getajob.entites.Entite;
import getajob.entites.Joueur;
import getajob.monde.Niveau;
import getajob.monde.Tournee;
import getajob.moteur.Camera;
import getajob.moteur.Clavier;
import getajob.moteur.Jeu;
import getajob.moteur.Ressources;

/**
 * L'écran de la partie. Il relie les trois morceaux de la partie :
 *   - le Niveau (le monde et ses entités), qu'il fait vivre ;
 *   - la Tournee (les règles), qu'il consulte pour savoir si c'est gagné ou perdu ;
 *   - la Camera, qui suit le héros.
 * Puis il dessine : le décor, les entités de la plus éloignée à la plus proche, la flèche, et le bandeau d'informations.
 * Échap met en pause : on ne met plus rien à jour, on dessine seulement un voile par-dessus.
 */
public class EcranPartie extends Ecran {
    private final Niveau niveau;
    private final Tournee tournee;
    private final Camera camera = new Camera();
    private boolean enPause;
    private double temps;

    public EcranPartie(Jeu jeu) {
        super(jeu);
        niveau = new Niveau(jeu.clavier());
        tournee = new Tournee(niveau);
        camera.suivre(niveau.joueur(), Niveau.LARGEUR);
    }

    @Override
    public void mettreAJour(double dt, Clavier clavier) {
        if (clavier.vientDAppuyer(KeyEvent.VK_ESCAPE)) {
            enPause = !enPause;
        }
        if (enPause) {
            if (clavier.vientDAppuyer(KeyEvent.VK_Q, KeyEvent.VK_A)) {
                jeu.changerEcran(new EcranTitre(jeu));             // abandonner : retour au titre
            }
            return;
        }
        temps += dt;
        niveau.mettreAJour(dt);
        tournee.mettreAJour(dt);
        camera.suivre(niveau.joueur(), Niveau.LARGEUR);
        Tournee.Issue issue = tournee.issue();
        if (issue != Tournee.Issue.EN_COURS) {
            finir(issue);
        }
    }

    /** La nuit est finie : un récit de fin selon l'issue, puis le titre. */
    private void finir(Tournee.Issue issue) {
        Ecran titre = new EcranTitre(jeu);
        String aide = "MERCI D'AVOIR JOUÉ";
        Ecran fin = switch (issue) {
            case VICTOIRE -> {
                Ressources.jouerSon("mission");
                yield new EcranRecit(jeu, tournee.heure() + ". LE LOYER EST PAYÉ.", List.of(
                        "La régie a reçu ses 120 euros. Le courant reste allumé.",
                        "Trois livraisons, trois clients, et personne ne t'a dit merci.",
                        "Demain soir, il faudra recommencer."), aide, titre);
            }
            case PLUS_DE_VIES -> new EcranRecit(jeu, "SUR LE TROTTOIR.", List.of(
                    "Ils t'ont pris les sacs, puis le reste.",
                    "Youngssef ne te prêtera plus jamais ses livraisons.",
                    "À 6 heures, chez toi, la lumière s'éteint."), aide, titre);
            default -> new EcranRecit(jeu, "6:00. LE COMPTEUR SAUTE.", List.of(
                    "Il te manquait " + (Tournee.LOYER - niveau.joueur().argent()) + " euros.",
                    "La régie n'attend pas. Chez toi, il fait noir.",
                    "Le Maître Poulet, lui, ouvre à midi."), aide, titre);
        };
        jeu.changerEcran(fin);
    }

    @Override
    public void dessiner(Graphics2D g) {
        g.drawImage(niveau.decor(), -camera.x(), 0, null);
        dessinerLesPortes(g);
        for (Entite entite : niveau.entitesDansLOrdreDeDessin()) {
            entite.dessiner(g, camera);
        }
        dessinerLaFleche(g);
        dessinerLeBandeau(g);
        if (enPause) {
            g.setColor(new Color(18, 20, 28, 190));
            g.fillRect(0, 0, Jeu.LARGEUR, Jeu.HAUTEUR);
            ecrireAuCentre(g, "PAUSE", 120, 32, ORANGE);
            ecrireAuCentre(g, "ÉCHAP : REPRENDRE      Q : ABANDONNER", 150, 12, BLANC);
        }
    }

    /** Un liseré lumineux devant la porte où il faut aller : le comptoir, ou chez soi à la fin. */
    private void dessinerLesPortes(Graphics2D g) {
        Rectangle porte = switch (tournee.etape()) {
            case CHERCHER_LA_COMMANDE -> Niveau.COMPTOIR;
            case RENTRER -> Niveau.CHEZ_TOI;
            case LIVRER -> null;
        };
        if (porte != null) {
            int alpha = 70 + (int) (50 * Math.sin(temps * 4));
            g.setColor(new Color(240, 122, 26, alpha));
            g.fillRect(porte.x - camera.x(), porte.y, porte.width, porte.height);
        }
    }

    /** Un chevron au-dessus de la destination si on la voit ; sinon une flèche au bord de l'écran, avec la distance. */
    private void dessinerLaFleche(Graphics2D g) {
        double cible = tournee.destination();
        int ecranX = (int) cible - camera.x();
        int rebond = (int) (Math.sin(temps * 5) * 2);
        g.setColor(ORANGE);
        if (ecranX >= 0 && ecranX < Jeu.LARGEUR) {
            int haut = 132 + rebond;
            g.fillPolygon(new int[] {ecranX - 5, ecranX + 5, ecranX}, new int[] {haut, haut, haut + 6}, 3);
            return;
        }
        boolean aDroite = ecranX >= Jeu.LARGEUR;
        int bord = aDroite ? Jeu.LARGEUR - 8 : 8;
        int pointe = aDroite ? bord + 5 : bord - 5;
        g.fillPolygon(new int[] {bord, bord, pointe}, new int[] {150 + rebond, 162 + rebond, 156 + rebond}, 3);
        int metres = (int) Math.abs(cible - niveau.joueur().x()) / 10;
        g.setFont(Ressources.police(11));
        String distance = metres + " m";
        int largeur = g.getFontMetrics().stringWidth(distance);
        g.drawString(distance, aDroite ? bord - largeur - 4 : bord + 6, 160 + rebond);
    }

    /** En haut : l'heure, l'objectif, l'argent et les vies. En bas : le dernier message (un client, Old Geto...). */
    private void dessinerLeBandeau(Graphics2D g) {
        g.setColor(new Color(18, 20, 28, 200));
        g.fillRect(0, 0, Jeu.LARGEUR, 22);
        Joueur joueur = niveau.joueur();
        g.setFont(Ressources.police(16));
        g.setColor(BLANC);
        g.drawString(tournee.heure(), 8, 16);
        g.setFont(Ressources.police(12));
        g.setColor(ORANGE);
        g.drawString(tournee.objectif(), 56, 15);
        g.setColor(BLANC);
        String argent = joueur.argent() + " / " + Tournee.LOYER + " €";
        g.drawString(argent, Jeu.LARGEUR - 70 - g.getFontMetrics().stringWidth(argent), 15);
        for (int i = 0; i < Joueur.VIES_AU_DEPART; i++) {           // les vies : des petits carrés, rouges ou éteints
            g.setColor(i < joueur.vies() ? new Color(224, 74, 58) : new Color(70, 74, 88));
            g.fillRect(Jeu.LARGEUR - 52 + i * 14, 7, 9, 9);
        }
        String message = tournee.message();
        if (!message.isEmpty()) {
            g.setColor(new Color(18, 20, 28, 210));
            g.fillRect(0, Jeu.HAUTEUR - 24, Jeu.LARGEUR, 24);
            ecrireAuCentre(g, message, Jeu.HAUTEUR - 8, 12, BLANC);
        }
    }
}
