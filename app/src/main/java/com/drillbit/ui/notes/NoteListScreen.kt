package com.drillbit.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.DBCard
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarAction
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P12 笔记列表（Tab3）：标题 + 来源标注 + 时间，归纳稿卡片带主色左条 */
@Composable
fun NoteListScreen(state: NoteListUiState, onEvent: (NoteListEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "笔记",
                actions = {
                    DbTopBarAction(
                        text = if (state.summarizing) "梳理中…" else "梳理",
                        onClick = { onEvent(NoteListEvent.Summarize) },
                        enabled = !state.summarizing,
                    )
                    Spacer(Modifier.width(8.dp))
                    DbTopBarAction(
                        text = "备份",
                        onClick = { onEvent(NoteListEvent.Backup) },
                    )
                },
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = state.summaryText,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text2,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    state.items.forEach { note ->
                        NoteRow(
                            note = note,
                            onClick = { onEvent(NoteListEvent.NoteClick(note.noteId)) },
                        )
                    }
                    // 为右下角新建按钮留出空间，避免遮挡最后一条笔记
                    Spacer(Modifier.height(80.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                ) {
                    NewNoteButton(onClick = { onEvent(NoteListEvent.NewNote) })
                }
            }
        }
    }
}

/** 笔记卡片：标题 + 来源标注；归纳稿卡片左侧主色竖条 */
@Composable
private fun NoteRow(note: NoteCard, onClick: () -> Unit) {
    val colors = dbColors()
    DBCard(
        modifier = Modifier.padding(bottom = 10.dp),
        accent = note.isDigest,
        onClick = onClick,
    ) {
        Text(
            text = note.title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = note.sourceText,
            style = MaterialTheme.typography.labelSmall,
            color = colors.text2,
        )
    }
}

/** 新建笔记入口（右下角圆形按钮） */
@Composable
private fun NewNoteButton(onClick: () -> Unit) {
    val colors = dbColors()
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "新建笔记",
            tint = colors.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 预览假数据：含题目来源与归纳稿 */
private fun previewState() = NoteListUiState(
    items = listOf(
        NoteCard(
            noteId = "note-1",
            title = "自注意力为何是 O(n²)",
            sourceText = "来自题目 · 大模型基础 · 09-28",
            isDigest = false,
        ),
        NoteCard(
            noteId = "note-2",
            title = "ReAct 范式：思考与行动交替",
            sourceText = "来自 AI 问答 · 09-27",
            isDigest = false,
        ),
        NoteCard(
            noteId = "note-3",
            title = "LoRA 与全量微调的取舍",
            sourceText = "来自题目 · 模型微调与部署 · 09-26",
            isDigest = false,
        ),
        NoteCard(
            noteId = "note-4",
            title = "知识点归纳稿：Transformer 与注意力",
            sourceText = "大模型梳理生成 · 含 6 个主题 · 09-25",
            isDigest = true,
        ),
        NoteCard(
            noteId = "note-5",
            title = "KV Cache 与显存占用的关系",
            sourceText = "来自 AI 问答 · 09-24",
            isDigest = false,
        ),
    ),
    summaryText = "共 23 条 · 上次备份 09-28 21:10",
    summarizing = false,
)

/** 预览假数据：空列表 */
private fun previewEmptyState() = NoteListUiState(
    items = emptyList(),
    summaryText = "共 0 条 · 尚未备份",
    summarizing = false,
)

/** 预览假数据：梳理生成中 */
private fun previewSummarizingState() = previewState().copy(summarizing = true)

@Preview(name = "笔记列表 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteListPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记列表 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteListPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteListScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记列表 · 空列表 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteListEmptyPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteListScreen(state = previewEmptyState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记列表 · 梳理中 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteListSummarizingPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteListScreen(state = previewSummarizingState(), onEvent = {})
        }
    }
}
