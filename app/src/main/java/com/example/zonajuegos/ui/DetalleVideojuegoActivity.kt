package com.example.zonajuegos.ui

import com.example.zonajuegos.R
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.repository.BibliotecaDependencies
import com.example.zonajuegos.util.ImageLoader
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Valores estables que se guardan en Room y aparecen en el desplegable. */
private const val ESTADO_JUGADO = "Jugado"
private const val ESTADO_NO_JUGADO = "No jugado"
private const val ESTADO_JUGANDO = "Jugando"
private const val ESTADO_PENDIENTE = "Pendiente por jugar"
private const val ESTADO_COMPLETADO = "Completado"
private const val ESTADO_LOGROS = "Todos los logros conseguidos"

/** Ficha de un juego: muestra sus datos, guarda su estado y abre su tráiler. */
class DetalleVideojuegoActivity : AppCompatActivity() {

    private lateinit var bibliotecaViewModel: BibliotecaViewModel
    private lateinit var selectorEstado: Spinner
    private var juego: Videojuego? = null

    /** Evita guardar como nuevo estado la selección que se carga desde Room. */
    private var preparandoSelector = false

    /** Si se concede el permiso, registra la partida con la ubicación aproximada disponible. */
    private val permisoUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        registrarPartida(guardarUbicacion = concedido)
    }

    companion object {
        /** Clave con la que la tarjeta de colección envía el ID local de Room. */
        const val EXTRA_VIDEOJUEGO_ID = "extra_videojuego_id"

        /** Una ubicación almacenada con más antigüedad no sirve para registrar una partida actual. */
        private const val UBICACION_RECIENTE_MS = 2 * 60 * 60 * 1000L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_videojuego)

        bibliotecaViewModel = ViewModelProvider(
            this,
            BibliotecaViewModel.Factory(BibliotecaDependencies.repository(applicationContext))
        )[BibliotecaViewModel::class.java]
        selectorEstado = findViewById(R.id.spEstadoJuego)

        findViewById<Button>(R.id.btnVolverDetalle).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnVerTrailer).setOnClickListener { abrirTrailer() }

        // Solo se pasa el ID entre pantallas; Room sigue siendo la fuente de datos.
        val id = intent.getIntExtra(EXTRA_VIDEOJUEGO_ID, 0)
        if (id <= 0) {
            Toast.makeText(this, "No se encontró el juego.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch { cargarJuego(id) }
    }

    /** Lee la ficha actual de Room y conecta todos los controles de la pantalla. */
    private suspend fun cargarJuego(id: Int) {
        val cargado = bibliotecaViewModel.obtenerPorId(id)
        if (cargado == null) {
            Toast.makeText(this, "Este juego ya no está en la colección.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        juego = cargado
        findViewById<TextView>(R.id.tvDetalleTitulo).text = cargado.titulo
        findViewById<TextView>(R.id.tvDetalleGenero).text = "Género: ${cargado.genero}"
        findViewById<TextView>(R.id.tvDetallePlataforma).text = "Plataforma: ${cargado.plataforma}"
        findViewById<TextView>(R.id.tvDetalleTiempo).text = cargado.tiempoCompletar?.let {
            "Tiempo para completar: $it"
        } ?: "Tiempo para completar: sin indicar"
        findViewById<TextView>(R.id.tvDetalleLogros).text = textoLogros(cargado)
        findViewById<TextView>(R.id.tvDetallePartidas).text = textoPartidas(cargado)
        findViewById<Button>(R.id.btnVerTrailer).text = when {
            !cargado.trailerUrl.isNullOrBlank() -> "VER TRÁILER"
            !cargado.urlJuego.isNullOrBlank() -> "VER FICHA ONLINE"
            else -> "SIN ENLACE ONLINE"
        }

        mostrarPortada(cargado)
        prepararSelectorEstado(cargado)
    }

    /** Distingue un dato desconocido de un juego que no tiene logros. */
    private fun textoLogros(videojuego: Videojuego): String = when {
        videojuego.numeroLogros != null -> "Número de logros: ${videojuego.numeroLogros}"
        videojuego.tieneLogros -> "Tiene logros; no se conoce el número."
        else -> "Logros: no hay información disponible."
    }

    /** Devuelve el texto del contador de partidas guardado localmente. */
    private fun textoPartidas(videojuego: Videojuego): String =
        "Partidas registradas en este dispositivo: ${videojuego.vecesJugado}"

    /** Carga una carátula propia o la portada remota; muestra un icono si falta. */
    private fun mostrarPortada(videojuego: Videojuego) {
        val imagen = findViewById<ImageView>(R.id.ivPortada)
        val portada = videojuego.imagenUri ?: videojuego.imagenUrl
        if (portada.isNullOrBlank()) {
            imagen.setImageResource(android.R.drawable.ic_menu_gallery)
        } else {
            ImageLoader.load(imagen, portada)
        }
    }

    /** Ofrece el estado de logros solo cuando el juego tiene logros o así se indicó. */
    private fun prepararSelectorEstado(videojuego: Videojuego) {
        val estados = mutableListOf(
            ESTADO_JUGADO,
            ESTADO_NO_JUGADO,
            ESTADO_JUGANDO,
            ESTADO_PENDIENTE,
            ESTADO_COMPLETADO
        )

        val tieneLogros = videojuego.tieneLogros || (videojuego.numeroLogros ?: 0) > 0
        if (tieneLogros || videojuego.estado == ESTADO_LOGROS) {
            estados.add(ESTADO_LOGROS)
        }

        // Spinner es el desplegable estándar de Android y no necesita una dependencia extra.
        selectorEstado.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            estados
        ).also { adaptador ->
            adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        preparandoSelector = true
        val indiceGuardado = estados.indexOf(videojuego.estado)
        selectorEstado.setSelection(indiceGuardado.takeIf { it >= 0 } ?: estados.indexOf(ESTADO_NO_JUGADO))
        preparandoSelector = false

        // Un cambio a un estado jugado registra una partida una sola vez por transición.
        selectorEstado.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!preparandoSelector) guardarEstado(estados[position])
            }
        }
    }

    /** Persiste el estado y, al pasar a un estado jugado, registra la primera partida. */
    private fun guardarEstado(nuevoEstado: String) {
        val anterior = juego ?: return
        if (nuevoEstado == anterior.estado) return

        selectorEstado.isEnabled = false
        lifecycleScope.launch {
            try {
                bibliotecaViewModel.guardarEstado(anterior.id, nuevoEstado)
                val actualizado = bibliotecaViewModel.obtenerPorId(anterior.id)
                if (actualizado != null) juego = actualizado

                Toast.makeText(this@DetalleVideojuegoActivity, "Estado guardado.", Toast.LENGTH_SHORT).show()

                val pasaAEstadoJugado = !indicaQueSeHaJugado(anterior.estado) &&
                    indicaQueSeHaJugado(nuevoEstado)
                if (pasaAEstadoJugado) pedirPermisoORegistrar()
            } catch (_: Exception) {
                // Si falla el guardado, devolvemos el desplegable al valor que tenía Room.
                prepararSelectorEstado(anterior)
                Toast.makeText(this@DetalleVideojuegoActivity, "No se pudo guardar el estado.", Toast.LENGTH_LONG).show()
            } finally {
                selectorEstado.isEnabled = true
            }
        }
    }

    /** Solo estos estados confirman que la persona ya empezó a jugar. */
    private fun indicaQueSeHaJugado(estado: String): Boolean = estado in listOf(
        ESTADO_JUGADO,
        ESTADO_JUGANDO,
        ESTADO_COMPLETADO,
        ESTADO_LOGROS
    )

    /** Pide ubicación aproximada para asociar la partida; si se deniega, la guarda sin GPS. */
    private fun pedirPermisoORegistrar() {
        val permisoConcedido = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (permisoConcedido) {
            registrarPartida(guardarUbicacion = true)
        } else {
            permisoUbicacion.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    /** Suma una partida y registra su posición solo si Android la pudo proporcionar. */
    private fun registrarPartida(guardarUbicacion: Boolean) {
        val actual = juego ?: return
        lifecycleScope.launch {
            val ubicacion = if (guardarUbicacion) obtenerUltimaUbicacion() else null
            bibliotecaViewModel.registrarPartida(
                actual.id,
                ubicacion?.latitude,
                ubicacion?.longitude
            )

            val actualizado = bibliotecaViewModel.obtenerPorId(actual.id)
            if (actualizado != null) {
                juego = actualizado
                findViewById<TextView>(R.id.tvDetallePartidas).text = textoPartidas(actualizado)
            }

            val mensaje = if (ubicacion == null) {
                "Partida registrada sin ubicación."
            } else {
                "Partida y ubicación aproximada guardadas en este dispositivo."
            }
            Toast.makeText(this@DetalleVideojuegoActivity, mensaje, Toast.LENGTH_LONG).show()
        }
    }

    /** Usa la última ubicación aproximada conocida; nunca inicia seguimiento en segundo plano. */
    @Suppress("MissingPermission") // La función se llama tras comprobar el permiso del sistema.
    private fun obtenerUltimaUbicacion(): Location? = try {
        val gestor = getSystemService(LOCATION_SERVICE) as LocationManager
        gestor.getProviders(true)
            .mapNotNull { proveedor -> gestor.getLastKnownLocation(proveedor) }
            .maxByOrNull { it.time }
            ?.takeIf { System.currentTimeMillis() - it.time <= UBICACION_RECIENTE_MS }
    } catch (_: Exception) {
        null
    }

    /** Abre un tráiler legado guardado o la página del juego enlazada desde FreeToGame. */
    private fun abrirTrailer() {
        val actual = juego ?: return
        val url = actual.trailerUrl ?: actual.urlJuego
        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Este juego no tiene un enlace externo guardado.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: Exception) {
            Toast.makeText(this, "No hay una aplicación para abrir este enlace.", Toast.LENGTH_SHORT).show()
        }
    }
}
