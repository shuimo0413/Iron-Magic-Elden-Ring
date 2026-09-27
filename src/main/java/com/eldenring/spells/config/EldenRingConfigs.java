package com.eldenring.spells.config;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.spell.SpellBookStatReloader;
import com.eldenring.spells.item.AzurStaffBalance;
import com.eldenring.spells.item.talisman.PrimalGlintstoneBladeEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
/**
 * 注册 Forge 配置并在加载 / 热重载时写回 Spell / 矿洞运行时字段。
 * <p>
 * 整合包改数值的入口：
 * <ul>
 *   <li>{@code config/iss_elden_ring-server.toml} — 伤害、弹速、范围、蓝耗基数等玩法数字</li>
 *   <li>{@code config/iss_elden_ring-common.toml} — 辉石矿洞密度</li>
 *   <li>{@code config/irons_spellbooks_spell_config/iss_elden_ring/*.json} — 冷却、最大等级、开关、蓝耗/法强倍率</li>
 * </ul>
 */
public final class EldenRingConfigs {

    private EldenRingConfigs() {
    }

    /**
     * Forge 1.20.1 不支持 {@code (IEventBus, ModContainer)} 构造器注入，改由
     * {@link ModLoadingContext#get()} 拿到本模组的配置注册入口；mod 总线仍从入口传入。
     */
    public static void register(IEventBus modEventBus) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EldenRingServerConfig.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, EldenRingCommonConfig.SPEC);
        modEventBus.addListener(EldenRingConfigs::onModConfig);
    }

    private static void onModConfig(ModConfigEvent event) {
        ModConfig config = event.getConfig();
        // Unloading 已清除底层配置；此时读取 ConfigValue 会导致退出世界异常。
        if (event instanceof ModConfigEvent.Unloading) {
            if (config.getSpec() == EldenRingServerConfig.SPEC) {
                AzurStaffBalance.resetDefaults();
                PrimalGlintstoneBladeEffect.reset();
            }
            return;
        }
        if (config.getSpec() == EldenRingServerConfig.SPEC) {
            EldenRingServerConfig.apply();
            SpellBookStatReloader.reloadAll();
            EldenRingSpellsMod.LOGGER.info("Applied iss_elden_ring-server.toml to Spell fields.");
        } else if (config.getSpec() == EldenRingCommonConfig.SPEC) {
            EldenRingCommonConfig.apply();
            EldenRingSpellsMod.LOGGER.info("Applied iss_elden_ring-common.toml to cave fields.");
        }
    }
}
