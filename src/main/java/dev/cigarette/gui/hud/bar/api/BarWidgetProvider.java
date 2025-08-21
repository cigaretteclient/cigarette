package dev.cigarette.gui.hud.bar.api;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;

import java.util.List;

public interface BarWidgetProvider {
    void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out);
}

