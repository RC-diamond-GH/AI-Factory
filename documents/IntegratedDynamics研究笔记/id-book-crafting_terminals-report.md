已完成 group `crafting_terminals` 全部 35 页阅读（依据 `local://id-book-crafting_terminals-sources.md` 与 `-raw-snapshot`/JSON 全文）。按要求不写文件，以下为完整报告与 coverage，供父任务保存为 `local://id-book-crafting_terminals-report.md` / `-coverage.json`。

---

# Integrated Crafting / Terminals 研究报告（crafting_terminals, 35/35 页）

## 0. 范围与方法
- 数据源：`local://id-book-crafting_terminals.json`（每页含 url/title/完整 text/links）与 `-sources.md`（956 行），逐页读完；未重复联网抓取。
- 分组：Integrated Crafting 15 页、Integrated Terminals 11 页、Tutorials 9 页。
- 标注约定：**【事实】**=页面明示；**【推断】**=由页面语义推得、页面未直说；**【版本风险】**=1.21.1 书页内容对 1.20.1 可能不成立，需父任务源码核查。

## 1. 总体结论（Summary）
- **Integrated Crafting 与 Integrated Terminals 都是 ID 的 addon 扩展，不是 ID 核心默认能力。** 页面明文把二者称为 “extension to Integrated Dynamics” / “automation addon”。
  - Integrated Crafting：<https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/introduction.html>
  - Integrated Terminals：<https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/introduction.html>
- 自动合成的两个必需部件：**Crafting Interface**（向网络声明“在哪、如何合成”）与 **Crafting Writer**（按条件发起合成 job）。Terminals 只是 GUI 层（存储浏览/发起/查看/取消 job），不提供核心配方语义。
- 语义分层（对 DSL 关键）：
  1. **ID 核心值类型/逻辑层**：`Recipe` 值类型、ingredients（Item/Fluid/Energy/Integer）、`Operator`、`List<Recipe>`、`Predicate`、Variable Card、Logic Programmer、Network Reader aspects、Display Panel。→ 这部分才是 DSL 可直接作用的对象。
  2. **Autocrafting addon 行为层**：job 生命周期、ingredients 预留、阻塞、分配、可复用物料、job 状态/owner/channel/duration、取消。→ 目前**只有极少数通过 writer aspect 可由程序触发**，其余仅 GUI 配置或只读。
- **jobs 结果回收是外部副作用，必须由 IT 物流完成**：Crafting Interface 不会自动从机器取出产物；job 只有在“预期产物被插入存储网络”后才算完成；经管道/漏斗绕过 Interface 进入存储不被检测，job 会一直等待。→ 见 §4。

## 2. 部件与类型清单（Catalogue）

### 2.1 Integrated Crafting 部件
| 部件 | 作用 | 关键属性 | 出处 |
|---|---|---|---|
| Crafting Interface | 指向机器，绑定后接收手工 recipe Variable Card | channel、priority、tick rate、**按 ingredient 类型分别设 target side**、blocking mode | [basics](https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/basics.html) / [configuration](https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/configuration.html) |
| Attuned Crafting Interface | 自动读取并暴露目标机器**全部**配方（同 Machine Reader）；部分 modded 机器不支持，放置时红边显示 | GUI 内逐个 enable/disable、搜索、Enable/Disable/Invert | [basics](.../crafting_interface/basics.html) / [attuned_recipes](.../crafting_interface/attuned_recipes.html) |
| Enumerated Crafting Interface | 接收一张持有 **List<Recipe>** 的变量，暴露列表中每个 recipe；可用逻辑派生（Machine Reader 指向机器、过滤列表） | **不支持无限列表**；超长列表按 config 可调上限截断 | [basics](.../crafting_interface/basics.html) |
| Crafting Writer | 按 aspect + 条件自动发起 job | 见 §3 aspects | [crafting_writer](.../integratedcrafting/crafting_writer.html) |

### 2.2 Integrated Terminals 部件
| 部件 | 作用 | 出处 |
|---|---|---|
| Storage Terminal | 装在 Cable 上，GUI 式管理所连网络存储；按 ingredient 类型分 tab，可按 channel 分离 | [introduction](.../storage_terminal/introduction.html) / [usage](.../storage_terminal/usage.html) |
| Portable Storage Terminal | Storage Terminal + Crystallized Menril Chunk；需网络上有 Omni-Directional Connector，右键链接 | [portable](.../storage_terminal/portable.html) |
| Crafting Job Terminal | 查看/取消全部运行中 job；显示 remaining sub-jobs、status、duration、owner、channel | [crafting_job_terminal](.../integratedterminals/crafting_job_terminal.html) |

### 2.3 值类型/逻辑对象（DSL 可表达目标）
| 对象 | 类型/输入→输出 | 出处 |
|---|---|---|
| Recipe | 值类型；含 inputs+output+每 input 的 Reusable 标记 | [basics](.../crafting_interface/basics.html), [reusable](.../crafting_interface/reusable.html) |
| ingredients | Item / Fluid / Energy（Integer） | [introduction](.../integratedcrafting/introduction.html), [usage](.../storage_terminal/usage.html) |
| Predicate（终端过滤/自定义过滤） | ingredient 类型 → Boolean | [power_usage](.../storage_terminal/power_usage.html), [filtering tutorial](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_filtering.html) |
| List<Recipe> | 供 Enumerated Crafting Interface 使用 | [basics](.../crafting_interface/basics.html) |
| Operator | “Craft Recipe from Operator” aspect 的输入；返回 Recipe（可无输入） | [crafting_writer](.../integratedcrafting/crafting_writer.html) |

## 3. Crafting Writer — aspects 与外部控制边界（重点）
出处：<https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_writer.html>

| Aspect | Input | Properties |
|---|---|---|
| Craft Recipe | Recipe | Channel, Ignore crafting jobs, Craft missing ingredients, **Craft amount** |
| Craft Item | Item | Channel, Ignore storage contents, Ignore crafting jobs, Craft missing ingredients, **Storage contents craft delay** |
| Craft Fluid | Fluid | 同上 five |
| Craft Energy | Integer | 同上 five |
| Craft Recipe from Operator | Operator | Channel, Ignore crafting jobs, Craft missing ingredients, Craft amount |

- **【事实】** 发起条件由 aspect 决定；“nothing should be crafted” 通过插入**空值 card**（如 Item 类型空值）表达——这是把外部物流/逻辑状态接入合成控制的关键原语。
- **【事实】** 可配置：crafting channel；已运行 job 是否阻塞新 job；是否连带发起缺失输入的 job；存储网络中已有 ingredients 是否阻塞新 job（Ignore storage contents）；Storage contents craft delay。
- **【事实】** `Craft Recipe from Operator`：operator 返回的 recipe 在**无输入**情况下被 craft；operator 返回空 recipe 则不合成任何东西。
- **【推断/文档歧义】** 表格中 `Craft amount` 只出现在 Craft Recipe 与 Craft Recipe from Operator；Craft Item/Fluid/Energy 列出的是 `Storage contents craft delay`。文档未解释二者语义差异（是否“数量由存储差量/延迟驱动”），需父任务源码核查。
- **【推断】** 可编程控制的边界：**能**发起/按条件发起/连发起缺失子料/按 channel 分流/读运行状态；**不能**（页面无任何 writer/aspect）程序化取消 job、设 job 优先级、或直接写入/修改 recipe 列表（recipe 只能通过 interface 的 card 或 Attuned GUI 配置，或 Enumerated 的 List<Recipe> 变量动态提供）。取消仅见 Crafting Job Terminal 与 Network Reader 读数。

## 4. Crafting Interface 机制、边界与副作用
出处：<https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/crafting.html>

- **【事实】** job 启动时，一个或多个“拥有对应 recipe”的 Crafting Interface 被选用；所有必需 ingredients 从网络**拉取并扣留在该 interface 的 job 内**（防被别处挪用）→ 预留/占用是核心副作用。
- **【事实】** Interface 依据存储网络内容（数据来自 Integrated Tunnels interfaces）把 recipe 的全部 inputs 插入绑定机器。
- **【事实】** Interface **不会**导入机器产物；玩家负责让产物回到网络。**例外：Crafting Table**（只需 interface）。
- **【事实】** job 只有“预期产物被插入存储网络”后才算 completed。合法途径：(a) 对机器挂 active IT importer；(b) 机器自动输出进 Interface。经其他路径（他 mod 管道/漏斗直写箱子/抽屉）进入存储 **不被检测** → job 永久等待，即使终端可见产物。
- **【事实】** 插入 Interface 的一切（item/fluid/energy）进入所配置 channel 的存储网络。
- **阻塞模式**（默认开）：机器一次只接受一个 recipe，前一完成才发下一个；多输入并行机器可关，关闭后 multi-amount job “尽可能多地”喂入。出处：[blocking](.../crafting_interface/blocking.html)
- **分配（Distributed）**：把 job 分摊到多个 interface，条件是每个 interface **至少**精确暴露同一 recipe（示例 40 iron / 10 furnaces）。出处：[distribution](.../crafting_interface/distribution.html)
- **动态 recipe**：recipe 可随昼夜/库存变化；新增 job 立刻用新 recipe，**运行中 job 仍用旧 recipe**；变化越频繁越卡。出处：[dynamic_recipes](.../crafting_interface/dynamic_recipes.html)
- **Reusable ingredients**：在 Logic Programmer 中 shift-click 输入槽勾 Reusable，用于不消耗完输入/只掉耐久的情形；**在 Variable Card 创建期设定**。出处：[reusable](.../crafting_interface/reusable.html)
- **Recipe Feedback**：插入 card 显示绿勾/红叉；红=card 未暴露有效 recipe 或该 recipe 不能在绑定机器合成；绿=有效且机器“大概能”处理（早期反馈，非保证）。出处：[recipe_feedback](.../crafting_interface/recipe_feedback.html)
- **Attuned 边界**：Attuned 暴露机器全部 recipe（Crafting Table 就有数百），红边 recipe 对新 job 不可用；拖拽批量切换（起始格决定 enable/disable 方向）；搜索匹配 recipe 名/模组名/recipe id；Enable/Disable/Invert 作用于当前搜索结果（如 `minecraft:` + Disable）。出处：[attuned_recipes](.../crafting_interface/attuned_recipes.html)
- **Configuration**：每 ingredient 类型可分别设 target side（如 item 从侧面、fluid 从顶部）。出处：[configuration](.../crafting_interface/configuration.html)

### 4.1 调试/可观测
出处：<https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/debugging_jobs.html>
- Network Reader 提供：网络内全部可用 recipes、全部运行中 crafting jobs、全部缺失 ingredients；可用 Display Panel 可视化。
- 若装 Integrated Terminals：Crafting Job Terminal 可视化运行 job/缺失 ingredients，并可取消 job。

## 5. Terminals 机制（存储/合成/job/末影/便携/能耗）
- **Storage Terminal 存储**（[st/introduction](.../storage_terminal/introduction.html)、[usage](.../storage_terminal/usage.html)）：装在 Cable 上；tab 汇总所连网络存储、可按 channel 分离；底部显示 total/max；术语 ingredient = 可存储物（item/fluid/energy）。交互同背包；energy/fluid 需在自身背包用容器（电池/储罐）点击。选中存储 ingredient 后：再点小幅增加、shift 大幅增加、右键减少、滚轮增减；右键移动少量、左键全部持有、中键一半。搜索：空格分词、`|` 表示 or；前缀 `@` 模组、`#` tooltip、`$` item tag（例 `$plank @minecraft`）。多种排序；item 存储存在时多一个 crafting tab（内含合成网格、清空到背包/网络、平衡网格、合成后 auto-refill 配置）。Grid Size（仅高/仅宽/固定行列 small·medium·large）、GUI Scale；**所有终端设置按玩家+按终端持久化**，右下 anvil 按钮配置新放置终端的默认值。
- **Power Users**（[power_usage](.../storage_terminal/power_usage.html)）：hotkeys 见表；装 JEI 时可用其 recipe shift-click 快速填合成网格，auto-refill 来源可配；可同步 JEI 与终端搜索框；可用 Variable Card 插入自定义 **Predicate**（输入 ingredient 类型、输出 Boolean）替代文本过滤。
  - 关键绑定：【事实】Alt+F 聚焦搜索 Logic Programmer、Tab/Shift+Tab 切 tab、C 清空网格到背包、Shift+C 到存储、B 平衡网格。
- **终端内 Autocrafting**（[st/autocrafting](.../storage_terminal/autocrafting.html)，仅装 autocrafting mod 时）：recipe 按 tab/channel 显示；点 recipe → 选执行次数 → 显示 crafting plan（必需/缺失 ingredients）→ 启动 job；运行 job 在 Crafting Job Terminal 可见。Shift-click recipe 跳过次数 GUI 直达 plan。**正在被运行 job 生产的 ingredient 显示 spinner，颜色表示 job 状态，hover 显示状态与仍在合成的数量**。
- **Crafting Job Terminal**（[crafting_job_terminal](.../integratedterminals/crafting_job_terminal.html)）：列出全部运行 job，点开看详细；每 job 显示 remaining sub-jobs、status、duration、owner、channel；hover sub-job 看状态；**能取消 job**。仅装 autocrafting mod 时适用。
- **Ender Storage**（[ender_storage](.../storage_terminal/ender_storage.html)）：Storage Terminal 可用 Eye of Ender 右键 ender-upgrade；可选 ender particles（server 端 config，**默认关**）；新增 Ender Storage tab 显示**个人 Ender Chest**内容，可插入/取出含 shift-click；因绑定玩家而非网络，该 tab **无 channel selector、搜索框、Variable 过滤槽**；破坏返还 Eye of Ender；Portable Storage Terminal 可与 Eye of Ender 在合成网格合并升级，**保留已链接网络**。
- **Portable**（[portable](.../storage_terminal/portable.html)）：Crystallized Menril Chunk + Storage Terminal 合成；需网络有 Omni-Directional Connector，右键链接；可设 hotkey 打开背包中第一个 portable（默认 C）。
- **能耗**：**书中未出现任何 Storage/Crafting Interface/Writer 的能耗或能量消耗数值**。crafting/terminals 组唯一能量相关是：Energy 可作为 ingredient 参与 autocraft（[introduction](.../integratedcrafting/introduction.html)）与 Craft Energy aspect。→ **【事实】缺失说明/引用遗漏：无能耗教程与数值**。
- **配方（可压缩）**（[st/crafting](.../storage_terminal/crafting.html)、[crafting_job_terminal](.../integratedterminals/crafting_job_terminal.html)、[portable](.../storage_terminal/portable.html)、[ender_storage](.../storage_terminal/ender_storage.html)）：
  - Menril Glass：Drying Basin，Glass + Menril Resin 1000 mB，10s。
  - Chorus Glass：Drying Basin，Glass + Liquid Chorus 1000 mB，10s。
  - Storage Terminal：Crafting Table（Glowstone Dust×?, Menril Glass, Display Panel, Input/Output Variable Transformer）。
  - Crafting Job Terminal：Crafting Table（Glowstone Dust, Menril Glass, Display Panel）。
  - Portable Storage Terminal：Crystallized Menril Chunk ×2 + Storage Terminal。
  - Portable + Eye of Ender → ender-upgraded portable（保留网络）。

## 6. 教程实例（可作 DSL 验收场景）
1. **Setup Autocrafting**（[autocrafting_setup](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/autocrafting_setup.html)）：Crafting Interface 面向 Crafting Table；Logic Programmer 手写 Recipe（Oak Log → 4 Oak Planks，**不改默认 item match 设置**）；**必须手工创建，不能用 JEI 之类自动填充**；插入后绿勾=有效并已对网络可用。成就 “Auto-Planking”。
2. **Trigger Autocrafting**（[autocrafting_trigger](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/autocrafting_trigger.html)）：合成 Crafting Writer 放网络；经 IT Item Interface + 箱子把至少 1 Oak Log 加入网络；写 Oak Planks 的 card 插入 writer；1 log→4 planks；**移走 planks 后若还有 logs 会再合成 4 个**（存储差量驱动）；可调 aspect（如 ignore storage contents）以“尽可能多合成”。成就 “Effective Auto-Planking”。
3. **Storage Terminal**（[storage_terminal](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal.html)）：合成 Menril Glass、Storage Terminal，接网络后开 GUI。
4. **Ender-upgraded Storage Terminal**（[storage_terminal_ender](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_ender.html)）：右键 Eye of Ender→消耗并新增 tab；破坏返还；portable 用合成网格同样升级。
5. **Storage Terminal Filtering**（[storage_terminal_filtering](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_filtering.html)）：文本 `$plank @minecraft`；变量过滤：Logic Programmer → Predicate 类型 → “Item Enchantable” → 写入 Variable → 插入终端右下 Variable 槽。成就 “Dynamic Storage Terminal Filtering”。
6. 目录页：[crafting tutorials index/intro](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/introduction.html)、[terminals tutorials index/intro](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/introduction.html)。

## 7. 对将来 DSL 的约束（只列约束，不定语法）
1. **必须区分核心 vs addon**：`Recipe` 值类型/Variable Card/Logic Programmer/Predicate 属核心 ID 逻辑；自动合成（job、预留、阻塞、分配、writer aspect）属 Integrated Crafting addon。DSL 若表达自动合成，需把 addon 存在性作为可选能力，而非假定。
2. **jobs 是不可回滚的外部状态机**：ingredients 被 job 扣留；取消只经终端；job 完成依赖产物回注网络。DSL 需能表达“发起后不管/等待/依赖外部物流回收”的语义，并暴露失败态（等待中、缺失 ingredients）。
3. **外部可写面很窄**：仅 Crafting Writer 的 5 个 aspect + 属性 + 空值 card 抑制；其余（blocking、attuned enable/disable、target side、channel、priority）是 interface 配置，DSL 不能假定可运行时改。
4. **recipe 来源三态**：手工 card、Attuned 机器全量（可批量开关）、Enumerated List<Recipe>（动态、有限长、有上限）。DSL 需容纳“recipe 集可动态变化且运行中 job 用旧配方”的语义。
5. **匹配/等价性**：分配要求 interface “至少精确暴露同一 recipe”；recipe 有 item match 设置与 Reusable 标记；DSL 做 recipe 相等/包含判断需考虑这些字段。
6. **可观测面**：Network Reader 提供 recipes/jobs/missing ingredients；Terminal 提供 job status/duration/owner/channel；spinner 表示在产。DSL 的调试/断言可依赖这些只读数据。
7. **多类型统一**：item/fluid/energy 统一为 ingredient 概念；energy 用 Integer；过滤 Predicate 输入 ingredient 类型输出 Boolean。
8. **无能耗模型**：书中无相关数值，DSL 不得假定能耗参数存在。

## 8. 缺失/引用遗漏
- 无 Crafting Interface/Writer/Storage Terminal 的能耗说明；无 tick 级产能/速度数值（仅“tick rate/priority”可配，无数值）。
- Crafting Writer 表格 `Craft amount` vs `Storage contents craft delay` 语义未说明（见 §3）。
- 页面对 “modded 机器可能不支持 Attuned（红边）” 未列具体判定条件。
- 未说明 job 优先级与多个 interface 竞争时的确定选取规则（只说“选用拥有对应 recipe 的 interface”）。

## 9. 版本风险清单（交父任务源码核查，master-1.20-lts SHA 69ea239d03828fc76b0384dcc487af391fd9e6bf）
书中环境为 MC1.21.1 / NeoForge / ID 1.38.0 + 多扩展。**【版本风险】**
1. **Enumerated Crafting Interface**（List<Recipe> 暴露，config 上限）——1.21.1 新特性，1.20.1 是否存在需核查。
2. **Attuned Crafting Interface 的 Machine Reader 配方读取范围与红边判定**——跨版本可能不同。
3. **Crafting Writer aspect 名称/属性**（`Ignore storage contents`、`Storage contents craft delay`、`Craft amount`）——属性随版本演进，1.20.1 可能缺字段。
4. **Crafting Job Terminal 字段**（owner/channel/duration）——GUI 信息随版本变化。
5. **配方与干燥时间**（Menril Glass 10s / 1000 mB）——数值可能不同。
6. **Ender particles server config 默认值**——跨版本可能不同。
7. addon 版本独立性：Integrated Crafting / Terminals 各自版本线，需与 ID 1.38.0≠1.20.1 对应版本分别核对。

---

## coverage（35/35，全部已读；父任务保存为 `local://id-book-crafting_terminals-coverage.json`）

```json
{
  "group": "crafting_terminals",
  "assigned": 35, "read": 35, "failed": 0,
  "note": "书生成环境 MC1.21.1/NeoForge ID1.38.0 + addons；目标 master-1.20-lts SHA69ea239d03828fc76b0384dcc487af391fd9e6bf，legacy 项须源码核查。",
  "pages": [
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/attuned_recipes.html","read":"full","importance":"high","version_issue":"Attuned 配方读取/红边判定跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/basics.html","read":"full","importance":"high","version_issue":"Enumerated Crafting Interface（List<Recipe>）可能为 1.21.1 新特性，1.20.1 存在性待查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/blocking.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/configuration.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/crafting.html","read":"full","importance":"high","version_issue":"job 完成判定/产物回注语义跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/debugging_jobs.html","read":"full","importance":"medium","version_issue":"Network Reader crafting aspects 字段跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/distribution.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/dynamic_recipes.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/index.html","read":"full","importance":"low","version_issue":"无（目录别名）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/introduction.html","read":"full","importance":"low","version_issue":"无"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/recipe_feedback.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/reusable.html","read":"full","importance":"high","version_issue":"Reusable 标记与匹配语义需源码核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_writer.html","read":"full","importance":"high","version_issue":"aspect 名称/属性（Craft amount vs Storage contents craft delay）跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/index.html","read":"full","importance":"low","version_issue":"无（目录）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/introduction.html","read":"full","importance":"high","version_issue":"确认 Integrated Crafting 为 addon 而非核心"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/crafting_job_terminal.html","read":"full","importance":"high","version_issue":"job 字段 owner/channel/duration 随版本变化"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/index.html","read":"full","importance":"low","version_issue":"无（目录）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/introduction.html","read":"full","importance":"high","version_issue":"确认 Integrated Terminals 为 addon"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/autocrafting.html","read":"full","importance":"high","version_issue":"终端内 job 发起流程跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/crafting.html","read":"full","importance":"low","version_issue":"Menril Glass 时间/量可能不同"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/ender_storage.html","read":"full","importance":"medium","version_issue":"ender particles config 默认值可能不同"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/index.html","read":"full","importance":"low","version_issue":"无（目录）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/introduction.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/portable.html","read":"full","importance":"medium","version_issue":"低（hotkey 默认 C）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/power_usage.html","read":"full","importance":"medium","version_issue":"hotkeys/JEI 同步跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/usage.html","read":"full","importance":"high","version_issue":"搜索前缀/设置持久化行为跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/autocrafting_setup.html","read":"full","importance":"high","version_issue":"配方创建流程跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/autocrafting_trigger.html","read":"full","importance":"high","version_issue":"writer aspect 属性名跨版本需核查"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/index.html","read":"full","importance":"low","version_issue":"无（目录）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedcrafting/introduction.html","read":"full","importance":"low","version_issue":"无"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/index.html","read":"full","importance":"low","version_issue":"无（目录）"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/introduction.html","read":"full","importance":"low","version_issue":"无"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal.html","read":"full","importance":"medium","version_issue":"配方可能不同"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_ender.html","read":"full","importance":"medium","version_issue":"低"},
    {"url":"https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedterminals/storage_terminal_filtering.html","read":"full","importance":"medium","version_issue":"Predicate 名称（Item Enchantable）跨版本需核查"}
  ]
}
```

---

## 高密度摘要
- **35/35 页全部读完，0 失败。** 核心结论：Crafting Interface/Writer 属 **Integrated Crafting addon**，Storage/Crafting Job Terminal 属 **Integrated Terminals addon**，**均非 ID 核心**；ID 核心只提供 Recipe/ingredient/Predicate/Variable/Network Reader 等类型与逻辑。
- 自动合成关键语义：job 扣留 ingredients；interface 只插输入、不取产物（Crafting Table 例外）；**job 仅在产物回注存储网络后完成**，旁路注入不被检测 → 永久等待；阻塞默认开；分配需各 interface 精确暴露同一 recipe；Attuned 暴露全量配方并可按搜索批量开关；Enumerated 用 List<Recipe> 且不支持无限列表；Reusable 输入在 card 创建期设定；动态 recipe 只影响新 job。
- Crafting Writer 5 个 aspect（Craft Recipe / Item / Fluid / Energy / Recipe from Operator），属性含 channel、Ignore storage contents、Ignore crafting jobs、Craft missing ingredients、Storage contents craft delay、Craft amount；空值 card = 不合成。**外部可写面仅此，取消/优先级无程序接口（取消仅终端）。**
- 书中**无任何能耗数值**；`Craft amount` 与 `Storage contents craft delay` 语义未解释 → 记为缺失。
- **需父任务确认的版本项**：①Enumerated Crafting Interface 是否存在于 1.20.1；②writer aspect 字段在 1.20.1 的差异；③Attuned/Machine Reader 配方读取与红边判定；④Crafting Job Terminal 显示字段；⑤Menril Glass 等配方的时长/量；⑥ender particles config 默认值；⑦各 addon 与 ID 1.20.1 的对应版本线。