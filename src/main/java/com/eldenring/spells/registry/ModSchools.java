package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 向铁魔法的 {@link SchoolRegistry} 注册本模组学派。
 * <p>
 * 学派 id：{@code elden_ring_spells:glintstone}（显示名「辉石」）。
 * 法术 {@code DefaultConfig#setSchoolResource} 须指向 {@link #GLINTSTONE_RESOURCE}。
 */
public final class ModSchools {
    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, EldenRingSpellsMod.MOD_ID);

    /** 辉石学派 ResourceLocation，供法术 DefaultConfig 引用。 */
    public static final ResourceLocation GLINTSTONE_RESOURCE =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "glintstone");

    /**
     * 辉石学派：青蓝显示色、紫水晶触媒、独立强度/抗性属性与伤害类型。
     * <p>
     * 施法音暂用紫水晶钟鸣，贴合法环辉石晶体手感。
     */
    public static final DeferredHolder<SchoolType, SchoolType> GLINTSTONE = SCHOOLS.register(
            "glintstone",
            () -> new SchoolType(
                    GLINTSTONE_RESOURCE,
                    ModTags.GLINTSTONE_FOCUS,
                    Component.translatable("school.elden_ring_spells.glintstone")
                            .withStyle(Style.EMPTY.withColor(0x3EE8F0)),
                    ModAttributes.GLINTSTONE_SPELL_POWER,
                    ModAttributes.GLINTSTONE_MAGIC_RESIST,
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.AMETHYST_BLOCK_CHIME),
                    ModDamageTypes.GLINTSTONE_MAGIC
            )
    );

    private ModSchools() {
    }

    public static void register(IEventBus modEventBus) {
        SCHOOLS.register(modEventBus);
    }
}
