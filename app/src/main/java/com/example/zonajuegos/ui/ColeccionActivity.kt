package com.example.zonajuegos.ui

import com.example.zonajuegos.R
import com.example.zonajuegos.data.entity.Videojuego
import com.example.zonajuegos.data.repository.BibliotecaDependencies
import com.example.zonajuegos.ui.adapter.VideojuegoAdapter
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

/** Pantalla separada que presenta la colección completa y filtros combinables. */
class ColeccionActivity : AppCompatActivity() {
    private lateinit var bibliotecaViewModel: BibliotecaViewModel
    private lateinit var adaptador: VideojuegoAdapter
    private lateinit var filtroGenero: EditText
    private lateinit var filtroPlataforma: EditText
    private lateinit var cantidad: TextView
    private lateinit var sinResultados: TextView
    private val todosLosJuegos = mutableListOf<Videojuego>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coleccion)
        bibliotecaViewModel = ViewModelProvider(
            this,
            BibliotecaViewModel.Factory(BibliotecaDependencies.repository(applicationContext))
        )[BibliotecaViewModel::class.java]
        filtroGenero = findViewById(R.id.etFiltroGenero)
        filtroPlataforma = findViewById(R.id.etFiltroPlataforma)
        cantidad = findViewById(R.id.tvCantidadJuegos)
        sinResultados = findViewById(R.id.tvSinResultados)

        // El adaptador se comparte con otras listas para que las fichas tengan el mismo diseño.
        adaptador = VideojuegoAdapter(mutableListOf()) { juego ->
            startActivity(Intent(this, DetalleVideojuegoActivity::class.java)
                .putExtra(DetalleVideojuegoActivity.EXTRA_VIDEOJUEGO_ID, juego.id))
        }
        findViewById<RecyclerView>(R.id.rvColeccion).apply {
            layoutManager = LinearLayoutManager(this@ColeccionActivity)
            adapter = adaptador
        }
        findViewById<Button>(R.id.btnVolverColeccion).setOnClickListener { finish() }
        instalarFiltro(filtroGenero)
        instalarFiltro(filtroPlataforma)

        // Room emite cambios de inmediato al regresar del detalle o añadir otro juego.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bibliotecaViewModel.juegos.collect { juegos ->
                    todosLosJuegos.clear()
                    todosLosJuegos.addAll(juegos)
                    aplicarFiltros()
                }
            }
        }
    }

    /** Vuelve a calcular los resultados en cada edición sin necesidad de un botón de búsqueda. */
    private fun instalarFiltro(campo: EditText) {
        campo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = aplicarFiltros()
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    /** Combina los filtros de género y plataforma, sin distinguir mayúsculas. */
    private fun aplicarFiltros() {
        val genero = filtroGenero.text.toString().trim()
        val plataforma = filtroPlataforma.text.toString().trim()
        val coincidencias = todosLosJuegos.filter { juego ->
            (genero.isBlank() || juego.genero.contains(genero, ignoreCase = true)) &&
                (plataforma.isBlank() || juego.plataforma.contains(plataforma, ignoreCase = true))
        }
        adaptador.actualizarLista(coincidencias)
        cantidad.text = if (genero.isBlank() && plataforma.isBlank()) {
            "${todosLosJuegos.size} juegos"
        } else {
            "${coincidencias.size} de ${todosLosJuegos.size} juegos"
        }
        sinResultados.visibility = if (coincidencias.isEmpty()) View.VISIBLE else View.GONE
    }
}
