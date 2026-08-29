# 项目概要 (Project Overview)

- Boompala 是一款面向 Wear OS 手表的离线易学与占卜工具：用户可以进行六爻排盘（手动四态/已知卦象/铜钱摇卦）、梅花易数时间起卦、小六壬起课、塔罗牌占卜（单牌/时间流三牌/圣三角牌阵）、今日运势（黄历建除十二神与六十四卦每日值日轮转，以及结合用户生辰八字的流日个人专属运势评估）、命盘计算（八字排盘与起大运、西洋占星本命星盘、毕达哥拉斯生命灵数、袁天罡称骨算命、九星气学）和罗盘查看，并浏览 APK 内置的六十四卦、爻辞、易学知识与 78 张韦特塔罗牌图鉴释义。
- 运行时不依赖网络、服务器或手机配套应用；排盘与命理引擎、资料均来自本地模块/资源，归档使用设备本地 SQLite，设置使用 Preferences DataStore。
- 应用由单 Activity 驱动的 Compose 页面状态机组成，`app` 负责 Wear OS UI、传感器和存储，`engine` 负责与 UI 无关的历法、卦象规则、黄历运势、命盘命理、塔罗算法及数据校验。

# 技术栈 (Tech Stack)

- 语言：Kotlin（官方代码风格，JVM target 17）；少量 Node.js ESM 工具脚本用于导入资料。
- 平台/框架：Android Wear OS，Jetpack Compose、Compose for Wear Material 3/Foundation、`ComponentActivity`。
- 构建：Gradle Wrapper，Android Gradle Plugin 8.7.3，Kotlin 2.0.21，Compose compiler plugin，compile/target SDK 35，min SDK 26；Java/Kotlin 编译目标为 17。
- 关键库：`androidx.activity:activity-compose:1.10.0`、Compose UI/Animation 1.9.0、Wear Compose 1.6.2、`androidx.graphics:graphics-shapes:1.0.1`、Preferences DataStore 1.1.1、Gson 2.10.1、`cn.6tail:lunar:1.7.7`、JUnit 4.13.2。
- 存储：Android `SQLiteOpenHelper` 本地数据库 `boompa_archives.db`（schema version 2，归档表及 `cast_at` 降序索引）；DataStore 文件 `app_settings` 保存界面、偏好及用户生辰档案设置；没有 Room、网络 API 或远程数据库。
- Android 入口：`app/src/main/AndroidManifest.xml` 声明 standalone、必需的 watch feature、`android.permission.VIBRATE`（用于直驱震动马达保障不同手表 ROM 上触觉反馈的可靠性）和 launcher Activity；未声明网络或定位权限。

# 项目目录结构 (Directory Structure)

```text
.
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Wear OS standalone 清单与 MainActivity (含 VIBRATE 权限)
│       │   ├── java/com/boompala/
│       │   │   ├── MainActivity.kt       # ComponentActivity，挂载 BoompalaApp
│       │   │   ├── archive/              # SQLite 归档模型、数据库、快照编解码 (六爻/梅花/小六壬/塔罗)
│       │   │   ├── compass/              # 方位计算、传感器控制、三元九运数据
│       │   │   ├── settings/             # AppSettings 与 Preferences DataStore (字号/模式/排序/生辰/震动等)
│       │   │   └── ui/                   # Compose 页面 (主页/引导/排盘/运势/命盘/塔罗/罗盘/归档/资料)、背景光效与 Wear 滚动
│       │   └── res/                      # 应用名、图标、矢量图、78 张韦特塔罗 WebP 牌图与主题资源
│       ├── test/                          # app JVM 测试：归档、设置、UI 投影/导航/转场/背景/命盘/卡片
│       └── androidTest/                   # Compose/Wear 交互测试（当前为表冠旋转滚动测试）
├── engine/
│   └── src/
│       ├── main/kotlin/com/boompala/engine/
│       │   ├── BasicHexagramEngine.kt    # 四态输入到本卦/变卦的基础推导
│       │   ├── LiuYaoEngine.kt            # 六爻完整装卦编排
│       │   ├── astrology/                 # 西洋占星本命盘引擎 (十大行星经度/黄道十二宫/主要相位)
│       │   ├── bazi/                      # 八字命盘引擎 (四柱干支/藏干/十神/纳音/起大运)
│       │   ├── bone/                      # 袁天罡称骨算命引擎 (骨重换算与批注歌诀)
│       │   ├── calendar/                  # GanzhiCalendar seam、6tail 适配器与黄历建除数据源
│       │   ├── dailyfortune/              # 今日运势引擎：建除十二神、黄道吉凶、值日卦轮转与个人流日运势评估
│       │   ├── data/                      # 卦辞/爻辞/知识/塔罗 repository 与解析校验
│       │   ├── liuyao/                    # 六爻金钱掷卦模拟引擎
│       │   ├── meihua/                    # 梅花时间起卦、互卦、体用
│       │   ├── model/                     # 输入、时间、卦、爻、干支、结果模型
│       │   ├── ninestar/                  # 九星气学引擎 (本命星/月命星/倾斜星/主导五行)
│       │   ├── numerology/                # 毕达哥拉斯生命灵数引擎 (卓越数/态度数/流年数/九宫数盘)
│       │   ├── rules/                     # 八宫、卦名、纳甲、六亲、六神、旬空、八卦
│       │   ├── tarot/                     # 塔罗牌引擎、牌组、洗牌与牌阵 (单牌/时间流/圣三角)
│       │   └── xiaoliuren/                # 小六壬六宫循环规则与模型
│       ├── main/assets/                   # APK 内离线 JSON (爻辞/解释/知识/塔罗) 和 NOTICE 许可文件
│       └── test/kotlin/                   # engine 各模块规则、历法、数据、塔罗、运势、命理和回归测试
├── docs/                                  # 架构、规则口径 (六爻/梅花/小六壬/运势/罗盘)、数据来源和许可研究
├── tools/import_wikisource_yao_text.mjs   # Wikisource 爻辞导入/清洗工具
├── gradle/wrapper/                        # Gradle Wrapper
├── CONTEXT.md                             # 六爻领域词汇与术语边界
├── README.md                              # 功能、构建、验证和许可概览
├── settings.gradle.kts                    # 仅 include :app、:engine
├── build.gradle.kts                       # 根级插件版本声明
└── gradle.properties                      # AndroidX、JVM、Gradle 缓存/并行配置
```

`feature/liuyao/` 不是当前 Gradle 工程中的已启用模块（`settings.gradle.kts` 未 include）；其中出现的 `build/` 属于历史生成物，不应作为源码入口。`local.properties` 只保存本机 `sdk.dir`，被 `.gitignore` 忽略，不要复制为团队配置。

# 核心架构与模块设计 (Architecture & Key Modules)

## 运行时数据流

```text
设备公历 Instant + ZoneId + 用户生辰档案配置 (LocalDate + Hour + Gender)
        │
        ├── SixTailGanzhiCalendar (Solar -> Lunar -> EightChar)
        │       │
        │       ├── LiuYaoEngine: HexagramInput -> BasicHexagramEngine -> 八宫/纳甲/六亲/六神/旬空 -> DivinationResult
        │       ├── MeiHuaTimeEngine: 农历年支/月/日/时支数 -> 本卦/互卦/变卦/体用 -> MeiHuaTimeReading
        │       └── XiaoLiuRenEngine: 农历月 -> 日 -> 时 -> 六宫结果 -> XiaoLiuRenReading
        │
        ├── DailyFortuneEngine: GanzhiCalendar + SixTailDailyAlmanac (建除神/黄道) + HexagramRotation (值日卦) -> DailyFortuneReading
        │       └── (生辰已配置) + PersonalFortuneEvaluator -> PersonalDailyFortune (日主十神/神煞合冲/喜用色数/卦象共鸣)
        │
        ├── DestinyChartEngines (纯 Kotlin 本地确定性推算):
        │       ├── BaziEngine: 四柱八字 -> 藏干/十神/纳音/起大运 -> BaziProfile
        │       ├── WesternAstrologyEngine: 天文黄道经度 -> 十大行星/黄道星座/宫位/相位 -> WesternChartReading
        │       ├── NumerologyEngine: 出生年月日数理归约 -> 命数/卓越数/态度数/流年/九宫盘 -> NumerologyReading
        │       ├── BoneWeightEngine: 年月日时骨重累加 -> 骨重总两钱与批诗 -> BoneWeightReading
        │       └── NineStarKiEngine: 节气农历年份 -> 本命/月命/倾斜九星与五行 -> NineStarKiReading
        │
        └── TarotEngine: 78 张牌库 (大阿卡纳/小阿卡纳) + 洗牌/概率抽卡 -> 单牌/时间流三牌/圣三角 -> TarotReading

assets JSON -> Json*Repository -> BoompalaApp 离线依赖 -> 浏览页/结果页
占卜结果 -> ArchiveSnapshotCodec -> SQLite archives -> 详情页 decode（从快照读取，不重新计算）
```

## 主要模块与设计

- `engine.model` 是稳定领域合同。六条输入、`HexagramPattern.linesFromBottom`、`Hexagram.yaoFromBottom` 始终是“初爻到上爻”，`YaoPosition.indexFromBottom` 是算法索引；六爻模式要求恰好六条线。
- `BasicHexagramEngine` 只处理 6/7/8/9 的阴阳与动静；`LiuYaoEngine` 在此之上调用 `HexagramRules`、`NajiaRules`、`SixRelationRules`、`SixSpiritRules`、`VoidRules`，并为本卦/变卦分别重新装卦。
- `DirectHexagramInput` 将本卦与变卦逐爻差异转换成老阴/老阳，`LiuYaoCoinCastingEngine` 将掷铜钱结果转换为 6/7/8/9，从而复用同一条六爻装卦推导路径。
- `SixTailGanzhiCalendar` 必须先用 `Solar.fromYmdHms(...).getLunar()`，再交给 `EightChar.fromLunar`；`DivinationTimeInfo` 是六爻、梅花、小六壬和运势共用的时间模型。晚子时“算明天”由 calendar 策略控制。
- 梅花模块只使用自己的 `MeiHuaTimeReading` 和三线经卦投影，不携带六爻纳甲数据；互卦取本卦二三四爻/三四五爻，规则固定见 `docs/meihua-time-casting-rules.md`。
- `DailyFortuneEngine` 基于历法四柱、`SixTailDailyAlmanac` 计算建除十二神与十二神黄黑道吉凶，通过 `HexagramRotation` 计算六十四卦按农历节气与日期的轮转值日卦，并绑定离线卦辞与释义。当用户在设置中配置了出生日期时，`PersonalFortuneEvaluator` 进一步计算日主十神、神煞合冲、吉凶事件及五行平衡色数，规则见 `docs/daily-fortune-rules.md`。
- `DestinyChartEngines`（命盘系统）采用纯函数、离线、确定性计算：
  - `BaziEngine`：根据生日节气八字推导四柱、藏干、十神、纳音与顺逆起大运；
  - `WesternAstrologyEngine`：根据公历时间计算太阳至冥王星十大天体黄道经度、上升点（Ascendant）、黄道十二宫分布及主要相位（合相/六分相/刑相/三分相/对分相）；
  - `NumerologyEngine`：毕达哥拉斯生命灵数，支持卓越数（11/22/33）、生日数、态度数、个人流年数及九宫数盘；
  - `BoneWeightEngine`：袁天罡称骨算命，累加年月干支与农历月日时骨重，输出总两钱及命理诗；
  - `NineStarKiEngine`：九星气学推算本命星、月命星、倾斜星及主导五行。
- `TarotEngine` 提供标准 78 张韦特塔罗牌与 22 张大阿卡纳抽卡模式，随机洗牌带正逆位判定，支持单牌占卜、时间流三牌（过去/现在/未来）与圣三角牌阵（现状/阻碍/对策）。
- `BoompalaApp.kt` 是应用级状态机：管理 30 个 `AppScreen` 状态，协调当前排盘结果、命盘计算展示、生成 request id、浏览选择、归档草稿与详情返回目标；支持 `SwipeToDismissBox` 滑动返回与自定义页面转场动效。
- `AppSettings` 与个性化：支持圆屏/方屏/自动模式、字体缩放（小 0.9x/标准 1.0x/大 1.1x）、动画开关、表冠旋转开关、触觉震动开关与三档强度调节（弱/标准/强劲）、应用语言（中/英）、首页 11 大功能模块自定义排序与显隐管理、用户生辰八字与性别档案配置（用于驱动命盘与流日个人专属运势），以及新用户引导状态记录。
- 视觉与交互规范：
  - 所有纵向 Wear 页面优先复用 `RotaryScrollColumn`：`ScreenScaffold` 与 `LazyColumn` 共享同一 `LazyListState`；启用时安装 `RotaryScrollableDefaults.behavior` 和 haptic，scaffold 提供右侧位置指示；禁用表冠时仍保留触摸滚动和点击。
  - `AppBackground.kt` 提供根据当前页面主题自适应的环境顶光与多层星空微光（如今日运势流金、塔罗蓝紫、罗盘青碧、排盘苍璧），营造 Wear OS 深色屏幕的视觉层次。
  - `WearLoadingIndicator.kt` 采用 `androidx.graphics.shapes` 实现呼吸多边形形变加载动画。
- 六爻结果显示通过 `forResultDisplay()` 在 UI 边界按 `indexFromBottom` 倒序；`YaoLineDisplay` 将阴阳线形状与动爻标记分离。三线经卦必须使用 `trigramLineDisplayAt`，不能塞入要求六线的 `HexagramDisplayModel`。
- 罗盘读取加速度计/磁力计并可回退 rotation vector，做屏幕 remap、磁北方位规范化和跨 359°/0° 平滑；Compose 状态发布限制为最高约 15Hz。传统口径、24 山、后天八卦和元运数据以 `docs/compass-data-conventions.md` 为准。
- 归档体系覆盖六爻、梅花、小六壬和塔罗 4 类占卜源 (`ArchiveSource`)，统一保存为不可变结构化快照；详情通过 `ArchiveSnapshotCodec` 安全解析，避免用当前引擎重新计算历史结果。

# AI 协作与开发规范 (Rules & Conventions)

## 必须保持的合同

- 不改变 engine 的初爻到上爻数组语义、`YaoPosition`、`position.indexFromBottom`、6/7/8/9 含义或 `DivinationResult` 结构来解决显示问题。需要倒序时只在 UI、序列化或列表投影边界处理。
- 不复制六十四卦名称/卦象表或六爻计算逻辑；复用 `HexagramCatalog`、`HexagramReference`、现有 repository 和 engine。三卦/三线数据不要进入六线模型。
- 变卦必须由动爻阴阳翻转得到；变卦的八宫、纳甲和六亲按当前代码重新计算，不能沿用本卦宫。
- 历法输入是设备公历时间，但算法前必须走 Solar -> Lunar；绝不能把 Gregorian 月日直接传给 `Lunar.fromYmdHms`。不得用 try/catch 隐藏可复现的规则或数据错误。
- 梅花、小六壬、今日运势、罗盘、塔罗的口径是项目明确选择的可复现规则；命盘引擎（八字、西洋占星、生命灵数、称骨、九星）同样遵循纯函数可验证规范。修改规则时同步更新对应 `docs/*-rules.md` 或 `docs/compass-data-conventions.md` 与测试（注：新引入的命理子系统后续应逐步补齐对应的独立规则文档）。

## 代码与数据规范

- Kotlin 使用官方风格、4 空格缩进、`PascalCase` 类型、`camelCase` 函数/属性、枚举常量大写；优先使用不可变 `data class`、显式类型边界和小型纯函数。
- UI 文本与 Compose 结构改动要保留 40mm 圆屏可操作性、间距、动画、返回逻辑和滚动指示器。设置关闭 rotary 只代表不响应表冠，不得禁用触摸滚动/点击/导航。
- 引擎和 repository 保持 Android/UI 无关；资料解析必须验证 schema、来源/许可、非空字段、64 卦唯一性以及 `yao_text.json`（64×6/384 条结构）、`tarot_cards.json`（78 张卡牌结构）。
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

