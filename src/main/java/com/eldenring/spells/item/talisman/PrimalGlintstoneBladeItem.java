package com.eldenring.spells.item.talisman;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 源辉石刀：佩戴时降低最大生命值，并由独立效果类提供全学派蓝耗减免。
 * <p>
 * 1.20.1 的 Curios {@code getAttributeModifiers} 形参是 {@code UUID}、返回键类型是 {@code Attribute}
 * （1.21+ 才换成 {@code ResourceLocation} + {@code Holder<Attribute>}）。
 * 同一次调用里的两个修饰符挂在不同属性上，可以共用 Curios 给的同一个 UUID。
 */
public final class PrimalGlintstoneBladeItem extends TalismanItem {
    public PrimalGlintstoneBladeItem(Properties properties) {
        super(properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            UUID modifierId,
            ItemStack stack
    ) {
        Multimap<Attribute, AttributeModifier> modifiers =
                LinkedHashMultimap.create();
        modifiers.put(
                Attributes.MAX_HEALTH,
                new AttributeModifier(
                        modifierId,
                        "iss_elden_ring.primal_glintstone_blade.max_health",
                        -PrimalGlintstoneBladeEffect.maxHealthReduction(),
                        AttributeModifier.Operation.MULTIPLY_TOTAL
                )
        );
        modifiers.put(
                AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(
                        modifierId,
                        "iss_elden_ring.primal_glintstone_blade.spell_power",
                        PrimalGlintstoneBladeEffect.spellPowerBonus(),
                        AttributeModifier.Operation.MULTIPLY_BASE
                )
        );
        return modifiers;
    }

    /**
     * 1.20.1 的悬浮提示签名是 {@code (ItemStack, Level, List, TooltipFlag)}；1.21 才改成 {@code TooltipContext}。
     */
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.primal_glintstone_blade.effect")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.primal_glintstone_blade.lore_1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.primal_glintstone_blade.lore_2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
