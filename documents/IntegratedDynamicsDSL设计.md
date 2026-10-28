# Integrated Dynamics DSL 设计

## 1. 定位与设计决策

本文是语言及编译合同设计，不是已实现功能说明。目标为 Minecraft 1.20.1 / Forge 的 Integrated Dynamics；本轮只写规范，不实现解析器、编译器、网络包或游戏机制。

DSL 是“原生变量卡程序的批量构造语言”。它让 Agent 在一个命名、带类型的逻辑空间内组合计算，随后由客户端工具生成卡片构造计划。DSL 不引入新的求值器，不是运行在客户端或服务器上的通用脚本。

已确定的职责边界：

|层|职责|
|---|---|
|Agent|取得网络描述，选择输入绑定与输出目标，调用编译/部署工具，观察结果|
|DSL|描述输入、命名表达式、Operator 值和命名输出|
|客户端编译器|在工作线程解析、检查并展开卡片依赖图；不访问世界对象|
|服务端|接收结构化卡片构造计划，验证并分配真实变量ID、写入实际卡槽；从不接收DSL源码|
|ID及其扩展|持续读取、求值、采样历史和执行物流等原生行为|

沿用项目已有的“客户端策略/Tool，服务端游戏机制”约定，见 [客户端 ReAct 与 Tool 架构设计](./客户端ReAct与Tool架构设计.md)。不改变既有 AE2/JEI、game_action 或 shared_session 合同。

本规范选择：

1. 表达式采用函数式写法：命名绑定、空白函数应用、同质列表、条件选择、部分应用与组合。
2. 顶层只有 `input`、`let`、`output`；无部署、配置、赋值或事件语句。
3. 使用显式分号结束声明，换行与缩进只负责排版，不实现 Haskell layout rule。
4. 输入类型必须声明；中间结果和输出可以推断，也可以注解。
5. 不实现用户定义的泛型函数、参数函数定义、通用 lambda 或任意递归。函数值来自原生 Operator、输入绑定、部分应用及明确的原生组合操作。
6. 不把任意 lambda 改写成 Pipe/Pipe2：原生 Pipe 会先求值中间结果，机械改写可能破坏非严格参数和条件分支。语言明确保留原生求值边界，而不宣称完整 Haskell 惰性语义。
7. 具体原语由目标环境提供类型合同与注册ID，不从在线书的显示名猜测。

## 2. 从完整程序理解语言

### 2.1 按指定物品库存补货

```haskell
input stock : [Item];

let iron = item "minecraft:iron_ingot";
let limit = 64;
let count = itemCount stock iron;
let need = count < limit;

output request : Item = if need then iron else emptyItem;
output enabled : Boolean = need;
```

`stock` 不在源码中指定坐标。Agent 在工具参数里把它绑定到某个 Inventory Reader 的物品列表 aspect，或者同网络已有的对应变量。`request` 是 Item 值，`enabled` 是 Boolean 值；它们如何接到执行部件，由部署工具决定。

- `itemCount` 统计相同物品与NBT的总数量，不是槽数，也不是所有物品总量。
- `request` 的物品栈数量不是 exporter 的搬运速率。
- 使用 ItemStack Export aspect 时，工具必须单独处理 Empty is Wildcard、Blacklist、Rate、Channel 等属性；本段源码不能保证恰好补到64。
- 不能把 `request` 和 `enabled` 当成可同时插入同一个普通 exporter 的两张控制卡。

### 2.2 输出一个物品谓词

```haskell
input modName : String;

let fromMod : Fn(Item) -> Boolean =
    itemMod >> eq modName;

let acceptable : Fn(Item) -> Boolean =
    allOf fromMod itemIsEnchantable;

output predicate : Fn(Item) -> Boolean = acceptable;
```

`modName`由工具绑定到目标环境取得的模组显示名，不硬编码名称格式。`eq modName`绑定等于函数的第一个参数，得到`String -> Boolean`原生Operator；绑定仍是动态引用。`itemMod >> ...`将Item转为模组显示名再比较。`allOf`是谓词合取，不是Boolean值的`&&`。

模组显示名不是命名空间：`itemMod` 不承诺返回 `minecraft` 这类注册前缀。需要其他标识语义时，应查询并使用对应的真实原语。

### 2.3 动态选择函数，不提前执行函数

```haskell
input enabled : Boolean;

let predicate : Fn(Item) -> Boolean =
    if enabled then itemIsEnchantable else constant false;

output filter : Fn(Item) -> Boolean = predicate;
```

两分支产生相同签名的 Operator 值。`if` 选择的是函数值，不是遍历库存；原生消费者之后才对物品应用该函数。

### 2.4 使用已有 Delayer 的历史

```haskell
input history : [Boolean];
input start : Boolean;
input stop : Boolean;

let previous = getOr history 0 false;
output next : Boolean = if stop then false else start || previous;
```

工具将 `history` 绑定为某个 Delayer 的历史，容量1；将 `next` 接回该 Delayer 的采样输入。初始历史为空时返回false；停止条件优先。连接与采样配置不属于DSL。DSL自身没有可变状态，也不直接生成时间循环。

### 2.5 有限图中的无限列表

```haskell
let naturals = iterate 0 increment;
output firstTen : [Integer] = slice naturals 0 10;
```

`iterate` 映射原生 Lazy Built，`slice` 映射原生列表切片。定义图是有限且无即时循环的；无限的是按需生成的列表，不是源文件或卡片数量。

## 3. 词法、名称与声明

### 3.1 源文件

- 源码为UTF-8；编译输入长度按UTF-8字节计算，限制由环境合同提供。
- 空白与缩进没有语义；每个声明必须以 `;` 结束，包括最后一条。
- `--` 到行尾为注释；不支持嵌套块注释。`a--b` 会开始注释，减去负值应写成 `a - (-b)`。
- 标识符为 `[A-Za-z_][A-Za-z0-9_]*`，区分大小写。
- 关键字为 `input`、`let`、`output`、`if`、`then`、`else`、`true`、`false`、`op`、`item`、`block`、`fluid`、`nbt`、类型构造字 `Fn`。
- 字符串使用双引号及JSON式转义：`\"`、`\\`、`\n`、`\r`、`\t`、`\b`、`\f`、`\uXXXX`。字符串内容可为Unicode；非法Unicode转义报错。
- 不支持字符串插值、字符字面量、隐式资源命名空间或读取外部文件。

### 3.2 声明

```text
input name : Type;
let name [: Type] = expression;
output name [: Type] = expression;
```

`input` 与 `let` 共用表达式名称空间，禁止重名、遮蔽关键字、遮蔽环境中的内建名称。`output` 使用独立的端口名称空间，不能在表达式中作为变量引用；端口之间禁止重名。端口可与绑定同名，例如 `let request = ...; output request = request;`。

- 所有输入都必须有工具提供的绑定及匹配的类型合同；多余的绑定参数也应诊断，避免拼写错误被忽略。
- `let` 是节点命名/共享，不是可变赋值，不按文本顺序执行。
- 允许前向引用；全局解析后检查即时依赖环。自引用和间接引用环均拒绝。
- 检查整个源文件的名称与类型，不用未使用声明藏住错误；只有可从输出到达的节点进入生成计划。
- 至少有一个输出。未使用的输入/let可以给警告，但仍接受检查。
- 没有函数参数声明形式 `let f x = ...`、局部 `let ... in ...`、模块、导入源码或运行语句。

## 4. 类型系统

### 4.1 表面类型

```text
Boolean  Integer  Long  Double  String
Item     Block    Entity  Fluid  NBT
Ingredients  Recipe
[T]
Fn(T1, T2, ...) -> R
```

`[T]` 表示同质列表。`Fn(Item) -> Boolean` 表示原生单输入谓词；`Fn(Integer, Integer) -> Integer` 表示两个有序输入的原生Operator。参数列表不是一个tuple值；DSL没有tuple值类型。

`Fn() -> R` 可表示零参数Operator，使用 `call0` 求值。返回类型可以是另一个具体Fn类型；由原语合同或输入合同决定，不把所有箭头自动展平成一个原生参数列表。

`Fn` 在卡片上的ID值类型是 Operator，但编译器额外跟踪参数与返回签名。裸 `Operator` 不作为源类型接受，因为它丢失了检查 Filter/Apply 等操作所需的信息。

14个核心ID值类型均可通过输入与已描述原语使用。额外类型名（例如Chemical）只有在目标环境明确提供类型描述时才有效。未知类型报错，不把它降为Any。

Any、Number、Named、Nullable等ID类别只出现在原语内部约束中，不是源代码中可以随意使用的通配类型。特别是没有 `Any` 注解来绕过类型检查。

### 4.2 数字与类型转换

|字面量|类型|规则|
|---|---|---|
|`64`、`-1`|Integer|32位有符号，超范围报错|
|`64L`、`-1L`|Long|64位有符号，L后缀大小写固定|
|`1.0`、`1e3`|Double|必须包含小数点或指数；字面量须能表示有限值|

负整数字面量按带符号整体做范围检查，因此允许 `-2147483648` 和 `-9223372036854775808L`；不能先把正数部分解析到同宽整数再取负。

数字不根据上下文隐式改类型，混合数值运算也不自动插入转换。`input energy : Long; energy < 1000;` 是类型错误，应使用 `1000L`。转换通过 `toInteger`、`toLong`、`toDouble` 显式表达，运行结果沿用ID原生转换，不能宣称无损。

同类型的算术才被接受；普通 `+` 仅用于数字。Integer `/` 保留原生整数运算语义，不自动变Double。数值溢出、除零与浮点边界保留原生行为，不在本设计中虚构更安全的数学运行时。

### 4.3 推断规则

- 输入必须有具体类型；绑定注解是检查合同，不是强制转换。
- 列表元素、条件两分支、比较两侧必须能得到兼容的具体类型；基础规则要求同类型，不隐式做数值提升。
- 内建原语可有内部类型变量（例如eq的两参同类型），每个原语出现位置新鲜实例化。
- 用户 `let` 是单态共享绑定，不做Hindley–Milner let-generalization。一个绑定不能一处当Integer比较器，另一处当Item比较器。
- 未确定的函数参数、列表元素或输出类型必须由使用上下文、注解或工具的输出合同确定；否则报 `E_UNRESOLVED_TYPE`。
- 动态输入的类型合同由服务器环境描述支持；用户声明不能将一个未知Operator签名任意指定为Predicate。
- 编译期类型通过不保证外部输入永远保持类型；Proxy变更、目标变化等运行错误仍由原生验证/求值报告。

例如：

```haskell
let sameItem : Fn(Item, Item) -> Boolean = eq;
let nothing : [Item] = [];
output result : Boolean = sameItem emptyItem emptyItem;
```

没有上下文的 `let nothing = [];` 或直接输出未特化的 `eq` 拒绝，不生成含糊的“任意类型卡”。

## 5. 表达式与优先级

### 5.1 基本表达式

- 标量、对象字面量、列表、名称、括号。
- `f x y`：函数应用，左结合；`f x y` 与 `(f x) y` 意义一致。
- `if condition then yes else no`：condition必须Boolean，两分支类型兼容；`if`作为函数参数或运算子表达式时应加括号。
- 普通Boolean逻辑 `&&`、`||`、`not`；数字运算；同类型相等；数字顺序比较。
- `++`：String拼接或同元素类型List拼接，由已确定类型选原语。
- `>>`：从左到右的单输入函数组合，与ID Pipe方向一致；不是位移或副作用顺序。

|从高到低|语法|结合性|
|---|---|---|
|1|函数应用|左|
|2|前缀负号|右|
|3|`* / %`|左|
|4|`+ -`|左|
|5|`++`|右|
|6|`>>`|右|
|7|`== != < <= > >=`|不可串联|
|8|`&&`|右|
|9|`||`|右|
|10|`if ... then ... else ...`|按完整条件表达式解析|

`eq "Minecraft"` 先于 `>>` 绑定。`f -1` 解析为减法，而不是负数参数；写 `f (-1)`。`a < b < c` 拒绝，写 `a < b && b < c`。

`not`是Boolean函数，不是新的前缀运算符，所以 `not x && y` 先应用not。不存在整数/物品/空列表的隐式真值规则。

### 5.2 字面量与值构造

|语法|类型|意义|
|---|---|---|
|`true`、`false`|Boolean|明确的布尔值|
|`"text"`|String|字符串|
|`item "minecraft:iron_ingot"`|Item|该物品的基本原型，数量1，无附加NBT|
|`block "minecraft:stone"`|Block|该方块的默认状态|
|`fluid "minecraft:water" 1000`|Fluid|固定流体原型与正Integer数量，单位mB|
|`nbt "{Damage:10}"`|NBT|按目标SNBT解析的静态tag|
|`emptyItem`、`emptyFluid`|Item、Fluid|原生明确空值，不是空白卡|
|`[e1, e2, ...]`|`[T]`|有序同类型列表；元素可以是动态表达式|
|`op "namespace:registered_id"`|具体Fn|查询目标合同里的注册Operator，不执行它|

资源ID必须显式带命名空间、格式合法且存在于目标注册表。对象字面量不凭空给予玩家物品，不接受原始ItemStack JSON/卡片NBT。`fluid`的两个参数必须为字面量；动态构造通过有类型合同的原生操作表达。

Entity、Ingredients、Recipe和额外类型不提供猜测式的通用字符串构造器：使用输入绑定或已有原语。复杂Item/Fluid数据同样通过目标版本的原语组合，不套用1.21 data-component接口。

没有通用 `null`。某个类型的明确空值、空列表、缺失引用和求值失败必须分开。需要其他空值时通过其已描述原语或输入常量绑定取得。

## 6. 函数值、部分应用与内建库

### 6.1 函数应用

- 参数少于原生所需数量：产生剩余参数的Fn值，不计算最终结果。
- 参数刚好足够：求得结果节点。
- 参数超过所需数量：只有前一段返回具体Fn类型时才继续应用；否则报 `E_NOT_CALLABLE`。
- `lt 64` 是“64小于下一参数”，不是“下一参数小于64”；后一种写 `flip lt 64`。
- 不支持任意自定义参数函数或lambda。`let eligible = itemIsEnchantable;`是函数值绑定，不是函数源码定义。
- 不在客户端应用函数到世界读取值；所有动态求值留给ID。

### 6.2 固定便利名称与原生映射

下表的A/B/C/N是编译器内部类型变量，不是用户声明的类型参数。N限Integer/Long/Double。映射使用目标源码符号说明；实际传输使用其 `getUniqueName()` 返回的注册ResourceLocation，不猜字符串。

|名称或语法|类型/约束|ID原生映射|
|---|---|---|
|`add/sub/mul/divide/mod`与对应符号|`Fn(N,N)->N`|ARITHMETIC_ADDITION/SUBTRACTION/MULTIPLICATION/DIVISION/MODULUS|
|`min/max`|`Fn(N,N)->N`|ARITHMETIC_MINIMUM/MAXIMUM|
|`increment/decrement`|`Fn(N)->N`|ARITHMETIC_INCREMENT/DECREMENT|
|`eq/ne`、`==/!=`|`Fn(A,A)->Boolean`|RELATIONAL_EQUALS/NOTEQUALS|
|`lt/le/gt/ge`及顺序比较|`Fn(N,N)->Boolean`|RELATIONAL_LT/LE/GT/GE|
|`not`、`&&/||`|Boolean|LOGICAL_NOT/AND/OR|
|`if`|Boolean、两分支同类型|GENERAL_CHOICE|
|`toInteger/toLong/toDouble`|数值到对应具体类型|NUMBER_CAST_INTEGER/LONG/DOUBLE|
|`++`|String或`[A]`同类|STRING_CONCAT或LIST_CONCAT|
|`length`|`Fn([A])->Integer`|LIST_LENGTH|
|`isEmpty`|`Fn([A])->Boolean`|LIST_EMPTY|
|`get`|`Fn([A],Integer)->A`|LIST_ELEMENT|
|`getOr`|`Fn([A],Integer,A)->A`|LIST_ELEMENT_DEFAULT|
|`append`|`Fn([A],A)->[A]`|LIST_APPEND|
|`slice`|`Fn([A],Integer,Integer)->[A]`|LIST_SLICE，范围[start,end)|
|`map`|`Fn(Fn(A)->B,[A])->[B]`|OPERATOR_MAP|
|`filter`|`Fn(Fn(A)->Boolean,[A])->[A]`|OPERATOR_FILTER|
|`reduce f xs seed`|f为`Fn(A,B)->A`，xs为[B]，seed为A|OPERATOR_REDUCE，保持原生顺序f,xs,seed|
|`iterate seed f`|seed为A，f为`Fn(A)->A`，结果[A]|LIST_LAZYBUILT，保持原生顺序seed,f|
|`identity`|`Fn(A)->A`|GENERAL_IDENTITY|
|`constant x`|剩余一个任意类型参数，结果类型为x类型|GENERAL_CONSTANT的部分应用|
|`flip`|原Fn至少两个参数，交换前两个|OPERATOR_FLIP|
|`>>`|`Fn(A)->B`和`Fn(B)->C`得到`Fn(A)->C`|OPERATOR_PIPE|
|`pipe2 f g h`|f:`Fn(A)->B`，g:`Fn(A)->C`，h:`Fn(B,C)->D`|OPERATOR_PIPE2，结果`Fn(A)->D`|
|`allOf/anyOf`|两个相同输入类型的Predicate|OPERATOR_CONJUNCTION/DISJUNCTION|
|`negatePred`|Predicate到同签名Predicate|OPERATOR_NEGATION|
|`call0`|`Fn()->A`到A|OPERATOR_APPLY_0|
|`itemCount`|`Fn([Item],Item)->Integer`|OBJECT_ITEMSTACK_LIST_COUNT，匹配同物品及NBT|
|`itemMod`|`Fn(Item)->String`|OBJECT_ITEMSTACK_MODNAME，模组显示名|
|`itemIsStackable`|`Fn(Item)->Boolean`|OBJECT_ITEMSTACK_ISSTACKABLE|
|`itemIsEnchantable`|`Fn(Item)->Boolean`|OBJECT_ITEMSTACK_ISENCHANTABLE|

`&&`是对Boolean值的原生短路操作；`allOf`是之后会对同一个输入应用两个谓词的短路组合。不能互换。

不把 `pipe2 f g and` 当成 `allOf f g`：Pipe2会先求两个中间结果，后者可以不执行第二个谓词。`pipe2`本身明确承诺原生的先求中间值行为，不假装是非严格函数应用。

### 6.3 原语合同目录

便利名称不是封闭功能目录。`op "实际注册ID"`让程序使用其他核心或扩展原语，但条目必须有可检查的合同：

- 注册ID、所有者/所需扩展、参数数量与类型类别约束；
- 具体输出或可确定的输出类型规则；
- 原生参数顺序、消费的Operator签名约束；
- 必要的列表有限性要求；
- Fn值及序列化是否被目标支持。

不能用网页中的Global name或显示名直接替代注册ID。没有合同、输出Any无法确定、需要未知函数签名的条目报 `E_NATIVE_CONTRACT`，不通过用户注解或客户端随意提交一份新合同绕过。

便利名称只重命名原语，不改变参数顺序。`reduce`和`iterate`也保留原生顺序，使完整应用、部分应用和函数值使用一致；不隐藏一个只在完整调用时成立的参数排列适配器。未知扩展不得发布凭空的可调用名称。

## 7. 求值、有限性与状态

### 7.1 按需构造，不在编译期求值

编译器生成定义，而不取得输入当前值后替换它。`let count = itemCount stock iron;`不在客户端算一次库存数量，也不生成静态count快照。

- `if`映射Choice；`&&/||`映射原生逻辑操作，保留其短路行为。
- 未选分支仍须有合法引用、完整类型和可构造定义；短路不允许引用不存在的输入。
- 除字面量解析/编码外，不做计算式常量折叠、函数提前执行或自动物化。
- 不用代数重写改变原生错误和求值顺序；不把函数组合或图求值等同于完整Haskell call-by-need。
- `false && (1 / 0 == 0)`类型检查通过，编译器不主动执行除法；运行时保留AND的短路。除法错误是否出现，由实际请求路径决定。
- 条件、列表访问和函数组合产生的错误仍按ID实际合同处理，工具报告来源节点，不吞异常后返回假值。

### 7.2 列表有限性

在类型之外跟踪 `Finite / Infinite / Unknown` 属性，不增加用户书写的“无限类型”：

- 列表字面量为Finite；已验证的有限读取可以标Finite。
- iterate为Infinite；map继承其列表的有限性。
- 有确定有限边界的slice可得Finite；不能证明边界时保守跟踪。
- 原生要求有限列表的filter/reduce等：已知Infinite则编译错误；Unknown可生成计划并提示运行时可能失败；不能用注解伪造Finite。
- 不把某个未核对的原语一律标为“有限”或“安全处理无限”。原生length等行为保留真实合同，不重新定义数学无穷。

### 7.3 历史边界

Delayer历史通过输入绑定提供，不在即时定义图中加入指向输出的逆向边。部署工具可以把输出接回历史部件的采样槽；这一外部时间连接与即时卡片引用环不同。

Proxy输入仍是动态间接引用，不自动视为时间边界。循环Proxy或已有变量的循环依赖属于环境/原生验证错误，不会因为源码图无环就自动合法。

DSL不承担Delayer容量、采样间隔、更新优先级、持久化或重置；也不承诺同tick的精确读写顺序。

## 8. 可实现的语法骨架

下面使用EBNF描述声明与表达式。`Identifier`、字符串和数字是词法token；空白和注释已跳过。比较最多出现一次。对象构造器中的资源字符串与数量为字面量，不是表达式。

```ebnf
program       = declaration, { declaration } ;
declaration   = "input", Identifier, ":", type, ";"
              | "let", Identifier, [ ":", type ], "=", expr, ";"
              | "output", Identifier, [ ":", type ], "=", expr, ";" ;
type          = Identifier
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

`Fn`是类型位置专用构造关键字，不能被输入或let名称遮蔽；便利常量和函数名称也不能被用户遮蔽。类型中的其他Identifier必须在环境类型表中存在。

解析器优先匹配最长token：`>>`、`>=`、`<=`、`==`、`!=`、`++`、`&&`、`||`、`->`、注释`--`。负号不是数字token的一部分，范围检查可以识别UnaryMinus+整数token组成的带符号字面量。

## 9. 从表达式展开为变量卡

### 9.1 编译步骤

1. 解析为带源码范围的AST。
2. 收集input/let名称与输出端口；解析前向引用并检查即时依赖环。
3. 使用输入、输出及原语合同生成类型约束；确定数值、列表元素、Fn参数/返回类型。
4. 检查已知无限列表与有限消费合同；检查原语在目标环境存在。
5. 从输出追溯可达定义，去掉不需实体卡的名称别名。
6. 按确定顺序生成卡片构造节点与外部引用。源码相同且环境描述相同，局部节点编号、输出映射和顺序应稳定。
7. 返回计划与客户端源码映射；不分配真实变量ID、不写卡槽。

不做任意公共子表达式消除。对同一let的多次引用共享一个定义；字面量可按具体类型与规范编码共享。相同文本的两个原生调用不因为“看起来纯”就自动合并。

### 9.2 构造节点

|节点|原生落点|
|---|---|
|Value|ValueTypeVariableFacade：具体类型与静态值，包括已验证的常量列表|
|NativeOperatorValue|Operator值的常量卡：目标注册Operator；不是客户端代码或虚拟函数序列化blob|
|ReadAspect|AspectVariableFacade：真实partId与read aspect，由绑定合同解析|
|Proxy / Delay|对应原生代理/历史引用facade，由绑定合同解析|
|Call|OperatorVariableFacade：真实原生operator与有序变量引用|

已有变量输入是 `External(bindingHandle)` 引用，不必再生成一张实体卡；其他本批节点由 `Local(nodeKey)` 引用。所有生成节点在定义序列中先于引用者。External必须解析为同网络中的有效变量，不是客户端可以自由填入的任意真实ID。

### 9.3 具体展开规则

|源表达式|展开|
|---|---|
|字面量|Value节点|
|let别名/输出别名|引用已有节点，不为名字单独造卡|
|完全应用且callee为已知原生名称|直接Call该原语，使用原生参数顺序|
|原语作为值|NativeOperatorValue|
|部分应用或动态Fn应用|Call OPERATOR_APPLY，逐参绑定/应用；不依赖异质Apply N列表|
|if|Call GENERAL_CHOICE(condition, yes, no)|
|&& / \|\||Call LOGICAL_AND/OR，不改成Pipe2|
|函数组合、谓词操作|Call对应原生高阶原语，输入为Fn值节点|
|动态列表字面量|具体类型的空列表起点，再按顺序Call LIST_APPEND|
|全静态列表字面量|单个具体元素类型的Value列表；不提前执行普通函数把它变成常量|
|前缀负号|带符号字面量直接编码；其他数字表达式映射同类型zero - expression|
|reduce / iterate|保持原生参数顺序，与其他原语相同地应用|

函数参数消耗过程必须与类型检查一致；不知道结果Fn签名时不能凭“Apply输出Any”继续猜测。

### 9.4 补货示例的卡片图

第2.1节在stock绑定为ReadAspect且字面量共享的情况下，需要7个不同定义：

|局部键|定义|输入|
|---|---|---|
|0|ReadAspect stock|工具绑定|
|1|Value iron Item|铁锭基本原型|
|2|Value Integer 64|—|
|3|Call itemCount|0,1|
|4|Call lt|3,2|
|5|Value emptyItem|—|
|6|Call Choice|4,1,5|

输出request指向6，enabled指向4。不是额外生成两张“output语句卡”。若stock是已有变量引用，本批只需6个新定义。实体物品数量、输出多处投放需要的副本与空白卡消耗由部署决定，不等同于定义节点数。

## 10. 输入环境、编译结果与服务端合同

### 10.1 CompileEnvironment

由工具调用显式传入，工作线程只读取不可变描述数据：

- 目标网络/连接会话的描述身份；语言及原语合同版本。
- 类型表、允许的原语描述、资源字面量查验所需的数据视图。
- 输入名到绑定描述：ReadAspect、ExistingVariable、Proxy、DelayHistory、经认可的ConstantSnapshot。
- 输出端口的期望类型/Operator签名，可由部署目标给出；没有目标时源码注解也可特化。
- 源码字节数、节点数、依赖/字面量/NBT/类型深度等资源限制。具体数值是工具/服务端策略，不是语言数学语义。

源码不含坐标、网络授权、真实ID、卡槽或部件属性。输入声明也不授予修改该目标的权限。绑定注解只能验证环境合同，不能替换环境中的实际身份。

### 10.2 编译结果

- `nodes`：有限、拓扑排序的卡片定义序列，局部整数键。
- `imports`：计划中用到的外部绑定句柄与类型合同。
- `outputs`：命名端口到节点/外部引用及具体类型的映射。
- `diagnostics`：编译错误与警告。
- `sourceMap`：节点/绑定与源码范围映射，仅留客户端。
- 目标合同身份，用于发现编译和部署环境不一致。

编译失败不返回可部署的部分计划；成功不等于已经创建卡片。输出不携带原始ItemStack、任意卡片NBT、DSL AST、可执行客户端字节码或未认可的原生serializer。

### 10.3 服务端接收与执行

网络请求携带nodes/imports和明确的部署映射，不携带源码、sourceMap或客户端Tool名称。服务端按固定游戏请求类型调度，沿用项目既有策略/机制隔离。

服务端必须独立：

1. 验证发送者、网络、绑定和目的地；不相信客户端类型注解。
2. 检查消息/值/节点规模、序列拓扑和所有引用；检查真实注册ID和原生输入/输出合同。
3. 解析外部引用，检查当前可访问性与声明类型；校验目标aspect、卡槽、配置和资源条件。
4. 完整验证后才分配真实ID，建立Local到真实ID映射，并通过ID原生写卡/库存更新路径执行。
5. 返回真实创建/部署结果，错误关联局部节点键，客户端使用sourceMap定位源码。

服务端需要原生计划校验器及固定类型合同，不需要DSL parser、源码名称解析或Haskell式类型推导。该校验不是另一套DSL运行时。

“创建新变量”是此计划的默认语义；改写既有真实ID必须由另一个显式授权的游戏操作表达，不能在DSL里添加字段偷偷覆盖旧变量。图验证与提交前提检查可以先全部完成，但不凭文档承诺跨多个Mod库存的事务回滚或游戏副作用可撤销。

### 10.4 线程与取消

- 客户端主线程取编辑器/工具输入和环境描述；工作线程执行纯编译；客户端主线程接收结果、更新UI与发送请求。
- 编译scope可取消，跟随当前工具请求/连接生命周期，不用GlobalScope。
- 编译结果关联源码版本和连接代次；旧结果不覆盖新输入，不跨断线复用。
- 服务端世界与库存变更在服务端主线程。JNI、Rust或客户端求值器不在本设计中。
- 服务端只知本次请求局部节点键，不因保留错误定位而需要接收整份源码。

## 11. 诊断

诊断至少包含code、severity、message、主源码范围和可选相关范围。源码范围在客户端以UTF-16偏移保存，与Kotlin字符串/编辑器一致；行列面向人从1开始，明确列也是UTF-16计数。服务端返回局部节点键与实际错误，不猜客户端行列。

|代码|情况|
|---|---|
|E_SYNTAX|缺分号、非法token、错误声明/表达式|
|E_UNKNOWN_NAME / E_DUPLICATE_NAME|未定义名、重名或遮蔽保留名|
|E_CYCLE|即时定义循环；同时给出引用链|
|E_INPUT_BINDING|输入没有绑定、额外绑定或绑定合同不匹配|
|E_TYPE / E_UNRESOLVED_TYPE|具体类型冲突或无法特化|
|E_NOT_CALLABLE / E_FN_SIGNATURE|非函数应用、Fn参数/返回签名不匹配|
|E_EMPTY_LIST_TYPE|空列表元素类型不能从上下文确定|
|E_LITERAL_RANGE / E_LITERAL_RESOURCE / E_LITERAL_NBT|字面量越界、资源ID非法/不存在、SNBT非法|
|E_NATIVE_CONTRACT|原语不存在、扩展缺失或无可检查的类型合同|
|E_INFINITE_CONSUMER|将已知无限列表交给有限消费原语|
|E_UNSUPPORTED_SYNTAX|lambda、参数函数定义、赋值、循环等不属于语言|
|E_LIMIT|超过环境公布的编译/计划限制|
|E_TARGET_CONTRACT|目标环境/输出合同不再匹配|

对部署或运行错误另行标注阶段，不把“未写入”“写入失败”“原生求值失败”统称为语法错误。输出完整错误，不返回false/空Item来伪装成功。

设计审查用例：

- Long输入与Integer字面量比较：拒绝，提示L后缀或显式转换。
- 一个let同时被要求为两种不同Fn签名：拒绝，不复制成两个偷偷不同的变量。
- `if true then 1 else "x"`：拒绝，即使第二分支当下不会被求值。
- `let a = b; let b = a; output result = a;`：拒绝并报告a→b→a。
- `filter (flip lt 10) (iterate 0 increment)`：已知无限消费，拒绝。
- `filter itemIsEnchantable stock`：Fn(Item)→Boolean与[Item]相容。
- 未知签名Operator输入：拒绝，仅仅声明Fn类型不足以成为证据。
- 绑定历史的反馈程序：即时图无环；外部Delayer连接由部署合同核查。

这些是规范推导，不声明已经有编译器执行了这些用例。

## 12. 明确排除的能力

- 完整Haskell：模块、type class、ADTs、模式匹配、泛型函数定义、Monad/IO。
- 用户函数源码、lambda、任意递归、Y组合子或将递归卡片环作为函数递归。
- 客户端持续计算世界逻辑、服务器解释DSL、替代ID求值器。
- 部件放置/拆除、卡槽坐标、Channel/Rate/优先级设置、直接搬运物品。
- 自动接受所有在线1.21运算符或任意扩展卡片handler。
- 隐式物化世界读取、未知类型强制转换、吞掉错误的假回退。

这些排除项定义语言合同，不是留空的实现占位符。原生Operator值、部分应用、组合与谓词足以覆盖本文完整示例；需要超出此边界的语法时，必须先证明它的原生展开与求值语义，再修改规范。

## 13. 设计验证与来源

本轮做了源码机制核查与完整示例的类型/节点展开审查；没有实现编译器，因此没有语法执行、游戏部署或跨线程运行验证，也不为纯文档变更运行Gradle构建。

已核查的关键事实：

- Pipe/Pipe2先求中间值：[CombinedOperator](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/CombinedOperator.java#L247-L399)。
- Constant只请求第一个参数：[GeneralConstantOperator](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/GeneralConstantOperator.java)。
- 部分应用保存IVariable：[CurriedOperator](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/CurriedOperator.java)。
- Apply的不足参数与后续Operator应用：[ValueHelpers](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueHelpers.java#L144-L173)。
- Choice分支类型与值请求：[GeneralChoiceOperator](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/GeneralChoiceOperator.java)。
- 列表构造、高阶运算与具体Operator映射：[目标Operators](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java)。
- 历史与采样：[BlockEntityDelay](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityDelay.java)。
- 变量卡ID与写入：[VariableFacadeHandlerRegistry](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/VariableFacadeHandlerRegistry.java)。

教程参考：[Logic Programming](https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html)、[Operator教程](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html)、[Saving State](https://integrateddynamics.rubensworks.net/book/tutorials/saving_state.html)。在线书来自1.21.1/NeoForge环境，具体编译原语以目标1.20.1注册表与固定合同为准。
