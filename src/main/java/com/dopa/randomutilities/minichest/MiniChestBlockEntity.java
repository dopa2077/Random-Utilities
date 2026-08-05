package com.dopa.randomutilities.minichest;

import com.dopa.randomutilities.minichest.MiniChestMenu;
import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;

import java.util.List;

public class MiniChestBlockEntity extends BlockEntity implements LidBlockEntity, Container {
    public static final int SLOT_COUNT = 1;
    /** Higher than vanilla chest (~0.9–1.0) so the mini chest sounds higher-pitched. */
    public static final float SOUND_PITCH = 1.6F;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(items) {
        @Override
        protected void onContentsChanged(int index, ItemStack previousContents) {
            setChanged();
        }
    };
    private final ChestLidController chestLidController = new ChestLidController();
    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playChestSound(level, pos, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playChestSound(level, pos, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
            level.blockEvent(pos, state.getBlock(), 1, current);
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof MiniChestMenu menu && menu.getContainer() == MiniChestBlockEntity.this;
        }
    };

    public MiniChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MINI_CHEST.get(), pos, state);
    }

    public ItemStacksResourceHandler itemHandler() {
        return itemHandler;
    }

    public Component getDisplayName() {
        return Component.translatable("container.dopasrandomutilities.mini_chest");
    }

    public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, MiniChestBlockEntity entity) {
        entity.chestLidController.tickLid();
    }

    private static void playChestSound(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent sound) {
        level.playSound(
                null,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                sound,
                SoundSource.BLOCKS,
                0.5F,
                SOUND_PITCH + level.getRandom().nextFloat() * 0.1F
        );
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            this.chestLidController.shouldBeOpen(param > 0);
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    public void startOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.incrementOpeners(
                    containerUser.getLivingEntity(),
                    this.getLevel(),
                    this.getBlockPos(),
                    this.getBlockState(),
                    containerUser.getContainerInteractionRange()
            );
        }
    }

    @Override
    public void stopOpen(ContainerUser containerUser) {
        if (!this.remove && !containerUser.getLivingEntity().isSpectator()) {
            this.openersCounter.decrementOpeners(
                    containerUser.getLivingEntity(),
                    this.getLevel(),
                    this.getBlockPos(),
                    this.getBlockState()
            );
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.openersCounter.getEntitiesWithContainerOpen(this.getLevel(), this.getBlockPos());
    }

    public void recheckOpen() {
        if (!this.remove) {
            this.openersCounter.recheckOpeners(this.getLevel(), this.getBlockPos(), this.getBlockState());
        }
    }

    @Override
    public float getOpenNess(float partialTick) {
        return this.chestLidController.getOpenness(partialTick);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items.clear();
        ContainerHelper.loadAllItems(input, this.items);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = ContainerHelper.removeItem(this.items, slot, amount);
        if (!stack.isEmpty()) {
            this.setChanged();
        }
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
