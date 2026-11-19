package jeu;

import histoire.Progression;
import histoire.Sauvegarde;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * AIDE AU DEVELOPPEMENT, A SUPPRIMER A LA FIN (avec tout le dossier dev/ et ressources/dev/).
 * Joue la campagne entiere sans fenetre, comme les essais, et enregistre une partie au debut de chaque mission :
 * ressources/dev/<mission>.properties. Le menu d'aide (F2, lance avec -Dgaj.dev=true) les recharge.
 * Lancer : ./dev/raccourcis.sh
 */
public final class GenererRaccourcis {
    public static void main(String[] args) throws Exception {
        Path dossier = Path.of("ressources", "dev");
        Files.createDirectories(dossier);
        Robot robot = new Robot(Files.createTempFile("raccourcis", ".properties"), null);
        robot.jeu.nouvellePartie();
        Progression p = robot.partie().progression();
        Set<String> faites = new HashSet<>();
        garder(dossier, robot, faites);
        for (int tour = 0; tour < 600 && !p.terminee(); tour++) {
            for (int i = 0; i < 6000 && robot.partie() != null && (robot.partie().enScene() || robot.partie().auBilan()
                    || robot.partie().auCalendrier() || robot.partie().auVolant() || robot.partie().encartOuvert()
                    || robot.partie().combat().actif()); i++) {
                if (robot.partie().auBilan()) {
                    garder(dossier, robot, faites);                    // la mission suivante n'a pas encore commence
                }
                if (robot.partie().auVolant()) {
                    robot.conduire();
                } else if (robot.partie().combat().actif() && !robot.partie().encartOuvert() && !robot.partie().enScene()) {
                    robot.combattre();
                } else {
                    robot.frapper(KeyEvent.VK_E);
                }
            }
            if (p.terminee()) {
                break;
            }
            if (!robot.avancerVersObjectif()) {
                robot.images(30);
            }
        }
        System.out.println(faites.size() + " raccourcis : " + faites);
        System.exit(0);
    }

    private static void garder(Path dossier, Robot robot, Set<String> faites) throws Exception {
        Progression p = robot.partie().progression();
        if (p.courante() == null || p.debutJoue() || !faites.add(p.courante().id())) {
            return;
        }
        Monde m = robot.partie().monde();
        new Sauvegarde(dossier.resolve(p.courante().id() + ".properties")).enregistrer(p, m.carte(), m.joueur.x, m.joueur.y);
    }
}
