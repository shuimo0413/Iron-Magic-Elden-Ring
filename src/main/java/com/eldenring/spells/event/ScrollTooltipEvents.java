package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import io.redspace.ironsspellbooks.api.events.CustomizeScrollModNameEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 卷轴 tooltip 调整：铁魔法会对「非本体」法术卷轴自动插入一行模组显示名，
 * 本体卷轴没有这行；本模组取消该行，与原版铁魔法卷轴观感一致。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class ScrollTooltipEvents {
    private ScrollTooltipEvents() {
    }

    /**
     * 取消本模组法术卷轴上的模组名行（{@code CustomizeScrollModNameEvent}）。
     */
    @SubscribeEvent
    public static void hideAddonModNameOnScroll(CustomizeScrollModNameEvent event) {
        if (EldenRingSpellsMod.MOD_ID.equals(event.getModId())) {
            event.setCanceled(true);
        }
    }
}
