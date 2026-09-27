package com.eldenring.spells.item;

import com.eldenring.spells.registry.ModAttributes;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import io.redspace.ironsspellbooks.item.weapons.StaffTier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * 观星杖武器档位：近战数值贴近铁魔法普通法杖，额外给辉石法术强度。
 * <p>
 * 1.20.1 里铁魔法 {@code StaffItem} 只接受 {@link StaffTier}，且
 * {@code ExtendedSwordItem.createAttributes(...)} 与 {@code Item.Properties.attributes(...)}
 * 都不存在；所以这里改为直接继承 {@link StaffTier}，把近战数值与附加属性交给父类构造器，
 * 由 {@code StaffItem(Properties, StaffTier)} 生成默认属性。
 * <p>
 * {@code glintstone_spell_power +10%}（{@link AttributeModifier.Operation#MULTIPLY_BASE}）
 * → 手持时辉石系伤害约提升 10%。攻击伤害 / 攻速与灰须杖同档，避免近战过强。
 */
public final class AstrologerStaffTier extends StaffTier {

    /**
     * 主手攻击伤害加成（点）。调大 → 近战砸得更疼；法杖以施法为主，保持偏低。
     */
    private static final float ATTACK_DAMAGE_BONUS = 3.0F;

    /**
     * 攻击速度修正（加到原版基值上）。负值越接近 0 → 挥得越快；-3 与铁魔法多数法杖一致。
     */
    private static final float ATTACK_SPEED_MODIFIER = -3.0F;

    /**
     * 辉石法术强度乘数加成。0.10 = +10%；调大 → 辉石咒伤害更高。
     */
    private static final double GLINTSTONE_SPELL_POWER_BONUS = 0.10D;

    /**
     * 全局单例：档位不可变，注册物品时共享一份即可。
     * 必须声明在常量之后，保证类初始化时参数已就绪。
     */
    public static final AstrologerStaffTier INSTANCE = new AstrologerStaffTier();

    private AstrologerStaffTier() {
        super(
                ATTACK_DAMAGE_BONUS,
                ATTACK_SPEED_MODIFIER,
                new AttributeContainer(
                        ModAttributes.GLINTSTONE_SPELL_POWER,
                        GLINTSTONE_SPELL_POWER_BONUS,
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
    }
}
