---
name: 辉石世界线 P6a 哨塔数据包
overview: 学院哨塔的 jigsaw JSON、群系标签、箱子战利品、最小占位 NBT。建材用原版石砖 + 青色水晶块/簇点缀（无辉石砖）。不做学院本体，不放任何生物。正式外观留给 P6b。做完即停。
todos:
  - id: structure-json
    content: 单件或两件 jigsaw JSON，spawn_overrides 清空
    status: pending
  - id: biome-tag
    content: 河流/沼泽/针叶林/雪原群系标签
    status: pending
  - id: chest-loot
    content: 中阶卷轴 + 碎片；不要做成地图交易系统
    status: pending
  - id: placeholder-nbt
    content: 最小占位 NBT，让 /locate 能找到
    status: pending
  - id: compile-accept
    content: compileJava 通过；AGENTS.md 勾 P6a
    status: pending
isProject: false
---

# P6a — 学院哨塔数据包骨架

**一次只做本文件。依赖 P1 青色水晶块/簇。跳过已取消的阶段 5。做完即停，正式搭塔留给 P6b。**

索引：[辉石世界线总计划](辉石世界线总计划_f9cb546f.plan.md)

## 目标

地上能被 `/locate` 找到的小塔骨架，暗示「有人在研究辉石」。不是水轮、辩论室、满月女王。

## 必须遵守

- **内部没有任何生物。** `spawn_overrides` 清空；NBT 不放刷怪笼、不放盔甲架以外的实体。
- 不做钥匙门、多庭院、Boss、地图交易系统。
- 不加新群系。不改法术渲染。
- **建材：原版石砖 + P1 青色水晶块 / 簇点缀。** 没有辉石砖。

## 要做的数据包

- `worldgen/structure/` 单件或两件 jigsaw：底层教室 + 顶层观星台
- `structure_set` + `template_pool`
- `tags/worldgen/biome/has_structure/...`：河流、沼泽、针叶林、雪原
- 箱子：中阶卷轴、碎片
- 间距写进 `GlintstoneWorldTuning`，稀有
- 占位 NBT：能立起来的石砖小盒子即可

## 不要做

- 精心外观（P6b）
- 雷亚卢卡利亚本体、自定义生物
- 矿洞 / 星落坑返工

## 验收

- `/locate` 能找到哨塔结构
- 走进去没有怪
- `.\gradlew.bat compileJava` 通过
- [AGENTS.md](../../AGENTS.md) 勾：学院哨塔数据包骨架（占位 NBT）
