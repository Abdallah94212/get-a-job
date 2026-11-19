"""Les sons fabriques pour la direction sonore : ceux qu'aucun enregistrement libre ne donnait juste.

La tele de l'intro (son emission, sa rafale, sa neige), ce que le heros entend quand le choc le coupe du monde
(l'acouphene, la montee grave, le coup sourd), la rue (klaxons, bus, scooters et voitures qui passent, rafales de
vent, pneus sur chaussee mouillee), le scooter qu'on conduit (ralenti, arret), les interieurs (friteuse, serveurs,
bips du labo, horloge, radio, voisins a travers le mur, bureaux), Namek (cigales), le telephone (message, argent,
loyer), la caisse enregistreuse et le carton de chapitre. Appele par preparer.py.
"""
import tempfile
from pathlib import Path

import numpy as np
from scipy import signal

SR = 22050
rng = np.random.default_rng(4242)


def temps(d):
    return np.arange(int(d * SR)) / SR


def passe_bas(x, f, ordre=2):
    return signal.sosfilt(signal.butter(ordre, f, "low", fs=SR, output="sos"), x)


def passe_haut(x, f, ordre=2):
    return signal.sosfilt(signal.butter(ordre, f, "high", fs=SR, output="sos"), x)


def passe_bande(x, bas, haut, ordre=2):
    return signal.sosfilt(signal.butter(ordre, [bas, min(haut, SR / 2 - 200)], "band", fs=SR, output="sos"), x)


def norm(x, db):
    return x * (10 ** (db / 20) / (np.abs(x).max() + 1e-9))


def rms(x, db):
    return x * (10 ** (db / 20) / (np.sqrt(np.mean(x ** 2)) + 1e-9))


def bruit(n):
    return rng.standard_normal(n)


def brun(n):
    b = np.cumsum(bruit(n))
    return passe_haut(b, 20) / (np.abs(b).max() + 1e-9)


def boucle(x, fondu=0.5):
    n = int(fondu * SR)
    a = np.linspace(0, np.pi / 2, n)
    return np.concatenate([x[:n] * np.sin(a) + x[-n:] * np.cos(a), x[n:-n]])


def doux(x, a=0.003, b=0.03):
    x = x.copy()
    na, nb = int(a * SR), int(b * SR)
    x[:na] *= np.linspace(0, 1, na)
    x[-nb:] *= np.linspace(1, 0, nb)
    return x


def ton(f, d, forme="sin"):
    t = temps(d)
    if forme == "carre":
        return signal.square(2 * np.pi * f * t)
    if forme == "scie":
        return signal.sawtooth(2 * np.pi * f * t)
    return np.sin(2 * np.pi * f * t)


def cloche(f, d, declin=3.0, partiels=((1, 1), (2.76, 0.4), (5.4, 0.18), (8.9, 0.07))):
    t = temps(d)
    return sum(a * np.sin(2 * np.pi * f * p * t) * np.exp(-declin * p ** 0.6 * t) for p, a in partiels) * np.minimum(1, t / 0.002)


def marimba(f, d=0.6):
    t = temps(d)
    return (np.sin(2 * np.pi * f * t) * np.exp(-t * 9) + 0.25 * np.sin(2 * np.pi * f * 4 * t) * np.exp(-t * 30)) * np.minimum(1, t / 0.001)


def reverb(x, duree=1.6, mix=0.3):
    """Une queue de salle : du bruit qui s'eteint, convolue au son."""
    t = temps(duree)
    ri = bruit(len(t)) * np.exp(-t * 6.9 / duree)
    ri = passe_bas(ri, 5000)
    y = signal.fftconvolve(x, ri)[: len(x) + len(t)]
    y = y / (np.abs(y).max() + 1e-9) * np.abs(x).max()
    sec = np.concatenate([x, np.zeros(len(t))])
    y = np.pad(y, (0, max(0, len(sec) - len(y))))[: len(sec)]
    return sec * (1 - mix) + y * mix


def moteur_deux_temps(regime, duree, rugueux=0.25):
    """Un petit deux-temps : une explosion par tour, regime en tours par seconde (scalaire ou tableau)."""
    t = temps(duree)
    regime = np.broadcast_to(regime, t.shape).astype(float) * (1 + 0.02 * passe_bas(bruit(len(t)), 8) * 30)
    phase = np.cumsum(regime) / SR
    impulsions = np.exp(-(phase % 1) * 9) * np.sin(2 * np.pi * (phase % 1) * 3)
    x = passe_bande(impulsions, 70, 2400) + rugueux * passe_bande(bruit(len(t)), 500, 3200) * (0.5 + 0.5 * np.exp(-(phase % 1) * 5))
    return x


# ---------------------------------------------------------------- la tele de l'intro

def tele_neige():
    x = passe_bande(bruit(int(3 * SR)), 300, 7500)
    x *= 1 + 0.15 * np.sin(2 * np.pi * 50 * temps(3))               # le ronflement du secteur dans la neige
    return rms(boucle(x, 0.4), -26)


def tele_rafale():
    """Ce qui sort de la tele au moment ou il voit : le son sature, le presentateur bute sur un mot, puis la neige."""
    chemin = Path(tempfile.gettempdir()) / "get-a-job-presentateur.npy"
    voix = np.load(chemin) if chemin.exists() else bruit(SR * 4) * 0.1
    d = 3.0
    n = int(d * SR)
    x = np.zeros(n)
    fragment = voix[int(1.2 * SR): int(1.2 * SR) + int(0.16 * SR)]
    fragment = doux(fragment, 0.002, 0.01)
    pos = int(0.05 * SR)
    for k in range(7):                                                  # le mot qui revient, de plus en plus haché
        f = fragment[: int(len(fragment) * (1 - k * 0.1))]
        x[pos: pos + len(f)] += np.tanh(f * (3 + k)) * (1 - k * 0.06)
        pos += len(f) + int(rng.uniform(0.0, 0.05) * SR)
    sature = passe_bande(x, 400, 4500)
    neige = passe_bande(bruit(n), 300, 8000) * np.interp(temps(d), [0, 0.05, 0.6, 1.8, 3.0], [0, 1.0, 0.55, 0.35, 0.25])
    x = sature * np.interp(temps(d), [0, 1.2, 1.9], [1, 0.8, 0]) + neige * 0.6
    return norm(doux(x, 0.001, 0.4), -4)


def acouphene():
    """Le sifflement qui remplace le monde : il monte, tient, puis s'efface. Discret, jamais douloureux."""
    t = temps(7.0)
    env = np.interp(t, [0, 0.8, 4.5, 7.0], [0, 1, 0.75, 0]) ** 1.5
    x = np.sin(2 * np.pi * 5900 * t) * env + 0.25 * np.sin(2 * np.pi * 4100 * t + 0.4) * env
    x *= 1 + 0.08 * np.sin(2 * np.pi * 3.1 * t)
    return norm(x, -22)


def montee():
    """Une montee grave, presque inaudible au debut : la piece qui se resserre autour de lui."""
    d = 9.0
    t = temps(d)
    env = (t / d) ** 2.2
    x = np.sin(2 * np.pi * 42 * t) + 0.6 * np.sin(2 * np.pi * 42.7 * t) + 0.35 * np.sin(2 * np.pi * 84 * t)
    souffle = np.zeros_like(t)
    b = bruit(len(t))
    for i in range(0, len(t), 2048):                                    # un bruit filtre qui s'ouvre peu a peu
        j = min(len(t), i + 2048)
        souffle[i:j] = passe_bande(b[i:j], 120, 300 + 2500 * (i / len(t)) ** 2, 1)
    x = x * env + souffle * env * 0.35
    return norm(doux(x, 0.5, 0.05), -6)


def coup_sourd():
    t = temps(1.8)
    x = np.sin(2 * np.pi * 48 * t * (1 - 0.15 * t)) * np.exp(-t * 2.6) + 0.4 * passe_bas(bruit(len(t)), 180) * np.exp(-t * 8)
    return norm(doux(x, 0.001, 0.2), -4)


def cigarette_tombe():
    t = temps(0.9)
    x = np.zeros(len(t))
    for debut, g in ((0.0, 1.0), (0.11, 0.35), (0.19, 0.15)):          # elle touche le sol, rebondit a peine
        i = int(debut * SR)
        k = int(0.012 * SR)
        x[i:i + k] += passe_bande(bruit(k), 1500, 6000) * np.exp(-np.arange(k) / 50) * g
    braise = np.zeros(len(t))
    for _ in range(12):                                                 # la braise qui crepite une seconde
        i = rng.integers(int(0.1 * SR), len(t) - 200)
        braise[i:i + 120] += bruit(120) * np.exp(-np.arange(120) / 25) * rng.uniform(0.05, 0.2)
    return norm(passe_haut(x + braise, 900), -14)


# ---------------------------------------------------------------- la rue

def klaxon(f1, f2, morceaux):
    x = []
    for d, pause in morceaux:
        t = temps(d)
        k = (signal.square(2 * np.pi * f1 * t, 0.4) + 0.8 * signal.square(2 * np.pi * f2 * t, 0.4)) * np.minimum(1, t / 0.01)
        x.append(doux(passe_bande(k, 300, 3200), 0.005, 0.03))
        x.append(np.zeros(int(pause * SR)))
    y = np.concatenate(x)
    return norm(reverb(passe_bas(y, 2500), 1.2, 0.35), -11)


def voiture_passe_sec():
    t = temps(3.0)
    env = np.exp(-((t - 1.4) / 0.6) ** 2)
    roulement = passe_bande(bruit(len(t)), 120, 1400) * env
    hauteur = 85 * (1.1 - 0.2 / (1 + np.exp(-(t - 1.4) * 4)))
    moteur = passe_bas(signal.sawtooth(2 * np.pi * np.cumsum(hauteur) / SR), 600) * env * 0.5
    return norm(roulement + moteur, -12)


def bus_passe():
    t = temps(5.0)
    env = np.exp(-((t - 2.2) / 1.1) ** 2)
    hauteur = 38 * (1.08 - 0.16 / (1 + np.exp(-(t - 2.2) * 2.5)))
    diesel = passe_bas(signal.sawtooth(2 * np.pi * np.cumsum(hauteur) / SR) + 0.5 * bruit(len(t)) * 0.3, 500) * env
    pneus = passe_bande(bruit(len(t)), 200, 2400) * env * 0.5
    pschit = np.zeros(len(t))                                           # les freins a air, a l'arret
    i = int(3.4 * SR)
    k = int(0.7 * SR)
    pschit[i:i + k] = passe_bande(bruit(k), 2500, 9000) * np.exp(-np.arange(k) / (0.25 * SR)) * 0.35
    return norm(diesel + pneus + pschit, -10)


def scooter_passe():
    d = 3.2
    t = temps(d)
    env = np.exp(-((t - 1.3) / 0.55) ** 2)
    regime = 105 * (1.12 - 0.24 / (1 + np.exp(-(t - 1.3) * 5)))
    return norm(moteur_deux_temps(regime, d) * env, -12)


def rafale():
    d = 4.0
    t = temps(d)
    env = np.interp(t, [0, 1.2, 2.0, 4.0], [0, 1, 0.8, 0]) ** 1.5
    b = bruit(len(t))
    x = np.zeros(len(t))
    for i in range(0, len(t), 1024):
        j = min(len(t), i + 1024)
        c = 350 + 500 * env[i]
        x[i:j] = passe_bande(b[i:j], c * 0.5, c * 2.2, 1)
    return norm(x * env, -12)


def pneus_mouilles():
    x = passe_bande(bruit(int(3 * SR)), 1200, 7000) + 0.5 * passe_bande(bruit(int(3 * SR)), 200, 900)
    x *= 1 + 0.2 * passe_bas(bruit(int(3 * SR)), 6) * 20
    return rms(boucle(x, 0.4), -24)


def scooter_ralenti():
    x = moteur_deux_temps(30, 2.0, 0.4)
    x *= 1 + 0.25 * np.sin(2 * np.pi * 1.7 * temps(2.0))              # le ralenti qui hesite un peu
    return rms(boucle(x, 0.25), -22)


def scooter_coupe():
    d = 1.4
    t = temps(d)
    regime = 30 * np.clip(1 - t / 0.9, 0.0, 1) ** 1.5 + 0.1
    x = moteur_deux_temps(regime, d, 0.35) * np.interp(t, [0, 0.8, 1.1], [1, 0.6, 0])
    tic = np.zeros(len(t))                                              # le pot qui refroidit
    for debut in (1.05, 1.28):
        i = int(debut * SR)
        tic[i:i + 90] += passe_haut(bruit(90), 3000) * np.exp(-np.arange(90) / 15) * 0.25
    return norm(x + tic, -9)


# ---------------------------------------------------------------- les interieurs

def friture():
    d = 6.0
    n = int(d * SR)
    x = passe_bande(bruit(n), 2500, 9000) * 0.25
    for _ in range(int(d * 140)):                                       # l'huile qui crepite
        i = rng.integers(0, n - 300)
        k = rng.integers(30, 260)
        x[i:i + k] += passe_haut(bruit(k), 1800) * np.exp(-np.arange(k) / (k / 4)) * rng.uniform(0.2, 1)
    return rms(boucle(x, 0.4), -28)


def serveurs():
    d = 6.0
    t = temps(d)
    ventilos = passe_bande(bruit(len(t)), 150, 2500) * 0.6
    secteur = sum(np.sin(2 * np.pi * 50 * h * t) / h for h in (1, 2, 3, 4)) * 0.15
    sifflement = np.sin(2 * np.pi * 7400 * t) * 0.01
    disques = np.zeros(len(t))
    for _ in range(14):                                                 # les disques qui grattent
        i = rng.integers(0, len(t) - 400)
        disques[i:i + 300] += passe_haut(bruit(300), 2000) * np.exp(-np.arange(300) / 40) * 0.25
    return rms(boucle(ventilos + secteur + sifflement + disques, 0.4), -29)


def bip(f, n, ecart=0.12, d=0.07):
    x = []
    for _ in range(n):
        x.append(doux(ton(f, d) * 0.8 + 0.2 * ton(f * 2, d), 0.002, 0.01))
        x.append(np.zeros(int(ecart * SR)))
    return norm(np.concatenate(x), -20)


def horloge():
    x = np.zeros(int(2 * SR))
    for debut, f in ((0.0, 3200), (1.0, 2600)):                         # tic... tac
        i = int(debut * SR)
        k = int(0.02 * SR)
        x[i:i + k] += passe_bande(bruit(k), f * 0.7, f * 1.3) * np.exp(-np.arange(k) / 40)
    return norm(x, -20)


def cigales():
    d = 6.0
    t = temps(d)
    x = np.zeros(len(t))
    for f, vitesse, phase in ((5200, 38, 0), (4700, 41, 1.3), (5600, 35, 2.1)):
        trille = (0.5 + 0.5 * np.sin(2 * np.pi * vitesse * t + phase)) ** 3
        vague = 0.5 + 0.5 * np.sin(2 * np.pi * 0.17 * t + phase)
        x += passe_bande(bruit(len(t)), f * 0.85, f * 1.15) * trille * vague
    return rms(boucle(x, 0.6), -32)


def sonnerie_bureau():
    x = []
    for _ in range(2):
        t = temps(0.9)
        x.append(passe_bande(ton(440, 0.9) * (0.5 + 0.5 * signal.square(2 * np.pi * 20 * t)) + ton(480, 0.9), 300, 3000))
        x.append(np.zeros(int(1.0 * SR)))
    return norm(reverb(np.concatenate(x), 1.5, 0.5), -18)


def ascenseur():
    return norm(reverb(cloche(1318, 1.5, 2.5) + 0.7 * np.pad(cloche(1046, 1.4, 2.5), (int(0.25 * SR), 0))[: int(1.5 * SR)], 1.2, 0.3), -14)


def clavier():
    x = np.zeros(int(2.4 * SR))
    pos = 0
    while pos < len(x) - 400:
        k = 200
        x[pos:pos + k] += passe_bande(bruit(k), 1500, 6000) * np.exp(-np.arange(k) / 30) * rng.uniform(0.4, 1)
        pos += int(rng.uniform(0.06, 0.2) * SR) + (int(0.5 * SR) if rng.random() < 0.06 else 0)
    return norm(x, -20)


def voisins(syllabes, homme):
    """Une dispute a travers le mur : on n'entend que le grave, et un choc sourd."""
    x = []
    for _ in range(rng.integers(9, 15)):
        s = syllabes[rng.integers(len(syllabes))]
        x.append(s * rng.uniform(0.6, 1.0))
        if rng.random() < 0.25:
            x.append(np.zeros(int(rng.uniform(0.1, 0.4) * SR)))
    y = np.concatenate(x)
    if homme:
        k = int(0.3 * SR)
        y = np.concatenate([y, np.zeros(int(0.2 * SR))])
        y[-k:] += passe_bas(bruit(k), 120) * np.exp(-np.arange(k) / 900) * 2
    return norm(passe_bas(y, 520, 4), -16)


def radio(source):
    """La musique d'une petite radio de cuisine : sans graves, sans aigus, un peu saturee, avec du souffle."""
    x = source[: int(30 * SR)]
    x = passe_bande(x, 380, 3600, 3)
    x = np.tanh(x / (np.abs(x).max() + 1e-9) * 2.2) + 0.03 * passe_bande(bruit(len(x)), 2000, 8000)
    return rms(boucle(x, 1.0), -27)


# ---------------------------------------------------------------- le telephone, l'argent, les chapitres

def tel_sms():
    x = marimba(1046.5) * 0.9
    y = np.pad(marimba(1568, 0.7), (int(0.11 * SR), 0))
    n = max(len(x), len(y))
    return norm(reverb(np.pad(x, (0, n - len(x))) + np.pad(y, (0, n - len(y))) * 0.8, 0.8, 0.2), -12)


def tel_argent():
    x = cloche(2093, 0.6, 6) + 0.6 * np.pad(cloche(2637, 0.55, 6), (int(0.06 * SR), 0))[: int(0.6 * SR)]
    return norm(x, -15)


def tel_paye():
    t = temps(0.12)
    tampon = passe_bas(bruit(len(t)), 900) * np.exp(-t * 60)
    x = np.concatenate([tampon, np.zeros(int(0.05 * SR))])
    x = np.concatenate([x, marimba(784, 0.5) * 0.6])
    return norm(x, -12)


def caisse():
    tiroir = passe_bande(bruit(int(0.25 * SR)), 300, 2500) * np.exp(-temps(0.25) * 14)
    ding = cloche(2349, 1.0, 4)
    x = np.concatenate([np.zeros(int(0.05 * SR)), ding])
    x[:len(tiroir)] += tiroir * 0.8
    return norm(x, -12)


def chapitre():
    """Le carton d'un chapitre : un accord grave qui s'ouvre, une cloche lointaine. Pas une fanfare : une porte."""
    d = 5.0
    t = temps(d)
    accord = sum(np.sin(2 * np.pi * f * t) * np.exp(-t * 0.7) for f in (65.4, 98.0, 130.8, 155.6))
    accord *= np.minimum(1, t / 0.4)
    c = np.pad(cloche(523.3, 3.5, 1.6), (int(0.6 * SR), len(t)))[: len(t)] * 0.5
    return norm(reverb(accord * 0.6 + c, 2.5, 0.45), -8)


def fabriquer(syllabes_homme, syllabes_femme, source_radio):
    sons = {
        "tele_neige": (tele_neige(), True), "tele_rafale": (tele_rafale(), False), "acouphene": (acouphene(), False),
        "montee": (montee(), True), "coup_sourd": (coup_sourd(), False), "cigarette_tombe": (cigarette_tombe(), False),
        "klaxon_1": (klaxon(420, 520, [(0.35, 0.0)]), False),
        "klaxon_2": (klaxon(390, 495, [(0.12, 0.08), (0.18, 0.0)]), False),
        "klaxon_3": (klaxon(470, 590, [(0.9, 0.0)]), False),
        "voiture_passe_sec": (voiture_passe_sec(), False), "bus_passe": (bus_passe(), False),
        "scooter_passe": (scooter_passe(), False), "rafale_1": (rafale(), False), "rafale_2": (rafale(), False),
        "amb_pneus_mouilles": (pneus_mouilles(), False), "scooter_ralenti": (scooter_ralenti(), False),
        "scooter_coupe": (scooter_coupe(), False),
        "amb_friture": (friture(), True), "amb_serveurs": (serveurs(), True), "amb_horloge": (horloge(), False),
        "amb_cigales": (cigales(), True), "amb_radio": (radio(source_radio), True),
        "bip_1": (bip(1760, 2), False), "bip_2": (bip(2093, 1, d=0.12), False), "bip_3": (bip(1318, 3, 0.08, 0.05), False),
        "sonnerie_bureau": (sonnerie_bureau(), False), "ascenseur": (ascenseur(), False), "clavier": (clavier(), False),
        "voisins_1": (voisins(syllabes_homme, True), False), "voisins_2": (voisins(syllabes_femme, False), False),
        "tel_sms": (tel_sms(), False), "tel_argent": (tel_argent(), False), "tel_paye": (tel_paye(), False),
        "caisse": (caisse(), False), "chapitre": (chapitre(), False),
    }
    return sons
