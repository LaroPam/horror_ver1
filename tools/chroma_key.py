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
            if g > r * 1.25 and g > b * 1.25 and g > 40:
                px[x, y] = (0, 0, 0, 0)
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)
    im.save(dst, optimize=True)


if __name__ == "__main__":
    key(sys.argv[1], sys.argv[2])
