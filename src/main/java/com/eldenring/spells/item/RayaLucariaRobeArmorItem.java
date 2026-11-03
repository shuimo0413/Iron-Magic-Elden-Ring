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
 * 雷亚卢卡利亚学院长袍：占用胸甲槽，沿用现有辉石学派护甲属性与胸甲灌注机制。
 * 模型的躯干、袖子和下摆由同一件胸甲显示，不额外占用腿部装备槽。
 */
public final class RayaLucariaRobeArmorItem extends ImbuableChestplateArmorItem {
    /** 使用与观星者长袍一致的学派护甲材质、法术属性和外部传入的耐久设置。 */
    public RayaLucariaRobeArmorItem(Properties properties) {
        super(ArmorMaterialRegistry.SCHOOL, ArmorItem.Type.CHESTPLATE, properties,
                schoolAttributes(ModAttributes.GLINTSTONE_SPELL_POWER));
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
