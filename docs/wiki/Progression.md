# Progression

This page covers XLib's optional point and node progression module.

## When To Use It

Use the progression layer when you want:

- skill trees
- mastery or class branches
- form progression
- spendable point systems
- item-consume or kill-driven unlock pacing

## Point Types

Register point types such as:

- `yourmod:meat_points`
- `yourmod:vampiric_points`
- `yourmod:ki_mastery_points`

```java
UpgradeApi.registerPointType(UpgradePointType.of(
        ResourceLocation.fromNamespaceAndPath("yourmod", "meat_points")
));
```

If you want the built-in UI to explain how a point type is earned, add a matching lang key:

```json
"upgrade_point.yourmod.meat_points.desc": "Earned by eating tagged meat foods."
```

## Tracks and Nodes

Tracks group progression paths, and nodes are the unlock targets with requirements, costs, and reward bundles.

```java
UpgradeApi.registerTrack(UpgradeTrackDefinition.builder(vampiricTrackId)
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/vampire"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/bloodline"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/advanced"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/lifesteal"))
        .rootNode(bloodMastery1Id)
        .build());
```

```java
UpgradeApi.registerNode(UpgradeNodeDefinition.builder(finalBloodArtNodeId)
        .track(vampiricTrackId)
        .family(ResourceLocation.fromNamespaceAndPath("yourmod", "family/vampire"))
        .group(ResourceLocation.fromNamespaceAndPath("yourmod", "group/bloodline"))
        .page(ResourceLocation.fromNamespaceAndPath("yourmod", "page/advanced"))
        .tag(ResourceLocation.fromNamespaceAndPath("yourmod", "tag/lifesteal"))
        .requiredNode(bloodMastery3Id)
        .pointCost(vampiricPointsId, 50)
        .requirement(UpgradeRequirements.advancement(
                ResourceLocation.fromNamespaceAndPath("minecraft", "end/kill_dragon"),
                Component.literal("Free the End")
        ))
        .rewards(UpgradeRewardBundle.builder()
                .grantAbility(finalBloodArtId)
                .build())
        .build());
```

Useful node features:

- point costs
- prerequisite nodes
- extra requirements
- choice groups for branch commitment
- explicit locked nodes and locked tracks
- reward bundles for abilities, passives, items, recipes, and identities

## Progression Metadata

Tracks and nodes now share the same neutral `family/group/page/tag` metadata model used by abilities and passives.

Useful lookup helpers:

- `UpgradeApi.tracksInFamily(...)`
- `UpgradeApi.tracksInGroup(...)`
- `UpgradeApi.tracksOnPage(...)`
- `UpgradeApi.tracksWithTag(...)`
- `UpgradeApi.nodesInFamily(...)`
- `UpgradeApi.nodesInGroup(...)`
- `UpgradeApi.nodesOnPage(...)`
- `UpgradeApi.nodesWithTag(...)`

The built-in progression menu keeps this metadata out of the default detail flow so the stock screen stays focused on costs, prerequisites, requirements, rewards, and branch commitment. Custom presentations can still opt into track metadata near the summary area with `ProgressionMenuPresentation.showTrackMetadata(...)` when an addon wants a more catalog-heavy browse mode.

## Composite Requirements

Progression requirements can now be composed instead of forcing every branch gate into one custom predicate.

```java
UpgradeNodeDefinition.builder(advanceNodeId)
        .track(nikoUltimateTrackId)
        .requirement(UpgradeRequirements.any(
                UpgradeRequirements.trackCompleted(offensiveTrackId),
                UpgradeRequirements.trackCompleted(defensiveTrackId)
        ))
        .requirement(UpgradeRequirements.anyNodeUnlocked(strikerNodeId, waterNodeId))
        .build();
```

Useful helpers:

- `UpgradeRequirements.all(...)`
- `UpgradeRequirements.any(...)`
- `UpgradeRequirements.trackCompleted(...)`
- `UpgradeRequirements.anyNodeUnlocked(...)`
- `UpgradeRequirements.identityActive(...)`

## Unlocking and Revoking

Unlock:

```java
UpgradeApi.unlockNode(player, nodeId);
```

Revoke a node:

```java
UpgradeApi.revokeNode(player, nodeId);
```

Revoke a track:

```java
UpgradeApi.revokeTrack(player, trackId);
```

Clear all progression:

```java
UpgradeApi.clearProgress(player);
```

When rewards came from nodes or tracks, prefer progression revoke paths over raw grant commands so XLib can clean up the projected reward sources correctly.

## Reward Projection

Progression rewards are projected back into the core grant systems instead of living in their own isolated state store.

That keeps progression compatible with:

- modes
- combos
- recipe unlocks
- granted items
- player-authored mode presets

Public events:

- `XLibUpgradeRewardProjectionEvent.Projected`
- `XLibUpgradeRewardProjectionEvent.Cleared`

`UpgradeRewardBundle` can now also project identities:

```java
UpgradeRewardBundle.builder()
        .grantIdentity(originId)
        .grantAbility(signatureMoveId)
        .build();
```

That makes it practical to treat a node as an archetype or lineage commitment marker and then gate later nodes on that identity instead of inventing a second parallel progression flag system.

Profile-backed starting nodes can now also project into progression through `ProfileDefinition.unlockStartingNode(...)`. Those nodes are tracked as managed unlock sources instead of manual player unlocks, so `ProfileApi.rebuild(...)`, resets, and debug/export flows can add or remove them cleanly.

## Branch Commitment and Specialization

Use choice groups and explicit locks when a tree should force meaningful specialization instead of acting like one long linear checklist.

```java
UpgradeApi.registerNode(UpgradeNodeDefinition.builder(flameOriginNodeId)
        .track(originTrackId)
        .choiceGroup(ResourceLocation.fromNamespaceAndPath("yourmod", "choice/origin"))
        .lockedNode(frostOriginNodeId)
        .rewards(UpgradeRewardBundle.builder()
                .grantIdentity(flameOriginId)
                .build())
        .build());

UpgradeApi.registerNode(UpgradeNodeDefinition.builder(flameMasteryNodeId)
        .track(originTrackId)
        .requiredNode(flameOriginNodeId)
        .requirement(UpgradeRequirements.identityActive(flameOriginId))
        .build());
```

Useful helpers:

- `UpgradeNodeDefinition.Builder.choiceGroup(...)`
- `UpgradeNodeDefinition.Builder.lockedNode(...)`
- `UpgradeNodeDefinition.Builder.lockedTrack(...)`
- `UpgradeApi.nodesInChoiceGroup(...)`
- `UpgradeApi.firstStructuralUnlockFailure(...)`
- `UpgradeRewardBundle.Builder.grantIdentity(...)`

The built-in progression screen now shows choice-group metadata, locked-node and locked-track summaries, and identity rewards in the selected-node details panel. Unlock buttons also respect structural branch locks before point spending or extra requirement checks run.

## Exclusive Tracks

Use `exclusiveWith(...)` on tracks when one branch should block another like a class choice.

## Earning Points

Consume rules:

```java
UpgradeApi.registerConsumeRule(UpgradeConsumeRule.builder(eatMeatRuleId)
        .itemTag(ResourceLocation.fromNamespaceAndPath("yourmod", "meat_foods"))
        .foodOnly()
        .awardPoints(meatPointsId, 1)
        .incrementCounter(meatEatenCounterId, 1)
        .build());
```

Consume rules can now also reuse existing `AbilityRequirement` objects directly when your point-gain gate should mirror combat or grant logic you already authored elsewhere.

```java
UpgradeApi.registerConsumeRule(UpgradeConsumeRule.builder(eatFocusedMeatRuleId)
        .foodOnly()
        .requirement(AbilityRequirements.modeActive(stormFormId))
        .requirements(List.of(AbilityRequirements.resourceAtLeast(focusGaugeId, 20)))
        .awardPoints(meatPointsId, 1)
        .build());
```

Useful helpers:

- `UpgradeConsumeRule.Builder.requirement(...)`
- `UpgradeConsumeRule.Builder.requirements(...)`

Kill rules:

```java
UpgradeApi.registerKillRule(UpgradeKillRule.builder(villagerVampirismRuleId)
        .target(EntityType.VILLAGER)
        .requiredAbility(vampiricDrainId)
        .awardPoints(vampiricPointsId, 2)
        .incrementCounter(villagerKillsCounterId, 1)
        .build());
```

If a kill rule depends on a specific ability, record the hit:

```java
AbilityCombatTracker.recordAbilityHit(player, target, vampiricDrainId);
```

## Built-In UI

The built-in progression screen supports:

- track switching and all-tracks view
- a top-row layout mode switcher for list and tree presentation
- list mode plus an icon-node skill-tree canvas for tree mode
- reward descriptions for ability/passive rewards on the selected node
- point balances and counters
- node browsing
- choice-group and branch-lock details
- reward and requirement inspection
- identity reward summaries
- unlocking nodes from the client
- addon-authored hidden/locked/available access states for the menu itself
- addon-defined presentation profiles through `ProgressionMenuPresentationApi`
- point-source hints through `upgrade_point.<namespace>.<path>.desc`
- shared session-state handoff for selected track, selected node, and layout mode through `ProgressionMenuSessionStateApi` and `ProgressionMenuScreenContext`

The tree layout now uses icon-based node tiles with explicit branch pathing, centered node placement, and wrapped labels under each node for a more classic skill-tree read. `LIST` remains the compact fallback for simple browsing. Both are still bounded menu presentations rather than free-pan or zoomable editor canvases, but addons can now tune tree node spacing, label width, label line count, and node size through `ProgressionTreeLayout` when they need larger names to breathe.

## Progression Menu Presentation Profiles

`ProgressionMenuPresentationApi` lets addons swap the built-in progression screen between different palette and layout policies without forking the screen class.

```java
ProgressionMenuPresentationApi.register(ProgressionMenuPresentation.builder(treeMenuId)
        .defaultLayoutMode(ProgressionNodeLayoutMode.TREE)
        .availableLayoutModes(Set.of(
                ProgressionNodeLayoutMode.LIST,
                ProgressionNodeLayoutMode.TREE
        ))
        .treeLayout(ProgressionTreeLayout.builder()
                .columnSpacing(160)
                .rowSpacing(108)
                .labelWidth(132)
                .maxLabelLines(4)
                .build())
        .showPointSourceHints(false)
        .build());

ProgressionMenuPresentationApi.activate(treeMenuId);
```

Use `availableLayoutModes(Set.of(ProgressionNodeLayoutMode.TREE))` for a tree-only built-in screen, `Set.of(ProgressionNodeLayoutMode.LIST)` for a list-only screen, or both when players should be allowed to switch. The built-in layout button only appears when more than one mode is exposed.

Use `treeLayout(...)` when your addon wants the stock tree view but needs wider spacing or more wrapping room for long node names instead of forking the whole screen.

Useful helpers:

- `ProgressionMenuPresentationApi.register(...)`
- `ProgressionMenuPresentationApi.activate(...)`
- `ProgressionMenuPresentationApi.find(...)`
- `ProgressionNodeLayoutMode.LIST`
- `ProgressionNodeLayoutMode.TREE`
- `ProgressionTreeLayout.builder()`
- `ProgressionLayoutPlanner.plan(...)`

## Progression Menu Access Policies

`ProgressionMenuAccessApi` gives addon authors the same hidden/locked/available policy surface for the built-in progression screen.

```java
ProgressionMenuAccessApi.registerPolicy(menuSourceId, ProgressionMenuAccessPolicy.builder()
        .visibleWhen(ProgressionMenuRequirements.counterAtLeast(ritualSeenCounterId, 1))
        .availableWhen(ProgressionMenuRequirements.pointsAtLeast(vampiricPointsId, 1))
        .build());
```

Useful helpers:

- `ProgressionMenuAccessApi.registerPolicy(...)`
- `ProgressionMenuAccessApi.clearPolicy(...)`
- `ProgressionMenuAccessPolicy.builder().visibleWhen(...)`
- `ProgressionMenuAccessPolicy.builder().availableWhen(...)`
- `ProgressionMenuRequirements.counterAtLeast(...)`
- `ProgressionMenuRequirements.pointsAtLeast(...)`
- `ProgressionMenuRequirements.nodeUnlocked(...)`
- `ProgressionMenuRequirements.trackCompleted(...)`
- `ProgressionMenuRequirements.predicate(...)`

When a progression menu policy resolves to locked, the screen still opens for browsing track and node information, but the unlock action is disabled until the availability requirements pass.
