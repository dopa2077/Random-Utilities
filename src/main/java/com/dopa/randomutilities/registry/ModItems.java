package com.dopa.randomutilities.registry;

import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorType;
import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.FilterItem;
import com.dopa.randomutilities.filter.FilterProfile;
import com.dopa.randomutilities.filter.FilterRegistry;
import com.dopa.randomutilities.filter.item.AdvancedDevNullItem;
import com.dopa.randomutilities.filter.item.DevNullItem;
import com.dopa.randomutilities.machine.item.MachineUpgradeItem;
import com.dopa.randomutilities.filter.dev.UiTestBlockItem;
import com.dopa.randomutilities.filter.dev.UiTestItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(dOPasRandomUtilities.MOD_ID);

    public static final DeferredItem<DevNullItem> DEV_NULL =
            registerFilter("dev_null", DevNullItem::new, DevNullConfig.basicProfile());

    public static final DeferredItem<AdvancedDevNullItem> ADVANCED_DEV_NULL =
            registerFilter("advanced_dev_null", AdvancedDevNullItem::new, DevNullConfig.advancedProfile());

    public static final DeferredItem<UiTestItem> UI_TEST_ITEM =
            registerFilter("ui_test_item", UiTestItem::new, DevNullConfig.uiTestProfile());

    public static final DeferredItem<UiTestBlockItem> UI_TEST_BLOCK_ITEM = ITEMS.registerItem(
            "ui_test_block",
            props -> new UiTestBlockItem(ModBlocks.UI_TEST_BLOCK.get(), props)
    );

    public static final DeferredItem<BlockItem> MINI_CHEST = ITEMS.registerSimpleBlockItem(ModBlocks.MINI_CHEST);

    public static final DeferredItem<BlockItem> TRASH_CAN = ITEMS.registerSimpleBlockItem(ModBlocks.TRASH_CAN);

    public static final DeferredItem<BlockItem> REDSTONE_CLOCK =
            ITEMS.registerSimpleBlockItem(ModBlocks.REDSTONE_CLOCK);

    public static final DeferredItem<BlockItem> BASIC_ITEM_COLLECTOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.BASIC_ITEM_COLLECTOR);

    public static final DeferredItem<BlockItem> ADVANCED_ITEM_COLLECTOR =
            ITEMS.registerSimpleBlockItem(ModBlocks.ADVANCED_ITEM_COLLECTOR);

    public static final DeferredItem<Item> UPGRADE_CASING = ITEMS.registerItem("upgrade_casing", Item::new);
    public static final DeferredItem<MachineUpgradeItem> PRODUCTIVITY_UPGRADE = ITEMS.registerItem(
            "productivity_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.PRODUCTIVITY)
    );
    public static final DeferredItem<MachineUpgradeItem> OVERCLOCK_UPGRADE = ITEMS.registerItem(
            "overclock_upgrade",
            props -> new MachineUpgradeItem(props, MachineUpgradeItem.Kind.OVERCLOCK)
    );

    private static final Map<GeneratorType, DeferredItem<BlockItem>> GENERATORS = new EnumMap<>(GeneratorType.class);

    static {
        for (GeneratorType type : GeneratorType.values()) {
            boolean creative = type == GeneratorType.CREATIVE_STONE
                    || type == GeneratorType.CREATIVE_RANDOM_ORE
                    || type == GeneratorType.CREATIVE_METAL_BLOCK;
            GENERATORS.put(
                    type,
                    creative
                            ? ITEMS.registerSimpleBlockItem(ModBlocks.forType(type), props -> props.rarity(Rarity.EPIC))
                            : ITEMS.registerSimpleBlockItem(ModBlocks.forType(type))
            );
        }
    }

    private static <T extends FilterItem> DeferredItem<T> registerFilter(
            String id,
            Function<Item.Properties, T> factory,
            FilterProfile profile
    ) {
        return ITEMS.registerItem(
                id,
                props -> {
                    T item = factory.apply(props);
                    FilterRegistry.register(item, profile);
                    return item;
                },
                props -> props.stacksTo(1).component(ModDataComponents.FILTER_CONTENTS.get(), profile.defaultContents())
        );
    }

    public static DeferredItem<BlockItem> forType(GeneratorType type) {
        return GENERATORS.get(type);
    }

    public static final DeferredItem<BlockItem> BASIC_STONE_GENERATOR = forType(GeneratorType.BASIC_STONE);
    public static final DeferredItem<BlockItem> INTERMEDIATE_STONE_GENERATOR = forType(GeneratorType.INTERMEDIATE_STONE);
    public static final DeferredItem<BlockItem> ADVANCED_STONE_GENERATOR = forType(GeneratorType.ADVANCED_STONE);
    public static final DeferredItem<BlockItem> ELITE_STONE_GENERATOR = forType(GeneratorType.ELITE_STONE);
    public static final DeferredItem<BlockItem> ULTIMATE_STONE_GENERATOR = forType(GeneratorType.ULTIMATE_STONE);
    public static final DeferredItem<BlockItem> CREATIVE_STONE_GENERATOR = forType(GeneratorType.CREATIVE_STONE);
    public static final DeferredItem<BlockItem> RANDOM_ORE_GENERATOR = forType(GeneratorType.RANDOM_ORE);
    public static final DeferredItem<BlockItem> METAL_BLOCK_GENERATOR = forType(GeneratorType.METAL_BLOCK);
    public static final DeferredItem<BlockItem> CREATIVE_RANDOM_ORE_GENERATOR = forType(GeneratorType.CREATIVE_RANDOM_ORE);
    public static final DeferredItem<BlockItem> CREATIVE_METAL_BLOCK_GENERATOR = forType(GeneratorType.CREATIVE_METAL_BLOCK);

    private ModItems() {}
}
