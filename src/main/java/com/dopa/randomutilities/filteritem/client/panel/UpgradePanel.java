package com.dopa.randomutilities.filteritem.client.panel;

import com.dopa.randomutilities.filteritem.menu.UpgradeSlot;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class UpgradePanel extends AttachedPanel {
    private static final int BG = 0xFF28752E;
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
    private static final ItemStack UPGRADE_ICON = new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);

    private final List<UpgradeSlot> upgradeSlots;

    public UpgradePanel(List<UpgradeSlot> upgradeSlots) {
        super(
                PanelAnchor.RIGHT_BELOW,
                UpgradeSlot.panelWidth(),
                UpgradeSlot.AttachedPanelLayout.CONTENT_PAD * 2 + UpgradeSlot.TITLE_GAP + UpgradeSlot.ROWS * 18,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.upgrades")
        );
        this.upgradeSlots = upgradeSlots;
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
        for (int i = 0; i < UpgradeSlot.COUNT; i++) {
            int col = i % UpgradeSlot.COLS;
            int row = i / UpgradeSlot.COLS;
            int sx = gridX + col * 18;
            int sy = gridY + row * 18;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, sx - 1, sy - 1, 18, 18);
        }
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        for (UpgradeSlot slot : upgradeSlots) {
            slot.setActive(interactive);
        }
    }

    @Override
    protected void onClosed() {
        for (UpgradeSlot slot : upgradeSlots) {
            slot.setActive(false);
        }
    }
}
