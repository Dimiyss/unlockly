package com.arhiplabs.unstuckly.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earn_sessions")
data class EarnSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val startAt: Long,
    val endAt: Long,
    val activeSeconds: Long,
    val endedReason: String // e.g. "idle_timeout", "app_closed", "screen_off"
)
