package com.dopa.randomutilities.blockplacer.client;

import com.dopa.randomutilities.blockplacer.menu.AdvancedBlockPlacerMenu;
import com.dopa.randomutilities.gui.machine.AdvancedVolumeMachineScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public class AdvancedBlockPlacerScreen extends AdvancedVolumeMachineScreen<AdvancedBlockPlacerMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/advanced_block_placer.png");

    public AdvancedBlockPlacerScreen(AdvancedBlockPlacerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AdvancedBlockPlacerMenu.IMAGE_HEIGHT);
    }

    @Override
    protected Identifier backgroundTexture() {
        return BACKGROUND;
    }

    @Override
    protected String infoPanelKey() {
        return "gui.dopasrandomutilities.panel.info.advanced_block_placer.intro";
    }

    @Override
    protected boolean showMutePanel() {
        return false;
    }

    @Override
    @Nullable
    protected String optionalPickaxeTooltipKey() {
        return null;
    }
}
