package com.dopa.randomutilities.filteritem.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class EnergyPanel extends AttachedPanel {
    private static final int BG = 0xFF3A5570;

    public EnergyPanel() {
        super(
                PanelAnchor.RIGHT_TOP,
                96,
                64,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.energy")
        );
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        // Lightning bolt silhouette
        int c = 0xFFB8D4F0;
        graphics.fill(centerX - 1, centerY - 7, centerX + 2, centerY - 1, c);
        graphics.fill(centerX - 4, centerY - 1, centerX + 3, centerY + 1, c);
        graphics.fill(centerX - 1, centerY + 1, centerX + 2, centerY + 7, c);
        graphics.fill(centerX + 1, centerY + 2, centerX + 5, centerY + 4, c);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        graphics.text(font, title, bodyX + CONTENT_PAD, bodyY + CONTENT_PAD, 0xFFE8F0FF, false);
        graphics.text(font, Component.translatable("gui.dopasrandomutilities.panel.energy.empty"),
                bodyX + CONTENT_PAD, bodyY + CONTENT_PAD + 14, 0xFFA0B8D0, false);
    }
}
