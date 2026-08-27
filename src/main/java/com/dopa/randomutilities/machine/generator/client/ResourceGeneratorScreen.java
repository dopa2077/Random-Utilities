package com.dopa.randomutilities.machine.generator.client;

import com.dopa.randomutilities.machine.generator.config.GeneratorRecipePresence;
import com.dopa.randomutilities.core.gui.panel.PanelAnchor;
import com.dopa.randomutilities.core.gui.panel.PanelHost;
import com.dopa.randomutilities.core.machine.RedstoneMode;
import com.dopa.randomutilities.core.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.core.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.core.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.machine.generator.client.panel.GeneratorConfigPanel;
import com.dopa.randomutilities.core.gui.panel.ScrollingInfoPanel;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
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
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/resource_generator.png");
    private static final Identifier BURN_PROGRESS_SPRITE =
            Identifier.withDefaultNamespace("container/furnace/burn_progress");

    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;

    private static final int SLOT = 18;
    private static final int BELOW_GAP = 3;
    private static final int SIDE_GAP = 4;
    private static final int SIDE_H_GAP = 3;

    /** Generator icon cell (anchor for staggered side slots). */
    private static final int GEN_X = 44;
    private static final int GEN_Y = 35;

    // Side columns: mid-gap on Gen midline, with intentional spacing (not glued).
    private static final int SIDE_TOP_Y = GEN_Y - SLOT / 2 - SIDE_GAP / 2;
    private static final int SIDE_BOT_Y = GEN_Y + SLOT / 2 + SIDE_GAP / 2;
    private static final int LEFT_X = GEN_X - SLOT - SIDE_H_GAP;
    private static final int RIGHT_X = GEN_X + SLOT + SIDE_H_GAP;

    private static final int BELOW_X = GEN_X;
    private static final int BELOW_Y = GEN_Y + SLOT + BELOW_GAP;

    private static final int OUTPUT_X = 133;
    private static final int OUTPUT_Y = GEN_Y;
    /** Progress arrow; also the JEI recipe click area. */
    public static final int ARROW_W = 24;
    public static final int ARROW_H = 16;
    public static final int ARROW_X = 94;
    public static final int ARROW_Y = 35;

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = ResourceGeneratorMenu.TAB_Y_BIAS;

    @Nullable
    private GeneratorConfigPanel configPanel;
    @Nullable
    private MachineRedstonePanel redstonePanel;

    public ResourceGeneratorScreen(ResourceGeneratorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    public ResourceGeneratorMenu getMenu() {
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

        this.panelHost.add(new ScrollingInfoPanel(tabYBias, infoParagraphKeys(this.menu.generatorType().mode())));

        if (this.menu.supportsLockOutput()) {
            this.configPanel = new GeneratorConfigPanel(this, tabYBias);
            this.panelHost.add(this.configPanel);
            this.configPanel.initWidgets();
        }

        if (this.menu.upgradesEnabled()) {
            this.panelHost.add(new MachineUpgradePanel(
                    this.menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, tabYBias));
            this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_BELOW, tabYBias);
        } else {
            this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_TOP, tabYBias);
        }
        this.panelHost.add(this.redstonePanel);
        this.redstonePanel.initWidgets();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.panelHost.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth,
                mouseX, mouseY, partialTick);

        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                xo,
                yo,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );

        pulseMissing(graphics, xo, yo, LEFT_X, SIDE_TOP_Y, 0);
        pulseMissing(graphics, xo, yo, RIGHT_X, SIDE_TOP_Y, 1);
        pulseMissing(graphics, xo, yo, LEFT_X, SIDE_BOT_Y, 2);
        pulseMissing(graphics, xo, yo, RIGHT_X, SIDE_BOT_Y, 3);
        pulseMissing(graphics, xo, yo, BELOW_X, BELOW_Y, GeneratorRecipePresence.BELOW_SLOT);

        float progress = this.menu.progressFraction();
        int filled = Mth.ceil(progress * ARROW_W);
        if (filled > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BURN_PROGRESS_SPRITE,
                    ARROW_W,
                    ARROW_H,
                    0,
                    0,
                    xo + ARROW_X,
                    yo + ARROW_Y,
                    filled,
                    ARROW_H
            );
        }
    }

    private void pulseMissing(GuiGraphicsExtractor graphics, int xo, int yo, int slotX, int slotY, int ghostIndex) {
        if (ghostIndex >= 0 && this.menu.isGhostMissing(ghostIndex)) {
            float pulse = 0.35F + 0.35F * (0.5F + 0.5F * Mth.sin(System.currentTimeMillis() / 250.0F));
            int alpha = Mth.clamp((int) (pulse * 255.0F), 40, 180);
            graphics.fill(xo + slotX - 1, yo + slotY - 1, xo + slotX + 17, yo + slotY + 17, (alpha << 24) | 0xCC3333);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        ItemStack side0 = this.menu.ghostSideStack(0);
        ItemStack side1 = this.menu.ghostSideStack(1);
        ItemStack side2 = this.menu.ghostSideStack(2);
        ItemStack side3 = this.menu.ghostSideStack(3);
        ItemStack gen = this.menu.generatorIcon();
        ItemStack below = this.menu.ghostBelowStack();
        ItemStack output = this.menu.outputIcon();

        drawItem(graphics, side0, LEFT_X, SIDE_TOP_Y);
        drawItem(graphics, side1, RIGHT_X, SIDE_TOP_Y);
        drawItem(graphics, gen, GEN_X, GEN_Y);
        drawItem(graphics, side2, LEFT_X, SIDE_BOT_Y);
        drawItem(graphics, side3, RIGHT_X, SIDE_BOT_Y);
        drawItem(graphics, below, BELOW_X, BELOW_Y);
        drawItem(graphics, output, OUTPUT_X, OUTPUT_Y);

        Component tabTooltip = this.panelHost.hoveredTabTooltip(
                mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(this.font, tabTooltip, mouseX, mouseY);
        } else if (!UpgradeSlotTooltips.applyHover(
                graphics,
                this.font,
                mouseX,
                mouseY,
                this.hoveredSlot,
                this.hoveredSlot != null && this.menu.isUpgradeSlotIndex(this.hoveredSlot.index),
                this.menu.blockEntity().upgrades()
        )) {
            Component itemTooltip = hoveredDisplayTooltip(mouseX, mouseY, side0, side1, side2, side3, gen, below, output);
            if (itemTooltip != null) {
                graphics.setTooltipForNextFrame(this.font, itemTooltip, mouseX, mouseY);
            }
        }
    }

    @Nullable
    private Component hoveredDisplayTooltip(
            double mouseX, double mouseY,
            ItemStack side0, ItemStack side1, ItemStack side2, ItemStack side3,
            ItemStack gen, ItemStack below, ItemStack output) {
        Component tip = tooltipIfHover(mouseX, mouseY, LEFT_X, SIDE_TOP_Y, side0);
        if (tip != null) {
            return tip;
        }
        tip = tooltipIfHover(mouseX, mouseY, RIGHT_X, SIDE_TOP_Y, side1);
        if (tip != null) {
            return tip;
        }
        tip = tooltipIfHover(mouseX, mouseY, GEN_X, GEN_Y, gen);
        if (tip != null) {
            return tip;
        }
        tip = tooltipIfHover(mouseX, mouseY, LEFT_X, SIDE_BOT_Y, side2);
        if (tip != null) {
            return tip;
        }
        tip = tooltipIfHover(mouseX, mouseY, RIGHT_X, SIDE_BOT_Y, side3);
        if (tip != null) {
            return tip;
        }
        tip = tooltipIfHover(mouseX, mouseY, BELOW_X, BELOW_Y, below);
        if (tip != null) {
            return tip;
        }
        return tooltipIfHover(mouseX, mouseY, OUTPUT_X, OUTPUT_Y, output);
    }

    @Nullable
    private Component tooltipIfHover(double mouseX, double mouseY, int slotX, int slotY, ItemStack stack) {
        if (stack.isEmpty() || !this.isHovering(slotX, slotY, 16, 16, mouseX, mouseY)) {
            return null;
        }
        return stack.getHoverName();
    }

    private void drawItem(GuiGraphicsExtractor graphics, ItemStack stack, int slotX, int slotY) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.item(stack, this.leftPos + slotX, this.topPos + slotY, this.leftPos ^ this.topPos ^ slotX);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY, LABEL_COLOR, false);
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
        // Open body covers sibling tabs at the attachment edge — handle body/scrollbar first.
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
            if (this.panelHost.mouseClicked(event.x(), event.y())) {
                return true;
            }
            return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        }
        if (overTab) {
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
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean panelHandled = this.panelHost.mouseReleased();
        boolean handled = super.mouseReleased(event);
        return panelHandled || handled;
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

    private static String[] infoParagraphKeys(GeneratorType.Mode mode) {
        return switch (mode) {
            case RANDOM_ORE -> new String[] {
                    "gui.dopasrandomutilities.panel.info.generator.visual",
                    "gui.dopasrandomutilities.panel.info.generator.random_ore.howto"
            };
            case METAL_BLOCK -> new String[] {
                    "gui.dopasrandomutilities.panel.info.generator.visual",
                    "gui.dopasrandomutilities.panel.info.generator.metal_block.howto"
            };
            default -> new String[] {
                    "gui.dopasrandomutilities.panel.info.generator.visual",
                    "gui.dopasrandomutilities.panel.info.generator.howto",
                    "gui.dopasrandomutilities.panel.info.generator.lock"
            };
        };
    }
}
