package com.drillbit.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.drillbit.R

/**
 * 标题衬线字体（霞鹜文楷，覆盖常用汉字），用于标题类文字。
 * 缺字（生僻字、特殊符号）由系统字体自动兜底。
 */
val SerifFamily = FontFamily(
    Font(R.font.lxgw_wenkai_medium, FontWeight.Medium),
)

/** 标题风格：衬线 + 适当字距，营造书卷气 */
private fun serifStyle(size: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = SerifFamily,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    letterSpacing = spacing.sp,
)

/**
 * 排版规范：
 * 标题类（headline/title）用衬线书卷字体；正文与控件类用系统无衬线，清晰易读。
 */
val DbTypography = Typography(
    headlineSmall = serifStyle(22, 2.0),   // 页面大标题（题库 / 设置）
    titleLarge = serifStyle(19, 1.0),      // 二级页标题
    titleMedium = serifStyle(16, 0.5),     // 卡片标题、弹窗标题
    titleSmall = serifStyle(14, 1.0),      // 分组标题
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp),
    labelMedium = TextStyle(fontSize = 12.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontSize = 10.5.sp, letterSpacing = 0.2.sp),
)
