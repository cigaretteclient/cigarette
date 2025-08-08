package io.github.waqfs.agent;

import io.github.waqfs.gui.instance.Category;
import io.github.waqfs.gui.widget.ToggleOptionsWidget;
import net.minecraft.text.Text;

public class DevWidget {
    public static final Category category = new Category("Agents", 10, 100);
    public static final ToggleOptionsWidget bedwarsAgent = new ToggleOptionsWidget(Text.literal("Bedwars")).withDefaultState(false);
    public static final ToggleOptionsWidget murderMysteryAgent = new ToggleOptionsWidget(Text.literal("Murder Mystery")).withDefaultState(false);

    static {
        category.widget.setChildren(bedwarsAgent, murderMysteryAgent);
    }
}
