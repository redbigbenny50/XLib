package com.whatxe.xlib.combat;

import com.whatxe.xlib.XLibRegistryGuard;
import com.whatxe.xlib.api.event.XLibCombatMarkEvent;
import com.whatxe.xlib.attachment.ModAttachments;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

public final class CombatMarkApi {
    private static final Map<ResourceLocation, CombatMarkDefinition> MARKS = new LinkedHashMap<>();

    private CombatMarkApi() {}

    public static void bootstrap() {}

    public static CombatMarkDefinition registerMark(CombatMarkDefinition definition) {
        XLibRegistryGuard.ensureMutable("combat_marks");
        CombatMarkDefinition previous = MARKS.putIfAbsent(definition.id(), definition);
        if (previous != null) {
            throw new IllegalStateException("Duplicate combat mark registration: " + definition.id());
        }
        return definition;
    }

    public static Optional<CombatMarkDefinition> unregisterMark(ResourceLocation markId) {
        XLibRegistryGuard.ensureMutable("combat_marks");
        return Optional.ofNullable(MARKS.remove(markId));
    }

    public static Optional<CombatMarkDefinition> findMark(ResourceLocation markId) {
        return Optional.ofNullable(MARKS.get(markId));
    }

    public static Collection<CombatMarkDefinition> allMarks() {
        return List.copyOf(MARKS.values());
    }

    public static boolean has(LivingEntity entity, ResourceLocation markId) {
        return ModAttachments.getMarks(entity).has(markId);
    }

    public static Optional<CombatMarkState> state(LivingEntity entity, ResourceLocation markId) {
        return Optional.ofNullable(ModAttachments.getMarks(entity).state(markId));
    }

    public static boolean apply(LivingEntity entity, ResourceLocation markId, int durationTicks) {
        return apply(entity, markId, durationTicks, 1, 0.0D, null);
    }

    public static boolean apply(
            LivingEntity entity,
            ResourceLocation markId,
            int durationTicks,
            int stacks,
            double value,
            @Nullable ResourceLocation sourceId
    ) {
        CombatMarkDefinition definition = findMark(markId).orElse(null);
        if (definition == null || durationTicks <= 0) {
            return false;
        }

        CombatMarkData currentData = ModAttachments.getMarks(entity);
        CombatMarkState previousState = currentData.state(markId);
        int nextStacks = previousState == null
                ? Math.min(definition.maxStacks(), Math.max(1, stacks))
                : Math.min(definition.maxStacks(), previousState.stacks() + Math.max(1, stacks));
        int nextDuration = previousState == null || definition.refreshDurationOnReapply()
                ? Math.max(durationTicks, previousState != null ? previousState.durationTicks() : 0)
                : previousState.durationTicks();
        double nextValue = previousState == null
                ? value
                : (definition.addValueOnReapply() ? previousState.value() + value : value);
        CombatMarkState nextState = new CombatMarkState(nextDuration, nextStacks, nextValue, sourceId != null ? sourceId : previousState != null ? previousState.sourceId() : null);
        CombatMarkData updatedData = currentData.withMark(markId, nextState);
        if (updatedData.equals(currentData)) {
            return false;
        }
        ModAttachments.setMarks(entity, updatedData);
        NeoForge.EVENT_BUS.post(previousState == null
                ? new XLibCombatMarkEvent.Applied(entity, markId, null, nextState)
                : new XLibCombatMarkEvent.Refreshed(entity, markId, previousState, nextState));
        return true;
    }

    public static boolean remove(LivingEntity entity, ResourceLocation markId) {
        CombatMarkData currentData = ModAttachments.getMarks(entity);
        CombatMarkState previousState = currentData.state(markId);
        if (previousState == null) {
            return false;
        }
        CombatMarkData updatedData = currentData.withoutMark(markId);
        ModAttachments.setMarks(entity, updatedData);
        NeoForge.EVENT_BUS.post(new XLibCombatMarkEvent.Removed(entity, markId, previousState, null));
        return true;
    }

    public static void clear(LivingEntity entity) {
        CombatMarkData currentData = ModAttachments.getMarks(entity);
        if (currentData.marks().isEmpty()) {
            return;
        }
        for (Map.Entry<ResourceLocation, CombatMarkState> entry : currentData.marks().entrySet()) {
            NeoForge.EVENT_BUS.post(new XLibCombatMarkEvent.Removed(entity, entry.getKey(), entry.getValue(), null));
        }
        ModAttachments.setMarks(entity, CombatMarkData.empty());
    }

    public static CombatMarkData tick(LivingEntity entity, CombatMarkData currentData) {
        CombatMarkData updatedData = currentData;
        for (Map.Entry<ResourceLocation, CombatMarkState> entry : currentData.marks().entrySet()) {
            CombatMarkState nextState = entry.getValue().ticked();
            if (nextState.durationTicks() <= 0) {
                updatedData = updatedData.withoutMark(entry.getKey());
                NeoForge.EVENT_BUS.post(new XLibCombatMarkEvent.Expired(entity, entry.getKey(), entry.getValue(), null));
            } else {
                updatedData = updatedData.withMark(entry.getKey(), nextState);
            }
        }
        return sanitize(updatedData);
    }

    public static CombatMarkData sanitize(CombatMarkData data) {
        CombatMarkData updatedData = CombatMarkData.empty();
        for (Map.Entry<ResourceLocation, CombatMarkState> entry : data.marks().entrySet()) {
            if (MARKS.containsKey(entry.getKey())) {
                updatedData = updatedData.withMark(entry.getKey(), entry.getValue());
            }
        }
        return updatedData;
    }
}
