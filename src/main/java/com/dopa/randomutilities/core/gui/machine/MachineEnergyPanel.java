package com.dopa.randomutilities.core.gui.machine;

import com.dopa.randomutilities.core.gui.panel.AttachedPanel;
import com.dopa.randomutilities.core.gui.panel.PanelAnchor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Read-only Energy Meter: usage last tick, max receive, stored / capacity. */
public final class MachineEnergyPanel extends AttachedPanel {
    private static final int BG = 0xFF34949a;
    private static final ItemStack TAB_ICON = new ItemStack(Items.LIGHTNING_ROD.weathering().unaffected());
    private static final int USAGE_LABEL_Y = 30;
    private static final int USAGE_VALUE_Y = 42;
    private static final int MAX_LABEL_Y = 56;
    private static final int MAX_VALUE_Y = 68;
    private static final int STORED_LABEL_Y = 82;
    private static final int STORED_VALUE_Y = 94;

    public interface Host {
        int energyStored();

        int energyCapacity();

        int energyUsage();

        int energyMaxReceive();
    }

    private final Host host;

    public MachineEnergyPanel(Host host) {
        super(
                PanelAnchor.RIGHT_TOP,
                136,
                118,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.energy")
        );
        this.host = host;
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(TAB_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.energy.usage"), bodyX, bodyY + USAGE_LABEL_Y);
        drawValue(graphics, font, Component.translatable("gui.dopasrandomutilities.energy.fe_tick", host.energyUsage()),
                bodyX, bodyY + USAGE_VALUE_Y);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.energy.max"), bodyX, bodyY + MAX_LABEL_Y);
        drawValue(graphics, font, Component.translatable("gui.dopasrandomutilities.energy.fe_tick", host.energyMaxReceive()),
                bodyX, bodyY + MAX_VALUE_Y);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.energy.stored"), bodyX, bodyY + STORED_LABEL_Y);
        drawValue(
                graphics,
                font,
                Component.translatable(
                        "gui.dopasrandomutilities.energy.stored_tooltip",
                        host.energyStored(),
                        host.energyCapacity()
                ),
                bodyX,
                bodyY + STORED_VALUE_Y
        );
    }
}
