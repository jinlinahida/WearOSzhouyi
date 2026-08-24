package com.boompala.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.boompala.R
import com.boompala.archive.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ArchiveLoadingScreen(rotary: Boolean) {
    WearLoadingIndicator(label = stringResource(R.string.archive_loading))
}

@Composable
fun ArchiveListScreen(
    repo: ArchiveRepository,
    rotary: Boolean,
    refreshToken: Int = 0,
    onSelect: (Long) -> Unit,
    onBack: () -> Unit,
) {
    var source by remember { mutableStateOf<ArchiveSource?>(null) }
    var color by remember { mutableStateOf<Long?>(null) }
    var records by remember { mutableStateOf<List<ArchiveRecord>?>(null) }

    LaunchedEffect(source, color, refreshToken) {
        records = null
        records = withContext(Dispatchers.IO) { repo.list(source, color) }
    }
    val m = LocalUiMetrics.current

    RotaryScrollColumn(
        rotaryEnabled = rotary,
        modifier = Modifier.fillMaxSize(),
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            Text(stringResource(R.string.archive_title), style = MaterialTheme.typography.titleLarge)
        }
        item {
            Text(stringResource(R.string.archive_category_title), style = MaterialTheme.typography.labelMedium)
            Column {
                listOf(
                    null to stringResource(R.string.archive_category_all),
                    ArchiveSource.LIU_YAO to stringResource(R.string.home_feature_six_yao),
                    ArchiveSource.MEI_HUA to stringResource(R.string.home_feature_mei_hua),
                    ArchiveSource.XIAO_LIU_REN to stringResource(R.string.home_feature_xiao_liu_ren),
                    ArchiveSource.TAROT to stringResource(R.string.browse_tarot_title),
                ).forEach { (s, t) ->
                    OutlinedButton(onClick = { source = s }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (source == s) "● $t" else t)
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.archive_color_title), style = MaterialTheme.typography.labelMedium)
            Column {
                listOf(
                    null to stringResource(R.string.archive_color_all),
                    0xFF4CAF50L to stringResource(R.string.archive_color_green),
                    0xFF2196F3L to stringResource(R.string.archive_color_blue),
                    0xFFFF9800L to stringResource(R.string.archive_color_orange),
                    0xFFE91E63L to stringResource(R.string.archive_color_pink),
                ).forEach { (c, t) ->
                    OutlinedButton(onClick = { color = c }, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (c != null) {
                                Box(Modifier.padding(end = 8.dp).size(10.dp).background(Color(c), RoundedCornerShape(50)))
                            }
                            Text(if (color == c) "● $t" else t)
                        }
                    }
                }
            }
        }

        if (records == null) {
            item {
                WearLoadingIndicator(label = stringResource(R.string.archive_loading))
            }
        } else if (records.orEmpty().isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.archive_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(records.orEmpty(), key = { it.id }) { r ->
            OutlinedButton(onClick = { onSelect(r.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    if (r.color != 0L) {
                        Box(Modifier.padding(end = 8.dp).size(10.dp).background(Color(r.color), RoundedCornerShape(50)))
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        Text(r.name, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${r.source.displayName} · ${r.summary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back_home))
            }
        }
    }
}

@Composable
fun ArchiveDetailScreen(
    record: ArchiveRecord,
    repo: ArchiveRepository,
    rotary: Boolean,
    onChanged: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var edit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(record.name) }
    var note by remember { mutableStateOf(record.note) }
    var color by remember { mutableLongStateOf(record.color) }
    val snapshot = remember(record.snapshotJson) { ArchiveSnapshotCodec.decode(record.snapshotJson).getOrNull() }
    val m = LocalUiMetrics.current
    val castAtText = remember(record.castAt) {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(record.castAt))
    }

    RotaryScrollColumn(
        rotaryEnabled = rotary,
        modifier = Modifier.fillMaxSize(),
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item {
            ResultCard {
                Text(record.name, style = MaterialTheme.typography.titleMedium)
                DetailField(stringResource(R.string.archive_field_summary), "${record.source.displayName} · ${record.summary}")
                DetailField(stringResource(R.string.archive_field_cast_time), castAtText)
                if (record.note.isNotBlank()) {
                    DetailField(stringResource(R.string.archive_field_note), record.note)
                }
            }
        }
        if (snapshot == null) {
            item { Text(stringResource(R.string.archive_corrupt_tip)) }
        } else {
            snapshot.sections.forEach { (title, lines) ->
                item {
                    ResultCard {
                        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        lines.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { edit = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.archive_action_edit))
            }
        }
        item {
            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.archive_action_delete), color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_back))
            }
        }
    }

    if (edit) {
        ArchiveEditDialog(
            name = name,
            note = note,
            color = color,
            onName = { name = it },
            onNote = { note = it },
            onColor = { color = it },
            onSave = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repo.update(record.id, name, note, color)
                    }
                    edit = false
                    onChanged()
                }
            },
            onCancel = { edit = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            visible = true,
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.archive_delete_dialog_title)) },
            text = { Text(stringResource(R.string.archive_delete_dialog_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) { repo.delete(record.id) }
                            confirmDelete = false
                            onChanged()
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ArchiveEditDialog(
    name: String,
    note: String,
    color: Long,
    onName: (String) -> Unit,
    onNote: (String) -> Unit,
    onColor: (Long) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        visible = true,
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.archive_edit_dialog_title)) },
        text = {
            Column {
                ArchiveInput(stringResource(R.string.archive_tag_name_required), name, onName)
                ArchiveInput(stringResource(R.string.archive_tag_note_optional), note, onNote)
                Text(stringResource(R.string.archive_color_title))
                Column {
                    listOf(
                        0xFF4CAF50L to stringResource(R.string.archive_color_green),
                        0xFF2196F3L to stringResource(R.string.archive_color_blue),
                        0xFFFF9800L to stringResource(R.string.archive_color_orange),
                        0xFFE91E63L to stringResource(R.string.archive_color_pink),
                    ).forEach { (c, label) ->
                        OutlinedButton(onClick = { onColor(c) }, modifier = Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.padding(end = 8.dp).size(10.dp).background(Color(c), RoundedCornerShape(50)))
                                Text(if (c == color) "● $label" else label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = onSave) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ArchiveInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = Color(0xFFBDBDBD), style = MaterialTheme.typography.labelSmall)
        Box(Modifier.fillMaxWidth().padding(top = 3.dp).border(1.dp, Color(0xFF777777), RoundedCornerShape(6.dp))) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                textStyle = TextStyle(color = Color.White),
            )
        }
    }
}

@Composable
fun ArchiveTagScreen(
    draft: ArchiveDraft,
    repo: ArchiveRepository,
    rotary: Boolean,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var color by remember { mutableLongStateOf(0xFF4CAF50L) }
    var saving by remember { mutableStateOf(false) }
    val m = LocalUiMetrics.current

    RotaryScrollColumn(
        rotaryEnabled = rotary,
        modifier = Modifier.fillMaxSize(),
        contentPadding = m.screenPadding,
        itemSpacing = m.itemSpacing,
    ) {
        item { Text(stringResource(R.string.archive_tag_title), style = MaterialTheme.typography.titleMedium) }
        item { Text(stringResource(R.string.archive_tag_summary_prefix, draft.summary), style = MaterialTheme.typography.bodySmall) }
        item { ArchiveInput(stringResource(R.string.archive_tag_name_required), name) { name = it } }
        item { ArchiveInput(stringResource(R.string.archive_tag_note_optional), note) { note = it } }
        item {
            Text(stringResource(R.string.archive_color_title), style = MaterialTheme.typography.labelMedium)
            Column {
                listOf(
                    0xFF4CAF50L to stringResource(R.string.archive_color_green),
                    0xFF2196F3L to stringResource(R.string.archive_color_blue),
                    0xFFFF9800L to stringResource(R.string.archive_color_orange),
                    0xFFE91E63L to stringResource(R.string.archive_color_pink),
                ).forEach { (c, label) ->
                    OutlinedButton(onClick = { color = c }, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.padding(end = 8.dp).size(10.dp).background(Color(c), RoundedCornerShape(50)))
                            Text(if (c == color) "● $label" else label)
                        }
                    }
                }
            }
        }
        item {
            Button(
                enabled = name.isNotBlank() && !saving,
                onClick = {
                    if (!saving) {
                        saving = true
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repo.insert(draft.copy(name = name.trim(), note = note, color = color))
                            }
                            onSaved()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.archive_save_btn))
            }
        }
        item {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}
