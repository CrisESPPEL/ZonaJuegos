package com.example.zonajuegos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.zonajuegos.data.entity.JuegoCatalogo
import com.example.zonajuegos.data.repository.CatalogoRepository
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estados que la pantalla del catálogo puede presentar al usuario. */
data class CatalogoUiState(
    val juegos: List<JuegoCatalogo> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null
)

/** Estado de detalle de un juego, separado del estado del listado. */
data class DetalleCatalogoUiState(
    val juego: JuegoCatalogo? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val anadido: Boolean = false
)

/**
 * Mantiene el estado de catálogo/detalle entre cambios de configuración de la
 * Activity y delega las operaciones de datos en CatalogoRepository.
 */
class CatalogoViewModel(private val repository: CatalogoRepository) : ViewModel() {
    private val _catalogo = MutableStateFlow(CatalogoUiState())
    val catalogo = _catalogo.asStateFlow()

    private val _detalle = MutableStateFlow(DetalleCatalogoUiState())
    val detalle = _detalle.asStateFlow()

    /** Lista completa que Room mantiene; la pantalla recibe solo las coincidencias. */
    private var juegosGuardados: List<JuegoCatalogo> = emptyList()
    private var plataforma = ""
    private var categoria = ""

    init {
        // Room emite inmediatamente la caché mientras la primera petición está en curso.
        viewModelScope.launch {
            repository.observarCatalogo().collect { juegos ->
                juegosGuardados = juegos
                publicarCoincidencias()
            }
        }
        actualizarCatalogo()
    }

    /** Aplica los filtros escogidos y vuelve a consultar el servicio remoto. */
    fun buscar(plataformaElegida: String, categoriaElegida: String) {
        plataforma = plataformaElegida
        // La API publica sus categorías como etiquetas en minúsculas (por ejemplo, shooter).
        categoria = categoriaElegida.trim().lowercase(Locale.ROOT)
        publicarCoincidencias()
        actualizarCatalogo()
    }

    /** Aplica los mismos filtros a Room que se envían a FreeToGame. */
    private fun publicarCoincidencias() {
        val coincidencias = juegosGuardados.filter { juego ->
            coincidePlataforma(juego.plataforma) &&
                (categoria.isBlank() || juego.genero.contains(categoria, ignoreCase = true))
        }
        _catalogo.update { it.copy(juegos = coincidencias) }
    }

    /** Adapta los nombres Windows/browser del API a lo que guarda la respuesta. */
    private fun coincidePlataforma(nombre: String): Boolean = when (plataforma.lowercase()) {
        "", "all" -> true
        "windows", "pc" -> nombre.contains("windows", ignoreCase = true) ||
            nombre.contains("pc", ignoreCase = true)
        "browser", "web", "navegador" -> nombre.contains("browser", ignoreCase = true) ||
            nombre.contains("web", ignoreCase = true)
        else -> nombre.contains(plataforma, ignoreCase = true)
    }

    /** Descarga y cachea el catálogo, conservando la lista local si la red falla. */
    private fun actualizarCatalogo() {
        viewModelScope.launch {
            _catalogo.update { it.copy(cargando = true, error = null) }
            try {
                repository.actualizarCatalogo(
                    plataforma = plataforma.ifBlank { null },
                    categoria = categoria.ifBlank { null }
                )
            } catch (cancelacion: CancellationException) {
                // La cancelación es normal cuando se cierra la pantalla o se destruye el ViewModel.
                throw cancelacion
            } catch (_: Exception) {
                val tieneCache = juegosGuardados.isNotEmpty()
                _catalogo.update {
                    it.copy(error = if (tieneCache) {
                        "No se pudo actualizar. Se muestran los juegos guardados en este dispositivo."
                    } else {
                        "No se pudo conectar con FreeToGame. Comprueba Internet e inténtalo de nuevo."
                    })
                }
            } finally {
                _catalogo.update { it.copy(cargando = false) }
            }
        }
    }

    /** Consulta el detalle, usando la ficha cacheada si la red no está disponible. */
    fun cargarDetalle(id: Int) {
        viewModelScope.launch {
            _detalle.value = DetalleCatalogoUiState(cargando = true)
            try {
                _detalle.value = DetalleCatalogoUiState(juego = repository.obtenerDetalle(id))
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _detalle.value = DetalleCatalogoUiState(error = error.message ?: "No se pudo abrir el juego.")
            }
        }
    }

    /** Guarda en la colección personal el juego que se está viendo. */
    fun anadirALaColeccion(id: Int) {
        viewModelScope.launch {
            try {
                repository.anadirALaColeccion(id)
                _detalle.update { it.copy(anadido = true, error = null) }
            } catch (cancelacion: CancellationException) {
                throw cancelacion
            } catch (error: Exception) {
                _detalle.update { it.copy(error = error.message ?: "No se pudo guardar el juego.") }
            }
        }
    }

    /** Reintenta la petición manteniendo los filtros actualmente seleccionados. */
    fun reintentar() = actualizarCatalogo()

    /** Factory sencilla para proporcionar el repositorio sin una librería de DI. */
    class Factory(private val repository: CatalogoRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogoViewModel::class.java)) {
                return CatalogoViewModel(repository) as T
            }
            throw IllegalArgumentException("ViewModel no reconocido: ${modelClass.name}")
        }
    }
}
