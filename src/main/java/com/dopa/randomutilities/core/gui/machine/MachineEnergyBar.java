package com.dopa.randomutilities.core.gui.machine;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Vertical FE meter: empty track is baked into the machine GUI; this draws the fill. */
public final class MachineEnergyBar {
    public static final Identifier FILL =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/widget/energy_bar_full.png");
    public static final int FILL_WIDTH = 11;
    public static final int FILL_HEIGHT = 62;

    private MachineEnergyBar() {}

    public static void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int stored, int capacity) {
        if (capacity <= 0 || stored <= 0) {
            return;
        }
        int fillH = (int) ((long) stored * height / capacity);
        if (fillH <= 0 && stored > 0) {
            fillH = 1;
        }
        fillH = Math.min(height, fillH);
        int v = height - fillH;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FILL,
                x,
                y + v,
                0.0F,
                (float) v,
                width,
                fillH,
                FILL_WIDTH,
                FILL_HEIGHT
        );
    }

    public static boolean isHover(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public static void renderHoverTooltip(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            int stored,
            int capacity
    ) {
        graphics.setTooltipForNextFrame(
                font,
                Component.translatable("gui.dopasrandomutilities.energy.stored_tooltip", stored, capacity),
                mouseX,
                mouseY
        );
    }
}
