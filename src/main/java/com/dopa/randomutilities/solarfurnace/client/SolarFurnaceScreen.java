package com.dopa.randomutilities.solarfurnace.client;

import com.dopa.randomutilities.gui.panel.PanelAnchor;
import com.dopa.randomutilities.gui.panel.PanelHost;
import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.gui.machine.MachineRedstonePanel;
import com.dopa.randomutilities.gui.machine.MachineUpgradePanel;
import com.dopa.randomutilities.gui.machine.UpgradeSlotTooltips;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.solarfurnace.SolarPower;
import com.dopa.randomutilities.solarfurnace.client.panel.SolarFurnaceInformativePanel;
import com.dopa.randomutilities.solarfurnace.menu.SolarFurnaceMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SolarFurnaceScreen extends AbstractContainerScreen<SolarFurnaceMenu>
        implements MachineRedstonePanel.Host {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/solar_furnace.png");
    private static final Identifier BURN_PROGRESS_SPRITE =
            Identifier.withDefaultNamespace("container/furnace/burn_progress");
    private static final int TEXTURE_SIZE = 256;
    private static final int LABEL_COLOR = 0xFF404040;
    /** Packed extras at u ≥ 176 (skip red 18×18 guides). */
    private static final int LIT_BOLT_U = 177;
    private static final int LIT_BOLT_V = 1;
    private static final int CHIP_V = 22;
    private static final int CHIP_GREEN_U = 181;
    private static final int CHIP_YELLOW_U = 198;
    private static final int CHIP_RED_U = 215;

    private static final int ARROW_W = 24;
    private static final int ARROW_H = 16;
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;

    /** Sun-strength bands for the status square / tooltip (0–1 of noon peak). */
    private static final float FADING_MAX = 0.60F;
    private static final float STRONG_MAX = 0.95F;

    private enum SunlightBand {
        FADING,
        STRONG,
        PEAK
    }

    private final PanelHost panelHost = new PanelHost();
    private final int tabYBias = SolarFurnaceMenu.TAB_Y_BIAS;

    @Nullable
    private MachineRedstonePanel redstonePanel;

    public SolarFurnaceScreen(SolarFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        this.inventoryLabelY = this.imageHeight - 94;
        this.titleLabelY = 6;
    }

    public PanelHost getPanelHost() {
        return this.panelHost;
    }

    public int leftPos() {
        return this.leftPos;
    }

    public int topPos() {
        return this.topPos;
    }

    public int imageWidth() {
        return this.imageWidth;
    }

    @Override
    public <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable
            & net.minecraft.client.gui.narration.NarratableEntry> T addOverlayWidget(T widget) {
        return this.addRenderableWidget(widget);
    }

    @Override
    public Supplier<RedstoneMode> redstoneMode() {
        return () -> this.menu.redstoneMode();
    }

    @Override
    protected void init() {
        super.init();
        this.panelHost.clear();
        this.redstonePanel = null;

        this.panelHost.add(new SolarFurnaceInformativePanel(tabYBias));
        this.panelHost.add(new MachineUpgradePanel(
                this.menu.getUpgradeSlots(), PanelAnchor.RIGHT_TOP, tabYBias));
        this.redstonePanel = new MachineRedstonePanel(this, PanelAnchor.RIGHT_BELOW, tabYBias);
        this.panelHost.add(this.redstonePanel);
        this.redstonePanel.initWidgets();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.panelHost.layoutWidgets(this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        this.panelHost.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth,
                mouseX, mouseY, partialTick);

        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, TEXTURE_SIZE, TEXTURE_SIZE);

        drawThunderFill(graphics, xo, yo);
        drawStatusSquare(graphics, xo, yo);

        float progress = this.menu.progressFraction();
        if (progress > 0.0F) {
            int filled = Math.max(1, Mth.ceil(progress * ARROW_W));
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BURN_PROGRESS_SPRITE,
                    ARROW_W,
                    ARROW_H,
                    0,
                    0,
                    xo + ARROW_X,
                    yo + ARROW_Y,
                    filled,
                    ARROW_H
            );
        }
    }

    private void drawThunderFill(GuiGraphicsExtractor graphics, int xo, int yo) {
        float strength = this.menu.solarStrengthFraction();
        if (strength <= 0.0F) {
            return;
        }
        int fillH = Math.max(1, Mth.ceil(strength * SolarFurnaceMenu.THUNDER_H));
        int vOffset = SolarFurnaceMenu.THUNDER_H - fillH;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                xo + SolarFurnaceMenu.THUNDER_X,
                yo + SolarFurnaceMenu.THUNDER_Y + vOffset,
                (float) LIT_BOLT_U,
                (float) (LIT_BOLT_V + vOffset),
                SolarFurnaceMenu.THUNDER_W,
                fillH,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }

    private void drawStatusSquare(GuiGraphicsExtractor graphics, int xo, int yo) {
        Integer chipU = statusChipU();
        if (chipU == null) {
            return;
        }
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                xo + SolarFurnaceMenu.SOLAR_INDICATOR_X,
                yo + SolarFurnaceMenu.SOLAR_INDICATOR_Y,
                (float) chipU,
                (float) CHIP_V,
                SolarFurnaceMenu.SOLAR_INDICATOR_SIZE,
                SolarFurnaceMenu.SOLAR_INDICATOR_SIZE,
                TEXTURE_SIZE,
                TEXTURE_SIZE
        );
    }

    @Nullable
    private Integer statusChipU() {
        return switch (this.menu.solarStatus()) {
            case NO_SKY -> CHIP_RED_U;
            case NO_SUN -> null;
            case WORKING -> sunlightBand(this.menu.solarStrengthFraction()) == SunlightBand.FADING
                    ? CHIP_YELLOW_U
                    : CHIP_GREEN_U;
        };
    }

    private static SunlightBand sunlightBand(float strength) {
        if (strength < FADING_MAX) {
            return SunlightBand.FADING;
        }
        if (strength < STRONG_MAX) {
            return SunlightBand.STRONG;
        }
        return SunlightBand.PEAK;
    }

    @Override
    public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(graphics, mouseX, mouseY, partialTick);

        Component tabTooltip = this.panelHost.hoveredTabTooltip(
                mouseX, mouseY, this.leftPos, this.topPos, this.imageWidth);
        if (tabTooltip != null) {
            graphics.setTooltipForNextFrame(this.font, tabTooltip, mouseX, mouseY);
            return;
        }
        if (this.hoveredSlot != null
                && this.menu.isUpgradeSlotIndex(this.hoveredSlot.index)
                && this.hoveredSlot.hasItem()) {
            int used = this.menu.blockEntity().upgrades().countOf(this.hoveredSlot.getItem().getItem());
            Component peak = Component.translatable("gui.dopasrandomutilities.solar_furnace.peak_speed")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(UpgradeConfig.solarPeakPercent(used) + "%")
                            .withStyle(ChatFormatting.GREEN));
            UpgradeSlotTooltips.applyHover(
                    graphics,
                    this.font,
                    mouseX,
                    mouseY,
                    this.hoveredSlot,
                    true,
                    this.menu.blockEntity().upgrades(),
                    List.of(),
                    peak
            );
            return;
        }
        if (isHoveringSolar(mouseX, mouseY)) {
            List<FormattedCharSequence> lines = new ArrayList<>();
            for (Component line : solarTooltip()) {
                lines.add(line.getVisualOrderText());
            }
            graphics.setTooltipForNextFrame(this.font, lines, mouseX, mouseY);
        }
    }

    private boolean isHoveringSolar(double mouseX, double mouseY) {
        if (this.isHovering(
                SolarFurnaceMenu.SOLAR_INDICATOR_X,
                SolarFurnaceMenu.SOLAR_INDICATOR_Y,
                SolarFurnaceMenu.SOLAR_INDICATOR_SIZE,
                SolarFurnaceMenu.SOLAR_INDICATOR_SIZE,
                mouseX,
                mouseY
        )) {
            return true;
        }
        return this.isHovering(
                SolarFurnaceMenu.THUNDER_X,
                SolarFurnaceMenu.THUNDER_Y,
                SolarFurnaceMenu.THUNDER_W,
                SolarFurnaceMenu.THUNDER_H,
                mouseX,
                mouseY
        );
    }

    private List<Component> solarTooltip() {
        List<Component> lines = new ArrayList<>();
        SolarPower.Status status = this.menu.solarStatus();
        switch (status) {
            case NO_SKY -> {
                lines.add(Component.translatable("gui.dopasrandomutilities.solar_furnace.solar.no_sky")
                        .withStyle(ChatFormatting.RED));
                lines.add(Component.translatable("gui.dopasrandomutilities.solar_furnace.solar.no_sky.hint")
                        .withStyle(ChatFormatting.GRAY));
            }
            case NO_SUN -> {
                lines.add(Component.translatable("gui.dopasrandomutilities.solar_furnace.solar.no_sun")
                        .withStyle(ChatFormatting.DARK_GRAY));
                lines.add(Component.translatable("gui.dopasrandomutilities.solar_furnace.solar.no_sun.hint")
                        .withStyle(ChatFormatting.GRAY));
            }
            case WORKING -> {
                float strength = this.menu.solarStrengthFraction();
                int percent = Mth.clamp(Math.round(strength * 100.0F), 0, 100);
                SunlightBand band = sunlightBand(strength);
                ChatFormatting powerColor = band == SunlightBand.FADING
                        ? ChatFormatting.YELLOW
                        : ChatFormatting.GREEN;
                String bandKey = switch (band) {
                    case FADING -> "gui.dopasrandomutilities.solar_furnace.solar.working.weak";
                    case STRONG -> "gui.dopasrandomutilities.solar_furnace.solar.working.high";
                    case PEAK -> "gui.dopasrandomutilities.solar_furnace.solar.working.peak";
                };
                lines.add(Component.translatable(
                        "gui.dopasrandomutilities.solar_furnace.solar.working",
                        Integer.toString(percent)
                ).withStyle(powerColor));
                lines.add(Component.translatable(bandKey).withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, this.titleLabelY, LABEL_COLOR, false);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean overTab = false;
        for (var p : this.panelHost.panels()) {
            if (p.isMouseOverTab(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth)) {
                overTab = true;
                break;
            }
        }
        var occupying = this.panelHost.openPanel();
        boolean overBody = occupying != null
                && occupying.isMouseOverBody(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        // Open body covers sibling tabs at the attachment edge — handle body/scrollbar first.
        if (overBody) {
            for (int i = this.children().size() - 1; i >= 0; i--) {
                GuiEventListener child = this.children().get(i);
                if (child.mouseClicked(event, doubleClick)) {
                    this.setFocused(child);
                    this.clearFocus();
                    return true;
                }
            }
            for (int i = this.children().size() - 1; i >= 0; i--) {
                GuiEventListener child = this.children().get(i);
                if (child instanceof AbstractWidget widget && widget.visible && !widget.active
                        && isOverWidget(widget, event.x(), event.y())) {
                    return true;
                }
            }
            Slot slotUnder = findActiveSlotAt(event.x(), event.y());
            if (slotUnder != null && this.menu.isUpgradeSlotIndex(slotUnder.index)) {
                return super.mouseClicked(event, doubleClick);
            }
            if (this.panelHost.mouseClicked(event.x(), event.y())) {
                return true;
            }
            return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        }
        if (overTab) {
            return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
        }
        boolean handled = super.mouseClicked(event, doubleClick);
        if (handled) {
            this.clearFocus();
            return true;
        }
        return this.panelHost.handleTabClick(event.x(), event.y(), this.leftPos, this.topPos, this.imageWidth);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.panelHost.mouseDragged(event.x(), event.y())) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean panelHandled = this.panelHost.mouseReleased();
        boolean handled = super.mouseReleased(event);
        return panelHandled || handled;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.panelHost.mouseScrolled(mouseX, mouseY, scrollY, this.leftPos, this.topPos, this.imageWidth, this.font)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Slot findActiveSlotAt(double mouseX, double mouseY) {
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private static boolean isOverWidget(AbstractWidget widget, double mouseX, double mouseY) {
        return mouseX >= widget.getX()
                && mouseY >= widget.getY()
                && mouseX < widget.getX() + widget.getWidth()
                && mouseY < widget.getY() + widget.getHeight();
    }
}
