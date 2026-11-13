"""Ange du groupe de controle : seraphin en pixel art, six ailes plumees et des yeux."""
import math
from PIL import Image, ImageDraw

W, H = 212, 172
CX = W // 2
TOP = 48  # haut de la tete
ZOOM = 3
WING_SCALE = 0.82  # taille des ailes par rapport au corps

OUTLINE = (20, 20, 26, 255)
WHITE = (244, 244, 238, 255)
PALE = (226, 226, 222, 255)
LIGHT = (200, 200, 202, 255)
MID = (146, 146, 152, 255)
DARK = (84, 84, 92, 255)
ORANGE = (255, 122, 18, 255)
ORANGE_D = (196, 80, 10, 255)
BACKGROUND = (150, 150, 154, 255)

# Longueurs irregulieres : les plumes retombent en meches, pas en peigne
WISPS = (1.0, 0.82, 1.12, 0.9, 1.05, 0.78, 1.15)


def new_layer():
    return Image.new("RGBA", (W, H), (0, 0, 0, 0))


def outlined(layer):
    """Contour d'un pixel autour de la silhouette entiere, pas de chaque plume."""
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


def bezier(p0, p1, p2, t):
    u = 1 - t
    return (u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
            u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1])


def polar(x, y, angle, length):
    rad = math.radians(angle)
    return x + math.cos(rad) * length, y + math.sin(rad) * length


def feather(draw, x, y, angle, length, width, fill, edge, tip, bend):
    """Plume courbe et effilee : un rachis, un bord ombre, une pointe plus sombre."""
    steps = max(6, int(length))
    upper, lower, spine = [], [], []
    px, py = x, y
    for i in range(steps + 1):
        s = i / steps
        heading = math.radians(angle + bend * s)
        half = width * min(1.0, s * 5) * (1 - s ** 3) / 2
        spine.append((px, py))
        upper.append((px + math.cos(heading + math.pi / 2) * half, py + math.sin(heading + math.pi / 2) * half))
        lower.append((px - math.cos(heading + math.pi / 2) * half, py - math.sin(heading + math.pi / 2) * half))
        px += math.cos(heading) * length / steps
        py += math.sin(heading) * length / steps
    draw.polygon(upper + lower[::-1], fill=fill)
    draw.line(lower, fill=edge)
    draw.line(spine[steps // 5:], fill=edge if length > 16 else fill)
    draw.line(upper[int(steps * 0.72):] + lower[int(steps * 0.72):][::-1], fill=tip)


def eye(draw, x, y):
    """Oeil en amande : paupieres grises, iris orange, pupille."""
    draw.polygon([(x - 8, y), (x - 4, y - 4), (x + 4, y - 4), (x + 8, y), (x + 4, y + 4), (x - 4, y + 4)],
                 fill=WHITE, outline=DARK)
    draw.ellipse([x - 3, y - 3, x + 3, y + 3], fill=ORANGE, outline=ORANGE_D)
    draw.rectangle([x - 1, y - 1, x + 1, y + 1], fill=OUTLINE)


def left_wing(p0, p1, p2, hang, reach, longest, eye_at):
    """Aile gauche : remiges en meches, secondaires, couvertures, puis l'oeil sous un duvet."""
    layer = new_layer()
    draw = ImageDraw.Draw(layer)
    rows = [
        (12, 0.10, 1.00, longest, 8, PALE, MID, MID),
        (11, 0.04, 0.90, longest * 0.62, 7, PALE, LIGHT, MID),
        (10, 0.00, 0.78, longest * 0.34, 6, WHITE, LIGHT, LIGHT),
    ]
    for count, start, stop, length, width, fill, edge, tip in rows:
        for i in range(count):
            t = start + (stop - start) * (count - 1 - i) / (count - 1)
            x, y = bezier(p0, p1, p2, t)
            k = (t - start) / (stop - start)
            angle = hang + (reach - hang) * k ** 1.3
            size = length * (0.62 + 0.38 * k) * WISPS[i % len(WISPS)]
            feather(draw, x, y, angle, size, width, fill, edge, tip, (hang - reach) * 0.22)

    x, y = bezier(p0, p1, p2, eye_at)
    angle = hang + (reach - hang) * eye_at ** 1.3
    eye_x, eye_y = polar(x, y, angle, longest * 0.3)
    eye_x, eye_y = round(eye_x), round(eye_y)
    eye(draw, eye_x, eye_y)
    # Un duvet retombe sur la paupiere : l'oeil est dans l'aile, pas pose dessus
    for offset in (-9, -5, 0, 5, 9):
        feather(draw, eye_x + offset, eye_y - 11 + abs(offset) // 2, 90, 7, 6, WHITE, LIGHT, LIGHT, 0)
    return outlined(layer)


def mirrored(layer):
    both = layer.copy()
    both.alpha_composite(layer.transpose(Image.FLIP_LEFT_RIGHT))
    return both


def wings():
    """Trois paires identiques dans l'esprit : epaule en arc, plumes qui retombent. La derniere plus petite."""
    t = TOP

    def at(dx, dy):
        """Point d'aile mesure depuis le dos, reduit autour de l'attache."""
        return CX + dx * WING_SCALE, t + 30 + dy * WING_SCALE

    lower = left_wing(at(-7, 20), at(-28, 14), at(-54, 38), 86, 128, 40 * WING_SCALE, 0.6)
    middle = left_wing(at(-7, 0), at(-36, -26), at(-76, -4), 88, 150, 58 * WING_SCALE, 0.5)
    upper = left_wing(at(-6, -6), at(-22, -52), at(-64, -60), 150, 226, 44 * WING_SCALE, 0.5)
    layer = mirrored(middle)
    layer.alpha_composite(mirrored(lower))
    layer.alpha_composite(mirrored(upper))
    return layer


def body():
    layer = new_layer()
    d = ImageDraw.Draw(layer)
    t = TOP
    # Jambes tres longues
    d.polygon([(CX - 6, t + 66), (CX - 1, t + 66), (CX - 2, t + 112), (CX - 5, t + 112)], fill=LIGHT)
    d.polygon([(CX + 1, t + 66), (CX + 6, t + 66), (CX + 5, t + 112), (CX + 2, t + 112)], fill=MID)
    # Chaussures pointues
    d.polygon([(CX - 5, t + 112), (CX - 2, t + 112), (CX - 2, t + 116), (CX - 9, t + 116)], fill=WHITE)
    d.polygon([(CX + 2, t + 112), (CX + 5, t + 112), (CX + 9, t + 116), (CX + 2, t + 116)], fill=LIGHT)
    # Bras longs
    d.polygon([(CX - 11, t + 23), (CX - 8, t + 23), (CX - 10, t + 68), (CX - 14, t + 68)], fill=LIGHT)
    d.polygon([(CX + 8, t + 23), (CX + 11, t + 23), (CX + 14, t + 68), (CX + 10, t + 68)], fill=MID)
    # Mains effilees, trois doigts
    for side in (-1, 1):
        for finger in range(3):
            x = CX + side * (11 + finger)
            d.line([(x, t + 69), (x, t + 76 - finger)], fill=WHITE)
    # Veste cintree
    d.polygon([(CX - 11, t + 22), (CX + 11, t + 22), (CX + 7, t + 46), (CX + 9, t + 68), (CX - 9, t + 68), (CX - 7, t + 46)], fill=WHITE)
    # Cou et tete sans visage
    d.rectangle([CX - 2, t + 16, CX + 2, t + 22], fill=MID)
    d.ellipse([CX - 6, t, CX + 6, t + 18], fill=WHITE)
    return outlined(layer)


def details(layer):
    d = ImageDraw.Draw(layer)
    t = TOP
    # Ombre de la tete
    d.line([(CX + 5, t + 5), (CX + 5, t + 12)], fill=LIGHT)
    d.line([(CX + 4, t + 13), (CX + 2, t + 16)], fill=LIGHT)
    # Ombre de la veste
    d.polygon([(CX + 8, t + 23), (CX + 10, t + 23), (CX + 7, t + 46), (CX + 8, t + 67), (CX + 5, t + 67), (CX + 5, t + 46)], fill=LIGHT)
    # Chemise et revers
    d.polygon([(CX - 3, t + 22), (CX + 3, t + 22), (CX, t + 40)], fill=LIGHT)
    d.line([(CX - 4, t + 22), (CX, t + 41)], fill=DARK)
    d.line([(CX + 4, t + 22), (CX, t + 41)], fill=DARK)
    d.line([(CX, t + 41), (CX, t + 67)], fill=DARK)
    # Cravate fine
    d.rectangle([CX - 1, t + 22, CX + 1, t + 23], fill=ORANGE_D)
    d.polygon([(CX - 1, t + 25), (CX + 1, t + 25), (CX + 1, t + 37), (CX, t + 39), (CX - 1, t + 37)], fill=ORANGE)
    d.line([(CX + 1, t + 25), (CX + 1, t + 36)], fill=ORANGE_D)
    # Plis des jambes
    d.line([(CX - 3, t + 70), (CX - 3, t + 110)], fill=WHITE)
    d.line([(CX + 3, t + 70), (CX + 3, t + 110)], fill=DARK)


def main():
    sprite = new_layer()
    sprite.alpha_composite(wings())
    sprite.alpha_composite(body())
    details(sprite)
    sprite.save("outputs/ange_pixel_sprite.png")

    preview = Image.new("RGBA", (W, H), BACKGROUND)
    preview.alpha_composite(sprite)
    preview.resize((W * ZOOM, H * ZOOM), Image.NEAREST).save("outputs/ange_pixel_x3.png")


if __name__ == "__main__":
    main()
