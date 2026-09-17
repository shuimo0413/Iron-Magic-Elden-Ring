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
 * 星辰法师套装。沿用铁魔法学派护甲材质、胸甲灌注槽与属性构建逻辑。
 */
public final class CelestialMageArmorItem extends ImbuableChestplateArmorItem {
    public CelestialMageArmorItem(ArmorItem.Type armorType, Properties properties) {
        super(
                ArmorMaterialRegistry.SCHOOL,
                armorType,
                properties,
                schoolAttributes(ModAttributes.GLINTSTONE_SPELL_POWER)
        );
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
