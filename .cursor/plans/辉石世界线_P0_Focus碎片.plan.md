---
name: 辉石世界线 P0 Focus碎片
overview: 注册青/蓝/紫三种辉石碎片，替换紫水晶触媒。创造栏可测锻造台，不写合成表、不加方块、不改世界生成。做完即停。
todos:
  - id: register-shards
    content: ModItems 注册 cyan/blue/purple_glintstone_shard
    status: completed
  - id: focus-tags
    content: 改 glintstone_focus 与 school_focus，去掉紫水晶；school_focus 改引用本模组标签
    status: completed
  - id: shard-art
    content: 工具链 32×32 三种碎片贴图 + item model + 双语 lang
    status: completed
  - id: creative-tab
    content: 创造栏在卷轴列表前插入三种碎片
    status: completed
  - id: compile-accept
    content: compileJava 通过；AGENTS.md 勾 P0
    status: completed
isProject: false
---

# P0 — Focus 落地（最小可玩）

**一次只做本文件。做完验收后停，不要开始 P1。**

索引：[辉石世界线总计划](辉石世界线总计划_f9cb546f.plan.md)

## 目标

生存里用真正的辉石碎片当锻造台触媒，不再用紫水晶。创造栏可测。无合成表。

## 必须遵守

- 不写紫水晶 / 青金石等原版材料合成辉石的配方。
- 不加方块、矿洞、结构、装备。
- 不注册生物。不改法术渲染。
- 贴图走 `工具链/`，画布 32×32。

## 要改的文件

- [ModItems.java](../../src/main/java/com/eldenring/spells/registry/ModItems.java)：注册
  - `cyan_glintstone_shard`
  - `blue_glintstone_shard`
  - `purple_glintstone_shard`
- [glintstone_focus.json](../../src/main/resources/data/elden_ring_spells/tags/item/glintstone_focus.json)：写入三种碎片，**去掉** `minecraft:amethyst_shard`
- [school_focus.json](../../src/main/resources/data/irons_spellbooks/tags/item/school_focus.json)：改为 `#elden_ring_spells:glintstone_focus`，避免两处各写一份物品列表
- 可选：把碎片加进 `irons_spellbooks:lootable_focus`，让铁魔法结构宝箱也能滚到
- [ModCreativeTabs.java](../../src/main/java/com/eldenring/spells/registry/ModCreativeTabs.java)：卷轴列表前插入碎片
- `工具链/` 画三种碎片 JSON，用 `工具链/render_pixel_art.py` 导出，拷到 `textures/item/`
- `models/item/<id>.json`
- `lang/en_us.json` + `zh_cn.json`：青色辉石碎片 / 蓝色辉石碎片 / 紫色辉石碎片

## 不要做

- 任何 `recipe/`
- `ModBlocks`、worldgen、战利品表（除可选 lootable_focus 标签）
- 粉尘、法杖、袍子

## 验收

- 创造栏能拿到任一色碎片
- 卷轴锻造台用碎片能出辉石咒
- 紫水晶不再当辉石触媒
- JEI 里没有辉石碎片合成表
- `.\gradlew.bat compileJava` 通过
- [AGENTS.md](../../AGENTS.md)「已知状态」勾：辉石碎片 Focus 已替换紫水晶
