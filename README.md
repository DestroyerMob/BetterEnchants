# Better Enchanting

A NeoForge 1.21.1 mod that replaces vanilla enchanting with a deterministic,
tag-driven system using essences, enchanted books, and item affinity.

## Current status

Pre-alpha / internal playtesting.

## Features

- Enhanced enchanting table / Arcane Crucible
- Essence-based pool restriction and weighting
- Enchanted-book weighting
- Data-driven enchantment limits
- Enchantment fusion
- Custom enchantments
- Balance presets for vanilla-plus, balanced, overhaul, power-fantasy, and custom setups
- Configurable anvil, mending, XP, and roll behaviour
- Optional Silent Gear material support

## Supported versions

Minecraft 1.21.1  
NeoForge 21.1.228  
Java 21

## Build

```sh
./gradlew build
```

## Install

Put the jar in your mods folder.

## Screenshots

[UI screenshot](screenshots/README.md#ui-screenshot)

[Essence tooltip screenshot](screenshots/README.md#essence-tooltip-screenshot)

[Example roll tooltip screenshot](screenshots/README.md#example-roll-tooltip-screenshot)

## Configuration

See [docs/enhanced-enchanting-guidebook.md](docs/enhanced-enchanting-guidebook.md).

Balance notes live in [docs/enchantment-balance-pass.md](docs/enchantment-balance-pass.md).

## Known issues

- Pre-alpha balance is not final.
- Public release jars are not published yet.
- Screenshots/GIFs still need to be captured from an in-game build.
- The mod changes anvil, mending, XP, and enchanting behavior, so compatibility testing with other enchanting/anvil overhaul mods is still required.
- For Apotheosis or Apothic Enchanting, the safest setup is to let that mod own the vanilla enchanting table and use the Arcane Crucible for Better Enchanting.

## License

MIT. See [LICENSE](LICENSE).
