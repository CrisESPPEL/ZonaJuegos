package com.example.zonajuegos.data.local

import com.example.zonajuegos.data.entity.Partida
import com.example.zonajuegos.data.entity.JuegoCatalogo
import com.example.zonajuegos.data.entity.Videojuego
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acceso a los datos. Room genera la implementación SQL durante la
 * compilación; las funciones suspendidas no bloquean la interfaz.
 */
@Dao
interface VideojuegoDao {

    /** Emite el catálogo cacheado; la pantalla puede seguir mostrándolo sin red. */
    @Query("SELECT * FROM catalogo_juegos ORDER BY titulo COLLATE NOCASE")
    fun observarCatalogo(): Flow<List<JuegoCatalogo>>

    /** Busca una ficha cacheada para abrirla aunque la API no responda. */
    @Query("SELECT * FROM catalogo_juegos WHERE id = :id LIMIT 1")
    suspend fun obtenerJuegoCatalogo(id: Int): JuegoCatalogo?

    /** Inserta o actualiza las fichas que devuelve una consulta a FreeToGame. */
    @Upsert
    suspend fun guardarCatalogo(juegos: List<JuegoCatalogo>)

    /** Guarda un detalle actualizado sin borrar la fila local si ya existía. */
    @Upsert
    suspend fun guardarJuegoCatalogo(juego: JuegoCatalogo)

    /** Inserta una ficha sin reemplazar otra que ya tenga el mismo identificador. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(videojuego: Videojuego)

    /** Elimina la ficha seleccionada de la colección. */
    @Delete
    suspend fun eliminar(videojuego: Videojuego)

    /** Emite la biblioteca completa y se actualiza automáticamente tras cada cambio. */
    @Query("SELECT * FROM videojuegos ORDER BY creadoEn DESC, id DESC")
    fun obtenerTodos(): Flow<List<Videojuego>>

    /** Recupera un juego concreto para abrir su ficha detallada. */
    @Query("SELECT * FROM videojuegos WHERE id = :id LIMIT 1")
    suspend fun obtenerPorId(id: Int): Videojuego?

    /** Devuelve el último juego añadido para la sección destacada de Inicio. */
    @Query("SELECT * FROM videojuegos ORDER BY creadoEn DESC, id DESC LIMIT 1")
    suspend fun obtenerUltimo(): Videojuego?

    /** Sesiones con ubicación aproximada para contar partidas en el radio elegido. */
    @Query("SELECT * FROM partidas WHERE latitud IS NOT NULL AND longitud IS NOT NULL")
    suspend fun obtenerPartidasConUbicacion(): List<Partida>

    /** Añade una sesión a la tabla local de historial. */
    @Insert
    suspend fun insertarPartida(partida: Partida)

    /**
     * Suma una sesión de juego y guarda su ubicación aproximada cuando Android
     * concedió el permiso. Si falta una ubicación nueva, conserva la anterior.
     */
    @Query("UPDATE videojuegos SET vecesJugado = vecesJugado + 1, latitud = COALESCE(:latitud, latitud), longitud = COALESCE(:longitud, longitud) WHERE id = :id")
    suspend fun registrarPartida(id: Int, latitud: Double?, longitud: Double?)

    /** Guarda el estado elegido en la ficha del juego. */
    @Query("UPDATE videojuegos SET estado = :estado WHERE id = :id")
    suspend fun guardarEstado(id: Int, estado: String)

    /** Conserva un enlace de tráiler legado cuando ya existía en la colección. */
    @Query("UPDATE videojuegos SET trailerUrl = :url WHERE id = :id")
    suspend fun guardarTrailer(id: Int, url: String?)
}
