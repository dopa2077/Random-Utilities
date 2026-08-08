package com.dopa.randomutilities.itemcollector.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.client.gui.PanelTextScrollbar;
import com.dopa.randomutilities.itemcollector.ItemCollectorType;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ItemCollectorInformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int BODY_TEXT = 0xFFFFFFFF;
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);

    private final ItemCollectorType type;
    private final PanelTextScrollbar scrollbar = new PanelTextScrollbar();

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
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;

        List<FormattedCharSequence> lines = font.split(infoText(), maxWidth);
        int contentHeight = lines.size() * lineStep;
        int scrollPixels = scrollbar.begin(contentHeight, viewHeight);

        int textY = viewTop - scrollPixels;
        for (FormattedCharSequence line : lines) {
            if (textY + font.lineHeight > viewTop && textY < viewBottom) {
                graphics.text(font, line, textX, textY, BODY_TEXT, false);
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
        if (!isOpen() || scrollY == 0.0D) {
            return false;
        }
        if (!isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            return false;
        }
        int viewHeight = Math.max(0, panelHeight - TITLE_ROW_HEIGHT - 2 - CONTENT_PAD);
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;
        int contentHeight = font.split(infoText(), maxWidth).size() * lineStep;
        scrollbar.begin(contentHeight, viewHeight);
        return scrollbar.mouseScrolled(scrollY * lineStep * 2);
    }

    private Component infoText() {
        return type == ItemCollectorType.BASIC
                ? Component.translatable("gui.dopasrandomutilities.panel.info.item_collector.basic")
                : Component.translatable("gui.dopasrandomutilities.panel.info.item_collector.advanced");
    }
}
