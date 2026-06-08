# Changelog

## [1.0.0] - 2026-06-08

### Added
- Global price mode: `MINIMUM` (godroll) or `MAXIMUM` — set in `config/villagerbargains.json`
- Per-trade price overrides via `perTradePrices` map in config
- Mixin on `Villager#updateTrades` — applies price directly to each `MerchantOffer`
- `VanillaTrades.java` — central registry of all vanilla villager trade definitions
- `PriceResolver.java` — resolves MINIMUM or MAXIMUM price for a given trade ID
- `VillagerBargainsConfig.java` — loads/saves JSON config from the `config/` folder

### Compatibility
- Minecraft 26.1.2
- Fabric Loader 0.19.3+
- Fabric API 0.149.1+26.1.2
- Java 21 compile target, Java 25 runtime
