package com.dopa.randomutilities.filtersystem.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class EnergyPanel extends AttachedPanel {
    private static final int BG = 0xFF2A66B0;
    private static final ItemStack ENERGY_ICON = new ItemStack(Items.LIGHTNING_ROD.weathering().unaffected());

    public EnergyPanel() {
        super(
                PanelAnchor.RIGHT_TOP,
                108,
                64,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.energy")
        );
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(ENERGY_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        int statusY = bodyY + TITLE_ROW_HEIGHT + 2;
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.energy.status"),
                bodyX, statusY);
        drawValue(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.energy.empty"),
                bodyX, statusY + font.lineHeight + 1);
    }
}
