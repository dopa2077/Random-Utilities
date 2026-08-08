package com.dopa.randomutilities.machine.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.network.MachineSettingPayload;

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
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Synced Ignore / Low / High redstone panel for world machines. */
public final class MachineRedstonePanel extends AttachedPanel {
    private static final int BG = 0xFF962520;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 20;
    private static final int BUTTON_GAP = 4;
    private static final int TRAY_PAD = 5;
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

    public interface Host {
        <T extends net.minecraft.client.gui.components.events.GuiEventListener
                & net.minecraft.client.gui.components.Renderable
                & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget);

        Supplier<RedstoneMode> redstoneMode();
    }

    private final Host host;
    private final Consumer<RedstoneMode> modeSender;
    private final int tabYBias;
    private TextureIconButton ignoreButton;
    private TextureIconButton lowButton;
    private TextureIconButton highButton;
    private boolean widgetsCreated;

    public MachineRedstonePanel(Host host, PanelAnchor anchor, int tabYBias) {
        this(host, anchor, tabYBias, mode -> ClientPacketDistributor.sendToServer(MachineSettingPayload.redstone(mode)));
    }

    public MachineRedstonePanel(Host host, PanelAnchor anchor, int tabYBias, Consumer<RedstoneMode> modeSender) {
        super(anchor, 136, 108, BG, Component.translatable("gui.dopasrandomutilities.panel.redstone"));
        this.host = host;
        this.modeSender = modeSender;
        this.tabYBias = tabYBias;
    }

    @Override
    public int tabOffsetY() {
        return super.tabOffsetY() + tabYBias;
    }

    public void initWidgets() {
        if (widgetsCreated) {
            return;
        }
        widgetsCreated = true;
        ignoreButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.ignore"),
                TEX_IGNORE, RedstoneMode.IGNORE);
        lowButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.low"),
                TEX_LOW, RedstoneMode.LOW);
        highButton = levelButton(
                Component.translatable("gui.dopasrandomutilities.panel.redstone.high"),
                TEX_HIGH, RedstoneMode.HIGH);
        host.addOverlayWidget(ignoreButton);
        host.addOverlayWidget(lowButton);
        host.addOverlayWidget(highButton);
        refreshSelection();
        updateWidgetVisibility(false);
    }

    private TextureIconButton levelButton(Component label, Identifier texture, RedstoneMode target) {
        return new TextureIconButton(0, 0, BUTTON_W, BUTTON_H, texture, label, () -> setMode(target));
    }

    private void setMode(RedstoneMode next) {
        modeSender.accept(next);
        refreshSelection(next);
    }

    private void refreshSelection() {
        refreshSelection(host.redstoneMode().get());
    }

    private void refreshSelection(RedstoneMode mode) {
        if (!widgetsCreated) {
            return;
        }
        ignoreButton.setSelected(mode == RedstoneMode.IGNORE);
        lowButton.setSelected(mode == RedstoneMode.LOW);
        highButton.setSelected(mode == RedstoneMode.HIGH);
    }

    private static int selectorGroupWidth() {
        return BUTTON_W * 3 + BUTTON_GAP * 2;
    }

    private TrayBounds selectorTrayBounds(int bodyX, int bodyY) {
        return trayBounds(bodyX, panelWidth, selectorGroupWidth(), bodyY + SELECTOR_Y, BUTTON_H, TRAY_PAD);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive()) {
            return false;
        }
        TrayBounds tray = selectorTrayBounds(bodyXOpen(leftPos, imageWidth), bodyY(topPos));
        return isMouseOverRect(mouseX, mouseY, tray.x(), tray.y(), tray.width(), tray.height());
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
        lowButton.setX(x + BUTTON_W + BUTTON_GAP);
        lowButton.setY(y);
        highButton.setX(x + (BUTTON_W + BUTTON_GAP) * 2);
        highButton.setY(y);
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
        refreshSelection();
        renderTray(graphics, selectorTrayBounds(bodyX, bodyY), BG);
        RedstoneMode mode = host.redstoneMode().get();
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.redstone.control_status"),
                bodyX, bodyY + STATUS_Y);
        drawValue(graphics, font, Component.translatable(mode == RedstoneMode.IGNORE
                        ? "gui.dopasrandomutilities.panel.redstone.disabled"
                        : "gui.dopasrandomutilities.panel.redstone.enabled"),
                bodyX, bodyY + STATUS_Y + 10);
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.redstone.signal_required"),
                bodyX, bodyY + SIGNAL_Y);
        drawValue(graphics, font, Component.translatable(switch (mode) {
            case IGNORE -> "gui.dopasrandomutilities.panel.redstone.ignored";
            case LOW -> "gui.dopasrandomutilities.panel.redstone.low";
            case HIGH -> "gui.dopasrandomutilities.panel.redstone.high";
        }), bodyX, bodyY + SIGNAL_Y + 10);
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
                    x, y, this.width, this.height, ARGB.white(this.alpha));
            int iconX = x + (this.width - ICON_TEX) / 2;
            int iconY = y + (this.height - ICON_TEX) / 2;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, iconX, iconY, 0.0F, 0.0F,
                    ICON_TEX, ICON_TEX, ICON_TEX, ICON_TEX);
        }
    }
}
