#!/bin/bash
# Cuts the recorded (CC0) sounds out of their sources into src/main/resources/assets/cornerofeye/sounds.
#   tools/import_sounds.sh <folder with the downloaded files>
# Sources (all CC0, from OpenGameArt; credits in docs/CREDITS.md):
#   horrorbreathing.wav   https://opengameart.org/content/horror-breathing          (zip)
#   ghostbreath.flac      https://opengameart.org/content/ghost-breath
#   Deep Break N.wav      https://opengameart.org/content/deep-bone-crackbreak-sfx  (zip)
#   heartbeat_slow_0.wav  https://opengameart.org/content/heartbeat-sounds
#   rippingintomeat_0.mp3 https://opengameart.org/content/ripping-into-meat
set -e
IN="${1:?folder with the downloaded files}"
S="$(dirname "$0")/../src/main/resources/assets/cornerofeye/sounds"
enc() { ffmpeg -v error -y -ss "$2" -t "$3" -i "$IN/$1" -ac 1 -af "$4" -ar 44100 -c:a libvorbis -q:a 5 "$S/$5.ogg"; }
# Its breathing, a little lower than a person's.
enc horrorbreathing.wav 0.0 3.1 "asetrate=44100*0.86,aresample=44100,afade=t=in:d=0.05,afade=t=out:st=3.3:d=0.3,volume=-13dB" the_one_breath_rec1
enc horrorbreathing.wav 3.1 3.15 "asetrate=44100*0.8,aresample=44100,afade=t=in:d=0.05,afade=t=out:st=3.5:d=0.4,volume=-11dB" the_one_breath_rec2
enc ghostbreath.flac 0.15 3.0 "aresample=44100,asetrate=44100*0.9,aresample=44100,afade=t=in:d=0.05,afade=t=out:st=3.0:d=0.3,volume=14dB,alimiter=limit=0.95" the_one_breath_rec3
# Your heart: one beat, dull.
enc heartbeat_slow_0.wav 0.0 0.55 "afade=t=out:st=0.45:d=0.1,lowpass=f=900,volume=14dB,alimiter=limit=0.95" heartbeat_rec
# Its joints.
for i in 1 2 6 8; do enc "Deep Break $i.wav" 0.0 1.0 "afade=t=out:st=0.7:d=0.25,volume=-3dB" bone_crack$i; done
# What it does when it touches you.
enc rippingintomeat_0.mp3 3.4 1.2 "afade=t=in:d=0.02,afade=t=out:st=1.0:d=0.2,volume=6dB" flesh1
enc rippingintomeat_0.mp3 19.6 1.2 "afade=t=in:d=0.02,afade=t=out:st=1.0:d=0.2,volume=6dB" flesh2
enc rippingintomeat_0.mp3 22.8 1.2 "afade=t=in:d=0.02,afade=t=out:st=1.0:d=0.2,volume=6dB" flesh3
