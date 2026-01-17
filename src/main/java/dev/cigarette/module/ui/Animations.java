package dev.cigarette.module.ui;

import dev.cigarette.gui.AnimationConfig;
import dev.cigarette.gui.widget.SliderWidget;
import dev.cigarette.gui.widget.TextWidget;
import dev.cigarette.gui.widget.ToggleWidget;
import dev.cigarette.module.RenderModule;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.NotNull;

public class Animations extends RenderModule<ToggleWidget, Boolean> {
    public static final Animations INSTANCE = new Animations("ui.animations", "Animations", "Customize animation settings.");

    // GUI Animation Settings
    public final ToggleWidget guiAnimationsEnabled = (ToggleWidget) new ToggleWidget("GUI Animations", "Enable animations for GUI opening/closing.").withDefault(AnimationConfig.isGuiAnimationsEnabled());
    public final SliderWidget guiOpenDuration = new SliderWidget("GUI Open Duration", "How long the GUI takes to open (seconds).").withBounds(0.05f, (float) AnimationConfig.getGuiOpenDuration(), 2.0f).withAccuracy(2);
    public final SliderWidget guiCloseFactor = new SliderWidget("GUI Close Factor", "Multiplier for closing animation duration.").withBounds(0.1f, (float) AnimationConfig.getGuiCloseDurationFactor(), 5.0f).withAccuracy(2);
    public final SliderWidget guiStaggerDuration = new SliderWidget("GUI Stagger Duration", "Delay between staggered animations.").withBounds(0.0f, (float) AnimationConfig.getGuiStaggerDuration(), 0.5f).withAccuracy(3);
    public final SliderWidget guiSlideDistance = new SliderWidget("GUI Slide Distance", "Distance to slide during animation.").withBounds(0f, AnimationConfig.getGuiSlideDistance(), 100f).withAccuracy(0);
    public final SliderWidget guiScaleFactor = new SliderWidget("GUI Scale Factor", "Scale factor for GUI animation (0 = scale from 0, 1 = no scale).").withBounds(0.0f, (float) AnimationConfig.getGuiScaleFactor(), 1.0f).withAccuracy(2);
    public final SliderWidget guiFadeAlpha = new SliderWidget("GUI Fade Alpha", "Starting alpha transparency for fade animation.").withBounds(0.0f, (float) AnimationConfig.getGuiFadeAlpha(), 1.0f).withAccuracy(2);

    // HUD Animation Settings
    public final ToggleWidget hudAnimationsEnabled = (ToggleWidget) new ToggleWidget("HUD Animations", "Enable animations for HUD elements.").withDefault(AnimationConfig.isHudAnimationsEnabled());
    public final SliderWidget hudTransitionDuration = new SliderWidget("HUD Transition Duration", "How long HUD elements take to animate.").withBounds(0.05f, (float) AnimationConfig.getHudTransitionDuration(), 2.0f).withAccuracy(2);

    // Widget Animation Settings
    public final ToggleWidget widgetAnimationsEnabled = (ToggleWidget) new ToggleWidget("Widget Animations", "Enable animations for UI widgets.").withDefault(AnimationConfig.isWidgetAnimationsEnabled());
    public final SliderWidget widgetTransitionDuration = new SliderWidget("Widget Transition Duration", "How long widgets take to animate.").withBounds(0.05f, (float) AnimationConfig.getWidgetTransitionDuration(), 2.0f).withAccuracy(2);

    // Easing Function Settings (stored as string selections would be better, but for now we'll use dropdowns showing the name)
    // Note: These are informational only; actual easing is selected in separate settings
    public final TextWidget guiEasingInfo = new TextWidget("GUI Easing: " + AnimationConfig.getGuiEasing().name());
    public final TextWidget hudEasingInfo = new TextWidget("HUD Easing: " + AnimationConfig.getHudEasing().name());
    public final TextWidget widgetEasingInfo = new TextWidget("Widget Easing: " + AnimationConfig.getWidgetEasing().name());

    private Animations(String id, String name, String tooltip) {
        super(ToggleWidget.ToggleWidgetDisabled::module, id, name, tooltip);
        this.setChildren(
                new TextWidget("GUI Animations").withUnderline(),
                guiAnimationsEnabled, guiOpenDuration, guiCloseFactor, guiStaggerDuration, guiSlideDistance, guiScaleFactor, guiFadeAlpha,
                guiEasingInfo,
                new TextWidget("HUD Animations").withUnderline(),
                hudAnimationsEnabled, hudTransitionDuration,
                hudEasingInfo,
                new TextWidget("Widget Animations").withUnderline(),
                widgetAnimationsEnabled, widgetTransitionDuration,
                widgetEasingInfo
        );
        
        // Register config keys
        guiAnimationsEnabled.registerConfigKey("animations.gui.enabled");
        guiOpenDuration.registerConfigKey("animations.gui.open.duration");
        guiCloseFactor.registerConfigKey("animations.gui.close.factor");
        guiStaggerDuration.registerConfigKey("animations.gui.stagger.duration");
        guiSlideDistance.registerConfigKey("animations.gui.slide.distance");
        guiScaleFactor.registerConfigKey("animations.gui.scale.factor");
        guiFadeAlpha.registerConfigKey("animations.gui.fade.alpha");
        
        hudAnimationsEnabled.registerConfigKey("animations.hud.enabled");
        hudTransitionDuration.registerConfigKey("animations.hud.transition.duration");
        
        widgetAnimationsEnabled.registerConfigKey("animations.widgets.enabled");
        widgetTransitionDuration.registerConfigKey("animations.widgets.transition.duration");
        
        // Set up callbacks to update AnimationConfig when settings change
        setupCallbacks();
    }
    
    private void setupCallbacks() {
        guiAnimationsEnabled.setStateCallback((state) -> AnimationConfig.setGuiAnimationsEnabled((Boolean) state));
        guiOpenDuration.setStateCallback((value) -> AnimationConfig.setGuiOpenDuration(value));
        guiCloseFactor.setStateCallback((value) -> AnimationConfig.setGuiCloseDurationFactor(value));
        guiStaggerDuration.setStateCallback((value) -> AnimationConfig.setGuiStaggerDuration(value));
        guiSlideDistance.setStateCallback((value) -> AnimationConfig.setGuiSlideDistance(value.intValue()));
        guiScaleFactor.setStateCallback((value) -> AnimationConfig.setGuiScaleFactor(value));
        guiFadeAlpha.setStateCallback((value) -> AnimationConfig.setGuiFadeAlpha(value));
        
        hudAnimationsEnabled.setStateCallback((state) -> AnimationConfig.setHudAnimationsEnabled((Boolean) state));
        hudTransitionDuration.setStateCallback((value) -> AnimationConfig.setHudTransitionDuration(value));
        
        widgetAnimationsEnabled.setStateCallback((state) -> AnimationConfig.setWidgetAnimationsEnabled((Boolean) state));
        widgetTransitionDuration.setStateCallback((value) -> AnimationConfig.setWidgetTransitionDuration(value));
    }

    @Override
    protected void onWorldRender(WorldRenderContext ctx, @NotNull MatrixStack matrixStack) {
    }

    @Override
    protected void onEnabledTick(MinecraftClient client, @NotNull ClientWorld world,
            @NotNull ClientPlayerEntity player) {
    }

    @Override
    protected void onDisabledTick(MinecraftClient client) {
    }
}
