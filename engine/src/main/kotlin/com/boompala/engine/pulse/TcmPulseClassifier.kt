package com.boompala.engine.pulse

/**
 * 中医脉象辨证分类决策树引擎。
 * 结合心率、节律规整度、波形指标 (h1, h2, h3, K值) 与时辰子午流注，输出权威诊断与调摄报告。
 */
object TcmPulseClassifier {

    /**
     * 对脉搏指标执行中医辨证分类并打包综合结果。
     *
     * @param metrics 物理与波形特征指标
     * @param hour24 当前测量发生时的系统 24 小时制小时（0..23），用于推导子午流注经络
     * @param timestampMillis 测量时间戳
     */
    fun classify(
        metrics: PulseFeatureMetrics,
        hour24: Int,
        timestampMillis: Long = System.currentTimeMillis(),
    ): PulseDiagnosisResult {
        val category = determineCategory(metrics)
        val profile = PulseCatalog.getProfile(category)
        val meridian = PulseCatalog.getMeridianInfluence(hour24)

        return PulseDiagnosisResult(
            category = category,
            metrics = metrics,
            profile = profile,
            meridianInfo = meridian,
            timestampMillis = timestampMillis,
        )
    }

    private fun determineCategory(metrics: PulseFeatureMetrics): PulseCategory {
        // 1. 节律优先：规整度显著较低或期前搏动突变率 pNN50 >= 12% 判定为结代脉
        if (metrics.regularityPercent < 75.0 || metrics.pnn50Percent >= 12.0) {
            return PulseCategory.JIE_DAI
        }

        // 2. 频率极值判定
        if (metrics.heartRateBpm > 95.0) {
            return if (metrics.rmssdMs >= 40.0 || (metrics.isRawPpg && metrics.kValue >= 0.42)) {
                PulseCategory.HONG // 洪脉：来盛去衰，滔滔满指
            } else {
                PulseCategory.SHU // 数脉：一息六至，脉流急促
            }
        }
        if (metrics.heartRateBpm < 58.0) {
            return if (metrics.rmssdMs < 20.0 || (metrics.isRawPpg && metrics.kValue <= 0.28)) {
                PulseCategory.XI // 细脉：细小微弱
            } else {
                PulseCategory.CHI // 迟脉：一息三至，脉率偏缓
            }
        }

        // 3. 弦脉判定：交感神经紧张度高、血管阻力增大、RMSSD 偏低 (<= 22ms) 或 K >= 0.40
        if ((metrics.rmssdMs <= 22.0 && metrics.heartRateBpm >= 68.0) ||
            (metrics.isRawPpg && metrics.kValue >= 0.40 && metrics.h2Ratio >= 0.48)
        ) {
            return PulseCategory.XIAN
        }

        // 4. 滑脉判定：副交感活性旺盛、微循环充盈流利、RMSSD 充沛 (>= 42ms) 或 h3Ratio 灵动跳跃
        if ((metrics.rmssdMs >= 42.0 && metrics.heartRateBpm in 66.0..92.0) ||
            (metrics.isRawPpg && metrics.h3Ratio >= 0.52 && metrics.h2Ratio <= 0.44)
        ) {
            return PulseCategory.HUA
        }

        // 5. 细脉 / 濡脉判定：气血偏亏或湿困于表
        if (metrics.kValue <= 0.28 && metrics.heartRateBpm < 68.0) {
            return PulseCategory.XI
        }
        if (metrics.kValue <= 0.32 && metrics.h2Ratio <= 0.36 && metrics.heartRateBpm < 75.0) {
            return PulseCategory.RU
        }

        // 6. 缓脉：脉率在 58~68 之间，脉体和缓
        if (metrics.heartRateBpm in 58.0..68.0 && metrics.rmssdMs in 22.0..42.0) {
            return PulseCategory.HUAN
        }

        // 7. 平脉：形态适中稳健，脏腑冲和
        return PulseCategory.PING
    }
}
