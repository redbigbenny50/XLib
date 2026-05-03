package com.whatxe.xlib.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record VisualFormState(
        ResourceLocation activeFormId,
        ResourceLocation sourceId,
        long startedGameTime
) {
    public static final Codec<VisualFormState> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("active_form_id").forGetter(VisualFormState::activeFormId),
                    ResourceLocation.CODEC.fieldOf("source_id").forGetter(VisualFormState::sourceId),
                    Codec.LONG.fieldOf("started_game_time").forGetter(VisualFormState::startedGameTime)
            ).apply(instance, VisualFormState::new)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, VisualFormState> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
