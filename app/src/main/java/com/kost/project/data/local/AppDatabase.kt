package com.kost.project.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kost.project.data.model.AttendanceRecord
import com.kost.project.data.model.GlobalKasbonEntry
import com.kost.project.data.model.GlobalWeeklySummary
import com.kost.project.data.model.PrivateKasbonEntry
import com.kost.project.data.model.Transaction
import com.kost.project.data.model.WeeklyAdjustment
import com.kost.project.data.model.Worker

@Database(
    entities = [
        Transaction::class,
        Worker::class,
        AttendanceRecord::class,
        WeeklyAdjustment::class,
        GlobalWeeklySummary::class,
        GlobalKasbonEntry::class,
        PrivateKasbonEntry::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun workerDao(): WorkerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "project_kost_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
