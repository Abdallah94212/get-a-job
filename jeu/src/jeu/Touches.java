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

    /**
     * La fenetre a perdu le clavier (Cmd+Tab, une boite du systeme...) : les touches relachees pendant ce temps ne nous
     * arrivent jamais. On les oublie toutes, sinon une touche restee "tenue" ne pourrait plus etre frappee (E et Echap
     * ne repondraient plus).
     */
    public void toutLacher() {
        tenues.clear();
        frappees.clear();
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

    /** P ou Tab : sortir ou ranger le telephone. */
    boolean telephone() {
        return frappee(KeyEvent.VK_P, KeyEvent.VK_TAB);
    }

    /** F : descendre du scooter. */
    boolean scooter() {
        return frappee(KeyEvent.VK_F);
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

    /** J : un coup de poing (ou un coup avec l'arme en main). */
    boolean poing() {
        return frappee(KeyEvent.VK_J);
    }

    /** K : un coup de pied. */
    boolean pied() {
        return frappee(KeyEvent.VK_K);
    }

    /** G : lacher l'arme qu'on tient. */
    boolean lacher() {
        return frappee(KeyEvent.VK_G);
    }
}
