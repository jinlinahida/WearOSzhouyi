package com.boompala.pulse

import android.content.Context

/**
 * 把脉测量数据源工厂。
 * 直接装载 Wear OS 标准硬件传感器数据源，以真实硬件脉搏事件与 HRV 算法驱动把脉，零假数据、零不必要的弹窗拦截。
 */
object PulseDataSourceFactory {

    /**
     * 创建最优数据源。
     */
    fun createDataSource(
        context: Context,
        forceStandard: Boolean = false,
    ): PulseSensorDataSource {
        return StandardPulseDataSource(context)
    }
}
