package jeu;

import decor.Porte;
import decor.Rendu;
import decor.Zone;
import histoire.Chapitre;
import histoire.Cinematique;
import histoire.Figurant;
import histoire.Lieu;
import histoire.Mission;
import histoire.Objectif;
import histoire.Plan;
import histoire.Progression;
import histoire.Replique;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * La partie en cours : on marche dans le quartier, on parle, on agit, et l'histoire avance.
 * Cet ecran relie les systemes entre eux : le monde, la progression, les cinematiques, le son, l'interface
 * et la sauvegarde.
 */
final class EcranPartie implements Ecran {
    private enum Mode {
        JEU, SCENE, PAUSE, BILAN, TELEPHONE, TEMPS
    }

    /** Le temps qui vient de passer entre deux missions : d'ou l'on part, ou l'on arrive, et ce qui s'est passe entre-temps. */
    private record Saut(int jourAvant, int minutesAvant, int jour, int minutes, String delai, List<String> pendant) {
    }

    private Saut saut;

    private static final class Annonce {
        final String texte;
        final int couleur;
        float reste = 3.2f;
        float age;

        Annonce(String texte, int couleur) {
            this.texte = texte;
            this.couleur = couleur;
        }
    }

    private static final String[] PAUSE = {"REPRENDRE", "LA MISSION EN COURS", "TES POCHES", "OPTIONS", "COMMANDES", "MENU PRINCIPAL"};
    private static final float FONDU = 0.35f;

    private final Jeu jeu;
    private final Ressources res;
    private final Affichage affichage;
    private final Progression progression;
    private final Monde monde;
    private final Scenographe scenographe;
    private final Sonorisation sonorisation;
    private final PageOptions options;
    private final Telephone telephone;
    private final Effets effets;
    private final Combat combat;
    private final Encarts encarts;
    private final Deque<Cinematique> aJouer = new ArrayDeque<>();
    private final List<Annonce> annonces = new ArrayList<>();
    private final Map<String, Integer> bavardages = new HashMap<>();
    private Mode mode = Mode.JEU;
    private Progression.Avancee bilan;
    private float chrono = -1f;
    private float immunite;
    private float fondu = FONDU;
    private float tempsMode;
    private String refus;
    private float refusReste;
    private Zone zone;
    private float zoneReste;
    private int choix;
    private int page;
    private int argentVu = -1;
    // l'interface : ce qu'elle montre change en douceur
    private String objectifVu = "";
    private float objectifAge = 9f;
    private float santeVue = 100f;
    private float santeTrace = 100f;
    private float barresAlpha;
    private float barresReveil;
    private int ecartArgent;
    private float ecartAge = 9f;
    private float banniereAge = 9f;
    private String banniere;
    private float chocRepos;
    private float secousse;

    EcranPartie(Jeu jeu, Progression progression, String carte, float x, float y) {
        this.jeu = jeu;
        this.res = jeu.res;
        this.affichage = jeu.affichage;
        this.progression = progression;
        res.suivre(progression);
        this.monde = new Monde(res, progression);
        this.scenographe = new Scenographe(res, monde, progression, jeu.reglages, jeu.audio, affichage);
        this.sonorisation = new Sonorisation(jeu.audio, res);
        this.options = new PageOptions(jeu);
        this.telephone = new Telephone(res, progression, affichage, jeu.audio, monde);
        this.effets = new Effets(monde, jeu.audio);
        this.combat = new Combat(res, jeu.audio, effets);
        this.encarts = new Encarts(res, jeu.audio, affichage);
        scenographe.brancher(encarts, combat);
        monde.aller(carte, x, y);
        histoire.Calendrier cal = progression.etat().calendrier();
        Mission mission = progression.courante();
        if (!cal.connu()) {                                            // une partie d'avant le calendrier : il prend l'heure de la mission
            cal.regler(mission != null && mission.jour() > 0 ? mission.jour() : 1, mission != null && mission.jour() > 0 ? mission.heure() : 23 * 60);
        }
    }

    Monde monde() {
        return monde;
    }

    Progression progression() {
        return progression;
    }

    boolean enScene() {
        return mode == Mode.SCENE;
    }

    /** Vrai si une scene attend que le joueur choisisse une reponse. */
    boolean scenographeAuChoix() {
        return mode == Mode.SCENE && scenographe.auChoix();
    }

    String positionScene() {
        return mode == Mode.SCENE ? scenographe.position() : "";
    }

    boolean auTelephone() {
        return mode == Mode.TELEPHONE;
    }

    String encartCourant() {
        return encarts.courant();
    }

    boolean encartOuvert() {
        return encarts.ouvert();
    }

    Combat combat() {
        return combat;
    }

    /** Vrai quand le joueur a la main : ni scene, ni encart, ni pause, ni telephone, ni bilan. */
    boolean libre() {
        return mode == Mode.JEU && !encarts.ouvert();
    }

    boolean enPause() {
        return mode == Mode.PAUSE;
    }

    /** Vrai pendant l'ecran du temps qui passe, entre deux missions. */
    boolean auCalendrier() {
        return mode == Mode.TEMPS;
    }

    boolean auBilan() {
        return mode == Mode.BILAN;
    }

    /** Le temps qu'il fait : celui de la scene en cours, sinon celui de la mission. */
    String meteo() {
        if (mode == Mode.SCENE && scenographe.meteo() != null) {
            return scenographe.meteo();
        }
        Mission mission = progression.courante();
        return mission == null ? "pluie" : mission.meteo();
    }

    /** La musique du moment : celle de la scene, puis celle de l'objectif, de la mission dans les rues, du lieu. */
    String musique() {
        if (mode == Mode.BILAN) {
            return "mus_recit";
        }
        if (mode == Mode.SCENE) {
            if (scenographe.musique() != null) {
                return scenographe.musique();
            }
            if (scenographe.ecranNoir()) {
                return jeu.audio.musiqueCourante();
            }
        }
        Objectif objectif = progression.objectif();
        if (objectif != null && objectif.musique() != null) {
            return objectif.musique();
        }
        Mission mission = progression.courante();
        if (mission != null && mission.musique() != null && monde.dehors()) {
            return mission.musique();
        }
        return Sonorisation.musiqueDuLieu(monde.carte());
    }

    /**
     * Apres une erreur : on ferme tout ce qui peut retenir le joueur (encarts, scene, telephone, pause, combat a moitie
     * fini) et on lui rend la main la ou il est. La partie est enregistree.
     */
    @Override
    public void secourir() {
        encarts.toutFermer();
        scenographe.abandonner();
        aJouer.clear();
        if (combat.actif()) {
            combat.oublier();
        }
        monde.imposerPose(null);
        monde.piloter(false);
        page = 0;
        fondu = FONDU;
        if (bilan != null) {
            passer(Mode.BILAN);
        } else {
            passer(Mode.JEU);
        }
        monde.peupler();
        sauvegarder();
    }

    // ------------------------------------------------------------------ le deroulement

    @Override
    public void maj(float dt, Touches touches) {
        res.suivre(progression);
        if (progression.drapeau("organisation")) {                     // l'equipe se forme des que l'histoire la cree
            progression.etat().organisation().recruter(progression);
        }
        tempsMode += dt;
        fondu = Math.max(0f, fondu - dt);
        animerInterface(dt);
        if (encarts.ouvert()) {                                        // un encart : le jeu attend qu'on l'ait lu
            monde.attendre(0);
            encarts.maj(dt, touches);
            sonoriser(dt);
            return;
        }
        switch (mode) {
            case JEU -> jouer(dt, touches);
            case SCENE -> regarder(dt, touches);
            case PAUSE -> pause(touches);
            case TELEPHONE -> {
                monde.attendre(0);
                boolean range = telephone.maj(touches);
                String lancee = telephone.prendreLancee();
                if (lancee != null) {                                  // une operation vient de partir
                    jeu.audio.jouer("tel_achat", 0.5f, 0.2f, 1);
                    if (valider(Objectif.Genre.DELEGUER, lancee)) {
                        break;
                    }
                }
                if (range) {
                    jeu.audio.jouer("tel_ferme", 0.5f, 0.2f, 1);
                    jeu.audio.jouer(Audio.Bus.EFFETS, "vetement", 0.35f, 0.2f, 1.1f, 0.15f);   // on le range
                    monde.peupler();
                    sauvegarder();
                    passer(Mode.JEU);
                }
            }
            case TEMPS -> {
                monde.attendre(0);
                if (tempsMode > 1.2f && (touches.valider() || touches.retour()) || tempsMode > 12f) {
                    jeu.audio.jouer("ui_valide", 0.5f, 0, 1);
                    saut = null;
                    fondu = FONDU * 2;
                    if (!aJouer.isEmpty()) {
                        lancerScenes();
                    } else {
                        annonceEnAttente = false;
                        banniereAge = 0f;
                        jeu.audio.jouer("nouvelle_mission", 0.8f, 0, 1);
                        passer(Mode.JEU);
                    }
                }
            }
            case BILAN -> {
                monde.attendre(dt);
                if (tempsMode > 0.6f && (touches.valider() || touches.retour())) {
                    jeu.audio.jouer("ui_valide", 0.6f, 0, 1);
                    bilan = null;
                    monde.peupler();
                    passer(Mode.JEU);
                }
            }
        }
        sonoriser(dt);
    }

    /** Ce qu'on entend suit ce qu'on voit : le lieu de la scene, ou celui du joueur. */
    private void sonoriser(float dt) {
        String meteo = meteo();
        jeu.audio.musique(musique());
        if (mode == Mode.SCENE && scenographe.ecranNoir()) {
            Map<String, float[]> couches = new HashMap<>(scenographe.ambiancesEnPlus());
            if ("pluie".equals(meteo)) {
                couches.putIfAbsent("amb_pluie", new float[] {0.5f, 0, 1});
            }
            jeu.audio.ambiances(couches);
            return;
        }
        float moteur = monde.enScooter() && mode == Mode.JEU ? (monde.joueur.enMouvement ? monde.joueur.marche : 0) : -1;
        if (mode == Mode.SCENE && !scenographe.carteVue().equals(monde.carte())) {
            sonorisation.suivre(scenographe.carteVue(), scenographe.decorVu(), scenographe.camXVue() + Rendu.L / 2f,
                    scenographe.camXVue(), meteo, -1, true, dt, scenographe.ambiancesEnPlus(), !progression.etat().coupe());
        } else {
            sonorisation.suivre(monde.carte(), monde.scene(), monde.joueur.x, monde.camX(), meteo, moteur, mode != Mode.JEU, dt,
                    mode == Mode.SCENE ? scenographe.ambiancesEnPlus() : Map.of(), !progression.etat().coupe());
        }
    }

    /** La mission vient de commencer par une scene : sa banniere et son carillon attendent la fin de la scene. */
    private boolean annonceEnAttente;

    private void passer(Mode suivant) {
        mode = suivant;
        tempsMode = 0;
    }

    private void jouer(float dt, Touches touches) {
        if (touches.retour()) {
            choix = 0;
            page = 0;
            jeu.audio.jouer("ui_valide", 0.5f, 0, 0.8f);
            passer(Mode.PAUSE);
            return;
        }
        if (!combat.actif() && bilan == null) {                        // un combat ou un bilan en attente : la suite attend
            commencerMission();
        }
        if (mode != Mode.JEU) {
            return;
        }
        if (touches.telephone()) {
            telephone.ouvrir();
            jeu.audio.jouer("vetement", 0.4f, 0.2f, 1.2f);          // on le sort de la poche
            jeu.audio.jouer(Audio.Bus.INTERFACE, "tel_ouvre", 0.5f, 0.2f, 1, 0.15f);
            passer(Mode.TELEPHONE);
            return;
        }
        // le combat : les coups du heros (J, K), les ennemis, et ce qu'on ne peut pas faire en meme temps (marcher en frappant)
        String casse = monde.enScooter() ? null : combat.maj(dt, touches, monde, progression.etat());
        if (casse != null) {
            annoncer(casse, Affichage.GRIS);
        }
        frapperLesHabitants();
        temoins(dt);
        if (touches.lacher() && progression.etat().armeEnMain() != null && !combat.actif()) {
            annoncer("TU POSES " + progression.etat().armeEnMain().nom() + ". ELLE RESTE LÀ.", Affichage.GRIS);
            progression.etat().poserArme(monde.carte(), monde.joueur.x + (monde.joueur.versDroite ? 10 : -10), monde.joueur.y);
            sauvegarder();
            jeu.audio.jouer("ferraille", 0.5f, 0, 0.9f);
        }
        monde.imposerPose(monde.enScooter() ? null : combat.pose(progression.etat(), monde));
        if (combat.fini()) {
            finirCombat();
            return;
        }
        monde.meteo("pluie".equals(meteo()));
        if (combat.occupe()) {
            monde.avancer(0, 0, false, dt);
        } else {
            monde.avancer(touches.axeX(), touches.axeY(), touches.court(), dt);
        }
        if (!monde.enScooter()) {
            sonorisation.pieds(monde.carte(), meteo(), monde.joueur.enMouvement ? monde.chemin() : 0, monde.court(),
                    monde.joueur.vitesse(), monde.joueur.versDroite);
        }
        vivre(dt, touches);
        effets.maj(dt, "pluie".equals(meteo()), progression.etat());
        if (mode != Mode.JEU) {
            return;
        }
        immunite = Math.max(0f, immunite - dt);
        objectifAge += dt;
        ecartAge += dt;
        banniereAge += dt;
        barresReveil = Math.max(0f, barresReveil - dt);
        refusReste = Math.max(0f, refusReste - dt);
        zoneReste = Math.max(0f, zoneReste - dt);
        for (int i = 0; i < Math.min(3, annonces.size()); i++) {
            annonces.get(i).reste -= dt;
            annonces.get(i).age += dt;
        }
        annonces.removeIf(annonce -> annonce.reste <= 0);
        Zone ici = monde.scene().zoneEn(monde.joueur.x);
        if (ici != zone) {
            zone = ici;
            zoneReste = 3.5f;
        }
        Porte porte = monde.scene().porteEn(monde.joueur.x, monde.joueur.y);
        if (porte != null && porte.automatique()) {
            franchir(porte);
            return;
        }
        Objectif objectif = progression.objectif();
        if (objectif != null && objectif.genre() == Objectif.Genre.ALLER && objectif.cible().equals(monde.carte())) {
            valider(Objectif.Genre.ALLER, monde.carte());
            return;
        }
        String atteint = monde.pointAtteint();
        if (atteint != null) {
            valider(Objectif.Genre.ATTEINDRE, atteint);
            return;
        }
        Monde.Cible cible = monde.cible();
        if (cible != null && touches.valider()) {
            agir(cible);
            if (mode != Mode.JEU) {
                return;
            }
        }
        surveiller(dt);
    }

    /**
     * Comme dans la rue, un coup de poing ne passe pas inapercu : l'habitant touche fuit, riposte ou s'en souvient (Vie),
     * ceux qui regardent reagissent, et une riposte devient une bagarre, ou la police s'en mele.
     */
    private void frapperLesHabitants() {
        Combat.Geste g = combat.gesteActif();
        if (g == null || monde.enScooter()) {
            return;
        }
        float sens = monde.joueur.versDroite ? 1 : -1;
        for (Monde.Habitant h : List.copyOf(monde.habitants())) {
            float dx = (h.acteur.x - monde.joueur.x) * sens;
            if (h.acteur.texture == null || h.figurant.id().startsWith("scooter") || "bagarre".equals(h.reaction)
                    || dx < -4 || dx > g.portee() || Math.abs(h.acteur.y - monde.joueur.y) > 16 || !combat.atteindre(h)) {
                continue;
            }
            effets.impact(h.acteur.x - sens * 4, h.acteur.y - 26, 0xFFF0C0, 6);
            monde.trembler(1.6f);
            jeu.audio.jouer(g.son(), 0.7f, sens * 0.2f, 1f);
            Vie.Suite suite = monde.vie().frapper(h, sens);
            if (suite != Vie.Suite.RIEN) {
                engager(h, suite == Vie.Suite.POLICE ? Combat.POLICIER : Combat.PASSANT);
            }
        }
    }

    private float temoinsRepos;

    /** Pendant une bagarre, ceux qui sont autour regardent, crient, s'en vont ; un policier qui passe s'en mele. */
    private void temoins(float dt) {
        temoinsRepos -= dt;
        if (!combat.actif() || temoinsRepos > 0) {
            return;
        }
        temoinsRepos = 1.5f;
        Monde.Habitant flic = monde.vie().alerter(null, monde.joueur.x, monde.joueur.y);
        if (flic != null && combat.rue() != null) {
            engager(flic, Combat.POLICIER);
        }
    }

    /** Un habitant entre dans la bagarre (il riposte, ou la police arrive). */
    private void engager(Monde.Habitant h, Combat.Genre genre) {
        if ("bagarre".equals(h.reaction)) {
            return;
        }
        h.reaction = "bagarre";
        if (combat.actif()) {
            combat.renfort(genre, h.acteur);
            annoncer(genre == Combat.POLICIER ? "LA POLICE S'EN MÊLE." : h.figurant.nom() + " S'EN MÊLE.", ROUGE);
        } else {
            combat.bagarre(genre, h.acteur, h.figurant.id());
            annoncer(genre == Combat.POLICIER ? "LA POLICE T'ARRÊTE. DÉFENDS-TOI OU PRENDS L'AMENDE." : h.figurant.nom() + " RIPOSTE.", ROUGE);
            encarts.ouvrir("combat", progression);
        }
        jeu.audio.jouer(Audio.Bus.VOIX, "v_gars_geto_grogne", 0.5f, 0, 1.1f, 0);
    }

    /** Ce qui arrive au corps et au scooter pendant qu'on joue : les chocs, la location qui s'ecoule, la touche F. */
    private void vivre(float dt, Touches touches) {
        histoire.Etat etat = progression.etat();
        chocRepos = Math.max(0f, chocRepos - dt);
        secousse = Math.max(0f, secousse - dt);
        etat.livrer(dt);                                               // les commandes approchent
        etat.calendrier().passer(dt);                                  // une minute du quartier par seconde de jeu
        if (progression.drapeau("organisation")) {                     // l'equipe travaille pendant qu'on joue
            etat.organisation().recruter(progression);
            for (histoire.Organisation.Bilan b : etat.organisation().avancer(dt, etat)) {
                jeu.audio.jouer(b.net() >= 0 ? "tel_argent" : "tel_erreur", 0.5f, 0.3f, 1);
                valider(Objectif.Genre.ENCAISSER, b.operation());
            }
        }
        for (String avis = etat.avis(); avis != null; avis = etat.avis()) {
            annoncer(avis, Affichage.BLANC);
            if (avis.startsWith("NOUVEAU MESSAGE")) {
                jeu.audio.jouer("tel_sms", 0.5f, 0.3f, 1);
            } else if (avis.startsWith("RECETTE")) {                   // un commerce rapporte : la banque qui tinte
                jeu.audio.jouer("tel_argent", 0.5f, 0.3f, 1);
            } else if (avis.contains("COUPÉ LE COURANT")) {            // le compteur qui saute, chez soi
                jeu.audio.jouer("interrupteur", 0.5f, 0, 0.8f);
                jeu.audio.jouer("notif", 0.45f, 0.3f, 1);
            } else {
                jeu.audio.jouer("notif", 0.45f, 0.3f, 1);
            }
            jeu.audio.jouer("vibreur", 0.45f, 0.3f, 1);
        }
        if (monde.choc() && chocRepos <= 0f) {                         // le scooter a tape : on est sonne, jamais tue
            jeu.audio.jouer("choc", 0.75f, 0, 0.9f + chocRepos);
            if (!etat.possede("casque")) {
                etat.blesser(6f, 15f);
                effets.blessure(0.45f);
            } else {
                monde.trembler(4f);
            }
            chocRepos = 1.2f;
            secousse = 0.25f;
        }
        // la location ne s'ecoule que pendant qu'on roule ; a la fin, on s'arrete la ou l'on est, le scooter reste gare
        if (monde.enScooter() && etat.userLocation(dt)) {
            garer();
            progression.marquer("+location_finie");
            annoncer("LA LOCATION EST FINIE. LE SCOOTER RESTE GARÉ ICI : 5 EUROS POUR REPARTIR AVEC.", Affichage.GRIS);
            jeu.audio.jouer("ui_retour", 0.7f, 0, 1);
        }
        if (touches.scooter() && monde.enScooter()) {
            garer();
        }
        if (etat.ko() && !combat.actif()) {
            tomber();
        }
    }

    /**
     * Descend du scooter et le laisse ici : il reste a cette place, on le retrouvera au meme endroit. Le heros pose le
     * pied a cote, cote trottoir, jamais dans un mur.
     */
    private void garer() {
        float x = monde.joueur.x;
        float y = monde.joueur.y;
        progression.etat().garer(monde.carte(), x, y);
        progression.marquer("-scooter +scooter_gare");
        float[] pied = monde.placePourDescendre(x, y);
        monde.poser(pied[0], pied[1]);
        monde.peupler();
        sonorisation.garerScooter();
        sauvegarder();
    }

    /** On arrive a une porte en scooter : on le gare devant, puis on entre a pied. */
    private void garerDevantSiDedans(String destination) {
        if (progression.drapeau("scooter") && monde.dehors() && !Sonorisation.dehors(destination)) {
            progression.etat().garer(monde.carte(), monde.joueur.x, monde.joueur.y);
            progression.marquer("-scooter +scooter_gare");
            sonorisation.garerScooter();
        }
    }

    /** On remonte sur le scooter gare. Si sa location est finie, on la reprend d'abord (5 euros, payes par telephone). */
    private void remonter() {
        if (progression.drapeau("location_finie")) {
            if (!progression.etat().payer(5)) {
                refus = "LA LOCATION EST FINIE. IL TE FAUT 5 EUROS POUR LE REPRENDRE.";
                refusReste = 4f;
                jeu.audio.jouer("tel_erreur", 0.6f, 0, 1);
                return;
            }
            progression.etat().louer(180);
            progression.marquer("-location_finie");
            jeu.audio.jouer("tel_achat", 0.5f, 0, 1);
            annoncer("SCOOTER RELOUÉ : 3 MINUTES DE PLUS.", Affichage.BLANC);
        }
        float[] scooter = {progression.etat().scooterX(), progression.etat().scooterY()};
        progression.marquer("+scooter -scooter_gare");
        progression.etat().garer(null, 0, 0);
        monde.poser(scooter[0], scooter[1]);                           // on enfourche le scooter la ou il est gare
        monde.peupler();
        sonorisation.monterScooter();
        sauvegarder();
    }

    /** Une nuit chez soi : gratuite, mais il faut y revenir. */
    private void dormir() {
        jeu.audio.jouer("vetement", 0.5f, 0, 0.9f);
        jeu.audio.jouer(Audio.Bus.EFFETS, "grince", 0.3f, 0, 0.8f, 0.25f);    // le sommier
        if (progression.etat().coupe()) {                              // deux mois de retard : plus de courant, plus de nuit
            refus = "PLUS DE COURANT, PLUS DE CHAUFFAGE. LA RÉGIE ATTEND SON LOYER : PAIE-LE DEPUIS TON TÉLÉPHONE.";
            refusReste = 5f;
            jeu.audio.jouer("ui_refus", 0.7f, 0, 1);
            return;
        }
        for (String ligne : progression.laisserPasser(8 * 60)) {      // une nuit : l'equipe travaille, les colis arrivent
            annoncer(ligne, Affichage.GRIS);
        }
        progression.etat().soigner(histoire.Etat.SANTE_MAX);
        progression.etat().reposer();
        monde.peupler();
        for (histoire.Organisation.Bilan b : List.copyOf(progression.finiesPendant())) {   // les comptes rendus de la nuit
            valider(Objectif.Genre.ENCAISSER, b.operation());
        }
        jeu.audio.jouer(Audio.Bus.VOIX, "v_heros_soupir", 0.5f, 0, 1, 0.6f);   // il souffle enfin
        fondu = FONDU * 3;
        annoncer("TU AS DORMI.", Affichage.BLANC);
        sauvegarder();
    }

    /** Plus de sante : on se reveille chez soi, a moitie remis, les poches plus legeres. */
    private void tomber() {
        jeu.audio.jouer("corps_chute", 0.8f, 0, 0.9f);
        jeu.audio.jouer("coeur_vite", 0.7f, 0, 0.8f);
        int perdu = progression.etat().relever();
        if (monde.enScooter()) {                                       // le scooter reste la ou l'on est tombe : on le retrouvera
            progression.etat().garer(monde.carte(), monde.joueur.x, monde.joueur.y);
            progression.marquer("-scooter +scooter_gare");
        }
        progression.etat().calendrier().passer(4 * 60);                // quelques heures de trou
        rentrerChezSoi();
        fondu = FONDU * 3;
        zone = null;
        annonces.clear();
        annoncer(perdu > 0 ? "TU TE RÉVEILLES CHEZ TOI. IL TE MANQUE " + perdu + " EUROS." : "TU TE RÉVEILLES CHEZ TOI.", 0xE04A3A);
        sauvegarder();
    }

    /** La premiere fois qu'une mission devient la mission en cours : on l'annonce et on joue son ouverture. */
    private void commencerMission() {
        Mission mission = progression.courante();
        if (mission == null || progression.debutJoue()) {
            return;
        }
        progression.marquerDebutJoue();
        banniere = mission.id() + "|" + mission.titre();
        banniereAge = 0f;
        saut = dater(mission);
        monde.peupler();
        Cinematique debut = res.campagne.cinematique(mission.cinematiqueDebut());
        if (debut != null) {                                           // la scene d'ouverture d'abord, le carillon ensuite
            annonceEnAttente = true;
            aJouer.add(debut);
        }
        if (saut != null) {                                            // le temps a passe : on le montre avant tout
            jeu.audio.jouer("chapitre", 0.6f, 0, 1);
            passer(Mode.TEMPS);
        } else if (debut != null) {
            lancerScenes();
        } else {
            jeu.audio.jouer("nouvelle_mission", 0.8f, 0, 1);
        }
        sauvegarder();
    }

    /**
     * La mission dit quand elle se passe. Quelques minutes plus tard : l'horloge avance, on reste ou l'on est. Plus de six
     * heures : le temps passe hors de l'ecran (les operations, les colis, le scooter rendu) et l'on se reveille chez soi.
     */
    private Saut dater(Mission mission) {
        histoire.Calendrier cal = progression.etat().calendrier();
        if (mission.jour() <= 0) {
            return null;
        }
        float ecart = cal.ecart(mission.jour(), mission.heure());
        if (ecart < histoire.Calendrier.SAUT) {
            if (ecart > 0) {
                cal.passer(ecart);
            }
            return null;
        }
        int jourAvant = cal.jour();
        int minutesAvant = cal.minutes();
        List<String> pendant = progression.laisserPasser(ecart);
        cal.regler(mission.jour(), mission.heure());
        rentrerChezSoi();
        return new Saut(jourAvant, minutesAvant, mission.jour(), mission.heure(),
                histoire.Calendrier.delai(jourAvant, mission.jour(), mission.heure(), ecart), pendant);
    }

    /** On se reveille chez soi, au pied du lit. */
    private void rentrerChezSoi() {
        float[] lit = res.carte("appartement").reperes.get("lit");
        monde.aller("appartement", lit == null ? res.campagne.xDepart() : lit[0] - 30, res.campagne.yDepart());
        zone = null;
        annonces.clear();
    }

    private void surveiller(float dt) {
        Mission mission = progression.courante();
        if (mission == null || !progression.sousPression()) {
            chrono = -1f;
            return;
        }
        if (mission.chrono() > 0) {
            float avant = chrono;
            chrono = chrono < 0 ? mission.chrono() : chrono - dt;
            if (chrono <= 10 && (int) Math.ceil(chrono) != (int) Math.ceil(avant) && chrono > 0) {
                jeu.audio.jouer("ui_deplace", 0.6f, 0, 1.5f);
            }
            if (chrono <= 0) {
                echouer("TROP TARD. ON RECOMMENCE.");
                return;
            }
        }
        if (mission.furtive() && immunite <= 0 && monde.gardeQuiVoit() != null) {
            if (progression.etat().utiliser("lacrymo")) {              // l'arme sert une fois : elle evite la prise
                immunite = 5f;
                secousse = 0.2f;
                annoncer("LA LACRYMO T'A SORTI DE LÀ. ELLE EST VIDE.", Affichage.ORANGE);
                jeu.audio.jouer("ui_refus", 0.8f, 0, 1.4f);
                return;
            }
            progression.etat().coup(30f);                              // se faire prendre, c'est se faire secouer
            secousse = 0.3f;
            jeu.audio.jouer("coup", 0.8f, 0, 1);
            effets.blessure(1f);
            echouer("REPÉRÉ. TU PERDS LA MARCHANDISE.");
        }
    }

    private void echouer(String raison) {
        Mission mission = progression.courante();
        jeu.audio.jouer("ui_refus", 0.9f, 0, 1);
        annonces.clear();
        annoncer(raison, 0xE04A3A);
        progression.echouer();
        chrono = -1f;
        immunite = 2.5f;
        Lieu repli = mission.repli();
        if (repli != null) {
            float[] p = res.position(repli);
            monde.aller(repli.carte(), p[0], p[1]);
            fondu = FONDU;
        } else {
            monde.peupler();
        }
        if (progression.etat().ko()) {
            tomber();
            return;
        }
        sauvegarder();
    }

    private void agir(Monde.Cible cible) {
        switch (cible.genre()) {
            case AGIR -> {
                if (cible.id().equals("colis")) {
                    for (String objet : progression.etat().ouvrirColis()) {
                        annoncer("TU AS MAINTENANT : " + histoire.Etat.article(objet).nom(), Affichage.VERT);
                    }
                    jeu.audio.jouer("colis", 0.8f, 0, 1);
                    jeu.audio.jouer(Audio.Bus.EFFETS, "papier", 0.5f, 0, 1.2f, 0.3f);
                    monde.peupler();
                    sauvegarder();
                } else if (cible.id().equals("scooter_gare")) {
                    remonter();
                } else if (cible.id().equals(Monde.DESCENDRE)) {
                    garer();
                } else if (cible.id().startsWith(Monde.AU_SOL)) {
                    histoire.Etat.AuSol posee = monde.armeAuSol();
                    if (posee != null) {
                        progression.etat().ramasser(posee);
                        annoncer("TU RAMASSES : " + histoire.Etat.arme(posee.arme()).nom() + ".", Affichage.VERT);
                        jeu.audio.jouer("ferraille", 0.5f, 0, 1.1f);
                        sauvegarder();
                    }
                } else if (cible.id().startsWith(Monde.FOUILLE)) {
                    fouiller(cible.id().substring(Monde.FOUILLE.length()));
                } else if (cible.id().equals("dormir")) {
                    dormir();
                } else {
                    sonorisation.geste(cible.id());
                    valider(Objectif.Genre.AGIR, cible.id());
                }
            }
            case PARLER -> {
                if (!valider(Objectif.Genre.PARLER, cible.id())) {
                    Figurant f = res.campagne.figurants().get(cible.id());
                    Monde.Habitant h = monde.habitant(cible.id());
                    if (f == null && h != null) {                      // un passant de la rue : il repond en passant
                        h.sens = monde.joueur.x < h.acteur.x ? -1 : 1;
                        h.pause = 3;
                        monde.vie().dire(h, h.figurant.repliques().get(0).texte(), 3.2f);
                    } else if (f != null) {
                        bavarder(f);
                    }
                }
            }
            default -> franchir(cible.porte());
        }
    }

    /** Un passant a qui l'histoire ne demande rien dit une de ses phrases du moment, a tour de role. */
    private void bavarder(Figurant figurant) {
        Cinematique service = res.campagne.cinematique(figurant.service());
        if (service != null) {                                         // il a quelque chose a vendre ou a louer
            aJouer.add(service);
            lancerScenes();
            return;
        }
        List<Replique> dispo = figurant.repliques().stream().filter(r -> progression.verifie(r.condition())).toList();
        if (dispo.isEmpty()) {
            return;
        }
        int rang = bavardages.merge(figurant.id(), 1, Integer::sum) - 1;
        Cinematique mots = new Cinematique("bavardage");
        mots.plans().add(new Plan(Plan.Genre.DIT, new String[] {figurant.id()}, dispo.get(rang % dispo.size()).texte()));
        aJouer.add(mots);
        lancerScenes();
    }

    private void franchir(Porte porte) {
        if (combat.actif()) {
            refus = "PAS MAINTENANT.";
            refusReste = 1.5f;
            return;
        }
        for (histoire.Fermeture f : res.campagne.fermetures()) {         // une porte fermee a ce moment de l'histoire
            if (f.depuis().equals(monde.carte()) && f.vers().equals(porte.cible()) && progression.verifie(f.condition())) {
                refus = f.phrase();
                refusReste = 4f;
                jeu.audio.jouer("serrure", 0.5f, 0, 0.8f);
                return;
            }
        }
        if (!progression.lieuOuvert(porte.cible())) {
            refus = res.campagne.verrous().get(porte.cible());
            refusReste = 4f;
            jeu.audio.jouer("ui_refus", 0.7f, 0, 1);
            return;
        }
        String depart = monde.carte();
        garerDevantSiDedans(porte.cible());
        monde.aller(porte.cible(), porte.arriveeX(), porte.arriveeY());
        sonorisation.porte(depart, porte.cible());
        fondu = FONDU;
        zone = null;
        refusReste = 0;
        if (!valider(Objectif.Genre.ALLER, porte.cible())) {
            sauvegarder();
        }
    }

    /** Dit a l'histoire ce que le joueur vient de faire. Renvoie vrai si cela a fait avancer un objectif. */
    private boolean valider(Objectif.Genre genre, String cible) {
        List<String> ouvertsAvant = lieuxOuverts();
        Progression.Avancee avancee = progression.valider(genre, cible);
        if (avancee == null) {
            return false;
        }
        jeu.audio.jouer("objectif", 0.6f, 0, 1);
        annoncer(avancee.objectif().facultatif() ? "OBJECTIF SECONDAIRE ACCOMPLI" : "OBJECTIF ACCOMPLI", Affichage.VERT);
        Cinematique suite = res.campagne.cinematique(avancee.objectif().suite());
        if (suite != null) {
            aJouer.add(suite);
        }
        if (avancee.missionTerminee()) {
            progression.etat().finDeMission();                         // les commerces rapportent, le loyer tombe
            Cinematique fin = res.campagne.cinematique(avancee.mission().cinematiqueFin());
            if (fin != null) {
                aJouer.add(fin);
            }
            bilan = avancee;
            for (String lieu : lieuxOuverts()) {
                if (!ouvertsAvant.contains(lieu)) {
                    annoncer("LIEU OUVERT : " + res.campagne.nomDuLieu(lieu), Affichage.BLANC);
                }
            }
        }
        sauvegarder();
        if (!aJouer.isEmpty()) {
            lancerScenes();
        } else if (bilan != null) {
            jeu.audio.jouer("mission", 0.8f, 0, 1);
            passer(Mode.BILAN);
        } else {
            monde.peupler();
        }
        return true;
    }

    private List<String> lieuxOuverts() {
        List<String> ouverts = new ArrayList<>();
        for (String lieu : res.campagne.verrous().keySet()) {
            if (progression.lieuOuvert(lieu)) {
                ouverts.add(lieu);
            }
        }
        return ouverts;
    }

    /** Joue cette scene maintenant (les essais, les captures). */
    void jouerScene(Cinematique scene) {
        aJouer.add(scene);
        lancerScenes();
    }

    private void lancerScenes() {
        scenographe.jouer(aJouer.poll());
        passer(Mode.SCENE);
    }

    private void regarder(float dt, Touches touches) {
        monde.attendre(dt);
        scenographe.maj(dt, touches);
        if (scenographe.prendreMoisCompte()) {                        // le mois vient d'etre compte : on l'enregistre aussitot
            sauvegarder();
        }
        if (scenographe.enCours()) {
            return;
        }
        if (scenographe.carteFin() != null) {
            garerDevantSiDedans(scenographe.carteFin());
            monde.aller(scenographe.carteFin(), scenographe.xFin(), scenographe.yFin());
            zone = null;
        }
        if (!aJouer.isEmpty()) {
            lancerScenes();
            return;
        }
        Plan demande = scenographe.prendreCombat();
        if (demande != null) {                                         // la scene laisse la main : il faut se battre
            Combat.Genre genre = demande.mot(0).equals("alien") ? Combat.ALIEN : Combat.GUETTEUR;
            combat.commencer(genre, Integer.parseInt(demande.mot(1)), monde.joueur.x, monde.joueur.y, demande.mot(2), demande.mot(3),
                    monde.scene());
            passer(Mode.JEU);
            annoncer(genre == Combat.ALIEN ? "ILS T'ENCERCLENT." : "IL NE TE LAISSERA PAS PASSER.", ROUGE);
            encarts.ouvrir("combat", progression);
            return;
        }
        fondu = FONDU;
        monde.peupler();
        sauvegarder();
        if (annonceEnAttente && bilan == null) {                       // la mission s'affiche quand on reprend la main
            annonceEnAttente = false;
            banniereAge = 0f;
            jeu.audio.jouer("nouvelle_mission", 0.8f, 0, 1);
        }
        if (bilan != null) {
            jeu.audio.jouer("mission", 0.8f, 0, 1);
            passer(Mode.BILAN);
        } else {
            passer(Mode.JEU);
        }
    }

    private void pause(Touches touches) {
        monde.attendre(0);
        if (page == 3) {
            if (options.maj(touches)) {
                page = 0;
            }
            return;
        }
        if (page != 0) {
            if (touches.retour() || touches.valider()) {
                jeu.audio.jouer("ui_retour", 0.6f, 0, 1);
                page = 0;
            }
            return;
        }
        if (touches.retour()) {
            jeu.audio.jouer("ui_retour", 0.6f, 0, 1);
            passer(Mode.JEU);
            return;
        }
        if (touches.haut() || touches.bas()) {
            choix = Math.floorMod(choix + (touches.bas() ? 1 : -1), PAUSE.length);
            jeu.audio.jouer("ui_deplace", 0.7f, 0, 1);
        }
        if (touches.valider()) {
            jeu.audio.jouer("ui_valide", 0.6f, 0, 1);
            switch (choix) {
                case 0 -> passer(Mode.JEU);
                case 5 -> {
                    sauvegarder();
                    jeu.ouvrirMenu();
                }
                default -> page = choix;
            }
        }
    }

    /** Le combat est fini : la scene de victoire ou de defaite. Battu, on se releve a peine (on ne meurt pas). */
    private void finirCombat() {
        if (combat.rue() != null) {
            finirBagarre();
            return;
        }
        boolean gagne = combat.gagne();
        String suite = gagne ? combat.sceneVictoire() : combat.sceneDefaite();
        combat.oublier();
        monde.imposerPose(null);
        progression.marquer(gagne ? "+combat_gagne" : "-combat_gagne");
        if (!gagne) {
            progression.etat().soigner(30);
        }
        Cinematique scene = res.campagne.cinematique(suite);
        if (scene != null) {
            aJouer.add(scene);
            lancerScenes();
        }
        sauvegarder();
    }

    /** Une bagarre de rue se termine : pas de scene, mais des consequences (une amende si la police gagne). */
    private void finirBagarre() {
        boolean gagne = combat.gagne();
        boolean police = combat.ennemis().stream().anyMatch(e -> e.genre == Combat.POLICIER);
        combat.oublier();
        monde.imposerPose(null);
        for (Monde.Habitant h : monde.habitants()) {
            if ("bagarre".equals(h.reaction)) {
                monde.vie().apresBagarre(h, gagne);
            }
        }
        histoire.Etat etat = progression.etat();
        if (gagne) {
            annoncer(police ? "TU T'ES DÉGAGÉ. LA POLICE VA TE CHERCHER." : "IL N'INSISTE PAS.", Affichage.GRIS);
        } else {
            etat.soigner(30);
            if (police) {
                int amende = Math.min(etat.argent(), 90);
                etat.payer(amende, "AMENDE : TROUBLE À L'ORDRE PUBLIC");
                annoncer("INTERPELLÉ. AMENDE : " + amende + " EUROS.", ROUGE);
                jeu.audio.jouer("tel_erreur", 0.6f, 0, 1);
            } else {
                annoncer("TU T'ES FAIT CORRIGER.", ROUGE);
            }
        }
        sauvegarder();
    }

    private static final String[] RIEN = {"RIEN. DES ÉPLUCHURES ET UN PARAPLUIE CASSÉ.", "RIEN, À PART L'ODEUR.",
        "DES CARTONS MOUILLÉS. RIEN D'UTILE.", "UN VIEUX JOURNAL. LA KROUSTY EN UNE.", "RIEN. QUELQU'UN EST PASSÉ AVANT TOI."};

    /**
     * On fouille une poubelle. Pas de recompense a chaque fois : souvent rien, parfois quelques pieces, parfois de quoi
     * se defendre. Ce qu'elle contient est tire au sort une fois par mission (la meme poubelle, la meme mission : le
     * meme contenu). Une poubelle marquee "arme" sur la carte en cache toujours une.
     */
    private void fouiller(String repere) {
        histoire.Etat etat = progression.etat();
        etat.fouiller(repere);
        jeu.audio.jouer("froissement", 0.6f, 0, 0.9f);
        jeu.audio.jouer(Audio.Bus.EFFETS, "ferraille", 0.35f, 0, 1.1f, 0.25f);
        encarts.ouvrir("fouille", progression);
        String mission = progression.courante() == null ? "" : progression.courante().id();
        java.util.Random tirage = new java.util.Random(java.util.Objects.hash(repere, mission, etat.argent() / 50));
        float d = tirage.nextFloat();
        String trouve = null;
        if (repere.contains("arme")) {
            trouve = d < 0.5f ? "barre" : d < 0.8f ? "tuyau" : "planche";
        } else if (d < 0.36f) {
            annoncer(RIEN[tirage.nextInt(RIEN.length)], Affichage.GRIS);
        } else if (d < 0.56f) {
            int pieces = 1 + tirage.nextInt(4);
            etat.gagner(pieces, "TROUVÉ DANS UNE POUBELLE");
            annoncer(pieces + (pieces > 1 ? " EUROS" : " EURO") + " AU FOND D'UN SAC.", Affichage.BLANC);
        } else {
            float k = tirage.nextFloat();
            trouve = k < 0.32f ? "bouteille" : k < 0.6f ? "planche" : k < 0.85f ? "tuyau" : "barre";
        }
        if (trouve != null) {
            histoire.Etat.Arme arme = histoire.Etat.arme(trouve);
            histoire.Etat.Arme ancienne = etat.prendreArme(trouve);
            jeu.audio.jouer(Audio.Bus.EFFETS, trouve.equals("bouteille") ? "tinte" : "ferraille", 0.6f, 0, 1, 0.5f);
            annoncer("TU PRENDS : " + arme.nom() + (ancienne != null ? ". TU LAISSES " + ancienne.nom() + "." : "."), Affichage.VERT);
            encarts.ouvrir("armes", progression);                      // apres celui de la fouille, s'il vient de s'ouvrir
        }
        sauvegarder();
    }

    /** Tes poches : les objets de l'histoire, ce qu'on a achete, l'arme en main. */
    private void poches(Rendu rendu) {
        rendu.cadre(70, 34, 340, 206);
        affichage.police().ecrire(rendu, "TES POCHES", 84, 44, Affichage.ORANGE, 2);
        int y = 76;
        String[][] recits = {{"beeper", "beeper", "LE BEEPER VERT DU SDF"}, {"boitier_orange", "flipper", "LE BOÎTIER ORANGE"},
            {"eclat_irise", "eclat", "UN ÉCLAT IRISÉ"}};
        for (String[] o : recits) {
            if (progression.drapeau(o[0])) {
                decor.Texture t = res.image(o[1]);
                if (t != null) {
                    rendu.calqueEchelle(t, 86, y - 1, Math.max(1, 10 / Math.max(t.largeur(), t.hauteur())));
                }
                affichage.police().ecrire(rendu, o[2], 104, y, Affichage.BLANC, 1);
                y += 14;
            }
        }
        for (String objet : progression.etat().objets()) {
            histoire.Etat.Article art = histoire.Etat.article(objet);
            affichage.police().ecrire(rendu, art == null ? objet.toUpperCase() : art.nom(), 104, y, Affichage.GRIS, 1);
            y += 14;
        }
        histoire.Etat.Arme arme = progression.etat().armeEnMain();
        y += 6;
        affichage.petite().ecrire(rendu, "EN MAIN", 86, y, Affichage.SOMBRE, 1);
        affichage.police().ecrire(rendu, arme == null ? "TES POINGS. J : POING, K : PIED." : arme.nom() + " — " + arme.phrase(), 86,
                y + 10, Affichage.BLANC, 1);
        if (y == 82 && progression.etat().objets().isEmpty()) {
            affichage.police().ecrire(rendu, "RIEN QUI COMPTE.", 104, 76, Affichage.SOMBRE, 1);
        }
        affichage.petite().ecrire(rendu, "ECHAP : RETOUR", 84, 226, Affichage.GRIS, 1);
    }

    private void annoncer(String texte, int couleur) {
        annonces.add(new Annonce(texte, couleur));
    }

    void sauvegarder() {
        try {
            jeu.sauvegarde.enregistrer(progression, monde.carte(), monde.joueur.x, monde.joueur.y);
        } catch (IOException erreur) {
            annoncer("SAUVEGARDE IMPOSSIBLE", 0xE04A3A);
            System.err.println("Sauvegarde impossible : " + erreur.getMessage());
        }
    }

    // ------------------------------------------------------------------ l'affichage

    @Override
    public void dessiner(Rendu rendu) {
        boolean pluie = "pluie".equals(meteo());
        if (mode == Mode.SCENE) {
            scenographe.dessiner(rendu, pluie);
            encarts.dessiner(rendu);                                   // un encart ouvert par la scene doit se voir : c'est lui qu'on attend
            return;
        }
        monde.dessiner(rendu, combat.acteurs());
        for (histoire.Etat.AuSol a : progression.etat().auSol()) {      // les armes posees restent ou on les a laissees
            if (a.carte().equals(monde.carte())) {
                histoire.Etat.Arme arme = histoire.Etat.arme(a.arme());
                float x = a.x() - monde.camX();
                float l = arme.longueur() / 2f;
                rendu.ligne(x - l, a.y() - 1, x + l, a.y() - 3, arme.id().equals("bouteille") ? 3 : 2, arme.couleur());
            }
        }
        combat.dessiner(rendu, monde, progression.etat());
        if (pluie && monde.dehors()) {
            rendu.pluie(monde.tempsMs(), monde.camX(), 1f);
        }
        effets.dessinerMonde(rendu, pluie && monde.dehors());
        bulles(rendu);
        effets.dessinerVoiles(rendu, progression.etat(), monde.tempsMs());
        faisceaux(rendu);
        reperes(rendu);
        boolean libre = mode == Mode.JEU;                              // pause, telephone, bilan : l'interface se tait
        objectif(rendu, libre);
        etat(rendu);
        if (zone != null && zoneReste > 0) {
            rendu.calque(zone.etiquette(), Rendu.L - 10 - zone.etiquette().largeur(), 10);
        }
        if (chrono >= 0) {
            int secondes = (int) Math.ceil(chrono);
            String texte = secondes / 60 + ":" + String.format("%02d", secondes % 60);
            int largeur = affichage.police().largeur(texte, 2);
            ombre(rendu, texte, Rendu.L / 2 - largeur / 2, 12, secondes <= 10 ? 0xE04A3A : Affichage.BLANC, 2);
        }
        if (libre) {
            invite(rendu);
            toasts(rendu);
            banniere(rendu);
        }
        effets.dessinerInterface(rendu);
        if (fondu > 0) {
            rendu.fondu(fondu / FONDU);
        }
        if (mode == Mode.TELEPHONE) {
            telephone.dessiner(rendu);
        } else if (mode == Mode.PAUSE) {
            dessinerPause(rendu);
        } else if (mode == Mode.BILAN) {
            dessinerBilan(rendu);
        } else if (mode == Mode.TEMPS && saut != null) {
            dessinerTemps(rendu);
        }
        encarts.dessiner(rendu);
    }

    // ------------------------------------------------------------------ l'interface du jeu
    // Regle : on ne montre que ce qui sert maintenant. L'objectif en haut a gauche, les jauges en bas a gauche quand
    // elles bougent, les notifications en haut a droite, l'action possible en bas au centre. Pas de cadres pleins :
    // du texte ombre, des voiles degrades, des icones de quelques pixels.

    /** Ce que disent les habitants, au-dessus de leur tete : une ou deux lignes, sans cadre plein. */
    private void bulles(Rendu rendu) {
        for (Monde.Habitant h : monde.habitants()) {
            if (h.bulle == null || h.acteur.texture == null) {
                continue;
            }
            List<String> lignes = affichage.petite().couper(affichage.petite().propre(h.bulle), 120);
            int largeur = 0;
            for (String l : lignes) {
                largeur = Math.max(largeur, affichage.petite().largeur(l, 1));
            }
            int cx = Math.round(h.acteur.x - monde.camX());
            if (cx < -8 || cx > Rendu.L + 8) {
                continue;                                              // il parle hors de l'ecran : on ne l'entend pas d'ici
            }
            int bas = Math.round(h.acteur.y) - h.acteur.texture.hauteur() - 5;
            int hauteur = lignes.size() * 8 + 3;
            int x = Math.max(2, Math.min(Rendu.L - largeur - 6, cx - largeur / 2 - 2));
            int y = Math.max(2, bas - hauteur);
            float alpha = Math.min(1f, h.bulleReste / 0.3f);
            rendu.rect(x, y, largeur + 5, hauteur, 0x0C0C12, Math.round(190 * alpha));
            rendu.rect(Math.max(x + 2, Math.min(x + largeur, cx - 1)), y + hauteur, 3, 2, 0x0C0C12, Math.round(190 * alpha));
            for (int k = 0; k < lignes.size(); k++) {
                affichage.petite().ecrire(rendu, lignes.get(k), x + 3, y + 2 + k * 8, Affichage.BLANC, 1);
            }
        }
    }

    private static final String[] COEUR = {".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."};
    private static final String[] VENT = {"XXXX...", "....X..", "XXXXX..", ".....X.", "XXXXXX.", "......."};
    private static final String[] PIECE = {".XXXX.", "XX..XX", "X.XX.X", "X.XX.X", "XX..XX", ".XXXX."};
    private static final String[] PHONE = {"XXXXX", "X...X", "X...X", "X...X", "X...X", "XXXXX", "XX.XX"};
    private static final String[] HORLOGE = {".XXX.", "X.X.X", "X.XXX", "X...X", ".XXX."};
    private static final String[] COCHE = {"......X", ".....XX", "X...XX.", "XX.XX..", ".XXX...", "..X...."};
    private static final int ROUGE = 0xE04A3A;

    private static float doux(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private static void icone(Rendu rendu, String[] motif, int x, int y, int rvb, int opacite) {
        for (int j = 0; j < motif.length; j++) {
            for (int i = 0; i < motif[j].length(); i++) {
                if (motif[j].charAt(i) == 'X') {
                    rendu.rect(x + i, y + j, 1, 1, rvb, opacite);
                }
            }
        }
    }

    /** Du texte avec une ombre d'un pixel : lisible sur n'importe quel decor, sans cadre. */
    private int ombre(Rendu rendu, String texte, int x, int y, int rvb, int echelle) {
        return affichage.police().ecrireOmbre(rendu, texte, x, y, rvb, echelle);
    }

    private int ombrePetite(Rendu rendu, String texte, int x, int y, int rvb) {
        return affichage.petite().ecrireOmbre(rendu, texte, x, y, rvb, 1);
    }

    /** Un voile sombre qui s'efface vers la droite : il porte le texte sans l'enfermer. */
    private static void voile(Rendu rendu, int x, int y, int largeur, int hauteur, int opacite) {
        for (int i = 0; i < largeur; i += 4) {
            float k = 1f - (float) i / largeur;
            rendu.rect(x + i, y, Math.min(4, largeur - i), hauteur, 0x08080C, Math.round(opacite * k * k));
        }
    }

    /** En haut a gauche : la mission en petit, l'objectif en clair. Il glisse en place quand il change. */
    private void objectif(Rendu rendu, boolean libre) {
        Mission mission = progression.courante();
        String texte = Casse.phrase(mission == null ? "LE QUARTIER EST À TOI." : progression.objectif().texte());
        if (!texte.equals(objectifVu)) {
            objectifVu = texte;
            objectifAge = 0f;
        }
        String titre = mission == null ? "FIN DE L'ACTE 2" : mission.id() + "  " + mission.titre();
        List<String> lignes = affichage.police().couper(affichage.police().propre(texte), 214);
        float e = doux(objectifAge / 0.45f);
        int x = 12 + Math.round(-18 * (1 - e));
        int y = 12;
        int hauteur = 13 + lignes.size() * 12 + 4;
        int secondaires = progression.secondairesOuverts().size();
        voile(rendu, 0, y - 6, 300, hauteur + (secondaires > 0 ? 10 : 0) + 8, libre ? 215 : 160);
        rendu.rect(x, y + 12, 2, lignes.size() * 12 - 1, Affichage.ORANGE, 256);              // le trait de l'objectif
        if (objectifAge < 1.2f) {                                                              // il vient de changer : un eclat
            rendu.rect(x, y + 12, 2, lignes.size() * 12 - 1, 0xFFFFFF, Math.round(200 * (1 - objectifAge / 1.2f)));
        }
        ombrePetite(rendu, affichage.petite().propre(titre), x, y, Affichage.GRIS);
        for (int i = 0; i < lignes.size(); i++) {
            ombre(rendu, lignes.get(i), x + 7, y + 13 + i * 12, Affichage.BLANC, 1);
        }
        if (secondaires > 0) {
            int yy = y + 13 + lignes.size() * 12 + 2;
            rendu.rect(x + 1, yy + 2, 3, 3, Affichage.GRIS, 256);
            ombrePetite(rendu, secondaires + (secondaires > 1 ? " objectifs secondaires" : " objectif secondaire"), x + 7, yy,
                    Affichage.GRIS);
        }
    }

    /** L'interface suit le jeu en douceur : la barre de vie rattrape la sante, la trace des degats s'efface, les jauges
     *  apparaissent quand elles bougent et se retirent quand tout va bien. */
    private void animerInterface(float dt) {
        histoire.Etat etat = progression.etat();
        if (argentVu >= 0 && etat.argent() != argentVu && mode == Mode.JEU) {
            ecartArgent = etat.argent() - argentVu;
            ecartAge = 0f;
            barresReveil = 3f;
            if (ecartArgent > 0) {                                     // l'argent rentre : on l'entend, il brille
                jeu.audio.jouer("argent", 0.6f, -0.3f, 1);
                effets.pieces(14, Rendu.H - 18);
            } else {
                jeu.audio.jouer("piece", 0.45f, -0.3f, 0.8f);
            }
        }
        if (mode == Mode.JEU || argentVu < 0) {
            argentVu = etat.argent();
        }
        float sante = etat.sante();
        if (Math.abs(sante - santeVue) > 0.5f) {
            barresReveil = 3f;
        }
        santeVue += (sante - santeVue) * Math.min(1f, dt * 8f);
        santeTrace = santeTrace < santeVue ? santeVue : Math.max(santeVue, santeTrace - 12f * dt);
        boolean utiles = etat.blesse() || etat.endurance() < etat.enduranceMax() - 0.5f || etat.essouffle()
                || barresReveil > 0 || etat.location() > 0;
        barresAlpha += ((utiles ? 1f : 0f) - barresAlpha) * Math.min(1f, dt * 5f);
    }

    /** En bas a gauche : la vie et le souffle, fins, qui se cachent quand tout va bien ; l'argent, toujours. */
    private void etat(Rendu rendu) {
        histoire.Etat etat = progression.etat();
        int a = Math.round(256 * barresAlpha);
        int x = 12;
        int y = Rendu.H - 16;
        // l'argent : toujours la
        icone(rendu, PIECE, x, y, Affichage.ORANGE, 256);
        rendu.rect(x + 2, y + 2, 2, 2, 0xFFD27A, 256);
        int finArgent = x + 10 + ombre(rendu, etat.argent() + " EUROS", x + 10, y, Affichage.BLANC, 1);
        int finEcart = finArgent;
        if (ecartAge < 1.6f && ecartArgent != 0) {
            float k = doux(ecartAge / 1.6f);
            finEcart = finArgent + 6 + ombre(rendu, (ecartArgent > 0 ? "+" : "-") + Math.abs(ecartArgent), finArgent + 6,
                    y - Math.round(8 * k), ecartArgent > 0 ? Affichage.VERT : ROUGE, 1);
        }
        if (etat.nonLus() > 0 || !etat.colis().isEmpty()) {            // le telephone a quelque chose : apres le gain, jamais dessous
            int px = Math.max(finArgent + 34, finEcart + 10);
            icone(rendu, PHONE, px, y - 1, Affichage.GRIS, 256);
            rendu.rect(px + 4, y - 3, 4, 4, Affichage.ORANGE, 256);
            ombrePetite(rendu, etat.nonLus() > 0 ? "P" : "COLIS CHEZ TOI", px + 9, y + 1, Affichage.ORANGE);
        }
        histoire.Etat.Arme arme = etat.armeEnMain();
        if (arme != null) {                                            // l'arme en main, au-dessus des jauges
            int ay = y - (a < 8 ? 12 : 34);
            rendu.ligne(x, ay + 6, x + 7, ay - 1, 2, arme.couleur());
            int fin = x + 12 + ombrePetite(rendu, arme.nom(), x + 12, ay, Affichage.BLANC);
            int reste = etat.coupsRestants();
            ombrePetite(rendu, reste < 0 ? "J : FRAPPER   G : LÂCHER" : reste + (reste > 1 ? " COUPS" : " COUP") + "   J : FRAPPER", fin + 8, ay,
                    Affichage.GRIS);
        }
        if (combat.actif()) {
            barresReveil = 1f;                                         // en combat, la sante reste sous les yeux
        }
        if (a < 8) {
            return;
        }
        boolean clignote = etat.blesse() && monde.tempsMs() / 400 % 2 == 0;
        jauge(rendu, COEUR, x, y - 22, santeVue / 100f, santeTrace / 100f, 1f, etat.blesse() ? ROUGE : 0xD8504A, a,
                clignote ? Affichage.BLANC : 0xD8504A);
        jauge(rendu, VENT, x, y - 12, etat.endurance() / 100f, 0f, etat.enduranceMax() / 100f,
                etat.essouffle() ? Affichage.SOMBRE : 0x8FE0A8, a, etat.essouffle() ? Affichage.SOMBRE : 0x8FE0A8);
        if (etat.location() > 0) {
            int s = (int) Math.ceil(etat.location());
            int lx = x + 90;
            icone(rendu, HORLOGE, lx, y - 12, s <= 20 ? ROUGE : Affichage.GRIS, a);
            ombrePetite(rendu, "SCOOTER " + s / 60 + ":" + String.format("%02d", s % 60), lx + 8, y - 12,
                    s <= 20 && monde.tempsMs() / 300 % 2 == 0 ? ROUGE : Affichage.GRIS);
        }
    }

    /** Une jauge fine, decoupee en quatre crans ; la part perdue recemment reste claire un instant ; la part qu'on ne
     *  peut plus remplir (blesse) est hachuree. */
    private void jauge(Rendu rendu, String[] icone, int x, int y, float part, float trace, float plafond, int couleur, int a,
                       int couleurIcone) {
        int largeur = 72;
        icone(rendu, icone, x, y - 1, couleurIcone, a);
        int bx = x + 10;
        rendu.rect(bx - 1, y, largeur + 2, 5, 0x000000, a * 160 >> 8);
        int plein = Math.round(largeur * Math.max(0f, Math.min(1f, part)));
        int trait = Math.round(largeur * Math.max(0f, Math.min(1f, trace)));
        if (trait > plein) {
            rendu.rect(bx + plein, y + 1, trait - plein, 3, 0xF0E0D0, a * 200 >> 8);
        }
        rendu.rect(bx, y + 1, plein, 3, couleur, a);
        rendu.rect(bx, y + 1, plein, 1, 0xFFFFFF, a * 70 >> 8);                               // un reflet en haut
        int limite = Math.round(largeur * plafond);
        for (int i = limite; i < largeur; i += 2) {
            rendu.rect(bx + i, y + 1, 1, 3, 0x5A2A30, a);
        }
        for (int k = 1; k < 4; k++) {
            rendu.rect(bx + largeur * k / 4, y + 1, 1, 3, 0x000000, a * 120 >> 8);
        }
    }

    /** En haut a droite : les nouvelles, qui glissent, restent un peu, puis repartent. */
    private void toasts(Rendu rendu) {
        int y = zone != null && zoneReste > 0 ? 30 : 12;
        for (int i = 0; i < annonces.size() && i < 3; i++) {
            Annonce annonce = annonces.get(i);
            List<String> lignes = affichage.police().couper(affichage.police().propre(Casse.phrase(annonce.texte)), 170);
            int largeur = 8 + lignes.stream().mapToInt(l -> affichage.police().largeur(l, 1)).max().orElse(0) + 14;
            int hauteur = 8 + lignes.size() * 12;
            float entree = doux(annonce.age / 0.3f);
            float sortie = annonce.reste < 0.35f ? 1f - annonce.reste / 0.35f : 0f;
            int x = Rendu.L - 10 - largeur + Math.round((1 - entree) * 40 + sortie * 60);
            rendu.rect(x, y, largeur, hauteur, 0x0C0C12, 190);
            rendu.rect(x, y, 2, hauteur, annonce.couleur, 256);
            boolean message = annonce.texte.startsWith("NOUVEAU MESSAGE");
            boolean fait = annonce.texte.contains("ACCOMPLI");
            int tx = x + 7;
            if (message) {
                icone(rendu, PHONE, tx, y + 3, annonce.couleur, 256);
                tx += 8;
            } else if (fait) {
                icone(rendu, COCHE, tx, y + 3, Affichage.VERT, 256);
                tx += 9;
            }
            for (int k = 0; k < lignes.size(); k++) {
                affichage.police().ecrire(rendu, lignes.get(k), tx, y + 4 + k * 12, Affichage.BLANC, 1);
            }
            y += hauteur + 4;
        }
    }

    /** Une nouvelle mission : son titre au centre, entre deux traits, puis il s'efface. */
    private void banniere(Rendu rendu) {
        if (banniere == null || banniereAge > 3.6f) {
            return;
        }
        String[] parts = banniere.split("\\|", 2);
        float entree = doux(banniereAge / 0.5f);
        float sortie = banniereAge > 3.0f ? (banniereAge - 3.0f) / 0.6f : 0f;
        int a = Math.round(256 * entree * (1 - sortie));
        int y = 74;
        int demi = Math.round(110 * entree);
        rendu.rect(0, y - 6, Rendu.L, 34, 0x000000, a * 110 >> 8);
        rendu.rect(Rendu.L / 2 - demi, y - 3, demi * 2, 1, Affichage.ORANGE, a);
        rendu.rect(Rendu.L / 2 - demi, y + 25, demi * 2, 1, Affichage.ORANGE, a);
        if (a > 90) {
            String haut = "MISSION " + parts[0];
            affichage.petite().ecrire(rendu, haut, Rendu.L / 2 - affichage.petite().largeur(haut, 1) / 2, y, Affichage.GRIS, 1);
            String titre = affichage.police().propre(parts[1]);
            ombre(rendu, titre, Rendu.L / 2 - affichage.police().largeur(titre, 1), y + 9, Affichage.ORANGE, 2);
        }
    }

    private void faisceaux(Rendu rendu) {
        Mission mission = progression.courante();
        if (mission == null || !mission.furtive() || !progression.sousPression()) {
            return;
        }
        for (Monde.Habitant habitant : monde.habitants()) {
            if (habitant.figurant.garde()) {
                int x = Math.round(habitant.acteur.x - monde.camX());
                int y = Math.round(habitant.acteur.y);
                rendu.rect(habitant.sens > 0 ? x : x - 92, y - 16, 92, 34, 0xFFF0C0, immunite > 0 ? 18 : 44);
            }
        }
    }

    private void reperes(Rendu rendu) {
        Mission mission = progression.courante();
        if (mission == null) {
            return;
        }
        for (Objectif facultatif : progression.secondairesOuverts()) {
            if (monde.carte().equals(res.campagne.carteDe(facultatif))) {
                repere(rendu, monde.direction(facultatif), Affichage.GRIS, false);
            }
        }
        repere(rendu, monde.direction(progression.objectif()), Affichage.ORANGE, true);
    }

    private void repere(Rendu rendu, float[] ou, int couleur, boolean auBord) {
        if (ou == null) {
            return;
        }
        int x = Math.round(ou[0] - monde.camX());
        int saut = (int) (monde.tempsMs() / 300 % 2);
        if (x >= 6 && x <= Rendu.L - 6) {
            Affichage.fleche(rendu, x - 3, Math.max(60, Math.round(ou[1])) - 9 + saut, couleur);
        } else if (auBord) {
            Affichage.flecheCote(rendu, x < 6 ? 8 + saut : Rendu.L - 9 - saut, 150, x > 6, couleur);
        }
    }

    /** En bas au centre : ce que fait la touche E ici. Une touche dessinee, puis l'action, sans cadre. */
    private void invite(Rendu rendu) {
        if (refusReste > 0 && refus != null) {
            List<String> lignes = affichage.police().couper(affichage.police().propre(Casse.phrase(refus)), 300);
            int y = Rendu.H - 54 - lignes.size() * 11;
            rendu.rect(70, y - 5, 340, lignes.size() * 11 + 8, 0x08080C, 170);
            for (int i = 0; i < lignes.size(); i++) {
                String l = lignes.get(i);
                ombre(rendu, l, Rendu.L / 2 - affichage.police().largeur(l, 1) / 2, y + i * 12, Affichage.BLANC, 1);
            }
        }
        Monde.Cible cible = monde.cible();
        if (cible == null) {
            return;
        }
        if (cible.porte() != null) {
            if (cible.porte().etiquette() != null) {
                rendu.calqueCentre(cible.porte().etiquette(), Rendu.L / 2, Rendu.H - 26);
                if (!progression.lieuOuvert(cible.porte().cible())) {
                    int x = Rendu.L / 2 + cible.porte().etiquette().largeur() / 2 + 4;
                    rendu.cadre(x, Rendu.H - 26, 15, 15);
                    Affichage.etat(rendu, x + 4, Rendu.H - 23, histoire.EtatMission.VERROUILLEE);
                }
            }
            return;
        }
        String action = cible.invite().startsWith("E : ") ? cible.invite().substring(4) : cible.invite();
        action = affichage.police().propre(Casse.phrase(action));
        int largeur = 14 + affichage.police().largeur(action, 1);
        int x = Rendu.L / 2 - largeur / 2;
        int y = Rendu.H - 24;
        rendu.rect(x - 7, y - 6, largeur + 16, 17, 0x08080C, 190);
        int saut = monde.tempsMs() / 500 % 2 == 0 ? 0 : 1;
        rendu.rect(x, y - 2 + saut, 10, 11, 0x000000, 160);                                  // la touche, en relief
        rendu.rect(x, y - 3 + saut, 10, 10, Affichage.BLANC, 256);
        rendu.rect(x + 1, y + 6 + saut, 8, 1, Affichage.GRIS, 256);
        affichage.police().ecrire(rendu, "E", x + 3, y - 1 + saut, Affichage.ENCRE, 1);
        ombre(rendu, action, x + 14, y - 1, Affichage.BLANC, 1);
    }

    private void dessinerPause(Rendu rendu) {
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 150);
        if (page == 1) {
            rendu.cadre(50, 26, 380, 218);
            Menus.ficheMission(rendu, affichage, res.campagne, progression, progression.courante(), 62, 36, 356);
            affichage.petite().ecrire(rendu, "ECHAP : RETOUR", 62, 232, Affichage.GRIS, 1);
        } else if (page == 2) {
            poches(rendu);
        } else if (page == 3) {
            rendu.cadre(90, 34, 300, 206);
            options.dessiner(rendu, 104, 44);
        } else if (page == 4) {
            rendu.cadre(70, 40, 340, 190);
            Menus.commandes(rendu, affichage, 84, 52);
        } else {
            rendu.cadre(150, 70, 180, 130);
            affichage.police().ecrireCentre(rendu, "PAUSE", Rendu.L / 2, 78, Affichage.ORANGE, 2);
            for (int i = 0; i < PAUSE.length; i++) {
                Menus.ligne(rendu, affichage, PAUSE[i], 172, 110 + i * 16, i == choix, true);
            }
        }
    }

    /** L'ecran de fin de mission : ce qu'on vient de faire, et ce que cela debloque. */
    private void dessinerBilan(Rendu rendu) {
        Mission faite = bilan.mission();
        Mission suivante = bilan.suivante();
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 170);
        rendu.cadre(70, 34, 340, 202);
        affichage.police().ecrireCentre(rendu, "MISSION TERMINÉE", Rendu.L / 2, 42, Affichage.VERT, 2);
        affichage.police().ecrireCentre(rendu, faite.id() + "  " + faite.titre(), Rendu.L / 2, 70, Affichage.BLANC, 1);
        int y = 90;
        Affichage.etat(rendu, 96, y + 2, histoire.EtatMission.TERMINEE);
        affichage.police().ecrire(rendu, "OBJECTIFS : " + faite.etapes().size() + " SUR " + faite.etapes().size(), 110, y, Affichage.GRIS, 1);
        if (!faite.facultatifs().isEmpty()) {
            y += 13;
            affichage.police().ecrire(rendu, "SECONDAIRES : " + bilan.facultatifsFaits() + " SUR " + faite.facultatifs().size(),
                    110, y, Affichage.GRIS, 1);
        }
        rendu.rect(96, y + 20, 288, 1, Affichage.SOMBRE, 256);
        y += 28;
        if (suivante == null) {
            affichage.police().ecrireCentre(rendu, "FIN DE LA PARTIE JOUABLE", Rendu.L / 2, y, Affichage.ORANGE, 1);
            affichage.police().ecrireCentre(rendu, "LES ACTES 3 À 7 SONT EN COURS D'ÉCRITURE.", Rendu.L / 2, y + 16, Affichage.GRIS, 1);
        } else {
            Chapitre avant = res.campagne.chapitreDe(faite);
            Chapitre apres = res.campagne.chapitreDe(suivante);
            if (avant != apres) {
                affichage.police().ecrireCentre(rendu, "NOUVEAU CHAPITRE : " + apres.titre() + " - " + apres.sousTitre(),
                        Rendu.L / 2, y, Affichage.ORANGE, 1);
                y += 16;
            }
            affichage.police().ecrire(rendu, "NOUVELLE MISSION DÉBLOQUÉE", 96, y, Affichage.GRIS, 1);
            Affichage.etat(rendu, 96, y + 16, histoire.EtatMission.DISPONIBLE);
            affichage.police().ecrire(rendu, suivante.id() + "  " + suivante.titre(), 110, y + 14, Affichage.BLANC, 1);
            affichage.police().ecrire(rendu, suivante.lieu(), 110, y + 27, Affichage.GRIS, 1);
            int ligne = y + 44;
            for (String lieu : suivante.ouvre()) {
                affichage.police().ecrire(rendu, "LIEU OUVERT : " + res.campagne.nomDuLieu(lieu), 96, ligne, Affichage.VERT, 1);
                ligne += 13;
            }
        }
        if (tempsMode > 0.6f) {
            affichage.police().ecrireCentre(rendu, "E : CONTINUER", Rendu.L / 2, 218, Affichage.BLANC, 1);
        }
    }

    /**
     * Le temps qui passe : l'horloge tourne de l'ancienne heure a la nouvelle, les jours defilent, puis ce qui est arrive
     * pendant ce temps, ligne par ligne. Rien n'est invente : ce sont les vrais mouvements.
     */
    private void dessinerTemps(Rendu rendu) {
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x07070C, 256);
        float t = doux(tempsMode / 1.6f);
        float avant = saut.jourAvant() * 1440f + saut.minutesAvant();
        float apres = saut.jour() * 1440f + saut.minutes();
        float vu = avant + (apres - avant) * t;
        int jourVu = (int) (vu / 1440f);
        int minutesVues = Math.round(vu - jourVu * 1440f) % 1440;
        int cx = Rendu.L / 2;
        affichage.petite().ecrire(rendu, saut.delai(), cx - affichage.petite().largeur(saut.delai(), 1) / 2, 46, Affichage.ORANGE, 1);
        String jour = histoire.Calendrier.nomDuJour(jourVu);
        affichage.police().ecrireCentre(rendu, jour, cx, 62, Affichage.BLANC, 3);
        affichage.police().ecrireCentre(rendu, histoire.Calendrier.horloge(minutesVues), cx, 96, Affichage.GRIS, 2);
        int semaine = 7;
        for (int k = 0; k < semaine; k++) {                             // la semaine : le jour qui s'allume
            int jk = jourVu - 3 + k;
            int x = cx - semaine * 15 / 2 + k * 15;
            boolean ici = k == 3;
            rendu.rect(x, 122, 12, 3, ici ? Affichage.ORANGE : 0x2A2A36, 256);
            String initiale = histoire.Calendrier.nomDuJour(jk).substring(0, 1);
            affichage.petite().ecrire(rendu, initiale, x + 4, 128, ici ? Affichage.BLANC : Affichage.SOMBRE, 1);
        }
        int y = 150;
        if (!saut.pendant().isEmpty()) {
            affichage.petite().ecrire(rendu, "PENDANT CE TEMPS", cx - affichage.petite().largeur("PENDANT CE TEMPS", 1) / 2, y,
                    Affichage.SOMBRE, 1);
            y += 12;
        }
        for (int i = 0; i < saut.pendant().size(); i++) {
            float apparition = doux((tempsMode - 1.4f - i * 0.35f) / 0.3f);
            if (apparition <= 0) {
                break;
            }
            String ligne = saut.pendant().get(i);
            int l = affichage.police().largeur(ligne, 1);
            int couleur = ligne.contains("+ ") ? Affichage.VERT : ligne.startsWith("LOYER") ? ROUGE : Affichage.BLANC;
            affichage.police().ecrire(rendu, ligne, cx - l / 2, y + Math.round((1 - apparition) * 6), couleur, 1);
            y += 13;
        }
        if (tempsMode > 1.2f) {
            affichage.petite().ecrire(rendu, "E : CONTINUER", cx - affichage.petite().largeur("E : CONTINUER", 1) / 2, 248,
                    Affichage.GRIS, 1);
        }
    }
}
