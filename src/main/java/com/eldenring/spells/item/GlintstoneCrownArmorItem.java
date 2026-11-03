package com.eldenring.spells.item;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModAttributes;
import io.redspace.ironsspellbooks.entity.armor.GenericArmorModel;
import io.redspace.ironsspellbooks.entity.armor.GenericCustomArmorRenderer;
import io.redspace.ironsspellbooks.item.armor.ImbuableChestplateArmorItem;
import io.redspace.ironsspellbooks.registries.ArmorMaterialRegistry;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
/**
 * 学院四种辉石头冠共用铁魔法学派护甲属性，各自使用独立的原始 Blockbench 几何与贴图。
 */
public final class GlintstoneCrownArmorItem extends ImbuableChestplateArmorItem {
    private final String crownName;

    /** 头冠名称同时用于默认的 <name>_armor.geo.json 几何和 <name>.png 贴图。 */
    public GlintstoneCrownArmorItem(String crownName, Item.Properties properties) {
        super(ArmorMaterialRegistry.SCHOOL, ArmorItem.Type.HELMET, properties,
                schoolAttributes(ModAttributes.GLINTSTONE_SPELL_POWER));
        this.crownName = crownName;
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
