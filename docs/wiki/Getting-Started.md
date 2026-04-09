# Getting Started

This page is the quickest path from "I added XLib as a dependency" to "my addon can register and equip an ability."

## Core Concepts

- Ownership and slot assignment are separate.
- Ownership answers whether the player has access to an ability, passive, recipe, or managed item.
- Slot assignment answers which combat slot an owned ability is currently equipped to.
- Most XLib systems are source-tracked, so grants should use stable source ids such as `yourmod:class/saiyan` or `yourmod:quest/dragon_clear`.

If you want the full shipped-systems tour before building against one specific API, read [System Overview and Status](System-Overview-and-Status.md) next. It summarizes the current runtime loop, client surfaces, and the major systems that already ship in the repo.

## Minimal Ability Flow

Register abilities during normal mod setup before XLib registries freeze.

```java
ResourceLocation kamehamehaId = ResourceLocation.fromNamespaceAndPath("yourmod", "kamehameha");

AbilityApi.registerAbility(AbilityDefinition.builder(kamehamehaId, AbilityIcon.ofTexture(
        ResourceLocation.fromNamespaceAndPath("yourmod", "textures/ability/kamehameha")
))
        .cooldownTicks(100)
        .action((player, data) -> {
            // Apply your gameplay effect here.
            return AbilityUseResult.success(data);
        })
        .build());
```

Grant and assign it later:

```java
AbilityGrantApi.grant(player, kamehamehaId, ResourceLocation.fromNamespaceAndPath("yourmod", "class/saiyan"));
AbilityLoadoutApi.assign(player, 0, kamehamehaId);
```

## Recommended Mental Model

- `AbilityApi` registers what exists.
- `AbilityGrantApi` decides what the player owns.
- `AbilityLoadoutApi` decides what is equipped.
- `AbilityRuntime` decides whether activation actually succeeds.

## Reading Order

- Read [System Overview and Status](System-Overview-and-Status.md) when you want the whole-mod architecture snapshot first.
- Read [Abilities and Loadouts](Abilities-and-Loadouts.md) for normal combat content.
- Read [Modes and Combos](Modes-and-Combos.md) for stances, forms, transformations, and combo follow-ups.
- Read [Grants, Items, and Recipes](Grants-Items-and-Recipes.md) when unlock state should come from quests, gear, trees, or recipes.
- Read [Progression](Progression.md) when you need points, counters, tracks, or nodes.
- Read [Events, Commands, and Testing](Events-Commands-and-Testing.md) before release or when debugging.

## Good Default Pattern

- Register content early.
- Add `family(...)`, `group(...)`, `page(...)`, and `tag(...)` metadata during ability, passive, mode, granted-item, restricted-recipe, or progression registration when you want XLib's APIs and built-in tools to reflect your addon's own organization.
- Grant ownership with stable source ids.
- Keep loadout logic separate from unlock logic.
- Only advertise built-in loadout management or the optional `Cycle Loadout` keybind when your addon actually wants those surfaces exposed.
- Treat progression, modes, and context systems as sources that feed into the same core grant systems rather than as special one-off state stores.
