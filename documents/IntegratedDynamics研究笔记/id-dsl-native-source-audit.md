# ID DSL 原生源码校审（1.31.3 / 69ea239d）

## 0. 范围与结论

本文记录 ID DSL 编译器/制卡实现直接依赖的原生事实。目标固定为 Minecraft 1.20.1 / Forge 47.0.1 / Integrated Dynamics `1.31.3`，commit [`69ea239d03828fc76b0384dcc487af391fd9e6bf`](https://github.com/CyclopsMC/IntegratedDynamics/tree/69ea239d03828fc76b0384dcc487af391fd9e6bf)。CyclopsCore 对应源码版本为 `1.20.0`（tag [`1.20.1-1.20.0`](https://github.com/CyclopsMC/CyclopsCore/tree/1.20.1-1.20.0)）。

本轮的“已验证”仅指**静态阅读目标 commit 源码**。没有启动游戏、没有执行 Gradle、没有在真实世界制卡或放置卡，也没有客户端/服务端运行结果。文中行为若来自方法实现即视为源码事实；未运行验证的在设计文档中单独标注。

关键修正（相对于旧 DSL 规范）：

- 原生卡不是手写 NBT：`_type`/`_id` 由 `VariableFacadeHandlerRegistry` 写入；算子/值/列表都有 registry 序列化器。
- 变量 ID 是世界存档级 `GlobalCounters` 计数器，不是网络内编号；客户端不能分配。
- Aspect/Proxy/Delay/部分调用的 facade output type 不是完整静态类型；DSL 必须用固定表、常量 value metadata、facade 依赖图和网络对象引用做保守静态证明，绝不求值活变量。
- 部分应用不能落成“Operator value 卡”保存动态绑定；原生 `CurriedOperator.Serializer` 会对已绑定变量调用 `getValue()` 快照。动态部分应用必须是 `operator_apply` 调用卡。
- 变量仓库不做类型校验；卡不在仓库/网络时引用不可解析；制卡与放置是分离步骤。
- 原生列表 `isInfinite` 的传播不完整（map/slice/append 不继承），DSL 必须自己跟踪有限性；服务端对不可证明有限的无限消费者必须拒绝。

## 1. 卡片载体、空白判定与写入

### 1.1 物品

- 变量卡物品注册名 `integrateddynamics:variable`，`RegistryEntries.ITEM_VARIABLE` 为 `ItemVariable`（[RegistryEntries.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/RegistryEntries.java#L56-L60)）。
- `ItemVariable` 通过 `VariableFacadeHolderConfig.CAPABILITY` 读取根 NBT；该 capability 对任意变量物品都存在（[ItemVariable.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/item/ItemVariable.java#L64-L70)）。
- 物品使用默认 `new Item.Properties()`，即最大堆叠 64（[ItemVariableConfig.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/item/ItemVariableConfig.java#L25-L31)）。变量仓库的槽位则强制 stack=1（[BlockEntityVariablestore.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityVariablestore.java#L38-L60)）。

### 1.2 空白判定

`VariableFacadeHandlerRegistry.handle(ItemStack)`：

- `itemStack.isEmpty() || !hasTag()` → `DUMMY_FACADE`。
- `handle(CompoundTag)` 要求 `_type` 为字符串且 `_id` 为 int；缺任一项 → `DUMMY_FACADE`；`_type` 未注册 → `DUMMY_FACADE`。
- `DUMMY_FACADE.isValid() == false`。

来源：[VariableFacadeHandlerRegistry.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/VariableFacadeHandlerRegistry.java#L86-L107)。

因此“真正空白”定义为：物品是 `integrateddynamics:variable`，且 NBT 不同时含 `_type` 与 `_id` 两个键。只有 `_type` 或只有 `_id` 的卡是 invalid，不是空白；制卡不得覆盖。仅无 NBT 或仅有显示名等无关 NBT 的卡可视为空白，但服务端应通过解析 facade 再次确认 `isValid()==false` 且不是已编程卡。

### 1.3 写入 API

`IVariableFacadeHandlerRegistry` 提供：

- `void write(CompoundTag tag, F facade, IVariableFacadeHandler<F> handler)`：写 `_type`/`_id` 和 handler payload。
- `ItemStack writeVariableFacadeItem(ItemStack stack, F facade, handler)`：复制 stack，直接写给定 facade（不生成 ID）。
- `ItemStack writeVariableFacadeItem(boolean generateId, ItemStack stack, handler, IVariableFacadeFactory<F> factory, Level level, @Nullable Player player, @Nullable BlockState blockState)`：复制 stack，创建 facade；若 `generateId` 且旧卡有 `_id>-1`，**复用旧 ID**；否则按 `generateId` 生成。会 post `LogicProgrammerVariableFacadeCreatedEvent`（若 player 非空）。
- `F writeVariableFacade(boolean generateId, ...)` 同上但不写 stack。

来源：[IVariableFacadeHandlerRegistry.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/api/item/IVariableFacadeHandlerRegistry.java#L55-L120)、[VariableFacadeHandlerRegistry.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/VariableFacadeHandlerRegistry.java#L108-L143)。

**堆叠陷阱**：`writeVariableFacadeItem` 是 `itemStack.copy()` 后写整个 stack 的 NBT，不拆分数量。对 count>1 的空白堆写卡会得到 N 张同 ID 的已编程卡。制卡必须先从空白堆取出 count=1 的一份，再写卡。

### 1.4 Handler 名称与 facade 构造器

`_type` 精确值（`getUniqueName()`）：

| `_type` | Handler | Facade 构造器 |
|---|---|---|
| `integrateddynamics:valuetype` | `ValueTypes.REGISTRY`（`ValueTypeRegistry`） | `new ValueTypeVariableFacade(boolean gen, IValueType<V>, V)` |
| `integrateddynamics:operator` | `Operators.REGISTRY`（`OperatorRegistry`） | `new OperatorVariableFacade(boolean gen, IOperator, int[])` |
| `integrateddynamics:aspect` | `Aspects.REGISTRY`（`AspectRegistry`） | `new AspectVariableFacade(boolean gen, int partId, IAspect)` |
| `integrateddynamics:proxy` | `ProxyVariableFacadeHandler.getInstance()` | `new ProxyVariableFacade(boolean gen, int proxyId)` |
| `integrateddynamics:delay` | `DelayVariableFacadeHandler.getInstance()` | `new DelayVariableFacade(boolean gen, int proxyId)` |

来源：[ValueTypeRegistry.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeRegistry.java#L99-L128)、[OperatorRegistry.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/OperatorRegistry.java#L163-L193)、[AspectRegistry.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/part/aspect/AspectRegistry.java#L173-L199)、[ProxyVariableFacadeHandler.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/ProxyVariableFacadeHandler.java#L27-L47)、[DelayVariableFacadeHandler.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/DelayVariableFacadeHandler.java#L27-L47)。

Payload：

- value：`typeName`（字符串，value type 的 `getUniqueName()`）、`value`（raw Tag，`ValueHelpers.serializeRaw`）。
- operator call：`operatorName`（Tag，`Operators.REGISTRY.serialize`）、`variableIds`（int array）。
- aspect：`partId`（int）、`aspectName`（字符串，aspect `getUniqueName()`）。
- proxy/delay：`partId`（int）。注意 key 是 `partId`，值是 proxyId/delayId。

### 1.5 ID 分配

`VariableFacadeBase(boolean generateId)` 在 `generateId=true` 时调用 `generateId()`：

```java
return IntegratedDynamics.globalCounters.getNext("variable");
```

`IntegratedDynamics` 在构造时把 `globalCounters = new GlobalCounters(this)` 注册为 world storage（[IntegratedDynamics.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/IntegratedDynamics.java#L100-L110)、[VariableFacadeBase.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/VariableFacadeBase.java#L29-L39)）。

CyclopsCore `GlobalCounters`（tag `1.20.1-1.20.0`）：

- world storage，跨所有维度共享；
- `getNext(key)` 取当前值、存 `next+1`、返回当前值；空时从 0 开始；
- `synchronized`；溢出时 `incr<0` 归 0；
- 注释称“persisted, and consistent over server and clients”。

来源：[GlobalCounters.java](https://github.com/CyclopsMC/CyclopsCore/blob/1.20.1-1.20.0/src/main/java/org/cyclops/cyclopscore/persist/world/GlobalCounters.java)。

结论：ID 是世界级、跨维度、持久化的计数器；客户端绝不能调用 `generateId()` 分配；服务端失败后已分配的 ID 不会回滚。

## 2. 原生值 type 与序列化

### 2.1 具体 value type id

`ValueTypeBase.getUniqueName() = new ResourceLocation(modId, getTypeName())`（modId=`integrateddynamics`）。核心注册：

| 界面/DSL 类型 | 原生 id | 注册位置 |
|---|---|---|
| Boolean | `integrateddynamics:boolean` | `ValueTypes.BOOLEAN` |
| Integer | `integrateddynamics:integer` | `ValueTypes.INTEGER` |
| Long | `integrateddynamics:long` | `ValueTypes.LONG` |
| Double | `integrateddynamics:double` | `ValueTypes.DOUBLE` |
| String | `integrateddynamics:string` | `ValueTypes.STRING` |
| NBT | `integrateddynamics:nbt` | `ValueTypes.NBT` |
| List | `integrateddynamics:list` | `ValueTypes.LIST` |
| Operator | `integrateddynamics:operator` | `ValueTypes.OPERATOR` |
| Item | `integrateddynamics:itemstack` | `ValueTypes.OBJECT_ITEMSTACK` |
| Block | `integrateddynamics:block` | `ValueTypes.OBJECT_BLOCK` |
| Entity | `integrateddynamics:entity` | `ValueTypes.OBJECT_ENTITY` |
| Fluid | `integrateddynamics:fluidstack` | `ValueTypes.OBJECT_FLUIDSTACK` |
| Ingredients | `integrateddynamics:ingredients` | `ValueTypes.OBJECT_INGREDIENTS` |
| Recipe | `integrateddynamics:recipe` | `ValueTypes.OBJECT_RECIPE` |

类别（不是 DSL 可写类型）：`any`、`number`、`named`、`uniquely_named`、`nullable`，均注册为 `integrateddynamics:<name>`（[ValueTypes.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypes.java#L14-L37)、[ValueTypeBase.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeBase.java#L53-L59)）。

### 2.2 raw value NBT

`ValueHelpers.serializeRaw(value)` 直接调用 `value.getType().serialize(value)`；`serialize(value)` 外包 `{valueType,value}`（DSL 的 value 卡用 raw `value`，typeName 单独存）。

| 类型 | raw Tag |
|---|---|
| Boolean | `ByteTag`，true=1 / false=0 |
| Integer | `IntTag` |
| Long | `LongTag` |
| Double | `DoubleTag` |
| String | `StringTag` |
| NBT | `CompoundTag`，非空时 `{v:<Tag>}`；空为无 `v` 的 CompoundTag |
| Item | `ItemStack.save(tag)` + `Count` int；空栈为无键 CompoundTag |
| Block | `BlockHelpers.serializeBlockState(state)`；空为无键 CompoundTag |
| Fluid | `FluidStack.writeToNBT(tag)` |
| Operator | `Operators.REGISTRY.serialize(operator)`；普通原生 operator 为 StringTag 唯一名；组合/curried 为 CompoundTag `{serializer,value}` |
| List | `{proxyName:"integrateddynamics:<factory>", serialized:<factory tag>}`；materialized factory 的 serialized 为 `{valueType:"<element type id>", values:[<raw>...]}` |

来源：[ValueHelpers.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueHelpers.java#L203-L235)、[ValueTypeBoolean.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeBoolean.java#L35-L50)、[ValueTypeInteger.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeInteger.java#L39-L53)、[ValueTypeLong.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeLong.java#L38-L52)、[ValueTypeDouble.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeDouble.java#L38-L52)、[ValueTypeString.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeString.java#L34-L46)、[ValueTypeNbt.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeNbt.java#L42-L60)、[ValueObjectTypeItemStack.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueObjectTypeItemStack.java#L61-L89)、[ValueObjectTypeBlock.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueObjectTypeBlock.java#L55-L69)、[ValueObjectTypeFluidStack.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueObjectTypeFluidStack.java#L48-L65)、[ValueTypeList.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeList.java#L51-L80)、[ValueTypeListProxyMaterializedFactory.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyMaterializedFactory.java#L30-L100)。

### 2.3 列表的静态元素类型

- `ValueTypeList.ValueList.getRawValue()` 是 `IValueTypeListProxy`；`IValueTypeListProxy.getValueType()` 是**元素类型**，对 materialized/positioned proxy 是构造器传入的具体类型，读取它是纯静态 metadata，不需要求值或读世界。
- DSL 写 value 列表时必须使用具体元素类型，即使列表为空：`ValueList.ofList(elementType, [])` → materialized proxy `{valueType:<concrete>,values:[]}`。`ValueTypes.LIST.getDefault()` 是 `CATEGORY_ANY` 空表，不能用于需要 `[T]` 的静态证明。
- 原生 value type `integrateddynamics:list` 本身不携带元素类型；元素类型只在 list proxy 内。

来源：[IValueTypeListProxy.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/api/evaluate/variable/IValueTypeListProxy.java#L12-L49)、[ValueTypeListProxyMaterialized.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyMaterialized.java#L20-L42)。

## 3. 变量仓库与网络可见性

- `BlockEntityVariablestore`：5×9=45 格，构造 `super(..., INVENTORY_SIZE, 1)`，即每格 stack=1；`canPlaceItem` 只要求变量 facade capability 存在（所有变量物品都有），空白卡也可放入。
- `read/onLoad/onDirty` 触发 `variableContainer.refreshVariables(network, inventory, sendEvent, ctx)`；服务端 dirty 时刷新并 post `VariableContentsUpdatedEvent`。
- `VariableContainerDefault` 清空 cache，遍历库存，用 `RegistryEntries.ITEM_VARIABLE.getVariableFacade(...)` 解析；只把 `isValid()` 的 facade 以 `_id` 为 key 放入 cache。若没有任何有效 facade，则把第一个 invalid facade 以 id（通常是 -1）放入，使空卡解析为 true（dummy 行为）；这不是类型校验。
- `PartNetwork.getVariableCache()` 是各变量容器 cache 的 composite view；`hasVariableFacade(id)`/`getVariableFacade(id)` 只查该 composite。卡在玩家背包时不在 cache，引用不可解析。
- 变量仓库本身**不验证**卡片的类型/依赖/环；这些在引用方使用 `validate/getVariable` 时才暴露。

来源：[BlockEntityVariablestore.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityVariablestore.java#L36-L90)、[VariableContainerDefault.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/capability/variablecontainer/VariableContainerDefault.java#L36-L80)、[PartNetwork.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/network/PartNetwork.java#L113-L160)。

网络/part 解析 API：

- `NetworkHelpers.getNetwork(world,pos,side)` → `LazyOptional<INetwork>`；
- `NetworkHelpers.getPartNetwork(INetwork or LazyOptional)` → `LazyOptional<IPartNetwork>`；
- `PartNetwork.hasPartVariable(partId, IAspectRead)` 检查 part 状态/target/aspect 是否可读；
- `PartNetwork.getPartType(partId).getUniqueName()` 可比对 part 类型；part id 是网络局部。

来源：[NetworkHelpers.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/helper/NetworkHelpers.java#L77-L165)、[PartNetwork.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/network/PartNetwork.java#L64-L118)。

## 4. Operator registry 名称与高阶语义

`OperatorBase.getUniqueName = new ResourceLocation(modId, unlocalizedType.replace('.', '_') + "_" + operatorName)`；Builder 的 `unlocalizedType` 是 kind 链以 `.` 连接（[OperatorBase.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/OperatorBase.java#L54-L63)、[OperatorBuilder.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/build/OperatorBuilder.java#L240-L275)）。

实际唯一名（全部加 `integrateddynamics:`）：

| Java 常量 | 唯一名 | 参数/输出（native 声明） |
|---|---|---|
| LOGICAL_AND/OR/NOT | `logical_and`, `logical_or`, `logical_not` | Boolean |
| ARITHMETIC_ADDITION/SUBTRACTION/MULTIPLICATION/DIVISION/MODULUS/MAXIMUM/MINIMUM/INCREMENT/DECREMENT | `arithmetic_*`（addition/subtraction/multiplication/division/modulus/maximum/minimum/increment/decrement） | [NUMBER...]→NUMBER |
| RELATIONAL_EQUALS | `relational_equals` | [ANY,ANY]→Boolean |
| RELATIONAL_NOTEQUALS | `relational_notequals` | 组合 `!equals` → Boolean |
| RELATIONAL_GT/LT | `relational_gt`, `relational_lt` | [NUMBER,NUMBER]→Boolean |
| RELATIONAL_GE/LE | `relational_ge`, `relational_le` | 组合 `or(equals,gt/lt)` |
| NUMBER_CAST_INTEGER/LONG/DOUBLE | `number_cast_integer/long/double` | [NUMBER]→具体 |
| STRING_CONCAT | `string_concat` | [String,String]→String |
| LIST_LENGTH/EMPTY | `list_length`, `list_empty` | [LIST]→Integer/Boolean |
| LIST_ELEMENT | `list_get` | [LIST,Integer]→ANY（conditional=元素类型） |
| LIST_ELEMENT_DEFAULT | `list_get_or_default` | [LIST,Integer,ANY]→ANY（conditional=第 3 个类型） |
| LIST_APPEND | `list_append` | [LIST,ANY]→LIST |
| LIST_SLICE | `list_slice` | [LIST,Integer,Integer]→LIST |
| LIST_CONCAT | `list_concat` | [LIST,LIST]→LIST |
| LIST_LAZYBUILT | `list_lazybuilt` | [ANY,OPERATOR]→LIST |
| OPERATOR_APPLY | `operator_apply` | [OPERATOR,ANY]→ANY |
| OPERATOR_APPLY_0 | `operator_apply0` | [OPERATOR]→ANY |
| OPERATOR_MAP/FILTER | `operator_map`, `operator_filter` | [OPERATOR,LIST]→LIST |
| OPERATOR_CONJUNCTION/DISJUNCTION | `operator_conjunction`, `operator_disjunction` | [OPERATOR,OPERATOR]→OPERATOR |
| OPERATOR_NEGATION | `operator_negation` | [OPERATOR]→OPERATOR |
| OPERATOR_PIPE | `operator_pipe` | [OPERATOR,OPERATOR]→OPERATOR |
| OPERATOR_PIPE2 | `operator_pipe2` | [OPERATOR,OPERATOR,OPERATOR]→OPERATOR |
| OPERATOR_FLIP | `operator_flip` | [OPERATOR]→OPERATOR |
| OPERATOR_REDUCE | `operator_reduce` | [OPERATOR,LIST,ANY]→ANY（conditional=第 3 个类型） |
| GENERAL_CHOICE/IDENTITY/CONSTANT | `general_choice`, `general_identity`, `general_constant` | [Boolean,ANY,ANY]→ANY / [ANY]→ANY / [ANY,ANY]→ANY |
| OBJECT_ITEMSTACK_LIST_COUNT | `itemstack_itemlistcount` | [LIST,Item]→Integer |
| OBJECT_ITEMSTACK_MODNAME | `itemstack_mod` | [Item]→String |
| OBJECT_ITEMSTACK_ISSTACKABLE | `itemstack_stackable` | [Item]→Boolean |
| OBJECT_ITEMSTACK_ISENCHANTABLE | `itemstack_enchantable` | [Item]→Boolean |

来源见 [Operators.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java) 对应常量行；`CompositionalOperator` 的唯一名由 `build(..., unlocalizedType)` 决定（[CompositionalOperator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/CompositionalOperator.java#L153-L188)）。

### 4.1 Apply/Curry

`ValueHelpers.evaluateOperator(operator, variables)`：

- 参数数 > 原生 required：先 apply 前 required 个，结果必须是 OPERATOR，再应用到剩余参数；否则 `OPERATOR_ERROR_CURRYINGOVERFLOW`。
- 参数数 < required：返回 `ValueOperator.of(new CurriedOperator(operator, variables))`。
- 相等：直接 `operator.evaluate`。

来源：[ValueHelpers.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueHelpers.java#L144-L183)。

`operator_apply` 通过 `FUNCTION_OPERATOR_TAKE_OPERATOR` 取第一个 input 为 operator、第二个为 variable，调用上述方法；`operator_apply0` 用空变量数组。`operator_apply2/3/apply_n` 是多个参数/列表版本。

`CurriedOperator` 本身实现 `getInputTypes()` = base 剩余的输入类型、`getOutputType()` = base output、`getRequiredInputLength()` = 剩余数。它的 Serializer（用于 operator value 卡）会对每个 applied variable 调 `getValue()`，把值写进 `values`；**动态绑定会被快照**。来源：[CurriedOperator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/CurriedOperator.java#L150-L260)。

结论：DSL 的部分应用使用 `call operator_apply` + base `native` 节点，而不是保存 CurriedOperator 的 value 卡。

### 4.2 Choice/Constant/Identity

- `general_choice` 输入 `[Boolean, ANY, ANY]`；`evaluate` 仅读取选中的分支；`validateTypes` 要求第 1、2 分支类型相互对应；`getConditionalOutputType` 返回第 1 个分支（then）的类型。来源：[GeneralChoiceOperator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/GeneralChoiceOperator.java#L19-L73)。
- `general_constant` 输入 `[ANY, ANY]`，function 为 `variables -> variables.getValue(0)`；conditional output 是第 0 个输入类型；第二个参数不被读取。来源：[GeneralConstantOperator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/GeneralConstantOperator.java#L12-L43)。
- `general_identity` `[ANY]→ANY`，conditional output 是输入类型。

### 4.3 Pipe/Pipe2/Flip

`CombinedOperator.Pipe.pipeVariablesToOperators(allVariables, operators)`：

- 最后一个 operator 是目标，前面 n-1 个先以 `allVariables[0]` 为唯一输入求值，结果包装成 `new Variable<>(...)`；
- 然后把 intermediates + 剩余输入传给最后一个 operator。
- `getPipedInputOutputTypes`：输入类型 = 第一个 operator 的第 0 个输入类型，加上最后一个 operator 从 index `operators.length-1` 开始的输入类型；输出 = 最后一个 operator output（若 OPERATOR 则 ANY）。
- 所以 `pipe f g` 对 unary f/g 是 `g(f(x))`；g 可以有额外参数；f 的输出只作为 g 的第一个输入。f、g 的中间结果**先求值**。

`Pipe2` 复用 `Pipe.pipeVariablesToOperators`，`asOperator` 使用 `Pipe` 的实现；`FUNCTION_THREE_OPERATORS` 要求第三个 operator 至少有 2 个输入，并以前两个输入类型约束前两个 operator；第三个的其余输入成为结果的额外参数。

`Flip` 要求 operator 输入数 ≥2；交换前两个类型，`evaluate` 交换前两个变量的读取顺序；多于 2 个输入时其余不变。

来源：[CombinedOperator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/CombinedOperator.java#L245-L440)、[OperatorBuilders.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/OperatorBuilders.java#L229-L271)。

### 4.4 Map/Filter/Reduce/List

- `operator_map` 使用 `ValueTypeListProxyOperatorMapped`；operator 被逐个应用到列表元素；其 value type 为 operator 的 output（若 operator 恰 1 输入），否则为 OPERATOR。虽然输出 list 元素类型在运行时确定，但 DSL 静态要求 unary。
- `operator_filter` 对无限列表（`list.getRawValue().isInfinite()`）抛错；对每个元素求谓词，要求 Boolean。
- `operator_reduce` 输入顺序 [operator, list, seed]；对每个元素调用 `evaluateOperator(inner, accumulator, listValue)`，要求 inner 恰 2 输入 [A,B]→A；无限列表抛错。
- `list_lazybuilt` 输入顺序 [seed, operator]，返回 `ValueTypeListProxyLazyBuilt`；`isInfinite()==true`；`getLength()` 返回 `Integer.MAX_VALUE`（不是抛错）；`get(index)` 逐次应用 operator。
- `list_slice` 要求 `from < to` 且非负，否则抛错；`ValueTypeListProxySlice.getLength()` 为 `min(baseLength,to)-from`，`isInfinite` 继承 false。
- `operator_map`/`slice`/`append`/`concat` 的 proxy 不覆写 `isInfinite`，因此原生 `isInfinite` 在 map/slice 后可能错误地返回 false；DSL 必须自行跟踪有限性，服务端不能只信原生 `isInfinite`。
- `itemstack_itemlistcount` 使用 `ItemStack.isSameItemSameTags` 累加 count；查询空 ItemStack 时 count=0；非 itemstack 元素/无限列表抛错。
- `itemstack_mod` 通过 `ForgeRegistries.ITEMS.getKey` 取 namespace，再映射 `ModList` 显示名；`minecraft` → `Minecraft`。不是 namespace/id。
- `itemstack_stackable`/`enchantable` 对空栈返回 false。

来源：[Operators.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/operator/Operators.java#L938-L1288)、[ValueTypeListProxyLazyBuilt.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyLazyBuilt.java#L35-L60)、[ValueTypeListProxySlice.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxySlice.java#L27-L42)、[ValueTypeListProxyOperatorMapped.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyOperatorMapped.java#L24-L40)、[ValueTypeListProxyBase.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyBase.java#L80-L90)。

## 5. Aspect 静态类型表

`AspectVariableFacade.getOutputType()` 只返回 `aspect.getValueType()`；`IAspectRead.getValueType()` 对列表方面是 `ValueTypes.LIST`，不含元素类型。列表方面必须用固定表（元素类型来自创建 proxy 的构造器）：

| aspect unique name | 注册 part | 元素类型 | 有限性 | 证据 |
|---|---|---|---|---|
| `integrateddynamics:list_inventory_itemstacks` | `integrateddynamics:inventory_reader` | `integrateddynamics:itemstack` | finite | `ValueTypeListProxyPositionedInventory` 构造传 `ValueTypes.OBJECT_ITEMSTACK` |
| `integrateddynamics:list_fluid_fluidstacks` | `integrateddynamics:fluid_reader` | `integrateddynamics:fluidstack` | finite | `ValueTypeListProxyPositionedTankFluidStacks` |
| `integrateddynamics:list_fluid_capacities` | `integrateddynamics:fluid_reader` | `integrateddynamics:integer` | finite | `ValueTypeListProxyPositionedTankCapacities` |
| `integrateddynamics:list_entity_entities` | `integrateddynamics:entity_reader` | `integrateddynamics:entity` | finite | `ValueList.ofList(OBJECT_ENTITY, ...)` |
| `integrateddynamics:list_entity_players` | `integrateddynamics:entity_reader` | `integrateddynamics:entity` | finite | 同上 |
| `integrateddynamics:list_extradimensional_players` | `integrateddynamics:extradimensional_reader` | `integrateddynamics:entity` | finite | 同上 |
| `integrateddynamics:list_world_players` | `integrateddynamics:world_reader` | `integrateddynamics:entity` | finite | 同上 |
| `integrateddynamics:list_recipehandler_recipes` | `integrateddynamics:machine_reader` | `integrateddynamics:recipe` | finite | `ValueTypeListProxyPositionedRecipes` 传 `OBJECT_RECIPE` |

没有任何“energy list”方面。能源标量为 Integer：`integer_fe_amount` / `integer_fe_capacity`（`machine_reader`，FE 存储）以及 `integer_network_energy_stored` / `integer_network_energy_max` / `integer_network_energy_consumptionrate` / `integer_network_energy_batterycount`（`network_reader`）。标量可直接 `getValueType()` 证明。

来源：[Aspects.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/Aspects.java#L191-L260)、[Aspects.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/Aspects.java#L340-L470)、[Aspects.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/Aspects.java#L505-L520)、[Aspects.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/Aspects.java#L640-L670)、[Aspects.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/Aspects.java#L785-L800)、[AspectReadBuilders.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/read/AspectReadBuilders.java#L66-L76)、[AspectReadBuilders.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/part/aspect/read/AspectReadBuilders.java#L275-L300)、[ValueTypeListProxyPositionedInventory.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyPositionedInventory.java#L20-L31)、[ValueTypeListProxyPositionedTankFluidStacks.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyPositionedTankFluidStacks.java#L18-L27)、[ValueTypeListProxyPositionedTankCapacities.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyPositionedTankCapacities.java#L18-L27)、[ValueTypeListProxyPositionedRecipes.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeListProxyPositionedRecipes.java#L18-L28)。

所有上述 positioned proxy 的 `getLength()` 读真实容器/槽数，`isInfinite` 继承 base=false；因此静态有限性为 finite，但这只是列表长度有限，不证明容器当前有内容。

## 6. 常量 value facade 的静态 metadata

`ValueTypeVariableFacade` 在读取卡时反序列化 `value`；`IValueTypeVariableFacade` 暴露 `getValueType()`、`getValue()`：

- 标量：`getValueType()` 即具体类型；`getValue()` 是值。
- List：`getValue()` 为 `ValueTypeList.ValueList`；`getRawValue().getValueType()` 是元素类型。**不要调用 `getVariable().getValue()`**。
- Operator：`getValue()` 为 `ValueTypeOperator.ValueOperator`；`getRawValue()` 返回 `IOperator`，其 `getInputTypes()/getOutputType()` 是静态签名。
- Item/Block/Fluid/NBT：`getValue()` 已反序列化；只读类型安全。Block 反序列化在 facade 创建时已完成，静态类型不需要 world。

来源：[IValueTypeVariableFacade.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/api/item/IValueTypeVariableFacade.java#L12-L35)、[ValueTypeVariableFacade.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/ValueTypeVariableFacade.java#L50-L95)、[ValueTypeList.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeList.java#L96-L135)、[ValueTypeOperator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/variable/ValueTypeOperator.java#L150-L190)。

## 7. Operator call facade 的静态图

`OperatorVariableFacade`：

- `getOperator()` 返回反序列化的 `IOperator`；`getVariableIds()` 是有序输入 id。
- `getOutputType()` 只返回 `operator.getOutputType()`（声明值），不是 `getConditionalOutputType`；对 `operator_apply`/`list_get`/`map`/`pipe` 等会返回 ANY/LIST/OPERATOR，不能作为实际类型。
- `getVariable(network, partNetwork)` 才会构造变量/表达式；不要用它做 describe。

`PartNetwork.getVariableFacade(id)` 可静态拿到依赖 facade（如果已放入网络变量容器）。递归规则见设计文档 §9。未知/缺失/环/无法证明 → `E_INPUT_BINDING`。

## 8. Proxy / Delay

- Proxy facade `getProxyId()` → `PartNetwork.getProxy(proxyId)` → `DimPos` → `BlockEntityProxy`。Proxy 从库存 `SLOT_READ=0` 读取目标卡；`InventoryVariableEvaluator.getVariable` 会解析该卡。静态 describe 可读该槽卡 facade 并递归推断，不调用 `getVariable().getType()`。
- Delay 继承 Proxy；`BlockEntityDelay.getVariable(network)` 返回固定 `VariableAdapter`，`getType()` 恒为 `ValueTypes.LIST`，`getValue()` 返回当前历史 `list`。历史 `list` 由 ticker 用输入变量的 `value.getType()` 构造 `ValueList.ofList(...)`；所以元素类型 = delay 读槽卡片的静态类型。延迟容量/间隔不影响类型。
- Proxy/Delay 的 facade `getOutputType()` 均为 `CATEGORY_ANY`（ProxyVariableFacade 实现；Delay 继承）；不能直接信任。
- 需要在已授权网络内解析；循环 proxy/delay 引用必须检测并拒绝；目标槽空/类型不可证明 → 对相应绑定报 `E_INPUT_BINDING`。

来源：[ProxyVariableFacade.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/item/ProxyVariableFacade.java#L30-L110)、[BlockEntityProxy.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityProxy.java#L48-L120)、[BlockEntityDelay.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityDelay.java#L70-L95)、[BlockEntityDelay.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/blockentity/BlockEntityDelay.java#L176-L225)、[InventoryVariableEvaluator.java](https://github.com/CyclopsMC/IntegratedDynamics/blob/69ea239d03828fc76b0384dcc487af391fd9e6bf/src/main/java/org/cyclops/integrateddynamics/core/evaluate/InventoryVariableEvaluator.java#L90-L130)。

## 9. 已知的旧规范不实假设

| 旧说法 | 源码事实 | 设计处理 |
|---|---|---|
| 服务端完整验证后才分配真实 ID | 原生 ID 在 facade `generateId=true` 时立即分配；需要真实 ID 的原生 validate 只能之后 | 分 preflight 与 allocate/write；失败 ID 不回滚 |
| 制卡失败可回滚/全成功 | 原生写卡/库存事件无事务 | 预检容量/类型，失败保留已建卡并报告 |
| 源码长度与偏移都按 UTF-8 | Kotlin 字符串/编辑器按 UTF-16 | 源限制 UTF-8 bytes 32768；Range 用 UTF-16 offset，行列 1 起 |
| Fn/List 输入可信任注解 | facade output 对 list/operator/proxy 丢元素/签名；Proxy/Delay=ANY | describe 静态证明；不能证明报 E_INPUT_BINDING |
| 部分应用保存为 Operator value 卡 | CurriedOperator.Serializer 对 applied variable 调 getValue() 快照 | 动态部分应用编译为 operator_apply 调用节点 |
| map 继承有限性即可 | 原生 map/slice/append 的 isInfinite 不传播 | DSL 自行跟踪；服务端拒绝不可证明有限的无限消费者 |
| slice 任意 [start,end) | from>=to 抛错 | 字面量 from>=to 编译错误；动态运行期错误 |
| length(iterate) 抛错/无穷 | `LazyBuilt.getLength()=Integer.MAX_VALUE` | 文档明确返回 2147483647，不虚构错误 |
| 部署工具自动接 Delayer | 冻结职责不自动放置/改部件 | 用户手动接线；文档改为手动 |
| Java 常量名即注册名 | 注册名是 kind 链 + operatorName | 使用实际 `integrateddynamics:*` 表 |
| Aspect 的 list 元素类型可从 getValueType 得到 | 只返回 `integrateddynamics:list` | 固定 aspect 表 + part 匹配 |
| 变量仓库会验证卡类型 | 只缓存 valid facade，不验类型 | 服务端 preflight + 使用期原生错误 |

## 10. 未验证事项

- 未启动 Minecraft/Forge，未实际调用 `writeVariableFacadeItem`、未真实制卡、未放入变量仓库、未连接部件。
- 未运行 Gradle/测试；未验证客户端 tool 与 backend 的实际网络包/UI。
- 未验证所有扩展 Mod 的 aspect/operator 注册；本文只覆盖固定版本核心。
- 未验证实际 `ItemStack.save` 在 1.20.1 的具体 Tag 字段细节；实现应使用 native API 生成，不手写 NBT。
- 未验证跨维度/多人并发下 GlobalCounters 的实际同步时序；实现必须只在服务端分配。
