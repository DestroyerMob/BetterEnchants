# Enhanced Enchanting Guidebook

## 1. Overview and Vision

**Working name:** Better Enchanting

**Current mod id:** `betterenchanting`

**Goal:** Turn the vanilla enchanting table into a deterministic, player-driven crafting experience. Players sculpt the enchantment pool with item tags, essences, and enchanted books, with clear feedback and controlled randomness. The focus is meaningful choice instead of pure RNG or spam rerolling.

## 2. Design Pillars

- Tags on enchantments and items define compatibility and affinity.
- Essences restrict or boost the enchantment pool.
- Enchanted books add targeted weight toward known enchantments.
- Enchanting offers should be stable enough to prevent free reroll abuse.
- Item type still matters, but player sculpting should dominate the feel.
- Mending, material repair, and passive environmental repair should each solve durability differently instead of making Mending the universal best answer.
- Data packs should be able to tune tags, essences, and loot distribution.

## 3. Current Implementation

The project implements an enhanced enchanting flow as a NeoForge `1.21.1` mod.

- By default, vanilla enchanting tables open the same essence-aware UI, so the essence system is part of the normal enchanting-table workflow.
- If `enchanting.enhanced_table_takeover` is disabled, vanilla enchanting tables are left alone and the Arcane Crucible block opens the enhanced UI instead.
- `EnhancedEnchantingMenu` provides slots for one target item, lapis, and three total pool modifiers that can be essences or enchanted books.
- The three modifier slots hold up to three pool modifiers. Physical slot position does not choose which offer a modifier affects.
- `EnchantingRoller` computes deterministic roll previews from the player enchantment seed, selected option, target item, essences, and books.
- `EssenceDefinitions` loads essence behavior from `data/betterenchanting/better_enchanting/essences/*.json`, falling back to Java defaults.
- `EnchantmentLimitRules` loads item enchantment limits from `data/betterenchanting/better_enchanting/enchantment_limits/*.json`.
- Item and enchantment tags under `data/betterenchanting/tags` drive compatibility and affinity.
- Complete subtype sets are simplified for display: all armor pieces show as Armor, complete tool groups show as Tool, and complete weapon groups show as Weapon.
- Global loot modifiers inject essence drops into selected vanilla loot tables.
- Client tooltips show item, essence, and enchantment affinity tags. Do not show enchantment target tags on enchanted items.
- The custom GUI renders the vanilla enchanting-table texture directly, with a small attached side pocket for three modifier slots to the right of the enchantment options and extra pool details kept in option tooltips.
- The enchanting UI should stay visually close to vanilla by deriving its background from `textures/gui/container/enchanting_table.png`; do not change block or item textures unless that is explicitly requested.

## 4. Core Mechanics

### Tag System

Every enchantment should have one or more affinity tags, such as Fire, Frost, Lightning, Mining, Vitality, Mobility, Physical, Void, Treasure, and Curse.

Base items should have target tags, such as armor, weapons, tools, pickaxes, bows, swords, tridents, and similar subtypes. These tags establish the starting compatibility pool.

### Enhanced Enchanting Table GUI

Required slots:

- Main item
- Lapis
- Pool modifiers, currently 3 total slots

Each pool modifier slot accepts either an essence or an enchanted book. Do not split this into separate essence and book slot banks unless the design explicitly changes.

Keep the modifier slots out of the inventory label area. The current placement is a compact vertical column beside the three vanilla offer rows. Do not use a custom widened enchanting-table texture for this; render the vanilla texture and draw the small modifier pocket separately. The side pocket should keep even slot padding: 3px left, 3px right, 3px top, and 3px bottom inside the border. Keep the vanilla title and inventory labels visible.

Modifier contents are treated as an unordered set. The menu builds a deterministic modifier plan from the player enchantment seed and the modifier identities, then rolls those modifiers into the three offer slots. Moving the same essence or enchanted book to a different physical modifier slot should not reveal a different biased offer. Empty planned offer slots roll from the normal item/bookshelf pool.

The current implementation uses menu-local inventory state because it modifies the vanilla enchanting table rather than adding a separate block. Longer term, persistent offer caching should be stored through a world/client-safe mechanism that does not require a custom block.

### Pool Generation and Options

Pool generation should combine:

- Target item compatibility and target tags
- The planned direct modifier assigned to that offer, if any
- Any global modifier effects that apply to all offers
- Modifier essence affinity tags, pool restriction behavior, or book-provided enchantment boosts
- Bookshelf power as the main quality and cost driver
- The mod's relaxed compatibility rules

The current implementation shows three independent options. Each option is deterministic from the player enchantment seed and option index, but pool sculpting comes from the seeded modifier plan, not from the physical modifier slot position.

The Crucible no longer presents vanilla-style tiered power levels. All three offers use the same base XP level cost, derived from bookshelf power:

- 0 bookshelves gives a very low base cost and low roll power.
- 15 bookshelves gives the maximum base cost and maximum roll power.
- Essences control which enchantments can roll. Enchanted books and item material can provide small roll-power boosts, but bookshelves remain the main controllable power source.

This means players choose between low-power cheap experimentation and high-power expensive rolls, while still getting three independent spins at the chosen power.

Multi-enchant rolls should prefer synergistic combinations that represent more of the slotted essence tags. The current implementation already applies a new-tag combo multiplier after the first selection.

Vanilla-style mutual exclusivity is intentionally removed across the board so players can combine enchantment families that normally exclude each other, such as multiple weapon damage boosts. The narrow exception is Fortunes Touch: Fortune and Silk Touch are allowed to meet long enough to fuse into Fortunes Touch, and Fortunes Touch is mutually exclusive with Fortune and Silk Touch afterward.

### Books

Enchanted books should act as a strong but controlled signal:

- A book can include an enchantment in a restricted pool.
- Repeated matching books increase weight.
- The final offered level should be at least the strongest matching book level when the book boost wins.

### Essences

Essences should be data-driven and support:

- The item id
- One affinity tag
- A weight multiplier
- Whether the essence restricts the pool or only boosts matching options
- Whether the essence applies to all offers
- Whether the essence blocks one planned offer slot

Essences should stay single-affinity; hybrid two-tag essences are intentionally not part of the current design.

Current acquisition rules use about a 20% chance by default for direct mob, block, fishing, curing, and injected chest rolls.

- Essence of Fire: Blaze and Magma Cube drops, Nether fortress and bastion chests, and lava fishing hooks.
- Essence of Frost: Stray and Polar Bear drops, igloo/frozen-style chest routes, and shipwreck chest routes.
- Essence of Lightning: Charged Creepers killed during storms, plus lightning rod and copper crafting.
- Essence of Force: Iron Golem and Ravager drops, armorer villager trades, and simple crafting.
- Essence of Excavation: Ore mining with Fortune, mineshaft and stronghold chests, and simple crafting.
- Essence of Warding: Iron Golem, Ravager, and Warden drops, plus bastion and ancient city chests.
- Essence of Vitality: Successful zombie villager curing, cleric villager trades, and golden apple crafting.
- Essence of Motion: Phantom and Rabbit drops, shipwreck/end city chest routes, and simple crafting.
- Essence of the Void: Enderman drops, with an extra End-dimension bonus roll, plus End city and ancient city chests.
- Essence of Fortune: Buried treasure, underwater ruin and elder guardian/ocean monument routes, Luck of the Sea fishing, librarian trades, and crafting.
- Essence of Purification: Successful zombie villager curing.

Essence of Purification is a special modifier defined through essence behavior flags. It applies to all offers, removes enchantments tagged `minecraft:curse` from the remaining active pools, and blocks one planned offer slot. If the player enchants from a cleaned remaining roll, the purification essence is consumed along with the chosen roll's own direct modifier.

### Enchantment Limits

Enchanting limits are data-driven and loaded from `data/betterenchanting/better_enchanting/enchantment_limits/*.json`.

Default rules:

- Global base maximum: 6 enchantments
- Armor base maximum: 4 enchantments
- Weapon base maximum: 4 enchantments
- Tool base maximum: 3 enchantments
- Gold material bonus: +1 enchantment

Type limits apply only when they are lower than the global base. If an item matches multiple types, such as an axe matching both weapon and tool tags, the strictest matching type limit wins. Material bonuses are applied after the base limit, so gold tools default to 4, gold weapons default to 5, and gold armor defaults to 5.

Current JSON shape:

```json
{
  "global_max": 6,
  "type_limits": {
    "armor": 4,
    "weapon": 4,
    "tool": 3
  },
  "material_bonus": {
    "gold": 1
  }
}
```

Material bonus keys map to item tags. For example, `"gold": 1` uses `#betterenchanting:materials/gold`. Full namespaced tag ids may also be used. Optional `item_limits` entries can set explicit base limits for individual item ids before material bonuses are applied.

Material tags are defined for vanilla tool, weapon, and armor materials under `data/betterenchanting/tags/item/materials/`, including wood, stone, iron, gold, diamond, netherite, leather, chainmail, turtle, copper, heavy core, and prismarine.

Silent Gear tools, weapons, and armor are also treated as having virtual Better Enchanting material tags based on the primary material of their main part. No item tag JSON is required for those virtual tags. A Silent Gear item with a `silentgear:mythril` head/main part resolves as `#betterenchanting:materials/mythril` and `#betterenchanting:materials/silentgear/mythril` for target mappings, testing, material bonuses, and current-material checks. Coatings do not change the material tag used here.

Silent Gear virtual material tags can apply `material_bonus` capacity increases. Enchantments that fit only because of that material bonus are marked as bonus-capacity enchantments in the tooltip. If the Silent Gear head or armor part is swapped and the new material no longer provides the bonus, those later enchantments become over-limit and dormant instead of being deleted.

### Common Config

General mechanics are configured through the NeoForge common config file `betterenchanting-common.toml`.

Default values:

```toml
[general]
preset = "balanced"
use_advanced_config_values = false

[anvil]
max_cost = 30
enchantment_level_merge = "additive"

[enchanting]
enhanced_table_takeover = true
max_bookshelf_power = 15
min_base_cost = 1
max_base_cost = 30
base_cost_per_bookshelf_power = 2
min_level_cost = 1
max_level_cost = 3
bookshelf_power_per_level_cost = 5
lapis_cost = 1
essence_power_bonus = 0
book_power_bonus = 2
gold_material_power_bonus = 1
book_weight_multiplier = 8.0
new_tag_combo_multiplier = 3.0
max_candidate_weight = 1000000

[vein_miner]
connected_blocks_per_level = 16

[tree_capitator]
max_logs = 96
leaf_scan_radius = 4
min_natural_leaves = 4

[perfect_strike]
ready_threshold = 1.0
window_ticks = 4
damage_multiplier = 2.0
min_cooldown_variance = -0.12
max_cooldown_variance = 0.12

[shocked]
damage_multiplier = 1.2
particle_type = "minecraft:electric_spark"
particles_enabled = true
particle_interval_ticks = 2
particle_count = 3
particle_horizontal_spread = 0.35
particle_vertical_spread = 0.75
particle_speed = 0.03

[shocking]
duration_ticks = 100

[frostbite]
frost_ticks_per_level = 25
frozen_duration_ticks = 100

[cinderstep]
duration_ticks = 60
speed_bonus_per_level = 0.06

[overcharged]
duration_ticks = 200
strength_max_amplifier = 4
regeneration_max_amplifier = 2
speed_max_amplifier = 0

[beheading]
base_head_drop_chance = 0.10
head_drop_chance_per_looting_level = 0.05
headshot_upper_eye_band = 0.20
headshot_lower_eye_band = -0.10

[headshot]
damage_bonus_per_level = 0.10
upper_eye_band = 0.20
lower_eye_band = -0.10

[curse_of_rebound]
reflected_damage_ratio = 0.25

[curse_of_fragility]
durability_damage_multiplier = 2.0

[seismic_cushion]
explosion_radius_per_level = 1.0

[verdant_regrowth]
base_repair_interval_ticks = 1200
fast_repair_interval_ticks = 600
durability_repaired_per_level = 1
scan_horizontal_radius = 1
scan_vertical_radius = 1

[essence_acquisition]
direct_drop_chance = 0.2

[mending]
base_chance_denominator = 10
denominator_reduction_per_level = 1
min_chance_denominator = 1
durability_repaired_per_level = 2

[fortunes_touch]
secondary_drop_chance_per_level = 0.1
secondary_drop_max_chance = 1.0

[enchanting_rolls]
multi_enchant_roll_bound = 50
multi_enchant_level_divisor = 2
enchantability_divisor = 4
level_variance = 0.15

[experience]
curve = "exponential"
linear_xp_per_level = 7
```

The `general.preset` option is the normal-player control surface. Available presets are `vanilla_plus`, `balanced`, `overhaul`, `power_fantasy`, and `custom`. The default `balanced` preset matches the mod's current intended defaults. `vanilla_plus` is conservative and leaves the vanilla enchanting table alone by default. `overhaul` makes essences/books stronger and treats Better Enchanting as a core gear-progression system. `power_fantasy` is intentionally generous and not meant as a balanced default. `custom` makes the advanced values in the config file the source of truth.

When `general.use_advanced_config_values` is `false`, any preset other than `custom` fully controls balance values and the detailed sections act as the editable `custom` baseline. When it is `true`, the selected preset is ignored for balance and the advanced values below are used instead. This is not a per-field inheritance or overlay system. Cosmetic particle settings for Shocked are always read directly from config.

The effective `enchanting.enhanced_table_takeover` value defaults to `true` in the `balanced`, `overhaul`, and `power_fantasy` presets, so vanilla enchanting tables open the enhanced UI. In `vanilla_plus`, it defaults to `false`; vanilla enchanting tables are left alone and the Arcane Crucible block becomes the enhanced enchanting station instead. The Arcane Crucible shapelessly crafts from one enchanting table and can shapelessly craft back into one enchanting table.

Enhanced enchanting balance lives behind the effective balance layer rather than inline menu constants. Bookshelf power controls offer level requirements and roll quality through `min_base_cost`, `max_base_cost`, and `base_cost_per_bookshelf_power`; those values do not have to match the levels consumed. The actual charged XP levels use `min_level_cost`, `max_level_cost`, and `bookshelf_power_per_level_cost`, which default to 0-5 power costing 1 level, 6-10 costing 2 levels, and 11-15 costing 3 levels. In the serious presets, `essence_power_bonus` is 0 so essences control the pool without increasing roll strength. Modifier-specific power nudges live in `book_power_bonus` and `gold_material_power_bonus`, while `essence_power_bonus` remains available for custom configs and power-fantasy tuning. Candidate weighting is tuned through `book_weight_multiplier`, `new_tag_combo_multiplier`, and `max_candidate_weight`.

Custom enchantment behavior that affects player-facing balance also lives in config. Vein Miner size, Tree Capitator log and natural-leaf checks, Perfect Strike timing, damage, and cooldown variance, Shocked damage multiplier and particles, Shocking duration, Frostbite frost buildup and freeze duration, Cinderstep speed boost and duration, Curse of Rebound reflection ratio, Curse of Fragility durability damage multiplier, Seismic Cushion explosion size, Verdant Regrowth repair amount, timing, and scan radius, Mending repair math, Fortunes Touch secondary drop chance, and the core enhanced-enchanting roll formula can all be tuned without recompiling the mod.

Mending is intentionally not controlled by balance presets. It always uses the explicit `[mending]` config values, so the modded Mending behavior remains stable unless a pack or player edits that section directly.

Better Enchanting avoids making Mending the universal best durability solution. Mending, material repair, and passive environmental repair each solve durability in different ways: XP convenience, resource certainty, and slow passive recovery.

### Durability Maintenance

Better Enchanting treats durability as a small ecosystem instead of a single best enchantment. Vanilla repair often becomes either annoying before Mending or nearly irrelevant after Mending. This mod should give players several practical maintenance paths with different costs and rhythms.

| Repair method | Strength | Weakness | Best use case |
| --- | --- | --- | --- |
| Mending | Works anywhere XP is gained and sustains gear while playing | Costs enchantment space and is weaker than vanilla Mending | Long-term general sustain on valuable gear |
| Verdant Regrowth | Completely passive and free | Slow, environmental, and not useful mid-combat or deep mining | Idle/top-up repair and nature-themed gear |
| Material repair | Immediate, predictable, and no XP tax | Consumes real repair materials | Directly fixing tools, especially before high-value netherite gear |

The goal is not to make Mending bad. Mending should still feel good, convenient, and worth taking on items that deserve long-term sustain. It should simply stop being mandatory on every item. Material repair keeps resources meaningful without adding vanilla's XP/anvil punishment, and Verdant Regrowth fills a separate fantasy where nature slowly restores an item over time.

Better Enchanting also adds the `playerGriefing` game rule. It defaults to `true` and is intended as the player-caused counterpart to vanilla `mobGriefing`. Seismic Cushion uses it for block destruction: when `playerGriefing` is false, its explosion still happens but does not break blocks.

Event-driven essence acquisition uses `essence_acquisition.direct_drop_chance`, which defaults to 20%. Loot-table injected essence chances remain data-pack controlled. Essence villager trades are data-driven through `better_enchanting/essence_trades`.

Anvils no longer use vanilla's Too Expensive cutoff. Non-material anvil operations are capped to `anvil.max_cost`, while repairing a damaged item with its repair material costs 0 XP. Repairing an item with another item, such as pickaxe plus pickaxe, still costs XP and is capped normally.

Anvil enchantment level merging is configurable. `additive` makes matching enchantment levels add together, such as level 2 plus level 3 becoming level 5. `vanilla` keeps Minecraft's default behavior, where matching equal levels increase by one and mismatched levels keep the higher value.

The experience curve supports two modes. `exponential` keeps vanilla's increasing XP-per-level curve. `linear` makes every level require `experience.linear_xp_per_level` XP.

Mending has 5 levels by default. Each XP point rolls a repair chance: level 1 rolls 1 in 10, level 2 rolls 1 in 9, level 3 rolls 1 in 8, level 4 rolls 1 in 7, and level 5 rolls 1 in 6. Successful rolls repair 2 durability per Mending level by default.

### Datapack Customization

Better Enchanting keeps structured rules in datapack JSON rather than config when the rule is naturally a list, mapping, tag, or recipe.

Pack-facing data folders:

- `better_enchanting/essences/*.json`: essence item, affinity tag, weight multiplier, and special behavior.
- `better_enchanting/enchantment_limits/*.json`: global, type, item, and material enchantment limits.
- `better_enchanting/enchantment_fusions/*.json`: enchantment recipes such as Fortune plus Silk Touch into Fortunes Touch.
- `better_enchanting/essence_trades/*.json`: villager essence trades.
- `better_enchanting/enchantment_targets/*.json`: item-tag to enchantment-target-tag mappings.
- `better_enchanting/tag_simplification/*.json`: display simplification groups such as Helmet, Body Armor, Leggings, and Boots becoming Armor.
- `better_enchanting/tag_display/*.json`: visible tag labels and colors. This can be supplied as datapack data and as resource-pack assets; resource-pack assets keep client tooltips colored on multiplayer clients.

Essence definition example:

```json
{
  "item": "betterenchanting:purification_essence",
  "tags": ["betterenchanting:purification"],
  "weight_multiplier": 1.0,
  "restricts_pool": false,
  "removed_tags": ["minecraft:curse"],
  "applies_to_all_offers": true,
  "blocks_offer": true
}
```

`removed_tags` excludes enchantments matching the listed enchantment tag ids from the active offer pool. The older `removes_curses` flag is still accepted for datapack compatibility and behaves like `removed_tags: ["minecraft:curse"]`.

Essence trade example:

```json
{
  "trades": [
    {
      "profession": "minecraft:armorer",
      "level": 2,
      "essence": "betterenchanting:physical_essence",
      "emerald_cost": 8,
      "count": 1,
      "max_uses": 8,
      "xp": 10,
      "price_multiplier": 0.05
    }
  ]
}
```

Target mapping example:

```json
{
  "rules": [
    {
      "item_tag": "my_pack:steel_tools",
      "enchantment_tag": "betterenchanting:targets/tools"
    }
  ]
}
```

Silent Gear material targets are generated dynamically from the same main-part material id. A `silentgear:mythril` head/main part adds the enchantment target tags `#betterenchanting:targets/materials/mythril` and `#betterenchanting:targets/materials/silentgear/mythril` when the item is rolled in the enchanting table. The virtual material item tags also pass through `better_enchanting/enchantment_targets/*.json`, so an existing rule such as `#betterenchanting:materials/wood` to `#betterenchanting:targets/wood` works for Silent Gear wood heads. Enchantments can be placed in either dynamic material tag even if that Silent Gear material is not present in the current pack; if no matching material exists, nothing matches and no error is raised.

Better Enchanting target-tagged enchantments are also checked when their gameplay level is queried. If an item no longer resolves any target tag that the enchantment belongs to, that enchantment is dormant and returns level 0 for gameplay effects. Enchantments without Better Enchanting target tags fall back to the item's normal supported-enchantment check, so vanilla and modded enchantments also stop working when placed on unsupported items.

If an item has more currently valid enchantments than its current enchantment limit allows, the later enchantments in the visible tooltip order are dormant and return level 0. Fusion outputs are already applied by Better Enchanting's enchanting and anvil paths before this check is shown to players, so fused results count as the enchantment actually present on the item. This prevents Silent Gear part swaps from keeping a material-only bonus active after the material changes. For example, Verdant Regrowth can remain on a swapped tool visually, but it only repairs durability while the tool currently resolves the wood target tag.

Client tooltips color enchantment names by their dominant affinity/tag display color. Enchanted non-book items also show their current valid enchantment count and effective limit, such as `Enchantments: 5/5 (base 4)`. Enchantments that fit only because of material bonus capacity are italicized while still active, so players can see which enchantment would become dormant if the material changed. Dormant enchantments are struck through and marked with a reason such as `[Wrong tag]` or `[Over limit]`. Enchantments that are dormant because they exceed the current effective enchantment limit are also italicized.

Testing command: `/itemtags reroll` recomputes the held item's current Better Enchanting virtual material tags and target tags and prints them to chat. It is a debug/admin command and does not store tags on the item.

Tag display example:

```json
{
  "enchantment_tags": [
    {
      "tag": "my_pack:storm",
      "label": "Storm",
      "color": "#8fd8ff"
    }
  ]
}
```

Verdant Regrowth uses tags for its environmental checks. By default, the growth-block scan is intentionally close range: 1 block horizontally and 1 block vertically, so the player needs to be standing on or immediately beside living growth. The default biome tag is empty; packs can add biomes if they want whole-biome healing.

- Growth blocks: `data/<namespace>/tags/block/verdant_regrowth_growth_blocks.json`
- Verdant biomes: `data/<namespace>/tags/worldgen/biome/verdant_regrowth_biomes.json`
- Harvest crops: `data/<namespace>/tags/block/harvest_crops.json`

### Implemented Custom Enchantments and Effects

- Vacuum is a Void enchantment that moves finalized drops into the player's inventory. If inventory space runs out, leftovers drop at the player's feet.
- Auto-Smelt is a Fire enchantment for harvestable tools. It transforms finalized block drops through smelting recipes so it works after drop modifiers such as Fortune.
- Cinderstep is a 5-level Fire enchantment for boots. When the wearer takes damage tagged as fire damage, such as lava, campfires, magma blocks, or being on fire, they gain a short Cinderstep movement speed boost. The default boost is +6% movement speed per level for 3 seconds.
- Shocked is a harmful status effect that makes affected living entities take 20% more damage from incoming damage by default. It hides the vanilla potion swirl and emits electric spark particles while active.
- Shocking is a Lightning weapon enchantment that applies Shocked for 5 seconds by default when the enchanted weapon deals damage.
- Overcharged is a 5-level Lightning and Defensive enchantment for body armor. When the wearer is struck by lightning, they gain Strength, Regeneration, and Speed for 10 seconds by default. The lightning strike is not canceled; Overcharged rewards the rare strike rather than replacing lightning damage prevention.
- Frostbite is a 5-level Frost weapon enchantment. Damaging hits add vanilla frozen ticks until the target reaches its freeze threshold, then apply Frozen for 5 seconds by default. Frozen targets are tinted blue, held at the freeze threshold, and have their movement, flying speed, and jump strength reduced to zero while remaining affected by knockback and gravity. Frostbite does not add more frost while Frozen is active.
- Beheading is a single-level Physical and Treasure enchantment for swords and axes. When a player directly kills a supported mob or another player with a melee headshot from the enchanted weapon, Beheading can add one matching head drop. The default chance is 10%, plus 5% per active Looting level on the killing weapon, capped at one head. Player heads preserve the defeated player's profile. Looting supports the same sword and axe item pool so this synergy works on both Beheading weapon types.
- Headshot is a 5-level Physical enchantment for ranged weapons. When a player lands a projectile headshot with an enchanted bow, crossbow, or trident, the final damage is increased by 10% per level by default. The valid headshot band and per-level bonus are configurable.
- Curse of Fragility is a single-level Vitality and Curse enchantment for damageable items. After vanilla durability processing such as Unbreaking, it multiplies the final durability damage by `curse_of_fragility.durability_damage_multiplier`, which defaults to 2.0.
- Curse of Rebound is a Curse affinity enchantment for weapons. When a player damages a non-player living target with a cursed weapon, 25% of the final damage dealt is reflected back to the player as thorns-style damage by default.
- Gelbound is a single-level Mobility enchantment for boots. It negates fall damage and bounces the player upward like a slime block; sneaking suppresses the bounce and allows normal fall damage.
- Seismic Cushion is a 5-level recipe-only Mobility enchantment for boots, created by combining Feather Falling and Gelbound. It inherits the Feather Falling level. Sneak-landing negates fall damage and creates a level-scaled explosion at the player; the player is the explosion source and is excluded from self-damage. Block destruction is controlled by the `playerGriefing` game rule.
- Verdant Regrowth is a 5-level Vitality enchantment for tools and armor that also targets the wood target. Equipped or held enchanted items slowly repair near tagged growth blocks, with optional whole-biome healing available through data packs; sunlight uses the faster configured repair interval. Higher levels increase durability repaired per repair tick, not the repair interval.
- Vein Miner is a Mining enchantment for harvestable tools. It has 5 levels and breaks up to 16 connected matching blocks per level by default.
- Tree Capitator is a single-level Mining enchantment for axes. Breaking a valid tree log cuts connected matching logs from that tree, up to the configured limit. It only activates when the connected log group has enough nearby natural, non-persistent leaves, so player-built log piles do not qualify. Sapling-grown trees still qualify because their leaves are generated as natural leaves.
- Perfect Strike is a single-level Physical enchantment for swords and axes. When the weapon reaches full attack readiness, a short configurable window opens; landing a direct hit in that window applies the configured damage multiplier after normal critical-hit damage. Each successful Perfect Strike weapon hit applies a small random temporary attack-speed variance for the next cooldown, so the next timing window is slightly less predictable.
- Fortunes Touch is a Mining enchantment created by combining Fortune and Silk Touch. It consumes both ingredients, acts like Silk Touch for the primary block drop, and can add the ordinary non-Silk drop as a secondary roll.
- Fortunes Touch inherits the level of the Fortune ingredient used to create it. Its secondary ordinary-drop roll chance is 10% per level: level 1 = 10%, level 2 = 20%, level 3 = 30%, and so on, capped at 100%.
- Harvest is a 5-level Vitality enchantment for hoes. Right-clicking a mature tagged crop harvests it with the enchanted hoe, drops the crop loot, and resets the crop to its youngest age. Each level expands the square area by two blocks: level 1 = 1x1, level 2 = 3x3, level 3 = 5x5, level 4 = 7x7, and level 5 = 9x9.

### Enchantment Fusion Recipes

Enchantment fusion recipes are data-driven JSON files loaded from `data/<namespace>/better_enchanting/enchantment_fusions/*.json`.

Default Fortunes Touch recipe:

```json
{
  "ingredients": [
    "minecraft:fortune",
    "minecraft:silk_touch"
  ],
  "result": {
    "enchantment": "betterenchanting:fortunes_touch",
    "level": {
      "type": "ingredient",
      "enchantment": "minecraft:fortune"
    }
  }
}
```

Default Seismic Cushion recipe fuses Feather Falling plus Gelbound and inherits the Feather Falling level.

The `result.level` rule can be a constant number, an enchantment id string, `{ "type": "ingredient", "enchantment": "<id>" }`, `{ "type": "sum_ingredients" }`, `{ "type": "max_ingredient" }`, or `{ "type": "min_ingredient" }`. Fusion results are treated as recipe outputs and are kept mutually exclusive with their ingredients by the enhanced enchanting roller. Ingredients in the same fusion recipe are allowed to meet even if another exclusivity rule would normally clash, so the recipe can complete.

General enchantment exclusivity is datapack-driven through the vanilla `exclusive_set` field on enchantment JSON. Because Better Enchanting empties vanilla's broad exclusive-set tags, pack makers can add focused custom exclusivity tags without restoring the old blanket conflicts.

## 5. Reroll and Anti-Exploit Design

Desired behavior:

- Offers should not reroll for free by removing and re-adding the same inputs.
- A visible **Resculpt** or **Reroll Offers** action should refresh offers within the current sculpted pool.
- Reroll should cost XP and possibly lapis or a small consumable.
- Cost may scale with option power, consecutive rerolls, or the number of active modifiers.
- Slot changes may provide live preview, but stable offer caching should prevent abuse.

Implementation status:

- Current previews are deterministic from the player enchantment seed and inputs.
- There is no dedicated reroll button yet.
- There is no persistent BlockEntity offer cache yet.

## 6. Visual Feedback

Desired behavior:

- Each offer should show a colored border, glow, or icon based on its dominant affinity tag.
- Hovering should show dominant and secondary tag breakdowns.
- Multi-tag offers should show multiple color pips or another compact multi-tag indicator.
- Essence changes should visibly alter pool size, mode, and likely offer colors.

Implementation status:

- Current GUI keeps the vanilla option rows and adds pool size, active essence tag count, and book boost count to the option tooltip.
- The option tooltip counts are per-roll so modifiers in other slots do not appear to affect the hovered spin.
- Tooltip support exists for item, essence, and enchantment affinity tags. Target tags should stay out of enchanted item tooltips.
- Enchantment clue names in option hover text are colored by dominant affinity tag.

## 7. Constraints and Compatibility

- Keep Minecraft's identity intact: swords, hoes, armor, tools, and books should feel different.
- Avoid vanilla-style reroll spam.
- Pool calculation must stay fast and cacheable.
- Data packs should be able to add or retune essences and tags.
- Compatibility with vanilla and modded enchantments matters; optional tag entries should be preferred where possible.
- Balance should make XP costs meaningful without making experimentation feel punishing.
- The mod should stay focused on enchanting unless a later feature explicitly expands it into a broader ability system.

## 8. Technical Architecture Direction

Current architecture:

- Optional Arcane Crucible block path for compatibility when vanilla enchanting table takeover is disabled
- Custom `MenuType`
- Custom `AbstractContainerMenu`
- Custom `AbstractContainerScreen`
- Data-driven essence definitions
- Data-driven essence villager trades
- Tag-driven item and enchantment filtering
- `PoolModifierRules` owns modifier-slot classification, per-roll modifier lists, and modifier consumption.
- `EnchantingPowerRules` owns bookshelf cost and roll-power math.
- `EnchantmentTargetTags` loads item tag to enchantment target tag mappings.
- `TagSimplifier` loads display simplification groups.
- `TagDisplayRules` loads tag labels and colors used by tooltips and option clue names.

Target architecture:

- Add persistent state for cached offers without forcing the vanilla enchanting table to own extra block state.
- Store target item, slotted essences/books, cached pool data, current offers, and reroll state.
- Keep a dedicated pool calculator class.
- Save persistent state through block entity NBT.
- Keep all balancing constants easy to tune.

## 9. Extensibility

Potential future additions:

- Ritual integration for creating essences or priming future enchanting modifiers.
- High-tier hybrid enchants or ability-like enchantments.
- Favorite setup saving.
- Config toggles for strict versus flexible item compatibility.
- Data-driven reroll cost formulas.

## 10. Open Decisions

- Reroll XP cost formula.
- Whether strict or flexible item-type restrictions should be the default.
- Whether offer updates should be live, locked, or require a commit/resculpt action.
- Whether the public-facing name should become Tagforge, Veilweaver Enchanting, or remain Better Enchanting.

## 11. Near-Term Fix List

- Add persistent offer state and cached offers.
- Add a Resculpt/Reroll Offers button with cost.
- Move any new special-case balance into data or config when it becomes player-visible.
- Keep build configuration machine-neutral; do not commit personal `org.gradle.java.home` paths.
