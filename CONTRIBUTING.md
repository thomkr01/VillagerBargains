# Contributing to Villager Bargains

## Branches

```
main      ← stable releases only
develop   ← integration branch (features merge here first)
feature/* ← individual features / fixes, branched from develop
```

1. `git checkout develop && git pull && git checkout -b feature/my-change`
2. Make the change and run `./gradlew build` (builds the jar and runs unit and game tests).
3. Open a pull request into `develop`. CI also runs the jar and the game tests on a real
   server for every supported Minecraft version.
4. When `develop` is ready to release, open a pull request from `develop` into `main`.

## Design rules

* **Only touch the price roll.** Every hook must let vanilla draw its random number first
  (`original.call(...)`) and only then replace the result via `PriceRolls`. Skipping the
  call would shift the random sequence and change other outcomes.
* **Never widen a range.** A pricing mode picks a value vanilla itself could have produced.
* **Leave discounts alone.** Reputation/gossip and Hero of the Village (`specialPriceDiff`)
  stay vanilla in every mode; the demand rule only pins the demand value.
* **Every trade is covered.** A new or changed price hook must keep `AllTradesSweepGameTest`
  green (same items and random sequence in every mode, `MINIMUM ≤ NORMAL ≤ MAXIMUM`). Game
  tests derive their seeds from `villagerbargains.test.seedOffset`, which CI varies across
  its 3 runs per Minecraft version, so tests must not depend on one particular seed.
* **One jar.** When vanilla code differs between supported versions, write one mixin per
  variant. Target classes missing from the version you compile against are named as strings
  (`@Mixin(targets = "…")`); `VillagerBargainsMixinPlugin` skips a mixin whose target is absent.
  Code shared by all versions must only use vanilla API that exists in all of them.
* One responsibility per class, a short Javadoc on every class, and each mixin documents the
  vanilla line it hooks.

## Where things live

| Change | File(s) |
|--------|---------|
| Pricing behaviour | `src/main/java/com/villagerbargains/config/PricingMode.java` |
| Config file format | `src/main/java/com/villagerbargains/config/VillagerBargainsConfig.java` |
| Price-roll hooks (books, gear, uniform) | `src/main/java/com/villagerbargains/mixin/` + `villagerbargains.mixins.json` |
| Demand rule | `price/DemandRule.java`, hooks in `mixin/rules/` + `villagerbargains.rules.mixins.json` |
| Mod Menu screen / texts | `src/client/java/…/client/`, `src/main/resources/assets/villagerbargains/lang/` |
| Game tests | `src/gametest/java/` (must also run on every supported version): `EnchantedBookPriceGameTest`, `EnchantedGearPriceGameTest`, `DemandRuleGameTest`, `AllTradesSweepGameTest`, `UniformTradeCostGameTest` |
| Supported Minecraft versions | `"minecraft"` in `fabric.mod.json` + the matrix in `.github/workflows/build.yml` |
| Compile-against versions, mod version | `gradle.properties` |

## Adding a Minecraft version

1. Add it to `"minecraft"` in `src/main/resources/fabric.mod.json` and to the `server-test`
   matrix in `.github/workflows/build.yml` (with its Fabric API version).
2. If it is the newest, update `gradle.properties` (values from <https://fabricmc.net/develop>).
3. Push. If the server test for the new version fails, compare the vanilla code each mixin
   documents with the new version and add a variant mixin where it changed.

## Releasing

Bump `mod_version` in `gradle.properties`, merge to `main`, then push a tag `v<mod_version>`
(e.g. `v2.0.0`). The release workflow publishes a GitHub release with the jar.
