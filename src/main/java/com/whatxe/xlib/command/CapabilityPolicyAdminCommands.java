package com.whatxe.xlib.command;

import com.whatxe.xlib.capability.CapabilityCheck;
import com.whatxe.xlib.capability.CapabilityPolicyApi;
import com.whatxe.xlib.capability.CapabilityPolicyData;
import com.whatxe.xlib.capability.CapabilityPolicyDefinition;
import com.whatxe.xlib.capability.ResolvedCapabilityPolicyState;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

final class CapabilityPolicyAdminCommands {
    private CapabilityPolicyAdminCommands() {}

    static int apply(CommandSourceStack source, ServerPlayer target, ResourceLocation policyId) {
        if (CapabilityPolicyApi.find(policyId).isEmpty()) {
            source.sendFailure(Component.literal("Unknown capability policy: " + policyId));
            return 0;
        }
        CapabilityPolicyApi.apply(target, policyId, CapabilityPolicyApi.COMMAND_SOURCE);
        source.sendSuccess(() -> Component.literal("Applied policy " + policyId + " to " + target.getName().getString()), true);
        return 1;
    }

    static int revoke(CommandSourceStack source, ServerPlayer target, ResourceLocation policyId) {
        CapabilityPolicyApi.revoke(target, policyId, CapabilityPolicyApi.COMMAND_SOURCE);
        source.sendSuccess(() -> Component.literal("Revoked policy " + policyId + " from " + target.getName().getString()), true);
        return 1;
    }

    static int clear(CommandSourceStack source, ServerPlayer target) {
        CapabilityPolicyApi.clearAll(target);
        source.sendSuccess(() -> Component.literal("Cleared all capability policies from " + target.getName().getString()), true);
        return 1;
    }

    static int list(CommandSourceStack source, ServerPlayer target) {
        List<CapabilityPolicyDefinition> active = CapabilityPolicyApi.activePolicies(target);
        if (active.isEmpty()) {
            source.sendSuccess(() -> Component.literal(target.getName().getString() + " has no active capability policies."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Active capability policies for " + target.getName().getString() + ":"), false);
        CapabilityPolicyData data = CapabilityPolicyApi.getData(target);
        for (CapabilityPolicyDefinition policy : active) {
            String sources = data.sourcesFor(policy.id()).toString();
            source.sendSuccess(() -> Component.literal("  " + policy.id() + " (sources: " + sources + ")"), false);
        }
        return active.size();
    }

    static int debug(CommandSourceStack source, ServerPlayer target) {
        ResolvedCapabilityPolicyState resolved = CapabilityPolicyApi.resolved(target);
        source.sendSuccess(() -> Component.literal("Resolved capability state for " + target.getName().getString() + ":"), false);
        for (CapabilityCheck check : CapabilityCheck.values()) {
            boolean allowed = resolved.allows(check);
            if (!allowed) {
                source.sendSuccess(() -> Component.literal("  BLOCKED: " + check.name()), false);
            }
        }
        long blockedCount = java.util.Arrays.stream(CapabilityCheck.values())
                .filter(c -> !resolved.allows(c))
                .count();
        if (blockedCount == 0) {
            source.sendSuccess(() -> Component.literal("  All capabilities permitted."), false);
        }
        return 1;
    }
}
