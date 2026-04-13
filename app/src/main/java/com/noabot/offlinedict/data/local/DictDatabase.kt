package com.noabot.offlinedict.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.noabot.offlinedict.data.local.entity.DictEntry
import com.noabot.offlinedict.data.local.entity.DictEntryFts

@Database(
    entities = [DictEntry::class, DictEntryFts::class],
    version = 1,
    exportSchema = false
)
abstract class DictDatabase : RoomDatabase() {

    abstract fun dictDao(): DictDao

    companion object {
        private const val DATABASE_NAME = "ecdict.db"

        @Volatile
        private var INSTANCE: DictDatabase? = null

        fun getInstance(context: Context): DictDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): DictDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                DictDatabase::class.java,
                DATABASE_NAME
            )
                .createFromAsset(DATABASE_NAME)
                .build()
        }
    }
}
