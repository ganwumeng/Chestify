package com.ganwumeng.chestify.client;

import com.ganwumeng.chestify.OpenChestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChestifyOverlay {
    private static final int COLUMNS = 8;
    private static final int ROWS = 10;
    private static final int ITEMS_PER_PAGE = COLUMNS * ROWS;
    private static final int CELL = 18;
    private static final int PADDING = 6;
    private static final int HEADER_HEIGHT = 16;
    private static final int SEARCH_HEIGHT = 22;
    private static final int PANEL_WIDTH = COLUMNS * CELL + PADDING * 2;
    private static final int PANEL_HEIGHT = HEADER_HEIGHT + SEARCH_HEIGHT + ROWS * CELL + PADDING * 2;
    private static final double CENTER_WARP_RADIUS = 18.0D;
    private static final double SAME_POINT_RADIUS = 6.0D;

    private static final MarkedChestScanner SCANNER = new MarkedChestScanner();
    private static int page;
    private static double restoreMouseX;
    private static double restoreMouseY;
    private static long restoreMouseUntilMillis;
    private static EditBox searchBox;

    private ChestifyOverlay() {
    }

    public static void extract(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        restoreMousePosition(client);

        layoutSearchBox(screenWidth, screenHeight);

        List<MarkedChest> chests = filterChests(SCANNER.getMarkedChests(client));
        int totalPages = Math.max(1, (chests.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = clamp(page, 0, totalPages - 1);

        int x = 4;
        int y = Math.max(4, (screenHeight - PANEL_HEIGHT) / 2);

        graphics.fill(x, y, x + PANEL_WIDTH, y + HEADER_HEIGHT + PADDING + 2, 0x66101010);

        drawPageButton(graphics, x + PADDING, y + PADDING - 1, "<", page > 0);
        drawPageButton(graphics, x + PANEL_WIDTH - PADDING - 14, y + PADDING - 1, ">", page < totalPages - 1);
        drawPageButton(graphics, x + PANEL_WIDTH - PADDING - 32, y + PADDING - 1, "S", true);

        String pageText = (page + 1) + "/" + totalPages;
        int pageTextX = x + (PANEL_WIDTH - client.font.width(pageText)) / 2;
        graphics.text(client.font, pageText, pageTextX, y + PADDING + 3, 0xFFE0E0E0, false);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(chests.size(), start + ITEMS_PER_PAGE);
        ItemStack hoveredIcon = ItemStack.EMPTY;
        for (int i = start; i < end; i++) {
            int local = i - start;
            int cellX = x + PADDING + (local % COLUMNS) * CELL;
            int cellY = y + PADDING + HEADER_HEIGHT + SEARCH_HEIGHT + (local / COLUMNS) * CELL;
            MarkedChest chest = chests.get(i);

            boolean hovered = mouseX >= cellX && mouseX < cellX + CELL && mouseY >= cellY && mouseY < cellY + CELL;
            if (hovered) {
                graphics.fill(cellX, cellY, cellX + CELL, cellY + CELL, 0x55FFFFFF);
                graphics.outline(cellX, cellY, CELL, CELL, 0xCCFFFFFF);
            }
            ItemStack icon = chest.icon();
            graphics.item(icon, cellX + 1, cellY + 1);
            graphics.itemDecorations(client.font, icon, cellX + 1, cellY + 1);

            if (hovered) {
                hoveredIcon = icon;
            }
        }

        if (chests.isEmpty()) {
            Component empty = Component.literal(isSearching() ? "No matches" : "No marked chests");
            int emptyX = x + (PANEL_WIDTH - client.font.width(empty)) / 2;
            graphics.text(client.font, empty, emptyX, y + PADDING + HEADER_HEIGHT + SEARCH_HEIGHT + 6, 0xFFB0B0B0, false);
        }

        if (!hoveredIcon.isEmpty()) {
            graphics.nextStratum();
            drawItemTooltip(graphics, client, hoveredIcon, mouseX, mouseY);
        }
    }

    public static boolean mouseClicked(int screenWidth, int screenHeight, double mouseX, double mouseY, int button) {
        if (button != 0) {
            return false;
        }

        Minecraft client = Minecraft.getInstance();
        List<MarkedChest> chests = filterChests(SCANNER.getMarkedChests(client));
        int totalPages = Math.max(1, (chests.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        page = clamp(page, 0, totalPages - 1);

        int x = 4;
        int y = Math.max(4, (screenHeight - PANEL_HEIGHT) / 2);

        if (isInside(mouseX, mouseY, x + PADDING, y + PADDING - 1, 14, 14)) {
            if (page > 0) {
                page--;
            }
            return true;
        }

        if (isInside(mouseX, mouseY, x + PANEL_WIDTH - PADDING - 14, y + PADDING - 1, 14, 14)) {
            if (page < totalPages - 1) {
                page++;
            }
            return true;
        }

        if (isInside(mouseX, mouseY, x + PANEL_WIDTH - PADDING - 32, y + PADDING - 1, 14, 14)) {
            Minecraft.getInstance().setScreen(new ChestifyConfigScreen(Minecraft.getInstance().screen));
            return true;
        }

        int searchX = x + PADDING;
        int searchY = y + PADDING + HEADER_HEIGHT + 2;
        if (isInside(mouseX, mouseY, searchX, searchY, PANEL_WIDTH - PADDING * 2, 16)) {
            return false;
        }

        if (searchBox != null) {
            searchBox.setFocused(false);
        }

        int gridX = x + PADDING;
        int gridY = y + PADDING + HEADER_HEIGHT + SEARCH_HEIGHT;
        if (!isInside(mouseX, mouseY, gridX, gridY, COLUMNS * CELL, ROWS * CELL)) {
            return false;
        }

        int col = ((int) mouseX - gridX) / CELL;
        int row = ((int) mouseY - gridY) / CELL;
        int index = page * ITEMS_PER_PAGE + row * COLUMNS + col;
        if (index >= 0 && index < chests.size()) {
            rememberMousePosition(mouseX, mouseY);
            ClientPlayNetworking.send(new OpenChestPayload(chests.get(index).pos()));
            return true;
        }

        return false;
    }

    public static void onContainerScreenOpened() {
        if (System.currentTimeMillis() < restoreMouseUntilMillis) {
            restoreMouseUntilMillis = System.currentTimeMillis() + 1200L;
        }
    }

    public static EditBox createSearchBox(Minecraft client, int screenWidth, int screenHeight) {
        searchBox = new EditBox(client.font, 0, 0, 0, 16, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setHint(Component.literal("Search"));
        layoutSearchBox(screenWidth, screenHeight);
        return searchBox;
    }

    public static void layoutSearchBox(int screenWidth, int screenHeight) {
        if (searchBox == null) {
            return;
        }

        int x = 4;
        int y = Math.max(4, (screenHeight - PANEL_HEIGHT) / 2);
        searchBox.setSize(PANEL_WIDTH - PADDING * 2, 16);
        searchBox.setX(x + PADDING);
        searchBox.setY(y + PADDING + HEADER_HEIGHT + 2);
        searchBox.visible = true;
        searchBox.active = true;
    }

    private static void drawPageButton(GuiGraphicsExtractor graphics, int x, int y, String label, boolean enabled) {
        Minecraft client = Minecraft.getInstance();
        int top = enabled ? 0xFFE0E0E0 : 0xFF707070;
        int bottom = enabled ? 0xFF8A8A8A : 0xFF3A3A3A;
        graphics.fillGradient(x, y, x + 14, y + 14, top, bottom);
        graphics.outline(x, y, 14, 14, enabled ? 0xFF000000 : 0xFF202020);
        graphics.fill(x + 1, y + 1, x + 13, y + 2, enabled ? 0x66FFFFFF : 0x33333333);
        int color = enabled ? 0xFFFFFFFF : 0xFF777777;
        int textX = x + (14 - client.font.width(label)) / 2;
        graphics.text(client.font, label, textX, y + 3, color, true);
    }

    private static boolean isInside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static List<MarkedChest> filterChests(List<MarkedChest> chests) {
        String query = searchText();
        if (query.isEmpty()) {
            return chests;
        }

        List<MarkedChest> filtered = new ArrayList<>();
        for (MarkedChest chest : chests) {
            if (matches(chest.icon(), query)) {
                filtered.add(chest);
            }
        }
        return filtered;
    }

    private static boolean matches(ItemStack stack, String query) {
        String displayName = normalize(stack.getHoverName().getString());
        String itemId = normalize(String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem())));
        for (String token : query.split("\\s+")) {
            if (!displayName.contains(token) && !itemId.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSearching() {
        return !searchText().isEmpty();
    }

    private static String searchText() {
        return searchBox == null ? "" : normalize(searchBox.getValue());
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static void drawItemTooltip(GuiGraphicsExtractor graphics, Minecraft client, ItemStack icon, int mouseX, int mouseY) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        for (Component line : Screen.getTooltipFromItem(client, icon)) {
            tooltip.add(ClientTooltipComponent.create(line.getVisualOrderText()));
        }
        icon.getTooltipImage().ifPresent(image -> tooltip.add(Math.min(1, tooltip.size()), ClientTooltipComponent.create(image)));
        graphics.tooltip(client.font, tooltip, mouseX, mouseY, ChestifyOverlay::positionTooltipAwayFromGrid, null);
    }

    private static Vector2i positionTooltipAwayFromGrid(int screenWidth, int screenHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight) {
        int x = mouseX + 14;
        if (x + tooltipWidth + 4 > screenWidth) {
            x = mouseX - tooltipWidth - 14;
        }

        int y = mouseY - tooltipHeight - 10;
        if (y < 4) {
            y = mouseY + 14;
        }

        x = clamp(x, 4, Math.max(4, screenWidth - tooltipWidth - 4));
        y = clamp(y, 4, Math.max(4, screenHeight - tooltipHeight - 4));
        return new Vector2i(x, y);
    }

    private static void rememberMousePosition(double mouseX, double mouseY) {
        restoreMouseX = mouseX;
        restoreMouseY = mouseY;
        restoreMouseUntilMillis = System.currentTimeMillis() + 3500L;
    }

    private static void restoreMousePosition(Minecraft client) {
        if (System.currentTimeMillis() >= restoreMouseUntilMillis) {
            return;
        }

        double rawX = restoreMouseX * client.getWindow().getWidth() / (double) client.getWindow().getGuiScaledWidth();
        double rawY = restoreMouseY * client.getWindow().getHeight() / (double) client.getWindow().getGuiScaledHeight();
        double cursorX = client.mouseHandler.xpos();
        double cursorY = client.mouseHandler.ypos();
        double centerX = client.getWindow().getWidth() / 2.0D;
        double centerY = client.getWindow().getHeight() / 2.0D;

        if (isNear(cursorX, cursorY, rawX, rawY, SAME_POINT_RADIUS)) {
            return;
        }

        if (!isNear(cursorX, cursorY, centerX, centerY, CENTER_WARP_RADIUS)
                || isNear(rawX, rawY, centerX, centerY, CENTER_WARP_RADIUS)) {
            restoreMouseUntilMillis = 0L;
            return;
        }

        GLFW.glfwSetCursorPos(client.getWindow().handle(), rawX, rawY);
        client.mouseHandler.setIgnoreFirstMove();
        restoreMouseUntilMillis = 0L;
    }

    private static boolean isNear(double x, double y, double targetX, double targetY, double radius) {
        double dx = x - targetX;
        double dy = y - targetY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
