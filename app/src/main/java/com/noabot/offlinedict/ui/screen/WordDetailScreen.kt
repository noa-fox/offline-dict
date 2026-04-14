package com.noabot.offlinedict.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.ui.viewmodel.WordDetailViewModel

/**
 * 单词详情界面
 *
 * 展示完整单词信息：
 * - 单词标题和音标
 * - 词性和星级标记
 * - 中文释义（全部）
 * - 英文翻译
 * - 词形变化
 *
 * @param word 要查看的单词
 * @param onBackClick 返回回调
 * @param viewModel 详情 ViewModel
 * @param modifier Modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    word: String,
    onBackClick: () -> Unit,
    viewModel: WordDetailViewModel,
    modifier: Modifier = Modifier
) {
    val wordDetail by viewModel.wordDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "单词详情") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                error != null -> {
                    ErrorContent(
                        message = error!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                wordDetail != null -> {
                    WordDetailContent(
                        entry = wordDetail!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    // 初始状态，等待加载
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * 单词详情内容
 *
 * @param entry 词典条目
 * @param modifier Modifier
 */
@Composable
private fun WordDetailContent(
    entry: DictEntry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // 单词标题
        Text(
            text = entry.word,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 音标行
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (entry.phonetic.isNotEmpty()) {
                PhoneticItem(
                    label = "英",
                    phonetic = entry.phonetic
                )
            }
            if (entry.phoneticUs.isNotEmpty()) {
                PhoneticItem(
                    label = "美",
                    phonetic = entry.phoneticUs
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 词性和星级标记
        @OptIn(ExperimentalLayoutApi::class)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (entry.pos.isNotEmpty()) {
                TagChip(text = entry.pos)
            }
            if (entry.collins > 0) {
                TagChip(text = "柯林斯 ${entry.collins} 星")
            }
            if (entry.oxford == 1) {
                TagChip(text = "牛津核心")
            }
            if (entry.tag.isNotEmpty()) {
                entry.tag.split("/").forEach { tag ->
                    if (tag.isNotEmpty()) {
                        TagChip(text = tag.trim())
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 中文释义
        if (entry.definition.isNotEmpty()) {
            DetailSection(
                title = "释义",
                content = entry.definition
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 英文翻译
        if (entry.translation.isNotEmpty()) {
            DetailSection(
                title = "翻译",
                content = entry.translation
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 词形变化
        if (entry.exchange.isNotEmpty()) {
            ExchangeSection(exchange = entry.exchange)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * 音标项
 *
 * @param label 标签（英/美）
 * @param phonetic 音标内容
 */
@Composable
private fun PhoneticItem(
    label: String,
    phonetic: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = phonetic,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 标签芯片
 *
 * @param text 标签文本
 */
@Composable
private fun TagChip(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * 详情区块
 *
 * @param title 区块标题
 * @param content 区块内容
 */
@Composable
private fun DetailSection(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 词形变化区块
 *
 * 解析 exchange 字段并展示词形变化
 * 格式示例："/p/past/p/过去式/d/past_participle/d/过去分词/s/plural/s/复数"
 *
 * @param exchange 词形变化原始数据
 */
@Composable
private fun ExchangeSection(
    exchange: String,
    modifier: Modifier = Modifier
) {
    val exchangeItems = parseExchange(exchange)

    if (exchangeItems.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "词形变化",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))

        exchangeItems.forEach { (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "$label:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * 解析 exchange 字段
 *
 * exchange 字段格式：/类型代码/值/类型代码/值...
 * 类型代码：
 * - p: 过去式 (past)
 * - d: 过去分词 (past participle)
 * - i: 现在分词 (present participle/ing form)
 * - 3: 第三人称单数 (third person singular)
 * - s: 复数 (plural)
 * - r: 比较级 (comparative)
 * - t: 最高级 (superlative)
 * - f: 反义词 (antonym)
 * - m: 复合词 (compound)
 *
 * @param exchange 原始 exchange 字符串
 * @return 解析后的词形变化列表 (标签, 值)
 */
private fun parseExchange(exchange: String): List<Pair<String, String>> {
    if (exchange.isEmpty()) return emptyList()

    val result = mutableListOf<Pair<String, String>>()
    val parts = exchange.split("/").filter { it.isNotEmpty() }

    // 每两个元素为一组：类型代码 + 值
    var i = 0
    while (i + 1 < parts.size) {
        val typeCode = parts[i]
        val value = parts[i + 1]

        val label = when (typeCode) {
            "p" -> "过去式"
            "d" -> "过去分词"
            "i" -> "现在分词"
            "3" -> "第三人称单数"
            "s" -> "复数"
            "r" -> "比较级"
            "t" -> "最高级"
            "f" -> "反义词"
            "m" -> "复合词"
            else -> typeCode
        }

        if (value.isNotEmpty()) {
            result.add(label to value)
        }

        i += 2
    }

    return result
}

/**
 * 错误状态内容
 *
 * @param message 错误信息
 */
@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}