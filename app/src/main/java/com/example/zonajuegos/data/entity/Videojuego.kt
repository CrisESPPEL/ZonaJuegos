package com.example.zonajuegos.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registro persistente de un juego de la colección.
 *
 * Cada propiedad representa una columna de Room. Los campos de portada admiten
 * una imagen del catálogo de FreeToGame o una carátula elegida/fotografiada.
 * La ubicación y el contador solo se usan para sugerencias locales; nunca se
 * envían al servicio del catálogo ni se comparten con un servidor.
 */
@Entity(tableName = "videojuegos")
data class Videojuego(
    /** Identificador local autogenerado por SQLite. */
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Nombre que se muestra en la biblioteca y en la ficha del juego. */
    val titulo: String,
    /** Género libre, por ejemplo aventura, rol o carreras. */
    val genero: String,
    /** Plataforma normalizada al guardar: PC, PlayStation, Xbox o Nintendo. */
    val plataforma: String,

    /** URI privada o persistente de la foto/caratula elegida por el usuario. */
    val imagenUri: String? = null,
    /** URL de miniatura recibida desde el catálogo online, si se añadió desde allí. */
    val imagenUrl: String? = null,
    /** Identificador antiguo conservado para migrar bibliotecas de versiones anteriores. */
    val rawgId: Int? = null,
    /** Enlace de tráiler guardado en una versión anterior, si todavía existe. */
    val trailerUrl: String? = null,
    /** Página del juego obtenida de FreeToGame, si se añadió desde el catálogo. */
    val urlJuego: String? = null,
    /** Duración orientativa escrita por la persona. */
    val tiempoCompletar: String? = null,
    /** Indica si el usuario sabe que el juego tiene logros. */
    @ColumnInfo(defaultValue = "0")
    val tieneLogros: Boolean = false,
    /** Cantidad opcional de logros; FreeToGame no ofrece este dato. */
    val numeroLogros: Int? = null,
    /** Estado que la persona elige para organizar el progreso de este juego. */
    @ColumnInfo(defaultValue = "'No jugado'")
    val estado: String = "No jugado",
    /** Fecha de alta en milisegundos para ordenar la colección por novedad. */
    @ColumnInfo(defaultValue = "0")
    val creadoEn: Long = System.currentTimeMillis(),
    /** Cambios a un estado jugado que se han registrado en esta instalación. */
    @ColumnInfo(defaultValue = "0")
    val vecesJugado: Int = 0,

    /** Última ubicación aproximada donde se marcó como jugado, si hubo permiso. */
    val latitud: Double? = null,
    /** Longitud aproximada asociada a la última sesión registrada. */
    val longitud: Double? = null
)
