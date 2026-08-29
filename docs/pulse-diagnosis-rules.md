# 中医把脉口径与算法规范 (Pulse Diagnosis Conventions)

本文档记录 Boompala 中医“把脉”模块的硬件传感器数据流、Task Force HRV 时域特征提取、SQI 置信度与覆盖率质检、十二脉象分类口径及结果呈现规范。

---

## 1. 硬件传感器三合一数据流与脱腕拦截

系统完全基于 Android Wear OS 标准传感器架构，拒绝任何私有闭源 SDK 与虚假合成数据：

1. **`Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT`（离腕传感器）**：
   - 硬件级红外与电容传感器，检测用户是否佩戴手表；
   - 离腕（`0.0f`）时立即阻断把脉，提示“未贴紧手腕”，严禁伪造数据。
2. **`Sensor.TYPE_HEART_BEAT`（硬件真实逐搏事件传感器）**：
   - 每次硬件 AFE 捕捉到真实的 QRS/PPG 收缩峰时上报一次事件；
   - 使用 `event.timestamp`（系统纳秒级硬件时钟，杜绝 `System.currentTimeMillis()` 带来的线程排队与 GC 抖动）记录真实心搏发生的物理时刻；
   - 计算相邻心搏间期 $IBI_i = (timestamp_{i} - timestamp_{i-1}) / 10^6 \text{ ms}$；
   - 作为屏幕中央脉冲线展开与触觉微震的**唯一物理触发源**。
3. **`Sensor.TYPE_HEART_RATE`（心率与接触置信度传感器）**：
   - 实时采集瞬时脉率（BPM）与接触质量 Accuracy（0..3），作为心率数值与质检置信度的辅助数据流。

---

## 2. 信号处理与质控（SQI & Coverage）流水线

```text
硬件逐搏事件 (TYPE_HEART_BEAT, event.timestamp 纳秒)
       │
       ├── 离腕判定 (OFFBODY_DETECT == 0 -> 阻断并提示)
       ├── 纳秒时间戳差值 -> 逐搏间期序列 IBI (300ms ~ 1800ms 生理极限过滤)
       │
       ▼
20 秒采样窗口质量评估 (PulseFeatureExtractor.extractFromBeatSeries)
       ├── 有效心搏总数 (Valid Beat Count >= 10)
       ├── 采样时间覆盖率 (Coverage Rate >= 60%)
       ├── 传感器平均置信度 (Mean Confidence >= 1.0)
       │
       ├── 质检未达标 -> PulseSensorState.QualityFailed (要求重测)
       │
       ▼ (质检达标)
HRV 时域特征提取与辨证计算
       ├── 平均心率 BPM_mean
       ├── 迷走神经活性指标 RMSSD = sqrt(1/(N-1) * sum((IBI_{i+1} - IBI_i)^2))
       ├── 早搏与突发间歇比率 pNN50 = Count(|IBI_{i+1} - IBI_i| > 50ms) / (N-1) * 100%
       └── 节律规整度系数 Regularity
       │
       ▼
中医十二脉象辨证分类器 (TcmPulseClassifier)
       │
       ▼
把脉结果 (置顶脉象波形图 + 脉象特征 + 8 层辨证调摄体系 + 当令时辰子午流注)
```

---

## 3. 十二经典脉象判定标准

| 脉象分类 | 核心物理 / 真实 HRV 时域判定条件 | 临床体征释义 |
| :--- | :--- | :--- |
| **平脉 (Ping)** | $68 \le \text{BPM} \le 82$, $25\text{ms} \le \text{RMSSD} \le 42\text{ms}$ | 节律从容，有神有胃有根，脏腑冲和 |
| **滑脉 (Hua)** | $66 \le \text{BPM} \le 92$, $\text{RMSSD} \ge 42\text{ms}$（微循环充盈流利） | 往来流利，应指圆滑，如珠走盘 |
| **弦脉 (Xian)** | $\text{BPM} \ge 68$, $\text{RMSSD} \le 22\text{ms}$（交感张力高，管壁紧绷） | 端直以长，如按琴弦，血管张力增高 |
| **迟脉 (Chi)** | $\text{BPM} < 58$, $\text{RMSSD} \ge 20\text{ms}$ | 一息不足四至，阳气不足或阴寒内盛 |
| **数脉 (Shu)** | $\text{BPM} > 95$, $\text{RMSSD} < 40\text{ms}$ | 一息五至以上，热邪内蕴或虚热内生 |
| **洪脉 (Hong)** | $\text{BPM} > 95$, $\text{RMSSD} \ge 40\text{ms}$（脉体汹涌澎湃） | 来盛去衰，滔滔满指，火热炽盛 |
| **缓脉 (Huan)** | $58 \le \text{BPM} \le 68$, $22\text{ms} \le \text{RMSSD} \le 42\text{ms}$ | 怠缓和柔，一息四至，脾胃气和 |
| **濡脉 (Ru)** | $\text{BPM} < 75$, $K \le 0.32$, 脉体浮软 | 浮而细软，如絮在水，气虚夹湿 |
| **细脉 (Xi)** | $\text{BPM} < 68$, $K \le 0.28$ 或 $\text{RMSSD} < 20\text{ms}$ 且脉力微弱 | 脉细如线，应指明显，阴血亏虚 |
| **结代脉 (Jie Dai)** | $\text{Regularity} < 75\%$ 或 $\text{pNN50} \ge 12\%$（突发期前搏动停顿） | 脉来歇止，缓/数而偶有一止，气血不续 |
| **沉脉 (Chen)** | 轻按不显，深按充盈 | 深潜入内，里寒水气或气机内收 |
| **浮脉 (Fu)** | 轻取即得，重按稍减 | 升越于表，外邪在表或虚阳浮越 |

---

## 4. 界面与交互规范

1. **命名规范**：摒弃悬浮的“推演”字眼，全量采用“把脉”、“开始把脉”、“把脉结果”等朴素大白话；
2. **走带律动**：脉冲波形由真实 `TYPE_HEART_BEAT` 逐搏事件物理驱动，实时反映心律快慢与波动；
3. **结果呈现**：脉象波形图置于卡片顶部最显眼位置，下方紧跟“四字脉诀”与症候释义；
4. **质控兜底**：离腕时阻断并提示“未贴紧手腕”；覆盖率不足 $60\%$ 时拒绝妄断并提供【重新把脉】按钮。
