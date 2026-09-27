package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
/**
 * 向铁魔法的 {@link SchoolRegistry} 注册本模组学派。
 * <p>
 * 学派 id：{@code iss_elden_ring:glintstone}（显示名「辉石」）。
 * 法术 {@code DefaultConfig#setSchoolResource} 须指向 {@link #GLINTSTONE_RESOURCE}。
 */
public final class ModSchools {
    public static final DeferredRegister<SchoolType> SCHOOLS =
            DeferredRegister.create(SchoolRegistry.SCHOOL_REGISTRY_KEY, EldenRingSpellsMod.MOD_ID);

    /** 辉石学派 ResourceLocation，供法术 DefaultConfig 引用。 */
    public static final ResourceLocation GLINTSTONE_RESOURCE =
            new ResourceLocation(EldenRingSpellsMod.MOD_ID, "glintstone");

    /**
     * 辉石学派：青蓝显示色、三色辉石碎片触媒、独立强度/抗性属性与伤害类型。
     * <p>
     * 施法音暂用紫水晶钟鸣，贴合法环辉石晶体手感。
     * <p>
     * 1.20.1 的 {@link SchoolType} 构造器收 {@code Supplier<Attribute>/Supplier<SoundEvent>}；
     * 本模组属性句柄的 {@code RegistryObject} 本身就是 Supplier，可直接传，
     * 但声音必须从 {@code Holder}（1.21 的 {@code wrapAsHolder}）改成 lambda。
     */
    public static final RegistryObject<SchoolType> GLINTSTONE = SCHOOLS.register(
            "glintstone",
            () -> new SchoolType(
                    GLINTSTONE_RESOURCE,
                    ModTags.GLINTSTONE_FOCUS,
                    Component.translatable("school.iss_elden_ring.glintstone")
                            .withStyle(Style.EMPTY.withColor(0x3EE8F0)),
                    ModAttributes.GLINTSTONE_SPELL_POWER,
                    ModAttributes.GLINTSTONE_MAGIC_RESIST,
                    () -> SoundEvents.AMETHYST_BLOCK_CHIME,
                    ModDamageTypes.GLINTSTONE_MAGIC
            )
    );

    private ModSchools() {
    }

    public static void register(IEventBus modEventBus) {
        SCHOOLS.register(modEventBus);
    }
}
