package com.noabot.offlinedict.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noabot.offlinedict.domain.usecase.GetWordDetailUseCase
import com.noabot.offlinedict.domain.usecase.SearchWordsUseCase

/**
 * ViewModel 工厂类
 *
 * 用于创建带有依赖注入的 ViewModel 实例
 *
 * @property searchWordsUseCase 搜索单词用例
 * @property getWordDetailUseCase 获取单词详情用例
 */
class DictViewModelFactory(
    private val searchWordsUseCase: SearchWordsUseCase,
    private val getWordDetailUseCase: GetWordDetailUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            SearchViewModel::class.java -> {
                SearchViewModel(searchWordsUseCase) as T
            }
            WordDetailViewModel::class.java -> {
                WordDetailViewModel(getWordDetailUseCase) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
    }
}