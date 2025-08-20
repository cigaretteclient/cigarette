package dev.cigarette.module.bedwars;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.Glow;
import dev.cigarette.module.TickModule;
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

public class EntityESP extends TickModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "EntityESP";
    protected static final String MODULE_TOOLTIP = "Highlights all miscellaneous entities with their team color.";
    protected static final String MODULE_ID = "bedwars.entityesp";
    private final ToggleWidget enableEnderDragons = new ToggleWidget(Text.literal("Dragons"), null).withDefaultState(true);
    private final ToggleWidget enableIronGolems = new ToggleWidget(Text.literal("Iron Golems"), null).withDefaultState(true);
    private final ToggleWidget enableSilverfish = new ToggleWidget(Text.literal("Silverfish"), null).withDefaultState(true);
    private final Glow.Context glowContext = new Glow.Context();

    public EntityESP() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
        TextWidget header = new TextWidget(Text.literal("Types")).withUnderline();
        this.setChildren(header, enableEnderDragons, enableIronGolems, enableSilverfish);
        enableEnderDragons.registerConfigKey("bedwars.entityesp.enderdragons");
        enableIronGolems.registerConfigKey("bedwars.entityesp.irongolems");
        enableSilverfish.registerConfigKey("bedwars.entityesp.silverfish");
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
            if (entity instanceof EnderDragonEntity && !enableEnderDragons.getRawState()) continue;
            if (entity instanceof IronGolemEntity && !enableIronGolems.getRawState()) continue;
            if (entity instanceof SilverfishEntity && !enableSilverfish.getRawState()) continue;
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
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.BEDWARS && GameDetector.subGame == GameDetector.ChildGame.INSTANCED_BEDWARS;
    }
}
