package com.antigravity.unlockly.data.db

import androidx.room.*
import com.antigravity.unlockly.data.model.Wallet
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallet WHERE id = 1")
    fun getWalletFlow(): Flow<Wallet?>

    @Query("SELECT * FROM wallet WHERE id = 1")
    suspend fun getWallet(): Wallet?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: Wallet)
}
