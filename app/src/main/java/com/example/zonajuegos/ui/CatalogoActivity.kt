package com.example.zonajuegos.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.zonajuegos.R
import com.example.zonajuegos.data.repository.CatalogoDependencies
import com.example.zonajuegos.ui.adapter.CatalogoAdapter
import kotlinx.coroutines.launch

/** Catálogo online de juegos free-to-play, respaldado por Room para modo sin conexión. */
class CatalogoActivity : AppCompatActivity() {
    private lateinit var viewModel: CatalogoViewModel
    private lateinit var adaptador: CatalogoAdapter
    private lateinit var progreso: ProgressBar
    private lateinit var mensaje: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalogo)

        // La Activity solo conecta vistas y observa el estado publicado por ViewModel.
        val repository = CatalogoDependencies.repository(applicationContext)
        viewModel = ViewModelProvider(this, CatalogoViewModel.Factory(repository))
            .get(CatalogoViewModel::class.java)

        progreso = findViewById(R.id.pbCatalogo)
        mensaje = findViewById(R.id.tvEstadoCatalogo)
        val plataforma = findViewById<Spinner>(R.id.spPlataformaCatalogo)
        val categoria = findViewById<EditText>(R.id.etCategoriaCatalogo)

        plataforma.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Todas las plataformas", "Windows", "Navegador")
        )

        adaptador = CatalogoAdapter { juego ->
            startActivity(
                Intent(this, DetalleCatalogoActivity::class.java)
                    .putExtra(DetalleCatalogoActivity.EXTRA_JUEGO_ID, juego.id)
            )
        }
        findViewById<RecyclerView>(R.id.rvCatalogo).apply {
            layoutManager = LinearLayoutManager(this@CatalogoActivity)
            adapter = adaptador
        }

        findViewById<Button>(R.id.btnBuscarCatalogo).setOnClickListener {
            val plataformaApi = when (plataforma.selectedItemPosition) {
                1 -> "windows"
                2 -> "browser"
                else -> ""
            }
            viewModel.buscar(plataformaApi, categoria.text.toString().trim())
        }
        findViewById<Button>(R.id.btnVolverCatalogo).setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.catalogo.collect { estado ->
                    adaptador.mostrar(estado.juegos)
                    progreso.visibility = if (estado.cargando) View.VISIBLE else View.GONE
                    mensaje.text = estado.error ?: if (estado.juegos.isEmpty()) {
                        if (estado.cargando) "Cargando catálogo…" else "No hay juegos guardados. Conéctate y pulsa Buscar."
                    } else {
                        "${estado.juegos.size} juegos. Los datos se conservan en Room."
                    }
                }
            }
        }
    }
}
