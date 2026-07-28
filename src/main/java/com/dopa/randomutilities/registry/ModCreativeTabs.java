package com.dopa.randomutilities.registry;

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
                        output.accept(ModItems.BASIC_STONE_GENERATOR.get());
                        output.accept(ModItems.INTERMEDIATE_STONE_GENERATOR.get());
                        output.accept(ModItems.ADVANCED_STONE_GENERATOR.get());
                        output.accept(ModItems.ELITE_STONE_GENERATOR.get());
                        output.accept(ModItems.ULTIMATE_STONE_GENERATOR.get());
                        output.accept(ModItems.CREATIVE_STONE_GENERATOR.get());
                        output.accept(ModItems.RANDOM_ORE_GENERATOR.get());
                        output.accept(ModItems.METAL_BLOCK_GENERATOR.get());
                        output.accept(ModItems.CREATIVE_RANDOM_ORE_GENERATOR.get());
                        output.accept(ModItems.CREATIVE_METAL_BLOCK_GENERATOR.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
