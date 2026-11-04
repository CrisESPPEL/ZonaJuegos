
package com.example.zonajuegos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow

@Dao
interface VideojuegoDao {

    @Insert
    suspend fun insertar(videojuego: Videojuego)

    @Delete
    suspend fun eliminar(videojuego: Videojuego)

    @Query("SELECT * FROM videojuegos ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<Videojuego>>

    @Query("SELECT * FROM videojuegos WHERE id = :id")
    suspend fun obtenerPorId(id: Int): Videojuego?
}
