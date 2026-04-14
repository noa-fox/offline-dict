# OfflineDict 技术架构文档

> 最后更新：2026-04-13

## 1. 项目概述

OfflineDict 是一款纯离线的 Android 英汉词典应用，内置 ECDICT 开源词典数据（约 300 万词条），支持前缀匹配和全文搜索功能。

### 1.1 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9.20 | 开发语言 |
| Jetpack Compose | BOM 2023.10.01 | UI 框架 |
| Room | 2.6.1 | 数据库 ORM |
| SQLite FTS5 | 内置 | 全文搜索引擎 |
| Coroutines | 1.7.3 | 异步编程 |
| Lifecycle ViewModel | 2.6.2 | MVVM 架构组件 |

### 1.2 构建配置

- **minSdk**: 29 (Android 10)
- **compileSdk / targetSdk**: 34 (Android 14)
- **Java**: 17
- **AGP**: 8.2.0
- **Kapt**: 已启用（Room 注解处理）
- **ProGuard**: Release 构建已启用代码混淆

---

## 2. 项目架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                     Presentation Layer (UI)                      │
│                                                                  │
│  ┌──────────────────┐       ┌───────────────────────────────┐   │
│  │  SearchScreen     │       │  WordDetailScreen              │   │
│  │  (搜索界面)       │       │  (单词详情界面)                 │   │
│  │                   │       │                                │   │
│  │  - SearchBar      │       │  - 单词 & 音标                 │   │
│  │  - WordList       │       │  - 词性 & 释义                 │   │
│  │  - LoadingState   │       │  - 词形变化                    │   │
│  └────────┬─────────┘       └───────────────┬────────────────┘   │
│           │                                 │                    │
│           ▼                                 ▼                    │
│  ┌──────────────────────────────────────────────────────┐       │
│  │                    ViewModels                         │       │
│  │  ┌────────────────────┐  ┌──────────────────────┐    │       │
│  │  │ SearchViewModel     │  │ WordDetailViewModel   │    │       │
│  │  │ - searchQuery       │  │ - selectedWord        │    │       │
│  │  │ - searchResults     │  │ - wordDetail          │    │       │
│  │  │ - onQueryChange()   │  │ - loadDetail()        │    │       │
│  │  └─────────┬──────────┘  └──────────┬───────────┘    │       │
│  └────────────┼────────────────────────┼────────────────┘       │
└───────────────┼────────────────────────┼────────────────────────┘
                │                        │
                ▼                        ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Domain Layer                             │
│                                                                  │
│  ┌────────────────────────┐  ┌────────────────────────────┐    │
│  │ SearchWordsUseCase      │  │ GetWordDetailUseCase        │    │
│  │ (搜索用例)              │  │ (详情查询用例)               │    │
│  └────────────┬───────────┘  └────────────┬───────────────┘    │
│               │                           │                    │
│               └─────────────┬─────────────┘                    │
│                             ▼                                  │
│               ┌─────────────────────────┐                      │
│               │   DictRepository        │                      │
│               │   (数据仓库抽象)         │                      │
│               └───────────┬─────────────┘                      │
└───────────────────────────┼────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Data Layer                             │
│                                                                  │
│               ┌─────────────────────────┐                       │
│               │      DictDao            │                       │
│               │  (数据访问对象)          │                       │
│               │                         │                       │
│               │  - searchByPrefix()     │                       │
│               │  - searchFuzzy()        │                       │
│               │  - findByWord()         │                       │
│               └───────────┬─────────────┘                       │
│                           │                                     │
│                           ▼                                     │
│               ┌─────────────────────────┐                       │
│               │    DictDatabase         │                       │
│               │  (Room 数据库单例)       │                       │
│               │                         │                       │
│               │  createFromAsset()      │                       │
│               │  预填充 ecdict.db        │                       │
│               └───────────┬─────────────┘                       │
│                           │                                     │
│                           ▼                                     │
│  ┌─────────────────────────────────────────────────────┐        │
│  │                SQLite Storage                        │        │
│  │  ┌──────────────────┐    ┌──────────────────────┐   │        │
│  │  │  ecdict (主表)    │◄───│ ecdict_fts (FTS5表)  │   │        │
│  │  │  15 字段完整数据  │    │ 全文搜索虚拟表        │   │        │
│  │  │  ~300 万词条      │    │ word+definition+     │   │        │
│  │  │                  │    │ translation           │   │        │
│  │  └──────────────────┘    └──────────────────────┘   │        │
│  └─────────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心模块说明

### 3.1 数据层 (data/) — ✅ 已实现

| 文件 | 路径 | 职责 | 状态 |
|------|------|------|------|
| **DictEntry.kt** | `data/local/entity/` | 词典条目实体类，映射 `ecdict` 表（15 字段）；包含 `DictEntryFts` 映射 FTS5 虚拟表 | ✅ |
| **DictDatabase.kt** | `data/local/` | Room 数据库定义，单例模式，使用 `createFromAsset()` 预填充 `ecdict.db` | ✅ |
| **DictDao.kt** | `data/local/` | 数据访问接口，提供三种查询：前缀匹配、FTS5 模糊搜索、精确查询 | ✅ |
| **DictRepository.kt** | `data/repository/` | 数据仓库层，封装 DAO 调用，智能选择搜索策略（前缀/模糊），返回 `Flow` 响应式流 | ✅ |

### 3.2 领域层 (domain/) — ✅ 已实现

| 文件 | 路径 | 职责 | 状态 |
|------|------|------|------|
| **UseCase.kt** | `domain/usecase/` | UseCase 基础接口定义 | ✅ |
| **SearchWordsUseCase** | `domain/usecase/` | 封装搜索业务逻辑，接收查询参数，返回 `Flow<List<DictEntry>>` | ✅ |
| **GetWordDetailUseCase** | `domain/usecase/` | 封装单词详情查询逻辑，返回 `Flow<DictEntry?>` | ✅ |

### 3.3 表现层 (ui/) — ❌ 待实现

| 文件 | 路径 | 职责 | 状态 |
|------|------|------|------|
| **MainActivity.kt** | `ui/` | 应用入口 Activity，承载 Compose 导航 | ❌ |
| **SearchScreen.kt** | `ui/screen/` | 搜索界面 Composable：搜索栏 + 结果列表 | ❌ |
| **WordDetailScreen.kt** | `ui/screen/` | 单词详情界面 Composable：音标、释义、词形变化等 | ❌ |
| **SearchViewModel.kt** | `ui/viewmodel/` | 搜索页面 ViewModel：管理搜索状态、防抖、结果流 | ✅ |
| **WordDetailViewModel.kt** | `ui/viewmodel/` | 详情页面 ViewModel：管理选中单词、加载详情 | ✅ |
| **AppNavigation.kt** | `ui/navigation/` | Compose Navigation 路由配置 | ❌ |
| **WordCard.kt** | `ui/component/` | 可复用的单词卡片组件 | ❌ |
| **SearchBar.kt** | `ui/component/` | 可复用的搜索栏组件 | ❌ |
| **OfflineDictTheme.kt** | `ui/theme/` | Compose 主题配置（颜色、字体、形状） | ❌ |

### 3.4 工具/脚本 — ❌ 待实现

| 文件 | 路径 | 职责 | 状态 |
|------|------|------|------|
| **import_ecdict.py** | `scripts/` | Python 脚本：将 ECDICT CSV 导入 SQLite 并生成预填充数据库 | ❌ |
| **ecdict.db** | `app/src/main/assets/` | 预填充的 SQLite 数据库文件（由脚本生成） | ❌ |

### 3.5 配置/资源 — ✅ 已实现

| 文件 | 路径 | 职责 | 状态 |
|------|------|------|------|
| **build.gradle.kts** (根) | 项目根目录 | 插件版本声明（AGP 8.2.0, Kotlin 1.9.20） | ✅ |
| **app/build.gradle.kts** | app/ | 应用模块配置、依赖声明、编译选项 | ✅ |
| **settings.gradle.kts** | 项目根目录 | 仓库配置、模块声明 | ✅ |
| **AndroidManifest.xml** | app/src/main/ | 应用清单，声明 MainActivity | ✅ |
| **strings.xml** | app/src/main/res/values/ | 字符串资源 | ✅ |
| **themes.xml** | app/src/main/res/values/ | 应用主题（Material Light NoActionBar） | ✅ |
| **.gitignore** | 项目根目录 | Git 忽略规则 | ✅ |

---

## 4. 数据流说明

### 4.1 搜索流程（完整链路）

```
用户输入 "app"
       │
       ▼
┌─────────────────────┐
│   SearchScreen      │  ← Compose TextField
│   onQueryChange()   │
└─────────┬───────────┘
          │ "app"
          ▼
┌─────────────────────────┐
│   SearchViewModel       │
│                         │
│   searchQuery           │  ← MutableStateFlow<String>
│     .debounce(300ms)    │  ← 防抖：避免频繁查询
│     .distinctUntilChanged()
│     .flatMapLatest { q →│  ← 切换最新查询，取消旧请求
│       repository.       │
│         searchWords(q)  │
│     }                   │
│     .stateIn(...)       │  ← 转为 StateFlow 供 UI 收集
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────┐
│   DictRepository        │
│                         │
│   searchWords("app")    │
│   ├─ 不含空格 → 单单词  │
│   └─ dictDao.           │
│        searchByPrefix() │
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────┐
│   DictDao               │
│                         │
│   @Query:               │
│   SELECT * FROM ecdict  │
│   WHERE word LIKE       │
│     'app%'              │
│   ORDER BY word ASC     │
│   LIMIT 50              │
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────┐
│   Room / SQLite         │
│   ecdict 表索引查询      │
│   返回 List<DictEntry>   │
└─────────┬───────────────┘
          │
          ▼  (逐层返回 Flow)
┌─────────────────────────┐
│   SearchViewModel       │  ← StateFlow 发射新状态
│   searchResults.emit()  │
└─────────┬───────────────┘
          │
          ▼
┌─────────────────────────┐
│   SearchScreen          │  ← LazyColumn 自动重组
│   展示搜索结果列表       │
└─────────────────────────┘
```

### 4.2 两种搜索策略对比

| 场景 | 输入示例 | 触发方法 | SQL 策略 | 排序 |
|------|----------|----------|----------|------|
| **单词输入** | `app` | `searchByPrefix` | `LIKE 'app%'` | 字母序 ASC |
| **短语搜索** | `apple pie` | `searchFuzzy` | `ecdict_fts MATCH 'apple pie'` | BM25 相关性 |
| **精确查词** | 点击列表项 | `findByWord` | `WHERE word = 'apple'` | — |

### 4.3 响应式数据流模式

```kotlin
// ViewModel 中的 Flow 链式处理
val searchResults: StateFlow<List<DictEntry>> = searchQuery
    .debounce(300)                              // 输入防抖 300ms
    .filter { it.isNotBlank() }                 // 过滤空查询
    .distinctUntilChanged()                     // 相同查询去重
    .flatMapLatest { query ->                   // 切换最新流（取消旧请求）
        repository.searchWords(query)
            .catch { emit(emptyList()) }        // 异常处理
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
```

---

## 5. ECDICT 数据导入方案

### 5.1 数据源说明

- **来源**: [ECDICT](https://github.com/skywind3000/ECDICT) (skywind3000/ECDICT)
- **许可证**: CC-BY-SA 4.0
- **原始文件**: `stardict.csv`（UTF-8 编码）
- **数据量**: 约 300 万词条
- **CSV 列顺序**: `word, sw, phonetic, phonetic_us, definition, translation, pos, collins, oxford, tag, bnc, frq, exchange, detail, audio`

### 5.2 Python 导入脚本设计

```python
#!/usr/bin/env python3
"""
ECDICT 数据导入脚本
将 stardict.csv 转换为预填充的 SQLite 数据库 ecdict.db
"""

import csv
import sqlite3
import os
import time

# 配置
CSV_FILE = "stardict.csv"
DB_FILE = "ecdict.db"
BATCH_SIZE = 10000  # 每批插入数量

def create_database(db_path):
    """创建数据库和表结构"""
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    
    # 创建主表
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS ecdict (
            word        TEXT PRIMARY KEY,
            sw          TEXT NOT NULL DEFAULT '',
            phonetic    TEXT NOT NULL DEFAULT '',
            phonetic_us TEXT NOT NULL DEFAULT '',
            definition  TEXT NOT NULL DEFAULT '',
            translation TEXT NOT NULL DEFAULT '',
            pos         TEXT NOT NULL DEFAULT '',
            collins     INTEGER NOT NULL DEFAULT 0,
            oxford      INTEGER NOT NULL DEFAULT 0,
            tag         TEXT NOT NULL DEFAULT '',
            bnc         INTEGER NOT NULL DEFAULT 0,
            frq         INTEGER NOT NULL DEFAULT 0,
            exchange    TEXT NOT NULL DEFAULT '',
            detail      TEXT NOT NULL DEFAULT '',
            audio       TEXT NOT NULL DEFAULT ''
        )
    """)
    
    # 创建 FTS5 虚拟表
    cursor.execute("""
        CREATE VIRTUAL TABLE IF NOT EXISTS ecdict_fts USING fts5(
            word,
            definition,
            translation,
            content='ecdict',
            content_rowid='word'
        )
    """)
    
    conn.commit()
    return conn

def import_csv(conn, csv_path):
    """批量导入 CSV 数据"""
    cursor = conn.cursor()
    batch = []
    count = 0
    start_time = time.time()
    
    with open(csv_path, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        for row in reader:
            # 确保字段数量正确（15 列）
            while len(row) < 15:
                row.append('')
            
            batch.append((
                row[0],   # word
                row[1],   # sw
                row[2],   # phonetic
                row[3],   # phonetic_us
                row[4],   # definition
                row[5],   # translation
                row[6],   # pos
                int(row[7]) if row[7] else 0,     # collins
                int(row[8]) if row[8] else 0,     # oxford
                row[9],   # tag
                int(row[10]) if row[10] else 0,   # bnc
                int(row[11]) if row[11] else 0,   # frq
                row[12],  # exchange
                row[13],  # detail
                row[14],  # audio
            ))
            
            # 批量插入
            if len(batch) >= BATCH_SIZE:
                cursor.executemany(
                    "INSERT OR IGNORE INTO ecdict VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    batch
                )
                conn.commit()
                count += len(batch)
                print(f"  已导入 {count:,} 条记录...")
                batch.clear()
    
    # 插入剩余数据
    if batch:
        cursor.executemany(
            "INSERT OR IGNORE INTO ecdict VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            batch
        )
        conn.commit()
        count += len(batch)
    
    elapsed = time.time() - start_time
    print(f"✅ 主表导入完成: {count:,} 条记录，耗时 {elapsed:.1f} 秒")
    return count

def build_fts_index(conn):
    """构建 FTS5 索引"""
    cursor = conn.cursor()
    print("🔨 构建 FTS5 索引...")
    start_time = time.time()
    
    # 将主表数据填充到 FTS5 表
    cursor.execute("""
        INSERT OR REPLACE INTO ecdict_fts(rowid, word, definition, translation)
        SELECT rowid, word, definition, translation FROM ecdict
    """)
    conn.commit()
    
    elapsed = time.time() - start_time
    print(f"✅ FTS5 索引构建完成，耗时 {elapsed:.1f} 秒")

def optimize_database(conn):
    """数据库优化"""
    cursor = conn.cursor()
    print("🔧 优化数据库...")
    
    # 分析统计信息
    cursor.execute("ANALYZE")
    conn.commit()
    
    # 获取数据库大小
    db_size = os.path.getsize(DB_FILE)
    print(f"📦 数据库大小: {db_size / 1024 / 1024:.1f} MB")

def main():
    print("=" * 50)
    print("ECDICT 数据导入工具")
    print("=" * 50)
    
    if not os.path.exists(CSV_FILE):
        print(f"❌ 找不到文件: {CSV_FILE}")
        return
    
    # 删除旧数据库
    if os.path.exists(DB_FILE):
        os.remove(DB_FILE)
    
    total_start = time.time()
    
    # 1. 创建数据库
    print("\n📋 创建数据库结构...")
    conn = create_database(DB_FILE)
    
    # 2. 导入 CSV
    print(f"\n📥 导入 {CSV_FILE}...")
    count = import_csv(conn, CSV_FILE)
    
    # 3. 构建 FTS5 索引
    print("\n🔍 构建全文搜索索引...")
    build_fts_index(conn)
    
    # 4. 优化
    print("\n⚙️  优化数据库...")
    optimize_database(conn)
    
    conn.close()
    
    total_elapsed = time.time() - total_start
    print(f"\n{'=' * 50}")
    print(f"✅ 全部完成！总计耗时 {total_elapsed:.1f} 秒")
    print(f"📦 输出文件: {DB_FILE}")
    print(f"📋 总词条数: {count:,}")
    print(f"{'=' * 50}")

if __name__ == "__main__":
    main()
```

### 5.3 性能优化要点

| 优化项 | 策略 | 预期效果 |
|--------|------|----------|
| **批量插入** | 每 10,000 条提交一次事务 | 比逐条插入快 100x+ |
| **事务处理** | 使用 `executemany` + 手动 `commit` | 减少 I/O 开销 |
| **INSERT OR IGNORE** | 跳过重复词条 | 避免主键冲突错误 |
| **FTS5 延迟构建** | 先导入主表，再填充 FTS5 | 避免插入时维护索引开销 |
| **ANALYZE** | 导入后更新统计信息 | 优化查询计划 |

### 5.4 300 万词条数据量估算

| 指标 | 估算值 | 说明 |
|------|--------|------|
| **CSV 文件大小** | ~800 MB | 原始 stardict.csv |
| **SQLite 数据库（无索引）** | ~600-800 MB | 主表数据 |
| **FTS5 索引** | ~200-300 MB | 全文搜索索引 |
| **总数据库大小** | ~800 MB - 1.1 GB | 包含主表 + FTS5 |
| **APK 大小** | ~400-550 MB | 数据库压缩后放入 APK |
| **导入时间** | 3-8 分钟 | 取决于磁盘 I/O 速度 |
| **安装后占用** | ~1-1.5 GB | 数据库解压到设备 |

> ⚠️ **注意**: 预填充数据库会使 APK 显著增大。如果 APK 超过 Google Play 的 150MB 限制，需考虑使用 Play Asset Delivery 或将数据库放在外部存储首次启动时导入。

### 5.5 脚本使用方式

```bash
# 1. 下载 ECDICT 数据
git clone https://github.com/skywind3000/ECDICT.git
cd ECDICT

# 2. 运行导入脚本
python3 import_ecdict.py

# 3. 将生成的 ecdict.db 复制到 Android 项目
cp ecdict.db /path/to/offline-dict/app/src/main/assets/
```

---

## 6. 当前进度表

### 6.1 按模块

| 模块 | 状态 | 完成度 | 说明 |
|------|:----:|:------:|------|
| **Gradle 构建配置** | ✅ | 100% | 根 + app 级 build.gradle.kts，依赖齐全 |
| **数据模型 (Entity)** | ✅ | 100% | DictEntry + DictEntryFts，15 字段完整映射 |
| **Room 数据库** | ✅ | 100% | DictDatabase 单例，createFromAsset 预填充 |
| **DAO 接口** | ✅ | 100% | 前缀匹配、FTS5 搜索、精确查询 |
| **Repository 层** | ✅ | 100% | 智能搜索策略，Flow 响应式返回 |
| **Manifest & 资源** | ✅ | 100% | Manifest、strings.xml、themes.xml |
| **数据导入脚本** | ❌ | 0% | Python 脚本设计完成，未实现 |
| **预填充数据库** | ❌ | 0% | 需先运行导入脚本生成 ecdict.db |
| **MainActivity** | ❌ | 0% | Manifest 中已声明，类未实现 |
| **Compose UI** | ❌ | 0% | 搜索界面 + 详情界面待开发 |
| **ViewModel 层** | ❌ | 0% | SearchViewModel + WordDetailViewModel |
| **Domain UseCases** | ✅ | 100% | SearchWordsUseCase + GetWordDetailUseCase + UseCase 基类 |
| **搜索历史** | ❌ | 0% | 历史记录存储与展示 |
| **收藏功能** | ❌ | 0% | 收藏单词的增删查 |
| **词形变化处理** | ❌ | 0% | exchange 字段解析与展示 |
| **发音功能** | ❌ | 0% | TTS 或预置音频播放 |
| **导航路由** | ❌ | 0% | Compose Navigation 配置 |
| **主题系统** | ❌ | 0% | Compose 主题（颜色、字体、暗色模式） |

### 6.2 按文件

| 文件 | 状态 |
|------|:----:|
| `build.gradle.kts` (根) | ✅ |
| `app/build.gradle.kts` | ✅ |
| `settings.gradle.kts` | ✅ |
| `DictEntry.kt` | ✅ |
| `DictDatabase.kt` | ✅ |
| `DictDao.kt` | ✅ |
| `DictRepository.kt` | ✅ |
| `AndroidManifest.xml` | ✅ |
| `strings.xml` | ✅ |
| `themes.xml` | ✅ |
| `scripts/import_ecdict.py` | ✅ |
| `app/src/main/assets/ecdict.db` | ❌ |
| `ui/MainActivity.kt` | ❌ |
| `ui/screen/SearchScreen.kt` | ❌ |
| `ui/screen/WordDetailScreen.kt` | ❌ |
| `ui/viewmodel/SearchViewModel.kt` | ✅ |
| `ui/viewmodel/WordDetailViewModel.kt` | ✅ |
| `ui/component/WordCard.kt` | ❌ |
| `ui/component/SearchBar.kt` | ❌ |
| `ui/navigation/AppNavigation.kt` | ❌ |
| `domain/usecase/UseCase.kt` | ✅ |
| `domain/usecase/SearchWordsUseCase.kt` | ✅ |
| `domain/usecase/GetWordDetailUseCase.kt` | ✅ |

---

## 7. 下一步开发计划

### P0 — 最高优先级（数据先行）

| # | 任务 | 预计工时 | 说明 |
|---|------|----------|------|
| 1 | **编写 ECDICT 导入脚本** | 2-3 小时 | Python 脚本，CSV → SQLite |
| 2 | **生成预填充数据库** | 10-30 分钟 | 运行脚本，生成 ecdict.db |
| 3 | **放置数据库到 assets/** | 5 分钟 | 复制到 `app/src/main/assets/` |
| 4 | **验证数据库可正常查询** | 1 小时 | 编写单元测试验证 DAO 查询 |

### P1 — UI 层（核心交互）

| # | 任务 | 预计工时 | 说明 |
|---|------|----------|------|
| 5 | **实现 MainActivity** | 1 小时 | Compose Activity 入口 |
| 6 | **实现 SearchScreen** | 3-4 小时 | 搜索栏 + 结果列表 |
| 7 | **实现 WordDetailScreen** | 3-4 小时 | 单词详情展示 |
| 8 | **实现 Compose 导航** | 1-2 小时 | 列表 → 详情页面跳转 |

### P2 — ViewModel 层（状态管理）

| # | 任务 | 预计工时 | 说明 |
|---|------|----------|------|
| 9 | **实现 SearchViewModel** | 2-3 小时 | 搜索状态、防抖、Flow 处理 |
| 10 | **实现 WordDetailViewModel** | 1-2 小时 | 详情加载、状态管理 |
| 11 | **实现依赖注入** | 1-2 小时 | 手动注入或引入 Hilt/Koin |

### P3 — Domain 层（业务逻辑）

| # | 任务 | 预计工时 | 说明 |
|---|------|----------|------|
| 12 | **实现 SearchWordsUseCase** | 1 小时 | 封装搜索逻辑 |
| 13 | **实现 GetWordDetailUseCase** | 1 小时 | 封装详情查询逻辑 |
| 14 | **词形变化解析** | 2-3 小时 | 解析 exchange 字段，展示变形 |

### P4 — 增强功能（锦上添花）

| # | 任务 | 预计工时 | 说明 |
|---|------|----------|------|
| 15 | **搜索历史记录** | 2-3 小时 | Room 存储最近搜索 |
| 16 | **收藏单词功能** | 3-4 小时 | 收藏表 + UI |
| 17 | **发音功能** | 2-3 小时 | Android TTS 或预置音频 |
| 18 | **暗色主题** | 1-2 小时 | Compose 暗色模式适配 |
| 19 | **性能优化** | 2-3 小时 | 列表分页、图片缓存等 |

---

## 8. 技术决策记录

### 8.1 为什么选择 FTS5？

| 方案 | 优点 | 缺点 |
|------|------|------|
| LIKE 查询 | 简单易用 | 300 万数据下性能差，不支持相关性排序 |
| SQLite FTS4 | 成熟稳定 | 不支持 phrase 查询优化 |
| **SQLite FTS5** ✅ | 高性能，BM25 相关性排序，短语搜索 | 需要 Room 2.4+ 支持 |

### 8.2 为什么使用 createFromAsset 预填充？

- **离线优先**: 无需网络即可使用全部功能
- **启动快速**: 避免首次启动时的数据下载和处理
- **用户体验**: 开箱即用，零配置

> ⚠️ 风险：APK 体积会显著增大（~400-550MB）。如超出应用商店限制，需改用 Play Asset Delivery 或首次启动时从外部存储导入。

### 8.3 为什么选择 Room？

- **类型安全**: 编译时检查 SQL 语句正确性
- **Flow 支持**: 原生支持 Kotlin Flow 响应式编程
- **迁移支持**: 结构化的数据库版本管理
- **FTS5 集成**: Room 2.4+ 原生支持 `@Fts5` 注解

### 8.4 为什么使用 Flow 而非 LiveData？

- **冷流特性**: `flatMapLatest` 可自动取消旧查询
- **Kotlin 原生**: 与协程生态无缝集成
- **操作符丰富**: `debounce`、`distinctUntilChanged`、`catch` 等

---

## 9. 性能考量

### 9.1 数据库查询优化

- **前缀搜索**: 利用 `word` 主键索引，LIKE 前缀匹配可走索引
- **FTS5 搜索**: BM25 算法自动按相关性排序，无需额外索引
- **结果限制**: 默认 LIMIT 50，避免一次性加载过多数据
- **分页扩展**: 后续可引入 `OFFSET` 或 `keyset pagination`

### 9.2 UI 性能优化

- **输入防抖**: 300ms debounce 减少不必要的查询
- **LazyColumn**: Compose 延迟列表，只渲染可见项
- **key() 稳定**: 列表项使用 `word` 作为 key，避免不必要的重组
- **remember**: 缓存计算结果，减少重复计算

---

## 10. 参考资料

- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Guide to App Architecture](https://developer.android.com/topic/architecture#recommended-app-arch)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [SQLite FTS5 Extension](https://www.sqlite.org/fts5.html)
- [ECDICT 开源词典](https://github.com/skywind3000/ECDICT)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
