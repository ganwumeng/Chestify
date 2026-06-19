package com.ganwumeng.chestify.client;

import com.ganwumeng.chestify.Chestify;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MarkedChestScanner {
    private static final long RESCAN_INTERVAL_MILLIS = 500L;

    private long lastScanMillis;
    private List<MarkedChest> cached = List.of();

    public List<MarkedChest> getMarkedChests(Minecraft client) {
        long now = System.currentTimeMillis();
        if (now - lastScanMillis >= RESCAN_INTERVAL_MILLIS) {
            lastScanMillis = now;
            cached = scan(client);
        }
        return cached;
    }

    private static List<MarkedChest> scan(Minecraft client) {
        ClientLevel world = client.level;
        Player player = client.player;
        if (world == null || player == null) {
            return List.of();
        }

        double radius = Chestify.searchRadius();
        AABB box = AABB.ofSize(player.position(), radius * 2.0D, radius * 2.0D, radius * 2.0D);
        List<ItemFrame> frames = world.getEntities(EntityTypeTest.forClass(ItemFrame.class), box, frame ->
                frame.distanceToSqr(player) <= radius * radius && !frame.getItem().isEmpty());

        Set<BlockPos> seen = new HashSet<>();
        List<MarkedChest> result = new ArrayList<>();

        for (ItemFrame frame : frames) {
            BlockPos chestPos = Chestify.findMarkedChestPos(world, frame);
            if (chestPos == null || !seen.add(chestPos)) {
                continue;
            }

            ItemStack icon = frame.getItem().copy();
            icon.setCount(1);
            result.add(new MarkedChest(chestPos.immutable(), icon));
        }

        result.sort(Comparator
                .comparingInt((MarkedChest chest) -> CreativeModeItemOrder.indexOf(client, chest.icon()))
                .thenComparingDouble(chest -> player.position().distanceToSqr(chest.pos().getCenter()))
                .thenComparingInt(chest -> chest.pos().getY())
                .thenComparingInt(chest -> chest.pos().getX())
                .thenComparingInt(chest -> chest.pos().getZ()));
        return List.copyOf(result);
    }
}
