package dev.containerscan;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeybindHandler {
    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(Identifier.of("containerscan", "main"));

    private static KeyBinding openKey;

    private KeybindHandler() {}

    public static void init() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.containerscan.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.wasPressed()) {
                // Closing while the screen is open is handled inside ScanScreen.tick().
                if (mc.currentScreen == null && mc.player != null) mc.setScreen(new ScanScreen());
            }
        });
    }
}
