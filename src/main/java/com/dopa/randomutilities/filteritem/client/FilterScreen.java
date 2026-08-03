package com.dopa.randomutilities.filteritem.client;

import com.dopa.randomutilities.filteritem.FilterContents;
import com.dopa.randomutilities.filteritem.client.panel.ConfiguratorPanel;
import com.dopa.randomutilities.filteritem.client.panel.CosmeticPanel;
import com.dopa.randomutilities.filteritem.client.panel.EnergyPanel;
import com.dopa.randomutilities.filteritem.client.panel.InformativePanel;
import com.dopa.randomutilities.filteritem.client.panel.PanelAnchor;
import com.dopa.randomutilities.filteritem.client.panel.PanelHost;
import com.dopa.randomutilities.filteritem.client.panel.RedstonePanel;
import com.dopa.randomutilities.filteritem.client.panel.UpgradePanel;
import com.dopa.randomutilities.filteritem.menu.FilterMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier GATHER_TEXTURE =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/gather.png");

    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int TEXTURE_SIZE = 256;
    private static final int INTERIOR_U = 7;
    private static final int INTERIOR_W = 162;
    private static final int DIVIDER_V = 126;
    private static final int DIVIDER_STRIP_H = 7;

    private static final int LABEL_COLOR = 0xFF404040;
    private static final int BODY_COLOR = 0xFFC6C6C6;

    private static final int BASIC_FOOTER_Y = 35;
    private static final int BASIC_IMAGE_HEIGHT = 114 + 18;
    private static final int LARGE_SLOT = 26;

    private static final int GATHER_BUTTON_SIZE = 13;
    private static final int GATHER_ICON_SIZE = 11;
    private static final int HIGHLIGHT_BORDER = 2;
    private static final float OVER_CAP_PULSE_SPEED = 0.25F;

    private static double pendingMouseX = Double.NaN;
    private static double pendingMouseY = Double.NaN;

    private final int containerRows;
    private final PanelHost panelHost = new PanelHost();

    @Nullable
    private ConfiguratorPanel configuratorPanel;
    @Nullable
    private CosmeticPanel cosmeticPanel;
    @Nullable
    private RedstonePanel redstonePanel;
    private IconButton gatherButton;
    private boolean gatherConfirmPending;

    public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
        super(
                menu,
                inventory,
                title,
                176,
                menu.isBasic() ? BASIC_IMAGE_HEIGHT : 114 + menu.getRows() * 18
        );
        this.containerRows = menu.isBasic() ? 1 : menu.getRows();
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    public FilterMenu getMenu() {
        return this.menu;
    }

    public PanelHost getPanelHost() {
        return this.panelHost;
    }

    public int leftPos() {
        return this.leftPos;
    }

    public int topPos() {
        return this.topPos;
    }

    public int imageWidth() {
        return this.imageWidth;
    }

    public <T extends net.minecraft.client.gui.components.events.GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    public void removeOverlayWidget(net.minecraft.client.gui.components.events.GuiEventListener widget) {
        this.removeWidget(widget);
    }

    public void sendMenuButton(int buttonId) {
        preserveMousePosition(this.minecraft);
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
    }

    public void sendSlotButton(int normalButtonId, int rowButtonId) {
        sendMenuButton(isShiftHeldPublic() ? rowButtonId : normalButtonId);
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

    public void clearGatherConfirm() {
        this.gatherConfirmPending = false;
    }

    public Font getFont() {
        return this.font;
    }

    public static void preserveMousePosition(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        long window = minecraft.getWindow().handle();
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window, x, y);
        pendingMouseX = x[0];
        pendingMouseY = y[0];
    }

    private void restoreMousePositionIfPending() {
        if (this.minecraft == null || Double.isNaN(pendingMouseX)) {
            return;
        }
        GLFW.glfwSetCursorPos(this.minecraft.getWindow().handle(), pendingMouseX, pendingMouseY);
        this.minecraft.mouseHandler.setIgnoreFirstMove();
        pendingMouseX = Double.NaN;
        pendingMouseY = Double.NaN;
    }

    private Component gatherTooltip() {
        MutableComponent tooltip = Component.translatable("gui.dopasrandomutilities.gather.tooltip");
        if (this.menu.wouldGatherChange()) {
            tooltip.append("\n\n")
                    .append(Component.translatable("gui.dopasrandomutilities.gather.tooltip_warning")
                            .withStyle(ChatFormatting.RED));
            if (this.gatherConfirmPending) {
                tooltip.append("\n\n")
                        .append(Component.translatable("gui.dopasrandomutilities.remove_slot.tooltip_void_confirm")
                                .withStyle(ChatFormatting.DARK_RED));
            }
        }
        return tooltip;
    }

    private void onGatherPressed() {
        if (this.menu.wouldGatherChange()) {
            if (this.gatherConfirmPending) {
                this.gatherConfirmPending = false;
                sendMenuButton(FilterMenu.BTN_GATHER);
            } else {
                this.gatherConfirmPending = true;
                if (this.configuratorPanel != null) {
                    this.configuratorPanel.clearRemoveConfirm();
                }
                if (this.gatherButton != null) {
                    this.gatherButton.updateTooltip(gatherTooltip());
                }
            }
            return;
        }
        this.gatherConfirmPending = false;
        sendMenuButton(FilterMenu.BTN_GATHER);
    }

    @Override
    protected void init() {
        super.init();
        this.panelHost.clear();
        this.configuratorPanel = null;
        this.cosmeticPanel = null;
        this.redstonePanel = null;

        var profile = this.menu.profile();
        this.panelHost.add(new InformativePanel(this.menu.isBasic()));

        if (profile != null && profile.showConfigurator()) {
            this.configuratorPanel = new ConfiguratorPanel(this);
            this.panelHost.add(this.configuratorPanel);
            this.configuratorPanel.initWidgets();
        }

        if (profile != null && profile.showCosmetic()) {
            PanelAnchor cosmeticAnchor = profile.showConfigurator() ? PanelAnchor.LEFT_LOW : PanelAnchor.LEFT_BELOW;
            this.cosmeticPanel = new CosmeticPanel(this, cosmeticAnchor, profile.showCosmeticHighlight());
            this.panelHost.add(this.cosmeticPanel);
            this.cosmeticPanel.initWidgets();
        }

        if (profile != null && profile.showEnergy()) {
            this.panelHost.add(new EnergyPanel());
        }
        if (profile != null && profile.showUpgrades()) {
            this.panelHost.add(new UpgradePanel(this.menu.getUpgradeSlots()));
        }
        if (profile != null && profile.showRedstone()) {
            this.redstonePanel = new RedstonePanel(this);
            this.panelHost.add(this.redstonePanel);
            this.redstonePanel.initWidgets();
        }

        if (profile != null && profile.showGatherButton()) {
            this.gatherButton = new IconButton(
                    this.leftPos + this.imageWidth - GATHER_BUTTON_SIZE - 4,
                    this.topPos + 4,
                    GATHER_BUTTON_SIZE,
                    GATHER_ICON_SIZE,
                    GATHER_TEXTURE,
                    gatherTooltip(),
                    this::onGatherPressed
            );
            this.addRenderableWidget(this.gatherButton);
            this.gatherConfirmPending = false;
        } else {
            this.gatherButton = null;
            this.gatherConfirmPending = false;
        }

        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
        if (this.menu.shouldRestoreConfigPanel()) {
            this.panelHost.snapOpen(PanelAnchor.LEFT_BELOW);
            this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
        }
        restoreMousePositionIfPending();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.tick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);

        if (this.gatherButton != null) {
            if (this.gatherConfirmPending && !this.menu.wouldGatherChange()) {
                this.gatherConfirmPending = false;
            }
            this.gatherButton.updateTooltip(gatherTooltip());
        }
    }

    @Override
    public void onClose() {
        if (this.configuratorPanel != null) {
            this.configuratorPanel.onScreenClose();
        }
        super.onClose();
    }

    private void coverUnusedSlotsOnLastRow(GuiGraphicsExtractor graphics, int xo, int yo, int slotCount) {
        int usedOnLastRow = slotCount % 9;
        if (usedOnLastRow == 0) {
            return;
        }
        int row = slotCount / 9;
        int coverY = yo + 18 + row * 18;
        for (int col = usedOnLastRow; col < 9; col++) {
            VanillaContainerPanel.blitRegion(graphics, xo + 8 + col * 18, coverY, 18, 18,
                    INTERIOR_U, DIVIDER_V, INTERIOR_W, DIVIDER_STRIP_H);
        }
    }

    private void renderBasicBackground(GuiGraphicsExtractor graphics) {
        int xo = this.leftPos;
        int yo = this.topPos;

        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, BASIC_FOOTER_Y, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(xo + 7, yo + 17, xo + this.imageWidth - 7, yo + BASIC_FOOTER_Y, BODY_COLOR);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + BASIC_FOOTER_Y,
                0.0F, 126.0F, this.imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        int frameX = xo + FilterMenu.BASIC_SLOT_X - (LARGE_SLOT - 18) / 2;
        int frameY = yo + FilterMenu.BASIC_SLOT_Y - (LARGE_SLOT - 18) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, frameX, frameY, LARGE_SLOT, LARGE_SLOT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        // Panels first so the inventory frame draws over them (glued / underneath look).
        this.panelHost.tick();
        this.panelHost.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth,
                mouseX, mouseY, partialTick);
        if (this.menu.isBasic()) {
            renderBasicBackground(graphics);
        } else {
            int xo = this.leftPos;
            int yo = this.topPos;
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                    this.imageWidth, this.containerRows * 18 + 17, TEXTURE_SIZE, TEXTURE_SIZE);
            graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + this.containerRows * 18 + 17,
                    0.0F, 126.0F, this.imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
            coverUnusedSlotsOnLastRow(graphics, xo, yo, this.menu.getPageSlotCount());
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.menu.isBasic()) {
            renderSelectedSlotHighlight(graphics);
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        if (!this.menu.isBasic()) {
            renderOverCapWarnings(graphics, partialTick);
        }
        Component tabTooltip = this.panelHost.hoveredTabTooltip(
                mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(this.font, tabTooltip, mouseX, mouseY);
        }
    }

    private void renderOverCapWarnings(GuiGraphicsExtractor graphics, float partialTick) {
        int maxStack = this.configuratorPanel != null
                ? this.configuratorPanel.effectiveMaxStackForDisplay()
                : this.menu.getMaxStackSizeSetting();
        float pulse = (float) (0.35F + 0.25F * (0.5F + 0.5F * Math.sin(partialTick * OVER_CAP_PULSE_SPEED)));
        int alpha = (int) (pulse * 255.0F) << 24;
        int tint = alpha | 0xFF0000;

        for (int i = 0; i < this.menu.getPageSlotCount(); i++) {
            Slot slot = this.menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (stack.isEmpty() || stack.getCount() <= maxStack) {
                continue;
            }
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x, y, x + 16, y + 16, tint);
        }
    }

    private void renderSelectedSlotHighlight(GuiGraphicsExtractor graphics) {
        int selected = this.menu.getSelectedSlot();
        int local = selected - this.menu.getPage() * FilterContents.SLOTS_PER_PAGE;
        if (local < 0 || local >= this.menu.getPageSlotCount()) {
            return;
        }
        Slot slot = this.menu.slots.get(local);
        if (this.menu.isFilterSlotOverCap(local)) {
            return;
        }
        int color = 0xFF000000 | (this.menu.isHighlightMatchColor()
                ? this.menu.getColor()
                : FilterContents.DEFAULT_HIGHLIGHT_COLOR);
        int x = this.leftPos + slot.x;
        int y = this.topPos + slot.y;
        int t = HIGHLIGHT_BORDER;
        graphics.fill(x - t, y - t, x + 16 + t, y, color);
        graphics.fill(x - t, y + 16, x + 16 + t, y + 16 + t, color);
        graphics.fill(x - t, y, x, y + 16, color);
        graphics.fill(x + 16, y, x + 16 + t, y + 16, color);
    }

    @Override
    protected void renderSlotContents(GuiGraphicsExtractor graphics, ItemStack itemStack, Slot slot, @Nullable String itemCount) {
        if (!itemStack.isEmpty() && isFilterSlot(slot) && itemStack.getCount() > 1) {
            itemCount = CompactCountFormat.format(itemStack.getCount());
        }
        super.renderSlotContents(graphics, itemStack, slot, itemCount);
    }

    private boolean isFilterSlot(Slot slot) {
        if (this.menu.isBasic()) {
            return slot.index == 0;
        }
        return slot.index < this.menu.getPageSlotCount();
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack itemStack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(itemStack));
        if (!this.menu.isBasic() && this.hoveredSlot != null && isFilterSlot(this.hoveredSlot)
                && this.menu.isFilterSlotOverCap(this.hoveredSlot.index)) {
            tooltip.add(Component.empty());
            tooltip.add(Component.translatable("gui.dopasrandomutilities.over_cap.tooltip")
                    .withStyle(ChatFormatting.RED));
        }
        return tooltip;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.getFocused() instanceof EditBox editBox) {
            if (event.isConfirmation()) {
                this.clearFocus();
                return true;
            }
            if (editBox.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // Tabs / panel bodies sit outside the inventory rect; AbstractContainerScreen treats those
        // clicks as slotId -999 (drop) and returns true, so handle them without that path.
        boolean overTab = false;
        for (var p : this.panelHost.panels()) {
            if (p.isMouseOverTab(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth)) {
                overTab = true;
                break;
            }
        }
        var occupying = this.panelHost.openPanel();
        boolean overBody = occupying != null
                && occupying.isMouseOverBody(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        if (overTab) {
            return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        }
        if (overBody) {
            // Widgets first (Screen children), then empty-body close — skip container outside-click.
            for (int i = this.children().size() - 1; i >= 0; i--) {
                GuiEventListener child = this.children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    this.setFocused(child);
                    // Clear sticky button highlight only — clearing all focus kills slider drag/release.
                    if (child instanceof Button) {
                        this.clearFocus();
                    } else {
                        // Screen.mouseDragged only forwards when isDragging(); vanilla mouseClicked sets this.
                        this.setDragging(true);
                    }
                    if (this.configuratorPanel != null) {
                        this.configuratorPanel.clearFocusIfOutside(event.x(), event.y());
                    }
                    return true;
                }
            }
            // Inactive widgets still occupy space; AbstractWidget.isMouseOver often ignores them.
            for (int i = this.children().size() - 1; i >= 0; i--) {
                GuiEventListener child = this.children().get(i);
                if (child instanceof AbstractWidget widget
                        && widget.visible
                        && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            // Upgrade slots live in the panel body — let the container handle them; do not close.
            Slot slotUnder = findActiveSlotAt(event.x(), event.y());
            if (slotUnder != null && this.menu.isUpgradeSlotIndex(slotUnder.index)) {
                boolean handled = super.mouseClicked(event, doubleClick);
                if (handled && !(this.getFocused() instanceof EditBox)) {
                    this.clearFocus();
                }
                return true;
            }
            return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            // Drop button highlight focus; keep EditBox focus for typing.
            if (!(this.getFocused() instanceof EditBox)) {
                this.clearFocus();
            }
            if (this.configuratorPanel != null) {
                this.configuratorPanel.clearFocusIfOutside(event.x(), event.y());
            }
            return true;
        }
        if (this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth)) {
            return true;
        }
        if (this.configuratorPanel != null) {
            this.configuratorPanel.clearFocusIfOutside(event.x(), event.y());
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        // Outside-GUI release can skip focused overlay widgets; deliver release first so sliders commit.
        GuiEventListener focused = this.getFocused();
        boolean handled = focused != null && focused.mouseReleased(event);
        this.setDragging(false);
        if (handled) {
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.panelHost.mouseScrolled(mouseX, mouseY, scrollY, this.leftPos, this.topPos, this.imageWidth, this.font)) {
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

    @Nullable
    private Slot findActiveSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) {
                continue;
            }
            if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
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
            this.setTooltip(Tooltip.create(tooltip));
        }

        void updateTooltip(Component tooltip) {
            this.setTooltip(Tooltip.create(tooltip));
        }

        @Override
        public void onClick(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
            if (this.active) {
                this.onPress.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.getMessage());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_SPRITES.get(this.active, this.isHoveredOrFocused()),
                    x,
                    y,
                    this.width,
                    this.height,
                    ARGB.white(this.alpha)
            );
            int iconX = x + (this.width - iconSize) / 2;
            int iconY = y + (this.height - iconSize) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F,
                    iconSize, iconSize, iconSize, iconSize);
        }
    }
}
