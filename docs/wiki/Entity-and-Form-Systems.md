# Entity and Form Systems

This page covers the five entity-relationship and embodiment systems XLib ships: capability policies, entity bindings, lifecycle stages, visual forms, and body transitions.

All five follow the same registry-style pattern used elsewhere in XLib: `bootstrap()`, `register()`, `unregister()` (test use only), `findDefinition()`, and data stored in synced or entity-scoped NeoForge attachments.

XLib's cue and form plumbing is backend-agnostic. The optional BLib bridge under `com.whatxe.xlib.integration.blib` can route XLib runtime cues into BLib `AzCommand` playback when BLib is present, but BLib is not required for the five systems on this page to function.

---

## Capability Policies (`com.whatxe.xlib.capability`)

Capability policies restrict what a player can do during a given source-tracked period. Any number of policies can be active from any number of sources simultaneously. Resolution is lazy: XLib merges them into a `ResolvedCapabilityPolicyState` the first time it is queried on a tick and caches the result.

### Policy axes

| Policy record | What it controls |
|---|---|
| `InteractionPolicy` | right-click interaction with blocks and entities |
| `InventoryPolicy` | opening the personal inventory |
| `MovementPolicy` | walking, jumping, sprinting |
| `MenuPolicy` | opening block menus (furnaces, chests, etc.) |
| `CraftingPolicy` | crafting output slot access |
| `EquipmentPolicy` | equipping or unequipping armor |
| `HeldItemPolicy` | switching or using held items |
| `ContainerPolicy` | any container the player has open |
| `PickupDropPolicy` | picking up or dropping items |

### Core API

```java
// Registration (call during bootstrap)
CapabilityPolicyApi.register(CapabilityPolicyDefinition def);

// Apply / revoke
CapabilityPolicyApi.apply(player, policyId);
CapabilityPolicyApi.apply(player, policyId, sourceId);
CapabilityPolicyApi.revoke(player, policyId, sourceId);
CapabilityPolicyApi.revokeSource(player, sourceId);   // revoke all for one source
CapabilityPolicyApi.clearAll(player);

// Query
CapabilityPolicyApi.allows(player, CapabilityCheck check);
CapabilityPolicyApi.hasActivePolicy(player, policyId);
CapabilityPolicyApi.activePolicies(player);
CapabilityPolicyApi.resolved(player);  // ResolvedCapabilityPolicyState
```

### Requirement helper

```java
AbilityRequirements.capabilityPolicyActive(policyId)
```

### JSON authoring

Capability policies can be defined in datapacks without writing Java. Place files under `data/<namespace>/xlib/capability_policies/*.json`. Each file is parsed by `DataDrivenCapabilityPolicyApi` and becomes available through `CapabilityPolicyApi.find()` at runtime.

See [Declarative JSON Reference](Declarative-JSON-Reference) — **Capability Policy Definitions** for the full field table.

---

## Entity Bindings (`com.whatxe.xlib.binding`)

Entity bindings represent a typed, source-tracked relationship between two living entities. XLib maintains a runtime UUID cache so `EntityBindingApi.find(instanceId)` and `EntityBindingApi.bindings(entity)` are O(1) without scanning level entity lists.

### Binding kinds

`LINK`, `ATTACHMENT`, `OCCUPANCY`, `CONTROL`, `TETHER`, `CONTAINMENT`

### Stacking policies

| Policy | Behavior |
|---|---|
| `SINGLE` | only one binding of this id per primary entity; `bind()` is a no-op if one exists |
| `REPLACE` | replaces an existing binding of the same id; fires `REPLACED` break reason on the old one |
| `STACK` | allows multiple bindings of the same id on the same primary entity |

### Break conditions

`PRIMARY_DIES`, `SECONDARY_DIES`, `PRIMARY_DISCONNECTS`, `SECONDARY_DISCONNECTS`, `RANGE_EXCEEDED`

Break conditions are declared on the definition and automatically checked by `EntityBindingHooks`. Range-exceeded checks require addon code to call `EntityBindingApi.breakByCondition(entity, RANGE_EXCEEDED)` at the appropriate time (XLib does not track distance itself).

### Core API

```java
// Registration
EntityBindingApi.register(EntityBindingDefinition def);

// Bind (returns instance UUID)
UUID instanceId = EntityBindingApi.bind(primary, secondary, bindingId, sourceId);

// Unbind
EntityBindingApi.unbind(instanceId, EntityBindingEndReason.MANUAL);

// Query
EntityBindingApi.bindings(entity);               // all bindings where entity is primary
EntityBindingApi.bindings(entity, kind);          // filtered by kind
EntityBindingApi.between(entityA, entityB);       // bindings in either direction
EntityBindingApi.find(instanceId);               // Optional<EntityBindingState>
```

### Definition builder

```java
EntityBindingDefinition.builder(id, EntityBindingKind.CONTROL)
    .stackingPolicy(EntityBindingStackingPolicy.REPLACE)
    .symmetry(EntityBindingSymmetry.DIRECTED)
    .breakConditions(EntityBindingBreakCondition.PRIMARY_DIES, EntityBindingBreakCondition.SECONDARY_DIES)
    .durationTicks(200)           // auto-enables TICK_DURATION tick policy
    .build();
```

### Requirement helpers

```java
AbilityRequirements.bindingActive(bindingId)       // player has an active binding with this id
AbilityRequirements.bindingKindActive(kind)        // player has any active binding of this kind
```

---

## Lifecycle Stages (`com.whatxe.xlib.lifecycle`)

A lifecycle stage is a named, time-aware player state with authored transitions, projected grants, and a status machine (`ACTIVE` → `PENDING_TRANSITION` → next stage via `completePendingTransition`).

### Transition triggers

`TIMER` (auto, based on elapsed ticks), `MANUAL` (via command or API call), `DEATH`, `RESPAWN`, `ADVANCEMENT`, `CONDITION`

### Projections applied while a stage is active

- `StateFlagApi.grant(...)` for each authored state flag id
- `GrantBundleApi` activation for each authored bundle id
- `IdentityApi` projection for each authored identity id
- `CapabilityPolicyApi.apply(...)` for each authored policy id
- `VisualFormApi.apply(...)` if a projected visual form id is defined

Projections are cleared when a stage transitions out via `completePendingTransition` or `clearStage`.

### Core API

```java
// Registration
LifecycleStageApi.register(LifecycleStageDefinition def);

// Set / clear
LifecycleStageApi.setStage(player, stageId, sourceId);
LifecycleStageApi.clearStage(player, sourceId);

// Transitions
LifecycleStageApi.requestTransition(player, targetStageId, sourceId);  // MANUAL trigger
LifecycleStageApi.completePendingTransition(player);

// Query
LifecycleStageApi.state(player);                  // Optional<LifecycleStageState>
LifecycleStageApi.isInStage(player, stageId);
```

### Definition builder

```java
LifecycleStageDefinition.builder(id)
    .durationTicks(1200)
    .addAutoTransition(LifecycleStageTransition.timer(nextStageId))
    .addAutoTransition(LifecycleStageTransition.onDeath(deathStageId))
    .addManualTransitionTarget(manualTargetId)
    .addProjectedStateFlag(flagId)
    .addProjectedGrantBundle(bundleId)
    .addProjectedCapabilityPolicy(policyId)
    .projectedVisualForm(formId)
    .build();
```

### Requirement helper

```java
AbilityRequirements.lifecycleStageActive(stageId)
```

### JSON authoring

Lifecycle stages can be defined in datapacks without writing Java. Place files under `data/<namespace>/xlib/lifecycle_stages/*.json`. Each file is parsed by `DataDrivenLifecycleStageApi` and becomes available through `LifecycleStageApi.findDefinition()` at runtime. Projected cross-references (grant bundles, identities, state flags, capability policies, and visual forms) are validated against Java-registered definitions at reload time, with warnings logged for any unresolved ids.

See [Declarative JSON Reference](Declarative-JSON-Reference) — **Lifecycle Stage Definitions** for the full field table.

---

## Visual Forms (`com.whatxe.xlib.form`)

A visual form is a named definition that tells animation, model, and HUD backends which profile to apply while the form is active. Multiple forms can be active simultaneously; the first one registered for the player is the primary form.

Visual forms are source-tracked: `revoke(player, formId, sourceId)` only removes the form if the specified source owns it. `revokeSource(player, sourceId)` removes all forms granted by that source.

### Form kinds

`HUMANOID`, `CREATURE`, `VEHICLE`, `CONSTRUCT`, `SPIRIT`, `ABSTRACT`

### Definition builder

```java
VisualFormDefinition.builder(id, VisualFormKind.CREATURE)
    .modelProfileId(id("model/wolf_body"))
    .cueRouteProfileId(id("cue/quadruped_cues"))
    .hudProfileId(id("hud/creature_bars"))
    .firstPersonPolicy(FirstPersonPolicy.HIDDEN)
    .renderScale(1.2f)
    .build();
```

### Core API

```java
VisualFormApi.register(def);
VisualFormApi.apply(player, formId, sourceId);
VisualFormApi.revoke(player, formId, sourceId);
VisualFormApi.revokeSource(player, sourceId);
VisualFormApi.clearAll(player);

VisualFormApi.active(player);         // Optional<VisualFormDefinition> — primary form
VisualFormApi.hasForm(player, formId);
VisualFormApi.activeForms(player);    // all active form ids
```

### Requirement helper

```java
AbilityRequirements.visualFormActive(formId)
```

### JSON authoring

Visual forms can be defined in datapacks without writing Java. Place files under `data/<namespace>/xlib/visual_forms/*.json`. Each file is parsed by `DataDrivenVisualFormApi` and becomes available through `VisualFormApi.findDefinition()` at runtime. The `kind` field is required; all profile references and the `first_person_policy` are optional.

See [Declarative JSON Reference](Declarative-JSON-Reference) — **Visual Form Definitions** for the full field table.

---

## Body Transitions (`com.whatxe.xlib.body`)

A body transition represents the player controlling or occupying a different entity body — possession, projection, hatching, emergence, or return. Only one transition can be active at a time.

When a transition begins, XLib optionally applies a temporary capability policy and temporary visual form to the player. Both are revoked when `returnToOrigin()` or `clear()` is called.

### Transition kinds

| Kind | Semantic |
|---|---|
| `REPLACE` | player's body is replaced with the target entity body |
| `POSSESS` | player's consciousness inhabits a separate entity body |
| `PROJECT` | player projects a copy of themselves into a target entity |
| `HATCH` | player transitions out of an egg or larval form |
| `EMERGE` | player transitions out of a dormant or chrysalis-like state |
| `RETURN` | a return-home transition back to the origin body |

### Origin body policies

`PRESERVE` (keep origin body alive), `DESTROY` (remove origin body), `SHELL` (keep as empty shell)

### Definition builder

```java
BodyTransitionDefinition.builder(id, BodyTransitionKind.POSSESS)
    .originBodyPolicy(OriginBodyPolicy.SHELL)
    .controlPolicy(BodyControlPolicy.POSSESSED)
    .temporaryCapabilityPolicy(id("policy/possession_restrictions"))
    .temporaryVisualForm(id("form/ethereal_spirit"))
    .reversible(true)
    .build();
```

### Core API

```java
BodyTransitionApi.register(def);

// Begin a transition (targetEntityId is the body entity UUID)
BodyTransitionApi.begin(player, transitionId, targetEntityId, sourceId);

// End
BodyTransitionApi.returnToOrigin(player, sourceId);  // reversible only
BodyTransitionApi.clear(player, sourceId);           // force-end

// Query
BodyTransitionApi.active(player);                    // Optional<BodyTransitionState>
BodyTransitionApi.isTransitioning(player);
BodyTransitionApi.isControlling(player, targetEntityId);
```

### Requirement helper

```java
AbilityRequirements.bodyTransitionActive()
```

---

## Sanitization on login and respawn

All five systems sanitize player attachment state on login and (where applicable) on respawn. Unknown or stale stage IDs, form IDs, transition IDs, and policy IDs are silently dropped rather than causing runtime errors when addon content is removed.

## Debug export

All five systems are included in the `/xlib debug export` JSON dump under `capability_policies`, `entity_bindings`, `lifecycle_stage`, `visual_forms`, and `body_transition` keys.
