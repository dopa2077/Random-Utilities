package com.dopa.randomutilities.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** 16×16 hitbox over a filter-mode well; no vanilla button sprites. */
public final class FilterModeButton extends AbstractWidget {
    private final Runnable onPress;

    public FilterModeButton(int x, int y, Component tooltip, Runnable onPress) {
        super(x, y, FilterModeIcon.SIZE, FilterModeIcon.SIZE, tooltip);
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(tooltip));
    }

    public void updateTooltip(Component tooltip) {
        this.setMessage(tooltip);
        this.setTooltip(Tooltip.create(tooltip));
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.active) {
            this.onPress.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Idle well comes from FilterRow; hover uses vanilla button_highlighted in FilterModeIcon.
    }
}
