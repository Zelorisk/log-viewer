package dev.logviewer.client.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    private static final KeyMapping.Category CATEGORY =
        KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("logviewer", "keys")
        );

    public static final KeyMapping TOGGLE_LOG_VIEWER =
        KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.logviewer.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F7,
                CATEGORY
            )
        );

    public static final KeyMapping TOGGLE_MOUSE_CONTROL =
        KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.logviewer.mouse",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                CATEGORY
            )
        );

    public static void register() {}
}
