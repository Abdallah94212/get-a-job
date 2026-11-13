"""Tout ce qui n'est pas une facade : sols, ciel, arbres, mobilier urbain, voitures, lieux de l'histoire, tour."""
import math
import random
from pathlib import Path

import numpy as np
from PIL import Image, ImageEnhance

from outils import *
from facades import coq, rideau, vitre, garde_corps


def _bruit(t, graine, amplitude=5):
    t.grain(amplitude, graine)
    return t.img


# ---------------------------------------------------------------- sols
def sols():
    rng = random.Random(7)
    s = {}
    t = Toile(64, 64, (42, 42, 50, 255))
    for _ in range(40):
        x, y = rng.randrange(64), rng.randrange(64)
        t.point(x, y, rng.choice(((54, 54, 62), (34, 34, 42))))
    t.ligne([(10, 0), (14, 20), (11, 40), (16, 63)], (34, 34, 42))
    t.rect(40, 30, 58, 44, (38, 38, 46))
    s["sol_route"] = _bruit(t, 1, 4)
    t = Toile(64, 64, (80, 80, 88, 255))
    for k in range(0, 64, 16):
        t.ligne([(0, k), (63, k)], (62, 62, 70))
        t.ligne([(k + (8 if (k // 16) % 2 else 0), 0), (k + (8 if (k // 16) % 2 else 0), 63)], (66, 66, 74))
    for _ in range(6):
        x, y = rng.randrange(60), rng.randrange(60)
        t.rect(x, y, x + 3, y + 2, (58, 58, 66))
    s["sol_trottoir"] = _bruit(t, 2, 5)
    t = Toile(8, 8, (118, 118, 124, 255))
    t.ligne([(0, 0), (7, 0)], (140, 140, 146))
    t.ligne([(0, 7), (7, 7)], (84, 84, 92))
    s["sol_bordure"] = t.img
    t = Toile(64, 64, (58, 54, 52, 255))
    for _ in range(90):
        t.point(rng.randrange(64), rng.randrange(64), rng.choice(((70, 66, 62), (46, 44, 44), (52, 60, 54))))
    s["sol_terreplein"] = _bruit(t, 3, 6)
    t = Toile(16, 16, (42, 42, 50, 255))
    t.rect(0, 0, 8, 16, (168, 168, 166))
    s["sol_passage"] = _bruit(t, 4, 8)
    t = Toile(2, 182)
    t.rect(0, 0, 2, 42, (160, 160, 158, 255))
    s["sol_tirets"] = t.img
    t = Toile(2, 8, (160, 160, 158, 255))
    s["sol_ligne"] = t.img
    t = Toile(64, 64, (96, 96, 104, 255))
    for k in range(0, 64, 32):
        t.ligne([(0, k), (63, k)], (74, 74, 82))
        t.ligne([(k, 0), (k, 63)], (74, 74, 82))
    t.rect(30, 0, 34, 64, lum(ORANGE_S))
    s["sol_parvis"] = _bruit(t, 5, 4)
    t = Toile(64, 64, (48, 48, 54, 255))
    t.ligne([(0, 20), (30, 26), (40, 50), (63, 54)], (36, 36, 42))
    t.ligne([(20, 0), (26, 30), (22, 63)], (36, 36, 42))
    t.rect(0, 0, 2, 64, (92, 92, 92))
    for _ in range(30):
        t.point(rng.randrange(64), rng.randrange(64), (60, 70, 62))
    s["sol_cour"] = _bruit(t, 6, 5)
    t = Toile(20, 48)
    t.poly([(10, 0), (19, 14), (13, 14), (13, 47), (7, 47), (7, 14), (0, 14)], (166, 166, 164))
    s["sol_fleche"] = t.img
    t = Toile(40, 16)
    t.texte(2, 1, "BUS", (160, 160, 158), 5, 2)
    s["sol_bus"] = t.img
    t = Toile(14, 14)
    t.ellipse(0, 0, 14, 14, (64, 64, 72), (30, 30, 36))
    t.ligne([(3, 7), (10, 7)], (40, 40, 48))
    t.ligne([(7, 3), (7, 10)], (40, 40, 48))
    s["sol_egout"] = t.img
    t = Toile(12, 9)
    t.poly([(0, 2), (9, 0), (11, 6), (2, 8)], (176, 174, 164))
    t.ligne([(3, 3), (8, 2)], (110, 110, 110))
    t.ligne([(3, 5), (9, 4)], (110, 110, 110))
    s["sol_journal"] = t.img
    t = Toile(40, 60, (16, 16, 22, 255))
    for k in range(6):
        t.rect(0, 60 - (k + 1) * 9, 40, 60 - k * 9, ton((92, 92, 100), 1 - k * 0.16))
        t.ligne([(0, 60 - k * 9 - 1), (39, 60 - k * 9 - 1)], ton((130, 130, 136), 1 - k * 0.16))
    s["sol_escalier"] = t.img
    return s


def halo(couleur, taille=32):
    t = Toile(taille, taille)
    px, c = t.img.load(), (taille - 1) / 2
    for y in range(taille):
        for x in range(taille):
            k = max(0.0, 1 - math.hypot(x - c, y - c) / c) ** 2
            if k > 0.02:
                px[x, y] = (int(couleur[0] * k), int(couleur[1] * k), int(couleur[2] * k), 255)
    return t.img


def ciel():
    rng = random.Random(11)
    t = Toile(512, 128)
    haut = (8, 8, 16)
    for y in range(128):
        k = min(1.0, y / 104) ** 1.6
        t.rect(0, y, 512, y + 1, tuple(int(haut[i] + (NUIT[i] - haut[i]) * k) for i in range(3)) + (255,))
    for _ in range(150):
        x, y = rng.randrange(512), rng.randrange(84)
        v = rng.choice((70, 100, 140, 200))
        t.point(x, y, (v, v, min(255, v + 16)))
    t.ellipse(372, 16, 392, 36, (216, 216, 200))
    t.ellipse(377, 20, 382, 25, (186, 186, 172))
    t.ellipse(384, 27, 388, 31, (190, 190, 176))
    t.ellipse(379, 29, 382, 32, (196, 196, 182))
    x = 0
    while x < 512:
        w, h = rng.randrange(10, 34), rng.randrange(5, 22)
        t.rect(x, 108 - h, x + w, 128, (24, 24, 34))
        for _ in range(w * h // 60):
            t.point(x + rng.randrange(w), 108 - h + 1 + rng.randrange(h), (96, 92, 80))
        x += w + rng.randrange(0, 6)
    t.rect(0, 112, 512, 128, NUIT + (255,))
    return t.img


# ---------------------------------------------------------------- vegetation et mobilier
def pin():
    """Pin parasol du terre-plein : tronc penche, houppier large et plat."""
    rng = random.Random(21)
    t = Toile(120, 150)
    t.ligne([(62, 149), (60, 110), (55, 76), (58, 50)], (60, 54, 52), 6)
    t.ligne([(57, 80), (34, 46)], (60, 54, 52), 3)
    t.ligne([(58, 70), (86, 42)], (60, 54, 52), 3)
    t.ligne([(64, 149), (62, 110), (58, 78)], (82, 76, 72), 1)
    for c, n, dy in (((34, 44, 42), 26, 8), ((46, 58, 54), 22, 2), ((62, 76, 70), 12, -3)):
        for _ in range(n):
            x, y = rng.randrange(4, 96), rng.randrange(10, 46) + dy
            y += int(abs(x - 52) * 0.22)
            t.ellipse(x, y, x + rng.randrange(18, 30), y + rng.randrange(9, 15), c)
    return t.img


ARBRES = Path(__file__).resolve().parent / "packs" / "arbres_karsiori"
# les arbres de l'avenue alternent : (arbre du pack, sa couleur, hauteur, en miroir)
VARIANTES_ARBRES = [(9, "GREEN", 126, False), (4, "GREEN", 130, False), (9, "YELLOWISH GREEN", 116, True), (4, "TEAL", 122, True),
                    (9, "GREEN", 130, True), (4, "YELLOWISH GREEN", 118, False), (9, "YELLOWISH GREEN", 122, False),
                    (4, "SANDY GREEN", 128, True)]


def arbres():
    """Les arbres de l'avenue : ceux du pack de karsiori (CC0), le tronc allonge a la taille d'un arbre d'alignement,
    assombris pour la nuit. Le feuillage reste tel que dessine : seul le tronc nu, sous lui, est repete."""
    out = {}
    for k, (numero, couleur, hauteur, miroir) in enumerate(VARIANTES_ARBRES):
        prefixe = "TREE 8_" if numero == 9 else "TREE 4_"          # dans le pack, les fichiers de l'arbre 9 s'appellent "TREE 8"
        nom = next(f for f in sorted(ARBRES.glob(prefixe + "*.png"))
                   if f.name.replace(" ", "").upper().endswith("_" + couleur.replace(" ", "") + ".PNG"))
        im = Image.open(nom).convert("RGBA")
        im = im.crop(im.getbbox())
        a = np.asarray(im)
        largeurs = (a[..., 3] > 0).sum(1)
        i = int(largeurs.argmax())
        while i < im.height - 1 and largeurs[i] >= 0.3 * largeurs.max():
            i += 1
        rangee = min(i + 2, im.height - 8)
        a = np.concatenate([a[:rangee], np.repeat(a[rangee:rangee + 1], max(0, hauteur - im.height), 0), a[rangee:]], 0)
        arbre = Image.fromarray(a)
        if miroir:
            arbre = arbre.transpose(Image.FLIP_LEFT_RIGHT)
        pied = np.nonzero(np.asarray(arbre)[-1, :, 3])[0]
        centre = int(pied.mean()) if len(pied) else arbre.width // 2
        rgb = ImageEnhance.Brightness(ImageEnhance.Color(arbre.convert("RGB")).enhance(0.8)).enhance(0.62).convert("RGBA")
        rgb.putalpha(arbre.getchannel("A"))
        toile = Image.new("RGBA", (90, 130))
        toile.alpha_composite(rgb, (45 - centre, 130 - arbre.height))
        out[f"platane_{k}"] = toile
    return out


def platane():
    rng = random.Random(22)
    t = Toile(90, 130)
    t.ligne([(45, 129), (45, 70)], (98, 96, 92), 5)
    for bx, by in ((20, 34), (38, 16), (58, 20), (72, 40), (30, 52), (62, 56)):
        t.ligne([(45, 74), (bx, by)], (78, 76, 74), 2)
    for y in range(74, 128, 9):
        t.rect(44 + (y % 2), y, 47, y + 4, (74, 72, 70))
    for c, n in (((48, 50, 48), 26), ((66, 64, 56), 20), ((88, 82, 66), 10)):
        for _ in range(n):
            x, y = rng.randrange(4, 72), rng.randrange(4, 62)
            t.ellipse(x, y, x + rng.randrange(8, 16), y + rng.randrange(6, 11), c)
    return t.img


def lampadaire():
    """Mat gris a long bras courbe ; la lanterne eclaire vers le bas."""
    t = Toile(44, 132)
    t.rect(4, 14, 7, 132, (74, 76, 84))
    t.rect(3, 118, 8, 132, (58, 60, 68))
    t.ligne([(5, 14), (10, 6), (22, 3), (38, 3)], (74, 76, 84), 2)
    t.rect(30, 4, 42, 7, (56, 58, 66))
    t.rect(31, 7, 41, 8, lum((250, 240, 208)))
    return t.img


def petits():
    """Potelet, feu, panneaux, banc, poubelle, plaque de rue."""
    p = {}
    t = Toile(5, 13)
    t.rect(1, 1, 4, 13, (46, 48, 58))
    t.rect(1, 0, 4, 2, (120, 120, 130))
    t.rect(1, 4, 4, 5, (130, 130, 138))
    p["potelet"] = t.img
    t = Toile(12, 62)
    t.rect(5, 16, 7, 62, (52, 54, 62))
    t.rect(2, 0, 10, 18, (28, 28, 34))
    t.ellipse(4, 2, 8, 6, (58, 50, 50))
    t.ellipse(4, 7, 8, 11, (58, 54, 48))
    t.ellipse(4, 12, 8, 16, lum((206, 226, 214)))
    t.rect(3, 28, 9, 36, (28, 28, 34))
    t.rect(5, 30, 7, 34, lum((200, 210, 206)))
    p["feu"] = t.img
    t = Toile(13, 44)
    t.rect(6, 12, 7, 44, (72, 74, 82))
    t.ellipse(0, 0, 13, 13, (150, 150, 158), (60, 60, 68))
    t.rect(3, 5, 10, 8, BLANC)
    p["sens_interdit"] = t.img
    t = Toile(46, 72)
    t.rect(22, 0, 24, 72, (72, 74, 82))
    for k, (nom, clair) in enumerate((("PARIS", True), ("VILLEJUIF", True), ("HOPITAL", False), ("A6", False))):
        y, fond = 2 + k * 11, (206, 206, 200) if clair else (52, 60, 86)
        t.poly([(2, y), (40, y), (45, y + 4), (40, y + 9), (2, y + 9)], fond)
        t.texte(4, y + 2, nom, (30, 30, 38) if clair else BLANC, 3)
    p["direction"] = t.img
    t = Toile(13, 44)
    t.rect(6, 13, 7, 44, (72, 74, 82))
    t.rect(0, 0, 13, 13, (52, 60, 86))
    t.texte(4, 3, "P", BLANC, 5)
    p["parking"] = t.img
    t = Toile(32, 18)
    for y in (1, 5, 9):
        t.rect(1, y, 31, y + 3, (84, 80, 76))
    t.rect(3, 0, 5, 18, (40, 40, 48))
    t.rect(27, 0, 29, 18, (40, 40, 48))
    t.rect(0, 11, 32, 13, (70, 66, 64))
    p["banc"] = t.img
    t = Toile(9, 18)
    t.rect(4, 0, 5, 18, (60, 62, 70))
    t.ellipse(0, 3, 9, 7, (70, 72, 80))
    t.poly([(1, 5), (8, 5), (7, 16), (2, 16)], (104, 110, 108))
    t.ligne([(3, 8), (5, 13)], (140, 144, 142))
    p["poubelle"] = t.img
    t = Toile(60, 17)
    t.rect(0, 0, 60, 17, (40, 46, 70), )
    t.rect(1, 1, 59, 16, (200, 200, 206))
    t.rect(2, 2, 58, 15, (40, 46, 70))
    t.texte_centre(0, 3, 60, "AVENUE DE", BLANC, 3)
    t.texte_centre(0, 9, 60, "FONTAINEBLEAU", BLANC, 3)
    p["plaque_rue"] = t.img
    t = Toile(32, 32)
    for k in range(-32, 64, 6):
        t.ligne([(k, 0), (k + 32, 32)], (104, 108, 118))
        t.ligne([(k + 32, 0), (k, 32)], (104, 108, 118))
    p["grillage"] = t.img
    t = Toile(4, 4, (86, 88, 96, 255))
    p["metal"] = t.img
    return p


def mobilier():
    """Ce qui manquait aux trottoirs : velo, boite aux lettres, barriere, buisson. Dessines comme le reste du decor."""
    p = {}
    t = Toile(34, 22)
    for cx in (7, 26):
        t.ellipse(cx - 7, 8, cx + 7, 22, None, (36, 36, 44))
        t.point(cx, 15, (96, 98, 108))
    t.ligne([(7, 15), (13, 5), (23, 5), (26, 15)], (104, 108, 118))
    t.ligne([(13, 5), (17, 15), (23, 5)], (104, 108, 118))
    t.ligne([(7, 15), (17, 15)], (84, 88, 98))
    t.ligne([(11, 2), (15, 2)], (30, 30, 36))
    t.ligne([(13, 2), (13, 5)], (84, 88, 98))
    t.ligne([(23, 5), (24, 1), (27, 1)], (60, 62, 72))
    p["velo"] = t.img
    t = Toile(12, 24)
    t.rect(5, 14, 7, 24, (52, 54, 62))
    t.rect(0, 2, 12, 16, (126, 118, 78))
    t.rect(0, 0, 12, 3, (98, 92, 62))
    t.rect(0, 13, 12, 16, (98, 92, 62))
    t.rect(2, 6, 10, 7, (36, 34, 30))
    t.rect(3, 9, 9, 12, (150, 142, 100))
    p["boite_lettres"] = t.img
    t = Toile(44, 19)
    t.rect(1, 1, 43, 2, (112, 116, 126))
    t.rect(1, 14, 43, 15, (112, 116, 126))
    for x in range(1, 44, 4):
        t.rect(x, 1, x + 1, 15, (96, 100, 110))
    for x in (1, 42):
        t.rect(x, 0, x + 1, 19, (112, 116, 126))
        t.rect(x - 1, 18, x + 2, 19, (70, 72, 82))
    p["barriere"] = t.img
    t = Toile(32, 17)
    for x0, y0, x1, y1, c in ((0, 5, 16, 17, (34, 44, 42)), (9, 0, 25, 17, (40, 52, 48)), (18, 4, 32, 17, (34, 44, 42)),
                              (4, 8, 13, 15, (46, 60, 54)), (13, 3, 21, 10, (50, 64, 58)), (21, 8, 28, 14, (44, 58, 52))):
        t.ellipse(x0, y0, x1, y1, c)
    t.rect(0, 15, 32, 17, (30, 38, 38))
    p["buisson"] = t.img
    return p


def accessoires():
    """Les petites choses que le pack n'a pas : de quoi travailler au labo, et les objets qu'on ramasse dans l'histoire."""
    a = {}
    t = Toile(22, 5)                                                    # un clavier
    t.rect(0, 1, 22, 5, (60, 62, 70))
    t.rect(1, 0, 21, 4, (150, 154, 160))
    for x in range(2, 20, 2):
        t.point(x, 1, (96, 100, 108))
        t.point(x + 1, 2, (96, 100, 108))
    a["clavier"] = t.img
    t = Toile(6, 4)                                                     # une souris
    t.ellipse(0, 0, 6, 4, (150, 154, 160))
    t.point(2, 0, (60, 62, 70))
    a["souris"] = t.img
    t = Toile(18, 12)                                                   # une pile de dossiers
    for k, c in enumerate(((150, 160, 180), (196, 190, 170), (170, 120, 110), (196, 190, 170))):
        t.rect(k % 2, 9 - k * 3, 16 + k % 2, 12 - k * 3, c)
        t.rect(k % 2, 11 - k * 3, 16 + k % 2, 12 - k * 3, ton(c, 0.7))
    a["dossiers"] = t.img
    t = Toile(46, 34)                                                   # des cartons sur une palette
    t.rect(0, 30, 46, 34, (96, 84, 70))
    for x in (2, 20, 38):
        t.rect(x, 32, x + 4, 34, (60, 52, 46))
    for x, y, w, h in ((1, 14, 22, 16), (23, 16, 21, 14), (8, 0, 24, 14)):
        t.rect(x, y, x + w, y + h, (124, 108, 88))
        t.rect(x, y, x + w, y + 2, (140, 124, 102))
        t.ligne([(x + w // 2, y), (x + w // 2, y + h - 1)], (90, 78, 64))
        t.rect(x + 3, y + h - 7, x + 10, y + h - 3, BLANC)
    a["palette"] = t.img
    t = Toile(40, 40)                                                   # un chariot de labo, des fioles dessus
    t.rect(0, 12, 40, 15, (170, 174, 182))
    t.rect(0, 30, 40, 33, (130, 134, 142))
    for x in (1, 37):
        t.rect(x, 12, x + 2, 38, (96, 100, 108))
        t.ellipse(x - 1, 36, x + 3, 40, (30, 30, 36))
    for k, x in enumerate((5, 13, 21, 29)):
        h = 8 + k % 2 * 3
        t.rect(x + 1, 12 - h, x + 4, 12 - h + 3, (150, 170, 176))
        t.ellipse(x - 1, 12 - h + 2, x + 6, 12, (120, 140, 150))
        t.rect(x, 9, x + 5, 12, lum(VERT) if k % 2 else ton(VERT, 0.55))
    t.rect(6, 24, 20, 30, (124, 108, 88))
    t.rect(24, 22, 34, 30, (190, 190, 184))
    a["chariot"] = t.img
    t = Toile(10, 6)                                                    # le Flipper Zero orange
    t.rect(0, 0, 10, 6, ORANGE)
    t.rect(2, 1, 7, 4, (40, 32, 30))
    a["flipper"] = t.img
    t = Toile(16, 18)                                                   # un bidon de produit
    t.rect(1, 3, 15, 18, (60, 76, 110))
    t.rect(5, 0, 11, 3, (40, 50, 76))
    t.rect(3, 8, 13, 13, BLANC)
    t.poly([(6, 12), (10, 12), (8, 9)], (176, 150, 60))
    a["bidon"] = t.img
    return a


def pub(genre):
    """Panneau publicitaire eclaire, sur pied : c'est la que la Krousty parle au quartier."""
    t = Toile(24, 52)
    t.rect(10, 34, 14, 52, (60, 62, 70))
    t.rect(0, 0, 24, 36, (40, 42, 50))
    fond = {"job": (28, 24, 26), "poulet": (236, 232, 220), "krousty": ORANGE_S}[genre]
    t.rect(2, 2, 22, 34, lum(fond))
    if genre == "job":
        t.texte_centre(2, 5, 20, "GET", lum(ORANGE), 5)
        t.texte_centre(2, 13, 20, "A", lum(ORANGE), 5)
        t.texte_centre(2, 21, 20, "JOB", lum(ORANGE), 5)
        t.rect(5, 30, 19, 31, lum((150, 146, 140)))
    elif genre == "poulet":
        coq(t, 3, 5, lum((232, 100, 8)), 2)
        t.texte_centre(2, 22, 20, "MENU", (40, 36, 36), 3)
        t.texte_centre(2, 28, 20, "NUIT", (40, 36, 36), 3)
    else:
        t.texte(6, 6, "K", (30, 24, 24), 5, 2)
        t.texte_centre(2, 24, 20, "CORP", (30, 24, 24), 3)
    return t.img


def abri():
    a = {}
    rng = random.Random(31)
    t = Toile(60, 38)
    for y in range(38):
        for x in range(60):
            if (x + y) % 3 == 0 and rng.random() < 0.7:
                t.point(x, y, (84, 92, 110))
    t.rect(0, 0, 60, 2, (60, 62, 70, 255))
    for x in (0, 29, 58):
        t.rect(x, 0, x + 2, 38, (60, 62, 70, 255))
    t.rect(4, 26, 56, 29, (70, 66, 64, 255))
    a["abri_vitre"] = t.img
    a["abri_toit"] = Toile(4, 4, (46, 48, 56, 255)).img
    return a


def metro():
    m = {}
    t = Toile(30, 80)
    t.rect(14, 22, 16, 80, (60, 62, 70))
    t.ellipse(4, 0, 26, 22, lum((236, 228, 190)), (60, 62, 70))
    t.texte(10, 4, "M", (30, 30, 38), 5, 2)
    t.rect(0, 26, 30, 40, (40, 46, 70))
    t.texte_centre(0, 28, 30, "KREMLIN", BLANC, 3)
    t.texte_centre(0, 34, 30, "BICETRE", BLANC, 3)
    m["metro_totem"] = t.img
    t = Toile(60, 20)
    garde_corps(t, 0, 2, 60, 18)
    t.rect(0, 0, 60, 3, (52, 70, 60, 255))
    m["metro_garde"] = t.img
    return m


# ---------------------------------------------------------------- vehicules
def voiture():
    """Une voiture en six petites textures, assemblees en deux boites dans le niveau."""
    v = {}
    corps, sombre = (118, 120, 128), (60, 62, 70)
    t = Toile(120, 26)
    t.poly([(2, 12), (10, 4), (112, 4), (118, 10), (118, 19), (2, 19)], corps)
    t.rect(2, 16, 118, 19, sombre)
    for cx in (24, 94):
        t.ellipse(cx - 11, 5, cx + 11, 27, (0, 0, 0, 0))
        t.ellipse(cx - 9, 8, cx + 9, 26, (22, 22, 28))
        t.ellipse(cx - 4, 13, cx + 4, 21, (96, 96, 104))
    t.ligne([(46, 5), (46, 18)], sombre)
    t.ligne([(74, 5), (74, 18)], sombre)
    t.rect(4, 8, 8, 11, (190, 190, 184))
    v["auto_flanc"] = t.img
    t = Toile(68, 16)
    t.poly([(10, 0), (56, 0), (67, 15), (0, 15)], corps)
    t.poly([(12, 2), (31, 2), (31, 14), (4, 14)], VITRE)
    t.poly([(34, 2), (54, 2), (63, 14), (34, 14)], VITRE)
    t.ligne([(13, 3), (8, 12)], VITRE_R)
    v["auto_cabine"] = t.img
    for nom, feux in (("auto_arriere", (128, 84, 84)), ("auto_avant", (206, 204, 190))):
        t = Toile(52, 26)
        t.poly([(3, 4), (49, 4), (51, 9), (51, 19), (1, 19), (1, 9)], corps)
        t.rect(1, 16, 51, 20, sombre)
        t.rect(3, 6, 12, 10, feux)
        t.rect(40, 6, 49, 10, feux)
        t.rect(19, 11, 33, 15, (196, 196, 190))
        t.rect(21, 12, 31, 14, (60, 60, 68))
        if nom == "auto_avant":
            t.rect(15, 5, 37, 9, (40, 42, 50))
        for x in (3, 42):
            t.rect(x, 19, x + 7, 26, (22, 22, 28))
        v[nom] = t.img
    t = Toile(46, 16)
    t.poly([(5, 0), (41, 0), (45, 15), (0, 15)], corps)
    t.poly([(7, 2), (39, 2), (42, 13), (3, 13)], VITRE)
    t.ligne([(9, 4), (20, 4)], VITRE_R)
    v["auto_lunette"] = t.img
    v["auto_toit"] = Toile(4, 4, corps + (255,)).img
    return v


def divers():
    d = {}
    # scooter de livraison vu de dos, avec le coffre du Maitre Poulet
    t = Toile(18, 34)
    t.rect(7, 22, 11, 34, (22, 22, 28))
    t.rect(4, 14, 14, 24, (60, 62, 70))
    t.rect(2, 2, 16, 14, (206, 204, 196))
    coq(t, 4, 5, (232, 100, 8))
    t.rect(1, 13, 4, 15, (128, 84, 84))
    t.rect(14, 13, 17, 15, (128, 84, 84))
    d["scooter"] = t.img
    # table de bistrot et deux chaises
    t = Toile(44, 22)
    t.ellipse(14, 4, 30, 9, (150, 150, 156))
    t.rect(21, 8, 23, 22, (60, 62, 70))
    for x in (2, 32):
        t.rect(x, 0, x + 10, 2, (96, 90, 84))
        t.rect(x, 2, x + 2, 22, (70, 66, 62))
        t.rect(x + 8, 2, x + 10, 22, (70, 66, 62))
        t.rect(x, 11, x + 10, 13, (96, 90, 84))
    d["terrasse"] = t.img
    # cagettes de l'epicerie
    rng = random.Random(41)
    t = Toile(40, 20)
    for x, y in ((0, 10), (14, 10), (28, 10), (7, 0), (21, 0)):
        t.rect(x, y, x + 12, y + 10, (96, 86, 74))
        t.ligne([(x, y + 5), (x + 11, y + 5)], (64, 58, 52))
        for k in range(4):
            t.ellipse(x + 1 + k * 3, y - 2, x + 4 + k * 3, y + 2, rng.choice(((110, 126, 104), (150, 130, 96), (128, 120, 110))))
    d["cagettes"] = t.img
    # le SDF du bas de l'allee, assis contre le mur
    t = Toile(26, 26)
    t.poly([(4, 25), (5, 12), (10, 7), (17, 8), (21, 14), (24, 25)], (72, 70, 74))
    t.poly([(6, 25), (8, 16), (18, 15), (22, 25)], (92, 86, 80))
    t.ellipse(9, 1, 17, 9, (150, 140, 132))
    t.rect(8, 0, 18, 4, (44, 46, 54))
    t.rect(1, 21, 5, 25, (190, 190, 184))
    d["sdf"] = t.img
    # sacs-poubelle et cartons
    t = Toile(30, 16)
    t.ellipse(0, 4, 14, 16, (30, 30, 36))
    t.ellipse(10, 2, 24, 16, (36, 36, 44))
    t.ligne([(13, 5), (17, 8)], (90, 90, 100))
    t.rect(20, 7, 30, 16, (124, 108, 88))
    t.ligne([(20, 11), (29, 11)], (90, 78, 64))
    d["dechets"] = t.img
    # vieille camionnette du parking, vue de face
    t = Toile(60, 58)
    t.poly([(6, 6), (54, 6), (58, 26), (58, 48), (2, 48), (2, 26)], (150, 150, 152))
    t.rect(2, 30, 58, 48, (96, 100, 104))
    t.poly([(9, 9), (29, 9), (29, 24), (6, 24)], VITRE)
    t.poly([(31, 9), (51, 9), (54, 24), (31, 24)], VITRE)
    t.ellipse(6, 32, 16, 42, (206, 204, 190))
    t.ellipse(44, 32, 54, 42, (206, 204, 190))
    t.rect(22, 36, 38, 41, (40, 42, 50))
    t.rect(0, 46, 60, 50, (60, 62, 70))
    t.rect(5, 50, 14, 58, (22, 22, 28))
    t.rect(46, 50, 55, 58, (22, 22, 28))
    d["camionnette"] = t.img
    # gabarit du joueur, en attendant le vrai heros
    for nom, dos in (("joueur_dos", True), ("joueur_face", False)):
        t = Toile(22, 48)
        t.poly([(7, 12), (15, 12), (18, 20), (19, 44), (3, 44), (4, 20)], (60, 60, 74))
        t.rect(6, 43, 10, 48, (24, 24, 30))
        t.rect(12, 43, 16, 48, (24, 24, 30))
        t.ellipse(6, 1, 16, 12, (30, 30, 38) if dos else (150, 140, 132))
        t.poly([(6, 5), (8, 0), (10, 3), (12, 0), (14, 3), (16, 5)], (24, 24, 30))
        t.ligne([(4, 20), (3, 43)], (86, 86, 100))
        t.ligne([(7, 12), (4, 20)], (86, 86, 100))
        if dos:
            t.ligne([(11, 14), (11, 43)], (28, 28, 36))
        else:
            t.rect(8, 6, 10, 7, (24, 24, 30))
            t.rect(12, 6, 14, 7, (24, 24, 30))
            t.rect(10, 13, 12, 22, (86, 86, 96))
        d[nom] = t.img
    return d


# ---------------------------------------------------------------- lieux de l'histoire
def pignon(largeur, hauteur, mur, graine):
    """Mur aveugle sur le cote d'un immeuble, celui qu'on voit de face en remontant l'avenue."""
    rng = random.Random(graine)
    t = Toile(largeur, hauteur, mur + (255,))
    for y in range(0, hauteur, 42):
        t.ligne([(0, y), (largeur - 1, y)], ton(mur, 0.85))
    for _ in range(largeur // 6):
        x, y = rng.randrange(largeur), rng.randrange(hauteur - 30)
        t.ligne([(x, y), (x, y + rng.randrange(8, 30))], ton(mur, 0.88))
    t.rect(0, 0, largeur, 3, ton(mur, 1.2))
    t.rect(0, hauteur - 8, largeur, hauteur, ton(mur, 0.7))
    return t, rng


def pignon_sisyphe(largeur, hauteur, acte):
    """Le mur de la laverie : Sisyphe pousse sa pierre vers le haut. La Krousty le repeint a l'acte 5."""
    t, rng = pignon(largeur, hauteur, MUR_BLANC, 51)
    x0, y0 = (largeur - 110) // 2, hauteur - 108
    noir = (24, 24, 30)
    if acte >= 5:
        t.rect(x0 - 8, y0 - 6, x0 + 118, y0 + 92, (176, 86, 20))
        noir = (150, 72, 16)
    t.ligne([(x0, y0 + 74), (x0 + 110, y0 + 30)], noir, 2)                      # la pente
    t.ellipse(x0 + 52, y0 + 2, x0 + 98, y0 + 48, noir)                           # la pierre
    if acte < 5:
        t.ellipse(x0 + 60, y0 + 8, x0 + 78, y0 + 22, (58, 58, 66))
        t.ellipse(x0 + 24, y0 + 30, x0 + 34, y0 + 40, noir)                      # la tete
        t.poly([(x0 + 28, y0 + 38), (x0 + 50, y0 + 26), (x0 + 54, y0 + 34), (x0 + 34, y0 + 52)], noir)  # le dos plie
        t.ligne([(x0 + 44, y0 + 30), (x0 + 58, y0 + 22)], noir, 3)               # les bras
        t.ligne([(x0 + 32, y0 + 50), (x0 + 20, y0 + 66)], noir, 4)               # les jambes
        t.ligne([(x0 + 30, y0 + 50), (x0 + 6, y0 + 58)], noir, 4)
        t.ligne([(x0 + 6, y0 + 58), (x0 + 2, y0 + 72)], noir, 3)
        for cx in (x0 + 60, x0 + 84, x0 + 26):
            t.ligne([(cx, y0 + 48), (cx, y0 + 48 + rng.randrange(6, 16))], noir)  # coulures
        t.texte(x0 + 8, y0 + 80, "SISY", noir, 5, 2)
        t.texte(x0 + 58, y0 + 80, "PHE", ton(MUR_BLANC, 0.9), 5, 2)
    t.grain(5, 52)
    return t.img


def pignon_affiche(largeur, hauteur):
    """Le grand panneau de recrutement de la Krousty : GET A JOB."""
    t, _ = pignon(largeur, hauteur, PIERRE, 53)
    x0, y0, w, h = (largeur - 124) // 2, 60, 124, 78
    t.rect(x0 - 3, y0 - 3, x0 + w + 3, y0 + h + 3, (40, 42, 50))
    t.rect(x0, y0, x0 + w, y0 + h, lum((30, 26, 28)))
    t.texte_centre(x0, y0 + 10, w, "GET A JOB", lum(ORANGE), 5, 2)
    t.rect(x0 + 14, y0 + 32, x0 + w - 14, y0 + 34, lum(ORANGE_S))
    t.texte_centre(x0, y0 + 42, w, "KROUSTY CORP", lum((214, 208, 192)), 5)
    t.texte_centre(x0, y0 + 56, w, "ON EMBAUCHE", lum((150, 146, 140)), 3)
    t.texte_centre(x0, y0 + 66, w, "MONTEZ NOUS VOIR", lum((150, 146, 140)), 3)
    for lx in (x0 + 20, x0 + w - 24):
        t.rect(lx, y0 - 9, lx + 4, y0 - 5, lum((250, 240, 208)))
    t.grain(5, 54)
    return t.img


def garage_pignon():
    """Le garage d'Omar, porte ouverte : sous les neons, la RS6 dort sous sa bache."""
    rng = random.Random(61)
    t = Toile(250, 120, (104, 104, 110, 255))
    t.rect(0, 0, 250, 30, (80, 80, 88))
    t.rect(0, 0, 250, 3, ZINC)
    t.rect(14, 6, 236, 26, (196, 194, 186))
    t.texte_centre(14, 9, 222, "GARAGE OMAR", (36, 36, 44), 5, 2)
    t.texte_centre(0, 32, 250, "MECANIQUE - CARROSSERIE - PNEUS", (60, 60, 68), 3)
    x0, y0, w, h = 40, 42, 150, 76
    t.rect(x0 - 2, y0 - 2, x0 + w + 2, y0 + h, FER)
    froid = (150, 160, 170)
    for i in range(h):
        t.rect(x0, y0 + i, x0 + w, y0 + i + 1, lum(ton(froid, 0.9 - 0.45 * i / h)))
    t.rect(x0, y0, x0 + w, y0 + 8, RIDEAU)                                        # le rideau releve
    for xx in (x0 + 20, x0 + 70, x0 + 112):
        t.rect(xx, y0 + 12, xx + 24, y0 + 14, lum((240, 246, 250)))               # neons
    t.rect(x0 + 4, y0 + 20, x0 + 34, y0 + 44, lum(ton(froid, 0.4)))               # panneau d'outils
    for k in range(8):
        t.ligne([(x0 + 7 + k * 3, y0 + 23), (x0 + 7 + k * 3, y0 + 23 + rng.randrange(6, 16))], lum(ton(froid, 0.8)))
    # la RS6 sous sa bache
    t.poly([(x0 + 44, y0 + h - 2), (x0 + 48, y0 + 50), (x0 + 66, y0 + 36), (x0 + 110, y0 + 36), (x0 + 128, y0 + 50),
            (x0 + 134, y0 + h - 2)], lum((176, 176, 172)))
    for xx in (x0 + 60, x0 + 82, x0 + 104, x0 + 122):
        t.ligne([(xx, y0 + 40), (xx + rng.randrange(-4, 5), y0 + h - 4)], lum((128, 128, 126)))
    t.ligne([(x0 + 48, y0 + 52), (x0 + 128, y0 + 52)], lum((140, 140, 138)))
    for k in range(3):                                                             # pile de pneus
        t.ellipse(x0 + 4, y0 + h - 12 - k * 8, x0 + 26, y0 + h - 2 - k * 8, lum((26, 26, 32)))
    t.rect(x0 + 138, y0 + h - 22, x0 + 148, y0 + h - 2, lum((70, 74, 82)))         # bidon
    t.rect(204, 70, 228, 118, FER)
    t.rect(206, 72, 226, 117, (60, 62, 72))
    tags(t, 196, 40, 50, 26, rng, 2)
    affichettes(t, 4, 60, 30, 40, rng, 3)
    t.grain(5, 62)
    return t.img


def garage_avenue(largeur):
    rng = random.Random(63)
    t = Toile(largeur, 120, (104, 104, 110, 255))
    t.rect(0, 0, largeur, 30, (80, 80, 88))
    t.rect(0, 0, largeur, 3, ZINC)
    t.texte_centre(0, 9, largeur, "PNEUS  FREINS  VIDANGE", (150, 148, 142), 5, 2)
    for x in range(20, largeur - 40, 60):
        t.rect(x, 40, x + 34, 62, VITRE)
        for xb in range(x, x + 34, 4):
            t.ligne([(xb, 40), (xb, 61)], FER)
    rideau(t, largeur // 2 - 40, 66, 80, 52, rng, 3)
    for k in range(4):
        t.ellipse(12, 108 - k * 8, 34, 118 - k * 8, (26, 26, 32))
    tags(t, 6, 66, largeur - 12, 40, rng, 4)
    t.grain(5, 64)
    return t.img


def labo_mur():
    """Le labo de Gero : un ancien atelier de couture. Sous le rideau, une lueur verte."""
    rng = random.Random(71)
    t = Toile(170, 110, BRIQUE + (255,))
    for y in range(2, 110, 4):
        t.ligne([(0, y), (169, y)], BRIQUE_J)
    t.rect(0, 0, 170, 4, ZINC)
    t.rect(20, 10, 150, 26, (120, 114, 108))
    t.texte_centre(20, 15, 130, "ATELIER DE COUTURE", (84, 78, 76), 5)
    rideau(t, 34, 40, 78, 66, rng, 3)
    vert = lum(VERT)
    t.rect(34, 104, 112, 106, vert)
    for yy in range(48, 104, 9):
        t.rect(36 + rng.randrange(60), yy, 40 + rng.randrange(60), yy + 1, lum(ton(VERT, 0.6)))
    t.rect(128, 52, 150, 108, FER)
    t.rect(130, 54, 148, 107, (56, 56, 64))
    t.rect(132, 58, 146, 66, lum(ton(VERT, 0.35)))
    t.rect(6, 30, 22, 44, (70, 72, 80))                                            # extracteur
    t.ellipse(8, 32, 20, 44, (40, 42, 50))
    t.ligne([(14, 44), (14, 100)], FER, 2)
    t.grain(5, 72)
    return t.img


def tour():
    """La tour Krousty Corp, en trois volumes empiles. On la voit du bas de l'avenue."""
    rng = random.Random(81)
    r = {}

    def volume(largeur, hauteur, allume):
        t = Toile(largeur, hauteur, (34, 36, 46, 255))
        for y in range(0, hauteur, 25):
            t.rect(0, y, largeur, y + 3, (50, 52, 64))
            for x in range(0, largeur, 12):
                if rng.random() < allume:
                    t.rect(x + 3, y + 6, x + 11, y + 21, lum(ton(BUREAU, rng.choice((1, 0.8, 0.6)))))
        for x in range(0, largeur, 12):
            t.rect(x, 0, x + 2, hauteur, (62, 64, 76))
        return t

    t = volume(225, 175, 0.12)
    # le hall : portes vitrees, et l'ecran de l'accueil
    t.rect(60, 120, 165, 175, FER)
    for i in range(52):
        t.rect(63, 122 + i, 162, 123 + i, lum(ton(BUREAU, 0.95 - 0.3 * i / 52)))
    for x in range(63, 162, 20):
        t.rect(x, 122, x + 2, 175, FER)
    t.rect(92, 128, 134, 146, lum((30, 26, 28)))
    t.texte_centre(92, 131, 42, "BIENVENUE", lum(ORANGE), 3)
    t.texte_centre(92, 138, 42, "SUR TITAN", lum(ORANGE), 3)
    t.rect(96, 156, 130, 166, lum(ton(BUREAU, 0.45)))
    t.rect(50, 108, 175, 118, (26, 26, 34))
    t.texte_centre(50, 110, 125, "KROUSTY CORP", lum(ORANGE), 5)
    r["tour_base"] = t.img
    r["tour_milieu"] = volume(160, 175, 0.1).img
    t = volume(100, 125, 0.06)
    t.rect(0, 0, 100, 62, (30, 28, 32))
    t.ellipse(28, 6, 72, 50, lum(ORANGE))
    t.texte(41, 14, "K", (30, 24, 24), 5, 4)
    t.rect(0, 60, 100, 64, lum(ORANGE))
    t.rect(0, 0, 100, 2, lum(ORANGE_S))
    r["tour_sommet"] = t.img
    r["tour_flanc"] = volume(60, 175, 0.06).img
    t = Toile(6, 60)
    t.rect(2, 6, 4, 60, (70, 72, 82))
    r["tour_antenne"] = t.img
    r["tour_feu"] = halo(ORANGE, 16)
    return r


def etiquette(texte):
    """Le nom d'un lieu, affiche en bas de l'ecran quand on y entre."""
    w = largeur_texte(texte) + 12
    t = Toile(w, 15)
    t.rect(0, 0, w, 15, (96, 96, 108))
    t.rect(1, 1, w - 1, 14, ENCRE)
    t.texte(6, 4, texte, BLANC)
    return t.img
