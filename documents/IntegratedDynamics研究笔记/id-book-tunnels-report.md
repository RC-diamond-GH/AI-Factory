# Integrated Tunnels (group: tunnels) 完整阅读报告

**阅读范围**: `local://id-book-tunnels.json` / `-sources.md` 全部 **29 页面**（12 手册页 + 17 教程页），逐页读完，无跳过。以下所有结论均附原始 URL；`[事实]`=书内明文；`[推断]`=需父任务源码核查。**未写任何文件**（按父任务更正，报告与 coverage 全部在本消息输出）。

---

## 1. 总体架构

- **定位**: Integrated Tunnels 是 Integrated Dynamics 的扩展，跨 ID 网络搬运 **item / fluid / energy**，并做世界交互与玩家模拟。逻辑过滤器用 ID 的 variable/operator 构建（`introduction.html`）。[事实]
  - https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/introduction.html
- **部件模型（Concepts）**: 共 4 种部件形态 —— `Importer`(蓝色)、`Exporter`(橙色)、`Interface`(白色) 两种（普通 / Filtering）。Importer 永远从 target **拉**到同网络内的 Interface；Exporter 永远从 Interface **推**到 target。“Items 总是从左到右”。可加多个 Interface 并设 **priority** 决定优先填充/排空；也可加多个 Importer/Exporter 并设 relative priority。Filtering Interface 用于限定何物可通过（如锁定库存只允许特定物品）。用 ID variable 指定搬运内容；**空白 Variable Card = 搬运全部**。装 Integrated Crafting 时 Exporter 可用 `Craft`：缺料先自动合成。[事实]
  - https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/concepts.html
- **Interface 教程**: Interface 是挂在 **Cable** 上的部件，分别连接 inventory / tank / energy machine，把 target 暴露给网络。[事实]
  - https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/interfaces.html
- **合成关系**: `Interface`(item/fluid/energy) + `Input Variable Transformer` = Importer；`Interface` + `Output Variable Transformer` = Exporter；两个 Interface = Filtering Interface。World 系 = 对应 Importer/Exporter + `Logic Director`。`Player Simulator` 由 4 个 World 部件 + Emerald 合成。[事实]
  - item/fluid/energy.html、world/*.html、player.html（URL 见覆盖表）
- **Aspect 概念**（ID 核心）: 每个可执行操作是一个 **Aspect**，带 `Input`（类型）与一组 `Properties`。Importer/Exporter/Interface/World/Player 的操作全部以 aspect 形式暴露，绑定在部件上执行。[事实]

---

## 2. 共享属性（书内**只列名、无语义**）

下表属性出现在几乎所有 aspect 上，书内**从未解释行为**——这是未来 DSL 语义必须从源码补齐的清单。[事实/缺失]

| 属性 | 出现域 | 书内语义 |
|---|---|---|
| `Channel` | 几乎所有 transfer aspect | 未说明（[推断] 用于同部件多逻辑通道配对 importer/exporter） |
| `Round-Robin` | 多数 import / export-all / world 系 | 未说明（[推断] 多 Interface/多 target 间轮询分配） |
| `Item/Energy/Fluid Transfer Rate` | 各域 | 未说明（[推断] 每 tick 速率上限；默认值需源码） |
| `Passive Interaction` / `Passive Interaction Ignores Filter` | item/fluid/energy 的 transfer aspect | 未说明。**⚠ 不得描述为“关闭主动搬运”**；语义待源码核查 |
| `Craft` | 多个 export/place/click aspect | 未说明（[推断] 依赖 Integrated Crafting 自动合成缺料） |
| `Check Stack Size` / `Check Fluid Amount` / `Check Energy Amount` | 各域 | 未说明（[推断] 比对上/下限的开关） |
| `Item Slot` | item 域 | 有说明（见 §4） |
| `Slot-based` | predicate aspect | 未说明 |
| `NBT subset` / `NBT superset` / `Require Item/Fluid NBT` / `Check NBT Recursively` | NBT aspect | 未说明（[推断] NBT 匹配模式） |
| `Blacklist` | 多数 filter/item aspect | 未说明（[推断] 反转匹配） |
| `Empty is Wildcard` | Item/Fluid aspect | 未说明（[推断] 空值匹配任意，呼应空白卡=全部） |
| `Apply to Insertions` / `Apply to Extractions` / `Allow if not applied` | 全部 Filter aspect（Filtering Interface） | 未说明（[推断] 控制过滤方向与未作用时的默认放行） |
| `Ignore Replacable`（原文拼写如此） | world block/fluid place | 未说明（[推断] 允许覆盖可替换方块，如草） |
| `Cause Block Update` / `Right Hand` / `Silk Touch` / `Match Block` / `Break Block When No Drops` | world block | 未说明 |
| `Dispense` / `Offset X/Y/Z` / `Lifespan (ticks)` / `Pickup Delay (ticks)` / `Velocity` / `Yaw` / `Pitch` / `Ignore Pickup Delay` | world item | 未说明 |
| `Entity Index` | world 全系 + player | 未说明（[推断] 选定面向前方实体） |

---

## 3. 各传输域的 Aspect 全集

### 3.1 Items（`item.html`）
Export 侧: `Export All Items`(Boolean)、`Export Item Amount`(Integer,每 tick)、`Export Item Slot`(Integer)、`Export Item`(Item)、`Export Items`(List)、`Export Items Predicate`(Operator)、`Export Items NBT`(NBT)。
Import 侧: 同名 `Import *`（`Import Item Amount` 无 Round-Robin/Transfer Rate? 实为有 Round-Robin）。
Filter 侧（Filtering Interface）: `Filter All Items`(Boolean)、`Filter Item`(Item)、`Filter Items`(List)、`Filter Items Predicate`(Operator)、`Filter Items NBT`(NBT)。
- **关键类型语义** `[事实]`:
  - `Input: Boolean` 的 aspect（All Items / Filter All Items）是**开关/闸门**，不选物；真正选物需 `Item`/`List`/`Operator`/`NBT` 输入。**不得假定 Boolean 卡能筛物品。**
  - `Export/Import Item Slot`: **-1 = 任意槽，-2 或更小 = 无槽**（原文）。
  - `Export Item Amount` / `Import Item Amount`: Integer，语义“each tick 搬给定数量”。
  - `Export Items`/`Import Items` 带 `Craft`；`Import Items`(List) 教程中来自 Inventory Reader。
  - https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/item.html

### 3.2 Fluids（`fluid.html`）
Export/Import 侧: `Export All Fluids`(Boolean)、`Export Fluid Amount`(Integer)、`Export Fluid`(Fluid)、`Export Fluids`(List)、`Export Fluids Predicate`(Operator)、`Export Fluids NBT`(NBT)；Import 同名镜像。
Filter 侧: `Filter All Fluids`(Boolean)、`Filter Fluid`(Fluid)、`Filter Fluids`(List)、`Filter Fluids Predicate`(Operator)、`Filter Fluids NBT`(NBT)。
- Fluid 侧额外属性 `Empty is Wildcard`、`Check NBT`；`Export Fluid Amount` 无 Round-Robin。`[事实]`
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/fluid.html

### 3.3 Energy（`energy.html`）
`Export Energy`(Boolean)、`Export Energy Amount`(Input: **Long**,每 tick)、`Import Energy`(Boolean)、`Import Energy Amount`(Long)、`Filter All Energy`(Boolean)。
- 注意 **Energy 数量输入为 Long**（与 Item/Fluid 的 Integer 不同）。`[事实]`
- Export 侧有 `Craft`、`Passive Interaction*`、`Check Energy Amount`、`Energy Transfer Rate`；Import 侧有 `Round-Robin`。`[事实]`
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/energy.html

---

## 4. 世界交互（World Interaction）

概览页仅目录（Items/Fluids/Energy/Block）。[事实]
https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/index.html

### 4.1 World Block（`world/block.html`）
- 用 `World Block Importer` 破坏、`World Block Exporter` 放置。支持 silk touch 等属性。`[事实]`
- Place 侧: `Place All Item Blocks`(Boolean)、`Place Item Block`(Item)、`Place Item Blocks`(List)、`Place Item Blocks Predicate`(Operator)、`Place Item Blocks NBT`(NBT)、`Place Block`(Block)、`Place Blocks`(List)、`Place Blocks Predicate`(Operator)。
- Pick Up 侧: `Pick Up All Item Blocks`(Boolean)、`Pick Up Item Block`(Item)、`Pick Up Item Blocks`(List)、`Pick Up Item Blocks Predicate`、`Pick Up Item Blocks NBT`、`Pick Up Block`(Block)、`Pick Up Blocks`(List)、`Pick Up Blocks Predicate`。
- 破坏类属性含 `Silk Touch`、`Break Block When No Drops`、`Match Block`；放置类含 `Ignore Replacable`、`Cause Block Update`。`[事实]`
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/block.html

### 4.2 World Item（`world/item.html`）
- 放置物品到世界、拾取、并与带 inventory 的实体交互（掉落潜影盒、Donkey）。属性可调 dispensing、throw-velocity、pickup delay、pitch、yaw。`[事实]`
- 放置侧: `Place All Item Entities`(Boolean)、`Place Item Entities`(Integer)、`Place Item Entity`(Item)、`Place Item Entities`(List)、`Place Item Entities Predicate`、`Place Item Entities NBT`。
- 拾取侧: `Pick Up All Item Entities`(Boolean)、`Pick Up Item Entities`(Integer)、`Pick Up Item Entity`(Item)、`Pick Up Item Entities`(List)、`Pick Up Item Entities Predicate`、`Pick Up Item Entities NBT`。
- **实体目标传输**（`To Entities`/`From Entities`）: `Export All Items To Entities`(Boolean)、`Export Item Amount To Entities`(Integer)、`Export Item Slot To Entities`(Integer)、`Export Item To Entities`(Item)、`Export Items To Entities`(List)、`Export Items Predicate To Entities`(Operator)、`Export Items NBT To Entities`(NBT)；Import 侧镜像。这类 aspect 不带 `Blacklist`/`Check NBT` 一律齐全，带 `Entity Index`。`[事实]`
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/item.html

### 4.3 World Fluid（`world/fluid.html`）
- 放置/拾取流体源方块、与 tank 实体交互；可控制是否触发 block update。`[事实]`
- Place: `Place All Fluids`(Boolean)、`Place Fluid`(Fluid)、`Place Fluids`(List)、`Place Fluids Predicate`、`Place Fluids NBT`。
- Entity 侧: `Export All Fluid To Entities`(Boolean)、`Export Fluid Amount To Entities`(Integer)、`Export Fluid To Entities`、`Export Fluids To Entities`、`Export Fluids Predicate To Entities`、`Export Fluids NBT To Entities`；Import 镜像（`Import All Fluid From Entities` 等）。
- Pick Up: `Pick Up All Fluids`(Boolean)、`Pick Up Fluid`、`Pick Up Fluids`、`Pick Up Fluids Predicate`、`Pick Up Fluids NBT`。
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/fluid.html

### 4.4 World Energy（`world/energy.html`）
- 与带能量缓存的实体（如掉落的电池）交互。`[事实]`
- `Export All Energy To Entities`(Boolean)、`Export Energy Amount To Entities`(Long)、`Import All Energy From Entities`(Boolean)、`Import Energy Amount From Entities`(Long)。
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/world/energy.html

---

## 5. 玩家模拟（`player.html`）

- `Player Simulator` 可自动用任意手/物品点击；高级属性改点击位置等。`[事实]`
- Aspect: `Click Empty`(Boolean)、`Click`(Boolean)、`Click Amount`(Integer)、`Click Item`(Item)、`Click Items`(List)、`Click Item Predicate`(Operator)、`Click Item NBT`(NBT)。
- 属性: `Right Click`、`Right Hand`、`Continuous Click`、`Sneak`、`Right Click Duration`、`Network Inventory`、`Entity Index`、`Offset X/Y/Z`、`Check Stack Size`、`Check NBT`、`Item Transfer Rate`、`Craft`、`Blacklist`、`Empty is Wildcard`、`NBT subset/superset/Require/Check NBT Recursively`。`[事实]`
- https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/player.html

---

## 6. 教程逐条完整逻辑（含必须的非卡设置）

| 教程 | 完整逻辑组合 | 必须的非卡设置 | URL |
|---|---|---|---|
| Interfaces | 合成 Item/Fluid/Energy Interface（各 4）；挂 Cable 暴露 target | — | .../tutorials/integratedtunnels/interfaces.html |
| Importer Exporter | Interface+Output Transformer=Exporter；Interface+Input Transformer=Importer | — | .../importer_exporter.html |
| Import All Items | 空 Variable Card 放入 Item Importer → 全部导入 | 空白卡 | .../import_all_items.html |
| Export Items with Limit | Inventory Reader 的 `Inventory Count`(Integer 输出) → `Less Than`(`numberLessThan(Number,Number)`) 与常量 10 比较 → Boolean 进 `Export All Items`。**每 tick 动态求值**：库存数 <10 时继续导出 | 比较常量 10；Reader 对准目标库存 | .../export_items_limit.html |
| Import Item List | Inventory Reader 的 `Items`(List 输出) → `Import Items`(List) | Reader 提供源列表 | .../import_items_list.html |
| Export Enchantable Items | `Enchantable` 算子 `itemstackIsEnchantable(Item)→Boolean` 进 `Export Items Predicate`(Operator) | 无 | .../export_enchantable_items.html |
| Filter Item Storage if Day | `Is Day`(Aspect, **Boolean 输出**) → `Filter All Items`(Boolean) 于 Filtering Item Interface；仅白天暴露库存 | Filtering Item Interface 对准 Chest | .../filter_storage_day.html |
| Filter Item Storage by Mod | `Equals` 用 `Apply` 部分应用到字符串常量 "IntegratedDynamics" → 再用 `Pipe` 把 `Item Mod`(`itemstackMod(Item)→String`) 管入 → 得 Operator(Item→Boolean) 进 `Filter Items Predicate`；**所有依赖变量须放入 Variable Store** | Variable Store | .../filter_storage_mod.html |
| World Item Importer/Exporter | World 版 = Item Exporter/Importer + Logic Director；可与带库存掉落物（潜影盒）交互 | Logic Director | .../world_item_importer_exporter.html |
| Drop All Items | 空 Variable Card 放入 World Item Exporter → `Place All Item Entities` | 空白卡 | .../drop_all_items.html |
| Pick up Items with Limit | 常量 5 写入 Variable Card 放入 World Item Importer → `Pick Up Item Entities`(Integer) 每次拾 5 | 常量卡 5 | .../pickup_items_limit.html |
| World Block Importer/Exporter | World 版 = Item Exporter/Importer + Logic Director；可 silk touch | Logic Director | .../world_block_importer_exporter.html |
| Break Only Stone | 引用 `Stone` 的 Variable Card 放入 World Block Importer → `Pick Up Block`(Block)；可用属性 silk touch 避免只得 Cobblestone | 卡内容=Stone | .../break_stone.html |
| Place Items by Tag | `Item Tag Values`(`stringItemsByTag`? 书中标注 Global name `stringItemsByTag(String)→List`) 取 `logs` 标签物品列表 → `Place Item Blocks`(List) 于 World Block Exporter | 无 | .../place_logwood.html |
| Click with Sword | 含 Diamond Sword 的 Variable Card 放入 Player Simulator → `Click Item`(Item)，用网络中的剑自主攻击 | 卡内容=Diamond Sword | .../click_sword.html |

教程索引: https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/index.html （列出 15 项）
教程引言: https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/introduction.html

**动态编程要点 `[事实/推断]`**: filter_storage_mod 证明 **Operator 可作为变量捕获/依赖其它变量**（部分应用 + Pipe 组合，依赖项须入 Variable Store）；export_items_limit 证明 **aspect 的 Boolean 输入每 tick 从 reader 动态读取**（reader 捕获实时库存）。→ DSL 必须支持动态 predicate/多变量捕获，不能只支持静态常量。

---

## 7. 边界与错误（书内明确的）

- Item slot: **-1 任意槽；≤-2 无槽**（Export/Import Item Slot）。`[事实]`
- 空白 Variable Card = 搬运全部 / 无条件（All* aspect）。`[事实]`
- 过滤方向: Filtering Interface 的 filter aspect 有 `Apply to Insertions` / `Apply to Extractions` / `Allow if not applied`。`[事实]`
- NBT 匹配模式属性（subset/superset/Require/Recursively）——行为未定义。`[缺失]`
- 优先级（priority）用于 Interface 排序与 Importer/Exporter 排序。`[事实]`
- 书内**未定义**任何错误信息/失败返回/异常处理。`[缺失]`

---

## 8. 依赖 / 状态 / 副作用

- **网络状态**: Interface 暴露 target；Importer/Exporter 在同网络的 Interface ↔ target 间搬运（方向由部件类型固定）。priority 影响填充/排空顺序。`[事实]`
- **Aspect 是带副作用的操作**（实际移动/破坏/放置/点击）；Boolean/Integer/Amount 输入控制其每 tick 是否/多少执行。`[推断]`
- **`Craft` 依赖 Integrated Crafting**（外部 mod，非 ID 核心）。`[事实]`
- **`Craft` 在 exporter**：缺料先自动合成（concepts 明文）。`[事实]`
- **Variable Store** 是过滤器/谓词依赖变量的存储依赖（filter_storage_mod 明确要求）。`[事实]`

---

## 9. 版本疑问（供父任务源码核查）

目标 = MC1.20.1 Forge / ID master-1.20-lts @ `69ea239d03828fc76b0384dcc487af391fd9e6bf`；本书生成于 **MC1.21.1 / NeoForge ID1.38.0 + 多扩展**。全部为 `[推断]`，需逐项核对：

1. `Passive Interaction` / `Passive Interaction Ignores Filter` 是否存在于 1.20.1 各 aspect；**其真实语义必须以源码为准（本报告不给出解释）**。
2. Player Simulator 的 `Right Click` / `Right Click Duration` / `Continuous Click` / `Sneak` / `Network Inventory` / `Entity Index` / `Offset X/Y/Z` 属性在 1.20.1 是否齐全（旧版可能用 Use/Attack 语义）。
3. World 实体交互族（`*To Entities` / `*From Entities`、`Entity Index`）在 1.20.1 是否存在、命名是否一致。
4. `Ignore Replacable`（拼写存疑）、`Cause Block Update`、`Silk Touch`、`Match Block`、`Break Block When No Drops` 的 1.20.1 存在性。
5. `Check Stack Size` / `Check Fluid Amount` / `Check Energy Amount`、各 `* Transfer Rate` 默认值与语义。
6. `Empty is Wildcard`、`Slot-based`、`NBT subset/superset/Require/Craft` 相关属性在 1.20.1 的存在性与行为。
7. **Energy Amount 输入类型 Long** vs Item/Fluid Integer —— 1.20.1 是否一致。
8. 书用 **NBT** 概念；1.21.1 底层为 data components，1.20.1 为 NBT —— NBT aspect 语义是否随版本不同。
9. `Craft` 属性与 Integrated Crafting 版本耦合。
10. 各教程中的 **advancement ID / 合成配方** 在 1.20.1 是否相同（配方布局可能变）。

---

## 10. 对未来 DSL 的语义约束（只列约束，不定语法）

1. 必须区分 **gate（Boolean 输入）** 与 **selector（Item/List/Operator/NBT 输入）**——Boolean 不筛物。
2. 必须表达 **每 tick 执行** 语义 + **动态输入重求值**（reader 驱动 Boolean/Amount）。
3. 需支持 aspect 的 **属性（Properties）** 作为一等配置项，含未在书内定义的语义键（Channel/Round-Robin/Rate/Passive Interaction/Craft/Check*/NBT 模式），其语义须由源码字典提供。
4. 需支持 **filter 的方向性**（insertions/extractions）与 **未应用时默认放行**。
5. 需表达 **优先级**（Interface、Importer/Exporter）。
6. 需区分 **域**（Item/Fluid/Energy/World/Player）与 **target 形态**（inventory/tank/energy machine/entity/world position）。
7. 需支持 **Operator 构造与变量捕获**（Apply/Pipe/partial application、Variable Store 依赖），predicate 用 Operator(Item→Boolean)。
8. 需覆盖 **量级控制**：Amount 型（每 tick N）与 **外部读取+条件 gate**（限值/保留）两条路径；无原生“库存保留 X”面向量，需组合实现。
9. 需覆盖 **世界交互副作用**（破坏/放置/dispense/velocity/pickup delay/block update/silk touch）与 **玩家模拟点击**（手、右键、sneak、continuous、点击位置、网络库存取物）。
10. 需表达 **空白卡=无条件全部** 的默认语义。
11. 槽位语义: **-1 任意 / ≤-2 无槽**。
12. 需区分 **Craft（外部依赖 Integrated Crafting）** 为可选能力。

---

## 11. 遗漏 / 缺失说明（书内未提供的）

- 全部 Properties 的行为定义（见 §2）。
- Priority、Channel、Round-Robin 的精确算法与默认值。
- Transfer Rate 默认值/单位。
- `Passive Interaction`(PASSIVE_IO) 的真实语义（**书内 0 字说明；必须源码核查**）。
- 错误/失败/边界返回值。
- 过滤时 NBT 各模式的精确匹配规则。
- 世界交互的距离/范围/坐标约定（Offset 相对谁）。
- `Is Day` aspect 的归属（世界 aspect）与判定细节。
- 配方页只有形状轮廓（无具体网格图），未含研究/进度解锁条件细节。

---

## 12. Coverage（29/29 全部已读）

格式: URL | 状态 | 重要性 | 版本疑问

**手册（12）**
1. .../manual/main/integratedtunnels/index.html | read | low(目录) | 无
2. .../integratedtunnels/introduction.html | read | mid(定位) | 无
3. .../integratedtunnels/concepts.html | read | **high**(部件模型/优先级/空白卡/Craft) | 无
4. .../integratedtunnels/item.html | read | **high**(item aspect 全集/槽位语义) | Passive/Check/NBT 属性存在性
5. .../integratedtunnels/fluid.html | read | **high**(fluid aspect 全集) | 同上
6. .../integratedtunnels/energy.html | read | **high**(energy aspect/Long 输入) | 输入类型一致性
7. .../integratedtunnels/player.html | read | **high**(玩家模拟 aspect/属性) | Right Click/Continuous/Network Inventory 存在性
8. .../integratedtunnels/world/index.html | read | low(目录) | 无
9. .../integratedtunnels/world/item.html | read | **high**(世界物品/实体交互) | *To/From Entities、Entity Index
10. .../integratedtunnels/world/fluid.html | read | **high**(世界流体/实体 tank) | 同上
11. .../integratedtunnels/world/energy.html | read | mid(世界能量实体) | Long/Entity Index
12. .../integratedtunnels/world/block.html | read | **high**(破坏/放置/silk touch) | Ignore Replacable/Silk Touch

**教程（17）**
13. .../tutorials/tutorials/integratedtunnels/index.html | read | low(索引) | 无
14. .../integratedtunnels/introduction.html | read | low | 无
15. .../integratedtunnels/interfaces.html | read | mid(Interface 合成/定位) | 配方
16. .../integratedtunnels/importer_exporter.html | read | mid(合成关系) | 配方
17. .../integratedtunnels/import_all_items.html | read | mid(空白卡语义) | 无
18. .../integratedtunnels/export_items_limit.html | read | **high**(Reader+LessThan 动态 gate) | Reader/operator 存在性
19. .../integratedtunnels/import_items_list.html | read | **high**(List 源) | 无
20. .../integratedtunnels/export_enchantable_items.html | read | **high**(Enchantable predicate) | 算子名
21. .../integratedtunnels/filter_storage_day.html | read | **high**(动态昼夜过滤) | Is Day aspect
22. .../integratedtunnels/filter_storage_mod.html | read | **high**(Apply/Pipe/Variable Store 动态 Operator) | 算子与 Variable Store
23. .../integratedtunnels/world_item_importer_exporter.html | read | mid(World 合成) | 配方
24. .../integratedtunnels/drop_all_items.html | read | mid(空白卡=全丢) | 无
25. .../integratedtunnels/pickup_items_limit.html | read | **high**(常量 Amount 拾取) | 无
26. .../integratedtunnels/world_block_importer_exporter.html | read | mid(World block 合成) | 配方
27. .../integratedtunnels/break_stone.html | read | mid(Block 指定破坏/silk touch) | Block vs Item 值
28. .../integratedtunnels/place_logwood.html | read | **high**(Tag Values 列表→放置) | 算子名 `stringItemsByTag`
29. .../integratedtunnels/click_sword.html | read | mid(Player Simulator Item 点击) | 属性集

**统计**: 29/29 read，0 失败；high 16、mid 9、low 4。

---

## 需要父任务确认的版本项（汇总）

① PASSIVE_IO(`Passive Interaction`) 真实语义与 1.20.1 是否存在；② Player Simulator 属性集；③ World 实体交互族；④ block 破坏/放置属性；⑤ Check*/Transfer Rate 语义与默认值；⑥ Empty is Wildcard/Slot-based/NBT 模式属性；⑦ Energy Long vs Item/Fluid Integer；⑧ 1.20.1 用 NBT、1.21.1 用 data components 对 NBT aspect 的影响；⑨ Craft 与 Integrated Crafting 版本耦合；⑩ 教程配方/advancement 在 1.20.1 的一致性。