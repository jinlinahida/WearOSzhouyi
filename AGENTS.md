# 项目概要 (Project Overview)

- Boompala 是一款面向 Wear OS 手表的高颜值、现代且以离线自治为核心的易学、命理与传统健康研习工具箱：
  - **易学排盘**：六爻排盘（手动四态/已知卦象/铜钱摇卦/报数起卦，含用神、伏神与格局分析）、梅花易数时间起卦、小六壬起课；
  - **传统脉诊**：腕上中医把脉（硬件纳秒逐搏采样、脱腕拦截、Task Force HRV 时域特征提取、十二经典脉象辨证分类与子午流注调摄指引）；
  - **韦特塔罗**：78 张经典牌图（大/小阿卡纳）全套离线释义，支持单牌、时间流三牌、圣三角与凯尔特十字 4 种经典牌阵；
  - **今日运势**：黄历建除十二神、黄黑道吉凶、六十四卦每日值日轮转，以及结合用户生辰八字的流日个人专属运势评估；
  - **命盘系统**：八字排盘与顺逆起大运、西洋占星本命星盘（十大行星/十二宫/相位）、毕达哥拉斯生命灵数（卓越数/态度数/流年/九宫盘）、袁天罡称骨算命、九星气学；
  - **罗盘与修持**：玄空罗盘（传感器融合、三元九运二十四山）、腕上极简电子木鱼（实木雕刻拟物光影与硬件级敲击触感）；
  - **辞海图鉴**：内置六十四卦、爻辞（384 爻）、彖象传、易学基础与 78 张韦特塔罗牌图鉴。
- **离线自治与网络/安全边界**：
  - **核心计算 100% 离线自治**：所有排盘引擎、命盘算法、黄历运势、传统辞海及脉象分类器均纯本地确定性运行，零外部强依赖，离线完全可用；
  - **网络仅用于两大可选增强能力**：
    1. **局域网免打字扫码配对**（`LocalKeySyncServer`）：手表端启动临时轻量 HTTP 服务，生成配对二维码，手机扫码即可免在表盘键盘打字快速同步配置 API Key/URL/模型，单次有效 Token，5 分钟超时或配对后彻底自毁，不常驻后台；
    2. **SI·思辨 / AI 六爻深度解卦**（`OpenAiCompatibleClient`）：直连用户自备的 OpenAI 兼容端点（DeepSeek、OpenAI、Moonshot 或自定义端点），基于本地引擎装配好的结构化卦象上下文进行 SSE 流式解卦；
  - **硬件级密钥安全**：用户配置的 API Key 统一经由 Android Keystore AES-256-GCM 硬件加密存储（`KeyStoreCrypto`），严禁明文落盘。
- 应用由单 Activity 驱动的 Compose 页面状态机（33 个 `AppScreen` 状态）组成，`app` 负责 Wear OS UI、传感器、硬件触觉与存储，`engine` 负责与 UI 无关的历法、卦象规则、黄历运势、命盘命理、脉诊分类、塔罗算法、AI 客户端及数据校验。

# 技术栈 (Tech Stack)

- **语言与构建**：Kotlin（官方代码风格，JVM target 17）；Gradle Wrapper，Android Gradle Plugin 8.7.3，Kotlin 2.0.21，Compose compiler plugin；compileSdk / targetSdk 35，minSdk 26（Android 8.0+ / Wear OS 2.0+）。
- **平台与 UI**：Android Wear OS，Jetpack Compose、Compose for Wear Material 3 / Foundation、`ComponentActivity`、`androidx.graphics:graphics-shapes:1.0.1`（呼吸多边形形变加载与木鱼动效）。
- **关键库**：`androidx.activity:activity-compose:1.10.0`、Compose UI/Animation 1.9.0、Wear Compose 1.6.2、Preferences DataStore 1.1.1、Gson 2.10.1、`cn.6tail:lunar:1.7.7`、JUnit 4.13.2。
- **传感器与硬件**：
  - 脉诊与生理：`Sensor.TYPE_HEART_BEAT`（纳秒硬件逐搏事件）、`Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT`（脱腕拦截）、`Sensor.TYPE_HEART_RATE`；
  - 罗盘与姿态：`Sensor.TYPE_ACCELEROMETER`、`Sensor.TYPE_MAGNETIC_FIELD`、`Sensor.TYPE_ROTATION_VECTOR`；
  - 触觉系统：`AppHaptics`（Samsung Galaxy Watch 原厂专有常量 101/102/50107 + Wear 4 标准常量 18/19/20 + `FLAG_IGNORE_VIEW_SETTING` + 短冲程 OneShot 兜底与系统 Vibrator 直驱）。
- **存储与安全**：
  - 本地数据库：Android `SQLiteOpenHelper` 数据库 `boompa_archives.db`（schema version 2，归档表及 `cast_at` 降序索引）；
  - 偏好配置：Preferences DataStore 文件 `app_settings`；
  - 硬件加密：Android Keystore (`AES/GCM/NoPadding` 256-bit，无缝向前迁移与 JVM 单元测试安全降级）。
- **网络与通信**：`LocalKeySyncServer`（基于标准 `ServerSocket` 的临时配对服务）、`OpenAiCompatibleClient`（流式 SSE 解析器）。
- **Android 入口与权限**：`app/src/main/AndroidManifest.xml` 声明 standalone、必需的 watch feature、`android:usesCleartextTraffic="true"`（局域网临时配对所需）与 4 项必要权限：
  - `android.permission.VIBRATE`（触觉马达直驱保障）；
  - `android.permission.BODY_SENSORS`（脉诊与心搏硬件采样）；
  - `android.permission.INTERNET` 与 `android.permission.ACCESS_NETWORK_STATE`（局域网配对与可选 SI 解卦接口直连）。

# 项目目录结构 (Directory Structure)

```text
.
├── app/
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Wear OS standalone 清单、权限与 MainActivity
│       │   ├── java/com/boompala/
│       │   │   ├── MainActivity.kt       # ComponentActivity，挂载 BoompalaApp 状态机
│       │   │   ├── ai/sync/              # 局域网临时扫码配对 (LocalKeySyncServer, QrCodeGenerator, Payload)
│       │   │   ├── archive/              # SQLite 归档模型、数据库、快照编解码 (含脉象与 AI 解卦存储)
│       │   │   ├── compass/              # 方位计算、传感器融合平滑、三元九运数据
│       │   │   ├── pulse/                # 脉诊硬件传感器数据源封装 (逐搏采样、脱腕状态机)
│       │   │   ├── settings/             # AppSettings、DataStore 与 KeyStoreCrypto 硬件安全加密
│       │   │   └── ui/                   # Wear OS Compose 页面体系
│       │   │       ├── ai/               # SI·思辨流式解卦卡片与增量加载 (AiDivinationCard)
│       │   │       ├── muyu/             # 极简电子木鱼立体雕刻界面与触感 (MuyuScreen)
│       │   │       ├── pulse/            # 动态脉搏波 Canvas 与十二脉象辨证结果 (PulseMeasureScreen, PulseResultScreen)
│       │   │       ├── BoompalaApp.kt    # 应用级状态机 (33 个 AppScreen 状态与滑动返回编排)
│       │   │       ├── BoompalaWheelPicker.kt   # 通用数字/爻位转轮选择交互组件
│       │   │       ├── ScalingRotaryScrollColumn.kt # ScalingLazyColumn 曲线缩放滚动容器
│       │   │       ├── RotaryScrollColumn.kt        # 标准 LazyColumn 表冠平滑滚动容器
│       │   │       ├── AppBackground.kt  # 自适应环境顶光与多层星空微光背景
│       │   │       ├── PageTransitions.kt# 页面转场曲线与 AppHaptics 原厂级硬件触觉引擎
│       │   │       └── ...               # 排盘、命盘、塔罗、运势、罗盘、浏览、设置等屏幕
│       │   └── res/                      # 应用名、图标、矢量图、78 张韦特塔罗 WebP 牌图与主题资源
│       ├── test/                         # app JVM 测试：归档、设置、加密、UI 投影、转场、触觉
│       └── androidTest/                  # Compose/Wear 交互测试
├── engine/
│   └── src/
│       ├── main/kotlin/com/boompala/engine/
│       │   ├── BasicHexagramEngine.kt    # 四态输入到本卦/变卦的基础推导
│       │   ├── LiuYaoEngine.kt            # 六爻完整装卦编排 (八宫/纳甲/六亲/六神/世应/旬空)
│       │   ├── ai/                        # OpenAI 兼容流式客户端、Prompt 结构化构建、URL 规范化
│       │   ├── astrology/                 # 西洋占星本命盘引擎 (十大行星经度/黄道十二宫/主要相位)
│       │   ├── bazi/                      # 八字命盘引擎 (四柱干支/藏干/十神/纳音/起大运)
│       │   ├── bone/                      # 袁天罡称骨算命引擎 (骨重换算与批注歌诀)
│       │   ├── calendar/                  # GanzhiCalendar seam、6tail 适配器与黄历建除数据源
│       │   ├── dailyfortune/              # 今日运势引擎：建除十二神、黄道吉凶、值日卦与流日个人运势
│       │   ├── data/                      # 卦辞/爻辞/知识/塔罗 repository 与离线解析校验
│       │   ├── liuyao/                    # 六爻金钱掷卦模拟与报数起卦引擎 (NumberCastingEngine)
│       │   ├── meihua/                    # 梅花时间起卦、互卦、体用
│       │   ├── model/                     # 输入、时间、卦、爻、干支、结果领域模型
│       │   ├── ninestar/                  # 九星气学引擎 (本命星/月命星/倾斜星/主导五行)
│       │   ├── numerology/                # 毕达哥拉斯生命灵数引擎 (卓越数/态度数/流年数/九宫数盘)
│       │   ├── pulse/                     # 脉诊核心算法：Butterworth滤波/Elgendi峰值/HRV特征/TcmPulseClassifier
│       │   ├── rules/                     # 纳甲、六亲、六神、八宫、旬空、伏神 (FuShen)、用神 (YongShen)、格局 (Status)
│       │   ├── tarot/                     # 塔罗牌引擎、牌组、洗牌与 4 款牌阵 (单牌/时间流/圣三角/凯尔特十字)
│       │   └── xiaoliuren/                # 小六壬六宫循环规则与模型
│       ├── main/assets/                   # APK 内离线 JSON (爻辞/解释/知识/塔罗) 和 NOTICE 许可文件
│       └── test/kotlin/                   # engine 各模块规则、脉诊、AI、历法、数据、命理单元测试
├── docs/                                  # 架构、规则口径 (六爻/梅花/小六壬/运势/罗盘/脉诊)、数据来源和许可研究
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
[易学与命理主干 (100% 离线确定性)]
设备公历 Instant + ZoneId + 用户生辰配置 (LocalDate + Hour + Gender)
        │
        ├── SixTailGanzhiCalendar (Solar -> Lunar -> EightChar)
        │       │
        │       ├── LiuYaoEngine: HexagramInput (手动/已知/金钱/报数) -> BasicHexagramEngine
        │       │                 -> 八宫/纳甲/六亲/六神/世应/旬空/伏神/用神/格局 -> DivinationResult
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
        ├── TarotEngine: 78 张牌库 (大/小阿卡纳) + 随机正逆位洗牌 -> 单牌/时间流/圣三角/凯尔特十字 -> TarotReading
        └── MuyuEngine: 实木光影拟物微交互 + 硬件级敲击触感 -> 功德计数持久化

[中医切脉数据流 (硬件纳秒逐搏 & 离线辨证)]
硬件逐搏事件 (TYPE_HEART_BEAT, event.timestamp 纳秒) + 脱腕传感器 (OFFBODY_DETECT)
        │
        ├── 脱腕拦截 (0.0f 阻断并在 15s 超时自动退出)
        ├── 纳秒时间戳差值 -> 生理极限过滤 (300ms ~ 1800ms IBI 序列)
        ├── 20 秒采样窗口 SQI 质检 (有效心搏 >= 10, 覆盖率 >= 60%, 置信度 >= 1.0)
        ├── Task Force HRV 特征提取 (RMSSD 迷走神经活性, pNN50, 节律规整度)
        └── TcmPulseClassifier: 中医十二经典脉象分类 -> 把脉结果 + 8层辨证调摄 + 子午流注

[SI·思辨 / AI 深度解卦 (可选联网增强)]
DivinationResult (已装卦完整领域模型)
        │
        ├── AiPromptBuilder: 装配卦名/八宫/六亲/世应/动爻生克/用神/伏神结构化上下文
        ├── OpenAiCompatibleClient: 直连自备 API (DeepSeek/OpenAI/Moonshot/自定义)
        ├── SSE 增量字符推流 -> AiStreamBuffer -> AiDivinationCard 渐进渲染
        └── 结果持久化: 结构化快照打包为 AiArchiveData 存入 SQLite 归档

[局域网免打字扫码配对]
用户进入扫码配置 -> LocalKeySyncServer 启动 (分配随机单次 UUID Token 与 5 分钟自毁定时器)
        └── 手机局域网扫码填写 -> HTTP POST 传输 -> KeyStoreCrypto (AES-256-GCM) 硬件加密保存 -> Server 彻底销毁
```

## 主要模块与设计

- **易学排盘核心合同**：
  - `engine.model` 是稳定领域合同。六条输入、`HexagramPattern.linesFromBottom`、`Hexagram.yaoFromBottom` 始终是“初爻到上爻”，`YaoPosition.indexFromBottom` 是算法索引；六爻模式要求恰好六条线。
  - `BasicHexagramEngine` 只处理 6/7/8/9 的阴阳与动静；`LiuYaoEngine` 在此之上调用 `HexagramRules`、`NajiaRules`、`SixRelationRules`、`SixSpiritRules`、`VoidRules`、`FuShenRules`、`LiuYaoStatusRules`、`YongShenEvaluator`，并为本卦/变卦分别重新装卦。
  - 起卦多样性统一收敛：`DirectHexagramInput`（已知卦象直排）、`LiuYaoCoinCastingEngine`（铜钱掷卦模拟）、`NumberCastingEngine`（两数/三数报数起卦）均统一转换并复用 `HexagramInput` 装卦推导路径。
  - 变卦必须由动爻阴阳翻转得到；变卦的八宫、纳甲、六亲按当前代码重新计算，不得沿用本卦宫。
- **历法与时间口径**：
  - `SixTailGanzhiCalendar` 必须先用 `Solar.fromYmdHms(...).getLunar()`，再交给 `EightChar.fromLunar`；`DivinationTimeInfo` 是六爻、梅花、小六壬和运势共用的时间模型。晚子时“算明天”由 calendar 策略控制。
- **中医切脉与脉诊体系 (Pulse)**：
  - 拒绝私有闭源 SDK 与虚假合成数据；完全基于 Wear OS 标准传感器。
  - 使用 `SensorEvent.timestamp`（硬件纳秒时钟，杜绝 GC 抖动）记录真实心搏时刻；
  - 严格执行 20 秒采样窗口质量控制（有效心搏 $\ge 10$、覆盖率 $\ge 60\%$、置信度 $\ge 1.0$），未达标或脱腕（15 秒超时）直接拦截打回重测；
  - 详尽规则参见 `docs/pulse-diagnosis-rules.md`。
- **SI·思辨 / AI 深度解卦与安全**：
  - `AiPromptBuilder` 严格基于本地引擎装配好的结构化领域上下文构建提示词，禁止把未装卦的生硬数值直接丢给模型；
  - `KeyStoreCrypto` 采用 Android Keystore `AES-256-GCM` 硬件加密存储用户 API Key，提供向前兼容迁移与 JVM 单元测试安全降级；
  - `LocalKeySyncServer` 遵循“按需开启、单次 Token、超时自毁、严禁常驻”的极简安全原则。
- **状态机与导航体系**：
  - `BoompalaApp.kt` 是应用级状态机：管理 33 个 `AppScreen` 状态，协调排盘结果、脉诊数据流、命盘展示、木鱼计数、归档快照与详情返回目标；支持 `SwipeToDismissBox` 滑动返回与自定义页面转场动效。
- **设置与个性化配置**：
  - `AppSettings` 与 DataStore：支持圆屏/方屏/自动模式、字号缩放（0.9x/1.0x/1.1x）、动画开关、表冠旋转开关、触觉震动开关与强度调节（弱/标准/强劲）、应用语言（中/英）、首页 14 大功能模块长按拖拽自定义排序与显隐管理、用户生辰档案、木鱼功德累计，以及可选的 SI 联网模式与供应商配置。
- **Wear OS 专属视觉与交互规范**：
  - **滚动组件分工**：
    - `RotaryScrollColumn`：基于标准 `LazyColumn`，配合 `ScreenScaffold` 与表冠事件，用于长文本、设置页、辞海浏览与归档列表；
    - `ScalingRotaryScrollColumn`：基于 Wear Compose `ScalingLazyColumn`，用于卡片流等需要上下视差缩放聚焦的场景；
    - `BoompalaWheelPicker`：用于报数起卦和日期滚轮选择；
    - 关闭 rotary 只代表不响应表冠，不得禁用触摸滑动/点击/导航。
  - **原厂级硬件触觉 (`AppHaptics`)**：
    - 针对 Samsung Galaxy Watch 派发三星专有常量（`101` ROTARY_SCROLL、`102` ROTARY_FOCUS、`50107` ROTARY_LIMIT），直驱 X 轴 LRA 纯净波形；针对 Wear 4 派发系统标准常量（`18`、`19`、`20`）；
    - 优先走 DecorView 携带 `FLAG_IGNORE_VIEW_SETTING` 唤醒驱动，无 View 时降级为短冲程 OneShot 脉冲与系统 Vibrator 直驱；
    - 细分专属场景：`click`、`toggle`、`cardFlip`、`coinToss`、`muyuTap`、`pulseBeat`、手势返回阈值微触、物理键微触；
    - **铁律**：所有触觉必须受 `hapticFeedbackEnabled` 与 `hapticIntensity` 统一门控。
  - **极简文案与 40mm 圆屏防溢出**：
    - 腕上屏幕空间极其宝贵，操作按钮文案必须极简精炼（优先使用“进入”、“保存”、“删除”、“重置”等两字动词），严禁冗长句式导致截断折行。
  - **极简线条图标规范**：
    - 统一遵循 **Morphicons / Lucide** 现代极简 Stroke 线条规范（标准 24×24 视口、`strokeWidth="1.8"`、`strokeLineCap="round"`、`strokeLineJoin="round"`、`fillColor="#00000000"`），所有功能与设置图标均统一收录维护于 `app/src/main/res/drawable/`。
- **归档体系**：
  - 覆盖六爻、梅花、小六壬、塔罗、脉象推演 5 类占卜源 (`ArchiveSource`)，并支持 `AiArchiveData` 结构化存储；统一保存为不可变结构化快照，详情通过 `ArchiveSnapshotCodec` 解析，避免用当前引擎重新推算历史结果。

# AI 协作与开发规范 (Rules & Conventions)

## 必须保持的合同

- **排盘与领域数据合同**：
  - 不改变 engine 的初爻到上爻数组语义、`YaoPosition`、`position.indexFromBottom`、6/7/8/9 含义或 `DivinationResult` 结构来解决显示问题。需要倒序时只在 UI、序列化或列表投影边界处理。
  - 不复制六十四卦名称/卦象表或六爻计算逻辑；复用 `HexagramCatalog`、`HexagramReference`、现有 repository 和 engine。三卦/三线数据不要进入六线模型。
  - 历法输入是设备公历时间，但算法前必须走 Solar -> Lunar；绝不能把 Gregorian 月日直接传给 `Lunar.fromYmdHms`。不得用 try/catch 隐藏可复现的规则或数据错误。
  - 六爻、梅花、小六壬、运势、罗盘、塔罗、五大命盘及中医脉诊口径以项目规则文档为准。修改规则时同步更新对应 `docs/*-rules.md` 或 `docs/compass-data-conventions.md` 与单元测试。
- **AI / SI 边界合同**：
  - AI 永远只是离线引擎的“阐释层”与“叙事层”，严禁越俎代庖替代底层引擎装卦或判定；
  - `AiPromptBuilder` 必须消费本地引擎装配好的结构化结果，禁止将未装卦的生硬数据丢给模型；
  - 网络请求必须使用异步流式协程与 SSE 解析，UI 必须支持优雅降级与网络异常恢复，断网绝不影响基础排盘结果。
- **脉诊传感器与时钟合同**：
  - 严禁使用合成数据假装把脉；计算心搏间期必须使用硬件纳秒 `event.timestamp`，严禁使用 `System.currentTimeMillis()` 避免 GC 导致脉象失真；
  - 严禁绕过 SQI 质检门禁（采样时间不足、心搏过少或脱腕时必须明确提示重测，不得强行输出假结果）。
- **密钥安全与端口生命周期**：
  - 禁止在任何日志、常规 DataStore 字段或 SQLite 明文存储用户 API Key，必须通过 `KeyStoreCrypto` 硬件加密；
  - 局域网配对 HTTP 服务遵循“按需开启、单次 Token、即用即关、5 分钟超时自毁”准则，严禁常驻后台暴露监听端口。

## 代码与数据规范

- Kotlin 使用官方风格、4 空格缩进、`PascalCase` 类型、`camelCase` 函数/属性、枚举常量大写；优先使用不可变 `data class`、显式类型边界和小型纯函数。
- UI 文本与 Compose 结构改动要保留 40mm 圆屏可操作性、间距、动画、返回逻辑和滚动指示器。
- 引擎和 repository 保持 Android/UI 无关；资料解析必须验证 schema、来源/许可、非空字段、64 卦唯一性以及 `yao_text.json`（64×6/384 条结构）、`tarot_cards.json`（78 张卡牌结构）。
- 新增或修改离线资产时同步更新对应 `NOTICE-*.txt`、`docs/research/` 和解析测试。运行时资源必须放在 `engine/src/main/assets/`，不要改成网络加载。
- 归档保存完整快照；详情按 `archiveDetailId -> ArchiveRepository.get -> ArchiveSnapshotCodec.decode` 加载。
- 传感器在页面可见/生命周期恢复时注册，不可见/暂停时注销；使用 `CompassMath` 的规范化和最短圆周平滑，不在 UI 中重写方位公式。
- 不提交 `.gradle/`、`.kotlin/`、`build/`、`local.properties`、APK 或 IDE 文件。依赖版本以两个 module 的 `build.gradle.kts` 为准。

## 验证与已知陷阱

- **常规 JVM 测试**：`./gradlew --no-daemon :engine:test :app:testDebugUnitTest`。
- **Debug 构建**：`./gradlew --no-daemon :app:assembleDebug`；输出为 `app/build/outputs/apk/debug/app-debug.apk`。
- **Release 本地构建**：`./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease -x :app:lintVitalAnalyzeRelease`；输出为 `app/build/outputs/apk/release/app-release.apk`。Release 使用 debug signing，适合本地安装测试。
- 每次提交前运行 `git diff --check`，并检查 `git status` 确认只包含目标文件。
- **当前工具链已知限制**：标准 `lintVitalAnalyzeRelease` 可能因 Kotlin Analysis API / `NonNullableMutableLiveDataDetector` 的 `IncompatibleClassChangeError` 失败；排除 lint 生成的 APK 只能说明源码编译/打包完成，不能宣称 Release lint 通过。
- 编译、JVM 测试和 Compose 注入测试不等同于真实手表验证。若没有连接 Wear OS 手表/模拟器，不要宣称表冠滚动、震动、右侧指示器、传感器方向、圆屏布局或帧率已验证。
- 怀疑崩溃时先定位真实调用链并复现；优先修正数据边界/模型投影，禁止通过扩大 `try/catch`、放宽 `require` 或吞异常掩盖问题。
- 变更应保持局部，不要无授权重构模块、替换架构、改变数据库历史数据或删除与请求无关的资料/许可/状态文本。
