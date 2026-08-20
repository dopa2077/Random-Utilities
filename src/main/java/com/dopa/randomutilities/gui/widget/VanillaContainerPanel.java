package com.dopa.randomutilities.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared vanilla chest chrome for stitched container backgrounds (not a per-block texture). */
public final class VanillaContainerPanel {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int PANEL_BORDER = 7;
    private static final int PANEL_HEADER = 17;
    private static final int DIVIDER_V = 126;
    private static final int DIVIDER_STRIP_H = 7;
    private static final int FOOTER_BOTTOM_V = 215;
    private static final int INTERIOR_U = 7;
    private static final int INTERIOR_W = 162;

    private VanillaContainerPanel() {}

    public static void blit(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        blitRegion(graphics, x, y, PANEL_BORDER, PANEL_HEADER, 0.0F, 0.0F, PANEL_BORDER, PANEL_HEADER);
        blitRegion(graphics, x + PANEL_BORDER, y, width - PANEL_BORDER * 2, PANEL_HEADER,
                INTERIOR_U, 0.0F, INTERIOR_W, PANEL_HEADER);
        blitRegion(graphics, x + width - PANEL_BORDER, y, PANEL_BORDER, PANEL_HEADER,
                176 - PANEL_BORDER, 0.0F, PANEL_BORDER, PANEL_HEADER);
        int bodyY = y + PANEL_HEADER;
        int bodyHeight = height - PANEL_HEADER - PANEL_BORDER;
        int bottomY = y + height - PANEL_BORDER;
        if (bodyHeight > 0) {
            blitRegion(graphics, x, bodyY, PANEL_BORDER, bodyHeight, 0.0F, DIVIDER_V, PANEL_BORDER, DIVIDER_STRIP_H);
            blitRegion(graphics, x + PANEL_BORDER, bodyY, width - PANEL_BORDER * 2, bodyHeight,
                    INTERIOR_U, DIVIDER_V, INTERIOR_W, DIVIDER_STRIP_H);
            blitRegion(graphics, x + width - PANEL_BORDER, bodyY, PANEL_BORDER, bodyHeight,
                    176 - PANEL_BORDER, DIVIDER_V, PANEL_BORDER, DIVIDER_STRIP_H);
        }
        blitRegion(graphics, x, bottomY, PANEL_BORDER, PANEL_BORDER, 0.0F, FOOTER_BOTTOM_V, PANEL_BORDER, PANEL_BORDER);
        blitRegion(graphics, x + PANEL_BORDER, bottomY, width - PANEL_BORDER * 2, PANEL_BORDER,
                INTERIOR_U, FOOTER_BOTTOM_V, INTERIOR_W, PANEL_BORDER);
        blitRegion(graphics, x + width - PANEL_BORDER, bottomY, PANEL_BORDER, PANEL_BORDER,
                176 - PANEL_BORDER, FOOTER_BOTTOM_V, PANEL_BORDER, PANEL_BORDER);
    }

    public static void blitRegion(
            GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            float u, float v, int srcWidth, int srcHeight
    ) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, x, y, u, v,
                width, height, srcWidth, srcHeight, TEXTURE_SIZE, TEXTURE_SIZE);
    }
}
