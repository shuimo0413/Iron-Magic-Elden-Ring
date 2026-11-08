package com.eldenring.spells.entity.academy;

import com.eldenring.spells.registry.ModSpells;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 一间教室学者的施法表，对应一份 {@code data/<命名空间>/mob_spellcasting/<实体ID>.json}。
 * <p>
 * 四个列表与铁魔法 {@code WizardAttackGoal#setSpells} 的攻击 / 防御 / 位移 / 辅助分类一一对应；
 * 同一法术写多次即提高它在该分类里被抽中的概率。法术伤害、弹速等仍由法术自身的配置决定。
 * <pre>
 * {
 *   "attack":   ["iss_elden_ring:glintstone_pebble"],
 *   "defense":  [],
 *   "movement": [],
 *   "support":  [],
 *   "spell_quality": { "min": 0.0, "max": 0.0 }
 * }
 * </pre>
 *
 * @param minSpellQuality 施法等级下限 = 法术最高等级 × 该比例（向下取整，至少 1 级）；0 → 固定 1 级
 * @param maxSpellQuality 施法等级上限比例；每次施法在 [min, max] 间随机
 */
public record AcademyScholarSpellConfig(
        List<AbstractSpell> attackSpells,
        List<AbstractSpell> defenseSpells,
        List<AbstractSpell> movementSpells,
        List<AbstractSpell> supportSpells,
        float minSpellQuality,
        float maxSpellQuality
) {

    /** 未写 {@code spell_quality.min} 时的等级比例。0 → 学者只放 1 级法术；调大 → 默认用更高等级的法术。 */
    public static final float DEFAULT_MIN_SPELL_QUALITY = 0.0F;

    /** 未写 {@code spell_quality.max} 时的等级比例。与下限同为 0 → 固定 1 级。 */
    public static final float DEFAULT_MAX_SPELL_QUALITY = 0.0F;

    private static final String ATTACK_KEY = "attack";
    private static final String DEFENSE_KEY = "defense";
    private static final String MOVEMENT_KEY = "movement";
    private static final String SUPPORT_KEY = "support";
    private static final String SPELL_QUALITY_KEY = "spell_quality";

    /**
     * 文件缺失或解析失败时的保底施法表：只放辉石魔砾，与随模组附带的默认 JSON 内容一致。
     */
    public static AcademyScholarSpellConfig fallback() {
        return new AcademyScholarSpellConfig(
                List.of(ModSpells.GLINTSTONE_PEBBLE.get()),
                List.of(),
                List.of(),
                List.of(),
                DEFAULT_MIN_SPELL_QUALITY,
                DEFAULT_MAX_SPELL_QUALITY
        );
    }

    /**
     * 解析并校验一份 JSON。任何字段类型错误、未知法术 ID、等级比例越界或四个列表全空都会抛出
     * {@link JsonParseException}，由加载器记录文件路径与原因后改用 {@link #fallback()}。
     */
    public static AcademyScholarSpellConfig parse(JsonElement rootElement) {
        if (!rootElement.isJsonObject()) {
            throw new JsonParseException("root must be a JSON object");
        }
        JsonObject root = rootElement.getAsJsonObject();
        List<AbstractSpell> attackSpells = parseSpellList(root, ATTACK_KEY);
        List<AbstractSpell> defenseSpells = parseSpellList(root, DEFENSE_KEY);
        List<AbstractSpell> movementSpells = parseSpellList(root, MOVEMENT_KEY);
        List<AbstractSpell> supportSpells = parseSpellList(root, SUPPORT_KEY);
        if (attackSpells.isEmpty() && defenseSpells.isEmpty() && movementSpells.isEmpty() && supportSpells.isEmpty()) {
            throw new JsonParseException("at least one spell is required in attack/defense/movement/support");
        }

        float minSpellQuality = DEFAULT_MIN_SPELL_QUALITY;
        float maxSpellQuality = DEFAULT_MAX_SPELL_QUALITY;
        if (root.has(SPELL_QUALITY_KEY)) {
            JsonElement qualityElement = root.get(SPELL_QUALITY_KEY);
            if (!qualityElement.isJsonObject()) {
                throw new JsonParseException("'" + SPELL_QUALITY_KEY + "' must be an object with 'min' and 'max'");
            }
            JsonObject quality = qualityElement.getAsJsonObject();
            minSpellQuality = parseQuality(quality, "min", minSpellQuality);
            maxSpellQuality = parseQuality(quality, "max", maxSpellQuality);
            if (minSpellQuality > maxSpellQuality) {
                throw new JsonParseException("'" + SPELL_QUALITY_KEY + ".min' must not exceed '" + SPELL_QUALITY_KEY + ".max'");
            }
        }
        return new AcademyScholarSpellConfig(
                attackSpells,
                defenseSpells,
                movementSpells,
                supportSpells,
                minSpellQuality,
                maxSpellQuality
        );
    }

    private static List<AbstractSpell> parseSpellList(JsonObject root, String key) {
        if (!root.has(key)) {
            return List.of();
        }
        JsonElement listElement = root.get(key);
        if (!listElement.isJsonArray()) {
            throw new JsonParseException("'" + key + "' must be an array of spell ids");
        }
        JsonArray spellIdArray = listElement.getAsJsonArray();
        List<AbstractSpell> spells = new ArrayList<>(spellIdArray.size());
        for (int index = 0; index < spellIdArray.size(); index++) {
            JsonElement spellIdElement = spellIdArray.get(index);
            if (!spellIdElement.isJsonPrimitive() || !spellIdElement.getAsJsonPrimitive().isString()) {
                throw new JsonParseException("'" + key + "[" + index + "]' must be a spell id string");
            }
            String rawSpellId = spellIdElement.getAsString();
            ResourceLocation spellId = ResourceLocation.tryParse(rawSpellId);
            if (spellId == null) {
                throw new JsonParseException("'" + key + "[" + index + "]' is not a valid id: " + rawSpellId);
            }
            AbstractSpell spell = SpellRegistry.getSpell(spellId);
            if (spell == SpellRegistry.none()) {
                throw new JsonParseException("'" + key + "[" + index + "]' is not a registered spell: " + rawSpellId);
            }
            spells.add(spell);
        }
        return List.copyOf(spells);
    }

    private static float parseQuality(JsonObject quality, String key, float defaultValue) {
        if (!quality.has(key)) {
            return defaultValue;
        }
        JsonElement valueElement = quality.get(key);
        if (!valueElement.isJsonPrimitive() || !valueElement.getAsJsonPrimitive().isNumber()) {
            throw new JsonParseException("'" + SPELL_QUALITY_KEY + "." + key + "' must be a number");
        }
        float value = valueElement.getAsFloat();
        if (value < 0.0F || value > 1.0F) {
            throw new JsonParseException("'" + SPELL_QUALITY_KEY + "." + key + "' must be between 0 and 1, got " + value);
        }
        return value;
    }
}
