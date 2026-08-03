package com.boompala.archive

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "boompa_archives.db"
private const val DB_VERSION = 2

class ArchiveDatabase(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE archives (
            id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, note TEXT NOT NULL,
            color INTEGER NOT NULL, source TEXT NOT NULL, cast_at INTEGER NOT NULL,
            archived_at INTEGER NOT NULL, summary TEXT NOT NULL, snapshot_json TEXT NOT NULL,
            schema_version INTEGER NOT NULL)""")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) onCreate(db)
        if (oldVersion < 2) db.execSQL("CREATE INDEX IF NOT EXISTS idx_archives_cast_at ON archives(cast_at DESC)")
    }
}

class ArchiveRepository(context: Context) {
    private val helper = ArchiveDatabase(context.applicationContext)
    fun insert(draft: ArchiveDraft): Long = helper.writableDatabase.let { db ->
        val values = android.content.ContentValues().apply {
            put("name", draft.name); put("note", draft.note); put("color", draft.color)
            put("source", draft.source.name); put("cast_at", draft.castAt)
            put("archived_at", System.currentTimeMillis()); put("summary", draft.summary)
            put("snapshot_json", draft.snapshotJson); put("schema_version", 1)
        }
        db.insertOrThrow("archives", null, values)
    }
    fun list(source: ArchiveSource? = null, color: Long? = null): List<ArchiveRecord> {
        val where = buildList { if (source != null) add("source=?"); if (color != null) add("color=?") }
        val args = buildList { if (source != null) add(source.name); if (color != null) add(color.toString()) }
        helper.readableDatabase.query("archives", null, where.joinToString(" AND ").ifEmpty { null }, args.toTypedArray().takeIf { it.isNotEmpty() }, null, null, "cast_at DESC").use { c ->
            return buildList { while (c.moveToNext()) runCatching { add(c.toRecord()) } }
        }
    }
    fun get(id: Long): ArchiveRecord? = list().firstOrNull { it.id == id }
    fun update(id: Long, name: String, note: String, color: Long): Int = helper.writableDatabase.update("archives", android.content.ContentValues().apply { put("name", name); put("note", note); put("color", color) }, "id=?", arrayOf(id.toString()))
    fun delete(id: Long): Int = helper.writableDatabase.delete("archives", "id=?", arrayOf(id.toString()))
    private fun android.database.Cursor.toRecord() = ArchiveRecord(getLong(0), getString(1), getString(2), getLong(3), ArchiveSource.valueOf(getString(4)), getLong(5), getLong(6), getString(7), getString(8), getInt(9))
}
