"""Le heros en grand, dessine a la main : le portrait des dialogues et des cinematiques, facon jeu PC-98.

Aucune generation : chaque forme est posee ici, a partir du modele du personnage (casquette beige a monogramme et
bande verte, rouge, verte ; petites lunettes noires ovales ; ecouteurs a fil ; cheveux noirs mi-longs ; haut rouge).
Le dessin est fait quatre fois trop grand, avec un trait d'encre et des ombres tranchees, puis ramene a la taille du
jeu (480 x 270) et a 16 couleurs tramees (decor/pc98.py). Lancement : python heros_portrait.py  (depuis designs/)
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent / "decor"))
import pc98  # noqa: E402
from outils import Toile  # noqa: E402

K = 4                                   # on dessine quatre fois trop grand
L, H = 480, 270
SORTIE = Path(__file__).resolve().parent / "outputs"
ENCRE = (30, 22, 30)
PEAU, PEAU_O, PEAU_C = (238, 198, 160), (198, 146, 114), (250, 226, 200)
CHEVEUX, CHEVEUX_C = (28, 24, 36), (78, 70, 96)
TOILE, TOILE_O, MOTIF = (208, 182, 140), (168, 142, 104), (178, 150, 110)
VERT, ROUGE, VISIERE, VISIERE_O = (40, 118, 72), (202, 46, 50), (112, 82, 54), (70, 50, 34)
TEE, TEE_O, TEE_C = (184, 58, 54), (130, 36, 42), (212, 96, 84)


def adoucir(pts, passes=2):
    """Arrondit un contour ferme (coupe les angles, methode de Chaikin) : des courbes au lieu de segments."""
    for _ in range(passes):
        out = []
        for i, (x0, y0) in enumerate(pts):
            x1, y1 = pts[(i + 1) % len(pts)]
            out += [(0.75 * x0 + 0.25 * x1, 0.75 * y0 + 0.25 * y1), (0.25 * x0 + 0.75 * x1, 0.25 * y0 + 0.75 * y1)]
        pts = out
    return pts


class Dessin:
    def __init__(self, fond):
        self.img = Image.new("RGB", (L * K, H * K), fond)
        self.d = ImageDraw.Draw(self.img)
        self.s, self.dy = 1.0, 0            # le buste est agrandi autour de (240, 150) et remonte dans le cadre

    def pt(self, x, y):
        return ((240 + (x - 240) * self.s) * K, (150 + (y - 150) * self.s + self.dy) * K)

    def forme(self, pts, c, trait=0, courbe=0):
        if courbe:
            pts = adoucir(pts, courbe)
        p = [self.pt(x, y) for x, y in pts]
        self.d.polygon(p, fill=c)
        if trait:
            self.d.line(p + [p[0]], fill=ENCRE, width=int(trait * K), joint="curve")

    def trait(self, pts, c=ENCRE, e=1.2):
        self.d.line([self.pt(x, y) for x, y in pts], fill=c, width=max(1, int(e * K)), joint="curve")

    def ovale(self, x0, y0, x1, y1, c, trait=0):
        (a, b), (e, f) = self.pt(x0, y0), self.pt(x1, y1)
        self.d.ellipse([a, b, e, f], fill=c, outline=ENCRE if trait else None, width=int(trait * K))

    def arc(self, x0, y0, x1, y1, debut, fin, c):
        (a, b), (e, f) = self.pt(x0, y0), self.pt(x1, y1)
        self.d.arc([a, b, e, f], debut, fin, fill=c, width=K)


def miroir(pts):
    return [(480 - x, y) for x, y in pts]


def decor(d):
    """Chez lui, la nuit : un mur brun, le store de la fenetre, la lueur bleue de la tele qui vient de la gauche."""
    d.forme([(0, 0), (480, 0), (480, 270), (0, 270)], (104, 78, 62))
    for x in range(0, 480, 48):                                        # le lambris
        d.trait([(x, 0), (x, 270)], (84, 62, 50), 1)
    d.forme([(0, 0), (150, 0), (90, 270), (0, 270)], (92, 92, 120))    # la lumiere de la tele sur le mur
    d.forme([(0, 0), (70, 0), (30, 270), (0, 270)], (120, 128, 168))
    d.forme([(366, 24), (470, 24), (470, 190), (366, 190)], (40, 36, 46), 1.4)   # la fenetre et son store
    for y in range(32, 188, 9):
        d.forme([(370, y), (466, y), (466, y + 5), (370, y + 5)], (70, 66, 84))
    d.forme([(360, 190), (476, 190), (476, 197), (360, 197)], (64, 48, 40), 1)


def buste(d, expression="tele"):
    # --- le haut rouge : les epaules tombantes
    corps = [(92, 270), (112, 228), (168, 206), (312, 206), (368, 228), (388, 270)]
    d.forme(corps, TEE, 1.6, 1)
    d.forme([(300, 206), (312, 206), (368, 228), (388, 270), (322, 270), (318, 232)], TEE_O)
    d.forme([(112, 228), (168, 206), (182, 206), (140, 236), (126, 270), (104, 270)], TEE_C)
    for a, b in (((150, 232), (160, 262)), ((334, 234), (326, 266)), ((180, 222), (186, 240))):   # les plis
        d.trait([a, b], TEE_O, 1.2)
    # --- le cou
    d.forme([(212, 176), (268, 176), (274, 214), (206, 214)], PEAU, 1.4)
    d.forme([(246, 176), (268, 176), (274, 214), (252, 214)], PEAU_O)
    d.forme([(212, 180), (268, 180), (262, 196), (236, 202), (214, 192)], PEAU_O)           # l'ombre du menton
    d.forme([(196, 206), (284, 206), (268, 222), (240, 228), (212, 222)], PEAU)             # l'encolure
    d.forme([(250, 206), (284, 206), (268, 222), (246, 227)], PEAU_O)
    d.trait([(194, 205), (212, 223), (240, 230), (268, 223), (286, 205)], ENCRE, 1.6)
    d.trait([(190, 208), (210, 228), (240, 236), (270, 228), (290, 208)], TEE_O, 2)
    # --- les cheveux, derriere le visage : mi-longs, jusqu'aux epaules
    d.forme([(170, 90), (310, 90), (320, 150), (314, 200), (296, 212), (290, 150), (190, 150), (184, 212), (166, 200), (160, 150)],
            CHEVEUX, 1.4, 1)
    for x, y0, y1 in ((168, 120, 196), (175, 130, 204), (312, 120, 196), (305, 130, 204)):
        d.trait([(x, y0), (x + (2 if x < 240 else -2), y1)], CHEVEUX_C, 1)
    # --- les oreilles
    d.ovale(174, 122, 192, 154, PEAU, 1.3)
    d.ovale(288, 122, 306, 154, PEAU_O, 1.3)
    # --- le visage : machoire anguleuse, menton etroit
    visage = [(188, 96), (292, 96), (294, 134), (286, 160), (264, 186), (240, 197), (216, 186), (194, 160), (186, 134)]
    d.forme(visage, PEAU, 1.5, 2)
    d.forme([(268, 98), (292, 98), (294, 134), (286, 160), (264, 186), (248, 194), (262, 166), (270, 134)], PEAU_O)   # la joue dans l'ombre
    d.forme([(189, 100), (196, 100), (200, 158), (193, 152)], PEAU_C)                       # la tele eclaire la joue gauche
    d.forme(visage, PEAU, 0, 2) if False else None
    d.trait(adoucir(visage, 2) + adoucir(visage, 2)[:1], ENCRE, 1.5)
    # --- les meches qui encadrent le visage
    for pts, c in (([(184, 98), (198, 98), (197, 150), (191, 182), (181, 156)], CHEVEUX),
                   (miroir([(184, 98), (198, 98), (197, 150), (191, 182), (181, 156)]), (20, 16, 26))):
        d.forme(pts, c, 1.2)
    d.trait([(190, 104), (189, 168)], CHEVEUX_C, 1)
    # --- le nez, la bouche
    d.forme([(240, 152), (248, 172), (238, 175)], PEAU_O)
    d.trait([(241, 150), (248, 172), (238, 175)], ENCRE, 1.1)
    if expression == "tele":
        d.trait([(224, 183), (240, 184), (256, 182)], (110, 60, 60), 1.6)                   # la bouche, fermee
        d.trait([(232, 190), (248, 190)], PEAU_O, 1.4)
    else:                                                              # la bouche s'ouvre toute seule : il ne s'en rend pas compte
        d.ovale(231, 176, 249, 193, (84, 38, 46), 1.3)
        d.forme([(235, 187), (245, 187), (243, 191), (237, 191)], (150, 76, 76))            # la langue, a peine
        d.forme([(196, 128), (200, 136), (196, 141), (192, 136)], (190, 214, 244), 1, 1)    # une goutte de sueur, a la tempe
        d.forme([(288, 162), (291, 168), (288, 172), (285, 168)], (190, 214, 244), 1, 1)
    # --- la casquette
    calotte = [(168, 98), (169, 74), (184, 52), (214, 40), (266, 40), (296, 52), (311, 74), (312, 98)]
    d.forme(calotte, TOILE, 1.5)
    d.forme([(278, 44), (296, 52), (311, 74), (312, 98), (282, 98), (284, 70)], TOILE_O)
    for cx in range(182, 304, 20):                                     # le monogramme : deux arcs entrelaces, ton sur ton
        for cy in (54, 66, 78):
            x = cx + (10 if cy == 66 else 0)
            if 176 < x < 304 and not (cy == 54 and (x < 204 or x > 278)):
                d.arc(x - 5, cy - 4, x + 1, cy + 4, 40, 320, MOTIF)
                d.arc(x - 1, cy - 4, x + 5, cy + 4, 220, 140, MOTIF)
    d.trait([(240, 40), (214, 66), (206, 98)], TOILE_O, 1.2)                                # les coutures
    d.trait([(240, 40), (266, 66), (274, 98)], (150, 126, 92), 1.2)
    d.ovale(234, 35, 246, 43, TOILE_O, 1.2)                                                 # le bouton
    d.forme([(168, 84), (312, 84), (312, 89), (168, 89)], VERT)                             # la bande : verte, rouge, verte
    d.forme([(168, 89), (312, 89), (312, 94), (168, 94)], ROUGE)
    d.forme([(168, 94), (312, 94), (312, 99), (168, 99)], VERT)
    d.forme([(284, 84), (312, 84), (312, 99), (284, 99)], (30, 86, 54))
    d.forme([(284, 89), (312, 89), (312, 94), (284, 94)], (150, 34, 40))
    d.trait([(168, 84), (312, 84)], ENCRE, 1)
    d.trait(calotte, ENCRE, 1.5)
    # la visiere d'une casquette, vue de face : pas plus large que la calotte, courbee vers le bas au milieu
    visiere = [(172, 99), (308, 99), (310, 107), (290, 119), (240, 125), (190, 119), (170, 107)]
    d.forme(visiere, VISIERE, 1.5)
    d.forme([(170, 107), (190, 119), (240, 125), (290, 119), (310, 107), (288, 114), (240, 119), (192, 114)], VISIERE_O)
    d.trait([(176, 102), (304, 102)], (150, 114, 78), 1.2)
    d.trait(visiere + [visiere[0]], ENCRE, 1.5)
    if True:
        # les sourcils, fronces, puis les petites lunettes noires ovales, sans monture
        if expression == "tele":                                       # les sourcils, fronces
            sourcil = [(196, 136), (232, 142), (232, 146), (196, 141)]
            d.forme(sourcil, CHEVEUX)
            d.forme(miroir(sourcil), CHEVEUX)
        else:                                                          # la stupeur : les sourcils montent, en arc
            d.trait([(196, 135), (213, 127), (232, 132)], CHEVEUX, 2.6)
            d.trait(miroir([(196, 135), (213, 127), (232, 132)]), CHEVEUX, 2.6)
        for x0 in (194, 244):
            d.ovale(x0, 144, x0 + 42, 162, (14, 14, 22), 1.2)
            if expression == "tele":
                d.forme([(x0 + 8, 148), (x0 + 22, 147), (x0 + 18, 152), (x0 + 6, 153)], (126, 146, 208))   # le reflet de l'ecran
                d.trait([(x0 + 26, 148), (x0 + 32, 148)], (70, 76, 100), 1)
            else:                                                      # l'ecran a change : le reflet envahit le verre
                d.ovale(x0 + 4, 146.5, x0 + 38, 159.5, (120, 160, 250))     # un bleu franc, qui reste dans le verre noir
                d.forme([(x0 + 10, 149), (x0 + 30, 148), (x0 + 28, 152), (x0 + 9, 153)], (190, 214, 255))
        d.trait([(236, 152), (244, 152)], (120, 120, 136), 1.4)                             # le pont
        d.trait([(194, 152), (182, 148)], (120, 120, 136), 1.2)                             # les branches
        d.trait([(286, 152), (298, 148)], (90, 90, 104), 1.2)
    # --- les ecouteurs blancs et leurs fils
    d.forme([(178, 132), (187, 132), (187, 146), (178, 146)], (244, 244, 240), 1)
    d.forme([(293, 132), (302, 132), (302, 146), (293, 146)], (206, 206, 210), 1)
    d.trait([(182, 146), (186, 186), (206, 232), (226, 270)], (244, 244, 240), 1.5)
    d.trait([(298, 146), (294, 186), (274, 232), (254, 270)], (206, 206, 210), 1.5)


def _genou(d):
    """Le cadre des gros plans de la main : le canape, sa cuisse (le pantalon noir, l'empiecement rouge), la manche."""
    d.forme([(0, 0), (480, 0), (480, 270), (0, 270)], (64, 66, 90))
    d.forme([(0, 0), (190, 0), (110, 270), (0, 270)], (96, 104, 146))               # la lumiere de la tele
    for x in range(40, 480, 70):
        d.trait([(x, 0), (x - 20, 150)], (52, 54, 76), 1.2)
    d.forme([(0, 196), (480, 150), (480, 270), (0, 270)], (40, 38, 48), 1.6)        # la cuisse
    d.forme([(250, 178), (480, 154), (480, 214), (262, 240)], (128, 32, 40), 1.2)   # l'empiecement de cuir rouge
    d.trait([(0, 232), (250, 210)], (26, 24, 32), 1.2)
    d.forme([(0, 96), (150, 104), (160, 182), (0, 204)], TEE, 1.6)                  # la manche rouge
    d.forme([(0, 170), (160, 160), (160, 182), (0, 204)], TEE_O)
    d.trait([(40, 110), (60, 176)], TEE_O, 1.2)
    d.trait([(100, 108), (112, 172)], TEE_O, 1.2)
    d.forme([(140, 100), (166, 100), (176, 184), (150, 186)], TEE_O, 1.4)           # le poignet en cote


def main(serree=False):
    """La main, de pres. D'abord detendue : la cigarette entre deux doigts, la fumee qui monte. Puis (serree=True) elle
    lache : les doigts s'ouvrent, la cigarette tombe."""
    d = Dessin((64, 66, 90))
    _genou(d)
    gant, gant_c = (26, 26, 36), (60, 60, 78)
    if not serree:
        d.forme([(162, 98), (252, 96), (262, 180), (172, 184)], gant, 1.6, 1)        # la mitaine
        d.trait([(180, 112), (244, 110)], gant_c, 1.2)
        d.trait([(184, 168), (250, 164)], gant_c, 1.2)
        d.forme([(196, 100), (240, 62), (258, 72), (226, 112)], PEAU, 1.5, 1)        # le pouce
        for k, (y, bout) in enumerate(((98, 336), (120, 346), (142, 334), (162, 314))):
            c = PEAU if k % 2 == 0 else (224, 180, 144)
            d.forme([(250, y), (bout, y - 2 + k), (bout + 4, y + 16 + k), (252, y + 20)], c, 1.4, 1)
            d.trait([(bout - 12, y + 2 + k), (bout - 4, y + 3 + k)], PEAU_C, 1)      # l'ongle
        d.forme([(322, 112), (418, 100), (420, 109), (324, 122)], (244, 242, 232), 1.2)    # la cigarette
        d.forme([(322, 112), (346, 109), (348, 119), (324, 122)], (210, 160, 96))
        d.forme([(404, 102), (418, 100), (420, 109), (406, 111)], (120, 116, 116))
        d.forme([(414, 100), (424, 99), (426, 108), (416, 109)], (255, 138, 40))           # la braise
        fumee = [(424, 98), (432, 78), (422, 58), (436, 36), (428, 14), (438, 0)]
        d.trait(fumee, (176, 180, 204), 2.2)
        d.trait([(x + 8, y + 6) for x, y in fumee[1:4]], (132, 136, 166), 1.4)
    else:
        d.forme([(162, 98), (252, 96), (262, 180), (172, 184)], gant, 1.6, 1)        # la meme main : les doigts ont lache
        d.trait([(180, 112), (244, 110)], gant_c, 1.2)
        d.trait([(184, 168), (250, 164)], gant_c, 1.2)
        d.forme([(196, 100), (228, 58), (246, 64), (226, 112)], PEAU, 1.5, 1)        # le pouce, ecarte
        for k in range(4):                                                           # les doigts s'ouvrent et retombent
            y, ex, ey = 98 + k * 21, 334 - k * 9, 96 + k * 31
            c = PEAU if k % 2 == 0 else (224, 180, 144)
            d.forme([(250, y), (ex, ey), (ex + 3, ey + 16), (252, y + 19)], c, 1.4, 1)
            d.trait([(ex - 12, ey + 3), (ex - 4, ey + 4)], PEAU_C, 1)
        d.forme([(372, 168), (383, 173), (350, 232), (339, 227)], (244, 242, 232), 1.2)    # la cigarette, en train de tomber
        d.forme([(372, 168), (383, 173), (376, 186), (365, 181)], (210, 160, 96))
        d.forme([(339, 227), (350, 232), (346, 242), (335, 237)], (255, 138, 40))          # la braise, en bas
        for a, b in (((366, 140), (370, 158)), ((390, 146), (392, 164)), ((346, 150), (351, 164))):
            d.trait([a, b], (176, 180, 204), 1.4)                                    # le sillage de la chute
        for x, y in ((330, 250), (352, 256), (340, 262)):
            d.forme([(x, y), (x + 4, y), (x + 4, y + 4), (x, y + 4)], (150, 146, 150))
    net = d.img.resize((L, H), Image.LANCZOS)
    return pc98.convertir(net, L, H, couleurs=16, trame=26, saturation=1.1, contraste=1.08)


def portrait(expression="tele"):
    """Le plan complet, a la taille du jeu : 480 x 270, 16 couleurs tramees. expression : "tele" (calme) ou "choc"
    (les sourcils hauts, la bouche qui s'ouvre, le reflet qui envahit les verres). Il garde toujours ses lunettes."""
    d = Dessin((104, 78, 62))
    decor(d)
    d.s, d.dy = 1.22, -22
    buste(d, expression)
    net = d.img.resize((L, H), Image.LANCZOS)
    return pc98.convertir(net, L, H, couleurs=16, trame=26, saturation=1.1, contraste=1.08), d.img


def ecran(img, nom, texte):
    """Le portrait sous la boite de dialogue."""
    t = Toile(L, H)
    t.img.paste(img)
    t.rect(10, 212, 470, 262, (14, 14, 26, 255))
    for x0, y0, x1, y1 in ((10, 212, 470, 213), (10, 261, 470, 262), (10, 212, 11, 262), (469, 212, 470, 262)):
        t.rect(x0, y0, x1, y1, (150, 150, 178, 255))
    t.rect(16, 202, 22 + 6 * len(nom) + 8, 214, (150, 150, 178, 255))
    t.rect(17, 203, 21 + 6 * len(nom) + 8, 213, (70, 62, 110, 255))
    t.texte(22, 206, nom, (240, 240, 244), 5)
    t.texte(22, 224, texte, (240, 240, 244), 5)
    return t.img.convert("RGB")


if __name__ == "__main__":
    vues = [portrait("tele")[0], main(False), portrait("choc")[0], main(True)]
    planche = Image.new("RGB", (L * 2 + 8, H * 2 + 8), (0, 0, 0))
    for k, (nom, img) in enumerate(zip(("tele", "main", "choc", "lache"), vues)):
        img.save(SORTIE / f"heros_plan_{nom}.png")
        planche.paste(img, ((k % 2) * (L + 8), (k // 2) * (H + 8)))
    planche.resize((planche.width * 2, planche.height * 2), Image.NEAREST).save(SORTIE / "heros_suite_intro.png")
    print("ok")
