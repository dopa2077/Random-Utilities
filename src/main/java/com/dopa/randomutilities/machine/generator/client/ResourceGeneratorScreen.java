package com.dopa.randomutilities.machine.generator.client;

import com.dopa.randomutilities.config.GeneratorRecipePresence;
import com.dopa.randomutilities.filteritem.client.panel.PanelAnchor;
import com.dopa.randomutilities.filteritem.client.panel.PanelHost;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.client.panel.MachineRedstonePanel;
import com.dopa.randomutilities.machine.client.panel.MachineUpgradePanel;
import com.dopa.randomutilities.machine.generator.client.panel.GeneratorConfigPanel;
import com.dopa.randomutilities.machine.generator.client.panel.GeneratorInformativePanel;
import com.dopa.randomutilities.machine.generator.menu.ResourceGeneratorMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class ResourceGeneratorScreen extends AbstractContainerScreen<ResourceGeneratorMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier FURNACE_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int BODY_COLOR = 0xFFC6C6C6;

    private static final int GEN_X = 56;
    private static final int GEN_Y = 26;
    private static final int SLOT = 18;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = ResourceGeneratorMenu.TAB_Y_BIAS;

    @Nullable
    private GeneratorConfigPanel configPanel;
    @Nullable
    private MachineRedstonePanel redstonePanel;

    public ResourceGeneratorScreen(ResourceGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    public ResourceGeneratorMenu getMenu() {
        return this.menu;
    }

    @Override
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return () -> this.menu.redstoneMode();
    }

    @Override
    protected void init() {
        super.init();
        this.panelHost.clear();
        this.configPanel = null;
        this.redstonePanel = null;

        this.panelHost.add(new GeneratorInformativePanel(tabYBias));

        if (this.menu.supportsLockOutput()) {
            this.configPanel = new GeneratorConfigPanel(this, tabYBias);
            this.panelHost.add(this.configPanel);
            this.configPanel.initWidgets();
        }

        if (this.menu.upgradesEnabled()) {
            this.panelHost.add(new MachineUpgradePanel(
                    this.menu.getUpgradeSlots(), PanelAnchor.RIGHT_BELOW, tabYBias));
            this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_LOW, tabYBias);
        } else {
            this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_BELOW, tabYBias);
        }
        this.panelHost.add(this.redstonePanel);
        this.redstonePanel.initWidgets();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.tick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.panelHost.tick();
        this.panelHost.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth,
                mouseX, mouseY, partialTick);

        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);
        // Cover unused fuel slot.
        graphics.fill(xo + 55, yo + 51, xo + 73, yo + 71, BODY_COLOR);

        drawGhostSlot(graphics, xo, yo, GEN_X, GEN_Y - SLOT, 0); // N
        drawGhostSlot(graphics, xo, yo, GEN_X + SLOT, GEN_Y, 1); // E
        drawGhostSlot(graphics, xo, yo, GEN_X, GEN_Y + SLOT, 2); // S
        drawGhostSlot(graphics, xo, yo, GEN_X - SLOT, GEN_Y, 3); // W
        drawGhostSlot(graphics, xo, yo, GEN_X, GEN_Y + SLOT * 2, GeneratorRecipePresence.BELOW_SLOT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + GEN_X - 1, yo + GEN_Y - 1, 18, 18);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + OUTPUT_X - 1, yo + OUTPUT_Y - 1, 18, 18);

        float progress = this.menu.progressFraction();
        if (progress > 0.0F) {
            int filled = Math.max(1, Math.round(ARROW_W * progress));
            graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_BACKGROUND,
                    xo + ARROW_X, yo + ARROW_Y, 176.0F, 14.0F, filled, ARROW_H, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    private void drawGhostSlot(GuiGraphicsExtractor graphics, int xo, int yo, int slotX, int slotY, int ghostIndex) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + slotX - 1, yo + slotY - 1, 18, 18);
        if (this.menu.isGhostMissing(ghostIndex)) {
            float pulse = 0.35F + 0.35F * (0.5F + 0.5F * Mth.sin(System.currentTimeMillis() / 250.0F));
            int alpha = Mth.clamp((int) (pulse * 255.0F), 40, 180);
            graphics.fill(xo + slotX - 1, yo + slotY - 1, xo + slotX + 17, yo + slotY + 17, (alpha << 24) | 0xCC3333);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        drawItem(graphics, this.menu.ghostSideStack(0), GEN_X, GEN_Y - SLOT);
        drawItem(graphics, this.menu.ghostSideStack(1), GEN_X + SLOT, GEN_Y);
        drawItem(graphics, this.menu.ghostSideStack(2), GEN_X, GEN_Y + SLOT);
        drawItem(graphics, this.menu.ghostSideStack(3), GEN_X - SLOT, GEN_Y);
        drawItem(graphics, this.menu.ghostBelowStack(), GEN_X, GEN_Y + SLOT * 2);
        drawItem(graphics, this.menu.generatorIcon(), GEN_X, GEN_Y);
        drawItem(graphics, this.menu.outputIcon(), OUTPUT_X, OUTPUT_Y);

        Component tabTooltip = this.panelHost.hoveredTabTooltip(
                mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(this.font, tabTooltip, mouseX, mouseY);
        }
    }

    private void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, int slotX, int slotY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.item(stack, this.leftPos + slotX, this.topPos + slotY, this.leftPos ^ this.topPos ^ slotX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
            for (int i = this.children().size() - 1; i >= 0; i--) {
                GuiEventListener child = this.children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    this.setFocused(child);
                    this.clearFocus();
                    return true;
                }
            }
            for (int i = this.children().size() - 1; i >= 0; i--) {
                GuiEventListener child = this.children().get(i);
                if (child instanceof AbstractWidget widget && widget.visible && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            Slot slotUnder = findActiveSlotAt(event.x(), event.y());
            if (slotUnder != null && this.menu.isUpgradeSlotIndex(slotUnder.index)) {
                return super.mouseClicked(event, doubleClick);
            }
            return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            this.clearFocus();
            return true;
        }
        return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.panelHost.mouseScrolled(mouseX, mouseY, scrollY, this.leftPos, this.topPos, this.imageWidth, this.font)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Slot findActiveSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isOverWidget(AbstractWidget widget, double mouseX, double mouseY) {
        return mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }
}
