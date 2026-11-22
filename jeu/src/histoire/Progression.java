package histoire;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * L'avancee du joueur dans la campagne : la mission en cours, l'objectif en cours, ce qui est ouvert, et ce dont
 * l'histoire se souvient (les drapeaux : un choix de dialogue, un objet donne, le scooter qu'on conduit).
 * Les missions se jouent dans l'ordre : finir l'une est la seule facon de debloquer la suivante.
 */
public final class Progression {
    /** Ce que la validation d'un objectif vient de changer. */
    public record Avancee(Objectif objectif, Mission mission, boolean missionTerminee, Mission suivante, int facultatifsFaits) {
        public boolean campagneTerminee() {
            return missionTerminee && suivante == null;
        }
    }

    private final Campagne campagne;
    private final Set<String> facultatifsFaits = new LinkedHashSet<>();
    private final Set<String> drapeaux = new LinkedHashSet<>();
    private final Etat etat = new Etat();
    private int rang;
    private int etape;
    private boolean debutJoue;

    public Progression(Campagne campagne) {
        this.campagne = campagne;
    }

    public Campagne campagne() {
        return campagne;
    }

    public Mission courante() {
        return rang < campagne.missions().size() ? campagne.missions().get(rang) : null;
    }

    public Objectif objectif() {
        Mission mission = courante();
        return mission == null ? null : mission.etapes().get(etape);
    }

    public int rang() {
        return rang;
    }

    public int etape() {
        return etape;
    }

    public Set<String> facultatifsFaits() {
        return facultatifsFaits;
    }

    public Set<String> drapeaux() {
        return drapeaux;
    }

    /** Vrai une fois que le heros a quitte Old Geto : le patron est devenu l'ennemi. */
    public boolean getoEnnemi() {
        Mission mission = courante();
        return drapeau("parti_de_chez_geto") || mission == null || mission.id().compareTo("2") >= 0;
    }

    /**
     * Ou en est la rue : elle perd ses couleurs a mesure que le heros monte.
     * 0 : le quartier vivant (0.1, 1.1) ; 1 : il livre pour Geto ; 2 : il l'a quitte ; 3 : il tient la rue (2.3 et apres).
     */
    public int etapeDeLaRue() {
        Mission mission = courante();
        String id = mission == null ? "9" : mission.id();
        if (!getoEnnemi()) {
            return id.equals("0.1") || id.equals("1.1") ? 0 : 1;
        }
        return id.compareTo("2.4") >= 0 ? 3 : 2;            // la rue grisit quand la RS6 part a Namek
    }

    /** La sante, le souffle, l'argent et le scooter du heros. */
    public Etat etat() {
        return etat;
    }

    /**
     * Des heures ou des jours passent hors de l'ecran (entre deux missions, une nuit) : ce qui devait arriver arrive.
     * Les operations de l'equipe se terminent et sont payees, les colis arrivent, le corps se repose, et le scooter
     * emprunte retourne chez Youngssef. Renvoie ce qui s'est passe, en lignes courtes, pour l'ecran du temps qui passe.
     */
    public java.util.List<String> laisserPasser(float minutes) {
        java.util.List<String> pendant = new java.util.ArrayList<>();
        finiesPendant.clear();
        if (drapeau("organisation")) {
            etat.organisation().recruter(this);
            int net = 0;
            int finies = 0;
            for (int i = 0; i < 6 && !etat.organisation().enCours().isEmpty(); i++) {
                for (Organisation.Bilan b : etat.organisation().avancer(Math.min(minutes * 60f, 4000f), etat)) {
                    finiesPendant.add(b);
                    net += b.recette() - b.labo();
                    finies++;
                }
            }
            if (finies > 0) {
                pendant.add(finies + (finies > 1 ? " OPÉRATIONS TERMINÉES : + " : " OPÉRATION TERMINÉE : + ") + net + " EUROS");
            }
        }
        int avant = etat.colis().size();
        etat.livrer(minutes * 60f);
        if (etat.colis().size() > avant) {
            pendant.add("UN COLIS T'ATTEND CHEZ TOI");
        }
        if (drapeau("scooter") || drapeau("scooter_gare")) {       // dit une fois, au moment ou ca arrive
            marquer("-scooter -scooter_gare -location_finie");
            etat.rendreScooter();
            pendant.add("TU AVAIS SON SCOOTER : YOUNGSSEF L'A RÉCUPÉRÉ");
        }
        if (minutes >= Calendrier.SAUT) {
            boolean blesse = etat.sante() < Etat.SANTE_MAX;
            etat.soigner(Etat.SANTE_MAX);
            etat.reposer();
            if (blesse) {
                pendant.add("TES BLESSURES ONT CICATRISÉ");
            }
        }
        int duAvant = etat.loyerDu();
        etat.calendrier().passer(minutes);
        etat.majLoyer();
        if (etat.loyerDu() > duAvant) {                                // un nouveau mois est tombe pendant ce temps
            pendant.add("LOYER : " + etat.loyerDu() + " MOIS À PAYER À LA RÉGIE");
        }
        return pendant;
    }

    private final java.util.List<Organisation.Bilan> finiesPendant = new java.util.ArrayList<>();

    /** Les operations terminees pendant le dernier temps passe hors de l'ecran. */
    public java.util.List<Organisation.Bilan> finiesPendant() {
        return finiesPendant;
    }

    public boolean drapeau(String nom) {
        return drapeaux.contains(nom);
    }

    /** Leve (+nom) ou baisse (-nom) des drapeaux. Un nom sans signe est leve. */
    public void marquer(String changements) {
        for (String mot : changements.trim().split("\\s+")) {
            if (mot.startsWith("-")) {
                drapeaux.remove(mot.substring(1));
            } else if (!mot.isEmpty()) {
                String nom = mot.startsWith("+") ? mot.substring(1) : mot;
                drapeaux.add(nom);
                String objet = Inventaire.DRAPEAUX.get(nom);           // un objet de l'histoire : il est aussi dans la poche
                if (objet != null && !etat.inventaire().possede(objet)) {
                    etat.inventaire().ajouter(objet, 1);
                }
            }
        }
    }

    /** Vrai si chaque drapeau demande (+nom) est leve et chaque drapeau refuse (-nom) est baisse. */
    public boolean verifie(String condition) {
        if (condition == null || condition.isBlank()) {
            return true;
        }
        for (String mot : condition.trim().split("\\s+")) {
            boolean voulu = !mot.startsWith("-");
            String nom = mot.startsWith("+") || mot.startsWith("-") ? mot.substring(1) : mot;
            if (drapeaux.contains(nom) != voulu) {
                return false;
            }
        }
        return true;
    }

    public boolean terminee() {
        return courante() == null;
    }

    public EtatMission etat(Mission mission) {
        int place = campagne.rang(mission.id());
        return place < rang ? EtatMission.TERMINEE : place == rang ? EtatMission.DISPONIBLE : EtatMission.VERROUILLEE;
    }

    /** Vrai si on peut entrer dans cette carte : elle n'a jamais ete fermee, ou une mission deja commencee l'a ouverte. */
    public boolean lieuOuvert(String carte) {
        if (!campagne.verrous().containsKey(carte)) {
            return true;
        }
        List<Mission> missions = campagne.missions();
        for (int i = 0; i <= Math.min(rang, missions.size() - 1); i++) {
            if (missions.get(i).ouvre().contains(carte)) {
                return true;
            }
        }
        return false;
    }

    /** Vrai si le personnage est dans le quartier en ce moment de l'histoire. */
    public boolean present(Figurant figurant) {
        return rang >= campagne.rang(figurant.de()) && rang <= campagne.rang(figurant.a()) && verifie(figurant.condition());
    }

    /** Vrai si l'histoire attend qu'on s'occupe de cette cible : objectif en cours, ou objectif secondaire pas encore fait. */
    public boolean attendue(Objectif.Genre genre, String cible) {
        return trouver(genre, cible) != null;
    }

    private Objectif trouver(Objectif.Genre genre, String cible) {
        Mission mission = courante();
        if (mission == null) {
            return null;
        }
        Objectif principal = mission.etapes().get(etape);
        if (principal.genre() == genre && (principal.cible().equals(cible) || principal.cible().equals("*")) && verifie(principal.condition())) {
            return principal;
        }
        for (Objectif facultatif : mission.facultatifs()) {
            if (facultatif.genre() == genre && facultatif.cible().equals(cible) && !facultatifsFaits.contains(cible)
                    && verifie(facultatif.condition())) {
                return facultatif;
            }
        }
        return null;
    }

    /** L'objet qu'il faudrait avoir pour faire cet objectif maintenant, s'il manque ; null sinon. */
    public String objetManquant(Objectif.Genre genre, String cible) {
        Objectif objectif = trouver(genre, cible);
        return objectif != null && objectif.objet() != null && !etat.inventaire().possede(objectif.objet()) ? objectif.objet() : null;
    }

    /** Ce qui manque en poche pour faire cet objectif maintenant (rembourser quelqu'un), ou 0 s'il ne demande rien. */
    public int argentManquant(Objectif.Genre genre, String cible) {
        Objectif objectif = trouver(genre, cible);
        return objectif == null ? 0 : Math.max(0, objectif.argent() - etat.argent());
    }

    /**
     * Remet l'inventaire d'accord avec l'histoire, a la reprise d'une partie : les objets que les drapeaux disent en
     * poche (le beeper, le boitier), et les livraisons deja remises par une scene de cette mission mais pas encore
     * livrees (une partie enregistree avant que l'inventaire existe). Rien n'est ajoute deux fois.
     */
    public void reparerInventaire() {
        Inventaire inv = etat.inventaire();
        for (var e : Inventaire.DRAPEAUX.entrySet()) {
            if (drapeau(e.getKey()) && !inv.possede(e.getValue())) {
                inv.ajouter(e.getValue(), 1);
            }
        }
        Mission mission = courante();
        if (mission != null) {
            java.util.Map<String, Integer> attendu = new java.util.LinkedHashMap<>();
            for (int j = etape; j < mission.etapes().size(); j++) {
                Objectif o = mission.etapes().get(j);
                if (o.objet() != null) {
                    attendu.merge(o.objet(), Math.max(1, quantiteRemise(o.suite(), o.objet())), Integer::sum);
                }
            }
            for (var e : attendu.entrySet()) {
                boolean donne = debutJoue && quantiteDonnee(mission.cinematiqueDebut(), e.getKey()) > 0;
                for (int i = 0; i < etape && !donne; i++) {
                    donne = quantiteDonnee(mission.etapes().get(i).suite(), e.getKey()) > 0;
                }
                int manque = e.getValue() - inv.quantite(e.getKey());
                if (donne && manque > 0) {
                    inv.ajouter(e.getKey(), manque);
                }
            }
        }
        inv.prendreMouvements();
    }

    private int quantiteDonnee(String scene, String objet) {
        return compter(scene, objet, Plan.Genre.DONNE);
    }

    private int quantiteRemise(String scene, String objet) {
        return compter(scene, objet, Plan.Genre.REMET);
    }

    private int compter(String scene, String objet, Plan.Genre genre) {
        Cinematique c = scene == null ? null : campagne.cinematique(scene);
        int n = 0;
        if (c != null) {
            for (Plan p : c.plans()) {
                if (p.genre() == genre && p.mot(0).equals(objet)) {
                    n += p.mot(1).isEmpty() ? 1 : p.mot(1).equals("*") ? 1 : Integer.parseInt(p.mot(1));
                }
            }
        }
        return n;
    }

    /** Les objectifs secondaires qu'on peut encore faire maintenant. */
    public List<Objectif> secondairesOuverts() {
        Mission mission = courante();
        return mission == null ? List.of() : mission.facultatifs().stream()
                .filter(o -> !facultatifsFaits.contains(o.cible()) && verifie(o.condition())).toList();
    }

    /**
     * Le joueur vient de parler a ce personnage, d'agir sur ce point, d'entrer dans cette carte ou d'atteindre ce point.
     * Renvoie ce que cela a fait avancer, ou null si l'histoire n'attendait pas cela maintenant.
     */
    public Avancee valider(Objectif.Genre genre, String cible) {
        Objectif objectif = trouver(genre, cible);
        if (objectif == null || objectif.objet() != null && !etat.inventaire().possede(objectif.objet())) {
            return null;                                               // pas de livraison sans la marchandise
        }
        if (objectif.argent() > etat.argent()) {
            return null;                                               // pas de remboursement sans l'argent
        }
        Mission mission = courante();
        if (objectif.facultatif()) {
            facultatifsFaits.add(cible);
            return new Avancee(objectif, mission, false, null, facultatifsFaits.size());
        }
        if (etape + 1 < mission.etapes().size()) {
            etape++;
            return new Avancee(objectif, mission, false, null, facultatifsFaits.size());
        }
        int faits = facultatifsFaits.size();
        rang++;
        etape = 0;
        debutJoue = false;
        facultatifsFaits.clear();
        return new Avancee(objectif, mission, true, courante(), faits);
    }

    public boolean sousPression() {
        Objectif objectif = objectif();
        return objectif != null && objectif.sousPression();
    }

    /** Le chrono est ecoule ou un garde nous a vus : on reprend au premier objectif de la sequence sous pression. */
    public void echouer() {
        Mission mission = courante();
        if (mission == null) {
            return;
        }
        while (etape > 0 && mission.etapes().get(etape - 1).sousPression()) {
            etape--;
        }
    }

    public boolean debutJoue() {
        return debutJoue;
    }

    public void marquerDebutJoue() {
        debutJoue = true;
        noteesDebut = etat.notees();
        argentDebut = etat.argent();
    }

    private long noteesDebut;
    private int argentDebut;

    /** Le compte au debut de la mission en cours : combien de mouvements avant elle, et le solde. */
    public long noteesDebut() {
        return noteesDebut;
    }

    public int argentDebut() {
        return argentDebut;
    }

    public void reprendreDebut(long notees, int argent) {
        noteesDebut = notees;
        argentDebut = argent;
    }

    /** Remet la progression dans un etat lu dans une sauvegarde. Refuse un etat qui n'existe pas dans la campagne. */
    public void restaurer(int rangLu, int etapeLue, Set<String> facultatifsLus, boolean debutLu, Set<String> drapeauxLus) {
        int total = campagne.missions().size();
        if (rangLu < 0 || rangLu > total) {
            throw new IllegalArgumentException("Mission inconnue : " + rangLu);
        }
        if (etapeLue < 0 || (rangLu < total && etapeLue >= campagne.missions().get(rangLu).etapes().size())) {
            throw new IllegalArgumentException("Objectif inconnu : " + etapeLue);
        }
        rang = rangLu;
        etape = rangLu == total ? 0 : etapeLue;
        debutJoue = debutLu;
        facultatifsFaits.clear();
        facultatifsFaits.addAll(facultatifsLus);
        drapeaux.clear();
        drapeaux.addAll(drapeauxLus);
        reparerInventaire();
    }

    public void restaurer(int rangLu, int etapeLue, Set<String> facultatifsLus, boolean debutLu) {
        restaurer(rangLu, etapeLue, facultatifsLus, debutLu, Set.of());
    }
}
