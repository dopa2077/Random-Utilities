package com.dopa.randomutilities.core.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/** Tracks JEI ghost-ingredient drag so screens can draw a connector line. */
public final class JeiGhostDragState {
    /** Soft JEI-green indicator — readable but not heavy. */
    private static final int LINE_COLOR = 0x9913C90A;

    private static boolean active;
    private static double startX;
    private static double startY;

    private JeiGhostDragState() {}

    public static void beginDrag() {
        // Creative JEI click-to-cursor also hits the ghost handler with doStart=true.
        // Only show the line while the mouse is actually held (true drag).
        if (!isMouseButtonHeld()) {
            return;
        }
        double[] pos = guiMousePos();
        startX = pos[0];
        startY = pos[1];
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
        // Drop stale state from click-pickups that never got onComplete().
        if (!isMouseButtonHeld()) {
            endDrag();
            return;
        }
        drawSmoothLine(graphics, startX, startY, mouseX, mouseY, LINE_COLOR);
    }

    private static boolean isMouseButtonHeld() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return false;
        }
        long window = mc.getWindow().handle();
        return GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
    }

    private static double[] guiMousePos() {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getWindow();
        var mouse = mc.mouseHandler;
        double x = mouse.xpos() * (double) window.getGuiScaledWidth() / (double) window.getScreenWidth();
        double y = mouse.ypos() * (double) window.getGuiScaledHeight() / (double) window.getScreenHeight();
        return new double[] {x, y};
    }

    /** Xiaolin Wu anti-aliased line — avoids the stair-step look of Bresenham fills. */
    private static void drawSmoothLine(
            GuiGraphicsExtractor graphics, double x0, double y0, double x1, double y1, int color) {
        boolean steep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
        if (steep) {
            double t = x0;
            x0 = y0;
            y0 = t;
            t = x1;
            x1 = y1;
            y1 = t;
        }
        if (x0 > x1) {
            double t = x0;
            x0 = x1;
            x1 = t;
            t = y0;
            y0 = y1;
            y1 = t;
        }

        double dx = x1 - x0;
        double dy = y1 - y0;
        double gradient = dx == 0.0 ? 1.0 : dy / dx;

        int baseAlpha = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        double xEnd = Math.round(x0);
        double yEnd = y0 + gradient * (xEnd - x0);
        double xGap = rfpart(x0 + 0.5);
        int xpxl1 = (int) xEnd;
        int ypxl1 = (int) Math.floor(yEnd);
        plot(graphics, steep, xpxl1, ypxl1, rfpart(yEnd) * xGap, baseAlpha, rgb);
        plot(graphics, steep, xpxl1, ypxl1 + 1, fpart(yEnd) * xGap, baseAlpha, rgb);
        double intery = yEnd + gradient;

        xEnd = Math.round(x1);
        yEnd = y1 + gradient * (xEnd - x1);
        xGap = fpart(x1 + 0.5);
        int xpxl2 = (int) xEnd;
        int ypxl2 = (int) Math.floor(yEnd);
        plot(graphics, steep, xpxl2, ypxl2, rfpart(yEnd) * xGap, baseAlpha, rgb);
        plot(graphics, steep, xpxl2, ypxl2 + 1, fpart(yEnd) * xGap, baseAlpha, rgb);

        for (int x = xpxl1 + 1; x < xpxl2; x++) {
            plot(graphics, steep, x, (int) Math.floor(intery), rfpart(intery), baseAlpha, rgb);
            plot(graphics, steep, x, (int) Math.floor(intery) + 1, fpart(intery), baseAlpha, rgb);
            intery += gradient;
        }
    }

    private static void plot(
            GuiGraphicsExtractor graphics, boolean steep, int x, int y, double coverage, int baseAlpha, int rgb) {
        if (coverage <= 0.004) {
            return;
        }
        int alpha = Mth.clamp((int) Math.round(baseAlpha * coverage), 0, 255);
        if (alpha <= 0) {
            return;
        }
        int px = steep ? y : x;
        int py = steep ? x : y;
        graphics.fill(px, py, px + 1, py + 1, (alpha << 24) | rgb);
    }

    private static double fpart(double x) {
        return x - Math.floor(x);
    }

    private static double rfpart(double x) {
        return 1.0 - fpart(x);
    }
}
