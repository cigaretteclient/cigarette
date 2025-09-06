package dev.cigarette.module.murdermystery;

import dev.cigarette.agent.MurderMysteryAgent;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.mixin.KeyBindingAccessor;
import dev.cigarette.module.TickModule;
import dev.cigarette.module.combat.AutoClicker;
import dev.cigarette.module.combat.PlayerAimbot;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

public class AutoBow extends TickModule<ToggleWidget, Boolean> {
    public static final dev.cigarette.module.murdermystery.AutoBow INSTANCE = new dev.cigarette.module.murdermystery.AutoBow("murdermystery.autobow", "AutoBow", "Automatically aims and fires a bow at the murderer.");

    private final SliderWidget shootDelay = new SliderWidget("Shoot Delay", "Maximum delay to draw before shooting").withBounds(20, 45, 60).withAccuracy(1);
    private final ToggleWidget genericMode = new ToggleWidget("Generic Mode", "Use PlayerAimbot target without role checks").withDefaultState(false);
    private final SliderWidget targetRange = new SliderWidget("Max Range", "Maximum range to shoot a target.").withBounds(3, 5, 15);
    private final ToggleWidget prediction = new ToggleWidget("Prediction", "Predict target position for bow").withDefaultState(false);
    private final SliderWidget predictionTicks = new SliderWidget("Prediction Ticks", "Ticks ahead to predict").withBounds(0, 5, 20).withAccuracy(1);
    // AutoBow intentionally relies on PlayerAimbot's aimToleranceDeg to keep a single synchronized tolerance.
    private boolean paOldEnableState, paOldPredictionState, paMMOldState = false;

    private double paOldPredictionTicks;

    private boolean isMurderer = false;

    // Reused as draw ticks counter
    private int aimTicks = 0;
    private int itemSlot;
    private boolean drawing = false; // whether we are currently holding right-click to draw the bow

    private AutoBow(String id, String name, String tooltip) {
        super(ToggleWidget::module, id, name, tooltip);
        this.setChildren(shootDelay, targetRange, genericMode, prediction, predictionTicks);
        shootDelay.registerConfigKey(id + ".shootDelay");
        targetRange.registerConfigKey(id + ".targetRange");
        genericMode.registerConfigKey(id + ".genericMode");
        prediction.registerConfigKey(id + ".prediction");
        predictionTicks.registerConfigKey(id + ".predictionTicks");
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world, @NotNull ClientPlayerEntity player) {
        client.attackCooldown = 0;
        HitResult hitResult = client.crosshairTarget;
        if (hitResult == null) return;
        KeyBinding aimKey = KeyBinding.byId("key.use");
        if (aimKey == null) return;

        LivingEntity activeTarget = PlayerAimbot.INSTANCE.activeTarget;
        if (activeTarget == null) {
            // No target -> reset drawing state
            if (drawing) {
                aimKey.setPressed(false);
                drawing = false;
                aimTicks = 0;
            }
            return;
        }

        // Role / item selection logic (unchanged) -------------------------------------------------
        if (!genericMode.getRawState()) {
            Optional<MurderMysteryAgent.PersistentPlayer> tPlayer = MurderMysteryAgent.getVisiblePlayers().stream().filter((p) -> p.playerEntity == activeTarget).findFirst();
            if (tPlayer.isPresent()) {
                Optional<MurderMysteryAgent.PersistentPlayer> self = MurderMysteryAgent.getVisiblePlayers().stream().filter((p) -> p.playerEntity == MinecraftClient.getInstance().player).findFirst();
                if (self.isPresent()) {
                    if (self.get().role == MurderMysteryAgent.PersistentPlayer.Role.MURDERER) {
                        isMurderer = true;
                        ItemStack i = self.get().itemStack;
                        DefaultedList<ItemStack> is = self.get().playerEntity.getInventory().getMainStacks();
                        for (int ix = 0; ix < is.toArray().length; ix++) {
                            if (is.get(ix).equals(i)) {
                                if (MinecraftClient.getInstance().player != null) {
                                    MinecraftClient.getInstance().player.getInventory().setSelectedSlot(ix);
                                }
                                this.itemSlot = ix;
                            }
                        }
                    } else {
                        isMurderer = false;
                        DefaultedList<ItemStack> is = self.get().playerEntity.getInventory().getMainStacks();
                        for (int ix = 0; ix < is.toArray().length; ix++) {
                            if (MurderMysteryAgent.isDetectiveItem(is.get(ix))) {
                                if (MinecraftClient.getInstance().player != null) {
                                    MinecraftClient.getInstance().player.getInventory().setSelectedSlot(ix);
                                }
                                this.itemSlot = ix;
                            }
                        }
                    }
                }
            }
        }
        // -----------------------------------------------------------------------------------------

        if (!genericMode.getRawState() && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getInventory().getSelectedSlot() != this.itemSlot) {
            // Selected slot changed away from bow/detective item -> reset
            aimKey.setPressed(false);
            drawing = false;
            this.aimTicks = 0;
            return;
        }

        double aimTolerance = Math.toRadians(PlayerAimbot.INSTANCE.aimToleranceDeg.getRawState());
        double yawDiff = Math.toRadians(Math.abs(activeTarget.getYaw() - player.getYaw()));
        double pitchDiff = Math.toRadians(Math.abs(activeTarget.getPitch() - player.getPitch()));
        double angularDistance = Math.hypot(yawDiff, pitchDiff);
        boolean angleAligned = angularDistance <= aimTolerance;
        boolean inRange = player.squaredDistanceTo(activeTarget) <= Math.pow(this.targetRange.getRawState(), 2);
        boolean holdingBow = MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getMainHandStack().getItem().toString().toLowerCase().contains("bow");

        // Core shooting logic: start drawing only when aligned & in range & holding a bow.
        if (angleAligned && inRange && holdingBow) {
            if (!drawing) {
                // Begin drawing
                aimKey.setPressed(true);
                drawing = true;
                aimTicks = 0;
            } else {
                aimTicks++;
                int requiredTicks = this.shootDelay.getRawState().intValue();
                if (aimTicks >= requiredTicks) {
                    // Release to fire
                    aimKey.setPressed(false);
                    drawing = false;
                    aimTicks = 0;
                }
            }
        } else {
            // Not aligned/in range or not holding bow: ensure we are not drawing
            if (drawing) {
                aimKey.setPressed(false);
                drawing = false;
                aimTicks = 0;
            }
        }

        // Safety: if key binding timesPressed accumulates from other logic reset it when idle
        if (!drawing) {
            KeyBindingAccessor accessor = (KeyBindingAccessor) Objects.requireNonNull(KeyBinding.byId("key.use"));
            if (accessor.getTimesPressed() > 0) {
                accessor.setTimesPressed(0);
            }
        }
    }

    @Override
    protected void whenEnabled() {
        this.paOldEnableState = PlayerAimbot.INSTANCE.getRawState();
        this.paMMOldState = PlayerAimbot.INSTANCE.murderMysteryMode.getRawState();
        this.paOldPredictionState = PlayerAimbot.INSTANCE.prediction.getRawState();
        this.paOldPredictionTicks = PlayerAimbot.INSTANCE.predictionTicks.getRawState();

        AutoClicker.INSTANCE.widget.setRawState(false);
        PlayerAimbot.INSTANCE.widget.setRawState(true);
        PlayerAimbot.INSTANCE.prediction.setRawState(prediction.getRawState());
        PlayerAimbot.INSTANCE.predictionTicks.setRawState(predictionTicks.getRawState());
        if (!genericMode.getRawState()) {
            PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(true);
        }
        drawing = false;
        aimTicks = 0;
    }

    @Override
    protected void whenDisabled() {
        PlayerAimbot.INSTANCE.widget.setRawState(this.paOldEnableState);
        PlayerAimbot.INSTANCE.murderMysteryMode.setRawState(this.paMMOldState);
        PlayerAimbot.INSTANCE.prediction.setRawState(this.paOldPredictionState);
        PlayerAimbot.INSTANCE.predictionTicks.setRawState(this.paOldPredictionTicks);
        // Ensure key released
        KeyBinding aimKey = KeyBinding.byId("key.use");
        if (aimKey != null) aimKey.setPressed(false);
        drawing = false;
        aimTicks = 0;
    }
}
