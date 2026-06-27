# Apothic Infusion Modifier Rules

Better Enchanting can optionally require items from its modifier slots when an Apothic Enchanting infusion recipe is offered in the integrated enchanting table. This is a datapack hook only. The mod does not ship default modifier requirements for Apothic infusion recipes.

Rule files go under:

```text
data/<namespace>/better_enchanting/apothic_infusion_modifiers/*.json
```

Each rule points at an existing Apothic infusion recipe id and lists the modifier-slot inputs required before that infusion can be applied.

```json
{
  "infusion": "apothic_enchanting:example_infusion",
  "modifiers": [
    "betterenchanting:fire_essence",
    { "tag": "betterenchanting:essences" }
  ],
  "consume": true
}
```

`modifiers` can contain item ids, `#tag_id` strings, `{ "item": "namespace:item" }`, or `{ "tag": "namespace:tag" }`. Each listed modifier must be present in a different Better Enchanting modifier slot. If `consume` is omitted, the matched modifiers are consumed when the infusion succeeds.

A file can also contain multiple rules:

```json
{
  "rules": [
    {
      "infusion": "apothic_enchanting:example_infusion",
      "modifier": "betterenchanting:fire_essence"
    }
  ]
}
```

If a rule exists and its modifiers are missing, the integrated table still shows the Apothic infusion offer, but the offer is disabled with a missing modifier message. If no rule exists for the matched infusion recipe, Apothic infusion behaves normally.
