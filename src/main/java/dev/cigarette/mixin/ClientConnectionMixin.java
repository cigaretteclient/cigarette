package dev.cigarette.mixin;

import dev.cigarette.Cigarette;
import dev.cigarette.agent.DevWidget;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.PacketCallbacks;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    private static final String[] C2S_LOG_BLACKLIST = {
            "serverbound/minecraft:pong",
            "serverbound/minecraft:ping_request",
            "serverbound/minecraft:client_tick_end",
            "serverbound/minecraft:keep_alive",
            "serverbound/minecraft:move_player_pos",
    };
    private static final String[] C2S_BLACKLIST = {};
    private static final String[] S2C_LOG_BLACKLIST = {
            "clientbound/minecraft:add_entity",
            "clientbound/minecraft:boss_event",
            "clientbound/minecraft:entity_position_sync",
            "clientbound/minecraft:keep_alive",
            "clientbound/minecraft:level_chunk_with_light",
            "clientbound/minecraft:level_particles",
            "clientbound/minecraft:move_entity_pos",
            "clientbound/minecraft:move_entity_pos_rot",
            "clientbound/minecraft:move_entity_rot",
            "clientbound/minecraft:ping",
            "clientbound/minecraft:pong_response",
            "clientbound/minecraft:remove_entities",
            "clientbound/minecraft:rotate_head",
            "clientbound/minecraft:set_entity_motion",
            "clientbound/minecraft:set_entity_data",
            "clientbound/minecraft:set_equipment",
            "clientbound/minecraft:set_objective",
            "clientbound/minecraft:set_player_team",
            "clientbound/minecraft:update_attributes",
    };
    private static final String[] S2C_BLACKLIST = {};

    @Inject(method = "sendInternal", at = @At("HEAD"), cancellable = true)
    private void sendInternal(Packet<?> packet, @org.jspecify.annotations.Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (!Cigarette.IN_DEV_ENVIRONMENT || !DevWidget.c2sPacketLogging.getRawState()) return;
        String type = packet.getPacketType().toString();
        for (String blacklisted : C2S_LOG_BLACKLIST) {
            if (type.equals(blacklisted)) {
                return;
            }
        }
        for (String blacklisted : C2S_BLACKLIST) {
            if (type.equals(blacklisted)) {
                ci.cancel();
                return;
            }
        }
        System.out.println("\uD83D\uDEAC " + type + " & " + packet);
    }

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static void handlePacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (!Cigarette.IN_DEV_ENVIRONMENT || !DevWidget.s2cPacketLogging.getRawState()) return;
        String type = packet.getPacketType().toString();
        for (String blacklisted : S2C_LOG_BLACKLIST) {
            if (type.equals(blacklisted)) {
                return;
            }
        }
        for (String blacklisted : S2C_BLACKLIST) {
            if (type.equals(blacklisted)) {
                ci.cancel();
                return;
            }
        }
        System.out.println("\uD83D\uDEAC " + type + " & " + packet);
    }
}
