# Integrated Dynamics DSL 使用指南

## 0. 适用范围与边界

本指南对应 Minecraft 1.20.1 / Forge 47.4.10 / Integrated Dynamics `1.31.3`（commit `69ea239d03828fc76b0384dcc487af391fd9e6bf`）。语言、类型规则、plan schema 与原生 API 的完整合同见 [`IntegratedDynamicsDSL设计.md`](./IntegratedDynamicsDSL设计.md)；源码证据见 [`IntegratedDynamics研究笔记/id-dsl-native-source-audit.md`](./IntegratedDynamics研究笔记/id-dsl-native-source-audit.md)。

语言、纯编译器／独立计划校验和 Agent Tool 已实现并经过构建、测试与离线烟测。**没有启动游戏、没有实际制卡／放置／接线或验证原生网络求值**；离线结果不代替游戏内验收。

DSL 用于生成原生变量卡构造计划，不是客户端脚本，也不会在客户端执行世界逻辑。制卡只把卡放进玩家主背包；不自动放置、不自动接变量仓库、不改部件。

## 1. 工具速查

| 工具/操作 | 参数 | 作用 | 是否改世界 |
|---|---|---|---|
| `id_compile` | `{source: string, bindings: object}` | 客户端预览：解析、静态检查、生成诊断与 plan 预览 | 否 |
| `id_make_cards` | `{source: string, bindings: object}` | 重新编译，发送 plan，服务端校验并制卡到主背包 | 是（背包/空白卡） |
| `id_describe_inputs`（后端） | `{bindings}` | 只读解析网络/part/facade，返回每个输入的静态类型与有限性 | 否 |
| `id_create_cards`（后端） | `{plan}` | 独立校验 plan、预检、分配 ID、写卡 | 是 |

`bindings` 默认 `{}`。source 中的每个 `input` 必须恰好有一个 binding；多出/缺少都会报错。

## 2. 准备 bindings

### 2.1 两类 binding

```json
{"kind":"variable","id":42}
```

- 指向同一授权网络中已存在的变量卡 ID；卡必须在变量仓库/变量容器中，网络能解析。
- Proxy 卡、Delay 卡也用这种形式；它们的 payload `partId` 不是变量 ID。
- 变量 ID 可在变量卡 tooltip/网络描述中看到；不要猜 ID。

```json
{"kind":"aspect","part_id":7,"aspect":"integrateddynamics:list_inventory_itemstacks"}
```

- `part_id` 是**当前网络局部**的 part id；不同网络的相同数字不是同一 part。
- `aspect` 是注册 ID，不是教程显示名。
- describe 会检查 part 类型、aspect 是否 read、part 是否在授权网络中。

### 2.2 describe 返回什么

示例：

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

- 类型 JSON：标量字符串；列表 `{list:T}`；Fn `{fn:[...],result:T}`；没有 Any/类型变量。
- `finite`：`finite`/`infinite`/`unknown`；标量按 finite 处理。
- 源文件里的 input 注解必须与 describe 类型完全一致。describe 是权威，不能用源码注解把 Proxy/Delay/未知 Operator 强行声明成某个 Fn 或列表元素类型。

### 2.3 静态类型证明（不会求值活变量）

describe 只读 facade/网络对象 metadata，不调用 `variable.getValue()`，不读取库存内容。可证明来源：

1. **常量 value 卡**：`getValueType()`；列表再读 raw list proxy 的 `getValueType()`；Operator 常量读 operator 的 `getInputTypes()/getOutputType()`。
2. **标量 Aspect**：`aspect.getValueType()`（Boolean/Integer/Long/Double/String/Item/Block/Fluid/NBT/Entity/Recipe 等具体类型）。
3. **列表 Aspect**：`getValueType()` 只是 `integrateddynamics:list`，必须用固定表匹配 aspect id + part：
   - `integrateddynamics:list_inventory_itemstacks`，part `inventory_reader` → `[Item]`
   - `list_fluid_fluidstacks`，`fluid_reader` → `[Fluid]`
   - `list_fluid_capacities`，`fluid_reader` → `[Integer]`
   - `list_entity_entities` / `list_entity_players`，`entity_reader` → `[Entity]`
   - `list_extradimensional_players`，`extradimensional_reader` → `[Entity]`
   - `list_world_players`，`world_reader` → `[Entity]`
   - `list_recipehandler_recipes`，`machine_reader` → `[Recipe]`
   - 全部为 finite（当前容器槽数有限）；unknown/扩展 aspect 报错。
4. **Operator 调用卡**：`getOperator()` + `getVariableIds()` 递归输入卡，按固定 operator 规则计算；不调用 `getVariable()/getType()`（那会走 conditional output，可能求值）。
5. **Proxy/Delay**：按 `partNetwork.getProxy(proxyId)` 找到方块实体，读输入槽卡，递归静态类型；Delay 的元素类型来自输入槽卡，Proxy 的类型来自目标卡。检测循环；空槽/不可证明报错。

证明只在一次请求内复用成功结果，不跨请求缓存世界状态；较浅位置的证明不会绕过后来路径的深度限制。无环图也有总展开额度，超过后返回 `E_LIMIT` 和拆分 bindings／简化依赖图的提示，不降级为猜测类型。具体额度见 §11。既有 `identity`／`constant` 调用卡继承第一个参数的列表有限性，不能把已知 infinite 变成 unknown。

因此：**空 Delay、空 Proxy、未知扩展 List/Fn、把动态 Operator 当任意 Fn，都会在 describe 阶段失败**，不会在运行时“猜”。这是保守静态环境，不是所有在线书/扩展都自动支持。

### 2.4 常见输入的推荐绑定

- 库存物品列表：`aspect{part_id:<inventory_reader>, aspect:"integrateddynamics:list_inventory_itemstacks"}` → `[Item]`。
- 流体列表：`list_fluid_fluidstacks` → `[Fluid]`；容量列表：`list_fluid_capacities` → `[Integer]`。
- 单个数值/布尔：绑定对应 Aspect（如 `integer_inventory_count`、`integer_fluid_amount`、`integer_fe_amount`、`boolean_inventory_full`）或已有变量。
- 能源：没有 energy list aspect；FE/网络能源是 Integer 标量。
- 已有函数变量：只有在 describe 能静态证明 Operator 签名时才可作为 `Fn` 输入；Proxy/Delay 的函数类型也一样。

## 3. 写 DSL 源文件

### 3.1 最小结构

```haskell
input stock : [Item];
input limit : Integer;

let iron = item "minecraft:iron_ingot";
let count = itemCount stock iron;
let need = count < limit;

output request : Item = if need then iron else emptyItem;
output enabled : Boolean = need;
```

要点：

- 每条声明以 `;` 结束；`--` 到行尾是注释。
- `input` 必须带类型；`let`/`output` 可省略类型，但上下文必须能具体化。
- `if` 两分支同类型；`&&/||` 只用于 Boolean；`allOf/anyOf` 才用于谓词。
- 数字不隐式转换；Long 用 `1000L`；需要时写 `toInteger/toLong/toDouble`。
- 空列表必须能从上下文得到元素类型；`let xs = [];` 单独出现会报错。
- 不支持 lambda、自定义参数函数、赋值、循环、模块、`let ... in ...`。

### 3.2 常用名字

- 算术：`add/sub/mul/div/mod/min/max/increment/decrement` 及 `+ - * / %`。
- 比较：`eq/ne/lt/le/gt/ge` 及 `== != < <= > >=`。
- 列表：`length/isEmpty/get/getOr/append/slice/map/filter/reduce/iterate`。
- 函数：`identity/constant/flip/pipe2/allOf/anyOf/negatePred/call0`，`>>`。
- 物品：`itemCount/itemMod/itemIsStackable/itemIsEnchantable`。
- 字面量：`true/false/字符串/整数/1.0/1L/item/block/fluid/nbt/emptyItem/emptyFluid`。
- 原生 Operator：`op "integrateddynamics:..."`；只接受可静态证明签名的原生 op，高阶组合 op 不能直接 `op` 成函数值，使用内建语法。

## 4. `id_compile`：预览

调用：

```json
{"source":"input stock : [Item];\n...","bindings":{"stock":{...},"limit":{...}}}
```

返回/显示（语义）：

- 诊断列表：`code/severity/message/range`，range 为 UTF-16 offset、行列 1 起；包含相关范围/节点。
- plan 预览：与 `id_make_cards` 将发送的 plan 同构；不含真实 ID，不写卡。
- 不修改游戏状态；不预留空白卡、不分配 ID。
- 源文件上限 32768 UTF-8 bytes；nodes 256；inputs 64；outputs 64；type depth 16；单字面量文本 8192 bytes；列表字面量 256。超限 `E_LIMIT`。
- `unknown` 有限性只给 `W_UNKNOWN_FINITENESS` warning，plan 仍可生成；不要依赖它做正常业务，运行期原生可能失败或长时间遍历（原生 `isInfinite` 对 map/slice 传播不完整）。
- 预览不检查游戏注册表是否存在对应 Item/Block/Fluid；服务端制卡预检会最终核对存在性、非空原型和 Minecraft SNBT。预览成功不是库存容量或原生运行求值成功的保证。

编译错误不会产生可部署的部分 plan。

## 5. `id_make_cards`：制卡

调用与 `id_compile` 相同；客户端会重新编译，并把 plan 发到 `id_create_cards`。

### 5.1 服务端会检查

- plan schema/version/contract/上限；node id 连续、refs 只指更早节点/imports；outputs 类型。
- 每个 node 的类型由固定 operator 签名表重算；不接受客户端自报类型。
- imports 的网络/part/facade 静态类型重新证明；Aspect part 匹配/可读。
- operator 注册 ID/allowlist/签名；资源 ID/Item 非空/Block 默认状态/Fluid 数量/SNBT；列表元素类型/长度。
- 有限性消费者；空白卡数量与主背包容量。

### 5.2 空白卡与容量

- 需要的新卡数 = `nodes` 数 + 不重复的 aspect `(part_id, aspect)` 数；重复 aspect descriptor 共享一张卡（返回的 `cards` 只列首个 import 名，其他 import 指向同一 ID）。`variable` imports 不建卡；outputs 不额外建卡。未使用的源 input 不会进入 plan，也不会建 Aspect 卡。
- 每张新卡消耗一张**无 NBT 的空白** `integrateddynamics:variable`。原生语义上“无 `_type` 且无 `_id`”就是 dummy，但工具更保守：任何非空 tag（自定义名/label/`_type`/`_id`）都不当作空白、不覆盖；只有 `_type` 或只有 `_id` 是 invalid，也不是空白。
- 卡必须拆成 count=1 写；对 count>1 的堆写卡会得到同 ID 的多张卡，工具不会这样做。
- 空白卡总数不足/主背包放不下 → `E_TARGET_CONTRACT`（phase=preflight），预检阶段失败，不分配 ID、不消耗卡。
- 只放主背包（36 槽：9 快捷栏 + 27 背包），不使用创造模式绕过；不放 armor/offhand。
- 变量仓库 45 格/stack=1 是放置阶段容量，不是制卡容量；卡多时准备多个仓库。

### 5.3 成功返回

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

- `cards` 包含 aspect import 卡（`input`）和每个新 node（`node`）；不包含 variable imports。
- `outputs` 给实际 ID 与类型；如果 output 只是既有 input，返回该 ID，无新卡。
- 卡已经在玩家主背包；还没有放入变量仓库/接线。

### 5.4 失败/部分成功

- 预检失败：无新 ID、无卡、无空白消耗。
- 写卡开始后失败：已写卡/已消耗空白/已分配 ID 不保证回滚；响应会带 `phase`、已创建数量/卡和错误 `node`/`input`。不要重试整批而不检查背包，否则会重复建卡/消耗。
- ID 可能跳号（失败消耗）；这是正常现象，不要伪造连续 ID。
- 不承诺跨 Mod 库存/事件的事务回滚。

## 6. 制卡之后：放置与接线

### 6.1 放变量仓库

1. 变量仓库方块 `integrateddynamics:variablestore` 有 45 格，每格只能放 1 张卡；只接受变量卡（含空白）。
2. 把所有被引用的卡放进**同一网络**的仓库/变量容器：Aspect import 卡、每个 node 卡、输出所依赖的卡。可以跨多个仓库，但必须同网络。
3. 放卡顺序在全部放完前不重要；网络刷新后按 `_id` 解析。放一半就接输出通常会出现 “variable not in network”/invalid 类错误。
4. 变量仓库不做类型校验；错误会在消费者 validate/求值时暴露。不要因为“能放进去”就认为类型正确。
5. 从网络移除卡/破坏仓库/换网络会使引用失效；Aspect 卡的 partId、Proxy/Delay 卡的 proxyId 都是网络/位置相关，不要跨网络复用。

### 6.2 接输出

- 把输出卡接到目标部件槽/逻辑编程器/显示器/exporter 等；DSL 不会自动放置或配置。
- `request` 与 `enabled` 不要假定能同时插在同一个普通 exporter 的两个控制槽；具体槽位语义由原生部件决定。
- Aspect 读取卡（如 `stock`）通常只需放在变量仓库供其他卡引用，不一定要接到部件槽；但对应 part 必须在同一网络。
- 输出只是既有变量时，原卡已在网络中，可直接使用返回的 ID。

### 6.3 Delayer/Proxy 手动接线

- Delay 卡由 Delay 方块输出；Delay 的输入槽（slot 0）卡片决定历史元素类型。把 `next` 卡放入该槽形成反馈是用户操作。
- describe 要求 Delay 输入槽已有静态可证明的卡；空槽会失败。首次可先放一张 Boolean 常量卡完成证明，再换入/接入 `next`；替换后类型必须仍是 Boolean。
- DSL 不生成反向边、不自动改 Delay 容量/间隔；同 tick 顺序不承诺。
- Proxy 卡的目标卡在 Proxy 读槽；移动/更换目标会使静态类型失效，运行期原生报错。

## 7. 端到端示例：库存补货

### 7.1 binding

```json
{
  "stock": {"kind":"aspect","part_id":7,"aspect":"integrateddynamics:list_inventory_itemstacks"},
  "limit": {"kind":"variable","id":42}
}
```

`stock` 的 part 必须是 `inventory_reader` 且在网络中；`limit` 必须是 Integer 变量卡。

### 7.2 源码

```haskell
input stock : [Item];
input limit : Integer;

let iron = item "minecraft:iron_ingot";
let count = itemCount stock iron;
let need = count < limit;

output request : Item = if need then iron else emptyItem;
output enabled : Boolean = need;
```

### 7.3 编译/制卡结果

- 直接展开：5 个 node 卡（iron、itemCount、lt、emptyItem、choice）+ 1 个 aspect import 卡 = 6 张新卡；`limit` 是既有变量，不建卡。
- `outputs.request` 指向 choice 卡；`enabled` 指向 lt 卡；没有额外 output 卡。
- 预览以实际 plan 为准；字面量/operator 常量共享可能减少节点。

### 7.4 放置/接线

1. 将 5 张 node 卡 + `stock` aspect 卡放入同一网络的变量仓库（1 个仓库即可，6≤45）；`limit` 已在网络中。
2. 全部放完后确认网络刷新。
3. 把 `request` 卡与 `enabled` 卡接到目标 exporter/条件槽；具体槽位/配置（Empty is Wildcard、Blacklist、Rate、Channel）由你按原生部件设置。
4. `itemCount` 统计同 item+NBT 的数量；`request` 的物品栈数量不是 exporter 速率；DSL 不保证恰好补到 limit。

## 8. 更多示例

### 8.1 物品谓词

```haskell
input modName : String;

let fromMod : Fn(Item) -> String = itemMod;
let matchesMod : Fn(Item) -> Boolean = fromMod >> eq modName;
let acceptable : Fn(Item) -> Boolean = allOf matchesMod itemIsEnchantable;

output predicate : Fn(Item) -> Boolean = acceptable;
```

- binding `modName` 必须是 String 变量/可证明的 String；不能把 namespace 当显示名。
- 直接展开约 6 张卡：itemMod、eq、apply(eq, modName)、pipe、itemIsEnchantable、conjunction；输出指向 conjunction。
- `allOf` 对同一输入短路两个谓词；不是 Boolean `&&`。
- 该输出卡可插入接受 `Fn(Item)->Boolean` 的原生槽。

### 8.2 动态选择函数

```haskell
input enabled : Boolean;
let fallback : Fn(Item) -> Boolean = constant false;
let predicate : Fn(Item) -> Boolean = if enabled then itemIsEnchantable else fallback;
output filterFn : Fn(Item) -> Boolean = predicate;
```

- 直接展开约 5 张卡：general_constant、false、apply(constant,false)、itemIsEnchantable、choice；`enabled` 既有变量。
- `constant false` 的剩余参数类型由 `Fn(Item)->Boolean` 上下文特化；如果没有上下文会报 `E_UNRESOLVED_TYPE`。
- 不在编译期对库存应用函数；Choice 选择 Operator 值，消费者再应用。

### 8.3 Delayer 历史

```haskell
input history : [Boolean];
input start : Boolean;
input stop : Boolean;

let previous = getOr history 0 false;
output next : Boolean = if stop then false else start || previous;
```

- `history` 绑定 Delay 卡；describe 沿 Delay 输入槽证明 `[Boolean]`。空槽/未知类型会失败，不能靠源码注解。
- 制卡后，把 `next` 卡接入 Delay 读槽（手动）；`history` 卡也需在变量仓库。
- 初始历史为空时 `getOr` 返回 false；`stop` 优先。
- Delay 容量/间隔/采样顺序不由 DSL 设置。

### 8.4 无限列表与切片

```haskell
let naturals = iterate 0 increment;
output firstTen : [Integer] = slice naturals 0 10;
```

- 直接展开：0、increment、lazybuilt、0、10、slice 等卡；输出为 slice 调用卡。
- `iterate` 是 infinite；`slice` 有有限边界，得到 finite `[Integer]`。
- 不要写 `filter`/`reduce`/`itemCount` 直接消费 `iterate`：`E_INFINITE_CONSUMER`。`length (iterate...)` 原生返回 2147483647，不是错误；避免依赖这个值做正常业务判断。

## 9. 常见错误与排查

| 现象/代码 | 原因 | 处理 |
|---|---|---|
| `E_INPUT_BINDING` | binding 缺失/多余、变量 ID 不在网络、part id/aspect 不匹配、Proxy/Delay 空槽/环、List/Fn 无法静态证明 | 检查网络/part/卡是否放入；不要用源码注解补类型；Delay 先放可证明的输入卡 |
| `E_TYPE` / `E_UNRESOLVED_TYPE` | 数字混型、if 分支不同、空列表无上下文、Fn 未特化 | 用 `L`/显式转换/注解；给空列表类型；给 Fn 上下文 |
| `E_NATIVE_CONTRACT` | operator/aspect 不存在、用显示名、可变签名 apply/apply0 当 native、扩展无固定合同 | 用注册 ID/普通应用/内建 `call0`；固定高阶 scheme 可以特化，扩展需显式合同 |
| `E_INFINITE_CONSUMER` / `W_UNKNOWN_FINITENESS` | filter/reduce/itemCount 消费已知 infinite / 无法证明有限的列表 | 先 slice 到有限边界；绑定有限列表；不要依赖 map/slice 后的原生 isInfinite |
| `E_LITERAL_RESOURCE` | Item/Block/Fluid ID 不存在，或 `item "minecraft:air"`／`fluid "minecraft:empty"` 产生空原型 | 修正资源；空值用 `emptyItem`/`emptyFluid`，Block 的 air 是合法默认状态 |
| `E_LITERAL_RANGE` | Integer/Long 越界、Fluid 数量≤0、静态可判无效的 slice 边界（包括部分应用的负起点） | 修正数值／数量；slice 要求 `0 <= from < to`，get/getOr 索引保留原生运行期语义 |
| `E_LITERAL_NBT` | SNBT 语法错误、内部递归超限或尾随文本 | 修正 SNBT；递归预算 32，文本 8192 bytes 超限另报 `E_LIMIT` |
| `E_LIMIT` | 源/节点/类型/输入输出/列表/字面量或外部 facade 证明工作超限 | 拆分程序或 bindings；简化列表/依赖图 |
| `E_TARGET_CONTRACT` / `E_TYPE` / `E_NATIVE_CONTRACT` / `E_INPUT_BINDING` | plan schema/ref/type/operator/contract 或 preflight/create 失败 | 重新 `id_compile`；检查空白卡/背包；不要手改 plan |
| `E_TARGET_CONTRACT`（phase=preflight） | 空白卡不足或主背包放不下 | 清背包、准备足够空白卡；aspect import 也算卡 |
| `E_TARGET_CONTRACT`（phase=create） | 写卡/背包/事件异常，或卡不是真正空白 | 检查已创建卡/ID；用无 NBT 空白卡；不要盲目整批重试 |
| `E_TYPE`，比较函数或函数列表 | Fn 不提供 DSL 相等性，包括嵌套列表 | 比较函数的输入／输出数据，不比较算子对象身份 |
| “variable not in network” | 卡没放入仓库/依赖卡缺失/网络不同/未刷新 | 把所有依赖卡放同一网络仓库；检查 part/aspect 卡 |
| 卡显示 invalid/错误 | 卡被移动、part/proxy 丢失、类型不匹配 | 检查 partId/proxyId/网络；重新 describe |
| `itemCount` 结果 0 | 查询物品空/不同 NBT/列表中无匹配 | 检查 item 字面量与库存 NBT |
| `itemMod` 结果不是 namespace | 原生返回 Mod 显示名（minecraft→Minecraft） | 用显示名比较；需要 namespace 用其他原生操作/合同 |
| `slice` 运行错误 | from>=to/负索引 | 保证 from<to 且非负；字面量会编译期报错 |
| 只建了部分卡 | 写卡中途失败，ID 已消耗 | 查看错误 node/输入与已创建列表；不要重复覆盖 |

## 10. 源码持久化与隐私

机制 RPC 边界：

- `id_describe_inputs`/`id_create_cards` **不携带** source、AST、sourceMap；服务端不解析/执行 DSL，只用 plan。
- 计划本身不含源码；服务端错误只带 phase/node/input。

但共享会话边界不同：

- 现有 `SharedContextCodec` 会把模型 `ToolCall.arguments`（含 `source`）作为对话上下文保存到服务端世界存档。因此不能笼统声称“服务端从不接收/保存 DSL 源码”。
- 这段 source 是不透明对话数据，不参与卡片、plan 校验或 ID 分配；沿用现有隐私/容量合同。
- 不要把密钥／敏感信息写进 source；机制 RPC 无源码不等于服务器不可见或加密。若不应保存到持久化会话，就不要经该会话调用工具；清理历史不承诺安全擦除世界备份。

## 11. 限制速查

| 项 | 值 |
|---|---:|
| 源文件 | 32768 UTF-8 bytes |
| 类型深度 | 16 |
| 声明 / 解析、AST 与跨 let 检查／降 IR 遍历深度 | 1024 / 128 |
| 共享 typed IR nodes | 8192 |
| plan nodes（实体定义卡） | 256 |
| inputs/imports | 64 |
| outputs | 64 |
| 单个字符串/NBT 字面量 | 8192 UTF-8 bytes |
| SNBT 值递归深度（根值为 0） | 32 |
| 既有 facade 证明引用深度（根为 0） / 每请求 facade 展开额度 | 32 / 4096 |
| 列表字面量元素 | 256 |
| 不同 value 卡的物化 JSON 节点合计 | 4096 |
| 完整 plan UTF-8 bytes / 原始 JSON 物理深度 | 262144 / 64 |
| 一次制卡的主背包物理上限 | 36 张新卡；还需有可用空白卡与可释放槽 |
| binding/input 名称（后端） | 64 UTF-8 bytes |
| 诊断偏移 | UTF-16 offset，行列 1 起 |

## 12. 未验证事项

- 未启动游戏；未实际制卡、拆空白卡、放入变量仓库、接部件、验证 Delayer 反馈。
- 构建、纯编译与 ToolManager 错误路径已离线执行；Minecraft 实体库存与原生制卡事件依赖 Forge 启动环境，未用离线结果冒充实测。界面只验证本地化格式化输出，未启动实际游戏界面。
- 未验证所有扩展 aspect/operator；只支持固定核心表和显式合同。
- 未验证多人/跨维度 GlobalCounters 时序；实现必须只在服务端分配 ID。
