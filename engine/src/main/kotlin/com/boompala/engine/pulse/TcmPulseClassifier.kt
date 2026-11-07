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
        measuredWaveform: List<Float> = emptyList(),
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
            measuredWaveform = measuredWaveform,
        )
    }

    private fun determineCategory(metrics: PulseFeatureMetrics): PulseCategory {
        // 1. 节律优先：期前搏动突变或规整度显著较低判定为结代脉
        val isIrregular = metrics.ectopicBeatCount >= 2 ||
            (metrics.ectopicBeatCount >= 1 && metrics.regularityPercent < 78.0) ||
            metrics.regularityPercent < 68.0
        if (isIrregular) {
            return PulseCategory.JIE_DAI
        }

        // 2. 频率极值判定
        if (metrics.heartRateBpm > 95.0) {
            return if (metrics.rmssdMs >= 38.0 || (metrics.isRawPpg && metrics.kValue >= 0.42 && metrics.h1 >= 0.90)) {
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

        // 3. 弦脉判定：交感神经紧张度高、血管阻力增大、RMSSD 偏低 (15..22ms, BPM >= 68) 或 (isRawPpg 且 K >= 0.40 且 h2Ratio >= 0.48)
        if ((metrics.rmssdMs in 15.0..22.0 && metrics.heartRateBpm >= 68.0) ||
            (metrics.isRawPpg && metrics.kValue >= 0.40 && metrics.h2Ratio >= 0.48)
        ) {
            return PulseCategory.XIAN
        }

        // 4. 滑脉判定：副交感活性旺盛、微循环充盈流利、RMSSD 充沛 (>= 45ms, BPM in 68..95) 或 (isRawPpg 且 h3Ratio >= 0.52 且 h2Ratio <= 0.44)
        if ((metrics.rmssdMs >= 45.0 && metrics.heartRateBpm in 68.0..95.0) ||
            (metrics.isRawPpg && metrics.h3Ratio >= 0.52 && metrics.h2Ratio <= 0.44)
        ) {
            return PulseCategory.HUA
        }

        // 5. 浮脉判定：阳气升发于表、升支迅捷 (BPM in 78..95, RMSSD in 28.0..45.0) 或 (isRawPpg 且 BPM in 74..95 且 K in 0.34..0.40 且 h2Ratio <= 0.40)
        if ((metrics.heartRateBpm in 78.0..95.0 && metrics.rmssdMs in 28.0..45.0) ||
            (metrics.isRawPpg && metrics.heartRateBpm in 74.0..95.0 && metrics.kValue in 0.34..0.40 && metrics.h2Ratio <= 0.40)
        ) {
            return PulseCategory.FU
        }

        // 6. 细脉判定：微细如发丝 (RMSSD < 15ms, BPM in 58..72) 或 (isRawPpg 且 K <= 0.28 且 RMSSD < 20ms)
        if ((metrics.heartRateBpm in 58.0..72.0 && metrics.rmssdMs < 15.0) ||
            (metrics.isRawPpg && metrics.kValue <= 0.28 && metrics.rmssdMs < 20.0)
        ) {
            return PulseCategory.XI
        }

        // 7. 沉脉判定：脉位深潜、交感内敛 (BPM in 58..64 且 RMSSD in 15..24，或 BPM in 64..68 且 RMSSD in 15..22) 或 (isRawPpg 且 BPM in 58..70 且 K in 0.22..0.30)
        if ((metrics.heartRateBpm in 58.0..64.0 && metrics.rmssdMs in 15.0..24.0) ||
            (metrics.heartRateBpm in 64.0..68.0 && metrics.rmssdMs in 15.0..22.0) ||
            (metrics.isRawPpg && metrics.heartRateBpm in 58.0..70.0 && metrics.kValue in 0.22..0.30)
        ) {
            return PulseCategory.CHEN
        }

        // 8. 濡脉判定：气血双虚夹湿、浮而细软 (BPM in 64..78 且 RMSSD in 22..28) 或 (isRawPpg 且 BPM < 78 且 K <= 0.32 且 h2Ratio <= 0.35)
        if ((metrics.heartRateBpm in 64.0..78.0 && metrics.rmssdMs in 22.0..28.0) ||
            (metrics.isRawPpg && metrics.heartRateBpm < 78.0 && metrics.kValue <= 0.32 && metrics.h2Ratio <= 0.35)
        ) {
            return PulseCategory.RU
        }

        // 9. 缓脉判定：一息四至、从容和柔 (BPM in 58..68 且 RMSSD >= 24.0)
        if (metrics.heartRateBpm in 58.0..68.0 && metrics.rmssdMs >= 24.0) {
            return PulseCategory.HUAN
        }

        // 10. 平脉：形态适中稳健，脏腑冲和
        return PulseCategory.PING
    }
}
