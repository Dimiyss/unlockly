package com.arhiplabs.unstuckly.fakes

import com.arhiplabs.unstuckly.data.db.WalletDao
import com.arhiplabs.unstuckly.data.model.Wallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeWalletDao(initialWallet: Wallet? = null) : WalletDao {

    private val _walletFlow = MutableStateFlow(initialWallet)

    override fun getWalletFlow(): Flow<Wallet?> = _walletFlow.asStateFlow()

    override suspend fun getWallet(): Wallet? = _walletFlow.value

    override suspend fun insertOrUpdateWallet(wallet: Wallet) {
        _walletFlow.value = wallet
    }
}
