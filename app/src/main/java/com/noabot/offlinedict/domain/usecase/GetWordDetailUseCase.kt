package com.noabot.offlinedict.domain.usecase

import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.data.repository.DictRepository
import kotlinx.coroutines.flow.Flow

/**
 * 获取单词详情用例
 *
 * 封装单词详情查询逻辑，精确匹配查询单词
 *
 * @property repository 词典数据仓库
 */
open class GetWordDetailUseCase(
    private val repository: DictRepository
) : UseCase<String, DictEntry?> {

    /**
     * 执行查询
     *
     * @param param 要查询的单词
     * @return 单词详情 Flow，未找到时返回 null
     */
    override fun invoke(param: String): Flow<DictEntry?> {
        return repository.getWordDetail(param)
    }
}