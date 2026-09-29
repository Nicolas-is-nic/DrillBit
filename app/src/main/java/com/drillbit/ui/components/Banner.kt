package com.drillbit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.drillbit.ui.theme.dbColors

/** 提示条类型：成功 / 警示 / 信息 */
enum class BannerType { OK, WARN, INFO }

/** 提示条数据（跨页通用，供 UiState 直接携带） */
data class BannerUi(val text: String, val type: BannerType)

/** 行内提示条：上传结果、连接测试、计数说明等 */
@Composable
fun Banner(
    text: String,
    modifier: Modifier = Modifier,
    type: BannerType = BannerType.INFO,
) {
    val colors = dbColors()
    val background = when (type) {
        BannerType.OK -> colors.okBg
        BannerType.WARN -> colors.badBg
        BannerType.INFO -> colors.chip
    }
    val foreground = when (type) {
        BannerType.OK -> colors.ok
        BannerType.WARN -> colors.bad
        BannerType.INFO -> colors.primary
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = foreground,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .padding(horizontal = 11.dp, vertical = 9.dp),
    )
}
