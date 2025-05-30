package io.github.waqfs.gui;

import io.github.waqfs.Cigarette;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBinding implements ClientModInitializer {
    private static final Identifier HUD_KEYBINDING_ID = Identifier.of(Cigarette.MOD_ID, "hud-keybinding");
    private static net.minecraft.client.option.KeyBinding keyBinding;

    private static final CigaretteScreen screen = new CigaretteScreen();

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new net.minecraft.client.option.KeyBinding("toggle.gui.cigarette.waqfs", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "gui.cigarette.waqfs"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                if (client.currentScreen instanceof CigaretteScreen) {
                    client.currentScreen.close();
                } else {
                    screen.setParent(client.currentScreen);
                    client.setScreen(screen);
                }
            }
        });
    }
}
