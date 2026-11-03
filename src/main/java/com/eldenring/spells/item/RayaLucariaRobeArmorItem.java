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
 * 雷亚卢卡利亚学院长袍：占用胸甲槽，沿用现有辉石学派护甲属性与胸甲灌注机制。
 * 模型的躯干、袖子和下摆由同一件胸甲显示，不额外占用腿部装备槽。
 * <p>
 * 1.20.1 护甲材质改用 {@link ExtendedArmorMaterials#WIZARD}，辉石法强在属性覆写里补（同 {@link AstrologerArmorItem}）。
 */
public final class RayaLucariaRobeArmorItem extends ImbuableChestplateArmorItem {

    /** 辉石法术强度加成：+10%（乘算基值），与 1.21.1 分支 {@code schoolAttributes} 的单件数值一致。 */
    private static final AttributeContainer GLINTSTONE_SPELL_POWER_CONTAINER =
            new AttributeContainer(
                    ModAttributes.GLINTSTONE_SPELL_POWER,
                    0.10D,
                    AttributeModifier.Operation.MULTIPLY_BASE
            );

    /** 使用与观星者长袍一致的学派护甲材质、法术属性和外部传入的耐久设置。 */
    public RayaLucariaRobeArmorItem(Properties properties) {
        super(ExtendedArmorMaterials.WIZARD, ArmorItem.Type.CHESTPLATE, properties);
    }

    /** 在铁魔法材质属性之上补一条辉石法强（原因见 {@link AstrologerArmorItem}）。 */
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

    /** 复用铁魔法护甲渲染器，让长袍骨骼跟随穿戴者的躯干和双臂动作。 */
    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(
                new GenericArmorModel<RayaLucariaRobeArmorItem>(
                        EldenRingSpellsMod.MOD_ID, "raya_lucaria_robe"
                )
        );
    }
}
