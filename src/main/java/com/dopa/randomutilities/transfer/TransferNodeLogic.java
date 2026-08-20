package com.dopa.randomutilities.transfer;

import com.dopa.randomutilities.machine.RedstoneMode;
import com.dopa.randomutilities.util.GhostItemFilter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

final class TransferNodeLogic {
    static final int STACK_TRANSFER_ITEMS = 64;
    private static final Direction[] FACINGS = Direction.values();

    private TransferNodeLogic() {}

    static void tick(ServerLevel level, BlockPos pos, BlockState state, TransferNodeBlockEntity be) {
        boolean anyHead = false;
        boolean needSignal = false;
        for (Direction facing : FACINGS) {
            if (!be.hasHead(facing)) {
                continue;
            }
            anyHead = true;
            TransferNodeBlockEntity.Head head = be.head(facing);
            if (head.backoff() <= 0 && head.redstoneMode() != RedstoneMode.IGNORE) {
                needSignal = true;
                break;
            }
        }
        if (!anyHead) {
            return;
        }
        int signal = needSignal ? level.getBestNeighborSignal(pos) : 0;
        for (Direction facing : FACINGS) {
            if (!be.hasHead(facing)) {
                continue;
            }
            tickHead(level, pos, state, be, facing, signal);
        }
    }

    private static void tickHead(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            TransferNodeBlockEntity be,
            Direction facing,
            int signal
    ) {
        TransferNodeBlockEntity.Head head = be.head(facing);
        if (head.backoff() > 0) {
            head.setBackoff(head.backoff() - 1);
            return;
        }

        if (!head.redstoneMode().allowsOperation(signal)) {
            return;
        }

        be.refreshNetwork(level, facing);
        List<TransferNetworks.Destination> destinations = head.destinations();
        if (destinations.isEmpty()) {
            head.setBackoff(TransferNetworks.BACKOFF_TICKS);
            return;
        }
        boolean moved = switch (head.kind()) {
            case ITEM -> tryTransferItems(level, pos, facing, destinations, be, head);
            case FLUID -> tryTransferFluids(level, pos, facing, destinations, be, head);
            case ENERGY -> tryTransferEnergy(level, pos, facing, destinations, be, head);
        };
        if (!moved) {
            if (head.kind() == HeadKind.ENERGY) {
                head.setLastEnergyPulled(0);
            }
            head.setBackoff(TransferNetworks.BACKOFF_TICKS);
        }
    }

    private static boolean tryTransferItems(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            List<TransferNetworks.Destination> destinations,
            TransferNodeBlockEntity be,
            TransferNodeBlockEntity.Head head
    ) {
        ResourceHandler<ItemResource> source = level.getCapability(
                Capabilities.Item.BLOCK,
                pos.relative(facing),
                facing.getOpposite()
        );
        if (source == null) {
            return false;
        }
        return tryTransfer(level, source, destinations, be, head);
    }

    private static boolean tryTransfer(
            ServerLevel level,
            ResourceHandler<ItemResource> source,
            List<TransferNetworks.Destination> destinations,
            TransferNodeBlockEntity be,
            TransferNodeBlockEntity.Head head
    ) {
        int budget = head.transferBudget();
        int moved = 0;
        ItemResource last = null;
        int size = source.size();
        for (int slot = 0; slot < size && moved < budget; slot++) {
            ItemResource resource = source.getResource(slot);
            if (resource.isEmpty()) {
                continue;
            }
            if (!GhostItemFilter.allows(resource, head.filterSlots(), head.whitelistMode())) {
                continue;
            }
            int thisMove = extractAndInsert(level, source, slot, resource, destinations, head, budget - moved);
            if (thisMove > 0) {
                moved += thisMove;
                last = resource;
            }
        }
        if (moved <= 0 || last == null) {
            return false;
        }
        head.setTransferredDisplay(last.toStack(1));
        be.noteTransfer();
        head.setBackoff(Math.max(0, head.transferInterval() - 1));
        return true;
    }

    private static int extractAndInsert(
            ServerLevel level,
            ResourceHandler<ItemResource> source,
            int slot,
            ItemResource resource,
            List<TransferNetworks.Destination> destinations,
            TransferNodeBlockEntity.Head head,
            int remaining
    ) {
        int moved = 0;
        int start = head.destCursor();
        int count = destinations.size();
        while (moved < remaining) {
            int available = source.getAmountAsInt(slot);
            if (available <= 0 || source.getResource(slot).isEmpty()) {
                break;
            }
            int want = Math.min(remaining - moved, available);
            int thisRound = 0;
            for (int offset = 0; offset < count; offset++) {
                int index = Math.floorMod(start + offset, count);
                ResourceHandler<ItemResource> handler = head.handlerAt(level, index);
                if (handler == null) {
                    continue;
                }
                int inserted = moveInto(source, slot, resource, want, handler);
                if (inserted > 0) {
                    start = Math.floorMod(index + 1, count);
                    head.setDestCursor(start);
                    moved += inserted;
                    thisRound = inserted;
                    break;
                }
            }
            if (thisRound <= 0) {
                break;
            }
        }
        return moved;
    }

    private static int moveInto(
            ResourceHandler<ItemResource> source,
            int slot,
            ItemResource resource,
            int want,
            ResourceHandler<ItemResource> dest
    ) {
        try (Transaction tx = Transaction.open(null)) {
            int canInsert;
            try (Transaction sim = Transaction.open(tx)) {
                canInsert = dest.insert(resource, want, sim);
            }
            if (canInsert <= 0) {
                return 0;
            }
            int extracted = source.extract(slot, resource, canInsert, tx);
            if (extracted <= 0) {
                return 0;
            }
            int inserted = dest.insert(resource, extracted, tx);
            if (inserted != extracted) {
                return 0;
            }
            tx.commit();
            return extracted;
        }
    }

    private static boolean tryTransferFluids(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            List<TransferNetworks.Destination> destinations,
            TransferNodeBlockEntity be,
            TransferNodeBlockEntity.Head head
    ) {
        ResourceHandler<FluidResource> source = level.getCapability(
                Capabilities.Fluid.BLOCK,
                pos.relative(facing),
                facing.getOpposite()
        );
        if (source == null) {
            return false;
        }
        int budget = head.fluidBudget();
        int moved = 0;
        FluidResource last = FluidResource.EMPTY;
        int size = source.size();
        for (int slot = 0; slot < size && moved < budget; slot++) {
            FluidResource resource = source.getResource(slot);
            if (resource.isEmpty() || !allowsFluid(resource, head)) {
                continue;
            }
            int thisMove = moveFluid(level, source, slot, resource, destinations, head, budget - moved);
            if (thisMove > 0) {
                moved += thisMove;
                last = resource;
            }
        }
        if (moved <= 0 || last.isEmpty()) {
            return false;
        }
        Item bucket = last.getFluid().getBucket();
        head.setTransferredDisplay(bucket == Items.AIR ? ItemStack.EMPTY : new ItemStack(bucket));
        be.noteTransfer();
        head.setBackoff(Math.max(0, head.transferInterval() - 1));
        return true;
    }

    private static boolean allowsFluid(FluidResource resource, TransferNodeBlockEntity.Head head) {
        Item bucket = resource.getFluid().getBucket();
        if (bucket == Items.AIR) {
            return allFiltersEmpty(head);
        }
        return GhostItemFilter.allows(ItemResource.of(bucket), head.filterSlots(), head.whitelistMode());
    }

    private static boolean allFiltersEmpty(TransferNodeBlockEntity.Head head) {
        for (ItemStack slot : head.filterSlots()) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int moveFluid(
            ServerLevel level,
            ResourceHandler<FluidResource> source,
            int slot,
            FluidResource resource,
            List<TransferNetworks.Destination> destinations,
            TransferNodeBlockEntity.Head head,
            int remaining
    ) {
        int moved = 0;
        int start = head.destCursor();
        int count = destinations.size();
        while (moved < remaining) {
            int available = source.getAmountAsInt(slot);
            if (available <= 0 || source.getResource(slot).isEmpty()) {
                break;
            }
            int want = Math.min(remaining - moved, available);
            int thisRound = 0;
            for (int offset = 0; offset < count; offset++) {
                int index = Math.floorMod(start + offset, count);
                TransferNetworks.Destination dest = destinations.get(index);
                ResourceHandler<FluidResource> handler = level.getCapability(
                        Capabilities.Fluid.BLOCK,
                        dest.inventoryPos(),
                        dest.insertFace()
                );
                if (handler == null) {
                    continue;
                }
                int inserted = moveFluidInto(source, slot, resource, want, handler);
                if (inserted > 0) {
                    start = Math.floorMod(index + 1, count);
                    head.setDestCursor(start);
                    moved += inserted;
                    thisRound = inserted;
                    break;
                }
            }
            if (thisRound <= 0) {
                break;
            }
        }
        return moved;
    }

    private static int moveFluidInto(
            ResourceHandler<FluidResource> source,
            int slot,
            FluidResource resource,
            int want,
            ResourceHandler<FluidResource> dest
    ) {
        try (Transaction tx = Transaction.open(null)) {
            int canInsert;
            try (Transaction sim = Transaction.open(tx)) {
                canInsert = dest.insert(resource, want, sim);
            }
            if (canInsert <= 0) {
                return 0;
            }
            int extracted = source.extract(slot, resource, canInsert, tx);
            if (extracted <= 0) {
                return 0;
            }
            int inserted = dest.insert(resource, extracted, tx);
            if (inserted != extracted) {
                return 0;
            }
            tx.commit();
            return extracted;
        }
    }

    private static boolean tryTransferEnergy(
            ServerLevel level,
            BlockPos pos,
            Direction facing,
            List<TransferNetworks.Destination> destinations,
            TransferNodeBlockEntity be,
            TransferNodeBlockEntity.Head head
    ) {
        EnergyHandler source = level.getCapability(
                Capabilities.Energy.BLOCK,
                pos.relative(facing),
                facing.getOpposite()
        );
        if (source == null) {
            return false;
        }
        int budget = head.energyBudget();
        int start = head.destCursor();
        int count = destinations.size();
        for (int offset = 0; offset < count; offset++) {
            int index = Math.floorMod(start + offset, count);
            TransferNetworks.Destination dest = destinations.get(index);
            EnergyHandler handler = level.getCapability(
                    Capabilities.Energy.BLOCK,
                    dest.inventoryPos(),
                    dest.insertFace()
            );
            if (handler == null) {
                continue;
            }
            int moved = moveEnergy(source, handler, budget);
            if (moved > 0) {
                head.setDestCursor(Math.floorMod(index + 1, count));
                head.setLastEnergyPulled(moved);
                be.noteTransfer();
                head.setBackoff(Math.max(0, head.transferInterval() - 1));
                return true;
            }
        }
        head.setLastEnergyPulled(0);
        return false;
    }

    private static int moveEnergy(EnergyHandler source, EnergyHandler dest, int want) {
        try (Transaction tx = Transaction.open(null)) {
            int canInsert;
            try (Transaction sim = Transaction.open(tx)) {
                canInsert = dest.insert(want, sim);
            }
            if (canInsert <= 0) {
                return 0;
            }
            int extracted = source.extract(canInsert, tx);
            if (extracted <= 0) {
                return 0;
            }
            int inserted = dest.insert(extracted, tx);
            if (inserted != extracted) {
                return 0;
            }
            tx.commit();
            return extracted;
        }
    }
}
