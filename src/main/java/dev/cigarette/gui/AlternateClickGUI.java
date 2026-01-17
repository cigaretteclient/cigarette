package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.*;
import dev.cigarette.lib.Color;
import dev.cigarette.module.BaseModule;
import dev.cigarette.gui.CigaretteScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class AlternateClickGUI extends Screen {
    
    // Private helper to get colors dynamically
    private int getPrimaryColor() { return AnimationConfig.getPrimaryColor(); }
    private int getSecondaryColor() { return AnimationConfig.getSecondaryColor(); }
    private int getPrimaryTextColor() { return AnimationConfig.getPrimaryTextColor(); }
    private int getSecondaryTextColor() { return AnimationConfig.getSecondaryTextColor(); }
    private int getBackgroundColor() { return AnimationConfig.getBackgroundColor(); }
    private int getDarkBackgroundColor() { return AnimationConfig.getDarkBackgroundColor(); }
    private int getHoverColor() { return AnimationConfig.getHoverColor(); }
    private int getSelectionColor() { return AnimationConfig.getSelectionColor(); }
    private int getCheckboxBackgroundColor() { return AnimationConfig.getCheckboxBackgroundColor(); }
    private int getCheckboxBorderColor() { return AnimationConfig.getCheckboxBorderColor(); }
    private int getCheckboxCheckColor() { return AnimationConfig.getCheckboxCheckColor(); }

    private Screen parent = null;

    // Animation state
    private boolean opening = false;
    private boolean closing = false;
    private long animStartNanos = 0L;
    private double animProgress = 1.0; // 0.0 = closed, 1.0 = fully open

    // Container dimensions
    private static final int CONTAINER_WIDTH = 380;
    private static final int CONTAINER_HEIGHT = 280;
    private static final int SIDEBAR_WIDTH = 90;
    private static final int HEADER_HEIGHT = 22;
    private static final int CATEGORY_HEIGHT = 20;
    private static final int MODULE_HEIGHT = 18;

    // Container position (centered by default)
    private int containerX;
    private int containerY;

    // Dragging state
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    // Category selection
    private int selectedCategoryIndex = 0;
    private final List<CategoryInstance> categories = new ArrayList<>();
    private final AnimationHelper categoryAnimation = new AnimationHelper(0, 0.3);
    private final AnimationHelper categoryBackgroundAnimation = new AnimationHelper(0, 0.35);

    // Module list for selected category
    private final List<ModuleEntry> moduleEntries = new ArrayList<>();
    private int contentScrollOffset = 0;
    private @Nullable ModuleEntry expandedModule = null;

    private static class ModuleEntry {
        final BaseModule<?, ?> module;
        final BaseWidget<?> widget;
        boolean expanded = false;
        int settingsHeight = 0;
        final AnimationHelper expansionAnimation = new AnimationHelper(0, 0.25);
        final AnimationHelper toggleAnimation = new AnimationHelper(0, 0.2);
        public String key;

        // Track widget bounds for interaction
        final List<WidgetBounds> settingWidgets = new ArrayList<>();

        ModuleEntry(BaseModule<?, ?> module) {
            this.module = module;
            this.widget = module.wrapper != null ? module.wrapper : module.widget;
            this.key = module.toString() + "-" + module.hashCode();
        }
    }
    
    private static class WidgetBounds {
        final BaseWidget<?> widget;
        int x, y, width, height;
        
        WidgetBounds(BaseWidget<?> widget) {
            this.widget = widget;
        }
    }

    public AlternateClickGUI() {
        super(Text.literal("Cigarette Client"));
    }

    public void setParent(@Nullable Screen parent) {
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.categories.clear();
        this.moduleEntries.clear();

        // Center container
        containerX = (width - CONTAINER_WIDTH) / 2;
        containerY = (height - CONTAINER_HEIGHT) / 2;

        // Start opening animation if animations are enabled
        if (AnimationConfig.isGuiAnimationsEnabled()) {
            this.opening = true;
            this.closing = false;
            this.animStartNanos = System.nanoTime();
            this.animProgress = 0.0;
        } else {
            this.opening = false;
            this.closing = false;
            this.animProgress = 1.0;
        }

        // Load categories
        for (CategoryInstance categoryInstance : Cigarette.CONFIG.CATEGORIES.values()) {
            if (categoryInstance != null) {
                categories.add(categoryInstance);
            }
        }

        // Select first category
        if (!categories.isEmpty()) {
            selectCategory(0);
        }
    }

    private void selectCategory(int index) {
        if (index >= 0 && index < categories.size()) {
            selectedCategoryIndex = index;
            categoryAnimation.setTarget(index * CATEGORY_HEIGHT);
            // Reset background animation when category changes
            categoryBackgroundAnimation.set(0);
            categoryBackgroundAnimation.setTarget(0);
            moduleEntries.clear();
            contentScrollOffset = 0;
            expandedModule = null;

            CategoryInstance category = categories.get(index);
            List<BaseModule<?, ?>> sortedModules = new ArrayList<>(category.children);
            sortedModules.sort(Comparator.comparing(m -> m.widget.getMessage().getString()));

            for (BaseModule<?, ?> module : sortedModules) {
                ModuleEntry entry = new ModuleEntry(module);
                // Initialize toggle animation based on current state
                if (isModuleEnabled(entry)) {
                    entry.toggleAnimation.set(1.0);
                }
                moduleEntries.add(entry);
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();

        // Check if clicking header to drag
        if (mx >= containerX && mx < containerX + CONTAINER_WIDTH &&
            my >= containerY && my < containerY + HEADER_HEIGHT) {
            dragging = true;
            dragOffsetX = (int) mx - containerX;
            dragOffsetY = (int) my - containerY;
            return true;
        }

        // Check sidebar category clicks
        int catY = containerY + HEADER_HEIGHT;
        for (int i = 0; i < categories.size(); i++) {
            if (mx >= containerX && mx < containerX + SIDEBAR_WIDTH &&
                my >= catY && my < catY + CATEGORY_HEIGHT) {
                selectCategory(i);
                return true;
            }
            catY += CATEGORY_HEIGHT;
        }

        // Check module clicks in content area
        int contentX = containerX + SIDEBAR_WIDTH;
        int contentY = containerY + HEADER_HEIGHT;
        int contentWidth = CONTAINER_WIDTH - SIDEBAR_WIDTH;
        int contentHeight = CONTAINER_HEIGHT - HEADER_HEIGHT;

        if (mx >= contentX && mx < contentX + contentWidth &&
            my >= contentY && my < contentY + contentHeight) {
            
            int moduleY = contentY - contentScrollOffset;
            for (ModuleEntry entry : moduleEntries) {
                int entryHeight = MODULE_HEIGHT + (entry.expanded ? entry.settingsHeight : 0);
                
                // Check module header click
                if (my >= Math.max(moduleY, contentY) && my < Math.min(moduleY + MODULE_HEIGHT, contentY + contentHeight) &&
                    moduleY + MODULE_HEIGHT > contentY && moduleY < contentY + contentHeight) {
                    
                    // Check if clicking on toggle area (right side)
                    if (mx >= contentX + contentWidth - 20) {
                        // Toggle module enabled state
                        toggleModule(entry);
                        return true;
                    }
                    
                    // Toggle settings expansion
                    if (entry.expanded) {
                        entry.expanded = false;
                        expandedModule = null;
                    } else {
                        if (expandedModule != null) {
                            expandedModule.expanded = false;
                        }
                        entry.expanded = true;
                        expandedModule = entry;
                        entry.settingsHeight = calculateSettingsHeight(entry);
                    }
                    return true;
                }
                
                // Check settings area click
                if (entry.expanded && my >= moduleY + MODULE_HEIGHT && my < moduleY + entryHeight) {
                    // Check clicks on individual setting widgets
                    for (WidgetBounds bounds : entry.settingWidgets) {
                        if (mx >= bounds.x && mx < bounds.x + bounds.width &&
                            my >= bounds.y && my < bounds.y + bounds.height) {
                            // Forward click to the widget
                            if (handleWidgetClick(bounds.widget, click, doubled, bounds)) {
                                return true;
                            }
                        }
                    }
                }
                
                moduleY += entryHeight;
            }
        }

        return false;
    }
    
    private boolean handleWidgetClick(BaseWidget<?> widget, Click click, boolean doubled, WidgetBounds bounds) {
        if (widget instanceof DropdownWidget dropdown) {
            // Handle dropdown expand/collapse first (ColorDropdownWidget is a DropdownWidget)
            // Set position and dimensions so isMouseOver() works correctly
            dropdown.setX(bounds.x);
            dropdown.setY(bounds.y);
            dropdown.setWidth(bounds.width);
            dropdown.setHeight(bounds.height);
            return dropdown.mouseClicked(click, doubled);
        } else if (widget instanceof SliderWidget slider && !slider.disabled) {
            // Handle slider interaction
            return slider.mouseClicked(click, doubled);
        } else if (widget instanceof ColorWheelWidget colorWheel && !colorWheel.disabled) {
            return colorWheel.mouseClicked(click, doubled);
        } else if (widget instanceof ToggleKeybindWidget toggleKeybind) {
            // Set position and dimensions for proper click detection
            toggleKeybind.setX(bounds.x);
            toggleKeybind.setY(bounds.y);
            toggleKeybind.setWidth(bounds.width);
            toggleKeybind.setHeight(bounds.height);
            toggleKeybind.widget.setX(bounds.x);
            toggleKeybind.widget.setY(bounds.y);
            toggleKeybind.widget.setWidth(bounds.width);
            toggleKeybind.widget.setHeight(bounds.height);
            return toggleKeybind.mouseClicked(click, doubled);
        } else if (widget instanceof KeybindWidget keybind) {
            // Set position and dimensions for proper click detection
            keybind.setX(bounds.x);
            keybind.setY(bounds.y);
            keybind.setWidth(bounds.width);
            keybind.setHeight(bounds.height);
            return keybind.mouseClicked(click, doubled);
        } else if (widget instanceof ToggleWidget) {
            // Toggle the boolean value
            if (!widget.isStateless()) {
                Object state = widget.getRawState();
                if (state instanceof Boolean b) {
                    @SuppressWarnings("unchecked")
                    BaseWidget<Boolean> bw = (BaseWidget<Boolean>) widget;
                    bw.setRawState(!b);
                    return true;
                }
            }
        }
        
        return false;
    }

    private void toggleModule(ModuleEntry entry) {
        // Toggle the actual module widget, not the wrapper
        BaseWidget<?> moduleWidget = entry.module.widget;
        
        if (moduleWidget == null || moduleWidget.isStateless()) {
            return;
        }
        
        Object state = moduleWidget.getRawState();
        if (state instanceof Boolean b) {
            @SuppressWarnings("unchecked")
            BaseWidget<Boolean> bw = (BaseWidget<Boolean>) moduleWidget;
            bw.setRawState(!b);
            // Animate toggle state
            entry.toggleAnimation.setTarget(b ? 0.0 : 1.0);
        }
    }

    private int calculateSettingsHeight(ModuleEntry entry) {
        entry.settingWidgets.clear();
        
        if (entry.widget instanceof DropdownWidget dropdown) {
            return calculateWidgetHeightRecursively(dropdown.getChildren(), 0);
        }
        return 50;
    }
    
    /**
     * Recursively calculates the total height needed for all widgets at any nesting depth.
     */
    private int calculateWidgetHeightRecursively(Map<String, BaseWidget<?>> widgets, int depth) {
        int height = 8; // Top padding
        
        for (var child : widgets.values()) {
            if (child instanceof ColorWheelWidget) {
                height += 14 + 160 + 10; // Label + wheel + spacing
            } else if (child instanceof SliderWidget) {
                height += 14 + 12 + 6; // Label + slider + spacing
            } else if (child instanceof KeybindWidget || child instanceof ToggleKeybindWidget) {
                height += 20 + 6; // Widget height + spacing
            } else if (child instanceof ToggleWidget) {
                height += 18 + 4; // Widget height + spacing
            } else if (child instanceof ColorSquareWidget) {
                height += 16 + 4; // Widget height + spacing
            } else if (child instanceof DropdownWidget innerDropdown) {
                height += 20; // Header height
                // Add height for expanded children
                height += calculateWidgetHeightRecursively(innerDropdown.getChildren(), depth + 1);
            } else {
                height += 22; // Default for other widgets
            }
        }
        
        return height + 8; // Bottom padding
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging) {
            containerX = (int) click.x() - dragOffsetX;
            containerY = (int) click.y() - dragOffsetY;
            containerX = Math.max(0, Math.min(containerX, width - CONTAINER_WIDTH));
            containerY = Math.max(0, Math.min(containerY, height - CONTAINER_HEIGHT));
            return true;
        }
        
        // Check if any widget is being dragged
        if (expandedModule != null) {
            for (WidgetBounds bounds : expandedModule.settingWidgets) {
                if (bounds.widget instanceof SliderWidget slider && !slider.disabled) {
                    if (slider.mouseDragged(click, offsetX, offsetY)) {
                        return true;
                    }
                } else if (bounds.widget instanceof ColorWheelWidget colorWheel && !colorWheel.disabled) {
                    if (colorWheel.mouseDragged(click, offsetX, offsetY)) {
                        return true;
                    }
                } else if (bounds.widget instanceof DropdownWidget dropdown) {
                    // Forward drag to nested dropdown which may contain ColorWheelWidget
                    dropdown.setX(bounds.x);
                    dropdown.setY(bounds.y);
                    dropdown.setWidth(bounds.width);
                    dropdown.setHeight(bounds.height);
                    if (dropdown.mouseDragged(click, offsetX, offsetY)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        
        // Forward to widgets
        if (expandedModule != null) {
            for (WidgetBounds bounds : expandedModule.settingWidgets) {
                if (bounds.widget instanceof SliderWidget slider) {
                    slider.mouseReleased(click);
                } else if (bounds.widget instanceof ColorWheelWidget colorWheel) {
                    colorWheel.mouseReleased(click);
                } else if (bounds.widget instanceof DropdownWidget dropdown) {
                    // Forward release to nested dropdown
                    dropdown.setX(bounds.x);
                    dropdown.setY(bounds.y);
                    dropdown.setWidth(bounds.width);
                    dropdown.setHeight(bounds.height);
                    dropdown.mouseReleased(click);
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentX = containerX + SIDEBAR_WIDTH;
        int contentY = containerY + HEADER_HEIGHT;
        int contentWidth = CONTAINER_WIDTH - SIDEBAR_WIDTH;
        int contentHeight = CONTAINER_HEIGHT - HEADER_HEIGHT;

        if (mouseX >= contentX && mouseX < contentX + contentWidth &&
            mouseY >= contentY && mouseY < contentY + contentHeight) {
            
            int totalHeight = getTotalContentHeight();
            int maxScroll = Math.max(0, totalHeight - contentHeight);
            
            contentScrollOffset -= (int) (verticalAmount * 15);
            contentScrollOffset = Math.max(0, Math.min(contentScrollOffset, maxScroll));
            return true;
        }
        return false;
    }

    private int getTotalContentHeight() {
        int total = 0;
        for (ModuleEntry entry : moduleEntries) {
            total += MODULE_HEIGHT + (entry.expanded ? entry.settingsHeight : 0);
        }
        return total;
    }

    @Override
    public void close() {
        assert this.client != null;
        
        if (AnimationConfig.isGuiAnimationsEnabled() && !closing) {
            // Start closing animation
            this.opening = false;
            this.closing = true;
            this.animStartNanos = System.nanoTime();
        } else if (!AnimationConfig.isGuiAnimationsEnabled()) {
            // No animation, close immediately
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        // If a keybind widget is actively capturing input, forward the key press to it
        if (CigaretteScreen.bindingKey != null) {
            if (CigaretteScreen.bindingKey.keyPressed(input)) {
                return true;
            }
        }

        switch (input.getKeycode()) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Update opening/closing animation
        if (opening || closing) {
            double elapsed = (System.nanoTime() - animStartNanos) / 1_000_000_000.0;
            double duration = opening ? AnimationConfig.getGuiOpenDuration() : 
                             (AnimationConfig.getGuiOpenDuration() * AnimationConfig.getGuiCloseDurationFactor());
            
            double t = Math.min(1.0, elapsed / duration);
            double eased = AnimationConfig.getGuiEasing().apply(t);
            
            if (opening) {
                animProgress = eased;
                if (t >= 1.0) {
                    opening = false;
                    animProgress = 1.0;
                }
            } else if (closing) {
                animProgress = 1.0 - eased;
                if (t >= 1.0) {
                    // Animation complete, actually close
                    assert this.client != null;
                    this.client.setScreen(parent);
                    return;
                }
            }
        }
        
        // Update animations
        categoryAnimation.update(deltaTicks);
        categoryBackgroundAnimation.update(deltaTicks);
        for (ModuleEntry entry : moduleEntries) {
            entry.expansionAnimation.update(deltaTicks);
            entry.toggleAnimation.update(deltaTicks);
            
            // Update expansion animation target
            if (entry.expanded && entry.expansionAnimation.get() < entry.settingsHeight) {
                entry.expansionAnimation.setTarget(entry.settingsHeight);
            } else if (!entry.expanded && entry.expansionAnimation.get() > 0) {
                entry.expansionAnimation.setTarget(0);
            }
            
            // Update toggle animation target - check every frame to sync with settings toggles
            boolean shouldBeOn = isModuleEnabled(entry);
            double targetToggle = shouldBeOn ? 1.0 : 0.0;
            entry.toggleAnimation.setTarget(targetToggle);
        }
        
        // Dim background with animated alpha
        int bgAlpha = (int) (0x80 * animProgress);
        context.fill(0, 0, width, height, (bgAlpha << 24));

        // Apply transformations if animating
        context.getMatrices().pushMatrix();
        
        if (animProgress < 1.0) {
            // Calculate center point for scaling
            double centerX = containerX + CONTAINER_WIDTH / 2.0;
            double centerY = containerY + CONTAINER_HEIGHT / 2.0;
            
            // Apply scaling
            double scaleFactor = AnimationConfig.getGuiScaleFactor() + 
                               (1.0 - AnimationConfig.getGuiScaleFactor()) * animProgress;
            context.getMatrices().translate((float) centerX, (float) centerY);
            context.getMatrices().scale((float) scaleFactor, (float) scaleFactor, new Matrix3x2f().identity());
            context.getMatrices().translate((float) -centerX, (float) -centerY);
            
            // Apply slide animation
            double slideDistance = AnimationConfig.getGuiSlideDistance() * (1.0 - animProgress);
            context.getMatrices().translate(0, (float) slideDistance);
        }
        
        // Calculate alpha for fade effect
        int fadeAlpha = (int) (255 * (AnimationConfig.getGuiFadeAlpha() + 
                                      (1.0 - AnimationConfig.getGuiFadeAlpha()) * animProgress));
        
        // Container background with fade
        int containerBg = (fadeAlpha << 24) | (getDarkBackgroundColor() & 0x00FFFFFF);
        context.fill(containerX, containerY, containerX + CONTAINER_WIDTH, containerY + CONTAINER_HEIGHT, containerBg);

        // Header gradient with fade
        int[] headerGradient = ColorScheme.getCategoryHeaderGradient();
        int headerStart = (fadeAlpha << 24) | (headerGradient[0] & 0x00FFFFFF);
        int headerEnd = (fadeAlpha << 24) | (headerGradient[1] & 0x00FFFFFF);
        GradientRenderer.renderHorizontalGradient(context, containerX, containerY, containerX + CONTAINER_WIDTH, containerY + HEADER_HEIGHT, headerStart, headerEnd);
        int textColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "Cigarette", containerX + CONTAINER_WIDTH / 2, containerY + 6, textColor);

        // Sidebar with fade
        int sidebarBg = (fadeAlpha << 24) | (getBackgroundColor() & 0x00FFFFFF);
        context.fill(containerX, containerY + HEADER_HEIGHT, containerX + SIDEBAR_WIDTH, containerY + CONTAINER_HEIGHT, sidebarBg);

        // Categories with selection indicator
        int catY = containerY + HEADER_HEIGHT;
        
        // Draw animated selection indicator with fade
        int indicatorY = containerY + HEADER_HEIGHT + categoryAnimation.getInt();
        int indicatorColor = (fadeAlpha << 24) | (getPrimaryColor() & 0x00FFFFFF);
        context.fill(containerX, indicatorY, containerX + 2, indicatorY + CATEGORY_HEIGHT, indicatorColor);
        
        // Animate the category background scaling after the indicator settles
        // Only start the background animation when the indicator is close to its target
        double indicatorProgress = categoryAnimation.getProgress();
        if (indicatorProgress > 0.7 && categoryBackgroundAnimation.get() == 0) {
            // Start left-to-right scaling
            categoryBackgroundAnimation.setTarget(SIDEBAR_WIDTH - 4);
        }
        
        for (int i = 0; i < categories.size(); i++) {
            CategoryInstance category = categories.get(i);
            boolean isSelected = i == selectedCategoryIndex;
            boolean isHovered = mouseX >= containerX && mouseX < containerX + SIDEBAR_WIDTH && mouseY >= catY && mouseY < catY + CATEGORY_HEIGHT;

            if (isSelected) {
                // Render left-to-right scaling rounded rect for selected category
                int rectWidth = categoryBackgroundAnimation.getInt();
                int rectX = containerX + 2; // Start after the indicator bar
                int rectY = catY + 2;
                int rectHeight = CATEGORY_HEIGHT - 4;
                
                if (rectWidth > 0) {
                    // Draw rectangular selection with animated width
                    int rectColor = (fadeAlpha << 24) | (getPrimaryColor() & 0x00FFFFFF);
                    context.fill(rectX, rectY, rectX + rectWidth, rectY + rectHeight, rectColor);
                }
            } else if (isHovered) {
                int hoverColor = (fadeAlpha << 24) | (getHoverColor() & 0x00FFFFFF);
                context.fill(containerX, catY, containerX + SIDEBAR_WIDTH, catY + CATEGORY_HEIGHT, hoverColor);
            }

            String name = category.widget.getHeaderName();
            int baseCatTextColor = isSelected ? 0xFFFFFFFF : getPrimaryTextColor();
            int catTextColor = (fadeAlpha << 24) | (baseCatTextColor & 0x00FFFFFF);
            context.drawTextWithShadow(textRenderer, name, containerX + 6, catY + 5, catTextColor);
            catY += CATEGORY_HEIGHT;
        }

        // Content area
        int contentX = containerX + SIDEBAR_WIDTH;
        int contentY = containerY + HEADER_HEIGHT;
        int contentWidth = CONTAINER_WIDTH - SIDEBAR_WIDTH;
        int contentHeight = CONTAINER_HEIGHT - HEADER_HEIGHT;

        int contentBg = (fadeAlpha << 24) | (getDarkBackgroundColor() & 0x00FFFFFF);
        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, contentBg);

        // Scissor for content
        Scissor.pushExclusive(context, contentX, contentY, contentX + contentWidth, contentY + contentHeight);

        int moduleY = contentY - contentScrollOffset;
        for (ModuleEntry entry : moduleEntries) {
            int animatedSettingsHeight = entry.expansionAnimation.getInt();
            int entryHeight = MODULE_HEIGHT + animatedSettingsHeight;
            
            if (moduleY + entryHeight > contentY && moduleY < contentY + contentHeight) {
                boolean isHovered = mouseX >= contentX && mouseX < contentX + contentWidth && mouseY >= moduleY && mouseY < moduleY + MODULE_HEIGHT;

                // Module header with expansion transition
                double expansionProgress = entry.expansionAnimation.getProgress();
                int baseHeaderBg = entry.expanded ? 
                    interpolateColor(isHovered ? getHoverColor() : getBackgroundColor(), getSecondaryColor(), expansionProgress) :
                    (isHovered ? getHoverColor() : getBackgroundColor());
                int headerBg = (fadeAlpha << 24) | (baseHeaderBg & 0x00FFFFFF);
                context.fill(contentX, moduleY, contentX + contentWidth, moduleY + MODULE_HEIGHT, headerBg);

                // Module name
                String moduleName = entry.module.widget.getMessage().getString();
                int moduleNameColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                context.drawTextWithShadow(textRenderer, moduleName, contentX + 6, moduleY + 4, moduleNameColor);

                // Checkbox toggle indicator (same as ToggleWidget)
                boolean isModuleOn = isModuleEnabled(entry);
                int checkboxX = contentX + contentWidth - 16;
                int checkboxY = moduleY + 3;
                int baseCheckboxColor = getCheckboxBackgroundColor();
                int checkboxColor = (fadeAlpha << 24) | (baseCheckboxColor & 0x00FFFFFF);
                context.fill(checkboxX, checkboxY, checkboxX + 12, checkboxY + 12, checkboxColor);
                int checkboxBorderColor = (fadeAlpha << 24) | (getCheckboxBorderColor() & 0x00FFFFFF);
                drawBorder(context, checkboxX, checkboxY, 12, 12, checkboxBorderColor);
                
                // Animated checkmark based on toggle animation progress
                double checkProgress = entry.toggleAnimation.get();
                if (checkProgress > 0.0) {
                    // Scale and fade the checkmark
                    double scale = 0.3 + 0.7 * checkProgress; // Start at 30% scale, grow to 100%
                    int alpha = (int) (checkProgress * 255);
                    int checkColor = (alpha << 24) | (getCheckboxCheckColor() & 0x00FFFFFF);
                    
                    // Calculate scaled checkmark bounds
                    double centerX = checkboxX + 6; // Center of checkbox
                    double centerY = checkboxY + 6;
                    double halfWidth = 3 * scale;
                    double halfHeight = 3 * scale;
                    
                    int checkX1 = (int) (centerX - halfWidth);
                    int checkY1 = (int) (centerY - halfHeight);
                    int checkX2 = (int) (centerX + halfWidth);
                    int checkY2 = (int) (centerY + halfHeight);
                    
                    context.fill(checkX1, checkY1, checkX2, checkY2, checkColor);
                }

                String arrow = entry.expanded ? "▼" : "►";
                int arrowColor = (fadeAlpha << 24) | (0xFF888888 & 0x00FFFFFF);
                context.drawTextWithShadow(textRenderer, arrow, contentX + contentWidth - 26, moduleY + 4, arrowColor);

                // Settings dropdown with animation
                if (animatedSettingsHeight > 0) {
                    int settingsY = moduleY + MODULE_HEIGHT;
                    int settingsBg = (fadeAlpha << 24) | (getDarkBackgroundColor() & 0x00FFFFFF);
                    context.fill(contentX, settingsY, contentX + contentWidth, settingsY + animatedSettingsHeight, settingsBg);
                    
                    // Only render settings if sufficiently expanded
                    if (animatedSettingsHeight > 10) {
                        renderSettings(context, entry, contentX + 4, settingsY + 4, contentWidth - 8, animatedSettingsHeight - 8, mouseX, mouseY, deltaTicks, fadeAlpha);
                    }
                }
            }
            moduleY += entryHeight;
        }

        Scissor.popExclusive();

        // Scrollbar
        int totalHeight = getTotalContentHeight();
        if (totalHeight > contentHeight) {
            int scrollbarHeight = Math.max(15, (contentHeight * contentHeight) / totalHeight);
            int scrollbarY = contentY + (contentScrollOffset * (contentHeight - scrollbarHeight)) / Math.max(1, totalHeight - contentHeight);
            int scrollbarColor = (fadeAlpha << 24) | (0xFF555555 & 0x00FFFFFF);
            context.fill(contentX + contentWidth - 3, scrollbarY, contentX + contentWidth, scrollbarY + scrollbarHeight, scrollbarColor);
        }

        // Border with fade
        if (AnimationConfig.isBorderEnabled()) {
            int borderColor = (fadeAlpha << 24) | (AnimationConfig.getBorderColor() & 0x00FFFFFF);
            drawBorder(context, containerX, containerY, CONTAINER_WIDTH, CONTAINER_HEIGHT, borderColor);
        }
        
        // Restore matrix
        context.getMatrices().popMatrix();
    }

    private boolean isModuleEnabled(ModuleEntry entry) {
        // Check the actual module widget, not the wrapper
        BaseWidget<?> moduleWidget = entry.module.widget;
        
        if (moduleWidget == null || moduleWidget.isStateless()) {
            return false;
        }
        Object state = moduleWidget.getRawState();
        return state instanceof Boolean b && b;
    }

    private void renderSettings(DrawContext context, ModuleEntry entry, int x, int y, int width, int maxHeight, int mouseX, int mouseY, float deltaTicks, int fadeAlpha) {
        if (entry.widget instanceof DropdownWidget dropdown) {
            DropdownWidget<?,?> dw = (DropdownWidget<?,?>) entry.widget;
            // Clear existing bounds to rebuild them during recursive rendering
            List<WidgetBounds> oldBounds = new ArrayList<>(entry.settingWidgets);
            entry.settingWidgets.clear();
            
            int finalY = renderWidgetsRecursively(context, dw.getChildren(), entry, x, y, width, maxHeight, mouseX, mouseY, deltaTicks, 0, fadeAlpha);
        }
    }
    
    /**
     * Recursively renders widgets and their nested children at any depth.
     * @param depth Indentation level for nested widgets
     * @param fadeAlpha Alpha value for fade animation
     * @return Final Y position after rendering all widgets
     */
    private int renderWidgetsRecursively(DrawContext context, Map<String, BaseWidget<?>> widgets, ModuleEntry entry, 
                                          int x, int startY, int width, int maxHeight, int mouseX, int mouseY, 
                                          float deltaTicks, int depth, int fadeAlpha) {
        int settingY = startY;
        int indent = depth * 10; // 10 pixels indentation per level
        int availableWidth = width - indent;
        
        for (Map.Entry<String, BaseWidget<?>> en : widgets.entrySet()) {
            BaseWidget<?> widget = en.getValue();
            
            // Stop rendering if we exceed available height
            if (settingY - startY >= maxHeight) break;
            
            // Create bounds for this widget
            WidgetBounds bounds = new WidgetBounds(widget);
            entry.settingWidgets.add(bounds);
            int renderX = x + indent;
            
            // Determine widget type and render appropriately
            if (widget instanceof ColorWheelWidget colorWheel) {
                ColorWheelWidget colorWheelWidget = (ColorWheelWidget) widget;
                // Render label
                int labelColor = (fadeAlpha << 24) | (0xFFAAAAAA & 0x00FFFFFF);
                context.drawTextWithShadow(textRenderer, widget.getMessage().getString(), renderX, settingY, labelColor);
                settingY += 14;
                
                int wheelSize = Math.min(availableWidth, 160);
                // Set bounds to the actual wheel position
                bounds.x = renderX + (availableWidth - wheelSize) / 2;
                bounds.y = settingY;
                bounds.width = wheelSize;
                bounds.height = wheelSize;
                
                // Initialize color wheel with current module color if available
                if (!widget.isStateless()) {
                    Object state = widget.getRawState();
                    if (state instanceof Integer colorValue) {
                        double[] hsl = Color.rgbToHsl(colorValue);
                        colorWheelWidget.setHSL(hsl[0], hsl[1], hsl[2]);
                        
                        // Set up callback to update widget state when color changes
                        colorWheelWidget.setColorCallback((newColor) -> {
                            @SuppressWarnings("unchecked")
                            BaseWidget<Integer> colorWidget = (BaseWidget<Integer>) widget;
                            colorWidget.setRawState(newColor);
                        });
                    }
                }
                
                // Render color wheel
                if (settingY - startY + wheelSize <= maxHeight) {
                    colorWheelWidget.setX(bounds.x);
                    colorWheelWidget.setY(bounds.y);
                    colorWheelWidget.setDimensions(wheelSize, wheelSize);
                    colorWheelWidget.render(context, mouseX, mouseY, deltaTicks);
                }
                settingY += wheelSize + 10;
                
            } else if (widget instanceof SliderWidget slider) {
                // Render slider label with current value
                String label = widget.getMessage().getString();
                if (!widget.isStateless()) {
                    Object state = widget.getRawState();
                    if (state instanceof Double d) {
                        label += ": " + String.format("%.2f", d);
                    }
                }
                int labelColor = (fadeAlpha << 24) | (0xFFAAAAAA & 0x00FFFFFF);
                context.drawTextWithShadow(textRenderer, label, renderX, settingY, labelColor);
                settingY += 14;
                
                // Set bounds to the actual slider bar position
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth - 20;
                bounds.height = 12;
                
                // Manually render slider bar to avoid text overlap
                if (settingY - startY + 12 <= maxHeight) {
                    SliderWidget sliderWidget = (SliderWidget) widget;
                    sliderWidget.setX(bounds.x);
                    sliderWidget.setY(bounds.y);
                    sliderWidget.setWidth(bounds.width);
                    sliderWidget.setHeight(12);
                    
                    double value = !sliderWidget.isStateless() ? (Double) sliderWidget.getRawState() : sliderWidget.minState;
                    double progress = (value - sliderWidget.minState) / (sliderWidget.maxState - sliderWidget.minState);
                    
                    // Draw slider track
                    int trackColor = (fadeAlpha << 24) | (0xFF333333 & 0x00FFFFFF);
                    context.fill(bounds.x, bounds.y + 4, bounds.x + bounds.width, bounds.y + 8, trackColor);
                    
                    // Draw filled portion
                    int fillWidth = (int) (bounds.width * progress);
                    int fillColor = (fadeAlpha << 24) | (getPrimaryColor() & 0x00FFFFFF);
                    context.fill(bounds.x, bounds.y + 4, bounds.x + fillWidth, bounds.y + 8, fillColor);
                    
                    // Draw slider handle
                    int handleX = bounds.x + fillWidth - 2;
                    int handleColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                    context.fill(handleX, bounds.y + 2, handleX + 4, bounds.y + 10, handleColor);
                }
                settingY += 18; // 12 + spacing
                
            } else if (widget instanceof KeybindWidget keybind) {
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth;
                bounds.height = 20;
                KeybindWidget keybindWidget = (KeybindWidget) widget;
                
                if (settingY - startY + 20 <= maxHeight) {
                    boolean isBinding = CigaretteScreen.bindingKey == keybindWidget;
                    boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                      mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                    
                    // Label
                    int labelColor = (fadeAlpha << 24) | (0xFFAAAAAA & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, keybindWidget.getMessage().getString(), renderX, settingY + 2, labelColor);
                    
                    // Keybind display box
                    int boxX = renderX + availableWidth - 60;
                    int baseBoxColor = isBinding ? getPrimaryColor() : (isHovered ? getHoverColor() : getDarkBackgroundColor());
                    int boxColor = (fadeAlpha << 24) | (baseBoxColor & 0x00FFFFFF);
                    context.fill(boxX, settingY, boxX + 56, settingY + 18, boxColor);
                    int borderColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                    drawBorder(context, boxX, settingY, 56, 18, borderColor);
                    
                    // Key name
                    String keyName = isBinding ? "..." : keybindWidget.toString();
                    int textX = boxX + 28 - textRenderer.getWidth(keyName) / 2;
                    int keyTextColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, keyName, textX, settingY + 5, keyTextColor);
                }
                settingY += 26;
                
            } else if (widget instanceof ToggleKeybindWidget toggleKeybind) {
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth;
                bounds.height = 20;

                ToggleKeybindWidget toggleKeybindWidget = (ToggleKeybindWidget) widget;
                
                if (settingY - startY + 20 <= maxHeight) {
                    boolean isBinding = CigaretteScreen.bindingKey == toggleKeybindWidget.widget;
                    boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                      mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                    
                    // Label with toggle state
                    boolean isOn = !toggleKeybindWidget.isStateless() && toggleKeybindWidget.getRawState() instanceof Boolean b && b;
                    String label = toggleKeybindWidget.getMessage().getString() + (isOn ? " [ON]" : " [OFF]");
                    int baseLabelColor = isOn ? 0xFF00FF00 : 0xFFAAAAAA;
                    int labelColor = (fadeAlpha << 24) | (baseLabelColor & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, label, renderX, settingY + 2, labelColor);
                    
                    // Keybind display box
                    int boxX = renderX + availableWidth - 60;
                    int baseBoxColor = isBinding ? getPrimaryColor() : (isHovered ? getHoverColor() : getDarkBackgroundColor());
                    int boxColor = (fadeAlpha << 24) | (baseBoxColor & 0x00FFFFFF);
                    context.fill(boxX, settingY, boxX + 56, settingY + 18, boxColor);
                    int borderColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                    drawBorder(context, boxX, settingY, 56, 18, borderColor);
                    
                    // Key name
                    String keyName = isBinding ? "..." : toggleKeybindWidget.widget.toString();
                    int textX = boxX + 28 - textRenderer.getWidth(keyName) / 2;
                    int keyTextColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, keyName, textX, settingY + 5, keyTextColor);
                }
                settingY += 26;
                
            } else if (widget instanceof ToggleWidget toggle) {
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth;
                bounds.height = 18;
                
                ToggleWidget toggleWidget = (ToggleWidget) widget;
                
                if (settingY - startY + 18 <= maxHeight) {
                    // Render toggle with checkbox
                    boolean isOn = !toggleWidget.isStateless() && toggleWidget.getRawState() instanceof Boolean b && b;
                    boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                      mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                    
                    // Checkbox
                    int baseCheckboxColor = isHovered ? getHoverColor() : getCheckboxBackgroundColor();
                    int checkboxColor = (fadeAlpha << 24) | (baseCheckboxColor & 0x00FFFFFF);
                    context.fill(renderX, settingY + 2, renderX + 12, settingY + 14, checkboxColor);
                    int borderColor = (fadeAlpha << 24) | (getCheckboxBorderColor() & 0x00FFFFFF);
                    drawBorder(context, renderX, settingY + 2, 12, 12, borderColor);
                    
                    // Animated checkmark based on ToggleWidget's enable animation
                    float maxEnableTicks = 5f; // Same as ToggleWidget.MAX_ENABLE_TICKS
                    // Update enable ticks to mimic ToggleWidget's internal render behavior
                    if (isOn) {
                        toggleWidget.ticksOnEnable = Math.min(toggleWidget.ticksOnEnable + deltaTicks, maxEnableTicks);
                    } else {
                        toggleWidget.ticksOnEnable = Math.max(toggleWidget.ticksOnEnable - deltaTicks, 0f);
                    }
                    float enableProgress = toggleWidget.ticksOnEnable / maxEnableTicks;
                    if (enableProgress > 0.0f) {
                        // Fade in the checkmark without scaling
                        int alpha = (int) (enableProgress * 255);
                        int checkColor = (alpha << 24) | (getCheckboxCheckColor() & 0x00FFFFFF);
                        
                        // Draw a simple checkmark shape using multiple fills
                        // Vertical part of checkmark
                        context.fill(renderX + 3, settingY + 6, renderX + 5, settingY + 10, checkColor);
                        // Horizontal part
                        context.fill(renderX + 5, settingY + 8, renderX + 9, settingY + 10, checkColor);
                    }
                    
                    // Label
                    int labelColor = (fadeAlpha << 24) | (0xFFAAAAAA & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, toggleWidget.getMessage().getString(), renderX + 16, settingY + 4, labelColor);
                }
                settingY += 22;
                
            } else if (widget instanceof ColorSquareWidget colorSquare) {
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth;
                bounds.height = 16;

                ColorSquareWidget colorSquareWidget = (ColorSquareWidget) widget;
                
                if (settingY - startY + 16 <= maxHeight) {
                    // Render color square
                    int color = !colorSquareWidget.isStateless() ? (int) colorSquareWidget.getRawState() : 0xFFFFFFFF;
                    // Apply fade to color preview
                    int fadedColor = (fadeAlpha << 24) | (color & 0x00FFFFFF);
                    context.fill(renderX, settingY + 2, renderX + 12, settingY + 14, fadedColor);
                    int borderColor = (fadeAlpha << 24) | (getPrimaryTextColor() & 0x00FFFFFF);
                    drawBorder(context, renderX, settingY + 2, 12, 12, borderColor);
                    
                    // Show hex value
                    String hexValue = String.format("#%06X", color & 0xFFFFFF);
                    int labelColor = (fadeAlpha << 24) | (0xFFAAAAAA & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, hexValue, renderX + 16, settingY + 4, labelColor);
                }
                settingY += 20;
                
            } else if (widget instanceof DropdownWidget innerDropdown) {
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth;
                bounds.height = 20;
                
                DropdownWidget<?,?> dwInner = (DropdownWidget<?,?>) widget;
                // Position the dropdown widget for click handling
                dwInner.setX(bounds.x);
                dwInner.setY(bounds.y);
                dwInner.setWidth(bounds.width);
                dwInner.setHeight(bounds.height);
                
                // Render the nested dropdown header
                if (settingY - startY + 20 <= maxHeight) {
                    // Render header background
                    boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                      mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                    int baseHeaderColor = isHovered ? getHoverColor() : getBackgroundColor();
                    int headerColor = (fadeAlpha << 24) | (baseHeaderColor & 0x00FFFFFF);
                    context.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, headerColor);
                    
                    // Render header text with dropdown indicator
                    String indicator = dwInner.isExpanded() ? " ▼" : " ▶";
                    int textColor = (fadeAlpha << 24) | (getSecondaryTextColor() & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, dwInner.getMessage().getString() + indicator, bounds.x + 4, bounds.y + 2, textColor);
                }
                settingY += 20;
                
                // Recursively render nested children if expanded
                if (dwInner.isExpanded() && settingY - startY < maxHeight) {
                    settingY = renderWidgetsRecursively(context, dwInner.getChildren(), entry, x, settingY, width, maxHeight, mouseX, mouseY, deltaTicks, depth + 1, fadeAlpha);
                    settingY += 4; // Extra spacing after expanded content
                }
                
            } else {
                // Generic widget rendering
                bounds.x = renderX;
                bounds.y = settingY;
                bounds.width = availableWidth;
                bounds.height = 18;
                
                if (settingY - startY + 18 <= maxHeight) {
                    int labelColor = (fadeAlpha << 24) | (0xFFAAAAAA & 0x00FFFFFF);
                    context.drawTextWithShadow(textRenderer, widget.getMessage().getString(), renderX, settingY + 2, labelColor);
                }
                settingY += 22;
            }
        }
        
        return settingY;
    }

    
    /**
     * Interpolates between two colors.
     */
    private int interpolateColor(int color1, int color2, double progress) {
        progress = Math.max(0, Math.min(1, progress));
        
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        int a = (int) (a1 + (a2 - a1) * progress);
        int r = (int) (r1 + (r2 - r1) * progress);
        int g = (int) (g1 + (g2 - g1) * progress);
        int b = (int) (b1 + (b2 - b1) * progress);
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void drawBorder(DrawContext context, int x, int y, int w, int h, int color) {
        int borderWidth = AnimationConfig.getBorderWidth();
        context.fill(x, y, x + w, y + borderWidth, color); // Top border
        context.fill(x, y + h - borderWidth, x + w, y + h, color); // Bottom border
        context.fill(x, y, x + borderWidth, y + h, color); // Left border
        context.fill(x + w - borderWidth, y, x + w, y + h, color); // Right border
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
