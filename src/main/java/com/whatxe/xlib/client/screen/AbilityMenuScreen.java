package com.whatxe.xlib.client.screen;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityResourceCost;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.ModeDefinition;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.client.AbilityIconRenderer;
import com.whatxe.xlib.network.AssignAbilityPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public class AbilityMenuScreen extends Screen {
    private static final int SLOT_STRIP_X_OFFSET = 178;
    private static final int SLOT_STRIP_Y = 70;
    private static final int SLOT_BUTTON_Y = 96;
    private static final int DETAILS_PANEL_WIDTH = 150;
    private static final int DETAILS_PANEL_X_OFFSET = 170;
    private static final int DETAILS_PANEL_Y = 124;
    private static final int ABILITY_BUTTON_WIDTH = 150;
    private static final int ABILITY_BUTTON_X_OFFSET = 20;
    private static final int SEARCH_BOX_Y = 134;
    private static final int FILTER_ROW_Y = 160;
    private static final int ABILITY_LIST_Y = 186;
    private static final int ABILITY_BUTTON_SPACING = 24;
    private static final int MIN_VISIBLE_ABILITY_BUTTONS = 3;
    private static final int MAX_VISIBLE_ABILITY_BUTTONS = 7;
    private static final int FOOTER_BUTTON_SPACING = 26;

    private final List<Button> slotButtons = new ArrayList<>();
    private final List<Button> abilityButtons = new ArrayList<>();
    private List<AbilityDefinition> visibleAbilities = List.of();
    private List<ResourceLocation> editableModeIds = List.of();
    private int selectedSlot = 0;
    private int abilityScrollOffset = 0;
    private int visibleAbilityButtonCount = MIN_VISIBLE_ABILITY_BUTTONS;
    private int abilityButtonX;
    private Button clearSlotButton;
    private Button cycleLoadoutButton;
    private Button openProgressionButton;
    private Button filterButton;
    private Button sortButton;
    private EditBox searchBox;
    private int loadoutModeIndex = -1;
    private AbilityFilter abilityFilter = AbilityFilter.ALL;
    private AbilitySort abilitySort = AbilitySort.NAME;
    private AbilityData cachedVisibleData = AbilityData.empty();
    private String cachedSearchQuery = "";
    private AbilityFilter cachedFilter = this.abilityFilter;
    private AbilitySort cachedSort = this.abilitySort;

    public AbilityMenuScreen() {
        super(Component.translatable("screen.xlib.ability_menu"));
    }

    @Override
    protected void init() {
        buildMenuWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        refreshVisibleState();
        updateSlotButtons();
        updateAbilityButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xD0101010, 0xE0101010);
        int slotStripX = this.width / 2 - SLOT_STRIP_X_OFFSET;
        int detailsX = this.width / 2 - DETAILS_PANEL_X_OFFSET;
        int abilityPanelLeft = this.abilityButtonX - 6;
        int abilityPanelTop = SEARCH_BOX_Y - 18;
        int abilityPanelBottom = abilityPanelBottom();

        AbilityData data = currentData();
        guiGraphics.fill(detailsX - 8, DETAILS_PANEL_Y - 8, detailsX + DETAILS_PANEL_WIDTH + 8, abilityPanelBottom, 0x33202020);
        guiGraphics.fill(abilityPanelLeft, abilityPanelTop, abilityPanelLeft + ABILITY_BUTTON_WIDTH + 18, abilityPanelBottom, 0x33202020);

        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            int x = slotStripX + slot * 40;
            int color = slot == this.selectedSlot ? 0xAA2E5E3F : 0x66303030;
            guiGraphics.fill(x, SLOT_STRIP_Y, x + 36, SLOT_STRIP_Y + 20, color);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 16, 0xFFFFFF, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.xlib.ability_menu.instructions"),
                this.width / 2 - 150,
                34,
                0xCFCFCF,
                false
        );
        guiGraphics.drawString(this.font, Component.translatable("screen.xlib.available_abilities"), this.abilityButtonX, SEARCH_BOX_Y - 14, 0xFFFFFF, false);

        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            int x = slotStripX + slot * 40;
            Optional<AbilityDefinition> maybeAbility = resolveVisibleAbility(assignedAbilityIdForCurrentTarget(data, slot));
            if (maybeAbility.isPresent()) {
                AbilityIconRenderer.render(guiGraphics, this.minecraft, maybeAbility.get().icon(), x + 10, SLOT_STRIP_Y + 2, 16, 16);
            }
        }

        Optional<AbilityDefinition> selectedAbility = resolveVisibleAbility(assignedAbilityIdForCurrentTarget(data, this.selectedSlot));
        Component slotTitle = Component.translatable(
                "screen.xlib.selected_slot",
                this.selectedSlot + 1,
                selectedAbility.map(AbilityDefinition::displayName).orElse(Component.translatable("screen.xlib.empty_slot"))
        );
        guiGraphics.drawString(this.font, slotTitle, detailsX, DETAILS_PANEL_Y, 0xFFFFFF, false);
        selectedAbility.ifPresent(ability -> AbilityIconRenderer.render(
                guiGraphics,
                this.minecraft,
                ability.icon(),
                detailsX + DETAILS_PANEL_WIDTH - 18,
                DETAILS_PANEL_Y - 2,
                16,
                16
        ));

        Component description = selectedAbility
                .map(AbilityDefinition::description)
                .orElse(Component.translatable("screen.xlib.empty_slot_desc"));
        int descriptionY = DETAILS_PANEL_Y + 18;
        Player player = currentPlayer();
        for (FormattedCharSequence line : this.font.split(description, DETAILS_PANEL_WIDTH)) {
            guiGraphics.drawString(this.font, line, detailsX, descriptionY, 0xD8D8D8, false);
            descriptionY += 10;
        }

        descriptionY += 6;
        if (selectedAbility.isPresent()) {
            AbilityDefinition ability = selectedAbility.get();
            if (ability.usesCharges()) {
                Component charges = Component.translatable(
                        "screen.xlib.ability_charges_stat",
                        data.chargeCountFor(ability.id(), ability.maxCharges()),
                        ability.maxCharges()
                );
                guiGraphics.drawString(this.font, charges, detailsX, descriptionY, 0xF2E5A3, false);
                descriptionY += 10;
            } else if (ability.cooldownTicks() > 0) {
                Component cooldown = Component.translatable(
                        "screen.xlib.ability_cooldown_stat",
                        formatSeconds(ability.cooldownTicks())
                );
                guiGraphics.drawString(this.font, cooldown, detailsX, descriptionY, 0xF2E5A3, false);
                descriptionY += 10;
            }

            if (ability.toggleAbility() && ability.durationTicks() > 0) {
                Component duration = Component.translatable(
                        "screen.xlib.ability_duration_stat",
                        formatSeconds(ability.durationTicks())
                );
                guiGraphics.drawString(this.font, duration, detailsX, descriptionY, 0xA9E6B0, false);
                descriptionY += 10;
            }

            for (AbilityResourceCost cost : ability.resourceCosts()) {
                Optional<AbilityResourceDefinition> resource = AbilityApi.findResource(cost.resourceId());
                Component costLine = Component.translatable(
                        "screen.xlib.ability_cost_stat",
                        cost.amount(),
                        resource.map(AbilityResourceDefinition::displayName).orElse(Component.literal(cost.resourceId().toString()))
                );
                guiGraphics.drawString(this.font, costLine, detailsX, descriptionY, 0x8FD7FF, false);
                descriptionY += 10;
            }

            descriptionY = appendRequirementLines(guiGraphics, detailsX, descriptionY, player, data, ability.assignRequirements(),
                    "screen.xlib.ability_assign_requirement_stat", 0xE7C98B);
            descriptionY = appendRequirementLines(guiGraphics, detailsX, descriptionY, player, data, ability.activateRequirements(),
                    "screen.xlib.ability_activate_requirement_stat", 0xD8B6FF);
            descriptionY = appendRequirementLines(guiGraphics, detailsX, descriptionY, player, data, ability.stayActiveRequirements(),
                    "screen.xlib.ability_sustain_requirement_stat", 0x9EE0FF);
        }

        renderAbilityButtonIcons(guiGraphics);

        if (maxAbilityScrollOffset() > 0) {
            renderScrollBar(guiGraphics);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverAbilityList(mouseX, mouseY) && maxAbilityScrollOffset() > 0) {
            scrollAbilityList(scrollY < 0 ? 1 : -1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void buildMenuWidgets() {
        this.clearWidgets();
        this.slotButtons.clear();
        this.abilityButtons.clear();
        refreshVisibleState();

        int buttonRowX = this.width / 2 - SLOT_STRIP_X_OFFSET;
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            final int targetSlot = slot;
            Button button = Button.builder(Component.literal(Integer.toString(slot + 1)), pressed -> {
                this.selectedSlot = targetSlot;
                updateSlotButtons();
            }).bounds(buttonRowX + slot * 40, SLOT_BUTTON_Y, 36, 20).build();
            this.slotButtons.add(this.addRenderableWidget(button));
        }

        this.abilityButtonX = this.width / 2 + ABILITY_BUTTON_X_OFFSET;
        this.visibleAbilityButtonCount = computeVisibleAbilityButtonCount();
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset, 0, maxAbilityScrollOffset());

        this.searchBox = this.addRenderableWidget(new EditBox(this.font, this.abilityButtonX, SEARCH_BOX_Y, ABILITY_BUTTON_WIDTH, 20, Component.empty()));
        this.searchBox.setHint(Component.translatable("screen.xlib.ability_menu.search_hint"));
        this.searchBox.setResponder(value -> {
            refreshVisibleState();
            updateAbilityButtons();
        });
        this.filterButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleFilter())
                .bounds(this.abilityButtonX, FILTER_ROW_Y, 72, 20)
                .build());
        this.sortButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleSort())
                .bounds(this.abilityButtonX + 78, FILTER_ROW_Y, 72, 20)
                .build());

        for (int index = 0; index < this.visibleAbilityButtonCount; index++) {
            final int visibleIndex = index;
            Button abilityButton = Button.builder(Component.literal(""), pressed -> assignVisibleAbility(visibleIndex))
                    .bounds(this.abilityButtonX, ABILITY_LIST_Y + index * ABILITY_BUTTON_SPACING, ABILITY_BUTTON_WIDTH, 20)
                    .build();
            this.abilityButtons.add(this.addRenderableWidget(abilityButton));
        }

        int clearButtonY = ABILITY_LIST_Y + this.visibleAbilityButtonCount * ABILITY_BUTTON_SPACING + 8;
        int detailsX = this.width / 2 - DETAILS_PANEL_X_OFFSET;
        int footerButtonY = clearButtonY - FOOTER_BUTTON_SPACING;
        this.clearSlotButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.xlib.clear_slot"), pressed -> assignAbility(Optional.empty()))
                .bounds(this.abilityButtonX, clearButtonY, ABILITY_BUTTON_WIDTH, 20)
                .build());
        this.cycleLoadoutButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleLoadoutTarget())
                .bounds(detailsX, footerButtonY, DETAILS_PANEL_WIDTH, 20)
                .build());
        this.openProgressionButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.xlib.open_progression"),
                        pressed -> {
                            if (this.minecraft != null) {
                                this.minecraft.setScreen(new ProgressionMenuScreen());
                            }
                        })
                .bounds(detailsX, clearButtonY, DETAILS_PANEL_WIDTH, 20)
                .build());

        updateSlotButtons();
        updateAbilityButtons();
        updateLoadoutButton();
    }

    private void updateSlotButtons() {
        for (int slot = 0; slot < this.slotButtons.size(); slot++) {
            String label = slot == this.selectedSlot ? "[" + (slot + 1) + "]" : Integer.toString(slot + 1);
            this.slotButtons.get(slot).setMessage(Component.literal(label));
        }
    }

    private void updateAbilityButtons() {
        refreshVisibleState();
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset, 0, maxAbilityScrollOffset());
        updateLoadoutButton();
        updateFilterAndSortButtons();
        for (int visibleIndex = 0; visibleIndex < this.abilityButtons.size(); visibleIndex++) {
            int abilityIndex = this.abilityScrollOffset + visibleIndex;
            Button button = this.abilityButtons.get(visibleIndex);
            if (abilityIndex < this.visibleAbilities.size()) {
                AbilityDefinition ability = this.visibleAbilities.get(abilityIndex);
                button.visible = true;
                button.active = currentPlayer() != null && AbilityGrantApi.canAssign(currentPlayer(), currentData(), ability);
                button.setMessage(Component.literal("   ").append(ability.displayName()));
            } else {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.literal(""));
            }
        }
    }

    private void assignVisibleAbility(int visibleIndex) {
        int abilityIndex = this.abilityScrollOffset + visibleIndex;
        if (abilityIndex >= 0 && abilityIndex < this.visibleAbilities.size()) {
            assignAbility(Optional.of(this.visibleAbilities.get(abilityIndex).id()));
        }
    }

    private void scrollAbilityList(int delta) {
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset + delta, 0, maxAbilityScrollOffset());
        updateAbilityButtons();
    }

    private int computeVisibleAbilityButtonCount() {
        int availableHeight = Math.max(80, this.height - ABILITY_LIST_Y - 72);
        int visibleCount = (availableHeight + 4) / ABILITY_BUTTON_SPACING;
        return Mth.clamp(visibleCount, MIN_VISIBLE_ABILITY_BUTTONS, MAX_VISIBLE_ABILITY_BUTTONS);
    }

    private int maxAbilityScrollOffset() {
        return Math.max(0, this.visibleAbilities.size() - this.visibleAbilityButtonCount);
    }

    private boolean isMouseOverAbilityList(double mouseX, double mouseY) {
        if (this.abilityButtons.isEmpty()) {
            return false;
        }

        int left = this.abilityButtonX - 6;
        int top = ABILITY_LIST_Y - 18;
        int right = this.abilityButtonX + ABILITY_BUTTON_WIDTH + 18;
        int bottom = abilityListBottom();
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int trackX = this.abilityButtonX + ABILITY_BUTTON_WIDTH + 6;
        int trackY = ABILITY_LIST_Y;
        int trackHeight = this.visibleAbilityButtonCount * ABILITY_BUTTON_SPACING - 4;
        int maxOffset = maxAbilityScrollOffset();
        int thumbHeight = Math.max(12, trackHeight * this.visibleAbilityButtonCount / this.visibleAbilities.size());
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (maxOffset == 0 ? 0 : Math.round((float)this.abilityScrollOffset / maxOffset * thumbTravel));

        guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackHeight, 0x66303030);
        guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF8BCF9A);
    }

    private int abilityListBottom() {
        return ABILITY_LIST_Y + this.visibleAbilityButtonCount * ABILITY_BUTTON_SPACING - 4;
    }

    private int abilityPanelBottom() {
        return this.clearSlotButton != null ? this.clearSlotButton.getY() + 26 : ABILITY_LIST_Y + 90;
    }

    private void assignAbility(Optional<ResourceLocation> abilityId) {
        PacketDistributor.sendToServer(new AssignAbilityPayload(this.selectedSlot, abilityId, currentEditingModeId()));
    }

    private AbilityData currentData() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return AbilityData.empty();
        }
        return ModAttachments.get(this.minecraft.player);
    }

    private static String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / 20.0F);
    }

    private void refreshVisibleState() {
        AbilityData data = currentData();
        Player player = currentPlayer();
        if (player == null) {
            this.visibleAbilities = List.of();
            this.editableModeIds = List.of();
            this.cachedVisibleData = AbilityData.empty();
            this.cachedSearchQuery = "";
            return;
        }

        String query = normalizedSearchQuery();
        if (data.equals(this.cachedVisibleData) && query.equals(this.cachedSearchQuery)
                && this.cachedFilter == this.abilityFilter && this.cachedSort == this.abilitySort) {
            return;
        }

        this.cachedVisibleData = data;
        this.cachedSearchQuery = query;
        this.cachedFilter = this.abilityFilter;
        this.cachedSort = this.abilitySort;
        this.editableModeIds = computeEditableModeIds(data, player);
        Optional<ResourceLocation> editingModeId = currentEditingModeId();
        Set<ResourceLocation> assignedIds = assignedAbilityIdsForCurrentTarget(data, editingModeId.orElse(null));
        List<AbilityDefinition> filteredAbilities = new ArrayList<>();
        for (AbilityDefinition ability : AbilityApi.allAbilities()) {
            if (!AbilityGrantApi.canView(player, data, ability)) {
                continue;
            }
            if (!matchesSearch(ability, query)) {
                continue;
            }
            if (this.abilityFilter == AbilityFilter.ASSIGNABLE && !AbilityGrantApi.canAssign(player, data, ability)) {
                continue;
            }
            if (this.abilityFilter == AbilityFilter.ASSIGNED && !assignedIds.contains(ability.id())) {
                continue;
            }
            filteredAbilities.add(ability);
        }
        filteredAbilities.sort(abilityComparator(data, editingModeId.orElse(null)));
        this.visibleAbilities = List.copyOf(filteredAbilities);
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset, 0, maxAbilityScrollOffset());
    }

    private Player currentPlayer() {
        return this.minecraft != null ? this.minecraft.player : null;
    }

    private Optional<ResourceLocation> assignedAbilityIdForCurrentTarget(AbilityData data, int slot) {
        return AbilityLoadoutApi.assignedAbilityId(data, currentEditingModeId().orElse(null), slot);
    }

    private List<ResourceLocation> computeEditableModeIds(AbilityData data, Player player) {
        List<ResourceLocation> editableModes = new ArrayList<>();
        for (ModeDefinition mode : ModeApi.allModes()) {
            if (data.modeLoadouts().containsKey(mode.abilityId())) {
                editableModes.add(mode.abilityId());
                continue;
            }
            AbilityDefinition modeAbility = AbilityApi.findAbility(mode.abilityId()).orElse(null);
            if (modeAbility != null && AbilityGrantApi.canView(player, data, modeAbility)) {
                editableModes.add(mode.abilityId());
            }
        }
        return List.copyOf(editableModes);
    }

    private Optional<ResourceLocation> currentEditingModeId() {
        if (this.editableModeIds.isEmpty()) {
            this.loadoutModeIndex = -1;
            return Optional.empty();
        }
        if (this.loadoutModeIndex < -1 || this.loadoutModeIndex >= this.editableModeIds.size()) {
            this.loadoutModeIndex = -1;
        }
        return this.loadoutModeIndex >= 0 ? Optional.of(this.editableModeIds.get(this.loadoutModeIndex)) : Optional.empty();
    }

    private void cycleLoadoutTarget() {
        refreshVisibleState();
        if (this.editableModeIds.isEmpty()) {
            this.loadoutModeIndex = -1;
            updateLoadoutButton();
            return;
        }

        int nextIndex = this.loadoutModeIndex + 1;
        this.loadoutModeIndex = nextIndex >= this.editableModeIds.size() ? -1 : nextIndex;
        refreshVisibleState();
        updateAbilityButtons();
        updateLoadoutButton();
    }

    private void updateLoadoutButton() {
        if (this.cycleLoadoutButton == null) {
            return;
        }
        this.cycleLoadoutButton.active = !this.editableModeIds.isEmpty();
        this.cycleLoadoutButton.setMessage(Component.translatable("screen.xlib.loadout_target", loadoutTargetLabel()));
    }

    private Component loadoutTargetLabel() {
        return currentEditingModeId()
                .flatMap(AbilityApi::findAbility)
                .map(AbilityDefinition::displayName)
                .orElse(Component.translatable("screen.xlib.loadout_target.base"));
    }

    private Optional<AbilityDefinition> resolveVisibleAbility(Optional<ResourceLocation> abilityId) {
        Player player = currentPlayer();
        if (player == null || abilityId.isEmpty()) {
            return Optional.empty();
        }
        AbilityData data = currentData();
        return AbilityApi.findAbility(abilityId.get())
                .filter(ability -> AbilityGrantApi.canView(player, data, ability));
    }

    private int appendRequirementLines(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Player player,
            AbilityData data,
            List<AbilityRequirement> requirements,
            String translationKey,
            int color
    ) {
        int y = startY;
        for (AbilityRequirement requirement : requirements) {
            Optional<Component> failure = requirement.validate(player, data);
            if (failure.isEmpty()) {
                continue;
            }

            Component requirementLine = Component.translatable(translationKey, failure.get());
            for (FormattedCharSequence line : this.font.split(requirementLine, DETAILS_PANEL_WIDTH)) {
                guiGraphics.drawString(this.font, line, x, y, color, false);
                y += 10;
            }
        }
        return y;
    }

    private void renderAbilityButtonIcons(GuiGraphics guiGraphics) {
        if (this.minecraft == null) {
            return;
        }

        for (int visibleIndex = 0; visibleIndex < this.abilityButtons.size(); visibleIndex++) {
            int abilityIndex = this.abilityScrollOffset + visibleIndex;
            if (abilityIndex >= this.visibleAbilities.size()) {
                continue;
            }

            Button button = this.abilityButtons.get(visibleIndex);
            if (!button.visible) {
                continue;
            }

            AbilityDefinition ability = this.visibleAbilities.get(abilityIndex);
            AbilityIconRenderer.render(guiGraphics, this.minecraft, ability.icon(), button.getX() + 3, button.getY() + 2, 16, 16);
        }
    }

    private void cycleFilter() {
        this.abilityFilter = AbilityFilter.values()[(this.abilityFilter.ordinal() + 1) % AbilityFilter.values().length];
        refreshVisibleState();
        updateAbilityButtons();
    }

    private void cycleSort() {
        this.abilitySort = AbilitySort.values()[(this.abilitySort.ordinal() + 1) % AbilitySort.values().length];
        refreshVisibleState();
        updateAbilityButtons();
    }

    private void updateFilterAndSortButtons() {
        if (this.filterButton != null) {
            this.filterButton.setMessage(this.abilityFilter.label());
        }
        if (this.sortButton != null) {
            this.sortButton.setMessage(this.abilitySort.label());
        }
    }

    private String normalizedSearchQuery() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(AbilityDefinition ability, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String name = ability.displayName().getString().toLowerCase(Locale.ROOT);
        return name.contains(query) || ability.id().toString().toLowerCase(Locale.ROOT).contains(query);
    }

    private Set<ResourceLocation> assignedAbilityIdsForCurrentTarget(AbilityData data, @org.jetbrains.annotations.Nullable ResourceLocation modeId) {
        Set<ResourceLocation> assignedIds = new LinkedHashSet<>();
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            AbilityLoadoutApi.assignedAbilityId(data, modeId, slot).ifPresent(assignedIds::add);
        }
        return Set.copyOf(assignedIds);
    }

    private Comparator<AbilityDefinition> abilityComparator(AbilityData data, @org.jetbrains.annotations.Nullable ResourceLocation modeId) {
        Comparator<AbilityDefinition> byName = Comparator.comparing((AbilityDefinition ability) -> ability.displayName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(ability -> ability.id().toString());
        return switch (this.abilitySort) {
            case NAME -> byName;
            case ASSIGNED -> Comparator.comparingInt((AbilityDefinition ability) -> assignedSlotIndex(data, modeId, ability.id()))
                    .thenComparing(byName);
            case COOLDOWN -> Comparator.comparingInt(AbilityDefinition::cooldownTicks).thenComparing(byName);
        };
    }

    private int assignedSlotIndex(AbilityData data, @org.jetbrains.annotations.Nullable ResourceLocation modeId, ResourceLocation abilityId) {
        for (int slot = 0; slot < AbilityData.SLOT_COUNT; slot++) {
            if (AbilityLoadoutApi.assignedAbilityId(data, modeId, slot).filter(abilityId::equals).isPresent()) {
                return slot;
            }
        }
        return AbilityData.SLOT_COUNT;
    }

    private enum AbilityFilter {
        ALL("screen.xlib.ability_menu.filter.all"),
        ASSIGNABLE("screen.xlib.ability_menu.filter.assignable"),
        ASSIGNED("screen.xlib.ability_menu.filter.assigned");

        private final String translationKey;

        AbilityFilter(String translationKey) {
            this.translationKey = translationKey;
        }

        private Component label() {
            return Component.translatable(this.translationKey);
        }
    }

    private enum AbilitySort {
        NAME("screen.xlib.ability_menu.sort.name"),
        ASSIGNED("screen.xlib.ability_menu.sort.assigned"),
        COOLDOWN("screen.xlib.ability_menu.sort.cooldown");

        private final String translationKey;

        AbilitySort(String translationKey) {
            this.translationKey = translationKey;
        }

        private Component label() {
            return Component.translatable(this.translationKey);
        }
    }
}

