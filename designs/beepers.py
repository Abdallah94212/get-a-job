"""La scene des deux beepers, devant la RS6 : dessinee a la main, comme les gros plans de l'intro (facon PC-98).

Le heros sort de ses poches deux boitiers, un dans chaque main : le beeper vert du SDF ("appuie pas dessus") et le
boitier orange du mecano, celui qui ouvre la RS6. Il ne sait plus lequel est le bon. Quatre plans :

- gp_deux_mains   : les deux boitiers, en vue subjective, devant la voiture bachee qui attend dans le noir ;
- gp_hesite_g/_d  : lui, qui regarde l'un puis l'autre (le reflet dans ses lunettes dit lequel) ;
- gp_pouce        : le pouce au-dessus du bouton du beeper VERT : le joueur voit l'erreur avant qu'elle arrive ;
- gp_clic         : le pouce enfonce ; l'ecran du beeper s'allume d'un vert qui n'a rien a faire la.
- gp_orange       : apres coup, le bon : le pouce sur le boitier orange, et derriere, les feux de la RS6 qui clignotent.

Lancement : python beepers.py  (depuis designs/). Les images vont dans jeu/ressources/interface.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent / "decor"))
import heros_portrait as hp  # noqa: E402
import pc98  # noqa: E402

L, H = 480, 270
SORTIE = Path(__file__).resolve().parents[1] / "jeu" / "ressources" / "interface"
ENCRE = hp.ENCRE
GANT, GANT_C = (26, 26, 36), (60, 60, 78)
VERT_B, VERT_O, LCD, LCD_O = (54, 120, 78), (30, 78, 52), (164, 214, 150), (104, 150, 96)
ORANGE, ORANGE_O, ORANGE_C = (238, 120, 30), (176, 76, 22), (255, 170, 80)
TOLE, BACHE, BACHE_O = (52, 56, 66), (112, 116, 124), (78, 82, 92)


def garage(d, feux=False):
    """Le fond : le garage d'Omar, la nuit. Un neon froid, la RS6 sous sa bache grise ; ses feux, si on l'a ouverte."""
    d.forme([(0, 0), (480, 0), (480, 270), (0, 270)], (34, 36, 46))
    for x in range(0, 480, 40):                                        # le rideau de fer, au fond
        d.trait([(x, 0), (x, 150)], (44, 48, 60), 1.4)
    d.forme([(120, 10), (360, 10), (480, 150), (0, 150)], (46, 50, 62))   # la lumiere du neon qui tombe
    d.forme([(170, 4), (310, 4), (310, 10), (170, 10)], (200, 220, 226), 1)  # le neon, en haut
    d.forme([(0, 150), (480, 150), (480, 270), (0, 270)], (40, 40, 46))  # le sol de beton
    for x in range(-200, 480, 60):
        d.trait([(240 + (x - 240) * 0.3, 150), (x, 270)], (48, 48, 56), 1)
    caisse = [(60, 160), (90, 120), (170, 96), (330, 96), (400, 118), (430, 160), (420, 176), (70, 176)]   # le break, bache
    d.forme(caisse, BACHE, 1.6, 2)
    d.forme([(330, 96), (400, 118), (430, 160), (420, 176), (360, 176), (370, 130)], BACHE_O)
    for a, b in (((150, 100), (130, 170)), ((230, 98), (238, 172)), ((310, 98), (330, 172))):   # les plis de la bache
        d.trait([a, b], BACHE_O, 1.4)
    d.forme([(90, 168), (126, 168), (122, 186), (94, 186)], (20, 20, 24), 1.2)   # les roues qui depassent
    d.forme([(350, 168), (386, 168), (382, 186), (354, 186)], (20, 20, 24), 1.2)
    if feux:                                                           # les clignotants, a travers la bache
        for x, y in ((70, 158), (420, 158)):
            d.ovale(x - 26, y - 14, x + 26, y + 14, (180, 110, 40))
            d.ovale(x - 14, y - 7, x + 14, y + 7, (255, 190, 80))


def beeper(d, x, y, l, allume=False):
    """Le beeper vert du SDF : un vieux boitier de plastique, un petit ecran a cristaux, un seul bouton."""
    h = l * 0.62
    d.forme([(x, y), (x + l, y), (x + l, y + h), (x, y + h)], VERT_B, 1.4, 1)
    d.forme([(x + l * 0.78, y), (x + l, y), (x + l, y + h), (x + l * 0.8, y + h)], VERT_O)
    ecran = (235, 255, 160) if allume else LCD
    d.forme([(x + l * 0.1, y + h * 0.14), (x + l * 0.74, y + h * 0.14), (x + l * 0.74, y + h * 0.56), (x + l * 0.1, y + h * 0.56)],
            ecran, 1.1)
    for k in range(4):                                                 # "--:--" en segments
        cx = x + l * (0.17 + k * 0.15) + (l * 0.05 if k > 1 else 0)
        d.trait([(cx, y + h * 0.36), (cx + l * 0.08, y + h * 0.36)], (30, 60, 40) if not allume else (60, 120, 30), 1.6)
    d.ovale(x + l * 0.4 - 2, y + h * 0.3, x + l * 0.4 + 2, y + h * 0.34, (30, 60, 40))
    d.ovale(x + l * 0.4 - 2, y + h * 0.4, x + l * 0.4 + 2, y + h * 0.44, (30, 60, 40))
    d.ovale(x + l * 0.32, y + h * 0.66, x + l * 0.52, y + h * 0.92, (30, 70, 44), 1.1)   # le bouton
    d.trait([(x + l * 0.06, y + h * 0.08), (x + l * 0.7, y + h * 0.08)], (100, 170, 120), 1)
    if allume:                                                         # la lueur verte qui deborde
        for r in (1.25, 1.5):
            cx, cy = x + l * 0.42, y + h * 0.35
            d.trait([(cx - l * r * 0.5, cy), (cx - l * r * 0.62, cy)], (150, 255, 150), 1.4)
            d.trait([(cx + l * r * 0.5, cy), (cx + l * r * 0.62, cy)], (150, 255, 150), 1.4)


def boitier(d, x, y, l, appuye=False):
    """Le boitier orange du mecano : un petit ecran noir allume d'orange, une croix de direction, un bouton."""
    h = l * 0.56
    d.forme([(x, y), (x + l, y), (x + l, y + h), (x, y + h)], ORANGE, 1.4, 2)
    d.forme([(x, y + h * 0.7), (x + l, y + h * 0.7), (x + l, y + h), (x, y + h)], ORANGE_O)
    d.forme([(x + l * 0.08, y + h * 0.16), (x + l * 0.56, y + h * 0.16), (x + l * 0.56, y + h * 0.62), (x + l * 0.08, y + h * 0.62)],
            (24, 20, 20), 1.1)
    for k in range(3):                                                 # une petite icone, en pixels oranges
        d.forme([(x + l * (0.18 + k * 0.1), y + h * 0.3), (x + l * (0.24 + k * 0.1), y + h * 0.3),
                 (x + l * (0.24 + k * 0.1), y + h * (0.46 - k * 0.04)), (x + l * (0.18 + k * 0.1), y + h * (0.46 - k * 0.04))],
                ORANGE_C)
    cx, cy, r = x + l * 0.76, y + h * 0.4, l * 0.13                    # la croix de direction
    d.ovale(cx - r, cy - r, cx + r, cy + r, (60, 30, 20) if appuye else ORANGE_O, 1.1)
    d.ovale(cx - r * 0.4, cy - r * 0.4, cx + r * 0.4, cy + r * 0.4, ORANGE_C)
    d.trait([(x + l * 0.06, y + h * 0.07), (x + l * 0.9, y + h * 0.07)], ORANGE_C, 1)


def tenir(d, x, y, l, h, gauche=True, pouce=None, appuye=False):
    """Une main gantee qui tient un boitier pose dans sa paume (le boitier, en x, y, l x h, est deja dessine).
    La mitaine noire sous la moitie basse ; quatre doigts nus replies sur le bord exterieur ; le pouce du cote
    interieur, ou au-dessus d'un point (pouce = (px, py)) : pret a appuyer, ou enfonce (appuye)."""
    ext = x if gauche else x + l                                       # le bord ou s'enroulent les doigts
    sens = -1 if gauche else 1                                         # vers l'exterieur
    u = l / 100                                                        # l'unite : un centieme de la largeur du boitier
    # la manche rouge et le poignet, qui montent du bas de l'image
    poignet_x = x + l * (0.35 if gauche else 0.65)
    d.forme([(poignet_x - 34 * u, y + h + 70 * u), (poignet_x + 34 * u, y + h + 70 * u), (poignet_x + 46 * u, 300),
             (poignet_x - 46 * u, 300)], hp.TEE, 1.6)
    d.forme([(poignet_x - 34 * u, y + h + 70 * u), (poignet_x + 34 * u, y + h + 70 * u), (poignet_x + 36 * u, y + h + 82 * u),
             (poignet_x - 36 * u, y + h + 82 * u)], hp.TEE_O, 1.2)
    # la paume dans sa mitaine : sous le boitier, elle deborde un peu vers l'exterieur
    paume = [(ext + sens * 14 * u, y + h * 0.35), (ext - sens * 20 * u, y + h * 0.55), (x + l * 0.5, y + h + 6 * u),
             (poignet_x + 34 * u * (-sens), y + h + 72 * u), (poignet_x + 34 * u * sens, y + h + 72 * u),
             (ext + sens * 26 * u, y + h * 0.9)]
    d.forme(paume, GANT, 1.6, 1)
    d.trait([(ext + sens * 10 * u, y + h * 0.75), (x + l * 0.5, y + h + 2 * u)], GANT_C, 1.2)
    # quatre doigts replies sur le bord : on en voit le bout, en arcs, poses sur la tranche
    for k in range(4):
        fy = y + h * (0.08 + k * 0.2)
        c = hp.PEAU if k % 2 == 0 else (224, 180, 144)
        d.forme([(ext + sens * 12 * u, fy), (ext - sens * 9 * u, fy + 2 * u), (ext - sens * 11 * u, fy + 10 * u),
                 (ext - sens * 8 * u, fy + 16 * u), (ext + sens * 12 * u, fy + 16 * u)], c, 1.3, 1)
        d.trait([(ext - sens * 8 * u, fy + 5 * u), (ext - sens * 4 * u, fy + 4 * u)], hp.PEAU_C, 1)    # l'ongle
    # le pouce : court et epais, il part du bas du boitier, cote interieur, et vient se poser (ou planer) sur le bouton
    vers = 1 if gauche else -1                                         # du cote interieur
    if pouce is None:
        bout = (x + l * (0.82 if gauche else 0.18), y + h * 0.78)      # au repos, sur le bord
    else:
        bout = (pouce[0], pouce[1] - (0 if appuye else 16 * u))
    base = (bout[0] + vers * 22 * u, bout[1] + 34 * u)
    e = 11 * u                                                         # la demi-epaisseur du pouce
    pouce_forme = [(base[0] - e * 1.2, base[1] + e), (bout[0] - e, bout[1] + e * 0.6), (bout[0] - e * 0.4, bout[1] - e * 0.6),
                   (bout[0] + e * 0.6, bout[1] - e * 0.5), (bout[0] + e, bout[1] + e * 0.5), (base[0] + e * 1.3, base[1] + e * 0.4)]
    d.forme(pouce_forme, hp.PEAU, 1.5, 2)
    d.trait([(bout[0] - e * 0.45, bout[1] - e * 0.1), (bout[0] + e * 0.45, bout[1] - e * 0.05)], hp.PEAU_C, 1.4)   # l'ongle
    d.trait([((bout[0] + base[0]) / 2 - e * 0.6, (bout[1] + base[1]) / 2), ((bout[0] + base[0]) / 2 + e * 0.5,
             (bout[1] + base[1]) / 2 - e * 0.2)], hp.PEAU_O, 1.1)  # la phalange
    if pouce is not None and not appuye:                               # son ombre sur le boitier : il va toucher
        d.ovale(pouce[0] - 7 * u, pouce[1] - 2 * u, pouce[0] + 7 * u, pouce[1] + 3 * u, (20, 40, 28))


def deux_mains(d):
    """Vue subjective : a gauche le beeper vert, a droite le boitier orange, la voiture bachee entre les deux."""
    garage(d)
    beeper(d, 40, 112, 120)
    tenir(d, 40, 112, 120, 120 * 0.62, gauche=True)
    boitier(d, 316, 116, 128)
    tenir(d, 316, 116, 128, 128 * 0.56, gauche=False)


def hesite(cote):
    """Lui, qui regarde un boitier puis l'autre. Il les tient devant lui, a hauteur de poitrine."""
    d = hp.Dessin((34, 36, 46))
    garage(d)
    d.s, d.dy = 1.22, -22
    hp.buste(d, "hesite_" + cote)
    d.s, d.dy = 1.0, 0
    leve_g = -16 if cote == "g" else 0                                 # celui qu'il regarde, il le leve un peu
    leve_d = -16 if cote == "d" else 0
    beeper(d, 34, 182 + leve_g, 88)
    tenir(d, 34, 182 + leve_g, 88, 88 * 0.62, gauche=True)
    boitier(d, 356, 184 + leve_d, 92)
    tenir(d, 356, 184 + leve_d, 92, 92 * 0.56, gauche=False)
    return d


def pouce(appuye):
    """Tres pres : le beeper vert dans la main, le pouce au-dessus du bouton (puis dessus). Derriere, la RS6 ne bouge pas."""
    d = hp.Dessin((34, 36, 46))
    garage(d)
    d.forme([(0, 0), (480, 0), (480, 270), (0, 270)], (30, 32, 40))   # de si pres, le fond n'est plus qu'une ombre
    d.forme([(0, 170), (480, 170), (480, 270), (0, 270)], (36, 36, 42))
    x, y, l = 130, 40, 220
    h = l * 0.62
    beeper(d, x, y, l, allume=appuye)
    bouton = (x + l * 0.42, y + h * 0.79)
    tenir(d, x, y, l, h, gauche=True, pouce=bouton, appuye=appuye)
    if appuye:
        for a, b in (((bouton[0] - 50, bouton[1] - 4), (bouton[0] - 74, bouton[1] - 14)),
                     ((bouton[0] + 50, bouton[1] - 4), (bouton[0] + 74, bouton[1] - 14)),
                     ((bouton[0] - 30, bouton[1] - 34), (bouton[0] - 44, bouton[1] - 52)),
                     ((bouton[0] + 30, bouton[1] - 34), (bouton[0] + 44, bouton[1] - 52))):
            d.trait([a, b], (235, 255, 160), 2.6)                      # "clic" : des traits de choc
    return d


def orange():
    """Apres coup : le bon boitier. Le pouce sur la croix orange ; derriere, a travers la bache, les feux qui clignotent."""
    d = hp.Dessin((34, 36, 46))
    garage(d, feux=True)
    x, y, l = 150, 120, 180
    h = l * 0.56
    boitier(d, x, y, l, appuye=True)
    tenir(d, x, y, l, h, gauche=False, pouce=(x + l * 0.76, y + h * 0.4), appuye=True)
    return d


def finir(d, couleurs=32):
    net = d.img.resize((L, H), Image.LANCZOS)
    return pc98.convertir(net, L, H, couleurs=couleurs, trame=24, saturation=1.1, contraste=1.08).convert("RGBA")


def toutes():
    d = hp.Dessin((34, 36, 46))
    deux_mains(d)
    return {"gp_deux_mains": finir(d), "gp_hesite_g": finir(hesite("g")), "gp_hesite_d": finir(hesite("d")),
            "gp_pouce": finir(pouce(False)), "gp_clic": finir(pouce(True)), "gp_orange": finir(orange())}


if __name__ == "__main__":
    images = toutes()
    planche = Image.new("RGB", (L * 2 + 8, H * 3 + 16), (0, 0, 0))
    for k, (nom, img) in enumerate(images.items()):
        img.save(SORTIE / f"{nom}.png")
        planche.paste(img.convert("RGB"), ((k % 2) * (L + 8), (k // 2) * (H + 8)))
    planche.save(Path(__file__).resolve().parent / "outputs" / "beepers_planche.png")
    print("ok :", ", ".join(images))
