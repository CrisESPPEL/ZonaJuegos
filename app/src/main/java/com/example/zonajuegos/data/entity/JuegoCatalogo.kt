package com.example.zonajuegos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Copia local de un juego descargado de FreeToGame.
 *
 * Esta tabla es la caché del catálogo online. Se mantiene separada de
 * [Videojuego], que representa los juegos que el usuario ha añadido a su
 * biblioteca personal.
 */
@Entity(tableName = "catalogo_juegos")
data class JuegoCatalogo(
    /** FreeToGame usa un ID estable para consultar el detalle. */
    @PrimaryKey val id: Int,
    val titulo: String,
    val resumen: String,
    val descripcion: String,
    val genero: String,
    val plataforma: String,
    val miniaturaUrl: String?,
    val imagenesJson: String,
    val desarrollador: String,
    val editor: String,
    val fechaLanzamiento: String,
    val urlJuego: String,
    val requisitosMinimos: String,
    /** Hora de descarga, útil para saber cuándo se actualizó la caché. */
    val actualizadoEn: Long = System.currentTimeMillis()
)
