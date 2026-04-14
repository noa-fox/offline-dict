package com.noabot.offlinedict.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.noabot.offlinedict.ui.screen.SearchScreen
import com.noabot.offlinedict.ui.screen.WordDetailScreen
import com.noabot.offlinedict.ui.viewmodel.DictViewModelFactory
import com.noabot.offlinedict.ui.viewmodel.SearchViewModel
import com.noabot.offlinedict.ui.viewmodel.WordDetailViewModel

/**
 * 导航路由定义
 */
object AppRoutes {
    const val SEARCH = "search"
    const val WORD_DETAIL = "word_detail/{word}"

    /**
     * 构建单词详情路由
     *
     * @param word 单词
     * @return 完整路由路径
     */
    fun wordDetail(word: String): String {
        return "word_detail/${encodeURIComponent(word)}"
    }

    /**
     * 从路由参数中提取单词
     *
     * @param encodedWord 编码后的单词
     * @return 解码后的单词
     */
    fun decodeWord(encodedWord: String): String {
        return decodeURIComponent(encodedWord)
    }
}

/**
 * 应用导航图
 *
 * @param navController 导航控制器
 * @param viewModelFactory ViewModel 工厂
 * @param startDestination 起始路由
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModelFactory: DictViewModelFactory,
    startDestination: String = AppRoutes.SEARCH
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 搜索页面
        composable(AppRoutes.SEARCH) {
            val searchViewModel: SearchViewModel = viewModel(factory = viewModelFactory)

            SearchScreen(
                onWordClick = { word ->
                    navController.navigate(AppRoutes.wordDetail(word))
                },
                viewModel = searchViewModel
            )
        }

        // 单词详情页面
        composable(
            route = AppRoutes.WORD_DETAIL,
            arguments = listOf(
                androidx.navigation.NavArgument("word") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedWord = backStackEntry.arguments?.getString("word") ?: ""
            val word = AppRoutes.decodeWord(encodedWord)

            val wordDetailViewModel: WordDetailViewModel = viewModel(factory = viewModelFactory)

            // 进入页面时加载单词详情
            LaunchedEffect(word) {
                wordDetailViewModel.loadWordDetail(word)
            }

            WordDetailScreen(
                word = word,
                onBackClick = {
                    navController.popBackStack()
                    wordDetailViewModel.clear()
                },
                viewModel = wordDetailViewModel
            )
        }
    }
}