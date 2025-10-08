package dev.cigarette.module.combat;

import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class PerfectHit extends TickModule<ToggleWidget, Boolean> {
    public static final PerfectHit INSTANCE = new PerfectHit("combat.perfecthit", "PerfectHit", "Perfectly times hits on opponents while holding attack.");

    private final SliderWidget clickPercent = new SliderWidget("Click Percent", "The percentage chance for a click to occur each tick of the game while holding left-click and aiming at a hittable entity.").withBounds(0, 0.9, 1).withAccuracy(2);


    private PerfectHit(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(clickPercent);
        clickPercent.registerConfigKey(id + ".clickpercent");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        client.attackCooldown = 0;
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null || !KeybindHelper.KEY_ATTACK.isPhysicallyPressed()) return;
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();
            if (!(entity instanceof LivingEntity livingEntity)) return;
            if (livingEntity.hurtTime > 1) return;
            if (Math.random() > clickPercent.getRawState()) return;
            KeybindHelper.KEY_ATTACK.press();
        }
    }

    @Override
    protected void whenEnabled() {
        AutoClicker.INSTANCE.widget.setRawState(false);
    }
}
