"""Old Geto en deux versions : en couleurs tant qu'il est le patron, gris quand il devient l'ennemi.

Le dessin est celui de designs/geto_sprite.py ; seules ses couleurs changent. Le patron prend ses couleurs
directement dans la palette ternie (peau halee, t-shirt bleu, coq orange) ; l'ennemi est ramene a la rampe gris-bleu.
"""
import sys
from pathlib import Path

import palette as PAL


def _c(i, pal=None):
    c = (PAL.TERNE if pal is None else pal)[i]
    return (int(c[0]), int(c[1]), int(c[2]), 255)


def sprites(racine):
    """(le patron, l'ennemi) : deux images de 72 x 90."""
    sys.path.insert(0, str(Path(racine) / "designs"))
    import geto_sprite as G
    origine = {k: getattr(G, k) for k in ("OUTLINE", "WHITE", "PALE", "APRON_SHADE", "MID_STAIN", "SKIN", "SKIN_LIGHT",
                                          "SKIN_SHADE", "SCAR", "HAIR", "SHIRT", "SHIRT_SHADE", "TROUSERS",
                                          "TROUSERS_SHADE", "SHOE", "ORANGE", "ORANGE_D")}
    ennemi = PAL.vers_gris(G.sprite())
    couleurs = dict(OUTLINE=_c(25), WHITE=_c(19), PALE=_c(20), APRON_SHADE=_c(21), MID_STAIN=_c(31),
                    SKIN=_c(3), SKIN_LIGHT=_c(30), SKIN_SHADE=_c(4), SCAR=_c(6), HAIR=_c(21),
                    SHIRT=_c(16), SHIRT_SHADE=_c(23), TROUSERS=_c(24), TROUSERS_SHADE=_c(25), SHOE=_c(6),
                    ORANGE=_c(9, PAL.DOUCE), ORANGE_D=_c(1, PAL.DOUCE))
    for k, v in couleurs.items():
        setattr(G, k, v)
    patron = G.sprite()
    for k, v in origine.items():
        setattr(G, k, v)
    return patron, ennemi
