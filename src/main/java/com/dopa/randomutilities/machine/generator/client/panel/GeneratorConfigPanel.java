package com.dopa.randomutilities.machine.generator.client.panel;

import com.dopa.randomutilities.filteritem.client.panel.AttachedPanel;
import com.dopa.randomutilities.filteritem.client.panel.PanelAnchor;
import com.dopa.randomutilities.machine.generator.client.ResourceGeneratorScreen;
import com.dopa.randomutilities.machine.network.MachineSettingPayload;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

public final class GeneratorConfigPanel extends AttachedPanel {
    private static final int BG = 0xFF1A4548;
    private static final int LOCK_LABEL_Y = 28;
    private static final int LOCK_BUTTON_Y = 40;
    private static final int LOCK_BUTTON_H = 18;
    private static final ItemStack COMPARATOR_ICON = new ItemStack(Items.COMPARATOR);

    private final ResourceGeneratorScreen screen;
    private final int tabYBias;
    private Button lockButton;
    private boolean widgetsCreated;

    public GeneratorConfigPanel(ResourceGeneratorScreen screen, int tabYBias) {
        super(
                PanelAnchor.LEFT_BELOW,
                136,
                72,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.config")
        );
        this.screen = screen;
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
        lockButton = Button.builder(Component.empty(), b -> toggleLock())
                .bounds(0, 0, 100, LOCK_BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.dopasrandomutilities.panel.config.lock_output.tooltip")))
                .build();
        screen.addOverlayWidget(lockButton);
        refreshLockButton();
        updateWidgetVisibility(false);
    }

    private void toggleLock() {
        boolean next = !screen.getMenu().isOutputLocked();
        ClientPacketDistributor.sendToServer(MachineSettingPayload.lockOutput(next));
        lockButton.setMessage(Component.translatable(next
                ? "gui.dopasrandomutilities.panel.config.lock_output.enabled"
                : "gui.dopasrandomutilities.panel.config.lock_output.disabled"));
    }

    private void refreshLockButton() {
        if (!widgetsCreated || lockButton == null) {
            return;
        }
        boolean locked = screen.getMenu().isOutputLocked();
        lockButton.setMessage(Component.translatable(locked
                ? "gui.dopasrandomutilities.panel.config.lock_output.enabled"
                : "gui.dopasrandomutilities.panel.config.lock_output.disabled"));
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        lockButton.setX(bx + (panelWidth - lockButton.getWidth()) / 2);
        lockButton.setY(by + LOCK_BUTTON_Y);
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        lockButton.visible = interactive;
        lockButton.active = interactive;
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(COMPARATOR_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(GuiGraphicsExtractor graphics, Font font, int bodyX, int bodyY,
                                  int mouseX, int mouseY, float partialTick) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        refreshLockButton();
        drawLabel(graphics, font, Component.translatable("gui.dopasrandomutilities.panel.config.lock_output"),
                bodyX, bodyY + LOCK_LABEL_Y);
    }
}
