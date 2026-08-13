package com.dopa.randomutilities.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Thermal-style side panel attached to a container GUI: a colored tab that expands
 * into a solid body with slide + fade animation.
 */
public abstract class AttachedPanel {
    public static final int TAB_SIZE = 22;
    public static final int TAB_GAP = 0;
    public static final int TOP_INSET = 4;
    public static final int GUI_WIDTH = 176;
    public static final int CONTENT_PAD = 8;
    public static final float ANIM_DURATION_SEC = 0.12F;
    public static final float WIDGET_SHOW_PROGRESS = 0.92F;

    /** Grey labels inside expanded panels (normal weight). */
    public static final int LABEL_COLOR = 0xFFA0A0A0;
    /** Black normal values inside expanded panels. */
    public static final int VALUE_COLOR = 0xFF000000;
    /** Open-panel title color (bold yellow). */
    public static final int TITLE_COLOR = 0xFFFFFF55;
    protected static final int TITLE_ICON_RADIUS = 8;
    /** Vertical space reserved for icon + single-line title. */
    public static final int TITLE_ROW_HEIGHT = CONTENT_PAD + TITLE_ICON_RADIUS * 2;

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

    public Component title() {
        return title;
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

    /** True while any of the body is still drawn (open, opening, or closing). */
    public boolean isOccupying() {
        return progress > 0.001F || state != AnimState.CLOSED;
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

    /** Instantly open with no animation (e.g. after menu rebuild). */
    public void snapOpen() {
        state = AnimState.OPEN;
        progress = 1.0F;
        onOpened();
        updateWidgetVisibility(contentsInteractive());
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

    /** Smootherstep easing for slide + fade. */
    public static float ease(float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        return t * t * t * (t * (t * 6.0F - 15.0F) + 10.0F);
    }

    public int tabOffsetY() {
        return TOP_INSET + anchor.stackIndex() * (TAB_SIZE + TAB_GAP);
    }

    public int tabX(int leftPos, int imageWidth) {
        if (anchor.isLeft()) {
            // Ride the outer (left) corner; at progress 0 this equals the closed tab home.
            return leftPos - bodyWidthAnimated() + 1;
        }
        // Right tabs stay pinned at the inventory attachment edge.
        return leftPos + imageWidth - 1;
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

    public boolean isMouseOverBody(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (progress <= 0.001F) {
            return false;
        }
        int w = bodyWidthAnimated();
        int h = bodyHeightAnimated();
        if (w <= 0 || h <= 0) {
            return false;
        }
        int x = bodyXAnimated(leftPos, imageWidth);
        int y = bodyY(topPos);
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /**
     * Non-widget decorative regions (e.g. button trays) that should consume clicks without closing the panel.
     */
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        return false;
    }

    protected record TrayBounds(int x, int y, int width, int height) {}

    protected static TrayBounds trayBounds(int bodyX, int panelWidth, int groupWidth, int groupY, int groupHeight, int trayPad) {
        int trayX = bodyX + (panelWidth - groupWidth) / 2 - trayPad;
        int trayY = groupY - trayPad;
        int trayW = groupWidth + trayPad * 2;
        int trayH = groupHeight + trayPad * 2;
        return new TrayBounds(trayX, trayY, trayW, trayH);
    }

    /** Tray whose content group starts at {@code groupX}/{@code groupY} (not centered). */
    protected static TrayBounds trayBoundsAt(int groupX, int groupY, int groupWidth, int groupHeight, int trayPad) {
        return new TrayBounds(
                groupX - trayPad,
                groupY - trayPad,
                groupWidth + trayPad * 2,
                groupHeight + trayPad * 2
        );
    }

    /** Tray around a full-width inner button ({@link #CONTENT_PAD} on each side). */
    protected TrayBounds innerButtonTray(int bodyX, int bodyY, int buttonY, int buttonH, int trayPad) {
        return trayBoundsAt(
                bodyX + CONTENT_PAD,
                bodyY + buttonY,
                panelWidth - CONTENT_PAD * 2,
                buttonH,
                trayPad
        );
    }

    protected void renderTray(GuiGraphicsExtractor graphics, TrayBounds tray, int bgColor) {
        graphics.fill(tray.x(), tray.y(), tray.x() + tray.width(), tray.y() + tray.height(), darken(bgColor, 40));
    }

    protected static boolean isMouseOverRect(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /**
     * Fully-open body origin relative to the GUI (leftPos/topPos space uses absolute screen coords).
     */
    public int bodyXOpen(int leftPos, int imageWidth) {
        return anchor.isLeft() ? leftPos - panelWidth + 1 : leftPos + imageWidth - 1;
    }

    public int bodyY(int topPos) {
        return topPos + tabOffsetY();
    }

    /** Animated body X for the current eased progress. */
    public int bodyXAnimated(int leftPos, int imageWidth) {
        int visible = bodyWidthAnimated();
        if (anchor.isLeft()) {
            return leftPos - visible + 1;
        }
        return leftPos + imageWidth - 1;
    }

    /** Width grows from tab size to full panel width. */
    public int bodyWidthAnimated() {
        float e = ease(progress);
        return Math.max(TAB_SIZE, Math.round(TAB_SIZE + (panelWidth - TAB_SIZE) * e));
    }

    /** Height grows from tab size to full panel height. */
    public int bodyHeightAnimated() {
        float e = ease(progress);
        return Math.max(TAB_SIZE, Math.round(TAB_SIZE + (panelHeight - TAB_SIZE) * e));
    }

    public int contentX(int leftPos, int imageWidth) {
        return bodyXOpen(leftPos, imageWidth) + CONTENT_PAD;
    }

    public int contentY(int topPos) {
        return bodyY(topPos) + CONTENT_PAD;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                       int mouseX, int mouseY, float partialTick) {
        renderBodyIfOpen(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);
        renderTab(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
    }

    /** Draw only the tab strip icon. */
    public void renderTabOnly(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                              int mouseX, int mouseY) {
        renderTab(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
    }

    /** Icon at the tab center with no chrome — used while the panel body is open/animating. */
    public void renderTabIconOnly(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth) {
        int x = tabX(leftPos, imageWidth);
        int y = tabY(topPos);
        renderIcon(graphics, font, x + TAB_SIZE / 2, y + TAB_SIZE / 2);
    }

    /** Draw the expanding body if this panel is open/animating. */
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
        fillPanel(graphics, x, y, TAB_SIZE, TAB_SIZE, bg, attachmentSide());
        renderIcon(graphics, font, x + TAB_SIZE / 2, y + TAB_SIZE / 2);
    }

    protected void renderBody(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                              int mouseX, int mouseY, float partialTick) {
        float e = ease(progress);
        int w = bodyWidthAnimated();
        int h = bodyHeightAnimated();
        if (w <= 0 || h <= 0) {
            return;
        }
        int x = bodyXAnimated(leftPos, imageWidth);
        int y = bodyY(topPos);
        // Size-only expand (always opaque) — avoids icon compositing with a fading body and
        // removes the need for a sharp TAB_SIZE pad that looked like a small button overlay.
        int bg = 0xFF000000 | (backgroundColor & 0x00FFFFFF);
        fillPanel(graphics, x, y, w, h, bg, attachmentSide());

        if (e >= 0.55F && w >= panelWidth - 2 && h >= panelHeight - 2) {
            renderContents(graphics, font, bodyXOpen(leftPos, imageWidth), y, mouseX, mouseY, partialTick);
        }
    }

    /** Edge that tucks under the inventory frame — skip that side's bevel. */
    private AttachmentSide attachmentSide() {
        return anchor.isLeft() ? AttachmentSide.RIGHT : AttachmentSide.LEFT;
    }

    private enum AttachmentSide {
        NONE,
        LEFT,
        RIGHT
    }

    /**
     * Vanilla/Thermal-style rounded panel: 2px cut corners with light/dark edge bevel.
     * Attachment side skips the vertical bevel so the panel reads as under the inventory frame.
     */
    protected void fillPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int argb) {
        fillPanel(graphics, x, y, w, h, argb, AttachmentSide.NONE);
    }

    private void fillPanel(GuiGraphicsExtractor graphics, int x, int y, int w, int h, int argb, AttachmentSide attach) {
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

        // Vanilla inventory-style bevel: bright top bar, slightly lighter left, dark right + bottom
        int borderTop = lighten(argb, 48);
        int borderLeft = lighten(argb, 28);
        int borderDark = darken(argb, 52);
        int borderDarker = darken(argb, 68);

        // Top highlight (stronger horizontal bar) — stop short of attachment edge
        int topLeft = attach == AttachmentSide.LEFT ? x : x + r;
        int topRight = attach == AttachmentSide.RIGHT ? x + w : x + w - r;
        graphics.fill(topLeft, y, topRight, y + 1, borderTop);
        // Bottom dark bar + inset deepen
        graphics.fill(topLeft, y + h - 1, topRight, y + h, borderDark);
        if (h > 3) {
            graphics.fill(topLeft, y + h - 2, topRight, y + h - 1, borderDarker);
        }
        // Left light / right dark vertical bars; soft shadow on attachment edge
        if (attach == AttachmentSide.LEFT) {
            graphics.fill(x, y + r, x + 1, y + h - r, darken(argb, 36));
        } else {
            graphics.fill(x, y + r, x + 1, y + h - r, borderLeft);
        }
        if (attach == AttachmentSide.RIGHT) {
            graphics.fill(x + w - 1, y + r, x + w, y + h - r, darken(argb, 36));
        } else {
            graphics.fill(x + w - 1, y + r, x + w, y + h - r, borderDark);
            if (w > 3) {
                graphics.fill(x + w - 2, y + r, x + w - 1, y + h - r, borderDarker);
            }
        }

        // Corner border pixels (skip attachment-side corners)
        if (attach != AttachmentSide.LEFT) {
            graphics.fill(x + 1, y + 1, x + 2, y + 2, borderTop);
            graphics.fill(x + 1, y + h - 2, x + 2, y + h - 1, borderDark);
        }
        if (attach != AttachmentSide.RIGHT) {
            graphics.fill(x + w - 2, y + 1, x + w - 1, y + 2, borderTop);
            graphics.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, borderDark);
        }
    }

    protected int contentInnerWidth() {
        return panelWidth - CONTENT_PAD * 2;
    }

    /** Title row: bold yellow title beside the tab-sized icon (icon drawn separately at tab center). */
    protected void renderTitleRow(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY) {
        int textX = bodyX + TAB_SIZE + 2;
        int textY = bodyY + Math.max(0, (TAB_SIZE - font.lineHeight) / 2);
        graphics.text(font, title.copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                textX, textY, TITLE_COLOR, true);
    }

    /** Label at content left; wraps within the panel content width. */
    protected void drawLabel(GuiGraphicsExtractor graphics, Font font, Component text, int bodyX, int y) {
        drawWrapped(graphics, font, text, bodyX + CONTENT_PAD, y, contentInnerWidth(), LABEL_COLOR, false, true);
    }

    /** Label at an absolute x, wrapping up to the content right edge (`bodyX + CONTENT_PAD + inner`). */
    protected void drawLabel(GuiGraphicsExtractor graphics, Font font, Component text, int bodyX, int x, int y) {
        int maxWidth = Math.max(8, bodyX + CONTENT_PAD + contentInnerWidth() - x);
        drawWrapped(graphics, font, text, x, y, maxWidth, LABEL_COLOR, false, true);
    }

    protected void drawValue(GuiGraphicsExtractor graphics, Font font, Component text, int bodyX, int y) {
        drawWrapped(graphics, font, text, bodyX + CONTENT_PAD + 10, y, Math.max(8, contentInnerWidth() - 10), VALUE_COLOR, false, false);
    }

    protected void drawValue(GuiGraphicsExtractor graphics, Font font, Component text, int bodyX, int x, int y) {
        int maxWidth = Math.max(8, bodyX + CONTENT_PAD + contentInnerWidth() - x);
        drawWrapped(graphics, font, text, x, y, maxWidth, VALUE_COLOR, false, false);
    }

    /** Returns vertical space used by the wrapped block. */
    protected int drawWrapped(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y,
                              int maxWidth, int color, boolean bold) {
        return drawWrapped(graphics, font, text, x, y, maxWidth, color, bold, false);
    }

    protected int drawWrapped(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y,
                              int maxWidth, int color, boolean bold, boolean shadow) {
        int cursorY = y;
        for (String line : wrapText(font, text.getString(), maxWidth, bold)) {
            Component lineComponent = bold
                    ? Component.literal(line).withStyle(ChatFormatting.BOLD)
                    : Component.literal(line);
            graphics.text(font, lineComponent, x, cursorY, color, shadow);
            cursorY += font.lineHeight + 1;
        }
        return cursorY - y;
    }

    protected static List<String> wrapText(Font font, String text, int maxWidth, boolean bold) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (measure(font, candidate, bold) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                if (measure(font, word, bold) <= maxWidth) {
                    current.setLength(0);
                    current.append(word);
                } else {
                    int start = 0;
                    while (start < word.length()) {
                        int end = start + 1;
                        while (end <= word.length() && measure(font, word.substring(start, end), bold) <= maxWidth) {
                            end++;
                        }
                        end = Math.max(start + 1, end - 1);
                        lines.add(word.substring(start, end));
                        start = end;
                    }
                    current.setLength(0);
                }
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static int measure(Font font, String text, boolean bold) {
        if (bold) {
            return font.width(Component.literal(text).withStyle(ChatFormatting.BOLD));
        }
        return font.width(text);
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

    /**
     * Optional mouse-wheel handling while this panel is open.
     * @return true if the scroll was consumed
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY,
                                 int leftPos, int topPos, int imageWidth, Font font) {
        return false;
    }

    /** Optional click handling (e.g. info scrollbar) before empty-body close. */
    public boolean mouseClicked(double mouseX, double mouseY) {
        return false;
    }

    /** Optional drag handling while a panel-owned interaction is active. */
    public boolean mouseDragged(double mouseX, double mouseY) {
        return false;
    }

    /** Optional release handling for panel-owned drag interactions. */
    public boolean mouseReleased() {
        return false;
    }
}
