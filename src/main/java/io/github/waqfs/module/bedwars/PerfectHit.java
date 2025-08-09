package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.BedwarsAgent;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.mixin.MinecraftClientInvoker;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class PerfectHit extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "PerfectHit";
    protected static final String MODULE_TOOLTIP = "Perfectly times hits on opponents while holding attack.";
    protected static final String MODULE_ID = "bedwars.perfecthit";

    private static final int COOLDOWN_TICKS = 4;
    private int cooldown = 0;

    public PerfectHit() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        BedwarsAgent.perfectHitModule = this;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        HitResult hitResult = client.crosshairTarget;
        KeyBinding attackKey = KeyBinding.byId("key.attack");
        if (hitResult == null || attackKey == null || !attackKey.isPressed()) return;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();
            if (!(entity instanceof LivingEntity livingEntity)) return;
            if (livingEntity.hurtTime > 1) return;
            attackKey.wasPressed();
            ((MinecraftClientInvoker) client).invokeDoAttack();
            cooldown = COOLDOWN_TICKS;
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        if (cooldown > 0) cooldown--;
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

    @Override
    protected void whenEnabled() {
        BedwarsAgent.autoClickerModule.widget.setRawState(false);
    }
}
