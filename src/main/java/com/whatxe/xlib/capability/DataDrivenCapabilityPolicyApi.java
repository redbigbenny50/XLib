package com.whatxe.xlib.capability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.whatxe.xlib.XLib;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenCapabilityPolicyApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/capability_policies";
    private static volatile Map<ResourceLocation, LoadedCapabilityPolicyDefinition> loadedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, LoadedCapabilityPolicyDefinition> syncedDefinitions = Map.of();
    private static volatile Map<ResourceLocation, String> definitionJsonByFileId = Map.of();

    private DataDrivenCapabilityPolicyApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(resolvedDefinitions().keySet());
    }

    public static Optional<LoadedCapabilityPolicyDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(resolvedDefinitions().get(definitionId));
    }

    public static Map<ResourceLocation, String> definitionJsonsForSync() {
        return Map.copyOf(definitionJsonByFileId);
    }

    public static void syncDefinitionsFromJson(Map<ResourceLocation, String> jsonByFileId) {
        syncedDefinitions = parseDefinitionsFromJson(jsonByFileId, "client-synced capability_policy");
    }

    public static void clearSyncedDefinitions() {
        syncedDefinitions = Map.of();
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedCapabilityPolicyDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedCapabilityPolicyDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "capability_policy");
        ResourceLocation policyId = object.has("id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "id"))
                : fileId;

        CapabilityPolicyDefinition.Builder builder = CapabilityPolicyDefinition.builder(policyId);

        if (object.has("merge_mode")) {
            String modeName = GsonHelper.getAsString(object, "merge_mode").toUpperCase(java.util.Locale.ROOT);
            try {
                builder.mergeMode(CapabilityPolicyMergeMode.valueOf(modeName));
            } catch (IllegalArgumentException ignored) {
                throw new IllegalArgumentException("Unknown merge_mode '" + modeName
                        + "'; valid values: restrictive, permissive");
            }
        }

        if (object.has("priority")) {
            builder.priority(GsonHelper.getAsInt(object, "priority"));
        }

        if (object.has("inventory")) {
            builder.inventory(parseInventoryPolicy(GsonHelper.getAsJsonObject(object, "inventory")));
        }

        if (object.has("equipment")) {
            builder.equipment(parseEquipmentPolicy(GsonHelper.getAsJsonObject(object, "equipment")));
        }

        if (object.has("held_items")) {
            builder.heldItems(parseHeldItemPolicy(GsonHelper.getAsJsonObject(object, "held_items")));
        }

        if (object.has("crafting")) {
            builder.crafting(parseCraftingPolicy(GsonHelper.getAsJsonObject(object, "crafting")));
        }

        if (object.has("containers")) {
            builder.containers(parseContainerPolicy(GsonHelper.getAsJsonObject(object, "containers")));
        }

        if (object.has("pickup_drop")) {
            builder.pickupDrop(parsePickupDropPolicy(GsonHelper.getAsJsonObject(object, "pickup_drop")));
        }

        if (object.has("interaction")) {
            builder.interaction(parseInteractionPolicy(GsonHelper.getAsJsonObject(object, "interaction")));
        }

        if (object.has("menus")) {
            builder.menus(parseMenuPolicy(GsonHelper.getAsJsonObject(object, "menus")));
        }

        if (object.has("movement")) {
            builder.movement(parseMovementPolicy(GsonHelper.getAsJsonObject(object, "movement")));
        }

        return new LoadedCapabilityPolicyDefinition(policyId, builder.build());
    }

    private static InventoryPolicy parseInventoryPolicy(JsonObject object) {
        InventoryPolicy.Builder builder = InventoryPolicy.builder()
                .canOpenInventory(bool(object, "can_open_inventory", true))
                .canMoveItems(bool(object, "can_move_items", true))
                .canUseHotbar(bool(object, "can_use_hotbar", true))
                .canUseOffhand(bool(object, "can_use_offhand", true))
                .canChangeSelectedHotbarSlot(bool(object, "can_change_selected_hotbar_slot", true));
        if (object.has("allowed_hotbar_slots")) {
            for (JsonElement slotElement : GsonHelper.getAsJsonArray(object, "allowed_hotbar_slots")) {
                builder.allowHotbarSlot(slotElement.getAsInt());
            }
        }
        if (object.has("blocked_hotbar_slots")) {
            for (JsonElement slotElement : GsonHelper.getAsJsonArray(object, "blocked_hotbar_slots")) {
                builder.blockHotbarSlot(slotElement.getAsInt());
            }
        }
        return builder.build();
    }

    private static EquipmentPolicy parseEquipmentPolicy(JsonObject object) {
        EquipmentPolicy.Builder builder = EquipmentPolicy.builder()
                .canEquipArmor(bool(object, "can_equip_armor", true))
                .canUnequipArmor(bool(object, "can_unequip_armor", true))
                .canEquipHeldItems(bool(object, "can_equip_held_items", true));
        readLocations(object, "allowed_armor_item", "allowed_armor_items").forEach(builder::allowArmorItem);
        readLocations(object, "blocked_armor_item", "blocked_armor_items").forEach(builder::blockArmorItem);
        readLocations(object, "allowed_armor_item_tag", "allowed_armor_item_tags").forEach(builder::allowArmorItemTag);
        readLocations(object, "blocked_armor_item_tag", "blocked_armor_item_tags").forEach(builder::blockArmorItemTag);
        if (object.has("suppressed_armor_slots")) {
            for (JsonElement slotElement : GsonHelper.getAsJsonArray(object, "suppressed_armor_slots")) {
                builder.suppressArmorSlot(slotElement.getAsString());
            }
        }
        return builder.build();
    }

    private static HeldItemPolicy parseHeldItemPolicy(JsonObject object) {
        HeldItemPolicy.Builder builder = HeldItemPolicy.builder()
                .canUseMainHand(bool(object, "can_use_main_hand", true))
                .canUseOffhand(bool(object, "can_use_offhand", true))
                .canBlockWithShields(bool(object, "can_block_with_shields", true))
                .canUseTools(bool(object, "can_use_tools", true))
                .canUseWeapons(bool(object, "can_use_weapons", true))
                .canPlaceBlocks(bool(object, "can_place_blocks", true))
                .canBreakBlocks(bool(object, "can_break_blocks", true));
        readLocations(object, "allowed_item", "allowed_items").forEach(builder::allowItem);
        readLocations(object, "blocked_item", "blocked_items").forEach(builder::blockItem);
        if (object.has("allowed_item_tags")) {
            for (JsonElement tagElement : GsonHelper.getAsJsonArray(object, "allowed_item_tags")) {
                builder.allowItemTag(ResourceLocation.parse(tagElement.getAsString()));
            }
        }
        if (object.has("blocked_item_tags")) {
            for (JsonElement tagElement : GsonHelper.getAsJsonArray(object, "blocked_item_tags")) {
                builder.blockItemTag(ResourceLocation.parse(tagElement.getAsString()));
            }
        }
        return builder.build();
    }

    private static CraftingPolicy parseCraftingPolicy(JsonObject object) {
        CraftingPolicy.Builder builder = CraftingPolicy.builder()
                .canUsePlayerCrafting(bool(object, "can_use_player_crafting", true))
                .canUseCraftingTable(bool(object, "can_use_crafting_table", true));
        if (object.has("allowed_station_tags")) {
            for (JsonElement tagElement : GsonHelper.getAsJsonArray(object, "allowed_station_tags")) {
                builder.allowStationTag(ResourceLocation.parse(tagElement.getAsString()));
            }
        }
        if (object.has("blocked_station_tags")) {
            for (JsonElement tagElement : GsonHelper.getAsJsonArray(object, "blocked_station_tags")) {
                builder.blockStationTag(ResourceLocation.parse(tagElement.getAsString()));
            }
        }
        return builder.build();
    }

    private static ContainerPolicy parseContainerPolicy(JsonObject object) {
        return ContainerPolicy.builder()
                .canOpenContainers(bool(object, "can_open_containers", true))
                .canOpenChests(bool(object, "can_open_chests", true))
                .canOpenFurnaces(bool(object, "can_open_furnaces", true))
                .canOpenBrewingStands(bool(object, "can_open_brewing_stands", true))
                .canOpenShulkerBoxes(bool(object, "can_open_shulker_boxes", true))
                .canInsertIntoFurnace(bool(object, "can_insert_into_furnace", true))
                .canExtractFromFurnace(bool(object, "can_extract_from_furnace", true))
                .canTakeFurnaceOutput(bool(object, "can_take_furnace_output", true))
                .canInsertIntoBrewing(bool(object, "can_insert_into_brewing", true))
                .canExtractFromBrewing(bool(object, "can_extract_from_brewing", true))
                .canTakeBrewingOutput(bool(object, "can_take_brewing_output", true))
                .canTakeCraftingOutput(bool(object, "can_take_crafting_output", true))
                .canInsertIntoAnvil(bool(object, "can_insert_into_anvil", true))
                .canTakeAnvilOutput(bool(object, "can_take_anvil_output", true))
                .build();
    }

    private static PickupDropPolicy parsePickupDropPolicy(JsonObject object) {
        PickupDropPolicy.Builder builder = PickupDropPolicy.builder()
                .canPickupItems(bool(object, "can_pickup_items", true))
                .canDropItems(bool(object, "can_drop_items", true));
        readLocations(object, "allowed_item", "allowed_items").forEach(builder::allowItem);
        readLocations(object, "blocked_item", "blocked_items").forEach(builder::blockItem);
        if (object.has("allowed_item_tags")) {
            for (JsonElement tagElement : GsonHelper.getAsJsonArray(object, "allowed_item_tags")) {
                builder.allowItemTag(ResourceLocation.parse(tagElement.getAsString()));
            }
        }
        if (object.has("blocked_item_tags")) {
            for (JsonElement tagElement : GsonHelper.getAsJsonArray(object, "blocked_item_tags")) {
                builder.blockItemTag(ResourceLocation.parse(tagElement.getAsString()));
            }
        }
        return builder.build();
    }

    private static InteractionPolicy parseInteractionPolicy(JsonObject object) {
        InteractionPolicy.Builder builder = InteractionPolicy.builder()
                .canInteractWithBlocks(bool(object, "can_interact_with_blocks", true))
                .canInteractWithEntities(bool(object, "can_interact_with_entities", true))
                .canUseBeds(bool(object, "can_use_beds", true))
                .canRideEntities(bool(object, "can_ride_entities", true))
                .canAttackPlayers(bool(object, "can_attack_players", true))
                .canAttackMobs(bool(object, "can_attack_mobs", true));
        readLocations(object, "allowed_block", "allowed_blocks").forEach(builder::allowBlock);
        readLocations(object, "blocked_block", "blocked_blocks").forEach(builder::blockBlock);
        readLocations(object, "allowed_block_tag", "allowed_block_tags").forEach(builder::allowBlockTag);
        readLocations(object, "blocked_block_tag", "blocked_block_tags").forEach(builder::blockBlockTag);
        readLocations(object, "allowed_entity", "allowed_entities").forEach(builder::allowEntity);
        readLocations(object, "blocked_entity", "blocked_entities").forEach(builder::blockEntity);
        readLocations(object, "allowed_entity_tag", "allowed_entity_tags").forEach(builder::allowEntityTag);
        readLocations(object, "blocked_entity_tag", "blocked_entity_tags").forEach(builder::blockEntityTag);
        return builder.build();
    }

    private static MenuPolicy parseMenuPolicy(JsonObject object) {
        return MenuPolicy.builder()
                .canOpenAbilityMenu(bool(object, "can_open_ability_menu", true))
                .canOpenProgressionMenu(bool(object, "can_open_progression_menu", true))
                .canOpenInventoryScreen(bool(object, "can_open_inventory_screen", true))
                .build();
    }

    private static MovementPolicy parseMovementPolicy(JsonObject object) {
        return MovementPolicy.builder()
                .canSprint(bool(object, "can_sprint", true))
                .canSneak(bool(object, "can_sneak", true))
                .canJump(bool(object, "can_jump", true))
                .canFly(bool(object, "can_fly", true))
                .build();
    }

    private static boolean bool(JsonObject object, String key, boolean defaultValue) {
        return object.has(key) ? GsonHelper.getAsBoolean(object, key) : defaultValue;
    }

    private static List<ResourceLocation> readLocations(JsonObject object, String singleKey, String pluralKey) {
        java.util.ArrayList<ResourceLocation> values = new java.util.ArrayList<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(ResourceLocation.parse(GsonHelper.getAsString(object, singleKey)));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        return List.copyOf(values);
    }

    public record LoadedCapabilityPolicyDefinition(
            ResourceLocation id,
            CapabilityPolicyDefinition definition
    ) {}

    private static Map<ResourceLocation, LoadedCapabilityPolicyDefinition> resolvedDefinitions() {
        Map<ResourceLocation, LoadedCapabilityPolicyDefinition> definitions = new LinkedHashMap<>(loadedDefinitions);
        syncedDefinitions.forEach(definitions::putIfAbsent);
        return Map.copyOf(definitions);
    }

    private static Map<ResourceLocation, LoadedCapabilityPolicyDefinition> parseDefinitionsFromJson(
            Map<ResourceLocation, String> jsonByFileId,
            String label
    ) {
        Map<ResourceLocation, LoadedCapabilityPolicyDefinition> definitions = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, String> entry : jsonByFileId.entrySet()) {
            try {
                LoadedCapabilityPolicyDefinition definition = parseDefinition(entry.getKey(), JsonParser.parseString(entry.getValue()));
                LoadedCapabilityPolicyDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate " + label + " id: " + definition.id());
                }
            } catch (RuntimeException exception) {
                XLib.LOGGER.error("Failed to parse {} {}", label, entry.getKey(), exception);
            }
        }
        return Map.copyOf(definitions);
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedCapabilityPolicyDefinition> definitions = new LinkedHashMap<>();
            Map<ResourceLocation, String> definitionJsons = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    LoadedCapabilityPolicyDefinition definition = parseDefinition(entry.getKey(), entry.getValue());
                    LoadedCapabilityPolicyDefinition previous = definitions.putIfAbsent(definition.id(), definition);
                    if (previous != null) {
                        throw new IllegalStateException("Duplicate datapack capability_policy id: " + definition.id());
                    }
                    definitionJsons.put(entry.getKey(), GSON.toJson(entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse datapack capability_policy {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
            definitionJsonByFileId = Map.copyOf(definitionJsons);
        }
    }
}
