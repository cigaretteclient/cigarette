package dev.cigarette.lib;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class ItemStackL {
    public static String[] getLoreLines(ItemStack item) {
        LoreComponent lore = item.get(DataComponentTypes.LORE);
        if (lore == null) return new String[0];
        List<Text> lines = lore.lines();
        String[] strLines = new String[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            strLines[i] = TextL.toColorCodedString(lines.get(i));
        }
        return strLines;
    }
}
