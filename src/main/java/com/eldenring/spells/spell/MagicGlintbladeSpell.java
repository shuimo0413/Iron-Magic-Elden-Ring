package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.MagicGlintbladeEntity;
import com.eldenring.spells.particle.glintstone.GlintstoneFx;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.sigil.AcademySigilFx;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import com.eldenring.spells.entity.GlintstoneTrailStyle;

/**
 * 魔法辉剑（Magic Glintblade）：瞬时在身前偏右悬停一柄辉剑，短延迟后追踪飞出。
 * <p>
 * 延迟写在实体上，不是 {@link CastType#LONG}。冷却允许同时挂多柄。
 */
public class MagicGlintbladeSpell extends EldenRingAbstractSpell {

    // —— 玩法/视觉数字（toml 只覆盖玩法字段）——
    // —— 法术书 / 蓝耗 / 冷却 ——

        /** 1 级蓝耗。高于迅剑、低于大魔砾。 */
        public static int SPELL_BASE_MANA_COST = 16;

        /** 每升 1 级额外蓝耗。 */
        public static int SPELL_MANA_COST_PER_LEVEL = 3;

        /** 1 级法术强度基数。 */
        public static int SPELL_BASE_SPELL_POWER = 14;

        /** 每级额外法术强度。 */
        public static int SPELL_SPELL_POWER_PER_LEVEL = 2;

        /** 吟唱 tick。0 = 瞬时生成悬停剑。 */
        public static int SPELL_CAST_TIME_TICKS = 0;

        /**
         * 冷却（秒）。可同时挂多柄辉剑，但不要低到无脑铺满。
         */
        public static double SPELL_COOLDOWN_SECONDS = 1.15;

        /** 最大等级。 */
        public static int SPELL_MAX_LEVEL = 1;

        /**
         * 命中伤害 = 法术强度 × 本系数。
         * 调大 → 单剑更痛；辉剑本职是「延迟追踪」，单发应略强于迅剑。
         */
        public static float DAMAGE_PER_SPELL_POWER = 1.08f;

        // —— 悬停生成点（相对眼睛）——

        /**
         * 相对眼睛沿视线前移（方块）。调大 → 剑离脸更远。
         */
        public static double HOVER_FORWARD_OFFSET_BLOCKS = 1.35;

        /**
         * 相对视线平面向右（方块）。法环辉剑略偏右手外侧。
         */
        public static double HOVER_RIGHT_OFFSET_BLOCKS = 0.42;

        /**
         * 相对视线平面向上（方块）。正值 = 略高于准星。
         */
        public static double HOVER_UP_OFFSET_BLOCKS = 0.18;

        /**
         * 悬停阶段时长（tick）。到期后发射。调大 → 更像陷阱；调小 → 更快出手。
         */
        public static int HOVER_DURATION_TICKS = 18;

        /**
         * 渲染用上下浮动振幅（方块）。只影响客户端视觉，不改碰撞。
         */
        public static float HOVER_BOB_AMPLITUDE_BLOCKS = 0.06f;

        /**
         * 渲染浮动角速度（弧度 / tick）。
         */
        public static float HOVER_BOB_RADIANS_PER_TICK = 0.28f;

        // —— 飞行 / 追踪 ——

        /**
         * 发射后飞行速度（方块/tick，传给 {@code AbstractMagicProjectile#getSpeed()}）。
         * 调大 → 更难躲开；调小 → 更有「看剑飞来」的时间。
         */
        public static float PROJECTILE_FLIGHT_SPEED = 0.82f;

        /**
         * 追踪索敌半径（方块）。
         */
        public static double PROJECTILE_TRACKING_RANGE_BLOCKS = 28.0;

        /**
         * 每 tick 允许的最大转向角度（度）。
         * 辉剑应对标「较强追踪」，比魔砾略狠，但仍能侧移甩掉。
         */
        public static float PROJECTILE_MAX_TURN_ANGLE_DEGREES_PER_TICK = 5.5f;

        /**
         * 发射后再直飞的 tick 数；其间不做追踪。
         * 避免刚离手就被身旁杂兵拧歪。
         */
        public static int PROJECTILE_TRACKING_START_DELAY_TICKS = 3;

        /**
         * 索敌锥半角（度）：目标须落在当前飞行方向此锥内。
         */
        public static float PROJECTILE_TRACKING_ACQUIRE_CONE_HALF_ANGLE_DEGREES = 38.0f;

        /** 当前速度过小（近似静止）时跳过本 tick 转向。 */
        public static double PROJECTILE_MINIMUM_SPEED_FOR_HOMING = 1.0e-4;

        /** 朝向夹角极小时直接对齐（弧度）。 */
        public static double PROJECTILE_DIRECTION_ALIGN_EPSILON_RADIANS = 1.0e-5;

        /**
         * 瞄准点相对目标碰撞箱：0=脚底，1=头顶。取偏上避免扎地。
         */
        public static double TRACKING_AIM_HEIGHT_FRACTION = 0.68;

        /**
         * 发射后忽略命中检测的 tick 数，避免刚加速就擦到施法者。
         */
        public static int COLLISION_GRACE_TICKS = 2;

        /**
         * 实体总寿命（含悬停）。到期 discard。
         */
        public static int ENTITY_LIFETIME_TICKS = 90;

        /**
         * 命中判定相对目标箱子的外扩（方块）。
         */
        public static float HIT_DETECTION_INFLATION_BLOCKS = 0.28f;

        // —— 视觉 ——

        /**
         * 模型整体缩放。辉剑比迅剑更细更长。
         */
        public static float SWORD_RENDER_SCALE = 1.05f;

        /** 沿本地 X/Z 再乘一次，做出细长剑身。 */
        public static float SWORD_RADIAL_SCALE = 0.72f;

        /** 剑身自发光（更深的蓝）。 */
        public static int SWORD_BODY_COLOR_ARGB = 0xC01038B0;

        /** 剑刃。 */
        public static int SWORD_BLADE_COLOR_ARGB = 0xD03878F0;

        /** 刃锋。 */
        public static int SWORD_EDGE_COLOR_ARGB = 0xE0B8D8FF;

        /** 光晕。 */
        public static int SWORD_GLOW_COLOR_ARGB = 0x881848D0;

        /** 飞行光轨外辉。 */
        public static int TRAIL_GLOW_COLOR_ARGB = 0xAA2460E8;

        /** 飞行光轨光芯。 */
        public static int TRAIL_CORE_COLOR_ARGB = 0xE0D0ECFF;

        /**
         * 飞行连续光轨：较短较细，强调「剑划过」而不是彗星尾。
         */
        public static com.eldenring.spells.entity.GlintstoneTrailStyle TRAIL_STYLE = new com.eldenring.spells.entity.GlintstoneTrailStyle(
                4.8,
                0.045f,
                0.010f,
                0.22f,
                0.10f,
                20
        );

        /** 飞行点缀粒子强度。 */
        public static float TRAIL_PARTICLE_INTENSITY = 0.7f;

        /** 命中爆裂粒子强度。 */
        public static float IMPACT_PARTICLE_INTENSITY = 1.35f;

    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "magic_glintblade");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(MagicGlintbladeSpell.SPELL_MAX_LEVEL)
            .setCooldownSeconds(MagicGlintbladeSpell.SPELL_COOLDOWN_SECONDS)
            .build();

    public MagicGlintbladeSpell() {
        this.manaCostPerLevel = MagicGlintbladeSpell.SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = MagicGlintbladeSpell.SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = MagicGlintbladeSpell.SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = MagicGlintbladeSpell.SPELL_CAST_TIME_TICKS;
        this.baseManaCost = MagicGlintbladeSpell.SPELL_BASE_MANA_COST;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getDamageAmount(spellLevel, caster), 2)
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

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.AMETHYST_BLOCK_CHIME);
    }

    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.18f, 0.38f, 0.95f);
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
            AcademySigilFx.spawnAboveHead(level, castingEntity);
            MagicGlintbladeEntity glintbladeEntity = new MagicGlintbladeEntity(level, castingEntity);
            Vec3 lookDirection = castingEntity.getLookAngle();
            Vec3 hoverPosition = computeHoverWorld(castingEntity, lookDirection, glintbladeEntity.getBbHeight());
            glintbladeEntity.setPos(hoverPosition);
            glintbladeEntity.setStoredLaunchDirection(lookDirection);
            glintbladeEntity.setDamage(getDamageAmount(spellLevel, castingEntity));

            float yawDegrees = (float) (Mth.atan2(lookDirection.x, lookDirection.z) * Mth.RAD_TO_DEG);
            float pitchDegrees = (float) (Mth.atan2(
                    lookDirection.y,
                    lookDirection.horizontalDistance()
            ) * Mth.RAD_TO_DEG);
            glintbladeEntity.setYRot(yawDegrees);
            glintbladeEntity.setXRot(pitchDegrees);
            glintbladeEntity.yRotO = yawDegrees;
            glintbladeEntity.xRotO = pitchDegrees;

            level.addFreshEntity(glintbladeEntity);
            GlintstoneFx.castBurst(
                    level,
                    hoverPosition.x,
                    hoverPosition.y + 0.35,
                    hoverPosition.z,
                    0.75f
            );
        }
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 眼睛前方偏右偏上：法环辉剑挂在准星外侧，而不是从嘴里喷出来。
     */
    private static Vec3 computeHoverWorld(LivingEntity caster, Vec3 lookDirection, float entityHeight) {
        Vec3 forward = lookDirection.lengthSqr() > 1.0e-8 ? lookDirection.normalize() : new Vec3(0.0, 0.0, 1.0);
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        Vec3 right = forward.cross(worldUp);
        if (right.lengthSqr() < 1.0e-8) {
            right = new Vec3(1.0, 0.0, 0.0);
        } else {
            right = right.normalize();
        }
        Vec3 planeUp = right.cross(forward).normalize();
        return caster.getEyePosition()
                .subtract(0.0, entityHeight * 0.5, 0.0)
                .add(forward.scale(MagicGlintbladeSpell.HOVER_FORWARD_OFFSET_BLOCKS))
                .add(right.scale(MagicGlintbladeSpell.HOVER_RIGHT_OFFSET_BLOCKS))
                .add(planeUp.scale(MagicGlintbladeSpell.HOVER_UP_OFFSET_BLOCKS));
    }

    private float getDamageAmount(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * MagicGlintbladeSpell.DAMAGE_PER_SPELL_POWER;
    }
}
