package com.dopa.randomutilities.machine.generator.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorInformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int BODY_TEXT = 0xFFFFFFFF;
    private static final int SCROLLBAR_TRACK = 0x66000000;
    private static final int SCROLLBAR_THUMB = 0xFFC0C0C0;
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);

    private static final String[] PARAGRAPH_KEYS = {
            "gui.dopasrandomutilities.panel.info.generator.visual",
            "gui.dopasrandomutilities.panel.info.generator.howto",
            "gui.dopasrandomutilities.panel.info.generator.lock"
    };

    private final int tabYBias;
    private int scrollPixels;

    public GeneratorInformativePanel(int tabYBias) {
        super(PanelAnchor.LEFT_TOP, 122, 90, BG, Component.translatable("gui.dopasrandomutilities.panel.info"));
        this.tabYBias = tabYBias;
    }

    @Override
    public int tabOffsetY() {
        return super.tabOffsetY() + tabYBias;
    }

    @Override
    protected void onOpened() {
        scrollPixels = 0;
    }

    @Override
    protected void onClosed() {
        scrollPixels = 0;
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(BOOK_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        int textX = bodyX + CONTENT_PAD;
        int viewTop = bodyY + TITLE_ROW_HEIGHT + 2;
        int viewBottom = bodyY + panelHeight - CONTENT_PAD;
        int viewHeight = Math.max(0, viewBottom - viewTop);
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;
        List<FormattedCharSequence> lines = buildLines(font, maxWidth);
        int contentHeight = lines.size() * lineStep;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollPixels = Mth.clamp(scrollPixels, 0, maxScroll);
        int textY = viewTop - scrollPixels;
        for (FormattedCharSequence line : lines) {
            if (line != null && textY + font.lineHeight > viewTop && textY < viewBottom) {
                graphics.text(font, line, textX, textY, BODY_TEXT, false);
            }
            textY += lineStep;
        }
        if (maxScroll > 0) {
            int trackX = bodyX + panelWidth - CONTENT_PAD + 1;
            graphics.fill(trackX, viewTop, trackX + 2, viewBottom, SCROLLBAR_TRACK);
            int thumbHeight = Math.max(6, Math.round(viewHeight * (viewHeight / (float) contentHeight)));
            int thumbTravel = viewHeight - thumbHeight;
            int thumbY = viewTop + Math.round(thumbTravel * (scrollPixels / (float) maxScroll));
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, SCROLLBAR_THUMB);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY,
                                 int leftPos, int topPos, int imageWidth, Font font) {
        if (!contentsInteractive() || !isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            return false;
        }
        int viewHeight = Math.max(0, panelHeight - TITLE_ROW_HEIGHT - 2 - CONTENT_PAD);
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;
        int contentHeight = buildLines(font, maxWidth).size() * lineStep;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        if (maxScroll <= 0) {
            return false;
        }
        scrollPixels = Mth.clamp(scrollPixels - (int) Math.round(scrollY * lineStep), 0, maxScroll);
        return true;
    }

    /** Wrapped paragraphs with a blank line ({@code null} entry) between each. */
    private static List<FormattedCharSequence> buildLines(Font font, int maxWidth) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 0; i < PARAGRAPH_KEYS.length; i++) {
            if (i > 0) {
                lines.add(null);
            }
            lines.addAll(font.split(Component.translatable(PARAGRAPH_KEYS[i]), maxWidth));
        }
        return lines;
    }
}
