package com.jericx.trainr.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json

class Converters {

    @TypeConverter
    fun fromStringList(value: String): List<String> = Json.decodeFromString(value)

    @TypeConverter
    fun fromListString(list: List<String>): String = Json.encodeToString(list)
}
