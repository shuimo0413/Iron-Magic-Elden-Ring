package com.eldenring.spells.event;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.command.AcademyTerrainCheckCommand;
import com.eldenring.spells.command.ExportAcademyGridCommand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * 注册开发 / 运维向命令（游戏事件总线）。
 */
@EventBusSubscriber(modid = EldenRingSpellsMod.MOD_ID)
public final class ModCommandEvents {
    private ModCommandEvents() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        ExportAcademyGridCommand.register(event.getDispatcher());
        AcademyTerrainCheckCommand.register(event.getDispatcher());
    }
}
