package jeu;

import decor.Rendu;
import histoire.Etat;
import histoire.Figurant;
import histoire.Organisation;
import histoire.Progression;
import java.util.ArrayList;
import java.util.List;

/**
 * Le telephone du heros. Tout ce qu'il fait a une suite dans le monde : une commande devient un colis chez soi,
 * un commerce achete rapporte a chaque mission, une reponse a un message change l'histoire, un appel a Youngssef
 * fait venir le scooter. Il ne garde rien lui-meme : il montre et modifie l'etat du heros (histoire.Etat).
 */
final class Telephone {
    private enum Ecran {
        ACCUEIL("TÉLÉPHONE"), MESSAGES("MESSAGES"), FIL(""), BANQUE("BANQUE"), OPERATIONS("OPÉRATIONS"), LANCER(""),
        EQUIPE("ÉQUIPE"), ANNONCES("ANNONCES"), COMMANDES("COMMANDES"), CONTACTS("CONTACTS");

        final String nom;

        Ecran(String nom) {
            this.nom = nom;
        }
    }
    private static final int X = 162, Y = 20, L = 156, H = 230;
    private static final int FOND = 0x14141C, BARRE = 0x22222E, ROUGE = 0xE04A3A;
    /** Les gens dont on a le numero, et la mission a partir de laquelle on les connait. */
    private static final String[][] CARNET = {{"youngssef", "1.2"}, {"geto", "1.2"}, {"diallo", "1.2"}, {"omar", "1.2"}, {"gero", "1.4"}};

    private final Ressources res;
    private final Progression progression;
    private final Affichage affichage;
    private final Audio audio;
    private final Monde monde;
    private Ecran ecran = Ecran.ACCUEIL;
    private static final int COLONNES = 3;
    /** Les transitions : l'ecran precedent, et l'avancee du glissement vers le nouveau (1 : arrive). */
    private Ecran ecranVu = Ecran.ACCUEIL;
    private float glisse = 1f;
    private float sortie;
    private int choix;
    private String contact;
    private String info = "";
    /** L'operation qu'on prepare (ecran LANCER), le petit choisi et la paie proposee. */
    private Organisation.Operation preparee;
    private Organisation.Paie paie = Organisation.Paie.JUSTE;
    /** L'operation qu'on vient de lancer, pour que la partie le sache (un objectif peut l'attendre) ; lue une fois. */
    private String lancee;

    Telephone(Ressources res, Progression progression, Affichage affichage, Audio audio, Monde monde) {
        this.res = res;
        this.progression = progression;
        this.affichage = affichage;
        this.audio = audio;
        this.monde = monde;
    }

    void ouvrir() {
        if (progression.drapeau("organisation")) {
            orga().recruter(progression);
        }
        ecran = Ecran.ACCUEIL;
        ecranVu = ecran;
        choix = 0;
        info = "";
        sortie = 0f;                                                    // il sort de la poche
    }

    private Etat etat() {
        return progression.etat();
    }

    /** Le nom affiche d'un correspondant. */
    String nom(String id) {
        Figurant figurant = res.campagne.figurants().get(id);
        if (figurant != null) {
            return figurant.nom();
        }
        try {
            return res.campagne.voix(id).nom();
        } catch (RuntimeException inconnu) {
            return id.toUpperCase().replace('_', ' ');
        }
    }

    /** Ceux qu'on a rencontres, ou qui nous ont ecrit. */
    List<String> contacts() {
        List<String> contacts = new ArrayList<>();
        for (String[] fiche : CARNET) {
            if (res.campagne.mission(fiche[1]) != null && progression.rang() >= res.campagne.rang(fiche[1])) {
                contacts.add(fiche[0]);
            }
        }
        for (String de : etat().fils()) {
            if (!contacts.contains(de)) {
                contacts.add(de);
            }
        }
        return contacts;
    }

    private List<Etat.Commerce> annonces() {
        return Etat.COMMERCES.stream().filter(c -> progression.verifie(c.condition())).toList();
    }

    /** Les applis de l'ecran d'accueil : les operations et l'equipe n'apparaissent qu'une fois l'equipe formee. */
    private List<Ecran> applis() {
        List<Ecran> a = new ArrayList<>(List.of(Ecran.MESSAGES, Ecran.BANQUE));
        if (orga().active()) {
            a.add(Ecran.OPERATIONS);
            a.add(Ecran.EQUIPE);
        }
        a.addAll(List.of(Ecran.ANNONCES, Ecran.COMMANDES, Ecran.CONTACTS));
        return a;
    }

    private Organisation orga() {
        return etat().organisation();
    }

    /** L'operation lancee depuis la derniere fois qu'on a demande, ou null. */
    String prendreLancee() {
        String l = lancee;
        lancee = null;
        return l;
    }

    private int lignes() {
        return switch (ecran) {
            case ACCUEIL -> applis().size();
            case OPERATIONS -> orga().disponibles(progression).size();
            case LANCER -> orga().membres().size();
            case EQUIPE -> orga().membres().size();
            case MESSAGES -> etat().fils().size();
            case FIL -> etat().reponses(contact).size();
            case BANQUE -> 1;
            case ANNONCES -> annonces().size();
            case COMMANDES -> Etat.ARTICLES.size();
            case CONTACTS -> contacts().size();
        };
    }

    /** Une image : les touches. Renvoie vrai quand le joueur range le telephone. */
    boolean maj(Touches touches) {
        boolean range = majTouches(touches);
        if (ecran != ecranVu) {                                         // un nouvel ecran : il glisse depuis la droite
            ecranVu = ecran;
            glisse = 0f;
        }
        glisse = Math.min(1f, glisse + 1f / 9f);                       // les transitions avancent a chaque image
        sortie = Math.min(1f, sortie + 1f / 8f);
        return range;
    }

    private boolean majTouches(Touches touches) {
        if (touches.retour() || touches.telephone()) {
            if (ecran != Ecran.ACCUEIL && !touches.telephone()) {
                audio.jouer("tel_tic", 0.45f, 0, 0.8f);                // on revient d'un ecran : le meme tic, plus grave
            }
            if (ecran == Ecran.ACCUEIL || touches.telephone()) {
                return true;
            }
            ecran = ecran == Ecran.FIL ? Ecran.MESSAGES : ecran == Ecran.LANCER ? Ecran.OPERATIONS : Ecran.ACCUEIL;
            choix = 0;
            info = "";
            return false;
        }
        int n = lignes();
        if (ecran == Ecran.ACCUEIL && n > 0) {                         // la grille : gauche, droite d'une case ; haut, bas d'une rangee
            int avant = choix;
            if (touches.gauche() || touches.droite()) {
                choix = Math.floorMod(choix + (touches.droite() ? 1 : -1), n);
            } else if (touches.haut() || touches.bas()) {
                int c = choix + (touches.bas() ? COLONNES : -COLONNES);
                choix = c < 0 ? Math.min(n - 1, choix + (n - 1) / COLONNES * COLONNES) : c >= n ? choix % COLONNES : c;
            }
            if (choix != avant) {
                audio.jouer("tel_tic", 0.4f, 0, 1.05f);
            }
        } else if (n > 0 && (touches.haut() || touches.bas())) {
            choix = Math.floorMod(choix + (touches.bas() ? 1 : -1), n);
            audio.jouer("tel_tic", 0.4f, 0, 1.05f);
        }
        choix = n == 0 ? 0 : Math.min(choix, n - 1);
        if (ecran == Ecran.LANCER && (touches.gauche() || touches.droite())) {    // la paie : radin, juste, avec prime
            Organisation.Paie[] p = Organisation.Paie.values();
            paie = p[Math.max(0, Math.min(p.length - 1, paie.ordinal() + (touches.droite() ? 1 : -1)))];
            audio.jouer("tel_tic", 0.4f, 0, 1.2f);
        }
        return touches.valider() && n > 0 && valider();
    }

    /** E sur la ligne choisie. Renvoie vrai si le telephone se range (le scooter arrive). */
    private boolean valider() {
        boolean fait = true;
        boolean ranger = false;
        switch (ecran) {
            case ACCUEIL -> {
                ecran = applis().get(choix);
                choix = 0;
                info = "";
            }
            case OPERATIONS -> {
                preparee = orga().disponibles(progression).get(choix);
                paie = Organisation.Paie.JUSTE;
                ecran = Ecran.LANCER;
                choix = meilleur(preparee);
                info = "";
            }
            case LANCER -> {
                Organisation.Membre m = orga().membres().get(choix);
                String refus = orga().lancer(preparee.id(), m.id, paie, etat());
                fait = refus == null;
                if (fait) {
                    lancee = preparee.id();
                    ecran = Ecran.OPERATIONS;
                    choix = 0;
                    info = m.nom + " EST PARTI : " + preparee.nom() + ".";
                } else {
                    info = refus;
                }
            }
            case EQUIPE -> {                                           // sa fiche mene aux operations, pour lui en confier une
                ecran = Ecran.OPERATIONS;
                choix = 0;
            }
            case MESSAGES -> {
                contact = etat().fils().get(choix);
                etat().lire(contact);
                ecran = Ecran.FIL;
                choix = 0;
            }
            case FIL -> {
                String drapeaux = etat().repondre(contact, choix);
                if (!drapeaux.isEmpty()) {
                    progression.marquer(drapeaux);
                }
                choix = 0;
            }
            case BANQUE -> {
                fait = etat().payerLoyer();
                info = fait ? "LOYER PAYÉ." : etat().loyerDu() == 0 ? "RIEN À PAYER." : "IL TE MANQUE DE L'ARGENT.";
            }
            case ANNONCES -> {
                Etat.Commerce commerce = annonces().get(choix);
                fait = etat().acheter(commerce);
                info = fait ? "C'EST SIGNÉ." : etat().commerces().contains(commerce.id()) ? "C'EST DÉJÀ À TOI." : "IL TE MANQUE DE L'ARGENT.";
            }
            case COMMANDES -> {
                Etat.Article article = Etat.ARTICLES.get(choix);
                fait = etat().commander(article.id());
                info = fait ? "COMMANDÉ. LIVRAISON CHEZ TOI." : etat().dejaPris(article.id()) ? "DÉJÀ PRIS." : "IL TE MANQUE DE L'ARGENT.";
            }
            case CONTACTS -> {
                String qui = contacts().get(choix);
                fait = false;
                if (qui.equals("youngssef")) {
                    ranger = fait = appelerLeScooter();
                } else {
                    info = "IL NE RÉPOND PAS.";
                }
            }
        }
        audio.jouer(son(fait), 0.55f, 0, 1);
        return ranger;
    }

    /** Le son d'une action du telephone : chaque appli repond a sa facon, toujours dans la meme famille de sons vitres. */
    /** Le petit le mieux place pour cette operation : libre, de ce talent, le plus fiable. */
    private int meilleur(Organisation.Operation op) {
        List<Organisation.Membre> ms = orga().membres();
        int mieux = 0;
        int score = Integer.MIN_VALUE;
        for (int i = 0; i < ms.size(); i++) {
            int s = orga().chance(op, ms.get(i), Organisation.Paie.JUSTE) + (ms.get(i).libre() ? 100 : 0);
            if (s > score) {
                score = s;
                mieux = i;
            }
        }
        return mieux;
    }

    private String son(boolean fait) {
        if (!fait) {
            return "tel_erreur";
        }
        return switch (ecran) {
            case BANQUE -> "tel_paye";
            case ANNONCES, COMMANDES -> "tel_achat";
            case FIL -> "tel_sms";
            default -> "tel_tic";
        };
    }

    /** Youngssef fait porter son scooter la ou l'on est : plus cher qu'au banc, mais on ne marche pas. */
    private boolean appelerLeScooter() {
        Figurant youngssef = res.campagne.figurants().get("youngssef");
        if (youngssef == null || !progression.present(youngssef)) {
            info = "IL NE RÉPOND PAS.";
        } else if (!monde.dehors()) {
            info = "SORS D'ABORD DANS LA RUE.";
        } else if (progression.drapeau("scooter") || progression.drapeau("scooter_gare") || progression.drapeau("pret_scooter")) {
            info = "TU L'AS DÉJÀ.";
        } else if (!etat().payer(8, "SCOOTER LIVRÉ")) {
            info = "HUIT EUROS. TU NE LES AS PAS.";
        } else {
            etat().ecrire("youngssef", "Amène-moi le scooter.");
            etat().recevoir("youngssef", "Un petit te le dépose. Huit euros, trois minutes.", List.of());
            etat().lire("youngssef");
            etat().louer(180f);
            progression.marquer("+scooter -scooter_gare");
            return true;
        }
        return false;
    }


    // ------------------------------------------------------------------ l'affichage
    // Un vrai telephone : la barre d'etat (l'heure du quartier), un ecran d'accueil fait d'icones, des applis qui
    // glissent en place. Peu de texte : des listes, des cartes, des jauges, des montants colores. Le detail
    // n'apparait que sur la ligne choisie.

    private static final int SURFACE = 0x1C1C26, TRAIT = 0x2A2A36;
    private static final int[] TEINTES = {0x3BB273, 0xD9A441, 0xE07B39, 0x8E6CD8, 0x2E9CCA, 0xC2563A, 0x4A7BD8};
    private static final String[][] GLYPHES = {
        {"XXXXXXX", "X.....X", "X.X.X.X", "X.....X", "XXXXXXX", "..XX...", ".X....."},      // messages
        {"..XXXX.", ".X....X", "XXXX...", ".X.....", "XXXX...", ".X....X", "..XXXX."},      // banque
        {"...X...", "..XXX..", ".X.X.X.", "...X...", "XXXXXXX", "X.....X", "XXXXXXX"},      // operations
        {".X...X.", "XXX.XXX", ".X...X.", "XXX.XXX", "XXX.XXX", "X.X.X.X", "X.X.X.X"},      // equipe
        {"XXXXXXX", "X.X.X.X", "XXXXXXX", "X.....X", "X.XX..X", "X.XX..X", "XXXXXXX"},      // annonces
        {"..XXX..", ".X.X.X.", "XXXXXXX", "X..X..X", "X..X..X", "X..X..X", "XXXXXXX"},      // commandes
        {"..XXX..", "..XXX..", "..XXX..", "...X...", ".XXXXX.", "XXXXXXX", "XXXXXXX"}};     // contacts

    private static int rang(Ecran e) {
        return switch (e) {
            case MESSAGES, FIL -> 0;
            case BANQUE -> 1;
            case OPERATIONS, LANCER -> 2;
            case EQUIPE -> 3;
            case ANNONCES -> 4;
            case COMMANDES -> 5;
            default -> 6;
        };
    }

    private static String court(Ecran e) {
        return switch (e) {
            case MESSAGES -> "MESSAGES";
            case BANQUE -> "BANQUE";
            case OPERATIONS -> "AFFAIRES";
            case EQUIPE -> "ÉQUIPE";
            case ANNONCES -> "ANNONCES";
            case COMMANDES -> "BOUTIQUE";
            default -> "CONTACTS";
        };
    }

    void dessiner(Rendu rendu) {
        float monte = 1 - (1 - sortie) * (1 - sortie) * (1 - sortie);
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x000000, Math.round(130 * monte));
        int dy = Math.round((1 - monte) * 60);
        int y0 = Y + dy;
        rendu.rect(X - 7, y0 - 12, L + 14, H + 24, 0x06060A, 256);       // la coque
        rendu.rect(X - 6, y0 - 11, L + 12, H + 22, 0x2A2A36, 256);
        rendu.rect(X - 5, y0 - 10, L + 10, H + 20, 0x0C0C12, 256);
        rendu.rect(X + L / 2 - 14, y0 - 7, 28, 3, 0x2A2A36, 256);
        rendu.rect(X, y0, L, H, FOND, 256);
        barreDEtat(rendu, y0);
        float k = 1 - (1 - glisse) * (1 - glisse);
        int decale = Math.round((1 - k) * 24);
        int haut = y0 + 12;
        if (ecran != Ecran.ACCUEIL) {
            haut = entete(rendu, y0 + 12, decale);
        }
        int y = haut + 4;
        switch (ecran) {
            case ACCUEIL -> accueil(rendu, y0 + 12);
            case MESSAGES -> messages(rendu, y, decale);
            case FIL -> fil(rendu, y, decale);
            case BANQUE -> banque(rendu, y, decale);
            case OPERATIONS -> operations(rendu, y, decale);
            case LANCER -> lancer(rendu, y, decale);
            case EQUIPE -> equipe(rendu, y, decale);
            case ANNONCES -> annonces(rendu, y, decale);
            case COMMANDES -> commandes(rendu, y, decale);
            case CONTACTS -> contacts(rendu, y, decale);
        }
        if (glisse < 1f) {                                              // le fondu du glissement
            rendu.rect(X, haut, L, y0 + H - 12 - haut, FOND, Math.round(200 * (1 - k)));
        }
        if (!info.isEmpty()) {                                          // le retour d'une action : un bandeau, au-dessus des touches
            List<String> l = affichage.petite().couper(affichage.petite().propre(Casse.phrase(info)), L - 16);
            int h = l.size() * 8 + 5;
            int iy = y0 + H - 14 - h;
            boolean erreur = info.startsWith("IL TE MANQUE") || info.contains("REFUSE") || info.startsWith("IL NE") || info.contains("PAS");
            rendu.rect(X + 4, iy, L - 8, h, erreur ? 0x3A1A1A : 0x1A3A26, 256);
            rendu.rect(X + 4, iy, 2, h, erreur ? ROUGE : Affichage.VERT, 256);
            for (int i = 0; i < l.size(); i++) {
                affichage.petite().ecrire(rendu, l.get(i), X + 10, iy + 3 + i * 8, Affichage.BLANC, 1);
            }
        }
        touches(rendu, y0 + H - 10);
    }

    /** L'heure et le jour du quartier, le reseau, et rien d'autre. */
    private void barreDEtat(Rendu rendu, int y) {
        histoire.Calendrier cal = etat().calendrier();
        affichage.petite().ecrire(rendu, cal.horloge(), X + 5, y + 3, Affichage.BLANC, 1);
        String jour = cal.nomDuJour().substring(0, 3) + ".";
        affichage.petite().ecrire(rendu, jour, X + L / 2 - affichage.petite().largeur(jour, 1) / 2, y + 3, Affichage.SOMBRE, 1);
        for (int b = 0; b < 4; b++) {                                   // le reseau : quatre barres
            rendu.rect(X + L - 20 + b * 3, y + 9 - b * 2 - 2, 2, b * 2 + 2, b < 3 ? Affichage.GRIS : TRAIT, 256);
        }
        if (etat().nonLus() > 0) {
            rendu.rect(X + L - 27, y + 4, 3, 3, Affichage.ORANGE, 256);
        }
    }

    /** Le haut d'une appli : la fleche du retour, son icone, son nom ; renvoie le y ou commence son contenu. */
    private int entete(Rendu rendu, int y, int decale) {
        int teinte = TEINTES[rang(ecran)];
        affichage.police().ecrire(rendu, "<", X + 5 + decale, y + 4, Affichage.SOMBRE, 1);
        String titre = ecran == Ecran.FIL ? nom(contact) : ecran == Ecran.LANCER ? preparee.nom() : court(ecran);
        while (titre.length() > 4 && affichage.police().largeur(titre, 1) > L - 40) {
            titre = titre.substring(0, titre.length() - 2).trim() + "…";
            titre = titre.replace("……", "…");
        }
        if (ecran == Ecran.FIL) {
            avatar(rendu, contact, X + 14 + decale, y + 2, 14);
            affichage.police().ecrire(rendu, affichage.police().propre(titre), X + 32 + decale, y + 4, Affichage.BLANC, 1);
        } else {
            icone(rendu, rang(ecran), X + 14 + decale, y + 2, 14, false);
            affichage.police().ecrire(rendu, affichage.police().propre(titre), X + 32 + decale, y + 4, Affichage.BLANC, 1);
        }
        rendu.rect(X, y + 19, L, 1, teinte, 160);
        return y + 20;
    }

    /** Les touches utiles a cet ecran, comme de petites touches de clavier. */
    private void touches(Rendu rendu, int y) {
        String[][] t = ecran == Ecran.LANCER ? new String[][] {{"E", "LANCER"}, {"< >", "PAIE"}, {"ÉCHAP", "RETOUR"}}
                : ecran == Ecran.ACCUEIL ? new String[][] {{"E", "OUVRIR"}, {"P", "RANGER"}}
                : new String[][] {{"E", ecran == Ecran.FIL ? "RÉPONDRE" : "CHOISIR"}, {"ÉCHAP", "RETOUR"}};
        int x = X + 6;
        for (String[] k : t) {
            int lk = affichage.petite().largeur(k[0], 1) + 4;
            rendu.rect(x, y - 1, lk, 8, TRAIT, 256);
            affichage.petite().ecrire(rendu, k[0], x + 2, y, Affichage.BLANC, 1);
            x += lk + 3;
            affichage.petite().ecrire(rendu, k[1], x, y, Affichage.SOMBRE, 1);
            x += affichage.petite().largeur(k[1], 1) + 8;
        }
    }

    /** L'icone d'une appli : une pastille de sa couleur, son dessin en blanc. */
    private void icone(Rendu rendu, int rang, int x, int y, int cote, boolean choisie) {
        int teinte = TEINTES[rang];
        rendu.rect(x + 1, y, cote - 2, cote, teinte, 256);              // un carre aux coins rabattus
        rendu.rect(x, y + 1, cote, cote - 2, teinte, 256);
        rendu.rect(x + 1, y + cote / 2, cote - 2, cote / 2 - 1, 0x000000, 40);
        String[] g = GLYPHES[rang];
        int e = Math.max(1, (cote - 6) / 7);
        int gx = x + (cote - 7 * e) / 2;
        int gy = y + (cote - 7 * e) / 2;
        for (int j = 0; j < 7; j++) {
            for (int i = 0; i < 7; i++) {
                if (g[j].charAt(i) == 'X') {
                    rendu.rect(gx + i * e, gy + j * e, e, e, 0xFFFFFF, 235);
                }
            }
        }
        if (choisie) {
            rendu.rect(x - 2, y - 2, cote + 4, 1, Affichage.BLANC, 256);
            rendu.rect(x - 2, y + cote + 1, cote + 4, 1, Affichage.BLANC, 256);
            rendu.rect(x - 2, y - 2, 1, cote + 4, Affichage.BLANC, 256);
            rendu.rect(x + cote + 1, y - 2, 1, cote + 4, Affichage.BLANC, 256);
        }
    }

    /** Le visage d'un correspondant, en vignette ; ses initiales s'il n'a pas de portrait. */
    private void avatar(Rendu rendu, String id, int x, int y, int cote) {
        String portrait = null;
        try {
            portrait = res.campagne.voix(id).portrait();
        } catch (RuntimeException inconnu) {
            portrait = null;
        }
        decor.Texture t = portrait == null ? null : res.image(portrait);
        rendu.rect(x, y, cote, cote, 0x2E2E3C, 256);
        if (t != null) {
            rendu.calque(decor.Poses.vignette(t, cote), x, y);
        } else {
            String n = nom(id);
            affichage.petite().ecrire(rendu, n.substring(0, 1), x + cote / 2 - 2, y + cote / 2 - 3, Affichage.BLANC, 1);
        }
    }

    /** Une pastille de texte (un statut, un prix), a droite ou ailleurs ; renvoie sa largeur. */
    private int pastille(Rendu rendu, String texte, int xDroite, int y, int couleur) {
        int l = affichage.petite().largeur(texte, 1) + 6;
        rendu.rect(xDroite - l, y - 1, l, 9, couleur, 60);
        affichage.petite().ecrire(rendu, texte, xDroite - l + 3, y, couleur, 1);
        return l;
    }

    /** La ligne choisie d'une liste : un fond plus clair et un trait de la couleur de l'appli. */
    private void choisie(Rendu rendu, int y, int h, boolean oui) {
        if (oui) {
            rendu.rect(X + 2, y, L - 4, h, SURFACE, 256);
            rendu.rect(X + 2, y, 2, h, TEINTES[rang(ecran)], 256);
        }
    }

    private static String coupe(String texte, int max) {
        return texte.length() > max ? texte.substring(0, max - 1).trim() + "…" : texte;
    }

    private String euros(int montant) {
        return (montant > 0 ? "+" : montant < 0 ? "-" : "") + Math.abs(montant) + " €";
    }

    // ------------------------------------------------------------------ les ecrans

    private void accueil(Rendu rendu, int y) {
        histoire.Calendrier cal = etat().calendrier();
        affichage.police().ecrireCentre(rendu, cal.horloge(), X + L / 2, y + 4, Affichage.BLANC, 2);
        String date = cal.nomDuJour() + "  ·  JOUR " + cal.jour() + "  ·  ";
        String solde = etat().argent() + " €";
        int ld = affichage.petite().largeur(date, 1) + affichage.petite().largeur(solde, 1);
        affichage.petite().ecrire(rendu, date, X + L / 2 - ld / 2, y + 25, Affichage.SOMBRE, 1);
        affichage.petite().ecrire(rendu, solde, X + L / 2 - ld / 2 + affichage.petite().largeur(date, 1), y + 25, Affichage.ORANGE, 1);
        List<Ecran> applis = applis();
        int cellule = 48;
        int gx = X + (L - COLONNES * cellule) / 2;
        int gy = y + 42;
        for (int i = 0; i < applis.size(); i++) {
            Ecran a = applis.get(i);
            int cx = gx + (i % COLONNES) * cellule + (cellule - 26) / 2;
            int cy = gy + (i / COLONNES) * 39;
            icone(rendu, rang(a), cx, cy, 26, i == choix);
            String nom = court(a);
            affichage.petite().ecrire(rendu, nom, cx + 13 - affichage.petite().largeur(nom, 1) / 2, cy + 28,
                    i == choix ? Affichage.BLANC : Affichage.SOMBRE, 1);
            String badge = null;
            int couleur = ROUGE;
            if (a == Ecran.MESSAGES && etat().nonLus() > 0) {
                badge = Integer.toString(etat().nonLus());
            } else if (a == Ecran.BANQUE && etat().loyerDu() > 0) {
                badge = "!";
            } else if (a == Ecran.COMMANDES && !etat().colis().isEmpty()) {
                badge = Integer.toString(etat().colis().size());
                couleur = Affichage.VERT;
            } else if (a == Ecran.OPERATIONS && !orga().enCours().isEmpty()) {
                badge = Integer.toString(orga().enCours().size());
                couleur = Affichage.VERT;
            }
            if (badge != null) {
                int lb = Math.max(9, affichage.petite().largeur(badge, 1) + 4);
                rendu.rect(cx + 26 - lb + 3, cy - 3, lb, 9, couleur, 256);
                affichage.petite().ecrire(rendu, badge, cx + 26 - lb + 5, cy - 2, Affichage.BLANC, 1);
            }
        }
        List<String> notes = etat().notifications();                    // les deux dernieres, une ligne chacune
        int ny = gy + ((applis.size() + COLONNES - 1) / COLONNES) * 39 + 2;
        for (int i = notes.size() - 1, n = 0; i >= 0 && n < 2 && ny + n * 13 < Y + H - 24; i--, n++) {
            String t = affichage.petite().propre(Casse.phrase(notes.get(i)));
            List<String> l = affichage.petite().couper(t, L - 18);
            rendu.rect(X + 4, ny + n * 13, L - 8, 11, SURFACE, 256);
            rendu.rect(X + 4, ny + n * 13, 2, 11, Affichage.ORANGE, n == 0 ? 256 : 120);
            affichage.petite().ecrire(rendu, l.size() > 1 ? l.get(0) + "…" : l.get(0), X + 9, ny + n * 13 + 2,
                    n == 0 ? Affichage.BLANC : Affichage.GRIS, 1);
        }
    }

    private void messages(Rendu rendu, int y, int d) {
        List<String> fils = etat().fils();
        if (fils.isEmpty()) {
            affichage.petite().ecrire(rendu, "AUCUN MESSAGE.", X + 8 + d, y + 4, Affichage.SOMBRE, 1);
        }
        for (int i = 0; i < fils.size() && y < Y + H - 40; i++) {
            String de = fils.get(i);
            choisie(rendu, y, 24, i == choix);
            avatar(rendu, de, X + 7 + d, y + 3, 18);
            boolean nouveau = etat().nonLu(de);
            affichage.police().ecrire(rendu, affichage.police().propre(nom(de)), X + 30 + d, y + 3, nouveau || i == choix ? Affichage.BLANC : Affichage.GRIS, 1);
            List<Etat.Message> fil = etat().fil(de);
            Etat.Message dernier = fil.get(fil.size() - 1);
            String apercu = (dernier.deMoi() ? "TOI : " : "") + affichage.petite().propre(dernier.texte());
            affichage.petite().ecrire(rendu, coupe(apercu, 34), X + 30 + d, y + 14, Affichage.SOMBRE, 1);
            if (nouveau) {
                rendu.rect(X + L - 10, y + 6, 4, 4, Affichage.ORANGE, 256);
            } else if (!etat().reponses(de).isEmpty()) {
                pastille(rendu, "À TOI", X + L - 5, y + 4, Affichage.ORANGE);
            }
            y += 26;
        }
    }

    private void fil(Rendu rendu, int y, int d) {
        List<Etat.Message> fil = etat().fil(contact);
        List<Etat.Reponse> reponses = etat().reponses(contact);
        int bas = Y + H - 26 - reponses.size() * 14;
        int debut = fil.size();                                          // on montre les derniers messages qui tiennent
        int place = bas - y;
        while (debut > 0) {
            int h = affichage.police().couper(affichage.police().propre(fil.get(debut - 1).texte()), L - 40).size() * 12 + 8;
            if (h > place) {
                break;
            }
            place -= h;
            debut--;
        }
        for (int i = debut; i < fil.size(); i++) {
            Etat.Message m = fil.get(i);
            List<String> lignes = affichage.police().couper(affichage.police().propre(m.texte()), L - 40);
            int largeur = 0;
            for (String l : lignes) {
                largeur = Math.max(largeur, affichage.police().largeur(l, 1));
            }
            int h = lignes.size() * 12 + 5;
            int x = m.deMoi() ? X + L - 8 - largeur - 8 + d : X + 6 + d;
            rendu.rect(x, y, largeur + 8, h, m.deMoi() ? 0x2E6B4A : SURFACE, 256);
            for (int k = 0; k < lignes.size(); k++) {
                affichage.police().ecrire(rendu, lignes.get(k), x + 4, y + 3 + k * 12, Affichage.BLANC, 1);
            }
            y += h + 3;
        }
        if (!reponses.isEmpty()) {                                       // les reponses possibles : des boutons
            int ry = bas + 2;
            for (int i = 0; i < reponses.size(); i++) {
                boolean oui = i == choix;
                rendu.rect(X + 6, ry, L - 12, 12, oui ? 0x2E6B4A : SURFACE, 256);
                affichage.police().ecrire(rendu, coupe(affichage.police().propre(reponses.get(i).texte()), 26), X + 10, ry + 2,
                        oui ? Affichage.BLANC : Affichage.GRIS, 1);
                ry += 14;
            }
        }
    }

    private void banque(Rendu rendu, int y, int d) {
        affichage.petite().ecrire(rendu, "SOLDE", X + L / 2 - affichage.petite().largeur("SOLDE", 1) / 2 + d, y + 2, Affichage.SOMBRE, 1);
        affichage.police().ecrireCentre(rendu, etat().argent() + " €", X + L / 2 + d, y + 11, Affichage.BLANC, 2);
        y += 32;
        int du = etat().loyerDu();
        String loyer = du == 0 ? "LOYER À JOUR" : "LOYER : " + du + " MOIS EN RETARD" + (etat().coupe() ? " · COURANT COUPÉ" : "");
        int lp = affichage.petite().largeur(loyer, 1) + 8;
        rendu.rect(X + L / 2 - lp / 2 + d, y, lp, 10, du == 0 ? Affichage.VERT : ROUGE, 60);
        affichage.petite().ecrire(rendu, loyer, X + L / 2 - lp / 2 + 4 + d, y + 1, du == 0 ? Affichage.VERT : ROUGE, 1);
        y += 14;
        boolean actif = du > 0;
        rendu.rect(X + 10, y, L - 20, 13, actif ? (choix == 0 ? Affichage.ORANGE : 0x5A3A1A) : SURFACE, 256);
        String bouton = actif ? "PAYER UN MOIS · " + Etat.LOYER + " €" : "RIEN À PAYER";
        affichage.police().ecrireCentre(rendu, bouton, X + L / 2, y + 2, actif ? Affichage.ENCRE : Affichage.SOMBRE, 1);
        y += 19;
        if (orga().active()) {                                          // l'affaire en deux chiffres
            int entre = orga().totalRecettes();
            int sort = orga().totalPaies() + orga().totalLabo() + orga().totalFrais();
            affichage.petite().ecrire(rendu, "AFFAIRES", X + 6, y, Affichage.SOMBRE, 1);
            String e = euros(entre);
            String s = euros(-sort);
            affichage.petite().ecrire(rendu, e, X + 60, y, Affichage.VERT, 1);
            affichage.petite().ecrire(rendu, s, X + L - 6 - affichage.petite().largeur(s, 1), y, Affichage.GRIS, 1);
            y += 11;
        }
        affichage.petite().ecrire(rendu, "MOUVEMENTS", X + 6, y, Affichage.SOMBRE, 1);
        y += 9;
        List<Etat.Ligne> journal = etat().journal();
        for (int i = journal.size() - 1; i >= 0 && y < Y + H - 26; i--) {
            Etat.Ligne l = journal.get(i);
            boolean entree = l.montant() > 0;
            int c = entree ? Affichage.VERT : l.libelle().startsWith("LOYER") || l.libelle().startsWith("AMENDE") ? ROUGE : Affichage.BLANC;
            rendu.rect(X + 6, y + 2, 3, 3, c, 256);                     // une puce : entree verte, sortie claire, loyer rouge
            String libelle = affichage.petite().propre(Casse.phrase(l.libelle()));
            affichage.petite().ecrire(rendu, coupe(libelle, 29), X + 12 + d, y, Affichage.GRIS, 1);
            String m = euros(l.montant());
            affichage.petite().ecrire(rendu, m, X + L - 6 - affichage.petite().largeur(m, 1), y, entree ? Affichage.VERT : Affichage.BLANC, 1);
            y += 10;
        }
    }

    /** Une jauge : son icone (un point), sa barre de couleur, son chiffre. */
    private void jauge(Rendu rendu, int x, int y, int largeur, String nom, int valeur) {
        int c = couleurJauge(valeur);
        affichage.petite().ecrire(rendu, nom, x, y, Affichage.SOMBRE, 1);
        int bx = x + 30;
        int l = largeur - 30 - 14;
        rendu.rect(bx, y + 2, l, 3, TRAIT, 256);
        rendu.rect(bx, y + 2, Math.round(l * valeur / 100f), 3, c, 256);
        affichage.petite().ecrire(rendu, Integer.toString(valeur), bx + l + 3, y, c, 1);
    }

    private static int couleurJauge(int v) {
        return v >= 70 ? Affichage.VERT : v >= 45 ? Affichage.ORANGE : ROUGE;
    }

    private void operations(Rendu rendu, int y, int d) {
        List<Organisation.EnCours> encours = orga().enCours();
        for (Organisation.EnCours e : encours) {                       // ce qui tourne : qui, quoi, ou ca en est
            Organisation.Operation op = Organisation.operation(e.operation);
            rendu.rect(X + 4, y, L - 8, 20, SURFACE, 256);
            avatar(rendu, e.membre, X + 7 + d, y + 3, 14);
            affichage.petite().ecrire(rendu, orga().membre(e.membre).nom + " · " + op.nom(), X + 25 + d, y + 3, Affichage.BLANC, 1);
            float part = 1 - Math.max(0, e.reste) / e.total;
            rendu.rect(X + 25, y + 13, L - 34, 3, TRAIT, 256);
            rendu.rect(X + 25, y + 13, Math.round((L - 34) * part), 3, e.retarde ? ROUGE : Affichage.VERT, 256);
            y += 23;
        }
        if (!encours.isEmpty()) {
            y += 2;
        }
        affichage.petite().ecrire(rendu, "À CONFIER", X + 6, y, Affichage.SOMBRE, 1);
        y += 10;
        List<Organisation.Operation> ops = orga().disponibles(progression);
        for (int i = 0; i < ops.size(); i++) {
            Organisation.Operation op = ops.get(i);
            boolean tourne = encours.stream().anyMatch(e -> e.operation.equals(op.id()));
            int net = op.recette() - op.labo() - op.frais() - op.paie();
            choisie(rendu, y - 2, 13, i == choix);
            int lp = pastille(rendu, tourne ? "EN COURS" : euros(net), X + L - 5, y + 1, tourne ? Affichage.SOMBRE : Affichage.VERT);
            String nomOp = affichage.police().propre(op.nom());
            while (nomOp.length() > 4 && affichage.police().largeur(nomOp, 1) > L - 18 - lp) {
                nomOp = nomOp.substring(0, nomOp.length() - 2).trim() + "…";
                nomOp = nomOp.replace("……", "…");
            }
            affichage.police().ecrire(rendu, nomOp, X + 8 + d, y, i == choix ? Affichage.BLANC : Affichage.GRIS, 1);
            y += 14;
        }
        List<Organisation.Bilan> bilans = orga().bilans();
        if (!bilans.isEmpty() && y < Y + H - 46) {
            y += 4;
            affichage.petite().ecrire(rendu, "DERNIERS RETOURS", X + 6, y, Affichage.SOMBRE, 1);
            y += 10;
            for (int i = bilans.size() - 1; i >= Math.max(0, bilans.size() - 3) && y < Y + H - 26; i--) {
                Organisation.Bilan b = bilans.get(i);
                String qui = orga().membre(b.membre()) == null ? b.membre() : orga().membre(b.membre()).nom;
                String net = euros(b.net());
                affichage.petite().ecrire(rendu, qui + " · " + b.issue().nom, X + 8 + d, y, Affichage.GRIS, 1);
                affichage.petite().ecrire(rendu, net, X + L - 6 - affichage.petite().largeur(net, 1), y,
                        b.net() >= 0 ? Affichage.VERT : ROUGE, 1);
                y += 9;
            }
        }
    }

    /** Confier une operation : a qui (son visage, ses deux jauges, sa chance), combien le payer, ce que ca engage. */
    private void lancer(Rendu rendu, int y, int d) {
        Organisation.Operation op = preparee;
        List<Organisation.Membre> ms = orga().membres();
        for (int i = 0; i < ms.size(); i++) {
            Organisation.Membre m = ms.get(i);
            boolean oui = i == choix;
            choisie(rendu, y, 32, oui);
            avatar(rendu, m.id, X + 7 + d, y + 3, 18);
            affichage.police().ecrire(rendu, m.nom, X + 30 + d, y + 2, oui ? Affichage.BLANC : Affichage.GRIS, 1);
            String etatM = !m.libre() ? "OCCUPÉ" : m.confiance < op.confianceMin() ? "REFUSE" : orga().chance(op, m, paie) + " %";
            pastille(rendu, etatM, X + L - 5, y + 3, !m.libre() || m.confiance < op.confianceMin() ? ROUGE : Affichage.VERT);
            if (m.talent == op.talent()) {
                rendu.rect(X + 30 + affichage.police().largeur(m.nom, 1) + 4 + d, y + 5, 4, 4, Affichage.ORANGE, 256);   // son talent
            }
            jauge(rendu, X + 30 + d, y + 14, L - 36, "FIABLE", m.fiabilite);
            jauge(rendu, X + 30 + d, y + 22, L - 36, "CONF.", m.confiance);
            y += 34;
        }
        y += 2;
        Organisation.Paie[] paies = Organisation.Paie.values();          // la paie : trois choix cote a cote
        int lp = (L - 12) / paies.length;
        for (int i = 0; i < paies.length; i++) {
            boolean oui = paies[i] == paie;
            int px = X + 6 + i * lp;
            rendu.rect(px, y, lp - 2, 18, oui ? Affichage.ORANGE : SURFACE, 256);
            String n = paies[i].nom;
            String s = Organisation.paie(op, paies[i]) + " €";
            affichage.petite().ecrire(rendu, n, px + (lp - 2) / 2 - affichage.petite().largeur(n, 1) / 2, y + 2, oui ? Affichage.ENCRE : Affichage.GRIS, 1);
            affichage.petite().ecrire(rendu, s, px + (lp - 2) / 2 - affichage.petite().largeur(s, 1) / 2, y + 10, oui ? Affichage.ENCRE : Affichage.BLANC, 1);
        }
        y += 23;
        Organisation.Membre m = ms.get(Math.min(choix, ms.size() - 1));
        int salaire = Organisation.paie(op, paie);
        int minutes = Math.max(1, Math.round(orga().duree(op, m) / 60));
        String[][] l = {{"PAYÉ MAINTENANT", euros(-(op.frais() + salaire))}, {"RECETTE ESPÉRÉE", euros(op.recette() - op.labo())},
            {"DURÉE", "~" + minutes + " MIN"}};
        for (String[] r : l) {
            affichage.petite().ecrire(rendu, r[0], X + 8, y, Affichage.SOMBRE, 1);
            int c = r[1].startsWith("-") ? Affichage.BLANC : r[1].startsWith("+") ? Affichage.VERT : Affichage.GRIS;
            affichage.petite().ecrire(rendu, r[1], X + L - 8 - affichage.petite().largeur(r[1], 1), y, c, 1);
            y += 9;
        }
    }

    /** L'equipe : une carte par petit. Visage, nom, talent, s'il est libre, ses deux jauges, son bilan. */
    private void equipe(Rendu rendu, int y, int d) {
        List<Organisation.Membre> ms = orga().membres();
        for (int i = 0; i < ms.size(); i++) {
            Organisation.Membre m = ms.get(i);
            boolean oui = i == choix;
            rendu.rect(X + 4, y, L - 8, 50, oui ? SURFACE : 0x17171F, 256);
            if (oui) {
                rendu.rect(X + 4, y, 2, 50, TEINTES[3], 256);
            }
            avatar(rendu, m.id, X + 9 + d, y + 4, 24);
            affichage.police().ecrire(rendu, m.nom, X + 38 + d, y + 4, Affichage.BLANC, 1);
            affichage.petite().ecrire(rendu, m.talent.nom, X + 38 + d, y + 16, Affichage.ORANGE, 1);
            pastille(rendu, m.libre() ? "LIBRE" : "EN OPÉRATION", X + L - 8, y + 4, m.libre() ? Affichage.VERT : Affichage.ORANGE);
            jauge(rendu, X + 9 + d, y + 31, L - 20, "FIABLE", m.fiabilite);
            jauge(rendu, X + 9 + d, y + 40, L - 20, "CONF.", m.confiance);
            String bilan = m.reussites + " OK · " + m.echecs + " RATÉ" + (m.echecs > 1 ? "S" : "");
            affichage.petite().ecrire(rendu, bilan, X + L - 8 - affichage.petite().largeur(bilan, 1), y + 16, Affichage.SOMBRE, 1);
            y += 54;
        }
    }

    private void annonces(Rendu rendu, int y, int d) {
        List<Etat.Commerce> annonces = annonces();
        for (int i = 0; i < annonces.size(); i++) {
            Etat.Commerce c = annonces.get(i);
            boolean aMoi = etat().commerces().contains(c.id());
            boolean oui = i == choix;
            rendu.rect(X + 4, y, L - 8, 24, oui ? SURFACE : 0x17171F, 256);
            if (oui) {
                rendu.rect(X + 4, y, 2, 24, TEINTES[4], 256);
            }
            affichage.police().ecrire(rendu, affichage.police().propre(c.nom()), X + 9 + d, y + 3, oui ? Affichage.BLANC : Affichage.GRIS, 1);
            affichage.petite().ecrire(rendu, "+" + c.recette() + " € À CHAQUE MISSION", X + 9 + d, y + 14, Affichage.VERT, 1);
            pastille(rendu, aMoi ? "À TOI" : c.prix() + " €", X + L - 8, y + 4, aMoi ? Affichage.VERT : Affichage.ORANGE);
            y += 27;
        }
    }

    private void commandes(Rendu rendu, int y, int d) {
        for (int i = 0; i < Etat.ARTICLES.size() && y < Y + H - 30; i++) {
            Etat.Article a = Etat.ARTICLES.get(i);
            boolean oui = i == choix;
            String statut = etat().possede(a.id()) ? "À TOI" : etat().colis().contains(a.id()) ? "CHEZ TOI" : etat().enRoute(a.id()) ? "EN ROUTE"
                    : a.prix() + " €";
            choisie(rendu, y - 2, 22, oui);
            affichage.police().ecrire(rendu, affichage.police().propre(a.nom()), X + 8 + d, y, oui ? Affichage.BLANC : Affichage.GRIS, 1);
            pastille(rendu, statut, X + L - 5, y + 1, etat().dejaPris(a.id()) ? Affichage.VERT : Affichage.ORANGE);
            affichage.petite().ecrire(rendu, coupe(affichage.petite().propre((a.arme() ? "ARME · " : "") + a.effet()), 36), X + 8 + d, y + 11,
                    Affichage.SOMBRE, 1);
            y += 24;
        }
        affichage.petite().ecrire(rendu, "LIVRÉ DEVANT TA PORTE, CHEZ TOI.", X + 6, Y + H - 24, Affichage.SOMBRE, 1);
    }

    private void contacts(Rendu rendu, int y, int d) {
        List<String> contacts = contacts();
        if (contacts.isEmpty()) {
            affichage.petite().ecrire(rendu, "TU NE CONNAIS ENCORE PERSONNE.", X + 8, y + 4, Affichage.SOMBRE, 1);
        }
        for (int i = 0; i < contacts.size() && y < Y + H - 30; i++) {
            String qui = contacts.get(i);
            boolean oui = i == choix;
            choisie(rendu, y, 22, oui);
            avatar(rendu, qui, X + 7 + d, y + 2, 18);
            affichage.police().ecrire(rendu, affichage.police().propre(nom(qui)), X + 30 + d, y + 6, oui ? Affichage.BLANC : Affichage.GRIS, 1);
            if (qui.equals("youngssef")) {
                pastille(rendu, "SCOOTER · 8 €", X + L - 5, y + 7, Affichage.ORANGE);
            }
            y += 24;
        }
    }
}
