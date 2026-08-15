package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.tuning.TerraMagicaTuning;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组 MobEffect 注册。
 * <p>
 * 魔法之境只注册<strong>一个</strong>全局效果 ID；多座法阵、不同施法者共用它，
 * 属性修饰符 id 也固定，因此 SPELL_POWER +30% 绝不会叠成 60%。
 */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, EldenRingSpellsMod.MOD_ID);

    /**
     * 属性修饰符固定 id（不是效果注册 id）。
     * 同 id 的 {@link AttributeModifier} 会替换而非相加。
     */
    public static final ResourceLocation TERRA_MAGICA_ATTRIBUTE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "mobeffect_terra_magica");

    /**
     * 魔法之境：全局法术强度 +30%（{@link AttributeRegistry#SPELL_POWER}）。
     * 颜色取辉石青，方便 HUD 图标描边。
     */
    public static final DeferredHolder<MobEffect, MobEffect> TERRA_MAGICA =
            MOB_EFFECTS.register("terra_magica", () ->
                    new MagicMobEffect(MobEffectCategory.BENEFICIAL, 0x3EE8F0)
                            .addAttributeModifier(
                                    AttributeRegistry.SPELL_POWER,
                                    TERRA_MAGICA_ATTRIBUTE_MODIFIER_ID,
                                    TerraMagicaTuning.SPELL_POWER_BONUS_MULTIPLIED_TOTAL,
                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                            )
            );

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
