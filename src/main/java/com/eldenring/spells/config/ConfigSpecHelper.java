package com.eldenring.spells.config;

import net.minecraftforge.common.ForgeConfigSpec;
/**
 * {@link ForgeConfigSpec.Builder} 的短注释封装，避免每个键重复写 define 样板。
 * <p>
 * 默认值一律从 Spell / CommonConfig 运行时字段传入，保证「第一次生成的 toml」和源码默认一致。
 */
final class ConfigSpecHelper {

    private ConfigSpecHelper() {
    }

    static ForgeConfigSpec.DoubleValue floating(
            ForgeConfigSpec.Builder builder,
            String name,
            String comment,
            double defaultValue,
            double min,
            double max
    ) {
        return builder.comment(comment).defineInRange(name, defaultValue, min, max);
    }

    static ForgeConfigSpec.IntValue integer(
            ForgeConfigSpec.Builder builder,
            String name,
            String comment,
            int defaultValue,
            int min,
            int max
    ) {
        return builder.comment(comment).defineInRange(name, defaultValue, min, max);
    }
}
