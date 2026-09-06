package com.arhiplabs.unstuckly.data.db

import androidx.room.*
import com.arhiplabs.unstuckly.data.model.EarnSession
import com.arhiplabs.unstuckly.data.model.BlockAttempt
import kotlinx.coroutines.flow.Flow

@Dao
interface EarnSessionDao {
    @Insert
    suspend fun insertEarnSession(session: EarnSession): Long

    @Query("SELECT * FROM earn_sessions ORDER BY startAt DESC LIMIT 20")
    fun getRecentEarnSessions(): Flow<List<EarnSession>>

    @Insert
    suspend fun insertBlockAttempt(attempt: BlockAttempt): Long
}
