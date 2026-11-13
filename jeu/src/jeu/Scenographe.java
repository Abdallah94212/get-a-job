package jeu;

import decor.Acteur;
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
    private int lettresDites;
    private int choisi;
    private boolean choixOuvert;
    /** Un "noir" sans texte : la scene se joue sur fond noir jusqu'a son prochain decor (une douane qu'on ne montre pas). */
    private boolean fondNoir;
    private String dernierQuiParle;
    private long tempsMs;
    private String carteFin;
    private float xFin;
    private float yFin;
    private String musique;
    private String meteo;

    Scenographe(Ressources res, Monde monde, Progression progression, Reglages reglages, Audio audio, Affichage affichage) {
        this.res = res;
        this.monde = monde;
        this.progression = progression;
        this.reglages = reglages;
        this.audio = audio;
        this.affichage = affichage;
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
        musique = null;
        meteo = null;
        dernierQuiParle = null;
        choixOuvert = false;
        fondNoir = false;
        ouverture = surPlace ? 0 : 0.4f;
        if (surPlace) {
            monde.piloter(true);
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
        if (touches.retour() && plan() != null && plan().genre() != Plan.Genre.CHOIX) {
            passer();
            return;
        }
        while (scene != null) {
            Plan plan = plan();
            if (plan == null) {
                finir();
            } else if (!executer(plan, dt, touches)) {
                return;
            }
        }
    }

    private void finir() {
        scene = null;
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
    private void passer() {
        texte = null;
        for (int i = rang; i < scene.plans().size(); i++) {
            Plan plan = scene.plans().get(i);
            switch (plan.genre()) {
                case CARTE -> carteDecor = plan.mot(0);
                case JOUEUR -> placer(plan);
                case DRAPEAU -> progression.marquer(String.join(" ", plan.mots()));
                case CHOIX -> {
                    rang = i;
                    choisi = 0;
                    return;
                }
                case SAUT -> {
                    i = scene.etiquette(plan.mot(0));
                }
                case SI -> {
                    if (progression.verifie(plan.mot(0))) {
                        i = scene.etiquette(plan.mot(1));
                    }
                }
                default -> { }
            }
        }
        finir();
    }

    private void placer(Plan plan) {
        carteFin = plan.mot(0);
        xFin = res.abscisse(carteFin, plan.mot(1));
        yFin = Float.parseFloat(plan.mot(2));
    }

    private void suivant() {
        rang++;
    }

    private void sauter(String etiquette) {
        rang = scene.etiquette(etiquette) + 1;
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
            case SON -> audio.jouer(plan.mot(0), plan.mot(1).isEmpty() ? 1f : Float.parseFloat(plan.mot(1)), 0, 1);
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
                if (!patienter(3.4f, dt, true, touches)) {
                    return false;
                }
            }
            case GROS_PLAN -> {
                if (attente == 0 && !plan.mot(1).isEmpty()) {
                    ouverture = 0.3f;
                }
                float duree = plan.mot(1).isEmpty() ? 60f : Float.parseFloat(plan.mot(1));
                if (!patienter(duree, dt, true, touches)) {
                    return false;
                }
            }
            case ANNONCE -> {
                if (attente == 0) {
                    audio.jouer("metro_annonce", 0.8f, 0, 1);
                }
                if (!patienter(3.0f, dt, true, touches)) {
                    return false;
                }
            }
            case SMS -> {
                if (attente == 0) {
                    audio.jouer("vibreur", 0.9f, 0.3f, 1);
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
        if (touches.valider()) {
            audio.jouer("ui_valide", 0.5f, 0, 1);
            String etiquette = options.get(choisi).etiquette();
            choisi = 0;
            choixOuvert = false;
            sauter(etiquette);
            return true;
        }
        return false;
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
        if (attente >= total || (abregeable && attente > 0.6f && touches.valider())) {
            attente = 0;
            return true;
        }
        return false;
    }

    /** Un texte qui s'ecrit : la premiere pression le termine, la seconde passe a la suite. */
    private boolean dire(Plan plan, float dt, Touches touches) {
        if (texte == null) {
            texte = new TexteProgressif(plan.texte(), reglages.lettresParSeconde());
            lettresDites = 0;
            if (plan.genre() == Plan.Genre.DIT) {
                dernierQuiParle = plan.mot(0);
            }
        }
        texte.avancer(dt);
        int lettres = texte.visible().length();
        if (plan.genre() == Plan.Genre.DIT && lettres / 2 > lettresDites / 2 && !texte.complet()
                && Character.isLetterOrDigit(texte.visible().charAt(lettres - 1))) {
            audio.jouer("voix", 0.45f, 0, res.campagne.timbre(plan.mot(0)));
        }
        lettresDites = lettres;
        if (touches.valider()) {
            if (texte.complet()) {
                texte = null;
                audio.jouer("ui_deplace", 0.35f, 0, 1.3f);
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
            if (image != null) {
                rendu.calque(image, (Rendu.L - image.largeur()) / 2, (Rendu.H - image.hauteur()) / 2);
            }
            if (!plan.texte().isEmpty()) {
                List<String> lignes = affichage.police().couper(affichage.police().propre(plan.texte()), Rendu.L - 40);
                for (int i = 0; i < lignes.size(); i++) {
                    affichage.police().ecrireCentre(rendu, lignes.get(i), Rendu.L / 2, Rendu.H - 12 - (lignes.size() - i) * 11,
                            Affichage.GRIS, 1);
                }
            }
            rendu.fondu(1f - Math.min(1f, attente / 0.35f));
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
