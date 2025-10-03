package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class KeyBinding implements ClientModInitializer {
    @SuppressWarnings("unused")
    private static final Identifier HUD_KEYBINDING_ID = Identifier.of(Cigarette.MOD_ID, "hud-keybinding");

    private static net.minecraft.client.option.KeyBinding keyBinding;

    @Override
    public void onInitializeClient() {
        keyBinding = KeyBindingHelper.registerKeyBinding(new net.minecraft.client.option.KeyBinding("Toggle GUI", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "Cigarette | User Interface"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (keyBinding.wasPressed()) {
                if (client.currentScreen instanceof CigaretteScreen) {
                    client.currentScreen.close();
                } else {
                    Cigarette.SCREEN.setParent(client.currentScreen);
                    client.setScreen(Cigarette.SCREEN);
                }
            }
        });
    }
}
