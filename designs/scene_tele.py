"""Le plan central de l'introduction : le heros de dos, affale sur son canape, devant la tele, seule lumiere de la piece.

La tele est le point focal : tout ce qui est eclaire l'est par elle. On ne voit jamais ce qu'elle montre, seulement sa
lumiere. Trois etats du meme plan :
- "programme" : un ecran bleu, flou ; la piece dans le bleu ; lui, immobile.
- "eclat"     : l'ecran devient blanc, la piece est inondee ; il s'est penche en avant, les epaules montees.
- "neige"     : apres ; la neige grise, la piece eteinte ; il n'a pas bouge.
Dessine a la main, quatre fois trop grand, puis ramene au pixel doux du jeu (decor/pc98.py).
Lancement : python scene_tele.py   (depuis designs/)
"""
import random
import sys
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

sys.path.insert(0, str(Path(__file__).resolve().parent / "decor"))
import pc98  # noqa: E402

K, L, H = 4, 480, 270
SORTIE = Path(__file__).resolve().parent / "outputs"
ECRANS = {"programme": (120, 168, 236), "eclat": (236, 244, 255), "neige": (128, 132, 144)}
FORCE = {"programme": 0.62, "eclat": 1.45, "neige": 0.34}


class Toile:
    def __init__(self, fond):
        self.img = Image.new("RGB", (L * K, H * K), fond)
        self.d = ImageDraw.Draw(self.img)

    def p(self, pts):
        return [(x * K, y * K) for x, y in pts]

    def forme(self, pts, c, trait=None, e=1.2):
        self.d.polygon(self.p(pts), fill=c)
        if trait:
            q = self.p(pts)
            self.d.line(q + [q[0]], fill=trait, width=max(1, int(e * K)), joint="curve")

    def rect(self, x0, y0, x1, y1, c):
        self.d.rectangle([x0 * K, y0 * K, x1 * K, y1 * K], fill=c)

    def ovale(self, x0, y0, x1, y1, c):
        self.d.ellipse([x0 * K, y0 * K, x1 * K, y1 * K], fill=c)

    def trait(self, pts, c, e=1.0):
        self.d.line(self.p(pts), fill=c, width=max(1, int(e * K)), joint="curve")


def ton(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def mix(a, b, k):
    return tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3))


def lueur(base, couleur, force, rayon):
    """Ajoute la lumiere de l'ecran : un halo tres flou centre sur la tele, plus fort au sol devant elle."""
    halo = Image.new("RGB", base.size, (0, 0, 0))
    d = ImageDraw.Draw(halo)
    c = ton(couleur, 0.55 * force)
    d.ellipse([(300 - rayon) * K, (140 - rayon * 0.8) * K, (300 + rayon) * K, (140 + rayon * 0.8) * K], fill=c)
    d.ellipse([200 * K, 196 * K, 400 * K, 236 * K], fill=ton(couleur, 0.35 * force))      # la flaque de lumiere au sol
    halo = halo.filter(ImageFilter.GaussianBlur(40 * K))
    return ImageChops.add(base, halo)


def vignette(img, force):
    """Les coins retombent dans le noir : la piece n'existe que la ou la tele l'eclaire."""
    masque = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(masque)
    d.ellipse([60 * K, 10 * K, 500 * K, 300 * K], fill=255)
    masque = masque.filter(ImageFilter.GaussianBlur(70 * K))
    noir = Image.new("RGB", img.size, (6, 6, 10))
    return Image.composite(img, Image.blend(img, noir, force), masque)


def piece(t, etat):
    ecran = ECRANS[etat]
    mur, sol = (16, 16, 26), (20, 17, 22)
    t.rect(0, 0, 480, 196, mur)
    t.rect(0, 196, 480, 270, sol)
    t.trait([(0, 196), (480, 196)], (30, 28, 36), 1)
    for x in range(0, 480, 40):                                        # les lames du parquet, en fuite
        t.trait([(240 + (x - 240) * 0.55, 196), (240 + (x - 240) * 1.6, 270)], (26, 22, 28), 0.8)
    # la fenetre, a gauche : le store, la pluie, la lumiere orange de la rue qui passe entre les lames
    t.rect(22, 34, 108, 156, (30, 30, 42))
    t.rect(26, 38, 104, 152, (18, 22, 38))
    rng = random.Random(4)
    for y in range(40, 150, 6):
        t.rect(26, y, 104, y + 3, (40, 44, 62))
        if rng.random() < 0.5:
            x = rng.randrange(30, 90)
            t.rect(x, y + 3, x + rng.randrange(6, 16), y + 5, (96, 66, 42))
    for k in range(18):                                                # la pluie sur la vitre
        x = rng.randrange(28, 102)
        y = rng.randrange(40, 140)
        t.trait([(x, y), (x - 1, y + rng.randrange(5, 12))], (70, 80, 110), 0.6)
    t.rect(20, 156, 110, 160, (34, 32, 42))                            # l'appui de fenetre
    # la porte, a droite, dans le noir
    t.rect(436, 60, 476, 196, (12, 12, 18))
    t.trait([(436, 60), (436, 196)], (26, 26, 34), 1)
    # le meuble de la tele, les cassettes, la console, son fil
    t.rect(232, 176, 374, 206, (38, 28, 26))
    t.rect(232, 176, 374, 179, (60, 46, 40))
    t.rect(242, 184, 300, 200, (20, 16, 18))
    for k in range(5):
        t.rect(246 + k * 10, 186, 254 + k * 10, 198, (34 + k * 3, 30, 40))
    t.rect(310, 186, 360, 196, (26, 26, 32))
    t.rect(352, 190, 356, 192, (40, 160, 70))                          # le voyant de la console
    t.trait([(330, 196), (338, 212), (320, 222)], (14, 14, 18), 1.2)
    # la tele : un gros poste cathodique, l'ecran bombe
    t.rect(250, 102, 358, 178, (30, 30, 36))
    t.rect(250, 102, 358, 105, (46, 46, 54))
    t.ovale(258, 108, 342, 172, ton(ecran, 0.35))
    t.forme([(262, 112), (338, 112), (340, 140), (338, 168), (262, 168), (260, 140)], ecran)
    t.rect(346, 112, 352, 118, (20, 20, 24))
    t.rect(346, 122, 352, 128, (20, 20, 24))
    t.rect(347, 166, 350, 168, (220, 40, 40))                          # le voyant de veille
    if etat == "programme":                                            # une image qu'on ne lit pas : du flou, des lignes
        t.ovale(282, 124, 320, 158, (196, 222, 255))
        t.ovale(292, 116, 336, 148, (160, 196, 250))
    for y in range(112, 168, 3):
        t.trait([(262, y), (338, y)], ton(ecran, 0.82), 0.5)
    if etat == "neige":
        for k in range(500):
            x, y = rng.randrange(263, 338), rng.randrange(113, 167)
            v = rng.choice((70, 110, 170, 220))
            t.rect(x, y, x + 1, y + 0.6, (v, v, v + 6))
    t.trait([(262, 116), (296, 113)], (255, 255, 255), 0.8)             # le reflet sur le verre bombe
    # une plante, a cote, en ombre chinoise
    t.forme([(386, 172), (404, 172), (402, 206), (388, 206)], (24, 20, 22))
    for a, b in (((394, 172), (374, 132)), ((394, 172), (386, 120)), ((395, 172), (404, 126)), ((396, 172), (420, 140)), ((394, 172), (366, 150))):
        t.forme([a, b, (b[0] + 6, b[1] + 6)], (14, 18, 20))


def table(t, etat, temps):
    ecran = ECRANS[etat]
    t.rect(196, 200, 334, 206, (30, 24, 24))                           # la table basse
    t.rect(196, 200, 334, 201, mix((44, 36, 34), ecran, 0.35))
    t.rect(200, 206, 204, 218, (20, 16, 18))
    t.rect(326, 206, 330, 218, (20, 16, 18))
    t.forme([(244, 194), (276, 192), (278, 199), (246, 201)], mix((150, 146, 136), ecran, 0.3))   # la lettre de la regie
    t.ovale(208, 195, 226, 201, (50, 50, 58))                          # le cendrier, du cote du heros
    t.rect(316, 186, 324, 200, (110, 30, 34))                          # une canette
    t.rect(316, 186, 324, 188, (170, 170, 176))
    if etat != "neige":                                                # la fumee monte devant le mur, pas devant l'ecran
        import math
        fumee = [(217 + 3.5 * math.sin(k * 0.45) + k * 0.12, 195 - k * 3.2) for k in range(24)]
        t.trait(fumee, mix((40, 42, 58), ecran, 0.22), 0.8)            # un filet, a peine plus clair que le mur
        t.trait(fumee[:6], mix((60, 62, 80), ecran, 0.3), 0.9)


def canape(t, etat):
    """Le dossier du canape, au premier plan : on est derriere lui."""
    ecran = ECRANS[etat]
    dos = [(30, 270), (34, 230), (46, 220), (240, 217), (430, 220), (442, 230), (446, 270)]
    t.forme(dos, (20, 18, 26))
    t.trait([(46, 221), (240, 218), (430, 221)], mix((40, 36, 50), ecran, 0.6 * FORCE[etat]), 1.4)   # le bord eclaire
    t.trait([(238, 214), (238, 270)], (18, 16, 22), 1)
    for x in (120, 360):
        t.trait([(x, 222), (x + 2, 260)], (20, 18, 26), 1)


def heros(t, etat):
    """De dos, assis : la casquette, les cheveux jusqu'aux epaules, le haut rouge. La tele le dessine en contre-jour :
    un liseré clair sur sa droite, cote ecran."""
    ecran = ECRANS[etat]
    penche = etat == "eclat"
    dy = 9 if penche else 0                                            # penche en avant : la tete descend, les epaules montent
    rim = mix((60, 70, 110), ecran, 0.85)
    rouge, rouge_o = (118, 34, 36), (66, 18, 24)
    h = dy // 2
    # le dos et les epaules, larges, qui tombent ; le haut est trop grand pour lui
    t.forme([(112, 222), (118, 204 - h), (134, 194 - h), (160, 188 - h), (196, 188 - h), (222, 194 - h), (238, 204 - h), (244, 222)], rouge_o)
    t.forme([(196, 188 - h), (222, 194 - h), (238, 204 - h), (244, 222), (226, 222), (214, 200 - h)], rouge)
    t.trait([(196, 188 - h), (222, 194 - h), (238, 204 - h), (244, 221)], rim, 1.3)
    t.trait([(150, 200), (146, 220)], (48, 12, 18), 1)
    t.trait([(208, 202), (212, 220)], (48, 12, 18), 1)
    t.rect(168, 172 + dy, 188, 192 - h, (52, 36, 36))                  # la nuque, dans l'ombre
    # la casquette : une calotte ronde, de dos
    toile, toile_o = (116, 100, 78), (74, 62, 50)
    t.ovale(147, 106 + dy, 209, 178 + dy, toile_o)
    t.forme([(178, 106 + dy), (196, 110 + dy), (206, 124 + dy), (209, 142 + dy), (190, 142 + dy), (188, 118 + dy)], toile)
    t.d.arc([147 * K, (106 + dy) * K, 209 * K, (178 + dy) * K], 270, 360, fill=mix(toile, ecran, 0.65), width=int(1.3 * K))
    t.trait([(178, 107 + dy), (166, 124 + dy), (162, 142 + dy)], ton(toile_o, 0.82), 0.8)      # les coutures
    t.trait([(178, 107 + dy), (190, 124 + dy), (194, 142 + dy)], ton(toile, 0.86), 0.8)
    for x, y in ((160, 128), (170, 118), (186, 120), (198, 130), (176, 132)):                 # le monogramme, a peine
        t.ovale(x - 1.3, y + dy - 1.3, x + 1.3, y + dy + 1.3, ton(toile_o, 0.78))
    t.ovale(175, 104 + dy, 181, 109 + dy, toile_o)                                             # le bouton
    t.rect(146, 140 + dy, 210, 142 + dy, (22, 62, 40))                                         # la bande verte, rouge, verte
    t.rect(146, 142 + dy, 210, 144 + dy, (104, 24, 28))
    t.rect(146, 144 + dy, 210, 146 + dy, (22, 62, 40))
    t.rect(198, 140 + dy, 210, 146 + dy, mix((60, 50, 50), ecran, 0.5))
    # les cheveux, sous la casquette : des meches jusqu'aux epaules, pas un bloc
    cheveux = [(147, 146 + dy), (210, 146 + dy), (212, 160 + dy), (208, 174 + dy), (212, 190 - h), (202, 184 - h), (198, 194 - h),
               (190, 182 + dy), (184, 188 + dy), (178, 180 + dy), (170, 188 + dy), (164, 180 + dy), (158, 194 - h), (152, 184 - h),
               (144, 190 - h), (148, 174 + dy), (144, 160 + dy)]
    t.forme(cheveux, (10, 10, 14))
    t.trait([(210, 147 + dy), (212, 160 + dy), (208, 174 + dy), (212, 189 - h)], rim, 1.1)
    for x0, x1 in ((154, 152), (162, 160), (196, 198), (204, 206)):
        t.trait([(x0, 150 + dy), (x1, 182 + dy)], (26, 26, 36), 0.7)
    t.forme([(170, 146 + dy), (172, 136 + dy), (184, 136 + dy), (186, 146 + dy)], (10, 10, 14))  # l'ouverture de la casquette
    t.rect(170, 138 + dy, 186, 140 + dy, toile_o)                                                # sa patte de reglage
    # l'ecouteur : le fil blanc qui passe sur l'epaule
    t.trait([(207, 160 + dy), (210, 178), (214, 196 - dy // 2), (216, 212)], (210, 214, 224), 0.9)


def scene(etat):
    t = Toile((16, 16, 26))
    piece(t, etat)
    t.img = lueur(t.img, ECRANS[etat], FORCE[etat], 200)
    t.d = ImageDraw.Draw(t.img)
    # la tele elle-meme reste nette par-dessus son halo
    ecran = ECRANS[etat]
    t.forme([(262, 112), (338, 112), (340, 140), (338, 168), (262, 168), (260, 140)], mix(ecran, (255, 255, 255), 0.15 if etat != "neige" else 0))
    if etat == "programme":
        t.ovale(282, 124, 320, 158, (210, 230, 255))
        t.ovale(292, 116, 336, 148, (176, 206, 252))
    if etat == "neige":
        rng = random.Random(9)
        for k in range(700):
            x, y = rng.randrange(263, 338), rng.randrange(113, 167)
            v = rng.choice((60, 100, 160, 230))
            t.rect(x, y, x + 1, y + 0.7, (v, v, v + 6))
    for y in range(112, 168, 3):
        t.trait([(262, y), (338, y)], ton(ecran, 0.78), 0.5)
    table(t, etat, 0)
    heros(t, etat)
    canape(t, etat)
    if etat == "eclat":                                                # l'eclat : un voile blanc froid sur tout
        voile = Image.new("RGB", t.img.size, (200, 214, 240))
        t.img = Image.blend(t.img, voile, 0.10)
    t.img = vignette(t.img, {"programme": 0.78, "eclat": 0.45, "neige": 0.85}[etat])
    net = t.img.resize((L, H), Image.LANCZOS)
    return pc98.convertir(net, L, H, couleurs=32, trame=20, saturation=1.1, contraste=1.1)


def toutes():
    return {"gp_dos": scene("programme"), "gp_dos_eclat": scene("eclat"), "gp_dos_neige": scene("neige")}


if __name__ == "__main__":
    vues = toutes()
    planche = Image.new("RGB", (L * 2 + 8, H * 2 + 8), (0, 0, 0))
    for k, (nom, img) in enumerate(vues.items()):
        img.save(SORTIE / f"intro_{nom}.png")
        planche.paste(img, ((k % 2) * (L + 8), (k // 2) * (H + 8)))
    planche.resize((planche.width * 2, planche.height * 2), Image.NEAREST).save(SORTIE / "intro_dos_planche.png")
    print("ok")
