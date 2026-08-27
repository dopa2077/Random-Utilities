package com.dopa.randomutilities.machine.breaker.client;

import com.dopa.randomutilities.machine.breaker.menu.AdvancedBlockBreakerMenu;
import com.dopa.randomutilities.core.gui.machine.AdvancedVolumeMachineScreen;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

public class AdvancedBlockBreakerScreen extends AdvancedVolumeMachineScreen<AdvancedBlockBreakerMenu> {
    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath("dopasrandomutilities", "textures/gui/machine/advanced_block_breaker.png");

    public AdvancedBlockBreakerScreen(AdvancedBlockBreakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, AdvancedBlockBreakerMenu.IMAGE_HEIGHT);
    }

    @Override
    protected Identifier backgroundTexture() {
        return BACKGROUND;
    }

    @Override
    protected String infoPanelKey() {
        return "gui.dopasrandomutilities.panel.info.advanced_block_breaker.intro";
    }

    @Override
    protected boolean showMutePanel() {
        return true;
    }

    @Override
    @Nullable
    protected String optionalPickaxeTooltipKey() {
        return "gui.dopasrandomutilities.advanced_block_breaker.pickaxe.optional";
    }
}
