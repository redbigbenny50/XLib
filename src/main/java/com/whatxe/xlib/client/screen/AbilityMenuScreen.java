package com.whatxe.xlib.client.screen;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityData;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityGrantApi;
import com.whatxe.xlib.ability.AbilityLoadoutApi;
import com.whatxe.xlib.ability.AbilityLoadoutFeatureApi;
import com.whatxe.xlib.ability.AbilityLoadoutFeatureDecision;
import com.whatxe.xlib.ability.AbilityRequirement;
import com.whatxe.xlib.ability.AbilityResourceCost;
import com.whatxe.xlib.ability.AbilityResourceDefinition;
import com.whatxe.xlib.ability.AbilitySlotContainerApi;
import com.whatxe.xlib.ability.AbilitySlotContainerDefinition;
import com.whatxe.xlib.ability.AbilitySlotReference;
import com.whatxe.xlib.ability.ModeApi;
import com.whatxe.xlib.ability.ModeDefinition;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.client.AbilityMenuScreenContext;
import com.whatxe.xlib.client.AbilityMenuSessionState;
import com.whatxe.xlib.client.AbilityMenuSessionStateApi;
import com.whatxe.xlib.client.ProgressionMenuScreenFactoryApi;
import com.whatxe.xlib.client.AbilityIconRenderer;
import com.whatxe.xlib.client.AbilityContainerLayoutApi;
import com.whatxe.xlib.client.AbilityContainerLayoutDefinition;
import com.whatxe.xlib.client.ProgressionMenuScreenContext;
import com.whatxe.xlib.client.AbilitySlotLayoutAnchor;
import com.whatxe.xlib.client.AbilitySlotLayoutMode;
import com.whatxe.xlib.client.AbilitySlotLayoutPlanner;
import com.whatxe.xlib.client.AbilitySlotWidgetMetadata;
import com.whatxe.xlib.client.AbilitySlotWidgetRole;
import com.whatxe.xlib.menu.AbilityMenuAccessApi;
import com.whatxe.xlib.menu.AbilityMenuCatalog;
import com.whatxe.xlib.menu.MenuAccessDecision;
import com.whatxe.xlib.menu.ProgressionMenuAccessApi;
import com.whatxe.xlib.presentation.AbilityMenuPresentation;
import com.whatxe.xlib.presentation.AbilityMenuPresentationApi;
import com.whatxe.xlib.presentation.MenuPalette;
import com.whatxe.xlib.network.AssignAbilityPayload;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class AbilityMenuScreen extends Screen {
    private static final int SLOT_STRIP_X_OFFSET = 178;
    private static final int SLOT_AREA_Y = 70;
    private static final int SLOT_AREA_WIDTH = 360;
    private static final int SLOT_AREA_HEIGHT = 96;
    private static final int DETAILS_PANEL_WIDTH = 150;
    private static final int DETAILS_PANEL_X_OFFSET = 170;
    private static final int DETAILS_PANEL_Y = 124;
    private static final int ABILITY_BUTTON_WIDTH = 150;
    private static final int ABILITY_BUTTON_X_OFFSET = 20;
    private static final int ABILITY_PANEL_TOP_GAP = 8;
    private static final int ABILITY_PANEL_HEADER_HEIGHT = 18;
    private static final int ABILITY_PANEL_ROW_SPACING = 26;
    private static final int ABILITY_LIST_TOP_GAP = 4;
    private static final int ABILITY_BUTTON_SPACING = 24;
    private static final int MIN_VISIBLE_ABILITY_BUTTONS = 3;
    private static final int MAX_VISIBLE_ABILITY_BUTTONS = 7;
    private static final int FOOTER_BUTTON_SPACING = 26;
    private static final int PAGE_TAB_WIDTH = 18;
    private static final int PAGE_TAB_HEIGHT = 16;
    private static final int PASSIVE_PANEL_HEIGHT = 68;
    private static final int PASSIVE_ROW_HEIGHT = 18;
    private static final int PASSIVE_VISIBLE_ROWS = 2;
    private static final int PASSIVE_ICON_SIZE = 14;

    private final List<Button> slotButtons = new ArrayList<>();
    private final List<Button> pageTabButtons = new ArrayList<>();
    private final List<Button> abilityButtons = new ArrayList<>();
    private List<AbilityDefinition> visibleAbilities = List.of();
    private List<PassiveDefinition> visiblePassives = List.of();
    private List<AbilityDefinition> catalogAbilities = List.of();
    private List<ResourceLocation> editableModeIds = List.of();
    private List<ResourceLocation> editableContainerIds = List.of();
    private int selectedSlot = 0;
    private int abilityScrollOffset = 0;
    private int passiveScrollOffset = 0;
    private int visibleAbilityButtonCount = MIN_VISIBLE_ABILITY_BUTTONS;
    private int abilityButtonX;
    private Button clearSlotButton;
    private Button cycleLoadoutButton;
    private Button cycleContainerButton;
    private Button cycleContainerPageButton;
    private Button openProgressionButton;
    private Button filterButton;
    private Button sortButton;
    private Button pageButton;
    private Button groupButton;
    private Button familyButton;
    private EditBox searchBox;
    private int loadoutModeIndex = -1;
    private @Nullable ResourceLocation pendingEditingModeId;
    private AbilityFilter abilityFilter = AbilityFilter.ALL;
    private AbilitySort abilitySort = AbilitySort.CATALOG;
    private AbilityData cachedVisibleData = AbilityData.empty();
    private String cachedSearchQuery = "";
    private AbilityFilter cachedFilter = this.abilityFilter;
    private AbilitySort cachedSort = this.abilitySort;
    private @Nullable ResourceLocation selectedPageId;
    private @Nullable ResourceLocation selectedGroupId;
    private @Nullable ResourceLocation selectedFamilyId;
    private @Nullable ResourceLocation selectedContainerId;
    private int selectedContainerPage = 0;
    private @Nullable ResourceLocation cachedPageId;
    private @Nullable ResourceLocation cachedGroupId;
    private @Nullable ResourceLocation cachedFamilyId;
    private @Nullable ResourceLocation cachedContainerId;
    private int cachedContainerPage;
    private MenuAccessDecision currentMenuAccess = MenuAccessDecision.available();
    private MenuAccessDecision currentProgressionMenuAccess = MenuAccessDecision.available();
    private AbilityLoadoutFeatureDecision currentLoadoutFeatureDecision = AbilityLoadoutFeatureDecision.disabled();
    private @Nullable ResourceLocation selectedPassiveId;

    public AbilityMenuScreen() {
        this(AbilityMenuScreenContext.defaultContext());
    }

    public AbilityMenuScreen(AbilityMenuScreenContext context) {
        super(Component.translatable("screen.xlib.ability_menu"));
        AbilityMenuSessionState sessionState = context.sessionState();
        this.selectedSlot = Math.max(0, sessionState.selectedSlot());
        this.pendingEditingModeId = sessionState.editingModeId();
        this.selectedContainerId = AbilitySlotContainerApi.PRIMARY_CONTAINER_ID;
        this.selectedContainerPage = 0;
    }

    @Override
    protected void init() {
        buildMenuWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        refreshAccessState();
        if (this.currentMenuAccess.isHidden()) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(null);
            }
            return;
        }
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
        AbilityMenuPresentation presentation = currentPresentation();
        MenuPalette palette = presentation.palette();
        guiGraphics.fillGradient(0, 0, this.width, this.height, palette.backgroundTopColor(), palette.backgroundBottomColor());
        int detailsX = this.width / 2 - DETAILS_PANEL_X_OFFSET;
        int abilityPanelLeft = this.abilityButtonX - 6;
        AbilityData data = currentData();
        AbilityContainerLayoutDefinition slotLayout = currentContainerLayout();
        AbilitySlotLayoutPlanner.LayoutPlan slotPlan = currentSlotLayoutPlan();
        int abilityPanelTop = abilityPanelTop(slotLayout, slotPlan);
        int abilityPanelBottom = abilityPanelBottom();
        int slotOriginX = slotAreaOriginX(slotLayout, slotPlan);
        int slotOriginY = slotAreaOriginY(slotLayout, slotPlan);
        int detailsPanelY = detailsPanelY(slotLayout, slotPlan);
        guiGraphics.fill(detailsX - 8, detailsPanelY - 8, detailsX + DETAILS_PANEL_WIDTH + 8, abilityPanelBottom, palette.panelColor());
        guiGraphics.fill(abilityPanelLeft, abilityPanelTop, abilityPanelLeft + ABILITY_BUTTON_WIDTH + 18, abilityPanelBottom, palette.secondaryPanelColor());

        int slotCount = this.slotButtons.size();
        if (slotLayout.layoutMode() != AbilitySlotLayoutMode.RADIAL) {
            guiGraphics.fill(
                    slotOriginX - 6,
                    slotOriginY - 14,
                    slotOriginX + slotPlan.width() + 6,
                    slotOriginY + slotPlan.height() + 6,
                    palette.secondaryPanelColor()
            );
        }
        for (AbilitySlotLayoutPlanner.CategoryPlacement placement : slotPlan.categories()) {
            guiGraphics.drawString(
                    this.font,
                    placement.label(),
                    slotOriginX + placement.x(),
                    slotOriginY + placement.y(),
                    palette.infoColor(),
                    false
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderSlotButtonOverlays(guiGraphics, palette);

        guiGraphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 16, palette.titleColor(), false);
        int infoX = this.width / 2 - 150;
        int infoY = drawCenteredWrappedText(
                guiGraphics,
                this.width / 2,
                34,
                300,
                this.currentLoadoutFeatureDecision.managementEnabled()
                        ? Component.translatable("screen.xlib.ability_menu.instructions")
                        : Component.translatable("screen.xlib.ability_menu.instructions_basic"),
                palette.bodyColor()
        );
        if (this.currentMenuAccess.isLocked()) {
            drawWrappedText(
                    guiGraphics,
                    infoX,
                    infoY + 2,
                    300,
                    Component.translatable(
                            "screen.xlib.menu_locked",
                            this.currentMenuAccess.reason().orElse(Component.translatable("screen.xlib.progression_menu.locked"))
                    ),
                    palette.warningColor()
            );
        }
        Component availableAbilitiesLabel = Component.translatable("screen.xlib.available_abilities_count", this.visibleAbilities.size());
        guiGraphics.drawString(
                this.font,
                availableAbilitiesLabel,
                abilityPanelLeft + (ABILITY_BUTTON_WIDTH + 18 - this.font.width(availableAbilitiesLabel)) / 2,
                abilityPanelTop + 5,
                palette.titleColor(),
                false
        );

        for (int slot = 0; slot < slotCount; slot++) {
            Button slotButton = this.slotButtons.get(slot);
            int x = slotButton.getX();
            int y = slotButton.getY();
            Optional<AbilityDefinition> maybeAbility = resolveVisibleAbility(assignedAbilityIdForCurrentTarget(data, slot));
            if (maybeAbility.isPresent()) {
                if (slot == this.selectedSlot) {
                    guiGraphics.fill(
                            x + 8,
                            y + 1,
                            x + 28,
                            y + 19,
                            withAlpha(palette.highlightColor(), 0x66)
                    );
                }
                AbilityIconRenderer.render(guiGraphics, this.minecraft, maybeAbility.get().icon(), x + 10, y + 2, 16, 16);
            } else {
                String slotLabel = slotButtonLabel(slot);
                guiGraphics.drawString(
                        this.font,
                        slotLabel,
                        x + 18 - this.font.width(slotLabel) / 2,
                        y + 6,
                        palette.infoColor(),
                        false
                );
            }
        }

        Optional<AbilityDefinition> selectedAbility = resolveVisibleAbility(assignedAbilityIdForCurrentTarget(data, this.selectedSlot));
        Component slotTitle = Component.translatable(
                "screen.xlib.selected_slot",
                selectedSlotDisplayLabel(),
                selectedAbility.map(AbilityDefinition::displayName).orElse(Component.translatable("screen.xlib.empty_slot"))
        );
        guiGraphics.drawString(this.font, slotTitle, detailsX, detailsPanelY, 0xFFFFFF, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.xlib.container_target", containerTargetLabel()),
                detailsX,
                detailsPanelY + 10,
                palette.infoColor(),
                false
        );
        int metadataY = detailsPanelY + 20;
        metadataY = appendSlotMetadataLine(guiGraphics, detailsX, metadataY, slotMetadata(this.selectedSlot).categoryLabel(),
                "screen.xlib.slot_category_stat", palette.infoColor());
        metadataY = appendSlotMetadataLine(guiGraphics, detailsX, metadataY, currentSlotRoleLabel(),
                "screen.xlib.slot_role_stat", palette.familyColor());
        metadataY = appendSlotMetadataLine(guiGraphics, detailsX, metadataY, currentSlotInputHint(),
                "screen.xlib.slot_input_stat", palette.tagColor());
        metadataY = appendSlotMetadataLine(guiGraphics, detailsX, metadataY, currentContainerOwnerLabel(),
                "screen.xlib.container_owner_stat", palette.groupColor());
        if (slotMetadata(this.selectedSlot).softLocked()) {
            guiGraphics.drawString(this.font, Component.translatable("screen.xlib.slot_soft_locked_stat"), detailsX, metadataY, palette.warningColor(), false);
            metadataY += 10;
        }
        metadataY = appendSlotMetadataLine(guiGraphics, detailsX, metadataY, slotMetadata(this.selectedSlot).roleHint(),
                "screen.xlib.slot_hint_stat", palette.requirementColor());
        selectedAbility.ifPresent(ability -> AbilityIconRenderer.render(
                guiGraphics,
                this.minecraft,
                ability.icon(),
                detailsX + DETAILS_PANEL_WIDTH - 18,
                detailsPanelY - 2,
                16,
                16
        ));

        Component description = selectedAbility
                .map(AbilityDefinition::description)
                .orElse(Component.translatable("screen.xlib.empty_slot_desc"));
        int descriptionY = Math.max(detailsPanelY + 28, metadataY + 4);
        int detailBottom = detailSectionBottom();
        Player player = currentPlayer();
        for (FormattedCharSequence line : this.font.split(description, DETAILS_PANEL_WIDTH)) {
            if (!hasDetailRoom(descriptionY, detailBottom)) {
                break;
            }
            guiGraphics.drawString(this.font, line, detailsX, descriptionY, palette.bodyColor(), false);
            descriptionY += 10;
        }

        descriptionY += 6;
        if (selectedAbility.isPresent()) {
            AbilityDefinition ability = selectedAbility.get();
            if (ability.usesCharges() && hasDetailRoom(descriptionY, detailBottom)) {
                Component charges = Component.translatable(
                        "screen.xlib.ability_charges_stat",
                        data.chargeCountFor(ability.id(), ability.maxCharges()),
                        ability.maxCharges()
                );
                guiGraphics.drawString(this.font, charges, detailsX, descriptionY, palette.emphasisColor(), false);
                descriptionY += 10;
            } else if (ability.cooldownTicks() > 0 && hasDetailRoom(descriptionY, detailBottom)) {
                Component cooldown = Component.translatable(
                        "screen.xlib.ability_cooldown_stat",
                        formatSeconds(ability.cooldownTicks())
                );
                guiGraphics.drawString(this.font, cooldown, detailsX, descriptionY, palette.emphasisColor(), false);
                descriptionY += 10;
            }

            if (ability.toggleAbility() && ability.durationTicks() > 0 && hasDetailRoom(descriptionY, detailBottom)) {
                Component duration = Component.translatable(
                        "screen.xlib.ability_duration_stat",
                        formatSeconds(ability.durationTicks())
                );
                guiGraphics.drawString(this.font, duration, detailsX, descriptionY, palette.successColor(), false);
                descriptionY += 10;
            }

            for (AbilityResourceCost cost : ability.resourceCosts()) {
                if (!hasDetailRoom(descriptionY, detailBottom)) {
                    break;
                }
                Optional<AbilityResourceDefinition> resource = AbilityApi.findResource(cost.resourceId());
                Component costLine = Component.translatable(
                        "screen.xlib.ability_cost_stat",
                        cost.amount(),
                        resource.map(AbilityResourceDefinition::displayName).orElse(Component.literal(displayMetadataLabel(cost.resourceId())))
                );
                guiGraphics.drawString(this.font, costLine, detailsX, descriptionY, palette.infoColor(), false);
                descriptionY += 10;
            }

            if (presentation.showMetadataDetails()) {
                descriptionY = appendOptionalMetadataLine(guiGraphics, detailsX, descriptionY, ability.familyId(),
                        "screen.xlib.ability_family_stat", palette.familyColor(), detailBottom);
                descriptionY = appendOptionalMetadataLine(guiGraphics, detailsX, descriptionY, ability.groupId(),
                        "screen.xlib.ability_group_stat", palette.groupColor(), detailBottom);
                descriptionY = appendOptionalMetadataLine(guiGraphics, detailsX, descriptionY, ability.pageId(),
                        "screen.xlib.ability_page_stat", palette.pageColor(), detailBottom);
                descriptionY = appendTagLine(guiGraphics, detailsX, descriptionY, ability.tags(), palette.tagColor(), detailBottom);
            }

            if (presentation.showRequirementBreakdown()) {
                descriptionY = appendRequirementLines(guiGraphics, detailsX, descriptionY, player, data, ability.assignRequirements(),
                        "screen.xlib.ability_assign_requirement_stat", palette.emphasisColor(), detailBottom);
                descriptionY = appendRequirementLines(guiGraphics, detailsX, descriptionY, player, data, ability.activateRequirements(),
                        "screen.xlib.ability_activate_requirement_stat", palette.requirementColor(), detailBottom);
                descriptionY = appendRequirementLines(guiGraphics, detailsX, descriptionY, player, data, ability.stayActiveRequirements(),
                        "screen.xlib.ability_sustain_requirement_stat", palette.infoColor(), detailBottom);
            }
        }

        renderPassivePanel(guiGraphics, palette);

        renderAbilityButtonIcons(guiGraphics);

        if (maxAbilityScrollOffset() > 0) {
            renderScrollBar(guiGraphics);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverPassivePanel(mouseX, mouseY) && maxPassiveScrollOffset() > 0) {
            this.passiveScrollOffset = Mth.clamp(this.passiveScrollOffset + (scrollY < 0 ? 1 : -1), 0, maxPassiveScrollOffset());
            return true;
        }
        if (isMouseOverAbilityList(mouseX, mouseY) && maxAbilityScrollOffset() > 0) {
            scrollAbilityList(scrollY < 0 ? 1 : -1);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int passiveIndex = passiveIndexAt(mouseX, mouseY);
        if (passiveIndex >= 0 && passiveIndex < this.visiblePassives.size()) {
            this.selectedPassiveId = this.visiblePassives.get(passiveIndex).id();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void buildMenuWidgets() {
        this.clearWidgets();
        this.slotButtons.clear();
        this.pageTabButtons.clear();
        this.abilityButtons.clear();
        this.filterButton = null;
        this.sortButton = null;
        this.pageButton = null;
        this.groupButton = null;
        this.familyButton = null;
        this.abilityFilter = AbilityFilter.ALL;
        this.abilitySort = AbilitySort.CATALOG;
        this.selectedPageId = null;
        this.selectedGroupId = null;
        this.selectedFamilyId = null;
        refreshAccessState();
        refreshVisibleState();

        int slotCount = currentContainerSlotCount();
        this.selectedSlot = Mth.clamp(this.selectedSlot, 0, Math.max(0, slotCount - 1));
        AbilityContainerLayoutDefinition slotLayout = currentContainerLayout();
        AbilitySlotLayoutPlanner.LayoutPlan slotPlan = currentSlotLayoutPlan();
        int buttonRowX = slotAreaOriginX(slotLayout, slotPlan);
        int buttonRowY = slotAreaOriginY(slotLayout, slotPlan);
        for (int slot = 0; slot < slotCount; slot++) {
            final int targetSlot = slot;
            AbilitySlotLayoutPlanner.SlotPlacement placement = slotPlan.slots().get(slot);
            Button button = Button.builder(Component.empty(), pressed -> {
                this.selectedSlot = targetSlot;
                updateSlotButtons();
            }).bounds(buttonRowX + placement.x(), buttonRowY + placement.y(), 36, 20).build();
            this.slotButtons.add(this.addRenderableWidget(button));
        }
        int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(currentData(), currentContainerId()));
        for (AbilitySlotLayoutPlanner.PageTabPlacement placement :
                AbilitySlotLayoutPlanner.planPageTabs(slotLayout, slotPlan, pageCount, PAGE_TAB_WIDTH, PAGE_TAB_HEIGHT, 4)) {
            final int pageIndex = placement.pageIndex();
            Button button = Button.builder(Component.empty(), pressed -> setContainerPage(pageIndex))
                    .bounds(buttonRowX + placement.x(), buttonRowY + placement.y(), PAGE_TAB_WIDTH, PAGE_TAB_HEIGHT)
                    .build();
            this.pageTabButtons.add(this.addRenderableWidget(button));
        }

        this.abilityButtonX = this.width / 2 + ABILITY_BUTTON_X_OFFSET;
        this.visibleAbilityButtonCount = computeVisibleAbilityButtonCount();
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset, 0, maxAbilityScrollOffset());

        int searchBoxY = searchBoxY(slotLayout, slotPlan);
        int abilityListY = abilityListY(slotLayout, slotPlan);

        this.searchBox = this.addRenderableWidget(new EditBox(this.font, this.abilityButtonX, searchBoxY, ABILITY_BUTTON_WIDTH, 20, Component.empty()));
        this.searchBox.setHint(Component.translatable("screen.xlib.ability_menu.search_hint"));
        this.searchBox.setResponder(value -> {
            refreshVisibleState();
            updateAbilityButtons();
        });

        for (int index = 0; index < this.visibleAbilityButtonCount; index++) {
            final int visibleIndex = index;
            Button abilityButton = Button.builder(Component.literal(""), pressed -> assignVisibleAbility(visibleIndex))
                    .bounds(this.abilityButtonX, abilityListY + index * ABILITY_BUTTON_SPACING, ABILITY_BUTTON_WIDTH, 20)
                    .build();
            this.abilityButtons.add(this.addRenderableWidget(abilityButton));
        }

        int clearButtonY = abilityListY + this.visibleAbilityButtonCount * ABILITY_BUTTON_SPACING + 8;
        int detailsX = this.width / 2 - DETAILS_PANEL_X_OFFSET;
        int footerButtonY = clearButtonY - FOOTER_BUTTON_SPACING;
        this.clearSlotButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.xlib.clear_slot"), pressed -> assignAbility(Optional.empty()))
                .bounds(this.abilityButtonX, clearButtonY, ABILITY_BUTTON_WIDTH, 20)
                .build());
        this.cycleContainerButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleContainerTarget())
                .bounds(detailsX, footerButtonY - FOOTER_BUTTON_SPACING, DETAILS_PANEL_WIDTH, 20)
                .build());
        this.cycleContainerPageButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleContainerPage())
                .bounds(detailsX, footerButtonY - FOOTER_BUTTON_SPACING * 2, DETAILS_PANEL_WIDTH, 20)
                .build());
        this.cycleLoadoutButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleLoadoutTarget())
                .bounds(detailsX, footerButtonY, DETAILS_PANEL_WIDTH, 20)
                .build());
        this.openProgressionButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.xlib.open_progression"),
                        pressed -> {
                    if (this.minecraft != null && !this.currentProgressionMenuAccess.isHidden()) {
                                ProgressionMenuScreenFactoryApi.openActive(this.minecraft, ProgressionMenuScreenContext.fromCurrentState(this));
                            }
                        })
                .bounds(detailsX, clearButtonY, DETAILS_PANEL_WIDTH, 20)
                .build());

        updateSlotButtons();
        updateAbilityButtons();
        updateContainerButtons();
        updateLoadoutButton();
    }

    private void updateSlotButtons() {
        int slotCount = currentContainerSlotCount();
        this.selectedSlot = Mth.clamp(this.selectedSlot, 0, Math.max(0, slotCount - 1));
        for (int slot = 0; slot < this.slotButtons.size(); slot++) {
            this.slotButtons.get(slot).setMessage(Component.empty());
        }
        for (int pageIndex = 0; pageIndex < this.pageTabButtons.size(); pageIndex++) {
            Button button = this.pageTabButtons.get(pageIndex);
            button.setMessage(Component.literal(pageIndex == this.selectedContainerPage ? "[" + (pageIndex + 1) + "]" : Integer.toString(pageIndex + 1)));
            button.active = pageIndex != this.selectedContainerPage;
        }
        syncSessionState();
    }

    private void updateAbilityButtons() {
        refreshVisibleState();
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset, 0, maxAbilityScrollOffset());
        updateLoadoutButton();
        updateContainerButtons();
        updateFilterAndSortButtons();
        updateScopeButtons();
        for (int visibleIndex = 0; visibleIndex < this.abilityButtons.size(); visibleIndex++) {
            int abilityIndex = this.abilityScrollOffset + visibleIndex;
            Button button = this.abilityButtons.get(visibleIndex);
            if (abilityIndex < this.visibleAbilities.size()) {
                AbilityDefinition ability = this.visibleAbilities.get(abilityIndex);
                button.visible = true;
                button.active = this.currentMenuAccess.isAvailable()
                        && currentPlayer() != null
                        && !currentSlotSoftLocked()
                        && AbilityGrantApi.canAssign(currentPlayer(), currentData(), ability);
                button.setMessage(abilityButtonLabel(ability));
            } else {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.literal(""));
            }
        }
        if (this.clearSlotButton != null) {
            this.clearSlotButton.active = this.currentMenuAccess.isAvailable() && !currentSlotSoftLocked();
        }
        if (this.openProgressionButton != null) {
            this.openProgressionButton.active = !this.currentProgressionMenuAccess.isHidden();
        }
    }

    private void assignVisibleAbility(int visibleIndex) {
        if (!this.currentMenuAccess.isAvailable()) {
            return;
        }
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
        int availableHeight = Math.max(80, this.height - abilityListY() - 72);
        int visibleCount = (availableHeight + 4) / ABILITY_BUTTON_SPACING;
        return Mth.clamp(visibleCount, MIN_VISIBLE_ABILITY_BUTTONS, MAX_VISIBLE_ABILITY_BUTTONS);
    }

    private int maxPassiveScrollOffset() {
        return Math.max(0, this.visiblePassives.size() - PASSIVE_VISIBLE_ROWS);
    }

    private int maxAbilityScrollOffset() {
        return Math.max(0, this.visibleAbilities.size() - this.visibleAbilityButtonCount);
    }

    private boolean isMouseOverAbilityList(double mouseX, double mouseY) {
        if (this.abilityButtons.isEmpty()) {
            return false;
        }

        int left = this.abilityButtonX - 6;
        int top = abilityListY() - 18;
        int right = this.abilityButtonX + ABILITY_BUTTON_WIDTH + 18;
        int bottom = abilityListBottom();
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        MenuPalette palette = currentPresentation().palette();
        int trackX = this.abilityButtonX + ABILITY_BUTTON_WIDTH + 6;
        int trackY = abilityListY();
        int trackHeight = this.visibleAbilityButtonCount * ABILITY_BUTTON_SPACING - 4;
        int maxOffset = maxAbilityScrollOffset();
        int thumbHeight = Math.max(12, trackHeight * this.visibleAbilityButtonCount / this.visibleAbilities.size());
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (maxOffset == 0 ? 0 : Math.round((float)this.abilityScrollOffset / maxOffset * thumbTravel));

        guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackHeight, palette.scrollbarTrackColor());
        guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, palette.scrollbarThumbColor());
    }

    private int abilityListBottom() {
        return abilityListY() + this.visibleAbilityButtonCount * ABILITY_BUTTON_SPACING - 4;
    }

    private int abilityPanelBottom() {
        return this.clearSlotButton != null ? this.clearSlotButton.getY() + 26 : abilityListY() + 90;
    }

    private int passivePanelTop() {
        int panelBottom = passivePanelBottom();
        return panelBottom - PASSIVE_PANEL_HEIGHT;
    }

    private int passivePanelBottom() {
        return this.cycleLoadoutButton != null ? this.cycleLoadoutButton.getY() - 8 : abilityPanelBottom() - 8;
    }

    private int detailSectionBottom() {
        return passivePanelTop() - 6;
    }

    private void assignAbility(Optional<ResourceLocation> abilityId) {
        if (!this.currentMenuAccess.isAvailable() || currentSlotSoftLocked()) {
            return;
        }
        PacketDistributor.sendToServer(new AssignAbilityPayload(currentSlotReference(), abilityId, currentEditingModeId()));
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

    private AbilityMenuPresentation currentPresentation() {
        return AbilityMenuPresentationApi.active();
    }

    private AbilityContainerLayoutDefinition currentContainerLayout() {
        return AbilityContainerLayoutApi.resolvedLayout(currentContainerId());
    }

    private AbilitySlotLayoutPlanner.LayoutPlan currentSlotLayoutPlan() {
        return AbilitySlotLayoutPlanner.plan(currentContainerLayout(), currentContainerSlotCount(), 36, 20, 4);
    }

    private int slotAreaOriginX(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        int areaLeft = this.width / 2 - SLOT_STRIP_X_OFFSET;
        return switch (layout.anchor()) {
            case TOP_LEFT, LEFT_MIDDLE, BOTTOM_LEFT -> areaLeft;
            case TOP_RIGHT, RIGHT_MIDDLE, BOTTOM_RIGHT -> areaLeft + Math.max(0, SLOT_AREA_WIDTH - plan.width());
            case BOTTOM_CENTER -> areaLeft + Math.max(0, (SLOT_AREA_WIDTH - plan.width()) / 2);
        };
    }

    private int slotAreaOriginY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        int centeredY = SLOT_AREA_Y + Math.max(0, (SLOT_AREA_HEIGHT - plan.height()) / 2);
        return switch (layout.anchor()) {
            case TOP_LEFT, TOP_RIGHT -> SLOT_AREA_Y;
            case LEFT_MIDDLE, RIGHT_MIDDLE, BOTTOM_CENTER -> centeredY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> SLOT_AREA_Y + Math.max(0, SLOT_AREA_HEIGHT - plan.height());
        };
    }

    private int slotSectionBottom(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        int slotBottom = slotAreaOriginY(layout, plan) + plan.height();
        int pageCount = Math.max(1, AbilitySlotContainerApi.resolvedPageCount(currentData(), currentContainerId()));
        for (AbilitySlotLayoutPlanner.PageTabPlacement placement : AbilitySlotLayoutPlanner.planPageTabs(
                layout,
                plan,
                pageCount,
                PAGE_TAB_WIDTH,
                PAGE_TAB_HEIGHT,
                4
        )) {
            slotBottom = Math.max(slotBottom, slotAreaOriginY(layout, plan) + placement.y() + PAGE_TAB_HEIGHT);
        }
        return slotBottom;
    }

    private int abilityPanelTop(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return slotSectionBottom(layout, plan) + ABILITY_PANEL_TOP_GAP;
    }

    private int searchBoxY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return abilityPanelTop(layout, plan) + ABILITY_PANEL_HEADER_HEIGHT;
    }

    private int filterRowY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return searchBoxY(layout, plan) + ABILITY_PANEL_ROW_SPACING;
    }

    private int pageRowY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return filterRowY(layout, plan) + ABILITY_PANEL_ROW_SPACING;
    }

    private int groupRowY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return pageRowY(layout, plan) + ABILITY_PANEL_ROW_SPACING;
    }

    private int familyRowY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return groupRowY(layout, plan) + ABILITY_PANEL_ROW_SPACING;
    }

    private int abilityListY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return searchBoxY(layout, plan) + ABILITY_PANEL_ROW_SPACING + ABILITY_LIST_TOP_GAP;
    }

    private int abilityListY() {
        return abilityListY(currentContainerLayout(), currentSlotLayoutPlan());
    }

    private int detailsPanelY(AbilityContainerLayoutDefinition layout, AbilitySlotLayoutPlanner.LayoutPlan plan) {
        return Math.max(DETAILS_PANEL_Y, slotSectionBottom(layout, plan) + 12);
    }

    private AbilitySlotWidgetMetadata slotMetadata(int slotIndex) {
        return currentContainerLayout().slotMetadata(slotIndex);
    }

    private boolean currentSlotSoftLocked() {
        return slotMetadata(this.selectedSlot).softLocked();
    }

    private boolean hasDetailRoom(int y, int maxY) {
        return y + 9 <= maxY;
    }

    private String slotButtonLabel(int slotIndex) {
        AbilitySlotWidgetMetadata metadata = slotMetadata(slotIndex);
        String label = metadata.shortLabel() != null && !metadata.shortLabel().getString().isBlank()
                ? metadata.shortLabel().getString()
                : Integer.toString(slotIndex + 1);
        return slotIndex == this.selectedSlot ? "[" + label + "]" : label;
    }

    private Component selectedSlotDisplayLabel() {
        AbilitySlotWidgetMetadata metadata = slotMetadata(this.selectedSlot);
        return metadata.shortLabel() != null ? metadata.shortLabel() : Component.literal(Integer.toString(this.selectedSlot + 1));
    }

    private Optional<Component> currentSlotRoleLabel() {
        AbilitySlotWidgetRole role = slotMetadata(this.selectedSlot).role();
        return role == AbilitySlotWidgetRole.STANDARD
                ? Optional.empty()
                : Optional.of(Component.literal(role.name().toLowerCase(Locale.ROOT).replace('_', ' ')));
    }

    private Optional<Component> currentSlotInputHint() {
        if (this.minecraft == null) {
            return Optional.empty();
        }
        return com.whatxe.xlib.client.AbilityControlInputHandler.slotHint(this.minecraft, currentSlotReference()).map(Component::literal);
    }

    private Optional<Component> currentContainerOwnerLabel() {
        return AbilitySlotContainerApi.findContainer(currentContainerId())
                .map(definition -> {
                    String ownerLabel = humanize(definition.ownerType().name().toLowerCase(Locale.ROOT));
                    if (definition.ownerId() != null) {
                        ownerLabel = ownerLabel + " (" + displayMetadataLabel(definition.ownerId()) + ")";
                    }
                    return Component.literal(ownerLabel);
                });
    }

    private void setContainerPage(int pageIndex) {
        this.selectedContainerPage = 0;
    }

    private Component abilityButtonLabel(AbilityDefinition ability) {
        MutableComponent label = Component.literal("   ").append(ability.displayName());
        metadataDetailId(ability, currentPresentation().entryDetail())
                .map(AbilityMenuScreen::displayMetadataLabel)
                .ifPresent(detail -> label.append(Component.literal(" - " + detail)));
        return label;
    }

    private Optional<ResourceLocation> metadataDetailId(
            AbilityDefinition ability,
            AbilityMenuPresentation.EntryDetail entryDetail
    ) {
        return switch (entryDetail) {
            case NONE -> Optional.empty();
            case FAMILY -> ability.familyId();
            case GROUP -> ability.groupId();
            case PAGE -> ability.pageId();
        };
    }

    private void refreshVisibleState() {
        AbilityData data = currentData();
        Player player = currentPlayer();
        refreshAccessState();
        if (player == null) {
            this.visibleAbilities = List.of();
            this.visiblePassives = List.of();
            this.catalogAbilities = List.of();
            this.editableModeIds = List.of();
            this.editableContainerIds = List.of(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID);
            this.loadoutModeIndex = -1;
            this.pendingEditingModeId = null;
            this.cachedVisibleData = AbilityData.empty();
            this.cachedSearchQuery = "";
            this.selectedPageId = null;
            this.selectedGroupId = null;
            this.selectedFamilyId = null;
            this.cachedContainerId = this.selectedContainerId;
            this.cachedContainerPage = this.selectedContainerPage;
            this.selectedContainerId = AbilitySlotContainerApi.PRIMARY_CONTAINER_ID;
            this.selectedContainerPage = 0;
            this.selectedPassiveId = null;
            this.passiveScrollOffset = 0;
            syncSessionState();
            return;
        }

        String query = normalizedSearchQuery();
        if (data.equals(this.cachedVisibleData) && query.equals(this.cachedSearchQuery)
                && this.cachedFilter == this.abilityFilter && this.cachedSort == this.abilitySort
                && Objects.equals(this.cachedPageId, this.selectedPageId)
                && Objects.equals(this.cachedGroupId, this.selectedGroupId)
                && Objects.equals(this.cachedFamilyId, this.selectedFamilyId)
                && Objects.equals(this.cachedContainerId, this.selectedContainerId)
                && this.cachedContainerPage == this.selectedContainerPage) {
            return;
        }

        this.cachedVisibleData = data;
        this.cachedSearchQuery = query;
        this.cachedFilter = this.abilityFilter;
        this.cachedSort = this.abilitySort;
        this.editableModeIds = computeEditableModeIds(data, player);
        this.editableContainerIds = computeEditableContainerIds(data, player);
        applyPendingContainerSelection();
        applyPendingEditingMode();
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
        this.catalogAbilities = List.copyOf(filteredAbilities);
        this.visiblePassives = data.grantedPassives().stream()
                .map(PassiveApi::findPassive)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing((PassiveDefinition passive) -> passive.displayName().getString().toLowerCase(Locale.ROOT))
                        .thenComparing(passive -> passive.id().toString()))
                .toList();
        if (this.selectedPassiveId == null || this.visiblePassives.stream().noneMatch(passive -> passive.id().equals(this.selectedPassiveId))) {
            this.selectedPassiveId = this.visiblePassives.isEmpty() ? null : this.visiblePassives.getFirst().id();
        }
        this.passiveScrollOffset = Mth.clamp(this.passiveScrollOffset, 0, maxPassiveScrollOffset());
        AbilityMenuCatalog.Scope scope = AbilityMenuCatalog.sanitizeScope(
                this.catalogAbilities,
                new AbilityMenuCatalog.Scope(this.selectedPageId, this.selectedGroupId, this.selectedFamilyId)
        );
        this.selectedPageId = scope.pageId();
        this.selectedGroupId = scope.groupId();
        this.selectedFamilyId = scope.familyId();
        this.cachedPageId = this.selectedPageId;
        this.cachedGroupId = this.selectedGroupId;
        this.cachedFamilyId = this.selectedFamilyId;
        this.cachedContainerId = this.selectedContainerId;
        this.cachedContainerPage = this.selectedContainerPage;

        List<AbilityDefinition> scopedAbilities = new ArrayList<>(AbilityMenuCatalog.filter(this.catalogAbilities, scope));
        scopedAbilities.sort(abilityComparator(data, editingModeId.orElse(null)));
        this.visibleAbilities = List.copyOf(scopedAbilities);
        this.abilityScrollOffset = Mth.clamp(this.abilityScrollOffset, 0, maxAbilityScrollOffset());
        syncSessionState();
    }

    private void refreshAccessState() {
        Player player = currentPlayer();
        AbilityData data = player == null ? AbilityData.empty() : ModAttachments.get(player);
        this.currentMenuAccess = AbilityMenuAccessApi.decision(player, data);
        this.currentProgressionMenuAccess = ProgressionMenuAccessApi.decision(player);
        this.currentLoadoutFeatureDecision = AbilityLoadoutFeatureApi.decision(player, data);
    }

    private Player currentPlayer() {
        return this.minecraft != null ? this.minecraft.player : null;
    }

    private Optional<ResourceLocation> assignedAbilityIdForCurrentTarget(AbilityData data, int slot) {
        return AbilityLoadoutApi.assignedAbilityId(data, currentEditingModeId().orElse(null), slotReference(slot));
    }

    private List<ResourceLocation> computeEditableModeIds(AbilityData data, Player player) {
        if (!this.currentLoadoutFeatureDecision.managementEnabled()) {
            this.loadoutModeIndex = -1;
            return List.of();
        }
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

    private List<ResourceLocation> computeEditableContainerIds(AbilityData data, Player player) {
        return List.of(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID);
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
            syncSessionState();
            return;
        }

        int nextIndex = this.loadoutModeIndex + 1;
        this.loadoutModeIndex = nextIndex >= this.editableModeIds.size() ? -1 : nextIndex;
        refreshVisibleState();
        updateAbilityButtons();
        updateLoadoutButton();
        syncSessionState();
    }

    private void cycleContainerTarget() {
        this.selectedContainerId = AbilitySlotContainerApi.PRIMARY_CONTAINER_ID;
        this.selectedContainerPage = 0;
    }

    private void cycleContainerPage() {
        this.selectedContainerPage = 0;
    }

    private void updateLoadoutButton() {
        if (this.cycleLoadoutButton == null) {
            return;
        }
        this.cycleLoadoutButton.visible = this.currentLoadoutFeatureDecision.managementEnabled();
        this.cycleLoadoutButton.active = this.currentLoadoutFeatureDecision.managementEnabled() && !this.editableModeIds.isEmpty();
        this.cycleLoadoutButton.setMessage(Component.translatable("screen.xlib.loadout_target", loadoutTargetLabel()));
    }

    private void updateContainerButtons() {
        if (this.cycleContainerButton != null) {
            this.cycleContainerButton.visible = false;
            this.cycleContainerButton.active = false;
        }
        if (this.cycleContainerPageButton != null) {
            this.cycleContainerPageButton.visible = false;
            this.cycleContainerPageButton.active = false;
        }
    }

    private void applyPendingEditingMode() {
        if (this.pendingEditingModeId == null) {
            return;
        }
        int requestedIndex = this.editableModeIds.indexOf(this.pendingEditingModeId);
        this.loadoutModeIndex = requestedIndex >= 0 ? requestedIndex : -1;
        this.pendingEditingModeId = null;
    }

    private void applyPendingContainerSelection() {
        this.selectedContainerId = AbilitySlotContainerApi.PRIMARY_CONTAINER_ID;
        this.selectedContainerPage = 0;
    }

    private void syncSessionState() {
        AbilityMenuSessionStateApi.setState(AbilityMenuSessionState.of(
                this.selectedSlot,
                currentEditingModeId().orElse(null)
        ));
    }

    private Component loadoutTargetLabel() {
        return currentEditingModeId()
                .flatMap(AbilityApi::findAbility)
                .map(AbilityDefinition::displayName)
                .orElse(Component.translatable("screen.xlib.loadout_target.base"));
    }

    private Component containerTargetLabel() {
        return AbilitySlotContainerApi.findContainer(currentContainerId())
                .map(AbilitySlotContainerDefinition::displayName)
                .orElse(Component.literal(displayMetadataLabel(currentContainerId())));
    }

    private ResourceLocation currentContainerId() {
        return AbilitySlotContainerApi.PRIMARY_CONTAINER_ID;
    }

    private int currentContainerSlotCount() {
        return AbilityData.SLOT_COUNT;
    }

    private AbilitySlotReference slotReference(int slotIndex) {
        return AbilitySlotReference.primary(slotIndex);
    }

    private AbilitySlotReference currentSlotReference() {
        return slotReference(this.selectedSlot);
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
        return appendRequirementLines(guiGraphics, x, startY, player, data, requirements, translationKey, color, Integer.MAX_VALUE);
    }

    private int appendRequirementLines(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Player player,
            AbilityData data,
            List<AbilityRequirement> requirements,
            String translationKey,
            int color,
            int maxY
    ) {
        int y = startY;
        for (AbilityRequirement requirement : requirements) {
            Optional<Component> failure = requirement.validate(player, data);
            if (failure.isEmpty()) {
                continue;
            }

            Component requirementLine = Component.translatable(translationKey, failure.get());
            for (FormattedCharSequence line : this.font.split(requirementLine, DETAILS_PANEL_WIDTH)) {
                if (!hasDetailRoom(y, maxY)) {
                    return y;
                }
                guiGraphics.drawString(this.font, line, x, y, color, false);
                y += 10;
            }
        }
        return y;
    }

    private int appendOptionalMetadataLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Optional<ResourceLocation> maybeId,
            String translationKey,
            int color
    ) {
        return appendOptionalMetadataLine(guiGraphics, x, startY, maybeId, translationKey, color, Integer.MAX_VALUE);
    }

    private int appendOptionalMetadataLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Optional<ResourceLocation> maybeId,
            String translationKey,
            int color,
            int maxY
    ) {
        if (maybeId.isEmpty()) {
            return startY;
        }
        return appendWrappedLine(guiGraphics, x, startY, Component.translatable(translationKey, Component.literal(displayMetadataLabel(maybeId.get()))), color, maxY);
    }

    private int appendSlotMetadataLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Optional<Component> value,
            String translationKey,
            int color
    ) {
        if (value.isEmpty()) {
            return startY;
        }
        return appendWrappedLine(guiGraphics, x, startY, Component.translatable(translationKey, value.get()), color);
    }

    private int appendSlotMetadataLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            @Nullable Component value,
            String translationKey,
            int color
    ) {
        return value == null ? startY : appendSlotMetadataLine(guiGraphics, x, startY, Optional.of(value), translationKey, color);
    }

    private int appendTagLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Set<ResourceLocation> tags,
            int color
    ) {
        return appendTagLine(guiGraphics, x, startY, tags, color, Integer.MAX_VALUE);
    }

    private int appendTagLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Set<ResourceLocation> tags,
            int color,
            int maxY
    ) {
        if (tags.isEmpty()) {
            return startY;
        }
        return appendWrappedLine(
                guiGraphics,
                x,
                startY,
                Component.translatable("screen.xlib.ability_tags_stat", Component.literal(joinIds(tags))),
                color,
                maxY
        );
    }

    private int appendWrappedLine(GuiGraphics guiGraphics, int x, int startY, Component line, int color) {
        return appendWrappedLine(guiGraphics, x, startY, line, color, Integer.MAX_VALUE);
    }

    private int appendWrappedLine(GuiGraphics guiGraphics, int x, int startY, Component line, int color, int maxY) {
        int y = startY;
        for (FormattedCharSequence wrappedLine : this.font.split(line, DETAILS_PANEL_WIDTH)) {
            if (!hasDetailRoom(y, maxY)) {
                return y;
            }
            guiGraphics.drawString(this.font, wrappedLine, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private void renderSlotButtonOverlays(GuiGraphics guiGraphics, MenuPalette palette) {
        for (int slot = 0; slot < this.slotButtons.size(); slot++) {
            Button slotButton = this.slotButtons.get(slot);
            if (slot == this.selectedSlot) {
                drawOutline(guiGraphics,
                        slotButton.getX() - 1,
                        slotButton.getY() - 1,
                        slotButton.getWidth() + 2,
                        slotButton.getHeight() + 2,
                        palette.highlightColor());
            }
            if (slotMetadata(slot).softLocked()) {
                guiGraphics.fill(
                        slotButton.getX() + slotButton.getWidth() - 6,
                        slotButton.getY(),
                        slotButton.getX() + slotButton.getWidth(),
                        slotButton.getY() + 6,
                        palette.warningColor()
                );
            }
        }
    }

    private static void drawOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
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

    private void renderPassivePanel(GuiGraphics guiGraphics, MenuPalette palette) {
        int panelX = this.width / 2 - DETAILS_PANEL_X_OFFSET;
        int top = passivePanelTop();
        int bottom = passivePanelBottom();
        int right = panelX + DETAILS_PANEL_WIDTH;
        guiGraphics.fill(panelX - 4, top - 4, right + 4, bottom + 4, withAlpha(palette.secondaryPanelColor(), 0xC8));
        guiGraphics.drawString(this.font, Component.translatable("screen.xlib.passive_panel", this.visiblePassives.size()), panelX, top, palette.titleColor(), false);

        List<PassiveDefinition> displayedPassives = displayedPassives();
        int listY = top + 14;
        for (int index = 0; index < displayedPassives.size(); index++) {
            PassiveDefinition passive = displayedPassives.get(index);
            int rowY = listY + index * PASSIVE_ROW_HEIGHT;
            boolean selected = passive.id().equals(this.selectedPassiveId);
            PassiveState passiveState = passiveState(passive);
            if (selected) {
                guiGraphics.fill(panelX - 2, rowY - 1, right + 2, rowY + PASSIVE_ROW_HEIGHT - 2, withAlpha(palette.highlightColor(), 0x34));
            }
            if (this.minecraft != null) {
                AbilityIconRenderer.render(guiGraphics, this.minecraft, passive.icon(), panelX + 1, rowY + 1, PASSIVE_ICON_SIZE, PASSIVE_ICON_SIZE);
            }
            String stateLabel = passiveState.label().getString();
            int stateWidth = this.font.width(stateLabel);
            int nameWidth = Math.max(18, DETAILS_PANEL_WIDTH - PASSIVE_ICON_SIZE - 10 - stateWidth - 6);
            String name = this.font.plainSubstrByWidth(passive.displayName().getString(), nameWidth);
            guiGraphics.drawString(this.font, name, panelX + PASSIVE_ICON_SIZE + 6, rowY + 4, palette.bodyColor(), false);
            guiGraphics.drawString(this.font, stateLabel, right - stateWidth, rowY + 4, passiveState.color(), false);
        }

        int previewY = top + 14 + PASSIVE_VISIBLE_ROWS * PASSIVE_ROW_HEIGHT + 2;
        PassiveDefinition selectedPassive = selectedPassive();
        Component preview = selectedPassive == null
                ? Component.translatable("screen.xlib.passive_panel.none")
                : selectedPassiveDescription(selectedPassive);
        int previewColor = selectedPassive == null ? palette.bodyColor() : passiveState(selectedPassive).color();
        for (FormattedCharSequence line : this.font.split(preview, DETAILS_PANEL_WIDTH)) {
            guiGraphics.drawString(this.font, line, panelX, previewY, previewColor, false);
            break;
        }

        if (maxPassiveScrollOffset() > 0) {
            renderPassiveScrollBar(guiGraphics, panelX, listY, palette);
        }
    }

    private void renderPassiveScrollBar(GuiGraphics guiGraphics, int panelX, int listY, MenuPalette palette) {
        int trackX = panelX + DETAILS_PANEL_WIDTH + 2;
        int trackHeight = PASSIVE_VISIBLE_ROWS * PASSIVE_ROW_HEIGHT - 2;
        int thumbHeight = Math.max(8, trackHeight * PASSIVE_VISIBLE_ROWS / this.visiblePassives.size());
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = listY + (maxPassiveScrollOffset() == 0 ? 0
                : Math.round((float) this.passiveScrollOffset / maxPassiveScrollOffset() * thumbTravel));
        guiGraphics.fill(trackX, listY, trackX + 3, listY + trackHeight, palette.scrollbarTrackColor());
        guiGraphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, palette.scrollbarThumbColor());
    }

    private List<PassiveDefinition> displayedPassives() {
        int fromIndex = Math.min(this.passiveScrollOffset, this.visiblePassives.size());
        int toIndex = Math.min(this.visiblePassives.size(), fromIndex + PASSIVE_VISIBLE_ROWS);
        return this.visiblePassives.subList(fromIndex, toIndex);
    }

    private int passiveIndexAt(double mouseX, double mouseY) {
        if (!isMouseOverPassiveRows(mouseX, mouseY)) {
            return -1;
        }
        int row = (int) ((mouseY - (passivePanelTop() + 14)) / PASSIVE_ROW_HEIGHT);
        if (row < 0 || row >= PASSIVE_VISIBLE_ROWS) {
            return -1;
        }
        int index = this.passiveScrollOffset + row;
        return index < this.visiblePassives.size() ? index : -1;
    }

    private boolean isMouseOverPassivePanel(double mouseX, double mouseY) {
        int left = this.width / 2 - DETAILS_PANEL_X_OFFSET - 4;
        int right = left + DETAILS_PANEL_WIDTH + 8;
        int top = passivePanelTop() - 4;
        int bottom = passivePanelBottom() + 4;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private boolean isMouseOverPassiveRows(double mouseX, double mouseY) {
        int left = this.width / 2 - DETAILS_PANEL_X_OFFSET - 4;
        int right = left + DETAILS_PANEL_WIDTH + 8;
        int top = passivePanelTop() + 12;
        int bottom = top + PASSIVE_VISIBLE_ROWS * PASSIVE_ROW_HEIGHT;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private @Nullable PassiveDefinition selectedPassive() {
        if (this.selectedPassiveId == null) {
            return null;
        }
        return this.visiblePassives.stream()
                .filter(passive -> passive.id().equals(this.selectedPassiveId))
                .findFirst()
                .orElse(null);
    }

    private Component selectedPassiveDescription(PassiveDefinition passive) {
        String descriptionKey = passive.translationKey() + ".desc";
        return Language.getInstance().has(descriptionKey)
                ? passive.description()
                : Component.translatable("screen.xlib.passive_panel.no_description", passive.displayName());
    }

    private PassiveState passiveState(PassiveDefinition passive) {
        Player player = currentPlayer();
        MenuPalette palette = currentPresentation().palette();
        if (player == null || passive.firstFailedActiveRequirement(player, currentData()).isPresent()) {
            return new PassiveState(Component.translatable("screen.xlib.passive_state.paused"), palette.warningColor());
        }
        return new PassiveState(Component.translatable("screen.xlib.passive_state.active"), palette.successColor());
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

    private void cyclePageScope() {
        refreshVisibleState();
        this.selectedPageId = nextCatalogSelection(this.selectedPageId, AbilityMenuCatalog.availablePages(this.catalogAbilities));
        this.selectedGroupId = null;
        this.selectedFamilyId = null;
        refreshVisibleState();
        updateAbilityButtons();
    }

    private void cycleGroupScope() {
        refreshVisibleState();
        this.selectedGroupId = nextCatalogSelection(
                this.selectedGroupId,
                AbilityMenuCatalog.availableGroups(this.catalogAbilities, this.selectedPageId)
        );
        this.selectedFamilyId = null;
        refreshVisibleState();
        updateAbilityButtons();
    }

    private void cycleFamilyScope() {
        refreshVisibleState();
        this.selectedFamilyId = nextCatalogSelection(
                this.selectedFamilyId,
                AbilityMenuCatalog.availableFamilies(this.catalogAbilities, this.selectedPageId, this.selectedGroupId)
        );
        refreshVisibleState();
        updateAbilityButtons();
    }

    private @Nullable ResourceLocation nextCatalogSelection(
            @Nullable ResourceLocation currentSelection,
            List<ResourceLocation> options
    ) {
        if (options.isEmpty()) {
            return null;
        }
        if (currentSelection == null) {
            return options.getFirst();
        }
        int currentIndex = options.indexOf(currentSelection);
        if (currentIndex < 0 || currentIndex + 1 >= options.size()) {
            return null;
        }
        return options.get(currentIndex + 1);
    }

    private void updateFilterAndSortButtons() {
        if (this.filterButton != null) {
            this.filterButton.setMessage(this.abilityFilter.label());
        }
        if (this.sortButton != null) {
            this.sortButton.setMessage(this.abilitySort.label());
        }
    }

    private void updateScopeButtons() {
        if (this.pageButton != null) {
            this.pageButton.setMessage(scopeButtonLabel(
                    "screen.xlib.ability_menu.scope_page_all",
                    "screen.xlib.ability_menu.scope_page",
                    this.selectedPageId
            ));
        }
        if (this.groupButton != null) {
            this.groupButton.setMessage(scopeButtonLabel(
                    "screen.xlib.ability_menu.scope_group_all",
                    "screen.xlib.ability_menu.scope_group",
                    this.selectedGroupId
            ));
        }
        if (this.familyButton != null) {
            this.familyButton.setMessage(scopeButtonLabel(
                    "screen.xlib.ability_menu.scope_family_all",
                    "screen.xlib.ability_menu.scope_family",
                    this.selectedFamilyId
            ));
        }
    }

    private Component scopeButtonLabel(String allTranslationKey, String selectedTranslationKey, @Nullable ResourceLocation selectedId) {
        return selectedId == null
                ? Component.translatable(allTranslationKey)
                : Component.translatable(selectedTranslationKey, Component.literal(displayMetadataLabel(selectedId)));
    }

    private String normalizedSearchQuery() {
        return this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesSearch(AbilityDefinition ability, String query) {
        if (query.isEmpty()) {
            return true;
        }
        String name = ability.displayName().getString().toLowerCase(Locale.ROOT);
        if (name.contains(query) || ability.id().toString().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        for (ResourceLocation metadataId : ability.metadataIds()) {
            if (metadataId.toString().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private Set<ResourceLocation> assignedAbilityIdsForCurrentTarget(AbilityData data, @org.jetbrains.annotations.Nullable ResourceLocation modeId) {
        Set<ResourceLocation> assignedIds = new LinkedHashSet<>();
        int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, currentContainerId()));
        for (int slot = 0; slot < slotCount; slot++) {
            AbilityLoadoutApi.assignedAbilityId(data, modeId, slotReference(slot)).ifPresent(assignedIds::add);
        }
        return Set.copyOf(assignedIds);
    }

    private Comparator<AbilityDefinition> abilityComparator(AbilityData data, @org.jetbrains.annotations.Nullable ResourceLocation modeId) {
        Comparator<AbilityDefinition> byName = Comparator.comparing((AbilityDefinition ability) -> ability.displayName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(ability -> ability.id().toString());
        return switch (this.abilitySort) {
            case CATALOG -> AbilityMenuCatalog.catalogComparator();
            case NAME -> byName;
            case ASSIGNED -> Comparator.comparingInt((AbilityDefinition ability) -> assignedSlotIndex(data, modeId, ability.id()))
                    .thenComparing(byName);
            case COOLDOWN -> Comparator.comparingInt(AbilityDefinition::cooldownTicks).thenComparing(byName);
        };
    }

    private int assignedSlotIndex(AbilityData data, @org.jetbrains.annotations.Nullable ResourceLocation modeId, ResourceLocation abilityId) {
        int slotCount = Math.max(1, AbilitySlotContainerApi.resolvedSlotsPerPage(data, currentContainerId()));
        for (int slot = 0; slot < slotCount; slot++) {
            if (AbilityLoadoutApi.assignedAbilityId(data, modeId, slotReference(slot)).filter(abilityId::equals).isPresent()) {
                return slot;
            }
        }
        return slotCount;
    }

    private static String joinIds(Collection<ResourceLocation> ids) {
        return ids.stream()
                .map(AbilityMenuScreen::displayMetadataLabel)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private int drawWrappedText(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            int width,
            Component text,
            int color
    ) {
        int y = startY;
        for (FormattedCharSequence line : this.font.split(text, width)) {
            guiGraphics.drawString(this.font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int drawCenteredWrappedText(
            GuiGraphics guiGraphics,
            int centerX,
            int startY,
            int width,
            Component text,
            int color
    ) {
        int y = startY;
        for (FormattedCharSequence line : this.font.split(text, width)) {
            int lineWidth = this.font.width(line);
            guiGraphics.drawString(this.font, line, centerX - lineWidth / 2, y, color, false);
            y += 10;
        }
        return y;
    }

    private static String displayMetadataLabel(ResourceLocation metadataId) {
        String path = metadataId.getPath();
        int slashIndex = path.indexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < path.length()) {
            path = path.substring(slashIndex + 1);
        }
        return humanize(path);
    }

    private static String humanize(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        boolean capitalizeNext = true;
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (character == '_' || character == '/' || character == '-') {
                builder.append(' ');
                capitalizeNext = true;
                continue;
            }
            builder.append(capitalizeNext ? Character.toUpperCase(character) : character);
            capitalizeNext = false;
        }
        return builder.toString();
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
        CATALOG("screen.xlib.ability_menu.sort.catalog"),
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

    private record PassiveState(Component label, int color) {}
}

