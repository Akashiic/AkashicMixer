package com.akashiic.mixer.mixins.common;

import net.minecraft.network.play.server.S29PacketSoundEffect;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The sound packet's fields; its getters are client-only ({@code @SideOnly}), stripped on a dedicated server. */
@Mixin(S29PacketSoundEffect.class)
public interface AccessorS29PacketSoundEffect {

    @Accessor("field_149219_a")
    String akashicmixer$getName();

    /** x times 8. */
    @Accessor("field_149217_b")
    int akashicmixer$getX();

    /** y times 8. */
    @Accessor("field_149218_c")
    int akashicmixer$getY();

    /** z times 8. */
    @Accessor("field_149215_d")
    int akashicmixer$getZ();

    @Accessor("field_149216_e")
    float akashicmixer$getVolume();

    /** Pitch times 63, 0..255. */
    @Accessor("field_149214_f")
    int akashicmixer$getPitch();
}
