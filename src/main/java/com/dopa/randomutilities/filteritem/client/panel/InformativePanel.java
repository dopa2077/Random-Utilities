package com.dopa.randomutilities.filteritem.client.panel;

import com.dopa.randomutilities.config.DevNullConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class InformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int BODY_TEXT = 0xFFFFFFFF;
    private static final int SCROLLBAR_TRACK = 0x66000000;
    private static final int SCROLLBAR_THUMB = 0xFFC0C0C0;
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);

    private final boolean basic;
    private int scrollPixels;

    public InformativePanel(boolean basic) {
        super(
                PanelAnchor.LEFT_TOP,
                108,
                78,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.info")
        );
        this.basic = basic;
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
        int maxWidth = contentInnerWidth() - 4; // room for scrollbar
        int lineStep = font.lineHeight + 1;

        List<ContentLine> lines = buildContent(font, maxWidth);
        int contentHeight = lines.isEmpty() ? 0 : lines.size() * lineStep;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollPixels = Mth.clamp(scrollPixels, 0, maxScroll);

        int textY = viewTop - scrollPixels;
        for (ContentLine line : lines) {
            int lineBottom = textY + font.lineHeight;
            if (lineBottom > viewTop && textY < viewBottom) {
                drawLine(graphics, font, line, textX, textY);
            }
            textY += lineStep;
        }

        if (maxScroll > 0) {
            int trackX = bodyX + panelWidth - CONTENT_PAD + 1;
            graphics.fill(trackX, viewTop, trackX + 2, viewBottom, SCROLLBAR_TRACK);
            int thumbHeight = Math.max(6, Math.round(viewHeight * (viewHeight / (float) contentHeight)));
            int thumbTravel = viewHeight - thumbHeight;
            int thumbY = viewTop + (maxScroll == 0 ? 0 : Math.round(thumbTravel * (scrollPixels / (float) maxScroll)));
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, SCROLLBAR_THUMB);
        }
    }

    /**
     * @return true if the scroll was consumed
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY,
                                 int leftPos, int topPos, int imageWidth, Font font) {
        if (!contentsInteractive() || !isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            return false;
        }
        int viewHeight = Math.max(0, panelHeight - TITLE_ROW_HEIGHT - 2 - CONTENT_PAD);
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;
        int contentHeight = buildContent(font, maxWidth).size() * lineStep;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        if (maxScroll <= 0) {
            return false;
        }
        scrollPixels = Mth.clamp(scrollPixels - (int) Math.round(scrollY * lineStep), 0, maxScroll);
        return true;
    }

    private List<ContentLine> buildContent(Font font, int maxWidth) {
        List<ContentLine> lines = new ArrayList<>();
        if (basic) {
            Component paragraph = Component.translatable(
                    "gui.dopasrandomutilities.panel.info.basic",
                    Component.literal("BIG").withStyle(ChatFormatting.BOLD),
                    Component.literal(Integer.toString(DevNullConfig.basicMaxStackSize()))
                            .withStyle(ChatFormatting.BLACK)
            );
            appendWrapped(lines, font, paragraph, maxWidth, BODY_TEXT);
            return lines;
        }

        Component intro = Component.translatable(
                "gui.dopasrandomutilities.panel.info.advanced.intro",
                Component.literal("/dev/null").withStyle(ChatFormatting.BOLD)
        );
        appendWrapped(lines, font, intro, maxWidth, BODY_TEXT);
        lines.add(ContentLine.blankLine());
        appendWrapped(lines, font,
                Component.translatable("gui.dopasrandomutilities.panel.info.advanced.limits_header"),
                maxWidth, BODY_TEXT);
        lines.add(limitLine("gui.dopasrandomutilities.panel.info.advanced.stack_limit",
                DevNullConfig.advancedMaxStackSize()));
        lines.add(limitLine("gui.dopasrandomutilities.panel.info.advanced.slots",
                DevNullConfig.advancedMaxSlots()));
        lines.add(limitLine("gui.dopasrandomutilities.panel.info.advanced.pages",
                DevNullConfig.advancedMaxPages()));
        return lines;
    }

    private static void appendWrapped(List<ContentLine> lines, Font font, Component text, int maxWidth, int color) {
        for (FormattedCharSequence seq : font.split(text, maxWidth)) {
            lines.add(ContentLine.wrapped(seq, color));
        }
    }

    private static ContentLine limitLine(String labelKey, int value) {
        return ContentLine.limit(
                Component.literal("- ").append(Component.translatable(labelKey)),
                Component.literal(Integer.toString(value))
        );
    }

    private static void drawLine(GuiGraphicsExtractor graphics, Font font, ContentLine line, int x, int y) {
        if (line.isBlank()) {
            return;
        }
        if (line.label() != null) {
            graphics.text(font, line.label(), x, y, LABEL_COLOR, false);
            int valueX = x + font.width(line.label()) + 4;
            graphics.text(font, line.value(), valueX, y, VALUE_COLOR, false);
            return;
        }
        graphics.text(font, line.text(), x, y, line.color(), false);
    }

    private record ContentLine(
            FormattedCharSequence text,
            int color,
            Component label,
            Component value,
            boolean isBlank
    ) {
        static ContentLine wrapped(FormattedCharSequence text, int color) {
            return new ContentLine(text, color, null, null, false);
        }

        static ContentLine blankLine() {
            return new ContentLine(null, 0, null, null, true);
        }

        static ContentLine limit(Component label, Component value) {
            return new ContentLine(null, 0, label, value, false);
        }
    }
}
