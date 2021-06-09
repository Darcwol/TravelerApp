package com.darkvyl.traveler.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.darkvyl.traveler.PlaceDetailsActivity
import com.darkvyl.traveler.Shared
import com.darkvyl.traveler.databinding.ListImageViewBinding
import com.darkvyl.traveler.viewHolders.ImageVH


class ImageAdapter : RecyclerView.Adapter<ImageVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVH {
        val binding = ListImageViewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageVH(binding).also { holder ->
            binding.place.setOnClickListener {
                val placeId = holder.layoutPosition + 1
                val intent = Intent(parent.context, PlaceDetailsActivity::class.java).also {
                    it.putExtra("placeId", placeId.toLong())
                }
                parent.context.startActivity(intent)
            }
        }
    }

    override fun onBindViewHolder(holder: ImageVH, position: Int) {
        holder.bind(Shared.imageList[position])
    }

    override fun getItemCount(): Int = Shared.imageList.size
}