package dev.cigarette.module.ui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.CigaretteScreen;
import dev.cigarette.gui.hud.bar.BarDisplay;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Color;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class TargetHUD extends RenderModule<ToggleWidget, Boolean> {
    public static final TargetHUD INSTANCE = new TargetHUD("ui.targethud", "TargetHUD", "Displays the current target & kill logs.");

    private final ColorDropdownWidget<ToggleWidget, Boolean> bgColor = ColorDropdownWidget.buildToggle("BG Color", "The background color of the bar.").withAlpha(true).withDefaultColor(Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f)).withDefaultState(true);
    private final SliderWidget rowHeight = new SliderWidget("Row Height", "The height of each row of the bar.").withBounds(20, 24, 30);
    private final SliderWidget globalPadding = new SliderWidget("Global Padding", "The padding between each row of the bar.").withBounds(0, 4, 10);
    private final SliderWidget maxRows = new SliderWidget("Max Rows", "The maximum number of rows to display.").withBounds(1, 3, 5);

    private BarDisplay display;

    private TargetHUD(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.widget.withDefaultState(true);
        this.setChildren(
                bgColor,
                rowHeight,
                globalPadding,
                maxRows
        );
        rowHeight.registerConfigKey(id + "rowheight");
        globalPadding.registerConfigKey(id + "padding");
        maxRows.registerConfigKey(id + "maxrows");
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
                                 @NotNull ClientPlayerEntity player) {
        if (display == null) {
            display = new BarDisplay();
            Cigarette.registerHudElement(display);
        }
        BarDisplay.BG_COLOR = bgColor.getToggleState() ? bgColor.getStateARGB() : Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f);
        BarDisplay.TARGET_ROW_HEIGHT = rowHeight.getRawState().intValue();
        BarDisplay.GLOBAL_PADDING = globalPadding.getRawState().intValue();
        BarDisplay.MAX_ROWS = maxRows.getRawState().intValue();
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
