# Contributing to Villager Bargains

## Branches

```
main      ← stable releases only
develop   ← integration branch (features merge here first)
feature/* ← individual features / fixes, branched from develop
```

1. `git checkout develop && git pull && git checkout -b feature/my-change`
2. Make the change and run `./gradlew build` (compiles both Minecraft versions and runs the tests).
3. Open a pull request into `develop`. CI also boots a dedicated server for every version.
4. When `develop` is ready to release, open a pull request from `develop` into `main`.

## Design rules

* **Only touch the price roll.** Every hook must let vanilla draw its random number first
  (`original.call(...)`) and only then replace the result via `PriceRolls`. Skipping the
  call would shift the random sequence and change other outcomes.
* **Never widen a range.** A pricing mode picks a value vanilla itself could have produced.
* **Shared first.** Code goes in `src/` unless the vanilla code it targets differs between
  Minecraft versions; only then does it go in `versions/<minecraft>/src/`.
* One responsibility per class, a short Javadoc on every class, and each mixin documents the
  vanilla line it hooks.

## Where things live

| Change | File(s) |
|--------|---------|
| Pricing behaviour | `src/main/java/com/villagerbargains/config/PricingMode.java` |
| Config file format | `src/main/java/com/villagerbargains/config/VillagerBargainsConfig.java` |
| Hook shared by all versions | `src/main/java/com/villagerbargains/mixin/` + `villagerbargains.mixins.json` |
| Hook for one version | `versions/<mc>/src/main/java/com/villagerbargains/mixin/version/` + that folder's `villagerbargains.version.mixins.json` |
| Mod Menu screen / texts | `src/client/java/…/client/`, `src/main/resources/assets/villagerbargains/lang/` |
| Minecraft / Fabric / Mod Menu versions | `versions/<mc>/gradle.properties` |
| Loader / Loom / mod version | `gradle.properties` |

## Adding a Minecraft version

1. Copy the closest `versions/<mc>` folder to `versions/<new>` and update its `gradle.properties`
   (values from <https://fabricmc.net/develop>).
2. Add `<new>` to the list in `settings.gradle` and to the matrices in `.github/workflows/`.
3. Run `./gradlew :<new>:build`. If a mixin no longer finds its target, compare the vanilla
   code it documents with the new version and move the hook into `versions/<new>/src/` if it differs.

## Releasing

Bump `mod_version` in `gradle.properties`, merge to `main`, then push a tag `v<mod_version>`
(e.g. `v2.0.0`). The release workflow publishes one GitHub release per Minecraft version.
