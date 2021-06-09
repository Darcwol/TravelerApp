package com.darkvyl.traveler

import android.graphics.Bitmap
import com.darkvyl.traveler.database.AppDatabase

object Shared {
    val imageList = mutableListOf<Bitmap>()
    var db: AppDatabase? = null
}