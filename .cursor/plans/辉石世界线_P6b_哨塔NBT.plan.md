---
name: 辉石世界线 P6b 哨塔NBT
overview: 在创造里搭建学院哨塔（底层教室 + 顶层观星台）并导出正式 NBT。外观：原版石砖 + 青色水晶块/簇点缀，内部空研究空间，无任何生物。本阶段必须进游戏。
todos:
  - id: build-classroom
    content: 创造里搭底层教室：桌椅/书架/箱子，导出 NBT
    status: pending
  - id: build-observatory
    content: 创造里搭顶层观星台：青色水晶块/簇点缀，导出 NBT
    status: pending
  - id: replace-placeholder
    content: 用正式 NBT 覆盖占位；确认 jigsaw 接口与 P6a 一致
    status: pending
  - id: verify-look
    content: 新世界 /locate 验证；外观不像铁魔法火焰塔；内部无生物
    status: pending
isProject: false
---

# P6b — 学院哨塔正式 NBT

**一次只做本文件。依赖 P6a JSON 已能生成。本阶段要进游戏搭。做完即停。**

索引：[辉石世界线总计划](辉石世界线总计划_f9cb546f.plan.md)

## 目标

一座小塔，地上能看见「有人在研究辉石」。外观一眼石砖 + 青色晶体，不会被认成铁魔法火焰塔。走进去是空的研究空间。

## 必须遵守

- **内部没有任何生物。** 不放刷怪笼。
- 不要改成学院本体。
- 箱子沿用 P6a 战利品。
- **建材：原版石砖 + P1 青色水晶块 / 簇。** 没有辉石砖。

## 房间

| 建议文件 | 内容 |
|------|------|
| 底层教室 | 桌椅 / 书架 / 箱子 |
| 顶层观星台 | 开阔、青色晶体点缀 |

若 P6a 是单件结构，也可以做成一座完整小塔一个 NBT。

## 工作流程（进游戏）

1. `.\gradlew.bat runClient`（JDK 21）
2. 创造里用石砖 + 青色水晶块/簇搭建
3. 结构方块保存，拷进 `data/elden_ring_spells/structure/`
4. 开新世界 `/locate` 验证

## 不要做

- 自定义生物、刷怪笼、钥匙门
- 地图交易系统
- 返工矿洞或星落坑

## 验收

- `/locate` 能找到哨塔
- 外观一眼石砖 + 青色晶体
- 走进去是空的研究空间，没有怪
- [AGENTS.md](../../AGENTS.md) 勾：学院哨塔正式 NBT
