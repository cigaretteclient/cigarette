package dev.cigarette.module.skyblock;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.KeybindHelper;
import dev.cigarette.helper.TickHelper;
import dev.cigarette.lib.*;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.UUID;

public class RedGifter extends RenderModule<ToggleWidget, Boolean> {
    public static final RedGifter INSTANCE = new RedGifter("skyblock.redgifter", "Auto Red Gifter", "Automatically gives and opens red gifts.");

    private static final RenderLayer RENDER_LAYER = RenderLayer.of("cigarette.blockespnophase", 1536, Renderer.BLOCK_ESP_NOPHASE, RenderLayer.MultiPhaseParameters.builder().build(false));

    public final ToggleWidget gifter = new ToggleWidget("Run as Gifter", "Automatically gives red gifts to nearby players.").withDefaultState(false);
    public final ToggleWidget opener = new ToggleWidget("Run as Opener", "Automatically opens red gifts you receive.").withDefaultState(true);
    private final ToggleWidget cycleInventory = new ToggleWidget("Cycle Inventory", "Swap gifts from within your inventory into the hotbar to continue gifting.").withDefaultState(true);
    private final ToggleWidget cycleSacks = new ToggleWidget("Cycle Sack", "Pull gifts from within your sacks into your inventory to continue gifting.").withDefaultState(false);
    private final ToggleWidget cycleStash = new ToggleWidget("Cycle Stash", "Pull gifts from within your stash into your inventory to continue gifting.").withDefaultState(false);
    private final ToggleWidget clearInventory = new ToggleWidget("Clear Inventory", "Automatically clears everything besides gifts from your inventory to refill from sacks.").withDefaultState(false);
    private final KeybindWidget clearInventoryNow = new KeybindWidget("Clear Now", "Clears non-gift drops and bad gift drops from your inventory when pressed.");
    private final ToggleWidget setTrashLocation = new ToggleWidget("Set Trash", "Sets the trash drop location and look direction when clearing inventory.");
    private final ToggleWidget setWorthLocation = new ToggleWidget("Set Worth", "Sets the worth drop location and look direction when clearing inventory.");

    public UUID playerToGift = null;
    private boolean paused = false;
    private boolean stashMayHaveGifts = true;
    private boolean sacksMayHaveGifts = true;
    private @Nullable ItemDropLocation trashDropLocation = null;
    private @Nullable ItemDropLocation worthDropLocation = null;

    private RedGifter(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget("Drop Locations").withUnderline();
        this.setChildren(gifter, opener, cycleInventory, cycleSacks, cycleStash, clearInventory, clearInventoryNow, header, setTrashLocation, setWorthLocation);
        gifter.registerConfigKey(id + ".asgifter");
        opener.registerConfigKey(id + ".asopener");
        cycleInventory.registerConfigKey(id + ".cycleinventory");
        cycleSacks.registerConfigKey(id + ".cyclesacks");
        cycleStash.registerConfigKey(id + ".cyclestash");
        clearInventory.registerConfigKey(id + ".clearinventory");
        clearInventoryNow.registerConfigKey(id + ".clearinventory.now");
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
        cycleStash.registerModuleCallback((Boolean state) -> {
            if (state) clearInventory.setRawState(true);
        });
        clearInventory.registerModuleCallback((Boolean state) -> {
            if (state) cycleSacks.setRawState(true);
        });
        setTrashLocation.registerModuleCallback((Boolean state) -> {
            if (!state) return;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                trashDropLocation = new ItemDropLocation(player.getPos(), player.getRotationVector());
            }
            setTrashLocation.setRawState(false);
        });
        setWorthLocation.registerModuleCallback((Boolean state) -> {
            if (!state) return;
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player != null) {
                worthDropLocation = new ItemDropLocation(player.getPos(), player.getRotationVector());
            }
            setWorthLocation.setRawState(false);
        });
    }

    private void blockNextTicks() {
        this.paused = true;
    }

    private void unblockNextTicks() {
        this.paused = false;
    }

    private void reset() {
        this.playerToGift = null;
        this.paused = false;
        this.stashMayHaveGifts = true;
        this.sacksMayHaveGifts = true;
        TickHelper.unschedule(this);
    }

    public static boolean itemIsGift(ItemStack item) {
        String itemName = TextL.toColorCodedString(item.getName());
        return itemIsGift(itemName);
    }

    public static boolean itemIsGift(String itemName) {
        return itemName.endsWith("Red Gift") || itemName.endsWith("White Gift") || itemName.endsWith("Green Gift");
    }

    public static boolean itemIsWorthSomething(ItemStack item) {
        String itemName = TextL.toColorCodedString(item.getName());
        return itemIsWorthSomething(itemName);
    }

    public static boolean itemIsWorthSomething(String itemName) {
        return itemName.endsWith("Krampus Helmet") || itemName.endsWith("Winter Island") || itemName.endsWith("Cryopowder Shard") || itemName.endsWith("Snowman") || itemName.endsWith("Golden Gift") || itemName.endsWith("Holly Dye") || itemName.endsWith("Talisman");
    }

    public static boolean itemIsTrash(ItemStack item) {
        String itemName = TextL.toColorCodedString(item.getName());
        return !(itemIsGift(itemName) || itemIsWorthSomething(itemName));
    }

    public static boolean holdingGift() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ItemStack mainHand = client.player.getStackInHand(Hand.MAIN_HAND);
            return itemIsGift(mainHand);
        }
        return false;
    }

    public static int nextSlotWithGifts(@NotNull ClientPlayerEntity player) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (itemIsGift(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean switchToWinterSack(@NotNull ClientPlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            String itemName = TextL.toColorCodedString(stack.getName());
            if (itemName.endsWith("Winter Sack")) {
                player.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    private void clearInventoryOfTrash(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, int startingSlot, boolean worthNext) {
        this.blockNextTicks();
        if (startingSlot < 0 || startingSlot >= 36) {
            if (worthNext) {
                clearInventoryOfWorth(client, player, 0, false);
                return;
            }
            closeOpenedGUI(client, player);
            this.unblockNextTicks();
            return;
        }
        if (trashDropLocation != null && player.squaredDistanceTo(trashDropLocation.position) < 0.5) {
            PlayerEntityL.setRotationVector(player, trashDropLocation.direction);
            TickHelper.scheduleOnce(this, () -> clearInventoryOfTrash(client, player, startingSlot, worthNext), 1);
            return;
        }
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) {
                Cigarette.CHAT_LOGGER.error("Tried to clear inventory but the interaction manager did not exist.");
                this.reset();
                return;
            }
            for (int slot = startingSlot; slot < 36; slot++) {
                int actualSlot = slot < 9 ? slot + 36 : slot;
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack == ItemStack.EMPTY || !itemIsTrash(stack) || slot == 7 || slot == 8) continue;

                player.swingHand(Hand.MAIN_HAND);
                client.interactionManager.clickSlot(0, actualSlot, 1, SlotActionType.THROW, player);

                clearInventoryOfTrash(client, player, slot + 1, worthNext);
                return;
            }
            clearInventoryOfTrash(client, player, -1, worthNext);
        }, 1);
    }

    private void clearInventoryOfWorth(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, int startingSlot, boolean trashNext) {
        this.blockNextTicks();
        if (startingSlot < 0 || startingSlot >= 36) {
            if (trashNext) {
                clearInventoryOfTrash(client, player, 0, false);
                return;
            }
            this.closeOpenedGUI(client, player);
            this.unblockNextTicks();
            return;
        }
        if (worthDropLocation != null && player.squaredDistanceTo(worthDropLocation.position) < 0.5) {
            PlayerEntityL.setRotationVector(player, worthDropLocation.direction);
            TickHelper.scheduleOnce(this, () -> clearInventoryOfWorth(client, player, startingSlot, trashNext), 1);
            return;
        }
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) {
                Cigarette.CHAT_LOGGER.error("Tried to clear inventory but the interaction manager did not exist.");
                this.reset();
                return;
            }
            for (int slot = startingSlot; slot < 36; slot++) {
                int actualSlot = slot < 9 ? slot + 36 : slot;
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack == ItemStack.EMPTY || !itemIsWorthSomething(stack) || slot == 7 || slot == 8) continue;

                player.swingHand(Hand.MAIN_HAND);
                client.interactionManager.clickSlot(0, actualSlot, 1, SlotActionType.THROW, player);

                clearInventoryOfWorth(client, player, slot + 1, trashNext);
                return;
            }
            clearInventoryOfWorth(client, player, -1, trashNext);
        }, 1);
    }

    private boolean isInventoryPrettyMuchFull(@NotNull ClientPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        int emptySlots = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) emptySlots++;
        }
        return emptySlots < 4;
    }

    private void closeOpenedGUI(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        if (client.currentScreen == null || client.interactionManager == null) return;
        player.closeHandledScreen();
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
        matrixStack.push();

        Matrix4f matrix = Renderer.getCameraTranslatedMatrix(matrixStack, ctx);
        Tessellator tessellator = Tessellator.getInstance();

        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        if (trashDropLocation != null && !trashDropLocation.position.isInRange(ctx.camera().getPos(), 3)) {
            Renderer.drawCube(buffer, matrix, 0x80FF0000, trashDropLocation.position.add(0, 1.7, 0), 0.6f);
            Renderer.drawFakeLine(buffer, matrix, 0x80FF0000, trashDropLocation.position.add(0, 1.7, 0).toVector3f(), trashDropLocation.position.add(trashDropLocation.direction.multiply(2)).add(0, 1.7, 0).toVector3f(), 0.05f);
        }
        if (worthDropLocation != null && !worthDropLocation.position.isInRange(ctx.camera().getPos(), 3)) {
            Renderer.drawCube(buffer, matrix, 0x8000FF00, worthDropLocation.position.add(0, 1.7, 0), 0.6f);
            Renderer.drawFakeLine(buffer, matrix, 0x8000FF00, worthDropLocation.position.add(0, 1.7, 0).toVector3f(), worthDropLocation.position.add(worthDropLocation.direction.multiply(2)).add(0, 1.7, 0).toVector3f(), 0.05f);
        }

        BuiltBuffer build = buffer.endNullable();
        if (build != null) RENDER_LAYER.draw(build);

        matrixStack.pop();
    }

    /**
     * Looks at and opens any gifts around the player that say "CLICK TO OPEN".
     *
     * @param client The minecraft client.
     * @param world  The client world.
     * @param player The client player.
     */
    private void openTick(@NotNull MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
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

    }

    /**
     * Gifts the target player or triggers refills when none are found in the inventory.
     *
     * @param client The minecraft client.
     * @param player The client player.
     */
    private void giftTick(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        if (!RedGifter.holdingGift()) {
            refillGifts(client, player);
            return;
        }
        PlayerEntity targetPlayer = WorldL.getRealPlayerByUUID(this.playerToGift);
        if (targetPlayer == null) {
            Cigarette.CHAT_LOGGER.error("Target player no longer exists.");
            this.reset();
            return;
        }
        if (player.distanceTo(targetPlayer) > 3) {
            Cigarette.CHAT_LOGGER.error("Target player is out of range.");
            this.reset();
            return;
        }
        PlayerEntityL.setRotationVector(player, targetPlayer.getPos().add(0, 1, 0).subtract(player.getEyePos()));
        if (client.interactionManager == null) {
            Cigarette.CHAT_LOGGER.error("Tried to use a gift but the interaction manager did not exist.");
            this.reset();
            return;
        }
        client.interactionManager.interactEntity(player, targetPlayer, Hand.MAIN_HAND);
        client.interactionManager.interactEntity(player, targetPlayer, Hand.OFF_HAND);
    }

    /**
     * Attempts to swap to the next stack of gifts, exploring the stash and a winter sack for more if enabled.
     *
     * @param client The minecraft client.
     * @param player The client player.
     */
    private void refillGifts(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        int slot = nextSlotWithGifts(player);
        if (slot == -1) {
            if (this.isInventoryPrettyMuchFull(player) && this.clearInventory.getRawState()) {
                clearInventoryOfTrash(client, player, 0, true);
                return;
            }
            if (cycleStash.getRawState() && stashMayHaveGifts) {
                refillFromStash(client, player);
                return;
            } else if (cycleSacks.getRawState() && sacksMayHaveGifts) {
                refillFromSacks(client, player);
                return;
            }
        } else if (slot < 9) {
            player.getInventory().setSelectedSlot(slot);
            return;
        } else if (cycleInventory.getRawState()) {
            refillFromInventory(client, player, slot);
            return;
        }
        Cigarette.CHAT_LOGGER.info("No more gifts to give.");
        this.reset();
    }

    /**
     * Forges packets to swap an inventory slot to the hotbar.
     *
     * @param client The minecraft client.
     * @param player The client player.
     * @param slot   The slot in the inventory to swap into the hotbar.
     */
    private void refillFromInventory(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, int slot) {
        this.blockNextTicks();
        if (client.interactionManager == null) {
            Cigarette.CHAT_LOGGER.error("Attempted to swap gifts from inventory but the interaction manager did not exist.");
            this.reset();
            return;
        }
        client.interactionManager.clickSlot(0, slot, 0, SlotActionType.SWAP, player);
        TickHelper.scheduleOnce(this, () -> {
            this.closeOpenedGUI(client, player);
            this.unblockNextTicks();
        }, 1);
    }

    /**
     * Opens and pulls gifts out from the material stash.
     *
     * @param client The minecraft client.
     * @param player The client player.
     */
    private void refillFromStash(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        this.blockNextTicks();
        player.networkHandler.sendChatCommand("viewstash material");
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) {
                Cigarette.CHAT_LOGGER.error("Attempted to pull gifts from stash but the interaction manager did not exist.");
                this.reset();
                return;
            }
            Screen gui = client.currentScreen;
            if (gui == null) {
                Cigarette.CHAT_LOGGER.error("Attempted to pull gifts from stash but no GUI was opened.");
                Cigarette.CHAT_LOGGER.info("Assuming stash is empty.");
                stashMayHaveGifts = false;
                this.unblockNextTicks();
                return;
            }
            String guiTitle = TextL.toColorCodedString(gui.getTitle());
            if (!guiTitle.equals("§rView Stash")) {
                Cigarette.CHAT_LOGGER.error("Attempted to pull gifts from stash but an unknown GUI was opened.");
                this.reset();
                return;
            }
            ScreenHandler screenHandler = player.currentScreenHandler;
            for (Slot slot : screenHandler.slots) {
                ItemStack stack = slot.getStack();
                if (!itemIsGift(stack)) continue;
                client.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.PICKUP, player);
                TickHelper.scheduleOnce(this, () -> {
                    this.closeOpenedGUI(client, player);
                    this.unblockNextTicks();
                }, 1);
                Cigarette.CHAT_LOGGER.info("Refilled from stash.");
                return;
            }
        }, 20);
        Cigarette.CHAT_LOGGER.error("Stash does not contain any gifts.");
        stashMayHaveGifts = false;
        this.unblockNextTicks();
    }

    /**
     * Opens and pulls gifts out from a winter sack.
     *
     * @param client The minecraft client.
     * @param player The client player.
     */
    private void refillFromSacks(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player) {
        this.blockNextTicks();
        boolean switchedToSack = this.switchToWinterSack(player);
        if (!switchedToSack) {
            Cigarette.CHAT_LOGGER.error("Failed to switch to a winter sack. Make sure one is in the last hotbar slot.");
            this.reset();
            return;
        }
        KeybindHelper.KEY_USE_ITEM.holdForTicks(1);
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) {
                Cigarette.CHAT_LOGGER.error("Attempted to pull gifts from sacks but the interaction manager did not exist.");
                this.reset();
                return;
            }
            Screen gui = client.currentScreen;
            if (gui == null) {
                Cigarette.CHAT_LOGGER.error("Attempted to pull gifts from sacks but no GUI was opened.");
                this.reset();
                return;
            }
            String guiTitle = TextL.toColorCodedString(gui.getTitle());
            if (!guiTitle.equals("§rWinter Sack")) {
                Cigarette.CHAT_LOGGER.error("Attempted to pull gifts from sacks but an unknown GUI was opened.");
                this.reset();
                return;
            }
            ScreenHandler screenHandler = player.currentScreenHandler;
            for (Slot slot : screenHandler.slots) {
                ItemStack stack = slot.getStack();
                if (!itemIsGift(stack)) continue;
                for (String line : ItemStackL.getLoreLines(stack)) {
                    if (!line.contains("Stored")) continue;
                    if (!line.contains("§e")) continue;
                    client.interactionManager.clickSlot(screenHandler.syncId, slot.id, 0, SlotActionType.PICKUP, player);
                    TickHelper.scheduleOnce(this, () -> {
                        this.closeOpenedGUI(client, player);
                        this.unblockNextTicks();
                    }, 1);
                    Cigarette.CHAT_LOGGER.info("Refilled from sacks.");
                    return;
                }
            }
            Cigarette.CHAT_LOGGER.error("Sack does not contain any gifts.");
            stashMayHaveGifts = false;
            this.closeOpenedGUI(client, player);
            this.unblockNextTicks();
        }, 20);
    }

    @Override
    protected void onEnabledTick(@NotNull MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (this.paused) return;
        if (clearInventoryNow.getKeybind().isPhysicallyPressed()) {
            clearInventoryOfTrash(client, player, 0, true);
            return;
        }
        if (opener.getRawState()) openTick(client, world, player);
        else if (gifter.getRawState()) giftTick(client, player);
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
        this.playerToGift = null;
    }

    @Override
    public boolean inValidGame() {
        return GameDetector.rootGame == GameDetector.ParentGame.SKYBLOCK;
    }

    private record ItemDropLocation(Vec3d position, Vec3d direction) {
        public ItemDropLocation(Vec3d position, Vec3d direction) {
            this.position = new Vec3d(position.x, position.y, position.z);
            this.direction = new Vec3d(direction.x, direction.y, direction.z).normalize();
        }
    }
}
