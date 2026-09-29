package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drillbit.ui.theme.dbColors

/**
 * 设置行（跳转型）：标题 + 右侧值 + 箭头。
 *
 * valueText 为空则不显示值；onClick 为空时整行不可点且不显示箭头（如版本号只读行）。
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        if (valueText != null) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                color = colors.text2,
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "›",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = colors.text2,
            )
        }
    }
}

/** 设置行（开关型）：标题 + 右侧开关（深色模式等） */
@Composable
fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = dbColors()
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.line, shape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 13.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.card,
                uncheckedTrackColor = colors.off,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}
