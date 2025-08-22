package dev.cigarette.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientMixin {
    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int cooldown);

    @Invoker("doItemUse")
    void invokeDoItemUse();
}
