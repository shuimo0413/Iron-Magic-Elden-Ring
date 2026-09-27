# NeoForge 1.21.1 专用数据包（本分支不打包）

`irons_apothic`（[Apotheosis x Iron's Spellbooks Compat](https://www.curseforge.com/minecraft/mc-mods/apotheosis-x-irons-spellbooks-compat)）**仅有 NeoForge 1.21.1**，官方明确不做 1.20.1 回移。

本目录从 `src/main/resources/data/` 挪出，避免 Forge 1.20.1 jar 打进无效的：

- `neoforge:conditions` / `data_maps`（1.21 NeoForge 特性）
- 依赖不存在的 `irons_apothic` 模组 ID

**主线 `neoforge-1.21.1` 分支仍应把这些文件放在 `src/main/resources/data/`。**  
本 `forge-1.20.1` 分支若要神化兼容，请另接 1.20.1 方案（如 Fallen Gems & Affixes / Apothic Compat），不要照搬本目录 JSON。
