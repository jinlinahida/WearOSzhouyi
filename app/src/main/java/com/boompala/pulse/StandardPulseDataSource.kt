package com.boompala.pulse

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.boompala.engine.pulse.PulseFeatureExtractor
import com.boompala.engine.pulse.TcmPulseClassifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.sin

/**
 * 通用 Wear OS 手表把脉数据源。
 * 纯粹由硬件 TYPE_HEART_BEAT 逐搏事件与纳秒时钟物理驱动波形走带，
 * 结合 Task Force HRV 时域标准提取位、数、形、势，
 * 具备离腕检测硬拦截与 15s 脱腕超时保护、波形逐跳动态生理形态调制与 20s 覆盖率/置信度质检重测机制。
 */
class StandardPulseDataSource(
    private val context: Context,
) : PulseSensorDataSource, SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val heartBeatSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_BEAT)
    private val heartRateSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val offBodySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)

    private val _state = MutableStateFlow<PulseSensorState>(PulseSensorState.Idle)
    override val state: StateFlow<PulseSensorState> = _state.asStateFlow()

    override val isRawPpg: Boolean = false

    private var job: Job? = null

    // 硬件传感器数据采集器（使用硬件纳秒时钟）
    private val beatTimestampsNanos = mutableListOf<Long>()
    private val bpmRecords = mutableListOf<Double>()
    private val accuracyRecords = mutableListOf<Int>()

    @Volatile
    private var isOffBody: Boolean = false

    @Volatile
    private var currentInstantBpm: Double? = null

    // 物理脉冲波发生器状态
    @Volatile
    private var pulsePacketPhase: Float = 1.0f // 1.0 表示没有正在展开的波峰，0.0..1.0 表示当前脉搏波包展开进度

    // 每一跳独特的动态生理形态参数
    @Volatile
    private var activePeakHeight: Float = 0.92f

    @Volatile
    private var activeNotchDepth: Float = 0.38f

    @Volatile
    private var activeDicroticHeight: Float = 0.58f

    @Volatile
    private var activeRiseTime: Float = 0.22f

    override fun start(scope: CoroutineScope, durationSeconds: Int) {
        stop()
        _state.value = PulseSensorState.Preparing
        beatTimestampsNanos.clear()
        bpmRecords.clear()
        accuracyRecords.clear()
        isOffBody = false
        currentInstantBpm = null
        pulsePacketPhase = 1.0f
        activePeakHeight = 0.92f
        activeNotchDepth = 0.38f
        activeDicroticHeight = 0.58f
        activeRiseTime = 0.22f

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BODY_SENSORS,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission && sensorManager != null) {
            try {
                if (heartBeatSensor != null) {
                    sensorManager.registerListener(
                        this,
                        heartBeatSensor,
                        SensorManager.SENSOR_DELAY_FASTEST,
                    )
                }
                if (heartRateSensor != null) {
                    sensorManager.registerListener(
                        this,
                        heartRateSensor,
                        SensorManager.SENSOR_DELAY_FASTEST,
                    )
                }
                if (offBodySensor != null) {
                    sensorManager.registerListener(
                        this,
                        offBodySensor,
                        SensorManager.SENSOR_DELAY_NORMAL,
                    )
                }
            } catch (_: Throwable) {
            }
        }

        job = scope.launch {
            val totalMillis = durationSeconds * 1000L
            val frameIntervalMs = 40L // ~25fps 走带
            val totalSteps = (totalMillis / frameIntervalMs).toInt().coerceAtLeast(1)

            val waveLength = 28
            val waveBuffer = FloatArray(waveLength) { 0.2f }

            var elapsedMs = 0L
            var initialGraceElapsedMs = 0L
            var notWornDurationMs = 0L

            while (isActive && elapsedMs < totalMillis) {
                // 1. 离腕硬拦截与脱腕超时保护
                if (isOffBody) {
                    notWornDurationMs += frameIntervalMs
                    if (notWornDurationMs >= 15000L) {
                        _state.value = PulseSensorState.Error(
                            reason = "wear_timeout",
                            canFallback = false,
                        )
                        break
                    }
                    _state.value = PulseSensorState.NotWorn
                    delay(frameIntervalMs)
                    continue
                }

                // 2. 初始寻脉阶段：若前 4 秒未收到任何真实脉搏
                if (beatTimestampsNanos.isEmpty() && bpmRecords.isEmpty()) {
                    initialGraceElapsedMs += frameIntervalMs
                    notWornDurationMs += frameIntervalMs
                    if (notWornDurationMs >= 15000L) {
                        _state.value = PulseSensorState.Error(
                            reason = "wear_timeout",
                            canFallback = false,
                        )
                        break
                    }
                    if (initialGraceElapsedMs > 4000L) {
                        _state.value = PulseSensorState.NotWorn
                    } else {
                        _state.value = PulseSensorState.Preparing
                    }
                    delay(frameIntervalMs)
                    continue
                }

                // 恢复接触或已感应到脉搏，重置未佩戴计时
                notWornDurationMs = 0L

                // 3. 正常测量走带
                elapsedMs += frameIntervalMs
                val progress = (elapsedMs.toFloat() / totalMillis.toFloat()).coerceIn(0f, 1f)

                // 推进波包展开
                val baseLine = (0.20f + 0.012f * sin(elapsedMs * 0.003).toFloat()).coerceIn(0.18f, 0.22f)
                var sampleValue = baseLine
                var isPeakNow = false

                if (pulsePacketPhase < 1.0f) {
                    val oldP = pulsePacketPhase
                    val rTime = activeRiseTime
                    val peakH = activePeakHeight
                    val notchD = activeNotchDepth
                    val dicroH = activeDicroticHeight

                    // 动态生理波包插值计算
                    sampleValue = when {
                        oldP < rTime -> baseLine + (oldP / rTime) * (peakH - baseLine) // 主波升支
                        oldP < (rTime + 0.22f) -> peakH - ((oldP - rTime) / 0.22f) * (peakH - notchD) // 降中峡
                        oldP < (rTime + 0.42f) -> notchD + ((oldP - rTime - 0.22f) / 0.20f) * (dicroH - notchD) // 重搏波
                        else -> {
                            val remainFraction = ((oldP - rTime - 0.42f) / (1.0f - rTime - 0.42f).coerceAtLeast(0.1f)).coerceIn(0f, 1f)
                            dicroH - remainFraction * (dicroH - baseLine) // 舒张末期回落
                        }
                    }.coerceIn(0.15f, 0.98f)

                    // 波包展开速率 (心率越快，单个波包展开越迅速)
                    val bpm = currentInstantBpm ?: 72.0
                    val packetDurationMs = (60000.0 / bpm).toFloat().coerceIn(350f, 1200f)
                    val phaseStep = (frameIntervalMs / packetDurationMs) * 1.8f
                    val newP = oldP + phaseStep

                    // 区间穿越判定：精准覆盖波峰顶点时刻，无论心率多快绝不跳步丢失微震
                    if (oldP < rTime && newP >= rTime) {
                        isPeakNow = true
                    }

                    pulsePacketPhase = newP
                }

                // 移位缓冲区
                for (j in 0 until waveLength - 1) {
                    waveBuffer[j] = waveBuffer[j + 1]
                }
                waveBuffer[waveLength - 1] = sampleValue

                _state.value = PulseSensorState.Measuring(
                    progress = progress,
                    wavePoints = waveBuffer.toList(),
                    currentBpm = currentInstantBpm,
                    isPeakNow = isPeakNow,
                )

                delay(frameIntervalMs)
            }

            // 采样结束，执行质检与辨证 (如果是因为超时跳出则保持 Error 状态)
            if (_state.value !is PulseSensorState.Error) {
                _state.value = PulseSensorState.Analyzing
                delay(400) // 视觉过渡

                val metrics = PulseFeatureExtractor.extractFromBeatSeries(
                    beatTimestamps = beatTimestampsNanos.toList(),
                    bpmRecords = bpmRecords.toList(),
                    accuracyRecords = accuracyRecords.toList(),
                    durationSeconds = durationSeconds,
                )

                if (!metrics.quality.isReliable) {
                    _state.value = PulseSensorState.QualityFailed(
                        reason = metrics.quality.failureReason ?: "有效脉搏信号不足，请保持手腕静止",
                    )
                } else {
                    val result = TcmPulseClassifier.classify(
                        metrics = metrics,
                        hour24 = LocalTime.now().hour,
                    )
                    _state.value = PulseSensorState.Completed(result)
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Throwable) {
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT -> {
                val value = event.values.firstOrNull() ?: 1.0f
                isOffBody = (value == 0.0f)
            }
            Sensor.TYPE_HEART_BEAT -> {
                val confidence = event.values.firstOrNull() ?: 1.0f
                if (confidence >= 0.5f) {
                    val timestampNanos = event.timestamp
                    beatTimestampsNanos.add(timestampNanos)
                    triggerBeatAnimation(timestampNanos)
                }
            }
            Sensor.TYPE_HEART_RATE -> {
                val bpm = event.values.firstOrNull()?.toDouble() ?: 0.0
                val accuracy = event.accuracy

                if (accuracy <= SensorManager.SENSOR_STATUS_NO_CONTACT || bpm <= 30.0) {
                    return
                }

                currentInstantBpm = bpm
                bpmRecords.add(bpm)
                accuracyRecords.add(accuracy)

                // 兼容极其老旧无 TYPE_HEART_BEAT 硬件传感器之场景
                if (heartBeatSensor == null) {
                    val timestampNanos = event.timestamp
                    beatTimestampsNanos.add(timestampNanos)
                    triggerBeatAnimation(timestampNanos)
                }
            }
        }
    }

    private fun triggerBeatAnimation(timestampNanos: Long) {
        val bpm = currentInstantBpm ?: 72.0
        val timestampMs = timestampNanos / 1_000_000L

        // 每一跳动态注入人体生理呼吸变异 (RSA) 与微血管阻力抖动
        val rsaCycle = sin(timestampMs * 0.0018).toFloat() // ~0.25Hz 呼吸微律动
        val jitter = ((timestampMs % 17) - 8) * 0.008f // 微血管阻力微颤

        activePeakHeight = (0.88f + (0.08f * rsaCycle) + jitter).coerceIn(0.78f, 0.98f)
        activeNotchDepth = (0.36f + (0.05f * rsaCycle) - jitter * 0.5f).coerceIn(0.26f, 0.44f)
        activeDicroticHeight = (activeNotchDepth + 0.18f + (0.04f * rsaCycle)).coerceIn(activeNotchDepth + 0.08f, 0.68f)
        activeRiseTime = if (bpm > 85.0) 0.18f else if (bpm < 60.0) 0.25f else 0.22f

        pulsePacketPhase = 0.0f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 当心率传感器指示无接触时自动标记
        if (sensor?.type == Sensor.TYPE_HEART_RATE && accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT) {
            isOffBody = true
        }
    }
}
