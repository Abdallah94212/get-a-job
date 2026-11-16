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
    "objet": ("sfx_100_v2.zip#sfx100v2_items_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-9)),
    "cles": ("100-CC0-SFX_0.zip#key_open_01.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-9)),
    "papier": ("100-CC0-SFX_0.zip#paper_02.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-8)),
    "serrure": ("sfx_100_v2.zip#sfx100v2_lock_open_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-9)),
    "ui_deplace": ("sfx_100_v2.zip#sfx100v2_switch_01.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-14)),
    # la vie de la rue, au loin : des bruits qui passent, au hasard
    "sirene_loin": ("siren_0.mp3", "sirens-and-alarm-noise", "aquinn", "CC0", dict(genre="ponctuel", debut=3, duree=9, bas=1600, niveau=-14)),
    "chien_loin": ("dog_barking_mono.wav", "dog-barking-mono", "haeldb", "CC0", dict(genre="ponctuel", bas=2400, niveau=-14)),
    "moto_passe": ("motorcycle_engine_-_start_and_drive_away.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                   dict(genre="ponctuel", debut=7, duree=9, bas=3500, niveau=-12)),
    "alarme_loin": ("sfx_loops.zip#alarm_02.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0", dict(genre="ponctuel", duree=4, bas=1800, niveau=-20)),
    "orage_1": ("thunder_-_lightning_-_i_-_without_rain_wind_background_noise.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                dict(genre="ponctuel", duree=12, niveau=-5)),
    "orage_2": ("thunder_-_lightning_-_ii_-_without_rain_wind_background_noise.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                dict(genre="ponctuel", duree=12, niveau=-5)),
    "orage_3": ("thunder_-_lightning_-_iii_-_without_rain_wind_background_noise.mp3", "free-general-ambience-sounds", "Gregor Quendel", "CC-BY-SA 4.0",
                dict(genre="ponctuel", duree=12, niveau=-5)),
    "amb_periph": ("sfx_100_v2.zip#sfx100v2_loop_highway.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="boucle", bas=700, niveau=-33)),
    "bouteille": ("rpg_sound_pack.zip#bottle.wav", "rpg-sound-pack", "artisticdude", "CC0", dict(genre="ponctuel", niveau=-16)),
    "vaisselle": ("100-CC0-SFX_0.zip#dishes_02.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-16)),
    "micro_ondes": ("100-CC0-SFX_0.zip#microwave_door_open.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-15)),
    "flaque": ("100-CC0-SFX_0.zip#splash_01.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-15)),
    # le corps
    "aie_1": ("01._damage_grunt_male.wav", "damage-sounds-male-audio-pack", "VoiceBosch", "CC-BY-SA 4.0", dict(genre="ponctuel", niveau=-8)),
    "aie_2": ("02._damage_grunt_male.wav", "damage-sounds-male-audio-pack", "VoiceBosch", "CC-BY-SA 4.0", dict(genre="ponctuel", niveau=-8)),
    "aie_3": ("05._damage_grunt_male.wav", "damage-sounds-male-audio-pack", "VoiceBosch", "CC-BY-SA 4.0", dict(genre="ponctuel", niveau=-8)),
    "effort": ("03._effort_grunt_male.wav", "effort-sounds-male-audio-pack", "VoiceBosch", "CC-BY-SA 4.0", dict(genre="ponctuel", niveau=-11)),
    "souffle": ("breathing%20tired.wav", "breathing-tired", "mikeask", "CC0", dict(genre="ponctuel", niveau=-11)),
    "coeur": ("heartbeat_slow_0.wav", "heartbeat-sounds", "bart", "CC0", dict(genre="ponctuel", niveau=-7)),
    "coeur_vite": ("heartbeat_fast_0.wav", "heartbeat-sounds", "bart", "CC0", dict(genre="ponctuel", niveau=-7)),
    "coup": ("100-CC0-SFX_0.zip#hit_02.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-8)),
    "choc": ("100-CC0-SFX_0.zip#slam_03.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-6)),
    # l'argent, le telephone, les objets
    "argent": ("Coins%20sounds%20%5BOGG%5D.zip#1_Coins.ogg", "coins-sounds", "Jalastram", "CC-BY 3.0", dict(genre="ponctuel", niveau=-11)),
    "piece": ("rpg_sound_pack.zip#coin2.wav", "rpg-sound-pack", "artisticdude", "CC0", dict(genre="ponctuel", niveau=-13)),
    "notif": ("chirptone.wav", "modern-ringtone-chirptone", "Zane Little Music", "CC0", dict(genre="ponctuel", duree=1.2, niveau=-15)),
    "colis": ("100-CC0-SFX_0.zip#wooded_box_open.ogg", "100-cc0-sfx", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-10)),
    "clic": ("rpg_sound_pack.zip#metal-small1.wav", "rpg-sound-pack", "artisticdude", "CC0", dict(genre="ponctuel", niveau=-14)),
    "souffle_air": ("sfx_100_v2.zip#sfx100v2_air_02.ogg", "100-cc0-sfx-2", "rubberduck", "CC0", dict(genre="ponctuel", niveau=-11)),
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

# ---------------------------------------------------------------- la bibliotheque elargie (direction sonore)
KENNEY_RPG = ("RPGsounds_Kenney.zip", "50-rpg-sound-effects", "Kenney", "CC0")
KENNEY_UI = ("kenney_interfaceSounds.zip", "interface-sounds", "Kenney", "CC0")
PAS = ("footsteps_0.zip", "footsteps-on-different-surfaces", "congusbongus", "CC-BY 3.0")
MAISON = ("sounds_8.zip", "various-sound-effects", "laleksic", "CC0")
FOLEY = ("foley_pack.zip", "super-foley-pack", "dklon", "CC-BY 3.0")
RD1 = ("100-CC0-SFX_0.zip", "100-cc0-sfx", "rubberduck", "CC0")
RD2 = ("sfx_100_v2.zip", "100-cc0-sfx-2", "rubberduck", "CC0")
RDB = ("sfx_loops.zip", "30-cc0-sfx-loops", "rubberduck", "CC0")


def depuis(pack, dedans, **t):
    archive, page, auteur, licence = pack
    return (archive + "#" + dedans, page, auteur, licence, dict(genre="ponctuel", niveau=-10) | t)


ENREGISTREMENTS.update({
    # les pas, sol par sol : six prises par sol, pour ne jamais entendre deux fois le meme pas
    **{f"pas_carrelage_{k + 1}": depuis(PAS, f"tile/{k}.ogg", niveau=-11) for k in range(6)},
    **{f"pas_beton_{k + 1}": depuis(PAS, f"boots/{k}.ogg", niveau=-11) for k in range(6)},
    **{f"pas_metal_{k + 1}": depuis(PAS, f"metal/{k}.ogg", niveau=-13, bas=5000) for k in range(5)},
    **{f"pas_gravier_{k + 1}": depuis(PAS, f"gravel/{k}.ogg", niveau=-12) for k in range(5)},
    **{f"pas_flaque_{k + 1}": depuis(PAS, f"water/{k}.ogg", niveau=-12) for k in range(4)},
    **{f"feuillage_{k + 1}": depuis(PAS, f"grass/{k}.ogg", niveau=-11) for k in range(3)},
    **{f"parquet_{k + 1}": depuis(MAISON, f"floor_creak{k + 1}.wav", niveau=-15) for k in range(3)},
    # les gestes
    "poignee": depuis(KENNEY_RPG, "metalLatch.ogg", niveau=-12),
    "declic": depuis(KENNEY_RPG, "metalClick.ogg", niveau=-14),
    "livre_ouvre": depuis(KENNEY_RPG, "bookOpen.ogg", niveau=-11),
    "livre_ferme": depuis(KENNEY_RPG, "bookClose.ogg", niveau=-11),
    **{f"livre_page_{k}": depuis(KENNEY_RPG, f"bookFlip{k}.ogg", niveau=-13) for k in (1, 2, 3)},
    **{f"pose_{k}": depuis(KENNEY_RPG, f"bookPlace{k}.ogg", niveau=-12) for k in (1, 2, 3)},
    "sac_pose": depuis(KENNEY_RPG, "dropLeather.ogg", niveau=-10),
    **{f"vetement_{k}": depuis(KENNEY_RPG, f"cloth{k}.ogg", niveau=-14) for k in (1, 2, 3, 4)},
    **{f"grince_{k}": depuis(KENNEY_RPG, f"creak{k}.ogg", niveau=-15) for k in (1, 2, 3)},
    "couteau": depuis(KENNEY_RPG, "knifeSlice.ogg", niveau=-13),
    "hachoir": depuis(KENNEY_RPG, "chop.ogg", niveau=-11),
    "monnaie": depuis(KENNEY_RPG, "handleCoins.ogg", niveau=-12),
    "porte_ouvre_bois": depuis(KENNEY_RPG, "doorOpen_1.ogg", niveau=-10),
    **{f"porte_ferme_{k}": depuis(KENNEY_RPG, f"doorClose_{k}.ogg", niveau=-9) for k in (1, 2, 3)},
    "tiroir_ouvre": depuis(MAISON, "drawer_open.wav", niveau=-12),
    "tiroir_ferme": depuis(MAISON, "drawer_close.wav", niveau=-12),
    "interrupteur": depuis(MAISON, "switch_on.wav", niveau=-13),
    "robinet": depuis(MAISON, "tap_water.wav", niveau=-14, duree=3, bas=1800),
    "plaque_egout": depuis(MAISON, "manhole_open.wav", niveau=-10),
    "porte_grince": depuis(MAISON, "door_creak_open.wav", niveau=-12),
    "cles_main": depuis(MAISON, "pickup_keys.wav", niveau=-12),
    "chaise": depuis(FOLEY, "chair_push.wav", niveau=-12, duree=1.4),
    "corps_chute": depuis(FOLEY, "body_hit_on_ground.wav", niveau=-8, duree=1.2),
    "froissement": depuis(FOLEY, "leather_rustling.wav", niveau=-15, duree=1.0),
    **{f"vaisselle_{k}": depuis(RD1, f"dishes_0{k}.ogg", niveau=-16) for k in (1, 3, 4)},
    **{f"tinte_{k}": depuis(RD1, f"glass_0{k}.ogg", niveau=-17) for k in (1, 3, 4, 5)},
    **{f"ferraille_{k}": depuis(RD1, f"metal_{k:02d}.ogg", niveau=-13) for k in (1, 2, 5, 7, 9)},
    **{f"outil_{k}": depuis(RD1, f"tools_0{k}.ogg", niveau=-13) for k in (1, 3, 4, 5)},
    "casserole": depuis(RD1, "pot_01.ogg", niveau=-14),
    "clochette_2": depuis(RD1, "bell_02.ogg", niveau=-11),
    "bequille": depuis(RD2, "sfx100v2_metal_hit_01.ogg", niveau=-11),
    "caisse_pose": depuis(RD2, "sfx100v2_wood_hit_02.ogg", niveau=-9),
    **{f"cailloux_{k}": depuis(RD2, f"sfx100v2_stones_0{k}.ogg", niveau=-15) for k in (1, 2, 3)},
    "amb_ruissellement": ("sfx_loops.zip#water_flowing.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0",
                          dict(genre="boucle", bas=2600, haut=180, niveau=-30)),
    "amb_compresseur": ("sfx_loops.zip#pump_01.ogg", "30-cc0-sfx-loops", "rubberduck", "CC0",
                        dict(genre="boucle", bas=1400, niveau=-31)),
    # le telephone et les menus : une seule famille de sons, douce, vitree
    "tel_tic": depuis(KENNEY_UI, "glass_002.ogg", niveau=-17),
    "tel_ouvre": depuis(KENNEY_UI, "maximize_002.ogg", niveau=-15),
    "tel_ferme": depuis(KENNEY_UI, "minimize_002.ogg", niveau=-15),
    "tel_achat": depuis(KENNEY_UI, "confirmation_001.ogg", niveau=-13),
    "tel_erreur": depuis(KENNEY_UI, "error_006.ogg", niveau=-15),
    "ui_deplace": depuis(KENNEY_UI, "click_002.ogg", niveau=-16),
    "ui_retour": depuis(KENNEY_UI, "back_002.ogg", niveau=-15),
})


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
    extrait = CACHE / ("z_" + archive.split(".")[0] + "_" + dedans.replace("/", "_"))
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
    a, b = int(0.002 * SR), min(int(0.02 * SR), len(x) // 3)
    x[:a] *= np.linspace(0, 1, a)
    x[-b:] *= np.linspace(1, 0, b)
    return x


CATEGORIES = [  # le rangement de la bibliotheque : (dossier, debuts de nom)
    ("musique", ("mus_",)),
    ("ambiances", ("amb_",)),
    ("pas", ("pas_", "parquet_")),
    ("interface", ("ui_", "tel_", "objectif", "mission", "nouvelle_mission", "notif", "vibreur", "chapitre")),
    ("vehicules", ("scooter", "voiture_", "moto_", "bus_", "klaxon_", "bequille")),
    ("metro", ("metro_",)),
    ("cinema", ("tele", "acouphene", "montee", "coup_sourd", "cigarette_")),
    ("ville", ("sirene_", "chien_", "alarme_", "orage_", "tonnerre", "rafale_", "bouteille", "flaque", "gresillement",
               "plaque_egout", "sonnerie_", "ascenseur", "clavier", "voisins_", "bip_", "robinet")),
    ("corps", ("aie_", "effort", "souffle", "coeur", "coup", "choc", "corps_")),
]


def dossier(nom):
    for categorie, debuts in CATEGORIES:
        if nom.startswith(debuts):
            return SORTIE / categorie
    return SORTIE / "objets"


def ecrire(nom, x, long):
    """En WAV 16 bits pour un son court, en AU mu-law pour un long. Le format est choisi par ffmpeg."""
    x = np.clip(x, -1, 1).astype(np.float32)
    dossier(nom).mkdir(parents=True, exist_ok=True)
    sortie = dossier(nom) / (nom + (".au" if long else ".wav"))
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
    sons["ui_refus"] = (niveau_crete(note(196, 0.3, declin=9) + note(147, 0.3, declin=9) * 0.8, -9), False)
    sons["objectif"] = (niveau_crete(echo(suite([659, 784, 988], 0.09)), -9), False)
    sons["mission"] = (niveau_crete(echo(suite([523, 659, 784, 988, 1319], 0.12), 0.16, 0.4, 5), -8), False)
    sons["nouvelle_mission"] = (niveau_crete(echo(suite([392, 587], 0.16), 0.18, 0.35, 4), -10), False)
    t = temps(1.0)                                                      # le telephone qui vibre, deux fois
    vib = np.sin(2 * np.pi * 170 * t) * (np.sin(2 * np.pi * 31 * t) > -0.2) * (((t % 0.5) < 0.32))
    sons["vibreur"] = (niveau_crete(filtre(vib, bas=900), -12), False)
    n = rng.standard_normal(int(1.4 * SR))                              # la neige de la tele
    # une voiture qui passe sur la chaussee mouillee : le chuintement des pneus, qui monte, passe et s'eloigne
    t = temps(3.2)
    enveloppe = np.exp(-((t - 1.5) / 0.55) ** 2)
    pneus = filtre(rng.standard_normal(len(t)), bas=2600, haut=300) * enveloppe
    grave = filtre(bruit_brun(len(t)), bas=220) * enveloppe * 2.5
    hauteur = 70 * (1.12 - 0.24 / (1 + np.exp(-(t - 1.5) * 4)))          # l'effet Doppler, a peine
    moteur = np.sin(2 * np.pi * np.cumsum(hauteur) / SR) * enveloppe * 0.4
    sons["voiture_passe"] = (niveau_crete(pneus + grave + moteur, -12), False)
    # le gresillement d'une enseigne : de petits craquements secs
    g = np.zeros(int(0.7 * SR))
    for k in range(9):
        i = rng.integers(0, len(g) - 400)
        g[i:i + 300] += rng.standard_normal(300) * np.exp(-np.arange(300) / 60) * rng.uniform(0.4, 1)
    sons["gresillement"] = (niveau_crete(filtre(g, haut=1500), -16), False)
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
    # la direction sonore : la tele, le choc, la rue, les interieurs, le telephone (sons/bruitages.py)
    import bruitages
    syllabes = {g: [lire(f) for f in sorted((SORTIE / "voix" / g).glob(f"v_{g}_[0-9]*.au"))] for g in ("homme", "femme_passante")}
    source_radio = lire(telecharger("chilllofir-loop.ogg"))
    sons.update(bruitages.fabriquer(syllabes["homme"], syllabes["femme_passante"], source_radio))
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

## Les voix des personnages

Les dossiers `voix/<personnage>` contiennent les syllabes et les réactions (rire, soupir, « hm », « hein », surprise,
grognement, inspiration) de chaque personnage, et `voix/tele_emission` le présentateur de la télé. Elles sont
calculées par `designs/sons/voix.py` (synthèse source-filtre : une corde vocale, trois formants, des consonnes) :
aucune voix humaine enregistrée, aucun doublage généré. Elles suivent la licence du jeu.

## Rangement

`musique/`, `ambiances/` (boucles), `ville/` (bruits de la rue et du dehors), `vehicules/`, `metro/`, `pas/`
(un sol par famille de prises), `objets/` (les gestes), `corps/`, `cinema/` (ce qui sert aux cinématiques),
`interface/` (menus et téléphone), `voix/`. Un nom terminé par `_1`, `_2`... est une prise d'une famille :
le jeu en joue une au hasard, jamais deux fois la même de suite.
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
    for f in SORTIE.glob("*.*"):                                       # l'ancien rangement, tout a plat
        if f.suffix in (".wav", ".au"):
            f.unlink()
    for f in SORTIE.glob("*/*"):
        if f.parent.name != "voix" and f.suffix in (".wav", ".au") and f.stem not in ENREGISTREMENTS:
            f.unlink()
    for nom, (fichier, page, auteur, licence, t) in ENREGISTREMENTS.items():
        deja = [dossier(nom) / (nom + e) for e in (".wav", ".au") if (dossier(nom) / (nom + e)).exists()]
        sortie = preparer(nom, fichier, t) if not deja or "-tout" in sys.argv else deja[0]
        print(f"{nom:22s} {sortie.stat().st_size // 1024:6d} ko  ({auteur}, {licence})")
    fabriques = fabriquer()
    print("fabriques :", ", ".join(fabriques))
    credits(fabriques)
    total = sum(f.stat().st_size for f in SORTIE.rglob("*") if f.is_file())
    print(f"total : {total / 1e6:.1f} Mo")


if __name__ == "__main__":
    main()
