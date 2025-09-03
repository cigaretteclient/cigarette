package dev.cigarette.module.murdermystery;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.mixin.KeyBindingAccessor;
import dev.cigarette.module.TickModule;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.PerfectHit;
import dev.cigarette.module.combat.PlayerAimbot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Optional;

public class AutoBow extends TickModule<ToggleWidget, Boolean> {
    public static final dev.cigarette.module.murdermystery.AutoBow INSTANCE = new dev.cigarette.module.murdermystery.AutoBow("murdermystery.autobow", "AutoBow", "Automatically aims and fires a bow at the murderer.");

    private final SliderWidget shootDelay = new SliderWidget("Shoot Delay", "Maximum range to target players").withBounds(20, 45, 60).withAccuracy(1);

    private boolean paOldEnableState, paMMOldState = false;

    private boolean isMurderer = false;

    private int aimTicks = 0;
    private int itemSlot;

    private AutoBow(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(shootDelay);
        shootDelay.registerConfigKey(id + ".shootDelay");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        client.attackCooldown = 0;
        HitResult hitResult = client.crosshairTarget;
        if (hitResult.getType() == HitResult.Type.MISS) {}
        KeyBinding aimKey = KeyBinding.byId("key.use");
        KeyBindingAccessor aimAccessor = (KeyBindingAccessor) aimKey;
        if (hitResult == null || aimKey == null || aimKey.isPressed()) return;

        LivingEntity activeTarget = PlayerAimbot.INSTANCE.activeTarget;
        if (activeTarget == null) return;
        Optional<MurderMysteryAgent.PersistentPlayer> tPlayer = MurderMysteryAgent.getVisiblePlayers().stream().filter((p) -> p.playerEntity == activeTarget).findFirst();
        if (tPlayer.isPresent()) {
            MurderMysteryAgent.PersistentPlayer.Role targetRole = tPlayer.get().role;
            Optional<MurderMysteryAgent.PersistentPlayer> self = MurderMysteryAgent.getVisiblePlayers().stream().filter((p) -> p.playerEntity == MinecraftClient.getInstance().player).findFirst();
            if (self.isPresent()) {
                if (self.get().role == MurderMysteryAgent.PersistentPlayer.Role.MURDERER) {
                    isMurderer = true;
                    ItemStack i = self.get().itemStack;
                    DefaultedList<ItemStack> is = self.get().playerEntity.getInventory().getMainStacks();
                    for (int ix = 0; ix < is.toArray().length; ix++) {
                        if (is.get(ix).equals(i)) {
                            assert MinecraftClient.getInstance().player != null;
                            MinecraftClient.getInstance().player.getInventory().setSelectedSlot(ix);
                            this.itemSlot = ix;
                        }
                    }
                } else {
                    isMurderer = false;
                    DefaultedList<ItemStack> is = self.get().playerEntity.getInventory().getMainStacks();
                    for (int ix = 0; ix < is.toArray().length; ix++) {
                        if (MurderMysteryAgent.isDetectiveItem(is.get(ix))) {
                            assert MinecraftClient.getInstance().player != null;
                            MinecraftClient.getInstance().player.getInventory().setSelectedSlot(ix);
                            this.itemSlot = ix;
                        }
                    }
                }
            }
        }

        if (this.aimTicks < this.shootDelay.getRawState().intValue()) {
            assert MinecraftClient.getInstance().player != null;
            if (MinecraftClient.getInstance().player.getInventory().getSelectedSlot() != this.itemSlot) {
                aimKey.setPressed(false);
                this.aimTicks = 0;
                return;
            }
            aimKey.setPressed(true);
            aimTicks++;
        } else {
            aimKey.setPressed(false);
            this.aimTicks = 0;
        }
    }

    @Override
    protected void whenEnabled() {
        this.paOldEnableState = PlayerAimbot.INSTANCE.getRawState();
        this.paMMOldState = PlayerAimbot.INSTANCE.murderMysteryMode.getRawState();

        AutoClicker.INSTANCE.widget.setRawState(false);
        PlayerAimbot.INSTANCE.widget.setRawState(true);
        PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(true);
    }

    @Override
    protected void whenDisabled() {
        PlayerAimbot.INSTANCE.widget.setRawState(this.paOldEnableState);
        PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(this.paMMOldState);
    }
}
