package com.dopa.randomutilities.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Square control drawn from a 16×16 PNG (chrome included). No vanilla {@code widget/button} sprite.
 */
public final class IconButton extends AbstractWidget {
    public static final int TEXTURE_SIZE = 16;

    private final Identifier texture;
    private final Runnable onPress;

    public IconButton(int x, int y, int size, Identifier texture, Component tooltip, Runnable onPress) {
        super(x, y, size, size, Component.empty());
        this.texture = texture;
        this.onPress = onPress;
        this.setTooltip(Tooltip.create(tooltip));
    }

    public void updateTooltip(Component tooltip) {
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
        int x = this.getX();
        int y = this.getY();
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.texture,
                x,
                y,
                0.0F,
                0.0F,
                this.width,
                this.height,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }
}
