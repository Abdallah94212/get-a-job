"""Old Geto, gerant du Maitre Poulet : calvitie, toque, cicatrice droite, corpulent, tablier sans bretelles."""
from PIL import Image, ImageDraw

W, H = 72, 90
HAT_ROOM = 14  # la toque ajoute de la hauteur au-dessus du corps
CX = W // 2
ZOOM = 8

OUTLINE = (20, 20, 26, 255)
WHITE = (244, 244, 238, 255)
PALE = (218, 218, 214, 255)
APRON_SHADE = (178, 178, 180, 255)
SKIN = (186, 182, 176, 255)
SKIN_LIGHT = (214, 210, 204, 255)
SKIN_SHADE = (136, 132, 128, 255)
SCAR = (12, 12, 16, 255)
SHIRT = (112, 112, 120, 255)
SHIRT_SHADE = (84, 84, 92, 255)
TROUSERS = (62, 62, 70, 255)
TROUSERS_SHADE = (44, 44, 52, 255)
SHOE = (30, 30, 36, 255)
STAGE = (150, 150, 154, 255)
MID_STAIN = (150, 150, 154, 255)
HAIR = (112, 112, 118, 255)
ORANGE = (255, 122, 18, 255)
ORANGE_D = (196, 80, 10, 255)

# Notre coq, dessine pour le jeu
ROOSTER = (
    "..O......",
    ".OOO..O.O",
    "OOOO.OOOO",
    ".OOOOOOOO",
    "..OOOOOO.",
    "...OOOO..",
    "...O..O..",
)
LETTERS = {
    "A": (".O.", "OOO", "O.O"), "E": ("OOO", "OO.", "OOO"), "I": ("OOO", ".O.", "OOO"),
    "L": ("O..", "O..", "OOO"), "M": ("O.O", "OOO", "O.O"), "O": ("OOO", "O.O", "OOO"),
    "P": ("OOO", "OOO", "O.."), "R": ("OO.", "OO.", "O.O"), "T": ("OOO", ".O.", ".O."),
    "U": ("O.O", "O.O", "OOO"),
}


def new_layer():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def outlined(layer):
    src = layer.load()
    out = layer.copy()
    dst = out.load()
    for y in range(H):
        for x in range(W):
            if src[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < W and 0 <= ny < H and src[nx, ny][3]:
                    dst[x, y] = OUTLINE
                    break
    return out


def stamp(layer, rows, x, y):
    px = layer.load()
    for j, row in enumerate(rows):
        for i, c in enumerate(row):
            if c == "O":
                px[x + i, y + j] = ORANGE
            elif c == "D":
                px[x + i, y + j] = ORANGE_D


def pixel_text(layer, text, x, y):
    for letter in text:
        stamp(layer, LETTERS[letter], x, y)
        x += 4


def hat():
    """Toque posee en arriere de la tete : le crane degarni reste visible devant."""
    layer = new_layer()
    d = ImageDraw.Draw(layer)
    d.rectangle([CX - 8, 9, CX + 8, 20], fill=WHITE)
    d.ellipse([CX - 13, 1, CX - 2, 12], fill=WHITE)
    d.ellipse([CX - 5, 0, CX + 5, 10], fill=WHITE)
    d.ellipse([CX + 2, 1, CX + 13, 12], fill=WHITE)
    layer = outlined(layer)
    d = ImageDraw.Draw(layer)
    d.line([(CX - 8, 13), (CX + 8, 13)], fill=APRON_SHADE)
    d.line([(CX - 4, 4), (CX - 3, 11)], fill=PALE)
    d.line([(CX + 4, 4), (CX + 3, 11)], fill=PALE)
    d.arc([CX + 2, 1, CX + 13, 12], 10, 80, fill=PALE)
    return layer


def silhouette():
    layer = new_layer()
    d = ImageDraw.Draw(layer)
    # Jambes et chaussures
    d.polygon([(CX - 13, 54), (CX - 1, 54), (CX - 2, 70), (CX - 12, 70)], fill=TROUSERS)
    d.polygon([(CX + 1, 54), (CX + 13, 54), (CX + 12, 70), (CX + 2, 70)], fill=TROUSERS_SHADE)
    d.rectangle([CX - 14, 71, CX - 2, 73], fill=SHOE)
    d.rectangle([CX + 2, 71, CX + 14, 73], fill=SHOE)
    # Bras tres epais, ecartes par le ventre
    d.polygon([(CX - 18, 21), (CX - 12, 21), (CX - 20, 50), (CX - 29, 50), (CX - 27, 30)], fill=SKIN)
    d.polygon([(CX + 12, 21), (CX + 18, 21), (CX + 27, 30), (CX + 29, 50), (CX + 20, 50)], fill=SKIN_SHADE)
    # Mains
    d.rectangle([CX - 29, 51, CX - 21, 57], fill=SKIN)
    d.rectangle([CX + 21, 51, CX + 29, 57], fill=SKIN_SHADE)
    layer = outlined(layer)
    # Le torse et le gros ventre passent devant les bras
    torso = new_layer()
    d = ImageDraw.Draw(torso)
    d.polygon([(CX - 18, 21), (CX + 18, 21), (CX + 20, 36), (CX - 20, 36)], fill=SHIRT)
    d.ellipse([CX - 23, 27, CX + 23, 59], fill=SHIRT)
    # Tablier sous le ventre, plus etroit vers le bas
    d.polygon([(CX - 19, 49), (CX + 19, 49), (CX + 16, 65), (CX - 16, 65)], fill=WHITE)
    # Cou epais, double menton et tete chauve
    d.rectangle([CX - 7, 18, CX + 7, 24], fill=SKIN_SHADE)
    # Touffes de cheveux, irregulieres, juste au-dessus des oreilles
    for side in (-1, 1):
        d.polygon([(CX + side * 8, 5), (CX + side * 10, 7), (CX + side * 11, 6), (CX + side * 10, 9),
                   (CX + side * 11, 9), (CX + side * 10, 11), (CX + side * 8, 11)], fill=HAIR)
    d.ellipse([CX - 9, 2, CX + 9, 23], fill=SKIN)
    d.rectangle([CX - 10, 11, CX - 9, 15], fill=SKIN)
    d.rectangle([CX + 9, 11, CX + 10, 15], fill=SKIN_SHADE)
    layer.alpha_composite(outlined(torso))
    return layer


def details(layer):
    d = ImageDraw.Draw(layer)
    # Manches retroussees
    d.polygon([(CX - 18, 22), (CX - 14, 22), (CX - 20, 33), (CX - 26, 33)], fill=SHIRT_SHADE)
    d.polygon([(CX + 14, 22), (CX + 18, 22), (CX + 26, 33), (CX + 20, 33)], fill=SHIRT_SHADE)
    d.line([(CX - 27, 34), (CX - 21, 34)], fill=OUTLINE)
    d.line([(CX + 21, 34), (CX + 27, 34)], fill=OUTLINE)
    # Doigts
    for x in (CX - 27, CX - 25, CX - 23, CX + 23, CX + 25, CX + 27):
        d.line([(x, 54), (x, 57)], fill=OUTLINE)
    # Le ventre : lumiere devant, ombre en dessous, le t-shirt deborde sur le tablier
    d.arc([CX - 22, 28, CX + 22, 58], 15, 165, fill=SHIRT_SHADE)
    d.arc([CX - 21, 27, CX + 21, 57], 25, 155, fill=SHIRT_SHADE)
    d.chord([CX - 22, 44, CX + 22, 58], 0, 180, fill=OUTLINE)
    d.chord([CX - 21, 44, CX + 21, 57], 0, 180, fill=SHIRT_SHADE)
    # Le tablier repart sous le ventre, noue sur les cotes
    d.polygon([(CX - 19, 51), (CX + 19, 51), (CX + 16, 64), (CX - 16, 64)], fill=WHITE)
    d.arc([CX - 22, 44, CX + 22, 58], 15, 165, fill=OUTLINE)
    d.line([(CX - 21, 50), (CX - 18, 51)], fill=APRON_SHADE)
    d.line([(CX + 18, 51), (CX + 21, 50)], fill=APRON_SHADE)
    d.polygon([(CX + 11, 55), (CX + 16, 52), (CX + 15, 64), (CX + 11, 64)], fill=PALE)
    d.line([(CX - 8, 58), (CX - 9, 64)], fill=APRON_SHADE)
    d.line([(CX + 7, 58), (CX + 8, 64)], fill=APRON_SHADE)
    d.point([(CX - 2, 60), (CX - 1, 61), (CX + 4, 62)], fill=MID_STAIN)
    # L'enseigne du Maitre Poulet sur le t-shirt
    stamp(layer, ROOSTER, CX - 4, 32)
    pixel_text(layer, "MAITRE", CX - 11, 40)
    pixel_text(layer, "POULET", CX - 11, 44)
    # Reflet du crane et bas de joue
    d.ellipse([CX - 6, 4, CX - 2, 6], fill=SKIN_LIGHT)
    d.arc([CX - 9, 7, CX + 9, 23], 20, 80, fill=SKIN_SHADE)
    d.line([(CX - 6, 21), (CX + 6, 21)], fill=SKIN_SHADE)
    # Cicatrice bien droite en travers du front, avec ses points de suture
    d.line([(CX - 8, 9), (CX + 8, 9)], fill=SCAR)
    for x in range(CX - 7, CX + 8, 3):
        d.line([(x, 8), (x, 10)], fill=SCAR)
    # Calvitie : les cheveux restent sur les cotes
    # Yeux plisses et lourds
    d.line([(CX - 7, 14), (CX - 3, 14)], fill=OUTLINE)
    d.line([(CX + 3, 14), (CX + 7, 14)], fill=OUTLINE)
    d.line([(CX - 6, 13), (CX - 4, 13)], fill=SKIN_SHADE)
    d.line([(CX + 4, 13), (CX + 6, 13)], fill=SKIN_SHADE)
    # Nez et demi-sourire en coin
    d.line([(CX - 1, 15), (CX - 1, 17)], fill=SKIN_SHADE)
    d.line([(CX - 4, 19), (CX + 3, 19), (CX + 5, 17)], fill=OUTLINE)
    # Serviette blanche posee sur l'epaule
    towel = new_layer()
    td = ImageDraw.Draw(towel)
    td.polygon([(CX + 9, 19), (CX + 18, 21), (CX + 19, 37), (CX + 11, 36)], fill=WHITE)
    towel = outlined(towel)
    td = ImageDraw.Draw(towel)
    td.line([(CX + 14, 21), (CX + 15, 35)], fill=PALE)
    td.line([(CX + 12, 32), (CX + 18, 33)], fill=APRON_SHADE)
    layer.alpha_composite(towel)


def sprite():
    body = silhouette()
    details(body)
    layer = hat()
    layer.alpha_composite(body, (0, HAT_ROOM))
    return layer


def main():
    image = sprite()
    image.save("outputs/geto_sprite.png")
    preview = Image.new("RGBA", (W, H), STAGE)
    preview.alpha_composite(image)
    preview.resize((W * ZOOM, H * ZOOM), Image.NEAREST).save("outputs/geto_sprite_x8.png")

    # Test : ecran 256x192, l'allee de nuit, silhouette du heros a 48 px pour l'echelle
    screen = Image.new("RGBA", (256, 192), (62, 62, 66, 255))
    d = ImageDraw.Draw(screen)
    d.rectangle([0, 150, 256, 192], fill=(38, 38, 42, 255))
    d.rectangle([50, 108, 64, 150], fill=(24, 24, 28, 255))
    d.ellipse([51, 100, 63, 112], fill=(24, 24, 28, 255))
    screen.alpha_composite(image, (140, 150 - 88))
    screen.resize((256 * 3, 192 * 3), Image.NEAREST).save("outputs/geto_ecran.png")


if __name__ == "__main__":
    main()
