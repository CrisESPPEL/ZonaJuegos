package com.example.zonajuegos.ui.adapter

import com.example.zonajuegos.R
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.util.ImageLoader
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** Lista de tarjetas reutilizable tanto en la biblioteca como en la portada. */
class VideojuegoAdapter(
    private val videojuegos: MutableList<Videojuego>,
    private val onItemClick: (Videojuego) -> Unit
) : RecyclerView.Adapter<VideojuegoAdapter.VideojuegoViewHolder>() {

    /** Mantiene referencias a los controles de una tarjeta para enlazarlos rápido. */
    inner class VideojuegoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contenedor: View = itemView.findViewById(R.id.contenedorTarjeta)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTituloItem)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcionItem)
        val tvMeta: TextView = itemView.findViewById(R.id.tvMetaItem)
        val ivPortada: ImageView = itemView.findViewById(R.id.ivPortadaItem)
        val tvIconoGenero: TextView = itemView.findViewById(R.id.tvGeneroIcono)

        init {
            // Se consulta bindingAdapterPosition al tocar para evitar posiciones obsoletas.
            itemView.setOnClickListener {
                val posicion = bindingAdapterPosition
                if (posicion != RecyclerView.NO_POSITION) onItemClick(videojuegos[posicion])
            }
        }
    }

    /** Infla el diseño XML de una tarjeta, que se comparte entre las pantallas. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideojuegoViewHolder {
        val vista = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_videojuego, parent, false)
        return VideojuegoViewHolder(vista)
    }

    /** Rellena una ficha, elige una imagen distintiva y aplica el color de plataforma. */
    override fun onBindViewHolder(holder: VideojuegoViewHolder, position: Int) {
        val juego = videojuegos[position]
        holder.tvTitulo.text = juego.titulo
        holder.tvDescripcion.text = "${juego.genero} · ${juego.plataforma}"
        holder.tvMeta.text = buildList {
            add("Estado: ${juego.estado}")
            juego.tiempoCompletar?.takeIf(String::isNotBlank)?.let { add("⏱ $it") }
            if (juego.vecesJugado > 0) add("▶ ${juego.vecesJugado} partidas")
        }.joinToString("   ")
        holder.tvMeta.visibility = if (holder.tvMeta.text.isBlank()) View.GONE else View.VISIBLE

        val portada = juego.imagenUri ?: juego.imagenUrl
        if (portada.isNullOrBlank()) {
            holder.ivPortada.visibility = View.GONE
            holder.tvIconoGenero.visibility = View.VISIBLE
            holder.tvIconoGenero.text = iconoParaGenero(juego.genero)
        } else {
            holder.ivPortada.visibility = View.VISIBLE
            holder.tvIconoGenero.visibility = View.GONE
            holder.ivPortada.contentDescription = "Carátula de ${juego.titulo}"
            ImageLoader.load(holder.ivPortada, portada)
        }

        val color = Color.parseColor(colorParaPlataforma(juego.plataforma))
        holder.contenedor.background = GradientDrawable().apply {
            cornerRadius = 20f
            setColor(Color.WHITE)
            setStroke(4, color)
        }
        holder.contenedor.contentDescription =
            "${juego.titulo}, ${juego.genero}, ${juego.plataforma}. Abrir detalles."
    }

    /** Sustituye el conjunto visible para aplicar filtros sin crear otro adaptador. */
    fun actualizarLista(nuevos: List<Videojuego>) {
        videojuegos.clear()
        videojuegos.addAll(nuevos)
        notifyDataSetChanged()
    }

    /** Devuelve el número de elementos que RecyclerView puede mostrar. */
    override fun getItemCount(): Int = videojuegos.size

    /** Asigna un símbolo reconocible a cada género aunque el juego no tenga portada. */
    private fun iconoParaGenero(genero: String): String = when (genero.trim().lowercase()) {
        "aventura", "adventure" -> "🗺️"
        "carreras", "carrera", "racing" -> "🏎️"
        "rol", "rpg", "role-playing" -> "🛡️"
        "deportes", "deporte", "sports" -> "⚽"
        "acción", "accion", "action", "shooter" -> "⚔️"
        "puzles", "puzzle", "puzzle-platformer" -> "🧩"
        "estrategia", "strategy" -> "♟️"
        "simulación", "simulacion", "simulation" -> "🏙️"
        "sandbox", "mundo abierto", "open world" -> "🌄"
        "plataformas", "platformer" -> "🕹️"
        "terror", "horror" -> "👻"
        else -> "🎮"
    }

    /** Normaliza alias comunes antes de elegir el color del borde de la tarjeta. */
    private fun colorParaPlataforma(plataforma: String): String {
        val nombre = plataforma.lowercase()
        return when {
            "pc" in nombre || "steam" in nombre -> "#777F8A"
            "playstation" in nombre || "play" in nombre || "ps4" in nombre || "ps5" in nombre -> "#3478F6"
            "xbox" in nombre -> "#2D9B58"
            "nintendo" in nombre || "switch" in nombre -> "#D84545"
            else -> "#8B74C8"
        }
    }
}
