package com.drillbit.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DBTextField
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarAction
import com.drillbit.ui.components.DbTopBarInfo
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

/** P13 笔记详情 / 编辑：标题与正文可改，保留来源题目跳转与删除入口 */
@Composable
fun NoteEditScreen(state: NoteEditUiState, onEvent: (NoteEditEvent) -> Unit) {
    val colors = dbColors()
    // 查看/编辑双态：已有笔记默认查看态（正文 Markdown 渲染），新建直接进编辑态；
    // 切换为页面内本地状态，不进契约（同保存弹窗本地编辑的先例）
    var editing by remember(state.noteId) { mutableStateOf(state.noteId == "new") }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "笔记",
                onBack = { onEvent(NoteEditEvent.Back) },
                actions = {
                    if (editing) {
                        DbTopBarAction(text = "完成", onClick = { editing = false })
                    } else {
                        DbTopBarAction(text = "编辑", onClick = { editing = true })
                    }
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                if (editing) {
                    DBTextField(
                        label = "标题",
                        value = state.title,
                        onChange = { text -> onEvent(NoteEditEvent.TitleChange(text)) },
                    )
                    Spacer(Modifier.height(11.dp))
                    DBTextField(
                        label = "正文",
                        value = state.content,
                        onChange = { text -> onEvent(NoteEditEvent.ContentChange(text)) },
                        singleLine = false,
                        minHeight = 150.dp,
                    )
                } else {
                    // 查看态：正文为模型/手写的 Markdown，富渲染呈现
                    Text(
                        text = state.title.ifBlank { "无标题笔记" },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text,
                    )
                    Spacer(Modifier.height(10.dp))
                    Markdown(
                        content = state.content.ifBlank { "（空）" }.replace(Regex("\\n{3,}"), "\\n\\n"),
                        typography = markdownTypography(
                            text = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                        ),
                    )
                }
                Spacer(Modifier.height(8.dp))
                state.sourceQuestionText?.let { source ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEvent(NoteEditEvent.SourceClick) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "来源题目",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.text2,
                        )
                        Text(
                            text = source,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.text2,
                        )
                    }
                    Text(
                        text = "点击来源题目可跳回该题查看完整解析",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = "删除这条笔记",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.bad,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEvent(NoteEditEvent.DeleteClick) }
                        .padding(vertical = 12.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            if (editing) {
                HorizontalDivider(thickness = 1.dp, color = colors.line)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
                ) {
                    DBButton(
                        text = "取消",
                        onClick = { editing = false },
                        modifier = Modifier.weight(1f),
                        type = DBButtonType.GHOST,
                    )
                    Spacer(Modifier.width(9.dp))
                    DBButton(
                        text = "保存修改",
                        onClick = {
                            onEvent(NoteEditEvent.Save)
                            editing = false
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.deleteConfirmVisible) {
            DeleteNoteDialog(onEvent = onEvent)
        }
    }
}

/** 删除笔记二次确认弹窗 */
@Composable
private fun DeleteNoteDialog(onEvent: (NoteEditEvent) -> Unit) {
    val colors = dbColors()
    ScrimModal {
        Text(
            text = "删除这条笔记",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "删除后无法恢复。",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(NoteEditEvent.DeleteCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "删除",
                onClick = { onEvent(NoteEditEvent.DeleteConfirm) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.WARN,
            )
        }
    }
}

/** 预览假数据：编辑来自题目的笔记 */
private fun previewState() = NoteEditUiState(
    noteId = "note-1",
    title = "自注意力为何是 O(n²)",
    content = "注意力分数矩阵 QKᵀ 的形状是 n×n，这是平方复杂度的根源。\n\n" +
        "序列长度翻倍，矩阵元素数量翻四倍，显存与计算量同步翻四倍。\n\n" +
        "长上下文的优化方向都在绕开这个 n×n 矩阵。",
    sourceQuestionText = "大模型基础 · 第 12 题",
    deleteConfirmVisible = false,
)

/** 预览假数据：新建笔记（无来源题目） */
private fun previewNewState() = NoteEditUiState(
    noteId = "new",
    title = "",
    content = "",
    sourceQuestionText = null,
    deleteConfirmVisible = false,
)

@Preview(name = "笔记编辑 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteEditPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteEditScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记编辑 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteEditPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteEditScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "笔记编辑 · 删除确认 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteEditDeletePreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteEditScreen(state = previewState().copy(deleteConfirmVisible = true), onEvent = {})
        }
    }
}

@Preview(name = "笔记编辑 · 新建 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun NoteEditNewPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            NoteEditScreen(state = previewNewState(), onEvent = {})
        }
    }
}
