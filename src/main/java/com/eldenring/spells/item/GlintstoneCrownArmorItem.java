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
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 学院四种辉石头冠共用铁魔法学派护甲属性，各自使用独立的原始 Blockbench 几何与贴图。
 * <p>
 * 1.20.1 没有 {@code ArmorMaterialRegistry.SCHOOL}，与 {@link AstrologerArmorItem} 一样改用
 * {@link ExtendedArmorMaterials#WIZARD}，并覆写属性补上辉石法术强度。
 */
public final class GlintstoneCrownArmorItem extends ImbuableChestplateArmorItem {

    /** 辉石法术强度加成：+10%（乘算基值），与 1.21.1 分支 {@code schoolAttributes} 的单件数值一致。 */
    private static final AttributeContainer GLINTSTONE_SPELL_POWER_CONTAINER =
            new AttributeContainer(
                    ModAttributes.GLINTSTONE_SPELL_POWER,
                    0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            );

    private final String crownName;

    /** 头冠名称同时用于默认的 <name>_armor.geo.json 几何和 <name>.png 贴图。 */
    public GlintstoneCrownArmorItem(String crownName, Item.Properties properties) {
        super(ExtendedArmorMaterials.WIZARD, ArmorItem.Type.HELMET, properties);
        this.crownName = crownName;
    }

    /**
     * 在铁魔法材质属性之上补一条辉石法强。1.20.1 的 {@code ExtendedArmorItem} 构造器不读取
     * {@code AttributeContainer}，必须在这里覆写（原因详见 {@link AstrologerArmorItem}）。
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

    /** 沿用铁魔法默认护甲渲染器，按约定查找头冠几何和贴图。 */
    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(
                new GenericArmorModel<GlintstoneCrownArmorItem>(EldenRingSpellsMod.MOD_ID, crownName)
        );
    }
}
