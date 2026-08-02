package com.dopa.randomutilities.filteritem.client.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns attached panels for a screen and enforces exclusive open with close-then-open sequencing.
 */
public final class PanelHost {
    private final List<AttachedPanel> panels = new ArrayList<>();
    @Nullable
    private AttachedPanel pendingOpen;
    private long lastNanos = System.nanoTime();

    public void clear() {
        panels.clear();
        pendingOpen = null;
    }

    public void add(AttachedPanel panel) {
        panels.add(panel);
    }

    public List<AttachedPanel> panels() {
        return List.copyOf(panels);
    }

    @Nullable
    public AttachedPanel openPanel() {
        for (AttachedPanel panel : panels) {
            if (panel.isExpanded()) {
                return panel;
            }
        }
        return null;
    }

    public void tick() {
        long now = System.nanoTime();
        float dt = Math.min(0.05F, (now - lastNanos) / 1_000_000_000.0F);
        lastNanos = now;

        for (AttachedPanel panel : panels) {
            panel.tick(dt);
        }

        if (pendingOpen != null) {
            AttachedPanel current = openPanel();
            if (current == null) {
                AttachedPanel toOpen = pendingOpen;
                pendingOpen = null;
                toOpen.requestOpen();
            } else if (current != pendingOpen && !current.isAnimating()) {
                current.requestClose();
            }
        }
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int leftPos, int topPos, int imageWidth,
                       int mouseX, int mouseY, float partialTick) {
        for (AttachedPanel panel : panels) {
            panel.render(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);
        }
    }

    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        for (AttachedPanel panel : panels) {
            panel.layoutWidgets(leftPos, topPos, imageWidth);
        }
    }

    /**
     * @return true if a tab click was handled
     */
    public boolean handleTabClick(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        for (AttachedPanel panel : panels) {
            if (!panel.isMouseOverTab(mouseX, mouseY, leftPos, topPos, imageWidth)) {
                continue;
            }
            if (panel.isOpen() || panel.state() == AttachedPanel.AnimState.OPENING) {
                pendingOpen = null;
                panel.requestClose();
                return true;
            }
            requestOpen(panel);
            return true;
        }
        return false;
    }

    public void requestOpen(AttachedPanel panel) {
        AttachedPanel current = openPanel();
        if (current == null) {
            pendingOpen = null;
            panel.requestOpen();
            return;
        }
        if (current == panel) {
            return;
        }
        pendingOpen = panel;
        current.requestClose();
    }
}
