package com.boompala.archive

enum class ArchiveSource(val displayName: String) {
    LIU_YAO("六爻"),
    MEI_HUA("时间起卦"),
    XIAO_LIU_REN("小六壬"),
    TAROT("塔罗"),
}

data class ArchiveRecord(
    val id: Long,
    val name: String,
    val note: String,
    val color: Long,
    val source: ArchiveSource,
    val castAt: Long,
    val archivedAt: Long,
    val summary: String,
    val snapshotJson: String,
    val schemaVersion: Int,
)

data class ArchiveDraft(
    val name: String,
    val note: String,
    val color: Long,
    val source: ArchiveSource,
    val castAt: Long,
    val summary: String,
    val snapshotJson: String,
)

data class ArchiveSnapshot(val version: Int, val source: ArchiveSource, val title: String, val sections: Map<String, List<String>>)
