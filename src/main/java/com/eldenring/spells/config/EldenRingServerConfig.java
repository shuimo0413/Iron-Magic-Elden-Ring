package com.eldenring.spells.config;

import com.eldenring.spells.particle.cometazur.CometAzurFx;
import com.eldenring.spells.spell.CannonOfHaimaSpell;
import com.eldenring.spells.spell.CarianGreatswordSpell;
import com.eldenring.spells.spell.CarianPhalanxSpell;
import com.eldenring.spells.spell.CarianSlicerSpell;
import com.eldenring.spells.spell.CometAzurSpell;
import com.eldenring.spells.spell.CometSpell;
import com.eldenring.spells.spell.CrystalBarrageSpell;
import com.eldenring.spells.spell.CrystalBurstSpell;
import com.eldenring.spells.spell.FoundingRainOfStarsSpell;
import com.eldenring.spells.spell.GavelOfHaimaSpell;
import com.eldenring.spells.spell.GlintbladePhalanxSpell;
import com.eldenring.spells.spell.GlintstoneArcSpell;
import com.eldenring.spells.spell.GlintstoneCometSpell;
import com.eldenring.spells.spell.GlintstonePebbleSpell;
import com.eldenring.spells.spell.GlintstoneStarsSpell;
import com.eldenring.spells.spell.GreatGlintstoneShardSpell;
import com.eldenring.spells.spell.GreatbladePhalanxSpell;
import com.eldenring.spells.spell.MagicGlintbladeSpell;
import com.eldenring.spells.spell.SpiralShardSpell;
import com.eldenring.spells.spell.StarShowerSpell;
import com.eldenring.spells.spell.StarlightSpell;
import com.eldenring.spells.spell.StarsOfRuinSpell;
import com.eldenring.spells.spell.SwiftGlintstoneShardSpell;
import com.eldenring.spells.spell.TerraMagicaSpell;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 玩法数值（伤害系数、弹速、范围、蓝耗基数、吟唱 tick 等）。
 * <p>
 * 文件位置：
 * <ul>
 *   <li>全局默认：{@code config/elden_ring_spells-server.toml}</li>
 *   <li>单世界覆盖：{@code saves/&lt;世界&gt;/serverconfig/elden_ring_spells-server.toml}</li>
 * </ul>
 * 进世界时加载并同步到客户端。整合包改平衡优先改这份 toml。
 * <p>
 * 颜色、握点、彗星头缩放、挥砍滚转、粒子密度、出生前移、追踪延迟、索敌锥角不进这里。
 * 冷却 / 最大等级 / 法术开关走铁魔法 JSON。
 */
public final class EldenRingServerConfig {

    public static final ModConfigSpec SPEC;

    public static final HomingValues GLINTSTONE_PEBBLE;
    public static final HomingValues SWIFT_GLINTSTONE_SHARD;
    public static final HomingValues GREAT_GLINTSTONE_SHARD;
    public static final HomingValues GLINTSTONE_COMET;
    public static final HomingValues COMET;
    public static final VolleyValues GLINTSTONE_STARS;
    public static final VolleyValues STAR_SHOWER;
    public static final VolleyValues STARS_OF_RUIN;
    public static final SpiralValues SPIRAL_SHARD;
    public static final FoundingRainValues FOUNDING_RAIN_OF_STARS;
    public static final StarlightValues STARLIGHT;
    public static final TerraMagicaValues TERRA_MAGICA;
    public static final CometAzurValues COMET_AZUR;
    public static final GavelValues GAVEL_OF_HAIMA;
    public static final CannonValues CANNON_OF_HAIMA;
    public static final CarianSlicerValues CARIAN_SLICER;
    public static final CarianSlicerValues CARIAN_GREATSWORD;
    public static final MagicGlintbladeValues MAGIC_GLINTBLADE;
    public static final GlintbladePhalanxValues GLINTBLADE_PHALANX;
    public static final GlintbladePhalanxValues CARIAN_PHALANX;
    public static final GlintbladePhalanxValues GREATBLADE_PHALANX;
    public static final CrystalBarrageValues CRYSTAL_BARRAGE;
    public static final CrystalBurstValues CRYSTAL_BURST;
    public static final GlintstoneArcValues GLINTSTONE_ARC;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(
                "Elden Ring Spells 玩法数值。进世界后由服务端同步。",
                "冷却 / 最大等级 / 启用 / 蓝耗倍率 / 法强倍率请改铁魔法 JSON：",
                "  config/irons_spellbooks_spell_config/elden_ring_spells/<法术id>.json",
                "指令：/ironsSpellbooks generate_file elden_ring_spells:glintstone_pebble full",
                "颜色、粒子密度、彗星头缩放、握点、动画 tick、出生前移、索敌锥角写死在 Java，不进这份文件。"
        );

        GLINTSTONE_PEBBLE = HomingValues.create(builder, "glintstone_pebble", new HomingSeed(
                GlintstonePebbleSpell.SPELL_BASE_MANA_COST,
                GlintstonePebbleSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstonePebbleSpell.SPELL_BASE_SPELL_POWER,
                GlintstonePebbleSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstonePebbleSpell.SPELL_CAST_TIME_TICKS,
                GlintstonePebbleSpell.PROJECTILE_FLIGHT_SPEED,
                GlintstonePebbleSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                GlintstonePebbleSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                GlintstonePebbleSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                null
        ));
        SWIFT_GLINTSTONE_SHARD = HomingValues.create(builder, "swift_glintstone_shard", new HomingSeed(
                SwiftGlintstoneShardSpell.SPELL_BASE_MANA_COST,
                SwiftGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL,
                SwiftGlintstoneShardSpell.SPELL_BASE_SPELL_POWER,
                SwiftGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                SwiftGlintstoneShardSpell.SPELL_CAST_TIME_TICKS,
                SwiftGlintstoneShardSpell.PROJECTILE_FLIGHT_SPEED,
                SwiftGlintstoneShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                SwiftGlintstoneShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                SwiftGlintstoneShardSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                null
        ));
        GREAT_GLINTSTONE_SHARD = HomingValues.create(builder, "great_glintstone_shard", new HomingSeed(
                GreatGlintstoneShardSpell.SPELL_BASE_MANA_COST,
                GreatGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL,
                GreatGlintstoneShardSpell.SPELL_BASE_SPELL_POWER,
                GreatGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GreatGlintstoneShardSpell.SPELL_CAST_TIME_TICKS,
                GreatGlintstoneShardSpell.PROJECTILE_FLIGHT_SPEED,
                GreatGlintstoneShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                GreatGlintstoneShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                GreatGlintstoneShardSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                GreatGlintstoneShardSpell.EXPLOSION_RADIUS_BLOCKS
        ));
        GLINTSTONE_COMET = HomingValues.create(builder, "glintstone_comet", new HomingSeed(
                GlintstoneCometSpell.SPELL_BASE_MANA_COST,
                GlintstoneCometSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstoneCometSpell.SPELL_BASE_SPELL_POWER,
                GlintstoneCometSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstoneCometSpell.SPELL_CAST_TIME_TICKS,
                GlintstoneCometSpell.PROJECTILE_FLIGHT_SPEED,
                GlintstoneCometSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                GlintstoneCometSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                GlintstoneCometSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                GlintstoneCometSpell.EXPLOSION_RADIUS_BLOCKS
        ));
        COMET = HomingValues.create(builder, "comet", new HomingSeed(
                CometSpell.SPELL_BASE_MANA_COST,
                CometSpell.SPELL_MANA_COST_PER_LEVEL,
                CometSpell.SPELL_BASE_SPELL_POWER,
                CometSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CometSpell.SPELL_CAST_TIME_TICKS,
                CometSpell.PROJECTILE_FLIGHT_SPEED,
                CometSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                CometSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                CometSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                CometSpell.EXPLOSION_RADIUS_BLOCKS
        ));

        GLINTSTONE_STARS = VolleyValues.create(builder, "glintstone_stars", new VolleySeed(
                GlintstoneStarsSpell.SPELL_BASE_MANA_COST,
                GlintstoneStarsSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintstoneStarsSpell.SPELL_BASE_SPELL_POWER,
                GlintstoneStarsSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintstoneStarsSpell.SPELL_CAST_TIME_TICKS,
                GlintstoneStarsSpell.PROJECTILE_FLIGHT_SPEED,
                GlintstoneStarsSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                GlintstoneStarsSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                GlintstoneStarsSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                GlintstoneStarsSpell.PROJECTILE_COUNT,
                GlintstoneStarsSpell.PROJECTILE_SPAWN_STAGGER_TICKS
        ));
        STAR_SHOWER = VolleyValues.create(builder, "star_shower", new VolleySeed(
                StarShowerSpell.SPELL_BASE_MANA_COST,
                StarShowerSpell.SPELL_MANA_COST_PER_LEVEL,
                StarShowerSpell.SPELL_BASE_SPELL_POWER,
                StarShowerSpell.SPELL_SPELL_POWER_PER_LEVEL,
                StarShowerSpell.SPELL_CAST_TIME_TICKS,
                StarShowerSpell.PROJECTILE_FLIGHT_SPEED,
                StarShowerSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                StarShowerSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                StarShowerSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                StarShowerSpell.PROJECTILE_COUNT,
                StarShowerSpell.PROJECTILE_SPAWN_STAGGER_TICKS
        ));
        STARS_OF_RUIN = VolleyValues.create(builder, "stars_of_ruin", new VolleySeed(
                StarsOfRuinSpell.SPELL_BASE_MANA_COST,
                StarsOfRuinSpell.SPELL_MANA_COST_PER_LEVEL,
                StarsOfRuinSpell.SPELL_BASE_SPELL_POWER,
                StarsOfRuinSpell.SPELL_SPELL_POWER_PER_LEVEL,
                StarsOfRuinSpell.SPELL_CAST_TIME_TICKS,
                StarsOfRuinSpell.PROJECTILE_FLIGHT_SPEED,
                StarsOfRuinSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                StarsOfRuinSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                StarsOfRuinSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                StarsOfRuinSpell.PROJECTILE_COUNT,
                StarsOfRuinSpell.PROJECTILE_SPAWN_STAGGER_TICKS
        ));

        SPIRAL_SHARD = SpiralValues.create(builder);
        FOUNDING_RAIN_OF_STARS = FoundingRainValues.create(builder);
        STARLIGHT = StarlightValues.create(builder);
        TERRA_MAGICA = TerraMagicaValues.create(builder);
        COMET_AZUR = CometAzurValues.create(builder);
        GAVEL_OF_HAIMA = GavelValues.create(builder);
        CANNON_OF_HAIMA = CannonValues.create(builder);
        CARIAN_SLICER = CarianSlicerValues.create(
                builder,
                "carian_slicer",
                carianSlicerSlashSeed(),
                CarianSlicerValues::applySlicer
        );
        CARIAN_GREATSWORD = CarianSlicerValues.create(
                builder,
                "carian_greatsword",
                carianGreatswordSlashSeed(),
                CarianSlicerValues::applyGreatsword
        );
        MAGIC_GLINTBLADE = MagicGlintbladeValues.create(builder);
        GLINTBLADE_PHALANX = GlintbladePhalanxValues.create(
                builder,
                "glintblade_phalanx",
                "半圆上的辉剑数量。辉剑圆阵默认 5。",
                phalanxSeedFromGlintblade(),
                GlintbladePhalanxValues::applyGlintblade
        );
        CARIAN_PHALANX = GlintbladePhalanxValues.create(
                builder,
                "carian_phalanx",
                "半圆上的辉剑数量。卡利亚圆阵默认 9。",
                phalanxSeedFromCarian(),
                GlintbladePhalanxValues::applyCarian
        );
        GREATBLADE_PHALANX = GlintbladePhalanxValues.create(
                builder,
                "greatblade_phalanx",
                "半圆上的大剑数量。巨剑阵默认 3。",
                phalanxSeedFromGreatblade(),
                GlintbladePhalanxValues::applyGreatblade
        );
        CRYSTAL_BARRAGE = CrystalBarrageValues.create(builder);
        CRYSTAL_BURST = CrystalBurstValues.create(builder);
        GLINTSTONE_ARC = GlintstoneArcValues.create(builder);

        SPEC = builder.build();
    }

    private EldenRingServerConfig() {
    }

    /**
     * 把 toml 写回各 Spell 运行时字段。视觉 / 动画常量保持 Java 默认。
     */
    public static void apply() {
        applyHoming(GLINTSTONE_PEBBLE, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, explosion) -> {
            GlintstonePebbleSpell.SPELL_BASE_MANA_COST = mana;
            GlintstonePebbleSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            GlintstonePebbleSpell.SPELL_BASE_SPELL_POWER = power;
            GlintstonePebbleSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            GlintstonePebbleSpell.SPELL_CAST_TIME_TICKS = castTime;
            GlintstonePebbleSpell.PROJECTILE_FLIGHT_SPEED = speed;
            GlintstonePebbleSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            GlintstonePebbleSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            GlintstonePebbleSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
        });
        applyHoming(SWIFT_GLINTSTONE_SHARD, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, explosion) -> {
            SwiftGlintstoneShardSpell.SPELL_BASE_MANA_COST = mana;
            SwiftGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            SwiftGlintstoneShardSpell.SPELL_BASE_SPELL_POWER = power;
            SwiftGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            SwiftGlintstoneShardSpell.SPELL_CAST_TIME_TICKS = castTime;
            SwiftGlintstoneShardSpell.PROJECTILE_FLIGHT_SPEED = speed;
            SwiftGlintstoneShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            SwiftGlintstoneShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            SwiftGlintstoneShardSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
        });
        applyHoming(GREAT_GLINTSTONE_SHARD, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, explosion) -> {
            GreatGlintstoneShardSpell.SPELL_BASE_MANA_COST = mana;
            GreatGlintstoneShardSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            GreatGlintstoneShardSpell.SPELL_BASE_SPELL_POWER = power;
            GreatGlintstoneShardSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            GreatGlintstoneShardSpell.SPELL_CAST_TIME_TICKS = castTime;
            GreatGlintstoneShardSpell.PROJECTILE_FLIGHT_SPEED = speed;
            GreatGlintstoneShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            GreatGlintstoneShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            GreatGlintstoneShardSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
            if (explosion != null) {
                GreatGlintstoneShardSpell.EXPLOSION_RADIUS_BLOCKS = explosion;
            }
        });
        applyHoming(GLINTSTONE_COMET, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, explosion) -> {
            GlintstoneCometSpell.SPELL_BASE_MANA_COST = mana;
            GlintstoneCometSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            GlintstoneCometSpell.SPELL_BASE_SPELL_POWER = power;
            GlintstoneCometSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            GlintstoneCometSpell.SPELL_CAST_TIME_TICKS = castTime;
            GlintstoneCometSpell.PROJECTILE_FLIGHT_SPEED = speed;
            GlintstoneCometSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            GlintstoneCometSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            GlintstoneCometSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
            if (explosion != null) {
                GlintstoneCometSpell.EXPLOSION_RADIUS_BLOCKS = explosion;
            }
        });
        applyHoming(COMET, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, explosion) -> {
            CometSpell.SPELL_BASE_MANA_COST = mana;
            CometSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            CometSpell.SPELL_BASE_SPELL_POWER = power;
            CometSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            CometSpell.SPELL_CAST_TIME_TICKS = castTime;
            CometSpell.PROJECTILE_FLIGHT_SPEED = speed;
            CometSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            CometSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            CometSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
            if (explosion != null) {
                CometSpell.EXPLOSION_RADIUS_BLOCKS = explosion;
            }
        });

        applyVolley(GLINTSTONE_STARS, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, count, stagger) -> {
            GlintstoneStarsSpell.SPELL_BASE_MANA_COST = mana;
            GlintstoneStarsSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            GlintstoneStarsSpell.SPELL_BASE_SPELL_POWER = power;
            GlintstoneStarsSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            GlintstoneStarsSpell.SPELL_CAST_TIME_TICKS = castTime;
            GlintstoneStarsSpell.PROJECTILE_FLIGHT_SPEED = speed;
            GlintstoneStarsSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            GlintstoneStarsSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            GlintstoneStarsSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
            GlintstoneStarsSpell.PROJECTILE_COUNT = count;
            GlintstoneStarsSpell.PROJECTILE_SPAWN_STAGGER_TICKS = stagger;
        });
        applyVolley(STAR_SHOWER, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, count, stagger) -> {
            StarShowerSpell.SPELL_BASE_MANA_COST = mana;
            StarShowerSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            StarShowerSpell.SPELL_BASE_SPELL_POWER = power;
            StarShowerSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            StarShowerSpell.SPELL_CAST_TIME_TICKS = castTime;
            StarShowerSpell.PROJECTILE_FLIGHT_SPEED = speed;
            StarShowerSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            StarShowerSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            StarShowerSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
            StarShowerSpell.PROJECTILE_COUNT = count;
            StarShowerSpell.PROJECTILE_SPAWN_STAGGER_TICKS = stagger;
        });
        applyVolley(STARS_OF_RUIN, (mana, manaPer, power, powerPer, castTime, speed, range, turn, damage, count, stagger) -> {
            StarsOfRuinSpell.SPELL_BASE_MANA_COST = mana;
            StarsOfRuinSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            StarsOfRuinSpell.SPELL_BASE_SPELL_POWER = power;
            StarsOfRuinSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            StarsOfRuinSpell.SPELL_CAST_TIME_TICKS = castTime;
            StarsOfRuinSpell.PROJECTILE_FLIGHT_SPEED = speed;
            StarsOfRuinSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            StarsOfRuinSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
            StarsOfRuinSpell.SPELL_DAMAGE_PER_SPELL_POWER = damage;
            StarsOfRuinSpell.PROJECTILE_COUNT = count;
            StarsOfRuinSpell.PROJECTILE_SPAWN_STAGGER_TICKS = stagger;
        });

        SPIRAL_SHARD.apply();
        FOUNDING_RAIN_OF_STARS.apply();
        STARLIGHT.apply();
        TERRA_MAGICA.apply();
        COMET_AZUR.apply();
        GAVEL_OF_HAIMA.apply();
        CANNON_OF_HAIMA.apply();
        CARIAN_SLICER.apply();
        CARIAN_GREATSWORD.apply();
        MAGIC_GLINTBLADE.apply();
        GLINTBLADE_PHALANX.apply();
        CARIAN_PHALANX.apply();
        GREATBLADE_PHALANX.apply();
        CRYSTAL_BARRAGE.apply();
        CRYSTAL_BURST.apply();
        GLINTSTONE_ARC.apply();
    }

    private static void applyHoming(HomingValues values, HomingTarget target) {
        target.accept(
                values.baseManaCost.get(),
                values.manaCostPerLevel.get(),
                values.baseSpellPower.get(),
                values.spellPowerPerLevel.get(),
                values.castTimeTicks.get(),
                values.projectileFlightSpeed.get().floatValue(),
                values.projectileTrackingRangeBlocks.get(),
                values.projectileMaxTurnAngleDegreesPerTick.get().floatValue(),
                values.spellDamagePerSpellPower.get().floatValue(),
                values.explosionRadiusBlocks == null ? null : values.explosionRadiusBlocks.get().floatValue()
        );
    }

    private static void applyVolley(VolleyValues values, VolleyTarget target) {
        target.accept(
                values.baseManaCost.get(),
                values.manaCostPerLevel.get(),
                values.baseSpellPower.get(),
                values.spellPowerPerLevel.get(),
                values.castTimeTicks.get(),
                values.projectileFlightSpeed.get().floatValue(),
                values.projectileTrackingRangeBlocks.get(),
                values.projectileMaxTurnAngleDegreesPerTick.get().floatValue(),
                values.spellDamagePerSpellPower.get().floatValue(),
                values.projectileCount.get(),
                values.projectileSpawnStaggerTicks.get()
        );
    }

    private record HomingSeed(
            int baseMana,
            int manaPerLevel,
            int basePower,
            int powerPerLevel,
            int castTime,
            float flightSpeed,
            double trackingRange,
            float turnAngle,
            float damage,
            Float explosionRadius
    ) {
    }

    private record VolleySeed(
            int baseMana,
            int manaPerLevel,
            int basePower,
            int powerPerLevel,
            int castTime,
            float flightSpeed,
            double trackingRange,
            float turnAngle,
            float damage,
            int projectileCount,
            int staggerTicks
    ) {
    }

    @FunctionalInterface
    private interface HomingTarget {
        void accept(
                int mana,
                int manaPer,
                int power,
                int powerPer,
                int castTime,
                float speed,
                double range,
                float turn,
                float damage,
                Float explosion
        );
    }

    @FunctionalInterface
    private interface VolleyTarget {
        void accept(
                int mana,
                int manaPer,
                int power,
                int powerPer,
                int castTime,
                float speed,
                double range,
                float turn,
                float damage,
                int count,
                int stagger
        );
    }

    /**
     * 单发限角追踪弹（魔砾族 / 彗星族）共用键。
     */
    public static final class HomingValues {
        public final ModConfigSpec.IntValue baseManaCost;
        public final ModConfigSpec.IntValue manaCostPerLevel;
        public final ModConfigSpec.IntValue baseSpellPower;
        public final ModConfigSpec.IntValue spellPowerPerLevel;
        public final ModConfigSpec.IntValue castTimeTicks;
        public final ModConfigSpec.DoubleValue projectileFlightSpeed;
        public final ModConfigSpec.DoubleValue projectileTrackingRangeBlocks;
        public final ModConfigSpec.DoubleValue projectileMaxTurnAngleDegreesPerTick;
        public final ModConfigSpec.DoubleValue spellDamagePerSpellPower;
        public final ModConfigSpec.DoubleValue explosionRadiusBlocks;

        private HomingValues(
                SpellBookKeys book,
                HomingFlightKeys flight,
                ModConfigSpec.DoubleValue explosionRadiusBlocks
        ) {
            this.baseManaCost = book.baseManaCost;
            this.manaCostPerLevel = book.manaCostPerLevel;
            this.baseSpellPower = book.baseSpellPower;
            this.spellPowerPerLevel = book.spellPowerPerLevel;
            this.castTimeTicks = book.castTimeTicks;
            this.projectileFlightSpeed = flight.speed;
            this.projectileTrackingRangeBlocks = flight.range;
            this.projectileMaxTurnAngleDegreesPerTick = flight.turn;
            this.spellDamagePerSpellPower = flight.damage;
            this.explosionRadiusBlocks = explosionRadiusBlocks;
        }

        static HomingValues create(ModConfigSpec.Builder builder, String section, HomingSeed seed) {
            builder.push(section);
            SpellBookKeys book = SpellBookKeys.define(
                    builder, seed.baseMana, seed.manaPerLevel, seed.basePower, seed.powerPerLevel, seed.castTime
            );
            HomingFlightKeys flight = HomingFlightKeys.define(builder, seed, true);
            ModConfigSpec.DoubleValue explosion = null;
            if (seed.explosionRadius != null) {
                explosion = ConfigSpecHelper.floating(
                        builder,
                        "explosion_radius_blocks",
                        "命中爆炸半径（方块）。调大 → 清群更强。",
                        seed.explosionRadius,
                        0.0,
                        16.0
                );
            }
            builder.pop();
            return new HomingValues(book, flight, explosion);
        }
    }

    public static final class VolleyValues {
        public final ModConfigSpec.IntValue baseManaCost;
        public final ModConfigSpec.IntValue manaCostPerLevel;
        public final ModConfigSpec.IntValue baseSpellPower;
        public final ModConfigSpec.IntValue spellPowerPerLevel;
        public final ModConfigSpec.IntValue castTimeTicks;
        public final ModConfigSpec.DoubleValue projectileFlightSpeed;
        public final ModConfigSpec.DoubleValue projectileTrackingRangeBlocks;
        public final ModConfigSpec.DoubleValue projectileMaxTurnAngleDegreesPerTick;
        public final ModConfigSpec.DoubleValue spellDamagePerSpellPower;
        public final ModConfigSpec.IntValue projectileCount;
        public final ModConfigSpec.IntValue projectileSpawnStaggerTicks;

        private VolleyValues(
                SpellBookKeys book,
                HomingFlightKeys flight,
                ModConfigSpec.IntValue projectileCount,
                ModConfigSpec.IntValue projectileSpawnStaggerTicks
        ) {
            this.baseManaCost = book.baseManaCost;
            this.manaCostPerLevel = book.manaCostPerLevel;
            this.baseSpellPower = book.baseSpellPower;
            this.spellPowerPerLevel = book.spellPowerPerLevel;
            this.castTimeTicks = book.castTimeTicks;
            this.projectileFlightSpeed = flight.speed;
            this.projectileTrackingRangeBlocks = flight.range;
            this.projectileMaxTurnAngleDegreesPerTick = flight.turn;
            this.spellDamagePerSpellPower = flight.damage;
            this.projectileCount = projectileCount;
            this.projectileSpawnStaggerTicks = projectileSpawnStaggerTicks;
        }

        static VolleyValues create(ModConfigSpec.Builder builder, String section, VolleySeed seed) {
            builder.push(section);
            SpellBookKeys book = SpellBookKeys.define(
                    builder, seed.baseMana, seed.manaPerLevel, seed.basePower, seed.powerPerLevel, seed.castTime
            );
            HomingFlightKeys flight = HomingFlightKeys.define(builder, new HomingSeed(
                    seed.baseMana, seed.manaPerLevel, seed.basePower, seed.powerPerLevel, seed.castTime,
                    seed.flightSpeed, seed.trackingRange, seed.turnAngle, seed.damage, null
            ), true);
            ModConfigSpec.IntValue count = ConfigSpecHelper.integer(
                    builder, "projectile_count", "单次施法弹数。", seed.projectileCount, 1, 32
            );
            ModConfigSpec.IntValue stagger = ConfigSpecHelper.integer(
                    builder, "projectile_spawn_stagger_ticks", "相邻两发间隔（tick）。调大更疏。", seed.staggerTicks, 0, 40
            );
            builder.pop();
            return new VolleyValues(book, flight, count, stagger);
        }
    }

    private record SpellBookKeys(
            ModConfigSpec.IntValue baseManaCost,
            ModConfigSpec.IntValue manaCostPerLevel,
            ModConfigSpec.IntValue baseSpellPower,
            ModConfigSpec.IntValue spellPowerPerLevel,
            ModConfigSpec.IntValue castTimeTicks
    ) {
        static SpellBookKeys define(
                ModConfigSpec.Builder builder,
                int baseMana,
                int manaPer,
                int basePower,
                int powerPer,
                int castTime
        ) {
            return new SpellBookKeys(
                    ConfigSpecHelper.integer(builder, "base_mana_cost", "1 级基础蓝耗。铁魔法还会再乘 JSON 里的 manaMultiplier。", baseMana, 0, 10_000),
                    ConfigSpecHelper.integer(builder, "mana_cost_per_level", "每升 1 级额外蓝耗。", manaPer, 0, 10_000),
                    ConfigSpecHelper.integer(builder, "base_spell_power", "1 级法术强度基数，再乘伤害系数才是伤。", basePower, 0, 10_000),
                    ConfigSpecHelper.integer(builder, "spell_power_per_level", "每级额外法术强度。", powerPer, 0, 10_000),
                    ConfigSpecHelper.integer(builder, "cast_time_ticks", "吟唱时长（tick）。0=瞬时；CONTINUOUS 则是最长按住时间。", castTime, 0, 20_000)
            );
        }
    }

    private record HomingFlightKeys(
            ModConfigSpec.DoubleValue speed,
            ModConfigSpec.DoubleValue range,
            ModConfigSpec.DoubleValue turn,
            ModConfigSpec.DoubleValue damage
    ) {
        static HomingFlightKeys define(ModConfigSpec.Builder builder, HomingSeed seed, boolean includeDamage) {
            return new HomingFlightKeys(
                    ConfigSpecHelper.floating(builder, "projectile_flight_speed", "弹道速度（方块/tick）。越大越难躲。", seed.flightSpeed, 0.05, 8.0),
                    ConfigSpecHelper.floating(builder, "projectile_tracking_range_blocks", "追踪索敌半径（方块）。", seed.trackingRange, 1.0, 128.0),
                    ConfigSpecHelper.floating(builder, "projectile_max_turn_angle_degrees_per_tick", "每 tick 最大转向（度）。越小越像法环轻追踪。", seed.turnAngle, 0.0, 180.0),
                    includeDamage
                            ? ConfigSpecHelper.floating(builder, "spell_damage_per_spell_power", "最终伤害 = 法术强度 × 本系数。", seed.damage, 0.0, 20.0)
                            : null
            );
        }
    }

    public static final class SpiralValues {
        private final SpellBookKeys book;
        private final HomingFlightKeys flight;
        private final ModConfigSpec.IntValue maxEntityHits;

        private SpiralValues(SpellBookKeys book, HomingFlightKeys flight, ModConfigSpec.IntValue maxEntityHits) {
            this.book = book;
            this.flight = flight;
            this.maxEntityHits = maxEntityHits;
        }

        static SpiralValues create(ModConfigSpec.Builder builder) {
            builder.push("spiral_shard");
            SpellBookKeys book = SpellBookKeys.define(
                    builder,
                    SpiralShardSpell.SPELL_BASE_MANA_COST,
                    SpiralShardSpell.SPELL_MANA_COST_PER_LEVEL,
                    SpiralShardSpell.SPELL_BASE_SPELL_POWER,
                    SpiralShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                    SpiralShardSpell.SPELL_CAST_TIME_TICKS
            );
            HomingFlightKeys flight = HomingFlightKeys.define(builder, new HomingSeed(
                    SpiralShardSpell.SPELL_BASE_MANA_COST,
                    SpiralShardSpell.SPELL_MANA_COST_PER_LEVEL,
                    SpiralShardSpell.SPELL_BASE_SPELL_POWER,
                    SpiralShardSpell.SPELL_SPELL_POWER_PER_LEVEL,
                    SpiralShardSpell.SPELL_CAST_TIME_TICKS,
                    SpiralShardSpell.PROJECTILE_FLIGHT_SPEED,
                    SpiralShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                    SpiralShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                    SpiralShardSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                    null
            ), true);
            SpiralValues values = new SpiralValues(
                    book,
                    flight,
                    ConfigSpecHelper.integer(builder, "max_entity_hits", "最多穿透命中次数。", SpiralShardSpell.PROJECTILE_MAX_ENTITY_HITS, 1, 64)
            );
            builder.pop();
            return values;
        }

        void apply() {
            SpiralShardSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            SpiralShardSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            SpiralShardSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            SpiralShardSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            SpiralShardSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            SpiralShardSpell.PROJECTILE_FLIGHT_SPEED = flight.speed.get().floatValue();
            SpiralShardSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = flight.range.get();
            SpiralShardSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = flight.turn.get().floatValue();
            SpiralShardSpell.SPELL_DAMAGE_PER_SPELL_POWER = flight.damage.get().floatValue();
            SpiralShardSpell.PROJECTILE_MAX_ENTITY_HITS = maxEntityHits.get();
        }
    }

    public static final class FoundingRainValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.IntValue rainDropsPerTick;
        private final ModConfigSpec.DoubleValue rainDropFallSpeed;
        private final ModConfigSpec.IntValue rainZoneDamageIntervalTicks;
        private final ModConfigSpec.DoubleValue overheadCloudRadiusBlocks;
        private final ModConfigSpec.IntValue overheadCloudLifetimeTicks;

        private FoundingRainValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.IntValue rainDropsPerTick,
                ModConfigSpec.DoubleValue rainDropFallSpeed,
                ModConfigSpec.IntValue rainZoneDamageIntervalTicks,
                ModConfigSpec.DoubleValue overheadCloudRadiusBlocks,
                ModConfigSpec.IntValue overheadCloudLifetimeTicks
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.rainDropsPerTick = rainDropsPerTick;
            this.rainDropFallSpeed = rainDropFallSpeed;
            this.rainZoneDamageIntervalTicks = rainZoneDamageIntervalTicks;
            this.overheadCloudRadiusBlocks = overheadCloudRadiusBlocks;
            this.overheadCloudLifetimeTicks = overheadCloudLifetimeTicks;
        }

        static FoundingRainValues create(ModConfigSpec.Builder builder) {
            builder.push("founding_rain_of_stars");
            FoundingRainValues values = new FoundingRainValues(
                    SpellBookKeys.define(
                            builder,
                            FoundingRainOfStarsSpell.SPELL_BASE_MANA_COST,
                            FoundingRainOfStarsSpell.SPELL_MANA_COST_PER_LEVEL,
                            FoundingRainOfStarsSpell.SPELL_BASE_SPELL_POWER,
                            FoundingRainOfStarsSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            FoundingRainOfStarsSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(builder, "spell_damage_per_spell_power", "每次雨幕结算伤害 = 法强 × 本系数。", FoundingRainOfStarsSpell.SPELL_DAMAGE_PER_SPELL_POWER, 0.0, 20.0),
                    ConfigSpecHelper.integer(builder, "rain_drops_per_tick", "每 tick 落下的雨针数量。", FoundingRainOfStarsSpell.RAIN_DROPS_PER_TICK, 0, 64),
                    ConfigSpecHelper.floating(builder, "rain_drop_fall_speed_blocks_per_tick", "雨针下落速度（方块/tick）。", FoundingRainOfStarsSpell.RAIN_DROP_FALL_SPEED_BLOCKS_PER_TICK, 0.05, 8.0),
                    ConfigSpecHelper.integer(builder, "rain_zone_damage_interval_ticks", "雨幕伤害结算间隔（tick）。", FoundingRainOfStarsSpell.RAIN_ZONE_DAMAGE_INTERVAL_TICKS, 1, 200),
                    ConfigSpecHelper.floating(builder, "overhead_cloud_radius_blocks", "头顶雨云水平半径（方块）。", FoundingRainOfStarsSpell.OVERHEAD_CLOUD_RADIUS_BLOCKS, 0.5, 32.0),
                    ConfigSpecHelper.integer(builder, "overhead_cloud_lifetime_ticks", "雨云寿命（tick）。", FoundingRainOfStarsSpell.OVERHEAD_CLOUD_LIFETIME_TICKS, 1, 2000)
            );
            builder.pop();
            return values;
        }

        void apply() {
            FoundingRainOfStarsSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            FoundingRainOfStarsSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            FoundingRainOfStarsSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            FoundingRainOfStarsSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            FoundingRainOfStarsSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            FoundingRainOfStarsSpell.SPELL_DAMAGE_PER_SPELL_POWER = damagePerSpellPower.get().floatValue();
            FoundingRainOfStarsSpell.RAIN_DROPS_PER_TICK = rainDropsPerTick.get();
            FoundingRainOfStarsSpell.RAIN_DROP_FALL_SPEED_BLOCKS_PER_TICK = rainDropFallSpeed.get().floatValue();
            FoundingRainOfStarsSpell.RAIN_ZONE_DAMAGE_INTERVAL_TICKS = rainZoneDamageIntervalTicks.get();
            FoundingRainOfStarsSpell.OVERHEAD_CLOUD_RADIUS_BLOCKS = overheadCloudRadiusBlocks.get();
            FoundingRainOfStarsSpell.OVERHEAD_CLOUD_LIFETIME_TICKS = overheadCloudLifetimeTicks.get();
        }
    }

    /**
     * 星光玩法键：蓝耗 / 法强 / 持续 / 光源亮度。
     */
    public static final class StarlightValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.IntValue starDurationTicks;
        private final ModConfigSpec.IntValue lightLevel;

        private StarlightValues(
                SpellBookKeys book,
                ModConfigSpec.IntValue starDurationTicks,
                ModConfigSpec.IntValue lightLevel
        ) {
            this.book = book;
            this.starDurationTicks = starDurationTicks;
            this.lightLevel = lightLevel;
        }

        static StarlightValues create(ModConfigSpec.Builder builder) {
            builder.push("starlight");
            StarlightValues values = new StarlightValues(
                    SpellBookKeys.define(
                            builder,
                            StarlightSpell.SPELL_BASE_MANA_COST,
                            StarlightSpell.SPELL_MANA_COST_PER_LEVEL,
                            StarlightSpell.SPELL_BASE_SPELL_POWER,
                            StarlightSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            StarlightSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "star_duration_ticks",
                            "星星持续（tick）。2400=120 秒。",
                            StarlightSpell.STAR_DURATION_TICKS,
                            20,
                            72_000
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "light_level",
                            "中心光源亮度（0–15）。14=原版火把。",
                            StarlightSpell.LIGHT_LEVEL,
                            0,
                            15
                    )
            );
            builder.pop();
            return values;
        }

        void apply() {
            StarlightSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            StarlightSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            StarlightSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            StarlightSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            StarlightSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            StarlightSpell.STAR_DURATION_TICKS = starDurationTicks.get();
            StarlightSpell.LIGHT_LEVEL = lightLevel.get();
        }
    }

    public static final class TerraMagicaValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue zoneRadiusBlocks;
        private final ModConfigSpec.IntValue zoneBaseDurationTicks;
        private final ModConfigSpec.IntValue zoneDurationTicksPerLevel;
        private final ModConfigSpec.DoubleValue spellPowerBonusMultipliedTotal;

        private TerraMagicaValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue zoneRadiusBlocks,
                ModConfigSpec.IntValue zoneBaseDurationTicks,
                ModConfigSpec.IntValue zoneDurationTicksPerLevel,
                ModConfigSpec.DoubleValue spellPowerBonusMultipliedTotal
        ) {
            this.book = book;
            this.zoneRadiusBlocks = zoneRadiusBlocks;
            this.zoneBaseDurationTicks = zoneBaseDurationTicks;
            this.zoneDurationTicksPerLevel = zoneDurationTicksPerLevel;
            this.spellPowerBonusMultipliedTotal = spellPowerBonusMultipliedTotal;
        }

        static TerraMagicaValues create(ModConfigSpec.Builder builder) {
            builder.push("terra_magica");
            TerraMagicaValues values = new TerraMagicaValues(
                    SpellBookKeys.define(
                            builder,
                            TerraMagicaSpell.SPELL_BASE_MANA_COST,
                            TerraMagicaSpell.SPELL_MANA_COST_PER_LEVEL,
                            TerraMagicaSpell.SPELL_BASE_SPELL_POWER,
                            TerraMagicaSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            TerraMagicaSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(builder, "zone_radius_blocks", "法阵半径（方块）。", TerraMagicaSpell.ZONE_RADIUS_BLOCKS, 0.5, 32.0),
                    ConfigSpecHelper.integer(builder, "zone_base_duration_ticks", "1 级法阵持续（tick）。600=30 秒。", TerraMagicaSpell.ZONE_BASE_DURATION_TICKS, 20, 20_000),
                    ConfigSpecHelper.integer(builder, "zone_duration_ticks_per_level", "每级额外持续（tick）。", TerraMagicaSpell.ZONE_DURATION_TICKS_PER_LEVEL, 0, 2000),
                    ConfigSpecHelper.floating(builder, "spell_power_bonus_multiplied_total", "站内全局法术强度乘数加成（0.30= +30%）。", TerraMagicaSpell.SPELL_POWER_BONUS_MULTIPLIED_TOTAL, 0.0, 5.0)
            );
            builder.pop();
            return values;
        }

        void apply() {
            TerraMagicaSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            TerraMagicaSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            TerraMagicaSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            TerraMagicaSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            TerraMagicaSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            TerraMagicaSpell.ZONE_RADIUS_BLOCKS = zoneRadiusBlocks.get().floatValue();
            TerraMagicaSpell.ZONE_BASE_DURATION_TICKS = zoneBaseDurationTicks.get();
            TerraMagicaSpell.ZONE_DURATION_TICKS_PER_LEVEL = zoneDurationTicksPerLevel.get();
            TerraMagicaSpell.SPELL_POWER_BONUS_MULTIPLIED_TOTAL = spellPowerBonusMultipliedTotal.get();
        }
    }

    public static final class CometAzurValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.IntValue startupDurationTicks;
        private final ModConfigSpec.DoubleValue jetBeamMaxRangeBlocks;
        private final ModConfigSpec.DoubleValue jetBeamDamageRadiusBlocks;
        private final ModConfigSpec.IntValue jetBeamDamageIntervalTicks;
        private final ModConfigSpec.DoubleValue jetBeamDamagePerSpellPower;

        private CometAzurValues(
                SpellBookKeys book,
                ModConfigSpec.IntValue startupDurationTicks,
                ModConfigSpec.DoubleValue jetBeamMaxRangeBlocks,
                ModConfigSpec.DoubleValue jetBeamDamageRadiusBlocks,
                ModConfigSpec.IntValue jetBeamDamageIntervalTicks,
                ModConfigSpec.DoubleValue jetBeamDamagePerSpellPower
        ) {
            this.book = book;
            this.startupDurationTicks = startupDurationTicks;
            this.jetBeamMaxRangeBlocks = jetBeamMaxRangeBlocks;
            this.jetBeamDamageRadiusBlocks = jetBeamDamageRadiusBlocks;
            this.jetBeamDamageIntervalTicks = jetBeamDamageIntervalTicks;
            this.jetBeamDamagePerSpellPower = jetBeamDamagePerSpellPower;
        }

        static CometAzurValues create(ModConfigSpec.Builder builder) {
            builder.push("comet_azur");
            CometAzurValues values = new CometAzurValues(
                    SpellBookKeys.define(
                            builder,
                            CometAzurSpell.SPELL_BASE_MANA_COST,
                            CometAzurSpell.SPELL_MANA_COST_PER_LEVEL,
                            CometAzurSpell.SPELL_BASE_SPELL_POWER,
                            CometAzurSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            CometAzurSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.integer(builder, "startup_duration_ticks", "蓄力漩涡时长（tick）。40=2 秒。", CometAzurSpell.STARTUP_DURATION_TICKS, 0, 200),
                    ConfigSpecHelper.floating(builder, "jet_beam_max_range_blocks", "喷流最大射程（方块）。", CometAzurSpell.JET_BEAM_MAX_RANGE_BLOCKS, 4.0, 128.0),
                    ConfigSpecHelper.floating(builder, "jet_beam_damage_radius_blocks", "喷流伤害圆柱半径（方块）。", CometAzurSpell.JET_BEAM_DAMAGE_RADIUS_BLOCKS, 0.1, 8.0),
                    ConfigSpecHelper.integer(builder, "jet_beam_damage_interval_ticks", "喷流伤害结算间隔（tick）。", CometAzurSpell.JET_BEAM_DAMAGE_INTERVAL_TICKS, 1, 40),
                    ConfigSpecHelper.floating(builder, "jet_beam_damage_per_spell_power", "每次喷流结算伤害 = 法强 × 本系数。", CometAzurSpell.JET_BEAM_DAMAGE_PER_SPELL_POWER, 0.0, 20.0)
            );
            builder.pop();
            return values;
        }

        void apply() {
            CometAzurSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            CometAzurSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            CometAzurSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            CometAzurSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            CometAzurSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            CometAzurSpell.STARTUP_DURATION_TICKS = startupDurationTicks.get();
            CometAzurSpell.JET_BEAM_MAX_RANGE_BLOCKS = jetBeamMaxRangeBlocks.get();
            CometAzurSpell.JET_BEAM_DAMAGE_RADIUS_BLOCKS = jetBeamDamageRadiusBlocks.get().floatValue();
            CometAzurSpell.JET_BEAM_DAMAGE_INTERVAL_TICKS = jetBeamDamageIntervalTicks.get();
            CometAzurSpell.JET_BEAM_DAMAGE_PER_SPELL_POWER = jetBeamDamagePerSpellPower.get().floatValue();
            CometAzurFx.JET_PARTICLE_MAX_ALONG_BLOCKS = (float) CometAzurSpell.JET_BEAM_MAX_RANGE_BLOCKS;
        }
    }

    public static final class GavelValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue directHitDamage;
        private final ModConfigSpec.DoubleValue shockwaveDamage;
        private final ModConfigSpec.DoubleValue directHitRadius;
        private final ModConfigSpec.DoubleValue shockwaveRadius;
        private final ModConfigSpec.DoubleValue directHitKnockback;
        private final ModConfigSpec.DoubleValue shockwaveKnockback;

        private GavelValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue directHitDamage,
                ModConfigSpec.DoubleValue shockwaveDamage,
                ModConfigSpec.DoubleValue directHitRadius,
                ModConfigSpec.DoubleValue shockwaveRadius,
                ModConfigSpec.DoubleValue directHitKnockback,
                ModConfigSpec.DoubleValue shockwaveKnockback
        ) {
            this.book = book;
            this.directHitDamage = directHitDamage;
            this.shockwaveDamage = shockwaveDamage;
            this.directHitRadius = directHitRadius;
            this.shockwaveRadius = shockwaveRadius;
            this.directHitKnockback = directHitKnockback;
            this.shockwaveKnockback = shockwaveKnockback;
        }

        static GavelValues create(ModConfigSpec.Builder builder) {
            builder.push("gavel_of_haima");
            GavelValues values = new GavelValues(
                    SpellBookKeys.define(
                            builder,
                            GavelOfHaimaSpell.SPELL_BASE_MANA_COST,
                            GavelOfHaimaSpell.SPELL_MANA_COST_PER_LEVEL,
                            GavelOfHaimaSpell.SPELL_BASE_SPELL_POWER,
                            GavelOfHaimaSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            GavelOfHaimaSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(builder, "direct_hit_damage_per_spell_power", "锤头直击伤害 = 法强 × 本系数。", GavelOfHaimaSpell.DIRECT_HIT_DAMAGE_PER_SPELL_POWER, 0.0, 20.0),
                    ConfigSpecHelper.floating(builder, "shockwave_damage_per_spell_power", "冲击波伤害 = 法强 × 本系数。", GavelOfHaimaSpell.SHOCKWAVE_DAMAGE_PER_SPELL_POWER, 0.0, 20.0),
                    ConfigSpecHelper.floating(builder, "direct_hit_radius_blocks", "直击判定半径（方块）。", GavelOfHaimaSpell.DIRECT_HIT_RADIUS_BLOCKS, 0.1, 16.0),
                    ConfigSpecHelper.floating(builder, "shockwave_radius_blocks", "冲击波半径（方块）。", GavelOfHaimaSpell.SHOCKWAVE_RADIUS_BLOCKS, 0.1, 32.0),
                    ConfigSpecHelper.floating(builder, "direct_hit_knockback_strength", "直击击退强度。", GavelOfHaimaSpell.DIRECT_HIT_KNOCKBACK_STRENGTH, 0.0, 8.0),
                    ConfigSpecHelper.floating(builder, "shockwave_knockback_strength", "冲击波击退强度。", GavelOfHaimaSpell.SHOCKWAVE_KNOCKBACK_STRENGTH, 0.0, 8.0)
            );
            builder.pop();
            return values;
        }

        void apply() {
            GavelOfHaimaSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            GavelOfHaimaSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            GavelOfHaimaSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            GavelOfHaimaSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            GavelOfHaimaSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            GavelOfHaimaSpell.DIRECT_HIT_DAMAGE_PER_SPELL_POWER = directHitDamage.get().floatValue();
            GavelOfHaimaSpell.SHOCKWAVE_DAMAGE_PER_SPELL_POWER = shockwaveDamage.get().floatValue();
            GavelOfHaimaSpell.DIRECT_HIT_RADIUS_BLOCKS = directHitRadius.get().floatValue();
            GavelOfHaimaSpell.SHOCKWAVE_RADIUS_BLOCKS = shockwaveRadius.get().floatValue();
            GavelOfHaimaSpell.DIRECT_HIT_KNOCKBACK_STRENGTH = directHitKnockback.get();
            GavelOfHaimaSpell.SHOCKWAVE_KNOCKBACK_STRENGTH = shockwaveKnockback.get();
        }
    }

    /**
     * 海摩炮弹玩法键：蓝耗 / 法强 / 蓄力 tick / 弹速 / 爆炸半径 / 伤害系数 / 击退。
     */
    public static final class CannonValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.DoubleValue explosionRadius;
        private final ModConfigSpec.DoubleValue flightSpeed;
        private final ModConfigSpec.DoubleValue knockback;

        private CannonValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.DoubleValue explosionRadius,
                ModConfigSpec.DoubleValue flightSpeed,
                ModConfigSpec.DoubleValue knockback
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.explosionRadius = explosionRadius;
            this.flightSpeed = flightSpeed;
            this.knockback = knockback;
        }

        static CannonValues create(ModConfigSpec.Builder builder) {
            builder.push("cannon_of_haima");
            CannonValues values = new CannonValues(
                    SpellBookKeys.define(
                            builder,
                            CannonOfHaimaSpell.SPELL_BASE_MANA_COST,
                            CannonOfHaimaSpell.SPELL_MANA_COST_PER_LEVEL,
                            CannonOfHaimaSpell.SPELL_BASE_SPELL_POWER,
                            CannonOfHaimaSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            CannonOfHaimaSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(builder, "damage_per_spell_power", "爆炸伤害 = 法强 × 本系数。", CannonOfHaimaSpell.DAMAGE_PER_SPELL_POWER, 0.0, 20.0),
                    ConfigSpecHelper.floating(builder, "explosion_radius_blocks", "爆炸半径（方块）。落地或碰敌立刻结算。", CannonOfHaimaSpell.EXPLOSION_RADIUS_BLOCKS, 0.5, 32.0),
                    ConfigSpecHelper.floating(builder, "projectile_flight_speed", "出手初速（方块/tick）。越大抛物线越平、射得越远。", CannonOfHaimaSpell.PROJECTILE_FLIGHT_SPEED, 0.05, 8.0),
                    ConfigSpecHelper.floating(builder, "explosion_knockback_strength", "爆炸击退强度。", CannonOfHaimaSpell.EXPLOSION_KNOCKBACK_STRENGTH, 0.0, 8.0)
            );
            builder.pop();
            return values;
        }

        void apply() {
            CannonOfHaimaSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            CannonOfHaimaSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            CannonOfHaimaSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            CannonOfHaimaSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            CannonOfHaimaSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            CannonOfHaimaSpell.DAMAGE_PER_SPELL_POWER = damagePerSpellPower.get().floatValue();
            CannonOfHaimaSpell.EXPLOSION_RADIUS_BLOCKS = explosionRadius.get().floatValue();
            CannonOfHaimaSpell.PROJECTILE_FLIGHT_SPEED = flightSpeed.get().floatValue();
            CannonOfHaimaSpell.EXPLOSION_KNOCKBACK_STRENGTH = knockback.get();
        }
    }

    public static final class CarianSlicerValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.DoubleValue slashRadiusBlocks;
        private final ModConfigSpec.DoubleValue slashHalfAngleDegrees;
        private final ModConfigSpec.DoubleValue slashKnockbackStrength;
        private final SlashApplyTarget applyTarget;

        private CarianSlicerValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.DoubleValue slashRadiusBlocks,
                ModConfigSpec.DoubleValue slashHalfAngleDegrees,
                ModConfigSpec.DoubleValue slashKnockbackStrength,
                SlashApplyTarget applyTarget
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.slashRadiusBlocks = slashRadiusBlocks;
            this.slashHalfAngleDegrees = slashHalfAngleDegrees;
            this.slashKnockbackStrength = slashKnockbackStrength;
            this.applyTarget = applyTarget;
        }

        static CarianSlicerValues create(
                ModConfigSpec.Builder builder,
                String section,
                SlashSeed seed,
                SlashApplyTarget applyTarget
        ) {
            builder.push(section);
            CarianSlicerValues values = new CarianSlicerValues(
                    SpellBookKeys.define(
                            builder,
                            seed.baseManaCost,
                            seed.manaCostPerLevel,
                            seed.baseSpellPower,
                            seed.spellPowerPerLevel,
                            seed.castTimeTicks
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "damage_per_spell_power",
                            "每刀伤害 = 法强 × 本系数。连斩频率高，调大要小心。",
                            seed.damagePerSpellPower,
                            0.0,
                            20.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "slash_radius_blocks",
                            "扇形攻击半径（方块）。调大 → 更远也能砍到。",
                            seed.slashRadiusBlocks,
                            0.5,
                            16.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "slash_half_angle_degrees",
                            "扇形半角（度）。相对视线左右各半角；调大 → 侧面更容易命中。",
                            seed.slashHalfAngleDegrees,
                            5.0,
                            180.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "slash_knockback_strength",
                            "命中击退强度。调大 → 被砍的怪往后弹得更开。",
                            seed.slashKnockbackStrength,
                            0.0,
                            8.0
                    ),
                    applyTarget
            );
            builder.pop();
            return values;
        }

        void apply() {
            applyTarget.accept(
                    book.baseManaCost.get(),
                    book.manaCostPerLevel.get(),
                    book.baseSpellPower.get(),
                    book.spellPowerPerLevel.get(),
                    book.castTimeTicks.get(),
                    damagePerSpellPower.get().floatValue(),
                    slashRadiusBlocks.get().floatValue(),
                    slashHalfAngleDegrees.get().floatValue(),
                    slashKnockbackStrength.get()
            );
        }

        static void applySlicer(
                int baseManaCost,
                int manaCostPerLevel,
                int baseSpellPower,
                int spellPowerPerLevel,
                int castTimeTicks,
                float damagePerSpellPower,
                float slashRadiusBlocks,
                float slashHalfAngleDegrees,
                double slashKnockbackStrength
        ) {
            CarianSlicerSpell.SPELL_BASE_MANA_COST = baseManaCost;
            CarianSlicerSpell.SPELL_MANA_COST_PER_LEVEL = manaCostPerLevel;
            CarianSlicerSpell.SPELL_BASE_SPELL_POWER = baseSpellPower;
            CarianSlicerSpell.SPELL_SPELL_POWER_PER_LEVEL = spellPowerPerLevel;
            CarianSlicerSpell.SPELL_CAST_TIME_TICKS = castTimeTicks;
            CarianSlicerSpell.DAMAGE_PER_SPELL_POWER = damagePerSpellPower;
            CarianSlicerSpell.SLASH_RADIUS_BLOCKS = slashRadiusBlocks;
            CarianSlicerSpell.SLASH_HALF_ANGLE_DEGREES = slashHalfAngleDegrees;
            CarianSlicerSpell.SLASH_KNOCKBACK_STRENGTH = slashKnockbackStrength;
        }

        static void applyGreatsword(
                int baseManaCost,
                int manaCostPerLevel,
                int baseSpellPower,
                int spellPowerPerLevel,
                int castTimeTicks,
                float damagePerSpellPower,
                float slashRadiusBlocks,
                float slashHalfAngleDegrees,
                double slashKnockbackStrength
        ) {
            CarianGreatswordSpell.SPELL_BASE_MANA_COST = baseManaCost;
            CarianGreatswordSpell.SPELL_MANA_COST_PER_LEVEL = manaCostPerLevel;
            CarianGreatswordSpell.SPELL_BASE_SPELL_POWER = baseSpellPower;
            CarianGreatswordSpell.SPELL_SPELL_POWER_PER_LEVEL = spellPowerPerLevel;
            CarianGreatswordSpell.SPELL_CAST_TIME_TICKS = castTimeTicks;
            CarianGreatswordSpell.DAMAGE_PER_SPELL_POWER = damagePerSpellPower;
            CarianGreatswordSpell.SLASH_RADIUS_BLOCKS = slashRadiusBlocks;
            CarianGreatswordSpell.SLASH_HALF_ANGLE_DEGREES = slashHalfAngleDegrees;
            CarianGreatswordSpell.SLASH_KNOCKBACK_STRENGTH = slashKnockbackStrength;
        }
    }

    /**
     * 卡利亚斩击咒 toml 默认值。单位与对应 Spell 字段注释一致。
     */
    private record SlashSeed(
            int baseManaCost,
            int manaCostPerLevel,
            int baseSpellPower,
            int spellPowerPerLevel,
            int castTimeTicks,
            float damagePerSpellPower,
            float slashRadiusBlocks,
            float slashHalfAngleDegrees,
            double slashKnockbackStrength
    ) {
    }

    @FunctionalInterface
    private interface SlashApplyTarget {
        void accept(
                int baseManaCost,
                int manaCostPerLevel,
                int baseSpellPower,
                int spellPowerPerLevel,
                int castTimeTicks,
                float damagePerSpellPower,
                float slashRadiusBlocks,
                float slashHalfAngleDegrees,
                double slashKnockbackStrength
        );
    }

    private static SlashSeed carianSlicerSlashSeed() {
        return new SlashSeed(
                CarianSlicerSpell.SPELL_BASE_MANA_COST,
                CarianSlicerSpell.SPELL_MANA_COST_PER_LEVEL,
                CarianSlicerSpell.SPELL_BASE_SPELL_POWER,
                CarianSlicerSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CarianSlicerSpell.SPELL_CAST_TIME_TICKS,
                CarianSlicerSpell.DAMAGE_PER_SPELL_POWER,
                CarianSlicerSpell.SLASH_RADIUS_BLOCKS,
                CarianSlicerSpell.SLASH_HALF_ANGLE_DEGREES,
                CarianSlicerSpell.SLASH_KNOCKBACK_STRENGTH
        );
    }

    private static SlashSeed carianGreatswordSlashSeed() {
        return new SlashSeed(
                CarianGreatswordSpell.SPELL_BASE_MANA_COST,
                CarianGreatswordSpell.SPELL_MANA_COST_PER_LEVEL,
                CarianGreatswordSpell.SPELL_BASE_SPELL_POWER,
                CarianGreatswordSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CarianGreatswordSpell.SPELL_CAST_TIME_TICKS,
                CarianGreatswordSpell.DAMAGE_PER_SPELL_POWER,
                CarianGreatswordSpell.SLASH_RADIUS_BLOCKS,
                CarianGreatswordSpell.SLASH_HALF_ANGLE_DEGREES,
                CarianGreatswordSpell.SLASH_KNOCKBACK_STRENGTH
        );
    }

    public static final class MagicGlintbladeValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.IntValue hoverDurationTicks;
        private final HomingFlightKeys flight;

        private MagicGlintbladeValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.IntValue hoverDurationTicks,
                HomingFlightKeys flight
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.hoverDurationTicks = hoverDurationTicks;
            this.flight = flight;
        }

        static MagicGlintbladeValues create(ModConfigSpec.Builder builder) {
            builder.push("magic_glintblade");
            SpellBookKeys book = SpellBookKeys.define(
                    builder,
                    MagicGlintbladeSpell.SPELL_BASE_MANA_COST,
                    MagicGlintbladeSpell.SPELL_MANA_COST_PER_LEVEL,
                    MagicGlintbladeSpell.SPELL_BASE_SPELL_POWER,
                    MagicGlintbladeSpell.SPELL_SPELL_POWER_PER_LEVEL,
                    MagicGlintbladeSpell.SPELL_CAST_TIME_TICKS
            );
            MagicGlintbladeValues values = new MagicGlintbladeValues(
                    book,
                    ConfigSpecHelper.floating(builder, "damage_per_spell_power", "命中伤害 = 法强 × 本系数。", MagicGlintbladeSpell.DAMAGE_PER_SPELL_POWER, 0.0, 20.0),
                    ConfigSpecHelper.integer(builder, "hover_duration_ticks", "漩涡凝结时长（tick）。到期后发射。", MagicGlintbladeSpell.HOVER_DURATION_TICKS, 0, 200),
                    HomingFlightKeys.define(builder, new HomingSeed(
                            MagicGlintbladeSpell.SPELL_BASE_MANA_COST,
                            MagicGlintbladeSpell.SPELL_MANA_COST_PER_LEVEL,
                            MagicGlintbladeSpell.SPELL_BASE_SPELL_POWER,
                            MagicGlintbladeSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            MagicGlintbladeSpell.SPELL_CAST_TIME_TICKS,
                            MagicGlintbladeSpell.PROJECTILE_FLIGHT_SPEED,
                            MagicGlintbladeSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                            MagicGlintbladeSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK,
                            MagicGlintbladeSpell.DAMAGE_PER_SPELL_POWER,
                            null
                    ), false)
            );
            builder.pop();
            return values;
        }

        void apply() {
            MagicGlintbladeSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            MagicGlintbladeSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            MagicGlintbladeSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            MagicGlintbladeSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            MagicGlintbladeSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            MagicGlintbladeSpell.DAMAGE_PER_SPELL_POWER = damagePerSpellPower.get().floatValue();
            MagicGlintbladeSpell.HOVER_DURATION_TICKS = hoverDurationTicks.get();
            MagicGlintbladeSpell.PROJECTILE_FLIGHT_SPEED = flight.speed.get().floatValue();
            MagicGlintbladeSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = flight.range.get();
            MagicGlintbladeSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = flight.turn.get().floatValue();
        }
    }

    /**
     * 辉剑圆阵 / 卡利亚圆阵 / 巨剑阵共用玩法键：蓝耗 / 单剑伤害 / 剑数 / 触发距离 / 跟手寿命 / 射出后追踪。
     */
    public static final class GlintbladePhalanxValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.IntValue bladeCount;
        private final ModConfigSpec.DoubleValue autoLaunchRangeBlocks;
        private final ModConfigSpec.IntValue hoverLifetimeTicks;
        private final HomingFlightKeys flight;
        private final PhalanxApplyTarget applyTarget;

        private GlintbladePhalanxValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.IntValue bladeCount,
                ModConfigSpec.DoubleValue autoLaunchRangeBlocks,
                ModConfigSpec.IntValue hoverLifetimeTicks,
                HomingFlightKeys flight,
                PhalanxApplyTarget applyTarget
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.bladeCount = bladeCount;
            this.autoLaunchRangeBlocks = autoLaunchRangeBlocks;
            this.hoverLifetimeTicks = hoverLifetimeTicks;
            this.flight = flight;
            this.applyTarget = applyTarget;
        }

        static GlintbladePhalanxValues create(
                ModConfigSpec.Builder builder,
                String section,
                String bladeCountComment,
                PhalanxSeed seed,
                PhalanxApplyTarget applyTarget
        ) {
            builder.push(section);
            SpellBookKeys book = SpellBookKeys.define(
                    builder,
                    seed.baseMana,
                    seed.manaPerLevel,
                    seed.basePower,
                    seed.powerPerLevel,
                    seed.castTime
            );
            GlintbladePhalanxValues values = new GlintbladePhalanxValues(
                    book,
                    ConfigSpecHelper.floating(
                            builder,
                            "damage_per_spell_power",
                            "单剑命中伤害 = 法强 × 本系数。总伤约等于本系数 × 剑数。",
                            seed.damage,
                            0.0,
                            20.0
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "blade_count",
                            bladeCountComment,
                            seed.bladeCount,
                            1,
                            16
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "auto_launch_range_blocks",
                            "玩家周围多少格内有敌人就自动射出（方块）。",
                            seed.autoLaunchRange,
                            1.0,
                            64.0
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "hover_lifetime_ticks",
                            "一直没有敌人时跟手多久后消失（tick）。20 tick = 1 秒。",
                            seed.hoverLifetimeTicks,
                            20,
                            2400
                    ),
                    HomingFlightKeys.define(builder, new HomingSeed(
                            seed.baseMana,
                            seed.manaPerLevel,
                            seed.basePower,
                            seed.powerPerLevel,
                            seed.castTime,
                            seed.flightSpeed,
                            seed.trackingRange,
                            seed.turnAngle,
                            seed.damage,
                            null
                    ), false),
                    applyTarget
            );
            builder.pop();
            return values;
        }

        void apply() {
            applyTarget.accept(
                    book.baseManaCost.get(),
                    book.manaCostPerLevel.get(),
                    book.baseSpellPower.get(),
                    book.spellPowerPerLevel.get(),
                    book.castTimeTicks.get(),
                    damagePerSpellPower.get().floatValue(),
                    bladeCount.get(),
                    autoLaunchRangeBlocks.get(),
                    hoverLifetimeTicks.get(),
                    flight.speed.get().floatValue(),
                    flight.range.get(),
                    flight.turn.get().floatValue()
            );
        }

        private static void applyGlintblade(
                int mana,
                int manaPer,
                int power,
                int powerPer,
                int castTime,
                float damage,
                int blades,
                double autoLaunch,
                int hover,
                float speed,
                double range,
                float turn
        ) {
            GlintbladePhalanxSpell.SPELL_BASE_MANA_COST = mana;
            GlintbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            GlintbladePhalanxSpell.SPELL_BASE_SPELL_POWER = power;
            GlintbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            GlintbladePhalanxSpell.SPELL_CAST_TIME_TICKS = castTime;
            GlintbladePhalanxSpell.DAMAGE_PER_SPELL_POWER = damage;
            GlintbladePhalanxSpell.BLADE_COUNT = blades;
            GlintbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS = autoLaunch;
            GlintbladePhalanxSpell.HOVER_LIFETIME_TICKS = hover;
            GlintbladePhalanxSpell.PROJECTILE_FLIGHT_SPEED = speed;
            GlintbladePhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            GlintbladePhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
        }

        private static void applyCarian(
                int mana,
                int manaPer,
                int power,
                int powerPer,
                int castTime,
                float damage,
                int blades,
                double autoLaunch,
                int hover,
                float speed,
                double range,
                float turn
        ) {
            CarianPhalanxSpell.SPELL_BASE_MANA_COST = mana;
            CarianPhalanxSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            CarianPhalanxSpell.SPELL_BASE_SPELL_POWER = power;
            CarianPhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            CarianPhalanxSpell.SPELL_CAST_TIME_TICKS = castTime;
            CarianPhalanxSpell.DAMAGE_PER_SPELL_POWER = damage;
            CarianPhalanxSpell.BLADE_COUNT = blades;
            CarianPhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS = autoLaunch;
            CarianPhalanxSpell.HOVER_LIFETIME_TICKS = hover;
            CarianPhalanxSpell.PROJECTILE_FLIGHT_SPEED = speed;
            CarianPhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            CarianPhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
        }

        private static void applyGreatblade(
                int mana,
                int manaPer,
                int power,
                int powerPer,
                int castTime,
                float damage,
                int blades,
                double autoLaunch,
                int hover,
                float speed,
                double range,
                float turn
        ) {
            GreatbladePhalanxSpell.SPELL_BASE_MANA_COST = mana;
            GreatbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL = manaPer;
            GreatbladePhalanxSpell.SPELL_BASE_SPELL_POWER = power;
            GreatbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL = powerPer;
            GreatbladePhalanxSpell.SPELL_CAST_TIME_TICKS = castTime;
            GreatbladePhalanxSpell.DAMAGE_PER_SPELL_POWER = damage;
            GreatbladePhalanxSpell.BLADE_COUNT = blades;
            GreatbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS = autoLaunch;
            GreatbladePhalanxSpell.HOVER_LIFETIME_TICKS = hover;
            GreatbladePhalanxSpell.PROJECTILE_FLIGHT_SPEED = speed;
            GreatbladePhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS = range;
            GreatbladePhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = turn;
        }
    }

    private record PhalanxSeed(
            int baseMana,
            int manaPerLevel,
            int basePower,
            int powerPerLevel,
            int castTime,
            float damage,
            int bladeCount,
            double autoLaunchRange,
            int hoverLifetimeTicks,
            float flightSpeed,
            double trackingRange,
            float turnAngle
    ) {
    }

    @FunctionalInterface
    private interface PhalanxApplyTarget {
        void accept(
                int mana,
                int manaPer,
                int power,
                int powerPer,
                int castTime,
                float damage,
                int bladeCount,
                double autoLaunch,
                int hover,
                float speed,
                double range,
                float turn
        );
    }

    private static PhalanxSeed phalanxSeedFromGlintblade() {
        return new PhalanxSeed(
                GlintbladePhalanxSpell.SPELL_BASE_MANA_COST,
                GlintbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL,
                GlintbladePhalanxSpell.SPELL_BASE_SPELL_POWER,
                GlintbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GlintbladePhalanxSpell.SPELL_CAST_TIME_TICKS,
                GlintbladePhalanxSpell.DAMAGE_PER_SPELL_POWER,
                GlintbladePhalanxSpell.BLADE_COUNT,
                GlintbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS,
                GlintbladePhalanxSpell.HOVER_LIFETIME_TICKS,
                GlintbladePhalanxSpell.PROJECTILE_FLIGHT_SPEED,
                GlintbladePhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                GlintbladePhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK
        );
    }

    private static PhalanxSeed phalanxSeedFromCarian() {
        return new PhalanxSeed(
                CarianPhalanxSpell.SPELL_BASE_MANA_COST,
                CarianPhalanxSpell.SPELL_MANA_COST_PER_LEVEL,
                CarianPhalanxSpell.SPELL_BASE_SPELL_POWER,
                CarianPhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL,
                CarianPhalanxSpell.SPELL_CAST_TIME_TICKS,
                CarianPhalanxSpell.DAMAGE_PER_SPELL_POWER,
                CarianPhalanxSpell.BLADE_COUNT,
                CarianPhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS,
                CarianPhalanxSpell.HOVER_LIFETIME_TICKS,
                CarianPhalanxSpell.PROJECTILE_FLIGHT_SPEED,
                CarianPhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                CarianPhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK
        );
    }

    private static PhalanxSeed phalanxSeedFromGreatblade() {
        return new PhalanxSeed(
                GreatbladePhalanxSpell.SPELL_BASE_MANA_COST,
                GreatbladePhalanxSpell.SPELL_MANA_COST_PER_LEVEL,
                GreatbladePhalanxSpell.SPELL_BASE_SPELL_POWER,
                GreatbladePhalanxSpell.SPELL_SPELL_POWER_PER_LEVEL,
                GreatbladePhalanxSpell.SPELL_CAST_TIME_TICKS,
                GreatbladePhalanxSpell.DAMAGE_PER_SPELL_POWER,
                GreatbladePhalanxSpell.BLADE_COUNT,
                GreatbladePhalanxSpell.AUTO_LAUNCH_RANGE_BLOCKS,
                GreatbladePhalanxSpell.HOVER_LIFETIME_TICKS,
                GreatbladePhalanxSpell.PROJECTILE_FLIGHT_SPEED,
                GreatbladePhalanxSpell.PROJECTILE_TRACKING_RANGE_BLOCKS,
                GreatbladePhalanxSpell.PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK
        );
    }

    /**
     * 结晶连弹玩法键：蓝耗 / 法强 / 最长按住 / 弹速 / 射程 / 散射半角 / 连射间隔 / 伤害系数。
     */
    public static final class CrystalBarrageValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.DoubleValue flightSpeed;
        private final ModConfigSpec.DoubleValue maxRangeBlocks;
        private final ModConfigSpec.DoubleValue scatterHalfAngleDegrees;
        private final ModConfigSpec.IntValue shardSpawnIntervalTicks;

        private CrystalBarrageValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.DoubleValue flightSpeed,
                ModConfigSpec.DoubleValue maxRangeBlocks,
                ModConfigSpec.DoubleValue scatterHalfAngleDegrees,
                ModConfigSpec.IntValue shardSpawnIntervalTicks
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.flightSpeed = flightSpeed;
            this.maxRangeBlocks = maxRangeBlocks;
            this.scatterHalfAngleDegrees = scatterHalfAngleDegrees;
            this.shardSpawnIntervalTicks = shardSpawnIntervalTicks;
        }

        static CrystalBarrageValues create(ModConfigSpec.Builder builder) {
            builder.push("crystal_barrage");
            CrystalBarrageValues values = new CrystalBarrageValues(
                    SpellBookKeys.define(
                            builder,
                            CrystalBarrageSpell.SPELL_BASE_MANA_COST,
                            CrystalBarrageSpell.SPELL_MANA_COST_PER_LEVEL,
                            CrystalBarrageSpell.SPELL_BASE_SPELL_POWER,
                            CrystalBarrageSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            CrystalBarrageSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "spell_damage_per_spell_power",
                            "单片伤害 = 法强 × 本系数。连射很密，默认比迅魔砾单发低。",
                            CrystalBarrageSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                            0.0,
                            20.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "projectile_flight_speed",
                            "碎片速度（方块/tick）。越大越难躲。",
                            CrystalBarrageSpell.PROJECTILE_FLIGHT_SPEED,
                            0.05,
                            8.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "projectile_max_range_blocks",
                            "直线最大射程（方块）。超过就碎裂消失。",
                            CrystalBarrageSpell.PROJECTILE_MAX_RANGE_BLOCKS,
                            2.0,
                            64.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "scatter_half_angle_degrees",
                            "散射锥半角（度）。调大更散，不会叠成一条线。",
                            CrystalBarrageSpell.SCATTER_HALF_ANGLE_DEGREES,
                            1.0,
                            45.0
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "shard_spawn_interval_ticks",
                            "相邻两发间隔（tick）。1=每 tick 一发。",
                            CrystalBarrageSpell.SHARD_SPAWN_INTERVAL_TICKS,
                            1,
                            20
                    )
            );
            builder.pop();
            return values;
        }

        void apply() {
            CrystalBarrageSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            CrystalBarrageSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            CrystalBarrageSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            CrystalBarrageSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            CrystalBarrageSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            CrystalBarrageSpell.SPELL_DAMAGE_PER_SPELL_POWER = damagePerSpellPower.get().floatValue();
            CrystalBarrageSpell.PROJECTILE_FLIGHT_SPEED = flightSpeed.get().floatValue();
            CrystalBarrageSpell.PROJECTILE_MAX_RANGE_BLOCKS = maxRangeBlocks.get();
            CrystalBarrageSpell.SCATTER_HALF_ANGLE_DEGREES = scatterHalfAngleDegrees.get().floatValue();
            CrystalBarrageSpell.SHARD_SPAWN_INTERVAL_TICKS = shardSpawnIntervalTicks.get();
        }
    }

    /**
     * 结晶散射玩法键：蓝耗 / 法强 / 弹速 / 射程 / 散射半角 / 齐射片数 / 伤害系数。
     */
    public static final class CrystalBurstValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.DoubleValue flightSpeed;
        private final ModConfigSpec.DoubleValue maxRangeBlocks;
        private final ModConfigSpec.DoubleValue scatterHalfAngleDegrees;
        private final ModConfigSpec.IntValue projectileCount;

        private CrystalBurstValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.DoubleValue flightSpeed,
                ModConfigSpec.DoubleValue maxRangeBlocks,
                ModConfigSpec.DoubleValue scatterHalfAngleDegrees,
                ModConfigSpec.IntValue projectileCount
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.flightSpeed = flightSpeed;
            this.maxRangeBlocks = maxRangeBlocks;
            this.scatterHalfAngleDegrees = scatterHalfAngleDegrees;
            this.projectileCount = projectileCount;
        }

        static CrystalBurstValues create(ModConfigSpec.Builder builder) {
            builder.push("crystal_burst");
            CrystalBurstValues values = new CrystalBurstValues(
                    SpellBookKeys.define(
                            builder,
                            CrystalBurstSpell.SPELL_BASE_MANA_COST,
                            CrystalBurstSpell.SPELL_MANA_COST_PER_LEVEL,
                            CrystalBurstSpell.SPELL_BASE_SPELL_POWER,
                            CrystalBurstSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            CrystalBurstSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "spell_damage_per_spell_power",
                            "单片伤害 = 法强 × 本系数。齐射很密，默认比迅魔砾单发低。",
                            CrystalBurstSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                            0.0,
                            20.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "projectile_flight_speed",
                            "碎片速度（方块/tick）。越大越难躲。",
                            CrystalBurstSpell.PROJECTILE_FLIGHT_SPEED,
                            0.05,
                            8.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "projectile_max_range_blocks",
                            "直线最大射程（方块）。超过就碎裂消失。",
                            CrystalBurstSpell.PROJECTILE_MAX_RANGE_BLOCKS,
                            2.0,
                            64.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "scatter_half_angle_degrees",
                            "散射锥半角（度）。比结晶连弹更开。调大更散。",
                            CrystalBurstSpell.SCATTER_HALF_ANGLE_DEGREES,
                            1.0,
                            60.0
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "projectile_count",
                            "一次齐射的碎片数量。",
                            CrystalBurstSpell.PROJECTILE_COUNT,
                            2,
                            32
                    )
            );
            builder.pop();
            return values;
        }

        void apply() {
            CrystalBurstSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            CrystalBurstSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            CrystalBurstSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            CrystalBurstSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            CrystalBurstSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            CrystalBurstSpell.SPELL_DAMAGE_PER_SPELL_POWER = damagePerSpellPower.get().floatValue();
            CrystalBurstSpell.PROJECTILE_FLIGHT_SPEED = flightSpeed.get().floatValue();
            CrystalBurstSpell.PROJECTILE_MAX_RANGE_BLOCKS = maxRangeBlocks.get();
            CrystalBurstSpell.SCATTER_HALF_ANGLE_DEGREES = scatterHalfAngleDegrees.get().floatValue();
            CrystalBurstSpell.PROJECTILE_COUNT = projectileCount.get();
        }
    }

    /**
     * 辉石弯弧玩法键：蓝耗 / 法强 / 弹速 / 射程 / 起止半宽 / 穿透次数 / 伤害系数。
     */
    public static final class GlintstoneArcValues {
        private final SpellBookKeys book;
        private final ModConfigSpec.DoubleValue damagePerSpellPower;
        private final ModConfigSpec.DoubleValue flightSpeed;
        private final ModConfigSpec.DoubleValue maxRangeBlocks;
        private final ModConfigSpec.DoubleValue startHalfWidthBlocks;
        private final ModConfigSpec.DoubleValue maxHalfWidthBlocks;
        private final ModConfigSpec.IntValue maxEntityHits;

        private GlintstoneArcValues(
                SpellBookKeys book,
                ModConfigSpec.DoubleValue damagePerSpellPower,
                ModConfigSpec.DoubleValue flightSpeed,
                ModConfigSpec.DoubleValue maxRangeBlocks,
                ModConfigSpec.DoubleValue startHalfWidthBlocks,
                ModConfigSpec.DoubleValue maxHalfWidthBlocks,
                ModConfigSpec.IntValue maxEntityHits
        ) {
            this.book = book;
            this.damagePerSpellPower = damagePerSpellPower;
            this.flightSpeed = flightSpeed;
            this.maxRangeBlocks = maxRangeBlocks;
            this.startHalfWidthBlocks = startHalfWidthBlocks;
            this.maxHalfWidthBlocks = maxHalfWidthBlocks;
            this.maxEntityHits = maxEntityHits;
        }

        static GlintstoneArcValues create(ModConfigSpec.Builder builder) {
            builder.push("glintstone_arc");
            GlintstoneArcValues values = new GlintstoneArcValues(
                    SpellBookKeys.define(
                            builder,
                            GlintstoneArcSpell.SPELL_BASE_MANA_COST,
                            GlintstoneArcSpell.SPELL_MANA_COST_PER_LEVEL,
                            GlintstoneArcSpell.SPELL_BASE_SPELL_POWER,
                            GlintstoneArcSpell.SPELL_SPELL_POWER_PER_LEVEL,
                            GlintstoneArcSpell.SPELL_CAST_TIME_TICKS
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "spell_damage_per_spell_power",
                            "单次命中伤害 = 法强 × 本系数。弯弧能穿一排，默认略低于魔砾。",
                            GlintstoneArcSpell.SPELL_DAMAGE_PER_SPELL_POWER,
                            0.0,
                            20.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "projectile_flight_speed",
                            "弯弧速度（方块/tick）。越大越难躲。",
                            GlintstoneArcSpell.PROJECTILE_FLIGHT_SPEED,
                            0.05,
                            8.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "projectile_max_range_blocks",
                            "直线最大射程（方块）。超过就碎裂消失。",
                            GlintstoneArcSpell.PROJECTILE_MAX_RANGE_BLOCKS,
                            2.0,
                            64.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "arc_start_half_width_blocks",
                            "出手时弯弧半宽（方块）。调大 → 出手就已经很宽。",
                            GlintstoneArcSpell.ARC_START_HALF_WIDTH_BLOCKS,
                            0.3,
                            16.0
                    ),
                    ConfigSpecHelper.floating(
                            builder,
                            "arc_max_half_width_blocks",
                            "飞到最大射程时的弯弧半宽（方块）。调大 → 横向扩散更夸张。",
                            GlintstoneArcSpell.ARC_MAX_HALF_WIDTH_BLOCKS,
                            0.5,
                            24.0
                    ),
                    ConfigSpecHelper.integer(
                            builder,
                            "max_entity_hits",
                            "最多穿透命中次数。每个敌人只结算一次。",
                            GlintstoneArcSpell.PROJECTILE_MAX_ENTITY_HITS,
                            1,
                            64
                    )
            );
            builder.pop();
            return values;
        }

        void apply() {
            GlintstoneArcSpell.SPELL_BASE_MANA_COST = book.baseManaCost.get();
            GlintstoneArcSpell.SPELL_MANA_COST_PER_LEVEL = book.manaCostPerLevel.get();
            GlintstoneArcSpell.SPELL_BASE_SPELL_POWER = book.baseSpellPower.get();
            GlintstoneArcSpell.SPELL_SPELL_POWER_PER_LEVEL = book.spellPowerPerLevel.get();
            GlintstoneArcSpell.SPELL_CAST_TIME_TICKS = book.castTimeTicks.get();
            GlintstoneArcSpell.SPELL_DAMAGE_PER_SPELL_POWER = damagePerSpellPower.get().floatValue();
            GlintstoneArcSpell.PROJECTILE_FLIGHT_SPEED = flightSpeed.get().floatValue();
            GlintstoneArcSpell.PROJECTILE_MAX_RANGE_BLOCKS = maxRangeBlocks.get();
            GlintstoneArcSpell.ARC_START_HALF_WIDTH_BLOCKS = startHalfWidthBlocks.get().floatValue();
            GlintstoneArcSpell.ARC_MAX_HALF_WIDTH_BLOCKS = maxHalfWidthBlocks.get().floatValue();
            GlintstoneArcSpell.PROJECTILE_MAX_ENTITY_HITS = maxEntityHits.get();
        }
    }
}
