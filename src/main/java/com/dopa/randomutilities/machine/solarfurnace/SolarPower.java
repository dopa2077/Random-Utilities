package com.dopa.randomutilities.machine.solarfurnace;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;

/** Sky-access and sun-angle evaluation for the solar furnace. */
public final class SolarPower {
    /** Peak cook speed relative to a vanilla furnace (1.0 = full speed). */
    public static final float PEAK_SPEED = 0.9F;

    public enum Status {
        WORKING,
        NO_SKY,
        NO_SUN
    }

    public record Snapshot(float factor, Status status) {
        public int permille() {
            return Mth.clamp(Math.round(factor * 1000.0F), 0, 1000);
        }

        public int percent() {
            return Mth.clamp(Math.round(factor * 100.0F), 0, 100);
        }
    }

    private SolarPower() {}

    public static Snapshot evaluate(Level level, BlockPos furnacePos) {
        BlockPos skyPos = furnacePos.above();
        if (!level.canSeeSky(skyPos)) {
            return new Snapshot(0.0F, Status.NO_SKY);
        }
        if (level.getEffectiveSkyBrightness(skyPos) <= 0 || level.isDarkOutside()) {
            return new Snapshot(0.0F, Status.NO_SUN);
        }

        float sunAngleDeg = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, skyPos);
        float sunAngleRad = sunAngleDeg * ((float) Math.PI / 180.0F);
        // Same soft correction Daylight Detector uses so noon reads high and dawn/dusk taper.
        float offset = sunAngleRad < (float) Math.PI ? 0.0F : (float) (Math.PI * 2.0);
        sunAngleRad += (offset - sunAngleRad) * 0.2F;
        float height = Mth.cos(sunAngleRad);
        if (height <= 0.0F) {
            return new Snapshot(0.0F, Status.NO_SUN);
        }
        if (level.isRainingAt(skyPos)) {
            height *= level.isThundering() ? 0.2F : 0.5F;
        }
        return new Snapshot(PEAK_SPEED * height, Status.WORKING);
    }
}
