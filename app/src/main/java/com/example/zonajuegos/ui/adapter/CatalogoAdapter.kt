package com.example.zonajuegos.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.zonajuegos.R
import com.example.zonajuegos.data.entity.JuegoCatalogo
import com.example.zonajuegos.util.ImageLoader

/** Adaptador sencillo para los juegos que vienen de la API, no de la biblioteca personal. */
class CatalogoAdapter(private val alSeleccionar: (JuegoCatalogo) -> Unit) :
    RecyclerView.Adapter<CatalogoAdapter.JuegoViewHolder>() {

    private val juegos = mutableListOf<JuegoCatalogo>()

    inner class JuegoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val portada: ImageView = itemView.findViewById(R.id.ivCatalogoPortada)
        private val titulo: TextView = itemView.findViewById(R.id.tvCatalogoTitulo)
        private val descripcion: TextView = itemView.findViewById(R.id.tvCatalogoDescripcion)

        fun enlazar(juego: JuegoCatalogo) {
            titulo.text = juego.titulo
            descripcion.text = listOf(juego.genero, juego.plataforma)
                .filter(String::isNotBlank)
                .joinToString(" · ")
            ImageLoader.load(portada, juego.miniaturaUrl)
            itemView.setOnClickListener { alSeleccionar(juego) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JuegoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_catalogo, parent, false)
        return JuegoViewHolder(vista)
    }

    override fun onBindViewHolder(holder: JuegoViewHolder, position: Int) {
        holder.enlazar(juegos[position])
    }

    override fun getItemCount(): Int = juegos.size

    /** Reemplaza las filas visibles tras una emisión de Room o una nueva consulta. */
    fun mostrar(nuevaLista: List<JuegoCatalogo>) {
        juegos.clear()
        juegos.addAll(nuevaLista)
        notifyDataSetChanged()
    }
}
