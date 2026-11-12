package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.registry.ModAttachments;
import com.eldenring.spells.registry.ModEffects;
import net.minecraft.tags.DamageTypeTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.minecraft.world.entity.LivingEntity;

@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class MagicShieldEvents {
    private MagicShieldEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void absorb(LivingDamageEvent.Pre event) {
        LivingEntity owner = event.getEntity();
        if (owner.level().isClientSide || !owner.hasEffect(ModEffects.MAGIC_SHIELD)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || event.getSource().is(DamageTypeTags.BYPASSES_EFFECTS)) return;
        var shield = owner.getData(ModAttachments.MAGIC_SHIELD).entity;
        if (shield != null && !shield.isRemoved() && shield.level() == owner.level()) {
            event.setNewDamage(shield.absorb(event.getNewDamage(), event.getSource().getSourcePosition()));
        }
    }

    /** 死亡、退出/重进、跨维度、牛奶清除后，临时引用与状态不残留。 */
    @SubscribeEvent
    public static void cleanup(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity owner) || owner.level().isClientSide
                || (!owner.hasData(ModAttachments.MAGIC_SHIELD) && !owner.hasEffect(ModEffects.MAGIC_SHIELD))) return;
        var state = owner.getData(ModAttachments.MAGIC_SHIELD);
        var shield = state.entity;
        if (shield == null || shield.isRemoved() || shield.level() != owner.level()
                || !owner.isAlive() || !owner.hasEffect(ModEffects.MAGIC_SHIELD)) {
            if (shield != null) shield.discard();
            state.entity = null;
            owner.removeEffect(ModEffects.MAGIC_SHIELD);
        }
    }
}
