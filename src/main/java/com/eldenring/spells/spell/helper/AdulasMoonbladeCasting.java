package com.eldenring.spells.spell.helper;

import com.eldenring.spells.entity.AdulasMoonbladeEntity;
import com.eldenring.spells.entity.AdulasMoonbladeWaveProjectile;
import com.eldenring.spells.registry.ModSounds;
import com.eldenring.spells.spell.data.AdulasMoonbladeCastData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 亚杜拉的月光剑吟唱期实体生命周期：生成 / 补绑锚点、刷新伤害、每刀射出剑气。
 */
public final class AdulasMoonbladeCasting {

    /**
     * 剑气生成点相对眼睛沿水平朝向前移（方块）。太小容易嵌进自己，太大容易穿墙。
     */
    private static final double WAVE_SPAWN_FORWARD_OFFSET_BLOCKS = 0.6;

    private AdulasMoonbladeCasting() {
    }

    /**
     * 若尚未绑定或实体已失效，则生成新的斩击锚点并写入 CastData；否则只刷新伤害。
     */
    public static void ensureMoonbladeEntity(
            Level level,
            LivingEntity caster,
            AdulasMoonbladeCastData castData,
            float slashDamage,
            float waveDamage
    ) {
        AdulasMoonbladeEntity existing = castData.moonbladeEntity();
        if (existing != null && !existing.isRemoved()) {
            existing.setDamages(slashDamage, waveDamage);
            return;
        }
        AdulasMoonbladeEntity moonbladeEntity = new AdulasMoonbladeEntity(level, caster, slashDamage, waveDamage);
        level.addFreshEntity(moonbladeEntity);
        castData.bindMoonbladeEntity(moonbladeEntity);
    }

    /**
     * 本刀命中帧：沿施法者水平朝向射出一道冰月牙剑气，并在出弹点播放飞弹射出音。
     * <p>
     * 只取偏航角、丢掉俯仰：抬头 / 低头都贴着眼睛高度水平直飞，生成点也只在水平方向前移。
     */
    public static void launchWave(Level level, LivingEntity caster, float waveDamage) {
        if (level.isClientSide) {
            return;
        }
        Vec3 horizontalForward = Vec3.directionFromRotation(0.0f, caster.getYRot());
        GlintstoneCastHelper.spawnAlongLook(
                level,
                caster,
                AdulasMoonbladeWaveProjectile::new,
                0.0,
                0.0,
                0.0f,
                waveDamage,
                horizontalForward,
                horizontalForward.scale(WAVE_SPAWN_FORWARD_OFFSET_BLOCKS),
                false
        );
        ModSounds.playProjectileLaunch(level, caster);
    }

    /**
     * 请求停止并清空绑定；实体会在本刀命中窗结束后自行 discard。
     */
    public static void requestStop(@Nullable AdulasMoonbladeCastData castData) {
        if (castData == null) {
            return;
        }
        castData.reset();
    }
}
