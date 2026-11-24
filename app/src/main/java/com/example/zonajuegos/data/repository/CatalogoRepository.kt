package com.example.zonajuegos.data.repository

import android.content.Context
import com.example.zonajuegos.data.entity.JuegoCatalogo
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.local.VideojuegoDao
import com.example.zonajuegos.data.local.VideojuegoDatabase
import com.example.zonajuegos.data.remote.FreeToGameClient
import com.example.zonajuegos.data.remote.FreeToGameDto
import com.example.zonajuegos.data.remote.FreeToGameService
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Único punto que decide cuándo usar Internet, cuándo usar Room y cómo pasar
 * de los modelos HTTP a los modelos persistidos de la aplicación.
 */
class CatalogoRepository(
    private val dao: VideojuegoDao,
    private val servicio: FreeToGameService
) {
    private val gson = Gson()

    /** Observa Room: la lista se puede dibujar antes de terminar una petición. */
    fun observarCatalogo(): Flow<List<JuegoCatalogo>> = dao.observarCatalogo()

    /**
     * Consulta el listado remoto y lo actualiza en Room. Si falla la red, lanza
     * un error para que la pantalla muestre el mensaje; Room conserva la caché.
     */
    suspend fun actualizarCatalogo(plataforma: String?, categoria: String?) {
        val respuesta = servicio.listarJuegos(plataforma, categoria)
        if (!respuesta.isSuccessful) {
            throw IOException("FreeToGame respondió con HTTP ${respuesta.code()}.")
        }

        val lista = respuesta.body() ?: throw IOException("FreeToGame devolvió una lista vacía.")
        dao.guardarCatalogo(lista.mapNotNull(::convertir))
    }

    /**
     * Busca detalle actualizado. Si el servidor no responde, devuelve la copia
     * local para que la ficha siga abierta sin conexión.
     */
    suspend fun obtenerDetalle(id: Int): JuegoCatalogo {
        try {
            val respuesta = servicio.obtenerJuego(id)
            if (respuesta.isSuccessful) {
                respuesta.body()?.let { dto ->
                    convertir(dto)?.let { juego ->
                        dao.guardarJuegoCatalogo(juego)
                        return juego
                    }
                }
            }
        } catch (cancelacion: CancellationException) {
            // No convertir la salida de la pantalla en un intento de lectura offline.
            throw cancelacion
        } catch (_: Exception) {
            // La consulta local de abajo es el respaldo si falta Internet o falla el servidor.
        }

        return dao.obtenerJuegoCatalogo(id)
            ?: throw IOException("No hay conexión ni una ficha guardada para este juego.")
    }

    /** Añade a la biblioteca personal los datos que la persona ya vio en el catálogo. */
    suspend fun anadirALaColeccion(id: Int) {
        val juego = dao.obtenerJuegoCatalogo(id)
            ?: throw IOException("Primero hay que cargar la ficha del juego.")

        dao.insertar(
            Videojuego(
                titulo = juego.titulo,
                genero = juego.genero.ifBlank { "Otros" },
                plataforma = normalizarPlataforma(juego.plataforma),
                imagenUrl = juego.miniaturaUrl,
                urlJuego = juego.urlJuego
            )
        )
    }

    /** Convierte un DTO sin asumir que los campos opcionales siempre existen. */
    private fun convertir(dto: FreeToGameDto): JuegoCatalogo? {
        if (dto.id <= 0 || dto.title.isBlank()) return null

        return JuegoCatalogo(
            id = dto.id,
            titulo = dto.title,
            resumen = dto.resumen.orEmpty(),
            descripcion = dto.description.orEmpty(),
            genero = dto.genre.orEmpty(),
            plataforma = dto.platform.orEmpty(),
            miniaturaUrl = dto.thumbnail,
            imagenesJson = gson.toJson(dto.screenshots.orEmpty()),
            desarrollador = dto.developer.orEmpty(),
            editor = dto.publisher.orEmpty(),
            fechaLanzamiento = dto.fechaLanzamiento.orEmpty(),
            urlJuego = dto.urlJuego ?: dto.urlPerfil.orEmpty(),
            requisitosMinimos = gson.toJson(dto.requisitos)
        )
    }

    /** Los juegos del catálogo usan Windows o navegador; se mapea Windows a PC. */
    private fun normalizarPlataforma(plataforma: String): String = when {
        plataforma.contains("windows", ignoreCase = true) -> "PC"
        plataforma.contains("browser", ignoreCase = true) || plataforma.contains("web", ignoreCase = true) -> "Navegador"
        else -> plataforma.ifBlank { "PC" }
    }
}

/** Construye dependencias compartidas para que las Activities no creen clientes HTTP distintos. */
object CatalogoDependencies {
    fun repository(context: Context): CatalogoRepository = CatalogoRepository(
        dao = VideojuegoDatabase.getDatabase(context).videojuegoDao(),
        servicio = FreeToGameClient.service
    )
}
