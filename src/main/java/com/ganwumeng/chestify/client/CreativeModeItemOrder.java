package com.ganwumeng.chestify.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class CreativeModeItemOrder {
    private static final int UNKNOWN_ORDER = Integer.MAX_VALUE;

    private static Map<Item, Integer> itemOrder;

    private CreativeModeItemOrder() {
    }

    public static int indexOf(Minecraft client, ItemStack stack) {
        if (stack.isEmpty()) {
            return UNKNOWN_ORDER;
        }

        Map<Item, Integer> order = itemOrder;
        if (order == null || order.isEmpty()) {
            order = buildOrder(client);
            itemOrder = order;
        }

        return order.getOrDefault(stack.getItem(), UNKNOWN_ORDER);
    }

    private static Map<Item, Integer> buildOrder(Minecraft client) {
        Map<Item, Integer> order = collectFromTabs(CreativeModeTabs.tabs());
        if (!order.isEmpty()) {
            return order;
        }

        ClientPacketListener connection = client.getConnection();
        if (connection != null) {
            CreativeModeTabs.tryRebuildTabContents(connection.enabledFeatures(), false, connection.registryAccess());
        }

        order = collectFromTabs(CreativeModeTabs.tabs());
        if (!order.isEmpty()) {
            return order;
        }

        return collectFromTabs(CreativeModeTabs.allTabs());
    }

    private static Map<Item, Integer> collectFromTabs(List<CreativeModeTab> tabs) {
        Map<Item, Integer> order = new IdentityHashMap<>();
        int index = 0;

        for (CreativeModeTab tab : tabs) {
            for (ItemStack stack : tab.getDisplayItems()) {
                if (!stack.isEmpty() && !order.containsKey(stack.getItem())) {
                    order.put(stack.getItem(), index);
                }
                index++;
            }
        }

        return order;
    }
}
