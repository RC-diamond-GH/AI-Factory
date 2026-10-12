# AI-Factory Kotlin Mod 开发注意事项

## 1. 本项目的技术栈

| 组件 | 配置 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.4.10 |
| ForgeGradle | `[6.0,6.2)` |
| Gradle Wrapper | 8.8 |
| Kotlin JVM 插件 | 2.2.21 |
| Kotlin for Forge（KFF） | 4.12.0 |
| Java 与 Kotlin 字节码目标 | Java 17 |
| Gradle 启动 JVM | 本机 JDK 21 |

Kotlin/JVM 编译为 JVM 字节码，直接调用 Forge 和 Minecraft 的 Java API。项目保留 `build.gradle` 的 Groovy DSL；使用 Kotlin 写 Mod 不要求改成 `build.gradle.kts`。

KFF 4.12.0 提供 Kotlin 2.2.21 标准库与反射、协程及 JSON 序列化运行库，并提供支持 Kotlin `object` 的语言加载器。KFF 的版本必须同时适配 Minecraft、Forge 和 Kotlin 编译器，不要直接照抄针对 Minecraft 1.21 或 NeoForge 的依赖。

## 2. 构建与启动

在项目根目录的 PowerShell 中执行：

```powershell
. .\Set-DevEnv.ps1
.\gradlew.bat build
.\gradlew.bat runClient
```

- `Set-DevEnv.ps1` 为当前会话设置 JDK 21 和代理；若不执行，系统默认 Java 可能不满足 Gradle 启动要求。
- `java.toolchain` 和 `kotlin.jvmToolchain(17)` 控制 Mod 的编译目标。Gradle 使用 Java 21 不代表游戏或 Mod 可以使用 Java 21 API。
- 本机 JDK 路径在 `gradle.properties` 和 `Set-DevEnv.ps1` 中；换机器时需要调整。
- 代理需监听 `127.0.0.1:7897`。首次构建需下载 Kotlin 插件和 KFF，离线缓存不全时无法构建。
- 构建产物为 `build/libs/ai_factory-1.0.0.jar`，`build` 会执行 Forge 的 `reobfJar`；不要把未重混淆的开发产物作为发布版本。

生成或更新 IDE 启动配置：

```powershell
.\gradlew.bat genIntellijRuns genEclipseRuns
```

IntelliJ IDEA 中用 Gradle 导入项目，Gradle JVM 选择 JDK 21，项目 SDK 选择 JDK 17，启用 Kotlin 支持。Eclipse 的 Forge 启动配置可生成，但 Java 编辑器本身不提供完整 Kotlin 语言支持，Kotlin 开发优先使用 IntelliJ IDEA。

独立服务端开发启动使用 `runServer`。必须先阅读并接受 Minecraft EULA，再在对应运行目录的 `eula.txt` 中设置 `eula=true`；不要让脚本替用户接受协议。不要让开发客户端与独立服务端同时修改同一个世界或日志目录。

## 3. 目录与框架范围

- Kotlin 源码：`src/main/kotlin/com/rcdiamond/aifactory/`。
- Java 源码（如需要混合开发）：`src/main/java/`。
- 新增客户端资源使用：`src/main/resources/assets/ai_factory/`。
- 新增服务端数据使用：`src/main/resources/data/ai_factory/`。
- 元数据：`src/main/resources/META-INF/mods.toml`。
- Mod 标识和发布信息：`gradle.properties`。
- 项目名称：`AI-Factory`；Mod ID：`ai_factory`；入口：`AIFactory`。
- 项目许可证为远端仓库既有的 Apache-2.0，根目录 `LICENSE` 随发布 JAR 打包。`LICENSE.txt` 与 `CREDITS.txt` 保留 Forge MDK 的许可和归属信息。

当前为最小可加载框架：入口通过 KFF 的 Mod 事件总线订阅公共初始化事件并记录初始化完成。没有注册方块、物品或创造模式物品栏，没有业务配置、客户端初始化、机器或配方。

新增实际内容时再添加 `DeferredRegister`、配置和资源；不要保留空注册器、无功能事件处理器或示例占位内容。

## 4. 加载器与发布依赖

`mods.toml` 使用：

```toml
modLoader="kotlinforforge"
loaderVersion="${loader_version_range}"
```

当前 `loader_version_range=[4.12,5)` 表示 KFF 语言加载器范围，不是 Forge 47 的范围；Forge 要求仍在 `dependencies` 中单独声明。资源处理任务在构建时展开变量。

开发环境由 Gradle 的 `implementation` 依赖提供 KFF。这个依赖声明不会自动把整个 KFF 打进 Mod JAR。发布时，客户端和独立服务端均需安装适配 Minecraft 1.20.1 的 Kotlin for Forge，并在下载页声明该依赖。仅复制本 Mod JAR 到没有 KFF 的环境不能运行。

不要额外手动打包一套 Kotlin 标准库来绕过 KFF，否则容易发生类加载与版本冲突。升级 Kotlin 时同时核对 KFF 提供的运行库版本；编译期可用的 API 不一定在玩家安装的旧运行库里存在。

Forge 开发模式需要正确发现 Mod 的类和元数据。本项目把 Java、Kotlin、资源输出统一到 `build/modClasses/<sourceSet>`；不要只把 Java 输出加入运行配置，漏掉 Kotlin 类或 `mods.toml`。`runClient`、`runServer`、`runData` 等仍使用同一个 `sourceSets.main`。

## 5. 入口、初始化和注册生命周期

`AIFactory` 是带 `@Mod` 的 Kotlin `object`，由 KFF 创建。入口使用 `thedarkcolour.kotlinforforge.forge.MOD_BUS` 获得 Mod 事件总线，不依赖 `FMLJavaModLoadingContext` 的 Java 入口构造函数。未来需要配置时使用同包的 `registerConfig` 辅助函数。KFF 的 `MOD_CONTEXT` 是 Kotlin 加载上下文，不应当作 Java 加载上下文调用其成员。

- `object` 第一次访问时会初始化；这不等于 Forge 注册事件已经完成。
- 使用 `DeferredRegister` 注册方块、物品和创造模式物品栏，并连接 **Mod 事件总线**。
- `RegistryObject.get()` 只能在对应注册完成后调用。可以放在方块物品的注册工厂、创造模式物品栏图标及内容回调中，不要在顶层字段初始化时提前读取。
- 生命周期事件属于 Mod 事件总线，服务器启动等游戏事件属于 `MinecraftForge.EVENT_BUS`，不要混淆。
- 需要主线程执行的生命周期工作通过 `event.enqueueWork` 排队；不要假设所有加载事件都在游戏主线程执行。

## 6. Kotlin 对象与事件订阅

当前 `AIFactory` 通过 `MOD_BUS.addListener` 订阅公共初始化事件。未来新增事件处理器可选择：

1. 显式注册对象实例，用实例 `@SubscribeEvent` 方法处理事件。
2. 使用 `@Mod.EventBusSubscriber`；KFF 支持 Kotlin `object` 的自动订阅。客户端处理器必须用 `Dist.CLIENT` 限制加载。

不要同时自动订阅和手动注册同一处理器，否则可能重复处理事件。

Kotlin 没有 Java 式 `static` 成员。`companion object`、顶层函数、`object` 的 JVM 表示不同：

- Java 必须直接调用静态方法时，考虑 `@JvmStatic`。
- Java API 必须反射读取公开字段时，考虑 `@JvmField`；普通 Kotlin 属性通常生成访问器。
- 编译期常量使用 `const val`，例如 Mod ID。
- 用于字段、访问器或参数的注解应核对目标，必要时写 `@field:`、`@get:`、`@param:`。

不要为每个 Kotlin 属性或事件方法无差别添加 JVM 注解；先确认 Java 调用方或加载器究竟要求什么。

## 7. Java 互操作、空值与配置

Minecraft/Forge 的 Java API 可能返回 Kotlin 平台类型。平台类型没有可靠的非空保证：

- 不要通过大量 `!!` 把问题推迟到运行时。
- 外部输入或查询结果可能为空时，明确处理 `null`。
- 资源 ID 输入用 `ResourceLocation.tryParse` 验证，再检查目标是否注册，避免非法资源 ID 直接抛异常。
- 合法资源 ID 不代表注册表中一定有该物品；两项检查都需要。

当前不注册业务配置。未来新增配置时，只处理本 Mod 的配置事件，加载与重载时更新缓存，不要在配置加载前读 `ConfigValue.get()`。集合初始化为空集合，避免未初始化的 `lateinit` 在事件时序变化时崩溃。

Java 泛型通配符、SAM 回调和 Kotlin 类型推断可能导致编译错误；优先补充明确类型或使用方法引用，不要为了消除编译错误使用不安全强制转换。

## 8. 客户端与服务端隔离

`Minecraft`、渲染器、屏幕等类只存在于客户端。不要在公共入口的字段类型、对象初始化或注册工厂中引用这些类。

当前入口不引用客户端专用类。未来新增客户端初始化时放入独立的客户端包，并用 `Dist.CLIENT` 限制自动订阅。仅仅把客户端调用包在普通条件分支里，不一定能避免类加载时触发独立服务端错误。

每次涉及客户端类、对象初始化或入口修改时，都需要检查独立服务端；客户端能启动不代表服务端安全。

## 9. 协程、线程和性能

KFF 提供协程运行库，但这不意味着 Forge API 自动具备线程安全性。

- 不要在 `Dispatchers.IO` 或 `Dispatchers.Default` 中直接读写世界、实体、容器或玩家状态。
- 异步计算或 IO 完成后，使用对应服务器/客户端的任务队列回到正确线程。
- 避免 `GlobalScope`：任务需要可取消的生命周期，世界卸载、断线或服务器停止时应清理。
- 网络包处理遵循 Forge 的线程调度规则，不能因使用协程省略主线程切换。

Kotlin 不是天然慢，但集合转换、捕获 lambda、装箱、临时 `data class` 和序列链可能分配对象。高频 tick 和渲染代码优先使用直接循环与复用的数据结构；低频初始化则以可读性为主。不要为“更 Kotlin”把简单循环改成分配更重的调用链。

## 10. 资源与验收

方块和物品注册成功不等于资源完整。新增内容时同时核对：注册 ID、方块状态、模型、纹理引用、翻译、掉落表、标签。资源路径区分大小写，ID 使用小写命名。

本次修改按要求仅执行构建与打包验证，不启动客户端或服务端。以下游戏内检查由使用者手动完成，不应把构建成功视为这些检查已经通过。

发布前至少检查：

1. `build` 成功，发布 JAR 中包含 Kotlin 类、展开后的 `mods.toml` 和资源。
2. 客户端与独立服务端均能加载 AI-Factory；公共入口不加载客户端专用类，能进入世界。
3. 有方块、物品和创造模式物品栏后，再检查模型、名称、图标、放置与破坏行为；当前框架没有这些内容。
4. 有掉落表和世界交互后，再检查生存模式与服务端行为；当前不声明这些检查已通过。
5. 项目当前没有自动化测试；`test NO-SOURCE` 不是业务测试通过。
6. 在非开发环境同时安装 Forge、KFF 和重混淆后的 Mod JAR，验证实际发布加载。

Mixin 如有需要可保留 Java 实现，减少字节码签名和注解处理方面的摩擦；其他功能仍可以使用 Kotlin。不要求把整个项目改成纯 Kotlin。

## 11. 参考资料

- [Kotlin for Forge 4.x 官方说明](https://github.com/thedarkcolour/KotlinForForge/blob/4.x/README.md)
- [KFF 4.12.0 发布依赖](https://thedarkcolour.github.io/KotlinForForge/thedarkcolour/kfflib/4.12.0/kfflib-4.12.0.pom)
- [Kotlin Gradle 配置与版本兼容性](https://kotlinlang.org/docs/gradle-configure-project.html)
- [Forge 1.20.1 开发文档](https://docs.minecraftforge.net/en/1.20.1/)
- [本项目单元测试实施方案](./documents/单元测试实施方案.md)
