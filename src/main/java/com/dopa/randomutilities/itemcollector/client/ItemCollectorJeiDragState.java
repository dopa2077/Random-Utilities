package com.dopa.randomutilities.itemcollector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Tracks JEI ghost-ingredient drag so the collector screen can draw a connector line. */
public final class ItemCollectorJeiDragState {
    /** Soft JEI-green indicator — readable but not heavy. */
    private static final int LINE_COLOR = 0x9913C90A;

    private static boolean active;
    private static int startX;
    private static int startY;

    private ItemCollectorJeiDragState() {}

    public static void beginDrag() {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        var mouse = mc.mouseHandler;
        startX = (int) (mouse.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        startY = (int) (mouse.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
        active = true;
    }

    public static void endDrag() {
        active = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static void renderLine(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!active) {
            return;
        }
        drawLine(graphics, startX, startY, mouseX, mouseY, LINE_COLOR);
    }

    private static void drawLine(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            graphics.fill(x, y, x + 1, y + 1, color);
            if (x == x1 && y == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                y += sy;
            }
        }
    }
}
