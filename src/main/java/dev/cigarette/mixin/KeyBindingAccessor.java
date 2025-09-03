package dev.cigarette.mixin;

import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBinding.class)
public interface KeyBindingAccessor {
    @Accessor("timesPressed")
    public void setTimesPressed(int timesPressed);

    @Accessor("")

    @Accessor
    int getTimesPressed();
}
