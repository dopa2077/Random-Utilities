package com.dopa.randomutilities.filteritem.client.panel;

import com.dopa.randomutilities.filteritem.menu.UpgradeSlot;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class UpgradePanel extends AttachedPanel {
    private static final int BG = 0xFF2F4A38;
    private static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

    private final List<UpgradeSlot> upgradeSlots;

    public UpgradePanel(List<UpgradeSlot> upgradeSlots) {
        super(
                PanelAnchor.RIGHT_BELOW,
                UpgradeSlot.AttachedPanelLayout.CONTENT_PAD * 2 + UpgradeSlot.COLS * 18,
                UpgradeSlot.AttachedPanelLayout.CONTENT_PAD * 2 + UpgradeSlot.TITLE_GAP + UpgradeSlot.ROWS * 18,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.upgrades")
        );
        this.upgradeSlots = upgradeSlots;
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        int c = 0xFFB8E0C0;
        // Soft upgrade arrow / chip
        graphics.fill(centerX - 5, centerY + 3, centerX + 6, centerY + 6, c);
        graphics.fill(centerX - 2, centerY - 5, centerX + 3, centerY + 4, c);
        graphics.fill(centerX - 5, centerY - 2, centerX + 6, centerY + 1, 0xFF7AAD88);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        graphics.text(font, title, bodyX + CONTENT_PAD, bodyY + CONTENT_PAD, 0xFFD8F0E0, false);
        int gridY = bodyY + CONTENT_PAD + UpgradeSlot.TITLE_GAP;
        for (int i = 0; i < UpgradeSlot.COUNT; i++) {
            int col = i % UpgradeSlot.COLS;
            int row = i / UpgradeSlot.COLS;
            int sx = bodyX + CONTENT_PAD + col * 18;
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
