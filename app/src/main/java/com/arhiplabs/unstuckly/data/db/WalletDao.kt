package com.arhiplabs.unstuckly.data.db

import androidx.room.*
import com.arhiplabs.unstuckly.data.model.Wallet
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
