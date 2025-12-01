package dev.cigarette.module.combat;

import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

public class AimSlow extends TickModule<ToggleWidget, Boolean> {

    public static final AimSlow INSTANCE =
            new AimSlow("combat.aimslow", "AimSlow",
                    "Slows your sensitivity when your crosshair is on a player.");

    // 1–10: 1 = almost normal, 10 = super slow
    private final SliderWidget slowStrength =
            new SliderWidget("Slow Strength",
                    "1 = light slowdown, 10 = very strong slowdown.")
                    .withBounds(1, 3, 10)
                    .withAccuracy(0);

    private final SliderWidget maxRange =
            new SliderWidget("Max Range",
                    "Maximum distance for slowdown to apply.")
                    .withBounds(1.0, 3.1, 10)
                    .withAccuracy(1);

    private double lastAppliedMultiplier = 1.0;

    private AimSlow(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(slowStrength, maxRange);

        slowStrength.registerConfigKey(id + ".slowstrength");
        maxRange.registerConfigKey(id + ".maxrange");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client,
                                 @NotNull ClientWorld world,
                                 @NotNull ClientPlayerEntity player) {

        if (client.currentScreen != null)
            return;

        if (client.options == null || client.options.getMouseSensitivity() == null)
            return;

        boolean onPlayer = isCrosshairOnPlayer(client, player);

        // convert 1–10 into a 1.0–0.1 multiplier
        int strength = slowStrength.getRawState().intValue(); // 1–10
        if (strength < 1) strength = 1;
        if (strength > 10) strength = 10;

        double targetMultiplier = onPlayer ? (1.0 / strength) : 1.0;

        if (Math.abs(targetMultiplier - lastAppliedMultiplier) < 0.0001)
            return;

        double currentSens = client.options.getMouseSensitivity().getValue();

        double baseSens = currentSens / lastAppliedMultiplier;
        double newSens = baseSens * targetMultiplier;

        client.options.getMouseSensitivity().setValue(newSens);
        lastAppliedMultiplier = targetMultiplier;
    }

    private boolean isCrosshairOnPlayer(MinecraftClient client, ClientPlayerEntity player) {
        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit))
            return false;

        if (!(entityHit.getEntity() instanceof LivingEntity target))
            return false;

        if (target == player)
            return false;

        double range = maxRange.getRawState();
        double rangeSq = range * range;

        return player.squaredDistanceTo(target) <= rangeSq;
    }
}
