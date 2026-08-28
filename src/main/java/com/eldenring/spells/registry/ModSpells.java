package com.eldenring.spells.registry;

import com.eldenring.spells.EldenRingSpellsMod;
import com.eldenring.spells.spell.CannonOfHaimaSpell;
import com.eldenring.spells.spell.CarianSlicerSpell;
import com.eldenring.spells.spell.CometAzurSpell;
import com.eldenring.spells.spell.CrystalBarrageSpell;
import com.eldenring.spells.spell.CrystalBurstSpell;
import com.eldenring.spells.spell.CometSpell;
import com.eldenring.spells.spell.FoundingRainOfStarsSpell;
import com.eldenring.spells.spell.GavelOfHaimaSpell;
import com.eldenring.spells.spell.MagicGlintbladeSpell;
import com.eldenring.spells.spell.GlintstoneArcSpell;
import com.eldenring.spells.spell.GlintstoneCometSpell;
import com.eldenring.spells.spell.GlintstonePebbleSpell;
import com.eldenring.spells.spell.GlintstoneStarsSpell;
import com.eldenring.spells.spell.GreatGlintstoneShardSpell;
import com.eldenring.spells.spell.SpiralShardSpell;
import com.eldenring.spells.spell.StarlightSpell;
import com.eldenring.spells.spell.StarShowerSpell;
import com.eldenring.spells.spell.StarsOfRuinSpell;
import com.eldenring.spells.spell.SwiftGlintstoneShardSpell;
import com.eldenring.spells.spell.TerraMagicaSpell;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 本模组全部法术挂到铁魔法 {@link SpellRegistry#SPELL_REGISTRY_KEY} 的入口。
 * <p>
 * 具体施法逻辑在 {@code spell/XxxSpell.java}，这里只负责注册。
 * 注册名取自 {@link AbstractSpell#getSpellName()}（即 ResourceLocation 的 path），
 * 必须与语言键 {@code spell.elden_ring_spells.<path>}、图标 path 一致。
 */
public final class ModSpells {
    public static final DeferredRegister<AbstractSpell> SPELLS =
            DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY, EldenRingSpellsMod.MOD_ID);

    /** 辉石魔砾：基础瞬时单发，限角追踪。 */
    public static final Supplier<AbstractSpell> GLINTSTONE_PEBBLE =
            registerSpell(new GlintstonePebbleSpell());

    /** 辉石迅魔砾：更快更便宜、单发更弱。 */
    public static final Supplier<AbstractSpell> SWIFT_GLINTSTONE_SHARD =
            registerSpell(new SwiftGlintstoneShardSpell());

    /** 辉石弯弧：横向拉开的青色穿透刃，不追踪。 */
    public static final Supplier<AbstractSpell> GLINTSTONE_ARC =
            registerSpell(new GlintstoneArcSpell());

    /** 结晶连弹：按住散射不追踪的迅魔砾碎片，不能边走边放。 */
    public static final Supplier<AbstractSpell> CRYSTAL_BARRAGE =
            registerSpell(new CrystalBarrageSpell());

    /** 结晶散射：瞬时齐射不追踪的迅魔砾碎片，可边走边放，扇面比连弹更开。 */
    public static final Supplier<AbstractSpell> CRYSTAL_BURST =
            registerSpell(new CrystalBurstSpell());

    /** 辉石大魔砾：大体积弹，命中小范围爆炸。 */
    public static final Supplier<AbstractSpell> GREAT_GLINTSTONE_SHARD =
            registerSpell(new GreatGlintstoneShardSpell());

    /** 辉石彗星：介于大魔砾与帚星之间的彗星弹。 */
    public static final Supplier<AbstractSpell> GLINTSTONE_COMET =
            registerSpell(new GlintstoneCometSpell());

    /** 辉石流星：三发错峰强追踪。 */
    public static final Supplier<AbstractSpell> GLINTSTONE_STARS =
            registerSpell(new GlintstoneStarsSpell());

    /** 流星雨：六发错峰强追踪。 */
    public static final Supplier<AbstractSpell> STAR_SHOWER =
            registerSpell(new StarShowerSpell());

    /** 毁灭流星：长吟唱后八发齐射。 */
    public static final Supplier<AbstractSpell> STARS_OF_RUIN =
            registerSpell(new StarsOfRuinSpell());

    /** 创星雨：星云出手后抽光点升空，雨云里落下白紫雨针。 */
    public static final Supplier<AbstractSpell> FOUNDING_RAIN_OF_STARS =
            registerSpell(new FoundingRainOfStarsSpell());

    /** 帚星：巨型彗星，大半径爆炸。 */
    public static final Supplier<AbstractSpell> COMET =
            registerSpell(new CometSpell());

    /** 旋飞魔砾：单实体双螺旋，可穿透。 */
    public static final Supplier<AbstractSpell> SPIRAL_SHARD =
            registerSpell(new SpiralShardSpell());

    /** 星光：头顶跟随小星，火把级照明，持续 120 秒。 */
    public static final Supplier<AbstractSpell> STARLIGHT =
            registerSpell(new StarlightSpell());

    /** 魔法之境：脚下法阵，站内全局法术强度 +30%。 */
    public static final Supplier<AbstractSpell> TERRA_MAGICA =
            registerSpell(new TerraMagicaSpell());

    /** 彗星亚兹勒：2 秒蓄力后按住喷流，不能叠多发。 */
    public static final Supplier<AbstractSpell> COMET_AZUR =
            registerSpell(new CometAzurSpell());

    /** 海摩大槌：身前巨锤砸地，直击 + 冲击波。 */
    public static final Supplier<AbstractSpell> GAVEL_OF_HAIMA =
            registerSpell(new GavelOfHaimaSpell());

    /** 海摩炮弹：1 秒蓄力后抛出不追踪的抛物线炮弹，落地/碰敌爆炸。 */
    public static final Supplier<AbstractSpell> CANNON_OF_HAIMA =
            registerSpell(new CannonOfHaimaSpell());

    /** 卡利亚迅剑：按住正反手连斩。 */
    public static final Supplier<AbstractSpell> CARIAN_SLICER =
            registerSpell(new CarianSlicerSpell());

    /** 魔法辉剑：身前悬停后追踪飞出。 */
    public static final Supplier<AbstractSpell> MAGIC_GLINTBLADE =
            registerSpell(new MagicGlintbladeSpell());

    private ModSpells() {
    }

    /**
     * 用 Spell 自己的 path 当注册名，避免字段名和 ResourceLocation 各写一份对不上。
     */
    private static Supplier<AbstractSpell> registerSpell(AbstractSpell spell) {
        return SPELLS.register(spell.getSpellName(), () -> spell);
    }

    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
    }
}
