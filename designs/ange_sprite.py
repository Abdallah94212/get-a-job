"""Sprite de gameplay de l'ange : meme design que ange_pixel.py, redessine a sa vraie taille."""
import math
from PIL import Image, ImageDraw

W, H = 124, 96
CX = W // 2
TOP = 28  # haut de la tete
ZOOM = 6
WING_SCALE = 0.43
FRAMES = 12
IDLE_FRAMES = 120
SLOW_BLINK = (1, 2, 3, 4, 4, 4, 3, 2, 1)
BLINK_STARTS = {"upper": 6, "middle": 52, "lower": 84}  # ecarts irreguliers de 3 a 5 secondes
FLOAT = 2  # marge pour que le corps flotte sans sortir du cadre

OUTLINE = (20, 20, 26, 255)
WHITE = (244, 244, 238, 255)
PALE = (222, 222, 218, 255)
LIGHT = (190, 190, 194, 255)
MID = (136, 136, 142, 255)
DARK = (84, 84, 92, 255)
ORANGE = (255, 122, 18, 255)
ORANGE_D = (196, 80, 10, 255)
STAGE = (62, 62, 66, 255)
BLOOD_DARK = (70, 0, 8, 255)
BLOOD_BRIGHT = (255, 16, 24, 255)
YELLOW = (238, 255, 20, 255)
COBALT_BLUE = (20, 26, 255, 255)
# Palettes de l'ult, relevees sur l'ancien bug de couleurs, sans son rose pale
COBALT = {OUTLINE: YELLOW, MID: COBALT_BLUE, PALE: MID, LIGHT: WHITE, DARK: (190, 194, 255, 255)}
BLACK_YELLOW = {OUTLINE: (136, 142, 255, 255), MID: (238, 255, 136, 255), PALE: OUTLINE, LIGHT: WHITE,
                DARK: (190, 194, 255, 255)}

WISPS = (1.0, 0.8, 1.1, 0.88, 1.05, 0.8)


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


def bezier(p0, p1, p2, t):
    u = 1 - t
    return (u * u * p0[0] + 2 * u * t * p1[0] + t * t * p2[0],
            u * u * p0[1] + 2 * u * t * p1[1] + t * t * p2[1])


def polar(x, y, angle, length):
    rad = math.radians(angle)
    return x + math.cos(rad) * length, y + math.sin(rad) * length


def feather(draw, x, y, angle, length, width, fill, edge, vein):
    """Plume souple : bord ombre, nervure centrale et barbes grises sur les plus longues."""
    near_a = polar(*polar(x, y, angle, length * 0.3), angle + 90, width / 2)
    near_b = polar(*polar(x, y, angle, length * 0.3), angle - 90, width / 2)
    far_a = polar(*polar(x, y, angle, length * 0.78), angle + 90, width / 2)
    far_b = polar(*polar(x, y, angle, length * 0.78), angle - 90, width / 2)
    tip = polar(x, y, angle, length)
    draw.polygon([(x, y), near_a, far_a, tip, far_b, near_b], fill=fill)
    draw.line([near_b, far_b, tip], fill=edge)
    if length >= 5:
        draw.line([polar(x, y, angle, length * 0.2), polar(x, y, angle, length * 0.9)], fill=vein)
    if length >= 14:
        start = polar(x, y, angle, length * 0.6)
        draw.line([start, polar(*start, angle + 140, width * 0.6)], fill=vein)


EYELIDS = (
    ("..DDDDD..", ".WWOOOWW.", "WWOOKOOWW", ".WWOOOWW.", "..LLLLL.."),  # ouvert
    ("..PPPPP..", ".DDDDDDD.", "WWOOKOOWW", ".WWOOOWW.", "..LLLLL.."),
    ("..PPPPP..", ".PPPPPPP.", "DDDDDDDDD", ".WWOOOWW.", "..LLLLL.."),
    ("..PPPPP..", ".PPPPPPP.", "PPPPPPPPP", ".DDDDDDD.", "..LLLLL.."),
    ("..PPPPP..", ".PPPPPPP.", "PPPPPPPPP", ".PPPPPPP.", "..DDDDD.."),  # ferme
)


def eye(layer, x, y, lid=0):
    """Oeil de 9x5 pixels : blanc, iris orange, pupille. lid va de 0 (ouvert) a 4 (ferme)."""
    px = layer.load()
    colors = {"D": DARK, "L": LIGHT, "P": PALE, "W": WHITE, "O": ORANGE, "K": OUTLINE}
    for j, row in enumerate(EYELIDS[lid]):
        for i, c in enumerate(row):
            if c != ".":
                px[x - 4 + i, y - 2 + j] = colors[c]


def rotated(point, center, degrees):
    rad = math.radians(degrees)
    dx, dy = point[0] - center[0], point[1] - center[1]
    return (center[0] + dx * math.cos(rad) - dy * math.sin(rad),
            center[1] + dx * math.sin(rad) + dy * math.cos(rad))


def left_wing(p0, p1, p2, hang, reach, longest, eye_at, tilt=0.0, lid=0):
    """Aile gauche. tilt fait pivoter toute l'aile autour de son attache : le battement."""
    p1 = rotated(p1, p0, tilt)
    p2 = rotated(p2, p0, tilt)
    hang += tilt
    reach += tilt
    layer = new_layer()
    draw = ImageDraw.Draw(layer)
    rows = [
        (10, 0.10, 1.00, longest, 4, PALE, MID, MID),
        (8, 0.03, 0.88, longest * 0.62, 4, WHITE, MID, PALE),
        (6, 0.00, 0.70, longest * 0.34, 4, WHITE, LIGHT, WHITE),
    ]
    for count, start, stop, length, width, fill, edge, vein in rows:
        for i in range(count):
            t = start + (stop - start) * (count - 1 - i) / (count - 1)
            x, y = bezier(p0, p1, p2, t)
            k = (t - start) / (stop - start)
            angle = hang + (reach - hang) * k ** 1.3
            feather(draw, x, y, angle, length * (0.65 + 0.35 * k) * WISPS[i % len(WISPS)], width, fill, edge, vein)
    layer = outlined(layer)
    x, y = bezier(p0, p1, p2, eye_at)
    angle = hang + (reach - hang) * eye_at ** 1.3
    eye(layer, *[round(v) for v in polar(x, y, angle, longest * 0.32)], lid)
    return layer


def mirrored(layer):
    both = layer.copy()
    both.alpha_composite(layer.transpose(Image.FLIP_LEFT_RIGHT))
    return both


def wing_specs(beat):
    """Les trois ailes gauches, du fond vers l'avant : (nom, parametres, inclinaison)."""
    t = TOP

    def at(dx, dy):
        return CX + dx * WING_SCALE, t + 16 + dy * WING_SCALE

    return [
        ("upper", (at(-6, -6), at(-30, -50), at(-70, -54), 146, 216, 44 * WING_SCALE, 0.5), -6 * beat),
        ("middle", (at(-7, 0), at(-36, -26), at(-76, -4), 88, 150, 58 * WING_SCALE, 0.72), -4 * beat),
        ("lower", (at(-7, 20), at(-28, 14), at(-54, 38), 86, 128, 40 * WING_SCALE, 0.6), -3 * beat),
    ]


def wings(beat=0.0, lids=None):
    """beat va de -1 a 1 : les ailes montent ou descendent. lids donne la paupiere de chaque paire."""
    lids = lids or {}
    layer = new_layer()
    for name, spec, tilt in wing_specs(beat):
        layer.alpha_composite(mirrored(left_wing(*spec, tilt, lids.get(name, 0))))
    return layer


def eye_centers(beat):
    """Le centre des six yeux, calcule comme dans left_wing."""
    centers = []
    for _, (p0, p1, p2, hang, reach, longest, eye_at), tilt in wing_specs(beat):
        p1, p2 = rotated(p1, p0, tilt), rotated(p2, p0, tilt)
        hang, reach = hang + tilt, reach + tilt
        x, y = bezier(p0, p1, p2, eye_at)
        angle = hang + (reach - hang) * eye_at ** 1.3
        x, y = [round(v) for v in polar(x, y, angle, longest * 0.32)]
        centers += [(x, y), (W - 1 - x, y)]
    return centers


def body():
    layer = new_layer()
    d = ImageDraw.Draw(layer)
    t = TOP
    # Jambes et chaussures
    d.polygon([(CX - 4, t + 35), (CX - 1, t + 35), (CX - 2, t + 58), (CX - 3, t + 58)], fill=LIGHT)
    d.polygon([(CX + 1, t + 35), (CX + 4, t + 35), (CX + 3, t + 58), (CX + 2, t + 58)], fill=MID)
    d.line([(CX - 5, t + 59), (CX - 2, t + 59)], fill=WHITE)
    d.line([(CX + 2, t + 59), (CX + 5, t + 59)], fill=LIGHT)
    # Bras longs et mains
    d.polygon([(CX - 6, t + 12), (CX - 5, t + 12), (CX - 6, t + 36), (CX - 8, t + 36)], fill=LIGHT)
    d.polygon([(CX + 5, t + 12), (CX + 6, t + 12), (CX + 8, t + 36), (CX + 6, t + 36)], fill=MID)
    d.line([(CX - 7, t + 37), (CX - 7, t + 40)], fill=WHITE)
    d.line([(CX + 7, t + 37), (CX + 7, t + 40)], fill=WHITE)
    # Veste
    d.polygon([(CX - 6, t + 11), (CX + 6, t + 11), (CX + 4, t + 24), (CX + 5, t + 35), (CX - 5, t + 35), (CX - 4, t + 24)], fill=WHITE)
    # Cou et tete sans visage
    d.rectangle([CX - 1, t + 8, CX + 1, t + 11], fill=MID)
    d.ellipse([CX - 3, t, CX + 3, t + 9], fill=WHITE)
    return outlined(layer)


def details(layer):
    d = ImageDraw.Draw(layer)
    t = TOP
    # Ombres
    d.line([(CX + 3, t + 3), (CX + 3, t + 6)], fill=LIGHT)
    d.line([(CX + 4, t + 13), (CX + 3, t + 24)], fill=LIGHT)
    d.line([(CX + 3, t + 25), (CX + 4, t + 34)], fill=LIGHT)
    # Revers et cravate : la seule couleur
    d.line([(CX - 2, t + 11), (CX, t + 21)], fill=DARK)
    d.line([(CX + 2, t + 11), (CX, t + 21)], fill=DARK)
    d.line([(CX, t + 22), (CX, t + 34)], fill=DARK)
    d.line([(CX, t + 11), (CX, t + 19)], fill=ORANGE)
    d.point((CX, t + 11), fill=ORANGE_D)


def frame(beat, bob, lids=None):
    sprite = new_layer()
    sprite.alpha_composite(wings(beat, lids))
    sprite.alpha_composite(body())
    details(sprite)
    moved = new_layer()
    moved.alpha_composite(sprite, (0, FLOAT - bob))
    return moved


def anaglyph(image, shift):
    """Effet lunettes 3D : une copie rouge a gauche, une bleue a droite, du blanc la ou elles se superposent."""
    src = image.load()
    result = new_layer()
    dst = result.load()
    for y in range(H):
        for x in range(W):
            lx, rx = x + shift, x - shift
            left = src[lx, y] if 0 <= lx < W else (0, 0, 0, 0)
            right = src[rx, y] if 0 <= rx < W else (0, 0, 0, 0)
            if not left[3] and not right[3]:
                continue
            red = left[0] if left[3] else 0
            blue = right[2] if right[3] else 0
            if red >= 120 and blue >= 120:
                # Deux tons clairs : du gris neutre, sinon le melange tire vers le rose
                shade = min(red, blue)
                dst[x, y] = (shade, shade, shade, 255)
            else:
                dst[x, y] = (red, 0, blue, 255)
    return result


def psychedelic(clean, shift, palette):
    """Ult : les couleurs de la palette, et un dedoublement rouge et bleu par-dessus."""
    split = anaglyph(clean, shift).load()
    src = clean.load()
    result = new_layer()
    dst = result.load()
    for y in range(H):
        for x in range(W):
            red, green, blue, alpha = split[x, y]
            if not alpha:
                continue
            on_body = abs(x - CX) <= 15 and src[x, y][3]
            if not green and max(red, blue) >= 120 and not on_body:
                dst[x, y] = (red, 0, blue, 255)
            elif src[x, y][3]:
                dst[x, y] = palette.get(src[x, y], src[x, y])
            else:
                dst[x, y] = (red, green, blue, 255)
    return result


def restore_tie(image, clean):
    """La cravate reste toujours orange."""
    src = clean.load()
    dst = image.load()
    for y in range(H):
        for x in range(W):
            if x in range(CX - 1, CX + 2) and src[x, y] in (ORANGE, ORANGE_D):
                dst[x, y] = src[x, y]


def restore_eyes(bugged, beat, bob):
    """Remet les vrais yeux par-dessus l'effet, cernes de noir pour qu'ils ressortent."""
    clean = frame(beat, bob).load()
    overlay = new_layer()
    for x, y in eye_centers(beat):
        eye(overlay, x, y + FLOAT - bob)
    src = overlay.load()
    dst = bugged.load()
    visible = set()
    for y in range(H):
        for x in range(W):
            if src[x, y][3] and clean[x, y] == src[x, y]:
                dst[x, y] = src[x, y]
                visible.add((x, y))
    for x, y in visible:
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) not in visible and 0 <= nx < W and 0 <= ny < H:
                dst[nx, ny] = OUTLINE


def tears_at(draw, centers, step):
    """Deux filets de sang par oeil, vifs et cernes de sombre, qui s'allongent puis lachent une goutte."""
    for n, (x, y) in enumerate(centers):
        for side, offset in ((-2, 0), (2, 5)):
            phase = (step + n * 2 + offset) % FRAMES
            end = y + 4 + phase
            cx = x + side
            draw.line([(cx - 1, y), (cx - 1, end)], fill=BLOOD_DARK)
            draw.line([(cx + 1, y), (cx + 1, end)], fill=BLOOD_DARK)
            draw.line([(cx, y), (cx, end + 1)], fill=BLOOD_BRIGHT)
            if phase >= 8:
                drop = end + (phase - 7) * 2 + 1
                draw.rectangle([cx, drop, cx + 1, drop + 1], fill=BLOOD_BRIGHT)


def blood_tears(layer, beat, bob, step):
    centers = [(x, y + FLOAT - bob + 4) for x, y in eye_centers(beat)]
    tears_at(ImageDraw.Draw(layer), centers, step)


def export(frames, name, duration):
    """Une planche transparente pour le jeu et un GIF pour voir l'animation."""
    sheet = Image.new("RGBA", (W * len(frames), H), (0, 0, 0, 0))
    gif = []
    for i, image in enumerate(frames):
        sheet.alpha_composite(image, (W * i, 0))
        shot = Image.new("RGBA", (W, H), STAGE)
        shot.alpha_composite(image)
        big = shot.convert("RGB").resize((W * 4, H * 4), Image.NEAREST)
        gif.append(big.quantize(colors=200, dither=Image.Dither.NONE))
    sheet.save(f"outputs/{name}_sheet.png")
    gif[0].save(f"outputs/{name}.gif", save_all=True, append_images=gif[1:], duration=duration, loop=0)


def main():
    def motion(i):
        phase = 2 * math.pi * i / FRAMES
        return math.sin(phase), round(math.sin(phase) + 1)

    def lids_at(i):
        """Clignement lent : chaque paire a son tour, puis un temps mort ou tout le monde regarde."""
        lids = {}
        for name, start in BLINK_STARTS.items():
            if 0 <= i - start < len(SLOW_BLINK):
                lids[name] = SLOW_BLINK[i - start]
        return lids

    # Etat normal : battement lent, clignements lents. Dix battements pour une boucle
    idle = [frame(*motion(i), lids_at(i)) for i in range(IDLE_FRAMES)]
    export(idle, "ange_sprite_idle", 120)

    # Ult, vie basse : cobalt puis noir et jaune, dedoublement 3D, cravate et yeux intacts, larmes de sang
    ult = []
    for i in range(FRAMES):
        beat, bob = motion(i)
        clean = frame(beat, bob)
        palette = BLACK_YELLOW if i % 4 >= 2 else COBALT
        image = psychedelic(clean, 2 + round(math.sin(4 * math.pi * i / FRAMES)), palette)
        restore_tie(image, clean)
        restore_eyes(image, beat, bob)
        blood_tears(image, beat, bob, i)
        ult.append(image)
    export(ult, "ange_sprite_ult", 120)

    sprite = frame(0.0, 0)
    sprite.save("outputs/ange_sprite.png")
    ult[5].save("outputs/ange_sprite_ult.png")
    for image, name in ((sprite, "ange_sprite_x6"), (ult[5], "ange_sprite_ult_x6")):
        preview = Image.new("RGBA", (W, H), STAGE)
        preview.alpha_composite(image)
        preview.resize((W * ZOOM, H * ZOOM), Image.NEAREST).save(f"outputs/{name}.png")


if __name__ == "__main__":
    main()
