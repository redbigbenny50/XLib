package com.whatxe.xlib.ability;

import java.util.Objects;
import java.util.OptionalInt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;

public final class AbilityScoreboardApi {
    private AbilityScoreboardApi() {}

    public static OptionalInt readScore(Player player, String objectiveName) {
        if (player == null) {
            return OptionalInt.empty();
        }
        String resolvedObjectiveName = Objects.requireNonNull(objectiveName, "objectiveName").trim();
        if (resolvedObjectiveName.isEmpty()) {
            return OptionalInt.empty();
        }
        Objective objective = player.getScoreboard().getObjective(resolvedObjectiveName);
        if (objective == null) {
            return OptionalInt.empty();
        }
        ReadOnlyScoreInfo scoreInfo = player.getScoreboard().getPlayerScoreInfo(player, objective);
        if (scoreInfo == null) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(scoreInfo.value());
    }
}
