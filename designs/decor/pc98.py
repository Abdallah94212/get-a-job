"""Le rendu des cinematiques et des portraits : une illustration anime ramenee au pixel doux des jeux PC-98.

Peu de couleurs (16), choisies sur l'image elle-meme, et un trame regulier (matrice de Bayer) qui melange deux
couleurs voisines pour faire les degrades. C'est du pixel art, mais fin : l'image garde son trait.
"""
import numpy as np
from PIL import Image, ImageEnhance

BAYER = np.array([[0, 8, 2, 10], [12, 4, 14, 6], [3, 11, 1, 9], [15, 7, 13, 5]], dtype=np.float32) / 16 - 0.5


def convertir(img, largeur, hauteur, couleurs=16, trame=34, saturation=1.25, contraste=1.15):
    """L'illustration, a la taille voulue, en `couleurs` couleurs tramees."""
    img = img.convert("RGB").resize((largeur, hauteur), Image.LANCZOS)
    img = ImageEnhance.Contrast(ImageEnhance.Color(img).enhance(saturation)).enhance(contraste)
    palette = img.quantize(couleurs, method=Image.MEDIANCUT, dither=Image.NONE)
    pal = np.array(palette.getpalette()[:couleurs * 3], dtype=np.float32).reshape(-1, 3)
    a = np.asarray(img).astype(np.float32)
    seuil = np.tile(BAYER, (hauteur // 4 + 1, largeur // 4 + 1))[:hauteur, :largeur]
    a = np.clip(a + seuil[..., None] * trame, 0, 255)
    idx = ((a[:, :, None, :] - pal[None, None]) ** 2).sum(-1).argmin(-1)
    return Image.fromarray(pal[idx].astype(np.uint8))
