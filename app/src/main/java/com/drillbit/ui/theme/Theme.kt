package com.drillbit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * 亮色 Material3 方案：语义色板映射到 Material 组件（弹窗、导航栏等）。
 */
private fun lightScheme(c: DbColors) = lightColorScheme(
    primary = c.primary,
    onPrimary = c.onPrimary,
    primaryContainer = c.chip,
    onPrimaryContainer = c.text,
    background = c.bg,
    onBackground = c.text,
    surface = c.card,
    onSurface = c.text,
    surfaceVariant = c.card2,
    onSurfaceVariant = c.text2,
    surfaceTint = c.primary,
    error = c.bad,
    onError = c.card,
    errorContainer = c.badBg,
    onErrorContainer = c.bad,
    outline = c.line,
    outlineVariant = c.line,
    scrim = c.scrim,
    secondary = c.ok,
    onSecondary = c.card,
    secondaryContainer = c.okBg,
    onSecondaryContainer = c.ok,
)

/**
 * 暗色 Material3 方案。
 */
private fun darkScheme(c: DbColors) = darkColorScheme(
    primary = c.primary,
    onPrimary = c.onPrimary,
    primaryContainer = c.chip,
    onPrimaryContainer = c.text,
    background = c.bg,
    onBackground = c.text,
    surface = c.card,
    onSurface = c.text,
    surfaceVariant = c.card2,
    onSurfaceVariant = c.text2,
    surfaceTint = c.primary,
    error = c.bad,
    onError = c.card,
    errorContainer = c.badBg,
    onErrorContainer = c.bad,
    outline = c.line,
    outlineVariant = c.line,
    scrim = c.scrim,
    secondary = c.ok,
    onSecondary = c.card,
    secondaryContainer = c.okBg,
    onSecondaryContainer = c.ok,
)

/**
 * 应用主题：darkTheme 切换只换色板，不换形状与排版。
 * 骨架阶段 darkTheme 由内存态持有（默认亮色），阶段 2 接 DataStore 持久化。
 */
@Composable
fun DrillBitTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkDbColors else LightDbColors
    CompositionLocalProvider(LocalDbColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme(colors) else lightScheme(colors),
            typography = DbTypography,
            content = content,
        )
    }
}
