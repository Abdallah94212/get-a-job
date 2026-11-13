"""Les interieurs ou l'on entre : chaque fonction dessine le fond d'une carte, en 270 pixels de haut."""
import random
from pathlib import Path

from PIL import Image, ImageEnhance

from outils import *
from facades import coq

PLAFOND, SOL_INT = 40, 180   # bas du plafond, pied des murs


def _carrelage(t, x0, x1, clair, sombre, pas=16):
    for y in range(SOL_INT, 270, pas):
        for x in range(x0, x1, pas):
            pair = ((x - x0) // pas + (y - SOL_INT) // pas) % 2 == 0
            t.rect(x, y, min(x1, x + pas), y + pas, clair if pair else sombre)


def _cloison(t, x):
    t.rect(x, PLAFOND, x + 6, SOL_INT, (70, 70, 80))
    t.rect(x, PLAFOND, x + 1, SOL_INT, (96, 96, 106))


def maitre_poulet():
    """Le Maitre Poulet vu de l'interieur : la salle, le comptoir, la cuisine, puis l'arriere-boutique."""
    rng = random.Random(301)
    t = Toile(760, 270, (26, 26, 34, 255))
    creme, orange = (170, 160, 140), (232, 100, 8)
    # --- la salle
    t.rect(0, PLAFOND, 296, SOL_INT, creme)
    t.rect(0, 100, 296, 104, orange)
    t.rect(0, 150, 296, SOL_INT, ton(creme, 0.8))
    _carrelage(t, 0, 436, (128, 120, 108), (96, 90, 84))
    t.rect(6, 104, 30, SOL_INT, FER)                                   # la porte sur l'avenue
    t.rect(9, 108, 27, 150, NUIT)
    t.rect(25, 140, 27, 148, (150, 150, 156))
    t.rect(48, 64, 210, 148, FER)                                      # la vitrine, la nuit dehors
    t.rect(51, 67, 207, 145, NUIT)
    for lx, ly in ((80, 80), (150, 76), (190, 90)):
        t.ellipse(lx, ly, lx + 6, ly + 6, (150, 140, 110))
    t.texte(72, 104, "TELUOP ERTIAM", ton(orange, 0.5), 5)             # l'enseigne, vue a l'envers
    for tx in (60, 130, 200):                                          # tables et chaises contre le mur
        t.rect(tx, 160, tx + 40, 164, (150, 150, 156))
        t.rect(tx + 18, 164, tx + 22, SOL_INT + 8, (60, 62, 70))
        for cx in (tx - 12, tx + 42):
            t.rect(cx, 150, cx + 10, 152, orange)
            t.rect(cx, 152, cx + 2, SOL_INT + 8, (70, 66, 62))
            t.rect(cx + 8, 152, cx + 10, SOL_INT + 8, (70, 66, 62))
            t.rect(cx, 166, cx + 10, 168, orange)
    t.ellipse(236, 52, 276, 92, (240, 236, 224), FER)
    coq(t, 243, 62, orange, 3)
    # --- derriere le comptoir : les menus, la rotissoire
    _cloison(t, 296)
    t.rect(302, PLAFOND, 430, SOL_INT, ton(creme, 0.9))
    for k in range(3):
        mx = 306 + k * 42
        t.rect(mx, 50, mx + 38, 78, (250, 244, 220))
        t.rect(mx + 3, 54, mx + 16, 64, orange)
        for ly in (56, 62, 68, 72):
            t.rect(mx + 20, ly, mx + 34, ly + 1, (120, 110, 96))
    t.rect(310, 92, 372, 172, (60, 56, 54))
    for k in range(4):
        yb = 100 + k * 17
        t.ligne([(312, yb + 5), (369, yb + 5)], (30, 28, 28))
        for xx in range(316, 364, 12):
            t.ellipse(xx, yb, xx + 10, yb + 10, (214, 150, 84))
            t.rect(xx + 2, yb + 2, xx + 5, yb + 4, (246, 200, 130))
    t.rect(384, 120, 424, 172, (150, 154, 160))                       # la friteuse
    t.rect(388, 126, 420, 140, (40, 42, 50))
    # --- la cuisine
    _cloison(t, 430)
    blanc = (188, 192, 196)
    t.rect(436, PLAFOND, 600, SOL_INT, blanc)
    for x in range(436, 600, 12):
        t.ligne([(x, PLAFOND), (x, SOL_INT - 1)], ton(blanc, 0.86))
    for y in range(PLAFOND, SOL_INT, 12):
        t.ligne([(436, y), (599, y)], ton(blanc, 0.86))
    _carrelage(t, 436, 606, (112, 114, 120), (92, 94, 100))
    t.poly([(456, 44), (580, 44), (566, 74), (470, 74)], (84, 88, 96))  # la hotte
    t.rect(450, 132, 590, 178, (150, 154, 160))                       # les fourneaux
    t.rect(450, 132, 590, 135, (190, 194, 200))
    for xx in (462, 500, 538):
        t.ellipse(xx, 122, xx + 24, 134, (50, 52, 60))
        t.rect(xx + 4, 150, xx + 20, 170, (60, 62, 70))
    t.rect(444, 86, 596, 89, (110, 112, 120))                         # l'etagere et ses casseroles
    for xx in range(450, 590, 18):
        t.rect(xx, 86 - rng.randrange(8, 16), xx + 12, 86, rng.choice(((120, 124, 132), (90, 92, 100))))
    # --- l'arriere-boutique : la reserve, et ce qu'Old Geto vend derriere le comptoir
    _cloison(t, 600)
    beton = (104, 104, 110)
    t.rect(606, PLAFOND, 760, SOL_INT, beton)
    t.rect(606, SOL_INT, 760, 270, (70, 70, 76))
    for y in range(SOL_INT + 14, 270, 22):
        t.ligne([(606, y), (759, y)], (58, 58, 64))
    for ey in (70, 104, 138):                                          # les etageres et les cartons
        t.rect(614, ey, 700, ey + 3, (60, 62, 70))
        xx = 616
        while xx < 690:
            w, h = rng.randrange(12, 22), rng.randrange(14, 26)
            t.rect(xx, ey - h, xx + w, ey, rng.choice(((124, 108, 88), (104, 92, 76), (140, 122, 98))))
            t.ligne([(xx, ey - h // 2), (xx + w - 1, ey - h // 2)], (80, 70, 58))
            xx += w + 2
    for xx in (614, 698):
        t.rect(xx, 50, xx + 3, SOL_INT, (60, 62, 70))
    t.rect(626, 150, 664, SOL_INT, (124, 108, 88))                    # le carton entrouvert : la Verte
    t.rect(630, 146, 660, 152, VERT)
    t.rect(634, 142, 656, 146, ton(VERT, 0.7))
    t.rect(712, 92, 752, SOL_INT, (150, 154, 160))                    # la porte de derriere
    t.rect(716, 96, 748, SOL_INT - 2, (110, 114, 122))
    t.rect(718, 128, 722, 140, (60, 62, 70))
    t.texte(716, 80, "RUELLE", (60, 62, 70), 5)
    t.rect(0, PLAFOND - 3, 760, PLAFOND, (60, 60, 70))
    for nx in (60, 180, 330, 500, 660):                                # les neons
        t.rect(nx, PLAFOND - 6, nx + 40, PLAFOND - 3, (250, 246, 230))
    t.grain(4, 302)
    return t.img


def comptoir():
    t = Toile(128, 44)
    t.rect(0, 4, 128, 44, (96, 90, 84))
    t.rect(0, 0, 128, 5, (190, 186, 176))
    t.rect(0, 14, 128, 17, (232, 100, 8))
    t.rect(86, -0, 110, 0, FER)
    return t.img


def caisse():
    t = Toile(22, 18)
    t.rect(0, 6, 22, 18, (50, 52, 60))
    t.rect(3, 0, 19, 8, (60, 62, 70))
    t.rect(5, 2, 17, 6, BUREAU)
    return t.img


def appartement():
    """Chez toi : le canape, la tele cathodique, le poster, le livre sur la table, le mur du plan."""
    rng = random.Random(311)
    t = Toile(480, 270, (22, 22, 30, 255))
    mur = (58, 58, 70)
    t.rect(0, PLAFOND, 480, SOL_INT, mur)
    t.rect(0, 168, 480, SOL_INT, ton(mur, 0.75))
    t.rect(0, SOL_INT, 480, 270, (78, 68, 62))
    for y in range(SOL_INT + 12, 270, 14):
        t.ligne([(0, y), (479, y)], (62, 54, 50))
    for x in range(20, 480, 60):
        t.ligne([(x, SOL_INT), (x - 14, 269)], (62, 54, 50))
    t.rect(8, 100, 34, SOL_INT, FER)                                   # la porte
    t.rect(11, 104, 31, SOL_INT - 2, (74, 66, 60))
    t.rect(27, 138, 29, 146, (150, 150, 156))
    t.rect(50, 54, 100, 130, (196, 192, 180))                          # le poster : un homme qui se tient la tete
    source = Path(__file__).resolve().parent / "packs" / "affiche_chambre" / "affiche.jpg"
    affiche = Image.open(source).convert("RGB")
    l = affiche.height * 44 // 70                                      # recadree au format du poster, centree sur lui
    affiche = affiche.crop(((affiche.width - l) // 2, 0, (affiche.width + l) // 2, affiche.height))
    affiche = affiche.resize((44, 70), Image.LANCZOS).quantize(20, dither=Image.NONE).convert("RGB")
    t.img.paste(ImageEnhance.Brightness(affiche).enhance(0.8), (53, 57))
    t.rect(120, 140, 250, 176, (90, 90, 102))                         # le canape
    t.rect(114, 132, 126, 180, (74, 74, 86))
    t.rect(244, 132, 256, 180, (74, 74, 86))
    t.rect(120, 124, 250, 144, (104, 104, 116))
    t.ligne([(185, 126), (185, 174)], (74, 74, 86))
    t.rect(284, 150, 352, 180, (60, 54, 50))                          # le meuble de la tele
    t.rect(290, 106, 346, 152, (40, 40, 48))
    t.rect(300, 152, 336, 156, (30, 30, 36))
    t.rect(170, 180, 226, 184, (70, 62, 56))                          # la table basse et le livre
    t.rect(174, 184, 178, 198, (54, 48, 44))
    t.rect(218, 184, 222, 198, (54, 48, 44))
    t.rect(188, 175, 208, 180, (150, 40, 40))
    t.rect(189, 176, 207, 177, (196, 190, 176))
    t.rect(372, 56, 462, 136, (96, 82, 66))                           # le mur du plan : photos et fils rouges
    points = []
    for _ in range(9):
        px, py = rng.randrange(378, 446), rng.randrange(62, 118)
        t.rect(px, py, px + 12, py + 10, (190, 186, 176))
        t.rect(px + 2, py + 2, px + 10, py + 6, (90, 90, 100))
        points.append((px + 6, py + 1))
    for a, b in zip(points, points[1:]):
        t.ligne([a, b], (190, 44, 44))
    t.rect(376, 150, 458, SOL_INT, (70, 62, 56))                      # le bureau sous le mur
    t.rect(380, 144, 404, 150, (110, 112, 120))
    t.grain(4, 312)
    return t.img


# ---------------------------------------------------------------- les lieux de mission
def piece(largeur, mur, sol, joints=None, porte=True):
    """Une piece vide : plafond, mur, sol, et la porte de sortie a gauche."""
    t = Toile(largeur, 270, (22, 22, 30, 255))
    t.rect(0, PLAFOND, largeur, SOL_INT, mur)
    t.rect(0, 166, largeur, SOL_INT, ton(mur, 0.78))
    t.rect(0, SOL_INT, largeur, 270, sol)
    for y in range(SOL_INT + 14, 270, 18):
        t.ligne([(0, y), (largeur - 1, y)], joints or ton(sol, 0.8))
    t.rect(0, PLAFOND - 3, largeur, PLAFOND, (60, 60, 70))
    if porte:
        t.rect(8, 100, 34, SOL_INT, FER)
        t.rect(11, 104, 31, SOL_INT - 2, ton(mur, 0.6))
        t.rect(27, 138, 29, 146, (150, 150, 156))
    return t


def _neons(t, positions, c=(240, 246, 250)):
    for x in positions:
        t.rect(x, PLAFOND - 6, x + 44, PLAFOND - 3, c)


def _silhouette(t, x, c, tete=(150, 140, 132), h=52):
    """Un personnage debout contre le mur, en attendant son vrai dessin."""
    y = SOL_INT + 6 - h
    t.poly([(x + 3, y + 12), (x + 13, y + 12), (x + 15, y + h - 4), (x + 1, y + h - 4)], c)
    t.rect(x + 3, y + h - 4, x + 7, y + h, (24, 24, 30))
    t.rect(x + 9, y + h - 4, x + 13, y + h, (24, 24, 30))
    t.ellipse(x + 3, y, x + 13, y + 12, tete)


def labo():
    """Le labo de Gero, en cinq zones qu'on lit de gauche a droite : le vestiaire ou l'on se change, la paillasse ou
    l'on prepare, les cuves ou la Piccolo devient la Cell Perfect, le poste de controle qui surveille, le stock."""
    rng = random.Random(321)
    L = 960
    t = piece(L, (96, 84, 84), (70, 70, 76))
    for y in range(PLAFOND + 2, 166, 4):
        t.ligne([(40, y), (L - 1, y)], (80, 68, 70))
    _neons(t, (70, 230, 390, 590, 760, 880))
    acier, acier_c, acier_o = (130, 134, 142), (170, 174, 182), (96, 100, 108)
    # --- une gaine d'aeration court sous le plafond, d'un bout a l'autre
    t.rect(40, 46, L, 56, (104, 108, 116))
    t.rect(40, 46, L, 48, (132, 136, 144))
    for x in range(60, L, 48):
        t.ligne([(x, 46), (x, 55)], (80, 84, 92))
    for x in (150, 470, 804):
        t.rect(x, 56, x + 22, 62, (60, 62, 70))
        for k in range(4):
            t.ligne([(x + 3 + k * 5, 57), (x + 3 + k * 5, 61)], (120, 124, 132))
    # --- le vestiaire : casiers, blouses pendues, coin cafe, consignes
    for k in range(3):
        x0 = 48 + k * 22
        t.rect(x0, 84, x0 + 21, SOL_INT + 4, (84, 96, 104))
        t.rect(x0 + 1, 85, x0 + 20, SOL_INT + 3, (104, 116, 124))
        t.rect(x0 + 1, 132, x0 + 20, 133, (70, 80, 88))
        for y0 in (90, 138):
            for k2 in range(3):
                t.ligne([(x0 + 6, y0 + k2 * 3), (x0 + 15, y0 + k2 * 3)], (70, 80, 88))
            t.rect(x0 + 16, y0 + 16, x0 + 18, y0 + 20, (190, 190, 184))
        t.rect(x0 + 7, 86, x0 + 14, 89, BLANC)
    t.rect(46, 80, 116, 84, (70, 80, 88))
    t.rect(52, 72, 70, 80, (124, 108, 88))                              # un carton oublie sur les casiers
    t.ligne([(52, 76), (69, 76)], (90, 78, 64))
    t.rect(74, 74, 86, 80, (60, 62, 70))
    t.rect(122, 72, 178, 74, (60, 62, 70))                              # la patere et trois blouses
    for k, c in enumerate(((214, 214, 208), (198, 198, 192), (214, 214, 208))):
        x0 = 126 + k * 17
        t.ligne([(x0 + 6, 74), (x0 + 6, 78)], (60, 62, 70))
        t.poly([(x0 + 2, 80), (x0 + 10, 80), (x0 + 13, 124), (x0 - 1, 124)], c)
        t.ligne([(x0 + 6, 82), (x0 + 6, 122)], ton(c, 0.75))
        t.rect(x0 + 1, 100, x0 + 4, 104, ton(c, 0.8))
    t.rect(184, 96, 212, 128, BLANC)                                    # les consignes de securite
    t.rect(185, 97, 211, 104, (190, 60, 50))
    t.texte(187, 98, "REGLES", BLANC, 3)
    for k in range(5):
        t.rect(187, 108 + k * 4, 187 + rng.randrange(10, 19), 109 + k * 4, (120, 120, 126))
    t.rect(186, 140, 194, 166, (176, 50, 44))                           # l'extincteur
    t.rect(188, 136, 192, 140, (60, 62, 70))
    t.ligne([(193, 140), (198, 150)], (40, 40, 48))
    # --- la paillasse : deux plans de travail, la verrerie, les etageres de produits, la hotte
    for x0 in (220, 380):
        t.rect(x0, 140, x0 + 140, 146, acier_c)
        t.rect(x0 + 4, 146, x0 + 136, SOL_INT + 6, acier_o)
        for xx in (x0 + 36, x0 + 70, x0 + 104):                         # les portes du meuble
            t.ligne([(xx, 148), (xx, SOL_INT + 4)], (76, 80, 88))
        for xx in (x0 + 28, x0 + 62, x0 + 96, x0 + 128):
            t.rect(xx, 160, xx + 2, 166, acier_c)
        for k in range(6):
            bx, bh = x0 + 8 + k * 22, rng.randrange(14, 30)
            t.rect(bx + 4, 140 - bh, bx + 8, 140 - bh + 8, (150, 170, 176))
            t.ellipse(bx, 140 - bh + 6, bx + 12, 140, (120, 140, 150))
            t.ellipse(bx + 2, 140 - bh // 2, bx + 10, 139, lum(VERT) if k % 2 else ton(VERT, 0.55))
        t.ligne([(x0 + 14, 112), (x0 + 60, 100), (x0 + 110, 116)], (150, 170, 176))
        t.rect(x0 + 6, 74, x0 + 134, 77, (70, 66, 64))                  # l'etagere murale et ses bocaux
        t.rect(x0 + 10, 77, x0 + 12, 84, (70, 66, 64))
        t.rect(x0 + 128, 77, x0 + 130, 84, (70, 66, 64))
        xx = x0 + 10
        while xx < x0 + 126:
            w, h = rng.randrange(5, 10), rng.randrange(7, 14)
            c = rng.choice(((150, 170, 176), (128, 120, 96), (104, 112, 124), (150, 150, 156), (96, 128, 104)))
            t.rect(xx, 74 - h, xx + w, 74, c)
            t.rect(xx, 74 - h, xx + w, 74 - h + 2, ton(c, 0.6))
            t.rect(xx + 1, 74 - h + 4, xx + w - 1, 74 - h + 6, BLANC)
            xx += w + rng.randrange(1, 4)
    t.rect(232, 124, 240, 140, (60, 62, 70))                            # le microscope
    t.rect(236, 118, 246, 124, (60, 62, 70))
    t.rect(242, 124, 245, 134, (150, 154, 160))
    t.rect(228, 137, 250, 140, (40, 40, 48))
    t.rect(492, 124, 512, 140, (190, 190, 184))                         # la centrifugeuse
    t.rect(492, 124, 512, 128, (150, 150, 156))
    t.rect(496, 131, 500, 134, lum(VERT))
    t.rect(503, 131, 508, 136, (40, 40, 48))
    for x0, y0 in ((300, 136), (318, 137), (410, 136)):                 # des carnets et des fiches qui trainent
        t.rect(x0, y0, x0 + 12, y0 + 4, rng.choice((BLANC, (196, 190, 170), (150, 160, 180))))
        t.ligne([(x0 + 2, y0 + 2), (x0 + 9, y0 + 2)], (110, 110, 118))
    t.rect(534, 70, 600, SOL_INT + 4, acier)                            # la hotte, vitre baissee a moitie
    t.rect(534, 70, 600, 76, acier_c)
    t.rect(540, 80, 594, 132, (40, 46, 52))
    t.rect(540, 80, 594, 104, (70, 84, 92))
    t.ligne([(540, 104), (593, 104)], acier_c)
    t.ellipse(552, 112, 566, 132, (120, 140, 150))
    t.ellipse(555, 120, 563, 131, lum(VERT))
    t.rect(574, 118, 578, 132, (150, 170, 176))
    t.rect(540, 136, 594, 142, (60, 62, 70))
    for k in range(3):
        t.rect(546 + k * 16, 137, 552 + k * 16, 141, (190, 60, 50) if k == 0 else (150, 150, 156))
    t.rect(560, 56, 574, 70, (104, 108, 116))                           # elle souffle dans la gaine
    # --- les cuves : deux reacteurs, leurs tuyaux, le tableau de bord, l'armoire electrique
    t.rect(608, SOL_INT - 2, 760, SOL_INT + 6, (70, 70, 76))
    for x in range(608, 760, 12):                                       # le marquage au sol
        t.poly([(x, SOL_INT + 6), (x + 6, SOL_INT + 6), (x + 10, SOL_INT + 1), (x + 4, SOL_INT + 1)], (176, 150, 60))
    for x0 in (616, 668):
        t.rect(x0, 84, x0 + 40, SOL_INT + 2, acier)
        t.rect(x0, 84, x0 + 40, 90, acier_c)
        t.rect(x0 + 2, 90, x0 + 4, SOL_INT, acier_o)
        t.rect(x0 + 6, 116, x0 + 34, 150, lum(VERT))
        t.rect(x0 + 6, 116, x0 + 34, 120, lum(ton(VERT, 1.2)))
        for k in range(4):                                              # les bulles
            t.point(x0 + 10 + rng.randrange(20), 124 + rng.randrange(22), lum((200, 255, 210)))
        t.rect(x0 + 6, 96, x0 + 34, 110, (40, 40, 48))                  # la plaque et le manometre
        t.ellipse(x0 + 9, 97, x0 + 21, 109, BLANC)
        t.ligne([(x0 + 15, 103), (x0 + 18, 99)], (190, 60, 50))
        t.rect(x0 + 24, 99, x0 + 31, 102, lum(VERT))
        t.rect(x0 + 24, 104, x0 + 31, 107, (150, 60, 50))
        t.ellipse(x0 + 14, 156, x0 + 26, 168, (60, 62, 70))             # la vanne
        t.ligne([(x0 + 14, 162), (x0 + 25, 162)], (190, 60, 50))
        t.ligne([(x0 + 20, 84), (x0 + 20, 56)], (110, 114, 122), 3)
    t.ligne([(656, 130), (668, 130)], (110, 114, 122), 3)
    t.ligne([(708, 140), (722, 140), (722, SOL_INT)], (110, 114, 122), 3)
    t.rect(720, 76, 756, 124, (60, 66, 76))                             # l'armoire electrique
    t.rect(722, 78, 754, 122, (84, 92, 104))
    t.poly([(732, 86), (744, 86), (738, 96)], (176, 150, 60))
    t.rect(737, 88, 739, 92, NUIT)
    for k in range(4):
        t.rect(726 + k * 7, 102, 731 + k * 7, 108, (40, 40, 48))
        t.rect(727 + k * 7, 103 + (k % 2) * 2, 730 + k * 7, 105 + (k % 2) * 2, (190, 190, 184))
    t.rect(726, 112, 730, 116, lum(VERT))
    t.rect(733, 112, 737, 116, lum((255, 122, 18)))
    for k, c in enumerate(((40, 40, 48), (150, 60, 50), (60, 70, 110))):  # les cables qui en descendent
        t.ligne([(728 + k * 6, 124), (728 + k * 6, 150 + k * 6), (746 + k * 4, 160 + k * 4), (760 + k * 3, SOL_INT + 4)], c)
    # --- le poste de controle : le tableau blanc, le mur d'ecrans, la baie
    t.rect(770, 66, 848, 116, (150, 150, 156))
    t.rect(772, 68, 846, 114, (226, 226, 220))
    t.ligne([(778, 78), (790, 74), (800, 80), (812, 74)], (40, 60, 110))
    for k in range(3):                                                  # des hexagones : une molecule
        cx, cy = 786 + k * 12, 92 + (k % 2) * 6
        t.poly([(cx - 5, cy), (cx - 2, cy - 4), (cx + 3, cy - 4), (cx + 6, cy), (cx + 3, cy + 4), (cx - 2, cy + 4)], None, (40, 60, 110))
    t.texte(820, 74, "C+P", (190, 60, 50), 3)
    t.ligne([(818, 84), (840, 84)], (190, 60, 50))
    t.texte(818, 90, "72 H", (40, 60, 110), 3)
    t.rect(818, 100, 842, 110, (240, 220, 120))                         # un pense-bete
    t.rect(772, 116, 846, 118, (120, 120, 126))
    t.rect(780, 115, 788, 117, (190, 60, 50))
    t.rect(792, 115, 800, 117, (40, 60, 110))
    for k in range(1, 3):                                               # les ecrans de suivi, a cote de la baie
        x0 = 772 + k * 26
        t.rect(x0, 124, x0 + 24, 144, (40, 40, 48))
        t.rect(x0 + 2, 126, x0 + 22, 142, lum((30, 60, 50)))
        y = 138
        for xx in range(x0 + 3, x0 + 21, 3):
            ny = max(128, min(140, y + rng.randrange(-4, 4)))
            t.ligne([(xx, y), (xx + 3, ny)], lum(VERT))
            y = ny
    # --- le stock : le rayonnage, les sacs de Piccolo, les cartons, la palette, le rideau de fer
    t.rect(858, 70, 910, SOL_INT + 4, (60, 62, 70))
    for ey in (92, 118, 146):
        t.rect(856, ey, 912, ey + 3, (104, 108, 116))
    for ey in (92, 118):
        for k in range(3):
            x0 = 860 + k * 17
            t.poly([(x0, ey), (x0 + 2, ey - 16), (x0 + 13, ey - 16), (x0 + 15, ey)], (112, 104, 86))
            t.rect(x0 + 4, ey - 10, x0 + 11, ey - 6, (86, 140, 70))
    for k in range(3):
        x0 = 860 + k * 17
        t.rect(x0, 130, x0 + 15, 146, (124, 108, 88))
        t.ligne([(x0, 137), (x0 + 14, 137)], (90, 78, 64))
        t.rect(x0 + 4, 139, x0 + 11, 143, BLANC)
    for k in range(3):
        t.poly([(862 + k * 15, SOL_INT + 4), (865 + k * 15, 156), (877 + k * 15, 156), (880 + k * 15, SOL_INT + 4)], (112, 104, 86))
    t.rect(918, 60, 954, 132, (60, 62, 70))                             # le rideau de fer, vu de dedans
    for y in range(62, 132, 3):
        t.ligne([(918, y), (953, y)], (44, 46, 54))
    t.rect(918, 136, 954, 148, (176, 150, 60))
    t.grain(4, 322)
    return t.img


def epicerie():
    """Notre epicerie : les rayons, les frigos, le comptoir, et les petits qui tiennent la boutique."""
    rng = random.Random(331)
    t = piece(600, (150, 144, 128), (120, 112, 100), (96, 90, 82))
    _neons(t, (80, 240, 400), (250, 244, 224))
    for x0 in (60, 190):                                              # les rayons
        t.rect(x0, 66, x0 + 110, SOL_INT + 4, (70, 66, 64))
        for ey in range(84, SOL_INT, 22):
            t.rect(x0 + 2, ey, x0 + 108, ey + 2, (120, 116, 110))
            xx = x0 + 4
            while xx < x0 + 100:
                w, h = rng.randrange(4, 9), rng.randrange(8, 16)
                t.rect(xx, ey - h, xx + w, ey, rng.choice(((170, 160, 140), (128, 128, 136), (196, 190, 170), (110, 120, 110))))
                xx += w + 1
    for x0 in (330, 384):                                             # les frigos eclaires
        t.rect(x0, 70, x0 + 48, SOL_INT + 4, (60, 62, 70))
        t.rect(x0 + 3, 74, x0 + 45, SOL_INT - 2, BUREAU)
        for ey in range(96, SOL_INT - 4, 20):
            t.rect(x0 + 3, ey, x0 + 45, ey + 2, (110, 114, 122))
            for xx in range(x0 + 6, x0 + 42, 6):
                t.rect(xx, ey - 10, xx + 4, ey, rng.choice(((150, 160, 170), (120, 130, 150), (170, 170, 160))))
    t.rect(470, 136, 590, SOL_INT + 6, (96, 90, 84))                  # le comptoir, et ce qu'il y a dessous
    t.rect(466, 132, 594, 138, (176, 170, 158))
    t.rect(540, 116, 566, 132, (50, 52, 60))
    t.rect(480, 150, 520, 174, (40, 40, 48))
    t.rect(486, 156, 514, 160, VERT)
    t.grain(4, 332)
    return t.img


def garage():
    """L'atelier d'Omar : la RS6 sous sa bache, le pont, l'etabli ou traine le Flipper Zero orange."""
    rng = random.Random(341)
    t = piece(760, (104, 104, 110), (64, 64, 70))
    _neons(t, (80, 260, 440, 620))
    t.rect(60, 132, 200, 138, (150, 140, 120))                        # l'etabli
    t.rect(64, 138, 196, SOL_INT + 6, (80, 76, 72))
    t.rect(64, 70, 196, 124, (60, 62, 70))
    for k in range(12):
        t.ligne([(72 + k * 10, 76), (72 + k * 10, 76 + rng.randrange(14, 40))], (150, 154, 160))
    for k in range(4):                                                # piles de pneus
        t.ellipse(214, 168 - k * 12, 250, 186 - k * 12, (26, 26, 32))
        t.ellipse(224, 172 - k * 12, 240, 180 - k * 12, (60, 60, 68))
    # la RS6 sous sa bache
    t.poly([(290, SOL_INT + 8), (296, 150), (330, 120), (440, 118), (486, 146), (500, SOL_INT + 8)], (176, 176, 172))
    for xx in (320, 360, 400, 440, 470):
        t.ligne([(xx, 124), (xx + rng.randrange(-6, 7), SOL_INT + 4)], (128, 128, 126))
    t.ligne([(298, 152), (486, 150)], (140, 140, 138))
    for x0 in (540, 640):                                             # le pont elevateur
        t.rect(x0, 70, x0 + 8, SOL_INT + 6, (150, 130, 60))
    t.rect(536, 110, 652, 116, (120, 104, 50))
    t.poly([(552, 110), (560, 92), (632, 92), (640, 110)], (80, 82, 92))
    t.rect(690, 96, 750, SOL_INT + 4, (70, 66, 64))                   # le bureau d'Omar
    t.rect(696, 102, 744, 136, VITRE)
    t.texte(696, 84, "BUREAU", (60, 62, 70), 5)
    t.rect(700, 150, 740, 156, (150, 140, 120))
    t.grain(4, 342)
    return t.img


def bureaux():
    """L'open space de la Krousty : des postes a perte de vue, et une salle de reunion derriere une vitre."""
    rng = random.Random(351)
    t = piece(760, (120, 124, 132), (86, 88, 98), (72, 74, 84))
    _neons(t, range(60, 720, 110), BUREAU)
    for x in range(40, 760, 18):
        t.ligne([(x, PLAFOND), (x, 166)], (108, 112, 120))
    for x0 in range(60, 440, 76):                                     # les postes de travail
        t.rect(x0, 128, x0 + 66, SOL_INT + 6, (150, 154, 162))
        t.rect(x0, 128, x0 + 66, 132, (190, 194, 200))
        t.rect(x0 + 20, 104, x0 + 46, 124, (40, 42, 50))
        t.rect(x0 + 22, 106, x0 + 44, 122, BUREAU if rng.random() < 0.7 else (60, 64, 76))
        t.rect(x0 + 30, 124, x0 + 36, 128, (40, 42, 50))
        t.rect(x0 + 4, 150, x0 + 20, SOL_INT + 4, ton(ORANGE_S, 0.9))
    t.rect(470, 60, 660, SOL_INT, (60, 64, 76))                       # la salle de reunion vitree
    t.rect(474, 64, 656, SOL_INT - 2, (150, 166, 184))
    for x in (520, 566, 612):
        t.ligne([(x, 64), (x, SOL_INT - 3)], (60, 64, 76))
    t.rect(500, 150, 630, 156, (90, 94, 104))
    _silhouette(t, 556, (40, 42, 54), (120, 112, 106))                # Soul, derriere la vitre
    t.ligne([(476, 70), (530, 150)], (190, 204, 220))
    t.rect(690, 70, 744, SOL_INT, (150, 154, 162))                    # l'ascenseur
    t.ligne([(717, 72), (717, SOL_INT - 1)], (90, 94, 104))
    t.rect(748, 110, 752, 122, ORANGE)
    t.texte_centre(0, 46, 440, "KROUSTY CORP - CHAQUE TRIMESTRE COMPTE", (150, 154, 162), 3)
    t.grain(3, 352)
    return t.img


def hall_tour():
    """Le hall de la tour : l'accueil, l'ecran, les portiques, et l'ascenseur dont le dernier bouton dit URANUS."""
    t = piece(640, (58, 60, 72), (132, 134, 142), (110, 112, 120))
    _neons(t, range(60, 600, 120), BUREAU)
    for x in range(40, 640, 40):
        t.rect(x, PLAFOND, x + 3, 166, (72, 74, 88))
    t.rect(150, 60, 330, 112, (26, 24, 28))                           # l'ecran de l'accueil
    t.ellipse(170, 70, 200, 100, ORANGE)
    t.texte(180, 76, "K", (30, 24, 24), 5, 2)
    t.texte(212, 72, "BIENVENUE", ORANGE, 5, 2)
    t.texte(212, 90, "SUR TITAN", ORANGE, 5, 2)
    t.rect(160, 136, 320, SOL_INT + 6, (90, 92, 104))                 # la banque d'accueil
    t.rect(156, 132, 324, 138, (176, 180, 190))
    t.rect(160, 150, 320, 153, ORANGE)
    for x0 in (380, 420, 460):                                        # les portiques
        t.rect(x0, 146, x0 + 10, SOL_INT + 8, (150, 154, 162))
        t.rect(x0 + 2, 148, x0 + 8, 152, ORANGE)
    t.rect(530, 64, 610, SOL_INT, (150, 154, 162))                    # l'ascenseur
    t.ligne([(570, 66), (570, SOL_INT - 1)], (90, 94, 104))
    t.rect(616, 84, 630, 150, (40, 42, 50))
    for k in range(6):
        t.rect(620, 90 + k * 9, 626, 95 + k * 9, (150, 154, 162))
    t.rect(620, 86, 626, 89, ORANGE)
    t.texte(590, 52, "URANUS", ORANGE, 3)
    t.grain(3, 362)
    return t.img


# ---------------------------------------------------------------- les cartes paralleles
def metro():
    """La rame de l'intermission : un long couloir, des barres, des sieges, le tunnel aux fenetres."""
    rng = random.Random(371)
    t = piece(960, (150, 150, 144), (84, 84, 92), (70, 70, 78), porte=False)
    t.rect(0, PLAFOND, 960, 60, (176, 176, 170))
    for x in range(0, 960, 6):
        t.rect(x, PLAFOND - 6, x + 3, PLAFOND - 3, (240, 244, 236))
    for k, x0 in enumerate(range(20, 960, 160)):
        t.rect(x0, 70, x0 + 100, 126, FER)                             # les fenetres sur le tunnel
        t.rect(x0 + 3, 73, x0 + 97, 123, (14, 14, 20))
        t.ligne([(x0 + 3, 96 + (k % 3) * 4), (x0 + 96, 92 + (k % 3) * 4)], (60, 60, 70))
        t.rect(x0 + 6, 136, x0 + 94, 160, (96, 104, 150))              # les sieges
        t.rect(x0 + 6, 160, x0 + 94, 172, (70, 76, 112))
        for sx in range(x0 + 6, x0 + 94, 22):
            t.ligne([(sx, 136), (sx, 171)], (60, 64, 96))
        t.rect(x0 + 112, 66, x0 + 150, SOL_INT, (120, 120, 116))       # les portes
        t.rect(x0 + 116, 76, x0 + 129, 130, (14, 14, 20))
        t.rect(x0 + 133, 76, x0 + 146, 130, (14, 14, 20))
        t.ligne([(x0 + 131, 66), (x0 + 131, SOL_INT - 1)], FER)
        t.rect(x0 + 104, PLAFOND, x0 + 107, SOL_INT + 30, (196, 196, 190))  # les barres
    t.rect(380, 46, 580, 56, (40, 46, 70))
    t.texte_centre(380, 49, 200, "LIGNE 7 - LE KREMLIN-BICETRE", BLANC, 3)
    t.grain(3, 372)
    return t.img


def namek():
    """Namek, au lever du soleil : le seul moment chaud du jeu. Les plants de Piccolo, les montagnes, la RS6."""
    rng = random.Random(381)
    t = Toile(960, 270)
    for y in range(SOL_INT):
        k = y / SOL_INT
        t.rect(0, y, 960, y + 1, (int(226 + 26 * k), int(150 + 70 * k), int(84 + 60 * k), 255))
    t.ellipse(640, 96, 720, 176, (255, 240, 190))
    for base, c, amp in ((150, (176, 120, 84), 44), (164, (150, 98, 70), 30)):   # les montagnes
        points, x = [(0, SOL_INT)], 0
        while x < 960:
            points.append((x, base - rng.randrange(0, amp)))
            x += rng.randrange(30, 80)
        t.poly(points + [(960, base), (960, SOL_INT)], c)
    t.rect(0, SOL_INT - 4, 960, 270, (186, 140, 86))
    for y in range(SOL_INT, 270, 12):
        t.ligne([(0, y), (959, y)], (160, 118, 72))
    for x in range(40, 960, 26):                                       # les rangs de Piccolo
        for y in (SOL_INT + 2, SOL_INT + 30, SOL_INT + 62):
            if 380 < x < 560 and y == SOL_INT + 2:
                continue
            h = rng.randrange(14, 24)
            t.ligne([(x, y), (x, y - h)], (70, 110, 60), 2)
            for k in range(3):
                t.ellipse(x - 6, y - h + k * 4 - 3, x + 6, y - h + k * 4 + 5, (86, 140, 70) if k % 2 else (104, 160, 84))
    t.rect(770, 110, 860, SOL_INT, (214, 184, 140))                    # la baraque
    t.rect(764, 104, 866, 112, (150, 104, 70))
    t.rect(800, 136, 824, SOL_INT, (110, 76, 52))
    t.grain(4, 382)
    return t.img


def boucherie():
    """La boucherie de Mounir, a la sortie du metro Crimee : carrelage blanc, vitrine refrigeree, chambre froide."""
    rng = random.Random(391)
    t = piece(600, (176, 176, 170), (110, 108, 106), (92, 90, 90))
    for y in range(PLAFOND + 4, 166, 12):                              # le carrelage blanc du mur
        for x in range(40, 600, 12):
            t.rect(x, y, x + 11, y + 11, (186, 186, 180) if (x // 12 + y // 12) % 2 else (178, 178, 172))
    _neons(t, (100, 280, 460), (246, 248, 240))
    t.rect(60, 54, 250, 96, (40, 44, 40))                              # l'ardoise des prix
    t.rect(63, 57, 247, 93, (52, 58, 54))
    for k, (nom, prix) in enumerate((("POULET FERMIER", "6,90"), ("AGNEAU", "17,50"), ("BOEUF", "14,90"), ("MERGUEZ", "11,00"))):
        t.texte(70, 61 + k * 8, nom, (226, 226, 214), 3)
        t.texte(214, 61 + k * 8, prix, (240, 200, 120), 3)
    t.rect(66, 100, 248, 103, (110, 112, 120))                         # la barre des crochets
    for x in range(78, 240, 26):
        t.ligne([(x, 103), (x, 110)], (150, 150, 156))
        t.ellipse(x - 7, 110, x + 7, 132, (206, 176, 150))
        t.ellipse(x - 5, 114, x + 5, 128, (190, 156, 132))
    t.rect(260, 122, 470, SOL_INT + 6, (150, 150, 156))                # la vitrine refrigeree
    t.rect(264, 126, 466, 156, (40, 46, 54))
    t.poly([(264, 126), (466, 126), (452, 112), (278, 112)], (176, 196, 210))
    for k in range(9):
        x = 270 + k * 22
        c = rng.choice(((170, 90, 84), (190, 110, 100), (150, 70, 70), (206, 176, 150)))
        t.rect(x, 140, x + 18, 154, c)
        t.rect(x + 2, 141, x + 16, 143, ton(c, 1.15))
        t.rect(x + 4, 136, x + 14, 140, (226, 226, 220))               # l'etiquette
    t.rect(260, 156, 470, 160, (196, 196, 200))
    t.rect(480, 64, 560, SOL_INT + 4, (150, 156, 164))                 # la chambre froide
    t.rect(484, 68, 556, SOL_INT, (176, 182, 190))
    t.rect(540, 110, 552, 120, (90, 92, 100))
    t.texte(490, 76, "CHAMBRE", (60, 62, 70), 3)
    t.texte(490, 84, "FROIDE", (60, 62, 70), 3)
    t.rect(566, 90, 594, 130, (60, 62, 70))                            # le calendrier et le telephone
    t.rect(568, 92, 592, 106, (226, 226, 220))
    t.texte(570, 96, "2026", (170, 40, 40), 3)
    t.grain(3, 392)
    return t.img


def etage_anges():
    """L'etage sans numero : un open space sans fin, dans l'autre dimension. Ici, toutes les couleurs debordent."""
    import colorsys
    t = Toile(960, 270)
    px = t.img.load()
    for y in range(270):
        for x in range(960):
            teinte = (x / 520 + y / 400 + 0.08 * ((x // 24 + y // 18) % 2)) % 1.0
            clair = 0.62 if y < SOL_INT else 0.42 + 0.2 * ((x // 32 + (y - SOL_INT) // 16) % 2)
            r, g, b = colorsys.hsv_to_rgb(teinte, 0.5, clair if y > PLAFOND else 0.25)
            px[x, y] = (int(r * 255), int(g * 255), int(b * 255), 255)
    for x0 in range(30, 960, 84):                                      # des bureaux jusqu'a l'horizon, personne assis
        for rang, (y, k) in enumerate(((120, 0.5), (146, 0.75), (SOL_INT, 1.0))):
            w = int(54 * k)
            xx = x0 + rang * 14
            t.rect(xx, y - int(20 * k), xx + w, y, (236, 236, 240))
            t.rect(xx + w // 3, y - int(36 * k), xx + w * 2 // 3, y - int(22 * k), (250, 250, 255))
    t.rect(440, 150, 520, 156, (20, 20, 26))                           # la chaise et le formulaire de l'entretien
    t.rect(470, 132, 492, 150, (20, 20, 26))
    t.rect(452, 144, 468, 150, BLANC)
    t.rect(900, 96, 940, SOL_INT, (150, 150, 150))                     # l'escalier de service
    t.rect(904, 100, 936, SOL_INT - 2, (60, 60, 60))
    t.texte(898, 84, "URANUS", (20, 20, 26), 3)
    return t.img


def bureau_baba():
    """Le dernier etage : un tres grand bureau, tres simple. La ville tout autour, un homme de dos, un fauteuil vide."""
    rng = random.Random(391)
    t = piece(600, (34, 34, 44), (52, 50, 56), (44, 42, 48))
    t.rect(60, 52, 420, 168, (12, 12, 20))                             # la baie vitree et la ville
    x = 60
    while x < 420:
        w, h = rng.randrange(10, 26), rng.randrange(20, 80)
        t.rect(x, 168 - h, min(420, x + w), 168, (26, 26, 38))
        for _ in range(w * h // 40):
            t.rect(x + rng.randrange(w), 168 - h + rng.randrange(h), x + rng.randrange(w) + 1, 168 - h + rng.randrange(h) + 1, (130, 124, 104))
        x += w + 1
    for _ in range(40):
        t.point(60 + rng.randrange(360), 52 + rng.randrange(40), (150, 150, 170))
    for x in (60, 180, 300, 420):
        t.rect(x - 2, 52, x + 2, 168, (60, 62, 76))
    _silhouette(t, 232, (20, 20, 28), (20, 20, 28), h=60)              # Baba, de dos, les mains jointes
    t.rect(300, 146, 400, 152, (96, 90, 84))                           # le bureau et le fauteuil vide
    t.rect(306, 152, 394, SOL_INT + 6, (70, 66, 62))
    t.poly([(330, SOL_INT + 6), (332, 112), (368, 112), (370, SOL_INT + 6)], (40, 38, 44))
    t.rect(470, 56, 580, 150, (96, 82, 66))                            # son mur a lui : les memes photos, les memes fils
    points = []
    for _ in range(12):
        px_, py_ = rng.randrange(476, 562), rng.randrange(62, 132)
        t.rect(px_, py_, px_ + 12, py_ + 10, (190, 186, 176))
        points.append((px_ + 6, py_ + 1))
    for a, b in zip(points, points[1:]):
        t.ligne([a, b], (190, 44, 44))
    t.grain(3, 392)
    return t.img
