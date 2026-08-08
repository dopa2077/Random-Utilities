package com.dopa.randomutilities.redstoneclock.client;

import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.client.gui.PanelHost;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.machine.client.panel.MachineRedstonePanel;
import com.dopa.randomutilities.redstoneclock.RedstoneClockBlockEntity;
import com.dopa.randomutilities.redstoneclock.RedstoneClockMenu;
import com.dopa.randomutilities.redstoneclock.client.panel.RedstoneClockInformativePanel;
import com.dopa.randomutilities.redstoneclock.network.RedstoneClockSettingPayload;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

public class RedstoneClockScreen extends AbstractContainerScreen<RedstoneClockMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier CHEST_BACKGROUND =
            Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int TEXTURE_SIZE = 256;
    private static final int BODY_COLOR = 0xFFC6C6C6;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int FOOTER_Y = 70;
    private static final int PLAYER_INV_HEIGHT = 96;
    private static final int IMAGE_HEIGHT = FOOTER_Y + PLAYER_INV_HEIGHT + 1;

    private static final int STEPPER_W = 18;
    private static final int STEPPER_H = 12;
    private static final int VALUE_W = 28;
    private static final int STEPPER_RIGHT_INSET = 12;
    private static final int TRAY_PAD = 3;
    private static final int TRAY_COLOR = 0xFFA8A8A8;
    private static final int INTERVAL_Y = 28;
    private static final int PULSE_Y = 46;

    private final PanelHost panelHost = new PanelHost();
    private MachineRedstonePanel redstonePanel;
    private StepperButton intervalMinus;
    private StepperButton intervalPlus;
    private StepperButton pulseMinus;
    private StepperButton pulsePlus;

    public RedstoneClockScreen(RedstoneClockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, IMAGE_HEIGHT);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    public int leftPos() {
        return leftPos;
    }

    public int topPos() {
        return topPos;
    }

    public int imageWidth() {
        return imageWidth;
    }

    public PanelHost getPanelHost() {
        return panelHost;
    }

    @Override
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return menu::redstoneMode;
    }

    @Override
    protected void init() {
        super.init();
        panelHost.clear();
        redstonePanel = null;

        panelHost.add(new RedstoneClockInformativePanel());
        redstonePanel = new MachineRedstonePanel(
                this,
                PanelAnchor.RIGHT_TOP,
                0,
                mode -> ClientPacketDistributor.sendToServer(RedstoneClockSettingPayload.redstone(mode))
        );
        panelHost.add(redstonePanel);
        redstonePanel.initWidgets();

        intervalMinus = stepper("-", true, false);
        intervalPlus = stepper("+", true, true);
        pulseMinus = stepper("-", false, false);
        pulsePlus = stepper("+", false, true);
        addRenderableWidget(intervalMinus);
        addRenderableWidget(intervalPlus);
        addRenderableWidget(pulseMinus);
        addRenderableWidget(pulsePlus);
        layoutSteppers();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
    }

    private StepperButton stepper(String label, boolean interval, boolean increase) {
        Component tip = Component.translatable(interval
                        ? "gui.dopasrandomutilities.redstone_clock.interval.tooltip"
                        : "gui.dopasrandomutilities.redstone_clock.pulse.tooltip")
                .append("\n")
                .append(Component.translatable("gui.dopasrandomutilities.redstone_clock.step.shift")
                        .withStyle(ChatFormatting.GRAY))
                .append("\n")
                .append(Component.translatable("gui.dopasrandomutilities.redstone_clock.step.ctrl")
                        .withStyle(ChatFormatting.GRAY));
        IntConsumer adjust = interval ? this::adjustInterval : this::adjustPulse;
        return new StepperButton(label, tip, step -> adjust.accept(increase ? step : -step));
    }

    private void adjustInterval(int delta) {
        int current = menu.interval();
        int next = RedstoneClockBlockEntity.clampInterval((long) current + delta);
        if (next != current) {
            ClientPacketDistributor.sendToServer(RedstoneClockSettingPayload.interval(next));
        }
    }

    private void adjustPulse(int delta) {
        int current = menu.pulseLength();
        int max = menu.interval();
        int next = RedstoneClockBlockEntity.clampPulse((long) current + delta, max);
        if (next != current) {
            ClientPacketDistributor.sendToServer(RedstoneClockSettingPayload.pulse(next));
        }
    }

    private static int stepAmount(MouseButtonEvent event) {
        if (event.hasControlDown()) {
            return 100;
        }
        if (event.hasShiftDown()) {
            return 10;
        }
        return 1;
    }

    private void layoutSteppers() {
        int groupX = stepperGroupX();
        int valueX = groupX + STEPPER_W + 2;
        int plusX = valueX + VALUE_W + 2;
        intervalMinus.setRectangle(STEPPER_W, STEPPER_H, groupX, topPos + INTERVAL_Y);
        intervalPlus.setRectangle(STEPPER_W, STEPPER_H, plusX, topPos + INTERVAL_Y);
        pulseMinus.setRectangle(STEPPER_W, STEPPER_H, groupX, topPos + PULSE_Y);
        pulsePlus.setRectangle(STEPPER_W, STEPPER_H, plusX, topPos + PULSE_Y);
    }

    private int stepperGroupWidth() {
        return STEPPER_W + 2 + VALUE_W + 2 + STEPPER_W;
    }

    private int stepperGroupX() {
        return leftPos + imageWidth - STEPPER_RIGHT_INSET - stepperGroupWidth();
    }

    private void renderStepperTray(GuiGraphicsExtractor graphics) {
        int groupX = stepperGroupX();
        int trayX = groupX - TRAY_PAD;
        int trayY = topPos + INTERVAL_Y - TRAY_PAD;
        int trayW = stepperGroupWidth() + TRAY_PAD * 2;
        int trayH = (PULSE_Y - INTERVAL_Y) + STEPPER_H + TRAY_PAD * 2;
        graphics.fill(trayX, trayY, trayX + trayW, trayY + trayH, TRAY_COLOR);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        panelHost.tick();
        panelHost.layoutWidgets(leftPos, topPos, imageWidth);
        layoutSteppers();
        updateStepperStates();
    }

    private void updateStepperStates() {
        int interval = menu.interval();
        int pulse = menu.pulseLength();
        intervalMinus.active = interval > RedstoneClockBlockEntity.MIN_INTERVAL;
        intervalPlus.active = interval < RedstoneClockBlockEntity.MAX_INTERVAL;
        pulseMinus.active = pulse > RedstoneClockBlockEntity.MIN_INTERVAL;
        pulsePlus.active = pulse < interval;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        panelHost.tick();
        panelHost.render(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);

        int xo = leftPos;
        int yo = topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo, 0.0F, 0.0F,
                imageWidth, FOOTER_Y, TEXTURE_SIZE, TEXTURE_SIZE);
        graphics.fill(xo + 7, yo + 17, xo + imageWidth - 7, yo + FOOTER_Y, BODY_COLOR);
        renderStepperTray(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST_BACKGROUND, xo, yo + FOOTER_Y,
                0.0F, 126.0F, imageWidth, PLAYER_INV_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);
        graphics.text(font, Component.translatable("gui.dopasrandomutilities.redstone_clock.interval"),
                leftPos + 12, topPos + INTERVAL_Y + 2, LABEL_COLOR, false);
        graphics.text(font, Component.translatable("gui.dopasrandomutilities.redstone_clock.pulse"),
                leftPos + 12, topPos + PULSE_Y + 2, LABEL_COLOR, false);
        drawValue(graphics, menu.interval(), INTERVAL_Y);
        drawValue(graphics, menu.pulseLength(), PULSE_Y);

        Component tabTooltip = panelHost.hoveredTabTooltip(mouseX, mouseY, leftPos, topPos, imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(font, tabTooltip, mouseX, mouseY);
        }
    }

    private void drawValue(GuiGraphicsExtractor graphics, int value, int rowY) {
        int valueX = stepperGroupX() + STEPPER_W + 2;
        String text = Integer.toString(value);
        int textX = valueX + (VALUE_W - font.width(text)) / 2;
        graphics.text(font, text, textX, topPos + rowY + 2, 0xFF000000, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overTab = false;
        for (var panel : panelHost.panels()) {
            if (panel.isMouseOverTab(event.x(), event.y(), leftPos, topPos, imageWidth)) {
                overTab = true;
                break;
            }
        }
        var occupying = panelHost.openPanel();
        boolean overBody = occupying != null
                && occupying.isMouseOverBody(event.x(), event.y(), leftPos, topPos, imageWidth);
        // Open body covers sibling tabs at the attachment edge — handle body/scrollbar first.
        if (overBody) {
            for (int i = children().size() - 1; i >= 0; i--) {
                GuiEventListener child = children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    setFocused(child);
                    return true;
                }
            }
            if (panelHost.mouseClicked(event.x(), event.y())) {
                return true;
            }
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        if (overTab) {
            return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
        }
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        return panelHost.handleTabClick(event.x(), event.y(), leftPos, topPos, imageWidth);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean panelHandled = panelHost.mouseReleased();
        boolean handled = super.mouseReleased(event);
        return panelHandled || handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelHost.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, font)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static final class StepperButton extends AbstractWidget {
        private static final WidgetSprites SPRITES = new WidgetSprites(
                Identifier.withDefaultNamespace("widget/button"),
                Identifier.withDefaultNamespace("widget/button_disabled"),
                Identifier.withDefaultNamespace("widget/button_highlighted")
        );

        private final String label;
        private final IntConsumer onStep;

        StepperButton(String label, Component tooltip, IntConsumer onStep) {
            super(0, 0, STEPPER_W, STEPPER_H, Component.literal(label));
            this.label = label;
            this.onStep = onStep;
            setTooltip(Tooltip.create(tooltip));
        }

        @Override
        protected boolean isValidClickButton(MouseButtonInfo buttonInfo) {
            return buttonInfo.button() == InputConstants.MOUSE_BUTTON_LEFT
                    || buttonInfo.button() == InputConstants.MOUSE_BUTTON_RIGHT;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            if (active) {
                onStep.accept(stepAmount(event));
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }

        @Override
        public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    SPRITES.get(active, isHoveredOrFocused()),
                    x,
                    y,
                    width,
                    height,
                    ARGB.white(alpha)
            );
            Font font = net.minecraft.client.Minecraft.getInstance().font;
            int textX = x + (width - font.width(label)) / 2;
            int textY = y + (height - font.lineHeight) / 2 + 1;
            int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
            graphics.text(font, label, textX, textY, color, false);
        }
    }
}
