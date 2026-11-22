package getajob.entites;

import getajob.monde.Niveau;

/**
 * Un client qui attend sa commande devant chez lui. Il ne marche pas : il se tourne vers le héros quand celui-ci approche.
 * Une fois servi, il dit sa phrase et rentre (il disparaît du niveau).
 */
public class Client extends Personnage {
    private final String nom;
    private final String phrase;
    private boolean servi;

    public Client(String nom, String sprite, double x, double y, String phrase) {
        super(x, y, sprite);
        this.nom = nom;
        this.phrase = phrase;
    }

    @Override
    public void mettreAJour(double dt, Niveau niveau) {
        versDroite = niveau.joueur().x() > x;
        animer(dt, false);
    }

    public void servir() {
        servi = true;
    }

    public boolean servi() {
        return servi;
    }

    public String nom() {
        return nom;
    }

    public String phrase() {
        return phrase;
    }
}
