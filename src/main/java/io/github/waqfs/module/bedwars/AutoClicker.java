package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.mixin.KeyBindingAccessor;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class AutoClicker extends TickModule {
    protected static final String MODULE_NAME = "AutoClicker";
    protected static final String MODULE_TOOLTIP = "Automatically clicks at a rate of ~18 clicks/second when holding attack.";
    protected static final String MODULE_ID = "bedwars.autoclicker";

    private static final double CLICK_PERCENT = 0.9;

    public AutoClicker() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        BedwarsAgent.autoClickerModule = this;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        client.attackCooldown = 0;
        HitResult hitResult = client.crosshairTarget;
        KeyBinding attackKey = KeyBinding.byId("key.attack");
        KeyBindingAccessor attackKeyAccessor = (KeyBindingAccessor) attackKey;
        if (hitResult == null || attackKey == null || !attackKey.isPressed()) return;
        if (Math.random() > CLICK_PERCENT) return;
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            attackKeyAccessor.setTimesPressed(attackKeyAccessor.getTimesPressed() + 1);
        }
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    @Override
    protected void whenEnabled() {
        BedwarsAgent.perfectHitModule.widget.setState(false);
    }
}
