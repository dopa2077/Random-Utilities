package com.dopa.randomutilities.core.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Whitelist / blacklist icons from packed cells on {@link FilterRow#TEXTURE}. */
public final class FilterModeIcon {
    public static final int SIZE = 16;
    private static final int TEXTURE_SIZE = 256;
    private static final Identifier BUTTON_HIGHLIGHT =
            Identifier.withDefaultNamespace("widget/button_highlighted");
    /** Inner 16×16 of the packed 18×18 red guides (skip the red). */
    private static final int WHITELIST_U = 177;
    private static final int WHITELIST_V = 1;
    private static final int BLACKLIST_U = 194;
    private static final int BLACKLIST_V = 1;

    private FilterModeIcon() {}

    public static void render(GuiGraphicsExtractor graphics, boolean whitelist, int x, int y, boolean hovered) {
        if (hovered) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_HIGHLIGHT,
                    x,
                    y,
                    SIZE,
                    SIZE
            );
        }
        if (whitelist) {
            blitCell(graphics, x, y, WHITELIST_U, WHITELIST_V);
        } else {
            blitCell(graphics, x, y, BLACKLIST_U, BLACKLIST_V);
        }
    }

    private static void blitCell(GuiGraphicsExtractor graphics, int x, int y, int u, int v) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                FilterRow.TEXTURE,
                x,
                y,
                (float) u,
                (float) v,
                SIZE,
                SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }
}
