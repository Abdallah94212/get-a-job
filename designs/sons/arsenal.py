"""Les sons de l'arsenal : quatre detonations qui ne se ressemblent pas (le pistolet sec, le pistolet lourd qui cogne, la
mitraillette qui crepite, le fusil qui claque et roule dans la rue), la mecanique (degainer, chargeur qui sort, qui
entre, culasse, clic a vide, douilles), la lame (elle siffle, elle mord), le metal d'une batte, les balles qui
ricochent ou s'ecrasent dans un mur, et le coup qui touche quelqu'un. Fabriques ici comme ceux d'acte1.py.
Lancer avec python3.14 depuis designs/sons :
    python3.14 arsenal.py
"""
from pathlib import Path

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 22050
RACINE = Path(__file__).resolve().parents[2] / "jeu" / "ressources" / "sons"
rng = np.random.default_rng(2207)


def t(d):
    return np.arange(int(d * SR)) / SR


def bande(x, b, h):
    return signal.sosfilt(signal.butter(2, [b, min(h, SR / 2 - 200)], "band", fs=SR, output="sos"), x)


def bas(x, f):
    return signal.sosfilt(signal.butter(2, f, "low", fs=SR, output="sos"), x)


def haut(x, f):
    return signal.sosfilt(signal.butter(2, f, "high", fs=SR, output="sos"), x)


def env(n, attaque, chute):
    e = np.ones(n)
    a = max(1, int(attaque * SR))
    e[:a] = np.linspace(0, 1, a)
    e *= np.exp(-np.arange(n) / (chute * SR))
    return e


def bruit(d):
    return rng.normal(size=int(d * SR))


def poser(x, y, a):
    i = int(a * SR)
    fin = min(len(x), i + len(y))
    x[i:fin] += y[:fin - i]
    return x


def ecrire(dossier, nom, x, db=-6):
    (RACINE / dossier).mkdir(parents=True, exist_ok=True)
    x = x * (10 ** (db / 20) / (np.abs(x).max() + 1e-9))
    wavfile.write(RACINE / dossier / f"{nom}.wav", SR, (x * 32767).astype(np.int16))


def coup_de_feu(duree, grave, claque, queue, saturation):
    """Une detonation : le claquement (bruit large, tres court), le corps grave (une sinusoide qui plonge), et la queue :
    l'echo de la rue, filtre, qui s'eteint lentement."""
    n = int(duree * SR)
    x = np.zeros(n)
    c = bande(bruit(0.03), 1200, 9000) * env(int(0.03 * SR), 0.0005, 0.006) * claque
    poser(x, c, 0)
    d = t(0.18)
    f = grave * (1 + 2.5 * np.exp(-d * 40))
    corps = np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(d), 0.001, 0.05)
    poser(x, corps, 0.002)
    q = bas(bruit(duree), 1400) * env(n, 0.004, queue) * 0.35
    poser(x, q, 0.01)
    x = np.tanh(x * saturation)
    for retard, k in ((0.07, 0.25), (0.16, 0.12)):                       # les facades renvoient le coup
        poser(x, bas(x, 900) * k, retard)
    return x


def pistolet():
    return coup_de_feu(0.5, 140, 1.0, 0.07, 2.2)


def pistolet_lourd():
    return coup_de_feu(0.75, 90, 1.2, 0.12, 3.0)


def mitraillette():
    return coup_de_feu(0.22, 170, 0.8, 0.035, 2.0)


def fusil():
    x = coup_de_feu(0.9, 110, 1.3, 0.16, 3.2)
    return poser(x, bande(bruit(0.02), 3000, 8000) * env(int(0.02 * SR), 0.0005, 0.004) * 0.4, 0.004)


def clic(f=3200, d=0.025, k=1.0):
    return bande(bruit(d), f * 0.6, f * 1.6) * env(int(d * SR), 0.0005, d / 5) * k


def degaine():
    """La main sur la crosse, l'arme qui sort du jean : un frottement, un petit cliquetis."""
    x = np.zeros(int(0.3 * SR))
    poser(x, bande(bruit(0.16), 900, 5000) * env(int(0.16 * SR), 0.03, 0.05) * 0.5, 0)
    poser(x, clic(4200), 0.17)
    return x


def chargeur_sort():
    x = np.zeros(int(0.35 * SR))
    poser(x, clic(2600, 0.03), 0)
    poser(x, bande(bruit(0.12), 600, 3000) * env(int(0.12 * SR), 0.01, 0.04) * 0.4, 0.04)
    return x


def chargeur_entre():
    x = np.zeros(int(0.3 * SR))
    poser(x, bande(bruit(0.05), 500, 2500) * env(int(0.05 * SR), 0.005, 0.02) * 0.5, 0)
    poser(x, clic(2000, 0.04, 1.4), 0.06)
    return x


def culasse():
    """La culasse qu'on tire puis qu'on lache : deux claquements metalliques, le second plus net."""
    x = np.zeros(int(0.32 * SR))
    poser(x, clic(1800, 0.05), 0)
    d = t(0.08)
    metal = sum(np.sin(2 * np.pi * f * d) for f in (2300, 3700, 5100)) * env(len(d), 0.0005, 0.02) * 0.4
    poser(x, metal, 0.14)
    poser(x, clic(3000, 0.05, 1.3), 0.14)
    return x


def vide():
    """Le chien qui tombe sur rien : un clic sec, petit, ridicule."""
    x = np.zeros(int(0.15 * SR))
    return poser(x, clic(3600, 0.02, 1.0), 0)


def douille():
    """Une douille de laiton qui rebondit sur le bitume."""
    x = np.zeros(int(0.5 * SR))
    for k, (a, g) in enumerate(((0.0, 1.0), (0.11, 0.6), (0.19, 0.35), (0.25, 0.2))):
        d = t(0.05)
        ton = sum(np.sin(2 * np.pi * f * d) for f in (4200, 6100, 7900)) * env(len(d), 0.0003, 0.012)
        poser(x, ton * g, a)
    return x * 0.6


def lame():
    """Une lame qui fend l'air : un souffle qui monte et retombe, une pointe metallique au milieu."""
    n = int(0.28 * SR)
    b = bruit(0.28)
    e = np.sin(np.linspace(0, np.pi, n)) ** 2
    x = bande(b, 1800, 7000) * e
    d = t(0.28)
    x += np.sin(2 * np.pi * (3000 + 2000 * d) * d) * e * 0.12
    return x


def lame_touche():
    """La lame qui mord : un choc sourd et un tintement qui reste."""
    x = np.zeros(int(0.5 * SR))
    poser(x, bas(bruit(0.06), 1500) * env(int(0.06 * SR), 0.001, 0.02), 0)
    d = t(0.45)
    poser(x, sum(np.sin(2 * np.pi * f * d) * k for f, k in ((2800, 1), (4500, 0.6), (6900, 0.3))) * env(len(d), 0.001, 0.12) * 0.35, 0.005)
    return x


def lame_relique():
    """La relique : le meme sifflement, et dessous un accord qui chante, comme du verre frotte."""
    x = np.zeros(int(0.7 * SR))
    poser(x, lame(), 0)
    d = t(0.65)
    accord = sum(np.sin(2 * np.pi * f * d + np.sin(2 * np.pi * 6 * d) * 0.6) for f in (660, 990, 1320, 1760))
    poser(x, accord * env(len(d), 0.03, 0.2) * 0.12, 0.02)
    return x


def batte():
    """Le bois ou l'alu qui rencontre quelqu'un : un toc creux, grave."""
    x = np.zeros(int(0.35 * SR))
    d = t(0.25)
    poser(x, np.sin(2 * np.pi * 180 * d) * env(len(d), 0.001, 0.05), 0)
    poser(x, bande(bruit(0.04), 600, 3000) * env(int(0.04 * SR), 0.0005, 0.01), 0)
    return x


def ricochet():
    """La balle qui touche un mur et repart : un claquement, puis le sifflement qui descend."""
    x = np.zeros(int(0.45 * SR))
    poser(x, clic(2500, 0.03, 1.2), 0)
    d = t(0.35)
    f = 3200 * np.exp(-d * 3)
    poser(x, np.sin(2 * np.pi * np.cumsum(f) / SR) * env(len(d), 0.01, 0.12) * 0.3, 0.02)
    return x


def impact_mur():
    """La balle dans le platre : un choc et des gravats."""
    x = np.zeros(int(0.3 * SR))
    poser(x, bande(bruit(0.04), 400, 3000) * env(int(0.04 * SR), 0.0005, 0.01), 0)
    for k in range(5):
        poser(x, clic(1500 + k * 400, 0.015, 0.4), 0.03 + k * 0.03 + rng.random() * 0.02)
    return x


def impact_chair():
    """Une balle qui touche quelqu'un : un choc mat, sourd, sans rien de joli."""
    x = np.zeros(int(0.25 * SR))
    poser(x, bas(bruit(0.06), 700) * env(int(0.06 * SR), 0.001, 0.02) * 1.2, 0)
    d = t(0.1)
    poser(x, np.sin(2 * np.pi * 95 * d) * env(len(d), 0.001, 0.03), 0)
    return x


def garde():
    """Les avant-bras qui encaissent : un choc etouffe."""
    x = np.zeros(int(0.2 * SR))
    poser(x, bas(bruit(0.05), 900) * env(int(0.05 * SR), 0.001, 0.015), 0)
    return x


def contre():
    """Le contre : un claquement net, et un petit accord qui dit "bien joue"."""
    x = np.zeros(int(0.4 * SR))
    poser(x, bande(bruit(0.03), 1500, 8000) * env(int(0.03 * SR), 0.0005, 0.006), 0)
    d = t(0.3)
    poser(x, (np.sin(2 * np.pi * 1320 * d) + np.sin(2 * np.pi * 1980 * d) * 0.6) * env(len(d), 0.002, 0.07) * 0.3, 0.01)
    return x


def esquive():
    """Il plonge de cote : un froissement rapide de tissu."""
    n = int(0.22 * SR)
    return bande(bruit(0.22), 700, 5000) * np.sin(np.linspace(0, np.pi, n)) ** 1.5


def plongeon():
    """L'attaque qui descend et frappe le sol : un souffle, puis le bitume qui tremble."""
    x = np.zeros(int(0.6 * SR))
    poser(x, bande(bruit(0.15), 400, 3000) * np.linspace(0, 1, int(0.15 * SR)) * 0.5, 0)
    d = t(0.4)
    poser(x, np.sin(2 * np.pi * 60 * d) * env(len(d), 0.001, 0.1) + bas(bruit(0.4), 400) * env(len(d), 0.001, 0.08) * 0.6, 0.15)
    return x


if __name__ == "__main__":
    ecrire("armes", "tir_pistolet", pistolet(), -5)
    ecrire("armes", "tir_lourd", pistolet_lourd(), -4)
    ecrire("armes", "tir_mitraillette", mitraillette(), -7)
    ecrire("armes", "tir_fusil", fusil(), -4)
    ecrire("armes", "degaine", degaine(), -12)
    ecrire("armes", "chargeur_sort", chargeur_sort(), -11)
    ecrire("armes", "chargeur_entre", chargeur_entre(), -10)
    ecrire("armes", "culasse", culasse(), -10)
    ecrire("armes", "vide", vide(), -12)
    ecrire("armes", "douille", douille(), -18)
    ecrire("armes", "lame", lame(), -10)
    ecrire("armes", "lame_touche", lame_touche(), -8)
    ecrire("armes", "lame_relique", lame_relique(), -8)
    ecrire("armes", "batte", batte(), -7)
    ecrire("armes", "ricochet", ricochet(), -12)
    ecrire("armes", "impact_mur", impact_mur(), -11)
    ecrire("armes", "impact_chair", impact_chair(), -8)
    ecrire("corps", "garde_bloque", garde(), -9)
    ecrire("corps", "contre", contre(), -9)
    ecrire("corps", "esquive", esquive(), -12)
    ecrire("corps", "plongeon", plongeon(), -6)
    print("arsenal : 21 sons")
