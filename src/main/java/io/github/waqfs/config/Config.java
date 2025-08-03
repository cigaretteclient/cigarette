package io.github.waqfs.config;

import io.github.waqfs.gui.instance.Category;
import io.github.waqfs.module.bedwars.*;
import io.github.waqfs.module.keybind.AddGlassBlock;
import io.github.waqfs.module.keybind.BreakBlock;
import io.github.waqfs.module.keybind.VClip;
import io.github.waqfs.module.murdermystery.GoldESP;

public class Config {
    public Category keybinds = new Category("Keybinds", 10, 10);
    public Category murderMystery = new Category("Murder Mystery", 120, 10);
    public Category bedwars = new Category("Bed Wars", 230, 10);

    public Category[] allCategories = new Category[]{keybinds, murderMystery, bedwars};

    public Config() {
        this.keybinds.attach(new AddGlassBlock(), new BreakBlock(), new VClip());
        this.murderMystery.attach(new io.github.waqfs.module.murdermystery.PlayerESP(), new GoldESP());
        this.bedwars.attach(new io.github.waqfs.module.bedwars.PlayerESP(), new FireballESP(), new DefenseViewer(), new JumpReset(), new AutoClicker(), new PerfectHit(), new AutoTool());
    }
}
