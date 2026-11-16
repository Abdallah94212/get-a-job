"""Les voix des personnages : des syllabes sans langue, a la maniere des jeux qui font "parler" leurs personnages
sans doublage. Chaque personnage a sa banque : une dizaine de syllabes et quelques reactions (rire, soupir,
hesitation, surprise, grognement, inspiration), toutes calculees ici par synthese source-filtre :

- la source est une corde vocale : une somme d'harmoniques dont la pente donne le timbre (sombre, clair), avec du
  souffle, de la friture vocale (la voix rauque de Geto, du SDF), du tremblement (les vieux) ;
- le filtre est le conduit vocal : trois formants par voyelle, mis a l'echelle de la taille du conduit (un enfant,
  une femme, un homme), avec une nasalite reglable (Gero, le type du metro) ;
- les consonnes sont de petits bruits places devant la voyelle : une explosion (b, d, k, t), un murmure (m, n),
  un glissement (w, l), un souffle (h), un sifflement (s).

Le jeu choisit une syllabe au hasard a chaque groupe de lettres et la transpose selon l'intonation de la phrase et
l'emotion du moment (jeu/src/jeu/Parole.java). Lancement : python sons/voix.py (depuis designs/).
"""
from pathlib import Path

import numpy as np
from scipy import signal

SR = 22050
RACINE = Path(__file__).resolve().parents[2]
SORTIE = RACINE / "jeu" / "ressources" / "sons" / "voix"

# Les formants d'un conduit d'homme adulte, en Hz : F1, F2, F3.
VOYELLES = {"a": (730, 1100, 2450), "e": (470, 1900, 2550), "i": (290, 2250, 2950), "o": (500, 860, 2400),
            "u": (330, 820, 2250), "eu": (440, 1450, 2350), "ai": (620, 1750, 2500), "on": (420, 800, 2350)}
LARGEURS = (120, 170, 230)


class Voix:
    """Le reglage d'une voix. f0 : hauteur moyenne. k : taille du conduit (1 homme, 1.17 femme, 1.25 ado clair).
    pente : 1 neutre, plus = plus sombre. souffle, friture, nasal, tremble : entre 0 et 1. duree : la voyelle, en s."""

    def __init__(self, f0, k=1.0, pente=1.0, souffle=0.1, friture=0.0, nasal=0.0, tremble=0.0, duree=0.09,
                 consonnes="mbdw", voyelles=("a", "o", "e"), telephone=False, graine=1):
        self.f0, self.k, self.pente, self.souffle = f0, k, pente, souffle
        self.friture, self.nasal, self.tremble, self.duree = friture, nasal, tremble, duree
        self.consonnes, self.voyelles, self.telephone = consonnes, voyelles, telephone
        self.rng = np.random.default_rng(graine)


def resonance(f, centre, largeur):
    """La reponse d'un formant (un resonateur du second ordre) a la frequence f."""
    return 1.0 / np.sqrt((1 - (f / centre) ** 2) ** 2 + (f * largeur / centre ** 2) ** 2)


def bruit_lisse(rng, n, coupure):
    b = rng.standard_normal(n + 64)
    b = signal.lfilter(*signal.butter(1, coupure, fs=SR), b)[64:]
    return b / (np.abs(b).max() + 1e-9)


def voise(v, f0, formants, amplitude, nasal=None, sourdine=1.0):
    """Le son voise : f0 (Hz par echantillon), formants (3 tableaux, Hz par echantillon), amplitude par echantillon.
    Les harmoniques sont calculees par trames de 64 echantillons et interpolees : la voix glisse sans clic."""
    n = len(f0)
    if v.tremble:
        f0 = f0 * (1 + v.tremble * 0.045 * np.sin(2 * np.pi * 5.3 * np.arange(n) / SR))
        amplitude = amplitude * (1 + v.tremble * 0.2 * np.sin(2 * np.pi * 5.3 * np.arange(n) / SR + 1))
    f0 = f0 * (1 + 0.012 * bruit_lisse(v.rng, n, 30) + v.friture * 0.03 * bruit_lisse(v.rng, n, 90))
    phase = 2 * np.pi * np.cumsum(f0) / SR
    trame = 64
    centres = np.arange(0, n, trame)
    hmax = int(min(9500, 4800 * v.k * 1.4) / f0.min()) + 1
    sortie = np.zeros(n)
    nasal = v.nasal if nasal is None else nasal
    for h in range(1, hmax + 1):
        fh = h * f0[centres]
        # la pente de la source : presque plate sous 500 Hz, puis elle descend ; plus forte pour une voix sombre
        gain = (1 + (fh / 500) ** 2) ** (-0.42 * v.pente) * sourdine ** (h - 1)
        # les formants en parallele, chacun ramene a son pic : F2 et F3 restent presents, la voix reste claire
        enveloppe = np.full_like(fh, 0.004)
        for i, g in enumerate((1.0, 0.8, 0.55)):
            c, b = formants[i][centres] * v.k, LARGEURS[i] * (1 + 0.4 * (v.k - 1))
            enveloppe += g * resonance(fh, c, b) * b / c
        if nasal:
            enveloppe *= (1 + nasal * 2.5 * resonance(fh, 260 * v.k, 120) / 4) * (1 - nasal * 0.75 * np.exp(-((fh - 1050 * v.k) / 260) ** 2))
        a = np.interp(np.arange(n), centres, gain * enveloppe * (fh < 10500))
        sortie += a * np.sin(h * phase)
    if v.friture:                                                       # la voix qui accroche : une periode sur deux plus faible
        sortie *= 1 - v.friture * 0.5 * (0.5 + 0.5 * np.sin(phase / 2 + v.rng.uniform(0, 6)))
        sortie *= 1 + v.friture * 0.35 * bruit_lisse(v.rng, n, 60)
    return sortie / (np.abs(sortie).max() + 1e-9) * amplitude


def souffle_formants(v, n, formants, amplitude):
    """Le souffle qui passe par la bouche ouverte : du bruit colore par les formants de la voyelle."""
    b = v.rng.standard_normal(n)
    sortie = np.zeros(n)
    for i, g in enumerate((1.0, 0.7, 0.4)):
        f = np.median(formants[i]) * v.k
        lo, hi = max(80, f - LARGEURS[i] * 2), min(SR / 2 - 200, f + LARGEURS[i] * 2)
        sortie += g * signal.lfilter(*signal.butter(2, [lo, hi], "band", fs=SR), b)
    return sortie / (np.abs(sortie).max() + 1e-9) * amplitude


def bande(v, n, bas, haut, amplitude=1.0):
    b = v.rng.standard_normal(n)
    b = signal.lfilter(*signal.butter(2, [bas, min(haut, SR / 2 - 200)], "band", fs=SR), b)
    return b / (np.abs(b).max() + 1e-9) * amplitude


LOCUS = {"b": 800, "p": 800, "m": 900, "w": 750, "d": 1750, "t": 1750, "n": 1600, "l": 1150, "g": 2100, "k": 2100,
         "s": 1700, "h": None}
EXPLOSION = {"b": (300, 1500), "p": (300, 1500), "d": (2400, 4500), "t": (2600, 5200), "g": (1300, 2600), "k": (1400, 3000)}


def syllabe(v, consonne, voyelle, duree=None, hauteur=(1.0, 0.95), force=1.0, souffle=None):
    """Une syllabe : la consonne, puis la voyelle qui s'ouvre et se referme. hauteur : f0 relative au debut, a la fin."""
    duree = v.duree if duree is None else duree
    cible = np.array(VOYELLES[voyelle], dtype=float)
    pre = {"b": 0.012, "d": 0.012, "g": 0.012, "p": 0.018, "t": 0.018, "k": 0.02, "m": 0.045, "n": 0.04,
           "w": 0.0, "l": 0.0, "h": 0.045, "s": 0.065, "": 0.0}[consonne[:1] if consonne else ""]
    glisse = 0.05 if "w" in consonne else 0.035 if consonne else 0.0
    n_pre, n_voy = int(pre * SR), int((duree + glisse) * SR)
    n_fin = int(0.035 * SR)
    n = n_pre + n_voy + n_fin
    t = np.arange(n) / SR
    # la hauteur : une petite montee a l'attaque, puis la pente voulue
    debut, fin = hauteur
    f0 = v.f0 * np.interp(t, [0, n_pre / SR + 0.02, n / SR], [debut * 0.97, debut, fin])
    # les formants : du lieu de la consonne vers la voyelle
    depart = cible.copy()
    if "w" in consonne:
        depart = np.array(VOYELLES["u"], dtype=float)
    elif consonne and LOCUS.get(consonne[0]):
        depart = np.array([max(250, cible[0] * 0.55), LOCUS[consonne[0]], cible[2]])
    formants = [np.interp(t, [0, n_pre / SR, n_pre / SR + glisse + 1e-4, n / SR], [depart[i], depart[i], cible[i], cible[i]])
                for i in range(3)]
    # l'enveloppe de la voyelle
    env = np.interp(t, [0, n_pre / SR, n_pre / SR + 0.012, (n_pre + n_voy) / SR, n / SR], [0, 0, 1, 0.85, 0]) ** 1.2
    x = voise(v, f0, formants, env)
    souffle = v.souffle if souffle is None else souffle
    if souffle:
        x += souffle_formants(v, n, formants, env * souffle * 0.9)
    c = consonne[:1] if consonne else ""
    if c in ("m", "n"):                                                 # le murmure nasal, bouche fermee
        murmure = voise(v, f0[:n_pre + 200], [np.full(n_pre + 200, 260.0), np.full(n_pre + 200, 1000.0 if c == "m" else 1500.0),
                                              np.full(n_pre + 200, 2300.0)], np.interp(np.arange(n_pre + 200), [0, 80, n_pre, n_pre + 200], [0, 0.4, 0.45, 0]),
                        nasal=0.9, sourdine=0.75)
        x[:n_pre + 200] += murmure
    elif c in EXPLOSION:                                                # l'explosion, et le souffle des sourdes
        bas, haut = EXPLOSION[c]
        i = n_pre - int(0.006 * SR)
        k = int(0.008 * SR)
        x[i:i + k] += bande(v, k, bas, haut, 0.55 if c in "ptk" else 0.35) * np.exp(-np.arange(k) / (k / 3))
        if c in "bdg":                                                  # la barre voisee, avant l'explosion
            x[:i] += voise(v, f0[:i], [np.full(i, 250.0), np.full(i, 900.0), np.full(i, 2300.0)], np.full(i, 0.12), sourdine=0.5)
        else:
            a = int(0.025 * SR)
            x[n_pre:n_pre + a] += souffle_formants(v, a, [f[n_pre:n_pre + a] for f in formants], np.linspace(0.3, 0, a))
    elif c == "h":
        x[:n_pre + 300] += souffle_formants(v, n_pre + 300, [f[:n_pre + 300] for f in formants],
                                            np.interp(np.arange(n_pre + 300), [0, 200, n_pre, n_pre + 300], [0, 0.35, 0.3, 0]))
    elif c == "s":
        x[:n_pre] += bande(v, n_pre, 4200, 9500, 0.22) * np.interp(np.arange(n_pre), [0, 300, n_pre - 200, n_pre], [0, 1, 1, 0])
    return x * force


def silence(d):
    return np.zeros(int(d * SR))


def finir(v, x, niveau=-17.0):
    """Le passage au telephone si besoin, puis un niveau commun : toutes les voix sonnent aussi fort."""
    if v.telephone:
        x = signal.sosfilt(signal.butter(4, [340, 3300], "band", fs=SR, output="sos"), x)
        x = np.tanh(x / (np.abs(x).max() + 1e-9) * 1.8)
    x = np.append(x[0], x[1:] - 0.75 * x[:-1])                      # la bouche qui rayonne : un peu plus d'aigu
    x = x - np.mean(x)
    actif = np.abs(x) > np.abs(x).max() * 0.05
    rms = np.sqrt(np.mean(x[actif] ** 2)) + 1e-9
    x = x * (10 ** (niveau / 20) / rms)
    crete = np.abs(x).max()
    if crete > 0.7:
        x *= 0.7 / crete
    a, b = int(0.002 * SR), int(0.012 * SR)
    x[:a] *= np.linspace(0, 1, a)
    x[-b:] *= np.linspace(1, 0, b)
    return x.astype(np.float32)


# ---------------------------------------------------------------- les reactions

def rire(v):
    x = []
    for k, h in enumerate((1.18, 1.1, 1.02, 0.95)[: 3 + int(v.duree < 0.1)]):
        x.append(syllabe(v, "h", "a", duree=0.075, hauteur=(h, h * 0.92), souffle=v.souffle + 0.3, force=1 - k * 0.12))
        x.append(silence(0.045))
    return np.concatenate(x)


def soupir(v):
    n = int(0.75 * SR)
    t = np.arange(n) / SR
    env = np.interp(t, [0, 0.12, 0.45, 0.75], [0, 1, 0.5, 0])
    formants = [np.full(n, f) for f in VOYELLES["eu"]]
    x = souffle_formants(v, n, formants, env * 0.8)
    f0 = v.f0 * np.interp(t, [0, 0.75], [1.0, 0.78])
    x += voise(v, f0, formants, np.interp(t, [0, 0.1, 0.3, 0.6, 0.75], [0, 0.0, 0.35, 0.15, 0]) * (1 - v.souffle * 0.5))
    return x


def hm(v):
    n = int(0.42 * SR)
    t = np.arange(n) / SR
    f0 = v.f0 * np.interp(t, [0, 0.15, 0.42], [0.98, 1.06, 0.97])
    murm = [np.full(n, 260.0), np.full(n, 1000.0), np.full(n, 2300.0)]
    return voise(v, f0, murm, np.interp(t, [0, 0.04, 0.32, 0.42], [0, 1, 0.8, 0]), nasal=0.95, sourdine=0.72)


def hein(v):
    return syllabe(v, "", "ai", duree=0.2, hauteur=(0.95, 1.32), souffle=v.souffle * 0.6) * 1.0


def oh(v):
    return syllabe(v, "h", "o", duree=0.17, hauteur=(1.25, 1.05), force=1.0)


def grogne(v):
    rauque = Voix(v.f0 * 0.78, v.k, v.pente + 0.4, v.souffle + 0.15, min(1.0, v.friture + 0.55), v.nasal, v.tremble,
                  v.duree, telephone=v.telephone, graine=99)
    return syllabe(rauque, "", "eu", duree=0.32, hauteur=(1.0, 0.9))


def inspire(v):
    n = int(0.32 * SR)
    t = np.arange(n) / SR
    env = np.interp(t, [0, 0.05, 0.22, 0.32], [0, 0.6, 1, 0])
    b = bande(v, n, 900 * v.k, 5200 * v.k, 1.0)
    centre = np.interp(t, [0, 0.32], [1400, 2400]) * v.k
    x = np.zeros(n)
    for i in range(0, n, 512):                                          # la gorge qui se serre : le bruit monte
        j = min(n, i + 512)
        x[i:j] = signal.lfilter(*signal.butter(2, [centre[i] * 0.6, min(SR / 2 - 300, centre[i] * 1.6)], "band", fs=SR), b[i:j])
    return x * env


REACTIONS = {"rire": rire, "soupir": soupir, "hm": hm, "hein": hein, "oh": oh, "grogne": grogne, "inspire": inspire}

# ---------------------------------------------------------------- les personnages
# Chaque banque est une voix ; plusieurs personnages secondaires partagent une banque, a une hauteur differente.
BANQUES = {
    # le heros : grave sans etre lourd, un peu voile, il pese ses mots
    "heros": Voix(118, 1.0, 1.15, 0.2, 0.06, 0, 0, 0.095, "mbdnw", ("a", "o", "eu", "e"), graine=11),
    "heros_jeune": Voix(140, 1.05, 1.0, 0.12, 0, 0, 0, 0.085, "mbdlw", ("a", "e", "o", "i"), graine=12),
    # Old Geto : la voix basse, rauque, posee ; chaque syllabe prend son temps
    "geto": Voix(86, 0.93, 1.3, 0.12, 0.38, 0, 0, 0.13, "mbwh", ("o", "u", "a", "eu"), graine=13),
    "geto_tel": Voix(86, 0.93, 1.3, 0.12, 0.38, 0, 0, 0.13, "mbwh", ("o", "u", "a", "eu"), telephone=True, graine=14),
    # Youngssef : clair, rapide, des syllabes courtes qui sautent
    "youngssef": Voix(178, 1.12, 0.8, 0.07, 0, 0, 0, 0.062, "tkdbw", ("e", "i", "a", "ai"), graine=15),
    # Tata Lili : chaude, chantante
    "diallo": Voix(205, 1.16, 1.0, 0.12, 0, 0.1, 0.15, 0.1, "mnlbw", ("a", "o", "e", "on"), graine=16),
    # Omar : sec, coupe
    "omar": Voix(112, 1.0, 1.0, 0.08, 0.2, 0, 0, 0.075, "dgkb", ("a", "o", "ai"), graine=17),
    "mecano": Voix(102, 0.98, 1.35, 0.28, 0.08, 0.2, 0, 0.085, "mnb", ("eu", "o", "on"), graine=18),
    # Bidoche : grave et rond, il rit facilement
    "mounir": Voix(96, 0.96, 1.15, 0.1, 0.12, 0, 0, 0.1, "bmdw", ("o", "a", "u"), graine=19),
    # la femme a l'echarpe rouge : douce, beaucoup d'air
    "femme": Voix(212, 1.18, 1.2, 0.32, 0, 0, 0, 0.105, "mnlw", ("a", "e", "eu", "o"), graine=20),
    # le type du metro : bas, nasal, trop lent
    "frotteur": Voix(100, 1.0, 1.2, 0.22, 0.1, 0.65, 0, 0.13, "mnls", ("eu", "o", "e", "on"), graine=21),
    "vieux": Voix(104, 0.97, 1.25, 0.32, 0.15, 0.1, 0.8, 0.12, "mbh", ("o", "eu", "a"), graine=22),
    "ecouteurs": Voix(228, 1.2, 0.85, 0.12, 0, 0, 0, 0.078, "tdbn", ("e", "i", "a"), graine=23),
    # Gero : precis, nasal, clinique, toujours le meme debit
    "gero": Voix(126, 1.03, 0.9, 0.05, 0, 0.45, 0, 0.085, "tkpd", ("i", "e", "eu"), graine=24),
    # Soul : calme, lie, sans aspérité
    "soul": Voix(112, 1.0, 1.1, 0.1, 0, 0, 0, 0.11, "mlwn", ("o", "a", "u", "on"), graine=25),
    "soul_jeune": Voix(134, 1.04, 1.0, 0.1, 0, 0, 0, 0.1, "mlwn", ("o", "a", "e"), graine=26),
    # le SDF : tres bas, casse, plus de souffle que de voix
    "sdf": Voix(84, 0.95, 1.4, 0.48, 0.45, 0, 0.3, 0.12, "hmb", ("eu", "o", "a"), graine=27),
    "rayan": Voix(162, 1.1, 0.85, 0.08, 0, 0.1, 0, 0.068, "tkdb", ("e", "a", "i", "ai"), graine=28),
    "moussa": Voix(148, 1.08, 1.05, 0.14, 0, 0, 0, 0.088, "bmdw", ("a", "o", "e"), graine=29),
    "haddad": Voix(100, 0.98, 1.2, 0.22, 0.15, 0, 0.35, 0.12, "bmlw", ("a", "o", "u"), graine=30),
    "douanier": Voix(108, 1.0, 0.95, 0.06, 0.05, 0, 0, 0.08, "tkpd", ("a", "o", "ai"), graine=31),
    "gars_geto": Voix(94, 0.97, 1.2, 0.12, 0.28, 0, 0, 0.09, "gkbd", ("o", "a", "eu"), graine=32),
    "regie": Voix(198, 1.15, 0.95, 0.08, 0, 0, 0, 0.088, "mnbd", ("e", "a", "o"), telephone=True, graine=33),
    # les passants, les clients, la police
    "homme": Voix(116, 1.0, 1.05, 0.12, 0.05, 0, 0, 0.09, "mbdgw", ("a", "o", "e", "eu"), graine=34),
    "femme_passante": Voix(205, 1.16, 1.0, 0.16, 0, 0, 0, 0.088, "mnbl", ("a", "e", "o"), graine=35),
    "policier": Voix(110, 1.0, 1.0, 0.06, 0.08, 0, 0, 0.08, "dktb", ("a", "o", "ai"), graine=36),
    # le presentateur de la tele, qu'on n'entend que par le petit haut-parleur
    "presentateur": Voix(128, 1.02, 0.95, 0.06, 0, 0, 0, 0.07, "dtbmnl", ("a", "e", "o", "i"), graine=37),
}


def banque(nom, v, nombre=10):
    dossier = SORTIE / nom
    dossier.mkdir(parents=True, exist_ok=True)
    combinaisons = [(c, o) for c in v.consonnes for o in v.voyelles]
    v.rng.shuffle(combinaisons)
    sons = {}
    for i in range(nombre):
        c, o = combinaisons[i % len(combinaisons)]
        if c in "bdgm" and v.rng.random() < 0.35 and "w" in v.consonnes:
            c += "w"                                                    # bwa, mwo : la syllabe qui roule
        h = v.rng.uniform(0.96, 1.05)
        sons[f"v_{nom}_{i + 1}"] = syllabe(v, c, o, duree=v.duree * v.rng.uniform(0.85, 1.15), hauteur=(h, h * v.rng.uniform(0.9, 0.98)))
    for r, f in REACTIONS.items():
        sons[f"v_{nom}_{r}"] = f(v)
    for n, x in sons.items():
        ecrire(dossier / (n + ".wav"), finir(v, x, -17 if n[-1].isdigit() else -19))
    return len(sons)


def ecrire(chemin, x):
    """En AU mu-law, 8 bits par echantillon : deux fois plus leger qu'un WAV, et Java le lit sans bibliotheque."""
    import subprocess
    x = np.clip(x, -1, 1).astype(np.float32)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", "-", "-c:a", "pcm_mulaw",
                    "-f", "au", str(Path(chemin).with_suffix(".au"))], input=x.tobytes(), check=True)


def emission(duree=24.0):
    """Ce que la tele raconte, de loin : un presentateur qui enchaine, un fond musical tres bas, un petit haut-parleur."""
    v = BANQUES["presentateur"]
    v.rng = np.random.default_rng(370)
    morceaux = []
    total = 0
    while total < duree * SR:
        phrase = v.rng.integers(6, 15)
        for s in range(phrase):
            h = 1.08 - 0.16 * s / phrase + v.rng.uniform(-0.04, 0.04)   # la phrase descend vers la fin
            c = v.consonnes[v.rng.integers(len(v.consonnes))]
            o = v.voyelles[v.rng.integers(len(v.voyelles))]
            morceaux.append(syllabe(v, c, o, duree=v.duree * v.rng.uniform(0.7, 1.3), hauteur=(h, h * 0.97)) * v.rng.uniform(0.7, 1))
            if v.rng.random() < 0.3:
                morceaux.append(silence(v.rng.uniform(0.03, 0.08)))      # entre deux mots
        morceaux.append(silence(v.rng.uniform(0.25, 0.6)))               # la respiration entre deux phrases
        total = sum(len(m) for m in morceaux)
    voix = np.concatenate(morceaux)[: int(duree * SR)]
    voix /= np.abs(voix).max() + 1e-9
    t = np.arange(len(voix)) / SR
    fond = sum(np.sin(2 * np.pi * f * t) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.07 * t + k)) for k, f in enumerate((110, 164.8, 220, 277.2)))
    x = voix * 0.8 + fond / 4 * 0.08
    x = signal.sosfilt(signal.butter(3, [260, 5200], "band", fs=SR, output="sos"), x)
    x += signal.sosfilt(signal.butter(2, [2200, 3000], "band", fs=SR, output="sos"), x) * 0.6   # le haut-parleur qui sonne
    n = int(1.0 * SR)                                                   # la boucle : la fin se fond dans le debut
    a = np.linspace(0, np.pi / 2, n)
    x = np.concatenate([x[:n] * np.sin(a) + x[-n:] * np.cos(a), x[n:-n]])
    return x / np.abs(x).max() * 0.5, voix


def main():
    total = 0
    for nom, v in BANQUES.items():
        if nom == "presentateur":
            continue
        total += banque(nom, v)
    x, voix = emission()
    ecrire(SORTIE / "tele_emission.wav", x.astype(np.float32))
    import tempfile
    np.save(Path(tempfile.gettempdir()) / "get-a-job-presentateur.npy", voix.astype(np.float32))   # pour la rafale (preparer.py)
    print(f"{len(BANQUES) - 1} voix, {total} sons, plus l'emission de tele")


if __name__ == "__main__":
    main()
