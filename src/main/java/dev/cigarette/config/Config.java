package dev.cigarette.config;

import dev.cigarette.module.bedwars.*;
import dev.cigarette.module.murdermystery.PlayerESP;
import dev.cigarette.Cigarette;
import dev.cigarette.agent.DevWidget;
import dev.cigarette.gui.CategoryInstance;
import dev.cigarette.module.bedwars.*;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.JumpReset;
import dev.cigarette.module.combat.PerfectHit;
import dev.cigarette.module.keybind.AddGlassBlock;
import dev.cigarette.module.keybind.BreakBlock;
import dev.cigarette.module.keybind.VClip;
import dev.cigarette.module.murdermystery.GoldESP;
import dev.cigarette.module.murdermystery.PlayerESP;
import dev.cigarette.module.render.ProjectileESP;
import dev.cigarette.module.ui.GUI;
import dev.cigarette.module.ui.ModuleList;
import dev.cigarette.module.ui.Notifications;
import dev.cigarette.module.ui.Watermark;
import dev.cigarette.module.zombies.Aimbot;
import dev.cigarette.module.zombies.PowerupESP;
import dev.cigarette.module.render.ProjectileESP;
import dev.cigarette.module.zombies.ReviveAura;
import dev.cigarette.module.zombies.ZombieESP;

public class Config {
    public CategoryInstance keybinds = new CategoryInstance("Keybinds", 10, 10);
    public CategoryInstance murderMystery = new CategoryInstance("Murder Mystery", 120, 10);
    public CategoryInstance bedwars = new CategoryInstance("Bed Wars", 230, 10);
    public CategoryInstance zombies = new CategoryInstance("Zombies", 340, 10);
    public CategoryInstance combat = new CategoryInstance("Combat", 450, 10);
    public CategoryInstance render = new CategoryInstance("Render", 560, 10);
    public CategoryInstance ui = new CategoryInstance("UI", 670, 10);

    public CategoryInstance[] allCategories = new CategoryInstance[]{keybinds, combat, render, murderMystery, bedwars, zombies, ui, Cigarette.IN_DEV_ENVIRONMENT ? DevWidget.CATEGORY_INSTANCE : null};

    public final Watermark RENDER_WATERMARK = new Watermark();
    public final AutoClicker COMBAT_AUTOCLICKER = new AutoClicker();
    public final JumpReset COMBAT_JUMP_RESET = new JumpReset();
    public final PerfectHit COMBAT_PERFECT_HIT = new PerfectHit();
    public final PlayerESP MYSTERY_PLAYERESP = new PlayerESP();
    public final Aimbot ZOMBIES_AIMBOT = new Aimbot();

    public Config() {
        this.keybinds.attach(new AddGlassBlock(), new BreakBlock(), new VClip());
        this.murderMystery.attach(MYSTERY_PLAYERESP, new GoldESP());
        this.bedwars.attach(new FireballESP(), new EntityESP(), new DefenseViewer(), new AutoTool(), new Bridger());
        this.zombies.attach(new ZombieESP(), ZOMBIES_AIMBOT, new ReviveAura(), new PowerupESP());
        this.combat.attach(COMBAT_AUTOCLICKER, COMBAT_JUMP_RESET, COMBAT_PERFECT_HIT);
        this.render.attach(new dev.cigarette.module.render.PlayerESP(), new ProjectileESP());
        this.ui.attach(new GUI(), new Notifications(), new ModuleList(), new TargetHUD(), RENDER_WATERMARK);
    }
}
