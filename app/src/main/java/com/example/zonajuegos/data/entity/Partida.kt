package com.example.zonajuegos.data.entity

import com.example.zonajuegos.data.entity.Videojuego
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una sesión local de juego. Guardar una fila por sesión permite contar con
 * precisión qué juego se ha marcado como jugado cerca de cada zona, en vez de
 * atribuir todas sus partidas a la última ubicación conocida.
 */
@Entity(
    tableName = "partidas",
    foreignKeys = [
        ForeignKey(
            entity = Videojuego::class,
            parentColumns = ["id"],
            childColumns = ["videojuegoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("videojuegoId")]
)
data class Partida(
    /** Identificador único de la sesión. */
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    /** Juego al que pertenece; borrar el juego elimina sus sesiones asociadas. */
    val videojuegoId: Int,
    /** Coordenadas aproximadas guardadas solo si la persona concedió el permiso. */
    val latitud: Double,
    val longitud: Double,
    /** Momento local de la sesión para auditoría y ordenación futura. */
    val fecha: Long = System.currentTimeMillis()
)
