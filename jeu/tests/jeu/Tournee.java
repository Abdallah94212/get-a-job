package jeu;

import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Joue toute la campagne sans fenetre, en marchant touche par touche, et enregistre une capture a chaque replique,
 * chaque choix et chaque gros plan, puis a chaque nouvel objectif. Arguments : dossier de sortie, puis
 * facultativement l'indice de la reponse a choisir partout (0 par defaut).
 */
public final class Tournee {
    private Tournee() {
    }

    public static void main(String[] args) throws Exception {
        Path sortie = Path.of(args[0]);
        int reponse = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        Path partie = Files.createTempFile("partie", ".properties");
        Files.delete(partie);
        Robot robot = new Robot(partie, sortie);
        robot.images(40);
        robot.photo("menu");
        robot.frapper(KeyEvent.VK_E);
        String derniere = "";
        String dernierePlace = "";
        for (int tour = 0; tour < 600 && robot.partie() != null && !robot.partie().progression().terminee(); tour++) {
            EcranPartie p = robot.partie();
            for (int i = 0; i < 8000 && (p.enScene() || p.auBilan()); i++) {
                String place = p.positionScene();
                if (!place.equals(dernierePlace) && place.matches(".*(DIT|CHOIX|GROS_PLAN|SMS|ANNONCE|NOIR|CARTON|TITRE|RECIT)")) {
                    robot.images(place.endsWith("TITRE") || place.endsWith("CARTON") ? 50 : 24);
                    robot.photo(place.replace('#', '_').replace(':', '_'));
                    dernierePlace = place;
                }
                if (p.auBilan() && !dernierePlace.equals("bilan" + tour)) {
                    robot.images(20);
                    robot.photo("bilan");
                    dernierePlace = "bilan" + tour;
                }
                if (p.scenographeAuChoix() && reponse > 0) {
                    robot.images(2);
                    for (int k = 0; k < reponse; k++) {
                        robot.frapper(KeyEvent.VK_DOWN);
                    }
                }
                robot.frapper(KeyEvent.VK_E);
            }
            if (p.progression().terminee()) {
                break;
            }
            String ou = p.progression().courante().id() + "_" + p.progression().etape();
            if (!ou.equals(derniere)) {
                robot.images(6);
                robot.photo("jeu_" + ou + "_" + p.monde().carte());
                derniere = ou;
            }
            if (!robot.marcherVersObjectif() && !p.enScene()) {
                robot.photo("BLOQUE_" + ou);
                System.out.println("BLOQUE " + ou + " carte " + p.monde().carte());
                break;
            }
        }
        robot.laisserPasser();
        robot.images(10);
        robot.photo("fin");
        System.out.println("termine : " + robot.partie().progression().terminee() + ", drapeaux " + robot.partie().progression().drapeaux());
        System.out.println("sons joues : " + robot.jeu.audio.journal().stream().distinct().sorted().toList());
    }
}
