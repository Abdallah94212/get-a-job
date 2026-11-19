"""Les objets de l'inventaire : une icone de 16 x 16 par objet (obj_<id>.png), et pour ce qu'on porte a la main
pendant une livraison, un petit dessin tenu (tenu_<id>.png) que le jeu pose dans la main du heros.

Meme trait que les objets de l'histoire (personnages.py : le sac, les caisses, le colis) : des aplats, une lumiere
venue du haut, une ombre d'un pixel. Lancer avec python3.14 depuis designs/decor :
    python3.14 inventaire.py
"""
from pathlib import Path

from outils import *

RACINE = Path(__file__).resolve().parents[2]
INTERFACE = RACINE / "jeu" / "ressources" / "interface"

PAPIER, PAPIER_O = (226, 220, 206), (176, 168, 150)
KRAFT, KRAFT_O, KRAFT_C = (168, 126, 82), (128, 92, 58), (196, 156, 108)
METAL, METAL_O, METAL_C = (120, 124, 134), (84, 88, 98), (170, 174, 182)
OMBRE = (14, 14, 20, 140)


def toile():
    return Toile(16, 16)


def ombre(t, x0, x1, y):
    t.rect(x0, y, x1, y + 1, OMBRE)


def sac_lili():
    """Le sac blanc du Maitre Poulet : papier plie en haut, le logo orange."""
    t = toile()
    ombre(t, 3, 14, 15)
    t.rect(3, 4, 13, 15, PAPIER)
    t.rect(11, 4, 13, 15, PAPIER_O)
    t.rect(3, 2, 13, 4, PAPIER_O)
    t.rect(5, 1, 11, 2, PAPIER_O)
    t.rect(5, 7, 10, 11, ORANGE)
    t.rect(6, 8, 9, 10, (255, 196, 120))
    return t.img


def sac_omar():
    """Le gros sac du garage : kraft, double, les anses."""
    t = toile()
    ombre(t, 1, 15, 15)
    t.rect(1, 5, 15, 15, KRAFT)
    t.rect(12, 5, 15, 15, KRAFT_O)
    t.rect(1, 5, 15, 6, KRAFT_C)
    t.ligne([(4, 5), (5, 1), (7, 1), (8, 5)], KRAFT_O)
    t.ligne([(8, 5), (9, 1), (11, 1), (12, 5)], KRAFT_O)
    t.rect(4, 9, 10, 12, ORANGE)
    return t.img


def enveloppe():
    t = toile()
    ombre(t, 1, 15, 13)
    t.rect(1, 4, 15, 13, PAPIER)
    t.ligne([(1, 4), (8, 9), (15, 4)], PAPIER_O)
    t.rect(1, 12, 15, 13, PAPIER_O)
    t.rect(7, 8, 9, 10, (190, 60, 50))
    return t.img


def caisses_poulet():
    t = toile()
    ombre(t, 0, 16, 15)
    for x, y in ((0, 8), (8, 8), (4, 1)):
        t.rect(x, y, x + 8, y + 7, (170, 160, 142))
        t.rect(x, y, x + 8, y + 1, (200, 192, 174))
        t.rect(x + 2, y + 2, x + 5, y + 4, (180, 90, 80))
    return t.img


def sachets():
    """Trois sachets zippes de Cell : la poudre verte qui brille."""
    t = toile()
    ombre(t, 1, 15, 15)
    for i, (x, y) in enumerate(((1, 6), (6, 3), (10, 7))):
        t.rect(x, y, x + 5, y + 8, (210, 216, 214))
        t.rect(x, y, x + 5, y + 1, (150, 156, 160))
        t.rect(x + 1, y + 3, x + 4, y + 7, lum(VERT) if i == 1 else (90, 200, 120))
    return t.img


def sandwich():
    t = toile()
    ombre(t, 1, 15, 13)
    t.poly([(1, 12), (8, 4), (15, 12)], (214, 170, 110))
    t.rect(2, 9, 14, 10, (110, 170, 80))
    t.rect(3, 10, 13, 11, (190, 80, 70))
    t.rect(1, 11, 15, 13, (200, 150, 92))
    return t.img


def piccolo():
    t = toile()
    ombre(t, 3, 13, 15)
    t.ligne([(8, 15), (8, 3)], (70, 110, 60), 2)
    for k in range(3):
        t.ellipse(2, 1 + k * 4, 14, 8 + k * 4, (120, 200, 100) if k % 2 else (96, 176, 84))
    return t.img


def cell():
    t = toile()
    ombre(t, 2, 14, 15)
    t.rect(3, 2, 13, 15, (60, 62, 70))
    t.rect(3, 2, 13, 4, (90, 92, 100))
    t.rect(5, 6, 11, 12, lum(VERT))
    t.rect(6, 7, 8, 9, (220, 255, 230))
    return t.img


def beeper():
    t = toile()
    ombre(t, 3, 13, 15)
    t.rect(3, 2, 13, 15, (60, 120, 70))
    t.rect(4, 3, 12, 8, (30, 40, 34))
    t.rect(5, 5, 11, 6, lum(VERT))
    t.rect(5, 10, 7, 12, (40, 80, 48))
    t.rect(9, 10, 11, 12, (40, 80, 48))
    return t.img


def boitier():
    t = toile()
    ombre(t, 2, 14, 15)
    t.rect(2, 3, 14, 15, (232, 100, 8))
    t.rect(2, 3, 14, 5, (255, 150, 60))
    t.rect(4, 7, 9, 11, (30, 30, 36))
    t.rect(10, 8, 12, 10, (240, 240, 230))
    return t.img


def eclat():
    t = toile()
    t.poly([(8, 1), (13, 7), (9, 15), (3, 9)], (170, 140, 230))
    t.poly([(8, 1), (13, 7), (8, 8)], (140, 230, 220))
    t.poly([(3, 9), (8, 8), (9, 15)], (230, 150, 210))
    t.point(9, 4, (255, 255, 255))
    return t.img


def casque():
    t = toile()
    ombre(t, 2, 14, 14)
    t.ellipse(2, 2, 15, 15, (40, 42, 52))
    t.rect(2, 9, 15, 14, (40, 42, 52))
    t.rect(8, 6, 14, 10, (120, 150, 200))
    t.rect(3, 4, 6, 5, (90, 94, 110))
    return t.img


def baskets():
    t = toile()
    ombre(t, 1, 15, 15)
    t.poly([(1, 14), (1, 8), (6, 6), (9, 10), (15, 11), (15, 14)], (236, 236, 230))
    t.rect(1, 13, 15, 15, (200, 60, 50))
    t.ligne([(4, 8), (7, 10)], (120, 120, 130))
    return t.img


def gilet():
    t = toile()
    ombre(t, 2, 14, 15)
    t.poly([(4, 1), (6, 1), (8, 4), (10, 1), (12, 1), (14, 4), (14, 15), (2, 15), (2, 4)], (60, 70, 60))
    t.rect(7, 4, 9, 15, (40, 46, 40))
    t.rect(3, 8, 13, 10, (90, 104, 88))
    return t.img


def lacrymo():
    t = toile()
    ombre(t, 5, 11, 15)
    t.rect(5, 4, 11, 15, (200, 60, 50))
    t.rect(6, 1, 10, 4, (40, 40, 46))
    t.rect(5, 7, 11, 9, (240, 240, 230))
    return t.img


def bouteille():
    t = toile()
    t.poly([(3, 14), (2, 12), (9, 5), (11, 3), (13, 5), (11, 7), (4, 15)], (60, 120, 80))
    t.ligne([(10, 4), (12, 6)], (150, 210, 170))
    return t.img


def planche():
    t = toile()
    t.poly([(1, 13), (12, 2), (15, 5), (4, 16)], (150, 112, 70))
    t.ligne([(3, 12), (12, 3)], (180, 140, 92))
    t.point(9, 6, (60, 60, 66))
    return t.img


def tuyau():
    t = toile()
    t.ligne([(2, 14), (13, 3)], METAL_O, 4)
    t.ligne([(2, 13), (12, 3)], METAL_C, 1)
    t.rect(11, 1, 15, 5, METAL)
    return t.img


def barre():
    t = toile()
    t.ligne([(1, 15), (14, 2)], (70, 72, 80), 3)
    t.ligne([(1, 14), (13, 2)], (130, 134, 146), 1)
    t.ligne([(13, 2), (15, 4)], (70, 72, 80), 3)
    return t.img


def canette():
    t = toile()
    ombre(t, 5, 11, 15)
    t.rect(5, 3, 11, 15, (196, 40, 40))
    t.rect(5, 3, 11, 4, (200, 200, 206))
    t.rect(5, 14, 11, 15, (160, 160, 168))
    t.rect(7, 6, 9, 12, (240, 240, 236))
    return t.img


def pierre():
    t = toile()
    ombre(t, 2, 14, 14)
    t.poly([(3, 12), (2, 8), (5, 4), (11, 3), (14, 7), (13, 12)], (120, 112, 106))
    t.poly([(5, 4), (11, 3), (10, 6), (6, 7)], (150, 142, 136))
    t.point(9, 9, (90, 84, 80))
    return t.img


def billets():
    """L'argent des clients, plie en deux, un elastique autour : ce n'est pas le tien."""
    t = toile()
    ombre(t, 2, 14, 14)
    for k, c in enumerate(((88, 140, 96), (104, 156, 110), (120, 172, 124))):
        t.rect(2 + k, 9 - k * 2, 14 + k - 1, 13 - k * 2, c)
    t.rect(7, 5, 9, 13, (200, 60, 50))
    t.point(4, 10, (230, 240, 230))
    return t.img


# ---------------------------------------------------------------- ce qu'on tient a la main pendant une livraison
def tenu_sac(c, o, logo=True, l=8, h=9):
    t = Toile(l, h + 3)
    t.ligne([(2, 3), (3, 0), (l - 4, 0), (l - 3, 3)], o)
    t.rect(0, 3, l, h + 3, c)
    t.rect(l - 2, 3, l, h + 3, o)
    if logo:
        t.rect(2, 6, l - 3, 9, ORANGE)
    return t.img


def tenu_enveloppe():
    t = Toile(7, 5)
    t.rect(0, 0, 7, 5, PAPIER)
    t.ligne([(0, 0), (3, 3), (6, 0)], PAPIER_O)
    return t.img


def tenu_sachet():
    t = Toile(4, 6)
    t.rect(0, 0, 4, 6, (210, 216, 214))
    t.rect(1, 2, 3, 5, (90, 200, 120))
    return t.img


def tous():
    out = {
        "obj_sac_lili": sac_lili(), "obj_sac_omar": sac_omar(), "obj_enveloppe": enveloppe(),
        "obj_caisses_poulet": caisses_poulet(), "obj_sachets": sachets(), "obj_sandwich": sandwich(),
        "obj_piccolo": piccolo(), "obj_cell": cell(), "obj_beeper": beeper(), "obj_boitier_orange": boitier(),
        "obj_eclat": eclat(), "obj_casque": casque(), "obj_baskets": baskets(), "obj_gilet": gilet(),
        "obj_lacrymo": lacrymo(), "obj_bouteille": bouteille(), "obj_planche": planche(), "obj_tuyau": tuyau(),
        "obj_barre": barre(), "obj_recette_geto": billets(), "obj_canette": canette(), "obj_pierre": pierre(), "tenu_recette_geto": tenu_enveloppe(),
        "tenu_sac_lili": tenu_sac(PAPIER, PAPIER_O), "tenu_sac_omar": tenu_sac(KRAFT, KRAFT_O, True, 10, 11),
        "tenu_enveloppe": tenu_enveloppe(), "tenu_sachets": tenu_sachet(), "tenu_sandwich": tenu_enveloppe(),
    }
    return out


if __name__ == "__main__":
    for nom, image in tous().items():
        image.save(INTERFACE / f"{nom}.png")
    print(len(tous()), "images ->", INTERFACE)
