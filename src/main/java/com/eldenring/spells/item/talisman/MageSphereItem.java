package com.eldenring.spells.item.talisman;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * 魔法师球护符：Curios 护符。佩戴时提供全局法术强度 +5%（铁魔法 {@link AttributeRegistry#SPELL_POWER}）。
 */
public final class MageSphereItem extends TalismanItem {

    /**
     * 全局法术强度乘数加成。0.05 = +5%；调大则全学派伤害同步上升。
     */
    private static final double SPELL_POWER_BONUS = 0.05D;

    public MageSphereItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            ResourceLocation modifierId,
            ItemStack stack
    ) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(
                AttributeRegistry.SPELL_POWER,
                new AttributeModifier(
                        modifierId,
                        SPELL_POWER_BONUS,
                        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                )
        );
        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.mage_sphere.effect")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.mage_sphere.lore_1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.mage_sphere.lore_2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.mage_sphere.lore_3")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
