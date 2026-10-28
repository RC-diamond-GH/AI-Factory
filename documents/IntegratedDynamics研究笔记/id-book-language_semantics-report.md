All 34 assigned pages have been read in full (via `sources.md` full-text and targeted JSON lookups). Below is the complete deliverable — no files written (read-only scout per parent correction).

---

# ID Book · group `language_semantics` — 完整阅读报告

**阅读范围**：manifest group `language_semantics` 全部 34 URL；全部读完（100%）。书生成环境：MC1.21.1/NeoForge ID 1.38.0 + 多扩展（见 manifest `generated_mods_note`）。本报告只描述书中正文；凡推断均标 `[INFERENCE]`。

父任务校审：本报告的教程行为描述属于在线 1.21.1 书；目标 1.20.1 的确认与差异见 [总报告](local://id-book-research.md)。Recipe/Ingredients 是 ID 核心类型，不依赖 Integrated Crafting；实际自动合成执行依赖该扩展。

---

## 1. 架构总览（机制如何拼接）

书的 "Logic" 章节把 ID 的逻辑系统拆成三层（[manual/logic/index.html](https://integrateddynamics.rubensworks.net/book/manual/logic/index.html)）：

1. **Value Types**（值类型）— 数据域。
2. **Variables**（Variable Card / Variable Store）— 传输与引用载体。
3. **Advanced**（目前仅 NBT Path）— 复杂数据导航。

核心运行模型（跨页综合）：

- **Variable Card = (Value Type, reference to Value)**，是 "logic transport" 的单位（[variable_card.html](https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_card.html)）。
- 变量分 **static**（定义一次，永不改变）与 **dynamic**（由 Operators 或 Reader Aspects 的 "Logic Evaluation" 产生，值随时间变化，如世界时间）。
- **求值是 lazy / pull-based**：动态变量只在"其值被请求"时求值（Display Panel 显示，或被另一动态变量求值引用）。
- **引用必须可解析于同一 network 内**：教程使用 Variable Store 存放被引用卡片，依赖具有传递性。1.20 源码还会索引 active-variable 部件（包括 writer）的卡槽，不能把 Variable Store 当成唯一合法来源：[PartNetwork](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/network/PartNetwork.java#L119-L155)。
- **普通即时表达式不记忆历史**：Delayer 提供显式历史值与跨 tick 反馈。这不等于整个网络没有可变状态；Proxy、库存、配置及扩展合成任务也有各自的状态。
- 每个卡片有 **唯一 ID**，网络报错时展示 ID 辅助 debug；Labeler 可给标签代替 ID。
- **每个 operator 应用各自产生一张独立 Variable Card**（[saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)）；卡片可反复取出到 Logic Programmer 重新编辑（[logicOperations.html](https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html)）。

---

## 2. 值类型逐条语义

枚举共 **14 种**（[value_types/index.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/index.html)）：Boolean, Integer, Double, Long, String, List, Operator, NBT, Block, Item, Entity, Fluid, Ingredients, Recipe。

| 类型 | 语义（书中原文要点） | 边界/范围 | URL |
|---|---|---|---|
| Boolean | 最简类型，True/False | — | [boolean.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/boolean.html) |
| Integer | 正负整数 | −2147483648 … 2147483647；属 **Number** category | [integer.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/integer.html) |
| Double | 实数（可正负） | 属 **Number** category | [double.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/double.html) |
| Long | 同 Integers 更大范围 | −9223372036854775808 … 9223372036854775807；属 **Number** category | [long.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/long.html) |
| String | 字符序列（含 Unicode：`x!(àà)ç`） | — | [string.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/string.html) |
| List | 某一种 Value Type 的列表；**元素必须同类型** | 例：(0,1,2,3)、("a","b","c")、(3.33,1.14,5,6) | [list.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/list.html) |
| Operator | 指向一个 Operator 的值；支持 **部分应用/Currying**（metaprogramming：operator 作为数据被其他 operator 处理） | 可用一个 Integer 部分应用 → 新的虚拟 operator（1 输入 1 输出）；再应用一个 Integer → Integer；高级操作含 reduce list、按 Predicate 过滤 | [operator.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/operator.html) |
| NBT | 可持有多种类型值的 tag 对象；变量持有其 **引用**；有读取/扩展 operator；作为**低层 fallback** | 例：箱子含物品的 NBT、镐的附魔信息 | [nbt.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/nbt.html) |
| Block | 对 Minecraft block 的引用 | Dirt、Cobblestone、Red Stained Clay、Chest | [block.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/block.html) |
| Item | 对 Minecraft item 的引用（可含 NBT，如 Enchanted Sword） | Apple、Cobblestone、Diamond Pickaxe | [item.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/item.html) |
| Entity | 对某 Minecraft entity 的引用 | Zombie、Cow、Player_abc | [entity.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/entity.html) |
| Fluid | 对某 Minecraft fluid 的引用（含量） | 写法 `Lava@1000mB`、`Water@3mB`、`Menril Resin@3030mB` | [fluid.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/fluid.html) |
| Ingredients | Item、Fluid、Energy 量（Integer）的集合；主要表示 Recipe 的输入/输出 | — | [ingredients.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/ingredients.html) |
| Recipe | 抽象合成配方：**Ingredients → Ingredients**；Logic Programmer 可视化创建，或 Machine Reader 从（支持的）机器读取；供自动合成（Integrated Crafting 等）使用 | shift 悬停显示产物；shift-click 输入槽可配 **是否考虑 NBT / 可否跨配方复用（不被完全消耗）/ 是否考虑 tag 替代** | [recipe.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/recipe.html) |

**关于"nullable / 空值"语义**：书中这 34 页**没有**任何页显式讨论 null/缺失值语义，也没有 Boolean/数值的 null 概念。最接近的"安全取缺失"机制：
- **Get Or Default** `listGetOrDefault(List,Integer,Any)→Any`："安全取列表指定位置，元素不可用则返回给定默认值"（[saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)）。
- Delayer 空历史需用 Get Or Default 兜底（同上）→ 暗示直接越界取列表元素会失败（[INFERENCE]，书中未给错误文本）。
- NBT Path "Match First" 只取首个匹配（可能无匹配，未说明无匹配行为）（[nbt_path.html](https://integrateddynamics.rubensworks.net/book/manual/logic/advanced/nbt_path.html)）。

---

## 3. 类型类别与高阶操作

- **Number category**：显式由 Integer、Double、Long 组成（三页均注明 "part of the Number category"）。`numberAdd(Number,Number)→Number` 表明这些类型统一归入 Number 参与算术（[logicOperations.html](https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html)）。
- **Any**：作为 operator 签名的占位/泛型（`anyEquals(Any,Any)→Boolean`、`operatorApply(Operator,Any)→Any`、`booleanChoice(Boolean,Any,Any)→Any`）。它不是一个可枚举的值类型页（[saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)、[theValueOfOperators.html](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)）。
- **Predicate**：不是独立值类型，定义为"一个输入、Boolean 输出的 Operator"，供 Filter 使用（[theValueOfOperators.html](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)）。
- **类型约束（错误边界）**：`anyEquals` "两值类型必须相同"；`booleanChoice` "第二、三类型必须相等"（[saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)）。
- **高阶操作**：Operator 作为一等值——Apply（逐参绑定/currying）、Filter（List × Predicate）、reduce、翻转参数、组合 Predicate；"operators are treated as data"（[operator.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/operator.html)、[theValueOfOperators.html](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)）。

---

## 4. 求值 / 引用 / 即时依赖 与 时间反馈

- **Lazy 即时求值**：Addition 卡片不直接保存"和"，只在网络需要时基于变量引用计算（[logicOperations.html](https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html)）。动态变量仅在"被请求"时评估（Display Panel 或作为另一动态变量求值的一部分）（[variable_card.html](https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_card.html)）。
- **依赖解析**：被引用卡片须在同一 network 的 Variable Store 中；多 Store 可共存；依赖传递同样要求（[variable_store.html](https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_store.html)）。缺少 Store → 网络找不到引用（教程反复强调）——这是最常见的失败条件。
- **无状态性**：变量恒按当前输入重算，网络不记忆历史（[saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)）。
- **时间反馈（Delayer）**（[delay.html](https://integrateddynamics.rubensworks.net/book/manual/machines/delay.html) + [saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)）：
  - 维护 top slot 卡片值的历史，以 **List** 暴露；**最新值在 List 末尾**；GUI 可设最大历史长度（saving_state 中称 **Capacity**、**Ticks/Operation**）。
  - Referenced card（或任意卡片）必须留在 top slot，Delayer 才工作。
  - **反馈环**：放进 Delayer 的卡片其值依赖 Delayer 自身历史；每 tick Delayer 求值该卡并保存结果，成为下一 tick 的上一值。
  - **依赖必须间接**：把 history 卡片本身放回 top slot → 报错"variable is referring to itself"。
  - **边沿检测**：Capacity 2 的历史在按钮按下后的 tick 恰为 `[False, True]`；与静态 List `[False, True]` 用 `Equals` 比较得 Edge。
  - 反馈环可保存任意值类型（计数器、计时器）。

---

## 5. 物化 / 编码（动态↔静态）

- **Materializer**（[materializer.html](https://integrateddynamics.rubensworks.net/book/manual/machines/materializer.html)）：把动态卡变静态——插入动态卡 + 新空卡，空卡得到该动态卡**当前值**的静态值。用途：记录（log）值、不再需要动态求值。卡片可管道进出。**两个 encode 按钮**把 materialized 值以 encoded string 放剪贴板（分享/跨世界）：第一个可读编码，第二个更短易传。
- **Proxy**（[proxy.html](https://integrateddynamics.rubensworks.net/book/manual/machines/proxy.html)）：对某卡的**动态引用**——top slot 插卡，另写一张新卡，动态取 proxy 中卡的值；被引用卡（或任意卡）须留在 top slot。用途：链式卡片中手动替换某一值。卡片可管道进出。
- **Delayer** 见 §4，是第三种"机器级"引用/状态机制。

Materializer 在生成静态卡的时刻需要源卡；生成后的静态值不再依赖源卡驻留。Proxy 与 Delayer 则需要其输入槽中的卡来提供后续动态引用或采样。

---

## 6. NBT Path 语义（[nbt_path.html](https://integrateddynamics.rubensworks.net/book/manual/logic/advanced/nbt_path.html)）

- **目的**：用 String 表达式导航深层 NBT，替代复杂 operator 链。
- **field selector**：`.fieldName` 或 `["fieldName"]`。例：`.root.child1.child2` on `{root:{child1:{child2:"some value"}}}` → `"some value"`。
- **多字段**：`[fieldA,fieldB]` 或通配 `*`。例：`.a[b1,b2]*` on `{a:{b1:{c:"some value"}}}` → `"some value"`。
- **列表索引**：`[1]`。例：`.a[1]` on `{a:[10,20]}` → `"20"`（书中写作字符串 20）。
- **切片**：`[start:end:step]`，仅 start 必填。例：`.a[1:4:2]` on `{a:[0,1,2,3,4,5]}` → `[1,3]`。
- **过滤**：`[?(expr)]`；`@`=当前 tag，`..`=父 tag，`$`=根 tag。例：`$.a[?(@==3)]` → `[3]`。
- **布尔组合**：网页将 and 显示成 `§§`；1.20 英文语言资源明确记录实际表达式运算符为 `&&`、`||`、`!`。这是 CyclopsCore NBT Path，不应与 Minecraft 原版命令的 NBT Path 语法混为一谈：[语言资源](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/resources/assets/integrateddynamics/lang/en_us.json)。
- **三个 operator**：`stringNbtPathMatchFirst(String,NBT)→NBT`（首匹配）、`stringNbtPathMatchAll(String,NBT)→List`（全部匹配为 List）、`stringNbtPathTest(String,NBT)→Boolean`（是否匹配）。

---

## 7. 逐教程归纳（输入 / 操作 / 输出 / 失败条件）

> 每条给出：输入 → 操作 → 输出 → 失败/约束。

**tutorials/index.html / introduction.html**（[index](https://integrateddynamics.rubensworks.net/book/tutorials/index.html)、[introduction](https://integrateddynamics.rubensworks.net/book/tutorials/introduction.html)）
- 12 个主教程：Introduction, Menril Basics, Network Foundations, Basic Network Components, Aspects, Logic Operations, Advanced Network Components, Advanced Operations, The Value of Operators, Saving State, Recipe Handling, NBT, Challenges；另有扩展教程分区（Tunnel/Crafting/Terminal/Scripting/NBT/Mekanism）。教程含 rewards/achievements。
- 失败条件：无。

**menrilBasics.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/menrilBasics.html)）— 发展进度（压缩）
- 输入：Menril Log。操作：Squeezer（跳踩压平；红石脉冲可重置）→ Menril Resin 1000mB；Drying Basin 邻接 → 7.5s → Block of Crystallized Menril。后期可造机械版（Mechanical Squeezer / Drying Basin）。
- 用于产出本 mod 的基础材料，是 Logic Programmer 等的前置。

**logicOperations.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html)）
- 输入：空 Variable Card ×2，两个 Integer。操作：Logic Programmer（block/item + Portable 可互转）中选 Integer operator，点击、输入数字，插空卡绑定 → 两张 Integer 卡；再选 Addition `numberAdd(Number,Number)→Number`，插入两 Integer 卡，绑到新卡。
- 输出：Addition 卡 → cable + Display Panel 显示和。
- 失败条件/约束：lazy——卡不含和；引用须放 **Variable Store**，否则网络找不到。卡片可重新插回 Programmer 修改。

**advancedOperations.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/advancedOperations.html)）
- (a) 输入 Integer 1,10,100 → 建静态 List → Display Panel。
- (b) 输入静态 Item 卡 → `itemstackMod(Item)→String` 动态取 mod 名 → Display Panel。**须用 Variable Store** 存被引用变量。
- (c) Entity Reader 挂网络 → Aspect "Entity"（output Entity，property List Element）→ `entityTargetBlock(Entity)→Block` 取注视方块 → Display Panel。

**theValueOfOperators.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)）
- 输入：静态 Operator 卡（Arithmetic Addition）+ 两个 Integer。操作：`operatorApply(Operator,Any)→Any` 逐次绑定参数（第一次→剩一个输入的 Operator，第二次→结果）→ Display Panel。
- Filter：List 1,10,100 + 存 `anyEquals` 卡 → apply 到 10 得 Predicate → `operatorFilter(Operator,List)→List` → 只留 10 → 显示。
- 失败条件：Predicate 必须 1 输入 Boolean 输出；类型须匹配。

**saving_state.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)）
- 输入：Redstone Reader（targeting Button）Boolean "Redstone" 卡。操作：Delayer Capacity=1，Ticks/Operation=1；写 history 卡。
- **latch**：On/Off 两按钮 → `Previous=listGetOrDefault(history,0,False)`；`NotOff=booleanNot(Off)`；`Keep=booleanAnd(Previous,NotOff)`；`State=booleanOr(On,Keep)`；State 放回 Delayer top slot；第二份 State 写入 Redstone Writer→Redstone Lamp。
- **toggle**：第二个 Delayer Capacity=2，历史 `[False,True]`；Edge = history `anyEquals` 静态 List [False,True]；`NotPrevious=booleanNot(Previous)`；`State=booleanChoice(Edge,NotPrevious,Previous)`。
- 输出：灯 latch/toggle。
- 失败条件：反馈依赖必须**间接**（直接把自身 history 卡放 top slot → "referring to itself" 错误）；所有中间卡放 Variable Store，否则网络找不到；Delayer 空历史需 Get Or Default 兜底。

**recipe_handling.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/recipe_handling.html)）
- (a) Machine Reader 对着 Crafting Table → Aspect "Recipes"（output List）读全部配方。
- (b) 在 Logic Programmer 可视创建 Recipe（如 Chest）。
- (c) Aspect "Recipe By Output"（output Operator）：对 Ingredients 应用返回**首个**以该 Ingredients 为输出的 Recipe。建单个 Chest 的 Ingredients → 读 aspect → `operatorApply` → Display Panel。
- 失败条件：所用变量须入 Variable Store；机器须"支持" recipe（否则 Machine Reader 不暴露）。

**nbt.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/nbt.html)）
- 输入：Block Reader 对 lectern。操作：读 Aspect "Tile Entity NBT"（output NBT）→ Display；`nbtGetCompound(NBT,String)→NBT` 读 Book 子 tag；`stringNbtPathMatchFirst` 用 NBT Path（如 `Book/id`）从根读 id。
- 失败条件：字段不存在/路径不匹配（行为未文档化）。

**challenges.html**（[link](https://integrateddynamics.rubensworks.net/book/tutorials/challenges.html)）
- Creeper Taming：Creeper HP → Display。
- Sponge Step Sound：播放 Wet Sponge 的 step sound。
- **Recursive Recursion：创建一个 infinite List 并显示**（暗示 List 可为惰性/自引用无限结构）。`[INFERENCE]` 具体机制书中未给。
- Tools for Obsidian?：判断 Stone Pickaxe 能否破坏 Obsidian → 显示。
- Smart Pressure Plate：Entity Reader 按实体名匹配，Redstone Writer 直连门开门；提示"把实体名作为第一个参数传给 equals operator"（说明 `anyEquals` 可接受 entity 名 String 作首参）。

---

## 8. 对未来 DSL 的约束（只列语义约束，不设计语法）

1. **类型系统**：ID 核心提供 14 个具体值类型；List 同质；Number 是 Integer/Double/Long 的类别，Any 也是类型类别，不等同于 Haskell 类型变量或某个实际存储类型。Predicate 指单输入、Boolean 输出的 Operator。
2. **引用与快照**：变量卡保存的读取引用、算子依赖、Proxy 引用与 Materializer 静态快照必须区分。不能仅凭 Item/Block/Entity 等类型名，把它们一概理解为持续跟随世界变化的活引用。
3. **惰性/按需求值**：表达式默认不立即求值；需在 DSL 中表达"lazy variable / 引用网络节点"。
4. **即时表达式与时间状态**：普通组合依赖当前输入；持久历史可由 Delayer 引入。即时循环依赖与经过历史边界的反馈不同；不能由反馈教程推出支持任意递归。
5. **变量作用域**：被引用节点必须在同一 network 的变量索引中可见；Variable Store 是教程采用的部署方式。
6. **一等 Operator / currying**：Operator 可存、部分应用、作为参数（Filter/reduce/Apply）。DSL 需支持高阶函数与部分应用。
7. **节点身份**：教程中的普通 operator 应用会产生独立卡片 ID；编译器内部子表达式与实体卡并非必须一一对应，高阶 Operator 值也有自身序列化结构。
8. **错误模型**：类型不匹配（Equals 要求同类型；Choice 两分支同类型）、越界取列表、Delayer 自引用、引用不可解析、无匹配路径——均需可诊断（ID/label）。
9. **时间/tick 语义**：Delayer 以 tick 为粒度，历史=List（最新在末），Capacity/Ticks-Operation 参数化。
10. **NBT 作为低层逃生舱**：String 路径表达式是一等公民（MatchFirst/All/Test）。
11. **物化边界**：Materializer 生成 static 快照后不再依赖源卡；Proxy 是动态间接引用，Delayer 是历史采样引用。

---

## 9. 需要 1.20.1 (target SHA 69ea239d) 核查的机制清单

以下几项在 1.21.1 书中出现，需父任务在 master-1.20-lts 源码核查是否成立（含命名与行为）：

| # | 机制 | 书中来源 | 核查点 |
|---|---|---|---|
| V1 | Delayer 的 **Capacity / Ticks/Operation** GUI 与反馈环语义；self-reference 报错文本 | [delay.html](https://integrateddynamics.rubensworks.net/book/manual/machines/delay.html), [saving_state.html](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html) | 1.20.1 是否有同名参数、latch/toggle 教程行为一致；saving_state 教程是否为本版新内容 |
| V2 | Materializer 的**双 encode 按钮**（readable/short）与 clipboard 编码 | [materializer.html](https://integrateddynamics.rubensworks.net/book/manual/machines/materializer.html) | 1.20.1 是否存在两按钮及两种编码 |
| V3 | Proxy 动态引用语义 | [proxy.html](https://integrateddynamics.rubensworks.net/book/manual/machines/proxy.html) | 行为/槽位约束一致 |
| V4 | NBT Path operator（MatchFirst/All/Test）与语法（field/index/slice/filter、`@`/`..`/`$`、`&&`(=书中§§)/`||`/`!`） | [nbt_path.html](https://integrateddynamics.rubensworks.net/book/manual/logic/advanced/nbt_path.html) | 1.20.1 支持程度；`§§` 实际 token |
| V5 | Recipe **shift-click 输入槽属性**：是否考虑 NBT / 可复用 / **tag 替代** | [recipe.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/recipe.html) | tag 替代项是否为较新特性 |
| V6 | Ingredients 支持 Item、**Fluid、Energy(Integer)** 三类 | [ingredients.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/ingredients.html) | 1.20.1 是否含 fluid/energy |
| V7 | 值类型全集（尤其 **Fluid / Ingredients / Recipe**）与 Number category 划分（Integer/Double/Long） | [value_types/index.html](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/index.html) | 1.20.1 值类型列表是否一致 |
| V8 | Operator 签名：`itemstackMod`、`entityTargetBlock`、`nbtGetCompound`、`listGetOrDefault`、`booleanChoice`、`operatorApply`、`operatorFilter`、`anyEquals`、`numberAdd` | 各教程 | 名称/签名/约束（同类型、Choice 分支同类型）一致 |
| V9 | Aspects：Entity Reader "Entity"(List Element)、"Target Block"；Machine Reader "Recipes"(List)、"Recipe By Output"(Operator)；Block Reader "Tile Entity NBT" | [advancedOperations](https://integrateddynamics.rubensworks.net/book/tutorials/advancedOperations.html), [recipe_handling](https://integrateddynamics.rubensworks.net/book/tutorials/recipe_handling.html), [nbt](https://integrateddynamics.rubensworks.net/book/tutorials/nbt.html) | aspect 名称与输出类型 |
| V10 | **infinite/递归 List** 是否可行（Recursive Recursion challenge） | [challenges.html](https://integrateddynamics.rubensworks.net/book/tutorials/challenges.html) | 机制未在书中说明，需源码确认 |
| V11 | lazy 求值 + "每子表达式各自一张卡"+ Variable Store 强制引用解析 | [logicOperations](https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html), [variable_store](https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_store.html) | 核心机制，确认跨版本不变 |
| V12 | Portable ↔ Logic Programmer 互转 | [logicOperations.html](https://integrateddynamics.rubensworks.net/book/tutorials/logicOperations.html) | 1.20.1 是否一致 |

---

## 10. 事实/推断与引用遗漏

- **FACT**：§2所有值类型范围/定义、§4 lazy/无状态/Delayer 语义、§5 Materializer/Proxy、§6 NBT Path 语法与 operator 签名、§7 教程步骤——均直接来自对应页面正文。
- **INFERENCE**：Integer/Long 溢出行为未文档化；NBT path "no match" 行为；List 越界失败文本；infinite List 机制；Number 类别内混合运算（如 Integer+Double）；`§§`= `&&`；"引用可变"性质。
- **引用遗漏/缺口（书中未覆盖，须外部补）**：
  1. 无 `Number`/`Any`/`Predicate` 独立类型页（类别仅以文字提及）。
  2. **无任何 null/缺失值语义说明**（这是本任务"nullable"关注点的直接缺口）。
  3. 无错误/异常类型清单（仅有 Delayer 自引用一处错误文本）。
  4. 无 List 的 lazy/infinite 语义正式说明（仅 challenge 暗示）。
  5. Operator 完整清单与签名不属本组（在 `operators` 组 `logic_programming.html`）；本组只出现被教程引用的算子。
  6. 未说明 network/tick 调度的精确时机（仅"每 tick 求值"）。
- **版本环境警示**：书为 MC1.21.1/NeoForge ID1.38.0，含多个扩展。1.20 核心 ID 已注册 Recipe/Ingredients 类型和 Machine Reader 的配方读取 aspects；能否读取某台机器取决于该目标提供的 recipe-handler 能力。实际自动合成接口、writer 与 job 系统属于 Integrated Crafting：[ValueTypes](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypes.java)、[Machine Reader](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/PartTypeMachineReader.java)。

---

## Coverage JSON（34/34，全部已读）

```json
{
  "group": "language_semantics",
  "assigned": 34, "read": 34, "failed": 0,
  "generated_env": "MC1.21.1/NeoForge ID1.38.0 (+crafting/tunnels/terminals/scripting/nbt/rest/mekanism)",
  "target": "integrateddynamics master-1.20-lts SHA69ea239d03828fc76b0384dcc487af391fd9e6bf",
  "pages": [
    {"url":".../manual/logic/index.html","read":true,"importance":"medium","version":"核心导航，稳定"},
    {"url":".../manual/logic/advanced/index.html","read":true,"importance":"low","version":"仅索引"},
    {"url":".../manual/logic/advanced/nbt_path.html","read":true,"importance":"high","version":"需核查 V4"},
    {"url":".../manual/logic/value_types/index.html","read":true,"importance":"high","version":"需核查 V7"},
    {"url":".../manual/logic/value_types/boolean.html","read":true,"importance":"medium","version":"稳定"},
    {"url":".../manual/logic/value_types/integer.html","read":true,"importance":"medium","version":"稳定；Number category"},
    {"url":".../manual/logic/value_types/double.html","read":true,"importance":"medium","version":"稳定；Number category"},
    {"url":".../manual/logic/value_types/long.html","read":true,"importance":"medium","version":"稳定；Number category"},
    {"url":".../manual/logic/value_types/string.html","read":true,"importance":"medium","version":"稳定"},
    {"url":".../manual/logic/value_types/list.html","read":true,"importance":"high","version":"同质列表语义，确认"},
    {"url":".../manual/logic/value_types/operator.html","read":true,"importance":"high","version":"currying/高阶，确认 V8"},
    {"url":".../manual/logic/value_types/nbt.html","read":true,"importance":"high","version":"引用语义，确认"},
    {"url":".../manual/logic/value_types/block.html","read":true,"importance":"medium","version":"确认"},
    {"url":".../manual/logic/value_types/item.html","read":true,"importance":"medium","version":"确认"},
    {"url":".../manual/logic/value_types/entity.html","read":true,"importance":"medium","version":"确认"},
    {"url":".../manual/logic/value_types/fluid.html","read":true,"importance":"medium","version":"核查 V6/V7"},
    {"url":".../manual/logic/value_types/ingredients.html","read":true,"importance":"high","version":"核查 V6"},
    {"url":".../manual/logic/value_types/recipe.html","read":true,"importance":"high","version":"核查 V5（Integrated Crafting）"},
    {"url":".../manual/logic/variables/index.html","read":true,"importance":"low","version":"索引"},
    {"url":".../manual/logic/variables/variable_card.html","read":true,"importance":"high","version":"lazy/static-dynamic，核查 V11"},
    {"url":".../manual/logic/variables/variable_store.html","read":true,"importance":"high","version":"引用解析域，核查 V11"},
    {"url":".../manual/machines/delay.html","read":true,"importance":"high","version":"核查 V1"},
    {"url":".../manual/machines/materializer.html","read":true,"importance":"high","version":"核查 V2"},
    {"url":".../manual/machines/proxy.html","read":true,"importance":"high","version":"核查 V3"},
    {"url":".../tutorials/index.html","read":true,"importance":"low","version":"索引"},
    {"url":".../tutorials/introduction.html","read":true,"importance":"low","version":"稳定"},
    {"url":".../tutorials/menrilBasics.html","read":true,"importance":"low","version":"进度/配方，可压缩"},
    {"url":".../tutorials/logicOperations.html","read":true,"importance":"high","version":"核查 V12/V11"},
    {"url":".../tutorials/advancedOperations.html","read":true,"importance":"high","version":"核查 V9（aspects）"},
    {"url":".../tutorials/theValueOfOperators.html","read":true,"importance":"high","version":"核查 V8（Apply/Filter）"},
    {"url":".../tutorials/saving_state.html","read":true,"importance":"high","version":"最可能为新内容，核查 V1"},
    {"url":".../tutorials/recipe_handling.html","read":true,"importance":"high","version":"核查 V5/V9（Integrated Crafting）"},
    {"url":".../tutorials/nbt.html","read":true,"importance":"medium","version":"核查 V9/V4"},
    {"url":".../tutorials/challenges.html","read":true,"importance":"medium","version":"含 infinite List，核查 V10"}
  ]
}
```

**覆盖数量**：34/34 已读，0 失败。**需父任务确认的版本项**：V1–V12（见 §9），其中 V1（Delayer Capacity/Ticks/Operation + saving_state 教程）、V2（Materializer 双编码）、V4（NBT Path 语法/`§§`）、V5（Recipe 输入槽 tag 替代）为新书最可能在 1.20.1 缺失/差异者。