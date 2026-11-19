package jeu;

import decor.Rendu;
import decor.Texture;
import histoire.Etat;
import histoire.Inventaire;
import histoire.Inventaire.Categorie;
import histoire.Inventaire.Ligne;
import histoire.Progression;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * L'inventaire a l'ecran (touche I, ou "Tes poches" dans la pause). Trois onglets (livraisons, armes, objets), une
 * grille d'illustrations avec leurs quantites, et a droite la fiche de l'objet choisi : ce que c'est, a qui il va, ce
 * qu'on peut en faire. E equipe une arme (ou la range), mange le sandwich ; G pose une arme par terre. Les objets
 * dont l'histoire a besoin ne se jettent pas.
 */
final class Poches {
    private static final int X = 36, Y = 26, L = 408, H = 218;
    private static final int COLONNES = 5, CASE = 36;
    private static final int[] TEINTES = {0xE07B39, 0xC2563A, 0x8E6CD8};

    private final Ressources res;
    private final Progression progression;
    private final Affichage affichage;
    private final Audio audio;
    private final Monde monde;
    private Categorie onglet = Categorie.LIVRAISONS;
    private int choix;
    private float age;
    private float choixAge;
    private String info = "";
    private float infoReste;
    /** Les objets qui viennent d'entrer : ils brillent un instant quand on ouvre. */
    private final Map<String, Float> nouveaux = new HashMap<>();

    Poches(Ressources res, Progression progression, Affichage affichage, Audio audio, Monde monde) {
        this.res = res;
        this.progression = progression;
        this.affichage = affichage;
        this.audio = audio;
        this.monde = monde;
    }

    private Inventaire inv() {
        return progression.etat().inventaire();
    }

    void ouvrir() {
        age = 0;
        choix = 0;
        info = "";
        if (inv().lignes(onglet).isEmpty()) {                          // on ouvre sur un onglet qui a quelque chose
            for (Categorie c : Categorie.values()) {
                if (!inv().lignes(c).isEmpty()) {
                    onglet = c;
                    break;
                }
            }
        }
    }

    /** Un objet vient d'entrer dans l'inventaire : il brillera la prochaine fois qu'on le voit. */
    void signaler(String id) {
        nouveaux.put(id, 1.6f);
    }

    /** Une image. Renvoie vrai quand on referme. */
    boolean maj(float dt, Touches touches) {
        age += dt;
        choixAge += dt;
        infoReste = Math.max(0, infoReste - dt);
        nouveaux.replaceAll((k, v) -> v - dt * (age > 0.3f ? 1 : 0));
        nouveaux.values().removeIf(v -> v <= 0);
        if (touches.retour() || touches.frappee(KeyEvent.VK_I)) {
            audio.jouer("ui_retour", 0.5f, 0, 1);
            return true;
        }
        List<Ligne> lignes = inv().lignes(onglet);
        int n = lignes.size();
        if (touches.frappee(KeyEvent.VK_TAB)) {
            changer(1);
            return false;
        }
        int avant = choix;
        if (touches.droite()) {
            if (n == 0 || choix % COLONNES == COLONNES - 1 || choix == n - 1) {
                changer(1);                                            // au bout de la rangee : l'onglet suivant
                return false;
            }
            choix++;
        } else if (touches.gauche()) {
            if (n == 0 || choix % COLONNES == 0) {
                changer(-1);
                return false;
            }
            choix--;
        } else if (touches.bas() && choix + COLONNES < n) {
            choix += COLONNES;
        } else if (touches.haut() && choix - COLONNES >= 0) {
            choix -= COLONNES;
        }
        if (choix != avant) {
            choixAge = 0;
            audio.jouer("tel_tic", 0.35f, 0, 1.1f);
        }
        if (n == 0) {
            return false;
        }
        choix = Math.min(choix, n - 1);
        Ligne l = lignes.get(choix);
        if (touches.valider()) {
            utiliser(l);
        } else if (touches.lacher()) {
            poser(l);
        }
        return false;
    }

    private void changer(int sens) {
        Categorie[] c = Categorie.values();
        onglet = c[Math.floorMod(onglet.ordinal() + sens, c.length)];
        choix = 0;
        choixAge = 0;
        audio.jouer("tel_tic", 0.4f, 0, 0.9f);
    }

    private void dire(String texte) {
        info = texte;
        infoReste = 2.4f;
    }

    private void utiliser(Ligne l) {
        Etat etat = progression.etat();
        switch (l.fiche().usage()) {
            case EQUIPER -> {
                if (inv().equipee() == l) {
                    inv().equiper(null);
                    dire("TU RANGES " + l.fiche().nom() + ".");
                    audio.jouer(Audio.Bus.EFFETS, "vetement", 0.4f, 0, 1.1f, 0);
                } else {
                    inv().equiper(l);
                    dire(l.fiche().nom() + " EN MAIN.");
                    audio.jouer(Audio.Bus.EFFETS, l.id.equals("bouteille") ? "tinte" : "ferraille", 0.45f, 0, 1.1f, 0);
                }
            }
            case UTILISER -> {                                         // le sandwich : on le mange
                if (inv().retirer(l.id, 1)) {
                    etat.soigner(25);
                    etat.reposer();
                    dire("TU MANGES. ÇA VA MIEUX.");
                    audio.jouer(Audio.Bus.EFFETS, "papier", 0.5f, 0, 1f, 0);
                }
            }
            case PORTE -> {
                dire("À LIVRER : " + l.fiche().pour() + ".");
                audio.jouer("tel_tic", 0.3f, 0, 0.8f);
            }
            default -> {
                dire(l.fiche().mission() ? "TU LE GARDES SUR TOI." : "IL SERT TOUT SEUL : TU L'AS SUR TOI.");
                audio.jouer("tel_tic", 0.3f, 0, 0.8f);
            }
        }
    }

    private void poser(Ligne l) {
        if (l.fiche().usage() != histoire.Inventaire.Usage.EQUIPER || l.fiche().mission()) {
            dire(l.fiche().mission() ? "TU EN AS BESOIN. TU LE GARDES." : l.fiche().categorie() == Categorie.ARMES
                    ? "TES MUNITIONS RESTENT DANS TES POCHES." : "CE N'EST PAS UNE CHOSE QU'ON POSE.");
            audio.jouer("ui_refus", 0.5f, 0, 1);
            return;
        }
        float dx = monde.joueur.versDroite ? 10 : -10;
        progression.etat().poser(l, monde.carte(), monde.joueur.x + dx, monde.joueur.y);
        dire(l.fiche().nom() + " POSÉE PAR TERRE.");
        audio.jouer(Audio.Bus.EFFETS, "ferraille", 0.5f, 0, 0.9f, 0);
    }

    // ------------------------------------------------------------------ l'affichage

    void dessiner(Rendu rendu) {
        float entree = Math.min(1f, age / 0.22f);
        float doux = 1 - (1 - entree) * (1 - entree) * (1 - entree);
        rendu.rect(0, 0, Rendu.L, Rendu.H, 0x05050A, Math.round(165 * doux));
        int y0 = Y + Math.round((1 - doux) * 18);
        rendu.rect(X - 1, y0 - 1, L + 2, H + 2, 0x4A4A62, 256);
        rendu.rect(X, y0, L, H, 0x14141E, 256);
        rendu.rect(X, y0, L, 3, Affichage.ORANGE, 256);
        affichage.police().ecrire(rendu, "TES POCHES", X + 12, y0 + 10, Affichage.BLANC, 2);
        // les onglets
        int tx = X + 132;
        for (Categorie c : Categorie.values()) {
            boolean ici = c == onglet;
            int nb = inv().lignes(c).stream().mapToInt(l -> l.quantite).sum();
            String t = c.nom + (nb > 0 ? "  " + nb : "");
            int l = affichage.petite().largeur(t, 1) + 14;
            rendu.rect(tx, y0 + 12, l, 13, ici ? TEINTES[c.ordinal()] : 0x22222E, 256);
            affichage.petite().ecrire(rendu, t, tx + 7, y0 + 15, ici ? Affichage.BLANC : Affichage.GRIS, 1);
            tx += l + 4;
        }
        affichage.petite().ecrire(rendu, "TAB", X + L - 26, y0 + 15, Affichage.SOMBRE, 1);
        // la grille
        List<Ligne> lignes = inv().lignes(onglet);
        int gx = X + 12;
        int gy = y0 + 36;
        for (int i = 0; i < COLONNES * 4; i++) {
            int cx = gx + (i % COLONNES) * (CASE + 4);
            int cy = gy + (i / COLONNES) * (CASE + 4);
            rendu.rect(cx, cy, CASE, CASE, 0x1C1C28, 256);
            if (i >= lignes.size()) {
                continue;
            }
            Ligne l = lignes.get(i);
            boolean ici = i == choix;
            Float brille = nouveaux.get(l.id);
            if (brille != null && brille > 0) {                         // il vient d'arriver : il brille
                rendu.rect(cx, cy, CASE, CASE, 0xFFF0C0, Math.round(70 * Math.min(1f, brille)));
            }
            Texture icone = res.image("obj_" + l.id);
            int saut = ici ? Math.round((float) Math.sin(Math.min(1f, choixAge * 5) * Math.PI) * -2) : 0;
            if (icone != null) {
                rendu.calqueEchelle(icone, cx + 2, cy + 2 + saut, 2);
            }
            if (l.quantite > 1) {
                String q = "×" + l.quantite;
                int lq = affichage.petite().largeur(q, 1) + 4;
                rendu.rect(cx + CASE - lq - 1, cy + CASE - 9, lq, 8, 0x0C0C12, 230);
                affichage.petite().ecrire(rendu, q, cx + CASE - lq + 1, cy + CASE - 8, Affichage.BLANC, 1);
            }
            if (inv().equipee() == l) {                                 // en main : un coin orange
                rendu.rect(cx, cy, 8, 2, Affichage.ORANGE, 256);
                rendu.rect(cx, cy, 2, 8, Affichage.ORANGE, 256);
            }
            if (l.fiche().usage() == Inventaire.Usage.PORTE) {          // a livrer : une pastille verte
                rendu.rect(cx + CASE - 5, cy + 2, 3, 3, Affichage.VERT, 256);
            }
            if (ici) {
                int c = TEINTES[onglet.ordinal()];
                rendu.rect(cx - 1, cy - 1, CASE + 2, 1, c, 256);
                rendu.rect(cx - 1, cy + CASE, CASE + 2, 1, c, 256);
                rendu.rect(cx - 1, cy - 1, 1, CASE + 2, c, 256);
                rendu.rect(cx + CASE, cy - 1, 1, CASE + 2, c, 256);
            }
        }
        // la fiche
        int fx = X + 12 + COLONNES * (CASE + 4) + 8;
        int fl = X + L - 12 - fx;
        rendu.rect(fx - 6, y0 + 32, 1, H - 52, 0x2A2A36, 256);
        if (lignes.isEmpty()) {
            String vide = switch (onglet) {
                case LIVRAISONS -> "RIEN À LIVRER. CE QU'ON TE CONFIE ARRIVE ICI.";
                case ARMES -> "TES POINGS. LES POUBELLES CACHENT PARFOIS DE QUOI SE DÉFENDRE.";
                default -> "RIEN QUI COMPTE, POUR L'INSTANT.";
            };
            int vy = y0 + 46;
            for (String m : affichage.police().couper(affichage.police().propre(vide), fl)) {
                affichage.police().ecrire(rendu, m, fx, vy, Affichage.SOMBRE, 1);
                vy += 12;
            }
        } else {
            fiche(rendu, lignes.get(Math.min(choix, lignes.size() - 1)), fx, y0 + 34, fl);
        }
        // les touches, et le retour d'une action
        int by = y0 + H - 14;
        if (infoReste > 0) {
            affichage.police().ecrire(rendu, affichage.police().propre(info), X + 12, by - 1, Affichage.BLANC, 1);
        } else {
            touches(rendu, X + 12, by, lignes.isEmpty() ? null : lignes.get(Math.min(choix, lignes.size() - 1)));
        }
    }

    private void fiche(Rendu rendu, Ligne l, int x, int y, int largeur) {
        Inventaire.Fiche f = l.fiche();
        Texture icone = res.image("obj_" + l.id);
        if (icone != null) {
            int flotte = Math.round((float) Math.sin(age * 2.4) * 1.5f);
            rendu.rect(x + 4, y + 50, 40, 2, 0x000000, 90);
            rendu.calqueEchelle(icone, x, y + flotte, 3);
        }
        int tx = x + 54;
        affichage.petite().ecrire(rendu, f.categorie().nom, tx, y + 2, TEINTES[f.categorie().ordinal()], 1);
        int ny = y + 12;
        for (String m : affichage.police().couper(affichage.police().propre(f.nom()), largeur - 54)) {
            affichage.police().ecrire(rendu, m, tx, ny, Affichage.BLANC, 1);
            ny += 12;
        }
        if (l.quantite > 1) {
            affichage.petite().ecrire(rendu, "QUANTITÉ : " + l.quantite, tx, ny + 2, Affichage.GRIS, 1);
        }
        int dy = Math.max(y + 58, ny + 14);
        for (String m : affichage.police().couper(affichage.police().propre(f.description()), largeur)) {
            affichage.police().ecrire(rendu, m, x, dy, Affichage.GRIS, 1);
            dy += 12;
        }
        dy += 4;
        if (f.pour() != null) {
            dy = info(rendu, x, dy, "POUR", f.pour(), Affichage.VERT);
        }
        if (f.valeur() > 0 && f.categorie() == Categorie.LIVRAISONS) {
            dy = info(rendu, x, dy, "VAUT", f.valeur() + " € À LA LIVRAISON", Affichage.BLANC);
        }
        Etat.Arme arme = f.categorie() == Categorie.ARMES ? Etat.arme(l.id) : null;
        if (arme != null && arme.aFeu()) {                               // une arme a feu : son rythme, son chargeur, ses balles
            dy = info(rendu, x, dy, "DÉGÂTS", Math.round(arme.degats()) + "  ·  PORTÉE " + Math.round(arme.portee()), Affichage.BLANC);
            dy = info(rendu, x, dy, "CADENCE", Math.round(arme.cadence()) + " PAR SECONDE" + (arme.automatique() ? ", EN RAFALE" : ", COUP PAR COUP"),
                    Affichage.BLANC);
            dy = info(rendu, x, dy, "CHARGEUR", l.usure + " / " + arme.chargeur() + "  ·  RECHARGE " + String.format("%.1f", arme.rechargement())
                    .replace('.', ',') + " S", l.usure == 0 ? Affichage.ORANGE : Affichage.BLANC);
            int reserve = inv().quantite(arme.munition());
            dy = info(rendu, x, dy, "BALLES", reserve + " " + Inventaire.fiche(arme.munition()).nom(), reserve == 0 ? Affichage.ORANGE : Affichage.VERT);
        } else if (arme != null) {
            dy = info(rendu, x, dy, "DÉGÂTS", Math.round(arme.degats()) + "  ·  PORTÉE " + Math.round(arme.portee()), Affichage.BLANC);
            if (arme.coups() > 0) {
                int reste = arme.coups() - l.usure;
                affichage.petite().ecrire(rendu, "USURE", x, dy, Affichage.SOMBRE, 1);
                for (int k = 0; k < arme.coups(); k++) {
                    rendu.rect(x + 40 + k * 7, dy + 1, 5, 5, k < reste ? Affichage.ORANGE : 0x2A2A36, 256);
                }
                dy += 11;
            } else {
                dy = info(rendu, x, dy, "USURE", "NE CASSE PAS", Affichage.VERT);
            }
        }
        if (f.mission()) {
            info(rendu, x, dy, "HISTOIRE", "TU NE PEUX PAS T'EN DÉFAIRE", Affichage.SOMBRE);
        }
    }

    private int info(Rendu rendu, int x, int y, String nom, String valeur, int couleur) {
        affichage.petite().ecrire(rendu, nom, x, y, Affichage.SOMBRE, 1);
        affichage.petite().ecrire(rendu, valeur, x + 40, y, couleur, 1);
        return y + 10;
    }

    private void touches(Rendu rendu, int x, int y, Ligne l) {
        java.util.List<String[]> t = new java.util.ArrayList<>();
        if (l != null) {
            switch (l.fiche().usage()) {
                case EQUIPER -> t.add(new String[] {"E", inv().equipee() == l ? "RANGER" : "PRENDRE EN MAIN"});
                case UTILISER -> t.add(new String[] {"E", "MANGER"});
                case LANCER -> t.add(new String[] {"H", "LANCER, EN COMBAT"});
                default -> { }
            }
            if (l.fiche().usage() == histoire.Inventaire.Usage.EQUIPER && !l.fiche().mission()) {
                t.add(new String[] {"G", "POSER PAR TERRE"});
            }
        }
        t.add(new String[] {"TAB", "CATÉGORIE"});
        t.add(new String[] {"I", "FERMER"});
        for (String[] k : t) {
            int lk = affichage.petite().largeur(k[0], 1) + 4;
            rendu.rect(x, y - 1, lk, 8, 0x2A2A36, 256);
            affichage.petite().ecrire(rendu, k[0], x + 2, y, Affichage.BLANC, 1);
            x += lk + 3;
            affichage.petite().ecrire(rendu, k[1], x, y, Affichage.SOMBRE, 1);
            x += affichage.petite().largeur(k[1], 1) + 9;
        }
    }
}
