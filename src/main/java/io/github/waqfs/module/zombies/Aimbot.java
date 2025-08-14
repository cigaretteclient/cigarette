package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.gui.widget.SliderWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.PlayerEntityL;
import io.github.waqfs.module.TickModule;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

public class Aimbot extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "Aimbot";
    protected static final String MODULE_TOOLTIP = "Automatically aims at zombies.";
    protected static final String MODULE_ID = "zombies.aimbot";

    private final ToggleWidget nearCrosshair = new ToggleWidget(Text.literal("Near Crosshair"), Text.literal("Shoots the closest zombie in the direction you are facing, ignoring distance.")).withDefaultState(true);
    private final SliderWidget crosshairAngle = new SliderWidget(Text.literal("Max Angle")).withBounds(5, 30, 90);

    private KeyBinding rightClickKey = null;

    public Aimbot() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.setChildren(nearCrosshair, crosshairAngle);
        nearCrosshair.registerConfigKey("zombies.aimbot.crosshair");
        crosshairAngle.registerConfigKey("zombies.aimbot.crosshair.angle");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (rightClickKey == null) {
            rightClickKey = KeyBinding.byId("key.use");
            return;
        }
        if (rightClickKey.isPressed()) {
            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockResult = (BlockHitResult) hitResult;
                BlockState lookingAt = world.getBlockState(blockResult.getBlockPos());
                if (lookingAt.isIn(BlockTags.BUTTONS)) return;
            }
            if (ZombiesAgent.isGun(player.getMainHandStack())) {
                ZombiesAgent.ZombieTarget closest = nearCrosshair.getRawState() ? ZombiesAgent.getClosestZombieTo(player, crosshairAngle.getRawState().floatValue()) : ZombiesAgent.getClosestZombie();
                if (closest == null) return;
                Vec3d vector = closest.getDirectionVector(player);
                PlayerEntityL.setRotationVector(player, vector);
            }
        }
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
