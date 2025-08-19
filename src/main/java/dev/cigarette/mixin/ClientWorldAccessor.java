package dev.cigarette.mixin;

import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientWorld.class)
public interface ClientWorldAccessor {
    /**
     * Accessor for the pendingUpdateManager field in ClientWorld.
     *
     * @return The PendingUpdateManager instance for this client world.
     */
    @Accessor("pendingUpdateManager")
    PendingUpdateManager getPendingUpdateManager();
}
