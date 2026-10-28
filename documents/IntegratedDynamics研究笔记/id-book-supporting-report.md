# 教程支持章节研读：supporting（15 页）

## 范围与证据

入口、引言、目录、标签索引、材料与基础机器全部覆盖。每页正文和附录已保存在 [分片原文](local://id-book-supporting.json)，原始 HTML 在 [全书快照](local://id-book-raw-snapshot.json)，准确页面清单在 [coverage](local://id-book-supporting-coverage.json)。配方的名称、时间、产出等展示数据保留在抓取文本与原始 HTML 中；没有在游戏里逐条验证配方。正文机制如下。

## 基础材料与进度

- [Menril Trees](https://integrateddynamics.rubensworks.net/book/manual/prerequisites/menril_trees.html)：蓝色高树；叶与浆果发光，浆果可食用；木材含树脂，是大量 ID 物品的材料来源。
- [Meneglin Biome](https://integrateddynamics.rubensworks.net/book/manual/prerequisites/meneglin_biome.html)：Menril 树集中出现的生物群系；树脂影响周边颜色。属于世界生成和材料获取，不是表达式求值能力。
- [Squeezer](https://integrateddynamics.rubensworks.net/book/manual/prerequisites/squeezer.html)：通过实体跳踩/落下挤压，不限玩家；物品产物落入相邻库存或掉到地上；流体留在机器中，在存在目标储罐时向两个侧面之一流动；红石脉冲可重置。主要用于从 Menril Logs 获取 Menril Resin。
- [Drying Basin](https://integrateddynamics.rubensworks.net/book/manual/prerequisites/drying_basin.html)：将物品或流体干燥，树脂转为 Crystallized Menril；书警告不要加入热流体。具体配方和默认耗时属于在线生成环境，不直接保证目标 1.20.1 的相同数值。
- [Mechanical Squeezer](https://integrateddynamics.rubensworks.net/book/manual/machines/mechanical_squeezer.html)：能量驱动替代手动跳踩，通常更快并提供更高产出；附录包含多种挤压配方。
- [Mechanical Drying Basin](https://integrateddynamics.rubensworks.net/book/manual/machines/mechanical_drying_basin.html)：能量驱动、更快的干燥机；附录包括树脂/液态 Chorus 结晶、海绵干燥、物品转化等配方。不会改变变量卡的语言机制。

## 能量基础设施

- [Energy Battery](https://integrateddynamics.rubensworks.net/book/manual/machines/energy_battery.html)：网络能量消耗是配置项，不是每个环境一律强制开启。电池增加网络储能容量；书列 RF/Tesla 能量输入。红石信号启用各面自动输出；背包中 Shift+右键切换给双手物品供能的 auto-supply。能量系统是否工作取决于实际配置与目标能力。
- [Generator](https://integrateddynamics.rubensworks.net/book/manual/machines/generator.html)：用可燃物产生能量。不是 DSL 必须实现的内部求值过程，属于网络运行前提。

## 导航与默认配置边界

- [入口](https://integrateddynamics.rubensworks.net/book/index.html)明确：书中数据基于默认配置，实际环境可不同；生成环境为 1.21.1/NeoForge，且包含多个扩展。
- [Introduction](https://integrateddynamics.rubensworks.net/book/introduction.html)：教程帮助学习核心概念，手册提供机制查阅；配方项和正文链接提供跨章节导航。
- Manual、Machines、Prerequisites 目录与 Tag Index 仅提供入口，不产生额外运行语义；同内容目录 URL 与 index.html 别名已归并。

## 对后续 DSL 的约束（不定语法）

1. 不把材料配方、能量默认值写成语言的硬编码事实。
2. 将计算程序与网络实际可运行条件分开；缺能量、未加载、缺部件与语法/类型错误不同。
3. 基础机器可作为读取/执行目标，但其读写能力取决于实际 capability/aspect，而不是页面上能看到一种机器就认定存在任意控制接口。
4. 合成原生卡片与取得真实物品资源是两种行为；资源取得和世界副作用不能由客户端提交的计算计划凭空决定。
