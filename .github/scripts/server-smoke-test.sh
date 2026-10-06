#!/usr/bin/env bash
# Usage: server-smoke-test.sh <minecraft version> <directory containing built jars>
#
# Starts a Fabric dedicated server with only Villager Bargains installed, waits for the
# world to finish loading, stops it, and fails if the mod did not load cleanly.
set -euo pipefail

MC="$1"
JARS="$(realpath "$2")"
LOADER="$(grep '^loader_version=' gradle.properties | cut -d= -f2)"

MOD_JAR="$(find "$JARS" -name "villagerbargains-*+${MC}.jar" ! -name '*-sources.jar' | head -n1)"
[ -n "$MOD_JAR" ] || { echo "No jar for Minecraft $MC in $JARS"; exit 1; }

INSTALLER="$(curl -fsS https://meta.fabricmc.net/v2/versions/installer | jq -r '[.[] | select(.stable)][0].version')"

mkdir -p server/mods && cd server
curl -fsS -o fabric-server.jar "https://meta.fabricmc.net/v2/versions/loader/${MC}/${LOADER}/${INSTALLER}/server/jar"
cp "$MOD_JAR" mods/
echo 'eula=true' > eula.txt
printf 'online-mode=false\nlevel-type=minecraft\\:flat\nspawn-protection=0\n' > server.properties

echo "Starting Minecraft $MC (Fabric Loader $LOADER) with $(basename "$MOD_JAR")"
# Feed "stop" to the console once the server reports it is ready.
timeout 600 java -Xmx2G -jar fabric-server.jar nogui < <(
  until grep -q 'Done (' logs/latest.log 2>/dev/null; do sleep 2; done
  echo stop
) | tee server.log

grep -q 'Villager Bargains loaded' server.log || { echo 'FAIL: mod did not initialise'; exit 1; }
grep -q 'Done (' server.log || { echo 'FAIL: server did not finish starting'; exit 1; }
if grep -E 'Mixin apply.*failed|InvalidInjectionException|InjectionError|MixinApplyError' server.log; then
  echo 'FAIL: mixin errors'
  exit 1
fi
echo "OK: Minecraft $MC server started with Villager Bargains"
