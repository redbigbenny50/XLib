# Grants, Items, and Recipes

This page covers the main non-combat unlock systems that XLib manages for addon authors.

## Contextual Grants

Context providers answer "grant this while condition X is true."

Typical examples:

- in a biome
- in a dimension
- while a status effect is active
- while wearing an armor set
- while a mode is active

Example:

```java
ContextGrantApi.registerProvider(BuiltInContextProviders.biomePack(
        ResourceLocation.fromNamespaceAndPath("yourmod", "desert_pack"),
        ResourceLocation.fromNamespaceAndPath("yourmod", "context/desert_pack"),
        ResourceLocation.fromNamespaceAndPath("minecraft", "desert"),
        builder -> builder.grantAbility(sandBurstId)
));
```

## Granted Items

Granted items are XLib-managed inventory items tied to ownership state.

Use them for:

- quest rewards
- class or mode gear
- revoke-safe cleanup
- undroppable bound items
- external-storage policy enforcement

Example:

```java
GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(boundRelicId, Items.NETHER_STAR)
        .undroppable()
        .blockExternalStorage()
        .build());
```

Grant it with a stable source id:

```java
GrantedItemGrantApi.grant(player, boundRelicId, ResourceLocation.fromNamespaceAndPath("yourmod", "quest/relic_trial"));
```

Available storage policies:

- `allowExternalStorage()`
- `reclaimFromOpenStorage()`
- `blockExternalStorage()`

`blockExternalStorage()` is proactive for normal player interaction paths: XLib guards open external slots and blocks stacked-on-other insertion attempts for currently-owned managed items with that policy.

Public events:

- `XLibGrantedItemEvent.Reclaimed`
- `XLibGrantedItemEvent.Removed`

## Recipe Restrictions

Recipe restriction is now broader than exact recipe-id locking.

Use it when:

- a recipe should unlock from a quest, class, node, or mode
- a whole family of outputs should lock together
- the viewer should explain why the recipe is locked
- advancement completion should auto-unlock recipe access

Exact definition example:

```java
RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(recipeId)
        .unlockSource(ResourceLocation.fromNamespaceAndPath("yourmod", "quest/dragon_clear"))
        .unlockAdvancement(ResourceLocation.fromNamespaceAndPath("minecraft", "story/enter_the_nether"))
        .unlockHint(Component.literal("Defeat the Ender Dragon"))
        .hiddenWhenLocked(false)
        .build());
```

Selector rule example:

```java
RecipePermissionApi.registerRestrictedRule(RestrictedRecipeRule.builder(ruleId)
        .category(ResourceLocation.withDefaultNamespace("equipment"))
        .output(ResourceLocation.fromNamespaceAndPath("minecraft", "diamond_sword"))
        .unlockSource(ResourceLocation.fromNamespaceAndPath("yourmod", "quest/dragon_clear"))
        .unlockHint(Component.literal("Complete the Dragon Trial"))
        .hiddenWhenLocked(false)
        .build());
```

Selectors can match by:

- recipe tag
- category
- output item
- output tag
- optional output NBT

Grant access manually when you want source-tracked control:

```java
RecipePermissionApi.grant(player, recipeId, ResourceLocation.fromNamespaceAndPath("yourmod", "quest/dragon_clear"));
```

XLib resolves selector matches into a cache when recipe restrictions reload or mutate, so inspection, runtime checks, and admin commands do not rescan the full recipe set every time.

Public event:

- `XLibRecipePermissionEvent`

## JEI / EMI Integration

If JEI or EMI is present, XLib can integrate with them directly.

- JEI can hide locked recipes live and show visible-lock labels, hints, and unlock-source tooltips.
- EMI can show lock labels, hints, and unlock-source tooltips for locked recipes.

If another addon wants to build its own viewer bridge, `JeiEmiRecipeViewerAddon` remains the dependency-free integration surface.
