"""Le heros, tel qu'on le joue : casquette beige a monogramme et bande verte, rouge, verte ; petites lunettes noires ;
ecouteurs blancs a fil ; long t-shirt rouge delave ; mitaines noires ; pantalon large noir a empiecement rouge ;
grosses baskets blanches. Cheveux noirs mi-longs qui depassent de la casquette.

Tout est pose pixel par pixel, a la taille finale : rien n'est reduit, chaque detail tient sur ses propres pixels.
"""
import random

from PIL import Image

from outils import Toile, ton

PEAU, PEAU_O, PEAU_C = (216, 170, 136), (176, 128, 100), (236, 196, 162)
NOIR = (22, 20, 26)
TOILE, TOILE_O, MOTIF = (198, 172, 130), (160, 136, 100), (150, 122, 86)
VERT, ROUGE_BANDE, VISIERE = (34, 112, 66), (196, 40, 44), (96, 70, 46)
TEE, TEE_O, TEE_C, PALE = (178, 54, 50), (128, 34, 38), (206, 86, 76), (232, 176, 158)
PANT, PANT_O, CUIR = (40, 38, 46), (26, 24, 30), (128, 32, 38)
BASKET, SEMELLE = (220, 218, 212), (124, 124, 130)
VERRE, BLANC = (12, 12, 16), (240, 240, 236)


def _tete(t, ox=0, oy=0):
    """La tete de profil, tournee vers la droite : 15 pixels de haut."""
    r = lambda x0, y0, x1, y1, c: t.rect(x0 + ox, y0 + oy, x1 + ox, y1 + oy, c)
    p = lambda x, y, c: t.point(x + ox, y + oy, c)
    r(5, 7, 9, 16, NOIR)                                               # les cheveux sur la nuque
    r(8, 9, 15, 15, PEAU)                                              # le visage
    r(9, 15, 14, 16, PEAU_O)                                           # la machoire
    p(15, 12, PEAU)                                                    # le nez
    r(10, 16, 13, 17, PEAU_O)                                          # le cou
    t.poly([(6 + ox, 6 + oy), (7 + ox, 2 + oy), (10 + ox, 1 + oy), (13 + ox, 1 + oy), (15 + ox, 3 + oy), (15 + ox, 6 + oy)], TOILE)
    p(9, 3, MOTIF), p(12, 3, MOTIF), p(8, 5, MOTIF), p(11, 5, MOTIF), p(14, 5, MOTIF)   # le monogramme
    r(6, 2, 8, 6, TOILE_O)
    r(6, 6, 16, 7, VERT)                                               # la bande : verte, rouge, verte
    r(6, 7, 16, 8, ROUGE_BANDE)
    r(6, 8, 16, 9, VERT)
    r(14, 9, 20, 10, VISIERE)                                          # la visiere
    r(16, 10, 20, 11, ton(VISIERE, 0.7))
    r(9, 11, 12, 12, (70, 70, 78))                                     # la branche des lunettes
    r(12, 11, 15, 13, VERRE)                                           # le verre noir, etroit
    p(12, 11, (110, 114, 128))
    p(9, 12, BLANC)                                                    # l'ecouteur
    p(13, 14, (140, 90, 82))                                           # la bouche


def profil(pas=False, charge=False):
    """Le heros dans la rue, de profil : 22 x 48, comme tous les personnages du jeu."""
    t = Toile(22, 48)
    if pas:                                                            # les jambes : le pantalon large, l'empiecement rouge
        t.poly([(4, 32), (10, 32), (8, 44), (1, 44)], PANT_O)
        t.poly([(9, 32), (16, 32), (19, 44), (12, 44)], PANT)
        t.rect(11, 34, 16, 39, CUIR)
        for x0 in (0, 12):
            t.rect(x0, 44, x0 + 8, 47, BASKET)
            t.rect(x0 + 2, 45, x0 + 6, 46, (44, 44, 52))
            t.rect(x0, 47, x0 + 8, 48, SEMELLE)
    else:
        t.poly([(4, 32), (16, 32), (17, 44), (3, 44)], PANT)
        t.rect(3, 34, 6, 44, PANT_O)
        t.rect(9, 34, 15, 39, CUIR)
        t.rect(9, 39, 15, 40, ton(CUIR, 0.7))
        t.rect(3, 44, 18, 47, BASKET)
        t.rect(7, 45, 13, 46, (44, 44, 52))
        t.rect(15, 44, 18, 46, (190, 188, 184))
        t.rect(3, 47, 18, 48, SEMELLE)
    t.rect(4, 31, 16, 33, PANT_O)                                      # la ceinture cloutee
    t.point(7, 32, (170, 170, 178)), t.point(10, 32, (170, 170, 178)), t.point(13, 32, (170, 170, 178))
    t.poly([(5, 17), (14, 17), (16, 21), (16, 31), (4, 31), (4, 21)], TEE)       # le t-shirt rouge, long
    t.rect(4, 21, 6, 31, TEE_O)
    t.point(14, 22, PALE), t.point(15, 23, PALE), t.point(14, 24, PALE), t.point(15, 26, PALE)   # le motif
    _tete(t)
    for x, y in ((9, 13), (9, 14), (10, 15), (10, 17), (11, 19), (11, 21), (12, 23), (12, 25)):  # le fil des ecouteurs
        t.point(x, y, BLANC)
    if charge:                                                         # les caisses de viande, portees a deux bras
        t.rect(8, 17, 22, 29, (170, 160, 142))
        t.rect(8, 17, 22, 19, (196, 188, 170))
        t.ligne([(8, 23), (21, 23)], (130, 122, 108))
        t.rect(11, 20, 15, 22, (180, 90, 80))
        t.rect(9, 29, 18, 31, TEE_C)
        t.rect(18, 27, 21, 31, NOIR)
    else:
        t.rect(8, 19, 12, 30, TEE_C)                                   # le bras, le long du corps
        t.rect(11, 19, 12, 30, TEE)
        t.rect(8, 29, 12, 30, TEE_O)
        t.rect(8, 30, 12, 32, NOIR)                                    # la mitaine
        t.rect(9, 32, 12, 33, PEAU)                                    # le bout des doigts
    return t.img


def assis(cigarette=True):
    """Affale sur le canape, de profil, les jambes allongees, une cigarette entre les doigts (toile de 34 x 40)."""
    t = Toile(34, 40)
    t.poly([(4, 16), (13, 16), (16, 30), (4, 30)], TEE)                # le buste, en arriere
    t.rect(4, 20, 6, 30, TEE_O)
    t.poly([(8, 27), (24, 28), (27, 31), (27, 36), (22, 36), (20, 33), (8, 33)], PANT)   # les jambes, tendues
    t.rect(12, 29, 18, 32, CUIR)
    t.rect(22, 36, 31, 39, BASKET)
    t.rect(22, 39, 31, 40, SEMELLE)
    t.ligne([(11, 20), (19, 25)], TEE_C, 2)                            # le bras, pose sur la cuisse
    t.rect(19, 24, 22, 27, NOIR)
    if cigarette:
        t.rect(22, 25, 25, 26, (236, 236, 230))
        t.point(25, 25, (255, 140, 40, 254))                           # la braise
    _tete(t, 0, -1)
    return t.img


def sur_scooter(t, pas=False):
    """Le heros assis sur le scooter (toile de 46 x 48)."""
    t.poly([(14, 13), (21, 13), (24, 26), (12, 26)], TEE)
    t.rect(12, 20, 15, 26, TEE_O)
    t.poly([(18, 24), (28, 26), (30, 33), (26, 34), (24, 29), (16, 28)], PANT)
    t.rect(21, 26, 26, 29, CUIR)
    d = 1 if pas else 0
    t.rect(26, 32 + d, 32, 35 + d, BASKET)
    t.rect(26, 35 + d, 32, 36 + d, SEMELLE)
    t.ligne([(21, 16), (31, 13)], TEE_C, 2)
    t.rect(30, 11, 33, 14, NOIR)                                       # la mitaine sur le guidon
    _tete(t, 8, -4)


def _bords(img, k=0.74):
    """Pas de contour noir : la derniere rangee de chaque bord est un ton plus sombre."""
    src = img.copy()
    l, h = img.size
    for y in range(h):
        for x in range(l):
            r, g, b, a = src.getpixel((x, y))
            if a and any(not (0 <= x + dx < l and 0 <= y + dy < h) or src.getpixel((x + dx, y + dy))[3] == 0
                         for dx, dy in ((1, 0), (-1, 0), (0, -1)) if 0 <= y + dy):
                if 0 <= x - 1 and x + 1 < l and src.getpixel((x - 1, y))[3] and src.getpixel((x + 1, y))[3] and y > 0 \
                        and src.getpixel((x, y - 1))[3]:
                    continue
                img.putpixel((x, y), (int(r * k), int(g * k), int(b * (k + 0.03)), 255))
    return img


def buste():
    """Le portrait des dialogues, dessine directement en 96 x 112 (les autres bustes sont des 48 x 56 agrandis) :
    deux fois plus de details pour le personnage qu'on voit le plus."""
    t = Toile(96, 112)
    rng = random.Random(17)
    # --- le t-shirt : rouge delave, epaules tombantes
    t.poly([(6, 112), (12, 90), (30, 79), (66, 79), (84, 90), (90, 112)], TEE)
    t.poly([(6, 112), (12, 90), (30, 79), (35, 79), (22, 92), (17, 112)], TEE_C)
    t.poly([(90, 112), (84, 90), (66, 79), (60, 79), (74, 92), (79, 112)], TEE_O)
    for _ in range(170):                                               # le delave : de petites taches plus claires ou plus sombres
        x, y = rng.randrange(8, 88), rng.randrange(82, 112)
        if t.img.getpixel((x, y))[3]:
            r, g, b, _a = t.img.getpixel((x, y))
            k = rng.choice((0.9, 0.94, 1.07, 1.1))
            t.point(x, y, (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k))))
    for x0, x1, y in ((20, 26, 96), (70, 76, 97), (24, 30, 104)):      # les plis de l'epaule
        t.ligne([(x0, y), (x1, y + 6)], TEE_O)
    # --- le cou
    t.rect(39, 64, 57, 84, PEAU)
    t.rect(50, 64, 57, 84, PEAU_O)
    t.rect(39, 70, 57, 74, PEAU_O)                                     # l'ombre du menton
    t.poly([(34, 79), (62, 79), (58, 87), (48, 90), (38, 87)], PEAU)   # l'encolure
    t.poly([(52, 79), (62, 79), (58, 87), (50, 90)], PEAU_O)
    t.ligne([(33, 79), (38, 88), (48, 91), (58, 88), (63, 79)], TEE_O, 2)
    t.ligne([(33, 81), (38, 90), (48, 93), (58, 90), (63, 81)], TEE_C)
    # --- le dragon, pale, en travers de la poitrine, et deux lignes de texte
    corps = [(24, 101), (28, 98), (33, 100), (38, 103), (43, 101), (48, 98), (53, 100), (58, 103), (63, 101), (68, 98), (72, 100)]
    t.ligne(corps, PALE, 2)
    for x, y in corps[1:-1]:                                           # les ecailles et la crete
        t.point(x, y - 2, PALE)
        t.point(x + 2, y + 2, ton(PALE, 0.8))
    t.poly([(18, 99), (24, 97), (25, 102), (20, 104)], PALE)           # la tete, la gueule ouverte
    t.point(21, 99, TEE_O)
    t.ligne([(18, 98), (15, 95)], PALE)
    t.ligne([(19, 104), (16, 106)], PALE)
    t.ligne([(72, 100), (77, 97), (79, 99)], PALE)                     # la queue
    for x in range(26, 70, 2):
        if x % 10 != 6:
            t.point(x, 108, PALE)
    for x in range(30, 66, 2):
        if x % 8 != 4:
            t.point(x, 111, ton(PALE, 0.85))
    # --- les cheveux derriere le visage, mi-longs
    t.poly([(21, 34), (29, 32), (31, 46), (30, 62), (27, 72), (22, 68)], NOIR)
    t.poly([(75, 34), (67, 32), (65, 46), (66, 62), (69, 72), (74, 68)], ton(NOIR, 0.7))
    t.ligne([(24, 40), (24, 64)], (62, 56, 70))
    t.ligne([(27, 44), (26, 68)], (44, 40, 50))
    t.ligne([(71, 42), (71, 66)], (40, 36, 46))
    # --- le visage
    t.ellipse(27, 20, 70, 75, PEAU_O)
    t.ellipse(27, 20, 62, 74, PEAU)
    t.poly([(30, 56), (66, 56), (57, 77), (40, 77)], PEAU)             # la machoire, anguleuse
    t.poly([(59, 56), (66, 56), (57, 77), (52, 77), (56, 66)], PEAU_O)
    t.ellipse(31, 46, 40, 58, PEAU_C)                                  # la pommette dans la lumiere
    t.ellipse(22, 44, 30, 58, PEAU)                                    # les oreilles
    t.ellipse(67, 44, 75, 58, PEAU_O)
    t.rect(46, 50, 50, 61, PEAU_O)                                     # le nez
    t.rect(45, 49, 46, 59, PEAU_C)
    t.rect(44, 61, 52, 63, ton(PEAU, 0.66))
    t.rect(40, 67, 57, 69, (128, 82, 76))                              # la bouche, fermee, un coin plus bas
    t.rect(43, 69, 54, 70, PEAU_O)
    t.point(39, 68, (128, 82, 76)), t.point(57, 69, (128, 82, 76))
    t.rect(44, 73, 53, 74, PEAU_O)                                     # le menton
    # --- les ecouteurs blancs et leurs fils
    t.rect(24, 49, 28, 54, BLANC)
    t.rect(25, 50, 27, 53, (180, 180, 184))
    t.rect(69, 49, 73, 54, (206, 206, 206))
    t.ligne([(26, 55), (28, 70), (36, 88), (44, 112)], BLANC)
    t.ligne([(71, 55), (69, 70), (60, 88), (51, 112)], (200, 200, 204))
    # --- la casquette : toile a monogramme, bande verte, rouge, verte, visiere brune
    t.poly([(21, 34), (22, 20), (29, 9), (41, 3), (55, 3), (67, 9), (74, 20), (75, 34)], TOILE)
    t.poly([(62, 6), (67, 9), (74, 20), (75, 34), (64, 34)], TOILE_O)
    for y in range(9, 25, 7):                                          # le monogramme : de petits losanges ton sur ton, espaces
        for x in range(27 + (y // 7 % 2) * 5, 72, 10):
            if t.img.getpixel((x, y))[:3] in (TOILE, TOILE_O):
                for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                    t.point(x + dx, y + dy, ton(TOILE, 0.86))
    t.ligne([(48, 3), (36, 26)], TOILE_O)                              # les coutures des panneaux
    t.ligne([(48, 3), (60, 26)], ton(TOILE_O, 0.9))
    t.rect(45, 1, 52, 4, TOILE_O)                                      # le bouton
    t.rect(21, 26, 76, 29, VERT)
    t.rect(21, 29, 76, 32, ROUGE_BANDE)
    t.rect(21, 32, 76, 35, VERT)
    t.rect(64, 26, 76, 35, ton(VERT, 0.75))
    t.rect(64, 29, 76, 32, ton(ROUGE_BANDE, 0.75))
    t.poly([(21, 35), (75, 35), (75, 38), (66, 43), (30, 43), (21, 38)], VISIERE)   # la visiere courbe, vue de face
    t.ligne([(22, 36), (74, 36)], ton(VISIERE, 1.3))
    t.poly([(26, 41), (30, 43), (66, 43), (70, 41)], ton(VISIERE, 0.62))
    t.rect(29, 43, 68, 46, ton(PEAU, 0.72))                            # l'ombre de la visiere sur le front
    # --- les sourcils, puis les petites lunettes noires ovales
    t.ligne([(32, 44), (44, 46)], NOIR, 2)
    t.ligne([(53, 46), (65, 44)], NOIR, 2)
    for x0 in (31, 51):
        t.ellipse(x0 + 1, 48, x0 + 15, 54, VERRE)
        t.ligne([(x0 + 3, 49), (x0 + 8, 49)], (120, 126, 144))         # le reflet
    t.rect(46, 50, 52, 52, (86, 86, 96))                               # le pont
    t.ligne([(31, 50), (24, 48)], (86, 86, 96))                        # les branches
    t.ligne([(66, 50), (73, 48)], (60, 60, 68))
    return _bords(t.img)
