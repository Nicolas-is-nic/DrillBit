package com.drillbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drillbit.ServiceLocator
import com.drillbit.ui.components.BannerType
import com.drillbit.ui.components.BannerUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 模型配置 ViewModel：编辑四项配置、连通性测试（显示延迟）、保存到 DataStore。
 */
class ModelConfigViewModel : ViewModel() {

    private val stateFlow = MutableStateFlow(
        ModelConfigUiState(
            apiType = ApiType.OPENAI,
            url = "",
            apiKey = "",
            modelName = "",
            testing = false,
            testResult = null,
        ),
    )
    val state: StateFlow<ModelConfigUiState> = stateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val s = ServiceLocator.settingsStore.snapshot()
            stateFlow.value = stateFlow.value.copy(
                apiType = if (s.llmType == "anthropic") ApiType.ANTHROPIC else ApiType.OPENAI,
                url = s.llmUrl,
                apiKey = s.llmKey,
                modelName = s.llmModel,
            )
        }
    }

    fun onEvent(event: ModelConfigEvent) {
        when (event) {
            is ModelConfigEvent.TypeChange -> stateFlow.value = stateFlow.value.copy(apiType = event.type)
            is ModelConfigEvent.UrlChange -> stateFlow.value = stateFlow.value.copy(url = event.text)
            is ModelConfigEvent.KeyChange -> stateFlow.value = stateFlow.value.copy(apiKey = event.text)
            is ModelConfigEvent.NameChange -> stateFlow.value = stateFlow.value.copy(modelName = event.text)
            ModelConfigEvent.Test -> test()
            ModelConfigEvent.Save -> save()
            else -> Unit // Back 由导航层处理
        }
    }

    private fun test() {
        if (stateFlow.value.testing) return
        viewModelScope.launch {
            stateFlow.value = stateFlow.value.copy(testing = true, testResult = null)
            val current = stateFlow.value
            val settings = com.drillbit.data.DbSettings(
                llmUrl = current.url,
                llmKey = current.apiKey,
                llmModel = current.modelName,
                llmType = if (current.apiType == ApiType.ANTHROPIC) "anthropic" else "openai",
            )
            runCatching { ServiceLocator.llmClient.testConnection(settings) }
                .onSuccess { ms ->
                    stateFlow.value = stateFlow.value.copy(
                        testing = false,
                        testResult = BannerUi("连接测试通过 · 延迟 $ms 毫秒", BannerType.OK),
                    )
                }
                .onFailure { e ->
                    stateFlow.value = stateFlow.value.copy(
                        testing = false,
                        testResult = BannerUi("连接失败：${e.message ?: "未知错误"}", BannerType.WARN),
                    )
                }
        }
    }

    private fun save() {
        viewModelScope.launch {
            val current = stateFlow.value
            // 防护写失败（磁盘满/DataStore 损坏）：与 test() 对称，无 adb 环境崩溃代价高（review M-2）
            runCatching {
                ServiceLocator.settingsStore.setLlm(
                    url = current.url,
                    key = current.apiKey,
                    model = current.modelName,
                    type = if (current.apiType == ApiType.ANTHROPIC) "anthropic" else "openai",
                )
            }.onSuccess {
                stateFlow.value = stateFlow.value.copy(
                    testResult = BannerUi("已保存", BannerType.OK),
                )
            }.onFailure { e ->
                stateFlow.value = stateFlow.value.copy(
                    testResult = BannerUi("保存失败：${e.message ?: "未知错误"}", BannerType.WARN),
                )
            }
        }
    }
}
