package jeu;

import decor.Rendu;
import histoire.Etat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ce qui fait vivre l'image et le son autour du joueur, sans rien changer au jeu : la pluie qui gicle sous les pas,
 * la buee du souffle dans le froid, la fumee du scooter, la vapeur des bouches d'egout, les phares qui passent et se
 * refletent sur la chaussee, l'orage, les etincelles d'une enseigne, la poussiere dans la lumiere de la tele. Et le
 * corps : l'image qui tremble et rougit quand on prend un coup, le coeur qu'on entend quand on est blesse.
 */
final class Effets {
    /** Une particule : en coordonnees du monde (elle defile avec la camera) ou de l'ecran (l'interface). */
    private static final class Grain {
        float x, y, vx, vy, gravite, vie, duree, taille, croissance;
        int couleur, opacite;
        boolean additif, ecran;
    }

    /** Une voiture qui passe sur la chaussee, de nuit : on ne voit que ses phares et leur reflet sur l'eau. */
    private static final class Voiture {
        float x, vitesse;
        int sens;
    }

    private final Monde monde;
    private final Audio audio;
    private final Random hasard = new Random(11);
    private final List<Grain> grains = new ArrayList<>();
    private final List<Voiture> voitures = new ArrayList<>();
    private float prochainEclair = 18f;
    private float eclair;
    private float tonnerreDans = -1f;
    private boolean tonnerreDedans;
    private float prochaineVoiture = 4f;
    private float prochaineEtincelle = 9f;
    private float prochainSouffle = 2f;
    private float rythmeGouttes;
    private float rythmeFumee;
    private float rythmeVapeur;
    private float coeurDans;
    private float blessure;
    private boolean etaitEssouffle;
    private String carteVue = "";

    Effets(Monde monde, Audio audio) {
        this.monde = monde;
        this.audio = audio;
    }

    // ------------------------------------------------------------------ ce qui arrive au corps

    /** Un coup, une chute : l'image tremble et rougit, on l'entend. */
    void blessure(float force) {
        blessure = Math.max(blessure, Math.min(1f, force));
        monde.trembler(3f + 5f * force);
        audio.jouer("aie_" + (1 + hasard.nextInt(3)), 0.8f, 0, 0.95f + hasard.nextFloat() * 0.1f);
    }

    /** De l'argent qui rentre : quelques eclats dores pres du solde, en bas a gauche. */
    void pieces(int x, int y) {
        for (int i = 0; i < 9; i++) {
            Grain g = grain(x + hasard.nextFloat() * 50, y + 3, (hasard.nextFloat() - 0.5f) * 30, -30 - hasard.nextFloat() * 40,
                    0.7f + hasard.nextFloat() * 0.4f, 1, 0xFFC860, 230);
            g.additif = true;
            g.ecran = true;
            g.gravite = 60;
        }
    }

    /** Un coup qui porte : une gerbe d'eclats a l'endroit du choc (coordonnees du monde). */
    void impact(float x, float y, int couleur, int nombre) {
        for (int i = 0; i < nombre; i++) {
            double a = hasard.nextDouble() * Math.PI * 2;
            float v = 40 + hasard.nextFloat() * 90;
            Grain g = grain(x, y, (float) Math.cos(a) * v, (float) Math.sin(a) * v - 30, 0.25f + hasard.nextFloat() * 0.25f, 1,
                    couleur, 255);
            g.additif = true;
            g.gravite = 220;
        }
    }

    private Grain grain(float x, float y, float vx, float vy, float duree, float taille, int couleur, int opacite) {
        Grain g = new Grain();
        g.x = x;
        g.y = y;
        g.vx = vx;
        g.vy = vy;
        g.duree = duree;
        g.taille = taille;
        g.couleur = couleur;
        g.opacite = opacite;
        grains.add(g);
        return g;
    }

    // ------------------------------------------------------------------ le temps qui passe

    void maj(float dt, boolean pluie, Etat etat) {
        String carte = monde.carte();
        if (!carte.equals(carteVue)) {                                 // nouveau lieu : on repart de zero
            carteVue = carte;
            grains.removeIf(g -> !g.ecran);
            voitures.clear();
        }
        boolean dehors = monde.dehors();
        boolean avenue = monde.carte().startsWith("avenue");     // les voitures ne passent pas dans la cour
        var joueur = monde.joueur;
        // la pluie gicle sous les pas, plus fort quand on court ou qu'on roule
        if (dehors && pluie && joueur.enMouvement) {
            rythmeGouttes -= dt;
            if (rythmeGouttes <= 0) {
                rythmeGouttes = monde.enScooter() ? 0.04f : monde.court() ? 0.07f : 0.13f;
                for (int i = 0; i < (monde.enScooter() ? 3 : 2); i++) {
                    Grain g = grain(joueur.x + (hasard.nextFloat() - 0.5f) * 10, joueur.y - 1, (hasard.nextFloat() - 0.5f) * 50,
                            -30 - hasard.nextFloat() * 50, 0.3f + hasard.nextFloat() * 0.2f, 1, 0xB8C4D8, 170);
                    g.gravite = 320;
                }
                if (monde.court() && hasard.nextFloat() < 0.12f) {
                    audio.jouer("flaque", 0.35f, 0, 0.9f + hasard.nextFloat() * 0.2f);
                }
            }
        }
        // la buee du souffle, dans le froid
        if (dehors && !monde.enScooter()) {
            prochainSouffle -= dt * (monde.court() ? 2.2f : 1f);
            if (prochainSouffle <= 0) {
                prochainSouffle = 3f + hasard.nextFloat();
                int sens = joueur.versDroite ? 1 : -1;
                for (int i = 0; i < 5; i++) {
                    Grain g = grain(joueur.x + sens * 6, joueur.y - 41 + hasard.nextFloat() * 2, sens * (10 + hasard.nextFloat() * 10),
                            -6 - hasard.nextFloat() * 6, 1.3f + hasard.nextFloat() * 0.5f, 1.5f, 0xDDE2EC, 70);
                    g.croissance = 2.2f;
                }
            }
        }
        // la fumee du deux-temps
        if (monde.enScooter() && joueur.enMouvement) {
            rythmeFumee -= dt;
            if (rythmeFumee <= 0) {
                rythmeFumee = 0.07f;
                int sens = joueur.versDroite ? 1 : -1;
                Grain g = grain(joueur.x - sens * 22, joueur.y - 7, -sens * (20 + hasard.nextFloat() * 20), -6 - hasard.nextFloat() * 8,
                        0.8f + hasard.nextFloat() * 0.4f, 2, 0x8A8C98, 80);
                g.croissance = 3f;
            }
        }
        // la vapeur qui monte des bouches d'egout, sur la chaussee
        if (avenue) {
            rythmeVapeur -= dt;
            if (rythmeVapeur <= 0) {
                rythmeVapeur = 0.12f;
                float cam = monde.camX();
                for (int b = (int) Math.floor(cam / 560f) - 1; b <= (int) Math.floor((cam + Rendu.L) / 560f) + 1; b++) {
                    float bx = b * 560f + 260f;
                    Grain g = grain(bx + (hasard.nextFloat() - 0.5f) * 14, 238, (hasard.nextFloat() - 0.4f) * 8, -14 - hasard.nextFloat() * 10,
                            2.2f + hasard.nextFloat(), 3, 0xC8CCD8, 34);
                    g.croissance = 4f;
                }
            }
        }
        // la poussiere qui flotte dans la lumiere de la tele, chez soi
        if (carte.equals("appartement") && hasard.nextFloat() < dt * 3f) {
            Grain g = grain(250 + hasard.nextFloat() * 140, 110 + hasard.nextFloat() * 90, (hasard.nextFloat() - 0.5f) * 4,
                    -2 - hasard.nextFloat() * 3, 5f + hasard.nextFloat() * 3, 1, 0x9AB4FF, 80);
            g.additif = true;
        }
        // des etincelles, de temps en temps, sur l'enseigne du Maitre Poulet
        float[] poulet = monde.scene().reperes.get("poulet");
        if (carte.equals("avenue_bas") && poulet != null && Math.abs(poulet[0] - joueur.x) < 300) {
            prochaineEtincelle -= dt;
            if (prochaineEtincelle <= 0) {
                prochaineEtincelle = 7f + hasard.nextFloat() * 12f;
                for (int i = 0; i < 8; i++) {
                    Grain g = grain(poulet[0] + 30 + hasard.nextFloat() * 6, 104, (hasard.nextFloat() - 0.5f) * 70,
                            -20 - hasard.nextFloat() * 40, 0.5f + hasard.nextFloat() * 0.4f, 1, 0xFFB060, 255);
                    g.additif = true;
                    g.gravite = 260;
                }
                audio.jouer("gresillement", 0.45f, 0, 1f);
            }
        }
        // les voitures qui passent
        if (avenue) {
            prochaineVoiture -= dt;
            if (prochaineVoiture <= 0) {
                prochaineVoiture = 8f + hasard.nextFloat() * 12f;
                Voiture v = new Voiture();
                v.sens = hasard.nextBoolean() ? 1 : -1;
                v.vitesse = 300 + hasard.nextFloat() * 160;
                v.x = monde.camX() + (v.sens > 0 ? -260 : Rendu.L + 260);
                voitures.add(v);
                // sur la chaussee mouillee, les pneus chuintent ; au sec, on entend surtout le moteur et le roulement
                audio.jouer(pluie ? "voiture_passe" : "voiture_passe_sec", 0.55f, v.sens * 0.3f, 0.85f + hasard.nextFloat() * 0.3f);
            }
        }
        for (Voiture v : voitures) {
            v.x += v.sens * v.vitesse * dt;
        }
        voitures.removeIf(v -> Math.abs(v.x - (monde.camX() + Rendu.L / 2f)) > 900);
        // l'orage : l'eclair d'abord, le tonnerre ensuite ; dedans, on le voit par la fenetre et on l'entend etouffe
        if (pluie && !carte.equals("metro") && !carte.equals("namek")) {
            prochainEclair -= dt;
            if (prochainEclair <= 0) {
                prochainEclair = 28f + hasard.nextFloat() * 40f;
                eclair = 1f;
                tonnerreDans = 0.6f + hasard.nextFloat() * 1.8f;
                tonnerreDedans = !dehors;
            }
        }
        if (tonnerreDans > 0) {
            tonnerreDans -= dt;
            if (tonnerreDans <= 0) {
                // dedans, le mur l'etouffe deja (le bus du dehors passe par son filtre) : on le baisse a peine
                audio.jouer("orage_" + (1 + hasard.nextInt(3)), tonnerreDedans ? 0.6f : 0.75f, (hasard.nextFloat() - 0.5f) * 0.8f, 1f);
            }
        }
        eclair = Math.max(0f, eclair - dt * 2.4f);
        // le corps : le coup qui s'efface, le coeur quand on est blesse, le souffle quand on est a bout
        blessure = Math.max(0f, blessure - dt * 1.6f);
        if (etat.blesse()) {
            coeurDans -= dt;
            if (coeurDans <= 0) {
                boolean grave = etat.sante() < 18f;
                coeurDans = grave ? 0.62f : 1.05f;
                audio.jouer(grave ? "coeur_vite" : "coeur", 0.55f, 0, 1f);
            }
        } else {
            coeurDans = 0;
        }
        if (etat.essouffle() && !etaitEssouffle) {
            audio.jouer("souffle", 0.6f, 0, 1f);
        }
        etaitEssouffle = etat.essouffle();
        // les particules vivent
        for (Grain g : grains) {
            g.vie += dt;
            g.vy += g.gravite * dt;
            g.x += g.vx * dt;
            g.y += g.vy * dt;
            g.taille += g.croissance * dt;
        }
        grains.removeIf(g -> g.vie >= g.duree);
        if (grains.size() > 900) {
            grains.subList(0, grains.size() - 900).clear();
        }
    }

    // ------------------------------------------------------------------ l'image

    /** Ce qui vit dans le decor : a dessiner juste apres lui, avant l'interface. */
    void dessinerMonde(Rendu rendu, boolean pluie) {
        float cam = monde.camX();
        for (Voiture v : voitures) {                                   // les phares, et leur trainee sur l'eau
            float x = v.x - cam;
            float avant = x + v.sens * 34;
            float arriere = x - v.sens * 34;
            rendu.halo(avant + v.sens * 50, 238, 90, 14, 0xFFF2C8, 0.35f);   // le cone sur la chaussee
            rendu.halo(avant, 232, 14, 8, 0xFFF6DC, 0.95f);
            rendu.halo(arriere, 232, 9, 6, 0xFF3020, 0.8f);
            if (pluie) {
                rendu.halo(avant, 252, 6, 18, 0xFFF0C8, 0.4f);          // le reflet, etire vers le bas
                rendu.halo(arriere, 250, 4, 14, 0xFF3020, 0.35f);
            }
            rendu.halo(x, 150, 260, 70, 0xFFE8C0, 0.05f);                // les facades eclairees au passage
        }
        for (Grain g : grains) {
            if (g.ecran) {
                continue;
            }
            dessinerGrain(rendu, g, Math.round(g.x - cam), Math.round(g.y));
        }
        if (eclair > 0) {                                               // l'eclair : le ciel blanchit, deux fois
            float k = eclair > 0.75f ? 1f : eclair > 0.55f ? 0.2f : eclair * 0.9f;
            rendu.voile(0xD8E2FF, (tonnerreDedans ? 0.18f : 0.55f) * k);
        }
    }

    private static void dessinerGrain(Rendu rendu, Grain g, int x, int y) {
        float reste = 1f - g.vie / g.duree;
        int t = Math.max(1, Math.round(g.taille));
        int force = Math.round(g.opacite * Math.min(1f, reste * 1.6f));
        if (g.additif) {
            for (int j = 0; j < t; j++) {
                for (int i = 0; i < t; i++) {
                    rendu.ajouter(x + i, y + j, g.couleur, force);
                }
            }
        } else if (g.taille > 2.2f) {                                  // la vapeur, la buee, la fumee : des taches douces
            rendu.tache(x, y, g.taille, g.couleur, force);
        } else {
            rendu.rect(x - t / 2, y - t / 2, t, t, g.couleur, force);
        }
    }

    /** Ce qui pese sur l'image entiere : le rouge d'un coup, le rouge qui bat quand on est blesse, le noir du souffle court. */
    void dessinerVoiles(Rendu rendu, Etat etat, long tempsMs) {
        if (blessure > 0) {
            rendu.vignette(0xB01818, blessure * 0.9f);
        }
        if (etat.blesse()) {
            float battement = (float) Math.pow(Math.max(0, Math.sin(tempsMs / (etat.sante() < 18f ? 620.0 : 1050.0) * Math.PI * 2)), 6);
            rendu.vignette(0x6A0A10, 0.25f + 0.35f * battement * (etat.sante() < 18f ? 1.4f : 1f));
        }
        if (etat.essouffle()) {
            rendu.vignette(0x000000, 0.45f);
        }
    }

    /** Ce qui se pose par-dessus l'interface : les eclats de l'argent. */
    void dessinerInterface(Rendu rendu) {
        for (Grain g : grains) {
            if (g.ecran) {
                dessinerGrain(rendu, g, Math.round(g.x), Math.round(g.y));
            }
        }
    }
}
