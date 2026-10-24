"""Clears an open field, builds whatever the extra commands say, puts the player there and lets it hunt them.

    python3 tools/dev/hunt_test.py <name> <player x y z yaw pitch> <monster x y z> [seconds] [commands...]

The field spans x 460..510, z -100..-40, grass at y -62 (you stand on y -61). Prints where it is every two
seconds and records build/devshots/<name>.mp4. VIEW=front films the player from the front (F5 twice), to see
what is happening right under or behind them. TURN="8:0:10;12:180:40" turns the player to yaw 0, pitch 10
at 8 s and back at 12 s (to look away and back). AFTER="cmd;cmd" runs once the player is in place (to sit
them in a boat, say).
"""
import os
import sys
import time

sys.path.insert(0, "tools/dev")
from devlib import key, rcon, record  # noqa: E402

name = sys.argv[1]
px, py, pz, yaw, pitch = (float(v) for v in sys.argv[2:7])
mx, my, mz = (float(v) for v in sys.argv[7:10])
seconds = int(sys.argv[10]) if len(sys.argv) > 10 else 30
extra = sys.argv[11:]
rcon(f"tp Dev {px} -40 {pz} 0 0")
time.sleep(2)
rcon("kill @e[type=cornerofeye:the_one]", "kill @e[type=item]", "kill @e[type=boat]", "cornerofeye stage 0", "time set 18000", "gamemode survival Dev",
     # fill is limited to 32768 blocks: clear the air in slabs.
     *[f"fill 460 {y} -100 510 {min(y + 9, -20)} -40 air" for y in range(-61, -20, 10)],
     "fill 460 -62 -100 510 -62 -40 grass_block", *extra)
rcon("effect clear Dev", "effect give Dev resistance 99999 255 true", "effect give Dev regeneration 99999 5 true",
     "effect give Dev night_vision 99999 0 true", f"tp Dev {px} {py} {pz} {yaw} {pitch}")
time.sleep(1)
after = [c for c in os.environ.get("AFTER", "").split(";") if c]
if after:
    rcon(*after)
time.sleep(1)
front = os.environ.get("VIEW") == "front"
if front:
    key("F5")
    key("F5")
proc = record(name, seconds)
time.sleep(0.3)
rcon(f'summon cornerofeye:the_one {mx} {my} {mz} {{Phase:"HUNT"}}')
turns = [tuple(float(v) for v in t.split(":")) for t in os.environ.get("TURN", "").split(";") if t]
for i in range(seconds * 2 - 2):
    time.sleep(0.5)
    for at, ty, tp in turns:
        if abs(i * 0.5 + 0.8 - at) < 0.25:
            rcon(f"tp Dev {px} {py} {pz} {ty} {tp}")
    if i % 4 == 0:
        out = rcon("execute as @e[type=cornerofeye:the_one] run data get entity @s Pos")
        print(round(i * 0.5 + 0.8, 1), out.strip().split("data:")[-1].strip()[:50])
proc.wait()
if front:
    key("F5")
rcon("kill @e[type=cornerofeye:the_one]")
