package com.eldenring.spells.spell;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.entity.CarianSlicerEntity;
import com.eldenring.spells.registry.ModSchools;
import com.eldenring.spells.spell.combat.CarianSlicerCombat;
import com.eldenring.spells.spell.data.CarianSlicerCastData;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * 卡利亚迅剑（Carian Slicer）：右手召唤深蓝辉剑，按住则连斩身前扇形。
 * <p>
 * {@link CastType#CONTINUOUS}：按住施法键维持连斩；松开后当前这一刀收完再淡出。
 * 铁魔法 CONTINUOUS 默认会持续到时间/蓝耗尽，客户端另发 CancelCast 才能松手即停。
 * <p>
 * 本类只对接铁魔法施法管线（起手刷剑、每 tick 续命、结束请求收刀）。
 * 挥砍节奏在 {@link CarianSlicerEntity}，扇形命中在 {@link CarianSlicerCombat}，
 * 本次吟唱绑哪把剑在 {@link CarianSlicerCastData}。剑在身前左右挥砍，不做弹道。
 */
public class CarianSlicerSpell extends EldenRingAbstractSpell {

    // -------------------------------------------------------------------------
    // 法术档（冷却 / 等级走铁魔法 JSON；蓝耗 / 强度 / 时长可被 server.toml 覆盖）
    // -------------------------------------------------------------------------

    /**
     * 最大等级种子。运行时以 {@code irons_spellbooks_spell_config} 里本咒 JSON 为准。
     */
    public static final int SPELL_MAX_LEVEL = 1;

    /**
     * 冷却（秒）。迅剑要能连点/短按衔接，所以明显短于海摩大槌那种重击。
     * 调大 → 松手后再按空窗更长；调小 → 更接近原作「几乎无 CD」。
     */
    public static final double SPELL_COOLDOWN_SECONDS = 0.35;

    /**
     * 1 级基础法力消耗。CONTINUOUS 会按铁魔法节奏反复扣（约每 10 tick 进 {@link #onCast}）。
     * 调大 → 按住连斩更吃蓝。
     */
    public static int SPELL_BASE_MANA_COST = 8;

    /** 每升一级额外法力消耗。当前定死 1 级，字段仍留给铁魔法等级曲线。 */
    public static int SPELL_MANA_COST_PER_LEVEL = 2;

    /**
     * 1 级基础法术强度。挥砍伤害 = {@link #getSpellPower} × {@link #SLASH_DAMAGE_PER_SPELL_POWER}。
     */
    public static int SPELL_BASE_SPELL_POWER = 12;

    /** 每升一级额外法术强度。当前定死 1 级。 */
    public static int SPELL_SPELL_POWER_PER_LEVEL = 2;

    /**
     * 按住最长持续时间（tick）。20 tick = 1 秒；160 ≈ 8 秒。
     * CONTINUOUS 用它当「一口气能砍多久」的上限，不是起手蓄力。
     * 调大 → 蓝够的话能砍更久；调小 → 更早被时间掐断。
     */
    public static int SPELL_CAST_TIME_TICKS = 160;

    // -------------------------------------------------------------------------
    // 扇形挥砍（Combat / Entity 读这里的运行时字段）
    // -------------------------------------------------------------------------

    /**
     * 挥砍伤害系数：最终伤害 = 法术强度 × 本值。
     * 调大 → 单刀更疼，连斩 DPS 一起涨。
     */
    public static float SLASH_DAMAGE_PER_SPELL_POWER = 0.92f;

    /**
     * 扇形半径（方块），从眼睛前方原点量。
     * 调大 → 更远也能刮到；调小 → 必须贴身。
     */
    public static float SLASH_RANGE_BLOCKS = 2.75f;

    /**
     * 扇形半角（度）。左右合计约 116° 的圆锥。
     * 调大 → 余光里的怪也吃刀；调小 → 必须对准。
     */
    public static float SLASH_HALF_ANGLE_DEGREES = 58.0f;

    /**
     * 击退强度（原版 knockback 标量）。迅剑刻意偏低，连斩时别把怪弹出下一刀扇形。
     * 调大 → 更像重击；调小 → 怪更粘在身前。
     */
    public static double SLASH_KNOCKBACK_STRENGTH = 0.18;

    /**
     * 单刀周期（tick）。实体按这个间隔正反手交替。
     * 调大 → 连斩更疏、更好读招；调小 → 更快但更吃无敌帧处理。
     */
    public static int SLASH_CYCLE_TICKS = 10;

    /** 注册 ID：{@code elden_ring_spells:carian_slicer}。 */
    private final ResourceLocation spellResourceLocation =
            ResourceLocation.fromNamespaceAndPath(EldenRingSpellsMod.MOD_ID, "carian_slicer");

    /**
     * 铁魔法默认档：普通稀有度、辉石学派、冷却与最大等级种子。
     * 冷却 / 最大等级运行时仍可被铁魔法 JSON 覆盖。
     */
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(ModSchools.GLINTSTONE_RESOURCE)
            .setMaxLevel(SPELL_MAX_LEVEL)
            .setCooldownSeconds(SPELL_COOLDOWN_SECONDS)
            .build();

    /**
     * 把蓝耗 / 强度 / 吟唱时长写进铁魔法基类字段。
     * toml 加载后会改上面的 static，再由 config apply 同步到已注册的 Spell 实例。
     */
    public CarianSlicerSpell() {
        this.manaCostPerLevel = SPELL_MANA_COST_PER_LEVEL;
        this.baseSpellPower = SPELL_BASE_SPELL_POWER;
        this.spellPowerPerLevel = SPELL_SPELL_POWER_PER_LEVEL;
        this.castTime = SPELL_CAST_TIME_TICKS;
        this.baseManaCost = SPELL_BASE_MANA_COST;
    }

    /**
     * 伤害源把原版受伤无敌帧打成 0 tick。
     * 不打的话，第二刀会落在第一刀的 i-frame 里，连斩实际伤害为 0。
     */
    @Override
    public SpellDamageSource getDamageSource(Entity projectile, Entity attacker) {
        return super.getDamageSource(projectile, attacker).setIFrames(0);
    }

    /**
     * 法术书三行：单刀伤害、扇形半径（方块）、「按住连斩」提示。
     */
    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable(
                        "ui.irons_spellbooks.damage",
                        Utils.stringTruncation(getSlashDamage(spellLevel, caster), 2)
                ),
                Component.translatable(
                        "ui.irons_spellbooks.radius",
                        Utils.stringTruncation(SLASH_RANGE_BLOCKS, 1)
                ),
                Component.translatable("ui.elden_ring_spells.hold_to_combo")
        );
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    /**
     * 持续吟唱：按住才维持连斩。松手走 {@link #onServerCastComplete}，不是 INSTANT 那种「放完就没」。
     */
    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return spellResourceLocation;
    }

    /**
     * 起手不走铁魔法默认音；挥砍声由实体 / 客户端按每一刀播。
     */
    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.empty();
    }

    /**
     * 结束同样不播铁魔法收招音，避免和辉剑淡出叠在一起。
     */
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /**
     * 起手不播铁魔法那一次定格动作。左右挥砍由客户端按每一刀重播，
     * 否则 CONTINUOUS 整段按住只会抡一下。
     */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return AnimationHolder.none();
    }

    /**
     * 准星 / 指向提示色（RGB 0–1）：深蓝辉石。
     */
    @Override
    public Vector3f getTargetingColor() {
        return new Vector3f(0.22f, 0.42f, 1.0f);
    }

    /**
     * 刚按下：立刻刷出一把跟手辉剑，把引用塞进本次吟唱的 {@link CarianSlicerCastData}。
     * 之后的刀由实体按 {@link #SLASH_CYCLE_TICKS} 自己抡，不要在 {@link #onCast} 里再叠实体。
     */
    @Override
    public void onServerPreCast(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (!level.isClientSide && playerMagicData != null) {
            // 上次没淡完又起手时，先丢掉身边残留的迅剑，避免双手两把。
            CarianSlicerCombat.discardOwnedSlicers(level, entity);
            CarianSlicerCastData castData = new CarianSlicerCastData();
            CarianSlicerEntity slicerEntity = new CarianSlicerEntity(
                    level,
                    entity,
                    getSlashDamage(spellLevel, entity)
            );
            level.addFreshEntity(slicerEntity);
            slicerEntity.refreshWhileCasting();
            castData.bindSlicerEntity(slicerEntity);
            playerMagicData.setAdditionalCastData(castData);
        }
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
    }

    /**
     * 每个吟唱 tick 告诉剑「还按着」；实体自己决定何时接下刀。
     * 剑丢了（区块卸载、被清实体等）就补一把再绑回 CastData，避免按住却突然没刀。
     */
    @Override
    public void onServerCastTick(
            Level level,
            int spellLevel,
            LivingEntity entity,
            @Nullable MagicData playerMagicData
    ) {
        if (level.isClientSide || playerMagicData == null) {
            return;
        }
        if (!(playerMagicData.getAdditionalCastData() instanceof CarianSlicerCastData castData)) {
            return;
        }
        CarianSlicerEntity slicerEntity = castData.slicerEntity();
        if (slicerEntity == null || slicerEntity.isRemoved()) {
            CarianSlicerEntity replacement = new CarianSlicerEntity(
                    level,
                    entity,
                    getSlashDamage(spellLevel, entity)
            );
            level.addFreshEntity(replacement);
            castData.bindSlicerEntity(replacement);
            slicerEntity = replacement;
        }
        slicerEntity.refreshWhileCasting();
        slicerEntity.setSlashDamage(getSlashDamage(spellLevel, entity));
    }

    /**
     * CONTINUOUS 约每 10 tick 进这里，铁魔法在 {@code super.onCast} 里扣蓝。
     * 刀已经由实体在挥，这里不要再 {@code addFreshEntity} 一把剑。
     */
    @Override
    public void onCast(
            Level level,
            int spellLevel,
            LivingEntity castingEntity,
            CastSource castSource,
            MagicData playerMagicData
    ) {
        super.onCast(level, spellLevel, castingEntity, castSource, playerMagicData);
    }

    /**
     * 松手 / 没蓝 / 时间到：{@link CarianSlicerEntity#requestStop()} 让当前这一刀收完再淡出，
     * 不要 {@code discard()} 立刻删实体。解绑 CastData，避免 complete 之后还握着过期引用。
     */
    @Override
    public void onServerCastComplete(
            Level level,
            int spellLevel,
            LivingEntity entity,
            MagicData playerMagicData,
            boolean cancelled
    ) {
        if (playerMagicData.getAdditionalCastData() instanceof CarianSlicerCastData castData) {
            CarianSlicerEntity slicerEntity = castData.slicerEntity();
            if (slicerEntity != null && !slicerEntity.isRemoved()) {
                slicerEntity.requestStop();
            }
            castData.bindSlicerEntity(null);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    /**
     * 当前等级下的单刀伤害。Combat 结算时读实体上缓存的这个值。
     */
    private float getSlashDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster) * SLASH_DAMAGE_PER_SPELL_POWER;
    }
}
