# Boompala 易学与命理工具箱 (Wear OS)

[![Platform: Wear OS](https://img.shields.io/badge/Platform-Wear%20OS-blue.svg)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Compose Wear](https://img.shields.io/badge/Compose%20for%20Wear-Material%203-green.svg)](https://developer.android.com/training/wearables/compose)
[![Offline Core](https://img.shields.io/badge/Core%20Engines-100%25%20Offline-brightgreen.svg)](#离线自治与网络边界)
[![Tests](https://img.shields.io/badge/JVM%20Tests-224%20passing-informational.svg)](#构建与验证)

**Boompala** 是一款面向 Wear OS 手表的易学、命理与传统健康研习工具箱。所有排盘、命盘、黄历运势、脉象辨证与辞海图鉴均由**本地确定性算法**推算，离线完全可用；网络仅用于两项**可选**增强能力（局域网扫码配对与 SI 联网解卦）。

项目基于 Kotlin 与 Jetpack Compose for Wear OS 构建，在 40mm 圆形腕上屏幕上追求原厂级的表冠滚动、物理触感与动效表现。

---

## 核心功能

### 传统易学与占卜

- **六爻排盘**：四种起卦路径统一收敛到同一装卦合同 —— 手动排爻（老阴/少阳/少阴/老阳四态）、铜钱摇卦模拟、已知本卦/变卦直排、数字报数起卦。完整装卦体系涵盖八宫归属、纳甲干支、六亲生克、六神、世应爻位、旬空判定、伏神与用神推导；动爻翻转后的**变卦独立重新装卦**，不沿用本卦宫。
- **梅花易数**：按农历年支、月、日、时支数推演本卦、互卦、变卦，判定体用与五行生克。
- **小六壬**：大安、留连、速喜、赤口、小吉、空亡六宫循环起课。

### 韦特塔罗

- **78 张全套离线图鉴**：22 张大阿卡纳 + 56 张小阿卡纳经典莱德·韦特牌图（WebP）与正逆位详尽释义，支持仅用大阿卡纳、关闭逆位等偏好。
- **4 种牌阵已接入独立界面**：单牌、时间流三牌（过去/现在/未来）、圣三角（现状/阻碍/对策）、凯尔特十字（10 宫位）。引擎层另有四元素阵与六爻卦阵已完成实现并通过单元测试，界面入口待接入。
- **真随机洗牌与正逆位判定**，结果可作为结构化快照归档。

### 今日黄历与个人运势

- **传统黄历**：建除十二神与十二神黄黑道吉凶。
- **六十四卦轮转值日**：依据二十四节气与农历流日映射当日值日卦象。
- **流日个人专属运势**：结合生辰档案推算日主十神、神煞合冲、喜用五行与幸运色数。输出为民俗结构的确定性推演，界面明确标注不构成现实吉凶断言。

### 五大确定性离线命盘

纯本地推算，不依赖任何外部服务：

- **八字命盘**：四柱干支、地支藏干、十神、纳音五行与顺逆起大运。
- **西洋占星本命盘**：十大行星黄道经度、上升点、黄道十二宫与主要相位（合/六合/刑/拱/冲）。
- **毕达哥拉斯生命灵数**：命数、卓越数（11/22/33）、生日数、态度数、个人流年数与 3×3 九宫数盘。
- **袁天罡称骨算命**：年月日时骨重累加与批注歌诀。
- **九星气学**：本命星、月命星、倾斜星与主导五行能量。

### 中医脉诊参考（离线辨证）

基于 Wear OS 标准传感器，拒绝私有闭源 SDK 与任何合成数据：

- **数据来源**：`TYPE_HEART_BEAT` 逐搏事件用 `event.timestamp`（硬件纳秒时钟，规避 GC 与线程排队抖动）记录真实心搏时刻；`TYPE_LOW_LATENCY_OFFBODY_DETECT` 拦截脱腕；`TYPE_HEART_RATE` 提供瞬时脉率与接触置信度。
- **信号处理**：IBI 生理极限过滤（300ms ~ 1800ms）、Butterworth 带通、Elgendi 峰值检测、Task Force HRV 时域特征（RMSSD、pNN50、节律规整度）。
- **硬性质检门禁**：20 秒采样窗口内需同时满足 **有效心搏 ≥ 10、覆盖率 ≥ 60%、平均置信度 ≥ 1.0**；未达标或脱腕（15 秒超时）直接拦截并提示重测，**绝不强行输出结果**。
- **辨证输出**：十二经典脉象分类 + 子午流注时辰调摄指引。

> 该模块是生理特征的启发式归纳，属于传统文化研习语境，**不是医学诊断，不构成医疗建议**。相关口径与阈值来源详见 `docs/pulse-diagnosis-rules.md`。

### 玄空罗盘

加速度计与磁力计融合（支持旋转矢量回退），做屏幕 remap 与最短圆周平滑；显示磁北方位角、后天八卦方位、二十四山与三元九运旺衰。支持真北开关与磁偏角校正。Compose 状态发布节流在 15Hz 以控制功耗。

### 腕上木鱼

实木雕刻拟物光影与硬件级敲击触感，功德计数持久化。

### 典籍辞海与知识库

内置 64 卦全本卦辞、爻辞（384 爻）、彖传、象传与白话现代释义，以及 21 篇易学基础知识和 78 张塔罗牌图鉴，全部随 APK 离线打包。

### 结构化本地归档

SQLite（`boompa_archives.db`）保存**不可变结构化快照**，覆盖六爻、梅花、小六壬、塔罗、脉象推演 5 类来源，并可存入 SI 解卦结果。详情通过 `ArchiveSnapshotCodec` 解码历史快照，不用当前引擎重算旧卦。

---

## 离线自治与网络边界

**核心计算 100% 离线**：所有排盘引擎、命盘算法、黄历运势、脉象分类器与辞海数据都是纯本地确定性运算，无任何强制外部依赖。

网络权限只服务于两项可选增强，且都不常驻：

1. **局域网免打字扫码配对**（`LocalKeySyncServer`）：手表按需启动临时轻量 HTTP 服务，生成配对二维码，手机在同一局域网扫码即可免在表盘键盘上逐字输入 API Key / URL / 模型。采用**单次有效 Token + 5 分钟超时自毁 + 配对后彻底销毁**，不做后台常驻监听。
2. **SI 联网解卦**（`OpenAiCompatibleClient`）：直连用户自备的 OpenAI 兼容端点（内置 DeepSeek / OpenAI / Moonshot 预设，或完全自定义 Base URL 与模型），基于**本地引擎已装卦的结构化上下文**做 SSE 流式解卦。断网或调用失败时优雅降级，排盘结果本身不受影响。

**密钥安全**：API Key 一律经 Android Keystore `AES/GCM/NoPadding` 256-bit 硬件加密存储（`KeyStoreCrypto`），禁止明文落盘、禁止写入日志与普通偏好字段；界面仅展示掩码。

权限清单（`app/src/main/AndroidManifest.xml`）：

| 权限 | 用途 |
| --- | --- |
| `VIBRATE` | 触觉马达直驱保障 |
| `BODY_SENSORS` | 脉诊与心搏硬件采样 |
| `INTERNET` | 局域网配对服务与可选 SI 端点直连 |
| `ACCESS_NETWORK_STATE` | 联网能力前置检查 |

`android:usesCleartextTraffic="true"` 是局域网配对（设备本地 HTTP）所需，且声明范围附带用途注释说明。

---

## Wear OS 专属设计与交互

- **表冠滚动**：`RotaryScrollColumn`（标准 `LazyColumn`，用于长文本、设置、辞海浏览、归档列表）与 `ScalingRotaryScrollColumn`（`ScalingLazyColumn`，用于卡片流缩放聚焦）分工明确；`BoompalaWheelPicker` 负责报数与日期滚轮。关闭表冠只影响旋转输入，不禁用触摸、点击与导航。
- **原厂级触觉**（`AppHaptics`）：针对 Samsung Galaxy Watch 派发三星专有常量（`101` ROTARY_SCROLL / `102` ROTARY_FOCUS / `50107` ROTARY_LIMIT）直驱 X 轴 LRA；针对 Wear 4 派发系统标准常量；无 View 时降级为短冲程 OneShot 与系统 Vibrator 直驱。细分 `click` / `toggle` / `cardFlip` / `coinToss` / `muyuTap` / `pulseBeat` / 手势返回阈值微触等场景。**所有触感统一受 `hapticFeedbackEnabled` 与 `hapticIntensity` 门控。**
- **视觉体系**：自适应环境顶光与多层星空微光背景（运势流金、塔罗蓝紫、罗盘青碧、排盘苍璧）、基于 `androidx.graphics:graphics-shapes` 的呼吸多边形形变加载、自定义页面转场曲线。
- **极简文案与 40mm 防溢出**：操作按钮优先使用「进入」「保存」「删除」「重置」等两字动词，避免折行截断。
- **极简线条图标**：统一遵循 Lucide / Morphicons Stroke 规范（24×24 视口、圆角端点与连接、`strokeWidth` 以 `1.8` 为主，少数图标按视觉重量微调），收录于 `app/src/main/res/drawable/`。
- **导航**：单 Activity + `AppScreen` 枚举状态机（34 个页面状态），全局 `SwipeToDismissBox` 手势返回并配有对应的返回目标映射。
- **个性化设置**：首页 14 个功能模块长按拖拽排序与显隐管理、生辰档案、圆屏/方屏/自动模式、3 档字号（0.9x/1.0x/1.1x）、动画开关、表冠开关、触觉开关与三档强度、常亮屏幕、罗盘真北与磁偏角、塔罗逆位与仅大阿卡纳、中/英语言切换。

---

## 项目架构

```text
.
├── app/                              # Wear OS 表现层与硬件交互
│   └── src/
│       ├── main/java/com/boompala/
│       │   ├── MainActivity.kt       # 单 Activity 入口，挂载 BoompalaApp 状态机
│       │   ├── ai/sync/              # 局域网临时扫码配对（LocalKeySyncServer / QrCodeGenerator / Payload）
│       │   ├── archive/              # SQLite 归档、模型与快照编解码
│       │   ├── compass/              # 方位计算、传感器融合平滑、三元九运数据
│       │   ├── pulse/                # 脉诊硬件传感器数据源（逐搏采样、脱腕状态机）
│       │   ├── settings/             # AppSettings、DataStore 与 KeyStoreCrypto 硬件加密
│       │   └── ui/                   # Compose 页面体系
│       │       ├── ai/               # SI 流式解卦卡片与增量渲染
│       │       ├── muyu/             # 电子木鱼拟物界面
│       │       ├── pulse/            # 脉搏波 Canvas 与脉象结果
│       │       └── ...               # BoompalaApp 状态机、滚动容器、背景、转场与触感
│       ├── test/                     # app JVM 测试：归档、设置、加密、配对服务、投影与触感
│       └── androidTest/              # Compose/Wear 交互测试
├── engine/                           # 领域算法层（不引用 Android UI 框架）
│   └── src/
│       ├── main/kotlin/com/boompala/engine/
│       │   ├── BasicHexagramEngine.kt   # 四态输入 → 本卦/变卦推导
│       │   ├── LiuYaoEngine.kt          # 六爻完整装卦编排
│       │   ├── ai/                      # OpenAI 兼容流式客户端、Prompt 结构化构建、URL 规范化
│       │   ├── astrology/               # 西洋占星本命盘
│       │   ├── bazi/                    # 八字四柱/藏干/十神/纳音/大运
│       │   ├── bone/                    # 称骨算命
│       │   ├── calendar/                # GanzhiCalendar seam 与 6tail 适配器、黄历建除数据源
│       │   ├── dailyfortune/            # 建除十二神、黄道吉凶、值日卦与个人流日运势
│       │   ├── data/                    # 离线 JSON repository 与解析校验
│       │   ├── liuyao/                  # 铜钱掷卦模拟与报数起卦
│       │   ├── meihua/                  # 梅花时间起卦、互卦、体用
│       │   ├── model/                   # 输入、时间、卦、爻、干支领域模型
│       │   ├── ninestar/                # 九星气学
│       │   ├── numerology/              # 生命灵数
│       │   ├── pulse/                   # Butterworth / Elgendi / HRV 特征 / 十二脉象分类
│       │   ├── rules/                   # 纳甲、六亲、六神、八宫、旬空、伏神、用神、格局
│       │   ├── tarot/                   # 牌库、洗牌、牌阵
│       │   └── xiaoliuren/              # 小六壬六宫
│       ├── main/assets/               # APK 内置离线 JSON（爻辞/释义/知识/塔罗）与 NOTICE
│       └── test/kotlin/               # 各模块规则、脉诊、AI、历法、数据、命理单元测试
├── docs/                             # 架构、各领域规则口径、数据来源与许可研究
├── tools/import_wikisource_yao_text.mjs   # Wikisource 爻辞导入与清洗
└── gradle/                           # Gradle Wrapper
```

`engine` 采用 `com.android.library` 插件，仅为打包 `assets/`；其源码不引用任何 `android` / `androidx` 框架类型，因此可用纯 JVM 单元测试直接驱动（测试通过系统属性 `yaoTextAssetPath` 等读取资产文件做校验）。`feature/liuyao/` 不是 Gradle 工程已启用模块（`settings.gradle.kts` 未 include），其中内容不可作为源码入口。

---

## 环境要求

- Android Studio（Ladybug 或更新）与现代 Android 构建环境
- JDK 17（JVM target 17）
- Android SDK：compileSdk / targetSdk 35，minSdk 26
- Wear OS 实体手表或 Android Studio Wear 模拟器

`local.properties` 仅保存本机 `sdk.dir`，已被 `.gitignore` 忽略，不要复制为团队配置。

---

## 构建与验证

### 1. 运行 JVM 单元测试

```bash
./gradlew --no-daemon :engine:test :app:testDebugUnitTest
```

当前共 224 个测试（engine 130 / app 94），全部通过。

### 2. 生成 Debug APK

```bash
./gradlew --no-daemon :app:assembleDebug
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

### 3. 生成 Release APK（本地安装包）

```bash
./gradlew --no-daemon :app:testDebugUnitTest :app:assembleRelease -x :app:lintVitalAnalyzeRelease
```

输出：`app/build/outputs/apk/release/app-release.apk`

> **诚实说明**：Release 目前使用 debug 签名，仅适合本地侧载测试。排除 `lintVitalAnalyzeRelease` 是因为当前 Kotlin Analysis API 上游工具链在 `NonNullableMutableLiveDataDetector` 上会抛 `IncompatibleClassChangeError`；**排除 lint 生成的 APK 只能证明源码编译与打包完成，不代表 Release lint 已通过**。
>
> 编译、JVM 测试与 Compose 注入测试**不等同于真机验证**。表冠滚动、震动手感、右侧弧形指示条、传感器朝向、40mm 圆屏布局与帧率需要在实体 Wear OS 手表或模拟器上另行确认。

---

## 数据与许可

所有离线资料位于 `engine/src/main/assets/`，均带 `schemaVersion` 与来源标注：

- `yao_text.json` — 64 卦 / 384 爻爻辞、卦辞、彖传、象传，来自公共领域古籍；每条记录携带 `sourceRevision` 与 Wikisource 永久版本 URL，可逐条回溯校对。导入与清洗流程见 `tools/import_wikisource_yao_text.mjs`。
- `hexagram_interpretations.json` — 八卦与 64 卦现代白话释义。
- `knowledge.json` — 21 篇易学基础资料条目。
- `tarot_cards.json` 与 WebP 牌图 — 1909 年莱德·韦特·史密斯塔罗（Pamela Colman Smith 绘，公共领域）。
- 许可细节见资产同级 `NOTICE-*.txt`、`docs/licenses/`（含 `6tail-lunar-MIT.txt`）与 `docs/research/` 四篇来源研究文档。
- 历法依赖 `cn.6tail:lunar`（MIT）。历法输入为设备公历时间，算法内部严格走 `Solar → Lunar → EightChar` 路径。

---

## 已知限制与后续方向

- 表现层自动化测试偏少：`androidTest` 目前仅覆盖少量 Compose 交互，34 屏状态机与圆屏布局主要依赖人工验证。
- 部分界面文案仍为中文字面量硬编码，未完全走 `strings.xml`，语言切换在这些位置覆盖不完整。
- 引擎层四元素阵与六爻卦阵已实现并测试，尚缺 UI 入口。
- 脉诊阈值规则为经验启发式，缺乏外部标定数据集验证。
