package com.dopa.randomutilities.core.gui.widget;

import com.dopa.randomutilities.core.filter.menu.GhostFilterSlot;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntPredicate;

/**
 * Strip from {@code textures/gui/widget/filter_row.png}: mode well, then ghost wells.
 * Blit at the mode-icon 16×16 origin; wells are 18px apart (same as slot spacing).
 * Occupied wells overlay the third packed cell (plain slot, no paper).
 */
public final class FilterRow {
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/widget/filter_row.png");
    public static final int WELL = 18;
    public static final int HEIGHT = 18;
    private static final int TEXTURE_SIZE = 256;
    /** First well frame; inner 16×16 sits at u=8, matching {@code FILTER_ICON_X = 8}. */
    private static final int STRIP_U = 7;
    private static final int STRIP_V = 0;
    /** Inner 16×16 of the third 18×18 red guide (empty slot, no paper). */
    private static final int OCCUPIED_U = 211;
    private static final int OCCUPIED_V = 1;

    private FilterRow() {}

    public static void blit(GuiGraphicsExtractor graphics, int iconX, int iconY, int slotCount) {
        blit(graphics, iconX, iconY, slotCount, i -> false);
    }

    public static void blit(
            GuiGraphicsExtractor graphics,
            int iconX,
            int iconY,
            int slotCount,
            IntPredicate occupied
    ) {
        int width = WELL * (1 + slotCount);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                TEXTURE,
                iconX - 1,
                iconY - 1,
                (float) STRIP_U,
                (float) STRIP_V,
                width,
                HEIGHT,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
        for (int i = 0; i < slotCount; i++) {
            if (!occupied.test(i)) {
                continue;
            }
            graphics.blit(
                    RenderPipelines.GUI_TEXTURED,
                    TEXTURE,
                    iconX + WELL * (i + 1),
                    iconY,
                    (float) OCCUPIED_U,
                    (float) OCCUPIED_V,
                    FilterModeIcon.SIZE,
                    FilterModeIcon.SIZE,
                    TEXTURE_SIZE,
                    TEXTURE_SIZE
            );
        }
    }

    /** Semi-transparent wash over a ghost item, same look as occupied filter-row wells. */
    public static void tintGhostItem(GuiGraphicsExtractor graphics, int leftPos, int topPos, Slot slot) {
        if (!slot.hasItem()) {
            return;
        }
        int x = leftPos + slot.x;
        int y = topPos + slot.y;
        graphics.fill(x, y, x + FilterModeIcon.SIZE, y + FilterModeIcon.SIZE, 0x40FFFFFF);
    }

    /** Empty ghost-filter wells: occupied slots keep the vanilla item tooltip. */
    public static boolean applyEmptyHover(
            GuiGraphicsExtractor graphics,
            Font font,
            int mouseX,
            int mouseY,
            @Nullable Slot hovered
    ) {
        if (!(hovered instanceof GhostFilterSlot) || hovered.hasItem()) {
            return false;
        }
        graphics.setTooltipForNextFrame(
                font,
                Component.translatable("gui.dopasrandomutilities.filter.ghost"),
                mouseX,
                mouseY
        );
        return true;
    }
}
