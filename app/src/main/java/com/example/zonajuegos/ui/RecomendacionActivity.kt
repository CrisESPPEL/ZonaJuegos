package com.example.zonajuegos.ui

import com.example.zonajuegos.R
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.repository.BibliotecaDependencies
import com.example.zonajuegos.util.ImageLoader
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlin.random.Random
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Recomendación local que usa partidas registradas y, si faltan, una elección aleatoria. */
class RecomendacionActivity : AppCompatActivity() {
    private lateinit var bibliotecaViewModel: BibliotecaViewModel
    private lateinit var botonBuscar: Button
    private lateinit var imagen: ImageView
    private lateinit var titulo: TextView
    private lateinit var motivo: TextView
    private lateinit var ubicacionTexto: TextView

    /** La pregunta de permiso solo se lanza después de pulsar el botón de recomendación. */
    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        buscarRecomendacion(usandoUbicacion = concedido)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recomendacion)
        bibliotecaViewModel = ViewModelProvider(
            this,
            BibliotecaViewModel.Factory(BibliotecaDependencies.repository(applicationContext))
        )[BibliotecaViewModel::class.java]
        botonBuscar = findViewById(R.id.btnBuscarJuegoZona)
        imagen = findViewById(R.id.ivRecomendacion)
        titulo = findViewById(R.id.tvJuegoRecomendado)
        motivo = findViewById(R.id.tvMotivoRecomendacion)
        ubicacionTexto = findViewById(R.id.tvUbicacionRecomendacion)

        findViewById<Button>(R.id.btnVolverRecomendacion).setOnClickListener { finish() }
        botonBuscar.setOnClickListener {
            val permisoConcedido = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (permisoConcedido) buscarRecomendacion(usandoUbicacion = true)
            else permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    /**
     * Elige el juego más jugado dentro de cinco kilómetros si hay datos; de lo
     * contrario selecciona uno al azar y explica por qué no se pudo usar la zona.
     */
    private fun buscarRecomendacion(usandoUbicacion: Boolean) {
        botonBuscar.isEnabled = false
        lifecycleScope.launch {
            try {
                val juegos = bibliotecaViewModel.juegos.first()
                if (juegos.isEmpty()) {
                    titulo.text = "Aún no hay juegos en tu colección"
                    motivo.text = "Añade un juego desde Inicio para recibir una recomendación."
                    imagen.visibility = View.GONE
                    return@launch
                }

                // Primero intenta una ubicación reciente; si no existe, pide una nueva al proveedor del dispositivo.
                val ubicacionActual = if (usandoUbicacion) obtenerUbicacionActual() else null
                ubicacionTexto.text = ubicacionActual?.let {
                    "Ubicación aproximada: ${"%.3f".format(it.latitude)}, ${"%.3f".format(it.longitude)}"
                } ?: "Ubicación no disponible; se recomendará un juego al azar."
                val partidas = bibliotecaViewModel.partidasConUbicacion()
                val partidasPorJuegoCerca = if (ubicacionActual != null) {
                    partidas.filter { partida ->
                        distanciaEnMetros(
                            ubicacionActual.latitude,
                            ubicacionActual.longitude,
                            partida.latitud,
                            partida.longitud
                        ) <= RADIO_ZONA_METROS
                    }
                        .groupingBy { it.videojuegoId }
                        .eachCount()
                } else {
                    emptyMap()
                }

                // Sin ubicación o sin sesiones cercanas, siempre recomendamos un juego de la biblioteca.
                val masJugadoEnZona = partidasPorJuegoCerca.maxByOrNull { it.value }
                val recomendado = masJugadoEnZona?.key?.let { id -> juegos.firstOrNull { it.id == id } }
                    ?: juegos[Random.nextInt(juegos.size)]

                titulo.text = recomendado.titulo
                motivo.text = if (masJugadoEnZona != null) {
                    "El más jugado entre los registros guardados a menos de 5 km (${masJugadoEnZona.value} partidas en esta zona)."
                } else when {
                    !usandoUbicacion -> "Sugerencia aleatoria: no se concedió acceso a la ubicación."
                    ubicacionActual == null -> "Sugerencia aleatoria: no se pudo obtener una ubicación actual. Comprueba que la ubicación esté activada."
                    else -> "Sugerencia aleatoria: todavía no hay partidas registradas cerca de esta zona."
                }

                mostrarPortada(recomendado)
            } catch (_: Exception) {
                // Aunque falle la lectura local, la pantalla recupera el botón para volver a intentarlo.
                titulo.text = "No se pudo consultar la colección"
                motivo.text = "Vuelve a intentarlo. Si no hay ubicación o registros, se elegirá un juego al azar."
                imagen.visibility = View.GONE
            } finally {
                botonBuscar.isEnabled = true
            }
        }
    }

    /** Muestra la carátula local o la portada descargada del catálogo online. */
    private fun mostrarPortada(juego: Videojuego) {
        val portada = juego.imagenUri ?: juego.imagenUrl
        if (portada.isNullOrBlank()) {
            imagen.visibility = View.GONE
        } else {
            imagen.visibility = View.VISIBLE
            ImageLoader.load(imagen, portada)
        }
    }

    /** Usa una posición reciente o solicita una sola actualización; nunca inicia seguimiento continuo. */
    @Suppress("MissingPermission") // Solo se llama tras conceder ACCESS_COARSE_LOCATION.
    private suspend fun obtenerUbicacionActual(): Location? {
        return try {
            val manager = getSystemService(LOCATION_SERVICE) as LocationManager
            val reciente = manager.getProviders(true)
                .mapNotNull { proveedor -> manager.getLastKnownLocation(proveedor) }
                .maxByOrNull { it.time }
                ?.takeIf { System.currentTimeMillis() - it.time <= UBICACION_RECIENTE_MS }
            if (reciente != null) return reciente

            val proveedor = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .firstOrNull { manager.isProviderEnabled(it) } ?: return null

            // En emulador, primero envía una ubicación desde Extended Controls o con `adb emu geo fix`.
            withTimeoutOrNull(15_000L) {
                suspendCancellableCoroutine { continuacion ->
                    val listener = android.location.LocationListener { ubicacion ->
                        if (continuacion.isActive) continuacion.resume(ubicacion)
                    }
                    manager.requestSingleUpdate(proveedor, listener, Looper.getMainLooper())
                    continuacion.invokeOnCancellation { manager.removeUpdates(listener) }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Distancia entre dos coordenadas geográficas con la fórmula de Haversine. */
    private fun distanciaEnMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val desde = Location("juego").apply { latitude = lat1; longitude = lon1 }
        return desde.distanceTo(Location("actual").apply { latitude = lat2; longitude = lon2 })
    }

    companion object {
        /** Radio de búsqueda de registros de juego alrededor de la ubicación aproximada. */
        private const val RADIO_ZONA_METROS = 5_000f
        /** Prefiere una ubicación de sistema reciente; de lo contrario se pide un único dato nuevo. */
        private const val UBICACION_RECIENTE_MS = 2 * 60 * 1000L
    }
}
