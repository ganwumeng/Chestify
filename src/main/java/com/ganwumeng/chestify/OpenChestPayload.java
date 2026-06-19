package com.ganwumeng.chestify;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenChestPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenChestPayload> ID = new Type<>(Chestify.id("open_chest"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenChestPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, OpenChestPayload::pos, OpenChestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
