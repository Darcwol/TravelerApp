package com.darkvyl.traveler.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.darkvyl.traveler.PlaceDto

@Database(
    entities = [PlaceDto::class],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract val place: PlaceDao

    companion object {
        fun open(context: Context) = Room.databaseBuilder(context,
            AppDatabase::class.java, "placesdb").build()
    }
}