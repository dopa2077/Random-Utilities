package com.dopa.randomutilities.core.gui.widget;

import com.dopa.randomutilities.core.gui.panel.PanelLayout;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Shared vertical text scrollbar for info panels: wheel, track click, and thumb drag.
 * Geometry is refreshed from {@link #render}; hit testing uses a wider pad than the 2px draw width.
 */
public final class PanelTextScrollbar {
    private static final int TRACK_WIDTH = 2;
    private static final int HIT_PAD = 4;
    private static final int MIN_THUMB = 6;
    private static final int TRACK_COLOR = 0x66000000;
    private static final int THUMB_COLOR = 0xFFC0C0C0;

    private int scrollPixels;
    private boolean dragging;
    private int dragOffsetY;

    private int viewTop;
    private int viewHeight;
    private int contentHeight;
    private int maxScroll;
    private int trackX;
    private int thumbY;
    private int thumbHeight;
    private boolean visible;

    public void reset() {
        scrollPixels = 0;
        endDrag();
        visible = false;
        maxScroll = 0;
    }

    public int scrollPixels() {
        return scrollPixels;
    }

    public boolean isDragging() {
        return dragging;
    }

    /**
     * Clamps scroll against the current content and returns the scroll offset to apply when drawing text.
     */
    public int begin(int contentHeight, int viewHeight) {
        this.contentHeight = Math.max(0, contentHeight);
        this.viewHeight = Math.max(0, viewHeight);
        maxScroll = Math.max(0, this.contentHeight - this.viewHeight);
        scrollPixels = Mth.clamp(scrollPixels, 0, maxScroll);
        visible = maxScroll > 0;
        if (!visible) {
            endDrag();
        }
        return scrollPixels;
    }

    public void render(GuiGraphicsExtractor graphics, int bodyX, int panelWidth, int viewTop, int viewBottom) {
        this.viewTop = viewTop;
        this.viewHeight = Math.max(0, viewBottom - viewTop);
        trackX = bodyX + panelWidth - PanelLayout.CONTENT_PAD + 1;
        if (!visible) {
            return;
        }
        graphics.fill(trackX, viewTop, trackX + TRACK_WIDTH, viewBottom, TRACK_COLOR);
        thumbHeight = Math.max(MIN_THUMB, Math.round(viewHeight * (viewHeight / (float) Math.max(1, contentHeight))));
        int thumbTravel = Math.max(0, viewHeight - thumbHeight);
        thumbY = viewTop + (maxScroll == 0 ? 0 : Math.round(thumbTravel * (scrollPixels / (float) maxScroll)));
        graphics.fill(trackX, thumbY, trackX + TRACK_WIDTH, thumbY + thumbHeight, THUMB_COLOR);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible) {
            return false;
        }
        int hitX = trackX - HIT_PAD;
        int hitW = TRACK_WIDTH + HIT_PAD * 2;
        return mouseX >= hitX && mouseX < hitX + hitW
                && mouseY >= viewTop && mouseY < viewTop + viewHeight;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        int thumbTravel = Math.max(1, viewHeight - thumbHeight);
        if (mouseY < thumbY || mouseY >= thumbY + thumbHeight) {
            float ratio = (float) ((mouseY - viewTop - thumbHeight / 2.0) / thumbTravel);
            scrollPixels = Mth.clamp(Math.round(ratio * maxScroll), 0, maxScroll);
            thumbY = viewTop + Math.round(thumbTravel * (scrollPixels / (float) maxScroll));
        }
        dragging = true;
        dragOffsetY = (int) mouseY - thumbY;
        return true;
    }

    public boolean mouseDragged(double mouseY) {
        if (!dragging || !visible) {
            return false;
        }
        int thumbTravel = Math.max(0, viewHeight - thumbHeight);
        int newThumbY = Mth.clamp((int) mouseY - dragOffsetY, viewTop, viewTop + thumbTravel);
        float ratio = thumbTravel == 0 ? 0.0F : (newThumbY - viewTop) / (float) thumbTravel;
        scrollPixels = Mth.clamp(Math.round(ratio * maxScroll), 0, maxScroll);
        thumbY = newThumbY;
        return true;
    }

    public boolean mouseReleased() {
        if (!dragging) {
            return false;
        }
        endDrag();
        return true;
    }

    /** @param pixelDelta positive scrolls content up (wheel up); typically {@code scrollY * lineStep}. */
    public boolean mouseScrolled(double pixelDelta) {
        if (!visible || maxScroll <= 0 || pixelDelta == 0.0D) {
            return false;
        }
        scrollPixels = Mth.clamp(scrollPixels - (int) Math.round(pixelDelta), 0, maxScroll);
        return true;
    }

    private void endDrag() {
        dragging = false;
        dragOffsetY = 0;
    }
}
