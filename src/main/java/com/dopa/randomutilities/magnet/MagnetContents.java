package com.dopa.randomutilities.magnet;

import java.util.ArrayList;
import java.util.List;

import com.dopa.randomutilities.machine.config.UpgradeConfig;
import com.dopa.randomutilities.magnet.config.MagnetConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record MagnetContents(
        boolean enabled,
        boolean whitelist,
        int range,
        boolean collectMode,
        boolean ignorePickupDelay,
        boolean pauseOnSneak,
        boolean pullXp,
        boolean particles,
        int color,
        List<ItemStack> filters,
        List<ItemStack> upgrades
) {
    public static final int FILTER_SLOTS = 8;
    public static final int DEFAULT_COLOR = 0xFFFFFF;

    private static final int FLAG_ENABLED = 1;
    private static final int FLAG_WHITELIST = 1 << 1;
    private static final int FLAG_COLLECT = 1 << 2;
    private static final int FLAG_IGNORE_DELAY = 1 << 3;
    private static final int FLAG_PAUSE_SNEAK = 1 << 4;
    private static final int FLAG_PULL_XP = 1 << 5;
    private static final int FLAG_PARTICLES = 1 << 6;

    private static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> STACK_LIST =
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());

    public static MagnetContents defaults() {
        return new MagnetContents(
                false,
                false,
                MagnetConfig.baseRange(),
                false,
                false,
                true,
                false,
                true,
                DEFAULT_COLOR,
                empty(FILTER_SLOTS),
                empty(UpgradeConfig.UPGRADE_SLOT_COUNT)
        );
    }

    public static final Codec<MagnetContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(MagnetContents::enabled),
            Codec.BOOL.optionalFieldOf("whitelist", false).forGetter(MagnetContents::whitelist),
            Codec.INT.optionalFieldOf("range", 4).forGetter(MagnetContents::range),
            Codec.BOOL.optionalFieldOf("collect", false).forGetter(MagnetContents::collectMode),
            Codec.BOOL.optionalFieldOf("ignore_pickup_delay", false).forGetter(MagnetContents::ignorePickupDelay),
            Codec.BOOL.optionalFieldOf("pause_on_sneak", true).forGetter(MagnetContents::pauseOnSneak),
            Codec.BOOL.optionalFieldOf("pull_xp", false).forGetter(MagnetContents::pullXp),
            Codec.BOOL.optionalFieldOf("particles", true).forGetter(MagnetContents::particles),
            Codec.INT.optionalFieldOf("color", DEFAULT_COLOR).forGetter(MagnetContents::color),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("filters", List.of()).forGetter(MagnetContents::filters),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("upgrades", List.of()).forGetter(MagnetContents::upgrades)
    ).apply(instance, MagnetContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MagnetContents> STREAM_CODEC = StreamCodec.of(
            MagnetContents::encode,
            MagnetContents::decode
    );

    public MagnetContents {
        color = color & 0xFFFFFF;
        range = Math.max(0, range);
        filters = padCopy(filters, FILTER_SLOTS);
        upgrades = padCopy(upgrades, UpgradeConfig.UPGRADE_SLOT_COUNT);
    }

    private static void encode(RegistryFriendlyByteBuf buf, MagnetContents value) {
        buf.writeByte(value.packedFlags());
        buf.writeVarInt(value.range());
        buf.writeInt(value.color());
        STACK_LIST.encode(buf, value.filters());
        STACK_LIST.encode(buf, value.upgrades());
    }

    private static MagnetContents decode(RegistryFriendlyByteBuf buf) {
        int flags = buf.readByte() & 0xFF;
        int range = buf.readVarInt();
        int color = buf.readInt();
        List<ItemStack> filters = STACK_LIST.decode(buf);
        List<ItemStack> upgrades = STACK_LIST.decode(buf);
        return new MagnetContents(
                (flags & FLAG_ENABLED) != 0,
                (flags & FLAG_WHITELIST) != 0,
                range,
                (flags & FLAG_COLLECT) != 0,
                (flags & FLAG_IGNORE_DELAY) != 0,
                (flags & FLAG_PAUSE_SNEAK) != 0,
                (flags & FLAG_PULL_XP) != 0,
                (flags & FLAG_PARTICLES) != 0,
                color,
                filters,
                upgrades
        );
    }

    private int packedFlags() {
        int flags = 0;
        if (enabled) {
            flags |= FLAG_ENABLED;
        }
        if (whitelist) {
            flags |= FLAG_WHITELIST;
        }
        if (collectMode) {
            flags |= FLAG_COLLECT;
        }
        if (ignorePickupDelay) {
            flags |= FLAG_IGNORE_DELAY;
        }
        if (pauseOnSneak) {
            flags |= FLAG_PAUSE_SNEAK;
        }
        if (pullXp) {
            flags |= FLAG_PULL_XP;
        }
        if (particles) {
            flags |= FLAG_PARTICLES;
        }
        return flags;
    }

    public NonNullList<ItemStack> filterSlots() {
        NonNullList<ItemStack> slots = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack stack = filters.get(i);
            slots.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return slots;
    }

    public ItemStack filter(int index) {
        if (index < 0 || index >= FILTER_SLOTS) {
            return ItemStack.EMPTY;
        }
        return filters.get(index);
    }

    public ItemStack upgrade(int index) {
        if (index < 0 || index >= upgrades.size()) {
            return ItemStack.EMPTY;
        }
        return upgrades.get(index);
    }

    public MagnetContents withEnabled(boolean enabled) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withWhitelist(boolean whitelist) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withRange(int range) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withCollectMode(boolean collectMode) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withIgnorePickupDelay(boolean ignorePickupDelay) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withPauseOnSneak(boolean pauseOnSneak) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withPullXp(boolean pullXp) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withParticles(boolean particles) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withColor(int color) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    public MagnetContents withFilter(int index, ItemStack stack) {
        if (index < 0 || index >= FILTER_SLOTS) {
            return this;
        }
        List<ItemStack> next = new ArrayList<>(filters);
        next.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, next, upgrades);
    }

    public MagnetContents withUpgrades(List<ItemStack> upgrades) {
        return copy(enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color, filters, upgrades);
    }

    private MagnetContents copy(
            boolean enabled,
            boolean whitelist,
            int range,
            boolean collectMode,
            boolean ignorePickupDelay,
            boolean pauseOnSneak,
            boolean pullXp,
            boolean particles,
            int color,
            List<ItemStack> filters,
            List<ItemStack> upgrades
    ) {
        return new MagnetContents(
                enabled, whitelist, range, collectMode, ignorePickupDelay, pauseOnSneak, pullXp, particles, color,
                filters, upgrades
        );
    }

    private static List<ItemStack> empty(int size) {
        List<ItemStack> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(ItemStack.EMPTY);
        }
        return list;
    }

    private static List<ItemStack> padCopy(List<ItemStack> in, int size) {
        List<ItemStack> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = in != null && i < in.size() ? in.get(i) : ItemStack.EMPTY;
            out.add(stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        return List.copyOf(out);
    }
}
