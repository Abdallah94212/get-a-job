package decor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Dessine le decor en 2D dans une image de 480 x 270 : des couches qui defilent a des vitesses differentes,
 * des objets et des personnages tries par profondeur, des lumieres ajoutees par-dessus et une trame de console
 * des annees 2000. Il sait aussi poser l'interface et les effets des cinematiques par-dessus.
 */
public final class Rendu {
    public static final int L = 480;
    public static final int H = 270;
    private static final int[] TRAME = {0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5};

    public final int[] image = new int[L * H];
    public boolean tramage = true;
    private long temps;

    /** Dessine le decor et ses personnages, la camera ayant son bord gauche en camX. */
    public void dessiner(Scene scene, float camX, long tempsMs, List<Acteur> acteurs) {
        temps = tempsMs;
        ciel(scene, camX);
        couche(scene.couches.get(Scene.LOIN), camX);
        couche(scene.couches.get(Scene.FOND), camX);
        List<Acteur> tries = new ArrayList<>(acteurs);
        tries.sort(Comparator.comparingDouble(a -> a.y));
        int suivant = 0;
        List<Element> objets = scene.couches.get(Scene.OBJETS);
        for (int i = 0; i < objets.size(); i++) {
            Element e = objets.get(i);
            while (suivant < tries.size() && tries.get(suivant).y < e.base()) {
                personnage(tries.get(suivant++), camX);
            }
            element(e, i, camX);
        }
        while (suivant < tries.size()) {
            personnage(tries.get(suivant++), camX);
        }
        couche(scene.couches.get(Scene.DEVANT), camX);
        couche(scene.couches.get(Scene.LUMIERES), camX);
        if (tramage) {
            tramer();
        }
    }

    private void ciel(Scene scene, float camX) {
        Texture ciel = scene.ciel;
        int decalage = Math.round(camX * 0.04f);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                image[y * L + x] = ciel != null && y < ciel.hauteur()
                        ? ciel.pixels()[y * ciel.largeur() + Math.floorMod(x + decalage, ciel.largeur())]
                        : 0xFF000000 | scene.nuit;
            }
        }
    }

    private void couche(List<Element> elements, float camX) {
        for (int i = 0; i < elements.size(); i++) {
            element(elements.get(i), i, camX);
        }
    }

    private static float bruit(long n) {
        n = (n ^ 61) ^ (n >>> 16);
        n *= 9;
        n ^= n >>> 4;
        n *= 0x27d4eb2dL;
        n ^= n >>> 15;
        return (n & 0xFFFF) / 65535f;
    }

    private void element(Element e, int indice, float camX) {
        int sx = Math.round(e.x() - camX * e.parallaxe());
        if (sx >= L || sx + e.texture().largeur() <= 0) {
            return;
        }
        if (e.a(Element.CLIGNOTE) && (temps / 700 + indice) % 2 == 0) {
            return;
        }
        float lumiere = e.lumiere();
        if (e.a(Element.SCINTILLE)) {
            lumiere *= 0.5f + 0.5f * bruit(temps / 110 + indice * 31L);
        }
        coller(e.texture(), sx, Math.round(e.y()), e.a(Element.MIROIR), Math.round(lumiere * 256), e.a(Element.ADDITIF));
    }

    private void coller(Texture t, int sx, int sy, boolean miroir, int lum256, boolean additif) {
        int tw = t.largeur();
        int[] source = t.pixels();
        for (int ty = Math.max(0, -sy); ty < t.hauteur() && sy + ty < H; ty++) {
            int ligne = (sy + ty) * L;
            for (int tx = Math.max(0, -sx); tx < tw && sx + tx < L; tx++) {
                int argb = source[ty * tw + (miroir ? tw - 1 - tx : tx)];
                if ((argb >>> 24) < 128) {
                    continue;
                }
                int i = ligne + sx + tx;
                int fond = additif ? image[i] : 0;
                int r = Math.min(255, ((fond >> 16) & 255) + (((argb >> 16) & 255) * lum256 >> 8));
                int v = Math.min(255, ((fond >> 8) & 255) + (((argb >> 8) & 255) * lum256 >> 8));
                int b = Math.min(255, (fond & 255) + ((argb & 255) * lum256 >> 8));
                image[i] = 0xFF000000 | r << 16 | v << 8 | b;
            }
        }
    }

    /** Un personnage et son ombre au sol. */
    private void personnage(Acteur acteur, float camX) {
        Texture t = acteur.texture;
        int pied = Math.round(acteur.y);
        int centre = Math.round(acteur.x - camX);
        if (t == null || centre < -t.largeur() || centre > L + t.largeur()) {
            return;
        }
        if (acteur.ombre) {
            int demi = Math.min(16, t.largeur() / 2 - 1);
            for (int oy = -1; oy <= 1; oy++) {
                for (int ox = -demi + Math.abs(oy) * 2; ox <= demi - Math.abs(oy) * 2; ox++) {
                    int px = centre + ox;
                    int py = pied + oy;
                    if (px >= 0 && px < L && py >= 0 && py < H) {
                        image[py * L + px] = 0xFF000000 | ((image[py * L + px] >> 1) & 0x7F7F7F);
                    }
                }
            }
        }
        boolean second = acteur.marche && (temps / 170) % 2 == 0;
        Texture dessin = second && acteur.pas != null ? acteur.pas : t;
        int saut = second && acteur.pas == null ? 1 : 0;
        coller(dessin, centre - dessin.largeur() / 2, pied - dessin.hauteur() - acteur.hauteur - saut, acteur.miroir, 256, false);
    }

    /** Reduit a 15 bits avec une trame, comme sur les consoles de l'epoque. */
    private void tramer() {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                int i = y * L + x;
                int decalage = (TRAME[(y & 3) * 4 + (x & 3)] >> 1) - 4;
                int c = image[i];
                int r = Math.max(0, Math.min(255, ((c >> 16) & 255) + decalage)) & 0xF8;
                int v = Math.max(0, Math.min(255, ((c >> 8) & 255) + decalage)) & 0xF8;
                int b = Math.max(0, Math.min(255, (c & 255) + decalage)) & 0xF8;
                image[i] = 0xFF000000 | r << 16 | v << 8 | b;
            }
        }
    }

    // ------------------------------------------------------------------ l'interface, posee par-dessus le decor

    /** Pose une image d'interface, son coin haut gauche en (x, y). */
    public void calque(Texture t, int x, int y) {
        coller(t, x, y, false, 256, false);
    }

    /** Pose une image d'interface centree en x. */
    public void calqueCentre(Texture t, int centreX, int y) {
        coller(t, centreX - t.largeur() / 2, y, false, 256, false);
    }

    /** Un rectangle plein. opacite va de 0 (invisible) a 256 (opaque). */
    public void rect(int x, int y, int largeur, int hauteur, int rvb, int opacite) {
        int r = (rvb >> 16) & 255;
        int v = (rvb >> 8) & 255;
        int b = rvb & 255;
        for (int py = Math.max(0, y); py < Math.min(H, y + hauteur); py++) {
            for (int px = Math.max(0, x); px < Math.min(L, x + largeur); px++) {
                int c = image[py * L + px];
                int nr = ((c >> 16) & 255) + ((r - ((c >> 16) & 255)) * opacite >> 8);
                int nv = ((c >> 8) & 255) + ((v - ((c >> 8) & 255)) * opacite >> 8);
                int nb = (c & 255) + ((b - (c & 255)) * opacite >> 8);
                image[py * L + px] = 0xFF000000 | nr << 16 | nv << 8 | nb;
            }
        }
    }

    /** Un cadre d'interface : fond sombre, lisere clair, comme les etiquettes des lieux. */
    public void cadre(int x, int y, int largeur, int hauteur) {
        rect(x, y, largeur, hauteur, 0x60606C, 256);
        rect(x + 1, y + 1, largeur - 2, hauteur - 2, 0x14141A, 236);
    }

    public void point(int x, int y, int rvb) {
        if (x >= 0 && x < L && y >= 0 && y < H) {
            image[y * L + x] = 0xFF000000 | rvb;
        }
    }

    // ------------------------------------------------------------------ les effets des cinematiques

    /** Assombrit toute l'image : sert aux fondus. part va de 0 (rien) a 1 (noir). */
    public void fondu(float part) {
        rect(0, 0, L, H, 0x000000, Math.round(Math.max(0f, Math.min(1f, part)) * 256));
    }

    /** Retire la couleur : les souvenirs. part va de 0 (rien) a 1 (tout gris). */
    public void gris(float part) {
        int k = Math.round(part * 256);
        for (int i = 0; i < image.length; i++) {
            int c = image[i];
            int r = (c >> 16) & 255;
            int v = (c >> 8) & 255;
            int b = c & 255;
            int g = Math.min(255, (r * 77 + v * 150 + b * 29 >> 8) * 9 / 8);
            image[i] = 0xFF000000 | (r + ((g - r) * k >> 8)) << 16 | (v + ((g - v) * k >> 8)) << 8 | (b + ((g - b) * k >> 8));
        }
    }

    /** Voile de couleur sur toute l'image : la lueur verte, l'ecran rouge, l'eclair blanc. */
    public void voile(int rvb, float part) {
        rect(0, 0, L, H, rvb, Math.round(Math.max(0f, Math.min(1f, part)) * 256));
    }

    /** L'autre dimension : les couleurs glissent en bandes, comme une flaque d'essence. */
    public void irise(long tempsMs) {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < L; x++) {
                int c = image[y * L + x];
                double phase = (x + y * 2 + tempsMs / 6.0) / 38.0;
                int r = (int) (((c >> 16) & 255) * (0.75 + 0.45 * Math.sin(phase)));
                int v = (int) (((c >> 8) & 255) * (0.75 + 0.45 * Math.sin(phase + 2.1)));
                int b = (int) ((c & 255) * (0.75 + 0.45 * Math.sin(phase + 4.2)));
                image[y * L + x] = 0xFF000000 | Math.min(255, r + 14) << 16 | Math.min(255, v + 14) << 8 | Math.min(255, b + 22);
            }
        }
    }

    /**
     * La pluie : des traits fins qui tombent en biais, plus serres et plus clairs au premier plan, et des eclaboussures
     * sur le trottoir et la chaussee. force va de 0 (rien) a 1 (averse).
     */
    public void pluie(long tempsMs, float camX, float force) {
        if (force <= 0) {
            return;
        }
        int gouttes = Math.round(220 * force);
        for (int k = 0; k < gouttes; k++) {
            long graine = k * 7919L + 13;
            float profondeur = 0.4f + 0.6f * bruit(graine);
            float vitesse = 260 + 260 * profondeur;
            int longueur = 4 + Math.round(8 * profondeur);
            float x0 = bruit(graine + 1) * (L + 120) - (camX * (0.3f + 0.7f * profondeur)) % (L + 120);
            float y0 = (bruit(graine + 2) * (H + 40) + tempsMs / 1000f * vitesse) % (H + 40) - 20;
            int x = Math.floorMod(Math.round(x0 - y0 * 0.18f), L + 120) - 60;
            int clair = 0x8890A0 + (profondeur > 0.8f ? 0x202020 : 0);
            int opacite = Math.round(40 + 70 * profondeur);
            for (int i = 0; i < longueur; i++) {
                int px = x - Math.round(i * 0.18f);
                int py = Math.round(y0) - i;
                if (px >= 0 && px < L && py >= 0 && py < H) {
                    melanger(py * L + px, clair, opacite);
                }
            }
        }
        int eclaboussures = Math.round(46 * force);
        for (int k = 0; k < eclaboussures; k++) {
            long graine = k * 104729L + tempsMs / 90;
            int x = Math.round(bruit(graine) * L);
            int y = 186 + Math.round(bruit(graine + 3) * 80);
            melanger(y * L + x, 0xA0A8B8, 90);
            if (x + 1 < L) {
                melanger(y * L + x + 1, 0xA0A8B8, 50);
            }
            if (y > 0 && x > 0) {
                melanger((y - 1) * L + x - 1, 0xA0A8B8, 40);
            }
        }
    }

    private void melanger(int i, int rvb, int opacite) {
        int c = image[i];
        int r = ((c >> 16) & 255) + ((((rvb >> 16) & 255) - ((c >> 16) & 255)) * opacite >> 8);
        int v = ((c >> 8) & 255) + ((((rvb >> 8) & 255) - ((c >> 8) & 255)) * opacite >> 8);
        int b = (c & 255) + (((rvb & 255) - (c & 255)) * opacite >> 8);
        image[i] = 0xFF000000 | r << 16 | v << 8 | b;
    }

    /** Les deux bandes noires du cinema, en haut et en bas. */
    public void bandes(int hauteur) {
        rect(0, 0, L, hauteur, 0x000000, 256);
        rect(0, H - hauteur, L, hauteur, 0x000000, 256);
    }
}
