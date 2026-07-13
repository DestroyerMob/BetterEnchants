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
- If `enchanting.enhanced_table_takeover` is disabled, vanilla enchanting tables are left alone. The Arcane Crucible remains a distillation station; it is not an alternate enchanting table.
- The Arcane Crucible has no container screen. Using a valid amethyst medium or catalyst directly on the block stores as much of that held stack as fits. Its synchronized world display floats the medium over the portal, draws catalysts inward as distillation advances, traces continuously filled progress runes around the rim, and floats completed essence above the process. Every displayed stack is ray-pickable: look at its cyan focus halo and use it with an empty hand to retrieve that exact slot, or sneak-use any display to retrieve everything. Recipes load from `data/betterenchanting/better_enchanting/distillation/*.json`.
- `EnhancedEnchantingMenu` provides slots for one target item, one required essence reagent, and three total modifier slots.
- The reagent opens the base enchantment pool. The three modifier slots can refine that pool with additional essence affinities or provide special modifiers such as enchanted books and purification.
- The Attunement Pedestal has no container screen. Using an enchanted target, essence, or Nether Star directly on it stores that input and displays it in-world. It upgrades one Focus-selected enchantment by exactly one level and borrows power from an enchanting table within four horizontal and two vertical blocks.
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

Every enchantment should have at least two ordinary affinity tags, such as Fire, Frost, Lightning, Mining, Vitality, Mobility, Physical, Void, and Curse-adjacent cleanup tags. Exactly one of those affinities is also marked primary under `data/<namespace>/tags/enchantment/primary/<affinity>.json`; the remaining ordinary affinities are secondary. Primary and secondary affinities are equally valid for essence reagents and modifiers. Only the primary affinity controls the enchantment's display color and per-affinity equipment limit. Curse enchantments still need a non-curse primary affinity so they can appear through ordinary reagents and be removed by purification.

Base items should have target tags, such as armor, weapons, tools, pickaxes, bows, swords, tridents, and similar subtypes. These tags establish the starting compatibility pool.

### Enhanced Enchanting Table GUI

Required slots:

- Main item
- Essence reagent
- Modifier slots, currently 3 total slots

The reagent slot accepts normal pool-restricting essences. Without a reagent, the table shows no enchantment offers. Modifier slots accept essences, enchanted books, and special essence modifiers. Nether Stars belong in the Attunement Pedestal instead of the enchanting table.

Keep the modifier slots out of the inventory label area. The current placement is a compact vertical column beside the three vanilla offer rows. Do not use a custom widened enchanting-table texture for this; render the vanilla texture and draw the small modifier pocket separately. The side pocket should keep even slot padding: 3px left, 3px right, 3px top, and 3px bottom inside the border. Keep the vanilla title and inventory labels visible.

Modifier contents are treated as an unordered set. Normal tagged essence modifiers refine the reagent pool globally, affecting all three offers instead of being assigned to one physical offer row. Enchanted books remain targeted modifiers in the deterministic modifier plan. Moving the same modifier to a different physical slot should not reveal a different biased offer.

The five enchanting inputs are stored on the vanilla enchanting-table block entity and saved to NBT. Closing the UI leaves them in the table; breaking the table drops them. The client-synchronized inventory also drives the in-world display: the target floats over the table, the reagent floats above it, and the three modifiers orbit the target. With Apothic Enchanting present, synchronized emerald, rose, and violet halo strands visualize Eterna, Quanta, and Arcana. The number of particles in each strand scales with the corresponding table stat, and the halo remains absent until the server confirms at least one usable enchantment offer.

### Interactive Enchanting Mode

`enchanting.interactive_mode` enables a complete in-world alternative to the GUI. It defaults to `false` in Better Enchanting itself so installing the mod does not replace the expected interface without consent. MinecraftBeyond enables it in both the active and distributed default config.

- Use a target item or reagent on the table to insert it.
- Sneak-use an essence, enchanted book, or other accepted modifier to insert it into the next modifier slot.
- Look at a displayed input and use it with an empty hand to retrieve that slot; sneak-use a display to retrieve every input.
- Hold the Attunement Focus in either hand to reveal the three offer orbs above the table. Look at one and use the Focus to perform that roll.
- Looking at the displayed target shows its current enchantments; retrieve displayed inputs with an empty hand as usual.
- Sneak-use the table with an empty hand to open the GUI as a maintenance fallback.

Offer state is calculated on the server from the player's real enchantment seed. Each orb uses the primary-affinity color of its first revealed enchantment. Apothic clue count is authoritative: zero clues produce a neutral mystery orb, partial clues list only what the shelves reveal, and a fully revealed roll says so explicitly. Orb color never leaks an enchantment that the current clue budget has hidden.

### Attunement Pedestal Upgrades

The enchanting table applies new enchantments; the pedestal raises enchantments the item already has. Use an enchanted item and matching essence directly on the pedestal, then hold the Attunement Focus. The first use on a world-space enchantment orb selects it and turns it gold; using that gold orb again performs the upgrade or reports the missing essence, catalyst, nearby table, or table power in the action bar. Modular tools expose their individual part items and part enchantment orbs, so an upgrade is written back to the exact selected part rather than whichever matching part happens to be found first. The target, essence, and catalyst displays are individually ray-pickable with an empty hand; sneak-use any display to retrieve every stored stack.

Each upgrade is deterministic and raises the selected enchantment by one level. It consumes a number of any matching-affinity essence equal to the resulting level: level I to II costs two, II to III costs three, and so on. Primary affinity remains the display color and limit category; every ordinary affinity on the enchantment is valid for payment. The pedestal charges no XP or lapis.

The pedestal finds the nearest enchanting table within four blocks horizontally and two blocks vertically and uses that table's existing bookshelf or Apothic Eterna power. Required power is three times the resulting level, capped at 30. A maxed multi-level enchantment can be raised once above its normal maximum if the item has no other overleveled enchantment; that final step also consumes one Nether Star. Single-level enchantments and already-overleveled items cannot be raised further.

### Pool Generation and Options

Pool generation should combine:

- Target item compatibility and target tags
- The reagent affinity tags
- Any refining essence tags or global special modifier effects
- The planned direct book modifier assigned to that offer, if any
- Modifier essence affinity tags, pool restriction behavior, or book-provided enchantment boosts
- Bookshelf power as the main roll-quality driver
- The mod's relaxed compatibility rules

The current implementation shows three deterministic options. Options are generated together, and later options exclude enchantments already offered by earlier options so the table does not present duplicate rolls.

The Crucible no longer presents vanilla-style tiered XP costs. All three offers use the same base roll power, derived from bookshelf power:

- 0 bookshelves gives very low roll power.
- 15 bookshelves gives the maximum configured roll power.
- Essences control which enchantments can roll. Enchanted books and item material can provide small roll-power boosts, but bookshelves remain the main controllable power source.

This means players choose between low-power experimentation and high-power rolls, while the reagent provides the actual enchanting cost.

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
- Whether the essence has special modifier behavior such as curse removal or modifier consumption

Essences should stay single-affinity; hybrid two-tag essences are intentionally not part of the current design.

Every essence has a deterministic Arcane Crucible formula using one amethyst shard as the medium. Novice Masons can sell two amethyst shards for four emeralds as a fallback route to the first geode; geodes remain the efficient bulk source. Current bonus-acquisition rules use about a 20% chance by default for direct mob, block, fishing, curing, and injected chest rolls.

The Attunement Focus also supports geode exploration. Shift-right-click scans the player's current chunk and its eight neighbours across the dimension's complete build height, without force-loading chunks. The search uses section palettes to skip irrelevant sections, emits a chunk-aligned Resonance pulse even on a miss, and highlights natural geodes through their unobtainable budding-amethyst blocks. The Focus has a 15-second cooldown. Tuning-orb interactions consume their click before item use, so tuning never starts the scan or its cooldown.

- Essence of Fire: blaze powder catalyst; also Blaze/Magma Cube drops, Nether chests, and lava fishing hooks.
- Essence of Frost: snowball catalyst; also Stray/Polar Bear drops and cold-region or shipwreck chests.
- Essence of Lightning: copper ingot plus lightning rod catalysts; also Charged Creepers killed during storms.
- Essence of Force: flint catalyst; also Iron Golem/Ravager drops and armorer trades.
- Essence of Excavation: lapis lazuli catalyst; also Fortune ore mining and mineshaft/stronghold chests.
- Essence of Warding: iron ingot catalyst; also Iron Golem, Ravager, and Warden drops plus bastion/ancient-city chests.
- Essence of Vitality: golden apple catalyst; also successful zombie-villager curing and cleric trades.
- Essence of Motion: feather catalyst; also Phantom/Rabbit drops and shipwreck/end-city chests.
- Essence of the Void: ender pearl catalyst; also Enderman drops, an End-dimension bonus, and end-city/ancient-city chests.
- Essence of Purification: quartz catalyst; also successful zombie-villager curing.

Essence of Purification is a special modifier defined through essence behavior flags. It applies to all offers, removes enchantments tagged `minecraft:curse` from the remaining active pools, and is consumed if the player enchants from the cleaned pool. Ordinary tagged essence refiners are not consumed; the reagent is consumed when an enchantment completes. Enchanted-book modifiers are consumed when their assigned offer is used. Nether Stars are only consumed by the Attunement Pedestal's overlevel step.

### Enchantment Limits

Enchanting limits are data-driven and loaded from `data/betterenchanting/better_enchanting/enchantment_limits/*.json`.

The common config option `enchanting.override_vanilla_enchantment_limits` controls whether these Better Enchanting limit systems are active. It defaults to `true`. When set to `false`, Better Enchanting's table and anvil paths clamp vanilla enchantments back to their known vanilla max levels and stop enforcing the custom per-item enchantment capacity limits.

Default rules:

- Global base maximum: 6 enchantments
- Armor base maximum: 4 enchantments
- Weapon base maximum: 4 enchantments
- Tool base maximum: 3 enchantments
- Gold material bonus: +1 enchantment
- Per-primary-affinity maximum: 2 enchantments

Type limits apply only when they are lower than the global base. If an item matches multiple types, such as an axe matching both weapon and tool tags, the strictest matching type limit wins. Material bonuses are applied after the base limit, so gold tools default to 4, gold weapons default to 5, and gold armor defaults to 5.

The per-affinity limit counts only each enchantment's `primary/<affinity>` marker. Secondary affinity membership continues to shape table pools through essences and modifiers but consumes no additional category allowance. For example, Auto-Smelt remains available from both Fire and Mining sources, but counts only as Fire; Efficiency and Fortune count as Mining. Legacy datapack enchantments without a primary marker retain the previous conservative behavior and count against all of their Better Enchanting affinity tags until classified.

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

Silent Gear tools, weapons, and armor are also treated as having virtual Better Enchanting material tags based on the primary material of their main part. Mobs Tool Forging modular tools use their head material the same way. No item tag JSON is required for those virtual tags. A Silent Gear item with a `silentgear:mythril` head/main part resolves as `#betterenchanting:materials/mythril` and `#betterenchanting:materials/silentgear/mythril`; a Mobs Tool Forging gold-headed tool resolves as `#betterenchanting:materials/gold` and `#betterenchanting:materials/mobstoolforging/gold`. These tags feed target mappings, testing, material bonuses, and current-material checks. Vanilla wood-family materials such as oak, spruce, birch, crimson, warped, bamboo, and similar variants are normalized to `#betterenchanting:materials/wood` instead of creating per-species wood tags. Silent Gear coatings and Mobs Tool Forging handles/bindings/wraps/foci/treatments do not change the primary material tag used here.

Virtual material tags can apply `material_bonus` capacity increases. Enchantments that fit only because of that material bonus are marked as bonus-capacity enchantments in the tooltip. If the modular head/main material or armor part is swapped and the new material no longer provides the bonus, those later enchantments become over-limit and dormant instead of being deleted.

### Common Config

General mechanics are configured through the NeoForge common config file `betterenchanting-common.toml`.

Default values:

```toml
[general]
preset = "balanced"
use_advanced_config_values = false

[anvil]
max_cost = 30

[enchanting]
enhanced_table_takeover = true
override_vanilla_enchantment_limits = true
max_bookshelf_power = 15
min_base_cost = 1
max_base_cost = 30
base_cost_per_bookshelf_power = 2
min_level_cost = 1
max_level_cost = 3
bookshelf_power_per_level_cost = 5
lapis_cost = 1 # legacy config; enhanced table completion now consumes one essence reagent
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

The effective `enchanting.enhanced_table_takeover` value defaults to `true` in the `balanced`, `overhaul`, and `power_fantasy` presets, so vanilla enchanting tables open the enhanced UI. In `vanilla_plus`, it defaults to `false` and vanilla enchanting tables are left alone. In every preset, the Arcane Crucible distills essence and never opens the enchanting UI. It crafts from a cauldron, five obsidian, and three amethyst shards. The Attunement Pedestal crafts from one amethyst shard, three gold ingots, and three obsidian.

Enhanced enchanting balance lives behind the effective balance layer rather than inline menu constants. Bookshelf power controls roll quality through `min_base_cost`, `max_base_cost`, and `base_cost_per_bookshelf_power`. The old level-cost fields remain in config for compatibility and the offer-tier icon, but enhanced table completion no longer charges XP levels or lapis. In the serious presets, `essence_power_bonus` is 0 so essences control the pool without increasing roll strength. Modifier-specific power nudges live in `book_power_bonus` and `gold_material_power_bonus`, while `essence_power_bonus` remains available for custom configs and power-fantasy tuning. Candidate weighting is tuned through `book_weight_multiplier`, `new_tag_combo_multiplier`, and `max_candidate_weight`.

Custom enchantment behavior that affects player-facing balance also lives in config. Vein Miner size, Tree Capitator log and natural-leaf checks, Perfect Strike timing, damage, and cooldown variance, Drawn Steel preparation and damage, Distant Edge distance and damage, Moonlit Reversal timing, damage, and posture, Shocked damage multiplier and particles, Shocking duration, Frostbite frost buildup and freeze duration, Cinderstep speed boost and duration, Curse of Rebound reflection ratio, Curse of Fragility durability damage multiplier, Seismic Cushion explosion size, Verdant Regrowth repair amount, timing, and scan radius, Mending repair math, Fortunes Touch secondary drop chance, and the core enhanced-enchanting roll formula can all be tuned without recompiling the mod.

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

Use `item_tag` when one tag is sufficient. Use `item_tags` when an item must match every listed tag (AND semantics):

```json
{
  "rules": [
    {
      "item_tags": [
        "betterenchanting:materials/copper",
        "betterenchanting:weapons/ranged"
      ],
      "enchantment_tag": "betterenchanting:targets/weapons/copper_ranged"
    }
  ]
}
```

Modular material targets are generated dynamically from the same primary material id. A `silentgear:mythril` head/main part adds the enchantment target tags `#betterenchanting:targets/materials/mythril` and `#betterenchanting:targets/materials/silentgear/mythril`; a Mobs Tool Forging `mobstoolforging:gold` head adds `#betterenchanting:targets/materials/gold` and `#betterenchanting:targets/materials/mobstoolforging/gold` when the item is rolled in the enchanting table. The virtual material item tags also pass through `better_enchanting/enchantment_targets/*.json`, so an existing rule such as `#betterenchanting:materials/wood` to `#betterenchanting:targets/wood` works for modular wood heads. Enchantments can be placed in either dynamic material tag even if that material is not present in the current pack; if no matching material exists, nothing matches and no error is raised.

Better Enchanting target-tagged enchantments are also checked when their gameplay level is queried. If an item no longer resolves any target tag that the enchantment belongs to, that enchantment is dormant and returns level 0 for gameplay effects. Enchantments without Better Enchanting target tags fall back to the item's normal supported-enchantment check, so vanilla and modded enchantments also stop working when placed on unsupported items.

If an item has more currently valid enchantments than its current enchantment limit allows, the later enchantments in the visible tooltip order are dormant and return level 0. Fusion outputs are already applied by Better Enchanting's enchanting and anvil paths before this check is shown to players, so fused results count as the enchantment actually present on the item. This prevents modular material swaps from keeping a material-only bonus active after the material changes. For example, Verdant Regrowth can remain on a swapped tool visually, but it only repairs durability while the tool currently resolves the wood target tag.

Client tooltips color enchantment names by their primary affinity's display color. Enchanted non-book items also show their current valid enchantment count and effective limit, such as `Enchantments: 5/5 (base 4)`. Enchantments that fit only because of material bonus capacity are italicized while still active, so players can see which enchantment would become dormant if the material changed. Dormant enchantments are struck through and marked with a reason such as `[Wrong tag]` or `[Over limit]`. Enchantments that are dormant because they exceed the current effective enchantment limit are also italicized.

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
- Harvest crops: `data/<namespace>/tags/block/harvest_crops.json`. The default tag includes `#minecraft:crops`; mature `CropBlock` implementations and age-bearing bonemealable bushes are also recognized as a compatibility fallback for mods that omit the shared tag.

### Implemented Custom Enchantments and Effects

- Vacuum is a Void enchantment that moves finalized drops into the player's inventory. If inventory space runs out, leftovers drop at the player's feet.
- Auto-Smelt is a Fire enchantment for harvestable tools. It transforms finalized block drops through smelting recipes so it works after drop modifiers such as Fortune.
- Cinderstep is a 5-level Fire enchantment for boots. When the wearer takes damage tagged as fire damage, such as lava, campfires, magma blocks, or being on fire, they gain a short Cinderstep movement speed boost. The default boost is +6% movement speed per level for 3 seconds.
- Shocked is a harmful status effect that makes affected living entities take 20% more damage from incoming damage by default. It hides the vanilla potion swirl and emits electric spark particles while active.
- Shocking is a Lightning weapon enchantment that applies Shocked for 5 seconds by default when the enchanted weapon deals damage.
- Overcharged is a 5-level Lightning and Defensive enchantment for body armor. When the wearer is struck by lightning, they gain Strength, Regeneration, and Speed for 10 seconds by default. The lightning strike is not canceled; Overcharged rewards the rare strike rather than replacing lightning damage prevention.
- Frostbite is a 5-level Frost weapon enchantment. Damaging hits add vanilla frozen ticks until the target reaches its freeze threshold, then apply Frozen for 5 seconds by default. Frozen targets are tinted blue, held at the freeze threshold, and have their movement, flying speed, and jump strength reduced to zero while remaining affected by knockback and gravity. Frostbite does not add more frost while Frozen is active.
- Beheading is a single-level Physical and Treasure enchantment for swords and axes. When a player directly kills a supported mob or another player with a melee headshot from the enchanted weapon, Beheading can add one matching head drop. The default chance is 10%, plus 5% per active Looting level on the killing weapon, capped at one head. Player heads preserve the defeated player's profile. Looting supports the same sword and axe item pool so this synergy works on both Beheading weapon types.
- Flash Step is a single-level Mobility enchantment for leggings. Double-tapping the forward keybind sends a server-validated blink request; if the enchantment is active, the player teleports up to 4 blocks horizontally in the direction they are facing. The server clamps the destination before wall collisions, checks for a safe standing volume, and applies a short cooldown.
- Headshot is a 5-level Physical enchantment for ranged weapons. When a player lands a projectile headshot with an enchanted bow, crossbow, or trident, the final damage is increased by 10% per level by default. The valid headshot band and per-level bonus are configurable.
- Curse of Fragility is a single-level Vitality and Curse enchantment for damageable items. After vanilla durability processing such as Unbreaking, it multiplies the final durability damage by `curse_of_fragility.durability_damage_multiplier`, which defaults to 2.0.
- Curse of Rebound is a Curse affinity enchantment for weapons. When a player damages a non-player living target with a cursed weapon, 25% of the final damage dealt is reflected back to the player as thorns-style damage by default.
- Gelbound is a single-level Mobility enchantment for boots. It negates fall damage and bounces the player upward like a slime block; sneaking suppresses the bounce and allows normal fall damage.
- Seismic Cushion is a 5-level recipe-only Mobility enchantment for boots, created by combining Feather Falling and Gelbound. It inherits the Feather Falling level. Sneak-landing negates fall damage and creates a level-scaled explosion at the player; the player is the explosion source and is excluded from self-damage. Block destruction is controlled by the `playerGriefing` game rule.
- Sticky Grip is a single-level Vitality enchantment for durability-tagged items. The normal drop key and inventory-slot throw key are ignored while it is active, but players can still pick the item up on the cursor and throw it outside the inventory manually.
- Verdant Regrowth is a 5-level Vitality enchantment for tools and armor that also targets the wood target. Equipped or held enchanted items slowly repair near tagged growth blocks, with optional whole-biome healing available through data packs; sunlight uses the faster configured repair interval. Higher levels increase durability repaired per repair tick, not the repair interval.
- Vein Miner is a Mining enchantment for harvestable tools. It has 5 levels and breaks up to 16 connected matching blocks per level by default.
- Tree Capitator is a 2-level Mining enchantment for axes. Both levels cut connected matching logs from a valid natural tree, up to the configured limit. Level II also resolves the tree's sapling or propagule from its log family and replants the lowest trunk footprint after a successful chop; four base logs are restored as a complete 2x2 sapling group. Replanting only occurs if every required position is replaceable, supported, inside the world border, and editable by the player. The enchantment only activates when the connected log group has enough nearby natural, non-persistent leaves, so player-built log piles do not qualify. Sapling-grown trees still qualify because their leaves are generated as natural leaves.
- Perfect Strike is a single-level Physical enchantment for swords and axes. When the weapon reaches full attack readiness, a short configurable window opens; landing a direct hit in that window applies the configured damage multiplier after normal critical-hit damage. Each successful Perfect Strike weapon hit applies a small random temporary attack-speed variance for the next cooldown, so the next timing window is slightly less predictable.
- Drawn Steel is a single-level Physical and Mobility enchantment for katanas. Keeping an enchanted katana continuously selected fills a HUD preparation bar; after the configured 40 ticks, the bar turns gold and an amethyst chime confirms that the server considers it ready. The next entity attack consumes the preparation, and a fully charged attack deals the configured 1.25x final damage. Changing the selected slot resets preparation. Drawn Steel and Perfect Strike are mutually exclusive.
- Distant Edge is a 3-level Physical and Mobility enchantment for katanas. A fully charged direct strike whose nearest target-hitbox point is at least three blocks from the player's eye deals 8%, 12%, or 16% additional final damage. A sharp strong-hit sound and critical sparks confirm the spacing bonus.
- Moonlit Reversal is a single-level Defensive and Physical katana enchantment integrated with Mobs Combat. Each successful server-confirmed parry creates one reversal during the full configured counter window. The first entity attack consumes it; if that attack is fully charged, it deals 1.2x final damage plus 6 flat slash posture damage and emits enchanted-hit sparks. Perfect blocks do not activate it, and it safely remains inactive when Mobs Combat is absent.
- Fortunes Touch is a Mining enchantment created by combining Fortune and Silk Touch. It consumes both ingredients, acts like Silk Touch for the primary block drop, and can add the ordinary non-Silk drop as a secondary roll.
- Fortunes Touch inherits the level of the Fortune ingredient used to create it. Its secondary ordinary-drop roll chance is 10% per level: level 1 = 10%, level 2 = 20%, level 3 = 30%, and so on, capped at 100%.
- Resonance is a Mining enchantment for pickaxes. Breaking an ore sends a visible reveal wave through matching nearby ores; each revealed block uses a pulsing cyan/violet outline, soft central glow, and expanding echo rings that remain visible through intervening blocks for the configured duration.
- Harvest is a 5-level Vitality enchantment for hoes. Right-clicking a mature vanilla or modded crop harvests it with the enchanted hoe, drops the crop loot, and resets the crop to its youngest age. Shared `#minecraft:crops` entries are supported directly, with safe class/property fallbacks for untagged mod crops. Each level expands the square area by two blocks: level 1 = 1x1, level 2 = 3x3, level 3 = 5x5, level 4 = 7x7, and level 5 = 9x9.

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
- Reroll should cost a small consumable or reagent-style resource.
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

- Persistent vanilla enchanting-table input inventory with synchronized in-world item rendering
- Persistent, screenless Arcane Crucible block entity with direct block interaction, synchronized world rendering, and data-driven distillation recipes
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

- Extend persistent state from stored inputs to cached offers and reroll state where that materially improves stability.
- Keep stored inputs synchronized without duplicating inventories in menu-only state.
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
