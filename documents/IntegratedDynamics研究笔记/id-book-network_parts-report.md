已完整读完 group `network_parts` 的 34 个页面（全部来自 `local://id-book-network_parts.json` / `-sources.md`，无需再抓网页）。按父任务更正要求：**只读 scout，不写文件**；以下为完整报告 + coverage + 部件/Aspect 清单，均由父任务统一保存。

---

# network_parts 研究报告

父任务校审：以下为在线 1.21.1 页面知识；1.20.1 的 API、配置复制等差异见 [总报告](local://id-book-research.md)。`Energy Channel` 不能和 Tunnels 的物流通道混用。书中部件能力目录含其他模组注入的 aspects。

## 0. 结论摘要（高密度）

- **网络模型**：`Logic Cable` 互连构成 network；part 可贴在 cable 的每一面。Wrench：Shift+右键拆线/拆 part，普通右键使 cable 与其它 cable 断开。可用 `Facade`（任意材料外观）隐藏 cable；cable 兼容 **MC MultiPart**（可埋进复杂 facade 结构）。— [networks.html](https://integrateddynamics.rubensworks.net/book/manual/networks.html)
- **Parts 通用四参数**（[introduction.html](https://integrateddynamics.rubensworks.net/book/manual/parts/introduction.html)、[settings.html](https://integrateddynamics.rubensworks.net/book/manual/parts/settings.html)）：
  1. `Ticks/Operation`：每 N tick 执行一次，**值越大越慢**。
  2. `Priority`：同一 network 内同 tick 的执行顺序；priority 大的先执行（B=1 永远早于 A=-1）；**相等则顺序随机**。
  3. `Energy Channel`：取能通道；`-1`=通配（匹配所有通道），`0`=默认通道（无通道设置的部件如 energy cell 用）；**仅当为 network 启用能量消耗时相关**。
  4. `Target Side`：交互目标方块的哪一面；默认 = 部件所贴的那一面（introduction 措辞“实际所贴面”，settings 措辞“所贴 cable 面的对面”，二者等价——都是紧邻的目标方块那一面）。
- **Offset**（[offsets.html](https://integrateddynamics.rubensworks.net/book/manual/parts/offsets.html)）：默认指向紧邻坐标；装 `Part Enhancement` 后才可改，偏移**相对部件目标位置**，**上限 = 已装 enhancement 数**；可手动设、可用 Wrench 的 offset-mode（Shift+右键储蓄位置→右键写入 part）、**也可用 Variable Card 做成 dynamic offset**。
- **Wrench 配置复制**（settings.html）：`Copy All`（整体，仅粘到同类型 part）/ `Copy Settings`（更新间隔、priority、channel、side、offsets）/ `Copy Aspect`（当前 aspect + variable card + 全部 aspect 设置）。后两者可粘到任意 part。**只复制你改过的项**（不会重置未改项）；粘贴时每个 variable card 消耗 1 张空白 `Variable Card`；提高 offset 上限消耗 `Offset Enhancements`；空中右键可打开“将粘贴内容清单”并可逐项关闭。
- **无线/跨维度（重点，勿误判）**：
  - **Mono-Directional Connector**（[connector_mono_directional.html](https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_mono_directional.html)）：两个 connector 连线，**必须成一条直线且距离 ≤ 512 格**；有效连接时显示为 active。**书未声明它可跨维度**（“直线 ≤512”语义上意味同维度）。
  - **Omni-Directional Connector**（[connector_omni_directional.html](https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_omni_directional.html)）：**明确可连接其它维度的 network**，亦可把 >2 个 network 用单个 connector 连起来。首次合成得到唯一 `group id`；同组才能在 network 间连通；每组粒子颜色唯一；group id 见 tooltip 或右键已放置的 connector；可把多个 connector 加入一组（用已有 connector 合成新的）；改装：单独合成 → 全新空组；两个一起合成 → 把第一个的组复制给第二个。
  - ✅ 因此**不能声称无线连接不可跨维度**：omni 支持跨维度；mono 仅同维度直线 ≤512。
- **空页说明**：`writer/entity`、`writer/machine` 在本书内 aspect 列表**为空**（文本止于 “Aspect”）。这不是抓取失败，而是 [introduction.html](https://integrateddynamics.rubensworks.net/book/manual/parts/introduction.html) 明示的机制：“某些 reader/writer 只有在特定其它 mod 存在时才含 aspect；若无 aspect，合成时物品 tooltip 会加警告。”→ Entity/Machine Writer 的 aspect 属于**扩展/其它 mod 提供**，不在 ID 核心。
- **扩展门控**：Refined Storage 的 `RS xxx` 与 Mekanism 的 Radiation/Fission/Chemical 属对应集成。FE 能量读取不能仅凭名字归类为另一个 addon：1.20 核心 Machine Reader 已注册能量相关 aspects，实际可用性取决于目标的能力提供者：[Machine Reader](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/PartTypeMachineReader.java)。

---

## 1. Coverage（34/34 已读）

```json
[
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/diagnostics.html","read":true,"importance":"core","version_issue":"命令语法\\integrateddynamics networkdiagnostics（1.21/1.38）；1.20.1 需源码核对命令注册"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/networks.html","read":true,"importance":"core","version_issue":"逻辑配方含Cobbled Deepslate(1.17+);1.20.1存在同名方块;MC MultiPart兼容性需核对"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/index.html","read":true,"importance":"index","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/introduction.html","read":true,"importance":"core","version_issue":"提到NBT(link nbt_path);1.20.1术语一致性需核"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/offsets.html","read":true,"importance":"core","version_issue":"Part Enhancement/Offset Enhancement命名需与1.20.1核对"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_mono_directional.html","read":true,"importance":"core","version_issue":"512格上限;书未声明跨维度——不要扩大解释"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_omni_directional.html","read":true,"importance":"core","version_issue":"明确支持跨维度;group id机制需与1.20.1核对"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/other/display.html","read":true,"importance":"core","version_issue":"Copy按钮/剪贴板行为客户端实现,版本不敏感"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/other/index.html","read":true,"importance":"index","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/other/light.html","read":true,"importance":"core","version_issue":"Static/Dynamic Light Panel均在;无API暴露aspect"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/audio.html","read":true,"importance":"supporting","version_issue":"含Piglin/Head Note等;1.20.1是否全含需核(1.19+多数存在)"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/block.html","read":true,"importance":"core","version_issue":"'Tile Entity NBT'术语=1.21 BlockEntity改名后仍可能保留;Dimension返回String"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/entity.html","read":true,"importance":"core","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/extra_dimensional.html","read":true,"importance":"core","version_issue":"Tick time单位标注ns需核"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/fluid.html","read":true,"importance":"core","version_issue":"RS network fluids属扩展门控;Tank ID属性索引语义需核"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/index.html","read":true,"importance":"index","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/introduction.html","read":true,"importance":"core","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/inventory.html","read":true,"importance":"core","version_issue":"RS network/craftable/crafting items属扩展门控"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/machine.html","read":true,"importance":"core","version_issue":"FE/RS/Mekanism Fission属扩展门控,非核心"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/network.html","read":true,"importance":"core","version_issue":"chemical系列可能Mekanism门控;Recipes/Active Crafting Jobs属RS/Crafting扩展"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/redstone.html","read":true,"importance":"core","version_issue":"Comparator值语义需与1.20.1核对"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/reader/world.html","read":true,"importance":"core","version_issue":"Radiation属Mekanism门控;Tick time单位ms(与extra-dimensional的ns不同)"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/settings.html","read":true,"importance":"core","version_issue":"Wrench Copy All/Settings/Aspect模式命名需与1.20.1核对"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/audio.html","read":true,"importance":"supporting","version_issue":"Sound/Frequency/Text(tts)需核1.20.1实现"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/effect.html","read":true,"importance":"supporting","version_issue":"粒子清单含1.17+新粒子(Sculk/Glow等);1.20.1子集不同"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/entity.html","read":true,"importance":"core","version_issue":"aspect列表为空——依赖扩展mod;1.20.1需查扩展"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/index.html","read":true,"importance":"index","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/inventory.html","read":true,"importance":"core","version_issue":"全部aspect为RS/Crafting扩展门控,核心无内容"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/machine.html","read":true,"importance":"core","version_issue":"aspect列表为空——依赖扩展mod"},
 {"url":"https://integrateddynamics.rubensworks.net/book/manual/parts/writer/redstone.html","read":true,"importance":"core","version_issue":"无"},
 {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/advancedNetworkComponents.html","read":true,"importance":"tutorial","version_issue":"Materializer/Proxy,非本组part;仅作上下文"},
 {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/aspects.html","read":true,"importance":"tutorial","version_issue":"示例读写联动,无版本敏感点"},
 {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/basicNetworkComponents.html","read":true,"importance":"tutorial","version_issue":"配方含Cobbled Deepslate"},
 {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/networkFoundations.html","read":true,"importance":"tutorial","version_issue":"基础方块配方,版本不敏感"}
]
```

覆盖：**34/34 页面已读**（reader 9 正文 + 1 intro + 1 index；writer 6 + 1 index；other 4 + 1 index；parts 顶层 3；manual 2；tutorial 4）。索引页 3 个，正文页 31 个。

---

## 2. 机制详解

### 2.1 网络与电缆（networks.html）
- network = 相连 `Logic Cable` 集合；part 贴于其上。Wrench 管理：Shift+右键 = 移除 cable/part；普通右键 = 与其它 cable 断开连接。
- 隐藏：`Facade`（可仿任意材料，配方 4×Slimeball + 1×Crystallized Menril Chunk → 32）；cable 兼容 **MC MultiPart**。
- 命名配方（本页）：Logic Cable = Crystallized Menril Chunk ×4 + Stick ×2 + Redstone Dust ×1 → 3；Wrench = Crystallized Menril Chunk ×4 + Redstone Dust ×1。
- **无区块加载/强制加载说明**；本页只说物理连成网络。

### 2.2 Parts 通用语义
- 见摘要第 0 节（四参数、offset、wrench 复制、扩展门控警告）。
- **能量**：仅 `Energy Channel` 涉及；energy 相关 aspect 见 Network Reader。**书未给各 part 的能耗数值/是否默认启用能量消耗**（settings 只说“若为 network 启用能量消耗”）。
- **更新间隔/优先级/通道/面** 语义 = Ticks/Operation、Priority、Energy Channel、Target Side（settings.html）。

### 2.3 Others
- **Light**（[light.html](https://integrateddynamics.rubensworks.net/book/manual/parts/other/light.html)）：静态灯常亮；动态灯（Dynamic Light Panel）由 **Boolean 或 Integer** Variable Card 控制（布尔=开关，整数=亮度档）。无 aspect 概念（非 reader/writer）。
- **Display**（[display.html](https://integrateddynamics.rubensworks.net/book/manual/parts/other/display.html)）：显示一张 Variable Card 的值（任意类型，监控/调试）；Wrench 右键旋转；GUI 内 Copy 按钮或系统复制热键把当前显示值复制到剪贴板。
- **Mono/Omni Connector**：见摘要第 0 节。

### 2.4 Diagnostics（diagnostics.html）
- 仅 OP 可用。命令 `\integrateddynamics networkdiagnostics` 启动**临时本地 web 服务器**；点链接在浏览器打开表格，列出**服务器上所有 network 及其元素**；点击行可在世界中高亮、或 Teleport 玩家过去。用途：查看**跨所有 network 的 tick time**。

### 2.5 Readers（感知）
原则（[reader/introduction.html](https://integrateddynamics.rubensworks.net/book/manual/parts/reader/introduction.html)）：把世界信息读进 `Variable Card`；每个 reader 含 1..N 个 aspect，各有不同 Value Type。完整 aspect 清单见第 3 节。

- **Block**（[block.html](https://integrateddynamics.rubensworks.net/book/manual/parts/reader/block.html)）：面向方块的信息。
- **Entity**：**部件所面向的那个方格内**的实体（注意是“方格内”）。
- **Extra-Dimensional**：服务器级信息（在线玩家等）。
- **Fluid**：读面向的 fluid tank（多 tank，`Tank ID` 属性选 tank）。
- **Inventory**：读面向的 inventory（槽数/物品数），**自身不搬运物品**。
- **Machine**：读面向机器的“工作状态”，含 recipe handler / FE / RS / Mekanism Fission 等集成 aspect。
- **Network**：读**所面向的 ID network**（不必与自身所在 network 相同）。
- **Redstone**：读面向方块的红石信息。
- **World**：读**部件所在世界**的天气/时间等（注意与 Block Reader 的“目标方块处 Light Level”区分：World Reader 含 `Light Level`、`Day Time` 等世界级值）。
- **Audio**：读环境正在播放的音符。

### 2.6 Writers（控制/副作用）
- **Audio Writer**：播放音符/声音/文字转语音。
- **Effect Writer**：按输入 Double 发射粒子（速度=输入值），含偏移/数量/散布属性。
- **Entity Writer**：配置与控制实体——**本书无 aspect（扩展提供）**。
- **Inventory Writer**：inventory/管道相关动作，**自身不搬运物品**；本书全部 aspect 为 RS/Crafting 扩展（触发/取消合成任务）。
- **Machine Writer**：配置与控制机器——**本书无 aspect（扩展提供）**。
- **Redstone Writer**：输出红石信号（Boolean 全信号 / Integer 精确信号 / Pulse 变化触发脉冲），属性 `Strong Power`、`Pulse Value`、`Pulse Length`。

### 2.7 Tutorials（教程实例，压缩）
- **networkFoundations**：搭 ≥10 cable 的网络；合成 Variable Card(24)、Wrench、Input/Output Variable Transformer；卡=存值引用，transformer=做可贴 cable 的读写部件。
- **basicNetworkComponents**：合成 Redstone/Block/Inventory/Machine Reader + Static Light Panel + Display Panel。
- **aspects**：把 Redstone Reader 贴在 cable 上，右键 GUI 把 Integer `Redstone` aspect 绑到 Variable Card；同网络放 Display Panel 显示该卡；再用 Redstone Writer 输出一个由 Reader 读到的最大整型红石值。→ 演示“读 aspect→卡→显示/写”闭环。
- **advancedNetworkComponents**：Materializer（dynamic 卡→static 卡）与 Proxy（变量间接层）。

---

## 3. 部件 / Aspect 清单（完整，含读写类型与属性）

> 标记：R=Reader(感知输出)、W=Writer(控制输入)、D=显示/执行部件。`Properties` 为 GUI 中该 aspect 的可配置项。扩展门控项已标注。registryID **未编造**。

### 3.1 Light / Display / Connectors（D）
| 部件 | 类型 | 输入/输出 | 备注 |
|---|---|---|---|
| Static Light Panel | D | 无 | 常亮 |
| Dynamic Light Panel | D | Boolean/Integer Variable Card | 布尔开关或整数亮度 |
| Display Panel | D | 任意类型 Variable Card | 显示值；可 Wrench 旋转；Copy 到剪贴板 |
| Mono-Directional Connector | D(网络) | — | 直线 ≤512 格，成对 |
| Omni-Directional Connector | D(网络) | — | group id；**可跨维度**；可多网络 |

### 3.2 Reader: Audio（R，输出全为 Integer）
Harp Note / Base Drum Note / Snare Note / Hat Note / Bass Note / Flute Note / Bell Note / Guitar Note / Chime Note / Xylophone Note / Iron Xylophone Note / Cow Bell Note / Didgeridoo Note / Bit Note / Banjo Note / Pling Note / Zombie Note / Skeleton Note / Creeper Note / Dragon Note / Wither Skeleton Note / Piglin Note / Head Note。
- 每项：Reads a … note, expected range [0,24]；Output Integer；Property: `Range`。

### 3.3 Reader: Block（R）
| Aspect | 输出 | Properties |
|---|---|---|
| Has Block | Boolean | — |
| Dimension | String | — |
| X Coordinate / Y Coordinate / Z Coordinate | Integer | — |
| Block | Block | — |
| Tile Entity NBT | NBT | — |
| Biome | String | — |
| Light Level | Integer | — |

### 3.4 Reader: Entity（R）
| Aspect | 输出 | Properties |
|---|---|---|
| Item Frame Rotation | Integer | — |
| Entities | List | — |
| Players | List | — |
| Entity | Entity | `List Element` |
| Item Frame | Item | — |

### 3.5 Reader: Extra-Dimensional（R）
| Aspect | 输出 |
|---|---|
| Random | Integer |
| Player Count | Integer |
| Tick time | Integer |
| TPS | Double |
| Players | List |

### 3.6 Reader: Fluid（R）
| Aspect | 输出 | Properties |
|---|---|---|
| Tank Full / Tank Empty / Tank Not Empty / Is Tank | Boolean | — |
| Fluid Amount | Integer | `Tank ID` |
| Total Fluid Amount | Integer | — |
| Fluid Capacity | Integer | `Tank ID` |
| Total Fluid Capacity | Integer | — |
| Tanks | Integer | — |
| Fluid Fill Ratio | Double | `Tank ID` |
| Tank Fluids | List | — |
| Tank Capacities | List | — |
| Tank Fluid | Fluid | `Tank ID` |
| Block Fluid | Fluid | — |
| RS network fluids | List | ⚠ Refined Storage 扩展 |

### 3.7 Reader: Inventory（R）
| Aspect | 输出 | Properties |
|---|---|---|
| Inventory Full / Inventory Empty / Inventory Not Empty / Is Inventory | Boolean | — |
| Inventory Count | Integer | — |
| Slots | Integer | — |
| Slots Filled | Integer | — |
| Fill Ratio | Double | — |
| Items | List | — |
| Slot Item | Item | `Slot ID` |
| RS network items | List | ⚠ RS 扩展 |
| RS craftable items | List | ⚠ RS 扩展 |
| RS crafting items | List | ⚠ RS 扩展 |

### 3.8 Reader: Machine（R）
核心（worker/recipe）：
| Aspect | 输出 | Properties |
|---|---|---|
| Is Worker / Has Work / Can Work / Is Working / Has Temperature | Boolean | — |
| Temperature / Maximum Temperature / Minimum Temperature / Default Temperature | Double | — |
| Is Recipe Handler | Boolean | — |
| Recipes | List | — |
| Recipe By Input / Recipe By Output / Recipes By Input / Recipes By Output / Recipe Output By Input / Recipe Inputs By Output | Operator | — |
扩展（FE = Forge Energy；RS；Mekanism）：
| Aspect | 输出 |
|---|---|
| Is FE Handler / Is FE Receiver / Is FE Provider / Can Extract FE / Can Insert FE / Is FE Buffer Full / Is FE Buffer Empty / Is FE Buffer Not Empty | Boolean |
| Stored FE / FE Capacity | Integer |
| FE Fill Ratio | Double |
| Is RS Network | Boolean ⚠ RS |
| Fission Reactor Damage / Actual Burn Rate / Maximum Burn Rate / Set Burn Rate / Environmental Loss | Double ⚠ Mekanism |
| Fission Reactor Heating Rate | Long ⚠ Mekanism |

### 3.9 Reader: Network（R）
| Aspect | 输出 | Properties |
|---|---|---|
| Is Network | Boolean | — |
| Elements | Integer | — |
| Energy Batteries | Integer | — |
| Energy Stored | Integer | `Channel` |
| Energy Capacity | Integer | `Channel` |
| Energy Consumption Rate | Integer | — |
| Value | Any | — |
| Variable Value By ID | Operator | — |
| Network Item Count | Long | `Channel` |
| Network Item Max Count | Long | `Channel` |
| Network Items | List | `Channel` |
| Network Count Of Item | Operator | `Channel` |
| Network Item Interfaces | Integer | `Channel` |
| Network Fluid Count / Fluid Max Count | Long | `Channel` |
| Network Fluids | List | `Channel` |
| Network Count Of Fluid | Operator | `Channel` |
| Network Fluid Interfaces | Integer | `Channel` |
| Network Chemical Count / Chemical Max Count | Long | `Channel` ⚠ chemical |
| Network Chemicals | List | `Channel` ⚠ |
| Network Count Of Chemical | Operator | `Channel` ⚠ |
| Network Chemical Interfaces | Integer | `Channel` ⚠ |
| Recipes | List | `Channel` ⚠ RS/Crafting |
| Active Crafting Jobs | List | `Channel` ⚠ |
| Crafting Ingredients | List | `Channel` ⚠ |
> 关键：network 可**暴露一个 value**（`Value` aspect）；`Channel` 属性用于按通道查询存量。

### 3.10 Reader: Redstone（R）
| Aspect | 输出 | Properties |
|---|---|---|
| Redstone Low | Boolean | — |
| Redstone | Boolean | — |
| Redstone High | Boolean | — |
| Redstone Clock | Boolean | `Pulse Interval`、`Pulse Length`、`Pulse Time Offset` |
| Redstone | Integer | — |
| Comparator | Integer | — |
（两个同名 `Redstone`：一个 Boolean、一个 Integer，按绑定类型区分。）

### 3.11 Reader: World（R）
| Aspect | 输出 |
|---|---|
| Weather: Clear / Weather: Rain / Weather: Thunder | Boolean |
| Is Day / Is Night | Boolean |
| Rain Countdown | Integer |
| Tick time | Integer（ms，世界平均） |
| Day Time | Integer |
| Light Level | Integer |
| TPS | Double |
| Time | Long（受 set time 影响） |
| Total Time | Long（不受影响） |
| World Name | String |
| Players | List |
| Radiation | Double ⚠ Mekanism |

### 3.12 Writer: Audio（W，输入 Integer，Property: `Volume`）
与 Reader Audio 同名音符（Harp…Head Note），另加：
- `Sound`：输入 String（播放给定声音名），Properties `Volume`、`Frequency`。
- `Text`：输入 String（文字转语音），Property `Range`。

### 3.13 Writer: Effect（W，输入 Double，Properties：`X Offset`、`Y Offset`、`Z Offset`、`Particle Count`、`X Spread`、`Y Spread`、`Z Spread`）
粒子 aspect（“Emit a … particle with a certain velocity”）：Angry Villager, Bubble, Cloud, Crit, Damage Indicator, Dragon Breath, Dripping Lava, Falling Lava, Landing Lava, Dripping Water, Falling Water, Effect, Elder Guardian, Enchanted Hit, Enchanted, End Rod, Explosion Emitter, Explosion, Sonic Boom, Firework, Fishing, Flame, Sculk Soul, Sculk Charge Pop, Soul Fire Flame, Soul, Flash, Happy Villager, Composter, Heart, Instant Effect, Slime, Snowball, Large Smoke, Lava, Mycelium, Note, Poof, Portal, Rain, Smoke, Sneeze, Spit, Squid Ink, Sweep Attack, Totem of Undying, Underwater, Splash, Witch, Bubble Pop, Downwards Current, Upwards Current, Nautilus, Dolphin, Cosy Campfire Smoke, Signal Campfire Smoke, Dripping Honey, Falling Honey, Landing Honey, Falling Nectar, Falling Spore Blossom, Ash, Crimson Spore, Warped Spore, Spore Blossom Air, Dripping Obsidian Tear, Falling Obsidian Tear, Landing Obsidian Tear, Reverse Portal, White Ash, Small Flame, Snowflake, Dripping Dripstone Lava, Falling Dripstone Lava, Dripping Dripstone Water, Falling Dripstone Water, Glow Squid Ink, Glow, Wax On, Wax Off, Electric Spark, Scrape。
> ⚠ 版本：Sculk/Glow/Ash/Small Flame/Snowflake 等为 1.17+ 粒子；1.20.1 子集需源码核对。

### 3.14 Writer: Entity（W） — **本书无 aspect（扩展提供）**
### 3.15 Writer: Inventory（W） — 全部为 RS/Crafting 扩展
| Aspect | 输入 | Properties |
|---|---|---|
| RS craft item | Item | `Skip If In Storage`、`Crafting Count` |
| RS craft items | List | `Skip If In Storage`、`Crafting Count` |
| RS cancel all crafting jobs | Boolean | — |
| RS cancel crafting jobs | Item | — |
| RS cancel crafting jobs | List | — |
### 3.16 Writer: Machine（W） — **本书无 aspect（扩展提供）**

### 3.17 Writer: Redstone（W，Property: `Strong Power`）
| Aspect | 输入 | 附加 Properties |
|---|---|---|
| Redstone | Boolean（true→满红石信号） | — |
| Redstone | Integer（精确信号） | — |
| Redstone Pulse | Boolean（值变化时发脉冲） | `Pulse Value`、`Pulse Length` |
| Redstone Pulse | Integer（值变化时发脉冲） | `Pulse Value`、`Pulse Length` |

---

## 4. 对 DSL 设计的约束（仅事实/边界，不定语法）

1. **Part 能力三分**：感知型（Reader，产出值绑定到 Variable Card）、控制型（Writer，消费 Variable Card 值触发副作用）、显示/执行型（Display/Light/Connector）。DSL 需能表达“读 aspect→卡→写/显示”的数据流。
2. **运行参数必须可表达**：Ticks/Operation（周期）、Priority（同 tick 相对次序，可相等→随机）、Energy Channel（-1 通配 / 0 默认）、Target Side（目标面）。
3. **目标定位**：默认紧邻目标方块；offset 需 Part Enhancement 解锁，可为**静态或 dynamic（由 Variable Card 驱动）**；上限=enhancement 数。DSL 若要表达“改变目标点”必须考虑动态 offset。
4. **值类型体系**（本组出现的输出类型）：Boolean、Integer、Long、Double、String、List、Item、Block、Fluid、Entity、NBT、Operator、Any。Reader 输出、Writer 输入如上表。
5. **属性化 aspect**：多 tank（`Tank ID`）、多槽（`Slot ID`）、列表元素（`List Element`）、通道（`Channel`）、脉冲（`Pulse Interval/Length/Offset/Value`）、粒子偏移/散布/数量、`Strong Power`、音量/频率/范围。DSL 必须允许 aspect 带参数化设置。
6. **Operator 型输出**（Machine Reader 的 Recipe By Input 等；Network Reader 的 Count Of Item 等）：aspect 可不只是“值”，而可返回**可调用算子**——DSL 需支持 higher-order（函数值）。
7. **网络级量**：Network Reader 可查询网络的能量、item/fluid/chemical 存量（按 Channel）、暴露的 `Value`、以及通过 `Variable Value By ID` 按整数 id 取值 → 存在“variable 的整数标识符”概念。
8. **跨网络/跨维度**：Omni Connector 支持**跨维度**与多网络合并（group id）；Mono 仅同维度直线 ≤512。DSL 需允许“网络边界跨维度”这一事实，不可假设单维度。
9. **能量/通道**：energy 消耗与 Channel 相关，且“仅当启用能量消耗时适用”；书未给默认能耗模型。
10. **无 aspect 的 part**：Entity/Machine Writer 在本环境无核心 aspect；DSL 应把“部件可能有 0 个可用 aspect（依赖 mod）”作为合法状态。
11. **无区块加载语义披露**：本书未说明任何 part/connector 的区块加载行为——DSL 不应臆断自动加载。

---

## 5. 需要父任务确认的版本项

1. **1.21.1/NeoForge/ID1.38.0 vs 目标 1.20.1 Forge（master-1.20-lts @69ea239d）**：所有 aspect 名、属性名是否逐字一致需源码核对；本书不保证适用于 1.20.1。
2. **扩展归属**：`RS *`（Refined Storage）、`Fission Reactor *`/`Radiation`/chemical（Mekanism）、`FE *`（Forge Energy）、Writer `Effect`/`Audio` 的 Sound/Text 究竟哪个是 ID 核心、哪个是扩展，需以 1.20.1 源码/mod 判定。
3. **reader/machine 的 `Tile Entity NBT`**：1.21 已改称 BlockEntity；1.20.1 是否仍为 Tile Entity，需核（可能只是 aspect 显示名，底层类型一致）。
4. **reader/network 的 `chemical` 系列**：依赖哪个 mod（Mekanism gas/slurry?）需确认；1.20.1 是否存在。
5. **writer/effect 粒子清单**：1.20.1 支持的粒子子集不同（Sculk/Glow/Ash/Snowflake 等为后续版本新增），需按 1.20.1 源码裁剪。
6. **reader/audio & writer/audio 音符清单**：Piglin Note、Head Note 等是否为 1.20.1 具备。
7. **命令 `\integrateddynamics networkdiagnostics`** 与临时 web server 行为在 1.20.1/Forge 是否一致。
8. **能量模型**：1.20.1 中“为 network 启用能量消耗”的开关与各 part 能耗数值（本书未给）。
9. **`Dimension` 返回 String 的具体格式**（如 `minecraft:overworld`）需源码确认。
10. **Mono Connector 是否允许跨维度**：本书仅写“直线 ≤512 格”，**未声明跨维度**；请勿与 Omni 的跨维度混合解释（用户明确提醒勿误判无线不可跨维度——omni 明确支持跨维度，mono 未声称）。

---

**产物路径**：父任务请将以上内容保存为 `local://id-book-network_parts-report.md`；coverage JSON 见第 1 节，保存为 `local://id-book-network_parts-coverage.json`；Aspect 全目录见第 3 节，可另存 `local://id-book-parts-inventory.json`。
**覆盖数量**：34/34 分配 URL 全部读完（正文 31、索引 3）。
**核心版本确认项**：见第 5 节（重点：扩展门控归属、1.20.1 aspect/粒子/音符子集、无线跨维度以 omni 为准）。
