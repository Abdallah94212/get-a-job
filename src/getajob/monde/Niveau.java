package getajob.monde;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import getajob.entites.Client;
import getajob.entites.Ennemi;
import getajob.entites.Entite;
import getajob.entites.Joueur;
import getajob.moteur.Clavier;
import getajob.moteur.Ressources;

/**
 * Le niveau : l'avenue, ses obstacles, et tout ce qui y vit (le héros, les racketteurs, les clients).
 * Il sait dire si un endroit est libre (bloque), et fait vivre toutes les entités à chaque tour.
 */
public class Niveau {
    public static final int LARGEUR = 4260;
    private static final int X_MIN = 12;
    private static final int X_MAX = 4250;
    private static final int Y_MIN = 188;          // le pied des façades
    private static final int Y_MAX = 264;          // le bord de la chaussée

    /** Devant la porte du Maître Poulet : c'est là qu'on prend les commandes. */
    public static final Rectangle COMPTOIR = new Rectangle(556, 186, 40, 20);
    /** Devant la porte du 12 : c'est chez le héros. */
    public static final Rectangle CHEZ_TOI = new Rectangle(1178, 186, 40, 20);

    private final BufferedImage decor = Ressources.image("avenue");
    private final List<Rectangle> obstacles = Ressources.obstacles();
    private final Joueur joueur;
    private final List<Client> clients = new ArrayList<>();
    private final List<Ennemi> ennemis = new ArrayList<>();

    public Niveau(Clavier clavier) {
        joueur = new Joueur(315, 216, clavier);       // sur le trottoir, devant la rangée de lampadaires
        // les trois clients de la nuit, du plus proche au plus loin
        clients.add(new Client("Tata Lili", "p_cliente", 1640, 198, "Enfin ! Il est encore chaud, au moins ?"));
        clients.add(new Client("Monsieur Diallo", "p_vieux", 2700, 198, "Tu remercieras Youngssef. Et fais attention en redescendant."));
        clients.add(new Client("le garagiste", "p_client", 3900, 198, "Quarante, c'est ça ? Tiens. Et file, ils tournent dans le coin."));
        // deux racketteurs au départ ; d'autres arrivent pendant la nuit (Tournee)
        ajouterEnnemi(900, 1350, 226);
        ajouterEnnemi(2050, 2550, 236);
    }

    public void ajouterEnnemi(double debutRonde, double finRonde, double y) {
        ennemis.add(new Ennemi(debutRonde, finRonde, y));
    }

    /** Vrai si ces pieds sortent de la rue ou touchent un obstacle (un banc, un poteau, une voiture garée). */
    public boolean bloque(Rectangle pieds) {
        if (pieds.x < X_MIN || pieds.x + pieds.width > X_MAX || pieds.y < Y_MIN || pieds.y + pieds.height > Y_MAX) {
            return true;
        }
        for (Rectangle obstacle : obstacles) {
            if (obstacle.intersects(pieds)) {
                return true;
            }
        }
        return false;
    }

    /** Un tour de vie pour tout le monde. Chacun sait ce qu'il doit faire : c'est sa méthode mettreAJour. */
    public void mettreAJour(double dt) {
        for (Entite entite : entites()) {
            entite.mettreAJour(dt, this);
        }
    }

    /** Toutes les entités présentes (les clients servis sont rentrés chez eux). */
    public List<Entite> entites() {
        List<Entite> toutes = new ArrayList<>();
        toutes.add(joueur);
        toutes.addAll(ennemis);
        for (Client client : clients) {
            if (!client.servi()) {
                toutes.add(client);
            }
        }
        return toutes;
    }

    /** Les entités dans l'ordre où les dessiner : la plus haute (au fond, près des façades) d'abord. C'est la profondeur. */
    public List<Entite> entitesDansLOrdreDeDessin() {
        List<Entite> triees = entites();
        triees.sort(Comparator.comparingDouble(Entite::y));
        return triees;
    }

    public Joueur joueur() {
        return joueur;
    }

    public List<Ennemi> ennemis() {
        return ennemis;
    }

    public List<Client> clients() {
        return clients;
    }

    public BufferedImage decor() {
        return decor;
    }
}
