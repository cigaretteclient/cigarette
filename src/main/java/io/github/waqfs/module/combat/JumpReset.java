package io.github.waqfs.module.combat;

import io.github.waqfs.gui.widget.ToggleWidget;
import io.github.waqfs.module.BaseModule;


public class JumpReset extends BaseModule<ToggleWidget, Boolean> {
    protected static final String MODULE_NAME = "JumpReset";
    protected static final String MODULE_TOOLTIP = "Jumps upon taking damage to reduce knockback.";
    protected static final String MODULE_ID = "combat.jumpreset";

    public JumpReset() {
        super(ToggleWidget::module, MODULE_ID, MODULE_NAME, MODULE_TOOLTIP);
    }
}
