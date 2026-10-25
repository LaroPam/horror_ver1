"""Fires every moment, event and command of the mod in a prepared dev world, reports what each one did, and lists
any exceptions the server or client logged meanwhile. Needs the dev server and client running (tools/dev).

    python3 tools/dev/smoke_test.py
"""
import sys
import time

sys.path.insert(0, '/home/user/horror_ver1/tools/dev')
import devlib as d  # noqa: E402

LOG = '/home/user/horror_ver1/build/devshots/'


def lines(name):
    with open(LOG + name, encoding='utf-8', errors='replace') as f:
        return f.readlines()


start_server, start_client = len(lines('server.log')), len(lines('client.log'))


def r(*cmds):
    return d.rcon(*cmds)


def last(out):
    rows = [l.strip() for l in out.strip().splitlines() if l.strip() and not l.startswith('>')]
    return rows[-1] if rows else ''


# ---------------------------------------------------------------- the stage
r('kill @e[type=cornerofeye:the_one]', 'kill @e[type=cornerofeye:mimic]', 'kill @e[type=villager]', 'kill @e[type=item]',
  'kill @e[type=item_frame]', 'gamerule doDaylightCycle false', 'time set 18000', 'weather clear',
  'cornerofeye reset', 'cornerofeye stage 3', 'gamemode survival Dev', 'tp Dev 484.5 -50 -68.5')
time.sleep(1.5)
for y in range(-61, -45):
    r(f'fill 455 {y} -110 515 {y} -35 air')
r('fill 455 -62 -110 515 -62 -35 grass_block')
# A house with a door, a window, a bed and a chest.
r('fill 480 -61 -74 488 -56 -62 stone_bricks hollow', 'fill 480 -61 -74 488 -61 -62 oak_planks',
  'setblock 484 -60 -62 oak_door[facing=north,half=lower,hinge=left]', 'setblock 484 -59 -62 oak_door[facing=north,half=upper,hinge=left]',
  'fill 480 -60 -70 480 -60 -67 glass',
  'setblock 486 -60 -72 red_bed[facing=north,part=head]', 'setblock 486 -60 -71 red_bed[facing=north,part=foot]',
  'setblock 482 -60 -73 chest[facing=south]', 'item replace block 482 -60 -73 container.0 with iron_sword',
  'setblock 487 -58 -73 torch', 'setblock 481 -58 -63 wall_torch[facing=east]')
# A tree outside, a deep pool, a village, a doll hung up in the dark.
# A closed stone room with a high ceiling, as in a cave.
r('fill 455 -62 -60 465 -56 -50 stone hollow')
r('fill 496 -61 -55 496 -57 -55 oak_log', 'fill 465 -62 -98 475 -55 -88 glass hollow', 'fill 466 -61 -97 474 -56 -89 water',
  'fill 501 -61 -101 507 -57 -101 stone', 'summon item_frame 504 -59 -100 {Facing:3b,Item:{id:"cornerofeye:cursed_doll",Count:1b}}')
for i in range(4):
    r(f'summon villager {498 + i * 2} -61 -45')
r('effect give Dev resistance 99999 255 true', 'effect give Dev regeneration 99999 5 true', 'effect give Dev saturation 99999 0 true',
  'spawnpoint Dev 486 -60 -72')
time.sleep(1)


def clear_one():
    r('kill @e[type=cornerofeye:the_one]', 'effect clear Dev minecraft:darkness', 'effect clear Dev minecraft:slowness')
    time.sleep(0.6)


def moment(name, where, wait=5.0):
    clear_one()
    r(f'tp Dev {where}')
    time.sleep(1.2)
    out = last(r(f'cornerofeye moment {name} Dev'))
    time.sleep(wait)
    here = 'here now: ✔' in r('cornerofeye status')
    print(f'moment {name:11s}: {out}   (still here after {wait:.0f}s: {here})')


moment('door', '484.5 -60 -68.5 180 5', 7)
moment('window', '486.5 -60 -68.5 100 0', 6)
moment('ceiling', '460.5 -61 -55.5 0 0', 5)
moment('behind', '470.5 -61 -60.5 0 0', 4)
moment('still', '470.5 -61 -60.5 0 0', 3)
moment('peek', '496.5 -61 -66.5 0 0', 4)
moment('chase', '470.5 -61 -75.5 0 0', 5)
moment('underwater', '470.5 -57 -93.5 0 40', 4)
# Lights it put out stay dead, across restarts too: new spots every run.
import random
lx, lz = 468 + random.randrange(0, 20), -84 + random.randrange(0, 8)
r('give Dev torch 4', f'setblock {lx} -61 {lz} lantern', f'setblock {lx + 2} -61 {lz - 1} lantern', f'setblock {lx - 2} -61 {lz - 2} lantern')
moment('lights_out', f'{lx}.5 -61 {lz - 1}.5 0 0', 6)
moment('paralysis', '486.5 -60 -70.5 180 0', 6)
moment('lured', '500.5 -61 -90.5 180 0', 5)
moment('massacre', '500.5 -61 -50.5 0 0', 8)
clear_one()

# ---------------------------------------------------------------- events
r('tp Dev 484.5 -60 -68.5 180 0')
time.sleep(1)
for event in ['distant_sound', 'far_light_out', 'footsteps', 'mining', 'door', 'whisper', 'lights_out', 'fake_chat', 'sign',
              'scratches', 'handprints', 'writing', 'stare', 'mimic', 'gnaw']:
    print(f'event {event:13s}:', last(r(f'cornerofeye event {event} Dev')))
    time.sleep(1.5)
r('kill @e[type=cornerofeye:mimic]')

# ---------------------------------------------------------------- commands
print('director     :', last(r('cornerofeye director Dev')))
print('habit        :', last(r('cornerofeye habit Dev light 0.6')))
print('morning      :', last(r('cornerofeye morning Dev')))
r('tp Dev 484.5 -61 -100.5')
time.sleep(1.5)
print('visit        :', last(r('cornerofeye visit Dev')))
print('dream        :', last(r('cornerofeye dream Dev')))
time.sleep(4)
r('execute as Dev at Dev run tp Dev ~ 60 ~')
time.sleep(2)
print('after dream  :', last(r('execute as Dev run data get entity @s Dimension')))
print('take         :', last(r('cornerofeye take Dev')))
time.sleep(3)
r('execute as Dev at Dev run tp Dev ~ 60 ~')
time.sleep(2)
print('after take   :', last(r('execute as Dev run data get entity @s Dimension')), last(r('execute as Dev run data get entity @s playerGameType')))
r('cornerofeye bloodmoon on')
time.sleep(1)
print('appear hunt  :', last(r('cornerofeye appear hunt Dev')))
time.sleep(5)
r('cornerofeye bloodmoon off', 'kill @e[type=cornerofeye:the_one]', 'cornerofeye habit Dev light 0', 'tp Dev 484.5 -60 -68.5')
time.sleep(1)
d.shot('smoke_end')

server = lines('server.log')[start_server:]
client = lines('client.log')[start_client:]
noise = ('netty', 'Reflective', 'IllegalAccess', 'PlatformDependent', 'MCP')
for name, ls in (('server', server), ('client', client)):
    bad = [l.rstrip() for l in ls if ('Exception' in l or '/ERROR]' in l or 'Caused by' in l) and not any(n in l for n in noise)]
    print(f'{name}: {len(bad)} error lines')
    for l in bad[:25]:
        print('   ', l[:220])
