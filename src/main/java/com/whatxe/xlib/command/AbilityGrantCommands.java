package com.whatxe.xlib.command;

import com.mojang.brigadier.CommandDispatcher;
import com.whatxe.xlib.XLib;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AbilityGrantCommands {
    private AbilityGrantCommands() {}

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        registerDispatcher(event.getDispatcher());
    }

    private static void registerDispatcher(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(XLib.MODID)
                .requires(source -> source.hasPermission(2))
                .then(AbilityCommandTree.build())
                .then(PassiveCommandTree.build())
                .then(GrantedItemCommandTree.build())
                .then(RecipeCommandTree.build())
                .then(ProgressionCommandTree.build())
                .then(ProfileCommandTree.build())
                .then(CapabilityPolicyCommandTree.build())
                .then(EntityClassificationCommandTree.build())
                .then(EntityBindingCommandTree.build())
                .then(LifecycleStageCommandTree.build())
                .then(VisualFormCommandTree.build())
                .then(BodyTransitionCommandTree.build())
                .then(DebugCommandTree.build()));
    }
}
