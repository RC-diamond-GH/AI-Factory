# AI-Factory

[English](README.md) | 简体中文

AI-Factory 将基于 LLM 的助手带入 Minecraft：在游戏中对话、请求经过权限检查的游戏操作，也可以通过独立的 ME 智能终端或集成动力 Agent 终端检查对应网络。

**目前处于早期 Beta 阶段。** 维护者已实测基础对话面板及 AE2 联动功能；多人游戏、独立服务端运行和各种边界情况**尚未实测**。尝试前请备份世界；Beta 更新不承诺存档兼容性。

## 入门指南

### 1. 安装 Mod 与依赖

| 组件 | 版本／要求 |
| --- | --- |
| Minecraft | **1.20.1** |
| Mod 加载器 | **Forge**；开发目标为 **47.4.10** |
| Java | **17** |
| Kotlin for Forge（KFF） | **必需**；使用适配 Minecraft 1.20.1 的 **4.12.0** |
| Applied Energistics 2 | **可选**；联动面向 Forge 版 **15.4.11** |
| GuideME | 使用 AE2 联动时安装 **20.1.7** |
| Just Enough Items（JEI） | **可选，仅客户端**；Forge 1.20.1 的 **15.x 且至少 15.62.0**；开发目标 **15.62.0.219** |
| Integrated Dynamics | **可选**；Forge 1.20.1，开发目标 **1.31.3**；同时安装其依赖 CyclopsCore 与 CommonCapabilities |

1. 在启动器中创建 Minecraft 1.20.1 Forge 实例。
2. 从 [GitHub Releases](https://github.com/RC-diamond-GH/AI-Factory/releases) 下载 AI-Factory 发布 JAR，放入该实例的 `mods` 目录。请选择 `.jar` 附件，不要使用 GitHub 自动生成的源码压缩包或带 `-plain.jar` 后缀的构建包。
3. 安装适配 Minecraft 1.20.1 的 [Kotlin for Forge](https://github.com/thedarkcolour/KotlinForForge)。AI-Factory **没有内嵌 KFF**；即使不安装 AE2，也需要 KFF。
4. 如需 ME 功能，再安装 [AE2](https://github.com/AppliedEnergistics/Applied-Energistics-2) 15.4.11 和 [GuideME](https://github.com/AppliedEnergistics/GuideME) 20.1.7，选择它们适配 Minecraft 1.20.1 的 Forge 构建。
5. 如需配方查询，在客户端安装适配 Minecraft 1.20.1 的 Forge 版 [JEI](https://github.com/mezz/JustEnoughItems)。JEI 不内嵌；这些 Tool 不要求服务端安装 JEI。
6. 如需集成动力功能，在两侧安装 Forge 1.20.1 的 [Integrated Dynamics](https://github.com/CyclopsMC/IntegratedDynamics)、CyclopsCore 和 CommonCapabilities。开发组合为 ID **1.31.3**、CyclopsCore **1.20.0**、CommonCapabilities **2.9.7**；不需要 AE2 或 Integrated Tunnels。

不安装 AE2 时，普通聊天助手仍可使用，但不会注册 ME 物品和终端功能。本项目是 Forge Mod，不是 Fabric 或 NeoForge 构建。

当前网络设计要求**客户端和服务端都安装同一构建的** AI-Factory 与 KFF；使用 AE2 联动还需两侧安装 AE2 与 GuideME，使用 ID 联动还需两侧安装 ID 及其依赖。这是安装要求，**不代表多人游戏或独立服务端已经验证可用**。

### 2. 设置 LLM API 访问方式

你需要自行提供 API 地址、API 密钥和模型。AI-Factory 使用 **OpenAI 兼容的 Chat Completions API**；如需使用游戏工具，模型和服务商必须支持流式响应及工具／函数调用。不保证兼容所有服务商，Mod 也不包含 API 订阅或额度。

1. 启动游戏、进入世界，按 **G** 打开助手。可以在 Minecraft 的控制设置中修改按键。
2. 首次打开时，Mod 会在**客户端游戏实例**中创建以下文件：

   ```text
   config/ai-factory/model.toml
   config/ai-factory/agent.toml
   ```

   默认 API 密钥为空，因此填写前出现配置错误属于预期行为。
3. 编辑 `model.toml`，例如：

   ```toml
   base_url = "https://api.openai.com/v1"
   api_key = "YOUR_API_KEY"
   model = "gpt-4o-mini"
   ```

   将密钥和模型替换为你的服务商提供的实际值。使用其他兼容服务时，还需替换 `base_url`。这里填写的是 API 根地址，不是完整请求地址：Mod 会追加 `/chat/completions`，但不会自动添加 `/v1`。
4. 回到对话面板，点击**重载配置**，然后发送消息。

`agent.toml` 控制系统提示、Agent 最大步骤数、模型请求超时和工具超时。初次使用可以保留自动生成的值。已有文件不会被自动覆盖。普通 G 键助手重载配置时会重置会话，ME 会话重载时保留共享历史。

LLM 请求由客户端使用该玩家的配置发起，不需要配置服务端 LLM Provider。API 调用可能产生费用，包括自动生成 ME 会话标题时的一次额外请求。对话历史及相关工具结果会发送给配置的服务商。**API 密钥以明文保存**：不要提交或分享 `model.toml`。共享 ME 历史保存在世界中，并在本地缓存；不要将它当成私人笔记，公开上传前也应检查其中的内容。

### 3. 尝试 AE2 联动（可选）

将 **ME 智能元件**装入 ME 驱动器，再把 **ME 智能终端**安装到 AE2 线缆的空闲面上。终端需要网络供电和一个通道。右键终端即可新建或打开会话，使用的 LLM 配置与普通助手相同。

智能元件提供 **1K / 4K / 16K / 64K / 256K** 五档容量。每张最多容纳 **8 个会话**，共享该元件的容量。元件保存的是对话上下文，不是普通物品堆叠。存储量使用本地 `cl100k_base` 计数，不等于服务商的计费 Token，也不保证模型能够接收全部已存历史。

### 4. 尝试集成动力联动（可选）

合成或从“集成动力 Agent”创造模式栏取得 **集成动力 Agent 终端**，放置后连接 ID 逻辑线缆。网络初始化后右键终端，创建或打开会话。它是完整方块，不安装在 AE2 线缆上，也不消耗 ME 通道或元件容量。

终端保存自身 UUID 和所引用的会话 UUIDs；上下文保存在独立的世界数据中。每个终端最多 **32 个会话**，每个规范化上下文最多 **4 MiB**。正常破坏或潜行扳手拆除的掉落物携带 UUID 引用，重新放置可延续会话；这是实现的存储路径，**尚未游戏内验收**。ID Agent 与 ME Agent 的历史、会话锁和工具集独立，LLM 配置文件共用；会话及工具观察同样会发送给配置的服务商。


## 目前的能力范围

### 普通助手——G 键

- 游戏内对话、流式回答，以及有步骤上限的 Agent 循环：可以请求工具并根据实际结果继续回答。
- `give_item`：向请求玩家本人给予已注册物品，需服务端授予等同 `/give` 的权限（权限等级至少为 2）。
- `execute_command`：以请求玩家的权限和上下文执行服务端命令，不会授予管理员权限。

命令可能影响世界或其他玩家。关闭普通聊天会取消当前回合，但**取消不会撤销已经执行的操作**。这是具备特定工具的助手，不是对 Minecraft 全部功能的无限制控制。

### ME 智能终端——可选 AE2 联动

- 与智能元件关联的持久化会话，提供会话列表、自动标题、清空历史和删除功能。
- 世界存档中的上下文存储及本地缓存。设计上使用独占会话锁，但多用户争用尚未实测。
- 后台回合及状态 HUD：关闭 ME 界面**不会**取消正在运行的回合。保存确认、回合结束且界面关闭后，才释放会话锁。

| ME 工具 | 能力 |
| --- | --- |
| `ae2_read_inventory` | 读取网络库存及可合成条目。 |
| `ae2_item_details` | 查看条目的身份、数量及有范围限制的 NBT 详情。 |
| `ae2_crafting_plan` | 预览合成计划和缺料，不提交合成任务。 |
| `ae2_request_crafting` | 根据已有样板提交真实 AE2 合成任务，产物留在网络中。 |
| `ae2_manage_patterns` | 查看样板容器；编码并安装受支持的工作台、加工、切石及锻造样板。 |

### 集成动力 Agent 终端——可选 ID 联动

| ID 工具 | 能力 |
| --- | --- |
| `id_network_info` | 原生网络概况：线缆、元素、部件、可解析变量定义、能耗及材料组件。 |
| `id_list_parts` | 分页检查已加载部件的 ID、类型、通道、位置及实际目标面。 |
| `id_list_variables` | 分页检查 Variable Store 与活动部件卡槽中的可解析定义，返回类型与 operator／aspect／proxy／delay 引用。 |
| `id_compile` | 将类 Haskell DSL 预览为原生卡片图，返回输出类型、空白卡数量和带位置诊断；不制卡、不分配 ID。 |
| `id_make_cards` | 重新编译，经服务端独立校验后消耗真正空白卡，在主背包中创建原生变量卡。 |

列表参数为 `filter`、从 0 开始的 `page`、`page_size`（1–100，默认 20）。分页读取的是实时网络，不是冻结快照；不强制加载其他区块。变量 `valid` 只是原生 facade 的基本有效性标志，不证明依赖／类型验证或运行求值成功。输入类型证明只读定义元数据，不读取实时值。

DSL 使用 `input name : Type;`、`let name = expression;`、`output port = expression;`，并显式绑定变量／Aspect。支持类型化列表、部分应用、组合、条件和固定原生算子表，不是完整 Haskell，不支持递归或任意脚本。先预览，再明确要求制卡。`id_make_cards` 只使用主背包 36 槽，不覆盖既有卡，不部署卡或配置物流；所有依赖卡需手动放入同一网络的变量仓库，再安装输出卡。失败报告阶段、节点／输入、可定位时的源码位置和部分结果；已经完成的写入不回滚。

详见 [DSL 使用指南](documents/IntegratedDynamicsDSL使用指南.md)、[语言／wire／后端设计](documents/IntegratedDynamicsDSL设计.md)及[共享解释器 API](documents/共享类Haskell解释器.md)。制卡 RPC 与卡片 NBT 不含源码，但源码仍可能随会话保存到世界存档、发送给配置的 LLM 服务商。两端必须使用共享会话协议 **7**。

ID 会话可后台运行和显示 HUD；服务端检查会话锁、终端身份、引用、维度、加载状态及交互权限。首次打开需在 8 格内，持锁后台操作仅放宽距离。离线编译、工具／错误路径烟测与打包不能替代真实制卡、网络求值、网络合并／拆分、无线连接、界面或多人验收；这些游戏内检查仍需手动完成。


### 配方查询——可选 JEI 联动

客户端安装 JEI 后，普通助手和 ME 助手都会获得以下只读工具。未安装 JEI 时，两项工具均不注册，其余非 JEI 功能保持可用。

| JEI 工具 | 能力 |
| --- | --- |
| `jei_find_ingredients` | 按本地化名称或注册 ID 搜索可见的物品、流体和自定义材料类型，分页返回材料引用。 |
| `jei_query_recipes` | 查询材料的制作配方、作为原料或催化剂的用途，可限定 JEI 分类；返回展示槽位及分类的工作台／机器。 |

可以尝试问“用 JEI 查一下活塞怎么制作”或“铁锭参与哪些 JEI 配方？”。模型必须支持并实际调用工具；普通文字回答本身不证明发生了查询。

材料 `key` 区分 JEI 定义的子类型，不是每个 NBT 字段的完整指纹。JEI 重载或有界引用缓存淘汰条目后，旧引用会返回 `unknown_key`，需要重新搜索。直接使用物品／流体 ID 选择默认堆叠。runtime 暂时不可用时返回 `jei_not_ready`，不删除 Agent 已注册的工具。

结果属于 JEI **展示数据**，不是可执行配方，也不证明玩家或 ME 网络能合成。轮换候选可能相互关联；仅用于索引、不展示的材料不在槽位视图中。工具不提取机器耗能、耗时、概率或执行条件；布局缺失／失败、隐藏候选和截断都会明确标记。


## 实测状态与限制

**维护者已完成实测：**

- 基础游戏内对话面板。
- AE2 联动功能。

这些是功能层面的检查，不是对每个操作及运行环境的穷尽验证。以上为维护者报告的实测结果，不代表本次文档更新启动了游戏。

**尚未实测：**

- 游戏内的 JEI 查询，包括插件发现、第三方配方分类和 runtime 重载。
- 多人游戏，包括共享会话的并发访问。
- 独立服务端启动与运行。
- 各类边界情况：大规模历史、容量限制、断线、超时、取消竞态、损坏存档，以及与其他 Mod 的交互。

## 反馈与 Issue

欢迎在 [GitHub Issues](https://github.com/RC-diamond-GH/AI-Factory/issues) 提交 Bug、建议和使用体验。请尽量提供：

- AI-Factory、Minecraft、Forge、KFF，以及已安装的 AE2／GuideME 版本。
- 问题发生在单人游戏、多人游戏还是独立服务端。
- 复现步骤、预期行为和实际结果。
- 相关的**脱敏**日志或截图；API 问题还可提供便于公开的服务商及模型名称。

不要附上 API 密钥、认证请求头、未经检查的配置文件或敏感对话历史。提交前请检查日志和截图，移除秘密信息。

## 开发与许可证

构建环境、打包注意事项、Provider 契约及历史验证记录已整理到[开发与踩坑记录](documents/开发与踩坑记录.md)。实现细节另见 [AE2 智能存储拓展交付说明](documents/AE2智能存储拓展交付说明.md)和[客户端 ReAct 与 Tool 架构设计](documents/客户端ReAct与Tool架构设计.md)。

AI-Factory 使用 [Apache-2.0](LICENSE) 许可证。发布 JAR 内嵌的 JTokkit 使用 [MIT 许可证](src/main/resources/META-INF/licenses/jtokkit-LICENSE.txt)；Kotlin for Forge、AE2、GuideME、JEI、Integrated Dynamics、CyclopsCore 和 CommonCapabilities 需另行安装。
