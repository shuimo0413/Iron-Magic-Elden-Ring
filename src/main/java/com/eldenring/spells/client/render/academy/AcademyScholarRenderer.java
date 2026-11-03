package com.eldenring.spells.client.render.academy;

import com.eldenring.spells.item.RayaLucariaRobeArmorItem;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMobRenderer;
import io.redspace.ironsspellbooks.util.DefaultBipedBoneIdents;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * 学院学者渲染器：复用铁魔法通用施法者渲染管线与装备层，四间教室共用。
 * <p>
 * 额外挂一层 {@link RobeSkirtLayer}：铁魔法生物装备层按身体部位逐块画装备，腿部骨骼只取腿甲槽，
 * 而雷亚卢卡利亚长袍的下摆骨骼（{@code armorTorsoExtension*Leg}）属于胸甲、只在腿部位显示，
 * 学者不穿腿甲时下摆会整块缺失。这一层在腿甲槽为空时，把长袍按胸甲槽再画到两条腿上，只露出下摆。
 */
public final class AcademyScholarRenderer extends AbstractSpellCastingMobRenderer {

    public AcademyScholarRenderer(EntityRendererProvider.Context context) {
        super(context, new AcademyScholarModel());
        addRenderLayer(new RobeSkirtLayer(this));
    }

    /**
     * 只处理左右腿主护甲骨骼（不碰 {@code _2} 副骨骼，避免下摆画两遍），且只对雷亚卢卡利亚长袍生效：
     * 其他整套几何的胸甲按腿部位渲染会带出腿甲造型。
     */
    private static final class RobeSkirtLayer extends ItemArmorGeoLayer<AbstractSpellCastingMob> {

        private static final String LEFT_LEG_ARMOR_BONE = DefaultBipedBoneIdents.LEFT_LEG_ARMOR_BONE_IDENT;
        private static final String RIGHT_LEG_ARMOR_BONE = DefaultBipedBoneIdents.RIGHT_LEG_ARMOR_BONE_IDENT;

        private RobeSkirtLayer(GeoRenderer<AbstractSpellCastingMob> renderer) {
            super(renderer);
        }

        @Nullable
        @Override
        protected ItemStack getArmorItemForBone(GeoBone bone, AbstractSpellCastingMob animatable) {
            String boneName = bone.getName();
            if (!LEFT_LEG_ARMOR_BONE.equals(boneName) && !RIGHT_LEG_ARMOR_BONE.equals(boneName)) {
                return null;
            }
            if (this.leggingsStack != null && !this.leggingsStack.isEmpty()) {
                return null;
            }
            if (this.chestplateStack == null || !(this.chestplateStack.getItem() instanceof RayaLucariaRobeArmorItem)) {
                return null;
            }
            return this.chestplateStack;
        }

        @Nonnull
        @Override
        protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, AbstractSpellCastingMob animatable) {
            return EquipmentSlot.CHEST;
        }

        @Nonnull
        @Override
        protected ModelPart getModelPartForBone(
                GeoBone bone,
                EquipmentSlot slot,
                ItemStack stack,
                AbstractSpellCastingMob animatable,
                HumanoidModel<?> baseModel
        ) {
            return LEFT_LEG_ARMOR_BONE.equals(bone.getName()) ? baseModel.leftLeg : baseModel.rightLeg;
        }
    }
}
