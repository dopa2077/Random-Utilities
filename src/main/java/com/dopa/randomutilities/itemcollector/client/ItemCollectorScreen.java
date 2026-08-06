package com.dopa.randomutilities.itemcollector.client;

import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.client.gui.PanelHost;
import com.dopa.randomutilities.itemcollector.menu.ItemCollectorMenu;
import com.dopa.randomutilities.itemcollector.network.ItemCollectorSettingPayload;
import com.dopa.randomutilities.itemcollector.client.panel.ItemCollectorConfigPanel;
import com.dopa.randomutilities.itemcollector.client.panel.ItemCollectorCosmeticPanel;
import com.dopa.randomutilities.itemcollector.client.panel.ItemCollectorInformativePanel;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.client.panel.MachineRedstonePanel;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public class ItemCollectorScreen extends AbstractContainerScreen<ItemCollectorMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier BLACKLIST_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/blacklist_icon.png");
    private static final Identifier WHITELIST_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/whitelist_icon.png");
    private static final Identifier RANGE_OVERLAY_ICON =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/gather.png");

    private static final int TEXTURE_SIZE = 256;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int IMAGE_HEIGHT = 114 + 18;
    private static final int FOOTER_Y = 35;
    private static final int ICON_SIZE = 16;
    private static final int ICON_Y = 18;
    private static final int OVERLAY_BUTTON_SIZE = 13;
    private static final int OVERLAY_ICON_SIZE = 11;

    private final PanelHost panelHost = new PanelHost();
    @Nullable
    private MachineRedstonePanel redstonePanel;
    @Nullable
    private ItemCollectorConfigPanel configPanel;
    @Nullable
    private ItemCollectorCosmeticPanel cosmeticPanel;
    @Nullable
    private Button modeButton;
    @Nullable
    private IconButton rangeOverlayButton;

    public ItemCollectorScreen(ItemCollectorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    public ItemCollectorMenu getMenu() {
        return menu;
    }

    public PanelHost getPanelHost() {
        return panelHost;
    }

    public int leftPos() {
        return leftPos;
    }

    public int topPos() {
        return topPos;
    }

    public int imageWidth() {
        return imageWidth;
    }

    public boolean isRangeOverlayEnabled() {
        var be = menu.blockEntity();
        var level = be.getLevel();
        if (level == null) {
            return false;
        }
        return ItemCollectorClientOverlay.isEnabled(level.dimension(), be.getBlockPos());
    }

    @Override
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addOverlayWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return menu::redstoneMode;
    }

    @Override
    protected void init() {
        super.init();
        panelHost.clear();
        configPanel = null;
        cosmeticPanel = null;
        redstonePanel = null;

        panelHost.add(new ItemCollectorInformativePanel(menu.collectorType()));

        configPanel = new ItemCollectorConfigPanel(this);
        panelHost.add(configPanel);
        configPanel.initWidgets();

        cosmeticPanel = new ItemCollectorCosmeticPanel(this);
        panelHost.add(cosmeticPanel);
        cosmeticPanel.initWidgets();

        redstonePanel = new MachineRedstonePanel(
                this,
                PanelAnchor.RIGHT_TOP,
                0,
                mode -> ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                        ItemCollectorSettingPayload.KIND_REDSTONE,
                        mode.ordinal()
                ))
        );
        panelHost.add(redstonePanel);
        redstonePanel.initWidgets();

        panelHost.layoutWidgets(leftPos, topPos, imageWidth);

        rangeOverlayButton = new IconButton(
                leftPos + imageWidth - OVERLAY_BUTTON_SIZE - 4,
                topPos + 4,
                OVERLAY_BUTTON_SIZE,
                OVERLAY_ICON_SIZE,
                RANGE_OVERLAY_ICON,
                rangeOverlayTooltip(),
                this::toggleRangeOverlay
        );
        addRenderableWidget(rangeOverlayButton);

        if (menu.collectorType().supportsWhitelist()) {
            int iconX = ItemCollectorMenu.iconX(menu.collectorType());
            modeButton = Button.builder(Component.empty(), b -> toggleFilterMode())
                    .bounds(leftPos + iconX, topPos + ICON_Y, ICON_SIZE, ICON_SIZE)
                    .tooltip(Tooltip.create(Component.translatable(
                            menu.isWhitelistMode()
                                    ? "gui.dopasrandomutilities.item_collector.whitelist"
                                    : "gui.dopasrandomutilities.item_collector.blacklist")))
                    .build();
            addRenderableWidget(modeButton);
        }
    }

    public boolean isShiftHeldPublic() {
        if (this.minecraft == null) {
            return false;
        }
        if (this.minecraft.hasShiftDown()) {
            return true;
        }
        long window = this.minecraft.getWindow().handle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private Component rangeOverlayTooltip() {
        boolean enabled = isRangeOverlayEnabled();
        return Component.translatable("gui.dopasrandomutilities.item_collector.range_overlay")
                .append("\n")
                .append(Component.translatable(enabled
                                ? "gui.dopasrandomutilities.item_collector.range_overlay.enabled"
                                : "gui.dopasrandomutilities.item_collector.range_overlay.disabled")
                        .withStyle(enabled ? ChatFormatting.AQUA : ChatFormatting.GRAY));
    }

    private void toggleRangeOverlay() {
        var be = menu.blockEntity();
        var level = be.getLevel();
        if (level == null) {
            return;
        }
        ItemCollectorClientOverlay.toggle(level.dimension(), be.getBlockPos());
        if (rangeOverlayButton != null) {
            rangeOverlayButton.updateTooltip(rangeOverlayTooltip());
        }
    }

    private void toggleFilterMode() {
        boolean next = !menu.isWhitelistMode();
        ClientPacketDistributor.sendToServer(new ItemCollectorSettingPayload(
                ItemCollectorSettingPayload.KIND_FILTER_MODE,
                next ? 1 : 0
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        panelHost.tick();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
        if (modeButton != null) {
            modeButton.setTooltip(Tooltip.create(Component.translatable(
                    menu.isWhitelistMode()
                            ? "gui.dopasrandomutilities.item_collector.whitelist"
                            : "gui.dopasrandomutilities.item_collector.blacklist")));
        }
    }

    @Override
    public void onClose() {
        ItemCollectorJeiDragState.endDrag();
        if (configPanel != null) {
            configPanel.onScreenClose();
        }
        super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panelHost.render(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);

        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                imageWidth, FOOTER_Y, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(xo + 7, yo + 17, xo + imageWidth - 7, yo + FOOTER_Y, BODY_COLOR);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + FOOTER_Y,
                0.0F, 126.0F, imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        for (int i = 0; i < menu.collectorType().filterSlotCount(); i++) {
            Slot slot = menu.slots.get(i);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + slot.x, yo + slot.y, 16, 16);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        renderGhostSlotTints(graphics);
        renderFilterModeIcon(graphics, mouseX, mouseY);
        ItemCollectorJeiDragState.renderLine(graphics, mouseX, mouseY);
        Component tabTooltip = panelHost.hoveredTabTooltip(mouseX, mouseY, leftPos, topPos, imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(font, tabTooltip, mouseX, mouseY);
        }
    }

    private void renderGhostSlotTints(GuiGraphicsExtractor graphics) {
        int count = menu.collectorType().filterSlotCount();
        for (int i = 0; i < count; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) {
                continue;
            }
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, 0x40FFFFFF);
        }
    }

    private void renderFilterModeIcon(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int iconX = ItemCollectorMenu.iconX(menu.collectorType());
        Identifier icon = menu.collectorType().supportsWhitelist() && menu.isWhitelistMode()
                ? WHITELIST_ICON
                : BLACKLIST_ICON;
        graphics.blit(RenderPipelines.GUI_TEXTURED, icon, leftPos + iconX, topPos + ICON_Y,
                0.0F, 0.0F, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        if (!menu.collectorType().supportsWhitelist()
                && isHovering(iconX, ICON_Y, ICON_SIZE, ICON_SIZE, mouseX, mouseY)) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.dopasrandomutilities.item_collector.blacklist"),
                    mouseX,
                    mouseY
            );
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overTab = false;
        for (var panel : panelHost.panels()) {
            if (panel.isMouseOverTab(event.x(), event.y(), leftPos, topPos, imageWidth)) {
                overTab = true;
                break;
            }
        }
        var occupying = panelHost.openPanel();
        boolean overBody = occupying != null
                && occupying.isMouseOverBody(event.x(), event.y(), leftPos, topPos, imageWidth);
        // Config steppers sit near the inventory attachment; treat their hitboxes as panel clicks
        // even when the cursor straddles leftPos by a few pixels.
        boolean overConfigControl = configPanel != null
                && configPanel.contentsInteractive()
                && configPanel.isMouseOverInteractiveWidget(event.x(), event.y());
        if (overTab) {
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        if (overBody || overConfigControl) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    setFocused(child);
                    // Buttons/steppers: no drag. EditBoxes and ChannelSliders need setDragging for drag.
                    if (child instanceof Button) {
                        clearFocus();
                    } else {
                        setDragging(true);
                    }
                    if (configPanel != null) {
                        configPanel.clearFocusIfOutside(event.x(), event.y());
                    }
                    return true;
                }
            }
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child instanceof AbstractWidget widget
                        && widget.visible
                        && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            if (overBody) {
                return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
            }
            return false;
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            if (!(getFocused() instanceof EditBox)) {
                clearFocus();
            }
            if (configPanel != null) {
                configPanel.clearFocusIfOutside(event.x(), event.y());
            }
            return true;
        }
        if (panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth)) {
            return true;
        }
        if (configPanel != null) {
            configPanel.clearFocusIfOutside(event.x(), event.y());
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        GuiEventListener focused = getFocused();
        boolean handled = focused != null && focused.mouseReleased(event);
        setDragging(false);
        if (handled) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelHost.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, font)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean isOverWidget(AbstractWidget widget, double mouseX, double mouseY) {
        return mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }

    private static final class IconButton extends AbstractWidget {
        private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
                Identifier.withDefaultNamespace("widget/button"),
                Identifier.withDefaultNamespace("widget/button_disabled"),
                Identifier.withDefaultNamespace("widget/button_highlighted")
        );

        private final Identifier texture;
        private final int iconSize;
        private final Runnable onPress;

        IconButton(int x, int y, int size, int iconSize, Identifier texture, Component tooltip, Runnable onPress) {
            super(x, y, size, size, Component.empty());
            this.texture = texture;
            this.iconSize = iconSize;
            this.onPress = onPress;
            setTooltip(Tooltip.create(tooltip));
        }

        void updateTooltip(Component tooltip) {
            setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (active) {
                onPress.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_SPRITES.get(active, isHoveredOrFocused()),
                    x,
                    y,
                    width,
                    height,
                    ARGB.white(alpha)
            );
            int iconX = x + (width - iconSize) / 2;
            int iconY = y + (height - iconSize) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F,
                    iconSize, iconSize, iconSize, iconSize);
        }
    }
}
