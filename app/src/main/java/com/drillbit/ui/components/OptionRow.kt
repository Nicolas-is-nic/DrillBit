package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/** 选项状态：未作答 / 多选答前选中（主色描边）/ 正确 / 误选 / 答后锁定 */
enum class OptionState { DEFAULT, SELECTED, GOOD, BAD, DISABLED }

/**
 * 答题选项行：左侧字母块 + 选项文本。
 *
 * 状态着色由传入的 state 决定，页面层只负责把索引映射成状态，不判定对错。
 * onClick 为空时整行不可点（答后锁定）。
 */
@Composable
fun OptionRow(
    label: String,
    text: String,
    state: OptionState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(10.dp)
    val borderColor = when (state) {
        OptionState.SELECTED -> colors.primary
        OptionState.GOOD -> colors.ok
        OptionState.BAD -> colors.bad
        OptionState.DEFAULT, OptionState.DISABLED -> colors.line
    }
    val rowBackground = when (state) {
        OptionState.GOOD -> colors.okBg
        OptionState.BAD -> colors.badBg
        OptionState.DEFAULT, OptionState.SELECTED, OptionState.DISABLED -> colors.card
    }
    val markBackground = when (state) {
        OptionState.GOOD -> colors.ok
        OptionState.SELECTED -> colors.primary
        OptionState.BAD -> colors.bad
        OptionState.DEFAULT, OptionState.DISABLED -> colors.chip
    }
    val markColor = when (state) {
        OptionState.GOOD, OptionState.BAD -> colors.card
        OptionState.SELECTED -> colors.onPrimary
        OptionState.DISABLED -> colors.off
        OptionState.DEFAULT -> colors.text2
    }
    val textColor = if (state == OptionState.DISABLED) colors.text2 else colors.text
    val flagText = when (state) {
        OptionState.GOOD -> "正确"
        OptionState.BAD -> "你选"
        OptionState.DEFAULT, OptionState.SELECTED, OptionState.DISABLED -> null
    }
    val clickable = if (onClick != null && state != OptionState.DISABLED) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(rowBackground)
            .border(1.dp, borderColor, shape)
            .then(clickable)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(markBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = markColor,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
        if (flagText != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = flagText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.card,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (state == OptionState.GOOD) colors.ok else colors.bad)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}
