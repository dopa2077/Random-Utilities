package com.dopa.randomutilities.core.gui.panel;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
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
            if (panel.isOccupying()) {
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
        // Frame-rate animation: containerTick is 20 Hz and made the 0.12s slide look stepped.
        tick();
        // Closed tabs, then bodies (cover neighbors), then occupying icon on top of the body corner.
        for (AttachedPanel panel : panels) {
            if (panel.isOccupying() && panel.progress() > 0.001F) {
                continue;
            }
            panel.renderTabOnly(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY);
        }
        for (AttachedPanel panel : panels) {
            panel.renderBodyIfOpen(graphics, font, leftPos, topPos, imageWidth, mouseX, mouseY, partialTick);
        }
        AttachedPanel occupying = openPanel();
        if (occupying != null && occupying.progress() > 0.001F) {
            occupying.renderTabIconOnly(graphics, font, leftPos, topPos, imageWidth);
        }
    }

    public void layoutWidgets(int leftPos, int topPos, int imageWidth) {
        for (AttachedPanel panel : panels) {
            panel.layoutWidgets(leftPos, topPos, imageWidth);
        }
    }

    /**
     * Areas JEI should avoid: every tab, plus each panel body while it is open/animating.
     */
    public List<Rect2i> collectExtraAreas(int leftPos, int topPos, int imageWidth) {
        List<Rect2i> areas = new ArrayList<>();
        for (AttachedPanel panel : panels) {
            areas.add(new Rect2i(
                    panel.tabX(leftPos, imageWidth),
                    panel.tabY(topPos),
                    AttachedPanel.TAB_SIZE,
                    AttachedPanel.TAB_SIZE
            ));
            if (panel.progress() > 0.001F) {
                int w = panel.bodyWidthAnimated();
                int h = panel.bodyHeightAnimated();
                if (w > 0 && h > 0) {
                    areas.add(new Rect2i(
                            panel.bodyXAnimated(leftPos, imageWidth),
                            panel.bodyY(topPos),
                            w,
                            h
                    ));
                }
            }
        }
        return areas;
    }

    @Nullable
    public Component hoveredTabTooltip(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        AttachedPanel occupying = openPanel();
        if (occupying != null && occupying.isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            // Open panel suppresses its own tab tooltip; closed tabs under the body stay hidden too.
            return null;
        }
        for (AttachedPanel panel : panels) {
            if (panel.isOccupying()) {
                continue;
            }
            if (panel.isMouseOverTab(mouseX, mouseY, leftPos, topPos, imageWidth)) {
                return panel.title();
            }
        }
        return null;
    }

    /**
     * @return true if a tab click was handled
     */
    public boolean handleTabClick(double mouseX, double mouseY, int leftPos, int topPos, int imageWidth) {
        AttachedPanel occupying = openPanel();
        if (occupying != null && occupying.isMouseOverBody(mouseX, mouseY, leftPos, topPos, imageWidth)) {
            if (occupying.isMouseOverTab(mouseX, mouseY, leftPos, topPos, imageWidth)) {
                pendingOpen = null;
                occupying.requestClose();
                return true;
            }
            if (occupying.isMouseOverDecorativeArea(mouseX, mouseY, leftPos, topPos, imageWidth)) {
                return true;
            }
            // Other tabs under this body are covered visually — treat as empty body (close),
            // do not open/activate the covered tab.
            pendingOpen = null;
            occupying.requestClose();
            return true;
        }
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

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY,
                                 int leftPos, int topPos, int imageWidth, Font font) {
        AttachedPanel occupying = openPanel();
        if (occupying == null) {
            return false;
        }
        return occupying.mouseScrolled(mouseX, mouseY, scrollY, leftPos, topPos, imageWidth, font);
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        AttachedPanel occupying = openPanel();
        return occupying != null && occupying.mouseClicked(mouseX, mouseY);
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        AttachedPanel occupying = openPanel();
        return occupying != null && occupying.mouseDragged(mouseX, mouseY);
    }

    public boolean mouseReleased() {
        AttachedPanel occupying = openPanel();
        return occupying != null && occupying.mouseReleased();
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

    /** Instantly open the panel at the given anchor (used after screen rebuild). */
    public void snapOpen(PanelAnchor anchor) {
        pendingOpen = null;
        for (AttachedPanel panel : panels) {
            if (panel.anchor() == anchor) {
                panel.snapOpen();
                return;
            }
        }
    }
}
