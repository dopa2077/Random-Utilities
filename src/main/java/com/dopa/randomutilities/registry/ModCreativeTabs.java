package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.config.FeatureConfig;
import com.dopa.randomutilities.config.ModContentIds;
import com.dopa.randomutilities.generator.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.transfer.HeadKind;
import com.dopa.randomutilities.transfer.TransferNodeItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, dOPasRandomUtilities.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> RANDOM_UTILITIES =
            CREATIVE_MODE_TABS.register("random_utilities", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dopasrandomutilities"))
                    .icon(() -> tabIcon(
                            ModItems.forType(GeneratorType.BASIC_STONE),
                            ModItems.DEV_NULL,
                            ModItems.MINI_CHEST,
                            ModItems.LASSO
                    ))
                    .displayItems((parameters, output) -> {
                        accept(output, ModItems.DEV_NULL);
                        accept(output, ModItems.ADVANCED_DEV_NULL);
                        accept(output, ModItems.MINI_CHEST);
                        accept(output, ModItems.TRASH_CAN);
                        accept(output, ModItems.REDSTONE_CLOCK);
                        accept(output, ModItems.BASIC_ITEM_COLLECTOR);
                        accept(output, ModItems.ADVANCED_ITEM_COLLECTOR);
                        accept(output, ModItems.ITEM_MAGNET);
                        accept(output, ModItems.SOLAR_FURNACE);
                        accept(output, ModItems.FISHNET);
                        accept(output, ModItems.SIMPLE_BLOCK_BREAKER);
                        accept(output, ModItems.ADVANCED_BLOCK_BREAKER);
                        accept(output, ModItems.SIMPLE_BLOCK_PLACER);
                        accept(output, ModItems.ADVANCED_BLOCK_PLACER);
                        accept(output, ModItems.COMBUSTION_GENERATOR);
                        accept(output, ModItems.SOLAR_PANEL_CONTROLLER);
                        accept(output, ModItems.SOLAR_PANEL_TIER1);
                        accept(output, ModItems.SOLAR_PANEL_TIER2);
                        accept(output, ModItems.SOLAR_PANEL_TIER3);
                        accept(output, ModItems.SIMPLE_CORE_FRAME);
                        accept(output, ModItems.ADVANCED_CORE_FRAME);
                        accept(output, ModItems.LASSO);
                        accept(output, ModItems.GOLDEN_LASSO);
                        accept(output, ModItems.CURSED_LASSO);
                        accept(output, ModItems.TINY_TNT);
                        accept(output, ModItems.WOOD_CHIP);
                        accept(output, ModItems.CARDBOARD_BOX);
                        for (var pipe : ModItems.pipes()) {
                            output.accept(pipe.get());
                        }
                        accept(output, ModItems.TRANSFER_NODE);
                        if (FeatureConfig.isItemEnabled(ModContentIds.TRANSFER_NODE_FLUID)) {
                            accept(output, TransferNodeItem.create(HeadKind.FLUID));
                        }
                        if (FeatureConfig.isItemEnabled(ModContentIds.TRANSFER_NODE_ENERGY)) {
                            accept(output, TransferNodeItem.create(HeadKind.ENERGY));
                        }
                        accept(output, ModItems.FILTER);
                        for (GeneratorType type : GeneratorType.values()) {
                            accept(output, ModItems.forType(type));
                        }
                    })
                    .build());

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> UPGRADES =
            CREATIVE_MODE_TABS.register("upgrades", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.dopasrandomutilities.upgrades"))
                    .icon(() -> tabIcon(
                            ModItems.UPGRADE_CASING,
                            ModItems.PRODUCTIVITY_UPGRADE
                    ))
                    .displayItems((parameters, output) -> {
                        accept(output, ModItems.UPGRADE_CASING);
                        accept(output, ModItems.PRODUCTIVITY_UPGRADE);
                        accept(output, ModItems.OVERCLOCK_UPGRADE);
                        accept(output, ModItems.FORTUNE_MESH_UPGRADE);
                        accept(output, ModItems.TREASURE_MESH_UPGRADE);
                        accept(output, ModItems.ENERGY_UPGRADE);
                        accept(output, ModItems.FLUID_CAPACITY_UPGRADE);
                        accept(output, ModItems.EFFICIENCY_UPGRADE);
                        accept(output, ModItems.RANGE_UPGRADE);
                        accept(output, ModItems.STACK_UPGRADE);
                    })
                    .build());

    private ModCreativeTabs() {}

    private static void accept(CreativeModeTab.Output output, DeferredItem<? extends Item> item) {
        if (item != null) {
            output.accept(item.get());
        }
    }

    private static void accept(CreativeModeTab.Output output, ItemStack stack) {
        if (!stack.isEmpty()) {
            output.accept(stack);
        }
    }

    private static ItemStack tabIcon(DeferredItem<? extends Item>... candidates) {
        for (DeferredItem<? extends Item> candidate : candidates) {
            if (candidate != null) {
                return new ItemStack(candidate.get());
            }
        }
        return ItemStack.EMPTY;
    }
}
