"""Transforme une illustration en pixel art aux couleurs du jeu.

Les personnages principaux sont d'abord illustres (ComfyUI, en local), puis ramenes ici a la taille du jeu :
- on detoure le fond uni (remplissage depuis les bords, a tolerance),
- on reduit l'image a sa taille finale, sans lissage parasite,
- on la ramene a la palette nocturne du jeu (desaturation legere, ombres bleutees, peu de couleurs),
- on pose un trait sombre la ou la silhouette touche le vide, comme dans les portraits en pixel des annees 2000.
"""
from collections import deque

import numpy as np
from PIL import Image, ImageEnhance, ImageFilter

NUIT = np.array([30, 30, 42], dtype=np.float32)


def detourer(img, tolerance=38, graines=None):
    """Rend transparent le fond uni : tout ce qui, depuis les bords, ressemble a la couleur du coin."""
    rgb = np.asarray(img.convert("RGB"), dtype=np.int32)
    h, w, _ = rgb.shape
    fond = np.zeros((h, w), dtype=bool)
    graines = graines or [(0, 0), (w - 1, 0), (0, h // 2), (w - 1, h // 2)]
    for gx, gy in graines:
        ref = rgb[gy, gx]
        file = deque([(gx, gy)])
        while file:
            x, y = file.popleft()
            if x < 0 or y < 0 or x >= w or y >= h or fond[y, x]:
                continue
            if np.abs(rgb[y, x] - ref).sum() > tolerance:
                continue
            fond[y, x] = True
            file.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    alpha = np.where(fond, 0, 255).astype(np.uint8)
    out = img.convert("RGBA")
    out.putalpha(Image.fromarray(alpha).filter(ImageFilter.MinFilter(3)))
    return out


def nocturne(img, saturation=0.72, contraste=1.18, teinte=0.16):
    """Les couleurs de la nuit du jeu : un peu moins saturees, plus contrastees, les ombres tirees vers le bleu."""
    alpha = img.getchannel("A")
    rgb = ImageEnhance.Color(img.convert("RGB")).enhance(saturation)
    rgb = ImageEnhance.Contrast(rgb).enhance(contraste)
    a = np.asarray(rgb, dtype=np.float32)
    lum = a.mean(axis=2, keepdims=True) / 255
    a = a * (1 - teinte * (1 - lum)) + NUIT * 1.6 * teinte * (1 - lum)
    out = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8)).convert("RGBA")
    out.putalpha(alpha)
    return out


def reduire(img, largeur, hauteur, couleurs=30):
    """Reduit a la taille finale et a un nombre limite de couleurs, sans tramage : de vrais aplats de pixel art."""
    petit = img.resize((largeur, hauteur), Image.LANCZOS)
    alpha = np.asarray(petit.getchannel("A"))
    rgb = petit.convert("RGB").quantize(colors=couleurs, method=Image.MEDIANCUT, dither=Image.NONE).convert("RGB")
    out = rgb.convert("RGBA")
    out.putalpha(Image.fromarray(np.where(alpha >= 128, 255, 0).astype(np.uint8)))
    return out


def tracer(img, couleur=(16, 16, 22)):
    """Un trait d'un pixel la ou la silhouette touche le vide."""
    a = np.asarray(img).copy()
    plein = a[:, :, 3] >= 128
    bord = plein & ~(np.roll(plein, 1, 0) & np.roll(plein, -1, 0) & np.roll(plein, 1, 1) & np.roll(plein, -1, 1))
    bord[0, :] = bord[-1, :] = False                                  # le bas du buste reste coupe net
    a[bord] = list(couleur) + [255]
    return Image.fromarray(a)


def pixeliser(source, boite, largeur, hauteur, couleurs=30, tolerance=38, miroir=False, trait=True, detour=True):
    """Toute la chaine : decouper la boite (x0, y0, x1, y1) dans l'illustration, detourer, nocturne, reduire, tracer."""
    img = Image.open(source).convert("RGBA").crop(boite)
    if detour:
        img = detourer(img, tolerance)
    img = reduire(nocturne(img), largeur, hauteur, couleurs)
    if trait:
        img = tracer(img)
    return img.transpose(Image.FLIP_LEFT_RIGHT) if miroir else img
