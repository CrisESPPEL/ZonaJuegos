package com.example.zonajuegos.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Punto único para mostrar carátulas e imágenes remotas o locales.
 * Glide gestiona descargas en segundo plano, recicla solicitudes de listas y
 * mantiene una caché local para reducir tráfico y ayudar sin conexión.
 */
object ImageLoader {
    fun load(imageView: ImageView, source: String?) {
        if (source.isNullOrBlank()) {
            Glide.with(imageView).clear(imageView)
            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            return
        }

        Glide.with(imageView)
            .load(source)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(imageView)
    }
}
