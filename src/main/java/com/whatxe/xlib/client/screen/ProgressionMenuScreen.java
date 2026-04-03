package com.whatxe.xlib.client.screen;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.AbilityDefinition;
import com.whatxe.xlib.ability.GrantedItemApi;
import com.whatxe.xlib.ability.PassiveApi;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.network.UnlockUpgradeNodePayload;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeNodeDefinition;
import com.whatxe.xlib.progression.UpgradePointType;
import com.whatxe.xlib.progression.UpgradeProgressData;
import com.whatxe.xlib.progression.UpgradeRequirement;
import com.whatxe.xlib.progression.UpgradeRequirements;
import com.whatxe.xlib.progression.UpgradeTrackDefinition;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
    private static final int NODE_LIST_Y = 128;
    private static final int NODE_LIST_X_OFFSET = 186;
    private static final int DETAILS_PANEL_X_OFFSET = 6;
    private static final int DETAILS_PANEL_WIDTH = 202;
    private static final int TRACK_NAV_Y = 46;
    private static final int TRACK_LABEL_Y = 50;
    private static final int SUMMARY_Y = 76;
    private static final int ACTION_BUTTON_Y_OFFSET = 34;

    private final List<Button> nodeButtons = new ArrayList<>();
    private List<TrackView> trackViews = List.of();
    private List<NodeEntry> visibleNodes = List.of();
    private @Nullable ResourceLocation selectedNodeId;
    private int selectedTrackIndex;
    private int nodeScrollOffset;
    private int visibleNodeButtonCount = MIN_VISIBLE_NODE_BUTTONS;
    private int nodeButtonX;
    private Button previousTrackButton;
    private Button nextTrackButton;
    private Button unlockButton;
    private Button openAbilityMenuButton;

    public ProgressionMenuScreen() {
        super(Component.translatable("screen.xlib.progression_menu"));
    }

    @Override
    protected void init() {
        buildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
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
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0xD0101010, 0xE0101010);

        int listPanelLeft = this.nodeButtonX - 6;
        int listPanelTop = NODE_LIST_Y - 18;
        int listPanelBottom = nodeListBottom() + 6;
        int detailsX = this.width / 2 + DETAILS_PANEL_X_OFFSET;
        int detailsPanelTop = TRACK_NAV_Y;
        int detailsPanelBottom = this.height - 26;

        guiGraphics.fill(listPanelLeft, listPanelTop, listPanelLeft + NODE_BUTTON_WIDTH + 18, listPanelBottom, 0x33202020);
        guiGraphics.fill(detailsX - 8, detailsPanelTop - 8, detailsX + DETAILS_PANEL_WIDTH + 8, detailsPanelBottom, 0x33202020);

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawString(this.font, this.title, this.width / 2 - this.font.width(this.title) / 2, 16, 0xFFFFFF, false);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.xlib.progression_menu.instructions"),
                this.width / 2 - 198,
                30,
                0xCFCFCF,
                false
        );
        guiGraphics.drawString(this.font, Component.translatable("screen.xlib.progression_menu.nodes"), this.nodeButtonX, NODE_LIST_Y - 14, 0xFFFFFF, false);

        TrackView trackView = currentTrackView();
        Component trackLine = Component.translatable("screen.xlib.progression_menu.track", trackView.title());
        guiGraphics.drawCenteredString(this.font, trackLine, this.width / 2 + 6, TRACK_LABEL_Y, 0xFFFFFF);

        int summaryY = SUMMARY_Y;
        summaryY = drawWrappedLine(
                guiGraphics,
                detailsX,
                summaryY,
                DETAILS_PANEL_WIDTH,
                0xF2E5A3,
                Component.translatable("screen.xlib.progression_menu.points", pointSummary())
        );
        summaryY = drawWrappedLine(
                guiGraphics,
                detailsX,
                summaryY + 2,
                DETAILS_PANEL_WIDTH,
                0x9FD4FF,
                Component.translatable("screen.xlib.progression_menu.counters", counterSummary())
        );
        List<Component> pointSources = pointSourceSummary();
        if (!pointSources.isEmpty()) {
            summaryY = drawSectionHeader(guiGraphics, detailsX, summaryY + 8, "screen.xlib.progression_menu.point_sources");
            for (Component sourceLine : pointSources) {
                summaryY = drawWrappedLine(guiGraphics, detailsX, summaryY, DETAILS_PANEL_WIDTH, 0xD8B6FF, sourceLine);
            }
        }

        Optional<NodeEntry> maybeSelectedNode = selectedNodeEntry();
        if (maybeSelectedNode.isEmpty()) {
            guiGraphics.drawString(
                    this.font,
                    Component.translatable("screen.xlib.progression_menu.no_nodes"),
                    detailsX,
                    summaryY + 16,
                    0xD0D0D0,
                    false
            );
            return;
        }

        UpgradeProgressData data = currentData();
        NodeEntry selectedNode = maybeSelectedNode.get();
        UpgradeNodeDefinition node = selectedNode.node();
        NodeState nodeState = nodeState(node, data);

        int detailY = summaryY + 18;
        guiGraphics.drawString(this.font, node.displayName(), detailsX, detailY, 0xFFFFFF, false);
        detailY += 12;
        guiGraphics.drawString(this.font, nodeState.label(), detailsX, detailY, nodeState.color(), false);
        detailY += 16;

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY, "screen.xlib.progression_menu.costs");
        if (node.pointCosts().isEmpty()) {
            detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, 0xA9E6B0,
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            boolean unlocked = data.hasUnlockedNode(node.id());
            for (Map.Entry<ResourceLocation, Integer> entry : sortedEntries(node.pointCosts())) {
                int currentPoints = data.points(entry.getKey());
                int color = unlocked || currentPoints >= entry.getValue() ? 0xA9E6B0 : 0xFF9D9D;
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
            detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, 0xA9E6B0,
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            for (ResourceLocation requiredNodeId : sortedIds(node.requiredNodes())) {
                boolean satisfied = data.hasUnlockedNode(requiredNodeId);
                detailY = drawWrappedLine(
                        guiGraphics,
                        detailsX,
                        detailY,
                        DETAILS_PANEL_WIDTH,
                        satisfied ? 0xA9E6B0 : 0xFF9D9D,
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
                        0xD8B6FF,
                        Component.literal("- ").append(requirement.description())
                );
            }
        }

        detailY = drawSectionHeader(guiGraphics, detailsX, detailY + 4, "screen.xlib.progression_menu.rewards");
        List<Component> rewards = rewardSummary(node);
        if (rewards.isEmpty()) {
            drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, 0xD0D0D0,
                    Component.translatable("screen.xlib.progression_menu.none"));
        } else {
            for (Component rewardLine : rewards) {
                detailY = drawWrappedLine(guiGraphics, detailsX, detailY, DETAILS_PANEL_WIDTH, 0x8FD7FF, rewardLine);
            }
        }

        if (maxNodeScrollOffset() > 0) {
            renderScrollBar(guiGraphics);
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

    private void buildWidgets() {
        this.clearWidgets();
        this.nodeButtons.clear();
        refreshTrackViews();
        refreshVisibleNodes();

        int previousTrackX = this.width / 2 - 60;
        this.previousTrackButton = this.addRenderableWidget(Button.builder(Component.literal("<"), pressed -> cycleTrack(-1))
                .bounds(previousTrackX, TRACK_NAV_Y, 20, 20)
                .build());
        this.nextTrackButton = this.addRenderableWidget(Button.builder(Component.literal(">"), pressed -> cycleTrack(1))
                .bounds(previousTrackX + 120, TRACK_NAV_Y, 20, 20)
                .build());

        this.nodeButtonX = this.width / 2 - NODE_LIST_X_OFFSET;
        this.visibleNodeButtonCount = computeVisibleNodeButtonCount();
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset, 0, maxNodeScrollOffset());
        for (int index = 0; index < this.visibleNodeButtonCount; index++) {
            final int visibleIndex = index;
            Button nodeButton = Button.builder(Component.empty(), pressed -> selectVisibleNode(visibleIndex))
                    .bounds(this.nodeButtonX, NODE_LIST_Y + index * NODE_BUTTON_SPACING, NODE_BUTTON_WIDTH, 20)
                    .build();
            this.nodeButtons.add(this.addRenderableWidget(nodeButton));
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
        this.selectedTrackIndex = Mth.clamp(this.selectedTrackIndex, 0, Math.max(0, this.trackViews.size() - 1));
    }

    private void refreshVisibleNodes() {
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

        List<NodeEntry> orderedNodes = new ArrayList<>();
        Set<ResourceLocation> visited = new LinkedHashSet<>();
        Map<ResourceLocation, List<UpgradeNodeDefinition>> children = buildChildrenByParent(filteredNodes.values());
        ArrayDeque<UpgradeNodeDefinition> rootQueue = new ArrayDeque<>();

        if (selectedTrackId != null) {
            UpgradeApi.findTrack(selectedTrackId).ifPresent(track -> {
                for (ResourceLocation rootNodeId : track.rootNodes()) {
                    UpgradeNodeDefinition rootNode = filteredNodes.get(rootNodeId);
                    if (rootNode != null) {
                        rootQueue.add(rootNode);
                    }
                }
            });
        }

        for (UpgradeNodeDefinition node : filteredNodes.values()) {
            if (inTrackPrerequisiteCount(node, filteredNodes.keySet()) == 0) {
                rootQueue.add(node);
            }
        }

        while (!rootQueue.isEmpty()) {
            UpgradeNodeDefinition rootNode = rootQueue.removeFirst();
            appendNodeTree(rootNode, 0, children, visited, orderedNodes);
        }

        for (UpgradeNodeDefinition node : filteredNodes.values()) {
            if (visited.add(node.id())) {
                orderedNodes.add(new NodeEntry(node, inTrackPrerequisiteCount(node, filteredNodes.keySet())));
            }
        }

        this.visibleNodes = List.copyOf(orderedNodes);
        if (this.selectedNodeId == null || this.visibleNodes.stream().noneMatch(entry -> entry.node().id().equals(this.selectedNodeId))) {
            this.selectedNodeId = this.visibleNodes.isEmpty() ? null : this.visibleNodes.getFirst().node().id();
        }
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset, 0, maxNodeScrollOffset());
    }

    private void updateTrackButtons() {
        boolean hasMultipleTracks = this.trackViews.size() > 1;
        if (this.previousTrackButton != null) {
            this.previousTrackButton.active = hasMultipleTracks;
        }
        if (this.nextTrackButton != null) {
            this.nextTrackButton.active = hasMultipleTracks;
        }
    }

    private void updateNodeButtons() {
        this.nodeScrollOffset = Mth.clamp(this.nodeScrollOffset, 0, maxNodeScrollOffset());
        for (int visibleIndex = 0; visibleIndex < this.nodeButtons.size(); visibleIndex++) {
            int nodeIndex = this.nodeScrollOffset + visibleIndex;
            Button button = this.nodeButtons.get(visibleIndex);
            if (nodeIndex >= this.visibleNodes.size()) {
                button.visible = false;
                button.active = false;
                button.setMessage(Component.empty());
                continue;
            }

            NodeEntry entry = this.visibleNodes.get(nodeIndex);
            NodeState nodeState = nodeState(entry.node(), currentData());
            String prefix = this.selectedNodeId != null && this.selectedNodeId.equals(entry.node().id()) ? "> " : "";
            Component label = Component.literal(prefix + "  ".repeat(Math.max(0, entry.depth())))
                    .append(entry.node().displayName())
                    .append(Component.literal(" [" + nodeState.shortLabel() + "]"));
            button.visible = true;
            button.active = true;
            button.setMessage(label);
        }
    }

    private void updateActionButtons() {
        Optional<NodeEntry> maybeSelectedNode = selectedNodeEntry();
        if (this.unlockButton == null) {
            return;
        }

        if (maybeSelectedNode.isEmpty()) {
            this.unlockButton.active = false;
            return;
        }

        UpgradeNodeDefinition node = maybeSelectedNode.get().node();
        UpgradeProgressData data = currentData();
        boolean trackAvailable = node.trackId() == null || !UpgradeApi.isTrackBlocked(data, node.trackId());
        this.unlockButton.active = !data.hasUnlockedNode(node.id())
                && trackAvailable
                && hasRequiredNodes(node, data)
                && hasPointCosts(node, data);
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
    }

    private void selectVisibleNode(int visibleIndex) {
        int nodeIndex = this.nodeScrollOffset + visibleIndex;
        if (nodeIndex >= 0 && nodeIndex < this.visibleNodes.size()) {
            this.selectedNodeId = this.visibleNodes.get(nodeIndex).node().id();
            updateNodeButtons();
            updateActionButtons();
        }
    }

    private void unlockSelectedNode() {
        selectedNodeEntry().ifPresent(entry -> PacketDistributor.sendToServer(new UnlockUpgradeNodePayload(entry.node().id())));
    }

    private void openAbilityMenu() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new AbilityMenuScreen());
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
        return Math.max(0, this.visibleNodes.size() - this.visibleNodeButtonCount);
    }

    private int nodeListBottom() {
        return NODE_LIST_Y + this.visibleNodeButtonCount * NODE_BUTTON_SPACING - 4;
    }

    private boolean isMouseOverNodeList(double mouseX, double mouseY) {
        int left = this.nodeButtonX - 6;
        int top = NODE_LIST_Y - 18;
        int right = this.nodeButtonX + NODE_BUTTON_WIDTH + 18;
        int bottom = nodeListBottom() + 6;
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    private void renderScrollBar(GuiGraphics guiGraphics) {
        int trackX = this.nodeButtonX + NODE_BUTTON_WIDTH + 6;
        int trackY = NODE_LIST_Y;
        int trackHeight = this.visibleNodeButtonCount * NODE_BUTTON_SPACING - 4;
        int maxOffset = maxNodeScrollOffset();
        int thumbHeight = Math.max(12, trackHeight * this.visibleNodeButtonCount / this.visibleNodes.size());
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = trackY + (maxOffset == 0 ? 0 : Math.round((float) this.nodeScrollOffset / maxOffset * thumbTravel));

        guiGraphics.fill(trackX, trackY, trackX + 4, trackY + trackHeight, 0x66303030);
        guiGraphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFF8BCF9A);
    }

    private TrackView currentTrackView() {
        return this.trackViews.isEmpty()
                ? new TrackView(null, Component.translatable("screen.xlib.progression_menu.all_tracks"))
                : this.trackViews.get(this.selectedTrackIndex);
    }

    private Optional<NodeEntry> selectedNodeEntry() {
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

    private NodeState nodeState(UpgradeNodeDefinition node, UpgradeProgressData data) {
        if (data.hasUnlockedNode(node.id())) {
            return new NodeState(
                    Component.translatable("screen.xlib.progression_menu.unlocked"),
                    "Done",
                    0xA9E6B0
            );
        }
        if (hasRequiredNodes(node, data) && hasPointCosts(node, data)) {
            return new NodeState(
                    Component.translatable("screen.xlib.progression_menu.ready"),
                    "Ready",
                    0xF2E5A3
            );
        }
        return new NodeState(
                Component.translatable("screen.xlib.progression_menu.locked"),
                "Locked",
                0xFF9D9D
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
        for (NodeEntry entry : this.visibleNodes) {
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

    private static Map<ResourceLocation, List<UpgradeNodeDefinition>> buildChildrenByParent(Iterable<UpgradeNodeDefinition> nodes) {
        Map<ResourceLocation, List<UpgradeNodeDefinition>> childrenByParent = new LinkedHashMap<>();
        for (UpgradeNodeDefinition node : nodes) {
            for (ResourceLocation requiredNodeId : node.requiredNodes()) {
                childrenByParent.computeIfAbsent(requiredNodeId, ignored -> new ArrayList<>()).add(node);
            }
        }
        for (List<UpgradeNodeDefinition> children : childrenByParent.values()) {
            children.sort(Comparator.comparing(child -> child.id().toString()));
        }
        return childrenByParent;
    }

    private void appendNodeTree(
            UpgradeNodeDefinition node,
            int depth,
            Map<ResourceLocation, List<UpgradeNodeDefinition>> childrenByParent,
            Set<ResourceLocation> visited,
            List<NodeEntry> output
    ) {
        if (!visited.add(node.id())) {
            return;
        }

        output.add(new NodeEntry(node, depth));
        for (UpgradeNodeDefinition child : childrenByParent.getOrDefault(node.id(), List.of())) {
            appendNodeTree(child, depth + 1, childrenByParent, visited, output);
        }
    }

    private int inTrackPrerequisiteCount(UpgradeNodeDefinition node, Set<ResourceLocation> visibleNodeIds) {
        int count = 0;
        for (ResourceLocation requiredNodeId : node.requiredNodes()) {
            if (visibleNodeIds.contains(requiredNodeId)) {
                count++;
            }
        }
        return count;
    }

    private Component displayPointName(ResourceLocation pointTypeId) {
        return UpgradeApi.findPointType(pointTypeId)
                .map(UpgradePointType::displayName)
                .orElse(Component.literal(pointTypeId.toString()));
    }

    private Component displayNodeName(ResourceLocation nodeId) {
        return UpgradeApi.findNode(nodeId)
                .map(UpgradeNodeDefinition::displayName)
                .orElse(Component.literal(nodeId.toString()));
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
                        .append(Component.literal(recipeId.toString()))));
        return rewards;
    }

    private Component displayAbilityName(ResourceLocation abilityId) {
        return AbilityApi.findAbility(abilityId)
                .map(AbilityDefinition::displayName)
                .orElse(Component.literal(abilityId.toString()));
    }

    private Component displayPassiveName(ResourceLocation passiveId) {
        return PassiveApi.findPassive(passiveId)
                .map(passive -> passive.displayName())
                .orElse(Component.literal(passiveId.toString()));
    }

    private Component displayGrantedItemName(ResourceLocation grantedItemId) {
        return GrantedItemApi.findGrantedItem(grantedItemId).isPresent()
                ? Component.literal(grantedItemId.getPath())
                : Component.literal(grantedItemId.toString());
    }

    private int drawSectionHeader(GuiGraphics guiGraphics, int x, int y, String translationKey) {
        guiGraphics.drawString(this.font, Component.translatable(translationKey), x, y, 0xFFFFFF, false);
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

    private static List<Map.Entry<ResourceLocation, Integer>> sortedEntries(Map<ResourceLocation, Integer> values) {
        return values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .toList();
    }

    private static List<ResourceLocation> sortedIds(Set<ResourceLocation> values) {
        return values.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList();
    }

    private record TrackView(@Nullable ResourceLocation trackId, Component title) {}

    private record NodeEntry(UpgradeNodeDefinition node, int depth) {}

    private record NodeState(Component label, String shortLabel, int color) {}
}
