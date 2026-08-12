package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, dOPasRandomUtilities.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RANDOM_UTILITIES =
            CREATIVE_MODE_TABS.register("random_utilities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dopasrandomutilities"))
                    .icon(() -> new ItemStack(ModItems.BASIC_STONE_GENERATOR.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.DEV_NULL.get());
                        output.accept(ModItems.ADVANCED_DEV_NULL.get());
                        output.accept(ModItems.MINI_CHEST.get());
                        output.accept(ModItems.TRASH_CAN.get());
                        output.accept(ModItems.REDSTONE_CLOCK.get());
                        output.accept(ModItems.BASIC_ITEM_COLLECTOR.get());
                        output.accept(ModItems.ADVANCED_ITEM_COLLECTOR.get());
                        output.accept(ModItems.SOLAR_FURNACE.get());
                        output.accept(ModItems.FISHNET.get());
                        for (GeneratorType type : GeneratorType.values()) {
                            output.accept(ModItems.forType(type).get());
                        }
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UPGRADES =
            CREATIVE_MODE_TABS.register("upgrades", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dopasrandomutilities.upgrades"))
                    .icon(() -> new ItemStack(ModItems.UPGRADE_CASING.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.UPGRADE_CASING.get());
                        output.accept(ModItems.PRODUCTIVITY_UPGRADE.get());
                        output.accept(ModItems.OVERCLOCK_UPGRADE.get());
                        output.accept(ModItems.FORTUNE_MESH_UPGRADE.get());
                        output.accept(ModItems.TREASURE_MESH_UPGRADE.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
