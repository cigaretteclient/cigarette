package dev.cigarette.agent;

import dev.cigarette.gui.CategoryInstance;
import dev.cigarette.gui.widget.ToggleWidget;

public class DevWidget {
    public static final CategoryInstance CATEGORY_INSTANCE = new CategoryInstance("Agents", 10, 100);
    public static final ToggleWidget bedwarsAgent = new ToggleWidget("Bedwars", null).withDefaultState(false);
    public static final ToggleWidget murderMysteryAgent = new ToggleWidget("Murder Mystery", null).withDefaultState(false);
    public static final ToggleWidget zombiesAgent = new ToggleWidget("Zombies", null).withDefaultState(false);
    public static final ToggleWidget c2sPacketLogging = new ToggleWidget("Packet Logs C2S", null).withDefaultState(false);
    public static final ToggleWidget s2cPacketLogging = new ToggleWidget("Packet Logs S2C", null).withDefaultState(false);

    static {
        CATEGORY_INSTANCE.widget.setChildren(bedwarsAgent, murderMysteryAgent, zombiesAgent, c2sPacketLogging, s2cPacketLogging);
    }
}
