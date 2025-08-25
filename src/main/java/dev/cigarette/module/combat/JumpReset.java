package dev.cigarette.module.combat;

import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.BaseModule;


public class JumpReset extends BaseModule<ToggleWidget, Boolean> {
    public static final JumpReset INSTANCE = new JumpReset("combat.jumpreset", "JumpReset", "Jumps upon taking damage to reduce knockback.");

    private JumpReset(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
    }
}
