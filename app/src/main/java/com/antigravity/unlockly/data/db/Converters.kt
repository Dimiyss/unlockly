package com.antigravity.unlockly.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromListToString(list: List<String>?): String {
        return list?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun fromStringToList(data: String?): List<String> {
        if (data.isNullOrEmpty()) return emptyList()
        return data.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
