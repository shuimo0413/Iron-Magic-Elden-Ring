package com.eldenring.spells.item;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModAttributes;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.entity.armor.GenericArmorModel;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.item.armor.ExtendedArmorMaterials;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.item.weapons.AttributeContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 星辰法师套装（设计图版宽檐帽 / 面具 / 蓝金长袍）。
 * 沿用铁魔法学派护甲材质、胸甲灌注槽与属性构建逻辑。
 * <p>
 * 1.20.1 的护甲材质不是 registry 条目，{@code ArmorMaterialRegistry.SCHOOL} 整类不存在，
 * 因此改用最接近的 {@link ExtendedArmorMaterials#WIZARD}（学派护甲槽位耐久、魔法布修复、
 * {@code schoolArmorMap()} 防御数值，且材质自带 法力上限 +125 / 全学派法强 +5%）。
 */
public final class CelestialMageArmorItem extends ImbuableChestplateArmorItem {

    /**
     * 单件辉石法术强度加成：+10%（乘算基值）。四部位各挂一条，凑齐 +40%；
     * 数值写死在视觉/装备层，不进 toml。
     */
    private static final AttributeContainer GLINTSTONE_SPELL_POWER_CONTAINER =
            new AttributeContainer(
                    ModAttributes.GLINTSTONE_SPELL_POWER,
                    0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            );

    public CelestialMageArmorItem(ArmorItem.Type armorType, Properties properties) {
        super(
                ExtendedArmorMaterials.WIZARD,
                armorType,
                properties
        );
    }

    /**
     * 在铁魔法材质属性（防御 / 韧性 / 法力上限 / 全学派法强）之上补一条辉石法强。
     * <p>
     * 必须覆写而不是走构造器末尾的 {@code AttributeContainer...}：1.20.1 的
     * {@code ExtendedArmorItem} 构造器收下这些容器后并不读取，属性完全由 armor material
     * 的 {@code getAdditionalAttributes()} 决定（见其 {@code platformHandleDefaultModifiers}）。
     * <p>
     * 用 {@link AttributeContainer#createModifier(String)} + 槽位名生成修饰符，四个部位各自得到
     * 不同 UUID，避免同属性上同 UUID 的修饰符互相覆盖（与铁魔法 {@code StaffItem} 同一套做法）。
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        Multimap<Attribute, AttributeModifier> baseModifiers = super.getDefaultAttributeModifiers(slot);
        if (slot != this.getEquipmentSlot()) {
            return baseModifiers;
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .putAll(baseModifiers)
                .put(
                        ModAttributes.GLINTSTONE_SPELL_POWER.get(),
                        GLINTSTONE_SPELL_POWER_CONTAINER.createModifier(slot.getName())
                )
                .build();
    }

    /**
     * 四个部位共用星辰法师模型；铁魔法渲染器按装备槽控制对应骨骼显隐。
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(
                new GenericArmorModel<CelestialMageArmorItem>(
                        EldenRingSpellsMod.MOD_ID,
                        "celestial_mage"
                )
        );
    }
}
