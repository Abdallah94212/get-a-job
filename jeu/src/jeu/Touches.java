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
        return (tenue(KeyEvent.VK_RIGHT, KeyEvent.VK_D) ? 1 : 0) - (tenue(KeyEvent.VK_LEFT, KeyEvent.VK_Q) ? 1 : 0);
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

    /** E ou Entree : agir sur ce qui est devant soi, en jeu (Espace, en jeu, fait sauter). */
    boolean interagir() {
        return frappee(KeyEvent.VK_E, KeyEvent.VK_ENTER);
    }

    /** Espace, en jeu : sauter. */
    boolean saut() {
        return frappee(KeyEvent.VK_SPACE);
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
        return frappee(KeyEvent.VK_LEFT, KeyEvent.VK_Q);
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

    /** L : le balayage. */
    boolean balayage() {
        return frappee(KeyEvent.VK_L);
    }

    /** H : lancer (une canette, une pierre, ou l'arme qu'on tient). */
    boolean lancer() {
        return frappee(KeyEvent.VK_H);
    }

    /**
     * A : l'attaque. Elle choisit seule : de loin avec une arme a feu, un tir ; de pres, ou sans arme a feu, un coup (ou
     * un elan vers l'adversaire vise). En l'air, un coup saute. (A n'est plus la gauche : Q et la fleche s'en chargent.)
     */
    boolean attaque() {
        return frappee(KeyEvent.VK_A);
    }

    /** A maintenu : une arme automatique continue de tirer. */
    boolean attaqueTenue() {
        return tenue(KeyEvent.VK_A);
    }

    /** C : l'esquive, dans la direction tenue (ou en arriere). */
    boolean esquive() {
        return frappee(KeyEvent.VK_C);
    }

    /** X maintenu : la garde. X au moment ou un coup part : le contre. */
    boolean garde() {
        return tenue(KeyEvent.VK_X);
    }

    boolean contre() {
        return frappee(KeyEvent.VK_X);
    }

    /** R : recharger l'arme a feu. */
    boolean recharger() {
        return frappee(KeyEvent.VK_R);
    }

    /** V : l'arme suivante (les poings, puis chaque arme du sac). */
    boolean changerArme() {
        return frappee(KeyEvent.VK_V);
    }

    /** La fleche du bas (ou S) tenue : en l'air, A devient une attaque plongeante. */
    boolean basTenu() {
        return tenue(KeyEvent.VK_DOWN, KeyEvent.VK_S);
    }

    /** G : lacher l'arme qu'on tient. */
    boolean lacher() {
        return frappee(KeyEvent.VK_G);
    }
}
