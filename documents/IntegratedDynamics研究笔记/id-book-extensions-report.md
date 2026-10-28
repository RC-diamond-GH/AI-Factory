# Group `extensions` 完整研究报告

父任务校审：完整可点击引用和计数补正在 [引用补正](local://id-book-extensions-corrections.md)，每个页面的规范 URL 见 [coverage](local://id-book-extensions-coverage.json)。目标 1.20.1 的扩展存在性、NBT 四模式与 Chemical 类型已核查，其他细节不应仅凭在线书推广到旧版本；详见 [总报告](local://id-book-research.md)。

## 0. 范围与素材
- 分配：49 页面（Integrated Scripting 23，Integrated NBT 9，Integrated REST 3，Integrated Mekanism 14），目录页包括在内；每个页面的规范 URL 见本组 coverage。
- 已完整读取：49/49（JSON `local://id-book-extensions.json` 逐页 `text` 全文 + `links`；`-sources.md` 目录）。无抓取失败、无缺失正文。
- 在线书环境：MC 1.21.1 / NeoForge / ID 1.38.0 + 扩展。**目标项目 ID `master-1.20-lts` @`69ea239d`** —— 所有“在线新增特性”在 §5 单列，标注需父任务跨版本源码核查。

---

## 1. Integrated Scripting（23 页）

### 1.1 三个核心部件
来源：concepts.html / disk.html / drive.html / terminal.html / materials.html
- **Scripting Terminal + Scripting Disk + Scripting Drive** 三者构成“编辑→存储→接入网络”链路（concepts.html）。
- **Scripting Disk**：只存原始脚本文本，本身不起作用；用 Variable Card + Mendesite 合成；插入 Drive 后获得**唯一数字 id**，该 id 用于 Terminal 内修改脚本、并把脚本成员绑定到 Variable Card；Disk 可跨 Drive/跨网络移动而不丢脚本数据，但**引用该 id 的 Variable Card 在换网络后会开始报错**；Disk **无存储容量上限**，建议按管理需要分盘（disk.html）。
- **Scripting Drive**：方块，接入 ID 网络，**只接受一张 Disk**，负责把 Disk 内容暴露给网络；合成 = Variable Store + Jukebox + 3×Scripting Disk（drive.html）。
- **Scripting Terminal**：必须接入网络的 part；右键打开简化 IDE；左上选 Disk id，下方列该盘全部文件；"+" 新建文件；右栏为文本编辑区，**每次修改后自动保存**；提供**有限语法高亮，无法检测语法错误——错误在通过 Variable Card 执行脚本时才报告**；把脚本成员用作逻辑的方式：在编辑器中选中成员名（变量名/函数名），再把空卡插入右侧槽完成绑定（terminal.html）。合成材料：Redstone Dust / Mendesite / Redstone Dust / Output Variable Transformer / Display Panel / Input Variable Transformer / Redstone Dust×2（terminal.html）。
- **Materials**：Mendesite = Menril Resin + Andesite 于 Drying Basin（15 s）或 Mechanical Drying Basin（0.75 s）（materials.html；配方页 squeeze/drying_basin 属基础章节）。

### 1.2 脚本语言与值语义
- **语言**：脚本用 JavaScript 语法，**ECMAScript (ECMA-262) 的全部特性可用**（writing/js.html）。
- **值类型互通**：所有 ID 值类型（Integers、Booleans、Items…）在 JS 中可用，反之亦然；`let` 可重赋值、`const` 不可；把成员绑定到卡后即可当普通变量使用（如长 String 或用循环生成值）；列表如 `[1,1,2,3,5]`（writing/variables.html）。
- **函数即 Operator**：Operators 是一等公民，任何 JS 函数定义方式（`function` 关键字 / lambda）都能成为 Operator；函数参数在执行时接收 ID 值并**翻译为 JS 值**；绑定方式同常量/变量（选中函数名 + 插卡）（writing/functions.html）。示例：`isEven`、`isOdd`、`fibonacci`（示例代码含 `for (let i=1; i<=number; i++)`，与形参 `limit` 不一致，属文档示例笔误 [推断]）。
- **全局函数**：全局 `idContext` 变量，通过其 `ops` 字段把**所有 ID 内建 operator 暴露为函数**，如 `idContext.ops.itemstackStackable(item)`、`idContext.ops.itemstackStacksize(item)`；operator 的“全局名”可在 Logic Programmer 的 tooltip 或本书逻辑编程章节查到；可把 operator 存到常量变量以简化（writing/globals.html）。
- **对象方法**：Blocks/Items/Fluids 等对象值类型自带**方法**（本质是首参绑定到该对象的普通函数）。全局函数 `itemstackStackable(Item)` → 方法 `item.stackable()`（0 参）；二元及以上全局函数方法化时**参数整体左移一位**，如 `itemstackStrength(Item,Block)` → `item.strength(block)`；另有 `item.size()`、`item.nbt().Damage` 等（writing/methods.html）。
- 小结：**全局函数名 `itemstackXxx` ↔ 方法名 `xxx` 是同一 operator 的两种调用面**；方法论与 §4 Chemical Operators 的 “Global name / Method name” 成对列出完全一致。

### 1.3 Advanced
- **Transient Storage**（transient_storage.html）：ID 基于函数式原则，状态本不存在（只能靠 Delayer 等外部手段）。JS 允许在函数外声明可变变量（如 `let`）并在函数调用中修改，从而**部分实现状态**（计数器、滑动平均）。**关键边界：状态是易失的——任何时候重新插入网络中的变量卡、或服务器重启都会重置**；需要持久状态请用 Delayer。
- **External Editing**（external_editing.html）：脚本位于 `world/integratedscripting/<diskId>/`（单机为 `saves/`）；**游戏运行时也可安全用外部编辑器修改**，保存后游戏内自动更新且依赖变量一并更新；Terminal 内的修改也会反向同步到外部编辑器（支持该功能的现代 IDE）。
- **Logging**（logging.html）：`console.log()` → per-script `.stdout`、`console.error()` → `.stderr`（如 `myfile.js.stdout`）；可用 Terminal 或外部编辑器访问；**默认上限 2096 行**（原文如此），超出需咨询服主。
- **TypeScript**（typescript.html）：**只支持纯 JS**；提供 `world/integratedscripting/integratedscripting.d.ts` 类型声明（含全部全局函数、值类型及其方法），仅外部 IDE 用于类型安全；**该文件每次世界重启自动重新生成，不应手改**。

### 1.4 教程
- **basics**（tutorials/basics.html）：Drying Basin 产 Mendesite → 合成 Scripting Disk(×8) → Scripting Drive(×2)。
- **terminal**（tutorials/terminal.html）：合成/放置 Terminal 于连接 Drive 的 cable，右键打开；"+" 新建文件；写 `const myVar = 123;`；双击选中后插空卡绑定；在 Display Panel 显示。
- **functions**（tutorials/functions.html）：创建 `Item → Boolean` 函数作为 filter Operator；用 Inventory Reader 读物品列表并 `Filter`；示例 `item.isStackable() && item.size() >= 16`；`Filter` operator 全局名 `operatorFilter(Operator, List)`、方法名 `Operator.filter(List)`。

### 1.5 边界/错误/状态/副作用（汇总）
| 项 | 事实 | 来源 |
|---|---|---|
| 语法错误检测 | Terminal 不检测，运行时（卡执行）才报错 | terminal.html |
| 脚本成员绑定 | 必须先选中成员名再插卡；卡绑定的是成员引用 | terminal.html, variables.html |
| 跨网络引用 | 引用他网 Disk id 的卡开始报错 | disk.html |
| 状态持久性 | 顶层可变变量易失（重插卡/重启即失） | transient_storage.html |
| 磁盘容量 | 无上限 | disk.html |
| 日志 | 每脚本独立 .stdout/.stderr，默认 2096 行上限 | logging.html |
| 副文件 | .d.ts 自动重生成，不可手改 | typescript.html |

### 1.6 对未来 DSL 的约束（不涉及语法）
- 脚本“逻辑单元 = 可绑定到 Variable Card 的具名成员”；DSL 必须定义**从命名成员到卡的绑定/引用模型**，并处理**跨网络引用失效**语义。
- 值双向映射 ID↔宿主语言，且**函数 = Operator 一等值**，可作 filter/predicate 参数。
- 存在“全局函数（`typeXxx`）与方法（`value.xxx()`）”**双调用面**，DSL 需决定是否保留二者并提供 operator 名录/名称解析。
- **状态模型默认无状态/易失**；持久状态须显式外部机制（Delayer 之类）。DSL 若引入变量/状态，必须明确其生命周期与重置点。
- 外部文件系统与脚本存储路径、自动重载、日志流是既有交互面；错误在**执行期**而非编辑期暴露 —— DSL 工具链应选择何时报错。

---

## 2. Integrated NBT（9 页）

### 2.1 机制
- 定位：扩展，提供对**任意 NBT 类型变量**的可视化浏览/检查/深层值提取，无需额外布线（introduction.html）。
- **NBT Extractor（方块）**：左侧槽放 NBT Variable Card → 加载该 NBT 的完整树 → 左键选路径 → 写入输出卡。合成：Redstone Dust / Logic Programmer / Portable Logic Programmer / Redstone Dust / Crystallized Menril Chunk / Block of Crystallized Menril / Crystallized Menril Chunk（nbt_extractor.html）。
- **NBT Extractor Remote（物品）**：右键任意已放置的 Extractor 绑定；之后可在**同维度任意位置**打开同一浏览界面；**约束：要求 line-of-sight loading —— 目标区块必须已加载**。合成含 Ender Pearl（nbt_extractor_remote.html）。

### 2.2 操作语义 / Output Mode（usage.html）
- 左槽 NBT 卡 → 树；**右键** compound tag / list entry 展开/折叠；**左键**目标条目选路径；右侧槽放兼容卡按当前 Output Mode 写入。
- Output Mode 四种：
  - **Reference Mode**：输出变量持有对提取值的**活引用**，源 NBT 变化时更新（多数场景推荐）。
  - **Operator Mode**：把提取路径编码为**可复用 Operator**，接受任意 NBT 输入并返回子值。
  - **Value Mode**：把**当前值写为固定常量**（快照）。
  - **NBT Path Mode**：把路径本身存为 **CyclopsCore NBT Path 字符串**。
- **Auto Refresh 开关**：控制 GUI 内 NBT 树是否随源变量自动刷新；关闭可冻结视图以观察快速变化的数据。

### 2.3 教程
- **entity_nbt_variable**（tutorials/entity_nbt_variable.html）：Entity Reader + `Entity` aspect → 活实体引用卡；`Entity NBT` operator（全局名 `entityNbt(Entity)→NBT`，方法名 `Entity.nbt()→NBT`）转 NBT compound；放入 Extractor 左槽，确保 Reference Mode，选路径写出。
- **display_entity_position**（tutorials/display_entity_position.html）：展开 `Pos` 列表（X/Y/Z，double），左键 `#0` 选 X 并写出；放入 Display Panel 实时显示 X，Auto Refresh 开启时随实体移动更新。

### 2.4 边界/错误/副作用
- Remote 依赖目标 chunk 加载才能加载界面（nbt_extractor_remote.html）。
- Reference vs Value 是**活引用 vs 快照**的核心语义分叉；Operator Mode 使同一路径可复用于不同 NBT。
- 路径可序列化为 CyclopsCore NBT Path 字符串（跨系统的路径表示）。

### 2.5 对 DSL 约束
- 需定义 **NBT 类型的一等表示 + 路径寻址**（键、列表索引）；路径可编码/序列化。
- 提取结果须区分**引用（响应式）**与**值（常量快照）**两种绑定语义；并可包装为**可复用函数（Operator，NBT→子值）**。DSL 的变量绑定/响应式模型必须覆盖这三种。

---

## 3. Integrated REST（3 页）
- **intro**（introduction.html）：通过 REST API **读/写** ID 网络；可经 Web 协议读网络与世界状态；向世界中的 **HTTP Proxy 方块 POST 值**进行交互；服务器地址见服务端控制台，**默认 `http://localhost:3000/`**。
- **HTTP Proxy**（http.html）：拥有**内部值状态**，可经 HTTP POST 更新；**必须在 Proxy 中选定某值类型，只接受该类型的请求**；更多见 GitHub wiki。合成：Logic Director / Crystallized Menril Chunk / Proxy / Crystallized Menril Chunk。
- 对 DSL 约束：网络对外存在 **HTTP 边界**，输入是**受类型约束的 JSON/POST 值**；`value_type_chemical` 明确声明 Chemical 值也“fully supported within ... Integrated REST”（value_type_chemical.html）——DSL 的类型系统若覆盖 Chemical 需考虑其 REST 序列化面。

---

## 4. Integrated Mekanism（14 页）

### 4.1 定位（introduction.html）
读取 chemical 相关机器属性、对其施加 operators、跨网络传输 chemicals、通过 Storage Terminal 访问、并做 chemicals 自动合成。

### 4.2 **Chemical 值类型**（logic/value_type_chemical.html）
- 新增**专用值类型**，表示对 Mekanism chemical 的引用（例：Sodium、Oxygen、Dirty Gold Slurry、Biomass）。
- Chemicals 亦支持于 **Recipes 与 Ingredients**；chemical 配方在 Logic Programmer 中有更便捷入口 **Chemical Recipe**（支持 JEI 等）。
- **“Chemical values are also fully supported within mods such as Integrated Scripting and Integrated REST”** → 该类型跨 Scripting/REST 通用。

### 4.3 Chemical Reader（logic/part_reader_chemical.html）
Part，读其面对方向 chemical 机器信息；合成：Basic Chemical Tank ×2 + Input Variable Transformer + Basic Chemical Tank ×…（共 4 个 tank 槽位+IVT）。Aspects（输出类型 / 属性）：
| Aspect | 输出 | 属性 |
|---|---|---|
| Tank Full / Tank Empty / Tank Not Empty | Boolean | — |
| Is Tank | Boolean | — |
| Chemical Amount | Long | Tank ID |
| Total Chemical Amount | Long | — |
| Chemical Capacity | Long | Tank ID |
| Total Chemical Capacity | Long | — |
| Tanks | Integer | — |
| Chemical Fill Ratio | Double | Tank ID |
| Tank Chemicals | List | — |
| Tank Capacities | List | — |
| Tank Chemical | Chemical | Tank ID |

### 4.4 New read aspects（logic/part_readers.html）
World Reader / Machine Reader 增补：`Radiation`（Double，Sv/h）、`Fission Reactor Damage`（Double）、`Actual Burn Rate`（Double）、`Maximum Burn Rate`（Double）、`Set Burn Rate`（Double）、`Heating Rate`（Long，mB/t）、`Environmental Loss`（Double）。均围绕 Mekanism 辐射/裂变反应堆统计。

### 4.5 Chemical Operators（logic/operators.html，全量）
每条同时给出 **Global name** 与 **Method name**（即 §1.2 的全局函数/方法双面）：
- Item 侧：`itemstackIsChemicalStack(Item)→Boolean` / `Item.isChemicalStack()`；`itemstackChemicalStack(Item)→Chemical` / `Item.chemicalStack()`；`itemstackChemicalCapacity(Item)→Long`（mB）/ `Item.chemicalCapacity()`。
- Chemical 侧：`chemicalstackAmount→Long`；`chemicalstackTint→Integer`；`chemicalstackIsRadioactive→Boolean`；`chemicalstackRadioactivity→Double`；`chemicalstackIsCoolant→Boolean`；`chemicalstackThermalEnthalpy→Double`；`chemicalstackConductivity→Double`；`chemicalstackIsCooledCoolant→Boolean`；`chemicalstackCooledCoolantOf(Chemical)→Chemical`；`chemicalstackIsHeatedCoolant→Boolean`；`chemicalstackHeatedCoolantOf(Chemical)→Chemical`；`chemicalstackIsFuel→Boolean`；`chemicalstackBurnTicks→Integer`；`chemicalstackEnergyPerTick→Long`；`chemicalstackIsRawEqual(Chemical,Chemical)→Boolean`；`chemicalstackMod→String`；`chemicalstackWithAmount(Chemical,Long)→Chemical`；`chemicalstackTags→List`。
- String 侧：`stringChemicalsByTag(String)→List` / `String.chemicalsByTag()`；`stringChemicalByName(String)→Chemical` / `String.chemicalByName()`。
- Ingredients 侧：`ingredientsChemicals(Ingredients)→List`；`ingredientsWithChemical(Ingredients,Integer,Chemical)→Ingredients`；`ingredientsWithChemicals(Ingredients,List)→Ingredients`。
- Entity 侧：`entityRadiation(Entity)→Double`（Sv/h）/ `Entity.radiation()`。

### 4.6 Chemical Tunnels（manual/tunnels.html）
- 与 items/fluids/energy 传输一致。**警告：Mekanism Chemical Tank 默认并非各面都允许 chemical 交互**，可能需在 Tank GUI 为连接面显式允许 Input/Output。
- 部件：`Chemical Interface`（暴露 tank 到网络）、`Chemical Importer` / `Chemical Exporter`（导入/导出）；进阶：`World Chemical Importer`、`World Chemical Exporter`、`Filtering Chemical Interface`。合成链：Interface = Crystallized Menril Chunk×5 + Basic Chemical Tank…；Importer/Exporter = Interface + Input/Output Variable Transformer；World 版 = Importer/Exporter + Logic Director；Filtering = Interface×2。
- Aspect 族（含 Input 与 Properties）：
  - 导出（Interface 面向）：Export All Chemicals(Boolean)、Export Chemical Amount(Long)、Export Chemical(Chemical)、Export Chemicals(List)、Export Chemicals Predicate(Operator)。
  - 导入：Import All Chemicals(Boolean)、Import Chemical Amount(Long)、Import Chemical(Chemical)、Import Chemicals(List)、Import Chemicals Predicate(Operator)。
  - 面向实体导出/导入：Export/Import All Chemicals (To/From Entities)(Boolean)、…Amount…、…Chemical…、…Chemicals…、…Predicate…，额外属性 **Entity Index**。
  - 过滤（Filtering Interface）：Filter All Chemicals(Boolean)、Filter Chemical(Chemical)、Filter Chemicals(List)、Filter Chemicals Predicate(Operator)。
  - 通用 Properties：**Channel、Round-Robin、Blacklist、Empty is Wildcard、Chemical Transfer Rate、Passive Interaction、Passive Interaction Ignores Filter、Check Chemical Amount、Entity Index、Apply to Insertions、Apply to Extractions、Allow if not applied**。

### 4.7 Chemical Terminals（terminals.html）
当网络挂有**至少一个有效 Chemical Interface**，Storage Terminal 即可像处理 items/fluids/energy 一样交互全部 chemicals。

### 4.8 Chemical Crafting（crafting.html）
与 items/fluids/energy 完全相同的自动合成流程：在 Logic Programmer 建 **Chemical Recipe**，或用 Machine Reader 读取机器内全部配方；配方可放入 Crafting Interface 用于 Crafting Writer / Storage Terminal 等处的自动合成。**同样警告 Tank 侧向配置**。

### 4.9 教程
- **reading**（tutorials/reading.html）：Chemical Reader 面对 Tank → 读 “Tank Empty”(Boolean) → 读 “Tank Chemical”(Chemical, prop Tank ID) → 对 Chemical 值施加 `Is Radioactive`（`chemicalstackIsRadioactive`/`Chemical.isRadioactive()`）→ Display Panel；中间变量需放 Variable Store。
- **tunnels**（tutorials/tunnels.html）：Chemical Interface 对 Tank；Chemical Importer 全量导入；Chemical Exporter 导出指定 chemical（如 Sodium）；Chemical 值可在 Logic Programmer 搜 "Chemical" 并由背包/JEI 拖入 tank 创建；aspects 同 §4.6。

### 4.10 对 DSL 约束
- Chemical 是**独立一等类型**（非 Item/Fluid 子类复用），需专门 operator 集；值含“类型 + 数量(mB)”，并区分 **raw equality**；带 tag/name 双向查表。
- 需表达 **Mekanism 外部类型映射**（chemical ↔ item/ingredient/string/entity 的多向转换）。
- 传输/过滤属 Tunnels 的 aspect + property 模型（Channel/Round-Robin/Blacklist/Passive…），DSL 若“用卡表达逻辑”需覆盖 Operator 作为 predicate 入参（Filter/Export/Import…Predicate 接受 `Operator`）。
- 辐射/裂变反应堆读数是 World/Machine Reader 的 aspects，非 chemical 类型本身。
- **扩展边界**：Mekanism chemical 属扩展功能，**不可当作 ID 核心**，也不可未经核查声称适用于 1.20.1 Forge（见 §5）。

---

## 5. 版本疑问清单（需父任务跨版本源码核查；在线书 = MC1.21.1/NeoForge/ID1.38.0）
1. **Integrated NBT 是否为 1.20-lts 存在的扩展**：NBT Extractor 方块、NBT Extractor Remote、四 Output Modes（含 **CyclopsCore NBT Path Mode**、**Operator Mode**）—— 疑为较新特性。[推断] 需核查 master-1.20-lts 是否有对应扩展/方块与模式。
2. **Scripting 的 API 形态**：`idContext.ops.*` 全局、对象方法（`item.stackable()`/`Item.chemicalStack()`）、`integratedscripting.d.ts`、外部编辑路径 `world/integratedscripting/`、transient storage 语义 —— 核查 1.20 分支 API 名与是否一致。
3. **Logging 上限 “2096 行”** —— 数值异常（疑文档笔误或实际配置默认值），核查源码/config 实值。
4. **Integrated REST**：默认端口 3000、HTTP Proxy 的“选定类型才接受请求”行为 —— 核查 1.20 分支。
5. **Integrated Mekanism 扩展**：chemical 值类型 + §4.5 全部 operators + §4.6 aspect/property（尤其 `Passive Interaction Ignores Filter`、`Check Chemical Amount`、Entity 变体）—— 疑为较新；核查该扩展在 1.20.1 Forge 是否存在及其子集。
6. **Chemical 的跨扩展承诺**（value_type_chemical.html“fully supported in Scripting and REST”）—— 仅对在线版本成立，1.20 需核查。

---

## 6. 覆盖清单（49/49，作为 coverage 依据）
```json
[
{"url":".../manual/main/integratednbt/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/main/integratednbt/introduction.html","read":true,"importance":"med","note":"NBT扩展定位"},
{"url":".../manual/main/integratednbt/nbt_extractor.html","read":true,"importance":"high","note":"方块+配方；核查1.20是否存在"},
{"url":".../manual/main/integratednbt/nbt_extractor_remote.html","read":true,"importance":"high","note":"远程+chunk加载约束"},
{"url":".../manual/main/integratednbt/usage.html","read":true,"importance":"high","note":"四Output Mode；版本疑点"},
{"url":".../manual/main/integratedrest/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/main/integratedrest/introduction.html","read":true,"importance":"med","note":"REST API，默认3000"},
{"url":".../manual/main/integratedrest/http.html","read":true,"importance":"high","note":"HTTP Proxy类型约束"},
{"url":".../manual/main/integratedscripting/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/main/integratedscripting/introduction.html","read":true,"importance":"med","note":"JS支持"},
{"url":".../manual/main/integratedscripting/materials.html","read":true,"importance":"med","note":"Mendesite配方(可压缩)"},
{"url":".../manual/main/integratedscripting/concepts.html","read":true,"importance":"high","note":"三部件模型"},
{"url":".../manual/main/integratedscripting/disk.html","read":true,"importance":"high","note":"id/跨网报错/无上限"},
{"url":".../manual/main/integratedscripting/drive.html","read":true,"importance":"med","note":"单盘接入"},
{"url":".../manual/main/integratedscripting/terminal.html","read":true,"importance":"high","note":"IDE/绑定/运行时错误"},
{"url":".../manual/main/integratedscripting/writing/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/main/integratedscripting/writing/js.html","read":true,"importance":"med","note":"ECMA-262全集"},
{"url":".../manual/main/integratedscripting/writing/variables.html","read":true,"importance":"high","note":"let/const+值互通"},
{"url":".../manual/main/integratedscripting/writing/functions.html","read":true,"importance":"high","note":"函数=Operator"},
{"url":".../manual/main/integratedscripting/writing/globals.html","read":true,"importance":"high","note":"idContext.ops"},
{"url":".../manual/main/integratedscripting/writing/methods.html","read":true,"importance":"high","note":"对象方法/参数左移"},
{"url":".../manual/main/integratedscripting/advanced/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/main/integratedscripting/advanced/transient_storage.html","read":true,"importance":"high","note":"易失状态语义"},
{"url":".../manual/main/integratedscripting/advanced/external_editing.html","read":true,"importance":"med","note":"world/integratedscripting/"},
{"url":".../manual/main/integratedscripting/advanced/logging.html","read":true,"importance":"med","note":"stdout/stderr上限2096；版本疑点"},
{"url":".../manual/main/integratedscripting/advanced/typescript.html","read":true,"importance":"med","note":"d.ts自动生成"},
{"url":".../manual/manual/integratedmekanism/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/manual/integratedmekanism/introduction.html","read":true,"importance":"med","note":"扩展定位"},
{"url":".../manual/manual/integratedmekanism/crafting.html","read":true,"importance":"med","note":"Chemical Recipe/自动合成；扩展"},
{"url":".../manual/manual/integratedmekanism/terminals.html","read":true,"importance":"med","note":"Storage Terminal化学访问"},
{"url":".../manual/manual/integratedmekanism/tunnels.html","read":true,"importance":"high","note":"Chemical tunnels全aspects/properties；版本疑点"},
{"url":".../manual/manual/integratedmekanism/logic/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../manual/manual/integratedmekanism/logic/value_type_chemical.html","read":true,"importance":"high","note":"Chemical值类型；跨扩展承诺"},
{"url":".../manual/manual/integratedmekanism/logic/part_reader_chemical.html","read":true,"importance":"high","note":"Chemical Reader aspects"},
{"url":".../manual/manual/integratedmekanism/logic/part_readers.html","read":true,"importance":"med","note":"辐射/裂变 aspects；扩展"},
{"url":".../manual/manual/integratedmekanism/logic/operators.html","read":true,"importance":"high","note":"全量chemical operators；版本疑点"},
{"url":".../tutorials/tutorials/integratedmekanism/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../tutorials/tutorials/integratedmekanism/introduction.html","read":true,"importance":"low","note":"概述"},
{"url":".../tutorials/tutorials/integratedmekanism/reading.html","read":true,"importance":"med","note":"读Tank示例"},
{"url":".../tutorials/tutorials/integratedmekanism/tunnels.html","read":true,"importance":"med","note":"化学传输示例"},
{"url":".../tutorials/tutorials/integratednbt/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../tutorials/tutorials/integratednbt/introduction.html","read":true,"importance":"low","note":"概述"},
{"url":".../tutorials/tutorials/integratednbt/entity_nbt_variable.html","read":true,"importance":"high","note":"Entity NBT路径示例"},
{"url":".../tutorials/tutorials/integratednbt/display_entity_position.html","read":true,"importance":"med","note":"实时引用示例"},
{"url":".../tutorials/tutorials/integratedscripting/index.html","read":true,"importance":"low","note":"目录页"},
{"url":".../tutorials/tutorials/integratedscripting/introduction.html","read":true,"importance":"low","note":"概述"},
{"url":".../tutorials/tutorials/integratedscripting/basics.html","read":true,"importance":"med","note":"配方(可压缩)"},
{"url":".../tutorials/tutorials/integratedscripting/terminal.html","read":true,"importance":"med","note":"写脚本+绑定"},
{"url":".../tutorials/tutorials/integratedscripting/functions.html","read":true,"importance":"high","note":"filter operator示例"}
]
```
（URL 前缀 `https://integrateddynamics.rubensworks.net/book/`）

## 7. 给父任务的确认项
- §5 六项版本疑问（重点：**Integrated NBT 扩展是否在 1.20-lts 存在**、**Chemical 扩展在 1.20.1 Forge 的覆盖面**、Scripting API 名一致性、log 上限 2096）。
- 交叉链接提示：NBT usage 页指向 `integratedmekanism/introduction`（chemical 与 NBT 相关）；REST intro 指向 `integratedterminals`（文件类型页不在本组，属 terminals 任务）。
- 本组结论中所有机制/语义均为**书内事实**（链接见各节）；对 1.20-lts 的适用性均为 **[推断]**，未核查源码。

**产物**：因你更正为只读 scout，本消息即完整报告（含 coverage 全 49 URL）。未写任何文件。