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
        float origine;
        float origineY;
        int sens = 1;
        // sa vie quand personne ne lui parle (voir Vie)
        boolean passant;
        float vitesse = 20;
        float sortieX;
        boolean enRoute;
        float cibleX;
        float cibleY;
        float pause;
        String bulle;
        float bulleReste;
        float bulleRepos = 8;
        float bousculeRepos;
        String reaction;
        float reactionReste;
        Habitant partenaire;
        float causeReste;
        int sujet;
        int tour = -1;
        int coups;

        Habitant(Figurant figurant, Acteur acteur) {
            this.figurant = figurant;
            this.acteur = acteur;
            this.origine = acteur.x;
            this.origineY = acteur.y;
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
    private boolean choc;
    private float tremblement;
    private final java.util.Random secousses = new java.util.Random(5);
    private boolean court;
    private final Vie vie;
    private boolean pluie;

    public Monde(Ressources res, Progression progression) {
        this.res = res;
        this.progression = progression;
        this.heros = new Acteur("toi", null, 0, 0);
        this.vie = new Vie(this, res, progression);
    }

    /** La vie du quartier : les routines, les passants, les reactions. */
    Vie vie() {
        return vie;
    }

    /** Le temps qu'il fait dehors : sous la pluie, il y a moins de monde. */
    void meteo(boolean pleut) {
        pluie = pleut;
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
            for (Habitant h : habitants) {                             // la scene les a places : c'est leur nouvelle place
                h.origine = h.acteur.x;
                h.origineY = h.acteur.y;
                h.enRoute = false;
            }
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

    /** L'image tremble un instant : un coup, une chute. */
    void trembler(float force) {
        tremblement = Math.max(tremblement, force);
    }

    /** Le heros a un scooter gare quelque part, qui l'attend. */
    boolean scooterGare() {
        return progression.drapeau("scooter_gare") && progression.etat().scooterCarte() != null;
    }

    /** Vrai a l'image ou le scooter vient de heurter un obstacle a pleine vitesse. */
    boolean choc() {
        return choc;
    }

    /** Vrai si le heros court vraiment (la touche est tenue et il lui reste du souffle). */
    boolean court() {
        return court;
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
        joueur.stopper();
        heros.x = x;
        heros.y = y;
        camX = cibleCamera();
        peupler();
    }

    /** Un petit de l'equipe parti sur une operation n'est pas a la boutique (ni son scooter devant). */
    private boolean libre(Figurant figurant) {
        if (figurant.membre() == null || !progression.drapeau("organisation")) {
            return true;
        }
        histoire.Organisation.Membre m = progression.etat().organisation().membre(figurant.membre());
        return m == null || m.libre();
    }

    /** Remet dans la carte les personnages et les objets que l'histoire y place en ce moment. */
    public void peupler() {
        Map<String, Habitant> avant = new HashMap<>();
        List<Habitant> passants = new ArrayList<>();
        for (Habitant h : habitants) {
            avant.put(h.figurant.id(), h);
            if (h.passant) {
                passants.add(h);                                       // les passants de la rue continuent leur chemin
            }
        }
        habitants.clear();
        points.clear();
        for (Figurant figurant : res.campagne.figurants().values()) {
            if (figurant.lieu().carte().equals(carte) && progression.present(figurant) && libre(figurant)) {
                float[] p = res.position(figurant.lieu());
                Acteur acteur = new Acteur(figurant.id(), res.image(figurant.image()), p[0], p[1]);
                acteur.pas = figurant.amplitude() > 0 ? res.image(figurant.image() + "_2") : null;
                Habitant habitant = new Habitant(figurant, acteur);
                Habitant ancien = avant.get(figurant.id());
                if (ancien != null) {                                  // il etait deja la : il garde sa place, son regard, son humeur
                    acteur.x = ancien.acteur.x;
                    acteur.y = ancien.acteur.y;
                    habitant.sens = ancien.sens;
                    habitant.reaction = ancien.reaction;
                    habitant.reactionReste = ancien.reactionReste;
                    habitant.enRoute = ancien.enRoute;
                    habitant.cibleX = ancien.cibleX;
                    habitant.cibleY = ancien.cibleY;
                    habitant.coups = ancien.coups;
                    habitant.origine = ancien.origine;               // sa place, y compris celle qu'une scene lui a donnee
                    habitant.origineY = ancien.origineY;
                    habitant.pause = ancien.pause;
                    habitant.bulle = ancien.bulle;
                    habitant.bulleReste = ancien.bulleReste;
                    habitant.bulleRepos = ancien.bulleRepos;
                    habitant.bousculeRepos = ancien.bousculeRepos;
                }
                habitants.add(habitant);
            }
        }
        habitants.addAll(passants);
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
        if (carte.equals("appartement") && !progression.etat().colis().isEmpty()) {     // une commande est arrivee
            points.put("colis", new Acteur("colis", res.image("colis"), 74, 212));
        }
        if (scooterGare() && progression.etat().scooterCarte().equals(carte) && !enScooter()) {
            Acteur scooter = new Acteur("scooter_gare", res.image("scooter_gare"), progression.etat().scooterX(),
                    progression.etat().scooterY());
            points.put("scooter_gare", scooter);
        }
        habiller();
    }

    /**
     * Le dessin et la vitesse du heros, selon qu'il marche, conduit le scooter ou porte des caisses.
     * Blesse, il marche moins vite ; charge et a bout de souffle, il se traine.
     */
    private void habiller() {
        histoire.Etat etat = progression.etat();
        if (poseImposee != null && !enScooter()) {
            heros.texture = poseImposee;
            heros.pas = poseImposee;
            return;
        }
        if (enScooter()) {
            heros.texture = res.image("heros_scooter");
            heros.pas = res.image("heros_scooter_2");
            joueur.marche = 170;
            joueur.course = 240;
            joueur.reprise = 420;
            joueur.frein = 900;
        } else if (charge()) {
            heros.texture = res.image("heros_charge");
            heros.pas = res.image("heros_charge_2");
            joueur.marche = etat.endurance() <= 0f ? 36 : 62;
            joueur.course = joueur.marche;
            joueur.reprise = Joueur.REPRISE;
            joueur.frein = Joueur.REPRISE;
        } else {
            heros.texture = res.image("heros");
            heros.pas = res.image("heros_2");
            float allure = etat.blesse() ? 0.85f : 1f;
            joueur.marche = Joueur.MARCHE * allure;
            joueur.course = Joueur.COURSE * allure;
            joueur.reprise = Joueur.REPRISE;
            joueur.frein = Joueur.REPRISE;
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
        boolean bouge = dx != 0 || dy != 0;
        boolean scooter = enScooter();
        // le souffle : courir en coute, porter aussi ; le scooter n'en demande pas (c'est pour ca qu'on le loue)
        boolean aPied = progression.etat().respirer(bouge && !scooter, court && bouge && !scooter && !charge(), charge(), dt);
        this.court = scooter ? court : aPied;
        joueur.avancer(scene, dx, dy, this.court, dt);
        choc = scooter && joueur.choc > 150f;
        chemin = (float) Math.hypot(joueur.x - x0, joueur.y - y0);
        bouger(dt);
        camX += (cibleCamera() - camX) * Math.min(1f, dt * 6f);
        tremblement = Math.max(0f, tremblement - dt * 18f);
    }

    private void bouger(float dt) {
        vie.maj(dt, pluie);
        for (Habitant habitant : habitants) {
            Figurant f = habitant.figurant;
            if (f.amplitude() > 0 && habitant.reaction == null) {
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
        choc = false;
        court = false;
        joueur.stopper();
        progression.etat().respirer(false, false, false, dt);
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
        Acteur colis = points.get("colis");
        if (colis != null && aPortee(colis.x, colis.y)) {
            return new Cible(Objectif.Genre.AGIR, "colis", "E : OUVRIR LE COLIS", null);
        }
        Acteur gare = points.get("scooter_gare");
        if (gare != null && aPortee(gare.x, gare.y)) {
            return new Cible(Objectif.Genre.AGIR, "scooter_gare", "E : REMONTER SUR LE SCOOTER", null);
        }
        boolean roule = enScooter();
        Habitant proche = null;
        Habitant passant = null;
        for (Habitant habitant : habitants) {
            if (habitant.acteur.texture == null) {
                continue;
            }
            if (habitant.passant) {                                    // un passant ne prend jamais la place d'une porte
                if (!roule && aPortee(habitant.acteur.x, habitant.acteur.y) && habitant.reaction == null) {
                    passant = habitant;
                }
                continue;
            }
            boolean attendu = progression.attendue(Objectif.Genre.PARLER, habitant.figurant.id());
            if (roule && !attendu) {
                continue;                                              // en roulant, on ne bavarde pas : on descend d'abord
            }
            boolean bavard = habitant.figurant.service() != null
                    || habitant.figurant.repliques().stream().anyMatch(r -> progression.verifie(r.condition()));
            if (aPortee(habitant.acteur.x, habitant.acteur.y) && (attendu || bavard) && (proche == null || attendu)) {
                proche = habitant;
            }
        }
        if (proche != null && progression.attendue(Objectif.Genre.PARLER, proche.figurant.id())) {
            return new Cible(Objectif.Genre.PARLER, proche.figurant.id(), "E : PARLER À " + proche.figurant.nom(), null);
        }
        if (!enScooter() && armeAuSol() != null) {                     // une arme posee par terre, la ou on l'a laissee
            return new Cible(Objectif.Genre.AGIR, AU_SOL, "E : RAMASSER " + histoire.Etat.arme(armeAuSol().arme()).nom(), null);
        }
        if (!enScooter()) {                                            // une poubelle qu'on n'a pas encore fouillee
            for (Map.Entry<String, float[]> r : scene.reperes.entrySet()) {
                if (r.getKey().startsWith("fouille") && !progression.etat().fouillee(r.getKey()) && aPortee(r.getValue()[0], r.getValue()[1])) {
                    return new Cible(Objectif.Genre.AGIR, FOUILLE + r.getKey(), "E : FOUILLER LA POUBELLE", null);
                }
            }
        }
        if (proche != null) {
            return new Cible(Objectif.Genre.PARLER, proche.figurant.id(), "E : PARLER À " + proche.figurant.nom(), null);
        }
        float[] lit = carte.equals("appartement") ? scene.reperes.get("lit") : null;
        if (lit != null && aPortee(lit[0], lit[1]) && (progression.etat().sante() < histoire.Etat.SANTE_MAX
                || progression.etat().endurance() < progression.etat().enduranceMax())) {
            return new Cible(Objectif.Genre.AGIR, "dormir", "E : DORMIR", null);
        }
        Porte porte = scene.porteEn(joueur.x, joueur.y);
        if (porte != null) {
            return new Cible(Objectif.Genre.ALLER, porte.cible(), null, porte);   // a scooter aussi : on le gare devant
        }
        if (roule && joueur.vitesse() < ARRET_SCOOTER) {
            return new Cible(Objectif.Genre.AGIR, DESCENDRE, "E : DESCENDRE DU SCOOTER", null);
        }
        if (passant != null) {
            return new Cible(Objectif.Genre.PARLER, passant.figurant.id(), "E : PARLER À " + passant.figurant.nom(), null);
        }
        return null;
    }

    /** On peut descendre du scooter quand il roule a moins de cette vitesse : presque arrete. */
    static final float ARRET_SCOOTER = 70f;
    static final String DESCENDRE = "descendre_scooter";
    static final String FOUILLE = "fouille:";
    static final String AU_SOL = "ausol";

    /** L'arme posee par terre a portee du heros, ou null. */
    histoire.Etat.AuSol armeAuSol() {
        for (histoire.Etat.AuSol a : progression.etat().auSol()) {
            if (a.carte().equals(carte) && Math.abs(joueur.x - a.x()) <= 16 && Math.abs(joueur.y - a.y()) <= 14) {
                return a;
            }
        }
        return null;
    }

    /**
     * Ou poser le pied en descendant du scooter garé en (x, y) : a cote de lui, cote trottoir d'abord, jamais dans un mur
     * ni dans un obstacle. Renvoie {x, y}, ou la place du scooter elle-meme s'il n'y a rien de mieux.
     */
    float[] placePourDescendre(float x, float y) {
        float[][] essais = {{0, -14}, {0, 14}, {-16, 0}, {16, 0}, {-16, -12}, {16, -12}, {0, -24}};
        for (float[] e : essais) {
            if (!scene.bloque(x + e[0], y + e[1], Joueur.RAYON) && scene.porteEn(x + e[0], y + e[1]) == null) {
                return new float[] {x + e[0], y + e[1]};
            }
        }
        return new float[] {x, y};
    }

    /** Le heros est repousse (un coup recu) : de dx pixels, s'il n'y a pas de mur. */
    void repousser(float dx) {
        if (!scene.bloque(joueur.x + dx, joueur.y, Joueur.RAYON)) {
            joueur.x += dx;
            heros.x = joueur.x;
        }
    }

    /** Le dessin impose au heros par le combat (un coup, une garde), ou null. */
    private decor.Texture poseImposee;

    void imposerPose(decor.Texture pose) {
        poseImposee = pose;
        if (pose != null) {
            heros.texture = pose;
            heros.pas = pose;
        } else {
            habiller();
        }
    }

    /** Le joueur est pose ici sans changer de carte (il descend du scooter). */
    void poser(float x, float y) {
        joueur.x = x;
        joueur.y = y;
        joueur.stopper();
        heros.x = x;
        heros.y = y;
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
        if (objectif.genre() == Objectif.Genre.DELEGUER || objectif.genre() == Objectif.Genre.ENCAISSER) {
            return new float[] {joueur.x, joueur.y - 60};                  // ca se passe au telephone, la ou l'on est
        }
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
        int secousse = tremblement > 0.3f ? Math.round((secousses.nextFloat() * 2 - 1) * tremblement) : 0;
        rendu.dessiner(scene, Math.round(camX) + secousse, tempsMs, acteurs(enPlus));
    }
}
