package com.dopa.randomutilities;

import com.dopa.randomutilities.integration.ModCompat;
import com.dopa.randomutilities.config.ConfigPack;
import com.dopa.randomutilities.config.ModContentRegistration;
import com.dopa.randomutilities.core.filter.FilterItemHandler;
import com.dopa.randomutilities.core.filter.FilterRegistry;
import com.dopa.randomutilities.core.filter.NestedItemFilterBridge;
import com.dopa.randomutilities.core.filter.config.DevNullConfig;
import com.dopa.randomutilities.logistics.transfer.config.TransferNodeConfig;
import com.dopa.randomutilities.core.machine.config.PoweredMachinesConfig;
import com.dopa.randomutilities.core.machine.config.UpgradeConfig;
import com.dopa.randomutilities.machine.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.core.util.GhostItemFilter;
import com.dopa.randomutilities.machine.fishnet.config.FishnetConfig;
import com.dopa.randomutilities.machine.fishnet.config.TreasureLootConfig;
import com.dopa.randomutilities.item.lasso.config.LassoConfig;
import com.dopa.randomutilities.item.magnet.config.MagnetConfig;
import com.dopa.randomutilities.machine.combustion.config.CombustionGeneratorConfig;
import com.dopa.randomutilities.machine.solar.panel.config.SolarPanelConfig;
import com.dopa.randomutilities.machine.solar.furnace.config.SolarFurnaceConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModCreativeTabs;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.dopa.randomutilities.registry.ModEntities;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.registry.ModMenus;
import com.dopa.randomutilities.registry.ModNetwork;
import com.dopa.randomutilities.registry.ModSounds;
import com.dopa.randomutilities.registry.ModTriggers;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ModSetup {
    private static final Identifier GENERATOR_RECIPES_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":generator_recipes");
    private static final Identifier DEV_NULL_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":devnull_config");
    private static final Identifier UPGRADE_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":upgrade_config");
    private static final Identifier TRANSFER_NODE_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":transfer_node_config");
    private static final Identifier POWERED_MACHINES_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":powered_machines_config");
    private static final Identifier SOLAR_FURNACE_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":solar_furnace_config");
    private static final Identifier COMBUSTION_GENERATOR_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":combustion_generator_config");
    private static final Identifier SOLAR_PANEL_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":solar_panel_config");
    private static final Identifier FISHNET_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":fishnet_config");
    private static final Identifier TREASURE_LOOT_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":treasure_loot_config");
    private static final Identifier LASSO_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":lasso_config");
    private static final Identifier MAGNET_CONFIG_LISTENER =
            Identifier.parse(dOPasRandomUtilities.MOD_ID + ":magnet_config");

    private ModSetup() {}

    public static void register(IEventBus modEventBus) {
        ModContentRegistration.ensureRegistered();
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModTriggers.TRIGGER_TYPES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(ModCompat::onInterModEnqueue);
        GhostItemFilter.setNestedItemFilter(NestedItemFilterBridge.INSTANCE);
        ConfigPack.ensureRoot();
        // Load early so JEI (and other client plugins) see config values, not jar defaults only.
        TreasureLootConfig.load();
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    ConfigPack.ensureRoot();
                    DevNullConfig.load();
                    PoweredMachinesConfig.load();
                    TransferNodeConfig.load();
                    SolarFurnaceConfig.load();
                    CombustionGeneratorConfig.load();
                    SolarPanelConfig.load();
                    UpgradeConfig.load();
                    FishnetConfig.load();
                    TreasureLootConfig.load();
                    LassoConfig.load();
                    MagnetConfig.load();
                    GeneratorRecipeConfig.load();
                }));
        modEventBus.addListener(ModSetup::registerCapabilities);
        modEventBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        var filterItems = FilterRegistry.allItems();
        if (filterItems.length > 0) {
            event.registerItem(
                    Capabilities.Item.ITEM,
                    (stack, access) -> new FilterItemHandler(access),
                    filterItems
            );
        }
        registerIfPresent(ModBlockEntities.MINI_CHEST, type ->
                event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.itemHandler()));
        registerIfPresent(ModBlockEntities.TRASH_CAN, type ->
                event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.itemHandler()));
        registerIfPresent(ModBlockEntities.SOLAR_FURNACE, type ->
                event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.itemHandler(side)));
        registerIfPresent(ModBlockEntities.FISHNET, type ->
                event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.itemHandler(side)));
        registerIfPresent(ModBlockEntities.SIMPLE_BLOCK_PLACER, type ->
                event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.itemHandler()));
        registerIfPresent(ModBlockEntities.ADVANCED_BLOCK_PLACER, type -> {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.itemHandler());
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> be.energy());
        });
        registerIfPresent(ModBlockEntities.ADVANCED_BLOCK_BREAKER, type -> {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.pickaxeHandler());
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> be.energy());
        });
        registerIfPresent(ModBlockEntities.COMBUSTION_GENERATOR, type -> {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> be.items());
            event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> be.energy());
        });
        registerIfPresent(ModBlockEntities.SOLAR_PANEL_CONTROLLER, type ->
                event.registerBlockEntity(Capabilities.Energy.BLOCK, type, (be, side) -> be.energy()));
    }

    private static <T extends BlockEntity> void registerIfPresent(
            @Nullable Supplier<BlockEntityType<T>> type,
            Consumer<BlockEntityType<T>> action
    ) {
        if (type != null) {
            action.accept(type.get());
        }
    }

    @EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID)
    public static final class Events {
        private Events() {}

        @SubscribeEvent
        public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
            event.addListener(
                    GENERATOR_RECIPES_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> {
                        GeneratorRecipeConfig.reload();
                        GeneratorRecipeConfig.rebuildBlockPools();
                    }
            );
            event.addListener(
                    DEV_NULL_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> DevNullConfig.reload()
            );
            event.addListener(
                    UPGRADE_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> UpgradeConfig.reload()
            );
            event.addListener(
                    TRANSFER_NODE_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> TransferNodeConfig.reload()
            );
            event.addListener(
                    POWERED_MACHINES_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> PoweredMachinesConfig.reload()
            );
            event.addListener(
                    SOLAR_FURNACE_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> SolarFurnaceConfig.reload()
            );
            event.addListener(
                    COMBUSTION_GENERATOR_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> CombustionGeneratorConfig.reload()
            );
            event.addListener(
                    SOLAR_PANEL_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> SolarPanelConfig.reload()
            );
            event.addListener(
                    FISHNET_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> FishnetConfig.reload()
            );
            event.addListener(
                    TREASURE_LOOT_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> TreasureLootConfig.reload()
            );
            event.addListener(
                    LASSO_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> LassoConfig.reload()
            );
            event.addListener(
                    MAGNET_CONFIG_LISTENER,
                    (ResourceManagerReloadListener) resourceManager -> MagnetConfig.reload()
            );
        }

        @SubscribeEvent
        public static void onServerStarting(ServerStartingEvent event) {
            GeneratorRecipeConfig.rebuildBlockPools();
        }
    }
}
