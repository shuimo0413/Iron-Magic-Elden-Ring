package com.eldenring.spells.item;

import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 卢瑟特的辉石杖武器档位：近战与亚兹勒的辉石杖同档，额外给全学派通用法术强度。
 * <p>
 * 1.20.1 里铁魔法 {@code StaffItem} 只接受 {@link StaffTier}，附加属性交给父类构造器生成
 * （原因同 {@link AzurGlintstoneStaffTier}）。
 * <p>
 * 铁魔法 {@code spell_power +35%}（{@link AttributeModifier.Operation#MULTIPLY_BASE}）
 * → 手持时所有学派法术强度约提升 35%；代价是 {@link LusatStaffBalance} 的蓝耗倍率。
 */
public final class LusatGlintstoneStaffTier extends StaffTier {

    /**
     * 主手攻击伤害加成（点）。调大 → 近战砸得更疼；法杖以施法为主，与亚兹勒杖保持一致。
     */
    private static final float ATTACK_DAMAGE_BONUS = 3.0F;

    /**
     * 攻击速度修正（加到原版基值上）。负值越接近 0 → 挥得越快；-3 与铁魔法多数法杖一致。
     */
    private static final float ATTACK_SPEED_MODIFIER = -3.0F;

    /**
     * 通用法术强度乘数加成。0.35 = +35%；调大 → 全学派法术伤害 / 效果更强。
     */
    private static final double SPELL_POWER_BONUS = 0.35D;

    /**
     * 全局单例：档位不可变，注册物品时共享一份即可。
     * 必须声明在常量之后，保证类初始化时参数已就绪。
     */
    public static final LusatGlintstoneStaffTier INSTANCE = new LusatGlintstoneStaffTier();

    private LusatGlintstoneStaffTier() {
        super(
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED_MODIFIER,
                new AttributeContainer(
                        AttributeRegistry.SPELL_POWER,
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
    }
}
