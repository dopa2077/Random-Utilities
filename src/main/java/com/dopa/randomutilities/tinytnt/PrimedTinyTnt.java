package com.dopa.randomutilities.tinytnt;

import com.dopa.randomutilities.registry.ModEntities;
import com.dopa.randomutilities.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public class PrimedTinyTnt extends Entity implements TraceableEntity {
    private static final EntityDataAccessor<Integer> DATA_FUSE_ID =
            SynchedEntityData.defineId(PrimedTinyTnt.class, EntityDataSerializers.INT);
    public static final int DEFAULT_FUSE_TIME = 80;
    /** Small boom: knockback/particles stay tiny. Block damage is capped to touching blocks. */
    public static final float EXPLOSION_POWER = 0.8F;
    private static final ExplosionDamageCalculator TINY_BLAST = new ExplosionDamageCalculator() {
        private static final double MAX_BLOCK_DISTANCE = 1.2;
        private static final float SOFT_RESISTANCE_CAP = 0.4F;
        private static final float HARD_BLOCK_RESISTANCE = 3.5F;

        @Override
        public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power) {
            return Vec3.atCenterOf(pos).distanceToSqr(explosion.center()) <= MAX_BLOCK_DISTANCE * MAX_BLOCK_DISTANCE;
        }

        @Override
        public Optional<Float> getBlockExplosionResistance(
                Explosion explosion,
                BlockGetter level,
                BlockPos pos,
                BlockState block,
                FluidState fluid
        ) {
            Optional<Float> vanilla = super.getBlockExplosionResistance(explosion, level, pos, block, fluid);
            if (vanilla.isEmpty() || vanilla.get() >= HARD_BLOCK_RESISTANCE) {
                return vanilla;
            }
            return Optional.of(Math.min(vanilla.get(), SOFT_RESISTANCE_CAP));
        }
    };
    private static final WeightedList<ExplosionParticleInfo> BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
            .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
            .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
            .build();

    private @Nullable EntityReference<LivingEntity> owner;

    public PrimedTinyTnt(EntityType<PrimedTinyTnt> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public PrimedTinyTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(ModEntities.PRIMED_TINY_TNT.get(), level);
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.15F, -Math.cos(rot) * 0.02);
        this.setFuse(DEFAULT_FUSE_TIME);
        this.xo = x;
        this.yo = y;
        this.zo = z;
        this.owner = EntityReference.of(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
        entityData.define(DATA_FUSE_ID, DEFAULT_FUSE_TIME);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04;
    }

    @Override
    public void tick() {
        this.handlePortal();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.setDeltaMovement(this.getDeltaMovement().scale(this.getAirDrag()));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }

        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (!this.level().isClientSide()) {
                this.explode();
            }
        } else {
            this.updateFluidInteraction();
            if (this.level().isClientSide()) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.25, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    protected void explode() {
        if (!(this.level() instanceof ServerLevel server) || !server.getGameRules().get(GameRules.TNT_EXPLODES)) {
            return;
        }
        server.explode(
                this,
                Explosion.getDefaultDamageSource(server, this),
                TINY_BLAST,
                this.getX(),
                this.getY(0.0625),
                this.getZ(),
                EXPLOSION_POWER,
                false,
                Level.ExplosionInteraction.TNT,
                ParticleTypes.EXPLOSION,
                ParticleTypes.EXPLOSION,
                BLOCK_PARTICLES,
                ModSounds.TINY_TNT_EXPLODE
        );
    }

    /** Always 1 chip; extra chips at 50%, 25%, and 5% (max 4). */
    public static int rollWoodChips(RandomSource random) {
        int chips = 1;
        if (random.nextFloat() < 0.50F) {
            chips++;
        }
        if (random.nextFloat() < 0.25F) {
            chips++;
        }
        if (random.nextFloat() < 0.05F) {
            chips++;
        }
        return chips;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putShort("fuse", (short) this.getFuse());
        EntityReference.store(this.owner, output, "owner");
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setFuse(input.getShortOr("fuse", (short) DEFAULT_FUSE_TIME));
        this.owner = EntityReference.read(input, "owner");
    }

    @Override
    public @Nullable LivingEntity getOwner() {
        return EntityReference.getLivingEntity(this.owner, this.level());
    }

    @Override
    public void restoreFrom(Entity oldEntity) {
        super.restoreFrom(oldEntity);
        if (oldEntity instanceof PrimedTinyTnt primed) {
            this.owner = primed.owner;
        }
    }

    public void setFuse(int time) {
        this.entityData.set(DATA_FUSE_ID, time);
    }

    public int getFuse() {
        return this.entityData.get(DATA_FUSE_ID);
    }

    public static int getRandomShortFuse(int fuse, RandomSource random) {
        return random.nextInt(Math.max(1, fuse / 4)) + fuse / 8;
    }

    @Override
    public final boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}
