package com.dopa.randomutilities.generator.client.panel;

import com.dopa.randomutilities.gui.panel.AttachedPanel;
import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.widget.PanelTextScrollbar;
import com.dopa.randomutilities.generator.config.GeneratorType;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class GeneratorInformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int BODY_TEXT = 0xFFFFFFFF;
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);

    private static final String[] RECIPE_PARAGRAPH_KEYS = {
            "gui.dopasrandomutilities.panel.info.generator.visual",
            "gui.dopasrandomutilities.panel.info.generator.howto",
            "gui.dopasrandomutilities.panel.info.generator.lock"
    };
    private static final String[] RANDOM_ORE_PARAGRAPH_KEYS = {
            "gui.dopasrandomutilities.panel.info.generator.visual",
            "gui.dopasrandomutilities.panel.info.generator.random_ore.howto"
    };
    private static final String[] METAL_BLOCK_PARAGRAPH_KEYS = {
            "gui.dopasrandomutilities.panel.info.generator.visual",
            "gui.dopasrandomutilities.panel.info.generator.metal_block.howto"
    };

    private final int tabYBias;
    private final GeneratorType.Mode mode;
    private final PanelTextScrollbar scrollbar = new PanelTextScrollbar();

    public GeneratorInformativePanel(int tabYBias, GeneratorType.Mode mode) {
        super(PanelAnchor.LEFT_TOP, 122, 90, BG, Component.translatable("gui.dopasrandomutilities.panel.info"));
        this.tabYBias = tabYBias;
        this.mode = mode;
    }

    @Override
    public int tabOffsetY() {
        return super.tabOffsetY() + tabYBias;
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
        List<FormattedCharSequence> lines = buildLines(font, maxWidth);
        int contentHeight = lines.size() * lineStep;
        int scrollPixels = scrollbar.begin(contentHeight, viewHeight);
        int textY = viewTop - scrollPixels;
        for (FormattedCharSequence line : lines) {
            if (line != null && textY + font.lineHeight > viewTop && textY < viewBottom) {
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
        if (!contentsInteractive() || !isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            return false;
        }
        int viewHeight = Math.max(0, panelHeight - TITLE_ROW_HEIGHT - 2 - CONTENT_PAD);
        int maxWidth = contentInnerWidth() - 4;
        int lineStep = font.lineHeight + 1;
        int contentHeight = buildLines(font, maxWidth).size() * lineStep;
        scrollbar.begin(contentHeight, viewHeight);
        return scrollbar.mouseScrolled(scrollY * lineStep);
    }

    private String[] paragraphKeys() {
        return switch (mode) {
            case RANDOM_ORE -> RANDOM_ORE_PARAGRAPH_KEYS;
            case METAL_BLOCK -> METAL_BLOCK_PARAGRAPH_KEYS;
            case RECIPE -> RECIPE_PARAGRAPH_KEYS;
        };
    }

    /** Wrapped paragraphs with a blank line ({@code null} entry) between each. */
    private List<FormattedCharSequence> buildLines(Font font, int maxWidth) {
        String[] keys = paragraphKeys();
        List<FormattedCharSequence> lines = new ArrayList<>();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                lines.add(null);
            }
            lines.addAll(font.split(Component.translatable(keys[i]), maxWidth));
        }
        return lines;
    }
}
