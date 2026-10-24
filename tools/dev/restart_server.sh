#!/bin/bash
# Stops the dev server over RCON (if it is up), starts it again and waits until it is ready.
cd "$(dirname "$0")/../.."
LOGS=build/devshots; mkdir -p $LOGS
python3 tools/dev/rcon.py stop >/dev/null 2>&1
for i in $(seq 1 30); do
  alive=0
  for p in $(ls /proc | grep -E '^[0-9]+$'); do
    [ "$p" = "$$" ] || [ "$p" = "$PPID" ] && continue
    exe=$(readlink /proc/$p/exe 2>/dev/null); case "$exe" in *java*) ;; *) continue;; esac
    tr '\0' ' ' < /proc/$p/cmdline 2>/dev/null | grep -q forgeserveruserdev && alive=1
  done
  [ $alive = 0 ] && break; sleep 1
done
(JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 nohup ./gradlew --no-daemon runServer > $LOGS/server.log 2>&1 &)
for i in $(seq 1 80); do grep -q "Done (" $LOGS/server.log && break; sleep 3; done
echo server up
