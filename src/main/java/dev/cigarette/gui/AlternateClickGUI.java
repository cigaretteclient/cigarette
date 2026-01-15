package dev.cigarette.gui;

import dev.cigarette.Cigarette;
import dev.cigarette.gui.widget.*;
import dev.cigarette.module.BaseModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Alternate GUI layout - a single draggable contained box with sidebar categories and module list.
 */
public class AlternateClickGUI extends Screen {
    private static final int PRIMARY_COLOR = 0xFFFE5F00;
    private static final int SECONDARY_COLOR = 0xFFC44700;
    private static final int PRIMARY_TEXT_COLOR = 0xFFFFFFFF;
    private static final int BACKGROUND_COLOR = 0xFF1A1A1A;
    private static final int DARK_BACKGROUND_COLOR = 0xFF0D0D0D;
    private static final int HOVER_COLOR = 0xFF2A2A2A;

    private Screen parent = null;

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
        } else if (widget instanceof KeybindWidget keybind) {
            return keybind.mouseClicked(click, doubled);
        } else if (widget instanceof ToggleKeybindWidget toggleKeybind) {
            return toggleKeybind.mouseClicked(click, doubled);
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
            int height = 8; // Top padding
            
            for (var child : dropdown.getChildren().values()) {
                if (child instanceof BaseWidget<?> widget) {
                    WidgetBounds bounds = new WidgetBounds(widget);
                    entry.settingWidgets.add(bounds);
                    
                    // Determine height based on widget type
                    if (widget instanceof ColorWheelWidget) {
                        height += 184; // Label (14) + wheel (160) + spacing (10)
                    } else if (widget instanceof SliderWidget) {
                        height += 32; // Label (14) + slider (12) + spacing (6)
                    } else if (widget instanceof KeybindWidget || widget instanceof ToggleKeybindWidget) {
                        height += 26; // Label + keybind display + spacing
                    } else if (widget instanceof DropdownWidget innerDropdown) {
                        height += 24; // Header height only - children are managed by the dropdown itself
                    } else {
                        height += 22; // Default for toggles, text, etc.
                    }
                }
            }
            
            return Math.max(30, height + 8); // Bottom padding
        }
        return 50;
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
        this.client.setScreen(parent);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        switch (input.getKeycode()) {
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_RIGHT_SHIFT -> this.close();
        }
        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        // Update animations
        categoryAnimation.update(deltaTicks);
        for (ModuleEntry entry : moduleEntries) {
            entry.expansionAnimation.update(deltaTicks);
            entry.toggleAnimation.update(deltaTicks);
            
            // Update expansion animation target
            if (entry.expanded && entry.expansionAnimation.get() < entry.settingsHeight) {
                entry.expansionAnimation.setTarget(entry.settingsHeight);
            } else if (!entry.expanded && entry.expansionAnimation.get() > 0) {
                entry.expansionAnimation.setTarget(0);
            }
            
            // Update toggle animation target
            boolean shouldBeOn = isModuleEnabled(entry);
            double targetToggle = shouldBeOn ? 1.0 : 0.0;
            if (Math.abs(entry.toggleAnimation.get() - targetToggle) > 0.01) {
                entry.toggleAnimation.setTarget(targetToggle);
            }
        }
        
        // Dim background
        context.fill(0, 0, width, height, 0x80000000);

        // Container background
        context.fill(containerX, containerY, containerX + CONTAINER_WIDTH, containerY + CONTAINER_HEIGHT, DARK_BACKGROUND_COLOR);

        // Header gradient
        int[] headerGradient = ColorScheme.getCategoryHeaderGradient();
        GradientRenderer.renderHorizontalGradient(context, containerX, containerY, containerX + CONTAINER_WIDTH, containerY + HEADER_HEIGHT, headerGradient[0], headerGradient[1]);
        context.drawCenteredTextWithShadow(textRenderer, "Cigarette", containerX + CONTAINER_WIDTH / 2, containerY + 6, PRIMARY_TEXT_COLOR);

        // Sidebar
        context.fill(containerX, containerY + HEADER_HEIGHT, containerX + SIDEBAR_WIDTH, containerY + CONTAINER_HEIGHT, BACKGROUND_COLOR);

        // Categories with selection indicator
        int catY = containerY + HEADER_HEIGHT;
        
        // Draw animated selection indicator
        int indicatorY = containerY + HEADER_HEIGHT + categoryAnimation.getInt();
        context.fill(containerX, indicatorY, containerX + 2, indicatorY + CATEGORY_HEIGHT, PRIMARY_COLOR);
        
        for (int i = 0; i < categories.size(); i++) {
            CategoryInstance category = categories.get(i);
            boolean isSelected = i == selectedCategoryIndex;
            boolean isHovered = mouseX >= containerX && mouseX < containerX + SIDEBAR_WIDTH && mouseY >= catY && mouseY < catY + CATEGORY_HEIGHT;

            if (isSelected) {
                // Subtle highlight for selected
                int alpha = 0x30000000;
                context.fill(containerX, catY, containerX + SIDEBAR_WIDTH, catY + CATEGORY_HEIGHT, alpha | (SECONDARY_COLOR & 0x00FFFFFF));
            } else if (isHovered) {
                context.fill(containerX, catY, containerX + SIDEBAR_WIDTH, catY + CATEGORY_HEIGHT, HOVER_COLOR);
            }

            String name = category.widget.getHeaderName();
            int textColor = isSelected ? PRIMARY_COLOR : PRIMARY_TEXT_COLOR;
            context.drawTextWithShadow(textRenderer, name, containerX + 6, catY + 5, textColor);
            catY += CATEGORY_HEIGHT;
        }

        // Content area
        int contentX = containerX + SIDEBAR_WIDTH;
        int contentY = containerY + HEADER_HEIGHT;
        int contentWidth = CONTAINER_WIDTH - SIDEBAR_WIDTH;
        int contentHeight = CONTAINER_HEIGHT - HEADER_HEIGHT;

        context.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, DARK_BACKGROUND_COLOR);

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
                int headerBg = entry.expanded ? 
                    interpolateColor(isHovered ? HOVER_COLOR : BACKGROUND_COLOR, SECONDARY_COLOR, expansionProgress) :
                    (isHovered ? HOVER_COLOR : BACKGROUND_COLOR);
                context.fill(contentX, moduleY, contentX + contentWidth, moduleY + MODULE_HEIGHT, headerBg);

                // Module name
                String moduleName = entry.module.widget.getMessage().getString();
                context.drawTextWithShadow(textRenderer, moduleName, contentX + 6, moduleY + 4, PRIMARY_TEXT_COLOR);

                // Animated toggle indicator
                double toggleProgress = entry.toggleAnimation.get();
                int toggleColor = interpolateColor(0xFF444444, 0xFF00FF00, toggleProgress);
                context.fill(contentX + contentWidth - 14, moduleY + 4, contentX + contentWidth - 4, moduleY + 14, toggleColor);

                // Arrow with rotation hint via different characters
                String arrow = entry.expanded ? "▼" : "►";
                context.drawTextWithShadow(textRenderer, arrow, contentX + contentWidth - 26, moduleY + 4, 0xFF888888);

                // Settings dropdown with animation
                if (animatedSettingsHeight > 0) {
                    int settingsY = moduleY + MODULE_HEIGHT;
                    context.fill(contentX, settingsY, contentX + contentWidth, settingsY + animatedSettingsHeight, 0xFF121212);
                    
                    // Only render settings if sufficiently expanded
                    if (animatedSettingsHeight > 10) {
                        renderSettings(context, entry, contentX + 4, settingsY + 4, contentWidth - 8, animatedSettingsHeight - 8, mouseX, mouseY, deltaTicks);
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
            context.fill(contentX + contentWidth - 3, scrollbarY, contentX + contentWidth, scrollbarY + scrollbarHeight, 0xFF555555);
        }

        // Border
        drawBorder(context, containerX, containerY, CONTAINER_WIDTH, CONTAINER_HEIGHT, PRIMARY_COLOR);
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

    private void renderSettings(DrawContext context, ModuleEntry entry, int x, int y, int width, int maxHeight, int mouseX, int mouseY, float deltaTicks) {
        if (entry.widget instanceof DropdownWidget dropdown) {
            DropdownWidget<?,?> dw = (DropdownWidget<?,?>) entry.widget;
            int settingY = y;
            int settingIndex = 0;
            
            for (Map.Entry<String, BaseWidget<?>> en : dw.getChildren().entrySet()) {
                BaseWidget<?> child = en.getValue();

                if (child instanceof BaseWidget<?> widget && settingIndex < entry.settingWidgets.size()) {
                    WidgetBounds bounds = entry.settingWidgets.get(settingIndex);
                    
                    // Stop rendering if we exceed available height
                    if (settingY - y >= maxHeight) break;
                    
                    // Determine widget height and render appropriately
                    if (widget instanceof ColorWheelWidget colorWheel) {
                        ColorWheelWidget colorWheelWidget = (ColorWheelWidget) en.getValue();
                        // Render label
                        context.drawTextWithShadow(textRenderer, en.getValue().getMessage().getString(), x, settingY, 0xFFAAAAAA);
                        settingY += 14;
                        
                        int wheelSize = Math.min(width, 160);
                        // Set bounds to the actual wheel position
                        bounds.x = x + (width - wheelSize) / 2;
                        bounds.y = settingY;
                        bounds.width = wheelSize;
                        bounds.height = wheelSize;
                        
                        // Render color wheel
                        if (settingY - y + wheelSize <= maxHeight) {
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
                        context.drawTextWithShadow(textRenderer, label, x, settingY, 0xFFAAAAAA);
                        settingY += 14;
                        
                        // Set bounds to the actual slider bar position
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 12;
                        
                        // Manually render slider bar to avoid text overlap
                        if (settingY - y + 12 <= maxHeight) {
                            SliderWidget sliderWidget = (SliderWidget) en.getValue();
                            sliderWidget.setX(bounds.x);
                            sliderWidget.setY(bounds.y);
                            sliderWidget.setWidth(bounds.width - 20);
                            sliderWidget.setHeight(12);
                            
                            double value = !sliderWidget.isStateless() ? (Double) sliderWidget.getRawState() : sliderWidget.minState;
                            double progress = (value - sliderWidget.minState) / (sliderWidget.maxState - sliderWidget.minState);
                            
                            // Draw slider track
                            context.fill(bounds.x, bounds.y + 4, bounds.x + bounds.width - 20, bounds.y + 8, 0xFF333333);
                            
                            // Draw filled portion
                            int fillWidth = (int) ((bounds.width - 20) * progress);
                            context.fill(bounds.x, bounds.y + 4, bounds.x + fillWidth, bounds.y + 8, PRIMARY_COLOR);
                            
                            // Draw slider handle
                            int handleX = bounds.x + fillWidth - 2;
                            context.fill(handleX, bounds.y + 2, handleX + 4, bounds.y + 10, PRIMARY_TEXT_COLOR);
                        }
                        settingY += 18; // 12 + spacing
                        
                    } else if (widget instanceof KeybindWidget keybind) {
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 20;
                        KeybindWidget keybindWidget = (KeybindWidget) en.getValue();
                        
                        if (settingY - y + 20 <= maxHeight) {
                            boolean isBinding = CigaretteScreen.bindingKey == keybindWidget;
                            boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                              mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                            
                            // Label
                            context.drawTextWithShadow(textRenderer, keybindWidget.getMessage().getString(), x, settingY + 2, 0xFFAAAAAA);
                            
                            // Keybind display box
                            int boxX = x + width - 60;
                            int boxColor = isBinding ? PRIMARY_COLOR : (isHovered ? HOVER_COLOR : DARK_BACKGROUND_COLOR);
                            context.fill(boxX, settingY, boxX + 56, settingY + 18, boxColor);
                            drawBorder(context, boxX, settingY, 56, 18, PRIMARY_TEXT_COLOR);
                            
                            // Key name
                            String keyName = isBinding ? "..." : keybindWidget.toString();
                            int textX = boxX + 28 - textRenderer.getWidth(keyName) / 2;
                            context.drawTextWithShadow(textRenderer, keyName, textX, settingY + 5, PRIMARY_TEXT_COLOR);
                        }
                        settingY += 26;
                        
                    } else if (widget instanceof ToggleKeybindWidget toggleKeybind) {
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 20;

                        ToggleKeybindWidget toggleKeybindWidget = (ToggleKeybindWidget) en.getValue();
                        
                        if (settingY - y + 20 <= maxHeight) {
                            boolean isBinding = CigaretteScreen.bindingKey == toggleKeybindWidget.widget;
                            boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                              mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                            
                            // Label with toggle state
                            boolean isOn = !toggleKeybindWidget.isStateless() && toggleKeybindWidget.getRawState() instanceof Boolean b && b;
                            String label = toggleKeybindWidget.getMessage().getString() + (isOn ? " [ON]" : " [OFF]");
                            context.drawTextWithShadow(textRenderer, label, x, settingY + 2, isOn ? 0xFF00FF00 : 0xFFAAAAAA);
                            
                            // Keybind display box
                            int boxX = x + width - 60;
                            int boxColor = isBinding ? PRIMARY_COLOR : (isHovered ? HOVER_COLOR : DARK_BACKGROUND_COLOR);
                            context.fill(boxX, settingY, boxX + 56, settingY + 18, boxColor);
                            drawBorder(context, boxX, settingY, 56, 18, PRIMARY_TEXT_COLOR);
                            
                            // Key name
                            String keyName = isBinding ? "..." : toggleKeybindWidget.widget.toString();
                            int textX = boxX + 28 - textRenderer.getWidth(keyName) / 2;
                            context.drawTextWithShadow(textRenderer, keyName, textX, settingY + 5, PRIMARY_TEXT_COLOR);
                        }
                        settingY += 26;
                        
                    } else if (widget instanceof ToggleWidget toggle) {
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 18;
                        
                        ToggleWidget toggleWidget = (ToggleWidget) en.getValue();
                        
                        if (settingY - y + 18 <= maxHeight) {
                            // Render toggle with checkbox
                            boolean isOn = !toggleWidget.isStateless() && toggleWidget.getRawState() instanceof Boolean b && b;
                            boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                              mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                            
                            // Checkbox
                            int checkboxColor = isHovered ? HOVER_COLOR : DARK_BACKGROUND_COLOR;
                            context.fill(x, settingY + 2, x + 12, settingY + 14, checkboxColor);
                            drawBorder(context, x, settingY + 2, 12, 12, PRIMARY_TEXT_COLOR);
                            
                            if (isOn) {
                                // Checkmark
                                context.fill(x + 3, settingY + 5, x + 9, settingY + 11, 0xFF00FF00);
                            }
                            
                            // Label
                            context.drawTextWithShadow(textRenderer, toggleWidget.getMessage().getString(), x + 16, settingY + 4, 0xFFAAAAAA);
                        }
                        settingY += 22;
                        
                    } else if (widget instanceof ColorSquareWidget colorSquare) {
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 16;

                        ColorSquareWidget colorSquareWidget = (ColorSquareWidget) en.getValue();
                        
                        if (settingY - y + 16 <= maxHeight) {
                            // Render color square
                            int color = !colorSquareWidget.isStateless() ? (int) colorSquareWidget.getRawState() : 0xFFFFFFFF;
                            context.fill(x, settingY + 2, x + 12, settingY + 14, color);
                            drawBorder(context, x, settingY + 2, 12, 12, PRIMARY_TEXT_COLOR);
                            
                            // Show hex value
                            String hexValue = String.format("#%06X", color & 0xFFFFFF);
                            context.drawTextWithShadow(textRenderer, hexValue, x + 16, settingY + 4, 0xFFAAAAAA);
                        }
                        settingY += 20;
                        
                    } else if (widget instanceof DropdownWidget innerDropdown) {
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 20;
                        
                        DropdownWidget<?,?> dwInner = (DropdownWidget<?,?>) en.getValue();
                        // Position the dropdown widget for click handling
                        dwInner.setX(bounds.x);
                        dwInner.setY(bounds.y);
                        dwInner.setWidth(bounds.width);
                        dwInner.setHeight(bounds.height);
                        
                        // Render the nested dropdown header
                        if (settingY - y + 20 <= maxHeight) {
                            // Render header background
                            boolean isHovered = mouseX >= bounds.x && mouseX < bounds.x + bounds.width &&
                                              mouseY >= bounds.y && mouseY < bounds.y + bounds.height;
                            int headerColor = isHovered ? 0xFF2A2A2A : 0xFF1A1A1A;
                            context.fill(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, headerColor);
                            
                            // Render header text with dropdown indicator
                            context.drawTextWithShadow(textRenderer, dwInner.getMessage().getString() + " ▼", bounds.x + 4, bounds.y + 2, 0xFFAAAAAA);
                        }
                        settingY += 20;
                        
                        // Check if dropdown is expanded and render children
                        if (dwInner.isExpanded() && settingY - y < maxHeight) {
                            for (Map.Entry<String, BaseWidget<?>> childEn : dwInner.getChildren().entrySet()) {
                                BaseWidget<?> childWidget = childEn.getValue();
                                
                                if (childWidget instanceof ColorWheelWidget colorWheelChild) {
                                    // Render color wheel inside the dropdown
                                    int childWheelSize = Math.min(width, 160);
                                    int wheelX = x + (width - childWheelSize) / 2;
                                    int wheelY = settingY;
                                    
                                    if (wheelY - y + childWheelSize <= maxHeight) {
                                        // Create bounds for the color wheel so it can be clicked
                                        WidgetBounds wheelBounds = new WidgetBounds(colorWheelChild);
                                        wheelBounds.x = wheelX;
                                        wheelBounds.y = wheelY;
                                        wheelBounds.width = childWheelSize;
                                        wheelBounds.height = childWheelSize;
                                        entry.settingWidgets.add(wheelBounds);
                                        
                                        colorWheelChild.setX(wheelX);
                                        colorWheelChild.setY(wheelY);
                                        colorWheelChild.setDimensions(childWheelSize, childWheelSize);
                                        colorWheelChild.render(context, mouseX, mouseY, deltaTicks);
                                        settingY += childWheelSize + 10;
                                    }
                                } else if (childWidget instanceof SliderWidget childSlider) {
                                    // Render slider inside the dropdown
                                    context.drawTextWithShadow(textRenderer, childWidget.getMessage().getString(), x, settingY, 0xFFAAAAAA);
                                    settingY += 14;
                                    
                                    if (settingY - y + 12 <= maxHeight) {
                                        // Create bounds for the slider so it can be clicked
                                        WidgetBounds sliderBounds = new WidgetBounds(childSlider);
                                        sliderBounds.x = x;
                                        sliderBounds.y = settingY;
                                        sliderBounds.width = width - 20;
                                        sliderBounds.height = 12;
                                        entry.settingWidgets.add(sliderBounds);
                                        
                                        childSlider.setX(x);
                                        childSlider.setY(settingY);
                                        childSlider.setWidth(width - 20);
                                        childSlider.setHeight(12);
                                        
                                        double sliderValue = !childSlider.isStateless() ? (Double) childSlider.getRawState() : childSlider.minState;
                                        double sliderProgress = (sliderValue - childSlider.minState) / (childSlider.maxState - childSlider.minState);
                                        
                                        context.fill(x, settingY + 4, x + width - 20, settingY + 8, 0xFF333333);
                                        int sliderFillWidth = (int) ((width - 20) * sliderProgress);
                                        context.fill(x, settingY + 4, x + sliderFillWidth, settingY + 8, PRIMARY_COLOR);
                                        int sliderHandleX = x + sliderFillWidth - 2;
                                        context.fill(sliderHandleX, settingY + 2, sliderHandleX + 4, settingY + 10, PRIMARY_TEXT_COLOR);
                                        settingY += 18;
                                    }
                                }
                            }
                            settingY += 4; // Extra spacing after expanded content
                        } else {
                            settingY += 4;
                        }
                        
                    } else {
                        // Generic widget rendering
                        bounds.x = x;
                        bounds.y = settingY;
                        bounds.width = width;
                        bounds.height = 18;
                        
                        if (settingY - y + 18 <= maxHeight) {
                            context.drawTextWithShadow(textRenderer, widget.getMessage().getString(), x, settingY + 2, 0xFFAAAAAA);
                        }
                        settingY += 22;
                    }
                    
                    settingIndex++;
                }
            }
        }
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
        context.fill(x, y, x + w, y + 1, color); // Top border
        context.fill(x, y + h - 1, x + w, y + h, color); // Bottom border
        context.fill(x, y, x + 1, y + h, color); // Left border
        context.fill(x + w - 1, y, x + w, y + h, color); // Right border
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
