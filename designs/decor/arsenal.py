"""L'arsenal et le combat a mains nues, dessines au meme trait que le heros (heros.py) : pixel par pixel, a la taille
finale, aplats, lumiere du haut, pas de contour noir.

- Les poses de combat du heros qui manquaient : tir a une main, tir a deux mains, rechargement, garde haute, coup de
  pied circulaire, uppercut, balayette, esquive, coups sautes (poing, pied), attaque plongeante, projete. Toutes sur
  la toile de combat (34 x 48), le corps au meme endroit que dans heros.combat : le jeu les centre sur les pieds.
- Les armes tenues en main (arme_*.png), tournees vers la droite, la poignee en bas a gauche : le jeu les accroche a
  la main de la pose de tir.
- Les icones d'inventaire (obj_*.png, 16 x 16) des nouvelles armes et des munitions.
- Trois adversaires tires du gars de Geto : le sabreur, le tireur et la brute (plus large, plus lourd).

Lancer avec python3.14 depuis designs/decor :
    python3.14 arsenal.py
"""
from pathlib import Path

from PIL import Image

import heros
from heros import (BASKET, CUIR, NOIR, PANT, PANT_O, PEAU, SEMELLE, TEE, TEE_C, TEE_O)
from outils import Toile, ton

RACINE = Path(__file__).resolve().parents[2]
INTERFACE = RACINE / "jeu" / "ressources" / "interface"
OX = 6                                                                 # le corps commence a x = 6 sur la toile de combat
VIDE = (0, 0, 0, 0)


# ---------------------------------------------------------------- les morceaux du corps
def _corps(sans_jambes=False, descendre=0):
    """Le heros sans son bras (et sans ses jambes si on les redessine), descendu de quelques pixels s'il se baisse."""
    corps = heros.profil()
    t = Toile(34, 48)
    if sans_jambes:
        haut = corps.crop((0, 0, 22, 33))
        t.img.paste(haut, (OX, descendre), haut)
    else:
        t.img.paste(corps, (OX, descendre), corps)
    r = lambda x0, y0, x1, y1, c: t.rect(x0 + OX, y0 + descendre, x1 + OX, y1 + descendre, c)
    r(8, 19, 12, 30, TEE)                                              # on efface le bras le long du corps
    r(4, 21, 6, 31, TEE_O)
    if not sans_jambes:
        r(8, 30, 13, 33, PANT)
    else:
        r(8, 30, 13, 31, TEE)
    return t


def _bras(t, points, poing=True):
    """Un bras : l'epaule, le coude, la main ; la mitaine noire au bout, les phalanges claires."""
    t.ligne(points, TEE_C, 2)
    x, y = points[-1]
    if poing:
        t.rect(x - 1, y - 2, x + 3, y + 2, NOIR)
        t.rect(x + 2, y - 1, x + 3, y + 1, PEAU)


def _jambe(t, points, devant=True, semelle_vers=(1, 0)):
    """Une jambe de pantalon large (4 pixels), l'empiecement rouge a la cuisse, la basket blanche au bout."""
    t.ligne(points, PANT if devant else PANT_O, 4)
    (hx, hy), (gx, gy) = points[0], points[1]
    if devant:
        t.ligne([((hx * 2 + gx) // 3, (hy * 2 + gy) // 3), ((hx + gx * 2) // 3, (hy + gy * 2) // 3)], CUIR, 2)
    x, y = points[-1]
    sx, sy = semelle_vers
    if sx and not sy:                                                  # le pied a plat, pointe vers l'avant
        t.rect(x - 2, y - 1, x + 5, y + 2, BASKET)
        t.rect(x - 2, y + 2, x + 5, y + 3, SEMELLE)
    elif sy and not sx:                                                # la semelle face a l'ennemi (coup de pied tendu)
        t.rect(x - 1, y - 3, x + 2, y + 4, BASKET)
        t.rect(x + 1, y - 3, x + 2, y + 4, SEMELLE)
    else:                                                              # le pied en diagonale
        t.rect(x - 2, y - 2, x + 3, y + 2, BASKET)
        t.rect(x + 1, y, x + 3, y + 2, SEMELLE)


def _pencher(img, decalage):
    """Le haut du corps part de "decalage" pixels, les pieds restent : comme Poses.penche dans le jeu."""
    l, h = img.size
    out = Image.new("RGBA", (l, h), VIDE)
    for y in range(h):
        k = 1 - y / (h - 1)
        dx = round(decalage * k * k)
        for x in range(l):
            p = img.getpixel((x, y))
            if p[3] and 0 <= x + dx < l:
                out.putpixel((x + dx, y), p)
    return out


def _tourner(img, angle, centre):
    return img.rotate(angle, resample=Image.NEAREST, center=centre)


# ---------------------------------------------------------------- les poses
MAIN_TIR = (OX + 23, 20)          # la main tendue de la pose "tir" : le jeu y accroche la crosse du pistolet
EPAULE_TIR = (OX + 11, 22)        # pose "tir_deux" : la crosse a l'epaule


def tir():
    """Le bras tendu a hauteur d'epaule, l'autre main qui soutient : le buste un peu en arriere pour encaisser le recul."""
    t = _corps()
    _bras(t, [(OX + 11, 20), (OX + 17, 20), (OX + 22, 20)])
    t.ligne([(OX + 10, 22), (OX + 15, 24), (OX + 20, 21)], ton(TEE_C, 0.85), 2)   # l'autre main sous la premiere
    return _pencher(t.img, -1)


def tir_deux():
    """L'arme longue a l'epaule : le coude du bras arriere leve, la main avant sous le canon."""
    t = _corps()
    t.ligne([(OX + 9, 20), (OX + 7, 24), (OX + 12, 23)], ton(TEE_C, 0.85), 2)
    t.rect(OX + 11, 21, OX + 14, 24, NOIR)
    t.ligne([(OX + 11, 20), (OX + 16, 25), (OX + 21, 24)], TEE_C, 2)
    t.rect(OX + 20, 22, OX + 24, 25, NOIR)
    return _pencher(t.img, -1)


def recharge():
    """Les deux mains a la ceinture, la tete baissee sur l'arme : le chargeur change."""
    t = _corps()
    _bras(t, [(OX + 11, 20), (OX + 13, 26), (OX + 17, 27)])
    t.ligne([(OX + 10, 21), (OX + 12, 28), (OX + 14, 30)], ton(TEE_C, 0.85), 2)
    t.rect(OX + 13, 29, OX + 16, 32, NOIR)
    return _pencher(t.img, 2)


def garde_haute():
    """La garde fermee : les deux avant-bras devant le visage, le menton rentre."""
    t = _corps()
    t.ligne([(OX + 10, 21), (OX + 14, 25), (OX + 16, 15)], ton(TEE_C, 0.85), 2)
    t.rect(OX + 14, 12, OX + 18, 16, NOIR)
    t.ligne([(OX + 11, 20), (OX + 17, 24), (OX + 19, 14)], TEE_C, 3)
    t.rect(OX + 17, 11, OX + 21, 15, NOIR)
    return _pencher(t.img, -1)


def circulaire():
    """Le coup de pied circulaire : la jambe monte a hauteur de tete, le buste part en arriere pour faire contrepoids."""
    t = _corps(sans_jambes=True)
    _jambe(t, [(OX + 8, 33), (OX + 6, 40), (OX + 6, 45)], devant=False)
    _jambe(t, [(OX + 12, 32), (OX + 19, 25), (OX + 27, 19)], semelle_vers=(0, 1))
    t.ligne([(OX + 10, 21), (OX + 4, 23)], TEE_C, 2)                   # les bras ouverts pour l'equilibre
    t.rect(OX + 2, 21, OX + 5, 25, NOIR)
    return _pencher(t.img, -4)


def poing_fort():
    """L'uppercut : le poing remonte du bas jusqu'au-dessus de la tete, le genou avant plie, tout le corps se deplie."""
    t = _corps(sans_jambes=True)
    _jambe(t, [(OX + 8, 33), (OX + 4, 40), (OX + 3, 45)], devant=False)
    _jambe(t, [(OX + 12, 32), (OX + 17, 38), (OX + 16, 45)])
    _bras(t, [(OX + 12, 20), (OX + 18, 15), (OX + 19, 7)])
    return _pencher(t.img, 2)


def balayette():
    """Accroupi tres bas, une main au sol, la jambe avant qui fauche le bitume."""
    t = _corps(sans_jambes=True, descendre=13)
    _jambe(t, [(OX + 8, 45), (OX + 4, 46), (OX + 1, 46)], devant=False)
    _jambe(t, [(OX + 12, 44), (OX + 20, 46), (OX + 26, 45)])
    t.ligne([(OX + 10, 34), (OX + 6, 40), (OX + 5, 46)], TEE_C, 2)     # la main posee au sol
    t.rect(OX + 3, 45, OX + 7, 48, NOIR)
    return t.img


def esquive():
    """L'esquive : il plonge sur le cote, tres bas, les bras rentres, la jambe arriere qui pousse."""
    t = _corps(sans_jambes=True, descendre=9)
    _jambe(t, [(OX + 8, 41), (OX + 2, 44), (OX - 3, 46)], devant=False)
    _jambe(t, [(OX + 12, 40), (OX + 17, 42), (OX + 16, 46)])
    t.ligne([(OX + 11, 29), (OX + 15, 33), (OX + 16, 27)], TEE_C, 2)
    t.rect(OX + 14, 24, OX + 18, 28, NOIR)
    return _pencher(t.img, -5)


def pied_saute():
    """Le coup de pied saute : la jambe avant tendue a l'horizontale, l'autre repliee sous lui, le buste en arriere."""
    t = _corps(sans_jambes=True)
    _jambe(t, [(OX + 8, 33), (OX + 5, 38), (OX + 11, 41)], devant=False)
    _jambe(t, [(OX + 12, 32), (OX + 20, 32), (OX + 27, 31)], semelle_vers=(0, 1))
    _bras(t, [(OX + 11, 20), (OX + 15, 23), (OX + 18, 19)])
    return _pencher(t.img, -3)


def poing_saute():
    """Le poing saute : les genoux ramenes, le poing qui part vers le bas, vers la tete de celui qui est au sol."""
    t = _corps(sans_jambes=True)
    _jambe(t, [(OX + 8, 33), (OX + 4, 37), (OX + 7, 42)], devant=False)
    _jambe(t, [(OX + 12, 32), (OX + 17, 37), (OX + 12, 41)], semelle_vers=(1, 1))
    _bras(t, [(OX + 11, 20), (OX + 17, 23), (OX + 23, 26)])
    return _pencher(t.img, 2)


def plongeon():
    """L'attaque plongeante : il tombe sur l'ennemi, la jambe tendue vers le sol en diagonale, les bras en arriere."""
    t = _corps(sans_jambes=True)
    _jambe(t, [(OX + 8, 33), (OX + 4, 37), (OX + 9, 40)], devant=False)
    _jambe(t, [(OX + 12, 32), (OX + 18, 39), (OX + 24, 45)], semelle_vers=(1, 1))
    t.ligne([(OX + 9, 21), (OX + 3, 17)], TEE_C, 2)
    t.rect(OX + 1, 15, OX + 4, 18, NOIR)
    return _pencher(t.img, 4)


def projete():
    """Projete en arriere : le corps part a l'oblique, les bras et les jambes en avant, sans controle."""
    t = Toile(34, 48)
    touche = heros.combat("touche")
    t.img.paste(_tourner(touche, 34, (17, 40)), (0, 0))
    return t.img


POSES = {"tir": tir, "tir_deux": tir_deux, "recharge": recharge, "garde_haute": garde_haute, "circulaire": circulaire,
         "poing_fort": poing_fort, "balayette": balayette, "esquive": esquive, "pied_saute": pied_saute,
         "poing_saute": poing_saute, "plongeon": plongeon, "projete": projete}


# ---------------------------------------------------------------- les armes en main (tournees vers la droite)
ACIER, ACIER_O, ACIER_C = (58, 60, 68), (38, 40, 46), (110, 114, 126)
CHROME, CHROME_O = (150, 154, 164), (104, 108, 118)
BOIS, BOIS_O = (122, 84, 52), (88, 60, 38)
POLY = (44, 44, 40)


def arme_pistolet():
    """8 x 5 : la culasse, la poignee en bas a gauche (c'est la qu'est la main)."""
    t = Toile(9, 6)
    t.rect(0, 0, 9, 2, ACIER)
    t.rect(0, 0, 9, 1, ACIER_C)
    t.rect(1, 2, 4, 6, ACIER_O)
    t.point(5, 2, ACIER_O)
    return t.img


def arme_pistolet_lourd():
    t = Toile(11, 7)
    t.rect(0, 0, 11, 3, CHROME)
    t.rect(0, 0, 11, 1, (200, 204, 212))
    t.rect(8, 1, 11, 2, CHROME_O)
    t.rect(1, 3, 4, 7, BOIS)
    t.rect(1, 3, 2, 7, BOIS_O)
    t.point(5, 3, CHROME_O)
    return t.img


def arme_mitraillette():
    """13 x 8 : courte, le chargeur droit sous la poignee, une crosse repliee."""
    t = Toile(14, 8)
    t.rect(2, 1, 14, 4, ACIER)
    t.rect(2, 1, 14, 2, ACIER_C)
    t.rect(11, 2, 14, 3, ACIER_O)
    t.rect(4, 4, 6, 8, POLY)                                           # la poignee
    t.rect(7, 4, 9, 8, ACIER_O)                                        # le chargeur
    t.rect(0, 1, 2, 3, POLY)                                           # la crosse repliee
    return t.img


def arme_fusil():
    """20 x 7 : long, crosse et garde-main beiges, chargeur courbe."""
    t = Toile(21, 7)
    t.rect(0, 2, 5, 5, (150, 126, 90))                                 # la crosse
    t.rect(0, 2, 5, 3, (176, 150, 110))
    t.rect(5, 1, 15, 4, ACIER)
    t.rect(5, 1, 15, 2, ACIER_C)
    t.rect(11, 2, 16, 4, (150, 126, 90))                               # le garde-main
    t.rect(15, 2, 21, 3, ACIER_O)                                      # le canon
    t.rect(6, 4, 8, 7, POLY)
    t.poly([(9, 4), (11, 4), (12, 6), (10, 7)], ACIER_O)               # le chargeur courbe
    return t.img


# ---------------------------------------------------------------- les icones (16 x 16)
OMBRE = (14, 14, 20, 140)


def _icone(arme, dx=0, dy=0):
    t = Toile(16, 16)
    w, h = arme.size
    k = 1
    x = (16 - w * k) // 2 + dx
    y = (16 - h * k) // 2 + dy
    t.rect(x + 1, y + h + 1, x + w, y + h + 2, OMBRE)
    t.img.paste(arme, (x, y), arme)
    return t.img


def icone_batte():
    t = Toile(16, 16)
    t.ligne([(2, 14), (13, 3)], (196, 160, 110), 2)
    t.ligne([(9, 7), (14, 2)], (214, 178, 126), 3)
    t.ligne([(2, 14), (4, 12)], (40, 40, 46), 2)                       # le scotch du manche
    t.point(13, 2, (236, 210, 160))
    return t.img


def icone_katana(relique=False):
    t = Toile(16, 16)
    lame = [(150, 230, 220), (180, 160, 240), (240, 160, 210)] if relique else [(214, 218, 226)]
    for i in range(10):                                                # la lame, un pixel de couleur par pas
        c = lame[i % len(lame)]
        t.point(5 + i, 10 - i, c)
        t.point(6 + i, 10 - i, ton(c, 0.8))
    t.rect(3, 10, 6, 12, (200, 170, 60) if relique else (60, 60, 66))  # la tsuba
    t.ligne([(1, 14), (4, 11)], (30, 30, 36), 2)                       # la poignee
    t.point(2, 13, (190, 60, 50))
    if relique:
        t.point(12, 2, (255, 255, 255))
    return t.img


def icone_balles(couleur, longues=False):
    t = Toile(16, 16)
    for i in range(4):
        x = 2 + i * 3
        h = 10 if longues else 7
        y = 14 - h
        t.rect(x, y + 2, x + 2, 14, couleur)
        t.rect(x, y, x + 2, y + 2, (190, 120, 70))                     # la pointe cuivre
        t.point(x, y + 2, ton(couleur, 1.2))
    t.rect(1, 14, 15, 15, OMBRE)
    return t.img


# ---------------------------------------------------------------- les adversaires
def _recolorer(img, table):
    out = img.copy()
    for y in range(img.size[1]):
        for x in range(img.size[0]):
            p = img.getpixel((x, y))
            if p[3] and p[:3] in table:
                out.putpixel((x, y), table[p[:3]] + (p[3],))
    return out


def _elargir(img, kx, ky):
    w, h = img.size
    return img.resize((round(w * kx), round(h * ky)), Image.NEAREST)


def adversaires():
    out = {}
    for suffixe in ("", "_2"):
        base = Image.open(INTERFACE / f"p_gars_geto{suffixe}.png").convert("RGBA")
        gris, gris_c = (74, 70, 66), (99, 94, 89)
        sab = _recolorer(base, {gris: (46, 50, 66), gris_c: (64, 70, 90)})
        for x in range(5, 17):                                         # le bandeau rouge du sabreur
            for y in range(4, 6):
                if sab.getpixel((x, y))[3]:
                    sab.putpixel((x, y), (176, 40, 44, 255))
        out["p_sabreur" + suffixe] = sab
        out["p_tireur" + suffixe] = _recolorer(base, {gris: (64, 70, 56), gris_c: (86, 94, 74)})
        brute = _recolorer(base, {gris: (52, 46, 46), gris_c: (72, 64, 62)})
        out["p_brute" + suffixe] = _elargir(brute, 1.36, 1.1)
    return out


# ---------------------------------------------------------------- les objets de l'histoire
def lame_plantee():
    """La lame de Namek : un rocher clair, une lame enfoncee jusqu'au tiers, la tsuba doree, la poignee usee (24 x 34)."""
    t = Toile(24, 34)
    t.ellipse(1, 22, 23, 34, (14, 14, 20, 120))                        # l'ombre
    t.poly([(2, 32), (3, 24), (8, 19), (16, 19), (21, 24), (22, 32)], (150, 140, 120))
    t.poly([(8, 19), (16, 19), (19, 22), (6, 23)], (180, 170, 150))     # le dessus du rocher, dans la lumiere
    t.poly([(15, 24), (21, 25), (22, 32), (16, 32)], (118, 110, 96))
    for i, c in enumerate(((150, 230, 220, 254), (180, 160, 240, 254), (240, 160, 210, 254))):   # la lame, irisee
        t.rect(11, 9 + i * 4, 13, 13 + i * 4, c)
    t.point(11, 10, (255, 255, 255, 254))
    t.rect(8, 7, 16, 9, (200, 170, 60))                                # la tsuba doree
    t.rect(11, 0, 13, 7, (38, 34, 40))                                 # la poignee, son tressage use
    for y in (1, 3, 5):
        t.point(11, y, (120, 60, 56))
    return t.img


def sac_sport():
    """Le sac de sport noir tombe du coffre, a moitie ouvert (24 x 14)."""
    t = Toile(24, 14)
    t.ellipse(1, 9, 23, 14, (14, 14, 20, 120))
    t.poly([(2, 12), (2, 5), (5, 3), (19, 3), (22, 5), (22, 12)], (34, 34, 40))
    t.rect(2, 3, 22, 5, (52, 52, 60))
    t.ligne([(7, 3), (9, 0), (15, 0), (17, 3)], (24, 24, 28), 2)       # les anses
    t.rect(10, 4, 16, 6, (70, 70, 80))                                 # la fermeture ouverte
    t.rect(11, 5, 15, 6, (120, 112, 90))                               # une crosse qui depasse
    t.rect(19, 7, 21, 11, (200, 60, 50))                               # la marque
    return t.img


def tous():
    out = {"heros_" + nom: f() for nom, f in POSES.items()}
    armes = {"pistolet": arme_pistolet(), "pistolet_lourd": arme_pistolet_lourd(), "mitraillette": arme_mitraillette(),
             "fusil": arme_fusil()}
    for nom, img in armes.items():
        out["arme_" + nom] = img
        out["obj_" + nom] = _icone(img)
    out["obj_batte"] = icone_batte()
    out["obj_katana"] = icone_katana()
    out["obj_katana_relique"] = icone_katana(True)
    out["obj_balles_9"] = icone_balles((176, 150, 80))
    out["obj_balles_45"] = icone_balles((190, 170, 110))
    out["obj_balles_fusil"] = icone_balles((160, 140, 70), True)
    out.update(adversaires())
    out["lame_plantee"] = lame_plantee()
    out["sac_sport"] = sac_sport()
    return out


if __name__ == "__main__":
    images = tous()
    for nom, image in images.items():
        image.save(INTERFACE / f"{nom}.png")
    print(len(images), "images ->", INTERFACE)
