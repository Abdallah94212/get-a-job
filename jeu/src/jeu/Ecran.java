package jeu;

import decor.Rendu;

/** Un etat du jeu qui occupe tout l'ecran : le menu principal ou la partie en cours. */
interface Ecran {
    /** Fait avancer l'ecran d'un pas de temps, selon les touches. */
    void maj(float dt, Touches touches);

    void dessiner(Rendu rendu);
}
