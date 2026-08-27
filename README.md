# Elden Ring Spells（法环 · 铁魔法扩展）

Minecraft **1.21.1** / **NeoForge** 扩展模组，依赖 [Iron's Spells 'n Spellbooks](https://iron.wiki/developers/)，主题为《艾尔登法环》。

## 给 AI / 协作者

后续改代码请先读：

- **[代码阅读路径.md](./代码阅读路径.md)** — 从哪个文件点进去、下一份打开谁（顺着读）
- **[AGENTS.md](./AGENTS.md)** — 完整技术说明、目录、加法术流程、命令与约束  
- **[.cursor/rules/elden-ring-spells.mdc](./.cursor/rules/elden-ring-spells.mdc)** — Cursor 常驻规则（自动带入对话）

## 快速命令

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat build
.\gradlew.bat runClient
```

需要 **JDK 21**。模组 ID：`elden_ring_spells`。

## 现状

已可构建；示例法术 `glintstone_pebble` 已注册，施法效果仍为占位。
