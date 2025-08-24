package dev.cigarette.module.zombies;

import dev.cigarette.GameDetector;
import dev.cigarette.agent.ZombiesAgent;
import dev.cigarette.gui.widget.ColorDropdownWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Glow;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ZombieESP extends TickModule<ToggleWidget, Boolean> {
    public static final ZombieESP INSTANCE = new ZombieESP("zombies.zombieesp", "ZombieESP", "Highlights all the zombies in ESP.");

    private final Glow.Context glowContext = new Glow.Context();

    private final ColorDropdownWidget<ToggleWidget, Boolean> enableZombies = ColorDropdownWidget.buildToggle("Zombies", null).withAlpha(false).withDefaultColor(0xFF2C936C).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableSkeletons = ColorDropdownWidget.buildToggle("Skeletons", null).withAlpha(false).withDefaultColor(0xFFE0E0E0).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableBlazes = ColorDropdownWidget.buildToggle("Blazes", null).withAlpha(false).withDefaultColor(0xFFFCA50F).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableWolves = ColorDropdownWidget.buildToggle("Wolves", null).withAlpha(false).withDefaultColor(0xFF3FE6FC).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableCreepers = ColorDropdownWidget.buildToggle("Creepers", null).withAlpha(false).withDefaultColor(0xFF155B0D).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableMagmaCubes = ColorDropdownWidget.buildToggle("Magma Cubes", null).withAlpha(false).withDefaultColor(0xFFFC4619).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableSlimes = ColorDropdownWidget.buildToggle("Slimes", null).withAlpha(false).withDefaultColor(0xFF155B0D).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableWitches = ColorDropdownWidget.buildToggle("Witches", null).withAlpha(false).withDefaultColor(0xFFA625F7).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableEndermite = ColorDropdownWidget.buildToggle("Endermite", null).withAlpha(false).withDefaultColor(0xFFA625F7).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableSilverfish = ColorDropdownWidget.buildToggle("Silverish", null).withAlpha(false).withDefaultColor(0xFF3F3F3F).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableIronGolems = ColorDropdownWidget.buildToggle("Iron Golems", null).withAlpha(false).withDefaultColor(0xFF3FE6FC).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableGhast = ColorDropdownWidget.buildToggle("Ghast", null).withAlpha(false).withDefaultColor(0xFF3FE6FC).withDefaultState(true);
    private final ColorDropdownWidget<ToggleWidget, Boolean> enableGiants = ColorDropdownWidget.buildToggle("Giant Zombies", null).withAlpha(false).withDefaultColor(0xFF2C936C).withDefaultState(true);


    private ZombieESP(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget("Types").withUnderline();
        this.setChildren(header, enableZombies, enableBlazes, enableWolves, enableCreepers, enableMagmaCubes, enableSlimes, enableWitches, enableEndermite, enableSilverfish, enableIronGolems, enableGhast, enableGiants);
        enableZombies.registerConfigKey(id + ".zombies");
        enableBlazes.registerConfigKey(id + ".blazes");
        enableWolves.registerConfigKey(id + ".wolves");
        enableCreepers.registerConfigKey(id + ".creepers");
        enableMagmaCubes.registerConfigKey(id + ".magmacubes");
        enableSlimes.registerConfigKey(id + ".slimes");
        enableWitches.registerConfigKey(id + ".witches");
        enableEndermite.registerConfigKey(id + ".endermite");
        enableSilverfish.registerConfigKey(id + ".silverfish");
        enableIronGolems.registerConfigKey(id + ".irongolems");
        enableGhast.registerConfigKey(id + ".ghast");
        enableGiants.registerConfigKey(id + ".giants");
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
                case ENDERMITE -> addGlow(enableEndermite, zombie.uuid);
                case SILVERFISH -> addGlow(enableSilverfish, zombie.uuid);
                case IRON_GOLEM -> addGlow(enableIronGolems, zombie.uuid);
                case GHAST -> addGlow(enableGhast, zombie.uuid);
                case GIANT_ZOMBIE -> addGlow(enableGiants, zombie.uuid);
            }
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.ZOMBIES && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_ZOMBIES;
    }
}
