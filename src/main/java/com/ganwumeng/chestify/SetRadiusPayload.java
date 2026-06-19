package com.ganwumeng.chestify;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetRadiusPayload(int radius) implements CustomPacketPayload {
    public static final Type<SetRadiusPayload> ID = new Type<>(Chestify.id("set_radius"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetRadiusPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SetRadiusPayload::radius, SetRadiusPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
