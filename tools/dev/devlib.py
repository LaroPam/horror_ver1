"""Helpers for driving the dev client from scripts: RCON commands, keys, mouse, screenshots.

The client runs on a virtual display (Xvfb :99); screenshots are grabbed from it with ffmpeg.
Shots go to build/devshots/ (ignored by git). Typical use from the repo root:

    python3 -c "import sys; sys.path.insert(0, 'tools/dev'); from devlib import *; rcon('time set noon'); shot('noon')"
"""
import os
import subprocess
import time

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
OUT = os.path.join(ROOT, "build", "devshots")
ENV = {"DISPLAY": ":99", "PATH": "/usr/bin:/bin"}
os.makedirs(OUT, exist_ok=True)


def rcon(*cmds):
    return subprocess.run(["python3", os.path.join(os.path.dirname(__file__), "rcon.py"), *cmds],
                          capture_output=True, text=True).stdout


def shot(name):
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "x11grab", "-video_size", "1280x720", "-i", ":99",
                    "-frames:v", "1", os.path.join(OUT, name + ".png")], env=ENV)
    return os.path.join(OUT, name + ".png")


def grid(names, out, cols=3):
    from PIL import Image
    ims = [Image.open(os.path.join(OUT, n + ".png")).resize((427, 240)) for n in names]
    rows = (len(ims) + cols - 1) // cols
    canvas = Image.new("RGB", (427 * cols, 240 * rows))
    for k, im in enumerate(ims):
        canvas.paste(im, ((k % cols) * 427, (k // cols) * 240))
    canvas.save(os.path.join(OUT, out + ".png"))
    return os.path.join(OUT, out + ".png")


def key(name):
    subprocess.run(["xdotool", "key", name], env=ENV)


def mouse(x, y):
    subprocess.run(["xdotool", "mousemove", str(x), str(y)], env=ENV)


def click(button=1):
    subprocess.run(["xdotool", "click", str(button)], env=ENV)


def studio():
    """A closed lime concrete room at x 300..340, z 40..80 lit by light blocks: a green screen for renders."""
    rcon("gamemode spectator Dev", "tp Dev 320 -55 60 0 0")
    time.sleep(3)
    rcon("fill 300 -61 40 340 -61 80 lime_concrete",
         "fill 300 -60 40 300 -40 80 lime_concrete", "fill 340 -60 40 340 -40 80 lime_concrete",
         "fill 300 -60 40 340 -40 40 lime_concrete", "fill 300 -60 80 340 -40 80 lime_concrete",
         "fill 301 -60 41 339 -41 79 air", "fill 300 -40 40 340 -40 80 lime_concrete",
         "fill 302 -42 42 338 -42 78 minecraft:light[level=15] replace air",
         "fill 302 -50 42 338 -50 78 minecraft:light[level=15] replace air",
         "time set noon", "cornerofeye stage 0", "weather clear")


def pose(x, y, z, yaw, nbt):
    """Puts one posed, AI-less copy of it in the world (the client needs -Dcornerofeye.alwaysVisible=true)."""
    rcon("kill @e[type=cornerofeye:the_one]")
    time.sleep(0.5)
    return rcon(f"summon cornerofeye:the_one {x} {y} {z} {{NoAI:1b,Silent:1b,PersistenceRequired:1b,Rotation:[{yaw}f,0f],{nbt}}}")


def merge(nbt):
    return rcon(f"data merge entity @e[type=cornerofeye:the_one,limit=1] {{{nbt}}}")
