"""Outils communs du decor : couleurs de nuit, toile de dessin, police en pixels."""
import random
from PIL import Image, ImageDraw

GRAIN = True  # le grain des surfaces ; a couper si l'on ramene un jour le jeu a une palette fixe (decor/palette.py)
T = 2  # unites du monde par texel pour l'architecture (1 m = 28 unites)

# Le jeu est presque gris : ces couleurs sont deja eclairees comme en pleine nuit.
ENCRE = (20, 20, 26)
NUIT = (30, 30, 42)  # couleur du brouillard et de l'horizon
MUR_BLANC, MUR_BLANC_O, MUR_BLANC_C = (122, 124, 134), (100, 102, 112), (140, 142, 150)
PIERRE, PIERRE_O, PIERRE_C = (112, 110, 114), (90, 88, 94), (132, 130, 132)
BRIQUE, BRIQUE_J = (94, 80, 80), (76, 64, 66)
ARDOISE, ARDOISE_O, ZINC = (52, 54, 64), (42, 44, 54), (88, 92, 102)
BETON, BETON_O, BETON_C = (102, 102, 108), (80, 80, 88), (120, 120, 126)
TUILE = (64, 56, 56)
VITRE, VITRE_R = (24, 26, 38), (44, 48, 64)
VERRES = ((30, 34, 48), (34, 38, 54), (26, 30, 44), (38, 42, 58))
MENEAU = (58, 62, 74)
VOLET, VOLET_O = (48, 54, 56), (36, 40, 44)
FER = (30, 30, 36)
RIDEAU, RIDEAU_O = (78, 80, 88), (58, 60, 68)
ALLUME, ALLUME_F = (232, 222, 196), (190, 178, 152)
TELE, BUREAU = (150, 170, 220), (206, 214, 226)
ORANGE, ORANGE_S = (255, 122, 18), (196, 80, 10)
VERT = (70, 240, 110)
BLANC = (236, 236, 230)


def lum(c):
    """Couleur lumineuse : l'alpha 254 dit au jeu que ce pixel brille dans la nuit."""
    return (c[0], c[1], c[2], 254)


def ton(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


POLICE5 = {
    "A": ".###./#...#/#...#/#####/#...#/#...#/#...#", "B": "####./#...#/#...#/####./#...#/#...#/####.",
    "C": ".###./#...#/#..../#..../#..../#...#/.###.", "D": "####./#...#/#...#/#...#/#...#/#...#/####.",
    "E": "#####/#..../#..../####./#..../#..../#####", "F": "#####/#..../#..../####./#..../#..../#....",
    "G": ".###./#...#/#..../#.###/#...#/#...#/.###.", "H": "#...#/#...#/#...#/#####/#...#/#...#/#...#",
    "I": ".###./..#../..#../..#../..#../..#../.###.", "J": "..###/...#./...#./...#./...#./#..#./.##..",
    "K": "#...#/#..#./#.#../##.../#.#../#..#./#...#", "L": "#..../#..../#..../#..../#..../#..../#####",
    "M": "#...#/##.##/#.#.#/#.#.#/#...#/#...#/#...#", "N": "#...#/##..#/#.#.#/#..##/#...#/#...#/#...#",
    "O": ".###./#...#/#...#/#...#/#...#/#...#/.###.", "P": "####./#...#/#...#/####./#..../#..../#....",
    "Q": ".###./#...#/#...#/#...#/#.#.#/#..#./.##.#", "R": "####./#...#/#...#/####./#.#../#..#./#...#",
    "S": ".####/#..../#..../.###./....#/....#/####.", "T": "#####/..#../..#../..#../..#../..#../..#..",
    "U": "#...#/#...#/#...#/#...#/#...#/#...#/.###.", "V": "#...#/#...#/#...#/#...#/#...#/.#.#./..#..",
    "W": "#...#/#...#/#...#/#.#.#/#.#.#/##.##/#...#", "X": "#...#/#...#/.#.#./..#../.#.#./#...#/#...#",
    "Y": "#...#/#...#/.#.#./..#../..#../..#../..#..", "Z": "#####/....#/...#./..#../.#.../#..../#####",
    "0": ".###./#...#/#..##/#.#.#/##..#/#...#/.###.", "1": "..#../.##../..#../..#../..#../..#../.###.",
    "2": ".###./#...#/....#/...#./..#../.#.../#####", "3": "####./....#/....#/.###./....#/....#/####.",
    "4": "...#./..##./.#.#./#..#./#####/...#./...#.", "5": "#####/#..../####./....#/....#/#...#/.###.",
    "6": ".###./#..../#..../####./#...#/#...#/.###.", "7": "#####/....#/...#./..#../..#../..#../..#..",
    "8": ".###./#...#/#...#/.###./#...#/#...#/.###.", "9": ".###./#...#/#...#/.####/....#/....#/.###.",
    "-": "...../...../...../.###./...../...../.....", ".": "...../...../...../...../...../.##../.##..",
    "'": "..#../..#../.#.../...../...../...../.....", "/": "....#/....#/...#./..#../.#.../#..../#....",
    "!": "..#../..#../..#../..#../..#../...../..#..", "?": ".###./#...#/....#/...#./..#../...../..#..",
    ":": "...../..#../..#../...../..#../..#../.....", "&": ".##../#..#./#.#../.#.../#.#.#/#..#./.##.#",
    "+": "...../..#../..#../#####/..#../..#../.....", ",": "...../...../...../...../..#../..#../.#...",
    " ": "...../...../...../...../...../...../.....",
    '"': ".#.#./.#.#./...../...../...../...../.....", "(": "...#./..#../.#.../.#.../.#.../..#../...#.",
    ")": ".#.../..#../...#./...#./...#./..#../.#...", ";": "...../..#../..#../...../..#../..#../.#...",
    "%": "##..#/##.#./...#./..#../.#.../.#.##/#..##", "<": "...#./..#../.#.../#..../.#.../..#../...#.",
    ">": ".#.../..#../...#./....#/...#./..#../.#...", "*": "...../#.#.#/.###./#####/.###./#.#.#/.....",
    "=": "...../...../#####/...../#####/...../.....",
}
# Les lettres accentuees du francais : la lettre de base, et ce qu'on pose au-dessus (ou au-dessous pour la cedille).
ACCENTS = {"É": ("E", "aigu"), "È": ("E", "grave"), "Ê": ("E", "circ"), "Ë": ("E", "trema"), "À": ("A", "grave"),
           "Â": ("A", "circ"), "Î": ("I", "circ"), "Ï": ("I", "trema"), "Ô": ("O", "circ"), "Ù": ("U", "grave"),
           "Û": ("U", "circ"), "Ç": ("C", "cedille")}
SIGNES = {"aigu": ("...#.", "..#.."), "grave": (".#...", "..#.."), "circ": ("..#..", ".#.#."), "trema": (".....", ".#.#.")}
POLICE3 = {
    "A": ".#./#.#/###/#.#/#.#", "B": "##./#.#/##./#.#/##.", "C": ".##/#../#../#../.##", "D": "##./#.#/#.#/#.#/##.",
    "E": "###/#../##./#../###", "F": "###/#../##./#../#..", "G": ".##/#../#.#/#.#/.##", "H": "#.#/#.#/###/#.#/#.#",
    "I": "###/.#./.#./.#./###", "J": "..#/..#/..#/#.#/.#.", "K": "#.#/#.#/##./#.#/#.#", "L": "#../#../#../#../###",
    "M": "#.#/###/###/#.#/#.#", "N": "##./#.#/#.#/#.#/#.#", "O": "###/#.#/#.#/#.#/###", "P": "##./#.#/##./#../#..",
    "Q": "###/#.#/#.#/###/..#", "R": "##./#.#/##./#.#/#.#", "S": ".##/#../.#./..#/##.", "T": "###/.#./.#./.#./.#.",
    "U": "#.#/#.#/#.#/#.#/###", "V": "#.#/#.#/#.#/#.#/.#.", "W": "#.#/#.#/###/###/#.#", "X": "#.#/#.#/.#./#.#/#.#",
    "Y": "#.#/#.#/.#./.#./.#.", "Z": "###/..#/.#./#../###", "0": "###/#.#/#.#/#.#/###", "1": ".#./##./.#./.#./###",
    "2": "##./..#/.#./#../###", "3": "##./..#/.#./..#/##.", "4": "#.#/#.#/###/..#/..#", "5": "###/#../##./..#/##.",
    "6": ".##/#../###/#.#/###", "7": "###/..#/.#./.#./.#.", "8": "###/#.#/###/#.#/###", "9": "###/#.#/###/..#/##.",
    "-": ".../.../###/.../...", ".": ".../.../.../.../.#.", "'": ".#./.#./.../.../...", "/": "..#/..#/.#./#../#..",
    "!": ".#./.#./.#./.../.#.", ":": ".../.#./.../.#./...", "+": ".../.#./###/.#./...", ",": ".../.../.../.#./#..",
    "&": ".#./#../.#./#.#/.##", "?": "##./..#/.#./.../.#.", " ": ".../.../.../.../...",
}
POLICES = {5: POLICE5, 3: POLICE3}
SANS_ACCENT = str.maketrans("ÉÈÊËÀÂÎÏÔÙÛÇ", "EEEEAAIIOUUC")


def planche_police(police):
    """Toutes les lettres d'une police sur une ligne, pour que le jeu ecrive lui-meme ses textes.
    Renvoie l'image et la liste des caracteres, dans l'ordre. Police 5 : cases de 6 x 11, les deux rangees du haut
    pour les accents, les deux du bas pour la cedille. Police 3 : cases de 4 x 6, sans accents."""
    glyphes = POLICES[police]
    lettres = sorted(glyphes) + (sorted(ACCENTS) if police == 5 else [])
    haut = 2 if police == 5 else 0
    t = Toile(len(lettres) * (police + 1), 11 if police == 5 else 6)
    for n, lettre in enumerate(lettres):
        base, signe = ACCENTS.get(lettre, (lettre, None))
        rangees = glyphes[base].split("/")
        if signe == "cedille":
            rangees = rangees + ["..#..", ".#..."]
        elif signe:
            rangees = list(SIGNES[signe]) + rangees
            for j, rangee in enumerate(rangees):
                for i, pixel in enumerate(rangee):
                    if pixel == "#":
                        t.point(n * 6 + i, j, (255, 255, 255))
            continue
        for j, rangee in enumerate(rangees):
            for i, pixel in enumerate(rangee):
                if pixel == "#":
                    t.point(n * (police + 1) + i, j + haut, (255, 255, 255))
    return t.img, "".join(lettres)


def largeur_texte(texte, police=5, echelle=1):
    return max(0, len(texte) * (police + 1) - 1) * echelle


class Toile:
    """Une image qu'on dessine pixel par pixel, sans lissage."""

    def __init__(self, largeur, hauteur, fond=None):
        self.l, self.h = largeur, hauteur
        self.img = Image.new("RGBA", (largeur, hauteur), fond if fond else (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def rect(self, x0, y0, x1, y1, c):
        """Rectangle plein, bornes de droite et du bas exclues."""
        if x1 > x0 and y1 > y0:
            self.d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=c)

    def ligne(self, points, c, epaisseur=1):
        self.d.line(points, fill=c, width=epaisseur)

    def ellipse(self, x0, y0, x1, y1, c, contour=None):
        self.d.ellipse([x0, y0, x1 - 1, y1 - 1], fill=c, outline=contour)

    def poly(self, points, c, contour=None):
        self.d.polygon(points, fill=c, outline=contour)

    def point(self, x, y, c):
        if 0 <= x < self.l and 0 <= y < self.h:
            self.img.putpixel((x, y), c if len(c) == 4 else c + (255,))

    def texte(self, x, y, texte, c, police=5, echelle=1):
        """Ecrit en majuscules avec la police en pixels. Renvoie la largeur ecrite."""
        glyphes = POLICES[police]
        for lettre in texte.upper().translate(SANS_ACCENT):
            for j, rangee in enumerate(glyphes.get(lettre, glyphes[" "]).split("/")):
                for i, pixel in enumerate(rangee):
                    if pixel == "#":
                        self.rect(x + i * echelle, y + j * echelle, x + (i + 1) * echelle, y + (j + 1) * echelle, c)
            x += (police + 1) * echelle
        return largeur_texte(texte, police, echelle)

    def texte_centre(self, x, y, largeur, texte, c, police=5, echelle=1):
        self.texte(x + (largeur - largeur_texte(texte, police, echelle)) // 2, y, texte, c, police, echelle)

    def coller(self, image, x, y):
        """Colle une image en gardant ses pixels lumineux tels quels."""
        masque = image.split()[3].point(lambda a: 255 if a >= 128 else 0)
        self.img.paste(image, (x, y), masque)

    def grain(self, amplitude, graine):
        """Petites variations sur les surfaces mates, pour casser les aplats. Coupe avec la palette du jeu :
        ramene a 32 couleurs, ce bruit devenait des points parasites."""
        if not GRAIN:
            return
        rng = random.Random(graine)
        px = self.img.load()
        for y in range(self.h):
            for x in range(self.l):
                r, g, b, a = px[x, y]
                if a == 255 and rng.random() < 0.4:
                    d = rng.randint(-amplitude, amplitude)
                    px[x, y] = (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), 255)

    def assombrir_haut(self, minimum, y_bas):
        """La lumiere des lampadaires n'atteint pas le haut des immeubles."""
        px = self.img.load()
        for y in range(min(self.h, y_bas)):
            k = minimum + (1 - minimum) * y / max(1, y_bas)
            for x in range(self.l):
                r, g, b, a = px[x, y]
                if a == 255:
                    px[x, y] = (int(r * k), int(g * k), int(b * k), 255)


def tags(t, x, y, largeur, hauteur, rng, nombre):
    """Des gribouillis de bombe sur un mur ou un rideau de fer."""
    if largeur < 12 or hauteur < 8:
        return
    for _ in range(nombre):
        c = rng.choice(((150, 150, 160), (26, 26, 32), (196, 196, 202), (60, 62, 72)))
        px, py = rng.randrange(x, x + largeur - 8), rng.randrange(y + 2, y + hauteur - 2)
        points = [(px, py)]
        for _ in range(rng.randrange(4, 8)):
            px = max(x, min(x + largeur - 1, px + rng.randrange(-3, 9)))
            py = max(y, min(y + hauteur - 1, py + rng.randrange(-6, 7)))
            points.append((px, py))
        t.ligne(points, c)


def affichettes(t, x, y, largeur, hauteur, rng, nombre=3):
    """De petites affiches collees, certaines a moitie arrachees."""
    if largeur < 10 or hauteur < 12:
        return
    for _ in range(nombre):
        ax, ay = rng.randrange(x, x + largeur - 8), rng.randrange(y, y + hauteur - 10)
        c = rng.choice(((168, 164, 152), (140, 140, 146), (96, 96, 104)))
        t.rect(ax, ay, ax + 7, ay + 9, c)
        t.rect(ax + 1, ay + 2, ax + 6, ay + 3, ton(c, 0.5))
        t.rect(ax + 1, ay + 5, ax + 4, ay + 6, ton(c, 0.6))
        if rng.random() < 0.4:
            t.rect(ax + 4, ay + 6, ax + 7, ay + 9, ton(c, 0.45))


def garde_corps(t, x, y, largeur, hauteur):
    """Garde-corps en fer : une lisse, une traverse basse et des barreaux."""
    for xb in range(x, x + largeur, 2):
        t.ligne([(xb, y), (xb, y + hauteur - 1)], FER)
    t.ligne([(x, y), (x + largeur - 1, y)], FER)
    t.ligne([(x, y + hauteur - 1), (x + largeur - 1, y + hauteur - 1)], FER)
