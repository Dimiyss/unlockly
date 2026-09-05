package com.antigravity.unlockly.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.antigravity.unlockly.data.model.Rule
import com.antigravity.unlockly.data.model.Wallet
import com.antigravity.unlockly.data.model.EarnSession
import com.antigravity.unlockly.data.model.BlockAttempt

@Database(
    entities = [Rule::class, Wallet::class, EarnSession::class, BlockAttempt::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun ruleDao(): RuleDao
    abstract fun walletDao(): WalletDao
    abstract fun earnSessionDao(): EarnSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "unlockly_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
