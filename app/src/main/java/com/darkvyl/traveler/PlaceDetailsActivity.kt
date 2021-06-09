package com.darkvyl.traveler

import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.core.os.HandlerCompat
import com.darkvyl.traveler.databinding.ActivityPlaceDetailsBinding
import kotlin.concurrent.thread

class PlaceDetailsActivity : AppCompatActivity() {
    private val binding by lazy { ActivityPlaceDetailsBinding.inflate(layoutInflater) }
    private val id by lazy { intent.extras!!.getLong("placeId") }
    private val handler = HandlerCompat.createAsync(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        thread {
            val place = Shared.db?.place?.get(id)
            handler.post {
                binding.placeDetails.setText(place?.note)
                binding.placePhoto.setImageBitmap(BitmapFactory.decodeFile(place?.path))
            }
        }
    }

    fun save(view: View) {
        thread {
            val place = Shared.db?.place?.get(id)
            place?.note = binding.placeDetails.text.toString()
            if (place != null) {
                Shared.db?.place?.insert(place)
            }
            finish()
        }
    }


}