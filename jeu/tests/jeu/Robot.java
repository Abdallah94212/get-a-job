package jeu;

import decor.Capture;
import decor.Porte;
import decor.Rendu;
import decor.Scene;
import histoire.Objectif;
import histoire.Progression;
import histoire.Sauvegarde;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Un joueur automatique pour les essais : il fait tourner le vrai jeu sans fenetre, appuie sur les touches,
 * et se rend a chaque objectif en passant par les portes, comme le ferait un joueur.
 */
final class Robot {
    static final float PAS = 1f / 30f;

    final Jeu jeu;
    final Touches touches = new Touches();
    final Rendu rendu = new Rendu();
    private final Path captures;
    private int numero;
    int compteur;

    Robot(Path sauvegarde, Path captures) throws IOException {
        this(sauvegarde, captures, null);
    }

    /** Avec un mixeur hors ligne : le son est vraiment calcule, image par image, et on peut le mesurer. */
    Robot(Path sauvegarde, Path captures, Reglages reglages) throws IOException {
        Reglages r = reglages == null ? new Reglages(null) : reglages;
        this.jeu = reglages == null ? new Jeu(Path.of("ressources"), new Sauvegarde(sauvegarde), r, true)
                : new Jeu(Path.of("ressources"), new Sauvegarde(sauvegarde), r, Audio.horsLigne(Path.of("ressources", "sons"), r));
        this.captures = captures;
    }

    /** Une image du jeu, sans toucher au clavier. */
    void image() {
        compteur++;
        jeu.maj(PAS, touches);
        touches.finImage();
    }

    void images(int nombre) {
        for (int i = 0; i < nombre; i++) {
            image();
        }
    }

    /** Appuie sur une touche le temps d'une image. */
    void frapper(int code) {
        touches.presser(code);
        image();
        touches.relacher(code);
        image();
    }

    /** Enregistre l'ecran courant si un dossier de captures a ete donne. */
    void photo(String nom) throws IOException {
        if (captures == null) {
            return;
        }
        Files.createDirectories(captures);
        jeu.dessiner(rendu);
        Capture.enregistrer(rendu, captures.resolve(String.format("%02d_%s.png", ++numero, nom)), 2);
    }

    EcranPartie partie() {
        return jeu.partie();
    }

    /** Regarde les cinematiques et les ecrans de fin de mission jusqu'a rendre la main au joueur. */
    void laisserPasser() {
        for (int i = 0; i < 6000 && partie() != null && (partie().enScene() || partie().auBilan() || partie().auCalendrier() || partie().encartOuvert()
                || partie().combat().actif()); i++) {
            if (partie().combat().actif() && !partie().encartOuvert() && !partie().enScene()) {
                combattre();
            } else {
                frapper(KeyEvent.VK_E);
            }
        }
    }

    /** Un pas de combat : se placer face a l'ennemi le plus proche, a portee de poing, et frapper. */
    void combattre() {
        Monde monde = partie().monde();
        Combat.Ennemi cible = null;
        for (Combat.Ennemi e : partie().combat().ennemis()) {
            if (e.vivant() && (cible == null || Math.abs(e.acteur.x - monde.joueur.x) < Math.abs(cible.acteur.x - monde.joueur.x))) {
                cible = e;
            }
        }
        if (cible == null) {
            image();
            return;
        }
        float cote = cible.acteur.x >= monde.joueur.x ? 1 : -1;
        for (float essai : new float[] {cote * 18, -cote * 18, cote * 10, -cote * 10}) {     // un cote libre, contre lui
            if (!monde.scene().bloque(cible.acteur.x - essai, cible.acteur.y, decor.Joueur.RAYON)) {
                monde.joueur.x = cible.acteur.x - essai;
                monde.joueur.y = cible.acteur.y;
                cote = Math.signum(essai);
                break;
            }
        }
        monde.joueur.versDroite = cote > 0;
        frapper(compteur % 3 == 0 ? KeyEvent.VK_K : KeyEvent.VK_J);
        images(8);
    }

    /** La premiere porte a prendre depuis la carte courante pour aller vers une autre, ou null. */
    Porte porteVers(String but) {
        Monde monde = partie().monde();
        Map<String, Porte> depart = new HashMap<>();
        Deque<String> file = new ArrayDeque<>();
        depart.put(monde.carte(), null);
        file.add(monde.carte());
        while (!file.isEmpty()) {
            String ici = file.poll();
            if (ici.equals(but)) {
                return depart.get(ici);
            }
            for (Porte porte : jeu.res.carte(ici).portes) {
                String de = ici;
                boolean ferme = jeu.res.campagne.fermetures().stream().anyMatch(f -> f.depuis().equals(de)
                        && f.vers().equals(porte.cible()) && partie().progression().verifie(f.condition()));
                if (!ferme && !depart.containsKey(porte.cible()) && jeu.res.carteExiste(porte.cible())) {
                    depart.put(porte.cible(), ici.equals(monde.carte()) ? porte : depart.get(ici));
                    file.add(porte.cible());
                }
            }
        }
        return null;
    }

    /** Se pose devant une porte et la prend. Renvoie vrai si on a change de carte. */
    boolean prendre(Porte porte) {
        Monde monde = partie().monde();
        Scene scene = monde.scene();
        String avant = monde.carte();
        monde.joueur.x = (porte.x1() + porte.x2()) / 2f;
        monde.joueur.y = Math.max(scene.yMin, Math.min(scene.yMax, porte.yMax()) - 1);
        frapper(KeyEvent.VK_E);
        return !monde.carte().equals(avant);
    }

    // ------------------------------------------------------------------ la marche, touche par touche

    private static final int CASE = 4;

    private boolean libre(Scene scene, int cx, int cy, boolean rue, float butX) {
        float x = cx * CASE;
        float y = cy * CASE;
        // Le robot garde une marge autour des meubles : il s'arrete a quelques pixels pres et ne doit pas s'y accrocher.
        if (scene.bloque(x, y, decor.Joueur.RAYON + 4)
                || (scene.bloque(x, y - 4, decor.Joueur.RAYON + 4) && y - 4 >= scene.yMin)
                || (scene.bloque(x, y + 4, decor.Joueur.RAYON + 4) && y + 4 <= scene.yMax)) {
            return false;
        }
        // Dans l'avenue, le robot marche sur la chaussee, loin des facades, sauf pour s'approcher de son but.
        return !rue || y >= 216 || Math.abs(x - butX) < 40 || Math.abs(x - partie().monde().joueur.x) < 40;
    }

    /**
     * Va a pied, en tenant les touches comme un joueur, jusqu'a une case qui satisfait le but.
     * Renvoie faux s'il n'existe pas de chemin ou si le robot n'arrive pas au bout.
     */
    boolean marcher(java.util.function.BiPredicate<Float, Float> but, float butX, float marge) {
        Monde monde = partie().monde();
        Scene scene = monde.scene();
        String carte = monde.carte();
        boolean rue = carte.startsWith("avenue");
        int x0 = Math.round(monde.joueur.x / CASE);
        int y0 = Math.round(monde.joueur.y / CASE);
        Map<Long, Long> origine = new HashMap<>();
        Deque<long[]> file = new ArrayDeque<>();
        long depart = ((long) x0 << 32) | (y0 & 0xFFFFFFFFL);
        origine.put(depart, depart);
        file.add(new long[] {x0, y0});
        long arrivee = -1;
        float meilleur = -1;
        while (!file.isEmpty()) {
            long[] c = file.poll();
            long cle = (c[0] << 32) | (c[1] & 0xFFFFFFFFL);
            float px = c[0] * (float) CASE;
            float py = c[1] * (float) CASE;
            // Le robot s'arrete a quelques pixels pres : le but doit rester vrai tout autour de la case choisie.
            if (but.test(px - marge, py - marge) && but.test(px + marge, py - marge) && but.test(px - marge, py + marge)
                    && but.test(px + marge, py + marge)
                    && c[1] > meilleur) {
                meilleur = c[1];
                arrivee = cle;
            }
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                long nx = c[0] + d[0];
                long ny = c[1] + d[1];
                long voisine = (nx << 32) | (ny & 0xFFFFFFFFL);
                if (!origine.containsKey(voisine) && libre(scene, (int) nx, (int) ny, rue, butX)) {
                    origine.put(voisine, cle);
                    file.add(new long[] {nx, ny});
                }
            }
        }
        if (arrivee < 0) {
            System.out.println("  pas de chemin depuis " + monde.joueur.x + "," + monde.joueur.y + " cases " + origine.size());
            return false;
        }
        java.util.List<long[]> chemin = new java.util.ArrayList<>();
        for (long c = arrivee; c != depart; c = origine.get(c)) {
            chemin.add(0, new long[] {c >> 32, (int) c});
        }
        boolean arrive = true;
        for (long[] c : chemin) {
            float cx = c[0] * CASE;
            float cy = c[1] * CASE;
            int essais = 0;
            while (Math.abs(monde.joueur.x - cx) > 3f || Math.abs(monde.joueur.y - cy) > 3f) {
                tenir(KeyEvent.VK_RIGHT, cx - monde.joueur.x > 3f);
                tenir(KeyEvent.VK_LEFT, monde.joueur.x - cx > 3f);
                tenir(KeyEvent.VK_DOWN, cy - monde.joueur.y > 3f);
                tenir(KeyEvent.VK_UP, monde.joueur.y - cy > 3f);
                image();
                if (++essais > 200 || partie() == null || !monde.carte().equals(carte) || partie().enScene()) {
                    if (essais > 200) {
                        System.out.println("  coince en " + monde.joueur.x + "," + monde.joueur.y + " vers " + cx + "," + cy);
                    }
                    arrive = false;
                    break;
                }
            }
            if (!arrive) {
                break;
            }
        }
        for (int code : new int[] {KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_UP, KeyEvent.VK_SHIFT}) {
            touches.relacher(code);
        }
        image();
        return arrive || !monde.carte().equals(carte);
    }

    private void tenir(int code, boolean enfoncee) {
        if (enfoncee) {
            touches.presser(code);
        } else {
            touches.relacher(code);
        }
    }

    /** Comme avancerVersObjectif, mais en marchant jusqu'a la cible au lieu de s'y poser. */
    boolean marcherVersObjectif() {
        Progression progression = partie().progression();
        Monde monde = partie().monde();
        Objectif objectif = progression.objectif();
        if (objectif.genre() == Objectif.Genre.DELEGUER || objectif.genre() == Objectif.Genre.ENCAISSER) {
            return avancerVersObjectif();                              // ca se fait au telephone, pas a pied
        }
        String but = jeu.res.campagne.carteDe(objectif);
        int etape = progression.etape();
        int rang = progression.rang();
        String carte = monde.carte();
        if (!carte.equals(but)) {
            Porte porte = porteVers(but);
            if (porte == null || !marcher((x, y) -> porte.devant(x, y), (porte.x1() + porte.x2()) / 2f, porte.automatique() ? 0 : 4)) {
                return false;
            }
            if (monde.carte().equals(carte) && porte.automatique()) {
                int versLeBout = (porte.x1() + porte.x2()) / 2f > monde.joueur.x ? KeyEvent.VK_RIGHT : KeyEvent.VK_LEFT;
                touches.presser(versLeBout);
                for (int i = 0; i < 40 && monde.carte().equals(carte); i++) {
                    image();
                }
                touches.relacher(versLeBout);
            } else if (monde.carte().equals(carte)) {
                frapper(KeyEvent.VK_E);
            }
            return !monde.carte().equals(carte);
        }
        float[] ou = null;
        if (objectif.genre() == Objectif.Genre.PARLER) {
            for (Monde.Habitant habitant : monde.habitants()) {
                if (habitant.figurant.id().equals(objectif.cible())) {
                    ou = new float[] {habitant.acteur.x, habitant.acteur.y};
                }
            }
        } else if (objectif.genre() == Objectif.Genre.AGIR || objectif.genre() == Objectif.Genre.ATTEINDRE) {
            ou = jeu.res.position(jeu.res.campagne.points().get(objectif.cible()).lieu());
        }
        if (ou == null) {
            return false;
        }
        float[] cible = ou;
        boolean arrive = marcher((x, y) -> Math.abs(x - cible[0]) <= Monde.PORTEE_X - 6 && Math.abs(y - cible[1]) <= Monde.PORTEE_Y - 6,
                cible[0], 0);
        if (progression.etape() != etape || progression.rang() != rang) {
            return true;
        }
        if (!arrive) {
            System.out.println("  DBG pas arrive pres de " + cible[0] + "," + cible[1] + " : " + monde.joueur.x + "," + monde.joueur.y
                    + " scene " + partie().positionScene());
            return false;
        }
        frapper(KeyEvent.VK_E);
        if (progression.etape() == etape && progression.rang() == rang) {
            System.out.println("  DBG E sans effet en " + monde.joueur.x + "," + monde.joueur.y + " cible " + monde.cible() + " vise " + cible[0]);
        }
        return progression.etape() != etape || progression.rang() != rang;
    }

    /**
     * Fait un pas vers l'objectif en cours : une porte a franchir, ou la cible elle-meme.
     * Renvoie faux si l'objectif est hors d'atteinte : c'est qu'une mission ne peut pas etre terminee.
     */
    boolean avancerVersObjectif() {
        Progression progression = partie().progression();
        Monde monde = partie().monde();
        Objectif objectif = progression.objectif();
        if (objectif.genre() == Objectif.Genre.DELEGUER) {
            if (deleguer(objectif.cible(), false)) {
                return true;
            }
            // personne ne lui fait assez confiance : on gagne celle de Youngssef, des livraisons payees avec une prime
            histoire.Organisation orga = progression.etat().organisation();
            for (int tour = 0; tour < 10; tour++) {
                histoire.Organisation.Membre y = orga.membre("youngssef");
                deleguer("livraisons", true);
                for (int i = 0; i < 30 * 900 && !y.libre(); i++) {
                    image();
                }
                if (deleguer(objectif.cible(), false)) {
                    return true;
                }
            }
            return false;
        }
        if (objectif.genre() == Objectif.Genre.ENCAISSER) {               // on attend que l'operation se termine
            int etape = progression.etape();
            int rang = progression.rang();
            for (int i = 0; i < 30 * 900 && progression.etape() == etape && progression.rang() == rang; i++) {
                image();
            }
            return progression.etape() != etape || progression.rang() != rang;
        }
        String but = jeu.res.campagne.carteDe(objectif);
        if (!monde.carte().equals(but)) {
            Porte porte = porteVers(but);
            return porte != null && prendre(porte);
        }
        float[] ou = null;
        if (objectif.genre() == Objectif.Genre.PARLER) {
            for (Monde.Habitant habitant : monde.habitants()) {
                if (habitant.figurant.id().equals(objectif.cible())) {
                    ou = new float[] {habitant.acteur.x, habitant.acteur.y};
                }
            }
        } else if (objectif.genre() == Objectif.Genre.AGIR || objectif.genre() == Objectif.Genre.ATTEINDRE) {
            ou = jeu.res.position(jeu.res.campagne.points().get(objectif.cible()).lieu());
        }
        if (ou == null) {
            return false;
        }
        Scene scene = monde.scene();
        monde.joueur.x = ou[0];
        monde.joueur.y = Math.max(scene.yMin, Math.min(scene.yMax, ou[1]));
        if (Math.abs(monde.joueur.y - ou[1]) > Monde.PORTEE_Y) {
            return false;
        }
        int etape = progression.etape();
        int rang = progression.rang();
        if (objectif.genre() == Objectif.Genre.ATTEINDRE) {
            image();
        } else {
            frapper(KeyEvent.VK_E);
        }
        return progression.etape() != etape || progression.rang() != rang;
    }

    /**
     * Confie une operation depuis le telephone, touche par touche, comme un joueur : P, OPERATIONS, l'operation, puis
     * E sur le petit que le telephone propose (le mieux place). * : la premiere operation de la liste.
     */
    boolean deleguer(String operation, boolean prime) {
        Progression progression = partie().progression();
        int etape = progression.etape();
        int rang = progression.rang();
        frapper(KeyEvent.VK_P);
        frapper(KeyEvent.VK_RIGHT);                                    // la grille des applis : la troisieme
        frapper(KeyEvent.VK_RIGHT);
        frapper(KeyEvent.VK_E);                                        // OPERATIONS
        java.util.List<histoire.Organisation.Operation> ops = progression.etat().organisation().disponibles(progression);
        int k = 0;
        for (int i = 0; i < ops.size(); i++) {
            if (ops.get(i).id().equals(operation)) {
                k = i;
            }
        }
        for (int i = 0; i < k; i++) {
            frapper(KeyEvent.VK_DOWN);
        }
        frapper(KeyEvent.VK_E);                                        // l'operation
        if (prime) {
            frapper(KeyEvent.VK_RIGHT);                                // la paie : avec une prime
        }
        frapper(KeyEvent.VK_E);                                        // le petit propose : on lance
        if (partie().auTelephone()) {
            frapper(KeyEvent.VK_P);
        }
        return progression.etape() != etape || progression.rang() != rang;
    }

    /** Dans une scene, choisit la reponse numero rang (0 : la premiere) au prochain choix, puis laisse la scene finir. */
    void repondre(int rangReponse) {
        for (int i = 0; i < 2000 && partie() != null && partie().enScene(); i++) {
            if (partie().scenographeAuChoix()) {
                for (int k = 0; k < rangReponse; k++) {
                    frapper(KeyEvent.VK_DOWN);
                }
                frapper(KeyEvent.VK_E);
                return;
            }
            frapper(KeyEvent.VK_E);
        }
    }
}
