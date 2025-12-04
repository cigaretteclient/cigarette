package dev.cigarette.module.skyblock;

import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.lib.TextL;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.TickModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RedGifter extends TickModule<ToggleWidget, Boolean> {
    public static final RedGifter INSTANCE = new RedGifter("skyblock.redgifter", "Auto Red Gifter", "Automatically gives and opens red gifts.");

    public UUID playerToGift = null;
    public final ToggleWidget gifter = new ToggleWidget("Run as Gifter", "Automatically gives red gifts to nearby players.").withDefaultState(false);
    public final ToggleWidget opener = new ToggleWidget("Run as Opener", "Automatically opens red gifts you receive.").withDefaultState(true);

    private RedGifter(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(gifter, opener);
        gifter.registerConfigKey(id + ".asgifter");
        opener.registerConfigKey(id + ".asopener");
        gifter.registerModuleCallback((Boolean state) -> {
            if (opener.getRawState() == state) {
                opener.setRawState(!state);
            }
        });
        opener.registerModuleCallback((Boolean state) -> {
            if (gifter.getRawState() == state) {
                gifter.setRawState(!state);
            }
        });
    }

    public static boolean isAGiftOfSomeSorts(ItemStack item) {
        String itemName = TextL.toColorCodedString(item.getName());
        return itemName.endsWith("Red Gift") || itemName.endsWith("White Gift") || itemName.endsWith("Green Gift");
    }

    public static boolean isHoldingAGift() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);
            return isAGiftOfSomeSorts(mainHand);
        }
        return false;
    }

    @Override
    protected void onEnabledTick(@NotNull MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (opener.getRawState()) {
            for (Entity entity : world.getOtherEntities(player, player.getBoundingBox().expand(4))) {
                if (!(entity instanceof ArmorStandEntity armorStand)) continue;

                Text customName = armorStand.getCustomName();
                if (customName == null) continue;

                String name = TextL.toColorCodedString(customName);
                if (!name.equals("§r§e§lCLICK TO OPEN")) continue;

                PlayerEntityL.setRotationVector(player, armorStand.getPos().add(0, 1.5, 0).subtract(player.getEyePos()));
                if (client.interactionManager != null) {
                    client.interactionManager.interactEntity(player, armorStand, Hand.MAIN_HAND);
                    client.interactionManager.interactEntity(player, armorStand, Hand.OFF_HAND);
                }
            }
        } else if (gifter.getRawState() && this.playerToGift != null) {
            if (!RedGifter.isHoldingAGift()) {
                return;
            }
            PlayerEntity targetPlayer = WorldL.getRealPlayerByUUID(this.playerToGift);
            if (targetPlayer == null || player.distanceTo(targetPlayer) > 3) {
                this.playerToGift = null;
                return;
            }
            PlayerEntityL.setRotationVector(player, targetPlayer.getPos().add(0, 1, 0).subtract(player.getEyePos()));
            if (client.interactionManager != null) {
                client.interactionManager.interactEntity(player, targetPlayer, Hand.MAIN_HAND);
                client.interactionManager.interactEntity(player, targetPlayer, Hand.OFF_HAND);
            }
        }
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.playerToGift = null;
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.SKYBLOCK;
    }
}
