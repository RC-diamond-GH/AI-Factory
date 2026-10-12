本地开发环境
==============================
Minecraft 1.20.1 / Forge 47.4.10（官方推荐版）/ Gradle Wrapper 8.8。
示例 Mod 已使用 Kotlin 2.2.21 改写，依赖 Kotlin for Forge 4.12.0。
保留 examplemod 标识与示例配置，Mod 信息在 gradle.properties 中修改。
Kotlin 源码位于 src\main\kotlin；开发注意事项见 documents\Kotlin开发注意事项.md。

在项目目录的 PowerShell 中执行：
    . .\Set-DevEnv.ps1
    .\gradlew.bat build
    .\gradlew.bat runClient

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
已执行 genIntellijRuns，可选择生成的客户端/服务端启动配置。
Eclipse：可使用 genEclipseRuns 生成启动配置；Kotlin 编辑优先使用 IntelliJ IDEA。
构建产物：build\libs\examplemod-1.0.0.jar。
示例内容：examplemod:example_block 方块与方块物品、examplemod:example_item 食物、
examplemod:example_tab 独立创造模式物品栏；包含模型、中英文名称、方块掉落和镐挖掘标签。
运行发布 JAR 时，客户端与服务端均需另外安装兼容 1.20.1 的 Kotlin for Forge 4.12.x。
本次 Kotlin 迁移不启动客户端或服务端，游戏内加载、物品栏、放置与掉落由使用者手动验证。
构建验证：Gradle build 成功，reobfJar 已执行；发布 JAR 包含 Java 17 目标的 Kotlin 类、
展开后的 KFF 加载器元数据和全部 8 个 JSON 资源。项目当前没有自动化测试。

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
