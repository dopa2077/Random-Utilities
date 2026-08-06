package com.dopa.randomutilities.machine.generator.client;

import com.dopa.randomutilities.machine.generator.config.GeneratorRecipePresence;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.client.gui.PanelHost;
import com.dopa.randomutilities.machine.item.MachineUpgradeItem;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.client.panel.MachineRedstonePanel;
import com.dopa.randomutilities.machine.client.panel.MachineUpgradePanel;
import com.dopa.randomutilities.machine.generator.client.panel.GeneratorConfigPanel;
import com.dopa.randomutilities.machine.generator.client.panel.GeneratorInformativePanel;
import com.dopa.randomutilities.machine.generator.menu.ResourceGeneratorMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ResourceGeneratorScreen extends AbstractContainerScreen<ResourceGeneratorMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier FURNACE_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int PLAYER_INV_HEIGHT = 96;
    /** Player-inv texture starts above slots (label strip); slots stay at Y=84. */
    private static final int MACHINE_FOOTER_Y = 70;

    private static final int SLOT = 18;
    private static final int BELOW_GAP = 3;
    private static final int SIDE_GAP = 3;
    private static final int SIDE_H_GAP = 3;
    private static final int LARGE_SLOT = 32;

    /** Generator icon cell (anchor for staggered side slots). */
    private static final int GEN_X = 44;
    private static final int GEN_Y = 31;

    // Side columns: mid-gap on Gen midline, with intentional spacing (not glued).
    private static final int SIDE_TOP_Y = GEN_Y - SLOT / 2 - SIDE_GAP / 2;
    private static final int SIDE_BOT_Y = GEN_Y + SLOT / 2 + SIDE_GAP / 2;
    private static final int LEFT_X = GEN_X - SLOT - SIDE_H_GAP;
    private static final int RIGHT_X = GEN_X + SLOT + SIDE_H_GAP;

    private static final int BELOW_X = GEN_X;
    private static final int BELOW_Y = GEN_Y + SLOT + BELOW_GAP;

    private static final int OUTPUT_X = 134;
    private static final int OUTPUT_Y = GEN_Y;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int ARROW_X = RIGHT_X + SLOT + 9;
    private static final int ARROW_Y = GEN_Y + (SLOT - ARROW_H) / 2;
    private static final float ARROW_EMPTY_U = 79.0F;
    private static final float ARROW_EMPTY_V = 34.0F;
    private static final float ARROW_FILL_U = 176.0F;
    private static final float ARROW_FILL_V = 14.0F;

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

        this.panelHost.add(new GeneratorInformativePanel(tabYBias));

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

        // Plain inventory chrome (no furnace slot cutouts fighting our layout).
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, MACHINE_FOOTER_Y, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(xo + 7, yo + 17, xo + this.imageWidth - 7, yo + MACHINE_FOOTER_Y, BODY_COLOR);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + MACHINE_FOOTER_Y,
                0.0F, 126.0F, this.imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Staggered sides: [side0]  [side1] / Gen / [side2]  [side3], Below under Gen with gap.
        drawGhostSlot(graphics, xo, yo, LEFT_X, SIDE_TOP_Y, 0);
        drawGhostSlot(graphics, xo, yo, RIGHT_X, SIDE_TOP_Y, 1);
        drawGhostSlot(graphics, xo, yo, GEN_X, GEN_Y, -1);
        drawGhostSlot(graphics, xo, yo, LEFT_X, SIDE_BOT_Y, 2);
        drawGhostSlot(graphics, xo, yo, RIGHT_X, SIDE_BOT_Y, 3);
        drawGhostSlot(graphics, xo, yo, BELOW_X, BELOW_Y, GeneratorRecipePresence.BELOW_SLOT);

        // Large output like basic /dev/null (centered on 16×16 item origin).
        int outFrameX = xo + OUTPUT_X + 8 - LARGE_SLOT / 2;
        int outFrameY = yo + OUTPUT_Y + 8 - LARGE_SLOT / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, outFrameX, outFrameY, LARGE_SLOT, LARGE_SLOT);

        // Empty furnace arrow, then filled progress strip.
        graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                xo + ARROW_X, yo + ARROW_Y, ARROW_EMPTY_U, ARROW_EMPTY_V,
                ARROW_W, ARROW_H, TEXTURE_SIZE, TEXTURE_SIZE);
        float progress = this.menu.progressFraction();
        if (progress > 0.0F) {
            int filled = Math.max(1, Math.round(ARROW_W * progress));
            graphics.blit(RenderPipelines.GUI_TEXTURED, FURNACE_TEXTURE,
                    xo + ARROW_X, yo + ARROW_Y, ARROW_FILL_U, ARROW_FILL_V,
                    filled, ARROW_H, TEXTURE_SIZE, TEXTURE_SIZE);
        }
    }

    private void drawGhostSlot(GuiGraphicsExtractor graphics, int xo, int yo, int slotX, int slotY, int ghostIndex) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + slotX - 1, yo + slotY - 1, 18, 18);
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
        } else if (this.hoveredSlot != null
                && this.menu.isUpgradeSlotIndex(this.hoveredSlot.index)
                && this.hoveredSlot.hasItem()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : upgradeSlotTooltip(this.hoveredSlot.getItem())) {
                lines.add(line.getVisualOrderText());
            }
            graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        } else {
            Component itemTooltip = hoveredDisplayTooltip(mouseX, mouseY, side0, side1, side2, side3, gen, below, output);
            if (itemTooltip != null) {
                graphics.setTooltipForNextFrame(this.font, itemTooltip, mouseX, mouseY);
            }
        }
    }

    private List<Component> upgradeSlotTooltip(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        int used = this.menu.blockEntity().upgrades().countOf(stack.getItem());
        int max = UpgradeConfig.maxPerType(this.menu.generatorType());
        if (stack.getItem() instanceof MachineUpgradeItem upgrade) {
            int perUpgrade = upgrade.kind().percent();
            MutableComponent boost = Component.translatable(upgrade.kind().tooltipKey())
                    .withStyle(ChatFormatting.GRAY);
            boost.append(Component.literal(perUpgrade + "%").withStyle(ChatFormatting.GREEN));
            lines.add(boost);
            lines.add(Component.empty());
            MutableComponent total = Component.translatable("gui.dopasrandomutilities.upgrade.total_boost")
                    .withStyle(ChatFormatting.GRAY);
            total.append(Component.literal((used * perUpgrade) + "%").withStyle(ChatFormatting.GREEN));
            lines.add(total);
        }
        ChatFormatting color = used >= max && max > 0 ? ChatFormatting.RED : ChatFormatting.GREEN;
        lines.add(Component.translatable(
                "gui.dopasrandomutilities.upgrade.available",
                Integer.toString(used),
                Integer.toString(max)
        ).withStyle(color));
        return lines;
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
