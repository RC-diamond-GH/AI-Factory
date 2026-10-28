# 共享类 Haskell 解释器

本文描述 `com.rcdiamond.aifactory.lang` 包。它是纯 Kotlin 的共享语言前端与可扩展后端框架，被 Integrated Dynamics DSL 及后续需要“小型命名表达式图”的功能复用。**框架本身不引用 Forge、Minecraft、Integrated Dynamics、客户端或任何世界对象**；域类型、原生 Operator 名称和对象构造器都由宿主通过环境注入。

它与《IntegratedDynamicsDSL设计》的关系是：该规范决定 ID 目标环境的类型名、原语目录与原生落点；本包只实现共享的词法、语法、类型检查、即时依赖、源映射、确定性 typed graph 以及一个可用于 smoke test 的纯后端。DSL 编译器消费 `TypedModule`，把 `IrOperation` 映射为原生卡片计划；服务端不需要本包。

## 1. 设计边界

已实现：

- 声明式源码：`input`、`let`、`output`，显式分号，`--` 行注释。
- JSON 风格字符串转义、Unicode 标量、注释、UTF-16 源码范围。
- 有符号 Integer/Long 最小值、Double、带后缀数字、范围检查。
- 同质列表、`Fn(...) -> ...` 类型、条件、算术/比较/布尔/拼接/组合优先级。
- 空白函数应用、左结合应用链、部分应用。
- 前向引用、单态 `let`、重复名/保留名检查、即时依赖环检测。
- 宿主注入名义类型、多态原语 scheme、数值/拼接/自定义约束、对象构造器、`op` 目录。
- 无 `Any` 逃逸：未特化类型直接报错。
- 不可变 `LanguageLimits`，所有深度/数量限制返回诊断而不是 StackOverflow。
- 确定性 typed graph、仅保留可达节点、稳定连续节点编号、源范围。
- 通用 `TypedBackend`、严格纯求值后端、标准标量环境与高阶列表/谓词原语。

明确不做：

- 用户 lambda、函数参数定义、通用递归、Y 组合子、模块/import/type class/ADT。
- 编译期求值世界读取、自动常量折叠、自动类型提升/强制转换。
- 任意 `Any`、未知类型降级、未知原生合同猜测。
- 在纯后端模拟原生非严格参数（见第 7 节）。

## 2. 包与文件

|文件|内容|
|---|---|
|`Source.kt`|`SourceText`、`SourceSpan`、`SourcePosition`、`utf8ByteLength`|
|`Diagnostics.kt`|`Diagnostic`、`Severity`、`DiagnosticCode`、`RelatedDiagnostic`|
|`Limits.kt`|`LanguageLimits`、`EvaluationLimits`|
|`Lexer.kt`|`LanguageLexer`、`Token`、`TokenKind`|
|`Ast.kt`|`Program`、声明、表达式、类型引用、静态构造参数|
|`Parser.kt`|`LanguageParser`、`ParseResult`、`parseLanguageSource`|
|`Types.kt`|`LangType`、`TypeScheme`、`TypeConstraint`、`SchemeConstraint`|
|`Environment.kt`|`LanguageEnvironment`、`EnvironmentBuilder`、原语/构造器/Operator 合同、`PrimitiveTypeRule`|
|`Inference.kt`|内部名称解析、环检测、单态类型检查、约束终结|
|`TypedGraph.kt`|`TypedModule`、`TypedGraph`、`TypedNode`、`IrOperation`、`LiteralValue`|
|`Lowering.kt`|checked AST 到确定性 typed graph|
|`Compiler.kt`|`LangCompiler`、`CompilationResult`、`compileLanguage`|
|`Backend.kt`|`TypedBackend`、`BackendResult`|
|`PureBackend.kt`|`PureEvaluator`、`PureValue`、`PurePrimitive`、`StandardScalarRuntime`|
|`StandardScalarEnvironment.kt`|标准标量环境、`FlipPrimitiveRule`|

所有公开 API 位于 `com.rcdiamond.aifactory.lang`。内部实现使用 `internal`，宿主应只依赖本文列出的公开符号。

## 3. 编译入口

```kotlin
val compiler = LangCompiler(
    environment = StandardScalarEnvironment.environment,
    limits = LanguageLimits.DEFAULT,
)

val result: CompilationResult = compiler.compile(
    sourceName = "program.idsl",
    text = """
        input stockCount : Integer;
        let enough = stockCount >= 64;
        output ready : Boolean = enough;
    """.trimIndent(),
)

if (!result.success) {
    println(result.renderDiagnostics())
} else {
    val module: TypedModule = result.module!!
    module.graph.nodes      // 确定性的类型化 DAG
    module.graph.inputs
    module.graph.outputs
}

// 直接重载
val fromText: CompilationResult = compiler.compile(SourceText.of("program.idsl", text))
```

`CompilationResult`：

```kotlin
data class CompilationResult(
    val source: SourceText,
    val diagnostics: List<Diagnostic>,
    val program: Program?,     // 语义失败时仍可保留，供编辑器定位；不可据此 lowering
    val module: TypedModule?,  // 只有全流程成功才非空
)
```

`source` 是所有 span 的基准。编译失败不会返回部分 graph。超大源码在建立行索引前就被拒绝；此时 `source` 是空的占位 `SourceText`，诊断消息给出实际长度/字节数，`span` 为 `0..0`。

`LangCompiler` 无可变共享状态，可被并发复用。

## 4. 源码范围、Unicode 与大小检查

- `SourceSpan(start, end)` 是半开区间，单位是 Kotlin 字符串的 **UTF-16 offset**。
- `SourceText.positionAt(offset)` 给出 `(offset, line, column)`；行、列均从 **1** 开始，列按 UTF-16 单元计数。
- 换行支持 `\n`、`\r\n`、单独的 `\r`；`lineText(line)` 不包含行终止符。
- 字符串内容与标识符规则：源码文件 UTF-8；标识符仅 `[A-Za-z_][A-Za-z0-9_]*`（ASCII），字符串可含 Unicode。
- 字符串使用 JSON 转义：`\" \\ \/ \b \f \n \r \t \uXXXX`。代理对按 Unicode 标量解码；未配对代理报 `E_SYNTAX`。

无分配的大小工具，供 DSL/工具复用：

```kotlin
fun utf8ByteLength(text: String, stopAfter: Int = Int.MAX_VALUE): Int
```

- 不创建中间字节数组。
- ASCII 1、BMP 2、代理对 4、孤立代理 3（不会低估）。
- 超过 `stopAfter` 时立即返回 `stopAfter + 1`（无法表示时返回 `Int.MAX_VALUE`）。
- `SourceText.byteLength` 首次读取时用该函数计算并缓存，不会每次重新扫描。

`LangCompiler.compile(name, text)` 在 `SourceText.of` 前先检查 UTF-16 字符数与 UTF-8 字节数，避免为巨大输入先构建行索引。

## 5. 语法

### 5.1 声明

```text
input  name : Type ;
let    name [: Type] = expr ;
output name [: Type] = expr ;
```

- 每条声明都必须以 `;` 结束，包括最后一条；换行/缩进无语义。
- `input`/`let` 共享绑定命名空间；`output` 是独立端口命名空间。
- 绑定名不能与另一个绑定、环境中的原语名或关键字重名；输出端口可以与绑定同名，但输出端口不能在表达式中当变量引用。
- 至少一个输出。
- 允许前向引用。`let` 的即时依赖不能成环；自引用与间接环都报 `E_CYCLE`，诊断包含引用链。
- `let` 是单态共享定义，不做 Hindley–Milner 泛化。
- 仅从输出可达的节点进入 typed graph。

### 5.2 表达式与优先级

|优先级（高→低）|语法|结合性|
|---|---|---|
|1|函数应用 `f a b`|左|
|2|前缀负号 `-x`|右|
|3|`* / %`|左|
|4|`+ -`|左|
|5|`++`（String 或同质 List 拼接）|右|
|6|`>>`（一元函数组合）|右|
|7|`== != < <= > >=`|不可串联|
|8|`&&`|右|
|9|`\|\|`|右|
|10|`if c then a else b`|完整表达式|

实现细节：

- 右结合运算符链按迭代收集后折叠，超长链不会 StackOverflow。
- `f -1` 是减法；负数参数写 `f (-1)`。
- `a < b < c` 拒绝，写 `a < b && b < c`。
- `if` 作为参数/运算子表达式必须加括号。
- 列表 `[e1, e2]` 必须同质；空列表元素类型由上下文或注解决定。
- 对象字面量：`item "..."`、`block "..."`、`fluid "..." 1000`、`nbt "{...}"`，其中 fluid 数量是正 Integer 字面量；`op "..."` 从环境 Operator 目录取 scheme。
- 前缀负号对直接数字字面量折叠；其他表达式降为 `Negate`。
- `not` 是普通 Boolean 原语，不是前缀运算符。

### 5.3 数字

|字面量|类型|
|---|---|
|`64`、`-1`|Integer（32 位有符号）|
|`64L`、`-1L`|Long（64 位有符号，大写 L）|
|`1.0`、`1e3`|Double，必须是有限值|

带符号整体做范围检查，所以 `-2147483648`、`-9223372036854775808L` 合法；先把正数部分解析为同宽整数再取负会错误。不隐式提升，`Long < 1000` 是类型错误。整数溢出、除零、浮点边界沿用实际后端行为；纯后端使用 JVM `Int`/`Long`/`Double` 语义。

## 6. 类型、环境与注入

### 6.1 类型

```kotlin
sealed interface LangType {
    data class Nominal(val name: String)
    data class ListType(val element: LangType)
    data class FunctionType(val parameters: List<LangType>, val result: LangType)
    data class Variable(val id: Int)      // 仅推断内部出现
}
```

- `FunctionType.parameters` 是参数序列，不是 tuple；零参数 `Fn() -> R` 合法。
- 成功模块中的每个节点类型都由 `LangType.isConcrete()` 为真；`Variable` 不会进入 graph。
- 无 `Any`：未特化输出/参数/列表元素报 `E_UNRESOLVED_TYPE`；空列表无上下文报 `E_EMPTY_LIST_TYPE`。
- 注解只做检查，不做转换。`Integer` 不会接受 `Long` 或 `Double`。

### 6.2 TypeScheme

```kotlin
data class TypeScheme(
    val type: LangType,
    val quantified: Set<Int> = emptySet(),
    val constraints: List<SchemeConstraint> = emptyList(),
)

data class SchemeConstraint(val variableId: Int, val constraint: TypeConstraint)
```

`quantified` 中的变量在每次原语出现时重新实例化，因此同一原语可在不同位置特化为不同类型。`let` 不泛化，只保留一个类型。

### 6.3 约束

```kotlin
sealed interface TypeConstraint {
    data object Numeric      // 解析后必须属于 environment.numericTypes
    data object Concat       // String 或 ListType
    data object Equatable
    data class Named(val name: String) // 由环境注册的 TypeConstraintRule
}
```

- 默认数值类型为 `Integer`、`Long`、`Double`；可通过 `numericTypes(...)`/`addNumericType(...)` 调整，但必须已注册。
- 约束在类型变量解析后检查；未知约束名视为不满足（配置/合同错误会显示为 `E_TYPE`）。
- 约束不会插入转换。

### 6.4 环境构造

```kotlin
val env = LanguageEnvironment.basic().builder()
    .addNominalType("Item")
    .addNominalType("NBT")
    .addObjectConstructor(
        ObjectKind.ITEM,
        "Item",
        listOf(ConstructorArgumentKind.RESOURCE_ID),
    )
    .addObjectConstructor(
        ObjectKind.NBT,
        "NBT",
        listOf(ConstructorArgumentKind.TEXT), // SNBT 任意文本，不走 resource 校验
    )
    .resourceIdValidator { id -> id.matches(Regex("^[a-z0-9_.-]+:[a-z0-9_./-]+$")) }
    .addPrimitive("itemCount", TypeScheme(/* ... */), "host primitive")
    .operatorCatalog { id -> hostCatalog.resolve(id) }
    .build()
```

`LanguageEnvironment.basic()` 只含 `Boolean`、`Integer`、`Long`、`Double`、`String` 与默认数值集合；没有标量原语、对象构造器或 Operator 目录。`StandardScalarEnvironment.environment` 在此基础上加入标量/列表原语。

构造器参数仅支持：

- `RESOURCE_ID`：必须是 Text 参数，若设置了 `resourceIdValidator` 则校验；
- `POSITIVE_INTEGER`：必须是正 Integer 参数；
- `TEXT`：任意文本，适合 NBT/SNBT，不调用 resource 校验。

语法固定了各关键字形态：`item`/`block`/`nbt` 一个文本，`fluid` 一个文本加一个正 Integer；`ObjectConstructorSpec` 会验证参数种类组合。

### 6.5 原语、部分应用与高阶重载

普通原语用固定 scheme。应用是逐步的：

```text
add : Numeric N => Fn(N, N) -> N
add 1        : Fn(Integer) -> Integer   // 部分应用
add 1 2      : Integer
```

应用链在 typed graph 中保持为嵌套 `Apply` 节点；后端可以把“完整应用的原语”折叠为原生直接调用，也可以保留动态 apply。

有些原生原语的结果类型取决于参数类型，例如 `flip` 必须交换任意 arity 的前两个参数。固定一阶 scheme 无法表达；环境可注入：

```kotlin
fun interface PrimitiveTypeRule {
    fun typeAfter(arguments: List<LangType>, context: PrimitiveTypeContext): LangType?
}

interface PrimitiveTypeContext {
    fun resolve(type: LangType): LangType
    fun freshVariable(): LangType.Variable
    fun constrain(type: LangType, constraint: TypeConstraint)
    fun unify(expected: LangType, actual: LangType)
}
```

- 类型检查器按参数顺序调用规则，直到返回值非空；随后普通应用处理剩余参数。
- 该原语出现处的 callee 类型被具体化为 `Fn(consumedTypes) -> ruleResult`，typed graph 仍是普通 `Primitive` + `Apply`，不需要特殊 IR 节点。
- 规则抛 `PrimitiveTypeException(code, message)` 表示确定性失败；抛 `null` 表示还需要更多参数；全部参数用完后仍为 null 报 `E_UNRESOLVED_TYPE`。
- 裸使用规则原语作为值（未给足够参数）报 `E_UNRESOLVED_TYPE`，不会退化为 `Any`。

`StandardScalarEnvironment.FlipPrimitiveRule` 是现成实现：`Fn(A,B,C,...) -> R` 去 `flip` 后得到 `Fn(B,A,C,...) -> R`，arity ≥ 2。

## 7. Typed graph 与后端

### 7.1 数据结构

```kotlin
data class TypedModule(
    val source: SourceText,
    val program: Program,
    val graph: TypedGraph,
)

data class TypedGraph(
    val nodes: List<TypedNode>,
    val inputs: List<TypedInput>,
    val outputs: List<TypedOutput>,
)

data class TypedNode(
    val id: Int,
    val type: LangType,
    val operation: IrOperation,
    val span: SourceSpan,
)

data class TypedInput(val name: String, val type: LangType, val nodeId: Int, val span: SourceSpan)
data class TypedOutput(val name: String, val type: LangType, val reference: IrRef, val span: SourceSpan)
sealed interface IrRef { data class Node(val id: Int) }
```

保证：

- `nodes[i].id == i`，编号连续。
- 每个引用只指向更早的节点；DAG 无环、天然拓扑有序。
- 仅从输出可达的节点存在；`let` 是别名，不单独建节点；多次引用共享同一节点。
- 输入在首次被引用时生成 `Input` 节点；未使用输入不出现在 `inputs`。
- 所有类型具体；所有 span 指向原源码。
- 相同源码 + 相同环境产生相同节点、编号与顺序。不做调用 CSE。

### 7.2 IrOperation

|节点|语义|
|---|---|
|`Input(name)`|外部绑定；类型来自 `input` 声明|
|`Primitive(name)`|该出现处实例化的原语函数值；`TypedNode.type` 是其函数类型|
|`OperatorLiteral(id)`|`op "id"` 的函数值；类型由目录 scheme 特化|
|`Constant(LiteralValue)`|Boolean/Integer/Long/Double/String 字面量|
|`ListLiteral(elements)`|同质列表；元素可以是动态节点|
|`Apply(callee, argument)`|一次函数应用；部分应用是 Apply 链|
|`Choice(cond, yes, no)`|惰性条件，只取一个分支|
|`Arithmetic(op, ...)`|加/减/乘/除/模|
|`Comparison(op, ...)`|相等/不等/数字顺序|
|`LogicalAnd/Or`|语言级短路|
|`Concat`|String 或同质 List 拼接，由类型区分|
|`Compose`|从左到右的一元函数组合|
|`Negate`|非字面量前缀负号|
|`Construct(kind, args)`|已校验的静态对象字面量（Text/Integer 参数）|

`Apply` 不再声明“所有后端都严格”：**参数需求语义属于被调用者**。`Choice`、`LogicalAnd`、`LogicalOr` 是唯一语言级承诺的惰性操作。原生后端可以折叠完整原语应用并保留 `constant` 等非严格参数；纯后端则严格求值参数，且故意不提供 `constant`。

### 7.3 Backend 接口

```kotlin
fun interface TypedBackend<R> {
    fun lower(module: TypedModule): BackendResult<R>
}

data class BackendResult<R>(
    val result: R?,
    val diagnostics: List<Diagnostic>,
)
```

后端在自己的构造函数中捕获绑定、原生目录、运行时值和限制。DSL 编译器可实现 `TypedBackend<IdPlan>`，把 `Primitive("itemCount")` 映射到原生注册 ID，把 `Apply(Apply(Primitive(...)))` 折叠为直接调用，把 `Choice`、`LogicalAnd/Or` 保留为原生短路操作。

## 8. 纯求值后端

`PureEvaluator` 是共享框架的参考后端与测试工具：

```kotlin
val module = LangCompiler(env).compile("test.idsl", source).module!!
val evaluation = PureEvaluator(
    primitives = StandardScalarRuntime.primitives,
    inputs = mapOf(
        "xs" to PureValue.ListValue(listOf(PureValue.IntegerValue(1))),
    ),
    limits = EvaluationLimits.DEFAULT,
).evaluate(module)

if (evaluation.success) {
    val outputs: Map<String, PureValue> = evaluation.result!!.outputs
}
```

实现语义：

- 按需求值，memoize 节点；`Choice`、`&&`、`||` 只求值所需分支。
- 输入必须提供并匹配声明类型；缺失/类型不符报 `E_INPUT_BINDING`。
- 缺失原语运行时、`op` 字面量报 `E_NATIVE_CONTRACT`；原生 `op` 需要原生后端。
- 普通 Apply 参数严格求值；因此纯运行时只支持严格原语。ID 的 `constant` 这类非严格原语不由纯后端模拟。
- 求值步骤/深度/列表物化数量受 `EvaluationLimits` 控制，超限报 `E_LIMIT`。
- 求值异常（除零、越界等）转换为 `E_EVALUATION` 诊断并附带节点源码范围。
- 输出值再次按输出类型校验；不匹配为 `E_TARGET_CONTRACT`。

`PureValue`：

|值|说明|
|---|---|
|`BooleanValue`/`IntegerValue`/`LongValue`/`DoubleValue`/`StringValue`|标量|
|`ListValue(elements)`|没有单独元素类型字段；空列表可匹配任意 `ListType`|
|`ObjectValue(kind, typeName, arguments)`|静态对象构造结果|
|`FunctionValue`|部分应用可观察；`applyTo(arg)` 继续应用，`callZero()` 调用零参函数|

`FunctionValue.signature` 随已应用参数收缩；`requiredArity` 保存原始总参数数；`appliedArguments` 供测试/工具观察部分应用。

## 9. 标准标量环境

`StandardScalarEnvironment.environment` 含：

- 算术：`add`、`sub`、`mul`、`divide`、`mod`、`min`、`max`，`increment`、`decrement`；
- 比较：`eq`、`ne`（同类型）；`lt`、`le`、`gt`、`ge`（数值）；
- 布尔：`not`；
- 转换：`toInteger`、`toLong`、`toDouble`；
- 列表：`length`、`isEmpty`、`get`、`getOr`、`append`、`slice`、`map`、`filter`、`reduce`；
- 高阶：`identity`、`flip`（动态规则）、`call0`、`allOf`、`anyOf`、`negatePred`、`pipe2`。

不包含 `constant`：纯后端严格求值，无法兑现其原生非严格第二参数。ID 环境可自行用固定 scheme 或原生后端实现 `constant`，并在自身后端折叠完整调用。

`StandardScalarRuntime.primitives` 提供与上述固定原语对应的严格运行时。`slice` 使用 `[start, end)`，越界报错；`reduce` 为左折叠，顺序保持 `f, list, seed`；`flip` 支持任意 arity ≥2。

## 10. 诊断

`Diagnostic`：

```kotlin
data class Diagnostic(
    val code: DiagnosticCode,
    val severity: Severity,
    val message: String,
    val span: SourceSpan,
    val related: List<RelatedDiagnostic> = emptyList(),
)
```

`Diagnostic.render(source)` 输出 `error[CODE] name:line:column: message`。类型检查遇到第一个类型错误即终止（fail fast），名称/语法问题可积累多条；所有用户错误以诊断返回，不以异常抛给调用方。限制、字面量、合同和求值错误同样有稳定代码。

主要代码：`E_SYNTAX`、`E_UNKNOWN_NAME`、`E_DUPLICATE_NAME`、`E_CYCLE`、`E_INPUT_BINDING`、`E_TYPE`、`E_UNRESOLVED_TYPE`、`E_NOT_CALLABLE`、`E_FN_SIGNATURE`、`E_EMPTY_LIST_TYPE`、`E_LITERAL_RANGE`、`E_LITERAL_RESOURCE`、`E_LITERAL_NBT`、`E_NATIVE_CONTRACT`、`E_INFINITE_CONSUMER`、`E_UNSUPPORTED_SYNTAX`、`E_LIMIT`、`E_TARGET_CONTRACT`、`E_UNKNOWN_TYPE`、`E_EVALUATION`、`W_UNUSED_BINDING`。框架当前不产生 `E_LITERAL_NBT`/`E_INFINITE_CONSUMER`/`W_UNUSED_BINDING`；它们保留给目标后端/策略层。纯框架不解析 SNBT，`nbt` 文本由原生后端校验。

## 11. 限制

`LanguageLimits` 为不可变 data class，所有字段必须为正：

|字段|作用|
|---|---|
|`maxSourceBytes`|UTF-8 源码字节上限；在建立行索引前预检|
|`maxSourceChars`|UTF-16 字符上限|
|`maxTokens`|词法 token 上限|
|`maxDeclarations`|顶层声明数量|
|`maxNestingDepth`|表达式/类型/括号递归深度|
|`maxTypeDepth`|类型结构深度、统一/occurs 深度|
|`maxListLength`|单个列表字面量元素数|
|`maxStringLength`|解码后字符串 UTF-16 长度|
|`maxNodes`|typed graph 节点数|
|`maxDependencyDepth`|let 依赖链/环搜索深度|
|`maxDiagnostics`|保留诊断数量|

`EvaluationLimits` 独立控制纯后端的 `maxSteps`、`maxDepth`、`maxListElements`。解析器对右结合运算符链使用迭代折叠；依赖环搜索使用显式栈；其余递归路径都有深度上限，因此超限返回 `E_LIMIT`，不会以 StackOverflow 结束。

## 12. 示例

```haskell
input stockCount : Integer;
input history : [Boolean];
input start : Boolean;

let iron = item "minecraft:iron_ingot";
let need = stockCount < 64;
let previous = getOr history 0 false;
let next = if start then need else previous;

output request : Item = if need then iron else emptyItem;
output enabled : Boolean = next;
output function : Fn(Item, Item) -> Boolean = eq;
```

在共享标量环境中，`Item`、`emptyItem` 与 `op` 需要由 ID 宿主注入；共享框架只检查并生成 typed graph。`emptyItem` 若作为宿主原语，应有自己的固定类型 scheme。

编译后：

- 每个 `let` 引用共享同一 `IrRef`；
- `if` 生成 `Choice`；
- `eq` 原语作为值生成 `Primitive`，输出函数类型具体化；
- 仅 output 可达的节点进入图；
- 原生后端按 `Primitive` 名称查表并自行决定卡片构造。

## 13. 扩展指南

新增一个域（例如 ID DSL）：

1. `LanguageEnvironment.basic().builder()` 添加名义类型（`Item`、`Block`、`Fluid`、`NBT` 等）。
2. 为对象关键字添加 `ObjectConstructorSpec`；NBT 用 `ConstructorArgumentKind.TEXT`。
3. 注册固定原语 scheme；高阶/重载原语用 `PrimitiveTypeRule`。
4. 注册 `OperatorCatalog`，`op "id"` 的类型来自其 `OperatorScheme`。
5. 实现 `TypedBackend<Plan>`：遍历 `nodes`，按 `IrOperation` 生成原生定义，使用 `inputs`/`outputs` 和 span 生成源映射。
6. 原生后端应自行验证注册 ID、SNBT、资源存在性和原生合同；共享框架不访问注册表。

不要把 Minecraft/Forge/ID 类型放进 `lang` 包；不要给未知类型或缺失合同添加 `Any`/回退值。

## 14. 验证状态

本切片按并行开发约定只编写、不运行 Gradle 构建或测试。行为测试位于 `src/test/kotlin/com/rcdiamond/aifactory/lang/`，覆盖：

- `LexerParserTest`：最长 token、注释、转义/代理对、优先级、应用与减号、比较链、分号、数字边界；
- `TypeCheckTest`：前向引用、端口命名空间、环、重复/遮蔽、未特化类型、空列表、单态、注解不转换、数值/条件/拼接/函数签名；
- `TypedGraphTest`：可达性、别名共享、Apply 链、全部核心 IR、连续编号、反向引用、确定性、逐出现实例化；
- `EnvironmentTest`：对象构造、NBT 文本、资源校验、Operator 目录、自定义约束、任意 arity 的 `flip`、环境隔离；
- `PureEvaluatorTest`：算术、短路、Choice、输入校验、字符串、列表原语、高阶列表、部分应用、组合、flip、call0、谓词组合、溢出/浮点、运行时错误；
- `LimitsTest`：源码/字节/token/字符串/嵌套/列表/声明/类型/依赖/节点/求值限制；
- `SourceTest`：UTF-16 行列、CRLF、`utf8ByteLength`、诊断渲染。

集成后由父代理统一运行测试与构建；本文不宣称这些测试已经执行通过。
