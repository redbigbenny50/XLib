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

Nodes are unlock targets with requirements, costs, and reward bundles.

```java
UpgradeApi.registerNode(UpgradeNodeDefinition.builder(finalBloodArtNodeId)
        .track(vampiricTrackId)
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
- reward bundles for abilities, passives, items, and recipes

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
- point balances and counters
- node browsing
- reward and requirement inspection
- unlocking nodes from the client
- point-source hints through `upgrade_point.<namespace>.<path>.desc`

It is still a list/detail screen, not a graph-style tree canvas.
