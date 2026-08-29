package com.antigravity.unlockly.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class Rule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productivePackages: List<String>,
    val blockedPackages: List<String>,
    val productiveMinutesTarget: Int = 30,
    val rewardMinutes: Int = 20,
    val dailyCapMinutes: Int = 120,
    val emergencyPassword: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

