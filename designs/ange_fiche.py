"""Fiche de personnage de l'ange, facon ecole d'art : un PDF A4 paysage de cinq pages."""
import colorsys
import os
import sys
from PIL import Image, ImageDraw, ImageFont
import ange_sprite as a
import ange_vues as v

PAGE_W, PAGE_H = 2480, 1754
MARGIN = 110
PAPER = (241, 237, 227)
INK = (32, 30, 28)
SOFT = (120, 114, 104)
ACCENT = (214, 92, 8)
PLATE = (62, 62, 66)
GUIDE = (150, 150, 156)
TOTAL = 5

SERIF = "/System/Library/Fonts/Supplemental/Georgia.ttf"
MONO = "/System/Library/Fonts/Supplemental/Courier New.ttf"
MONO_B = "/System/Library/Fonts/Supplemental/Courier New Bold.ttf"


def font(path, size):
    return ImageFont.truetype(path, size)


H1 = font(SERIF, 84)
H2 = font(SERIF, 44)
H3 = font(MONO_B, 30)
BODY = font(MONO, 30)
SMALL = font(MONO, 25)
SMALL_B = font(MONO_B, 25)


def new_page(number, title, subtitle, total=TOTAL, who="L'ange du groupe de contrôle"):
    page = Image.new("RGB", (PAGE_W, PAGE_H), PAPER)
    d = ImageDraw.Draw(page)
    d.text((MARGIN, 76), title, font=H1, fill=INK)
    d.text((MARGIN, 182), subtitle, font=BODY, fill=SOFT)
    d.line([(MARGIN, 238), (PAGE_W - MARGIN, 238)], fill=INK, width=3)
    d.text((MARGIN, PAGE_H - 74), f"GET A JOB / Fiche de personnage / {who}", font=SMALL, fill=SOFT)
    d.text((PAGE_W - MARGIN - 130, PAGE_H - 74), f"{number} / {total}", font=SMALL, fill=SOFT)
    return page, d


def wrap(d, text, typeface, width):
    lines, line = [], ""
    for word in text.split():
        trial = f"{line} {word}".strip()
        if d.textlength(trial, font=typeface) <= width:
            line = trial
        else:
            lines.append(line)
            line = word
    lines.append(line)
    return lines


def paragraph(d, x, y, text, width, typeface=BODY, fill=INK, leading=1.28):
    size = typeface.size
    for line in wrap(d, text, typeface, width):
        d.text((x, y), line, font=typeface, fill=fill)
        y += int(size * leading)
    return y


def plate(page, image, x, y, scale, fill=PLATE):
    """Colle un sprite sur un fond sombre, agrandi sans lissage."""
    w, h = image.size
    back = Image.new("RGBA", (w, h), fill + (255,))
    back.alpha_composite(image.convert("RGBA"))
    page.paste(back.convert("RGB").resize((w * scale, h * scale), Image.NEAREST), (x, y))
    return w * scale, h * scale


def recolored(image, palette):
    result = image.copy()
    px = result.load()
    for y in range(result.height):
        for x in range(result.width):
            if px[x, y][3]:
                px[x, y] = palette.get(px[x, y], px[x, y])
    return result


def eyes_in(view):
    """Retrouve le centre des yeux visibles : une pupille entre deux pixels d'iris."""
    px = view.load()
    return [(x, y) for y in range(a.H) for x in range(1, a.W - 1)
            if px[x, y] == a.OUTLINE and px[x - 1, y] == a.ORANGE and px[x + 1, y] == a.ORANGE]


def ult_view(view, palette, step=3):
    """Applique l'ult a n'importe quelle vue : effet, yeux cernes, larmes de sang."""
    centers = eyes_in(view)
    image = a.psychedelic(view, 2, palette)
    clean = view.load()
    dst = image.load()
    overlay = a.new_layer()
    for x, y in centers:
        a.eye(overlay, x, y)
    src = overlay.load()
    visible = {(x, y) for y in range(a.H) for x in range(a.W) if src[x, y][3] and src[x, y] == clean[x, y]}
    for x, y in visible:
        dst[x, y] = src[x, y]
    for x, y in visible:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) not in visible:
                dst[nx, ny] = a.OUTLINE
    a.tears_at(ImageDraw.Draw(image), [(x, y + 4) for x, y in centers], step)
    return image


def swatches(d, x, y, items, width=520):
    for color, name, use in items:
        d.rectangle([x, y, x + 64, y + 46], fill=color[:3], outline=INK, width=2)
        hexa = "#{:02X}{:02X}{:02X}".format(*color[:3])
        d.text((x + 82, y - 4), f"{hexa}  {name}", font=SMALL_B, fill=INK)
        d.text((x + 82, y + 24), use, font=SMALL, fill=SOFT)
        y += 72
    return y


# ------------------------------------------------------------------ Page 1
def page_identity():
    page, d = new_page(1, "L'ange", "Groupe de contrôle de la Krousty Corp. Gardien de Baba. Ennemi de la tour.")
    sprite = v.front()
    w, h = plate(page, sprite, MARGIN, 280, 10)
    d.text((MARGIN, 280 + h + 12), "VERSION DE BASE, DE FACE. Sprite agrandi 10 fois.", font=SMALL, fill=SOFT)

    # Mesures, sous le dessin
    y = 280 + h + 62
    d.text((MARGIN, y), "Mesures en jeu", font=H2, fill=INK)
    y += 66
    measures = (("Case du sprite", "124 x 96 px"), ("Hauteur du corps", "60 px"), ("Envergure", "environ 112 px"),
                ("Hauteur du héros", "48 px"), ("Écran du jeu", "256 x 192 px"))
    column = (w - 60) // 2
    for i, (label, value) in enumerate(measures):
        x = MARGIN + (i % 2) * (column + 60)
        row = y + (i // 2) * 44
        d.text((x, row), label, font=SMALL, fill=INK)
        d.text((x + column - d.textlength(value, font=SMALL_B), row), value, font=SMALL_B, fill=INK)

    x = MARGIN + w + 70
    width = PAGE_W - MARGIN - x
    y = 280
    d.text((x, y), "Qui il est", font=H2, fill=INK)
    y = paragraph(d, x, y + 66, "Un employé devenu séraphin. Le jeu n'explique jamais ce qu'il est. Il apparaît dès que le héros entre dans les bureaux, et il est réel pour lui.", width)
    y += 30
    d.text((x, y), "Ce qui ne change jamais", font=H2, fill=INK)
    y += 66
    for rule in ("Pas de visage. Ce sont les ailes qui regardent.",
                 "Six ailes plumées : trois paires qui partent du dos.",
                 "Un œil sur chaque aile, six en tout.",
                 "La cravate orange, toujours orange, même en ult."):
        d.ellipse([x + 2, y + 10, x + 16, y + 24], fill=ACCENT)
        y = paragraph(d, x + 34, y, rule, width - 34) + 8
    y += 30

    # Essai a l'echelle, ecran du jeu
    screen = Image.new("RGBA", (256, 192), (62, 62, 66, 255))
    sd = ImageDraw.Draw(screen)
    sd.rectangle([0, 150, 256, 192], fill=(38, 38, 42, 255))
    sd.rectangle([40, 108, 54, 150], fill=(24, 24, 28, 255))
    sd.ellipse([41, 100, 53, 112], fill=(24, 24, 28, 255))
    screen.alpha_composite(sprite, (118, 150 - (a.TOP + a.FLOAT + 60)))
    scale = max(1, min((PAGE_H - 130 - y - 44) // 192, width // 256))
    _, sh = plate(page, screen, x, y, scale)
    d.text((x, y + sh + 10), "À L'ÉCHELLE DU JEU. En noir : le gabarit du héros.", font=SMALL, fill=SOFT)
    return page


# ------------------------------------------------------------------ Page 2
def page_turnaround():
    page, d = new_page(2, "Vues et proportions", "Les quatre côtés, version de base puis ult. Toutes les vues ont la même échelle.")
    scale = 4
    label_w = 270
    cell = (PAGE_W - 2 * MARGIN - label_w) // 4
    top = 300
    views = [(name, make()) for name, make in v.VIEWS]
    for i, (name, view) in enumerate(views):
        x = MARGIN + label_w + i * cell
        d.text((x + (cell - d.textlength(name, font=H3)) / 2, top - 44), name, font=H3, fill=INK)
        plate(page, view, x + (cell - a.W * scale) // 2, top, scale, (205, 202, 194))
        plate(page, ult_view(view, a.COBALT if i % 2 == 0 else a.BLACK_YELLOW), x + (cell - a.W * scale) // 2, top + a.H * scale + 84, scale)
    d.text((MARGIN + label_w, top + a.H * scale + 28), "EN ULT, VIE BASSE. Les deux phases de couleur alternent en jeu.", font=SMALL_B, fill=ACCENT)

    # Lignes de proportion, mesurees en tetes
    base = a.TOP + a.FLOAT
    marks = (("Haut de la tête", 0), ("Menton : 1 tête", 10), ("Bas de la veste", 35), ("Pieds : 6 têtes", 60))
    line_end = PAGE_W - MARGIN
    for label, offset in marks:
        y = top + (base + offset) * scale
        for dash in range(MARGIN + label_w - 18, line_end, 34):
            d.line([(dash, y), (min(dash + 18, line_end), y)], fill=ACCENT, width=2)
        d.text((MARGIN, y - 14), label, font=SMALL, fill=INK)

    y = top + 2 * a.H * scale + 124
    notes = (("Proportions", "Une tête fait 10 pixels et le corps six têtes de haut. Silhouette très allongée, avec des bras longs jusqu'à mi-cuisse."),
             ("Perspective", "Au profil et au trois-quarts, les ailes raccourcissent. Elles restent sur la même échelle et la même hauteur que de face."),
             ("Dos", "Les six ailes s'attachent entre les omoplates, par-dessus la veste. Leurs yeux existent aussi de ce côté."))
    column = (PAGE_W - 2 * MARGIN - 2 * 70) // 3
    for i, (title, text) in enumerate(notes):
        x = MARGIN + i * (column + 70)
        d.text((x, y), title, font=H3, fill=INK)
        paragraph(d, x, y + 44, text, column, BODY)
    return page


# ------------------------------------------------------------------ Page 3
def page_details():
    page, d = new_page(3, "Détails et couleurs", "Comment sont construits l'aile, l'œil et les larmes, et les couleurs exactes.")
    left = MARGIN
    # Aile
    wing_scale = 8
    crop_box = (2, a.FLOAT + 2, a.CX, a.TOP + a.FLOAT + 44)
    wing = v.front().crop(crop_box)
    back = Image.new("RGBA", wing.size, PLATE + (255,))
    back.alpha_composite(wing)
    page.paste(back.convert("RGB").resize((wing.width * wing_scale, wing.height * wing_scale), Image.NEAREST), (left, 290))
    wing_h = wing.height * wing_scale
    x = left + wing.width * wing_scale + 40
    d.text((x, 286), "L'aile", font=H2, fill=INK)
    y = 352
    width = 640
    for title, text in (("Trois paires", "Du fond vers l'avant : les ailes du haut, celles du milieu, puis la petite paire du bas."),
                        ("Trois rangs de plumes", "Les longues plumes, les moyennes, puis les petites plumes blanches près de l'attache."),
                        ("Traits gris", "Chaque plume a un bord ombré et une nervure. Les plus longues ont une barbe."),
                        ("Silhouette", "Seul le tour de l'aile est cerné de noir, jamais chaque plume : sinon on obtient une armure.")):
        d.text((x, y), title, font=H3, fill=INK)
        y = paragraph(d, x, y + 40, text, width, SMALL) + 16

    # Oeil et larmes
    row = 290 + wing_h + 54
    d.text((left, row), "L'œil et les larmes", font=H2, fill=INK)
    row += 72
    ex, ey = eyes_in(v.front())[0]
    ult = ult_view(v.front(), a.COBALT, 6)
    closed = a.new_layer()
    a.eye(closed, 8, 5, 4)
    opened = a.new_layer()
    a.eye(opened, 8, 5)
    shots = ((opened.crop((2, 1, 15, 10)), "Ouvert, 9 x 5 px"), (closed.crop((2, 1, 15, 10)), "Fermé, clignement"),
             (ult.crop((ex - 8, ey - 5, ex + 9, ey + 22)), "Ult : cerné, en larmes"))
    x = left
    for shot, caption in shots:
        zoom = 18 if shot.height < 15 else 11
        _, sh = plate(page, shot, x, row, zoom)
        d.text((x, row + sh + 8), caption, font=SMALL, fill=SOFT)
        x += max(shot.width * zoom, 300) + 50

    # Couleurs
    cx = 1640
    d.text((cx, 286), "Base", font=H2, fill=INK)
    y = swatches(d, cx, 352, ((a.WHITE, "Blanc", "plumes, veste, tête"), (a.PALE, "Blanc cassé", "grandes plumes"),
                              (a.LIGHT, "Gris clair", "ombres douces"), (a.MID, "Gris moyen", "traits, jambe"),
                              (a.DARK, "Gris foncé", "revers, paupière"), (a.OUTLINE, "Encre", "contour, pupille"),
                              (a.ORANGE, "Orange", "cravate et iris : seule couleur")))
    y += 26
    d.text((cx, y), "Ult", font=H2, fill=INK)
    swatches(d, cx, y + 66, ((a.COBALT_BLUE, "Cobalt", "plumes, phase 1"), (a.YELLOW, "Jaune", "contours, phase 1"),
                             ((238, 255, 136, 255), "Jaune pâle", "plumes, phase 2"), (a.OUTLINE, "Noir", "plumes, phase 2"),
                             ((244, 0, 26, 255), "Rouge 3D", "dédoublement, côté gauche"), ((20, 0, 244, 255), "Bleu 3D", "dédoublement, côté droit"),
                             (a.BLOOD_BRIGHT, "Sang", "larmes, cernées de foncé")))
    return page


# ------------------------------------------------------------------ Page 4
def strip(page, d, sheet, x, y, scale, frames, per_row=6):
    for position, i in enumerate(frames):
        frame = sheet.crop((a.W * i, 0, a.W * i + a.W, a.H))
        cx = x + (position % per_row) * (a.W * scale + 6)
        cy = y + (position // per_row) * (a.H * scale + 34)
        plate(page, frame, cx, cy, scale)
        d.text((cx + 6, cy + a.H * scale + 2), f"{i + 1:03d}", font=SMALL, fill=SOFT)


def page_animation():
    page, d = new_page(4, "Animations", "120 ms par image. Base : 120 images, 14,4 secondes. Ult : 12 images, 1,4 seconde.")
    idle = Image.open("outputs/ange_sprite_idle_sheet.png").convert("RGBA")
    ult = Image.open("outputs/ange_sprite_ult_sheet.png").convert("RGBA")
    scale = 3
    x = MARGIN
    y = 262
    d.text((x, y), "Base, au combat", font=H3, fill=INK)
    d.text((x + 380, y + 3), "Battement lent et flottement de 2 px. En haut : un battement. En bas : un clignement lent.", font=SMALL, fill=SOFT)
    strip(page, d, idle, x, y + 42, scale, (0, 2, 4, 14, 16, 18) + (6, 7, 8, 9, 12, 14))
    y2 = y + 42 + 2 * (a.H * scale + 34) + 18
    d.text((x, y2), "Ult, vie basse", font=H3, fill=ACCENT)
    d.text((x + 380, y2 + 3), "Cobalt puis noir et jaune toutes les 2 images, 3D de 1 à 3 px, yeux ouverts, sang.", font=SMALL, fill=SOFT)
    strip(page, d, ult, x, y2 + 42, scale, range(a.FRAMES))
    return page


# ------------------------------------------------------------------ Page 5
def rainbow(image):
    result = image.copy()
    px = result.load()
    for y in range(a.H):
        for x in range(a.W):
            r, g, b, alpha = px[x, y]
            if not alpha or px[x, y] == a.OUTLINE or abs(x - a.CX) <= 12:
                continue
            red, green, blue = colorsys.hsv_to_rgb((x / 70 + y / 110) % 1.0, 0.62, r / 255)
            px[x, y] = (int(red * 255), int(green * 255), int(blue * 255), 255)
    return result


def only_3d(image):
    result = a.anaglyph(image, 2)
    a.restore_tie(result, image)
    return result


def sky_test(image):
    result = image.copy()
    px = result.load()
    for y in range(a.H):
        for x in range(a.W):
            r, g, b, alpha = px[x, y]
            if alpha and b >= 120 and r < 120 and g <= 26:
                px[x, y] = (110, 200, 255, 255)
    return result


def page_psychedelic():
    page, d = new_page(5, "Recherches psychédéliques", "Tout ce qu'on a essayé pour l'ult, ce qui est retenu et pourquoi le reste est écarté.")
    clean = v.front()
    pink_a = (255, 244, 244, 255)
    pink_b = (255, 222, 222, 255)
    flash = {a.OUTLINE: (136, 136, 142, 255), a.MID: (222, 218, 255, 255), a.PALE: (238, 255, 222, 255), a.WHITE: pink_a,
             a.LIGHT: (255, 20, 20, 255), a.DARK: (26, 255, 190, 255), a.ORANGE: (190, 194, 255, 255), a.ORANGE_D: (84, 84, 92, 255)}
    bug_cobalt = {a.OUTLINE: a.YELLOW, a.MID: a.COBALT_BLUE, a.PALE: (136, 136, 142, 255), a.WHITE: pink_a, a.LIGHT: pink_b,
                  a.DARK: (190, 194, 255, 255), a.ORANGE: (218, 255, 190, 255), a.ORANGE_D: a.ORANGE}
    bug_black = {a.OUTLINE: (136, 142, 255, 255), a.MID: (238, 255, 136, 255), a.PALE: a.OUTLINE, a.WHITE: pink_a, a.LIGHT: pink_b,
                 a.DARK: (218, 255, 190, 255), a.ORANGE: (190, 194, 255, 255), a.ORANGE_D: (84, 84, 92, 255)}
    cyan = {**bug_cobalt, a.WHITE: (0, 225, 255, 255), a.LIGHT: (0, 170, 225, 255)}
    orange = {**bug_cobalt, a.WHITE: a.WHITE, a.LIGHT: a.ORANGE}

    sheet = Image.open("outputs/ange_sprite_ult_sheet.png").convert("RGBA")
    final_a = sheet.crop((a.W, 0, a.W * 2, a.H))
    final_b = sheet.crop((a.W * 2, 0, a.W * 3, a.H))

    cells = [
        (final_a, "RETENU", "Cobalt, jaune et 3D", "L'ult finale, phase 1."),
        (final_b, "RETENU", "Noir, jaune et 3D", "L'ult finale, phase 2."),
        (recolored(clean, bug_cobalt), "ORIGINE", "Le bug, cobalt", "Un vrai bug de GIF. Tout est parti de là."),
        (recolored(clean, bug_black), "ORIGINE", "Le bug, noir et jaune", "Gardé, sans son rose pâle."),
        (recolored(clean, flash), "ÉCARTÉ", "Le bug, flash pâle", "L'étape rose, retirée."),
        (only_3d(clean), "ÉCARTÉ", "3D seule", "Belle, mais pas assez psychédélique."),
        (rainbow(clean), "ÉCARTÉ", "Arc-en-ciel", "Trop de couleurs, on perd les yeux."),
        (recolored(clean, cyan), "ÉCARTÉ", "Cyan électrique", "On perd le blanc de l'ange."),
        (recolored(clean, orange), "ÉCARTÉ", "Blanc et orange", "L'orange doit rester à la cravate."),
        (sky_test(final_a), "ÉCARTÉ", "Bleu ciel", "Moins agressif que le cobalt."),
    ]
    colors = {"RETENU": ACCENT, "ORIGINE": INK, "ÉCARTÉ": SOFT}
    columns = 5
    scale = 3
    cell_w = (PAGE_W - 2 * MARGIN) // columns
    for i, (image, status, title, why) in enumerate(cells):
        x = MARGIN + (i % columns) * cell_w
        y = 280 + (i // columns) * 640
        plate(page, image, x, y + 44, scale)
        d.text((x, y), status, font=H3, fill=colors[status])
        ty = y + 44 + a.H * scale + 12
        d.text((x, ty), title, font=SMALL_B, fill=INK)
        paragraph(d, x, ty + 34, why, a.W * scale, SMALL, SOFT)
    return page


def main():
    target = sys.argv[1]
    if os.path.exists(target) and "--force" not in sys.argv:
        raise SystemExit(f"Existe deja : {target}")
    pages = [page_identity(), page_turnaround(), page_details(), page_animation(), page_psychedelic()]
    pages[0].save(target, "PDF", save_all=True, append_images=pages[1:], resolution=300, quality=96, subsampling=0)
    print(target, os.path.getsize(target))


if __name__ == "__main__":
    main()
