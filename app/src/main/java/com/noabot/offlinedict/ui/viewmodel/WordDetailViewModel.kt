package com.noabot.offlinedict.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.domain.usecase.GetWordDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 单词详情页面 ViewModel
 *
 * 管理选中单词的详情状态：
 * - 加载单词详情
 * - 异常处理
 * - 状态管理（加载中、成功、错误）
 *
 * @property getWordDetailUseCase 获取单词详情用例
 */
class WordDetailViewModel(
    private val getWordDetailUseCase: GetWordDetailUseCase
) : ViewModel() {

    // 当前选中的单词
    private val _selectedWord = MutableStateFlow<String?>(null)
    val selectedWord: StateFlow<String?> = _selectedWord.asStateFlow()

    // 单词详情状态
    private val _wordDetail = MutableStateFlow<DictEntry?>(null)
    val wordDetail: StateFlow<DictEntry?> = _wordDetail.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误状态
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * 加载单词详情
     *
     * @param word 要查询的单词
     */
    fun loadWordDetail(word: String) {
        _selectedWord.value = word
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            getWordDetailUseCase(word)
                .catch { e ->
                    _isLoading.value = false
                    _error.value = e.message ?: "加载失败"
                    _wordDetail.value = null
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = null
                )
                .collect { detail ->
                    _isLoading.value = false
                    _wordDetail.value = detail
                    if (detail == null) {
                        _error.value = "未找到该单词"
                    }
                }
        }
    }

    /**
     * 清空状态
     */
    fun clear() {
        _selectedWord.value = null
        _wordDetail.value = null
        _isLoading.value = false
        _error.value = null
    }
}