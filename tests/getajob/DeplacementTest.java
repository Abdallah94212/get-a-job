package getajob;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Les déplacements du héros : le clavier, la vitesse, les obstacles, les bords de la rue. */
class DeplacementTest {
    private Clavier clavier;
    private Niveau niveau;
    private Joueur joueur;

    @BeforeEach
    void preparer() {
        clavier = new Clavier();
        niveau = new Niveau(clavier);
        niveau.ennemis().clear();                     // personne pour gêner
        joueur = niveau.joueur();
    }

    /** Fait tourner le niveau pendant une durée, par pas de 1/60 s, comme la vraie boucle. */
    private void attendre(double secondes) {
        for (double t = 0; t < secondes; t += 1.0 / 60) {
            niveau.mettreAJour(1.0 / 60);
        }
    }

    @Test
    void laFlecheDeDroiteFaitAvancerALaBonneVitesse() {
        double depart = joueur.x();
        clavier.appuyer(KeyEvent.VK_RIGHT);
        attendre(1.0);
        assertEquals(depart + Joueur.VITESSE, joueur.x(), 3, "95 pixels en une seconde");
    }

    @Test
    void sansToucheLeHerosNeBougePas() {
        double x = joueur.x();
        double y = joueur.y();
        attendre(1.0);
        assertEquals(x, joueur.x());
        assertEquals(y, joueur.y());
    }

    @Test
    void unObstacleArreteLeHeros() {
        // le banc du 12 : un obstacle de 1404 à 1436 en x, de 186 à 194 en y
        joueur.placer(1390, 191);
        clavier.appuyer(KeyEvent.VK_RIGHT);
        attendre(1.0);
        assertTrue(joueur.x() < 1404 - 5, "il reste devant le banc : " + joueur.x());
    }

    @Test
    void contreUnObstacleOnGlisseSurLAutreAxe() {
        joueur.placer(1390, 191);
        clavier.appuyer(KeyEvent.VK_RIGHT);
        clavier.appuyer(KeyEvent.VK_DOWN);
        attendre(1.0);
        assertTrue(joueur.y() > 200, "bloqué en x, il descend quand même vers la chaussée : " + joueur.y());
        assertTrue(joueur.x() > 1404, "une fois plus bas, il passe le banc : " + joueur.x());
    }

    @Test
    void onNeSortPasDeLaRue() {
        joueur.placer(400, 200);
        clavier.appuyer(KeyEvent.VK_UP);
        attendre(2.0);
        assertTrue(joueur.y() >= 188, "il s'arrête au pied des façades : " + joueur.y());
    }
}
