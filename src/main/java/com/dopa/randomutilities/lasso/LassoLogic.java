package com.dopa.randomutilities.lasso;

import com.dopa.randomutilities.lasso.config.LassoConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public final class LassoLogic {
    private static final float BASIC_MAX_HEALTH = 18.0F;
    private static final float BOSS_CAPTURE_HEALTH_FRACTION = 0.05F;
    private static final float CURSED_NORMAL_COST = 2.0F;
    private static final float CURSED_BOSS_COST = 8.0F;

    private static final Identifier ENDER_DRAGON_ID = Identifier.withDefaultNamespace("ender_dragon");
    private static final Identifier WITHER_ID = Identifier.withDefaultNamespace("wither");
    private static final Identifier WARDEN_ID = Identifier.withDefaultNamespace("warden");

    private LassoLogic() {}

    public enum Failure {
        ALREADY_FULL,
        EMPTY,
        INVALID_TARGET,
        TOO_WEAK,
        BOSS_TOO_HEALTHY,
        CONFIG_DENIED,
        NEED_DURABILITY_DEPLOY,
        NEED_XP_CAPTURE,
        NEED_XP_DEPLOY,
        NEED_HEALTH,
        SPAWN_BLOCKED;

        boolean playsDenial() {
            return switch (this) {
                case ALREADY_FULL,
                        TOO_WEAK,
                        BOSS_TOO_HEALTHY,
                        CONFIG_DENIED,
                        NEED_DURABILITY_DEPLOY,
                        NEED_XP_CAPTURE,
                        NEED_XP_DEPLOY,
                        NEED_HEALTH -> true;
                case EMPTY, INVALID_TARGET, SPAWN_BLOCKED -> false;
            };
        }
    }

    public static Optional<Component> tryCapture(
            LassoTier tier,
            ItemStack stack,
            ServerPlayer player,
            LivingEntity target,
            InteractionHand hand
    ) {
        ServerLevel level = (ServerLevel) player.level();
        if (LassoCapture.has(stack)) {
            return fail(level, player, Failure.ALREADY_FULL);
        }
        Failure validation = validateCaptureTarget(tier, target);
        if (validation != null) {
            return fail(level, player, validation);
        }
        if (!LassoConfig.allows(tier, target.getType())) {
            return fail(level, player, Failure.CONFIG_DENIED);
        }
        if (!player.hasInfiniteMaterials() && !canAffordCapture(tier, stack, player, target)) {
            return fail(level, player, captureCostFailure(tier, stack, player, target));
        }

        LassoCapture capture = createCapture(player.registryAccess(), target);
        if (capture == null) {
            return fail(level, player, Failure.INVALID_TARGET);
        }
        BlockPos blockPos = target.blockPosition();
        Vec3 feet = target.position();
        LassoCapture.set(stack, capture);
        if (!player.hasInfiniteMaterials()) {
            applyCaptureCost(tier, stack, player, target, hand);
        }
        target.discard();
        LassoFeedback.playCaptureSuccess(level, blockPos, feet);
        return Optional.empty();
    }

    public static Optional<Component> tryDeploy(
            LassoTier tier,
            ItemStack stack,
            ServerPlayer player,
            BlockPos spawnPos,
            InteractionHand hand
    ) {
        ServerLevel level = (ServerLevel) player.level();
        LassoCapture capture = LassoCapture.get(stack);
        if (capture == null) {
            return fail(level, player, Failure.EMPTY);
        }
        if (!player.hasInfiniteMaterials() && !canAffordDeploy(tier, stack, player)) {
            return fail(level, player, deployCostFailure(tier));
        }

        Entity entity = spawnCaptured(level, capture, spawnPos);
        if (entity == null) {
            return fail(level, player, Failure.SPAWN_BLOCKED);
        }

        LassoCapture.clear(stack);
        if (!player.hasInfiniteMaterials()) {
            applyDeployCost(tier, stack, player, hand);
        }
        LassoFeedback.playDeploySuccess(level, spawnPos, entity.position());
        return Optional.empty();
    }

    private static @Nullable Entity spawnCaptured(ServerLevel level, LassoCapture capture, BlockPos spawnPos) {
        EntityType<?> type = capture.entityType();
        if (type == null) {
            return null;
        }
        Entity entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (entity == null) {
            return null;
        }
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), capture.entityData()));
        Vec3 center = Vec3.atBottomCenterOf(spawnPos);
        entity.setPos(center.x, center.y, center.z);
        entity.setYRot(level.getRandom().nextFloat() * 360.0F);
        if (!level.noCollision(entity)) {
            return null;
        }
        if (!level.addFreshEntity(entity)) {
            return null;
        }
        return entity;
    }

    public static BlockPos deployPosFromUse(Player player) {
        return player.blockPosition().relative(player.getDirection());
    }

    private static @Nullable LassoCapture createCapture(HolderLookup.Provider registries, LivingEntity target) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        target.saveWithoutId(output);
        var tag = output.buildResult();
        stripWorldFields(tag);
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (entityId == null) {
            return null;
        }
        return new LassoCapture(entityId, tag, target.getHealth(), target.getMaxHealth());
    }

    private static void stripWorldFields(net.minecraft.nbt.CompoundTag tag) {
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("FallDistance");
        tag.remove("Fire");
        tag.remove("Air");
        tag.remove("OnGround");
        tag.remove("Dimension");
        tag.remove("Passengers");
        tag.remove("PortalCooldown");
        tag.remove("Leash");
    }

    private static @Nullable Failure validateCaptureTarget(LassoTier tier, LivingEntity target) {
        if (!target.isAlive() || target.isRemoved()) {
            return Failure.INVALID_TARGET;
        }
        if (target instanceof Player || target.isInvulnerable()) {
            return Failure.INVALID_TARGET;
        }
        if (!target.getPassengers().isEmpty()) {
            return Failure.INVALID_TARGET;
        }
        if (target.isPassenger()) {
            return Failure.INVALID_TARGET;
        }
        boolean boss = isBoss(target);
        return switch (tier) {
            case BASIC -> {
                if (boss || target.getMaxHealth() > BASIC_MAX_HEALTH) {
                    yield Failure.TOO_WEAK;
                }
                yield null;
            }
            case GOLDEN -> boss ? Failure.TOO_WEAK : null;
            case CURSED -> {
                if (boss && target.getHealth() > target.getMaxHealth() * BOSS_CAPTURE_HEALTH_FRACTION) {
                    yield Failure.BOSS_TOO_HEALTHY;
                }
                yield null;
            }
        };
    }

    private static boolean canAffordCapture(LassoTier tier, ItemStack stack, Player player, LivingEntity target) {
        return switch (tier) {
            case BASIC -> remainingDurability(stack) >= 1;
            case GOLDEN -> player.experienceLevel >= 1;
            case CURSED -> player.getHealth() > pickupHealthCost(target);
        };
    }

    private static boolean canAffordDeploy(LassoTier tier, ItemStack stack, Player player) {
        return switch (tier) {
            case BASIC -> remainingDurability(stack) >= 1;
            case GOLDEN -> player.experienceLevel >= 1;
            case CURSED -> true;
        };
    }

    private static void applyCaptureCost(LassoTier tier, ItemStack stack, ServerPlayer player, LivingEntity target, InteractionHand hand) {
        switch (tier) {
            case BASIC -> {}
            case GOLDEN -> player.giveExperienceLevels(-1);
            case CURSED -> {
                ServerLevel level = (ServerLevel) player.level();
                DamageSource source = level.damageSources().magic();
                player.hurtServer(level, source, pickupHealthCost(target));
            }
        }
    }

    private static void applyDeployCost(LassoTier tier, ItemStack stack, ServerPlayer player, InteractionHand hand) {
        switch (tier) {
            case BASIC -> stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            case GOLDEN -> player.giveExperienceLevels(-1);
            case CURSED -> {}
        }
    }

    private static boolean isBoss(LivingEntity target) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        if (id == null) {
            return false;
        }
        return id.equals(ENDER_DRAGON_ID) || id.equals(WITHER_ID) || id.equals(WARDEN_ID);
    }

    private static float pickupHealthCost(LivingEntity target) {
        return isBoss(target) ? CURSED_BOSS_COST : CURSED_NORMAL_COST;
    }

    private static int remainingDurability(ItemStack stack) {
        int max = stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
        int damage = stack.getOrDefault(DataComponents.DAMAGE, 0);
        return Math.max(0, max - damage);
    }

    private static Optional<Component> fail(ServerLevel level, ServerPlayer player, Failure failure) {
        if (failure.playsDenial()) {
            LassoFeedback.playDenial(level, player);
        }
        return message(failure);
    }

    static Optional<Component> translatedFailure(String keyPrefix, Enum<?> failure) {
        return Optional.of(Component.translatable(keyPrefix + failure.name().toLowerCase()));
    }

    private static Failure captureCostFailure(LassoTier tier, ItemStack stack, Player player, LivingEntity target) {
        return switch (tier) {
            case BASIC -> Failure.NEED_DURABILITY_DEPLOY;
            case GOLDEN -> Failure.NEED_XP_CAPTURE;
            case CURSED -> Failure.NEED_HEALTH;
        };
    }

    private static Failure deployCostFailure(LassoTier tier) {
        return switch (tier) {
            case BASIC -> Failure.NEED_DURABILITY_DEPLOY;
            case GOLDEN -> Failure.NEED_XP_DEPLOY;
            case CURSED -> Failure.NEED_DURABILITY_DEPLOY;
        };
    }

    private static Optional<Component> message(Failure failure) {
        return translatedFailure("item.dopasrandomutilities.lasso.failure.", failure);
    }
}
