package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class Aimbot extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Aimbot";
    protected static final String MODULE_TOOLTIP = "Automatically aims at zombies.";
    protected static final String MODULE_ID = "zombies.aimbot";

    public Aimbot() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        ZombiesAgent.ZombieTarget closest = ZombiesAgent.getClosestZombie();
        if (closest == null) return;
        Vec3d vector = closest.getDirectionVector(player);
        PlayerEntityL.setRotationVector(player, vector);
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
