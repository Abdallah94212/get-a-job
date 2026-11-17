package histoire;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * L'etat du heros, en dehors de l'histoire : sa sante, son souffle, son argent, et le scooter qu'il a sous la main.
 *
 * Les trois se tiennent : courir vide le souffle, se faire prendre coute de la sante, une sante basse raccourcit le
 * souffle et ralentit la marche, et tout ce qui repare (manger, louer un scooter pour ne plus courir) coute de l'argent,
 * que seules les livraisons rapportent. Dormir chez soi est gratuit, mais il faut y retourner.
 */
public final class Etat {
    public static final float SANTE_MAX = 100f;
    public static final float SEUIL_BLESSE = 35f;
    /** Ce que coute de courir, par seconde, et ce qu'on reprend en marchant ou a l'arret. */
    static final float COURSE = 18f;
    static final float PORTAGE = 5f;
    static final float REPRISE_MARCHE = 12f;
    static final float REPRISE_ARRET = 26f;
    /** Essouffle, on ne recourt pas avant d'avoir repris ce souffle-la. */
    static final float SEUIL_REPRISE = 30f;

    private float sante = SANTE_MAX;
    private float endurance = 100f;
    private int argent;
    private boolean essouffle;
    /** Secondes de location restantes ; 0 : pas de location (le scooter est a soi le temps d'une mission, ou absent). */
    private float location;
    private String scooterCarte;
    private float scooterX;
    private float scooterY;

    public float sante() {
        return sante;
    }

    public float endurance() {
        return endurance;
    }

    public int argent() {
        return argent;
    }

    public boolean essouffle() {
        return essouffle;
    }

    public boolean blesse() {
        return sante < SEUIL_BLESSE;
    }

    public boolean ko() {
        return sante <= 0f;
    }

    /** Blesse, on a moins de souffle. */
    public float enduranceMax() {
        return blesse() ? 60f : 100f;
    }

    public float location() {
        return location;
    }

    public String scooterCarte() {
        return scooterCarte;
    }

    public float scooterX() {
        return scooterX;
    }

    public float scooterY() {
        return scooterY;
    }

    // ------------------------------------------------------------------ l'argent

    public void gagner(int montant) {
        gagner(montant, montant >= 0 ? "PAIE" : "DÉPENSE");
    }

    /** Recoit (ou perd, si le montant est negatif) de l'argent ; le telephone en garde la trace. */
    public void gagner(int montant, String libelle) {
        int avant = argent;
        argent = Math.max(0, argent + montant);
        noter(libelle, argent - avant);
    }

    /** Paie si on a de quoi. Renvoie faux, sans rien prendre, si l'argent manque. */
    public boolean payer(int montant) {
        return payer(montant, "DÉPENSE");
    }

    public boolean payer(int montant, String libelle) {
        if (montant > argent) {
            return false;
        }
        argent -= montant;
        noter(libelle, -montant);
        return true;
    }

    private long notees;

    private void noter(String libelle, int montant) {
        if (montant != 0) {
            notees++;
            journal.add(new Ligne(libelle, montant));
            while (journal.size() > 800) {
                journal.remove(0);
            }
        }
    }

    // ------------------------------------------------------------------ la sante

    /** Perd de la sante, sans descendre sous le plancher (un choc en scooter sonne, il ne tue pas). */
    public void blesser(float degats, float plancher) {
        sante = Math.max(Math.min(sante, plancher), sante - degats);
        endurance = Math.min(endurance, enduranceMax());
    }

    public void soigner(float points) {
        sante = Math.min(SANTE_MAX, sante + points);
    }

    /** Un repas ou une nuit : le souffle revient d'un coup. */
    public void reposer() {
        endurance = enduranceMax();
        essouffle = false;
    }

    /** K.-O. : on se reveille chez soi, a moitie remis, et il manque un tiers de l'argent. Renvoie ce qu'on a perdu. */
    public int relever() {
        int perdu = argent * 3 / 10;
        gagner(-perdu, "PERDU EN TOMBANT");
        sante = 50f;
        reposer();
        rendreScooter();
        return perdu;
    }

    // ------------------------------------------------------------------ le souffle

    /**
     * Une image de jeu. veutCourir : la touche de course est tenue et le heros avance a pied ; porte : il avance avec
     * une charge. Renvoie vrai s'il court vraiment (il lui reste du souffle).
     */
    public boolean respirer(boolean bouge, boolean veutCourir, boolean porte, float dt) {
        boolean court = veutCourir && !essouffle;
        if (court) {
            endurance -= COURSE * (possede("baskets") ? 0.65f : 1f) * dt;
            if (endurance <= 0f) {
                endurance = 0f;
                essouffle = true;
            }
        } else if (porte && bouge) {
            endurance = Math.max(0f, endurance - PORTAGE * dt);
        } else {
            endurance = Math.min(enduranceMax(), endurance + (bouge ? REPRISE_MARCHE : REPRISE_ARRET) * dt);
            if (essouffle && endurance >= SEUIL_REPRISE) {
                essouffle = false;
            }
        }
        return court;
    }

    /** Un coup donne coute du souffle ; a bout, on frappe plus lentement. */
    public void effort(float points) {
        endurance = Math.max(0f, endurance - points);
        if (endurance <= 0f) {
            essouffle = true;
        }
    }

    // ------------------------------------------------------------------ le scooter

    /** Loue le scooter pour une duree. */
    public void louer(float secondes) {
        location = secondes;
        scooterCarte = null;
    }

    /** Le temps de location passe. Renvoie vrai a l'instant ou elle se termine. */
    public boolean userLocation(float dt) {
        if (location <= 0f) {
            return false;
        }
        location -= dt;
        if (location <= 0f) {
            location = 0f;
            return true;
        }
        return false;
    }

    /** Le scooter est laisse a cet endroit. */
    public void garer(String carte, float x, float y) {
        scooterCarte = carte;
        scooterX = x;
        scooterY = y;
    }

    /** Le scooter repart a son proprietaire : plus de location, plus de scooter gare. */
    public void rendreScooter() {
        location = 0f;
        scooterCarte = null;
    }

    /** Remet l'etat tel qu'il a ete sauvegarde. */
    public void restaurer(float santeLue, float enduranceLue, int argentLu, float locationLue, String carte, float x, float y) {
        sante = Math.max(1f, Math.min(SANTE_MAX, santeLue));
        endurance = Math.max(0f, Math.min(enduranceMax(), enduranceLue));
        argent = Math.max(0, argentLu);
        location = Math.max(0f, locationLue);
        scooterCarte = carte == null || carte.isBlank() ? null : carte;
        scooterX = x;
        scooterY = y;
        essouffle = false;
    }

    // ================================================================== le telephone
    // Tout ce que le telephone montre vit ici : les messages, l'argent qui entre et qui sort, les commandes en route,
    // les colis qui attendent a la maison, les commerces achetes et le loyer.

    /** Un message recu ou envoye. */
    public record Message(String de, String texte, boolean deMoi) {
    }

    /** Une reponse possible a un message : ce qu'on ecrit, et les drapeaux que cela leve dans l'histoire ("-" : aucun). */
    /** Une reponse possible a un message. Entre parentheses, c'est un geste (ne rien repondre) : rien n'est envoye. */
    public record Reponse(String texte, String drapeaux) {
        public boolean silencieuse() {
            return texte.startsWith("(");
        }
    }

    /** Une ligne du releve de compte. */
    public record Ligne(String libelle, int montant) {
    }

    /** Un objet qu'on commande. Il n'arrive pas dans la poche : il est livre chez soi, et il faut aller l'ouvrir. */
    public record Article(String id, String nom, String effet, int prix, boolean arme) {
    }

    /** Un commerce a vendre : il rapporte sa recette a chaque mission terminee. */
    public record Commerce(String id, String nom, int prix, int recette, String condition) {
    }

    public static final List<Article> ARTICLES = List.of(
            new Article("casque", "CASQUE", "LES CHOCS EN SCOOTER NE BLESSENT PLUS", 35, false),
            new Article("baskets", "BASKETS DE COURSE", "COURIR COÛTE UN TIERS DE SOUFFLE EN MOINS", 45, false),
            new Article("gilet", "GILET RENFORCÉ", "LES COUPS FONT MOITIÉ MOINS MAL", 80, false),
            new Article("lacrymo", "BOMBE LACRYMOGÈNE", "REPÉRÉ, ELLE TE SORT DE LÀ UNE FOIS. PUIS ELLE EST VIDE", 60, true));
    public static final List<Commerce> COMMERCES = List.of(
            new Commerce("kiosque", "LE KIOSQUE", 120, 10, ""),
            new Commerce("laverie", "LA LAVERIE", 260, 24, "+parti_de_chez_geto"));
    public static final int LOYER = 40;
    /** Le temps de jeu entre la commande et le colis devant la porte. */
    public static final float DELAI = 60f;

    private final List<Message> messages = new ArrayList<>();
    private final Map<String, List<Reponse>> attentes = new LinkedHashMap<>();
    private final Set<String> nonLus = new LinkedHashSet<>();
    private final Set<String> commerces = new LinkedHashSet<>();
    private final Map<String, Float> commandes = new LinkedHashMap<>();
    private final List<String> colis = new ArrayList<>();
    private final List<Ligne> journal = new ArrayList<>();
    private final Deque<String> avis = new ArrayDeque<>();
    private final List<String> notifications = new ArrayList<>();
    private int loyersPayes;
    private int loyersVus = -1;

    public static Article article(String id) {
        return ARTICLES.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ les notifications

    /** Previent le joueur : le jeu l'affiche une fois, le telephone le garde. */
    public void prevenir(String texte) {
        avis.add(texte);
        notifications.add(texte);
        while (notifications.size() > 8) {
            notifications.remove(0);
        }
    }

    /** La prochaine notification a afficher, ou null. */
    public String avis() {
        return avis.poll();
    }

    public List<String> notifications() {
        return notifications;
    }

    // ------------------------------------------------------------------ les messages

    /** Un message arrive. reponses : ce qu'on pourra lui repondre (vide : il n'attend rien). */
    public void recevoir(String de, String texte, List<Reponse> reponses) {
        messages.add(new Message(de, texte, false));
        nonLus.add(de);
        if (reponses.isEmpty()) {
            attentes.remove(de);
        } else {
            attentes.put(de, List.copyOf(reponses));
        }
        prevenir("NOUVEAU MESSAGE. P : TÉLÉPHONE.");
    }

    /** Envoie la reponse choisie. Renvoie les drapeaux qu'elle leve dans l'histoire ("" : aucun). */
    public String repondre(String de, int rang) {
        List<Reponse> possibles = attentes.remove(de);
        if (possibles == null || rang < 0 || rang >= possibles.size()) {
            return "";
        }
        if (!possibles.get(rang).silencieuse()) {                      // "(Ne rien repondre.)" : un geste, pas un message
            messages.add(new Message(de, possibles.get(rang).texte(), true));
        }
        String drapeaux = possibles.get(rang).drapeaux();
        return drapeaux.equals("-") ? "" : drapeaux;
    }

    /** Ecrit soi-meme a quelqu'un (une demande faite depuis les contacts). */
    public void ecrire(String a, String texte) {
        messages.add(new Message(a, texte, true));
    }

    public List<Reponse> reponses(String de) {
        return attentes.getOrDefault(de, List.of());
    }

    /** Les conversations, la plus recente d'abord. */
    public List<String> fils() {
        List<String> fils = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (!fils.contains(messages.get(i).de())) {
                fils.add(messages.get(i).de());
            }
        }
        return fils;
    }

    public List<Message> fil(String de) {
        return messages.stream().filter(m -> m.de().equals(de)).toList();
    }

    public void lire(String de) {
        nonLus.remove(de);
    }

    public boolean nonLu(String de) {
        return nonLus.contains(de);
    }

    public int nonLus() {
        return nonLus.size();
    }

    // ------------------------------------------------------------------ les commandes et les colis

    public boolean possede(String objet) {
        return inventaire.possede(objet);
    }

    /** Les objets achetes qu'on a en poche (la categorie "objets" de l'inventaire, hors objets de l'histoire). */
    public List<String> objets() {
        return inventaire.lignes(Inventaire.Categorie.OBJETS).stream().map(l -> l.id).filter(id -> article(id) != null).toList();
    }

    /** Vrai si l'objet est deja a soi, en route, ou dans un colis pas encore ouvert. */
    public boolean dejaPris(String objet) {
        return possede(objet) || commandes.containsKey(objet) || colis.contains(objet);
    }

    public boolean enRoute(String objet) {
        return commandes.containsKey(objet);
    }

    /** Commande un objet : on paie tout de suite, il sera livre chez soi. Renvoie faux si l'argent manque ou s'il est deja pris. */
    public boolean commander(String objet) {
        Article article = article(objet);
        if (article == null || dejaPris(objet) || !payer(article.prix(), "COMMANDE : " + article.nom())) {
            return false;
        }
        commandes.put(objet, DELAI);
        prevenir("COMMANDE CONFIRMÉE : " + article.nom() + ". LIVRAISON CHEZ TOI.");
        return true;
    }

    /** Le temps de jeu passe : les commandes approchent, puis le colis est pose devant la porte, chez soi. */
    public void livrer(float dt) {
        for (String objet : new ArrayList<>(commandes.keySet())) {
            float reste = commandes.get(objet) - dt;
            if (reste > 0f) {
                commandes.put(objet, reste);
            } else {
                commandes.remove(objet);
                colis.add(objet);
                prevenir("TON COLIS EST ARRIVÉ CHEZ TOI : " + article(objet).nom() + ".");
            }
        }
    }

    public List<String> colis() {
        return colis;
    }

    /** Ouvre les colis qui attendent a la maison. Renvoie ce qu'ils contenaient. */
    public List<String> ouvrirColis() {
        List<String> ouverts = new ArrayList<>(colis);
        for (String o : colis) {
            inventaire.ajouter(o, 1);                                  // le colis ouvert : l'objet rejoint l'inventaire
        }
        colis.clear();
        return ouverts;
    }

    /** Se sert d'un objet qui ne sert qu'une fois (la lacrymo). Renvoie faux si on ne l'a pas. */
    public boolean utiliser(String objet) {
        return inventaire.retirer(objet, 1);
    }

    /** Un coup : le gilet en prend la moitie. */
    public void coup(float degats) {
        blesser(possede("gilet") ? degats / 2f : degats, 0f);
    }

    // ------------------------------------------------------------------ l'arsenal

    /**
     * La famille d'une arme : elle decide de ce que fait la touche A. Les armes de contact frappent (une barre ne tire
     * pas) ; les armes a feu tirent de loin et laissent les poings faire le travail de pres.
     */
    public enum Famille {
        IMPROVISEE, CONTONDANT, LAME, PISTOLET, MITRAILLETTE, FUSIL;

        public boolean aFeu() {
            return this == PISTOLET || this == MITRAILLETTE || this == FUSIL;
        }
    }

    /**
     * Une arme. Pour toutes : degats, portee (pixels), duree du geste ou de l'intervalle entre deux tirs (secondes), recul
     * donne a l'ennemi (pixels par seconde), coups avant de casser (0 : ne casse pas), couleur et longueur du dessin dans
     * la main. Pour une arme a feu : cadence (tirs par seconde), dispersion (radians, au premier tir), echauffement (ce que
     * chaque tir ajoute a la dispersion), chargeur, temps de rechargement, munition (l'objet de l'inventaire), automatique
     * (A maintenu tire en continu), perce (combien de corps la balle traverse). rang : le moment de l'histoire ou elle
     * devient trouvable (1 : des le debut ; 4 : les missions les plus dures). relique : unique, jamais dupliquee.
     */
    public record Arme(String id, String nom, Famille famille, float degats, float portee, float duree, float recul, int coups,
                       int couleur, int longueur, String phrase, float cadence, float dispersion, float echauffement, int chargeur,
                       float rechargement, String munition, boolean automatique, int perce, int rang, boolean relique) {
        /** Une arme de contact (la forme d'avant l'arsenal). */
        public Arme(String id, String nom, float degats, float portee, float duree, float recul, int coups, int couleur, int longueur,
                    String phrase) {
            this(id, nom, Famille.IMPROVISEE, degats, portee, duree, recul, coups, couleur, longueur, phrase, 0, 0, 0, 0, 0, null,
                    false, 0, 1, false);
        }

        public boolean aFeu() {
            return famille.aFeu();
        }

        /** Le dessin de l'arme dans la main, pour une arme a feu (arme_pistolet...), ou null. */
        public String dessin() {
            return aFeu() ? "arme_" + id : null;
        }

        /** Une arme longue se tient a deux mains, la crosse a l'epaule. */
        public boolean deuxMains() {
            return famille == Famille.MITRAILLETTE || famille == Famille.FUSIL;
        }
    }

    private static Arme contact(String id, String nom, Famille f, float degats, float portee, float duree, float recul, int coups,
                                int couleur, int longueur, int rang, boolean relique, String phrase) {
        return new Arme(id, nom, f, degats, portee, duree, recul, coups, couleur, longueur, phrase, 0, 0, 0, 0, 0, null, false, 0,
                rang, relique);
    }

    private static Arme feu(String id, String nom, Famille f, float degats, float portee, float cadence, float dispersion,
                            float echauffement, int chargeur, float rechargement, String munition, boolean auto, int perce, float recul,
                            int rang, String phrase) {
        return new Arme(id, nom, f, degats, portee, 1f / cadence, recul, 0, 0x3A3C44, 8, phrase, cadence, dispersion, echauffement,
                chargeur, rechargement, munition, auto, perce, rang, false);
    }

    public static final List<Arme> ARMES = List.of(
            new Arme("barre", "BARRE DE FER", 28, 40, 0.55f, 160, 0, 0x8A8E98, 18, "Lourde et lente, mais elle couche n'importe qui."),
            new Arme("tuyau", "TUYAU DE PLOMB", 22, 36, 0.45f, 120, 14, 0x9A9488, 15, "Plus leger que la barre. Il finira par plier."),
            new Arme("planche", "PLANCHE CLOUÉE", 18, 34, 0.42f, 90, 6, 0x9C7448, 16, "Elle ne tiendra pas longtemps."),
            new Arme("bouteille", "BOUTEILLE", 20, 26, 0.32f, 70, 1, 0x3E7A52, 9, "Un seul coup, et elle eclate."),
            contact("batte", "BATTE DE BASEBALL", Famille.CONTONDANT, 24, 38, 0.44f, 220, 0, 0xC4A06E, 17, 2, false,
                    "Deux elans : de cote, puis par-dessus. Le second envoie voler."),
            contact("katana", "KATANA", Famille.LAME, 22, 36, 0.3f, 90, 0, 0xD6DAE2, 20, 3, false,
                    "Rapide. Trois coupes ; la troisieme souleve."),
            contact("katana_relique", "KATANA RELIQUE", Famille.LAME, 30, 42, 0.28f, 120, 0, 0xB4A0F0, 22, 4, true,
                    "La lame chante. Quatre coupes ; la derniere libere une onde."),
            feu("pistolet", "PISTOLET", Famille.PISTOLET, 14, 230, 3.5f, 0.03f, 0.03f, 8, 1.1f, "balles_9", false, 0, 90, 2,
                    "Precis, coup par coup. Huit balles."),
            feu("pistolet_lourd", "PISTOLET LOURD", Famille.PISTOLET, 28, 260, 1.7f, 0.02f, 0.06f, 6, 1.5f, "balles_45", false, 1, 230, 4,
                    "Lent, brutal : la balle traverse et couche."),
            feu("mitraillette", "MITRAILLETTE", Famille.MITRAILLETTE, 8, 175, 11f, 0.07f, 0.025f, 25, 1.7f, "balles_9", true, 0, 50, 3,
                    "A maintenu : une rafale. Elle s'ecarte vite."),
            feu("fusil", "FUSIL D'ASSAUT", Famille.FUSIL, 18, 320, 7f, 0.035f, 0.015f, 30, 2.1f, "balles_fusil", true, 0, 120, 4,
                    "Loin et fort. Ses munitions sont rares."));

    public static Arme arme(String id) {
        return ARMES.stream().filter(a -> a.id().equals(id)).findFirst().orElse(null);
    }

    /** Tout ce qu'on a sur soi : livraisons, armes, objets. */
    private final Inventaire inventaire = new Inventaire();

    public Inventaire inventaire() {
        return inventaire;
    }
    /** Les petits et les operations qu'on leur confie : leur argent passe par cette banque-ci, la seule. */
    private final Organisation organisation = new Organisation();

    public Organisation organisation() {
        return organisation;
    }
    /** Les poubelles deja fouillees pendant la mission en cours (on peut y revenir a la mission suivante). */
    private final Set<String> fouillees = new HashSet<>();

    /** L'arme en main, ou null : les poings. */
    public Arme armeEnMain() {
        Inventaire.Ligne l = inventaire.equipee();
        return l == null ? null : arme(l.id);
    }

    /**
     * Prend une arme : elle va dans le sac et dans la main. Le sac tient trois armes ; au-dela, celle qu'on avait en
     * main reste la. Renvoie l'arme laissee, ou null si on garde tout.
     */
    public Arme prendreArme(String id) {
        Arme a = arme(id);
        if (a != null && a.relique() && inventaire.possede(id)) {      // une relique n'existe qu'une fois
            return null;
        }
        Inventaire.Ligne laissee = inventaire.prendreArme(id, a != null && a.aFeu() ? a.chargeur() : 0);
        return laissee == null ? null : arme(laissee.id);
    }

    /** Prend une arme la ou l'on est : si le sac est plein, celle qu'on tenait reste par terre ici (avec son usure). */
    public Arme prendreArme(String id, String carte, float x, float y) {
        Arme a = arme(id);
        if (a != null && a.relique() && inventaire.possede(id)) {
            return null;
        }
        Inventaire.Ligne laissee = inventaire.prendreArme(id, a != null && a.aFeu() ? a.chargeur() : 0);
        if (laissee == null) {
            return null;
        }
        auSol.add(new AuSol(laissee.id, laissee.usure, carte, x, y));
        return arme(laissee.id);
    }

    /** Les balles dans le chargeur de l'arme en main (une arme a feu range ce compte dans son usure), ou -1. */
    public int chargees() {
        Arme a = armeEnMain();
        return a == null || !a.aFeu() ? -1 : inventaire.equipee().usure;
    }

    /** Les balles en reserve pour l'arme en main (dans les poches), ou -1. */
    public int reserve() {
        Arme a = armeEnMain();
        return a == null || !a.aFeu() ? -1 : inventaire.quantite(a.munition());
    }

    /** Une balle part : renvoie faux si le chargeur est vide. */
    public boolean tirer() {
        Arme a = armeEnMain();
        if (a == null || !a.aFeu() || inventaire.equipee().usure <= 0) {
            return false;
        }
        inventaire.equipee().usure--;
        return true;
    }

    /** Recharge l'arme en main avec ce qu'on a en poche. Renvoie le nombre de balles glissees dans le chargeur. */
    public int recharger() {
        Arme a = armeEnMain();
        if (a == null || !a.aFeu()) {
            return 0;
        }
        Inventaire.Ligne l = inventaire.equipee();
        int n = Math.min(a.chargeur() - l.usure, inventaire.quantite(a.munition()));
        if (n <= 0) {
            return 0;
        }
        inventaire.consommer(a.munition(), n);
        l.usure += n;
        return n;
    }

    /** L'arme en main sort de l'inventaire (elle a casse, ou on l'a jetee). */
    public void lacherArme() {
        inventaire.lacher();
    }

    /** Une arme posee par terre : elle reste ou on l'a laissee, et se ramasse avec son usure. */
    public record AuSol(String arme, int usure, String carte, float x, float y) {
    }

    private final List<AuSol> auSol = new ArrayList<>();

    /** Lache l'arme en main a cet endroit : elle reste par terre, on peut revenir la chercher. */
    public void poserArme(String carte, float x, float y) {
        Inventaire.Ligne l = inventaire.equipee();
        if (l == null) {
            return;
        }
        poser(l, carte, x, y);
    }

    /** Pose cette arme du sac par terre (depuis l'inventaire). */
    public void poser(Inventaire.Ligne l, String carte, float x, float y) {
        if (l == null || l.fiche().usage() != Inventaire.Usage.EQUIPER || l.fiche().mission()) {
            return;                                                    // les munitions et la relique ne se posent pas
        }
        auSol.add(new AuSol(l.id, l.usure, carte, x, y));
        while (auSol.size() > 12) {                                    // le quartier ne garde pas tout : les plus vieilles partent
            auSol.remove(0);
        }
        inventaire.enlever(l);
    }

    public List<AuSol> auSol() {
        return auSol;
    }

    /** Ramasse une arme posee : dans le sac, et dans la main. Si le sac est plein, celle qu'on tenait prend sa place par terre. */
    public void ramasser(AuSol posee) {
        if (!auSol.remove(posee)) {
            return;
        }
        Inventaire.Ligne laissee = inventaire.prendreArme(posee.arme(), posee.usure());
        if (laissee != null) {
            auSol.add(new AuSol(laissee.id, laissee.usure, posee.carte(), posee.x(), posee.y()));
        }
    }

    /** L'arme a servi : renvoie vrai si elle vient de casser. */
    public boolean userArme() {
        Arme a = armeEnMain();
        if (a == null || a.coups() == 0) {
            return false;
        }
        Inventaire.Ligne l = inventaire.equipee();
        l.usure++;
        if (l.usure >= a.coups()) {
            lacherArme();
            return true;
        }
        return false;
    }

    public int coupsRestants() {
        Arme a = armeEnMain();
        return a == null || a.coups() == 0 ? -1 : a.coups() - inventaire.equipee().usure;
    }

    public boolean fouillee(String id) {
        return fouillees.contains(id);
    }

    public void fouiller(String id) {
        fouillees.add(id);
    }

    /** Une nouvelle mission : les poubelles ont ete videes et remplies. */
    public void nouvellesPoubelles() {
        fouillees.clear();
    }

    // ------------------------------------------------------------------ les commerces et le loyer

    public Set<String> commerces() {
        return commerces;
    }

    public boolean acheter(Commerce commerce) {
        if (commerces.contains(commerce.id()) || !payer(commerce.prix(), "ACHAT : " + commerce.nom())) {
            return false;
        }
        commerces.add(commerce.id());
        prevenir(commerce.nom() + " EST À TOI. RECETTE : " + commerce.recette() + " EUROS PAR MISSION.");
        return true;
    }

    public int recettes() {
        return COMMERCES.stream().filter(c -> commerces.contains(c.id())).mapToInt(Commerce::recette).sum();
    }

    /**
     * Le loyer du 12 suit le calendrier, et lui seul : trois mois de retard au debut de l'histoire (la troisieme relance
     * de la regie), puis un mois de plus le 5 de chaque mois (jours 5, 35, 65...). Ce qui est du, c'est ce qui est
     * tombe moins ce qui a ete paye : la banque, le telephone, les recapitulatifs lisent tous ce meme chiffre.
     */
    public static final int ARRIERES = 3;
    public static final int JOUR_DU_LOYER = 5;

    public int loyersTombes() {
        int jour = calendrier.jour();
        return ARRIERES + (jour >= JOUR_DU_LOYER ? 1 + (jour - JOUR_DU_LOYER) / 30 : 0);
    }

    public int loyerDu() {
        return Math.max(0, loyersTombes() - loyersPayes);
    }

    /** Quatre mois de retard : la regie coupe le courant, on ne dort plus chez soi. */
    public boolean coupe() {
        return loyerDu() >= ARRIERES + 1;
    }

    /** Paie un mois de loyer a la regie. */
    public boolean payerLoyer() {
        if (loyerDu() <= 0 || !payer(LOYER, "LOYER DU 12 (LA RÉGIE)")) {
            return false;
        }
        loyersPayes++;
        prevenir(loyerDu() == 0 ? "LOYER À JOUR. LA RÉGIE NE RELANCE PLUS." : "LOYER PAYÉ. IL RESTE " + loyerDu() + " MOIS DE RETARD.");
        return true;
    }

    /** Paie tout le retard qu'on peut payer. Renvoie le nombre de mois regles. */
    public int payerTousLesLoyers() {
        int n = 0;
        while (loyerDu() > 0 && payerLoyer()) {
            n++;
        }
        return n;
    }

    /** Le calendrier a avance : un nouveau mois de loyer est peut-etre tombe. La regie le fait savoir, une fois. */
    public void majLoyer() {
        int tombes = loyersTombes();
        if (loyersVus < 0) {
            loyersVus = tombes;
        }
        if (tombes > loyersVus) {
            loyersVus = tombes;
            prevenir(coupe() ? "LA RÉGIE A COUPÉ LE COURANT : " + loyerDu() + " MOIS DE LOYER EN RETARD."
                    : "NOUVEAU MOIS DE LOYER : " + LOYER + " EUROS POUR LA RÉGIE, À PAYER DEPUIS TON TÉLÉPHONE (BANQUE).");
        }
    }

    /** Une mission se termine : les commerces achetes rapportent leur recette. (Le loyer, lui, suit le calendrier.) */
    public void finDeMission() {
        nouvellesPoubelles();
        for (Commerce commerce : COMMERCES) {
            if (commerces.contains(commerce.id())) {
                gagner(commerce.recette(), "RECETTE : " + commerce.nom());
                prevenir("RECETTE : " + commerce.nom() + ", + " + commerce.recette() + " EUROS.");
            }
        }
    }

    /** Le nombre de mouvements notes depuis le debut (le journal, lui, ne garde que les derniers). */
    public long notees() {
        return notees;
    }

    public List<Ligne> journal() {
        return journal;
    }

    // ------------------------------------------------------------------ la sauvegarde du telephone

    private static final String CHAMP = "\u0001";
    private static final String SUITE = "\u0002";

    private final Calendrier calendrier = new Calendrier();

    /** Le jour et l'heure. */
    public Calendrier calendrier() {
        return calendrier;
    }

    public void enregistrer(Properties p) {
        p.setProperty("tel.commerces", String.join(",", commerces));
        p.setProperty("tel.colis", String.join(",", colis));
        p.setProperty("loyer.payes", Integer.toString(loyersPayes));
        p.setProperty("journal.notees", Long.toString(notees));
        p.setProperty("loyer.vus", Integer.toString(loyersVus));
        p.setProperty("tel.nonlus", String.join(",", nonLus));
        p.setProperty("tel.commandes", String.join(SUITE, commandes.entrySet().stream().map(e -> e.getKey() + CHAMP + e.getValue()).toList()));
        p.setProperty("tel.messages", String.join(SUITE, messages.stream().map(m -> m.de() + CHAMP + m.deMoi() + CHAMP + m.texte()).toList()));
        p.setProperty("tel.journal", String.join(SUITE, journal.stream().map(l -> l.libelle() + CHAMP + l.montant()).toList()));
        p.setProperty("tel.notifications", String.join(SUITE, notifications));
        List<String> a = new ArrayList<>();
        attentes.forEach((de, reponses) -> reponses.forEach(r -> a.add(de + CHAMP + r.texte() + CHAMP + r.drapeaux())));
        p.setProperty("tel.attentes", String.join(SUITE, a));
        inventaire.enregistrer(p);
        p.setProperty("fouillees", String.join(",", fouillees));
        organisation.enregistrer(p);
        calendrier.enregistrer(p);
        p.setProperty("ausol", String.join(SUITE, auSol.stream().map(s -> s.arme() + CHAMP + s.usure() + CHAMP + s.carte() + CHAMP + s.x()
                + CHAMP + s.y()).toList()));
    }

    public void relire(Properties p) {
        commerces.clear(); colis.clear(); nonLus.clear(); commandes.clear();
        messages.clear(); journal.clear(); notifications.clear(); attentes.clear(); avis.clear();
        commerces.addAll(liste(p.getProperty("tel.commerces", ""), ","));
        colis.addAll(liste(p.getProperty("tel.colis", ""), ","));
        nonLus.addAll(liste(p.getProperty("tel.nonlus", ""), ","));
        String ancienDu = p.getProperty("tel.loyer");
        notees = Long.parseLong(p.getProperty("journal.notees", "0"));
        for (String c : liste(p.getProperty("tel.commandes", ""), SUITE)) {
            String[] m = c.split(CHAMP, -1);
            commandes.put(m[0], Float.parseFloat(m[1]));
        }
        for (String c : liste(p.getProperty("tel.messages", ""), SUITE)) {
            String[] m = c.split(CHAMP, -1);
            messages.add(new Message(m[0], m[2], Boolean.parseBoolean(m[1])));
        }
        for (String c : liste(p.getProperty("tel.journal", ""), SUITE)) {
            String[] m = c.split(CHAMP, -1);
            journal.add(new Ligne(m[0], Integer.parseInt(m[1])));
        }
        notifications.addAll(liste(p.getProperty("tel.notifications", ""), SUITE));
        notees = Math.max(notees, journal.size());
        for (String c : liste(p.getProperty("tel.attentes", ""), SUITE)) {
            String[] m = c.split(CHAMP, -1);
            attentes.computeIfAbsent(m[0], k -> new ArrayList<>()).add(new Reponse(m[1], m[2]));
        }
        if (!inventaire.relire(p)) {                                   // une partie d'avant l'inventaire : on y range ce qu'elle avait
            for (String o : liste(p.getProperty("tel.objets", ""), ",")) {
                inventaire.ajouter(o, 1);
            }
            String lue = p.getProperty("arme", "");
            if (arme(lue) != null) {
                inventaire.prendreArme(lue, Integer.parseInt(p.getProperty("arme.usure", "0")));
            }
            inventaire.prendreMouvements();
        }
        fouillees.clear();
        fouillees.addAll(liste(p.getProperty("fouillees", ""), ","));
        organisation.relire(p);
        calendrier.relire(p);
        if (p.getProperty("loyer.payes") != null) {
            loyersPayes = Integer.parseInt(p.getProperty("loyer.payes"));
            loyersVus = Integer.parseInt(p.getProperty("loyer.vus", "-1"));
        } else {                                                        // une partie d'avant : on garde ce qu'elle disait du
            loyersPayes = Math.max(0, loyersTombes() - (ancienDu == null ? 0 : Integer.parseInt(ancienDu)));
            loyersVus = -1;
        }
        auSol.clear();
        for (String c : liste(p.getProperty("ausol", ""), SUITE)) {
            String[] m = c.split(CHAMP, -1);
            if (arme(m[0]) != null) {
                auSol.add(new AuSol(m[0], Integer.parseInt(m[1]), m[2], Float.parseFloat(m[3]), Float.parseFloat(m[4])));
            }
        }
    }

    private static List<String> liste(String texte, String separateur) {
        List<String> morceaux = new ArrayList<>();
        for (String morceau : texte.split(separateur, -1)) {
            if (!morceau.isBlank()) {
                morceaux.add(morceau);
            }
        }
        return morceaux;
    }
}
