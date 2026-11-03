package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.academy.AcademyScholarSpellConfigLoader;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 在服务端 datapack 重载链里挂上学院学者施法表加载器（游戏事件总线，不是 mod 总线）。
 */
@Mod.EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class AcademyScholarReloadEvents {
    private AcademyScholarReloadEvents() {
    }

    @SubscribeEvent
    public static void addAcademyScholarSpellConfigLoader(AddReloadListenerEvent event) {
        event.addListener(new AcademyScholarSpellConfigLoader());
    }
}
