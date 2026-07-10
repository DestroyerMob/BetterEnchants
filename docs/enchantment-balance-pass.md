# Enchantment Balance Pass

Status: source-data balance pass for pre-alpha defaults. This pass reviews the current data pack files, config defaults, source tags, fusion recipes, and acquisition routes. It is not a replacement for timed in-game survival playtesting, so measured notes should be added as those runs happen.

## Method

- Balance against default config values first. Wider configurability should not be used to hide weak defaults.
- Treat `balanced` as the shipping baseline. Other config presets are templates for different audiences, not replacements for measured default playtesting.
- Treat source tags as the player's main deterministic acquisition path.
- Treat enchanted books as targeted boosters, not the primary way to make missing source tags work.
- Evaluate durability systems as a three-way choice: Mending for XP convenience, material repair for resource certainty, and passive environmental repair for slow recovery.
- Vanilla exclusive-set tags are intentionally empty in this mod. Conflicts below only list Better Enchanting's active special conflicts.
- Stage labels: Early game, First diamonds, First enchanting setup, Nether, End, Post-game gear.
- Tier labels: Utility, Standard, Strong, Premium, Capstone, Risk.

## Default Adjustment Log

- Added `minecraft:looting`, `minecraft:luck_of_the_sea`, and `minecraft:lure` to the Treasure source tag so Essence of Fortune can deliberately target loot and fishing enchantments.
- Set `essence_power_bonus` to 0 for the raw custom default and serious presets so essences control roll contents without increasing roll strength. `power_fantasy` keeps an essence power bonus because that preset is intentionally generous.

## Enchantment Table

| Enchantment | Max level | Source tags | Expected acquisition stage | Intended power tier | Conflicts | Config values | Playtest notes |
| --- | ---: | --- | --- | --- | --- | --- | --- |
| Auto-Smelt | 1 | Fire | Nether | Strong utility | None | weight 2; cost 15-55; anvil 4 | Watch interaction with Fortune, Vein Miner, and XP loss from smelting ores. Keep as convenience, not ore duplication. |
| Beheading | 1 | Physical, Treasure | First diamonds | Trophy utility | None | weight 2; cost 15-55; anvil 6; `beheading.base_head_drop_chance=0.10`; `beheading.head_drop_chance_per_looting_level=0.05`; headshot band -0.10 to 0.20 | Swords and axes only. Requires a melee headshot killing blow. Looting now supports the same sword/axe pool so the synergy works on axes too. Verify PvP head profiles, charged-creeper duplicate avoidance, and unsupported mobs. |
| Headshot | 5 | Physical | First enchanting setup | Skill damage | None | weight 2; cost 14-75; anvil 4; `headshot.damage_bonus_per_level=0.10`; headshot band -0.10 to 0.20 | Ranged weapons only. Uses projectile impact location for bows, crossbow arrows, and thrown tridents; level 5 is +50% damage on confirmed headshots by default. Verify PvP readability and firework crossbow expectations separately. |
| Cinderstep | 5 | Fire | Nether | Utility mobility | None | weight 2; cost 12+5/75; anvil 4; `cinderstep.duration_ticks=60`; `cinderstep.speed_bonus_per_level=0.06` | Gives Fire a boot identity without reducing fire damage. Test lava/magma traversal and Nether combat escape value. |
| Curse of Fragility | 1 | Vitality, Curse | First enchanting setup | Risk | None | weight 1; cost 10-45; anvil 8; `curse_of_fragility.durability_damage_multiplier=2.0` | Bad roll in Vitality pools. Verify it feels meaningfully punishing without making cursed early tools instantly unusable. |
| Curse of Rebound | 1 | Physical, Curse | First enchanting setup | Risk | None | weight 1; cost 10-50; anvil 8; `curse_of_rebound.reflected_damage_ratio=0.25` | Good as a bad roll in Physical pools. Verify reflected damage is noticeable but not instantly lethal to careless players. |
| Flash Step | 1 | Mobility | First enchanting setup | Mobility utility | None | weight 2; cost 14-50; anvil 4; distance 4 blocks; cooldown 12 ticks | Leggings identity enchant. Test double-tap feel against vanilla sprint double-tap and confirm server-side collision checks prevent wall clipping. |
| Fortunes Touch | 5 | Mining | Post-game gear | Capstone | Fortune, Silk Touch | weight 1; cost 15+3/75; anvil 8; `fortunes_touch.secondary_drop_chance_per_level=0.1`, max 1.0 | Fusion-only feel is right. Main risk is becoming strictly better than both Fortune and Silk Touch too early. |
| Frostbite | 5 | Frost | First diamonds | Strong combat control | None | weight 2; cost 14+6/75; anvil 4; `frostbite.frost_ticks_per_level=25`; frozen duration 100 ticks | Gives Frost its first combat identity. Watch ranged chaining and whether 5 seconds of movement lock feels too oppressive in PvP. |
| Gelbound | 1 | Mobility | First enchanting setup | Strong utility | Seismic Cushion set | weight 3; cost 12-45; anvil 4 | Full fall immunity is powerful, but boot-only and disabled by Seismic Cushion. Watch how early Mobility essence appears from rabbits, phantoms, and shipwrecks. |
| Harvest | 5 | Vitality | First enchanting setup | Utility | None | weight 4; cost 10+5/75; anvil 4 | Strong quality-of-life enchant. Radius scaling to 9x9 at level 5 is high but mostly farm labor reduction. |
| Overcharged | 5 | Lightning, Defensive | Nether | Rare-event combat burst | None | weight 1; cost 18+6/75; anvil 6; `overcharged.duration_ticks=200`; strength max amplifier 4, regeneration max amplifier 2, speed max amplifier 0 | Lightning-triggered payoff for body armor. Test with Channeling setups and lightning rods; rare trigger can be strong, but should not become generic always-on defense. |
| Perfect Strike | 1 | Physical | First enchanting setup | Strong combat | None | weight 2; cost 16-55; anvil 4; ready 1.0, window 4 ticks, damage x2.0, cooldown variance -0.12 to 0.12 | Needs hands-on combat testing. Default should reward timing without making crit swords delete bosses. |
| Seismic Cushion | 5 | Mobility | Nether | Capstone utility | Gelbound, Feather Falling | weight 1; cost 18+6/75; anvil 6; `seismic_cushion.explosion_radius_per_level=1.0`; gated by `playerGriefing` | Very high griefing/blast utility. Keep rare and fusion-adjacent. Test server-safe defaults with `playerGriefing=false`. |
| Shocking | 1 | Lightning | First diamonds | Strong combat | None | weight 2; cost 15-50; anvil 4; duration 100 ticks; Shocked damage x1.2 | Lightning essence is not trivial, so x1.2 is reasonable. Check stacking with Perfect Strike and crits. |
| Sticky Grip | 1 | Vitality | First enchanting setup | Utility | None | weight 2; cost 10-45; anvil 4 | Prevents accidental drop-key tosses for durability-tagged items. Verify manual cursor throws from inventory still work and that inventory-slot Q throws are blocked. |
| Tree Capitator | 2 | Mining | First enchanting setup | Strong utility | Vein Miner | weight 2; cost 14+20/60+15; anvil 4; max logs 96; leaf radius 4; min natural leaves 4 | Level I fells the tree; level II replants a valid 1x1 or 2x2 sapling footprint. Natural-leaf checks keep this tied to generated trees. Test player-built log piles, giant trees, and modded log/sapling naming. |
| Vacuum | 1 | Void | First diamonds | Utility | None | weight 2; cost 12-50; anvil 4 | Good convenience. Void targeting should delay reliable access, but Endermen can make it appear before the End. |
| Vein Miner | 5 | Mining | First diamonds | Strong utility | Tree Capitator | weight 2; cost 12+4/75; anvil 6; `vein_miner.connected_blocks_per_level=16` | Level 5 means up to 80 blocks. Watch durability drain, server tick impact, and Fortune synergy. |
| Verdant Regrowth | 5 | Vitality | Early game | Premium utility | None | weight 2; cost 12+4/45; anvil 4; repair 1 per level every 1200 ticks, 600 in sunlight | Wood-only targeting keeps it distinct. Verify wrong-material dormancy prevents modular material/tool-head abuse. |
| Aqua Affinity | 1 | Vitality, Defensive | First enchanting setup | Utility | None | vanilla default; no BE numeric config | Source tags are fine. It should be easy but not crowd helmet rolls too much. |
| Bane of Arthropods | 5 | Physical | First enchanting setup | Standard combat | None | vanilla default; no BE numeric config | Vanilla damage exclusivity is removed. Watch if stacking all damage types makes this feel like free filler. |
| Binding Curse | 1 | Void, Curse | First enchanting setup | Risk | None | vanilla default; removed by Purification | Good risk entry for Void and curse cleanup testing. |
| Blast Protection | 5 | Defensive | First enchanting setup | Standard defense | None | weight 2; cost 5+6/75; anvil 4 | Armor exclusivity is removed by empty vanilla conflict tags. Watch stacked protections at post-game limits. |
| Breach | 5 | Physical | First diamonds | Strong combat | None | weight 2; cost 15+3/75; anvil 4 | Mace-specific. Removing damage conflicts is high power but item-limited. |
| Channeling | 1 | Lightning | First diamonds | Utility combat | None | vanilla default; no BE numeric config | Good Lightning identity. Weather gating naturally limits impact. |
| Density | 5 | Physical | First diamonds | Strong combat | None | vanilla default; no BE numeric config | Mace-specific. Watch with Breach and Wind Burst because damage exclusivity is relaxed. |
| Depth Strider | 5 | Mobility | First enchanting setup | Utility | None | weight 2; cost 10+5/75; anvil 4 | Level 5 is stronger than vanilla. Check whether it trivializes water travel before Nether. |
| Efficiency | 5 | Mining | First diamonds | Standard utility | None | vanilla default; no BE numeric config | Core Mining roll. Should be common enough without invalidating Vein Miner. |
| Feather Falling | 5 | Mobility, Defensive | First enchanting setup | Strong defense | Seismic Cushion set | weight 5; cost 5+6/75; anvil 2 | Level 5 plus Gelbound/Seismic interactions need boot-stack testing. |
| Fire Aspect | 5 | Fire | Nether | Strong combat | None | weight 2; cost 10+5/75; anvil 4 | Level 5 is a deliberate late Fire reward. Check burn time and loot disruption. |
| Fire Protection | 5 | Fire, Defensive | Nether | Standard defense | None | weight 5; cost 10+5/75; anvil 2 | Useful Nether bridge between Fire and Defensive pools. Watch with regular Protection stacking. |
| Flame | 1 | Fire | Nether | Standard combat | None | vanilla default; no BE numeric config | Good simple Fire roll. |
| Fortune | 5 | Mining | First diamonds | Premium utility | Fortunes Touch | weight 2; cost 15+3/75; anvil 4 | Level 5 is a major economy lever. Test diamond/ancient debris expectations carefully. |
| Frost Walker | 5 | Frost, Mobility, Treasure | First enchanting setup | Premium utility | None | weight 2; cost 10+5/75; anvil 4 | Three source tags make this widely reachable. Verify that is intentional for exploration identity. |
| Impaling | 5 | Physical | First diamonds | Standard combat | None | vanilla default; no BE numeric config | Trident-specific. No immediate concern. |
| Infinity | 1 | Treasure | Post-game gear | Premium utility | None | vanilla default; no BE numeric config | Treasure-only targeting is appropriate. Check with Mending because vanilla bow conflict is removed. |
| Knockback | 5 | Physical | First enchanting setup | Standard combat | None | weight 5; cost 5+6/75; anvil 2 | Level 5 may be annoying more than powerful. Check player feel. |
| Looting | 5 | Treasure | First enchanting setup | Premium utility | None | weight 2; cost 15+3/75; anvil 4 | Now targetable by Essence of Fortune and supported on swords/axes for Beheading synergy. Watch mob-drop economy at levels 4-5. |
| Loyalty | 5 | Lightning | First diamonds | Utility combat | None | weight 5; cost 12+4/75; anvil 2 | Trident-specific. High levels mostly improve return speed. |
| Luck of the Sea | 5 | Treasure | First enchanting setup | Utility economy | None | weight 2; cost 15+3/75; anvil 4 | Now targetable by Essence of Fortune. Check treasure flow if fishing is common on servers. |
| Lure | 5 | Treasure | First enchanting setup | Utility economy | None | weight 2; cost 15+3/75; anvil 4 | Now targetable by Essence of Fortune. Watch if paired with Luck of the Sea accelerates essence gain too much. |
| Mending | 5 | Vitality, Treasure | Post-game gear | Capstone sustain | None | weight 2; cost 5+6/75; anvil 4; chance 1/10 to 1/6; repair 2 per level | Five-level Mending is a defining balance choice. Test repair rate with XP farms before release. |
| Multishot | 1 | Void | First diamonds | Standard combat | None | vanilla default; no BE numeric config | Void targeting makes crossbow crowd control feel slightly odd but acceptable. |
| Piercing | 5 | Physical | First diamonds | Standard combat | None | weight 10; cost 1+7/75; anvil 1 | Crossbow-specific. High weight means it should appear often in Physical crossbow pools. |
| Power | 5 | Physical | First enchanting setup | Standard combat | None | vanilla default; no BE numeric config | Bow baseline. Removing Infinity conflict should be watched elsewhere. |
| Projectile Protection | 5 | Defensive | First enchanting setup | Standard defense | None | weight 5; cost 3+6/75; anvil 2 | Armor exclusivity removed. Check if stacked protection makes ranged threats irrelevant. |
| Protection | 5 | Defensive | First enchanting setup | Standard defense | None | weight 10; cost 1+7/75; anvil 1 | High weight baseline. Watch with other protections because conflict tags are empty. |
| Punch | 5 | Physical | First enchanting setup | Standard combat | None | weight 2; cost 12+4/75; anvil 4 | Level 5 may be more disruptive than useful. Check PvP/server feel if relevant. |
| Quick Charge | 3 | Lightning | First diamonds | Standard combat | None | vanilla default; no BE numeric config | Good Lightning crossbow option. |
| Respiration | 5 | Vitality, Defensive | First enchanting setup | Utility | None | weight 2; cost 10+5/75; anvil 4 | Level 5 is a strong water-exploration reward. Fine if helmet capacity remains meaningful. |
| Riptide | 3 | Mobility | First diamonds | Premium mobility | None | vanilla default; no BE numeric config | Mobility identity is good. Check with Loyalty because vanilla riptide conflicts are removed. |
| Sharpness | 5 | Physical | First enchanting setup | Standard combat | None | vanilla default; no BE numeric config | Core Physical roll. Main issue is stacking with other damage enchants. |
| Silk Touch | 1 | Mining | First diamonds | Premium utility | Fortunes Touch | vanilla default; no BE numeric config | Fusion ingredient. Keep clear tooltip/fusion path. |
| Smite | 5 | Physical | First enchanting setup | Standard combat | None | vanilla default; no BE numeric config | Watch stacked damage families. |
| Soul Speed | 5 | Mobility, Treasure | Nether | Premium mobility | None | weight 1; cost 10+5/75; anvil 8 | Nether identity fits. Level 5 could be very fast, but terrain-specific. |
| Sweeping Edge | 5 | Physical | First enchanting setup | Standard combat | None | weight 2; cost 5+6/75; anvil 4 | Check with Perfect Strike on swords. |
| Swift Sneak | 5 | Mobility, Treasure | End | Premium mobility | None | weight 1; cost 25+1/75; anvil 8 | Ancient-city style reward. Treasure tag could make it easier, so watch early Essence of Fortune routes. |
| Thorns | 5 | Defensive | First enchanting setup | Strong defense | None | weight 1; cost 10+5/75; anvil 8 | Full armor Thorns V is likely dangerous. Needs durability and reflected-damage testing. |
| Unbreaking | 5 | Vitality | First enchanting setup | Premium sustain | None | weight 5; cost 5+6/75; anvil 2 | Key sustain enchant. Watch with Mending V and Verdant Regrowth. |
| Vanishing Curse | 1 | Void, Curse | First enchanting setup | Risk | None | vanilla default; removed by Purification | Good Purification test case. |
| Wind Burst | 3 | Mobility, Treasure | End | Premium mobility combat | None | vanilla default; no BE numeric config | Mace-specific End/post-game reward. Watch with Seismic Cushion and Density. |

## Stage Playtest Matrix

| Stage | Default setup | Expected active tags | What to test | Source-data result | Measured notes |
| --- | --- | --- | --- | --- | --- |
| Early game | No table or a 0-shelf table if the player rushes one; wood/stone/iron gear; sparse essences from crucible distillation, shipwrecks, rabbits, or early trades | Mobility, Physical, Mining, Frost, Treasure are possible but unreliable | Does the system still work without essences? Are low-power rolls understandable? Is the crucible formula guide sufficient? Can Verdant Regrowth appear only on valid wood items? | Pass with watch item. The table can function early, but deterministic control should feel weak until essences/books appear. Verdant Regrowth is the main early identity hook. | TODO in-game |
| First diamonds | Diamond pickaxe and partial shelves; first mining-focused rolls; possible mineshaft/shipwreck/chest essences | Mining, Treasure, Mobility, Physical | Are Fortune V, Vein Miner, and Efficiency attainable too early? Does gold +1 capacity create interesting but not mandatory choices? | Watch. Mining power is the largest economy risk. Vein Miner level 5 and Fortune level 5 should be checked before release. | TODO in-game |
| First enchanting setup | 15 shelves, lapis, mixed iron/diamond gear, a few essences/books | Defensive, Physical, Mining, Vitality, Mobility | Are three offers readable? Do essence restrictions make the pool feel deliberate? Are over-limit and wrong-tag inactive indicators understandable? | Pass. UI now explains mode/tags/modifiers/pool. Main risk is too many level 5 vanilla upgrades entering ordinary progression. | TODO in-game |
| Nether | Fire essence routes from blazes, magma cubes, Nether fortress, bastions; Soul Speed starts to matter | Fire, Defensive, Mobility, Treasure | Do Fire and Nether mobility rewards feel stage-appropriate? Does Auto-Smelt trivialize resource processing? | Pass with watch item. Fire source tag is well staged. Auto-Smelt needs ore/drop economy observation. | TODO in-game |
| End | Enderman/End city Void and Mobility routes; better chance at Treasure-style gear | Void, Mobility, Treasure | Does Vacuum arrive at a sensible time? Are Swift Sneak, Wind Burst, Mending, and Infinity too easy through Treasure? | Watch. Treasure is intentionally broad and now includes loot/fishing enchants, so Essence of Fortune must stay scarce enough. | TODO in-game |
| Post-game gear | Max shelves, curated essences, book boosters, anvil merging, fusion recipes, Silent Gear or Mobs Tool Forging material swaps | All tags | Can players build exciting gear while capacity limits still bite? Do fusion recipes respect capacity and inactive ordering? Are modular materials prevented from carrying wrong-tag bonuses? | Pass with watch item. Capacity/inactive behavior protects material swapping, but stacked vanilla protection and damage families need combat testing. | TODO in-game |

## Durability Route Tests

The release question is not whether Mending is weak or strong in isolation. The question is whether each repair route has a situation where it feels like the sensible choice.

| Repair route | Should feel good when | Watch for |
| --- | --- | --- |
| Mending | The player is actively adventuring, XP is naturally coming in, and the item is valuable enough to deserve long-term sustain | Do not over-nerf it into "bad vanilla Mending"; it still needs to feel worth taking |
| Material repair | The player has spare repair materials, wants immediate durability back, or is maintaining lower/mid-tier gear | Resource cost should matter, but the removed XP tax should make repair feel understandable rather than punitive |
| Verdant Regrowth | The player is around their base, idling near growth, farming, sorting inventory, or topping up nature-themed gear | Keep it slow enough that it is passive sustain, not dominant idle repair |

## Release-Gate Questions

- Does Fortune V plus Vein Miner create resource gains that force every server to retune defaults?
- Does Mending V repair too quickly with common XP farms?
- Does each durability route have a clear moment where it is the sensible choice?
- Does removing vanilla damage/protection exclusivity make post-game combat flat rather than interesting?
- Does Essence of Fortune become too broad now that it covers Looting, Luck of the Sea, and Lure?
- Are Tree Capitator's natural-leaf checks enough to keep it distinct from Vein Miner?
- Does Perfect Strike feel learnable at 4 ticks, or is the default window too punishing?
