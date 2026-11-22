package getajob;

import javax.swing.JFrame;

import getajob.moteur.Jeu;

/** Point d'entrée du programme : ouvre la fenêtre, y place le jeu, puis démarre la boucle de jeu. */
public class Main {
    public static void main(String[] args) {
        Jeu jeu = new Jeu();
        JFrame fenetre = new JFrame("GET A JOB");
        fenetre.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fenetre.setResizable(false);
        fenetre.add(jeu);
        fenetre.pack();
        fenetre.setLocationRelativeTo(null);
        fenetre.setVisible(true);
        jeu.demarrer();
    }
}
