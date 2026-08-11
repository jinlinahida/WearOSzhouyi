# 项目概要 (Project Overview)

- Boompala 是一款面向 Wear OS 手表的离线易学工具：用户可以进行六爻手动/已知卦象排盘、梅花易数时间起卦、小六壬和罗盘查看，并浏览 APK 内置的六十四卦、爻辞与知识资料。
- 运行时不依赖网络、服务器或手机配套应用；排盘引擎和资料均来自本地模块/资源，归档使用设备本地 SQLite，设置使用 Preferences DataStore。
- 应用由单 Activity 驱动的 Compose 页面状态机组成，`app` 负责 Wear OS UI、传感器和存储，`engine` 负责与 UI 无关的历法、卦象规则及数据校验。

# 技术栈 (Tech Stack)

- 语言：Kotlin（官方代码风格，JVM target 17）；少量 Node.js ESM 工具脚本用于导入资料。
- 平台/框架：Android Wear OS，Jetpack Compose、Compose for Wear Material 3/Foundation、`ComponentActivity`。
- 构建：Gradle Wrapper，Android Gradle Plugin 8.7.3，Kotlin 2.0.21，Compose compiler plugin，compile/target SDK 35，min SDK 33；Java/Kotlin 编译目标为 17。
- 关键库：`androidx.activity:activity-compose`、Compose UI/Animation 1.9.0、Wear Compose 1.6.2、Preferences DataStore 1.1.1、Gson 2.10.1、`cn.6tail:lunar:1.7.7`、JUnit 4.13.2。
- 存储：Android `SQLiteOpenHelper` 本地数据库 `boompa_archives.db`（schema version 2，归档表及 `cast_at` 索引）；DataStore 文件 `app_settings` 保存界面设置；没有 Room、网络 API 或远程数据库。
- Android 入口：`app/src/main/AndroidManifest.xml` 声明 standalone、必需的 watch feature 和 launcher Activity；未声明网络或定位权限。

# 项目目录结构 (Directory Structure)

```text
.
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Wear OS standalone 清单与 MainActivity
│       │   ├── java/com/boompala/
│       │   │   ├── MainActivity.kt       # ComponentActivity，挂载 BoompalaApp
│       │   │   ├── archive/              # SQLite 归档模型、数据库、快照编解码
│       │   │   ├── compass/              # 方位计算、传感器控制、三元九运数据
│       │   │   ├── settings/             # AppSettings 与 Preferences DataStore
│       │   │   └── ui/                   # Compose 页面、导航状态、Wear 滚动与显示投影
│       │   └── res/values/                # 应用名和主题资源
│       ├── test/                          # app JVM 测试：归档、设置、UI 投影/导航
│       └── androidTest/                   # Compose/Wear 交互测试（当前为旋转滚动测试）
├── engine/
│   └── src/
│       ├── main/kotlin/com/boompala/engine/
│       │   ├── BasicHexagramEngine.kt    # 四态输入到本卦/变卦的基础推导
│       │   ├── LiuYaoEngine.kt            # 六爻完整装卦编排
│       │   ├── model/                     # 输入、时间、卦、爻、干支、结果模型
│       │   ├── rules/                     # 八宫、卦名、纳甲、六亲、六神、旬空、八卦
│       │   ├── calendar/                  # GanzhiCalendar seam 与 6tail 适配器
│       │   ├── data/                      # 卦辞/爻辞/知识 repository 与解析校验
│       │   ├── meihua/                    # 梅花时间起卦、互卦、体用
│       │   └── xiaoliuren/                # 小六壬六宫循环规则与模型
│       ├── main/assets/                   # APK 内离线 JSON 和 NOTICE 许可文件
│       └── test/kotlin/                   # engine 规则、历法、数据和回归测试
├── docs/                                  # 架构、规则口径、数据来源和许可研究
├── tools/import_wikisource_yao_text.mjs   # Wikisource 爻辞导入/清洗工具
├── gradle/wrapper/                        # Gradle Wrapper
├── CONTEXT.md                             # 六爻领域词汇与术语边界
├── README.md                              # 功能、构建、验证和许可概览
├── settings.gradle.kts                    # 仅 include :app、:engine
├── build.gradle.kts                       # 根级插件版本声明
└── gradle.properties                      # AndroidX、JVM、Gradle 缓存/并行配置
```

`feature/liuyao/` 不是当前 Gradle 工程中的已启用模块（`settings.gradle.kts` 未 include）；其中出现的 `build/` 属于生成物，不应作为源码入口。`local.properties` 只保存本机 `sdk.dir`，被 `.gitignore` 忽略，不要复制为团队配置。

# 核心架构与模块设计 (Architecture & Key Modules)

## 运行时数据流

```text
设备公历 Instant + ZoneId
        │
        ▼
SixTailGanzhiCalendar (Solar -> Lunar -> EightChar)
        │
        ├── LiuYaoEngine: HexagramInput -> BasicHexagramEngine -> 八宫/纳甲/六亲/六神/旬空 -> DivinationResult
        ├── MeiHuaTimeEngine: 农历年支/月/日/时支数 -> 本卦/互卦/变卦/体用
        └── XiaoLiuRenEngine: 农历月 -> 日 -> 时 -> 六宫结果

assets JSON -> Json*Repository -> BoompalaApp 离线依赖 -> 浏览页/结果页
结果 -> ArchiveSnapshotCodec -> SQLite archives -> 详情页 decode（从快照读取，不重新计算）
```

## 主要模块

- `engine.model` 是稳定领域合同。六条输入、`HexagramPattern.linesFromBottom`、`Hexagram.yaoFromBottom` 始终是“初爻到上爻”，`YaoPosition.indexFromBottom` 是算法索引；六爻模式要求恰好六条线。
- `BasicHexagramEngine` 只处理 6/7/8/9 的阴阳与动静；`LiuYaoEngine` 在此之上调用 `HexagramRules`、`NajiaRules`、`SixRelationRules`、`SixSpiritRules`、`VoidRules`，并为本卦/变卦分别重新装卦。
- `DirectHexagramInput` 将本卦与变卦逐爻差异转换成老阴/老阳，从而复用同一条六爻计算路径，不要新建第二套装卦算法。
- `SixTailGanzhiCalendar` 必须先用 `Solar.fromYmdHms(...).getLunar()`，再交给 `EightChar.fromLunar`；`DivinationTimeInfo` 是六爻、梅花和小六壬共用的时间模型。晚子时“算明天”由 calendar 策略控制。
- 梅花模块只使用自己的 `MeiHuaTimeReading` 和三线经卦投影，不携带六爻纳甲数据；互卦取本卦二三四爻/三四五爻，规则固定见 `docs/meihua-time-casting-rules.md`。
- `BoompalaApp.kt` 是应用级状态机：`AppScreen`、生成中的 request id、当前结果、浏览选择和归档返回目标均在此协调；没有独立 NavHost。
- UI 页面按功能拆分：`YaoInputScreen` 输入，`ResultScreen` 六爻结果，`MeiHua*` 梅花结果，`XiaoLiuRenScreen` 小六壬，`CompassScreen` 罗盘，`BrowseScreens` 离线资料，`ArchiveScreens` 归档，`SettingsScreen/AboutScreen` 设置与关于。
- 所有纵向 Wear 页面优先复用 `RotaryScrollColumn`：`ScreenScaffold` 与 `LazyColumn` 共享同一 `LazyListState`；启用时安装 `RotaryScrollableDefaults.behavior` 和 haptic，scaffold 提供右侧位置指示；禁用表冠时仍保留触摸滚动和点击。
- 六爻结果显示通过 `forResultDisplay()` 在 UI 边界按 `indexFromBottom` 倒序；`YaoLineDisplay` 将阴阳线形状与动爻标记分离。三线经卦必须使用 `trigramLineDisplayAt`，不能塞入要求六线的 `HexagramDisplayModel`。
- 罗盘读取加速度计/磁力计并可回退 rotation vector，做屏幕 remap、磁北方位规范化和跨 359°/0° 平滑；Compose 状态发布限制为最高约 15Hz。传统口径、24 山、后天八卦和元运数据以 `docs/compass-data-conventions.md` 为准。

# AI 协作与开发规范 (Rules & Conventions)

## 必须保持的合同

- 不改变 engine 的初爻到上爻数组语义、`YaoPosition`、`position.indexFromBottom`、6/7/8/9 含义或 `DivinationResult` 结构来解决显示问题。需要倒序时只在 UI、序列化或列表投影边界处理。
- 不复制六十四卦名称/卦象表或六爻计算逻辑；复用 `HexagramCatalog`、`HexagramReference`、现有 repository 和 engine。三卦/三线数据不要进入六线模型。
- 变卦必须由动爻阴阳翻转得到；变卦的八宫、纳甲和六亲按当前代码重新计算，不能沿用本卦宫。
- 历法输入是设备公历时间，但算法前必须走 Solar -> Lunar；绝不能把 Gregorian 月日直接传给 `Lunar.fromYmdHms`。不得用 try/catch 隐藏可复现的规则或数据错误。
- 梅花、小六壬、罗盘的口径是项目明确选择的可复现规则；修改规则时同步更新对应 `docs/*-rules.md` 或 `docs/compass-data-conventions.md` 与测试，不混入另一流派的公式。

## 代码与数据规范

- Kotlin 使用官方风格、4 空格缩进、`PascalCase` 类型、`camelCase` 函数/属性、枚举常量大写；优先使用不可变 `data class`、显式类型边界和小型纯函数。
- UI 文本与 Compose 结构改动要保留 40mm 圆屏可操作性、间距、动画、返回逻辑和滚动指示器。设置关闭 rotary 只代表不响应表冠，不得禁用触摸滚动/点击/导航。
- 引擎和 repository 保持 Android/UI 无关；资料解析必须验证 schema、来源/许可、非空字段、64 卦唯一性以及 `yao_text.json` 的 64×6/384 条结构。
- 新增或修改离线资产时同步更新对应 `NOTICE-*.txt`、`docs/research/` 和解析测试。运行时资源必须放在 `engine/src/main/assets/`，不要改成网络加载。
- 归档保存完整快照；详情按 `archiveDetailId -> ArchiveRepository.get -> ArchiveSnapshotCodec.decode` 加载。保留重复事件记录、schema version 和原始快照，避免用当前引擎重新计算历史结果。
- 传感器在页面可见/生命周期恢复时注册，不可见/暂停时注销；使用 `CompassMath` 的规范化和最短圆周平滑，不在 UI 中重写方位公式。
- 不提交 `.gradle/`、`.kotlin/`、`build/`、`local.properties`、APK 或 IDE 文件。不要手改 Gradle 缓存；依赖版本以两个 module 的 `build.gradle.kts` 为准。

## 验证与已知陷阱

- 常规 JVM 测试：`./gradlew --no-daemon :engine:test :app:testDebugUnitTest`。
- Debug 构建：`./gradlew --no-daemon :app:assembleDebug`；输出为 `app/build/outputs/apk/debug/app-debug.apk`。
- Release 当前建议：`./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease -x :app:lintVitalAnalyzeRelease`；输出为 `app/build/outputs/apk/release/app-release.apk`。Release 使用 debug signing，适合本地安装，不是商店发布包。
- 每次提交前运行 `git diff --check`，并检查 `git status` 确认只包含目标文件。
- 当前工具链已知限制：标准 `lintVitalAnalyzeRelease` 可能因 Kotlin Analysis API/`NonNullableMutableLiveDataDetector` 的 `IncompatibleClassChangeError` 失败；排除 lint 生成的 APK 只能说明源码编译/打包完成，不能宣称 Release lint 通过。
- 编译、JVM 测试和 Compose 注入测试不等同于真实手表验证。若没有连接 Wear OS 手表/模拟器，不要宣称表冠滚动、震动、右侧指示器、传感器方向、圆屏布局或帧率已验证。
- 怀疑崩溃时先定位真实调用链并复现；优先修正数据边界/模型投影，禁止通过扩大 `try/catch`、放宽 `require` 或吞异常掩盖问题。
- 变更应保持局部，不要无授权重构模块、替换架构、改变数据库历史数据或删除与请求无关的资料/许可/状态文本。

