package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
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

public class ZombieESP extends TickModule {
    protected static final String MODULE_NAME = "ZombieESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the zombies in ESP.";
    protected static final String MODULE_ID = "zombies.zombieesp";
    private final Glow.Context glowContext = new Glow.Context();
    private final ToggleOptionsWidget enableZombies = new ToggleOptionsWidget(Text.literal("ESP Zombies"));
    private final ToggleOptionsWidget enableSkeletons = new ToggleOptionsWidget(Text.literal("ESP Skeletons"));
    private final ToggleOptionsWidget enableBlazes = new ToggleOptionsWidget(Text.literal("ESP Blazes"));
    private final ToggleOptionsWidget enableWolves = new ToggleOptionsWidget(Text.literal("ESP Wolves"));
    private final ToggleOptionsWidget enableCreepers = new ToggleOptionsWidget(Text.literal("ESP Creepers"));
    private final ToggleOptionsWidget enableMagmaCubes = new ToggleOptionsWidget(Text.literal("ESP Magma Cubes"));
    private final ToggleOptionsWidget enableSlimes = new ToggleOptionsWidget(Text.literal("ESP Slimes"));
    private final ToggleOptionsWidget enableWitches = new ToggleOptionsWidget(Text.literal("ESP Witches"));


    public ZombieESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        this.widget.setOptions(enableZombies, enableBlazes, enableWolves, enableCreepers, enableMagmaCubes, enableSlimes, enableWitches);
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
            if (entity instanceof ZombieEntity && enableZombies.getState()) this.glowContext.addGlow(uuid, 0x01A014);
            else if (entity instanceof BlazeEntity && enableBlazes.getState()) this.glowContext.addGlow(uuid, 0xFCA50F);
            else if (entity instanceof WolfEntity && enableWolves.getState()) this.glowContext.addGlow(uuid, 0x3FE6FC);
            else if (entity instanceof SkeletonEntity && enableSkeletons.getState()) this.glowContext.addGlow(uuid, 0xE0E0E0);
            else if (entity instanceof CreeperEntity && enableCreepers.getState()) this.glowContext.addGlow(uuid, 0x6DFC5D);
            else if (entity instanceof MagmaCubeEntity && enableMagmaCubes.getState()) this.glowContext.addGlow(uuid, 0xFC4619);
            else if (entity instanceof SlimeEntity && enableSlimes.getState()) this.glowContext.addGlow(uuid, 0x6DFC5D);
            else if (entity instanceof WitchEntity && enableWitches.getState()) this.glowContext.addGlow(uuid, 0xA625F7);
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
