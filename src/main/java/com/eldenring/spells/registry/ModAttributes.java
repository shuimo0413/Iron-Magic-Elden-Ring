package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.attribute.MagicPercentAttribute;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 辉石学派相关属性：法术强度与对应抗性。
 * <p>
 * 命名约定与铁魔法一致：{@code <school>_spell_power} / {@code <school>_magic_resist}，
 * 以便装备/词条按属性反查学派。
 */
public final class ModAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, EldenRingSpellsMod.MOD_ID);

    /**
     * 辉石法术强度（百分比属性，默认 1.0 = 100%）。
     * 调高装备加成可显著提升辉石系伤害。
     */
    public static final DeferredHolder<Attribute, Attribute> GLINTSTONE_SPELL_POWER =
            ATTRIBUTES.register("glintstone_spell_power", () ->
                    new MagicPercentAttribute(
                            "attribute.elden_ring_spells.glintstone_spell_power",
                            1.0D, -100.0D, 100.0D
                    ).setSyncable(true));

    /**
     * 辉石魔法抗性（百分比属性，默认 1.0）。
     * 调高 → 受到辉石伤害更少（经铁魔法 soft-cap 公式）。
     */
    public static final DeferredHolder<Attribute, Attribute> GLINTSTONE_MAGIC_RESIST =
            ATTRIBUTES.register("glintstone_magic_resist", () ->
                    new MagicPercentAttribute(
                            "attribute.elden_ring_spells.glintstone_magic_resist",
                            1.0D, -100.0D, 100.0D
                    ).setSyncable(true));

    private ModAttributes() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::modifyEntityAttributes);
    }

    /**
     * 把辉石属性挂到所有生物实体上，否则 {@link io.redspace.ironsspellbooks.api.spells.SchoolType#getPowerFor}
     * 会因缺少属性而回退为 1。
     */
    private static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(entityType -> {
            event.add(entityType, GLINTSTONE_SPELL_POWER);
            event.add(entityType, GLINTSTONE_MAGIC_RESIST);
        });
    }
}
