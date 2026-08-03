# Boompala 第一阶段架构

## 模块边界

```text
app
├── ui/          Wear OS Compose 页面、页面状态和交互
└── MainActivity Android 入口

engine
├── model/       与 UI 无关的输入与完整排盘结果
├── rules/       八宫、纳甲、六亲、六神、旬空
├── calendar/    四柱历法接口与 6tail/lunar-java 适配器
├── data/        可替换的爻辞数据接口
├── BasicHexagramEngine
│               纯 Kotlin 的爻级推导
├── LiuYaoEngine
│               完整装卦编排
└── meihua/      独立的梅花易数时间起卦、互卦与体用规则
```

`app` 只负责输入、展示和当前页面状态；`engine` 不依赖 Compose、Activity、WebView、网络或服务器。后续历史记录、收藏和其他易学工具可以各自增加 data/feature 模块，而不改变六爻核心模型。

## 当前已确认的计算边界

- 六条输入始终按初爻到上爻保存。
- `MANUAL_CAST` 与 `DIRECT_INPUT` 都先转换为同一个 `HexagramInput`；
  已知本卦/变卦通过逐爻阴阳差异还原动爻，不另设动爻输入，也不复制装卦逻辑。
- 少阳/少阴为静爻，老阳/老阴为动爻。
- 本卦直接使用输入阴阳；动爻翻转阴阳形成变卦。
- 没有动爻时，变卦在结果模型中为 `null`，UI 不渲染变卦区。
- 八宫、世应采用已固定的 bopo/najia 参考规则。
- 纳甲采用京房纳甲，六亲按各自卦宫五行计算，变卦重新定宫。
- 六神由日干起青龙顺排，旬空由日柱计算。
- `DivinationTimeInfo` 是历法显示与装卦共用的唯一时间模型。
  `SixTailGanzhiCalendar` 从设备公历时间依次完成 Solar、Lunar、干支转换，
  同时给出公历、农历与四柱；四柱采用精确节气交接、设备时区，
  晚子时默认按“日柱算明天”。

梅花时间起卦使用同一历法输出中的农历年地支、农历月日和时支数字，
但不依赖 `LiuYaoResult`、纳甲或六亲；完整规则和固定体用口径见
`docs/meihua-time-casting-rules.md`。

## Wear OS 滚动边界

- `RotaryScrollColumn` 统一采用轻量 Wear Material 3 结构：
  `AppScaffold → ScreenScaffold → LazyColumn`。`ScreenScaffold` 与列表共享同一个
  `LazyListState`；列表使用 `RotaryScrollableDefaults.behavior` 处理表冠、触觉反馈，
  scaffold 负责右侧 `ScrollIndicator`，不引入 `TransformingLazyColumn`。
  当前供结果页和设置页使用，后续历史页直接复用。
- 表冠滚动设置由 Preferences DataStore 持久化，默认开启；关闭时不安装
  rotary behavior，列表的触摸滚动与点击行为保持不变。

## 当前刻意未固化的规则

`engine/src/main/assets/yao_text.json` 保存 64×6 条位置爻辞。`JsonLineTextRepository`
在应用启动时从该离线 JSON 读取并校验 64 卦、384 条、每卦六个位置和无重复键。
数据来源、固定修订号与 CC BY-SA 4.0 归属存于 JSON 元数据和同目录
`NOTICE-yao-text.txt`；加载失败时仍降级到空 repository，UI 会明确显示“爻辞数据不可用”。
晚子时“算明天/算当天”已通过 `SixTailGanzhiCalendar` 构造参数显式切换，其他流派差异也应保持在 engine 的策略边界内。
