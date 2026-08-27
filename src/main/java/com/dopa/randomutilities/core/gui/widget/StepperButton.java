package com.dopa.randomutilities.core.gui.widget;

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

import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/** Vanilla-sprite +/- control used by config trays and the redstone clock. */
public final class StepperButton extends AbstractWidget {
    private static final WidgetSprites SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("widget/button"),
            Identifier.withDefaultNamespace("widget/button_disabled"),
            Identifier.withDefaultNamespace("widget/button_highlighted")
    );

    private final String label;
    private final IntConsumer onStep;
    private final BooleanSupplier enabled;
    private final boolean scaledSteps;

    public StepperButton(int width, int height, String label, Component tooltip, Runnable onPress) {
        this(width, height, label, tooltip, step -> onPress.run(), () -> true, false);
    }

    public StepperButton(
            int width,
            int height,
            String label,
            Component tooltip,
            IntConsumer onStep,
            BooleanSupplier enabled
    ) {
        this(width, height, label, tooltip, onStep, enabled, true);
    }

    private StepperButton(
            int width,
            int height,
            String label,
            Component tooltip,
            IntConsumer onStep,
            BooleanSupplier enabled,
            boolean scaledSteps
    ) {
        super(0, 0, width, height, Component.literal(label));
        this.label = label;
        this.onStep = onStep;
        this.enabled = enabled;
        this.scaledSteps = scaledSteps;
        this.active = enabled.getAsBoolean();
        setTooltip(Tooltip.create(tooltip));
    }

    @Override
    protected boolean isValidClickButton(net.minecraft.client.input.MouseButtonInfo buttonInfo) {
        if (!scaledSteps) {
            return super.isValidClickButton(buttonInfo);
        }
        return buttonInfo.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT
                || buttonInfo.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.active = enabled.getAsBoolean();
        if (!active) {
            return;
        }
        int amount = 1;
        if (scaledSteps) {
            if (event.hasControlDown()) {
                amount = 100;
            } else if (event.hasShiftDown()) {
                amount = 10;
            }
        }
        onStep.accept(amount);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.active = enabled.getAsBoolean();
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
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (height - font.lineHeight) / 2 + 1;
        int color = active ? 0xFFFFFFFF : 0xFFA0A0A0;
        graphics.text(font, label, textX, textY, color, false);
    }
}
