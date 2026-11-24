package com.example.zonajuegos.data.repository

import android.content.Context
import com.example.zonajuegos.data.entity.Partida
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.local.VideojuegoDao
import com.example.zonajuegos.data.local.VideojuegoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/** Encapsula las operaciones locales sobre la colección y las partidas. */
class BibliotecaRepository(private val dao: VideojuegoDao) {
    fun observarJuegos(): Flow<List<Videojuego>> = dao.obtenerTodos()

    suspend fun insertar(juego: Videojuego) = withContext(Dispatchers.IO) { dao.insertar(juego) }
    suspend fun obtenerPorId(id: Int): Videojuego? = withContext(Dispatchers.IO) { dao.obtenerPorId(id) }
    suspend fun guardarEstado(id: Int, estado: String) = withContext(Dispatchers.IO) {
        dao.guardarEstado(id, estado)
    }
    suspend fun guardarTrailer(id: Int, url: String?) = withContext(Dispatchers.IO) {
        dao.guardarTrailer(id, url)
    }
    suspend fun partidasConUbicacion(): List<Partida> = withContext(Dispatchers.IO) {
        dao.obtenerPartidasConUbicacion()
    }

    /** Actualiza contador y coordenadas en una operación SQL y guarda la sesión local. */
    suspend fun registrarPartida(id: Int, latitud: Double?, longitud: Double?) = withContext(Dispatchers.IO) {
        dao.registrarPartida(id, latitud, longitud)
        if (latitud != null && longitud != null) {
            dao.insertarPartida(Partida(videojuegoId = id, latitud = latitud, longitud = longitud))
        }
    }
}

/** Provee un repositorio enlazado a la instancia compartida de Room. */
object BibliotecaDependencies {
    fun repository(context: Context) = BibliotecaRepository(
        VideojuegoDatabase.getDatabase(context).videojuegoDao()
    )
}
