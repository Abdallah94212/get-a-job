"""Fiche de personnage d'Old Geto, sur le modele de celle des anges : un PDF A4 paysage de quatre pages."""
import os
import sys
from PIL import Image, ImageDraw
import ange_fiche as f
import ange_sprite as angel
import geto_sprite as g
import geto_vues as v

TOTAL = 4
WHO = "Old Geto, gérant du Maître Poulet"
TOP = g.HAT_ROOM  # haut du corps, sous la toque


def page(number, title, subtitle):
    return f.new_page(number, title, subtitle, TOTAL, WHO)


def bullet_list(d, x, y, items, width):
    for item in items:
        d.ellipse([x + 2, y + 10, x + 16, y + 24], fill=f.ACCENT)
        y = f.paragraph(d, x + 34, y, item, width - 34) + 8
    return y


def alley_screen(sprite):
    screen = Image.new("RGBA", (256, 192), (62, 62, 66, 255))
    d = ImageDraw.Draw(screen)
    d.rectangle([0, 150, 256, 192], fill=(38, 38, 42, 255))
    d.rectangle([50, 108, 64, 150], fill=(24, 24, 28, 255))
    d.ellipse([51, 100, 63, 112], fill=(24, 24, 28, 255))
    screen.alpha_composite(sprite, (140, 150 - 88))
    return screen


# ------------------------------------------------------------------ Page 1
def page_identity():
    p, d = page(1, "Old Geto", "Gérant du Maître Poulet. Le premier patron du héros, puis le boss de la vendetta.")
    sprite = v.front()
    w, h = f.plate(p, sprite, f.MARGIN, 280, 14)
    d.text((f.MARGIN, 280 + h + 12), "DE FACE. Sprite agrandi 14 fois.", font=f.SMALL, fill=f.SOFT)

    x = f.MARGIN + w + 80
    width = f.PAGE_W - f.MARGIN - x
    y = 280
    d.text((x, y), "Qui il est", font=f.H2, fill=f.INK)
    y = f.paragraph(d, x, y + 66, "Il vend la Verte derrière le comptoir, il compte les billets et il garde tout. Calme, souriant, condescendant. Il prend le départ du héros comme une trahison et lance la vendetta. En vérité, c'est un pion de Baba depuis vingt-cinq ans.", width)
    y += 26
    d.text((x, y), "Ce qui ne change jamais", font=f.H2, fill=f.INK)
    y = bullet_list(d, x, y + 66, (
        "La cicatrice droite en travers du front, noire et recousue.",
        "La calvitie : des cheveux sur les côtés, la toque posée en arrière.",
        "Le tablier blanc sans bretelles et la serviette sur l'épaule gauche.",
        "Le logo orange du Maître Poulet : sa seule couleur.",
        "Le demi-sourire en coin et les yeux plissés.",
    ), width)
    y += 22
    d.text((x, y), "Sa phrase", font=f.H2, fill=f.INK)
    y = f.paragraph(d, x, y + 66, "« Les faibles livrent. Les forts encaissent. C'est pas moi qui ai fait le monde comme ça. »", width, f.H3)
    y += 26
    d.text((x, y), "Mesures en jeu", font=f.H2, fill=f.INK)
    y += 66
    for label, value in (("Case du sprite", "72 x 90 px"), ("Hauteur avec la toque", "88 px"),
                         ("Hauteur du corps", "72 px"), ("Largeur, bras compris", "61 px"),
                         ("Hauteur du héros", "48 px")):
        d.text((x, y), label, font=f.SMALL, fill=f.INK)
        d.text((x + width - d.textlength(value, font=f.SMALL_B), y), value, font=f.SMALL_B, fill=f.INK)
        y += 40
    return p


# ------------------------------------------------------------------ Page 2
def page_turnaround():
    p, d = page(2, "Vues et proportions", "Les quatre côtés à la même échelle. De trois-quarts et de profil, il regarde vers la gauche.")
    scale = 6
    label_w = 300
    cell = (f.PAGE_W - 2 * f.MARGIN - label_w) // 4
    top = 310
    for i, (name, make) in enumerate(v.VIEWS):
        x = f.MARGIN + label_w + i * cell
        d.text((x + (cell - d.textlength(name, font=f.H3)) / 2, top - 46), name, font=f.H3, fill=f.INK)
        f.plate(p, make(), x + (cell - g.W * scale) // 2, top, scale, (205, 202, 194))
    marks = (("Haut de la toque", 0), ("Haut du crâne", TOP + 2), ("Menton : 1 tête", TOP + 23),
             ("Nœud du tablier", TOP + 49), ("Pieds : 3,5 têtes", TOP + 73))
    for label, offset in marks:
        y = top + offset * scale
        for dash in range(f.MARGIN + label_w - 18, f.PAGE_W - f.MARGIN, 34):
            d.line([(dash, y), (min(dash + 18, f.PAGE_W - f.MARGIN), y)], fill=f.ACCENT, width=2)
        d.text((f.MARGIN, y - 14), label, font=f.SMALL, fill=f.INK)
    y = top + g.H * scale + 70
    notes = (("Proportions", "Une tête fait 21 pixels. Le corps ne fait que 3,5 têtes de haut : il est trapu, tout en ventre, avec des bras très épais et des jambes courtes."),
             ("Profil", "Le ventre avance bien au-delà de la poitrine. La serviette passe par-dessus l'épaule : un pan devant, un pan derrière."),
             ("Dos", "Une couronne de cheveux autour de l'arrière du crâne. Le tablier n'est plus qu'un cordon noué dans le dos."))
    column = (f.PAGE_W - 2 * f.MARGIN - 2 * 70) // 3
    for i, (title, text) in enumerate(notes):
        x = f.MARGIN + i * (column + 70)
        d.text((x, y), title, font=f.H3, fill=f.INK)
        f.paragraph(d, x, y + 44, text, column, f.SMALL)
    return p


# ------------------------------------------------------------------ Page 3
def page_details():
    p, d = page(3, "Détails et couleurs", "Ce qui le rend reconnaissable au premier coup d'œil, et ses couleurs exactes.")
    front = v.front()
    crops = (
        (front.crop((g.CX - 14, 0, g.CX + 15, TOP + 25)), 16, "La tête",
         "Calvitie avec deux touffes au-dessus des oreilles. Toque posée en arrière. Cicatrice droite, noire, avec ses points de suture."),
        (front.crop((g.CX - 13, TOP + 30, g.CX + 14, TOP + 49)), 16, "Le logo",
         "Notre coq et le nom, en orange. Assez petit pour laisser voir le ventre, et sous la serviette pour rester lisible."),
        (v.back().crop((g.CX - 10, TOP + 44, g.CX + 11, TOP + 60)), 16, "Le nœud",
         "Tablier sans bretelles : on ne voit son cordon que de dos, noué au creux des reins."),
    )
    x = f.MARGIN
    y = 290
    for image, zoom, title, text in crops:
        _, h = f.plate(p, image, x, y, zoom)
        d.text((x, y + h + 16), title, font=f.H3, fill=f.INK)
        f.paragraph(d, x, y + h + 58, text, image.width * zoom, f.SMALL)
        x += image.width * zoom + 60

    cx = 1600
    d.text((cx, 286), "Couleurs", font=f.H2, fill=f.INK)
    f.swatches(d, cx, 352, ((g.WHITE, "Blanc", "toque, tablier, serviette"), (g.SKIN, "Peau", "visage et bras, en gris"),
                            (g.HAIR, "Cheveux", "touffes sur les côtés"), (g.SHIRT, "T-shirt", "gris moyen"),
                            (g.TROUSERS, "Pantalon", "gris foncé"), (g.SCAR, "Cicatrice", "noire, la plus foncée"),
                            (g.ORANGE, "Orange", "le logo : seule couleur"), (g.OUTLINE, "Encre", "contour")))
    return p


# ------------------------------------------------------------------ Page 4
def page_story():
    p, d = page(4, "À l'écran et dans l'histoire", "Sa taille face aux autres, et ce que son design raconte.")
    sprite = v.front()
    _, sh = f.plate(p, alley_screen(sprite), f.MARGIN, 290, 4)
    d.text((f.MARGIN, 290 + sh + 12), "DANS L'ALLÉE, À L'ÉCHELLE DU JEU. En noir : le gabarit du héros.", font=f.SMALL, fill=f.SOFT)

    # Alignement des tailles sur une meme ligne de sol
    lineup = Image.new("RGBA", (250, 100), (205, 202, 194, 255))
    ld = ImageDraw.Draw(lineup)
    ground = 96
    ld.rectangle([18, ground - 48, 32, ground], fill=(24, 24, 28, 255))
    ld.ellipse([19, ground - 56, 31, ground - 44], fill=(24, 24, 28, 255))
    lineup.alpha_composite(sprite, (44, ground - 88))
    angel_front = angel.frame(0.0, 0)
    lineup.alpha_composite(angel_front, (122, ground - (angel.TOP + angel.FLOAT + 60)))
    x = f.MARGIN + 256 * 4 + 80
    _, lh = f.plate(p, lineup, x, 290, 4, (205, 202, 194))
    d.text((x, 290 + lh + 12), "LES TAILLES : héros (gabarit), Old Geto, un ange.", font=f.SMALL, fill=f.SOFT)

    y = 290 + sh + 90
    notes = (("Son rôle", "Acte 1 : le patron des livraisons. Acte 3 : le boss de la vendetta, mission « Châtiment de la tombe ». Le héros lui pose trois questions, puis le joueur l'épargne ou le tue."),
             ("Son combat", "Lent mais il frappe très fort, sans colère, comme on corrige un enfant. Avant de se battre, il pose sa serviette et sa toque sur le comptoir, sans se presser. Ses bras sont étonnamment épais."),
             ("Le lien orange", "Dans le jeu, l'orange veut dire Krousty. Il le porte dès l'acte 1 sur son t-shirt : le Maître Poulet était la première affaire de Baba. Personne ne le dit."))
    column = (f.PAGE_W - 2 * f.MARGIN - 2 * 70) // 3
    for i, (title, text) in enumerate(notes):
        nx = f.MARGIN + i * (column + 70)
        d.text((nx, y), title, font=f.H3, fill=f.INK)
        f.paragraph(d, nx, y + 44, text, column, f.SMALL)
    return p


def main():
    target = sys.argv[1]
    if os.path.exists(target) and "--force" not in sys.argv:
        raise SystemExit(f"Existe deja : {target}")
    pages = [page_identity(), page_turnaround(), page_details(), page_story()]
    pages[0].save(target, "PDF", save_all=True, append_images=pages[1:], resolution=300, quality=96, subsampling=0)
    print(target, os.path.getsize(target))


if __name__ == "__main__":
    main()
