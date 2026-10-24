#!/bin/bash
# Stops the dev server over RCON (if it is up), starts it again and waits until it is ready.
cd "$(dirname "$0")/../.."
LOGS=build/devshots; mkdir -p $LOGS
python3 tools/dev/rcon.py stop >/dev/null 2>&1
servers() {
  for p in $(ls /proc | grep -E '^[0-9]+$'); do
    [ "$p" = "$$" ] || [ "$p" = "$PPID" ] && continue
    exe=$(readlink /proc/$p/exe 2>/dev/null); case "$exe" in *java*) ;; *) continue;; esac
    tr '\0' ' ' < /proc/$p/cmdline 2>/dev/null | grep -q forgeserveruserdev && echo $p
  done
}
for i in $(seq 1 45); do
  [ -z "$(servers)" ] && break; sleep 1
done
# Still there: it would hold the ports and the new one would start without RCON. Make sure it is gone.
for p in $(servers); do kill $p; done
for i in $(seq 1 15); do [ -z "$(servers)" ] && break; sleep 1; done
for p in $(servers); do kill -9 $p; done
(JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 nohup ./gradlew --no-daemon runServer > $LOGS/server.log 2>&1 &)
for i in $(seq 1 80); do grep -q "Done (" $LOGS/server.log && break; sleep 3; done
echo server up
