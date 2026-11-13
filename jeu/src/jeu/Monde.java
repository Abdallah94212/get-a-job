package jeu;

import decor.Acteur;
import decor.Joueur;
import decor.Porte;
import decor.Rendu;
import decor.Scene;
import histoire.Figurant;
import histoire.Objectif;
import histoire.Point;
import histoire.Progression;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Le quartier tel qu'il est a ce moment de l'histoire : la carte ou se tient le joueur, les personnages presents,
 * les objets sur lesquels on peut agir, la camera. Il ne decide rien de l'histoire : il la met dans le decor.
 */
public final class Monde {
    static final float PORTEE_X = 30f;
    static final float PORTEE_Y = 40f;
    private static final float VUE_GARDE = 92f;
    private static final float COULOIR_GARDE = 20f;

    /** Ce sur quoi la touche E agirait maintenant : un personnage, un point du decor ou une porte. */
    record Cible(Objectif.Genre genre, String id, String invite, Porte porte) {
    }

    /** Un personnage de l'histoire pose dans la carte courante. */
    static final class Habitant {
        final Figurant figurant;
        final Acteur acteur;
        final float origine;
        int sens = 1;

        Habitant(Figurant figurant, Acteur acteur) {
            this.figurant = figurant;
            this.acteur = acteur;
            this.origine = acteur.x;
            this.sens = figurant.miroir() ? -1 : 1;
        }
    }

    final Joueur joueur = new Joueur();
    private final Ressources res;
    private final Progression progression;
    private final List<Habitant> habitants = new ArrayList<>();
    private final Map<String, Acteur> points = new HashMap<>();
    private final Acteur heros;
    private Scene scene;
    private String carte;
    private float camX;
    private long tempsMs;
    private float chemin;
    private boolean scenePilote;

    public Monde(Ressources res, Progression progression) {
        this.res = res;
        this.progression = progression;
        this.heros = new Acteur("toi", null, 0, 0);
    }

    public String carte() {
        return carte;
    }

    Scene scene() {
        return scene;
    }

    float camX() {
        return camX;
    }

    long tempsMs() {
        return tempsMs;
    }

    List<Habitant> habitants() {
        return habitants;
    }

    Habitant habitant(String id) {
        for (Habitant h : habitants) {
            if (h.figurant.id().equals(id)) {
                return h;
            }
        }
        return null;
    }

    /** L'acteur du heros : une cinematique jouee sur place peut le deplacer elle-meme. */
    Acteur heros() {
        return heros;
    }

    /** Pendant une scene sur place, c'est elle qui deplace le heros : on ne recopie plus la position du joueur. */
    void piloter(boolean parLaScene) {
        if (parLaScene && !scenePilote) {
            heros.x = joueur.x;
            heros.y = joueur.y;
        } else if (!parLaScene && scenePilote) {
            joueur.x = heros.x;
            joueur.y = heros.y;
        }
        scenePilote = parLaScene;
    }

    boolean dehors() {
        return Sonorisation.dehors(carte);
    }

    /** Le joueur conduit le scooter de Youngssef : seulement dans les rues, on le gare en entrant quelque part. */
    boolean enScooter() {
        return progression.drapeau("scooter") && dehors();
    }

    boolean charge() {
        return progression.drapeau("charge");
    }

    /** Le chemin parcouru par le joueur a la derniere image, pour le bruit des pas. */
    float chemin() {
        return chemin;
    }

    /** Change de carte, pose le joueur a l'arrivee et remet en place ceux qui s'y trouvent. */
    public void aller(String nom, float x, float y) {
        if (!nom.equals(carte)) {
            habitants.clear();
        }
        scene = res.carte(nom);
        carte = nom;
        joueur.x = x;
        joueur.y = y;
        heros.x = x;
        heros.y = y;
        camX = cibleCamera();
        peupler();
    }

    /** Remet dans la carte les personnages et les objets que l'histoire y place en ce moment. */
    public void peupler() {
        Map<String, Habitant> avant = new HashMap<>();
        for (Habitant h : habitants) {
            avant.put(h.figurant.id(), h);
        }
        habitants.clear();
        points.clear();
        for (Figurant figurant : res.campagne.figurants().values()) {
            if (figurant.lieu().carte().equals(carte) && progression.present(figurant)) {
                float[] p = res.position(figurant.lieu());
                Acteur acteur = new Acteur(figurant.id(), res.image(figurant.image()), p[0], p[1]);
                acteur.pas = figurant.amplitude() > 0 ? res.image(figurant.image() + "_2") : null;
                Habitant habitant = new Habitant(figurant, acteur);
                Habitant ancien = avant.get(figurant.id());
                if (ancien != null) {                                  // il etait deja la : il garde sa place et son regard
                    acteur.x = ancien.acteur.x;
                    acteur.y = ancien.acteur.y;
                    habitant.sens = ancien.sens;
                }
                habitants.add(habitant);
            }
        }
        for (Point point : res.campagne.points().values()) {
            if (point.lieu().carte().equals(carte) && point.image() != null && (progression.attendue(Objectif.Genre.AGIR, point.id())
                    || progression.attendue(Objectif.Genre.ATTEINDRE, point.id()))) {
                float[] p = res.position(point.lieu());
                Acteur objet = new Acteur(point.id(), res.image(point.image()), p[0], p[1]);
                objet.hauteur = point.hauteur();
                objet.ombre = point.hauteur() == 0;
                points.put(point.id(), objet);
            }
        }
        habiller();
    }

    /** Le dessin et la vitesse du heros, selon qu'il marche, conduit le scooter ou porte des caisses. */
    private void habiller() {
        if (enScooter()) {
            heros.texture = res.image("heros_scooter");
            heros.pas = res.image("heros_scooter_2");
            joueur.marche = 170;
            joueur.course = 240;
        } else if (charge()) {
            heros.texture = res.image("heros_charge");
            heros.pas = res.image("heros_charge_2");
            joueur.marche = 62;
            joueur.course = 62;
        } else {
            heros.texture = res.image("heros");
            heros.pas = res.image("heros_2");
            joueur.marche = Joueur.MARCHE;
            joueur.course = Joueur.COURSE;
        }
    }

    private float cibleCamera() {
        float voulu = joueur.x - Rendu.L / 2f + (joueur.versDroite ? 40 : -40);
        float max = Math.max(scene.xMin - 12, scene.xMax + 12 - Rendu.L);
        return Math.max(scene.xMin - 12, Math.min(max, voulu));
    }

    /** Fait avancer le joueur, les patrouilles et la camera d'un pas de temps. */
    public void avancer(float dx, float dy, boolean court, float dt) {
        tempsMs += Math.round(dt * 1000);
        habiller();
        float x0 = joueur.x;
        float y0 = joueur.y;
        joueur.avancer(scene, dx, dy, court, dt);
        chemin = (float) Math.hypot(joueur.x - x0, joueur.y - y0);
        bouger(dt);
        camX += (cibleCamera() - camX) * Math.min(1f, dt * 6f);
    }

    private void bouger(float dt) {
        for (Habitant habitant : habitants) {
            Figurant f = habitant.figurant;
            if (f.amplitude() > 0) {
                habitant.acteur.x += habitant.sens * f.vitesse() * dt;
                if (Math.abs(habitant.acteur.x - habitant.origine) > f.amplitude()) {
                    habitant.sens = habitant.acteur.x > habitant.origine ? -1 : 1;
                }
                habitant.acteur.marche = true;
            }
        }
    }

    /** Le temps passe sans que le joueur bouge : pendant un dialogue, une pause. Les patrouilles, elles, continuent. */
    void attendre(float dt) {
        tempsMs += Math.round(dt * 1000);
        chemin = 0;
        joueur.enMouvement = false;
    }

    private boolean aPortee(float x, float y) {
        return Math.abs(joueur.x - x) <= PORTEE_X && Math.abs(joueur.y - y) <= PORTEE_Y;
    }

    /** Ce sur quoi E agirait : d'abord ce que l'histoire attend, puis un passant, puis une porte. Null s'il n'y a rien. */
    Cible cible() {
        for (Point point : res.campagne.points().values()) {
            if (point.lieu().carte().equals(carte) && progression.attendue(Objectif.Genre.AGIR, point.id())) {
                float[] p = res.position(point.lieu());
                if (aPortee(p[0], p[1])) {
                    return new Cible(Objectif.Genre.AGIR, point.id(), point.invite(), null);
                }
            }
        }
        Habitant proche = null;
        for (Habitant habitant : habitants) {
            if (habitant.acteur.texture == null) {
                continue;
            }
            boolean attendu = progression.attendue(Objectif.Genre.PARLER, habitant.figurant.id());
            boolean bavard = habitant.figurant.repliques().stream().anyMatch(r -> progression.verifie(r.condition()));
            if (aPortee(habitant.acteur.x, habitant.acteur.y) && (attendu || bavard) && (proche == null || attendu)) {
                proche = habitant;
            }
        }
        if (proche != null) {
            return new Cible(Objectif.Genre.PARLER, proche.figurant.id(), "E : PARLER À " + proche.figurant.nom(), null);
        }
        Porte porte = scene.porteEn(joueur.x, joueur.y);
        return porte == null ? null : new Cible(Objectif.Genre.ALLER, porte.cible(), null, porte);
    }

    /** Le point a atteindre que le joueur vient d'approcher, ou null. */
    String pointAtteint() {
        Objectif objectif = progression.objectif();
        if (objectif == null || objectif.genre() != Objectif.Genre.ATTEINDRE || !progression.attendue(Objectif.Genre.ATTEINDRE, objectif.cible())) {
            return null;
        }
        Point point = res.campagne.points().get(objectif.cible());
        if (!point.lieu().carte().equals(carte)) {
            return null;
        }
        float[] p = res.position(point.lieu());
        return Math.abs(joueur.x - p[0]) <= PORTEE_X && Math.abs(joueur.y - p[1]) <= PORTEE_Y ? point.id() : null;
    }

    /** Le garde qui voit le joueur en ce moment, ou null. Un garde ne voit que devant lui, et pas jusqu'a la chaussee. */
    Habitant gardeQuiVoit() {
        for (Habitant habitant : habitants) {
            float devant = (joueur.x - habitant.acteur.x) * habitant.sens;
            if (habitant.figurant.garde() && devant > 0 && devant < VUE_GARDE
                    && Math.abs(joueur.y - habitant.acteur.y) < COULOIR_GARDE) {
                return habitant;
            }
        }
        return null;
    }

    /**
     * L'endroit de la carte courante ou mene l'objectif : la cible elle-meme si elle est ici,
     * sinon la porte a prendre pour s'en approcher. Null si aucun chemin n'y mene.
     */
    float[] direction(Objectif objectif) {
        String but = res.campagne.carteDe(objectif);
        if (but == null) {
            return null;
        }
        if (but.equals(carte) && objectif.genre() != Objectif.Genre.ALLER) {
            if (objectif.genre() == Objectif.Genre.PARLER) {
                Habitant habitant = habitant(objectif.cible());
                return habitant == null || habitant.acteur.texture == null ? null
                        : new float[] {habitant.acteur.x, habitant.acteur.y - habitant.acteur.texture.hauteur()};
            }
            float[] p = res.position(res.campagne.points().get(objectif.cible()).lieu());
            Acteur objet = points.get(objectif.cible());
            return new float[] {p[0], p[1] - 30 - (objet == null ? 0 : objet.hauteur)};
        }
        Porte porte = premierePorteVers(but);
        return porte == null ? null : new float[] {(porte.x1() + porte.x2()) / 2f, scene.yMin - 46};
    }

    private Porte premierePorteVers(String but) {
        Map<String, Porte> depart = new HashMap<>();
        Deque<String> file = new ArrayDeque<>();
        depart.put(carte, null);
        file.add(carte);
        while (!file.isEmpty()) {
            String ici = file.poll();
            if (ici.equals(but)) {
                return depart.get(ici);
            }
            for (Porte porte : res.carte(ici).portes) {
                if (!depart.containsKey(porte.cible()) && res.carteExiste(porte.cible())) {
                    depart.put(porte.cible(), ici.equals(carte) ? porte : depart.get(ici));
                    file.add(porte.cible());
                }
            }
        }
        return null;
    }

    List<Acteur> acteurs(List<Acteur> enPlus) {
        List<Acteur> tous = new ArrayList<>();
        for (Habitant habitant : habitants) {
            habitant.acteur.miroir = habitant.sens < 0;
            tous.add(habitant.acteur);
        }
        tous.addAll(points.values());
        if (!scenePilote) {
            heros.x = joueur.x;
            heros.y = joueur.y;
            heros.miroir = !joueur.versDroite;
            heros.marche = joueur.enMouvement;
        }
        tous.add(heros);
        tous.addAll(enPlus);
        return tous;
    }

    void dessiner(Rendu rendu, List<Acteur> enPlus) {
        rendu.dessiner(scene, Math.round(camX), tempsMs, acteurs(enPlus));
    }
}
