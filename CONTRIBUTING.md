# Contributing to VillagerBargains

This project uses a **main / develop** branching model for safe, modular iteration.

## Branch workflow

```
main      ← stable releases only
develop   ← integration branch (all features merge here first)
feature/* ← individual features / fixes branched from develop
```

## Making a change

1. Branch off `develop`:
   ```bash
   git checkout develop
   git pull
   git checkout -b feature/my-change
   ```
2. Make your changes.
3. Open a **Pull Request** from `feature/my-change` → `develop`.
4. Once reviewed and CI passes, merge into `develop`.
5. When `develop` is stable and ready for a release, open a PR from `develop` → `main`.

## What belongs where

| File | What to change |
|------|----------------|
| `VanillaTrades.java` | Add/remove/update trade ranges for a new MC version |
| `gradle.properties` | Bump `minecraft_version`, `loader_version`, `mod_version` |
| `VillagerBargainsConfig.java` | Add new config fields |
| `TradeJsonBuilder.java` | Change the JSON structure of an override |
| `GodRollResourcePack.java` | Change how overrides are built/registered |
| `InMemoryPack.java` | Change how the pack is served |
| `.github/workflows/build.yml` | Change CI steps or Java version |

## Code style

- Keep each class focused on one responsibility.
- No magic numbers — use the `TradeDefinition` fields.
- All public classes/methods get a one-line Javadoc comment.
