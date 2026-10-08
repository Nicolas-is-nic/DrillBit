package com.drillbit.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.components.Banner
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.ChatBubble
import com.drillbit.ui.components.ChatRole
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DBTextField
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.components.DbTopBarInfo
import com.drillbit.ui.components.ScrimModal
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/**
 * P10 / P11 AI 问答：从题目进入时自动带入题干、选项、解析作为上下文，
 * 回答下方可一键保存到笔记（P11 弹层）。
 */
@Composable
fun ChatScreen(state: ChatUiState, onEvent: (ChatEvent) -> Unit) {
    val colors = dbColors()
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "AI 问答",
                onBack = { onEvent(ChatEvent.Back) },
                actions = { DbTopBarInfo(text = state.modelName) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp),
            ) {
                // 上下文块仅在 contextSummary 非空时展示（契约 7.6）
                if (state.contextSummary.isNotBlank()) {
                    ContextBlock(summary = state.contextSummary)
                }
                if (state.messages.isEmpty()) {
                    Text(
                        text = "直接提问，回答有启发时点「保存到笔记」沉淀下来",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.text2,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                state.messages.forEach { message ->
                    ChatBubble(
                        role = message.role,
                        text = message.text,
                        modifier = Modifier.padding(bottom = 10.dp),
                        streaming = message.streaming,
                        showSave = message.showSave,
                        onSave = { onEvent(ChatEvent.SaveClick(message.id)) },
                    )
                }
                state.errorBannerText?.let { errorText ->
                    Banner(
                        text = errorText,
                        modifier = Modifier.padding(top = 2.dp),
                        type = BannerType.WARN,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            HorizontalDivider(thickness = 1.dp, color = colors.line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
            ) {
                DBTextField(
                    label = "",
                    value = state.input,
                    onChange = { text -> onEvent(ChatEvent.InputChange(text)) },
                    modifier = Modifier.weight(1f),
                    placeholder = "继续追问",
                )
                Spacer(Modifier.width(9.dp))
                DBButton(
                    text = "发送",
                    onClick = { onEvent(ChatEvent.Send) },
                    modifier = Modifier.width(64.dp),
                    enabled = state.input.isNotBlank() && !state.sending,
                )
            }
        }
        state.saveDialog?.let { dialog ->
            SaveNoteDialog(dialog = dialog, onEvent = onEvent)
        }
    }
}

/** 上下文说明块：明确告诉用户这次提问带入了哪些内容 */
@Composable
private fun ContextBlock(summary: String) {
    val colors = dbColors()
    val shape = RoundedCornerShape(11.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 11.dp)
            .clip(shape)
            .background(colors.card2)
            .border(1.dp, colors.line, shape)
            .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Text(
            text = "已带入上下文",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = colors.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = summary,
            style = MaterialTheme.typography.labelSmall,
            color = colors.text2,
        )
    }
}

/**
 * P11 保存到笔记弹层：标题与内容预填，可改后保存。
 *
 * 输入过程不改契约 state，保存时通过 SaveConfirm(title, content) 一次性回传。
 */
@Composable
private fun SaveNoteDialog(dialog: SaveNoteDialogState, onEvent: (ChatEvent) -> Unit) {
    val colors = dbColors()
    var title by remember(dialog) { mutableStateOf(dialog.title) }
    var content by remember(dialog) { mutableStateOf(dialog.content) }
    ScrimModal {
        Text(
            text = "保存到笔记",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(10.dp))
        DBTextField(
            label = "笔记标题",
            value = title,
            onChange = { title = it },
        )
        Spacer(Modifier.height(11.dp))
        DBTextField(
            label = "内容（含来源题目，可编辑）",
            value = content,
            onChange = { content = it },
            singleLine = false,
            minHeight = 96.dp,
            maxHeight = 220.dp,
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            DBButton(
                text = "取消",
                onClick = { onEvent(ChatEvent.SaveCancel) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "存入笔记",
                onClick = { onEvent(ChatEvent.SaveConfirm(title = title, content = content)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 预览假数据：一轮问答（AI 回复已完成，可保存） */
private fun previewState() = ChatUiState(
    modelName = "gpt-4o-mini",
    contextSummary = "题干 + 4 个选项 + 解析（来自大模型基础 第 12 题）",
    messages = listOf(
        ChatMessageUi(
            id = 1,
            role = ChatRole.ME,
            text = "为什么长上下文要改用线性注意力？平方复杂度到底卡在哪一步？",
            streaming = false,
            showSave = false,
        ),
        ChatMessageUi(
            id = 2,
            role = ChatRole.AI,
            text = "卡在注意力分数矩阵这一步：QKᵀ 的形状是 n×n，序列翻倍，这个矩阵的显存与计算量翻四倍。\n\n" +
                "线性注意力的做法是先算 KᵀV（形状 d×d，与 n 无关），再用 Q 去乘，把复杂度降到 O(n·d²)，" +
                "代价是注意力分布的表达能力被削弱。",
            streaming = false,
            showSave = true,
        ),
    ),
    input = "",
    sending = false,
    errorBannerText = null,
    saveDialog = null,
)

/** 预览假数据：AI 回复生成中 */
private fun previewStreamingState() = previewState().copy(
    messages = listOf(
        previewState().messages.first(),
        ChatMessageUi(
            id = 3,
            role = ChatRole.AI,
            text = "卡在注意力分数矩阵这一步：QKᵀ 的形状是 n×n，序列翻倍",
            streaming = true,
            showSave = false,
        ),
    ),
)

/** 预览假数据：空对话（无上下文，仅引导文案） */
private fun previewEmptyState() = ChatUiState(
    modelName = "gpt-4o-mini",
    contextSummary = "",
    messages = emptyList(),
    input = "",
    sending = false,
    errorBannerText = null,
    saveDialog = null,
)

/** 预览假数据：请求失败提示 + 保存弹层打开 */
private fun previewDialogState() = previewState().copy(
    errorBannerText = "请求失败：连接超时，请检查模型配置与网络后重试",
    saveDialog = SaveNoteDialogState(
        title = "线性注意力为何能降到 O(n·d²)",
        content = "卡在注意力分数矩阵这一步：QKᵀ 的形状是 n×n，序列翻倍，这个矩阵的显存与计算量翻四倍。\n" +
            "来源：大模型基础 · 第 12 题",
    ),
)

@Preview(name = "AI 问答 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ChatPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ChatScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "AI 问答 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun ChatPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ChatScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "AI 问答 · 生成中 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ChatStreamingPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ChatScreen(state = previewStreamingState(), onEvent = {})
        }
    }
}

@Preview(name = "AI 问答 · 空对话 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ChatEmptyPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ChatScreen(state = previewEmptyState(), onEvent = {})
        }
    }
}

@Preview(name = "AI 问答 · 保存弹层 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ChatDialogPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ChatScreen(state = previewDialogState(), onEvent = {})
        }
    }
}
