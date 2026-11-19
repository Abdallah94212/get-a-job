package histoire;

import java.util.Properties;

/**
 * Le temps du quartier : le jour (1 est le vendredi ou tout commence) et l'heure. Chaque mission dit quand elle se
 * passe ; entre deux missions, le temps qui s'ecoule se voit (un ecran de calendrier), et ce qui devait arriver arrive :
 * les operations se terminent, les colis arrivent, le scooter retourne a Youngssef, on se reveille chez soi.
 * Pendant qu'on joue, l'horloge avance d'une minute par seconde.
 */
public final class Calendrier {
    public static final String[] JOURS = {"VENDREDI", "SAMEDI", "DIMANCHE", "LUNDI", "MARDI", "MERCREDI", "JEUDI"};
    /** Au-dela de six heures, ce n'est plus "un peu plus tard" : on rentre chez soi, une nuit ou plus a passe. */
    public static final int SAUT = 6 * 60;
    public static final int JOURNEE = 24 * 60;

    private int jour;
    private float minutes;

    /** Vrai une fois que le calendrier a ete regle (une vieille sauvegarde n'en a pas). */
    public boolean connu() {
        return jour > 0;
    }

    public int jour() {
        return jour;
    }

    public int minutes() {
        return (int) minutes;
    }

    public void regler(int jourVoulu, int minutesVoulues) {
        jour = jourVoulu;
        minutes = minutesVoulues;
    }

    /** Le temps passe (en minutes de jeu). */
    public void passer(float duree) {
        minutes += duree;
        while (minutes >= JOURNEE) {
            minutes -= JOURNEE;
            jour++;
        }
    }

    /** Combien de minutes jusqu'a ce jour, cette heure (negatif si c'est deja passe). */
    public float ecart(int jourVoulu, int minutesVoulues) {
        return (jourVoulu - jour) * (float) JOURNEE + minutesVoulues - minutes;
    }

    public String nomDuJour() {
        return nomDuJour(jour);
    }

    public static String nomDuJour(int jour) {
        return JOURS[Math.floorMod(jour - 1, 7)];
    }

    /** "23:40". */
    public String horloge() {
        return horloge((int) minutes);
    }

    public static String horloge(int minutes) {
        return String.format("%02d:%02d", minutes / 60 % 24, minutes % 60);
    }

    /** Le moment de la journee, pour les habitudes des gens : la nuit, le matin, l'apres-midi, le soir. */
    public enum Moment { NUIT, MATIN, APRES_MIDI, SOIR }

    public Moment moment() {
        return moment((int) minutes);
    }

    public static Moment moment(int minutes) {
        int h = minutes / 60 % 24;
        return h < 6 ? Moment.NUIT : h < 12 ? Moment.MATIN : h < 18 ? Moment.APRES_MIDI : Moment.SOIR;
    }

    /** Ce qu'on ecrit sur l'ecran du temps qui passe : "LE LENDEMAIN SOIR", "TROIS SEMAINES PLUS TARD"... */
    public static String delai(int jourAvant, int jourApres, int minutesApres, float ecart) {
        int jours = jourApres - jourAvant;
        String moment = switch (moment(minutesApres)) {
            case NUIT -> " DANS LA NUIT";
            case MATIN -> " MATIN";
            case APRES_MIDI -> " APRÈS-MIDI";
            case SOIR -> " SOIR";
        };
        if (jours <= 0) {
            return ecart < 3 * 60 ? "UN PEU PLUS TARD" : "QUELQUES HEURES PLUS TARD";
        }
        if (jours == 1) {
            return "LE LENDEMAIN" + moment;
        }
        String[] nombres = {"", "", "DEUX", "TROIS", "QUATRE", "CINQ", "SIX"};
        if (jours < 7) {
            return nombres[jours] + " JOURS PLUS TARD";
        }
        if (jours < 28) {
            int semaines = Math.round(jours / 7f);
            return semaines <= 1 ? "UNE SEMAINE PLUS TARD" : nombres[Math.min(6, semaines)] + " SEMAINES PLUS TARD";
        }
        int mois = Math.round(jours / 30f);
        return mois <= 1 ? "UN MOIS PLUS TARD" : nombres[Math.min(6, mois)] + " MOIS PLUS TARD";
    }

    void enregistrer(Properties p) {
        p.setProperty("temps.jour", Integer.toString(jour));
        p.setProperty("temps.minutes", Float.toString(minutes));
    }

    void relire(Properties p) {
        jour = Integer.parseInt(p.getProperty("temps.jour", "0"));
        minutes = Float.parseFloat(p.getProperty("temps.minutes", "0"));
    }
}
