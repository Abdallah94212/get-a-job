package histoire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Tout ce que le heros a sur lui : les livraisons qu'on lui confie, les armes ramassees, les objets de l'histoire et
 * ceux qu'il a achetes. C'est la seule base des objets du jeu : les scenes donnent et reprennent (donne, remet), les
 * objectifs verifient qu'on a bien l'objet (avec:), le combat lit l'arme equipee, le telephone y range les colis
 * ouverts, la sauvegarde l'ecrit. Une ligne par objet : les objets empilables (trois sachets, six plants) partagent
 * une ligne et une quantite ; les autres (une arme et son usure, le beeper) gardent chacun la leur.
 */
public final class Inventaire {
    public enum Categorie {
        LIVRAISONS("LIVRAISONS"), ARMES("ARMES"), OBJETS("OBJETS");

        public final String nom;

        Categorie(String nom) {
            this.nom = nom;
        }
    }

    /** Ce qu'on peut faire d'un objet depuis l'inventaire. */
    public enum Usage {
        AUCUN, EQUIPER, UTILISER, PORTE, LANCER
    }

    /**
     * Un genre d'objet. pour : a qui il est destine (une livraison), ou null ; mission : il sert a l'histoire, on ne
     * peut ni le jeter ni le perdre ; valeur : ce qu'il vaut en euros, 0 si ce n'est pas la question.
     */
    public record Fiche(String id, String nom, Categorie categorie, String description, boolean empilable, boolean mission,
                        Usage usage, String pour, int valeur) {
    }

    /** Une ligne de l'inventaire. usure : les coups deja donnes avec une arme. */
    public static final class Ligne {
        public final String id;
        public int quantite;
        public int usure;

        Ligne(String id, int quantite, int usure) {
            this.id = id;
            this.quantite = quantite;
            this.usure = usure;
        }

        public Fiche fiche() {
            return Inventaire.fiche(id);
        }
    }

    /** Un objet vient d'entrer (delta > 0) ou de sortir : le jeu le montre un instant. */
    public record Mouvement(String id, int delta) {
    }

    private static final Map<String, Fiche> FICHES = new LinkedHashMap<>();

    private static void fiche(String id, String nom, Categorie c, String description, boolean empilable, boolean mission, Usage usage,
                              String pour, int valeur) {
        FICHES.put(id, new Fiche(id, nom, c, description, empilable, mission, usage, pour, valeur));
    }

    static {
        Categorie l = Categorie.LIVRAISONS;
        fiche("sac_lili", "SAC BLANC", l, "Une commande du Maître Poulet, encore chaude.", false, true, Usage.PORTE, "TATA LILI, AU 9", 12);
        fiche("sac_omar", "GROS SAC", l, "Le poulet des mécanos d'Omar. Lourd.", false, true, Usage.PORTE, "OMAR, AU GARAGE", 24);
        fiche("enveloppe", "ENVELOPPE DE GETO", l, "L'argent de Bidoche. Tu ne la comptes pas.", false, true, Usage.PORTE, "BIDOCHE, À CRIMÉE", 0);
        fiche("caisses_poulet", "CAISSES DE POULET", l, "Vingt kilos, pour la chambre froide de Geto.", false, true, Usage.PORTE,
                "LE MAÎTRE POULET", 0);
        fiche("sachets", "SACHET DE CELL", l, "Un sachet par client. Ne te fais pas voir.", true, true, Usage.PORTE, "LES CLIENTS DE GETO", 30);
        fiche("recette_geto", "L'ARGENT POUR GETO", l, "Ce que les clients ont payé. Ce n'est pas à toi : tu le rapportes au comptoir.",
                true, true, Usage.PORTE, "OLD GETO, AU COMPTOIR", 1);
        fiche("piccolo", "PLANT DE PICCOLO", l, "La plante de Namek, pour Gero.", true, true, Usage.PORTE, "GERO, AU LABO", 0);
        fiche("cell", "CELL PERFECT", l, "Ce que Gero a tiré des cuves. Pour l'épicerie.", false, true, Usage.PORTE, "L'ÉPICERIE", 0);
        Categorie a = Categorie.ARMES;
        fiche("bouteille", "BOUTEILLE", a, "Fait mal, une fois.", false, false, Usage.EQUIPER, null, 0);
        fiche("planche", "PLANCHE", a, "Lente, mais elle porte.", false, false, Usage.EQUIPER, null, 0);
        fiche("tuyau", "TUYAU", a, "Du métal creux. Il plie à la longue.", false, false, Usage.EQUIPER, null, 0);
        fiche("barre", "BARRE DE FER", a, "Lourde. Ne casse pas.", false, false, Usage.EQUIPER, null, 0);
        fiche("batte", "BATTE DE BASEBALL", a, "Du frêne, du scotch au manche. Le second élan envoie voler.", false, false, Usage.EQUIPER, null, 0);
        fiche("katana", "KATANA", a, "Une vraie lame, pas un souvenir de vacances. Trois coupes, la dernière soulève.", false, false,
                Usage.EQUIPER, null, 0);
        fiche("katana_relique", "KATANA RELIQUE", a, "Planté dans la roche de Namek depuis on ne sait quand. La lame chante quand l'éclat est près.",
                false, true, Usage.EQUIPER, null, 0);
        fiche("pistolet", "PISTOLET", a, "Le vieux pistolet de Papy Bocal. Huit balles, coup par coup.", false, false, Usage.EQUIPER, null, 0);
        fiche("pistolet_lourd", "PISTOLET LOURD", a, "Chromé, crosse en bois. Six balles qui couchent ce qu'elles touchent.", false, false,
                Usage.EQUIPER, null, 0);
        fiche("mitraillette", "MITRAILLETTE", a, "Courte, bruyante. Garde A enfoncé : elle vide son chargeur et s'écarte.", false, false,
                Usage.EQUIPER, null, 0);
        fiche("fusil", "FUSIL D'ASSAUT", a, "Ce que Geto gardait pour la suite. Porte loin. Ses cartouches sont rares.", false, false,
                Usage.EQUIPER, null, 0);
        fiche("balles_9", "BALLES DE 9 MM", a, "Pour le pistolet et la mitraillette.", true, false, Usage.AUCUN, null, 0);
        fiche("balles_45", "BALLES DE .45", a, "Pour le pistolet lourd. Grosses, rares.", true, false, Usage.AUCUN, null, 0);
        fiche("balles_fusil", "CARTOUCHES DE FUSIL", a, "Pour le fusil d'assaut. Compte-les.", true, false, Usage.AUCUN, null, 0);
        fiche("canette", "CANETTE PLEINE", a, "Ça se lance (H). Ça sonne, ça surprend, ça ne fait pas tomber.", true, false, Usage.LANCER, null, 0);
        fiche("pierre", "PIERRE", a, "Un pavé descellé. Lancé (H), ça fait mal.", true, false, Usage.LANCER, null, 0);
        Categorie o = Categorie.OBJETS;
        fiche("sandwich", "SANDWICH DE BIDOCHE", o, "Il en a fait un de trop. Ça se mange, ou ça se donne.", false, false,
                Usage.UTILISER, null, 0);
        fiche("beeper", "BEEPER VERT", o, "Le SDF te l'a mis dans la main. N'appuie pas dessus.", false, true, Usage.AUCUN, null, 0);
        fiche("boitier_orange", "BOÎTIER ORANGE", o, "Il copie les télécommandes. Il ouvre les voitures.", false, true, Usage.AUCUN, null, 0);
        fiche("eclat", "ÉCLAT IRISÉ", o, "Il ne reflète pas la lumière : il en fait.", false, true, Usage.AUCUN, null, 0);
        fiche("casque", "CASQUE", o, "En scooter, un choc ne te blesse plus.", false, false, Usage.AUCUN, null, 35);
        fiche("baskets", "BASKETS DE COURSE", o, "Courir coûte un tiers de souffle en moins.", false, false, Usage.AUCUN, null, 45);
        fiche("gilet", "GILET RENFORCÉ", o, "Les coups font moitié moins mal.", false, false, Usage.AUCUN, null, 80);
        fiche("lacrymo", "BOMBE LACRYMOGÈNE", o, "Repéré, elle te sort de là. Une fois.", false, false, Usage.AUCUN, null, 60);
    }

    /** Les drapeaux de l'histoire qui sont aussi des objets qu'on a en poche (pour reprendre une vieille sauvegarde). */
    public static final Map<String, String> DRAPEAUX = Map.of("beeper", "beeper", "boitier_orange", "boitier_orange",
            "eclat_irise", "eclat");

    public static final int ARMES_MAX = 4;

    private final List<Ligne> lignes = new ArrayList<>();
    private Ligne equipee;
    private final List<Mouvement> mouvements = new ArrayList<>();

    public static Fiche fiche(String id) {
        return FICHES.get(id);
    }

    public static boolean connu(String id) {
        return FICHES.containsKey(id);
    }

    public List<Ligne> lignes() {
        return lignes;
    }

    public List<Ligne> lignes(Categorie c) {
        return lignes.stream().filter(l -> l.fiche().categorie() == c).toList();
    }

    public int quantite(String id) {
        return lignes.stream().filter(l -> l.id.equals(id)).mapToInt(l -> l.quantite).sum();
    }

    public boolean possede(String id) {
        return quantite(id) > 0;
    }

    /** Ajoute n exemplaires. Un objet unique deja en poche n'est pas duplique. Renvoie ce qui a vraiment ete ajoute. */
    public int ajouter(String id, int n) {
        Fiche f = fiche(id);
        if (f == null || n <= 0) {
            return 0;
        }
        int ajoute = 0;
        if (f.empilable()) {
            Ligne l = lignes.stream().filter(x -> x.id.equals(id)).findFirst().orElse(null);
            if (l == null) {
                lignes.add(new Ligne(id, n, 0));
            } else {
                l.quantite += n;
            }
            ajoute = n;
        } else if (f.categorie() == Categorie.ARMES) {
            for (int i = 0; i < n; i++) {
                lignes.add(new Ligne(id, 1, 0));
                ajoute++;
            }
        } else if (!possede(id)) {                                     // un objet unique : une seule fois
            lignes.add(new Ligne(id, 1, 0));
            ajoute = 1;
        }
        if (ajoute > 0) {
            mouvements.add(new Mouvement(id, ajoute));
        }
        return ajoute;
    }

    /** Retire n exemplaires si on les a tous ; sinon ne retire rien et renvoie faux (pas de livraison a moitie). */
    public boolean retirer(String id, int n) {
        if (quantite(id) < n || n <= 0) {
            return false;
        }
        int reste = n;
        for (Ligne l : new ArrayList<>(lignes)) {
            if (reste == 0) {
                break;
            }
            if (!l.id.equals(id)) {
                continue;
            }
            int pris = Math.min(reste, l.quantite);
            l.quantite -= pris;
            reste -= pris;
            if (l.quantite == 0) {
                lignes.remove(l);
                if (l == equipee) {
                    equipee = null;
                }
            }
        }
        mouvements.add(new Mouvement(id, -n));
        return true;
    }

    /** Consomme n exemplaires sans l'annoncer (les balles glissees dans un chargeur) ; renvoie faux s'il n'y en a pas assez. */
    public boolean consommer(String id, int n) {
        int avant = mouvements.size();
        boolean ok = retirer(id, n);
        while (mouvements.size() > avant) {
            mouvements.remove(mouvements.size() - 1);
        }
        return ok;
    }

    /** L'arme suivante, pour changer d'arme sans ouvrir le sac : les poings, puis chaque arme, puis les poings. */
    public Ligne armeSuivante() {
        List<Ligne> armes = lignes.stream().filter(l -> l.fiche().usage() == Usage.EQUIPER).toList();
        if (armes.isEmpty()) {
            return null;
        }
        int i = equipee == null ? -1 : armes.indexOf(equipee);
        return i + 1 < armes.size() ? armes.get(i + 1) : null;
    }

    /** Enleve cette ligne-la (une arme posee par terre depuis le sac). */
    public void enlever(Ligne l) {
        if (lignes.remove(l)) {
            if (l == equipee) {
                equipee = null;
            }
            mouvements.add(new Mouvement(l.id, -l.quantite));
        }
    }

    /** Les mouvements depuis la derniere fois qu'on a demande (pour l'annoncer a l'ecran). */
    public List<Mouvement> prendreMouvements() {
        List<Mouvement> m = new ArrayList<>(mouvements);
        mouvements.clear();
        return m;
    }

    // ------------------------------------------------------------------ les armes

    public Ligne equipee() {
        return equipee;
    }

    public void equiper(Ligne l) {
        if (l == null || lignes.contains(l) && l.fiche().usage() == Usage.EQUIPER) {
            equipee = l;
        }
    }

    /** Les armes qu'on tient en main (pas les projectiles) : le sac en porte quatre. */
    public long armes() {
        return lignes.stream().filter(l -> l.fiche().usage() == Usage.EQUIPER).count();
    }

    /**
     * Une arme ramassee : elle va dans le sac et dans la main. Le sac ne tient que quatre armes : au-dela, celle qu'on
     * avait en main reste par terre (renvoyee, pour le dire au joueur).
     */
    public Ligne prendreArme(String id, int usure) {
        Ligne laissee = null;
        if (armes() >= ARMES_MAX) {
            laissee = equipee != null ? equipee : lignes.stream().filter(l -> l.fiche().usage() == Usage.EQUIPER).findFirst().orElse(null);
            lignes.remove(laissee);
        }
        Ligne nouvelle = new Ligne(id, 1, usure);
        lignes.add(nouvelle);
        equipee = nouvelle;
        mouvements.add(new Mouvement(id, 1));
        return laissee;
    }

    /** L'arme en main sort de l'inventaire (cassee, ou posee par terre). */
    public Ligne lacher() {
        Ligne l = equipee;
        if (l != null) {
            lignes.remove(l);
            equipee = null;
            mouvements.add(new Mouvement(l.id, -1));
        }
        return l;
    }

    // ------------------------------------------------------------------ la sauvegarde

    void enregistrer(Properties p) {
        List<String> l = new ArrayList<>();
        for (Ligne x : lignes) {
            l.add(x.id + ":" + x.quantite + ":" + x.usure);
        }
        p.setProperty("inv", String.join(",", l));
        p.setProperty("inv.main", Integer.toString(equipee == null ? -1 : lignes.indexOf(equipee)));
    }

    /** Relit l'inventaire. Renvoie faux s'il n'y en a pas (une sauvegarde d'avant l'inventaire). */
    boolean relire(Properties p) {
        lignes.clear();
        equipee = null;
        mouvements.clear();
        String texte = p.getProperty("inv");
        if (texte == null) {
            return false;
        }
        for (String c : texte.split(",")) {
            String[] m = c.split(":");
            if (m.length == 3 && connu(m[0])) {
                lignes.add(new Ligne(m[0], Integer.parseInt(m[1]), Integer.parseInt(m[2])));
            }
        }
        int main = Integer.parseInt(p.getProperty("inv.main", "-1"));
        equipee = main >= 0 && main < lignes.size() ? lignes.get(main) : null;
        return true;
    }

    void vider() {
        lignes.clear();
        equipee = null;
        mouvements.clear();
    }
}
