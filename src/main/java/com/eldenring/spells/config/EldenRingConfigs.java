package com.eldenring.spells.config;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.spell.SpellBookStatReloader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

/**
 * 注册 NeoForge 配置并在加载 / 热重载时写回 Spell / 矿洞运行时字段。
 * <p>
 * 整合包改数值的入口：
 * <ul>
 *   <li>{@code config/elden_ring_spells-server.toml} — 伤害、弹速、范围、蓝耗基数等玩法数字</li>
 *   <li>{@code config/elden_ring_spells-common.toml} — 辉石矿洞密度</li>
 *   <li>{@code config/irons_spellbooks_spell_config/elden_ring_spells/*.json} — 冷却、最大等级、开关、蓝耗/法强倍率</li>
 * </ul>
 */
public final class EldenRingConfigs {

    private EldenRingConfigs() {
    }

    public static void register(ModContainer modContainer, IEventBus modEventBus) {
        modContainer.registerConfig(ModConfig.Type.SERVER, EldenRingServerConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.COMMON, EldenRingCommonConfig.SPEC);
        modEventBus.addListener(EldenRingConfigs::onModConfig);
    }

    private static void onModConfig(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        if (config.getSpec() == EldenRingServerConfig.SPEC) {
            EldenRingServerConfig.apply();
            SpellBookStatReloader.reloadAll();
            EldenRingSpellsMod.LOGGER.info("Applied elden_ring_spells-server.toml to Spell fields.");
        } else if (config.getSpec() == EldenRingCommonConfig.SPEC) {
            EldenRingCommonConfig.apply();
            EldenRingSpellsMod.LOGGER.info("Applied elden_ring_spells-common.toml to cave fields.");
        }
    }
}
