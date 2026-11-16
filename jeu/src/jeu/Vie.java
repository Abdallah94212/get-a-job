package jeu;

import decor.Acteur;
import decor.Porte;
import decor.Scene;
import histoire.Calendrier;
import histoire.Figurant;
import histoire.Lieu;
import histoire.Progression;
import histoire.Replique;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La vie du quartier quand personne ne lui parle. Chaque habitant suit sa routine (rester a son poste, aller et venir,
 * rester assis, tenir son comptoir), suit des yeux le heros qui passe, s'ecarte quand on le bouscule, dit une phrase
 * de temps en temps. Dans l'avenue, des passants sortent des immeubles et des rues voisines, traversent, s'arretent
 * pour discuter, puis s'en vont ; ils sont plus nombreux l'apres-midi que la nuit, moins nombreux sous la pluie.
 *
 * <p>La violence se voit : celui qu'on frappe fuit, riposte, ou s'en souvient selon qui il est ; ceux qui regardent
 * crient, s'eloignent, ou viennent s'en meler. Rien de cela ne touche a l'histoire : les personnages de la campagne
 * reviennent a leur place une fois calmes.
 */
final class Vie {
    /** Ce qu'il faut faire apres un coup porte a un habitant : rien de plus, ou une bagarre avec lui. */
    enum Suite { RIEN, BAGARRE, POLICE }

    static final String[] BOUSCULE = {"EH, DOUCEMENT.", "TU REGARDES PAS OÙ TU VAS ?", "PARDON ?", "OH, ÇA VA ?", "POUSSE-TOI PAS SUR MOI."};
    static final String[] PASSANTS = {"IL PLEUT DEPUIS TROIS JOURS.", "LA TOUR, ELLE GRANDIT TOUTES LES NUITS.",
        "ENCORE UN RIDEAU BAISSÉ.", "LE MÉTRO EST EN PANNE, COMME D'HAB.", "T'AS PAS UNE CLOPE ?", "BONNE SOIRÉE.",
        "JE TE CONNAIS, TOI ? T'ES LE PETIT DU 12 ?", "FAIS GAFFE, ÇA GLISSE.", "LE KROUSTY A ENCORE AUGMENTÉ SES PRIX.",
        "J'AI PAS LE TEMPS, DÉSOLÉ."};
    static final String[][] CONVERSATIONS = {
        {"T'AS VU LE PRIX DU KEBAB ?", "SEPT EUROS. SEPT.", "LE MONDE EST FOU."},
        {"IL PARAÎT QU'ILS RACHÈTENT LE CAFÉ.", "LE PATRON DIT NON.", "POUR L'INSTANT."},
        {"TU DESCENDS SAMEDI ?", "SI JE BOSSE PAS.", "TU BOSSES TOUJOURS."},
        {"MA MÈRE VEUT DÉMÉNAGER.", "OÙ ÇA ?", "LOIN DE LA TOUR, ELLE DIT."},
        {"T'AS VU LES PETITS DE L'ÉPICERIE ?", "ILS ONT DES BASKETS NEUVES.", "ÇA VEND PAS QUE DU RIZ."},
        {"IL FAIT FROID CE SOIR.", "ON EST EN NOVEMBRE, FRÈRE.", "C'EST PAS UNE EXCUSE."}};
    static final String[] FRAGILE = {"AÏE ! AU SECOURS !", "LAISSE-MOI !", "À L'AIDE !", "ARRÊTE !"};
    static final String[] COSTAUD = {"TU TE CROIS OÙ, TOI ?", "TU VAS REGRETTER ÇA.", "VAS-Y, VIENS."};
    static final String[] POLICE = {"ON SE CALME !", "POLICE ! LES MAINS !", "STOP !"};
    static final String[] PROCHE = {"T'ES MALADE ?!", "OH ! QU'EST-CE QUI TE PREND ?", "TU FAIS QUOI, LÀ ?"};
    static final String[] TEMOIN = {"OH ! ÇA VA PAS ?", "ARRÊTEZ !", "QUELQU'UN APPELLE LA POLICE !", "LAISSE-LE !"};
    private static final String[] TETES = {"p_client", "p_cliente", "p_voyageur", "p_vieux", "p_ecouteurs", "p_sportif"};

    private final Monde monde;
    private final Ressources res;
    private final Progression progression;
    private final Random hasard = new Random(31);
    private float apparition = 1.5f;
    private int numero;

    Vie(Monde monde, Ressources res, Progression progression) {
        this.monde = monde;
        this.res = res;
        this.progression = progression;
    }

    /** Combien de passants marchent dans l'avenue a cette heure, par ce temps. */
    static int foule(Calendrier.Moment moment, boolean pluie) {
        int n = switch (moment) {
            case NUIT -> 1;
            case MATIN -> 3;
            case APRES_MIDI -> 5;
            case SOIR -> 4;
        };
        return Math.max(1, n - (pluie ? 1 : 0));
    }

    void maj(float dt, boolean pluie) {
        Scene scene = monde.scene();
        if (monde.carte().startsWith("avenue")) {
            peuplerLaRue(dt, scene, pluie);
        }
        for (Monde.Habitant h : new ArrayList<>(monde.habitants())) {
            vivre(h, dt, scene);
        }
    }

    // ------------------------------------------------------------------ les passants de l'avenue

    private void peuplerLaRue(float dt, Scene scene, boolean pluie) {
        apparition -= dt;
        long passants = monde.habitants().stream().filter(h -> h.passant).count();
        int voulu = foule(progression.etat().calendrier().moment(), pluie);
        if (apparition > 0 || passants >= voulu) {
            return;
        }
        apparition = 3f + hasard.nextFloat() * 5f;
        if (hasard.nextInt(4) == 0 && passants + 2 <= voulu) {
            discussion(scene);
        } else {
            passant(scene);
        }
    }

    private Monde.Habitant creer(float x, float y) {
        String image = TETES[hasard.nextInt(TETES.length)];
        String id = "passant_rue_" + (numero++);
        String nom = image.equals("p_cliente") || image.equals("p_ecouteurs") ? "UNE PASSANTE" : "UN PASSANT";
        Figurant f = new Figurant(id, new Lieu(monde.carte(), "", 0, y), image, nom, "0.1", "fin");
        f.repliques().add(new Replique("", PASSANTS[hasard.nextInt(PASSANTS.length)]));
        Acteur a = new Acteur(id, res.image(image), x, y);
        a.pas = res.image(image + "_2");
        Monde.Habitant h = new Monde.Habitant(f, a);
        h.passant = true;
        h.vitesse = 22 + hasard.nextFloat() * 12;
        return h;
    }

    /** Un passant entre dans le champ : par le bord de l'ecran ou en sortant d'un immeuble, et va ailleurs. */
    private void passant(Scene scene) {
        float camX = monde.camX();
        List<float[]> departs = new ArrayList<>();
        for (Porte p : scene.portes) {
            float px = (p.x1() + p.x2()) / 2f;
            if (px > camX - 40 && px < camX + decor.Rendu.L + 40) {
                departs.add(new float[] {px, 1});
            }
        }
        departs.add(new float[] {camX - 18, 0});
        departs.add(new float[] {camX + decor.Rendu.L + 18, 0});
        float[] d = departs.get(hasard.nextInt(departs.size()));
        float x = Math.max(scene.xMin + 6, Math.min(scene.xMax - 6, d[0]));
        Float y = trottoir(scene, x);
        if (y == null || Math.abs(x - monde.joueur.x) < 30) {
            return;
        }
        Monde.Habitant h = creer(x, y);
        boolean versDroite = x < monde.joueur.x ? hasard.nextInt(4) > 0 : hasard.nextInt(4) == 0;
        h.sortieX = versDroite ? Math.min(scene.xMax - 4, x + 300 + hasard.nextFloat() * 400)
                : Math.max(scene.xMin + 4, x - 300 - hasard.nextFloat() * 400);
        h.enRoute = true;
        h.cibleX = h.sortieX;
        h.cibleY = y;
        monde.habitants().add(h);
    }

    /** Deux passants arretes au bord du trottoir, qui parlent entre eux, puis repartent chacun de son cote. */
    private void discussion(Scene scene) {
        float x = monde.camX() + 60 + hasard.nextFloat() * (decor.Rendu.L - 120);
        Float y = trottoir(scene, x);
        if (y == null || scene.bloque(x + 20, y, 5) || Math.abs(x - monde.joueur.x) < 40) {
            return;
        }
        Monde.Habitant a = creer(x, y);
        Monde.Habitant b = creer(x + 20, y);
        a.partenaire = b;
        b.partenaire = a;
        a.sens = 1;
        b.sens = -1;
        a.causeReste = b.causeReste = 16 + hasard.nextFloat() * 14;
        a.sujet = b.sujet = hasard.nextInt(CONVERSATIONS.length);
        a.sortieX = Math.max(scene.xMin + 4, x - 500);
        b.sortieX = Math.min(scene.xMax - 4, x + 520);
        monde.habitants().add(a);
        monde.habitants().add(b);
    }

    /** Une hauteur libre sur le trottoir, a cette abscisse, ou null. */
    private Float trottoir(Scene scene, float x) {
        for (int k = 0; k < 6; k++) {
            float y = scene.yMin + 4 + hasard.nextFloat() * 14;
            if (!scene.bloque(x, y, 5)) {
                return y;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ un habitant, une image

    private void vivre(Monde.Habitant h, float dt, Scene scene) {
        Acteur a = h.acteur;
        h.bulleReste = Math.max(0, h.bulleReste - dt);
        if (h.bulleReste <= 0) {
            h.bulle = null;
        }
        h.bulleRepos = Math.max(0, h.bulleRepos - dt);
        h.bousculeRepos = Math.max(0, h.bousculeRepos - dt);
        h.pause = Math.max(0, h.pause - dt);
        Figurant f = h.figurant;
        if (a.texture == null) {
            return;
        }
        float dxJ = monde.joueur.x - a.x;
        float dyJ = monde.joueur.y - a.y;
        boolean pres = Math.abs(dxJ) < 64 && Math.abs(dyJ) < 30;
        if (h.reaction != null) {
            reagir(h, dt, scene);
            return;
        }
        if (f.garde() || f.amplitude() > 0) {                          // une patrouille, un garde : leur ronde (Monde)
            return;
        }
        bousculer(h, scene, dxJ, dyJ);
        if (h.partenaire != null) {
            causer(h, dt, scene);
            return;
        }
        if (h.passant) {
            if (marcher(h, h.cibleX, h.cibleY, h.vitesse, dt, scene) || Math.abs(a.x - monde.joueur.x) > 640) {
                monde.habitants().remove(h);                           // il est entre chez lui, ou il a tourne au coin
            }
            return;
        }
        String routine = f.routine();
        if (progression.attendue(histoire.Objectif.Genre.PARLER, f.id())) {   // l'histoire l'attend : il reste a sa place
            routine = "attend";
            h.enRoute = false;
        }
        if (h.enRoute) {
            float vitesse = routine.equals("comptoir") ? 14 : 19;
            if (marcher(h, h.cibleX, h.cibleY, vitesse, dt, scene)) {
                h.enRoute = false;
                h.pause = 2 + hasard.nextFloat() * 5;
            }
        } else {
            a.marche = false;
            if (h.pause <= 0 && !routine.equals("attend")) {
                if ((routine.equals("flane") || routine.equals("comptoir")) && f.rayon() > 0 && hasard.nextInt(3) > 0) {
                    h.cibleX = h.origine + (hasard.nextFloat() * 2 - 1) * f.rayon();
                    h.cibleY = h.origineY + (routine.equals("flane") ? (hasard.nextFloat() * 2 - 1) * 5 : 0);
                    h.enRoute = true;
                } else if (!routine.equals("assis")) {
                    h.sens = -h.sens;                                  // il regarde de l'autre cote
                    h.pause = 2.5f + hasard.nextFloat() * 4;
                } else {
                    h.pause = 3;
                }
            }
            if (pres) {                                                // il suit des yeux celui qui passe
                h.sens = dxJ < 0 ? -1 : 1;
            }
        }
        parlerTout(h, pres);
    }

    /** De temps en temps, quelqu'un pres du heros dit une de ses phrases, sans ouvrir de dialogue. */
    private void parlerTout(Monde.Habitant h, boolean pres) {
        if (!pres || h.bulle != null || h.bulleRepos > 0 || h.passant || hasard.nextInt(400) != 0) {
            return;
        }
        List<Replique> dispo = h.figurant.repliques().stream().filter(r -> progression.verifie(r.condition()))
                .filter(r -> r.texte().length() <= 64).toList();
        if (!dispo.isEmpty()) {
            dire(h, dispo.get(hasard.nextInt(dispo.size())).texte().toUpperCase(), 3.5f);
            h.bulleRepos = 25 + hasard.nextFloat() * 20;
        }
    }

    void dire(Monde.Habitant h, String phrase, float duree) {
        h.bulle = phrase;
        h.bulleReste = duree;
    }

    private void causer(Monde.Habitant h, float dt, Scene scene) {
        Monde.Habitant autre = h.partenaire;
        h.acteur.marche = false;
        h.sens = autre.acteur.x < h.acteur.x ? -1 : 1;
        h.causeReste -= dt;
        if (h.acteur.x < autre.acteur.x) {                             // un seul des deux mene la conversation
            String[] lignes = CONVERSATIONS[h.sujet];
            int tour = (int) ((30 - h.causeReste) / 2.8f);
            if (tour >= 0 && tour < lignes.length && h.tour != tour) {
                h.tour = tour;
                dire(tour % 2 == 0 ? h : autre, lignes[tour], 2.6f);
            }
        }
        if (h.causeReste <= 0 || !monde.habitants().contains(autre)) {   // chacun repart de son cote
            h.partenaire = null;
            h.passant = true;
            h.enRoute = true;
            h.cibleX = h.sortieX;
            h.cibleY = h.acteur.y;
            h.vitesse = 24;
        }
    }

    /** Le heros lui rentre dedans : il s'ecarte s'il le peut, et le dit. */
    private void bousculer(Monde.Habitant h, Scene scene, float dxJ, float dyJ) {
        if (Math.abs(dxJ) > 9 || Math.abs(dyJ) > 6 || !monde.joueur.enMouvement || h.bousculeRepos > 0
                || h.figurant.id().startsWith("scooter")) {
            return;
        }
        h.bousculeRepos = 4f;
        String routine = h.figurant.routine();
        if (!routine.equals("assis") && !routine.equals("comptoir")) {
            for (float ecart : new float[] {dyJ > 0 ? -10 : 10, dyJ > 0 ? 10 : -10}) {
                if (!scene.bloque(h.acteur.x, h.acteur.y + ecart, 5)) {
                    h.acteur.y += ecart;
                    break;
                }
            }
        }
        h.sens = dxJ < 0 ? -1 : 1;
        dire(h, BOUSCULE[hasard.nextInt(BOUSCULE.length)], 2.2f);
    }

    /** Marche vers un point, en contournant ce qui le gene. Renvoie vrai a l'arrivee (ou s'il ne peut pas aller plus loin). */
    private boolean marcher(Monde.Habitant h, float tx, float ty, float vitesse, float dt, Scene scene) {
        Acteur a = h.acteur;
        float dx = tx - a.x;
        float dy = ty - a.y;
        if (Math.abs(dx) < 2 && Math.abs(dy) < 2) {
            a.marche = false;
            return true;
        }
        float d = (float) Math.hypot(dx, dy);
        float pas = Math.min(d, vitesse * dt);
        float nx = a.x + dx / d * pas;
        float ny = a.y + dy / d * pas;
        if (Math.abs(nx - monde.joueur.x) < 8 && Math.abs(ny - monde.joueur.y) < 5) {   // le heros est dans le passage
            a.marche = false;
            return false;
        }
        if (!scene.bloque(nx, ny, 5)) {
            a.x = nx;
            a.y = ny;
        } else if (!scene.bloque(nx, a.y + 6, 5)) {
            a.x = nx;
            a.y += 6 * dt * 4;
        } else if (!scene.bloque(nx, a.y - 6, 5)) {
            a.x = nx;
            a.y -= 6 * dt * 4;
        } else {
            a.marche = false;
            return true;
        }
        if (Math.abs(dx) > 0.5f) {
            h.sens = dx < 0 ? -1 : 1;
        }
        a.marche = true;
        return false;
    }

    // ------------------------------------------------------------------ la violence, et ceux qui la voient

    /**
     * Le heros vient de frapper cet habitant. Il est repousse, puis reagit selon qui il est ; ceux qui regardent aussi.
     * Renvoie ce que la partie doit faire ensuite (une bagarre s'il riposte, la police si elle intervient).
     */
    Suite frapper(Monde.Habitant h, float sens) {
        Scene scene = monde.scene();
        if (!scene.bloque(h.acteur.x + sens * 8, h.acteur.y, 5)) {
            h.acteur.x += sens * 8;
        }
        h.coups++;
        h.sens = sens > 0 ? -1 : 1;
        Suite suite = switch (h.figurant.caractere()) {
            case "fragile" -> {
                dire(h, FRAGILE[hasard.nextInt(FRAGILE.length)], 2.5f);
                fuir(h);
                yield Suite.RIEN;
            }
            case "costaud" -> {
                dire(h, COSTAUD[hasard.nextInt(COSTAUD.length)], 2.5f);
                yield Suite.BAGARRE;
            }
            case "police" -> {
                dire(h, POLICE[hasard.nextInt(POLICE.length)], 2.5f);
                yield Suite.POLICE;
            }
            default -> {                                               // quelqu'un de l'histoire : il recule, il s'en souvient
                dire(h, h.coups > 1 ? "ARRÊTE ÇA TOUT DE SUITE." : PROCHE[hasard.nextInt(PROCHE.length)], 2.8f);
                h.reaction = "vexe";
                h.reactionReste = 6f;
                vexer(h);
                yield Suite.RIEN;
            }
        };
        alerter(h, h.acteur.x, h.acteur.y);
        return suite;
    }

    /** Un petit de l'equipe qu'on frappe : sa confiance en prend un coup. */
    private void vexer(Monde.Habitant h) {
        String id = h.figurant.membre();
        if (id == null || !progression.drapeau("organisation")) {
            return;
        }
        histoire.Organisation.Membre m = progression.etat().organisation().membre(id);
        if (m != null) {
            m.confiance = Math.max(0, m.confiance - 12);
        }
    }

    /** Une bagarre a lieu ici : ceux qui sont autour reagissent. Renvoie un policier qui vient s'en meler, ou null. */
    Monde.Habitant alerter(Monde.Habitant victime, float x, float y) {
        Monde.Habitant policier = null;
        for (Monde.Habitant t : new ArrayList<>(monde.habitants())) {
            if (t == victime || t.reaction != null || t.acteur.texture == null || t.figurant.id().startsWith("scooter")) {
                continue;
            }
            float d = Math.abs(t.acteur.x - x);
            String c = t.figurant.caractere();
            if (c.equals("police") && d < 260) {
                policier = t;
                dire(t, POLICE[hasard.nextInt(POLICE.length)], 2.5f);
            } else if (d < 150) {
                t.sens = x < t.acteur.x ? -1 : 1;
                if (c.equals("fragile") || t.passant && hasard.nextBoolean()) {
                    dire(t, hasard.nextBoolean() ? "!" : "AU SECOURS !", 1.6f);
                    fuir(t);
                } else {
                    dire(t, TEMOIN[hasard.nextInt(TEMOIN.length)], 2.2f);
                    t.reaction = "regarde";
                    t.reactionReste = 4f;
                }
            }
        }
        return policier;
    }

    private void fuir(Monde.Habitant h) {
        Scene scene = monde.scene();
        float loin = h.acteur.x < monde.joueur.x ? -1 : 1;
        h.reaction = "fuite";
        h.reactionReste = 5f;
        h.cibleX = Math.max(scene.xMin + 4, Math.min(scene.xMax - 4, h.acteur.x + loin * 280));
        h.cibleY = h.acteur.y;
        h.partenaire = null;
    }

    private void reagir(Monde.Habitant h, float dt, Scene scene) {
        h.reactionReste -= dt;
        switch (h.reaction) {
            case "fuite" -> {
                boolean arrive = marcher(h, h.cibleX, h.cibleY, 78, dt, scene);
                if (h.passant && (arrive || Math.abs(h.acteur.x - monde.joueur.x) > 400)) {
                    monde.habitants().remove(h);                       // il a disparu au coin de la rue
                    return;
                }
                if (arrive || h.reactionReste <= 0) {
                    revenir(h);
                }
            }
            case "vexe", "regarde" -> {
                h.acteur.marche = false;
                h.sens = monde.joueur.x < h.acteur.x ? -1 : 1;
                float recul = monde.joueur.x < h.acteur.x ? 1 : -1;
                if (h.reaction.equals("vexe") && Math.abs(monde.joueur.x - h.acteur.x) < 20
                        && !scene.bloque(h.acteur.x + recul * 20 * dt, h.acteur.y, 5) && !h.figurant.routine().equals("assis")) {
                    h.acteur.x += recul * 20 * dt;                     // il garde ses distances
                }
                if (h.reactionReste <= 0) {
                    revenir(h);
                }
            }
            case "bagarre" -> h.sens = h.acteur.miroir ? -1 : 1;    // c'est le combat qui le mene
            default -> h.reaction = null;
        }
    }

    /** La bagarre est finie. Battu, il s'en va ; vainqueur, il reste, et le fait savoir. */
    void apresBagarre(Monde.Habitant h, boolean battu) {
        h.reaction = null;
        if (battu) {
            dire(h, h.figurant.caractere().equals("police") ? "TU VAS LE REGRETTER." : "OK, OK ! C'EST BON !", 2.5f);
            fuir(h);
        } else {
            dire(h, h.figurant.caractere().equals("police") ? "CIRCULEZ." : "T'AS COMPRIS ?", 2.5f);
            revenir(h);
        }
    }

    /** Calme : il retourne a sa place (un passant reprend son chemin). */
    private void revenir(Monde.Habitant h) {
        h.reaction = null;
        if (h.passant) {
            h.enRoute = true;
            h.cibleX = h.sortieX;
            h.cibleY = h.acteur.y;
        } else if (!h.figurant.garde() && h.figurant.amplitude() <= 0) {
            h.enRoute = true;
            h.cibleX = h.origine;
            h.cibleY = h.origineY;
        }
    }
}
