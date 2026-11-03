package com.eldenring.spells.entity.academy;

import com.eldenring.spells.registry.ModItems;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * 雷亚卢卡利亚学院学者：主动攻击玩家的辉石法师，四间教室共用这一个实体类。
 * <p>
 * 教室（{@link AcademyClassroom}）在实体类型注册时绑定，决定出生头冠与施法表 JSON。
 * 外观复用铁魔法通用施法者骨骼，出生时穿雷亚卢卡利亚长袍并戴对应辉石头冠。
 * 行为按普通怪物处理：和平难度移除、远离玩家时可自然消失；四种学者互为盟友，魔砾不会互伤。
 */
public class AcademyScholarEntity extends AbstractSpellCastingMob implements Enemy {

    /** 击杀经验值。僵尸为 5、观星者为 25；学者介于两者之间。 */
    private static final int EXPERIENCE_REWARD = 10;

    /** 进入战斗后追击 / 走位的移速倍率（乘在移动速度属性上）。调大 → 拉扯更积极。 */
    private static final double COMBAT_SPEED_MODIFIER = 1.15D;

    /** 无目标时随机闲逛的移速倍率。调小 → 更像在教室里踱步。 */
    private static final double WANDER_SPEED_MODIFIER = 0.8D;

    /** 无目标时注视玩家的距离（方块）。 */
    private static final float LOOK_AT_PLAYER_RANGE_BLOCKS = 8.0F;

    /** 近战攻击力占位：学者以施法为主，此值仅供原版属性需要。 */
    private static final double MELEE_ATTACK_DAMAGE = 3.0D;

    private final AcademyClassroom classroom;

    public AcademyScholarEntity(EntityType<? extends AcademyScholarEntity> entityType, Level level, AcademyClassroom classroom) {
        super(entityType, level);
        this.classroom = classroom;
        this.xpReward = EXPERIENCE_REWARD;
    }

    /** 所属教室：决定头冠与施法表。 */
    public AcademyClassroom classroom() {
        return classroom;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AcademyScholarAttackGoal(
                this,
                COMBAT_SPEED_MODIFIER,
                AcademyScholarBalance.attackIntervalMinTicks(),
                AcademyScholarBalance.attackIntervalMaxTicks()
        ));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WANDER_SPEED_MODIFIER));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, LOOK_AT_PLAYER_RANGE_BLOCKS));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, AcademyScholarEntity.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 属性注册用默认值；实际生成时再按 toml 写入基值（见 {@link #applyConfiguredAttributes()}）。 */
    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, MELEE_ATTACK_DAMAGE)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0D)
                .add(Attributes.MAX_HEALTH, AcademyScholarBalance.DEFAULT_MAX_HEALTH)
                .add(Attributes.FOLLOW_RANGE, AcademyScholarBalance.DEFAULT_FOLLOW_RANGE_BLOCKS)
                .add(Attributes.MOVEMENT_SPEED, AcademyScholarBalance.DEFAULT_MOVEMENT_SPEED);
    }

    /**
     * 1.20.1 的 {@code Mob#finalizeSpawn} 比 1.21 多一个尾部 {@code CompoundTag}，按原样透传。
     */
    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        applyConfiguredAttributes();
        RandomSource random = Utils.random;
        this.populateDefaultEquipmentSlots(random, difficulty);
        return super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
    }

    /** 出生装备：雷亚卢卡利亚长袍 + 所属教室辉石头冠；死亡不掉落。 */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(classroom.crownItem()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.RAYA_LUCARIA_ROBE.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
    }

    /** 首次生成时把 toml 里的生命 / 移速 / 索敌范围写入属性基值，并回满血。 */
    private void applyConfiguredAttributes() {
        setAttributeBaseValue(Attributes.MAX_HEALTH, AcademyScholarBalance.maxHealth());
        setAttributeBaseValue(Attributes.MOVEMENT_SPEED, AcademyScholarBalance.movementSpeed());
        setAttributeBaseValue(Attributes.FOLLOW_RANGE, AcademyScholarBalance.followRangeBlocks());
        this.setHealth(this.getMaxHealth());
    }

    private void setAttributeBaseValue(Attribute attribute, double baseValue) {
        AttributeInstance attributeInstance = this.getAttribute(attribute);
        if (attributeInstance != null) {
            attributeInstance.setBaseValue(baseValue);
        }
    }

    /** 四间教室的学者互为盟友：互不选为目标，铁魔法弹道与法术伤害的友伤判定也会跳过。 */
    @Override
    public boolean isAlliedTo(Entity otherEntity) {
        return super.isAlliedTo(otherEntity) || otherEntity instanceof AcademyScholarEntity;
    }

    /** 与原版怪物一致：和平难度直接移除。 */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    /** 音效走「敌对生物」音量滑块。 */
    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }
}
