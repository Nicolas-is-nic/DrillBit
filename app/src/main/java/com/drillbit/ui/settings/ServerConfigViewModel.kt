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
 * 服务器配置 ViewModel：地址与 Token 编辑、连通性测试（题库目录接口）、保存。
 */
class ServerConfigViewModel : ViewModel() {

    private val stateFlow = MutableStateFlow(
        ServerConfigUiState(
            url = "",
            token = "",
            testing = false,
            testResult = null,
        ),
    )
    val state: StateFlow<ServerConfigUiState> = stateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            val s = ServiceLocator.settingsStore.snapshot()
            stateFlow.value = stateFlow.value.copy(
                url = s.serverUrl,
                token = s.serverToken,
            )
        }
    }

    fun onEvent(event: ServerConfigEvent) {
        when (event) {
            is ServerConfigEvent.UrlChange -> stateFlow.value = stateFlow.value.copy(url = event.text)
            is ServerConfigEvent.TokenChange -> stateFlow.value = stateFlow.value.copy(token = event.text)
            ServerConfigEvent.Test -> test()
            ServerConfigEvent.Save -> save()
            else -> Unit // Back 由导航层处理
        }
    }

    private fun test() {
        if (stateFlow.value.testing) return
        viewModelScope.launch {
            stateFlow.value = stateFlow.value.copy(testing = true, testResult = null)
            val current = stateFlow.value
            if (current.url.isBlank()) {
                stateFlow.value = stateFlow.value.copy(
                    testing = false,
                    testResult = BannerUi("请先填写服务器地址", BannerType.WARN),
                )
                return@launch
            }
            if (current.url.trim().startsWith("http://")) {
                stateFlow.value = stateFlow.value.copy(
                    testing = false,
                    testResult = BannerUi("地址必须为 https，安卓默认禁止明文 http 访问", BannerType.WARN),
                )
                return@launch
            }
            val settings = com.drillbit.data.DbSettings(
                serverUrl = current.url,
                serverToken = current.token,
            )
            runCatching { ServiceLocator.bankRepository.fetchRemoteIndex(settings) }
                .onSuccess { items ->
                    stateFlow.value = stateFlow.value.copy(
                        testing = false,
                        testResult = BannerUi("连通性正常 · 服务器共有 ${items.size} 个题库", BannerType.OK),
                    )
                }
                .onFailure { e ->
                    stateFlow.value = stateFlow.value.copy(
                        testing = false,
                        testResult = BannerUi("连接失败：${e.message ?: "网络异常"}", BannerType.WARN),
                    )
                }
        }
    }

    private fun save() {
        viewModelScope.launch {
            val current = stateFlow.value
            ServiceLocator.settingsStore.setServer(current.url, current.token)
        }
    }
}
