package dev.cigarette.config;

import dev.cigarette.Cigarette;
import dev.cigarette.agent.DevWidget;
import dev.cigarette.gui.CategoryInstance;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.JumpReset;
import dev.cigarette.module.combat.PerfectHit;
import dev.cigarette.module.murdermystery.PlayerESP;
import dev.cigarette.module.ui.Watermark;
import dev.cigarette.module.zombies.Aimbot;

public class Config {
    public CategoryInstance[] allCategories = new CategoryInstance[]{Cigarette.IN_DEV_ENVIRONMENT ? DevWidget.CATEGORY_INSTANCE : null};

    public final Watermark RENDER_WATERMARK = new Watermark();
    public final AutoClicker COMBAT_AUTOCLICKER = new AutoClicker();
    public final JumpReset COMBAT_JUMP_RESET = new JumpReset();
    public final PerfectHit COMBAT_PERFECT_HIT = new PerfectHit();
    public final PlayerESP MYSTERY_PLAYERESP = new PlayerESP();
    public final Aimbot ZOMBIES_AIMBOT = new Aimbot();

    public Config() {
    }
}
