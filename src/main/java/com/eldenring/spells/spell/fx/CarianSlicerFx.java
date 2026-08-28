package com.eldenring.spells.spell.fx;

import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.registry.ModParticles;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 卡利亚迅剑特效：挥砍音效、刃尖新月粒子。密度 / 音高写死，不进 toml。
 */
public final class CarianSlicerFx {



    private CarianSlicerFx() {
    }

    /**
     * 挥砍瞬间音效。不套辉石爆发粒子，那颗青色球会挡第一人称。
     */
    public static void playSlashSounds(Level level, Vec3 slashOrigin) {
        level.playSound(
                null,
                slashOrigin.x,
                slashOrigin.y,
                slashOrigin.z,
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.NEUTRAL,
                0.85f,
                1.35f + level.random.nextFloat() * 0.15f
        );
        level.playSound(
                null,
                slashOrigin.x,
                slashOrigin.y,
                slashOrigin.z,
                SoundEvents.AMETHYST_BLOCK_HIT,
                SoundSource.NEUTRAL,
                0.7f,
                1.55f + level.random.nextFloat() * 0.2f
        );
    }

    /**
     * 挥砍过程中在刃尖撒卡利亚新月。位置跟实体挥砍弧，不跟手骨骼。
     */
    public static void spawnSwingParticles(CarianSlicerEntity slicerEntity, Level level) {
        Vec3 bladeTipWorld = slicerEntity.computeBladeTipWorld(slicerEntity.getSwingProgress(0.0f), 0.0f);

        level.addParticle(
                ModParticles.CARIAN_SPARK.get(),
                bladeTipWorld.x,
                bladeTipWorld.y,
                bladeTipWorld.z,
                (level.random.nextDouble() - 0.5) * 0.04,
                (level.random.nextDouble() - 0.5) * 0.03,
                (level.random.nextDouble() - 0.5) * 0.04
        );
        level.addParticle(
                ModParticles.CARIAN_SPARK.get(),
                bladeTipWorld.x,
                bladeTipWorld.y,
                bladeTipWorld.z,
                (level.random.nextDouble() - 0.5) * 0.12,
                (level.random.nextDouble() - 0.5) * 0.12,
                (level.random.nextDouble() - 0.5) * 0.12
        );
    }
}
