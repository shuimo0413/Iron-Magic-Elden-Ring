# Elden Ring Spells — AI / 开发说明

面向后续 AI 与人类协作者的项目说明书。改代码前先读本文件与 `.cursor/rules/`。

**解耦 / 数值做减法的构建说明书：** 根目录 [`法术解耦架构.md`](法术解耦架构.md)。下个话题按那份落地（删 Tuning、瘦 toml、Spell/Curve/Combat/Fx），不要再开一轮架构讨论。

**卡利亚迅剑（未完成，上一轮修改作废）：** 根目录 [`卡利亚迅剑话题交接.md`](卡利亚迅剑话题交接.md)。接着做迅剑抬臂 / 动作组之前必须先读；禁止沿用那一轮的对调、手改欧拉、Z=±90。

## 项目是什么

- **名称**：Elden Ring Spells（法环主题铁魔法扩展）
- **Mod ID**：`elden_ring_spells`
- **包名**：`com.eldenring.spells`
- **目标**：在 [Iron's Spells 'n Spellbooks](https://iron.wiki/developers/) 之上新增艾尔登法环风格法术 / 学派等内容
- **不是**：独立魔法系统；不要绕开铁魔法去自研一套施法管线

## 技术栈（固定版本，勿擅自大升级）

| 组件 | 版本 / 说明 |
|------|-------------|
| Minecraft | `1.21.1` |
| 加载器 | NeoForge `21.1.244`（见 `gradle.properties` 的 `neo_version`） |
| JDK | **必须 21**（`org.gradle.java.home` 已指向本机 Microsoft JDK 21） |
| 构建 | Gradle Wrapper + ModDevGradle（`net.neoforged.moddev`） |
| 铁魔法 | `irons_spellbooks` `1.21.1-3.16.2`（compileOnly `:api`，runClient 用 full jar） |
| Iron's Lib | `1.21.1-2.1.0` |
| 运行时连带 | Curios、GeckoLib、PlayerAnimator（版本写在 `gradle.properties`） |

版本号只改 `gradle.properties`，不要在多处硬编码散落副本（`neoforge.mods.toml` 里依赖范围若写死版本，升级时一并改）。

## 目录地图

```
src/main/java/com/eldenring/spells/
  EldenRingSpellsMod.java      # @Mod 入口，注册 DeferredRegister
  EldenRingSpellsClient.java   # 客户端（粒子 Provider / 实体渲染）
  registry/ModItems.java       # 物品 DeferredRegister
  registry/ModBlocks.java      # 辉石水晶簇 / 水晶块
  registry/ModFeatures.java    # 辉石矿洞 Feature 类型
  registry/ModParticles.java   # 粒子 DeferredRegister
  registry/ModEntities.java    # 弹道等实体 DeferredRegister
  registry/ModSpells.java      # 法术 DeferredRegister（挂 SpellRegistry.SPELL_REGISTRY_KEY）
  registry/ModSounds.java      # 施法音（SPELL_CAST 飞弹射出，SPELL_CAST_START 蓄力起手）
  registry/ModCreativeTabs.java
  spell/*.java                 # 法术本体（XxxSpell）；helper/curve/combat/fx/data 是被调用的函数
  entity/*.java                # 法术弹道等实体（可继承 AbstractMagicProjectile，非稳定 API）
  particle/glintstone/         # 辉石系通用粒子库（GlintstoneFx + Spark/Glow）
  client/render/               # 弹道朝向等通用渲染工具
  client/render/glintstone/    # 辉石彗星头模型 / Drawer / 具体弹道 Renderer
  world/GlintstoneColor.java   # 三色枚举
  worldgen/GlintstoneCaveFeature.java  # 现成洞穴整片刷同色水晶
  config/EldenRingConfigs.java         # 注册 toml；加载后 apply + SpellBookStatReloader
  config/EldenRingServerConfig.java    # 玩法数字 → config/elden_ring_spells-server.toml
  config/EldenRingCommonConfig.java    # 矿洞密度 → config/elden_ring_spells-common.toml
  client/ClientParticleProviders.java  # 粒子 Provider
  client/ClientEntityRenderers.java    # 实体 Renderer / 模型层
  client/ClientItemModels.java         # 卷轴 standalone 模型

工具链/                              # 所有离线工具脚本统一放这里（勿再写 tools/ 或 像素画/）
  render_pixel_art.py                # JSON 像素画 → 控制台预览 / PNG
  gen_glintstone_pebble.py           # 辉石魔砾卷轴 / 图标 / 粒子贴图
  gen_glintstone_comet_textures.py   # 彗星头 / 光晕贴图
  gen_glintstone_line.py             # 迅魔砾 / 大魔砾 / 流星 / 帚星卷轴与图标
  gen_glintstone_mineral_textures.py # 水晶块 / 簇占位贴图
  gen_glintstone_mineral_assets.py   # 水晶块/簇 blockstate/model/loot/recipe/矿洞 JSON
  *.json                             # 像素画源数据（32×32）
  iss-reference/                     # 对照铁魔法源码时解压的参考（可不提交）

src/main/resources/assets/elden_ring_spells/
  lang/en_us.json, zh_cn.json
  models/item/<item_id>.json
  particles/<particle_id>.json
  textures/item/<item_id>.png
  textures/particle/<particle_id>.png
  textures/gui/spell_icons/<spell_path>.png   # 必须与法术 ResourceLocation path 一致
  sounds.json
  sounds/spell_cast.ogg                       # 飞弹射出
  sounds/spell_cast_start.ogg                 # 蓄力起手

src/main/templates/META-INF/neoforge.mods.toml  # 模组元数据模板（${} 由 Gradle 展开）
```

产物：`build/libs/elden_ring_spells-<version>.jar`

## 依赖与编译约定

- 铁魔法官方文档：https://iron.wiki/developers/
- Maven：`https://code.redspace.io/releases`
- **编译**：优先使用 `compileOnly ...:api`（稳定包 `io.redspace.ironsspellbooks.api.*`）
- **开发运行**：`localRuntime` 拉完整铁魔法 + irons_lib + Curios/GeckoLib/PlayerAnimator
- 若 API 不够用、必须碰非 api 包：改用 full `implementation`/`compileOnly` 无 `:api`，并在 PR/说明里注明「非稳定 API」
- 当前工程因弹道 / `MagicManager` / `DamageSources` 已使用 **full jar `compileOnly`（无 `:api`）**
- 国内网络：已配阿里云 public 镜像；NeoForge 下载失败可重试 `.\gradlew.bat build --refresh-dependencies`

## 新增法术标准流程（AI 必须按此做）

1. 在 `spell/` 新建类，继承 `EldenRingAbstractSpell`（或 `AbstractSpell`）
2. 实现至少：`getSpellResource()`、`getDefaultConfig()`、`getCastType()`
3. 构造里设置 `baseManaCost` / `manaCostPerLevel` / `baseSpellPower` / `spellPowerPerLevel` / `castTime`
4. `DefaultConfig`：`setMinRarity`、`setSchoolResource`、`setMaxLevel`、`setCooldownSeconds`，最后 `.build()`
5. 核心玩法数字：只在 `EldenRingServerConfig` 加 toml 键，`apply` 写到 Spell 运行时字段（详见 `法术解耦架构.md`）
6. 复杂咒（近战/持续/时序）拆到 `spell/curve` / `combat` / `fx`（持续咒再加 `data`，锁人/清障进 `helper`）；视觉写死在这些类里，**不要新建 Tuning**，也**不要把这些函数写回 XxxSpell**
7. 在 `ModSpells` 用 `registerSpell(new YourSpell())` 注册
8. 语言键：`spell.elden_ring_spells.<spell_path>`（en_us + zh_cn 都要）
9. 图标：`assets/elden_ring_spells/textures/gui/spell_icons/<spell_path>.png`
10. 施法逻辑写在 `onCast(...)`；服务端生效时判 `!level.isClientSide`；末尾调用 `super.onCast(...)`
11. 弹道出手音用 `ModSounds.SPELL_CAST`，蓄力起手用 `ModSounds.SPELL_CAST_START`；不要用 `SoundEvents.AMETHYST_BLOCK_CHIME`。卡利亚迅剑除外（斩击自播）

参考：`spell/GlintstonePebbleSpell.java`；完整调用顺序见 `法术解耦架构.md`。

### ResourceLocation / 命名

- 命名空间永远是 `elden_ring_spells`
- path：小写 + 下划线，如 `glintstone_pebble`
- Java 类名：PascalCase + `Spell` 后缀，如 `GlintstonePebbleSpell`
- 注册字段：`SCREAMING_SNAKE`，如 `GLINTSTONE_PEBBLE`

### 学派

短期复用铁魔法内置学派（`SchoolRegistry.FIRE_RESOURCE` 等）。若要做法环独立学派，另开 `registry` + 资源，不要塞进单个 Spell 类里。

### 召唤类法术

铁魔法 3.14+ 使用 `SummonManager`；新召唤法术按官方 wiki「Porting Summons」实现 recast，不要抄旧的 summon timer MobEffect 写法。

## 常用命令

在项目根目录，且 JDK 21 生效时：

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat compileJava
.\gradlew.bat build
.\gradlew.bat runClient
```

- 不要用系统 PATH 里的 JDK 24 编本项目
- 不要全局安装 Gradle；只用 `gradlew.bat`

## AI 改代码时的约束

- **最小改动**：只改任务需要的文件；不顺便重构 MDK 示例残留以外的无关结构
- **不升级** Minecraft / NeoForge / 铁魔法大版本，除非用户明确要求
- **不引入** Fabric、Forge（旧）、Kotlin 为主语言等平行栈
- **不做** 完整铁魔法本体拷贝；扩展逻辑放在本 mod 包下
- 资源与代码同步：加法术 = Java + 双语 lang + 图标（可用占位图）
- **完整注释**：类 / 关键方法 / 可调常量需说明用途与单位；禁止复述代码的废话注释
- **完整变量名**：可读全称，避免含糊缩写；常量名含语义与单位
- **数字分家**：玩法（伤害、蓝耗、弹速、转向角、数量、半径）默认值只写在 `EldenRingServerConfig`，运行时写到 Spell 字段，整合包改 toml **不用重编译**。视觉/动画/握点写死在用到它的类。不要新建 `tuning/`。细则：`法术解耦架构.md`
- 用户未要求时：不改 git 配置、不主动 commit、不推远程

## 像素画 / 工具链

- **所有工具脚本统一放 `工具链/`**（禁止再写到 `tools/`、`像素画/` 等旧目录）
- 画布固定 **32×32**（`工具链/*.json` 的 `width` / `height`）
- 用 `工具链/render_pixel_art.py` 从 JSON 导出 PNG，再放入 `assets/elden_ring_spells/textures/...`
- 未明确要求时不要用其他尺寸（如 16 / 64）

## 已知状态

- [x] NeoForge 1.21.1 工程可 `build`
- [x] 铁魔法 API 编译通过
- [x] `ModItems` / `ModParticles` / `ModSpells` 注册骨架已就绪
- [x] 辉石魔砾：法术 + 卷轴物品 + 弹道（限角追踪）+ 辉石粒子库
- [x] 辉石迅魔砾 / 辉石大魔砾 / 辉石流星 / 帚星（共用 `AbstractGlintstoneProjectile`）
- [x] 辉石碎片 Focus 已替换紫水晶
- [x] 三色辉石矿物方块（水晶簇 / 水晶块，不生长、无建材、无矿石）
- [x] 辉石矿洞 Feature（三色等概率、一洞一色；无矿石矿脉）
- [x] 法术解耦（删 Tuning、瘦 toml、Curve/Combat/Fx）：见 `法术解耦架构.md`
- [ ] 卡利亚迅剑：第一刀右臂抬不到玩家正右方 90°。上一轮修改用户判定全部错误，见 `卡利亚迅剑话题交接.md`
- [ ] 自定义学派 / 法环内容批量设计尚未开始
- [ ] 辉石彗星（Glintstone Cometshard）尚未实现（用户本次未要求）
- [ ] 地表星落坑 / 粉尘装备 / 学院哨塔尚未实现

## 外部文档

- 铁魔法开发者：https://iron.wiki/developers/
- NeoForge 文档：https://docs.neoforged.net/
- 铁魔法源码（对照法术实现）：https://github.com/iron431/irons-spells-n-spellbooks
