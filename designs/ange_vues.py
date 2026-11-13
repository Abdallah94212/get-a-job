"""Vues de l'ange pour la fiche de personnage : face, trois-quarts, profil et dos."""
import math
from PIL import Image, ImageDraw
import ange_sprite as a

CX, T = a.CX, a.TOP


def squeezed_angle(angle, k):
    rad = math.radians(angle)
    return math.degrees(math.atan2(math.sin(rad), math.cos(rad) * k)) % 360


def wings_left(k, root_x=CX, lift=0):
    """Les trois ailes d'un cote, pointant a gauche, ecrasees par k selon l'angle de vue."""
    layer = a.new_layer()
    for _, (p0, p1, p2, hang, reach, longest, eye_at), _ in a.wing_specs(0.0):
        points = [(root_x + (x - CX) * k, y - lift) for x, y in (p0, p1, p2)]
        layer.alpha_composite(a.left_wing(*points, squeezed_angle(hang, k), squeezed_angle(reach, k), longest, eye_at))
    return layer


def wings_right(k, root_x=CX, lift=0):
    return wings_left(k, a.W - 1 - root_x, lift).transpose(Image.FLIP_LEFT_RIGHT)


def placed(layers):
    """Superpose les calques et applique la meme marge que le sprite."""
    view = a.new_layer()
    for layer in layers:
        view.alpha_composite(layer, (0, a.FLOAT))
    return view


def front():
    return a.frame(0.0, 0)


def back():
    """De dos, les ailes s'attachent devant nous, par-dessus la veste."""
    body = a.new_layer()
    d = ImageDraw.Draw(body)
    d.polygon([(CX - 4, T + 35), (CX - 1, T + 35), (CX - 2, T + 58), (CX - 3, T + 58)], fill=a.MID)
    d.polygon([(CX + 1, T + 35), (CX + 4, T + 35), (CX + 3, T + 58), (CX + 2, T + 58)], fill=a.LIGHT)
    d.line([(CX - 5, T + 59), (CX - 2, T + 59)], fill=a.LIGHT)
    d.line([(CX + 2, T + 59), (CX + 5, T + 59)], fill=a.WHITE)
    d.polygon([(CX - 6, T + 12), (CX - 5, T + 12), (CX - 6, T + 36), (CX - 8, T + 36)], fill=a.MID)
    d.polygon([(CX + 5, T + 12), (CX + 6, T + 12), (CX + 8, T + 36), (CX + 6, T + 36)], fill=a.LIGHT)
    d.line([(CX - 7, T + 37), (CX - 7, T + 40)], fill=a.WHITE)
    d.line([(CX + 7, T + 37), (CX + 7, T + 40)], fill=a.WHITE)
    d.polygon([(CX - 6, T + 11), (CX + 6, T + 11), (CX + 4, T + 24), (CX + 5, T + 35), (CX - 5, T + 35), (CX - 4, T + 24)], fill=a.WHITE)
    d.rectangle([CX - 1, T + 8, CX + 1, T + 11], fill=a.MID)
    d.ellipse([CX - 3, T, CX + 3, T + 9], fill=a.WHITE)
    body = a.outlined(body)
    d = ImageDraw.Draw(body)
    d.line([(CX, T + 24), (CX, T + 34)], fill=a.DARK)  # couture et fente de la veste
    d.line([(CX - 3, T + 3), (CX - 3, T + 6)], fill=a.LIGHT)
    wings = a.new_layer()
    wings.alpha_composite(wings_left(1.0))
    wings.alpha_composite(wings_right(1.0))
    return placed([body, wings])


def profile():
    """De profil, tourne vers la droite. L'aile lointaine depasse derriere la proche."""
    body = a.new_layer()
    d = ImageDraw.Draw(body)
    d.polygon([(CX - 2, T + 35), (CX + 2, T + 35), (CX + 1, T + 58), (CX - 1, T + 58)], fill=a.LIGHT)
    d.line([(CX - 1, T + 59), (CX + 4, T + 59)], fill=a.WHITE)
    d.polygon([(CX - 3, T + 11), (CX + 3, T + 11), (CX + 3, T + 35), (CX - 3, T + 35), (CX - 2, T + 24)], fill=a.WHITE)
    d.rectangle([CX - 1, T + 8, CX + 1, T + 11], fill=a.MID)
    d.ellipse([CX - 3, T, CX + 3, T + 9], fill=a.WHITE)
    body = a.outlined(body)
    d = ImageDraw.Draw(body)
    d.line([(CX + 3, T + 12), (CX + 3, T + 19)], fill=a.ORANGE)
    d.line([(CX - 1, T + 13), (CX - 1, T + 37)], fill=a.DARK)
    d.line([(CX, T + 13), (CX, T + 37)], fill=a.LIGHT)
    d.line([(CX, T + 38), (CX, T + 41)], fill=a.WHITE)
    return placed([wings_left(0.5, CX + 1, 3), body, wings_left(0.8, CX - 2)])


def three_quarter():
    """De trois-quarts, tourne vers la droite : l'aile lointaine est tres raccourcie."""
    body = a.new_layer()
    d = ImageDraw.Draw(body)
    d.polygon([(CX - 3, T + 35), (CX, T + 35), (CX - 1, T + 58), (CX - 2, T + 58)], fill=a.LIGHT)
    d.polygon([(CX + 1, T + 35), (CX + 4, T + 35), (CX + 3, T + 58), (CX + 2, T + 58)], fill=a.MID)
    d.line([(CX - 3, T + 59), (CX, T + 59)], fill=a.WHITE)
    d.line([(CX + 2, T + 59), (CX + 6, T + 59)], fill=a.LIGHT)
    d.polygon([(CX - 5, T + 12), (CX - 4, T + 12), (CX - 5, T + 36), (CX - 7, T + 36)], fill=a.LIGHT)
    d.polygon([(CX + 4, T + 12), (CX + 5, T + 12), (CX + 6, T + 36), (CX + 5, T + 36)], fill=a.MID)
    d.line([(CX - 6, T + 37), (CX - 6, T + 40)], fill=a.WHITE)
    d.line([(CX + 5, T + 37), (CX + 5, T + 40)], fill=a.WHITE)
    d.polygon([(CX - 5, T + 11), (CX + 5, T + 11), (CX + 3, T + 24), (CX + 4, T + 35), (CX - 4, T + 35), (CX - 3, T + 24)], fill=a.WHITE)
    d.rectangle([CX, T + 8, CX + 2, T + 11], fill=a.MID)
    d.ellipse([CX - 2, T, CX + 4, T + 9], fill=a.WHITE)
    body = a.outlined(body)
    d = ImageDraw.Draw(body)
    d.line([(CX, T + 11), (CX + 2, T + 21)], fill=a.DARK)
    d.line([(CX + 3, T + 11), (CX + 2, T + 21)], fill=a.DARK)
    d.line([(CX + 2, T + 22), (CX + 2, T + 34)], fill=a.DARK)
    d.line([(CX + 2, T + 12), (CX + 2, T + 19)], fill=a.ORANGE)
    d.line([(CX + 3, T + 13), (CX + 2, T + 24)], fill=a.LIGHT)
    return placed([wings_right(0.4, CX + 2), wings_left(0.85, CX - 1), body])


VIEWS = (("FACE", front), ("TROIS-QUARTS", three_quarter), ("PROFIL", profile), ("DOS", back))


if __name__ == "__main__":
    sheet = Image.new("RGBA", (a.W * 4, a.H), (150, 150, 154, 255))
    for i, (_, view) in enumerate(VIEWS):
        sheet.alpha_composite(view(), (a.W * i, 0))
    sheet.resize((a.W * 4 * 3, a.H * 3), Image.NEAREST).save("outputs/ange_vues_test.png")
