"""Les personnages du mode histoire, dans le trait du gabarit du heros.

Trois choses par personnage :
- son sprite debout, 22 x 48, vu de face, et un second dessin, jambes ecartees, pour la marche (suffixe _2) ;
- son buste, dessine a part en 48 x 56 puis agrandi deux fois : c'est lui qu'on voit pendant les dialogues ;
- au besoin, des poses (assis, de profil, en scooter, charge).
Le heros, lui, est de profil dans le decor : ses dessins sont faits ici aussi (heros, heros_2, heros_charge...).
"""
from PIL import Image

from outils import *

PEAU = (150, 140, 132)
PEAU_MATE = (128, 108, 96)
PEAU_FONCEE = (104, 86, 76)
PEAU_CLAIRE = (164, 146, 134)
NOIR = (24, 24, 30)
ROUGE_ECHARPE = (200, 40, 40)


# ---------------------------------------------------------------- les sprites debout
def perso(manteau, peau=PEAU, cheveux=NOIR, coiffe="court", detail=None, taille=48, jambes=NOIR, pas=False):
    """Un personnage de face. pas : le dessin de marche, une jambe en avant."""
    t = Toile(22, 48)
    d = 48 - taille
    clair = ton(manteau, 1.35)
    t.poly([(7, 12 + d), (15, 12 + d), (18, 20 + d), (19, 44), (3, 44), (4, 20 + d)], manteau)
    if pas:
        t.rect(4, 43, 8, 47, jambes)
        t.rect(14, 43, 18, 48, jambes)
        t.rect(3, 47, 8, 48, jambes)
    else:
        t.rect(6, 43, 10, 48, jambes)
        t.rect(12, 43, 16, 48, jambes)
    t.ligne([(4, 20 + d), (3, 43)], clair)
    t.ligne([(7, 12 + d), (4, 20 + d)], clair)
    t.ellipse(6, 1 + d, 16, 12 + d, peau)
    t.rect(8, 6 + d, 10, 7 + d, NOIR)
    t.rect(12, 6 + d, 14, 7 + d, NOIR)
    if coiffe == "court":
        t.poly([(6, 5 + d), (7, 1 + d), (15, 1 + d), (16, 5 + d), (14, 3 + d), (8, 3 + d)], cheveux)
    elif coiffe == "long":
        t.poly([(5, 13 + d), (5, 4 + d), (8, d), (14, d), (17, 4 + d), (17, 13 + d), (15, 12 + d), (15, 4 + d), (7, 4 + d), (7, 12 + d)],
               cheveux)
    elif coiffe == "casquette":
        t.rect(6, 1 + d, 16, 4 + d, cheveux)
        t.rect(5, 4 + d, 18, 5 + d, cheveux)
    elif coiffe == "capuche":
        t.poly([(5, 13 + d), (4, 5 + d), (8, d), (14, d), (18, 5 + d), (17, 13 + d), (15, 11 + d), (15, 4 + d), (7, 4 + d), (7, 11 + d)],
               manteau)
        t.ligne([(4, 5 + d), (8, d), (14, d), (18, 5 + d)], clair)
    elif coiffe == "bonnet":
        t.rect(6, d, 16, 4 + d, cheveux)
        t.rect(6, 4 + d, 16, 5 + d, ton(cheveux, 1.5))
    elif coiffe == "kepi":
        t.rect(6, d, 16, 3 + d, cheveux)
        t.rect(5, 3 + d, 17, 5 + d, ton(cheveux, 0.7))
        t.rect(10, 1 + d, 12, 2 + d, (190, 190, 184))
    elif coiffe == "gris":
        t.poly([(6, 6 + d), (7, 2 + d), (15, 2 + d), (16, 6 + d), (14, 4 + d), (8, 4 + d)], cheveux)
    elif coiffe == "ras":
        t.rect(7, 1 + d, 15, 3 + d, cheveux)
    if detail == "blouse":
        t.rect(10, 13 + d, 12, 43, (84, 84, 92))
        t.rect(7, 6 + d, 15, 7 + d, (40, 40, 48))
        t.rect(8, 6 + d, 10, 7 + d, (150, 190, 170))
        t.rect(12, 6 + d, 14, 7 + d, (150, 190, 170))
        t.rect(6, 28, 9, 32, (190, 190, 184))
    elif detail == "costume":
        t.poly([(9, 12 + d), (13, 12 + d), (11, 22 + d)], (190, 190, 196))
        t.rect(10, 13 + d, 12, 23 + d, (40, 44, 60))
    elif detail == "echarpe":
        t.rect(6, 11 + d, 16, 14 + d, ROUGE_ECHARPE)
        t.rect(13, 14 + d, 16, 26 + d, ROUGE_ECHARPE)
        t.ligne([(8, 14 + d), (10, 24 + d)], (200, 110, 30))           # le cordon orange d'un badge, rentre sous le manteau
    elif detail == "gilet":
        t.rect(5, 22 + d, 17, 24 + d, (190, 190, 184))
        t.rect(5, 30, 17, 32, (190, 190, 184))
        t.rect(15, 15 + d, 18, 19 + d, (190, 60, 50))
    elif detail == "sac":
        t.rect(17, 30, 22, 40, (170, 160, 140))
        t.ligne([(18, 26), (19, 30)], (120, 112, 100))
    elif detail == "doudoune":
        for y in range(16 + d, 40, 5):
            t.ligne([(5, y), (17, y)], ton(manteau, 0.7))
    elif detail == "bleu":
        t.rect(8, 14 + d, 14, 20 + d, ton(manteau, 0.7))
        t.rect(6, 30, 9, 33, NOIR)
    elif detail == "tablier":
        t.poly([(7, 18 + d), (15, 18 + d), (16, 42), (6, 42)], (210, 208, 200))
        t.ligne([(7, 18 + d), (9, 13 + d)], (210, 208, 200))
        t.ligne([(15, 18 + d), (13, 13 + d)], (210, 208, 200))
        t.rect(9, 9 + d, 13, 10 + d, ton(peau, 0.6))
    elif detail == "moustache":
        t.rect(9, 9 + d, 13, 10 + d, ton(peau, 0.55))
    elif detail == "ecouteurs":
        t.rect(5, 5 + d, 7, 8 + d, (60, 62, 70))
        t.rect(15, 5 + d, 17, 8 + d, (60, 62, 70))
        t.ligne([(6, 8 + d), (9, 20 + d)], (190, 190, 196))
    elif detail == "canne":
        t.ligne([(19, 28), (20, 48)], (90, 76, 60), 2)
    return t.img


def assis(manteau, peau, coiffe_couleur, detail=None):
    """Un personnage assis sur un banc, une jambe tendue : Youngssef et sa cheville."""
    t = Toile(26, 40)
    clair = ton(manteau, 1.35)
    t.poly([(7, 10), (15, 10), (18, 17), (18, 30), (4, 30), (4, 17)], manteau)
    t.ligne([(4, 17), (4, 29)], clair)
    t.rect(4, 28, 12, 33, NOIR)                                       # la cuisse sur le banc
    t.rect(8, 33, 11, 40, NOIR)                                       # une jambe pliee
    t.rect(12, 28, 25, 32, NOIR)                                      # l'autre tendue
    t.rect(22, 30, 26, 34, (190, 190, 184))                           # la cheville bandee
    t.ellipse(6, 0, 16, 11, peau)
    t.poly([(5, 11), (4, 4), (8, -1), (14, -1), (18, 4), (17, 11), (15, 9), (15, 3), (7, 3), (7, 9)], manteau)
    t.rect(8, 5, 10, 6, NOIR)
    t.rect(12, 5, 14, 6, NOIR)
    if detail == "telephone":
        t.rect(15, 4, 18, 10, (40, 40, 48))
        t.rect(16, 5, 17, 8, lum((150, 200, 230)))
    for y in range(15, 29, 5):
        t.ligne([(5, y), (17, y)], ton(manteau, 0.7))
    return t.img


# ---------------------------------------------------------------- le heros, de profil
def heros_profil(pas=False, charge=False):
    """Le heros vu de cote, comme dans les rues : son dessin est dans heros.py."""
    import heros
    return heros.profil(pas, charge)


def scooter_profil(caisse=(206, 204, 196), rideur=False, pas=False):
    """Le scooter de Youngssef vu de cote : un 50 cm3 gris fonce, une caisse de livraison a l'arriere."""
    t = Toile(46, 48)
    carros, carros_c = (70, 72, 84), (100, 102, 114)
    for x in (8, 37):                                                  # les roues
        t.ellipse(x - 6, 36, x + 6, 48, NOIR)
        t.ellipse(x - 3, 39, x + 3, 45, (110, 112, 120))
    t.poly([(8, 40), (16, 34), (30, 34), (36, 40)], carros)            # le plancher
    t.poly([(30, 34), (34, 20), (38, 20), (40, 34), (37, 40)], carros_c)  # le tablier avant
    t.ligne([(36, 20), (34, 12)], (40, 40, 48), 2)                     # la colonne de direction
    t.rect(31, 11, 39, 13, (40, 40, 48))                               # le guidon
    t.rect(38, 22, 41, 25, lum((240, 240, 226)))                       # le phare
    t.poly([(4, 34), (6, 28), (22, 28), (24, 34)], carros)             # la coque arriere
    t.rect(8, 25, 22, 28, (36, 36, 44))                                # la selle
    t.rect(2, 32, 5, 34, lum((200, 60, 50)))                           # le feu arriere
    t.rect(1, 13, 13, 26, caisse)                                      # la caisse
    t.rect(1, 13, 13, 15, ton(caisse, 0.8))
    t.rect(4, 18, 10, 22, (232, 100, 8))
    if rideur:                                                         # le heros assis dessus
        import heros
        heros.sur_scooter(t, pas)
    return t.img


# ---------------------------------------------------------------- les bustes des dialogues
def buste(peau, coiffe, cheveux, vetement, couleur, details=(), regard=0):
    """Un buste de 48 x 56, agrandi deux fois. Lumiere par la gauche, comme celle d'un ecran dans la nuit.
    coiffe : herisse, court, ras, crane, long, chignon, capuche, casquette, bonnet, kepi, gris.
    vetement : sweat, doudoune, costume, blouse, tablier, bleu, manteau, veste, gilet.
    details : moustache, barbe, lunettes, cicatrice, echarpe, badge, ecouteurs, cernes, telephone."""
    t = Toile(48, 56)
    ombre_peau = ton(peau, 0.78)
    clair_peau = ton(peau, 1.1)
    c_o, c_c = ton(couleur, 0.72), ton(couleur, 1.28)
    # le corps
    t.poly([(4, 56), (7, 44), (15, 39), (33, 39), (41, 44), (44, 56)], couleur)
    t.poly([(4, 56), (7, 44), (15, 39), (17, 39), (11, 46), (9, 56)], c_c)
    t.poly([(44, 56), (41, 44), (33, 39), (31, 39), (37, 46), (40, 56)], c_o)
    t.rect(20, 33, 28, 41, ombre_peau)                                 # le cou
    t.rect(20, 33, 23, 41, peau)
    if vetement == "sweat":
        t.poly([(16, 39), (24, 46), (32, 39)], c_o)
        t.ligne([(21, 42), (21, 50)], (150, 150, 156))
        t.ligne([(27, 42), (27, 50)], (150, 150, 156))
    elif vetement == "doudoune":
        for y in (44, 49, 54):
            t.ligne([(6, y), (42, y)], c_o)
        t.rect(17, 37, 31, 41, couleur)
        t.ligne([(24, 41), (24, 56)], c_o)
    elif vetement == "costume":
        t.poly([(18, 39), (24, 50), (30, 39)], (200, 200, 206))
        t.poly([(22, 40), (26, 40), (25, 52), (24, 54), (23, 52)], (40, 46, 66))
        t.poly([(15, 39), (24, 54), (18, 56), (12, 44)], c_c)
        t.poly([(33, 39), (24, 54), (30, 56), (36, 44)], c_o)
    elif vetement == "blouse":
        t.poly([(4, 56), (7, 44), (15, 39), (33, 39), (41, 44), (44, 56)], (214, 214, 208))
        t.poly([(44, 56), (41, 44), (33, 39), (31, 39), (37, 46), (40, 56)], (176, 176, 172))
        t.poly([(17, 39), (24, 48), (31, 39), (28, 39), (24, 44), (20, 39)], (90, 96, 110))
        t.poly([(15, 39), (22, 50), (19, 56), (12, 44)], (232, 232, 226))
        t.rect(33, 48, 38, 50, (60, 66, 80))
    elif vetement == "tablier":
        t.poly([(13, 44), (35, 44), (37, 56), (11, 56)], (214, 212, 204))
        t.ligne([(13, 44), (17, 39)], (214, 212, 204), 2)
        t.ligne([(35, 44), (31, 39)], (214, 212, 204), 2)
        t.rect(20, 50, 24, 53, (150, 110, 100))
    elif vetement == "bleu":
        t.poly([(14, 44), (34, 44), (36, 56), (12, 56)], ton(couleur, 0.88))
        t.rect(15, 46, 20, 50, c_o)
        t.rect(16, 52, 19, 54, NOIR)
        t.ligne([(17, 39), (14, 44)], c_c, 2)
    elif vetement == "manteau":
        t.ligne([(24, 41), (24, 56)], c_o)
        t.poly([(15, 39), (22, 47), (20, 56), (12, 46)], c_c)
    elif vetement == "veste":
        t.poly([(18, 39), (24, 47), (30, 39)], (60, 62, 70))
        t.ligne([(24, 47), (24, 56)], c_o)
    elif vetement == "gilet":
        t.rect(6, 47, 42, 50, (190, 190, 184))
        t.rect(32, 41, 38, 45, (190, 60, 50))
    # la tete
    t.ellipse(13, 9, 35, 37, peau)
    t.ellipse(25, 11, 35, 36, ombre_peau)
    t.ellipse(13, 9, 30, 36, peau)
    t.ellipse(11, 20, 16, 28, peau)                                    # les oreilles
    t.ellipse(32, 20, 37, 28, ombre_peau)
    t.ellipse(15, 13, 21, 19, clair_peau)                              # le front qui prend la lumiere
    yeux = 23
    for x0 in (17, 26):                                                # les yeux
        t.rect(x0, yeux, x0 + 5, yeux + 2, (210, 206, 200))
        t.rect(x0 + 2 + regard, yeux, x0 + 4 + regard, yeux + 2, NOIR)
        t.rect(x0, yeux - 1, x0 + 5, yeux, ton(peau, 0.55))
    t.rect(16, yeux - 4, 22, yeux - 3, ton(cheveux, 0.9) if coiffe not in ("crane",) else ton(peau, 0.6))
    t.rect(26, yeux - 4, 32, yeux - 3, ton(cheveux, 0.9) if coiffe not in ("crane",) else ton(peau, 0.6))
    t.rect(23, 25, 25, 30, ombre_peau)                                 # le nez
    t.rect(22, 30, 25, 31, ton(peau, 0.65))
    t.rect(20, 33, 28, 34, ton(peau, 0.6))                             # la bouche
    # les cheveux
    if coiffe == "herisse":
        t.poly([(12, 22), (12, 13), (15, 6), (17, 10), (20, 3), (23, 8), (26, 2), (28, 8), (32, 4), (33, 11), (36, 9),
                (36, 22), (33, 15), (15, 15)], cheveux)
    elif coiffe == "court":
        t.poly([(12, 22), (12, 14), (16, 9), (32, 9), (36, 14), (36, 22), (33, 15), (15, 15)], cheveux)
    elif coiffe == "ras":
        t.poly([(13, 18), (14, 12), (18, 9), (30, 9), (34, 12), (35, 18), (32, 14), (16, 14)], ton(cheveux, 1.0))
    elif coiffe == "crane":
        t.ellipse(16, 11, 24, 15, clair_peau)
        t.rect(12, 20, 14, 26, ton(cheveux, 1.0))
        t.rect(34, 20, 36, 26, ton(cheveux, 1.0))
    elif coiffe == "gris":
        t.poly([(12, 24), (12, 15), (16, 10), (32, 10), (36, 15), (36, 24), (33, 16), (25, 14), (15, 16)], cheveux)
    elif coiffe == "long":
        t.poly([(10, 44), (10, 18), (13, 11), (19, 7), (29, 7), (35, 11), (38, 18), (38, 44), (34, 42), (34, 18), (30, 13),
                (18, 13), (14, 18), (14, 42)], cheveux)
    elif coiffe == "chignon":
        t.ellipse(18, 2, 30, 11, cheveux)
        t.poly([(12, 24), (12, 15), (16, 9), (32, 9), (36, 15), (36, 24), (33, 16), (15, 16)], cheveux)
    elif coiffe == "capuche":
        t.poly([(8, 42), (8, 18), (12, 8), (20, 3), (28, 3), (36, 8), (40, 18), (40, 42), (35, 40), (35, 18), (31, 11),
                (17, 11), (13, 18), (13, 40)], couleur)
        t.ligne([(13, 40), (13, 18), (17, 11), (31, 11), (35, 18), (35, 40)], c_o)
        t.poly([(13, 18), (17, 12), (31, 12), (35, 18), (30, 15), (18, 15)], cheveux)
    elif coiffe == "casquette":
        t.poly([(12, 20), (12, 12), (17, 7), (31, 7), (36, 12), (36, 20), (33, 16), (15, 16)], cheveux)
        t.rect(10, 16, 30, 19, ton(cheveux, 0.7))
    elif coiffe == "bonnet":
        t.poly([(11, 19), (12, 10), (18, 5), (30, 5), (36, 10), (37, 19)], cheveux)
        t.rect(11, 16, 37, 20, ton(cheveux, 1.4))
    elif coiffe == "kepi":
        t.rect(12, 7, 36, 16, cheveux)
        t.rect(10, 15, 34, 18, ton(cheveux, 0.6))
        t.rect(22, 10, 26, 13, (196, 190, 160))
    # les details
    for detail in details:
        if detail == "moustache":
            t.poly([(18, 31), (30, 31), (29, 33), (19, 33)], ton(cheveux, 1.0))
        elif detail == "barbe":
            for x in range(15, 34, 2):
                for y in range(28, 37, 2):
                    if (x - 24) ** 2 / 100 + (y - 30) ** 2 / 50 < 1.3 and not 20 <= x <= 28 or y > 34:
                        t.point(x, y, ton(peau, 0.62))
        elif detail == "lunettes":
            t.rect(16, 22, 23, 26, (40, 40, 48))
            t.rect(25, 22, 32, 26, (40, 40, 48))
            t.rect(17, 23, 22, 25, (150, 190, 176))
            t.rect(26, 23, 31, 25, (150, 190, 176))
            t.rect(23, 23, 25, 24, (40, 40, 48))
        elif detail == "cicatrice":
            t.ligne([(19, 12), (30, 17)], ton(peau, 0.5))
            for x in range(20, 30, 3):
                t.ligne([(x, 12 + (x - 19) // 2), (x + 1, 16 + (x - 19) // 2)], ton(peau, 0.5))
        elif detail == "echarpe":
            t.poly([(13, 36), (35, 36), (37, 42), (11, 42)], ROUGE_ECHARPE)
            t.poly([(29, 42), (35, 42), (34, 54), (29, 54)], ton(ROUGE_ECHARPE, 0.85))
            t.ligne([(13, 39), (35, 39)], ton(ROUGE_ECHARPE, 0.75))
        elif detail == "badge":                                          # un cordon orange qui disparait sous le manteau
            t.ligne([(18, 42), (21, 52)], (200, 110, 30))
            t.ligne([(19, 42), (22, 52)], (170, 90, 26))
        elif detail == "ecouteurs":
            t.rect(10, 21, 14, 27, (60, 62, 70))
            t.ligne([(12, 27), (16, 44)], (190, 190, 196))
        elif detail == "cernes":
            t.rect(17, 26, 22, 27, ton(peau, 0.68))
            t.rect(26, 26, 31, 27, ton(peau, 0.68))
        elif detail == "telephone":
            t.rect(34, 18, 40, 30, (40, 40, 48))
            t.rect(35, 19, 39, 27, lum((150, 200, 230)))
            t.ellipse(36, 28, 44, 36, peau)
    out = t.img.resize((96, 112), Image.NEAREST)
    return out


def buste_geto(sprite):
    """Old Geto a deja son vrai dessin : son buste est un recadrage de son sprite, agrandi."""
    return sprite.crop((8, 0, 64, 62)).resize((112, 124), Image.NEAREST)


def buste_sdf():
    """Le SDF parle peu et ne leve pas la tete : un buste dans l'ombre de sa capuche."""
    return buste((122, 112, 104), "capuche", NOIR, "manteau", (72, 70, 74), ("barbe", "cernes"), regard=-1)


# ---------------------------------------------------------------- les petites choses de l'histoire
def alien(pas=False):
    t = Toile(18, 26)
    t.ellipse(2, 0, 16, 13, lum((120, 230, 150)))
    t.ellipse(4, 4, 8, 9, NOIR)
    t.ellipse(10, 4, 14, 9, NOIR)
    t.poly([(6, 12), (12, 12), (14, 22), (4, 22)], lum((90, 200, 130)))
    bras = 4 if pas else 0
    t.ligne([(5, 14), (0, 9 + bras)], lum((90, 200, 130)))
    t.ligne([(13, 14), (17, 9 + 4 - bras)], lum((90, 200, 130)))
    t.rect(5 - (1 if pas else 0), 22, 8, 26, lum((70, 170, 110)))
    t.rect(10, 22, 13 + (1 if pas else 0), 26, lum((70, 170, 110)))
    return t.img


def sac(c=(206, 204, 196), brille=False):
    t = Toile(14, 12)
    t.rect(1, 3, 13, 12, c)
    t.rect(3, 0, 11, 3, ton(c, 0.7))
    t.rect(4, 6, 10, 9, lum(VERT) if brille else (232, 100, 8))
    return t.img


def caisses():
    """Les caisses de poulet de Mounir, empilees par terre."""
    t = Toile(34, 22)
    for x, y in ((0, 10), (16, 10), (8, 0)):
        t.rect(x, y, x + 17, y + 12, (170, 160, 142))
        t.rect(x, y, x + 17, y + 2, (196, 188, 170))
        t.ligne([(x, y + 6), (x + 16, y + 6)], (130, 122, 108))
        t.rect(x + 3, y + 3, x + 8, y + 5, (180, 90, 80))
    return t.img


def plant():
    t = Toile(16, 26)
    t.ligne([(8, 26), (8, 6)], (70, 110, 60), 2)
    for k in range(4):
        t.ellipse(1, 2 + k * 5, 15, 11 + k * 5, lum((120, 200, 100)) if k % 2 else lum((96, 176, 84)))
    return t.img


def rs6(phares=True):
    t = Toile(132, 44)
    t.poly([(4, 34), (8, 22), (30, 18), (48, 4), (100, 4), (118, 18), (128, 22), (130, 34)], (96, 98, 106))
    t.poly([(50, 7), (72, 7), (72, 18), (36, 18)], VITRE)
    t.poly([(75, 7), (98, 7), (112, 18), (75, 18)], VITRE)
    t.rect(2, 30, 132, 36, (60, 62, 70))
    t.rect(4, 24, 12, 28, lum((240, 240, 230)) if phares else (150, 150, 140))
    t.rect(122, 24, 130, 28, lum((220, 60, 50)) if phares else (120, 60, 56))
    for x in (18, 96):
        t.ellipse(x, 26, x + 20, 44, NOIR)
        t.ellipse(x + 5, 31, x + 15, 39, (110, 112, 120))
    t.grain(3, 660)
    return t.img


# ---------------------------------------------------------------- les gros plans des cinematiques
def gros_plan_yeux():
    """Les yeux du heros, de tres pres. Dans chaque pupille, le carre bleu de la tele."""
    t = Toile(480, 150, (22, 20, 24, 255))
    peau, ombre = (118, 104, 96), (84, 74, 70)
    t.rect(0, 0, 480, 150, ombre)
    for y in range(0, 150, 3):
        k = 1 - abs(y - 75) / 120
        t.rect(0, y, 480, y + 3, ton(peau, 0.75 + 0.3 * k))
    for cx in (150, 330):
        t.poly([(cx - 90, 42), (cx - 40, 30), (cx + 40, 32), (cx + 90, 46), (cx + 80, 52), (cx - 80, 48)], (30, 26, 28))  # sourcil
        t.ellipse(cx - 70, 62, cx + 70, 116, (40, 34, 34))
        t.ellipse(cx - 64, 68, cx + 64, 112, (196, 192, 186))
        t.ellipse(cx - 26, 66, cx + 26, 114, (54, 40, 34))
        t.ellipse(cx - 13, 78, cx + 13, 102, (14, 12, 14))
        t.rect(cx + 2, 80, cx + 14, 90, lum((150, 170, 220)))         # le reflet de l'ecran
        t.rect(cx + 4, 82, cx + 12, 88, lum((206, 220, 250)))
        t.poly([(cx - 72, 66), (cx, 56), (cx + 72, 68), (cx + 64, 72), (cx, 63), (cx - 64, 72)], (40, 34, 34))  # paupiere
        t.ligne([(cx - 60, 118), (cx + 60, 118)], ton(peau, 0.62))
    t.grain(5, 901)
    return t.img


def gros_plan_lettre():
    """Le courrier de la regie, pose sur la table basse."""
    t = Toile(480, 270, (0, 0, 0, 0))
    t.rect(0, 0, 480, 270, (52, 44, 40))
    for y in range(0, 270, 9):
        t.ligne([(0, y), (479, y + 4)], (60, 52, 46))
    t.poly([(110, 22), (370, 14), (382, 252), (122, 260)], (206, 204, 196))
    t.poly([(110, 22), (370, 14), (371, 20), (111, 28)], (226, 224, 216))
    papier, encre = (206, 204, 196), (52, 52, 64)
    lignes = [("REGIE IMMOBILIERE DU 12", 5, (40, 46, 90)), ("AVENUE DE FONTAINEBLEAU", 3, (90, 90, 100)), ("", 3, encre),
              ("TROISIEME RELANCE", 5, (170, 40, 40)), ("", 3, encre),
              ("MONSIEUR,", 3, encre), ("MALGRE NOS COURRIERS, VOTRE LOYER", 3, encre), ("RESTE IMPAYE DEPUIS TROIS MOIS.", 3, encre),
              ("", 3, encre), ("SANS REGLEMENT SOUS QUINZE JOURS,", 3, encre), ("NOUS ENGAGERONS UNE PROCEDURE", 3, encre),
              ("D'EXPULSION.", 3, encre), ("", 3, encre), ("MONTANT DU : 1 860 EUROS", 5, encre)]
    y = 40
    for texte, police, c in lignes:
        if texte:
            t.texte(134, y, texte, c, police, 1 if police == 3 else 1)
        y += 15 if police == 5 else 11
    t.rect(260, 218, 340, 238, (170, 40, 40, 255))
    t.texte(266, 225, "RAPPEL", (226, 210, 206), 5)
    t.grain(3, 902)
    return t.img


def gros_plan_sac():
    """Le sac ouvert dans l'arriere-boutique : des sachets qui brillent en vert."""
    t = Toile(480, 270, (0, 0, 0, 255))
    t.rect(0, 0, 480, 270, (30, 30, 36))
    t.poly([(120, 250), (150, 90), (330, 90), (360, 250)], (176, 172, 160))
    t.poly([(150, 90), (330, 90), (316, 112), (164, 112)], (40, 40, 46))
    for k, (x, y) in enumerate(((180, 98), (214, 94), (250, 99), (282, 95), (198, 104), (262, 105))):
        t.rect(x, y, x + 26, y + 16, lum((90, 220, 120)) if k % 2 == 0 else lum((70, 190, 100)))
        t.rect(x + 2, y + 2, x + 24, y + 5, lum((180, 255, 200)))
    t.texte(200, 160, "MAITRE POULET", (232, 100, 8), 5, 2)
    t.grain(4, 903)
    return t.img


def gros_plan_beeper():
    """Le beeper vert dans la main du heros."""
    t = Toile(480, 270, (0, 0, 0, 255))
    t.rect(0, 0, 480, 270, (24, 24, 30))
    t.ellipse(120, 120, 380, 300, (118, 104, 96))
    t.ellipse(110, 150, 190, 230, (108, 96, 88))
    t.rect(190, 110, 290, 200, (50, 110, 70))
    t.rect(198, 120, 282, 150, lum((150, 230, 170)))
    t.texte(204, 130, "--:--", (30, 60, 40), 5, 2)
    t.rect(222, 166, 258, 184, (30, 70, 44))
    t.grain(4, 904)
    return t.img


def gros_plan_vitres():
    """Deux rames arretees cote a cote : a travers les deux vitres, un visage. Soul, en costume."""
    t = Toile(480, 270, (0, 0, 0, 255))
    t.rect(0, 0, 480, 270, (12, 12, 16))
    for x0, c in ((40, (150, 150, 144)), (250, (138, 138, 132))):
        t.rect(x0, 40, x0 + 190, 230, c)
        t.rect(x0 + 14, 60, x0 + 176, 170, FER)
        t.rect(x0 + 18, 64, x0 + 172, 166, (26, 30, 40))
        t.rect(x0, 190, x0 + 190, 196, ton(c, 0.8))
    t.rect(268, 64, 422, 166, (44, 48, 62))
    t.ellipse(320, 82, 362, 132, (128, 108, 96))
    t.poly([(318, 92), (322, 80), (360, 80), (364, 92), (356, 86), (326, 86)], NOIR)
    t.rect(330, 104, 337, 106, NOIR)
    t.rect(346, 104, 353, 106, NOIR)
    t.poly([(296, 166), (306, 136), (376, 136), (386, 166)], (52, 56, 70))
    t.poly([(330, 136), (352, 136), (341, 160)], (200, 200, 206))
    t.rect(339, 138, 343, 158, (40, 46, 66))
    t.ellipse(110, 90, 150, 138, ton(PEAU, 0.8))                      # ton propre reflet, de dos dans l'autre vitre
    t.poly([(108, 98), (112, 86), (120, 92), (126, 84), (132, 92), (140, 86), (150, 98)], NOIR)
    t.poly([(84, 166), (96, 140), (164, 140), (176, 166)], (52, 52, 64))
    for k in range(40):
        y = 64 + (k * 37) % 100
        t.ligne([(18 + (k * 53) % 150 + 40, y), (18 + (k * 53) % 150 + 52, y)], (60, 64, 80))
    t.grain(4, 906)
    return t.img


def gros_plan_gps():
    """L'ecran du GPS de la RS6 : une seule destination."""
    t = Toile(480, 270, (0, 0, 0, 255))
    t.rect(90, 40, 390, 230, (40, 40, 48))
    t.rect(98, 48, 382, 222, (30, 46, 60))
    for k in range(12):
        t.ligne([(98 + k * 26, 48), (98 + k * 20 + 40, 222)], (40, 60, 76))
    t.ligne([(140, 200), (180, 170), (220, 160), (260, 120), (300, 100), (330, 70)], lum((255, 160, 60)), 3)
    t.ellipse(324, 62, 338, 76, lum((255, 122, 18)))
    t.rect(98, 196, 382, 222, (20, 26, 34))
    t.texte(110, 204, "DESTINATION : NAMEK", lum((255, 200, 120)), 5)
    t.texte(300, 206, "2 140 KM", (150, 160, 176), 3)
    return t.img


def gros_plan_mur():
    """Le mur du plan, a la fin de l'acte 1 : de nouvelles photos, de nouveaux fils."""
    t = Toile(480, 270, (0, 0, 0, 255))
    t.rect(30, 20, 450, 250, (96, 82, 66))
    photos = [(60, 50, "LABO", VERT), (190, 40, "GARAGE", (150, 150, 160)), (320, 60, "RS6", (120, 120, 130)),
              (90, 160, "EPICERIE", (200, 180, 140)), (250, 150, "GETO", (232, 100, 8)), (370, 170, "TOUR", (255, 122, 18))]
    centres = []
    for x, y, nom, c in photos:
        t.rect(x, y, x + 64, y + 52, (206, 202, 190))
        t.rect(x + 4, y + 4, x + 60, y + 38, (50, 52, 60))
        t.rect(x + 18, y + 14, x + 46, y + 32, c)
        t.texte(x + 6, y + 41, nom, (40, 40, 48), 3)
        centres.append((x + 32, y + 2))
    for a, b in ((0, 1), (1, 2), (0, 3), (3, 4), (4, 5), (2, 5)):
        t.ligne([centres[a], centres[b]], (190, 44, 44), 2)
    t.ligne([centres[4], (centres[4][0] + 20, centres[4][1] + 40)], (40, 40, 48), 3)   # Geto barre d'un trait
    t.grain(4, 905)
    return t.img


# ---------------------------------------------------------------- tout le monde
GENS = {
    # id : (sprite de face, buste)  -- le buste reprend le visage et les couleurs du sprite
    "youngssef": (dict(manteau=(70, 76, 96), peau=PEAU_MATE, coiffe="capuche", detail="doudoune", taille=42),
                  dict(peau=PEAU_MATE, coiffe="capuche", cheveux=NOIR, vetement="doudoune", couleur=(70, 76, 96))),
    "diallo": (dict(manteau=(110, 84, 96), peau=PEAU_FONCEE, cheveux=(150, 70, 50), coiffe="long", taille=45),
               dict(peau=PEAU_FONCEE, coiffe="chignon", cheveux=(60, 40, 40), vetement="manteau", couleur=(110, 84, 96))),
    "omar": (dict(manteau=(60, 76, 110), peau=PEAU_MATE, coiffe="casquette", cheveux=(40, 50, 76), detail="bleu"),
             dict(peau=PEAU_MATE, coiffe="casquette", cheveux=(40, 50, 76), vetement="bleu", couleur=(60, 76, 110),
                  details=("moustache", "barbe", "cernes"))),
    "mecano": (dict(manteau=(60, 76, 110), coiffe="ras", cheveux=(40, 40, 48), detail="bleu"),
               dict(peau=PEAU, coiffe="ras", cheveux=(40, 40, 48), vetement="bleu", couleur=(60, 76, 110))),
    "mounir": (dict(manteau=(150, 150, 156), peau=PEAU_CLAIRE, coiffe="gris", cheveux=(170, 170, 176), detail="tablier"),
               dict(peau=PEAU_CLAIRE, coiffe="crane", cheveux=(170, 170, 176), vetement="tablier", couleur=(120, 110, 104),
                    details=("moustache",))),
    "femme": (dict(manteau=(96, 96, 108), peau=(160, 140, 128), cheveux=(40, 34, 34), coiffe="long", detail="echarpe", taille=46),
              dict(peau=(160, 140, 128), coiffe="long", cheveux=(40, 34, 34), vetement="manteau", couleur=(84, 84, 96),
                   details=("echarpe", "badge"))),
    "frotteur": (dict(manteau=(90, 84, 80), coiffe="crane", cheveux=(60, 60, 64)),
                 dict(peau=PEAU, coiffe="crane", cheveux=(60, 60, 64), vetement="veste", couleur=(90, 84, 80), details=("barbe",), regard=1)),
    "vieux": (dict(manteau=(74, 70, 66), peau=PEAU_MATE, coiffe="gris", cheveux=(176, 176, 180), detail="canne"),
              dict(peau=PEAU_MATE, coiffe="gris", cheveux=(176, 176, 180), vetement="manteau", couleur=(74, 70, 66),
                   details=("moustache", "cernes"))),
    "ecouteurs": (dict(manteau=(84, 84, 96), peau=(140, 120, 108), cheveux=(30, 28, 30), coiffe="long", detail="ecouteurs", taille=45),
                  dict(peau=(140, 120, 108), coiffe="long", cheveux=(30, 28, 30), vetement="sweat", couleur=(84, 84, 96),
                       details=("ecouteurs",))),
    "voyageur": (dict(manteau=(70, 72, 84), coiffe="bonnet", cheveux=(60, 64, 80)),
                 dict(peau=PEAU, coiffe="bonnet", cheveux=(60, 64, 80), vetement="manteau", couleur=(70, 72, 84))),
    "gero": (dict(manteau=(206, 206, 200), cheveux=(150, 150, 156), coiffe="gris", detail="blouse"),
             dict(peau=PEAU, coiffe="gris", cheveux=(150, 150, 156), vetement="blouse", couleur=(206, 206, 200),
                  details=("lunettes", "cernes"))),
    "soul": (dict(manteau=(52, 56, 70), peau=PEAU_MATE, detail="costume"),
             dict(peau=PEAU_MATE, coiffe="court", cheveux=NOIR, vetement="costume", couleur=(52, 56, 70))),
    "soul_jeune": (dict(manteau=(84, 84, 96), peau=PEAU_MATE, coiffe="capuche"),
                   dict(peau=PEAU_MATE, coiffe="capuche", cheveux=NOIR, vetement="sweat", couleur=(84, 84, 96))),
    "toi_jeune": (dict(manteau=(60, 60, 74), coiffe="court"),
                  dict(peau=PEAU, coiffe="herisse", cheveux=NOIR, vetement="sweat", couleur=(70, 70, 84))),
    "client": (dict(manteau=(80, 86, 92), coiffe="casquette", cheveux=(50, 54, 62), detail="sac"),
               dict(peau=PEAU, coiffe="casquette", cheveux=(50, 54, 62), vetement="veste", couleur=(80, 86, 92))),
    "cliente": (dict(manteau=(104, 96, 100), peau=(160, 140, 128), cheveux=(70, 56, 50), coiffe="long", taille=45),
                dict(peau=(160, 140, 128), coiffe="long", cheveux=(70, 56, 50), vetement="manteau", couleur=(104, 96, 100))),
    "sportif": (dict(manteau=(60, 64, 70), peau=PEAU_FONCEE, coiffe="ras", cheveux=NOIR),
                dict(peau=PEAU_FONCEE, coiffe="ras", cheveux=NOIR, vetement="sweat", couleur=(60, 64, 70))),
    "policier": (dict(manteau=(40, 46, 70), coiffe="kepi", cheveux=(30, 34, 54), detail="gilet"),
                 dict(peau=PEAU, coiffe="kepi", cheveux=(30, 34, 54), vetement="gilet", couleur=(40, 46, 70))),
    "rayan": (dict(manteau=(60, 62, 68), peau=PEAU_MATE, coiffe="casquette", cheveux=(28, 28, 34), detail="doudoune", taille=44),
              dict(peau=PEAU_MATE, coiffe="casquette", cheveux=(28, 28, 34), vetement="doudoune", couleur=(60, 62, 68))),
    "moussa": (dict(manteau=(84, 76, 70), peau=PEAU_FONCEE, coiffe="bonnet", cheveux=(40, 40, 48), taille=45),
               dict(peau=PEAU_FONCEE, coiffe="bonnet", cheveux=(40, 40, 48), vetement="sweat", couleur=(84, 76, 70))),
    "douanier": (dict(manteau=(70, 84, 76), coiffe="kepi", cheveux=(50, 62, 56)),
                 dict(peau=PEAU_MATE, coiffe="kepi", cheveux=(50, 62, 56), vetement="veste", couleur=(70, 84, 76), details=("moustache",))),
    "haddad": (dict(manteau=(96, 86, 74), peau=PEAU_MATE, coiffe="gris", cheveux=(176, 176, 180), detail="moustache"),
               dict(peau=PEAU_MATE, coiffe="gris", cheveux=(176, 176, 180), vetement="veste", couleur=(96, 86, 74),
                    details=("moustache", "lunettes"))),
    "gars_geto": (dict(manteau=(74, 70, 66), peau=PEAU_MATE, coiffe="capuche"),
                  dict(peau=PEAU_MATE, coiffe="capuche", cheveux=NOIR, vetement="sweat", couleur=(74, 70, 66))),
}


def tous(racine):
    """Toutes les images des personnages et des objets de l'histoire, par nom."""
    out = {}
    for nom, (sprite, b) in GENS.items():
        out["p_" + nom] = perso(**sprite)
        out["p_" + nom + "_2"] = perso(**sprite, pas=True)
        out["buste_" + nom] = buste(**b)
    out["p_youngssef_assis"] = assis((70, 76, 96), PEAU_MATE, NOIR, "telephone")
    out["p_youngssef_banc"] = assis((70, 76, 96), PEAU_MATE, NOIR)
    import heros
    out["buste_toi"] = heros.buste()
    out["heros_assis"] = heros.assis()
    import geto
    patron, ennemi = geto.sprites(racine)                              # le patron en couleurs, l'ennemi en gris
    out["geto"], out["geto_gris"] = patron, ennemi
    out["buste_geto"], out["buste_geto_gris"] = buste_geto(patron), buste_geto(ennemi)
    out["buste_sdf"] = buste_sdf()
    inconnu = Toile(48, 56)                                            # une voix au telephone, sans visage
    inconnu.ellipse(13, 9, 35, 37, (40, 40, 52))
    inconnu.poly([(4, 56), (7, 44), (15, 39), (33, 39), (41, 44), (44, 56)], (40, 40, 52))
    out["buste_inconnu"] = inconnu.img.resize((96, 112), Image.NEAREST)
    out["heros"] = heros_profil()
    out["heros_2"] = heros_profil(pas=True)
    out["heros_charge"] = heros_profil(charge=True)
    out["heros_charge_2"] = heros_profil(pas=True, charge=True)
    out["heros_scooter"] = scooter_profil(rideur=True)
    out["heros_scooter_2"] = scooter_profil(rideur=True, pas=True)
    out["scooter_gare"] = scooter_profil()
    out["p_alien"] = alien()
    out["p_alien_2"] = alien(True)
    out["sac_poulet"] = sac()
    out["sac_vert"] = sac((60, 62, 70), True)
    out["caisses"] = caisses()
    out["plant"] = plant()
    out["rs6"] = rs6()
    out["rs6_eteinte"] = rs6(False)
    import visions
    out.update(visions.toutes())                                       # l'introduction : les lunettes, la tele
    out["gp_lettre"] = gros_plan_lettre()
    out["gp_sac"] = gros_plan_sac()
    out["gp_beeper"] = gros_plan_beeper()
    out["gp_gps"] = gros_plan_gps()
    out["gp_vitres"] = gros_plan_vitres()
    out["gp_mur"] = gros_plan_mur()
    return out
