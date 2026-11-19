package getajob;

/**
 * La caméra : le décor fait 4 260 pixels de large, l'écran 480. La caméra dit quelle partie de l'avenue on voit.
 * Elle garde le héros au centre, sans jamais montrer ce qu'il y a avant le début ou après la fin du décor.
 * Pour dessiner une entité, on retire x() à sa position : c'est le passage du monde à l'écran.
 */
public class Camera {
    private double x;

    public void suivre(Entite cible, int largeurDuMonde) {
        double voulu = cible.x() - Jeu.LARGEUR / 2.0;
        x = Math.max(0, Math.min(voulu, largeurDuMonde - Jeu.LARGEUR));
    }

    public int x() {
        return (int) Math.round(x);
    }
}
