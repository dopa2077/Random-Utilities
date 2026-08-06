package com.dopa.randomutilities.itemcollector.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.itemcollector.ItemCollectorType;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ItemCollectorInformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int BODY_TEXT = 0xFFFFFFFF;
    private static final int SCROLLBAR_TRACK = 0x66000000;
    private static final int SCROLLBAR_THUMB = 0xFFC0C0C0;
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);

    private final ItemCollectorType type;
    private int scrollPixels;

    public ItemCollectorInformativePanel(ItemCollectorType type) {
        super(
                PanelAnchor.LEFT_TOP,
                122,
                type == ItemCollectorType.ADVANCED ? 90 : 78,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.info")
        );
        this.type = type;
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

        Component textKey = type == ItemCollectorType.BASIC
                ? Component.translatable("gui.dopasrandomutilities.panel.info.item_collector.basic")
                : Component.translatable("gui.dopasrandomutilities.panel.info.item_collector.advanced");
        List<FormattedCharSequence> lines = font.split(textKey, maxWidth);
        int contentHeight = lines.size() * lineStep;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        scrollPixels = Mth.clamp(scrollPixels, 0, maxScroll);

        int textY = viewTop - scrollPixels;
        for (FormattedCharSequence line : lines) {
            if (textY + font.lineHeight > viewTop && textY < viewBottom) {
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
        if (!isOpen() || scrollY == 0.0D) {
            return false;
        }
        int bodyX = bodyXOpen(leftPos, imageWidth);
        int bodyY = bodyY(topPos);
        if (!isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            return false;
        }
        int viewTop = bodyY + TITLE_ROW_HEIGHT + 2;
        int viewBottom = bodyY + panelHeight - CONTENT_PAD;
        int viewHeight = Math.max(0, viewBottom - viewTop);
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;
        Component textKey = type == ItemCollectorType.BASIC
                ? Component.translatable("gui.dopasrandomutilities.panel.info.item_collector.basic")
                : Component.translatable("gui.dopasrandomutilities.panel.info.item_collector.advanced");
        int contentHeight = font.split(textKey, maxWidth).size() * lineStep;
        int maxScroll = Math.max(0, contentHeight - viewHeight);
        if (maxScroll <= 0) {
            return false;
        }
        scrollPixels = Mth.clamp(scrollPixels - (int) (scrollY * lineStep * 2), 0, maxScroll);
        return true;
    }
}
