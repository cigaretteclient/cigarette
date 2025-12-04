package dev.cigarette.module.skyblock;

import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cigarette.Cigarette;
import dev.cigarette.GameDetector;
import dev.cigarette.gui.widget.KeybindWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.helper.TickHelper;
import dev.cigarette.lib.PlayerEntityL;
import dev.cigarette.lib.Renderer;
import dev.cigarette.lib.TextL;
import dev.cigarette.lib.WorldL;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
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
    private final ToggleWidget cycleSacks = new ToggleWidget("Cycle Sack", "Swap gifts from within your sacks into your inventory to continue gifting.").withDefaultState(false);
    private final ToggleWidget clearInventory = new ToggleWidget("Clear Inventory", "Automatically clears everything besides gifts from your inventory to refill from sacks.").withDefaultState(false);
    private final KeybindWidget clearInventoryNow = new KeybindWidget("Clear Now", "Clears non-gift drops and bad gift drops from your inventory when pressed.");
    private final ToggleWidget setTrashLocation = new ToggleWidget("Set Trash", "Sets the trash drop location and look direction when clearing inventory.");
    private final ToggleWidget setWorthLocation = new ToggleWidget("Set Worth", "Sets the worth drop location and look direction when clearing inventory.");

    public UUID playerToGift = null;
    private boolean waitingForGUI = false;
    private boolean justClearedInventory = false;
    private @Nullable ItemDropLocation trashDropLocation = null;
    private @Nullable ItemDropLocation worthDropLocation = null;

    private RedGifter(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        TextWidget header = new TextWidget("Drop Locations").withUnderline();
        this.setChildren(gifter, opener, cycleInventory, cycleSacks, clearInventory, clearInventoryNow, header, setTrashLocation, setWorthLocation);
        gifter.registerConfigKey(id + ".asgifter");
        opener.registerConfigKey(id + ".asopener");
        cycleInventory.registerConfigKey(id + ".cycleinventory");
        cycleSacks.registerConfigKey(id + ".cyclesacks");
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
        return itemName.endsWith("amond") || itemName.endsWith("Krampus Helmet") || itemName.endsWith("Winter Island") || itemName.endsWith("Cryopowder Shard") || itemName.endsWith("Snowman") || itemName.endsWith("Golden Gift") || itemName.endsWith("Holly Dye") || itemName.endsWith("Talisman");
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

    private void clearInventoryOfTrash(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, int startingSlot, boolean snapAim, boolean worthNext, boolean worthNextSnapAim) {
        if (startingSlot < 0 || startingSlot >= 36) {
            if (worthNext) {
                clearInventoryOfWorth(client, player, 0, worthNextSnapAim, false, false);
                return;
            }
            if (client.interactionManager != null) {
                player.closeHandledScreen();
            }
            waitingForGUI = false;
            justClearedInventory = true;
            return;
        }
        if (snapAim && trashDropLocation != null) {
            if (player.squaredDistanceTo(trashDropLocation.position) < 0.5) {
                PlayerEntityL.setRotationVector(player, trashDropLocation.direction);
                TickHelper.scheduleOnce(this, () -> {
                    clearInventoryOfTrash(client, player, startingSlot, false, worthNext, worthNextSnapAim);
                }, 1);
                return;
            }
        }
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) return;
            for (int slot = startingSlot; slot < 36; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack == ItemStack.EMPTY || !itemIsTrash(stack) || slot == 8) continue;
                player.swingHand(Hand.MAIN_HAND);

                int actualSlot = slot < 9 ? slot + 36 : slot;
                client.interactionManager.clickSlot(0, actualSlot, 1, SlotActionType.THROW, player);
                clearInventoryOfTrash(client, player, slot + 1, false, worthNext, worthNextSnapAim);
                return;
            }
            clearInventoryOfTrash(client, player, -1, false, worthNext, worthNextSnapAim);
        }, 1);
    }

    private void clearInventoryOfWorth(@NotNull MinecraftClient client, @NotNull ClientPlayerEntity player, int startingSlot, boolean snapAim, boolean trashNext, boolean trashNextSnapAim) {
        if (startingSlot < 0 || startingSlot >= 36) {
            if (trashNext) {
                clearInventoryOfTrash(client, player, 0, trashNextSnapAim, false, false);
                return;
            }
            if (client.interactionManager != null) {
                player.closeHandledScreen();
            }
            waitingForGUI = false;
            justClearedInventory = true;
            return;
        }
        if (snapAim && worthDropLocation != null) {
            if (player.squaredDistanceTo(worthDropLocation.position) < 0.5) {
                PlayerEntityL.setRotationVector(player, worthDropLocation.direction);
                TickHelper.scheduleOnce(this, () -> {
                    clearInventoryOfWorth(client, player, startingSlot, false, trashNext, trashNextSnapAim);
                }, 1);
                return;
            }
        }
        TickHelper.scheduleOnce(this, () -> {
            if (client.interactionManager == null) return;
            for (int slot = startingSlot; slot < 36; slot++) {
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack == ItemStack.EMPTY || !itemIsWorthSomething(stack) || slot == 8) continue;
                player.swingHand(Hand.MAIN_HAND);

                int actualSlot = slot < 9 ? slot + 36 : slot;
                client.interactionManager.clickSlot(0, actualSlot, 1, SlotActionType.THROW, player);
                clearInventoryOfWorth(client, player, slot + 1, false, trashNext, trashNextSnapAim);
                return;
            }
            clearInventoryOfWorth(client, player, -1, false, trashNext, trashNextSnapAim);
        }, 1);
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

    @Override
    protected void onEnabledTick(@NotNull MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        if (waitingForGUI) return;
        if (clearInventoryNow.getKeybind().isPhysicallyPressed()) {
            waitingForGUI = true;
            clearInventoryOfTrash(client, player, 0, false, true, false);
            return;
        }
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
            if (!RedGifter.holdingGift()) {
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
                        clearInventoryOfTrash(client, player, 0, true, true, true);
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

    private record ItemDropLocation(Vec3d position, Vec3d direction) {
        public ItemDropLocation(Vec3d position, Vec3d direction) {
            this.position = new Vec3d(position.x, position.y, position.z);
            this.direction = new Vec3d(direction.x, direction.y, direction.z).normalize();
        }
    }
}
