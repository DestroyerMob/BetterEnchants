# Changelog

All notable changes to Better Enchanting will be documented in this file.

## Unreleased

- Added README, license, build workflow, and screenshot placeholder documentation for public repo presentation.
- Added pre-alpha project status and supported version notes.
- Documented current known issues and compatibility warnings.
- Added unit tests for deterministic modifier planning and overflow-safe weighted selection.
- Polished essence display names and moved tag tooltip section labels to translation keys.
- Expanded enhanced enchanting offer tooltips with mode, active tags, direct/global modifiers, book boosts, and pool size.
- Added a default-focused enchantment balance pass worksheet and routed loot/fishing enchantments through the Treasure affinity tag.
- Added balance presets with an effective balance layer for normal-player templates and advanced/custom config control.
- Renamed the preset escape-hatch config to `use_advanced_config_values` to make its all-or-nothing behavior explicit.
- Changed serious defaults so essences control the enchantment pool without adding roll power.
- Removed Mending values from balance presets so modded Mending always uses the explicit `[mending]` config.
- Documented the durability design split between Mending, material repair, and passive environmental repair, including tradeoffs and playtest targets.
- Italicized bonus-capacity and over-limit enchantment tooltip lines so extra enchantments are visually marked.
- Loaded enchantment limit and target-tag data on the client so item tooltips can mark wrong-tag and over-limit enchantments reliably.
- Added an enchanted item capacity tooltip so players can see the current count versus the effective enchantment limit.
- Applied material bonus capacity to Silent Gear virtual material tags, with bonus-slot tooltip marking to show what material swaps can disable.
- Added Curse of Fragility, a Vitality curse for damageable items that increases final durability damage after Unbreaking.
- Added Frostbite, a Frost weapon enchantment that stacks vanilla frozen ticks into a temporary Frozen immobilize effect.
- Added Cinderstep, a Fire boot enchantment that grants a short speed boost after taking fire damage.
- Added Overcharged, a Lightning and Defensive body-armor enchantment that grants Strength, Regeneration, and Speed after the wearer is struck by lightning.
- Added Beheading, a sword and axe enchantment that can add one head drop from melee headshot kills against supported mobs and players, with Looting increasing the drop chance.
- Allowed Looting on axes so Beheading's Looting synergy works across both supported weapon types.
- Added Headshot, a 5-level ranged weapon enchantment that increases projectile damage on confirmed headshots.
- Fixed a startup crash caused by Cinderstep reading common config values during mob-effect registration before NeoForge had loaded config.
- Allowed zero-enchantability tag-targeted tools such as shears and flint and steel to receive enhanced enchanting offers.
- Added built-in default target-tag rules so item target resolution works before datapack reloads provide custom rules.
- Added disabled-offer tooltip reasons for blocked modifiers, empty pools, item enchantment limits, fusion-capacity failures, level costs, and lapis costs.
- Added Enchantment Descriptions compatibility keys for all Better Enchanting enchantments.
