package com.whatxe.xlib.client;

import com.whatxe.xlib.ability.AbilityControlAction;
import com.whatxe.xlib.ability.AbilityControlActionType;
import com.whatxe.xlib.ability.AbilityData;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class AbilityControlActionHandlerApi {
    private static final Map<AbilityControlActionType, AbilityControlActionHandler> HANDLERS = new LinkedHashMap<>();

    private AbilityControlActionHandlerApi() {}

    public static void bootstrap() {
        HANDLERS.putIfAbsent(AbilityControlActionType.ACTIVATE_SLOT, (minecraft, data, action) -> {
            if (!com.whatxe.xlib.ability.AbilitySlotContainerApi.PRIMARY_CONTAINER_ID.equals(action.containerId())
                    || action.slotIndex() < 0
                    || action.slotIndex() >= AbilityData.SLOT_COUNT) {
                return false;
            }
            PacketDistributor.sendToServer(new com.whatxe.xlib.network.ActivateAbilityPayload(
                    new com.whatxe.xlib.ability.AbilitySlotReference(action.containerId(), data.activeContainerPage(action.containerId()), action.slotIndex())
            ));
            AbilityClientState.flashSlot(new com.whatxe.xlib.ability.AbilitySlotReference(
                    action.containerId(),
                    data.activeContainerPage(action.containerId()),
                    action.slotIndex()
            ));
            return true;
        });
        HANDLERS.putIfAbsent(AbilityControlActionType.NEXT_PAGE, (minecraft, data, action) -> false);
        HANDLERS.putIfAbsent(AbilityControlActionType.PREVIOUS_PAGE, (minecraft, data, action) -> false);
        HANDLERS.putIfAbsent(AbilityControlActionType.SET_PAGE, (minecraft, data, action) -> false);
    }

    public static void register(AbilityControlActionType actionType, AbilityControlActionHandler handler) {
        HANDLERS.put(Objects.requireNonNull(actionType, "actionType"), Objects.requireNonNull(handler, "handler"));
    }

    public static boolean handle(Minecraft minecraft, AbilityData data, AbilityControlAction action) {
        AbilityControlActionHandler handler = HANDLERS.get(action.type());
        return handler != null && handler.handle(minecraft, data, action);
    }
}
