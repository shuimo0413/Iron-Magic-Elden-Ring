package com.eldenring.spells.item;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModAttributes;
import io.redspace.ironsspellbooks.entity.armor.GenericArmorModel;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.registries.ArmorMaterialRegistry;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

/**
 * 观星者套装。观星者 NPC 默认穿着；属性与学派护甲同级，长袍带灌注槽。
 */
public final class AstrologerArmorItem extends ImbuableChestplateArmorItem {
    public AstrologerArmorItem(ArmorItem.Type armorType, Properties properties) {
        super(
                ArmorMaterialRegistry.SCHOOL,
                armorType,
                properties,
                schoolAttributes(ModAttributes.GLINTSTONE_SPELL_POWER)
        );
    }

    /**
     * 四个部位共用观星者模型；铁魔法渲染器按装备槽控制对应骨骼显隐。
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public GeoArmorRenderer<?> supplyRenderer() {
        return new GenericCustomArmorRenderer<>(
                new GenericArmorModel<AstrologerArmorItem>(
                        EldenRingSpellsMod.MOD_ID,
                        "astrologer"
                )
        );
    }
}
