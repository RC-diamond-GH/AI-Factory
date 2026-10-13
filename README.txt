AI-Factory
==============================
仓库：https://github.com/RC-diamond-GH/AI-Factory
Minecraft 1.20.1 / Forge 47.4.10 / Gradle Wrapper 8.8。
Kotlin 2.2.21 / Kotlin for Forge 4.12.0 / Java 17 编译目标。
Mod ID：ai_factory；入口：com.rcdiamond.aifactory.AIFactory；版本：1.0.0。
入口注册公共游戏操作网络；客户端提供统一 LLM 模式、流式 ReAct、Tool 注册器和游戏内聊天界面。
内置 give_item 和 execute_command Tool，经网络由服务端执行；没有新增方块、物品或创造模式物品栏。
新增游戏内容时再添加对应注册器、配置与 assets/ai_factory、data/ai_factory 资源。
Kotlin 源码位于 src\main\kotlin\com\rcdiamond\aifactory；元数据在 gradle.properties 中配置。
开发注意事项见 AGENTS.md；测试方案见 documents\单元测试实施方案.md。

在项目目录的 PowerShell 中执行：
    . .\Set-DevEnv.ps1
    .\gradlew.bat build
    .\gradlew.bat runClient

已安装 GNU Make 时，可在项目根目录使用 Makefile 打包：
    make             # 默认 build，包含本地测试与 reobfJar
    make package     # build 的别名
    make rebuild     # gradlew.bat clean build，清理后重新打包
各目标自动在同一个 PowerShell 进程中加载 Set-DevEnv.ps1，无需提前手动加载。

Set-DevEnv.ps1 将代理环境变量注入当前 PowerShell 会话：
    HTTP_PROXY=http://127.0.0.1:7897
    HTTPS_PROXY=http://127.0.0.1:7897
    NO_PROXY=localhost,127.0.0.1,::1,*.local,10.*,172.16.*,192.168.*
代理程序必须监听 127.0.0.1:7897。Gradle 的 JVM 代理另配置在 gradle.properties 中。
未修改 Windows 用户级或系统级环境变量。
若脚本被执行策略阻止，可仅对当前会话执行：
    Set-ExecutionPolicy -Scope Process Bypass

Gradle 使用 C:\Program Files\Java\jdk-21。
Minecraft 1.20.1 的编译及开发客户端使用 Java 17 toolchain：
    .tools\jdk-17.0.20.1+1（Eclipse Temurin，Windows x64）
本地 JDK 不纳入 Git。迁移或克隆项目后需安装 JDK 17，并修改 gradle.properties
中的 org.gradle.java.home 和 org.gradle.java.installations.paths，
以及 Set-DevEnv.ps1 中的 JAVA_HOME，以匹配本机路径。

IntelliJ IDEA：打开本目录，Gradle JVM 选择 JDK 21，项目 SDK 选择 JDK 17；
生成 IDE 启动配置：.\gradlew.bat genIntellijRuns genEclipseRuns。
Eclipse 的启动配置可生成；Kotlin 编辑优先使用 IntelliJ IDEA。
构建产物：build\libs\ai_factory-1.0.0.jar。
运行发布 JAR 时，客户端与服务端均需另外安装适配 Minecraft 1.20.1 的 Kotlin for Forge。
项目采用远端仓库既有的 Apache-2.0 许可证（LICENSE），并随发布 JAR 打包。
LICENSE.txt、CREDITS.txt 与 changelog.txt 是 Forge MDK 随附的许可、归属与历史文件。

框架迁移验证：
    .\gradlew.bat clean build genIntellijRuns genEclipseRuns --console=plain
构建成功，reobfJar 已执行；发布 JAR 的入口字节码目标为 Java 17，
Forge/KFF 元数据已展开，许可证和资源包描述正确，不含旧示例内容或 Kotlin 运行库副本。
已接入 JUnit 5：常规 test/build 执行本地协议测试；真实 API 测试通过 openaiIntegrationTest 单独运行。
按项目约束未启动客户端或服务端，实际 Forge/KFF 加载与非开发环境发布验证尚未执行。
启动独立服务端前须自行阅读并接受 Minecraft EULA；脚本不会代为接受。

LLM API
==============================
IR 与抽象：com.rcdiamond.aifactory.client.llm。
实现：com.rcdiamond.aifactory.client.model_provider.openai_completion.OpenAICompletion。
统一接口：ModelProvider（非 SAM 接口）：
    suspend fun generate(prompt: LLMContext): GenerateResponse
    suspend fun generate_stream(prompt: LLMContext): Result<Flow<Result<AssistantEvent>>>

LLMContext 包含 systemPrompt、按序排列的 globalPrompt、可空的 tools 和 messages。
ChatMessage 为 User / Assistant / Tool；Assistant 支持可空文本和多个 ToolCall。
ToolDefinition.parameters 与 ToolCall.arguments 使用 kotlinx.serialization.json.JsonElement，
Usage 使用 ULong 对应 u64；FinishReason.Unknown 保留未知的终止原因原值。

OpenAICompletion(baseUrl, apiKey, model) 使用 Java 17 HttpClient，调用 /chat/completions。
baseUrl 是 API 根路径，例如 https://api.openai.com/v1；保留调用方提供的路径，
只去除末尾斜杠并追加 /chat/completions，不自动添加 /v1。
请求依次发送主 system 消息、各 globalPrompt 的 system 消息（filename + 换行 + content）、
以及历史 messages。null 或空 tools 不发送 tools 字段。
工具参数在 IR 中为 JSON 值，在 OpenAI wire format 中为 JSON 字符串；provider 不执行工具。
收到 Assistant.toolCalls 后，由调用方执行工具，把 Assistant 和对应 Tool 结果追加到历史后再次 generate。
generate 使用非流式响应，取 choices 中的第一项；解析 assistant 内容、工具调用、finish_reason 和可选 usage。

generate_stream 返回冷 Flow：每次 collect 发起独立请求，不收集则不建立连接。
请求使用 stream=true、stream_options.include_usage=true 和 Accept: text/event-stream，
增量解析 SSE data、UTF-8 跨网络分片、LF/CRLF/CR 换行、注释和多行 data，只消费 choice index=0。
AssistantEvent 包括 TextDelta(text)、ToolCallDelta(index, id?, name?, arguments?)、
Finished(reason) 与 TokenUsage(usage)。按 index 累积工具调用的 id/name/arguments 片段；
arguments 是原始 JSON 字符串片段，须在工具调用结束后拼接并解析，不能逐片段解析。
Finished 对应 finish_reason，不代表流已经结束；其后可能还有 usage，须继续收集至 [DONE]。
provider 不缓存完整回答，也不执行工具；流式工具结果同样通过 Assistant 与 Tool 历史消息回传。
外层 Result 表示请求构造是否成功；HTTP、网络与协议错误以末尾一个失败项结束 Flow。
Kotlin Result 的错误类型为 Throwable；此接口的 API 失败值均为 LLMAPIError：
OpenAIHttpException、OpenAIProtocolException 或 LLMTransportError，不附带上游响应体或敏感异常原因。
缺少 [DONE] 或 finish_reason 视为协议错误，不能把已收到的部分文本当作成功完成。

网络请求通过协程 await 挂起，流式响应通过有背压的 HTTP Publisher 接收，不阻塞调用线程。
取消协程会取消在途请求或流式 HTTP 订阅；CancellationException 不转成 Result.failure。
默认连接超时 20 秒，HTTP 请求配置超时 120 秒；流式收到响应头后的完整收集时限由调用方 withTimeout 管理。
generate 的 HTTP 非 2xx 抛 OpenAIHttpException（含 statusCode，不暴露上游响应体），
响应协议或工具参数 JSON 无效时抛 OpenAIProtocolException；其网络错误直接传播。不自动重试。
协程网络请求不访问游戏状态；调用方仍需管理任务生命周期，并在正确的游戏线程应用结果。
公共 Mod 入口不初始化 provider，不发网络请求。

测试命令（先执行 Set-DevEnv.ps1）：
    .\gradlew.bat test
    .\gradlew.bat openaiIntegrationTest
    .\gradlew.bat build
真实测试仅由测试进程加载仓库根目录 .env 中的 OPENAI_BASE_URL、OPENAI_API_KEY、OPENAI_MODEL，
分别验证非流式和流式的全局提示输入、add(7, 11) 工具调用、工具结果回传、最终结果 18 和 token usage。
本地测试覆盖流式增量交付、工具参数分片、SSE/UTF-8 分片、usage、终止协议、错误脱敏及取消释放连接。
任务每次显式执行都会调用真实 API，可能产生费用；普通 test/build 不读取 .env、不调用外网 API。
.env 仅供测试，不是运行时配置，不进入发布 JAR，也不应提交到版本控制。
报告：build/reports/tests/test/index.html、build/reports/tests/openaiIntegrationTest/index.html。

游戏内 Agent 与 Tool
==============================
架构设计：documents\客户端ReAct与Tool架构设计.md。
进入世界后默认按 G 打开 AI 助手聊天，可在控制设置中改键。首次打开会生成：
    .minecraft/config/ai-factory/model.toml
    .minecraft/config/ai-factory/agent.toml
开发客户端对应 run/config/ai-factory/。使用 Forge 已带的 NightConfig TOML，没有新增依赖。
model.toml 的 base_url、api_key、model 配置 OpenAI-compatible API；默认 api_key 为空，需自行填写。
agent.toml 的 system_prompt、max_steps、request_timeout_seconds、tool_timeout_seconds 配置 Agent。
默认步骤上限 8；模型完整请求超时 120 秒，单个 Tool 超时 15 秒。修改文件后点击“重载配置”。
配置成功重载会重置会话；已有文件不会被自动覆盖。运行时不从仓库 .env 加载模型配置。
model.toml 以明文保存 API 密钥，不要分享或提交；Git 已忽略 **/config/ai-factory/model.toml。

输入“给我 16 个钻石”并按 Enter / 发送，界面显示流式回答、Tool 状态及服务端结果。
give_item 参数为 item_id 和 count；只给予请求玩家本人，数量 1–2304。
服务端要求 /give 同等权限等级 >= 2，并检查注册物品、拒绝空气、NBT、任意命令或其他玩家目标。
背包不足时溢出物品在玩家脚边掉落；部分交付或拒绝会作为实际结果返回，不伪报全部成功。

execute_command 参数示例：{"command":"/time set day"}；也接受不带开头 / 的命令。
支持原版及模组注册的服务端命令，以请求玩家当前权限、位置、朝向和维度执行；@s 指向该玩家。
不提升权限；具体命令仍按自身规则授权。命令可影响世界或其他玩家，具有该玩家手动执行时的风险。
命令最长 32767 字符，拒绝空白、多行、NUL 与重复开头 /；反馈最多 8192 字符。
结果包含 command、success、result_code、message、output_truncated；result_code=0 本身不表示失败。
execute/function 等嵌套或多目标命令的 success 表示至少一个命令节点成功，具体效果仍需查看反馈；
无同步反馈、延迟动作或通过聊天频道发送的消息不能作为完整效果报告，已生效操作不会回滚。
输入“把时间设置为白天”即可让 Agent 选择命令 Tool。旧 agent.toml 不自动覆盖，
请将旧提示中的“仅影响自己”调整为“以请求玩家身份执行命令，命令可影响世界或其他玩家”后重载。
客户端和服务端均需安装协议版本 2 的 AI-Factory；频道缺失或版本不符的连接会被拒绝。
关闭界面或取消会停止当前回合，但已发送服务端的操作可能已生效，不代表撤销。
同一连接保留历史；断线清理会话与等待中的 Tool 请求。聊天界面不暂停游戏。

本次 build 和本地 Agent/Tool/config/协议测试通过；独立 smoke 走通本地流式 HTTP、
ReAct、注册 Tool、干运行网关、观察结果与最终回答。临时 smoke 文件验证后移除。
命令 Tool 新增本地参数 / 文本边界 / 反馈测试通过。独立命令 smoke 使用合成玩家上下文，
实际运行注册 Tool、原版 Commands 和 Forge CommandEvent，验证零结果成功、权限拒绝、
语法错误反馈、有界输出及事件取消；未启动世界或使用真实玩家，不等同游戏内验收。
按项目约束未启动 Minecraft，真实界面、服务端加载、权限与背包交互尚未完成游戏内验收；
检查清单见架构设计文档第 9 节。本次未调用付费外网 API。


官方 MDK 说明
==============================

Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "un-renamed" MCP source code (aka
SRG Names) - this means that you will not be able to read them directly against
normal code.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: `./gradlew genEclipseRuns`
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `./gradlew genIntellijRuns`
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can 
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
(this does not affect your code) and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license, if you do not agree with it you can change your mapping names to other crowdsourced names in your 
build.gradle. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

Additional Resources: 
=========================
Community Documentation: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/
LexManos' Install Video: https://youtu.be/8VEdtQLuLO0
Forge Forums: https://forums.minecraftforge.net/
Forge Discord: https://discord.minecraftforge.net/
