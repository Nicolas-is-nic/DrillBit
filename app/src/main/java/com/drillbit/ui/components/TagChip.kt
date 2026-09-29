package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/** 标签状态：普通（主色）/ 成功（有新版本、已备份）/ 警示（重考计数） */
enum class TagChipType { NORMAL, OK, WARN }

/** 小标签：题库版本标记、错题重考计数等（10.5sp，圆角 6dp） */
@Composable
fun TagChip(
    text: String,
    modifier: Modifier = Modifier,
    type: TagChipType = TagChipType.NORMAL,
) {
    val colors = dbColors()
    val background = when (type) {
        TagChipType.NORMAL -> colors.chip
        TagChipType.OK -> colors.okBg
        TagChipType.WARN -> colors.badBg
    }
    val foreground = when (type) {
        TagChipType.NORMAL -> colors.primary
        TagChipType.OK -> colors.ok
        TagChipType.WARN -> colors.bad
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
