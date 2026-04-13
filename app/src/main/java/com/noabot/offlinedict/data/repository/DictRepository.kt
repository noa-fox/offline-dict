package com.noabot.offlinedict.data.repository

import com.noabot.offlinedict.data.local.DictDao
import com.noabot.offlinedict.data.local.entity.DictEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DictRepository(private val dictDao: DictDao) {

    /**
     * 搜索单词
     * - 空查询返回空列表
     * - 单词查询（无空格）使用前缀匹配
     * - 短语查询使用 FTS5 模糊搜索
     *
     * @param query 搜索关键词
     * @param limit 返回结果数量限制
     * @return 搜索结果 Flow
     */
    fun searchWords(query: String, limit: Int = 50): Flow<List<DictEntry>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }

        val trimmedQuery = query.trim()

        // 判断是否为单个单词（无空格）
        val isSingleWord = !trimmedQuery.contains(" ")

        val results = if (isSingleWord) {
            dictDao.searchByPrefix(trimmedQuery, limit)
        } else {
            dictDao.searchFuzzy(trimmedQuery, limit)
        }

        emit(results)
    }

    /**
     * 获取单词详情（精确匹配）
     *
     * @param word 要查询的单词
     * @return 单词详情 Flow，未找到时返回 null
     */
    fun getWordDetail(word: String): Flow<DictEntry?> = flow {
        emit(dictDao.findByWord(word.trim()))
    }
}
