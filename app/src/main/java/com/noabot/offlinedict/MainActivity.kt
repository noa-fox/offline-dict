package com.noabot.offlinedict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.noabot.offlinedict.data.local.DictDatabase
import com.noabot.offlinedict.data.repository.DictRepository
import com.noabot.offlinedict.domain.usecase.GetWordDetailUseCase
import com.noabot.offlinedict.domain.usecase.SearchWordsUseCase
import com.noabot.offlinedict.ui.navigation.AppNavigation
import com.noabot.offlinedict.ui.theme.OfflineDictTheme
import com.noabot.offlinedict.ui.viewmodel.DictViewModelFactory

/**
 * 应用主入口 Activity
 *
 * 负责：
 * - 初始化数据库和依赖
 * - 设置 Compose UI
 * - 配置导航
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用边到边显示
        enableEdgeToEdge()

        // 初始化依赖
        val database = DictDatabase.getInstance(this)
        val repository = DictRepository(database.dictDao())
        val searchWordsUseCase = SearchWordsUseCase(repository)
        val getWordDetailUseCase = GetWordDetailUseCase(repository)
        val viewModelFactory = DictViewModelFactory(
            searchWordsUseCase = searchWordsUseCase,
            getWordDetailUseCase = getWordDetailUseCase
        )

        setContent {
            OfflineDictTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModelFactory = viewModelFactory)
                }
            }
        }
    }
}