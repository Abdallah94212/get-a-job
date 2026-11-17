"""Les sons de l'acte 1 refait : le telephone de Geto (sonnerie, decroche, raccroche), les sacs qu'on se passe de main
en main, et l'inventaire (un objet entre, un objet sort). Fabriques ici, comme ceux de bruitages.py : aucun
enregistrement libre ne collait exactement a ces gestes. Lancer avec python3.14 depuis designs/sons :
    python3.14 acte1.py
"""
from pathlib import Path

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 22050
RACINE = Path(__file__).resolve().parents[2] / "jeu" / "ressources" / "sons"
rng = np.random.default_rng(1101)


def t(d):
    return np.arange(int(d * SR)) / SR


def bande(x, bas, haut):
    return signal.sosfilt(signal.butter(2, [bas, min(haut, SR / 2 - 200)], "band", fs=SR, output="sos"), x)


def bas(x, f):
    return signal.sosfilt(signal.butter(2, f, "low", fs=SR, output="sos"), x)


def enveloppe(n, attaque, chute):
    e = np.ones(n)
    a = max(1, int(attaque * SR))
    e[:a] = np.linspace(0, 1, a)
    e *= np.exp(-np.arange(n) / (chute * SR))
    return e


def ecrire(dossier, nom, x, db=-6):
    x = x * (10 ** (db / 20) / (np.abs(x).max() + 1e-9))
    wavfile.write(RACINE / dossier / f"{nom}.wav", SR, (x * 32767).astype(np.int16))


def sonnerie():
    """Un portable bon marche qui sonne dans une poche : deux trilles, le haut-parleur qui sature un peu."""
    sortie = np.zeros(int(2.0 * SR))
    for debut in (0.0, 1.0):
        for k, f in enumerate((1318, 1568, 1318, 1568, 1760, 1568)):
            d = t(0.09)
            note = np.sign(np.sin(2 * np.pi * f * d)) * 0.5 + np.sin(2 * np.pi * f * 2 * d) * 0.2
            note *= enveloppe(len(d), 0.004, 0.06)
            i = int((debut + k * 0.1) * SR)
            sortie[i:i + len(d)] += note
    bourdon = np.sin(2 * np.pi * 160 * t(2.0)) * (np.sin(2 * np.pi * 9 * t(2.0)) > 0.2) * 0.25   # le vibreur dessous
    return bande(np.tanh((sortie + bourdon) * 1.6), 500, 4200)


def decroche():
    """Le pouce sur l'ecran, puis la ligne qui s'ouvre : un clic, un souffle de reseau."""
    clic = rng.normal(size=int(0.02 * SR)) * enveloppe(int(0.02 * SR), 0.001, 0.004)
    ligne = bande(rng.normal(size=int(0.35 * SR)), 300, 3200) * 0.12 * enveloppe(int(0.35 * SR), 0.03, 0.2)
    x = np.zeros(int(0.4 * SR))
    x[:len(clic)] += bande(clic, 1500, 7000)
    x[int(0.04 * SR):int(0.04 * SR) + len(ligne)] += ligne
    return x


def raccroche():
    """Le pouce qui raccroche : un clic sec et les deux notes descendantes de fin d'appel."""
    x = np.zeros(int(0.45 * SR))
    clic = bande(rng.normal(size=int(0.015 * SR)), 1500, 7000) * enveloppe(int(0.015 * SR), 0.001, 0.004)
    x[:len(clic)] += clic
    for k, f in enumerate((880, 660)):
        d = t(0.12)
        x[int((0.08 + k * 0.14) * SR):int((0.08 + k * 0.14) * SR) + len(d)] += np.sin(2 * np.pi * f * d) * 0.35 * enveloppe(len(d), 0.005, 0.05)
    return x


def sac_papier():
    """Un sac de papier kraft qu'on attrape : un froissement bref, sec, puis le poids qui retombe."""
    n = int(0.42 * SR)
    froisse = rng.normal(size=n) * (rng.random(n) > 0.75)              # des craquements, pas un souffle continu
    froisse = bande(froisse, 1800, 7500) * enveloppe(n, 0.01, 0.12)
    poids = bas(rng.normal(size=n), 220) * enveloppe(n, 0.002, 0.05) * 2.2
    x = froisse
    x[int(0.18 * SR):] += poids[:n - int(0.18 * SR)]
    return x


def remise():
    """La main qui prend : un frottement de tissu et un petit choc mat."""
    n = int(0.3 * SR)
    tissu = bande(rng.normal(size=n), 400, 2600) * enveloppe(n, 0.03, 0.07) * 0.7
    choc = bas(rng.normal(size=n), 300) * enveloppe(n, 0.001, 0.03) * 1.6
    x = tissu
    x[int(0.08 * SR):] += choc[:n - int(0.08 * SR)]
    return x


def notes(freqs, duree=0.09, ecart=0.07):
    x = np.zeros(int((ecart * len(freqs) + duree + 0.1) * SR))
    for k, f in enumerate(freqs):
        d = t(duree + 0.08)
        note = (np.sin(2 * np.pi * f * d) + 0.3 * np.sin(2 * np.pi * f * 2 * d)) * enveloppe(len(d), 0.004, 0.07)
        i = int(k * ecart * SR)
        x[i:i + len(d)] += note
    return x


if __name__ == "__main__":
    ecrire("interface", "tel_sonnerie", sonnerie(), -8)
    ecrire("interface", "tel_decroche", decroche(), -10)
    ecrire("interface", "tel_raccroche", raccroche(), -10)
    ecrire("interface", "inv_ajout", notes((784, 1175)), -14)
    ecrire("interface", "inv_retrait", notes((988, 659)), -15)
    ecrire("objets", "sac_papier", sac_papier(), -8)
    ecrire("objets", "remise", remise(), -9)
    print("7 sons ->", RACINE)
