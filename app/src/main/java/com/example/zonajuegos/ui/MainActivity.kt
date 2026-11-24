package com.example.zonajuegos.ui

import com.example.zonajuegos.BuildConfig
import com.example.zonajuegos.R
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.repository.BibliotecaDependencies
import com.example.zonajuegos.util.ImageLoader
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import java.io.File
import kotlinx.coroutines.launch

/**
 * Pantalla de alta. Su responsabilidad es validar el formulario, adjuntar una
 * carátula y guardar la ficha en Room; la biblioteca completa
 * se navega por separado en ColeccionActivity.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var bibliotecaViewModel: BibliotecaViewModel
    private lateinit var etTitulo: EditText
    private lateinit var etGenero: AutoCompleteTextView
    private lateinit var etPlataforma: AutoCompleteTextView
    private lateinit var etTiempo: EditText
    private lateinit var cbTieneLogros: CheckBox
    private lateinit var tvCaratula: TextView
    private lateinit var ivUltimoJuego: ImageView
    private lateinit var tvIconoUltimo: TextView
    private lateinit var tvUltimoJuego: TextView
    private lateinit var tvDetalleUltimo: TextView
    private var caratulaSeleccionada: Uri? = null
    private var ultimoId: Int? = null

    /** La cámara externa escribe la foto en un URI privado concedido por FileProvider. */
    private var uriFotoPendiente: Uri? = null
    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
        if (guardada && uriFotoPendiente != null) {
            caratulaSeleccionada = uriFotoPendiente
            mostrarCaratulaSeleccionada("Foto de carátula preparada")
        } else {
            Toast.makeText(this, "No se guardó ninguna foto.", Toast.LENGTH_SHORT).show()
        }
    }

    /** OpenDocument devuelve permiso persistible para poder mostrar la carátula más tarde. */
    private val elegirCaratula = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Algunas galerías dan acceso temporal solamente; se conserva el URI igualmente.
            }
            caratulaSeleccionada = uri
            mostrarCaratulaSeleccionada("Carátula de la galería preparada")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Room comparte la misma base con las pantallas de colección y detalles.
        bibliotecaViewModel = ViewModelProvider(
            this,
            BibliotecaViewModel.Factory(BibliotecaDependencies.repository(applicationContext))
        )[BibliotecaViewModel::class.java]
        conectarVistas()
        prepararSugerencias()
        prepararNavegacion()

        // Un único recolector sigue a Room mientras la pantalla está visible.
        // Esto evita registrar un collector nuevo cada vez que se vuelve a Inicio.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bibliotecaViewModel.juegos.collect { juegos -> mostrarUltimo(juegos.firstOrNull()) }
            }
        }
    }

    /** Enlaza los controles XML con las propiedades que usa la pantalla. */
    private fun conectarVistas() {
        etTitulo = findViewById(R.id.etTitulo)
        etGenero = findViewById(R.id.etGenero)
        etPlataforma = findViewById(R.id.etPlataforma)
        etTiempo = findViewById(R.id.etTiempoCompletar)
        cbTieneLogros = findViewById(R.id.cbTieneLogros)
        tvCaratula = findViewById(R.id.tvEstadoCaratula)
        ivUltimoJuego = findViewById(R.id.ivUltimoJuego)
        tvIconoUltimo = findViewById(R.id.tvIconoUltimoJuego)
        tvUltimoJuego = findViewById(R.id.tvUltimoJuego)
        tvDetalleUltimo = findViewById(R.id.tvDetalleUltimoJuego)

        findViewById<Button>(R.id.btnAgregar).setOnClickListener { agregarJuego() }
        findViewById<Button>(R.id.btnFotoCamara).setOnClickListener { abrirCamara() }
        findViewById<Button>(R.id.btnElegirGaleria).setOnClickListener {
            elegirCaratula.launch(arrayOf("image/*"))
        }
        findViewById<android.view.View>(R.id.cardUltimoJuego).setOnClickListener {
            ultimoId?.let(::abrirDetalle)
        }
    }

    /** Rellena los autocompletados con géneros y plataformas comunes sin impedir valores libres. */
    private fun prepararSugerencias() {
        etGenero.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, GENEROS))
        etGenero.threshold = 1
        etPlataforma.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, PLATAFORMAS))
        etPlataforma.threshold = 1
    }

    /** Conecta los accesos que abren pantallas con responsabilidades separadas. */
    private fun prepararNavegacion() {
        findViewById<Button>(R.id.btnColeccion).setOnClickListener {
            startActivity(android.content.Intent(this, ColeccionActivity::class.java))
        }
        findViewById<Button>(R.id.btnCatalogoOnline).setOnClickListener {
            startActivity(android.content.Intent(this, CatalogoActivity::class.java))
        }
        findViewById<Button>(R.id.btnRecomendacion).setOnClickListener {
            startActivity(android.content.Intent(this, RecomendacionActivity::class.java))
        }
        findViewById<Button>(R.id.btnAcercaDe).setOnClickListener {
            startActivity(android.content.Intent(this, AcercaDeActivity::class.java))
        }
    }

    /** Crea una ruta privada y pide a la aplicación de cámara del sistema que guarde la foto. */
    private fun abrirCamara() {
        val carpetaBase = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: filesDir
        val carpeta = File(carpetaBase, "covers")
        if (!carpeta.exists() && !carpeta.mkdirs()) {
            Toast.makeText(this, "No se pudo preparar la carpeta de carátulas.", Toast.LENGTH_SHORT).show()
            return
        }
        val archivo = File(carpeta, "caratula_${System.currentTimeMillis()}.jpg")
        val uriFoto = FileProvider.getUriForFile(
            this,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            archivo
        )
        uriFotoPendiente = uriFoto
        tomarFoto.launch(uriFoto)
    }

    /** Actualiza la etiqueta para que se sepa qué imagen se adjuntará al guardar. */
    private fun mostrarCaratulaSeleccionada(mensaje: String) {
        tvCaratula.text = mensaje
        tvCaratula.contentDescription = "Carátula seleccionada para el próximo juego"
    }

    /**
     * Valida los datos introducidos manualmente y los guarda en Room.
     * La exploración de datos remotos está en CatalogoActivity.
     */
    private fun agregarJuego() {
        val titulo = etTitulo.text.toString().trim()
        val genero = etGenero.text.toString().trim().ifBlank { "Otros" }
        val plataforma = normalizarPlataforma(etPlataforma.text.toString())

        if (titulo.isBlank()) {
            etTitulo.error = "Escribe el nombre del juego."
            etTitulo.requestFocus()
            return
        }
        if (plataforma.isBlank()) {
            etPlataforma.error = "Elige o escribe una plataforma."
            etPlataforma.requestFocus()
            return
        }

        // Se copia el formulario antes de la consulta de red para guardar una ficha coherente.
        val tiempoEscrito = etTiempo.text.toString().trim()
        val logrosMarcados = cbTieneLogros.isChecked
        val imagenSeleccionada = caratulaSeleccionada?.toString()
        cambiarEstadoFormulario(habilitado = false)
        lifecycleScope.launch {
            try {
                val juego = Videojuego(
                    titulo = titulo,
                    genero = genero,
                    plataforma = plataforma,
                    imagenUri = imagenSeleccionada,
                    tiempoCompletar = tiempoEscrito.ifBlank { null },
                    tieneLogros = logrosMarcados
                )

                // Las escrituras de Room también se ejecutan fuera del hilo principal.
                bibliotecaViewModel.insertar(juego)
                reproducirSonido(plataforma)
                limpiarFormulario()
                Toast.makeText(
                    this@MainActivity,
                    if (imagenSeleccionada != null) {
                        "Juego añadido con su carátula."
                    } else {
                        "Juego añadido. También puedes explorar el catálogo online."
                    },
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                Toast.makeText(this@MainActivity, "No se pudo guardar el juego. Comprueba el almacenamiento e inténtalo otra vez.", Toast.LENGTH_LONG).show()
            } finally {
                cambiarEstadoFormulario(habilitado = true)
            }
        }
    }

    /** Impide editar la ficha mientras se busca su portada y se completa el INSERT. */
    private fun cambiarEstadoFormulario(habilitado: Boolean) {
        etTitulo.isEnabled = habilitado
        etGenero.isEnabled = habilitado
        etPlataforma.isEnabled = habilitado
        etTiempo.isEnabled = habilitado
        cbTieneLogros.isEnabled = habilitado
        findViewById<Button>(R.id.btnFotoCamara).isEnabled = habilitado
        findViewById<Button>(R.id.btnElegirGaleria).isEnabled = habilitado
        findViewById<Button>(R.id.btnAgregar).isEnabled = habilitado
    }

    /** Limpia solo los datos del formulario; la foto ya se guardó en la fila de Room. */
    private fun limpiarFormulario() {
        etTitulo.text.clear()
        etGenero.text.clear()
        etPlataforma.text.clear()
        etTiempo.text.clear()
        cbTieneLogros.isChecked = false
        caratulaSeleccionada = null
        tvCaratula.text = "Portada automática al añadir desde el catálogo online"
    }

    /** Muestra el elemento más reciente y permite abrir su ficha completa. */
    private fun mostrarUltimo(juego: Videojuego?) {
        ultimoId = juego?.id
        if (juego == null) {
            tvUltimoJuego.text = "Aún no has añadido ningún juego"
            tvDetalleUltimo.text = "Tus nuevas incorporaciones aparecerán aquí"
            ivUltimoJuego.visibility = ImageView.GONE
            tvIconoUltimo.visibility = TextView.VISIBLE
            return
        }

        tvUltimoJuego.text = juego.titulo
        tvDetalleUltimo.text = "${juego.genero} · ${juego.plataforma}"
        val portada = juego.imagenUri ?: juego.imagenUrl
        if (portada.isNullOrBlank()) {
            ivUltimoJuego.visibility = ImageView.GONE
            tvIconoUltimo.visibility = TextView.VISIBLE
            tvIconoUltimo.text = "🎮"
        } else {
            ivUltimoJuego.visibility = ImageView.VISIBLE
            tvIconoUltimo.visibility = TextView.GONE
            ImageLoader.load(ivUltimoJuego, portada)
        }
    }

    /** Abre el detalle por id para que siempre se lean los datos actuales de Room. */
    private fun abrirDetalle(id: Int) {
        startActivity(android.content.Intent(this, DetalleVideojuegoActivity::class.java)
            .putExtra(DetalleVideojuegoActivity.EXTRA_VIDEOJUEGO_ID, id))
    }

    /**
     * Emite patrones cortos diferentes por consola. No necesita permisos ni
     * archivos adicionales y libera ToneGenerator cuando finaliza cada secuencia.
     */
    private fun reproducirSonido(plataforma: String) {
        val tonos = when {
            "pc" in plataforma.lowercase() -> listOf(ToneGenerator.TONE_PROP_ACK)
            "play" in plataforma.lowercase() -> listOf(ToneGenerator.TONE_PROP_BEEP, ToneGenerator.TONE_PROP_ACK)
            "xbox" in plataforma.lowercase() -> listOf(ToneGenerator.TONE_PROP_NACK)
            "nintendo" in plataforma.lowercase() || "switch" in plataforma.lowercase() ->
                listOf(ToneGenerator.TONE_PROP_BEEP2, ToneGenerator.TONE_PROP_BEEP)
            else -> listOf(ToneGenerator.TONE_PROP_BEEP)
        }
        val generador = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
        val handler = Handler(Looper.getMainLooper())
        fun siguiente(indice: Int) {
            if (indice >= tonos.size) {
                generador.release()
            } else {
                generador.startTone(tonos[indice], 120)
                handler.postDelayed({ siguiente(indice + 1) }, 160)
            }
        }
        siguiente(0)
    }

    /** Agrupa alias escritos a mano para aplicar filtros, sonido y color coherentes. */
    private fun normalizarPlataforma(texto: String): String = when {
        texto.contains("pc", ignoreCase = true) || texto.contains("steam", ignoreCase = true) -> "PC"
        texto.contains("playstation", ignoreCase = true) || texto.contains("ps4", ignoreCase = true) ||
            texto.contains("ps5", ignoreCase = true) || texto.equals("play", true) -> "PlayStation"
        texto.contains("xbox", ignoreCase = true) -> "Xbox"
        texto.contains("nintendo", ignoreCase = true) || texto.contains("switch", ignoreCase = true) -> "Nintendo"
        else -> texto.trim()
    }

    companion object {
        /** Opciones frecuentes mostradas como sugerencias, no como lista cerrada. */
        private val GENEROS = listOf(
            "Acción", "Aventura", "Carreras", "Deportes", "Estrategia", "Puzles",
            "Rol", "Sandbox", "Simulación", "Terror", "Plataformas"
        )
        /** Plataformas con códigos de color y sonidos propios en las tarjetas. */
        private val PLATAFORMAS = listOf("PC", "PlayStation", "Xbox", "Nintendo")
    }
}
