#!/bin/bash
# First-time setup of run/ for the scripted test harness: a flat offline world with RCON, Russian client.
cd "$(dirname "$0")/../.."
mkdir -p run
echo "eula=true" > run/eula.txt
if [ ! -f run/server.properties ]; then
  printf 'enable-rcon=true\nrcon.password=test\nrcon.port=25575\nbroadcast-rcon-to-ops=true\nonline-mode=false\nlevel-type=minecraft\\:flat\nspawn-monsters=false\nspawn-protection=0\n' > run/server.properties
fi
[ -f run/options.txt ] || echo "lang:ru_ru" > run/options.txt
echo "run/ ready"
