package com.eldenring.spells.entity.astrologer;

import com.eldenring.spells.registry.ModItems;
import com.eldenring.spells.registry.ModSpells;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.AbstractSpellCastingMob;
import io.redspace.ironsspellbooks.entity.mobs.abstract_spell_casting_mob.NeutralWizard;
import io.redspace.ironsspellbooks.entity.mobs.goals.FocusOnTradingPlayerGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.PatrolNearLocationGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardAttackGoal;
import io.redspace.ironsspellbooks.entity.mobs.goals.WizardRecoverGoal;
import io.redspace.ironsspellbooks.entity.mobs.wizards.IMerchantWizard;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * 观星者：中立辉石商人法师。
 * <p>
 * 只负责实体状态、AI、装备、交易生命周期与 NBT。报价构造委托 {@link AstrologerTrades}。
 * 外观由星辰护甲装备层覆盖；结构模板只写入实体 ID 与持久化标记。
 */
public class AstrologerEntity extends NeutralWizard implements IMerchantWizard {

    /**
     * 相对出生点的巡逻半径（方块）。调大 → 活动范围更广；调小 → 更贴观星台。
     */
    private static final float PATROL_RADIUS_BLOCKS = 16.0f;

    /**
     * 巡逻移速倍率。调大 → 走动更勤快；调小 → 更常站定。
     */
    private static final double PATROL_SPEED_MODIFIER = 0.75d;

    @Nullable
    private Player tradingPlayer;

    @Nullable
    private MerchantOffers offers;

    private long lastRestockGameTime;
    private int numberOfRestocksToday;
    private long lastRestockCheckDayTime;

    public AstrologerEntity(EntityType<? extends AbstractSpellCastingMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 25;
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FocusOnTradingPlayerGoal<>(this));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new WizardAttackGoal(this, 1.15f, 28, 55)
                .setSpells(
                        List.of(
                                ModSpells.GLINTSTONE_PEBBLE.get(),
                                ModSpells.GLINTSTONE_PEBBLE.get(),
                                ModSpells.SWIFT_GLINTSTONE_SHARD.get(),
                                ModSpells.MAGIC_GLINTBLADE.get(),
                                ModSpells.GLINTSTONE_STARS.get()
                        ),
                        List.of(),
                        List.of(),
                        List.of()
                )
                .setSpellQuality(0.35f, 0.70f)
        );
        this.goalSelector.addGoal(3, new PatrolNearLocationGoal(this, PATROL_RADIUS_BLOCKS, PATROL_SPEED_MODIFIER));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new WizardRecoverGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                this::isHostileTowards
        ));
        this.targetSelector.addGoal(5, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable SpawnGroupData spawnData
    ) {
        RandomSource random = Utils.random;
        this.populateDefaultEquipmentSlots(random, difficulty);
        return super.finalizeSpawn(level, difficulty, reason, spawnData);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.CELESTIAL_MAGE_HAT.get()));
        this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.CELESTIAL_MAGE_ROBE.get()));
        this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.CELESTIAL_MAGE_LEGGINGS.get()));
        this.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.CELESTIAL_MAGE_BOOTS.get()));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.LEGS, 0.0F);
        this.setDropChance(EquipmentSlot.FEET, 0.0F);
    }


    public static AttributeSupplier.Builder prepareAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ATTACK_KNOCKBACK, 0.0)
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Override
    public Optional<SoundEvent> getAngerSound() {
        return Optional.of(SoundRegistry.TRADER_NO.get());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean preventTrade = isAggressive() || (!this.level().isClientSide && this.getOffers().isEmpty());
        if (!preventTrade) {
            if (!this.level().isClientSide && !this.getOffers().isEmpty()) {
                if (shouldRestock()) {
                    restock();
                }
                this.startTrading(player);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void startTrading(Player player) {
        this.setTradingPlayer(player);
        this.openTradingScreen(player, this.getDisplayName(), 0);
    }

    @Override
    public int getRestocksToday() {
        return this.numberOfRestocksToday;
    }

    @Override
    public void setRestocksToday(int restocks) {
        this.numberOfRestocksToday = restocks;
    }

    @Override
    public long getLastRestockGameTime() {
        return this.lastRestockGameTime;
    }

    @Override
    public void setLastRestockGameTime(long time) {
        this.lastRestockGameTime = time;
    }

    @Override
    public long getLastRestockCheckDayTime() {
        return this.lastRestockCheckDayTime;
    }

    @Override
    public void setLastRestockCheckDayTime(long time) {
        this.lastRestockCheckDayTime = time;
    }

    @Override
    public Level level() {
        return super.level();
    }

    @Override
    public void setTradingPlayer(@Nullable Player tradingPlayer) {
        this.tradingPlayer = tradingPlayer;
    }

    @Override
    @Nullable
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = AstrologerTrades.createOffers(this.random);
            this.setLastRestockGameTime(this.level().getGameTime());
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.ambientSoundTime = -this.getAmbientSoundInterval();
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(this.getTradeUpdatedSound(!stack.isEmpty()), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    protected SoundEvent getTradeUpdatedSound(boolean isYesSound) {
        return isYesSound ? SoundRegistry.TRADER_YES.get() : SoundRegistry.TRADER_NO.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundRegistry.TRADER_YES.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        serializeMerchant(tag, this.offers, this.lastRestockGameTime, this.numberOfRestocksToday);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        deserializeMerchant(tag, loadedOffers -> this.offers = loadedOffers);
    }
}
