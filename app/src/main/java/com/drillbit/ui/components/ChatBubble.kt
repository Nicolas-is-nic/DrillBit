package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.theme.dbColors
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography

/**
 * 对话角色：用户（右侧主色气泡）/ 模型（左侧卡片气泡）。
 *
 * 分工方案未给出该枚举声明，按 7.6 节注释「ME / AI」定义。
 */
enum class ChatRole { ME, AI }

/**
 * 对话气泡：ME 靠右主色底，AI 靠左卡片底。
 *
 * streaming = true 时在气泡内提示「正在生成…」；showSave = true 时在 AI 气泡底部给出「保存到笔记」入口。
 */
@Composable
fun ChatBubble(
    role: ChatRole,
    text: String,
    modifier: Modifier = Modifier,
    streaming: Boolean = false,
    showSave: Boolean = false,
    onSave: () -> Unit = {},
) {
    val colors = dbColors()
    val isMe = role == ChatRole.ME
    val shape = if (isMe) {
        RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 4.dp, bottomStart = 13.dp)
    } else {
        RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp, bottomEnd = 13.dp, bottomStart = 4.dp)
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(shape)
                .background(if (isMe) colors.primary else colors.card)
                .then(if (isMe) Modifier else Modifier.border(1.dp, colors.line, shape))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (isMe || streaming) {
                // 用户消息与流式生成中：纯文本逐字显示（流式高频刷新，不做富渲染）
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    color = if (isMe) colors.onPrimary else colors.text,
                )
            } else {
                // AI 回答完成：Markdown 富渲染（列表/代码块/加粗等），配色自动跟随 MaterialTheme
                Markdown(
                    // 压缩 3+ 连续换行为标准段落分隔，避免模型输出多余空行导致大片留白
                    content = text.replace(Regex("\\n{3,}"), "\\n\\n"),
                    typography = markdownTypography(
                        text = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                    ),
                )
            }
            if (streaming) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "正在生成…",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                )
            }
            if (!isMe && showSave) {
                Spacer(Modifier.height(8.dp))
                // 标准按钮替代小热区文字：滚动容器内点击更可靠（真机反馈：文字点击无反应）
                DBButton(
                    text = "保存到笔记",
                    onClick = onSave,
                    type = DBButtonType.GHOST,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
