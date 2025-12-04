package dev.cigarette.module.skyblock;

import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.TickHelper;
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
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RedGifter extends TickModule<ToggleWidget, Boolean> {
    public static final RedGifter INSTANCE = new RedGifter("skyblock.redgifter", "Auto Red Gifter", "Automatically gives and opens red gifts.");

    public final ToggleWidget gifter = new ToggleWidget("Run as Gifter", "Automatically gives red gifts to nearby players.").withDefaultState(false);
    public final ToggleWidget opener = new ToggleWidget("Run as Opener", "Automatically opens red gifts you receive.").withDefaultState(true);
    private final ToggleWidget cycleInventory = new ToggleWidget("Cycle Inventory", "Swap gifts from within your inventory into the hotbar to continue gifting.").withDefaultState(true);
    private final ToggleWidget cycleSacks = new ToggleWidget("Cycle Sack", "Swap gifts from within your sacks into your inventory to continue gifting.").withDefaultState(false);

    public UUID playerToGift = null;
    private boolean waitingForGUI = false;
    private boolean justClearedInventory = false;

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
        cycleSacks.registerModuleCallback((Boolean state) -> {
            if (state) cycleInventory.setRawState(true);
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

    public static int nextSlotWithGifts(@NotNull ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (isAGiftOfSomeSorts(stack)) {
                return i;
            }
        }
        return -1;
    }

    private void clearInventory(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, int startingSlot) {
        if (startingSlot < 0 || startingSlot >= 36) {
            if (client.interactionManager != null) {
                player.closeHandledScreen();
                waitingForGUI = false;
                justClearedInventory = true;
            }
            return;
        }
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) return;
            for (int slot = startingSlot; slot < 36; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack == ItemStack.EMPTY || isAGiftOfSomeSorts(stack) || slot == 8) continue;
                player.swingHand(Hand.MAIN_HAND);

                int actualSlot = slot < 9 ? slot + 36 : slot;
                client.interactionManager.clickSlot(0, actualSlot, 1, SlotActionType.THROW, player);
                clearInventory(client, player, slot + 1);
                return;
            }
            clearInventory(client, player, -1);
        }, 1);
    }

    @Override
    protected void onEnabledTick(@NotNull MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (waitingForGUI) return;
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
                int slot = nextSlotWithGifts(player);
                if (justClearedInventory && slot != -1) {
                    justClearedInventory = false;
                }
                if (slot == -1) {
                    if (!cycleSacks.getRawState()) {
                        this.playerToGift = null;
                        Cigarette.CHAT_LOGGER.info("No more gifts to give, sack cycling is disabled.");
                        return;
                    }
                    if (justClearedInventory) {
                        justClearedInventory = false;
//                        perform sack check
                    } else {
                        waitingForGUI = true;
                        clearInventory(client, player, 0);
                    }
                    return;
                } else if (slot < 9) {
                    player.getInventory().setSelectedSlot(slot);
                } else {
                    if (client.interactionManager == null || !cycleInventory.getRawState()) {
                        this.playerToGift = null;
                        Cigarette.CHAT_LOGGER.info("No more gifts to give, inventory cycling is disabled.");
                        return;
                    }
                    client.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, player);
                    waitingForGUI = true;
                    TickHelper.scheduleOnce(this, () -> {
                        if (client.interactionManager != null) {
                            player.closeHandledScreen();
                        }
                        waitingForGUI = false;
                    }, 1);
                }
                return;
            }
            PlayerEntity targetPlayer = WorldL.getRealPlayerByUUID(this.playerToGift);
            if (targetPlayer == null || player.distanceTo(targetPlayer) > 3) {
                this.playerToGift = null;
                Cigarette.CHAT_LOGGER.info("Target player is out of range or no longer found.");
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
