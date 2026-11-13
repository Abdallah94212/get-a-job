"""Meubles et plantes du pack pH64 (Stacy Kendra Love, CC-BY 4.0), reteintes aux couleurs de chaque lieu.

Le decor du jeu reste celui dessine dans facades.py, objets.py et interieurs.py. Ce module ne fait que le meubler :
il decoupe les objets des planches du pack, les met a l'echelle du lieu (x2 dedans, x1 dehors) et les ramene
a la palette presque grise du jeu. Rien n'est redessine : on change seulement la teinte et la luminosite.
"""
from pathlib import Path
from PIL import Image

from outils import ALLUME, BUREAU

PACK = Path(__file__).resolve().parents[2] / "jeu" / "ressources" / "packs" / "ph64"
CONTOUR = (50, 43, 40)                                    # le trait du pack, qui sert aussi de dalle aux ecrans eteints
TRAIT = (66, 64, 70)                                      # le meme trait, adouci : le decor d'origine n'a pas de contour noir
ABAT_JOUR = {(246, 216, 150), (252, 247, 190), (226, 178, 126)}

# Chaque ambiance : luminosite, part de couleur gardee, teinte. Reglees sur les murs et les meubles d'origine.
AMBIANCES = {
    "sombre": (0.60, 0.25, (0.95, 0.95, 1.10)),           # chez toi, chez Baba : la nuit, une tele, une lampe
    "chaud": (0.80, 0.45, (1.03, 1.00, 0.93)),            # le Maitre Poulet, l'epicerie
    "froid": (0.82, 0.22, (0.96, 0.98, 1.06)),            # bureaux, hall, garage, labo : des neons
    "rue": (0.56, 0.35, (0.95, 0.97, 1.08)),              # dehors, sous les lampadaires
}

# Nom du jeu -> (planche, numero de l'objet dans la planche, compte de gauche a droite et de haut en bas).
CATALOGUE = {
    # se coucher, s'asseoir
    "lit": ("furniture", 1), "canape": ("furniture", 37), "fauteuil": ("furniture", 26), "pouf": ("furniture", 38),
    "chaise": ("furniture", 73), "chaise_dos": ("furniture", 70), "chaise_tissu": ("furniture", 76),
    "siege_bureau": ("furniture", 59), "siege_bureau_2": ("furniture", 60), "siege_rond": ("furniture", 62),
    "tabouret": ("furniture", 50), "tabouret_haut": ("furniture", 49),
    # poser, ranger
    "bureau": ("furniture", 58), "bureau_tiroirs": ("furniture", 56), "table": ("furniture", 41),
    "grande_table": ("furniture", 84), "table_basse": ("furniture", 33),
    "bibliotheque": ("furniture", 3), "etagere": ("furniture", 11), "etagere_etroite": ("furniture", 22),
    "classeur": ("furniture", 5), "commode": ("furniture", 13),
    "buffet": ("furniture", 9), "meuble_bas": ("furniture", 48), "armoire": ("furniture", 2),
    # eclairer
    "suspension": ("electronics", 9), "suspension_dome": ("electronics", 11), "suspension_atelier": ("electronics", 5),
    "lampadaire_salon": ("electronics", 20), "lampadaire_arc": ("electronics", 35), "lampe": ("electronics", 47),
    "lampe_bureau": ("electronics", 61), "applique": ("electronics", 37),
    # brancher
    "ecran": ("electronics", 13), "ecran_large": ("electronics", 14), "ecran_poste": ("electronics", 44),
    "tour_pc": ("electronics", 43), "imprimante": ("electronics", 18), "serveur": ("electronics", 16),
    "radio": ("electronics", 40), "camera": ("electronics", 42), "telephone": ("electronics", 90),
    "vieux_telephone": ("electronics", 91), "ventilateur": ("electronics", 75), "horloge": ("electronics", 81),
    "pendule": ("electronics", 80), "enceinte": ("electronics", 59),
    # cuisiner
    "frigo": ("electronics", 95), "micro_ondes": ("electronics", 103), "cafetiere": ("electronics", 71),
    "robot": ("electronics", 86), "grille_pain": ("electronics", 106), "balance": ("electronics", 108),
    # faire pousser
    "plante": ("plants", 98), "plante_haute": ("plants", 116), "plante_fine": ("plants", 132),
    "plante_large": ("plants", 99), "plante_basse": ("plants", 100), "petite_plante": ("plants", 97),
    "jardiniere": ("plants", 133), "fleurs": ("plants", 73),
}

_planches, _boites = {}, {}


def _reperer(nom):
    """Trouve les objets d'une planche : des groupes de pixels opaques separes par du vide."""
    im = Image.open(PACK / f"{nom}.png").convert("RGBA")
    w, h = im.size
    px = im.load()
    vu = [[False] * w for _ in range(h)]
    boites = []
    for y in range(h):
        for x in range(w):
            if px[x, y][3] >= 128 and not vu[y][x]:
                pile, b = [(x, y)], [x, y, x, y]
                vu[y][x] = True
                while pile:
                    cx, cy = pile.pop()
                    b = [min(b[0], cx), min(b[1], cy), max(b[2], cx), max(b[3], cy)]
                    for nx in range(max(0, cx - 2), min(w, cx + 3)):
                        for ny in range(max(0, cy - 2), min(h, cy + 3)):
                            if px[nx, ny][3] >= 128 and not vu[ny][nx]:
                                vu[ny][nx] = True
                                pile.append((nx, ny))
                boites.append(b)
    boites = [b for b in boites if (b[2] - b[0]) * (b[3] - b[1]) >= 6]
    boites.sort(key=lambda b: (b[1] // 24, b[0]))
    _planches[nom], _boites[nom] = im, boites


def brut(nom):
    """L'objet tel qu'il est dans le pack, sans retouche."""
    planche, numero = CATALOGUE[nom]
    if planche not in _planches:
        _reperer(planche)
    b = _boites[planche][numero]
    return _planches[planche].crop((b[0], b[1], b[2] + 1, b[3] + 1))


def _dalle(img):
    """Les pixels de l'ecran : le noir du pack, sauf le cadre qui touche le vide."""
    w, h = img.size
    px = img.load()
    noir = lambda x, y: 0 <= x < w and 0 <= y < h and px[x, y][3] >= 128 and px[x, y][:3] == CONTOUR
    coeur = {(x, y) for y in range(h) for x in range(w)
             if all(noir(x + i, y + j) for i in (-1, 0, 1) for j in (-1, 0, 1))}
    return {(x + i, y + j) for x, y in coeur for i in (-1, 0, 1) for j in (-1, 0, 1)
            if noir(x + i, y + j) and all(0 <= x + i + a < w and 0 <= y + j + b < h and px[x + i + a, y + j + b][3] >= 128
                                          for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)))}


def objet(nom, ambiance, echelle=2, allume=False, ecran=None):
    """L'objet aux couleurs du lieu. allume : l'abat-jour brille. ecran : la couleur d'un ecran en marche."""
    src = brut(nom)
    k, part, teinte = AMBIANCES[ambiance]
    dalle = _dalle(src) if ecran else set()
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    spx, opx = src.load(), out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = spx[x, y]
            if a < 128:
                continue
            if (x, y) in dalle:
                opx[x, y] = ecran + (254,)
            elif allume and (r, g, b) in ABAT_JOUR:
                opx[x, y] = tuple(int(v * (0.86 + 0.14 * (r, g, b)[1] / 247)) for v in ALLUME) + (254,)
            elif (r, g, b) == CONTOUR:
                opx[x, y] = tuple(int(TRAIT[i] * k * teinte[i]) for i in range(3)) + (255,)
            else:
                gris = 0.30 * r + 0.59 * g + 0.11 * b
                opx[x, y] = tuple(max(0, min(255, int((gris + (v - gris) * part) * k * teinte[i])))
                                  for i, v in enumerate((r, g, b))) + (255,)
    return out.resize((src.width * echelle, src.height * echelle), Image.NEAREST) if echelle != 1 else out


def groupe(pieces):
    """Assemble des objets en une seule image : (image, x, hauteur du pied au-dessus du sol). Ils se trient alors ensemble."""
    largeur = max(x + im.width for im, x, _ in pieces)
    hauteur = max(pied + im.height for im, _, pied in pieces)
    out = Image.new("RGBA", (largeur, hauteur), (0, 0, 0, 0))
    for im, x, pied in pieces:
        out.paste(im, (x, hauteur - pied - im.height), im.split()[3].point(lambda a: 255 if a >= 128 else 0))
    return out


def planche_temoin(ambiance="froid"):
    """Tous les objets retenus, avant et apres retouche, avec leur nom : pour verifier le catalogue d'un coup d'oeil."""
    from outils import Toile
    noms = sorted(CATALOGUE)
    t = Toile(8 * 110, (len(noms) + 7) // 8 * 110, (70, 70, 84, 255))
    for i, nom in enumerate(noms):
        x, y = i % 8 * 110, i // 8 * 110
        a, b = brut(nom), objet(nom, ambiance, 1, allume=True, ecran=BUREAU)
        t.img.alpha_composite(a, (x + 4, y + 12))
        t.img.alpha_composite(b, (x + 58, y + 12))
        t.texte(x + 2, y + 2, nom.replace("_", " ")[:26], (236, 236, 230), 3)
    return t.img


if __name__ == "__main__":
    import sys
    planche_temoin().save(sys.argv[1])
