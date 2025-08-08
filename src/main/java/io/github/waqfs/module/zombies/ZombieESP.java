package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.*;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ZombieESP extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "ZombieESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the zombies in ESP.";
    protected static final String MODULE_ID = "zombies.zombieesp";
    private final Glow.Context glowContext = new Glow.Context();
    private final ToggleColorWidget enableZombies = new ToggleColorWidget(Text.literal("Zombies"), false).withDefaultColor(0xFF01A014).withDefaultState(true);
    private final ToggleColorWidget enableSkeletons = new ToggleColorWidget(Text.literal("Skeletons"), false).withDefaultColor(0xFFE0E0E0).withDefaultState(true);
    private final ToggleColorWidget enableBlazes = new ToggleColorWidget(Text.literal("Blazes"), false).withDefaultColor(0xFFFCA50F).withDefaultState(true);
    private final ToggleColorWidget enableWolves = new ToggleColorWidget(Text.literal("Wolves"), false).withDefaultColor(0xFF3FE6FC).withDefaultState(true);
    private final ToggleColorWidget enableCreepers = new ToggleColorWidget(Text.literal("Creepers"), false).withDefaultColor(0xFF6DFC5D).withDefaultState(true);
    private final ToggleColorWidget enableMagmaCubes = new ToggleColorWidget(Text.literal("Magma Cubes"), false).withDefaultColor(0xFFFC4619).withDefaultState(true);
    private final ToggleColorWidget enableSlimes = new ToggleColorWidget(Text.literal("Slimes"), false).withDefaultColor(0xFF6DFC5D).withDefaultState(true);
    private final ToggleColorWidget enableWitches = new ToggleColorWidget(Text.literal("Witches"), false).withDefaultColor(0xFFA625F7).withDefaultState(true);

    public ZombieESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.setChildren(header, enableZombies, enableBlazes, enableWolves, enableCreepers, enableMagmaCubes, enableSlimes, enableWitches);
        enableZombies.registerAsOption("zombies.zombieesp.zombies");
        enableBlazes.registerAsOption("zombies.zombieesp.blazes");
        enableWolves.registerAsOption("zombies.zombieesp.wolves");
        enableCreepers.registerAsOption("zombies.zombieesp.creepers");
        enableMagmaCubes.registerAsOption("zombies.zombieesp.magmacubes");
        enableSlimes.registerAsOption("zombies.zombieesp.slimes");
        enableWitches.registerAsOption("zombies.zombieesp.witches");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            UUID uuid = entity.getUuid();
            if (entity instanceof ZombieEntity && enableZombies.getToggleState()) this.glowContext.addGlow(uuid, enableZombies.getStateRGB());
            else if (entity instanceof BlazeEntity && enableBlazes.getToggleState()) this.glowContext.addGlow(uuid, enableBlazes.getStateRGB());
            else if (entity instanceof WolfEntity && enableWolves.getToggleState()) this.glowContext.addGlow(uuid, enableWolves.getStateRGB());
            else if (entity instanceof SkeletonEntity && enableSkeletons.getToggleState()) this.glowContext.addGlow(uuid, enableSkeletons.getStateRGB());
            else if (entity instanceof CreeperEntity && enableCreepers.getToggleState()) this.glowContext.addGlow(uuid, enableCreepers.getStateRGB());
            else if (entity instanceof MagmaCubeEntity && enableMagmaCubes.getToggleState()) this.glowContext.addGlow(uuid, enableMagmaCubes.getStateRGB());
            else if (entity instanceof SlimeEntity && enableSlimes.getToggleState()) this.glowContext.addGlow(uuid, enableSlimes.getStateRGB());
            else if (entity instanceof WitchEntity && enableWitches.getToggleState()) this.glowContext.addGlow(uuid, enableWitches.getStateRGB());
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
