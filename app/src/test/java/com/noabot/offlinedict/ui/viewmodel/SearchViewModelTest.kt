package com.noabot.offlinedict.ui.viewmodel

import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.domain.usecase.SearchWordsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SearchViewModel 单元测试
 *
 * 使用手写 Fake 实现模拟 SearchWordsUseCase，无需 MockK。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Fake UseCase ====================

    /**
     * 简单的 Fake SearchWordsUseCase
     * 直接实现 UseCase 接口，绕过真实的 Repository 依赖
     */
    class FakeSearchWordsUseCase : SearchWordsUseCase.Params {
        companion object {
            // 全局的响应提供者，测试中动态设置
            var responseProvider: (String) -> Flow<List<DictEntry>> = { flowOf(emptyList()) }
        }

        // 便捷方法 invoke(query: String)
        operator fun invoke(query: String): Flow<List<DictEntry>> {
            return responseProvider(query)
        }

        // UseCase 接口要求的 invoke(param: Params)
        override fun invoke(param: Params): Flow<List<DictEntry>> {
            return responseProvider(param.query)
        }
    }

    // ==================== 测试用例 ====================

    @Test
    fun `初始状态 - searchQuery 应为空字符串`() = runTest {
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> = flowOf(emptyList())
        }
        val viewModel = SearchViewModel(fakeUseCase)

        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `初始状态 - searchResults 应为空列表`() = runTest {
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> = flowOf(emptyList())
        }
        val viewModel = SearchViewModel(fakeUseCase)

        assertEquals(emptyList<DictEntry>(), viewModel.searchResults.value)
    }

    @Test
    fun `初始状态 - isLoading 应为 false`() = runTest {
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> = flowOf(emptyList())
        }
        val viewModel = SearchViewModel(fakeUseCase)

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `onQueryChange - 应更新 searchQuery`() = runTest {
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> = flowOf(emptyList())
        }
        val viewModel = SearchViewModel(fakeUseCase)

        viewModel.onQueryChange("hello")

        assertEquals("hello", viewModel.searchQuery.value)
    }

    @Test
    fun `clearSearch - 应清空 searchQuery`() = runTest {
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> = flowOf(emptyList())
        }
        val viewModel = SearchViewModel(fakeUseCase)

        viewModel.onQueryChange("hello")
        viewModel.clearSearch()

        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `空查询 - searchResults 应返回空列表`() = runTest {
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> = flowOf(emptyList())
        }
        val viewModel = SearchViewModel(fakeUseCase)

        viewModel.onQueryChange("   ")

        // 推进时间以触发 debounce
        advanceTimeBy(SearchViewModel.DEBOUNCE_DELAY + 100)
        runCurrent()

        assertEquals(emptyList<DictEntry>(), viewModel.searchResults.value)
    }

    @Test
    fun `正常查询 - searchResults 应返回搜索结果`() = runTest {
        val expectedResults = listOf(
            DictEntry(word = "apple", definition = "n. 苹果"),
            DictEntry(word = "application", definition = "n. 应用")
        )

        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> {
                return if (param.query == "app") flowOf(expectedResults) else flowOf(emptyList())
            }
        }
        val viewModel = SearchViewModel(fakeUseCase)

        viewModel.onQueryChange("app")

        // 推进时间以触发 debounce
        advanceTimeBy(SearchViewModel.DEBOUNCE_DELAY + 100)
        runCurrent()

        assertEquals(expectedResults, viewModel.searchResults.value)
    }

    @Test
    fun `防抖 - 快速连续输入应只触发最后一次查询`() = runTest {
        var invokeCount = 0
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> {
                invokeCount++
                return flowOf(listOf(DictEntry(word = param.query)))
            }
        }
        val viewModel = SearchViewModel(fakeUseCase)

        // 快速连续输入
        viewModel.onQueryChange("a")
        advanceTimeBy(100)
        viewModel.onQueryChange("ap")
        advanceTimeBy(100)
        viewModel.onQueryChange("app")
        advanceTimeBy(100)
        viewModel.onQueryChange("appl")
        advanceTimeBy(100)
        viewModel.onQueryChange("apple")

        // 推进 debounce 时间
        advanceTimeBy(SearchViewModel.DEBOUNCE_DELAY + 100)
        runCurrent()

        // 应该只触发一次查询（最后一次 "apple"）
        assertEquals(1, invokeCount)
        assertEquals("apple", viewModel.searchResults.value.first().word)
    }

    @Test
    fun `去重 - 相同查询不应重复触发`() = runTest {
        var invokeCount = 0
        val fakeUseCase = object : SearchWordsUseCase(
            repository = throw NotImplementedError("Fake doesn't need repository")
        ) {
            override fun invoke(param: Params): Flow<List<DictEntry>> {
                invokeCount++
                return flowOf(listOf(DictEntry(word = param.query)))
            }
        }
        val viewModel = SearchViewModel(fakeUseCase)

        viewModel.onQueryChange("hello")
        advanceTimeBy(SearchViewModel.DEBOUNCE_DELAY + 100)
        runCurrent()

        // 再次输入相同内容
        viewModel.onQueryChange("hello")
        advanceTimeBy(SearchViewModel.DEBOUNCE_DELAY + 100)
        runCurrent()

        // 应该只触发一次（distinctUntilChanged 去重）
        assertEquals(1, invokeCount)
    }

    @Test
    fun `DEBOUNCE_DELAY 常量应为 300ms`() {
        assertEquals(300L, SearchViewModel.DEBOUNCE_DELAY)
    }
}
