package com.dopa.randomutilities.machine.client.panel;

import com.dopa.randomutilities.util.PanelLayout;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.filter.menu.UpgradeSlot;
import com.dopa.randomutilities.machine.menu.MachineUpgradeSlot;
import com.dopa.randomutilities.registry.ModItems;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class MachineUpgradePanel extends AttachedPanel {
    private static final int BG = 0xFF28752E;
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final ItemStack UPGRADE_ICON = new ItemStack(ModItems.UPGRADE_CASING.get());

    private final List<MachineUpgradeSlot> upgradeSlots;
    private final int tabYBias;

    public MachineUpgradePanel(List<MachineUpgradeSlot> upgradeSlots, PanelAnchor anchor, int tabYBias) {
        super(
                anchor,
                UpgradeSlot.panelWidth(),
                PanelLayout.CONTENT_PAD * 2 + UpgradeSlot.TITLE_GAP + UpgradeSlot.ROWS * 18,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.upgrades")
        );
        this.upgradeSlots = upgradeSlots;
        this.tabYBias = tabYBias;
    }

    @Override
    public int tabOffsetY() {
        return super.tabOffsetY() + tabYBias;
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(UPGRADE_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        int gridX = bodyX + (panelWidth - UpgradeSlot.gridWidth()) / 2;
        int gridY = bodyY + CONTENT_PAD + UpgradeSlot.TITLE_GAP;
        int count = Math.min(UpgradeConfig.UPGRADE_SLOT_COUNT, upgradeSlots.size());
        for (int i = 0; i < count; i++) {
            int col = i % UpgradeSlot.COLS;
            int row = i / UpgradeSlot.COLS;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE,
                    gridX + col * 18 - 1, gridY + row * 18 - 1, 18, 18);
        }
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        for (MachineUpgradeSlot slot : upgradeSlots) {
            slot.setActive(interactive);
        }
    }

    @Override
    protected void onClosed() {
        for (MachineUpgradeSlot slot : upgradeSlots) {
            slot.setActive(false);
        }
    }
}
