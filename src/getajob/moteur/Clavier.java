package getajob.moteur;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Le clavier. Java nous prévient quand une touche est enfoncée ou relâchée (keyPressed, keyReleased) ;
 * le Clavier garde en mémoire :
 *   - les touches tenues en ce moment (pour marcher tant qu'on appuie) ;
 *   - les touches qu'on vient d'enfoncer pendant ce tour de boucle (pour une action qui ne compte qu'une fois : frapper).
 * Les flèches et ZQSD (clavier français) marchent toutes les deux ; WASD aussi, pour un clavier anglais.
 */
public class Clavier extends KeyAdapter {
    private final Set<Integer> tenues = new HashSet<>();
    private final Set<Integer> justeAppuyees = new HashSet<>();

    @Override
    public void keyPressed(KeyEvent e) {
        appuyer(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        relacher(e.getKeyCode());
    }

    public void appuyer(int touche) {
        if (tenues.add(touche)) {                 // add renvoie faux si elle était déjà tenue (la répétition automatique)
            justeAppuyees.add(touche);
        }
    }

    public void relacher(int touche) {
        tenues.remove(touche);
    }

    /** Appelé par la boucle à la fin de chaque tour. */
    public void finDuTour() {
        justeAppuyees.clear();
    }

    public boolean tenue(int... touches) {
        for (int t : touches) {
            if (tenues.contains(t)) {
                return true;
            }
        }
        return false;
    }

    public boolean vientDAppuyer(int... touches) {
        for (int t : touches) {
            if (justeAppuyees.contains(t)) {
                return true;
            }
        }
        return false;
    }

    /** -1 vers la gauche, +1 vers la droite, 0 sinon. */
    public int directionX() {
        int droite = tenue(KeyEvent.VK_RIGHT, KeyEvent.VK_D) ? 1 : 0;
        int gauche = tenue(KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A) ? 1 : 0;
        return droite - gauche;
    }

    /** -1 vers le haut (le trottoir), +1 vers le bas (la chaussée), 0 sinon. */
    public int directionY() {
        int bas = tenue(KeyEvent.VK_DOWN, KeyEvent.VK_S) ? 1 : 0;
        int haut = tenue(KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W) ? 1 : 0;
        return bas - haut;
    }
}
