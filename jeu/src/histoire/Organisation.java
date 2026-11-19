package histoire;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/**
 * L'organisation du heros : ses petits (Youngssef, Savon, Ticket) et les operations qu'il leur confie.
 *
 * <p>Il n'y a qu'une banque, celle du heros (Etat) : chaque euro passe par elle, avec un libelle. Lancer une
 * operation, c'est payer tout de suite les frais et la paie convenue (l'argent est engage) ; quand elle se termine,
 * la recette tombe (et la cuve de Gero se paie sur la Cell qui sort). Le bilan dit ce que l'operation a vraiment
 * rapporte, net.
 *
 * <p>Deux jauges par petit, distinctes : la fiabilite (sa capacite a mener une operation a bien : elle monte avec
 * l'experience) et la confiance (sa relation avec le heros : elle monte quand on le paie bien et qu'on tient parole,
 * elle baisse quand on est radin ou que ca tourne mal). La fiabilite pese sur la reussite et les retards ; la
 * confiance decide de ce qu'il accepte (pas d'operation risquee pour quelqu'un qui ne lui fait pas confiance) et de
 * ce qu'il garde pour lui. Chacun a un talent : sur une operation de son talent, il est bien meilleur.
 */
public final class Organisation {
    /** Ce qu'une operation demande : la route (livrer, aller chercher), le comptoir (vendre), le guet (attendre, recouvrer). */
    public enum Talent {
        ROUTE("LA ROUTE"), COMPTOIR("LE COMPTOIR"), GUET("LE GUET");

        public final String nom;

        Talent(String nom) {
            this.nom = nom;
        }
    }

    /** La paie qu'on propose : radin, juste, ou avec une prime. Elle change la confiance, et un peu la reussite. */
    public enum Paie {
        RADIN("RADIN", 0.7f, -6, -8), JUSTE("JUSTE", 1f, 1, 0), PRIME("AVEC PRIME", 1.4f, 4, 4);

        public final String nom;
        public final float facteur;
        public final int confiance;
        public final int chance;

        Paie(String nom, float facteur, int confiance, int chance) {
            this.nom = nom;
            this.facteur = facteur;
            this.confiance = confiance;
            this.chance = chance;
        }
    }

    /** Un petit de l'equipe. */
    public static final class Membre {
        public final String id;
        public final String nom;
        public final Talent talent;
        public final String portrait;
        public int fiabilite;
        public int confiance;
        public String occupation;
        public int reussites;
        public int echecs;

        Membre(String id, String nom, Talent talent, String portrait, int fiabilite, int confiance) {
            this.id = id;
            this.nom = nom;
            this.talent = talent;
            this.portrait = portrait;
            this.fiabilite = fiabilite;
            this.confiance = confiance;
        }

        public boolean libre() {
            return occupation == null;
        }
    }

    /**
     * Une operation qu'on peut confier : ses frais (paye au depart, et ce qu'ils couvrent), la paie de base du petit,
     * la recette esperee, la part du labo de Gero (prise sur la recette), sa duree (en secondes de jeu), sa
     * difficulte (0 : facile), la confiance qu'il faut pour l'accepter, le drapeau qui la rend possible.
     */
    public record Operation(String id, String nom, String resume, Talent talent, int frais, String quoi, int paie, int recette,
                            int labo, float duree, int difficulte, int confianceMin, String condition) {
    }

    public static final List<Operation> OPERATIONS = List.of(
            new Operation("livraisons", "LES LIVRAISONS DU SOIR", "Trois adresses, le scooter, la Cell rangée derrière les paquets de riz.",
                    Talent.ROUTE, 20, "ESSENCE DU SCOOTER", 40, 190, 0, 120, 20, 0, "+organisation"),
            new Operation("comptoir", "LE COMPTOIR DE L'ÉPICERIE", "Tenir la caisse, vendre du riz devant et de la Cell derrière. Le stock se paie d'avance.",
                    Talent.COMPTOIR, 60, "STOCK DE L'ÉPICERIE", 35, 150, 0, 150, 30, 0, "+organisation"),
            new Operation("ardoise", "L'ARDOISE D'OMAR", "Omar paie toujours « demain ». Quelqu'un de patient doit être là, demain.",
                    Talent.GUET, 0, "", 30, 110, 0, 90, 45, 20, "+organisation"),
            new Operation("namek", "LA COURSE DE NAMEK", "La RS6, six heures de route, la récolte, la douane. Gero cuit, l'épicerie écoule la Cell Perfect.",
                    Talent.ROUTE, 150, "ESSENCE, PÉAGE ET PLANTS", 200, 2500, 900, 300, 40, 50, "+gero_cadence"));

    public static Operation operation(String id) {
        return OPERATIONS.stream().filter(o -> o.id().equals(id)).findFirst().orElse(null);
    }

    /** Une operation en cours : qui, quoi, ce qu'on lui a paye, le temps qui reste. */
    public static final class EnCours {
        public final String operation;
        public final String membre;
        public final Paie paie;
        public final int paye;
        public final int frais;
        public float reste;
        public final float total;
        public boolean retarde;

        EnCours(String operation, String membre, Paie paie, int paye, int frais, float total) {
            this.operation = operation;
            this.membre = membre;
            this.paie = paie;
            this.paye = paye;
            this.frais = frais;
            this.reste = total;
            this.total = total;
        }
    }

    /** Le compte rendu d'une operation terminee. net : ce qu'elle a rapporte, frais et paie deduits. */
    public record Bilan(String operation, String membre, Issue issue, int recette, int frais, int paie, int labo, int net, String message) {
    }

    /** Comment ca s'est passe. */
    public enum Issue {
        REUSSIE("RÉUSSIE"), MOINS("MOINS QUE PRÉVU"), RATEE("RATÉE");

        public final String nom;

        Issue(String nom) {
            this.nom = nom;
        }
    }

    private final Map<String, Membre> membres = new LinkedHashMap<>();
    private final List<EnCours> enCours = new ArrayList<>();
    private final List<Bilan> bilans = new ArrayList<>();
    private long graine = 1906;
    private int totalRecettes;
    private int totalPaies;
    private int totalFrais;
    private int totalLabo;

    // ------------------------------------------------------------------ l'equipe

    /**
     * Les petits rejoignent l'equipe quand l'histoire les a recrutes. Leur confiance de depart vient de ce qui s'est
     * passe avec le heros : Youngssef se souvient de la part qu'on lui a donnee (ou pas) et de la promesse.
     */
    public void recruter(Progression progression) {
        if (progression.drapeau("recrue_youngssef") && !membres.containsKey("youngssef")) {
            int confiance = 50 + (progression.drapeau("youngssef_paye") ? 10 : -10) + (progression.drapeau("youngssef_paye_boitier") ? 5 : 0)
                    + (progression.drapeau("youngssef_promis") ? 10 : 0);
            membres.put("youngssef", new Membre("youngssef", "YOUNGSSEF", Talent.ROUTE, "buste_youngssef", 78, confiance));
        }
        if (progression.drapeau("recrue_rayan") && !membres.containsKey("rayan")) {
            membres.put("rayan", new Membre("rayan", "SAVON", Talent.COMPTOIR, "buste_rayan", 52, 50));
        }
        if (progression.drapeau("recrue_moussa") && !membres.containsKey("moussa")) {
            membres.put("moussa", new Membre("moussa", "TICKET", Talent.GUET, "buste_moussa", 64, 34));
        }
    }

    /** Ce qu'on sait d'eux, en une phrase : leurs atouts et leurs limites. */
    public static String portrait(String id) {
        return switch (id) {
            case "youngssef" -> "Rapide, sérieux, il connaît chaque porte du quartier. Le meilleur sur la route. Il veut être respecté, et payé.";
            case "rayan" -> "Il parle à tout le monde et vend n'importe quoi. Distrait, et il fait des prix à ses cousins.";
            case "moussa" -> "Patient, calme, il ne lâche jamais une dette. Il ne travaille que pour l'argent : paie-le bien.";
            default -> "";
        };
    }

    public List<Membre> membres() {
        return new ArrayList<>(membres.values());
    }

    public Membre membre(String id) {
        return membres.get(id);
    }

    public boolean active() {
        return !membres.isEmpty();
    }

    // ------------------------------------------------------------------ les operations

    /** Les operations que l'histoire permet maintenant. */
    public List<Operation> disponibles(Progression progression) {
        return OPERATIONS.stream().filter(o -> progression.verifie(o.condition())).toList();
    }

    /** La paie demandee pour cette operation, selon ce qu'on propose. */
    public static int paie(Operation op, Paie paie) {
        return Math.round(op.paie() * paie.facteur);
    }

    /** Ses chances de reussir, en pourcentage. */
    public int chance(Operation op, Membre m, Paie paie) {
        float c = 100 - op.difficulte() + (m.fiabilite - 50) * 0.6f + (m.talent == op.talent() ? 15 : -8) + (m.confiance - 50) * 0.15f
                + paie.chance;
        return Math.max(5, Math.min(96, Math.round(c)));
    }

    /** La duree attendue : quelqu'un de peu fiable, ou hors de son talent, met plus longtemps. */
    public float duree(Operation op, Membre m) {
        float k = 1 + Math.max(0, 60 - m.fiabilite) / 100f + (m.talent == op.talent() ? 0 : 0.25f) + (m.id.equals("moussa") ? 0.2f : 0);
        return op.duree() * k;
    }

    /** Pourquoi cette operation ne peut pas partir avec ce petit, ou null si elle le peut. */
    public String refus(Operation op, Membre m, Paie paie, Etat etat) {
        if (!m.libre()) {
            return m.nom + " EST DÉJÀ SUR UNE OPÉRATION.";
        }
        if (enCours.stream().anyMatch(e -> e.operation.equals(op.id()))) {
            return "CETTE OPÉRATION EST DÉJÀ EN COURS.";
        }
        if (m.confiance < op.confianceMin()) {
            return m.nom + " NE TE FAIT PAS ASSEZ CONFIANCE POUR ÇA.";
        }
        if (paie == Paie.RADIN && m.confiance < 30) {
            return m.nom + " REFUSE DE TRAVAILLER À CE PRIX.";
        }
        if (etat.argent() < op.frais() + paie(op, paie)) {
            return "IL TE MANQUE " + (op.frais() + paie(op, paie) - etat.argent()) + " EUROS POUR LANCER ÇA.";
        }
        return null;
    }

    /**
     * Lance l'operation : les frais et la paie sortent tout de suite de la banque. Renvoie null si elle part, ou la
     * raison du refus. Une operation n'est payee qu'une fois : c'est ici, au depart, et nulle part ailleurs.
     */
    public String lancer(String opId, String membreId, Paie paie, Etat etat) {
        Operation op = operation(opId);
        Membre m = membres.get(membreId);
        if (op == null || m == null) {
            return "OPÉRATION INCONNUE.";
        }
        String refus = refus(op, m, paie, etat);
        if (refus != null) {
            return refus;
        }
        int salaire = paie(op, paie);
        if (op.frais() > 0) {
            etat.payer(op.frais(), "FRAIS : " + op.quoi());
        }
        etat.payer(salaire, "PAIE DE " + m.nom + " : " + op.nom());
        totalFrais += op.frais();
        totalPaies += salaire;
        m.occupation = op.id();
        enCours.add(new EnCours(op.id(), m.id, paie, salaire, op.frais(), duree(op, m)));
        return null;
    }

    public List<EnCours> enCours() {
        return new ArrayList<>(enCours);
    }

    public List<Bilan> bilans() {
        return new ArrayList<>(bilans);
    }

    /**
     * Le temps passe (secondes de jeu ; une nuit de sommeil en vaut 300). Les operations terminees se reglent : la
     * recette entre, la cuve de Gero sort, les jauges bougent, le petit envoie un message. Renvoie les bilans du moment.
     */
    public List<Bilan> avancer(float secondes, Etat etat) {
        List<Bilan> finis = new ArrayList<>();
        Random hasard = new Random(graine);
        for (EnCours e : new ArrayList<>(enCours)) {
            e.reste -= secondes;
            if (e.reste > 0) {
                continue;
            }
            Membre m = membres.get(e.membre);
            Operation op = operation(e.operation);
            // quelqu'un de peu fiable prend du retard (une fois) : on le sait, il previent
            if (!e.retarde && hasard.nextInt(100) < Math.max(0, 62 - m.fiabilite)) {
                e.retarde = true;
                e.reste = e.total * 0.4f;
                etat.prevenir(m.nom + " A DU RETARD : " + op.nom() + ".");
                etat.recevoir(m.id, retard(m.id), List.of());
                continue;
            }
            enCours.remove(e);
            m.occupation = null;
            finis.add(regler(op, m, e, hasard.nextInt(100), hasard.nextFloat(), etat));
        }
        graine = hasard.nextLong();
        return finis;
    }

    /** Une operation se termine : selon le tirage, elle reussit, rapporte moins que prevu, ou rate. */
    private Bilan regler(Operation op, Membre m, EnCours e, int tirage, float part, Etat etat) {
        int chance = chance(op, m, e.paie);
        Issue issue;
        int recette;
        if (tirage < chance) {
            issue = Issue.REUSSIE;
            recette = Math.round(op.recette() * (0.88f + part * 0.27f));
        } else if (tirage < chance + (100 - chance) / 2) {
            issue = Issue.MOINS;
            recette = Math.round(op.recette() * (0.15f + part * 0.3f));
        } else {
            issue = Issue.RATEE;
            recette = 0;
        }
        boolean garde = issue != Issue.RATEE && (e.paie == Paie.RADIN || m.confiance < 30) && part < 0.5f;
        if (garde) {                                                    // mal paye, ou pas confiance : il se sert
            recette = Math.round(recette * 0.7f);
        }
        int labo = recette > 0 ? Math.min(op.labo(), recette) : 0;
        if (recette > 0) {
            etat.gagner(recette, (op.id().equals("ardoise") ? "RECOUVRÉ : " : "VENTE : ") + op.nom());
        }
        if (labo > 0) {
            etat.payer(labo, "LABO DE GERO : UNE CUVE");
        }
        totalRecettes += recette;
        totalLabo += labo;
        int net = recette - labo - e.frais - e.paye;
        // les jauges : l'experience fait la fiabilite ; la paie, le resultat et le respect font la confiance
        if (issue == Issue.REUSSIE) {                                   // l'experience vient lentement, et plafonne
            m.fiabilite = Math.min(92, m.fiabilite + (m.reussites % 2 == 0 ? 1 : 0));
            m.reussites++;
        } else {
            m.echecs++;
        }
        m.confiance = Math.max(0, Math.min(100, m.confiance + e.paie.confiance + (issue == Issue.RATEE ? -3 : issue == Issue.MOINS ? -1 : 0)
                + (m.id.equals("moussa") && e.paie == Paie.PRIME ? 2 : 0)));
        String message = message(m.id, issue, garde);
        Bilan bilan = new Bilan(op.id(), m.id, issue, recette, e.frais, e.paye, labo, net, message);
        bilans.add(bilan);
        while (bilans.size() > 60) {
            bilans.remove(0);
        }
        etat.recevoir(m.id, message, List.of());
        etat.prevenir(op.nom() + " : " + issue.nom + ". " + (net >= 0 ? "+ " : "- ") + Math.abs(net) + " EUROS NET.");
        return bilan;
    }

    /** Ce que le petit ecrit, avec sa voix : Youngssef bref, Savon bavard, Ticket factuel. */
    private static String message(String id, Issue issue, boolean garde) {
        String suffixe = garde ? (id.equals("rayan") ? " J'ai pris un petit quelque chose pour moi. Normal, non ?"
                : id.equals("moussa") ? " J'ai retenu ma commission. Tu payais pas assez." : " J'ai gardé ma part. On en reparle.") : "";
        return switch (id) {
            case "youngssef" -> switch (issue) {
                case REUSSIE -> "Livré. Zéro problème.";
                case MOINS -> "Un client a payé la moitié. Il dit demain. Je le connais, il paiera pas.";
                case RATEE -> "Patrouille au carrefour. J'ai fait demi-tour. Rien de vendu.";
            } + suffixe;
            case "rayan" -> switch (issue) {
                case REUSSIE -> "Grosse journée ! Les gens adorent le riz. Et le reste.";
                case MOINS -> "J'ai fait des prix. Mon cousin, sa copine, le cousin de sa copine... C'est de la pub, en vrai.";
                case RATEE -> "J'ai fermé plus tôt, y avait un match. Personne est venu de toute façon.";
            } + suffixe;
            case "moussa" -> switch (issue) {
                case REUSSIE -> "C'est réglé. J'ai attendu le temps qu'il fallait.";
                case MOINS -> "Il avait pas tout. J'ai pris ce qu'il y avait.";
                case RATEE -> "Il était pas là. Ou il a pas ouvert. Je repasserai.";
            } + suffixe;
            default -> issue.nom;
        };
    }

    private static String retard(String id) {
        return switch (id) {
            case "youngssef" -> "Le scooter a crevé. J'arrive, mais plus tard.";
            case "rayan" -> "J'ai un petit retard. Rien de grave. Enfin, je crois.";
            default -> "Ça prend plus de temps que prévu. J'attends.";
        };
    }

    // ------------------------------------------------------------------ les comptes

    public int totalRecettes() {
        return totalRecettes;
    }

    public int totalPaies() {
        return totalPaies;
    }

    public int totalFrais() {
        return totalFrais;
    }

    public int totalLabo() {
        return totalLabo;
    }

    /** Des frais qui ne viennent pas d'une operation (le loyer de l'epicerie, l'electricite) : comptes avec les frais. */
    public void compterFrais(int montant) {
        totalFrais += montant;
    }

    // ------------------------------------------------------------------ la sauvegarde

    public void enregistrer(Properties p) {
        p.setProperty("orga.graine", Long.toString(graine));
        p.setProperty("orga.totaux", totalRecettes + "," + totalPaies + "," + totalFrais + "," + totalLabo);
        List<String> ms = new ArrayList<>();
        for (Membre m : membres.values()) {
            ms.add(m.id + ":" + m.fiabilite + ":" + m.confiance + ":" + m.reussites + ":" + m.echecs);
        }
        p.setProperty("orga.membres", String.join(";", ms));
        List<String> es = new ArrayList<>();
        for (EnCours e : enCours) {
            es.add(e.operation + ":" + e.membre + ":" + e.paie + ":" + e.paye + ":" + e.frais + ":" + e.reste + ":" + e.total + ":" + e.retarde);
        }
        p.setProperty("orga.encours", String.join(";", es));
        List<String> bs = new ArrayList<>();
        for (Bilan b : bilans) {
            bs.add(b.operation() + ":" + b.membre() + ":" + b.issue() + ":" + b.recette() + ":" + b.frais() + ":" + b.paie() + ":" + b.labo()
                    + ":" + b.net());
        }
        p.setProperty("orga.bilans", String.join(";", bs));
    }

    public void relire(Properties p) {
        membres.clear();
        enCours.clear();
        bilans.clear();
        graine = Long.parseLong(p.getProperty("orga.graine", "1906"));
        String[] t = p.getProperty("orga.totaux", "0,0,0,0").split(",");
        totalRecettes = Integer.parseInt(t[0]);
        totalPaies = Integer.parseInt(t[1]);
        totalFrais = Integer.parseInt(t[2]);
        totalLabo = Integer.parseInt(t[3]);
        for (String s : p.getProperty("orga.membres", "").split(";")) {
            if (s.isBlank()) {
                continue;
            }
            String[] c = s.split(":");
            Membre m = switch (c[0]) {
                case "youngssef" -> new Membre("youngssef", "YOUNGSSEF", Talent.ROUTE, "buste_youngssef", 0, 0);
                case "rayan" -> new Membre("rayan", "SAVON", Talent.COMPTOIR, "buste_rayan", 0, 0);
                default -> new Membre("moussa", "TICKET", Talent.GUET, "buste_moussa", 0, 0);
            };
            m.fiabilite = Integer.parseInt(c[1]);
            m.confiance = Integer.parseInt(c[2]);
            m.reussites = Integer.parseInt(c[3]);
            m.echecs = Integer.parseInt(c[4]);
            membres.put(m.id, m);
        }
        for (String s : p.getProperty("orga.encours", "").split(";")) {
            if (s.isBlank()) {
                continue;
            }
            String[] c = s.split(":");
            EnCours e = new EnCours(c[0], c[1], Paie.valueOf(c[2]), Integer.parseInt(c[3]), Integer.parseInt(c[4]), Float.parseFloat(c[6]));
            e.reste = Float.parseFloat(c[5]);
            e.retarde = Boolean.parseBoolean(c[7]);
            enCours.add(e);
            if (membres.containsKey(e.membre)) {
                membres.get(e.membre).occupation = e.operation;
            }
        }
        for (String s : p.getProperty("orga.bilans", "").split(";")) {
            if (s.isBlank()) {
                continue;
            }
            String[] c = s.split(":");
            bilans.add(new Bilan(c[0], c[1], Issue.valueOf(c[2]), Integer.parseInt(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]),
                    Integer.parseInt(c[6]), Integer.parseInt(c[7]), ""));
        }
    }
}
