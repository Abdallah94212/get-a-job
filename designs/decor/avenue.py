"""Construit toutes les cartes du jeu, en 2D vue de cote, a partir de l'avenue de Fontainebleau au Kremlin-Bicetre.

L'avenue est decoupee en trois grandes cartes qui s'enchainent : le bas (le Maitre Poulet), le milieu (notre rue,
le garage) et le haut (les bureaux, la tour Krousty Corp). On entre dans les lieux par leur porte : chaque
interieur est une carte a part.
Reperes en pixels d'ecran (480 x 270) : x suit l'avenue, y descend ; le pied des facades est a y = SOL.
Lancement : python decor/avenue.py [acte]   (l'acte, de 1 a 7, change les commerces rachetes et le mur de Sisyphe)
"""
import random
import shutil
import sys
from pathlib import Path
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from outils import *
import facades as F
import interieurs as I
import meubles as M
import objets as O
import palette as PAL
import personnages as P

RACINE = Path(__file__).resolve().parents[2]
SORTIE = RACINE / "jeu" / "ressources" / "decor"
INTERFACE = RACINE / "jeu" / "ressources" / "interface"
ACTE = int(sys.argv[1]) if len(sys.argv) > 1 else 3

ECRAN = 480
SOL, BORDURE, ROUTE, BAS = 180, 206, 210, 270       # pied des facades, bord du trottoir, chaussee, bas de l'ecran
LOIN, FOND, OBJETS, DEVANT, LUMIERES = range(5)     # les couches, du fond vers l'avant
ADDITIF, SCINTILLE, CLIGNOTE, MIROIR = 1, 2, 4, 8

TEX, CARTES, rng = {}, {}, random.Random(2026)


class Carte:
    """Une carte en cours de construction : ses lignes de niveau et ce qu'il faut retenir pour la meubler."""

    def __init__(self, nom, exterieur=True):
        self.nom, self.exterieur = nom, exterieur
        self.lignes, self.trouees, self.degages, self.utilisees = [], [], [], set()
        self.x, self.reperes = 0, {}
        CARTES[nom] = self


C = None  # la carte qu'on est en train de construire


def tex(nom, image):
    TEX[nom] = image
    return nom


def image(nom, couche, x, y, parallaxe=1.0, drapeaux=0, lum=1.0):
    C.utilisees.add(nom)
    C.lignes.append(f"image {nom} {couche} {x:.0f} {y:.0f} {parallaxe:.3f} {drapeaux} {lum:.2f}")


def pose(nom, x, base, couche=OBJETS, **k):
    """Pose une image debout, son pied a la hauteur donnee."""
    image(nom, couche, x, base - TEX[nom].height, **k)


def lueur(nom, cx, cy, force):
    image(nom, LUMIERES, cx - TEX[nom].width // 2, cy - TEX[nom].height // 2, drapeaux=ADDITIF, lum=force)


def solide(x0, y0, x1, y1):
    C.lignes.append(f"solide {x0:.0f} {y0:.0f} {x1:.0f} {y1:.0f}")


def etiquette(texte):
    nom = "etiq_" + "".join(c if c.isalnum() else "_" for c in texte.lower())
    if nom not in TEX:
        tex(nom, O.etiquette(texte))
    C.utilisees.add(nom)
    return nom


def zone(texte, x0, x1):
    C.lignes.append(f"zone {len(C.lignes)} {x0:.0f} {x1:.0f} {etiquette(texte)}")


def porte(x0, x1, cible, ax, ay, texte=None, y_max=SOL + 22):
    """Un passage vers une autre carte. Sans texte, il se declenche tout seul (les bouts de l'avenue)."""
    C.lignes.append(f"porte {x0:.0f} {x1:.0f} {y_max if texte else BAS} {cible} {ax:.0f} {ay:.0f} "
                    f"{0 if texte else 1} {etiquette(texte) if texte else '-'}")


def B(genre, travees, nom="", **k):
    return dict(type=genre, travees=travees, nom=nom, **k)


def teinte(img, k, voile=0.0):
    """Assombrit une image et la noie un peu dans la nuit : pour ce qui est loin."""
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a >= 128:
                c = [min(255, int(v * (k if a == 255 else 1))) for v in (r, g, b)]
                px[x, y] = tuple(int(c[i] + (NUIT[i] - c[i]) * voile) for i in range(3)) + (a,)
    return out


# ---------------------------------------------------------------- les objets du pack pH64
def _cle(nom, amb, echelle, allume, ecran):
    cle = f"m_{nom}_{amb}{echelle}{'a' if allume else ''}{'e' if ecran else ''}"
    if cle not in TEX:
        tex(cle, M.objet(nom, amb, echelle, allume, ecran))
    return cle


def meuble(nom, x, base, amb, couche=FOND, echelle=2, allume=False, ecran=None, miroir=False, dur=False, halo=0.0):
    """Pose un objet du pack, son pied a la hauteur donnee. dur : on ne le traverse pas. halo : sa lampe eclaire."""
    cle = _cle(nom, amb, echelle, allume, ecran)
    pose(cle, x, base, couche, drapeaux=MIROIR if miroir else 0)
    w, h = TEX[cle].size
    if dur:
        solide(x + 3, base - 8, x + w - 3, base + 1)
    if halo:
        lueur("halo_chaud", x + w // 2, base - h + 10, halo)
    return w


def pendu(nom, cx, amb, plafond=36, halo=0.4):
    """Une suspension accrochee au plafond, allumee."""
    cle = _cle(nom, amb, 2, True, None)
    image(cle, FOND, cx - TEX[cle].width // 2, plafond)
    lueur("halo_chaud", cx, plafond + TEX[cle].height - 4, halo)


def ensemble(nom, x, base, amb, pieces, dur=True):
    """Plusieurs objets qui vont ensemble (un bureau, son ecran, sa chaise) : une seule image, donc une seule profondeur.
    pieces : (objet, decalage en x, hauteur du pied, options). Pose dans la piece : le joueur passe devant ou derriere."""
    cle = f"e_{C.nom}_{nom}"
    images = []
    for objet, dx, pied, *options in pieces:
        k = options[0] if options else {}
        im = TEX[objet[1:]] if objet.startswith("@") else M.objet(objet, amb, 2, k.get("allume", False), k.get("ecran"))
        images.append((im.transpose(Image.FLIP_LEFT_RIGHT) if k.get("miroir") else im, dx, pied))
    tex(cle, M.groupe(images))
    pose(cle, x, base, OBJETS)
    if dur:
        solide(x + 3, base - 8, x + TEX[cle].width - 3, base + 1)


def dehors(nom, x, base, **k):
    """Un objet du pack sur le trottoir : a l'echelle 1, aux couleurs de la nuit, trie avec le joueur."""
    return meuble(nom, x, base, "rue", OBJETS, 1, **k)


# ---------------------------------------------------------------- textures communes
HISTOIRE = set()  # les images des personnages et des cinematiques : elles vont dans ressources/interface


def textures():
    for nom, img in P.tous(RACINE).items():
        tex(nom, img)
        HISTOIRE.add(nom)
    for groupe in (O.petits(), O.abri(), O.metro(), O.divers(), O.mobilier(), O.accessoires()):
        for nom, img in groupe.items():
            tex(nom, img)
    sols = O.sols()
    t = Toile(64, BAS - SOL)
    t.img.paste(sols["sol_trottoir"].crop((0, 0, 64, BORDURE - SOL)), (0, 0))
    t.rect(0, BORDURE - SOL, 64, ROUTE - SOL, (118, 118, 124))
    t.rect(0, BORDURE - SOL, 64, BORDURE - SOL + 1, (142, 142, 148))
    t.img.paste(sols["sol_route"].crop((0, 0, 64, BAS - ROUTE)), (0, ROUTE - SOL))
    tex("sol", t.img)
    tex("tiret", Toile(40, 2, (160, 160, 158, 255)).img)
    t = Toile(46, BAS - ROUTE - 4)
    for y in range(0, t.h, 7):
        t.rect(0, y, 46, y + 4, (166, 166, 164))
    tex("passage", t.img)
    tex("sol_parvis", sols["sol_parvis"].crop((0, 0, 64, BORDURE - SOL)))
    tex("sol_cour", teinte(sols["sol_cour"].crop((0, 0, 64, 24)), 0.8))
    tex("sol_rue", teinte(sols["sol_route"].crop((0, 0, 64, 24)), 0.75))
    tex("ciel", O.ciel())
    tex("pin", O.pin())
    for nom, img in O.arbres().items():
        tex(nom, img)
    tex("lampadaire", O.lampadaire())
    for nom, c, taille in (("halo_chaud", (255, 236, 190), 56), ("halo_orange", ORANGE, 150), ("halo_vert", VERT, 130),
                           ("halo_froid", (200, 220, 255), 150), ("halo_salle", (255, 236, 190), 260)):
        tex(nom, O.halo(c, taille))
    tex("flaque", O.halo((255, 236, 190), 64).resize((150, 30), Image.NEAREST))
    tex("flaque_verte", O.halo(VERT, 64).resize((130, 26), Image.NEAREST))
    for genre in ("job", "poulet", "krousty"):
        tex("pub_" + genre, O.pub(genre))
    tex("garage_pignon", O.garage_pignon())
    tex("garage_avenue", O.garage_avenue(294))
    tex("labo", O.labo_mur())
    tex("pignon_sisyphe", O.pignon_sisyphe(150, 332, ACTE))
    tex("pignon_affiche", O.pignon_affiche(150, 356))
    t, r = O.pignon(64, 64, MUR_BLANC, 91)
    tags(t, 2, 20, 60, 40, r, 5)
    t.grain(5, 91)
    tex("muret", teinte(t.img, 0.7))
    tex("tele", Image.new("RGBA", (14, 28), TELE + (255,)))
    tex("tele_ecran", Image.new("RGBA", (48, 38), TELE + (255,)))
    t = Toile(30, 30)
    t.ellipse(0, 0, 30, 30, (240, 236, 224), FER)
    F.coq(t, 6, 8, (232, 100, 8), 2)
    tex("enseigne_coq", t.img)
    for nom, texte, style in (("laverie", "LAVERIE", "froid"), ("epicerie", "EPICERIE", "sombre"), ("cafe", "CAFE", "sombre"),
                              ("muscu", "MUSCU", "clair"), ("krousty", "KROUSTY", "orange")):
        tex("drapeau_" + nom, F.drapeau(texte, style, 3))
    aide = "FLECHES : MARCHER   MAJ : COURIR   E : ENTRER"
    t = Toile(largeur_texte(aide, 3) + 10, 11)
    t.rect(0, 0, t.l, 11, ENCRE)
    t.texte(5, 3, aide, (170, 170, 176), 3)
    tex("aide", t.img)
    v = O.voiture()
    auto = Image.new("RGBA", (120, 42), (0, 0, 0, 0))
    auto.paste(v["auto_cabine"], (28, 0))
    auto.paste(v["auto_flanc"], (0, 16), v["auto_flanc"])
    for k, gris in enumerate((0.5, 0.85, 1.15)):
        tex(f"auto_{k}", teinte(auto, gris))
    # le gabarit du joueur, de profil, en attendant le vrai heros
    t = Toile(20, 48)
    t.poly([(6, 12), (13, 12), (15, 20), (16, 44), (3, 44), (5, 20)], (60, 60, 74))
    t.rect(5, 43, 9, 48, (24, 24, 30))
    t.rect(10, 43, 15, 48, (24, 24, 30))
    t.ellipse(5, 1, 15, 12, (150, 140, 132))
    t.poly([(4, 6), (6, 0), (8, 3), (10, 0), (13, 2), (12, 6), (6, 7)], (24, 24, 30))
    t.rect(12, 6, 13, 7, (24, 24, 30))
    t.ligne([(5, 20), (3, 43)], (36, 36, 46))
    tex("joueur", t.img)
    # la tour : de pres au bout de l'avenue, et de plus en plus grande dans le lointain a mesure qu'on monte
    tour = O.tour()
    tex("tour_base", tour["tour_base"].resize((450, 350), Image.NEAREST))
    haute = Image.new("RGBA", (225, 535), (0, 0, 0, 0))
    haute.paste(tour["tour_antenne"].resize((6, 60)), (110, 0))
    haute.paste(tour["tour_sommet"], (62, 60))
    haute.paste(tour["tour_milieu"], (32, 185))
    haute.paste(tour["tour_base"], (0, 360))
    for nom, taille, voile in (("tour_loin_1", (46, 110), 0.45), ("tour_loin_2", (66, 157), 0.35), ("tour_loin_3", (94, 224), 0.2)):
        tex(nom, teinte(haute.resize(taille, Image.NEAREST), 1.0, voile))
    r = random.Random(77)
    t = Toile(256, 80)
    x = 0
    while x < 256:
        w, h = r.randrange(24, 60), r.randrange(30, 76)
        t.rect(x, 80 - h, x + w, 80, (38, 38, 50))
        for _ in range(w * h // 50):
            wx, wy = x + 2 + r.randrange(w - 4), 80 - h + 3 + r.randrange(h - 6)
            t.rect(wx, wy, wx + 2, wy + 3, r.choice(((120, 114, 98), (60, 62, 76), (60, 62, 76))))
        x += w
    tex("lointain", t.img)
    t = Toile(30, BAS)
    t.rect(9, 0, 23, BAS, (12, 12, 18))
    t.rect(4, 220, 28, BAS, (12, 12, 18))
    tex("devant_poteau", t.img)
    t = Toile(180, 70)
    for _ in range(36):
        x, y = r.randrange(0, 136), r.randrange(0, 40)
        t.ellipse(x, y, x + r.randrange(20, 44), y + r.randrange(12, 26), (12, 14, 18))
    tex("devant_feuilles", t.img)
    for nom in ("labo", "epicerie", "garage", "bureaux", "hall_tour", "metro", "namek", "etage_anges", "bureau_baba", "boucherie"):
        tex("int_" + nom, getattr(I, nom)())
    tex("ange", Image.open(RACINE / "designs" / "outputs" / "ange_sprite.png").convert("RGBA"))
    tex("int_poulet", I.maitre_poulet())
    tex("int_appart", I.appartement())
    tex("comptoir", I.comptoir())
    tex("caisse", I.caisse())


# ---------------------------------------------------------------- morceaux d'avenue
COMPTEUR = [0]
NOMS = ["TABAC", "BOULANGERIE", "COIFFURE", "PRESSE", "BAZAR", "FLEURS", "OPTIQUE", "BOUCHERIE", "LIBRAIRIE",
        "CORDONNERIE", "PRIMEUR", "MEUBLES", "AGENCE", "HOTEL", "RESTAURANT", "KEBAB", "SERRURIER", "CYCLES",
        "RETOUCHES", "TRAITEUR", "AUTO-ECOLE", "SUPERETTE", "FROMAGER", "JOUETS", "MERCERIE", "QUINCAILLERIE"]
NUMERO = [1]


def facade(style, travees, etages, boutiques):
    """Pose un immeuble a la suite. Renvoie le x de son bord gauche."""
    COMPTEUR[0] += 1
    nom = tex(f"facade_{COMPTEUR[0]:02d}", F.facade(style, travees, etages, boutiques, 100 + COMPTEUR[0], ACTE))
    x = C.x
    pose(nom, x, SOL, FOND)
    C.x += TEX[nom].width
    return x


def quelconque(rachat):
    """Un immeuble sans histoire, avec ses commerces fermes pour la nuit. rachat : a partir de quel acte la Krousty l'avale."""
    style, etages = rng.choice((("blanc", 6), ("blanc", 5), ("pierre", 6), ("brique", 6), ("barre", 9), ("maison", 2), ("barre", 8)))
    travees = rng.choice((6, 7, 8, 9)) if style != "maison" else rng.choice((5, 6))
    reste, boutiques, porte_faite = travees, [], False
    while reste > 0:
        if not porte_faite and (reste <= travees // 2 or reste == 1):
            NUMERO[0] += 2
            boutiques.append(B("porte", 1, str(NUMERO[0]), hall=rng.random() < 0.3))
            porte_faite, reste = True, reste - 1
            continue
        n = min(reste, rng.choice((2, 2, 3)))
        if n == 1:
            boutiques.append(B("mur", 1))
        elif rng.random() < 0.12:
            boutiques.append(B("vitrine", n, rng.choice(("SNACK", "BAR", "TABAC", "EPICERIE DE NUIT")), contenu="bar", rachat=rachat))
        else:
            boutiques.append(B("rideau", n, rng.choice(NOMS), rachat=rachat + rng.choice((0, 0, 1, 2))))
        reste -= n
    return facade(style, travees, etages, boutiques)


def mur(nom, lum=1.0):
    """Pose un mur deja dessine (pignon, garage, labo) a la suite."""
    x = C.x
    pose(nom, x, SOL, FOND, lum=lum)
    C.x += TEX[nom].width
    return x


def trouee(largeur, sol="sol_rue", muret=False):
    """Une ouverture entre deux immeubles : on y voit le lointain, donc la tour."""
    x = C.x
    for k in range(0, largeur, 64):
        image(sol, FOND, x + k, SOL - 24)
        if muret:
            image("muret", FOND, x + k, SOL - 24 - 64)
    C.x += largeur
    C.trouees.append((x, x + largeur))
    return x


def drapeau(nom, x):
    pose("drapeau_" + nom, x, SOL - 58, FOND)


def dans(plages, x, marge=0):
    return any(a - marge < x < b + marge for a, b in plages)


def finir_exterieur(tour_loin, tour_x, fin_marche=None):
    """Le sol, le lointain, le premier plan et tout le mobilier d'une carte d'avenue."""
    fin = C.x
    fin_marche = fin_marche or fin
    for x in range(-64, fin + 64, 64):
        image("sol", FOND, x, SOL)
    for x in range(40, fin, 150):
        image("tiret", FOND, x, ROUTE + 28)
    image(tour_loin, LOIN, tour_x, SOL - 8 - TEX[tour_loin].height, parallaxe=0.03)
    image("halo_orange", LOIN, tour_x + TEX[tour_loin].width // 2 - 75, SOL - TEX[tour_loin].height - 60, parallaxe=0.03,
          drapeaux=ADDITIF, lum=0.3)
    for x in range(0, fin // 2 + 500, 256):
        image("lointain", LOIN, x, SOL - 24 - 80 + 14, parallaxe=0.5, lum=0.8)
    for k in range(1, fin * 3 // 2 // 1500 + 1):
        image("devant_poteau", DEVANT, k * 1500, 0, parallaxe=1.5)
    for x in range(150, fin_marche - 100, 300):
        pose("lampadaire", x, BORDURE + 1)
        solide(x + 2, BORDURE - 5, x + 10, BORDURE + 3)
        lueur("halo_chaud", x + 36, BORDURE - 123, 0.9)
        lueur("flaque", x + 36, BORDURE + 14, 0.4)
    for x in range(310, fin_marche - 160, 300):
        if not dans(C.trouees, x + 45, 30) and not dans(C.degages, x + 45):
            pose(f"platane_{(x // 300) % len(O.VARIANTES_ARBRES)}", x, BORDURE - 1, lum=0.85)
            solide(x + 40, BORDURE - 6, x + 50, BORDURE + 2)
    for a, b in C.trouees:
        for x in range(a + 6, b - 4, 26):
            pose("potelet", x, SOL + 4)
        image("passage", FOND, (a + b) // 2 - 23, ROUTE + 2)
    x = 60
    while x < fin_marche - 300:
        x += rng.randrange(170, 460)
        if not dans(C.trouees, x + 60, 70) and not dans(C.degages, x + 60, 60):
            pose(f"auto_{rng.randrange(3)}", x, 250, drapeaux=MIROIR if rng.random() < 0.3 else 0)
            solide(x + 2, 240, x + 118, 253)
    for _ in range(fin // 500):
        pose(rng.choice(("dechets", "poubelle", "poubelle")), rng.randrange(100, fin_marche - 100), SOL + 4)
    # un velo attache a un lampadaire sur trois, une boite aux lettres, des buissons au fond des trouees
    for x in range(450, fin_marche - 100, 900):
        if not dans(C.trouees, x, 40) and not dans(C.degages, x):
            pose("velo", x - 36, BORDURE - 1, drapeaux=MIROIR if (x // 900) % 2 else 0)
    x = fin_marche // 3
    while dans(C.trouees, x, 30) or dans(C.degages, x, 30):
        x += 60
    pose("boite_lettres", x, SOL + 3)
    for a, b in C.trouees:
        if b - a <= 170:
            for bx in range(a + 4, b - 34, 38):
                pose("buisson", bx, SOL + 1, FOND, lum=0.9)
    C.lignes.append(f"limites 12 {fin_marche - 12} {SOL + 5} {BAS - 3}")
    C.fin = fin_marche


# ---------------------------------------------------------------- les trois cartes de l'avenue
def avenue_bas():
    """Le bas de l'avenue : le Maitre Poulet, chez toi, la fourche. C'est ici que tout commence."""
    global C
    C = Carte("avenue_bas")
    quelconque(7)
    x = facade("blanc", 8, 6, [B("porte", 1, "9"), B("rideau", 2, "TELEPHONIE"), B("poulet", 3), B("rideau", 2, "COIFFURE", rachat=7)])
    C.reperes["poulet"] = x + 126 + 113
    C.degages.append((x + 100, x + 330))
    image("enseigne_coq", FOND, x + 174, SOL - 56 - 34)
    lueur("halo_orange", x + 189, SOL - 30, 0.45)
    for sx in (x + 140, x + 162, x + 184):
        pose("scooter", sx, BORDURE - 2)
    C.reperes["sdf"] = x + 33
    C.reperes["n9"] = x + 21
    C.reperes["telephonie"] = x + 84
    dehors("plante_haute", x + 108, SOL + 3)
    dehors("plante_haute", x + 254, SOL + 3, miroir=True)
    pose("pub_poulet", x + 306, BORDURE)
    zone("MAITRE POULET", x + 126, x + 252)
    porte(x + 226, x + 252, "maitre_poulet", 46, 232, "E : ENTRER AU MAITRE POULET")
    quelconque(7)
    x = facade("brique", 7, 6, [B("vitrine", 3, "BRASSERIE", store=True, contenu="bar"), B("porte", 1, "12"),
                                B("rideau", 2, "PRESSE"), B("mur", 1)])
    C.reperes["appart"] = x + 147
    C.reperes["brasserie"] = x + 60
    C.reperes["presse"] = x + 210
    image("tele", FOND, x + 3 * 42 + 14, SOL - 356 + 266, drapeaux=SCINTILLE)
    lueur("halo_froid", x + 147, SOL - 76, 0.25)
    for sx in (x + 10, x + 66):
        pose("terrasse", sx, SOL + 8)
    for sx in (x - 4, x + 110):
        dehors("jardiniere", sx, SOL + 9)
    meuble("applique", x + 124, SOL - 56, "rue", FOND, 1, allume=True)
    lueur("halo_chaud", x + 130, SOL - 54, 0.35)
    zone("CHEZ TOI", x + 126, x + 168)
    porte(x + 132, x + 162, "appartement", 50, 236, "E : MONTER CHEZ TOI")
    x = trouee(170)
    pose("pin", x - 10, SOL + 3, lum=0.9)
    pose("pin", x + 70, SOL + 6, lum=0.85)
    pose("banc", x + 60, SOL + 12)
    solide(x + 60, SOL + 6, x + 92, SOL + 14)
    pose("sens_interdit", x + 150, BORDURE)
    zone("LE BANC", x, x + 170)
    C.reperes["banc"] = x + 76
    C.reperes["scooter"] = x + 112
    x = mur("pignon_affiche")
    lueur("halo_orange", x + 75, SOL - 258, 0.25)
    zone("GET A JOB", x, x + 150)
    C.reperes["affiche"] = x + 75
    for _ in range(3):
        quelconque(6)
    x = trouee(120)
    pose("direction", x + 40, BORDURE)
    for _ in range(4):
        quelconque(6)
    pose("pub_job", C.x - 200, BORDURE)
    finir_exterieur("tour_loin_1", 380)
    C.lignes.append(f"depart {C.reperes['poulet'] - 260} {SOL + 30}")


def avenue_milieu():
    """Le milieu : notre rue (Sisyphe, le labo, la laverie, l'epicerie), le garage d'Omar, le cafe, le parking."""
    global C
    C = Carte("avenue_milieu")
    for _ in range(2):
        quelconque(5)
    trouee(120)
    x = mur("pignon_sisyphe")
    zone("LE MUR DE SISYPHE", x, x + 150)
    C.reperes["sisyphe"] = x + 75
    x = mur("labo")
    lueur("halo_vert", x + 73, SOL - 14, 0.5)
    lueur("flaque_verte", x + 73, SOL + 9, 0.6)
    zone("LE LABO DE GERO", x, x + 170)
    C.reperes["labo"] = x + 139
    porte(x + 124, x + 154, "labo", 50, 240, "E : ENTRER DANS LE LABO")
    x = facade("pierre", 7, 6, [B("vitrine", 3, "LAVERIE", enseigne="froid", teinte=(206, 214, 222), contenu="machines"),
                                B("porte", 1, "21"), B("vitrine", 3, "ALIMENTATION GENERALE", store=True, porte="d")])
    drapeau("laverie", x + 2)
    zone("LA LAVERIE", x, x + 126)
    C.reperes["laverie"] = x + 63
    drapeau("epicerie", x + 283)
    pose("cagettes", x + 174, SOL + 6)
    pose("cagettes", x + 220, SOL + 6, drapeaux=MIROIR)
    dehors("fleurs", x + 262, SOL + 5)
    dehors("plante_large", x + 150, SOL + 5)
    zone("NOTRE EPICERIE", x + 168, x + 294)
    C.reperes["epicerie"] = x + 282
    porte(x + 270, x + 294, "epicerie", 50, 240, "E : ENTRER DANS L'EPICERIE")
    for _ in range(3):
        quelconque(5)
    x = mur("garage_pignon")
    mur("garage_avenue")
    C.degages.append((x, x + 260))
    lueur("halo_froid", x + 115, SOL - 40, 0.3)
    lueur("flaque", x + 115, SOL + 10, 0.3)
    zone("GARAGE OMAR", x, x + 544)
    C.reperes["garage"] = x + 115
    porte(x + 40, x + 190, "garage", 50, 240, "E : ENTRER DANS LE GARAGE")
    x = facade("barre", 9, 9, [B("rideau", 2, "PHARMACIE", enseigne="sombre", tags=0),
                               B("vitrine", 3, "CAFE DE L'AVENUE", store=True, contenu="bar"),
                               B("porte", 1, "29", hall=True), B("rideau", 2, "PRESSING", rachat=3), B("mur", 1)])
    drapeau("cafe", x + 201)
    for sx in (x + 96, x + 150):
        pose("terrasse", sx, SOL + 8)
    for sx in (x + 84, x + 196):
        dehors("jardiniere", sx, SOL + 9)
    pose("abri_vitre", x + 300, BORDURE - 2)
    pose("pub_krousty" if ACTE >= 3 else "pub_job", x + 362, BORDURE)
    if ACTE >= 3:
        lueur("halo_orange", x + 294, SOL - 46, 0.3)
    zone("LE CAFE", x + 84, x + 210)
    C.reperes["cafe"] = x + 140
    C.reperes["hall29"] = x + 231
    C.reperes["abri"] = x + 330
    quelconque(4)
    x = trouee(448, "sol_cour", muret=True)
    for gx in range(x, x + 448, 32):
        if not x + 190 < gx < x + 256:
            pose("grillage", gx, SOL + 2, FOND)
            pose("grillage", gx, SOL - 30, FOND)
    pose("camionnette", x + 300, SOL - 6, FOND, lum=0.8)
    pose("barriere", x + 200, SOL + 5)
    solide(x + 200, SOL - 2, x + 244, SOL + 6)
    for bx in range(x + 8, x + 180, 40):
        pose("buisson", bx, SOL + 2, FOND, lum=0.8)
    pose("parking", x + 4, BORDURE)
    zone("LE PARKING", x, x + 448)
    C.reperes["parking"] = x + 222
    x = facade("brique", 8, 6, [B("vitrine", 3, "SALLE DE SPORT", enseigne="clair", teinte=BUREAU, contenu="salle"),
                                B("porte", 1, "37"), B("vitrine", 2, "SNACK", contenu="bar", rachat=5), B("rideau", 2, "BAZAR", rachat=4)])
    drapeau("muscu", x + 2)
    zone("LA SALLE", x, x + 126)
    C.reperes["salle"] = x + 63
    for _ in range(3):
        quelconque(4)
    finir_exterieur("tour_loin_2", 340)


def avenue_haut():
    """Le haut : chez les Fatou, le metro, les bureaux de la Krousty, et la tour au bout."""
    global C
    C = Carte("avenue_haut")
    quelconque(3)
    x = facade("barre", 9, 9, [B("porte", 2, "RESIDENCE", hall=True), B("rideau", 2, "BOUCHERIE", rachat=4),
                               B("banque", 2, "BANQUE"), B("rideau", 2, "OPTIQUE", rachat=3), B("mur", 1)])
    lueur("halo_chaud", x + 42, SOL - 28, 0.3)
    dehors("plante_fine", x + 2, SOL + 3)
    dehors("plante_fine", x + 68, SOL + 3, miroir=True)
    zone("CHEZ LES FATOU", x, x + 84)
    C.reperes["fatou"] = x + 42
    pose("metro_totem", x + 200, BORDURE)
    pose("metro_garde", x + 220, SOL + 12)
    solide(x + 220, SOL + 2, x + 280, SOL + 14)
    lueur("halo_chaud", x + 215, BORDURE - 68, 0.5)
    zone("METRO KREMLIN-BICETRE", x + 190, x + 300)
    C.reperes["metro"] = x + 250
    porte(x + 212, x + 288, "metro", 40, 240, "E : DESCENDRE DANS LE METRO", SOL + 34)
    for _ in range(2):
        quelconque(3)
    trouee(120)
    for _ in range(2):
        quelconque(2)
    x = facade("verre", 18, 7, [B("vitrine", n, "KROUSTY CORP", enseigne="orange", teinte=BUREAU, contenu="vide")
                                for n in (4, 3, 4, 3, 4)])
    drapeau("krousty", x + 2)
    pose("pub_krousty", x + 120, BORDURE)
    for sx in range(x + 100, x + 756, 220):
        lueur("halo_orange", sx, SOL - 90, 0.18)
    zone("BUREAUX KROUSTY", x, x + 756)
    for sx in (x + 22, x + 92):
        dehors("plante_haute", sx, SOL + 3)
    C.reperes["bureaux"] = x + 64
    porte(x + 40, x + 90, "bureaux", 50, 240, "E : ENTRER DANS LES BUREAUX")
    for _ in range(3):
        quelconque(1)
    parvis = trouee(160)
    x = mur("tour_base")
    for k in range(parvis, C.x + 64, 64):
        image("sol_parvis", FOND, k, SOL)
    lueur("halo_froid", x + 225, SOL - 40, 0.35)
    lueur("halo_orange", x + 225, SOL - 120, 0.3)
    zone("TOUR KROUSTY CORP", x, C.x)
    C.reperes["tour"] = x + 200
    porte(x + 130, x + 230, "hall_tour", 50, 240, "E : ENTRER DANS LA TOUR")
    for sx in range(parvis + 12, parvis + 150, 48):
        dehors("jardiniere", sx, SOL + 7)
    for sx in (x + 96, x + 240):
        dehors("plante_haute", sx, SOL + 4)
    C.degages.append((parvis - 200, C.x))
    finir_exterieur("tour_loin_3", 300, fin_marche=x + 240)


# ---------------------------------------------------------------- les interieurs
def panneau_missions(titre, lignes):
    largeur = max(largeur_texte(l, 3) for l in [titre] + lignes) + 10
    t = Toile(largeur, 12 + len(lignes) * 8)
    t.rect(0, 0, t.l, t.h, (96, 96, 108))
    t.rect(1, 1, t.l - 1, t.h - 1, ENCRE)
    t.texte(5, 3, titre, ORANGE, 3)
    for k, ligne in enumerate(lignes):
        t.texte(5, 12 + k * 8, ligne, (190, 190, 196), 3)
    return t.img


def maitre_poulet():
    """L'interieur du Maitre Poulet : salle, comptoir, cuisine, arriere-boutique. Les missions de l'acte 1 partent d'ici."""
    global C
    C = Carte("maitre_poulet", exterieur=False)
    image("int_poulet", FOND, 0, 0)
    pose("comptoir", 302, 226)
    pose("caisse", 396, 186)
    solide(296, 180, 436, 228)
    for cx, force in ((150, 0.22), (366, 0.25), (518, 0.2)):
        lueur("halo_salle", cx, 150, force)
    lueur("halo_vert", 645, 150, 0.45)
    for cx in (80, 150, 220):
        pendu("suspension", cx, "chaud")
    meuble("plante_haute", 254, 187, "chaud")
    for k, sx in enumerate((56, 180)):
        ensemble(f"table_{k}", sx, 258, "chaud", [("chaise", 0, 0, dict(miroir=True)), ("table", 22, 0), ("chaise", 86, 0)])
    ensemble("plan_de_travail", 462, 258, "froid", [("grande_table", 0, 0), ("micro_ondes", 6, 40), ("grille_pain", 44, 40),
                                                    ("balance", 64, 40)])
    zone("LA SALLE", 0, 296)
    zone("LE COMPTOIR D'OLD GETO", 296, 436)
    zone("LA CUISINE", 436, 606)
    zone("L'ARRIERE-BOUTIQUE", 606, 760)
    porte(0, 40, "avenue_bas", CARTES["avenue_bas"].reperes["poulet"], SOL + 12, "E : SORTIR SUR L'AVENUE", BAS)
    porte(706, 760, "avenue_bas", CARTES["avenue_bas"].reperes["poulet"] + 150, SOL + 12, "E : SORTIR PAR LA RUELLE", 236)
    C.reperes.update(comptoir=(366, 232), arriere=(668, 240), salle=(150, 244))
    C.lignes += ["limites 10 750 230 266", "depart 46 236"]


def appartement():
    """Chez toi : la piece de l'intro. La tele eclaire tout."""
    global C
    C = Carte("appartement", exterieur=False)
    image("int_appart", FOND, 0, 0)
    image("tele_ecran", FOND, 294, 110, drapeaux=SCINTILLE)
    lueur("halo_froid", 318, 130, 0.5)
    lueur("halo_froid", 240, 190, 0.25)
    meuble("lampadaire_salon", 92, 184, "sombre", allume=True, halo=0.3)
    meuble("plante_fine", 256, 185, "sombre")
    meuble("horloge", 302, 78, "sombre")
    meuble("radio", 424, 150, "sombre")
    ensemble("coin_bureau", 56, 254, "sombre", [("bureau_tiroirs", 0, 0), ("ecran", 6, 38), ("lampe_bureau", 40, 38, dict(miroir=True)),
                                                ("chaise", 60, 0)])
    ensemble("lit", 352, 256, "sombre", [("lit", 0, 0)])
    zone("CHEZ TOI", 0, 480)
    porte(0, 44, "avenue_bas", CARTES["avenue_bas"].reperes["appart"], SOL + 12, "E : DESCENDRE DANS L'AVENUE", BAS)
    C.reperes.update(livre=(198, 208), tele=(318, 208), lit=(404, 244), plan=(417, 206), canape=(186, 206))
    C.lignes += ["limites 10 470 204 266", "depart 50 236"]


def interieur(nom, zones, sortie, titre, missions, halos=(), nuit=None):
    """Une carte d'interieur toute simple : un fond, des noms de lieux, une sortie a gauche.
    titre et missions rappellent a quoi sert le lieu dans l'histoire : le jeu les lit dans ressources/histoire."""
    global C
    C = Carte(nom, exterieur=False)
    C.nuit = nuit
    largeur = TEX["int_" + nom].width
    image("int_" + nom, FOND, 0, 0)
    for halo, cx, cy, force in halos:
        lueur(halo, cx, cy, force)
    for texte, x0, x1 in zones:
        zone(texte, x0, x1)
    if sortie:
        porte(0, 44, sortie[0], sortie[1], sortie[2], sortie[3], BAS)
    C.lignes += [f"limites 10 {largeur - 10} 204 266", "depart 50 240"]
    return largeur


def lieux_de_mission():
    milieu, haut = CARTES["avenue_milieu"].reperes, CARTES["avenue_haut"].reperes
    sol = SOL + 12
    interieur("labo", [("LE VESTIAIRE", 0, 214), ("LA PAILLASSE", 214, 604), ("LES CUVES", 604, 764),
                       ("LE POSTE DE CONTROLE", 764, 852), ("LE STOCK", 852, 960)],
              ("avenue_milieu", milieu["labo"], sol, "E : SORTIR DANS NOTRE RUE"),
              "LE LABO - ACTE 2", ["2.3 LE LABO : APPORTE LA PICCOLO A GERO"],
              [("halo_vert", 290, 130, 0.35), ("halo_vert", 450, 130, 0.35), ("halo_vert", 662, 130, 0.45),
               ("halo_vert", 566, 122, 0.2), ("halo_froid", 810, 134, 0.3), ("halo_chaud", 150, 150, 0.25)])
    # le vestiaire : le coin cafe sous les blouses, un fauteuil pour souffler
    meuble("meuble_bas", 120, 186, "froid")
    meuble("cafetiere", 126, 162, "froid")
    meuble("radio", 150, 162, "froid")
    meuble("plante_fine", 196, 187, "froid")
    ensemble("repos", 54, 258, "froid", [("fauteuil", 0, 0), ("table_basse", 60, 0), ("@dossiers", 70, 16)])
    # la paillasse : un tabouret, la balance, un chariot de fioles qu'on pousse d'un plan a l'autre
    meuble("tabouret_haut", 364, 190, "froid")
    meuble("balance", 334, 140, "froid")
    meuble("lampe_bureau", 440, 140, "froid")
    pose("chariot", 300, 250)
    solide(302, 242, 338, 251)
    pose("bidon", 606, 188, FOND)
    pose("bidon", 742, 190, FOND)
    # le poste de controle : la baie de serveurs et le reseau contre le mur, puis le poste de travail dans la piece
    for k in range(3):
        meuble("serveur", 770, 186 - k * 20, "froid")
    meuble("camera", 700, 64, "froid")
    ensemble("poste", 776, 252, "froid", [("bureau", 0, 0), ("ecran_poste", 6, 38, dict(ecran=BUREAU)),
                                          ("ecran", 34, 38, dict(ecran=(150, 220, 170))), ("@clavier", 12, 38),
                                          ("@souris", 38, 38), ("tour_pc", 62, 0), ("siege_bureau", 86, 0)])
    # le stock : la palette de cartons prete a partir
    pose("palette", 880, 254)
    solide(882, 246, 924, 255)
    CARTES["labo"].reperes.update(gero=(312, 206), cuves=(662, 206), poste=(806, 240), stock=(884, 206), casier=(82, 206),
                                  cafe=(140, 206))
    interieur("epicerie", [("NOTRE EPICERIE", 0, 460), ("LE COMPTOIR", 460, 600)],
              ("avenue_milieu", milieu["epicerie"], sol, "E : SORTIR SUR L'AVENUE"), "NOTRE EPICERIE - ACTES 2 ET 3",
              ["2.7 NOTRE EPICERIE : RECRUTE TES PETITS", "3.1 LA GUERILLA : DEFENDS LA BOUTIQUE, 3 ATTAQUES",
               "4.1 ON JOUE PAS LE MAINTIEN : LES MECANOS ARRIVENT"],
              [("halo_salle", 200, 150, 0.2), ("halo_froid", 380, 130, 0.25), ("halo_vert", 500, 160, 0.3)])
    meuble("plante_fine", 34, 187, "chaud")
    meuble("pendule", 438, 78, "chaud")
    meuble("telephone", 566, 131, "chaud")
    for k, sx in enumerate((104, 250)):
        ensemble(f"presentoir_{k}", sx, 242, "chaud", [("meuble_bas", 0, 0), ("balance" if k else "jardiniere", 4, 24),
                                                       ("jardiniere" if k else "petite_plante", 30, 24)])
    interieur("garage", [("L'ATELIER", 0, 270), ("LA RS6", 270, 520), ("LE PONT", 520, 680), ("LE BUREAU D'OMAR", 680, 760)],
              ("avenue_milieu", milieu["garage"], sol, "E : SORTIR DU GARAGE"), "GARAGE OMAR - ACTES 2 ET 4",
              ["2.1 LA RS6 : PRENDS LE FLIPPER ORANGE SUR L'ETABLI", "4.2 L'ATELIER : ENTRE PAR L'ARRIERE, BATS OMAR",
               "4.4 LES AVEUX D'OMAR : TOI AUSSI, TU LES AS VUS ?"],
              [("halo_froid", 390, 120, 0.3), ("halo_orange", 155, 128, 0.25)])
    meuble("radio", 66, 132, "froid")
    meuble("ventilateur", 104, 132, "froid")
    meuble("tabouret", 150, 191, "froid")
    meuble("pendule", 506, 82, "froid")
    ensemble("coin_omar", 618, 252, "froid", [("classeur", 0, 0), ("bureau_tiroirs", 30, 0), ("ecran", 38, 38),
                                               ("siege_bureau_2", 92, 0)])
    porte(300, 490, "namek", 60, 240, "E : PRENDRE LA RS6, CAP SUR NAMEK", 232)
    CARTES["garage"].reperes.update(etabli=(152, 206), rs6=(396, 208), bureau=(700, 208), omar=(620, 214), mecano=(170, 212),
                                    entree=(60, 240))
    CARTES["epicerie"].reperes.update(comptoir=(520, 206), rayons=(200, 206), frigos=(380, 206))
    interieur("namek", [("NAMEK", 0, 960)], ("garage", 400, 240, "E : RENTRER AU KREMLIN-BICETRE"), "NAMEK - ACTE 2",
              ["2.3 LA RECOLTE : TAPOTE VITE SUR LES PLANTS", "2.4 LA DOUANE : CACHE LA PICCOLO",
               "2.5 GO FAST : REMONTE AVANT LA FIN DU CHRONO"], nuit=(186, 140, 86))
    pose("rs6", 80, 228)
    solide(84, 218, 208, 229)
    CARTES["namek"].reperes.update(rs6=(150, 240), baraque=(812, 206),
                                   **{f"plant{k}": (x, y) for k, (x, y) in enumerate(((300, 214), (420, 246), (520, 212), (610, 250),
                                                                                    (700, 216), (860, 248)))})
    interieur("metro", [("LA RAME", 0, 960)], ("avenue_haut", haut["metro"], SOL + 24, "E : REMONTER DANS L'AVENUE"),
              "INTERMISSION - LE METRO", ["ARRETE LES FROTTEURS AVANT LA PROCHAINE STATION", "NE FRAPPE PAS UN INNOCENT",
                                          "PERSONNE NE TE DIRA MERCI"])
    CARTES["metro"].reperes.update(frotteur=(716, 214), femme=(746, 212), vitre=(480, 212), fond=(900, 230), entree=(120, 236),
                                   milieu=(470, 236), siege_femme=(560, 214), porte_rame=(430, 214), retour=(860, 236))
    interieur("boucherie", [("BOUCHERIE DE CRIMEE", 0, 600)], ("metro", 860, 236, "E : REPRENDRE LA LIGNE 7"),
              "CRIMEE", ["LA BOUCHERIE DE CRIMEE"], [("halo_froid", 360, 130, 0.3), ("halo_chaud", 150, 120, 0.2)])
    meuble("balance", 300, 112, "froid")
    pose("caisse", 430, 126, FOND)
    solide(256, 180, 474, 204)
    CARTES["boucherie"].reperes.update(mounir=(505, 210), caisses=(548, 242), entree=(60, 240))
    interieur("bureaux", [("L'OPEN SPACE", 0, 460), ("LA SALLE DE REUNION", 460, 670), ("L'ASCENSEUR", 670, 760)],
              ("avenue_haut", haut["bureaux"], sol, "E : SORTIR DES BUREAUX"), "BUREAUX KROUSTY - ACTE 5",
              ["5.1 LE BUREAU : FAIS MONTER LES GAINS", "5.3 LES ETAGES QUI BOUGENT : LE PREMIER ANGE", "5.4 LA FEMME : SUIS-LA POUR REVENIR"],
              [("halo_froid", 240, 120, 0.25), ("halo_froid", 566, 120, 0.25)])
    for k, sx in enumerate((60, 136, 212, 364)):
        meuble(("siege_bureau", "siege_rond", "siege_bureau_2", "siege_bureau")[k], sx + 16, 196, "froid")
    meuble("lampe_bureau", 98, 126, "froid", miroir=True)
    meuble("telephone", 138, 126, "froid")
    meuble("classeur", 438, 187, "froid")
    meuble("horloge", 434, 84, "froid")
    for sx in (500, 536, 604):
        meuble("chaise_tissu", sx, 187, "froid", miroir=sx > 560)
    meuble("plante_fine", 658, 187, "froid")
    meuble("camera", 716, 52, "froid")
    ensemble("poste_0", 120, 250, "froid", [("bureau", 0, 0), ("ecran_poste", 10, 38, dict(ecran=BUREAU)), ("tour_pc", 60, 0),
                                            ("siege_bureau", 84, 0)])
    ensemble("poste_1", 300, 250, "froid", [("siege_rond", 0, 0), ("bureau_tiroirs", 34, 0), ("imprimante", 44, 38),
                                            ("classeur", 94, 0)])
    interieur("hall_tour", [("LE HALL", 0, 520), ("L'ASCENSEUR", 520, 640)], ("avenue_haut", haut["tour"], sol, "E : SORTIR DE LA TOUR"),
              "LA TOUR - ACTE 7", ["7.1 BIENVENUE SUR TITAN : TRAVERSE LE HALL", "7.2 L'ENTRETIEN : L'ETAGE SANS NUMERO", "7.3 BABA : TOUT EN HAUT"],
              [("halo_orange", 240, 86, 0.3), ("halo_froid", 440, 130, 0.25)])
    meuble("plante_haute", 116, 188, "froid")
    meuble("plante_haute", 324, 188, "froid", miroir=True)
    meuble("ecran_poste", 176, 132, "froid", ecran=BUREAU)
    meuble("telephone", 284, 132, "froid")
    meuble("camera", 484, 60, "froid")
    ensemble("attente", 40, 252, "froid", [("canape", 0, 0), ("table_basse", 122, 0), ("petite_plante", 138, 16), ("fauteuil", 192, 0)])
    porte(520, 640, "etage_anges", 40, 240, "E : ASCENSEUR, DERNIER BOUTON", BAS)
    interieur("etage_anges", [("L'ETAGE SANS NUMERO", 0, 960)], ("hall_tour", 560, 240, "E : REDESCENDRE"), "L'ENTRETIEN - ACTE 7",
              ["QUI EST TON PATRON ?", "QUEL EST TON CODE ?", "QUI T'A FAIT MONTER ?"])
    for x in (150, 330, 560, 720):
        pose("ange", x, 214 + (x // 10) % 3 * 6)
    porte(880, 960, "bureau_baba", 50, 240, "E : L'ESCALIER DE SERVICE", BAS)
    interieur("bureau_baba", [("LE BUREAU DE BABA", 0, 600)], ("etage_anges", 860, 240, "E : REDESCENDRE"), "BABA - ACTE 7",
              ["IL T'ATTEND DEPUIS VINGT-CINQ ANS", "TU VOULAIS UN JOB ? VOILA LE MIEN."])
    meuble("plante_haute", 38, 187, "sombre")
    meuble("lampadaire_salon", 436, 187, "sombre", allume=True, halo=0.4)
    meuble("buffet", 476, 187, "sombre")
    meuble("vieux_telephone", 488, 163, "sombre")
    meuble("lampe", 540, 163, "sombre", allume=True, halo=0.3)
    for k, sx in enumerate((262, 346)):
        ensemble(f"fauteuil_{k}", sx, 248, "sombre", [("fauteuil", 0, 0)])


def construire():
    textures()
    avenue_bas()
    avenue_milieu()
    avenue_haut()
    bas, milieu, haut = CARTES["avenue_bas"], CARTES["avenue_milieu"], CARTES["avenue_haut"]
    for carte, suivante in ((bas, milieu), (milieu, haut)):
        # Les bouts de l'avenue : des passages assez larges pour qu'on ne puisse pas les enjamber en courant.
        carte.lignes.append(f"porte {carte.fin - 32} {carte.fin} {BAS} {suivante.nom} 46 {SOL + 30} 1 -")
        suivante.lignes.append(f"porte 0 32 {BAS} {carte.nom} {carte.fin - 50} {SOL + 30} 1 -")
        suivante.lignes.append(f"depart 46 {SOL + 30}")
    maitre_poulet()
    appartement()
    lieux_de_mission()
    for carte in CARTES.values():
        for nom, r in sorted(carte.reperes.items()):
            x, y = r if isinstance(r, tuple) else (r, SOL + 12)
            carte.lignes.append(f"repere {nom} {x:.0f} {y:.0f}")
    for dossier in (SORTIE, INTERFACE):
        if dossier.exists():
            shutil.rmtree(dossier)
        dossier.mkdir(parents=True)
    for police in (5, 3):
        planche, lettres = planche_police(police)
        planche.save(INTERFACE / f"police{police}.png")
        (INTERFACE / f"police{police}.txt").write_text(lettres, encoding="utf-8")
    for nom in sorted(TEX):
        if nom in HISTOIRE or nom in ("geto", "sdf", "flipper", "ange"):
            TEX[nom].save(INTERFACE / f"{nom}.png")                    # les gens gardent leurs couleurs : c'est la rue qui s'eteint
    for nom in sorted(set().union(*(c.utilisees for c in CARTES.values())) | {"ciel", "joueur", "aide"}):
        PAL.appliquer(TEX[nom], nom).save(SORTIE / f"{nom}.png")
    for carte in CARTES.values():
        noms = sorted(carte.utilisees | {"joueur", "aide"} | ({"ciel"} if carte.exterieur else set()))
        entete = [f"# GET A JOB, carte {carte.nom}, acte {ACTE}. Fichier genere par designs/decor/avenue.py : ne pas modifier a la main."]
        entete += [f"texture {n} {n}.png" for n in noms]
        nuit = getattr(carte, "nuit", None) or NUIT
        entete += ["joueur joueur", "aide aide", f"nuit {nuit[0]} {nuit[1]} {nuit[2]}"] + (["ciel ciel"] if carte.exterieur else [])
        (SORTIE / f"{carte.nom}.niveau").write_text("\n".join(entete + carte.lignes) + "\n")
        print(f"{carte.nom} : {sum(l.startswith('image') for l in carte.lignes)} images")
    PAL.java(RACINE / "jeu" / "src" / "decor" / "Palette.java")
    print("reperes", bas.reperes)


if __name__ == "__main__":
    construire()
