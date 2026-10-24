#!/bin/bash
# Kills the dev client (never this script or its parents), starts a fresh one on the virtual display and
# waits until it has joined the dev server. EXTRA_JVM="-Dcornerofeye.alwaysVisible=true" for renders.
cd "$(dirname "$0")/../.."
LOGS=build/devshots; mkdir -p $LOGS
SIZE=${DEV_SIZE:-1280x720}; W=${SIZE%x*}; H=${SIZE#*x}
pgrep -x Xvfb >/dev/null || (Xvfb :99 -screen 0 ${SIZE}x24 >/dev/null 2>&1 &)
for p in $(ls /proc | grep -E '^[0-9]+$'); do
  [ "$p" = "$$" ] || [ "$p" = "$PPID" ] && continue
  exe=$(readlink /proc/$p/exe 2>/dev/null); case "$exe" in *java*) ;; *) continue;; esac
  c=$(tr '\0' ' ' < /proc/$p/cmdline 2>/dev/null)
  case "$c" in *runClient*|*forgeclientuserdev*) kill $p;; esac
done
sleep 4
# Subtitles make sounds visible in screenshots. Set after the old client is gone: it saves its options on exit.
if [ -f run/options.txt ]; then
  grep -q '^showSubtitles' run/options.txt && sed -i 's/^showSubtitles:.*/showSubtitles:true/' run/options.txt || echo 'showSubtitles:true' >> run/options.txt
fi
n=$(grep -c "Dev joined the game" $LOGS/server.log 2>/dev/null); n=${n:-0}
(DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 ALSOFT_DRIVERS=null JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS $EXTRA_JVM" JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 nohup ./gradlew --no-daemon runClient --args="--quickPlayMultiplayer localhost:25565 --width $W --height $H" > $LOGS/client.log 2>&1 &)
for i in $(seq 1 120); do [ $(grep -c "Dev joined the game" $LOGS/server.log) -gt $n ] && break; sleep 3; done
sleep 6
echo joined
