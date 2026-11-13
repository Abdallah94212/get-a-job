"""Les gros plans de l'introduction : l'affiche du mur, la tele reflechie dans les lunettes du heros, puis ses yeux nus.

On ne montre jamais ce que la tele diffuse : seulement ce que ca lui fait.
Chaque image fait 480 x 150, comme les autres gros plans du jeu.
"""
from outils import Toile, lum, ton

ORANGE = (255, 122, 18)


def _visage(reflet):
    """Le visage de tres pres : la visiere en haut, les deux verres noirs. reflet(t, cx) dessine ce que le verre renvoie."""
    t = Toile(480, 150, (20, 18, 24, 255))
    peau = (150, 118, 100)
    for y in range(0, 150, 3):                                         # la peau, eclairee par l'ecran : plus claire au milieu
        k = 1 - abs(y - 80) / 130
        c = ton(peau, 0.7 + 0.35 * k)
        t.rect(0, y, 480, y + 3, (c[0], c[1], min(255, c[2] + 14)))
    t.rect(0, 0, 480, 20, (64, 46, 32))                                # le dessous de la visiere
    t.rect(0, 20, 480, 23, (40, 28, 20))
    for y in range(23, 40, 3):                                         # son ombre sur le front
        t.rect(0, y, 480, y + 3, ton(peau, 0.42 + (y - 23) * 0.012))
    t.poly([(0, 20), (34, 20), (26, 150), (0, 150)], (18, 16, 22))     # les cheveux, de chaque cote
    t.poly([(480, 20), (446, 20), (454, 150), (480, 150)], (12, 10, 16))
    t.poly([(226, 58), (254, 58), (262, 150), (218, 150)], ton(peau, 0.86))   # l'arete du nez
    t.rect(238, 58, 244, 150, ton(peau, 1.0))
    for cx in (150, 330):
        t.poly([(cx - 84, 46), (cx - 30, 38), (cx + 40, 40), (cx + 84, 50), (cx + 76, 55), (cx - 76, 52)], (24, 20, 24))   # le sourcil
        t.ellipse(cx - 82, 58, cx + 82, 116, (60, 60, 70))             # le bord du verre
        t.ellipse(cx - 79, 60, cx + 79, 114, (10, 10, 14))             # le verre noir
        reflet(t, cx)
        t.ligne([(cx - 60, 68), (cx - 20, 63)], (70, 74, 90))          # le brillant du verre
    t.rect(228, 82, 252, 86, (70, 70, 80))                             # le pont
    t.ligne([(68, 84), (30, 78)], (70, 70, 80), 2)                     # les branches
    t.ligne([(412, 84), (450, 78)], (56, 56, 64), 2)
    t.grain(4, 901)
    return t.img


def lunettes_tele():
    """Dans chaque verre, le rectangle bleu de la tele, avec ses lignes."""
    def reflet(t, cx):
        t.poly([(cx - 34, 70), (cx + 38, 67), (cx + 40, 104), (cx - 32, 106)], lum((120, 146, 210)))
        t.poly([(cx - 28, 74), (cx + 32, 72), (cx + 34, 100), (cx - 26, 101)], lum((196, 212, 250)))
        for y in range(74, 102, 4):
            t.ligne([(cx - 28, y), (cx + 34, y - 1)], lum((140, 160, 220)))
    return _visage(reflet)


def affiche():
    """L'affiche du mur, de pres, balayee par la lumiere de la tele : un homme qui se tient la tete."""
    from pathlib import Path
    from PIL import Image
    source = Path(__file__).resolve().parent / "packs" / "affiche_chambre" / "affiche.jpg"
    img = Image.open(source).convert("RGB")
    l, h = img.size
    bande = img.crop((int(l * 0.08), int(h * 0.44), int(l * 0.92), int(h * 0.44) + int(l * 0.84 * 150 / 480)))
    bande = bande.resize((240, 75), Image.LANCZOS).quantize(28, dither=Image.NONE).convert("RGB").resize((480, 150), Image.NEAREST)
    t = Toile(480, 150)
    t.img.paste(bande)
    px = t.img.load()
    for y in range(150):                                               # la nuit, et un pan de lumiere bleue en travers
        for x in range(480):
            r, g, b, _ = px[x, y]
            dans = abs((x - y * 0.6) - 230) < 70
            k = 0.95 if dans else 0.38
            px[x, y] = (int(r * k) + (10 if dans else 0), int(g * k) + (18 if dans else 2), min(255, int(b * k) + (46 if dans else 12)), 255)
    return t.img


def toutes():
    """Les deux plans du heros sont ses grands portraits dessines a la main (designs/heros_portrait.py), plein ecran."""
    import sys
    from pathlib import Path
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    import heros_portrait
    return {"gp_yeux": heros_portrait.portrait("tele")[0].convert("RGBA"),
            "gp_main": heros_portrait.main(False).convert("RGBA"),
            "gp_choc": heros_portrait.portrait("choc")[0].convert("RGBA"),
            "gp_lache": heros_portrait.main(True).convert("RGBA"), "gp_affiche": affiche()}
