# Integrated Dynamics DSL 设计

## 0. 版本、证据边界与结论

本文是 Minecraft 1.20.1 / Forge 47.0.1 / Integrated Dynamics `1.31.3` 的 DSL 语言、静态类型环境、编译计划与原生制卡合同。目标源码固定为 [`69ea239d03828fc76b0384dcc487af391fd9e6bf`](https://github.com/CyclopsMC/IntegratedDynamics/tree/69ea239d03828fc76b0384dcc487af391fd9e6bf)；原生 API、NBT、operator/aspect 表与细节证据见 [`IntegratedDynamics研究笔记/id-dsl-native-source-audit.md`](./IntegratedDynamics研究笔记/id-dsl-native-source-audit.md)。

**验证边界**：本轮只静态阅读目标 commit 源码，没有启动游戏、没有运行 Gradle、没有在真实世界制卡或放置卡，也没有验证客户端工具/后端网络包的运行结果；文中不把源码推导冒充游戏实测。凡涉及实际 `writeVariableFacadeItem`、库存容量、变量仓库刷新、Delayer 反馈的行为，实现后仍需游戏内验证。

本设计解决的是“有限语言 + 结构化计划 + 原生卡片”的闭环：

- Agent/用户写类 Haskell 的 DSL 源文件；
- 客户端离线编译为**有界、无源码、无原始卡片 NBT** 的 JSON 计划；
- 服务端独立校验计划，使用 ID 原生 facade/handler 制卡到玩家主背包；
- 用户自行把卡放入同一网络的变量仓库并接线，DSL/工具不放置、不改部件。

DSL 不是完整 Haskell，不是客户端求值器，也不是服务器脚本解释器。它只组合 ID 已有的值、Aspect 读取、Operator 和列表；所有世界读取/运算仍由 ID 原生求值。

## 1. 职责、工具与不变量

| 层 | 职责 |
|---|---|
| Agent/用户 | 提供源码与 bindings，选择目标网络/输出用途，观察预览与制卡结果 |
| `id_compile` | 纯客户端预览：解析、静态检查、生成 plan/诊断；不分配 ID、不写卡、不改世界 |
| `id_make_cards` | 重新编译并发送 plan；机制 RPC 只发送结构化计划，不发送源码/AST/sourceMap |
| `id_describe_inputs` | 服务端只读：按 bindings 解析网络/part/facade，返回静态类型与有限性；不求值活变量 |
| `id_create_cards` | 服务端：独立校验 plan，预检容量，分配 ID，写卡到主背包，返回实际 ID |
| ID 原生 | 变量卡求值、网络缓存、Aspect 读取、Proxy/Delay、列表/Operator 语义 |

不变量：

1. `id_describe_inputs`/`id_create_cards` 机制 RPC 不携带 DSL 源码、AST、sourceMap 或客户端字节码；`id_create_cards` 只接收 `version/contract/imports/nodes/outputs`。服务端不解析/执行 DSL。
2. 计划中每个 node 的类型是**断言**，服务端必须用固定签名表和输入计划独立重算；不信任客户端类型。
3. 服务端不从源码名称/显示名猜 operator 或 aspect；只接受注册 ID 和固定合同。
4. 不手工拼 `_type/_id` 或原生卡片 NBT；通过 `IVariableFacadeHandlerRegistry` 与 facade 构造器写卡。
5. 卡片进入玩家背包；不自动放入变量仓库、不自动接线、不修改部件属性。
6. 客户端不能分配变量 ID；ID 只在服务端 `generateId=true` 时分配。
7. 服务端不会“先完整验证原生图再分配 ID 且失败无痕”：原生 ID 在 facade 创建时分配，失败会消耗 ID。设计把流程拆成 preflight 与 allocate/write 两阶段，明确不可回滚边界。

**源码持久化边界（与机制 RPC 区分）**：现有 `SharedContextCodec` 会把模型 `ToolCall.arguments`（包括 `source`）作为对话上下文保存到服务端世界存档。因此不能笼统声称“服务端从不接收 DSL 源码”。准确合同是：

- `id_describe_inputs`/`id_create_cards` 机制 RPC 不携带源码、AST、sourceMap；服务端不解析、不执行、不用源码做类型/制卡决定；
- 共享会话持久化仍可能以**不透明对话数据**保存 `source` 文本，沿用现有隐私/容量合同；它不进入卡片、plan 校验或 ID 分配；
- 计划本身不含源码；服务端错误只带 phase/node/input，不返回源码。

## 2. 端到端流程

1. Agent 从会话/终端获得授权网络与输入信息，构造 bindings：
   - `{"kind":"variable","id":42}`：网络中已有变量卡；Proxy/Delay 卡也用这种形式。
   - `{"kind":"aspect","part_id":7,"aspect":"integrateddynamics:list_inventory_itemstacks"}`：网络中的 part id 与 aspect 注册 ID。
2. 调 `id_describe_inputs {bindings}`。服务端只读解析网络/part/facade，返回每个输入名的静态类型与 `finite`；不能静态证明的 Fn/List 元素类型直接报错。
3. 客户端把 `source` 与 `bindings` 交给 `id_compile`。编译器校验源输入注解与 describe 类型一致，做名称/类型/有限性/可达性检查，生成预览 plan 与诊断；不产生真实 ID。
4. 用户确认后调 `id_make_cards {source, bindings}`。客户端重新编译，发送 plan 到 `id_create_cards`。
5. 服务端重新做 schema/类型/注册表/网络/容量校验；为 aspect import 与每个新定义分配 ID、写卡，放入主背包；返回 created 卡与 outputs 映射。
6. 用户把卡放入同一网络的变量仓库（可多个仓库），确保所有依赖卡与 Aspect 卡都在网络中；把输出卡接到目标部件/管道。Delayer 等反馈由用户手动接线。
7. 若输出只是既有 variable 输入，则没有新卡，返回既有 ID；若中途失败，已建卡/已消耗空白卡可能保留，错误带 phase 与 node/input。

预览与制卡共享同一编译器与同一 plan 校验 helper；`id_make_cards` 不做“预览通过就跳过服务端检查”。

## 3. 语言示例

### 3.1 按库存补货

```haskell
input stock : [Item];
input limit : Integer;

let iron = item "minecraft:iron_ingot";
let count = itemCount stock iron;
let need = count < limit;

output request : Item = if need then iron else emptyItem;
output enabled : Boolean = need;
```

- `stock` 绑定到 `integrateddynamics:list_inventory_itemstacks` 的 Aspect（part `inventory_reader`），或绑定到已有 `[Item]` 变量；不是由源码坐标指定容器。
- `itemCount` 统计同物品且同 NBT 的堆叠数量，不是槽数，也不是所有物品总数；查询空物品返回 0。
- `request` 是 Item 值；`enabled` 是 Boolean。它们如何接到 exporter 槽/配置由用户完成，DSL 不设置 Empty is Wildcard/Blacklist/Rate/Channel。
- `if` 是原生 Choice，仅求值选中分支；两分支仍须静态同类型。
- 该示例的 plan 节点见 §11.2。

### 3.2 物品谓词

```haskell
input modName : String;

let fromMod : Fn(Item) -> String = itemMod;
let matchesMod : Fn(Item) -> Boolean = fromMod >> eq modName;
let acceptable : Fn(Item) -> Boolean = allOf matchesMod itemIsEnchantable;

output predicate : Fn(Item) -> Boolean = acceptable;
```

- `itemMod` 返回物品所属 Mod 的**显示名**（由 `ModList` 取得，`minecraft` 为 `Minecraft`），不是 namespace。`eq modName` 比较显示名，不是注册前缀。
- `eq` 的具体签名由上下文特化为 `Fn(String,String) -> Boolean`；`eq modName` 是部分应用，编译为 `operator_apply` 调用，不生成“已绑定动态值的 Operator value 卡”。
- `>>` 是原生 `operator_pipe` 的单参组合：`fromMod >> eq modName` 得到 `Fn(Item) -> Boolean`。原生 Pipe 会先把左函数结果求值成中间变量，再把右函数应用到它。
- `allOf` 是原生 predicate conjunction，对同一输入短路；不是 Boolean 值的 `&&`。
- 该程序会产生 `native` 卡：`itemMod`、`eq`、`itemIsEnchantable`，以及 `operator_apply`、`operator_pipe`、`operator_conjunction` 调用卡；具体共享/节点数见 §11。

### 3.3 动态选择函数值

```haskell
input enabled : Boolean;

let fallback : Fn(Item) -> Boolean = constant false;
let predicate : Fn(Item) -> Boolean =
    if enabled then itemIsEnchantable else fallback;

output filterFn : Fn(Item) -> Boolean = predicate;
```

- `constant false` 用 `general_constant` 的 apply 表达；其被忽略的第二个参数类型由上下文特化为 `Item`。它返回的是一个 Operator value，不会在编译期把 `false` 应用到库存。
- `if` 的两个分支都是 `Fn(Item) -> Boolean`；原生 Choice 只选一个 Operator 值，后续消费者再应用。
- 没有客户端 lambda；函数值只能来自原生 operator、部分应用、组合和输入。

### 3.4 Delayer 历史（外部状态）

```haskell
input history : [Boolean];
input start : Boolean;
input stop : Boolean;

let previous = getOr history 0 false;
output next : Boolean = if stop then false else start || previous;
```

- `history` 必须绑定到一个**静态可证明元素类型为 Boolean** 的 Delay 卡：describe 沿 Delay 的网络对象引用读取其输入槽卡片的静态类型；若输入槽为空/不可证明，`id_describe_inputs` 直接报 `E_INPUT_BINDING`，不能用源码注解伪造。
- 首次搭建时，Delay 输入槽需先有一张 Boolean 类型的卡（例如 Boolean 常量卡）才能被静态证明；用户随后可把 `next` 卡换入/接到该槽。此替换与反馈接线完全由用户完成，DSL/制卡不生成反向边，也不自动放置。
- 即时定义图没有环；`history` 是外部变量引用，`next` 不回指源码节点。Delay 的容量、间隔、优先级、持久化与同 tick 顺序不属于 DSL。

### 3.5 有限图中的无限列表

```haskell
let naturals = iterate 0 increment;
output firstTen : [Integer] = slice naturals 0 10;
```

```haskell
-- 编译错误示例：已知无限列表进入 filter
output bad : [Integer] = filter (flip lt 10) (iterate 0 increment);
```

- `iterate` 映射 `list_lazybuilt`，列表在需要时生成；源文件/卡片图仍是有限的。
- `slice` 的 `from < to` 必须成立；`slice naturals 0 10` 得到有限切片。原生 `list_slice` 在 `from >= to` 时抛错，不会返回空切片。
- 原生 `length (iterate ...)` 返回 `Integer.MAX_VALUE`（不是错误，也不是数学无穷）；`isEmpty` 对这类列表返回 false。DSL 不重新定义这些原生行为。
- 编译器自行跟踪有限性：`iterate` 为 infinite，`map`/`append` 等继承；已知 infinite 进入 `filter`/`reduce`/`itemCount` 报 `E_INFINITE_CONSUMER`。原生 `map`/`slice`/`append` 的 `isInfinite()` 不传播，不能只依赖它。

## 4. 词法、语法与声明

### 4.1 源文件

- 源码为 UTF-8；编译输入上限为 **32768 UTF-8 bytes**。AST/诊断范围使用 **UTF-16 offset**（Kotlin String 索引），行列从 1 开始，列也按 UTF-16 计数。字节限制与偏移单位不同，不能混用。
- 空白与缩进无语义；每条声明以 `;` 结束，最后一条也要。
- `--` 到行尾为注释；不支持块注释/嵌套注释。`a--b` 会开始注释；减法负数写 `a - (-b)`。
- 标识符 `[A-Za-z_][A-Za-z0-9_]*`，区分大小写。
- 关键字：`input`、`let`、`output`、`if`、`then`、`else`、`true`、`false`、`op`、`item`、`block`、`fluid`、`nbt`、`Fn`。这些不能被 input/let 名称遮蔽；内建函数名同样不能遮蔽。
- 字符串用双引号和 JSON 式转义：`\"`、`\\`、`\n`、`\r`、`\t`、`\b`、`\f`、`\uXXXX`；非法 Unicode 转义报错。字符串可含 Unicode。
- 不支持字符串插值、字符字面量、隐式资源命名空间、读取外部文件。

### 4.2 声明

```text
input  name : Type;
let    name [: Type] = expression;
output name [: Type] = expression;
```

- `input` 与 `let` 共用表达式名称空间；禁止重名、遮蔽关键字/内建名。
- `output` 使用独立端口名称空间；端口之间禁止重名；表达式不能引用 output 名称。端口可与 let/input 同名。
- 所有 input 都必须有对应 binding；bindings 中出现源文件不存在的 input 报错（防拼写错误）。
- `let` 是节点命名/共享，不是可变赋值，不按文本顺序执行；允许前向引用；全局解析后拒绝即时定义环（自引用/间接）。
- 编译器检查全文件名称/类型；只有从 outputs 可达的节点进入 plan；未使用 input/let 可以警告但接受。至少一个 output。
- 不支持 `let f x = ...`、局部 `let ... in ...`、模块、导入源码、赋值、事件/循环/部署语句。

### 4.3 EBNF

```ebnf
program       = declaration, { declaration } ;
declaration   = "input", Identifier, ":", type, ";"
              | "let", Identifier, [ ":", type ], "=", expr, ";"
              | "output", Identifier, [ ":", type ], "=", expr, ";" ;
type          = TypeName
              | "[", type, "]"
              | "Fn", "(", [ type, { ",", type } ], ")", "->", type ;
expr          = "if", expr, "then", expr, "else", expr | orExpr ;
orExpr        = andExpr, [ "||", orExpr ] ;
andExpr       = compareExpr, [ "&&", andExpr ] ;
compareExpr   = composeExpr, [ compareOp, composeExpr ] ;
compareOp     = "==" | "!=" | "<" | "<=" | ">" | ">=" ;
composeExpr   = concatExpr, [ ">>", composeExpr ] ;
concatExpr    = addExpr, [ "++", concatExpr ] ;
addExpr       = mulExpr, { ( "+" | "-" ), mulExpr } ;
mulExpr       = unaryExpr, { ( "*" | "/" | "%" ), unaryExpr } ;
unaryExpr     = "-", unaryExpr | application ;
application   = atom, { atom } ;
atom          = Identifier | scalar | "(", expr, ")" | list
              | "op", StringLiteral
              | "item", StringLiteral
              | "block", StringLiteral
              | "fluid", StringLiteral, IntegerLiteral
              | "nbt", StringLiteral ;
list          = "[", [ expr, { ",", expr } ], "]" ;
scalar        = "true" | "false" | IntegerLiteral
              | LongLiteral | DoubleLiteral | StringLiteral ;
```

- `Fn` 是类型位置专用关键字，不能作为值/变量名。
- 解析器最长匹配 `>>`、`>=`、`<=`、`==`、`!=`、`++`、`&&`、`||`、`->`、注释 `--`。
- `compareExpr` 最多一个比较符；`a < b < c` 拒绝，应写 `a < b && b < c`。
- 负号不是数字 token 的一部分；`-2147483648` 按带符号整体范围检查，不是先解析正数再取负。`f -1` 解析为减法；负数参数写 `f (-1)`。
- `if` 在最低优先级；作为函数参数/运算子表达式时加括号。
- 函数应用左结合；`f x y` 与 `(f x) y` 一致。
- `not` 是普通 Boolean 函数，不是新前缀运算符；没有整数/物品/空列表的隐式真值。

## 5. 类型系统

### 5.1 表面类型

```text
Boolean  Integer  Long  Double  String
Item     Block    Entity  Fluid  NBT
Ingredients  Recipe
[T]
Fn(T1, T2, ...) -> R
```

- `[T]` 是同质列表；`Fn(...) -> R` 是原生 Operator 的静态签名，参数列表不是 tuple 值；DSL 没有 tuple 值。
- `Fn` 参数/返回可以是另一个 `Fn`、列表或具体类型；`Fn() -> R` 表示零参数 Operator，用 `call0` 求值。
- 裸 `Operator` 不是源类型；它丢失参数/返回信息，无法检查 Filter/Apply/Pipe。`Any`、`Number`、`Named`、`Nullable` 不是可写类型，只能作为原生声明的内部约束；不能用注解绕过检查。
- 额外类型（如 Chemical）只有在服务端固定合同/类型表明确提供时才可出现；未知类型报错。
- **嵌套类型的外部证明限制**：DSL 类型语法/plan 结构允许 `[[T]]`、`Fn(Fn(A)->B, ...)` 作为中间节点类型；但后端固定 facade 合同只把列表元素识别为具体标量类型，Constants 只读一层 list proxy 元素类型：
  - 外部 `variable{id}` 绑定若要求 `[[T]]` 或列表元素为 Fn，`describe` 报 `E_INPUT_BINDING`；不能靠源注解补内层。
  - Operator value 卡的参数/返回若是 `integrateddynamics:operator`，其内层 Fn 签名不在 facade metadata；只有 call/combine 规则能从变量依赖图恢复的类型才可证明，否则拒绝。
  - Proxy/Delay/Aspect 同样只提供其声明的外层类型。
  新建 plan 可以构造嵌套类型节点；把该卡再次作为外部输入时，describe 仍按上述规则重新证明，证明失败即拒绝。

### 5.2 表面类型到原生 value type

| 表面类型 | 原生 id | 说明 |
|---|---|---|
| Boolean | `integrateddynamics:boolean` | |
| Integer | `integrateddynamics:integer` | 32 位有符号 |
| Long | `integrateddynamics:long` | 64 位有符号 |
| Double | `integrateddynamics:double` | |
| String | `integrateddynamics:string` | |
| Item | `integrateddynamics:itemstack` | 不是 ID 的 `Item` 显示名 |
| Block | `integrateddynamics:block` | |
| Entity | `integrateddynamics:entity` | 无字面量构造 |
| Fluid | `integrateddynamics:fluidstack` | 不是 `fluid` |
| NBT | `integrateddynamics:nbt` | |
| Ingredients | `integrateddynamics:ingredients` | 无字面量构造 |
| Recipe | `integrateddynamics:recipe` | 无字面量构造 |

列表在原生只是 `integrateddynamics:list`，元素类型不在 facade/output type 中；DSL 在 plan 类型中用 `{list:T}` 携带它。Operator 在原生只是 `integrateddynamics:operator`；DSL 在 plan 中用 `{fn:[...],result:...}` 携带签名。计划/服务端不得把 `integrateddynamics:list` 或 `integrateddynamics:operator` 当作完整 DSL 类型。

### 5.3 Plan 类型 JSON（wire）

```text
Type = "Boolean" | "Integer" | "Long" | "Double" | "String"
     | "Item" | "Block" | "Entity" | "Fluid" | "NBT"
     | "Ingredients" | "Recipe"
     | {"list": Type}
     | {"fn": [Type, ...], "result": Type}
```

- 标量是 JSON 字符串；列表是单键对象；Fn 是 `fn` 参数数组 + `result`。`{"fn":[],"result":T}` 是零参 Fn。
- 没有 `Any`、类型变量、可空标记或隐式数值提升。类型深度上限 16（节点计数）。
- 表面语法 `Fn(Item) -> Boolean` 在 plan 中为 `{"fn":["Item"],"result":"Boolean"}`。

### 5.4 数字字面量与转换

| 字面量 | 类型 | 规则 |
|---|---|---|
| `64`、`-1` | Integer | 32 位有符号；超出报错 |
| `64L`、`-1L` | Long | 64 位有符号；固定大写 L |
| `1.0`、`1e3` | Double | 必须含小数点或指数；必须是有限值 |

- 负整数字面量按整体带符号范围检查；允许 `-2147483648`、`-9223372036854775808L`。
- 不做隐式数值提升/转换；`input energy : Long; energy < 1000` 是类型错误，写 `1000L` 或显式转换。
- 转换：`toInteger`、`toLong`、`toDouble`；运行结果保留原生 cast 行为，不宣称无损。
- 同类型算术才接受；Integer `/` 保留原生整数除法；溢出/除零/浮点边界保留原生行为。
- 数字 category 是原生内部约束，不是 DSL 的通配类型。

### 5.5 类型推断规则

- input 必须有具体类型；binding 类型由服务端 describe 给出，源码注解必须**完全等于**该类型，否则 `E_INPUT_BINDING`。
- let 可注解也可推断；注解是检查合同，不是强制转换。
- 列表元素、if 两分支、比较/`++` 两侧必须得到兼容的**具体同类型**；不做数值提升。
- 内建原语可有内部多态（如 `eq` 两参同类型、`map` 的 A→B）；每个语法出现可新鲜特化，但用户 `let` 是单态共享，不做 Hindley–Milner let-generalization。一个 let 不能一处当 Integer 比较器、另一处当 Item 比较器。
- 未确定的 Fn 参数/列表元素/输出类型必须由上下文、注解或服务端输入合同确定，否则 `E_UNRESOLVED_TYPE`；空列表尤其需要上下文。
- 不能把未知签名的既有 Operator/Proxy/Delay 注解成任意 Fn；服务端静态证明失败即拒绝。
- 编译期类型通过不保证外部输入永远不变；Proxy 目标变更、变量存储卡替换、运行期列表/操作错误仍由原生报告。

示例：

```haskell
let sameItem : Fn(Item, Item) -> Boolean = eq;
let nothing : [Item] = [];
output result : Boolean = sameItem emptyItem emptyItem;
```

没有上下文的 `let nothing = [];` 或直接输出未特化的 `eq` 拒绝；不生成含糊的“任意类型卡”。

### 5.6 有限性（独立于类型）

每个 input 和可产生列表的节点额外跟踪 `finite` / `infinite` / `unknown`；plan 只对 imports 携带 `finite` 字段，节点有限性由编译器/服务端静态规则重算：

- 标量/单值：`finite`。
- 列表字面量、具体 materialized list、Aspect 列表（表中）: `finite`。
- `iterate`/`list_lazybuilt`：`infinite`。
- `map`、`filter`、`append`、`slice` 等按 §8 规则继承/收窄。
- Proxy/Delay 的列表：有限（Delay 队列容量有限），但元素类型仍需证明。
- `unknown` 只来自无法静态证明的外部列表；不能用来绕过无限消费者检查。

## 6. 表达式、字面量与值构造

### 6.1 优先级与结合性

| 从高到低 | 语法 | 结合性 |
|---|---|---|
| 1 | 函数应用 | 左 |
| 2 | 前缀负号 | 右 |
| 3 | `* / %` | 左 |
| 4 | `+ -` | 左 |
| 5 | `++` | 右 |
| 6 | `>>` | 右 |
| 7 | `== != < <= > >=` | 不可串联 |
| 8 | `&&` | 右 |
| 9 | `||` | 右 |
| 10 | `if ... then ... else ...` | 完整条件表达式 |

`eq "Minecraft"` 先于 `>>` 绑定；`a -- b` 是注释问题，减负数写 `a - (-b)`；`not x && y` 先应用 `not`。

### 6.2 字面量/值构造

| 语法 | 类型 | 意义 | 服务端构造 |
|---|---|---|---|
| `true` / `false` | Boolean | 明确布尔 | `ValueBoolean` |
| `"text"` | String | 字符串 | `ValueString` |
| `item "ns:id"` | Item | 该物品的基本原型，数量 1，无 NBT | `new ItemStack(item)`；拒绝 air/空栈 |
| `block "ns:id"` | Block | 该方块默认状态 | `block.defaultBlockState()` |
| `fluid "ns:id" 1000` | Fluid | 流体原型与正 Integer 数量，单位 mB | `new FluidStack(fluid, amount)` |
| `nbt "{Damage:10}"` | NBT | 目标 SNBT 解析的静态 tag；`""` 为空 NBT | `TagParser`；空串为空 |
| `emptyItem` | Item | `ItemStack.EMPTY`，不是空白卡 | `ValueObjectTypeItemStack.ValueItemStack.of(ItemStack.EMPTY)` |
| `emptyFluid` | Fluid | `FluidStack.EMPTY` | `ValueFluidStack.of(FluidStack.EMPTY)` |
| `[e1, e2, ...]` | `[T]` | 有序同类型列表；元素可为动态表达式 | materialized list / append 链 |
| `op "ns:registered_id"` | 具体 Fn | 查询原生 Operator 注册，不执行 | `ValueOperator.of(operator)` |

- 资源 ID 必须显式带命名空间、格式合法且存在于目标注册表；`item` 必须得到非空 `ItemStack`（`minecraft:air` 拒绝）。`block` 得到默认 block state；`fluid` 数量必须是正 Integer 字面量；不允许动态数量（动态数量用 `amount` 类原语）。
- 不接受原始 ItemStack JSON/NBT、卡片 NBT、任意 ItemStack 组件、动态资源 ID 或隐式默认命名空间。
- Entity、Ingredients、Recipe 不提供猜测式字符串构造器；只能通过 input/已有原语或固定合同获得。
- 没有通用 `null`。空 Item/Fluid、空列表、缺失引用、求值失败是不同概念；不使用 `null` 作为“任意空值”。
- 字符串/NBT 字面量文本（UTF-8 bytes）上限 8192；列表字面量长度上限 256；这些是编译/传输限制，不是原语语义。

### 6.3 `op` 的合同

`op "ns:id"` 只接受注册表中存在、且服务端固定合同可静态证明签名的 Operator：

- 其声明签名（`getInputTypes()/getOutputType()`）不含 `Any`/`Number`/`Named`/`Nullable` 等类别，或该 ID 在固定 typed 表中；否则 `E_NATIVE_CONTRACT`。
- 在 plan 中，`op` 节点是 `native`，必须给出具体 Fn 类型，例如：
  ```json
  {"id":0,"kind":"native","operator":"integrateddynamics:itemstack_mod",
   "type":{"fn":["Item"],"result":"String"}}
  ```
- 不允许用显示名/Global name/方法名替代注册 ID；不从在线书名称猜注册名。
- 高阶/组合 operator（apply/pipe/pipe2/flip/conjunction/disjunction/negation/map/filter/reduce 等）不能作为 `native` 常量；它们只由编译器生成的 `call` 表达。原因是原生声明只给 OPERATOR/ANY，真实 Fn 签名不在 registry 中；动态参数也不能安全地存进 Operator value 卡。

## 7. 函数值、应用、部分应用与内建库

### 7.1 应用语义

- `f x y` 左结合；参数少于/等于/多于原生 required 时：
  - 刚好：直接生成/求值 call。
  - 少于：产生剩余参数的 Fn 值（原生 `CurriedOperator` 语义），DSL 编译为 `operator_apply` 链，而不是“绑定快照的 Operator value 卡”。
  - 多于：只有当前结果的具体类型是 Fn 时才继续；否则 `E_NOT_CALLABLE`。DSL 可在静态类型上拆成多次 apply/call。
- `lt 64` 表示“64 小于下一参数”，不是“下一参数小于64”；要后者写 `flip lt 64`。
- 不在客户端把函数应用到世界读取值；所有动态求值留给 ID。
- 部分应用节点的静态结果：
  - `Fn(P1..Pn)->R` 应用 1 个类型为 P1 的参数 → `Fn(P2..Pn)->R`（n=1 时直接 R）。
  - 对 0 参 Fn 使用 `operator_apply` 会过应用并出错；0 参用 `operator_apply0`。
- Operator value 快照警告：原生 `CurriedOperator.Serializer` 会对已绑定变量调用 `getValue()`；因此任何捕获动态输入的 Fn 都不能写成 `value` 卡。DSL 的 `native` 仅用于无绑定参数的原生 operator 常量；动态 Fn 一律 `call`。

### 7.2 固定便利名称与原生映射

类型中的 A/B/C/N 是编译期元变量，必须在每个出现点具体化（plan 中不能出现）。N 限 Integer/Long/Double；表中“同类型”是 DSL 规则，原生 category 不用于放宽。

| 源名称/语法 | DSL 类型（特化后） | 原生注册名 | 注意 |
|---|---|---|---|
| `add/sub/mul/div/mod` 与 `+ - * / %` | `Fn(N,N)->N` | `arithmetic_addition/subtraction/multiplication/division/modulus` | 只同类型；不自动提升 |
| `min/max` | `Fn(N,N)->N` | `arithmetic_minimum/arithmetic_maximum` | |
| `increment/decrement` | `Fn(N)->N` | `arithmetic_increment/arithmetic_decrement` | |
| `eq/ne`、`==/!=` | `Fn(A,A)->Boolean` | `relational_equals/relational_notequals` | 同具体类型；`ne` 是组合 not-equals |
| `lt/le/gt/ge`、对应比较 | `Fn(N,N)->Boolean` | `relational_lt/relational_le/relational_gt/relational_ge` | `le/ge` 是 `equals || lt/gt` 组合，数字等价 |
| `not`、`&&/||` | Boolean | `logical_not/logical_and/logical_or` | `&&/||` 对 Boolean 变量短路 |
| `if` | Bool, A, A → A | `general_choice` | 仅求值选中分支；两分支必须同类型 |
| `toInteger/toLong/toDouble` | 数字 → 具体 | `number_cast_integer/number_cast_long/number_cast_double` | 原生 cast 可能丢精度 |
| `++` | String×String→String 或 `[A]×[A]→[A]` | `string_concat` / `list_concat` | 由已确定类型选择；不混合 |
| `length` | `Fn([A])->Integer` | `list_length` | 无限 lazy 列表返回 `Integer.MAX_VALUE` |
| `isEmpty` | `Fn([A])->Boolean` | `list_empty` | 无限 lazy 列表返回 false |
| `get` | `Fn([A],Integer)->A` | `list_get` | 越界/负索引抛错 |
| `getOr` | `Fn([A],Integer,A)->A` | `list_get_or_default` | 越界才读默认值；默认类型同 A |
| `append` | `Fn([A],A)->[A]` | `list_append` | 元素须与列表元素类型对应 |
| `slice` | `Fn([A],Integer,Integer)->[A]` | `list_slice` | `from < to`、非负；空区间抛错 |
| `map` | `Fn(Fn(A)->B,[A])->[B]` | `operator_map` | f 必须单参；输出元素类型 B |
| `filter` | `Fn(Fn(A)->Boolean,[A])->[A]` | `operator_filter` | 谓词单参；已知 infinite 拒绝 |
| `reduce f xs seed` | f:`Fn(A,B)->A`，xs:`[B]`，seed:A → A | `operator_reduce` | 原生顺序 op,list,seed；f 恰 2 参；infinite 抛错 |
| `iterate seed f` | seed:A，f:`Fn(A)->A` → [A] | `list_lazybuilt` | 原生顺序 seed,op；infinite |
| `identity` | `Fn(A)->A` | `general_identity` | |
| `constant x` | x:X → `Fn(A)->X` | `general_constant` + apply | 剩余参数类型 A 由上下文特化；忽略第二参 |
| `flip` | `Fn(A,B,C...)->R` → `Fn(B,A,C...)->R` | `operator_flip` | 至少 2 参；>2 时其余不变 |
| `>>` | `Fn(A)->B`、`Fn(B)->C` → `Fn(A)->C` | `operator_pipe` | DSL 只允许单参 f/g；原生 Pipe 支持 g 额外输入，不暴露到源语法 |
| `pipe2 f g h` | f:`Fn(A)->B`，g:`Fn(A)->C`，h:`Fn(B,C)->D` → `Fn(A)->D` | `operator_pipe2` | f/g 单参、h 恰 2 参；f/g 会先求值 |
| `allOf/anyOf` | Pred(A)、Pred(A) → Pred(A) | `operator_conjunction/operator_disjunction` | 原生谓词必须单参且输出 Boolean；短路 |
| `negatePred` | Pred(A) → Pred(A) | `operator_negation` | 原生谓词单参 |
| `call0` | `Fn()->A` → A | `operator_apply0` | 零参 Operator |
| `itemCount` | `Fn([Item],Item)->Integer` | `itemstack_itemlistcount` | 同 item+NBT 累加 count；空查询 0 |
| `itemMod` | `Fn(Item)->String` | `itemstack_mod` | Mod 显示名，不是 namespace |
| `itemIsStackable` | `Fn(Item)->Boolean` | `itemstack_stackable` | 空栈 false |
| `itemIsEnchantable` | `Fn(Item)->Boolean` | `itemstack_enchantable` | 空栈 false |

补充：

- `eq` 对数字原生会把数字类型归为 NUMBER 后比较对应；DSL 只允许同具体类型，避免 Integer/Long 混用。
- `le/ge` 是组合 operator；语义是 `eq || lt/gt`，不是单独的原生比较函数；对数字等价。
- `map`/`filter` 的原生声明输入是 [OPERATOR,LIST]，输出 LIST，静态元素类型不保留；plan 类型必须由 DSL 规则给出。
- `pipe2` 的原生实现先用 f、g 对同一输入求值，再把两个结果喂给 h；不是惰性 `allOf`，也没有第二谓词短路。
- `allOf/anyOf` 只接受单参 Boolean predicate；不是 Boolean 值的 `&&/||`。
- `constant`、`flip`、`>>`、`pipe2` 的函数参数必须是 Operator 值，不能是普通值；`constant x` 里的 x 可以是任意具体值（包括函数）。
- 没有用户定义泛型函数、lambda、任意递归；函数值来自原生 operator、部分应用、组合、input。

### 7.3 Operator value 与计划表示

- `native` 节点表示**无绑定参数**的原生 operator 常量；它生成一张 `_type=integrateddynamics:valuetype`、`typeName=integrateddynamics:operator` 的卡，值为 operator 序列化（普通内置为 StringTag 唯一名）。
- `call` 节点表示对一个注册 operator 的完整调用；它生成 `_type=integrateddynamics:operator` 的卡，`variableIds` 按 args 顺序引用依赖卡/既有变量。
- 部分应用/动态 Fn：`call operator_apply`（或 `operator_apply0`）的 args 引用一个 Fn 卡和一个值卡；base Fn 若是原生 operator，则由前序 `native` 节点提供。
- 不能把 `pipe`/`conjunction`/`flip` 的结果保存成 `value` Operator 卡；这会把动态捕获值快照。需要函数输出时，输出指向 `call` 节点。

## 8. 求值、有限性与状态

### 8.1 构造而非求值

- 编译器生成定义，不取得输入当前值后替换。`let count = itemCount stock iron;` 不在客户端算一次库存，也不生成静态 count 快照。
- `if` 映射 `general_choice`；`&&/||` 映射原生逻辑 operator，保留短路。
- 未选分支仍须有合法引用、完整类型和可构造定义；短路不允许引用不存在的输入。
- 除字面量解析/编码外，不做计算式常量折叠、函数提前执行或自动物化。
- 不做代数重写改变原生错误/求值顺序；不把图求值等同于完整 Haskell call-by-need。
- `false && (1 / 0 == 0)` 类型检查通过；编译器不主动执行除法；运行期是否触发除法由 AND 短路与真实请求路径决定。
- 条件/列表访问/函数组合产生的错误仍按 ID 实际合同报告；工具报告节点，不吞异常返回假值。

### 8.2 有限性规则

| 表达式/节点 | 有限性 |
|---|---|
| 标量/单值 | finite |
| 列表字面量（全静态或动态 append） | 取决于起点/追加；字面量 finite |
| Aspect 列表（固定表） | finite |
| 具体 materialized list / 变量常量列表 | 由 raw proxy `isInfinite()`（纯 metadata）或已知构造 finite |
| `iterate` / `list_lazybuilt` | infinite |
| `map f xs` | 继承 xs（即使原生 mapped proxy 的 isInfinite 丢标记） |
| `filter p xs` | xs 为 infinite 时编译错误；否则继承 xs（unknown 保持 unknown） |
| `append xs x` | 继承 xs |
| `concat xs ys` | 任一 infinite → infinite；否则 finite，除非有 unknown |
| `slice xs from to` | 边界为有限整数且 from<to → finite（即使 xs infinite；原生 slice 长度被 to 截断） |
| `getOr`/`get`/`length`/`isEmpty` | 结果标量，不传播列表有限性 |
| `if c then a else b`（列表） | 两分支都 finite → finite；任一 infinite → infinite；否则 unknown |
| Proxy 目标列表 | 跟随目标静态类型 |
| Delay 历史 | finite（队列容量有限），元素类型来自输入槽 |

有限性消费者：

- `filter`、`reduce`、`itemCount` 原生在 `isInfinite()` 为 true 时抛错；但 `map/slice/append` 的 proxy 不传播 `isInfinite`，所以原生检查有缺口。
- DSL 编译器对**静态 infinite** 输入报 `E_INFINITE_CONSUMER`（`filter`/`reduce`/`itemCount`）。
- `unknown` 在预览/计划校验中给 `W_UNKNOWN_FINITENESS` warning，plan 仍可生成；不把 unknown 伪造为 finite。运行期原生 `filter/reduce/itemCount` 可能抛错；若底层是 `map` 产生的 lazy infinite，原生的 `isInfinite()` 传播缺口可能导致长时间遍历，需在 UI/文档提示这一风险，不承诺服务端能证明终止。
- `length` 对 infinite 不做拒绝：原生返回 `Integer.MAX_VALUE`；`isEmpty` 返回 false。不要虚构“length 抛错”。
- `slice` 字面量 `from >= to` 在编译期报 `E_LITERAL_RANGE`；动态边界只能在运行期由原生抛错。
- 不把原生 API 的 `getLength()==Integer.MAX_VALUE` 当作数学无穷；不重新定义原生 length/isEmpty。

### 8.3 状态与历史

- Delayer 历史通过 input 绑定提供，不在即时定义图中加入指向输出的逆向边。部署/制卡不把输出接回历史槽；用户手动接线。
- Proxy 输入仍是动态间接引用，不自动视为时间边界；循环 Proxy/变量依赖由服务端静态图检测或原生运行期错误报告。
- DSL 不负责 Delay 容量、采样间隔、更新优先级、持久化或重置；不承诺同 tick 精确读写顺序。
- DSL 没有可变赋值/状态；所有“状态”来自原生读取器、Proxy、Delay、网络/世界。

## 9. 输入环境与静态类型证明（describe）

`id_describe_inputs` 只做**静态**解析，不调用 `variable.getValue()`，不读库存值，不执行 Operator/Aspect，也不用世界状态求类型。输入类型来源仅四类：

1. 常量 value facade 的 metadata；
2. 固定 Aspect 表；
3. Operator call facade 的固定签名规则 + 递归输入 facade；
4. Proxy/Delay 的网络对象引用图。

### 9.1 Binding 解析

- `variable{id}`：
  - 在会话授权网络的 `IPartNetwork` 中 `hasVariableFacade(id)`；否则 `E_INPUT_BINDING`。
  - `getVariableFacade(id)`；`isValid()` 否则报错。
  - 按 §9.2 静态推断类型。
- `aspect{part_id, aspect}`：
  - `Aspects.REGISTRY.getAspect(new ResourceLocation(aspect))`；必须非空且 `instanceof IAspectRead`。
  - `partNetwork.getPartType(partId)` 必须存在；part 的 `getUniqueName()` 必须与固定表的 part id 一致；`partNetwork.hasPartVariable(partId, (IAspectRead) aspect)` 必须为 true。
  - 标量 aspect：`aspect.getValueType()` 即具体类型；列表中：查固定表；Operator 中：查固定表，否则拒绝。
  - 不在表中/part 不匹配/未知 → `E_INPUT_BINDING`。
- 源输入注解与 describe 类型必须完全相等；describe 是权威。bindings 多出/缺少源 input 都报错。

### 9.2 facade 静态类型算法

```text
staticType(facade, visited) -> Type:
  if facade is IValueTypeVariableFacade:
      t = facade.getValueType()
      if t == integrateddynamics:list:
          raw = facade.getValue().getRawValue()
          return [staticType(raw.getValueType())]
      if t == integrateddynamics:operator:
          op = facade.getValue().getRawValue()
          return fn(op.getInputTypes(), op.getOutputType())
      return mapConcrete(t)
  if facade is IAspectVariableFacade:
      return aspectType(facade.getPartId(), facade.getAspect())
  if facade is IDelayVariableFacade:
      return delayType(facade.getProxyId(), visited)
  if facade is IProxyVariableFacade:
      return proxyType(facade.getProxyId(), visited)
  if facade is IOperatorVariableFacade:
      return operatorType(facade.getOperator(), facade.getVariableIds(), visited)
  fail E_INPUT_BINDING
```

- 只对**常量 value facade** 调用 `getValue()`（已反序列化，纯 metadata）；不对 Operator 表达式/Proxy/Delay/Aspect 调 `getVariable()/getValue()`。
- `IValueTypeVariableFacade` 的 `getValueType()` 对 list 是 `integrateddynamics:list`，必须用 raw proxy 的元素类型；对 operator 是 `integrateddynamics:operator`，必须用 operator 的静态签名。空列表若是 `CATEGORY_ANY` 默认值，元素类型不可证明 → 拒绝。
- 嵌套列表：后端固定合同只把 raw proxy 的元素类型映射到具体标量；若元素类型仍是 `integrateddynamics:list`/operator，静态类型不可证明 → `E_INPUT_BINDING`。新建 plan 内部可以有嵌套结构，但既有变量绑定不推断 `[[T]]`。
- `IOperatorVariableFacade.getOutputType()` 是声明 output，不是实际类型；apply/list_get/map/pipe 等会返回 ANY/LIST/OPERATOR，必须走规则表。
- 递归 visited 使用变量 id/proxy id/part id；循环 → `E_INPUT_BINDING`（外部图环），不是源文件即时环。后端静态 facade/Proxy/Delay 链深度上限 32（实现安全上限），超过同样报 `E_INPUT_BINDING`。
- 无法映射的具体类型（扩展）→ `E_INPUT_BINDING`，除非固定合同表明确。

### 9.3 Aspect 固定表

| aspect unique name | part id | 类型 | finite | 证据 |
|---|---|---|---|---|
| `integrateddynamics:list_inventory_itemstacks` | `integrateddynamics:inventory_reader` | `[Item]` | finite | `ValueTypeListProxyPositionedInventory` 构造 `OBJECT_ITEMSTACK` |
| `integrateddynamics:list_fluid_fluidstacks` | `integrateddynamics:fluid_reader` | `[Fluid]` | finite | `PositionedTankFluidStacks` |
| `integrateddynamics:list_fluid_capacities` | `integrateddynamics:fluid_reader` | `[Integer]` | finite | `PositionedTankCapacities` |
| `integrateddynamics:list_entity_entities` | `integrateddynamics:entity_reader` | `[Entity]` | finite | `ValueList.ofList(OBJECT_ENTITY,...)` |
| `integrateddynamics:list_entity_players` | `integrateddynamics:entity_reader` | `[Entity]` | finite | 同上 |
| `integrateddynamics:list_extradimensional_players` | `integrateddynamics:extradimensional_reader` | `[Entity]` | finite | 同上 |
| `integrateddynamics:list_world_players` | `integrateddynamics:world_reader` | `[Entity]` | finite | 同上 |
| `integrateddynamics:list_recipehandler_recipes` | `integrateddynamics:machine_reader` | `[Recipe]` | finite | `PositionedRecipes` 构造 `OBJECT_RECIPE` |

- 所有列表 aspect 的 `getValueType()` 都是 `integrateddynamics:list`；元素类型来自构造函数，不能从 registry 元数据读。
- 没有 energy list aspect；能源标量是 `integer_fe_amount`/`integer_fe_capacity`（machine_reader）与 `integer_network_energy_stored/max/consumptionrate/batterycount`（network_reader），可直接 `getValueType()`。
- 标量 aspect 如 `integer_inventory_count/slots/slotsfilled`、`boolean_inventory_full/empty/nonempty/applicable`、`double_inventory_fillratio`、`integer_fluid_amount/amounttotal/capacity/capacitytotal/tanks`、`double_fluid_fillratio` 可直接用 `getValueType()`。
- 未知/扩展 aspect：不在固定表 → `E_INPUT_BINDING`；不能靠源注解补类型。若扩展提供自己的固定合同，需显式扩展表。
- `valueType` 是 category（`any/number/named/nullable`）或 `OPERATOR` 的 aspect 也不在标量可证明集合；除非固定表明确，否则 `E_INPUT_BINDING`。

### 9.4 Operator 静态规则表

下列规则同时用于计划校验和既有变量静态推断。`type` 断言必须与重算结果完全相等（同类型，不做隐式提升）。

| operator id | 输入规则 | 输出 |
|---|---|---|
| `general_choice` | [Boolean, A, A]；两分支同类型 | A（then 的类型） |
| `general_identity` | [A] | A |
| `general_constant` | [A, B] | A |
| `operator_apply` | [Fn(P1..Pn)->R, T]；T=P1，n≥1 | n=1 时 R，否则 `Fn(P2..Pn)->R` |
| `operator_apply0` | [Fn()->R] | R |
| `operator_map` | [Fn(A)->B, [A]] | [B] |
| `operator_filter` | [Fn(A)->Boolean, [A]] | [A] |
| `operator_conjunction` / `operator_disjunction` | [Pred(A), Pred(A)]；Pred(A)=Fn(A)->Boolean | Pred(A) |
| `operator_negation` | [Pred(A)] | Pred(A) |
| `operator_pipe` | [Fn(A)->B, Fn(B,C...)->R] | `Fn(A,C...)->R` |
| `operator_pipe2` | [Fn(A)->B, Fn(A)->C, Fn(B,C,D...)->R] | `Fn(A,D...)->R` |
| `operator_flip` | [Fn(A,B,C...)->R]，≥2 参 | `Fn(B,A,C...)->R` |
| `operator_reduce` | [Fn(A,B)->A, [B], A]；f 恰 2 参 | A |
| `list_get` | [[A], Integer] | A |
| `list_get_or_default` | [[A], Integer, A] | A |
| `list_append` | [[A], A] | [A] |
| `list_concat` | [[A], [A]] | [A] |
| `list_slice` | [[A], Integer, Integer] | [A] |
| `list_lazybuilt` | [A, Fn(A)->A] | [A]（infinite） |
| `list_length` | [[A]] | Integer |
| `list_empty` | [[A]] | Boolean |
| `itemstack_itemlistcount` | [[Item], Item] | Integer |
| `itemstack_mod` | [Item] | String |
| `itemstack_stackable` / `itemstack_enchantable` | [Item] | Boolean |
| `relational_equals` / `relational_notequals` | [A, A] | Boolean |
| `relational_gt` / `relational_lt` / `relational_ge` / `relational_le` | [N, N] | Boolean |
| `arithmetic_*` | [N, N]（同具体 N） | N |
| `arithmetic_increment` / `arithmetic_decrement` | [N] | N |
| `number_cast_*` | [N] | 具体 Integer/Long/Double |
| `string_concat` | [String, String] | String |
| `logical_*` | Boolean | Boolean |
| `general_choice` 等见上 | | |

规则细节：

- `operator_apply` 的 first input 类型必须是 Fn；fn 不可为 0 参；arg 类型必须等于 P1。若 fn 是 `native`/call 的静态 Fn，直接用；若 fn 是 Proxy/Delay/未知 → 递归静态图；无法证明 → `E_INPUT_BINDING`。
- `operator_pipe`/`pipe2` 的 f/g 必须单参（DSL 源语法限制）；h 的额外参数保留在结果 Fn 中。`pipe2` 的 f/g 输出必须分别对应 h 的第 1/2 参数；f/g 都会先求值。
- `operator_flip` 至少 2 参；交换前两个，其余保持。
- `operator_map` f 必须单参；`operator_filter` 谓词必须单参 Boolean；`reduce` f 必须恰 2 参；`iterate` 的 f 输出必须与 seed 同类型。
- 不在表内的 operator：
  - 若 `getInputTypes()` 与 `getOutputType()` 全是具体类型（无 `any/number/named/nullable` 类别，且无未参数化 `list/operator`）→ 可用声明 output；
  - 否则 `E_INPUT_BINDING`/`E_NATIVE_CONTRACT`，不得靠 source 注解或 `validateTypes` 放宽。
- CurriedOperator（第三方卡）：`getInputTypes()` 是剩余参数、`getOutputType()` 是 base output，可静态使用；但 Applied values 可能是原生序列化快照，语义上不是动态引用，文档/告警需说明。DSL 自己不会生成这种 operator 字段。
- 递归输入 id 必须已在网络 variable cache；缺失/无效/环 → `E_INPUT_BINDING`。

### 9.5 Proxy / Delay

Proxy：

1. `getProxyId()`；`partNetwork.getProxy(proxyId)` 返回 `DimPos`，否则 `E_INPUT_BINDING`。
2. 取 `BlockEntityProxy`；读库存 `SLOT_READ=0` 的卡，`registry.handle(ValueDeseralizationContext.of(level), stack)`。
3. 对目标 facade 递归 `staticType`；visited proxyId 防环。
4. 不调用 `ProxyVariableFacade.getVariable()/getType()`（会走 target getVariable/getType，可能求值）。

Delay：

1. `getProxyId()`；`partNetwork.getProxy(proxyId)` → `BlockEntityDelay`。
2. `BlockEntityDelay.getVariable(...).getType()` 恒为 `integrateddynamics:list`；`getOutputType()` 仍是 ANY。
3. 元素类型 = Delay 读槽 `SLOT_READ=0` 卡片的 `staticType`；ticker 用 `value.getType()` 构造历史 `ValueList.ofList(...)`。
4. 空槽/invalid/未知 → 对 `[T]` 绑定 `E_INPUT_BINDING`；不能把 Delay 当任意类型。历史有限性 finite（队列容量有限）。
5. 递归防环；Delay 的输入槽卡若就是 Delay 自身/链上节点 → 拒绝。

两种卡都只作为 `variable{id}` 绑定；payload `partId` 是 proxyId/delayId，不是变量 ID。变量 ID 是卡的 `_id`。

## 10. 编译环境、计划与诊断

### 10.1 CompileEnvironment

由 `id_describe_inputs` 返回的不可变 JSON：

```json
{
  "ok": true,
  "contract": "id-1.31.3-dsl-1",
  "inputs": {
    "stock": {
      "binding": {"kind":"aspect","part_id":7,"aspect":"integrateddynamics:list_inventory_itemstacks"},
      "type": {"list":"Item"},
      "finite": "finite"
    },
    "limit": {
      "binding": {"kind":"variable","id":42},
      "type": "Integer",
      "finite": "finite"
    }
  }
}
```

- `contract` 固定；未知/不匹配 → `E_TARGET_CONTRACT`。
- 每个源 input 必须恰好一项；source 中多余/缺失都报错。额外 inputs 不静默忽略。
- 编译器不访问 MC/ID 类；只读 JSON 描述。错误响应不带 `inputs`，带 `code/message`。

### 10.2 编译步骤

1. 词法/语法解析为带 UTF-16 range 的 AST；检查声明、分号、token。
2. 收集 input/let/output；解析前向引用；检查即时依赖环。
3. 用 describe 类型 + 内建签名 + 注解生成具体类型约束；确定数值/列表/Fn 特化；检查空列表/未解析类型。
4. 检查 bindings 与 inputs 完全匹配；检查类型与 finite。
5. 跟踪有限性；检查已知 infinite 消费者；字面量范围/资源/SNBT 在服务端最终校验（客户端可预检 registry 视图，但不作为权威）。
6. 从 outputs 追溯可达节点；去掉纯别名；按确定顺序生成 plan 节点/refs。
7. 生成 `outputs` 映射与 sourceMap（客户端）；不分配 ID、不写卡。
8. 同一源+同一环境应产生稳定节点顺序/编号；不因哈希表遍历改变。

不做任意公共子表达式消除；同一 let 的多次引用共享一个定义；相同文本的两个原生调用不自动合并。字面量/operator 常量可按类型+规范值共享；共享必须保持确定性。

### 10.3 Plan wire（冻结）

```json
{
  "version": 1,
  "contract": "id-1.31.3-dsl-1",
  "imports": {
    "<inputName>": {
      "binding": {"kind":"variable","id":42}
                | {"kind":"aspect","part_id":7,"aspect":"namespace:id"},
      "type": "<type-json>",
      "finite": "finite" | "infinite" | "unknown"
    }
  },
  "nodes": [
    {"id":0,"kind":"value","type":"Integer","value":64},
    {"id":1,"kind":"native","type":{"fn":["Item"],"result":"String"},
     "operator":"integrateddynamics:itemstack_mod"},
    {"id":2,"kind":"call","type":"Boolean",
     "operator":"integrateddynamics:relational_equals",
     "args":[{"node":0},{"input":"limit"}]}
  ],
  "outputs": {
    "<port>": {"ref":{"node":2},"type":"Boolean"}
  }
}
```

规则：

- `version=1`，`contract="id-1.31.3-dsl-1"`。`imports` 的 key 必须是源 input 名且不重复；只包含被可达 nodes/outputs 实际引用的输入。源文件中未使用的 input 仍必须在编译期有 binding 并通过 describe，但不进入 plan，也不创建 Aspect 卡。
- `nodes` 数组 index 即 node id；`id` 必须等于 index 且连续。依赖只能引用**更早**的 node 或 imports；无环。
- `Reference` 恰好是 `{"node":integer}` 或 `{"input":"name"}` 之一，不能同时/额外字段。
- 每个 node 有 `{id,kind,type}`；`kind` 只能是 `value`/`native`/`call`。
- `value`：
  - `type` 不能是 Fn；`value` JSON 按类型编码：
    - Boolean/Integer/Long/Double/String：raw JSON 标量；
    - Item/Block：`{"resource":"namespace:id"}`；空 Item：`null`；
    - Fluid：`{"resource":"namespace:id","amount":1000}`；空 Fluid：`null`；
    - NBT：SNBT 字符串；
    - List：JSON 数组，元素按元素类型递归编码（支持嵌套具体列表）；空数组使用 node type 的元素类型；
    - 不允许 Operator/Fn 值、原始 ItemStack/NBT 卡、未知字段。
  - 列表元素只能是上述字面量编码，不能是 node ref/表达式；动态列表用 `call list_append` 构建。
- `native`：
  - `operator` 是实际注册 ID；`type` 必须是该 operator 的合法具体特化；
  - 仅用于无绑定参数的原生 operator 常量；高阶组合 operator 不允许作为 native。
- `call`：
  - `operator` 是实际注册 ID；`args` 是该 operator 的完整输入（除 `operator_apply`/`apply0` 的特殊规则外，长度必须等于原生 required）；
  - `operator_apply` 固定 2 args：`[fn, arg]`；`operator_apply0` 固定 1 arg：`[fn]`；
  - `type` 是调用结果的具体类型，由规则表重算；不能是 Any/变量；
  - 普通 operator 不允许少传/多传；部分应用必须显式 `operator_apply` 链。
- `outputs`：每个端口 `{ref,type}`；ref 可指向 input（无新卡）或 node；type 与 ref 类型一致。
- 计划不包含源码、sourceMap、卡片 NBT、真实 ID、玩家/网络坐标；网络/授权由后端会话上下文解析。
- 计划中不能出现 `Any`、类型变量、未解析 Fn、operator/aspect 显示名、原始 SNBT 之外的任意 NBT。

### 10.4 计划上限

| 项 | 上限 |
|---|---:|
| 源文件 UTF-8 bytes | 32768 |
| 类型深度（节点数） | 16 |
| nodes | 256 |
| inputs（imports） | 64 |
| outputs | 64 |
| 单个字符串/NBT 字面量 UTF-8 bytes | 8192 |
| 列表字面量元素数 | 256 |
| binding/input 名称（后端） | 64 UTF-8 bytes |
| node id | 连续 0..n-1 |

服务端可额外限制编码后的请求大小/字符串总长，但不得放宽以上语义限制。超过 → `E_LIMIT`。

### 10.5 诊断

诊断对象至少 `{code,severity,message,range,related?}`；`range` 为 UTF-16 offset，`start/end` 半开；行列从 1 起、列按 UTF-16。服务端错误带 `phase`、`node` 或 `input`，不猜源码行列。

| code | 情况 |
|---|---|
| `E_SYNTAX` | 缺分号、非法 token、错误声明/表达式 |
| `E_UNKNOWN_NAME` / `E_DUPLICATE_NAME` | 未定义名、重名/遮蔽保留名 |
| `E_CYCLE` | 源文件即时定义循环（带引用链） |
| `E_INPUT_BINDING` | 缺/多 binding、类型不匹配、外部 facade 无法静态证明、proxy/delay 槽空/环/part 不匹配 |
| `E_TYPE` / `E_UNRESOLVED_TYPE` | 具体类型冲突/无法特化 |
| `E_NOT_CALLABLE` / `E_FN_SIGNATURE` | 非函数应用、Fn 参数/返回不匹配 |
| `E_EMPTY_LIST_TYPE` | 空列表元素类型无法确定 |
| `E_LITERAL_RANGE` / `E_LITERAL_RESOURCE` / `E_LITERAL_NBT` | 数字越界、资源不存在/非法、SNBT 非法 |
| `E_NATIVE_CONTRACT` | operator/aspect 不存在、无固定合同、native 不允许 |
| `E_UNKNOWN_TYPE` | value type 不在固定核心类型表 |
| `E_EVALUATION` | 原生/环境报告的求值类错误（不吞成假值） |
| `E_INFINITE_CONSUMER` | 已知 infinite 传给 filter/reduce/itemCount |
| `W_UNKNOWN_FINITENESS` | 无法证明 finite；warning，plan 可生成，运行期可能失败/遍历很久 |
| `W_UNUSED_BINDING` | input 未被可达 plan 使用的 warning |
| `E_UNSUPPORTED_SYNTAX` | lambda/函数定义/赋值/循环等不属于语言 |
| `E_LIMIT` | 超过 §10.4 限制 |
| `E_TARGET_CONTRACT` | describe/plan contract 不匹配 |
| `E_TARGET_CONTRACT` / `E_TYPE` / `E_NATIVE_CONTRACT` / `E_INPUT_BINDING` | 服务端 plan schema/ref/类型/operator/contract 校验失败（phase=validate） |
| `E_TARGET_CONTRACT`（phase=preflight/create） | 空白卡不足、主背包容量不足、写卡/放置失败；不是独立 E_CARD_* code |

不把“未写入”“写入失败”“原生求值失败”统称语法错误；不返回 false/空 Item 伪装成功。

## 11. 从表达式展开为卡片

### 11.1 节点映射

| 源表达式/节点 | plan/原生落点 | 卡数 |
|---|---|---|
| 标量/Item/Block/Fluid/NBT/空值字面量 | `value` → `ValueTypeVariableFacade` | 1/唯一节点 |
| 全静态列表字面量 | `value` list（具体元素类型） | 1 |
| 动态列表字面量 | `value` 具体类型空列表 + `call list_append` 链 | 1 + 元素数 |
| `native` operator 常量 / `op` | `native` → Operator value 卡 | 1 |
| 原生完整调用 | `call` → OperatorVariableFacade | 1 |
| 部分应用/动态 Fn 应用 | `call operator_apply`（base 为 native Fn） | 1 + base 卡（若尚无） |
| 0 参应用 | `call operator_apply0` | 1 + base 卡 |
| `if` | `call general_choice` | 1 |
| `&&/||` | `call logical_and/or` | 1 |
| `>>` | `call operator_pipe` | 1 + 两个 Fn 卡 |
| `pipe2` | `call operator_pipe2` | 1 + 三个 Fn 卡 |
| `flip` | `call operator_flip` | 1 + Fn 卡 |
| `constant x` | native `general_constant`（特化 Fn(A,B)->A）+ `operator_apply` | 2（可共享 base） |
| `allOf/anyOf` | `call operator_conjunction/disjunction` | 1 + 两个 Fn 卡 |
| `negatePred` | `call operator_negation` | 1 + Fn 卡 |
| `call0` | `call operator_apply0` | 1 + Fn 卡 |
| `map/filter/reduce/iterate` | 对应 native call | 1 + 函数/列表卡 |
| Aspect import | `AspectVariableFacade` | 1 |
| variable import | 既有 ID | 0 |
| output 别名/input 直出 | 不建卡/指向既有 ID | 0 |

- 每个 `value`/`native`/`call` 节点对应一张实际卡；同一节点被多处引用只建一张。
- 纯 let 别名不建卡；从 outputs 不可达的节点不建卡。
- 不把 `output` 语句本身变成卡；output 只是端口到节点/既有变量的映射。
- 静态列表字面量可以是一个 value 卡；含动态元素的列表用 append 链，不能把表达式放进 value JSON。
- 字面量节点可按具体类型和规范值共享；共享后仍只建一张卡。

### 11.2 补货示例的 plan

源见 §3.1；假设 `stock` 是 aspect import、`limit` 是既有 Integer 变量。

```json
{
  "version": 1,
  "contract": "id-1.31.3-dsl-1",
  "imports": {
    "stock": {
      "binding": {"kind":"aspect","part_id":7,
        "aspect":"integrateddynamics:list_inventory_itemstacks"},
      "type": {"list":"Item"}, "finite": "finite"
    },
    "limit": {
      "binding": {"kind":"variable","id":42},
      "type": "Integer", "finite": "finite"
    }
  },
  "nodes": [
    {"id":0,"kind":"value","type":"Item","value":{"resource":"minecraft:iron_ingot"}},
    {"id":1,"kind":"call","type":"Integer",
     "operator":"integrateddynamics:itemstack_itemlistcount",
     "args":[{"input":"stock"},{"node":0}]},
    {"id":2,"kind":"call","type":"Boolean",
     "operator":"integrateddynamics:relational_lt",
     "args":[{"node":1},{"input":"limit"}]},
    {"id":3,"kind":"value","type":"Item","value":null},
    {"id":4,"kind":"call","type":"Item",
     "operator":"integrateddynamics:general_choice",
     "args":[{"node":2},{"node":0},{"node":3}]}
  ],
  "outputs": {
    "request": {"ref":{"node":4},"type":"Item"},
    "enabled": {"ref":{"node":2},"type":"Boolean"}
  }
}
```

卡数：5 个 node + 1 个 aspect import = 6；`limit` 是既有变量，0 张。outputs 不额外建卡。若 `stock` 改为 `variable{id}`，卡数 5。实体物品数量、exporter 速率/副本、空白卡消耗与节点数不是一回事。

### 11.3 动态函数选择示例的展开

```haskell
input enabled : Boolean;
let fallback : Fn(Item) -> Boolean = constant false;
let predicate : Fn(Item) -> Boolean = if enabled then itemIsEnchantable else fallback;
output filterFn : Fn(Item) -> Boolean = predicate;
```

- `general_constant` native 节点类型特化为 `Fn(Boolean,Item)->Boolean`；
- `operator_apply` call 类型 `Fn(Item)->Boolean`；
- `itemIsEnchantable` native 节点 `Fn(Item)->Boolean`；
- `general_choice` call 类型 `Fn(Item)->Boolean`，args 为 `[enabled, itemIsEnchantable, fallback]`；
- 输出指向 choice call。无 value Operator 卡；无编译期应用。

## 12. 服务端制卡（id_create_cards）

### 12.1 处理顺序

1. 解析 `{plan}`；校验 `version/contract`、严格 schema、未知字段、上限、node id/refs/拓扑。
2. 用纯 plan 校验 helper 重算每个 node 类型、operator 规则、outputs 类型；拒绝与断言不符。
3. 解析会话授权网络；重新解析 imports：
   - variable：`hasVariableFacade` + `getVariableFacade` + 静态类型证明；类型必须等于 plan；
   - aspect：`Aspects.REGISTRY.getAspect` + `IAspectRead` + part type + `hasPartVariable` + 固定表/标量类型；类型必须等于 plan；
   - 角色/权限、网络存在性；不信任客户端 binding 之外的数据。
4. 校验 operator 注册 ID、allowlist、原生签名/规则；校验资源 ID/注册表、Item 非空、Block 默认状态、Fluid 数量、SNBT 解析、列表元素类型/长度。
5. 校验有限性消费者（filter/reduce/itemCount）：已知 infinite → 拒绝；unknown → 保留 warning/风险提示，不伪造 finite。
6. 预检空白卡与背包容量（见 §12.5）；预检失败不改动。
7. 按拓扑顺序写卡：先 aspect imports，再 nodes；每张卡分配一个真实 ID；建立 node index → actual ID 映射。
8. 返回 created/cards/outputs；不放置、不改部件。

### 12.2 ID 与写卡 API

- ID 只在服务端生成：`VariableFacadeBase.generateId()` → `IntegratedDynamics.globalCounters.getNext("variable")`。世界级、跨维度、持久、客户端同步、synchronized、0 起；失败不回滚。
- 推荐每张卡：
  1. 从主背包取一张真正空白卡，拆成 count=1；
  2. 用 `IVariableFacadeHandlerRegistry.writeVariableFacadeItem(true, oneBlank,
     handler, factory, level, player, blockState)`；
  3. 从返回 copy 解析 `_id`（`registry.handle(ctx, out).getId()`），放入主背包。
  或先构造 `new ValueTypeVariableFacade(true,...)` / `new OperatorVariableFacade(true,op,ids)` / `new AspectVariableFacade(true,partId,aspect)`，再 `registry.writeVariableFacadeItem(oneBlank, facade, handler)`；两种都必须用 native handler/registry。
- Handler：
  - `ValueTypes.REGISTRY`
  - `Operators.REGISTRY`
  - `Aspects.REGISTRY`
  - `ProxyVariableFacadeHandler.getInstance()`
  - `DelayVariableFacadeHandler.getInstance()`
- 禁止手写 `_type/_id`、禁止伪造 ID、禁止把客户端给的 ID 当新卡 ID。
- `writeVariableFacadeItem(true,...)` 对已有 `_id>-1` 的卡会复用旧 ID；只对空白卡调用。不要对 count>1 的堆写卡。
- Aspect import 必须在任何引用它的 call 之前写，因为 call 的 `variableIds` 需要其真实 ID。
- variable import 不写卡，直接映射既有 ID；outputs 指向它时报告该 ID。

### 12.3 facade payload

| 卡 | facade | payload |
|---|---|---|
| value | `new ValueTypeVariableFacade(id, valueType, value)` | `typeName` + `value` raw Tag |
| operator call | `new OperatorVariableFacade(id, operator, int[] ids)` | `operatorName` Tag + `variableIds` |
| aspect | `new AspectVariableFacade(id, partId, aspect)` | `partId` + `aspectName` |
| proxy | `new ProxyVariableFacade(id, proxyId)` | `partId`（值是 proxyId） |
| delay | `new DelayVariableFacade(id, delayId)` | `partId`（值是 delayId） |

服务端制卡只创建 value/operator/aspect 卡；proxy/delay 由玩家用原生 Proxy/Delay 方块生成，DSL 只以 variable 绑定引用它们。

### 12.4 字面量构造

| plan value | 构造 |
|---|---|
| Boolean/Integer/Long/Double/String | `ValueTypeBoolean.ValueBoolean.of` / `ValueInteger.of` / `ValueLong.of` / `ValueDouble.of` / `ValueString.of`；范围/有限性校验 |
| Item `{resource}` | `ForgeRegistries.ITEMS.getValue`；非 null 且 `new ItemStack(item)` 非空；count=1，无 NBT |
| Item `null` | `ItemStack.EMPTY` |
| Block `{resource}` | `ForgeRegistries.BLOCKS.getValue`；`defaultBlockState()` |
| Fluid `{resource,amount}` | `ForgeRegistries.FLUIDS.getValue`；`new FluidStack(fluid, amount)`，amount>0 |
| Fluid `null` | `FluidStack.EMPTY` |
| NBT SNBT | `TagParser.parseTag`；空串 → empty `ValueNbt`；失败 `E_LITERAL_NBT` |
| List `[...]` | `ValueTypeList.ValueList.ofList(elementType, List<IValue>)`；元素递归按元素类型构造；元素类型来自 node.type 的 `{list:T}`；空数组同样使用具体 T |
| Nested List | 递归 `ValueList.ofList`；类型必须匹配 |

值卡必须用 `ValueHelpers.serializeRaw`/handler 写入，不能手拼 raw Tag。

### 12.5 空白卡与容量预检

- 需要的新卡数 = `nodes.length + count(unique aspect (part_id, aspect))`；`variable` imports 不建卡；outputs 不额外建卡；重复 aspect descriptor 共享一张卡（`cards` 用首个 import 名，后续 import 映射到同一 ID）。
- 原生语义的空白卡 = 物品 `integrateddynamics:variable` 且无 `_type`/`_id`；实现策略更保守：只消耗 **tag 为空（无任何 NBT）** 的卡，任何非空 tag（自定义名/label/`_type`/`_id`）都不覆盖、不视为可用空白。只有 `_type` 或只有 `_id` 的卡是 invalid，也不是空白。
- 空白卡总数必须 ≥ 新卡数；每张新卡需要一个 count=1 的空白卡。Item 最大堆叠 64，需按张拆分。
- 放入玩家主背包（`Inventory` 主槽，不含 armor/offhand）；不进入快捷栏之外的特殊槽；不做 creative 绕过。
- 预检（后端实际算法，36 个主背包槽：9 快捷栏 + 27 背包；不含 armor/offhand）：
  - 任意非空 tag 的变量卡既不是空白也不释放槽；
  - count=1 的空白卡可在原槽原位替换，不额外占槽；
  - count=c>1 的空白堆只有在“剩余需求≥c 且空槽≥c-1”时整堆使用：原槽 + c-1 个空槽；否则最多取 `min(remaining, freeSlots)` 张，每张占一个空槽，余量留在原槽；
  - 空白总数不足或无法为所有新卡预留槽 → `E_TARGET_CONTRACT`（phase=preflight），不消耗/不分配。
- 预检通过后才从主背包抽取 count=1 空白、写卡并放入预留下的槽；中途失败时未使用的空白尝试 `restore`，已写卡/已消耗 ID 不回滚。
- 变量仓库容量 45 格/stack=1 不是制卡容量；制卡只到背包，放置由用户完成。若一次生成超过 45 张，需要多个仓库在同一网络。

### 12.6 失败语义

- 预检失败：无 ID、无卡、无空白消耗。
- 写卡开始后失败：已写入的卡/已消耗空白不保证回滚；ID 已消耗；返回 `phase`、已创建数量、`cards` 与错误 node/input；不得报成功或伪造输出 ID。
- 不对跨 Mod 库存/事件做事务承诺；不删除玩家已有卡。
- 事件：若使用 `writeVariableFacadeItem(true,...,player,blockState)`，会 post `LogicProgrammerVariableFacadeCreatedEvent`；不依赖事件语义，标签/统计失败不改变卡片有效性。
- 标签：可给卡设置原生 label（输出端口名）以便识别；不得让任何语义依赖标签；失败不应使制卡失败。

### 12.7 返回

成功：

```json
{
  "ok": true,
  "phase": "create",
  "created": 6,
  "outputs": {
    "request": {"id": 123, "type": "Item"},
    "enabled": {"id": 121, "type": "Boolean"}
  },
  "cards": [
    {"input":"stock","id":120},
    {"node":0,"id":121},
    {"node":1,"id":122},
    {"node":2,"id":123},
    {"node":3,"id":124},
    {"node":4,"id":125}
  ]
}
```

- `cards` 包含 aspect import（用 `input`）和每个新 node（用 `node`）；不包含 variable import。
- `outputs` 返回实际变量 ID 与 plan 类型；若 output 指向 input，返回既有 ID。
- 失败响应至少含 `ok:false`、`phase:"create"`、`code`、`message`、可选 `node`/`input`；若已创建部分卡，含 `created`/`cards`，明确不可回滚。
- 不返回源码/卡 NBT/任意物品 NBT。

## 13. 制卡后放置与变量存储

### 13.1 变量仓库

- 变量仓库方块 `integrateddynamics:variablestore`；45 格；每格 stack=1；只接受带变量 facade capability 的物品（所有变量卡，包括空白）。
- 仓库在 dirty/加载时刷新：解析每张卡，把 `isValid()` 的 facade 按 `_id` 放入 cache；卡不在仓库/网络时，`PartNetwork` 的 composite cache 看不到它。
- 仓库**不做类型/依赖/环校验**；类型错误只在消费者 validate/getVariable 时暴露；无效卡不会成为可解析变量（空仓可能保留一个 dummy 行为）。
- 所有被引用的卡（aspect import 卡、每个 node 卡、输出依赖）必须放入同一网络的变量仓库/变量容器；可跨多个仓库方块，但必须在同一网络。
- 顺序：写卡后可以任意顺序放入；全部放入后网络缓存刷新，引用才可解析。放一半就接输出会出现 “variable not in network”/invalid 类错误。

### 13.2 输出接线

- 制卡只产生卡；不把输出卡自动放入 exporter/writer/面板槽，也不改部件配置。
- 用户自行把输出卡接到目标部件槽、逻辑编程器、显示器、exporter 等；槽位类型由原生 validate 检查。
- `request`/`enabled` 不能被假定为可同时插入同一个普通 exporter 的两张控制卡；具体槽位/语义由原生部件决定。
- `outputs` 中的 aspect import 卡（如 `stock`）本身可能不需要接到部件槽，但必须放在变量仓库供 call 引用；aspect 的 partId 属于当前网络。

### 13.3 Delayer/Proxy

- Delay 卡由 Delay 方块输出；其输入槽（read slot 0）的卡决定历史元素类型。把 `next` 卡放入该槽形成反馈是用户手动操作；DSL 不生成反向边、不自动放置。
- 首次静态证明需要 Delay 输入槽已有一张类型已知的卡；否则 describe 失败。可先放 Boolean 常量卡完成证明，再把 `next` 换入；替换后类型仍须匹配。
- Proxy 卡引用网络中的 Proxy 方块（payload proxyId）；目标卡在 Proxy 读槽。静态 describe 沿该引用证明类型；Proxy 目标变更会使原类型失效，运行期原生报错。
- 移动 Aspect/Proxy/Delay 卡到另一个网络可能指向不同/不存在 part/proxy，不要假定卡可跨网络复用。

## 14. 诊断审查用例

以下均为规范推导，不表示已有编译器执行：

- `input energy : Long; energy < 1000;` → `E_TYPE`，提示 `1000L`/`toLong`。
- `let f = eq;` 一处当 Integer 比较器、一处当 Item 比较器 → `E_TYPE`/`E_FN_SIGNATURE`（单态 let）。
- `if true then 1 else "x"` → `E_TYPE`，即使 else 不执行。
- `let a = b; let b = a; output x = a;` → `E_CYCLE`，报告 a→b→a。
- `filter (flip lt 10) (iterate 0 increment)` → `E_INFINITE_CONSUMER`。
- `filter itemIsEnchantable stock` 且 stock 为 `[Item]` → 通过；plan `operator_filter` 类型 `[Item]`。
- `slice naturals 10 10` → `E_LITERAL_RANGE`（原生会抛错，不返回空切片）。
- `pipe2 f g h` 中 f/g 非单参或 h 非二参 → `E_FN_SIGNATURE`。
- `constant false` 无上下文输出 → `E_UNRESOLVED_TYPE`；上下文 `Fn(Item)->Boolean` 可特化。
- `op "integrateddynamics:operator_map"` 作为独立 Fn 值 → `E_NATIVE_CONTRACT`（高阶 operator 不能作为 native 常量；使用内建 `map` 生成 call）。`op "integrateddynamics:logical_and"` 的声明签名 [Boolean,Boolean]→Boolean 具体，可作为 native。
- existing variable 的 facade output 是 ANY（apply/list_get/proxy/delay）且静态图不能证明 → `E_INPUT_BINDING`，不信任源注解。
- Delay 输入槽空，绑定 `[Boolean]` → `E_INPUT_BINDING`。
- Aspect `list_inventory_itemstacks` 的 part 不是 `inventory_reader` → `E_INPUT_BINDING`。
- `item "minecraft:air"` → `E_LITERAL_RESOURCE`（空栈）。
- NBT 非法 SNBT → `E_LITERAL_NBT`。
- plan nodes 引用未来节点 → `E_TYPE`（wire）或 `E_TARGET_CONTRACT`（validate）。
- 计划 type 与重算不符 → `E_TYPE`。
- 空白卡不足/背包满 → `E_TARGET_CONTRACT`（phase=preflight），不分配 ID。
- 写卡中途异常 → `E_TARGET_CONTRACT`（phase=create）或具体 native code，报告已创建卡/ID，不承诺回滚。

## 15. 旧规范修正记录

| 旧说法 | 修正 |
|---|---|
| 服务端完整验证后才分配 ID，失败无副作用 | 分 preflight 与 allocate/write；ID 在 facade 创建时分配，失败会消耗 ID；写卡不可事务回滚 |
| 源长度/偏移都用 UTF-8 | 源限制 UTF-8 bytes 32768；AST/诊断 UTF-16 offset，行列 1 起 |
| 部分应用可保存为 Operator value 卡 | 动态部分应用必须 `operator_apply` 调用；CurriedOperator 序列化会快照已绑定值 |
| Fn/List 输入可由用户注解决定 | describe 静态证明；Proxy/Delay/List/Operator facade 的 output 不足，不能证明即拒绝 |
| map 继承有限性就够 | 原生 map/slice/append 的 `isInfinite` 不传播；DSL 自己跟踪；已知 infinite 拒绝，unknown 只警告并提示运行期风险 |
| slice 任意 [start,end) | from<to 且非负；空区间原生抛错；字面量在编译期拒绝 |
| length(iterate) 抛错 | 原生返回 `Integer.MAX_VALUE`；isEmpty 返回 false |
| 部署工具把输出接回 Delayer | 制卡/工具不自动放置/接线；用户手动 |
| 给出 Java 常量名即可映射 | 使用实际注册名（`integrateddynamics:<kind>_<name>`）；旧表混用常量名 |
| Aspect 列表元素类型可从 getValueType 得到 | `IAspectRead.getValueType()` 只是 `integrateddynamics:list`；用固定 aspect 表 |
| 变量仓库会验证类型 | 仓库只缓存 valid facade；类型错误在使用时暴露；服务端 preflight 必须独立校验 |
| `allOf` 等同 `pipe2`/`&&` | `allOf` 是 predicate 短路组合；`pipe2` 先求 f/g；`&&` 是 Boolean 短路 |
| `itemMod` 返回 namespace | 返回 Mod 显示名（minecraft→Minecraft） |
| 原生卡可手写 NBT | 必须用 facade/handler；`_type/_id` 与 payload 由 registry 写 |
| 输出语句会生成卡 | 输出是映射；只给节点/既有变量建卡，别名不建卡 |

## 16. 明确排除

- 完整 Haskell：模块、type class、ADT、模式匹配、泛型函数定义、Monad/IO。
- 用户函数源码/lambda/任意递归/Y 组合子/递归卡片环作为函数递归。
- 客户端持续计算世界逻辑、服务器解释 DSL、替代 ID 求值器。
- 部件放置/拆除、卡槽坐标、Channel/Rate/优先级设置、直接搬运物品、自动接 Delayer。
- 未固定合同的扩展 operator/aspect、显示名猜测、任意卡片 handler/NBT、原始 ItemStack 数据。
- 隐式世界读取、隐式数值提升、Any 注解、空列表猜元素类型、吞错误的假回退。
- 事务性制卡/回滚、跨 Mod 库存一致性。

这些是合同边界，不是待实现占位符。原生 Operator 值、部分应用、组合、Aspect 读取和列表足以覆盖本文示例；超出边界必须先证明原生展开与类型合同，再修改规范。

## 17. 源码证据索引

所有链接固定到 `69ea239d03828fc76b0384dcc487af391fd9e6bf`；详细行号与补充见研究笔记。

- 卡片/空白/写卡：`core/item/VariableFacadeBase.java`、`core/item/VariableFacadeHandlerRegistry.java`、`item/ItemVariable.java`、`api/item/IVariableFacadeHandlerRegistry.java`。
- ID：`IntegratedDynamics.java`、`VariableFacadeBase.generateId()`；CyclopsCore `GlobalCounters.java`（tag `1.20.1-1.20.0`）。
- 变量仓库/网络：`blockentity/BlockEntityVariablestore.java`、`capability/variablecontainer/VariableContainerDefault.java`、`core/network/PartNetwork.java`、`core/helper/NetworkHelpers.java`。
- 值序列化：`core/evaluate/variable/ValueHelpers.java`、`ValueTypeBoolean/Integer/Long/Double/String/Nbt/List/Operator.java`、`ValueTypeListProxyMaterializedFactory.java`、`ValueObjectTypeItemStack/Block/FluidStack.java`。
- Operator：`core/evaluate/operator/Operators.java`、`OperatorBase.java`、`OperatorBuilder.java`、`CompositionalOperator.java`、`CombinedOperator.java`、`GeneralChoiceOperator.java`、`GeneralConstantOperator.java`、`CurriedOperator.java`、`core/evaluate/OperatorBuilders.java`。
- Aspect：`part/aspect/Aspects.java`、`part/aspect/read/AspectReadBuilders.java`、`core/part/aspect/AspectRegistry.java`、`core/item/AspectVariableFacade.java`、各 `PartType*Reader.java`、`ValueTypeListProxyPositioned*`。
- Proxy/Delay：`core/item/ProxyVariableFacade.java`、`core/evaluate/ProxyVariableFacadeHandler.java`、`blockentity/BlockEntityProxy.java`、`blockentity/BlockEntityDelay.java`、`core/evaluate/InventoryVariableEvaluator.java`。
- 书/教程（机制学习，非目标版本权威）：[Logic Programming](https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html)、[Operator 教程](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)、[Saving State](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)。在线书来自 1.21.1/NeoForge 环境，具体编译以 1.20.1 源码/注册表为准。

## 18. 待游戏内验证事项

以下不是设计留白，而是尚未执行的运行验证：

- `writeVariableFacadeItem(true, oneBlank, ...)` 在真实服务端的 ID/事件/背包路径；`ItemStack.save`/`Component` 序列化。
- 空白卡拆分/主背包容量预检/失败后部分卡状态。
- 变量仓库 45 格刷新、跨多个仓库/维度、卡移动/删除时引用失效。
- Aspect 固定表在真实 part/target/网络中的 `hasPartVariable` 行为。
- Proxy/Delay 静态图在真实方块实体/槽位/循环下的 describe 结果。
- 客户端 `id_compile`/`id_make_cards` 与后端 `id_describe_inputs`/`id_create_cards` 的网络包、线程、UI、权限与取消。
- Delayer 手动反馈接线、采样顺序与容量。
- 所有扩展 Mod 的额外 type/operator/aspect；本文只覆盖固定版本核心。
