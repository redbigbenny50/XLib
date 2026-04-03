package com.whatxe.xlib.ability;

import com.mojang.datafixers.util.Pair;
import com.whatxe.xlib.api.event.XLibGrantedItemEvent;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class GrantedItemRuntime {
    private static final String GRANTED_ITEM_ID_KEY = "xlib_granted_item_id";
    private static final String OWNER_UUID_KEY = "xlib_granted_item_owner";
    private static final String UNDROPPABLE_KEY = "xlib_granted_item_undroppable";

    private GrantedItemRuntime() {}

    public static AbilityData tick(ServerPlayer player, AbilityData currentData) {
        AbilityData updatedData = pruneMissingDefinitions(currentData);
        boolean inventoryChanged = enforceOpenContainerPolicies(player, updatedData);
        inventoryChanged |= removeRevokedItems(player, updatedData);
        if (inventoryChanged) {
            player.containerMenu.broadcastChanges();
        }
        return updatedData;
    }

    public static void reconcile(ServerPlayer player) {
        AbilityData currentData = ModAttachments.get(player);
        AbilityData updatedData = tick(player, currentData);
        if (!updatedData.equals(currentData)) {
            ModAttachments.set(player, updatedData);
        }
    }

    public static boolean restoreMissingUndroppableItems(ServerPlayer player) {
        return restoreMissingUndroppableItems(player, ModAttachments.get(player));
    }

    public static Optional<ResourceLocation> grantedItemId(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(GRANTED_ITEM_ID_KEY)) {
            return Optional.empty();
        }

        CompoundTag tag = customData.copyTag();
        if (!tag.contains(GRANTED_ITEM_ID_KEY, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        return Optional.of(ResourceLocation.parse(tag.getString(GRANTED_ITEM_ID_KEY)));
    }

    public static boolean isUndroppable(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(UNDROPPABLE_KEY)) {
            return false;
        }
        return customData.copyTag().getBoolean(UNDROPPABLE_KEY);
    }

    public static Optional<UUID> ownerUuid(ItemStack stack) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (!customData.contains(OWNER_UUID_KEY)) {
            return Optional.empty();
        }

        CompoundTag tag = customData.copyTag();
        return tag.hasUUID(OWNER_UUID_KEY) ? Optional.of(tag.getUUID(OWNER_UUID_KEY)) : Optional.empty();
    }

    public static void markGrantedItem(ItemStack stack, ResourceLocation grantedItemId, boolean undroppable) {
        markGrantedItem(stack, grantedItemId, null, undroppable);
    }

    public static void markGrantedItem(
            ItemStack stack,
            ResourceLocation grantedItemId,
            @Nullable UUID ownerId,
            boolean undroppable
    ) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(GRANTED_ITEM_ID_KEY, grantedItemId.toString());
            if (ownerId != null) {
                tag.putUUID(OWNER_UUID_KEY, ownerId);
            } else {
                tag.remove(OWNER_UUID_KEY);
            }
            if (undroppable) {
                tag.putBoolean(UNDROPPABLE_KEY, true);
            } else {
                tag.remove(UNDROPPABLE_KEY);
            }
        });
    }

    private static AbilityData pruneMissingDefinitions(AbilityData data) {
        AbilityData updatedData = data;
        for (ResourceLocation grantedItemId : Set.copyOf(updatedData.grantedItems())) {
            if (GrantedItemApi.findGrantedItem(grantedItemId).isPresent()) {
                continue;
            }
            for (ResourceLocation sourceId : Set.copyOf(updatedData.grantedItemSourcesFor(grantedItemId))) {
                updatedData = updatedData.withGrantedItemSource(grantedItemId, sourceId, false);
            }
        }
        return updatedData;
    }

    public static boolean grantNewlyGrantedItems(ServerPlayer player, AbilityData previousData, AbilityData currentData) {
        boolean changed = false;
        for (ResourceLocation grantedItemId : currentData.grantedItems()) {
            if (previousData.hasGrantedItem(grantedItemId)) {
                continue;
            }
            GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
            if (definition == null || hasMarkedItem(player, grantedItemId)) {
                continue;
            }

            ItemStack stack = definition.createStack(player, currentData);
            if (stack.isEmpty()) {
                continue;
            }
            markGrantedItem(stack, grantedItemId, player.getUUID(), definition.undroppable());
            if (player.getInventory().add(stack)) {
                changed = true;
            }
        }
        if (changed) {
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    public static boolean restoreMissingUndroppableItems(ServerPlayer player, AbilityData data) {
        boolean changed = false;
        for (ResourceLocation grantedItemId : data.grantedItems()) {
            GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
            if (definition == null || !definition.undroppable() || hasMarkedItem(player, grantedItemId)) {
                continue;
            }

            ItemStack stack = definition.createStack(player, data);
            if (stack.isEmpty()) {
                continue;
            }
            markGrantedItem(stack, grantedItemId, player.getUUID(), true);
            if (player.getInventory().add(stack)) {
                changed = true;
            }
        }
        if (changed) {
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    public static void installStorageGuards(ServerPlayer player) {
        guardStorageMenu(player, player.containerMenu);
    }

    public static void guardStorageMenu(ServerPlayer player, @Nullable AbstractContainerMenu menu) {
        if (menu == null || menu == player.inventoryMenu) {
            return;
        }

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (slot.container == player.getInventory() || slot instanceof StorageGuardSlot) {
                continue;
            }
            menu.slots.set(slotIndex, new StorageGuardSlot(player, slot));
        }
    }

    public static boolean reclaimUndroppableItemsFromOpenContainer(ServerPlayer player, AbilityData data) {
        return enforceOpenContainerPolicies(player, data);
    }

    public static boolean blocksExternalStorage(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        return blocksExternalStorage(ModAttachments.get(serverPlayer), serverPlayer, stack);
    }

    public static boolean enforceOpenContainerPolicies(ServerPlayer player, AbilityData data) {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null || menu == player.inventoryMenu) {
            return false;
        }

        boolean changed = handleCarriedManagedItem(player, data, menu);
        for (Slot slot : menu.slots) {
            if (slot.container == player.getInventory()) {
                continue;
            }
            changed |= handleExternalSlot(player, data, slot);
        }
        return changed;
    }

    private static boolean removeRevokedItems(ServerPlayer player, AbilityData data) {
        boolean changed = false;
        changed |= pruneList(player, player.getInventory().items, data);
        changed |= pruneList(player, player.getInventory().armor, data);
        changed |= pruneList(player, player.getInventory().offhand, data);
        AbstractContainerMenu menu = player.containerMenu;
        if (menu != null && menu != player.inventoryMenu) {
            changed |= handleCarriedManagedItem(player, data, menu);
            for (Slot slot : menu.slots) {
                if (slot.container == player.getInventory()) {
                    continue;
                }
                changed |= handleExternalSlot(player, data, slot);
            }
        }
        return changed;
    }

    private static boolean handleCarriedManagedItem(ServerPlayer player, AbilityData data, AbstractContainerMenu menu) {
        ItemStack carried = menu.getCarried();
        ManagedItemAction action = actionForExternalStack(player, data, carried);
        if (action == ManagedItemAction.NONE) {
            return false;
        }

        menu.setCarried(ItemStack.EMPTY);
        if (action == ManagedItemAction.RECLAIM) {
            player.getInventory().placeItemBackInInventory(carried.copy(), true);
            postReclaimed(player, carried);
        } else {
            postRemoved(player, carried, removalReason(data, carried));
        }
        return true;
    }

    private static boolean handleExternalSlot(ServerPlayer player, AbilityData data, Slot slot) {
        ItemStack stack = slot.getItem();
        ManagedItemAction action = actionForExternalStack(player, data, stack);
        if (action == ManagedItemAction.NONE) {
            return false;
        }

        slot.setByPlayer(ItemStack.EMPTY);
        if (action == ManagedItemAction.RECLAIM) {
            player.getInventory().placeItemBackInInventory(stack.copy(), true);
            postReclaimed(player, stack);
        } else {
            postRemoved(player, stack, removalReason(data, stack));
        }
        return true;
    }

    private static boolean pruneList(ServerPlayer player, List<ItemStack> stacks, AbilityData data) {
        boolean changed = false;
        for (int index = 0; index < stacks.size(); index++) {
            ItemStack stack = stacks.get(index);
            ResourceLocation grantedItemId = grantedItemId(stack).orElse(null);
            if (grantedItemId == null) {
                continue;
            }

            GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
            if (definition == null || (!data.hasGrantedItem(grantedItemId) && definition.removeWhenRevoked())) {
                stacks.set(index, ItemStack.EMPTY);
                postRemoved(
                        player,
                        stack,
                        definition == null ? XLibGrantedItemEvent.Reason.MISSING_DEFINITION : XLibGrantedItemEvent.Reason.REVOKED
                );
                changed = true;
            }
        }
        return changed;
    }

    private static boolean hasMarkedItem(ServerPlayer player, ResourceLocation grantedItemId) {
        Inventory inventory = player.getInventory();
        if (containsMarkedItem(inventory.items, player, grantedItemId, true)
                || containsMarkedItem(inventory.armor, player, grantedItemId, true)
                || containsMarkedItem(inventory.offhand, player, grantedItemId, true)) {
            return true;
        }

        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) {
            return false;
        }
        if (matchesPlayerOwnedGrantedItem(player, menu.getCarried(), grantedItemId, true)) {
            return true;
        }
        for (Slot slot : menu.slots) {
            boolean allowUnowned = slot.container == player.getInventory();
            if (matchesPlayerOwnedGrantedItem(player, slot.getItem(), grantedItemId, allowUnowned)) {
                return true;
            }
        }
        return false;
    }

    private static ManagedItemAction actionForExternalStack(ServerPlayer player, AbilityData data, ItemStack stack) {
        ResourceLocation grantedItemId = grantedItemId(stack).orElse(null);
        if (grantedItemId == null || stack.isEmpty()) {
            return ManagedItemAction.NONE;
        }
        if (!belongsToPlayer(player, stack)) {
            return ManagedItemAction.NONE;
        }

        GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
        if (definition == null) {
            return ManagedItemAction.REMOVE;
        }
        if (!data.hasGrantedItem(grantedItemId)) {
            return definition.removeWhenRevoked() ? ManagedItemAction.REMOVE : ManagedItemAction.NONE;
        }

        return switch (definition.storagePolicy()) {
            case ALLOW_STORAGE -> ManagedItemAction.NONE;
            case RECLAIM_FROM_OPEN_STORAGE, BLOCK_EXTERNAL_STORAGE -> ManagedItemAction.RECLAIM;
        };
    }

    private static boolean blocksExternalStorage(AbilityData data, ServerPlayer player, ItemStack stack) {
        ResourceLocation grantedItemId = grantedItemId(stack).orElse(null);
        if (grantedItemId == null || stack.isEmpty()) {
            return false;
        }

        GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
        return definition != null
                && belongsToPlayer(player, stack)
                && data.hasGrantedItem(grantedItemId)
                && definition.storagePolicy() == GrantedItemStoragePolicy.BLOCK_EXTERNAL_STORAGE;
    }

    private static XLibGrantedItemEvent.Reason removalReason(AbilityData data, ItemStack stack) {
        ResourceLocation grantedItemId = grantedItemId(stack).orElse(null);
        if (grantedItemId == null) {
            return XLibGrantedItemEvent.Reason.REVOKED;
        }
        GrantedItemDefinition definition = GrantedItemApi.findGrantedItem(grantedItemId).orElse(null);
        if (definition == null) {
            return XLibGrantedItemEvent.Reason.MISSING_DEFINITION;
        }
        if (!data.hasGrantedItem(grantedItemId)) {
            return XLibGrantedItemEvent.Reason.REVOKED;
        }
        return XLibGrantedItemEvent.Reason.STORAGE_POLICY;
    }

    private static void postReclaimed(ServerPlayer player, ItemStack stack) {
        ResourceLocation grantedItemId = grantedItemId(stack).orElse(null);
        if (grantedItemId == null) {
            return;
        }
        NeoForge.EVENT_BUS.post(new XLibGrantedItemEvent.Reclaimed(
                player,
                grantedItemId,
                stack,
                XLibGrantedItemEvent.Reason.STORAGE_POLICY
        ));
    }

    private static void postRemoved(ServerPlayer player, ItemStack stack, XLibGrantedItemEvent.Reason reason) {
        ResourceLocation grantedItemId = grantedItemId(stack).orElse(null);
        if (grantedItemId == null) {
            return;
        }
        NeoForge.EVENT_BUS.post(new XLibGrantedItemEvent.Removed(player, grantedItemId, stack, reason));
    }

    private static boolean containsMarkedItem(
            List<ItemStack> stacks,
            ServerPlayer player,
            ResourceLocation grantedItemId,
            boolean allowUnowned
    ) {
        for (ItemStack stack : stacks) {
            if (matchesPlayerOwnedGrantedItem(player, stack, grantedItemId, allowUnowned)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPlayerOwnedGrantedItem(
            ServerPlayer player,
            ItemStack stack,
            ResourceLocation grantedItemId,
            boolean allowUnowned
    ) {
        return grantedItemId(stack).filter(grantedItemId::equals).isPresent() && belongsToPlayer(player, stack, allowUnowned);
    }

    private static boolean belongsToPlayer(ServerPlayer player, ItemStack stack) {
        return belongsToPlayer(player, stack, false);
    }

    private static boolean belongsToPlayer(ServerPlayer player, ItemStack stack, boolean allowUnowned) {
        Optional<UUID> ownerId = ownerUuid(stack);
        return ownerId.map(player.getUUID()::equals).orElse(allowUnowned);
    }

    private enum ManagedItemAction {
        NONE,
        RECLAIM,
        REMOVE
    }

    private static final class StorageGuardSlot extends Slot {
        private final ServerPlayer player;
        private final Slot delegate;

        private StorageGuardSlot(ServerPlayer player, Slot delegate) {
            super(delegate.container, delegate.getContainerSlot(), delegate.x, delegate.y);
            this.player = player;
            this.delegate = delegate;
            this.index = delegate.index;
        }

        @Override
        public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
            this.delegate.onQuickCraft(oldStack, newStack);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            this.delegate.onTake(player, stack);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !blocksExternalStorage(this.player, stack) && this.delegate.mayPlace(stack);
        }

        @Override
        public ItemStack getItem() {
            return this.delegate.getItem();
        }

        @Override
        public boolean hasItem() {
            return this.delegate.hasItem();
        }

        @Override
        public void setByPlayer(ItemStack stack) {
            this.delegate.setByPlayer(stack);
        }

        @Override
        public void setByPlayer(ItemStack newStack, ItemStack oldStack) {
            this.delegate.setByPlayer(newStack, oldStack);
        }

        @Override
        public void set(ItemStack stack) {
            this.delegate.set(stack);
        }

        @Override
        public void setChanged() {
            this.delegate.setChanged();
        }

        @Override
        public int getMaxStackSize() {
            return this.delegate.getMaxStackSize();
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return this.delegate.getMaxStackSize(stack);
        }

        @Override
        public @Nullable Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
            return this.delegate.getNoItemIcon();
        }

        @Override
        public ItemStack remove(int amount) {
            return this.delegate.remove(amount);
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.delegate.mayPickup(player);
        }

        @Override
        public boolean isActive() {
            return this.delegate.isActive();
        }

        @Override
        public int getSlotIndex() {
            return this.delegate.getSlotIndex();
        }

        @Override
        public boolean isSameInventory(Slot other) {
            Slot comparison = other instanceof StorageGuardSlot guarded ? guarded.delegate : other;
            return this.delegate.isSameInventory(comparison);
        }

        @Override
        public boolean allowModification(Player player) {
            return this.delegate.allowModification(player);
        }

        @Override
        public int getContainerSlot() {
            return this.delegate.getContainerSlot();
        }

        @Override
        public boolean isHighlightable() {
            return this.delegate.isHighlightable();
        }

        @Override
        public boolean isFake() {
            return this.delegate.isFake();
        }
    }
}

