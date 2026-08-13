package com.dopa.randomutilities.fishnet.client.panel;

import com.dopa.randomutilities.client.gui.AttachedPanel;
import com.dopa.randomutilities.client.gui.PanelAnchor;
import com.dopa.randomutilities.fishnet.client.FishnetScreen;
import com.dopa.randomutilities.fishnet.network.FishnetSettingPayload;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Cosmetics: single toggle buttons for catch particles and splash sound. */
public final class FishnetCosmeticPanel extends AttachedPanel {
    private static final int BG = 0xFF7B5A96;
    private static final int BUTTON_W = 100;
    private static final int BUTTON_H = 18;
    private static final int TRAY_PAD = 4;
    private static final int PARTICLES_LABEL_Y = 22;
    private static final int PARTICLES_BUTTON_Y = 34;
    private static final int SOUND_LABEL_Y = 66;
    private static final int SOUND_BUTTON_Y = 78;
    private static final ItemStack DYE_ICON = new ItemStack(Items.DYE.pink());

    private final FishnetScreen screen;
    private final int tabYBias;
    private Button particlesButton;
    private Button soundButton;
    private boolean widgetsCreated;

    public FishnetCosmeticPanel(FishnetScreen screen, int tabYBias) {
        super(
                PanelAnchor.LEFT_BELOW,
                136,
                112,
                BG,
                Component.translatable("gui.dopasrandomutilities.panel.cosmetic")
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
        particlesButton = Button.builder(Component.empty(), b -> toggleParticles())
                .bounds(0, 0, BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.dopasrandomutilities.fishnet.particles.tooltip")))
                .build();
        soundButton = Button.builder(Component.empty(), b -> toggleSound())
                .bounds(0, 0, BUTTON_W, BUTTON_H)
                .tooltip(Tooltip.create(Component.translatable(
                        "gui.dopasrandomutilities.fishnet.sound.tooltip")))
                .build();
        screen.addOverlayWidget(particlesButton);
        screen.addOverlayWidget(soundButton);
        refreshButtons();
        updateWidgetVisibility(false);
    }

    private void toggleParticles() {
        boolean next = !screen.getMenu().isParticlesEnabled();
        ClientPacketDistributor.sendToServer(FishnetSettingPayload.particles(next));
        particlesButton.setMessage(enabledLabel(next, true));
    }

    private void toggleSound() {
        boolean next = !screen.getMenu().isSoundEnabled();
        ClientPacketDistributor.sendToServer(FishnetSettingPayload.sound(next));
        soundButton.setMessage(enabledLabel(next, false));
    }

    private void refreshButtons() {
        if (!widgetsCreated) {
            return;
        }
        particlesButton.setMessage(enabledLabel(screen.getMenu().isParticlesEnabled(), true));
        soundButton.setMessage(enabledLabel(screen.getMenu().isSoundEnabled(), false));
    }

    private static Component enabledLabel(boolean enabled, boolean particles) {
        if (particles) {
            return Component.translatable(enabled
                    ? "gui.dopasrandomutilities.fishnet.particles.enabled"
                    : "gui.dopasrandomutilities.fishnet.particles.disabled");
        }
        return Component.translatable(enabled
                ? "gui.dopasrandomutilities.fishnet.sound.enabled"
                : "gui.dopasrandomutilities.fishnet.sound.disabled");
    }

    private TrayBounds particlesTray(int bodyX, int bodyY) {
        return trayBounds(bodyX, panelWidth, BUTTON_W, bodyY + PARTICLES_BUTTON_Y, BUTTON_H, TRAY_PAD);
    }

    private TrayBounds soundTray(int bodyX, int bodyY) {
        return trayBounds(bodyX, panelWidth, BUTTON_W, bodyY + SOUND_BUTTON_Y, BUTTON_H, TRAY_PAD);
    }

    @Override
    public boolean isMouseOverDecorativeArea(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        if (!contentsInteractive()) {
            return false;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        TrayBounds particles = particlesTray(bx, by);
        TrayBounds sound = soundTray(bx, by);
        return isMouseOverRect(mouseX, mouseY, particles.x(), particles.y(), particles.width(), particles.height())
                || isMouseOverRect(mouseX, mouseY, sound.x(), sound.y(), sound.width(), sound.height());
    }

    @Override
    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        if (!widgetsCreated) {
            return;
        }
        int bx = bodyXOpen(leftPos, imageWidth);
        int by = bodyY(topPos);
        int x = bx + (panelWidth - BUTTON_W) / 2;
        particlesButton.setX(x);
        particlesButton.setY(by + PARTICLES_BUTTON_Y);
        soundButton.setX(x);
        soundButton.setY(by + SOUND_BUTTON_Y);
    }

    @Override
    protected void updateWidgetVisibility(boolean interactive) {
        if (!widgetsCreated) {
            return;
        }
        particlesButton.visible = interactive;
        soundButton.visible = interactive;
        particlesButton.active = interactive;
        soundButton.active = interactive;
    }

    @Override
    protected void onTick() {
        if (contentsInteractive()) {
            refreshButtons();
        }
    }

    @Override
    protected void renderIcon(GuiGraphicsExtractor graphics, Font font, int centerX, int centerY) {
        graphics.item(DYE_ICON, centerX - 8, centerY - 8, centerX ^ centerY);
    }

    @Override
    protected void renderContents(
            GuiGraphicsExtractor graphics,
            Font font,
            int bodyX,
            int bodyY,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        renderTitleRow(graphics, font, bodyX, bodyY);
        refreshButtons();
        drawLabel(
                graphics,
                font,
                Component.translatable("gui.dopasrandomutilities.fishnet.particles"),
                bodyX,
                bodyY + PARTICLES_LABEL_Y
        );
        renderTray(graphics, particlesTray(bodyX, bodyY), BG);
        drawLabel(
                graphics,
                font,
                Component.translatable("gui.dopasrandomutilities.fishnet.sound"),
                bodyX,
                bodyY + SOUND_LABEL_Y
        );
        renderTray(graphics, soundTray(bodyX, bodyY), BG);
    }
}
