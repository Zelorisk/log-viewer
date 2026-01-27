package dev.logviewer.client;

import dev.logviewer.client.gui.LogViewerHud;
import dev.logviewer.client.keybind.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;

public class LogViewerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindings.register();

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                LogViewerHud.getInstance().render(
                    graphics,
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight()
                );
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (KeyBindings.TOGGLE_LOG_VIEWER.consumeClick()) {
                LogViewerHud.getInstance().toggle();
            }

            while (KeyBindings.TOGGLE_MOUSE_CONTROL.consumeClick()) {
                LogViewerHud hud = LogViewerHud.getInstance();
                if (hud.isVisible()) {
                    hud.toggleMouseControl();
                    if (hud.isMouseControlActive()) {
                        client.mouseHandler.releaseMouse();
                    } else {
                        client.mouseHandler.grabMouse();
                    }
                }
            }
        });
    }
}
