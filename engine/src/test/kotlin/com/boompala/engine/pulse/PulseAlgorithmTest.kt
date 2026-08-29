package com.boompala.engine.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PulseAlgorithmTest {

    @Test
    fun testButterworthFilterRemovesDcAndHighFrequency() {
        val filter = PpgButterworthFilter(sampleRateHz = 25.0, highPassCutoffHz = 0.5, lowPassCutoffHz = 8.0)
        val n = 100
        // 合成信号：DC 偏移量 100.0 + 1.2Hz 主心律信号 + 11.0Hz 高频噪声
        val input = DoubleArray(n) { i ->
            val t = i / 25.0
            100.0 + 5.0 * sin(2.0 * PI * 1.2 * t) + 2.0 * sin(2.0 * PI * 11.0 * t)
        }

        val filtered = filter.filterBatch(input)
        assertEquals(n, filtered.size)

        // 验证 DC 偏移（基线漂移）被消除：均值接近 0
        val mean = filtered.average()
        assertTrue("直流偏移应被高通滤波器滤除，当前均值: $mean", kotlin.math.abs(mean) < 2.0)
    }

    @Test
    fun testElgendiPeakDetectionOnSyntheticPulse() {
        val sampleRate = 25.0
        val durationSec = 10.0
        val totalSamples = (sampleRate * durationSec).toInt()
        val pulseIntervalSec = 0.8 // 对应 75 BPM

        val signal = DoubleArray(totalSamples) { i ->
            val t = i / sampleRate
            val phase = (t % pulseIntervalSec) / pulseIntervalSec
            when {
                phase < 0.2 -> (phase / 0.2) * 10.0
                phase < 0.45 -> 10.0 - ((phase - 0.2) / 0.25) * 6.0
                phase < 0.60 -> 4.0 + ((phase - 0.45) / 0.15) * 2.0 // 重搏波
                else -> 6.0 * (1.0 - (phase - 0.60) / 0.40)
            }
        }

        val peaks = ElgendiPeakDetector.findPeaks(signal, sampleRate)
        // 10秒内 75BPM 大约有 12 个心搏周期
        assertTrue("检测到的波峰数应在 10 到 14 之间，实际: ${peaks.size}", peaks.size in 10..14)

        // 特征提取
        val metrics = PulseFeatureExtractor.extract(signal, peaks, sampleRate, isRawPpg = true)
        assertTrue("计算心率应接近 75 BPM，实际: ${metrics.heartRateBpm}", metrics.heartRateBpm in 70.0..80.0)
        assertTrue("节律应高度规整 (>90%)，实际: ${metrics.regularityPercent}", metrics.regularityPercent >= 90.0)
    }

    @Test
    fun testTcmPulseClassifierDecisions() {
        // 1. 弦脉判定：K >= 0.40 且 h2Ratio >= 0.48
        val wiryMetrics = PulseFeatureMetrics(
            heartRateBpm = 74.0,
            regularityPercent = 95.0,
            h1 = 1.0,
            h2 = 0.55,
            h3 = 0.58,
            kValue = 0.43,
            h3Ratio = 0.58,
            h2Ratio = 0.55,
            isRawPpg = true,
        )
        val wiryResult = TcmPulseClassifier.classify(wiryMetrics, hour24 = 15)
        assertEquals(PulseCategory.XIAN, wiryResult.category)
        assertEquals("申时", wiryResult.meridianInfo.earthlyBranch)
        assertEquals("足太阳膀胱经", wiryResult.meridianInfo.meridianName)

        // 2. 滑脉判定：h3Ratio >= 0.52 且 h2Ratio <= 0.44 且 K < 0.40
        val slipperyMetrics = PulseFeatureMetrics(
            heartRateBpm = 76.0,
            regularityPercent = 96.0,
            h1 = 1.0,
            h2 = 0.30,
            h3 = 0.62,
            kValue = 0.36,
            h3Ratio = 0.62,
            h2Ratio = 0.30,
            isRawPpg = true,
        )
        val slipperyResult = TcmPulseClassifier.classify(slipperyMetrics, hour24 = 9)
        assertEquals(PulseCategory.HUA, slipperyResult.category)
        assertEquals("巳时", slipperyResult.meridianInfo.earthlyBranch)
        assertEquals("足太阴脾经", slipperyResult.meridianInfo.meridianName)

        // 3. 迟脉判定：BPM < 58
        val slowMetrics = PulseFeatureMetrics(
            heartRateBpm = 52.0,
            regularityPercent = 92.0,
            h1 = 1.0,
            h2 = 0.35,
            h3 = 0.45,
            kValue = 0.33,
            h3Ratio = 0.45,
            h2Ratio = 0.35,
            isRawPpg = true,
        )
        val slowResult = TcmPulseClassifier.classify(slowMetrics, hour24 = 23)
        assertEquals(PulseCategory.CHI, slowResult.category)
        assertEquals("子时", slowResult.meridianInfo.earthlyBranch)

        // 4. 结代脉判定：节律规整度 < 75%
        val irregularMetrics = PulseFeatureMetrics(
            heartRateBpm = 70.0,
            regularityPercent = 65.0,
            h1 = 1.0,
            h2 = 0.35,
            h3 = 0.45,
            kValue = 0.34,
            h3Ratio = 0.45,
            h2Ratio = 0.35,
            isRawPpg = true,
        )
        val irregularResult = TcmPulseClassifier.classify(irregularMetrics, hour24 = 11)
        assertEquals(PulseCategory.JIE_DAI, irregularResult.category)
        assertEquals("午时", irregularResult.meridianInfo.earthlyBranch)
    }

    @Test
    fun testExtractFromBeatSeriesAndQualityEvaluation() {
        // 1. 模拟 20 秒内正常的 24 次心搏 (平均间期 800ms 对应 75 BPM)
        val normalTimestamps = mutableListOf<Long>()
        val normalBpms = mutableListOf<Double>()
        val normalAccuracies = mutableListOf<Int>()
        var curTime = 1000L
        for (i in 0 until 24) {
            normalTimestamps.add(curTime)
            normalBpms.add(75.0 + (i % 3))
            normalAccuracies.add(3)
            curTime += 800L
        }

        val goodMetrics = PulseFeatureExtractor.extractFromBeatSeries(
            beatTimestampsMs = normalTimestamps,
            bpmRecords = normalBpms,
            accuracyRecords = normalAccuracies,
            durationSeconds = 20,
        )
        assertTrue("数据应判定为可靠", goodMetrics.quality.isReliable)
        assertTrue("有效覆盖率应 > 75%，实际: ${goodMetrics.quality.coveragePercent}", goodMetrics.quality.coveragePercent >= 75.0)
        assertTrue("心率计算应在 70-80 之间，实际: ${goodMetrics.heartRateBpm}", goodMetrics.heartRateBpm in 70.0..80.0)

        // 2. 模拟样本不足/中途脱腕 (仅有 4 次心搏)
        val sparseTimestamps = listOf(1000L, 1800L, 2600L, 3400L)
        val sparseBpms = listOf(75.0, 75.0, 75.0, 75.0)
        val sparseAccuracies = listOf(3, 3, 3, 3)

        val badMetrics = PulseFeatureExtractor.extractFromBeatSeries(
            beatTimestampsMs = sparseTimestamps,
            bpmRecords = sparseBpms,
            accuracyRecords = sparseAccuracies,
            durationSeconds = 20,
        )
        assertTrue("样本严重不足应判定为不可靠", !badMetrics.quality.isReliable)
        assertNotNull("应具备明确的未达标原因提示", badMetrics.quality.failureReason)
    }

    @Test
    fun testHrvDrivenHuaAndXianClassification() {
        // 滑脉：RMSSD = 48.0ms (迷走神经充沛，微循环流利如珠)
        val huaMetrics = PulseFeatureMetrics(
            heartRateBpm = 76.0,
            regularityPercent = 95.0,
            rmssdMs = 48.0,
            isRawPpg = false,
        )
        val huaResult = TcmPulseClassifier.classify(huaMetrics, hour24 = 10)
        assertEquals(PulseCategory.HUA, huaResult.category)

        // 弦脉：RMSSD = 15.0ms (交感紧张，管壁紧绷如弦)
        val xianMetrics = PulseFeatureMetrics(
            heartRateBpm = 76.0,
            regularityPercent = 95.0,
            rmssdMs = 15.0,
            isRawPpg = false,
        )
        val xianResult = TcmPulseClassifier.classify(xianMetrics, hour24 = 10)
        assertEquals(PulseCategory.XIAN, xianResult.category)
    }
}
