package com.whatxe.xlib.ability;

import java.util.Collection;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class AbilitySoundPlayer {
    private AbilitySoundPlayer() {}

    public static void play(ServerPlayer player, AbilitySound sound) {
        SoundEvent resolvedSound = BuiltInRegistries.SOUND_EVENT.getOptional(sound.soundId()).orElse(null);
        if (resolvedSound == null) {
            return;
        }

        player.serverLevel().playSound(
                null,
                player.blockPosition(),
                resolvedSound,
                SoundSource.PLAYERS,
                sound.volume(),
                sound.pitch()
        );
    }

    public static void playAll(ServerPlayer player, Collection<AbilitySound> sounds) {
        for (AbilitySound sound : sounds) {
            play(player, sound);
        }
    }
}

