package jeu;

import decor.Rendu;
import decor.Texture;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * La route de Namek, au volant de la RS6 : une sequence de conduite vue du dessus, jouee dans la boucle du jeu (pas une
 * boucle a part). Trois troncons de plus en plus durs : l'autoroute de nuit (des camions a doubler), la departementale
 * (des virages, des nids-de-poule, des chevres), la piste de Namek a l'aube (etroite, des rochers, le vide au bord).
 *
 * <p>Fleches ou ZQSD : haut accelere, bas freine, gauche et droite tournent ; Maj : le turbo (il boit). La RS6 boit son
 * essence (des bidons sur le bord), s'abime a chaque choc et dans le gravier ; tomber de la piste, tomber en panne ou
 * casser la voiture fait repartir au debut du troncon. L'heure tourne jusqu'au lever du soleil : arriver en retard laisse
 * moins de temps pour la recolte qui suit (c'est la consequence, pas un echec).
 */
final class Conduite {
    /** Un troncon : sa longueur, la largeur de la route, ses virages (amplitude, frequence), et son monde. */
    record Troncon(String nom, float longueur, float largeur, float virage, float frequence, int sol, int bord, boolean vide,
                   boolean nuit) {
    }

    enum Genre { CAMION, NID, CHEVRE, ROCHER, BIDON }

    static final class Obstacle {
        final Genre genre;
        float s;
        float dx;
        final float l;
        final float h;
        float vs;
        float vx;
        boolean pris;
        /** Le dessin (Racing Pack de Kenney) : une voiture de la circulation, un plot, un fut, un pneu, un rocher. */
        String image;

        Obstacle(Genre genre, float s, float dx, float l, float h) {
            this.genre = genre;
            this.s = s;
            this.dx = dx;
            this.l = l;
            this.h = h;
        }
    }

    /** Ce qui reste a la fin : en avance ou en retard sur le soleil (secondes), l'etat de la voiture, les bidons pris. */
    record Resultat(float avance, float degats, int bidons, int sorties) {
    }

    private static final Troncon[] PARCOURS = {
        new Troncon("L'AUTOROUTE, DE NUIT", 9000, 150, 0, 0, 0x2C2C34, 0x14161C, false, true),
        new Troncon("LA DÉPARTEMENTALE", 9000, 112, 0, 0, 0x34302E, 0x1A1E16, false, true),
        new Troncon("LA PISTE DE NAMEK", 8000, 86, 0, 0, 0x8A5A3A, 0x3A1C14, true, false),
    };
    private static final float PAS = 4f;
    static final float VITESSE_MAX = 400f;
    static final float SOLEIL = 135f;                                  // secondes avant le lever du soleil (environ 1 min 30 de route, plus la marge)
    private static final int VOITURE_L = 14;
    private static final int VOITURE_H = 26;
    /** La boite de collision de la RS6 : la partie visible de son dessin, un pixel en retrait de chaque cote. */
    private float carL = VOITURE_L;
    private float carH = VOITURE_H;

    /** La partie visible d'un dessin (sans ses bords transparents) : {largeur, hauteur}. */
    static float[] visible(Texture t) {
        int x0 = t.largeur();
        int y0 = t.hauteur();
        int x1 = -1;
        int y1 = -1;
        for (int y = 0; y < t.hauteur(); y++) {
            for (int x = 0; x < t.largeur(); x++) {
                if ((t.pixels()[y * t.largeur() + x] >>> 24) > 100) {
                    x0 = Math.min(x0, x);
                    y0 = Math.min(y0, y);
                    x1 = Math.max(x1, x);
                    y1 = Math.max(y1, y);
                }
            }
        }
        return x1 < 0 ? new float[] {1, 1} : new float[] {x1 - x0 + 1, y1 - y0 + 1};
    }

    private final Ressources res;
    private final Audio audio;
    private final Affichage affichage;
    private final Random hasard = new Random(2024);
    private final float[] centre;
    private final float[] largeur;
    private final float longueur;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Map<Integer, Texture> voitures = new HashMap<>();
    /** Le decor du bord : des arbres, des lampadaires, de gros rochers ; position le long de la route, ecart, dessin. */
    private record Deco(float s, float dx, String image) {
    }

    private final List<Deco> decor = new ArrayList<>();
    /** Pour chaque troncon : la bande de route et le sol du bord, en carreaux de 32 (le Racing Pack de Kenney). */
    private final Texture[] bandes = new Texture[3];

    // l'etat de la course
    enum Etape { CONSIGNES, DECOMPTE, ROUTE, PERTE, ARRIVEE, PAUSE }

    private Etape etape = Etape.CONSIGNES;
    private float temps;
    private float chrono;
    float s;
    float x;
    float angle;
    float vitesse;
    /** La vitesse de cote (pixels par seconde), pour eviter les obstacles. */
    float glisse;
    static final float LATERALE_MAX = 120f;
    float essence = 60;
    float degats;
    private float invincible;
    private float secousse;
    private float gravier;
    private int troncon;
    private int bidons;
    private int sorties;
    private String perte;
    private Etape avantPause;
    private final List<float[]> etincelles = new ArrayList<>();
    private float camS;
    private Resultat resultat;

    Conduite(Ressources res, Audio audio, Affichage affichage) {
        this.res = res;
        this.audio = audio;
        this.affichage = affichage;
        float total = 0;
        for (Troncon t : PARCOURS) {
            total += t.longueur();
        }
        longueur = total;
        int n = (int) (total / PAS) + 2;
        centre = new float[n];
        largeur = new float[n];
        float cx = 0;
        float phase = 0;
        for (int i = 0; i < n; i++) {                                   // la route : on integre sa pente, sans a-coup entre troncons
            float sv = i * PAS;
            Troncon t = PARCOURS[indexDe(sv)];
            float debut = debutDe(indexDe(sv));
            float melange = Math.min(1f, (sv - debut) / 300f);
            Troncon avant = indexDe(sv) > 0 ? PARCOURS[indexDe(sv) - 1] : t;
            float freq = avant.frequence() + (t.frequence() - avant.frequence()) * melange;
            float amp = avant.virage() + (t.virage() - avant.virage()) * melange;
            phase += freq * PAS;
            cx += (float) Math.sin(phase) * amp * freq * PAS * 1.6f;
            centre[i] = cx;
            largeur[i] = t.largeur();                                   // la route change de largeur net, comme on la voit
        }
        Texture rs6 = res.image("cond_rs6");
        if (rs6 != null) {
            float[] vu = visible(rs6);
            carL = vu[0] - 2;
            carH = vu[1] - 2;
        }
        semer();
        decorer();
        for (int i = 0; i < PARCOURS.length; i++) {
            bandes[i] = bande(i);
        }
        x = centre[0];
    }

    private static int indexDe(float sv) {
        float d = 0;
        for (int i = 0; i < PARCOURS.length; i++) {
            d += PARCOURS[i].longueur();
            if (sv < d) {
                return i;
            }
        }
        return PARCOURS.length - 1;
    }

    static float debutDe(int i) {
        float d = 0;
        for (int k = 0; k < i; k++) {
            d += PARCOURS[k].longueur();
        }
        return d;
    }

    float centre(float sv) {
        int i = Math.max(0, Math.min(centre.length - 2, (int) (sv / PAS)));
        float k = sv / PAS - i;
        return centre[i] + (centre[i + 1] - centre[i]) * Math.max(0, Math.min(1, k));
    }

    float largeur(float sv) {
        int i = Math.max(0, Math.min(largeur.length - 1, (int) (sv / PAS)));
        return largeur[i];
    }

    private static final String[] SOLS = {"cond_herbe_nuit", "cond_herbe_nuit", "cond_sable_aube"};
    private static final String[] ROUTES = {"cond_asphalte_nuit", "cond_asphalte_nuit", "cond_piste_aube"};

    /** La bande de route d'un troncon, de sa largeur exacte, faite du carreau de route repete : on la pose par rangees. */
    private Texture bande(int i) {
        Texture carreau = res.image(ROUTES[i]);
        int l = Math.round(PARCOURS[i].largeur());
        int[] p = new int[l * 32];
        for (int y = 0; y < 32; y++) {
            for (int x0 = 0; x0 < l; x0++) {
                p[y * l + x0] = carreau == null ? 0xFF303038 : carreau.pixels()[(y % carreau.hauteur()) * carreau.largeur() + x0 % carreau.largeur()];
            }
        }
        return new Texture(l, 32, p);
    }

    /** Le bord de la route : des arbres sur la departementale, des lampadaires sur l'autoroute, de gros rochers a Namek. */
    private void decorer() {
        for (float sv = 60; sv < longueur; sv += 70 + hasard.nextInt(90)) {
            int t = indexDe(sv);
            float demi = largeur(sv) / 2;
            for (int cote : new int[] {-1, 1}) {
                if (hasard.nextInt(3) == 0) {
                    continue;
                }
                float ecart = demi + 26 + hasard.nextInt(90);
                String image = switch (t) {
                    case 0 -> ((int) sv / 70) % 4 == 0 ? "lampadaire" : null;
                    case 1 -> hasard.nextBoolean() ? "cond_arbre_nuit" : "cond_arbre_grand_nuit";
                    default -> hasard.nextInt(3) == 0 ? "cond_rocher_bord" : null;
                };
                if (image != null) {
                    decor.add(new Deco(sv, cote * (image.equals("lampadaire") ? demi + 18 : ecart), image));
                }
            }
        }
    }

    /** Les obstacles et les bidons, places une fois : la meme route a chaque essai. */
    private void semer() {
        for (float sv = 500; sv < longueur - 200; sv += 120 + hasard.nextInt(140)) {
            int t = indexDe(sv);
            float demi = largeur(sv) / 2 - 12;
            float dx = (hasard.nextFloat() * 2 - 1) * demi;
            // l'autoroute : la circulation, et des flaques d'huile ; la departementale : des travaux (plots, futs), des
            // pneus qui traversent, de l'huile ; la piste : des rochers, et des pneus perdus qui roulent
            int tirage = hasard.nextInt(10);
            Genre g = switch (t) {
                case 0 -> tirage < 8 ? Genre.CAMION : Genre.NID;
                case 1 -> tirage < 5 ? Genre.ROCHER : tirage < 8 ? Genre.CHEVRE : Genre.NID;
                default -> tirage < 8 ? Genre.ROCHER : Genre.CHEVRE;
            };
            String image = switch (g) {
                case CAMION -> "cond_trafic_" + (1 + hasard.nextInt(5));
                case NID -> "cond_huile";
                case CHEVRE -> "cond_pneu";
                default -> t == 1 ? (hasard.nextBoolean() ? "cond_plot" : "cond_baril") : "cond_rocher_" + (1 + hasard.nextInt(3));
            };
            Texture tex = res.image(image);
            float[] vu = tex == null ? new float[] {14, 14} : visible(tex);
            // les formes rondes (rochers, pneus, plots, futs) ne remplissent pas leur carre : on reserre la boite
            float serre = g == Genre.CAMION ? 0.94f : g == Genre.NID ? 0.55f : 0.74f;
            float l = vu[0] * serre;
            float h = vu[1] * serre;
            Obstacle o = new Obstacle(g, sv, dx, l, h);
            o.image = image;
            if (g == Genre.CAMION) {
                o.vs = 150 + hasard.nextInt(60);                         // elle roule, moins vite que toi
                float voie = largeur(sv) / 4;
                o.dx = hasard.nextBoolean() ? -voie : voie;
            }
            if (g == Genre.CHEVRE) {
                o.vx = (hasard.nextBoolean() ? -1 : 1) * (20 + hasard.nextInt(16));
            }
            if (g == Genre.ROCHER && t == 1 && hasard.nextInt(3) == 0) {   // les travaux : deux plots cote a cote
                Obstacle deux = new Obstacle(g, sv, dx + (dx > 0 ? -14 : 14), l, h);
                deux.image = image;
                obstacles.add(deux);
            }
            obstacles.add(o);
        }
        for (float sv = 700; sv < longueur - 300; sv += 700 + hasard.nextInt(300)) {
            float demi = largeur(sv) / 2 - 10;
            obstacles.add(new Obstacle(Genre.BIDON, sv, (hasard.nextFloat() * 2 - 1) * demi, 8, 10));
        }
    }

    boolean fini() {
        return resultat != null;
    }

    Resultat resultat() {
        return resultat;
    }

    Etape etape() {
        return etape;
    }

    int troncon() {
        return troncon;
    }

    float longueur() {
        return longueur;
    }

    /** Les obstacles proches, pour qui veut conduire a la place du joueur (les essais). */
    List<Obstacle> obstacles() {
        return obstacles;
    }

    // ------------------------------------------------------------------ une image

    void maj(float dt, Touches touches) {
        temps += dt;
        secousse = Math.max(0, secousse - dt * 6);
        etincelles.removeIf(e -> (e[4] -= dt) <= 0);
        for (float[] e : etincelles) {
            e[0] += e[2] * dt;
            e[1] += e[3] * dt;
        }
        switch (etape) {
            case CONSIGNES -> {
                moteur(0);
                if (temps > 0.8f && touches.valider()) {
                    etape = Etape.DECOMPTE;
                    temps = 0;
                    audio.jouer("scooter_demarre", 0.7f, 0, 0.62f);
                }
            }
            case DECOMPTE -> {
                moteur(0.1f);
                int avant = (int) Math.ceil(3 - (temps - dt));
                int apres = (int) Math.ceil(3 - temps);
                if (apres != avant && apres > 0) {
                    audio.jouer("ui_deplace", 0.7f, 0, 1f);
                }
                if (temps >= 3) {
                    audio.jouer("ui_valide", 0.8f, 0, 1.3f);
                    etape = Etape.ROUTE;
                    temps = 0;
                }
            }
            case ROUTE -> {
                if (touches.retour()) {
                    avantPause = etape;
                    etape = Etape.PAUSE;
                    audio.jouer("ui_retour", 0.5f, 0, 1);
                    return;
                }
                rouler(dt, touches);
            }
            case PERTE -> {
                moteur(0);
                if (temps > 1.6f) {
                    repartir();
                }
            }
            case PAUSE -> {
                moteur(0);
                if (touches.retour()) {
                    etape = avantPause;
                } else if (touches.valider()) {                         // recommencer le troncon
                    repartir();
                }
            }
            case ARRIVEE -> {
                moteur(0);
                vitesse *= Math.max(0, 1 - dt * 3);
                s += vitesse * dt;
                if (temps > 1.2f && touches.valider()) {
                    resultat = new Resultat(SOLEIL - chrono, degats, bidons, sorties);
                    audio.ambiances(Map.of());
                }
            }
        }
        // la camera regarde un peu devant quand on va vite : la voiture descend un peu a l'ecran (mais reste dessinee la ou
        // elle est vraiment, pour que ce qu'on voit toucher soit ce qui touche)
        camS += (s + vitesse * 0.14f - camS) * Math.min(1, dt * 8);
    }

    private void moteur(float regime) {
        Map<String, float[]> couches = new HashMap<>();
        couches.put("scooter", new float[] {0.35f + regime * 0.25f, 0, 0.55f + regime * 0.75f});
        if (regime > 0.6f) {
            couches.put("amb_vent", new float[] {(regime - 0.6f) * 1.2f, 0, 1});
        }
        if (gravier > 0) {
            couches.put("amb_pneus_mouilles", new float[] {0.4f, 0, 0.7f});
        }
        audio.ambiances(couches);
    }

    private void rouler(float dt, Touches touches) {
        chrono += dt;
        invincible = Math.max(0, invincible - dt);
        Troncon t = PARCOURS[troncon];
        boolean gaz = touches.tenue(KeyEvent.VK_UP, KeyEvent.VK_Z, KeyEvent.VK_W);
        boolean frein = touches.tenue(KeyEvent.VK_DOWN, KeyEvent.VK_S);
        boolean turbo = touches.court() && gaz && essence > 5;
        float max = VITESSE_MAX * (turbo ? 1.18f : 1f) * (essence <= 0 ? 0.0f : 1f);
        if (gaz && essence > 0) {
            vitesse += (turbo ? 330 : 240) * dt;
        } else if (frein) {
            vitesse -= 520 * dt;
        } else {
            vitesse -= 70 * dt;                                         // le moteur retient
        }
        vitesse = Math.max(0, Math.min(max, vitesse));
        // la direction : gauche et droite font glisser la voiture de cote, progressivement (pas de volant qui pivote)
        float braquage = (touches.tenue(KeyEvent.VK_RIGHT, KeyEvent.VK_D) ? 1 : 0) - (touches.tenue(KeyEvent.VK_LEFT, KeyEvent.VK_Q,
                KeyEvent.VK_A) ? 1 : 0);
        float laterale = LATERALE_MAX * Math.min(1f, 0.35f + vitesse / 300f);
        if (braquage != 0) {
            glisse += braquage * 340 * dt;
        } else {
            glisse *= Math.max(0, 1 - dt * 7);                          // on lache : elle se remet droite
        }
        glisse = Math.max(-laterale, Math.min(laterale, glisse));
        angle = glisse / 420f;                                          // elle penche un peu dans le sens ou elle glisse
        essence = Math.max(0, essence - (gaz ? (turbo ? 3.2f : 1.5f) : 0.3f) * dt);
        x += glisse * dt;
        s += vitesse * dt;
        // le bord : le gravier freine et abime ; sur la piste, au-dela, c'est le vide
        float ecart = Math.abs(x - centre(s));
        float demi = largeur(s) / 2;
        gravier = 0;
        if (ecart > demi - carL / 2f) {
            if (t.vide() && ecart > demi + 10) {
                perdre("TU AS QUITTÉ LA PISTE.", 22);
                sorties++;
                return;
            }
            gravier = 1;
            vitesse = Math.max(0, vitesse - vitesse * 2.4f * dt);
            degats += 3 * dt;
            secousse = Math.max(secousse, 0.4f);
            if (hasard.nextInt(4) == 0) {
                poussiere(x, s, t.vide() ? 0xC08050 : 0x807868);
            }
            if (ecart > demi + 24) {                                    // pas plus loin que le talus
                x = centre(s) + Math.signum(x - centre(s)) * (demi + 24);
            }
        }
        // les obstacles
        for (Obstacle o : obstacles) {
            if (o.pris) {
                continue;
            }
            o.s += o.vs * dt;
            if (o.genre == Genre.CHEVRE) {
                o.dx += o.vx * dt;
                if (Math.abs(o.dx) > largeur(o.s) / 2 + 10) {
                    o.vx = -o.vx;
                }
            }
            float ox = centre(o.s) + o.dx;
            float px = (o.l + carL) / 2f - Math.abs(ox - x);            // de combien les deux boites se recouvrent
            float ps = (o.h + carH) / 2f - Math.abs(o.s - s);
            if (px > 0 && ps > 0) {
                heurter(o, ox);
                if (o.genre != Genre.BIDON && o.genre != Genre.NID && !o.pris) {
                    separer(o, ox, px, ps);                             // on ne traverse pas ce qu'on percute
                }
                if (etape != Etape.ROUTE) {
                    return;
                }
            }
        }
        if (degats >= 100) {
            perdre("LA RS6 FUME. TU REPARS DU DÉBUT DU TRONÇON.", 0);
            return;
        }
        if (essence <= 0 && vitesse < 5) {
            perdre("PANNE SÈCHE. UN BIDON TE RAMÈNE AU DÉBUT DU TRONÇON.", 0);
            return;
        }
        int ici = indexDe(s);
        if (ici != troncon) {                                           // un nouveau troncon : c'est le prochain point de reprise
            troncon = ici;
            audio.jouer("objectif", 0.5f, 0, 1.2f);
        }
        if (s >= longueur) {
            etape = Etape.ARRIVEE;
            temps = 0;
            audio.jouer("mission", 0.7f, 0, 1);
        }
        moteur(vitesse / VITESSE_MAX);
    }

    private void heurter(Obstacle o, float ox) {
        if (o.genre == Genre.BIDON) {
            o.pris = true;
            bidons++;
            essence = Math.min(100, essence + 35);
            audio.jouer("tel_argent", 0.5f, 0, 0.8f);
            audio.jouer(Audio.Bus.EFFETS, "ferraille", 0.3f, 0, 1.6f, 0);
            return;
        }
        if (o.genre == Genre.NID) {                                     // l'huile : pas de choc, la voiture part en glissade
            if (invincible <= 0) {
                glisse = (hasard.nextBoolean() ? 1 : -1) * LATERALE_MAX;
                vitesse *= 0.85f;
                invincible = 0.5f;
                audio.jouer("pas_flaque", 0.7f, 0, 0.7f);
            }
            return;
        }
        if (invincible > 0) {
            return;
        }
        float coup = switch (o.genre) {                                 // il faut une dizaine de gros chocs pour la casser
            case CAMION -> 11;
            case ROCHER -> 9;
            case CHEVRE -> 5;
            default -> 3;
        };
        degats += coup;
        invincible = 0.7f;
        secousse = 1f;
        float garde = o.genre == Genre.NID ? 0.75f : 0.35f;
        vitesse *= garde;
        audio.jouer(o.genre == Genre.NID ? "coup_sourd" : "choc", 0.8f, 0, o.genre == Genre.CAMION ? 0.7f : 1f);
        if (o.genre != Genre.NID) {
            audio.jouer(Audio.Bus.EFFETS, "ferraille", 0.5f, 0, 0.8f, 0);
        }
        if (o.genre == Genre.CHEVRE) {
            o.pris = true;                                              // elle file sur le bas-cote, vexee
        }
        for (int k = 0; k < 10; k++) {                                  // des etincelles
            etincelles.add(new float[] {x, s, (hasard.nextFloat() * 2 - 1) * 120, -hasard.nextFloat() * 120 + vitesse * 0.4f, 0.4f,
                0xFFD27A});
        }
    }

    /**
     * La voiture et l'obstacle se recouvrent : on les separe par le cote le plus court. Par l'avant (on rentre dedans),
     * la RS6 reste derriere lui et ne va pas plus vite que lui ; par le cote (on le frotte), elle est repoussee de cote.
     */
    private void separer(Obstacle o, float ox, float px, float ps) {
        if (ps < px) {
            if (s < o.s) {
                s = o.s - (o.h + carH) / 2f - 0.5f;
                vitesse = Math.min(vitesse, Math.max(0, o.vs) * 0.9f);
            } else {
                s = o.s + (o.h + carH) / 2f + 0.5f;                    // il nous a rattrapes par l'arriere
            }
        } else {
            float sens = x >= ox ? 1 : -1;
            x += sens * (px + 0.5f);
            glisse = sens * Math.max(Math.abs(glisse) * 0.4f, 30);       // un petit rebond, du bon cote
        }
    }

    private void poussiere(float px, float ps, int couleur) {
        etincelles.add(new float[] {px + (hasard.nextFloat() * 2 - 1) * 6, ps - 10, (hasard.nextFloat() * 2 - 1) * 30, -40, 0.5f,
            couleur});
    }

    private float derniereReprise = -1;
    private int pertesIci;

    private void perdre(String pourquoi, float coup) {
        degats = Math.min(100, degats + coup);
        if (derniereReprise >= 0 && Math.abs(s - derniereReprise) < 400) {
            pertesIci++;                                                 // encore au meme endroit
        } else {
            pertesIci = 1;
        }
        derniereReprise = s;
        perte = pourquoi;
        etape = Etape.PERTE;
        temps = 0;
        vitesse = 0;
        secousse = 1.2f;
        audio.jouer("coup_sourd", 0.8f, 0, 0.8f);
        audio.jouer("choc", 0.7f, 0, 0.6f);
    }

    /** On repart au debut du troncon : la voiture reparee de ce qu'il faut, un bidon dans le reservoir. Le temps, lui, est perdu. */
    private void repartir() {
        float debut = debutDe(troncon) + 20;
        // deux fois au meme endroit : on repart juste avant ce passage, pour ne pas tourner en rond
        s = pertesIci >= 2 && derniereReprise > debut + 250 ? derniereReprise - 200 : debut;
        x = centre(s);
        angle = 0;
        glisse = 0;
        vitesse = 0;
        essence = Math.max(essence, 55);
        degats = Math.min(degats, 45);                                  // un depannage rapide : elle roule, cabossee
        invincible = 1.5f;
        etape = Etape.ROUTE;
        temps = 0;
        chrono += 4;                                                    // le temps de remettre la voiture sur la route
        for (Obstacle o : obstacles) {                                  // les camions repartent aussi
            if (o.genre == Genre.CAMION && o.s < s + 600 && o.s > s - 200) {
                o.s += 400;
            }
        }
        camS = s;
    }

    // ------------------------------------------------------------------ le dessin

    /** La ligne de l'ecran ou se trouve la voiture a l'arret ; lancee, elle descend un peu (la camera regarde devant). */
    private static final int HORIZON = 172;

    /** La route reste au milieu de l'ecran ; c'est la voiture qui se deplace dessus, de gauche a droite. */
    private int ecranX(float wx) {
        return Math.round(wx - centre(0) + Rendu.L / 2f);
    }

    /** Pour les essais : ou se dessine ce point de la route a l'ecran. */
    int ecranYDe(float ws) {
        return ecranY(ws);
    }

    private int ecranY(float ws) {
        return Math.round(HORIZON - (ws - camS));
    }

    void dessiner(Rendu rendu) {
        int tremble = secousse > 0 ? Math.round((hasard.nextFloat() * 2 - 1) * secousse * 3) : 0;
        Troncon t = PARCOURS[Math.min(PARCOURS.length - 1, indexDe(camS))];
        // le sol du bord, puis la route, en carreaux qui defilent
        Texture sol = res.image(SOLS[Math.min(PARCOURS.length - 1, indexDe(camS))]);
        int decale = Math.floorMod(Math.round(camS), 32);
        if (sol != null) {
            for (int y = -32 + decale; y < Rendu.H; y += 32) {
                for (int x0 = 0; x0 < Rendu.L; x0 += 32) {
                    rendu.calque(sol, x0 + tremble, y);
                }
            }
        } else {
            rendu.rect(0, 0, Rendu.L, Rendu.H, t.bord(), 256);
        }
        for (int y = -32 + decale; y < Rendu.H; y += 32) {
            float ws = camS + (HORIZON - y - 16);
            int ti = indexDe(ws);
            Texture bande = bandes[ti];
            int cx = ecranX(centre(ws)) + tremble;
            int demi = bande.largeur() / 2;
            if (PARCOURS[ti].vide()) {                                  // la piste : le ravin au-dela d'un talus de sable
                rendu.rect(0, y, cx - demi - 22, 32, 0x1A0C0A, 256);
                rendu.rect(cx + demi + 22, y, Rendu.L, 32, 0x1A0C0A, 256);
                rendu.rect(cx - demi - 24, y, 3, 32, 0x0E0605, 256);
                rendu.rect(cx + demi + 21, y, 3, 32, 0x0E0605, 256);
            }
            rendu.calque(bande, cx - demi, y);
        }
        for (int y = 0; y < Rendu.H; y += 2) {                          // les marquages
            float ws = camS + (HORIZON - y);
            Troncon ti = PARCOURS[indexDe(ws)];
            int cx = ecranX(centre(ws)) + tremble;
            int demi = Math.round(largeur(ws) / 2);
            if (ti.vide()) {
                rendu.rect(cx - demi, y, 2, 2, 0x5A3020, 200);
                rendu.rect(cx + demi - 2, y, 2, 2, 0x5A3020, 200);
                continue;
            }
            rendu.rect(cx - demi + 3, y, 2, 2, 0xD8D8D0, 170);
            rendu.rect(cx + demi - 5, y, 2, 2, 0xD8D8D0, 170);
            if (((int) (ws / 26)) % 2 == 0) {
                rendu.rect(cx - 1, y, 2, 2, 0xE8E0B0, 190);
                if (ti.largeur() > 120) {
                    rendu.rect(cx - demi / 2 - 1, y, 2, 2, 0xE8E0B0, 140);
                    rendu.rect(cx + demi / 2 - 1, y, 2, 2, 0xE8E0B0, 140);
                }
            }
        }
        for (Deco d : decor) {                                          // le bord : arbres, lampadaires, rochers
            int dy = ecranY(d.s());
            if (dy < -40 || dy > Rendu.H + 40) {
                continue;
            }
            int dx = ecranX(centre(d.s()) + d.dx()) + tremble;
            if (d.image().equals("lampadaire")) {
                rendu.rect(dx - 1, dy - 1, 3, 3, 0x202024, 256);
                rendu.rect(dx - 12, dy - 12, 24, 24, 0xFFE8A0, 26);
                rendu.rect(dx - 6, dy - 6, 12, 12, 0xFFE8A0, 40);
                rendu.rect(dx - 1, dy - 1, 2, 2, 0xFFF4C0, 256);
                continue;
            }
            Texture tex = res.image(d.image());
            if (tex != null) {
                rendu.calque(tex, dx - tex.largeur() / 2, dy - tex.hauteur() / 2);
            }
        }
        // l'arrivee
        int ya = ecranY(longueur);
        if (ya > -10 && ya < Rendu.H) {
            int cx = ecranX(centre(longueur)) + tremble;
            for (int k = -40; k < 40; k += 6) {
                rendu.rect(cx + k, ya, 3, 3, (k / 6) % 2 == 0 ? 0xFFFFFF : 0x101010, 256);
            }
        }
        // les obstacles
        for (Obstacle o : obstacles) {
            if (o.pris) {
                continue;
            }
            int oy = ecranY(o.s);
            if (oy < -60 || oy > Rendu.H + 60) {
                continue;
            }
            int ox = ecranX(centre(o.s) + o.dx) + tremble;
            dessinerObstacle(rendu, o, ox, oy);
        }
        // les phares, de nuit
        if (PARCOURS[troncon].nuit() && etape != Etape.PERTE) {
            for (int k = 0; k < 60; k += 3) {
                int largeurCone = 6 + k / 2;
                int py = ecranY(s) - 16 - k;
                int px = ecranX(x) + Math.round((float) Math.sin(angle) * k);
                rendu.rect(px - largeurCone / 2, py, largeurCone, 3, 0xFFF0B0, Math.max(0, 50 - k));
            }
        }
        // la voiture
        Texture v = voiture(angle);
        rendu.calque(v, ecranX(x) - v.largeur() / 2, ecranY(s) - v.hauteur() / 2);   // a sa vraie place
        for (float[] e : etincelles) {
            rendu.rect(ecranX(e[0]), ecranY(e[1]), 2, 2, (int) e[5], Math.round(255 * Math.min(1, e[4] * 3)));
        }
        if (vitesse > VITESSE_MAX * 0.8f && etape == Etape.ROUTE) {     // la vitesse : des traits qui filent
            for (int k = 0; k < 8; k++) {
                int lx = (int) ((k * 61 + temps * 900) % Rendu.L);
                rendu.rect(lx, (int) ((k * 37 + temps * 700) % Rendu.H), 1, 10, 0xFFFFFF, 60);
            }
        }
        if (!PARCOURS[troncon].nuit()) {                                 // l'aube sur Namek
            rendu.voile(0xFF9A50, 0.08f);
        }
        interfaceDeBord(rendu);
    }

    private void dessinerObstacle(Rendu rendu, Obstacle o, int ox, int oy) {
        if (o.genre == Genre.BIDON) {                                    // le bidon d'essence : rouge, et il brille
            int pulse = (int) (temps * 6) % 2;
            rendu.rect(ox - 7, oy - 8, 14, 16, 0xFFD27A, 50 + pulse * 40);
            rendu.rect(ox - 4, oy - 5, 8, 10, 0xC8361E, 256);
            rendu.rect(ox - 4, oy - 5, 8, 2, 0xE85A3E, 256);
            rendu.rect(ox - 2, oy - 7, 4, 2, 0x303030, 256);
            return;
        }
        Texture tex = o.image == null ? null : res.image(o.image);
        if (tex == null) {
            rendu.rect(ox - Math.round(o.l) / 2, oy - Math.round(o.h) / 2, Math.round(o.l), Math.round(o.h), 0x707070, 256);
            return;
        }
        if (o.genre != Genre.NID) {                                     // une ombre portee, decalee vers le bas
            rendu.rect(ox - tex.largeur() / 2 + 2, oy - tex.hauteur() / 2 + 3, tex.largeur() - 2, tex.hauteur() - 2, 0x000000, 70);
        }
        rendu.calque(tex, ox - tex.largeur() / 2, oy - tex.hauteur() / 2);
        if (o.genre == Genre.CAMION && PARCOURS[troncon].nuit()) {       // ses feux arriere, la nuit
            rendu.rect(ox - tex.largeur() / 2 + 2, oy + tex.hauteur() / 2 - 2, 3, 2, 0xFF3020, 256);
            rendu.rect(ox + tex.largeur() / 2 - 5, oy + tex.hauteur() / 2 - 2, 3, 2, 0xFF3020, 256);
        }
    }

    /** La RS6 vue du dessus, tournee de cet angle (le dessin est calcule une fois par cran de volant). */
    private Texture voiture(float a) {
        int cran = Math.round(a * 12);
        return voitures.computeIfAbsent(cran, k -> tourner(base(), k / 12f));
    }

    private Texture base = null;

    private Texture base() {
        if (base != null) {
            return base;
        }
        Texture rs6 = res.image("cond_rs6");                            // la RS6 d'Omar (Racing Pack de Kenney)
        if (rs6 != null) {
            base = rs6;
            return base;
        }
        int l = VOITURE_L;
        int h = VOITURE_H;
        int[] p = new int[l * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < l; i++) {
                boolean coin = (i == 0 || i == l - 1) && (j < 2 || j > h - 3);
                if (coin) {
                    continue;
                }
                int c = 0xFF5E6068;                                     // la carrosserie grise
                if (j >= 6 && j <= 9 && i >= 2 && i <= l - 3) {
                    c = 0xFF1A2030;                                     // le pare-brise
                } else if (j >= 18 && j <= 20 && i >= 2 && i <= l - 3) {
                    c = 0xFF1A2030;                                     // la lunette
                } else if (j >= 10 && j <= 17 && i >= 3 && i <= l - 4) {
                    c = 0xFF70747C;                                     // le toit
                } else if (j <= 1 && (i <= 3 || i >= l - 4)) {
                    c = 0xFFFFF0B0;                                     // les phares
                } else if (j >= h - 2 && (i <= 3 || i >= l - 4)) {
                    c = 0xFFE04A3A;                                     // les feux
                } else if (i == 0 || i == l - 1) {
                    c = 0xFF2A2A30;                                     // les roues, sur le cote
                }
                p[j * l + i] = c;
            }
        }
        base = new Texture(l, h, p);
        return base;
    }

    private static Texture tourner(Texture t, float a) {
        int cote = (int) Math.ceil(Math.hypot(t.largeur(), t.hauteur())) + 2;
        int[] p = new int[cote * cote];
        double c = Math.cos(a);
        double s = Math.sin(a);
        for (int y = 0; y < cote; y++) {
            for (int x = 0; x < cote; x++) {
                double dx = x - cote / 2.0;
                double dy = y - cote / 2.0;
                int sx = (int) Math.round(dx * c + dy * s + t.largeur() / 2.0);
                int sy = (int) Math.round(-dx * s + dy * c + t.hauteur() / 2.0);
                if (sx >= 0 && sy >= 0 && sx < t.largeur() && sy < t.hauteur()) {
                    p[y * cote + x] = t.pixels()[sy * t.largeur() + sx];
                }
            }
        }
        return new Texture(cote, cote, p);
    }

    private void interfaceDeBord(Rendu rendu) {
        // en haut : le troncon, la progression, l'heure du soleil
        rendu.rect(0, 0, Rendu.L, 20, 0x08080C, 200);
        affichage.petite().ecrire(rendu, PARCOURS[troncon].nom(), 8, 4, Affichage.ORANGE, 1);
        int bx = 8;
        int bl = 220;
        rendu.rect(bx, 13, bl, 2, 0x2A2A36, 256);
        rendu.rect(bx, 13, Math.round(bl * Math.min(1, s / longueur)), 2, Affichage.VERT, 256);
        for (int i = 1; i < PARCOURS.length; i++) {
            rendu.rect(bx + Math.round(bl * debutDe(i) / longueur), 11, 1, 6, Affichage.GRIS, 256);
        }
        float reste = SOLEIL - chrono;
        int heure = 6 * 60 - Math.round(Math.max(0, reste) * 60 / SOLEIL * 60) / 60;   // la route se fait en une heure de jeu
        String h = reste >= 0 ? String.format("%02d:%02d", heure / 60, heure % 60) : "SOLEIL LEVÉ";
        affichage.petite().ecrire(rendu, "LEVER DU SOLEIL : 06:00", Rendu.L - 128, 4, Affichage.SOMBRE, 1);
        affichage.police().ecrire(rendu, h, Rendu.L - 128, 11, reste < 10 ? 0xE04A3A : Affichage.BLANC, 1);
        // en bas : vitesse, essence, carrosserie
        rendu.rect(0, Rendu.H - 22, Rendu.L, 22, 0x08080C, 200);
        String kmh = Math.round(vitesse * 0.55f) + " KM/H";
        affichage.police().ecrire(rendu, kmh, 8, Rendu.H - 16, Affichage.BLANC, 1);
        jauge(rendu, 92, Rendu.H - 16, "ESSENCE", essence, essence < 20 ? 0xE04A3A : Affichage.ORANGE);
        jauge(rendu, 240, Rendu.H - 16, "CARROSSERIE", 100 - degats, degats > 70 ? 0xE04A3A : Affichage.VERT);
        affichage.petite().ecrire(rendu, "MAJ : TURBO", Rendu.L - 64, Rendu.H - 14, Affichage.SOMBRE, 1);
        switch (etape) {
            case CONSIGNES -> consignes(rendu);
            case DECOMPTE -> affichage.police().ecrireCentre(rendu, Integer.toString((int) Math.ceil(3 - temps)), Rendu.L / 2, 100,
                    Affichage.BLANC, 4);
            case PERTE -> {
                rendu.rect(0, 100, Rendu.L, 30, 0x000000, 180);
                affichage.police().ecrireCentre(rendu, perte, Rendu.L / 2, 110, 0xE04A3A, 1);
            }
            case PAUSE -> {
                rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 150);
                affichage.police().ecrireCentre(rendu, "PAUSE", Rendu.L / 2, 96, Affichage.BLANC, 2);
                affichage.petite().ecrire(rendu, "ÉCHAP : REPRENDRE   ·   E : REPRENDRE AU DÉBUT DU TRONÇON",
                        Rendu.L / 2 - affichage.petite().largeur("ÉCHAP : REPRENDRE   ·   E : REPRENDRE AU DÉBUT DU TRONÇON", 1) / 2,
                        124, Affichage.GRIS, 1);
            }
            case ARRIVEE -> arrivee(rendu);
            default -> { }
        }
    }

    private void jauge(Rendu rendu, int x, int y, String nom, float valeur, int couleur) {
        affichage.petite().ecrire(rendu, nom, x, y + 1, Affichage.SOMBRE, 1);
        int bx = x + affichage.petite().largeur(nom, 1) + 6;
        rendu.rect(bx, y + 2, 70, 4, 0x2A2A36, 256);
        rendu.rect(bx, y + 2, Math.round(70 * Math.max(0, valeur) / 100f), 4, couleur, 256);
    }

    private void consignes(Rendu rendu) {
        rendu.rect(70, 46, 340, 150, 0x0C0C14, 235);
        rendu.rect(70, 46, 340, 3, Affichage.ORANGE, 256);
        affichage.petite().ecrire(rendu, "MISSION 2.4", 84, 56, Affichage.ORANGE, 1);
        affichage.police().ecrire(rendu, "LA ROUTE DE NAMEK", 84, 66, Affichage.BLANC, 2);
        String[] lignes = {"Arrive avant le lever du soleil : après, les plants brûlent, et la récolte sera plus courte.",
            "La RS6 boit : ramasse les bidons rouges sur la route.", "Gauche et droite pour éviter camions, nids-de-poule, chèvres et rochers. Chaque choc l'abîme. Sur la piste, le bord, c'est le vide."};
        int y = 92;
        for (String l : lignes) {
            rendu.rect(84, y + 4, 3, 3, Affichage.ORANGE, 256);
            for (String m : affichage.police().couper(affichage.police().propre(l), 300)) {
                affichage.police().ecrire(rendu, m, 92, y, Affichage.GRIS, 1);
                y += 12;
            }
            y += 3;
        }
        String[][] touches = {{"HAUT", "ACCÉLÉRER"}, {"BAS", "FREINER"}, {"GAUCHE DROITE", "TOURNER"}, {"MAJ", "TURBO"}};
        int x = 84;
        for (String[] t : touches) {
            int lk = affichage.petite().largeur(t[0], 1) + 6;
            rendu.rect(x, 172, lk, 9, 0x2A2A36, 256);
            affichage.petite().ecrire(rendu, t[0], x + 3, 173, Affichage.BLANC, 1);
            x += lk + 4;
            affichage.petite().ecrire(rendu, t[1], x, 173, Affichage.GRIS, 1);
            x += affichage.petite().largeur(t[1], 1) + 12;
        }
        if (temps > 0.8f && (int) (temps * 2) % 2 == 0) {
            affichage.petite().ecrire(rendu, "E : DÉMARRER", 330, 184, Affichage.BLANC, 1);
        }
    }

    private void arrivee(Rendu rendu) {
        rendu.rect(110, 60, 260, 120, 0x0C0C14, 235);
        rendu.rect(110, 60, 260, 3, Affichage.VERT, 256);
        affichage.police().ecrire(rendu, "NAMEK", 124, 70, Affichage.BLANC, 2);
        float reste = SOLEIL - chrono;
        String temps1 = reste >= 0 ? "AVANT LE SOLEIL, DE " + Math.round(reste) + " SECONDES." : "LE SOLEIL EST LEVÉ DEPUIS " + Math.round(-reste)
                + " SECONDES : LA RÉCOLTE SERA PLUS COURTE.";
        int y = 96;
        for (String m : affichage.police().couper(affichage.police().propre(temps1), 232)) {
            affichage.police().ecrire(rendu, m, 124, y, reste >= 0 ? Affichage.VERT : 0xE04A3A, 1);
            y += 12;
        }
        affichage.police().ecrire(rendu, "CARROSSERIE : " + Math.round(100 - degats) + " %", 124, y + 4, Affichage.GRIS, 1);
        affichage.police().ecrire(rendu, "BIDONS : " + bidons + "   SORTIES DE ROUTE : " + sorties, 124, y + 18, Affichage.GRIS, 1);
        if (temps > 1.2f) {
            affichage.petite().ecrire(rendu, "E : CONTINUER", 300, 168, Affichage.BLANC, 1);
        }
    }
}
