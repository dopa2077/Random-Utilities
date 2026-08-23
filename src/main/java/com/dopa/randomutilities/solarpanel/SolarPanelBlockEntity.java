package com.dopa.randomutilities.solarpanel;

import com.dopa.randomutilities.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** Ownership link from a solar panel to at most one controller. No ticker / UI. */
public class SolarPanelBlockEntity extends BlockEntity {
    @Nullable
    private BlockPos controllerPos;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL.get(), pos, state);
    }

    @Nullable
    public BlockPos controllerPos() {
        return controllerPos;
    }

    public boolean isOwnedBy(BlockPos controller) {
        return controllerPos != null && controllerPos.equals(controller);
    }

    /**
     * True if unowned, owned by {@code controller}, or the stored owner is stale
     * (no controller BE at that position anymore).
     */
    public boolean isFreeOrOwnedBy(Level level, BlockPos controller) {
        if (controllerPos == null || controllerPos.equals(controller)) {
            return true;
        }
        return !(level.getBlockEntity(controllerPos) instanceof SolarPanelControllerBlockEntity);
    }

    public void claim(BlockPos controller) {
        if (controllerPos != null && controllerPos.equals(controller)) {
            return;
        }
        controllerPos = controller.immutable();
        setChanged();
    }

    public void clearOwner() {
        if (controllerPos == null) {
            return;
        }
        controllerPos = null;
        setChanged();
    }

    /** Clears ownership only if this panel still points at {@code controller}. */
    public void clearOwnerIf(BlockPos controller) {
        if (controllerPos != null && controllerPos.equals(controller)) {
            clearOwner();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        controllerPos = input.read("Controller", BlockPos.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (controllerPos != null) {
            output.store("Controller", BlockPos.CODEC, controllerPos);
        }
    }
}
