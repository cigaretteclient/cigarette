package io.github.waqfs.module.zombies;

import io.github.waqfs.GameDetector;
import io.github.waqfs.agent.ZombiesAgent;
import io.github.waqfs.gui.widget.ColorDropdownWidget;
import io.github.waqfs.gui.widget.TextWidget;
import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ZombieESP extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "ZombieESP";
    protected static final String MODULE_TOOLTIP = "Highlights all the zombies in ESP.";
    protected static final String MODULE_ID = "zombies.zombieesp";
    private final Glow.Context glowContext = new Glow.Context();
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableZombies = ColorDropdownWidget.buildToggle(Text.literal("Zombies"), null).withAlpha(false).withDefaultColor(0xFF01A014).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableSkeletons = ColorDropdownWidget.buildToggle(Text.literal("Skeletons"), null).withAlpha(false).withDefaultColor(0xFFE0E0E0).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableBlazes = ColorDropdownWidget.buildToggle(Text.literal("Blazes"), null).withAlpha(false).withDefaultColor(0xFFFCA50F).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableWolves = ColorDropdownWidget.buildToggle(Text.literal("Wolves"), null).withAlpha(false).withDefaultColor(0xFF3FE6FC).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableCreepers = ColorDropdownWidget.buildToggle(Text.literal("Creepers"), null).withAlpha(false).withDefaultColor(0xFF6DFC5D).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableMagmaCubes = ColorDropdownWidget.buildToggle(Text.literal("Magma Cubes"), null).withAlpha(false).withDefaultColor(0xFFFC4619).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableSlimes = ColorDropdownWidget.buildToggle(Text.literal("Slimes"), null).withAlpha(false).withDefaultColor(0xFF6DFC5D).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableWitches = ColorDropdownWidget.buildToggle(Text.literal("Witches"), null).withAlpha(false).withDefaultColor(0xFFA625F7).withDefaultState(true);

    public ZombieESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.setChildren(header, enableZombies, enableBlazes, enableWolves, enableCreepers, enableMagmaCubes, enableSlimes, enableWitches);
        enableZombies.registerConfigKey("zombies.zombieesp.zombies");
        enableBlazes.registerConfigKey("zombies.zombieesp.blazes");
        enableWolves.registerConfigKey("zombies.zombieesp.wolves");
        enableCreepers.registerConfigKey("zombies.zombieesp.creepers");
        enableMagmaCubes.registerConfigKey("zombies.zombieesp.magmacubes");
        enableSlimes.registerConfigKey("zombies.zombieesp.slimes");
        enableWitches.registerConfigKey("zombies.zombieesp.witches");
    }

    private void addGlow(ColorDropdownWidget<ToggleWidget, Boolean> widget, UUID uuid) {
        if (!widget.getToggleState()) return;
        this.glowContext.addGlow(uuid, widget.getStateRGB());
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (ZombiesAgent.ZombieTarget zombie : ZombiesAgent.getZombies()) {
            switch (zombie.type) {
                case ZOMBIE -> addGlow(enableZombies, zombie.uuid);
                case BLAZE -> addGlow(enableBlazes, zombie.uuid);
                case WOLF -> addGlow(enableWolves, zombie.uuid);
                case SKELETON -> addGlow(enableSkeletons, zombie.uuid);
                case CREEPER -> addGlow(enableCreepers, zombie.uuid);
                case MAGMACUBE -> addGlow(enableMagmaCubes, zombie.uuid);
                case SLIME -> addGlow(enableSlimes, zombie.uuid);
                case WITCH -> addGlow(enableWitches, zombie.uuid);

            }
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
