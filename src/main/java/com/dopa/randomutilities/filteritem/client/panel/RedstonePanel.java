package com.dopa.randomutilities.filteritem.client.panel;

import com.dopa.randomutilities.filteritem.client.FilterScreen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client-only redstone control preview for visual testing on Advanced /dev/null.
 */
public final class RedstonePanel extends AttachedPanel {
    private static final int BG = 0xFFB02E26;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 20;
    private static final int BUTTON_GAP = 4;
    private static final int TRAY_PAD = 3;
    private static final int SELECTOR_Y = 28;
    private static final int STATUS_Y = 58;
    private static final int SIGNAL_Y = 80;
    private static final int ICON_TEX = 16;

    private static final Identifier TEX_IGNORE =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/redstone_off.png");
    private static final Identifier TEX_LOW =
            Identifier.withDefaultNamespace("textures/block/redstone_torch_off.png");
    private static final Identifier TEX_HIGH =
            Identifier.withDefaultNamespace("textures/block/redstone_torch.png");

    private static final ItemStack REDSTONE_ICON = new ItemStack(Items.REDSTONE);

    private static final WidgetSprites BUTTON_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"),
            Identifier.withDefaultNamespace("widget/button_disabled"),
            Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    public enum RedstoneLevel {
        IGNORE,
        LOW,
        HIGH
    }

    private final FilterScreen screen;
    private RedstoneLevel level = RedstoneLevel.IGNORE;
    private TextureIconButton ignoreButton;
    private TextureIconButton lowButton;
    private TextureIconButton highButton;
    private boolean widgetsCreated;

    public RedstonePanel(FilterScreen screen) {
        super(
                PanelAnchor.RIGHT_LOW,
                136,
                108,
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
                TEX_IGNORE,
                RedstoneLevel.IGNORE);
        lowButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.low"),
                TEX_LOW,
                RedstoneLevel.LOW);
        highButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.high"),
                TEX_HIGH,
                RedstoneLevel.HIGH);

        screen.addOverlayWidget(ignoreButton);
        screen.addOverlayWidget(lowButton);
        screen.addOverlayWidget(highButton);
        refreshSelection();
        updateWidgetVisibility(false);
    }

    private TextureIconButton levelButton(Component label, Identifier texture, RedstoneLevel target) {
        return new TextureIconButton(0, 0, BUTTON_W, BUTTON_H, texture, label, () -> setLevel(target));
    }

    private void setLevel(RedstoneLevel next) {
        this.level = next;
        refreshSelection();
    }

    private void refreshSelection() {
        if (!widgetsCreated) {
            return;
        }
        ignoreButton.setSelected(level == RedstoneLevel.IGNORE);
        lowButton.setSelected(level == RedstoneLevel.LOW);
        highButton.setSelected(level == RedstoneLevel.HIGH);
    }

    private static int selectorGroupWidth() {
        return BUTTON_W * 3 + BUTTON_GAP * 2;
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int groupW = selectorGroupWidth();
        int x = bx + (panelWidth - groupW) / 2;
        int y = by + SELECTOR_Y;

        ignoreButton.setX(x);
        ignoreButton.setY(y);
        ignoreButton.setWidth(BUTTON_W);

        lowButton.setX(x + BUTTON_W + BUTTON_GAP);
        lowButton.setY(y);
        lowButton.setWidth(BUTTON_W);

        highButton.setX(x + (BUTTON_W + BUTTON_GAP) * 2);
        highButton.setY(y);
        highButton.setWidth(BUTTON_W);
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
        graphics.item(REDSTONE_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);

        int groupW = selectorGroupWidth();
        int trayX = bodyX + (panelWidth - groupW) / 2 - TRAY_PAD;
        int trayY = bodyY + SELECTOR_Y - TRAY_PAD;
        int trayW = groupW + TRAY_PAD * 2;
        int trayH = BUTTON_H + TRAY_PAD * 2;
        graphics.fill(trayX, trayY, trayX + trayW, trayY + trayH, darken(BG, 40));

        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.redstone.control_status"),
                bodyX, bodyY + STATUS_Y);
        drawValue(graphics, font, controlStatusValue(),
                bodyX, bodyY + STATUS_Y + 10);

        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.redstone.signal_required"),
                bodyX, bodyY + SIGNAL_Y);
        drawValue(graphics, font, signalRequiredValue(),
                bodyX, bodyY + SIGNAL_Y + 10);
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

    private static final class TextureIconButton extends AbstractWidget {
        private final Identifier texture;
        private final Runnable onPress;
        private boolean selected;

        TextureIconButton(int x, int y, int width, int height, Identifier texture, Component tooltip, Runnable onPress) {
            super(x, y, width, height, Component.empty());
            this.texture = texture;
            this.onPress = onPress;
            this.setTooltip(Tooltip.create(tooltip));
        }

        void setSelected(boolean selected) {
            this.selected = selected;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (this.active) {
                this.onPress.run();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, this.getMessage());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUTTON_SPRITES.get(this.active, this.selected || this.isHoveredOrFocused()),
                    x,
                    y,
                    this.width,
                    this.height,
                    ARGB.white(this.alpha)
            );
            int iconX = x + (this.width - ICON_TEX) / 2;
            int iconY = y + (this.height - ICON_TEX) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F,
                    ICON_TEX, ICON_TEX, ICON_TEX, ICON_TEX);
        }
    }
}
