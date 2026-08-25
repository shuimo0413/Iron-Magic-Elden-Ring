---
name: 辉石世界线 P2 矿石与辉石矿洞
overview: 三色辉石矿石走原版矿脉；水晶/水晶块用 GlintstoneCaveFeature 挂在现成洞穴上（三色等概率、一洞一色）。无 jigsaw、无箱子、无 NBT。做完即停。
todos:
  - id: ore-json
    content: 三色矿石 configured/placed feature + biome_modifier，对标铁矿；青常见蓝中紫稀
    status: pending
  - id: cave-feature
    content: GlintstoneCaveFeature：噪声整片；三色等概率；一洞一色；表面少量块+水晶，石头占多数
    status: pending
  - id: tuning-register
    content: GlintstoneWorldTuning 写入间距/概率；ModFeatures 注册；EldenRingSpellsMod 挂上
    status: pending
  - id: compile-accept
    content: compileJava 通过；AGENTS.md 勾 P2
    status: pending
isProject: false
---

# P2 — 矿石矿脉 + 辉石矿洞 Feature

**一次只做本文件。依赖 P1 方块已存在。做完验收后停。**

索引：[辉石世界线总计划](辉石世界线总计划_f9cb546f.plan.md)

## 目标

生存里能挖到辉石：普通矿脉出矿石；稀有「辉石矿洞」把现成洞穴表面刷成同色水晶/水晶块。

## 必须遵守

- **不做 jigsaw / structure / NBT / 箱子 / 刷怪覆盖。**
- **不做 geode。**
- 矿洞：**青 / 蓝 / 紫等概率（各 1/3）**；**一座矿洞只出一种颜色**。
- 矿石矿脉仍可青常见、蓝中、紫稀（与矿洞无关）。
- 常量进 `GlintstoneWorldTuning`。

## 矿石

- `minecraft:ore` + stone/deepslate replaceables + triangle 高度带
- `neoforge:add_features`，step `underground_ores`
- 三色分开 placed feature

## 辉石矿洞 Feature

自定义 `GlintstoneCaveFeature`：

- 低频 2D 噪声：chunk 要么全不刷，要么洞穴表面刷（相邻 chunk 连成一片）
- 同一片用同一种子定色，洞内全同色
- 定色均匀 1/3，不要青偏多
- 只处理邻接空气的石头/深板岩：低概率换水晶块，较高概率插随机大小水晶；内部石头不动
- step：`underground_decoration`

## 不要做

- jigsaw、占位 NBT、箱子战利品、建材、星落坑、装备

## 验收

- 新世界地下能挖到三色矿石并熔炼成碎片
- 能找到整片同色的辉石洞穴（石头仍占多数）
- 三种颜色矿洞都能刷到，且单洞不混色
- `.\gradlew.bat compileJava` 通过
- [AGENTS.md](../../AGENTS.md) 勾：辉石矿石矿脉与辉石矿洞 Feature
