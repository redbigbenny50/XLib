package com.whatxe.xlib.capability;

import com.whatxe.xlib.XLib;
import com.whatxe.xlib.XLibRegistryGuard;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public final class CapabilityPolicyApi {
    public static final ResourceLocation COMMAND_SOURCE =
            ResourceLocation.fromNamespaceAndPath(XLib.MODID, "command_capability_policy");

    private static final Map<ResourceLocation, CapabilityPolicyDefinition> POLICIES = new LinkedHashMap<>();

    private CapabilityPolicyApi() {}

    public static void bootstrap() {}

    // --- Registration ---

    public static CapabilityPolicyDefinition register(CapabilityPolicyDefinition policy) {
        XLibRegistryGuard.ensureMutable("capability_policies");
        CapabilityPolicyDefinition previous = POLICIES.putIfAbsent(policy.id(), policy);
        if (previous != null) {
            throw new IllegalStateException("Duplicate capability policy registration: " + policy.id());
        }
        return policy;
    }

    public static Optional<CapabilityPolicyDefinition> unregister(ResourceLocation policyId) {
        XLibRegistryGuard.ensureMutable("capability_policies");
        return Optional.ofNullable(POLICIES.remove(policyId));
    }

    public static Optional<CapabilityPolicyDefinition> find(ResourceLocation policyId) {
        CapabilityPolicyDefinition policy = POLICIES.get(policyId);
        if (policy != null) {
            return Optional.of(policy);
        }
        return DataDrivenCapabilityPolicyApi.findDefinition(policyId)
                .map(DataDrivenCapabilityPolicyApi.LoadedCapabilityPolicyDefinition::definition);
    }

    public static Collection<CapabilityPolicyDefinition> all() {
        return List.copyOf(POLICIES.values());
    }

    // --- Player state queries ---

    public static boolean hasActivePolicy(Player player, ResourceLocation policyId) {
        return getData(player).hasPolicy(policyId) && POLICIES.containsKey(policyId);
    }

    public static Set<ResourceLocation> sourcesFor(Player player, ResourceLocation policyId) {
        return getData(player).sourcesFor(policyId);
    }

    public static List<CapabilityPolicyDefinition> activePolicies(Player player) {
        return activePolicies(getData(player));
    }

    public static List<CapabilityPolicyDefinition> activePolicies(CapabilityPolicyData data) {
        return data.activePolicies().stream()
                .map(POLICIES::get)
                .filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparingInt(CapabilityPolicyDefinition::priority).reversed())
                .toList();
    }

    public static ResolvedCapabilityPolicyState resolved(Player player) {
        return resolved(getData(player));
    }

    public static ResolvedCapabilityPolicyState resolved(CapabilityPolicyData data) {
        List<CapabilityPolicyDefinition> active = activePolicies(data);
        if (active.isEmpty()) {
            return ResolvedCapabilityPolicyState.UNRESTRICTED;
        }

        InventoryPolicy inventory = InventoryPolicy.FULL;
        EquipmentPolicy equipment = EquipmentPolicy.FULL;
        HeldItemPolicy heldItems = HeldItemPolicy.FULL;
        CraftingPolicy crafting = CraftingPolicy.FULL;
        ContainerPolicy containers = ContainerPolicy.FULL;
        PickupDropPolicy pickupDrop = PickupDropPolicy.FULL;
        InteractionPolicy interaction = InteractionPolicy.FULL;
        MenuPolicy menus = MenuPolicy.FULL;
        MovementPolicy movement = MovementPolicy.FULL;
        Map<ResourceLocation, ResourceLocation> contributing = new LinkedHashMap<>();

        for (CapabilityPolicyDefinition policy : active) {
            inventory = mergeInventory(inventory, policy.inventory(), policy.mergeMode());
            equipment = mergeEquipment(equipment, policy.equipment(), policy.mergeMode());
            heldItems = mergeHeldItems(heldItems, policy.heldItems(), policy.mergeMode());
            crafting = mergeCrafting(crafting, policy.crafting(), policy.mergeMode());
            containers = mergeContainers(containers, policy.containers(), policy.mergeMode());
            pickupDrop = mergePickupDrop(pickupDrop, policy.pickupDrop(), policy.mergeMode());
            interaction = mergeInteraction(interaction, policy.interaction(), policy.mergeMode());
            menus = mergeMenus(menus, policy.menus(), policy.mergeMode());
            movement = mergeMovement(movement, policy.movement(), policy.mergeMode());
            data.sourcesFor(policy.id()).stream().findFirst()
                    .ifPresent(source -> contributing.put(policy.id(), source));
        }

        return new ResolvedCapabilityPolicyState(
                inventory, equipment, heldItems, crafting, containers,
                pickupDrop, interaction, menus, movement,
                Collections.unmodifiableMap(contributing)
        );
    }

    public static boolean allows(Player player, CapabilityCheck check) {
        return resolved(player).allows(check);
    }

    // --- Apply / revoke ---

    public static void apply(Player player, ResourceLocation policyId) {
        apply(player, policyId, COMMAND_SOURCE);
    }

    public static void apply(Player player, ResourceLocation policyId, ResourceLocation sourceId) {
        update(player, getData(player).withPolicySource(policyId, sourceId, true));
    }

    public static void apply(Player player, Collection<ResourceLocation> policyIds, ResourceLocation sourceId) {
        CapabilityPolicyData data = getData(player);
        for (ResourceLocation id : new LinkedHashSet<>(policyIds)) {
            data = data.withPolicySource(id, sourceId, true);
        }
        update(player, data);
    }

    public static void revoke(Player player, ResourceLocation policyId) {
        revoke(player, policyId, COMMAND_SOURCE);
    }

    public static void revoke(Player player, ResourceLocation policyId, ResourceLocation sourceId) {
        update(player, getData(player).withPolicySource(policyId, sourceId, false));
    }

    public static void revokeSource(Player player, ResourceLocation sourceId) {
        update(player, getData(player).clearPolicySource(sourceId));
    }

    public static void clearAll(Player player) {
        update(player, CapabilityPolicyData.empty());
    }

    public static void syncSource(Player player, ResourceLocation sourceId, Collection<ResourceLocation> policyIds) {
        CapabilityPolicyData data = getData(player);
        Set<ResourceLocation> desired = new LinkedHashSet<>(policyIds);
        for (ResourceLocation policyId : Set.copyOf(data.activePolicies())) {
            if (data.sourcesFor(policyId).contains(sourceId) && !desired.contains(policyId)) {
                data = data.withPolicySource(policyId, sourceId, false);
            }
        }
        for (ResourceLocation policyId : desired) {
            data = data.withPolicySource(policyId, sourceId, true);
        }
        update(player, data);
    }

    public static CapabilityPolicyData sanitize(CapabilityPolicyData data) {
        return data.retainRegistered(POLICIES.keySet());
    }

    // --- Attachment access ---

    public static CapabilityPolicyData getData(Player player) {
        return player.getData(com.whatxe.xlib.attachment.ModAttachments.PLAYER_CAPABILITY_POLICY);
    }

    public static void setData(Player player, CapabilityPolicyData data) {
        player.setData(com.whatxe.xlib.attachment.ModAttachments.PLAYER_CAPABILITY_POLICY, sanitize(data));
    }

    // --- Merge helpers ---

    private static InventoryPolicy mergeInventory(InventoryPolicy a, InventoryPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveInventory(a, b);
    }

    private static InventoryPolicy permissiveInventory(InventoryPolicy a, InventoryPolicy b) {
        return new InventoryPolicy(
                a.canOpenInventory() || b.canOpenInventory(),
                a.canMoveItems() || b.canMoveItems(),
                a.canUseHotbar() || b.canUseHotbar(),
                a.canUseOffhand() || b.canUseOffhand()
        );
    }

    private static EquipmentPolicy mergeEquipment(EquipmentPolicy a, EquipmentPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveEquipment(a, b);
    }

    private static EquipmentPolicy permissiveEquipment(EquipmentPolicy a, EquipmentPolicy b) {
        return new EquipmentPolicy(
                a.canEquipArmor() || b.canEquipArmor(),
                a.canUnequipArmor() || b.canUnequipArmor(),
                a.canEquipHeldItems() || b.canEquipHeldItems()
        );
    }

    private static HeldItemPolicy mergeHeldItems(HeldItemPolicy a, HeldItemPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveHeldItems(a, b);
    }

    private static HeldItemPolicy permissiveHeldItems(HeldItemPolicy a, HeldItemPolicy b) {
        return HeldItemPolicy.builder()
                .canUseMainHand(a.canUseMainHand() || b.canUseMainHand())
                .canUseOffhand(a.canUseOffhand() || b.canUseOffhand())
                .canBlockWithShields(a.canBlockWithShields() || b.canBlockWithShields())
                .canUseTools(a.canUseTools() || b.canUseTools())
                .canUseWeapons(a.canUseWeapons() || b.canUseWeapons())
                .canPlaceBlocks(a.canPlaceBlocks() || b.canPlaceBlocks())
                .canBreakBlocks(a.canBreakBlocks() || b.canBreakBlocks())
                .build();
    }

    private static CraftingPolicy mergeCrafting(CraftingPolicy a, CraftingPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveCrafting(a, b);
    }

    private static CraftingPolicy permissiveCrafting(CraftingPolicy a, CraftingPolicy b) {
        return CraftingPolicy.builder()
                .canUsePlayerCrafting(a.canUsePlayerCrafting() || b.canUsePlayerCrafting())
                .canUseCraftingTable(a.canUseCraftingTable() || b.canUseCraftingTable())
                .build();
    }

    private static ContainerPolicy mergeContainers(ContainerPolicy a, ContainerPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveContainers(a, b);
    }

    private static ContainerPolicy permissiveContainers(ContainerPolicy a, ContainerPolicy b) {
        return new ContainerPolicy(
                a.canOpenContainers() || b.canOpenContainers(),
                a.canOpenChests() || b.canOpenChests(),
                a.canOpenFurnaces() || b.canOpenFurnaces(),
                a.canOpenShulkerBoxes() || b.canOpenShulkerBoxes()
        );
    }

    private static PickupDropPolicy mergePickupDrop(PickupDropPolicy a, PickupDropPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissivePickupDrop(a, b);
    }

    private static PickupDropPolicy permissivePickupDrop(PickupDropPolicy a, PickupDropPolicy b) {
        return new PickupDropPolicy(
                a.canPickupItems() || b.canPickupItems(),
                a.canDropItems() || b.canDropItems()
        );
    }

    private static InteractionPolicy mergeInteraction(InteractionPolicy a, InteractionPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveInteraction(a, b);
    }

    private static InteractionPolicy permissiveInteraction(InteractionPolicy a, InteractionPolicy b) {
        return new InteractionPolicy(
                a.canInteractWithBlocks() || b.canInteractWithBlocks(),
                a.canInteractWithEntities() || b.canInteractWithEntities(),
                a.canAttackPlayers() || b.canAttackPlayers(),
                a.canAttackMobs() || b.canAttackMobs()
        );
    }

    private static MenuPolicy mergeMenus(MenuPolicy a, MenuPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveMenus(a, b);
    }

    private static MenuPolicy permissiveMenus(MenuPolicy a, MenuPolicy b) {
        return new MenuPolicy(
                a.canOpenAbilityMenu() || b.canOpenAbilityMenu(),
                a.canOpenProgressionMenu() || b.canOpenProgressionMenu(),
                a.canOpenInventoryScreen() || b.canOpenInventoryScreen()
        );
    }

    private static MovementPolicy mergeMovement(MovementPolicy a, MovementPolicy b, CapabilityPolicyMergeMode mode) {
        return mode == CapabilityPolicyMergeMode.RESTRICTIVE ? a.mergeRestrictive(b) : permissiveMovement(a, b);
    }

    private static MovementPolicy permissiveMovement(MovementPolicy a, MovementPolicy b) {
        return new MovementPolicy(
                a.canSprint() || b.canSprint(),
                a.canSneak() || b.canSneak(),
                a.canJump() || b.canJump(),
                a.canFly() || b.canFly()
        );
    }

    private static void update(Player player, CapabilityPolicyData data) {
        CapabilityPolicyData current = getData(player);
        if (!data.equals(current)) {
            setData(player, data);
        }
    }
}
