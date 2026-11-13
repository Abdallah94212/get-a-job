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
        JEU, SCENE, PAUSE, BILAN
    }

    private static final class Annonce {
        final String texte;
        final int couleur;
        float reste = 3.2f;

        Annonce(String texte, int couleur) {
            this.texte = texte;
            this.couleur = couleur;
        }
    }

    private static final String[] PAUSE = {"REPRENDRE", "LA MISSION EN COURS", "OPTIONS", "COMMANDES", "MENU PRINCIPAL"};
    private static final float FONDU = 0.35f;

    private final Jeu jeu;
    private final Ressources res;
    private final Affichage affichage;
    private final Progression progression;
    private final Monde monde;
    private final Scenographe scenographe;
    private final Sonorisation sonorisation;
    private final PageOptions options;
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
        monde.aller(carte, x, y);
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

    // ------------------------------------------------------------------ le deroulement

    @Override
    public void maj(float dt, Touches touches) {
        res.suivre(progression);
        tempsMode += dt;
        fondu = Math.max(0f, fondu - dt);
        switch (mode) {
            case JEU -> jouer(dt, touches);
            case SCENE -> regarder(dt, touches);
            case PAUSE -> pause(touches);
            case BILAN -> {
                monde.attendre(dt);
                if (tempsMode > 0.6f && touches.valider()) {
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
                    scenographe.camXVue(), meteo, -1, true, dt, scenographe.ambiancesEnPlus());
        } else {
            sonorisation.suivre(monde.carte(), monde.scene(), monde.joueur.x, monde.camX(), meteo, moteur, mode != Mode.JEU, dt,
                    mode == Mode.SCENE ? scenographe.ambiancesEnPlus() : Map.of());
        }
    }

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
        commencerMission();
        if (mode != Mode.JEU) {
            return;
        }
        monde.avancer(touches.axeX(), touches.axeY(), touches.court(), dt);
        if (!monde.enScooter() && monde.joueur.enMouvement) {
            sonorisation.marcher(monde.carte(), meteo(), monde.chemin(), touches.court() && !monde.charge());
        }
        immunite = Math.max(0f, immunite - dt);
        refusReste = Math.max(0f, refusReste - dt);
        zoneReste = Math.max(0f, zoneReste - dt);
        for (int i = 0; i < Math.min(3, annonces.size()); i++) {
            annonces.get(i).reste -= dt;
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

    /** La premiere fois qu'une mission devient la mission en cours : on l'annonce et on joue son ouverture. */
    private void commencerMission() {
        Mission mission = progression.courante();
        if (mission == null || progression.debutJoue()) {
            return;
        }
        progression.marquerDebutJoue();
        annoncer("NOUVELLE MISSION : " + mission.titre(), Affichage.ORANGE);
        jeu.audio.jouer("nouvelle_mission", 0.8f, 0, 1);
        monde.peupler();
        Cinematique debut = res.campagne.cinematique(mission.cinematiqueDebut());
        if (debut != null) {
            aJouer.add(debut);
            lancerScenes();
        }
        sauvegarder();
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
        sauvegarder();
    }

    private void agir(Monde.Cible cible) {
        switch (cible.genre()) {
            case AGIR -> {
                jeu.audio.jouer("objet", 0.7f, 0, 1);
                valider(Objectif.Genre.AGIR, cible.id());
            }
            case PARLER -> {
                if (!valider(Objectif.Genre.PARLER, cible.id())) {
                    bavarder(res.campagne.figurants().get(cible.id()));
                }
            }
            default -> franchir(cible.porte());
        }
    }

    /** Un passant a qui l'histoire ne demande rien dit une de ses phrases du moment, a tour de role. */
    private void bavarder(Figurant figurant) {
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
        if (!progression.lieuOuvert(porte.cible())) {
            refus = res.campagne.verrous().get(porte.cible());
            refusReste = 4f;
            jeu.audio.jouer("ui_refus", 0.7f, 0, 1);
            return;
        }
        String depart = monde.carte();
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

    private void lancerScenes() {
        scenographe.jouer(aJouer.poll());
        passer(Mode.SCENE);
    }

    private void regarder(float dt, Touches touches) {
        monde.attendre(dt);
        scenographe.maj(dt, touches);
        if (scenographe.enCours()) {
            return;
        }
        if (scenographe.carteFin() != null) {
            monde.aller(scenographe.carteFin(), scenographe.xFin(), scenographe.yFin());
            zone = null;
        }
        if (!aJouer.isEmpty()) {
            lancerScenes();
            return;
        }
        fondu = FONDU;
        monde.peupler();
        sauvegarder();
        if (bilan != null) {
            jeu.audio.jouer("mission", 0.8f, 0, 1);
            passer(Mode.BILAN);
        } else {
            passer(Mode.JEU);
        }
    }

    private void pause(Touches touches) {
        monde.attendre(0);
        if (page == 2) {
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
                case 4 -> {
                    sauvegarder();
                    jeu.ouvrirMenu();
                }
                default -> page = choix;
            }
        }
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
            return;
        }
        monde.dessiner(rendu, List.of());
        if (pluie && monde.dehors()) {
            rendu.pluie(monde.tempsMs(), monde.camX(), 1f);
        }
        faisceaux(rendu);
        reperes(rendu);
        mission(rendu);
        if (zone != null && zoneReste > 0) {
            rendu.calque(zone.etiquette(), Rendu.L - 8 - zone.etiquette().largeur(), 8);
        }
        if (chrono >= 0) {
            int secondes = (int) Math.ceil(chrono);
            String texte = secondes / 60 + ":" + String.format("%02d", secondes % 60);
            int largeur = affichage.police().largeur(texte, 2);
            rendu.cadre(Rendu.L - 20 - largeur, 28, largeur + 12, 24);
            affichage.police().ecrire(rendu, texte, Rendu.L - 14 - largeur, 30, secondes <= 10 ? 0xE04A3A : Affichage.ORANGE, 2);
        }
        invite(rendu);
        for (int i = 0; i < annonces.size() && i < 3; i++) {
            affichage.etiquette(rendu, annonces.get(i).texte, Rendu.L / 2, 66 + i * 18, annonces.get(i).couleur);
        }
        if (fondu > 0) {
            rendu.fondu(fondu / FONDU);
        }
        if (mode == Mode.PAUSE) {
            dessinerPause(rendu);
        } else if (mode == Mode.BILAN) {
            dessinerBilan(rendu);
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

    private void mission(Rendu rendu) {
        Mission mission = progression.courante();
        String titre = mission == null ? "FIN DE L'ACTE 2" : mission.id() + "  " + mission.titre();
        String texte = mission == null ? "LA SUITE EST EN COURS D'ÉCRITURE. LE QUARTIER EST À TOI." : progression.objectif().texte();
        List<String> lignes = affichage.police().couper(affichage.police().propre(texte), 200);
        int secondaires = progression.secondairesOuverts().size();
        int hauteur = 16 + lignes.size() * 11 + (secondaires > 0 ? 9 : 0);
        rendu.cadre(8, 8, 212, hauteur);
        affichage.police().ecrire(rendu, titre, 14, 10, Affichage.ORANGE, 1);
        for (int i = 0; i < lignes.size(); i++) {
            affichage.police().ecrire(rendu, lignes.get(i), 14, 21 + i * 11, Affichage.BLANC, 1);
        }
        if (secondaires > 0) {
            affichage.petite().ecrire(rendu, "+ " + secondaires + " OBJECTIF" + (secondaires > 1 ? "S" : "")
                    + " SECONDAIRE" + (secondaires > 1 ? "S" : "") + " - ECHAP : DETAILS", 14, 23 + lignes.size() * 11, Affichage.GRIS, 1);
        }
    }

    private void invite(Rendu rendu) {
        if (refusReste > 0 && refus != null) {
            List<String> lignes = affichage.police().couper(affichage.police().propre(refus), 300);
            rendu.cadre(80, 222 - lignes.size() * 11, 320, 10 + lignes.size() * 11);
            for (int i = 0; i < lignes.size(); i++) {
                affichage.police().ecrireCentre(rendu, lignes.get(i), Rendu.L / 2, 225 - lignes.size() * 11 + i * 11, Affichage.GRIS, 1);
            }
        }
        if (mode != Mode.JEU) {
            return;
        }
        Monde.Cible cible = monde.cible();
        if (cible == null) {
            return;
        }
        if (cible.porte() == null) {
            affichage.etiquette(rendu, cible.invite(), Rendu.L / 2, Rendu.H - 24, Affichage.BLANC);
        } else if (cible.porte().etiquette() != null) {
            rendu.calqueCentre(cible.porte().etiquette(), Rendu.L / 2, Rendu.H - 22);
            if (!progression.lieuOuvert(cible.porte().cible())) {
                int x = Rendu.L / 2 + cible.porte().etiquette().largeur() / 2 + 4;
                rendu.cadre(x, Rendu.H - 22, 15, 15);
                Affichage.etat(rendu, x + 4, Rendu.H - 19, histoire.EtatMission.VERROUILLEE);
            }
        }
    }

    private void dessinerPause(Rendu rendu) {
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, 150);
        if (page == 1) {
            rendu.cadre(50, 26, 380, 218);
            Menus.ficheMission(rendu, affichage, res.campagne, progression, progression.courante(), 62, 36, 356);
            affichage.petite().ecrire(rendu, "ECHAP : RETOUR", 62, 232, Affichage.GRIS, 1);
        } else if (page == 2) {
            rendu.cadre(90, 50, 300, 170);
            options.dessiner(rendu, 104, 62);
        } else if (page == 3) {
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
}
