# Capability Policies and Restrictions

This page covers XLib's capability-policy system: source-tracked restrictions that control what a player can do at runtime.

Use capability policies when your addon needs to restrict actions such as:

- opening inventory
- using the hotbar
- changing selected slots
- picking up or dropping items
- equipping armor
- using beds, furnaces, brewing stands, chests, or other workstations
- interacting with blocks or entities
- riding entities

XLib merges all active policies into one resolved state so multiple systems can impose restrictions without stomping each other.

## Core Pieces

- `CapabilityPolicyDefinition`
  Defines one named policy.
- `CapabilityPolicyApi`
  Registry plus apply/revoke/query helpers.
- `CapabilityPolicyData`
  Player attachment for source-tracked active policies.
- `ResolvedCapabilityPolicyState`
  Cached merged state used by runtime enforcement.
- `DataDrivenCapabilityPolicyApi`
  Datapack authoring path under `data/<namespace>/xlib/capability_policies/*.json`.

## Mental Model

The normal pattern is:

1. register or author one policy definition
2. apply it to a player from a specific source id
3. let XLib enforce the merged result through its hooks
4. revoke that same source later without disturbing unrelated policies

This is the right system for temporary lockouts, transformation restrictions, onboarding restrictions, possession states, and similar "you currently cannot do X" mechanics.

## Policy Axes

Current policy axes are:

- `InteractionPolicy`
- `InventoryPolicy`
- `MovementPolicy`
- `MenuPolicy`
- `CraftingPolicy`
- `EquipmentPolicy`
- `HeldItemPolicy`
- `ContainerPolicy`
- `PickupDropPolicy`

## Current Granularity

The newer capability-policy work widened the system past simple yes/no gates.

### Inventory and hotbar

Current inventory-level controls include:

- open personal inventory
- move items
- use hotbar
- use offhand
- change selected hotbar slot
- allow or block specific hotbar slots

### Held items and pickup

Current item-level controls include:

- allow or block held items by exact item id
- allow or block held items by item tag
- allow or block pickup by exact item id
- allow or block pickup by item tag

### Equipment

Current equipment-level controls include:

- equip armor
- unequip armor
- allow or block armor pieces by exact item id
- allow or block armor pieces by item tag

### Menus, containers, and workstations

Current workstation and menu controls include:

- open menus
- interact with open containers
- use crafting output
- use beds
- open brewing stands
- open or use workstation-style block menus such as furnaces or chests through the menu/container restriction path

### Block and entity interaction

Current interaction controls include:

- allow or block block interaction by exact block id
- allow or block block interaction by block tag
- allow or block entity interaction by exact entity id
- allow or block entity interaction by entity tag
- attack target filtering
- riding target filtering
- block break/place filtering through the same interaction selectors

Target entity selectors use merged real-plus-synthetic classification checks, so interaction policies can treat a player as if they count as a different mob or tag when synthetic classifications are active.

## Core API

Typical helpers:

```java
CapabilityPolicyApi.register(def);
CapabilityPolicyApi.apply(player, policyId, sourceId);
CapabilityPolicyApi.revoke(player, policyId, sourceId);
CapabilityPolicyApi.revokeSource(player, sourceId);
CapabilityPolicyApi.clearAll(player);

CapabilityPolicyApi.allows(player, check);
CapabilityPolicyApi.hasActivePolicy(player, policyId);
CapabilityPolicyApi.activePolicies(player);
CapabilityPolicyApi.resolved(player);
```

## Requirements

Use:

```java
AbilityRequirements.capabilityPolicyActive(policyId)
```

when a policy should also gate abilities, modes, or other authored conditions.

## Commands and Debugging

Useful command surfaces:

- `/xlib capability_policy apply|revoke|clear|list ...`
- `/xlib debug content capability_policies list`
- `/xlib debug content capability_policies inspect <id>`
- `/xlib debug state <player>`
- `/xlib debug export <player>`

## Datapack Authoring

Policies can be authored under:

- `data/<namespace>/xlib/capability_policies/*.json`

Use [Declarative JSON Reference](Declarative-JSON-Reference.md) for the full field list, including:

- `can_change_selected_hotbar_slot`
- `allowed_hotbar_slots`
- `blocked_hotbar_slots`
- exact item allow/block selectors
- item-tag allow/block selectors
- exact block/entity selectors
- block/entity tag selectors

## When To Use Capability Policies

Use capability policies when the problem is "this player currently cannot do this class of action."

Good fits:

- transformation lockdown
- possession restrictions
- onboarding/tutorial restrictions
- cursed equipment restrictions
- species or faction restrictions
- temporary stun/seal/control states

Use state flags instead when you only need named state markers and no direct runtime enforcement.

## Current Limits

- `blockExternalStorage()`-style managed-item protection and capability policies cover a lot of normal player paths, but they are still not a universal hook for every external inventory system another mod may invent.
- Armor effect suppression is still a separate concern from equip/unequip blocking. Capability policies currently focus on access and interaction control.
