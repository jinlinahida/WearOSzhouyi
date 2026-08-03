package com.boompala.archive

import com.boompala.engine.meihua.MeiHuaTimeReading
import com.boompala.engine.data.HexagramInterpretationRepository
import com.boompala.engine.model.DivinationResult
import com.boompala.engine.xiaoliuren.XiaoLiuRenReading
import com.google.gson.Gson
import com.google.gson.JsonParseException

object ArchiveSnapshotCodec {
    private val gson = Gson()
    fun encode(result: DivinationResult): String = gson.toJson(ArchiveSnapshot(1, ArchiveSource.LIU_YAO, result.original.name, mapOf(
        "时间" to listOf(result.timeInfo.gregorianDateTime.toString(), result.timeInfo.lunarDate),
        "本卦" to listOf(result.original.name, result.original.pattern.codeFromBottom),
        "变卦" to listOfNotNull(result.changed?.name, result.changed?.pattern?.codeFromBottom),
        "装卦" to result.yaoFromBottom.sortedByDescending { it.position.indexFromBottom }.map { "${it.position.displayName} ${it.yinYang.displayName} ${it.heavenlyStem.displayName}${it.earthlyBranch.displayName} ${it.sixRelation.displayName} ${it.sixSpirit.displayName} ${it.lineText.orEmpty()}" },
        "解释" to listOf("保存时的离线六爻解释快照"))))
    fun encode(result: DivinationResult, interpretations: HexagramInterpretationRepository): String = gson.toJson(ArchiveSnapshot(1, ArchiveSource.LIU_YAO, result.original.name, mapOf(
        "时间" to listOf(result.timeInfo.gregorianDateTime.toString(), result.timeInfo.lunarDate),
        "本卦" to listOf(result.original.name, result.original.pattern.codeFromBottom, interpretations.interpretationFor(result.original.pattern.codeFromBottom).toString()),
        "变卦" to listOfNotNull(result.changed?.name, result.changed?.pattern?.codeFromBottom, result.changed?.let { interpretations.interpretationFor(it.pattern.codeFromBottom).toString() }),
        "装卦" to result.yaoFromBottom.sortedByDescending { it.position.indexFromBottom }.map { "${it.position.displayName} ${it.yinYang.displayName} ${it.heavenlyStem.displayName}${it.earthlyBranch.displayName} ${it.sixRelation.displayName} ${it.sixSpirit.displayName} ${it.lineText.orEmpty()}" },
        "解释" to listOf("保存时离线解释已写入本快照"))))
    fun encode(result: MeiHuaTimeReading): String = gson.toJson(ArchiveSnapshot(1, ArchiveSource.MEI_HUA, result.original.name, mapOf(
        "时间" to listOf(result.timeInfo.gregorianDateTime.toString(), result.timeInfo.lunarDate),
        "本卦" to listOf(result.original.name), "互卦" to listOf(result.mutual.name), "变卦" to listOf(result.changed.name),
        "体用" to listOf("体：${result.bodyTrigram.displayName}", "用：${result.useTrigram.displayName}"), "动爻" to listOf(result.movingPosition.displayName),
        "解释" to listOf("保存时的离线梅花易数解释快照"))))
    fun encode(result: MeiHuaTimeReading, interpretations: HexagramInterpretationRepository): String = gson.toJson(ArchiveSnapshot(1, ArchiveSource.MEI_HUA, result.original.name, mapOf(
        "时间" to listOf(result.timeInfo.gregorianDateTime.toString(), result.timeInfo.lunarDate), "本卦" to listOf(result.original.name, interpretations.interpretationFor(result.original.codeFromBottom).toString()), "互卦" to listOf(result.mutual.name, interpretations.interpretationFor(result.mutual.codeFromBottom).toString()), "变卦" to listOf(result.changed.name, interpretations.interpretationFor(result.changed.codeFromBottom).toString()), "体用" to listOf("体：${result.bodyTrigram.displayName}", "用：${result.useTrigram.displayName}"), "动爻" to listOf(result.movingPosition.displayName), "解释" to listOf("保存时离线解释已写入本快照"))))
    fun encode(result: XiaoLiuRenReading): String = gson.toJson(ArchiveSnapshot(1, ArchiveSource.XIAO_LIU_REN, result.finalPalace.displayName, mapOf(
        "时间" to listOf(result.timeInfo.gregorianDateTime.toString(), result.timeInfo.lunarDate),
        "起课" to listOf("月${result.timeInfo.lunarMonth}：${result.monthPalace.displayName}", "日${result.timeInfo.lunarDay}：${result.dayPalace.displayName}", "时${result.timeInfo.hourGanzhi.earthlyBranch.index + 1}：${result.hourPalace.displayName}"),
        "六宫" to result.palaceCycle.map { it.displayName }, "最终" to listOf(result.finalPalace.displayName, result.finalPalace.meaning))))
    fun decode(json: String): Result<ArchiveSnapshot> = runCatching {
        val snapshot = gson.fromJson(json, ArchiveSnapshot::class.java) ?: error("empty snapshot")
        if (snapshot.version != 1) error("unknown snapshot version ${snapshot.version}")
        snapshot
    }
}
