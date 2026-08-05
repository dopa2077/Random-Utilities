package com.dopa.randomutilities.filter.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.filter.FilterProfile;
import com.dopa.randomutilities.filter.FilterRegistry;
import com.dopa.randomutilities.filter.network.FilterSelectPayload;
import com.dopa.randomutilities.registry.ModMenus;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = dOPasRandomUtilities.MOD_ID, value = Dist.CLIENT)
public final class FilterClient {
    private FilterClient() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FILTER.get(), FilterScreen::new);
    }

    @SubscribeEvent
    public static void registerItemTints(RegisterColorHandlersEvent.ItemTintSources event) {
        event.register(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "filter_color"),
                FilterTintSource.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(
                Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "filter_preview"),
                FilterPreviewModel.Unbaked.MAP_CODEC
        );
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null) {
            return;
        }
        ItemStack held = findCyclingStack(minecraft);
        if (held == null) {
            return;
        }
        if (minecraft.player.isShiftKeyDown()) {
            ClientPacketDistributor.sendToServer(new FilterSelectPayload(
                    FilterSelectPayload.MODE_CYCLE_NEXT,
                    Identifier.withDefaultNamespace("air")
            ));
            event.setCanceled(true);
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            BlockState state = minecraft.level.getBlockState(blockHit.getBlockPos());
            Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            if (id != null) {
                ClientPacketDistributor.sendToServer(new FilterSelectPayload(FilterSelectPayload.MODE_MATCH_BLOCK, id));
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() != null
                || !minecraft.player.isShiftKeyDown()) {
            return;
        }
        if (findCyclingStack(minecraft) == null) {
            return;
        }
        double delta = event.getScrollDeltaY();
        if (delta == 0.0D) {
            return;
        }
        byte mode = delta > 0.0D ? FilterSelectPayload.MODE_CYCLE_NEXT : FilterSelectPayload.MODE_CYCLE_PREV;
        ClientPacketDistributor.sendToServer(new FilterSelectPayload(mode, Identifier.withDefaultNamespace("air")));
        event.setCanceled(true);
    }

    private static ItemStack findCyclingStack(Minecraft minecraft) {
        ItemStack main = minecraft.player.getMainHandItem();
        FilterProfile profile = FilterRegistry.profile(main);
        if (profile != null && profile.slotCycling()) {
            return main;
        }
        ItemStack off = minecraft.player.getOffhandItem();
        profile = FilterRegistry.profile(off);
        return profile != null && profile.slotCycling() ? off : null;
    }
}
