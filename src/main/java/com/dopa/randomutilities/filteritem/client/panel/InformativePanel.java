package com.dopa.randomutilities.filteritem.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class InformativePanel extends AttachedPanel {
    private static final int BG = 0xFF3A3A3A;
    private static final int TEXT = 0xFFD0D0D0;
    private static final int ICON = 0xFFE8E8E8;

    private final Component description;

    public InformativePanel(boolean basic) {
        super(
                PanelAnchor.LEFT_TOP,
                108,
                78,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.info")
        );
        this.description = Component.translatable(basic
                ? "gui.dopasrandomutilities.panel.info.basic"
                : "gui.dopasrandomutilities.panel.info.advanced");
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        int r = 7;
        graphics.fill(centerX - r, centerY - r, centerX + r + 1, centerY + r + 1, 0xFF2A2A2A);
        graphics.fill(centerX - r + 1, centerY - r + 1, centerX + r, centerY + r, 0xFF555555);
        String glyph = "i";
        graphics.text(font, Component.literal(glyph),
                centerX - font.width(glyph) / 2,
                centerY - font.lineHeight / 2,
                ICON, false);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        graphics.text(font, title, bodyX + CONTENT_PAD, bodyY + CONTENT_PAD, 0xFFE0E0E0, false);
        int textX = bodyX + CONTENT_PAD;
        int textY = bodyY + CONTENT_PAD + 14;
        int maxWidth = panelWidth - CONTENT_PAD * 2;
        for (String line : wrap(font, description.getString(), maxWidth)) {
            graphics.text(font, Component.literal(line), textX, textY, TEXT, false);
            textY += font.lineHeight + 1;
            if (textY > bodyY + panelHeight - CONTENT_PAD - font.lineHeight) {
                break;
            }
        }
    }

    private static List<String> wrap(Font font, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (font.width(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
            } else {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                }
                current.setLength(0);
                current.append(word);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }
}
