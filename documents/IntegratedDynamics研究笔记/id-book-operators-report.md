# operators group 报告（只读 scout；完整交付如下，未写任何文件）

父任务校审：325 是在线 1.21.1 环境的目录，不是 1.20.1 注册表承诺。1.20.1 无此书的 Encode/Decode 三个原语，且部分 Item/Fluid data-component 原语与 NBT 原语不同；详见 [总报告](local://id-book-research.md)。完整机器目录保留显示签名，不能将显示名或交互名直接当注册 ID。

## 1. 摘要（结论优先）

- 分配来源文件 `local://id-book-operators-sources.md` **只含 1 个 URL**：`https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html`（全文 5109 行，已 100% 读完）。辅助目录 `local://id-book-operators.json`（4712 行）为 **325 条唯一显示签名**，已 100% 读完。
- **325 条=核心 298 + 扩展(Mekanism Chemical) 27**。核心 298 条全部来源 `manual/logic_programming.html`（少数若干条同时也被教程页引用，见 §7 provenance）。
- 运算符模型：内建 Operator 有有序输入与一个输出；可存在零参数函数，不能一概要求至少一个参数。单输入、Boolean 输出者称 Predicate。Global name 与 Method name 是文档/脚本交互名，独立于实际注册 ID。
- Operator 是**一等值**（类型 `Operator`）：`Pipe/.2/Flip/.&&./.||./!./Map/Filter/Reduce/Reduce1/Apply*` 构造与调用之；`Filter/Map/Reduce/Contains_P/Count_P/Uniq_P/Lazy Builbuilt` 以 Operator 作参数。
- 明确文档化的错误/边界只有少数：`listGet`「索引越界抛错」、`listGetOrDefault` 安全返回默认值、`error` 运算符「抛出显式错误」、`Equals/NotEquals` 要求两侧类型相同、`Choice` 要求第 2/3 参类型相同。**除零、负数 sqrt、空列表 head/tail/reduce、非法 regex、lazy 无限列表终止**——文档**未描述**，不得编造。
- 关键版本疑点（须父任务按 master-1.20-lts 源码核查）：Encode/Decode 家族、NBT path 家族、**"data component" 语义（1.20.5+/1.21 概念，1.20.1 为 NBT tag）**、Number 抽象类型与 ItemStackMax、Lazy List Builder 的动态/无限输出、Chemical 属扩展而非核心。

## 2. 文件与产物覆盖

| 产物 | 说明 | 状态 |
|---|---|---|
| `local://id-book-operators-sources.md` | logic_programming 全页正文（1 URL），含叙述段 + 全部运算符目录 | 已完整读（5109/5109 行） |
| `local://id-book-operators.json` | 325 唯一签名机器目录（name/symbol/inputs/output/interact_signatures/source） | 已完整读（4712/4712 行） |

**分配 URL 覆盖（1/1）**：
| URL | 已读 | 重要性 | 版本问题 |
|---|---|---|---|
| `https://integrateddynamics.rubensworks.net/book/manual/logic_programming.html` | ✅ 全文 | 极高（DSL 全部原语清单） | 在线书为 MC1.21.1/ID1.38.0；须比对 1.20.1 |

引用/说明遗漏：`-sources.md` 未含任何其它 URL 头（grep `^## https?://` 仅命中第 1 行），故本组无其它被分配页面；325 条内嵌 `source` 引用的其它页面归其它 group，见 §7。

## 3. 架构（部件如何连接）

- **值类型系统**：Boolean、Integer/Long/Double、String、List、Block/Item/Entity/Fluid、Operator、NBT、Ingredients/Recipe，以及 Number/Any/Named/Uniquely Named/Nullable 类别。书中的 List 签名未显示元素参数，但运行时 List 有元素类型，不能理解成无类型列表。Chemical 属 Mekanism 扩展。
- **卡片流程**（logic_programming 页事实）：Logic Programmer 把 Operator + 已填输入的 Variable Card 组合，在输出槽写入一张**动态 Variable Card**；被引用的 Variable Card 必须在**同一网络**的 Variable Store 中存在才能在求值时解析。无 Operator/Value Type 选择时插入卡片可**读回**其值。搜索框按名检索，左下槽按输入/输出类型过滤。有 Labeller 时按输出槽旁 "E" 可立即命名。
- **字符串编码通道**：Materializer 产出编码值；页内 `Decode Value` 元素把该编码串还原为 Variable Card；网络内 `Encode / Encode Compressed / Decode` 三个运算符做同等工作（可读/压缩两种形式，Decode 两者都接受）。
- **入口形态**：Logic Programmer 有方块形态与物品形态，可在合成栏互转；快捷键 `Alt+F` 聚焦搜索、`Alt+R` 重命名变量。合成：`Crafting Table + Block of Crystallized Menril + Crafting Table → Logic Programmer`；`Crafting Table + Logic Programmer → Portable Logic Programmer`；反向可还原。

## 4. 语义 / 边界 / 副作用要点（仅文档有据）

- **类型约束（有据）**：`anyEquals/anyNotEquals`「两侧类型必须相同」；`booleanChoice`「第 2、3 值类型必须相同」；`anyGreaterThanOrEquals/anyLessThanOrEquals` 接受 `Any,Any`（而 `>`,`<` 只接受 `Number,Number`）。
- **明确错误（有据）**：`listGet`「越界抛错」；`listGetOrDefault`「取不到则返回默认值」；`error`「抛自定义错误」（输入 String，输出 String）。
- **未描述的边界（禁止编造，标注 [INFERENCE] 仅作疑问提出）**：`numberDivide`/`numberModulus` 除零；`doubleSqrt` 负数；`listHead`/`listTail`/`reduce1` 空列表；`substring`/`slice` 负索引或越界；非法正则；`lazybuilt` 是否可为无限列表及其求值终止性；`Number` 抽象与 `Integer/Long/Double` 的隐式提升规则。
- **部分应用/高阶**：Apply 系列可绑定参数，Map/Filter/Reduce 接受 Operator；Pipe/Pipe2/Flip 与谓词组合构造新 Operator。部分应用已在 [Operator 类型页](https://integrateddynamics.rubensworks.net/book/manual/logic/value_types/operator.html) 和 [高阶教程](https://integrateddynamics.rubensworks.net/book/tutorials/theValueOfOperators.html) 明确说明，不属于未经文档支持的推断。
- **编码（有据）**：`encode`→可读串、`encode_compressed`→压缩串、`decode`←可读或压缩串；均与 Materializer 互通。
- **命名冲突（有据观察，非错误）**：显示名与符号**不唯一**，如 `mod` 出现于 Block/Item/Entity/Fluid/Chemical，`=Raw=` 出现于 Item/Fluid/Chemical，`Cast`/`Parse` 各多条。显示签名 ≠ 注册 ID（父任务须以源码核对注册名）。

## 5. 分类统计

| 类别 | 条数 | 归属 |
|---|---|---|
| Boolean 逻辑 | 5 | 核心 |
| Equality/Comparison | 6 | 核心 |
| Number 算术 | 9 | 核心 |
| Number 取整/格式化 | 4 | 核心 |
| Cast Number→(Double/Long/Integer) | 3 | 核心 |
| Integer 位运算 | 7 | 核心 |
| Double 数学 | 2 | 核心 |
| String | 21 | 核心 |
| Null 判定 | 2 | 核心 |
| List | 20 | 核心 |
| Block | 14 | 核心 |
| Item | 39 | 核心 |
| Entity | 39 | 核心 |
| Fluid | 21 | 核心 |
| Operator(高阶) | 16 | 核心 |
| NBT compound | 32 | 核心 |
| NBT scalar cast | 24 | 核心 |
| NBT path | 3 | 核心(疑点) |
| Ingredients | 9 | 核心 |
| Recipe | 5 | 核心 |
| Parse | 5 | 核心 |
| General(Choice/Identity/Constant/Encode/Decode) | 6 | 核心 |
| Cast(具体类型间) | 6 | 核心 |
| Chemical(Mekanism) | 27 | **扩展** |
| **合计** | **325** | 核心 298 + 扩展 27 |

## 6. 全量 325 条分类清单（每条唯一签名一次）

类型缩写：B=Boolean，N=Number，I=Integer，L=Long，D=Double，S=String，A=Any，Li=List，BLK=Block，IT=Item，EN=Entity，FL=Fluid，OP=Operator，Ch=Chemical(扩展)，NBT=NBT，ING=Ingredients，R=Recipe，NM=Named，UN=UniquelyNamed。标记：★=扩展来源页，◊=同时被教程页引用的核心项。

**A. Boolean 逻辑（5）**
| # | 名 | 符号 | 签名 | 备注 |
|---|---|---|---|---|
|1|And|`&&`|B,B→B|◊ saving_state|
|2|Or|`\|\|`|B,B→B|◊|
|3|Not|`!`|B→B|◊|
|4|Nand|`!&&`|B,B→B||
|5|Nor|`!\|!\|\|`（显示 `!||`）|B,B→B||

**B. Equality/Comparison（6）**
|6|Equals|`==`|A,A→B|◊；类型须相同|
|7|Not Equals|`!=`|A,A→B|类型须相同|
|8|Greater Than|`>`|N,N→B||
|9|Less Than|`<`|N,N→B|◊ export_items_limit|
|10|Greater Than or Equal|`>=`|A,A→B|注意入参为 Any|
|11|Less Than or Equal|`<=`|A,A→B|注意入参为 Any|

**C. Number 算术（9）**
|12|Addition|`+`|N,N→N|◊ logicOperations|
|13|Subtraction|`-`|N,N→N||
|14|Multiplication|`*`|N,N→N||
|15|Division|`/`|N,N→N|除零未描述|
|16|Modulus|`%`|N,N→N|除零未描述|
|17|Maximum|`max`|N,N→N|取大|
|18|Minimum|`min`|N,N→N|取小|
|19|Increment|`++`|N→N||
|20|Decrement|`--`|N→N||

**D. Number 取整/格式化（4）**
|21|Round|`‖‖`|N→I|四舍五入|
|22|Ceil|`⌈⌉`|N→I|向上|
|23|Floor|`⌊⌋`|N→I|向下|
|24|Compact|`compact`|N→S|更短、低精度表示|

**E. Cast Number→具体（3）**
|25|Cast Number to Double|`(double)`|N→D||
|26|Cast Number to Long|`(long)`|N→L||
|27|Cast Number to Integer|`(integer)`|N→I||

**F. Integer 位运算（7）**
|28|And|`&`|I,I→I|binaryAnd|
|29|Or|`\|`|I,I→I|binaryOr|
|30|Xor|`^`|I,I→I||
|31|Complement|`~`|I→I||
|32|Left Shift|`<<`|I,I→I||
|33|Right Shift|`>>`|I,I→I|带符号（ones fill）|
|34|Right Zero Shift|`>>>`|I,I→I|无符号（zeros fill）|

**G. Double 数学（2）**
|35|Square Root|`sqrt`|D→D||
|36|Power|`pow`|D,D→D||

**H. String（21）**
|37|Length|`len`|S→I|字符串长度|
|38|Concat|`+`|S,S→S||
|39|String Contains|`contains`|S,S→B||
|40|Contains Regex|`contains_regex`|S,S→B||
|41|Matches Regex|`matches_regex`|S,S→B||
|42|Index Of|`index_of`|S,S→I||
|43|Index Of Regex|`index_of_regex`|S,S→I||
|44|Starts With|`starts_with`|S,S→B||
|45|Ends With|`ends_with`|S,S→B||
|46|Split On|`split_on`|S,S→Li|字面分隔符|
|47|Split On Regex|`split_on_regex`|S,S→Li||
|48|Substring|`substring`|I,I,S→S|[start,end)|
|49|Regex Group|`regex_group`|S,I,S→S||
|50|Regex Groups|`regex_groups`|S,S→Li||
|51|Regex Scan|`regex_scan`|S,I,S→Li||
|52|Replace|`replace`|S,S,S→S||
|53|Replace Regex|`replace_regex`|S,S,S→S||
|54|Join|`join`|S,Li→S||
|55|To String|`name`|NM→S||
|56|Unique Name|`uname`|UN→S||
|57|Error|`error`|S→S|抛自定义错误|

**I. Null 判定（2）**
|58|Is Null|`o`|A→B|符号映射可疑（见 §8.8）|
|59|Is Not Null|`∅`|A→B|同上|

**J. List（20）**
|60|Length|`\| \|`|Li→I||
|61|Empty|`∅`|Li→B||
|62|Not Empty|`o`|Li→B||
|63|Get|`get`|Li,I→A|越界抛错|
|64|Get Or Default|`get_or_default`|Li,I,A→A|◊；安全取默认|
|65|Contains|`contains`|Li,A→B||
|66|Contains Predicate|`contains_p`|Li,OP→B|谓词任一为真|
|67|Count|`count`|Li,A→I||
|68|Count Predicate|`count_p`|Li,OP→I||
|69|Append|`append`|Li,A→Li||
|70|Concat|`concat`|Li,Li→Li||
|71|Lazy List Builder|`lazybuilt`|A,OP→Li|惰性构造；终止性未描述|
|72|Head|`head`|Li→A|空表未描述|
|73|Tail|`tail`|Li→Li|空表未描述|
|74|Unique Predicate|`uniq_p`|Li,OP→Li|按比较器去重|
|75|Uniq|`uniq`|Li→Li||
|76|Slice|`slice`|Li,I,I→Li|[start,end)|
|77|Intersection|`∩`|Li,Li→Li||
|78|List Equals (Set)|`=set=`|Li,Li→B|集合语义|
|79|List Equals (Multiset)|`=multiset=`|Li,Li→B|多重集语义|

**K. Block（14）**
|80|Opaque|`opaque`|BLK→B||
|81|Item|`itemstack`|BLK→IT||
|82|Mod|`mod`|BLK→S||
|83|Block Break Sound|`break_sound`|BLK→S||
|84|Block Place Sound|`place_sound`|BLK→S||
|85|Block Step Sound|`step_sound`|BLK→S||
|86|Block Is Shearable|`is_shearable`|BLK→B||
|87|Block Plant Age|`plant_age`|BLK→I||
|88|Block By Name|`block_by_name`|S→BLK||
|89|Block Properties|`block_props`|BLK→NBT||
|90|Block Properties|`block_with_props`|BLK,NBT→BLK||
|91|Block Properties|`block_all_props`|BLK→NBT|所有可能属性(列表值)|
|92|Block Tag Names|`block_tag_names`|BLK→Li||
|93|Block Tag Values|`block_tag_values`|S→Li||

**L. Item（39）**
|94|Size|`size`|IT→I||
|95|Max size|`maxsize`|IT→I|版本疑点（ItemStackMax）|
|96|Stackable|`stackable`|IT→B|≥2|
|97|Damageable|`damageable`|IT→B||
|98|Damage|`damage`|IT→I||
|99|Max Damage|`max_damage`|IT→I||
|100|Enchanted|`enchanted`|IT→B||
|101|Enchantable|`enchantable`|IT→B|◊ export_enchantable_items|
|102|Repair Cost|`repair_cost`|IT→I||
|103|Rarity|`rarity`|IT→S||
|104|Strength vs Block|`strength`|IT,BLK→D||
|105|Can Harvest|`can_harvest`|IT,BLK→B||
|106|Block|`block`|IT→BLK||
|107|Has Fluid|`is_fluidstack`|IT→B||
|108|Fluid|`fluidstack`|IT→FL||
|109|Fluid Capacity|`fluidstack_capacity`|IT→I|mB|
|110|NBT equals|`=NBT=`|IT,IT→B|data components 相等|
|111|Raw item equals no NBT|`=NoNBT=`|IT,IT→B|忽略 data/count，含 damage|
|112|Raw item equals|`=Raw=`|IT,IT→B|忽略 NBT/count/damage|
|113|Mod|`mod`|IT→S|◊ filter_storage_mod|
|114|Fuel Burn Time|`burn_time`|IT→I|ticks|
|115|Is Fuel|`can_burn`|IT→B||
|116|Item Tag Names|`item_tag_names`|IT→Li||
|117|Item Tag Values|`item_tag_values`|S→Li|◊ place_logwood|
|118|Item With Stacksize|`with_size`|IT,I→IT||
|119|Is FE Container|`is_fe_container`|IT→B||
|120|FE Stored|`stored_fe`|IT→I||
|121|FE Capacity|`capacity_fe`|IT→I||
|122|Item Has Inventory|`has_inventory`|IT→B||
|123|Item Inventory Size|`inventory_size`|IT→I||
|124|Item Inventory|`inventory`|IT→Li||
|125|Item By Name|`item_by_name`|S→IT||
|126|Item List Count|`item_list_count`|Li,IT→I|精确匹配的项统计|
|127|Item NBT|`NBT()`|IT→NBT||
|128|Has NBT|`has_nbt`|IT→B|称 "data components"（版本疑点）|
|129|Data Keys|`data_keys`|IT→Li|同上|
|130|Data Value|`data_value`|IT,S→NBT|同上|
|131|With Data|`with_data`|**FL**,S,NBT→NBT|⚠ 描述写"item stack"但签名/全局名 `fluidstackWithData` 为 Fluid（文档/抽取不一致）|
|132|Tooltip|`tooltip`|IT→Li||

**M. Entity（39）**
|133|Entity Tooltip|`entity_item_tooltip`|EN,IT→Li||
|134|Is Mob|`is_mob`|EN→B||
|135|Is Animal|`is_animal`|EN→B||
|136|Is Item|`is_item`|EN→B||
|137|Is Player|`is_player`|EN→B||
|138|Is Minecart|`is_minecart`|EN→B||
|139|Item|`item`|EN→IT||
|140|Health|`health`|EN→D||
|141|Width|`width`|EN→D||
|142|Height|`height`|EN→D||
|143|Is Burning|`is_burning`|EN→B||
|144|Is Wet|`is_wet`|EN→B||
|145|Is Crouching|`is_crouching`|EN→B||
|146|Is Eating|`is_eating`|EN→B||
|147|Armor Inventory|`armor_inventory`|EN→Li||
|148|Player Inventory|`inventory`|EN→Li||
|149|Mod|`mod`|EN→S||
|150|Target Block|`target_block`|EN→BLK|◊ advancedOperations|
|151|Target Entity|`target_entity`|EN→EN||
|152|Has Gui Open|`has_gui_open`|EN→B||
|153|Held Item Mainhand|`held_item_1`|EN→IT||
|154|Held Item Offhand|`held_item_2`|EN→IT||
|155|Mounted Entities|`mounted`|EN→Li||
|156|Item Frame Contents|`itemframe_contents`|EN→IT||
|157|Item Frame Rotation|`itemframe_rotation`|EN→I||
|158|Entity Hurt Sound|`hurtsound`|EN→S||
|159|Entity Death Sound|`deathsound`|EN→S||
|160|Entity Age|`age`|EN→I||
|161|Entity Is Child|`is_child`|EN→B||
|162|Entity Can Breed|`canbreed`|EN→B||
|163|Entity Is In Love|`is_in_love`|EN→B||
|164|Entity Can Breed With|`can_breed_with`|EN,IT→B||
|165|Entity Is Shearable|`is_shearable`|EN→B||
|166|Entity NBT|`NBT()`|EN→NBT|◊ entity_nbt_variable|
|167|Entity Type|`entity_type`|EN→S||
|168|Entity Items|`entity_items`|EN→Li||
|169|Entity Fluids|`entity_fluids`|EN→Li||
|170|Entity Energy Stored|`entity_stored_fe`|EN→I||
|171|Entity Energy Capacity|`entity_capacity_fe`|EN→I||

**N. Fluid（21）**
|172|Amount|`amount`|FL→I|mB|
|173|Block|`block`|FL→BLK||
|174|Light level|`light_level`|FL→I||
|175|Density|`density`|FL→I||
|176|Temperature|`temperature`|FL→I||
|177|Viscosity|`viscosity`|FL→I||
|178|Is Lighter Than Air|`lighter_than_air`|FL→B||
|179|Rarity|`rarity`|FL→S||
|180|Bucket empty sound|`sound_bucket_empty`|FL→S||
|181|Fluid vaporize sound|`sound_fluid_vaporize`|FL→S||
|182|Bucket fill sound|`sound_bucket_fill`|FL→S||
|183|Bucket|`bucket`|FL→IT||
|184|Raw fluid equals|`=Raw=`|FL,FL→B|忽略量|
|185|Mod|`mod`|FL→S||
|186|Fluid NBT|`NBT()`|FL→NBT||
|187|Fluid With Amount|`with_amount`|FL,I→FL||
|188|Data Keys|`data_keys`|FL→Li|版本疑点|
|189|Data Value|`data_value`|FL,S→NBT|版本疑点|
|190|Fluid Tag Names|`fluid_tag_names`|FL→Li||
|191|Fluid Tag Values|`fluid_tag_values`|S→Li||
|192|Fluid By Name|`fluid_by_name`|S→FL||

**O. Operator / 高阶（16）**
|193|Apply|`apply`|OP,A→A|◊ filter_storage_mod|
|194|Apply 2|`apply2`|OP,A,A→A||
|195|Apply 3|`apply3`|OP,A,A,A→A||
|196|Apply N|`apply_n`|OP,Li→A||
|197|Apply 0|`apply0`|OP→A||
|198|Map|`map`|OP,Li→Li||
|199|Filter|`filter`|OP,Li→Li|◊ integratedscripting/functions|
|200|Conjunction|`.&&.`|OP,OP→OP|谓词合取|
|201|Disjunction|`.||.`|OP,OP→OP|谓词析取|
|202|Negation|`!.`|OP→OP|谓词取反|
|203|Pipe|`.`|OP,OP→OP|◊ filter_storage_mod|
|204|Pipe 2|`.2`|OP,OP,OP→OP||
|205|Flip|`flip`|OP→OP|交换前两入参|
|206|Reduce|`reduce`|OP,Li,A→A||
|207|Reduce 1|`reduce1`|OP,Li→A|= reduce(op,tail(l),head(l))|
|208|Operator By Name|`op_by_name`|S→OP||

**P. NBT compound（32）**
|209|NBT Compound Size|`NBT{}.size`|NBT→I||
|210|NBT Compound Keys|`NBT{}.keys`|NBT→Li||
|211|NBT Compound Has Key|`NBT{}.has_key`|NBT,S→B||
|212|NBT Compound Entry Type|`NBT{}.type`|NBT,S→S||
|213|NBT Compound Value|`NBT{}.get_tag`|NBT,S→NBT||
|214|NBT Compound Value Boolean|`NBT{}.get_boolean`|NBT,S→B||
|215|NBT Compound Value Integer|`NBT{}.get_integer`|NBT,S→I||
|216|NBT Compound Value Long|`NBT{}.get_long`|NBT,S→L||
|217|NBT Compound Value Double|`NBT{}.get_double`|NBT,S→D||
|218|NBT Compound Value String|`NBT{}.get_string`|NBT,S→S||
|219|NBT Compound Value Compound|`NBT{}.get_compound`|NBT,S→NBT|◊ nbt|
|220|NBT Compound Value List NBT|`NBT{}.get_list_tag`|NBT,S→Li||
|221|NBT Compound Value Byte Array|`NBT{}.get_list_byte`|NBT,S→Li|为 Integer List|
|222|NBT Compound Value Integer Array|`NBT{}.get_list_int`|NBT,S→Li||
|223|NBT Compound Value Long Array|`NBT{}.get_list_long`|NBT,S→Li|为 Long List|
|224|NBT Compound Without|`NBT{}.without`|NBT,S→NBT||
|225|NBT Compound With Boolean|`NBT{}.with_boolean`|NBT,S,B→NBT||
|226|NBT Compound With Short|`NBT{}.with_short`|NBT,S,I→NBT|以 short 存|
|227|NBT Compound With Integer|`NBT{}.with_integer`|NBT,S,I→NBT||
|228|NBT Compound With Long|`NBT{}.with_long`|NBT,S,L→NBT||
|229|NBT Compound With Double|`NBT{}.with_double`|NBT,S,D→NBT||
|230|NBT Compound With Float|`NBT{}.with_float`|NBT,S,D→NBT|以 float 存|
|231|NBT Compound With String|`NBT{}.with_string`|NBT,S,S→NBT||
|232|NBT Compound With NBT|`NBT{}.with_tag`|NBT,S,NBT→NBT||
|233|NBT Compound With NBT List|`NBT{}.with_tag_list`|NBT,S,Li→NBT||
|234|NBT Compound With Byte List|`NBT{}.with_byte_list`|NBT,S,Li→NBT|Byte Array|
|235|NBT Compound With Int List|`NBT{}.with_int_list`|NBT,S,Li→NBT|Int Array|
|236|NBT Compound With Long List|`NBT{}.with_list_long`|NBT,S,Li→NBT|Long Array|
|237|NBT Compound Subset|`NBT{}.⊆`|NBT,NBT→B|递归子集|
|238|NBT Compound Union|`NBT{}.∪`|NBT,NBT→NBT|递归并|
|239|NBT Compound Intersection|`NBT{}.∩`|NBT,NBT→NBT|递归交|
|240|NBT Compound Minus|`NBT{}.∖`|NBT,NBT→NBT|递归差|

**Q. NBT 标量/数组 cast（24）**
|241|NBT Boolean As Boolean|`NBT.as_boolean`|NBT→B|Byte tag|
|242|NBT Byte As Integer|`NBT.as_byte`|NBT→I||
|243|NBT Short as Integer|`NBT.as_short`|NBT→I||
|244|NBT Integer As Integer|`NBT.as_int`|NBT→I||
|245|NBT Long As Long|`NBT.as_long`|NBT→L||
|246|NBT Double As Double|`NBT.as_double`|NBT→D||
|247|NBT Float As Double|`NBT.as_float`|NBT→D||
|248|NBT String As String|`NBT.as_string`|NBT→S||
|249|NBT List As NBT List|`NBT.as_tag_list`|NBT→Li||
|250|NBT Byte Array As Byte List|`NBT.as_byte_list`|NBT→Li||
|251|NBT Integer Array As Integer List|`NBT.as_int_list`|NBT→Li||
|252|NBT Long Array As Long List|`NBT.as_long_list`|NBT→Li||
|253|NBT Byte From Boolean|`NBT.from_boolean`|B→NBT|Byte tag|
|254|NBT Short From Integer|`NBT.from_short`|I→NBT|Short|
|255|NBT Byte From Integer|`NBT.from_byte`|I→NBT|Byte|
|256|NBT Integer From Integer|`NBT.from_int`|I→NBT|Int|
|257|NBT Long From Long|`NBT.from_long`|L→NBT|Long|
|258|NBT Double From Double|`NBT.from_double`|D→NBT|Double|
|259|NBT Float From Double|`NBT.from_float`|D→NBT|Float|
|260|NBT String From String|`NBT.from_string`|S→NBT|String|
|261|NBT List From Tag List|`NBT.from_tag_list`|Li→NBT|List|
|262|NBT Byte Array From Byte List|`NBT.from_byte_list`|Li→NBT|Byte Array|
|263|NBT Integer Array From Integer List|`NBT.from_int_list`|Li→NBT|Int Array|
|264|NBT Long Array From Long List|`NBT.from_long_list`|Li→NBT|Long Array|

**R. NBT Path（3，版本疑点）**
|265|NBT Path Match First|`NBT.path_match_first`|S,NBT→NBT|◊ nbt_path|来源 `manual/logic/advanced/nbt_path.html`|
|266|NBT Path Match All|`NBT.path_match_all`|S,NBT→Li|◊ 同上|
|267|NBT Path Test|`NBT.path_test`|S,NBT→B|◊ 同上|

**S. Ingredients（9）**
|268|Ingredient items|`Ingr.items`|ING→Li||
|269|Ingredient fluids|`Ingr.fluids`|ING→Li||
|270|Ingredient energy elements|`Ingr.energies`|ING→Li||
|271|Ingredients With Item|`Ingr.with_item`|ING,I,IT→ING||
|272|Ingredients With Fluid|`Ingr.with_fluid`|ING,I,FL→ING||
|273|Ingredients With Energy|`Ingr.with_energy`|ING,I,L→ING||
|274|Ingredients With Items|`Ingr.with_items`|ING,Li→ING||
|275|Ingredients With Fluids|`Ingr.with_fluids`|ING,Li→ING||
|276|Ingredients With Energy Elements|`Ingr.with_energies`|ING,Li→ING||

**T. Recipe（5）**
|277|Recipe Input Ingredients|`recipe_in`|R→ING||
|278|Recipe Output Ingredients|`recipe_out`|R→ING||
|279|Recipe With Input Ingredients|`Recipe.with_in`|R,ING→R||
|280|Recipe With Output Ingredients|`Recipe.with_out`|R,ING→R||
|281|Recipe With Input and Output Ingredients|`Recipe.with_io`|ING,ING→R||

**U. Parse（5）**
|282|Parse|`parse_boolean`|S→B||
|283|Parse|`parse_double`|S→D||
|284|Parse|`parse_integer`|S→I||
|285|Parse|`parse_long`|S→L||
|286|Parse|`parse_nbt`|S→NBT||

**V. General（6）**
|287|Choice|`?`|B,A,A→A|◊ saving_state；第2/3类型须相同|
|288|Identity|`id`|A→A||
|289|Constant|`K`|A,A→A|返回第 1 值|
|290|Encode|`encode`|A→S|版本疑点|
|291|Encode Compressed|`encode_compressed`|A→S|版本疑点|
|292|Decode|`decode`|S→A|版本疑点|

**W. Cast 具体类型间（6）**
|293|Cast|`()`|I→D||
|294|Cast|`()`|I→L||
|295|Cast|`()`|D→I||
|296|Cast|`()`|D→L||
|297|Cast|`()`|L→I||
|298|Cast|`()`|L→D||

**X. Chemical（Mekanism 扩展，27）★**
|299|Has Chemical|`is_chemicalstack`|IT→B|itemstackIsChemicalStack|
|300|Chemical|`chemicalstack`|IT→Ch||
|301|Chemical Capacity|`chemicalstack_capacity`|IT→L|mB|
|302|Amount|`amount`|Ch→L|mB|
|303|Tint|`tint`|Ch→I|颜色|
|304|Is Radioactive|`is_radioactive`|Ch→B|◊ reading|
|305|Radioactivity|`radioactivity`|Ch→D||
|306|Is Coolant|`is_coolant`|Ch→B|冷/热 coolant|
|307|Thermal Enthalpy|`thermal_enthalpy`|Ch→D||
|308|Conductivity|`conductivity`|Ch→D||
|309|Is Cooled Coolant|`is_cooled_coolant`|Ch→B||
|310|Cooled Coolant Of|`cooled_coolant_of`|Ch→Ch|返回对应 heated|
|311|Is Heated Coolant|`is_heated_coolant`|Ch→B||
|312|Heated Coolant Of|`heated_coolant_of`|Ch→Ch|返回对应 cooled|
|313|Is Fuel|`is_fuel`|Ch→B||
|314|Burn Ticks|`burn_ticks`|Ch→I||
|315|Energy per Tick|`energy_per_tick`|Ch→L||
|316|Raw chemical equals|`=Raw=`|Ch,Ch→B||
|317|Mod|`mod`|Ch→S||
|318|With Amount|`with_amount`|Ch,L→Ch||
|319|Tag Names|`chemical_tag_names`|Ch→Li||
|320|Tag Values|`chemical_tag_values`|S→Li||
|321|Chemical By Name|`chemical_by_name`|S→Ch||
|322|Chemicals|`Ingr.chemicals`|ING→Li||
|323|With Chemical|`Ingr.with_chemical`|ING,I,Ch→ING||
|324|With Chemicals|`Ingr.with_chemicals`|ING,Li→ING||
|325|Radiation|`radiation`|EN→D|Sv/h|

## 7. Provenance（325 条 `source` 字段观察到的源页；均非本组分配页，归各 group）

| source URL | 出现运算符（示例） |
|---|---|
| `manual/logic_programming.html` | 绝大多数（核心 298 的默认来源） |
| `tutorials/saving_state.html` | And/Or/Not、Get Or Default、Choice |
| `tutorials/logicOperations.html` | Addition |
| `tutorials/advancedOperations.html` | Target Block |
| `tutorials/nbt.html` | NBT Compound Value Compound |
| `tutorials/tutorials/integratedtunnels/filter_storage_mod.html` | Equals、Mod(item)、Pipe、Apply |
| `tutorials/tutorials/integratedtunnels/export_items_limit.html` | Less Than |
| `tutorials/tutorials/integratedtunnels/export_enchantable_items.html` | Enchantable |
| `tutorials/tutorials/integratedtunnels/place_logwood.html` | Item Tag Values |
| `tutorials/tutorials/integratedscripting/functions.html` | Filter |
| `tutorials/tutorials/integratednbt/entity_nbt_variable.html` | Entity NBT |
| `manual/logic/advanced/nbt_path.html` | NBT Path ×3 |
| `manual/manual/integratedmekanism/logic/operators.html` | Chemical ×27 |
| `tutorials/tutorials/integratedmekanism/reading.html` | Is Radioactive |

## 8. 版本疑点与不确定项（须父任务按 1.20.1 源码裁决）

1. **[高] "data component" 语义**：`has_nbt=hasNbt`、`data_keys`、`data_value`、`with_data`、`=NBT=` 的描述使用 "data components"——这是 MC1.20.5+/1.21 概念；**1.20.1 用 NBT tag**。名称/行为在 1.20.1 可能不同（如 hasNbt 仍是 tag 判定）。在线书 = 1.21.1/ID1.38.0，不保证对 1.20.1 成立。
2. **[高] Encode/Encode Compressed/Decode + Materializer/"Decode Value"**：仅见 1.21.1 文档；1.20.1 是否存在同名/同行为需核对。
3. **[高] ItemStackMax / `maxsize`**：返回 `Integer`（显示）；实际打包/上限语义（64、含 NBT 覆盖）未描述，且注册名可能是 `itemstackMaxSize`（须核 1.20.1）。
4. **[高] Lazy List Builder（`lazybuilt`）与 List 动态输出**：文档称"惰性构造"，可产生**长/可能无限**列表；Map/Filter/Reduce 在其上的求值终止性、短路语义**文档未描述** → [INFERENCE] DSL 不能假定有限列表。
5. **[中] NBT Path ×3**：来源 `manual/logic/advanced/nbt_path.html`，属较新功能；1.20.1 可用性未知。
6. **[中] Number 抽象类型**：`Number` 作为算术入参、实际由 Integer/Long/Double 承载；隐式提升规则、溢出语义未描述。
7. **[中] 运算符名/符号非唯一**：`Mod`×4、`=Raw=`×3、`Cast`/`Parse`/`NBT()`/`Concat`/`Length`/`Amount` 等重名；显示签名 ≠ 注册 ID（如 `blockMod` vs `itemstackMod` vs `fluidstackMod` vs `chemicalstackMod`）。DSL 必须以注册 ID 而非显示名索引。
8. **[低/抽取缺陷] 符号映射可疑**：`Is Null` 显示 `o`、`Is Not Null` 显示 `∅`（与直觉相反，疑为图标字形抽取错位）；`Round`=`‖‖`、List `Length`=`| |` 同理。以游戏内实际为准。
9. **[低/文档不一致] `with_data`**：描述"add … to the given **item stack**"，但 inputs 为 `Fluid,String,NBT`、global 名 `fluidstackWithData`。抽取/文档冲突，须以源码为准。
10. **[低] Cast 双套**：既有 `Cast Number to {Double,Long,Integer}`（from Number），又有具体类型间 `Cast ()`；是否重复/等价未说明。
11. **[确认] Chemical 家族属扩展**：来源 `integratedmekanism`，且 Chemical 量纲为 `Long`（与 Fluid 的 `Integer` 不同）——**不得计入 ID 核心**。

## 9. 交付物对应

- **report**：本消息（含摘要、架构、语义/边界、分类统计、全量清单、版本疑点）。
- **coverage**：§2 表——分配 URL 1/1 已读（`manual/logic_programming.html`，全文 5109 行）；325/325 签名已覆盖（§6 每签名恰一次）。
- **classification**：§6 表 + §5 统计（core 298 / extension 27），不确定项见 §8。

（无 TODO；未写文件；未执行 build/lint/tests。）
