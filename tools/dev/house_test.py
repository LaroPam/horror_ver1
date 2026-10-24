"""Builds a closed stone house with a door and lets it come for the player from a given side.

    python3 tools/dev/house_test.py <name> <monster x> <monster z> <player yaw> [seconds] [extra setblock commands...]

The house spans x 480..488, z -74..-62, floor y -61, with an oak door in the south wall at x 484.
"""
import sys
import time

sys.path.insert(0, "tools/dev")
from devlib import rcon, record, frames, key  # noqa: E402

name, mx, mz, yaw = sys.argv[1], float(sys.argv[2]), float(sys.argv[3]), float(sys.argv[4])
seconds = int(sys.argv[5]) if len(sys.argv) > 5 else 30
extra = sys.argv[6:]
rcon("tp Dev 484.5 -50 -68.5 0 0")
time.sleep(2)
rcon("kill @e[type=cornerofeye:the_one]", "kill @e[type=item]", "cornerofeye stage 0", "time set 18000", "gamemode survival Dev",
     "fill 470 -61 -86 500 -50 -50 air", "fill 470 -62 -86 500 -62 -50 grass_block",
     "fill 480 -61 -74 488 -56 -62 stone_bricks hollow", "fill 480 -61 -74 488 -61 -62 oak_planks",
     "setblock 484 -60 -62 oak_door[facing=north,half=lower,hinge=left]", "setblock 484 -59 -62 oak_door[facing=north,half=upper,hinge=left]",
     *extra)
rcon("effect clear Dev", "effect give Dev resistance 99999 255 true", "effect give Dev regeneration 99999 5 true",
     "effect give Dev night_vision 99999 0 true", f"tp Dev 484.5 -60 -68.5 {yaw} 4")
time.sleep(2)
proc = record(name, seconds)
time.sleep(0.3)
rcon(f'summon cornerofeye:the_one {mx} -60 {mz} {{Phase:"HUNT"}}')
for i in range(seconds * 2 - 2):
    time.sleep(0.5)
    out = rcon("execute as @e[type=cornerofeye:the_one] run data get entity @s Pos")
    if i % 4 == 0:
        print(round(i * 0.5 + 0.8, 1), out.strip().split("data:")[-1].strip()[:46])
proc.wait()
rcon("kill @e[type=cornerofeye:the_one]")
# Holes in the walls: refill each wall's air with bricks and count (the house is rebuilt on every run anyway).
walls = ["fill 480 -60 -74 480 -57 -62 stone_bricks replace air", "fill 488 -60 -74 488 -57 -62 stone_bricks replace air",
         "fill 480 -60 -74 488 -57 -74 stone_bricks replace air", "fill 481 -60 -62 487 -57 -62 stone_bricks replace air"]
print(rcon("execute if block 484 -60 -62 oak_door[open=true]", *walls).strip())
