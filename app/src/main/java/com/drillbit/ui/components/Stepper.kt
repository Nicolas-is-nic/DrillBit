package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/**
 * 加减号数量选择器：中间显示当前值，两侧按钮为 chip 底方块。
 *
 * value <= minValue 时减号置灰且不可点（下限锁定）。
 */
@Composable
fun Stepper(
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minValue: Int = 10,
) {
    val colors = dbColors()
    val minusEnabled = value > minValue
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.card)
            .border(1.dp, colors.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(symbol = "−", enabled = minusEnabled, onClick = onMinus)
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = colors.text,
        )
        StepperButton(symbol = "+", enabled = true, onClick = onPlus)
    }
}

/** 选择器单侧按钮（减号可禁用，加号恒可用） */
@Composable
private fun StepperButton(
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = dbColors()
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.chip)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) colors.primary else colors.off,
        )
    }
}
