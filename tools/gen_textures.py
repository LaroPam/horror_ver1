"""Generates the mod's textures. Run from the repo root: python3 tools/gen_textures.py

The entity texture layout mirrors TheOneModel.createBodyLayer(); keep the two in sync.
"""
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "cornerofeye", "textures")
rng = random.Random(1337)

SKIN = (198, 192, 180)
SKIN_SHADE = (156, 151, 142)
SKIN_DEEP = (112, 108, 103)
SKIN_LIGHT = (222, 217, 206)
VEIN = (118, 126, 140)
BRUISE = (128, 108, 112)
HAIR = (10, 9, 11)
HAIR_SHINE = (34, 32, 38)
BLACK = (4, 3, 4)
GUM = (78, 12, 16)
MOUTH = (30, 4, 6)
TOOTH = (226, 216, 182)
TOOTH_DARK = (176, 164, 128)
NAIL = (20, 18, 20)


def jitter(color, amount=8):
    d = rng.randint(-amount, amount)
    return tuple(max(0, min(255, c + d)) for c in color) + (255,)


def box_faces(u, v, w, h, d):
    """Standard Minecraft box UV: name -> (x, y, width, height)."""
    return {
        "top": (u + d, v, w, d),
        "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h),
        "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h),
        "back": (u + d + w + d, v + d, w, h),
    }


class Painter:
    def __init__(self, img, glow):
        self.img = img
        self.glow = glow

    def px(self, x, y, color, glow=None, skin_glow=True):
        self.img.putpixel((x, y), color if len(color) == 4 else color + (255,))
        if glow is not None:
            self.glow.putpixel((x, y), glow + (255,))
        elif skin_glow:
            # Faint glow of the pale skin, so its outline shows in the dark.
            self.glow.putpixel((x, y), (30, 30, 33, 255))

    def skin_rect(self, rect, base=SKIN, shade_edges=True, veins=0.02):
        x0, y0, w, h = rect
        for y in range(h):
            for x in range(w):
                c = base
                if shade_edges and (x == 0 or x == w - 1):
                    c = SKIN_SHADE
                r = rng.random()
                if r < veins:
                    c = VEIN
                elif r < veins + 0.012:
                    c = BRUISE
                self.px(x0 + x, y0 + y, jitter(c, 7))


def paint_entity():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    glow = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    p = Painter(img, glow)

    # --------------------------------------------------------------- head 7x10x7
    head = box_faces(0, 0, 7, 10, 7)
    for name, rect in head.items():
        p.skin_rect(rect, SKIN if name == "front" else SKIN_SHADE, veins=0.015)
    fx, fy, _, _ = head["front"]
    no_glow = (0, 0, 0)
    tooth_glow = (120, 114, 96)
    # A long, smooth, pale face. Brow shadow over two black pits.
    for x in (1, 2, 4, 5):
        p.px(fx + x, fy + 2, jitter(SKIN_SHADE, 4))
    for (x, y) in [(1, 3), (2, 3), (4, 3), (5, 3), (1, 4), (2, 4), (4, 4), (5, 4)]:
        p.px(fx + x, fy + y, BLACK + (255,), glow=no_glow)
    # Pinpricks of light that don't look quite the same way.
    p.px(fx + 1, fy + 4, (250, 250, 245, 255), glow=(255, 255, 250))
    p.px(fx + 5, fy + 3, (250, 250, 245, 255), glow=(255, 255, 250))
    for x in (1, 2, 4, 5):
        p.px(fx + x, fy + 5, jitter(SKIN_DEEP, 4))
    # Black tears.
    for y in (5, 6):
        p.px(fx + 1, fy + y, BLACK + (255,), glow=no_glow)
    p.px(fx + 5, fy + 5, BLACK + (255,), glow=no_glow)
    # A grin wider than the face: corners pull up, thin teeth top and bottom.
    p.px(fx + 0, fy + 6, BLACK + (255,), glow=no_glow)
    p.px(fx + 6, fy + 6, BLACK + (255,), glow=no_glow)
    for x in range(7):
        upper = TOOTH if x in (1, 3, 5) else BLACK
        p.px(fx + x, fy + 7, upper + (255,), glow=tooth_glow if upper == TOOTH else no_glow)
        p.px(fx + x, fy + 8, BLACK + (255,), glow=no_glow)
        lower = TOOTH if x in (0, 2, 4, 6) else BLACK
        p.px(fx + x, fy + 9, lower + (255,), glow=tooth_glow if lower == TOOTH else no_glow)
    # The grin keeps going around the sides of the head, almost to the ears.
    rx, ry, _, _ = head["right"]
    lx, ly, _, _ = head["left"]
    for (x, y) in [(6, 8), (5, 8), (4, 7), (3, 7), (2, 6), (6, 7), (5, 7)]:
        p.px(rx + x, ry + y, BLACK + (255,), glow=no_glow)
        p.px(lx + 6 - x, ly + y, BLACK + (255,), glow=no_glow)
    # Long, thin, wet hair over the top and down the back; a few strands down the sides.
    for name in ("top", "back", "right", "left"):
        x0, y0, w, h = head[name]
        for x in range(w):
            if name in ("right", "left") and (x >= 4 if name == "right" else x <= 2):
                continue
            if name == "top" or name == "back" or rng.random() < 0.7:
                length = h if name in ("top", "back") else rng.randint(2, h)
                for y in range(length):
                    if rng.random() < 0.85:
                        p.px(x0 + x, y0 + y, jitter(HAIR_SHINE if rng.random() < 0.15 else HAIR, 3), glow=no_glow)
    p.px(fx + 0, fy + 0, jitter(HAIR, 3), glow=no_glow)
    p.px(fx + 6, fy + 0, jitter(HAIR, 3), glow=no_glow)
    p.px(fx + 6, fy + 1, jitter(HAIR, 3), glow=no_glow)
    # Underside of the head is the roof of the mouth.
    bx, by, bw, bh = head["bottom"]
    for y in range(bh):
        for x in range(bw):
            edge = y == 0 or x in (0, bw - 1)
            p.px(bx + x, by + y, (TOOTH if edge and (x + y) % 2 == 0 else GUM if edge else MOUTH) + (255,), glow=no_glow)

    # ----------------------------------------------------------------- jaw 6x2x6 (below the head, at v=18)
    jaw = box_faces(0, 18, 6, 2, 6)
    for name, rect in jaw.items():
        p.skin_rect(rect, SKIN_SHADE, veins=0.0)
    jx, jy, _, _ = jaw["front"]
    for x in range(6):
        p.px(jx + x, jy, (TOOTH if x % 2 else BLACK) + (255,), glow=(120, 114, 96) if x % 2 else (0, 0, 0))
        p.px(jx + x, jy + 1, BLACK + (255,), glow=(0, 0, 0))
    tx, ty, tw, th = jaw["top"]
    for y in range(th):
        for x in range(tw):
            edge = x in (0, tw - 1) or y == th - 1
            p.px(tx + x, ty + y, (TOOTH if edge and (x + y) % 2 == 0 else GUM if edge else MOUTH) + (255,), glow=(0, 0, 0))

    # ---------------------------------------------------------------- torso 7x16x4
    torso = box_faces(28, 0, 7, 16, 4)
    for name, rect in torso.items():
        p.skin_rect(rect, SKIN if name == "front" else SKIN_SHADE, veins=0.025)
    tx, ty, _, _ = torso["front"]
    # Collarbones, then ribs you could count.
    for x in range(7):
        p.px(tx + x, ty + 1, jitter(SKIN_LIGHT, 4))
    for row in (3, 5, 7, 9):
        for x in range(7):
            if x != 3:
                p.px(tx + x, ty + row, jitter(SKIN_DEEP, 5))
                p.px(tx + x, ty + row - 1, jitter(SKIN_LIGHT, 4))
    for y in range(2, 10):
        p.px(tx + 3, ty + y, jitter(SKIN_LIGHT, 4))
    # A hollow where the stomach should be.
    for y in range(11, 16):
        for x in range(1, 6):
            p.px(tx + x, ty + y, jitter(SKIN_DEEP if 1 < x < 5 else SKIN_SHADE, 5))
    # Something black ran down from the mouth.
    for (x, length) in [(2, 5), (4, 3), (5, 7)]:
        for y in range(length):
            p.px(tx + x, ty + y, BLACK + (255,), glow=(0, 0, 0))
    # Spine and shoulder blades on the back.
    bx, by, _, _ = torso["back"]
    for y in range(16):
        p.px(bx + 3, by + y, jitter(SKIN_LIGHT if y % 2 else SKIN_DEEP, 4))
    for (x, y) in [(1, 2), (1, 3), (1, 4), (5, 2), (5, 3), (5, 4), (0, 5), (6, 5)]:
        p.px(bx + x, by + y, jitter(SKIN_DEEP, 4))

    # --------------------------------------------------------------------- limbs
    def limb(u, v, w, h, d, dark_top=0, dark_bottom=0, hand=False):
        faces = box_faces(u, v, w, h, d)
        for name, rect in faces.items():
            p.skin_rect(rect, SKIN_SHADE if name in ("right", "left", "back") else SKIN, veins=0.03)
        for name in ("right", "front", "left", "back"):
            x0, y0, fw, fh = faces[name]
            for x in range(fw):
                for y in range(dark_top):
                    p.px(x0 + x, y0 + y, jitter(SKIN_DEEP, 5))
                for y in range(fh - dark_bottom, fh):
                    p.px(x0 + x, y0 + y, jitter(SKIN_DEEP if not hand else HAIR, 5), glow=None if not hand else (0, 0, 0))
        if hand:
            x0, y0, fw, fh = faces["bottom"]
            for y in range(fh):
                for x in range(fw):
                    p.px(x0 + x, y0 + y, jitter(NAIL, 3), glow=(0, 0, 0))

    limb(52, 0, 3, 13, 3, dark_top=1, dark_bottom=2)   # right thigh
    limb(64, 0, 3, 13, 3, dark_top=1, dark_bottom=2)   # left thigh
    limb(76, 0, 3, 13, 3, dark_top=2, dark_bottom=1)   # right shin
    limb(88, 0, 3, 13, 3, dark_top=2, dark_bottom=1)   # left shin
    limb(100, 0, 2, 17, 2, dark_top=1, dark_bottom=2)  # right upper arm
    limb(108, 0, 2, 17, 2, dark_top=1, dark_bottom=2)  # left upper arm
    limb(116, 0, 2, 17, 2, dark_top=2, dark_bottom=1)  # right forearm
    limb(100, 20, 2, 17, 2, dark_top=2, dark_bottom=1)  # left forearm
    # Fingers: bone-pale knuckles fading into long black nails.
    fingers = box_faces(108, 20, 1, 8, 1)
    for name, (x0, y0, w, h) in fingers.items():
        for y in range(h):
            for x in range(w):
                c = SKIN_SHADE if y < 3 else SKIN_DEEP if y < 5 else NAIL
                p.px(x0 + x, y0 + y, jitter(c, 4), glow=None if y < 5 else (0, 0, 0))

    img.save(os.path.join(ROOT, "entity", "the_one.png"))
    glow.save(os.path.join(ROOT, "entity", "the_one_glow.png"))


def paint_jumpscare():
    size = 64
    img = Image.new("RGBA", (size, size), (0, 0, 0, 255))
    d = ImageDraw.Draw(img)
    # Pale, too-long face filling the frame.
    for y in range(size):
        for x in range(size):
            nx, ny = (x - 32) / 23.0, (y - 31) / 29.0
            r = nx * nx + ny * ny
            if r < 1.0:
                base = SKIN_LIGHT if r < 0.35 else SKIN if r < 0.7 else SKIN_SHADE if r < 0.9 else SKIN_DEEP
                img.putpixel((x, y), jitter(base, 9))
    # Eye pits, uneven, with dark rings.
    for (cx, cy, rx, ry) in [(21, 23, 7, 8), (43, 22, 6, 9)]:
        d.ellipse((cx - rx - 2, cy - ry - 2, cx + rx + 2, cy + ry + 2), fill=SKIN_DEEP + (255,))
        d.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=BLACK + (255,))
    img.putpixel((19, 24), (255, 255, 250, 255))
    img.putpixel((20, 24), (255, 255, 250, 255))
    img.putpixel((45, 20), (255, 255, 250, 255))
    img.putpixel((45, 21), (255, 255, 250, 255))
    # Black tears down to the mouth.
    for (x, y0, y1) in [(18, 30, 42), (23, 31, 37), (41, 30, 40), (46, 30, 44)]:
        for y in range(y0, y1):
            img.putpixel((x + (1 if rng.random() < 0.2 else 0), y), BLACK + (255,))
    # The grin.
    for x in range(5, 60):
        t = (x - 32) / 27.0
        top = 40 + 4 * (1 - t * t) - 8 * t * t
        bottom = 47 + 9 * (1 - t * t) - 8 * t * t
        for y in range(int(top), int(bottom) + 1):
            depth = (y - top) / max(1.0, bottom - top)
            color = GUM if y in (int(top), int(bottom)) else MOUTH if depth > 0.45 and abs(t) < 0.6 else BLACK
            img.putpixel((x, y), color + (255,))
        # Teeth: thin, pointed, too many.
        if x % 3 != 0:
            length = rng.randint(2, 4)
            for i in range(length):
                if x % 3 == 1 or i < length - 1:
                    img.putpixel((x, int(top) + 1 + i), (TOOTH if i < length - 1 else TOOTH_DARK) + (255,))
            length = rng.randint(2, 3)
            for i in range(length):
                if x % 3 == 2 or i < length - 1:
                    img.putpixel((x, int(bottom) - 1 - i), (TOOTH if i < length - 1 else TOOTH_DARK) + (255,))
    # Wet hair hanging over the forehead and down the sides.
    for x in range(4, 60):
        if rng.random() < 0.75:
            dist = abs(x - 32)
            length = rng.randint(2, 8) if dist < 14 else rng.randint(14, 60)
            for y in range(0, length):
                if rng.random() < 0.9:
                    img.putpixel((x, y), jitter(HAIR if rng.random() > 0.1 else HAIR_SHINE, 3))
    img = img.rotate(-14, resample=Image.NEAREST, fillcolor=(0, 0, 0, 255))
    # Darken towards the frame edge.
    for y in range(size):
        for x in range(size):
            r = math.hypot(x - 32, y - 32) / 45.0
            k = max(0.0, min(1.0, 1.25 - r))
            c = img.getpixel((x, y))
            img.putpixel((x, y), (int(c[0] * k), int(c[1] * k), int(c[2] * k), 255))
    img.save(os.path.join(ROOT, "gui", "jumpscare.png"))


def paint_vignette():
    size = 256
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y in range(size):
        for x in range(size):
            r = math.hypot((x - 127.5) / 127.5, (y - 127.5) / 127.5)
            a = max(0.0, min(1.0, (r - 0.45) / 0.85))
            img.putpixel((x, y), (0, 0, 0, int(255 * a ** 1.5)))
    img.save(os.path.join(ROOT, "gui", "dread_vignette.png"))


def paint_scrap():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    core = (16, 13, 22)
    edge = (58, 40, 84)
    glint = (128, 104, 170)
    shape = [
        "................",
        "...........#....",
        "..........##....",
        "........####..#.",
        ".......#####....",
        "......######....",
        ".....#######....",
        "....#######.....",
        "...########.....",
        "...#######......",
        "..#######.......",
        "..######........",
        ".#####..........",
        ".###............",
        "##..............",
        "................",
    ]
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch != "#":
                continue
            neighbours = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                             if 0 <= x + dx < 16 and 0 <= y + dy < 16 and shape[y + dy][x + dx] == "#")
            color = edge if neighbours < 4 else core
            img.putpixel((x, y), jitter(color, 6))
    for (x, y) in [(9, 4), (6, 8), (4, 11), (12, 3)]:
        img.putpixel((x, y), glint + (255,))
    img.save(os.path.join(ROOT, "item", "shadow_scrap.png"))


def paint_lantern():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    iron = (54, 54, 62)
    iron_light = (96, 96, 108)
    pane = (164, 120, 220)
    core = (244, 232, 255)
    shard = (200, 150, 245)
    for (x, y) in [(6, 1), (7, 1), (8, 1), (9, 1), (5, 2), (10, 2), (5, 3), (10, 3)]:
        img.putpixel((x, y), iron_light + (255,))
    for x in range(4, 12):
        img.putpixel((x, 4), iron + (255,))
        img.putpixel((x, 5), iron_light + (255,) if x in (4, 11) else iron + (255,))
    for y in range(6, 13):
        for x in range(4, 12):
            if x in (4, 11):
                img.putpixel((x, y), iron + (255,))
            elif 6 <= x <= 9 and 8 <= y <= 10:
                img.putpixel((x, y), core + (255,))
            else:
                img.putpixel((x, y), jitter(pane, 10))
    for x in range(4, 12):
        img.putpixel((x, 13), iron_light + (255,))
        img.putpixel((x, 14), iron + (255,))
    for (x, y) in [(3, 7), (2, 8), (12, 9), (13, 8), (3, 11), (12, 12)]:
        img.putpixel((x, y), shard + (255,))
    img.save(os.path.join(ROOT, "item", "lantern_of_revelation.png"))


if __name__ == "__main__":
    for sub in ("entity", "gui", "item"):
        os.makedirs(os.path.join(ROOT, sub), exist_ok=True)
    paint_entity()
    paint_jumpscare()
    paint_vignette()
    paint_scrap()
    paint_lantern()
    print("textures written to", os.path.normpath(ROOT))
