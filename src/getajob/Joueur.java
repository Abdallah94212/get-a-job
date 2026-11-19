package getajob;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/**
 * Le héros. Il marche au clavier, repousse les racketteurs (Espace), porte une commande et gagne de l'argent.
 * Il a trois vies ; après un coup, il est intouchable un court instant (il clignote) pour avoir le temps de fuir.
 */
public class Joueur extends Personnage {
    public static final double VITESSE = 95;              // pixels par seconde
    public static final int VIES_AU_DEPART = 3;
    private static final double DUREE_COUP = 0.22;          // la pose du coup de poing
    private static final double RECHARGE_COUP = 0.45;       // délai avant de pouvoir refrapper
    private static final double INTOUCHABLE = 1.5;          // après avoir été touché

    private final Clavier clavier;
    private int vies = VIES_AU_DEPART;
    private int argent;
    private boolean commande;
    private double pose;            // temps restant de la pose du coup
    private double recharge;        // temps restant avant de pouvoir refrapper
    private double intouchable;     // temps restant d'invincibilité

    public Joueur(double x, double y, Clavier clavier) {
        super(x, y, "heros");
        this.clavier = clavier;
    }

    @Override
    public void mettreAJour(double dt, Niveau niveau) {
        pose -= dt;
        recharge -= dt;
        intouchable -= dt;
        int dx = clavier.directionX();
        int dy = clavier.directionY();
        if (!frappe()) {                                     // pendant un coup, on ne marche pas
            deplacer(dx * VITESSE * dt, dy * VITESSE * 0.6 * dt, niveau);   // en profondeur, on avance moins vite
        }
        animer(dt, !frappe() && (dx != 0 || dy != 0));
        if (clavier.vientDAppuyer(KeyEvent.VK_SPACE) && recharge <= 0) {
            frapper(niveau);
        }
    }

    /** Un coup de poing devant soi : chaque racketteur dans la zone du coup est repoussé et étourdi. */
    private void frapper(Niveau niveau) {
        pose = DUREE_COUP;
        recharge = RECHARGE_COUP;
        Ressources.jouerSon("coup_sourd");
        Rectangle zone = zoneDeCoup();
        for (Ennemi ennemi : niveau.ennemis()) {
            if (zone.intersects(ennemi.contact())) {
                ennemi.recevoirCoup(versDroite, niveau);
            }
        }
    }

    /** Devant le héros, à hauteur de ses pieds : 28 pixels dans la direction où il regarde. */
    public Rectangle zoneDeCoup() {
        int debut = versDroite ? (int) x : (int) x - 28;
        return new Rectangle(debut, (int) y - 8, 28, 12);
    }

    /** Un racketteur l'a attrapé : une vie de moins, la commande perdue, et un petit recul. */
    public void etreTouche(boolean coupVenantDeDroite, Niveau niveau) {
        vies--;
        commande = false;
        intouchable = INTOUCHABLE;
        deplacer(coupVenantDeDroite ? -18 : 18, 0, niveau);
        Ressources.jouerSon("ui_refus");
    }

    public boolean peutEtreTouche() {
        return intouchable <= 0;
    }

    public void prendreCommande() {
        commande = true;
    }

    /** La commande est remise au client : il paie. */
    public void livrer(int prix) {
        commande = false;
        argent += prix;
    }

    public boolean porteUneCommande() {
        return commande;
    }

    public int vies() {
        return vies;
    }

    public int argent() {
        return argent;
    }

    public boolean frappe() {
        return pose > 0;
    }

    @Override
    protected BufferedImage image() {
        if (frappe()) {
            return Ressources.image("heros_poing");
        }
        if (intouchable > INTOUCHABLE - 0.3) {
            return Ressources.image("heros_touche");
        }
        return super.image();
    }

    @Override
    public void dessiner(Graphics2D g, Camera camera) {
        if (intouchable > 0 && (int) (intouchable * 10) % 2 == 0) {
            return;                                          // il clignote : une image sur deux, on ne le dessine pas
        }
        super.dessiner(g, camera);
        if (commande) {                                      // le sac du Maître Poulet, au bout du bras
            BufferedImage sac = Ressources.image("sac_poulet");
            int main = versDroite ? 6 : -6 - sac.getWidth();
            g.drawImage(sac, (int) Math.round(x) + main - camera.x(), (int) Math.round(y) - 26, null);
        }
    }
}
