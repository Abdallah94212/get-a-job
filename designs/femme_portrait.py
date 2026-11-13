"""La femme du metro en grand, dessinee a la main, dans le meme style que le heros (heros_portrait.py).

Tenue streetwear : bonnet bordeaux, cheveux blonds en bataille, foulard rouge noue au cou, debardeur blanc, chaine,
petite boucle d'oreille, cigarette au coin des levres. Le regard mi-clos, fatigue. Decor : une rame de metro, la nuit.
Lancement : python femme_portrait.py   (depuis designs/)
"""
from PIL import Image

import heros_portrait as HP
from heros_portrait import Dessin, ENCRE, L, H, SORTIE, miroir, pc98

PEAU, PEAU_O, PEAU_C = (244, 204, 184), (208, 150, 138), (254, 234, 222)
BLOND, BLOND_O, BLOND_C = (236, 198, 70), (172, 128, 40), (252, 230, 130)   # un blond franc : il ne doit pas se confondre avec la peau
BONNET, BONNET_O, BONNET_C = (126, 38, 50), (88, 24, 36), (156, 60, 68)
ROUGE, ROUGE_O = (204, 42, 46), (150, 26, 34)
BLANC, BLANC_O = (242, 240, 232), (196, 196, 204)


def decor(d):
    """Une rame de metro, tard : la paroi claire, la vitre noire ou filent les lumieres du tunnel, une barre, un siege."""
    d.forme([(0, 0), (480, 0), (480, 270), (0, 270)], (150, 160, 152))
    d.forme([(0, 0), (480, 0), (480, 22), (0, 22)], (214, 222, 214))                # le neon du plafond
    d.forme([(24, 40), (200, 40), (200, 176), (24, 176)], (18, 20, 32), 1.6)        # la vitre
    for y, x0, x1 in ((66, 30, 110), (92, 80, 196), (128, 34, 150), (150, 120, 198)):
        d.trait([(x0, y), (x1, y)], (220, 170, 90), 1.6)                            # les lumieres du tunnel
    d.forme([(330, 40), (470, 40), (470, 176), (330, 176)], (18, 20, 32), 1.6)
    d.trait([(336, 80), (440, 80)], (220, 170, 90), 1.6)
    d.trait([(360, 140), (466, 140)], (220, 170, 90), 1.6)
    d.forme([(0, 196), (480, 196), (480, 270), (0, 270)], (64, 96, 96), 1.4)        # le dossier des sieges
    d.forme([(438, 0), (448, 0), (448, 270), (438, 270)], (190, 196, 200), 1.2)     # la barre


def buste(d):
    # --- les epaules nues et le debardeur blanc
    d.forme([(118, 270), (134, 234), (180, 213), (300, 213), (346, 234), (362, 270)], PEAU, 1.6, 1)
    d.forme([(296, 213), (300, 213), (346, 234), (362, 270), (316, 270), (312, 236)], PEAU_O)
    d.forme([(184, 270), (190, 228), (203, 214), (215, 214), (222, 238), (240, 245), (258, 238), (265, 214), (277, 214),
             (290, 228), (296, 270)], BLANC, 1.5)
    d.forme([(266, 214), (277, 214), (290, 228), (296, 270), (272, 270), (270, 240)], BLANC_O)
    d.trait([(200, 250), (206, 268)], BLANC_O, 1.2)
    # --- le cou, la chaine, le foulard rouge noue
    d.forme([(214, 176), (266, 176), (271, 216), (209, 216)], PEAU, 1.4)
    d.forme([(246, 176), (266, 176), (271, 216), (252, 216)], PEAU_O)
    d.forme([(214, 180), (266, 180), (260, 194), (236, 199), (216, 191)], PEAU_O)
    d.trait([(212, 220), (240, 250), (268, 220)], (176, 180, 196), 1.4)             # la chaine
    d.ovale(236, 248, 244, 256, (176, 180, 196), 1)
    d.forme([(205, 199), (275, 199), (281, 216), (199, 216)], ROUGE, 1.5, 1)
    d.forme([(250, 199), (275, 199), (281, 216), (256, 216)], ROUGE_O)
    d.forme([(224, 213), (256, 213), (246, 240), (238, 244)], ROUGE_O, 1.4)         # le noeud et sa pointe
    d.forme([(228, 210), (252, 210), (250, 220), (230, 220)], ROUGE, 1.3, 1)
    for x, y in ((212, 206), (222, 209), (262, 206), (270, 210), (240, 228)):
        d.ovale(x, y, x + 2.5, y + 2.5, (250, 236, 226))                             # le motif du foulard
    # --- les cheveux blonds, derriere : en bataille, jusqu'aux epaules
    d.forme([(164, 96), (316, 96), (326, 160), (320, 216), (304, 228), (296, 204), (292, 150), (188, 150), (184, 204),
             (176, 228), (160, 216), (154, 160)], BLOND, 1.4, 1)
    d.forme([(300, 100), (316, 96), (326, 160), (320, 216), (304, 228), (298, 200), (300, 150)], BLOND_O)
    for x, y0, y1, c in ((164, 124, 206, BLOND_C), (172, 136, 216, BLOND_O), (180, 150, 200, BLOND_C), (310, 130, 212, (140, 104, 40))):
        d.trait([(x, y0), (x + (3 if x < 240 else -3), y1)], c, 1.2)
    # --- le visage : ovale, le menton fin
    visage = [(192, 100), (288, 100), (290, 134), (282, 160), (262, 184), (240, 193), (218, 184), (198, 160), (190, 134)]
    d.forme(visage, PEAU, 1.5, 2)
    d.forme([(270, 104), (288, 104), (290, 134), (282, 160), (262, 184), (250, 190), (264, 164), (272, 134)], PEAU_O, 0, 1)
    d.forme([(194, 110), (200, 110), (203, 156), (197, 150)], PEAU_C)
    d.trait(HP.adoucir(visage, 2) + HP.adoucir(visage, 2)[:1], ENCRE, 1.5)
    # --- les yeux mi-clos : la paupiere lourde, un trait de cils, des cernes
    for cx, s in ((214, 1), (266, -1)):
        d.forme([(cx - 17, 150), (cx - 6, 145), (cx + 8, 145), (cx + 17, 150), (cx + 8, 156), (cx - 6, 156)], (248, 246, 240), 0, 1)
        d.ovale(cx - 6, 144, cx + 6, 157, (58, 44, 44))
        d.ovale(cx - 2.5, 148, cx + 2.5, 153.5, (14, 12, 16))
        d.forme([(cx - 2 - s * 3, 146), (cx + 1 - s * 3, 146), (cx + 1 - s * 3, 149), (cx - 2 - s * 3, 149)], (250, 250, 250))
        d.forme([(cx - 19, 150), (cx - 6, 141), (cx + 8, 141), (cx + 19, 150), (cx + 8, 147.5), (cx - 6, 147.5)], PEAU_O, 0, 1)   # la paupiere
        d.trait([(cx - 18, 151), (cx - 6, 147), (cx + 8, 147), (cx + 18, 151)], ENCRE, 2.2)                # les cils
        d.trait([(cx - s * 18, 151), (cx - s * 24, 147)], ENCRE, 1.6)                                      # le trait qui s'etire
        d.trait([(cx - 14, 159), (cx, 161), (cx + 14, 159)], PEAU_O, 1.2)                                  # le cerne
        d.trait([(cx - 16, 135), (cx, 131), (cx + 16, 135)], (150, 118, 70), 1.8)                          # le sourcil, fin
    # --- le nez, les levres, la cigarette
    d.forme([(240, 158), (246, 171), (237, 173)], PEAU_O)
    d.trait([(244, 166), (246, 171), (238, 173)], ENCRE, 1)
    d.forme([(227, 181), (235, 178), (240, 180), (245, 178), (253, 181), (240, 184)], (188, 88, 96), 0, 1)
    d.forme([(229, 183), (251, 183), (246, 189), (234, 189)], (216, 124, 126), 0, 1)
    d.trait([(227, 182), (240, 183), (253, 182)], (110, 50, 60), 1.2)
    d.forme([(250, 181), (296, 191), (295, 196), (249, 186)], BLANC, 1.2)                                   # la cigarette
    d.forme([(288, 189), (296, 191), (295, 196), (287, 194)], (255, 138, 40))
    d.trait([(298, 190), (306, 172), (298, 154), (310, 134), (304, 116)], (206, 212, 214), 2)              # la fumee
    # --- la frange, en meches
    d.forme([(188, 104), (292, 104), (291, 127), (278, 116), (270, 131), (256, 118), (246, 133), (234, 118), (222, 131),
             (210, 116), (196, 129), (188, 122)], BLOND, 1.3)
    for x in (204, 228, 250, 272):
        d.trait([(x, 106), (x - 2, 120)], BLOND_O, 1)
    d.forme([(186, 104), (198, 104), (198, 150), (193, 178), (184, 152)], BLOND, 1.2, 1)                   # les meches de cote
    d.forme(miroir([(186, 104), (198, 104), (198, 150), (193, 178), (184, 152)]), BLOND_O, 1.2, 1)
    d.arc(180, 158, 188, 168, 0, 360, (190, 194, 206))                                                     # la boucle d'oreille
    # --- le bonnet bordeaux, un peu trop grand, et son revers a cotes
    bonnet = [(164, 104), (166, 70), (184, 42), (214, 28), (264, 28), (296, 42), (314, 70), (316, 104)]
    d.forme(bonnet, BONNET, 1.5, 1)
    d.forme([(280, 34), (296, 42), (314, 70), (316, 104), (286, 104), (288, 68)], BONNET_O, 0, 1)
    d.trait([(214, 34), (206, 84)], BONNET_C, 1.2)
    d.trait([(244, 30), (242, 84)], BONNET_O, 1.2)
    d.forme([(162, 86), (318, 86), (319, 107), (161, 107)], BONNET_C, 1.5)
    d.forme([(286, 86), (318, 86), (319, 107), (286, 107)], BONNET)
    for x in range(170, 316, 9):
        d.trait([(x, 88), (x, 105)], BONNET_O, 1)


def portrait():
    d = Dessin((150, 160, 152))
    decor(d)
    d.s, d.dy = 1.22, -22
    buste(d)
    net = d.img.resize((L, H), Image.LANCZOS)
    return pc98.convertir(net, L, H, couleurs=24, trame=20, saturation=1.15, contraste=1.08)


if __name__ == "__main__":
    img = portrait()
    img.save(SORTIE / "femme_portrait.png")
    HP.ecran(img, "LA FEMME A L'ECHARPE ROUGE", "CA VA. J'AI L'HABITUDE. C'EST CA, LE PIRE.").resize((L * 2, H * 2), Image.NEAREST).save(SORTIE / "femme_portrait_dialogue.png")
    print("ok")
