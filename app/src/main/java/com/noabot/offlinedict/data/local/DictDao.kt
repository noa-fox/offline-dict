package com.noabot.offlinedict.data.local

import androidx.room.Dao
import androidx.room.Query
import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.data.local.entity.DictEntryFts

@Dao
interface DictDao {

    /**
     * 前缀匹配搜索
     * 根据输入前缀查找单词，按字母排序
     * 
     * @param prefix 搜索前缀
     * @param limit 返回数量限制，默认50条
     * @return 匹配的词条列表
     */
    @Query(
        """
        SELECT * FROM ecdict
        WHERE word LIKE :prefix || '%'
        ORDER BY word ASC
        LIMIT :limit
        """
    )
    suspend fun searchByPrefix(prefix: String, limit: Int = 50): List<DictEntry>

    /**
     * 模糊搜索（FTS5 全文搜索）
     * 在 word、definition、translation 中进行全文搜索
     * 按相关性排序（bm25 排名函数，分数越低越相关）
     * 
     * @param query 搜索关键词
     * @param limit 返回数量限制，默认50条
     * @return 匹配的词条列表
     */
    @Query(
        """
        SELECT ecdict.* FROM ecdict
        JOIN ecdict_fts ON ecdict.word = ecdict_fts.word
        WHERE ecdict_fts MATCH :query
        ORDER BY bm25(ecdict_fts)
        LIMIT :limit
        """
    )
    suspend fun searchFuzzy(query: String, limit: Int = 50): List<DictEntry>

    /**
     * 精确查询
     * 根据单词精确查找单个词条
     * 
     * @param word 要查询的单词
     * @return 匹配的词条，不存在则返回 null
     */
    @Query("SELECT * FROM ecdict WHERE word = :word")
    suspend fun findByWord(word: String): DictEntry?
}
