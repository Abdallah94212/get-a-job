"""La femme du metro, en pixel art : deux tenues, streetwear (le metro, la nuit) et costume (la tour Krousty).

Dessinee pixel par pixel, de face, en proportions realistes (environ sept tetes), sur le modele de la reference
envoyee : aplats francs, deux ou trois nuances par matiere, pas de contour noir, les bords simplement plus sombres.
Lumiere par la gauche : les ombres tombent a droite.
Lancement : python femme_sprite.py   (depuis designs/) -> outputs/femme_*.png
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

L, H = 64, 116
SORTIE = Path(__file__).resolve().parent / "outputs"

PEAU = ((222, 168, 132), (190, 132, 100), (238, 192, 158))          # base, ombre, lumiere
CHEVEUX = ((212, 192, 142), (168, 146, 100), (236, 222, 180), (120, 100, 70))
BONNET = ((118, 34, 44), (86, 22, 32), (146, 52, 60))
DEBARDEUR = ((232, 228, 218), (196, 190, 180), (246, 244, 238))
CAMO = ((96, 92, 64), (122, 112, 78), (66, 64, 46), (44, 44, 36))
BASKETS = ((34, 34, 40), (22, 22, 26), (150, 150, 156))
ROUGE = ((196, 36, 40), (150, 22, 30), (226, 70, 70))
ORANGE = (240, 120, 24)
NOIR_VESTE = ((34, 34, 42), (22, 22, 28), (58, 58, 70))
CHEMISE = ((236, 236, 232), (196, 198, 204), (250, 250, 248))


class Pixels:
    def __init__(self):
        self.img = Image.new("RGBA", (L, H), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def rect(self, x0, y0, x1, y1, c):
        """Rectangle plein, bornes incluses."""
        if x1 >= x0 and y1 >= y0:
            self.d.rectangle([x0, y0, x1, y1], fill=tuple(c) + (255,))

    def poly(self, pts, c):
        self.d.polygon(pts, fill=tuple(c) + (255,))

    def ellipse(self, x0, y0, x1, y1, c):
        self.d.ellipse([x0, y0, x1, y1], fill=tuple(c) + (255,))

    def px(self, x, y, c):
        if 0 <= x < L and 0 <= y < H:
            self.img.putpixel((x, y), tuple(c) + (255,))

    def ligne(self, pts, c):
        self.d.line(pts, fill=tuple(c) + (255,))

    def plein(self, x, y):
        return 0 <= x < L and 0 <= y < H and self.img.getpixel((x, y))[3] > 0


def ton(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


# ---------------------------------------------------------------- les morceaux du corps
def tete(p, bonnet):
    peau, ombre, lumiere = PEAU
    p.ellipse(25, 9, 39, 29, peau)                                     # le visage
    p.poly([(26, 23), (38, 23), (35, 29), (29, 29)], peau)             # la machoire, un peu fine
    p.rect(36, 13, 38, 26, ombre)                                      # la joue dans l'ombre
    p.poly([(38, 22), (34, 29), (36, 29), (39, 23)], ombre)
    p.rect(27, 14, 29, 17, lumiere)                                    # le front qui prend la lumiere
    p.rect(24, 18, 25, 21, ombre)                                      # les oreilles
    p.rect(39, 18, 40, 21, ombre)
    p.px(24, 22, (200, 200, 206))                                      # une petite boucle d'oreille
    # les yeux mi-clos, le regard un peu fatigue
    for x in (27, 33):
        p.rect(x, 17, x + 3, 17, (90, 62, 52))                         # la paupiere
        p.rect(x, 18, x + 3, 18, (240, 236, 230))
        p.rect(x + 1, 18, x + 2, 18, (44, 32, 30))
    p.rect(27, 15, 30, 15, CHEVEUX[3])                                  # les sourcils
    p.rect(33, 15, 36, 15, CHEVEUX[3])
    p.px(32, 21, ombre)                                                 # le nez
    p.px(32, 22, ombre)
    p.px(31, 22, ton(peau, 0.9))
    p.rect(31, 25, 33, 25, (176, 104, 96))                             # la bouche
    p.px(32, 26, ton(peau, 0.92))
    # les cheveux blonds, courts et en desordre, qui depassent
    base, sombre, clair, _ = CHEVEUX
    if bonnet:
        p.poly([(23, 13), (25, 21), (26, 14)], base)
        p.poly([(41, 13), (39, 22), (38, 14)], sombre)
        for x, y0, y1 in ((22, 12, 19), (23, 18, 23), (41, 12, 20), (40, 19, 24), (24, 22, 24)):
            p.rect(x, y0, x, y1, base if x < 32 else sombre)
        for x in range(26, 39, 2):                                     # la frange sous le bonnet
            p.rect(x, 12, x, 13 + (x // 2) % 2, clair if x < 32 else base)
        # le bonnet bordeaux, roule sur le front
        b, bo, bl = BONNET
        p.ellipse(23, 2, 41, 16, b)
        p.rect(23, 9, 41, 13, b)
        p.rect(23, 10, 41, 13, bo)
        p.rect(24, 10, 40, 10, bl)
        p.rect(36, 3, 40, 9, bo)
        for x in range(25, 40, 3):                                     # les cotes de la maille
            p.rect(x, 4, x, 9, ton(b, 0.9))
    else:
        p.ellipse(23, 6, 41, 19, base)                                 # un carre flou, jusqu'a la machoire
        p.poly([(23, 12), (26, 12), (26, 28), (24, 29), (22, 24)], base)
        p.poly([(38, 12), (41, 12), (42, 24), (40, 29), (38, 28)], sombre)
        p.rect(36, 7, 40, 14, sombre)
        p.poly([(25, 10), (34, 9), (30, 16), (26, 16)], clair)          # la frange, sur le cote
        p.poly([(30, 10), (37, 10), (37, 15), (33, 13)], base)
        for x, y in ((23, 25), (24, 27), (41, 26), (40, 28), (26, 6), (31, 5), (36, 6)):
            p.px(x, y, clair if x < 32 else base)


def cou(p):
    peau, ombre, _ = PEAU
    p.rect(30, 28, 34, 32, peau)
    p.rect(30, 28, 34, 29, ombre)                                      # l'ombre sous le menton
    p.poly([(24, 33), (30, 31), (34, 31), (40, 33), (40, 35), (24, 35)], peau)   # les epaules
    p.rect(37, 33, 40, 35, ombre)


def bras(p, manche=None, longueur_manche=0):
    """Les bras le long du corps, fins. manche : la couleur d'une manche qui couvre le bras."""
    peau, ombre, lumiere = PEAU
    for x0 in (21, 40):
        p.poly([(x0 + 1, 33), (x0 + 3, 33), (x0 + 3, 48), (x0 + 3, 62), (x0, 62), (x0, 48)], peau)
        p.rect(x0 + 3, 35, x0 + 3, 61, ombre)
        p.rect(x0 + 1, 35, x0 + 1, 46, lumiere)
        p.rect(x0 + 1, 47, x0 + 2, 48, ombre)                          # le coude
        p.poly([(x0, 62), (x0 + 3, 62), (x0 + 3, 67), (x0 + 1, 68), (x0, 66)], peau)   # la main
        p.rect(x0 + 3, 63, x0 + 3, 66, ombre)
        if manche:
            m, mo, ml = manche
            p.poly([(x0 - 1, 32), (x0 + 4, 32), (x0 + 4, 33 + longueur_manche), (x0 - 1, 33 + longueur_manche)], m)
            p.rect(x0 + 3, 34, x0 + 4, 33 + longueur_manche, mo)
            p.rect(x0 - 1, 34, x0 - 1, 33 + longueur_manche, ml)
            p.rect(x0 - 1, 33 + longueur_manche, x0 + 4, 33 + longueur_manche, mo)


def camouflage(p, zone, graine=11):
    """Des taches de camouflage dans la zone deja peinte du pantalon."""
    rng = random.Random(graine)
    x0, y0, x1, y1 = zone
    for _ in range(140):
        cx, cy = rng.randrange(x0, x1), rng.randrange(y0, y1)
        c = rng.choice(CAMO[1:])
        for dx in range(rng.randrange(1, 4)):
            for dy in range(rng.randrange(1, 3)):
                if p.img.getpixel((min(L - 1, cx + dx), min(H - 1, cy + dy)))[:3] in CAMO:
                    p.px(cx + dx, cy + dy, c)


def jambes(p, c, large):
    """Deux jambes separees sous l'entrejambe ; large : le cargo qui s'evase et tombe sur la chaussure."""
    e = 2 if large else 0
    p.poly([(25, 58), (39, 58), (40, 70), (32, 70), (32, 72), (25, 70)], c)                   # le bassin
    p.poly([(25, 66), (31, 66), (31, 103), (23 - e, 103), (24 - e // 2, 80)], c)            # la jambe gauche
    p.poly([(33, 66), (40, 66), (40 + e // 2, 80), (41 + e, 103), (33, 103)], c)            # la jambe droite


def bas_cargo(p):
    """Un pantalon cargo camouflage, large, qui tombe sur les baskets, poches sur les cuisses."""
    c = CAMO[0]
    jambes(p, c, True)
    camouflage(p, (20, 58, 45, 104))
    p.rect(25, 57, 39, 59, ton(c, 0.75))                               # la ceinture
    p.rect(30, 58, 33, 59, (150, 150, 156))
    for x0, droite in ((22, False), (37, True)):                       # les poches cargo, sur les cuisses
        p.rect(x0, 78, x0 + 5, 87, ton(c, 0.85))
        p.rect(x0, 78, x0 + 5, 79, ton(c, 0.62))
        p.rect(x0 + 5 if droite else x0, 79, x0 + 5 if droite else x0, 87, ton(c, 0.6))
    p.rect(39, 66, 42, 102, ton(c, 0.72))                              # l'ombre a droite
    p.rect(31, 70, 31, 102, ton(c, 0.6))
    for y in (95, 98, 101):                                            # le tissu qui plisse sur les chevilles
        p.ligne([(22, y), (30, y + 1)], ton(c, 0.7))
        p.ligne([(34, y + 1), (42, y)], ton(c, 0.6))
    p.ligne([(37, 59), (38, 66), (36, 70)], ORANGE)                    # un cordon orange qui sort de la poche


def bas_tailleur(p):
    """Un pantalon de costume noir, droit, avec son pli."""
    n, no, nl = NOIR_VESTE
    jambes(p, n, False)
    p.rect(38, 66, 40, 102, no)
    p.rect(31, 70, 31, 102, no)
    p.rect(27, 70, 27, 102, nl)
    p.rect(36, 70, 36, 102, nl)


def baskets(p):
    b, bo, semelle = BASKETS
    for x0 in (21, 33):
        p.poly([(x0, 102), (x0 + 9, 102), (x0 + 10, 106), (x0 + 10, 109), (x0 - 1, 109), (x0 - 1, 104)], b)
        p.rect(x0 + 1, 103, x0 + 5, 104, (70, 70, 80))
        p.rect(x0 - 1, 109, x0 + 10, 111, semelle)
        p.rect(x0 + 7, 104, x0 + 9, 108, bo)


def mocassins(p):
    for x0 in (24, 33):
        p.poly([(x0, 103), (x0 + 7, 103), (x0 + 9, 107), (x0 + 9, 109), (x0 - 1, 109)], (22, 22, 26))
        p.rect(x0 + 1, 104, x0 + 5, 104, (74, 74, 82))
        p.rect(x0 - 1, 109, x0 + 9, 110, (58, 42, 36))


def foulard_cou(p):
    """Le foulard rouge, noue en bandana autour du cou, la pointe sur la poitrine."""
    r, ro, rl = ROUGE
    p.rect(29, 30, 35, 32, r)
    p.rect(29, 30, 35, 30, rl)
    p.poly([(28, 32), (35, 32), (31, 39)], r)
    p.poly([(33, 32), (35, 32), (32, 38), (31, 37)], ro)
    p.rect(35, 30, 36, 33, ro)                                         # le noeud, sur le cote
    p.px(37, 34, r)


def debardeur(p):
    t, to, tl = DEBARDEUR
    p.poly([(25, 34), (39, 34), (40, 44), (38, 56), (26, 56), (24, 44)], t)
    p.rect(26, 31, 27, 34, t)                                          # les bretelles
    p.rect(37, 31, 38, 34, to)
    p.rect(37, 37, 39, 55, to)                                         # l'ombre a droite
    p.rect(26, 37, 26, 48, tl)
    p.ligne([(28, 44), (30, 46)], to)
    p.ligne([(33, 49), (35, 51)], to)
    p.poly([(26, 56), (38, 56), (39, 58), (25, 58)], PEAU[0])         # un peu de ventre
    p.rect(36, 56, 39, 57, PEAU[1])
    p.ligne([(29, 33), (32, 36), (35, 33)], (150, 150, 158))           # la chaine
    p.rect(31, 36, 32, 37, (90, 92, 100))


def cigarette(p):
    p.rect(43, 64, 46, 64, (236, 236, 232))
    p.px(47, 64, (220, 90, 50))
    p.px(48, 62, (140, 140, 150))
    p.px(49, 60, (120, 120, 130))


def tenue_costume(p):
    """Le costume de la Krousty : veste noire, chemise blanche, cravate fine, badge orange, foulard en pochette."""
    n, no, nl = NOIR_VESTE
    c, co, cl = CHEMISE
    p.poly([(23, 32), (41, 32), (42, 36), (41, 66), (23, 66), (22, 36)], n)   # la veste, epaules marquees
    p.rect(37, 34, 41, 65, no)
    p.poly([(29, 31), (35, 31), (34, 44), (32, 50), (30, 44)], c)             # la chemise, dans le V
    p.rect(33, 33, 34, 43, co)
    p.poly([(28, 30), (31, 31), (30, 35)], cl)                                # le col
    p.poly([(36, 30), (33, 31), (34, 35)], co)
    p.rect(31, 32, 32, 48, (22, 22, 28))                                      # la cravate fine
    p.poly([(26, 32), (29, 31), (32, 50), (30, 52), (27, 40)], nl)            # le revers gauche, dans la lumiere
    p.poly([(38, 32), (35, 31), (32, 50), (34, 52), (37, 40)], no)            # le revers droit
    for y in (55, 60):                                                        # les boutons
        p.px(32, y, (84, 84, 96))
    p.rect(25, 38, 27, 39, ROUGE[0])                                          # le foulard rouge, en pochette
    p.px(26, 37, ROUGE[2])
    p.rect(24, 40, 28, 40, nl)                                                # la poche de poitrine
    p.ligne([(29, 33), (28, 49)], ORANGE)                                     # le badge Krousty, au cordon orange
    p.rect(26, 49, 29, 53, (236, 236, 232))
    p.rect(26, 49, 29, 49, ORANGE)
    p.rect(27, 51, 28, 51, (120, 120, 130))
    p.ligne([(23, 66), (41, 66)], no)


def assombrir_bords(p):
    """Pas de contour noir : la derniere rangee de chaque bord est juste un ton plus sombre."""
    src = p.img.copy()
    for y in range(H):
        for x in range(L):
            if src.getpixel((x, y))[3] == 0:
                continue
            voisins = [src.getpixel((x + dx, y + dy))[3] if 0 <= x + dx < L and 0 <= y + dy < H else 0
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
            if 0 in voisins:
                r, g, b, _ = src.getpixel((x, y))
                p.img.putpixel((x, y), (int(r * 0.72), int(g * 0.72), int(b * 0.76), 255))


def femme(tenue):
    p = Pixels()
    if tenue == "streetwear":
        bras(p)
        bas_cargo(p)
        baskets(p)
        debardeur(p)
        cou(p)
        foulard_cou(p)
        tete(p, bonnet=True)
        cigarette(p)
    else:
        bras(p, NOIR_VESTE, 30)
        bas_tailleur(p)
        mocassins(p)
        cou(p)
        tenue_costume(p)
        tete(p, bonnet=False)
    assombrir_bords(p)
    return p.img


def planche():
    """Les deux tenues cote a cote, agrandies six fois, sur un fond neutre, avec leur taille reelle a cote."""
    z = 6
    out = Image.new("RGB", (L * z * 2 + 200, H * z + 60), (226, 226, 222))
    d = ImageDraw.Draw(out)
    for k, tenue in enumerate(("streetwear", "costume")):
        img = femme(tenue)
        img.save(SORTIE / f"femme_{tenue}.png")
        grand = img.resize((L * z, H * z), Image.NEAREST)
        out.paste(grand, (20 + k * (L * z + 40), 30), grand)
        out.paste(img, (L * z * 2 + 120, 40 + k * 140), img)
        d.text((20 + k * (L * z + 40), 8), tenue.upper(), fill=(60, 60, 70))
    d.text((L * z * 2 + 110, 16), "taille reelle", fill=(60, 60, 70))
    out.save(SORTIE / "femme_planche.png")
    return out


if __name__ == "__main__":
    planche()
    print(SORTIE / "femme_planche.png")
