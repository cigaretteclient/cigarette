package io.github.waqfs.agent;

import io.github.waqfs.gui.CategoryInstance;
import io.github.waqfs.gui.widget.ToggleWidget;
import net.minecraft.text.Text;

public class DevWidget {
    public static final CategoryInstance CATEGORY_INSTANCE = new CategoryInstance("Agents", 10, 100);
    public static final ToggleWidget bedwarsAgent = new ToggleWidget(Text.literal("Bedwars"), null).withDefaultState(false);
    public static final ToggleWidget murderMysteryAgent = new ToggleWidget(Text.literal("Murder Mystery"), null).withDefaultState(false);
    public static final ToggleWidget zombiesAgent = new ToggleWidget(Text.literal("Zombies"), null).withDefaultState(false);

    static {
        CATEGORY_INSTANCE.widget.setChildren(bedwarsAgent, murderMysteryAgent, zombiesAgent);
    }
}
