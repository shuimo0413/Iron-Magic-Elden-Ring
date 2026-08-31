package com.eldenring.spells.spell.fx;

import com.eldenring.spells.registry.ModParticles;
import com.eldenring.spells.registry.ModSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 卡利亚迅剑斩击特效：沿刃留下小星星，与 {@link com.eldenring.spells.client.render.carian.CarianSlicerTrail}
 * 的 ribbon / 扫面一起用；命中帧播辉石蓄力起手音。
 * <p>
 * 粒子只用 {@code CARIAN_GLINT}（细闪）/ {@code CARIAN_MOTE}（十字星）/
 * {@code CARIAN_NOVA}（八芒星）/ {@code CARIAN_CROSS}（对角星）。
 * 每 tick 固定颗数，不掷概率，也不进 toml。粒子必须在客户端调用；斩击音由服务端广播。
 */
public final class CarianSlicerFx {

    /**
     * 沿剑身刷细闪的采样点数。每点固定一颗 {@code CARIAN_GLINT}。
     * 调大 → 刃上星星更密，连斩时更吃粒子预算。
     */
    private static final int BLADE_SAMPLE_COUNT = 3;

    /**
     * 本 tick 刃尖位移超过此值（方块）才在刃中 / 刃尖多两颗稍大的星。
     */
    private static final double FLASH_MIN_TIP_TRAVEL_BLOCKS = 0.22;

    /**
     * 星星残留速度倍率。接近 0 时粒子停在挥过的位置慢慢闪灭。
     */
    private static final double STAR_LINGER_VELOCITY_SCALE = 0.04;

    private CarianSlicerFx() {
    }

    /**
     * 每刀命中帧播一次辉石蓄力起手音（{@code spell_cast_start}）。
     * 起手 / 收招仍静音，不要接到 Spell 的 getCastStartSound。
     */
    public static void playSlashSound(Level level, LivingEntity caster) {
        if (level.isClientSide) {
            return;
        }
        ModSounds.playCastStart(level, caster);
    }

    /**
     * 本 tick 沿当前剑刃刷一层星星。
     *
     * @param hiltWorld     护手附近世界坐标
     * @param tipWorld      刃尖世界坐标
     * @param tipTravelSinceLastTick 上一 tick 到现在刃尖走过的位移（方块），用来判断是起手还是真正在砍
     */
    public static void spawnAlongSlash(
            Level level,
            Vec3 hiltWorld,
            Vec3 tipWorld,
            Vec3 tipTravelSinceLastTick
    ) {
        if (!level.isClientSide) {
            return;
        }
        Vec3 bladeOffset = tipWorld.subtract(hiltWorld);
        if (bladeOffset.lengthSqr() < 1.0e-6) {
            return;
        }
        double tipTravelBlocks = tipTravelSinceLastTick.length();
        boolean fastSwing = tipTravelBlocks >= FLASH_MIN_TIP_TRAVEL_BLOCKS;
        Vec3 swingDirection = tipTravelBlocks > 1.0e-6
                ? tipTravelSinceLastTick.normalize()
                : Vec3.ZERO;
        spawnStarsAlongBlade(level, hiltWorld, bladeOffset, swingDirection, fastSwing);
    }

    /**
     * 沿刃钉小星星：三点细闪 + 刃尖一颗十字星；挥得快时刃中再加八芒星、近尖再加对角星。
     */
    private static void spawnStarsAlongBlade(
            Level level,
            Vec3 hiltWorld,
            Vec3 bladeOffset,
            Vec3 swingDirection,
            boolean fastSwing
    ) {
        Vec3 starVelocity = swingDirection.scale(STAR_LINGER_VELOCITY_SCALE);
        for (int sampleIndex = 0; sampleIndex < BLADE_SAMPLE_COUNT; sampleIndex++) {
            float alongBlade = (sampleIndex + 0.35f) / BLADE_SAMPLE_COUNT;
            Vec3 samplePosition = hiltWorld.add(bladeOffset.scale(alongBlade));
            level.addParticle(
                    ModParticles.CARIAN_GLINT.get(),
                    samplePosition.x,
                    samplePosition.y,
                    samplePosition.z,
                    starVelocity.x,
                    starVelocity.y,
                    starVelocity.z
            );
        }
        Vec3 tipStarPosition = hiltWorld.add(bladeOffset.scale(
                (BLADE_SAMPLE_COUNT - 0.65f) / BLADE_SAMPLE_COUNT
        ));
        level.addParticle(
                ModParticles.CARIAN_MOTE.get(),
                tipStarPosition.x,
                tipStarPosition.y,
                tipStarPosition.z,
                starVelocity.x * 0.6,
                starVelocity.y * 0.6,
                starVelocity.z * 0.6
        );
        if (!fastSwing) {
            return;
        }
        Vec3 midBlade = hiltWorld.add(bladeOffset.scale(0.55));
        Vec3 nearTip = hiltWorld.add(bladeOffset.scale(0.88));
        level.addParticle(
                ModParticles.CARIAN_NOVA.get(),
                midBlade.x,
                midBlade.y,
                midBlade.z,
                starVelocity.x,
                starVelocity.y,
                starVelocity.z
        );
        level.addParticle(
                ModParticles.CARIAN_CROSS.get(),
                nearTip.x,
                nearTip.y,
                nearTip.z,
                starVelocity.x * 0.5,
                starVelocity.y * 0.5,
                starVelocity.z * 0.5
        );
    }
}
