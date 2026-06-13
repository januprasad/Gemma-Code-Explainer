package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ExplanationEntity::class], version = 1, exportSchema = false)
abstract class ExplanationDatabase : RoomDatabase() {
    abstract val explanationDao: ExplanationDao

    companion object {
        @Volatile
        private var INSTANCE: ExplanationDatabase? = null

        fun getDatabase(context: Context): ExplanationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExplanationDatabase::class.java,
                    "explanation_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
