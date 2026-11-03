package com.eldenring.spells.entity.academy;

import com.eldenring.spells.EldenRingSpellsMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务端 datapack 加载器：读取 {@code data/<命名空间>/mob_spellcasting/*.json}，
 * 资源 ID 与学者实体 ID 相同（如 {@code iss_elden_ring:karolos_scholar}）。
 * <p>
 * 每次 {@code /reload} 或进世界都会整体替换配置表并递增 {@link #revision()}；
 * {@link AcademyScholarAttackGoal} 在下一次选咒前发现版本变化后重新套用，存量学者无需重生。
 * 文件缺失或无效时记录日志并使用 {@link AcademyScholarSpellConfig#fallback()}，不会让服务器崩溃。
 */
public final class AcademyScholarSpellConfigLoader extends SimpleJsonResourceReloadListener {

    /** datapack 子目录名；两条分支一致，不受原版单复数目录改名影响。 */
    public static final String DIRECTORY = "mob_spellcasting";

    private static final Gson GSON = new GsonBuilder().create();

    private static volatile Map<ResourceLocation, AcademyScholarSpellConfig> configsById = Map.of();

    private static volatile int revision;

    public AcademyScholarSpellConfigLoader() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonById, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, AcademyScholarSpellConfig> parsedConfigs = new HashMap<>();
        jsonById.forEach((configId, json) -> {
            try {
                parsedConfigs.put(configId, AcademyScholarSpellConfig.parse(json));
            } catch (RuntimeException parseError) {
                EldenRingSpellsMod.LOGGER.error(
                        "Invalid academy scholar spell config data/{}/{}/{}.json: {}. Using the built-in Glintstone Pebble fallback.",
                        configId.getNamespace(), DIRECTORY, configId.getPath(), parseError.getMessage()
                );
            }
        });
        for (AcademyClassroom classroom : AcademyClassroom.values()) {
            ResourceLocation expectedConfigId = classroom.spellConfigId();
            if (!jsonById.containsKey(expectedConfigId)) {
                EldenRingSpellsMod.LOGGER.warn(
                        "Missing academy scholar spell config data/{}/{}/{}.json. Using the built-in Glintstone Pebble fallback.",
                        expectedConfigId.getNamespace(), DIRECTORY, expectedConfigId.getPath()
                );
            }
        }
        configsById = Map.copyOf(parsedConfigs);
        revision++;
        EldenRingSpellsMod.LOGGER.info("Loaded {} academy scholar spell config(s).", parsedConfigs.size());
    }

    /** 取某间教室的施法表；未加载到有效文件时返回保底的辉石魔砾配置。 */
    public static AcademyScholarSpellConfig get(ResourceLocation configId) {
        AcademyScholarSpellConfig config = configsById.get(configId);
        return config != null ? config : AcademyScholarSpellConfig.fallback();
    }

    /** 配置表版本号，每次重载 +1；AI 用它判断是否需要重新套用施法表。 */
    public static int revision() {
        return revision;
    }
}
