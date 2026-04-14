package com.noabot.offlinedict.domain.usecase

import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.data.repository.DictRepository
import kotlinx.coroutines.flow.Flow

/**
 * 搜索单词用例
 *
 * 封装搜索业务逻辑：
 * - 空查询返回空列表
 * - 单词查询（无空格）使用前缀匹配
 * - 短语查询使用 FTS5 模糊搜索
 *
 * @property repository 词典数据仓库
 */
open class SearchWordsUseCase(
    private val repository: DictRepository
) : UseCase<SearchWordsUseCase.Params, List<DictEntry>> {

    /**
     * 执行搜索
     *
     * @param param 搜索参数
     * @return 搜索结果 Flow
     */
    override fun invoke(param: Params): Flow<List<DictEntry>> {
        return repository.searchWords(
            query = param.query,
            limit = param.limit
        )
    }

    /**
     * 便捷方法：使用默认限制搜索
     */
    operator fun invoke(query: String): Flow<List<DictEntry>> {
        return invoke(Params(query = query))
    }

    /**
     * 搜索参数
     *
     * @param query 搜索关键词
     * @param limit 返回结果数量限制，默认 50
     */
    data class Params(
        val query: String,
        val limit: Int = DEFAULT_LIMIT
    ) {
        companion object {
            const val DEFAULT_LIMIT = 50
        }
    }
}