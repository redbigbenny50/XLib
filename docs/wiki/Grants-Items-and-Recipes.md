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

## Shared Requirement Adapters

If you already authored a reusable `AbilityRequirement`, you can now adapt it directly into grant-related condition surfaces instead of rewriting the same boolean checks again.

```java
AbilityRequirement empowered = AbilityRequirements.all(
        AbilityRequirements.modeActive(stormFormId),
        AbilityRequirements.resourceAtLeast(focusGaugeId, 20)
);

ContextGrantApi.registerProvider(SimpleContextGrantProvider.builder(providerId, sourceId)
        .when(empowered)
        .grantAbility(tempestDriveId)
        .build());

GrantCondition unlockCondition = GrantConditions.fromRequirement(empowered);
```

Useful helpers:

- `GrantConditions.fromRequirement(...)`
- `GrantConditions.all(...)`
- `GrantConditions.any(...)`
- `GrantConditions.not(...)`
- `SimpleContextGrantProvider.Builder.when(AbilityRequirement)`

## Grant Bundles and Identities

`GrantBundleApi` is the reusable "power package" layer above the normal per-ability/passive/item/recipe grant APIs.

Use a bundle when one source should hand out a stable package of:

- abilities
- passives
- granted items
- recipe permissions
- activation blocks
- state policies
- state flags

Example:

```java
GrantBundleApi.registerBundle(GrantBundleDefinition.builder(bundleId)
        .grantAbility(stormDriveId)
        .grantPassive(auraPassiveId)
        .grantGrantedItem(boundRelicId)
        .stateFlag(originMarkerId)
        .build());
```

`IdentityApi` builds on top of state flags plus grant bundles for long-lived archetypes or inherited traits:

```java
IdentityApi.registerIdentity(IdentityDefinition.builder(originId)
        .grantBundle(bundleId)
        .inherits(parentOriginId)
        .build());
```

Useful helpers:

- `GrantBundleApi.registerBundle(...)`
- `GrantBundleApi.grantBundle(...)`
- `GrantBundleApi.syncSourceBundles(...)`
- `IdentityApi.registerIdentity(...)`
- `IdentityApi.grantIdentity(...)`
- `IdentityApi.revokeIdentity(...)`
- `IdentityApi.resolvedGrantBundles(...)`

Identity flags stay source-tracked, while their projected bundles use a dedicated projection source so inherited identity packages do not stomp unrelated direct bundle grants on the same player.

## Delegated Powers and Ownership Inspection

Use `DelegatedGrantApi` when one player or entity temporarily shares a bundle with another and needs to revoke it later.

```java
DelegatedGrantApi.grantBundle(grantorPlayer, targetPlayer, sharedBundleId);
DelegatedGrantApi.revokeBundle(grantorPlayer, targetPlayer, sharedBundleId);
```

For source inspection, `GrantOwnershipApi` classifies grant sources and explains why they are active.

Useful helpers:

- `GrantOwnershipApi.describeSource(...)`
- `GrantOwnershipApi.describeSources(...)`
- `/xlib debug source <player> <source>`
- `/xlib debug dump <player>`
- `/xlib debug export <player>`

The structured debug export now includes:

- `identities`
- `grant_bundles`
- `grant_bundle_sources`
- `artifact_unlock_sources`
- `identity_states`
- `artifact_states`
- `grant_bundle_states`
- `source_descriptors`

`GrantOwnershipApi` now also classifies support-package sources, so source-descriptor output can tell you which package applied the grant, which supporter granted it, and which bundle ids were projected through that package.

## Support Packages

Use support packages when one player or entity should project a reusable bundle package onto a linked ally or helper target.

```java
SupportPackageApi.registerSupportPackage(SupportPackageDefinition.builder(packageId)
        .grantBundle(sharedBuffBundleId)
        .relationship(ResourceLocation.fromNamespaceAndPath("yourmod", "ally"))
        .build());
```

Apply or revoke the package through the supporter:

```java
SupportPackageApi.apply(supporterPlayer, targetPlayer, packageId);
SupportPackageApi.revoke(supporterPlayer, targetPlayer, packageId);
```

Useful helpers:

- `SupportPackageApi.registerSupportPackage(...)`
- `SupportPackageApi.canApply(...)`
- `SupportPackageApi.apply(...)`
- `SupportPackageApi.revoke(...)`
- `SupportPackageDefinition.Builder.grantBundle(...)`
- `SupportPackageDefinition.Builder.relationship(...)`
- `SupportPackageDefinition.Builder.allowSelf()`

Support packages use the same source-tracked ownership model as the rest of XLib, so revoking the package removes its projected bundle grants cleanly instead of leaving orphaned abilities or items behind.

## Entity Relationships and Controlled Entities

`EntityRelationshipApi` is the low-level ownership or bond layer for addon-defined entity relationships such as master, summon_owner, ally, or bonded_target.

Useful helpers:

- `EntityRelationshipApi.setOwner(...)`
- `EntityRelationshipApi.clear(...)`
- `EntityRelationshipApi.ownerId(...)`
- `EntityRelationshipApi.matchesAny(...)`
- `EntityRelationshipApi.relatedEntities(...)`

`ControlledEntityApi` builds on top of that storage for summon, minion, companion, or pet designs that also need a simple command-state channel.

Useful helpers:

- `ControlledEntityApi.bind(...)`
- `ControlledEntityApi.release(...)`
- `ControlledEntityApi.controllerId(...)`
- `ControlledEntityApi.controlledEntities(...)`
- `ControlledEntityApi.setCommand(...)`
- `ControlledEntityApi.currentCommand(...)`
- `ControlledEntityApi.clearCommand(...)`

This layer is intentionally lightweight. XLib tracks ownership and the current command token, while addon code still decides what "follow", "guard", or "attack" actually means in its own AI or behavior systems.

## Artifacts and Access Policies

`ArtifactDefinition` and `ArtifactApi` are the authored layer above the older `AbilityGrantingItem` and `AbilityUnlockItem` helpers when an addon wants to treat item-driven access as an explicit framework concept.

Use artifacts for:

- relic-style items that project bundles while equipped or present in inventory
- permanent unlock items that should also expose a named artifact state
- menu or progression visibility tied to equipment, unlock state, or identity

Example:

```java
ArtifactApi.registerArtifact(ArtifactDefinition.builder(relicId)
        .itemId(ResourceLocation.withDefaultNamespace("nether_star"))
        .presence(ArtifactPresenceMode.MAIN_HAND)
        .equippedBundle(activeBundleId)
        .unlockedBundle(unlockedBundleId)
        .unlockOnConsume()
        .build());
```

Useful helpers:

- `ArtifactApi.registerArtifact(...)`
- `ArtifactApi.isActive(...)`
- `ArtifactApi.isUnlocked(...)`
- `ArtifactApi.unlock(...)`
- `ArtifactApi.revokeUnlock(...)`
- `AbilityRequirements.artifactActive(...)`
- `AbilityRequirements.artifactUnlocked(...)`
- `AbilityRequirements.identityActive(...)`
- `ProgressionMenuRequirements.artifactActive(...)`
- `ProgressionMenuRequirements.artifactUnlocked(...)`
- `ProgressionMenuRequirements.identityActive(...)`
- `ProgressionMenuRequirements.holding(...)`
- `ProgressionMenuRequirements.wearing(...)`

Equipped bundles are projected through an active artifact source while the item is present in the required location. Unlocked bundles are projected through a stored artifact-unlock source and survive after the consume/unlock moment until revoked.

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

## Granted Item Metadata

Granted items now share the same neutral `family/group/page/tag` metadata model as the rest of XLib's catalog surfaces.

```java
GrantedItemApi.registerGrantedItem(GrantedItemDefinition.builder(boundRelicId, Items.NETHER_STAR)
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/relics"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/trial_rewards"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/rare"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/bound"))
        .undroppable()
        .blockExternalStorage()
        .build());
```

Useful lookup helpers:

- `GrantedItemApi.grantedItemsInFamily(...)`
- `GrantedItemApi.grantedItemsInGroup(...)`
- `GrantedItemApi.grantedItemsOnPage(...)`
- `GrantedItemApi.grantedItemsWithTag(...)`

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

Broad lock example:

```java
RecipePermissionApi.registerRestrictedRule(RestrictedRecipeRule.builder(ruleId)
        .matchAll(true)
        .unlockSource(ResourceLocation.fromNamespaceAndPath("yourmod", "progression/base_crafting"))
        .hiddenWhenLocked(false)
        .build());
```

Selectors can match by:

- match all recipes
- recipe tag
- recipe namespace
- category
- output item
- output item tag
- optional output NBT

Rule overlap is explicit now:

- highest `priority` wins when multiple selector rules match the same recipe
- ties fall through to the later-matched rule
- `/xlib recipes inspect ...` shows the matched rule id and priority

Useful broad selectors for large packs:

- `recipeNamespace("modid")` to lock a whole mod's recipe namespace
- `outputItemTag(...)` to lock all recipes whose result item is in a shared item tag

Grant access manually when you want source-tracked control:

```java
RecipePermissionApi.grant(player, recipeId, ResourceLocation.fromNamespaceAndPath("yourmod", "quest/dragon_clear"));
```

XLib resolves selector matches into a cache when recipe restrictions reload or mutate, so inspection, runtime checks, and admin commands do not rescan the full recipe set every time. `matchAll(true)` still resolves once into that cache, which is the intended way to lock very large recipe sets.

Public event:

- `XLibRecipePermissionEvent`

## Recipe Catalog Metadata

Restricted recipe content now also supports neutral `family/group/page/tag` metadata.

Exact definition example:

```java
RecipePermissionApi.registerRestrictedRecipe(RestrictedRecipeDefinition.builder(recipeId)
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/trials"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/relics"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/endgame"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/boss_gate"))
        .unlockSource(ResourceLocation.fromNamespaceAndPath("yourmod", "quest/dragon_clear"))
        .build());
```

Selector rule example:

```java
RecipePermissionApi.registerRestrictedRule(RestrictedRecipeRule.builder(ruleId)
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/trials"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/relics"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/endgame"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/boss_gate"))
        .category(ResourceLocation.withDefaultNamespace("equipment"))
        .output(ResourceLocation.fromNamespaceAndPath("minecraft", "diamond_sword"))
        .build());
```

Important distinction:

- `tag(...)` is XLib catalog metadata for your own organization.
- `recipeTag(...)` is a selector that matches actual recipe tags while resolving restricted recipes.

Useful lookup helpers:

- `RecipePermissionApi.restrictedRecipesInFamily(...)`
- `RecipePermissionApi.restrictedRecipesInGroup(...)`
- `RecipePermissionApi.restrictedRecipesOnPage(...)`
- `RecipePermissionApi.restrictedRecipesWithTag(...)`

The built-in `/xlib recipes inspect ...` command now also prints this metadata alongside the existing recipe selectors and unlock data.

## JEI / EMI Integration

If JEI or EMI is present, XLib can integrate with them directly.

- JEI can hide locked recipes live and show visible-lock labels, hints, and unlock-source tooltips.
- EMI can show lock labels, hints, and unlock-source tooltips for locked recipes.

If another addon wants to build its own viewer bridge, `JeiEmiRecipeViewerAddon` remains the dependency-free integration surface.
