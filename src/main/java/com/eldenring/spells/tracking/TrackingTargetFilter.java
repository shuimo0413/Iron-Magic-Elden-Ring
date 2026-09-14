package com.eldenring.spells.tracking;

import com.eldenring.spells.registry.ModAttachments;
import javax.annotation.Nullable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

/**
 * 辉石系索敌 / 追踪的排除过滤：先盟友规则，再按施法者偏好排除。
 * <p>
 * 仅当弹道 {@code owner} 是 {@link Player} 时读取其 Attachment；女仆 / 怪等非玩家施法一律
 * {@link TrackingIgnorePrefs#DEFAULT}，不猜测主人、不硬依赖其它 mod。
 * <p>
 * 分类是<strong>保守排除</strong>：认不出的模组生物落在「未分类」，不会被任何排除项踢掉。
 */
public final class TrackingTargetFilter {
    private TrackingTargetFilter() {
    }

    /**
     * 读取用于索敌的偏好：玩家 owner → Attachment，否则默认。
     */
    public static TrackingIgnorePrefs prefsForOwner(@Nullable Entity ownerEntity) {
        if (ownerEntity instanceof Player player) {
            return player.getData(ModAttachments.TRACKING_IGNORE_PREFS.get());
        }
        return TrackingIgnorePrefs.DEFAULT;
    }

    /**
     * 在已通过「活着 / 非旁观 / 非主人 / 非盟友」之后，按偏好判断是否允许追踪。
     *
     * @param ownerEntity 弹道主人；可为 null（视为默认偏好）
     * @return {@code true} 表示可以当作追踪 / 圆阵触发目标
     */
    public static boolean allowsTracking(@Nullable Entity ownerEntity, LivingEntity candidateEntity) {
        return allowsTracking(prefsForOwner(ownerEntity), candidateEntity);
    }

    /**
     * 按给定偏好判断候选是否允许被追踪（不含盟友 / 距离 / 锥角）。
     */
    public static boolean allowsTracking(TrackingIgnorePrefs prefs, LivingEntity candidateEntity) {
        TargetKind kind = classify(candidateEntity);
        return switch (kind) {
            case PLAYER -> !prefs.ignorePlayers();
            case PEACEFUL -> !prefs.ignorePeaceful();
            case NEUTRAL -> !prefs.ignoreNeutral();
            case HOSTILE -> !prefs.ignoreHostile();
            case UNKNOWN -> true;
        };
    }

    /**
     * 保守分类：先玩家 → 敌对 → 中立 → 明确和平 → 未分类。
     * 「无 Enemy 标签」绝不直接当成和平。
     */
    public static TargetKind classify(LivingEntity candidateEntity) {
        if (candidateEntity instanceof Player) {
            return TargetKind.PLAYER;
        }
        if (isHostileLike(candidateEntity)) {
            return TargetKind.HOSTILE;
        }
        if (candidateEntity instanceof NeutralMob) {
            return TargetKind.NEUTRAL;
        }
        if (isClearlyPeaceful(candidateEntity)) {
            return TargetKind.PEACEFUL;
        }
        return TargetKind.UNKNOWN;
    }

    /**
     * 敌对：原版 {@link Enemy}，或生成分类为 {@link MobCategory#MONSTER}。
     */
    private static boolean isHostileLike(LivingEntity candidateEntity) {
        if (candidateEntity instanceof Enemy) {
            return true;
        }
        return candidateEntity.getType().getCategory() == MobCategory.MONSTER;
    }

    /**
     * 仅排除有把握的被动生物；已判为中立 / 敌对的不会走到这里。
     */
    private static boolean isClearlyPeaceful(LivingEntity candidateEntity) {
        if (candidateEntity instanceof AbstractVillager) {
            return true;
        }
        if (candidateEntity instanceof AmbientCreature) {
            return true;
        }
        if (candidateEntity instanceof Animal) {
            return true;
        }
        if (candidateEntity instanceof WaterAnimal) {
            MobCategory category = candidateEntity.getType().getCategory();
            return category == MobCategory.WATER_CREATURE
                    || category == MobCategory.WATER_AMBIENT
                    || category == MobCategory.CREATURE;
        }
        MobCategory category = candidateEntity.getType().getCategory();
        return category == MobCategory.CREATURE
                || category == MobCategory.AMBIENT
                || category == MobCategory.WATER_AMBIENT;
    }

    /**
     * 索敌用的目标大类。
     */
    public enum TargetKind {
        PLAYER,
        HOSTILE,
        NEUTRAL,
        PEACEFUL,
        /** 模组怪等认不出的 LivingEntity：默认永不被类别排除。 */
        UNKNOWN
    }
}
