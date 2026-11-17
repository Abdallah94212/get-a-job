package decor;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Des poses tirees d'un dessin existant, pour le combat : penche en avant (il frappe) ou en arriere (il encaisse, il
 * arme son coup), tasse (accroupi, il se releve, il est a bout de souffle), couche (au sol). Le style reste celui du
 * dessin : ce sont ses propres pixels, deplaces, jamais redessines. Chaque pose est calculee une fois et gardee.
 */
public final class Poses {
    private static final Map<Texture, Map<String, Texture>> CACHE = new IdentityHashMap<>();

    private Poses() {
    }

    private static Texture memo(Texture t, String cle, java.util.function.Supplier<Texture> calcul) {
        if (t == null) {
            return null;
        }
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(t, k -> new HashMap<>()).computeIfAbsent(cle, k -> calcul.get());
        }
    }

    /**
     * Le haut du corps glisse de "decalage" pixels (positif : vers l'avant, c'est-a-dire vers la droite du dessin), les
     * pieds restent en place. Le dessin s'elargit d'autant pour ne rien couper.
     */
    public static Texture penche(Texture t, int decalage) {
        return memo(t, "penche" + decalage, () -> {
            int marge = Math.abs(decalage);
            int l = t.largeur() + 2 * marge;
            int[] p = new int[l * t.hauteur()];
            for (int y = 0; y < t.hauteur(); y++) {
                float haut = 1f - (float) y / Math.max(1, t.hauteur() - 1);  // 1 en haut, 0 aux pieds
                int dx = Math.round(decalage * haut * haut);
                for (int x = 0; x < t.largeur(); x++) {
                    int c = t.pixels()[y * t.largeur() + x];
                    if ((c >>> 24) != 0) {
                        p[y * l + x + marge + dx] = c;
                    }
                }
            }
            return new Texture(l, t.hauteur(), p);
        });
    }

    /** Le meme dessin, retourne de gauche a droite (une arme tenue par quelqu'un tourne vers la gauche). */
    public static Texture miroir(Texture t) {
        return memo(t, "miroir", () -> {
            int l = t.largeur();
            int[] p = new int[l * t.hauteur()];
            for (int y = 0; y < t.hauteur(); y++) {
                for (int x = 0; x < l; x++) {
                    p[y * l + (l - 1 - x)] = t.pixels()[y * l + x];
                }
            }
            return new Texture(l, t.hauteur(), p);
        });
    }

    /** Ecrase verticalement (k entre 0 et 1), les pieds au sol : accroupi, a genoux, plie en deux. */
    public static Texture tasse(Texture t, float k) {
        return memo(t, "tasse" + k, () -> {
            int h = Math.max(1, Math.round(t.hauteur() * k));
            int[] p = new int[t.largeur() * h];
            for (int y = 0; y < h; y++) {
                int sy = Math.min(t.hauteur() - 1, Math.round(y / k));
                System.arraycopy(t.pixels(), sy * t.largeur(), p, y * t.largeur(), t.largeur());
            }
            return new Texture(t.largeur(), h, p);
        });
    }

    /** Couche au sol : la tete du cote "versGauche", les pieds de l'autre. */
    public static Texture couche(Texture t, boolean versGauche) {
        return memo(t, "couche" + versGauche, () -> {
            int l = t.hauteur();
            int h = t.largeur();
            int[] p = new int[l * h];
            for (int y = 0; y < t.hauteur(); y++) {
                for (int x = 0; x < t.largeur(); x++) {
                    int c = t.pixels()[y * t.largeur() + x];
                    int nx = versGauche ? y : t.hauteur() - 1 - y;
                    int ny = versGauche ? t.largeur() - 1 - x : x;
                    p[ny * l + nx] = c;
                }
            }
            return new Texture(l, h, p);
        });
    }

    /**
     * Une vignette carree pour une liste de contacts : le haut du portrait (la tete et les epaules), reduit en moyennant
     * les pixels, pour garder les traits lisibles a 16 pixels.
     */
    public static Texture vignette(Texture t, int cote) {
        return memo(t, "vignette" + cote, () -> {
            int carre = Math.min(t.largeur(), Math.round(t.hauteur() * 0.78f));
            int x0 = (t.largeur() - carre) / 2;
            int y0 = Math.round(t.hauteur() * 0.06f);
            int[] p = new int[cote * cote];
            for (int y = 0; y < cote; y++) {
                for (int x = 0; x < cote; x++) {
                    int sx0 = x0 + x * carre / cote;
                    int sx1 = Math.max(sx0 + 1, x0 + (x + 1) * carre / cote);
                    int sy0 = y0 + y * carre / cote;
                    int sy1 = Math.max(sy0 + 1, y0 + (y + 1) * carre / cote);
                    long a = 0;
                    long r = 0;
                    long g = 0;
                    long b = 0;
                    int n = 0;
                    for (int sy = sy0; sy < Math.min(sy1, t.hauteur()); sy++) {
                        for (int sx = sx0; sx < Math.min(sx1, t.largeur()); sx++) {
                            int c = t.pixels()[sy * t.largeur() + sx];
                            int al = c >>> 24;
                            a += al;
                            r += ((c >> 16) & 0xFF) * al;
                            g += ((c >> 8) & 0xFF) * al;
                            b += (c & 0xFF) * al;
                            n++;
                        }
                    }
                    if (n > 0 && a > 0) {
                        int al = (int) (a / n);
                        p[y * cote + x] = (al > 100 ? 0xFF : al) << 24 | (int) (r / a) << 16 | (int) (g / a) << 8 | (int) (b / a);
                    }
                }
            }
            return new Texture(cote, cote, p);
        });
    }
}
