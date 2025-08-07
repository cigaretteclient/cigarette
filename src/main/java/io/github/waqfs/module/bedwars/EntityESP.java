package io.github.waqfs.module.bedwars;

import io.github.waqfs.GameDetector;
import io.github.waqfs.gui.widget.TextWidget;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import io.github.waqfs.lib.Glow;
import io.github.waqfs.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class EntityESP extends TickModule {
    protected static final String MODULE_NAME = "EntityESP";
    protected static final String MODULE_TOOLTIP = "Highlights all miscellaneous entities with their team color.";
    protected static final String MODULE_ID = "bedwars.entityesp";
    private final ToggleOptionsWidget enableEnderDragons = new ToggleOptionsWidget(Text.literal("Dragons")).withDefaultState(true);
    private final ToggleOptionsWidget enableIronGolems = new ToggleOptionsWidget(Text.literal("Iron Golems")).withDefaultState(true);
    private final ToggleOptionsWidget enableSilverfish = new ToggleOptionsWidget(Text.literal("Silverfish")).withDefaultState(true);
    private final Glow.Context glowContext = new Glow.Context();

    public EntityESP() {
        super(MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.widget.setOptions(header, enableEnderDragons, enableIronGolems, enableSilverfish);
        enableEnderDragons.registerAsOption("bedwars.entityesp.enderdragons");
        enableIronGolems.registerAsOption("bedwars.entityesp.irongolems");
        enableSilverfish.registerAsOption("bedwars.entityesp.silverfish");
    }

    private int getNameColor(Entity entity) {
        Text displayName = entity.getDisplayName();
        if (entity instanceof IronGolemEntity) {
            ClientWorld world = MinecraftClient.getInstance().world;
            assert world != null;
            Box box = Box.of(entity.getPos(), 0, 2.2, 0);
            for (Entity target : world.getOtherEntities(entity, box)) {
                if (!(target instanceof ArmorStandEntity)) continue;
                Text standDisplayName = target.getDisplayName();
                if (standDisplayName == null) continue;
                displayName = standDisplayName;
            }
        }
        if (displayName == null) return 0;
        List<Text> siblings = displayName.getSiblings();
        for (Text sibling : siblings) {
            TextColor color = sibling.getStyle().getColor();
            if (color == null) continue;
            if (color.getName().equals("dark_gray")) continue;
            if (color.getName().equals("gray")) continue;
            return color.getRgb();
        }
        return 0;
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        this.glowContext.removeAll();
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof EnderDragonEntity) && !(entity instanceof IronGolemEntity) && !(entity instanceof SilverfishEntity)) continue;
            if (entity instanceof EnderDragonEntity && !enableEnderDragons.getState()) continue;
            if (entity instanceof IronGolemEntity && !enableIronGolems.getState()) continue;
            if (entity instanceof SilverfishEntity && !enableSilverfish.getState()) continue;
            UUID uuid = entity.getUuid();
            int nameColor = getNameColor(entity);
            this.glowContext.addGlow(uuid, nameColor);
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.glowContext.removeAll();
    }

    @Override
    protected boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }

}
