package dev.cigarette.module.combat;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.BaseModule;


public class JumpReset extends BaseModule<ToggleWidget, Boolean> {
    public static final JumpReset INSTANCE = Cigarette.CONFIG.constructModule(new JumpReset("combat.jumpreset", "JumpReset", "Jumps upon taking damage to reduce knockback."), "Combat");

    public JumpReset(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }
}
