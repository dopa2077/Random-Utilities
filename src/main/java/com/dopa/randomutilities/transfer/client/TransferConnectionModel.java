package com.dopa.randomutilities.transfer.client;

import com.dopa.randomutilities.dOPasRandomUtilities;
import com.dopa.randomutilities.transfer.HeadKind;
import com.dopa.randomutilities.transfer.TransferNodeBlock;
import com.dopa.randomutilities.transfer.TransferNodeBlockEntity;
import com.dopa.randomutilities.transfer.TransferNodeFace;
import com.dopa.randomutilities.transfer.TransferPipeBlock;
import com.dopa.randomutilities.transfer.TransferPipeFace;
import com.mojang.math.Quadrant;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockModelRotation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import net.neoforged.neoforge.client.model.block.CustomUnbakedBlockStateModel;
import net.neoforged.neoforge.model.data.ModelData;

import java.util.List;

/**
 * One shared baked model for all pipe/node states. Arms, nozzles, and head plates
 * are chosen at mesh time from blockstate + neighbor/model data.
 */
public final class TransferConnectionModel implements DynamicBlockStateModel {
    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "transfer_connections");

    private static final Identifier CENTER =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_center");
    private static final Identifier ARM =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_arm");
    private static final Identifier ARM_SHORT =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_arm_short");
    private static final Identifier NOZZLE =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_nozzle");
    private static final Identifier PLATE =
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_node");
    private static final Identifier[] DOTS = {
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_node_item"),
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_node_fluid"),
            Identifier.fromNamespaceAndPath(dOPasRandomUtilities.MOD_ID, "block/transfer_node_energy")
    };

    private final BlockStateModelPart center;
    private final BlockStateModelPart[] arms;
    private final BlockStateModelPart[] shortArms;
    private final BlockStateModelPart[] nozzles;
    private final BlockStateModelPart[] plates;
    private final BlockStateModelPart[][] dots;

    private TransferConnectionModel(
            BlockStateModelPart center,
            BlockStateModelPart[] arms,
            BlockStateModelPart[] shortArms,
            BlockStateModelPart[] nozzles,
            BlockStateModelPart[] plates,
            BlockStateModelPart[][] dots
    ) {
        this.center = center;
        this.arms = arms;
        this.shortArms = shortArms;
        this.nozzles = nozzles;
        this.plates = plates;
        this.dots = dots;
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> parts
    ) {
        if (state.getBlock() instanceof TransferPipeBlock) {
            collectPipe(level, pos, state, parts);
            return;
        }
        if (state.getBlock() instanceof TransferNodeBlock) {
            collectNode(level, pos, state, parts);
        }
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> parts) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.hitResult instanceof BlockHitResult hit)) {
            return;
        }
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        collectParts(BlockAndTintGetter.EMPTY, hit.getBlockPos(), state, random, parts);
    }

    private void collectPipe(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            List<BlockStateModelPart> parts
    ) {
        parts.add(center);
        for (Direction direction : Direction.values()) {
            TransferPipeFace face = TransferPipeBlock.face(state, direction);
            if (!face.hasArm()) {
                continue;
            }
            boolean shortened = face.shortened() || towardHead(level, pos, direction);
            parts.add(shortened ? shortArms[direction.ordinal()] : arms[direction.ordinal()]);
            if (face == TransferPipeFace.INVENTORY) {
                parts.add(nozzles[direction.ordinal()]);
            }
        }
    }

    private void collectNode(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            List<BlockStateModelPart> parts
    ) {
        Direction visualFace = TransferNodeBlock.particleVisualFace(state);
        int kinds = kindMask(level, pos);
        if (visualFace != null) {
            addPlate(parts, kinds, visualFace);
            return;
        }
        int heads = headMask(level, pos);
        boolean hasPipe = state.getValue(TransferNodeBlock.HAS_PIPE);
        if (hasPipe) {
            parts.add(center);
        }
        for (Direction direction : Direction.values()) {
            boolean headed = TransferNodeBlockEntity.hasHead(heads, direction);
            if (headed) {
                addPlate(parts, kinds, direction);
            }
            TransferNodeFace face = TransferNodeBlock.face(state, direction);
            if (hasPipe && headed) {
                parts.add(shortArms[direction.ordinal()]);
            } else if (hasPipe && face.hasArm()) {
                boolean shortened = face == TransferNodeFace.INVENTORY;
                parts.add(shortened ? shortArms[direction.ordinal()] : arms[direction.ordinal()]);
                if (face == TransferNodeFace.INVENTORY) {
                    parts.add(nozzles[direction.ordinal()]);
                }
            }
        }
    }

    private static int headMask(BlockAndTintGetter level, BlockPos pos) {
        Integer mask = level.getModelData(pos).get(TransferNodeBlockEntity.HEADS);
        if (mask != null) {
            return mask;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.isSameThread()) {
            return 0;
        }
        BlockPos query = pos;
        if (level == BlockAndTintGetter.EMPTY && mc.hitResult instanceof BlockHitResult hit) {
            query = hit.getBlockPos();
        }
        if (mc.level.getBlockEntity(query) instanceof TransferNodeBlockEntity be) {
            return be.headMask();
        }
        return 0;
    }

    private void addPlate(List<BlockStateModelPart> parts, int kinds, Direction direction) {
        parts.add(plates[direction.ordinal()]);
        int ordinal = HeadKind.byOrdinal((kinds >> (direction.ordinal() * 2)) & 3).ordinal();
        parts.add(dots[ordinal][direction.ordinal()]);
    }

    private static int kindMask(BlockAndTintGetter level, BlockPos pos) {
        Integer packed = level.getModelData(pos).get(TransferNodeBlockEntity.KINDS);
        if (packed != null) {
            return packed;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.isSameThread()) {
            return 0;
        }
        BlockPos query = pos;
        if (level == BlockAndTintGetter.EMPTY && mc.hitResult instanceof BlockHitResult hit) {
            query = hit.getBlockPos();
        }
        if (mc.level.getBlockEntity(query) instanceof TransferNodeBlockEntity be) {
            return be.kindMask();
        }
        return 0;
    }

    private static boolean towardHead(BlockAndTintGetter level, BlockPos pos, Direction direction) {
        ModelData data = level.getModelData(pos.relative(direction));
        Integer mask = data.get(TransferNodeBlockEntity.HEADS);
        return mask != null && TransferNodeBlockEntity.hasHead(mask, direction.getOpposite());
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        int heads = 0;
        int kinds = 0;
        int shorts = 0;
        int channel = TransferPipeBlock.channel(state).ordinal();
        if (state.getBlock() instanceof TransferNodeBlock) {
            heads = headMask(level, pos);
            kinds = kindMask(level, pos);
            Integer packed = level.getModelData(pos).get(TransferNodeBlockEntity.PIPE_CHANNEL);
            if (packed != null) {
                channel = packed;
            }
        } else {
            for (Direction direction : Direction.values()) {
                if (TransferPipeBlock.face(state, direction) == TransferPipeFace.PIPE
                        && towardHead(level, pos, direction)) {
                    shorts |= 1 << direction.ordinal();
                }
            }
        }
        return new GeometryKey(state, heads, kinds, shorts, channel);
    }

    @Override
    public Material.Baked particleMaterial() {
        return center.particleMaterial();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return center.particleMaterial();
    }

    @Override
    public int materialFlags() {
        return center.materialFlags();
    }

    private record GeometryKey(BlockState state, int heads, int kinds, int shorts, int channel) {}

    public record Unbaked() implements CustomUnbakedBlockStateModel {
        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<? extends CustomUnbakedBlockStateModel> codec() {
            return CODEC;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(CENTER);
            resolver.markDependency(ARM);
            resolver.markDependency(ARM_SHORT);
            resolver.markDependency(NOZZLE);
            resolver.markDependency(PLATE);
            for (Identifier overlay : DOTS) {
                resolver.markDependency(overlay);
            }
        }

        @Override
        public BlockStateModel bake(ModelBaker baker) {
            int[][] rotations = plateRotations();
            BlockStateModelPart[][] dots = new BlockStateModelPart[DOTS.length][];
            for (int i = 0; i < DOTS.length; i++) {
                dots[i] = bakeFacing(baker, DOTS[i], rotations);
            }
            return new TransferConnectionModel(
                    bakePart(baker, CENTER, 0, 0),
                    bakeFacing(baker, ARM, armRotations()),
                    bakeFacing(baker, ARM_SHORT, armRotations()),
                    bakeFacing(baker, NOZZLE, armRotations()),
                    bakeFacing(baker, PLATE, rotations),
                    dots
            );
        }
    }

    private static int[][] armRotations() {
        int[][] rot = new int[6][2];
        rot[Direction.SOUTH.ordinal()] = new int[] {0, 0};
        rot[Direction.NORTH.ordinal()] = new int[] {0, 180};
        rot[Direction.WEST.ordinal()] = new int[] {0, 90};
        rot[Direction.EAST.ordinal()] = new int[] {0, 270};
        rot[Direction.UP.ordinal()] = new int[] {90, 0};
        rot[Direction.DOWN.ordinal()] = new int[] {270, 0};
        return rot;
    }

    private static int[][] plateRotations() {
        int[][] rot = new int[6][2];
        rot[Direction.DOWN.ordinal()] = new int[] {0, 0};
        rot[Direction.UP.ordinal()] = new int[] {180, 0};
        rot[Direction.NORTH.ordinal()] = new int[] {270, 0};
        rot[Direction.SOUTH.ordinal()] = new int[] {90, 0};
        rot[Direction.WEST.ordinal()] = new int[] {270, 270};
        rot[Direction.EAST.ordinal()] = new int[] {270, 90};
        return rot;
    }

    private static BlockStateModelPart[] bakeFacing(ModelBaker baker, Identifier model, int[][] rotations) {
        BlockStateModelPart[] parts = new BlockStateModelPart[6];
        for (Direction direction : Direction.values()) {
            int[] xy = rotations[direction.ordinal()];
            parts[direction.ordinal()] = bakePart(baker, model, xy[0], xy[1]);
        }
        return parts;
    }

    private static BlockStateModelPart bakePart(ModelBaker baker, Identifier model, int xRot, int yRot) {
        return SimpleModelWrapper.bake(baker, model, BlockModelRotation.get(Quadrant.fromXYAngles(
                quadrant(xRot),
                quadrant(yRot)
        )));
    }

    private static Quadrant quadrant(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 90 -> Quadrant.R90;
            case 180 -> Quadrant.R180;
            case 270 -> Quadrant.R270;
            default -> Quadrant.R0;
        };
    }
}
