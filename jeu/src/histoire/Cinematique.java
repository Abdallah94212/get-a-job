package histoire;

import java.util.ArrayList;
import java.util.List;

/** Une scene mise en scene avec les elements du jeu : une suite de plans joues dans l'ordre, avec des sauts. */
public final class Cinematique {
    private final String id;
    private final List<Plan> plans = new ArrayList<>();

    public Cinematique(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public List<Plan> plans() {
        return plans;
    }

    /**
     * Vrai si la scene ne change pas de decor : elle se joue la ou se trouve le joueur, avec ceux qui y sont.
     * Ses textes sur fond noir et ses cartons passent par-dessus, le temps de leur plan.
     */
    public boolean surPlace() {
        return plans.stream().noneMatch(p -> p.genre() == Plan.Genre.CARTE);
    }

    /** Le rang du plan qui porte cette etiquette, ou -1. */
    public int etiquette(String nom) {
        for (int i = 0; i < plans.size(); i++) {
            if (plans.get(i).genre() == Plan.Genre.ETIQUETTE && plans.get(i).mot(0).equals(nom)) {
                return i;
            }
        }
        return -1;
    }

    /** Les sauts qui menent nulle part (etiquette absente) et les etiquettes en double (on ne saurait ou sauter). */
    public List<String> sautsPerdus() {
        List<String> perdus = new ArrayList<>();
        java.util.Set<String> vues = new java.util.HashSet<>();
        for (Plan plan : plans) {
            if (plan.genre() == Plan.Genre.ETIQUETTE && !vues.add(plan.mot(0))) {
                perdus.add(plan.mot(0) + " (en double)");
            }
        }
        for (Plan plan : plans) {
            switch (plan.genre()) {
                case SAUT -> verifier(plan.mot(0), perdus);
                case SI -> verifier(plan.mot(1), perdus);
                case CHOIX -> plan.options().forEach(o -> verifier(o.etiquette(), perdus));
                default -> { }
            }
        }
        return perdus;
    }

    private void verifier(String nom, List<String> perdus) {
        if (etiquette(nom) < 0) {
            perdus.add(nom);
        }
    }
}
