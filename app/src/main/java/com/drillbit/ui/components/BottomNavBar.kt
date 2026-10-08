package com.drillbit.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Reorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drillbit.R
import com.drillbit.ui.theme.dbColors

/** 底部导航条目 */
data class DbTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

/** 四 Tab 常驻导航（题库 / 错题 / 笔记 / 设置），图标对照页面设计稿 */
val dbTabs = listOf(
    DbTabItem(
        route = "banks",
        labelRes = R.string.tab_banks,
        selectedIcon = Icons.Filled.Reorder,
        unselectedIcon = Icons.Outlined.Reorder,
    ),
    DbTabItem(
        route = "wrong",
        labelRes = R.string.tab_wrong,
        selectedIcon = Icons.Filled.Replay,
        unselectedIcon = Icons.Outlined.Replay,
    ),
    DbTabItem(
        route = "notes",
        labelRes = R.string.tab_notes,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
    ),
    DbTabItem(
        route = "settings",
        labelRes = R.string.tab_settings,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
    ),
)

/**
 * 底部导航栏：细线分隔 + 选中态主色、指示器透明（视觉对照设计稿）。
 */
@Composable
fun DbBottomNavBar(
    currentRoute: String?,
    onTabClick: (String) -> Unit,
) {
    val colors = dbColors()
    Column {
        HorizontalDivider(thickness = 1.dp, color = colors.line)
        NavigationBar(
            containerColor = colors.bg,
            tonalElevation = 0.dp,
        ) {
            dbTabs.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unselectedIconColor = colors.text2,
                        unselectedTextColor = colors.text2,
                    ),
                    onClick = { onTabClick(tab.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = stringResource(tab.labelRes),
                        )
                    },
                    label = {
                        Text(
                            stringResource(tab.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
    }
}
