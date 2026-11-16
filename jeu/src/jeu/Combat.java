package jeu;

import decor.Acteur;
import decor.Poses;
import decor.Rendu;
import decor.Texture;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Le combat au corps a corps.
 *
 * <p>Le heros : J enchaine trois coups (direct, second direct, crochet) si on rappuie pendant le geste ; K est le coup
 * de pied, lent, qui porte loin, repousse et casse une garde ; J avec une arme en main frappe avec l'arme. Chaque coup
 * coute du souffle : a bout, on frappe plus lentement. Touche, il recule ; frappe fort ou trois fois de suite, il tombe,
 * reste au sol un instant, puis se releve (intouchable le temps de se remettre).
 *
 * <p>Les ennemis : ils approchent, tournent autour en attendant leur tour (deux au plus attaquent a la fois), arment
 * leur coup (on le voit venir), frappent, et enchainent parfois un second coup. Touches, ils reculent et perdent le coup
 * qu'ils armaient ; frappes trois fois de suite, ou d'un coup lourd, ils tombent et se relevent en colere (ils attaquent
 * aussitot : on ne les enferme pas dans une rafale). Apres un premier coup recu, ils apprennent : ils levent leur garde
 * ou esquivent en arriere. Les aliens bondissent de loin. Un coup ne touche que pendant sa fenetre active, une fois par
 * ennemi, et l'image se fige un instant a l'impact.
 */
final class Combat {
    /** Un geste du heros : sa duree, sa fenetre active, sa portee, ses degats, le recul qu'il donne, son dessin. */
    record Geste(String nom, float duree, float debut, float fin, float portee, float degats, float recul, String pose, String son) {
    }

    static final Geste POING = new Geste("poing", 0.28f, 0.07f, 0.16f, 26, 9, 70, "heros_poing", "coup");
    static final Geste DIRECT = new Geste("direct", 0.26f, 0.06f, 0.15f, 27, 10, 80, "heros_poing", "coup");
    static final Geste CROCHET = new Geste("crochet", 0.42f, 0.13f, 0.25f, 28, 15, 150, "heros_crochet", "choc");
    static final Geste PIED = new Geste("pied", 0.52f, 0.18f, 0.32f, 34, 16, 170, "heros_pied", "choc");

    /**
     * Ce qu'est un ennemi : sa sante, sa vitesse, la force et le rythme de ses coups, son dessin ; sa garde et son esquive
     * (une fois alerte), le nombre de coups qu'il enchaine, et s'il bondit de loin.
     */
    record Genre(String id, float pv, float vitesse, float degats, float preparation, float repos, float portee, String image,
                 boolean danse, float garde, float esquive, int enchainement, boolean bond) {
    }

    static final Genre ALIEN = new Genre("alien", 34, 62, 8, 0.5f, 1.2f, 24, "p_alien", true, 0.15f, 0.35f, 2, true);
    static final Genre GUETTEUR = new Genre("guetteur", 46, 70, 11, 0.45f, 1.1f, 24, "p_gars_geto", false, 0.45f, 0.1f, 2, false);
    /** Un passant qui riposte : pas un combattant, mais il ne se laisse pas faire. */
    static final Genre PASSANT = new Genre("passant", 26, 64, 7, 0.55f, 1.5f, 22, null, false, 0.25f, 0.15f, 1, false);
    static final Genre POLICIER = new Genre("police", 40, 76, 10, 0.42f, 1.2f, 24, null, false, 0.5f, 0.05f, 2, false);

    enum Phase { APPROCHE, PREPARE, FRAPPE, RECULE, GARDE, ESQUIVE, BOND, SOL, RELEVE, TOMBE }

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
        /** Interrompu une fois, il arme le coup suivant sans se laisser couper par un petit coup : pas de rafale sans fin. */
        boolean armure;

        Ennemi(Genre genre, Acteur acteur, float phase) {
            this.genre = genre;
            this.acteur = acteur;
            this.dessin = acteur.texture;
            this.dessinPas = acteur.pas;
            this.pv = genre.pv;
            this.phase = phase;
            this.repos = 0.6f + phase * 0.5f;
        }

        boolean vivant() {
            return etat != Phase.TOMBE;
        }

        /** A terre ou en train de se relever : on ne le frappe pas. */
        boolean auSol() {
            return etat == Phase.SOL || etat == Phase.RELEVE;
        }
    }

    private final Ressources res;
    private final Audio audio;
    private final Effets effets;
    private final Random hasard = new Random(17);
    private final List<Ennemi> ennemis = new ArrayList<>();
    private Geste geste;
    private int chaine;
    private boolean suiteDemandee;
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

    Combat(Ressources res, Audio audio, Effets effets) {
        this.res = res;
        this.audio = audio;
        this.effets = effets;
    }

    /** Un combat commence : nombre ennemis de ce genre, autour de x ; les scenes a jouer selon qu'on gagne ou qu'on perd. */
    void commencer(Genre genre, int nombre, float x, float y, String sceneVictoire, String sceneDefaite, decor.Scene scene) {
        ennemis.clear();
        for (int i = 0; i < nombre; i++) {
            float cote = i % 2 == 0 ? 1 : -1;
            float[] place = placeLibre(scene, x + cote * (90 + 26 * (i / 2)), y + (i % 3 - 1) * 10, cote);
            Acteur a = new Acteur(genre.id + i, res.image(genre.image), place[0], place[1]);
            a.pas = res.image(genre.image + "_2");
            a.miroir = cote > 0;
            ennemis.add(new Ennemi(genre, a, i * 0.37f));
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
        e.repos = 0.8f;
        ennemis.add(e);
    }

    private void remettre() {
        fini = false;
        gagne = false;
        geste = null;
        chaine = 0;
        touche = 0;
        invincible = 0;
        chute = 0;
        releve = 0;
        gel = 0;
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

    /** L'habitant avec qui on se bat dans la rue, ou null pour un combat de l'histoire. */
    String rue() {
        return rue;
    }

    void oublier() {
        for (Ennemi e : ennemis) {                                     // un habitant reprend son dessin
            e.acteur.texture = e.etat == Phase.TOMBE && rue == null ? null : e.dessin;
            e.acteur.pas = e.dessinPas;
        }
        ennemis.clear();
        fini = false;
        rue = null;
        chute = 0;
        releve = 0;
        touche = 0;
        geste = null;
    }

    List<Ennemi> ennemis() {
        return ennemis;
    }

    /** Le geste en cours du heros (pour les essais), ou null. */
    Geste geste() {
        return geste;
    }

    /** Vrai pendant un coup, un choc, une chute : le heros ne marche pas en meme temps. */
    boolean occupe() {
        return geste != null || touche > 0 || chute > 0 || releve > 0;
    }

    /** Vrai si le heros est a terre (il ne peut rien faire, on ne peut pas le frapper). */
    boolean heroAuSol() {
        return chute > 0 || releve > 0;
    }

    /** Le dessin du heros en ce moment, ou null pour le laisser marcher normalement. */
    Texture pose(histoire.Etat etatHeros, Monde monde) {
        if (chute > 0) {
            return Poses.couche(res.image("heros_touche"), true);
        }
        if (releve > 0) {
            return Poses.tasse(res.image("heros_garde"), releve > 0.25f ? 0.62f : 0.82f);
        }
        if (touche > 0) {
            return Poses.penche(res.image("heros_touche"), touche > 0.18f ? -3 : -1);
        }
        if (geste != null) {
            float t = tempsGeste / (geste.duree() * lenteur);
            if (geste == CROCHET) {                                     // le crochet : l'epaule part, puis tout le corps suit
                return Poses.penche(res.image("heros_poing"), t < 0.3f ? -2 : 4);
            }
            if (geste == DIRECT) {
                return Poses.penche(res.image("heros_poing"), 2);
            }
            if (geste == PIED || geste == POING) {
                return res.image(geste.pose());
            }
            return res.image(geste.pose());
        }
        if (actif() && etatHeros.essouffle()) {                         // a bout de souffle : plie en deux
            return Poses.penche(Poses.tasse(res.image("heros_garde"), 0.92f), 2);
        }
        return actif() ? res.image("heros_garde") : null;
    }

    /**
     * Une image de combat. Le heros frappe s'il appuie sur J ou K (meme hors combat : on peut boxer dans le vide),
     * les ennemis vivent, les coups portent. Renvoie le message a afficher (une arme qui casse), ou null.
     */
    String maj(float dt, Touches touches, Monde monde, histoire.Etat etat) {
        if (gel > 0) {                                                  // l'impact : un instant, tout s'arrete
            gel -= dt;
            if (touches.poing() && geste != null) {
                suiteDemandee = true;
            }
            return null;
        }
        String message = null;
        touche = Math.max(0, touche - dt);
        invincible = Math.max(0, invincible - dt);
        encaisseAge += dt;
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
        if (!heroAuSol()) {
            message = gestes(dt, touches, monde, etat);
        }
        for (Ennemi e : ennemis) {
            vivre(e, dt, monde, etat);
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
        return message;
    }

    /** Les coups du heros : partir, enchainer, toucher pendant la fenetre active. */
    private String gestes(float dt, Touches touches, Monde monde, histoire.Etat etat) {
        String message = null;
        if (geste != null && touches.poing() && tempsGeste > geste.debut()) {
            suiteDemandee = true;                                       // rappuyer pendant le geste : le suivant s'enchaine
        }
        if (geste == null && touche <= 0) {
            if (touches.poing()) {
                partir(etat.armeEnMain() == null ? POING : null, etat);
                chaine = 1;
            } else if (touches.pied()) {
                partir(PIED, etat);
                chaine = 0;
            }
        }
        if (geste != null) {
            tempsGeste += dt;
            float d = lenteur;
            if (tempsGeste >= geste.debut() * d && tempsGeste <= geste.fin() * d) {
                message = porter(monde, etat);
            }
            if (tempsGeste >= geste.duree() * d) {
                boolean suite = suiteDemandee && etat.armeEnMain() == null && chaine >= 1 && chaine < 3;
                geste = null;
                suiteDemandee = false;
                if (suite) {
                    chaine++;
                    partir(chaine == 2 ? DIRECT : CROCHET, etat);
                } else {
                    chaine = 0;
                }
            }
        }
        return message;
    }

    private void partir(Geste voulu, histoire.Etat etat) {
        histoire.Etat.Arme arme = etat.armeEnMain();
        geste = voulu != null ? voulu : new Geste(arme.id(), arme.duree(), arme.duree() * 0.35f, arme.duree() * 0.6f,
                arme.portee(), arme.degats(), arme.recul(), "heros_frappe", "coup");
        tempsGeste = 0;
        suiteDemandee = false;
        lenteur = etat.essouffle() ? 1.35f : 1f;                         // a bout de souffle, tout est plus lent
        etat.effort(geste == PIED ? 11 : geste == CROCHET ? 8 : arme != null && voulu == null ? 9 : 5);
        ennemis.forEach(e -> e.touchePendantCeGeste = false);
        boolean lourd = geste == PIED || geste == CROCHET;
        audio.jouer(Audio.Bus.EFFETS, "vetement", lourd ? 0.45f : 0.35f, 0, lourd ? 0.9f : 1.4f, 0);   // le geste fend l'air
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

    /** Le coup du heros est dans sa fenetre active : il touche les ennemis devant lui, a portee, une fois chacun. */
    private String porter(Monde monde, histoire.Etat etat) {
        float sens = monde.joueur.versDroite ? 1 : -1;
        String message = null;
        boolean aTouche = false;
        for (Ennemi e : ennemis) {
            float dx = (e.acteur.x - monde.joueur.x) * sens;
            if (!e.vivant() || e.auSol() || e.touchePendantCeGeste || dx < -4 || dx > geste.portee()
                    || Math.abs(e.acteur.y - monde.joueur.y) > 16) {
                continue;
            }
            e.touchePendantCeGeste = true;
            if (e.etat == Phase.ESQUIVE) {                              // il a recule a temps : dans le vide
                continue;
            }
            aTouche = true;
            boolean lourd = geste == PIED || geste == CROCHET || geste.degats() > 18;
            boolean deFace = (e.acteur.x - monde.joueur.x) * (e.acteur.miroir ? 1 : -1) >= 0;
            if (e.etat == Phase.GARDE && deFace && !(geste == PIED && e.coupsBloques >= 1)) {
                e.coupsBloques++;                                       // la garde tient : peu de mal, il recule a peine
                e.pv -= geste.degats() * 0.2f;
                e.vx = sens * 40;
                e.minuteur = Math.max(e.minuteur, 0.25f);
                effets.impact(e.acteur.x - sens * 6, e.acteur.y - 24, 0xA0C8FF, 4);
                audio.jouer("ferraille", 0.35f, sens * 0.2f, 1.6f);
                touche = Math.max(touche, 0.12f);                       // le coup rebondit sur sa garde
                gel = 0.04f;
                if (e.coupsBloques >= 3 || geste == PIED) {             // la garde cede
                    e.etat = Phase.RECULE;
                    e.minuteur = 0.45f;
                }
                continue;
            }
            e.coupsRecus++;
            e.serie = e.serieAge < 1.4f ? e.serie + 1 : 1;
            e.serieAge = 0;
            e.pv -= geste.degats() * (e.etat == Phase.FRAPPE ? 1.2f : 1f);   // le contre fait plus mal
            e.eclat = 0.16f;
            e.vx = sens * geste.recul();
            e.colere = false;
            if (e.pv <= 0) {
                e.etat = Phase.TOMBE;
                e.minuteur = 0.9f;
            } else if ((e.etat == Phase.FRAPPE || e.etat == Phase.PREPARE && e.armure) && !lourd) {
                e.minuteur = Math.max(e.minuteur, 0.05f);               // en plein elan : un petit coup ne l'arrete pas
            } else if (e.serie >= 3 || lourd && e.serie >= 2 || geste == CROCHET) {
                e.etat = Phase.SOL;                                     // il tombe
                e.minuteur = 0.85f;
                e.serie = 0;
                audio.jouer("corps_chute", 0.6f, sens * 0.2f, 1.1f);
            } else {
                e.armure |= e.etat == Phase.PREPARE;                    // coupe une fois : la prochaine fois, il tiendra
                e.etat = Phase.RECULE;
                e.minuteur = 0.32f + geste.recul() / 900f;              // touche, il perd le coup qu'il preparait
            }
            gel = lourd ? 0.08f : 0.05f;
            effets.impact(e.acteur.x - sens * 4, e.acteur.y - 26, e.genre.danse ? 0xC8A0FF : 0xFFF0C0, 6 + (int) geste.degats() / 3);
            monde.trembler(lourd ? 3.5f : 1.8f);
            audio.jouer(geste.son(), 0.8f, sens * 0.2f, 0.9f + hasard.nextFloat() * 0.2f);
            if (e.pv <= 0) {
                audio.jouer(e.genre.danse ? "tele_rafale" : "corps_chute", e.genre.danse ? 0.35f : 0.7f, 0, 1.2f);
            } else {
                audio.jouer(Audio.Bus.VOIX, e.genre.danse ? "v_ecouteurs_oh" : "v_gars_geto_grogne", 0.5f, sens * 0.3f,
                        e.genre.danse ? 1.6f : 1f, 0.03f);
            }
        }
        if (aTouche && !geste.nom().equals("poing") && !geste.nom().equals("pied") && !geste.nom().equals("direct")
                && !geste.nom().equals("crochet") && etat.userArme()) {
            audio.jouer(geste.nom().equals("bouteille") ? "tinte" : "ferraille", 0.7f, 0, 1.1f);
            message = geste.nom().equals("bouteille") ? "LA BOUTEILLE A ÉCLATÉ." : "TON ARME A CÉDÉ.";
        }
        return message;
    }

    /** Le heros encaisse un coup : il recule ; trop fort ou trop souvent, il tombe. */
    private void encaisser(Ennemi e, float dx, Monde monde, histoire.Etat etat) {
        float degats = e.genre.degats * (e.colere ? 1.2f : 1f);
        etat.coup(degats);
        effets.blessure(degats / 25f);
        effets.impact(monde.joueur.x, monde.joueur.y - 26, 0xFF6050, 7);
        audio.jouer("coup", 0.8f, 0, 0.8f);
        coupsEncaisses = encaisseAge < 2.5f ? coupsEncaisses + 1 : 1;
        encaisseAge = 0;
        geste = null;                                                   // un coup recu coupe le sien
        chaine = 0;
        gel = 0.06f;
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

    /** Un ennemi : il approche, tourne, arme son coup, frappe et enchaine, encaisse, se garde, esquive, tombe, se releve. */
    private void vivre(Ennemi e, float dt, Monde monde, histoire.Etat etat) {
        Acteur a = e.acteur;
        e.eclat = Math.max(0, e.eclat - dt);
        e.minuteur = Math.max(0, e.minuteur - dt);
        e.repos = Math.max(0, e.repos - dt);
        e.serieAge += dt;
        e.phase += dt;
        if (e.vx != 0) {                                                // le recul s'amortit
            float nx = a.x + e.vx * dt;
            if (!monde.scene().bloque(nx, a.y, 6)) {
                a.x = nx;
            }
            e.vx *= Math.max(0, 1 - dt * 7);
            if (Math.abs(e.vx) < 4) {
                e.vx = 0;
            }
        }
        habiller(e);
        if (!e.vivant()) {
            a.marche = false;
            if (e.minuteur <= 0 && a.texture != null) {
                if (e.genre.danse) {
                    effets.impact(a.x, a.y - 10, 0xA0FFE0, 14);              // il se defait en eclats iridescents
                    a.texture = null;
                }
            }
            return;
        }
        float dx = monde.joueur.x - a.x;
        float dy = monde.joueur.y - a.y;
        if (e.etat != Phase.BOND && e.etat != Phase.ESQUIVE) {
            a.miroir = dx < 0;
        }
        long attaquants = ennemis.stream().filter(o -> o.etat == Phase.PREPARE || o.etat == Phase.FRAPPE || o.etat == Phase.BOND).count();
        switch (e.etat) {
            case RECULE -> {
                if (e.minuteur <= 0) {
                    e.etat = Phase.APPROCHE;
                    e.repos = Math.max(e.repos, 0.3f);
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
                    e.repos = 0;
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
                if (Math.abs(monde.joueur.x - a.x) < 14 && Math.abs(dy) < 14 && invincible <= 0 && !heroAuSol()) {
                    encaisser(e, dx, monde, etat);
                    e.minuteur = 0;
                }
                if (e.minuteur <= 0) {
                    e.etat = Phase.FRAPPE;
                    e.minuteur = 0.25f;
                    e.coupsRestants = 0;
                }
            }
            case PREPARE -> {
                a.marche = false;
                if (e.minuteur <= 0) {
                    if (e.genre.bond && Math.abs(dx) > e.genre.portee + 10) {
                        e.etat = Phase.BOND;                            // arme de loin : il bondit
                        e.minuteur = 0.28f;
                        audio.jouer(Audio.Bus.EFFETS, "vetement", 0.4f, 0, 1.3f, 0);
                        break;
                    }
                    e.etat = Phase.FRAPPE;
                    e.minuteur = 0.18f;
                    if (Math.abs(dx) <= e.genre.portee + 6 && Math.abs(dy) < 18 && invincible <= 0 && !heroAuSol()) {
                        encaisser(e, dx, monde, etat);
                    } else {
                        audio.jouer(Audio.Bus.EFFETS, "vetement", 0.3f, 0, 0.8f, 0);    // il frappe dans le vide
                    }
                }
            }
            case FRAPPE -> {
                if (e.minuteur <= 0) {
                    boolean aPortee = Math.abs(dx) <= e.genre.portee + 8 && Math.abs(dy) < 16;
                    if (e.coupsRestants > 0 && aPortee && !heroAuSol()) {   // il enchaine
                        e.coupsRestants--;
                        e.etat = Phase.PREPARE;
                        e.minuteur = e.genre.preparation * 0.45f;
                    } else {
                        e.etat = Phase.APPROCHE;
                        e.repos = e.genre.repos * (0.8f + hasard.nextFloat() * 0.5f) * (e.colere ? 0.5f : 1f);
                        e.colere = false;
                        e.armure = false;
                    }
                }
            }
            default -> approcher(e, dt, monde, dx, dy, attaquants);
        }
    }

    private void approcher(Ennemi e, float dt, Monde monde, float dx, float dy, long attaquants) {
        Acteur a = e.acteur;
        float sensHeros = monde.joueur.versDroite ? 1 : -1;
        boolean face = (a.x - monde.joueur.x) * sensHeros > 0;
        // le heros arme un coup vers lui : un ennemi alerte se garde ou esquive (il a deja pris des coups)
        if (geste != null && face && Math.abs(dx) < geste.portee() + 14 && Math.abs(dy) < 18 && e.coupsRecus > 0
                && tempsGeste < geste.debut() * lenteur && !e.touchePendantCeGeste) {
            float tirage = hasard.nextFloat();
            e.touchePendantCeGeste = true;                              // une decision par geste
            if (tirage < e.genre.esquive) {
                e.etat = Phase.ESQUIVE;
                e.minuteur = 0.24f;
                return;
            }
            if (tirage < e.genre.esquive + e.genre.garde) {
                e.etat = Phase.GARDE;
                e.minuteur = 0.55f;
                return;
            }
        }
        boolean derriere = !face;                                       // dans le dos du heros : il n'attend pas
        boolean aPortee = Math.abs(dx) <= e.genre.portee && Math.abs(dy) < 14;
        boolean libre = attaquants < 2 || e.colere;
        if (aPortee && (e.repos <= 0 || derriere && e.repos < 0.4f) && libre && !heroAuSol()) {
            preparer(e, derriere || e.colere ? 0.7f : 1f);
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
        float vy = Math.signum(dy) * Math.min(Math.abs(dy), 30) / 30f;
        if (attend) {
            vy += (float) Math.sin(e.phase * 1.7f + e.genre.pv) * 0.6f;    // il tourne autour
        }
        if (e.genre.danse) {                                            // les aliens avancent en dansant
            vy += (float) Math.sin(e.phase * 5) * 0.9f;
        }
        float vitesse = e.genre.vitesse * (e.colere ? 1.25f : 1f);
        float nx = a.x + vers * vitesse * dt;
        float ny = a.y + vy * vitesse * 0.5f * dt;
        decor.Scene scene = monde.scene();
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
        a.marche = vers != 0 || e.genre.danse || attend && Math.abs(vy) > 0.2f;
    }

    private void preparer(Ennemi e, float vitesse) {
        e.etat = Phase.PREPARE;
        e.minuteur = e.genre.preparation * vitesse;
        e.coupsRestants = e.genre.enchainement - 1;
        audio.jouer(Audio.Bus.VOIX, e.genre.danse ? "v_ecouteurs_inspire" : "v_gars_geto_inspire", 0.35f, 0,
                e.genre.danse ? 1.5f : 1f, 0);
    }

    /** Le dessin de l'ennemi selon ce qu'il fait : arme (penche en arriere), frappe (en avant), au sol, accroupi... */
    private void habiller(Ennemi e) {
        Acteur a = e.acteur;
        Texture base = e.dessin;
        if (base == null) {
            return;
        }
        Texture t = switch (e.etat) {
            case PREPARE -> e.genre.bond && e.minuteur > e.genre.preparation ? Poses.tasse(base, 0.82f) : Poses.penche(base, -3);
            case FRAPPE, BOND -> Poses.penche(base, 4);
            case RECULE -> Poses.penche(base, -4);
            case GARDE -> Poses.tasse(base, 0.93f);
            case ESQUIVE -> Poses.penche(base, -2);
            case SOL -> Poses.couche(base, true);
            case RELEVE -> Poses.tasse(base, 0.7f);
            case TOMBE -> e.genre.danse ? Poses.tasse(base, 0.6f) : Poses.couche(base, true);
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

    /**
     * Ce qui se dessine par-dessus le monde : l'arme dans la main du heros, le coup que prepare un ennemi (un eclat au
     * dessus de lui), sa garde, sa barre de vie, le blanc d'un ennemi qui vient d'etre touche.
     */
    void dessiner(Rendu rendu, Monde monde, histoire.Etat etat) {
        float camX = monde.camX();
        for (Ennemi e : ennemis) {
            if (e.acteur.texture == null) {
                continue;
            }
            int x = Math.round(e.acteur.x - camX);
            int haut = Math.round(e.acteur.y) - e.acteur.texture.hauteur();
            if (e.eclat > 0) {                                          // touche : il blanchit
                rendu.rect(x - 9, haut + 4, 18, e.acteur.texture.hauteur() - 6, 0xFFFFFF, 140);
            }
            if (e.etat == Phase.PREPARE) {                               // il prepare son coup : on le voit venir
                int k = (int) (e.minuteur * 20) % 2;
                int c = e.colere ? 0xFF2020 : e.genre.danse ? 0xC8A0FF : 0xFF5040;
                rendu.rect(x - 2, haut - 9 - k, 4, 6, c, 256);
                rendu.rect(x - 2, haut - 2 - k, 4, 2, c, 256);
            }
            if (e.etat == Phase.GARDE) {                                 // sa garde : un trait clair devant lui
                int devant = e.acteur.miroir ? -8 : 6;
                rendu.rect(x + devant, haut + 6, 2, 12, 0xA0C8FF, 200);
            }
            if (e.vivant() && e.pv < e.genre.pv) {
                rendu.rect(x - 9, haut - 4, 18, 3, 0x101018, 220);
                rendu.rect(x - 8, haut - 3, Math.max(1, Math.round(16 * e.pv / e.genre.pv)), 1, 0xE04A3A, 256);
            }
        }
        if (chute > 0 || releve > 0) {
            return;
        }
        histoire.Etat.Arme arme = etat.armeEnMain();
        if (arme != null && !monde.enScooter()) {                       // l'arme dans la main : levee pendant le coup
            float sens = monde.joueur.versDroite ? 1 : -1;
            float mx = monde.joueur.x - camX + sens * (geste != null && touche <= 0 ? 10 : 4);
            float my = monde.joueur.y - (geste != null && touche <= 0 ? 33 : 17);
            double angle;
            if (geste != null && touche <= 0 && !geste.nom().equals("pied")) {
                float t = Math.min(1f, tempsGeste / (geste.duree() * lenteur));
                angle = -Math.PI * 0.75 + t * Math.PI * 0.95;           // de leve derriere l'epaule a abattu devant
            } else {
                angle = Math.PI * 0.42;                                 // au repos : le long de la jambe
            }
            float bx = mx + (float) Math.cos(angle) * arme.longueur() * sens;
            float by = my + (float) Math.sin(angle) * arme.longueur();
            rendu.ligne(mx, my, bx, by, arme.id().equals("bouteille") ? 3 : 2, arme.couleur());
        }
    }
}
