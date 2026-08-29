package com.boompala.engine.pulse

import java.time.LocalTime

/**
 * 中医脉象辨证分类器（依据位、数、形、势四诊要素与子午流注时辰归经）。
 * 基于心率（数/迟）、RMSSD / 幅值（滑/弦/洪/细）、规整度 / pNN50（结代）与波形系数（沉/浮/濡/缓）综合裁决。
 */
object TcmPulseClassifier {

    fun classify(
        metrics: PulseFeatureMetrics,
        hour24: Int = LocalTime.now().hour,
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
        if ((metrics.rmssdMs >= 42.0 && metrics.heartRateBpm in 66.0..95.0) ||
            (metrics.isRawPpg && metrics.h3Ratio >= 0.52 && metrics.h2Ratio <= 0.44)
        ) {
            return PulseCategory.HUA
        }

        // 5. 浮脉判定：举之有余，升支迅捷幅值充盈 (75~95 BPM)
        if ((metrics.rmssdMs in 28.0..42.0 && metrics.heartRateBpm in 75.0..95.0 && metrics.kValue in 0.34..0.40) ||
            (metrics.isRawPpg && metrics.h1 >= 0.90 && metrics.h2Ratio <= 0.40 && metrics.heartRateBpm in 72.0..95.0)
        ) {
            return PulseCategory.FU
        }

        // 6. 细脉判定：微细如发丝
        if (metrics.kValue <= 0.28 && metrics.heartRateBpm < 68.0 && metrics.rmssdMs < 20.0) {
            return PulseCategory.XI
        }

        // 7. 沉脉判定：轻取不应，脉位深伏沉稳 (58~70 BPM，K 值低沉)
        if ((metrics.kValue in 0.24..0.30 && metrics.heartRateBpm in 58.0..70.0 && metrics.rmssdMs in 18.0..28.0 && metrics.h2Ratio >= 0.36) ||
            (metrics.isRawPpg && metrics.kValue in 0.22..0.29 && metrics.h2Ratio <= 0.38)
        ) {
            return PulseCategory.CHEN
        }

        // 8. 濡脉判定：气血偏亏，浮而细软 (h2Ratio <= 0.35 浮软)
        if ((metrics.kValue <= 0.32 && metrics.h2Ratio <= 0.35 && metrics.heartRateBpm < 75.0) ||
            (metrics.rmssdMs in 18.0..28.0 && metrics.heartRateBpm in 68.0..75.0 && metrics.kValue <= 0.32)
        ) {
            return PulseCategory.RU
        }

        // 9. 缓脉：脉率在 58~68 之间，脉体和缓
        if (metrics.heartRateBpm in 58.0..68.0 && metrics.rmssdMs in 22.0..42.0) {
            return PulseCategory.HUAN
        }

        // 10. 平脉：形态适中稳健，脏腑冲和
        return PulseCategory.PING
    }
}
