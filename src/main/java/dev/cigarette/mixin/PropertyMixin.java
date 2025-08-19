package dev.cigarette.mixin;

import com.mojang.authlib.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Property.class)
public class PropertyMixin {
    @Nullable String signature;

    public boolean hasSignature() {
        return signature != null && signature.length() > 0;
    }
}
