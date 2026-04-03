/*
 * Codex — free and open-source Material You eBook reader.
 * Copyright (C) 2024-2025 BlindMint
 * SPDX-License-Identifier: GPL-3.0-only
 */

package us.blindmint.codex.presentation.reader.viewer.webtoon

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import us.blindmint.codex.presentation.reader.viewer.ZoomableImageView

class WebtoonImageAdapter(
    private val pageCount: Int,
    private val getBitmap: (Int) -> Bitmap?,
    val onPageTap: (Int) -> Unit = {},
    val onPageLongPress: (Int, Float, Float) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<WebtoonImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = ZoomableImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pageCount

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ZoomableImageView = itemView as ZoomableImageView

        fun bind(position: Int) {
            imageView.apply {
                setMaxScale(3f)
                setVerticalMode(true)
                setUseSharedZoom(true)
                callbacks = ZoomableImageView.GestureCallbacks(
                    onTap = { _, _ -> onPageTap(position) },
                    onLongPress = { x, y -> onPageLongPress(position, x, y) }
                )
            }

            getBitmap(position)?.let { bitmap ->
                imageView.setBitmap(bitmap)
            }
        }
    }
}
