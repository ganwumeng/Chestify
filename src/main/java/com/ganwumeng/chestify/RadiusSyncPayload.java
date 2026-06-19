package com.ganwumeng.chestify;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RadiusSyncPayload(int radius, boolean canEdit) implements CustomPacketPayload {
    public static final Type<RadiusSyncPayload> ID = new Type<>(Chestify.id("radius_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadiusSyncPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, RadiusSyncPayload::radius,
                    ByteBufCodecs.BOOL, RadiusSyncPayload::canEdit,
                    RadiusSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
