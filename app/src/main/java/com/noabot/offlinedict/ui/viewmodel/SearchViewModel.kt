package com.noabot.offlinedict.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.domain.usecase.SearchWordsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * 搜索页面 ViewModel
 *
 * 管理搜索状态，实现响应式搜索：
 * - 输入防抖（300ms）
 * - 相同查询去重
 * - 自动切换最新查询（取消旧请求）
 * - 异常处理
 *
 * @property searchWordsUseCase 搜索单词用例
 */
class SearchViewModel(
    private val searchWordsUseCase: SearchWordsUseCase
) : ViewModel() {

    // 搜索输入状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 搜索结果状态（带防抖和去重）
    val searchResults: StateFlow<List<DictEntry>> = _searchQuery
        .debounce(DEBOUNCE_DELAY)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                searchWordsUseCase(query)
                    .catch { e ->
                        // 异常时返回空列表，避免 UI 崩溃
                        emit(emptyList())
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 更新搜索查询
     *
     * @param query 新的搜索关键词
     */
    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }

    companion object {
        // 防抖延迟时间（毫秒）
        const val DEBOUNCE_DELAY = 300L
    }
}