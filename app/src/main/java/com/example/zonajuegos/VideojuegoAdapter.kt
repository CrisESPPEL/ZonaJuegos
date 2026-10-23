package com.example.zonajuegos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class VideojuegoAdapter (
    private val videojuegos: MutableList<Videojuego>,
    private val onItemClick: (Videojuego) -> Unit
): RecyclerView.Adapter<VideojuegoAdapter.VideojuegoViewHolder>() {

    inner class VideojuegoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloItem)

        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionItem)

        init {
            itemView.setOnClickListener {
                val posicion = bindingAdapterPosition
            }

            if(posicion != RecyclerView.NO_POSITION) {
                onItemClick(videojuegos[posicion])
            }
        }

    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int): VideojuegoViewHolder {

        val vista = LayoutInflater.from(parent.context).inflate(
            R.layout.item_videojuego,
            parent,
            false
        )

        return VideojuegoViewHolder(vista)
    }

    //
    override fun onBindViewHolder(
        holder: VideojuegoViewHolder,
        position: Int
    ) {
        val videojuego = videojuegos[posicion]

        holder.tvTitulo.text = videojuego.titulo

        holder.tvDescripcion.text = "${videojuego.genero} · ${videojuego.plataforma}"
    }

    // Función que devuelve el número total de videojuegos en la lista
    override fun getItemCount(): Int {
        return videojuegos.size
    }

}