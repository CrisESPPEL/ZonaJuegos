
package com.example.zonajuegos

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videojuegos")
data class Videojuego(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val titulo: String,
    val genero: String,
    val plataforma: String,

    val imagenUri: String? = null,

    val latitud: Double? = null,
    val longitud: Double? = null
)
