# Boompala 易学

Boompala 是一款面向 Wear OS 手表的离线易学工具，使用 Kotlin、Jetpack Compose for Wear OS 和本地数据文件构建。应用不依赖网络服务，主要功能和排盘资料都保存在 APK 内。

## 功能

- 六爻排盘：输入六爻结果，计算本卦、变卦、纳甲、六亲、六神、世应和旬空，并显示爻辞。
- 时间起卦：根据当前时间进行梅花易数时间起卦，显示本卦、互卦、变卦和体用。
- 小六壬：根据农历月、日、时计算六宫结果。
- 罗盘：读取设备方向传感器，显示磁北、八卦、二十四山和元运九星。
- 离线浏览：查看六十四卦、爻辞、卦辞/彖传/象传和道教知识资料。
- 归档：保存六爻、梅花易数和小六壬结果，支持标记名称、备注、颜色、筛选、修改和删除。
- Wear OS 交互：支持触摸滚动、表冠旋转滚动、右侧滚动位置指示和可选震动反馈。
- 设置：圆屏/方屏模式、内容字号、页面动画和表冠滚动开关。

## 项目结构

```text
app/      Wear OS 应用、Compose UI、传感器、归档 SQLite 存储
engine/   与 UI 无关的六爻、梅花易数、小六壬和历法计算引擎
docs/     规则和资料说明
```

`engine` 负责领域计算和数据模型；`app` 负责页面、Wear OS 输入、离线资料加载和本地归档。六爻引擎数组保持“初爻到上爻”的领域契约，页面只在显示边界进行倒序展示。

## 环境要求

- Android Studio 或可用的 Android Gradle 环境
- JDK 17 目标兼容配置
- Android SDK 35
- Wear OS 手表或 Wear OS 模拟器

项目使用 Gradle Wrapper，首次构建可能需要下载或准备本地 Gradle 依赖缓存。

## 构建与测试

运行全部 JVM 单元测试：

```bash
./gradlew --no-daemon :app:testDebugUnitTest :engine:test
```

生成 Debug APK：

```bash
./gradlew --no-daemon :app:assembleDebug
```

生成当前 Release APK：

```bash
./gradlew --no-daemon :app:assembleRelease -x :app:lintVitalAnalyzeRelease
```

Release 构建目前关闭了 R8 混淆和资源收缩。原因是当前 AGP/R8 版本在 Wear/Compose 全程序分析阶段耗时异常；关闭 R8 后仍生成可安装的 Release 包。`lintVitalAnalyzeRelease` 还存在独立的 Kotlin Analysis API 工具链崩溃，因此构建命令暂时排除该任务，不能据此宣称 Release Lint 已通过。

## APK 输出位置

```text
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release.apk
```

当前 APK 使用 Debug signing，仅适合本地测试和手表安装，不适合作为正式商店发布签名。

## 性能注意事项

- 罗盘传感器可能以高于 UI 所需的频率回调；当前实现将 Compose 状态发布限制为最高 15Hz，避免持续触发整页重组和复杂 Canvas 重绘。
- 归档列表和详情的 SQLite 查询在 `Dispatchers.IO` 执行；详情查询使用 `WHERE id=?` 单行读取，避免在主线程扫描整张归档表和快照 JSON。
- 真实手表上的帧率、表冠震动和传感器表现需要在 Wear OS 手表或模拟器上单独验证，APK 编译成功不等同于硬件交互验证。

## 数据与许可

离线资料位于 `engine/src/main/assets/`：

- `yao_text.json`：卦辞、爻辞和经典文本
- `hexagram_interpretations.json`：六十四卦和八卦离线解释
- `knowledge.json`：知识文章
- `NOTICE-*.txt`：对应资料来源和许可说明

新增或修改资料时，应同步更新对应的 NOTICE 文件和数据校验测试。

