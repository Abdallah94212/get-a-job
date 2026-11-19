"""Les images de la route de Namek (jeu/Conduite.java), tirees du "Racing Pack" de Kenney (CC0, www.kenney.nl ;
le pack telecharge est dans designs/assets/kenney_racing/). On les reduit a l'echelle du jeu, on assombrit ce qui se
voit de nuit (l'autoroute, la departementale), on rechauffe l'aube de Namek, on donne aux rochers la couleur de sa
poussiere rouge. Lancer avec python3.14 depuis designs/decor :
    python3.14 conduite.py
"""
from pathlib import Path

import numpy as np
from PIL import Image

RACINE = Path(__file__).resolve().parents[2]
PACK = RACINE / "designs" / "assets" / "kenney_racing" / "pack" / "PNG"
INTERFACE = RACINE / "jeu" / "ressources" / "interface"


def ouvrir(chemin):
    return Image.open(PACK / chemin).convert("RGBA")


def reduire(im, hauteur=None, largeur=None):
    l, h = im.size
    k = hauteur / h if hauteur else largeur / l
    petit = im.resize((max(1, round(l * k)), max(1, round(h * k))), Image.LANCZOS)
    a = np.asarray(petit).copy()
    a[..., 3] = np.where(a[..., 3] > 110, 255, 0)                     # des bords nets, comme le reste du jeu
    return Image.fromarray(a)


def teinter(im, mul, ajout=(0, 0, 0)):
    a = np.asarray(im).astype(float)
    for c in range(3):
        a[..., c] = np.clip(a[..., c] * mul[c] + ajout[c], 0, 255)
    return Image.fromarray(a.astype(np.uint8))


def recolorer(im, couleur):
    """Garde la lumiere du dessin, change sa couleur : les rochers bleutes deviennent ceux de Namek."""
    a = np.asarray(im).astype(float)
    lum = (a[..., 0] * 0.3 + a[..., 1] * 0.59 + a[..., 2] * 0.11) / 255
    for c in range(3):
        a[..., c] = np.clip(couleur[c] * (0.35 + lum * 0.9), 0, 255)
    return Image.fromarray(a.astype(np.uint8))


NUIT = (0.42, 0.44, 0.55)
AUBE = (1.0, 0.82, 0.68)


def tous():
    out = {}
    out["cond_rs6"] = reduire(ouvrir("Cars/car_black_1.png"), 32)
    for i, nom in enumerate(("car_blue_2", "car_red_4", "car_green_1", "car_yellow_3", "car_black_2"), 1):
        out[f"cond_trafic_{i}"] = teinter(reduire(ouvrir(f"Cars/{nom}.png"), 38 if nom == "car_red_4" else 33), (0.8, 0.8, 0.9))
    for i in (1, 2, 3):
        out[f"cond_rocher_{i}"] = recolorer(reduire(ouvrir(f"Objects/rock{i}.png"), largeur=20), (150, 84, 56))
    out["cond_plot"] = reduire(ouvrir("Objects/cone_straight.png"), largeur=10)
    out["cond_baril"] = teinter(reduire(ouvrir("Objects/barrel_blue.png"), largeur=13), (0.8, 0.8, 0.9))
    out["cond_pneu"] = teinter(reduire(ouvrir("Objects/tires_white.png"), largeur=14), (0.8, 0.8, 0.85))
    huile = reduire(ouvrir("Objects/oil.png"), largeur=22)
    a = np.asarray(huile).copy()
    a[..., 3] = (a[..., 3] * 0.85).astype(np.uint8)
    out["cond_huile"] = teinter(Image.fromarray(a), (0.5, 0.5, 0.6))
    out["cond_arbre_nuit"] = teinter(reduire(ouvrir("Objects/tree_small.png"), largeur=26), NUIT)
    out["cond_arbre_grand_nuit"] = teinter(reduire(ouvrir("Objects/tree_large.png"), largeur=34), NUIT)
    out["cond_rocher_bord"] = recolorer(reduire(ouvrir("Objects/rock2.png"), largeur=28), (120, 66, 46))
    # les textures, en carreaux de 32 : le sol de chaque troncon
    out["cond_asphalte_nuit"] = teinter(reduire(ouvrir("Tiles/Asphalt road/road_asphalt22.png"), 32), (0.34, 0.36, 0.44))
    out["cond_herbe_nuit"] = teinter(reduire(ouvrir("Tiles/Grass/land_grass11.png"), 32), (0.3, 0.36, 0.4))
    out["cond_terre_nuit"] = teinter(reduire(ouvrir("Tiles/Dirt/land_dirt13.png"), 32), NUIT)
    out["cond_piste_aube"] = teinter(reduire(ouvrir("Tiles/Sand road/road_sand18.png"), 32), (0.82, 0.66, 0.54))
    out["cond_sable_aube"] = recolorer(reduire(ouvrir("Tiles/Sand/land_sand12.png"), 32), (176, 98, 62))
    return out


if __name__ == "__main__":
    for nom, image in tous().items():
        image.save(INTERFACE / f"{nom}.png")
    print(len(tous()), "images ->", INTERFACE)
