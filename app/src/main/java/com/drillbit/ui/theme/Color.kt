package com.drillbit.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * DrillBit 语义色板（双主题唯一色源，页面代码只从这里取色，禁止字面 hex）。
 * 亮色为暖色书卷风，暗色为深色护眼风，只换色板不换形状与排版。
 */
data class DbColors(
    /** 页面底色 */
    val bg: Color,
    /** 卡片、弹窗底色 */
    val card: Color,
    /** 输入框、上下文块底色 */
    val card2: Color,
    /** 主文字（暗色不用纯白，降炫光） */
    val text: Color,
    /** 次要文字 */
    val text2: Color,
    /** 细线、卡片描边 */
    val line: Color,
    /** 主色：强调、选中态 */
    val primary: Color,
    /** 主色上的文字 */
    val onPrimary: Color,
    /** 答对、成功 */
    val ok: Color,
    /** 成功底色 */
    val okBg: Color,
    /** 答错、删除、失败 */
    val bad: Color,
    /** 失败底色 */
    val badBg: Color,
    /** 标签底、进度槽 */
    val chip: Color,
    /** 禁用态 */
    val off: Color,
    /** 弹窗遮罩 */
    val scrim: Color,
    /** 题图容器底色（白底保题图可读，双主题同值；2026-10-09 review F-20 入板） */
    val imgBg: Color,
)

/** 亮色色板（暖色书卷） */
val LightDbColors = DbColors(
    bg = Color(0xFFFAF6EE),
    card = Color(0xFFFFFDF8),
    card2 = Color(0xFFFFFFFF),
    text = Color(0xFF3E2C20),
    text2 = Color(0xFFA08D7B),
    line = Color(0xFFEBDFCE),
    primary = Color(0xFF8B5E3C),
    onPrimary = Color(0xFFFFF9F2),
    ok = Color(0xFF7E8B60),
    okBg = Color(0xFFEDF0E4),
    bad = Color(0xFFA0503C),
    badBg = Color(0xFFF6E7E2),
    chip = Color(0xFFEFE6D8),
    off = Color(0xFFD8C9B8),
    scrim = Color(0x703E2C20),
    imgBg = Color(0xFFFFFFFF),
)

/** 暗色色板（深色护眼） */
val DarkDbColors = DbColors(
    bg = Color(0xFF16181C),
    card = Color(0xFF212429),
    card2 = Color(0xFF1B1E22),
    text = Color(0xFFE7E3DB),
    text2 = Color(0xFF8E8B84),
    line = Color(0xFF2E3238),
    primary = Color(0xFFE0A150),
    onPrimary = Color(0xFF1A1613),
    ok = Color(0xFF6FD3B0),
    okBg = Color(0xFF1D2E2A),
    bad = Color(0xFFF07A6A),
    badBg = Color(0xFF33211F),
    chip = Color(0xFF262A30),
    off = Color(0xFF3A3F45),
    scrim = Color(0x9E000000),
    imgBg = Color(0xFFFFFFFF),
)

/** 当前色板（由 DrillBitTheme 按亮暗注入） */
val LocalDbColors = staticCompositionLocalOf { LightDbColors }

/** 取当前语义色板 */
@androidx.compose.runtime.Composable
fun dbColors(): DbColors = LocalDbColors.current
