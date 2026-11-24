package com.example.zonajuegos.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.zonajuegos.R
import com.example.zonajuegos.data.repository.CatalogoDependencies
import com.example.zonajuegos.util.ImageLoader
import kotlinx.coroutines.launch

/** Ficha ampliada de FreeToGame con opción de guardar el juego en la biblioteca. */
class DetalleCatalogoActivity : AppCompatActivity() {
    private lateinit var viewModel: CatalogoViewModel

    companion object {
        const val EXTRA_JUEGO_ID = "extra_juego_catalogo_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_catalogo)

        val id = intent.getIntExtra(EXTRA_JUEGO_ID, 0)
        if (id <= 0) {
            finish()
            return
        }

        val repository = CatalogoDependencies.repository(applicationContext)
        viewModel = ViewModelProvider(this, CatalogoViewModel.Factory(repository))
            .get(CatalogoViewModel::class.java)

        findViewById<Button>(R.id.btnVolverDetalleCatalogo).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnAnadirBiblioteca).setOnClickListener {
            viewModel.anadirALaColeccion(id)
        }
        viewModel.cargarDetalle(id)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.detalle.collect { estado -> mostrarEstado(estado) }
            }
        }
    }

    /** Actualiza la pantalla solo con datos de Room/ViewModel, nunca desde la capa HTTP. */
    private fun mostrarEstado(estado: DetalleCatalogoUiState) {
        findViewById<ProgressBar>(R.id.pbDetalleCatalogo).visibility =
            if (estado.cargando) View.VISIBLE else View.GONE
        val aviso = findViewById<TextView>(R.id.tvAvisoDetalleCatalogo)
        aviso.text = when {
            estado.error != null -> estado.error
            estado.anadido -> "Añadido a tu colección."
            else -> ""
        }

        val juego = estado.juego ?: return
        findViewById<ImageView>(R.id.ivDetalleCatalogo).let { ImageLoader.load(it, juego.miniaturaUrl) }
        findViewById<TextView>(R.id.tvTituloDetalleCatalogo).text = juego.titulo
        findViewById<TextView>(R.id.tvGeneroDetalleCatalogo).text = "Género: ${juego.genero.ifBlank { "Sin indicar" }}"
        findViewById<TextView>(R.id.tvPlataformaDetalleCatalogo).text = "Plataforma: ${juego.plataforma.ifBlank { "Sin indicar" }}"
        findViewById<TextView>(R.id.tvDescripcionDetalleCatalogo).text =
            juego.descripcion.ifBlank { juego.resumen.ifBlank { "FreeToGame no ofrece una descripción para este juego." } }
        findViewById<TextView>(R.id.tvDesarrolladorDetalleCatalogo).text =
            "Desarrollador: ${juego.desarrollador.ifBlank { "Sin indicar" }}"
        findViewById<TextView>(R.id.tvLanzamientoDetalleCatalogo).text =
            "Lanzamiento: ${juego.fechaLanzamiento.ifBlank { "Sin indicar" }}"

        findViewById<Button>(R.id.btnFichaFreeToGame).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(juego.urlJuego)))
            } catch (_: Exception) {
                Toast.makeText(this, "No se pudo abrir la página del juego.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
