# Better Enchanting

Better Enchanting is a NeoForge 1.21.1 enchanting overhaul built around deterministic offers, affinity tags, essence reagents, enchanted-book guidance, and item/material compatibility.

## Current status

Pre-alpha / internal playtesting. The core table loop, custom enchantments, datapack extension points, modular-equipment routing, JEI guide, and explicit Apotheosis / Apothic Enchanting integration are implemented. Balance and UI details are still changing quickly.

## Project Facts

- Mod id: `betterenchanting`
- Current version: `0.1.0`
- Target: Minecraft 1.21.1, NeoForge 21.1.234, Java 21
- Optional integrations: JEI 19+, Apothic Enchanting 1.x, Mobs Tool Forging, and Silent Gear
- Common config: `config/betterenchanting-common.toml`

## Features

- Vanilla enchanting-table takeover with persistent target, reagent, and modifier storage
- A screenless Arcane Crucible that distills all ten essences with floating ingredients, orbiting catalysts, progress runes, and a visible result
- A screenless Attunement Pedestal that upgrades a chosen existing enchantment by exactly one level through selectable world-space orbs
- A reusable Attunement Focus for modular enchantment tuning and full-height, 3×3-chunk geode resonance searches
- Three deterministic offers sculpted by a required affinity essence, three unordered modifier slots, the target item, bookshelf power, and the player's enchantment seed
- Essence-based pool restriction/refinement, enchanted-book weighting, purification, and pedestal-only Nether Star overleveling
- Essence consumption in place of lapis/XP-level charges for the enhanced table; bookshelves control roll quality rather than payment
- Data-driven essence definitions, primary/secondary affinity roles, enchantment limits, item targets, affinity display, tag simplification, loot injection, villager trades, and enchantment fusions
- Twenty-five custom enchantments spanning combat, mining, mobility, durability, utility, and curses, including Drawn Steel's prepared katana strikes, Distant Edge spacing, Moonlit Reversal counters, Resonance's animated through-wall ore echoes, Tree Capitator II replanting, and modded-crop Harvest support
- Balance presets for vanilla-plus, balanced, overhaul, power-fantasy, and custom setups
- Multiple durability-maintenance paths through Mending, material repair, and passive recovery
- Configurable anvil, mending, XP, and roll behaviour
- Silent Gear and Mobs Tool Forging virtual material tags, capacity bonuses, part-aware routing, final-tool conflict-priority orbs, and dormant-over-limit handling when equipment materials change
- A JEI enchantment information category with compatible items, affinities, limits, summaries, and datapack-provided guide notes
- Optional Apothic Enchanting support, including Apothic table stats, clues, arcana/quanta weighting, infusion offers, and datapack-driven infusion modifier requirements

## Supported versions

- Minecraft 1.21.1
- NeoForge 21.1.234
- Java 21

## Build

```sh
./gradlew build
```

The built jar is written to `build/libs/`.

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

New enchantments should follow [docs/new-enchantment-checklist.md](docs/new-enchantment-checklist.md), including ordinary affinity tags with one primary role, an item target, player-facing guide text, and dormant-behaviour checks for incompatible or over-limit stacks.

The config option `enchanting.enhanced_table_takeover` controls whether vanilla enchanting tables open Better Enchanting's integrated interface. The Arcane Crucible always distills essence, while the Attunement Pedestal upgrades enchantments already present on an item or a selected modular part.

## Minecraft Beyond Integration

Minecraft Beyond supplies the full local-mod test matrix: Better Enchanting integrates Apothic table stats into the persistent vanilla-table interface, the Arcane Crucible owns deterministic essence distillation, Mobs Tool Forging routes enchantments through modular parts and materials, and MoreWeapons contributes weapon-family targets and part routes. The pack also merges Better Enchanting's global loot modifiers with its other injected loot systems.

## Known issues

- Pre-alpha balance is not final.
- Public release jars are not published yet.
- Screenshots/GIFs are still provisional and need a final in-game capture pass.
- The mod changes anvil, mending, XP, and enchanting behavior, so compatibility testing with other enchanting/anvil overhaul mods is still required.
- Apothic support uses reflection to avoid a hard dependency; if Apothic internals change, the integration should fail closed and fall back instead of crashing.

## License

MIT. See [LICENSE](LICENSE).
