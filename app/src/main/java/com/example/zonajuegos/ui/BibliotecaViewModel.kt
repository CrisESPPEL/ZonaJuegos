package com.example.zonajuegos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.repository.BibliotecaRepository
import kotlinx.coroutines.flow.Flow

/** ViewModel fino: publica el flujo de Room y delega las acciones al repositorio. */
class BibliotecaViewModel(private val repository: BibliotecaRepository) : ViewModel() {
    val juegos: Flow<List<Videojuego>> = repository.observarJuegos()

    suspend fun insertar(juego: Videojuego) = repository.insertar(juego)
    suspend fun obtenerPorId(id: Int): Videojuego? = repository.obtenerPorId(id)
    suspend fun guardarEstado(id: Int, estado: String) = repository.guardarEstado(id, estado)
    suspend fun guardarTrailer(id: Int, url: String?) = repository.guardarTrailer(id, url)
    suspend fun partidasConUbicacion() = repository.partidasConUbicacion()
    suspend fun registrarPartida(id: Int, latitud: Double?, longitud: Double?) =
        repository.registrarPartida(id, latitud, longitud)

    /** Crea el ViewModel con acceso a Room sin añadir una librería de inyección. */
    class Factory(private val repository: BibliotecaRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(BibliotecaViewModel::class.java)) {
                return BibliotecaViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel no reconocido: ${modelClass.name}")
        }
    }
}
