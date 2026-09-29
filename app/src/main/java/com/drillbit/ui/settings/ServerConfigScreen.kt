package com.drillbit.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.drillbit.ui.components.Banner
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.BannerUi
import com.drillbit.ui.components.DBButton
import com.drillbit.ui.components.DBButtonType
import com.drillbit.ui.components.DBTextField
import com.drillbit.ui.components.DbTopBar
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P18 服务器配置：地址 + 访问 Token，用于题库拉取与笔记上传鉴权 */
@Composable
fun ServerConfigScreen(state: ServerConfigUiState, onEvent: (ServerConfigEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "服务器配置",
                onBack = { onEvent(ServerConfigEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                DBTextField(
                    label = "服务器地址",
                    value = state.url,
                    onChange = { text -> onEvent(ServerConfigEvent.UrlChange(text)) },
                    mono = true,
                )
                Spacer(Modifier.height(11.dp))
                DBTextField(
                    label = "访问 Token",
                    value = state.token,
                    onChange = { text -> onEvent(ServerConfigEvent.TokenChange(text)) },
                    mono = true,
                    password = true,
                )
                Spacer(Modifier.height(11.dp))
                state.testResult?.let { result ->
                    Banner(
                        text = result.text,
                        modifier = Modifier.padding(bottom = 10.dp),
                        type = result.type,
                    )
                }
                Text(
                    text = "地址必须为 https，安卓默认禁止明文 http 访问；Token 用于题库拉取与笔记上传的鉴权",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                )
                Spacer(Modifier.height(16.dp))
            }
            HorizontalDivider(thickness = 1.dp, color = colors.line)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
            ) {
                DBButton(
                    text = if (state.testing) "测试中…" else "测试",
                    onClick = { onEvent(ServerConfigEvent.Test) },
                    modifier = Modifier.width(110.dp),
                    type = DBButtonType.GHOST,
                    enabled = !state.testing,
                )
                Spacer(Modifier.width(9.dp))
                DBButton(
                    text = "保存",
                    onClick = { onEvent(ServerConfigEvent.Save) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 预览假数据：连通性正常 */
private fun previewState() = ServerConfigUiState(
    url = "https://note.example.com",
    token = "token-abcdef123456",
    testing = false,
    testResult = BannerUi(text = "连通性正常 · 题库接口与笔记接口均可用", type = BannerType.OK),
)

/** 预览假数据：明文 http 被拒（失败提示） */
private fun previewFailedState() = ServerConfigUiState(
    url = "http://192.168.1.10:8000",
    token = "",
    testing = false,
    testResult = BannerUi(text = "地址无效：安卓禁止明文 http，请改用 https 地址", type = BannerType.WARN),
)

@Preview(name = "服务器配置 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ServerConfigPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ServerConfigScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "服务器配置 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun ServerConfigPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ServerConfigScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "服务器配置 · 校验失败 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ServerConfigFailedPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ServerConfigScreen(state = previewFailedState(), onEvent = {})
        }
    }
}
