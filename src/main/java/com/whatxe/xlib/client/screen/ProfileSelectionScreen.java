package com.whatxe.xlib.client.screen;

import com.whatxe.xlib.ability.AbilityApi;
import com.whatxe.xlib.ability.IdentityApi;
import com.whatxe.xlib.ability.ProfileApi;
import com.whatxe.xlib.ability.ProfileDefinition;
import com.whatxe.xlib.ability.ProfileGroupDefinition;
import com.whatxe.xlib.ability.ProfileSelectionData;
import com.whatxe.xlib.attachment.ModAttachments;
import com.whatxe.xlib.client.AbilityIconRenderer;
import com.whatxe.xlib.client.ProfileSelectionScreenContext;
import com.whatxe.xlib.network.ClaimProfilePayload;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import com.whatxe.xlib.progression.UpgradeApi;
import com.whatxe.xlib.progression.UpgradeRequirements;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public class ProfileSelectionScreen extends Screen {
    private static final int LIST_BUTTON_WIDTH = 170;
    private static final int DETAILS_WIDTH = 170;
    private static final int PANEL_TOP_Y = 36;
    private static final int TITLE_Y = 10;
    private static final int PANEL_BOTTOM_MARGIN = 30;

    private final @Nullable ResourceLocation requestedGroupId;
    private final List<Button> profileButtons = new ArrayList<>();
    private @Nullable Button confirmSelectionButton;
    private @Nullable ResourceLocation selectedProfileId;

    public ProfileSelectionScreen() {
        this(ProfileSelectionScreenContext.defaultContext());
    }

    public ProfileSelectionScreen(ProfileSelectionScreenContext context) {
        super(Component.translatable("screen.xlib.profile_selection"));
        this.requestedGroupId = context.pendingGroupId();
    }

    @Override
    protected void init() {
        rebuildProfileWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (currentPendingGroupId().isEmpty()) {
            this.minecraft.setScreen(null);
            return;
        }
        if (this.profileButtons.isEmpty() || this.confirmSelectionButton == null) {
            rebuildProfileWidgets();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int listX = centerX - 185;
        int detailsX = centerX + 15;
        int topY = PANEL_TOP_Y;
        guiGraphics.fill(listX - 8, topY - 8, listX + LIST_BUTTON_WIDTH + 8, this.height - PANEL_BOTTOM_MARGIN, 0xAA11141A);
        guiGraphics.fill(detailsX - 8, topY - 8, detailsX + DETAILS_WIDTH + 8, this.height - PANEL_BOTTOM_MARGIN, 0xAA171B22);

        guiGraphics.drawString(this.font, this.title, centerX - this.font.width(this.title) / 2, TITLE_Y, 0xFFFFFF, false);
        ProfileGroupDefinition group = currentGroup().orElse(null);
        if (group == null) {
            return;
        }

        guiGraphics.drawString(this.font, group.displayName(), listX, topY, 0xE3EEF8, false);
        int descriptionY = topY + 16;
        for (var line : this.font.split(group.description(), LIST_BUTTON_WIDTH)) {
            guiGraphics.drawString(this.font, line, listX, descriptionY, 0xB7C6D6, false);
            descriptionY += 10;
        }

        Optional<ProfileDefinition> selectedProfile = Optional.ofNullable(this.selectedProfileId).flatMap(ProfileApi::findProfile);
        int detailsY = topY;
        if (selectedProfile.isPresent()) {
            ProfileDefinition profile = selectedProfile.get();
            guiGraphics.drawString(this.font, profile.displayName(), detailsX, detailsY, 0xFFFFFF, false);
            if (this.minecraft != null) {
                AbilityIconRenderer.render(guiGraphics, this.minecraft, profile.icon(), detailsX + DETAILS_WIDTH - 20, detailsY - 2, 16, 16);
            }
            detailsY += 18;
            for (var line : this.font.split(profile.description(), DETAILS_WIDTH)) {
                guiGraphics.drawString(this.font, line, detailsX, detailsY, 0xB7C6D6, false);
                detailsY += 10;
            }
            detailsY += 10;
            detailsY = drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.bundles", joinValues(profile.grantBundles(), ProfileSelectionScreen::displayMetadataComponent)), 0x9BD38C);
            detailsY = drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.identities", joinValues(profile.identities(), ProfileSelectionScreen::displayIdentityName)), 0xC1A7FF);
            detailsY = drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.abilities", joinValues(profile.abilities(), ProfileSelectionScreen::displayAbilityName)), 0xFFD87B);
            detailsY = drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.modes", joinValues(profile.modes(), ProfileSelectionScreen::displayAbilityName)), 0xFFB48A);
            detailsY = drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.artifacts", joinValues(profile.unlockedArtifacts(), ProfileSelectionScreen::displayMetadataComponent)), 0x8ED7D7);
            detailsY = drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.nodes", joinValues(profile.startingNodes(), ProfileSelectionScreen::displayNodeName)), 0xE5A8A8);
            drawDetailLine(guiGraphics, detailsX, detailsY,
                    Component.translatable("screen.xlib.profile_selection.conflicts", joinValues(profile.incompatibleProfiles(), ProfileSelectionScreen::displayProfileName)), 0xF2A5A5);
        }
    }

    private void rebuildProfileWidgets() {
        this.clearWidgets();
        this.profileButtons.clear();
        this.confirmSelectionButton = null;
        Optional<ResourceLocation> pendingGroupId = currentPendingGroupId();
        if (pendingGroupId.isEmpty()) {
            return;
        }

        List<ProfileDefinition> profiles = availableProfiles(pendingGroupId.get());
        if (profiles.isEmpty()) {
            return;
        }
        if (this.selectedProfileId == null || profiles.stream().noneMatch(profile -> profile.id().equals(this.selectedProfileId))) {
            this.selectedProfileId = profiles.getFirst().id();
        }
        int startX = this.width / 2 - 185;
        int startY = PANEL_TOP_Y + 48;
        ProfileGroupDefinition group = currentGroup().orElse(null);
        if (group != null) {
            int descriptionY = PANEL_TOP_Y + 16;
            for (var ignored : this.font.split(group.description(), LIST_BUTTON_WIDTH)) {
                descriptionY += 10;
            }
            startY = descriptionY + 12;
        }
        for (int index = 0; index < profiles.size(); index++) {
            ProfileDefinition profile = profiles.get(index);
            Button button = Button.builder(profileButtonLabel(profile), pressed -> {
                this.selectedProfileId = profile.id();
                rebuildProfileWidgets();
            }).bounds(startX, startY + index * 24, LIST_BUTTON_WIDTH, 20).build();
            this.profileButtons.add(this.addRenderableWidget(button));
        }
        this.confirmSelectionButton = this.addRenderableWidget(Button.builder(
                Component.translatable("screen.xlib.profile_selection.confirm"),
                pressed -> {
                    if (this.selectedProfileId == null) {
                        return;
                    }
                    currentPendingGroupId().ifPresent(groupId ->
                            PacketDistributor.sendToServer(new ClaimProfilePayload(groupId, this.selectedProfileId)));
                }
        ).bounds(this.width / 2 + 15, this.height - 52, DETAILS_WIDTH, 20).build());
        this.confirmSelectionButton.active = this.selectedProfileId != null;
    }

    private Optional<ResourceLocation> currentPendingGroupId() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return Optional.empty();
        }
        ProfileSelectionData data = ModAttachments.getProfiles(this.minecraft.player);
        if (this.requestedGroupId != null && data.hasPendingGroup(this.requestedGroupId)) {
            return Optional.of(this.requestedGroupId);
        }
        return ProfileApi.firstPendingGroupId(data);
    }

    private Optional<ProfileGroupDefinition> currentGroup() {
        return currentPendingGroupId().flatMap(ProfileApi::findGroup);
    }

    private List<ProfileDefinition> availableProfiles(ResourceLocation groupId) {
        return ProfileApi.allProfiles().stream()
                .filter(profile -> profile.groupId().equals(groupId))
                .sorted(Comparator.comparing(profile -> profile.displayName().getString().toLowerCase(java.util.Locale.ROOT)))
                .toList();
    }

    private int drawDetailLine(GuiGraphics guiGraphics, int x, int y, Component component, int color) {
        for (var line : this.font.split(component, DETAILS_WIDTH)) {
            guiGraphics.drawString(this.font, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    private Component profileButtonLabel(ProfileDefinition profile) {
        return profile.id().equals(this.selectedProfileId)
                ? Component.literal("> ").append(profile.displayName())
                : profile.displayName();
    }

    private static Component joinValues(Collection<ResourceLocation> ids, Function<ResourceLocation, Component> display) {
        if (ids.isEmpty()) {
            return Component.literal("-");
        }
        MutableComponent value = Component.empty();
        boolean first = true;
        for (ResourceLocation id : ids.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList()) {
            if (!first) {
                value.append(Component.literal(", "));
            }
            value.append(display.apply(id));
            first = false;
        }
        return value;
    }

    private static Component displayAbilityName(ResourceLocation abilityId) {
        return AbilityApi.findAbility(abilityId)
                .map(ability -> ability.displayName())
                .orElseGet(() -> displayMetadataComponent(abilityId));
    }

    private static Component displayIdentityName(ResourceLocation identityId) {
        return IdentityApi.findIdentity(identityId).isPresent()
                ? UpgradeRequirements.displayIdentityName(identityId)
                : displayMetadataComponent(identityId);
    }

    private static Component displayNodeName(ResourceLocation nodeId) {
        return UpgradeApi.findNode(nodeId)
                .map(node -> node.displayName())
                .orElseGet(() -> displayMetadataComponent(nodeId));
    }

    private static Component displayProfileName(ResourceLocation profileId) {
        return ProfileApi.findProfile(profileId)
                .map(ProfileDefinition::displayName)
                .orElseGet(() -> displayMetadataComponent(profileId));
    }

    private static Component displayMetadataComponent(ResourceLocation id) {
        return Component.literal(displayMetadataLabel(id));
    }

    private static String displayMetadataLabel(ResourceLocation id) {
        String path = id.getPath();
        int slashIndex = path.indexOf('/');
        if (slashIndex >= 0 && slashIndex + 1 < path.length()) {
            path = path.substring(slashIndex + 1);
        }
        if (path.startsWith("demo_")) {
            path = path.substring("demo_".length());
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
}
