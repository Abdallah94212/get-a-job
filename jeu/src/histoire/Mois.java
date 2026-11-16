package histoire;

import java.util.ArrayList;
import java.util.List;

/**
 * Un mois d'activite, jour apres jour : l'organisation tourne toute seule pendant que le temps passe a l'ecran.
 *
 * <p>Rien n'est ajoute d'un coup : chaque jour, les petits libres partent sur l'operation de leur talent (et Youngssef
 * sur la course de Namek, une fois par semaine), payes au depart ; les operations se terminent et se reglent avec le
 * meme moteur que dans le jeu (reussites, ventes decevantes, echecs, retards) ; le loyer de l'epicerie, celui du 12 et
 * les frais de la semaine tombent a leur date. Chaque mouvement passe par la banque du heros, avec son libelle : le
 * solde final est la somme de tout ce qui s'est passe, et le joueur peut le relire ligne a ligne.
 */
public final class Mois {
    public static final int JOURS = 30;
    /** Une journee de jeu, pour les operations : une course de Namek en prend une a deux. */
    public static final float JOURNEE = 600;
    /** Le loyer de l'epicerie que Papy Bocal a cede : le premier du mois. */
    public static final int LOYER_EPICERIE = 450;
    /** Les frais de la semaine : l'electricite des frigos, les sacs, le credit des telephones. */
    public static final int FRAIS_SEMAINE = 120;

    /** Ce qui s'affiche : un mouvement de banque (montant non nul) ou un message d'un petit (montant nul). */
    public record Evenement(String de, String texte, int montant) {
    }

    public record Jour(int numero, List<Evenement> evenements, int solde) {
    }

    private final Progression progression;
    private final Etat etat;
    private final Organisation orga;
    private final int depart;
    /** Les totaux de l'organisation au premier jour : le resume ne compte que ce mois-ci. */
    private final int[] avant;
    private final List<Integer> soldes = new ArrayList<>();
    private int jour;

    public Mois(Progression progression) {
        this.progression = progression;
        this.etat = progression.etat();
        this.orga = etat.organisation();
        this.depart = etat.argent();
        this.avant = new int[] {orga.totalRecettes(), orga.totalPaies(), orga.totalLabo(), orga.totalFrais()};
        soldes.add(depart);
    }

    public int jour() {
        return jour;
    }

    public boolean fini() {
        return jour >= JOURS;
    }

    public int depart() {
        return depart;
    }

    public List<Integer> soldes() {
        return soldes;
    }

    /** Un jour passe. Renvoie ce qui s'est passe, dans l'ordre, et le solde du soir. */
    public Jour jourSuivant() {
        if (fini()) {
            return null;
        }
        jour++;
        long avant = etat.notees();
        List<Evenement> messages = new ArrayList<>();
        if (jour == 8) {                                               // le loyer de l'epicerie tombe le 8
            frais(LOYER_EPICERIE, "LOYER DE L'ÉPICERIE (PAPY BOCAL)");
        }
        if (jour == 5) {                                               // le loyer du 12 : tout le retard est rattrape
            while (etat.loyerDu() > 0 && etat.payerLoyer()) {
                orga.compterFrais(Etat.LOYER);
            }
        }
        if (jour % 7 == 0) {
            frais(FRAIS_SEMAINE, "FRAIS DE LA SEMAINE : FRIGOS, SACS, TÉLÉPHONES");
        }
        lancerLaJournee();
        for (Organisation.Bilan b : orga.avancer(JOURNEE, etat)) {
            messages.add(new Evenement(b.membre(), b.message(), 0));
        }
        List<Evenement> evenements = new ArrayList<>();
        List<Etat.Ligne> journal = etat.journal();
        int nouveaux = (int) Math.min(journal.size(), etat.notees() - avant);   // le journal plafonne : on compte ce qui est entre
        for (int i = journal.size() - nouveaux; i < journal.size(); i++) {
            evenements.add(new Evenement("banque", journal.get(i).libelle(), journal.get(i).montant()));
        }
        evenements.addAll(messages);
        soldes.add(etat.argent());
        return new Jour(jour, evenements, etat.argent());
    }

    /** Une depense du mois : elle sort de la banque, et compte avec les frais seulement si elle a pu etre payee. */
    private void frais(int montant, String libelle) {
        if (etat.payer(montant, libelle)) {
            orga.compterFrais(montant);
        }
    }

    /** Le reste du mois d'un coup (le joueur passe la sequence) : les memes jours, les memes mouvements. */
    public void finir() {
        while (!fini()) {
            jourSuivant();
        }
    }

    /** Chaque matin : qui part sur quoi. La course de Namek d'abord, une fois par semaine ; puis chacun son talent. */
    private void lancerLaJournee() {
        if (jour % 7 == 3) {
            for (String qui : new String[] {"youngssef", "moussa", "rayan"}) {
                Organisation.Membre m = orga.membre(qui);
                if (m != null && orga.lancer("namek", qui, Organisation.Paie.JUSTE, etat) == null) {
                    break;
                }
            }
        }
        for (Organisation.Membre m : orga.membres()) {
            if (!m.libre()) {
                continue;
            }
            String op = switch (m.talent) {
                case ROUTE -> "livraisons";
                case COMPTOIR -> "comptoir";
                case GUET -> "ardoise";
            };
            // Ticket ne travaille bien que bien paye : une prime de temps en temps, pour qu'il reste
            Organisation.Paie paie = m.id.equals("moussa") && jour % 3 == 0 ? Organisation.Paie.PRIME : Organisation.Paie.JUSTE;
            orga.lancer(op, m.id, paie, etat);
        }
    }

    /** Le resume du mois, par poste : ce que le joueur lit a la fin. */
    public int[] resume() {
        return new int[] {orga.totalRecettes() - avant[0], orga.totalPaies() - avant[1], orga.totalLabo() - avant[2],
            orga.totalFrais() - avant[3], etat.argent() - depart};
    }

    public Progression progression() {
        return progression;
    }
}
