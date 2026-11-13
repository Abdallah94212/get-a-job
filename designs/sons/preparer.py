"""Prepare tous les sons du jeu dans jeu/ressources/sons.

Deux origines :
- des enregistrements sous licence libre (CC0 surtout, quelques CC-BY-SA), telecharges depuis OpenGameArt puis
  decoupes, mis en boucle sans raccord audible, filtres et ramenes a un niveau commun ;
- des sons fabriques ici, quand aucun enregistrement libre ne convenait : le metro, le scooter, les sons d'interface,
  le vent de Namek. Ils n'ont pas d'auteur exterieur.
Tout sort en mono, 22 050 Hz : en WAV 16 bits pour les sons courts, en AU mu-law (deux fois plus leger) pour les
musiques et les ambiances longues. Java sait lire les deux sans bibliotheque.

Lancement : python sons/preparer.py   (depuis designs/, avec ffmpeg installe ; il faut numpy et scipy)
Les fichiers d'origine sont mis en cache dans le dossier temporaire du systeme : relancer ne retelecharge rien.
"""
import subprocess
import sys
import tempfile
import urllib.request
import zipfile
from pathlib import Path

import numpy as np
from scipy import signal

RACINE = Path(__file__).resolve().parents[2]
SORTIE = RACINE / "jeu" / "ressources" / "sons"
CACHE = Path(tempfile.gettempdir()) / "get-a-job-sons"
SR = 22050
OGA = "https://opengameart.org/sites/default/files/"

# ---------------------------------------------------------------- les enregistrements
# nom dans le jeu : (fichier ou archive#fichier, page, auteur, licence, traitement)
# traitement : genre (boucle, ponctuel, musique), debut et duree en secondes, filtres, niveau vise (dB RMS ou crete)
ENREGISTREMENTS = {
    # la rue
    "amb_pluie": ("amb_rain_loop_1.wav", "amb-rain-loop-1", "Kresiek the Furry", "CC0",
                  dict(genre="boucle", haut=120, niveau=-24)),
    "amb_gouttiere": ("rain-gutter-loop_0.mp3", "rain-gutter-loop", "ogrebane", "CC0",
                      dict(genre="boucle", duree=40, haut=150, niveau=-26)),
    "amb_rue_mouillee": ("city_ambience_-_traffic_-_street_-_cars_drive_by_on_a_rainy_street.mp3", "free-general-ambience-sounds",
                         "Gregor Quendel", "CC-BY-SA 4.0", dict(genre="boucle", bas=6000, niveau=-24)),
    "amb_rue": ("gatve%20Varniu_2.ogg", "high-traffic-road-sounds", "ignasd", "CC0",
                dict(genre="boucle", bas=2500, niveau=-26)),
    "amb_foule": ("crowd_-_mall_ambience.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                  dict(genre="boucle", duree=40, bas=1400, niveau=-28)),
    "amb_pluie_toit": ("rain_-_falling_on_a_metal_roof.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                       dict(genre="boucle", haut=200, niveau=-25)),
    "tonnerre": ("rain_-_distant_thunder.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                 dict(genre="ponctuel", debut=0, duree=14, niveau=-6)),
    # dedans
    "amb_pluie_vitre": ("rain_on_window_loop.wav", "rain-on-window-loop", "alxl", "CC0", dict(genre="boucle", bas=3500, niveau=-27)),
    "amb_frigo": ("sfx_loops.zip#ambient_01.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0", dict(genre="boucle", niveau=-27)),
    "amb_courant": ("sfx_100_v2.zip#sfx100v2_loop_ambient_03.ogg", "100-cc0-sfx-2", "rubberduck", "CC0",
                    dict(genre="boucle", niveau=-30)),
    "amb_machine": ("sfx_100_v2.zip#sfx100v2_loop_machine_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0",
                    dict(genre="boucle", niveau=-27)),
    "amb_bouillon": ("sfx_loops.zip#water_boiling.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0", dict(genre="boucle", niveau=-29)),
    "amb_ventilation": ("sfx_loops.zip#noise_01.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0",
                        dict(genre="boucle", bas=1800, niveau=-30)),
    "bulles": ("bubbles-single1.wav", "bubble-sound-effects", "bmaczero", "CC0", dict(genre="ponctuel", niveau=-10)),
    # les pas
    **{f"pas_mouille_{k}": (f"sfx_100_v2.zip#sfx100v2_footstep_wet_0{k}.ogg", "100-cc0-sfx-2", "rubberduck", "CC0",
                            dict(genre="ponctuel", niveau=-9)) for k in (1, 2, 3)},
    **{f"pas_bois_{k}": (f"sfx_100_v2.zip#sfx100v2_footstep_wood_0{k}.ogg", "100-cc0-sfx-2", "rubberduck", "CC0",
                         dict(genre="ponctuel", niveau=-10)) for k in (1, 2, 3, 4)},
    "pas_dur_1": ("sfx_100_v2.zip#sfx100v2_footstep_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-10)),
    "pas_dur_2": ("sfx_100_v2.zip#sfx100v2_footstep_02.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-10)),
    "pas_dur_3": ("03-footstep.ogg", "footsteps-0", "gboxmikefozzy", "CC0", dict(genre="ponctuel", niveau=-10)),
    "pas_dur_4": ("04-footstep.ogg", "footsteps-0", "gboxmikefozzy", "CC0", dict(genre="ponctuel", niveau=-10)),
    # les portes et les objets
    "porte": ("100-CC0-SFX_0.zip#door_close_03.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-6)),
    "porte_lourde": ("100-CC0-SFX_0.zip#door_close_04.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-6)),
    "porte_ouvre": ("100-CC0-SFX_0.zip#door_open.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-10)),
    "clochette": ("100-CC0-SFX_0.zip#bell_01.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-10)),
    "rideau_fer": ("sfx_loops.zip#rolling.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-8)),
    "metal": ("100-CC0-SFX_0.zip#metal_03.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-12)),
    "outils": ("100-CC0-SFX_0.zip#tools_02.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-12)),
    "objet": ("sfx_100_v2.zip#sfx100v2_items_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-9)),
    "cles": ("100-CC0-SFX_0.zip#key_open_01.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-9)),
    "papier": ("100-CC0-SFX_0.zip#paper_02.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-8)),
    "serrure": ("sfx_100_v2.zip#sfx100v2_lock_open_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-9)),
    "ui_deplace": ("sfx_100_v2.zip#sfx100v2_switch_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-14)),
    # les musiques
    "mus_menu": ("lights_at_night.flac", "lights-at-night", "Tsorthan Grove", "CC0", dict(genre="musique", duree=150, niveau=-21)),
    "mus_rue": ("chilllofir-loop.ogg", "chill-lofi-inspired-loop-edit", "omfgdude (boucle : qubodup)", "CC0",
                dict(genre="musique", boucle=False, niveau=-23)),
    "mus_recit": ("Mandatory%20Overtime.mp3", "mandatory-overtime", "Joth", "CC0", dict(genre="musique", niveau=-22)),
    "mus_souvenir": ("melancholic_void.flac", "melancholic-void", "Gena Sealov", "CC0", dict(genre="musique", duree=120, niveau=-22)),
    "mus_tension": ("deepasitgets.mp3", "deep-as-it-gets", "mutantleg", "CC0", dict(genre="musique", niveau=-22)),
    "mus_filature": ("infiltration_3.mp3", "infiltration", "Adiutorium", "CC0", dict(genre="musique", duree=120, niveau=-23)),
    "mus_labo": ("Factory.ogg", "factory-ambiance", "yd", "CC0", dict(genre="musique", niveau=-24)),
    "mus_nuit": ("EmptyCity.ogg", "emptycity-background-music", "yd", "CC0", dict(genre="musique", niveau=-23)),
}


def telecharger(fichier):
    """Le fichier d'origine, depuis le cache ou depuis OpenGameArt. archive#nom extrait un fichier d'une archive."""
    archive, _, dedans = fichier.partition("#")
    CACHE.mkdir(parents=True, exist_ok=True)
    local = CACHE / archive.replace("%20", "_").replace("%40", "@")
    if not local.exists():
        demande = urllib.request.Request(OGA + archive, headers={"User-Agent": "Mozilla/5.0"})
        local.write_bytes(urllib.request.urlopen(demande, timeout=120).read())
    if not dedans:
        return local
    extrait = CACHE / ("z_" + dedans)
    if not extrait.exists():
        with zipfile.ZipFile(local) as z:
            nom = next(n for n in z.namelist() if n.endswith("/" + dedans) or n == dedans)
            extrait.write_bytes(z.read(nom))
    return extrait


def lire(chemin):
    brut = subprocess.run(["ffmpeg", "-v", "error", "-i", str(chemin), "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                          capture_output=True, check=True).stdout
    return np.frombuffer(brut, dtype=np.float32).copy()


def filtre(x, bas=None, haut=None):
    if bas:
        x = signal.sosfiltfilt(signal.butter(4, bas, "low", fs=SR, output="sos"), x)
    if haut:
        x = signal.sosfiltfilt(signal.butter(2, haut, "high", fs=SR, output="sos"), x)
    return x.astype(np.float32)


def niveau_rms(x, db):
    rms = np.sqrt(np.mean(x ** 2)) + 1e-9
    return x * (10 ** (db / 20) / rms)


def niveau_crete(x, db):
    return x * (10 ** (db / 20) / (np.abs(x).max() + 1e-9))


def boucler(x, fondu=1.0):
    """Une boucle sans raccord : la fin se fond dans le debut, a puissance constante, puis on la coupe."""
    n = int(min(fondu, len(x) / 4 / SR) * SR)
    t = np.linspace(0, np.pi / 2, n)
    debut = x[:n] * np.sin(t) + x[-n:] * np.cos(t)
    return np.concatenate([debut, x[n:-n]]).astype(np.float32)


def nettoyer(x):
    """Un son court : on retire le silence autour, avec des fondus tres brefs pour ne pas claquer."""
    seuil = np.abs(x).max() * 0.02
    actif = np.where(np.abs(x) > seuil)[0]
    if len(actif):
        x = x[max(0, actif[0] - 40): actif[-1] + int(0.06 * SR)]
    x = x.copy()
    a, b = int(0.002 * SR), int(0.02 * SR)
    x[:a] *= np.linspace(0, 1, a)
    x[-b:] *= np.linspace(1, 0, b)
    return x


def ecrire(nom, x, long):
    """En WAV 16 bits pour un son court, en AU mu-law pour un long. Le format est choisi par ffmpeg."""
    x = np.clip(x, -1, 1).astype(np.float32)
    sortie = SORTIE / (nom + (".au" if long else ".wav"))
    codec = ["-c:a", "pcm_mulaw", "-f", "au"] if long else ["-c:a", "pcm_s16le"]
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-f", "f32le", "-ar", str(SR), "-ac", "1", "-i", "-", *codec, str(sortie)],
                   input=x.tobytes(), check=True)
    return sortie


def preparer(nom, fichier, t):
    x = lire(telecharger(fichier))
    debut = int(t.get("debut", 0) * SR)
    x = x[debut: debut + int(t["duree"] * SR)] if "duree" in t else x[debut:]
    x = filtre(x, t.get("bas"), t.get("haut"))
    if t["genre"] == "ponctuel":
        x = niveau_crete(nettoyer(x), t["niveau"])
    else:
        if t["genre"] == "boucle" or t.get("boucle", True):
            x = boucler(x, 2.0 if t["genre"] == "musique" else 1.0)
        x = niveau_rms(x, t["niveau"])
        crete = np.abs(x).max()
        if crete > 0.95:
            x *= 0.95 / crete
    return ecrire(nom, x, t["genre"] != "ponctuel" and len(x) > 6 * SR)


# ---------------------------------------------------------------- les sons fabriques ici
rng = np.random.default_rng(2026)


def temps(duree):
    return np.arange(int(duree * SR)) / SR


def bruit_brun(n):
    b = np.cumsum(rng.standard_normal(n))
    b = signal.sosfiltfilt(signal.butter(1, 20, "high", fs=SR, output="sos"), b)
    return b / (np.abs(b).max() + 1e-9)


def note(f, duree, attaque=0.004, declin=6.0, harmoniques=((1, 1.0), (2, 0.35), (3, 0.12))):
    """Une note douce, entre le piano electrique et la cloche."""
    t = temps(duree)
    x = sum(a * np.sin(2 * np.pi * f * h * t) * np.exp(-declin * h * t) for h, a in harmoniques)
    x *= np.minimum(1, t / attaque)
    return x


def suite(notes, ecart):
    total = int((ecart * (len(notes) - 1) + 1.2) * SR) + 2
    x = np.zeros(total)
    for k, f in enumerate(notes):
        n = note(f, 1.2)
        i = int(k * ecart * SR)
        x[i: i + len(n)] += n[: len(x) - i]
    return x


def echo(x, retard=0.11, gain=0.35, fois=4):
    y = np.concatenate([x, np.zeros(int(retard * fois * SR))])
    for k in range(1, fois + 1):
        i = int(retard * k * SR)
        y[i: i + len(x)] += x * gain ** k
    return y


def fabriquer():
    sons = {}
    # l'interface
    sons["ui_valide"] = (niveau_crete(note(880, 0.25, declin=14) + 0.6 * np.pad(note(1320, 0.22, declin=14), (int(0.05 * SR), 0))[: int(0.25 * SR)], -10), False)
    sons["ui_retour"] = (niveau_crete(note(523, 0.2, declin=16), -12), False)
    sons["ui_refus"] = (niveau_crete(note(196, 0.3, declin=9) + note(147, 0.3, declin=9) * 0.8, -9), False)
    sons["objectif"] = (niveau_crete(echo(suite([659, 784, 988], 0.09)), -9), False)
    sons["mission"] = (niveau_crete(echo(suite([523, 659, 784, 988, 1319], 0.12), 0.16, 0.4, 5), -8), False)
    sons["nouvelle_mission"] = (niveau_crete(echo(suite([392, 587], 0.16), 0.18, 0.35, 4), -10), False)
    t = temps(0.045)                                                    # le tic des dialogues : une voix, accordee en jeu
    sons["voix"] = (niveau_crete(signal.sawtooth(2 * np.pi * 220 * t, 0.5) * np.exp(-t * 60) * np.minimum(1, t / 0.003), -16), False)
    t = temps(1.0)                                                      # le telephone qui vibre, deux fois
    vib = np.sin(2 * np.pi * 170 * t) * (np.sin(2 * np.pi * 31 * t) > -0.2) * (((t % 0.5) < 0.32))
    sons["vibreur"] = (niveau_crete(filtre(vib, bas=900), -12), False)
    n = rng.standard_normal(int(1.4 * SR))                              # la neige de la tele
    sons["tele"] = (niveau_crete(filtre(n, bas=6000, haut=400) * np.linspace(1, 0.6, len(n)), -16), False)
    # le scooter : un deux-temps de 50 cm3, une explosion par tour, environ 95 par seconde
    periode = 232
    impulsion = np.zeros(periode)
    impulsion[:40] = np.exp(-np.arange(40) / 7) * np.sin(np.arange(40) / 2.5)
    moteur = np.tile(impulsion, 95) * (1 + 0.15 * np.sin(np.linspace(0, 2 * np.pi * 3, periode * 95)))
    moteur = filtre(moteur, bas=2400, haut=90) + 0.25 * filtre(rng.standard_normal(len(moteur)), bas=3000, haut=600)
    sons["scooter"] = (niveau_rms(boucler(moteur, 0.05), -20), False)
    t = temps(1.1)
    regime = 40 + 70 * np.clip((t - 0.25) / 0.6, 0, 1) ** 0.7
    phase = 2 * np.pi * np.cumsum(regime) / SR
    demarre = (signal.square(phase, 0.2) * 0.6 + rng.standard_normal(len(t)) * 0.3) * np.minimum(1, t / 0.05) * np.where(t < 0.2, 0.4, 1)
    sons["scooter_demarre"] = (niveau_crete(filtre(demarre, bas=2000, haut=70), -8), False)
    # le metro
    n = int(4.0 * SR)
    rame = 0.9 * filtre(bruit_brun(n), bas=260) + 0.25 * filtre(rng.standard_normal(n), bas=1800, haut=500)
    rame *= 1 + 0.12 * np.sin(np.linspace(0, 2 * np.pi * 2, n))
    for debut in (0.30, 0.42, 2.30, 2.42):                              # les joints des rails : ta-dam, ta-dam
        i = int(debut * SR)
        choc = filtre(rng.standard_normal(int(0.08 * SR)), bas=900) * np.exp(-np.arange(int(0.08 * SR)) / (0.012 * SR))
        rame[i: i + len(choc)] += 1.6 * choc
    sons["amb_metro"] = (niveau_rms(boucler(rame, 0.3), -21), False)
    t = temps(2.6)
    crisse = (np.sin(2 * np.pi * (2300 + 60 * np.sin(2 * np.pi * 6 * t)) * t) + 0.4 * np.sin(2 * np.pi * 3450 * t))
    crisse *= np.clip(t / 0.6, 0, 1) * np.clip((2.6 - t) / 0.9, 0, 1) * (0.6 + 0.4 * np.sin(2 * np.pi * 1.7 * t))
    sons["metro_freins"] = (niveau_crete(crisse * 0.5 + filtre(rng.standard_normal(len(t)), haut=3000) * 0.15, -14), False)
    t = temps(0.9)
    souffle = filtre(rng.standard_normal(len(t)), bas=7000, haut=1500) * np.exp(-t * 4) * np.minimum(1, t / 0.02)
    souffle[int(0.7 * SR): int(0.7 * SR) + 400] += filtre(rng.standard_normal(400), bas=400) * 2
    sons["metro_portes"] = (niveau_crete(souffle, -9), False)
    t = temps(1.3)
    sons["metro_signal"] = (niveau_crete(filtre(signal.square(2 * np.pi * 740 * t), bas=2500) * (t < 1.2), -18), False)
    sons["metro_annonce"] = (niveau_crete(echo(suite([659, 831, 988], 0.22), 0.2, 0.3, 3), -10), False)
    # le vent sec de Namek
    n = int(10 * SR)
    vent = rng.standard_normal(n)
    centre = 500 + 350 * np.sin(np.linspace(0, 2 * np.pi * 2, n))
    morceaux = []
    for k in range(0, n, 2205):
        b, a = signal.butter(2, [max(80, centre[k] * 0.6), centre[k] * 1.6], "band", fs=SR)
        morceaux.append(signal.lfilter(b, a, vent[k: k + 2205]))
    vent = np.concatenate(morceaux) * (0.6 + 0.4 * np.sin(np.linspace(0, 2 * np.pi * 3, n)))
    sons["amb_vent"] = (niveau_rms(boucler(vent, 1.0), -27), True)
    # le bourdon des neons
    t = temps(2.0)
    neon = sum(np.sin(2 * np.pi * 100 * h * t) / h for h in (1, 2, 3, 5, 7))
    neon += 0.05 * filtre(rng.standard_normal(len(t)), haut=4000)
    sons["amb_neon"] = (niveau_rms(neon, -34), False)
    for nom, (x, long) in sons.items():
        ecrire(nom, x, long)
    return sorted(sons)


CREDITS_ENTETE = """# Les sons et les musiques du jeu

Tous les fichiers de ce dossier sont produits par `designs/sons/preparer.py`, qui les télécharge, les découpe,
les met en boucle, les filtre et les ramène à un niveau commun. Ces modifications sont signalées ici, comme le
demandent les licences CC-BY-SA.

## Enregistrements et musiques sous licence libre

| Fichier du jeu | Œuvre d'origine | Auteur | Licence | Source |
|---|---|---|---|---|
"""

CREDITS_FIN = """
Les fichiers sous **CC-BY-SA 4.0** (Gregor Quendel) restent sous cette licence une fois modifiés : on peut les
réutiliser à condition de citer l'auteur et de les partager sous la même licence. Les fichiers **CC0** sont dans
le domaine public ; leurs auteurs sont cités quand même.

## Sons fabriqués pour le jeu

Aucun enregistrement libre ne convenait pour ceux-ci : ils sont calculés par `preparer.py` (bruit filtré,
oscillateurs, enveloppes). Ils n'ont pas d'auteur extérieur et suivent la licence du jeu.

{fabriques}
"""


def credits(fabriques):
    lignes = []
    for nom, (fichier, page, auteur, licence, t) in ENREGISTREMENTS.items():
        oeuvre = fichier.split("#")[-1].replace("%20", " ")
        lignes.append(f"| `{nom}` | {oeuvre} | {auteur} | {licence} | https://opengameart.org/content/{page} |")
    texte = CREDITS_ENTETE + "\n".join(lignes) + CREDITS_FIN.format(fabriques=", ".join(f"`{n}`" for n in fabriques))
    (SORTIE / "CREDITS.md").write_text(texte, encoding="utf-8")


def main():
    SORTIE.mkdir(parents=True, exist_ok=True)
    for f in SORTIE.glob("*"):
        if f.suffix in (".wav", ".au") and f.stem not in ENREGISTREMENTS:
            f.unlink()
    for nom, (fichier, page, auteur, licence, t) in ENREGISTREMENTS.items():
        sortie = preparer(nom, fichier, t) if not any((SORTIE / (nom + e)).exists() for e in (".wav", ".au")) or "-tout" in sys.argv \
            else next(SORTIE / (nom + e) for e in (".wav", ".au") if (SORTIE / (nom + e)).exists())
        print(f"{nom:22s} {sortie.stat().st_size // 1024:6d} ko  ({auteur}, {licence})")
    fabriques = fabriquer()
    print("fabriques :", ", ".join(fabriques))
    credits(fabriques)
    total = sum(f.stat().st_size for f in SORTIE.iterdir())
    print(f"total : {total / 1e6:.1f} Mo")


if __name__ == "__main__":
    main()
