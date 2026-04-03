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

class WebtoonAdapter(
    private val pageCount: Int,
    private val loadPage: suspend (Int) -> Bitmap?,
    private val onPageTap: (Int) -> Unit = {},
    private val onPageLongPress: (Int, Float, Float) -> Unit = { _, _, _ -> }
) : RecyclerView.Adapter<WebtoonAdapter.PageViewHolder>() {

    private val pageBitmaps = mutableMapOf<Int, Bitmap?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = ZoomableImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pageCount

    fun getBitmap(position: Int): Bitmap? = pageBitmaps[position]

    inner class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

            pageBitmaps[position]?.let { bitmap ->
                imageView.setBitmap(bitmap)
            }
        }
    }
}
