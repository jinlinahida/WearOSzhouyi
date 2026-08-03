package com.boompala.engine.xiaoliuren

import com.boompala.engine.calendar.SixTailGanzhiCalendar
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class XiaoLiuRenEngineTest {
    @Test fun fixedInput_andCycleBoundaries() {
        val calendar = SixTailGanzhiCalendar()
        val info = calendar.divinationTimeInfo(Instant.parse("2024-03-13T07:08:00Z"), ZoneId.of("Asia/Shanghai"))
        val reading = XiaoLiuRenEngine(calendar).calculate(info)
        // lunar 2/4, 辰时=5: 留连 -> 小吉 -> 大安
        assertEquals(2, info.lunarMonth)
        assertEquals(4, info.lunarDay)
        assertEquals(XiaoLiuRenPalace.LIU_LIAN, reading.monthPalace)
        assertEquals(XiaoLiuRenPalace.XIAO_JI, reading.dayPalace)
        assertEquals(XiaoLiuRenPalace.DA_AN, reading.finalPalace)
        assertEquals(XiaoLiuRenPalace.DA_AN, XiaoLiuRenEngineTestSupport.palace(6))
    }
}

private object XiaoLiuRenEngineTestSupport {
    fun palace(n: Int) = XiaoLiuRenPalace.entries[n % 6]
}
