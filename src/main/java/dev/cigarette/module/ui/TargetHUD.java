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
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class TargetHUD extends RenderModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "TargetHUD";
    protected static final String MODULE_TOOLTIP = "Displays the current target & kill logs.";
    protected static final String MODULE_ID = "ui.targethud";

    private final ColorDropdownWidget<ToggleWidget, Boolean> bgColor = ColorDropdownWidget.buildToggle(Text.of("BG Color"), Text.of("The background color of the bar.")).withAlpha(true).withDefaultColor(Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f));
    private final SliderWidget rowHeight = (SliderWidget) new SliderWidget(Text.literal("Row Height"), Text.literal("The height of each row of the bar.")).withBounds(20, 1, 30).withDefault(24D);
    private final SliderWidget globalPadding = (SliderWidget) new SliderWidget(Text.literal("Global Padding"), Text.literal("The padding between each row of the bar.")).withBounds(0, 1, 10).withDefault(4D);
    private final SliderWidget maxRows = (SliderWidget) new SliderWidget(Text.literal("Max Rows"), Text.literal("The maximum number of rows to display.")).withBounds(1, 1, 5).withDefault(3D);

    private BarDisplay display;

    public TargetHUD() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.widget.withDefaultState(true);
        this.setChildren(
                bgColor,
                rowHeight,
                globalPadding,
                maxRows
        );
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        BarDisplay.BG_COLOR = bgColor.getToggleState() ? bgColor.getStateARGB() : Color.colorTransparentize(CigaretteScreen.PRIMARY_COLOR, 0.4f);
        BarDisplay.TARGET_ROW_HEIGHT = rowHeight.getRawState().intValue();
        BarDisplay.GLOBAL_PADDING = globalPadding.getRawState().intValue();
        BarDisplay.MAX_ROWS = maxRows.getRawState().intValue();
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
                                 @NotNull ClientPlayerEntity player) {
        display = new BarDisplay();
        Cigarette.registerHudElement(display);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (display != null) {
            Cigarette.unregisterHudElement(display);
            display = null;
        }
    }
}
