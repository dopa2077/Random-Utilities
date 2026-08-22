package com.dopa.randomutilities;

import com.dopa.randomutilities.blockbreaker.AdvancedBlockBreakerNetwork;
import com.dopa.randomutilities.blockplacer.AdvancedBlockPlacerNetwork;
import com.dopa.randomutilities.compat.ModCompat;
import com.dopa.randomutilities.config.ModContentRegistration;
import com.dopa.randomutilities.filter.FilterNetwork;
import com.dopa.randomutilities.filter.NestedItemFilterBridge;
import com.dopa.randomutilities.filter.config.DevNullConfig;
import com.dopa.randomutilities.itemcollector.ItemCollectorNetwork;
import com.dopa.randomutilities.transfer.TransferNodeNetwork;
import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.generator.config.GeneratorRecipeConfig;
import com.dopa.randomutilities.machine.MachineNetwork;
import com.dopa.randomutilities.trashcan.TrashCanNetwork;
import com.dopa.randomutilities.redstoneclock.RedstoneClockNetwork;
import com.dopa.randomutilities.util.GhostItemFilter;
import com.dopa.randomutilities.fishnet.FishnetNetwork;
import com.dopa.randomutilities.fishnet.config.FishnetConfig;
import com.dopa.randomutilities.fishnet.config.TreasureLootConfig;
import com.dopa.randomutilities.lasso.config.LassoConfig;
import com.dopa.randomutilities.magnet.MagnetNetwork;
import com.dopa.randomutilities.magnet.config.MagnetConfig;
import com.dopa.randomutilities.registry.ModBlockEntities;
import com.dopa.randomutilities.registry.ModBlocks;
import com.dopa.randomutilities.registry.ModCreativeTabs;
import com.dopa.randomutilities.registry.ModDataComponents;
import com.dopa.randomutilities.registry.ModEntities;
import com.dopa.randomutilities.registry.ModItems;
import com.dopa.randomutilities.registry.ModMenus;
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
        // Load early so JEI (and other client plugins) see config values, not jar defaults only.
        TreasureLootConfig.load();
        modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                event.enqueueWork(() -> {
                    DevNullConfig.load();
                    UpgradeConfig.load();
                    FishnetConfig.load();
                    TreasureLootConfig.load();
                    LassoConfig.load();
                    MagnetConfig.load();
                    GeneratorRecipeConfig.load();
                }));
        modEventBus.addListener(FilterNetwork::registerCapabilities);
        modEventBus.addListener(ModSetup::registerCapabilities);
        modEventBus.addListener(FilterNetwork::registerPayloads);
        modEventBus.addListener(MachineNetwork::registerPayloads);
        modEventBus.addListener(ItemCollectorNetwork::registerPayloads);
        modEventBus.addListener(MagnetNetwork::registerPayloads);
        modEventBus.addListener(TransferNodeNetwork::registerPayloads);
        modEventBus.addListener(TrashCanNetwork::registerPayloads);
        modEventBus.addListener(RedstoneClockNetwork::registerPayloads);
        modEventBus.addListener(FishnetNetwork::registerPayloads);
        modEventBus.addListener(AdvancedBlockBreakerNetwork::registerPayloads);
        modEventBus.addListener(AdvancedBlockPlacerNetwork::registerPayloads);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
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
