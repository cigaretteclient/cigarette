package io.github.waqfs.config;

import io.github.waqfs.Cigarette;
import io.github.waqfs.agent.DevWidget;
import io.github.waqfs.gui.CategoryInstance;
import io.github.waqfs.module.bedwars.*;
import io.github.waqfs.module.combat.AutoClicker;
import io.github.waqfs.module.combat.JumpReset;
import io.github.waqfs.module.combat.PerfectHit;
import io.github.waqfs.module.keybind.AddGlassBlock;
import io.github.waqfs.module.keybind.BreakBlock;
import io.github.waqfs.module.keybind.VClip;
import io.github.waqfs.module.murdermystery.GoldESP;
import io.github.waqfs.module.zombies.Aimbot;
import io.github.waqfs.module.render.ModuleList;
import io.github.waqfs.module.render.Notifications;
import io.github.waqfs.module.render.PlayerESP;
import io.github.waqfs.module.render.ProjectileESP;
import io.github.waqfs.module.render.Watermark;
import io.github.waqfs.module.zombies.ReviveAura;
import io.github.waqfs.module.zombies.ZombieESP;

public class Config {
    public CategoryInstance keybinds = new CategoryInstance("Keybinds", 10, 10);
    public CategoryInstance murderMystery = new CategoryInstance("Murder Mystery", 120, 10);
    public CategoryInstance bedwars = new CategoryInstance("Bed Wars", 230, 10);
    public CategoryInstance zombies = new CategoryInstance("Zombies", 340, 10);
    public CategoryInstance combat = new CategoryInstance("Combat", 450, 10);
    public CategoryInstance render = new CategoryInstance("Render", 560, 10);

    public CategoryInstance[] allCategories = new CategoryInstance[]{keybinds, combat, render, murderMystery, bedwars, zombies, Cigarette.IN_DEV_ENVIRONMENT ? DevWidget.CATEGORY_INSTANCE : null};

    public final AutoClicker COMBAT_AUTOCLICKER = new AutoClicker();
    public final JumpReset COMBAT_JUMP_RESET = new JumpReset();
    public final PerfectHit COMBAT_PERFECT_HIT = new PerfectHit();
    public final io.github.waqfs.module.murdermystery.PlayerESP MYSTERY_PLAYERESP = new io.github.waqfs.module.murdermystery.PlayerESP();

    public Config() {
        this.keybinds.attach(new AddGlassBlock(), new BreakBlock(), new VClip());
        this.murderMystery.attach(MYSTERY_PLAYERESP, new GoldESP());
        this.bedwars.attach(new FireballESP(), new EntityESP(), new DefenseViewer(), new AutoTool(), new Bridger());
        this.zombies.attach(new ZombieESP(), new Aimbot(), new ReviveAura());
        this.combat.attach(COMBAT_AUTOCLICKER, COMBAT_JUMP_RESET, COMBAT_PERFECT_HIT);
        this.render.attach(new PlayerESP(), new ProjectileESP(), new Watermark(), new Notifications(), new ModuleList());
    }
}
