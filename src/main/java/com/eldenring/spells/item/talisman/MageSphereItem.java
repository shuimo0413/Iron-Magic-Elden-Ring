package com.eldenring.spells.item.talisman;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 魔法师球护符：Curios 护符。佩戴时提供全局法术强度 +5%（铁魔法 {@link AttributeRegistry#SPELL_POWER}）。
 * <p>
 * 1.20.1 的 Curios {@code getAttributeModifiers} 形参是 {@code UUID}、返回键类型是 {@code Attribute}
 * （1.21+ 才换成 {@code ResourceLocation} + {@code Holder<Attribute>}），所以这里用 Curios 给的
 * UUID 构造 {@link AttributeModifier}，属性本体从 {@code RegistryObject} 取 {@code .get()}。
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
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            SlotContext slotContext,
            UUID modifierId,
            ItemStack stack
    ) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        modifiers.put(
                AttributeRegistry.SPELL_POWER.get(),
                new AttributeModifier(
                        modifierId,
                        "iss_elden_ring.mage_sphere.spell_power",
                        SPELL_POWER_BONUS,
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
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.mage_sphere.effect")
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.mage_sphere.lore_1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.mage_sphere.lore_2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.iss_elden_ring.mage_sphere.lore_3")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
