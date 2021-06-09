package com.darkvyl.traveler

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "place")
data class PlaceDto(
        @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
        val path: String,
        var note: String
)