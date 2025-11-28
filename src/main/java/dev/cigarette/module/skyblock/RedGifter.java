package dev.cigarette.module.skyblock;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;

public class RedGifter extends TickModule<ToggleWidget, Boolean> {
    public static final RedGifter INSTANCE = new RedGifter("skyblock.redgifter", "Auto Red Gifter", "Automatically gives and opens red gifts.");

    private RedGifter(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }

    @Override
    protected void onEnabledTick(net.minecraft.client.MinecraftClient client, net.minecraft.client.world.ClientWorld world, net.minecraft.client.network.ClientPlayerEntity player) {
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.SKYBLOCK;
    }
}
