# Integrated Dynamics 教程全量调研与 1.20.1 机制知识库

## 0. 结论与证据边界

已用 6 个只读 sub-agent 分片阅读在线书，父任务负责抓取、覆盖核验、支持章节和重点源码校审。本轮不设计 DSL 语法，不实现编译器或 Mod，不启动游戏。

本轮的“完整”指：给定入口所能到达的默认英文教程、手册、目录及扩展章节全部归档与研读；并保留全量展示的运算符/aspect 数据。它不意味着教程未写出的内部行为、所有安装组合、所有异常和跨版本运行结果已经验证。

- [书入口](https://integrateddynamics.rubensworks.net/book/index.html)明确说明使用默认配置，生成于 Minecraft 1.21.1 / NeoForge，ID 1.38.0，并装有 Tunnels、Crafting、Terminals、Scripting、NBT、REST、Mekanism 与 Refined Storage 等。
- 项目目标是 Minecraft 1.20.1 / Forge。核心源码固定到 [ID 69ea239d](https://github.com/CyclopsMC/IntegratedDynamics/tree/69ea239d03828fc76b0384dcc487af391fd9e6bf)，mod_version=1.31.3。
- 在线书是机制学习来源，目标版本代码/运行注册表才是编译与部署合同。相同显示名称不是相同版本语义的证明。
- 所有事实附原始 URL；源码推导不冒充游戏实测。各 sub-agent 报告中的在线版本内容与未核查事项保留并标注，校审结论以本总报告和版本审计为准。

## 1. 覆盖与可重用产物

抓取内部链接闭包，排除其他语言翻译、图片/CSS/脚本和站外链接。抓取到 229 个 URL，按完整正文归并得到 197 份不同内容；32 个额外 URL 是目录/index 等同内容别名。抓取失败 0、未分配/未覆盖 URL 0。

|阅读分片|不同内容页数|研读者|完整报告|
|---|---:|---|---|
|语言、类型、变量、状态与核心教程|34|BookLanguage|[报告](local://id-book-language_semantics-report.md)|
|全部运算符目录|1|BookOperators|[报告](local://id-book-operators-report.md)|
|网络、配置、所有核心读写部件与显示|34|BookNetworkParts|[报告](local://id-book-network_parts-report.md)|
|Tunnels 手册和物流/世界交互教程|29|BookTunnels|[报告](local://id-book-tunnels-report.md)|
|Crafting 与 Terminals|35|BookCraftingTerminals|[报告](local://id-book-crafting_terminals-report.md)|
|Scripting、NBT、REST、Mekanism|49|BookExtensions|[报告](local://id-book-extensions-report.md)|
|材料、基础机器、引言与支持目录|15|父任务|[报告](local://id-book-supporting-report.md)|

机器可读与原始资料：

- [全书覆盖记录](local://id-book-coverage.json)：229 URL 与规范页面映射、分片及阅读状态。
- [抓取清单](local://id-book-manifest.json)：全部页面标题、来源、内容大小、别名与生成环境。
- [全书英文文本](local://id-book-fulltext.md)：197 份规范页面的正文/附录展示文本。
- [原始 HTML 快照](local://id-book-raw-snapshot.json)：229 URL 的原始 HTML；不包括图片等外部资源的二进制文件。
- [完整运算符目录](local://id-book-operators.json)：325 个唯一显示签名；同时保存名称、符号、描述、有序输入、输出、Global/Method 交互名和出处。
- [运算符版本标注](local://id-book-operators-versioned.json)：逐条标明归属与目标版本核查级别。290 个显示名称在目标语言资源中存在，不等于290个完整合同已逐一证明；27 个 Chemical 相关项属于扩展，5 个 data-component 项不同，3 个 Encode/Decode 项目标核心未提供。
- [325 条运算符分类](local://id-book-operator-classification.json)：每条目录项的类型族和语义备注，序号 1—325 完整无重复。
- [全部 aspect 展示记录](local://id-book-aspects.json)：460 次展示记录，含名称、输入/输出、属性列表、描述与页面来源；包含教程重复展示，不代表460个不同注册ID。
- [目标版本审计](local://id-book-version-audit.json)：16 项重点机制核查及固定的扩展提交。
- [扩展语言资源快照](local://id-book-addon-language-snapshots.json)、[扩展版本配置](local://id-book-addon-version-properties.json)：保留1.20分支的实际数据。

分片原文与逐页 coverage 与报告同名前缀对应，例如 `local://id-book-language_semantics.json`、`local://id-book-language_semantics-coverage.json`。配方、进度和快捷键等细节保留在原文，主报告只压缩其机制含义。

## 2. 核心系统：不要把四种图混为一谈

1. **网络连通图**：cable、无线连接器和可达路径；可以包含跨维度边。
2. **部件/目标图**：部件在哪里、面向哪个实际目标、该目标暴露什么能力。
3. **变量依赖图**：变量卡ID、读取器引用、算子输入引用、Proxy/Delayer间接引用。
4. **副作用与任务系统**：writer执行、物流搬运、世界交互、合成job；不属于纯数学表达式本身。

普通即时表达式按需取得当前输入，不自动保留历史。世界、库存、配置、Proxy和扩展任务有状态，所以“整个ID网络完全纯且无状态”并不准确。

来源：[Networks](https://integrateddynamics.rubensworks.net/book/manual/networks.html)、[变量卡](https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_card.html)、[Parts](https://integrateddynamics.rubensworks.net/book/manual/parts/introduction.html)、[保存状态教程](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)。

## 3. 类型系统

### 3.1 具体值类型与类别

1.20 核心注册14个具体类型：Boolean、Integer、Long、Double、String、List、Operator、NBT、Block、ItemStack（书显示Item）、Entity、FluidStack（书显示Fluid）、Ingredients、Recipe。

另有5个类别：Any、Number、Named、UniquelyNamed、Nullable。Number覆盖Integer/Long/Double；Any等类别不能直接等同于Haskell类型变量、无限精度Integer或任意异质容器。

- Integer为32位有符号整数；Long为64位；Double为浮点数。数值转换存在，但不能从英文Number签名推断任意混合数值表达式都自动按期望转换。
- List有元素类型，教程使用同质列表。书上的`List`签名没展示元素参数，不代表运行时没有元素类型。
- Operator的实际签名很重要；Predicate是单个输入且Boolean输出的Operator，不是一个独立注册值类型。
- Item/Fluid携带堆叠数量及数据；Entity等对象类型的实际引用/空值语义因类型而异。变量卡的动态读取引用不等于每个对象值永远跟随世界更新。
- Ingredients/Recipe是核心ID数据；Recipe可手工构建或从支持的机器读取。实际自动合成是另一个扩展的执行能力。
- 能量没有统一Integer合同：核心Machine Reader的储能读数有Integer，Ingredients.withEnergy及Tunnels能量量/速率使用Long。应按具体操作签名区分。

源码：[ValueTypes](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypes.java)、[数值转换](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueCastMappings.java)、[Nullable类别](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeCategoryNullable.java)、[能量Ingredients](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java#L4079-L4091)。

### 3.2 边界、空值与条件输出

- “空白卡”“空ItemStack”“空List”“不可解析变量”是不同情况。空白卡在某些Boolean执行槽可代表True/全部，不是安全停机值。
- Choice要求两分支类型兼容；1.20实现只请求选中的分支值。但是未选分支的卡片依赖和类型仍须能通过图构造/验证，不能借短路隐藏非法引用。
- Get/GetOrDefault/Head/Map/Apply等有条件输出类型，不能只看目录中的Any就把结果永久丢失类型。
- List Get越界会报错；GetOrDefault提供默认值，但默认值也有元素类型要求。
- 许多异常、数值溢出和空对象边界没有在教程逐一说明，不能用“像Haskell”代替ID的真实合同。

源码：[Choice](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/GeneralChoiceOperator.java)、[List Get/GetOrDefault](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java#L965-L1010)、[空白卡Facade](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/VariableFacadeHandlerRegistry.java)。

## 4. 卡片身份、引用与求值

- 常量卡保存类型和值；读取卡保存partId与aspect；算子卡保存operator与有序variableIds；Proxy/Delayer卡通过外部部件身份解析。
- 算子卡不内嵌整个依赖程序。被引用节点必须在同一网络变量索引中可见。
- 教程使用Variable Store部署中间卡，但1.20源码还索引writer等active-variable部件的卡槽；不要把Variable Store认定为唯一合法来源。
- ID通过服务端全局计数器分配真实变量ID。重写已有卡的常用路径可能保留已有ID；创建新变量与修改既有变量必须区别。
- 求值使用LazyExpression和缓存失效传播；不能只改NBT就假设网络立刻换用新程序。库存更新、VariableContentsUpdatedEvent、部件激活共同构成生效路径。
- 标签便于显示与排错，不替代ID的身份语义。显示名称、符号、Global/Method交互名与注册ResourceLocation也不是同一套名称。

来源：[Variable Store](https://integrateddynamics.rubensworks.net/book/manual/logic/variables/variable_store.html)、[Logic Programming](https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html)。源码：[OperatorVariableFacade](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/OperatorVariableFacade.java)、[PartNetwork索引](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/network/PartNetwork.java#L119-L155)、[LazyExpression](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/expression/LazyExpression.java)。

## 5. 高阶运算、惰性列表与状态

### 5.1 Operator是一等值

Apply系列支持参数绑定，绑定不足会得到剩余参数的Operator；Pipe组合输出到输入，Pipe2将两路结果组合，Flip改变前两个参数顺序；谓词可合取、析取、取反。Map/Filter/Reduce/Reduce1处理Operator与List。

CurriedOperator保存绑定的IVariable，动态读取变量可以参与绑定，不是所有闭包都变成常量快照。物化操作则会把绑定值变成脱离外部引用的值。

不能由这些原语直接推出支持任意Haskell函数、任意递归或完整Haskell类型推导；这轮只确认ID的实际表达能力。

来源：[The Value of Operators](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)、[Operator类型](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/operator.html)。源码：[CurriedOperator](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/CurriedOperator.java)、[ValueHelpers.evaluateOperator](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueHelpers.java#L144-L173)。

### 5.2 无限列表存在，但不等于无限程序随意求值

Lazy Built从起始值和后继Operator构造惰性列表。目标实现明确isInfinite=true。Filter、Reduce以及多个要求枚举全部元素的操作拒绝无限列表；其他操作的精确边界也不能只从名称猜。

因此即时卡片图的非法循环、按需生成无限列表、Delayer跨时间反馈是三个不同机制。

来源：[Challenges](https://integrateddynamics.rubensworks.net/book/tutorials/challenges.html)、[完整运算符目录](https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html)。源码：[ValueTypeListProxyLazyBuilt](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyLazyBuilt.java)、[Filter/Reduce](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java#L3078-L3248)。

### 5.3 三种机器不要混淆

|机制|输出语义|依赖与生命周期|
|---|---|---|
|Materializer|将当前动态值变成静态快照|生成时需要源卡；生成后的静态卡不要求源卡继续驻留|
|Proxy|以固定间接引用访问其槽中当前卡|输入卡改变会改变解析目标；不是保存上一tick|
|Delayer|以List暴露采样历史|容量、采样间隔、初始空历史、持久化与反馈必须考虑|

Delayer最新值在历史末尾。教程用容量1构造锁存、容量2识别[False,True]上升沿；空历史使用GetOrDefault。1.20代码存在容量、更新间隔、历史队列及NBT保存/读取；在线Saving State教程本身未出现在目标语言资源中。实际同tick观察顺序仍需实机核对，不能把不同部件都看成同时执行。

来源：[Materializer](https://integrateddynamics.rubensworks.net/book/manual/machines/materializer.html)、[Proxy](https://integrateddynamics.rubensworks.net/book/manual/machines/proxy.html)、[Delayer](https://integrateddynamics.rubensworks.net/book/manual/machines/delay.html)、[Saving State](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)。源码：[BlockEntityDelay](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityDelay.java#L132-L220)。

## 6. 网络、部件与配置

- cable的各面可以放part。读取器的aspect产生值，writer的aspect接收值并执行动作，显示器是输出消费者。
- 枚举部件可以从INetwork.getElements筛选IPartNetworkElement；IPartNetwork本身没有“列出所有变量ID”的公共接口。变量箱和active-variable卡槽要另外读取。
- 实际目标包括位置、维度、面、偏移与覆盖面。Dynamic Offset不是普通的固定世界坐标；受增强件与实际实现限制。
- Ticks/Operation数值越大执行越慢；Priority影响同tick顺序，相同priority不能作为可重复程序顺序保证。
- 书中的Energy Channel用于取能，不能等同于Tunnels的物流Channel或接口暴露通道。
- Mono连接器用于直线远程连接；Omni同组连接器可以跨维度。完整拓扑应沿IPathElement可达边，包含part提供的无线边，不是六方向电缆邻接图。
- 未加载部件不能等同于已删除；不能长期持有旧网络对象或旧part state。服务器提供的描述与部署时的实际状态要分开。
- 空aspect页面不是抓取失败。某些part的有效能力由其他mod注入或目标capability决定；安装组合会改变目录。
- Diagnostics可提供部件、耗时和部分存储观察数据，但不是完整变量程序、全部属性和全部电缆边的现成导出协议。

来源：[Settings](https://integrateddynamics.rubensworks.net/book/manual/parts/settings.html)、[Offsets](https://integrateddynamics.rubensworks.net/book/manual/parts/offsets.html)、[Omni Connector](https://integrateddynamics.rubensworks.net/book/manual/parts/other/connector_omni_directional.html)、[Diagnostics](https://integrateddynamics.rubensworks.net/book/manual/diagnostics.html)。源码：[NetworkDiagnostics](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/network/diagnostics/NetworkDiagnostics.java)、[Omni可达关系](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/PartTypeConnectorOmniDirectional.java#L213-L226)。

完整reader/writer目录在[部件报告](local://id-book-network_parts-report.md)与[aspect数据](local://id-book-aspects.json)，覆盖audio/block/entity/extra-dimensional/fluid/inventory/machine/network/redstone/world读取以及audio/effect/entity/inventory/machine/redstone写入；各页可注入扩展能力，不把名称当可用性保证。

## 7. Tunnels：计算和动作的分界

Interface将库存/储罐/能量提供给网络；Importer从目标导入接口所连接的网络存储；Exporter从网络存储导出到目标；Filtering Interface限制暴露和存取。World部件进行掉落实体、方块、流体与实体库存交互；Player Simulator模拟持物点击。方向不是由一张卡任意选择的。

- Boolean All类是门控，不能同时筛选物品种类。
- Item/Fluid原型、同类型List、Predicate Operator、NBT分别提供不同选择方式；Amount和Slot整数具有不同意义。
- 一个普通writer同一时刻只能有一个非空控制槽，槽序选择active aspect。不能把Boolean开关和Item选择各插一张卡到同一exporter。
- 属性存于部件，不存于表达式卡：Channel、Rate、Slot、Round-Robin、匹配数据、Blacklist、Empty is Wildcard、Passive Interaction等需要独立处理。
- 空白卡与空ItemStack不同；空ItemStack是否为通配由属性决定。
- PASSIVE_IO=true给部件增加被动storage filter，独立的有效目标分支仍主动搬运，不代表关闭主动IO。
- 装Integrated Crafting时，部分export动作的Craft属性可以请求缺失物料合成；这是扩展副作用，不是核心算术操作。
- 教程的每tick用语受Ticks/Operation、读取刷新和执行优化影响；不能据此保证精确同步或原子搬运。

来源：[Concepts](https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/concepts.html)、[Item](https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/item.html)、[Player](https://integrateddynamics.rubensworks.net/book/manual/main/integratedtunnels/player.html)。源码：[卡槽激活](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/part/write/PartTypeWriteBase.java#L154-L179)、[匹配及主动/被动IO](https://github.com/CyclopsMC/IntegratedTunnels/blob/ab189dd5f75716231f0bd8f9bbf0e19a920e1a95/src/main/java/org/cyclops/integratedtunnels/part/aspect/TunnelAspectWriteBuilders.java#L619-L845)。

教程提供的真实组合：

- [库存数量低于阈值时导出](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/export_items_limit.html)：读Inventory Count、Less Than比较、Boolean All gate；这里是库存总量，不是特定物品计数。
- [按模组过滤](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/filter_storage_mod.html)：Mod读取函数、Equals部分应用、Pipe，形成Item→Boolean。
- [按昼夜暴露库存](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/filter_storage_day.html)：World Reader Is Day控制Filtering Interface。
- [按标签放置方块](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/place_logwood.html)：标签查询产生List，再传给World Block Exporter。
- [模拟持剑点击](https://integrateddynamics.rubensworks.net/book/tutorials/tutorials/integratedtunnels/click_sword.html)：原型卡选择网络里的物品，Player Simulator执行动作。

所有29页的完整步骤和非卡设置在[Tunnels报告](local://id-book-tunnels-report.md)。

## 8. Crafting 与 Terminals

核心Recipe描述输入/输出Ingredients及匹配/可复用设置；Integrated Crafting负责接口暴露配方、writer触发job、物料预留、阻塞、分配、完成检测。机器是否支持配方读取取决于recipe-handler能力，不能假定任意机器都能枚举配方。

- 动态配方影响新job；正在运行的job保留已选配方。不能把修改卡片当成取消/改写已运行任务。
- job会预留输入，产物返回网络和识别是独立于表达式求值的过程；单纯宣告一个Recipe值不等于已经完成合成。
- 手工Interface与Attuned Interface不同；在线书还介绍Enumerated List<Recipe>接口，但目标1.20注册表未提供它。
- Terminals是存储浏览、谓词过滤、配方发起和job观察/取消界面；不替代逻辑求值。
- 在线书的Ender Storage升级不在目标分支发现的实现/资源中，不纳入目标保证。

来源：[Basics](https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/basics.html)、[Dynamic Recipes](https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/dynamic_recipes.html)、[Crafting](https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_interface/crafting.html)、[Crafting Writer](https://integrateddynamics.rubensworks.net/book/manual/main/integratedcrafting/crafting_writer.html)、[Storage Terminal](https://integrateddynamics.rubensworks.net/book/manual/main/integratedterminals/storage_terminal/usage.html)。

## 9. 其他扩展

### Scripting

Script Disk保存脚本，Drive暴露到网络，Terminal编辑并将具名成员绑定到卡。JS函数可作为Operator，内建函数通过idContext.ops和对象方法使用。顶层可变状态可能在重插卡/重启时重置；TypeScript页描述的是外部编辑器typings支持，不是TypeScript运行时。它证明已有“语言→ID变量/Operator”的桥接模式，但不是本项目原生变量卡DSL的替代方案。

来源：[Concepts](https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/concepts.html)、[Functions](https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/writing/functions.html)、[Transient Storage](https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/transient_storage.html)、[TypeScript](https://integrateddynamics.rubensworks.net/book/manual/main/integratedscripting/advanced/typescript.html)。

### NBT

Extractor的Reference、Operator、Value、NBT Path四模式分别是动态引用、可复用提取函数、快照和路径字符串。这四模式在目标1.20资源中也存在，不能只因在线版本较新就判为新功能。Remote有位置/加载范围限制。

核心NBT Path提供field、index、slice、filter等导航；它来自CyclopsCore，不是Minecraft原版命令的路径语言。网页将and显示为§§，目标语言资源明确是&&。

来源：[Extractor Usage](https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/usage.html)、[Remote](https://integrateddynamics.rubensworks.net/book/manual/main/integratednbt/nbt_extractor_remote.html)、[核心NBT Path](https://integrateddynamics.rubensworks.net/book/manual/logic/advanced/nbt_path.html)。

### REST

提供HTTP访问网络与HTTP Proxy外部输入。按值类型接受/更新数据，属于外部输入与扩展状态，不是普通纯Operator。教程提到默认端口3000；实际端口和暴露范围以服务器配置为准，不从教程推出安全或可达性保证。

来源：[REST Introduction](https://integrateddynamics.rubensworks.net/book/manual/main/integratedrest/introduction.html)、[HTTP Proxy](https://integrateddynamics.rubensworks.net/book/manual/main/integratedrest/http.html)。

### Mekanism

Chemical是附加值类型，有数量、标签、原始类型比较和相应Operator。Chemical Reader/物流/合成/终端能力属于扩展；目标1.20的IntegratedMekanism也注册ChemicalStack类型，但这不证明在线27条运算符和全部属性逐条与目标一致。目标机器的侧向Input/Output配置仍影响可用性。

来源：[Chemical类型](https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/value_type_chemical.html)、[Operators](https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/logic/operators.html)、[Tunnels](https://integrateddynamics.rubensworks.net/book/manual/manual/integratedmekanism/tunnels.html)。完整49页的机制在[扩展报告](local://id-book-extensions-report.md)。

## 10. 已确认的版本差异

|在线书内容|目标1.20.1核查结果|设计前的约束|
|---|---|---|
|Encode / Encode Compressed / Decode；Materializer编码按钮|目标核心未提供这些原语/按钮|不能把新书字符串编码流程当目标原语|
|Item/Fluid Data Keys / Data Value / With Data|目标使用NBT相关操作；这些data-component合同不可直接套用|分别按目标注册签名与NBT模型处理|
|Saving State教程|目标未含这章，但已有Delayer历史/持久化实现|机制可研究，实际反馈时序不冒充实测|
|Wrench Copy All / Settings / Aspect|目标Wrench仅DEFAULT/OFFSET/OFFSET_SIDE模式|不能依赖此新书部署捷径|
|Enumerated Crafting Interface|目标Crafting注册表只有普通、Attuned接口和Writer|List<Recipe>暴露接口不能默认存在|
|Terminal Ender升级|目标分支未发现对应实现或语言资源|不纳入保证支持的目标能力|
|NBT Extractor四模式|目标资源中存在|属于可用扩展候选，不当作核心默认安装|
|Chemical、Scripting typings|目标扩展存在相关类型/生成器|扩展类型与工具能力需显式区分|

目标扩展固定版本：Tunnels1.10.1、Crafting1.4.8、Terminals1.7.0、Scripting1.0.19、NBT1.6.3、REST1.1.11、Mekanism1.1.0。完整提交与证据见[版本审计](local://id-book-version-audit.json)。这些只是本次读取的分支快照，不声明所有版本组合已通过加载测试。

关键源码：[目标Operators](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java)、[目标Materializer屏幕](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/client/gui/container/ContainerScreenMaterializer.java)、[目标Wrench](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/item/ItemWrench.java)、[目标Crafting注册表](https://github.com/CyclopsMC/IntegratedCrafting/blob/ac3ffcd1ba7c212f63a2762818aa02048f654a04/src/main/java/org/cyclops/integratedcrafting/part/PartTypes.java)、[目标Terminals](https://github.com/CyclopsMC/IntegratedTerminals/tree/efeffcbe3361ec3375a36ad1ac156bc3dc91259a)。

## 11. 对下一阶段的语义约束（不设计语法）

1. 绑定到ID真实类型与目标注册表，不把网页显示名当稳定注册ID。
2. 区分普通值、动态读取、Operator值、引用身份、物化快照与时间状态。
3. 保持Choice/逻辑操作的求值边界；不把所有分支提前计算，也不让短路掩盖缺失依赖。
4. 即时循环、惰性无限列表与Delayer反馈分别建模，不能泛化成“支持任意递归”。
5. 表达式结果与writer/aspect部署配置分离；通道、slot、rate、empty语义不在最终卡片内。
6. 真实变量ID与玩家可修改目标仍由服务端负责；客户端编译产物只能描述可验证的卡片构造计划。
7. 编译线程处理描述数据，不操作世界或活的part state；运行时读取与副作用留在ID/服务端。
8. 扩展能力是安装环境的一部分；没有某个addon时，不能仍接受其类型、aspect或执行目标。
9. 编译期不能证明目标机器当前能接收物料、块已加载或job会完成；语法/类型、引用可见性、部署可用性与运行结果分开。
10. 这轮保留教材和目录，不预先固定DSL具体语法、完整语言范围或优化规则。

## 12. 仍属于资料缺口而非已证明事实的内容

- 教程并未逐条给出全部属性默认值、单位、NBT匹配算法、溢出/除零/非法输入行为。
- 已有16项重点源码核查，不等于325条在线显示签名及460条aspect展示都完成跨版本逐项运行证明。
- Delayer反馈、多个writer同tick交互、库存并发和网络拆合的实际行为没有本轮游戏实测。
- Scripting日志页写2096行、部分示例拼写等保留为文档表述，不将其未经实现核对当实际默认合同。
- 配方与资源图已归档HTML/文字，不声明对所有配方进行了离线图片归档或游戏验收。

这些边界不影响本轮教程全量知识交付；它们限定了下一阶段能从资料中直接采用哪些事实，不能用未验证假设填补。
