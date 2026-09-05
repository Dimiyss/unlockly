package com.arhiplabs.unstuckly.data.db

import androidx.room.*
import com.arhiplabs.unstuckly.data.model.Rule
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules WHERE isActive = 1")
    fun getActiveRules(): Flow<List<Rule>>

    @Query("SELECT * FROM rules WHERE isActive = 1 LIMIT 1")
    suspend fun getPrimaryActiveRule(): Rule?

    @Query("SELECT * FROM rules")
    fun getAllRules(): Flow<List<Rule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: Rule): Long

    @Update
    suspend fun updateRule(rule: Rule)

    @Delete
    suspend fun deleteRule(rule: Rule)
}
