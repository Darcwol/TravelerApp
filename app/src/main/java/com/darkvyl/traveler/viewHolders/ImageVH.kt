package com.darkvyl.traveler.viewHolders

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.darkvyl.traveler.databinding.ListImageViewBinding

class ImageVH(private val binding: ListImageViewBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(bitmap: Bitmap) {
        binding.place.setImageBitmap(bitmap)
    }
}