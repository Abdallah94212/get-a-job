"""Facades de l'avenue : cinq familles d'immeubles releves sur place, et leurs commerces."""
import random
from outils import *

TRAVEE, RDC, ETAGE = 42, 56, 42  # en texels : 3 m, 4 m, 3 m
COQ = ("..#......", ".###..#.#", "####.####", ".########", "..######.", "...####..", "...#..#..")


def coq(t, x, y, c, echelle=1):
    for j, rangee in enumerate(COQ):
        for i, pixel in enumerate(rangee):
            if pixel == "#":
                t.rect(x + i * echelle, y + j * echelle, x + (i + 1) * echelle, y + (j + 1) * echelle, c)


def _etat(rng, part=0.17):
    tirage = rng.random()
    if tirage < part * 0.65:
        return "allume"
    if tirage < part * 0.82:
        return "tele"
    return "voile" if tirage < part else "eteint"


def vitre(t, x, y, w, h, etat, rng, croisee=True):
    if etat == "eteint":
        t.rect(x, y, x + w, y + h, VITRE)
        if rng.random() < 0.5:
            t.rect(x + 1, y + 1, x + w // 2, y + 2, VITRE_R)
    else:
        c = {"allume": ALLUME, "tele": TELE, "voile": ALLUME_F}[etat]
        t.rect(x, y, x + w, y + h, lum(c))
        t.rect(x, y + h * 2 // 3, x + w, y + h, lum(ton(c, 0.78)))
        if etat == "allume" and rng.random() < 0.5:
            t.rect(x, y, x + w // 3, y + h, lum(ton(c, 0.7)))
    if croisee:
        t.ligne([(x + w // 2, y), (x + w // 2, y + h - 1)], FER)
        t.ligne([(x, y + h // 3), (x + w - 1, y + h // 3)], FER)


def fenetre_volets(t, cx, y0, rng, mur_clair):
    """Fenetre des immeubles blancs : volets a lattes, ouverts ou fermes."""
    if rng.random() < 0.3:
        t.rect(cx - 7, y0 + 9, cx + 7, y0 + 34, VOLET)
        for y in range(y0 + 10, y0 + 34, 2):
            t.ligne([(cx - 7, y), (cx + 6, y)], VOLET_O)
        t.ligne([(cx, y0 + 9), (cx, y0 + 33)], VOLET_O)
    else:
        vitre(t, cx - 7, y0 + 9, 14, 25, _etat(rng), rng)
        for x0 in (cx - 13, cx + 8):
            t.rect(x0, y0 + 9, x0 + 5, y0 + 34, VOLET)
            for y in range(y0 + 10, y0 + 34, 2):
                t.ligne([(x0, y), (x0 + 4, y)], VOLET_O)
        if rng.random() < 0.25:
            garde_corps(t, cx - 7, y0 + 27, 14, 7)
    t.rect(cx - 8, y0 + 34, cx + 8, y0 + 36, mur_clair)


def cheminees(t, w, yc, yt, rng, c):
    for _ in range(max(1, w // 110)):
        x, haut = rng.randrange(6, w - 16), yc + rng.randrange(0, 5)
        t.rect(x, haut + 3, x + 9, yt, c)
        t.rect(x - 1, haut + 3, x + 10, haut + 5, ton(c, 1.2))
        t.rect(x + 1, haut, x + 3, haut + 3, FER)
        t.rect(x + 5, haut + 1, x + 7, haut + 3, FER)


def _haussmann(t, w, yc, yt, ye, etages, rng, pierre):
    """Brique et pierre 1900, ou pierre de taille : balcons filants et toit d'ardoise."""
    mur, joint = (PIERRE, PIERRE_O) if pierre else (BRIQUE, BRIQUE_J)
    yr = ye + etages * ETAGE
    t.rect(0, ye, w, yr, mur)
    for y in range(ye + 2, yr, 4):
        t.ligne([(0, y), (w - 1, y)], joint)
    t.rect(0, ye, 5, yr, PIERRE)
    t.rect(w - 5, ye, w, yr, PIERRE)
    for e in range(etages):
        y0, niveau = ye + e * ETAGE, etages - e
        t.rect(0, y0 + ETAGE - 3, w, y0 + ETAGE, PIERRE_C)
        for b in range(w // TRAVEE):
            cx = b * TRAVEE + TRAVEE // 2
            t.rect(cx - 10, y0 + 5, cx + 10, y0 + ETAGE - 3, PIERRE_C)
            t.rect(cx - 11, y0 + 3, cx + 11, y0 + 5, PIERRE)
            vitre(t, cx - 7, y0 + 8, 14, 28, _etat(rng), rng)
            garde_corps(t, cx - 9, y0 + 29, 18, 7)
        if niveau in (2, 5):
            t.rect(0, y0 + ETAGE - 5, w, y0 + ETAGE - 2, PIERRE_C)
            garde_corps(t, 0, y0 + 27, w, 10)
    t.rect(0, yt, w, ye, ARDOISE)
    for y in range(yt + 3, ye, 3):
        t.ligne([(0, y), (w - 1, y)], ARDOISE_O)
    t.rect(0, yt, w, yt + 2, ZINC)
    t.rect(0, ye - 3, w, ye, PIERRE_C)
    for b in range(w // TRAVEE):
        cx = b * TRAVEE + TRAVEE // 2
        t.rect(cx - 7, yt + 8, cx + 7, ye - 4, ZINC)
        vitre(t, cx - 5, yt + 10, 10, ye - yt - 15, _etat(rng, 0.12), rng, croisee=False)
    cheminees(t, w, yc, yt, rng, BRIQUE)


def _blanc(t, w, yc, yt, ye, etages, rng):
    """Immeuble blanc des annees 1930-1950, a volets sombres."""
    yr = ye + etages * ETAGE
    t.rect(0, ye, w, yr, MUR_BLANC)
    for e in range(etages):
        y0 = ye + e * ETAGE
        t.ligne([(0, y0 + ETAGE - 1), (w - 1, y0 + ETAGE - 1)], MUR_BLANC_O)
        for b in range(w // TRAVEE):
            fenetre_volets(t, b * TRAVEE + TRAVEE // 2, y0, rng, MUR_BLANC_C)
    t.rect(0, yt, w, ye, MUR_BLANC)
    t.rect(0, yt, w, yt + 3, MUR_BLANC_C)
    t.ligne([(0, ye - 1), (w - 1, ye - 1)], MUR_BLANC_O)
    cheminees(t, w, yc, yt, rng, MUR_BLANC_O)


def _barre(t, w, yc, yt, ye, etages, rng):
    """Barre moderne des annees 1970 : bandeaux de beton et de vitres, loggias."""
    for e in range(etages):
        y0 = ye + e * ETAGE
        t.rect(0, y0, w, y0 + 16, BETON)
        t.ligne([(0, y0), (w - 1, y0)], BETON_C)
        t.ligne([(0, y0 + 15), (w - 1, y0 + 15)], BETON_O)
        t.rect(0, y0 + 16, w, y0 + ETAGE, VITRE)
        for b in range(w // TRAVEE):
            x0 = b * TRAVEE
            if b % 4 == 3:
                t.rect(x0 + 2, y0 + 16, x0 + TRAVEE - 2, y0 + ETAGE, ton(VITRE, 0.7))
                if rng.random() < 0.22:
                    t.rect(x0 + 8, y0 + 18, x0 + 24, y0 + ETAGE - 11, lum(ALLUME_F))
                garde_corps(t, x0 + 2, y0 + ETAGE - 11, TRAVEE - 4, 10)
                continue
            for k in range(3):
                xs, etat = x0 + k * 14, _etat(rng, 0.15)
                if etat != "eteint":
                    c = {"allume": ALLUME, "tele": TELE, "voile": ALLUME_F}[etat]
                    t.rect(xs + 1, y0 + 17, xs + 14, y0 + ETAGE - 1, lum(c))
                    t.rect(xs + 1, y0 + 32, xs + 14, y0 + ETAGE - 1, lum(ton(c, 0.75)))
                t.ligne([(xs, y0 + 16), (xs, y0 + ETAGE - 1)], BETON_O)
    t.rect(0, yt, w, ye, BETON_O)
    t.rect(0, yt, w, yt + 2, BETON_C)
    for _ in range(2):
        x = rng.randrange(8, max(9, w - 40))
        t.rect(x, yc + 2, x + 26, yt, BETON_O)
        t.rect(x, yc + 2, x + 26, yc + 4, BETON)


def _verre(t, w, yc, yt, ye, etages, rng):
    """Les bureaux de la Krousty : mur-rideau vitre et bannieres verticales orange."""
    yr, pas = ye + etages * ETAGE, 14
    for y in range(ye, yr, pas):
        for x in range(0, w, pas):
            t.rect(x, y, x + pas, y + pas, rng.choice(VERRES))
    for y in range(ye, yr, pas):
        if rng.random() < 0.32:
            debut = rng.randrange(0, w // pas)
            for k in range(debut, min(w // pas, debut + rng.randrange(2, 8))):
                t.rect(k * pas, y, (k + 1) * pas, y + pas, lum(ton(BUREAU, rng.choice((1, 1, 0.82)))))
                t.rect(k * pas + 3, y + 9, k * pas + 10, y + pas, lum(ton(BUREAU, 0.55)))
    for x in range(0, w, pas):
        t.ligne([(x, ye), (x, yr - 1)], MENEAU)
    for y in range(ye, yr, pas):
        t.ligne([(0, y), (w - 1, y)], MENEAU)
    for y in range(ye, yr, ETAGE):
        t.rect(0, y, w, y + 2, ton(MENEAU, 1.3))
    for x in range(TRAVEE * 2, w - TRAVEE, TRAVEE * 4):
        t.rect(x, ye + 12, x + 5, yr - 6, lum(ORANGE_S))
        t.rect(x + 1, ye + 12, x + 3, yr - 6, lum(ORANGE))
    t.rect(0, yt, w, ye, (26, 26, 34))
    t.rect(0, yt, w, yt + 1, MENEAU)
    for x in range(TRAVEE, w - 150, TRAVEE * 10):
        t.texte(x, yt + 1, "KROUSTY CORP", lum(ORANGE), 5, 2)


def _maison(t, w, yc, yt, ye, etages, rng):
    """Petites maisons de faubourg accolees, de hauteurs inegales."""
    x, yr = 0, ye + etages * ETAGE
    while x < w:
        n = min(rng.choice((2, 2, 3)), (w - x) // TRAVEE)
        lw, niveaux = n * TRAVEE, max(1, etages - rng.choice((0, 0, 1)))
        mur = rng.choice((MUR_BLANC, PIERRE, ton(MUR_BLANC, 0.85)))
        haut = yr - niveaux * ETAGE
        t.rect(x, haut, x + lw, yr, mur)
        t.rect(x, haut - 18, x + lw, haut, TUILE)
        for y in range(haut - 15, haut, 3):
            t.ligne([(x, y), (x + lw - 1, y)], ton(TUILE, 0.8))
        t.rect(x, haut - 18, x + lw, haut - 16, ton(TUILE, 1.3))
        t.rect(x, haut - 2, x + lw, haut, ton(mur, 1.15))
        t.rect(x + 5, haut - 28, x + 12, haut - 18, BRIQUE)
        t.rect(x + 4, haut - 29, x + 13, haut - 27, ton(BRIQUE, 1.2))
        for e in range(niveaux):
            for b in range(n):
                fenetre_volets(t, x + b * TRAVEE + TRAVEE // 2, haut + e * ETAGE, rng, ton(mur, 1.15))
        t.ligne([(x, haut), (x, yr - 1)], ton(mur, 0.75))
        x += lw


def _atelier(t, w, yc, yt, ye, etages, rng):
    """Mur d'atelier en brique, perce de petites fenetres a barreaux."""
    yr = ye + etages * ETAGE
    t.rect(0, ye, w, yr, BRIQUE)
    for y in range(ye + 2, yr, 4):
        t.ligne([(0, y), (w - 1, y)], BRIQUE_J)
    for b in range(w // TRAVEE):
        cx = b * TRAVEE + TRAVEE // 2
        t.rect(cx - 9, ye + 10, cx + 9, ye + 24, VITRE)
        for xb in range(cx - 9, cx + 9, 3):
            t.ligne([(xb, ye + 10), (xb, ye + 23)], FER)
        t.rect(cx - 10, ye + 24, cx + 10, ye + 26, PIERRE_C)
    t.rect(0, yt, w, ye, BRIQUE_J)
    t.rect(0, yt, w, yt + 2, ZINC)


# toit et souche de cheminee, en texels, puis le dessin des etages
STYLES = {
    "brique": (34, 14, lambda *a: _haussmann(*a, False)), "pierre": (34, 14, lambda *a: _haussmann(*a, True)),
    "blanc": (12, 14, _blanc), "barre": (10, 12, _barre), "verre": (16, 0, _verre),
    "maison": (30, 0, _maison), "atelier": (8, 0, _atelier),
}
SOCLES = {"brique": PIERRE, "pierre": PIERRE, "blanc": MUR_BLANC_O, "barre": BETON_O, "verre": (34, 36, 46),
          "maison": PIERRE_O, "atelier": BRIQUE_J}


def hauteur_facade(style, etages):
    toit, souche, _ = STYLES[style]
    return souche + toit + etages * ETAGE + RDC


# ---------------------------------------------------------------- commerces
def bandeau(t, x, y, w, nom, style="sombre"):
    """L'enseigne en bandeau, au-dessus de la vitrine."""
    fonds = {"sombre": (36, 36, 44), "clair": lum((226, 224, 214)), "froid": lum((206, 214, 224)),
             "orange": (40, 32, 30), "blanc": lum((240, 236, 224)), "peint": None}
    encres = {"sombre": lum((214, 208, 192)), "clair": (32, 32, 40), "froid": (32, 32, 40),
              "orange": lum(ORANGE), "blanc": lum((232, 100, 8)), "peint": (154, 150, 144)}
    if fonds[style]:
        t.rect(x + 1, y + 1, x + w - 1, y + 13, fonds[style])
    police = 5 if largeur_texte(nom, 5) <= w - 6 else 3
    t.texte_centre(x, y + (4 if police == 5 else 5), w, nom, encres[style], police)


def rideau(t, x, y, w, h, rng, nombre_tags=2, c=RIDEAU):
    """Rideau de fer baisse."""
    t.rect(x - 1, y - 1, x + w + 1, y + h, FER)
    t.rect(x, y, x + w, y + h, c)
    for yy in range(y + 2, y + h, 3):
        t.ligne([(x, yy), (x + w - 1, yy)], ton(c, 0.75))
    t.rect(x + w // 2 - 3, y + h - 5, x + w // 2 + 3, y + h - 1, FER)
    tags(t, x + 2, y + 3, w - 4, h - 9, rng, nombre_tags)


def vitrine(t, x, y, w, h, teinte, contenu, rng):
    """Vitrine eclairee : ce qu'il y a dedans se decoupe a contre-jour."""
    def ombre(k):
        return lum(ton(teinte, k))

    t.rect(x - 1, y - 1, x + w + 1, y + h + 1, FER)
    for i in range(h):
        t.rect(x, y + i, x + w, y + i + 1, ombre(1.0 - 0.28 * i / h))
    if contenu == "etals":
        for ys in range(y + 3, y + h - 6, 9):
            t.ligne([(x, ys + 7), (x + w - 1, ys + 7)], ombre(0.35))
            xx = x + 2
            while xx < x + w - 4:
                lw, hh = rng.randrange(2, 5), rng.randrange(3, 7)
                t.rect(xx, ys + 7 - hh, xx + lw, ys + 7, ombre(rng.choice((0.35, 0.5, 0.65, 0.8))))
                xx += lw + rng.randrange(1, 3)
    elif contenu == "machines":
        cote = min(13, h // 2 - 3)
        for rangee in range(2):
            ym = y + 3 + rangee * (cote + 3)
            for xm in range(x + 3, x + w - cote, cote + 2):
                t.rect(xm, ym, xm + cote, ym + cote, ombre(0.8))
                t.rect(xm + 1, ym + 1, xm + cote - 1, ym + 3, ombre(0.55))
                t.ellipse(xm + 2, ym + 4, xm + cote - 2, ym + cote, ombre(0.3))
                t.ellipse(xm + 4, ym + 6, xm + cote - 4, ym + cote - 2, ombre(0.5))
    elif contenu == "bar":
        t.rect(x, y + h * 3 // 5, x + w, y + h, ombre(0.4))
        t.rect(x + 2, y + 6, x + w - 2, y + 7, ombre(0.5))
        for xx in range(x + 4, x + w - 4, 3):
            t.rect(xx, y + 6 - rng.randrange(2, 5), xx + 1, y + 6, ombre(rng.choice((0.3, 0.6, 0.9))))
        for xx in range(x + 8, x + w - 8, 12):
            t.rect(xx, y + h * 3 // 5 - 6, xx + 5, y + h * 3 // 5 - 5, ombre(0.25))
            t.ligne([(xx + 2, y + h * 3 // 5 - 5), (xx + 2, y + h - 1)], ombre(0.25))
    elif contenu == "salle":
        for xx in range(x + 5, x + w - 12, 16):
            t.rect(xx, y + h - 14, xx + 9, y + h - 12, ombre(0.3))
            t.ligne([(xx + 4, y + h - 22), (xx + 4, y + h - 1)], ombre(0.3))
            t.ellipse(xx + 1, y + h - 26, xx + 8, y + h - 19, ombre(0.3))


def b_vitrine(t, x, y, w, nom, b, rng):
    bandeau(t, x, y, w, nom, b.get("enseigne", "sombre"))
    teinte, y0 = b.get("teinte", ALLUME), y + 15
    if b.get("store"):
        t.rect(x, y + 14, x + w, y + 19, (52, 52, 62))
        for xx in range(x, x + w, 4):
            t.rect(xx, y + 19, xx + 2, y + 21, (52, 52, 62))
        y0 = y + 22
    h, porte = y + RDC - 4 - y0, 16
    a_gauche = b.get("porte", "g") == "g"
    xp = x + 3 if a_gauche else x + w - 3 - porte
    vitrine(t, x + 3 + (porte + 2 if a_gauche else 0), y0, w - 8 - porte, h, teinte, b.get("contenu", "etals"), rng)
    t.rect(xp, y0 - 1, xp + porte, y + RDC - 1, FER)
    t.rect(xp + 2, y0 + 1, xp + porte - 2, y + RDC - 3, lum(ton(teinte, 0.85)))
    t.rect(xp + porte - 5, y0 + h // 2, xp + porte - 4, y0 + h // 2 + 5, FER)


def b_rideau(t, x, y, w, nom, b, rng):
    if nom:
        bandeau(t, x, y, w, nom, b.get("enseigne", "peint"))
    rideau(t, x + 3, y + 15, w - 6, RDC - 18, rng, b.get("tags", 2))


def b_krousty(t, x, y, w, nom, b, rng):
    """Un commerce rachete : rideau neuf, enseigne orange, affiche d'ouverture."""
    bandeau(t, x, y, w, "KROUSTY", "orange")
    coq_x = x + (w - largeur_texte("KROUSTY")) // 2 - 12
    t.rect(coq_x, y + 4, coq_x + 7, y + 11, lum(ORANGE))
    t.texte(coq_x + 1, y + 4, "K", (40, 32, 30), 5)
    rideau(t, x + 3, y + 15, w - 6, RDC - 18, rng, 0, (96, 98, 106))
    ax = x + w // 2 - 15
    t.rect(ax, y + 22, ax + 30, y + 40, lum(ORANGE_S))
    t.texte_centre(ax, y + 25, 30, "BIENTOT", (30, 26, 26), 3)
    t.texte_centre(ax, y + 32, 30, "ICI", (30, 26, 26), 3)


def b_porte(t, x, y, w, nom, b, rng):
    """Entree d'immeuble, avec son numero sur une plaque."""
    cx = x + w // 2
    t.rect(cx - 12, y + 9, cx + 12, y + RDC, PIERRE_C)
    t.rect(cx - 9, y + 12, cx + 9, y + RDC - 1, FER)
    verre = lum(ALLUME_F) if b.get("hall") else VITRE
    t.rect(cx - 7, y + 14, cx - 1, y + RDC - 13, verre)
    t.rect(cx + 1, y + 14, cx + 7, y + RDC - 13, verre)
    t.rect(cx - 7, y + RDC - 11, cx + 7, y + RDC - 2, (46, 46, 54))
    t.rect(cx + 13, y + 30, cx + 16, y + 36, (150, 150, 156))
    if nom:
        lw = largeur_texte(nom, 3) + 4
        t.rect(cx - lw // 2, y + 1, cx - lw // 2 + lw, y + 9, (40, 46, 64))
        t.texte(cx - lw // 2 + 2, y + 3, nom, BLANC, 3)
    affichettes(t, x + 2, y + 20, cx - 16 - x, 26, rng, 2)
    tags(t, cx + 15, y + 38, x + w - cx - 17, 14, rng, 1)


def b_mur(t, x, y, w, nom, b, rng):
    affichettes(t, x + 3, y + 14, w - 6, 30, rng, max(2, w // 14))
    tags(t, x + 2, y + 8, w - 4, 40, rng, 2)


def b_banque(t, x, y, w, nom, b, rng):
    bandeau(t, x, y, w, nom, "clair")
    t.rect(x + 4, y + 17, x + w - 26, y + RDC - 4, VITRE)
    t.rect(x + 5, y + 18, x + w // 2, y + 19, VITRE_R)
    gx = x + w - 22
    t.rect(gx, y + 20, gx + 18, y + 46, (60, 62, 72))
    t.rect(gx + 3, y + 23, gx + 15, y + 31, lum(BUREAU))
    for k in range(3):
        t.rect(gx + 4 + k * 4, y + 35, gx + 6 + k * 4, y + 37, (150, 150, 156))
        t.rect(gx + 4 + k * 4, y + 39, gx + 6 + k * 4, y + 41, (150, 150, 156))


def b_poulet(t, x, y, w, nom, b, rng):
    """Le Maitre Poulet : grand bandeau blanc eclaire, lettres et coq orange, comptoir ouvert sur la rue."""
    encre = lum((232, 100, 8))
    t.rect(x + 1, y, x + w - 1, y + 15, lum((240, 236, 224)))
    total = largeur_texte("MAITRE") + 6 + 9 + 6 + largeur_texte("POULET")
    tx = x + (w - total) // 2
    tx += t.texte(tx, y + 4, "MAITRE", encre) + 6
    coq(t, tx, y + 4, encre)
    t.texte(tx + 15, y + 4, "POULET", encre)
    t.rect(x + 1, y + 15, x + w - 1, y + 18, (150, 150, 156))
    t.texte_centre(x, y + 15, w, "", BLANC, 3)
    chaud, y0, h = (246, 228, 184), y + 19, RDC - 23
    for i in range(h):
        t.rect(x + 3, y0 + i, x + w - 3, y0 + i + 1, lum(ton(chaud, 1.0 - 0.22 * i / h)))
    t.rect(x + 2, y0 - 1, x + 3, y + RDC - 1, FER)
    t.rect(x + w - 3, y0 - 1, x + w - 2, y + RDC - 1, FER)
    # la rotissoire : trois broches de poulets dores
    rx = x + 6
    t.rect(rx, y0 + 2, rx + 34, y0 + 26, lum(ton(chaud, 0.5)))
    for k in range(3):
        yb = y0 + 6 + k * 7
        t.ligne([(rx + 1, yb + 2), (rx + 32, yb + 2)], lum(ton(chaud, 0.25)))
        for xx in range(rx + 3, rx + 29, 7):
            t.ellipse(xx, yb, xx + 6, yb + 5, lum((214, 150, 84)))
            t.rect(xx + 1, yb + 1, xx + 3, yb + 2, lum((246, 200, 130)))
    # les menus lumineux au-dessus du comptoir
    for k in range(3):
        mx = x + 46 + k * 17
        t.rect(mx, y0 + 2, mx + 15, y0 + 12, lum((250, 244, 220)))
        t.rect(mx + 2, y0 + 4, mx + 7, y0 + 7, encre)
        t.rect(mx + 9, y0 + 4, mx + 13, y0 + 5, lum(ton(chaud, 0.4)))
        t.rect(mx + 9, y0 + 7, mx + 13, y0 + 8, lum(ton(chaud, 0.4)))
    # le comptoir, la caisse, la porte
    t.rect(x + 3, y0 + h - 11, x + w - 22, y0 + h, lum(ton(chaud, 0.45)))
    t.rect(x + 3, y0 + h - 12, x + w - 22, y0 + h - 11, lum(ton(chaud, 0.7)))
    t.rect(x + 50, y0 + h - 18, x + 58, y0 + h - 12, lum(ton(chaud, 0.3)))
    t.rect(x + w - 20, y0 - 1, x + w - 3, y + RDC - 1, FER)
    t.rect(x + w - 18, y0 + 1, x + w - 5, y + RDC - 3, lum(ton(chaud, 0.9)))
    t.rect(x + w - 8, y0 + 14, x + w - 7, y0 + 19, FER)


BOUTIQUES = {"vitrine": b_vitrine, "rideau": b_rideau, "porte": b_porte, "mur": b_mur, "banque": b_banque,
             "poulet": b_poulet, "krousty": b_krousty}


def facade(style, travees, etages, boutiques, graine, acte=3):
    """Dessine une facade entiere. Les boutiques se suivent de gauche a droite et couvrent tout le rez-de-chaussee."""
    rng = random.Random(graine)
    toit, souche, dessin = STYLES[style]
    w, h = travees * TRAVEE, hauteur_facade(style, etages)
    t = Toile(w, h)
    yt, ye, yr = souche, souche + toit, h - RDC
    dessin(t, w, 0, yt, ye, etages, rng)
    if style != "verre":
        for x in range(TRAVEE, w, TRAVEE * rng.choice((3, 4))):
            t.ligne([(x, ye), (x, yr - 1)], FER)
        for _ in range(travees // 3):
            cx = rng.randrange(travees) * TRAVEE + TRAVEE // 2 + 12
            cy = ye + rng.randrange(etages) * ETAGE + 26
            t.rect(cx, cy, cx + 8, cy + 7, (96, 98, 104))
            t.ellipse(cx + 1, cy + 1, cx + 7, cy + 7, (54, 56, 64))
        for _ in range(travees * 2):
            sx, sy = rng.randrange(w), ye + rng.randrange(etages * ETAGE - 20)
            for k in range(rng.randrange(6, 16)):
                r, g, b, a = t.img.getpixel((sx, sy + k))
                if a == 255:
                    t.point(sx, sy + k, (max(0, r - 12), max(0, g - 12), max(0, b - 12)))
    t.rect(0, yr, w, h, SOCLES[style])
    t.rect(0, yr, w, yr + 1, ton(SOCLES[style], 0.7))
    x = 0
    for b in boutiques:
        lw = b["travees"] * TRAVEE
        genre = "krousty" if b.get("rachat", 99) <= acte else b["type"]
        BOUTIQUES[genre](t, x, yr, lw, b.get("nom", ""), b, rng)
        x += lw
    assert x == w, f"les boutiques couvrent {x} texels sur {w}"
    t.rect(0, h - 3, w, h, ton(SOCLES[style], 0.6))
    t.assombrir_haut(0.6, yr)
    t.grain(5, graine)
    return t.img


def drapeau(texte, style="sombre", police=5):
    """Enseigne verticale en drapeau, lisible de face quand on remonte l'avenue."""
    pas = police + 2
    w, h = police + 6, len(texte) * pas + 5
    t = Toile(w, h)
    t.rect(0, 0, w, h, FER)
    fond = {"sombre": (36, 36, 44), "clair": lum((226, 224, 214)), "froid": lum((206, 214, 224)),
            "orange": (40, 32, 30)}[style]
    encre = {"sombre": lum((214, 208, 192)), "clair": (32, 32, 40), "froid": (32, 32, 40), "orange": lum(ORANGE)}[style]
    t.rect(1, 1, w - 1, h - 1, fond)
    for k, lettre in enumerate(texte):
        t.texte(3, 3 + k * pas, lettre, encre, police)
    return t.img
