/**
 * 本模组全部「法术定义」都在这个包里。
 * <p>
 * 这里<strong>不是</strong>独立魔法系统：每个类都继承铁魔法
 * {@link io.redspace.ironsspellbooks.api.spells.AbstractSpell}，
 * 由铁魔法负责选法、扣蓝、冷却、施法动画与按键。本包只填「这道咒做什么」。
 *
 * <h2>读代码时先记住的生命周期</h2>
 * <ol>
 *   <li>玩家按下施法键 → 铁魔法查 {@link io.redspace.ironsspellbooks.api.spells.CastType}。
 *       {@code INSTANT} 几乎立刻进入 {@code onCast}；{@code LONG} 要先蓄力
 *       {@code castTime} 个 tick，期间可走 {@code onServerCastTick}。</li>
 *   <li>{@code onCast} 在<strong>服务端和客户端都会进</strong>。真正生成弹道 / 法阵必须包在
 *       {@code if (!level.isClientSide)} 里，否则会双端各刷一次。</li>
 *   <li>{@code onCast} <strong>末尾必须</strong> {@code super.onCast(...)}，
 *       否则铁魔法收尾（音效、统计、部分状态）不会跑。</li>
 * </ol>
 *
 * <h2>一个法术通常拆成几处</h2>
 * <ul>
 *   <li>本包 {@code XxxSpell.java}：注册 ID、稀有度、蓝耗、冷却、施法瞬间做什么。</li>
 *   <li>{@code tuning/XxxTuning.java}：速度、伤害系数、半径等<strong>可调数字</strong>。改手感只改 Tuning。</li>
 *   <li>{@code entity/}：弹道或法阵实体（飞行、追踪、爆炸）。Spell 只负责「生成」它们。</li>
 *   <li>{@code registry/ModSpells.java}：挂到铁魔法法术注册表。</li>
 *   <li>语言键 {@code spell.elden_ring_spells.<path>} + 图标 {@code textures/gui/spell_icons/<path>.png}。</li>
 * </ul>
 *
 * <h2>现有法术怎么分工</h2>
 * <ul>
 *   <li>单发弹道：魔砾 / 迅魔砾 / 大魔砾 / 辉石彗星 / 帚星 —— 都走
 *       {@link com.eldenring.spells.spell.GlintstoneCastHelper#spawnAlongLook}。</li>
 *   <li>连发流星：辉石流星 / 流星雨 / 毁灭流星 —— Spell 只生成
 *       {@code GlintstoneStarVolleyEntity}，由它按 tick 错峰出弹。</li>
 *   <li>旋飞魔砾：一发实体内部画双螺旋。</li>
 *   <li>魔法之境：脚下铺法阵，给站在里面的人加全局法术强度。</li>
 *   <li>创星雨：时序实体抽光点升空，消失时钉一朵头顶雨云（落星雨尚未实现）。</li>
 * </ul>
 *
 * 入门请先看 {@link com.eldenring.spells.spell.GlintstonePebbleSpell}（最简单的瞬时单发）
 * 和 {@link com.eldenring.spells.spell.GlintstoneCastHelper}（所有辉石弹怎么出生）。
 */
package com.eldenring.spells.spell;
