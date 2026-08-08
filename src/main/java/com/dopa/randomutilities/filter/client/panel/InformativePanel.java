package com.dopa.randomutilities.filter.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.client.gui.PanelTextScrollbar;
import com.dopa.randomutilities.filter.config.DevNullConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class InformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int BODY_TEXT = 0xFFFFFFFF;
    private static final int DASH_COLOR = 0xFFFFFFFF;
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);

    private final boolean basic;
    private final PanelTextScrollbar scrollbar = new PanelTextScrollbar();

    public InformativePanel(boolean basic) {
        super(
                PanelAnchor.LEFT_TOP,
                122,
                78,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.info")
        );
        this.basic = basic;
    }

    @Override
    protected void onOpened() {
        scrollbar.reset();
    }

    @Override
    protected void onClosed() {
        scrollbar.reset();
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
        int scrollPixels = scrollbar.begin(contentHeight, viewHeight);

        int textY = viewTop - scrollPixels;
        for (ContentLine line : lines) {
            int lineBottom = textY + font.lineHeight;
            if (lineBottom > viewTop && textY < viewBottom) {
                drawLine(graphics, font, line, textX, textY);
            }
            textY += lineStep;
        }

        scrollbar.render(graphics, bodyX, panelWidth, viewTop, viewBottom);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        return scrollbar.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY) {
        return contentsInteractive() && scrollbar.mouseClicked(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY) {
        return scrollbar.mouseDragged(mouseY);
    }

    @Override
    public boolean mouseReleased() {
        return scrollbar.mouseReleased();
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
        int contentHeight = buildContent(font, maxWidth).size() * lineStep;
        scrollbar.begin(contentHeight, viewHeight);
        return scrollbar.mouseScrolled(scrollY * lineStep);
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
            lines.add(ContentLine.blankLine());
            appendWrapped(lines, font,
                    Component.translatable("gui.dopasrandomutilities.panel.info.basic.manual"),
                    maxWidth, BODY_TEXT);
            return lines;
        }

        appendWrapped(lines, font,
                Component.translatable("gui.dopasrandomutilities.panel.info.advanced.intro"),
                maxWidth, BODY_TEXT);
        lines.add(ContentLine.blankLine());
        appendWrapped(lines, font,
                Component.translatable("gui.dopasrandomutilities.panel.info.advanced.features"),
                maxWidth, BODY_TEXT);
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
                Component.translatable(labelKey),
                Component.literal(Integer.toString(value))
        );
    }

    private static void drawLine(GuiGraphicsExtractor graphics, Font font, ContentLine line, int x, int y) {
        if (line.isBlank()) {
            return;
        }
        if (line.label() != null) {
            graphics.text(font, Component.literal("-"), x, y, DASH_COLOR, false);
            int labelX = x + font.width("- ");
            graphics.text(font, line.label(), labelX, y, LABEL_COLOR, false);
            int valueX = labelX + font.width(line.label()) + 4;
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
