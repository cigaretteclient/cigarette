package dev.cigarette.helper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatLogger {
    private void send(Text text) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return;
        player.sendMessage(text, false);
    }

    private MutableText header() {
        MutableText text = Text.empty();
        text.append(Text.literal("Cigarette").withColor(0xFF7700).formatted(Formatting.BOLD));
        text.append(Text.literal(" # ").formatted(Formatting.WHITE, Formatting.BOLD));
        return text;
    }

    public void info(MutableText message) {
        this.send(this.header().append(message.withColor(0xD16100)));
    }

    public void info(String message) {
        this.info(Text.literal(message));
    }
}
