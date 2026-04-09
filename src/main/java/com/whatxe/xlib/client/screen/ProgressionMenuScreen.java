package com.whatxe.xlib.client.screen;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.AbilityIcon;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.ability.PassiveDefinition;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.client.AbilityIconRenderer;
import com.whatxe.xlib.client.AbilityMenuScreenFactoryApi;
import com.whatxe.xlib.client.AbilityMenuScreenContext;
import com.whatxe.xlib.client.ProgressionMenuScreenContext;
import com.whatxe.xlib.client.ProgressionMenuSessionState;
import com.whatxe.xlib.client.ProgressionMenuSessionStateApi;
import com.whatxe.xlib.menu.AbilityMenuAccessApi;
import com.whatxe.xlib.menu.MenuAccessDecision;
import com.whatxe.xlib.menu.ProgressionMenuAccessApi;
import com.whatxe.xlib.network.UnlockUpgradeNodePayload;
import com.whatxe.xlib.presentation.MenuPalette;
import com.whatxe.xlib.presentation.ProgressionLayoutPlanner;
import com.whatxe.xlib.presentation.ProgressionMenuPresentation;
import com.whatxe.xlib.presentation.ProgressionMenuPresentationApi;
import com.whatxe.xlib.presentation.ProgressionNodeLayoutMode;
import com.whatxe.xlib.presentation.ProgressionTreeLayout;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import com.whatxe.xlib.progression.UpgradePointType;
import com.whatxe.xlib.progression.UpgradeProgressData;
import com.whatxe.xlib.progression.UpgradeRequirement;
import com.whatxe.xlib.progression.UpgradeRequirements;
import com.whatxe.xlib.progression.UpgradeTrackDefinition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

public class ProgressionMenuScreen extends Screen {
    private static final int NODE_BUTTON_WIDTH = 182;
    private static final int NODE_BUTTON_SPACING = 24;
    private static final int MIN_VISIBLE_NODE_BUTTONS = 4;
    private static final int MAX_VISIBLE_NODE_BUTTONS = 9;
    private static final int MIN_VISIBLE_CANVAS_ROWS = 5;
    private static final int CANVAS_CONNECTION_THICKNESS = 3;
    private static final int NODE_LIST_Y = 128;
    private static final int NODE_LIST_X_OFFSET = 186;
    private static final int DETAILS_PANEL_X_OFFSET = 6;
    private static final int DETAILS_PANEL_WIDTH = 202;
    private static final int TRACK_NAV_Y = 46;
    private static final int TRACK_LABEL_Y = 50;
    private static final int TRACK_BUTTON_SIZE = 20;
    private static final int TRACK_BUTTON_GAP = 10;
    private static final int SUMMARY_Y = 76;
    private static final int ACTION_BUTTON_Y_OFFSET = 34;
    private static final int LAYOUT_BUTTON_WIDTH = 98;

    private final List<Button> nodeButtons = new ArrayList<>();
    private List<TrackView> trackViews = List.of();
    private ProgressionLayoutPlanner.LayoutPlan nodeLayoutPlan = ProgressionLayoutPlanner.LayoutPlan.empty();
    private List<ProgressionLayoutPlanner.NodePlacement> visibleNodes = List.of();
    private @Nullable ResourceLocation selectedNodeId;
    private int selectedTrackIndex;
    private int nodeScrollOffset;
    private int visibleNodeButtonCount = MIN_VISIBLE_NODE_BUTTONS;
    private int nodeButtonX;
    private Button previousTrackButton;
    private Button nextTrackButton;
    private Button layoutModeButton;
    private Button unlockButton;
    private Button openAbilityMenuButton;
    private MenuAccessDecision currentMenuAccess = MenuAccessDecision.available();
    private MenuAccessDecision currentAbilityMenuAccess = MenuAccessDecision.available();
    private ProgressionNodeLayoutMode layoutMode = ProgressionNodeLayoutMode.TREE;
    private @Nullable ResourceLocation pendingTrackId;
    private @Nullable ResourceLocation pendingNodeId;

    public ProgressionMenuScreen() {
        this(ProgressionMenuScreenContext.defaultContext());
    }

    public ProgressionMenuScreen(ProgressionMenuScreenContext context) {
        super(Component.translatable("screen.xlib.progression_menu"));
        ProgressionMenuSessionState sessionState = context.sessionState();
        this.pendingTrackId = sessionState.selectedTrackId();
        this.pendingNodeId = sessionState.selectedNodeId();
        this.layoutMode = sessionState.layoutMode();
    }

    @Override
    protected void init() {
        buildWidgets();
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
        refreshTrackViews();
        refreshVisibleNodes();
        updateTrackButtons();
        updateNodeButtons();
        updateActionButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ProgressionMenuPresentation presentation = currentPresentation();
        MenuPalette palette = presentation.palette();
        guiGraphics.fillGradient(0, 0, this.width, this.height, palette.backgroundTopColor(), palette.backgroundBottomColor());

        int listPanelLeft = nodePanelLeft();
        int listPanelTop = NODE_LIST_Y - 18;
        int listPanelBottom = nodeListBottom() + 6;
        int detailsX = this.width / 2 + DETAILS_PANEL_X_OFFSET;
        int detailsPanelTop = TRACK_NAV_Y;
        int detailsPanelBottom = this.height - 26;

        if (isCanvasLayout()) {
            guiGraphics.fill(listPanelLeft, listPanelTop, nodePanelRight(), listPanelBottom, palette.panelColor());
            guiGraphics.fill(listPanelLeft + 6, listPanelTop + 6, nodePanelRight() - 6, listPanelBottom - 6, palette.secondaryPanelColor());
        } else {
            guiGraphics.fill(listPanelLeft, listPanelTop, nodePanelRight(), listPanelBottom, palette.secondaryPanelColor());
        }
        guiGraphics.fill(detailsX - 8, detailsPanelTop - 8, detailsX + DETAILS_PANEL_WIDTH + 8, detailsPanelBottom, palette.panelColor());
        if (isCanvasLayout()) {
            renderCanvasConnections(guiGraphics, palette);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (isCanvasLayout()) {
            renderCanvasNodes(guiGraphics, mouseX, mouseY, palette);
        }

        guiGraphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 16, palette.titleColor(), false);
        int infoY = drawWrappedLine(
                guiGraphics,
                this.width / 2 - 198,
                30,
                396,
                palette.bodyColor(),
                Component.translatable("screen.xlib.progression_menu.instructions")
        );
        if (this.currentMenuAccess.isLocked()) {
            drawWrappedLine(
                    guiGraphics,
                    this.width / 2 - 198,
                    infoY + 2,
                    396,
                    palette.warningColor(),
                    Component.translatable(
                            "screen.xlib.menu_locked",
                            this.currentMenuAccess.reason().orElse(Component.translatable("screen.xlib.progression_menu.locked"))
                    )
            );
        }
        guiGraphics.drawString(this.font, Component.translatable("screen.xlib.progression_menu.nodes"), nodeListLabelX(), NODE_LIST_Y - 14, palette.titleColor(), false);

        renderTrackHeader(guiGraphics, palette);

        int summaryY = SUMMARY_Y;
        summaryY = drawWrappedLine(
                guiGraphics,
                detailsX,
                summaryY,
                DETAILS_PANEL_WIDTH,
                palette.emphasisColor(),
                Component.translatable("screen.xlib.progression_menu.points", pointSummary())
        );
        summaryY = drawWrappedLine(
                guiGraphics,
                detailsX,
                summaryY + 2,
                DETAILS_PANEL_WIDTH,
                palette.infoColor(),
                Component.translatable("screen.xlib.progression_menu.counters", counterSummary())
        );
        List<Component> pointSources = presentation.showPointSourceHints() ? pointSourceSummary() : List.of();
        if (presentation.showPointSourceHints() && !pointSources.isEmpty()) {
            summaryY = drawSectionHeader(guiGraphics, detailsX, summaryY + 8, "screen.xlib.progression_menu.point_sources");
            for (Component sourceLine : pointSources) {
                summaryY = drawWrappedLine(guiGraphics, detailsX, summaryY, DETAILS_PANEL_WIDTH, palette.requirementColor(), sourceLine);
            }
        }
        if (presentation.showTrackMetadata()) {
            summaryY = drawTrackMetadata(detailsX, summaryY + 8, guiGraphics);
        }

        Optional<ProgressionLayoutPlanner.NodePlacement> maybeSelectedNode = selectedNodeEntry();
        if (maybeSelectedNode.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.xlib.progression_menu.no_nodes"),
                    detailsX,
                    summaryY + 16,
                    palette.bodyColor(),
                    false
            );
            return;
        }

        UpgradeProgressData data = currentData();
        UpgradeNodeDefinition node = maybeSelectedNode.get().node();
        NodeState nodeState = nodeState(node, data);

        int detailY = summaryY + 18;
        guiGraphics.drawString(this.font, node.displayName(), detailsX, detailY, palette.titleColor(), false);
        detailY += 12;
        guiGraphics.drawString(this.font, nodeState.label(), detailsX, detailY, nodeState.color(), false);
        detailY += 16;
        detailY = drawOptionalMetadataLine(guiGraphics, detailsX, detailY, node.choiceGroupId(),
                "screen.xlib.progression_menu.choice_group", palette.infoColor());
        detailY = drawResourceListLine(guiGraphics, detailsX, detailY, node.lockedNodes(),
                "screen.xlib.progression_menu.locks_nodes", palette.warningColor(), this::displayNodeName);
        detailY = drawResourceListLine(guiGraphics, detailsX, detailY, node.lockedTracks(),
                "screen.xlib.progression_menu.locks_tracks", palette.warningColor(), UpgradeApi::displayTrackName);

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY, "screen.xlib.progression_menu.costs");
        if (node.pointCosts().isEmpty()) {
            detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, palette.successColor(),
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            boolean unlocked = data.hasUnlockedNode(node.id());
            for (Map.Entry<ResourceLocation, Integer> entry : sortedEntries(node.pointCosts())) {
                int currentPoints = data.points(entry.getKey());
                int color = unlocked || currentPoints >= entry.getValue() ? palette.successColor() : palette.warningColor();
                Component costLine = unlocked
                        ? Component.translatable("screen.xlib.progression_menu.cost_paid", entry.getValue(), displayPointName(entry.getKey()))
                        : Component.literal(currentPoints + "/" + entry.getValue() + " ").append(displayPointName(entry.getKey()));
                detailY = drawWrappedLine(
                        guiGraphics,
                        detailsX,
                        detailY,
                        DETAILS_PANEL_WIDTH,
                        color,
                        Component.literal("- ").append(costLine)
                );
            }
        }

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY + 4, "screen.xlib.progression_menu.prerequisites");
        if (node.requiredNodes().isEmpty()) {
            detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, palette.successColor(),
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            for (ResourceLocation requiredNodeId : sortedIds(node.requiredNodes())) {
                boolean satisfied = data.hasUnlockedNode(requiredNodeId);
                detailY = drawWrappedLine(
                        guiGraphics,
                        detailsX,
                        detailY,
                        DETAILS_PANEL_WIDTH,
                        satisfied ? palette.successColor() : palette.warningColor(),
                        Component.literal("- ").append(displayNodeName(requiredNodeId))
                );
            }
        }

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY + 4, "screen.xlib.progression_menu.conditions");
        if (node.requirements().isEmpty()) {
            detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, 0xA9E6B0,
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            for (UpgradeRequirement requirement : node.requirements()) {
                detailY = drawWrappedLine(
                        guiGraphics,
                        detailsX,
                        detailY,
                        DETAILS_PANEL_WIDTH,
                        palette.requirementColor(),
                        Component.literal("- ").append(requirement.description())
                );
            }
        }

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY + 4, "screen.xlib.progression_menu.rewards");
        List<Component> rewards = rewardSummary(node);
        if (rewards.isEmpty()) {
            detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, palette.bodyColor(),
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            for (Component rewardLine : rewards) {
                detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, palette.rewardColor(), rewardLine);
            }
        }

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY + 4, "screen.xlib.progression_menu.reward_details");
        List<Component> rewardDescriptions = rewardDescriptionSummary(node);
        if (rewardDescriptions.isEmpty()) {
            drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, palette.bodyColor(),
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            for (Component rewardDescription : rewardDescriptions) {
                detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, palette.bodyColor(), rewardDescription);
            }
        }

        if (maxNodeScrollOffset() > 0) {
            renderScrollBar(guiGraphics);
        }
        if (isCanvasLayout()) {
            renderCanvasHoverCard(guiGraphics, mouseX, mouseY, palette);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverNodeList(mouseX, mouseY) && maxNodeScrollOffset() > 0) {
            scrollNodeList(scrollY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isCanvasLayout()) {
            ProgressionLayoutPlanner.NodePlacement hoveredNode = hoveredCanvasNode(mouseX, mouseY);
            if (hoveredNode != null) {
                selectNode(hoveredNode.node().id());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void buildWidgets() {
        this.clearWidgets();
        this.nodeButtons.clear();
        synchronizeLayoutMode();
        refreshAccessState();
        refreshTrackViews();
        refreshVisibleNodes();

        int previousTrackX = this.width / 2 - 60;
        this.previousTrackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), pressed -> cycleTrack(-1))
                .bounds(previousTrackX, TRACK_NAV_Y, TRACK_BUTTON_SIZE, TRACK_BUTTON_SIZE)
                .build());
        this.nextTrackButton = this.addRenderableWidget(Button.builder(Component.literal(">"), pressed -> cycleTrack(1))
                .bounds(previousTrackX + 120, TRACK_NAV_Y, TRACK_BUTTON_SIZE, TRACK_BUTTON_SIZE)
                .build());

        this.nodeButtonX = this.width / 2 - NODE_LIST_X_OFFSET;
        this.layoutModeButton = this.addRenderableWidget(Button.builder(Component.empty(), pressed -> cycleLayoutMode())
                .bounds(nodePanelLeft(), TRACK_NAV_Y, LAYOUT_BUTTON_WIDTH, 20)
                .build());

        this.visibleNodeButtonCount = computeVisibleNodeButtonCount();
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset, 0, maxNodeScrollOffset());
        if (!isCanvasLayout()) {
            for (int index = 0; index < this.visibleNodeButtonCount; index++) {
                final int visibleIndex = index;
                Button nodeButton = Button.builder(Component.empty(), pressed -> selectVisibleNode(visibleIndex))
                        .bounds(this.nodeButtonX, NODE_LIST_Y + index * NODE_BUTTON_SPACING, currentNodeButtonWidth(), 20)
                        .build();
                this.nodeButtons.add(this.addRenderableWidget(nodeButton));
            }
        }

        int actionButtonY = this.height - ACTION_BUTTON_Y_OFFSET;
        this.unlockButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.xlib.progression_menu.unlock"),
                        pressed -> unlockSelectedNode())
                .bounds(this.width / 2 + DETAILS_PANEL_X_OFFSET, actionButtonY, 98, 20)
                .build());
        this.openAbilityMenuButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.xlib.progression_menu.ability_menu"),
                        pressed -> openAbilityMenu())
                .bounds(this.width / 2 + DETAILS_PANEL_X_OFFSET + 104, actionButtonY, 98, 20)
                .build());

        updateTrackButtons();
        updateNodeButtons();
        updateActionButtons();
    }

    private void refreshTrackViews() {
        UpgradeProgressData data = currentData();
        List<TrackView> updatedViews = new ArrayList<>();
        updatedViews.add(new TrackView(null, Component.translatable("screen.xlib.progression_menu.all_tracks")));
        for (UpgradeTrackDefinition track : UpgradeApi.visibleTracks(data)) {
            updatedViews.add(new TrackView(track.id(), track.displayName()));
        }
        this.trackViews = List.copyOf(updatedViews);
        if (this.pendingTrackId != null) {
            int requestedIndex = indexOfTrack(this.pendingTrackId);
            this.selectedTrackIndex = requestedIndex >= 0 ? requestedIndex : 0;
            this.pendingTrackId = null;
        }
        this.selectedTrackIndex = Mth.clamp(this.selectedTrackIndex, 0, Math.max(0, this.trackViews.size() - 1));
        syncSessionState();
    }

    private void refreshVisibleNodes() {
        synchronizeLayoutMode();
        ResourceLocation selectedTrackId = currentTrackView().trackId();
        Set<ResourceLocation> visibleTrackIds = UpgradeApi.visibleTracks(currentData()).stream()
                .map(UpgradeTrackDefinition::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<ResourceLocation, UpgradeNodeDefinition> filteredNodes = new LinkedHashMap<>();
        for (UpgradeNodeDefinition node : UpgradeApi.allNodes()) {
            if (node.trackId() != null && !visibleTrackIds.contains(node.trackId())) {
                continue;
            }
            if (selectedTrackId == null || selectedTrackId.equals(node.trackId())) {
                filteredNodes.put(node.id(), node);
            }
        }

        List<ResourceLocation> preferredRoots = selectedTrackId == null
                ? List.of()
                : UpgradeApi.findTrack(selectedTrackId)
                        .map(track -> track.rootNodes().stream().filter(filteredNodes::containsKey).toList())
                        .orElse(List.of());
        this.nodeLayoutPlan = ProgressionLayoutPlanner.plan(this.layoutMode, filteredNodes.values(), preferredRoots);
        this.visibleNodes = List.copyOf(this.nodeLayoutPlan.placements());
        if (this.pendingNodeId != null && this.visibleNodes.stream().anyMatch(entry -> entry.node().id().equals(this.pendingNodeId))) {
            this.selectedNodeId = this.pendingNodeId;
        }
        this.pendingNodeId = null;
        if (this.selectedNodeId == null || this.visibleNodes.stream().noneMatch(entry -> entry.node().id().equals(this.selectedNodeId))) {
            this.selectedNodeId = this.visibleNodes.isEmpty() ? null : this.visibleNodes.getFirst().node().id();
        }
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset, 0, maxNodeScrollOffset());
        syncSessionState();
    }

    private void updateTrackButtons() {
        boolean hasMultipleTracks = this.trackViews.size() > 1;
        if (this.previousTrackButton != null) {
            this.previousTrackButton.active = hasMultipleTracks;
        }
        if (this.nextTrackButton != null) {
            this.nextTrackButton.active = hasMultipleTracks;
        }
        if (this.layoutModeButton != null) {
            boolean hasMultipleLayoutModes = currentPresentation().availableLayoutModes().size() > 1;
            this.layoutModeButton.visible = hasMultipleLayoutModes;
            this.layoutModeButton.active = hasMultipleLayoutModes;
            this.layoutModeButton.setMessage(Component.translatable("screen.xlib.progression_menu.layout", this.layoutMode.label()));
        }
        layoutTrackButtons();
    }

    private void layoutTrackButtons() {
        if (this.previousTrackButton == null || this.nextTrackButton == null) {
            return;
        }

        int centerX = this.width / 2 + 6;
        int titleWidth = this.font.width(currentTrackLine());
        int previousX = centerX - titleWidth / 2 - TRACK_BUTTON_GAP - TRACK_BUTTON_SIZE;
        int nextX = centerX + (titleWidth + 1) / 2 + TRACK_BUTTON_GAP;
        int layoutLaneWidth = this.layoutModeButton != null && this.layoutModeButton.visible ? LAYOUT_BUTTON_WIDTH + 12 : 12;
        int minPreviousX = nodePanelLeft() + layoutLaneWidth;
        int maxNextX = this.width - TRACK_BUTTON_SIZE - 18;

        if (previousX < minPreviousX) {
            int shift = minPreviousX - previousX;
            previousX += shift;
            nextX += shift;
        }
        if (nextX > maxNextX) {
            int shift = nextX - maxNextX;
            previousX -= shift;
            nextX -= shift;
        }
        if (previousX < minPreviousX) {
            previousX = minPreviousX;
        }
        if (nextX > maxNextX) {
            nextX = maxNextX;
        }

        this.previousTrackButton.setX(previousX);
        this.previousTrackButton.setY(TRACK_NAV_Y);
        this.nextTrackButton.setX(nextX);
        this.nextTrackButton.setY(TRACK_NAV_Y);
    }

    private void renderTrackHeader(GuiGraphics guiGraphics, MenuPalette palette) {
        int left = trackLabelLeft();
        int right = trackLabelRight();
        int maxWidth = Math.max(72, right - left);
        Component trackLine = clippedTrackLine(maxWidth);
        guiGraphics.drawCenteredString(this.font, trackLine, (left + right) / 2, TRACK_LABEL_Y, palette.titleColor());
    }

    private int trackLabelLeft() {
        if (this.previousTrackButton == null) {
            return this.width / 2 - 120;
        }
        return this.previousTrackButton.getX() + TRACK_BUTTON_SIZE + TRACK_BUTTON_GAP;
    }

    private int trackLabelRight() {
        if (this.nextTrackButton == null) {
            return this.width / 2 + 120;
        }
        return this.nextTrackButton.getX() - TRACK_BUTTON_GAP;
    }

    private Component currentTrackLine() {
        return Component.translatable("screen.xlib.progression_menu.track", currentTrackView().title());
    }

    private Component clippedTrackLine(int maxWidth) {
        String label = currentTrackLine().getString();
        if (this.font.width(label) <= maxWidth) {
            return Component.literal(label);
        }
        int ellipsisWidth = this.font.width("...");
        String clipped = this.font.plainSubstrByWidth(label, Math.max(12, maxWidth - ellipsisWidth));
        return Component.literal(clipped + "...");
    }

    private void updateNodeButtons() {
        if (isCanvasLayout()) {
            for (Button button : this.nodeButtons) {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.empty());
            }
            return;
        }

        ensureNodeButtonCapacity();
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset, 0, maxNodeScrollOffset());
        List<ProgressionLayoutPlanner.NodePlacement> displayedNodes = displayedNodes();
        for (int visibleIndex = 0; visibleIndex < this.nodeButtons.size(); visibleIndex++) {
            Button button = this.nodeButtons.get(visibleIndex);
            if (visibleIndex >= displayedNodes.size()) {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.empty());
                continue;
            }

            ProgressionLayoutPlanner.NodePlacement entry = displayedNodes.get(visibleIndex);
            NodeState nodeState = nodeState(entry.node(), currentData());
            boolean selected = this.selectedNodeId != null && this.selectedNodeId.equals(entry.node().id());
            button.visible = true;
            button.active = true;
            button.setX(nodeButtonX(entry));
            button.setY(nodeButtonY(entry));
            button.setWidth(currentNodeButtonWidth());
            button.setMessage(nodeButtonLabel(entry, nodeState, selected));
        }
    }

    private void updateActionButtons() {
        Optional<ProgressionLayoutPlanner.NodePlacement> maybeSelectedNode = selectedNodeEntry();
        if (this.unlockButton == null) {
            return;
        }
        if (this.openAbilityMenuButton != null) {
            this.openAbilityMenuButton.active = !this.currentAbilityMenuAccess.isHidden();
        }

        if (maybeSelectedNode.isEmpty()) {
            this.unlockButton.active = false;
            return;
        }

        UpgradeNodeDefinition node = maybeSelectedNode.get().node();
        UpgradeProgressData data = currentData();
        this.unlockButton.active = this.currentMenuAccess.isAvailable()
                && UpgradeApi.firstStructuralUnlockFailure(data, node).isEmpty();
    }

    private void cycleTrack(int delta) {
        if (this.trackViews.size() <= 1) {
            return;
        }
        this.selectedTrackIndex = Mth.positiveModulo(this.selectedTrackIndex + delta, this.trackViews.size());
        this.nodeScrollOffset = 0;
        refreshVisibleNodes();
        updateNodeButtons();
        updateActionButtons();
        syncSessionState();
    }

    private void selectVisibleNode(int visibleIndex) {
        List<ProgressionLayoutPlanner.NodePlacement> displayedNodes = displayedNodes();
        if (visibleIndex >= 0 && visibleIndex < displayedNodes.size()) {
            selectNode(displayedNodes.get(visibleIndex).node().id());
        }
    }

    private void selectNode(ResourceLocation nodeId) {
        this.selectedNodeId = nodeId;
        updateNodeButtons();
        updateActionButtons();
        syncSessionState();
    }

    private void unlockSelectedNode() {
        if (!this.currentMenuAccess.isAvailable()) {
            return;
        }
        selectedNodeEntry().ifPresent(entry -> PacketDistributor.sendToServer(new UnlockUpgradeNodePayload(entry.node().id())));
    }

    private void openAbilityMenu() {
        if (this.minecraft != null && !this.currentAbilityMenuAccess.isHidden()) {
            AbilityMenuScreenFactoryApi.openActive(this.minecraft, AbilityMenuScreenContext.fromCurrentState(this));
        }
    }

    private void scrollNodeList(int delta) {
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset + delta, 0, maxNodeScrollOffset());
        updateNodeButtons();
    }

    private int computeVisibleNodeButtonCount() {
        int availableHeight = Math.max(96, this.height - NODE_LIST_Y - 60);
        int visibleCount = (availableHeight + 4) / NODE_BUTTON_SPACING;
        return Mth.clamp(visibleCount, MIN_VISIBLE_NODE_BUTTONS, MAX_VISIBLE_NODE_BUTTONS);
    }

    private int maxNodeScrollOffset() {
        if (isCanvasLayout()) {
            return Math.max(0, this.nodeLayoutPlan.rowCount() - canvasVisibleRows());
        }
        return Math.max(0, this.visibleNodes.size() - this.visibleNodeButtonCount);
    }

    private int nodeListBottom() {
        if (isCanvasLayout()) {
            return canvasTop() + canvasVisibleRows() * canvasRowSpacing() - 10;
        }
        return NODE_LIST_Y + this.visibleNodeButtonCount * NODE_BUTTON_SPACING - 4;
    }

    private boolean isMouseOverNodeList(double mouseX, double mouseY) {
        int left = nodePanelLeft();
        int top = NODE_LIST_Y - 18;
        int right = nodePanelRight();
        int bottom = nodeListBottom() + 6;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        MenuPalette palette = currentPresentation().palette();
        int trackX = nodePanelRight() + 6;
        int trackY = NODE_LIST_Y;
        int visibleSpan = isCanvasLayout() ? canvasVisibleRows() : this.visibleNodeButtonCount;
        int trackHeight = isCanvasLayout()
                ? canvasVisibleRows() * canvasRowSpacing() - 8
                : this.visibleNodeButtonCount * NODE_BUTTON_SPACING - 4;
        int maxOffset = maxNodeScrollOffset();
        int totalSpan = isCanvasLayout() ? Math.max(1, this.nodeLayoutPlan.rowCount()) : Math.max(1, this.visibleNodes.size());
        int thumbHeight = Math.max(12, trackHeight * visibleSpan / totalSpan);
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (maxOffset == 0 ? 0 : Math.round((float) this.nodeScrollOffset / maxOffset * thumbTravel));

        guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackHeight, palette.scrollbarTrackColor());
        guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, palette.scrollbarThumbColor());
    }

    private TrackView currentTrackView() {
        return this.trackViews.isEmpty()
                ? new TrackView(null, Component.translatable("screen.xlib.progression_menu.all_tracks"))
                : this.trackViews.get(this.selectedTrackIndex);
    }

    private Optional<ProgressionLayoutPlanner.NodePlacement> selectedNodeEntry() {
        if (this.selectedNodeId == null) {
            return Optional.empty();
        }
        return this.visibleNodes.stream().filter(entry -> entry.node().id().equals(this.selectedNodeId)).findFirst();
    }

    private UpgradeProgressData currentData() {
        Player player = currentPlayer();
        return player == null ? UpgradeProgressData.empty() : ModAttachments.getProgression(player);
    }

    private @Nullable Player currentPlayer() {
        return this.minecraft != null ? this.minecraft.player : null;
    }

    private void refreshAccessState() {
        Player player = currentPlayer();
        this.currentMenuAccess = ProgressionMenuAccessApi.decision(player);
        this.currentAbilityMenuAccess = AbilityMenuAccessApi.decision(player);
    }

    private ProgressionMenuPresentation currentPresentation() {
        return ProgressionMenuPresentationApi.active();
    }

    private void synchronizeLayoutMode() {
        ProgressionMenuPresentation presentation = currentPresentation();
        if (!presentation.availableLayoutModes().contains(this.layoutMode)) {
            this.layoutMode = presentation.defaultLayoutMode();
            syncSessionState();
        }
    }

    private void cycleLayoutMode() {
        ProgressionMenuPresentation presentation = currentPresentation();
        List<ProgressionNodeLayoutMode> modes = List.copyOf(presentation.availableLayoutModes());
        if (modes.size() <= 1) {
            return;
        }

        int currentIndex = modes.indexOf(this.layoutMode);
        int nextIndex = currentIndex < 0 ? 0 : (currentIndex + 1) % modes.size();
        this.layoutMode = modes.get(nextIndex);
        this.nodeScrollOffset = 0;
        syncSessionState();
        buildWidgets();
    }

    private int indexOfTrack(@Nullable ResourceLocation trackId) {
        if (trackId == null) {
            return 0;
        }
        for (int index = 0; index < this.trackViews.size(); index++) {
            if (trackId.equals(this.trackViews.get(index).trackId())) {
                return index;
            }
        }
        return -1;
    }

    private void syncSessionState() {
        ProgressionMenuSessionStateApi.setState(new ProgressionMenuSessionState(
                currentTrackView().trackId(),
                this.selectedNodeId,
                this.layoutMode
        ));
    }

    private boolean isCanvasLayout() {
        return this.layoutMode != ProgressionNodeLayoutMode.LIST;
    }

    private int currentNodeButtonWidth() {
        return NODE_BUTTON_WIDTH;
    }

    private int nodePanelLeft() {
        return isCanvasLayout() ? 16 : this.nodeButtonX - 6;
    }

    private int nodePanelRight() {
        return isCanvasLayout() ? 16 + Math.max(220, this.width / 2 - 34) : this.nodeButtonX + NODE_BUTTON_WIDTH + 18;
    }

    private int nodeListLabelX() {
        return isCanvasLayout() ? nodePanelLeft() + 6 : this.nodeButtonX;
    }

    private int canvasTop() {
        return NODE_LIST_Y;
    }

    private int canvasInnerLeft() {
        return nodePanelLeft() + 16;
    }

    private int canvasInnerRight() {
        return nodePanelRight() - 16;
    }

    private int canvasVisibleRows() {
        int availableHeight = Math.max(128, this.height - canvasTop() - 60);
        return Math.max(MIN_VISIBLE_CANVAS_ROWS, (availableHeight + 8) / canvasRowSpacing());
    }

    private void ensureNodeButtonCapacity() {
        int requiredButtons = this.visibleNodeButtonCount;
        while (this.nodeButtons.size() < requiredButtons) {
            final int visibleIndex = this.nodeButtons.size();
            Button nodeButton = Button.builder(Component.empty(), pressed -> selectVisibleNode(visibleIndex))
                    .bounds(this.nodeButtonX, NODE_LIST_Y, currentNodeButtonWidth(), 20)
                    .build();
            this.nodeButtons.add(this.addRenderableWidget(nodeButton));
        }
    }

    private int canvasRowSpacing() {
        ProgressionTreeLayout layout = currentPresentation().treeLayout();
        int labelBlockHeight = layout.labelGap() + layout.maxLabelLines() * layout.labelLineHeight();
        return Math.max(layout.rowSpacing(), layout.nodeSize() + labelBlockHeight + 10);
    }

    private int canvasNodeSize() {
        return currentPresentation().treeLayout().nodeSize();
    }

    private int canvasColumnSpacing() {
        ProgressionTreeLayout layout = currentPresentation().treeLayout();
        int columns = Math.max(1, this.nodeLayoutPlan.columnCount());
        if (columns <= 1) {
            return 0;
        }
        int usableWidth = Math.max(canvasNodeSize(), canvasInnerRight() - canvasInnerLeft());
        int maxSpacing = Math.max(8, (usableWidth - canvasNodeSize()) / (columns - 1));
        int preferredSpacing = Math.max(layout.columnSpacing(), resolvedTreeLabelWidth() + 12);
        return Math.max(8, Math.min(preferredSpacing, maxSpacing));
    }

    private int canvasGridLeft() {
        int columns = Math.max(1, this.nodeLayoutPlan.columnCount());
        if (columns <= 1) {
            return canvasInnerLeft();
        }
        int gridWidth = canvasNodeSize() + (columns - 1) * canvasColumnSpacing();
        return canvasInnerLeft() + Math.max(0, (canvasInnerRight() - canvasInnerLeft() - gridWidth) / 2);
    }

    private int nodeButtonX(ProgressionLayoutPlanner.NodePlacement entry) {
        if (!isCanvasLayout()) {
            return this.nodeButtonX;
        }
        return canvasNodeX(entry);
    }

    private int nodeButtonY(ProgressionLayoutPlanner.NodePlacement entry) {
        if (!isCanvasLayout()) {
            int index = this.visibleNodes.indexOf(entry);
            return NODE_LIST_Y + Math.max(0, index - this.nodeScrollOffset) * NODE_BUTTON_SPACING;
        }
        return canvasNodeY(entry);
    }

    private int canvasNodeX(ProgressionLayoutPlanner.NodePlacement entry) {
        int columns = Math.max(1, this.nodeLayoutPlan.columnCount());
        if (columns <= 1) {
            return canvasInnerLeft() + Math.max(0, (canvasInnerRight() - canvasInnerLeft() - canvasNodeSize()) / 2);
        }
        int x = canvasGridLeft() + entry.column() * canvasColumnSpacing();
        return Mth.clamp(x, canvasInnerLeft(), canvasInnerRight() - canvasNodeSize());
    }

    private int canvasNodeY(ProgressionLayoutPlanner.NodePlacement entry) {
        return canvasTop() + (entry.row() - this.nodeScrollOffset) * canvasRowSpacing();
    }

    private int canvasNodeCenterX(ProgressionLayoutPlanner.NodePlacement entry) {
        return canvasNodeX(entry) + canvasNodeSize() / 2;
    }

    private int canvasNodeCenterY(ProgressionLayoutPlanner.NodePlacement entry) {
        return canvasNodeY(entry) + canvasNodeSize() / 2;
    }

    private List<ProgressionLayoutPlanner.NodePlacement> displayedNodes() {
        if (!isCanvasLayout()) {
            int fromIndex = Math.min(this.nodeScrollOffset, this.visibleNodes.size());
            int toIndex = Math.min(this.visibleNodes.size(), fromIndex + this.visibleNodeButtonCount);
            return this.visibleNodes.subList(fromIndex, toIndex);
        }

        int minRow = this.nodeScrollOffset;
        int maxRow = minRow + canvasVisibleRows();
        return this.visibleNodes.stream()
                .filter(entry -> entry.row() >= minRow && entry.row() < maxRow)
                .sorted(Comparator.comparingInt(ProgressionLayoutPlanner.NodePlacement::row)
                        .thenComparingInt(ProgressionLayoutPlanner.NodePlacement::column))
                .toList();
    }

    private Component nodeButtonLabel(
            ProgressionLayoutPlanner.NodePlacement entry,
            NodeState nodeState,
            boolean selected
    ) {
        MutableComponent label = Component.literal(selected ? "> " : "");
        label = label.append(entry.node().displayName());
        return switch (currentPresentation().nodeLabelMode()) {
            case NAME_ONLY -> label;
            case NAME_WITH_STATUS -> label.append(Component.literal(" [" + nodeState.shortLabel() + "]"));
            case NAME_WITH_COST -> label.append(Component.literal(" [" + primaryCostSummary(entry.node()) + "]"));
        };
    }

    private String primaryCostSummary(UpgradeNodeDefinition node) {
        int totalCost = node.pointCosts().values().stream().mapToInt(Integer::intValue).sum();
        return totalCost <= 0 ? "-" : Integer.toString(totalCost);
    }

    private List<String> wrappedTreeCanvasNodeLabel(UpgradeNodeDefinition node) {
        int maxWidth = resolvedTreeLabelWidth();
        int maxLines = currentPresentation().treeLayout().maxLabelLines();
        String label = node.displayName().getString().trim();
        if (label.isEmpty()) {
            return List.of();
        }

        String[] words = label.split("\\s+");
        List<String> lines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (this.font.width(candidate) <= maxWidth) {
                currentLine.setLength(0);
                currentLine.append(candidate);
                continue;
            }

            if (lines.size() == maxLines - 1) {
                StringBuilder remaining = new StringBuilder(currentLine);
                if (remaining.length() > 0) {
                    remaining.append(' ');
                }
                remaining.append(word);
                for (int remainingIndex = index + 1; remainingIndex < words.length; remainingIndex++) {
                    remaining.append(' ').append(words[remainingIndex]);
                }
                lines.add(ellipsizeToWidth(remaining.toString(), maxWidth));
                return List.copyOf(lines);
            }

            if (currentLine.length() == 0) {
                lines.add(ellipsizeToWidth(word, maxWidth));
            } else {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        return List.copyOf(lines);
    }

    private String ellipsizeToWidth(String label, int maxWidth) {
        if (this.font.width(label) <= maxWidth) {
            return label;
        }
        int ellipsisWidth = this.font.width("...");
        return this.font.plainSubstrByWidth(label, Math.max(12, maxWidth - ellipsisWidth)) + "...";
    }

    private @Nullable ProgressionLayoutPlanner.NodePlacement hoveredCanvasNode(double mouseX, double mouseY) {
        if (!isCanvasLayout()) {
            return null;
        }
        for (ProgressionLayoutPlanner.NodePlacement entry : displayedNodes()) {
            if (isMouseOverCanvasNode(entry, mouseX, mouseY)) {
                return entry;
            }
        }
        return null;
    }

    private boolean isMouseOverCanvasNode(ProgressionLayoutPlanner.NodePlacement entry, double mouseX, double mouseY) {
        int x = canvasNodeX(entry);
        int y = canvasNodeY(entry);
        int size = canvasNodeSize();
        return mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
    }

    private void renderCanvasConnections(GuiGraphics guiGraphics, MenuPalette palette) {
        Map<ResourceLocation, ProgressionLayoutPlanner.NodePlacement> visibleById = new LinkedHashMap<>();
        for (ProgressionLayoutPlanner.NodePlacement entry : displayedNodes()) {
            visibleById.put(entry.node().id(), entry);
        }

        UpgradeProgressData data = currentData();
        for (ProgressionLayoutPlanner.Edge edge : this.nodeLayoutPlan.edges()) {
            ProgressionLayoutPlanner.NodePlacement from = visibleById.get(edge.fromNodeId());
            ProgressionLayoutPlanner.NodePlacement to = visibleById.get(edge.toNodeId());
            if (from == null || to == null) {
                continue;
            }
            int connectionColor = connectionColor(to.node(), data, palette);
            renderTreeConnection(guiGraphics, from, to, connectionColor);
        }
    }

    private void renderTreeConnection(
            GuiGraphics guiGraphics,
            ProgressionLayoutPlanner.NodePlacement from,
            ProgressionLayoutPlanner.NodePlacement to,
            int color
    ) {
        int fromX = canvasNodeCenterX(from);
        int fromY = canvasNodeCenterY(from);
        int toX = canvasNodeCenterX(to);
        int toY = canvasNodeCenterY(to);
        int branchX = fromX + Math.max(16, (toX - fromX) / 3);
        drawCanvasHorizontalLine(guiGraphics, fromX, branchX, fromY, color);
        drawCanvasVerticalLine(guiGraphics, branchX, fromY, toY, color);
        drawCanvasHorizontalLine(guiGraphics, branchX, toX, toY, color);
    }

    private void drawCanvasHorizontalLine(GuiGraphics guiGraphics, int fromX, int toX, int centerY, int color) {
        int left = Math.min(fromX, toX);
        int right = Math.max(fromX, toX);
        guiGraphics.fill(left, centerY - CANVAS_CONNECTION_THICKNESS / 2, right, centerY + (CANVAS_CONNECTION_THICKNESS + 1) / 2, withAlpha(color, 0xD8));
    }

    private void drawCanvasVerticalLine(GuiGraphics guiGraphics, int centerX, int fromY, int toY, int color) {
        int top = Math.min(fromY, toY);
        int bottom = Math.max(fromY, toY);
        guiGraphics.fill(centerX - CANVAS_CONNECTION_THICKNESS / 2, top, centerX + (CANVAS_CONNECTION_THICKNESS + 1) / 2, bottom, withAlpha(color, 0xD8));
    }

    private void renderCanvasNodes(GuiGraphics guiGraphics, int mouseX, int mouseY, MenuPalette palette) {
        UpgradeProgressData data = currentData();
        ProgressionLayoutPlanner.NodePlacement hoveredNode = hoveredCanvasNode(mouseX, mouseY);
        for (ProgressionLayoutPlanner.NodePlacement entry : displayedNodes()) {
            boolean selected = this.selectedNodeId != null && this.selectedNodeId.equals(entry.node().id());
            boolean hovered = hoveredNode != null && hoveredNode.node().id().equals(entry.node().id());
            renderCanvasNode(guiGraphics, entry, nodeState(entry.node(), data), palette, selected, hovered);
            renderCanvasNodeLabel(guiGraphics, entry, palette, selected);
        }
    }

    private void renderCanvasNode(
            GuiGraphics guiGraphics,
            ProgressionLayoutPlanner.NodePlacement entry,
            NodeState nodeState,
            MenuPalette palette,
            boolean selected,
            boolean hovered
    ) {
        int x = canvasNodeX(entry);
        int y = canvasNodeY(entry);
        int size = canvasNodeSize();
        int stateColor = connectionColor(entry.node(), currentData(), palette);
        int borderColor = withAlpha(stateColor, hovered || selected ? 0xFF : 0xD8);
        int fillColor = withAlpha(scaleColor(stateColor, 0.26f), 0xC8);
        int innerColor = withAlpha(scaleColor(stateColor, 0.40f), 0xF0);
        int ringColor = selected
                ? palette.highlightColor()
                : hovered
                ? tintTowardWhite(stateColor, 0.25f)
                : withAlpha(scaleColor(stateColor, 0.75f), 0xA8);

        fillOctagon(guiGraphics, x - 2, y - 2, size + 4, withAlpha(scaleColor(ringColor, 0.4f), selected ? 0xC0 : 0x78));
        fillOctagon(guiGraphics, x, y, size, borderColor);
        fillOctagon(guiGraphics, x + 2, y + 2, size - 4, fillColor);
        fillOctagon(guiGraphics, x + 5, y + 5, size - 10, innerColor);
        guiGraphics.fill(x + 4, y + size - 7, x + size - 4, y + size - 4, withAlpha(stateColor, 0xC8));
        if (selected || hovered) {
            fillOctagon(guiGraphics, x + 1, y + 1, size - 2, withAlpha(ringColor, 0x6C));
            fillOctagon(guiGraphics, x + 3, y + 3, size - 6, fillColor);
            fillOctagon(guiGraphics, x + 6, y + 6, size - 12, innerColor);
        }

        renderCanvasNodeIcon(guiGraphics, entry.node(), x, y, size, palette);
        renderCanvasCostBadge(guiGraphics, entry.node(), x, y, size, palette, selected, nodeState);
    }

    private void renderCanvasNodeIcon(
            GuiGraphics guiGraphics,
            UpgradeNodeDefinition node,
            int x,
            int y,
            int size,
            MenuPalette palette
    ) {
        AbilityIcon icon = resolveCanvasNodeIcon(node);
        if (icon != null && this.minecraft != null) {
            int renderWidth = icon.kind() == AbilityIcon.Kind.ITEM ? 16 : Math.max(12, size - 12);
            int renderHeight = icon.kind() == AbilityIcon.Kind.ITEM ? 16 : Math.max(12, size - 12);
            int iconX = x + (size - renderWidth) / 2;
            int iconY = y + (size - renderHeight) / 2;
            AbilityIconRenderer.render(guiGraphics, this.minecraft, icon, iconX, iconY, renderWidth, renderHeight);
            return;
        }

        String glyph = canvasNodeGlyph(node);
        guiGraphics.drawCenteredString(this.font, glyph, x + size / 2, y + (size - this.font.lineHeight) / 2 - 1, palette.titleColor());
    }

    private void fillOctagon(GuiGraphics guiGraphics, int x, int y, int size, int color) {
        if (size <= 0) {
            return;
        }
        int inset = Math.max(1, size / 5);
        guiGraphics.fill(x + inset, y, x + size - inset, y + size, color);
        guiGraphics.fill(x, y + inset, x + size, y + size - inset, color);
    }

    private void renderCanvasCostBadge(
            GuiGraphics guiGraphics,
            UpgradeNodeDefinition node,
            int x,
            int y,
            int size,
            MenuPalette palette,
            boolean selected,
            NodeState nodeState
    ) {
        String cost = primaryCostSummary(node);
        if ("-".equals(cost)) {
            return;
        }
        int badgeWidth = Math.max(11, this.font.width(cost) + 6);
        int badgeX = x + size - badgeWidth + 2;
        int badgeY = y - 2;
        int badgeColor = selected ? palette.highlightColor() : withAlpha(nodeState.color(), 0xD2);
        guiGraphics.fill(badgeX, badgeY, badgeX + badgeWidth, badgeY + 10, withAlpha(scaleColor(badgeColor, 0.55f), 0xD6));
        guiGraphics.drawCenteredString(this.font, cost, badgeX + badgeWidth / 2, badgeY + 1, palette.titleColor());
    }

    private void renderCanvasNodeLabel(
            GuiGraphics guiGraphics,
            ProgressionLayoutPlanner.NodePlacement entry,
            MenuPalette palette,
            boolean selected
    ) {
        int centerX = canvasNodeCenterX(entry);
        ProgressionTreeLayout layout = currentPresentation().treeLayout();
        int y = canvasNodeY(entry) + canvasNodeSize() + layout.labelGap();
        int color = selected ? palette.titleColor() : palette.bodyColor();
        List<String> lines = wrappedTreeCanvasNodeLabel(entry.node());
        for (int index = 0; index < lines.size(); index++) {
            guiGraphics.drawCenteredString(this.font, lines.get(index), centerX, y + index * layout.labelLineHeight(), color);
        }
    }

    private void renderCanvasHoverCard(GuiGraphics guiGraphics, int mouseX, int mouseY, MenuPalette palette) {
        ProgressionLayoutPlanner.NodePlacement focus = hoveredCanvasNode(mouseX, mouseY);
        if (focus == null) {
            return;
        }

        NodeState nodeState = nodeState(focus.node(), currentData());
        String title = focus.node().displayName().getString();
        String subtitle = nodeState.label().getString();
        int width = Math.min(150, Math.max(this.font.width(title), this.font.width(subtitle)) + 12);
        int x = Mth.clamp(canvasNodeCenterX(focus) - width / 2, nodePanelLeft() + 8, nodePanelRight() - width - 8);
        int y = Math.max(canvasTop() - 2, canvasNodeY(focus) - 18);
        guiGraphics.fill(x, y, x + width, y + 20, withAlpha(palette.panelColor(), 0xF0));
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 19, withAlpha(palette.secondaryPanelColor(), 0xF2));
        guiGraphics.drawCenteredString(this.font, this.font.plainSubstrByWidth(title, width - 8), x + width / 2, y + 3, palette.titleColor());
        guiGraphics.drawCenteredString(this.font, subtitle, x + width / 2, y + 11, nodeState.color());
    }

    private @Nullable AbilityIcon resolveCanvasNodeIcon(UpgradeNodeDefinition node) {
        for (ResourceLocation abilityId : sortedIds(node.rewards().abilities())) {
            Optional<AbilityDefinition> ability = AbilityApi.findAbility(abilityId);
            if (ability.isPresent()) {
                return ability.get().icon();
            }
        }
        for (ResourceLocation passiveId : sortedIds(node.rewards().passives())) {
            Optional<PassiveDefinition> passive = PassiveApi.findPassive(passiveId);
            if (passive.isPresent()) {
                return passive.get().icon();
            }
        }
        return null;
    }

    private String canvasNodeGlyph(UpgradeNodeDefinition node) {
        if (!node.rewards().grantedItems().isEmpty()) {
            return glyphFrom(displayGrantedItemName(sortedIds(node.rewards().grantedItems()).getFirst()).getString());
        }
        if (!node.rewards().identities().isEmpty()) {
            return glyphFrom(displayIdentityName(sortedIds(node.rewards().identities()).getFirst()).getString());
        }
        if (!node.rewards().recipePermissions().isEmpty()) {
            return "RP";
        }
        return glyphFrom(node.displayName().getString());
    }

    private String glyphFrom(String value) {
        String[] words = value.trim().split("\\s+");
        StringBuilder builder = new StringBuilder(2);
        for (String word : words) {
            if (!word.isEmpty() && Character.isLetterOrDigit(word.charAt(0))) {
                builder.append(Character.toUpperCase(word.charAt(0)));
            }
            if (builder.length() == 2) {
                break;
            }
        }
        if (builder.length() == 0 && !value.isEmpty()) {
            builder.append(Character.toUpperCase(value.charAt(0)));
        }
        return builder.length() == 0 ? "?" : builder.toString();
    }

    private int connectionColor(UpgradeNodeDefinition node, UpgradeProgressData data, MenuPalette palette) {
        if (data.hasUnlockedNode(node.id())) {
            return palette.successColor();
        }
        if (UpgradeApi.firstStructuralUnlockFailure(data, node).isEmpty()) {
            return palette.emphasisColor();
        }
        if (hasRequiredNodes(node, data)) {
            return palette.warningColor();
        }
        return palette.subduedColor();
    }

    private NodeState nodeState(UpgradeNodeDefinition node, UpgradeProgressData data) {
        MenuPalette palette = currentPresentation().palette();
        if (data.hasUnlockedNode(node.id())) {
            return new NodeState(
                    Component.translatable("screen.xlib.progression_menu.unlocked"),
                    "Done",
                    palette.successColor()
            );
        }
        if (UpgradeApi.firstStructuralUnlockFailure(data, node).isEmpty()) {
            return new NodeState(
                    Component.translatable("screen.xlib.progression_menu.ready"),
                    "Ready",
                    palette.emphasisColor()
            );
        }
        return new NodeState(
                Component.translatable("screen.xlib.progression_menu.locked"),
                "Locked",
                palette.warningColor()
        );
    }

    private boolean hasRequiredNodes(UpgradeNodeDefinition node, UpgradeProgressData data) {
        return node.requiredNodes().stream().allMatch(data::hasUnlockedNode);
    }

    private boolean hasPointCosts(UpgradeNodeDefinition node, UpgradeProgressData data) {
        return node.pointCosts().entrySet().stream().allMatch(entry -> data.points(entry.getKey()) >= entry.getValue());
    }

    private Component pointSummary() {
        UpgradeProgressData data = currentData();
        if (data.pointBalances().isEmpty()) {
            return Component.translatable("screen.xlib.progression_menu.none");
        }

        MutableComponent summary = Component.empty();
        boolean first = true;
        for (Map.Entry<ResourceLocation, Integer> entry : sortedEntries(data.pointBalances())) {
            if (!first) {
                summary = summary.append(Component.literal(", "));
            }
            summary = summary.append(Component.literal(entry.getValue() + " ")).append(displayPointName(entry.getKey()));
            first = false;
        }
        return summary;
    }

    private Component counterSummary() {
        UpgradeProgressData data = currentData();
        if (data.counters().isEmpty()) {
            return Component.translatable("screen.xlib.progression_menu.none");
        }

        MutableComponent summary = Component.empty();
        boolean first = true;
        for (Map.Entry<ResourceLocation, Integer> entry : sortedEntries(data.counters())) {
            if (!first) {
                summary = summary.append(Component.literal(", "));
            }
            summary = summary.append(Component.literal(entry.getValue() + " ")).append(UpgradeRequirements.displayCounterName(entry.getKey()));
            first = false;
        }
        return summary;
    }

    private List<Component> pointSourceSummary() {
        Set<ResourceLocation> pointTypeIds = new LinkedHashSet<>();
        for (ProgressionLayoutPlanner.NodePlacement entry : this.visibleNodes) {
            pointTypeIds.addAll(entry.node().pointCosts().keySet());
        }

        List<Component> sourceLines = new ArrayList<>();
        Language language = Language.getInstance();
        for (ResourceLocation pointTypeId : sortedIds(pointTypeIds)) {
            UpgradeApi.findPointType(pointTypeId).ifPresent(pointType -> {
                String descriptionKey = pointType.descriptionKey();
                if (language.has(descriptionKey)) {
                    sourceLines.add(Component.literal("- ").append(pointType.displayName()).append(Component.literal(": "))
                            .append(Component.translatable(descriptionKey)));
                }
            });
        }
        return List.copyOf(sourceLines);
    }

    private Component displayPointName(ResourceLocation pointTypeId) {
        return UpgradeApi.findPointType(pointTypeId)
                .map(UpgradePointType::displayName)
                .orElse(Component.literal(displayMetadataLabel(pointTypeId)));
    }

    private Component displayNodeName(ResourceLocation nodeId) {
        return UpgradeApi.findNode(nodeId)
                .map(UpgradeNodeDefinition::displayName)
                .orElse(Component.literal(displayMetadataLabel(nodeId)));
    }

    private List<Component> rewardSummary(UpgradeNodeDefinition node) {
        List<Component> rewards = new ArrayList<>();
        node.rewards().abilities().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(abilityId ->
                rewards.add(Component.literal("- ").append(Component.translatable("screen.xlib.progression_menu.reward_ability"))
                        .append(Component.literal(": "))
                        .append(displayAbilityName(abilityId))));
        node.rewards().passives().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(passiveId ->
                rewards.add(Component.literal("- ").append(Component.translatable("screen.xlib.progression_menu.reward_passive"))
                        .append(Component.literal(": "))
                        .append(displayPassiveName(passiveId))));
        node.rewards().grantedItems().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(grantedItemId ->
                rewards.add(Component.literal("- ").append(Component.translatable("screen.xlib.progression_menu.reward_item"))
                        .append(Component.literal(": "))
                        .append(displayGrantedItemName(grantedItemId))));
        node.rewards().recipePermissions().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(recipeId ->
                rewards.add(Component.literal("- ").append(Component.translatable("screen.xlib.progression_menu.reward_recipe"))
                        .append(Component.literal(": "))
                        .append(Component.literal(displayMetadataLabel(recipeId)))));
        node.rewards().identities().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(identityId ->
                rewards.add(Component.literal("- ").append(Component.translatable("screen.xlib.progression_menu.reward_identity"))
                        .append(Component.literal(": "))
                        .append(displayIdentityName(identityId))));
        return rewards;
    }

    private List<Component> rewardDescriptionSummary(UpgradeNodeDefinition node) {
        List<Component> descriptions = new ArrayList<>();
        node.rewards().abilities().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(abilityId ->
                AbilityApi.findAbility(abilityId).ifPresent(ability -> descriptions.add(
                        Component.literal("- ")
                                .append(ability.displayName())
                                .append(Component.literal(": "))
                                .append(describedComponent(ability.displayName(), ability.translationKey() + ".desc"))
                )));
        node.rewards().passives().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(passiveId ->
                PassiveApi.findPassive(passiveId).ifPresent(passive -> descriptions.add(
                        Component.literal("- ")
                                .append(passive.displayName())
                                .append(Component.literal(": "))
                                .append(describedComponent(passive.displayName(), passive.translationKey() + ".desc"))
                )));
        return descriptions;
    }

    private Component describedComponent(Component displayName, String translationKey) {
        return Language.getInstance().has(translationKey)
                ? Component.translatable(translationKey)
                : Component.translatable("screen.xlib.progression_menu.no_description", displayName);
    }

    private int drawTrackMetadata(int x, int startY, GuiGraphics guiGraphics) {
        ResourceLocation trackId = currentTrackView().trackId();
        if (trackId == null) {
            return startY;
        }
        UpgradeTrackDefinition track = UpgradeApi.findTrack(trackId).orElse(null);
        if (track == null) {
            return startY;
        }

        MenuPalette palette = currentPresentation().palette();
        int y = startY;
        y = drawOptionalMetadataLine(guiGraphics, x, y, track.familyId(), "screen.xlib.progression_menu.family", palette.familyColor());
        y = drawOptionalMetadataLine(guiGraphics, x, y, track.groupId(), "screen.xlib.progression_menu.group", palette.groupColor());
        y = drawOptionalMetadataLine(guiGraphics, x, y, track.pageId(), "screen.xlib.progression_menu.page", palette.pageColor());
        return drawTagLine(guiGraphics, x, y, track.tags(), palette.tagColor());
    }

    private Component displayAbilityName(ResourceLocation abilityId) {
        return AbilityApi.findAbility(abilityId)
                .map(AbilityDefinition::displayName)
                .orElse(Component.literal(displayMetadataLabel(abilityId)));
    }

    private Component displayPassiveName(ResourceLocation passiveId) {
        return PassiveApi.findPassive(passiveId)
                .map(passive -> passive.displayName())
                .orElse(Component.literal(displayMetadataLabel(passiveId)));
    }

    private Component displayGrantedItemName(ResourceLocation grantedItemId) {
        return GrantedItemApi.findGrantedItem(grantedItemId).isPresent()
                ? Component.literal(displayMetadataLabel(grantedItemId))
                : Component.literal(displayMetadataLabel(grantedItemId));
    }

    private Component displayIdentityName(ResourceLocation identityId) {
        return IdentityApi.findIdentity(identityId).isPresent()
                ? UpgradeRequirements.displayIdentityName(identityId)
                : Component.literal(displayMetadataLabel(identityId));
    }

    private int drawSectionHeader(GuiGraphics guiGraphics, int x, int y, String translationKey) {
        guiGraphics.drawString(this.font, Component.translatable(translationKey), x, y, currentPresentation().palette().titleColor(), false);
        return y + 12;
    }

    private int drawWrappedLine(GuiGraphics guiGraphics, int x, int startY, int width, int color, Component text) {
        int y = startY;
        for (FormattedCharSequence line : this.font.split(text, width)) {
            guiGraphics.drawString(this.font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private int drawOptionalMetadataLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Optional<ResourceLocation> maybeId,
            String translationKey,
            int color
    ) {
        if (maybeId.isEmpty()) {
            return startY;
        }
        return drawWrappedLine(
                guiGraphics,
                x,
                startY,
                DETAILS_PANEL_WIDTH,
                color,
                Component.translatable(translationKey, Component.literal(displayMetadataLabel(maybeId.get())))
        );
    }

    private int drawTagLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Set<ResourceLocation> tags,
            int color
    ) {
        if (tags.isEmpty()) {
            return startY;
        }
        return drawWrappedLine(
                guiGraphics,
                x,
                startY,
                DETAILS_PANEL_WIDTH,
                color,
                Component.translatable("screen.xlib.progression_menu.tags", Component.literal(joinIds(tags)))
        );
    }

    private int drawResourceListLine(
            GuiGraphics guiGraphics,
            int x,
            int startY,
            Set<ResourceLocation> ids,
            String translationKey,
            int color,
            java.util.function.Function<ResourceLocation, Component> display
    ) {
        if (ids.isEmpty()) {
            return startY;
        }

        MutableComponent value = Component.empty();
        boolean first = true;
        for (ResourceLocation id : sortedIds(ids)) {
            if (!first) {
                value = value.append(Component.literal(", "));
            }
            value = value.append(display.apply(id));
            first = false;
        }
        return drawWrappedLine(
                guiGraphics,
                x,
                startY,
                DETAILS_PANEL_WIDTH,
                color,
                Component.translatable(translationKey, value)
        );
    }

    private static int withAlpha(int color, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (color & 0x00FFFFFF);
    }

    private int resolvedTreeLabelWidth() {
        ProgressionTreeLayout layout = currentPresentation().treeLayout();
        int columns = Math.max(1, this.nodeLayoutPlan.columnCount());
        int availableWidth = Math.max(36, canvasInnerRight() - canvasInnerLeft());
        if (columns <= 1) {
            return Math.min(layout.labelWidth(), availableWidth);
        }
        int usableWidth = Math.max(canvasNodeSize(), availableWidth);
        int maxSpacing = Math.max(8, (usableWidth - canvasNodeSize()) / (columns - 1));
        return Math.max(36, Math.min(layout.labelWidth(), Math.max(36, maxSpacing - 12)));
    }

    private static int scaleColor(int color, float factor) {
        int alpha = (color >>> 24) & 0xFF;
        int red = Math.min(255, Math.round(((color >>> 16) & 0xFF) * factor));
        int green = Math.min(255, Math.round(((color >>> 8) & 0xFF) * factor));
        int blue = Math.min(255, Math.round((color & 0xFF) * factor));
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int tintTowardWhite(int color, float amount) {
        int alpha = (color >>> 24) & 0xFF;
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        int tintedRed = Math.min(255, Math.round(red + (255 - red) * amount));
        int tintedGreen = Math.min(255, Math.round(green + (255 - green) * amount));
        int tintedBlue = Math.min(255, Math.round(blue + (255 - blue) * amount));
        return (alpha << 24) | (tintedRed << 16) | (tintedGreen << 8) | tintedBlue;
    }

    private static List<Map.Entry<ResourceLocation, Integer>> sortedEntries(Map<ResourceLocation, Integer> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList();
    }

    private static String joinIds(Collection<ResourceLocation> ids) {
        return ids.stream()
                .map(ProgressionMenuScreen::displayMetadataLabel)
                .sorted()
                .collect(java.util.stream.Collectors.joining(", "));
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

    private static List<ResourceLocation> sortedIds(Set<ResourceLocation> values) {
        return values.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private record TrackView(@Nullable ResourceLocation trackId, Component title) {}

    private record NodeState(Component label, String shortLabel, int color) {}
}
