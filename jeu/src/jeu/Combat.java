package jeu;

import decor.Acteur;
import decor.Poses;
import decor.Rendu;
import decor.Texture;
import histoire.Etat.Arme;
import histoire.Etat.Famille;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Le combat : les poings, les pieds, les sauts, les armes de contact et les armes a feu, dans un seul systeme.
 *
 * <p>La touche A choisit seule. Avec une arme a feu, un adversaire loin : un tir ; un adversaire proche : un coup de
 * poing ou de pied (la frontiere a une marge, pour ne pas basculer sans cesse de l'un a l'autre a la limite). Sans arme
 * a feu : un coup, ou un elan vers l'adversaire vise s'il est a quelques pas. A enchaine (poing, direct, pied ou crochet,
 * circulaire) ; tenir la direction de l'adversaire en appuyant donne l'uppercut. En l'air, A est un coup de pied saute
 * (bas + A : l'attaque plongeante) ; juste apres la reception, A reprend l'enchainement au sol. La cible : celle qui est
 * dans la direction tenue, sinon celle qui est devant, la plus proche ; celle qu'on frappait reste la cible tant qu'elle
 * tient debout.
 *
 * <p>Les touches d'avant restent : J (poing, rappuyer enchaine), K (pied ; maintenu, le coup puissant), L (balayage), H
 * (lancer). C esquive (un instant intouchable) ; X maintenu tient la garde ; X au moment ou un ennemi frappe (son signe
 * passe au blanc), c'est le contre : il vacille, on riposte. R recharge, V passe a l'arme suivante.
 *
 * <p>Chaque coup a quatre temps : l'elan (on le voit venir), le declenchement, l'impact (l'image se fige un instant,
 * l'ecran tremble un peu), le retour en garde. Ce qu'il fait a l'ennemi depend de sa force : la tete part en arriere,
 * il vacille, il tombe, il est projete, il est souleve (on peut le reprendre en l'air), et il met plus ou moins de temps
 * a se relever. Les coups sautes et l'uppercut passent par-dessus une garde.
 *
 * <p>Les ennemis : ils approchent, encerclent (deux au plus attaquent a la fois, un seul tireur fait feu a la fois),
 * arment leur coup, frappent et enchainent ; touches, ils reculent ; ils apprennent (garde, esquive). Le sabreur pare
 * et riposte, le tireur garde ses distances et vise (on voit son trait rouge) puis recharge, la brute encaisse les
 * petits coups et charge (on saute par-dessus, ou on esquive), l'alien bondit. Un ennemi qui voit le heros sauter
 * peut viser haut. Les balles s'arretent aux murs et aux meubles, celles du heros comme les leurs.
 */
final class Combat {
    /** Ce qu'un coup fait a celui qui le prend. */
    enum Effet { LEGER, LOURD, SOL, PROJETE, LANCE }

    /**
     * Un geste du heros : sa duree, sa fenetre active, sa portee, ses degats, le recul qu'il donne, son dessin, son son ;
     * ce qu'il fait a l'ennemi, s'il se donne en l'air, la pose d'elan, l'avancee du corps pendant le coup (pixels par
     * seconde), et s'il passe une garde.
     */
    record Geste(String nom, float duree, float debut, float fin, float portee, float degats, float recul, String pose, String son,
                 Effet effet, boolean aerien, String elan, float avance, boolean brise) {
        Geste(String nom, float duree, float debut, float fin, float portee, float degats, float recul, String pose, String son) {
            this(nom, duree, debut, fin, portee, degats, recul, pose, son, Effet.LEGER, false, "heros_garde", 0, false);
        }
    }

    static final Geste POING = new Geste("poing", 0.28f, 0.07f, 0.16f, 26, 9, 70, "heros_poing", "coup");
    static final Geste DIRECT = new Geste("direct", 0.26f, 0.06f, 0.15f, 27, 10, 80, "heros_poing", "coup");
    static final Geste CROCHET = new Geste("crochet", 0.42f, 0.13f, 0.25f, 28, 15, 150, "heros_crochet", "choc");
    static final Geste PIED = new Geste("pied", 0.52f, 0.18f, 0.32f, 34, 16, 170, "heros_pied", "choc");
    /** K maintenu, puis relache : le coup de pied charge. Lent, il casse une garde et envoie au sol. */
    static final Geste PIED_FORT = new Geste("pied_fort", 0.6f, 0.16f, 0.3f, 38, 22, 260, "heros_pied", "coup_sourd");
    /** L : le balayage, bas, des deux cotes. Peu de degats, mais tout ce qui est debout tombe, garde ou pas. */
    static final Geste BALAYAGE = new Geste("balayage", 0.5f, 0.18f, 0.32f, 30, 6, 40, "heros_balayette", "souffle_air");
    /** H : on lance ce qu'on a (une canette, une pierre, l'arme en main) ; l'objet part a la fin de l'elan. */
    static final Geste LANCER = new Geste("lancer", 0.42f, 0.2f, 0.22f, 0, 0, 0, "heros_frappe", "souffle_air");
    /** Le circulaire : la fin de l'enchainement, la jambe a hauteur de tete. Il envoie voler. */
    static final Geste CIRCULAIRE = new Geste("circulaire", 0.5f, 0.17f, 0.3f, 34, 17, 240, "heros_circulaire", "choc", Effet.PROJETE,
            false, "heros_garde", 25, false);
    /** L'uppercut (A en tenant la direction de l'adversaire) : il le souleve ; on le reprend en l'air. Passe la garde. */
    static final Geste POING_FORT = new Geste("poing_fort", 0.46f, 0.15f, 0.26f, 26, 15, 40, "heros_poing_fort", "coup_sourd", Effet.LANCE,
            false, "heros_balayette", 50, true);
    static final Geste POING_SAUTE = new Geste("poing_saute", 0.3f, 0.05f, 0.2f, 28, 11, 100, "heros_poing_saute", "coup", Effet.LEGER,
            true, "heros_poing_saute", 0, true);
    static final Geste PIED_SAUTE = new Geste("pied_saute", 0.45f, 0.06f, 0.34f, 32, 16, 230, "heros_pied_saute", "choc", Effet.PROJETE,
            true, "heros_pied_saute", 150, true);
    /** Bas + A en l'air : il tombe sur l'ennemi ; a la reception, une onde couche tout ce qui est autour. */
    static final Geste PLONGEON = new Geste("plongeon", 1.2f, 0f, 1.2f, 30, 14, 120, "heros_plongeon", "plongeon", Effet.SOL, true,
            "heros_plongeon", 90, true);
    /** La riposte d'un contre : rapide, sure, elle fait vaciller. */
    static final Geste RIPOSTE = new Geste("riposte", 0.34f, 0.04f, 0.16f, 30, 14, 160, "heros_crochet", "coup_sourd", Effet.LOURD,
            false, "heros_garde", 60, true);
    static final float CHARGE = 0.3f;
    /** A sans arme a feu : jusqu'ou le heros se jette vers sa cible. */
    static final float ELAN_MAX = 105;
    /** La marge entre tir et corps a corps : on passe au corps a corps sous 38 pixels, on revient au tir au-dela de 58. */
    static final float CORPS_ENTREE = 38;
    static final float CORPS_SORTIE = 58;

    /** Un objet lance : il vole (x, y au sol, z en hauteur), touche, puis retombe. */
    static final class Projectile {
        final String objet;
        final int usure;
        float x;
        float y;
        float z;
        final float vx;
        float vz;
        boolean fini;

        Projectile(String objet, int usure, float x, float y, float vx) {
            this.objet = objet;
            this.usure = usure;
            this.x = x;
            this.y = y;
            this.z = 26;
            this.vx = vx;
            this.vz = 70;
        }
    }

    /** La trace d'une balle, visible deux images (x, y au sol, h la hauteur du canon). */
    record Trace(float x0, float y0, float x1, float y1, float h, float[] age) {
    }

    private final List<Projectile> projectiles = new ArrayList<>();
    private final List<Trace> traces = new ArrayList<>();
    private final List<Object> habitantsAtteints = new ArrayList<>();
    private float charge = -1;
    private String aLancer;
    private int usureALancer;
    private float loin;
    private boolean fui;

    /** Comment un ennemi se bat : au contact, au sabre (il pare et riposte), a distance (il tire), en force (il charge). */
    enum Style { CORPS, SABRE, DISTANCE, BRUTE }

    /**
     * Ce qu'est un ennemi : sa sante, sa vitesse, la force et le rythme de ses coups, son dessin ; sa garde et son esquive
     * (une fois alerte), le nombre de coups qu'il enchaine, s'il bondit de loin ; son style, l'arme qu'il porte (et qu'il
     * lache en tombant), la part de ses coups qu'il vise haut quand le heros saute, et son armure (en dessous de ces
     * degats, un coup ne l'arrete pas).
     */
    record Genre(String id, float pv, float vitesse, float degats, float preparation, float repos, float portee, String image,
                 boolean danse, float garde, float esquive, int enchainement, boolean bond, Style style, String arme, float antiair,
                 float armure) {
        Genre(String id, float pv, float vitesse, float degats, float preparation, float repos, float portee, String image, boolean danse,
              float garde, float esquive, int enchainement, boolean bond) {
            this(id, pv, vitesse, degats, preparation, repos, portee, image, danse, garde, esquive, enchainement, bond, Style.CORPS, null,
                    0.2f, 0);
        }
    }

    static final Genre ALIEN = new Genre("alien", 34, 62, 8, 0.5f, 1.2f, 24, "p_alien", true, 0.15f, 0.35f, 2, true, Style.CORPS, null,
            0.3f, 0);
    static final Genre GUETTEUR = new Genre("guetteur", 46, 70, 11, 0.45f, 1.1f, 24, "p_gars_geto", false, 0.45f, 0.1f, 2, false);
    /** Un passant qui riposte : pas un combattant, mais il ne se laisse pas faire. */
    static final Genre PASSANT = new Genre("passant", 26, 64, 7, 0.55f, 1.5f, 22, null, false, 0.25f, 0.15f, 1, false, Style.CORPS, null,
            0f, 0);
    static final Genre POLICIER = new Genre("police", 40, 76, 10, 0.42f, 1.2f, 24, null, false, 0.5f, 0.05f, 2, false);
    /** Le sabreur de Geto : il pare beaucoup, riposte aussitot, enchaine trois coupes. Les coups sautes passent sa garde. */
    static final Genre SABREUR = new Genre("sabreur", 54, 72, 13, 0.42f, 1.0f, 32, "p_sabreur", false, 0.6f, 0.15f, 3, false, Style.SABRE,
            "katana", 0.35f, 0);
    /** Le tireur : il reste a distance, vise (un trait rouge), tire, recharge. De pres, il frappe avec la crosse. */
    static final Genre TIREUR = new Genre("tireur", 38, 66, 8, 0.5f, 1.3f, 22, "p_tireur", false, 0.2f, 0.35f, 1, false, Style.DISTANCE,
            "pistolet", 0f, 0);
    static final Genre MITRAILLEUR = new Genre("mitrailleur", 42, 62, 8, 0.5f, 1.5f, 22, "p_tireur", false, 0.2f, 0.3f, 1, false,
            Style.DISTANCE, "mitraillette", 0f, 0);
    /** La brute : lente, lourde, les petits coups ne l'arretent pas. Elle charge : on saute par-dessus, ou on esquive. */
    static final Genre BRUTE = new Genre("brute", 96, 46, 16, 0.65f, 1.6f, 28, "p_brute", false, 0.1f, 0f, 1, false, Style.BRUTE, null,
            0.15f, 14);

    /** Les ennemis par leur nom (dans le script de la campagne, et pour l'aide au developpement). */
    static Genre genre(String id) {
        for (Genre g : List.of(ALIEN, GUETTEUR, PASSANT, POLICIER, SABREUR, TIREUR, MITRAILLEUR, BRUTE)) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return id.equals("gars_geto") ? GUETTEUR : null;
    }

    /** Les compositions de groupe nommees : "visite", la bande de Geto devant l'epicerie. */
    static List<Genre> groupe(String id, int nombre) {
        if (id.equals("visite")) {
            return List.of(GUETTEUR, SABREUR, MITRAILLEUR, BRUTE);
        }
        Genre g = genre(id);
        List<Genre> l = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            l.add(g == null ? GUETTEUR : g);
        }
        return l;
    }

    enum Phase { APPROCHE, PREPARE, FRAPPE, RECULE, GARDE, ESQUIVE, BOND, SOL, RELEVE, TOMBE, VACILLE, VOLE, VISE, TIRE, RECHARGE, CHARGE }

    static final class Ennemi {
        final Genre genre;
        final Acteur acteur;
        final Texture dessin;
        final Texture dessinPas;
        float pv;
        Phase etat = Phase.APPROCHE;
        float minuteur;
        float repos;
        float vx;
        float eclat;
        float phase;
        boolean touchePendantCeGeste;
        /** Les coups recus recemment (pour tomber au troisieme) et depuis le debut (il apprend a se garder). */
        int serie;
        float serieAge = 9;
        int coupsRecus;
        int coupsBloques;
        int coupsRestants;
        boolean colere;
        /** Le cote ou il se poste en attendant son tour (il encercle le heros, il ne fait pas la queue). */
        int flanc = 1;
        /** Interrompu une fois, il arme le coup suivant sans se laisser couper par un petit coup : pas de rafale sans fin. */
        boolean armure;
        /** En l'air (projete, souleve) : sa hauteur et sa vitesse vers le haut. */
        float z;
        float vz;
        /** Il vise haut : le heros a saute, ce coup-ci le cueille en l'air. */
        boolean haut;
        /** La brute arme une charge (et non un coup). */
        boolean charge;
        /** Le tireur : balles dans son chargeur, balles de la rafale en cours, ou il vise, l'eclair de son canon. */
        int munitions;
        int tirsRestants;
        float viseX;
        float viseY;
        float flash;
        float cadence;
        /** Son arme est deja tombee par terre. */
        boolean depouille;

        Ennemi(Genre genre, Acteur acteur, float phase) {
            this.genre = genre;
            this.acteur = acteur;
            this.dessin = acteur.texture;
            this.dessinPas = acteur.pas;
            this.pv = genre.pv;
            this.phase = phase;
            this.repos = 0.6f + phase * 0.5f;
            Arme a = genre.arme == null ? null : histoire.Etat.arme(genre.arme);
            this.munitions = a != null && a.aFeu() ? a.chargeur() : 0;
        }

        boolean vivant() {
            return etat != Phase.TOMBE;
        }

        /** A terre ou en train de se relever : on ne le frappe pas. */
        boolean auSol() {
            return etat == Phase.SOL || etat == Phase.RELEVE;
        }

        Arme arme() {
            return genre.arme == null ? null : histoire.Etat.arme(genre.arme);
        }

        boolean tireur() {
            return genre.style == Style.DISTANCE;
        }
    }

    private final Ressources res;
    private final Audio audio;
    private final Effets effets;
    private final Random hasard = new Random(17);
    private final List<Ennemi> ennemis = new ArrayList<>();
    private Geste geste;
    private int chaine;
    /** L'enchainement de A (1 a 4), et si la suite demandee vient de A (sinon de J). */
    private int chaineA;
    private boolean suiteDemandee;
    private boolean suiteParA;
    private boolean versLaCible;
    private float tempsGeste;
    private float lenteur = 1f;
    private float touche;
    private float invincible;
    private float chute;
    private float releve;
    private int coupsEncaisses;
    private float encaisseAge = 9;
    private float gel;
    private String victoire;
    private String defaite;
    private String rue;
    private boolean fini;
    private boolean gagne;
    /** L'esquive : son temps restant, sa direction, le repos avant la suivante. */
    private float esquive;
    private float esquiveX;
    private float esquiveY;
    private float esquiveRepos;
    private boolean garde;
    /** L'elan vers une cible (A sans arme a feu, l'adversaire a quelques pas). */
    private float elan;
    private Ennemi elanCible;
    /** Depuis quand il a retouche le sol (A juste apres reprend l'enchainement au sol), et ses coups en l'air. */
    private float atterri = 9;
    private int coupsEnLAir;
    private boolean etaitEnLAir;
    /** Les armes a feu : l'intervalle avant le tir suivant, le rechargement, la sortie de l'arme, l'echauffement. */
    private float cadenceReste;
    private float recharge;
    private float rechargeTotal;
    private boolean rechargeSon;
    private float degaine;
    private float bloom;
    private float tirPose;
    private float flash;
    private float vide;
    /** Un tir demande pendant l'intervalle : il part des que l'arme le permet (on ne perd pas l'appui). */
    private float tirEnAttente;
    private String armeVue;
    /** La cible (celle qu'on garde tant qu'elle tient debout) et le style choisi pour elle : corps a corps ou tir. */
    private Ennemi cible;
    private boolean auCorps;
    /** Les coups portes sans en prendre : l'enchainement s'accelere au-dela de cinq. */
    private int combo;
    private float comboAge = 9;
    private int meilleurCombo;
    /** La difficulte : 1 au debut, un peu plus a mesure que l'histoire avance (plus vifs, plus forts). */
    private float niveau = 1f;
    private String annonce;

    Combat(Ressources res, Audio audio, Effets effets) {
        this.res = res;
        this.audio = audio;
        this.effets = effets;
    }

    void niveau(float n) {
        niveau = Math.max(0.8f, Math.min(1.4f, n));
    }

    /** Un combat commence : nombre ennemis de ce genre, autour de x ; les scenes a jouer selon qu'on gagne ou qu'on perd. */
    void commencer(Genre genre, int nombre, float x, float y, String sceneVictoire, String sceneDefaite, decor.Scene scene) {
        List<Genre> l = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            l.add(genre);
        }
        commencer(l, x, y, sceneVictoire, sceneDefaite, scene);
    }

    /** Un groupe d'ennemis differents (la bande de Geto : un bagarreur, un sabreur, un tireur, une brute). */
    void commencer(List<Genre> genres, float x, float y, String sceneVictoire, String sceneDefaite, decor.Scene scene) {
        ennemis.clear();
        for (int i = 0; i < genres.size(); i++) {
            Genre genre = genres.get(i);
            float cote = i % 2 == 0 ? 1 : -1;
            float ecart = genre.style == Style.DISTANCE ? 130 : 90 + 26 * (i / 2);
            float[] place = placeLibre(scene, x + cote * ecart, y + (i % 3 - 1) * 10, cote);
            Acteur a = new Acteur(genre.id + i, res.image(genre.image), place[0], place[1]);
            a.pas = res.image(genre.image + "_2");
            a.miroir = cote > 0;
            Ennemi e = new Ennemi(genre, a, i * 0.37f);
            e.flanc = (int) cote;
            ennemis.add(e);
        }
        victoire = sceneVictoire;
        defaite = sceneDefaite;
        rue = null;
        remettre();
    }

    /** Une bagarre de rue : un habitant qu'on a frappe et qui riposte, avec son propre dessin. Pas de scene a la fin. */
    void bagarre(Genre genre, Acteur adversaire, String id) {
        ennemis.clear();
        Ennemi e = new Ennemi(genre, adversaire, 0);
        e.repos = 0.25f;
        e.colere = true;
        ennemis.add(e);
        victoire = null;
        defaite = null;
        rue = id;
        remettre();
    }

    /** Quelqu'un vient s'en meler (un policier qui passait). */
    void renfort(Genre genre, Acteur venu) {
        Ennemi e = new Ennemi(genre, venu, 0.5f);
        e.flanc = ennemis.size() % 2 == 0 ? 1 : -1;
        e.repos = 0.8f;
        ennemis.add(e);
    }

    private void remettre() {
        fini = false;
        fui = false;
        loin = 0;
        charge = -1;
        projectiles.clear();
        traces.clear();
        gagne = false;
        geste = null;
        chaine = 0;
        chaineA = 0;
        touche = 0;
        invincible = 0;
        chute = 0;
        releve = 0;
        gel = 0;
        esquive = 0;
        elan = 0;
        cible = null;
        auCorps = false;
        combo = 0;
        meilleurCombo = 0;
        recharge = 0;
    }

    /** Une place ou un ennemi peut se tenir et bouger : dans les limites du sol, hors des meubles, de ce cote du heros. */
    private static float[] placeLibre(decor.Scene scene, float x, float y, float cote) {
        float py = Math.max(scene.yMin + 3, Math.min(scene.yMax - 3, y));
        for (int k = 0; k < 40; k++) {
            float px = Math.max(scene.xMin + 14, Math.min(scene.xMax - 14, x + (k % 2 == 0 ? 1 : -1) * cote * (k / 2) * 12));
            for (float dy : new float[] {0, 12, -12, 24, -24}) {
                float essai = Math.max(scene.yMin + 3, Math.min(scene.yMax - 3, py + dy));
                if (!scene.bloque(px, essai, 6)) {
                    return new float[] {px, essai};
                }
            }
        }
        return new float[] {x, py};
    }

    boolean actif() {
        return !ennemis.isEmpty() && !fini;
    }

    boolean fini() {
        return fini;
    }

    boolean gagne() {
        return gagne;
    }

    String sceneVictoire() {
        return victoire;
    }

    String sceneDefaite() {
        return defaite;
    }

    /** Vrai si la bagarre s'est finie parce que le heros s'est eloigne (on peut fuir une bagarre de rue). */
    boolean fui() {
        return fui;
    }

    List<Projectile> projectiles() {
        return projectiles;
    }

    List<Trace> traces() {
        return traces;
    }

    /** Un habitant touche par un objet lance ou une balle (la partie le fait reagir), ou null. */
    Object prendreHabitantAtteint() {
        return habitantsAtteints.isEmpty() ? null : habitantsAtteints.remove(0);
    }

    /** L'habitant avec qui on se bat dans la rue, ou null pour un combat de l'histoire. */
    String rue() {
        return rue;
    }

    void oublier() {
        for (Ennemi e : ennemis) {                                     // un habitant reprend son dessin
            e.acteur.texture = e.etat == Phase.TOMBE && rue == null ? null : e.dessin;
            e.acteur.pas = e.dessinPas;
            e.acteur.hauteur = 0;
        }
        ennemis.clear();
        fini = false;
        rue = null;
        chute = 0;
        releve = 0;
        touche = 0;
        geste = null;
        esquive = 0;
        elan = 0;
        recharge = 0;
        cible = null;
    }

    List<Ennemi> ennemis() {
        return ennemis;
    }

    Ennemi cible() {
        return cible;
    }

    /** Vrai si, contre la cible du moment, A frappe au lieu de tirer (une arme a feu en main, l'ennemi tout pres). */
    boolean auCorps() {
        return auCorps;
    }

    int combo() {
        return comboAge < 1.8f ? combo : 0;
    }

    float comboAge() {
        return comboAge;
    }

    /** Le rechargement en cours : sa part faite (0 a 1), ou -1. */
    float rechargement() {
        return recharge > 0 ? 1 - recharge / rechargeTotal : -1;
    }

    /** Vrai un instant apres un tir a vide. */
    boolean chargeurVide() {
        return vide > 0;
    }

    boolean enGarde() {
        return garde;
    }

    boolean enEsquive() {
        return esquive > 0;
    }

    /** Vrai tant qu'aucun coup ne peut le toucher (esquive, se relever, juste apres un coup recu). */
    boolean intouchable() {
        return invincible > 0;
    }

    // ------------------------------------------------------------------ viser

    /** Une ligne droite au sol, de (x0, y0) a (x1, y1), sans mur ni meuble dessus. */
    static boolean ligneLibre(decor.Scene scene, float x0, float y0, float x1, float y1) {
        float d = (float) Math.hypot(x1 - x0, y1 - y0);
        int n = (int) (d / 3);
        for (int i = 2; i < n - 1; i++) {
            float x = x0 + (x1 - x0) * i / n;
            float y = y0 + (y1 - y0) * i / n;
            if (scene.bloque(x, y, 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * La cible : un ennemi debout a portee, que l'on peut atteindre (aucun mur entre les deux). Une direction tenue
     * choisit celui qui est dans cette direction, meme s'il n'est pas le plus proche ; sinon, celui qui est devant ;
     * celle qu'on frappait garde la preference. La profondeur (y) compte double : la rue est vue de biais.
     */
    Ennemi choisirCible(Monde monde, float ix, float iy, float portee, boolean tir) {
        float hx = monde.joueur.x;
        float hy = monde.joueur.y;
        float sens = monde.joueur.versDroite ? 1 : -1;
        Ennemi mieux = null;
        float meilleur = Float.MAX_VALUE;
        for (Ennemi e : ennemis) {
            if (!e.vivant() || e.auSol()) {
                continue;
            }
            float dx = e.acteur.x - hx;
            float dy = e.acteur.y - hy;
            float d = (float) Math.hypot(dx, dy * 2);
            if (d > portee || tir && Math.abs(dy) > 40) {
                continue;
            }
            if (!ligneLibre(monde.scene(), hx, hy, e.acteur.x, e.acteur.y)) {
                continue;
            }
            float score = Math.max(4, d);
            if (ix != 0 || iy != 0) {
                float n = (float) Math.hypot(ix, iy);
                float alignement = (dx * ix + dy * 2 * iy) / (Math.max(1, d) * n);
                score *= alignement < 0.25f ? 3f : 1.5f - alignement;
            } else if (dx * sens < 0) {
                score *= 1.6f;                                          // dans son dos : seulement si personne devant
            }
            if (e == cible) {
                score *= 0.75f;
            }
            if (score < meilleur) {
                meilleur = score;
                mieux = e;
            }
        }
        return mieux;
    }

    /**
     * Le heros se tourne vers l'adversaire qu'il vise (le plus proche, meme dans son dos) et se cale a sa hauteur :
     * on oriente ses coups sans avoir a viser au pixel.
     */
    private void viser(Monde monde) {
        Ennemi c = cible != null && cible.vivant() && !cible.auSol() && Math.abs(cible.acteur.x - monde.joueur.x) < 75
                && Math.abs(cible.acteur.y - monde.joueur.y) < 26 ? cible : null;
        if (c == null) {
            float mieux = 75;
            for (Ennemi e : ennemis) {
                float d = Math.abs(e.acteur.x - monde.joueur.x);
                if (e.vivant() && !e.auSol() && d < mieux && Math.abs(e.acteur.y - monde.joueur.y) < 26) {
                    c = e;
                    mieux = d;
                }
            }
        }
        if (c == null) {
            return;
        }
        monde.joueur.versDroite = c.acteur.x >= monde.joueur.x;
        float dy = c.acteur.y - monde.joueur.y;
        float pas = Math.max(-6, Math.min(6, dy));
        if (!monde.scene().bloque(monde.joueur.x, monde.joueur.y + pas, decor.Joueur.RAYON)) {
            monde.joueur.y += pas;
        }
    }

    /** Le geste en cours du heros (pour les essais), ou null. */
    Geste geste() {
        return geste;
    }

    /** Vrai pendant un coup, un choc, une chute, une esquive, un elan, la garde : le heros ne marche pas en meme temps. */
    boolean occupe() {
        return geste != null || touche > 0 || chute > 0 || releve > 0 || esquive > 0 || elan > 0 || garde;
    }

    /** Vrai si le heros est a terre (il ne peut rien faire, on ne peut pas le frapper). */
    boolean heroAuSol() {
        return chute > 0 || releve > 0;
    }

    // ------------------------------------------------------------------ le dessin du heros

    /** Le dessin du heros en ce moment, ou null pour le laisser marcher normalement. */
    Texture pose(histoire.Etat etatHeros, Monde monde) {
        Arme arme = etatHeros.armeEnMain();
        boolean feu = arme != null && arme.aFeu();
        if (monde.projete()) {
            return res.image("heros_projete");
        }
        if (chute > 0) {
            return Poses.couche(res.image("heros_touche"), true);
        }
        if (releve > 0) {
            return Poses.tasse(res.image("heros_garde"), releve > 0.25f ? 0.62f : 0.82f);
        }
        if (touche > 0) {
            return Poses.penche(res.image("heros_touche"), touche > 0.18f ? -3 : -1);
        }
        if (esquive > 0) {                                              // il plonge : penche dans le sens de la fuite
            boolean devant = esquiveX * (monde.joueur.versDroite ? 1 : -1) > 0;
            return Poses.penche(res.image("heros_esquive"), devant ? 3 : 0);
        }
        if (elan > 0) {                                                 // il se jette vers sa cible
            return Poses.penche(res.image("heros_2"), 5);
        }
        if (charge >= 0) {                                              // il arme son coup de pied : il se ramasse, et ca se voit
            Texture armee = Poses.tasse(Poses.penche(res.image("heros_garde"), -3), 0.94f);
            return charge >= CHARGE && ((int) (charge * 14)) % 2 == 0 ? Poses.penche(armee, -1) : armee;
        }
        if (geste != null) {
            return poseDuGeste(geste);
        }
        if (garde) {
            return res.image("heros_garde_haute");
        }
        if (recharge > 0) {
            return res.image("heros_recharge");
        }
        if (feu && (tirPose > 0 || degaine > 0)) {                      // l'arme levee ; le recul pousse les epaules en arriere
            Texture t = res.image(arme.deuxMains() ? "heros_tir_deux" : "heros_tir");
            return flash > 0 ? Poses.penche(t, arme.famille() == Famille.PISTOLET ? -2 : -1) : t;
        }
        if (monde.enLAir()) {
            return null;                                                // le saut a son propre dessin
        }
        if (actif() && etatHeros.essouffle()) {                         // a bout de souffle : plie en deux
            return Poses.penche(Poses.tasse(res.image("heros_garde"), 0.92f), 2);
        }
        if (actif() && feu && cible != null && !auCorps) {
            return res.image(arme.deuxMains() ? "heros_tir_deux" : "heros_tir");
        }
        return actif() ? res.image("heros_garde") : null;
    }

    /** Les quatre temps d'un coup : l'elan, le coup qui part, l'impact (la pose pleine), le retour en garde. */
    private Texture poseDuGeste(Geste g) {
        float t = tempsGeste / (g.duree() * lenteur);
        boolean elanTemps = tempsGeste < g.debut() * lenteur * 0.7f;
        if (g == BALAYAGE) {
            return elanTemps ? Poses.tasse(res.image("heros_garde"), 0.8f) : res.image("heros_balayette");
        }
        if (g == LANCER) {
            return elanTemps ? Poses.penche(res.image("heros_frappe"), -3) : Poses.penche(res.image("heros_poing"), 3);
        }
        if (g == PIED_FORT) {
            return Poses.penche(res.image("heros_pied"), t < 0.5f ? 3 : 1);
        }
        if (g == PLONGEON) {
            return res.image("heros_plongeon");
        }
        if (g.aerien()) {
            return elanTemps ? Poses.penche(res.image(g.pose()), -2) : res.image(g.pose());
        }
        if (elanTemps && (g == POING || g == PIED)) {                   // l'anticipation : l'epaule ou la hanche recule
            return Poses.penche(res.image("heros_garde"), g == PIED ? -2 : -1);
        }
        if (g == CROCHET || g == RIPOSTE) {                             // le crochet : l'epaule part, puis tout le corps suit
            return Poses.penche(res.image("heros_poing"), t < 0.3f ? -2 : 4);
        }
        if (g == DIRECT) {
            return Poses.penche(res.image("heros_poing"), 2);
        }
        if (g == CIRCULAIRE || g == POING_FORT) {
            if (elanTemps) {
                return Poses.penche(res.image(g.elan()), -2);
            }
            float retour = g.fin() + (g.duree() - g.fin()) * 0.55f;
            if (tempsGeste > retour * lenteur) {
                return Poses.penche(res.image("heros_garde"), -1);      // le retour : il se remet en garde
            }
            return res.image(g.pose());
        }
        return res.image(g.pose());
    }

    // ------------------------------------------------------------------ une image de combat

    /**
     * Une image de combat. Le heros frappe, tire, saute, esquive, se garde (meme hors combat : on peut boxer dans le
     * vide), les ennemis vivent, les coups portent. Renvoie le message a afficher (une arme qui casse, l'arme en main
     * qui change), ou null.
     */
    String maj(float dt, Touches touches, Monde monde, histoire.Etat etat) {
        if (gel > 0) {                                                  // l'impact : un instant, tout s'arrete
            gel -= dt;
            if ((touches.poing() || touches.attaque()) && geste != null) {
                suiteDemandee = true;
                suiteParA = touches.attaque();
            }
            return null;
        }
        annonce = null;
        touche = Math.max(0, touche - dt);
        invincible = Math.max(0, invincible - dt);
        encaisseAge += dt;
        comboAge += dt;
        if (comboAge > 1.8f) {
            combo = 0;
        }
        for (Trace t : traces) {
            t.age()[0] += dt;
        }
        traces.removeIf(t -> t.age()[0] > 0.07f);
        if (monde.prendreChute()) {                                    // projete, il retombe : il est au sol
            chute = 0.7f;
            invincible = Math.max(invincible, 1.6f);
            effets.poussiere(monde.joueur.x, monde.joueur.y, 8);
            audio.jouer("corps_chute", 0.8f, 0, 0.9f);
            monde.trembler(3f);
        }
        if (chute > 0) {
            chute -= dt;
            if (chute <= 0) {
                releve = 0.45f;
                audio.jouer(Audio.Bus.EFFETS, "vetement", 0.4f, 0, 0.8f, 0);
            }
        } else if (releve > 0) {
            releve -= dt;
            if (releve <= 0) {
                invincible = Math.max(invincible, 0.5f);
            }
        }
        boolean enLAir = monde.enLAir();
        if (etaitEnLAir && monde.hauteurDuSaut() <= 0) {
            atterrir(monde, etat);
        }
        etaitEnLAir = monde.hauteurDuSaut() > 0;
        if (!enLAir) {
            coupsEnLAir = 0;
        }
        atterri += dt;
        majArmeAFeu(dt, etat);
        if (!heroAuSol() && !monde.projete()) {
            String m = gestes(dt, touches, monde, etat);
            annonce = m != null ? m : annonce;
        } else {
            garde = false;
        }
        majCible(monde, etat);
        for (Ennemi e : ennemis) {
            vivre(e, dt, monde, etat);
        }
        voler(dt, monde, etat);
        ecarterLesCorps(dt, monde);
        if (actif() && rue != null) {                                   // une bagarre de rue : on peut s'enfuir
            boolean tousLoin = ennemis.stream().filter(Ennemi::vivant).allMatch(e -> Math.abs(e.acteur.x - monde.joueur.x) > 220);
            loin = tousLoin ? loin + dt : 0;
            if (loin > 2.5f) {
                fini = true;
                fui = true;
                gagne = false;
            }
        }
        if (actif()) {
            if (ennemis.stream().noneMatch(Ennemi::vivant) && ennemis.stream().allMatch(e -> e.minuteur <= 0)) {
                fini = true;
                gagne = true;
            } else if (etat.sante() <= 1f) {
                fini = true;
                gagne = false;
            }
        }
        return annonce;
    }

    /** La cible suit le heros : on la garde tant qu'elle tient debout ; le style (tir ou corps a corps) a sa marge. */
    private void majCible(Monde monde, histoire.Etat etat) {
        if (cible != null && (!cible.vivant() || cible.auSol() || !ennemis.contains(cible))) {
            cible = null;
        }
        Arme a = etat.armeEnMain();
        boolean feu = a != null && a.aFeu();
        if (cible == null && actif()) {
            cible = choisirCible(monde, 0, 0, feu ? a.portee() : ELAN_MAX, feu);
        }
        if (cible == null) {
            auCorps = false;
            return;
        }
        float d = (float) Math.hypot(cible.acteur.x - monde.joueur.x, (cible.acteur.y - monde.joueur.y) * 2);
        if (auCorps && d > CORPS_SORTIE) {
            auCorps = false;
        } else if (!auCorps && d < CORPS_ENTREE) {
            auCorps = true;
        }
    }

    /** Les compteurs de l'arme a feu : l'intervalle entre deux tirs, l'echauffement qui retombe, le rechargement. */
    private void majArmeAFeu(float dt, histoire.Etat etat) {
        Arme vue = etat.armeEnMain();
        String id = vue == null ? null : vue.id();
        if (!java.util.Objects.equals(id, armeVue)) {                  // une autre arme en main : son propre rythme
            armeVue = id;
            cadenceReste = 0;
            bloom = 0;
            tirEnAttente = 0;
        }
        tirEnAttente = Math.max(0, tirEnAttente - dt);
        cadenceReste = Math.max(0, cadenceReste - dt);
        degaine = Math.max(0, degaine - dt);
        tirPose = Math.max(0, tirPose - dt);
        flash = Math.max(0, flash - dt);
        vide = Math.max(0, vide - dt);
        bloom = Math.max(0, bloom - dt * 0.35f);
        if (recharge > 0) {
            Arme a = etat.armeEnMain();
            if (a == null || !a.aFeu()) {
                recharge = 0;
                return;
            }
            recharge -= dt;
            if (!rechargeSon && recharge < rechargeTotal * 0.4f) {      // le chargeur neuf entre
                rechargeSon = true;
                audio.jouer("chargeur_entre", 0.6f, 0, 1f);
            }
            if (recharge <= 0) {
                boolean videAvant = etat.chargees() == 0;
                int n = etat.recharger();
                if (videAvant && n > 0) {
                    audio.jouer(Audio.Bus.EFFETS, "culasse", 0.6f, 0, 1f, 0.02f);
                }
                tirPose = 0.4f;
            }
        }
    }

    private void atterrir(Monde monde, histoire.Etat etat) {
        atterri = 0;
        if (geste == PLONGEON) {                                        // l'onde de la reception
            onde(monde, etat, monde.joueur.x, 36, PLONGEON.degats());
            geste = null;
            touche = 0.12f;                                             // il se releve du choc
        } else if (geste != null && geste.aerien()) {
            geste = null;
        }
    }

    /** Une onde au sol (la reception plongeante, la derniere coupe de la relique) : tout ce qui est debout autour tombe. */
    private void onde(Monde monde, histoire.Etat etat, float x, float rayon, float degats) {
        effets.poussiere(x, monde.joueur.y, 14);
        effets.impact(x, monde.joueur.y - 4, 0xFFF0C0, 8);
        monde.trembler(4.5f);
        audio.jouer("corps_chute", 0.7f, 0, 0.7f);
        for (Ennemi e : ennemis) {
            if (!e.vivant() || e.auSol() || e.z > 8 || Math.abs(e.acteur.x - x) > rayon || Math.abs(e.acteur.y - monde.joueur.y) > 16) {
                continue;
            }
            e.pv -= degats;
            e.eclat = 0.14f;
            e.vx = Math.signum(e.acteur.x - x) * 70;
            marquer(e);
            if (e.pv <= 0) {
                e.etat = Phase.TOMBE;
                e.minuteur = 0.9f;
            } else {
                e.etat = Phase.SOL;
                e.minuteur = 1.0f / niveau;
            }
        }
    }

    /** Un coup qui porte compte dans l'enchainement. */
    private void marquer(Ennemi e) {
        combo = comboAge < 1.8f ? combo + 1 : 1;
        comboAge = 0;
        meilleurCombo = Math.max(meilleurCombo, combo);
    }

    // ------------------------------------------------------------------ les gestes du heros

    /** Les coups du heros : partir, enchainer, toucher pendant la fenetre active ; esquiver, se garder, tirer. */
    private String gestes(float dt, Touches touches, Monde monde, histoire.Etat etat) {
        String message = null;
        Arme arme = etat.armeEnMain();
        boolean feu = arme != null && arme.aFeu();
        esquiveRepos = Math.max(0, esquiveRepos - dt);
        if (esquive > 0) {                                              // l'esquive : un bond de cote, rapide, puis il freine
            float v = 200 * Math.min(1f, esquive / 0.18f);
            float nx = monde.joueur.x + esquiveX * v * dt;
            float ny = monde.joueur.y + esquiveY * v * 0.6f * dt;
            decor.Scene s = monde.scene();
            if (!s.bloque(nx, monde.joueur.y, decor.Joueur.RAYON) && nx > s.xMin + 4 && nx < s.xMax - 4) {
                monde.joueur.x = nx;
            }
            if (!s.bloque(monde.joueur.x, ny, decor.Joueur.RAYON) && ny > s.yMin && ny < s.yMax) {
                monde.joueur.y = ny;
            }
            esquive -= dt;
            return null;
        }
        boolean recupere = geste == null || tempsGeste > geste.fin() * lenteur && !geste.aerien();
        if (touches.esquive() && esquiveRepos <= 0 && touche <= 0 && recupere && !monde.enLAir()) {
            esquiver(touches, monde, etat);
            return null;
        }
        if (touches.contre() && touche <= 0 && recupere && !monde.enLAir()) {
            Ennemi c = contrable(monde);
            if (c != null) {
                contrer(c, monde, etat);
                return null;
            }
        }
        garde = touches.garde() && geste == null && touche <= 0 && !monde.enLAir() && elan <= 0;
        if (garde) {
            recharge = 0;
            charge = -1;
            return null;
        }
        if (touches.changerArme() && geste == null && charge < 0) {
            return changerArme(etat);
        }
        if (touches.recharger() && feu && geste == null) {
            commencerRecharge(etat);
        }
        if (elan > 0) {                                                 // il se jette vers sa cible, puis frappe
            elan -= dt;
            Ennemi c = elanCible;
            if (c == null || !c.vivant() || c.auSol()) {
                elan = 0;
                return null;
            }
            float dx = c.acteur.x - monde.joueur.x;
            float dy = c.acteur.y - monde.joueur.y;
            monde.joueur.versDroite = dx >= 0;
            if (Math.abs(dx) <= 20 && Math.abs(dy) < 8 || elan <= 0) {
                elan = 0;
                chaineA = 1;
                partir(choisirCoupA(touches, monde, etat), etat, monde);
                return null;
            }
            float nx = monde.joueur.x + Math.signum(dx) * Math.min(Math.abs(dx) - 18, 300 * dt);
            float ny = monde.joueur.y + Math.signum(dy) * Math.min(Math.abs(dy), 90 * dt);
            if (!monde.scene().bloque(nx, ny, decor.Joueur.RAYON)) {
                monde.joueur.x = nx;
                monde.joueur.y = ny;
            } else {
                elan = 0;
            }
            return null;
        }
        if (geste != null && (touches.poing() || touches.attaque()) && tempsGeste > geste.debut() * lenteur * 0.5f) {
            suiteDemandee = true;                                       // rappuyer pendant le geste : le suivant s'enchaine
            suiteParA = touches.attaque();
            versLaCible = versLaCible(touches, monde);
        }
        if (charge >= 0) {                                              // K maintenu : on charge ; relache, le coup part
            charge += dt;
            if (!touches.tenue(java.awt.event.KeyEvent.VK_K)) {
                boolean fort = charge >= CHARGE;
                charge = -1;
                partir(fort ? PIED_FORT : PIED, etat, monde);
                chaine = 0;
            }
        } else if (geste == null && touche <= 0) {
            if (monde.enLAir()) {                                       // en l'air : les coups sautes
                if (coupsEnLAir < 2 && (touches.attaque() || touches.pied() || touches.poing())) {
                    Geste g = touches.attaque() && touches.basTenu() ? PLONGEON : touches.poing() ? POING_SAUTE : PIED_SAUTE;
                    coupsEnLAir++;
                    partir(g, etat, monde);
                }
            } else if (touches.attaque()) {
                attaquer(touches, monde, etat);
            } else if (touches.poing()) {
                partir(arme == null || feu ? POING : null, etat, monde);
                chaine = 1;
            } else if (touches.pied()) {
                charge = 0;
            } else if (touches.balayage()) {
                partir(BALAYAGE, etat, monde);
                chaine = 0;
            } else if (touches.lancer()) {
                preparerLancer(etat, monde);
            }
        }
        if (geste == null && feu && tirEnAttente > 0 && cadenceReste <= 0 && recharge <= 0 && degaine <= 0) {
            tirEnAttente = 0;
            tirer(arme, monde, etat, false);                            // l'appui garde pendant l'intervalle
        } else if (geste == null && feu && arme.automatique() && touches.attaqueTenue() && tirPose > 0 && !auCorps && charge < 0) {
            tirer(arme, monde, etat, true);                             // A maintenu : la rafale continue
        }
        if (geste != null) {
            tempsGeste += dt;
            float d = lenteur;
            if (geste.avance() > 0 && !geste.aerien() && tempsGeste >= geste.debut() * d * 0.5f && tempsGeste <= geste.fin() * d) {
                float sens = monde.joueur.versDroite ? 1 : -1;            // le corps suit le coup
                float nx = monde.joueur.x + sens * geste.avance() * dt;
                if (!monde.scene().bloque(nx, monde.joueur.y, decor.Joueur.RAYON) && !quelquUnDevant(monde, sens, 12)) {
                    monde.joueur.x = nx;
                }
            }
            if (geste == LANCER) {
                if (aLancer != null && tempsGeste >= geste.debut() * d) {   // l'objet quitte la main
                    float sens = monde.joueur.versDroite ? 1 : -1;
                    projectiles.add(new Projectile(aLancer, usureALancer, monde.joueur.x + sens * 8, monde.joueur.y, sens * 290));
                    aLancer = null;
                }
            } else if (tempsGeste >= geste.debut() * d && tempsGeste <= geste.fin() * d) {
                message = porter(monde, etat);
            }
            if (geste != null && tempsGeste >= geste.duree() * d) {
                boolean suiteJ = suiteDemandee && !suiteParA && etat.armeEnMain() == null && chaine >= 1 && chaine < 3;
                boolean suiteA = suiteDemandee && suiteParA && chaineA >= 1 && chaineA < maxChaineA(etat);
                Geste termine = geste;
                geste = null;
                suiteDemandee = false;
                if (suiteJ) {
                    chaine++;
                    partir(chaine == 2 ? DIRECT : CROCHET, etat, monde);
                } else if (suiteA && !termine.aerien()) {
                    chaineA++;
                    partir(choisirSuiteA(etat, monde), etat, monde);
                } else {
                    chaine = 0;
                    chaineA = 0;
                }
            }
        }
        return message;
    }

    /** Quelqu'un tout pres devant le heros (le corps qui suit un coup ne passe pas a travers). */
    private boolean quelquUnDevant(Monde monde, float sens, float distance) {
        for (Ennemi e : ennemis) {
            float dx = (e.acteur.x - monde.joueur.x) * sens;
            if (e.vivant() && !e.auSol() && dx > 0 && dx < distance && Math.abs(e.acteur.y - monde.joueur.y) < 8) {
                return true;
            }
        }
        return false;
    }

    /** Vrai si le joueur tient la direction de sa cible (A devient alors un coup puissant). */
    private boolean versLaCible(Touches touches, Monde monde) {
        float ix = touches.axeX();
        if (ix == 0) {
            return false;
        }
        Ennemi c = cible;
        float vers = c != null ? Math.signum(c.acteur.x - monde.joueur.x) : monde.joueur.versDroite ? 1 : -1;
        return Math.signum(ix) == vers;
    }

    /**
     * A : le systeme choisit. Une arme a feu et la cible loin (ou personne a portee de poing en plein combat) : un tir.
     * Sinon le corps a corps : un coup si la cible est a portee, un elan vers elle si elle est a quelques pas.
     */
    private void attaquer(Touches touches, Monde monde, histoire.Etat etat) {
        Arme a = etat.armeEnMain();
        boolean feu = a != null && a.aFeu();
        Ennemi c = choisirCible(monde, touches.axeX(), touches.axeY(), feu ? a.portee() : ELAN_MAX, feu);
        if (c != null && c != cible) {
            cible = c;                                                  // on change de cible : le style se recalcule pour elle
            float d = (float) Math.hypot(c.acteur.x - monde.joueur.x, (c.acteur.y - monde.joueur.y) * 2);
            auCorps = d < (CORPS_ENTREE + CORPS_SORTIE) / 2;
        }
        if (feu && (c != null ? !auCorps : actif() && cible == null)) {
            tirer(a, monde, etat, false);
            return;
        }
        if (c != null && !feu) {
            float dx = Math.abs(c.acteur.x - monde.joueur.x);
            float portee = a == null ? 26 : a.portee();
            if (dx > portee + 6 && dx <= ELAN_MAX && Math.abs(c.acteur.y - monde.joueur.y) < 30) {
                elan = 0.5f;                                            // trop loin pour frapper : il s'y jette
                elanCible = c;
                audio.jouer(Audio.Bus.EFFETS, "esquive", 0.4f, 0, 1.3f, 0);
                return;
            }
        }
        chaineA = atterri < 0.25f ? 2 : 1;                              // juste apres la reception : la suite du coup saute
        versLaCible = versLaCible(touches, monde);
        partir(choisirCoupA(touches, monde, etat), etat, monde);
        chaine = 0;
    }

    private int maxChaineA(histoire.Etat etat) {
        Arme a = etat.armeEnMain();
        if (a == null || a.aFeu()) {
            return 4;
        }
        return a.relique() ? 4 : a.famille() == Famille.LAME ? 3 : 2;
    }

    /** Le premier coup de A : selon l'arme, la direction tenue et l'etat de la cible. */
    private Geste choisirCoupA(Touches touches, Monde monde, histoire.Etat etat) {
        Arme a = etat.armeEnMain();
        if (a != null && !a.aFeu()) {
            return gesteArme(a, chaineA);
        }
        if (versLaCible || cible != null && cible.etat == Phase.VACILLE) {
            return POING_FORT;
        }
        return chaineA >= 2 ? DIRECT : POING;
    }

    /**
     * La suite de A : poing, direct, puis un pied ou un crochet (selon la garde de la cible, et pour ne pas repeter le
     * dernier), puis le circulaire. Tenir la direction de la cible ou la trouver vacillante donne l'uppercut.
     */
    private Geste choisirSuiteA(histoire.Etat etat, Monde monde) {
        Arme a = etat.armeEnMain();
        if (a != null && !a.aFeu()) {
            return gesteArme(a, chaineA);
        }
        if (versLaCible || cible != null && cible.etat == Phase.VACILLE) {
            return POING_FORT;
        }
        return switch (chaineA) {
            case 2 -> DIRECT;
            case 3 -> cible != null && (cible.etat == Phase.GARDE || cible.genre.garde > 0.4f) || hasard.nextBoolean() ? PIED : CROCHET;
            default -> CIRCULAIRE;
        };
    }

    /**
     * Les coups d'une arme de contact. Un objet ou une batte : de cote, puis par-dessus (le second couche, ou envoie
     * voler pour la batte). Le katana : deux coupes, puis une coupe montante qui souleve. La relique : trois coupes et
     * une quatrieme qui libere une onde.
     */
    private Geste gesteArme(Arme a, int pas) {
        float dur = a.duree();
        String n = a.id() + "_" + pas;
        if (a.famille() == Famille.LAME) {
            boolean fin = pas >= (a.relique() ? 4 : 3);
            boolean monte = pas == 3;
            float k = fin ? 1.4f : 1f;
            Effet effet = fin && a.relique() ? Effet.PROJETE : monte ? Effet.LANCE : Effet.LEGER;
            return new Geste(n + (monte ? "_monte" : pas % 2 == 0 ? "_revers" : ""), dur * k, dur * k * 0.3f, dur * k * 0.62f,
                    a.portee(), a.degats() * k, a.recul(), "heros_frappe", a.relique() ? "lame_relique" : "lame", effet, false, "heros_garde",
                    35, fin);
        }
        if (pas >= 2) {
            return new Geste(n + "_haut", dur * 1.25f, dur * 1.25f * 0.4f, dur * 1.25f * 0.65f, a.portee(), a.degats() * 1.3f,
                    a.recul() * 1.2f, "heros_frappe", a.id().equals("batte") ? "batte" : "coup",
                    a.id().equals("batte") ? Effet.PROJETE : Effet.SOL, false, "heros_garde", 20, true);
        }
        return new Geste(n, dur, dur * 0.35f, dur * 0.6f, a.portee(), a.degats(), a.recul(), "heros_frappe",
                a.id().equals("batte") ? "batte" : "coup", Effet.LEGER, false, "heros_garde", 15, false);
    }

    /** H : la canette ou la pierre d'abord, sinon l'arme en main ; rien a lancer, rien ne se passe. */
    private void preparerLancer(histoire.Etat etat, Monde monde) {
        histoire.Inventaire inv = etat.inventaire();
        String objet = inv.possede("pierre") ? "pierre" : inv.possede("canette") ? "canette" : null;
        int usure = 0;
        if (objet != null) {
            inv.retirer(objet, 1);
        } else if (inv.equipee() != null && !inv.equipee().fiche().mission()) {   // la relique ne se lance pas
            usure = inv.equipee().usure;
            objet = inv.lacher().id;
            recharge = 0;
        } else {
            return;
        }
        aLancer = objet;
        usureALancer = usure;
        partir(LANCER, etat, monde);
    }

    private void partir(Geste voulu, histoire.Etat etat, Monde monde) {
        viser(monde);
        Arme arme = etat.armeEnMain();
        geste = voulu != null ? voulu : new Geste(arme.id(), arme.duree(), arme.duree() * 0.35f, arme.duree() * 0.6f,
                arme.portee(), arme.degats(), arme.recul(), "heros_frappe", "coup");
        tempsGeste = 0;
        suiteDemandee = false;
        recharge = 0;
        lenteur = (etat.essouffle() ? 1.35f : 1f) * (combo() >= 5 ? 0.88f : 1f);   // a bout de souffle, plus lent ; en rythme, plus vif
        etat.effort(geste == PIED_FORT ? 16 : geste == PIED || geste == BALAYAGE || geste == CIRCULAIRE || geste == PIED_SAUTE ? 11
                : geste == CROCHET || geste == POING_FORT || geste == PLONGEON ? 8 : arme != null && voulu == null ? 9 : 5);
        ennemis.forEach(e -> e.touchePendantCeGeste = false);
        float sens = monde.joueur.versDroite ? 1 : -1;
        if (geste == PIED_SAUTE) {
            monde.elancer(sens * geste.avance());
        } else if (geste == PLONGEON) {
            monde.plonger(sens * geste.avance());
        }
        boolean lourd = geste == PIED || geste == CROCHET || geste == CIRCULAIRE || geste == POING_FORT || geste.aerien();
        if (geste.son().startsWith("lame")) {
            audio.jouer(Audio.Bus.EFFETS, geste.son(), 0.5f, 0, 0.9f + hasard.nextFloat() * 0.25f, 0.03f);
        } else {
            audio.jouer(Audio.Bus.EFFETS, "vetement", lourd ? 0.45f : 0.35f, 0, lourd ? 0.9f : 1.4f, 0);   // le geste fend l'air
        }
        audio.jouer(Audio.Bus.VOIX, "v_heros_grogne", lourd ? 0.3f : 0.2f, 0, lourd ? 1.1f : 1.3f, lourd ? 0.1f : 0.04f);
    }

    /** Le geste en cours est dans sa fenetre active : vrai pour toucher aussi un habitant qui passe. */
    Geste gesteActif() {
        return geste != null && tempsGeste >= geste.debut() * lenteur && tempsGeste <= geste.fin() * lenteur ? geste : null;
    }

    private final java.util.Set<Object> habitantsTouches = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private Geste dernierGeste;

    /** Vrai la premiere fois qu'un habitant est atteint par le geste en cours (une fois par geste). */
    boolean atteindre(Object habitant) {
        if (dernierGeste != geste) {
            dernierGeste = geste;
            habitantsTouches.clear();
        }
        if (geste != null && habitantsTouches.add(habitant)) {
            gel = 0.05f;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ esquive, garde, contre

    private void esquiver(Touches touches, Monde monde, histoire.Etat etat) {
        float ix = touches.axeX();
        float iy = touches.axeY();
        if (ix == 0 && iy == 0) {                                       // sans direction : en arriere, loin du plus proche
            Ennemi proche = null;
            float mieux = 999;
            for (Ennemi e : ennemis) {
                float d = Math.abs(e.acteur.x - monde.joueur.x);
                if (e.vivant() && d < mieux) {
                    proche = e;
                    mieux = d;
                }
            }
            ix = proche != null ? -Math.signum(proche.acteur.x - monde.joueur.x) : monde.joueur.versDroite ? -1 : 1;
            if (ix == 0) {
                ix = -1;
            }
        }
        float n = (float) Math.hypot(ix, iy);
        esquiveX = ix / n;
        esquiveY = iy / n;
        esquive = 0.3f;
        esquiveRepos = 0.55f;
        invincible = Math.max(invincible, 0.24f);
        geste = null;
        chaine = 0;
        chaineA = 0;
        charge = -1;
        elan = 0;
        etat.effort(8);
        effets.poussiere(monde.joueur.x, monde.joueur.y, 4);
        audio.jouer("esquive", 0.6f, 0, 1f);
    }

    /** L'ennemi dont le coup part a l'instant, a portee : celui qu'on peut contrer. */
    private Ennemi contrable(Monde monde) {
        Ennemi mieux = null;
        float d = 999;
        for (Ennemi e : ennemis) {
            if (!e.vivant() || !fenetreDeContre(e)) {
                continue;
            }
            float dx = Math.abs(e.acteur.x - monde.joueur.x);
            if (dx <= e.genre.portee + 14 && Math.abs(e.acteur.y - monde.joueur.y) < 20 && dx < d) {
                mieux = e;
                d = dx;
            }
        }
        return mieux;
    }

    /** Le coup de cet ennemi part dans un instant (ou part) : un contre maintenant le prend a contre-pied. */
    static boolean fenetreDeContre(Ennemi e) {
        return e.etat == Phase.PREPARE && !e.charge && e.minuteur <= 0.32f || e.etat == Phase.BOND;
    }

    private void contrer(Ennemi e, Monde monde, histoire.Etat etat) {
        e.etat = Phase.VACILLE;
        e.minuteur = 0.8f;
        e.coupsRestants = 0;
        e.colere = false;
        cible = e;
        monde.joueur.versDroite = e.acteur.x >= monde.joueur.x;
        invincible = Math.max(invincible, 0.5f);
        gel = 0.09f;
        effets.impact(e.acteur.x, e.acteur.y - 28, 0xE0F0FF, 9);
        audio.jouer("contre", 0.8f, 0, 1f);
        monde.trembler(2f);
        chaineA = 2;
        partir(RIPOSTE, etat, monde);
        e.touchePendantCeGeste = false;
    }

    // ------------------------------------------------------------------ les armes a feu

    /** V : l'arme suivante. Une arme a feu sort de la ceinture (un temps tres court), une arme de contact passe en main. */
    private String changerArme(histoire.Etat etat) {
        histoire.Inventaire inv = etat.inventaire();
        histoire.Inventaire.Ligne suivante = inv.armeSuivante();
        if (suivante == inv.equipee()) {
            return null;
        }
        inv.equiper(suivante);
        recharge = 0;
        Arme a = etat.armeEnMain();
        if (a != null && a.aFeu()) {
            degaine = 0.16f;
            audio.jouer("degaine", 0.6f, 0, 1f);
        } else {
            audio.jouer(Audio.Bus.EFFETS, a == null ? "vetement" : "ferraille", 0.4f, 0, 1.2f, 0);
        }
        return a == null ? "LES POINGS." : "EN MAIN : " + a.nom() + (a.aFeu() ? "  " + etat.chargees() + " / " + etat.reserve() : "") + ".";
    }

    /** R (ou un tir a vide) : le chargeur sort, un neuf entre, s'il en reste en poche. */
    private void commencerRecharge(histoire.Etat etat) {
        Arme a = etat.armeEnMain();
        if (a == null || !a.aFeu() || recharge > 0 || etat.chargees() >= a.chargeur()) {
            return;
        }
        if (etat.reserve() <= 0) {
            vide = 1.2f;
            audio.jouer("vide", 0.5f, 0, 0.9f);
            return;
        }
        recharge = a.rechargement();
        rechargeTotal = recharge;
        rechargeSon = false;
        audio.jouer("chargeur_sort", 0.6f, 0, 1f);
    }

    /**
     * Un tir : vers la cible (ou devant soi), avec la dispersion de l'arme et l'echauffement des tirs precedents. La balle
     * part du canon, s'arrete au premier mur ou au premier corps (le pistolet lourd en traverse un), perd de sa force au
     * loin. Le canon flambe, une douille saute, le heros encaisse le recul.
     */
    private void tirer(Arme a, Monde monde, histoire.Etat etat, boolean rafale) {
        if (recharge > 0 || degaine > 0 || cadenceReste > 0) {
            if (!rafale) {
                tirEnAttente = 0.3f;
            }
            return;
        }
        if (etat.chargees() <= 0) {
            if (!rafale) {
                vide = 1.2f;
                audio.jouer("vide", 0.6f, 0, 1f);
                commencerRecharge(etat);
            }
            tirPose = 0;
            return;
        }
        etat.tirer();
        cadenceReste = 1f / a.cadence();
        Ennemi c = cible != null && cible.vivant() && !cible.auSol() ? cible : null;
        if (c != null) {
            monde.joueur.versDroite = c.acteur.x >= monde.joueur.x;
        }
        float sens = monde.joueur.versDroite ? 1 : -1;
        float ax = c != null ? c.acteur.x - monde.joueur.x : sens;
        float ay = c != null ? c.acteur.y - monde.joueur.y : 0;
        if (Math.abs(ax) < 1) {
            ax = sens;
        }
        double angle = Math.atan2(ay, ax) + hasard.nextGaussian() * (a.dispersion() + bloom) * 0.6;
        bloom = Math.min(0.22f, bloom + a.echauffement());
        float bout = a.deuxMains() ? 21 : 14;
        float mx = monde.joueur.x + sens * bout;
        balle(monde, etat, mx, monde.joueur.y, (float) Math.cos(angle), (float) Math.sin(angle), a.portee(), a.degats(), a.perce(), true,
                a.famille(), 28 + monde.hauteurDuSaut());
        flash = 0.06f;
        tirPose = 0.55f;
        String son = switch (a.famille()) {
            case MITRAILLETTE -> "tir_mitraillette";
            case FUSIL -> "tir_fusil";
            default -> a.id().equals("pistolet_lourd") ? "tir_lourd" : "tir_pistolet";
        };
        audio.jouer(son, 0.85f, 0, 0.96f + hasard.nextFloat() * 0.08f);
        audio.jouer(Audio.Bus.EFFETS, "douille", 0.3f, 0, 0.9f + hasard.nextFloat() * 0.3f, 0.25f);
        effets.douille(monde.joueur.x + sens * 6, monde.joueur.y - 30 - monde.hauteurDuSaut(), sens);
        monde.trembler(a.famille() == Famille.FUSIL ? 1.4f : a.id().equals("pistolet_lourd") ? 1.8f : 0.7f);
        if (a.famille() == Famille.FUSIL || a.id().equals("pistolet_lourd")) {
            monde.repousser(-sens * 1.5f);                               // le recul pousse le heros
        }
        if (c != null && c.coupsRecus > 0 && c.etat == Phase.APPROCHE && hasard.nextFloat() < c.genre.esquive * 0.25f) {
            c.etat = Phase.ESQUIVE;                                     // il a compris : il plonge
            c.minuteur = 0.22f;
        }
    }

    /**
     * Une balle, du heros ou d'un ennemi, au ras du sol (x, y) dans la direction (cx, cy), tiree a hauteur h. Elle
     * s'arrete au premier mur. Celle du heros touche les ennemis (et les passants) ; celle d'un ennemi touche le heros.
     */
    private void balle(Monde monde, histoire.Etat etat, float x0, float y0, float cx, float cy, float portee, float degats, int perce,
                       boolean duHeros, Famille famille, float h) {
        decor.Scene scene = monde.scene();
        java.util.Set<Ennemi> deja = new java.util.HashSet<>();
        float fx = x0;
        float fy = y0;
        int traverses = perce;
        for (float d = 4; d <= portee; d += 3) {
            float x = x0 + cx * d;
            float y = y0 + cy * d;
            fx = x;
            fy = y;
            if (scene.bloque(x, y, 1) || x < scene.xMin - 20 || x > scene.xMax + 20) {
                effets.impact(x, y - h + 2, 0xC8C0A8, 4);              // la balle dans le mur
                audio.jouer(hasard.nextInt(3) == 0 ? "ricochet" : "impact_mur", 0.45f, 0, 0.9f + hasard.nextFloat() * 0.2f);
                traces.add(new Trace(x0, y0, x, y, h, new float[1]));
                return;
            }
            float force = d > portee * 0.6f ? 0.7f : 1f;
            if (duHeros) {
                for (Ennemi e : ennemis) {
                    if (!e.vivant() || e.auSol() || deja.contains(e) || Math.abs(e.acteur.x - x) > 5 || Math.abs(e.acteur.y - y) > 6
                            || e.z > 34) {
                        continue;
                    }
                    deja.add(e);
                    if (e.etat == Phase.ESQUIVE) {
                        continue;                                       // il a plonge : elle passe
                    }
                    toucherParBalle(e, degats * force, famille, Math.signum(cx), monde);
                    if (traverses-- <= 0) {
                        traces.add(new Trace(x0, y0, x, y, h, new float[1]));
                        return;
                    }
                }
                for (Monde.Habitant hab : monde.habitants()) {           // un passant sur la trajectoire
                    if (hab.acteur.texture != null && !hab.figurant.id().startsWith("scooter") && !"bagarre".equals(hab.reaction)
                            && Math.abs(hab.acteur.x - x) < 5 && Math.abs(hab.acteur.y - y) < 6) {
                        habitantsAtteints.add(hab);
                        effets.impact(x, y - 26, 0xD04030, 5);
                        traces.add(new Trace(x0, y0, x, y, h, new float[1]));
                        return;
                    }
                }
            } else if (Math.abs(monde.joueur.x - x) < 5 && Math.abs(monde.joueur.y - y) < 6) {
                if (invincible > 0 || heroAuSol()) {
                    continue;                                           // il esquive : la balle passe
                }
                traces.add(new Trace(x0, y0, x, y, h, new float[1]));
                encaisserBalle(degats * force, Math.signum(cx), monde, etat);
                return;
            }
        }
        traces.add(new Trace(x0, y0, fx, fy, h, new float[1]));
    }

    /** Une balle touche un ennemi : la reaction depend de l'arme (le pistolet le fait reculer, le lourd le couche...). */
    private void toucherParBalle(Ennemi e, float degats, Famille famille, float sens, Monde monde) {
        e.pv -= degats;
        e.eclat = 0.12f;
        e.coupsRecus++;
        e.serie = e.serieAge < 1.2f ? e.serie + 1 : 1;
        e.serieAge = 0;
        e.colere = false;
        marquer(e);
        effets.impact(e.acteur.x - sens * 3, e.acteur.y - 28 - e.z, 0xD04030, 5);
        audio.jouer("impact_chair", 0.6f, sens * 0.2f, 0.9f + hasard.nextFloat() * 0.2f);
        if (e.pv <= 0) {
            e.etat = Phase.TOMBE;
            e.minuteur = 0.9f;
            e.vx = sens * 90;
            return;
        }
        boolean lourde = degats >= 20;
        if (e.genre.armure > 0 && degats < e.genre.armure && e.serie < 6) {
            e.vx = sens * 20;                                           // la brute : elle encaisse sans s'arreter
            return;
        }
        if (lourde || famille == Famille.MITRAILLETTE && e.serie >= 5 || famille == Famille.FUSIL && e.serie >= 3) {
            e.etat = Phase.SOL;
            e.minuteur = 0.9f / niveau;
            e.vx = sens * 120;
            e.serie = 0;
            audio.jouer("corps_chute", 0.6f, 0, 1.1f);
        } else if (famille == Famille.FUSIL) {
            e.etat = Phase.VACILLE;
            e.minuteur = 0.4f;
            e.vx = sens * 50;
        } else if (e.etat != Phase.FRAPPE && e.etat != Phase.CHARGE) {
            e.etat = Phase.RECULE;                                      // la tete part en arriere, il perd son coup
            e.minuteur = famille == Famille.MITRAILLETTE ? 0.16f : 0.26f;
            e.vx = sens * 60;
        }
    }

    // ------------------------------------------------------------------ porter un coup

    /** Le coup du heros est dans sa fenetre active : il touche les ennemis devant lui, a portee, une fois chacun. */
    private String porter(Monde monde, histoire.Etat etat) {
        float sens = monde.joueur.versDroite ? 1 : -1;
        String message = null;
        boolean aTouche = false;
        float hz = monde.hauteurDuSaut();
        for (Ennemi e : ennemis) {
            float dx = (e.acteur.x - monde.joueur.x) * sens;
            boolean balaye = geste == BALAYAGE && Math.abs(dx) <= geste.portee();     // le balayage fait le tour
            if (!e.vivant() || e.auSol() || e.touchePendantCeGeste || !balaye && (dx < -4 || dx > geste.portee())
                    || Math.abs(e.acteur.y - monde.joueur.y) > 16 || geste == BALAYAGE && (e.etat == Phase.BOND || e.z > 4)) {
                continue;
            }
            if (geste.aerien() ? Math.abs(e.z - hz) > 34 : e.z > 22) {
                continue;                                               // trop haut, ou trop bas, pour ce coup
            }
            if (!ligneLibre(monde.scene(), monde.joueur.x, monde.joueur.y, e.acteur.x, e.acteur.y)) {
                continue;                                               // un meuble entre les deux
            }
            if (geste == BALAYAGE) {                                    // il perd ses appuis, garde ou pas
                e.touchePendantCeGeste = true;
                aTouche = true;
                e.pv -= geste.degats();
                e.eclat = 0.12f;
                e.etat = e.pv <= 0 ? Phase.TOMBE : Phase.SOL;
                e.minuteur = e.pv <= 0 ? 0.9f : 1.0f;
                e.vx = Math.signum(e.acteur.x - monde.joueur.x) * 30;
                gel = 0.06f;
                marquer(e);
                effets.impact(e.acteur.x, e.acteur.y - 6, 0xFFF0C0, 5);
                effets.poussiere(e.acteur.x, e.acteur.y, 5);
                audio.jouer("corps_chute", 0.7f, sens * 0.2f, 1.05f);
                monde.trembler(2.5f);
                continue;
            }
            e.touchePendantCeGeste = true;
            if (e.etat == Phase.ESQUIVE) {                              // il a recule a temps : dans le vide
                continue;
            }
            aTouche = true;
            boolean lourd = geste == PIED || geste == PIED_FORT || geste == CROCHET || geste.degats() > 18 || geste.effet() != Effet.LEGER;
            boolean deFace = (e.acteur.x - monde.joueur.x) * (e.acteur.miroir ? 1 : -1) >= 0;
            if (e.etat == Phase.GARDE && deFace && geste != PIED_FORT && !geste.brise() && !(geste == PIED && e.coupsBloques >= 1)) {
                e.coupsBloques++;                                       // la garde tient : peu de mal, il recule a peine
                e.pv -= geste.degats() * 0.2f;
                e.vx = sens * 40;
                e.minuteur = Math.max(e.minuteur, 0.25f);
                effets.impact(e.acteur.x - sens * 6, e.acteur.y - 24, 0xA0C8FF, 4);
                audio.jouer(e.genre.style == Style.SABRE ? "lame_touche" : "ferraille", 0.35f, sens * 0.2f, 1.6f);
                touche = Math.max(touche, 0.12f);                       // le coup rebondit sur sa garde
                gel = 0.04f;
                if (e.genre.style == Style.SABRE) {
                    e.repos = 0;                                        // le sabreur pare... et riposte aussitot
                    e.colere = true;
                }
                if (e.coupsBloques >= 3 || geste == PIED) {             // la garde cede
                    e.etat = Phase.RECULE;
                    e.minuteur = 0.45f;
                }
                continue;
            }
            e.coupsRecus++;
            e.serie = e.serieAge < 1.4f ? e.serie + 1 : 1;
            e.serieAge = 0;
            boolean contre = e.etat == Phase.FRAPPE || e.etat == Phase.CHARGE;
            e.pv -= geste.degats() * (contre ? 1.2f : e.etat == Phase.VACILLE ? 1.1f : 1f);   // le contre fait plus mal
            e.eclat = 0.16f;
            e.vx = sens * geste.recul();
            e.colere = false;
            marquer(e);
            reagir(e, sens, lourd, monde);
            gel = lourd ? 0.08f : 0.05f;
            int couleur = e.genre.danse ? 0xC8A0FF : geste.son().equals("lame_relique") ? 0xB0F0E8 : 0xFFF0C0;
            effets.impact(e.acteur.x - sens * 4, e.acteur.y - 26 - e.z, couleur, 6 + (int) geste.degats() / 3);
            monde.trembler(lourd ? 3.5f : 1.8f);
            audio.jouer(geste.son().startsWith("lame") ? "lame_touche" : geste.son(), 0.8f, sens * 0.2f, 0.9f + hasard.nextFloat() * 0.2f);
            if (e.pv <= 0) {
                audio.jouer(e.genre.danse ? "tele_rafale" : "corps_chute", e.genre.danse ? 0.35f : 0.7f, 0, 1.2f);
            } else {
                audio.jouer(Audio.Bus.VOIX, e.genre.danse ? "v_ecouteurs_oh" : "v_gars_geto_grogne", 0.5f, sens * 0.3f,
                        e.genre.danse ? 1.6f : 1f, 0.03f);
            }
        }
        if (aTouche && geste.son().equals("lame_relique") && geste.effet() == Effet.PROJETE) {
            onde(monde, etat, monde.joueur.x + sens * 40, 44, 12);      // la derniere coupe de la relique libere une onde
        }
        if (aTouche && etat.armeEnMain() != null && !etat.armeEnMain().aFeu() && !List.of("poing", "pied", "direct", "crochet",
                "pied_fort", "balayage", "circulaire", "poing_fort", "poing_saute", "pied_saute", "plongeon", "riposte").contains(geste.nom())
                && etat.userArme()) {
            boolean bouteille = geste.nom().startsWith("bouteille");
            audio.jouer(bouteille ? "tinte" : "ferraille", 0.7f, 0, 1.1f);
            message = bouteille ? "LA BOUTEILLE A ÉCLATÉ." : "TON ARME A CÉDÉ.";
        }
        if (aTouche && ennemis.stream().noneMatch(Ennemi::vivant)) {
            gel = 0.22f;                                                // le dernier tombe : un temps suspendu
        }
        return message;
    }

    /** Ce que le coup fait a l'ennemi, selon sa force et son genre. */
    private void reagir(Ennemi e, float sens, boolean lourd, Monde monde) {
        if (e.pv <= 0) {
            e.etat = Phase.TOMBE;
            e.minuteur = 0.9f;
            if (geste.effet() == Effet.PROJETE || geste.effet() == Effet.LANCE) {
                envoler(e, sens * 160, 140);
                e.etat = Phase.TOMBE;
            }
            return;
        }
        if (e.etat == Phase.VOLE) {                                     // repris en l'air : il y reste un peu
            e.vz = Math.max(e.vz, geste.aerien() ? 110 : 90);
            e.vx = sens * 60;
            return;
        }
        boolean brute = e.genre.style == Style.BRUTE;
        if (brute && geste.effet() == Effet.LEGER && geste.degats() < e.genre.armure && e.serie < 5 && geste != CROCHET && geste != PIED_FORT) {
            e.vx = sens * 25;                                           // la brute ne bronche pas pour si peu
            return;
        }
        switch (geste.effet()) {
            case PROJETE -> {
                if (brute) {
                    e.etat = Phase.VACILLE;
                    e.minuteur = 0.6f;
                } else {
                    envoler(e, sens * 210, 120);
                }
                return;
            }
            case LANCE -> {
                if (brute) {
                    e.etat = Phase.VACILLE;
                    e.minuteur = 0.7f;
                } else {
                    envoler(e, sens * 40, 215);
                }
                return;
            }
            case SOL -> {
                e.etat = Phase.SOL;
                e.minuteur = 0.95f / niveau;
                e.serie = 0;
                effets.poussiere(e.acteur.x, e.acteur.y, 5);
                audio.jouer("corps_chute", 0.6f, sens * 0.2f, 1.1f);
                return;
            }
            case LOURD -> {
                e.etat = Phase.VACILLE;
                e.minuteur = 0.7f;
                e.coupsRestants = 0;
                return;
            }
            default -> {
            }
        }
        if ((e.etat == Phase.FRAPPE || e.etat == Phase.PREPARE && e.armure) && !lourd) {
            e.minuteur = Math.max(e.minuteur, 0.05f);                   // en plein elan : un petit coup ne l'arrete pas
        } else if (e.serie >= 3 || lourd && e.serie >= 2 || geste == CROCHET || geste == PIED_FORT) {
            e.etat = Phase.SOL;                                         // il tombe
            e.minuteur = 0.85f / niveau;
            e.serie = 0;
            effets.poussiere(e.acteur.x, e.acteur.y, 4);
            audio.jouer("corps_chute", 0.6f, sens * 0.2f, 1.1f);
        } else {
            e.armure |= e.etat == Phase.PREPARE;                        // coupe une fois : la prochaine fois, il tiendra
            e.etat = Phase.RECULE;
            e.minuteur = 0.32f + geste.recul() / 900f;                  // touche, il perd le coup qu'il preparait
        }
    }

    /** L'ennemi decolle : projete (vers l'arriere) ou souleve (vers le haut). Il retombera, et restera au sol un moment. */
    private void envoler(Ennemi e, float vx, float vz) {
        e.etat = Phase.VOLE;
        e.vx = vx;
        e.vz = vz;
        e.z = Math.max(e.z, 1);
        e.coupsRestants = 0;
        e.serie = 0;
    }

    // ------------------------------------------------------------------ les objets lances

    /** Les objets lances : ils volent, touchent un ennemi (ou un passant), puis retombent la ou ils tombent. */
    private void voler(float dt, Monde monde, histoire.Etat etat) {
        for (Projectile p : projectiles) {
            if (p.fini) {
                continue;
            }
            p.x += p.vx * dt;
            p.vz -= 320 * dt;
            p.z += p.vz * dt;
            if (monde.scene().bloque(p.x, p.y, 2)) {                      // un mur : il s'y arrete
                p.z = 0;
            }
            for (Ennemi e : ennemis) {
                if (!e.vivant() || e.auSol() || Math.abs(e.acteur.x - p.x) > 9 || Math.abs(e.acteur.y - p.y) > 12 || p.z > 36) {
                    continue;
                }
                if (e.etat == Phase.ESQUIVE) {
                    continue;
                }
                toucher(e, p, monde);
                break;
            }
            if (!p.fini) {
                for (Monde.Habitant h : monde.habitants()) {            // un passant sur la trajectoire
                    if (h.acteur.texture != null && !h.figurant.id().startsWith("scooter") && !"bagarre".equals(h.reaction)
                            && Math.abs(h.acteur.x - p.x) < 8 && Math.abs(h.acteur.y - p.y) < 12 && p.z < 36) {
                        habitantsAtteints.add(h);
                        p.fini = true;
                        effets.impact(p.x, p.y - p.z, 0xFFF0C0, 5);
                        audio.jouer("choc", 0.6f, 0, 1.2f);
                        break;
                    }
                }
            }
            if (!p.fini && p.z <= 0) {
                p.fini = true;
                retomber(p, monde, etat);
            }
        }
        projectiles.removeIf(p -> p.fini);
    }

    private void toucher(Ennemi e, Projectile p, Monde monde) {
        p.fini = true;
        float sens = Math.signum(p.vx);
        float degats = switch (p.objet) {
            case "pierre" -> 10;
            case "canette" -> 4;
            case "bouteille" -> 14;
            case "barre", "batte" -> 16;
            default -> 12;
        };
        boolean lourd = p.objet.equals("barre") || p.objet.equals("planche") || p.objet.equals("batte");
        e.pv -= degats;
        e.eclat = 0.16f;
        e.vx = sens * (lourd ? 160 : 90);
        e.colere = false;
        marquer(e);
        if (e.pv <= 0) {
            e.etat = Phase.TOMBE;
            e.minuteur = 0.9f;
        } else if (lourd) {
            e.etat = Phase.SOL;
            e.minuteur = 0.85f;
        } else {
            e.etat = Phase.RECULE;
            e.minuteur = 0.45f;
        }
        gel = 0.05f;
        effets.impact(p.x, p.y - p.z, p.objet.equals("bouteille") ? 0xA0E0B0 : 0xFFF0C0, 8);
        monde.trembler(2f);
        audio.jouer(p.objet.equals("bouteille") ? "tinte" : p.objet.equals("canette") ? "tinte" : "choc", 0.75f, sens * 0.2f,
                p.objet.equals("canette") ? 1.4f : 1f);
        retomber(p, monde, null);
    }

    /** Ce qui retombe : une arme reste par terre (on la ramasse), une bouteille eclate, une canette, une pierre roulent au loin. */
    private void retomber(Projectile p, Monde monde, histoire.Etat etat) {
        if (p.objet.equals("bouteille")) {
            effets.impact(p.x, p.y, 0xA0E0B0, 10);
            audio.jouer("tinte", 0.6f, 0, 1.3f);
            return;
        }
        if (histoire.Etat.arme(p.objet) != null) {
            monde.poserAuSol(new histoire.Etat.AuSol(p.objet, p.usure, monde.carte(), p.x, p.y));
            audio.jouer("ferraille", 0.6f, 0, 0.9f);
        } else {
            audio.jouer(p.objet.equals("pierre") ? "cailloux" : "tinte", 0.4f, 0, 0.9f);
        }
    }

    // ------------------------------------------------------------------ les corps

    /** Les corps ne passent pas les uns dans les autres : deux ennemis trop proches s'ecartent, et laissent la place au heros. */
    private void ecarterLesCorps(float dt, Monde monde) {
        for (int i = 0; i < ennemis.size(); i++) {
            Ennemi a = ennemis.get(i);
            if (!a.vivant()) {
                continue;
            }
            float rayonA = a.genre.style == Style.BRUTE ? 17 : 13;
            for (int j = i + 1; j < ennemis.size(); j++) {
                Ennemi b = ennemis.get(j);
                if (!b.vivant()) {
                    continue;
                }
                float dx = b.acteur.x - a.acteur.x;
                float r = Math.max(rayonA, b.genre.style == Style.BRUTE ? 17 : 13);
                if (Math.abs(dx) < r && Math.abs(b.acteur.y - a.acteur.y) < 9) {
                    float pousse = (r - Math.abs(dx)) * 4 * dt * (dx >= 0 ? 1 : -1);
                    pousser(monde, a, -pousse);
                    pousser(monde, b, pousse);
                }
            }
            float dh = a.acteur.x - monde.joueur.x;
            if (Math.abs(dh) < 9 && Math.abs(a.acteur.y - monde.joueur.y) < 7 && a.etat != Phase.BOND && a.etat != Phase.CHARGE
                    && !monde.enLAir()) {
                pousser(monde, a, (9 - Math.abs(dh)) * 6 * dt * (dh >= 0 ? 1 : -1));
            }
        }
    }

    private static void pousser(Monde monde, Ennemi e, float dx) {
        if (!monde.scene().bloque(e.acteur.x + dx, e.acteur.y, 6)) {
            e.acteur.x += dx;
        }
    }

    // ------------------------------------------------------------------ le heros encaisse

    /** Le heros encaisse un coup : il recule ; trop fort ou trop souvent, il tombe. La garde en prend l'essentiel. */
    private void encaisser(Ennemi e, float dx, Monde monde, histoire.Etat etat) {
        float degats = e.genre.degats * (e.colere ? 1.2f : 1f) * niveau;
        boolean deFace = Math.signum(e.acteur.x - monde.joueur.x) == (monde.joueur.versDroite ? 1 : -1);
        if (garde && deFace && !e.charge) {                             // la garde : les avant-bras prennent le coup
            etat.coup(degats * 0.15f);
            etat.effort(10);
            audio.jouer("garde_bloque", 0.7f, 0, 1f);
            effets.impact(monde.joueur.x + Math.signum(e.acteur.x - monde.joueur.x) * 8, monde.joueur.y - 30, 0xA0C8FF, 4);
            monde.repousser(Math.signum(dx) * 4);
            gel = 0.04f;
            if (etat.essouffle()) {                                     // plus de souffle : la garde cede
                garde = false;
                touche = 0.45f;
                audio.jouer(Audio.Bus.VOIX, "v_heros_soupir", 0.4f, 0, 1f, 0);
            }
            return;
        }
        etat.coup(degats);
        effets.blessure(degats / 25f);
        effets.impact(monde.joueur.x, monde.joueur.y - 26 - monde.hauteurDuSaut(), 0xFF6050, 7);
        audio.jouer("coup", 0.8f, 0, 0.8f);
        coupsEncaisses = encaisseAge < 2.5f ? coupsEncaisses + 1 : 1;
        encaisseAge = 0;
        geste = null;                                                   // un coup recu coupe le sien, et le rechargement
        chaine = 0;
        chaineA = 0;
        elan = 0;
        recharge = 0;
        combo = 0;
        gel = 0.06f;
        if (monde.enLAir() || e.charge) {                               // cueilli en l'air, ou charge : il est projete
            monde.projeter(Math.signum(dx) * (e.charge ? 190 : 120), e.charge ? 150 : 90);
            coupsEncaisses = 0;
            invincible = 2f;
            monde.trembler(e.charge ? 5f : 3.5f);
            audio.jouer(Audio.Bus.VOIX, "v_heros_soupir", 0.4f, 0, 0.9f, 0.1f);
            return;
        }
        if (coupsEncaisses >= 3 || degats >= 12 && etat.sante() < 40) {
            chute = 0.85f;                                              // il tombe
            coupsEncaisses = 0;
            invincible = 2.2f;
            monde.repousser(Math.signum(dx) * 16);
            monde.trembler(4f);
            audio.jouer("corps_chute", 0.75f, 0, 0.95f);
            audio.jouer(Audio.Bus.VOIX, "v_heros_soupir", 0.4f, 0, 0.9f, 0.1f);
        } else {
            touche = 0.32f;
            invincible = 0.3f;                                          // assez pour respirer, pas assez pour ignorer un enchainement
            monde.repousser(Math.signum(dx) * 9);
        }
    }

    /** Une balle touche le heros : moins qu'un coup de poing, mais rien ne la pare. */
    private void encaisserBalle(float degats, float sens, Monde monde, histoire.Etat etat) {
        degats *= niveau;
        etat.coup(degats);
        effets.blessure(degats / 22f);
        effets.impact(monde.joueur.x, monde.joueur.y - 28 - monde.hauteurDuSaut(), 0xFF5040, 6);
        audio.jouer("impact_chair", 0.7f, 0, 0.85f);
        coupsEncaisses = encaisseAge < 2.5f ? coupsEncaisses + 1 : 1;
        encaisseAge = 0;
        geste = null;
        chaine = 0;
        chaineA = 0;
        recharge = 0;
        combo = 0;
        garde = false;
        touche = 0.22f;
        invincible = 0.18f;
        monde.repousser(sens * 5);
        monde.trembler(1.6f);
        if (coupsEncaisses >= 4) {
            chute = 0.8f;
            coupsEncaisses = 0;
            invincible = 2f;
            audio.jouer("corps_chute", 0.7f, 0, 0.95f);
        }
    }

    // ------------------------------------------------------------------ les ennemis

    /** Un ennemi : il approche, tourne, arme son coup, frappe et enchaine, encaisse, se garde, esquive, tombe, se releve. */
    private void vivre(Ennemi e, float dt, Monde monde, histoire.Etat etat) {
        Acteur a = e.acteur;
        e.eclat = Math.max(0, e.eclat - dt);
        e.minuteur = Math.max(0, e.minuteur - dt);
        e.repos = Math.max(0, e.repos - dt);
        e.flash = Math.max(0, e.flash - dt);
        e.cadence = Math.max(0, e.cadence - dt);
        e.serieAge += dt;
        e.phase += dt;
        if (e.z > 0 || e.etat == Phase.VOLE) {                          // en l'air : il monte, retombe, et reste au sol
            e.z += e.vz * dt;
            e.vz -= 520 * dt;
            if (e.z <= 0) {
                e.z = 0;
                e.vz = 0;
                effets.poussiere(a.x, a.y, 6);
                audio.jouer("corps_chute", 0.7f, 0, 1f);
                monde.trembler(2f);
                if (e.etat == Phase.VOLE) {
                    e.etat = Phase.SOL;
                    e.minuteur = 1.1f / niveau;
                }
                e.vx *= 0.4f;
            }
        }
        a.hauteur = Math.round(e.z);
        if (e.vx != 0) {                                                // le recul s'amortit
            float nx = a.x + e.vx * dt;
            if (!monde.scene().bloque(nx, a.y, 6)) {
                a.x = nx;
            } else if (e.etat == Phase.CHARGE) {
                finirCharge(e, monde, true);                            // la brute s'ecrase contre le mur
            } else {
                e.vx = 0;
            }
            if (e.etat != Phase.CHARGE) {
                e.vx *= Math.max(0, 1 - dt * (e.z > 0 ? 1.5f : 7));
                if (Math.abs(e.vx) < 4) {
                    e.vx = 0;
                }
            }
        }
        habiller(e);
        if (!e.vivant()) {
            a.marche = false;
            lacherSonArme(e, monde);
            if (e.minuteur <= 0 && a.texture != null && e.z <= 0) {
                if (e.genre.danse) {
                    effets.impact(a.x, a.y - 10, 0xA0FFE0, 14);              // il se defait en eclats iridescents
                    a.texture = null;
                }
            }
            return;
        }
        float dx = monde.joueur.x - a.x;
        float dy = monde.joueur.y - a.y;
        if (e.etat != Phase.BOND && e.etat != Phase.ESQUIVE && e.etat != Phase.CHARGE && e.etat != Phase.VOLE) {
            a.miroir = dx < 0;
        }
        long attaquants = ennemis.stream().filter(o -> o.etat == Phase.PREPARE || o.etat == Phase.FRAPPE || o.etat == Phase.BOND
                || o.etat == Phase.CHARGE).count();
        switch (e.etat) {
            case VOLE -> a.marche = false;
            case RECULE -> {
                if (e.minuteur <= 0) {
                    e.etat = Phase.APPROCHE;
                    e.repos = Math.max(e.repos, 0.3f);
                }
            }
            case VACILLE -> {                                           // il chancelle : il ne peut rien faire
                a.marche = false;
                if (e.minuteur <= 0) {
                    e.etat = Phase.APPROCHE;
                    e.repos = Math.max(e.repos, 0.35f);
                }
            }
            case SOL -> {
                a.marche = false;
                if (e.minuteur <= 0) {
                    e.etat = Phase.RELEVE;
                    e.minuteur = 0.45f;
                }
            }
            case RELEVE -> {
                if (e.minuteur <= 0) {                                  // debout, et en colere : il attaque tout de suite
                    e.etat = Phase.APPROCHE;
                    e.repos = e.tireur() ? 0.6f : 0;
                    e.colere = true;
                    e.armure = true;
                }
            }
            case GARDE -> {
                a.marche = false;
                if (e.minuteur <= 0) {
                    e.etat = Phase.APPROCHE;
                    e.coupsBloques = 0;
                    e.repos = Math.min(e.repos, 0.2f);                  // apres la garde, il contre
                }
            }
            case ESQUIVE -> {
                float nx = a.x - Math.signum(dx) * 150 * dt;
                if (!monde.scene().bloque(nx, a.y, 6)) {
                    a.x = nx;
                }
                a.marche = true;
                if (e.minuteur <= 0) {
                    e.etat = Phase.APPROCHE;
                    e.repos = 0.15f;
                }
            }
            case BOND -> {                                              // l'alien se jette sur le heros
                float sens = a.miroir ? -1 : 1;
                float nx = a.x + sens * 250 * dt;
                if (!monde.scene().bloque(nx, a.y, 6)) {
                    a.x = nx;
                }
                if (Math.abs(monde.joueur.x - a.x) < 14 && Math.abs(dy) < 14 && invincible <= 0 && !heroAuSol() && !monde.enLAir()) {
                    encaisser(e, dx, monde, etat);
                    e.minuteur = 0;
                }
                if (e.minuteur <= 0) {
                    e.etat = Phase.FRAPPE;
                    e.minuteur = 0.25f;
                    e.coupsRestants = 0;
                }
            }
            case CHARGE -> {                                            // la brute fonce, droit devant
                a.marche = true;
                if (Math.abs(monde.joueur.x - a.x) < 16 && Math.abs(dy) < 14 && invincible <= 0 && !heroAuSol()
                        && monde.hauteurDuSaut() < 12) {
                    encaisser(e, dx, monde, etat);
                    e.charge = false;
                    e.etat = Phase.FRAPPE;
                    e.minuteur = 0.5f;
                    e.vx = 0;
                } else if (e.minuteur <= 0) {
                    finirCharge(e, monde, false);
                }
            }
            case PREPARE -> {
                a.marche = false;
                if (e.charge && hasard.nextInt(5) == 0) {
                    effets.poussiere(a.x, a.y, 1);                      // la brute gratte le sol
                }
                if (e.minuteur <= 0) {
                    if (e.charge) {                                     // la brute s'elance
                        e.etat = Phase.CHARGE;
                        e.minuteur = 0.85f;
                        e.vx = Math.signum(dx) * 265;
                        a.miroir = dx < 0;
                        audio.jouer(Audio.Bus.VOIX, "v_gars_geto_grogne", 0.6f, 0, 0.7f, 0);
                        break;
                    }
                    if (e.genre.bond && Math.abs(dx) > e.genre.portee + 10) {
                        e.etat = Phase.BOND;                            // arme de loin : il bondit
                        e.minuteur = 0.28f;
                        audio.jouer(Audio.Bus.EFFETS, "vetement", 0.4f, 0, 1.3f, 0);
                        break;
                    }
                    e.etat = Phase.FRAPPE;
                    e.minuteur = 0.18f;
                    boolean aPortee = Math.abs(dx) <= e.genre.portee + 6 && Math.abs(dy) < 18;
                    // en l'air, le coup passe sous lui ; sauf s'il l'a vu sauter et a vise haut
                    if (aPortee && invincible <= 0 && !heroAuSol() && (!monde.enLAir() || e.haut)) {
                        encaisser(e, dx, monde, etat);
                    } else {
                        audio.jouer(Audio.Bus.EFFETS, e.genre.style == Style.SABRE ? "lame" : "vetement", 0.3f, 0, 0.8f, 0);
                    }
                    e.haut = false;
                }
            }
            case FRAPPE -> {
                if (e.minuteur <= 0) {
                    boolean aPortee = Math.abs(dx) <= e.genre.portee + 8 && Math.abs(dy) < 16;
                    if (e.coupsRestants > 0 && aPortee && !heroAuSol()) {   // il enchaine
                        e.coupsRestants--;
                        e.etat = Phase.PREPARE;
                        e.minuteur = e.genre.preparation * 0.45f / niveau;
                    } else {
                        e.etat = Phase.APPROCHE;
                        e.repos = e.genre.repos * (0.8f + hasard.nextFloat() * 0.5f) * (e.colere ? 0.5f : 1f) / niveau;
                        e.colere = false;
                        e.armure = false;
                    }
                }
            }
            case VISE -> viser(e, dt, monde, etat);
            case TIRE -> tirerEnnemi(e, monde, etat);
            case RECHARGE -> {
                a.marche = false;
                if (e.minuteur <= 0) {
                    Arme arme = e.arme();
                    e.munitions = arme == null ? 0 : arme.chargeur();
                    e.etat = Phase.APPROCHE;
                    e.repos = 0.3f;
                    audio.jouer("culasse", 0.4f, 0, 1.1f);
                }
            }
            default -> {
                if (e.tireur()) {
                    tenirDistance(e, dt, monde, dx, dy, attaquants);
                } else {
                    approcher(e, dt, monde, dx, dy, attaquants);
                }
            }
        }
    }

    private void finirCharge(Ennemi e, Monde monde, boolean mur) {
        e.charge = false;
        e.vx = 0;
        e.etat = Phase.VACILLE;                                         // essouffle, ou sonne par le mur : c'est le moment
        e.minuteur = mur ? 1.1f : 0.7f;
        effets.poussiere(e.acteur.x, e.acteur.y, mur ? 10 : 5);
        if (mur) {
            monde.trembler(3f);
            audio.jouer("coup_sourd", 0.6f, 0, 0.8f);
        }
    }

    /** Un ennemi tombe : son arme lui echappe et reste par terre, on peut la ramasser (avec ce qui reste dans le chargeur). */
    private void lacherSonArme(Ennemi e, Monde monde) {
        if (e.depouille || e.genre.arme == null || rue != null) {
            return;
        }
        e.depouille = true;
        Arme a = e.arme();
        int usure = a != null && a.aFeu() ? Math.max(e.munitions, a.chargeur() / 3) : 0;
        float sens = e.acteur.miroir ? 1 : -1;
        monde.poserAuSol(new histoire.Etat.AuSol(e.genre.arme, usure, monde.carte(), e.acteur.x + sens * 10, e.acteur.y + 2));
        audio.jouer("ferraille", 0.5f, 0, 0.9f);
    }

    private void approcher(Ennemi e, float dt, Monde monde, float dx, float dy, long attaquants) {
        Acteur a = e.acteur;
        float sensHeros = monde.joueur.versDroite ? 1 : -1;
        boolean face = (a.x - monde.joueur.x) * sensHeros > 0;
        // le heros arme un coup vers lui : un ennemi alerte se garde ou esquive (il a deja pris des coups)
        if (geste != null && face && Math.abs(dx) < geste.portee() + 14 && Math.abs(dy) < 18
                && (e.coupsRecus > 0 || e.genre.style == Style.SABRE)
                && tempsGeste < geste.debut() * lenteur && !e.touchePendantCeGeste) {
            float tirage = hasard.nextFloat();
            e.touchePendantCeGeste = true;                              // une decision par geste
            if (tirage < e.genre.esquive) {
                e.etat = Phase.ESQUIVE;
                e.minuteur = 0.24f;
                return;
            }
            if (tirage < e.genre.esquive + e.genre.garde && !geste.aerien()) {
                e.etat = Phase.GARDE;
                e.minuteur = 0.55f;
                return;
            }
        }
        for (Projectile p : projectiles) {                              // un objet arrive sur lui : il l'evite, s'il a appris
            if (!p.fini && e.coupsRecus > 0 && Math.abs(p.y - a.y) < 12 && (a.x - p.x) * Math.signum(p.vx) > 0
                    && Math.abs(a.x - p.x) < 50 && hasard.nextFloat() < e.genre.esquive * 0.08f) {
                e.etat = Phase.ESQUIVE;
                e.minuteur = 0.22f;
                return;
            }
        }
        boolean derriere = !face;                                       // dans le dos du heros : il n'attend pas
        boolean aPortee = Math.abs(dx) <= e.genre.portee && Math.abs(dy) < 14;
        boolean libre = attaquants < 2 || e.colere;
        if (aPortee && (e.repos <= 0 || derriere && e.repos < 0.4f) && libre && !heroAuSol()) {
            e.haut = monde.enLAir() && hasard.nextFloat() < e.genre.antiair * 3;   // il l'a vu sauter : il vise haut
            preparer(e, derriere || e.colere ? 0.7f : 1f);
            return;
        }
        if (e.genre.style == Style.BRUTE && e.repos <= 0 && libre && !heroAuSol() && Math.abs(dx) > 60 && Math.abs(dx) < 170
                && Math.abs(dy) < 16 && ligneLibre(monde.scene(), a.x, a.y, monde.joueur.x, monde.joueur.y) && hasard.nextInt(40) == 0) {
            preparer(e, 1.2f);                                          // la brute gratte le sol... et chargera
            e.charge = true;
            e.coupsRestants = 0;
            return;
        }
        if (e.genre.bond && e.repos <= 0 && libre && !heroAuSol() && Math.abs(dx) > 44 && Math.abs(dx) < 80 && Math.abs(dy) < 10
                && hasard.nextInt(90) == 0) {
            preparer(e, 1.2f);                                          // il se ramasse... et bondira
            return;
        }
        // il approche ; s'il doit attendre son tour, il garde ses distances et tourne autour
        boolean attend = attaquants >= 2 && !e.colere || e.repos > 0 || heroAuSol();
        float voulu = attend ? 46 + e.phase % 1 * 10 : e.genre.portee - 6;
        float vers = Math.abs(dx) > voulu ? Math.signum(dx) : Math.abs(dx) < voulu - 14 ? -Math.signum(dx) : 0;
        if (attend) {                                                   // il encercle : il prend son cote, il ne fait pas la queue
            float place = monde.joueur.x + e.flanc * voulu;
            float ecart = place - a.x;
            vers = Math.abs(ecart) > 6 ? Math.signum(ecart) : 0;
            if (Math.abs(dx) < 24 && Math.signum(ecart) == Math.signum(dx)) {
                dy += a.y > monde.joueur.y ? 22 : -22;                   // il passe en arc, pas a travers le heros
            }
        }
        float vy = Math.signum(dy) * Math.min(Math.abs(dy), 30) / 30f;
        if (attend) {
            vy += (float) Math.sin(e.phase * 1.7f + e.genre.pv) * 0.6f;    // il tourne autour
        }
        if (e.genre.danse) {                                            // les aliens avancent en dansant
            vy += (float) Math.sin(e.phase * 5) * 0.9f;
        }
        deplacer(e, vers, vy, e.genre.vitesse * (e.colere ? 1.25f : 1f), dt, monde);
        a.marche = vers != 0 || e.genre.danse || attend && Math.abs(vy) > 0.2f;
    }

    private static void deplacer(Ennemi e, float vers, float vy, float vitesse, float dt, Monde monde) {
        Acteur a = e.acteur;
        float nx = a.x + vers * vitesse * dt;
        float ny = a.y + vy * vitesse * 0.5f * dt;
        decor.Scene scene = monde.scene();
        ny = Math.max(scene.yMin + 2, Math.min(scene.yMax - 2, ny));
        if (!scene.bloque(nx, ny, 6)) {
            a.x = nx;
            a.y = ny;
        } else if (!scene.bloque(nx, a.y, 6)) {                         // il glisse le long de l'obstacle
            a.x = nx;
        } else if (!scene.bloque(a.x, ny, 6)) {
            a.y = ny;
        } else if (!scene.bloque(a.x, a.y + 8, 6)) {                    // coince : il contourne par le bas
            a.y += 30 * dt;
        }
    }

    /**
     * Le tireur garde ses distances : il recule si le heros approche, se rapproche s'il est trop loin ou s'il n'a pas de
     * ligne de tir, et se decale pour en trouver une. Son tour venu (un seul tireur a la fois), il vise ; de tout pres, il
     * frappe avec la crosse.
     */
    private void tenirDistance(Ennemi e, float dt, Monde monde, float dx, float dy, long attaquants) {
        Acteur a = e.acteur;
        float d = Math.abs(dx);
        Arme arme = e.arme();
        boolean ligne = ligneLibre(monde.scene(), a.x, a.y, monde.joueur.x, monde.joueur.y);
        if (d <= e.genre.portee && Math.abs(dy) < 14 && e.repos <= 0 && attaquants < 2 && !heroAuSol()) {
            preparer(e, 1f);                                            // trop pres pour tirer : la crosse
            return;
        }
        long tirent = ennemis.stream().filter(o -> o.etat == Phase.VISE || o.etat == Phase.TIRE).count();
        if (e.repos <= 0 && tirent == 0 && ligne && d > 34 && d < arme.portee() * 0.85f && Math.abs(dy) < 30 && !heroAuSol()) {
            if (e.munitions <= 0) {
                e.etat = Phase.RECHARGE;
                e.minuteur = 1.4f;
                audio.jouer("chargeur_sort", 0.4f, 0, 1f);
                return;
            }
            e.etat = Phase.VISE;
            e.minuteur = 0.6f / niveau;
            e.viseX = monde.joueur.x;
            e.viseY = monde.joueur.y;
            e.tirsRestants = arme.automatique() ? 5 : 1;
            audio.jouer(Audio.Bus.EFFETS, "culasse", 0.3f, 0, 1.2f, 0);
            return;
        }
        float vers = d < 80 ? -Math.signum(dx) : d > 150 || !ligne ? Math.signum(dx) : 0;
        float vy = !ligne ? (float) Math.sin(e.phase * 0.9f) : Math.signum(dy) * Math.min(Math.abs(dy), 20) / 30f
                + (float) Math.sin(e.phase * 1.3f) * 0.4f;
        deplacer(e, vers, vy, e.genre.vitesse * (vers < 0 ? 0.85f : 1f), dt, monde);
        a.marche = vers != 0 || Math.abs(vy) > 0.25f;
    }

    /** Il vise : le trait rouge suit le heros avec un temps de retard. Puis il tire. */
    private void viser(Ennemi e, float dt, Monde monde, histoire.Etat etat) {
        e.acteur.marche = false;
        float k = Math.min(1, dt * 3.5f);
        e.viseX += (monde.joueur.x - e.viseX) * k;
        e.viseY += (monde.joueur.y - e.viseY) * k;
        if (e.minuteur <= 0) {
            e.etat = Phase.TIRE;
            e.cadence = 0;
        }
    }

    /** La rafale (ou la balle) du tireur, vers l'endroit ou il vise ; puis il se repose, ou recharge. */
    private void tirerEnnemi(Ennemi e, Monde monde, histoire.Etat etat) {
        Arme arme = e.arme();
        e.acteur.marche = false;
        e.viseX += (monde.joueur.x - e.viseX) * 0.08f;
        e.viseY += (monde.joueur.y - e.viseY) * 0.08f;
        if (e.cadence > 0) {
            return;
        }
        if (e.tirsRestants <= 0 || e.munitions <= 0) {
            e.etat = e.munitions <= 0 ? Phase.RECHARGE : Phase.APPROCHE;
            e.minuteur = e.munitions <= 0 ? 1.4f : 0;
            e.repos = e.genre.repos * (0.8f + hasard.nextFloat() * 0.6f) / niveau;
            if (e.munitions <= 0) {
                audio.jouer("chargeur_sort", 0.4f, 0, 1f);
            }
            return;
        }
        e.tirsRestants--;
        e.munitions--;
        e.cadence = 1f / arme.cadence() * 1.3f;
        float sens = e.acteur.miroir ? -1 : 1;
        float ax = e.viseX - e.acteur.x;
        float ay = e.viseY - e.acteur.y;
        double angle = Math.atan2(ay, Math.abs(ax) < 1 ? sens : ax) + hasard.nextGaussian() * arme.dispersion() * 1.6;
        float mx = e.acteur.x + sens * (arme.deuxMains() ? 18 : 13);
        e.flash = 0.06f;
        balle(monde, etat, mx, e.acteur.y, (float) Math.cos(angle), (float) Math.sin(angle), arme.portee(), arme.degats() * 0.6f, 0, false,
                arme.famille(), 28);
        audio.jouer(arme.famille() == Famille.MITRAILLETTE ? "tir_mitraillette" : "tir_pistolet", 0.65f, sens * 0.3f,
                0.88f + hasard.nextFloat() * 0.08f);
        effets.douille(e.acteur.x + sens * 6, e.acteur.y - 30, sens);
    }

    private void preparer(Ennemi e, float vitesse) {
        e.etat = Phase.PREPARE;
        e.minuteur = e.genre.preparation * vitesse / niveau;
        e.coupsRestants = e.genre.enchainement - 1;
        e.charge = false;
        audio.jouer(Audio.Bus.VOIX, e.genre.danse ? "v_ecouteurs_inspire" : "v_gars_geto_inspire", 0.35f, 0,
                e.genre.danse ? 1.5f : e.genre.style == Style.BRUTE ? 0.75f : 1f, 0);
    }

    /** Le dessin de l'ennemi selon ce qu'il fait : arme (penche en arriere), frappe (en avant), au sol, accroupi... */
    private void habiller(Ennemi e) {
        Acteur a = e.acteur;
        Texture base = e.dessin;
        if (base == null) {
            return;
        }
        boolean vacille = ((int) (e.phase * 9)) % 2 == 0;
        Texture t = switch (e.etat) {
            case PREPARE -> e.charge ? Poses.tasse(Poses.penche(base, 3), 0.9f)
                    : e.genre.bond && e.minuteur > e.genre.preparation ? Poses.tasse(base, 0.82f) : Poses.penche(base, -3);
            case FRAPPE, BOND -> Poses.penche(base, 4);
            case CHARGE -> Poses.penche(base, 5);
            case RECULE -> Poses.penche(base, -4);
            case VACILLE -> Poses.tasse(Poses.penche(base, vacille ? -3 : 2), 0.96f);
            case GARDE -> Poses.tasse(base, 0.93f);
            case ESQUIVE -> Poses.penche(base, -2);
            case VOLE -> e.vz > 0 ? Poses.penche(base, -5) : Poses.couche(base, true);
            case SOL -> Poses.couche(base, true);
            case RELEVE -> Poses.tasse(base, 0.7f);
            case VISE, TIRE -> Poses.penche(base, e.flash > 0 ? -2 : -1);
            case RECHARGE -> Poses.tasse(Poses.penche(base, 2), 0.95f);
            case TOMBE -> e.genre.danse ? Poses.tasse(base, 0.6f) : e.z > 0 ? Poses.penche(base, -5) : Poses.couche(base, true);
            default -> null;
        };
        if (a.texture == null && e.etat == Phase.TOMBE) {
            return;                                                     // l'alien s'est deja defait
        }
        a.texture = t == null ? base : t;
        a.pas = t == null ? e.dessinPas : t;
    }

    /** Les ennemis encore visibles, pour le dessin du monde. */
    List<Acteur> acteurs() {
        List<Acteur> l = new ArrayList<>();
        for (Ennemi e : ennemis) {
            if (e.acteur.texture != null && rue == null) {
                l.add(e.acteur);
            }
        }
        return l;
    }

    // ------------------------------------------------------------------ le dessin par-dessus le monde

    /**
     * Ce qui se dessine par-dessus le monde : l'arme dans la main du heros (et l'eclair du canon, la trainee d'une lame),
     * les traces des balles, le coup que prepare un ennemi (un signe au-dessus de lui, blanc quand on peut le contrer),
     * le trait rouge d'un tireur qui vise, l'arme des ennemis, leur garde, leur barre de vie, le blanc d'un ennemi touche.
     */
    void dessiner(Rendu rendu, Monde monde, histoire.Etat etat) {
        float camX = monde.camX();
        long t = monde.tempsMs();
        for (Ennemi e : ennemis) {
            if (e.acteur.texture == null) {
                continue;
            }
            int x = Math.round(e.acteur.x - camX);
            int haut = Math.round(e.acteur.y) - e.acteur.texture.hauteur() - Math.round(e.z);
            if (e.eclat > 0) {                                          // touche : il blanchit
                rendu.rect(x - 9, haut + 4, 18, e.acteur.texture.hauteur() - 6, 0xFFFFFF, 140);
            }
            dessinerArmeEnnemi(rendu, e, camX, monde);
            if (e.etat == Phase.PREPARE || e.etat == Phase.BOND && e.minuteur > 0.1f) {   // il prepare son coup : on le voit venir
                int k = (int) (e.minuteur * 20) % 2;
                boolean contrable = fenetreDeContre(e);
                int c = contrable ? 0xF0F4FF : e.charge || e.colere ? 0xFF2020 : e.genre.danse ? 0xC8A0FF : 0xFF5040;
                rendu.rect(x - 2, haut - 9 - k, 4, 6, c, 256);
                rendu.rect(x - 2, haut - 2 - k, 4, 2, c, 256);
                if (contrable) {
                    rendu.halo(x, haut - 6, 7, 7, 0xC0D8FF, 0.6f);
                }
            }
            if (e.etat == Phase.GARDE) {                                 // sa garde : un trait clair devant lui
                int devant = e.acteur.miroir ? -8 : 6;
                rendu.rect(x + devant, haut + 6, 2, 12, 0xA0C8FF, 200);
            }
            if (e.etat == Phase.VACILLE) {                               // il voit des etoiles
                for (int i = 0; i < 3; i++) {
                    double ang = t / 160.0 + i * 2.1;
                    rendu.rect(x + (int) Math.round(Math.cos(ang) * 7) - 1, haut - 4 + (int) Math.round(Math.sin(ang) * 2), 2, 2, 0xFFE070, 230);
                }
            }
            if (e == cible && actif() && e.vivant() && !e.auSol()) {     // la cible : un petit chevron au-dessus
                int cy = haut - 13 + (int) ((t / 200) % 2);
                rendu.rect(x - 3, cy, 7, 1, 0xF0D080, 230);
                rendu.rect(x - 2, cy + 1, 5, 1, 0xF0D080, 230);
                rendu.rect(x - 1, cy + 2, 3, 1, 0xF0D080, 230);
            }
            if (e.vivant() && e.pv < e.genre.pv) {
                rendu.rect(x - 9, haut - 4, 18, 3, 0x101018, 220);
                rendu.rect(x - 8, haut - 3, Math.max(1, Math.round(16 * e.pv / e.genre.pv)), 1, 0xE04A3A, 256);
            }
        }
        for (Projectile p : projectiles) {                              // l'objet en l'air, et son ombre au sol
            int px = Math.round(p.x - camX);
            rendu.rect(px - 3, Math.round(p.y) - 1, 6, 2, 0x000000, 90);
            Texture tx = res.image("obj_" + p.objet);
            if (tx != null) {
                rendu.calque(tx, px - tx.largeur() / 2, Math.round(p.y - p.z) - tx.hauteur() / 2);
            }
        }
        for (Trace tr : traces) {                                       // les balles : un trait chaud, bref
            float h = tr.h();
            rendu.ligne(tr.x0() - camX, tr.y0() - h, tr.x1() - camX, tr.y1() - h, 1, 0xFFE6A0);
        }
        if (chute > 0 || releve > 0 || monde.projete()) {
            return;
        }
        Arme arme = etat.armeEnMain();
        if (arme == null || monde.enScooter()) {
            return;
        }
        if (arme.aFeu()) {
            dessinerArmeAFeu(rendu, monde, arme);
        } else {
            dessinerArmeDeContact(rendu, monde, arme, t);
        }
    }

    /** L'arme a feu : levee (pose de tir), baissee pendant le rechargement, a la hanche sinon. L'eclair au bout du canon. */
    private void dessinerArmeAFeu(Rendu rendu, Monde monde, Arme arme) {
        Texture dessin = res.image(arme.dessin());
        if (dessin == null) {
            return;
        }
        boolean droite = monde.joueur.versDroite;
        float sens = droite ? 1 : -1;
        float hx = monde.joueur.x - monde.camX();
        float pied = monde.joueur.y - monde.hauteurDuSaut();
        boolean leve = geste == null && touche <= 0 && esquive <= 0 && elan <= 0 && !garde && recharge <= 0
                && (tirPose > 0 || degaine > 0 || actif() && cible != null && !auCorps);
        int x;
        int y;
        if (leve && arme.deuxMains()) {
            x = Math.round(droite ? hx - 1 : hx + 1 - dessin.largeur());
            y = Math.round(pied - 48 + 20);
        } else if (leve) {
            x = Math.round(droite ? hx + 10 : hx - 10 - dessin.largeur());
            y = Math.round(pied - 48 + 18);
        } else if (recharge > 0) {                                      // baissee devant la ceinture, le chargeur qui change
            x = Math.round(droite ? hx + 5 : hx - 5 - dessin.largeur());
            y = Math.round(pied - 48 + 27);
            float part = 1 - recharge / rechargeTotal;
            int mx = Math.round(hx + sens * 9);
            int my = y + dessin.hauteur() + (part < 0.5f ? Math.round(part * 14) : Math.round((1 - part) * 14));
            rendu.rect(mx - 1, my, 2, 3, 0x2A2A30, 256);
        } else if (geste != null || garde || esquive > 0 || elan > 0) {
            return;                                                     // les mains sont prises : l'arme est a la ceinture
        } else {
            x = Math.round(droite ? hx + 1 : hx - 1 - dessin.largeur());
            y = Math.round(pied - 48 + 29);
        }
        rendu.calque(droite ? dessin : Poses.miroir(dessin), x, y);
        if (flash > 0 && leve) {                                        // l'eclair du canon
            float bx = droite ? x + dessin.largeur() + 1 : x - 2;
            float by = y + 1;
            rendu.halo(bx + sens * 3, by, 12, 8, 0xFFD890, 0.9f);
            rendu.rect(Math.round(bx), Math.round(by) - 1, 3, 3, 0xFFFFF0, 256);
            rendu.rect(Math.round(bx + sens * 3), Math.round(by), 3, 1, 0xFFE0A0, 256);
        }
    }

    /**
     * Une arme de contact : un trait dans la main, leve derriere l'epaule puis abattu devant pendant le coup. Le katana
     * a sa garde et sa poignee, et une trainee claire pendant la coupe ; la relique change de couleur en chantant.
     */
    private void dessinerArmeDeContact(Rendu rendu, Monde monde, Arme arme, long t) {
        float sens = monde.joueur.versDroite ? 1 : -1;
        float z = monde.hauteurDuSaut();
        boolean frappe = geste != null && touche <= 0 && !geste.nom().startsWith("pied") && geste != CIRCULAIRE && geste != BALAYAGE
                && !geste.aerien();
        float mx = monde.joueur.x - monde.camX() + sens * (frappe ? 10 : 4);
        float my = monde.joueur.y - z - (frappe ? 33 : 17);
        double depart = -Math.PI * 0.75;
        double course = Math.PI * 0.95;
        if (frappe && geste.nom().endsWith("_revers")) {                // le revers : de bas en haut
            depart = Math.PI * 0.25;
            course = -Math.PI * 0.85;
        } else if (frappe && geste.nom().endsWith("_monte")) {          // la coupe montante
            depart = Math.PI * 0.45;
            course = -Math.PI * 1.05;
        }
        double angle;
        float avance = 0;
        if (frappe) {
            avance = Math.min(1f, tempsGeste / (geste.duree() * lenteur));
            angle = depart + avance * course;                           // de leve derriere l'epaule a abattu devant
        } else {
            angle = arme.famille() == Famille.LAME ? Math.PI * 0.3 : Math.PI * 0.42;   // au repos : le long de la jambe
        }
        int couleur = arme.couleur();
        if (arme.relique()) {
            float h = (t % 2400) / 2400f;
            couleur = java.awt.Color.HSBtoRGB(h, 0.35f, 1f) & 0xFFFFFF;
        }
        boolean lame = arme.famille() == Famille.LAME;
        if (frappe && lame && avance > 0.15f && avance < 0.85f) {       // la trainee de la coupe
            for (int k = 1; k <= 4; k++) {
                double a = depart + Math.max(0, avance - k * 0.07f) * course;
                for (int i = 6; i <= arme.longueur(); i += 2) {
                    rendu.rect(Math.round(mx + (float) Math.cos(a) * i * sens), Math.round(my + (float) Math.sin(a) * i), 1, 1,
                            arme.relique() ? couleur : 0xE8F0FF, 150 - k * 30);
                }
            }
        }
        float bx = mx + (float) Math.cos(angle) * arme.longueur() * sens;
        float by = my + (float) Math.sin(angle) * arme.longueur();
        if (lame) {
            float gx = mx + (float) Math.cos(angle) * 5 * sens;
            float gy = my + (float) Math.sin(angle) * 5;
            rendu.ligne(mx, my, gx, gy, 2, 0x26262C);                   // la poignee
            rendu.rect(Math.round(gx) - 1, Math.round(gy) - 1, 3, 3, arme.relique() ? 0xC8A040 : 0x3A3A42, 256);   // la tsuba
            rendu.ligne(gx, gy, bx, by, 1, couleur);
            if (arme.relique()) {
                rendu.halo(bx, by, 5, 5, couleur, 0.5f);
            }
        } else {
            rendu.ligne(mx, my, bx, by, arme.id().equals("bouteille") || arme.id().equals("batte") ? 3 : 2, couleur);
            if (arme.id().equals("batte")) {
                rendu.ligne(mx, my, mx + (float) Math.cos(angle) * 4 * sens, my + (float) Math.sin(angle) * 4, 2, 0x2A2A30);
            }
        }
    }

    /** L'arme d'un ennemi : le pistolet ou la mitraillette du tireur (et son trait rouge quand il vise), le sabre du sabreur. */
    private void dessinerArmeEnnemi(Rendu rendu, Ennemi e, float camX, Monde monde) {
        Arme arme = e.arme();
        if (arme == null || !e.vivant() || e.auSol() || e.etat == Phase.VOLE) {
            return;
        }
        boolean droite = !e.acteur.miroir;
        float sens = droite ? 1 : -1;
        float x = e.acteur.x - camX;
        float pied = e.acteur.y - e.z;
        if (arme.aFeu()) {
            Texture dessin = res.image(arme.dessin());
            if (dessin == null) {
                return;
            }
            boolean leve = e.etat == Phase.VISE || e.etat == Phase.TIRE;
            int gx = Math.round(droite ? x + (leve ? 6 : 1) : x - (leve ? 6 : 1) - dessin.largeur());
            int gy = Math.round(pied - 48 + (leve ? 19 : 29));
            if (leve) {                                                 // le bras tendu vers le heros
                rendu.ligne(x + sens * 2, pied - 28, x + sens * 7, pied - 28, 2, 0x4A4E44);
            }
            rendu.calque(droite ? dessin : Poses.miroir(dessin), gx, gy);
            float bx = droite ? gx + dessin.largeur() : gx - 1;
            if (e.etat == Phase.VISE) {                                 // le trait rouge du viseur : il s'intensifie
                float part = 1 - e.minuteur / 0.6f;
                float ex = e.viseX - camX;
                float ey = e.viseY - 28;
                float d = (float) Math.hypot(ex - bx, ey - (gy + 1));
                int n = (int) (d / 3);
                for (int i = 0; i < n; i++) {
                    if ((i + monde.tempsMs() / 60) % 3 == 0) {
                        continue;
                    }
                    float px = bx + (ex - bx) * i / Math.max(1, n);
                    float py = gy + 1 + (ey - gy - 1) * i / Math.max(1, n);
                    rendu.rect(Math.round(px), Math.round(py), 1, 1, 0xFF3020, Math.round(60 + 160 * part));
                }
            }
            if (e.flash > 0) {
                rendu.halo(bx + sens * 3, gy + 1, 10, 7, 0xFFD890, 0.85f);
                rendu.rect(Math.round(bx), gy, 3, 3, 0xFFFFF0, 256);
            }
        } else {                                                        // le sabre : leve en preparant, abattu en frappant
            double angle = switch (e.etat) {
                case PREPARE -> -Math.PI * 0.7;
                case FRAPPE -> Math.PI * 0.15;
                case GARDE -> -Math.PI * 0.45;
                default -> Math.PI * 0.3;
            };
            float mx = x + sens * (e.etat == Phase.FRAPPE ? 9 : 4);
            float my = pied - (e.etat == Phase.PREPARE || e.etat == Phase.FRAPPE ? 32 : 18);
            float bx = mx + (float) Math.cos(angle) * arme.longueur() * sens;
            float by = my + (float) Math.sin(angle) * arme.longueur();
            rendu.ligne(mx, my, mx + (float) Math.cos(angle) * 4 * sens, my + (float) Math.sin(angle) * 4, 2, 0x26262C);
            rendu.ligne(mx + (float) Math.cos(angle) * 4 * sens, my + (float) Math.sin(angle) * 4, bx, by, 1, arme.couleur());
        }
    }
}
