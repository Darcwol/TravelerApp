package com.darkvyl.traveler.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.darkvyl.traveler.PlaceDto

@Dao
interface PlaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(placeDto: PlaceDto)

    @Query("SELECT * FROM place WHERE Id = :id;")
    fun get(id: Long): PlaceDto

    @Query("SELECT * FROM place;")
    fun getAll(): List<PlaceDto>
}