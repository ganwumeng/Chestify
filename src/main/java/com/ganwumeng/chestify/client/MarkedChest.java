package com.ganwumeng.chestify.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public record MarkedChest(BlockPos pos, ItemStack icon) {
}
