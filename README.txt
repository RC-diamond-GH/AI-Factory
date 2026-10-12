AI-Factory
==============================
仓库：https://github.com/RC-diamond-GH/AI-Factory
Minecraft 1.20.1 / Forge 47.4.10 / Gradle Wrapper 8.8。
Kotlin 2.2.21 / Kotlin for Forge 4.12.0 / Java 17 编译目标。
Mod ID：ai_factory；入口：com.rcdiamond.aifactory.AIFactory；版本：1.0.0。
当前为最小可加载框架：订阅公共初始化事件并记录初始化完成。
没有方块、物品、创造模式物品栏、业务配置或客户端专用代码。
新增真实功能时再添加对应注册器、配置与 assets/ai_factory、data/ai_factory 资源。
Kotlin 源码位于 src\main\kotlin\com\rcdiamond\aifactory；元数据在 gradle.properties 中配置。
开发注意事项见 AGENTS.md；测试方案见 documents\单元测试实施方案.md。

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
项目当前没有自动化测试，test 显示 NO-SOURCE；这不是业务测试通过。
按项目约束未启动客户端或服务端，实际 Forge/KFF 加载与非开发环境发布验证尚未执行。
启动独立服务端前须自行阅读并接受 Minecraft EULA；脚本不会代为接受。

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
