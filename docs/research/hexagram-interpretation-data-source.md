# 六十四卦通用解释数据来源与许可

## 目的与边界

`engine/src/main/assets/hexagram_interpretations.json` 为离线结果页提供每一卦的
卦名、上下卦及取象、核心卦义、关键词、通用趋势、处事建议，以及感情、学业/事业、
财运三个通用参考维度。它不参与排盘算法，不能自动选择用神，也不对具体问题作确定的
吉凶结论。

应用和数据均固定显示：**“通用卦义参考，不代表针对具体问题的确定结论。”**

## 来源与许可

这些白话概括是 **Boompala 项目原创整理**：以传统卦名、八卦取象和《周易》的公有领域
概念为写作范围，重新撰写为短篇通用说明；没有复制任何版权链不明确的现代解卦书、课程、
网站或 AI 服务输出。数据文件按 **CC0 1.0 Universal** 发布，许可证全文与说明链接为
<https://creativecommons.org/publicdomain/zero/1.0/>。

此 CC0 声明仅覆盖本项目新写的 `hexagram_interpretations.json` 内容，不改变已有
`yao_text.json` 的中文维基文库来源与 CC BY-SA 4.0 声明，也不改变项目代码的许可。

## 数据结构与校验

数据集使用稳定的六位 `code`（初爻至上爻，阳为 `1`、阴为 `0`）作为键，另有八卦取象表。
每个卦条目包含 `name`、`coreMeaning`、`upperTrigram`、`lowerTrigram`、`keywords`、
`generalTrend`、`advice`、`relationship`、`career`、`wealth`。运行时读取器拒绝：

- 非版本 1 的 JSON；
- 缺少固定免责声明、来源或许可的 JSON；
- 缺失或额外的八卦取象；
- 不是全部 64 个二进制卦码、重复卦码或任一字段不完整的数据。

`JsonHexagramInterpretationRepositoryTest` 会加载实际离线资源，覆盖完整性及“火水未济 → 山泽损”
固定回归案例。
