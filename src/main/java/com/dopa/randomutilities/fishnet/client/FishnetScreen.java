package com.dopa.randomutilities.fishnet.client;

import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.client.gui.PanelHost;
import com.dopa.randomutilities.fishnet.FishnetUpgradeInventory;
import com.dopa.randomutilities.fishnet.client.panel.FishnetCosmeticPanel;
import com.dopa.randomutilities.fishnet.client.panel.FishnetInformativePanel;
import com.dopa.randomutilities.fishnet.menu.FishnetMenu;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.client.panel.MachineRedstonePanel;
import com.dopa.randomutilities.machine.client.panel.MachineUpgradePanel;
import com.dopa.randomutilities.machine.item.MachineUpgradeItem;
import com.dopa.randomutilities.registry.ModItems;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FishnetScreen extends AbstractContainerScreen<FishnetMenu> implements MachineRedstonePanel.Host {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final Identifier FURNACE_TEXTURE =
            Identifier.withDefaultNamespace("textures/gui/container/furnace.png");
    private static final Identifier BURN_PROGRESS_SPRITE =
            Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int MACHINE_FOOTER_Y = 70;

    private static final int ARROW_W = 24;
    private static final int ARROW_H = 16;
    private static final float ARROW_EMPTY_U = 79.0F;
    private static final float ARROW_EMPTY_V = 34.0F;

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = FishnetMenu.TAB_Y_BIAS;

    @Nullable
    private MachineRedstonePanel redstonePanel;
    @Nullable
    private FishnetCosmeticPanel cosmeticPanel;

    public FishnetScreen(FishnetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
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

    @Override
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return () -> menu.redstoneMode();
    }

    @Override
    protected void init() {
        super.init();
        panelHost.clear();
        redstonePanel = null;
        cosmeticPanel = null;

        panelHost.add(new FishnetInformativePanel(tabYBias));
        cosmeticPanel = new FishnetCosmeticPanel(this, tabYBias);
        panelHost.add(cosmeticPanel);
        cosmeticPanel.initWidgets();
        panelHost.add(new MachineUpgradePanel(menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, tabYBias));
        redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_BELOW, tabYBias);
        panelHost.add(redstonePanel);
        redstonePanel.initWidgets();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        panelHost.tick();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panelHost.tick();
        panelHost.render(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);

        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                imageWidth, MACHINE_FOOTER_Y, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(xo + 7, yo + 17, xo + imageWidth - 7, yo + MACHINE_FOOTER_Y, BODY_COLOR);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + MACHINE_FOOTER_Y,
                0.0F, 126.0F, imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                SLOT_SPRITE,
                xo + FishnetMenu.ROD_X - 1,
                yo + FishnetMenu.ROD_Y - 1,
                18,
                18
        );
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = xo + FishnetMenu.GRID_LEFT + col * 18 - 1;
                int slotY = yo + FishnetMenu.GRID_TOP + row * 18 - 1;
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, slotX, slotY, 18, 18);
            }
        }

        // Empty arrow from furnace GUI atlas; white fill uses the 26.2 burn_progress sprite
        // (the old furnace.png UV strip no longer contains the animated arrow).
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FURNACE_TEXTURE,
                xo + FishnetMenu.ARROW_X,
                yo + FishnetMenu.ARROW_Y,
                ARROW_EMPTY_U,
                ARROW_EMPTY_V,
                ARROW_W,
                ARROW_H,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        float progress = menu.progressFraction();
        int filled = Mth.ceil(progress * ARROW_W);
        if (filled > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BURN_PROGRESS_SPRITE,
                    ARROW_W,
                    ARROW_H,
                    0,
                    0,
                    xo + FishnetMenu.ARROW_X,
                    yo + FishnetMenu.ARROW_Y,
                    filled,
                    ARROW_H
            );
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        Component tabTooltip = panelHost.hoveredTabTooltip(mouseX, mouseY, leftPos, topPos, imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(font, tabTooltip, mouseX, mouseY);
            return;
        }
        if (hoveredSlot != null
                && menu.isRodSlotIndex(hoveredSlot.index)
                && !hoveredSlot.hasItem()) {
            graphics.setTooltipForNextFrame(
                    font,
                    Component.translatable("gui.dopasrandomutilities.fishnet.need_rod"),
                    mouseX,
                    mouseY
            );
            return;
        }
        if (hoveredSlot != null
                && menu.isUpgradeSlotIndex(hoveredSlot.index)
                && hoveredSlot.hasItem()) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : upgradeSlotTooltip(hoveredSlot.getItem())) {
                lines.add(line.getVisualOrderText());
            }
            graphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }

    private List<Component> upgradeSlotTooltip(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName());
        Item item = stack.getItem();
        int used = menu.blockEntity().upgrades().countOf(item);
        int max = maxForUpgrade(item);
        if (item instanceof MachineUpgradeItem upgrade) {
            if (upgrade.kind().showsPercent()) {
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
            } else {
                lines.add(Component.translatable(upgrade.kind().tooltipKey()).withStyle(ChatFormatting.GRAY));
            }
        }
        ChatFormatting color = used >= max ? ChatFormatting.RED : ChatFormatting.GREEN;
        lines.add(Component.translatable(
                "gui.dopasrandomutilities.upgrade.available",
                Integer.toString(used),
                Integer.toString(max)
        ).withStyle(color));
        return lines;
    }

    private static int maxForUpgrade(Item item) {
        if (item == ModItems.PRODUCTIVITY_UPGRADE.get()) {
            return FishnetUpgradeInventory.MAX_PRODUCTIVITY;
        }
        if (item == ModItems.OVERCLOCK_UPGRADE.get()) {
            return FishnetUpgradeInventory.MAX_OVERCLOCK;
        }
        if (item == ModItems.FORTUNE_MESH_UPGRADE.get()) {
            return FishnetUpgradeInventory.maxFortuneMesh();
        }
        if (item == ModItems.TREASURE_MESH_UPGRADE.get()) {
            return FishnetUpgradeInventory.maxTreasureMesh();
        }
        return 0;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, LABEL_COLOR, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overTab = false;
        for (var p : panelHost.panels()) {
            if (p.isMouseOverTab(event.x(), event.y(), leftPos, topPos, imageWidth)) {
                overTab = true;
                break;
            }
        }
        var occupying = panelHost.openPanel();
        boolean overBody = occupying != null
                && occupying.isMouseOverBody(event.x(), event.y(), leftPos, topPos, imageWidth);
        if (overBody) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    setFocused(child);
                    clearFocus();
                    return true;
                }
            }
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child instanceof AbstractWidget widget && widget.visible && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            Slot slotUnder = findActiveSlotAt(event.x(), event.y());
            if (slotUnder != null && menu.isUpgradeSlotIndex(slotUnder.index)) {
                return super.mouseClicked(event, doubleClick);
            }
            if (panelHost.mouseClicked(event.x(), event.y())) {
                return true;
            }
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        if (overTab) {
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            clearFocus();
            return true;
        }
        return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean panelHandled = panelHost.mouseReleased();
        boolean handled = super.mouseReleased(event);
        return panelHandled || handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelHost.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, font)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Slot findActiveSlotAt(double mouseX, double mouseY) {
        for (Slot slot : menu.slots) {
            if (slot.isActive() && isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
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
