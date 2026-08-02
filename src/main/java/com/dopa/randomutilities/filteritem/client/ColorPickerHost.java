package com.dopa.randomutilities.filteritem.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Screen hook for {@link FilterColorPicker}. Implement on any screen to reuse the RGB picker overlay
 * without depending on {@link FilterScreen}.
 */
public interface ColorPickerHost {
    int width();

    int height();

    Font getFont();

    void clearFocus();

    int getPickerColor();

    void onPickerColorCommitted(int rgb);

    <T extends GuiEventListener & net.minecraft.client.gui.components.Renderable & net.minecraft.client.gui.narration.NarratableEntry> T addPickerWidget(T widget);

    void removePickerWidget(GuiEventListener widget);
}
