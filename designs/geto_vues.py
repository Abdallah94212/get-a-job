"""Vues d'Old Geto pour sa fiche : face, trois-quarts, profil et dos.

Au trois-quarts et au profil il regarde vers la gauche, pour qu'on voie la serviette sur son epaule gauche.
"""
from PIL import Image, ImageDraw
import geto_sprite as g

CX = g.CX
W_BODY = g.W


def with_hat(body, hat_shift=0):
    layer = g.new_layer()
    layer.alpha_composite(g.hat(), (hat_shift, 0))
    layer.alpha_composite(body, (0, g.HAT_ROOM))
    return layer


def front():
    return g.sprite()


def back():
    body = g.silhouette()
    px = body.load()
    # Couronne de cheveux tout autour de l'arriere du crane, le dessus reste degarni
    for y in range(9, 17):
        for x in range(CX - 9, CX + 10):
            top = 10 if x % 3 else 9
            if px[x, y] in (g.SKIN, g.SKIN_SHADE) and top <= y <= 14 + (x % 2):
                px[x, y] = g.HAIR
    # De dos, on ne voit pas le devant du tablier : sous le ventre, c'est le pantalon
    for y in range(52, 66):
        for x in range(W_BODY):
            if px[x, y] in (g.WHITE, g.PALE, g.APRON_SHADE, g.MID_STAIN):
                px[x, y] = g.TROUSERS if x < CX else g.TROUSERS_SHADE
    d = ImageDraw.Draw(body)
    d.ellipse([CX - 5, 3, CX - 1, 5], fill=g.SKIN_LIGHT)
    d.line([(CX - 5, 19), (CX + 5, 19)], fill=g.SKIN_SHADE)
    d.line([(CX - 4, 21), (CX + 4, 21)], fill=g.SKIN_SHADE)
    # Manches et dos du t-shirt
    d.polygon([(CX - 18, 22), (CX - 14, 22), (CX - 20, 33), (CX - 26, 33)], fill=g.SHIRT_SHADE)
    d.polygon([(CX + 14, 22), (CX + 18, 22), (CX + 26, 33), (CX + 20, 33)], fill=g.SHIRT_SHADE)
    d.line([(CX - 27, 34), (CX - 21, 34)], fill=g.OUTLINE)
    d.line([(CX + 21, 34), (CX + 27, 34)], fill=g.OUTLINE)
    for x in (CX - 27, CX - 25, CX - 23, CX + 23, CX + 25, CX + 27):
        d.line([(x, 54), (x, 57)], fill=g.OUTLINE)
    d.arc([CX - 22, 28, CX + 22, 58], 20, 160, fill=g.SHIRT_SHADE)
    # De dos, le tablier n'est plus qu'un cordon noue au creux des reins
    d.rectangle([CX - 20, 49, CX + 20, 50], fill=g.WHITE)
    d.line([(CX - 20, 51), (CX + 20, 51)], fill=g.APRON_SHADE)
    bow = g.new_layer()
    bd = ImageDraw.Draw(bow)
    bd.polygon([(CX - 6, 46), (CX - 1, 49), (CX - 6, 52)], fill=g.WHITE)
    bd.polygon([(CX + 6, 46), (CX + 1, 49), (CX + 6, 52)], fill=g.WHITE)
    bd.rectangle([CX - 1, 48, CX + 1, 51], fill=g.PALE)
    bd.line([(CX - 1, 52), (CX - 3, 58)], fill=g.WHITE, width=2)
    bd.line([(CX + 1, 52), (CX + 3, 58)], fill=g.WHITE, width=2)
    body.alpha_composite(g.outlined(bow))
    # Fond du pantalon
    d.line([(CX, 56), (CX, 70)], fill=g.TROUSERS_SHADE)
    # La serviette pend dans son dos, sur l'epaule gauche
    towel = g.new_layer()
    td = ImageDraw.Draw(towel)
    td.polygon([(CX - 18, 21), (CX - 9, 19), (CX - 11, 36), (CX - 19, 37)], fill=g.WHITE)
    towel = g.outlined(towel)
    td = ImageDraw.Draw(towel)
    td.line([(CX - 14, 21), (CX - 15, 35)], fill=g.PALE)
    td.line([(CX - 18, 33), (CX - 12, 32)], fill=g.APRON_SHADE)
    body.alpha_composite(towel)
    return with_hat(body)


def profile_right():
    """Dessine tourne vers la droite, cote serviette ; on le retourne ensuite."""
    back_layer = g.new_layer()
    d = ImageDraw.Draw(back_layer)
    d.polygon([(CX - 8, 54), (CX + 4, 54), (CX + 3, 70), (CX - 7, 70)], fill=g.TROUSERS)
    d.rectangle([CX - 8, 71, CX + 8, 73], fill=g.SHOE)
    body = g.outlined(back_layer)

    torso = g.new_layer()
    d = ImageDraw.Draw(torso)
    d.polygon([(CX - 10, 21), (CX + 6, 21), (CX + 9, 30), (CX - 11, 46), (CX - 12, 30)], fill=g.SHIRT)
    d.ellipse([CX - 12, 26, CX + 19, 58], fill=g.SHIRT)
    d.polygon([(CX + 3, 49), (CX + 17, 51), (CX + 15, 65), (CX + 1, 65)], fill=g.WHITE)
    d.rectangle([CX - 5, 18, CX + 5, 24], fill=g.SKIN_SHADE)
    d.polygon([(CX - 7, 5), (CX - 9, 8), (CX - 8, 13), (CX - 4, 13)], fill=g.HAIR)
    d.ellipse([CX - 7, 2, CX + 7, 23], fill=g.SKIN)
    d.polygon([(CX + 6, 12), (CX + 9, 15), (CX + 6, 16)], fill=g.SKIN)
    body.alpha_composite(g.outlined(torso))

    arm = g.new_layer()
    d = ImageDraw.Draw(arm)
    d.polygon([(CX - 7, 21), (CX + 2, 21), (CX + 4, 50), (CX - 6, 50)], fill=g.SKIN)
    d.rectangle([CX - 5, 51, CX + 3, 57], fill=g.SKIN)
    body.alpha_composite(g.outlined(arm))

    d = ImageDraw.Draw(body)
    d.polygon([(CX - 6, 22), (CX + 1, 22), (CX + 2, 33), (CX - 6, 33)], fill=g.SHIRT_SHADE)
    d.line([(CX - 7, 34), (CX + 3, 34)], fill=g.OUTLINE)
    for x in (CX - 3, CX - 1, CX + 1):
        d.line([(x, 54), (x, 57)], fill=g.OUTLINE)
    d.chord([CX - 12, 44, CX + 19, 58], 0, 180, fill=g.SHIRT_SHADE)
    d.arc([CX - 12, 44, CX + 19, 58], 0, 180, fill=g.OUTLINE)
    d.line([(CX - 11, 50), (CX + 3, 50)], fill=g.WHITE)
    d.line([(CX + 11, 53), (CX + 12, 64)], fill=g.APRON_SHADE)
    g.stamp(body, g.ROOSTER, CX + 7, 32)
    # Visage de profil
    d.ellipse([CX - 4, 4, CX, 6], fill=g.SKIN_LIGHT)
    d.rectangle([CX - 2, 10, CX - 1, 14], fill=g.SKIN_SHADE)
    d.line([(CX + 1, 9), (CX + 7, 9)], fill=g.SCAR)
    for x in (CX + 2, CX + 5):
        d.line([(x, 8), (x, 10)], fill=g.SCAR)
    d.line([(CX + 3, 14), (CX + 5, 14)], fill=g.OUTLINE)
    d.line([(CX + 3, 19), (CX + 6, 19), (CX + 7, 18)], fill=g.OUTLINE)
    d.line([(CX - 3, 21), (CX + 5, 21)], fill=g.SKIN_SHADE)
    # La serviette passe par-dessus l'epaule, un pan devant, un pan derriere
    towel = g.new_layer()
    td = ImageDraw.Draw(towel)
    td.polygon([(CX - 9, 21), (CX + 4, 19), (CX + 6, 33), (CX + 1, 34), (CX - 1, 24), (CX - 4, 32), (CX - 9, 31)], fill=g.WHITE)
    towel = g.outlined(towel)
    td = ImageDraw.Draw(towel)
    td.line([(CX + 3, 22), (CX + 4, 32)], fill=g.PALE)
    td.line([(CX - 7, 23), (CX - 7, 30)], fill=g.APRON_SHADE)
    body.alpha_composite(towel)
    return with_hat(body, -2)


def profile():
    return profile_right().transpose(Image.FLIP_LEFT_RIGHT)


def three_quarter():
    """De trois-quarts, tourne vers la gauche : le visage glisse a gauche, la serviette reste devant."""
    behind = g.new_layer()
    d = ImageDraw.Draw(behind)
    d.polygon([(CX - 14, 54), (CX - 3, 54), (CX - 4, 70), (CX - 13, 70)], fill=g.TROUSERS)
    d.polygon([(CX, 54), (CX + 11, 54), (CX + 10, 70), (CX + 1, 70)], fill=g.TROUSERS_SHADE)
    d.rectangle([CX - 17, 71, CX - 4, 73], fill=g.SHOE)
    d.rectangle([CX - 1, 71, CX + 11, 73], fill=g.SHOE)
    d.polygon([(CX - 17, 22), (CX - 13, 22), (CX - 21, 49), (CX - 27, 49), (CX - 25, 31)], fill=g.SKIN)
    d.rectangle([CX - 27, 50, CX - 21, 55], fill=g.SKIN)
    d.polygon([(CX + 11, 21), (CX + 17, 21), (CX + 25, 30), (CX + 27, 50), (CX + 18, 50)], fill=g.SKIN_SHADE)
    d.rectangle([CX + 19, 51, CX + 27, 57], fill=g.SKIN_SHADE)
    body = g.outlined(behind)

    torso = g.new_layer()
    d = ImageDraw.Draw(torso)
    d.polygon([(CX - 17, 21), (CX + 15, 21), (CX + 18, 36), (CX - 20, 36)], fill=g.SHIRT)
    d.ellipse([CX - 25, 27, CX + 19, 59], fill=g.SHIRT)
    d.polygon([(CX - 21, 49), (CX + 15, 49), (CX + 12, 65), (CX - 19, 65)], fill=g.WHITE)
    d.rectangle([CX - 8, 18, CX + 5, 24], fill=g.SKIN_SHADE)
    d.polygon([(CX + 5, 4), (CX + 9, 7), (CX + 9, 13), (CX + 5, 13)], fill=g.HAIR)
    d.polygon([(CX - 11, 6), (CX - 12, 8), (CX - 11, 11), (CX - 9, 11)], fill=g.HAIR)
    d.ellipse([CX - 11, 2, CX + 7, 23], fill=g.SKIN)
    d.rectangle([CX + 7, 11, CX + 8, 15], fill=g.SKIN_SHADE)
    d.polygon([(CX - 10, 13), (CX - 13, 16), (CX - 10, 17)], fill=g.SKIN)
    body.alpha_composite(g.outlined(torso))

    d = ImageDraw.Draw(body)
    d.polygon([(CX + 12, 22), (CX + 16, 22), (CX + 24, 33), (CX + 18, 33)], fill=g.SHIRT_SHADE)
    d.line([(CX + 19, 34), (CX + 25, 34)], fill=g.OUTLINE)
    for x in (CX + 21, CX + 23, CX + 25):
        d.line([(x, 54), (x, 57)], fill=g.OUTLINE)
    d.chord([CX - 25, 44, CX + 19, 58], 0, 180, fill=g.SHIRT_SHADE)
    d.arc([CX - 25, 44, CX + 19, 58], 0, 180, fill=g.OUTLINE)
    d.polygon([(CX - 21, 51), (CX + 15, 51), (CX + 12, 64), (CX - 19, 64)], fill=g.WHITE)
    d.arc([CX - 25, 44, CX + 19, 58], 15, 165, fill=g.OUTLINE)
    d.polygon([(CX + 8, 53), (CX + 14, 51), (CX + 12, 64), (CX + 8, 64)], fill=g.PALE)
    d.line([(CX - 11, 58), (CX - 12, 64)], fill=g.APRON_SHADE)
    g.stamp(body, g.ROOSTER, CX - 9, 32)
    g.pixel_text(body, "MAITRE", CX - 16, 40)
    g.pixel_text(body, "POULET", CX - 16, 44)
    # Visage tourne vers la gauche
    d.ellipse([CX - 8, 4, CX - 4, 6], fill=g.SKIN_LIGHT)
    d.arc([CX - 11, 7, CX + 7, 23], 10, 80, fill=g.SKIN_SHADE)
    d.line([(CX - 10, 9), (CX + 5, 9)], fill=g.SCAR)
    for x in range(CX - 9, CX + 6, 3):
        d.line([(x, 8), (x, 10)], fill=g.SCAR)
    d.line([(CX - 10, 14), (CX - 7, 14)], fill=g.OUTLINE)
    d.line([(CX - 2, 14), (CX + 2, 14)], fill=g.OUTLINE)
    d.line([(CX - 9, 13), (CX - 8, 13)], fill=g.SKIN_SHADE)
    d.line([(CX - 1, 13), (CX + 1, 13)], fill=g.SKIN_SHADE)
    d.line([(CX - 8, 19), (CX - 2, 19), (CX, 17)], fill=g.OUTLINE)
    d.line([(CX - 8, 21), (CX + 4, 21)], fill=g.SKIN_SHADE)
    towel = g.new_layer()
    td = ImageDraw.Draw(towel)
    td.polygon([(CX + 7, 19), (CX + 16, 21), (CX + 17, 37), (CX + 9, 36)], fill=g.WHITE)
    towel = g.outlined(towel)
    td = ImageDraw.Draw(towel)
    td.line([(CX + 12, 21), (CX + 13, 35)], fill=g.PALE)
    td.line([(CX + 10, 32), (CX + 16, 33)], fill=g.APRON_SHADE)
    body.alpha_composite(towel)
    return with_hat(body, -2)


VIEWS = (("FACE", front), ("TROIS-QUARTS", three_quarter), ("PROFIL", profile), ("DOS", back))


if __name__ == "__main__":
    sheet = Image.new("RGBA", (g.W * 4, g.H), g.STAGE)
    for i, (_, view) in enumerate(VIEWS):
        sheet.alpha_composite(view(), (g.W * i, 0))
    sheet.resize((g.W * 4 * 5, g.H * 5), Image.NEAREST).save("outputs/geto_vues_test.png")
