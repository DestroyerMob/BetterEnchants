# Better Enchanting

A NeoForge 1.21.1 mod that replaces vanilla enchanting with a deterministic,
tag-driven system using essences, enchanted books, and item affinity.

## Current status

Pre-alpha / internal playtesting. The current branch includes explicit Apotheosis / Apothic Enchanting integration for Minecraft Beyond.

## Project Facts

- Mod id: `betterenchanting`
- Current version: `0.1.0`
- Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21
- Optional integration: Apothic Enchanting 1.x
- Common config: `config/betterenchanting-common.toml`

## Features

- Enhanced enchanting table / Arcane Crucible
- Essence-based pool restriction and weighting
- Enchanted-book weighting
- Data-driven enchantment limits
- Enchantment fusion
- Custom enchantments
- Balance presets for vanilla-plus, balanced, overhaul, power-fantasy, and custom setups
- Multiple durability-maintenance paths through Mending, material repair, and passive recovery
- Configurable anvil, mending, XP, and roll behaviour
- Optional Silent Gear and Mobs Tool Forging material support
- Optional Apothic Enchanting support, including Apothic table stats, clues, arcana/quanta weighting, infusion offers, and datapack-driven infusion modifier requirements

## Supported versions

- Minecraft 1.21.1
- NeoForge 21.1.234
- Java 21

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

Apothic infusion modifier datapack hooks are documented in [docs/apothic-infusion-modifier-rules.md](docs/apothic-infusion-modifier-rules.md).

When Apothic Enchanting is installed, Better Enchanting leaves the vanilla enchanting table to Apothic and uses the Arcane Crucible as the Better Enchanting table surface. The config option `enchanting.enhanced_table_takeover` still controls vanilla-table takeover in non-Apothic setups.

## Known issues

- Pre-alpha balance is not final.
- Public release jars are not published yet.
- Screenshots/GIFs still need to be captured from an in-game build.
- The mod changes anvil, mending, XP, and enchanting behavior, so compatibility testing with other enchanting/anvil overhaul mods is still required.
- Apothic support uses reflection to avoid a hard dependency; if Apothic internals change, the integration should fail closed and fall back instead of crashing.

## License

MIT. See [LICENSE](LICENSE).
