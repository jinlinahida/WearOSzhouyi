package com.boompala.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.wear.compose.material3.*
import com.boompala.archive.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable fun ArchiveLoadingScreen(rotary: Boolean) {
    val m = LocalUiMetrics.current
    RotaryScrollColumn(rotaryEnabled = rotary, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("正在加载归档…") }
    }
}

@Composable fun ArchiveListScreen(repo: ArchiveRepository, rotary: Boolean, refreshToken: Int = 0, onSelect: (Long)->Unit, onBack: ()->Unit) {
    var source by remember { mutableStateOf<ArchiveSource?>(null) }; var color by remember { mutableStateOf<Long?>(null) }
    var records by remember { mutableStateOf<List<ArchiveRecord>?>(null) }
    LaunchedEffect(source, color, refreshToken) {
        records = null
        records = withContext(Dispatchers.IO) { repo.list(source, color) }
    }
    val m = LocalUiMetrics.current
    RotaryScrollColumn(rotaryEnabled = rotary, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(m.horizontalPadding, m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("归档") }
        item { Text("起卦方式"); Column { listOf(null to "全部", ArchiveSource.LIU_YAO to "六爻", ArchiveSource.MEI_HUA to "时间起卦", ArchiveSource.XIAO_LIU_REN to "小六壬").forEach { (s,t) -> OutlinedButton(onClick={source=s}, modifier=Modifier.fillMaxWidth()) { Text(if(source==s) "● $t" else t) } } } }
        item { Text("分类颜色"); Column { listOf(null to "全部颜色", 0xFF4CAF50L to "绿色", 0xFF2196F3L to "蓝色", 0xFFFF9800L to "橙色", 0xFFE91E63L to "粉色").forEach { (c,t) -> OutlinedButton(onClick={color=c}, modifier=Modifier.fillMaxWidth()) { Row { if (c != null) androidx.compose.foundation.layout.Box(Modifier.padding(end=8.dp).size(10.dp).background(Color(c), RoundedCornerShape(50))); Text(if(color==c) "● $t" else t) } } } } }
        if (records == null) item { Text("正在加载归档…") }
        items(records.orEmpty(), key={it.id}) { r -> OutlinedButton(onClick={ onSelect(r.id) }, modifier=Modifier.fillMaxWidth()) { Row { androidx.compose.foundation.layout.Box(Modifier.padding(end=8.dp).size(10.dp).background(Color(r.color), RoundedCornerShape(50))); Text("${r.name}\n${r.source.displayName} · ${r.summary}") } } }
        item { OutlinedButton(onClick=onBack, modifier=Modifier.fillMaxWidth()) { Text("返回首页") } }
    }
}

@Composable fun ArchiveDetailScreen(record: ArchiveRecord, repo: ArchiveRepository, rotary: Boolean, onChanged: ()->Unit, onBack: ()->Unit) {
    var edit by remember { mutableStateOf(false) }; var confirmDelete by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(record.name) }; var note by remember { mutableStateOf(record.note) }; var color by remember { mutableLongStateOf(record.color) }
    val snapshot = remember(record.snapshotJson) { ArchiveSnapshotCodec.decode(record.snapshotJson).getOrNull() }; val m=LocalUiMetrics.current
    RotaryScrollColumn(rotaryEnabled=rotary, modifier=Modifier.fillMaxSize(), contentPadding=PaddingValues(m.horizontalPadding,m.verticalPadding), itemSpacing=m.itemSpacing) {
        item { Text(record.name) }; item { Text("${record.source.displayName} · ${record.summary}") }; item { Text("起卦时间：${DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(record.castAt))}") }; item { Text("备注：${record.note.ifBlank { "无" }}") }
        if (snapshot == null) item { Text("此归档快照损坏或版本不受支持，原始记录仍已保留。") } else snapshot.sections.forEach { (title, lines) -> item { ResultCard { Text(title); lines.forEach { Text(it) } } } }
        item { OutlinedButton(onClick={edit=true},modifier=Modifier.fillMaxWidth()){Text("修改标记")} }; item { OutlinedButton(onClick={confirmDelete=true},modifier=Modifier.fillMaxWidth()){Text("删除归档")} }; item { OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("返回归档列表")} }
    }
    if (edit) ArchiveEditDialog(name,note,color,{name=it},{note=it},{color=it},{ repo.update(record.id,name,note,color); edit=false; onChanged() },{edit=false})
    if (confirmDelete) AlertDialog(visible=true,onDismissRequest={confirmDelete=false}, title={Text("确认删除？")}, text={Text("删除后不可恢复")}, confirmButton={Button(onClick={repo.delete(record.id);confirmDelete=false;onChanged()}){Text("删除")}}, dismissButton={OutlinedButton(onClick={confirmDelete=false}){Text("取消")}})
}

@Composable private fun ArchiveEditDialog(name:String,note:String,color:Long,onName:(String)->Unit,onNote:(String)->Unit,onColor:(Long)->Unit,onSave:()->Unit,onCancel:()->Unit) {
    AlertDialog(visible=true,onDismissRequest=onCancel,title={Text("修改归档")},text={Column { ArchiveInput("标记名称", name, onName); ArchiveInput("备注", note, onNote); Text("分类颜色"); Column { listOf(0xFF4CAF50L to "绿色",0xFF2196F3L to "蓝色",0xFFFF9800L to "橙色",0xFFE91E63L to "粉色").forEach { (c,label)->OutlinedButton(onClick={onColor(c)},modifier=Modifier.fillMaxWidth()){Row { androidx.compose.foundation.layout.Box(Modifier.padding(end=8.dp).size(10.dp).background(Color(c), RoundedCornerShape(50))); Text(if(c==color)"● $label" else label) }} } } }},confirmButton={Button(enabled=name.isNotBlank(),onClick=onSave){Text("保存")}},dismissButton={OutlinedButton(onClick=onCancel){Text("取消")}})
}

@Composable private fun ArchiveInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        Text(label, color = Color(0xFFBDBDBD))
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().padding(top=3.dp).border(1.dp, Color(0xFF777777), RoundedCornerShape(6.dp))) {
            BasicTextField(value=value,onValueChange=onValueChange, modifier=Modifier.fillMaxWidth().padding(10.dp), textStyle=TextStyle(color=Color.White))
        }
    }
}

@Composable fun ArchiveTagScreen(draft: ArchiveDraft, repo: ArchiveRepository, rotary: Boolean, onSaved: ()->Unit, onBack: ()->Unit) {
    var name by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; var color by remember { mutableLongStateOf(0xFF4CAF50) }; var saving by remember { mutableStateOf(false) }; val m=LocalUiMetrics.current
    RotaryScrollColumn(rotaryEnabled = rotary, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(m.horizontalPadding,m.verticalPadding), itemSpacing = m.itemSpacing) {
        item { Text("归档此次结果") }; item { Text("摘要：${draft.summary}") }
        item { ArchiveInput("标记名称（必填）", name) { name=it } }
        item { ArchiveInput("备注（可选）", note) { note=it } }
        item { Text("分类颜色"); Column { listOf(0xFF4CAF50L to "绿色",0xFF2196F3L to "蓝色",0xFFFF9800L to "橙色",0xFFE91E63L to "粉色").forEach { (c,label) -> OutlinedButton(onClick={color=c}, modifier=Modifier.fillMaxWidth()) { Row { androidx.compose.foundation.layout.Box(Modifier.padding(end=8.dp).size(10.dp).background(Color(c), RoundedCornerShape(50))); Text(if(c==color) "● $label" else label) } } } } }
        item { Button(enabled=name.isNotBlank()&&!saving,onClick={if(!saving){saving=true;repo.insert(draft.copy(name=name.trim(),note=note,color=color));onSaved()}},modifier=Modifier.fillMaxWidth()){Text("保存归档")} }
        item { OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth()){Text("取消") } }
    }
}
