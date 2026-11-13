package jeu;

import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/** Le clavier : les touches tenues, et celles qu'on vient juste d'enfoncer depuis la derniere image. */
public final class Touches {
    private final Set<Integer> tenues = new HashSet<>();
    private final Set<Integer> frappees = new HashSet<>();

    public void presser(int code) {
        if (tenues.add(code)) {
            frappees.add(code);
        }
    }

    public void relacher(int code) {
        tenues.remove(code);
    }

    /** A appeler a la fin de chaque image : une frappe ne compte qu'une fois. */
    public void finImage() {
        frappees.clear();
    }

    private static boolean dans(Set<Integer> touches, int... codes) {
        for (int code : codes) {
            if (touches.contains(code)) {
                return true;
            }
        }
        return false;
    }

    boolean tenue(int... codes) {
        return dans(tenues, codes);
    }

    boolean frappee(int... codes) {
        return dans(frappees, codes);
    }

    float axeX() {
        return (tenue(KeyEvent.VK_RIGHT, KeyEvent.VK_D) ? 1 : 0) - (tenue(KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A) ? 1 : 0);
    }

    float axeY() {
        return (tenue(KeyEvent.VK_DOWN, KeyEvent.VK_S) ? 1 : 0) - (tenue(KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W) ? 1 : 0);
    }

    boolean court() {
        return tenue(KeyEvent.VK_SHIFT);
    }

    /** E, Entree ou Espace : agir, parler, choisir, passer a la replique suivante. */
    boolean valider() {
        return frappee(KeyEvent.VK_E, KeyEvent.VK_ENTER, KeyEvent.VK_SPACE);
    }

    /** Echap : pause, retour, passer une cinematique. */
    boolean retour() {
        return frappee(KeyEvent.VK_ESCAPE);
    }

    boolean haut() {
        return frappee(KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W);
    }

    boolean bas() {
        return frappee(KeyEvent.VK_DOWN, KeyEvent.VK_S);
    }

    boolean gauche() {
        return frappee(KeyEvent.VK_LEFT, KeyEvent.VK_Q, KeyEvent.VK_A);
    }

    boolean droite() {
        return frappee(KeyEvent.VK_RIGHT, KeyEvent.VK_D);
    }
}
