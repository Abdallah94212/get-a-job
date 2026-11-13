"""Le heros, en pixel art : streetwear des annees 2000. Casquette a monogramme et bande verte et rouge, petites
lunettes noires ovales, ecouteurs a fil, long t-shirt rouge delave a manches longues avec un motif, mitaines noires,
pantalon large noir a empiecements rouges, grosses baskets. Cheveux noirs mi-longs qui depassent de la casquette.

Meme methode que la femme du metro (femme_sprite.py) : de face, aplats francs, pas de contour noir.
Lancement : python heros_sprite.py   (depuis designs/) -> outputs/heros_*.png
"""
from pathlib import Path

from PIL import Image, ImageDraw

import femme_sprite as F
from femme_sprite import Pixels, ton, L, H

SORTIE = Path(__file__).resolve().parent / "outputs"
PEAU = ((216, 170, 136), (178, 130, 100), (234, 194, 160))
CHEVEUX = ((30, 26, 34), (16, 14, 20), (64, 56, 70))
VESTE = ((36, 36, 44), (22, 22, 28), (62, 62, 74))
TSHIRT = ((232, 230, 222), (190, 190, 190), (248, 248, 244))
JEAN = ((58, 74, 112), (38, 50, 80), (84, 102, 140))
BASKET = ((216, 214, 208), (160, 160, 162), (110, 110, 118))


def baskets(p):
    b, bo, semelle = BASKET
    for x0 in (17, 34):
        p.poly([(x0, 102), (x0 + 11, 102), (x0 + 12, 107), (x0 + 12, 109), (x0 - 1, 109), (x0 - 1, 105)], b)
        p.rect(x0 + 8, 104, x0 + 11, 108, bo)
        p.rect(x0 + 2, 106, x0 + 6, 106, (40, 40, 48))                 # la bande sombre sur le cote
        p.rect(x0 - 1, 109, x0 + 12, 111, semelle)


def jean(p):
    c, o, l = JEAN
    p.poly([(24, 58), (40, 58), (41, 70), (32, 70), (32, 72), (23, 70)], c)                 # le bassin
    p.poly([(23, 66), (31, 66), (31, 105), (17, 105), (19, 80)], c)                          # la jambe gauche, tres large
    p.poly([(33, 66), (41, 66), (45, 80), (47, 105), (33, 105)], c)                          # la jambe droite
    p.poly([(40, 66), (41, 66), (45, 80), (47, 105), (43, 105), (41, 80)], o)                # l'ombre a droite
    p.rect(30, 70, 31, 104, o)
    p.rect(33, 72, 33, 104, o)
    p.ellipse(22, 76, 28, 90, l)                                                             # le delave sur les cuisses
    p.ellipse(35, 76, 40, 90, l)
    p.rect(24, 57, 40, 59, ton(c, 0.7))                                                      # la ceinture
    p.rect(31, 58, 33, 59, (160, 160, 166))
    for y in (93, 97, 101):                                                                  # le jean qui casse sur la basket
        p.ligne([(18, y), (30, y + 2)], o)
        p.ligne([(34, y + 2), (46, y)], ton(o, 0.85))
    p.ligne([(37, 60), (39, 68), (36, 73)], (170, 170, 178))                                 # une chaine de portefeuille


def tshirt(p):
    t, to, tl = TSHIRT
    p.poly([(25, 33), (39, 33), (41, 46), (40, 66), (24, 66), (23, 46)], t)                  # long : il depasse de la veste
    p.rect(37, 36, 40, 65, to)
    p.rect(24, 66, 40, 66, to)
    p.ligne([(28, 50), (31, 53)], to)
    p.ligne([(29, 34), (32, 38), (35, 34)], (150, 150, 160))                                 # la chaine au cou
    p.rect(31, 38, 32, 39, (96, 98, 108))


def veste(p):
    v, vo, vl = VESTE
    p.poly([(24, 30), (40, 30), (44, 35), (20, 35)], vl)                                     # la capuche, derriere la nuque
    for gauche in (True, False):                                                             # les manches, larges, qui couvrent le poignet
        pts = [(19, 33), (23, 33), (23, 50), (22, 67), (15, 67), (16, 48)]
        if not gauche:
            pts = [(L - 1 - x, y) for x, y in pts]
        p.poly(pts, v if gauche else vo)
        x0 = 15 if gauche else 41
        p.rect(x0 + 1, 44, x0 + 6, 45, (150, 150, 158))                                      # deux bandes grises sur la manche
        p.rect(x0 + 1, 48, x0 + 6, 48, (150, 150, 158))
        p.rect(x0 + 1, 65, x0 + 6, 67, vl)                                                   # le poignet en cote
        p.rect(x0 + 2, 68, x0 + 5, 71, PEAU[0] if gauche else PEAU[1])                       # la main
    p.poly([(19, 33), (27, 32), (28, 42), (27, 62), (20, 62), (19, 46)], v)                  # le pan gauche, ouvert
    p.poly([(37, 32), (45, 33), (45, 46), (44, 62), (37, 62), (36, 42)], vo)                 # le pan droit, dans l'ombre
    p.rect(27, 34, 27, 61, (120, 120, 130))                                                  # la fermeture eclair
    p.rect(36, 34, 36, 61, (90, 90, 100))
    p.rect(20, 60, 27, 62, vl)                                                               # le bas en cote
    p.rect(37, 60, 44, 62, v)
    p.ligne([(22, 50), (24, 56)], vl)                                                        # les poches
    p.ligne([(42, 50), (40, 56)], v)
    p.rect(20, 35, 20, 58, vl)


def tete(p):
    peau, ombre, lumiere = PEAU
    p.rect(30, 27, 34, 33, peau)                                                             # le cou
    p.rect(30, 28, 34, 29, ombre)
    p.ellipse(25, 9, 39, 29, peau)
    p.poly([(26, 23), (38, 23), (35, 30), (29, 30)], peau)                                   # la machoire, anguleuse
    p.rect(36, 13, 38, 26, ombre)
    p.poly([(38, 22), (34, 30), (36, 30), (39, 23)], ombre)
    for x in (27, 34):                                                                       # les yeux, etroits et durs
        p.rect(x, 17, x + 3, 17, (26, 20, 24))
        p.rect(x, 18, x + 3, 18, (236, 232, 226))
        p.rect(x + 1, 18, x + 2, 18, (70, 46, 34))
    p.ligne([(26, 14), (30, 15)], CHEVEUX[1])                                                # les sourcils, fronces
    p.ligne([(34, 15), (38, 14)], CHEVEUX[1])
    p.rect(32, 20, 32, 22, ombre)                                                            # le nez
    p.rect(30, 25, 34, 25, (150, 98, 88))                                                    # la bouche, fermee
    p.rect(31, 26, 33, 26, ombre)


def cheveux(p):
    c, o, l = CHEVEUX
    # la masse : mi-longs, une raie au milieu ; deux meches encadrent le visage jusqu'a la machoire, le front reste degage
    p.poly([(23, 17), (24, 9), (28, 5), (36, 5), (40, 9), (41, 17), (41, 26), (40, 28), (39, 19), (37, 13),
            (33, 10), (32, 13), (31, 10), (27, 13), (25, 19), (24, 28), (23, 26)], c)
    p.poly([(39, 12), (41, 17), (41, 26), (40, 28), (39, 19)], o)                            # le cote droit dans l'ombre
    p.ligne([(31, 10), (29, 14)], c)                                                         # deux meches courtes sur le front
    p.ligne([(33, 10), (35, 14)], c)
    p.ligne([(28, 7), (26, 12)], l)                                                          # les reflets
    p.ligne([(35, 6), (37, 10)], l)
    p.ligne([(24, 19), (24, 25)], l)
    p.px(24, 29, c)                                                                          # les pointes
    p.px(40, 29, o)


ROUGE = ((178, 54, 50), (128, 34, 38), (208, 88, 78))
PANTALON = ((38, 36, 44), (24, 22, 28), (60, 58, 68))
CUIR_ROUGE = ((122, 30, 36), (88, 20, 26))


def pantalon(p):
    """Large, noir, avec des empiecements de cuir rouge sombre sur les cuisses et les tibias ; il casse sur la basket."""
    c, o, l = PANTALON
    r, ro = CUIR_ROUGE
    p.poly([(24, 60), (40, 60), (41, 70), (32, 70), (32, 72), (23, 70)], c)
    p.poly([(23, 66), (31, 66), (31, 105), (17, 105), (19, 80)], c)
    p.poly([(33, 66), (41, 66), (45, 80), (47, 105), (33, 105)], c)
    p.poly([(23, 68), (30, 68), (30, 82), (21, 82)], r)                                      # les empiecements rouges
    p.poly([(34, 68), (41, 68), (43, 82), (34, 82)], ro)
    p.poly([(40, 66), (41, 66), (45, 80), (47, 105), (44, 105), (42, 80)], o)                # l'ombre a droite
    p.rect(30, 70, 31, 104, o)
    p.rect(33, 72, 33, 104, o)
    p.rect(24, 62, 40, 64, o)                                                                # la ceinture cloutee
    for x in range(25, 40, 3):
        p.px(x, 63, (170, 170, 178))
    for y in (100, 103):
        p.ligne([(18, y), (30, y + 1)], o)
        p.ligne([(34, y + 1), (46, y)], o)


def haut(p):
    """Un t-shirt rouge delave, trop grand, a manches longues ; un motif pale et deux lignes de texte sur la poitrine."""
    t, to, tl = ROUGE
    for gauche in (True, False):                                                             # les manches, larges
        pts = [(19, 33), (23, 33), (23, 50), (22, 67), (15, 67), (16, 48)]
        if not gauche:
            pts = [(L - 1 - x, y) for x, y in pts]
        p.poly(pts, t if gauche else to)
        x0 = 15 if gauche else 41
        p.rect(x0 + 1, 66, x0 + 6, 67, to if gauche else ton(to, 0.8))
        p.rect(x0 + 2, 68, x0 + 5, 70, (26, 26, 32))                                         # la mitaine noire
        p.rect(x0 + 2, 71, x0 + 5, 71, PEAU[0] if gauche else PEAU[1])                       # le bout des doigts
        if gauche:
            p.ligne([(18, 40), (17, 52)], tl)                                                # un pli clair sur la manche
    p.poly([(20, 33), (27, 31), (37, 31), (44, 33), (44, 46), (43, 64), (21, 64), (20, 46)], t)
    p.poly([(39, 33), (44, 33), (44, 46), (43, 64), (39, 64)], to)                           # l'ombre a droite
    p.rect(21, 64, 43, 64, to)
    p.rect(22, 36, 22, 58, tl)
    p.poly([(28, 31), (36, 31), (34, 34), (30, 34)], PEAU[1])                                # l'encolure
    p.ligne([(28, 31), (30, 34), (34, 34), (36, 31)], to)
    pale = (228, 168, 150)
    p.ligne([(25, 45), (27, 43), (29, 45), (31, 43), (33, 45), (35, 43), (37, 45), (39, 44)], pale)   # le motif, un dragon stylise
    p.px(24, 44, pale)
    p.px(26, 46, pale)
    p.px(36, 46, pale)
    for x in range(26, 39, 2):                                                               # deux lignes de petit texte
        p.px(x, 48, pale)
    for x in range(27, 38, 2):
        p.px(x, 50, pale)


def casquette(p):
    """Une casquette de luxe : toile beige a monogramme, bande verte, rouge, verte, visiere brune."""
    toile, motif = (190, 164, 124), (160, 134, 98)
    p.poly([(23, 13), (23, 9), (26, 5), (30, 4), (34, 4), (38, 5), (41, 9), (41, 13)], toile)   # la calotte, ronde et basse
    for y in range(6, 11, 2):                                                                # le monogramme, ton sur ton
        for x in range(25 + (y % 4), 40, 4):
            if p.plein(x, y):
                p.px(x, y, motif)
                p.px(x + 1, y, motif)
    p.poly([(38, 5), (41, 9), (41, 13), (38, 13)], ton(toile, 0.78))
    p.rect(31, 4, 33, 4, ton(toile, 0.8))                                                    # le bouton du dessus
    p.rect(23, 11, 41, 11, (34, 100, 62))                                                    # la bande verte, rouge, verte
    p.rect(23, 12, 41, 12, (176, 38, 42))
    p.rect(23, 13, 41, 13, (34, 100, 62))
    p.poly([(24, 14), (40, 14), (41, 15), (38, 16), (26, 16), (23, 15)], (86, 62, 42))       # la visiere courbe, vue de face
    p.rect(27, 16, 37, 16, (56, 40, 28))


def lunettes(p):
    """De petites lunettes noires ovales, sans monture, comme dans Matrix."""
    noir = (14, 14, 18)
    for x in (27, 33):
        p.rect(x, 18, x + 3, 19, noir)
        p.px(x, 18, (96, 100, 112))                                                          # le reflet
    p.rect(31, 18, 32, 18, (70, 70, 78))                                                     # le pont
    p.px(26, 18, (70, 70, 78))
    p.px(37, 18, (70, 70, 78))


def ecouteurs(p):
    blanc = (238, 238, 234)
    p.px(24, 21, blanc)
    p.px(40, 21, blanc)
    p.ligne([(24, 22), (25, 31), (29, 40), (31, 54)], blanc)                                 # les fils se rejoignent sur la poitrine
    p.ligne([(40, 22), (39, 31), (34, 40), (32, 54)], (200, 200, 200))
    p.ligne([(31, 54), (33, 62)], blanc)


def heros():
    p = Pixels()
    baskets(p)
    pantalon(p)
    haut(p)
    tete(p)
    cheveux(p)
    casquette(p)
    lunettes(p)
    ecouteurs(p)
    F.assombrir_bords(p)
    return p.img


def petit(img, hauteur=48):
    """La version de la rue : reduite a la taille des personnages du jeu."""
    img = img.crop(img.getbbox())
    a = img.resize((max(1, round(img.width * hauteur / img.height)), hauteur), Image.BOX)
    alpha = a.getchannel("A").point(lambda v: 255 if v >= 110 else 0)
    out = a.convert("RGB").quantize(16, dither=Image.NONE).convert("RGBA")
    out.putalpha(alpha)
    return out


def planche():
    z = 6
    img = heros()
    img.save(SORTIE / "heros_streetwear.png")
    mini = petit(img)
    mini.save(SORTIE / "heros_streetwear_48.png")
    out = Image.new("RGB", (L * z + 260, H * z + 60), (226, 226, 222))
    d = ImageDraw.Draw(out)
    grand = img.resize((L * z, H * z), Image.NEAREST)
    out.paste(grand, (20, 30), grand)
    d.text((20, 8), "HEROS - STREETWEAR", fill=(60, 60, 70))
    d.text((L * z + 60, 16), "taille reelle", fill=(60, 60, 70))
    out.paste(img, (L * z + 70, 40), img)
    d.text((L * z + 60, 190), "dans la rue (48 px), x4", fill=(60, 60, 70))
    m4 = mini.resize((mini.width * 4, mini.height * 4), Image.NEAREST)
    out.paste(m4, (L * z + 70, 215), m4)
    femme = SORTIE / "femme_streetwear.png"
    if femme.exists():
        f4 = petit(Image.open(femme).convert("RGBA"))
        f4 = f4.resize((f4.width * 4, f4.height * 4), Image.NEAREST)
        out.paste(f4, (L * z + 150, 215), f4)
    out.save(SORTIE / "heros_planche.png")


if __name__ == "__main__":
    planche()
    print(SORTIE / "heros_planche.png")
