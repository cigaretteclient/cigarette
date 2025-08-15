package io.github.waqfs.module.combat;

import io.github.waqfs.Cigarette;
import io.github.waqfs.gui.widget.SliderWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.mixin.KeyBindingAccessor;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class AutoClicker extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "AutoClicker";
    protected static final String MODULE_TOOLTIP = "Automatically clicks each tick when holding attack.";
    protected static final String MODULE_ID = "combat.autoclicker";
    private final SliderWidget clickPercent = new SliderWidget(Text.literal("Click Percent"), Text.literal("The percentage chance for a click to occur each tick of the game while holding left-click.")).withBounds(0, 0.9, 1).withAccuracy(2);

    public AutoClicker() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(clickPercent);
        clickPercent.registerConfigKey("combat.autoclicker.clickpercent");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        client.attackCooldown = 0;
        HitResult hitResult = client.crosshairTarget;
        KeyBinding attackKey = KeyBinding.byId("key.attack");
        KeyBindingAccessor attackKeyAccessor = (KeyBindingAccessor) attackKey;
        if (hitResult == null || attackKey == null || !attackKey.isPressed()) return;
        if (Math.random() > clickPercent.getRawState()) return;
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            attackKeyAccessor.setTimesPressed(attackKeyAccessor.getTimesPressed() + 1);
        }
    }

    @Override
    protected void whenEnabled() {
        Cigarette.CONFIG.COMBAT_PERFECT_HIT.widget.setRawState(false);
    }
}
