package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import com.eldenring.spells.spell.TerraMagicaSpell;

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
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "mobeffect_terra_magica");

    /**
     * 魔法之境：全局法术强度 +30%（{@link AttributeRegistry#SPELL_POWER}）。
     * 颜色取辉石青，方便 HUD 图标描边。
     * <p>
     * 末位 {@code cast()} 不能省：铁魔法 1.20.1 给 {@code MagicMobEffect} 加了
     * {@code IBackwardsAttributeCompatMobEffect} 兼容接口，其
     * {@code addAttributeModifier(Supplier&lt;Attribute&gt;, ResourceLocation, double, Operation)}
     * 返回的是接口本身而不是 {@link MobEffect}；{@code cast()} 才把结果转回效果对象，
     * 供 {@code DeferredRegister<MobEffect>} 注册。
     */
    public static final RegistryObject<MobEffect> TERRA_MAGICA =
            MOB_EFFECTS.register("terra_magica", () ->
                    new MagicMobEffect(MobEffectCategory.BENEFICIAL, 0x3EE8F0)
                            .addAttributeModifier(
                                    AttributeRegistry.SPELL_POWER,
                                    TERRA_MAGICA_ATTRIBUTE_MODIFIER_ID,
                                    TerraMagicaSpell.SPELL_POWER_BONUS_MULTIPLIED_TOTAL,
                                    AttributeModifier.Operation.MULTIPLY_TOTAL
                            )
                            .cast()
            );

    private ModEffects() {
    }

    public static void register(IEventBus modEventBus) {
        MOB_EFFECTS.register(modEventBus);
    }
}
