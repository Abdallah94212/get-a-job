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
        return id.compareTo("2.3") >= 0 ? 3 : 2;
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
                drapeaux.add(mot.startsWith("+") ? mot.substring(1) : mot);
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
        if (principal.genre() == genre && principal.cible().equals(cible) && verifie(principal.condition())) {
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
        if (objectif == null) {
            return null;
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
    }

    public void restaurer(int rangLu, int etapeLue, Set<String> facultatifsLus, boolean debutLu) {
        restaurer(rangLu, etapeLue, facultatifsLus, debutLu, Set.of());
    }
}
