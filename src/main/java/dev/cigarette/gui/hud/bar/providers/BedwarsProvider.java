package dev.cigarette.gui.hud.bar.providers;

import dev.cigarette.gui.hud.bar.api.BarWidget;
import dev.cigarette.gui.hud.bar.api.BarWidgetProvider;
import dev.cigarette.gui.hud.bar.widgets.EntityChipWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.math.Box;

import java.util.List;
import java.util.UUID;

public class BedwarsProvider implements BarWidgetProvider {
    @Override
    public void collect(MinecraftClient mc, ClientWorld world, TextRenderer tr, List<BarWidget> out) {
        if (world == null) return;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof EnderDragonEntity) && !(entity instanceof IronGolemEntity) && !(entity instanceof SilverfishEntity)) continue;
            UUID uuid = entity.getUuid();
            if (uuid == null) continue;
            String name = (entity instanceof EnderDragonEntity) ? "Dragon" : (entity instanceof IronGolemEntity) ? "Iron Golem" : "Silverfish";
            int color = getEntityNameColor(entity);
            out.add(new EntityChipWidget("uuid:" + uuid, entity, name, 0d, color == 0 ? 0xFFFFFFFF : color));
        }
    }

    private static int getEntityNameColor(Entity entity) {
        if (entity == null) return 0;
        Text displayName = entity.getDisplayName();
        if (entity instanceof IronGolemEntity) {
            ClientWorld world = MinecraftClient.getInstance().world;
            if (world != null) {
                Box box = Box.of(entity.getPos(), 0, 2.2, 0);
                for (Entity target : world.getOtherEntities(entity, box)) {
                    if (!(target instanceof ArmorStandEntity)) continue;
                    Text standDisplayName = target.getDisplayName();
                    if (standDisplayName == null) continue;
                    displayName = standDisplayName;
                }
            }
        }
        if (displayName == null) return 0;
        for (Text sibling : displayName.getSiblings()) {
            TextColor color = sibling.getStyle().getColor();
            if (color == null) continue;
            if ("dark_gray".equals(color.getName())) continue;
            if ("gray".equals(color.getName())) continue;
            return color.getRgb();
        }
        return 0;
    }
}

