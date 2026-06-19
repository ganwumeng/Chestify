package com.ganwumeng.chestify.client;

import com.ganwumeng.chestify.ChestifyConfig;
import com.ganwumeng.chestify.RadiusSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class ChestifyClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RadiusSyncPayload.ID, (payload, context) ->
                context.client().execute(() -> {
                    ChestifyConfig.setSearchRadius(payload.radius());
                    ChestifyConfig.setCanEditServerConfig(payload.canEdit());
                }));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof AbstractContainerScreen<?>) {
                ChestifyOverlay.onContainerScreenOpened();
                Screens.getWidgets(screen).add(ChestifyOverlay.createSearchBox(client, scaledWidth, scaledHeight));
                ScreenEvents.beforeExtract(screen).register((currentScreen, graphics, mouseX, mouseY, tickDelta) ->
                        ChestifyOverlay.layoutSearchBox(currentScreen.width, currentScreen.height));
                ScreenEvents.afterExtract(screen).register((currentScreen, graphics, mouseX, mouseY, tickDelta) ->
                        ChestifyOverlay.extract(graphics, currentScreen.width, currentScreen.height, mouseX, mouseY));
                ScreenMouseEvents.allowMouseClick(screen).register((currentScreen, event) ->
                        !ChestifyOverlay.mouseClicked(currentScreen.width, currentScreen.height, event.x(), event.y(), event.button()));
            }
        });
    }
}
