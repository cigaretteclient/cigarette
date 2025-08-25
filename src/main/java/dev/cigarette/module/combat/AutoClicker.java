package dev.cigarette.module.combat;

import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.mixin.KeyBindingAccessor;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class AutoClicker extends TickModule<ToggleWidget, Boolean> {
    public static final AutoClicker INSTANCE = new AutoClicker("combat.autoclicker", "AutoClicker", "Automatically clicks each tick when holding attack.");

    private final SliderWidget clickPercent = new SliderWidget("Click Percent", "The percentage chance for a click to occur each tick of the game while holding left-click.").withBounds(0, 0.9, 1).withAccuracy(2);

    private AutoClicker(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(clickPercent);
        clickPercent.registerConfigKey(id + ".clickpercent");
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
        PerfectHit.INSTANCE.widget.setRawState(false);
    }
}
