package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.StarlightEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.List;

/**
 * 星光（Starlight）：瞬时在头顶挂一颗跟随的青色小星，持续照亮周围。
 * <p>
 * 照明抄火把：原版 {@code Blocks.LIGHT} 亮度 14。原版方块光没有颜色通道，
 * 青色只做在星星模型和火星粒子上。再施放会替换自己的旧星。
 */
public class StarlightSpell extends EldenRingAbstractSpell {

    /** 1 级蓝耗。照明工具咒，低于魔砾。 */
    public static int SPELL_BASE_MANA_COST = 12;

    /** 每升 1 级额外蓝耗。本咒默认 1 级，留给 toml。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /**
     * 1 级法术强度。不参与伤害；仍保留以便铁魔法 UI / 等级曲线合法。
     */
    public static int SPELL_BASE_SPELL_POWER = 1;

    /** 每级额外法术强度。本咒不打伤害，默认 0。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 0;

    /** 吟唱 tick。0 = 瞬时出星。 */
    public static int SPELL_CAST_TIME_TICKS = 0;

    /**
     * 冷却（秒）。时长 120 秒，冷却只需挡住连点刷实体。
     */
    public static double SPELL_COOLDOWN_SECONDS = 2.0;

    /** 最大等级。法环原作不升级。 */
    public static int SPELL_MAX_LEVEL = 1;

    /**
     * 星星持续（tick）。20 tick = 1 秒；2400 = 120 秒（贴近法环原作）。
     * 调大 → 一次施放罩更久；调小 → 更常补放。
     */
    public static int STAR_DURATION_TICKS = 2400;

    /**
     * 中心光源亮度（0–15）。14 = 原版火把。
     * 调大 → 照得更远；调小 → 更像微光。
     */
    public static int LIGHT_LEVEL = 14;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "starlight");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(StarlightSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(StarlightSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public StarlightSpell() {
        this.manaCostPerLevel = StarlightSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = StarlightSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = StarlightSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = StarlightSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = StarlightSpell.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.duration",
                        Utils.timeFromTicks(StarlightSpell.STAR_DURATION_TICKS, 1)
                )
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /**
     * 铁魔法瞄准指示器颜色（RGB 0–1）。星光图标那一档青。
     */
    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.22f, 0.88f, 0.92f);
    }

    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        if (!level.isClientSide) {
            discardOwnedStars(level, castingEntity);
            AcademySigilFx.spawnAboveHead(level, castingEntity);

            StarlightEntity starlightEntity = new StarlightEntity(
                    level,
                    castingEntity,
                    StarlightSpell.STAR_DURATION_TICKS,
                    StarlightSpell.LIGHT_LEVEL
            );
            level.addFreshEntity(starlightEntity);
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 丢掉该施法者已有的星光。只搜周围 64 格：星星始终钉在头上，不会离主人更远。
     */
    private static void discardOwnedStars(Level level, LivingEntity owner) {
        double searchPaddingBlocks = 64.0;
        AABB searchBox = owner.getBoundingBox().inflate(searchPaddingBlocks);
        for (StarlightEntity existingStar : level.getEntitiesOfClass(StarlightEntity.class, searchBox)) {
            if (existingStar.getOwner() == owner) {
                existingStar.discard();
            }
        }
    }
}
