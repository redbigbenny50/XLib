package com.whatxe.xlib.ability;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.whatxe.xlib.XLib;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

public final class DataDrivenEquipmentBindingApi {
    private static final Gson GSON = new GsonBuilder().create();
    private static final String DIRECTORY = XLib.MODID + "/equipment_bindings";
    private static final String ARTIFACT_SOURCE_PATH_PREFIX = "equipment_binding_artifact/";
    private static volatile Map<ResourceLocation, LoadedEquipmentBindingDefinition> loadedDefinitions = Map.of();

    private DataDrivenEquipmentBindingApi() {}

    public static void bootstrap() {}

    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new ReloadListener());
    }

    public static Collection<ResourceLocation> allDefinitionIds() {
        return List.copyOf(loadedDefinitions.keySet());
    }

    public static Optional<LoadedEquipmentBindingDefinition> findDefinition(ResourceLocation definitionId) {
        return Optional.ofNullable(loadedDefinitions.get(definitionId));
    }

    public static Collection<ResolvedEquipmentBinding> collectResolvedBindings(ServerPlayer player, AbilityData currentData) {
        List<ResolvedEquipmentBinding> resolvedBindings = new ArrayList<>();
        for (LoadedEquipmentBindingDefinition definition : loadedDefinitions.values()) {
            try {
                if (!definition.matches(player, currentData)) {
                    continue;
                }
                resolvedBindings.add(new ResolvedEquipmentBinding(
                        definition.id(),
                        definition.sourceId(),
                        definition.snapshot(),
                        definition.grantBundles(),
                        definition.unlockArtifacts()
                ));
            } catch (RuntimeException exception) {
                XLib.LOGGER.error(
                        "Equipment binding {} failed for player {}",
                        definition.id(),
                        player.getGameProfile().getName(),
                        exception
                );
            }
        }
        return List.copyOf(resolvedBindings);
    }

    public static ResourceLocation artifactSourceIdFor(ResourceLocation sourceId) {
        return ResourceLocation.fromNamespaceAndPath(
                XLib.MODID,
                ARTIFACT_SOURCE_PATH_PREFIX + sourceId.getNamespace() + "/" + sourceId.getPath()
        );
    }

    public static java.util.Optional<ResourceLocation> parseArtifactSourceId(ResourceLocation sourceId) {
        if (!XLib.MODID.equals(sourceId.getNamespace()) || !sourceId.getPath().startsWith(ARTIFACT_SOURCE_PATH_PREFIX)) {
            return java.util.Optional.empty();
        }
        String suffix = sourceId.getPath().substring(ARTIFACT_SOURCE_PATH_PREFIX.length());
        String[] parts = suffix.split("/", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            return java.util.Optional.of(ResourceLocation.fromNamespaceAndPath(parts[0], parts[1]));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    static void setDefinitionsForTesting(Map<ResourceLocation, LoadedEquipmentBindingDefinition> definitions) {
        loadedDefinitions = Map.copyOf(definitions);
    }

    static LoadedEquipmentBindingDefinition parseDefinition(ResourceLocation fileId, JsonElement element) {
        JsonObject object = GsonHelper.convertToJsonObject(element, "equipment binding");
        ResourceLocation sourceId = object.has("source")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "source"))
                : object.has("source_id")
                ? ResourceLocation.parse(GsonHelper.getAsString(object, "source_id"))
                : fileId;
        AbilityRequirement requirement = AbilityRequirementJsonParser.parse(
                object.get("when"),
                DataDrivenConditionApi::requireCondition
        );
        Set<ResourceLocation> itemIds = readLocations(object, "item", "items");
        Set<ResourceLocation> itemTagIds = readLocations(object, "item_tag", "item_tags");
        Map<EquipmentSlotBinding, ResourceLocation> slotItems = readSlotBindings(object, "slot_items");
        Map<EquipmentSlotBinding, ResourceLocation> slotTags = readSlotBindings(object, "slot_tags");
        if (itemIds.isEmpty() && itemTagIds.isEmpty() && slotItems.isEmpty() && slotTags.isEmpty()) {
            throw new IllegalArgumentException("Equipment bindings require at least one item, item tag, slot item, or slot tag matcher");
        }
        Set<ArtifactPresenceMode> presenceModes = readPresenceModes(object);
        MatchMode matchMode = object.has("match")
                ? MatchMode.parse(GsonHelper.getAsString(object, "match"))
                : MatchMode.ALL;

        ContextGrantSnapshot.Builder snapshot = ContextGrantSnapshot.builder(sourceId);
        snapshot.grantAbilities(readLocations(object, "grant_ability", "grant_abilities"));
        snapshot.grantPassives(readLocations(object, "grant_passive", "grant_passives"));
        snapshot.grantGrantedItems(readLocations(object, "grant_granted_item", "grant_granted_items"));
        snapshot.grantRecipePermissions(readLocations(object, "grant_recipe_permission", "grant_recipe_permissions"));
        snapshot.blockAbilities(readLocations(object, "block_ability", "block_abilities"));
        snapshot.grantStatePolicies(readLocations(object, "grant_state_policy", "grant_state_policies"));
        snapshot.grantStateFlags(readLocations(object, "grant_state_flag", "grant_state_flags"));

        return new LoadedEquipmentBindingDefinition(
                fileId,
                sourceId,
                requirement,
                itemIds,
                itemTagIds,
                presenceModes,
                matchMode,
                slotItems,
                slotTags,
                snapshot.build(),
                readLocations(object, "grant_bundle", "grant_bundles"),
                readLocations(object, "unlock_artifact", "unlock_artifacts")
        );
    }

    private static Set<ArtifactPresenceMode> readPresenceModes(JsonObject object) {
        Set<ArtifactPresenceMode> presenceModes = new LinkedHashSet<>();
        if (object.has("presence")) {
            presenceModes.add(ArtifactPresenceMode.valueOf(GsonHelper.getAsString(object, "presence").trim().toUpperCase(java.util.Locale.ROOT)));
        }
        if (object.has("presence_modes")) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, "presence_modes")) {
                presenceModes.add(ArtifactPresenceMode.valueOf(element.getAsString().trim().toUpperCase(java.util.Locale.ROOT)));
            }
        }
        if (presenceModes.isEmpty()) {
            presenceModes.add(ArtifactPresenceMode.ARMOR);
        }
        return Set.copyOf(presenceModes);
    }

    private static Map<EquipmentSlotBinding, ResourceLocation> readSlotBindings(JsonObject object, String key) {
        if (!object.has(key)) {
            return Map.of();
        }
        JsonObject slotObject = GsonHelper.getAsJsonObject(object, key);
        Map<EquipmentSlotBinding, ResourceLocation> bindings = new LinkedHashMap<>();
        slotObject.entrySet().forEach(entry -> bindings.put(
                EquipmentSlotBinding.parse(entry.getKey()),
                ResourceLocation.parse(entry.getValue().getAsString())
        ));
        return Map.copyOf(bindings);
    }

    private static Set<ResourceLocation> readLocations(JsonObject object, String singleKey, String pluralKey) {
        Set<ResourceLocation> values = new LinkedHashSet<>();
        if (singleKey != null && object.has(singleKey)) {
            values.add(ResourceLocation.parse(GsonHelper.getAsString(object, singleKey)));
        }
        if (pluralKey != null && object.has(pluralKey)) {
            for (JsonElement element : GsonHelper.getAsJsonArray(object, pluralKey)) {
                values.add(ResourceLocation.parse(element.getAsString()));
            }
        }
        return Set.copyOf(values);
    }

    private static TagKey<Item> itemTag(ResourceLocation tagId) {
        return TagKey.create(Registries.ITEM, tagId);
    }

    public record ResolvedEquipmentBinding(
            ResourceLocation id,
            ResourceLocation sourceId,
            ContextGrantSnapshot snapshot,
            Set<ResourceLocation> grantBundles,
            Set<ResourceLocation> unlockArtifacts
    ) {}

    public enum MatchMode {
        ANY,
        ALL;

        static MatchMode parse(String rawValue) {
            return switch (rawValue.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "any" -> ANY;
                case "all" -> ALL;
                default -> throw new IllegalArgumentException("Unknown equipment binding match mode: " + rawValue);
            };
        }
    }

    public enum EquipmentSlotBinding {
        HEAD,
        CHEST,
        LEGS,
        FEET,
        MAIN_HAND,
        OFF_HAND;

        static EquipmentSlotBinding parse(String rawValue) {
            return switch (rawValue.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "head", "helmet" -> HEAD;
                case "chest", "chestplate" -> CHEST;
                case "legs", "leggings" -> LEGS;
                case "feet", "boots" -> FEET;
                case "main_hand", "mainhand" -> MAIN_HAND;
                case "off_hand", "offhand" -> OFF_HAND;
                default -> throw new IllegalArgumentException("Unknown equipment binding slot: " + rawValue);
            };
        }

        ItemStack stack(Player player) {
            return switch (this) {
                case HEAD -> player.getInventory().armor.size() > 3 ? player.getInventory().armor.get(3) : ItemStack.EMPTY;
                case CHEST -> player.getInventory().armor.size() > 2 ? player.getInventory().armor.get(2) : ItemStack.EMPTY;
                case LEGS -> player.getInventory().armor.size() > 1 ? player.getInventory().armor.get(1) : ItemStack.EMPTY;
                case FEET -> player.getInventory().armor.isEmpty() ? ItemStack.EMPTY : player.getInventory().armor.getFirst();
                case MAIN_HAND -> player.getMainHandItem();
                case OFF_HAND -> player.getOffhandItem();
            };
        }
    }

    public record LoadedEquipmentBindingDefinition(
            ResourceLocation id,
            ResourceLocation sourceId,
            AbilityRequirement requirement,
            Set<ResourceLocation> itemIds,
            Set<ResourceLocation> itemTagIds,
            Set<ArtifactPresenceMode> presenceModes,
            MatchMode matchMode,
            Map<EquipmentSlotBinding, ResourceLocation> slotItems,
            Map<EquipmentSlotBinding, ResourceLocation> slotTags,
            ContextGrantSnapshot snapshot,
            Set<ResourceLocation> grantBundles,
            Set<ResourceLocation> unlockArtifacts
    ) {
        public boolean hasGeneralMatchers() {
            return !this.itemIds.isEmpty() || !this.itemTagIds.isEmpty();
        }

        public boolean hasSlotMatchers() {
            return !this.slotItems.isEmpty() || !this.slotTags.isEmpty();
        }

        private boolean matches(Player player, AbilityData currentData) {
            if (this.requirement.validate(player, currentData).isPresent()) {
                return false;
            }
            return matchesGeneral(player) && matchesSlots(player);
        }

        private boolean matchesGeneral(Player player) {
            if (!hasGeneralMatchers()) {
                return true;
            }
            return switch (this.matchMode) {
                case ANY -> this.itemIds.stream().anyMatch(itemId -> matchesAcrossPresenceModes(player, itemId, this.presenceModes))
                        || this.itemTagIds.stream().anyMatch(tagId -> matchesAcrossPresenceModes(player, itemTag(tagId), this.presenceModes));
                case ALL -> this.itemIds.stream().allMatch(itemId -> matchesAcrossPresenceModes(player, itemId, this.presenceModes))
                        && this.itemTagIds.stream().allMatch(tagId -> matchesAcrossPresenceModes(player, itemTag(tagId), this.presenceModes));
            };
        }

        private boolean matchesSlots(Player player) {
            if (player == null) {
                return false;
            }
            for (Map.Entry<EquipmentSlotBinding, ResourceLocation> entry : this.slotItems.entrySet()) {
                if (!matchesItemId(entry.getKey().stack(player), entry.getValue())) {
                    return false;
                }
            }
            for (Map.Entry<EquipmentSlotBinding, ResourceLocation> entry : this.slotTags.entrySet()) {
                if (!matchesItemTag(entry.getKey().stack(player), itemTag(entry.getValue()))) {
                    return false;
                }
            }
            return true;
        }

        private static boolean matchesAcrossPresenceModes(Player player, ResourceLocation itemId, Set<ArtifactPresenceMode> presenceModes) {
            for (ArtifactPresenceMode presenceMode : presenceModes) {
                if (matchesInPresenceMode(player, itemId, presenceMode)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matchesAcrossPresenceModes(Player player, TagKey<Item> itemTag, Set<ArtifactPresenceMode> presenceModes) {
            for (ArtifactPresenceMode presenceMode : presenceModes) {
                if (matchesInPresenceMode(player, itemTag, presenceMode)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matchesInPresenceMode(Player player, ResourceLocation itemId, ArtifactPresenceMode presenceMode) {
            return switch (presenceMode) {
                case INVENTORY -> anyMatching(player.getInventory().items, itemId)
                        || anyMatching(player.getInventory().armor, itemId)
                        || anyMatching(player.getInventory().offhand, itemId);
                case HOTBAR -> anyMatching(player.getInventory().items.subList(0, Math.min(9, player.getInventory().items.size())), itemId);
                case MAIN_HAND -> matchesItemId(player.getMainHandItem(), itemId);
                case OFF_HAND -> matchesItemId(player.getOffhandItem(), itemId);
                case ARMOR -> anyMatching(player.getInventory().armor, itemId);
                case HEAD -> matchesItemId(EquipmentSlotBinding.HEAD.stack(player), itemId);
                case CHEST -> matchesItemId(EquipmentSlotBinding.CHEST.stack(player), itemId);
                case LEGS -> matchesItemId(EquipmentSlotBinding.LEGS.stack(player), itemId);
                case FEET -> matchesItemId(EquipmentSlotBinding.FEET.stack(player), itemId);
                case EQUIPPED -> matchesItemId(player.getMainHandItem(), itemId)
                        || matchesItemId(player.getOffhandItem(), itemId)
                        || anyMatching(player.getInventory().armor, itemId);
            };
        }

        private static boolean matchesInPresenceMode(Player player, TagKey<Item> itemTag, ArtifactPresenceMode presenceMode) {
            return switch (presenceMode) {
                case INVENTORY -> anyMatching(player.getInventory().items, itemTag)
                        || anyMatching(player.getInventory().armor, itemTag)
                        || anyMatching(player.getInventory().offhand, itemTag);
                case HOTBAR -> anyMatching(player.getInventory().items.subList(0, Math.min(9, player.getInventory().items.size())), itemTag);
                case MAIN_HAND -> matchesItemTag(player.getMainHandItem(), itemTag);
                case OFF_HAND -> matchesItemTag(player.getOffhandItem(), itemTag);
                case ARMOR -> anyMatching(player.getInventory().armor, itemTag);
                case HEAD -> matchesItemTag(EquipmentSlotBinding.HEAD.stack(player), itemTag);
                case CHEST -> matchesItemTag(EquipmentSlotBinding.CHEST.stack(player), itemTag);
                case LEGS -> matchesItemTag(EquipmentSlotBinding.LEGS.stack(player), itemTag);
                case FEET -> matchesItemTag(EquipmentSlotBinding.FEET.stack(player), itemTag);
                case EQUIPPED -> matchesItemTag(player.getMainHandItem(), itemTag)
                        || matchesItemTag(player.getOffhandItem(), itemTag)
                        || anyMatching(player.getInventory().armor, itemTag);
            };
        }

        private static boolean anyMatching(Iterable<ItemStack> stacks, ResourceLocation itemId) {
            for (ItemStack stack : stacks) {
                if (matchesItemId(stack, itemId)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean anyMatching(Iterable<ItemStack> stacks, TagKey<Item> itemTag) {
            for (ItemStack stack : stacks) {
                if (matchesItemTag(stack, itemTag)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean matchesItemId(ItemStack stack, ResourceLocation itemId) {
            return stack != null
                    && !stack.isEmpty()
                    && itemId.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }

        private static boolean matchesItemTag(ItemStack stack, TagKey<Item> itemTag) {
            return stack != null
                    && !stack.isEmpty()
                    && stack.is(itemTag);
        }
    }

    private static final class ReloadListener extends SimpleJsonResourceReloadListener {
        private ReloadListener() {
            super(GSON, DIRECTORY);
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<ResourceLocation, LoadedEquipmentBindingDefinition> definitions = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonById.entrySet()) {
                try {
                    definitions.put(entry.getKey(), parseDefinition(entry.getKey(), entry.getValue()));
                } catch (RuntimeException exception) {
                    XLib.LOGGER.error("Failed to parse equipment binding {}", entry.getKey(), exception);
                }
            }
            loadedDefinitions = Map.copyOf(definitions);
        }
    }
}
