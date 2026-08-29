# Boompala 易学与命理 (Wear OS)

[![Platform: Wear OS](https://img.shields.io/badge/Platform-Wear%20OS%204%2B%20%2F%20Android%2013%2B-blue.svg)](https://developer.android.com/wear)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg)](https://kotlinlang.org/)
[![Compose Wear](https://img.shields.io/badge/Compose%20for%20Wear-Material%203-green.svg)](https://developer.android.com/training/wearables/compose)
[![License: Offline & Standalone](https://img.shields.io/badge/Offline-100%25%20Standalone-brightgreen.svg)](#数据与许可)

**Boompala** 是一款专为 Wear OS 手表打造的高颜值、现代且完全离线的周易与命理占卜工具箱。

项目基于 Kotlin 与 Jetpack Compose for Wear OS 构建，致力于在小尺寸腕上屏幕提供极致流畅、沉浸式的易学与命理推算体验。**全功能 100% 离线运行，零网络权限、零外部 API、零数据上传**，所有排盘算法与离线辞海均内置于本地。

---

## 🌟 核心功能

### ☯️ 传统易学与占卜
- **六爻排盘**：
  - **多样起卦**：支持三枚铜钱摇卦模拟（全物理感振动掷卦）、手动四态选择（老阴/少阳/少阴/老阳）及已知卦象直排。
  - **完整装卦体系**：自动推导八宫归属、纳甲干支、六亲生克、六神（青龙/朱雀/勾陈/螣蛇/白虎/玄武）、世应爻位、旬空判定，支持动爻翻转并为变卦重新独立装卦。
- **梅花易数**：基于农历年支、月、日、时支数精准推演，生成本卦、互卦、变卦，判定体卦、用卦与五行生克吉凶。
- **小六壬**：传统六宫（大安、留连、速喜、赤口、小吉、空亡）循环推演，随手掐指起课。

### 🃏 韦特塔罗系统 (Tarot)
- **78 张全套离线图鉴**：内置 22 张大阿卡纳与 56 张小阿卡纳高清 WebP 牌图与详尽正逆位释义。
- **三种经典牌阵**：单牌占卜、时间流三牌（过去 / 现在 / 未来）、圣三角牌阵（现状 / 阻碍 / 对策）。
- **洗牌与抽取**：真随机洗牌算法，支持正逆位智能翻转判定与占卜快照归档。

### 📅 今日黄历与个人运势 (Daily Fortune)
- **传统黄历**：建除十二神（建、除、满、平、定、执、破、危、成、收、开、闭）与十二神黄黑道吉凶。
- **六十四卦轮转值日**：依据二十四节气与农历流日自动映射当日值日卦象与微言指引。
- **流日个人专属运势**：结合设置中配置的用户生辰与性别，推算当日日主十神、神煞合冲、吉凶事件评估及喜用五行、幸运色彩与数字。

### 🔮 五大确定性离线命盘 (Destiny Charts)
纯 Kotlin 编写的确定性命理算法引擎，本地推算、秒级呈现：
- **八字命盘 (Bazi)**：推导四柱八字干支、地支藏干、十神排盘、纳音五行与顺逆起大运（交运岁数与大运干支）。
- **西洋占星本命盘 (Astrology)**：计算太阳至冥王星十大行星黄道经度、上升点 (Ascendant)、黄道十二宫分布及主要相位（合/六合/刑/拱/冲）。
- **毕达哥拉斯生命灵数 (Numerology)**：推算命数、卓越数 (11/22/33)、生日数、态度数、个人流年数及 3×3 九宫数盘能量分布。
- **袁天罡称骨算命 (Bone Weight)**：累加年月日时骨重，输出总两钱与命理批注歌诀。
- **九星气学 (Nine Star Ki)**：计算本命星、月命星、倾斜星（最大潜能）及主导五行能量分析。

### 🧭 玄空罗盘 (Compass)
- **多传感器融合**：融合加速度计与磁力计（支持旋转矢量回退），做屏幕 remap 与最短圆周平滑。
- **三元九运与二十四山**：显示当前磁北方位角、后天八卦方位、二十四山及当下九运（离火运）旺衰指引。
- **高帧率节流**：Compose 状态发布限制在 15Hz，确保表盘流畅的同时避免过度消耗手表电量。

### 📖 典籍辞海与知识库 (Encyclopedia)
- 内置 64 卦全本卦辞、爻辞（384 爻）、彖传、象传，提供离线白话现代释义。
- 内置易学基础知识、天干地支、八卦类象等资料库。

### 📜 结构化本地归档 (Archives)
- **SQLite 不可变快照**：覆盖六爻、梅花、小六壬、塔罗 4 类占卜源，保存完整快照 JSON，查看详情不依赖引擎重算。
- 支持自定义标题、备注、分类筛选、排序与一键管理。

---

## ⌚ Wear OS 专属设计与交互

- **物理表冠旋转 (Rotary Scroll)**：全应用深度适配表冠平滑滚动，并联动细腻触觉反馈（Haptic）与屏幕右侧弧形位置指示条。
- **触觉马达直驱**：支持关闭 / 弱 / 标准 / 强劲三档震动调节，直驱马达保障各品牌手表 ROM（如小米、三星、TicWatch 等）上的触感一致性。
- **环境光效与星空微光**：自适应主题顶光与多层星空微光（运势流金、塔罗蓝紫、罗盘青碧、排盘苍璧），层次分明。
- **呼吸多边形加载**：基于 `androidx.graphics.shapes` 打造的几何形变呼吸动效。
- **高度个性化**：
  - 首页 11 大功能模块长按拖拽自定义排序与显隐管理；
  - 个人生辰八字档案配置（联动命盘与流日运势）；
  - 圆屏 / 方屏 / 自动屏幕模式适配；
  - 3 档字号缩放（0.9x / 1.0x / 1.1x）；
  - 中 / 英双语无缝切换；
  - 全局手势滑动返回（`SwipeToDismissBox`）。

---

## 🏗️ 项目架构

项目采用清晰的领域分层架构：

```text
.
├── app/                              # Wear OS 专属表现层与硬件交互
│   └── src/main/java/com/boompala/
│       ├── MainActivity.kt           # 单 Activity 入口，挂载 BoompalaApp 状态机
│       ├── archive/                  # 本地 SQLite 数据库 (boompa_archives.db) 与快照编解码
│       ├── compass/                  # 传感器融合、方位平滑与玄空罗盘数据
│       ├── settings/                 # AppSettings 与 Preferences DataStore 存储
│       └── ui/                       # Compose for Wear OS 页面、自适应背景微光、表冠滚动容器
├── engine/                           # 纯 Kotlin 领域算法引擎 (UI / Android 无关)
│   └── src/main/
│       ├── kotlin/com/boompala/engine/
│       │   ├── astrology/            # 西洋占星本命盘引擎 (十大行星/十二宫/相位)
│       │   ├── bazi/                 # 八字命盘引擎 (四柱/藏干/十神/纳音/起大运)
│       │   ├── bone/                 # 袁天罡称骨算命引擎
│       │   ├── calendar/             # 历法适配器 (Solar -> Lunar -> EightChar)
│       │   ├── dailyfortune/         # 今日运势与流日个人专属运势评估引擎
│       │   ├── data/                 # 离线 JSON 典籍与塔罗 Repository
│       │   ├── liuyao/               # 六爻排盘引擎与铜钱摇卦模拟
│       │   ├── meihua/               # 梅花易数时间起卦引擎
│       │   ├── ninestar/             # 九星气学引擎
│       │   ├── numerology/           # 毕达哥拉斯生命灵数引擎
│       │   ├── rules/                # 纳甲、六亲、六神、八宫、旬空等易学核心规则
│       │   ├── tarot/                # 塔罗牌引擎、洗牌算法与牌阵
│       │   └── xiaoliuren/           # 小六壬六宫循环算法
│       └── assets/                   # APK 内置离线 JSON 数据 (爻辞/典籍/知识/塔罗)
└── docs/                             # 易学口径、罗盘约定、运势规则与资料来源文档
```

---

## 🛠️ 环境要求

- **Android Studio** Ladybug / Koala 或现代 Android 构建环境
- **JDK 17**（JVM target 17）
- **Android SDK**：compileSdk / targetSdk 35，minSdk 26（Android 8.0+ / Wear OS 2.0+）
- **Wear OS 设备**：实体手表（支持几乎所有 Wear OS / Android 手表设备）或 Android Studio 模拟器

---

## 🚀 构建与验证

### 1. 运行单元测试
运行引擎算法与 App 逻辑的全部 JVM 单元测试：

```bash
./gradlew --no-daemon :engine:test :app:testDebugUnitTest
```

### 2. 生成 Debug APK
```bash
./gradlew --no-daemon :app:assembleDebug
```
输出文件：`app/build/outputs/apk/debug/app-debug.apk`

### 3. 生成 Release APK (本地安装包)
```bash
./gradlew --no-daemon :app:assembleRelease -x :app:lintVitalAnalyzeRelease
```
输出文件：`app/build/outputs/apk/release/app-release.apk`

> **说明**：当前 Release 构建使用 Debug 签名方便直接侧载到手表测试；排除 `lintVitalAnalyzeRelease` 是由于 Kotlin Analysis API 上游工具链在特定 Wear/Compose 规则分析时的已知兼容问题。

---

## 📄 数据与许可

Boompala 严格遵守开源许可与公共领域规范，所有内置数据均位于 `engine/src/main/assets/`：
- `yao_text.json`：来源于公共领域（Public Domain）经典古籍；
- `hexagram_interpretations.json` / `knowledge.json`：易学常识与现代白话整理；
- `tarot_cards.json` 与 WebP 牌图：经典 1909 年莱德·韦特·史密斯塔罗牌（Pamela Colman Smith 绘，属公共领域）；
- 详细许可和引用说明请参阅各资产同级目录下的 `NOTICE-*.txt` 与 `docs/` 文档。
