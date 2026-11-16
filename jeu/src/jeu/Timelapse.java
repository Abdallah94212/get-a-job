package jeu;

import decor.Rendu;
import histoire.Mois;
import histoire.Progression;
import java.util.ArrayList;
import java.util.List;

/**
 * Un mois qui passe a l'ecran : l'organisation tourne, le compte en banque bouge.
 *
 * <p>Le mois est calcule d'un seul coup au debut (avec les vraies operations et les vrais paiements : histoire.Mois),
 * puis rejoue jour apres jour : le calendrier avance, les notifications de la banque et les messages des petits
 * tombent dans le telephone, le solde monte et descend, la courbe se dessine, les totaux par poste s'additionnent.
 * A la fin, le bilan dit d'ou vient chaque euro. E accelere ; Echap passe (le mois est deja compte).
 */
final class Timelapse {
    private static final float JOUR = 0.95f;
    private static final String[] JOURS = {"LUNDI", "MARDI", "MERCREDI", "JEUDI", "VENDREDI", "SAMEDI", "DIMANCHE"};

    private record Notif(Mois.Evenement e, int jour, float ne) {
    }

    private final Affichage affichage;
    private final Audio audio;
    private final List<Mois.Jour> jours = new ArrayList<>();
    private final List<Notif> notifs = new ArrayList<>();
    private int depart;
    private int[] resume;
    private int montre;
    private float temps;
    private float soldeVu;
    private float horloge;
    private int recettes;
    private int paies;
    private int labo;
    private int frais;

    Timelapse(Affichage affichage, Audio audio) {
        this.affichage = affichage;
        this.audio = audio;
    }

    /** Calcule tout le mois maintenant : les transactions sont faites, il ne reste qu'a les montrer. */
    void commencer(Progression progression) {
        Mois mois = new Mois(progression);
        depart = mois.depart();
        jours.clear();
        notifs.clear();
        while (!mois.fini()) {
            jours.add(mois.jourSuivant());
        }
        resume = mois.resume();
        montre = 0;
        temps = 0;
        horloge = 0;
        soldeVu = depart;
        recettes = paies = labo = frais = 0;
    }

    boolean fini() {
        return montre >= jours.size();
    }

    /**
     * Une image : un jour de plus quand c'est l'heure (accelere : E tenue). Renvoie vrai quand le bilan est affiche,
     * lu (un instant) et que le joueur appuie sur E.
     */
    boolean maj(float dt, boolean accelere, boolean valide) {
        horloge += dt;
        if (!fini()) {
            temps += dt * (accelere ? 5 : 1);
            while (!fini() && temps >= JOUR) {
                temps -= JOUR;
                montrer(jours.get(montre++));
            }
            if (fini()) {
                temps = 0;
            }
        } else {
            temps += dt;
        }
        int cible = montre == 0 ? depart : jours.get(montre - 1).solde();
        soldeVu += (cible - soldeVu) * Math.min(1f, dt * 6);
        return fini() && temps > 1.2f && valide;
    }

    private void montrer(Mois.Jour j) {
        boolean entree = false;
        boolean sortie = false;
        boolean message = false;
        for (Mois.Evenement e : j.evenements()) {
            notifs.add(new Notif(e, j.numero(), horloge));
            if (e.montant() > 0) {
                recettes += e.montant();
                entree = true;
            } else if (e.montant() < 0) {
                if (e.texte().startsWith("PAIE")) {
                    paies -= e.montant();
                } else if (e.texte().startsWith("LABO")) {
                    labo -= e.montant();
                } else {
                    frais -= e.montant();
                }
                sortie = true;
            } else {
                message = true;
            }
        }
        while (notifs.size() > 40) {
            notifs.remove(0);
        }
        if (entree) {
            audio.jouer("tel_argent", 0.4f, 0.3f, 1);
        }
        if (sortie) {
            audio.jouer("piece", 0.3f, -0.3f, 0.8f);
        }
        if (message) {
            audio.jouer("tel_sms", 0.3f, 0.3f, 1);
        }
    }

    void dessiner(Rendu rendu) {
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x06060C, 170);
        int jour = Math.max(1, montre);
        // le calendrier
        affichage.petite().ecrire(rendu, "UN MOIS D'AFFAIRES", 24, 22, Affichage.ORANGE, 1);
        affichage.police().ecrire(rendu, "JOUR " + jour, 24, 34, Affichage.BLANC, 3);
        affichage.police().ecrire(rendu, JOURS[(jour - 1) % 7] + ", SEMAINE " + ((jour - 1) / 7 + 1), 24, 70, Affichage.GRIS, 1);
        for (int k = 0; k < Mois.JOURS; k++) {                        // trente cases : celles qui sont passees s'allument
            int cx = 24 + (k % 15) * 13;
            int cy = 86 + (k / 15) * 9;
            rendu.rect(cx, cy, 11, 7, k < montre ? Affichage.ORANGE : 0x2A2A36, k < montre ? 220 : 256);
        }
        // le solde, et ce qu'il fait aujourd'hui
        affichage.petite().ecrire(rendu, "SOLDE", 24, 112, Affichage.SOMBRE, 1);
        String solde = Math.round(soldeVu) + " EUROS";
        affichage.police().ecrire(rendu, solde, 24, 122, soldeVu >= depart ? Affichage.VERT : 0xE04A3A, 2);
        if (montre > 0) {
            int avant = montre == 1 ? depart : jours.get(montre - 2).solde();
            int delta = jours.get(montre - 1).solde() - avant;
            affichage.petite().ecrire(rendu, (delta >= 0 ? "+ " : "- ") + Math.abs(delta) + " AUJOURD'HUI", 24, 146,
                    delta >= 0 ? Affichage.VERT : 0xE04A3A, 1);
        }
        courbe(rendu, 24, 158, 196, 46);
        // les postes du mois, tels qu'ils s'additionnent
        int y = 212;
        y = poste(rendu, y, "RECETTES DES OPÉRATIONS", recettes, Affichage.VERT);
        y = poste(rendu, y, "PAIES DE YOUNGSSEF, SAVON, TICKET", -paies, Affichage.BLANC);
        y = poste(rendu, y, "CUVES DE GERO", -labo, Affichage.BLANC);
        poste(rendu, y, "FRAIS, STOCK ET LOYERS", -frais, Affichage.BLANC);
        notifications(rendu);
        if (fini() && temps > 0.4f) {
            bilan(rendu);
        } else {
            affichage.petite().ecrire(rendu, "E : PLUS VITE   ECHAP : PASSER", Rendu.L - 128, Rendu.H - 10, Affichage.SOMBRE, 1);
        }
    }

    private int poste(Rendu rendu, int y, String nom, int montant, int couleur) {
        String m = (montant >= 0 ? "+ " : "- ") + Math.abs(montant);
        affichage.petite().ecrire(rendu, nom, 24, y, Affichage.GRIS, 1);
        affichage.petite().ecrire(rendu, m, 220 - affichage.petite().largeur(m, 1), y, couleur, 1);
        return y + 9;
    }

    /** La courbe du solde, jour apres jour : verte quand elle monte, rouge quand elle descend. */
    private void courbe(Rendu rendu, int x, int y, int l, int h) {
        rendu.rect(x, y, l, h, 0x0E0E16, 230);
        int max = depart;
        int min = depart;
        for (Mois.Jour j : jours) {
            max = Math.max(max, j.solde());
            min = Math.min(min, j.solde());
        }
        float echelle = (h - 6) / (float) Math.max(1, max - min);
        int precedent = depart;
        for (int k = 0; k < montre; k++) {
            int s = jours.get(k).solde();
            int x0 = x + 3 + Math.round(k * (l - 6) / (float) Mois.JOURS);
            int x1 = x + 3 + Math.round((k + 1) * (l - 6) / (float) Mois.JOURS);
            float y0 = y + h - 3 - (precedent - min) * echelle;
            float y1 = y + h - 3 - (s - min) * echelle;
            rendu.ligne(x0, y0, x1, y1, 2, s >= precedent ? Affichage.VERT : 0xE04A3A);
            precedent = s;
        }
    }

    /** Le telephone, a droite : les derniers mouvements et messages, les plus recents en haut. */
    private void notifications(Rendu rendu) {
        int x = 252;
        int l = 210;
        rendu.rect(x - 2, 18, l + 4, 236, 0x0C0C12, 240);
        rendu.rect(x, 20, l, 232, 0x14141C, 256);
        rendu.rect(x, 20, l, 11, 0x22222E, 256);
        affichage.petite().ecrire(rendu, "NOTIFICATIONS", x + 4, 23, Affichage.GRIS, 1);
        int y = 36;
        for (int i = notifs.size() - 1; i >= 0 && y < 238; i--) {
            Notif n = notifs.get(i);
            int glisse = Math.max(0, Math.round((0.25f - (horloge - n.ne())) * 60));
            Mois.Evenement e = n.e();
            if (e.montant() != 0) {
                String m = (e.montant() > 0 ? "+ " : "- ") + Math.abs(e.montant());
                int c = e.montant() > 0 ? Affichage.VERT : 0xE04A3A;
                rendu.rect(x + 4 + glisse, y + 1, 3, 6, c, 256);
                String libelle = affichage.petite().propre(e.texte());
                affichage.petite().ecrire(rendu, libelle.length() > 38 ? libelle.substring(0, 37) + "…" : libelle, x + 10 + glisse, y,
                        Affichage.GRIS, 1);
                affichage.petite().ecrire(rendu, m, x + l - 4 - affichage.petite().largeur(m, 1) + glisse, y, c, 1);
                y += 9;
            } else {
                String nom = e.de().equals("youngssef") ? "YOUNGSSEF" : e.de().equals("rayan") ? "SAVON" : "TICKET";
                List<String> lignes = affichage.petite().couper(affichage.petite().propre(e.texte()), l - 16);
                int hb = 9 + lignes.size() * 8;
                rendu.rect(x + 4 + glisse, y - 1, l - 8, hb, 0x22222E, 256);
                affichage.police().ecrireTitre(rendu, nom, x + 7 + glisse, y, Affichage.ORANGE);
                for (int k = 0; k < lignes.size(); k++) {
                    affichage.petite().ecrire(rendu, lignes.get(k), x + 7 + glisse, y + 9 + k * 8, Affichage.BLANC, 1);
                }
                y += hb + 3;
            }
        }
    }

    /** Le bilan du mois : d'ou vient chaque euro. */
    private void bilan(Rendu rendu) {
        int x = 120;
        int y = 54;
        rendu.rect(x - 1, y - 1, 242, 164, 0x4A4A62, 256);
        rendu.rect(x, y, 240, 162, 0x14141E, 256);
        rendu.rect(x, y, 240, 3, Affichage.ORANGE, 256);
        affichage.petite().ecrire(rendu, "UN MOIS PLUS TARD", x + 12, y + 12, Affichage.ORANGE, 1);
        affichage.police().ecrire(rendu, "LE BILAN", x + 12, y + 22, Affichage.BLANC, 2);
        int ly = y + 50;
        String[][] lignes = {{"SOLDE AU DÉBUT DU MOIS", Integer.toString(depart)}, {"+ RECETTES DES OPÉRATIONS", "+ " + resume[0]},
            {"- PAIES DE L'ÉQUIPE", "- " + resume[1]}, {"- CUVES DE GERO", "- " + resume[2]}, {"- FRAIS, STOCK ET LOYERS", "- " + resume[3]}};
        for (String[] l : lignes) {
            affichage.police().ecrire(rendu, l[0], x + 12, ly, Affichage.GRIS, 1);
            affichage.police().ecrire(rendu, l[1], x + 228 - affichage.police().largeur(l[1], 1), ly, Affichage.BLANC, 1);
            ly += 13;
        }
        rendu.rect(x + 12, ly, 216, 1, Affichage.ORANGE, 256);
        ly += 5;
        String fin = (depart + resume[4]) + " EUROS";
        affichage.police().ecrireTitre(rendu, "SUR LE COMPTE", x + 12, ly + 2, Affichage.BLANC);
        affichage.police().ecrire(rendu, fin, x + 228 - affichage.police().largeur(fin, 2), ly - 2, Affichage.VERT, 2);
        if (temps > 1.2f) {
            affichage.petite().ecrire(rendu, "E : CONTINUER", x + 160, y + 150, Affichage.GRIS, 1);
        }
    }
}
