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
import com.drillbit.ui.components.Segmented
import com.drillbit.ui.theme.DrillBitTheme
import com.drillbit.ui.theme.dbColors

/** P17 模型配置：接口类型 / 地址 / 密钥 / 模型名，可测试连接 */
@Composable
fun ModelConfigScreen(state: ModelConfigUiState, onEvent: (ModelConfigEvent) -> Unit) {
    val colors = dbColors()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            DbTopBar(
                title = "模型配置",
                onBack = { onEvent(ModelConfigEvent.Back) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = "接口类型",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text2,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
                Segmented(
                    options = listOf("OpenAI 兼容", "Anthropic"),
                    selected = if (state.apiType == ApiType.OPENAI) 0 else 1,
                    onSelect = { index ->
                        onEvent(
                            ModelConfigEvent.TypeChange(
                                if (index == 0) ApiType.OPENAI else ApiType.ANTHROPIC,
                            ),
                        )
                    },
                )
                Spacer(Modifier.height(11.dp))
                DBTextField(
                    label = "接口地址",
                    value = state.url,
                    onChange = { text -> onEvent(ModelConfigEvent.UrlChange(text)) },
                    mono = true,
                )
                Spacer(Modifier.height(11.dp))
                DBTextField(
                    label = "API Key",
                    value = state.apiKey,
                    onChange = { text -> onEvent(ModelConfigEvent.KeyChange(text)) },
                    mono = true,
                    password = true,
                )
                Spacer(Modifier.height(11.dp))
                DBTextField(
                    label = "模型名称",
                    value = state.modelName,
                    onChange = { text -> onEvent(ModelConfigEvent.NameChange(text)) },
                    mono = true,
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
                    text = "密钥仅保存在本机，不会随题库或笔记上传到服务器",
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
                    text = if (state.testing) "测试中…" else "测试连接",
                    onClick = { onEvent(ModelConfigEvent.Test) },
                    modifier = Modifier.width(110.dp),
                    type = DBButtonType.GHOST,
                    enabled = !state.testing,
                )
                Spacer(Modifier.width(9.dp))
                DBButton(
                    text = "保存",
                    onClick = { onEvent(ModelConfigEvent.Save) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 预览假数据：OpenAI 兼容 + 测试通过 */
private fun previewState() = ModelConfigUiState(
    apiType = ApiType.OPENAI,
    url = "https://api.example.com/v1",
    apiKey = "sk-1234567890ab3f7a",
    modelName = "gpt-4o-mini",
    testing = false,
    testResult = BannerUi(text = "连接测试通过 · 延迟 620 毫秒", type = BannerType.OK),
)

/** 预览假数据：Anthropic + 测试失败 */
private fun previewAnthropicState() = ModelConfigUiState(
    apiType = ApiType.ANTHROPIC,
    url = "https://api.anthropic.com/v1",
    apiKey = "sk-ant-0987654321",
    modelName = "claude-3-5-sonnet",
    testing = false,
    testResult = BannerUi(text = "连接失败：密钥无效或额度不足", type = BannerType.WARN),
)

/** 预览假数据：测试中 */
private fun previewTestingState() = previewState().copy(testing = true, testResult = null)

@Preview(name = "模型配置 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ModelConfigPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ModelConfigScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "模型配置 · 暗色", widthDp = 360, heightDp = 780)
@Composable
private fun ModelConfigPreviewDark() {
    DrillBitTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ModelConfigScreen(state = previewState(), onEvent = {})
        }
    }
}

@Preview(name = "模型配置 · Anthropic 失败 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ModelConfigAnthropicPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ModelConfigScreen(state = previewAnthropicState(), onEvent = {})
        }
    }
}

@Preview(name = "模型配置 · 测试中 · 亮色", widthDp = 360, heightDp = 780)
@Composable
private fun ModelConfigTestingPreviewLight() {
    DrillBitTheme {
        Box(modifier = Modifier.fillMaxSize().background(dbColors().bg)) {
            ModelConfigScreen(state = previewTestingState(), onEvent = {})
        }
    }
}
