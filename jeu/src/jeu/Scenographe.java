package jeu;

import decor.Acteur;
import decor.Poses;
import decor.Rendu;
import decor.Scene;
import decor.Texture;
import histoire.Campagne;
import histoire.Cinematique;
import histoire.Plan;
import histoire.Progression;
import histoire.TexteProgressif;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Joue une cinematique avec les elements du jeu : un decor, des personnages qui marchent, une camera, des effets,
 * des dialogues avec bustes et choix, des gros plans, des sons. Une scene sans decor a elle se joue la ou se
 * trouve le joueur, avec les personnages deja presents : elle peut les faire bouger, et deplacer le heros.
 */
final class Scenographe {
    private static final int BANDES = 16;

    /** Un deplacement en cours : un personnage ou la camera va d'un point a un autre en un temps donne. */
    private static final class Trajet {
        final Acteur acteur;
        final float x0;
        final float y0;
        final float x1;
        final float y1;
        final float duree;
        float temps;

        Trajet(Acteur acteur, float x0, float y0, float x1, float y1, float duree) {
            this.acteur = acteur;
            this.x0 = x0;
            this.y0 = y0;
            this.x1 = x1;
            this.y1 = y1;
            this.duree = Math.max(0.01f, duree);
        }

        float part() {
            return Math.min(1f, temps / duree);
        }
    }

    private final Ressources res;
    private final Monde monde;
    private final Progression progression;
    private final Reglages reglages;
    private final Audio audio;
    private final Affichage affichage;
    private final Parole parole;
    private Encarts encarts;
    /** Le mois qui passe ("mois"), pendant qu'on le montre ; et le signal qu'il vient d'etre compte (a sauvegarder). */
    private Timelapse mois;
    private boolean moisCompte;
    /** Le combat que la scene demande en finissant ("combat alien 4 victoire defaite"), ou null. */
    private Plan combatDemande;
    private final Map<String, Acteur> acteurs = new LinkedHashMap<>();
    private final List<Trajet> trajets = new ArrayList<>();
    private final Map<String, float[]> ambiancesEnPlus = new LinkedHashMap<>();
    private Cinematique scene;
    private int rang;
    private boolean surPlace;
    private Scene decor;
    private String carteDecor;
    private float camX;
    private Trajet travelling;
    private String effet = "normal";
    private float eclair;
    private float ouverture;
    private float attente;
    private TexteProgressif texte;
    private int choisi;
    private boolean choixOuvert;
    /** Un "noir" sans texte : la scene se joue sur fond noir jusqu'a son prochain decor (une douane qu'on ne montre pas). */
    private boolean fondNoir;
    private String dernierQuiParle;
    private long tempsMs;
    private String carteFin;
    private float xFin;
    private float yFin;
    /** "joueur <carte> scooter x y" : la scene rend la main a cote du scooter gare dans cette carte, s'il y est. */
    private boolean finPresDuScooter;
    private String musique;
    private String meteo;
    /** La frappe de cette image a deja servi : elle ne doit pas aussi terminer la replique suivante. */
    private boolean frappeServie;
    /** Un objet qui passe de main en main ("donne sac_lili 1 geto", "remet sac_lili 1 diallo") : ce qui passe, et ou il en est. */
    private Plan transfert;
    private float transfertT;
    private Texture poseDonneur;
    private Monde.Habitant donneur;
    static final float DUREE_TRANSFERT = 0.8f;
    /** Quelqu'un de la scene au telephone : qui, avec qui, ou en est l'appel ("sonne", "ligne"), depuis quand. */
    private String appelQui;
    private String appelAvec;
    private String appelEtat;
    private float appelT;
    private float appelDuree;
    private int sensAvantAppel;
    /** Celui a qui le joueur vient de s'adresser (E) : on ne presente quelqu'un qu'au moment de le rencontrer vraiment. */
    private String interlocuteur;

    /** Le joueur vient d'appuyer sur E devant ce personnage : la scene qui suit est sa rencontre. */
    void parlerA(String id) {
        interlocuteur = id;
    }

    /** Pour les essais : qui est au telephone en ce moment, et l'etat de l'appel ("sonne", "ligne"), ou null. */
    String appel() {
        return appelQui == null ? null : appelQui + ":" + appelEtat;
    }

    Scenographe(Ressources res, Monde monde, Progression progression, Reglages reglages, Audio audio, Affichage affichage) {
        this.res = res;
        this.monde = monde;
        this.progression = progression;
        this.reglages = reglages;
        this.audio = audio;
        this.affichage = affichage;
        this.parole = new Parole(audio, res.campagne);
    }

    /** Les encarts et le combat de la partie : une scene peut montrer l'un et lancer l'autre. */
    void brancher(Encarts encarts, Combat combat) {
        this.encarts = encarts;
    }

    /** Vrai une fois, juste apres qu'un mois a ete compte : la partie doit l'enregistrer tout de suite. */
    boolean prendreMoisCompte() {
        boolean m = moisCompte;
        moisCompte = false;
        return m;
    }

    private void compterLeMois() {
        mois = new Timelapse(affichage, audio);
        mois.commencer(progression);
        moisCompte = true;
    }

    private Plan conduiteDemandee;

    /** La conduite demandee par la scene qui vient de finir ("conduite namek suite"), une seule fois, ou null. */
    Plan prendreConduite() {
        Plan p = conduiteDemandee;
        conduiteDemandee = null;
        return p;
    }

    /** Le combat demande par la scene qui vient de finir (une seule fois), ou null. */
    Plan prendreCombat() {
        Plan p = combatDemande;
        combatDemande = null;
        return p;
    }

    void jouer(Cinematique cinematique) {
        scene = cinematique;
        rang = 0;
        surPlace = cinematique.surPlace();
        decor = null;
        carteDecor = surPlace ? monde.carte() : null;
        acteurs.clear();
        trajets.clear();
        ambiancesEnPlus.clear();
        travelling = null;
        effet = "normal";
        eclair = 0;
        texte = null;
        attente = 0;
        carteFin = null;
        finPresDuScooter = false;
        musique = null;
        meteo = null;
        dernierQuiParle = null;
        choixOuvert = false;
        fondNoir = false;
        ouverture = surPlace ? 0 : 0.4f;
        parole.couper();
        if (!"normal".equals(audio.mixageCourant())) {
            audio.mixage("normal", 0.8f);
        }
        if (surPlace) {
            monde.piloter(true);
            java.util.Set<String> participants = new java.util.HashSet<>();
            for (Plan p : cinematique.plans()) {                       // ceux qui jouent dans la scene
                switch (p.genre()) {
                    case DIT, REGARDE, MARCHE, RETIRE, POSE, APPEL -> participants.add(p.mot(0));
                    case DONNE, REMET -> participants.add(p.mot(2));
                    default -> { }
                }
            }
            monde.reserver(participants);
        }
    }

    /** Ou en est la scene : son nom, le rang du plan et son genre. Pour les essais et les captures. */
    String position() {
        Plan plan = plan();
        return scene == null ? "" : scene.id() + "#" + rang + ":" + (plan == null ? "" : plan.genre());
    }

    boolean auChoix() {
        Plan plan = plan();
        return plan != null && plan.genre() == Plan.Genre.CHOIX;
    }

    boolean enCours() {
        return scene != null;
    }

    String carteFin() {
        return carteFin;
    }

    float xFin() {
        return xFin;
    }

    float yFin() {
        return yFin;
    }

    /** Vrai si la scene veut rendre la main a cote du scooter gare (il est bien dans la carte de fin). */
    boolean finPresDuScooter() {
        return finPresDuScooter;
    }

    /** La musique que la scene impose, ou null pour laisser celle du lieu. */
    String musique() {
        return musique;
    }

    /** Le temps qu'il fait dans la scene, ou null pour garder celui de la mission. */
    String meteo() {
        return meteo;
    }

    /** Le decor que montre la scene : celui du joueur pour une scene sur place. */
    String carteVue() {
        return carteDecor;
    }

    Scene decorVu() {
        return surPlace ? monde.scene() : decor;
    }

    float camXVue() {
        return surPlace ? monde.camX() : camX;
    }

    Map<String, float[]> ambiancesEnPlus() {
        return ambiancesEnPlus;
    }

    /** Vrai tant que l'ecran est noir : pas de decor a faire entendre. */
    boolean ecranNoir() {
        Plan plan = plan();
        Plan.Genre genre = plan == null ? null : plan.genre();
        return genre == Plan.Genre.NOIR || genre == Plan.Genre.TITRE || genre == Plan.Genre.CARTON || genre == Plan.Genre.GROS_PLAN
                || fondNoir || (!surPlace && decor == null);
    }

    private Plan plan() {
        return scene != null && rang < scene.plans().size() ? scene.plans().get(rang) : null;
    }

    private float x(String texteX) {
        return res.abscisse(carteDecor, texteX);
    }

    /** L'acteur qu'un plan designe : un acteur de la scene, un personnage present sur place, ou le heros. */
    private Acteur acteur(String id) {
        Acteur propre = acteurs.get(id);
        if (propre != null || !surPlace) {
            return propre;
        }
        if (id.equals("toi")) {
            return monde.heros();
        }
        Monde.Habitant habitant = monde.habitant(id);
        return habitant == null ? null : habitant.acteur;
    }

    void maj(float dt, Touches touches) {
        if (scene == null) {
            return;
        }
        tempsMs += Math.round(dt * 1000);
        frappeServie = false;
        eclair = Math.max(0f, eclair - dt * 2f);
        ouverture = Math.max(0f, ouverture - dt);
        for (Trajet trajet : trajets) {
            trajet.temps += dt;
            trajet.acteur.x = trajet.x0 + (trajet.x1 - trajet.x0) * trajet.part();
            trajet.acteur.y = trajet.y0 + (trajet.y1 - trajet.y0) * trajet.part();
            trajet.acteur.marche = trajet.part() < 1f;
            if (trajet.x1 != trajet.x0) {
                trajet.acteur.miroir = trajet.x1 < trajet.x0;
            }
        }
        trajets.removeIf(t -> t.part() >= 1f);
        if (travelling != null) {
            travelling.temps += dt;
            float p = travelling.part();
            float doux = p * p * (3 - 2 * p);
            camX = travelling.x0 + (travelling.x1 - travelling.x0) * doux;
            if (p >= 1f) {
                travelling = null;
            }
        }
        if ("ligne".equals(appelEtat)) {
            appelDuree += dt;
        }
        if (touches.retour() && plan() != null && plan().genre() != Plan.Genre.CHOIX) {
            passer();
            return;
        }
        int pas = 0;
        while (scene != null) {
            Plan plan = plan();
            if (plan == null) {
                finir();
            } else if (++pas > 4 * scene.plans().size() + 50) {     // une boucle sans rien a attendre : on n'y reste pas
                System.err.println("Scene en boucle, abandonnee : " + position());
                finir();
            } else if (!executer(plan, dt, touches)) {
                break;
            }
        }
        audio.parole(scene != null && (texte != null || auChoix() || parole.active()));
    }

    private void finir() {
        if (transfert != null) {                                        // un objet a moitie passe d'une main a l'autre : il arrive
            appliquerTransfert();
        }
        if (appelQui != null) {
            raccrocher(false);
        }
        interlocuteur = null;
        scene = null;
        parole.couper();
        audio.parole(false);
        if (!"normal".equals(audio.mixageCourant())) {                 // un choc ne survit pas a sa scene
            audio.mixage("normal", 1.5f);
        }
        if (surPlace) {
            monde.piloter(false);
            for (Acteur a : List.of(monde.heros())) {
                a.marche = false;
            }
        }
    }

    /**
     * Le joueur passe la scene : on saute a la fin, mais on applique ce qu'elle change pour la suite (les drapeaux,
     * l'endroit ou elle laisse le joueur). Un choix ne se passe pas : on s'arrete dessus, la reponse compte.
     */
    /**
     * "message soul | Le texte. | Une reponse -> +drapeau | Une autre -> -" : un message arrive sur le telephone, sans
     * arreter la scene. Les reponses sont ce que le joueur pourra envoyer, et les drapeaux qu'elles levent.
     */
    private void message(Plan plan) {
        String[] champs = plan.texte().split("\\|");
        java.util.List<histoire.Etat.Reponse> reponses = new java.util.ArrayList<>();
        for (int i = 1; i < champs.length; i++) {
            String[] parts = champs[i].split("->");
            reponses.add(new histoire.Etat.Reponse(parts[0].trim(), parts.length > 1 ? parts[1].trim() : "-"));
        }
        progression.etat().recevoir(plan.mot(0), champs[0].trim(), reponses);
    }

    /** "argent +24 | PAIE DU MAITRE POULET" : l'argent entre ou sort, et le releve du telephone dit pourquoi. */
    private void encaisser(Plan plan) {
        int montant = Integer.parseInt(plan.mot(0));
        progression.etat().gagner(montant, plan.texte().isEmpty() ? (montant >= 0 ? "PAIE" : "DÉPENSE") : plan.texte());
    }

    /** Le rythme de la parole face camera : un peu moins vite qu'un dialogue, il pese ses mots. */
    private static int lettresVisibles(float temps) {
        return Math.max(0, Math.round((temps - 0.35f) * 24f));
    }

    /**
     * Ce qu'il nous dit, face a nous : pas de boite de dialogue, pas de nom. De grandes lettres blanches sous son visage,
     * qui s'ecrivent a mesure qu'il parle, sur un voile qui monte du bas de l'image.
     */
    private void parler(Rendu rendu, String texte, float temps) {
        for (int i = 0; i < 70; i++) {
            rendu.rect(0, Rendu.H - 70 + i, Rendu.L, 1, 0x000000, Math.round(210f * i / 70f));
        }
        String dit = affichage.police().propre(texte);
        int visibles = Math.min(dit.length(), lettresVisibles(temps));
        List<String> lignes = affichage.police().couper(dit, (Rendu.L - 40) / 2);   // en grand, sur deux lignes au plus
        int echelle = 2;
        if (lignes.size() > 2) {
            echelle = 1;
            lignes = affichage.police().couper(dit, Rendu.L - 60);
        }
        int pas = echelle == 2 ? 18 : 12;
        int y = Rendu.H - 26 - lignes.size() * pas + (echelle == 2 ? 2 : 6);
        int restantes = visibles;
        for (String ligne : lignes) {
            String montre = ligne.substring(0, Math.max(0, Math.min(ligne.length(), restantes)));
            restantes -= ligne.length() + 1;
            int x = Rendu.L / 2 - affichage.police().largeur(ligne, echelle) / 2;
            affichage.police().ecrire(rendu, montre, x + echelle, y + echelle, 0x000000, echelle);
            affichage.police().ecrire(rendu, montre, x, y, Affichage.BLANC, echelle);
            y += pas;
        }
    }

    /** "soin 45" : un repas, un pansement. La sante remonte, le souffle revient. */
    private void soigner(Plan plan) {
        audio.jouer("papier", 0.5f, 0, 1.1f);                          // l'emballage
        audio.jouer("bouteille", 0.4f, 0, 1f);
        progression.etat().soigner(Float.parseFloat(plan.mot(0)));
        progression.etat().reposer();
    }

    /** "loue 180" : le heros repart sur le scooter, pour ce nombre de secondes. */
    private void louer(Plan plan) {
        progression.etat().louer(Float.parseFloat(plan.mot(0)));
        progression.marquer("+scooter -scooter_gare -location_finie");
    }

    /** Ce qui s'est passe hors de l'ecran (une ellipse) : la scene n'est pas montree, ses effets s'appliquent. */
    void appliquer(Cinematique ellipse) {
        jouer(ellipse);
        passer();
        if (scene != null) {
            finir();
        }
    }

    /** Une erreur a interrompu la scene : on la quitte la ou elle en est, sans rien rejouer. */
    void abandonner() {
        if (scene != null) {
            finir();
        }
        combatDemande = null;
        mois = null;
        texte = null;
    }

    private void passer() {
        texte = null;
        parole.couper();
        int pas = 0;
        for (int i = rang; i >= 0 && i < scene.plans().size(); i++) {
            if (++pas > 4 * scene.plans().size() + 50) {               // des sauts qui tournent en rond : on sort
                break;
            }
            Plan plan = scene.plans().get(i);
            switch (plan.genre()) {
                case CARTE -> carteDecor = plan.mot(0);
                case JOUEUR -> placer(plan);
                case DRAPEAU -> progression.marquer(String.join(" ", plan.mots()));
                case ARGENT -> encaisser(plan);
                case SMS -> progression.etat().recevoir(plan.mot(0), plan.texte(), java.util.List.of());
                case MESSAGE -> message(plan);
                case SOIN -> soigner(plan);
                case LOUE -> louer(plan);
                case ARME -> progression.etat().prendreArme(plan.mot(0), monde.carte(), monde.joueur.x - 10, monde.joueur.y);
                case TEMPS -> regler(plan);
                case LOYER -> progression.etat().payerTousLesLoyers();
                case DONNE, REMET -> {
                    if (transfert == null || transfert != plan) {
                        transfert = plan;
                    }
                    appliquerTransfert();
                }
                case APPEL -> {
                    if (plan.mot(1).equals("raccroche") && appelQui != null) {
                        raccrocher(false);
                    }
                }
                case MOIS -> {
                    if (mois == null) {
                        compterLeMois();
                    }
                    mois = null;
                }
                case ENCART -> encarts.ouvrir(plan.mot(0), progression);
                case COMBAT -> {
                    combatDemande = plan;
                    finir();
                    return;
                }
                case CONDUITE -> {                                      // passer la scene ne passe pas la route
                    conduiteDemandee = plan;
                    finir();
                    return;
                }
                case PAIE -> {
                    if (!progression.etat().payer(Integer.parseInt(plan.mot(0)))) {
                        i = fin(scene.etiquette(plan.mot(1)));
                    }
                }
                case CHOIX -> {
                    rang = i;
                    choisi = 0;
                    return;
                }
                case SAUT -> {
                    i = fin(scene.etiquette(plan.mot(0)));
                }
                case SI -> {
                    if (progression.verifie(plan.mot(0))) {
                        i = fin(scene.etiquette(plan.mot(1)));
                    }
                }
                default -> { }
            }
        }
        finir();
    }

    /**
     * La premiere fois qu'un personnage important parle, on le presente (sa fiche : portrait, role, ce que le heros sait
     * de lui). "youngssef_boutique" ou "geto_tel" sont les memes personnes que "youngssef" et "geto".
     */
    private boolean presenter(String qui) {
        if (encarts == null || qui == null || qui.isEmpty() || heros(qui) || qui.equals(appelAvec)) {
            return false;
        }
        String base = qui.contains("_") ? qui.substring(0, qui.indexOf('_')) : qui;
        if (!rencontre(qui, base)) {
            return false;                                              // il parle de loin, ou au telephone : pas encore une rencontre
        }
        for (String id : new String[] {"presente_" + qui, "presente_" + base}) {
            if (res.campagne.encarts().containsKey(id)) {
                return encarts.ouvrir(id, progression);
            }
        }
        return false;
    }

    /**
     * Vrai si le heros rencontre vraiment ce personnage : il vient de lui adresser la parole (E), ou celui-ci est la,
     * tout pres de lui, dans la scene jouee sur place.
     */
    private boolean rencontre(String qui, String base) {
        if (interlocuteur != null) {
            String i = interlocuteur.contains("_") ? interlocuteur.substring(0, interlocuteur.indexOf('_')) : interlocuteur;
            if (interlocuteur.equals(qui) || i.equals(base)) {
                return true;
            }
        }
        if (!surPlace) {
            Acteur a = acteurs.get(qui);                               // une scene dans un autre decor : il y est, face au heros
            return a != null && carteDecor != null;
        }
        Monde.Habitant h = monde.habitant(qui);
        return h != null && h.acteur.texture != null && Math.abs(h.acteur.x - monde.heros().x) < 56
                && Math.abs(h.acteur.y - monde.heros().y) < 40;
    }

    // ------------------------------------------------------------------ les objets de main en main

    /** La position (x, y a l'ecran) de la main de quelqu'un de la scene ; "toi" : le heros. */
    private float[] main(String qui) {
        float cam = surPlace ? monde.camX() : camX;
        Acteur h = surPlace ? monde.heros() : acteurs.getOrDefault("toi", monde.heros());
        if (qui == null || qui.isEmpty()) {                             // personne : le sol, juste devant le heros
            return new float[] {h.x - cam + (h.miroir ? -12 : 12), h.y - 4};
        }
        Acteur a = heros(qui) ? h : acteur(qui);
        if (a == null && surPlace) {
            a = monde.objet(qui);
        }
        if (a == null) {
            a = h;
        }
        int haut = a.texture == null ? 40 : a.texture.hauteur();
        return new float[] {a.x - cam, a.y - haut * 0.45f};
    }

    private static String sonDe(String objet) {
        return switch (objet) {
            case "sac_lili", "sac_omar" -> "sac_papier";
            case "enveloppe", "sandwich" -> "papier";
            case "caisses_poulet" -> "caisse";
            case "sachets" -> "froissement";
            case "piccolo" -> "feuillage_1";
            default -> "cles_main";
        };
    }

    /**
     * "donne objet n qui" : celui-la le tend, le heros le prend (il penche vers lui), l'objet passe de l'un a l'autre
     * et entre dans l'inventaire a l'arrivee. "remet" : l'inverse ; rien a remettre, rien ne se joue.
     */
    private boolean transferer(Plan plan, float dt, Touches touches) {
        histoire.Inventaire inv = progression.etat().inventaire();
        if (transfert == null) {
            if (plan.genre() == Plan.Genre.REMET && !inv.possede(plan.mot(0))) {
                return true;                                           // deja livre : on ne mime pas une remise
            }
            transfert = plan;
            transfertT = 0;
            audio.jouer(Audio.Bus.EFFETS, sonDe(plan.mot(0)), 0.6f, 0, 1f, 0);
            monde.imposerPose(Poses.penche(res.image("heros"), 2));    // il tend la main
            donneur = surPlace ? monde.habitant(plan.mot(2)) : null;
            if (donneur != null && donneur.acteur.texture != null) {
                poseDonneur = donneur.acteur.texture;
                donneur.acteur.texture = Poses.penche(poseDonneur, 2);
                donneur.sens = donneur.acteur.x < monde.heros().x ? 1 : -1;
            }
        }
        transfertT += dt * (valide(touches) ? 4 : 1);
        if (transfertT < DUREE_TRANSFERT) {
            return false;
        }
        appliquerTransfert();
        return true;
    }

    private void appliquerTransfert() {
        Plan plan = transfert;
        transfert = null;
        if (plan == null) {
            return;
        }
        histoire.Inventaire inv = progression.etat().inventaire();
        String objet = plan.mot(0);
        int n = plan.mot(1).isEmpty() ? 1 : plan.mot(1).equals("*") ? inv.quantite(objet) : Integer.parseInt(plan.mot(1));
        if (plan.genre() == Plan.Genre.DONNE) {
            if (inv.ajouter(objet, n) > 0) {
                audio.jouer(Audio.Bus.EFFETS, "remise", 0.55f, 0, 1f, 0);
                audio.jouer("inv_ajout", 0.45f, 0, 1);
            }
        } else if (inv.retirer(objet, Math.min(n, inv.quantite(objet)))) {
            audio.jouer(Audio.Bus.EFFETS, "remise", 0.55f, 0, 0.9f, 0);
            audio.jouer("inv_retrait", 0.4f, 0, 1);
        }
        monde.imposerPose(null);
        if (donneur != null && poseDonneur != null) {
            donneur.acteur.texture = poseDonneur;
        }
        donneur = null;
        poseDonneur = null;
    }

    /** L'objet en l'air, d'une main a l'autre, sur un petit arc. */
    private void dessinerTransfert(Rendu rendu) {
        if (transfert == null) {
            return;
        }
        boolean versHeros = transfert.genre() == Plan.Genre.DONNE;
        float[] a = main(versHeros ? transfert.mot(2) : "toi");
        float[] b = main(versHeros ? "toi" : transfert.mot(2));
        float p = Math.min(1f, transfertT / DUREE_TRANSFERT);
        float k = p * p * (3 - 2 * p);
        float x = a[0] + (b[0] - a[0]) * k;
        float y = a[1] + (b[1] - a[1]) * k - (float) Math.sin(Math.PI * k) * 10;
        Texture icone = res.image("obj_" + transfert.mot(0));
        if (icone != null) {
            rendu.rect(Math.round(x) - 5, Math.round(a[1] > b[1] ? b[1] : a[1]) + 26, 10, 2, 0x000000, 60);
            rendu.calque(icone, Math.round(x) - icone.largeur() / 2, Math.round(y) - icone.hauteur() / 2);
        }
        if (p > 0.85f) {                                                // l'arrivee : un petit eclat dans la main
            int r = Math.round((p - 0.85f) * 60);
            rendu.rect(Math.round(b[0]) - r, Math.round(b[1]), r * 2, 1, 0xFFF0C0, 180);
            rendu.rect(Math.round(b[0]), Math.round(b[1]) - r, 1, r * 2, 0xFFF0C0, 180);
        }
    }

    // ------------------------------------------------------------------ le telephone de quelqu'un

    /**
     * "appel geto sonne grossiste_tel" : son telephone sonne et vibre, il le sort et regarde l'ecran ; "appel geto
     * decroche" : il le porte a l'oreille et se detourne ; tant qu'il est en ligne, ses repliques et la voix au bout
     * du fil s'affichent comme un appel ; "appel geto raccroche" : il raccroche et revient vers le heros.
     */
    private boolean appeler(Plan plan, float dt, Touches touches) {
        String qui = plan.mot(0);
        switch (plan.mot(1)) {
            case "sonne" -> {
                if (attente == 0) {
                    appelQui = qui;
                    appelAvec = plan.mot(2);
                    appelEtat = "sonne";
                    appelT = 0;
                    audio.jouer("tel_sonnerie", 0.6f, 0.2f, 1);
                    audio.jouer("vibreur", 0.6f, 0.2f, 1);
                }
                appelT += dt;
                return patienter(1.8f, dt, true, touches);
            }
            case "decroche" -> {
                if (attente == 0) {
                    appelEtat = "ligne";
                    appelDuree = 0;
                    audio.coupe("tel_sonnerie");
                    audio.jouer("tel_decroche", 0.6f, 0.2f, 1);
                    Monde.Habitant h = surPlace ? monde.habitant(qui) : null;
                    if (h != null) {                                   // il se detourne : il parle a quelqu'un d'autre
                        sensAvantAppel = h.sens;
                        h.sens = h.acteur.x < monde.heros().x ? -1 : 1;
                    }
                }
                return patienter(0.5f, dt, false, touches);
            }
            default -> {
                if (attente == 0) {
                    raccrocher(true);
                }
                return patienter(0.5f, dt, false, touches);
            }
        }
    }

    private void raccrocher(boolean son) {
        if (son) {
            audio.jouer("tel_raccroche", 0.55f, 0.2f, 1);
        }
        Monde.Habitant h = surPlace && appelQui != null ? monde.habitant(appelQui) : null;
        if (h != null) {                                               // il range son telephone et revient vers le heros
            h.sens = h.acteur.x < monde.heros().x ? 1 : -1;
        }
        appelQui = null;
        appelAvec = null;
        appelEtat = null;
    }

    /** Le telephone dans la main de celui qui recoit l'appel : il vibre, ou il est a son oreille. */
    private void dessinerAppel(Rendu rendu) {
        if (appelQui == null) {
            return;
        }
        Acteur a = acteur(appelQui);
        if (a == null || a.texture == null) {
            return;
        }
        float cam = surPlace ? monde.camX() : camX;
        int cx = Math.round(a.x - cam);
        int haut = Math.round(a.y) - a.texture.hauteur();
        boolean gauche = a.miroir;
        if ("sonne".equals(appelEtat)) {                                // a la main, devant lui, l'ecran allume, qui tremble
            int tremble = (int) (appelT * 40) % 2;
            int px = cx + (gauche ? -10 : 6) + tremble;
            int py = haut + 22;
            rendu.rect(px, py, 5, 8, 0x14141A, 256);
            rendu.rect(px + 1, py + 1, 3, 5, (int) (appelT * 6) % 2 == 0 ? 0x9EE6B0 : 0xD8F4FF, 256);
            for (int k = 0; k < 2; k++) {                               // les ondes de la vibration
                int o = 3 + k * 3 + (int) (appelT * 8) % 2;
                rendu.rect(px - o, py + 2, 1, 4, 0xFFFFFF, 120 - k * 40);
                rendu.rect(px + 5 + o, py + 2, 1, 4, 0xFFFFFF, 120 - k * 40);
            }
            String nom = res.campagne.voix(appelAvec).nom();            // au-dessus de lui : qui appelle
            String ligne = "APPEL : " + nom;
            int l = affichage.petite().largeur(ligne, 1) + 12;
            int bx = Math.max(4, Math.min(Rendu.L - l - 4, cx - l / 2));
            rendu.rect(bx, haut - 16, l, 11, 0x14141A, 230);
            rendu.rect(bx, haut - 16, 3, 11, 0x3BB273, 256);
            affichage.petite().ecrire(rendu, ligne, bx + 7, haut - 14, Affichage.BLANC, 1);
        } else {                                                        // a l'oreille
            int px = cx + (gauche ? 3 : -6);
            rendu.rect(px, haut + 6, 3, 7, 0x14141A, 256);
            rendu.rect(px + 1, haut + 7, 1, 5, 0x5A6A80, 256);
            if (texte != null && parole.active() && appelAvec != null && appelAvec.equals(dernierQuiParle)) {
                for (int k = 0; k < 3; k++) {                           // la voix au bout du fil : de petites ondes
                    int o = 3 + k * 3;
                    int c = (int) (tempsMs / 90 + k) % 3 == 0 ? 200 : 90;
                    rendu.rect(px + (gauche ? 3 + o : -o - 1), haut + 7 + k % 2, 1, 4, 0xC8E0FF, c);
                }
            }
        }
    }

    /** "temps 32 10:00" : une ellipse dans la scene, l'horloge du quartier la suit (elle ne recule jamais). */
    private void regler(Plan plan) {
        histoire.Calendrier cal = progression.etat().calendrier();
        float ecart = cal.ecart(Integer.parseInt(plan.mot(0)), histoire.LecteurCampagne.minutes(plan.mot(1)));
        if (ecart > 0) {
            cal.passer(ecart);
        }
    }

    /** Le rang d'une etiquette, ou la fin de la scene si elle n'existe pas. */
    private int fin(int etiquette) {
        return etiquette < 0 ? scene.plans().size() : etiquette;
    }

    /**
     * "joueur <carte> <x> <y>" : ou la scene rend la main. "joueur <carte> scooter <x> <y>" : a cote du scooter, s'il est
     * gare dans cette carte (on retrouve son scooter la ou on l'a laisse) ; sinon en (x, y).
     */
    private void placer(Plan plan) {
        carteFin = plan.mot(0);
        boolean scooter = plan.mot(1).equals("scooter");
        int k = scooter ? 2 : 1;
        xFin = res.abscisse(carteFin, plan.mot(k));
        yFin = Float.parseFloat(plan.mot(k + 1));
        finPresDuScooter = scooter && progression.drapeau("scooter_gare") && carteFin.equals(progression.etat().scooterCarte());
    }

    private void suivant() {
        rang++;
    }

    /** Saute apres cette etiquette ; une etiquette absente termine la scene au lieu de la rejouer depuis le debut. */
    private void sauter(String etiquette) {
        int cible = scene.etiquette(etiquette);
        rang = cible < 0 ? scene.plans().size() : cible + 1;
    }

    /** Joue un plan. Renvoie vrai quand on peut passer au suivant (le rang a deja ete avance). */
    private boolean executer(Plan plan, float dt, Touches touches) {
        switch (plan.genre()) {
            case NOIR -> {
                if (plan.texte().isEmpty()) {
                    fondNoir = true;
                } else if (!dire(plan, dt, touches)) {
                    return false;
                }
            }
            case CARTE -> {
                fondNoir = false;
                carteDecor = plan.mot(0);
                decor = res.carte(carteDecor);
                acteurs.clear();
                trajets.clear();
                camX = cadrer(x(plan.mot(1)));
                ouverture = 0.4f;
            }
            case ACTEUR -> {
                Acteur acteur = new Acteur(plan.mot(0), res.image(plan.mot(1)), x(plan.mot(2)), Float.parseFloat(plan.mot(3)));
                acteur.pas = res.image(plan.mot(1) + "_2");
                acteur.miroir = plan.mot(4).equals("miroir");
                acteurs.put(acteur.id, acteur);
            }
            case POSE -> {
                Acteur acteur = acteur(plan.mot(0));
                if (acteur != null) {
                    acteur.texture = res.image(plan.mot(1));
                    acteur.pas = res.image(plan.mot(1) + "_2");
                }
            }
            case REGARDE -> {
                boolean gauche = plan.mot(1).equals("gauche");
                Monde.Habitant habitant = surPlace ? monde.habitant(plan.mot(0)) : null;
                if (habitant != null) {
                    habitant.sens = gauche ? -1 : 1;
                } else if (acteur(plan.mot(0)) != null) {
                    acteur(plan.mot(0)).miroir = gauche;
                }
            }
            case RETIRE -> {
                if (acteurs.remove(plan.mot(0)) == null && surPlace) {
                    Monde.Habitant habitant = monde.habitant(plan.mot(0));
                    if (habitant != null) {
                        habitant.acteur.texture = null;
                    }
                }
            }
            case MARCHE -> {
                Acteur acteur = acteur(plan.mot(0));
                if (acteur != null) {
                    trajets.add(new Trajet(acteur, acteur.x, acteur.y, x(plan.mot(1)), Float.parseFloat(plan.mot(2)),
                            Float.parseFloat(plan.mot(3))));
                }
            }
            case CAMERA -> {
                if (!surPlace) {
                    travelling = new Trajet(null, camX, 0, cadrer(x(plan.mot(0))), 0, Float.parseFloat(plan.mot(1)));
                }
            }
            case EFFET -> {
                if (plan.mot(0).equals("eclair")) {
                    eclair = 1f;
                } else {
                    effet = plan.mot(0);
                }
            }
            case JOUEUR -> placer(plan);
            case DRAPEAU -> progression.marquer(String.join(" ", plan.mots()));
            case ARGENT -> {
                encaisser(plan);
                audio.jouer("objet", 0.6f, 0, 1.2f);
            }
            case SOIN -> soigner(plan);
            case LOUE -> louer(plan);
            case PAIE -> {
                if (!progression.etat().payer(Integer.parseInt(plan.mot(0)))) {
                    audio.jouer("ui_refus", 0.6f, 0, 1);
                    sauter(plan.mot(1));
                    return true;
                }
                audio.jouer("monnaie", 0.6f, 0, 1);                    // on compte ses pieces sur le comptoir
            }
            case SON -> {
                // "son nom gain [net]" : net, le son echappe a l'etat de mixage (le detail qui reste net dans le choc)
                float gain = plan.mot(1).isEmpty() ? 1f : Float.parseFloat(plan.mot(1));
                Audio.Bus bus = plan.mot(2).equals("net") ? Audio.Bus.VOIX : Audio.busDe(plan.mot(0));
                audio.jouer(bus, plan.mot(0), gain, 0, 1, 0);
            }
            case ENCART -> {
                if (encarts != null && encarts.ouvrir(plan.mot(0), progression)) {
                    suivant();
                    return false;                                      // le jeu attend que l'encart soit lu
                }
            }
            case MOIS -> {                                             // un mois passe : compte d'un coup, montre jour apres jour
                if (mois == null) {
                    compterLeMois();
                }
                if (!mois.maj(dt, touches.tenue(java.awt.event.KeyEvent.VK_E, java.awt.event.KeyEvent.VK_ENTER,
                        java.awt.event.KeyEvent.VK_SPACE), valide(touches))) {
                    return false;
                }
                mois = null;
            }
            case ARME -> {
                progression.etat().prendreArme(plan.mot(0), monde.carte(), monde.joueur.x - 10, monde.joueur.y);
                audio.jouer("ferraille", 0.6f, 0, 1);
            }
            case COMBAT -> {                                           // la scene rend la main : on se bat
                combatDemande = plan;
                finir();
                return false;
            }
            case CONDUITE -> {                                         // la scene rend la main : on prend le volant
                conduiteDemandee = plan;
                finir();
                return false;
            }
            case TEMPS -> regler(plan);
            case LOYER -> {                                             // tout le retard de loyer est regle, d'un coup
                if (progression.etat().payerTousLesLoyers() > 0) {
                    audio.jouer("tel_paye", 0.5f, 0, 1);
                }
            }
            case DONNE, REMET -> {
                if (!transferer(plan, dt, touches)) {
                    return false;
                }
            }
            case APPEL -> {
                if (!appeler(plan, dt, touches)) {
                    return false;
                }
            }
            case MIXAGE -> audio.mixage(plan.mot(0), plan.mot(1).isEmpty() ? 1f : Float.parseFloat(plan.mot(1)));
            case MUSIQUE -> musique = plan.mot(0);
            case METEO -> meteo = plan.mot(0);
            case AMBIANCE -> {
                if (plan.mot(0).equals("aucune")) {
                    ambiancesEnPlus.clear();
                } else {
                    ambiancesEnPlus.put(plan.mot(0), new float[] {Float.parseFloat(plan.mot(1)), 0, 1});
                }
            }
            case ETIQUETTE -> { }
            case SAUT -> {
                sauter(plan.mot(0));
                return true;
            }
            case SI -> {
                if (progression.verifie(plan.mot(0))) {
                    sauter(plan.mot(1));
                    return true;
                }
            }
            case ATTEND -> {
                if (!patienter(Float.parseFloat(plan.mot(0)), dt, false, touches)) {
                    return false;
                }
            }
            case TITRE -> {
                if (!patienter(5.5f, dt, true, touches)) {
                    return false;
                }
            }
            case CARTON -> {
                if (attente == 0) {
                    audio.jouer("chapitre", 0.7f, 0, 1);               // le carton s'ouvre comme une porte, pas comme une fanfare
                }
                if (!patienter(3.4f, dt, true, touches)) {
                    return false;
                }
            }
            case GROS_PLAN -> {
                Plan precedent = rang > 0 ? scene.plans().get(rang - 1) : null;
                boolean continue_ = precedent != null && precedent.genre() == Plan.Genre.GROS_PLAN
                        && precedent.mot(0).equals(plan.mot(0)) || plan.mot(7).equals("coupe");
                if (attente == 0 && !plan.mot(1).isEmpty() && !continue_) {
                    ouverture = 0.3f;
                }
                if (plan.mot(8).startsWith("parle")) {                 // il parle : sa voix suit les lettres qui s'ecrivent
                    String dit = affichage.police().propre(plan.texte());
                    if (attente == 0) {                                // "parle:hesite" : le ton ; sans ton, il nous defie
                        String ton = plan.mot(8).contains(":") ? plan.mot(8).substring(plan.mot(8).indexOf(':') + 1) : "insolent";
                        parole.commencer("toi", ton, dit, 0f);
                    }
                    parole.suivre(dit.substring(0, Math.min(dit.length(), lettresVisibles(attente + dt))), dt);
                }
                float duree = plan.mot(1).isEmpty() ? 60f : Float.parseFloat(plan.mot(1));
                if (!patienter(duree, dt, true, touches)) {
                    return false;
                }
            }
            case ANNONCE -> {
                if (attente == 0) {
                    audio.jouer("metro_annonce", 0.8f, 0, 1);
                    parole.commencer("annonce", "neutre", plan.texte(), 0f);
                }
                if (attente > 1.1f) {                                  // apres le carillon, la voix des haut-parleurs
                    String dit = plan.texte();
                    parole.suivre(dit.substring(0, Math.min(dit.length(), Math.round((attente - 1.1f) * 30))), dt);
                }
                if (!patienter(3.0f, dt, true, touches)) {
                    return false;
                }
            }
            case MESSAGE -> {
                message(plan);
                audio.jouer("vibreur", 0.9f, 0.3f, 1);
                audio.jouer("tel_sms", 0.5f, 0.3f, 1);
            }
            case SMS -> {
                if (attente == 0) {
                    audio.jouer("vibreur", 0.9f, 0.3f, 1);
                    audio.jouer("tel_sms", 0.6f, 0.3f, 1);
                    progression.etat().recevoir(plan.mot(0), plan.texte(), java.util.List.of());   // le telephone le garde
                    progression.etat().lire(plan.mot(0));                                        // on vient de le lire a l'ecran
                }
                if (!patienter(60f, dt, true, touches)) {
                    return false;
                }
            }
            case CHOIX -> {
                return choisir(plan, touches);
            }
            default -> {
                if (!dire(plan, dt, touches)) {
                    return false;
                }
            }
        }
        suivant();
        return true;
    }

    private boolean choisir(Plan plan, Touches touches) {
        List<Plan.Option> options = plan.options();
        if (!choixOuvert) {                                            // la touche qui a fini la replique ne choisit pas
            choixOuvert = true;
            choisi = 0;
            return false;
        }
        if (touches.haut() || touches.bas()) {
            choisi = Math.floorMod(choisi + (touches.bas() ? 1 : -1), options.size());
            audio.jouer("ui_deplace", 0.7f, 0, 1);
        }
        if (valide(touches)) {
            audio.jouer("ui_valide", 0.5f, 0, 1);
            String etiquette = options.get(choisi).etiquette();
            choisi = 0;
            choixOuvert = false;
            sauter(etiquette);
            return true;
        }
        return false;
    }

    /** Une frappe de E ne sert qu'une fois par image : celle qui passe une replique ne termine pas la suivante. */
    private boolean valide(Touches touches) {
        if (frappeServie || !touches.valider()) {
            return false;
        }
        frappeServie = true;
        return true;
    }

    private float cadrer(float centre) {
        if (decor == null) {
            return 0;
        }
        float max = Math.max(decor.xMin - 12, decor.xMax + 12 - Rendu.L);
        return Math.max(decor.xMin - 12, Math.min(max, centre - Rendu.L / 2f));
    }

    private boolean patienter(float total, float dt, boolean abregeable, Touches touches) {
        attente += dt;
        if (attente >= total || (abregeable && attente > 0.6f && valide(touches))) {
            attente = 0;
            return true;
        }
        return false;
    }

    /** Un texte qui s'ecrit : la premiere pression le termine, la seconde passe a la suite. */
    private boolean dire(Plan plan, float dt, Touches touches) {
        if (texte == null && plan.genre() == Plan.Genre.DIT && presenter(plan.mot(0))) {
            return false;                                              // sa fiche d'abord : la replique attend qu'on l'ait lue
        }
        if (texte == null) {
            texte = new TexteProgressif(plan.texte(), reglages.lettresParSeconde());
            if (plan.genre() == Plan.Genre.DIT) {
                dernierQuiParle = plan.mot(0);
                float cote = heros(plan.mot(0)) ? -0.3f : 0.3f;        // la voix vient du cote de son buste
                texte.retarder(parole.commencer(plan.mot(0), plan.mot(1), plan.texte(), cote));
            }
        }
        texte.avancer(dt);
        if (plan.genre() == Plan.Genre.DIT) {
            parole.suivre(texte.visible(), dt);
        }
        if (valide(touches)) {
            parole.couper();                                           // on coupe la parole : la voix s'arrete net
            if (texte.complet()) {
                texte = null;
                audio.jouer("ui_deplace", 0.25f, 0, 1.3f);
                return true;
            }
            texte.terminer();
        }
        return false;
    }

    void dessiner(Rendu rendu, boolean pluie) {
        Plan plan = plan();
        Plan.Genre genre = plan == null ? null : plan.genre();
        if (ecranNoir()) {
            rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 256);
        } else {
            List<Acteur> enScene = new ArrayList<>(acteurs.values());
            if (surPlace) {
                monde.dessiner(rendu, enScene);
            } else {
                rendu.dessiner(decor, Math.round(camX), tempsMs, enScene);
            }
            if (pluie && Sonorisation.dehors(carteDecor)) {
                rendu.pluie(tempsMs, surPlace ? monde.camX() : camX, 1f);
            }
            switch (effet) {
                case "gris" -> rendu.gris(0.92f);
                case "irise" -> rendu.irise(tempsMs);
                case "vert" -> rendu.voile(Affichage.VERT, 0.16f + 0.05f * (float) Math.sin(tempsMs / 180.0));
                case "rouge" -> rendu.voile(0xC81E1E, 0.42f);
                case "nuit" -> rendu.fondu(0.45f);
                default -> { }
            }
            rendu.bandes(BANDES);
            dessinerAppel(rendu);
            dessinerTransfert(rendu);
        }
        if (eclair > 0) {
            rendu.voile(0xFFFFFF, eclair * 0.85f);
        }
        if (genre == Plan.Genre.TITRE) {
            titre(rendu);
        } else if (genre == Plan.Genre.CARTON) {
            String[] lignes = plan.texte().split("\\|");
            float part = Math.min(1f, attente / 0.5f);
            affichage.police().ecrireCentre(rendu, lignes[0], Rendu.L / 2, 112, Affichage.GRIS, 1);
            if (lignes.length > 1) {
                affichage.police().ecrireCentre(rendu, lignes[1], Rendu.L / 2, 128, Affichage.ORANGE, 2);
            }
            rendu.fondu(1f - part);
        } else if (genre == Plan.Genre.GROS_PLAN) {
            Texture image = res.image(plan.mot(0));
            float duree = plan.mot(1).isEmpty() ? 60f : Float.parseFloat(plan.mot(1));
            if (image != null && plan.mots().length >= 6) {
                // "gros_plan image duree zoomDebut zoomFin x y [secousse]" : la camera avance (ou recule) vers (x, y)
                float t = Math.min(1f, attente / duree);
                float doux = t * t * (3 - 2 * t);
                float zoom = Float.parseFloat(plan.mot(2)) + (Float.parseFloat(plan.mot(3)) - Float.parseFloat(plan.mot(2))) * doux;
                int sx = 0;
                int sy = 0;
                if (plan.mot(6).equals("secousse") && attente < 0.8f) {
                    float force = 3f * (1f - attente / 0.8f);
                    sx = Math.round((float) Math.sin(attente * 71) * force);
                    sy = Math.round((float) Math.cos(attente * 53) * force);
                }
                rendu.calqueZoom(image, zoom, Float.parseFloat(plan.mot(4)), Float.parseFloat(plan.mot(5)), sx, sy);
            } else if (image != null) {
                rendu.calque(image, (Rendu.L - image.largeur()) / 2, (Rendu.H - image.hauteur()) / 2);
            }
            if (plan.mot(8).startsWith("parle") && plan.mot(9).equals("buste")) {
                // "... parle:ton buste | texte" : le heros parle par-dessus l'image, son buste et sa boite a l'ecran (il
                // commente ce qu'on regarde, la fresque par exemple), au lieu des grands sous-titres
                String dit = affichage.police().propre(plan.texte());
                int visibles = Math.min(dit.length(), lettresVisibles(attente));
                affichage.buste(rendu, res.image(res.campagne.voix("toi").portrait()), true, true);
                affichage.dialogue(rendu, res.campagne.voix("toi").nom(), true, dit, visibles, visibles >= dit.length(), tempsMs);
            } else if (plan.mot(8).startsWith("parle")) {
                parler(rendu, plan.texte(), attente);
            } else if (!plan.texte().isEmpty()) {
                List<String> lignes = affichage.police().couper(affichage.police().propre(plan.texte()), Rendu.L - 40);
                for (int i = 0; i < lignes.size(); i++) {
                    affichage.police().ecrireCentre(rendu, lignes.get(i), Rendu.L / 2, Rendu.H - 12 - (lignes.size() - i) * 11,
                            Affichage.GRIS, 1);
                }
            }
            Plan avant = rang > 0 ? scene.plans().get(rang - 1) : null;
            boolean suite = avant != null && avant.genre() == Plan.Genre.GROS_PLAN && avant.mot(0).equals(plan.mot(0));
            if (!suite && !plan.mot(7).equals("coupe")) {              // le meme plan qui continue, ou une coupe franche : pas de noir
                rendu.fondu(1f - Math.min(1f, attente / 0.35f));
            }
        } else if (genre == Plan.Genre.MOIS && mois != null) {
            mois.dessiner(rendu);
        } else if (genre == Plan.Genre.SMS) {
            Campagne.Voix voix = res.campagne.voix(plan.mot(0));
            affichage.telephone(rendu, voix.nom(), plan.texte(), attente);
        } else if (genre == Plan.Genre.ANNONCE) {
            affichage.annonce(rendu, plan.texte());
        } else if (texte != null && genre == Plan.Genre.NOIR) {
            affichage.carton(rendu, texte.entier(), texte.visible().length(), 132, Affichage.BLANC);
            if (texte.complet() && tempsMs / 400 % 2 == 0) {
                Affichage.fleche(rendu, Rendu.L / 2 - 3, 190, Affichage.GRIS);
            }
        } else if (texte != null || genre == Plan.Genre.CHOIX) {
            dialogue(rendu, plan, genre);
        }
        affichage.petite().ecrire(rendu, "ECHAP : PASSER", Rendu.L - 62, 5, Affichage.SOMBRE, 1);
        if (ouverture > 0) {
            rendu.fondu(ouverture / 0.4f);
        }
    }

    /** Les bustes de ceux qui se parlent, la boite, le texte ; ou les reponses a choisir, sous le dernier qui a parle. */
    private void dialogue(Rendu rendu, Plan plan, Plan.Genre genre) {
        if (genre == Plan.Genre.RECIT) {
            affichage.dialogue(rendu, null, true, texte.entier(), texte.visible().length(), texte.complet(), tempsMs);
            return;
        }
        String parle = genre == Plan.Genre.DIT ? plan.mot(0) : null;
        if (parle != null && "ligne".equals(appelEtat) && (parle.equals(appelQui) || parle.equals(appelAvec))) {
            boolean distant = parle.equals(appelAvec);                 // la voix au bout du fil, ou celui qui telephone
            if (distant) {
                affichage.ecranAppel(rendu, res.campagne.voix(parle).nom(), appelDuree, parole.active(), tempsMs);
            } else {
                Texture b = res.image(res.campagne.voix(parle).portrait());
                affichage.buste(rendu, b, false, true);
                if (b != null) {                                        // le telephone contre son oreille, et la main qui le tient
                    int bx = Rendu.L - 14 - b.largeur() + b.largeur() * 3 / 20;
                    int by = Affichage.BOITE - b.hauteur() + b.hauteur() * 7 / 20;
                    rendu.rect(bx - 1, by - 1, 12, 26, 0xC8E0FF, 200);
                    rendu.rect(bx, by, 10, 24, 0x14141A, 256);
                    rendu.rect(bx + 2, by + 2, 6, 17, 0x2E3E5C, 256);
                    rendu.rect(bx + 3, by + 3, 2, 6, 0x9EC8FF, 256);
                    rendu.rect(bx - 3, by + 16, 15, 12, 0x7A5A48, 256);   // la main
                    rendu.rect(bx - 3, by + 16, 15, 2, 0x8E6C58, 256);
                }
            }
            affichage.dialogueAppel(rendu, res.campagne.voix(parle).nom(), distant, texte.entier(), texte.visible().length(),
                    texte.complet(), tempsMs);
            return;
        }
        boolean herosParle = parle != null && heros(parle);
        String autre = dernierQuiParle != null && !heros(dernierQuiParle) ? dernierQuiParle : null;
        if (parle != null && !herosParle) {
            autre = parle;
        }
        if (autre != null) {
            Campagne.Voix voix = res.campagne.voix(autre);
            affichage.buste(rendu, res.image(voix.portrait()), false, !herosParle && genre != Plan.Genre.CHOIX);
        }
        if (herosParle) {
            affichage.buste(rendu, res.image(res.campagne.voix(parle).portrait()), true, true);
        }
        if (genre == Plan.Genre.CHOIX) {
            List<String> options = new ArrayList<>();
            plan.options().forEach(o -> options.add(o.texte()));
            affichage.dialogue(rendu, null, true, "", 0, false, tempsMs);
            affichage.police().ecrire(rendu, "QUE RÉPONDS-TU ?", 26, Affichage.BOITE + 9, Affichage.SOMBRE, 1);
            affichage.choix(rendu, options, choisi);
            return;
        }
        Campagne.Voix voix = parle == null ? null : res.campagne.voix(parle);
        affichage.dialogue(rendu, voix == null ? null : voix.nom(), herosParle || autre == null, texte.entier(),
                texte.visible().length(), texte.complet(), tempsMs);
    }

    /** Le heros parle de lui-meme : aujourd'hui ("toi") ou dans un souvenir ("toi_jeune"). Son buste va a gauche. */
    private static boolean heros(String id) {
        return id.equals("toi") || id.startsWith("toi_");
    }

    /** L'ecran-titre : le nom du jeu apparait, orange sur noir, comme une enseigne qu'on allume. */
    private void titre(Rendu rendu) {
        float part = Math.min(1f, attente / 1.2f);
        String nom = "GET A JOB";
        int largeur = affichage.police().largeur(nom, 6);
        boolean allume = attente > 0.9f || (attente > 0.5f && tempsMs / 70 % 3 != 0);
        if (allume) {
            affichage.police().ecrire(rendu, nom, (Rendu.L - largeur) / 2 + 2, 92, 0x7A3A08, 6);
            affichage.police().ecrire(rendu, nom, (Rendu.L - largeur) / 2, 90, Affichage.ORANGE, 6);
        }
        if (attente > 1.8f) {
            affichage.police().ecrireCentre(rendu, "LE KREMLIN-BICÊTRE, AVENUE DE FONTAINEBLEAU", Rendu.L / 2, 176, Affichage.GRIS, 1);
        }
        rendu.fondu(1f - part);
    }
}
