"""Cuts a render shot against lime concrete out of its background: python3 tools/chroma_key.py in.png out.png

Promo renders are in-game screenshots of the real model, posed with
/summon cornerofeye:the_one ~ ~ ~ {NoAI:1b,Stage:3,Days:9} in a lime concrete room, with the client started
with -Dcornerofeye.alwaysVisible=true and the HUD hidden (F1).
"""
import sys

from PIL import Image


def key(src, dst):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            # Green, including the dark green of corners and shadows; the model has no green in it.
            # The darkest corners are barely green at all: almost black, but never bluish or reddish.
            if (g > 12 and g > r * 1.15 and g > b * 1.6) or (max(r, g, b) <= 24 and g >= r and g > b * 1.5):
                px[x, y] = (0, 0, 0, 0)
    drop_specks(im)
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    im.save(dst, optimize=True)


def drop_specks(im, keep=0.01):
    """Clears stray bits (particles, dust) that aren't connected to anything big."""
    px = im.load()
    w, h = im.size
    seen = bytearray(w * h)
    for start in range(w * h):
        if seen[start] or px[start % w, start // w][3] == 0:
            continue
        part, stack = [], [start]
        seen[start] = 1
        while stack:
            i = stack.pop()
            part.append(i)
            x, y = i % w, i // w
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                j = ny * w + nx
                if 0 <= nx < w and 0 <= ny < h and not seen[j] and px[nx, ny][3]:
                    seen[j] = 1
                    stack.append(j)
        if len(part) < keep * w * h:
            for i in part:
                px[i % w, i // w] = (0, 0, 0, 0)


if __name__ == "__main__":
    key(sys.argv[1], sys.argv[2])
