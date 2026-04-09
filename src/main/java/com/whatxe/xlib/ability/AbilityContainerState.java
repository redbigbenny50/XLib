package com.whatxe.xlib.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record AbilityContainerState(Map<ResourceLocation, AbilityContainerState.ContainerState> containers) {
    private static final Codec<Optional<ResourceLocation>> OPTIONAL_RESOURCE_CODEC = Codec.STRING.xmap(
            value -> value.isBlank() ? Optional.empty() : Optional.of(ResourceLocation.parse(value)),
            value -> value.map(ResourceLocation::toString).orElse("")
    );
    private static final Codec<List<List<Optional<ResourceLocation>>>> OPTIONAL_RESOURCE_PAGES_CODEC =
            OPTIONAL_RESOURCE_CODEC.listOf().listOf();
    private static final Codec<List<List<Integer>>> PAGE_DURATIONS_CODEC = Codec.INT.listOf().listOf();

    public static final Codec<AbilityContainerState> CODEC = Codec.unboundedMap(ResourceLocation.CODEC, ContainerState.CODEC)
            .xmap(AbilityContainerState::new, AbilityContainerState::containers);

    public AbilityContainerState {
        containers = normalizeContainers(containers);
    }

    public static AbilityContainerState empty() {
        return new AbilityContainerState(Map.of());
    }

    public static AbilityContainerState fromLegacy(
            List<Optional<ResourceLocation>> slots,
            List<Optional<ResourceLocation>> comboOverrides,
            List<Integer> comboOverrideDurations,
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        ContainerState primaryState = new ContainerState(
                0,
                List.of(List.copyOf(slots)),
                toPagedModeLoadouts(modeLoadouts),
                List.of(List.copyOf(comboOverrides)),
                List.of(List.copyOf(comboOverrideDurations))
        );
        if (primaryState.isEmpty()) {
            return empty();
        }
        return new AbilityContainerState(Map.of(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID, primaryState));
    }

    public boolean isEmpty() {
        return this.containers.isEmpty();
    }

    public boolean hasContainer(ResourceLocation containerId) {
        return this.containers.containsKey(containerId);
    }

    public Set<ResourceLocation> containerIds() {
        return this.containers.keySet();
    }

    public int activePage(ResourceLocation containerId) {
        return containerState(containerId).map(ContainerState::activePage).orElse(0);
    }

    public int pageCount(ResourceLocation containerId) {
        return containerState(containerId).map(ContainerState::pageCount).orElse(0);
    }

    public int maxSlotsPerPage(ResourceLocation containerId) {
        return containerState(containerId).map(ContainerState::maxSlotsPerPage).orElse(0);
    }

    public int slotCount(ResourceLocation containerId, int pageIndex) {
        return containerState(containerId).map(state -> state.slotCount(pageIndex)).orElse(0);
    }

    public Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> modeLoadouts(ResourceLocation containerId) {
        return containerState(containerId).map(ContainerState::modeLoadouts).orElse(Map.of());
    }

    public Optional<ResourceLocation> abilityInSlot(AbilitySlotReference slotReference) {
        return containerState(slotReference.containerId())
                .flatMap(state -> state.abilityInSlot(slotReference.pageIndex(), slotReference.slotIndex()));
    }

    public Optional<ResourceLocation> modeAbilityInSlot(ResourceLocation modeId, AbilitySlotReference slotReference) {
        return containerState(slotReference.containerId())
                .flatMap(state -> state.modeAbilityInSlot(modeId, slotReference.pageIndex(), slotReference.slotIndex()));
    }

    public Optional<ResourceLocation> comboOverrideInSlot(AbilitySlotReference slotReference) {
        return containerState(slotReference.containerId())
                .flatMap(state -> state.comboOverrideInSlot(slotReference.pageIndex(), slotReference.slotIndex()));
    }

    public int comboOverrideDurationForSlot(AbilitySlotReference slotReference) {
        return containerState(slotReference.containerId())
                .map(state -> state.comboOverrideDurationForSlot(slotReference.pageIndex(), slotReference.slotIndex()))
                .orElse(0);
    }

    public AbilityContainerState withAbilityInSlot(AbilitySlotReference slotReference, @Nullable ResourceLocation abilityId) {
        return updateContainer(slotReference.containerId(),
                state -> state.withAbilityInSlot(slotReference.pageIndex(), slotReference.slotIndex(), abilityId));
    }

    public AbilityContainerState withModeAbilityInSlot(
            ResourceLocation modeId,
            AbilitySlotReference slotReference,
            @Nullable ResourceLocation abilityId
    ) {
        return updateContainer(slotReference.containerId(),
                state -> state.withModeAbilityInSlot(modeId, slotReference.pageIndex(), slotReference.slotIndex(), abilityId));
    }

    public AbilityContainerState clearModeLoadout(ResourceLocation modeId) {
        if (this.containers.isEmpty()) {
            return this;
        }
        Map<ResourceLocation, ContainerState> updated = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<ResourceLocation, ContainerState> entry : this.containers.entrySet()) {
            ContainerState next = entry.getValue().clearModeLoadout(modeId);
            changed |= !next.equals(entry.getValue());
            if (!next.isEmpty()) {
                updated.put(entry.getKey(), next);
            }
        }
        return changed ? new AbilityContainerState(updated) : this;
    }

    public AbilityContainerState withComboOverride(AbilitySlotReference slotReference, @Nullable ResourceLocation abilityId, int durationTicks) {
        return updateContainer(slotReference.containerId(),
                state -> state.withComboOverride(slotReference.pageIndex(), slotReference.slotIndex(), abilityId, durationTicks));
    }

    public AbilityContainerState withActivePage(ResourceLocation containerId, int pageIndex) {
        return updateContainer(containerId, state -> state.withActivePage(pageIndex));
    }

    public AbilityContainerState clearAbilityState(ResourceLocation abilityId) {
        if (this.containers.isEmpty()) {
            return this;
        }
        Map<ResourceLocation, ContainerState> updated = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<ResourceLocation, ContainerState> entry : this.containers.entrySet()) {
            ContainerState next = entry.getValue().clearAbilityState(abilityId);
            changed |= !next.equals(entry.getValue());
            if (!next.isEmpty()) {
                updated.put(entry.getKey(), next);
            }
        }
        return changed ? new AbilityContainerState(updated) : this;
    }

    public AbilityContainerState clearComboOverridesForAbility(ResourceLocation abilityId) {
        return clearAbilityState(abilityId);
    }

    public AbilityContainerState tickComboOverrides() {
        if (this.containers.isEmpty()) {
            return this;
        }
        Map<ResourceLocation, ContainerState> updated = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<ResourceLocation, ContainerState> entry : this.containers.entrySet()) {
            ContainerState next = entry.getValue().tickComboOverrides();
            changed |= !next.equals(entry.getValue());
            if (!next.isEmpty()) {
                updated.put(entry.getKey(), next);
            }
        }
        return changed ? new AbilityContainerState(updated) : this;
    }

    public List<Optional<ResourceLocation>> legacyPrimarySlots(int legacySlotCount) {
        return legacySizedList(primaryState().map(ContainerState::basePage).orElse(List.of()), legacySlotCount);
    }

    public List<Optional<ResourceLocation>> legacyPrimaryComboOverrides(int legacySlotCount) {
        return legacySizedList(primaryState().map(ContainerState::baseComboPage).orElse(List.of()), legacySlotCount);
    }

    public List<Integer> legacyPrimaryComboDurations(int legacySlotCount) {
        return legacySizedDurations(primaryState().map(ContainerState::baseComboDurations).orElse(List.of()), legacySlotCount);
    }

    public Map<ResourceLocation, List<Optional<ResourceLocation>>> legacyPrimaryModeLoadouts(int legacySlotCount) {
        Map<ResourceLocation, List<Optional<ResourceLocation>>> legacyModeLoadouts = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<List<Optional<ResourceLocation>>>> entry : primaryState()
                .map(ContainerState::modeLoadouts)
                .orElse(Map.of())
                .entrySet()) {
            List<Optional<ResourceLocation>> basePage = entry.getValue().isEmpty() ? List.of() : entry.getValue().getFirst();
            legacyModeLoadouts.put(entry.getKey(), legacySizedList(basePage, legacySlotCount));
        }
        return Map.copyOf(legacyModeLoadouts);
    }

    private Optional<ContainerState> containerState(ResourceLocation containerId) {
        return Optional.ofNullable(this.containers.get(containerId));
    }

    private Optional<ContainerState> primaryState() {
        return containerState(AbilitySlotContainerApi.PRIMARY_CONTAINER_ID);
    }

    private AbilityContainerState updateContainer(ResourceLocation containerId, UnaryOperator<ContainerState> updater) {
        ContainerState currentState = this.containers.getOrDefault(containerId, ContainerState.empty());
        ContainerState updatedState = updater.apply(currentState);
        if (updatedState.equals(currentState)) {
            return this;
        }
        Map<ResourceLocation, ContainerState> updated = new LinkedHashMap<>(this.containers);
        if (updatedState.isEmpty()) {
            updated.remove(containerId);
        } else {
            updated.put(containerId, updatedState);
        }
        return new AbilityContainerState(updated);
    }

    private static Map<ResourceLocation, ContainerState> normalizeContainers(Map<ResourceLocation, ContainerState> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, ContainerState> normalized = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, ContainerState> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            normalized.put(entry.getKey(), entry.getValue());
        }
        return Map.copyOf(normalized);
    }

    private static Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> toPagedModeLoadouts(
            Map<ResourceLocation, List<Optional<ResourceLocation>>> modeLoadouts
    ) {
        if (modeLoadouts == null || modeLoadouts.isEmpty()) {
            return Map.of();
        }
        Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> pagedLoadouts = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, List<Optional<ResourceLocation>>> entry : modeLoadouts.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            pagedLoadouts.put(entry.getKey(), List.of(List.copyOf(entry.getValue())));
        }
        return Map.copyOf(pagedLoadouts);
    }

    private static List<Optional<ResourceLocation>> legacySizedList(List<Optional<ResourceLocation>> source, int size) {
        List<Optional<ResourceLocation>> normalized = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            normalized.add(index < source.size() ? source.get(index) : Optional.empty());
        }
        return List.copyOf(normalized);
    }

    private static List<Integer> legacySizedDurations(List<Integer> source, int size) {
        List<Integer> normalized = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            int value = index < source.size() && source.get(index) != null ? source.get(index) : 0;
            normalized.add(Math.max(0, value));
        }
        return List.copyOf(normalized);
    }

    public record ContainerState(
            int activePage,
            List<List<Optional<ResourceLocation>>> pages,
            Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> modeLoadouts,
            List<List<Optional<ResourceLocation>>> comboOverrides,
            List<List<Integer>> comboOverrideDurations
    ) {
        private static final Codec<ContainerState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("active_page", 0).forGetter(ContainerState::activePage),
                OPTIONAL_RESOURCE_PAGES_CODEC.optionalFieldOf("pages", List.of()).forGetter(ContainerState::pages),
                Codec.unboundedMap(ResourceLocation.CODEC, OPTIONAL_RESOURCE_PAGES_CODEC)
                        .optionalFieldOf("mode_loadouts", Map.of())
                        .forGetter(ContainerState::modeLoadouts),
                OPTIONAL_RESOURCE_PAGES_CODEC.optionalFieldOf("combo_overrides", List.of()).forGetter(ContainerState::comboOverrides),
                PAGE_DURATIONS_CODEC.optionalFieldOf("combo_override_durations", List.of()).forGetter(ContainerState::comboOverrideDurations)
        ).apply(instance, ContainerState::new));

        public ContainerState {
            pages = normalizeResourcePages(pages);
            modeLoadouts = normalizeModeLoadouts(modeLoadouts);
            comboOverrides = normalizeResourcePages(comboOverrides);
            comboOverrideDurations = normalizeDurationPages(comboOverrideDurations);
            activePage = Math.max(0, Math.min(activePage, Math.max(0, pageCount(pages, modeLoadouts, comboOverrides, comboOverrideDurations) - 1)));
        }

        public static ContainerState empty() {
            return new ContainerState(0, List.of(), Map.of(), List.of(), List.of());
        }

        public boolean isEmpty() {
            return this.activePage == 0
                    && allPagesEmpty(this.pages)
                    && allPagesEmpty(this.comboOverrides)
                    && allDurationPagesZero(this.comboOverrideDurations)
                    && this.modeLoadouts.isEmpty();
        }

        public int pageCount() {
            return pageCount(this.pages, this.modeLoadouts, this.comboOverrides, this.comboOverrideDurations);
        }

        public int slotCount(int pageIndex) {
            return Math.max(pageAt(this.pages, pageIndex).size(),
                    Math.max(pageAt(this.comboOverrides, pageIndex).size(), durationPageAt(this.comboOverrideDurations, pageIndex).size()));
        }

        public int maxSlotsPerPage() {
            int maxSlots = 0;
            for (int pageIndex = 0; pageIndex < pageCount(); pageIndex++) {
                maxSlots = Math.max(maxSlots, slotCount(pageIndex));
            }
            return maxSlots;
        }

        public Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> modeLoadouts() {
            return this.modeLoadouts;
        }

        public Optional<ResourceLocation> abilityInSlot(int pageIndex, int slotIndex) {
            return optionalValue(this.pages, pageIndex, slotIndex);
        }

        public Optional<ResourceLocation> modeAbilityInSlot(ResourceLocation modeId, int pageIndex, int slotIndex) {
            return optionalValue(this.modeLoadouts.get(modeId), pageIndex, slotIndex);
        }

        public Optional<ResourceLocation> comboOverrideInSlot(int pageIndex, int slotIndex) {
            return optionalValue(this.comboOverrides, pageIndex, slotIndex);
        }

        public int comboOverrideDurationForSlot(int pageIndex, int slotIndex) {
            List<Integer> page = durationPageAt(this.comboOverrideDurations, pageIndex);
            if (slotIndex < 0 || slotIndex >= page.size()) {
                return 0;
            }
            return page.get(slotIndex);
        }

        public ContainerState withAbilityInSlot(int pageIndex, int slotIndex, @Nullable ResourceLocation abilityId) {
            return new ContainerState(
                    this.activePage,
                    updatePage(this.pages, pageIndex, slotIndex, Optional.ofNullable(abilityId)),
                    this.modeLoadouts,
                    this.comboOverrides,
                    this.comboOverrideDurations
            );
        }

        public ContainerState withModeAbilityInSlot(ResourceLocation modeId, int pageIndex, int slotIndex, @Nullable ResourceLocation abilityId) {
            Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> updatedModeLoadouts = new LinkedHashMap<>(this.modeLoadouts);
            List<List<Optional<ResourceLocation>>> updatedPages = updatePage(
                    updatedModeLoadouts.getOrDefault(modeId, List.of()),
                    pageIndex,
                    slotIndex,
                    Optional.ofNullable(abilityId)
            );
            if (allPagesEmpty(updatedPages)) {
                updatedModeLoadouts.remove(modeId);
            } else {
                updatedModeLoadouts.put(modeId, updatedPages);
            }
            return new ContainerState(this.activePage, this.pages, updatedModeLoadouts, this.comboOverrides, this.comboOverrideDurations);
        }

        public ContainerState clearModeLoadout(ResourceLocation modeId) {
            if (!this.modeLoadouts.containsKey(modeId)) {
                return this;
            }
            Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> updatedModeLoadouts = new LinkedHashMap<>(this.modeLoadouts);
            updatedModeLoadouts.remove(modeId);
            return new ContainerState(this.activePage, this.pages, updatedModeLoadouts, this.comboOverrides, this.comboOverrideDurations);
        }

        public ContainerState withComboOverride(int pageIndex, int slotIndex, @Nullable ResourceLocation abilityId, int durationTicks) {
            List<List<Optional<ResourceLocation>>> updatedOverrides = updatePage(
                    this.comboOverrides,
                    pageIndex,
                    slotIndex,
                    durationTicks > 0 && abilityId != null ? Optional.of(abilityId) : Optional.empty()
            );
            List<List<Integer>> updatedDurations = updateDurationPage(
                    this.comboOverrideDurations,
                    pageIndex,
                    slotIndex,
                    Math.max(0, durationTicks)
            );
            return new ContainerState(this.activePage, this.pages, this.modeLoadouts, updatedOverrides, updatedDurations);
        }

        public ContainerState withActivePage(int pageIndex) {
            int clampedPage = Math.max(0, Math.min(pageIndex, Math.max(0, pageCount() - 1)));
            if (clampedPage == this.activePage) {
                return this;
            }
            return new ContainerState(clampedPage, this.pages, this.modeLoadouts, this.comboOverrides, this.comboOverrideDurations);
        }

        public ContainerState clearAbilityState(ResourceLocation abilityId) {
            if (allPagesEmpty(this.comboOverrides)) {
                return this;
            }
            boolean changed = false;
            List<List<Optional<ResourceLocation>>> updatedOverrides = new ArrayList<>();
            List<List<Integer>> updatedDurations = new ArrayList<>();
            int pageCount = Math.max(this.comboOverrides.size(), this.comboOverrideDurations.size());
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                List<Optional<ResourceLocation>> pageOverrides = new ArrayList<>(pageAt(this.comboOverrides, pageIndex));
                List<Integer> pageDurations = new ArrayList<>(durationPageAt(this.comboOverrideDurations, pageIndex));
                int pageWidth = Math.max(pageOverrides.size(), pageDurations.size());
                ensureSize(pageOverrides, pageWidth, Optional.empty());
                ensureSize(pageDurations, pageWidth, 0);
                for (int slotIndex = 0; slotIndex < pageWidth; slotIndex++) {
                    if (pageOverrides.get(slotIndex).filter(abilityId::equals).isPresent()) {
                        pageOverrides.set(slotIndex, Optional.empty());
                        pageDurations.set(slotIndex, 0);
                        changed = true;
                    }
                }
                updatedOverrides.add(List.copyOf(pageOverrides));
                updatedDurations.add(List.copyOf(pageDurations));
            }
            return changed
                    ? new ContainerState(this.activePage, this.pages, this.modeLoadouts, updatedOverrides, updatedDurations)
                    : this;
        }

        public ContainerState tickComboOverrides() {
            if (this.comboOverrides.isEmpty() && this.comboOverrideDurations.isEmpty()) {
                return this;
            }
            boolean changed = false;
            List<List<Optional<ResourceLocation>>> updatedOverrides = new ArrayList<>();
            List<List<Integer>> updatedDurations = new ArrayList<>();
            int pageCount = Math.max(this.comboOverrides.size(), this.comboOverrideDurations.size());
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                List<Optional<ResourceLocation>> pageOverrides = new ArrayList<>(pageAt(this.comboOverrides, pageIndex));
                List<Integer> pageDurations = new ArrayList<>(durationPageAt(this.comboOverrideDurations, pageIndex));
                int pageWidth = Math.max(pageOverrides.size(), pageDurations.size());
                ensureSize(pageOverrides, pageWidth, Optional.empty());
                ensureSize(pageDurations, pageWidth, 0);
                for (int slotIndex = 0; slotIndex < pageWidth; slotIndex++) {
                    int remainingTicks = pageDurations.get(slotIndex);
                    if (remainingTicks <= 0) {
                        if (pageOverrides.get(slotIndex).isPresent()) {
                            pageOverrides.set(slotIndex, Optional.empty());
                            changed = true;
                        }
                        pageDurations.set(slotIndex, 0);
                        continue;
                    }
                    int nextTicks = remainingTicks - 1;
                    pageDurations.set(slotIndex, Math.max(0, nextTicks));
                    if (nextTicks <= 0) {
                        pageOverrides.set(slotIndex, Optional.empty());
                    }
                    changed = true;
                }
                updatedOverrides.add(List.copyOf(pageOverrides));
                updatedDurations.add(List.copyOf(pageDurations));
            }
            return changed
                    ? new ContainerState(this.activePage, this.pages, this.modeLoadouts, updatedOverrides, updatedDurations)
                    : this;
        }

        private List<Optional<ResourceLocation>> basePage() {
            return this.pages.isEmpty() ? List.of() : this.pages.getFirst();
        }

        private List<Optional<ResourceLocation>> baseComboPage() {
            return this.comboOverrides.isEmpty() ? List.of() : this.comboOverrides.getFirst();
        }

        private List<Integer> baseComboDurations() {
            return this.comboOverrideDurations.isEmpty() ? List.of() : this.comboOverrideDurations.getFirst();
        }

        private static int pageCount(
                List<List<Optional<ResourceLocation>>> pages,
                Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> modeLoadouts,
                List<List<Optional<ResourceLocation>>> comboOverrides,
                List<List<Integer>> comboOverrideDurations
        ) {
            int pageCount = Math.max(pages.size(), Math.max(comboOverrides.size(), comboOverrideDurations.size()));
            for (List<List<Optional<ResourceLocation>>> modePages : modeLoadouts.values()) {
                pageCount = Math.max(pageCount, modePages.size());
            }
            return pageCount;
        }

        private static List<List<Optional<ResourceLocation>>> updatePage(
                List<List<Optional<ResourceLocation>>> sourcePages,
                int pageIndex,
                int slotIndex,
                Optional<ResourceLocation> value
        ) {
            List<List<Optional<ResourceLocation>>> updatedPages = new ArrayList<>(sourcePages);
            ensurePage(updatedPages, pageIndex);
            List<Optional<ResourceLocation>> page = new ArrayList<>(updatedPages.get(pageIndex));
            ensureSize(page, slotIndex + 1, Optional.empty());
            page.set(slotIndex, value);
            updatedPages.set(pageIndex, List.copyOf(page));
            return List.copyOf(updatedPages);
        }

        private static List<List<Integer>> updateDurationPage(
                List<List<Integer>> sourcePages,
                int pageIndex,
                int slotIndex,
                int value
        ) {
            List<List<Integer>> updatedPages = new ArrayList<>(sourcePages);
            ensureDurationPage(updatedPages, pageIndex);
            List<Integer> page = new ArrayList<>(updatedPages.get(pageIndex));
            ensureSize(page, slotIndex + 1, 0);
            page.set(slotIndex, Math.max(0, value));
            updatedPages.set(pageIndex, List.copyOf(page));
            return List.copyOf(updatedPages);
        }

        private static List<List<Optional<ResourceLocation>>> normalizeResourcePages(List<List<Optional<ResourceLocation>>> sourcePages) {
            if (sourcePages == null || sourcePages.isEmpty()) {
                return List.of();
            }
            List<List<Optional<ResourceLocation>>> normalized = new ArrayList<>();
            for (List<Optional<ResourceLocation>> page : sourcePages) {
                if (page == null) {
                    normalized.add(List.of());
                    continue;
                }
                List<Optional<ResourceLocation>> normalizedPage = new ArrayList<>(page.size());
                for (Optional<ResourceLocation> value : page) {
                    normalizedPage.add(value == null ? Optional.empty() : value);
                }
                normalized.add(List.copyOf(normalizedPage));
            }
            trimTrailingEmptyPages(normalized);
            return List.copyOf(normalized);
        }

        private static Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> normalizeModeLoadouts(
                Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> source
        ) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }
            Map<ResourceLocation, List<List<Optional<ResourceLocation>>>> normalized = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, List<List<Optional<ResourceLocation>>>> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                List<List<Optional<ResourceLocation>>> normalizedPages = normalizeResourcePages(entry.getValue());
                if (!allPagesEmpty(normalizedPages)) {
                    normalized.put(entry.getKey(), normalizedPages);
                }
            }
            return Map.copyOf(normalized);
        }

        private static List<List<Integer>> normalizeDurationPages(List<List<Integer>> sourcePages) {
            if (sourcePages == null || sourcePages.isEmpty()) {
                return List.of();
            }
            List<List<Integer>> normalized = new ArrayList<>();
            for (List<Integer> page : sourcePages) {
                if (page == null) {
                    normalized.add(List.of());
                    continue;
                }
                List<Integer> normalizedPage = new ArrayList<>(page.size());
                for (Integer value : page) {
                    normalizedPage.add(value == null ? 0 : Math.max(0, value));
                }
                normalized.add(List.copyOf(normalizedPage));
            }
            trimTrailingZeroPages(normalized);
            return List.copyOf(normalized);
        }

        private static Optional<ResourceLocation> optionalValue(List<List<Optional<ResourceLocation>>> pages, int pageIndex, int slotIndex) {
            List<Optional<ResourceLocation>> page = pageAt(pages, pageIndex);
            if (slotIndex < 0 || slotIndex >= page.size()) {
                return Optional.empty();
            }
            return page.get(slotIndex);
        }

        private static List<Optional<ResourceLocation>> pageAt(List<List<Optional<ResourceLocation>>> pages, int pageIndex) {
            if (pages == null || pageIndex < 0 || pageIndex >= pages.size()) {
                return List.of();
            }
            return pages.get(pageIndex);
        }

        private static List<Integer> durationPageAt(List<List<Integer>> pages, int pageIndex) {
            if (pages == null || pageIndex < 0 || pageIndex >= pages.size()) {
                return List.of();
            }
            return pages.get(pageIndex);
        }

        private static void ensurePage(List<List<Optional<ResourceLocation>>> pages, int pageIndex) {
            while (pages.size() <= pageIndex) {
                pages.add(List.of());
            }
        }

        private static void ensureDurationPage(List<List<Integer>> pages, int pageIndex) {
            while (pages.size() <= pageIndex) {
                pages.add(List.of());
            }
        }

        private static <T> void ensureSize(List<T> values, int targetSize, T fillValue) {
            while (values.size() < targetSize) {
                values.add(fillValue);
            }
        }

        private static boolean allPagesEmpty(List<List<Optional<ResourceLocation>>> pages) {
            for (List<Optional<ResourceLocation>> page : pages) {
                if (page.stream().anyMatch(Optional::isPresent)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean allDurationPagesZero(List<List<Integer>> pages) {
            for (List<Integer> page : pages) {
                for (Integer value : page) {
                    if (value != null && value > 0) {
                        return false;
                    }
                }
            }
            return true;
        }

        private static void trimTrailingEmptyPages(List<List<Optional<ResourceLocation>>> pages) {
            for (int index = pages.size() - 1; index >= 0; index--) {
                if (pages.get(index).stream().anyMatch(Optional::isPresent)) {
                    break;
                }
                pages.remove(index);
            }
        }

        private static void trimTrailingZeroPages(List<List<Integer>> pages) {
            for (int index = pages.size() - 1; index >= 0; index--) {
                boolean nonZero = false;
                for (Integer value : pages.get(index)) {
                    if (value != null && value > 0) {
                        nonZero = true;
                        break;
                    }
                }
                if (nonZero) {
                    break;
                }
                pages.remove(index);
            }
        }
    }
}
