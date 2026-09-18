package com.eldenring.spells.item.talisman;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;

import java.util.List;

/**
 * 源辉石刀：佩戴时降低最大生命值，并由独立效果类提供全学派蓝耗减免。
 */
public final class PrimalGlintstoneBladeItem extends TalismanItem {
    public PrimalGlintstoneBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            ResourceLocation modifierId,
            ItemStack stack
    ) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers =
                LinkedHashMultimap.create();
        modifiers.put(
                Attributes.MAX_HEALTH,
                new AttributeModifier(
                        modifierId,
                        -PrimalGlintstoneBladeEffect.maxHealthReduction(),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                )
        );
        return modifiers;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.primal_glintstone_blade.effect")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.primal_glintstone_blade.lore_1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.elden_ring_spells.primal_glintstone_blade.lore_2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
