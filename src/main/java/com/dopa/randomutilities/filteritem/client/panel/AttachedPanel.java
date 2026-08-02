package com.dopa.randomutilities.filteritem.client.panel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * Thermal-style side panel attached to a container GUI: a colored tab that expands
 * into a solid body with slide + fade animation.
 */
public abstract class AttachedPanel {
    public static final int TAB_SIZE = 22;
    public static final int TAB_GAP = 1;
    public static final int TOP_INSET = 4;
    public static final int GUI_WIDTH = 176;
    public static final int CONTENT_PAD = 8;
    public static final float ANIM_DURATION_SEC = 0.18F;
    public static final float WIDGET_SHOW_PROGRESS = 0.92F;

    public enum AnimState {
        CLOSED,
        OPENING,
        OPEN,
        CLOSING
    }

    protected final PanelAnchor anchor;
    protected final int panelWidth;
    protected final int panelHeight;
    protected final int backgroundColor;
    protected final Component title;

    private AnimState state = AnimState.CLOSED;
    private float progress;
    private float animSpeed = 1.0F / ANIM_DURATION_SEC;

    protected AttachedPanel(
            PanelAnchor anchor,
            int panelWidth,
            int panelHeight,
            int backgroundColor,
            Component title
    ) {
        this.anchor = anchor;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
        this.backgroundColor = backgroundColor;
        this.title = title;
    }

    public PanelAnchor anchor() {
        return anchor;
    }

    public AnimState state() {
        return state;
    }

    public float progress() {
        return progress;
    }

    public boolean isOpen() {
        return state == AnimState.OPEN;
    }

    public boolean isAnimating() {
        return state == AnimState.OPENING || state == AnimState.CLOSING;
    }

    public boolean isExpanded() {
        return state == AnimState.OPEN || state == AnimState.OPENING;
    }

    public boolean contentsInteractive() {
        return state == AnimState.OPEN && progress >= WIDGET_SHOW_PROGRESS;
    }

    public void requestOpen() {
        if (state == AnimState.OPEN || state == AnimState.OPENING) {
            return;
        }
        state = AnimState.OPENING;
    }

    public void requestClose() {
        if (state == AnimState.CLOSED || state == AnimState.CLOSING) {
            return;
        }
        state = AnimState.CLOSING;
    }

    public void toggle() {
        if (state == AnimState.OPEN || state == AnimState.OPENING) {
            requestClose();
        } else {
            requestOpen();
        }
    }

    public void tick(float deltaSeconds) {
        if (state == AnimState.OPENING) {
            progress = Math.min(1.0F, progress + deltaSeconds * animSpeed);
            if (progress >= 1.0F) {
                progress = 1.0F;
                state = AnimState.OPEN;
                onOpened();
            }
        } else if (state == AnimState.CLOSING) {
            progress = Math.max(0.0F, progress - deltaSeconds * animSpeed);
            if (progress <= 0.0F) {
                progress = 0.0F;
                state = AnimState.CLOSED;
                onClosed();
            }
        }
        onTick();
        updateWidgetVisibility(contentsInteractive());
    }

    /** Smoothstep easing for slide + fade. */
    public static float ease(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * (3.0F - 2.0F * t);
    }

    public int tabOffsetY() {
        return TOP_INSET + anchor.stackIndex() * (TAB_SIZE + TAB_GAP);
    }

    public int tabX(int leftPos, int imageWidth) {
        return anchor.isLeft() ? leftPos - TAB_SIZE : leftPos + imageWidth;
    }

    public int tabY(int topPos) {
        return topPos + tabOffsetY();
    }

    public int panelWidth() {
        return panelWidth;
    }

    public int panelHeight() {
        return panelHeight;
    }

    public boolean isMouseOverTab(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        int x = tabX(leftPos, imageWidth);
        int y = tabY(topPos);
        return mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= y && mouseY < y + TAB_SIZE;
    }

    /**
     * Fully-open body origin relative to the GUI (leftPos/topPos space uses absolute screen coords).
     */
    public int bodyXOpen(int leftPos, int imageWidth) {
        return anchor.isLeft() ? leftPos - panelWidth : leftPos + imageWidth;
    }

    public int bodyY(int topPos) {
        return topPos + tabOffsetY();
    }

    /** Animated body X for the current eased progress. */
    public int bodyXAnimated(int leftPos, int imageWidth) {
        float e = ease(progress);
        int full = panelWidth;
        int visible = Math.max(0, Math.round(full * e));
        if (anchor.isLeft()) {
            return leftPos - visible;
        }
        return leftPos + imageWidth;
    }

    public int bodyWidthAnimated() {
        return Math.max(0, Math.round(panelWidth * ease(progress)));
    }

    public int contentX(int leftPos, int imageWidth) {
        return bodyXOpen(leftPos, imageWidth) + CONTENT_PAD;
    }

    public int contentY(int topPos) {
        return bodyY(topPos) + CONTENT_PAD;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                       int mouseX, int mouseY, float partialTick) {
        renderTab(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
        renderBodyIfOpen(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);
    }

    /** Draw only the tab strip icon (first pass so bodies can paint over tabs). */
    public void renderTabOnly(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                              int mouseX, int mouseY) {
        renderTab(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
    }

    /** Draw the expanding body if this panel is open/animating (second pass). */
    public void renderBodyIfOpen(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                                 int mouseX, int mouseY, float partialTick) {
        if (progress > 0.001F) {
            renderBody(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);
        }
    }

    protected void renderTab(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                             int mouseX, int mouseY) {
        int x = tabX(leftPos, imageWidth);
        int y = tabY(topPos);
        boolean hovered = mouseX >= x && mouseX < x + TAB_SIZE && mouseY >= y && mouseY < y + TAB_SIZE;
        int bg = hovered || isExpanded() ? lighten(backgroundColor, 16) : backgroundColor;
        fillPanel(graphics, x, y, TAB_SIZE, TAB_SIZE, bg);
        renderIcon(graphics, font, x + TAB_SIZE / 2, y + TAB_SIZE / 2);
    }

    protected void renderBody(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                              int mouseX, int mouseY, float partialTick) {
        float e = ease(progress);
        int w = bodyWidthAnimated();
        if (w <= 0) {
            return;
        }
        int x = bodyXAnimated(leftPos, imageWidth);
        int y = bodyY(topPos);
        int alpha = Math.round(e * 255.0F) & 0xFF;
        int bg = (alpha << 24) | (backgroundColor & 0x00FFFFFF);
        fillPanel(graphics, x, y, w, panelHeight, bg);

        if (e >= 0.55F && w >= panelWidth - 2) {
            renderContents(graphics, font, bodyXOpen(leftPos, imageWidth), y, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Vanilla/Thermal-style rounded panel: 2px cut corners with light/dark edge bevel.
     */
    protected void fillPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) {
            return;
        }
        final int r = 2;
        if (w <= r * 2 || h <= r * 2) {
            graphics.fill(x, y, x + w, y + h, argb);
            return;
        }

        // Center + edge strips (leave outer corner pixels empty for the cut)
        graphics.fill(x + r, y, x + w - r, y + h, argb);
        graphics.fill(x, y + r, x + r, y + h - r, argb);
        graphics.fill(x + w - r, y + r, x + w, y + h - r, argb);

        // Inner corner steps (radius 2)
        graphics.fill(x + 1, y + 1, x + r, y + r, argb);
        graphics.fill(x + w - r, y + 1, x + w - 1, y + r, argb);
        graphics.fill(x + 1, y + h - r, x + r, y + h - 1, argb);
        graphics.fill(x + w - r, y + h - r, x + w - 1, y + h - 1, argb);

        int borderLight = lighten(argb, 22);
        int borderDark = darken(argb, 28);

        // Horizontal borders (inset for corners)
        graphics.fill(x + r, y, x + w - r, y + 1, borderLight);
        graphics.fill(x + r, y + h - 1, x + w - r, y + h, borderDark);
        // Vertical borders
        graphics.fill(x, y + r, x + 1, y + h - r, borderLight);
        graphics.fill(x + w - 1, y + r, x + w, y + h - r, borderDark);

        // Corner border pixels
        graphics.fill(x + 1, y + 1, x + 2, y + 2, borderLight);
        graphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, borderLight);
        graphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, borderDark);
        graphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, borderDark);
    }

    protected static int darken(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.max(0, ((argb >>> 16) & 0xFF) - amount);
        int g = Math.max(0, ((argb >>> 8) & 0xFF) - amount);
        int b = Math.max(0, (argb & 0xFF) - amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    protected static int lighten(int argb, int amount) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >>> 16) & 0xFF) + amount);
        int g = Math.min(255, ((argb >>> 8) & 0xFF) + amount);
        int b = Math.min(255, (argb & 0xFF) + amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    protected abstract void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY);

    protected abstract void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                           int mouseX, int mouseY, float partialTick);

    /** Called once when the panel finishes opening. Subclasses create/reposition widgets here if needed. */
    protected void onOpened() {}

    protected void onClosed() {}

    protected void onTick() {}

    /** Show/hide and enable widgets owned by this panel. */
    protected void updateWidgetVisibility(boolean interactive) {}

    /** Reposition widgets to match the open body. Called from screen init and when layout changes. */
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {}
}
