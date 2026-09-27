package com.eldenring.spells.registry;

import com.eldenring.spells.entity.astrologer.AstrologerEntity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
/**
 * 生物实体默认属性创建。观星者等常驻 NPC 的属性集中在此注册。
 */
public final class ModEntityAttributes {
    private ModEntityAttributes() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModEntityAttributes::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ASTROLOGER.get(), AstrologerEntity.prepareAttributes().build());
    }
}
