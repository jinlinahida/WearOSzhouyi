package com.boompala.pulse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

/**
 * 脉搏测量底层数据源统一抽象接口。
 */
interface PulseSensorDataSource {
    /** 当前数据源状态流 */
    val state: StateFlow<PulseSensorState>

    /** 是否为 Galaxy Watch 原生高精 PPG 真实波形源 */
    val isRawPpg: Boolean

    /**
     * 启动采样。
     * @param scope 协程作用域
     * @param durationSeconds 持续采样时长（默认 20 秒）
     */
    fun start(scope: CoroutineScope, durationSeconds: Int = 20)

    /**
     * 停止采样并释放传感器资源。
     */
    fun stop()
}
