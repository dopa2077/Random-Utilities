package com.dopa.randomutilities.filteritem.client.panel;

import com.dopa.randomutilities.filteritem.client.FilterScreen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/**
 * Client-only redstone control preview for visual testing on Advanced /dev/null.
 */
public final class RedstonePanel extends AttachedPanel {
    private static final int BG = 0xFFB02E26;
    private static final int LABEL = 0xFFE8E0E0;
    private static final int VALUE = 0xFFFFF0C0;
    private static final int BUTTON_H = 18;
    private static final int SELECTOR_Y = 22;
    private static final int STATUS_Y = 52;
    private static final int SIGNAL_Y = 74;

    public enum RedstoneLevel {
        IGNORE,
        LOW,
        HIGH
    }

    private final FilterScreen screen;
    private RedstoneLevel level = RedstoneLevel.IGNORE;
    private Button ignoreButton;
    private Button lowButton;
    private Button highButton;
    private boolean widgetsCreated;

    public RedstonePanel(FilterScreen screen) {
        super(
                PanelAnchor.RIGHT_LOW,
                106,
                100,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.redstone")
        );
        this.screen = screen;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;

        ignoreButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.ignore"),
                RedstoneLevel.IGNORE);
        lowButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.low"),
                RedstoneLevel.LOW);
        highButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.high"),
                RedstoneLevel.HIGH);

        screen.addOverlayWidget(ignoreButton);
        screen.addOverlayWidget(lowButton);
        screen.addOverlayWidget(highButton);
        updateWidgetVisibility(false);
        refreshButtonStyles();
    }

    private Button levelButton(Component label, RedstoneLevel target) {
        return Button.builder(label, b -> setLevel(target))
                .bounds(0, 0, 28, BUTTON_H)
                .tooltip(Tooltip.create(label))
                .build();
    }

    private void setLevel(RedstoneLevel next) {
        this.level = next;
        refreshButtonStyles();
    }

    private void refreshButtonStyles() {
        if (!widgetsCreated) {
            return;
        }
        // Active button stays enabled-looking; others slightly muted via active flag only for highlight cue
        ignoreButton.active = contentsInteractive();
        lowButton.active = contentsInteractive();
        highButton.active = contentsInteractive();
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int inner = panelWidth - CONTENT_PAD * 2;
        int gap = 4;
        int btnW = (inner - gap * 2) / 3;
        int y = by + SELECTOR_Y;

        ignoreButton.setX(bx + CONTENT_PAD);
        ignoreButton.setY(y);
        ignoreButton.setWidth(btnW);

        lowButton.setX(bx + CONTENT_PAD + btnW + gap);
        lowButton.setY(y);
        lowButton.setWidth(btnW);

        highButton.setX(bx + CONTENT_PAD + (btnW + gap) * 2);
        highButton.setY(y);
        highButton.setWidth(btnW);
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        ignoreButton.visible = interactive;
        lowButton.visible = interactive;
        highButton.visible = interactive;
        ignoreButton.active = interactive;
        lowButton.active = interactive;
        highButton.active = interactive;
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        // Compact redstone dust glyph (item render may not be available on GuiGraphicsExtractor)
        int dust = 0xFFE02020;
        int dark = 0xFF801010;
        graphics.fill(centerX - 1, centerY - 5, centerX + 2, centerY + 5, dust);
        graphics.fill(centerX - 4, centerY - 1, centerX + 5, centerY + 2, dust);
        graphics.fill(centerX - 3, centerY - 3, centerX - 1, centerY - 1, dark);
        graphics.fill(centerX + 2, centerY + 2, centerX + 4, centerY + 4, dark);
        graphics.fill(centerX + 3, centerY - 4, centerX + 5, centerY - 2, dust);
        graphics.fill(centerX - 5, centerY + 3, centerX - 3, centerY + 5, dust);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        graphics.text(font, title, bodyX + CONTENT_PAD, bodyY + CONTENT_PAD, 0xFFFFE080, false);

        // Highlight frame around the selected level button
        if (widgetsCreated && contentsInteractive()) {
            Button selected = switch (level) {
                case IGNORE -> ignoreButton;
                case LOW -> lowButton;
                case HIGH -> highButton;
            };
            int hx = selected.getX() - 1;
            int hy = selected.getY() - 1;
            int hw = selected.getWidth() + 2;
            int hh = selected.getHeight() + 2;
            int glow = 0xFFA8C8FF;
            graphics.fill(hx, hy, hx + hw, hy + 1, glow);
            graphics.fill(hx, hy + hh - 1, hx + hw, hy + hh, glow);
            graphics.fill(hx, hy, hx + 1, hy + hh, glow);
            graphics.fill(hx + hw - 1, hy, hx + hw, hy + hh, glow);
        }

        graphics.text(font, Component.translatable("gui.dopasrandomutilities.panel.redstone.control_status"),
                bodyX + CONTENT_PAD, bodyY + STATUS_Y, LABEL, false);
        graphics.text(font, controlStatusValue(),
                bodyX + CONTENT_PAD, bodyY + STATUS_Y + 10, VALUE, false);

        graphics.text(font, Component.translatable("gui.dopasrandomutilities.panel.redstone.signal_required"),
                bodyX + CONTENT_PAD, bodyY + SIGNAL_Y, LABEL, false);
        graphics.text(font, signalRequiredValue(),
                bodyX + CONTENT_PAD, bodyY + SIGNAL_Y + 10, VALUE, false);
    }

    private Component controlStatusValue() {
        return Component.translatable(level == RedstoneLevel.IGNORE
                ? "gui.dopasrandomutilities.panel.redstone.disabled"
                : "gui.dopasrandomutilities.panel.redstone.enabled");
    }

    private Component signalRequiredValue() {
        return switch (level) {
            case IGNORE -> Component.translatable("gui.dopasrandomutilities.panel.redstone.ignored");
            case LOW -> Component.translatable("gui.dopasrandomutilities.panel.redstone.low");
            case HIGH -> Component.translatable("gui.dopasrandomutilities.panel.redstone.high");
        };
    }
}
