package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.drillbit.ui.theme.dbColors
import com.drillbit.ui.theme.DrillBitTheme

/** 崩溃弹窗数据（全局状态驱动，任何页面之上弹出） */
data class CrashDialogUi(
    val timeText: String,
    val versionText: String,
    val stackText: String,
)

/** 崩溃弹窗事件 */
sealed interface CrashDialogEvent {
    data object Copy : CrashDialogEvent
    data object Close : CrashDialogEvent
}

/**
 * 崩溃日志弹窗：启动时展示上次闪退的时间、版本与堆栈，供用户截图反馈。
 *
 * 无 adb 环境下这是唯一的崩溃定位手段（对照 spec 3.2 / 8.1）。
 */
@Composable
fun CrashDialog(
    state: CrashDialogUi,
    onEvent: (CrashDialogEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(10.dp)
    ScrimModal(modifier = modifier) {
        Text(
            text = "上次运行发生崩溃",
            style = MaterialTheme.typography.titleMedium,
            color = colors.text,
        )
        Spacer(Modifier.height(8.dp))
        if (state.timeText.isNotBlank()) {
            Text(
                text = "时间：${state.timeText}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.text2,
            )
        }
        if (state.versionText.isNotBlank()) {
            Text(
                text = "版本：${state.versionText}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.text2,
            )
        }
        Text(
            text = "可截图发给开发定位问题",
            style = MaterialTheme.typography.labelMedium,
            color = colors.text2,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .clip(shape)
                .background(colors.card2)
                .border(1.dp, colors.line, shape)
                .padding(12.dp),
        ) {
            Text(
                text = state.stackText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    lineHeight = 17.sp,
                ),
                color = colors.text2,
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row {
            DBButton(
                text = "复制",
                onClick = { onEvent(CrashDialogEvent.Copy) },
                modifier = Modifier.weight(1f),
                type = DBButtonType.GHOST,
            )
            Spacer(Modifier.width(9.dp))
            DBButton(
                text = "关闭",
                onClick = { onEvent(CrashDialogEvent.Close) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 预览假数据：上次运行崩溃堆栈 */
private fun previewState() = CrashDialogUi(
    timeText = "2026-09-28 22:14:03",
    versionText = "v0.1.0（1）",
    stackText = "java.lang.NullPointerException\n" +
        "    at com.drillbit.ui.quiz.QuizViewModel.onOptionClick(QuizViewModel.kt:118)\n" +
        "    at com.drillbit.ui.quiz.QuizScreenKt\$OptionRow\$1.invoke(QuizScreen.kt:64)\n" +
        "    at androidx.compose.foundation.ClickableKt\$clickable\$4.invoke(Clickable.kt:203)",
)

@Preview(name = "崩溃弹窗 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun CrashDialogPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            CrashDialog(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "崩溃弹窗 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun CrashDialogPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            CrashDialog(state = previewState(), onEvent = {})
        }
    }
}
