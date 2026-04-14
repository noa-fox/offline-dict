package com.noabot.offlinedict.ui.viewmodel

import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.domain.usecase.GetWordDetailUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * WordDetailViewModel 单元测试
 *
 * 使用 Fake 实现模拟 GetWordDetailUseCase，无需 MockK。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WordDetailViewModelTest {

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
     * 简单的 Fake GetWordDetailUseCase
     * 允许在测试中动态设置返回结果
     */
    class FakeGetWordDetailUseCase : GetWordDetailUseCase(
        repository = object : Any() {} // 不需要真实 repository
    ) {
        var responseProvider: (String) -> Flow<DictEntry?> = { flowOf(null) }

        override fun invoke(param: String): Flow<DictEntry?> {
            return responseProvider(param)
        }
    }

    // ==================== 测试用例 ====================

    @Test
    fun `初始状态 - selectedWord 应为 null`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        val viewModel = WordDetailViewModel(fakeUseCase)

        assertNull(viewModel.selectedWord.value)
    }

    @Test
    fun `初始状态 - wordDetail 应为 null`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        val viewModel = WordDetailViewModel(fakeUseCase)

        assertNull(viewModel.wordDetail.value)
    }

    @Test
    fun `初始状态 - isLoading 应为 false`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        val viewModel = WordDetailViewModel(fakeUseCase)

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `初始状态 - error 应为 null`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        val viewModel = WordDetailViewModel(fakeUseCase)

        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadWordDetail - 加载成功时应正确设置详情`() = runTest {
        val expectedDetail = DictEntry(
            word = "hello",
            phonetic = "/həˈləʊ/",
            phoneticUs = "/həˈloʊ/",
            definition = "int. 你好",
            translation = "你好，您好",
            pos = "int."
        )

        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = { word ->
            if (word == "hello") flowOf(expectedDetail) else flowOf(null)
        }
        val viewModel = WordDetailViewModel(fakeUseCase)

        viewModel.loadWordDetail("hello")

        // 推进协程执行完毕
        advanceUntilIdle()

        assertEquals("hello", viewModel.selectedWord.value)
        assertEquals(expectedDetail, viewModel.wordDetail.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadWordDetail - 单词不存在时 error 应为 "未找到该单词"`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = { flowOf(null) }
        val viewModel = WordDetailViewModel(fakeUseCase)

        viewModel.loadWordDetail("nonexistent")

        advanceUntilIdle()

        assertEquals("nonexistent", viewModel.selectedWord.value)
        assertNull(viewModel.wordDetail.value)
        assertFalse(viewModel.isLoading.value)
        assertEquals("未找到该单词", viewModel.error.value)
    }

    @Test
    fun `loadWordDetail - 发生异常时应设置错误信息`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = {
            flow { throw RuntimeException("网络错误") }
        }
        val viewModel = WordDetailViewModel(fakeUseCase)

        viewModel.loadWordDetail("error_word")

        advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
        assertEquals("网络错误", viewModel.error.value)
        assertNull(viewModel.wordDetail.value)
    }

    @Test
    fun `loadWordDetail - 加载过程中 isLoading 应先变为 true`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = { flowOf(DictEntry(word = "test")) }
        val viewModel = WordDetailViewModel(fakeUseCase)

        viewModel.loadWordDetail("test")

        // 在协程执行前检查 isLoading
        assertTrue(viewModel.isLoading.value)

        advanceUntilIdle()

        // 执行完成后 isLoading 应变为 false
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `loadWordDetail - 加载前应清空之前的错误`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = { flowOf(DictEntry(word = "test")) }
        val viewModel = WordDetailViewModel(fakeUseCase)

        // 先模拟一个错误状态
        viewModel.loadWordDetail("nonexistent")
        advanceUntilIdle()
        assertEquals("未找到该单词", viewModel.error.value)

        // 再次加载一个存在的单词
        viewModel.loadWordDetail("test")
        advanceUntilIdle()

        // 错误应该被清空
        assertNull(viewModel.error.value)
    }

    @Test
    fun `clear - 应清空所有状态`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = { flowOf(DictEntry(word = "hello")) }
        val viewModel = WordDetailViewModel(fakeUseCase)

        // 先加载一些数据
        viewModel.loadWordDetail("hello")
        advanceUntilIdle()

        // 确认状态已设置
        assertEquals("hello", viewModel.selectedWord.value)
        assertEquals("hello", viewModel.wordDetail.value?.word)

        // 清空
        viewModel.clear()

        assertNull(viewModel.selectedWord.value)
        assertNull(viewModel.wordDetail.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.error.value)
    }

    @Test
    fun `loadWordDetail - 多次调用应更新为最新单词`() = runTest {
        val fakeUseCase = FakeGetWordDetailUseCase()
        fakeUseCase.responseProvider = { word ->
            flowOf(DictEntry(word = word, definition = "definition of $word"))
        }
        val viewModel = WordDetailViewModel(fakeUseCase)

        viewModel.loadWordDetail("apple")
        advanceUntilIdle()
        assertEquals("apple", viewModel.selectedWord.value)

        viewModel.loadWordDetail("banana")
        advanceUntilIdle()
        assertEquals("banana", viewModel.selectedWord.value)
        assertEquals("banana", viewModel.wordDetail.value?.word)
    }
}
