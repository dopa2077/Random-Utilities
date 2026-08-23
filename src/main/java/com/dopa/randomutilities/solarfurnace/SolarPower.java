package com.dopa.randomutilities.solarfurnace;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;

/** Sky-access and sun-angle evaluation for the solar furnace. */
public final class SolarPower {
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
        if (level.isDarkOutside()) {
            return new Snapshot(0.0F, Status.NO_SUN);
        }

        // visual/sun_angle: degrees east→west with 0 at zenith (noon). Height is cos(angle).
        // Level.getSunAngle(float) no longer exists on 26.x; do not apply the old daylight-detector
        // soft-wrap either — that assumed celestial radians where noon is also ~0, but mixed poorly
        // with degree values near the 360→0 noon wrap.
        float sunAngleDeg = level.environmentAttributes().getValue(EnvironmentAttributes.SUN_ANGLE, skyPos);
        float height = Mth.cos(sunAngleDeg * ((float) Math.PI / 180.0F));
        if (height <= 0.0F) {
            return new Snapshot(0.0F, Status.NO_SUN);
        }
        height = Mth.clamp(height, 0.01F, 1.0F);
        if (level.isRainingAt(skyPos)) {
            height *= level.isThundering() ? 0.2F : 0.5F;
        }
        return new Snapshot(height, Status.WORKING);
    }
}
