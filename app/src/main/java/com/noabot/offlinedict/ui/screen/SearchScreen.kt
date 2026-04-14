package com.noabot.offlinedict.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.ui.component.SearchBar
import com.noabot.offlinedict.ui.component.WordCard
import com.noabot.offlinedict.ui.viewmodel.SearchViewModel

/**
 * 搜索界面
 *
 * 包含：
 * - 搜索栏（顶部）
 * - 搜索结果列表（LazyColumn）
 * - 加载状态
 * - 空状态提示
 *
 * @param onWordClick 点击单词回调，跳转到详情页
 * @param viewModel 搜索 ViewModel
 * @param modifier Modifier
 */
@Composable
fun SearchScreen(
    onWordClick: (String) -> Unit,
    viewModel: SearchViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.onQueryChange(it) },
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 内容区域
            when {
                // 空查询状态
                searchQuery.isEmpty() -> {
                    EmptyQueryContent()
                }

                // 有结果
                searchResults.isNotEmpty() -> {
                    SearchResultList(
                        results = searchResults,
                        onWordClick = onWordClick
                    )
                }

                // 无结果
                searchQuery.isNotEmpty() && searchResults.isEmpty() -> {
                    NoResultsContent(query = searchQuery)
                }
            }
        }
    }
}

/**
 * 搜索结果列表
 *
 * @param results 搜索结果列表
 * @param onWordClick 点击回调
 * @param modifier Modifier
 */
@Composable
private fun SearchResultList(
    results: List<DictEntry>,
    onWordClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = results,
            key = { it.word }
        ) { entry ->
            WordCard(
                entry = entry,
                onClick = { onWordClick(entry.word) }
            )
        }
    }
}

/**
 * 空查询状态内容
 */
@Composable
private fun EmptyQueryContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "离线词典",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "输入单词开始搜索",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "内置 300 万词条",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

/**
 * 无结果状态内容
 *
 * @param query 搜索关键词
 */
@Composable
private fun NoResultsContent(
    query: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "未找到 \"$query\"",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "请检查拼写或尝试其他关键词",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}