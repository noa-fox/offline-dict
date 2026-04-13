package com.noabot.offlinedict.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts5
import androidx.room.PrimaryKey

/**
 * 词典条目实体类 - 对应 ecdict 数据库表
 * 字段说明：
 * - word: 单词本身
 * - sw: 排序用单词（小写，用于排序和索引）
 * - phonetic: 英式音标
 * - phoneticUs: 美式音标
 * - definition: 中文释义（多行，用 \n 分隔）
 * - translation: 英文翻译/解释
 * - pos: 词性（如 n., v., adj. 等）
 * - collins: 柯林斯星级（0-5）
 * - oxford: 是否牛津核心词汇（0/1）
 * - tag: 标签（如 zk/中考, gk/高考, cet4/四级 等）
 * - bnc: 英国国家语料库词频
 * - frq: 其他词频数据
 * - exchange: 词形变化（如 过去式/过去分词/复数 等）
 * - detail: JSON 格式的详细信息
 * - audio: 音频文件名
 */
@Entity(tableName = "ecdict")
data class DictEntry(
    @PrimaryKey
    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "sw")
    val sw: String = word.lowercase(),

    @ColumnInfo(name = "phonetic")
    val phonetic: String = "",

    @ColumnInfo(name = "phonetic_us")
    val phoneticUs: String = "",

    @ColumnInfo(name = "definition")
    val definition: String = "",

    @ColumnInfo(name = "translation")
    val translation: String = "",

    @ColumnInfo(name = "pos")
    val pos: String = "",

    @ColumnInfo(name = "collins")
    val collins: Int = 0,

    @ColumnInfo(name = "oxford")
    val oxford: Int = 0,

    @ColumnInfo(name = "tag")
    val tag: String = "",

    @ColumnInfo(name = "bnc")
    val bnc: Int = 0,

    @ColumnInfo(name = "frq")
    val frq: Int = 0,

    @ColumnInfo(name = "exchange")
    val exchange: String = "",

    @ColumnInfo(name = "detail")
    val detail: String = "",

    @ColumnInfo(name = "audio")
    val audio: String = ""
)

/**
 * FTS5 全文搜索虚拟表
 * 用于高性能的全文检索，支持前缀匹配和模糊搜索
 */
@Fts5(
    contentEntity = DictEntry::class,
    contentRowId = "word"
)
@Entity(tableName = "ecdict_fts")
data class DictEntryFts(
    @ColumnInfo(name = "word")
    val word: String,

    @ColumnInfo(name = "definition")
    val definition: String = "",

    @ColumnInfo(name = "translation")
    val translation: String = ""
)
