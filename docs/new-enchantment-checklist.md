# New Enchantment Checklist

Use this when adding a Better Enchanting enchantment.

1. Define the design: name, id, level count, target items, affinity tags, target tags, availability, exclusivity, and whether behavior needs config.
2. Add a `ResourceKey<Enchantment>` in `ModEnchantments`.
3. Add `data/betterenchanting/enchantment/<id>.json` with description, level range, costs, slots, `primary_items`, `supported_items`, and `exclusive_set` if needed.
4. Add `data/betterenchanting/tags/item/enchantable/<id>.json` for the allowed item pool.
5. Add the enchantment to the relevant affinity tag under `data/betterenchanting/tags/enchantment/`.
6. Add the enchantment to the relevant target tag under `data/betterenchanting/tags/enchantment/targets/`. For modular-material-specific enchantments such as Silent Gear or Mobs Tool Forging materials, use `targets/materials/<material_path>` or `targets/materials/<material_namespace>/<material_path>`; those tags can exist even when the material is not installed. Custom behavior must use gameplay enchantment-level checks such as `stack.getEnchantmentLevel(holder)` or `stack.getAllEnchantments(...)` so wrong-tag and over-limit enchantments become dormant.
7. Add vanilla behavior tags as appropriate: `minecraft:non_treasure`, `minecraft:tradeable`, `minecraft:tooltip_order`, and any focused exclusive-set tag.
8. Add the name translation key and a short player-facing `enchantment.betterenchanting.<id>.desc` description key in `assets/betterenchanting/lang/en_us.json` for Enchantment Descriptions compatibility.
9. If the enchantment has custom behavior, add a focused event class under `world/enchantment/`, register it in `BetterEnchanting`, and guard server-only behavior, permissions, recursion, block entities, and unbreakable blocks.
10. Put player-facing tuning values in `BetterEnchantingConfig` instead of hardcoding balance constants.
11. Update `docs/enhanced-enchanting-guidebook.md` with the implemented behavior and any data/config surface.
12. Verify with `./gradlew build`.
