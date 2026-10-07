#!/usr/bin/env bash
# Usage: server-test.sh <minecraft version> <fabric api version> <directory containing built jars>
#
# Runs the released jar on a real Fabric dedicated server for one Minecraft version:
#   1. boot with ONLY Villager Bargains installed (proves no other mods are needed and every
#      mixin applies; a hook that no longer matches vanilla crashes this boot), then
#   2. run the game tests from the -gametest jar on that same version (needs Fabric API),
#      three times on a fresh world each, with seed offsets 0, 100000 and 200000
#      (-Dvillagerbargains.test.seedOffset) so each run samples different trades.
set -euo pipefail

MC="$1"
FABRIC_API="$2"
JARS="$(realpath "$3")"
LOADER="$(grep '^loader_version=' gradle.properties | cut -d= -f2)"
MAVEN=https://maven.fabricmc.net/net/fabricmc/fabric-api

MOD_JAR="$(find "$JARS" -name 'villagerbargains-*.jar' ! -name '*-gametest.jar' | head -n1)"
TEST_JAR="$(find "$JARS" -name 'villagerbargains-*-gametest.jar' | head -n1)"
[ -n "$MOD_JAR" ] && [ -n "$TEST_JAR" ] || { echo "Jars not found in $JARS"; exit 1; }

INSTALLER="$(curl -fsS https://meta.fabricmc.net/v2/versions/installer | jq -r '[.[] | select(.stable)][0].version')"
mkdir -p server/mods && cd server
curl -fsS -o fabric-server.jar "https://meta.fabricmc.net/v2/versions/loader/${MC}/${LOADER}/${INSTALLER}/server/jar"
echo 'eula=true' > eula.txt
printf 'online-mode=false\nlevel-type=minecraft\\:flat\n' > server.properties

# Java 25 warns when a library calls the deprecated sun.misc.Unsafe memory methods; JOML,
# bundled with Minecraft, does. Allow them explicitly so the logs stay free of that warning.
JAVA=(java -Xmx2G --sun-misc-unsafe-memory-access=allow)

fail() { echo "FAIL (Minecraft $MC): $1"; exit 1; }

echo "=== 1. Boot Minecraft $MC with only $(basename "$MOD_JAR")"
cp "$MOD_JAR" mods/
timeout 600 "${JAVA[@]}" -jar fabric-server.jar nogui < <(
  until grep -q 'Done (' logs/latest.log 2>/dev/null; do sleep 2; done
  echo stop
) | tee boot.log
grep -q 'Villager Bargains loaded' boot.log || fail 'mod did not initialise'
grep -q 'Done (' boot.log || fail 'server did not finish starting'
! grep -E 'Mixin apply.*failed|InvalidInjectionException|InjectionError|MixinApplyError' boot.log || fail 'mixin errors'
echo "Hooks for this version:"; grep -E 'Applying|Skipping' boot.log

echo "=== 2. Game tests on Minecraft $MC (Fabric API $FABRIC_API)"
GAMETEST_API="$(curl -fsS "$MAVEN/fabric-api/$FABRIC_API/fabric-api-$FABRIC_API.pom" \
  | grep -A1 '<artifactId>fabric-gametest-api-v1</artifactId>' | grep -o '<version>[^<]*' | cut -d'>' -f2)"
curl -fsS -o mods/fabric-api.jar "$MAVEN/fabric-api/$FABRIC_API/fabric-api-$FABRIC_API.jar"
curl -fsS -o mods/fabric-gametest-api.jar "$MAVEN/fabric-gametest-api-v1/$GAMETEST_API/fabric-gametest-api-v1-$GAMETEST_API.jar"
cp "$TEST_JAR" mods/

# Run the full game tests several times, each on a fresh world and with a different seed
# offset (see TradeTestSupport#seed), so every run checks a new set of generated trades.
SEED_OFFSETS=(0 100000 200000)
RUN=0
for OFFSET in "${SEED_OFFSETS[@]}"; do
  RUN=$((RUN + 1))
  echo "=== 2.$RUN Game tests on Minecraft $MC (run $RUN of ${#SEED_OFFSETS[@]}, seed offset $OFFSET)"
  rm -rf world
  LOG="gametest-$OFFSET.log"
  timeout 600 "${JAVA[@]}" -Dfabric-api.gametest -Dvillagerbargains.test.seedOffset="$OFFSET" \
    -jar fabric-server.jar nogui < /dev/null | tee "$LOG"
  grep -q 'All [0-9]* required tests passed' "$LOG" \
    || fail "game tests did not pass (run $RUN, seed offset $OFFSET)"
  echo "--- Run $RUN (seed offset $OFFSET): $(grep -o 'All [0-9]* required tests passed' "$LOG" | tail -n1)"
done

echo "OK: Minecraft $MC (game tests passed with seed offsets ${SEED_OFFSETS[*]})"
