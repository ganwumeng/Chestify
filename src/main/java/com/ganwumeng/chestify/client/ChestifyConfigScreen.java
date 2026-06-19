package com.ganwumeng.chestify.client;

import com.ganwumeng.chestify.ChestifyConfig;
import com.ganwumeng.chestify.SetRadiusPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ChestifyConfigScreen extends Screen {
    private final Screen parent;
    private EditBox radiusBox;
    private Button saveButton;

    public ChestifyConfigScreen(Screen parent) {
        super(Component.literal("Chestify Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        boolean canEdit = ChestifyConfig.canEditServerConfig();

        radiusBox = new EditBox(font, centerX - 80, height / 2 - 18, 160, 20, Component.literal("Search radius"));
        radiusBox.setValue(Integer.toString(ChestifyConfig.searchRadius()));
        radiusBox.setMaxLength(3);
        radiusBox.setEditable(canEdit);
        radiusBox.active = canEdit;
        addRenderableWidget(radiusBox);

        saveButton = Button.builder(Component.literal("Save"), button -> save())
                .bounds(centerX - 80, height / 2 + 16, 76, 20)
                .build();
        saveButton.active = canEdit;
        addRenderableWidget(saveButton);

        addRenderableWidget(Button.builder(Component.literal("Done"), button -> onClose())
                .bounds(centerX + 4, height / 2 + 16, 76, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float tickDelta) {
        extractTransparentBackground(graphics);
        graphics.centeredText(font, title, width / 2, height / 2 - 62, 0xFFFFFFFF);
        graphics.text(font, Component.literal("Search radius"), width / 2 - 80, height / 2 - 32, 0xFFE0E0E0, false);

        if (!ChestifyConfig.canEditServerConfig()) {
            graphics.centeredText(font, Component.literal("Only singleplayer owners or server operators can change this."), width / 2, height / 2 + 48, 0xFFFFA0A0);
        } else {
            graphics.centeredText(font, Component.literal("Allowed range: " + ChestifyConfig.MIN_SEARCH_RADIUS + "-" + ChestifyConfig.MAX_SEARCH_RADIUS), width / 2, height / 2 + 48, 0xFFB0B0B0);
        }

        super.extractRenderState(graphics, mouseX, mouseY, tickDelta);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private void save() {
        int radius;
        try {
            radius = Integer.parseInt(radiusBox.getValue().trim());
        } catch (NumberFormatException ignored) {
            radius = ChestifyConfig.searchRadius();
        }

        radius = ChestifyConfig.clampRadius(radius);
        radiusBox.setValue(Integer.toString(radius));
        ChestifyConfig.setSearchRadius(radius);
        ClientPlayNetworking.send(new SetRadiusPayload(radius));
    }
}
