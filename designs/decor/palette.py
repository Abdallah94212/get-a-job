"""La palette du jeu : Endesga 32, par ENDESGA (lospec.com/palette-list/endesga-32), libre d'usage.

Ses rampes sont deja teintees : les ombres glissent vers le bleu et le violet, les lumieres vers le jaune. Chaque
texture du jeu est ramenee a ces 32 couleurs, couleur par couleur, au plus proche dans l'espace OKLab (un espace ou
la distance suit l'oeil). Avant, on rend un peu de saturation et de contraste que la nuit du decor avait eteints.
Les halos de lumiere ne sont pas touches : ils s'ajoutent en jeu et doivent rester doux.
"""
import numpy as np
from PIL import Image, ImageEnhance, ImageFilter

ENDESGA_32 = ("be4a2f d77643 ead4aa e4a672 b86f50 733e39 3e2731 a22633 e43b44 f77622 feae34 fee761 63c74d 3e8948 "
              "265c42 193c3e 124e89 0099db 2ce8f5 ffffff c0cbdc 8b9bb4 5a6988 3a4466 262b44 181425 ff0044 68386c "
              "b55088 f6757a e8b796 c28569").split()
COULEURS = np.array([[int(h[i:i + 2], 16) for i in (0, 2, 4)] for h in ENDESGA_32], float)

# ce qui ne passe pas a la palette : les lumieres additives, les degrades doux, et la police (blanche, recoloree en jeu)
EPARGNES = ("halo_", "flaque", "police")
ACTIVE = True   # le decor est enregistre en Endesga pure (l'etape 1) ; le jeu l'eteint ensuite, etape par etape


def _oklab(rgb):
    c = rgb / 255.0
    c = np.where(c <= 0.04045, c / 12.92, ((c + 0.055) / 1.055) ** 2.4)
    l = 0.4122214708 * c[..., 0] + 0.5363325363 * c[..., 1] + 0.0514459929 * c[..., 2]
    m = 0.2119034982 * c[..., 0] + 0.6806995451 * c[..., 1] + 0.1073969566 * c[..., 2]
    s = 0.0883024619 * c[..., 0] + 0.2817188376 * c[..., 1] + 0.6299787005 * c[..., 2]
    l, m, s = np.cbrt(l), np.cbrt(m), np.cbrt(s)
    return np.stack([0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                     1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                     0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s], -1)


_LAB = _oklab(COULEURS)
_POIDS = np.array([1.6, 1.0, 1.0])            # la clarte compte plus que la teinte : on garde la lisibilite
_POIDS_LUMIERE = np.array([0.6, 1.6, 1.6])    # ce qui brille garde sa teinte : la lueur verte de la Cell reste verte
_memo = {}


def _plus_proche(rgb, poids=_POIDS):
    """La couleur de la palette la plus proche de chaque couleur, calculee une fois par couleur distincte."""
    cles = rgb[:, 0] * 65536 + rgb[:, 1] * 256 + rgb[:, 2]
    uniques, inverse = np.unique(cles, return_inverse=True)
    cle = poids.tobytes()
    memo = _memo.setdefault(cle, {})
    a_faire = [u for u in uniques if u not in memo]
    if a_faire:
        u = np.array(a_faire)
        lab = _oklab(np.stack([u // 65536, u // 256 % 256, u % 256], -1).astype(float))
        idx = (((lab[:, None, :] - _LAB[None]) * poids) ** 2).sum(-1).argmin(1)
        for k, i in zip(a_faire, idx):
            memo[k] = COULEURS[i]
    return np.array([memo[u] for u in uniques])[inverse]


def appliquer(img, nom=""):
    """La texture aux couleurs de la palette ; la transparence (et l'alpha 254 des pixels qui brillent) est gardee."""
    if not ACTIVE or nom.startswith(EPARGNES):
        return img
    img = img.convert("RGBA")
    alpha = img.getchannel("A")
    rgb = img.convert("RGB")
    rgb = ImageEnhance.Color(rgb).enhance(1.6)
    rgb = ImageEnhance.Contrast(rgb).enhance(1.15)
    rgb = ImageEnhance.Brightness(rgb).enhance(1.12)
    # le grain des surfaces est lisse avant la palette : la ou un pixel s'ecarte a peine de ses voisins, il prend leur
    # valeur mediane ; les contours, les lettres et ce qui brille s'ecartent beaucoup plus et restent nets
    a = np.asarray(rgb).astype(np.int64)
    med = np.asarray(rgb.filter(ImageFilter.MedianFilter(3))).astype(np.int64)
    lisse = (np.abs(a - med).max(-1) <= 14) & (np.asarray(alpha) != 254)
    a[lisse] = med[lisse]
    a = a.reshape(-1, 3)
    brille = np.asarray(alpha).reshape(-1) == 254
    res = _plus_proche(a)
    if brille.any():
        res[brille] = _plus_proche(a[brille], _POIDS_LUMIERE)
    out = Image.fromarray(res.reshape(img.height, img.width, 3).astype(np.uint8)).convert("RGBA")
    out.putalpha(alpha)
    return out


# --- La rue qui s'eteint -------------------------------------------------------------------------------------------
# Au debut du jeu le quartier est en Endesga pure. Plus le heros monte, plus la rue perd ses couleurs : le jeu ne
# redessine rien, il remplace les 32 couleurs par celles de l'etape (decor/Palette.java, ecrit par java() ci-dessous).

def _rgb(lab):
    L, a, b = lab[..., 0], lab[..., 1], lab[..., 2]
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    c = np.stack([4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
                  -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
                  -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s], -1)
    c = np.clip(c, 0, 1)
    return np.round(np.where(c <= 0.0031308, 12.92 * c, 1.055 * c ** (1 / 2.4) - 0.055) * 255)


def ternir(chroma, clarte, nuit=0.0):
    """Les 32 couleurs, moins saturees, un peu plus sombres ; ce qui reste de couleur glisse vers le gris-bleu de la nuit."""
    lab = _oklab(COULEURS)
    lab[..., 1:] = lab[..., 1:] * chroma + np.array([-0.004, -0.018]) * nuit
    lab[..., 0] *= clarte
    return _rgb(lab)


TERNE = ternir(0.55, 0.93)      # les surfaces, poussiereuses
DOUCE = ternir(0.82, 0.98)      # ce qui brille : enseignes, fenetres, vitrines
# chaque etape : (les surfaces, ce qui brille)
ETAPES = [
    (COULEURS, COULEURS),                              # 1. le quartier vivant (missions 0.1 et 1.1)
    (TERNE, DOUCE),                                    # 2. il livre pour Geto (1.2 et 1.3)
    (ternir(0.22, 0.9, 0.6), DOUCE),                   # 3. il a quitte Geto (de la fin de 1.4 a 2.2)
    (ternir(0.0, 0.88, 1.0), ternir(0.6, 0.95)),       # 4. il tient la rue (2.3 et 2.4)
]
GRIS = [19, 20, 21, 22, 23, 24, 25]                    # la rampe gris-bleu


def vers_gris(img):
    """Une image ramenee a la seule rampe gris-bleu de la palette ternie (Old Geto, une fois devenu l'ennemi)."""
    a = np.asarray(img.convert("RGBA")).copy()
    px = a[..., :3].reshape(-1, 3).astype(float)
    cible = _oklab(TERNE[GRIS])
    idx = (((_oklab(px)[:, None, :] - cible[None]) * np.array([1.0, 0.2, 0.2])) ** 2).sum(-1).argmin(1)
    a[..., :3] = TERNE[GRIS][idx].reshape(a.shape[0], a.shape[1], 3)
    return Image.fromarray(a)


def java(fichier):
    """Ecrit decor/Palette.java : les 32 couleurs d'origine et, pour chaque etape, celles qui les remplacent."""
    def ligne(pal):
        return "{" + ", ".join("0x%02X%02X%02X" % tuple(int(v) for v in c) for c in pal) + "}"
    lignes = ["package decor;", "",
              "/** Les couleurs du decor, etape par etape. Fichier genere par designs/decor/palette.py : ne pas modifier a la main. */",
              "final class Palette {",
              "    /** Endesga 32 : les couleurs dans lesquelles le decor est enregistre. */",
              "    static final int[] ORIGINE = " + ligne(COULEURS) + ";",
              "    /** Pour chaque etape, la couleur qui remplace chacune des 32 sur les surfaces. */",
              "    static final int[][] SURFACES = {"] + ["        " + ligne(s) + "," for s, _ in ETAPES] + ["    };",
              "    /** Pareil pour les pixels qui brillent (alpha 254) : ils gardent plus de couleur. */",
              "    static final int[][] LUMIERES = {"] + ["        " + ligne(l) + "," for _, l in ETAPES] + ["    };", "",
              "    private Palette() { }", "}", ""]
    fichier.write_text("\n".join(lignes))
